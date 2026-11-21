# Demostración aislada de M2 para probar con M3 sin M1

Estado: desplegada el 20 de noviembre de 2026 en la EC2 existente. No sustituye el entorno principal ni modifica M1 o M3.

## Entrar por el frontend

Abrir `http://localhost:8089` con el túnel DEMO activo. Usar `admin` y la misma contraseña administrativa del entorno principal. La pestaña y una etiqueta visible indican DEMO y datos simulados. Seleccionar Administrador y abrir Galpones.

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
- Las identidades humanas se mantienen para facilitar el uso del frontend; los valores se copiaron al `.env` privado DEMO, no a imágenes ni al repositorio. La identidad interna del proxy M3 coincide en ambas copias; M3 sigue sin enviar credenciales.

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
