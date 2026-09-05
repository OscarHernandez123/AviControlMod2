# Feature Specification: Ordenar Sacrificio Sanitario (CU-VET-006)

**Created**: 2026-09-03  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Declaración Médica de Sacrificio Sanitario Epidemiológico (Priority: P1)

Como Médico Veterinario autorizado en la granja, quiero emitir una orden técnica formal e irrevocable de Sacrificio Sanitario (*stamping-out*) sobre un lote afectado por un brote patológico grave, no tratable o de reporte obligatorio oficial, para cortar radicalmente la cadena de transmisión biológica, proteger a los demás galpones de la granja y salvaguardar el estatus zoosanitario general[cite: 1].

**Why this priority**: Es la medida sanitaria de contingencia terminal de mayor jerarquía biológica; sin este mecanismo formal, no se pueden contener epidemias fulminantes ni liquidar biológicamente un lote sin pasar por faena comercial[cite: 1].

**Independent Test**: Se valida ejecutando el comando de orden de sacrificio sobre un galpón con lote activo en aislamiento, exigiendo justificación técnica y tarjeta profesional[cite: 1]. Se verifica que el aislamiento transicione al estado terminal `SACRIFICIO_SANITARIO`, se emita `SacrificioSanitarioOrdenado`, se cancele cualquier tratamiento en curso y se notifique a Inventario Vivo para descontar la población a cero[cite: 1].

**Acceptance Scenarios**:

1. **Scenario**: Emisión exitosa de orden de sacrificio sanitario
   - **Given** un Galpón "G-01" en Granja "GR-001" con Lote "L-100" activo en estado "AISLADO" o "EN_TRATAMIENTO"[cite: 1]
   - **And** el lote registra una población viva de 12000 aves[cite: 1]
   - **When** el Veterinario emite la orden con justificación "Brote fulminante compatible con Influenza Aviar de Alta Patogenicidad", método sanitario y matrícula profesional "MP-VET-88492-CO"[cite: 1]
   - **Then** el estado del aislamiento pasa irreversiblemente a "SACRIFICIO_SANITARIO"[cite: 1]
   - **And** el estado del galpón cambia a "SACRIFICIO_SANITARIO" bloqueando cualquier operación comercial[cite: 1]
   - **And** se despacha el evento de dominio "SacrificioSanitarioOrdenado"[cite: 1]
   - **And** se registra una entrada inmutable en "san_auditoria"[cite: 1]
   - **And** el sistema responde con código HTTP 201 Created

2. **Scenario**: Rechazo de orden sobre lote sin población viva
   - **Given** un Galpón "G-02" con Lote "L-200" cuya población viva actual en inventario es 0 aves[cite: 1]
   - **When** el Veterinario intenta emitir una orden de sacrificio sanitario
   - **Then** el sistema aborta la transacción
   - **And** retorna el código de error "VET-013: POBLACION_CERO_NO_OPERABLE" con código HTTP 422[cite: 1]
   - **And** no altera el expediente del lote

---

### User Story 2 - Liquidación Poblacional y Cancelación de Terapéuticas (Priority: P2)

Como Sistema de Sanidad e Inventario, quiero que la confirmación de la orden de sacrificio desencadene de forma atómica la cancelación de todos los tratamientos farmacológicos activos y el ajuste del inventario vivo a cero aves por causa médica[cite: 1], para que los costos de medicamentos se congelen y los tableros productivos reflejen de inmediato la baja total del activo biológico.

**Why this priority**: Evita inconsistencias graves de inventario (aves sacrificadas que sigan consumiendo alimento o recibiendo medicamentos en el sistema) y congela las pérdidas financieras en Módulo 3[cite: 1].

**Independent Test**: Emitir sacrificio sobre un lote con tratamientos en estado `EN_CURSO`[cite: 1]. Verificar que dichos tratamientos pasen inmediatamente a `SUSPENDIDO` con motivo de sacrificio sanitario y que el componente `InventarioVivo` registre la baja total de aves[cite: 1].

