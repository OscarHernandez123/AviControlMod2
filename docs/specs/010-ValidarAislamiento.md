# Feature Specification: Validar aislamiento de un galpón

**Created**: 2026-09-04  
**Last Updated**: 2026-09-22  

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Evaluación y Confirmación de Aislamiento Sanitario (Priority: P1)

Como Médico Veterinario de la granja, quiero acceder a la bandeja de solicitudes de revisión emitidas por los trabajadores de campo, evaluar los signos clínicos del galpón reportado y confirmar su aislamiento preventivo cuando su estado vigente sea `productiva`, para contener la propagación biológica y habilitar de forma inmediata el diagnóstico del lote (`Spec 011`).

**Why this priority**: Es la compuerta de contención biológica de la granja. Ningún lote puede ser puesto en cuarentena sin la evaluación técnica vinculante del Veterinario. A su vez, el aislamiento es el prerrequisito obligatorio para que el sistema permita ejecutar el caso de uso `Diagnosticar galpón` (`<<include>>`).

**Independent Test**: Se prueba ingresando al sistema con el rol `VETERINARIO` frente a una solicitud en estado `PENDIENTE` asociada a un galpón confirmado en estado `productiva` por el Módulo 1. Al confirmar el aislamiento, el sistema cambia el estado de la solicitud a `ATENDIDA`, registra el aislamiento en el subsistema sanitario, commitea el cambio de estado del galpón a `aislamiento` en el Módulo 1, persiste el evento en `san_outbox`, asienta la firma en `san_auditoria` y redirecciona al formulario de diagnóstico clínico (`Spec 011`). La mera emisión o consulta de la solicitud no modifica el galpón.

**Acceptance Scenarios**:

1. **Scenario**: Aislamiento exitoso de un galpón productivo con derivación a diagnóstico
   - **Given** que existe una solicitud de revisión en estado `PENDIENTE` enviada por un trabajador para un galpón específico
   - **And** el Módulo 1 confirma que el estado operativo vigente de dicho galpón es `productiva`
   - **And** el usuario autenticado posee el rol `VETERINARIO`
   - **When** el Veterinario confirma la orden de aislamiento preventivo ingresando las observaciones de campo
   - **Then** el sistema actualiza el estado del galpón a `aislamiento` en el Módulo 1
   - **And** marca la solicitud de revisión como `ATENDIDA`
   - **And** persiste atómicamente el evento de integración `AislamientoValidadoIntegrationEvent` en la tabla `san_outbox` bajo el tópico `sanitary.isolation.validated.v1`
   - **And** asienta la traza inmutable en `san_auditoria` con matrícula, usuario, fecha y hora
   - **And** habilita automáticamente el flujo de diagnóstico clínico inmediato (`Spec 011`) para dicho galpón

2. **Scenario**: Rechazo clínico de la sospecha (Desestimación del aislamiento)
   - **Given** una solicitud de revisión en estado `PENDIENTE` para un galpón en estado `productiva`
   - **When** el Veterinario realiza la inspección, concluye que no hay cuadro infectocontagioso y selecciona la opción `Rechazar validación` ingresando una justificación técnica obligatoria
   - **Then** el sistema actualiza la solicitud a estado `RECHAZADA` almacenando la justificación del descarte
   - **And** mantiene inalterado el estado del galpón como `productiva` en el Módulo 1
   - **And** registra el asiento de auditoría en `san_auditoria`
   - **And** no genera eventos de aislamiento ni deriva al flujo de diagnóstico

3. **Scenario**: Bloqueo por ausencia de solicitud de revisión previa
   - **Given** un galpón operativo en estado `productiva` que no posee ninguna solicitud de revisión registrada por un trabajador
   - **When** el Veterinario intenta forzar una orden de aislamiento directa
   - **Then** el sistema deniega la acción, notificando que todo aislamiento requiere como antecedente administrativo una solicitud formal de revisión de campo

4. **Scenario**: Bloqueo por estado incompatible del galpón en Módulo 1
   - **Given** una solicitud de revisión pendiente, pero el Módulo 1 reporta que el estado actual del galpón es `aislamiento`, `en_cosecha`, `vaciado_sanitario` o `inactivo`
   - **When** el Veterinario intenta evaluar o confirmar el aislamiento
   - **Then** el sistema interrumpe la operación informando que el galpón no cumple con el estado inicial mandatorio (`productiva`)
   - **And** no altera los registros sanitarios ni emite eventos de outbox

5. **Scenario**: Detección de colisión por cambio de estado concurrente en Módulo 1
   - **Given** que el galpón figuraba en estado `productiva` al momento de abrir la evaluación
   - **And** el galpón transiciona a otro estado en el Módulo 1 (ej. traslado a cosecha) antes de que el Veterinario presione confirmar
   - **When** el Veterinario confirma el aislamiento
   - **Then** el sistema revalida en tiempo real el estado en el Módulo 1, detecta el cambio de estado, cancela la transacción y notifica la inconsistencia al usuario

