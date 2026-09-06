# Feature Specification: Validar Aislamiento (CU-VET-001)

**Created**: 2026-09-01  
**Updated**: 2026-09-03 (Revisión Arquitectónica Post-Diagnóstico)  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Confirmación Formal de Aislamiento Sanitario (Priority: P1)

Como Médico Veterinario asignado y autorizado en la granja, quiero evaluar solicitudes de aislamiento sanitario (preventivas o emergentes) y registrar un dictamen médico inmutable de confirmación con severidad y fecha estimada de finalización, para activar las medidas de contención biológica e impedir de inmediato el movimiento, traslado o despacho a faena del lote[cite: 1].

**Why this priority**: Es la capacidad medular del actor veterinario[cite: 1]; sin esta confirmación estructurada, no existe amparo técnico ni bloqueo automatizado en planta de faena para evitar contingencias sanitarias[cite: 1].

**Independent Test**: Se valida ejecutando el comando de confirmación sobre un galpón con lote activo en estado `SOLICITADO` y población > 0[cite: 1]. Se verifica que el aislamiento transicione a `ACTIVO`, el galpón a `AISLADO`, se publique `AislamientoConfirmado`, se emita el evento de integración y se persista la auditoría inmutable[cite: 1].

**Acceptance Scenarios**:

1. **Scenario**: Confirmación exitosa de aislamiento sanitario con pre-registro previo
   - **Given** un Galpón "G-01" en Granja "GR-001" con Lote "L-100" activo[cite: 1]
   - **And** el lote registra una población viva de 8500 aves verificada en inventario[cite: 1]
   - **And** existe una solicitud de aislamiento "AIS-001" en estado "SOLICITADO"[cite: 1]
   - **And** la patología "ENF-010" figura activa en el catálogo (perteneciente a la granja "GR-001" o al catálogo corporativo global)[cite: 5, 11]
   - **When** el Veterinario emite la confirmación con severidad "CRITICO", fecha fin estimada "2026-09-07", tarjeta profesional "MP-VET-88492-CO" y criterio clínico "Cuadro respiratorio severo"[cite: 1]
   - **Then** el estado del aislamiento "AIS-001" pasa a "ACTIVO"[cite: 1]
   - **And** el estado sanitario del galpón "G-01" pasa a "AISLADO"[cite: 1]
   - **And** se despacha el evento de dominio "AislamientoConfirmado"[cite: 1]
   - **And** se despacha el evento de integración "GalponAisladoSanitariamenteIntegrationEvent" para bloquear cosecha en Módulo 3[cite: 1]
   - **And** se registra una entrada inmutable append-only en la tabla "san_auditoria"[cite: 1]
   - **And** el sistema responde con código HTTP 200/201 con payload JSON validado[cite: 1]

2. **Scenario**: Confirmación directa por inspección presencial en campo (Flujo Alterno FA-1)
   - **Given** un Galpón con Lote activo y población viva > 0 sin solicitud preexistente en el sistema[cite: 1]
   - **When** el Veterinario emite un dictamen de confirmación directa durante su visita técnica presencial[cite: 1]
   - **Then** el sistema inicializa el agregado en la misma unidad de trabajo transaccional[cite: 1]
   - **And** emite ordenadamente los eventos de dominio "AislamientoSolicitado" y "AislamientoConfirmado" para resguardar la trazabilidad cronológica total[cite: 1]

---

### User Story 2 - Desestimación de Aislamiento y Arbitraje de Restricciones Concurrentes (Priority: P2)

Como Médico Veterinario, quiero rechazar formalmente solicitudes de aislamiento improcedentes o falsas alarmas, garantizando que el galpón retorne a su operación normal únicamente cuando se constate que no subsisten otras restricciones sanitarias, terapéuticas o de retiro farmacológico activas[cite: 1].

**Why this priority**: Evita paradas productivas innecesarias sin comprometer la inocuidad alimentaria, previniendo que una desestimación desactive accidentalmente tratamientos o periodos de carencia en curso[cite: 1].