**Acceptance Scenarios**:

1. **Scenario**: Cancelación en cascada de tratamientos vigentes
   - **Given** un lote con dos tratamientos farmacológicos en estado "EN_CURSO"[cite: 1]
   - **When** se confirma la orden de sacrificio sanitario sobre el galpón[cite: 1]
   - **Then** todos los tratamientos asociados al lote pasan inmediatamente a estado "SUSPENDIDO"[cite: 1]
   - **And** se cancelan los cronogramas de dosificación pendientes
   - **And** se emiten los eventos de suspensión terapéutica asociados

2. **Scenario**: Descuento total del inventario vivo por sacrificio sanitario
   - **Given** un lote con población actual de 9500 aves en el Módulo 2[cite: 1]
   - **When** se procesa el evento "SacrificioSanitarioOrdenado" en el contexto de Inventario Vivo[cite: 1]
   - **Then** la población viva actual del lote se reduce a 0 aves[cite: 1]
   - **And** se tipifica la baja bajo la causa médica "SACRIFICIO_SANITARIO"
   - **And** el lote en Módulo 1 se marca como cerrado por contingencia sanitaria[cite: 1]

---

### User Story 3 - Integridad Legal, Inmutabilidad y Aislamiento Multi-Tenant (Priority: P3)

Como Auditor y Autoridad Sanitaria, quiero que la orden de sacrificio sea totalmente irrevocable, inmutable y restringida por credenciales veterinarias verificadas[cite: 1], para respaldar las actuaciones legales y los informes epidemiológicos ante los entes regulatorios.

**Why this priority**: El sacrificio sanitario representa la pérdida total del lote y puede involucrar auditorías externas o reclamos de indemnización/seguros; el expediente debe ser inalterable[cite: 1].

**Independent Test**: Intentar revertir, reactivar o borrar un aislamiento en estado `SACRIFICIO_SANITARIO` mediante llamadas API o comandos directos[cite: 1]. Comprobar que cualquier intento sea rechazado de forma tajante.

**Acceptance Scenarios**:

1. **Scenario**: Prohibición de reversión de sacrificio sanitario
   - **Given** un galpón cuyo aislamiento se encuentra en estado "SACRIFICIO_SANITARIO"[cite: 1]
   - **When** un usuario intenta enviar un comando de reintegro o reapertura clínica
   - **Then** el sistema bloquea la solicitud respondiendo HTTP 409 Conflict
   - **And** retorna el error "VET-010: OPERACION_ESTADO_INVALIDO"[cite: 1]
   - **And** preserva el estado de sacrificio intacto

2. **Scenario**: Rechazo de sacrificio por intento cross-tenant
   - **Given** un galpón ubicado en la Granja "GR-002"[cite: 1]
   - **When** un Veterinario con sesión exclusiva en Granja "GR-001" intenta ordenar el sacrificio sanitario[cite: 1]
   - **Then** el sistema interrumpe la ejecución con código HTTP 403 Forbidden[cite: 1]
   - **And** retorna el error "VET-009: GRANJA_NO_AUTORIZADA"[cite: 1]

---

### Edge Cases

