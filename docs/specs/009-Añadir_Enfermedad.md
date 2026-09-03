# Feature Specification: Añadir Enfermedad (CU-VET-002)

**Created**: 2026-09-03  
**Status**: Baseline Aprobado  
**Bounded Context**: `SanitaryManagementContext`  
**Actor Principal**: Médico Veterinario (Secundario: Administrador)  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registro de Patología en el Catálogo Maestro (Priority: P1)

Como Médico Veterinario (o Administrador del sistema), quiero registrar una nueva patología avícola con su código único, denominación técnica, nivel de riesgo y descripción clínica, para que esté disponible de forma estructurada e inequívoca al momento de validar aislamientos y registrar tratamientos.

**Why this priority**: Sin un catálogo formal de patologías activas, el sistema no puede satisfacer la precondición médica ni el invariante relacional de CU-VET-001, impidiendo el diagnóstico estandarizado y la trazabilidad de bioseguridad.

**Independent Test**: Se valida enviando el comando de creación con código normalizado y datos obligatorios. Se comprueba que la entidad persista con estado inicial `ACTIVA`, esté disponible para selección clínica, emita el evento `EnfermedadRegistrada` y genere registro append-only en auditoría.

**Acceptance Scenarios**:

1. **Scenario**: Registro exitoso de una nueva enfermedad
   - **Given** un usuario autenticado con rol `VETERINARIO` o `ADMINISTRADOR` en la granja "GR-001"
   - **And** el código nosológico "ENF-BRONQ-01" no existe previamente en el catálogo
   - **When** envía la solicitud con código "ENF-BRONQ-01", nombre "Bronquitis Infecciosa Aviar", nivel de riesgo "CRITICO" y descripción clínica correspondiente
   - **Then** el sistema persiste la patología con estado "ACTIVA"
   - **And** emite el evento de dominio "EnfermedadRegistrada"
   - **And** registra una entrada inmutable en "san_auditoria"
   - **And** responde con código HTTP 201 Created con el recurso persistido

2. **Scenario**: Rechazo por intento de duplicidad de código
   - **Given** que ya existe registrada una enfermedad con código "ENF-BRONQ-01" en la granja "GR-001"
   - **When** el usuario intenta registrar otra patología utilizando el mismo código "ENF-BRONQ-01"
   - **Then** el sistema aborta la transacción
   - **And** retorna el código de error "VET-011: CODIGO_ENFERMEDAD_DUPLICADO" con código HTTP 409 Conflict
   - **And** no altera el catálogo existente

---

### User Story 2 - Gestión del Ciclo de Vida y Deshabilitación Segura (Priority: P2)

Como Médico Veterinario, quiero inactivar patologías en desuso o erróneas sin eliminarlas físicamente de la base de datos, para impedir que se seleccionen en nuevos aislamientos mientras se preserva intacto el historial de los lotes pasados que la padecieron.

**Why this priority**: Garantiza el cumplimiento normativo de bioseguridad y la integridad referencial histórica; prohíbe roturas de integridad en aislamientos previos asociados a la patología.

**Independent Test**: Se inactiva una enfermedad que ya cuenta con aislamientos históricos. Se comprueba que su estado cambie a `INACTIVA`, que los registros pasados sigan consultándose sin alteración y que CU-VET-001 la rechace de inmediato si se intenta usar en una nueva validación.

**Acceptance Scenarios**:

1. **Scenario**: Inactivación exitosa de patología de catálogo
   - **Given** una enfermedad "ENF-GUMB-01" en estado "ACTIVA"
   - **When** el Veterinario solicita su inactivación justificando el motivo
   - **Then** el estado de la patología cambia a "INACTIVA"
   - **And** se emite el evento de dominio "EnfermedadInactivada"
   - **And** se registra la acción en la auditoría del sistema

2. **Scenario**: Bloqueo de uso de enfermedad inactiva en nuevas validaciones
   - **Given** una enfermedad "ENF-NEWC-02" en estado "INACTIVA"
   - **When** el Veterinario intenta seleccionarla para confirmar un nuevo aislamiento en CU-VET-001
   - **Then** el sistema rechaza la operación con código de error "VET-006: ENFERMEDAD_INACTIVA" (HTTP 422)
   - **And** exige seleccionar una patología vigente

---

### User Story 3 - Integridad Multi-Tenant y Protección Histórica (Priority: P3)

Como Responsable de Seguridad del Sistema, quiero asegurar que las definiciones de catálogo respeten el aislamiento de granja y que los registros no puedan ser borrados físicamente por ninguna interfaz, garantizando trazabilidad legal.

**Why this priority**: Evita la corrupción cruzada de nomenclaturas clínicas entre distintas granjas e impide la pérdida destructiva de datos sanitarios.

