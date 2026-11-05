"""Ejecuta pruebas y conserva un registro real, no resultados predeterminados."""
from pathlib import Path
import datetime,json,platform,subprocess,sys,os

root=Path(__file__).resolve().parent
out=root.parent/'docs/evidencias'
out.mkdir(parents=True,exist_ok=True)
result=subprocess.run([sys.executable,'-m','unittest','discover','-s','tests','-v'],cwd=root,capture_output=True,text=True,encoding='utf-8',errors='replace',env={**os.environ,'PYTHONIOENCODING':'utf-8'})
registro=result.stdout+result.stderr
(out/'pruebas_backend.txt').write_text(registro,encoding='utf-8')
data={'ejecutado_en_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'comando':'python -m unittest discover -s tests -v','python':platform.python_version(),'plataforma':platform.platform(),'exit_code':result.returncode,'alcance':'Datos sintéticos; comprobación funcional. Sin validación urbana ni prueba de dispositivo Android.'}
(out/'verificacion.json').write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
sys.stdout.reconfigure(encoding='utf-8',errors='replace')
print(registro)
sys.exit(result.returncode)
