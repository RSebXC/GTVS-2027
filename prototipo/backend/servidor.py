"""Servidor local de demostración. No exponer en Internet."""
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit
import argparse
import hmac
import json
import os
import secrets
import sys
import threading
import time
from .dominio import Repositorio,ErrorDominio

WEB=Path(__file__).resolve().parents[1]/'web'
ARCHIVOS={'/':('index.html','text/html; charset=utf-8'),'/app.js':('app.js','text/javascript; charset=utf-8'),
          '/styles.css':('styles.css','text/css; charset=utf-8'),'/manifest.webmanifest':('manifest.webmanifest','application/manifest+json'),
          '/icon.svg':('icon.svg','image/svg+xml')}

def crear_servidor(repo,host='127.0.0.1',port=8765,token=None):
    token=token or secrets.token_urlsafe(24)
    limiter={}
    lock=threading.Lock()
    class Handler(BaseHTTPRequestHandler):
        server_version='PrototipoAcademico/0.1'
        def log_message(self,format,*args): pass  # No conservar cuerpos, tokens ni trayectos.

        def enviar(self,status,body,tipo='application/json; charset=utf-8'):
            payload=body if isinstance(body,bytes) else json.dumps(body,ensure_ascii=False,allow_nan=False).encode('utf-8')
            self.send_response(status)
            self.send_header('Content-Type',tipo)
            self.send_header('Content-Length',str(len(payload)))
            self.send_header('Cache-Control','no-store')
            self.send_header('X-Content-Type-Options','nosniff')
            self.send_header('Content-Security-Policy',"default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'")
            self.send_header('Referrer-Policy','no-referrer')
            self.end_headers()
            self.wfile.write(payload)

        def error(self,e): self.enviar(e.status,{'error':{'codigo':e.codigo,'mensaje':e.mensaje},'sintetico':True})

        def moderador(self):
            valor=self.headers.get('Authorization','')
            if not hmac.compare_digest(valor,'Bearer '+token):
                raise ErrorDominio('NO_AUTORIZADO','Se requiere el token de moderación de esta sesión local.',403)

        def do_GET(self):
            path=urlsplit(self.path).path
            try:
                if path in ARCHIVOS:
                    name,tipo=ARCHIVOS[path]
                    self.enviar(200,(WEB/name).read_bytes(),tipo)
                elif path=='/api/salud': self.enviar(200,{'estado':'ok','version':'1.0.0','sintetico':True})
                elif path=='/api/escenario': self.enviar(200,repo.escenario())
                elif path=='/api/incidentes': self.enviar(200,{'incidentes':repo.listar_incidentes(),'sintetico':True})
                elif path=='/api/reportes':
                    self.moderador()
                    self.enviar(200,{'reportes':repo.listar_reportes(),'sintetico':True})
                else: raise ErrorDominio('NO_ENCONTRADO','Recurso inexistente.',404)
            except ErrorDominio as e: self.error(e)
            except Exception:
                self.error(ErrorDominio('ERROR_INTERNO','No fue posible completar la operación.',500))

        def do_POST(self):
            try:
                if not self.headers.get('Content-Type','').lower().startswith('application/json'):
                    raise ErrorDominio('TIPO_NO_ADMITIDO','Utilice application/json.',415)
                try: size=int(self.headers.get('Content-Length','0'))
                except ValueError: raise ErrorDominio('LONGITUD_INVALIDA','Longitud inválida.',400)
                if size<=0 or size>16384: raise ErrorDominio('CUERPO_INVALIDO','El cuerpo debe ser JSON y no exceder 16 KiB.',413)
                try:
                    def invalido(x): raise ValueError(x)
                    data=json.loads(self.rfile.read(size),parse_constant=invalido)
                except (ValueError,UnicodeError): raise ErrorDominio('JSON_INVALIDO','JSON inválido.',400)
                if not isinstance(data,dict): raise ErrorDominio('CUERPO_INVALIDO','Se requiere un objeto JSON.',400)
                path=urlsplit(self.path).path
                # Defensa local básica; no sustituye identidad, CSRF y limitación distribuida.
                with lock:
                    now=time.monotonic()
                    key=self.client_address[0]
                    recent=[t for t in limiter.get(key,[]) if now-t<60]
                    if len(recent)>=120: raise ErrorDominio('LIMITE_LOCAL','Demasiadas solicitudes; espere un minuto.',429)
                    limiter[key]=recent+[now]
                if path=='/api/rutas/calcular':
                    self.enviar(200,repo.calcular(data.get('origen'),data.get('destino'),data.get('perfil','todos')))
                elif path=='/api/reportes':
                    result=repo.reportar(data,self.headers.get('X-Demo-Actor','colaborador-local'),self.headers.get('Idempotency-Key'))
                    self.enviar(200 if result['reenvio'] else 201,result)
                elif path.startswith('/api/reportes/') and path.endswith('/revision'):
                    self.moderador()
                    id=path.split('/')[3]
                    self.enviar(200,repo.revisar(id,data))
                else: raise ErrorDominio('NO_ENCONTRADO','Operación inexistente.',404)
            except ErrorDominio as e: self.error(e)
            except Exception:
                self.error(ErrorDominio('ERROR_INTERNO','No fue posible completar la operación.',500))
    server=ThreadingHTTPServer((host,port),Handler)
    server.daemon_threads=True
    server.token_moderador=token
    return server

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--host',default='127.0.0.1')
    parser.add_argument('--port',type=int,default=8765)
    parser.add_argument('--db',default=str(Path(__file__).resolve().parents[1]/'datos/local.sqlite3'))
    args=parser.parse_args()
    Path(args.db).resolve().parent.mkdir(parents=True,exist_ok=True)
    server=crear_servidor(Repositorio(args.db),args.host,args.port,os.environ.get('TESIS_DEMO_MOD_TOKEN'))
    print(f'Prototipo sintético local: http://{args.host}:{server.server_port}',flush=True)
    print(f'Token de moderación solo para esta sesión: {server.token_moderador}',flush=True)
    print('Ctrl+C para detener. Sin datos reales, sin navegación real.',flush=True)
    try: server.serve_forever()
    except KeyboardInterrupt: pass
    finally: server.server_close()

if __name__=='__main__': main()