6. **Scenario**: Denegación de acceso por rol no facultado
   - **Given** un usuario autenticado con rol `TRABAJADOR` o `ADMINISTRADOR`
   - **When** intenta enviar el comando HTTP de validación o rechazo de un aislamiento
   - **Then** el sistema intercepta la petición y retorna HTTP 403 Forbidden
   - **And** no modifica el estado de la solicitud ni del galpón

---

### User Story 2 - Idempotencia y Blindaje de Trazabilidad Operativa (Priority: P2)

Como auditor de bioseguridad y aseguramiento de calidad, quiero garantizar que cada orden de aislamiento se procese de manera estrictamente idempotente y atómica, previniendo dobles aislamientos, eventos redundantes hacia Kafka o transiciones duplicadas ante reconexiones de red.

**Why this priority**: Un doble aislamiento o un evento replicado en el broker corrompería las métricas operativas del Módulo 1, alteraría los historiales de cuarentena y provocaría fallas de consistencia en el seguimiento veterinario.

**Independent Test**: Se envía el mismo comando de confirmación de aislamiento dos veces consecutivas bajo la misma cabecera `X-Idempotency-Key`. Se verifica que el segundo llamado retorna la respuesta HTTP idéntica almacenada en caché sin volver a mutar el Módulo 1, sin duplicar registros en `san_auditoria` y sin registrar mensajes redundantes en `san_outbox`.

**Acceptance Scenarios**:

1. **Scenario**: Reintento transparente bajo la misma clave de idempotencia
   - **Given** un comando de confirmación de aislamiento procesado exitosamente bajo la cabecera `X-Idempotency-Key: IDEMP-AIS-8840`
   - **When** el frontend o cliente HTTP retransmite la petición con idéntico payload y misma cabecera debido a un corte momentáneo de red
   - **Then** el sistema intercepta la clave de idempotencia
   - **And** retorna la respuesta original almacenada (código 200 OK y DTO de confirmación)
   - **And** no ejecuta una segunda mutación en el Módulo 1 ni inserta filas duplicadas en `san_outbox`

2. **Scenario**: Rechazo de aislamiento sobre un galpón que ya fue aislado
   - **Given** un galpón cuyo aislamiento ya fue consolidado en el sistema
   - **When** se emite una nueva solicitud o comando sobre dicho galpón
   - **Then** el sistema responde con HTTP 409 Conflict informando que el galpón se encuentra actualmente en régimen de aislamiento

---

### Edge Cases

- **Indisponibilidad del Módulo 1**: Si al momento de consultar o actualizar el estado del galpón el servicio/adaptador del Módulo 1 no responde o arroja timeout, la transacción sanitaria se aborta completamente (`Rollback`). No se marca la solicitud como atendida, no se persiste en `san_outbox` y se notifica el fallo de integración al usuario.
- **Solicitud en evaluación que ya fue atendida por otro veterinario**: Si dos veterinarios evalúan simultáneamente la misma solicitud, el primero que confirme eleva la versión optimista de la entidad. El segundo recibe un rechazo por colisión de concurrencia (`OptimisticLockException` / HTTP 409 Conflict), obligándolo a refrescar su pantalla.
- **Caída del broker Kafka**: El guardado de la entidad, la actualización de la solicitud, la traza de auditoría y el registro del evento en `san_outbox` se realizan en la base de datos local bajo una misma transacción SQL. La caída de Kafka no frena la operación médica; el scheduler publicará el evento pendiente en cuanto el broker esté disponible.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La evaluación, validación y desestimación del aislamiento clínico DEBE estar reservada exclusivamente a usuarios autenticados con rol `VETERINARIO`.
- **FR-002**: Toda validación de aislamiento DEBE tener como antecedente obligatorio una `SolicitudRevision` en estado `PENDIENTE`, emitida previamente por un usuario con rol `TRABAJADOR`.
- **FR-003**: La creación o consulta de una solicitud de revisión NO DEBE modificar el estado del galpón; el galpón mantiene su estado original hasta que exista una resolución veterinaria explícita.
- **FR-004**: La solicitud de revisión DEBE estar asociada a un identificador único de galpón (`galponId`) administrado y provisto por el Módulo 1.
- **FR-005**: El sistema DEBE validar de forma síncrona contra el Módulo 1 que el estado vigente del galpón sea estrictamente `productiva` tanto al consultar la solicitud como inmediatamente antes de confirmar el aislamiento.
- **FR-006**: Si el galpón reporta un estado distinto de `productiva`, el sistema DEBE rechazar la validación, abortar la transacción y notificar la inconsistencia sin alterar datos.
- **FR-007**: Si el Veterinario dictamina que los signos no corresponden a una alerta epidemiológica, el sistema DEBE permitir rechazar la solicitud, transicionando su estado a `RECHAZADA`, exigiendo una justificación textual de mínimo 10 caracteres y conservando el galpón en `productiva`.
- **FR-008**: Al confirmarse la validación del aislamiento, el sistema DEBE:
  - Cambiar el estado del galpón a `aislamiento` en el Módulo 1.
  - Actualizar el estado de la solicitud de revisión a `ATENDIDA`.
  - Crear el registro del agregado de aislamiento con fecha, observaciones clínicas y profesional a cargo.
  - Habilitar de inmediato la ejecución del caso de uso incluido `Diagnosticar galpón` (`Spec 011`).
