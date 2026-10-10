# Feature Specification: Ordenar sacrificio sanitario

**Created**: 2026-09-05  
**Last Updated**: 2026-09-22  
**Spec**: `docs/specs/012-OrdenarSacrificioSanitario.md`  
**Módulo**: 2 – Sanidad y Bioseguridad[cite: 8]  
**Actor Principal**: `VETERINARIO`[cite: 1, 8]  
**Precondición Obligatoria**: Diagnóstico clínico vigente con enfermedad letal (`Spec 011` completado con `requiereSacrificioSanitario == true`)[cite: 4, 8]  
**Relación UML**: Extensión (`<<extend>>`) del caso de uso `Diagnosticar galpón`[cite: 8]

---

## 1. Summary

El módulo **Ordenar sacrificio sanitario** permite al Médico Veterinario emitir y confirmar formalmente la orden de erradicación biológica total de un lote alojado en un galpón en cuarentena, cuando el diagnóstico clínico (`Spec 011`) ha identificado una patología letal de notificación obligatoria (`Spec 009` con `requiereSacrificioSanitario == true`)[cite: 4, 8].

El ciclo de vida del procedimiento se estructura en **dos etapas obligatorias y secuenciales**:

* **Etapa 1 — Emisión de la orden**: El Veterinario emite la orden asociada al diagnóstico letal, al galpón y al lote afectado. La orden se crea en estado `PENDIENTE` y **no modifica** la población de aves ni el estado operativo del galpón en el Módulo 1.
* **Etapa 2 — Confirmación de ejecución**: Tras culminar el procedimiento sanitario en campo, el Veterinario confirma la ejecución física. En ese instante, el sistema solicita al Módulo 1 establecer la **población del lote en cero (0)**, transicionar el estado del galpón de `aislamiento` a `vaciado_sanitario` y marcar la orden como `EJECUTADA` en una única transacción atómica.

**Restricciones globales mandatorias**:

* 🔒 **Prohibición absoluta de borrado físico**: Ninguna instrucción `DELETE` SQL está permitida sobre órdenes de sacrificio ni asientos de auditoría[cite: 1].
* 🔒 **Atomicidad e Integridad Transaccional**: La confirmación persiste de forma simultánea `san_ordenes_sacrificio` + `san_outbox` + `san_auditoria` bajo el mismo `@Transactional`[cite: 1].
* 🔒 **Seguridad y Atribución**: Acceso reservado exclusivamente a usuarios autenticados con rol `VETERINARIO`[cite: 1, 8].
* 🔒 **Concurrencia e Idempotencia**: Control optimista mediante `@Version` sobre la orden y cabeceras `X-Idempotency-Key` retenidas durante 24 horas[cite: 1].
* 🔒 **Alcance total no negociable**: La orden comprende estrictamente el 100% de la población del lote; el sistema prohíbe sacrificios parciales y no solicita cantidades numéricas manuales al usuario.

---

## 2. User Scenarios & Testing *(mandatory)*

### User Story 1 - Emisión de la Orden de Sacrificio Sanitario Total (Priority: P1)

**Como** Médico Veterinario de la granja,  
**Quiero** emitir una orden formal de sacrificio sanitario total sobre un lote cuyo diagnóstico clínico haya confirmado una patología letal,  
**Para** oficializar la directriz de erradicación biológica de toda la parvada afectada y dejar la orden en estado pendiente hasta su ejecución material en campo[cite: 4, 8].

**Why this priority**: Es la única vía autorizada para formalizar la erradicación sanitaria[cite: 8]. Sin esta orden previa, el personal de campo no puede proceder al sacrificio y disposición final de las aves, arriesgando la diseminación masiva del patógeno a galpones vecinos[cite: 4, 8].

**Independent Test**:
* **Caso A (Emisión exitosa)**: Envío de `POST /api/v1/sanitary/sacrificios` asociando un diagnóstico mortal vigente sobre galpón en `aislamiento` con población > 0. El sistema crea la orden en estado `PENDIENTE`, persiste el evento en outbox y registra la firma en auditoría, manteniendo intacta la población del lote y el estado del galpón en el Módulo 1.
* **Caso B (Rechazo por patología no letal)**: Envío de `POST` sobre un diagnóstico cuya enfermedad tiene `requiereSacrificioSanitario == false`. El sistema rechaza la solicitud retornando HTTP `409 Conflict` (`EnfermedadNoRequiereSacrificioException`)[cite: 4].

