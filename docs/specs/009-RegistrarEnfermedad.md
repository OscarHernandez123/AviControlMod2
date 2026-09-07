# Feature Specification: Registrar Enfermedad

**Created**: 2026-09-03

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Registro de Patologías en el Catálogo de la Granja (Priority: P1)

Como Médico Veterinario de la granja (o Administrador autorizado), quiero ingresar nuevas enfermedades aviares al catálogo asignándoles un código único, nombre oficial, nivel de gravedad y descripción básica, para disponer de diagnósticos normalizados al validar aislamientos y diagnosticar galpones o formular esquemas de medicación.

**Why this priority**: Es el catálogo maestro de causas clínicas. Sin este registro, el veterinario no puede diagnosticar formalmente un galpón ni asociar las medidas de contención o tratamientos a una enfermedad comprobada.

**Independent Test**: Se prueba ingresando una patología con código nuevo y datos obligatorios completos. Se comprueba que quede en estado `ACTIVA`, aparezca disponible de inmediato en la pantalla de aislamiento de galpones y se asiente en la auditoría inmutable de la granja.

**Acceptance Scenarios**:

1. **Scenario**: Alta exitosa de una nueva patología en el catálogo
   - **Given** que el usuario autenticado cuenta con rol "VETERINARIO" o "ADMINISTRADOR"
   - **And** el código "ENF-BRONQ-01" no existe previamente en la base de datos de la granja
   - **When** guarda la patología con código "ENF-BRONQ-01", nombre "Bronquitis Infecciosa Aviar", nivel de riesgo "CRÍTICO" y su descripción técnica
   - **Then** el sistema almacena la enfermedad en estado "ACTIVA"
   - **And** emite el evento de dominio "EnfermedadRegistrada"
   - **And** registra el alta en "san_auditoria" con la firma del usuario responsable
   - **And** la patología queda visible de inmediato para su uso en diagnósticos de galpón

2. **Scenario**: Rechazo por intento de duplicar código nosológico
   - **Given** que ya existe una patología registrada con el código "ENF-BRONQ-01"
   - **When** se intenta guardar otra enfermedad usando ese mismo código "ENF-BRONQ-01"
   - **Then** el sistema rechaza la operación informando que el código ya está en uso
   - **And** no altera los datos de la enfermedad preexistente

3. **Scenario**: Limpieza y formateo automático del código
   - **Given** que el código "ENF-GUMB-01" está libre para registro
   - **When** el usuario lo ingresa con espacios accidentales o minúsculas (ej. " enf-gumb-01 ")
   - **Then** el sistema limpia los espacios, lo transforma a mayúsculas ("ENF-GUMB-01") y lo almacena de forma estandarizada

---

### User Story 2 - Desactivación y Reactivación Segura de Patologías (Priority: P2)

Como Médico Veterinario de la granja, quiero desactivar patologías obsoletas o registradas por error sin borrarlas físicamente de la base de datos, para que dejen de aparecer en nuevos diagnósticos sin romper el historial de los galpones y lotes que las tuvieron en el pasado.

**Why this priority**: Protege la integridad histórica de la granja. Si se borrara un registro de enfermedad, los aislamientos y tratamientos históricos asociados perderían su diagnóstico de origen, invalidando la trazabilidad de bioseguridad.

**Independent Test**: Se desactiva una patología con antecedentes clínicos. Se verifica que cambie a `INACTIVA`, que los expedientes pasados sigan consultándose sin error y que el sistema impida seleccionarla en nuevos aislamientos.

**Acceptance Scenarios**:

1. **Scenario**: Desactivación justificada de una enfermedad
   - **Given** la patología "ENF-COR-01" en estado "ACTIVA"
   - **When** el Veterinario solicita su desactivación explicando el motivo técnico
   - **Then** el estado de la patología pasa a "INACTIVA"
   - **And** se publica el evento de dominio "EnfermedadInactivada"
   - **And** se asienta la modificación en la tabla de auditoría

2. **Scenario**: Bloqueo de selección para enfermedad inactiva
   - **Given** la enfermedad "ENF-NEWC-02" en estado "INACTIVA"
   - **When** el Veterinario intenta seleccionarla al confirmar un nuevo aislamiento en un galpón
   - **Then** el sistema interrumpe la confirmación indicando que la patología no se encuentra vigente
   - **And** exige seleccionar una enfermedad activa del catálogo

3. **Scenario**: Reactivación de una enfermedad previamente desactivada
   - **Given** la enfermedad "ENF-COR-01" en estado "INACTIVA"
   - **When** el Veterinario solicita su reactivación técnica
   - **Then** la patología retorna al estado "ACTIVA"
   - **And** queda nuevamente disponible para futuros diagnósticos
   - **And** se emite el evento "EnfermedadReactivada" registrando la novedad en auditoría

---

### User Story 3 - Inmutabilidad, Blindaje Histórico y Auditoría (Priority: P3)

Como auditor sanitario y responsable técnico de la granja, quiero garantizar que los registros del catálogo no puedan ser eliminados físicamente y que cualquier intento de modificación no autorizado sea bloqueado, para que la información epidemiológica no sufra manipulaciones ni pérdidas accidentales.

