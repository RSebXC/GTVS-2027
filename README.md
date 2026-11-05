# GTVS

**Guatemala Tránsito y Vialidad Segura**  
Prototipo académico de recomendación de rutas con riesgo estimado y reportes revisados. Proyecto de graduación de Rodrigo Sebastián Castro Aguilar, Ingeniería en Ciencias y Sistemas, Universidad de San Carlos de Guatemala. Asesor: Ing. André Mendoza Torres.

> «Segura» forma parte del nombre del proyecto. Ninguna ruta constituye una garantía de seguridad. El índice es una estimación relativa, no una probabilidad validada de victimización.

## Versiones

- **V1 / v1.0.0:** demostración local reproducible con red abstracta, API Python, SQLite, interfaz web y código Android. Diecisiete pruebas funcionales registradas; Android todavía sin compilación e instalación verificadas.
- **V2:** desarrollo separado para mapa real, selección de origen y destino, cuentas de colaboradores y moderación. Sus funciones se documentarán como realizadas solo después de comprobarlas. El alcance está pendiente de revisión con el asesor.

El año 2027 del repositorio identifica el proyecto previsto; el avance de noviembre de 2026 corresponde al curso y no a una tesis final aprobada.

## Ejecutar V1

Requiere Python 3.13. No requiere instalar dependencias Python.

```powershell
cd prototipo
python -m backend.servidor
```

Abrir `http://127.0.0.1:8765`. El servicio imprime un token temporal para la revisión local. No incluirlo en capturas ni publicarlo.

```powershell
python ejecutar_verificacion.py
```

El comando ejecuta pruebas reales y conserva sus resultados en `docs/evidencias/`. La red de V1 y sus incidentes son ficticios. No utilizar sus recorridos para desplazamientos.

## Organización

```text
prototipo/
  backend/   API, persistencia y reglas de V1
  web/       cliente web de V1
  android/   proyecto Android Kotlin
  tests/     verificaciones de dominio y HTTP
docs/
  arquitectura/ esquema físico de V1
  diagramas/ fuentes editables Mermaid
  evidencias/ registros reales sobre datos sintéticos
```

Consultar [guía de V1](prototipo/README.md), [alcance de V2](docs/REQUISITOS_V2.md), [licencia pendiente](docs/LICENCIA.md) y [registro de cambios](CHANGELOG.md).

## Privacidad y publicación

Este repositorio contiene código y documentación técnica. Excluye documentos personales de tesis, Word, PDF aportados, datos bancarios, credenciales y bases locales. No almacenar información real de víctimas o reportantes en la demostración. La administración debe validar permisos en servidor.

Neon se utilizará desde el servicio autorizado; la URL privada de Postgres no se integra en JavaScript público ni en una APK. GitHub Pages aloja archivos estáticos y no ejecuta por sí mismo un backend. Se confirmará disponibilidad y límites gratuitos antes de desplegar.

## Licencia

La licencia de reutilización está pendiente de elección del autor. No se ha aplicado MIT, Apache ni GPL por inferencia. Las dependencias y datos de terceros mantienen sus propias licencias y atribuciones.
