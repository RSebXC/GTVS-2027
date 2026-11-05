# Corrección de sesión y revisión de interfaz — 2026-11-05

El usuario informó que podía consultar rutas, pero después de acceder con Google o correo aparecía un error de token y el botón seguía ofreciendo iniciar sesión. Aportó una captura del prototipo ejecutado en su teléfono. Esa captura es evidencia comunicada por el usuario sobre la versión anterior, no una prueba presencial realizada por el desarrollador.

## Reproducción y causa

Se ejecutó `scripts/diagnose-sdk.ts` con `@neon/auth` 0.8.2 y una cuenta ficticia. El método `token()` devolvió `data` con claves `session,user`, sin `data.token`. El código del paquete muestra que `/token` comparte el tratamiento y la caché de `getSession`. La sesión sí contenía un JWT en `data.session.token`; al enviarlo a la API desplegada, `/api/me` respondió HTTP 200.

La implementación anterior dejaba el usuario en memoria, pero consultaba los permisos antes de actualizar la interfaz. Al fallar el token, el botón conservaba el estado anterior. Ambos defectos se corrigieron: se usa el JWT de la sesión del SDK y se actualiza el estado visible independientemente de la consulta del rol. Las peticiones públicas ya no solicitan tokens.

## Comprobaciones

`npm run build`: TypeScript y Vite completados. `npm test`: once pruebas aprobadas, incluidas dos regresiones del formato de sesión. Con el SDK real y API desplegada se comprobaron alta, recuperación de sesión, JWT aceptado, salida, nuevo ingreso y envío de un reporte desde una cuenta cuyo correo permanecía sin verificar. El reporte ficticio se retiró al finalizar. Las cuentas ficticias de Auth permanecen; no se imprimieron contraseñas ni tokens.

La compilación Android después de introducir mapa completo y panel inferior terminó con `BUILD SUCCESSFUL in 13s`. El APK debe reinstalarse para ver cambios nativos. No se realizó una nueva instalación en el teléfono ni una revisión visual interactiva completa. Google necesita una nueva comprobación del usuario: comparte la extracción corregida de JWT, pero su consentimiento OAuth no se automatizó.

## Cambios visuales y alcance

Web: modo claro/oscuro, tarjetas para tipos ya admitidos, estado de cuenta y panel inferior móvil ajustable. Android: mapa a pantalla completa con cabecera compacta, panel inferior ajustable, tipografía sans-serif explícita y conservación del formulario cuando se marca su ubicación.

Los mockups recibidos son referencias de interacción. No se incorporaron patrullajes, puntuaciones de reputación, verificaciones comunitarias inventadas, voz, tráfico en vivo ni etiquetas «zona segura». No hay mapa de calor validado. Los círculos de influencia corresponden a incidentes ficticios efectivamente aprobados.
