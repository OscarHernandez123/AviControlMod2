# Acceso académico al entorno principal

El proyecto mantiene un único entorno desplegable: el principal, conectado con M1 y M3. Se abre en `http://localhost:8088/roles` mediante el túnel SSH existente. Elegir Administrador, Nutricionista, Veterinario o Trabajador; el selector «Rol» permite cambiar en la misma ventana sin usuario ni contraseña. Si queda abierto un diálogo Basic anterior, cancelarlo y recargar.

## Conexiones

- M1 proporciona galpones y lotes mediante REST; las operaciones asíncronas conservan Kafka y la configuración existente.
- M3 consulta `https://3.21.155.197/api/integracion/modulo3` y confirma por REST la utilización del resultado de sacrificio.
- Recepciones, suministros, consumos y resultados deben corresponder al lote de M1 usado durante la prueba principal.

## Configuración

Compilar con `VITE_ACADEMIC_ROLE_SELECTOR=true` (variable Vite o argumento del Dockerfile frontend). `roleAccess.js` envía `X-Academic-Role` a `/api/` en el mismo origen según la ruta de la pestaña, omitiendo credenciales HTTP antiguas. `RoleSelector.jsx` muestra los cuatro roles.

El proxy usa `deploy/aws/academic-access/nginx.academic.conf.template`. Copiar esta plantilla y `render-academic-access.py` en `~/avicontrol-modulo2/academic-access` del servidor y ejecutar `python3 render-academic-access.py`. Se genera `nginx.active.conf` privado (modo 600), leyendo las identidades existentes de `.env`. No mostrar ese archivo ni ejecutar `nginx -T`: contiene identidades internas. Las claves no se incluyen en HTML, JavaScript, imagen ni repositorio.

`deploy/aws/compose.academic.yaml` permite construir el frontend académico y montar el proxy privado; se combina con el Compose privado. El navegador elige el rol sin autenticarse; el proxy usa la identidad interna correspondiente para mantener operaciones y auditoría. La lectura GET de inventario se comparte para que Veterinario pueda cargar los medicamentos. El frontend sigue publicado únicamente en `127.0.0.1:8088`.

El backend, PostgreSQL, perfiles y adaptadores REST/Kafka de M1, proxy HTTPS y autorización externa de M3 conservan su configuración. Los perfiles locales y fixtures existentes se mantienen para las pruebas automatizadas; no definen un segundo entorno desplegado.

## Validación y datos

Checkpoint del acceso académico: `backups/academic-access-20261121-060120` del servidor. Respaldo principal validado con `pg_restore --list`; datos y contenedores protegidos conservados. Se verificaron los cuatro roles sin claves, el inventario veterinario, los tres galpones M1, capacidades asíncronas, ApiVersions Kafka y rutas HTTPS M3.

La liquidación y confirmación REST de M3 se validaron previamente con datos simulados. Para la presentación principal falta coordinar con M1 un lote apto y registrar su ciclo desde las pantallas. Los registros de la prueba anterior no se migran a la base principal.

La copia de pruebas ya existente en AWS se conserva temporalmente fuera del despliegue definido por este repositorio, porque M3 todavía la utiliza. Su retirada debe coordinarse después de cambiar M3 a la API principal y conservar sus respaldos. Esta limpieza de ramas no borra bases, volúmenes, certificados ni datos de AWS.