**Independent Test**: Se ejecutan pruebas de inyección de comandos con tokens de granjas ajenas y sentencias de eliminación directa, verificando el bloqueo de seguridad y la ausencia de soporte para operaciones `DELETE`.

**Acceptance Scenarios**:

1. **Scenario**: Rechazo por intento de manipulación cross-tenant
   - **Given** un catálogo perteneciente a la Granja "GR-002"
   - **When** un usuario autenticado con permisos exclusivos en Granja "GR-001" intenta modificarlo
   - **Then** el sistema deniega el acceso con código HTTP 403 Forbidden
   - **And** retorna el error "VET-009: GRANJA_NO_AUTORIZADA"
   - **And** registra una alerta de seguridad en la auditoría

2. **Scenario**: Prohibición absoluta de borrado físico
   - **Given** una patología existente en el catálogo
   - **When** se intenta invocar una eliminación física directa por API
   - **Then** el sistema responde con código HTTP 405 Method Not Allowed
   - **And** no ejecuta ninguna sentencia destructiva sobre la base de datos

---

### Edge Cases

- **Normalización de código con espacios o minúsculas:** Si el usuario ingresa `" enf-bronq-01 "`, el sistema aplica limpieza de espacios y conversión a mayúsculas automática (`"ENF-BRONQ-01"`) antes de evaluar unicidad y persistencia.
- **Modificación de severidad con aislamientos activos:** Si se actualiza la severidad por defecto en el catálogo, los aislamientos que ya están en curso mantienen la severidad que les fue asignada al momento de su confirmación; el cambio solo aplica a registros futuros.
- **Reactivación de patología previamente inactivada:** Si una enfermedad inactiva necesita volver a utilizarse, el sistema admite un comando explícito de reactivación que valida nuevamente consistencia y emite `EnfermedadReactivada`.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema debe permitir registrar patologías avícolas exigiendo código único, nombre oficial, nivel de riesgo y descripción clínica.
- **FR-002**: El sistema debe exigir autenticación con rol `VETERINARIO` o `ADMINISTRADOR`, validando estrictamente el `granjaId` del token para aislamiento multi-tenant.
- **FR-003**: El sistema debe asegurar que el código de la enfermedad sea único dentro del contexto de la granja (o catálogo corporativo asignado).
- **FR-004**: El sistema debe asignar por defecto el estado `ACTIVA` a toda nueva patología registrada.
- **FR-005**: El sistema debe permitir transicionar el estado de una enfermedad entre `ACTIVA` e `INACTIVA` mediante comandos explícitos de dominio.
- **FR-006**: El sistema debe prohibir la selección de enfermedades en estado `INACTIVA` en cualquier flujo de validación de aislamiento o prescripción médica.
- **FR-007**: El sistema debe publicar el evento de dominio `EnfermedadRegistrada` inmediatamente después de completar la persistencia transaccional.
- **FR-008**: El sistema debe registrar una traza inmutable append-only en `san_auditoria` con cada alta, inactivación o reactivación de catálogo.
- **FR-009**: El sistema debe prohibir el borrado físico (`DELETE` relacional) sobre los registros de patologías del catálogo.
- **FR-010**: El sistema debe rechazar solicitudes duplicadas de creación mediante validación de índice único y cabecera `X-Idempotency-Key`.

### Key Entities

- **Enfermedad** *(Aggregate Root de Catálogo)*: Entidad raíz nosológica. Atributos: `id` (UUID), `granjaId` (UUID, nullable para soporte corporativo global), `codigo` (String normalizado único), `nombre` (String), `descripcion` (Text), `nivelRiesgo` (`LEVE`, `MODERADO`, `CRITICO`, `EMERGENCIA_SANITARIA`), `activa` (Boolean) y timestamps de control.
- **SeveridadRiesgo** *(Value Object)*: Nivel de gravedad epidemiológica por defecto asociado a la patología.
- **Auditoria (`san_auditoria`)**: Registro histórico append-only inmutable de trazabilidad operativa y cambios de estado.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cero por ciento (0%) de duplicidad de códigos nosológicos en la base de datos gracias a la restricción única relacional `(granja_id, codigo)`.
- **SC-002**: El 100% de las patologías creadas en estado `ACTIVA` quedan inmediatamente disponibles para ser consumidas por CU-VET-001.
- **SC-003**: El tiempo de respuesta para el registro y consulta de patologías es inferior a 250 milisegundos en al menos el 95% de las solicitudes atendidas bajo condiciones normales de operación.
- **SC-004**: El 100% de las modificaciones de estado en el catálogo quedan asentadas en `san_auditoria` con identificación del usuario responsable y `correlationId`.
- **SC-005**: Cero incidencias de pérdida de integridad referencial histórica tras la inactivación de enfermedades con antecedentes clínicos.