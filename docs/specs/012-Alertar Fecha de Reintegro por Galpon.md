# Feature Specification: Alertar Fecha de Reintegro por Galpón (CU-VET-005)

**Created**: 2026-09-03  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Detección Automática y Notificación de Fin de Retiro (Priority: P1)

Como Médico Veterinario (y equipo de operaciones de granja), quiero que el sistema detecte de forma proactiva y automática cuando un galpón aislado ha cumplido su fecha mínima de liberación (conclusión de cuarentena y retiro farmacológico)[cite: 1], generando una alerta sanitaria de reintegro pendiente, para iniciar oportunamente la inspección de certificación sanitaria sin retrasar el flujo productivo[cite: 1].

**Why this priority**: Es el puente automatizado que conecta el cumplimiento temporal calculado en CU-VET-004 con la acción médica resolutiva de CU-VET-003[cite: 1]; sin esta alerta, los galpones liberados quedarían en el limbo operativo, provocando sobrecostos por alimentación innecesaria y retrasos en faena[cite: 1].

**Independent Test**: Se valida mediante la ejecución del job programado o el consumo del evento `TiempoRetiroCumplido` sobre un galpón cuya fecha actual alcance o supere la fecha mínima de liberación[cite: 1]. Se verifica que se cree la entidad `AlertaSanitaria` en estado `GENERADA`, se transicione el aislamiento a `PENDIENTE_CERTIFICACION` y se emita el evento `AlertaReintegroGenerada`[cite: 1].

**Acceptance Scenarios**:

1. **Scenario**: Generación automática de alerta al cumplirse el retiro farmacológico
   - **Given** un Galpón "G-01" en Granja "GR-001" con aislamiento en estado "ACTIVO" o "EN_TIEMPO_DE_RETIRO"[cite: 1]
   - **And** la fecha mínima de liberación calculada es "2026-09-03T14:00:00Z"[cite: 1]
   - **And** la hora actual del sistema alcanza las "2026-09-03T14:00:01Z"
   - **When** el evaluador automático de retiro procesa el estado del galpón
   - **Then** el sistema crea una alerta sanitaria con tipo "REINTEGRO_PENDIENTE" y severidad "MODERADO"[cite: 1]
   - **And** el estado del aislamiento transiciona automáticamente a "PENDIENTE_CERTIFICACION"[cite: 1]
   - **And** se publica el evento de dominio "AlertaReintegroGenerada"
   - **And** se registra una entrada inmutable en "san_auditoria"[cite: 1]

2. **Scenario**: Idempotencia y supresión de alertas duplicadas
   - **Given** un Galpón "G-02" que ya posee una alerta activa de tipo "REINTEGRO_PENDIENTE" sin resolver[cite: 1]
   - **When** el job programado vuelve a ejecutarse en el siguiente ciclo horario
   - **Then** el sistema identifica la alerta no resuelta preexistente
   - **And** no crea registros duplicados de alerta ni despacha nuevos eventos redundantes en el bus de mensajería[cite: 1]

---

### User Story 2 - Notificación Preventiva de Vencimiento Próximo (Priority: P2)

Como Médico Veterinario, quiero recibir una notificación preventiva con 24 horas de antelación al vencimiento del tiempo de retiro, para planificar mi agenda de visitas técnicas e inspecciones clínicas previas a la certificación del galpón.

**Why this priority**: Mejora la eficiencia de campo y evita que los galpones permanezcan ociosos esperando una visita veterinaria imprevista tras haber cumplido su periodo de carencia.

**Independent Test**: Simular un galpón cuya fecha mínima de liberación vence en exactamente 24 horas. Verificar que el proyector de alertas emita un aviso informativo de programación sin alterar el estado operativo del aislamiento (el aislamiento se mantiene en restricción hasta el cumplimiento del 100% del tiempo).

**Acceptance Scenarios**:

1. **Scenario**: Emisión de aviso preventivo de fin de carencia
   - **Given** un Galpón "G-03" con aislamiento y fecha de liberación fijada para dentro de 24 horas[cite: 1]
   - **When** el proceso de vigilancia epidemiológica evalúa el cronograma sanitario
   - **Then** el sistema genera una notificación de tipo "AVISO_PREVENTIVO_REINTEGRO"
   - **And** el estado del aislamiento se mantiene inalterado en "EN_TIEMPO_DE_RETIRO"[cite: 1]
   - **And** no se habilita el comando de reintegro ni de certificación anticipada[cite: 1]

---

### User Story 3 - Cierre Automatizado de Alerta por Certificación o Normalización (Priority: P3)

Como Sistema y Auditor Sanitario, quiero que la alerta sanitaria de reintegro se marque automáticamente como `RESUELTA` en cuanto el Veterinario certifique el galpón o cuando ocurra un evento terminal (sacrificio sanitario), para mantener limpio el panel de novedades operativas[cite: 1].

**Why this priority**: Evita la persistencia de alertas obsoletas en los paneles de control y asegura que la métrica de atención médica refleje la realidad del galpón[cite: 1].

**Independent Test**: Disparar el evento `GalponCertificado` sobre un galpón con alerta abierta de reintegro y verificar que la alerta cambie su estado a `RESUELTA` con fecha de resolución y referencia al dictamen emitido[cite: 1].

**Acceptance Scenarios**:

1. **Scenario**: Resolución automática de alerta tras emisión de certificación
   - **Given** un Galpón "G-04" con una alerta "ALT-004" de tipo "REINTEGRO_PENDIENTE" en estado "GENERADA"[cite: 1]
   - **When** el Médico Veterinario ejecuta exitosamente la certificación sanitaria del galpón (CU-VET-003)[cite: 1]
   - **Then** el sistema intercepta el evento "GalponCertificado"[cite: 1]
   - **And** actualiza el estado de la alerta "ALT-004" a "RESUELTA"[cite: 1]
   - **And** asocia el identificador de la certificación emitida y la marca temporal de resolución[cite: 1]

---

### Edge Cases

- **Postergación de fecha durante alerta abierta:** Si el Veterinario prolonga la cuarentena o administra una nueva dosis mientras la alerta está activa, el recálculo de CU-VET-004 desplaza la fecha mínima de liberación[cite: 1]; el sistema cancela la alerta de reintegro previa y regresa el aislamiento al estado correspondiente (`ACTIVO` o `EN_TRATAMIENTO`)[cite: 1].
- **Sacrificio sanitario sobrevenido:** Si se ordena el sacrificio sanitario del lote mientras la alerta de reintegro está pendiente, la alerta pasa inmediatamente a estado `CERRADA` con motivo "LOTE_SACRIFICADO_SANITARIAMENTE"[cite: 1].
- **Falla en el despachador de notificaciones:** Si el broker de mensajería no está disponible, el patrón Outbox retiene el evento `AlertaReintegroGenerada` asegurando entrega confiable una vez restablecido el servicio.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema debe evaluar periódicamente el cronograma sanitario de los galpones aislados mediante un servicio programado (job) o por reacción a eventos de finalización de tiempo[cite: 1].
- **FR-002**: El sistema debe generar automáticamente una entidad `AlertaSanitaria` de tipo `REINTEGRO_PENDIENTE` cuando la fecha y hora actual sea mayor o igual a la `FechaMinimaLiberacion` calculada para el galpón[cite: 1].
- **FR-003**: El sistema debe asignar severidad `MODERADO` por defecto a las alertas de reintegro pendiente[cite: 1].
- **FR-004**: Al generarse la alerta por retiro cumplido, el sistema debe transicionar el estado del aislamiento a `PENDIENTE_CERTIFICACION`[cite: 1].
- **FR-005**: El sistema debe evitar duplicidad de alertas abiertas: no se debe instanciar una nueva alerta de reintegro si ya existe una alerta activa sin resolver para el mismo galpón y lote[cite: 1].
- **FR-006**: El sistema debe publicar el evento de dominio `AlertaReintegroGenerada` inmediatamente después de persistir la alerta en base de datos.
- **FR-007**: El sistema debe resolver automáticamente la alerta de reintegro al recibir el evento `GalponCertificado` emitido por CU-VET-003[cite: 1].
- **FR-008**: El sistema debe asociar obligatoriamente la `granjaId`, `galponId` y `loteId` en cada alerta para garantizar el aislamiento multi-tenant estricto[cite: 1].
- **FR-009**: El sistema debe persistir una entrada inmutable append-only en `san_auditoria` cada vez que una alerta sea generada, cancelada o resuelta[cite: 1].
- **FR-010**: El sistema debe prohibir la eliminación física (`DELETE` relacional) de las alertas generadas[cite: 1].

### Key Entities

- **AlertaSanitaria** *(Aggregate Root de Vigilancia)*: Representa la notificación formal de riesgo o novedad sanitaria[cite: 1]. Atributos: `id` (UUID), `granjaId` (UUID), `galponId` (UUID), `loteId` (UUID), `tipo` (`REINTEGRO_PENDIENTE`), `severidad` (`MODERADO`), `estado` (`GENERADA`, `VISTA`, `EN_ATENCION`, `RESUELTA`, `CERRADA`), `mensaje` (Text), `fechaGeneracion` y timestamps de resolución[cite: 1].
- **Aislamiento** *(Aggregate Root)*: Entidad afectada que transiciona su estado a `PENDIENTE_CERTIFICACION` cuando se constata el vencimiento del retiro[cite: 1].
- **CalculadorRetiroSanitarioService** *(Domain Service)*: Proveedor de la fecha mínima de liberación evaluada por el motor de alertas[cite: 1].
- **Auditoria (`san_auditoria`)**: Registro inmutable append-only de trazabilidad para la generación y resolución de alertas[cite: 1].

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los galpones con tiempo de retiro y aislamiento cumplido generan su alerta de reintegro en menos de 60 segundos a partir del momento exacto del vencimiento[cite: 1].
- **SC-002**: 0% de alertas duplicadas activas simultáneamente para la misma tupla de galpón y lote[cite: 1].
- **SC-003**: El 100% de las alertas de reintegro se resuelven y cierran automáticamente dentro de los 5 segundos posteriores a la confirmación de la certificación sanitaria (CU-VET-003)[cite: 1].
- **SC-004**: El tiempo de consulta del panel de alertas sanitarias activas por granja es inferior a 250 milisegundos en al menos el 95% de las solicitudes atendidas.
- **SC-005**: 100% de cumplimiento multi-tenant: Cero incidentes de recepción de alertas de galpones pertenecientes a granjas distintas a la autorizada para el usuario[cite: 1].