**Independent Test**: Se comprueba desestimando aislamientos en galpones limpios (transición directa a `NORMAL`) y en galpones que posean medicación en periodo de carencia (el galpón permanece en `EN_TIEMPO_DE_RETIRO`)[cite: 1].

**Acceptance Scenarios**:

1. **Scenario**: Desestimación en galpón sin restricciones sanitarias accesorias
   - **Given** un Galpón "G-02" con Lote "L-200" activo y solicitud "AIS-002" en estado "SOLICITADO"[cite: 1]
   - **And** el galpón no registra tratamientos, retiros ni medidas sanitarias concurrentes[cite: 1]
   - **When** el Veterinario dictamina la desestimación con criterio "Falsa alarma por estrés térmico transitorio"[cite: 1]
   - **Then** el estado del aislamiento "AIS-002" pasa a "DESESTIMADO"[cite: 1]
   - **And** el estado sanitario del galpón "G-02" retorna a "NORMAL"[cite: 1]
   - **And** se emite el evento de dominio "AislamientoDesestimado"[cite: 1]
   - **And** se genera la entrada respectiva en la auditoría inmutable[cite: 1]

2. **Scenario**: Desestimación en galpón con tratamiento o retiro farmacológico concurrente (Flujo Alterno FA-2)
   - **Given** un Galpón "G-03" con Lote "L-300" activo y solicitud "AIS-003" en estado "SOLICITADO"[cite: 1]
   - **And** el galpón registra un tratamiento farmacológico activo en estado "EN_TIEMPO_DE_RETIRO"[cite: 1]
   - **When** el Veterinario desestima el aislamiento "AIS-003"[cite: 1]
   - **Then** el aislamiento "AIS-003" pasa al estado "DESESTIMADO"[cite: 1]
   - **And** el galpón "G-03" permanece en estado "EN_TIEMPO_DE_RETIRO" sin transicionar a "NORMAL"[cite: 1]
   - **And** la respuesta notifica explícitamente la persistencia del bloqueo sanitario concurrente[cite: 1]

---

### User Story 3 - Integridad Poblacional, Concurrencia y Aislamiento Multi-Tenant (Priority: P3)

Como Auditor del Sistema y Responsable de Seguridad, quiero asegurar el estricto aislamiento de datos por tenant, el bloqueo de acciones sobre lotes cerrados/extinguidos y la resolución atómica de concurrencia, para blindar legalmente el registro médico[cite: 1].

**Why this priority**: Evita la corrupción cruzada entre granjas, elimina discrepancias biológicas (operaciones sobre aves inexistentes) y protege la consistencia del estado del galpón[cite: 1].

**Independent Test**: Se simulan peticiones concurrentes simultáneas sobre el mismo registro, intentos de acceso con tokens JWT de granjas distintas y envíos de confirmación sobre poblaciones iguales a cero[cite: 1].

**Acceptance Scenarios**:

1. **Scenario**: Rechazo por lote con población viva igual a cero
   - **Given** un Galpón "G-04" con Lote "L-400" cuya población viva es de 0 aves[cite: 1]
   - **And** existe una solicitud "AIS-004" en estado "SOLICITADO"[cite: 1]
   - **When** el Veterinario intenta confirmar el aislamiento "AIS-004"[cite: 1]
   - **Then** el sistema rechaza la transacción con código HTTP 422 Unprocessable Entity[cite: 1]
   - **And** retorna el error estructurado "VET-013: POBLACION_CERO_NO_AISLABLE"[cite: 1]
   - **And** el estado del aislamiento no sufre modificaciones[cite: 1]

2. **Scenario**: Rechazo por intento de acceso a otra granja (Violación Cross-Tenant)
   - **Given** una solicitud "AIS-999" radicada en la Granja "GR-002"[cite: 1]
   - **When** un Veterinario con credenciales exclusivas para la Granja "GR-001" intenta procesarla[cite: 1]
   - **Then** el sistema interrumpe la ejecución respondiendo HTTP 403 Forbidden[cite: 1]
   - **And** retorna el código "VET-009: GRANJA_NO_AUTORIZADA"[cite: 1]
   - **And** persiste un log de alerta de seguridad en auditoría[cite: 1]