#### Acceptance Scenarios

1. **AS-001 - Scenario**: Emisión exitosa de la orden de sacrificio total
   * **Given** que existe un diagnóstico clínico activo (`Spec 011`) vinculado a una enfermedad con `requiereSacrificioSanitario == true` (`Spec 009`)[cite: 4, 8]
   * **And** el galpón referenciado se encuentra en estado `aislamiento` verificado en el Módulo 1[cite: 4, 8]
   * **And** el lote alojado posee una población actual mayor a cero (`poblacionActual > 0`)
   * **When** el Veterinario confirma la emisión de la orden ingresando las observaciones de bioseguridad
   * **Then** el sistema crea la orden con `alcance = TOTAL`, `estado = PENDIENTE` y marca temporal UTC de emisión
   * **And** asocia formalmente la orden al `diagnosticoId`, `galponId` y `loteId` correspondientes
   * **And** persiste atómicamente el evento `OrdenSacrificioEmitidaIntegrationEvent` en `san_outbox` bajo el tópico `sanitary.sacrifice.issued.v1`[cite: 1]
   * **And** asienta la firma inmutable del Veterinario en `san_auditoria` con matrícula y timestamp[cite: 1]
   * **And** conserva la población del lote y el estado `aislamiento` del galpón sin modificaciones operativas

2. **AS-002 - Scenario**: Rechazo por enfermedad que no requiere sacrificio sanitario
   * **Given** un diagnóstico vigente cuya patología asociada tiene `requiereSacrificioSanitario == false` (enfermedad tratable clínicamente)[cite: 4]
   * **When** el Veterinario intenta forzar la emisión de una orden de sacrificio para dicho diagnóstico
   * **Then** el sistema bloquea la operación, responde con HTTP `409 Conflict` (`EnfermedadNoRequiereSacrificioException`)
   * **And** no crea la orden ni altera el expediente clínico

3. **AS-003 - Scenario**: Rechazo por galpón que no está en aislamiento
   * **Given** un diagnóstico mortal pero el galpón reporta en el Módulo 1 un estado operativo distinto a `aislamiento` (ej. `productiva`, `en_cosecha`, `vaciado_sanitario`)[cite: 4, 7]
   * **When** el Veterinario intenta emitir la orden
   * **Then** el sistema responde con HTTP `409 Conflict` (`EstadoGalponIncompatibleException`)
   * **And** aborta la transacción

4. **AS-004 - Scenario**: Rechazo por lote sin población activa
   * **Given** un diagnóstico mortal vigente sobre un galpón en `aislamiento`
   * **And** el Módulo 1 reporta que el lote asociado tiene una población igual a cero (`poblacionActual == 0`)
   * **When** el Veterinario intenta emitir la orden
   * **Then** el sistema responde con HTTP `409 Conflict` (`LoteSinPoblacionException`)
   * **And** no crea la orden de sacrificio

5. **AS-005 - Scenario**: Denegación de acceso a usuarios sin rol veterinario
   * **Given** un usuario autenticado con rol `TRABAJADOR` o `ADMINISTRADOR`[cite: 1]
   * **When** intenta emitir una orden de sacrificio
   * **Then** el sistema intercepta la petición con HTTP `403 Forbidden` y no altera la base de datos[cite: 1]

---

### User Story 2 - Confirmación de la Ejecución del Sacrificio Sanitario (Priority: P2)

**Como** Médico Veterinario de la granja,  
**Quiero** confirmar que la orden de sacrificio sanitario fue materializada físicamente en campo,  
**Para** actualizar la población del lote a cero, transicionar el galpón a estado `vaciado_sanitario` en el Módulo 1 y cerrar de forma concluyente la orden sanitaria[cite: 4, 8].

**Why this priority**: Es el acto concluyente de la erradicación zoosanitaria[cite: 8]. Sin esta confirmación técnica, el lote figuraría incorrectamente con aves vivas en el inventario biológico y el galpón no podría iniciar los periodos reglamentarios de desinfección y descanso sanitario[cite: 4, 8].

