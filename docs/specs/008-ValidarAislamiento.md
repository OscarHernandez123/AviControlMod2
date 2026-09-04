# Feature Specification: Validar Aislamiento (CU-VET-001)

**Created**: 2026-09-01  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Confirmación Formal de Aislamiento Sanitario (Priority: P1)

Como Médico Veterinario asignado y autorizado en la granja, quiero evaluar solicitudes de aislamiento sanitario (preventivas o emergentes) y registrar un dictamen médico inmutable de confirmación con severidad y fecha estimada de finalización, para activar las medidas de contención biológica e impedir de inmediato el movimiento, traslado o despacho a faena del lote.

**Why this priority**: Es la capacidad medular del actor veterinario; sin esta confirmación estructurada, no existe amparo técnico ni bloqueo automatizado en planta de faena para evitar contingencias sanitarias.

**Independent Test**: Se valida ejecutando el comando de confirmación sobre un galpón con lote activo en estado `SOLICITADO` y población > 0. Se verifica que el aislamiento transicione a `ACTIVO`, el galpón a `AISLADO`, se publique `AislamientoConfirmado`, se emita el evento de integración y se persista la auditoría inmutable.

**Acceptance Scenarios**:

1. **Scenario**: Confirmación exitosa de aislamiento sanitario con pre-registro previo
   - **Given** un Galpón "G-01" en Granja "GR-001" con Lote "L-100" activo
   - **And** el lote registra una población viva de 8500 aves verificada en inventario
   - **And** existe una solicitud de aislamiento "AIS-001" en estado "SOLICITADO"
   - **And** la patología "ENF-010" figura con estado activo en el catálogo nosológico
   - **When** el Veterinario emite la confirmación con severidad "CRITICO", fecha fin estimada "2026-09-07", tarjeta profesional "MP-VET-88492-CO" y criterio clínico "Cuadro respiratorio severo"
   - **Then** el estado del aislamiento "AIS-001" pasa a "ACTIVO"
   - **And** el estado sanitario del galpón "G-01" pasa a "AISLADO"
   - **And** se despacha el evento de dominio "AislamientoConfirmado"
   - **And** se despacha el evento de integración "GalponAisladoSanitariamenteIntegrationEvent" para bloquear cosecha en Módulo 3
   - **And** se registra una entrada inmutable append-only en la tabla "san_auditoria"
   - **And** el sistema responde con código HTTP 200/201 con payload JSON validado

2. **Scenario**: Confirmación directa por inspección presencial en campo (Flujo Alterno FA-1)
   - **Given** un Galpón con Lote activo y población viva > 0 sin solicitud preexistente en el sistema
   - **When** el Veterinario emite un dictamen de confirmación directa durante su visita técnica presencial
   - **Then** el sistema inicializa el agregado en la misma unidad de trabajo transaccional
   - **And** emite ordenadamente los eventos de dominio "AislamientoSolicitado" y "AislamientoConfirmado" para resguardar la trazabilidad cronológica total

---

### User Story 2 - Desestimación de Aislamiento y Arbitraje de Restricciones Concurrentes (Priority: P2)

Como Médico Veterinario, quiero rechazar formalmente solicitudes de aislamiento improcedentes o falsas alarmas, garantizando que el galpón retorne a su operación normal únicamente cuando se constate que no subsisten otras restricciones sanitarias, terapéuticas o de retiro farmacológico activas.

**Why this priority**: Evita paradas productivas innecesarias sin comprometer la inocuidad alimentaria, previniendo que una desestimación desactive accidentalmente tratamientos o periodos de carencia en curso.

**Independent Test**: Se comprueba desestimando aislamientos en galpones limpios (transición directa a `NORMAL`) y en galpones que posean medicación en periodo de carencia (el galpón permanece en `EN_TIEMPO_DE_RETIRO`).

**Acceptance Scenarios**:

1. **Scenario**: Desestimación en galpón sin restricciones sanitarias accesorias
   - **Given** un Galpón "G-02" con Lote "L-200" activo y solicitud "AIS-002" en estado "SOLICITADO"
   - **And** el galpón no registra tratamientos, retiros ni medidas sanitarias concurrentes
   - **When** el Veterinario dictamina la desestimación con criterio "Falsa alarma por estrés térmico transitorio"
   - **Then** el estado del aislamiento "AIS-002" pasa a "DESESTIMADO"
   - **And** el estado sanitario del galpón "G-02" retorna a "NORMAL"
   - **And** se emite el evento de dominio "AislamientoDesestimado"
   - **And** se genera la entrada respectiva en la auditoría inmutable

