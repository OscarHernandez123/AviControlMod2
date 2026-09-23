# Feature Specification: Diagnosticar un galpón

**Created**: 2026-09-04  
**Last Updated**: 2026-09-22  
**Spec**: `docs/specs/011-DiagnosticarGalpon.md`  
**Módulo**: 2 – Sanidad y Bioseguridad  
**Actor Principal**: `VETERINARIO`  
**Precondición Obligatoria**: Galpón en estado `aislamiento` con lote activo (`Spec 010` completado)

---

## 1. Summary

El módulo **Diagnosticar un galpón** formaliza el expediente clínico emitido por el Médico Veterinario sobre una parvada previamente puesta en cuarentena (`Spec 010`). La resolución del diagnóstico se bifurca en dos caminos clínicos mutuamente excluyentes según el indicador nosológico `requiereSacrificioSanitario` de la enfermedad dictaminada (`Spec 009`):

* **Rama Terapéutica** (`requiereSacrificioSanitario == false`): Exige la asociación de un protocolo de tratamiento estandarizado (`Spec 008`) y calcula de forma determinística la **fecha de reintegro** a producción (`fechaDiagnostico + diasTratamiento`).
* **Rama Mortal / Erradicación** (`requiereSacrificioSanitario == true`): Prescinde de medicación (`medicacionId = null`), no proyecta fecha de reintegro y **habilita la emisión de la orden de sacrificio sanitario total** (`Spec 012`, relación `<<extend>>`).

Adicionalmente, un proceso en segundo plano (`DiagnosticoReintegroScheduler`) evalúa los diagnósticos terapéuticos y ejecuta de manera autónoma la transición del galpón a estado `productiva` en el Módulo 1 al alcanzarse la fecha de reintegro, siempre y cuando la cuarentena no haya sido alterada por contingencias sanitarias intermedias.

**Restricciones globales mandatorias**:
* 🔒 **Prohibición de borrado físico**: Ninguna sentencia `DELETE` SQL está permitida sobre diagnósticos ni asientos de auditoría.
* 🔒 **Atomicidad e Integridad Transaccional**: Registro simultáneo de `san_diagnosticos` + `san_outbox` + `san_auditoria` en una única transacción de base de datos (`@Transactional`).
* 🔒 **Seguridad y Atribución**: Acceso restringido exclusivamente a usuarios autenticados con rol `VETERINARIO`.
* 🔒 **Concurrencia e Idempotencia**: Control optimista vía `@Version` sobre el expediente y cabeceras `X-Idempotency-Key` retenidas durante 24 horas.

---

## 2. User Scenarios & Testing *(mandatory)*

### User Story 1 - Registro del Dictamen Clínico de la Parvada Aislada (Priority: P1)

**Como** Médico Veterinario de la granja,  
**Quiero** registrar el diagnóstico de un lote alojado en un galpón en estado `aislamiento`, vinculando la enfermedad confirmada y seleccionando el protocolo de medicación o habilitando el sacrificio sanitario,  
**Para** prescribir la terapia oficial con fecha calculada de reintegro sanitario o proceder a la contención biosegura por sacrificio total.

**Why this priority**: Es el núcleo resolutivo de la atención médica veterinaria. Sin este dictamen formal, el galpón queda estancado en cuarentena indefinida, no se pueden dispensar tratamientos en campo y se bloquea la erradicación sanitaria en caso de patógenos letales.

**Independent Test**:
* **Caso A (Terapéutico)**: Petición `POST /api/v1/sanitary/diagnosticos` con un galpón en `aislamiento`, enfermedad tratable (`requiereSacrificioSanitario = false`) y medicación de 5 días. El sistema persiste el diagnóstico, calcula `fechaReintegro = fechaDiagnostico + 5 días`, persiste el evento en outbox y conserva el galpón en `aislamiento`.
* **Caso B (Mortal)**: Petición `POST` con galpón en `aislamiento` y enfermedad `requiereSacrificioSanitario = true`. El sistema no admite `medicacionId`, deja `fechaReintegro = null`, emite el evento correspondiente y retorna la habilitación del `Spec 012`.

#### Acceptance Scenarios

