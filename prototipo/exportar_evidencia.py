"""Guarda una consulta y el esquema físico realmente creado en una base temporal."""
from pathlib import Path
from tempfile import TemporaryDirectory
from datetime import datetime,timezone
import json
from backend.dominio import Repositorio

root=Path(__file__).resolve().parents[1]
out=root/'docs/evidencias'
out.mkdir(parents=True,exist_ok=True)
with TemporaryDirectory() as tmp:
    repo=Repositorio(Path(tmp)/'escenario.sqlite3')
    instant=datetime.now(timezone.utc)
    results={'escenario':repo.escenario(),'consulta_A_I':repo.calcular('A','I',instante=instant),
             'consulta_E_H':repo.calcular('E','H',instante=instant),
             'alcance':'Salida real del código sobre datos sintéticos. No es cartografía ni criminalidad observada.'}
    (out/'consulta_sintetica.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
    with repo.conectar() as c:
        sql='-- Esquema extraído de SQLite, prototipo sintético v0.1. Sin base geoespacial real.\nPRAGMA foreign_keys=ON;\n\n'
        sql+=';\n\n'.join(r[0] for r in c.execute("SELECT sql FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name"))+';\n'
    (root/'docs/tesis/ESQUEMA_SQLITE_IMPLEMENTADO.sql').write_text(sql,encoding='utf-8')
print('Consulta real de demostración y esquema SQLite guardados.')
