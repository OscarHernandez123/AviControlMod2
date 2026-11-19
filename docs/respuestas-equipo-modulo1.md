# Respuestas al equipo del Módulo 1 — Integración con Módulo 2

**Proyecto:** AVICONTROL
**Fecha:** 19 de noviembre de 2026
**Estado:** consultas REST y publicación Kafka implementadas y probadas localmente con ambos módulos, sin modificar el código del Módulo 1.

## 1. IP pública de la aplicación

La aplicación corre actualmente desde nuestro computador. **La IP pública de salida actual para las pruebas es `191.95.16.24`**, consultada el 19 de noviembre de 2026.

Solicitamos autorizar esta IP para acceder a los servicios Kafka y REST del Módulo 1, según sus reglas de acceso. Las pruebas se realizarán desde la misma conexión y VPN utilizadas al consultar la IP; esta puede cambiar al cambiar de red o por reasignación del proveedor.

**La IP de salida del futuro despliegue en AWS todavía no está definida.** La compartiremos cuando esté configurado ese entorno.

`localhost` corresponde únicamente a nuestras pruebas y no es una dirección pública que pueda utilizarse para autorizar el acceso desde otro entorno.

## 2. Librería Kafka y serializadores

Usamos **Spring for Apache Kafka**, mediante el starter de Spring Boot:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-kafka'
```

Los serializadores están configurados explícitamente en `Modulo1KafkaConfiguration.java`:

```properties
key.serializer=org.apache.kafka.common.serialization.StringSerializer
value.serializer=org.apache.kafka.common.serialization.StringSerializer
```

La clave es texto y el valor es el **sobre completo serializado como JSON**, con los datos de negocio dentro de `payload`.

También configuramos:

```properties
acks=all
enable.idempotence=true
max.in.flight.requests.per.connection=1
```

La publicación usa una outbox persistente con reintentos. La confirmación del broker no se considera una confirmación de aceptación de negocio del Módulo 1.

## 3. Configuración de bootstrap.servers

Se define mediante la propiedad de Spring Boot en `application-modulo1-rest.properties`:

```properties
spring.kafka.bootstrap-servers=${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

Para cambiar el broker mediante una variable de entorno en PowerShell:

```powershell
$env:SPRING_KAFKA_BOOTSTRAP_SERVERS = 'HOST_BROKER:PUERTO'
```

En nuestras pruebas usamos `localhost:19092`.

Si se pasa el argumento `--spring.kafka.bootstrap-servers=...` al iniciar la aplicación, este tiene prioridad sobre la variable de entorno. Para utilizar la variable, debe retirarse ese argumento o mantener ambos valores alineados.

## 4. eventType y eventVersion enviados

| Tópico | eventType | eventVersion |
|---|---|---|
| `avicontrol.mortalidad` | `MORTALIDAD_REGISTRADA` | `1` |
| `avicontrol.alerta-sanitaria` | `ALERTA_SANITARIA_EMITIDA` | `1` |
| `avicontrol.vaciado-sanitario` | `VACIADO_SANITARIO_SOLICITADO` | `1` |

Estos valores van en el sobre del evento, junto a `eventId`, `occurredAt`, `publishedAt`, `producer`, `aggregateType`, `aggregateId` y `payload`.

- `producer`: `modulo-2`.
- `aggregateType`: `Galpon`.
- `aggregateId`: `galponId` como texto.

## 5. Clave de los mensajes

Sí. En los tres tópicos usamos **`galponId`, UUID convertido a texto**, como clave del mensaje Kafka, incluso cuando la alerta sanitaria también identifica un lote.

La clave de idempotencia de negocio continúa siendo **`alertaId` dentro de `payload`**.

## 6. Campos del payload y formato de fechas

Sí. Los mensajes utilizan los nombres del contrato en `camelCase`.

### Mortalidad

- `alertaId`
- `galponId`
- `loteId`
- `fechaHoraEvento`
- `cantidadMuertos`

### Alerta sanitaria

- `alertaId`
- `galponId`
- `loteId`, cuando aplica
- `fechaHora`
- `accion`: `AISLAMIENTO` o `REANUDACION`
- Para `AISLAMIENTO`: `tipoEnfermedad` y `descripcion`; `gravedad` es opcional.
- Para `REANUDACION`: se omiten los datos de enfermedad.

La implementación requiere `galponId` también para las alertas sanitarias por lote, porque se utiliza como clave Kafka. Si llega `loteId`, se valida que corresponda al lote activo del galpón.

### Vaciado sanitario

- `alertaId`
- `galponId`
- `loteId`
- `fechaHoraEvento`

### Reglas de formato

- UUID como texto.
- Cantidades como enteros JSON, sin decimales ni comillas.
- Fechas ISO-8601 con zona; el payload se normaliza a `America/Bogota`.
- Los campos opcionales sin valor se omiten; no se envían como `null`.
- Los reintentos conservan el mismo `alertaId` y la fecha original.

Ejemplo de fecha:

```json
"fechaHoraEvento": "2026-11-19T10:30:00-05:00"
```

Si un evento pendiente supera el día de negocio antes de su entrega, se marca `EXPIRADO`; no se altera su fecha para forzar la aceptación.

## 7. Llamadas REST al Módulo 1

La aplicación hace actualmente estas consultas:

```http
GET /api/v1/galpones?page={pagina}&orden=nombre,asc
GET /api/v1/galpones/{galponId}
```

La primera recorre páginas de diez elementos, empezando en la página 1, para construir el listado y el resumen del Módulo 2. La segunda obtiene el estado, la capacidad y el lote activo, si existe.

**La aplicación no llama a rutas de escritura del Módulo 1**, ni a `/galpones/resumen` ni al historial de lotes. El cambio administrativo a `EN_COSECHA` sigue correspondiendo al Módulo 1.

Durante las pruebas se utilizaron adicionalmente las rutas existentes de creación de galpones/lotes y cambio administrativo de estado para preparar datos de demostración; esas llamadas pertenecen al script de pruebas, no al cliente REST de la aplicación.

La URL del servidor se configura mediante:

```powershell
$env:AVICONTROL_MODULO1_BASE_URL = 'http://HOST:PUERTO'
```

La URL debe corresponder al servidor, sin añadir `/api/v1`.

## Validación realizada y pendientes de coordinación

Ya probamos los formularios y endpoints originales del Módulo 2 contra el Módulo 1 con Kafka real. Verificamos:

- Mortalidad sin doble descuento al reintentar.
- Aislamiento y reanudación del galpón.
- Vaciado sanitario con desvinculación del lote.
- Claves, tipos, versiones y sobres de los mensajes publicados.
- Manejo de indisponibilidad y recuperación de las consultas REST.

No se modificó el código del Módulo 1.

Para conectarnos a su entorno necesitamos:

1. La dirección del broker Kafka accesible desde nuestra aplicación.
2. La URL base de la API REST.
3. Los requisitos de autenticación y cifrado de ambos servicios.

Queda pendiente acordar cómo recibirá el Módulo 2 la **aceptación o el rechazo de negocio** de cada evento. Actualmente distinguimos la entrega a Kafka de la aceptación del Módulo 1; sus rechazos quedan en sus logs.

El sacrificio sanitario desde `AISLAMIENTO` continúa bloqueado en el modo remoto, porque el contrato actual solo permite vaciado desde `EN_COSECHA`.