**Independent Test**:
* **Caso A (Confirmación exitosa)**: Envío de `PATCH /api/v1/sanitary/sacrificios/{id}/confirmar` sobre una orden en estado `PENDIENTE`. El sistema solicita al Módulo 1 fijar la población en 0 y cambiar el galpón a `vaciado_sanitario`, transiciona la orden a `EJECUTADA`, guarda la traza en auditoría y despacha el evento en outbox.
* **Caso B (Rechazo por orden ya ejecutada)**: Envío de `PATCH` sobre una orden que ya tiene estado `EJECUTADA`. El sistema responde con HTTP `409 Conflict` (`OrdenYaEjecutadaException`) sin duplicar mutaciones.

#### Acceptance Scenarios

1. **AS-006 - Scenario**: Confirmación exitosa de la ejecución del sacrificio
   * **Given** una orden de sacrificio en estado `PENDIENTE` asociada a un galpón en `aislamiento` con población > 0[cite: 4, 7]
   * **When** el Veterinario confirma la ejecución física del sacrificio sanitario ingresando el método y observaciones de bioseguridad
   * **Then** el sistema ordena al Módulo 1 establecer la población actual del lote exactamente en cero (`poblacionActual = 0`)
   * **And** ordena al Módulo 1 transicionar el estado del galpón de `aislamiento` a `vaciado_sanitario`
   * **And** actualiza la orden marcándola como `EJECUTADA` y registrando `fechaEjecucion` con la marca temporal UTC
   * **And** persiste atómicamente el evento `SacrificioEjecutadoIntegrationEvent` en `san_outbox` bajo el tópico `sanitary.sacrifice.executed.v1`[cite: 1]
   * **And** registra el asiento inmutable en `san_auditoria` con firma profesional del Veterinario[cite: 1]

2. **AS-007 - Scenario**: Rechazo de confirmación sobre orden ya ejecutada
   * **Given** una orden de sacrificio que ya fue confirmada previamente (`estado == EJECUTADA`), con el lote en cero y el galpón en `vaciado_sanitario`
   * **When** el usuario intenta enviar nuevamente la confirmación
   * **Then** el sistema rechaza la operación con HTTP `409 Conflict` (`OrdenYaEjecutadaException`)
   * **And** no aplica modificaciones adicionales ni genera nuevos eventos

3. **AS-008 - Scenario**: Rechazo por galpón que dejó de estar en aislamiento antes de confirmar
   * **Given** una orden en estado `PENDIENTE`
   * **And** el galpón cambió concurrentemente a un estado distinto de `aislamiento` en el Módulo 1
   * **When** el Veterinario intenta confirmar la ejecución
   * **Then** el sistema aborta la operación, responde con HTTP `409 Conflict` (`EstadoGalponIncompatibleException`)
   * **And** no modifica la población del lote ni la orden

4. **AS-009 - Scenario**: Preservación de estado mientras la orden esté pendiente
   * **Given** una orden de sacrificio en estado `PENDIENTE` cuya ejecución aún no se ha confirmado
   * **When** se consulta el galpón o el lote
   * **Then** el sistema verifica que la población del lote se mantiene intacta y el galpón permanece en `aislamiento`[cite: 4, 7]

5. **AS-010 - Scenario**: Denegación de acceso en confirmación por rol no autorizado
   * **Given** un usuario sin rol `VETERINARIO`
   * **When** intenta confirmar la ejecución de una orden
   * **Then** el sistema responde con HTTP `403 Forbidden` y mantiene la orden en `PENDIENTE`[cite: 1]

---

### User Story 3 - Idempotencia y Blindaje Transaccional (Priority: P3)

**Como** Médico Veterinario de la granja,
**Quiero** que los reintentos de red y las fallas de servicios externos sean gestionados de forma idempotente y atómica,
**Para** garantizar que ni emisiones ni confirmaciones puedan duplicarse, quedar a medias o producir inconsistencias entre el Módulo 2 y el Módulo 1.

**Why this priority**: Es la garantía de integridad zoosanitaria bajo condiciones de red inestable en campo. Sin blindaje idempotente y rollback cross-context, un timeout podría generar órdenes duplicadas, poblaciones extinguidas sin orden cerrada o galpones transicionados sin auditoría.

**Independent Test**:
* **Caso A (Idempotencia)**: Envío duplicado del mismo `POST` con la misma `X-Idempotency-Key`. El sistema devuelve la respuesta original cacheada sin crear una segunda orden.
* **Caso B (Rollback)**: Simulación de caída del Módulo 1 durante el `PATCH /confirmar`. La transacción completa hace Rollback: la orden permanece `PENDIENTE`, el lote no cambia y no se encola outbox.

