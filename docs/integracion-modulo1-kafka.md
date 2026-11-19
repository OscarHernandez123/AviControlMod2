# Publicación Kafka hacia Módulo 1

## Activación

Requiere el perfil `modulo1-rest`, PostgreSQL y acceso a REST y Kafka del Módulo 1. Las credenciales de conexión Kafka TLS/SASL pueden configurarse con las propiedades estándar de Spring Boot. El starter de Kafka registra el productor y sus propiedades.

```powershell
$env:SPRING_PROFILES_ACTIVE = 'modulo1-rest'
$env:AVICONTROL_MODULO1_BASE_URL = 'http://localhost:8080'
$env:SPRING_KAFKA_BOOTSTRAP_SERVERS = 'localhost:19092'
$env:SERVER_PORT = '8081'
.\gradlew.bat bootRun
```

Usar también las variables de PostgreSQL descritas en integracion-modulo1-rest.md. Los argumentos `--spring.kafka.bootstrap-servers=...` tienen prioridad sobre la variable de entorno.

## Solicitudes y seguimiento

Todas requieren autenticación Basic con los roles del backend. Las solicitudes nuevas responden 202 con `alertaId`, `estado: PENDIENTE` e `intentos`. Guardar el alertaId y reutilizarlo para reintentar la misma solicitud, sin cambiar su fecha ni sus datos.

| Ruta POST | Roles | Campos |
|---|---|---|
| `/api/integracion/modulo1/eventos/mortalidad` | TRABAJADOR o ADMINISTRADOR | alertaId, galponId, loteId, fechaHoraEvento, cantidadMuertos |
| `/api/integracion/modulo1/eventos/alerta-sanitaria` | VETERINARIO o ADMINISTRADOR | alertaId, galponId, loteId opcional, fechaHora, accion; AISLAMIENTO requiere tipoEnfermedad y descripcion, gravedad opcional |
| `/api/integracion/modulo1/eventos/vaciado-sanitario` | ADMINISTRADOR | alertaId, galponId, loteId, fechaHoraEvento |

`GET /api/integracion/modulo1/eventos/{alertaId}` permite consultar la entrega con cualquiera de esos roles. Se requiere galponId también en alertas sanitarias por lote para asegurar la clave Kafka. Si llega loteId, se valida su relación con el galpón activo mediante REST. Cantidad acepta solo un entero JSON positivo, no decimales ni texto. Fechas usan OffsetDateTime con zona; se normalizan a America/Bogota en el payload. Se omiten campos opcionales sin valor.

Ejemplo de mortalidad (sustituir ids y fecha por los valores vigentes):

```json
{
  "alertaId": "9d3c0b2e-6a58-4c42-8b0c-2a1f3f0f7a11",
  "galponId": "550e8400-e29b-41d4-a716-446655440000",
  "loteId": "2fc03a21-84c4-4c83-bb15-6607bb75cb90",
  "fechaHoraEvento": "2026-11-19T10:30:00-05:00",
  "cantidadMuertos": 2
}
```

## Contrato del productor

| Tópico | eventType | eventVersion |
|---|---|---|
| avicontrol.mortalidad | MORTALIDAD_REGISTRADA | 1 |
| avicontrol.alerta-sanitaria | ALERTA_SANITARIA_EMITIDA | 1 |
| avicontrol.vaciado-sanitario | VACIADO_SANITARIO_SOLICITADO | 1 |

Clave: galponId como UUID convertido a texto en los tres casos. Serializadores de clave y valor: `org.apache.kafka.common.serialization.StringSerializer`. El valor es el JSON del sobre con eventId estable, eventType, eventVersion, occurredAt, publishedAt, producer=modulo-2, aggregateType=Galpon, aggregateId=galponId y payload.

El productor usa acks=all, enable.idempotence=true y max.in.flight.requests.per.connection=1. La outbox mantiene un único publicador entre instancias con bloqueo asesor de PostgreSQL y no adelanta pendientes del mismo galpón. Esto ordena el envío; no garantiza el orden de consumo entre tópicos distintos.

## Persistencia y recuperación