2. **Scenario**: Desestimación en galpón con tratamiento o retiro farmacológico concurrente (Flujo Alterno FA-2)
   - **Given** un Galpón "G-03" con Lote "L-300" activo y solicitud "AIS-003" en estado "SOLICITADO"
   - **And** el galpón registra un tratamiento farmacológico activo en estado "EN_TIEMPO_DE_RETIRO"
   - **When** el Veterinario desestima el aislamiento "AIS-003"
   - **Then** el aislamiento "AIS-003" pasa al estado "DESESTIMADO"
   - **And** el galpón "G-03" permanece en estado "EN_TIEMPO_DE_RETIRO" sin transicionar a "NORMAL"
   - **And** la respuesta notifica explícitamente la persistencia del bloqueo sanitario concurrente

---

### User Story 3 - Integridad Poblacional, Concurrencia y Aislamiento Multi-Tenant (Priority: P3)

Como Auditor del Sistema y Responsable de Seguridad, quiero asegurar el estricto aislamiento de datos por tenant, el bloqueo de acciones sobre lotes cerrados/extinguidos y la resolución atómica de concurrencia, para blindar legalmente el registro médico.

**Why this priority**: Evita la corrupción cruzada entre granjas, elimina discrepancias biológicas (operaciones sobre aves inexistentes) y protege la consistencia del estado del galpón.

**Independent Test**: Se simulan peticiones concurrentes simultáneas sobre el mismo registro, intentos de acceso con tokens JWT de granjas distintas y envíos de confirmación sobre poblaciones iguales a cero.

**Acceptance Scenarios**:

1. **Scenario**: Rechazo por lote con población viva igual a cero
   - **Given** un Galpón "G-04" con Lote "L-400" cuya población viva es de 0 aves
   - **And** existe una solicitud "AIS-004" en estado "SOLICITADO"
   - **When** el Veterinario intenta confirmar el aislamiento "AIS-004"
   - **Then** el sistema rechaza la transacción con código HTTP 422
   - **And** retorna el error estructurado "VET-013: POBLACION_CERO_NO_AISLABLE"
   - **And** el estado del aislamiento no sufre modificaciones

2. **Scenario**: Rechazo por intento de acceso a otra granja (Violación Cross-Tenant)
   - **Given** una solicitud "AIS-999" radicada en la Granja "GR-002"
   - **When** un Veterinario con credenciales exclusivas para la Granja "GR-001" intenta procesarla
   - **Then** el sistema interrumpe la ejecución respondiendo HTTP 403 Forbidden
   - **And** retorna el código "VET-009: GRANJA_NO_AUTORIZADA"
   - **And** persiste un log de alerta de seguridad en auditoría

3. **Scenario**: Detección y bloqueo por colisión de concurrencia (Optimistic Locking)
   - **Given** un aislamiento "AIS-005" con número de versión 1 en base de datos
   - **And** una transacción concurrente procesa y persiste previamente el registro elevando la versión a 2
   - **When** un segundo hilo intenta validar "AIS-005" enviando como base la versión 1
   - **Then** el sistema detecta inconsistencia de versión
   - **And** rechaza la operación con código HTTP 409 Conflict y error "VET-014: CONCURRENCIA_DETECTADA"

4. **Scenario**: Garantía de idempotencia ante reintentos de red
   - **Given** que el Veterinario ya confirmó exitosamente el aislamiento "AIS-006" utilizando la cabecera "X-Idempotency-Key: IDEMP-KEY-123"
   - **When** el cliente HTTP reenvía la misma petición idéntica con la clave "IDEMP-KEY-123" por caída de socket
   - **Then** el backend intercepta la clave en caché
   - **And** no reejecuta lógica de dominio ni duplica registros de auditoría o eventos
   - **And** retorna de inmediato la respuesta previamente computada con código HTTP 200

---

### Edge Cases

