# Despliegue inicial del Módulo 2 en AWS

## Alcance y arquitectura preparada

Configuración para un ambiente de pruebas en EC2 Linux x86_64: Docker Compose ejecuta backend Java 21, frontend compilado con Nginx, PostgreSQL 16 y Caddy para HTTPS. No instala ni despliega el Módulo 1 ni otro broker Kafka. REST y Kafka se consumen del entorno que entregue su equipo.

Una instancia en una subred pública, con ruta a Internet Gateway y Elastic IP asociada, proporciona la IPv4 de salida a autorizar si el tráfico hacia Módulo 1 sale directamente a internet. Si se introduce NAT, VPN, proxy u otra ruta, debe verificarse la IP realmente observada por el destino. La IP local 191.95.16.24 no será la del despliegue.

Este primer entorno no tiene alta disponibilidad: la instancia, el volumen PostgreSQL y los certificados son puntos únicos. Los volúmenes Docker sobreviven a recrear contenedores, pero no sustituyen una copia externa ni protegen ante eliminar el volumen EC2. Antes de producción: acordar respaldo/restauración, identidad compartida, monitoreo y evaluar RDS y ECS según presupuesto. EC2, EBS, IPv4 pública y transferencia pueden generar costos; no se presupone gratuidad.

## Archivos

- Dockerfile raíz: compila el backend y ejecuta el JAR como usuario sin privilegios.
- frontend/Dockerfile: compila React; no ejecuta Vite en el servidor.
- frontend/nginx.conf: SPA y proxy de API con autenticación HTTP Basic comprobada por el backend.
- src/main/resources/application-aws.properties: perfil sin contraseñas ni conexión de BD de desarrollo por defecto; Swagger deshabilitado.
- deploy/aws/compose.yaml: servicios y volúmenes; solo publica 80 y 443. PostgreSQL y el backend no publican sus puertos al host.
- deploy/aws/Caddyfile: HTTPS automático para el dominio configurado.
- deploy/aws/.env.example: plantilla, sin secretos reales. La copia .env está ignorada por Git.

## Información necesaria de tu cuenta AWS

| Dato o acceso | Para qué se necesita |
|---|---|
| Tipo de cuenta: personal, educativa o laboratorio | Saber restricciones de servicios, regiones, IAM y vigencia de credenciales |
| Región autorizada | Ubicar EC2, EBS, red e IP fija; preferir compatibilidad con la red del Módulo 1 |
| Presupuesto máximo mensual y duración de las pruebas | Elegir capacidad y definir apagado y retención de datos; establecer alertas de presupuesto |
| Perfil AWS CLI autenticado y rol disponible | Ejecutar el despliegue; usar SSO/credenciales temporales cuando la cuenta lo permita |
| VPC y subred pública existentes o permiso para crearlas | Ruta al Internet Gateway y conectividad a REST/Kafka |
| Permisos para EC2, EBS, Security Groups y Elastic IP | Crear servidor, almacenamiento y dirección fija; iam:PassRole limitado al rol aprobado si aplica |
| Rol de instancia y acceso Systems Manager, o método administrativo aprobado | Administrar sin abrir SSH a internet; SSM necesita agente y conectividad a sus endpoints |
| Dominio o subdominio y capacidad de modificar DNS | Crear registro A a la Elastic IP y obtener certificado HTTPS |
| Personas/roles que harán pruebas | Crear contraseñas propias de administrador, veterinario, trabajador y nutricionista |

No enviar claves secretas, contraseñas de AWS ni llaves privadas por chat o Git. La autenticación de AWS debe hacerse en tu equipo. Si la cuenta tiene IAM Identity Center, el punto de partida es:

```powershell
aws configure sso --profile avicontrol
aws sso login --profile avicontrol
aws sts get-caller-identity --profile avicontrol
```

No asumir SSO disponible en un laboratorio: puede proporcionar su propio perfil temporal y un rol predefinido. Compartir únicamente el nombre del perfil, región y las restricciones conocidas.

## Información necesaria del equipo del Módulo 1