- **Orden de sacrificio emitida durante la noche o sin conexión:** El comando requiere conexión y respuesta síncrona transaccional debido a su impacto destructivo sobre el lote biológico; no se permite ejecución offline no verificada.
- **Galpón con proceso de cosecha comercial iniciado en Módulo 3:** Si existen camiones despachados o pesajes en curso, el sistema aborta la orden con error de conflicto comercial y exige comunicación inmediata con planta de faena para decomiso.
- **Mortalidad masiva natural previa al sacrificio:** Si durante la expedición de la orden la mortalidad natural reportada por los operarios ya liquidó el 100% de las aves, el sistema procesa el cierre por despoblación biológica y anula la ejecución del sacrificio por falta de población viva[cite: 1].
- **Intentos de borrado del expediente:** Cualquier invocación `DELETE` HTTP o SQL sobre la orden de sacrificio es rechazada; el expediente se conserva a perpetuidad en auditoría[cite: 1].

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema debe exigir que el usuario autenticado cuente con rol `VETERINARIO`, matrícula profesional verificada y autorización sobre la `granjaId` del galpón[cite: 1].
- **FR-002**: El sistema debe comprobar que el lote tenga población viva actual mayor a cero aves en `InventarioVivo` antes de admitir la orden de sacrificio (RN-VET-002)[cite: 1].
- **FR-003**: El sistema debe exigir el registro obligatorio de justificación técnica epidemiológica estructurada (mínimo 20 caracteres) y método de sacrificio autorizado.
- **FR-004**: Al emitirse la orden, el sistema debe transicionar de forma irreversible el estado del aislamiento a `SACRIFICIO_SANITARIO` (RN-VET-008)[cite: 1].
- **FR-005**: Al emitirse la orden, el sistema debe cambiar el estado del galpón a `SACRIFICIO_SANITARIO`, bloqueando cualquier movimiento, despacho o reutilización productiva[cite: 1].
- **FR-006**: El sistema debe suspender automáticamente todos los tratamientos farmacológicos activos vinculados al lote, congelando los cronogramas terapéuticos[cite: 1].
- **FR-007**: El sistema debe publicar el evento de integración `SacrificioSanitarioOrdenado` para que el contexto de Inventario Vivo descuente la población a cero aves[cite: 1].
- **FR-008**: El sistema debe registrar una entrada inmutable append-only en `san_auditoria` con el expediente completo del sacrificio, usuario responsable y `correlationId`[cite: 1].
- **FR-009**: El sistema debe prohibir estrictamente la reactivación, reapertura o reversión de un aislamiento en estado `SACRIFICIO_SANITARIO`[cite: 1].
- **FR-010**: El sistema debe prohibir de forma absoluta el borrado físico (`DELETE` relacional) de las órdenes de sacrificio registradas[cite: 1].

### Key Entities

- **OrdenSacrificioSanitario** *(Entidad Interna del Agregado Aislamiento)*: Expediente técnico-legal que formaliza la decisión de liquidación[cite: 1]. Atributos: `id` (UUID), `aislamientoId` (UUID), `veterinarioId` (UUID), `tarjetaProfesional` (String), `justificacionEpidemiologica` (Text), `metodoUtilizado` (String), `poblacionAfectada` (Integer) y `fechaOrden` (Timestamp UTC)[cite: 1].
- **Aislamiento** *(Aggregate Root)*: Entidad raíz transaccional que muta a su estado terminal definitivo `SACRIFICIO_SANITARIO`[cite: 1].
- **InventarioVivo**: Entidad de población biológica en Módulo 2 receptora del evento para asentar la baja médica total a 0 aves[cite: 1].
- **Tratamiento**: Entidades terapéuticas asociadas que se transicionan automáticamente a estado `SUSPENDIDO`[cite: 1].
- **Auditoria (`san_auditoria`)**: Tabla inmutable de registro histórico que almacena el snapshot legal de la orden y sus firmas de sesión[cite: 1].

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las órdenes de sacrificio confirmadas reducen la población viva del lote a cero aves en `InventarioVivo` en menos de 1 segundo tras la confirmación transaccional[cite: 1].
- **SC-002**: Cero por ciento (0%) de posibilidades de reapertura, reintegro o despacho comercial de aves pertenecientes a un galpón con orden de sacrificio emitida[cite: 1].
- **SC-003**: El 100% de los tratamientos farmacológicos activos del lote se suspenden de forma automática en cascada en la misma transacción[cite: 1].
- **SC-004**: El tiempo de respuesta para la ejecución y persistencia de la orden de sacrificio es inferior a 400 milisegundos en al menos el 95% de las solicitudes atendidas[cite: 1].
- **SC-005**: 100% de inmutabilidad y auditoría: Cero incidentes de registros de sacrificio eliminados o alterados en la base de datos a lo largo del ciclo de vida del sistema[cite: 1].