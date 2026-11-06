# Verificación demo.4 — 5 de noviembre de 2026

- Web: TypeScript y Vite completados, 272 módulos.
- Pruebas unitarias: 15 aprobadas, incluidas conservación de referencia/desvío, proveedor indisponible y ausencia de sectores cruzados.
- Integración contra API desplegada: 20 comprobaciones aprobadas. Identidad, aislamiento de Mis reportes, moderación, idempotencia, carga ficticia, consultas geográficas y persistencia se mantuvieron operativos.
- API `/health`: versión demo.4 confirmada antes de registrar los desvíos finales. Se incorporó esta comprobación porque una respuesta inmediata tras desplegar aún podía provenir de la versión anterior.
- Escenario principal: cuatro candidatos; tres cruzan algún círculo ficticio y uno no cruza ninguno. La comprobación geométrica se aplica a todos los círculos activos. Datos puntuales en `desvios_demo4.json`.
- Android: `BUILD SUCCESSFUL in 13s` después del rediseño final. No se instaló esta revisión: el dispositivo dejó de estar disponible y el usuario indicó que la levantaría por su cuenta. La instalación confirmada de demo.3 no se presenta como instalación de demo.4.
- Emblema: archivo separado con alfa de esquina 0, comprobado leyendo el PNG. Original conservado. La herramienta generativa puede introducir variaciones del dibujo.

La revisión visual interactiva de navegador y la prueba de uso completa en Android siguen pendientes. Estos resultados técnicos no son pruebas de precisión del riesgo, usabilidad formal, rendimiento ni seguridad de los barrios referenciados.
