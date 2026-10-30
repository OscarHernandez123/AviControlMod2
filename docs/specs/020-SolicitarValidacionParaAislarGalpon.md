# Feature Specification: 020 - Solicitar validación para aislar galpón

**Created**: 2026-09-05

**Last Updated**: 2026-10-28

## User Scenarios & Testing

### User Story 1 - Crear y enviar solicitud de validación de aislamiento (Priority: P1)

Como trabajador u operario de granja, quiero registrar y enviar una solicitud de validación para aislar un galpón seleccionado que se encuentra en estado `productivo` cuando observe síntomas o sospechas sanitarias, informando el motivo, los signos clínicos y capturando la población de aves vivas en riesgo, para que el veterinario reciba la solicitud y proceda con la evaluación correspondiente.

**Why this priority**: Es el punto de inicio para la contención preventiva de riesgos biológicos durante la etapa productiva. Permite al trabajador alertar formalmente al veterinario sin alterar por sí mismo el estado operativo del galpón.

**Independent Test**: Se puede probar seleccionando un galpón en estado `productivo` con lote activo y población mayor que cero, ingresando el motivo de la sospecha y los signos clínicos observados, y verificando que el sistema cree la solicitud inmutable en estado `PENDIENTE_VALIDACION`, asociada al galpón y al lote, conservando el galpón en `productivo` y dejándola disponible para la revisión del veterinario.

**Acceptance Scenarios**:

1. **Scenario**: Creación exitosa de la solicitud de validación de aislamiento
   - **Given** que un trabajador autenticado selecciona un galpón que se encuentra en estado `productivo` con un lote activo cuya población actual es mayor que cero
   - **When** el trabajador diligencia el motivo de la sospecha sanitaria, los signos clínicos observados y envía la solicitud de validación
   - **Then** el sistema almacena la solicitud inmutable con UUID único, fecha y hora de emisión, motivo, signos clínicos, población actual reportada, edad del lote en días y referencias al galpón y al lote
   - **And** establece el estado de la solicitud como `PENDIENTE_VALIDACION`
   - **And** conserva el estado del galpón como `productivo` hasta que el veterinario resuelva la validación (Spec 010)

2. **Scenario**: Intento de solicitud en un galpón con estado diferente a productivo
   - **Given** que el galpón seleccionado se encuentra en estado `aislamiento`, `en cosecha`, `disponible`, `mantenimiento` o `vaciado sanitario`
   - **When** el trabajador intenta registrar una solicitud de validación de aislamiento
   - **Then** el sistema rechaza la solicitud, informa que el galpón debe encontrarse en estado `productivo` y no crea el registro

3. **Scenario**: Intento de solicitud con motivo o signos clínicos vacíos
   - **Given** un galpón seleccionado en estado `productivo`
   - **When** el trabajador intenta enviar la solicitud sin ingresar el motivo de la sospecha o los signos clínicos, o ingresando solo espacios en blanco
   - **Then** el sistema bloquea el envío, resalta los campos obligatorios requeridos y no registra la solicitud

4. **Scenario**: Intento de solicitud por un usuario sin permisos de rol
   - **Given** que un usuario no autenticado o sin rol autorizado (`ROLE_TRABAJADOR` o administrador autorizado) intenta registrar una solicitud de validación
   - **When** solicita enviar la petición
   - **Then** el sistema deniega el acceso y no crea ninguna solicitud

---

### User Story 2 - Control de solicitudes pendientes, bandeja veterinaria y prevención de duplicados (Priority: P2)

Como trabajador u operario de granja, quiero que el sistema impida crear una segunda solicitud si el galpón ya tiene una solicitud de aislamiento pendiente de validación, y como veterinario, quiero consultar la bandeja de solicitudes pendientes para evaluarlas en orden.

**Why this priority**: Previene la duplicidad de peticiones concurrentes sobre un mismo galpón y asegura una bandeja de trabajo única, ordenada y clara para el médico veterinario.

**Independent Test**: Se puede probar intentando crear una segunda solicitud para un galpón que ya cuenta con una solicitud en estado `PENDIENTE_VALIDACION`, verificando que el sistema bloquee el reenvío con error de conflicto (`409 Conflict`) y que la solicitud única aparezca en la consulta paginada de solicitudes pendientes del veterinario.

