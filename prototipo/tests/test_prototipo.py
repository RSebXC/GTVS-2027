"""Verificaciones funcionales sobre datos sintéticos, sin inferencia urbana."""
from datetime import datetime,timezone,timedelta
from pathlib import Path
from tempfile import TemporaryDirectory
from concurrent.futures import ThreadPoolExecutor
from urllib.request import Request,urlopen
from urllib.error import HTTPError
import json
import threading
import unittest
from backend.dominio import Repositorio,ErrorDominio
from backend.escenario import TRAMOS,PERFILES
from backend.servidor import crear_servidor

class DominioTests(unittest.TestCase):
    def setUp(self):
        self.tmp=TemporaryDirectory()
        self.repo=Repositorio(Path(self.tmp.name)/'test.sqlite3')
        self.now=datetime.now(timezone.utc)
    def tearDown(self): self.tmp.cleanup()
    def aporte(self,tramo='BC',fecha=True):
        return {'tipo':'incidente_contextual','tramo':tramo,'gravedad':3,
                'descripcion':'Evidencia ficticia para prueba automática.',
                'ocurrido_en':self.now.isoformat() if fecha else None}
    def test_ruta_conectada_y_no_inventa_alternativas(self):
        res=self.repo.calcular('A','I',instante=self.now)
        self.assertTrue(res['sintetico'])
        self.assertEqual(len({tuple(r['nodos']) for r in res['alternativas']}),len(res['alternativas']))
        for a in res['alternativas']:
            self.assertEqual(a['nodos'][0],'A');self.assertEqual(a['nodos'][-1],'I')
            self.assertEqual(len(a['tramos']),len(a['nodos'])-1)
    def test_dijkstra_contra_enumeracion_independiente(self):
        # Oráculo exhaustivo de caminos simples del grafo pequeño; no usa el algoritmo probado.
        snap=self.repo.instantanea(self.now)
        graph={chr(65+i):[] for i in range(9)}
        for e,a,b,d,t,known in TRAMOS:
            graph[a].append((b,e,d,t,known));graph[b].append((a,e,d,t,known))
        for name,cfg in PERFILES.items():
            candidates=[]
            def visit(node,seen,cost):
                if node=='I': candidates.append(cost);return
                for other,e,d,t,known in graph[node]:
                    if other in seen or (not known and cfg['riesgo']):continue
                    w=cfg['distancia']*d/1000+cfg['tiempo']*t/60
                    if cfg['riesgo']:w+=cfg['riesgo']*snap['riesgos'][e]['valor']*d/1000
                    visit(other,seen|{other},cost+w)
            visit('A',{'A'},0)
            result=self.repo.calcular('A','I',name,self.now)['alternativas'][0]
            self.assertAlmostEqual(result['costos_por_perfil'][name],min(candidates),places=5)
    def test_informacion_faltante_no_es_cero(self):
        self.assertIsNone(self.repo.instantanea()['riesgos']['EH']['valor'])
        for a in self.repo.calcular('E','H')['alternativas']:
            if 'menor_riesgo_estimado' in a['perfiles'] or 'equilibrada' in a['perfiles']:
                self.assertNotIn('EH',a['tramos'])
        fast=self.repo.calcular('E','H','rapida')['alternativas'][0]
        self.assertIsNone(fast['costo_riesgo_relativo'])
    def test_pendiente_no_modifica_riesgo(self):
        antes=self.repo.instantanea(self.now)
        self.repo.reportar(self.aporte(),'autor','clave')
        despues=self.repo.instantanea(self.now)
        self.assertEqual(antes,despues)
    def test_idempotencia_y_conflicto(self):
        r=self.repo.reportar(self.aporte(),'autor','k')
        self.assertEqual(r['id'],self.repo.reportar(self.aporte(),'autor','k')['id'])
        with self.assertRaises(ErrorDominio) as err:self.repo.reportar(self.aporte('EF'),'autor','k')
        self.assertEqual(err.exception.status,409)
    def test_reenvios_concurrentes_no_duplican(self):
        with ThreadPoolExecutor(max_workers=4) as ex:
            rows=list(ex.map(lambda _:self.repo.reportar(self.aporte(),'autor','concurrente'),range(8)))
        self.assertEqual(len({x['id'] for x in rows}),1)
        self.assertEqual(len(self.repo.listar_reportes()),1)
    def test_vincular_mismo_hecho_no_aumenta_frecuencia(self):
        r=self.repo.reportar(self.aporte(),'autor','dup')
        before=self.repo.instantanea(self.now)
        self.repo.revisar(r['id'],{'decision':'aceptar','motivo':'Mismo hecho del escenario.','incidente_destino_id':'inc-demo-1','revision_esperada':1})
        after=self.repo.instantanea(self.now)
        self.assertEqual(before['riesgos'],after['riesgos'])
        self.assertEqual(len(self.repo.listar_incidentes()),3)
    def test_aceptar_nuevo_hecho_y_persistencia(self):
        r=self.repo.reportar(self.aporte('GH'),'autor')
        self.repo.revisar(r['id'],{'decision':'aceptar','motivo':'Hecho nuevo en ejemplo controlado.','revision_esperada':1})
        self.assertEqual(len(Repositorio(self.repo.path).listar_incidentes()),4)
        self.assertGreater(self.repo.instantanea()['riesgos']['GH']['valor'],0)
    def test_conflicto_de_revision(self):
        r=self.repo.reportar(self.aporte(),'autor')
        self.repo.revisar(r['id'],{'decision':'mantener_pendiente','motivo':'Se necesita más información.','revision_esperada':1})
        with self.assertRaises(ErrorDominio) as e:self.repo.revisar(r['id'],{'decision':'rechazar','motivo':'Revisión antigua no permitida.','revision_esperada':1})
        self.assertEqual(e.exception.codigo,'REVISION_CONCURRENTE')
    def test_fecha_desconocida_no_es_reciente(self):
        r=self.repo.reportar(self.aporte(fecha=False),'autor')
        with self.assertRaises(ErrorDominio) as e:self.repo.revisar(r['id'],{'decision':'aceptar','motivo':'Intentar crear sin fecha conocida.','revision_esperada':1})
        self.assertEqual(e.exception.codigo,'FECHA_INSUFICIENTE')
    def test_decaimiento_temporal_disminuye_contribucion(self):
        a=self.repo.instantanea(self.now)['riesgos']['BC']['valor']
        b=self.repo.instantanea(self.now+timedelta(days=7))['riesgos']['BC']['valor']
        self.assertAlmostEqual(b,a/2,places=5)
    def test_entradas_invalidas(self):
        for origin,dest,profile in [('Z','A','rapida'),('A','A','todos'),('A','I','segura')]:
            with self.subTest(origin=origin,dest=dest,profile=profile):
                with self.assertRaises(ErrorDominio):self.repo.calcular(origin,dest,profile)
        for severity in [True,0,6,'3']:
            with self.assertRaises(ErrorDominio):self.repo.reportar({**self.aporte(),'gravedad':severity},'autor')
        with self.assertRaises(ErrorDominio):self.repo.reportar({**self.aporte(),'ocurrido_en':(self.now+timedelta(days=1)).isoformat()},'autor')
    def test_rechazar_no_crea_incidente_y_deja_auditoria(self):
        r=self.repo.reportar(self.aporte(),'autor')
        self.repo.revisar(r['id'],{'decision':'rechazar','motivo':'Caso ficticio rechazado por revisión.','revision_esperada':1})
        self.assertEqual(len(self.repo.listar_incidentes()),3)
        with self.repo.conectar() as c:self.assertEqual(c.execute('SELECT COUNT(*) FROM revisiones').fetchone()[0],1)

class ApiTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp=TemporaryDirectory();cls.repo=Repositorio(Path(cls.tmp.name)/'api.sqlite3')
        cls.server=crear_servidor(cls.repo,port=0,token='token-solo-prueba')
        cls.thread=threading.Thread(target=cls.server.serve_forever,daemon=True);cls.thread.start()
        cls.base=f'http://127.0.0.1:{cls.server.server_port}'
    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown();cls.server.server_close();cls.thread.join();cls.tmp.cleanup()
    def request(self,path,data=None,headers=None):
        h=headers or {}
        if data is not None:h={**h,'Content-Type':'application/json'}
        req=Request(self.base+path,data=json.dumps(data).encode() if data is not None else None,headers=h)
        try:
            with urlopen(req,timeout=5) as r:return r.status,r.headers,r.read()
        except HTTPError as e:return e.code,e.headers,e.read()
    def test_web_y_archivos_privados(self):
        status,headers,body=self.request('/')
        self.assertEqual(status,200);self.assertIn(b'Escenario sint',body)
        self.assertIn("script-src 'self'",headers['Content-Security-Policy'])
        self.assertEqual(self.request('/backend/servidor.py')[0],404)
        self.assertEqual(self.request('/datos/local.sqlite3')[0],404)
    def test_moderacion_requiere_token(self):
        self.assertEqual(self.request('/api/reportes')[0],403)
        self.assertEqual(self.request('/api/reportes',headers={'Authorization':'Bearer token-solo-prueba'})[0],200)
    def test_recorrido_http_completo(self):
        d={'tipo':'otro','tramo':'GH','gravedad':2,'descripcion':'Prueba HTTP con información inventada.','ocurrido_en':datetime.now(timezone.utc).isoformat()}
        status,_,body=self.request('/api/reportes',d)
        self.assertEqual(status,201);id=json.loads(body)['id']
        status,_,_=self.request('/api/reportes/'+id+'/revision',{'decision':'aceptar','motivo':'Se acepta como nuevo caso sintético.','revision_esperada':1},{'Authorization':'Bearer token-solo-prueba'})
        self.assertEqual(status,200)
        status,_,body=self.request('/api/rutas/calcular',{'origen':'A','destino':'I','perfil':'todos'})
        self.assertEqual(status,200);self.assertTrue(json.loads(body)['sintetico'])
    def test_error_de_dominio_en_api(self):
        status,_,body=self.request('/api/rutas/calcular',{'origen':'Z','destino':'I'})
        self.assertEqual(status,422);self.assertEqual(json.loads(body)['error']['codigo'],'PUNTO_INVALIDO')

if __name__=='__main__':unittest.main(verbosity=2)
