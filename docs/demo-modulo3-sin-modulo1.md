# Demostración aislada de M2 para probar con M3 sin M1

Estado: desplegada el 20 de noviembre de 2026 en la EC2 existente. No sustituye el entorno principal ni modifica M1 o M3.

## Entrar por el frontend

Abrir `http://localhost:8089/roles` con el túnel DEMO activo. No se pide usuario ni contraseña. Elegir Administrador, Nutricionista, Veterinario o Trabajador; el selector «Rol DEMO» de la barra superior permite cambiar en la misma ventana. La pestaña y una etiqueta visible indican DEMO y datos simulados.

Para reabrir el túnel desde PowerShell:

```powershell
ssh -o ExitOnForwardFailure=yes -i "$env:USERPROFILE\Downloads\avicontrol-modulo2.pem" -N -L 8089:127.0.0.1:8089 ec2-user@3.21.155.197
```

El acceso principal permanece en localhost:8088. Las acciones hechas en 8089 solo afectan la base DEMO.

Datos iniciales:

| Campo | Valor de demostración |
|---|---|
| Galpón | DEMO - Galpon integracion M3 |
| GalponId | a402a368-cd4d-4e27-8d48-9e0d7eb8d374 |
| Lote | DEMO - Lote integracion M3 |
| LoteId | 7331eec8-e447-4868-bdef-8eca3fff8d44 |
| Estado | PRODUCTIVO |
| Población | 1000 aves simuladas |
| Aforo | 5000 |
| Edad inicial | 45 días al crear la demostración; luego avanza con el calendario |

Los identificadores se reutilizan únicamente en esta copia para que el equipo de M3 pueda probar con el lote ya acordado. El galpón original sigue vaciado y su lote no se reabre en M1. No se precargaron recepciones, consumos, planes nutricionales ni resultados financieros: se registrarán mediante los formularios con datos de prueba acordados.

## Aislamiento

- Proyecto Compose independiente: `avicontrol-m2-demo`.
- Base: `avicontrol_modulo2_demo`, usuario exclusivo de PostgreSQL y contraseña propia aleatoria guardada fuera de Git.
- Volumen: `avicontrol-m2-demo_demo_postgres_data`, distinto de `avicontrol-m2_postgres_data`.
- Backend con perfil `local`; los adaptadores REST y el productor/outbox Kafka de M1 están desactivados. El listener Kafka de M3 también está apagado.
- Backend y PostgreSQL solo están en la red interna de demostración. La interfaz tiene otra red para publicar exclusivamente `127.0.0.1:8089`.
- El contenedor HTTPS existente tiene acceso a la red DEMO para dirigir solo las rutas `/demo/` a `backend-demo`. Las rutas normales conservan su backend original.
- Límite de memoria: backend DEMO 512 MB, PostgreSQL DEMO 128 MB (shared_buffers 32 MB) e interfaz 48 MB. No se crearon otras instancias AWS.
- Las identidades humanas del backend se mantienen como identidades internas del proxy DEMO. Sus credenciales solo están en `.env` y `nginx.roles.active.conf` privados del servidor; no aparecen en el JavaScript, HTML, imagen ni repositorio. La identidad interna del proxy M3 no cambia.

La bodega DEMO tiene una capacidad de prueba de 100000 kg; no es un dato del entorno principal. Los catálogos iniciales son los de desarrollo del perfil local.

## Acceso de M3

Base de pruebas: `https://3.21.155.197/demo/api/integracion/modulo3`.

Ejemplo desde la EC2 autorizada de M3:

```bash
curl -i https://3.21.155.197/demo/api/integracion/modulo3/lotes/7331eec8-e447-4868-bdef-8eca3fff8d44/alimentos
```

Sin usuario ni contraseña; se mantiene HTTPS válido y la autorización por IP 52.15.95.91. Las respuestas llevan `X-Avicontrol-Environment: DEMO`. Las consultas devuelven partidas vacías hasta que se registren datos por los formularios.

La proyección se consulta en `/demo/api/lotes/{loteId}/alimento-requerido`. La confirmación se envía a `/demo/api/integracion/modulo3/resultados-sacrificio/utilizado`. Las respuestas de la API siguen el mismo contrato que el entorno principal; el prefijo `/demo` selecciona la copia de pruebas y se elimina antes de reenviar la solicitud.

M3 debe usar explícitamente esta base para la prueba y tratar sus datos como demostración, no como registros reales. La aparición en sus pantallas depende de que su equipo implemente el cliente REST. No se cambió su código ni su base de datos.

## Archivos de preparación