3. **Scenario**: Detección y bloqueo por colisión de concurrencia (Optimistic Locking)
   - **Given** un aislamiento "AIS-005" con número de versión 1 en base de datos[cite: 1]
   - **And** una transacción concurrente procesa y persiste previamente el registro elevando la versión a 2[cite: 1]
   - **When** un segundo hilo intenta validar "AIS-005" enviando como base la versión 1[cite: 1]
   - **Then** el sistema detecta inconsistencia de versión[cite: 1]
   - **And** rechaza la operación con código HTTP 409 Conflict y error "VET-014: CONCURRENCIA_DETECTADA"[cite: 1]

4. **Scenario**: Garantía de idempotencia ante reintentos de red
   - **Given** que el Veterinario ya confirmó exitosamente el aislamiento "AIS-006" utilizando la cabecera "X-Idempotency-Key: IDEMP-KEY-123"[cite: 1]
   - **When** el cliente HTTP reenvía la misma petición idéntica con la clave "IDEMP-KEY-123" por caída de conexión[cite: 1]
   - **Then** el backend intercepta la clave en la capa de infraestructura[cite: 1]
   - **And** no reejecuta lógica de dominio ni duplica registros de auditoría o eventos[cite: 1]
   - **And** retorna la respuesta original almacenada con código HTTP 200 OK y la cabecera "Idempotent-Replayed: true"

---

### Edge Cases

- **Lote cerrado antes de procesar dictamen:** Si el lote pasa a estado liquidado o inactivo en Módulo 1 durante la inspección, se bloquea la confirmación retornando `VET-002: LOTE_NO_ENCONTRADO` con HTTP 422 Unprocessable Entity[cite: 1].
- **Mortalidad masiva simultánea:** Si la población disminuye a 0 aves concurrentemente por actualización de mortalidad en Módulo 2, el Aggregate Root aborta la confirmación con error `VET-013` (HTTP 422)[cite: 1].
- **Inconsistencia de fecha de encasetamiento:** Si la `fechaInicio` ingresada es anterior a la fecha de recepción del lote en el galpón, se rechaza la transacción con `VET-007: TRATAMIENTO_PARAMETROS_INVALIDOS` (HTTP 422)[cite: 1].
- **Error sintáctico en payload:** Si el payload de entrada omite campos obligatorios o presenta formatos de fecha corruptos, el framework responde de inmediato con HTTP 400 Bad Request y estructura RFC 7807 antes de invocar la capa de aplicación.
- **Duplicidad de aislamiento activo:** Si el galpón ya registra un aislamiento en estado `ACTIVO`, el índice relacional parcial único (`uq_aislamiento_activo`) intercepta el guardado retornando `VET-003: AISLAMIENTO_ACTIVO_EXISTENTE` con HTTP 409 Conflict[cite: 1].

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema debe permitir al Veterinario consultar el estado sanitario, las alertas y la población viva del galpón previo a emitir su dictamen[cite: 1].
- **FR-002**: El sistema debe verificar la identidad y permisos bajo rol `VETERINARIO`, limitando estrictamente la ejecución a la `granjaId` presente en el JWT (RN-VET-001)[cite: 1].
- **FR-003**: El sistema debe abortar la confirmación si el lote no está en estado `ACTIVO` o si su población viva actual en `InventarioVivo` es menor o igual a 0 aves (RN-VET-002)[cite: 1].
- **FR-004**: El sistema debe validar que la `enfermedadId` referenciada exista y esté en estado `ACTIVA`, siendo visible si pertenece a la granja actual (`granja_id = :tenantId`) o al catálogo corporativo maestro (`granja_id IS NULL`) (RN-VET-004)[cite: 5, 11].
- **FR-005**: El sistema debe exigir una duración mínima para el período de aislamiento: la fecha y hora estimada de finalización debe ser al menos 24 horas posterior a la fecha y hora de inicio. Excepcionalmente, si el nivel de severidad se registra como `EMERGENCIA_SANITARIA`, se permite un tiempo mínimo de 12 horas (RN-VET-010).
- **FR-006**: Al confirmar, el sistema debe mutar el aislamiento a `ACTIVO`, el estado del galpón a `AISLADO`, bloquear despachos hacia Módulo 3 y publicar `AislamientoConfirmado`[cite: 1].
- **FR-007**: Al desestimar, el sistema debe invocar al `ValidadorRestriccionesSanitariasGalponService` y conmutar el galpón a `NORMAL` únicamente si no existen otras restricciones sanitarias vigentes (RN-VET-006).
- **FR-008**: El sistema debe capturar e inmortalizar el dictamen veterinario, la matrícula profesional y el criterio clínico, prohibiendo actualizaciones o sobreescrituras directas (RN-VET-005)[cite: 1].
- **FR-009**: El sistema debe persistir una entrada histórica append-only en `san_auditoria` con cada comando o rechazo transaccional relevante (RN-VET-008)[cite: 1].
- **FR-010**: El sistema debe prohibir de forma absoluta el borrado físico (`DELETE` SQL) sobre la tabla de aislamientos y sus dictámenes asociados (RN-VET-009)[cite: 1].
- **FR-011**: El sistema debe implementar control de concurrencia optimista (`version`) para abortar con HTTP 409 colisiones simultáneas y almacenar respuestas bajo la cabecera `X-Idempotency-Key`[cite: 1].
- **FR-012**: Cuando se reciba una solicitud con una `X-Idempotency-Key` ya procesada y payload idéntico, el sistema debe retornar el payload almacenado con código HTTP 200 OK y la cabecera de respuesta `Idempotent-Replayed: true` sin reejecutar lógica de dominio ni generar nuevos eventos.

