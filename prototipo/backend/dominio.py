from datetime import datetime, timezone, timedelta
import hashlib
import heapq
import json
import math
import sqlite3
import uuid
from contextlib import contextmanager
from .escenario import NODOS, TRAMOS, PERFILES, MODELO, VERSION_RED, semilla

class ErrorDominio(Exception):
    def __init__(self,codigo,mensaje,status=422):
        super().__init__(mensaje)
        self.codigo,self.mensaje,self.status=codigo,mensaje,status

def ahora(): return datetime.now(timezone.utc)

def fecha(valor):
    if not isinstance(valor,str): raise ErrorDominio('FECHA_INVALIDA','Indique una fecha ISO 8601 con zona horaria.')
    try:
        d=datetime.fromisoformat(valor.replace('Z','+00:00'))
        if d.tzinfo is None: raise ValueError()
        return d.astimezone(timezone.utc)
    except (ValueError,TypeError): raise ErrorDominio('FECHA_INVALIDA','La fecha debe incluir zona horaria.')

class Repositorio:
    def __init__(self,path):
        self.path=str(path)
        self.inicializar()

    @contextmanager
    def conectar(self):
        c=sqlite3.connect(self.path,timeout=10)
        c.row_factory=sqlite3.Row
        c.execute('PRAGMA foreign_keys=ON')
        try:
            with c:
                yield c
        finally:
            c.close()

    def inicializar(self):
        with self.conectar() as c:
            c.executescript('''
            CREATE TABLE IF NOT EXISTS meta(clave TEXT PRIMARY KEY,valor TEXT NOT NULL);
            CREATE TABLE IF NOT EXISTS incidentes(id TEXT PRIMARY KEY,tramo TEXT NOT NULL,
              gravedad INTEGER NOT NULL CHECK(gravedad BETWEEN 1 AND 5),ocurrido_en TEXT NOT NULL,
              respaldo REAL NOT NULL CHECK(respaldo BETWEEN 0 AND 1),estado TEXT NOT NULL);
            CREATE TABLE IF NOT EXISTS reportes(id TEXT PRIMARY KEY,actor TEXT NOT NULL,
              tipo TEXT NOT NULL,tramo TEXT NOT NULL,gravedad INTEGER NOT NULL,
              ocurrido_en TEXT,descripcion TEXT NOT NULL,recibido_en TEXT NOT NULL,
              estado TEXT NOT NULL,incidente_id TEXT REFERENCES incidentes(id),
              clave TEXT,huella TEXT NOT NULL,revision INTEGER NOT NULL DEFAULT 1,
              UNIQUE(actor,clave));
            CREATE TABLE IF NOT EXISTS revisiones(id TEXT PRIMARY KEY,reporte_id TEXT NOT NULL
              REFERENCES reportes(id),decision TEXT NOT NULL,motivo TEXT NOT NULL,
              realizada_en TEXT NOT NULL,revision_anterior INTEGER NOT NULL,
              incidente_id TEXT REFERENCES incidentes(id));
            ''')
            if not c.execute("SELECT valor FROM meta WHERE clave='revision'").fetchone():
                now=ahora()
                c.execute("INSERT INTO meta VALUES('revision','1')")
                c.execute("INSERT INTO meta VALUES('semilla_en',?)",(now.isoformat(),))
                c.executemany('INSERT INTO incidentes VALUES(?,?,?,?,?,\'aceptado\')',semilla(now))

    def escenario(self):
        with self.conectar() as c:
            m=dict(c.execute('SELECT clave,valor FROM meta'))
        return {'sintetico':True,'version_red':VERSION_RED,'nodos':NODOS,
                'tramos':[{'id':t[0],'origen':t[1],'destino':t[2],'distancia_m':t[3],
                           'tiempo_s':t[4],'cobertura':t[5],'bidireccional':True} for t in TRAMOS],
                'perfiles':PERFILES,'modelo':MODELO,'revision':int(m['revision']),
                'semilla_en':m['semilla_en'],'advertencia':'Red abstracta y evidencia ficticia. No utilizar para desplazamientos reales.'}

    def instantanea(self,instante=None):
        instante=instante or ahora()
        # Una transacción de lectura conserva la revisión y los hechos del mismo estado.
        with self.conectar() as c:
            c.execute('BEGIN')
            revision=int(c.execute("SELECT valor FROM meta WHERE clave='revision'").fetchone()[0])
            hechos=[dict(x) for x in c.execute("SELECT * FROM incidentes WHERE estado='aceptado'")]
        riesgos={}
        for t in TRAMOS:
            contrib=[]
            for i in hechos:
                if i['tramo']!=t[0]: continue
                antiguedad=max(0,(instante-fecha(i['ocurrido_en'])).total_seconds()/86400)
                temporal=math.exp(-math.log(2)*antiguedad/MODELO['semivida_dias'])
                valor=i['gravedad']*temporal*i['respaldo']
                contrib.append({'incidente_id':i['id'],'gravedad_demo':i['gravedad'],
                                'antiguedad_dias':round(antiguedad,4),'respaldo_demo':i['respaldo'],
                                'influencia_espacial_demo':1,'contribucion':round(valor,6)})
            riesgos[t[0]]={'valor':sum(x['contribucion'] for x in contrib) if t[5] else None,
                'suficiencia':'cobertura_sintetica_declarada' if t[5] else 'informacion_insuficiente',
                'incidentes_unicos':len(contrib),'contribuciones':contrib}
        return {'revision':revision,'calculado_en':instante.isoformat(),'version_red':VERSION_RED,
                'modelo':MODELO,'riesgos':riesgos,'sintetico':True}

    def calcular(self,origen,destino,perfil='todos',instante=None):
        ids={n['id'] for n in NODOS}
        if not isinstance(origen,str) or not isinstance(destino,str) or origen not in ids or destino not in ids:
            raise ErrorDominio('PUNTO_INVALIDO','Seleccione puntos de la red sintética.')
        if origen==destino: raise ErrorDominio('MISMO_PUNTO','Origen y destino deben ser diferentes.')
        if not isinstance(perfil,str) or perfil not in {*PERFILES,'todos'}:
            raise ErrorDominio('PERFIL_INVALIDO','Perfil no soportado.')
        snap=self.instantanea(instante)
        alternativas=[]
        no_disponibles=[]
        for nombre in PERFILES if perfil=='todos' else [perfil]:
            config=PERFILES[nombre]
            graph={i:[] for i in ids}
            for tramo,a,b,d,t,cobertura in TRAMOS:
                r=snap['riesgos'][tramo]['valor']
                if r is None and config['riesgo']>0: continue
                costo=config['distancia']*d/1000+config['tiempo']*t/60
                if config['riesgo']>0: costo+=config['riesgo']*r*d/1000
                graph[a].append((b,tramo,costo,d,t))
                graph[b].append((a,tramo,costo,d,t))
            queue=[(0,origen,[],[origen],0,0)]
            best={}
            found=None
            while queue:
                cost,n,edges,nodes,dist,duration=heapq.heappop(queue)
                if cost>=best.get(n,float('inf')): continue
                best[n]=cost
                if n==destino:
                    found=(cost,edges,nodes,dist,duration)
                    break
                for other,edge,weight,d,t in graph[n]:
                    if other not in best:
                        heapq.heappush(queue,(cost+weight,other,edges+[edge],nodes+[other],dist+d,duration+t))
            if found is None:
                no_disponibles.append({'perfil':nombre,'codigo':'SIN_RUTA_CON_INFORMACION_UTILIZABLE'})
                continue
            cost,edges,nodes,dist,duration=found
            missing=[e for e in edges if snap['riesgos'][e]['valor'] is None]
            q=None if missing else sum(snap['riesgos'][e]['valor']*next(t[3] for t in TRAMOS if t[0]==e)/1000 for e in edges)
            matching=next((a for a in alternativas if a['nodos']==nodes),None)
            if matching:
                matching['perfiles'].append(nombre)
                matching['costos_por_perfil'][nombre]=round(cost,6)
            else:
                alternativas.append({'id':f'alternativa-{len(alternativas)+1}','perfiles':[nombre],
                    'costos_por_perfil':{nombre:round(cost,6)},'nodos':nodes,'tramos':edges,
                    'distancia_m':dist,'tiempo_estimado_s':duration,
                    'costo_riesgo_relativo':round(q,6) if q is not None else None,
                    'sin_informacion':missing,'suficiencia':'informacion_insuficiente' if missing else 'cobertura_sintetica_declarada'})
        if not alternativas: raise ErrorDominio('SIN_RUTA','No hay conexión utilizable para el perfil solicitado.')
        return {**snap,'origen':origen,'destino':destino,'alternativas':alternativas,'no_disponibles':no_disponibles,
                'politica_faltantes':'Los perfiles con riesgo excluyen tramos sin información. Esto no demuestra que sean más seguros.',
                'advertencia':'Resultados computacionales sobre datos ficticios; no son recomendaciones para la Ciudad de Guatemala.'}

    def reportar(self,data,actor,clave=None):
        if not isinstance(data,dict): raise ErrorDominio('CUERPO_INVALIDO','Se requiere un objeto JSON.',400)
        tipo=data.get('tipo')
        tramo=data.get('tramo')
        gravedad=data.get('gravedad')
        descripcion=data.get('descripcion')
        ocurrido=data.get('ocurrido_en')
        if tipo not in ['incidente_contextual','obstruccion','otro']:
            raise ErrorDominio('TIPO_INVALIDO','Seleccione un tipo del catálogo demostrativo.')
        if not isinstance(tramo,str) or tramo not in {t[0] for t in TRAMOS}:
            raise ErrorDominio('TRAMO_INVALIDO','Seleccione un tramo de la red abstracta.')
        if type(gravedad) is not int or not 1<=gravedad<=5:
            raise ErrorDominio('GRAVEDAD_INVALIDA','La escala demostrativa acepta enteros de 1 a 5.')
        if not isinstance(descripcion,str) or not 10<=len(descripcion.strip())<=1000:
            raise ErrorDominio('DESCRIPCION_INVALIDA','La descripción debe tener entre 10 y 1000 caracteres.')
        if not isinstance(actor,str) or not 1<=len(actor)<=80:
            raise ErrorDominio('ACTOR_INVALIDO','Identidad demostrativa inválida.',400)
        if clave is not None and (not isinstance(clave,str) or not 1<=len(clave)<=100):
            raise ErrorDominio('CLAVE_INVALIDA','Clave de idempotencia inválida.',400)
        if ocurrido is not None:
            d=fecha(ocurrido)
            if d>ahora()+timedelta(minutes=5): raise ErrorDominio('FECHA_FUTURA','No se admite ocurrencia futura.')
            ocurrido=d.isoformat()
        normal={'tipo':tipo,'tramo':tramo,'gravedad':gravedad,'descripcion':descripcion.strip(),'ocurrido_en':ocurrido}
        huella=hashlib.sha256(json.dumps(normal,sort_keys=True,ensure_ascii=False).encode()).hexdigest()
        with self.conectar() as c:
            c.execute('BEGIN IMMEDIATE')
            if clave:
                old=c.execute('SELECT * FROM reportes WHERE actor=? AND clave=?',(actor,clave)).fetchone()
                if old:
                    if old['huella']!=huella: raise ErrorDominio('CLAVE_REUTILIZADA','La clave ya corresponde a otro contenido.',409)
                    return {'id':old['id'],'estado':old['estado'],'reenvio':True,'revision':old['revision']}
            id='rep-'+uuid.uuid4().hex[:12]
            c.execute('INSERT INTO reportes(id,actor,tipo,tramo,gravedad,ocurrido_en,descripcion,recibido_en,estado,clave,huella) VALUES(?,?,?,?,?,?,?,?,?,?,?)',
                (id,actor,tipo,tramo,gravedad,ocurrido,normal['descripcion'],ahora().isoformat(),'pendiente',clave,huella))
        return {'id':id,'estado':'pendiente','reenvio':False,'revision':1}

    def listar_reportes(self):
        with self.conectar() as c:
            return [dict(r) for r in c.execute('SELECT id,tipo,tramo,gravedad,ocurrido_en,descripcion,recibido_en,estado,incidente_id,revision FROM reportes ORDER BY recibido_en DESC LIMIT 100')]

    def listar_incidentes(self):
        with self.conectar() as c:
            return [dict(r) for r in c.execute("SELECT * FROM incidentes WHERE estado='aceptado' ORDER BY id")]

    def revisar(self,id,data):
        if not isinstance(data,dict): raise ErrorDominio('CUERPO_INVALIDO','Se requiere un objeto.',400)
        decision=data.get('decision')
        motivo=data.get('motivo')
        if decision not in ['aceptar','rechazar','mantener_pendiente']:
            raise ErrorDominio('DECISION_INVALIDA','Decisión no soportada.')
        if not isinstance(motivo,str) or not 10<=len(motivo.strip())<=1000:
            raise ErrorDominio('MOTIVO_INVALIDO','Registre un fundamento entre 10 y 1000 caracteres.')
        if type(data.get('revision_esperada')) is not int:
            raise ErrorDominio('REVISION_INVALIDA','Indique la revisión esperada.')
        with self.conectar() as c:
            c.execute('BEGIN IMMEDIATE')
            r=c.execute('SELECT * FROM reportes WHERE id=?',(id,)).fetchone()
            if not r: raise ErrorDominio('NO_ENCONTRADO','Reporte no encontrado.',404)
            if r['revision']!=data.get('revision_esperada'):
                raise ErrorDominio('REVISION_CONCURRENTE','El reporte cambió; recargue antes de revisar.',409)
            if r['estado']!='pendiente': raise ErrorDominio('YA_REVISADO','El reporte ya tiene una decisión final.',409)
            destino=None
            if decision=='aceptar':
                destino=data.get('incidente_destino_id')
                if destino:
                    if not isinstance(destino,str): raise ErrorDominio('INCIDENTE_INVALIDO','Destino inválido.')
                    i=c.execute("SELECT * FROM incidentes WHERE id=? AND estado='aceptado'",(destino,)).fetchone()
                    if not i or i['tramo']!=r['tramo']:
                        raise ErrorDominio('INCIDENTE_INCOMPATIBLE','El destino debe existir y pertenecer al mismo tramo demostrativo.')
                    # Vincular evidencia no cambia respaldo ni cuenta hechos nuevos.
                else:
                    if not r['ocurrido_en']:
                        raise ErrorDominio('FECHA_INSUFICIENTE','Un reporte sin fecha no es elegible para crear un hecho temporal. Manténgalo pendiente.')
                    destino='inc-'+uuid.uuid4().hex[:12]
                    c.execute('INSERT INTO incidentes VALUES(?,?,?,?,?,\'aceptado\')',
                              (destino,r['tramo'],r['gravedad'],r['ocurrido_en'],0.6))
                c.execute("UPDATE meta SET valor=CAST(valor AS INTEGER)+1 WHERE clave='revision'")
            estado={'aceptar':'aceptado','rechazar':'rechazado','mantener_pendiente':'pendiente'}[decision]
            c.execute('UPDATE reportes SET estado=?,incidente_id=?,revision=revision+1 WHERE id=?',(estado,destino,id))
            c.execute('INSERT INTO revisiones VALUES(?,?,?,?,?,?,?)',
                      (uuid.uuid4().hex,id,decision,motivo.strip(),ahora().isoformat(),r['revision'],destino))
        return {'id':id,'estado':estado,'incidente_id':destino,'revision':r['revision']+1}