1. **Scenario: Dictamen clínico exitoso con enfermedad tratable y medicación estándar**
   * **Given** que el galpón referenciado tiene estado vigente `aislamiento` en el Módulo 1 y aloja un lote activo
   * **And** el Veterinario selecciona una enfermedad activa con `requiereSacrificioSanitario == false` (`Spec 009`)
   * **And** selecciona una pauta de medicación activa con `diasTratamiento = 5` (`Spec 008`)
   * **When** el Veterinario confirma el registro del diagnóstico
   * **Then** el sistema almacena el expediente clínico con `enfermedadId` y `medicacionId`
   * **And** calcula y fija `fechaReintegro = fechaDiagnostico + 5 días` a medianoche UTC
   * **And** persiste atómicamente el evento `DiagnosticoRegistradoIntegrationEvent` en `san_outbox` bajo el tópico `sanitary.diagnosis.registered.v1`
   * **And** registra la firma inmutable en `san_auditoria` con matrícula profesional y justificación técnica
   * **And** mantiene al galpón en estado `aislamiento` durante el periodo terapéutico

2. **Scenario: Dictamen clínico con patología letal que exige sacrificio sanitario**
   * **Given** un galpón en estado `aislamiento` con lote activo
   * **And** el Veterinario selecciona una enfermedad activa con `requiereSacrificioSanitario == true` (ej. Newcastle velogénico o Influenza Aviar)
   * **When** el Veterinario confirma el diagnóstico sin asociar medicación
   * **Then** el sistema registra el diagnóstico con `medicacionId = null` y `fechaReintegro = null`
   * **And** persiste el evento de integración en `san_outbox`
   * **And** registra el asiento en `san_auditoria`
   * **And** retorna en el payload la bandera `habilitaSacrificio = true`, habilitando de inmediato la emisión de la orden de sacrificio sanitario total (`Spec 012`)

3. **Scenario: Reintegro automatizado a producción tras cumplir la pauta médica**
   * **Given** un diagnóstico clínico vigente con `fechaReintegro = 2026-09-09` y el galpón continúa en estado `aislamiento`
   * **When** el proceso programado `DiagnosticoReintegroScheduler` se ejecuta y comprueba que `hoy >= fechaReintegro`
   * **Then** el sistema invoca al Módulo 1 para transicionar el estado del galpón de `aislamiento` a `productiva`
   * **And** marca el diagnóstico sanitario como `CERRADO`
   * **And** persiste el evento `GalponReintegradoIntegrationEvent` en `san_outbox` bajo el tópico `sanitary.galpon.reintegrated.v1`
   * **And** asienta la traza automática del sistema en `san_auditoria`

4. **Scenario: Conservación de cuarentena cuando la fecha de reintegro es futura**
   * **Given** un diagnóstico registrado cuya `fechaReintegro` aún no se ha cumplido (`hoy < fechaReintegro`)
   * **When** el scheduler periódico evalúa los expedientes
   * **Then** el sistema no efectúa mutaciones de estado y preserva el galpón en `aislamiento`

5. **Scenario: Rechazo de diagnóstico sobre galpón no aislado**
   * **Given** que el galpón consultado en el Módulo 1 se encuentra en estado `productiva`, `en_cosecha` o `vacio`
   * **When** el Veterinario intenta emitir un diagnóstico
   * **Then** el sistema bloquea el guardado, retorna HTTP `409 Conflict` con código `GalponNoAisladoException` y no crea el expediente

6. **Scenario: Rechazo por omisión de enfermedad clínica**
   * **Given** un galpón validado en estado `aislamiento`
   * **When** el Veterinario envía el formulario omitiendo el identificador `enfermedadId`
   * **Then** el sistema responde con HTTP `400 Bad Request` indicando que la enfermedad nosológica es obligatoria

7. **Scenario: Rechazo por enfermedad tratable sin medicación prescrita**
   * **Given** la selección de una patología con `requiereSacrificioSanitario == false`
   * **When** el Veterinario intenta guardar el diagnóstico sin suministrar un `medicacionId` válido
   * **Then** el sistema detiene la transacción, responde con HTTP `400 Bad Request` (`MedicacionRequeridaException`) y no altera el expediente

8. **Scenario: Denegación de acceso por rol no veterinario**
   * **Given** un usuario autenticado con rol `TRABAJADOR` o `ADMINISTRADOR`
   * **When** intenta emitir o confirmar una orden diagnóstica
   * **Then** el sistema intercepta la solicitud, responde con HTTP `403 Forbidden` y no registra datos clínicos

---

### User Story 2 - Idempotencia y Blindaje contra Diagnósticos Duplicados (Priority: P2)

**Como** auditor de bioseguridad y aseguramiento de calidad,  
**Quiero** garantizar que el registro de diagnósticos sea estrictamente idempotente y atómico,  
**Para** evitar duplicidades en expedientes clínicos, superposición de fechas de reintegro o despacho reiterado de eventos ante reintentos de red.