`deploy/demo-modulo3/compose.yaml`, `Dockerfile.frontend` y `nginx.demo.conf` definen la copia. `index.demo.html` se genera a partir de la imagen frontend existente, cambiando el título e insertando la etiqueta DEMO; es un artefacto de ejecución ignorado por Git.

El servidor conserva el procedimiento `deploy_demo_m3.py` y los archivos de ejecución en `/home/ec2-user/avicontrol-modulo2/demo-modulo3`. Se construyó la interfaz con `docker build` directo porque la versión de Buildx del servidor no satisface `docker compose build`.

`seed-galpon.sql` es una semilla explícita, no una migración Flyway. Rechaza cualquier base distinta de `avicontrol_modulo2_demo`. Solo sustituye el registro automático inicial del simulador si todavía tiene su nombre/código predeterminados y versión cero; un galpón DEMO ya utilizado no se reinicia al volver a ejecutar la semilla.

## Verificaciones

Checkpoint del servidor: `/home/ec2-user/avicontrol-modulo2/backups/demo-m3-20261121-040331` (UTC). Se comprobaron:

- Listado y detalle autenticados del frontend DEMO: HTTP 200, galpón productivo, población 1000 y edad inicial 45 días.
- Capacidades DEMO con `asincrono=false`; perfil local y red interna sin salida del backend hacia M1.
- API HTTPS DEMO de alimentos/medicamentos HTTP 200 y cabecera DEMO. Las rutas reales siguen respondiendo HTTP 200.
- Las 35 tablas del entorno principal mantienen conteos y huellas; sus cuatro contenedores y `.env` no cambiaron. No se recreó PostgreSQL principal.
- Los dos contenedores PostgreSQL utilizan volúmenes diferentes.

La creación de esta copia no demuestra todavía una liquidación de M3: faltan los registros de prueba en formularios y la consulta desde su frontend.

Para retirar la copia, quitar primero las rutas DEMO y su conexión de red del proxy HTTPS y del Compose principal; después detener únicamente el proyecto `avicontrol-m2-demo`. Conservar el volumen si se desea continuar las pruebas. No ejecutar `down -v` sobre el entorno principal ni restaurar su base para borrar datos DEMO.

## Selector de roles sin contraseña (21 de noviembre de 2026)

El selector se activa exclusivamente compilando con `VITE_DEMO_ROLE_SELECTOR=true`. La compilación normal conserva el acceso previo. En DEMO las rutas `/` y `/login` llevan a `/roles`; cada pantalla ofrece el selector en la barra superior. Las peticiones al mismo origen bajo `/api/` llevan `X-Demo-Role`, calculado a partir de la ruta de la pestaña. Se omiten las credenciales HTTP antiguas del navegador, de forma que no hace falta usar ventanas InPrivate ni compartir una cookie entre pestañas de distintos roles.

`nginx.roles.conf.template` acepta exclusivamente los cuatro roles humanos y los traduce a las identidades internas correspondientes. El backend mantiene sus permisos y auditoría por rol. La lectura GET de `/api/inventario` se comparte en el proxy DEMO para permitir que Veterinario cargue el selector de medicamentos; no habilita escrituras administrativas. Una petición API sin rol válido devuelve 400 sin desafío Basic Auth. Este modo de presentación no constituye autenticación: cualquiera con acceso al túnel DEMO puede seleccionar sus roles.

Para renderizar el proxy, copiar `render-demo-roles.py` y `nginx.roles.conf.template` al directorio DEMO del servidor y ejecutar `python3 render-demo-roles.py`. El script genera `nginx.roles.active.conf` con modo 600. No mostrar su contenido ni ejecutar `nginx -T`, porque incluye las identidades internas. Establecer **solo en `.env` DEMO** `AVICONTROL_DEMO_NGINX_CONFIG=./nginx.roles.active.conf`; Compose monta esa configuración privada en el frontend. La configuración predeterminada sigue siendo `nginx.demo.conf` para despliegues sin el selector. Si se rotan las claves DEMO, regenerar el archivo y recrear el frontend para actualizar el montaje.

Construir los assets del frontend con la variable DEMO y copiarlos a la imagen del frontend antes de ejecutar `docker compose up -d --no-deps --no-build frontend-demo`. No basta con reutilizar los assets antiguos de la imagen base. Mantener el título y el aviso de datos simulados. Los archivos privados se montan al ejecutar el contenedor y no se copian a la imagen. Las rutas externas `/demo/api/...` de M3 siguen pasando por el proxy HTTPS existente, sin depender del selector ni de `X-Demo-Role`.