#### Acceptance Scenarios

1. **AS-011 - Scenario**: Reintento transparente en emisión bajo clave de idempotencia
   * **Given** que el Veterinario ya envió un `POST /api/v1/sanitary/sacrificios` exitoso con `X-Idempotency-Key = K1`
   * **And** la orden fue creada en estado `PENDIENTE` con `ordenId = O1`
   * **When** el cliente reintenta el mismo `POST` con la misma cabecera `X-Idempotency-Key = K1`
   * **Then** el sistema devuelve la respuesta original almacenada (HTTP `200`/`201` con `ordenId = O1`)
   * **And** no crea una segunda orden ni duplica asientos en `san_auditoria`
   * **And** no publica un nuevo `OrdenSacrificioEmitidaIntegrationEvent`

2. **AS-012 - Scenario**: Reintento transparente en confirmación bajo clave de idempotencia
   * **Given** que el Veterinario ya confirmó exitosamente la ejecución de la orden `O1` con `X-Idempotency-Key = K2`
   * **And** la orden quedó en estado `EJECUTADA`, el lote en cero y el galpón en `vaciado_sanitario`
   * **When** el cliente reintenta el `PATCH /confirmar` con la misma cabecera `X-Idempotency-Key = K2`
   * **Then** el sistema devuelve la respuesta original almacenada
   * **And** no vuelve a solicitar mutaciones al Módulo 1
   * **And** no duplica eventos en `san_outbox`

3. **AS-013 - Scenario**: Rollback total por indisponibilidad del Módulo 1 durante la confirmación
   * **Given** una orden en estado `PENDIENTE` con galpón en `aislamiento` y población > 0
   * **And** el Módulo 1 no responde o retorna timeout al intentar mutar población o estado del galpón
   * **When** el Veterinario confirma la ejecución del sacrificio
   * **Then** el sistema aborta la transacción completa (`Rollback`)
   * **And** la orden permanece en estado `PENDIENTE`
   * **And** la población del lote y el estado del galpón no se alteran
   * **And** no se persiste ningún evento en `san_outbox` ni asiento en `san_auditoria`

4. **AS-014 - Scenario**: Extinción del remanente por mortalidad natural previa a la confirmación
   * **Given** una orden en estado `PENDIENTE` emitida sobre un lote con población `N > 0`
   * **And** entre la emisión y la confirmación la población descendió a `M` (con `M > 0` pero `M < N`) por bajas biológicas naturales
   * **When** el Veterinario confirma la ejecución en campo
   * **Then** el sistema toma la población remanente `M` y ordena al Módulo 1 fijarla en cero
   * **And** registra la orden como `EJECUTADA` con `fechaEjecucion` UTC
   * **And** el evento `SacrificioEjecutadoIntegrationEvent` expone `poblacionExtinguida = M`

5. **AS-015 - Scenario**: Inhibición del reintegro automático por el `DiagnosticoReintegroScheduler`
   * **Given** un lote con orden de sacrificio en estado `PENDIENTE`
   * **And** existe un registro residual de reintegro vencido asociado al diagnóstico origen (Spec 011)
   * **When** el `DiagnosticoReintegroScheduler` evalúa el lote
   * **Then** el scheduler detecta la orden `PENDIENTE` y omite el reintegro automático
   * **And** el galpón permanece en estado `aislamiento`
   * **And** no se transiciona el lote ni se publica evento de reintegro

---

### Edge Cases

* **Mortalidad natural entre la emisión y la confirmación**: Si la población del lote desciende por bajas biológicas entre la emisión y la confirmación en campo, el sistema toma la población remanente al momento de confirmar y la extingue directamente a cero. La orden cubre la totalidad de aves presentes.
* **Indisponibilidad del Módulo 1 durante la confirmación**: Si el Módulo 1 no responde o retorna timeout al intentar mutar la población o el estado del galpón, toda la transacción se aborta (`Rollback`): la orden permanece `PENDIENTE`, la población no se altera y no se encola ningún evento en el outbox.
* **Intervención del Scheduler de Reintegro (`Spec 011`)**: Si un lote con orden de sacrificio en estado `PENDIENTE` tuviese por anomalía una fecha de reintegro previa, el `DiagnosticoReintegroScheduler` comprueba la existencia de la orden de sacrificio y omite el reintegro automático, manteniendo el galpón en `aislamiento`[cite: 4, 7].
* **Indisponibilidad del broker Kafka**: El patrón Transactional Outbox garantiza la persistencia local de la orden y del evento en `san_outbox` bajo la misma transacción SQL; el worker `OutboxRelayScheduler` reintentará el despacho tan pronto se restablezca la conectividad[cite: 1].
* **Reintentos por timeout de red**: El uso de la cabecera `X-Idempotency-Key` intercepta llamadas duplicadas tanto en la emisión como en la confirmación, devolviendo la respuesta original almacenada sin replicar transiciones[cite: 1].