- **FR-009**: La operación de aislamiento NO DEBE alterar atributos estructurales del galpón (capacidad, dimensiones) ni descontar inventarios de bodega.
- **FR-010**: Al confirmarse el aislamiento, el sistema DEBE persistir de forma atómica en la tabla `san_outbox` el evento `AislamientoValidadoIntegrationEvent` bajo el tópico `sanitary.isolation.validated.v1`.
- **FR-011**: Cada resolución veterinaria (validación confirmada o rechazada) DEBE registrar un asiento inmutable en la tabla `san_auditoria` con usuario, matrícula, fecha, hora, identificador de solicitud y dictamen.
- **FR-012**: El sistema DEBE aplicar control de concurrencia optimista (`@Version`) sobre la solicitud y soportar la cabecera `X-Idempotency-Key` almacenada en caché durante 24 horas.
- **FR-013**: Queda ESTRICTAMENTE PROHIBIDO el borrado físico (`DELETE` en SQL) sobre cualquier solicitud, registro de aislamiento o traza de auditoría.

---

### Key Entities

- **SolicitudRevision**: Antecedente administrativo de campo emitido por el trabajador. Atributos: `id` (UUID), `galponId` (UUID), `trabajadorId` (UUID), `estado` (Enum: `PENDIENTE`, `ATENDIDA`, `RECHAZADA`), `observacionesTrabajador` (Texto), `justificacionRechazo` (Texto nullable), `fechaSolicitud` (Timestamp), `version` (Integer).
- **AislamientoSanitario**: Aggregate Root del subsistema de sanidad. Atributos: `id` (UUID), `solicitudId` (UUID), `galponId` (UUID), `veterinarioId` (UUID), `observacionesClinicas` (Texto), `fechaInicio` (Timestamp), `activo` (Boolean).
- **Galpón**: Entidad externa perteneciente al Módulo 1. El Módulo 2 únicamente lee y solicita la mutación de su atributo de estado (`productiva` $\to$ `aislamiento`).
- **AislamientoValidadoIntegrationEvent**: Contrato de mensajería publicado hacia Kafka (`eventId`, `aggregateId`, `galponId`, `veterinarioId`, `estadoAnterior`, `estadoNuevo`, `occurredOn`).
- **Auditoría Sanitaria (`san_auditoria`)**: Bitácora inmutable append-only de responsabilidad veterinaria.
- **Outbox (`san_outbox`)**: Tabla transaccional para garantizar entrega de eventos atómicos bajo el patrón Transactional Outbox.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las evaluaciones de aislamiento se ejecutan sobre solicitudes formales de revisión registradas previamente por trabajadores de campo[cite: 4, 8].
- **SC-002**: El 100% de los intentos de confirmación o desestimación ejecutados por roles distintos a `VETERINARIO` son bloqueados con código HTTP 403 Forbidden.
- **SC-003**: El tiempo de respuesta del sistema para evaluar y persistir atómicamente la validación del aislamiento es inferior a 250 milisegundos bajo condiciones normales de red.
- **SC-004**: Cero por ciento (0%) de modificaciones sobre el estado del galpón provocadas por la simple emisión, consulta o navegación de una solicitud de revisión sin confirmación veterinaria[cite: 4].
- **SC-005**: El 100% de los aislamientos confirmados habilitan la disponibilidad inmediata del expediente de diagnóstico (`Spec 011`) en menos de 1 segundo tras el guardado[cite: 4, 8].
- **SC-006**: Cero incidentes (0%) de borrado físico (`DELETE`) en bases de datos sobre solicitudes o registros de aislamiento sanitario.
- **SC-007**: El 100% de los eventos de integración asociados se resguardan de forma atómica en `san_outbox` dentro de la misma transacción local de base de datos.
- **SC-008**: Cero duplicados (0%) en publicaciones de eventos o cambios de estado gracias a la validación de concurrencia optimista y la cabecera `X-Idempotency-Key`.