**Why this priority**: La pérdida de datos nosológicos destruye la validez de los reportes epidemiológicos y expone a la granja a no conformidades ante auditorías agropecuarias oficiales.

**Independent Test**: Se intentan ejecutar órdenes de borrado directo (`DELETE`) y solicitudes desde roles operativos sin privilegios, comprobando que el sistema bloquee el borrado físico y conserve siempre el historial en `san_auditoria`.

**Acceptance Scenarios**:

1. **Scenario**: Prohibición total de borrado físico en el catálogo
   - **Given** una enfermedad registrada en el catálogo de la granja
   - **When** se intenta ejecutar una instrucción de eliminación física directa (`DELETE`)
   - **Then** el sistema bloquea y rechaza la operación
   - **And** conserva el registro íntegro en la base de datos

2. **Scenario**: Rechazo a personal no facultado clínicamente
   - **Given** un usuario autenticado con rol sin facultades sanitarias (ej. "TRABAJADOR")
   - **When** intenta crear, desactivar o modificar una patología del catálogo
   - **Then** el sistema deniega el acceso por falta de permisos
   - **And** no aplica ninguna modificación en la base de datos

---

### Edge Cases

- **Cambio de severidad con galpones aislados en curso**: Si el Veterinario ajusta el nivel de riesgo de una enfermedad en el catálogo, los galpones que ya estén aislados conservan la severidad asignada al momento de su diagnóstico; el cambio rige exclusivamente para casos futuros.
- **Datos obligatorios incompletos o vacíos**: Si se intenta crear una patología omitiendo el código, el nombre oficial o con una descripción técnica menor a 10 caracteres, el sistema rechaza la solicitud de forma temprana antes de procesarla.
- **Reintentos por caída de red (Idempotencia)**: Si la orden de guardado se reenvía por problemas de conexión bajo la misma clave de operación, el sistema devuelve la confirmación previa sin duplicar la patología en la base de datos.

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: El sistema DEBE permitir registrar patologías aviares exigiendo obligatoriamente código nosológico único, nombre clínico oficial, nivel de gravedad y descripción técnica.
- **FR-002**: El acceso para registrar, desactivar o reactivar patologías en el catálogo DEBE estar restringido exclusivamente a usuarios autenticados con rol `VETERINARIO` o `ADMINISTRADOR`.
- **FR-003**: El código nosológico DEBE ser estrictamente único en la base de datos de la granja, normalizando espacios y convirtiendo el texto a mayúsculas antes de verificar.
- **FR-004**: Toda nueva patología DEBE registrarse con estado inicial `ACTIVA`.
- **FR-005**: El sistema DEBE permitir alternar el estado de una patología entre `ACTIVA` e `INACTIVA` mediante órdenes explícitas de dominio debidamente justificadas por el usuario.
- **FR-006**: El sistema DEBE impedir la selección de patologías en estado `INACTIVA` tanto en la validación y diagnóstico de aislamientos como en la prescripción de medicación.
- **FR-007**: Al registrarse una patología, el sistema DEBE publicar el evento de dominio `EnfermedadRegistrada`; al cambiar su estado, DEBE publicar `EnfermedadInactivada` o `EnfermedadReactivada` según corresponda.
- **FR-008**: Cada creación o cambio de estado DEBE persistir un registro histórico permanente e inmutable en `san_auditoria` con usuario responsable, fecha, hora y motivo técnico.
- **FR-009**: Queda ESTRICTAMENTE PROHIBIDO el borrado físico (`DELETE` SQL) sobre cualquier patología del catálogo.
- **FR-010**: El sistema DEBE aplicar control de concurrencia optimista (`version`) y soportar cabeceras de idempotencia para evitar registros duplicados ante reintentos de red.

### Key Entities

- **Enfermedad**: Aggregate Root que define la patología clínica aplicable a los lotes de la granja. Atributos: `id` (UUID), `codigo` (String normalizado único), `nombre` (String), `descripcion` (Texto), `nivelRiesgo` (`LEVE`, `MODERADO`, `CRÍTICO`, `EMERGENCIA_SANITARIA`), `activa` (Boolean), `version` (control concurrente) y marcas temporales.
- **Severidad Epidemiológica**: Value Object que tipifica el nivel de riesgo sanitario sugerido para clasificar la urgencia de la contención.
- **Auditoría Sanitaria (`san_auditoria`)**: Registro inmutable append-only que respalda las altas y modificaciones operativas en el catálogo.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: 0% de códigos nosológicos duplicados en la base de datos de la granja gracias a la validación de unicidad previa y a la restricción relacional única del campo.
- **SC-002**: El 100% de las patologías registradas en estado `ACTIVA` quedan disponibles para consulta y selección inmediata en el flujo de validación y diagnóstico de galpones.
- **SC-003**: Cero casos (0%) de patologías inactivas admitidas en nuevos registros de aislamiento o tratamientos médicos.
- **SC-004**: El tiempo de respuesta al registrar o actualizar patologías es menor a 250 milisegundos en condiciones normales de operación.
- **SC-005**: El 100% de las operaciones sobre el catálogo generan una traza persistida en `san_auditoria` con el usuario responsable y código de correlación.
- **SC-006**: Cero incidentes (0%) de patologías eliminadas físicamente de la base de datos durante todo el ciclo de vida del sistema.