---

## 3. Requirements *(mandatory)*

### Functional Requirements

| ID | Requerimiento Funcional | Justificación Técnica / Trazabilidad |
| :--- | :--- | :--- |
| **FR-001** | La emisión y confirmación de órdenes de sacrificio DEBE estar restringida exclusivamente a usuarios autenticados con rol `VETERINARIO`. | Restricción clínica y regulatoria de inocuidad[cite: 1, 8]. |
| **FR-002** | El sistema DEBE permitir la emisión de la orden únicamente sobre diagnósticos vigentes cuya enfermedad tenga `requiereSacrificioSanitario == true`. | Coherencia con el catálogo nosológico (`Spec 009`) y diagnóstico (`Spec 011`)[cite: 4, 8]. |
| **FR-003** | El galpón referenciado DEBE encontrarse en estado operativo `aislamiento` en el Módulo 1 al momento de emitir la orden. | Precondición sanitaria obligatoria (`Spec 010`)[cite: 4, 8]. |
| **FR-004** | El lote alojado en el galpón DEBE tener población actual mayor a cero (`poblacionActual > 0`) al momento de emitir la orden. | Validación de existencia biológica sujeta a erradicación. |
| **FR-005** | Cada orden DEBE quedar asociada de forma unívoca a un `diagnosticoId`, `galponId` y `loteId`. | Trazabilidad epidemiológica del lote[cite: 4, 8]. |
| **FR-006** | La emisión de la orden NO DEBE alterar la población del lote ni el estado del galpón; la orden se crea en estado `PENDIENTE`. | Separación formal entre instrucción clínica y ejecución material. |
| **FR-007** | Antes de confirmar la ejecución, el sistema DEBE verificar síncronamente que la orden esté en `PENDIENTE`, el galpón continúe en `aislamiento` y la población sea mayor a cero[cite: 4, 7]. | Consistencia de estados pre-ejecución. |
| **FR-008** | Al confirmar la ejecución, el sistema DEBE ordenar al Módulo 1 fijar la población actual del lote en cero (`0`). | Erradicación biológica efectiva del inventario. |
| **FR-009** | Al confirmar la ejecución, el sistema DEBE ordenar al Módulo 1 transicionar el galpón de `aislamiento` a `vaciado_sanitario`. | Habilitación de la etapa de desinfección y bioseguridad. |
| **FR-010** | Al confirmar la ejecución, el sistema DEBE marcar la orden como `EJECUTADA` y registrar la marca temporal UTC del momento exacto (`fechaEjecucion`). | Inmutabilidad de auditoría temporal[cite: 1]. |
| **FR-011** | La confirmación DEBE ejecutarse como una operación atómica: mutación de población + transición de galpón + cierre de orden + outbox + auditoría bajo un mismo `@Transactional`. | Integridad transaccional ACID[cite: 1]. |
| **FR-012** | Al confirmar la ejecución, el sistema DEBE persistir atómicamente el evento `SacrificioEjecutadoIntegrationEvent` en `san_outbox` bajo el tópico `sanitary.sacrifice.executed.v1`. | Entrega asíncrona garantizada vía Transactional Outbox[cite: 1]. |
| **FR-013** | Cada emisión y cada confirmación DEBE registrar un asiento inmutable en `san_auditoria` con usuario, matrícula profesional, `ordenId`, `galponId`, `loteId` y justificación técnica. | Trazabilidad legal zoosanitaria[cite: 1]. |
| **FR-014** | Queda ESTRICTAMENTE PROHIBIDO el borrado físico (`DELETE` en SQL) sobre las tablas `san_ordenes_sacrificio` y `san_auditoria`. | Mandato de conservación de registros sanitarios[cite: 1]. |
| **FR-015** | El sistema DEBE implementar control de concurrencia optimista (`@Version`) sobre la orden y admitir cabeceras `X-Idempotency-Key` retenidas durante 24 horas. | Prevención de colisiones concurrentes y reintentos[cite: 1]. |
| **FR-016** | Toda orden comprende el 100% de la población del lote (`alcance = TOTAL`); el sistema NO DEBE solicitar ni permitir el ingreso de cantidades parciales. | Alcance total mandatorio de erradicación. |