La migración V22 crea `modulo1_event_outbox`. La solicitud persiste su evento dentro de una transacción; un rollback no deja un evento publicable. Una clave existente con iguales datos devuelve su entrega sin duplicar la fila, incluso si ya cambió el estado remoto. Reutilizarla con otros datos devuelve 409.

El publicador corre cada 1000 ms (configurable con AVICONTROL_MODULO1_OUTBOX_DELAY_MS), espera el ACK del broker y después marca PUBLICADO. Ante fallo conserva PENDIENTE, incrementa intentos y programa reintento cinco segundos después. Guarda la clase del último error sin secretos.

Si cambia el día de negocio antes de la entrega, marca EXPIRADO, sin cambiar fechas ni reenviar automáticamente un evento que el contrato rechazaría. El estado se consulta por API; el error también queda en la tabla. Una recuperación debe acordarse con el equipo del Módulo 1.

La entrega es al menos una vez: una caída entre ACK y commit puede provocar reentrega con el mismo alertaId. Módulo 1 deduplica mortalidad y vaciado; la deduplicación de alertas sanitarias continúa pendiente en el acuerdo.

## Alcance y límites

PUBLICADO significa recepción por Kafka, no aceptación de negocio. El módulo 1 puede rechazar por cambios concurrentes ocurridos después de la consulta REST; esos rechazos solo aparecen en sus logs. No existe todavía un evento de respuesta.

Estas rutas solicitan publicación. No crean un registro de mortalidad ni una orden de sacrificio ni resuelven una solicitud de aislamiento de los flujos anteriores del módulo 2. Los endpoints originales y los formularios se conectan con la publicación mediante los flujos descritos en la siguiente sección; el registro confirmado de población y ejecución sigue pendiente de una respuesta de negocio. No usar una escritura local adicional como confirmación de una operación remota.

El vaciado solo se solicita desde EN_COSECHA. El cambio administrativo de cosecha y el sacrificio sanitario desde AISLAMIENTO siguen sujetos al acuerdo. No se modificó código del Módulo 1.

## Validación

Las pruebas unitarias verifican campos, fechas, estados y repetición. Modulo1OutboxPostgresTest verifica rollback, idempotencia, reintento conservando clave y eventId, y expiración sin publicar; utiliza esquemas temporales exclusivos.

```powershell
$env:AVICONTROL_RUN_POSTGRES_TESTS = 'true'
$env:AVICONTROL_TEST_DB_URL = 'jdbc:postgresql://localhost:15432/avicontrol_m2_rest_prueba'
.\gradlew.bat test --tests '*Modulo1OutboxPostgresTest' --tests '*SolicitarEventoModulo1UseCaseTest'
```

La prueba optativa Modulo1KafkaLiveTest requiere ambos servicios y el broker levantados en un ambiente de demostración. Crea un galpón y lote nuevos mediante REST del módulo 1, publica los tres tipos (aislamiento y reanudación incluidos), verifica efectos remotos y lee claves/sobres del broker. No ejecutarla contra producción. Por defecto usa localhost:8080, localhost:8081 y localhost:19092; las variables AVICONTROL_LIVE_MODULO1_URL, AVICONTROL_LIVE_MODULO2_URL y SPRING_KAFKA_BOOTSTRAP_SERVERS permiten cambiarlos.

```powershell
$env:AVICONTROL_RUN_KAFKA_LIVE_TESTS = 'true'
$env:SPRING_KAFKA_BOOTSTRAP_SERVERS = 'localhost:19092'
.\gradlew.bat test --tests '*Modulo1KafkaLiveTest'
```

La evidencia capturada se guarda en build/integracion-rest/eventos-kafka.json.

## Formularios y endpoints originales conectados

En `modulo1-rest`, las rutas originales conservan los campos del formulario y los roles existentes:

- `POST /api/galpones/{id}/mortalidad`: responde 202 con id, galponId, loteId e integracion. Requiere Idempotency-Key. Conserva causa, observaciones, responsable, cantidad y fecha en modulo1_operaciones (V23), dentro de la misma transacción que la outbox. No inventa población resultante ni clasificación. Repetir la misma clave devuelve la solicitud existente; cambiar sus datos devuelve 409. Los registros confirmados de mortalidad del flujo local no se utilizan como si fueran confirmaciones remotas.
- `POST /api/solicitudes-aislamiento/{id}/validacion`: ACEPTAR guarda la decisión veterinaria y encola AISLAMIENTO en una transacción; responde 202 con integracion. ACEPTADA describe la decisión del veterinario, no el estado del galpón remoto. RECHAZAR responde 200 sin publicar. La creación de solicitudes ya reconoce PRODUCTIVO además del alias local PRODUCTIVA.
- El ejecutor de órdenes productivas vencidas encola VACIADO usando el id de la orden como alertaId. No cambia población, alojamiento ni estado local a EJECUTADA. La consulta de órdenes muestra VACIADO_SOLICITADO y la entrega asociada; la base mantiene la orden pendiente hasta acordar confirmaciones. Cancelar/reprogramar después del vencimiento continúa prohibido por las reglas existentes.
- El proceso de diagnósticos vencidos encola REANUDACION con un alertaId determinista derivado del diagnóstico. No marca REINTEGRADO por el solo envío. Las ejecuciones posteriores no generan otra solicitud.
- Validar aptitud no cambia remotamente a EN_COSECHA. Ese cambio sigue siendo administrativo en Módulo 1. La pantalla lo explica.
- Sacrificio sanitario desde AISLAMIENTO queda deshabilitado en el formulario remoto y su endpoint devuelve 409 antes de modificar datos. El contrato no ofrece ese flujo.

El perfil local/test conserva los controladores y el comportamiento simulado previos. No combinar con modulo1-rest.

### Seguimiento visual

`GET /api/integracion/modulo1/operaciones` lista las últimas 50 operaciones sin exponer las observaciones clínicas. `GET /api/integracion/modulo1/operaciones/{id}` consulta una referencia. Los roles trabajador, veterinario y administrador pueden consultar su entrega.

El panel de seguimiento de trabajador, veterinario, diagnósticos y órdenes productivas distingue Pendiente de envío, Enviado/aceptación pendiente y Expirado/requiere revisión. Las referencias sobreviven a recargar porque se guardan en PostgreSQL. `GET /api/integracion/modulo1/capacidades` permite a las pantallas distinguir el modo remoto del local. El proxy Vite dirige estas lecturas con credenciales de trabajador de demostración, sin ampliar permisos de escritura.

La recepción por Kafka sigue sin acreditar aceptación de negocio. Las operaciones, las órdenes y los diagnósticos requieren el acuerdo de confirmación para cerrar su ciclo local, generar resultados definitivos y reconciliar rechazos. El panel y los mensajes no afirman lo contrario.

### Prueba completa de formularios

Se ejecutó en Edge sin ventana con ambos backends y Kafka reales. El script está en frontend/tests/integration/kafka-flows.mjs. Crea un galpón y lote propios mediante la API existente de Módulo 1; registra mortalidad, crea una solicitud de aislamiento, acepta la decisión veterinaria, registra un diagnóstico y programa una orden desde las pantallas reales.

Para no esperar días, prepara una medicación de prueba en PostgreSQL y adelanta únicamente las fechas del diagnóstico y la orden que acaba de crear en avicontrol_m2_rest_prueba. No modifica código ni configuración de Módulo 1. Comprueba los efectos por su API y guarda resultados y capturas en build/integracion-rest/formularios-resultados.json y formularios-final.png.

Requiere Windows con Edge en su ubicación habitual, frontend en 127.0.0.1:5173 apuntando a 8081, Módulo 1 en 8080 y los contenedores de integración de esta demostración. Ejecutar solo con esas bases de prueba:

```powershell
npm ci --prefix frontend
npm install --prefix build/ui-tests --no-package-lock playwright-core
node frontend/tests/integration/kafka-flows.mjs
```

Las pruebas FlujosKafkaModulo1UseCaseTest verifican preservación de datos, decisiones y no duplicación de los procesos automáticos. Modulo1OutboxPostgresTest incluye rollback conjunto de auditoría y evento.