**Why this priority**: Un doble diagnóstico generaría colisión en los cálculos de cuarentena del lote, desfasaría el seguimiento de periodos de retiro farmacológico y generaría inconsistencias ante las auditorías del ICA.

**Independent Test**: Se envía el mismo comando de diagnóstico dos veces consecutivas bajo la misma cabecera `X-Idempotency-Key`. Se verifica que el segundo llamado retorna la respuesta original almacenada en caché sin crear una segunda fila en `san_diagnosticos`, sin regenerar eventos en `san_outbox` y sin duplicar la auditoría.

#### Acceptance Scenarios

1. **Scenario: Reintento transparente bajo clave de idempotencia técnica**
   * **Given** un comando de diagnóstico procesado exitosamente bajo la cabecera `X-Idempotency-Key: IDEMP-DIAG-5501`
   * **When** el cliente HTTP retransmite la misma solicitud debido a un timeout transitorio de la conexión
   * **Then** el filtro de idempotencia intercepta la petición
   * **And** retorna la respuesta original almacenada (HTTP 201 Created con el payload original)
   * **And** no crea registros duplicados en base de datos ni emite mensajes redundantes en Kafka

2. **Scenario: Rechazo de nuevo diagnóstico sobre lote con diagnóstico activo**
   * **Given** un galpón en aislamiento que ya cuenta con un diagnóstico clínico en estado `ACTIVO`
   * **When** un veterinario intenta asentar un segundo diagnóstico sin haber cerrado o revertido el anterior
   * **Then** el sistema responde con HTTP `409 Conflict` informando la preexistencia de un expediente diagnóstico abierto

---

### Edge Cases

* **Cambio de estado del galpón durante la confección del diagnóstico**: Si el galpón deja de estar en `aislamiento` en el Módulo 1 mientras el Veterinario diligencia la pantalla, el sistema revalida el estado antes de abrir la transacción local y aborta con `EstadoGalponIncompatibleException`.
* **Modificación concurrente de la pauta de medicación**: Si la duración en días de la medicación asignada fue editada en el catálogo (`Spec 008`) antes de confirmar el diagnóstico, el sistema toma la versión vigente al momento del guardado para computar la `fechaReintegro` de forma exacta.
* **Galpón con estado anómalo al cumplirse la fecha de reintegro**: Si al llegar la `fechaReintegro` el galpón ya no está en `aislamiento` (ej. trasladado forzosamente o vaciado por contingencia), el scheduler no sobreescribe el estado en el Módulo 1, marca el diagnóstico como cerrado y registra una alerta en `san_auditoria`.
* **Interrupción del servicio durante el vencimiento de cuarentenas**: Si el microservicio experimenta una caída durante la ventana de ejecución del scheduler, al reiniciarse procesa de forma acumulativa todos los diagnósticos cuya `fechaReintegro <= NOW()`, regularizando las transiciones pendientes.
* **Indisponibilidad del broker Kafka**: El patrón Transactional Outbox garantiza la persistencia local de la entidad y del evento en `san_outbox` bajo la misma transacción; el worker en segundo plano reintenta la publicación hacia Kafka tan pronto como la conectividad se restablezca.

---

## 3. Requirements *(mandatory)*

### Functional Requirements