### Non-Functional Requirements

| ID | Categoría | Requerimiento | Métrica |
| :--- | :--- | :--- | :--- |
| **NFR-001** | Rendimiento | Latencia de emisión, confirmación, persistencia atómica y respuesta idempotente cacheada | < 1 s (p95) en confirmación; < 100 ms (p95) en respuestas idempotentes servidas desde caché |
| **NFR-002** | Seguridad | Acceso restringido exclusivamente al rol VETERINARIO | HTTP 403 Forbidden a otros roles |
| **NFR-003** | Integridad | Escritura atómica (orden + outbox + auditoría) bajo `@Transactional` | 100% de consistencia local |
| **NFR-004** | Persistencia | Prohibición estricta de sentencias `DELETE` SQL en base de datos | 0% borrados físicos |
| **NFR-005** | Disponibilidad | Si el Módulo 1 no responde durante la confirmación, la transacción se aborta completamente (Rollback) | 0% de inconsistencias por fallo cross-context |
| **NFR-006** | Consistencia | El `DiagnosticoReintegroScheduler` (Spec 011) debe omitir reintegros cuando exista orden de sacrificio `PENDIENTE` | 100% de inhibiciones correctas |

---

### Key Entities

```text
+---------------------------------------------------------------------------------+
|                                 <<Aggregate Root>>                              |
|                              OrdenSacrificioSanitario                           |
+---------------------------------------------------------------------------------+
| - id: UUID                                                                      |
| - diagnosticoId: UUID                                (Ref. Spec 011)            |
| - galponId: UUID                                     (Ref. Externa Módulo 1)    |
| - loteId: UUID                                       (Ref. Externa Módulo 1)    |
| - veterinarioId: UUID                                                           |
| - alcance: AlcanceOrden                              (TOTAL)                    |
| - estado: EstadoOrden                                (PENDIENTE, EJECUTADA)     |
| - fechaEmision: Instant                                                         |
| - fechaEjecucion: Instant [0..1]                     (Nullable, asignado al fin)|
| - observacionesEmision: String                       (Mínimo 10 caracteres)     |
| - observacionesEjecucion: String [0..1]              (Nullable)                 |
| - version: Integer                                   (@Version Optimistic Lock) |
+---------------------------------------------------------------------------------+
```

* **OrdenSacrificioSanitario** *(Aggregate Root)*: Entidad médica central que orquesta la directriz de sacrificio total y su ciclo de vida (`PENDIENTE` $\to$ `EJECUTADA`).
* **Diagnóstico**: Expediente clínico del `Spec 011` que sustenta la orden[cite: 4, 8]. Precondición estricta de existencia con patología letal[cite: 4, 8].
* **Enfermedad**: Entidad del catálogo nosológico (`Spec 009`) cuyo indicador `requiereSacrificioSanitario == true` autoriza la emisión[cite: 4, 8].
* **Galpón y Lote**: Entidades externas bajo propiedad del Módulo 1[cite: 8]. El Módulo 2 verifica su viabilidad y comanda las transiciones (`aislamiento` $\to$ `vaciado_sanitario` y `poblacion > 0` $\to$ `poblacion = 0`).
* **OrdenSacrificioEmitidaIntegrationEvent**: Evento publicado tras la emisión (`eventId`, `aggregateId`, `diagnosticoId`, `galponId`, `loteId`, `veterinarioId`, `occurredOn`).
* **SacrificioEjecutadoIntegrationEvent**: Evento publicado tras la ejecución material (`eventId`, `aggregateId`, `galponId`, `loteId`, `poblacionExtinguida`, `occurredOn`).
* **Auditoría Sanitaria (`san_auditoria`)**: Bitácora append-only inmutable de control legal[cite: 1].
* **Transactional Outbox (`san_outbox`)**: Tabla relacional local para garantizar publicación confiable hacia Kafka[cite: 1].

