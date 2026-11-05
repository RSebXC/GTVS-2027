# Alcance acordado para GTVS V2

Actualización: 5 de noviembre de 2026. Esta especificación reúne decisiones del estudiante; no acredita que todas las funciones estén implementadas. El asesor todavía no aprobó el alcance web y móvil. El capítulo 5 se actualizará al cerrar la versión comprobada.

## Funciones prioritarias

1. Consultar rutas sin sesión, para automóvil y dentro del municipio de Guatemala; escoger origen y destino por búsqueda o mapa. Mostrar recorrido y tres perfiles, sin navegación paso a paso.
2. Crear cuenta para registrar incidentes contextuales, obstrucciones o cierres. Correo/contraseña y Google previstos. Fotografías, corroboraciones y funcionamiento sin conexión quedan fuera.
3. Un administrador inicial revisa, corrige, rechaza o vincula reportes antes de su publicación. No se requiere pantalla de seguimiento del reportante en esta versión.
4. Los incidentes aprobados influyen en el cálculo demostrativo; la acumulación debe ser visible sin contar varias evidencias del mismo hecho como incidentes distintos.
5. Web y Android comparten API y persistencia; la app no contiene credenciales de base ni un backend público dentro de la APK.

## Entorno y alcance

Presupuesto Q0; no contratar servicios ni habilitar gasto por uso. Demostración pública, datos ficticios claramente rotulados. Pruebas iniciales del estudiante en su navegador y Xiaomi, sin afirmar evaluación con terceros. Android nativo Kotlin, APK de instalación manual y proyecto abrible en Android Studio. Idioma inicial español con estructura que permita traducción.

Un mapa geográfico real puede mostrar geometría real con eventos ficticios de demostración. Los símbolos deben indicar esa condición de forma persistente; no representan acusaciones sobre personas o comunidades. La alternativa abstracta de V1 se conserva para pruebas controladas.

## Riesgo y restricciones administrativas

Los incidentes temporales pierden influencia según configuración demostrativa. Una restricción administrativa persistente se representa aparte del índice, con motivo, geometría, vigencia o revisión requerida, autor y posibilidad de retiro. No se crea una etiqueta permanente «zona mala». La falta de incidentes no demuestra ausencia de riesgo.

## Servicios externos

Neon: proyecto y organización indicados por el propietario, rama production para la conexión solicitada. No resetear bases ni cambiar planes. GitHub Pages es candidato para frontend estático; requiere resolver el servicio de API/autenticación accesible desde Internet. Google OAuth necesita configuración efectiva del proveedor; no se mostrará como disponible si no funciona.

Las consultas de direcciones y rutas pueden transmitir ubicaciones a proveedores externos, autorizado por el estudiante. No activar localización sin acción y permiso del usuario. No guardar trayectos por defecto. Verificar límites gratuitos y atribuciones de cada proveedor antes de publicar.

## Apariencia

Tema claro moderno, mapa como superficie principal, panel inferior en móvil y lateral en escritorio. Azul y verde como base; rutas diferenciadas, leyenda legible y aviso de demostración. Las imágenes aportadas sirven de referencia visual, sin copiar logos o interfaces ajenos. Se propone símbolo vectorial propio para evitar texto deformado y asegurar legibilidad en tamaños pequeños.

## Estado

Este archivo es especificación. La evidencia de implementación y los pendientes se registran por versión en CHANGELOG y documentación de pruebas.