1. URL base REST completa y accesible desde AWS, con http/https y puerto, sin añadir /api/v1. Confirmar si usa autenticación. El cliente actual no envía credenciales REST: si las exige, hay que incorporarlas antes de conectar.
2. bootstrap.servers con los hosts y puertos reales; todos los brokers anunciados por advertised.listeners deben ser resolubles y accesibles desde EC2. Autorizar solo el bootstrap no basta si Kafka anuncia otros nodos.
3. Protocolo Kafka exacto: PLAINTEXT, SSL, SASL_PLAINTEXT o SASL_SSL; mecanismo SASL si aplica, credenciales y certificados por un canal seguro. La plantilla muestra SASL_SSL/SCRAM como ejemplo, no como acuerdo confirmado. Para mTLS/CA privada se deberán montar certificados y completar las propiedades estándar. Para MSK IAM se requiere además el autenticador correspondiente; esta plantilla no lo instala.
4. Confirmar los tres tópicos del contrato y permisos de producción sobre ellos; el consumidor grupo modulo-1 pertenece a su equipo. Confirmar los eventType y versión 1 ya probados.
5. Si la conexión será pública o privada mediante VPN/red compartida. En el segundo caso hacen falta rutas, DNS y reglas adicionales; una Elastic IP no da acceso a servicios privados.
6. Autorizar la IP de salida definitiva de AWS para REST y Kafka. Compartirla después de asignarla y comprobarla.
7. UUID de galpones/lotes y datos autorizados para pruebas, responsables y horario. No ejecutar los scripts que crean datos de demostración sobre producción.
8. Decisión sobre confirmación/rechazo de negocio y recuperación de eventos expirados. Publicado confirma el ACK de Kafka; no aceptación del Módulo 1. El sacrificio sanitario desde AISLAMIENTO sigue fuera del contrato.

## Preparación y arranque en EC2

No se han creado recursos en AWS. Antes de hacerlo hay que concretar región, presupuesto, permisos y conectividad. Con esos datos:

1. Crear o seleccionar la red, una instancia Linux x86_64 con almacenamiento persistente cifrado y capacidad para construir Java y Node. Dimensionar en función del presupuesto y la memoria de compilación; no elegir una micro por defecto.
2. Asociar una Elastic IP; usar acceso administrativo SSM si está disponible. Security Group: HTTPS 443 para quienes probarán; 80 para redirección y validación del certificado. No abrir 5432, 8080 ni Kafka en esta instancia. Si SSM no está permitido, limitar SSH al origen administrativo aprobado.
3. Instalar Docker Engine y Compose v2 siguiendo la documentación de la distribución elegida. Obtener una copia del repositorio y la revisión que se va a desplegar. El commit local debe estar disponible en el remoto o transferirse de forma aprobada; esta preparación no hace push.
4. Crear el registro DNS A del subdominio hacia la Elastic IP. No crear AAAA salvo que IPv6 esté realmente configurado. Para HTTPS público, Caddy debe recibir los desafíos de certificado por 80/443.
5. Copiar la plantilla y editarla únicamente en el servidor:

```bash
cp deploy/aws/.env.example deploy/aws/.env
chmod 600 deploy/aws/.env
```

Sustituir todos los marcadores por valores reales. Usar contraseñas distintas y robustas. Ajustar las propiedades Kafka al mecanismo acordado; no enviar contraseñas en argumentos del proceso ni registrar el contenido de .env. Para valores con $, comillas o punto y coma, usar el formato literal de Compose .env (comillas simples alrededor del valor completo cuando corresponda).

6. Comprobar configuración y construir sin imprimir secretos:

```bash
docker compose --env-file deploy/aws/.env -f deploy/aws/compose.yaml config --quiet
docker compose --env-file deploy/aws/.env -f deploy/aws/compose.yaml build
docker compose --env-file deploy/aws/.env -f deploy/aws/compose.yaml up -d
docker compose --env-file deploy/aws/.env -f deploy/aws/compose.yaml ps
docker compose --env-file deploy/aws/.env -f deploy/aws/compose.yaml logs --tail=100 backend https
```

Flyway aplica V22/V23 y las migraciones anteriores sobre la BD del Módulo 2. Las migraciones históricas aún crean tablas de simulación, pero el perfil remoto no las utiliza como fuente de galpones. No ejecutar down -v: eliminaría los datos y certificados persistidos.

7. Verificar HTTPS, autenticación, listado y detalle de galpones; después publicar una mortalidad acordada de prueba y confirmar con el otro equipo su aplicación y el estado de la outbox. Comprobar reinicio sin pérdida de eventos y respaldo/restauración antes de ampliar el uso.

## Acceso al frontend y límite de identidad

En este despliegue el navegador solicita primero usuario y contraseña HTTP Basic, verificados por el backend a través de Nginx. Las peticiones de API conservan esa identidad; no se inyectan las credenciales de demostración de Vite por ruta. Probar que sin credenciales responde 401 y que un trabajador no puede ejecutar acciones de administrador.