---

## 4. Success Criteria *(mandatory)*

### Measurable Outcomes

| ID | Criterio de Éxito | Métrica Objetivo | Validación Técnica |
| :--- | :--- | :--- | :--- |
| **SC-001** | El 100% de las órdenes emitidas corresponde a diagnósticos activos con `requiereSacrificioSanitario == true`. | 100% de cumplimiento | Validación en caso de uso contra `DiagnosticoQueryPort` y `EnfermedadQueryPort`[cite: 4, 8]. |
| **SC-002** | El 100% de las órdenes emitidas comprende la totalidad del lote sin admitir ingresos numéricos parciales. | Cero órdenes parciales | Invariante en Aggregate Root (`alcance = TOTAL`). |
| **SC-003** | El 100% de las órdenes en estado `PENDIENTE` conserva la población del lote y el estado `aislamiento` sin modificaciones. | Cero mutaciones anticipadas | Verificación de persistencia en Módulo 1 y tabla local[cite: 4, 7]. |
| **SC-004** | El 100% de las órdenes confirmadas fija la población en cero y transiciona el galpón a `vaciado_sanitario`. | 100% de efectividad | Test de integración con Testcontainers en `ConfirmarSacrificioUseCase`. |
| **SC-005** | El 100% de los intentos de emisión o confirmación ejecutados por roles distintos a `VETERINARIO` son bloqueados. | Cero accesos no autorizados | Prueba de seguridad HTTP 403 Forbidden[cite: 1]. |
| **SC-006** | El 95% de las confirmaciones válidas procesa la transacción de población, galpón y orden en menos de 1 segundo. | Latencia < 1 s | Pruebas de carga bajo concurrencia con Gatling. |
| **SC-007** | Cero duplicidad (0%) en órdenes o eventos outbox ante reintentos de red. | Cero duplicados | Validación con `X-Idempotency-Key` y `@Version`[cite: 1]. |
| **SC-008** | Cero incidentes (0%) de borrado físico (`DELETE` SQL) sobre órdenes o asientos de auditoría. | 100% de persistencia | Revocación de permisos `DELETE` a nivel de base de datos[cite: 1]. |
| **SC-009** | El 100% de los eventos de integración se resguarda en `san_outbox` dentro de la misma transacción local de base de datos. | Atomicidad total | Verificación transaccional rollback ante fallos simulados[cite: 1]. |

---

## 5. Dependencies & Cross-References

* **`Spec 008 – Registrar medicación`**: No aplica; las patologías letales prohíben esquemas terapéuticos[cite: 4, 8].
* **`Spec 009 – Registrar enfermedad`**: Provee el catálogo nosológico y el indicador `requiereSacrificioSanitario`[cite: 4, 8].
* **`Spec 010 – Validar aislamiento`**: Precondición base que asegura que el galpón esté aislado preventivamente[cite: 4, 8].
* **`Spec 011 – Diagnosticar galpón`**: Precondición directa. La relación UML es de extensión (`<<extend>>`): el sacrificio extiende el diagnóstico cuando se dictamina una enfermedad mortal[cite: 4, 8].
* **Módulo 1 (Control de Galpones y Lotes)**: Bounded Context propietario de `Galpón` y `Lote`[cite: 8]. El Módulo 2 lee los atributos y comanda las mutaciones finales (`aislamiento` $\to$ `vaciado_sanitario` y `poblacionActual` $\to$ `0`).

---

## 6. Notes

* **Trazabilidad Bidireccional**: Cada requerimiento `FR-001` a `FR-016` mapea punto a punto contra una tarea del plan técnico (`T0xx`) y contra una clase o invariante en la arquitectura hexagonal[cite: 1].
* **Separación Emisión vs. Ejecución**: La emisión es un acto clínico que formaliza la instrucción legal. La ejecución es el acto material en campo. Este desacoplamiento en dos tiempos previene que contingencias logísticas alteren prematuramente los inventarios biológicos de la granja.
* **Inhibición de Reintegros Automáticos**: Si un lote con orden de sacrificio `PENDIENTE` tuviese registros residuales en el scheduler del `Spec 011`, dicho componente omite cualquier intento de reintegro automático mientras la orden no sea cerrada[cite: 4, 7].
* **Notificación al Cliente Frontend**: El payload `OrdenSacrificioResponse` expone los atributos `estado` (`PENDIENTE` / `EJECUTADA`) y `fechaEjecucion` (nullable), permitiendo a la interfaz de usuario mostrar el estado del procedimiento y habilitar el botón de confirmación únicamente cuando la orden esté pendiente.

