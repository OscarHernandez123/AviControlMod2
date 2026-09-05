# Feature Specification: Solicitar validación para aislar galpón

**Created**: 2026-09-05  

## User Scenarios & Testing 

### User Story 1 - Crear y enviar solicitud de validación de aislamiento (Priority: P1)

Como trabajador u operario de granja, quiero registrar y enviar una solicitud de validación para aislar un galpón asignado que se encuentra en cosecha cuando observe síntomas o sospechas sanitarias, informando la justificación, signos clínicos y la población de aves vivas en riesgo, para que el veterinario reciba la solicitud y proceda con la evaluación correspondiente.

**Why this priority**: Es el punto de inicio para la contención preventiva de riesgos biológicos durante la etapa de cosecha. Permite al trabajador alertar formalmente al veterinario sin alterar por sí mismo el estado definitivo del galpón.

**Independent Test**: Se puede probar seleccionando un galpón asignado en estado "En cosecha" con lote activo, ingresando el motivo de la sospecha y los signos clínicos observados, y verificando que el sistema cree la solicitud en estado "Pendiente de validación", asociada al galpón y al lote, y disponible para la revisión del veterinario.

**Acceptance Scenarios**:

1. **Scenario**: Creación exitosa de la solicitud de validación de aislamiento
   - **Given** que un trabajador autenticado tiene asignado un galpón que se encuentra en estado "En cosecha" con un lote activo
   - **When** el trabajador diligencia la justificación sanitaria, los signos observados y envía la solicitud de validación
   - **Then** el sistema almacena la solicitud con UUID único, fecha/hora de emisión, motivo, signos clínicos, referencia al galpón y lote
   - **And** establece el estado de la solicitud como "Pendiente de validación"
   - **And** conserva el estado del galpón como "En cosecha" hasta que el veterinario resuelva la validación

2. **Scenario**: Intento de solicitud en un galpón con estado diferente a "En cosecha"
   - **Given** que el galpón asignado se encuentra en estado "productivo", "disponible" o "vaciado sanitario"
   - **When** el trabajador intenta registrar una solicitud de validación de aislamiento
   - **Then** el sistema rechaza la solicitud, informa que el galpón debe encontrarse en estado "En cosecha" y no crea el registro

3. **Scenario**: Intento de solicitud con justificación o síntomas vacíos
   - **Given** un galpón asignado en estado "En cosecha"
   - **When** el trabajador intenta enviar la solicitud sin ingresar el motivo de la sospecha o los signos clínicos
   - **Then** el sistema bloquea el envío, resalta los campos obligatorios requeridos y no registra la solicitud

4. **Scenario**: Intento de solicitud en un galpón no asignado al trabajador
   - **Given** que un trabajador intenta solicitar validación para un galpón que no tiene a su cargo
   - **When** solicita enviar la petición
   - **Then** el sistema deniega el acceso, informa la falta de asignación y no crea ninguna solicitud

---

### User Story 2 - Control de solicitudes pendientes y prevención de duplicados (Priority: P2)

Como trabajador u operario de granja, quiero que el sistema me informe si el galpón ya tiene una solicitud de aislamiento en trámite, para evitar duplicar peticiones y conocer que el veterinario ya tiene la novedad en espera.

**Why this priority**: Previene la duplicidad de registros y asegura un flujo ordenado y claro entre el operario y el equipo médico veterinario.

**Independent Test**: Se puede probar intentando crear una segunda solicitud para un galpón que ya cuenta con una solicitud en estado "Pendiente de validación", verificando que el sistema bloquee el reenvío y muestre el estado pendiente actual.

**Acceptance Scenarios**:

1. **Scenario**: Bloqueo de solicitud duplicada en trámite
   - **Given** que un galpón en estado "En cosecha" ya tiene una solicitud previa en estado "Pendiente de validación"
   - **When** el trabajador intenta registrar una nueva solicitud para ese mismo galpón
   - **Then** el sistema rechaza la nueva solicitud e informa que ya existe una solicitud en trámite pendiente de atención veterinaria

---

### Edge Cases

- **¿Qué sucede si el galpón no cuenta con un lote activo registrado?**
  - El sistema rechaza la creación de la solicitud e informa que se requiere un lote activo para solicitar la validación de aislamiento.

- **¿Qué sucede si la población actual de aves vivas en el lote es 0?**
  - El sistema bloquea la solicitud de aislamiento preventivo e indica que el lote no cuenta con aves vivas alojadas.

- **¿El envío de la solicitud cambia directamente el estado del galpón a "aislamiento"?**
  - No, el envío de la solicitud registra la petición en estado "Pendiente de validación"; el cambio de estado del galpón a `aislamiento` es potestad exclusiva del veterinario tras evaluar y validar la solicitud (SPEC-010).

- **¿Cómo valida el sistema los textos de justificación y síntomas ingresados?**
  - El sistema exige que el motivo y los signos clínicos contengan texto válido no vacío y no compuesto únicamente de espacios en blanco.

- **¿Puede un trabajador anular o retirar una solicitud una vez enviada?**
  - No, una vez enviada la solicitud queda formalmente registrada en la bitácora sanitaria de la granja a la espera de la intervención veterinaria.