**Acceptance Scenarios**:

1. **Scenario**: Bloqueo de solicitud duplicada en trámite
   - **Given** que un galpón en estado `productivo` ya tiene una solicitud previa en estado `PENDIENTE_VALIDACION`
   - **When** un trabajador intenta registrar una nueva solicitud para ese mismo galpón
   - **Then** el sistema rechaza la nueva solicitud e informa que ya existe una solicitud en evaluación pendiente de atención veterinaria

2. **Scenario**: Consulta de la bandeja de solicitudes pendientes por el veterinario
   - **Given** que existen solicitudes registradas en estado `PENDIENTE_VALIDACION`
   - **When** el veterinario consulta el listado de solicitudes pendientes
   - **Then** el sistema presenta las solicitudes paginadas con su galpón, lote, motivo, signos clínicos, población reportada, edad del lote y fecha de emisión

---

### Edge Cases

- **¿Qué sucede si el galpón no cuenta con un lote activo registrado?**
  - El sistema rechaza la creación de la solicitud e informa que se requiere un lote activo alojado para solicitar la validación de aislamiento.
- **¿Qué sucede si la población actual de aves vivas en el lote es 0?**
  - El sistema bloquea la solicitud de aislamiento preventivo e indica que el lote no cuenta con aves vivas alojadas.
- **¿El envío de la solicitud cambia directamente el estado del galpón a `aislamiento`?**
  - No, el envío de la solicitud únicamente registra la petición en estado `PENDIENTE_VALIDACION`; el cambio de estado del galpón a `aislamiento` es potestad exclusiva del veterinario tras evaluar y aprobar la solicitud (Spec 010).
- **¿Cómo valida el sistema los textos de justificación y síntomas ingresados?**
  - El sistema exige que el motivo y los signos clínicos contengan texto válido no vacío y no compuesto únicamente de espacios en blanco (aplicando `trim()`).
- **¿Puede un trabajador editar, anular o retirar una solicitud una vez enviada?**
  - No, una vez enviada la solicitud queda registrada de forma inmutable en la bitácora sanitaria a la espera de la decisión veterinaria.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir la creación y envío de solicitudes de validación de aislamiento exclusivamente a usuarios autenticados con rol de trabajador y administradores autorizados.
- **FR-002**: El sistema DEBE permitir que cualquier usuario autenticado con rol de trabajador registre solicitudes de validación sobre cualquier galpón en estado `productivo` con lote activo, sin requerir validación de asignación trabajador-galpón.
- **FR-003**: El sistema DEBE validar en el módulo 1 que el galpón seleccionado se encuentre en estado vigente `productivo` como condición obligatoria para admitir la solicitud.
- **FR-004**: El sistema DEBE identificar el lote actualmente alojado en el galpón mediante la consulta propietaria del módulo 1 y verificar que su población actual sea mayor que cero.
- **FR-005**: Para registrar la solicitud, el sistema DEBE exigir obligatoriamente el motivo/justificación de la sospecha sanitaria y la descripción de los signos clínicos observados.
- **FR-006**: El sistema DEBE capturar automáticamente del módulo 1 y asociar a la solicitud el identificador del lote activo, la población actual (número de aves vivas) y la edad en días del lote al momento de la emisión.
- **FR-007**: Al confirmar el envío, el sistema DEBE almacenar la solicitud con UUID único, fecha y hora de emisión (UTC), motivo, signos clínicos, población actual reportada, edad en días, llave foránea del galpón (`galponId`), llave foránea del lote (`loteId`), trabajador emisor y estado inicial `PENDIENTE_VALIDACION`.
- **FR-008**: El sistema NO DEBE modificar el estado del galpón como consecuencia del registro o envío de la solicitud de validación.
- **FR-009**: El sistema DEBE permitir a los usuarios con rol de veterinario (y administradores autorizados) consultar de forma paginada las solicitudes en estado `PENDIENTE_VALIDACION` para dar inicio al flujo de evaluación y validación de aislamiento (Spec 010).
- **FR-010**: El sistema DEBE exponer un mecanismo de consulta (endpoint `GET`) para que el veterinario pueda listar y filtrar la bandeja de solicitudes pendientes de validación.
- **FR-011**: El sistema DEBE garantizar en aplicación y persistencia que no exista más de una solicitud en estado `PENDIENTE_VALIDACION` para el mismo galpón simultáneamente; tras una decisión terminal (`APROBADA` o `RECHAZADA`), el galpón podrá admitir una nueva solicitud futura si cumple las reglas.
- **FR-012**: El sistema DEBE soportar cabeceras de idempotencia técnica (`X-Idempotency-Key`) y control de concurrencia optimista (`@Version`) para evitar la creación duplicada de solicitudes ante reintentos de red o peticiones simultáneas.
- **FR-013**: El sistema DEBE garantizar que las solicitudes de validación enviadas sean inmutables.
- **FR-014**: El sistema NO DEBE permitir registrar solicitudes en galpones sin lote activo o cuya población actual sea igual a cero.

