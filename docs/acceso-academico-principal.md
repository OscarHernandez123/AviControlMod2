# Acceso académico principal sin contraseña

Desde el 21/11/2026 el frontend principal de AWS abre directamente `http://localhost:8088/roles` mediante el túnel SSH existente. El profesor puede elegir Administrador, Nutricionista, Veterinario o Trabajador y cambiar desde el selector «Rol». No hace falta usuario, contraseña ni alternar ventanas InPrivate. Si queda abierto un diálogo Basic anterior, cancelarlo y recargar la página.

Este acceso usa el entorno principal y sus galpones reales de M1. El puerto 8089 y el prefijo HTTPS `/demo/` siguen siendo la copia de pruebas independiente. El frontend académico no muestra el rótulo DEMO ni permite adelantar reintegros simulados.

## Configuración

El modo se compila con `VITE_ACADEMIC_ROLE_SELECTOR=true` (variable Vite o argumento del Dockerfile frontend). `VITE_DEMO_ROLE_SELECTOR` no se activa. El frontend envía el rol de su ruta en `X-Academic-Role` en las peticiones `/api/` al mismo origen; descarta credenciales HTTP antiguas y cabeceras de rol heredadas. El modo DEMO conserva su cabecera y sus controles independientes.

El proxy usa `deploy/aws/academic-access/nginx.academic.conf.template`. Copiar esta plantilla y `render-academic-access.py` en `~/avicontrol-modulo2/academic-access` del servidor y ejecutar `python3 render-academic-access.py`. Se genera `nginx.active.conf` privado (modo 600), leyendo las identidades ya existentes del `.env` principal. No mostrar ese archivo ni ejecutar `nginx -T`: contiene identidades internas. No se copian claves al HTML, JavaScript, imagen ni repositorio.

`deploy/aws/compose.academic.yaml` permite construir el frontend con el modo académico y montar el proxy privado; se combina con el Compose privado. Las rutas del proxy no piden autenticación al navegador. El rol elegido selecciona la identidad interna para conservar las operaciones y su auditoría. La consulta GET de inventario se comparte entre los cuatro roles, permitiendo cargar los medicamentos como Veterinario. Este modo es acceso libre para presentación, no un mecanismo de autenticación de usuarios.

En AWS se conserva la configuración real del proyecto en `~/avicontrol-modulo2/compose.yaml`; solo su servicio `frontend` cambió de imagen y agregó el montaje del proxy académico. El backend, PostgreSQL, perfiles, `.env`, adaptadores REST/Kafka de M1, proxy HTTPS y autorización externa de M3 no cambian. El frontend sigue publicado únicamente en `127.0.0.1:8088`.

## Validación

Checkpoint: `backups/academic-access-20261121-060120` del servidor. Respaldo principal validado con `pg_restore --list`. Se compararon las huellas de todas las tablas de ambas bases: permanecen idénticas. El backend, PostgreSQL, HTTPS y todos los contenedores DEMO no se recrearon. `.env` principal y configuración HTTPS mantienen sus huellas.

Se comprobaron los cuatro roles sin credenciales de navegador, el inventario veterinario HTTP 200, los tres galpones de M1, capacidades asíncronas activas y ApiVersions de Kafka M1 con respuesta correcta, sin publicar eventos. Las APIs HTTPS de M3 principal y DEMO siguen HTTP 200. El resultado DEMO permanece UTILIZADO tras la confirmación real de M3. También se verificó que el bundle académico no contiene contraseñas ni el control de reintegro DEMO.

El equipo M3 confirmó la liquidación completa sobre DEMO y el POST de utilización HTTP 200. La ruta DEMO seguirá disponible para pruebas y presentación. El ciclo conjunto sobre los datos del entorno principal sigue pendiente de preparar con un lote de M1; los registros DEMO no se copian a esa base.