Checkpoint de activación: `backups/demo-roles-20261121-051318` en el servidor. Incluye un respaldo DEMO validado y `verification.json`: cuatro roles HTTP 200 sin claves, inventario veterinario HTTP 200, roles desconocidos rechazados, restricciones del backend conservadas, bundles sin contraseñas, huellas idénticas de todas las tablas de ambas bases, contenedores protegidos sin recrear, configuración principal y de HTTPS sin cambios, rutas HTTPS de M3 principales y DEMO HTTP 200. La imagen anterior se conserva como `avicontrol-m2-frontend:demo-m3-before-roles` para revertir el frontend sin restaurar las bases.
## Adelantar reintegro de un diagnóstico DEMO

En Veterinario → Diagnosticar → Detalle, el botón **Adelantar reintegro DEMO** permite cerrar anticipadamente un diagnóstico activo no sanitario para continuar la prueba de sacrificio productivo el mismo día. El navegador pide confirmación antes de enviar la operación. El botón solo está en la compilación con `VITE_DEMO_ROLE_SELECTOR=true`.

El endpoint `POST /api/demo/diagnosticos/{id}/adelantar-reintegro` requiere Veterinario, perfil `local` (sin `modulo1-rest`) y `avicontrol.demo.enabled=true`; está desactivado por defecto. Además verifica que la base sea exactamente `avicontrol_modulo2_demo`. Usa explícitamente el adaptador local y no publica eventos ni llama a M1/M3. La configuración HTTPS pública de M3 no expone esta ruta.

La operación bloquea el diagnóstico y valida que el mismo lote continúe aislado y no tenga otro diagnóstico activo. En una sola transacción cambia el galpón local a Productivo, resuelve el diagnóstico como Reintegrado con la fecha actual y agrega en sus observaciones una marca DEMO con motivo, usuario y hora. Conserva la fecha prevista de reintegro, prescripción, lote y todas las recepciones, suministros y consumos. Un reintento sobre un diagnóstico ya reintegrado devuelve su estado sin repetir el cambio. Cualquier fallo revierte todos los cambios de esa transacción. No se usa para sacrificios sanitarios.

Solo `.env` de la copia DEMO activa `AVICONTROL_DEMO_ENABLED=true` y selecciona la nueva imagen mediante `AVICONTROL_DEMO_BACKEND_IMAGE`. El Compose normal no cambia. No hay migraciones nuevas ni se modifican las existentes.

Antes del despliegue se validó el respaldo `backups/demo-reintegro-20261121-053016` del servidor. Verificación local: `spotlessApply check bootJar` sin errores; 240 pruebas declaradas, 219 ejecutadas correctamente y 21 omitidas por sus condiciones. Los casos nuevos comprueban la protección de la base principal, la activación exclusiva, la conservación de la prescripción/fecha prevista, los reintentos y los conflictos de lote/diagnóstico.

## Entorno principal y DEMO: estado de integración

La aplicación principal se abre por el túnel `http://localhost:8088`; usa los adaptadores REST/Kafka de M1. Verificación del 21/11/2026: listado REST HTTP 200 con los tres galpones de M1 y capacidades asíncronas activas. La API real para M3 es `https://3.21.155.197/api/integracion/modulo3`.

El puerto `http://localhost:8089` y el prefijo HTTPS `/demo/` usan otra base y un simulador local de galpones. Sus registros no se copian ni sincronizan al entorno principal. Se crearon para ejecutar pruebas sin disponibilidad de M1. Aunque se reutilizó el UUID del lote para coordinar con M3, la población, las recepciones, los consumos y el resultado DEMO son ficticios e independientes del lote real.

M3 confirmó desde 52.15.95.91 las partidas DEMO: 40 kg a 2500 y 20 kg a 3000, neto 160000 e impuesto de tramos 8000; 100 ml a 50, neto 5000 e impuesto de tramo 250. El resultado final se guardó por frontend con 1000 aves y 2500 kg. Quedan pendientes la liquidación en su frontend y la confirmación POST de utilización; la confirmación de consultas no demuestra esos dos pasos.

Publicar el código en `main` no copia las bases ni cambia automáticamente los contenedores de AWS. El selector libre y el reintegro anticipado permanecen desactivados en la configuración normal. La validación conjunta real de los tres módulos requiere registros reales para un mismo lote y que M3 use la base sin `/demo/`.

El entorno principal incorpora acceso académico sin contraseña desde el 21/11/2026; ver [acceso académico principal](acceso-academico-principal.md). Su selector muestra «Rol» y conserva la conexión con M1. El selector «Rol DEMO» y los controles simulados permanecen en la copia independiente.