- **Lote cerrado antes de procesar dictamen:** Si el lote pasa a estado liquidado o inactivo en Módulo 1 durante la inspección, se bloquea la confirmación retornando `VET-002: LOTE_NO_ENCONTRADO` (HTTP 422).
- **Mortalidad masiva simultánea:** Si la población disminuye a 0 aves concurrentemente por actualización de mortalidad en Módulo 2, el Aggregate Root aborta la confirmación con error `VET-013`.
- **Inconsistencia de fecha de encasetamiento:** Si la `fechaInicio` ingresada es anterior a la fecha de recepción del lote en el galpón, se rechaza la transacción con `VET-007: TRATAMIENTO_PARAMETROS_INVALIDOS` (HTTP 422).
- **Duplicidad de aislamiento activo:** Si el galpón ya registra un aislamiento en estado `ACTIVO`, el índice relacional parcial único (`uq_aislamiento_activo`) intercepta el guardado retornando `VET-003: AISLAMIENTO_ACTIVO_EXISTENTE` (HTTP 409).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST permitir al Veterinario consultar el estado sanitario, las alertas y la población viva del galpón previo a emitir su dictamen.
- **FR-002**: System MUST verificar la identidad y permisos bajo rol `VETERINARIO`, limitando estrictamente la ejecución a la `granjaId` presente en el JWT (RN-VET-001).
- **FR-003**: System MUST abortar la confirmación si el lote no está en estado `ACTIVO` o si su población viva actual en `InventarioVivo` es menor o igual a 0 aves (RN-VET-002).
- **FR-004**: System MUST validar la existencia y vigencia de la `enfermedadId` referenciada, rechazando patologías inactivas en el catálogo (RN-VET-004).
- **FR-005**: System MUST exigir una duración mínima para el período de aislamiento: la fecha y hora estimada de finalización debe ser al menos 24 horas posterior a la fecha y hora de inicio. Excepcionalmente, si el nivel de severidad se registra como `EMERGENCIA_SANITARIA`, se permite un tiempo mínimo de 12 horas (RN-VET-010).
- **FR-006**: System MUST al confirmar: mutar el aislamiento a `ACTIVO`, el estado del galpón a `AISLADO`, bloquear despachos hacia Módulo 3 y publicar `AislamientoConfirmado`.
- **FR-007**: System MUST al desestimar: invocar al `ValidadorRestriccionesSanitariasGalponService` y conmutar el galpón a `NORMAL` únicamente si no existen otras restricciones sanitarias vigentes (RN-VET-006).
- **FR-008**: System MUST capturar e inmortalizar el dictamen veterinario, la matrícula profesional y el criterio clínico, prohibiendo actualizaciones o sobreescrituras directas (RN-VET-005).
- **FR-009**: System MUST persistir una entrada histórica append-only en `san_auditoria` con cada comando o rechazo transaccional relevante (RN-VET-008).
- **FR-010**: System MUST prohibir de forma absoluta el borrado físico (`DELETE` SQL) sobre la tabla de aislamientos y sus dictámenes asociados (RN-VET-009).
- **FR-011**: System MUST implementar control de concurrencia optimista (`version`) y descarte de peticiones idénticas concurrentes mediante `X-Idempotency-Key`.

### Key Entities

- **Aislamiento**: Aggregate Root que representa la medida sanitaria. Encapsula invariantes de negocio (población viva > 0, período mínimo de observación y dictamen formal). Atributos: `id` (UUID), `granjaId`, `galponId`, `loteId`, `enfermedadId`, `veterinarioId`, `periodo` (VO), `severidad` (VO), `estado` (`SOLICITADO`, `ACTIVO`, `DESESTIMADO`), `dictamen` (entidad interna inmutable) y `version` (optimistic locking).
- **DictamenValidacion**: Entidad interna inmutable vinculada al Aislamiento. Captura el resultado de la evaluación clínica veterinaria. Atributos: `decision` (`CONFIRMADO`, `DESESTIMADO`), `criterioTecnico` (texto estructurado de 10-2000 caracteres), `tarjetaProfesional` y `fechaDictamen`.
- **Enfermedad**: Entidad de referencia del catálogo nosológico. Atributos: `id`, `granjaId`, `codigo`, `nombre` y `activa`.
- **Galpon, Lote, InventarioVivo**: Entidades externas de referencia de Módulo 1 y Módulo 2. Aportan datos de lectura para verificar vigencia operativa del galpón, estado activo del lote y población viva actual > 0.
- **Auditoria (`san_auditoria`)**: Registro histórico append-only inmutable para garantizar trazabilidad legal de cada intervención veterinaria y evento de rechazo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los aislamientos confirmados bloquean de forma inmediata y automática las órdenes de cosecha o despacho comercial en el Módulo 3 mediante el evento de integración.
- **SC-002**: 0% de falsos positivos en normalización: Cero galpones con tratamientos activos o tiempos de retiro vigentes transicionados indebidamente a `NORMAL` tras desestimar una solicitud.
- **SC-003**: El tiempo de respuesta del servicio de validación (`POST /validacion`) debe ser menor a 400 milisegundos en al menos el 95% de las solicitudes atendidas bajo condiciones normales de operación.
- **SC-004**: El 100% de las transacciones (confirmadas, desestimadas o rechazadas por violaciones de reglas) generan una traza persistida en `san_auditoria` con su respectivo `correlationId`.
- **SC-005**: 100% de aislamiento multi-tenant: Cero incidentes de lectura o escritura cruzada entre diferentes granjas durante las ejecuciones de pruebas automatizadas.