| ID | Requerimiento Funcional | Justificación Técnica / Trazabilidad |
| :--- | :--- | :--- |
| **FR-001** | El registro de diagnósticos clínicos DEBE estar restringido exclusivamente a usuarios autenticados con rol `VETERINARIO`. | Restricción clínica y regulatoria. |
| **FR-002** | El sistema DEBE validar de forma síncrona contra el Módulo 1 que el estado operativo vigente del galpón sea estrictamente `aislamiento`. | Precondición de aislamiento obligatoria (`Spec 010`). |
| **FR-003** | Todo diagnóstico DEBE vincularse obligatoriamente a un único `galponId` y al `loteId` activo que se encuentre alojado en él. | Trazabilidad epidemiológica del lote. |
| **FR-004** | El diagnóstico DEBE asociarse formalmente a una `Enfermedad` existente en el catálogo nosológico en estado activa. | Consistencia con el catálogo maestro (`Spec 009`). |
| **FR-005** | Si la enfermedad tiene `requiereSacrificioSanitario == false`, el sistema DEBE exigir la selección de una `Medicación` activa en el catálogo terapéutico. | Pauta médica de recuperación (`Spec 008`). |
| **FR-006** | Para enfermedades con `requiereSacrificioSanitario == false`, el sistema DEBE calcular automáticamente `fechaReintegro = fechaDiagnostico + diasTratamiento`. | Automatización del periodo de cuarentena. |
| **FR-007** | Si `requiereSacrificioSanitario == true`, el sistema DEBE forzar `medicacionId = null`, omitir la `fechaReintegro` y habilitar la orden de sacrificio sanitario. | Protocolo de erradicación biosegura (`Spec 012`). |
| **FR-008** | La `fechaDiagnostico` DEBE ser asignada por el sistema con la marca temporal UTC del momento exacto de la confirmación. | Inmutabilidad de auditoría temporal. |
| **FR-009** | El componente `DiagnosticoReintegroScheduler` DEBE evaluar periódicamente los diagnósticos activos para detectar periodos terapéuticos cumplidos. | Automatización reactiva de reintegro. |
| **FR-010** | El scheduler DEBE ordenar al Módulo 1 la transición del galpón de `aislamiento` a `productiva` única y exclusivamente si el galpón continúa en `aislamiento`. | Preservación del estado e idempotencia. |
| **FR-011** | La transición automática a `productiva` DEBE ejecutarse de forma idempotente, cerrando el expediente diagnóstico (`estado = CERRADO`). | Integridad de ciclo de vida del diagnóstico. |
| **FR-012** | Si el galpón reporta un estado operativo distinto de `aislamiento` al cumplirse la fecha de reintegro, el sistema NO DEBE alterar el estado en el Módulo 1. | Blindaje contra sobreescritura de estados concurrentes. |
| **FR-013** | Al confirmarse el guardado, el sistema DEBE persistir de forma atómica en `san_outbox` el evento `DiagnosticoRegistradoIntegrationEvent` bajo el tópico `sanitary.diagnosis.registered.v1`. | Publicación garantizada mediante Transactional Outbox. |
| **FR-014** | Cada diagnóstico DEBE asentar un registro inmutable en `san_auditoria` con usuario responsable, matrícula veterinaria, galpón, lote, enfermedad y medicación asociada. | Trazabilidad legal y de inocuidad alimentaria. |
| **FR-015** | Queda ESTRICTAMENTE PROHIBIDO el borrado físico (`DELETE` en SQL) sobre las tablas `san_diagnosticos` y `san_auditoria`. | Cumplimiento regulatorio agropecuario. |
| **FR-016** | El sistema DEBE aplicar control de concurrencia optimista (`@Version`) sobre el expediente y admitir cabeceras `X-Idempotency-Key` retenidas durante 24 horas. | Integridad ante reintentos de red y concurrencia. |

---

### Key Entities

```text
+---------------------------------------------------------------------------------+
|                                 <<Aggregate Root>>                              |
|                                    Diagnostico                                  |
+---------------------------------------------------------------------------------+
| - id: UUID                                                                      |
| - galponId: UUID                                     (Ref. Externa Módulo 1)    |
| - loteId: UUID                                       (Ref. Externa Módulo 1)    |
| - enfermedadId: UUID                                 (Ref. Spec 009)            |
| - medicacionId: UUID [0..1]                          (Ref. Spec 008, Nullable)  |
| - veterinarioId: UUID                                                           |
| - fechaDiagnostico: Instant                                                     |
| - fechaReintegro: Instant [0..1]                     (Calculado, Nullable)      |
| - observaciones: String                                                         |
| - estado: EstadoDiagnostico                          (ACTIVO, CERRADO)          |
| - version: Integer                                   (@Version Optimistic Lock) |
+---------------------------------------------------------------------------------+
```

* **Diagnostico** *(Aggregate Root)*: Entidad médica central del subsistema. Orquesta la relación entre el lote afectado, la enfermedad nosológica y el esquema de tratamiento.
* **Enfermedad**: Entidad del catálogo nosológico (`Spec 009`). Provee el atributo binario `requiereSacrificioSanitario` que decide la bifurcación clínica.
* **Medicación**: Entidad del catálogo terapéutico (`Spec 008`). Provee la dosimetría y los `diasTratamiento` para calcular la duración de la cuarentena.
* **Galpón y Lote**: Entidades externas pertenecientes al Bounded Context del Módulo 1. El Módulo 2 valida su existencia y gestiona sus estados sanitarios.
* **DiagnosticoRegistradoIntegrationEvent**: Contrato tipado despachado a Kafka (`eventId`, `aggregateId`, `galponId`, `loteId`, `enfermedadId`, `medicacionId`, `fechaReintegro`, `occurredOn`).
* **Auditoría Sanitaria (`san_auditoria`)**: Asiento inmutable append-only de responsabilidad legal profesional.
* **Transactional Outbox (`san_outbox`)**: Tabla transaccional para garantizar entrega de mensajería asíncrona sin transacciones distribuidas 2PC.

