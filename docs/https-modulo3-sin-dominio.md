# HTTPS de M2 para M3 sin dominio

Estado: operativo desde el 20 de noviembre de 2026. URL del servidor: `https://3.21.155.197`. Certificado público de Let's Encrypt para la IP, validado sin desactivar la comprobación TLS. No se modificó código de M1 ni M3.

## Acceso y primera prueba

El servicio `modulo3-https` permite las rutas de integración desde la IP de salida de M3 `52.15.95.91/32`. También permite loopback y la puerta de enlace interna de Docker para las verificaciones locales de M2. Utiliza la dirección real de la conexión; una cabecera X-Forwarded-For del cliente no concede acceso.

Desde la EC2 de M3:

```bash
curl -i --user modulo3_consulta \
  https://3.21.155.197/api/integracion/modulo3/lotes/7331eec8-e447-4868-bdef-8eca3fff8d44/alimentos
```

Curl solicita la contraseña sin incluirla en el comando. Entregarla al equipo por un canal privado; no guardarla en Git. Las credenciales siguen almacenadas en `.env` de M2, con permisos 600. No usar `-k` ni desactivar la verificación de certificados.

Resultado esperado con la cuenta correcta: HTTP 200 y `{loteId, partidas: []}` mientras el lote no tenga suministros registrados. Un 401 indica que falta autenticación válida. Un 403 desde otro equipo puede ser el bloqueo por IP; confirmar que la salida real de M3 sea 52.15.95.91. No hay interfaz web pública en la raíz: la interfaz administrativa conserva el túnel a localhost:8088.

Rutas publicadas:

- `/api/integracion/modulo3/`: GET de consultas y POST de confirmación de uso. Spring Security conserva los permisos del contrato.
- `/api/lotes/{loteId}/alimento-requerido`: GET de proyección.
- `/api/ordenes-sacrificio/{ordenId}/resultados`: GET de compatibilidad por orden.

El resto de rutas no se publica por esta entrada. El puerto 80 sirve exclusivamente `/.well-known/acme-challenge/`, necesario para emitir y renovar el certificado; no sirve la API por HTTP. El puerto 443 sirve TLS 1.2/1.3 con Basic Auth y control de origen. No se publican PostgreSQL ni el puerto 8080 del backend.

## Archivos y montaje

`deploy/aws/modulo3-https` contiene las plantillas Nginx, el fragmento de servicio Compose y las unidades de renovación. Mantiene LF también al clonar en Windows. Los certificados, claves y archivos de ejecución están excluidos de Git.

Los marcadores de las plantillas se sustituyen por `M2_PUBLIC_IP`, `M3_ALLOWED_IP`, `DOCKER_GATEWAY` y, para Compose, `HTTPS_IMAGE`. La puerta de enlace se obtiene de `docker network inspect avicontrol-m2_default`. Se reutiliza la imagen frontend ya desplegada como servidor Nginx, con una configuración independiente; no se recrean los contenedores base.

Directorio de ejecución: `/home/ec2-user/avicontrol-modulo2/https-modulo3`. La configuración activa se monta como archivo de solo lectura en el contenedor. Al actualizarla, mantener el mismo archivo/inodo, comprobar `docker compose exec -T modulo3-https nginx -t` y recargar con `nginx -s reload`. El resolver de Docker actualiza la dirección del backend si éste se recrea en otro despliegue.

La emisión se hizo con la imagen oficial `certbot/certbot:v5.4.0`, perfil `shortlived` y autenticación webroot. Se comprobó primero la ruta pública del desafío y una emisión simulada antes de pedir el certificado real. Las claves privadas y la cuenta ACME permanecen exclusivamente en el servidor. El certificado actual incluye SAN `IP Address:3.21.155.197` y vence el 27 de noviembre de 2026 a las 17:37:39 UTC; las renovaciones actualizan esa fecha.

## Renovación automática

Los certificados IP de Let's Encrypt utilizan el perfil de corta duración; por eso la renovación es obligatoria. El servicio consulta cada seis horas, con hasta quince minutos de variación del temporizador. No fuerza una emisión en cada ejecución: Certbot renueva cuando corresponde.

- Unidad: `avicontrol-m3-https-renew.service`.
- Temporizador: `avicontrol-m3-https-renew.timer`, habilitado y activo.
- Script: `https-modulo3/renew.sh`, ejecutado por root.
- Certbot utiliza un contenedor temporal limitado a 256 MB y un bloqueo para evitar ejecuciones simultáneas.
- Cuando renueva, crea una marca; el script valida Nginx y recarga solo el servidor HTTPS. No reinicia backend, frontend ni PostgreSQL.
- El script desactiva la pausa aleatoria propia de Certbot porque el temporizador ya introduce variación. No se consulta Kafka para emitir o renovar.

Comprobaciones de operación:

```bash
sudo systemctl list-timers avicontrol-m3-https-renew.timer
sudo systemctl show avicontrol-m3-https-renew.service -p Result -p ExecMainStatus
sudo journalctl -u avicontrol-m3-https-renew.service --since '2 days ago'
```

La simulación `renew --dry-run --run-deploy-hooks` pasó y el servicio systemd se ejecutó con `Result=success` y `ExecMainStatus=0`.

## Datos, verificación y reversión

Checkpoint: `/home/ec2-user/avicontrol-modulo2/backups/m3-https-20261121-023523` (fecha UTC). Contiene el respaldo pg_dump validado, la configuración Compose anterior, las huellas de tablas y `verification.json`. También hay copia del dump y del informe en `build/aws-deploy/m3-https-20261121-023523`, fuera de Git.

Se verificó:

- Las 35 tablas públicas mantienen conteos y huellas de contenido.
- Backend, frontend y PostgreSQL conservan sus contenedores. El archivo `.env` mantiene la misma huella.
- HTTPS valida cadena y nombre/IP con el almacén de confianza del sistema, sin excepciones TLS.
- Desde un origen local permitido: GET M3 200, sin credenciales 401 y POST con resultado inexistente 404, sin alterar un resultado real.
- Desde la IP externa del equipo de M2: TLS válido y GET rechazado con 403 por el filtro de origen.
- La renovación simulada, la recarga y el servicio programado funcionan.

La prueba desde la EC2 de M3 corresponde a su equipo; todavía no se afirma que su aplicación ya consuma M2 ni que su liquidación esté lista. Los datos de prueba pendientes se registran mediante los formularios y casos de uso, no insertando filas directamente.

Para retirar únicamente HTTPS: deshabilitar el temporizador, detener y quitar solo el servicio `modulo3-https` y restaurar la copia Compose del checkpoint. Conservar los respaldos y certificados para recuperación. No ejecutar `docker compose down -v` ni restaurar destructivamente la base de datos para revertir una entrada HTTPS.

## Fuentes

- [Certificados de IP en Certbot y perfil shortlived](https://letsencrypt.org/2026/03/11/shorter-certs-certbot/).
- [Renovación y deploy hooks de Certbot](https://eff-certbot.readthedocs.io/en/stable/using.html#renewing-certificates).
