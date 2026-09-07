# Feature Specification: Validar aislamiento de un galpón

**Created**: 2026-09-04  

## User Scenarios & Testing

### User Story 1 - Evaluar y validar el aislamiento solicitado (Priority: P1)

Como veterinario, quiero recibir la solicitud de revisión enviada por un trabajador y evaluar el galpón proporcionado por el módulo 1, para validar su aislamiento cuando corresponda y cambiar su estado de `En producción` a `aislamiento`.

**Why this priority**: La solicitud del trabajador permite que el veterinario comience la evaluación del galpón, pero la evaluación y la decisión de validar el aislamiento son responsabilidades exclusivas del veterinario.

**Independent Test**: Se puede probar utilizando una solicitud de revisión enviada por un trabajador para un galpón proporcionado por el módulo 1. El sistema debe permitir que únicamente un veterinario inicie la evaluación y valide el aislamiento. Si el galpón continúa en estado `En producción`, la validación debe cambiarlo a `aislamiento`; recibir o enviar la solicitud por sí solo no debe modificar su estado.

**Acceptance Scenarios**:

1. **Scenario**: Inicio de la evaluación a partir de la solicitud del trabajador
   - **Given** que un trabajador envió al veterinario una solicitud de revisión asociada con un galpón proporcionado por el módulo 1
   - **When** el veterinario recibe la solicitud y comienza la evaluación
   - **Then** el sistema le permite evaluar el galpón y conserva su estado sin cambios hasta que el veterinario complete la validación

2. **Scenario**: Validación correcta del aislamiento
   - **Given** que el veterinario está evaluando una solicitud enviada por un trabajador y el módulo 1 confirma que el galpón continúa en estado `En producción`
   - **When** el veterinario valida el aislamiento
   - **Then** el sistema cambia y guarda el estado del galpón como `aislamiento`

3. **Scenario**: Intento de evaluación sin solicitud previa
   - **Given** que el veterinario no ha recibido una solicitud de revisión enviada por un trabajador para el galpón
   - **When** intenta comenzar la evaluación o validar el aislamiento
   - **Then** el sistema rechaza la operación, informa que se requiere la solicitud y conserva el estado actual del galpón

4. **Scenario**: Intento de validación por el trabajador u otro usuario no autorizado
   - **Given** que un trabajador u otro usuario sin rol de veterinario intenta evaluar una solicitud o validar el aislamiento
   - **When** solicita ejecutar la acción
   - **Then** el sistema rechaza la operación y no modifica la solicitud ni el estado del galpón

5. **Scenario**: Galpón que no está en producción
   - **Given** que el veterinario recibió la solicitud, pero el módulo 1 informa que el estado vigente del galpón es diferente de `En producción`
   - **When** intenta validar el aislamiento
   - **Then** el sistema rechaza la validación, informa que el galpón no se encuentra en el estado requerido y conserva su estado actual

6. **Scenario**: Información del galpón incompleta o no disponible
   - **Given** que el módulo 1 no proporciona el galpón asociado con la solicitud o no permite conocer su estado vigente
   - **When** el veterinario intenta comenzar o completar la evaluación
   - **Then** el sistema rechaza la operación, informa que no pudo verificar el galpón y no modifica sus datos

### Edge Cases

- **Edge case #1 - Cambio del estado después de recibir la solicitud**

  - ¿Cómo maneja el sistema un galpón cuyo estado cambia después de que el veterinario recibe la solicitud y antes de completar la validación?  
    El sistema debe consultar nuevamente en el módulo 1 el estado vigente del galpón. Solo debe completar la validación si el estado continúa siendo `En producción`; en caso contrario, debe rechazarla e informar la inconsistencia.

- **Edge case #2 - Repetición de una validación completada**

  - ¿Cómo maneja el sistema un nuevo intento de validación sobre un galpón que ya se encuentra en estado `aislamiento`?  
    El sistema debe rechazar el nuevo intento, indicar que el galpón ya se encuentra en aislamiento y conservar su estado sin cambios adicionales.

- **Edge case #3 - Información incompleta o no disponible desde el módulo 1**

  - ¿Cómo maneja el sistema la evaluación cuando el módulo 1 no está disponible o devuelve información incompleta del galpón?  
    El sistema debe impedir que el veterinario complete la validación, identificar la información que no está disponible y no cambiar el estado del galpón utilizando datos inventados o desactualizados.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir que únicamente los usuarios con rol de veterinario comiencen la evaluación y validen el aislamiento de un galpón.
- **FR-002**: El veterinario DEBE recibir una solicitud de revisión enviada previamente por un trabajador para poder comenzar la evaluación del galpón.
- **FR-003**: El envío de la solicitud únicamente DEBE iniciar el flujo de revisión y NO DEBE permitir que el trabajador evalúe, valide ni cambie el estado del galpón.
- **FR-004**: La solicitud de revisión DEBE estar asociada con la entidad Galpón correspondiente proporcionada por el módulo 1.
- **FR-005**: El sistema DEBE obtener del módulo 1 la entidad Galpón asociada con la solicitud y verificar su estado vigente.
- **FR-006**: El sistema DEBE permitir la validación únicamente cuando la entidad Galpón proporcionada por el módulo 1 se encuentre en estado `En producción`.
- **FR-007**: Cuando el veterinario complete correctamente la validación, el sistema DEBE cambiar y guardar como `aislamiento` el estado de la entidad Galpón proporcionada por el módulo 1.
- **FR-008**: Cuando el galpón no se encuentre en estado `En producción`, el sistema DEBE rechazar la validación, informar la razón y conservar su estado actual.
- **FR-009**: Antes de confirmar la validación, el sistema DEBE consultar nuevamente en el módulo 1 el estado vigente del galpón y rechazar la operación si no puede verificarlo.
- **FR-010**: La validación NO DEBE modificar ningún dato de la entidad Galpón distinto de su estado ni alterar otras entidades proporcionadas por el módulo 1.

### Key Entities

- **Solicitud de revisión**: Representa el antecedente necesario para que comience la evaluación del aislamiento.
  - **Relaciones**: corresponde a un único galpón proporcionado por el módulo 1.
  - **Origen y efecto**: es enviada por un trabajador y recibida por el veterinario; su envío o recepción no valida el aislamiento ni cambia el estado del galpón.
- **Galpón**: Representa el espacio cuya necesidad de aislamiento evalúa el veterinario y es proporcionado por el módulo 1.
  - **Atributos utilizados**: estado.
  - **Relaciones**: corresponde a la solicitud de revisión que da inicio a la evaluación.
  - **Transición de estado**: pasa de `En producción` a `aislamiento` únicamente cuando el veterinario completa correctamente la validación.

## Success Criteria

### Measurable Outcomes

- **SC-001**: El 100 % de las evaluaciones de aislamiento comienza después de que el veterinario recibe una solicitud enviada por un trabajador.
- **SC-002**: El 100 % de los intentos de evaluación o validación realizados por roles distintos al veterinario es rechazado sin modificar el estado del galpón.
- **SC-003**: Al menos el 90 % de los veterinarios puede completar la validación del aislamiento en menos de 15 segundos después de revisar la solicitud.
- **SC-004**: El 95 % de las validaciones correctas establece el estado `aislamiento` en un máximo de 1 segundo después de su confirmación.
- **SC-005**: El 100 % de las solicitudes enviadas o recibidas conserva el estado del galpón sin cambios hasta que el veterinario complete la validación.