## UI Component Mapping (Prototipo ↔ Spec)

Esta sección documenta la correspondencia estricta entre los controles del prototipo visual oficial (`docs/prototype/Veterinario/012-OrdenarSacrificioSanitario.png`) y los requerimientos funcionales del sistema. Todo componente visual debe responder a un FR y ningún comportamiento fuera de este catálogo está permitido.

### Pantalla: Orden de sacrificio sanitario
- **Prototipo de Referencia**: `docs/prototype/Veterinario/012-OrdenarSacrificioSanitario.png`
- **Actor Exclusivo**: `VETERINARIO` (FR-001)

| Componente UI | Tipo | FR Asociado | Comportamiento Técnico y Validación |
| :--- | :--- | :--- | :--- |
| **Stat Cards de Resumen** | Metric Cards (4) | FR-003, FR-004, FR-006, FR-007 | Muestran: diagnósticos letales, órdenes pendientes, lotes con población, galpones en aislamiento |
| **Barra de Filtros** | Filter Bar | FR-003, FR-004 | Búsqueda por galpón, lote, diagnóstico y estado de orden |
| **Tabla de Diagnósticos Letales** | Data Grid | FR-002, FR-003, FR-005 | Lista diagnóstico, galpón, lote, población y estado operativo |
| **Botón "Emitir orden"** | Button (Primary) | FR-001, FR-002, FR-003, FR-006 | Abre el panel de emisión para diagnósticos mortales elegibles |
| **Panel "Emitir orden de sacrificio"** | Side Panel | FR-001, FR-002, FR-005, FR-006, FR-013 | Contiene el formulario y la transacción inicial de emisión |
| **Textarea "Observaciones de bioseguridad"** | Textarea | FR-006, FR-013 | Requiere justificación técnica con longitud mínima declarada por la regla de negocio |
| **Botón "Confirmar emisión"** | Button (Primary) | FR-006, FR-011, FR-012 | Ejecuta la creación de la orden y persiste evento + auditoría |
| **Badge "PENDIENTE"** | Status Badge | FR-006, FR-007 | Refleja que la orden fue emitida pero aún no ejecutada |
| **Botón "Confirmar ejecución"** | Button (Primary) | FR-007, FR-008, FR-009, FR-010, FR-011, FR-014, FR-015 | Ejecuta la mutación final de población y galpón en un único commit idempotente; prohíbe cualquier borrado físico |
| **Textarea "Observaciones de ejecución"** | Textarea | FR-010, FR-013 | Registra la evidencia del procedimiento material |
| **Badge "EJECUTADA"** | Status Badge | FR-010 | Refleja cierre concluyente y estado final del procedimiento |
| **Botón "Cancelar"** | Button (Secondary) | FR-001 | Cierra el panel sin mutar la orden |

### Elementos Prohibidos en la Pantalla (Guardrails Sanitarios)
- ❌ **Botón "Eliminar" / "Borrar"**: Terminantemente prohibido (`FR-014`). No existe eliminación física de órdenes ni auditoría.
- ❌ **Sacrificio parcial**: Prohibido (`FR-016`); la UI no acepta cantidades ni fracciones del lote.
- ❌ **Campos huérfanos**: Prohibido añadir campos que no pertenezcan al dominio funcional de sacrificio sanitario.

### Estados Operativos del Formulario
- **Validación inline**: Resaltado de campos obligatorios y bloqueo si el diagnóstico no requiere sacrificio, el galpón no está aislado o la población es cero.
- **Transacción en progreso**: Bloqueo de controles de envío durante el commit atómico (`san_ordenes_sacrificio` + `san_outbox` + `san_auditoria`).
- **Colisión de Concurrencia**: Modal informativo ante HTTP 409 (`OptimisticLockException` o `OrdenYaEjecutadaException`) solicitando recarga de datos.
- **Idempotencia de reintentos**: Si el cliente reenvía la misma petición con la misma `X-Idempotency-Key` (ventana de 24 h), la UI presenta la respuesta original sin abrir un nuevo diálogo de confirmación.