---

## 4. Success Criteria *(mandatory)*

### Measurable Outcomes

| ID | Criterio de Éxito | Métrica Objetivo | Validación Técnica |
| :--- | :--- | :--- | :--- |
| **SC-001** | El 100% de los diagnósticos se asocia formalmente a galpones confirmados en estado `aislamiento`. | 100% de cumplimiento | Intercepción en Use Case contra `GalponQueryPort`. |
| **SC-002** | El 100% de los diagnósticos registra una clave foránea válida hacia una `Enfermedad` activa. | Cero registros huérfanos | Constraint relacional y validación de dominio (`Spec 009`). |
| **SC-003** | El 100% de los diagnósticos con patología tratable computa con exactitud matemática `fechaReintegro = fechaDiagnostico + diasTratamiento`. | 100% de exactitud | Test unitario de Aggregate Root y Test de Integración. |
| **SC-004** | El 100% de los diagnósticos mortales prescinde de medicación y habilita la orden de sacrificio. | Cero asignaciones erróneas | Verificación de invariante en Aggregate Root (`Spec 012`). |
| **SC-005** | La latencia total para validar y persistir atómicamente el diagnóstico es inferior a 250 milisegundos. | Latencia < 250 ms | Pruebas de rendimiento con Gatling bajo concurrencia. |
| **SC-006** | El 100% de los intentos de registro por usuarios sin rol `VETERINARIO` son bloqueados. | Cero accesos indebidos | Test de seguridad de capa REST (HTTP 403 Forbidden). |
| **SC-007** | El 100% de los galpones en `aislamiento` con tratamiento vencido es transicionado a `productiva` por el scheduler. | 100% de efectividad | Test de integración con Testcontainers del job programado. |
| **SC-008** | Cero duplicidad (0%) en registros de diagnósticos o eventos outbox ante reintentos de red. | Cero duplicados | Validación con `X-Idempotency-Key` y `@Version`. |
| **SC-009** | Cero incidentes (0%) de borrado físico (`DELETE` SQL) sobre diagnósticos o trazas de auditoría. | 100% de persistencia | Verificación de permisos a nivel de usuario en base de datos. |
| **SC-010** | El 100% de los eventos de integración se resguarda en `san_outbox` dentro de la misma transacción local. | Atomicidad total | Verificación transaccional rollback con Testcontainers. |

---

## 5. Dependencies & Cross-References

* **`Spec 008 – Registrar medicación`**: Provee el catálogo de pautas terapéuticas y la duración en días de cada tratamiento.
* **`Spec 009 – Registrar enfermedad`**: Provee el catálogo nosológico y el indicador de letalidad `requiereSacrificioSanitario`.
* **`Spec 010 – Validar aislamiento`**: Precondición operativa obligatoria (`aislamiento`); este caso de uso es incluido de forma directa (`<<include>>`) tras aislar el galpón.
* **`Spec 012 – Ordenar sacrificio sanitario`**: Extensión funcional (`<<extend>>`) habilitada únicamente cuando la patología diagnosticada exige sacrificio total de la parvada.
* **Módulo 1 (Control de Galpones y Lotes)**: Proveedor de las entidades físicas externas `Galpón` y `Lote`, y receptor de los cambios de estado operativo (`productiva` $\leftrightarrow$ `aislamiento`).

---

## 6. Notes

* **Trazabilidad Bidireccional**: Cada requerimiento `FR-001` a `FR-016` mapea punto a punto contra una tarea del plan técnico (`T0xx`) y contra una clase o invariante en la arquitectura hexagonal.
* **Cálculo de Fechas y Zonas Horarias**: La `fechaReintegro` se computa normalizada a las 23:59:59 UTC del último día de tratamiento farmacológico para garantizar que la parvada complete las 24 horas del último ciclo antes de su retorno a producción.
* **Automatización del Reintegro**: El componente `DiagnosticoReintegroScheduler` opera como un comando de aplicación interno desacoplado del API HTTP, ejecutándose periódicamente mediante `@Scheduled(cron = "0 0 1 * * ?")` o intervalos configurables vía `application.yml`.
* **Notificación al Cliente Frontend**: El payload `DiagnosticoResponse` retorna los atributos calculados `fechaReintegro` y el booleano `habilitaSacrificio`, permitiendo a la interfaz de usuario redirigir al veterinario a la pantalla del `Spec 012` si la patología es mortal o retornar a la bandeja de monitoreo.