La pantalla de login/selector de roles de React continúa siendo de demostración: elegir un rol allí no concede permisos. Elegir el rol correspondiente a las credenciales usadas. Para cambiar de identidad puede ser necesario un perfil de navegador separado o cerrar la sesión Basic almacenada por el navegador. Antes de una publicación de producción se debe reemplazar esa UX por el módulo de identidad compartido.

El catálogo de medicamentos/alimentos aún incluye el adaptador de demostración y los parámetros de bodega deben completarse para utilizar esas pantallas. Las confirmaciones de negocio de Kafka siguen pendientes del acuerdo, como se documenta en integracion-modulo1-kafka.md.

## Operación mínima

Respaldar PostgreSQL con pg_dump a un destino externo protegido y probar la restauración. No guardar dumps ni .env en Git. Definir retención de logs, actualizaciones, alertas de disponibilidad y presupuesto. Antes de reemplazar una imagen, registrar su revisión y considerar compatibilidad de las migraciones: revertir la imagen no revierte la BD automáticamente.

No se incluye todavía una plantilla de aprovisionamiento de red/cuenta: se cerrará con los identificadores y permisos reales para no asumir una VPC, región o rol inexistentes.

## Fuentes técnicas

- [Elastic IP y cobro de IPv4 pública](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/elastic-ip-addresses-eip.html).
- [Conectividad de VPC e Internet Gateway](https://docs.aws.amazon.com/vpc/latest/userguide/how-it-works.html).
- [AWS CLI con IAM Identity Center](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-sso.html).
- [Rol de instancia para Systems Manager](https://docs.aws.amazon.com/us_en/systems-manager/latest/userguide/setup-instance-permissions.html).
- [Autorización Nginx mediante subrequest](https://nginx.org/en/docs/http/ngx_http_auth_request_module.html).
- [Requisitos de HTTPS automático en Caddy](https://caddyserver.com/docs/automatic-https).

## Comprobaciones de esta preparación

Se construyeron localmente ambas imágenes Docker. El backend arrancó con aws,modulo1-rest contra una base PostgreSQL temporal; Nginx devolvió 401 sin credenciales, 200 para la SPA autenticada y 403 al trabajador que solicitó una ruta de administrador. Se validaron Compose y la adaptación del Caddyfile sin solicitar certificados. Los contenedores y datos temporales de esas comprobaciones se retiraron.

Durante la preparación local todavía no se había verificado el entorno remoto ni se habían creado recursos AWS. La evidencia local está en build/aws-check/resultados.json. El despliegue posterior y sus comprobaciones se documentan a continuación; no se emitieron certificados para un dominio real.

## Entorno de pruebas desplegado en EC2 — 19 de noviembre de 2026

Instancia existente `i-04e068ee540cb28cf`, región `us-east-2`, Amazon Linux 2023, `t3.small`, Elastic IP `3.21.155.197`, asociada por el usuario desde la consola AWS. Reemplaza a la IPv4 automática `18.220.161.224`. Asignación: `eipalloc-0e04d72c19d6dc51a`. No se cambió el plan de la cuenta.

Docker y Compose están instalados. Los servicios backend, frontend y PostgreSQL se ejecutan desde `/home/ec2-user/avicontrol-modulo2/compose.yaml`, copia de `deploy/aws/compose.private.yaml`. Se cargaron las imágenes `avicontrol-m2-backend:aws-prep` y `avicontrol-m2-frontend:aws-prep`; PostgreSQL usa un volumen persistente. Flyway aplicó correctamente las 23 migraciones del Módulo 2.

El archivo `.env` reside exclusivamente en EC2, con permisos 600 y contraseñas aleatorias distintas. Los valores de conexión son:

```properties
AVICONTROL_MODULO1_BASE_URL=http://18.227.58.143
SPRING_KAFKA_BOOTSTRAP_SERVERS=18.227.58.143:9092
SPRING_KAFKA_PROPERTIES_SECURITY_PROTOCOL=PLAINTEXT
```

La URL configurada omite `/api/v1` porque el cliente REST lo agrega. El equipo del Módulo 1 confirmó que no requiere autenticación REST ni autenticación/certificados Kafka.

### Acceso sin dominio mediante túnel SSH

El frontend escucha únicamente en `127.0.0.1:8088` de EC2. Abrir otra ventana PowerShell local y mantener este comando ejecutándose:

```powershell
ssh -o ExitOnForwardFailure=yes -i "$env:USERPROFILE\Downloads\avicontrol-modulo2.pem" -N -L 8088:127.0.0.1:8088 ec2-user@3.21.155.197
```

Abrir `http://localhost:8088` en el navegador. El tráfico atraviesa el túnel SSH cifrado. No hay publicación web directa por HTTP ni HTTPS en esta modalidad. Usuario inicial: `admin`. Consultar la contraseña únicamente en la sesión SSH propia, sin pegarla en chats ni guardarla en Git:

```bash
grep '^AVICONTROL_ADMIN_PASSWORD=' ~/avicontrol-modulo2/.env
```

En la pantalla de demostración de React seleccionar el rol correspondiente a la identidad Basic. Para operar como trabajador o veterinario, utilizar sus credenciales respectivas en un perfil de navegador independiente.

### Resultado de las comprobaciones

- Frontend sin credenciales: HTTP 401; autenticado como administrador: HTTP 200.
- Capacidades: `asincrono=true`, `sacrificioSanitario=false`.
- Listado y detalle de galpones: consultas exitosas desde el Módulo 2 desplegado hacia el Módulo 1.
- Galpón autorizado `a402a368-cd4d-4e27-8d48-9e0d7eb8d374`, lote `7331eec8-e447-4868-bdef-8eca3fff8d44`: PRODUCTIVO, población 1975.
- Conexión TCP desde EC2 hacia `18.227.58.143:9092`: dos comprobaciones agotaron el tiempo de espera (salida 124 de timeout). Estas comprobaciones fueron realizadas desde la IP anterior `18.220.161.224`. La nueva salida verificada es `3.21.155.197`; se solicitó su autorización y falta confirmación del equipo. Falta que el equipo revise el puerto, reglas de entrada/firewall y escucha del broker. Después se verificará que los brokers anunciados también sean accesibles.
- Outbox: cero eventos. No se publicó mortalidad, aislamiento, reanudación ni vaciado en este entorno. No se modificó código del Módulo 1.

Antes de la primera publicación, avisar al equipo del Módulo 1 y acordar una mortalidad pequeña. Probar aislamiento y después reanudación. El vaciado queda pendiente: el otro equipo debe colocar el galpón EN_COSECHA y confirmar la ventana de prueba; desvincula el lote.

Para revisar el despliegue en la sesión SSH:

```bash
cd ~/avicontrol-modulo2
docker compose ps
docker compose logs --tail=100 backend
```
Comprobación posterior a la asociación de Elastic IP: salida pública 3.21.155.197, REST HTTP 200 con población 1975, tres servicios activos y cero registros en la outbox. El usuario confirmó acceso al frontend mediante el nuevo túnel. No se publicaron eventos.

### Corrección de nombres de lote en Galpones

El Módulo 2 conserva ahora el nombre recibido en loteActivo.nombre del Módulo 1 y lo expone como loteActual.nombre en el listado y el detalle propios. La tabla administrativa y el panel de detalle muestran ese nombre; si está ausente o vacío, muestran el identificador. Un lote nulo se presenta como «Sin lote actual». Como el listado remoto omite el lote, se consulta el detalle de los galpones de la página visible cuando falta esa información. No hay migraciones de base de datos.

La actualización usa imágenes avicontrol-m2-backend:lote-nombre y avicontrol-m2-frontend:lote-nombre, construidas con los artefactos probados. Las imágenes aws-prep anteriores quedan disponibles para reversión. Se verificaron el contrato REST, el adaptador local y los casos de interfaz con nombre, sin nombre y sin lote.
Verificación posterior al despliegue de esta corrección: listado y detalle coinciden con la API del Módulo 1 para los tres galpones. galpon demo muestra lote demo; galpon demo 2 no tiene lote; galpón demo 3 muestra lote demo 3. El frontend actualizado responde HTTP 200. Pasaron 17 pruebas Java y la comprobación de tabla/detalle en Edge con datos simulados. PostgreSQL no se reinició.

### Nueva orden después de cancelar

Se permite crear otra orden para el mismo galpón y lote cuando todas sus órdenes anteriores están CANCELADAS y el módulo 1 confirma EN_COSECHA con ese lote alojado. Se conserva el historial. Una orden no cancelada (incluida EJECUTADA o con vaciado solicitado) sigue bloqueando duplicados.

La migración V24 (actualmente SQL, antes Java) sustituye la unicidad histórica por una reserva de lote para órdenes no canceladas, con validación de consistencia e índice único. Está probada en H2 y PostgreSQL, incluyendo datos cancelados previos y dos solicitudes concurrentes. No modifica V17 ni el módulo 1. El formulario excluye del bloqueo únicamente las órdenes canceladas.

Se conserva la reprogramación existente; el spec 005 ya la contempla en FR-001 y FR-005. Este ajuste permite usar Programar orden después de una cancelación sin reactivar la orden anterior.

Antes de aplicar V24 se creó un respaldo en EC2: /home/ec2-user/avicontrol-modulo2/backups/antes-orden-tras-cancelacion.dump. Las imágenes de esta actualización se etiquetan orden-tras-cancelacion. Después de V24, no se debe volver a una imagen anterior que desconozca la reserva de lote sin preparar también la reversión de esquema o restaurar el respaldo.
### V24 exclusivamente SQL — 20 de noviembre de 2026

La clase Java de migración se retiró. Las migraciones comunes V1–V23 permanecen intactas en src/main/resources/db/migration. V24 está en db/vendor/postgresql/V24__permitir_orden_tras_cancelacion.sql y db/vendor/h2/V24__permitir_orden_tras_cancelacion.sql. Spring Boot selecciona solo el motor correspondiente con spring.flyway.locations=classpath:db/migration,classpath:db/vendor/{vendor}. Las dos variantes tienen el mismo efecto; sus sintaxis para identificar y eliminar la restricción anterior son distintas.

En una base nueva o en versión 23, Flyway aplica V24 SQL normalmente. En una base donde ya se ejecutó V24 Java, no se debe intentar aplicar el SQL otra vez ni desactivar la validación de Flyway.

Procedimiento para el entorno con V24 Java ya aplicada:
1. Respaldar la base y su historial de Flyway. Detener el backend durante la transición.
2. Comprobar que el esquema contiene la columna lote_reservado_id, el CHECK ck_orden_reserva_lote y el índice único uq_orden_no_cancelada_galpon_lote, y que las reservas coinciden con el estado de las órdenes.
3. Ejecutar con psql, ON_ERROR_STOP habilitado y contra la base correcta, el archivo src/main/resources/db/maintenance/normalizar_historial_v24_java_a_sql.sql. Este SQL comprueba el registro Java esperado y el esquema antes de cambiar únicamente el tipo y nombre del script en el historial. No cambia órdenes ni recrea objetos. Está fuera de las ubicaciones de migración automática.
4. Con la versión de Flyway compatible con el proyecto (12.4.0), configurar FLYWAY_URL, FLYWAY_USER y FLYWAY_PASSWORD de forma privada, usando las mismas ubicaciones SQL que el nuevo backend. Desde la raíz del repositorio ejecutar:

    flyway -locations=filesystem:src/main/resources/db/migration,filesystem:src/main/resources/db/vendor/postgresql repair
    flyway -locations=filesystem:src/main/resources/db/migration,filesystem:src/main/resources/db/vendor/postgresql validate
    flyway -locations=filesystem:src/main/resources/db/migration,filesystem:src/main/resources/db/vendor/postgresql info

5. Verificar que no hay migraciones fallidas, ausentes ni pendientes inesperadas y que V24 figura como SQL con checksum. Desplegar el nuevo backend y comprobar que no reejecuta V24 y conserva órdenes, lotes y eventos. No reiniciar la imagen anterior durante la transición.
6. Conservar el respaldo y el registro de la intervención. No ejecutar clean ni borrar el historial para resolver una diferencia de tipos.

Este procedimiento se prueba sobre PostgreSQL temporal: simula el registro JDBC de la V24 anterior, lo convierte a SQL, ejecuta repair/validate y confirma cero migraciones ejecutadas y los mismos registros de órdenes. Flyway repair puede afectar otras entradas del historial; antes de ejecutarlo deben revisarse todas las diferencias, no solo V24.

Referencias: [ubicaciones por motor en Spring Boot](https://docs.spring.io/spring-boot/how-to/data-initialization.html), [Flyway repair](https://documentation.red-gate.com/fd/repair-277578892.html).

La conversión del historial de AWS no se aplicó durante esta modificación del repositorio. La lectura por SSH no respondió durante esta revisión; antes del siguiente despliegue se debe recuperar el acceso y ejecutar el procedimiento controlado.