- **¿Qué sucede si un usuario no autenticado o sin rol de trabajador intenta crear la solicitud?**
  - El sistema bloquea la operación y exige autenticación con un usuario autorizado.

---

## Requirements 

### Functional Requirements

- **FR-001**: El sistema DEBE permitir la creación y envío de solicitudes de validación de aislamiento exclusivamente a usuarios autenticados con rol de trabajador (y administradores autorizados).
- **FR-002**: El sistema DEBE restringir el registro de solicitudes exclusivamente a los galpones que el trabajador tenga formalmente asignados a su cargo.
- **FR-003**: El sistema DEBE validar que el galpón seleccionado se encuentre en estado `En cosecha` como condición obligatoria para admitir la solicitud.
- **FR-004**: El sistema DEBE identificar el lote activo del galpón mediante la llave foránea almacenada en la entidad Lote, seleccionando el lote con la fecha de ingreso más reciente.
- **FR-005**: Para registrar la solicitud, el sistema DEBE exigir obligatoriamente: identificador del galpón, identificador del lote activo, fecha y hora de emisión, motivo/justificación de la sospecha sanitaria y descripción de los signos clínicos observados.
- **FR-006**: El sistema DEBE capturar y asociar a la solicitud la `población actual` (número de aves vivas) y la edad en días del lote activo al momento de la petición.
- **FR-007**: Al confirmar el envío, el sistema DEBE almacenar la solicitud con UUID único, fecha y hora de emisión, motivo, signos clínicos, población viva, llave foránea del galpón, llave foránea del lote, trabajador responsable y estado inicial `Pendiente de validación`.
- **FR-008**: El sistema NO DEBE modificar el estado del galpón como consecuencia del registro o envío de la solicitud de validación.
- **FR-009**: El sistema DEBE poner la solicitud generada a disposición del rol `Veterinario` para dar inicio al flujo de evaluación y validación de aislamiento (SPEC-010).
- **FR-010**: El sistema DEBE validar que no exista una solicitud previa en estado `Pendiente de validación` para el mismo galpón antes de admitir un nuevo registro.
- **FR-011**: El sistema DEBE garantizar que las solicitudes de validación registradas sean inmutables y conserven su trazabilidad en el sistema.
- **FR-012**: El sistema NO DEBE permitir registrar solicitudes en galpones sin lote activo o cuya población actual sea cero.

### Key Entities 

- **Solicitud de validación de aislamiento**: Representa la petición formal emitida por el trabajador para que el veterinario evalúe la necesidad de aislar el galpón.
  - Atributos: `UUID único`, `Fecha y hora de emisión`, `Motivo / Justificación sanitaria`, `Signos clínicos observados`, `Población actual reportada`, `Estado` (`Pendiente de validación`), `Llave foránea del galpón`, `Llave foránea del lote`, `Trabajador responsable`.
- **Galpón**: Representa la unidad física de producción avícola en cosecha para la cual se solicita la revisión.
  - Atributos utilizados: `UUID único`, `Nombre`, `Aforo máximo`, `Estado` (`En cosecha`).
- **Lote**: Representa el grupo de aves registrado que referencia al galpón mediante llave foránea.
  - Atributos utilizados: `UUID único`, `Nombre`, `Población actual` (número de aves vivas en riesgo), `Fecha de ingreso` (edad en días), `Llave foránea del galpón`.
- **Trabajador / Operario de granja**: Usuario autenticado que detecta la anomalía y genera la solicitud.
- **Veterinario**: Rol destinatario que recibe la solicitud para proceder con la evaluación y validación sanitaria.

---

## Success Criteria 

### Measurable Outcomes

- **SC-001**: El 100 % de las solicitudes de validación de aislamiento confirmadas se almacena en estado `Pendiente de validación` y vincula con exactitud el UUID del galpón y del lote activo.
- **SC-002**: El 100 % de las solicitudes creadas conserva el estado `En cosecha` del galpón sin alterarlo hasta la resolución veterinaria.
- **SC-003**: El 100 % de los intentos de crear solicitudes sobre galpones en estados distintos a `En cosecha` es rechazado por el sistema.
- **SC-004**: El 100 % de los intentos de enviar solicitudes duplicadas para un galpón con una petición en trámite es bloqueado.
- **SC-005**: El 100 % de las solicitudes confirmadas queda disponible de forma inmediata para la consulta y evaluación del rol Veterinario.
- **SC-006**: El 100 % de las solicitudes almacena la justificación, signos clínicos observados, población viva y usuario responsable.

---

## Out of Scope

- La evaluación médica, diagnóstico clínico y validación formal del aislamiento (cubierto en SPEC-010 *Validar aislamiento de un galpón*).
- El cambio definitivo del estado del galpón a `aislamiento` o la certificación sanitaria (responsabilidad exclusiva del veterinario).
- La toma de muestras de laboratorio o prescripción de medicamentos para el lote.
- La orden y ejecución de sacrificio sanitario o vaciado del galpón (cubierto en SPEC-012 *Ordenar sacrificio sanitario*).
- La creación, edición o administración de galpones, lotes y usuarios.