### Key Entities

- **Aislamiento**: Aggregate Root que representa la medida sanitaria[cite: 1]. Encapsula invariantes de negocio (población viva > 0, período mínimo de observación y dictamen formal)[cite: 1]. Atributos: `id` (UUID), `granjaId`, `galponId`, `loteId`, `enfermedadId`, `veterinarioId`, `periodo` (VO), `severidad` (VO), `estado` (`SOLICITADO`, `ACTIVO`, `DESESTIMADO`), `dictamen` (entidad interna inmutable) y `version` (optimistic locking)[cite: 1].
- **DictamenValidacion**: Entidad interna inmutable vinculada al Aislamiento[cite: 1]. Captura el resultado de la evaluación clínica veterinaria[cite: 1]. Atributos: `decision` (`CONFIRMADO`, `DESESTIMADO`), `criterioTecnico` (texto estructurado de 10-2000 caracteres), `tarjetaProfesional` y `fechaDictamen`[cite: 1].
- **Enfermedad**: Entidad de referencia del catálogo nosológico (`id`, `granjaId` nullable, `codigo`, `nombre`, `activa`)[cite: 5, 11].
- **Galpon, Lote, InventarioVivo**: Entidades externas de referencia de Módulo 1 y Módulo 2[cite: 1]. Aportan datos de lectura para verificar vigencia operativa del galpón, estado activo del lote y población viva actual > 0[cite: 1].
- **Auditoria (`san_auditoria`)**: Registro histórico append-only inmutable para garantizar trazabilidad legal de cada intervención veterinaria y evento de rechazo[cite: 1].

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los aislamientos confirmados bloquean de forma inmediata y automática las órdenes de cosecha o despacho comercial en el Módulo 3 mediante el evento de integración[cite: 1].
- **SC-002**: 0% de falsos positivos en normalización: Cero galpones con tratamientos activos o tiempos de retiro vigentes transicionados indebidamente a `NORMAL` tras desestimar una solicitud.
- **SC-003**: El tiempo de respuesta del servicio de validación (`POST /validacion`) debe ser menor a 400 milisegundos en al menos el 95% de las solicitudes atendidas bajo condiciones normales de operación.
- **SC-004**: El 100% de las transacciones (confirmadas, desestimadas o rechazadas por violaciones de reglas) generan una traza persistida en `san_auditoria` con su respectivo `correlationId`[cite: 1].
- **SC-005**: 100% de aislamiento multi-tenant: Cero incidentes de lectura o escritura cruzada entre diferentes granjas durante las ejecuciones de pruebas automatizadas.