### Non-Functional Requirements

| ID          | Categoría   | Requerimiento                                                                           | Métrica                                    |
| :---------- | :---------- | :-------------------------------------------------------------------------------------- | :----------------------------------------- |
| **NFR-001** | Rendimiento | Latencia de validación, creación y persistencia de la solicitud                         | < 1 segundo (p95) bajo carga normal        |
| **NFR-002** | Seguridad   | Acceso restringido exclusivamente a roles autorizados (TRABAJADOR / ADMINISTRADOR)      | HTTP 403 Forbidden a otros roles           |
| **NFR-003** | Integridad  | Restricción de unicidad a nivel de base de datos para solicitudes pendientes por galpón | 100% de bloqueo de duplicados concurrentes |

### Key Entities

- **Solicitud de validación de aislamiento**: Representa la petición formal e inmutable emitida por el trabajador para que el veterinario evalúe la necesidad de aislar un galpón.
  - **Atributos**: `UUID único`, `Fecha y hora de emisión`, `Motivo / Justificación sanitaria`, `Signos clínicos observados`, `Población actual reportada`, `Edad del lote en días`, `Estado` (`PENDIENTE_VALIDACION`, `APROBADA`, `RECHAZADA`), `galponId`, `loteId`, `trabajadorId`.
- **Galpón**: Representa la unidad física de producción avícola en estado `productivo` para la cual se solicita la revisión (Módulo 1).
  - **Atributos utilizados**: `UUID único`, `Nombre`, `Aforo máximo`, `Estado` (`productivo`).
- **Lote**: Representa el grupo de aves actualmente alojado en el galpón (Módulo 1).
  - **Atributos utilizados**: `UUID único`, `Nombre`, `Población actual` (`> 0`), `Fecha de ingreso` (edad en días), `galponId`.

## Success Criteria

### Measurable Outcomes

- **SC-001**: El 100 % de las solicitudes de validación de aislamiento confirmadas se almacena en estado `PENDIENTE_VALIDACION` y vincula con exactitud el UUID del galpón y del lote activo.
- **SC-002**: El 100 % de las solicitudes creadas conserva el estado `productivo` del galpón sin alterarlo hasta la resolución veterinaria.
- **SC-003**: El 100 % de los intentos de crear solicitudes sobre galpones en estados distintos a `productivo` o con población cero es rechazado por el sistema.
- **SC-004**: El 100 % de los intentos de enviar solicitudes duplicadas para un galpón que ya tiene una solicitud en `PENDIENTE_VALIDACION` es bloqueado con respuesta `409 Conflict`.
- **SC-005**: El 95 % de las solicitudes confirmadas queda disponible en menos de 1 segundo para la consulta y evaluación del rol Veterinario.
- **SC-006**: El 100 % de las solicitudes almacena el motivo, signos clínicos observados, población viva, edad del lote y usuario responsable de forma inmutable.

---

## Out of Scope

- La evaluación médica, diagnóstico clínico y aprobación o rechazo formal del aislamiento (cubierto en Spec 010 _Validar aislamiento de un galpón_ y Spec 011 _Diagnosticar un galpón_).
- El cambio definitivo del estado del galpón a `aislamiento` (responsabilidad exclusiva del veterinario en Spec 010).
- La prescripción de medicamentos o emisión de órdenes de sacrificio sanitario (cubierto en Spec 008, Spec 011 y Spec 012).
