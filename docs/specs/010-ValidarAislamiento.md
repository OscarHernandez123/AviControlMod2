# Feature Specification: Validar aislamiento de un galpón

**Created**: 2026-09-04  

## User Scenarios & Testing

### User Story 1 - Validar el aislamiento de un galpón (Priority: P1)

Como veterinario, quiero validar el aislamiento de un galpón proporcionado por el módulo 1 que se encuentra en estado `En cosecha`, para cambiar su estado a `aislamiento`.

**Why this priority**: La validación permite identificar que un galpón que se encontraba en proceso de cosecha debe quedar aislado y garantiza que este cambio solo pueda ser autorizado por el veterinario.

**Independent Test**: Se puede probar utilizando un galpón proporcionado por el módulo 1 cuyo estado vigente sea `En cosecha`. Al ejecutar la validación con un veterinario autenticado, el sistema debe cambiar el estado de la entidad Galpón a `aislamiento`. Si el usuario no es veterinario o el galpón tiene un estado diferente, debe rechazar la operación y conservar su estado actual.

**Acceptance Scenarios**:

1. **Scenario**: Validación correcta del aislamiento
   - **Given** que el módulo 1 proporciona un galpón cuyo estado vigente es `En cosecha`
   - **When** el veterinario selecciona la opción de validar aislamiento
   - **Then** el sistema cambia y guarda el estado del galpón como `aislamiento`

2. **Scenario**: Intento de validación de un galpón que no está en cosecha
   - **Given** que el módulo 1 proporciona un galpón cuyo estado vigente es diferente de `En cosecha`
   - **When** el veterinario selecciona la opción de validar aislamiento
   - **Then** el sistema rechaza la operación, indica que el galpón debe estar en estado `En cosecha` y conserva su estado actual

3. **Scenario**: Validación por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta validar el aislamiento de un galpón en estado `En cosecha`
   - **When** solicita ejecutar la validación
   - **Then** el sistema rechaza la operación y no modifica el estado del galpón

4. **Scenario**: Información del galpón incompleta o no disponible
   - **Given** que el módulo 1 no proporciona el galpón o no permite conocer su estado vigente
   - **When** el veterinario intenta validar el aislamiento
   - **Then** el sistema rechaza la operación, informa que no pudo verificar el estado del galpón y no modifica sus datos

### Edge Cases

- **Edge case #1 - Cambio del estado después de seleccionar el galpón**

  - ¿Cómo maneja el sistema un galpón cuyo estado cambia después de ser seleccionado y antes de confirmar la validación?  
    El sistema debe consultar nuevamente en el módulo 1 el estado vigente del galpón. Solo debe completar la validación si el estado continúa siendo `En cosecha`; en caso contrario, debe rechazar la operación e informar la inconsistencia.

- **Edge case #2 - Repetición de una validación completada**

  - ¿Cómo maneja el sistema un nuevo intento de validación sobre un galpón que ya se encuentra en estado `aislamiento`?  
    El sistema debe rechazar el nuevo intento, indicar que el galpón ya se encuentra en aislamiento y conservar su estado sin cambios adicionales.

- **Edge case #3 - Información incompleta o no disponible desde el módulo 1**

  - ¿Cómo maneja el sistema una validación cuando el módulo 1 no está disponible o devuelve información incompleta del galpón?  
    El sistema debe rechazar la validación, identificar la información que no está disponible y no cambiar el estado del galpón utilizando datos inventados o desactualizados.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir la validación del aislamiento exclusivamente a usuarios con rol de veterinario.
- **FR-002**: El sistema DEBE obtener del módulo 1 la entidad Galpón seleccionada y su estado vigente.
- **FR-003**: El sistema DEBE permitir la validación únicamente cuando la entidad Galpón proporcionada por el módulo 1 se encuentre en estado `En cosecha`.
- **FR-004**: Cuando la validación sea correcta, el sistema DEBE cambiar y guardar como `aislamiento` el estado de la entidad Galpón proporcionada por el módulo 1.
- **FR-005**: Cuando el galpón no se encuentre en estado `En cosecha`, el sistema DEBE rechazar la validación, informar la razón y conservar su estado actual.
- **FR-006**: Antes de confirmar la validación, el sistema DEBE consultar nuevamente en el módulo 1 el estado vigente del galpón y rechazar la operación si no puede verificarlo.
- **FR-007**: La validación NO DEBE modificar ningún dato de la entidad Galpón distinto de su estado ni alterar otras entidades proporcionadas por el módulo 1.

### Key Entities

- **Galpón**: Representa el espacio cuyo aislamiento se valida y es proporcionado por el módulo 1.
  - **Atributos utilizados**: estado.
  - **Transición de estado**: pasa de `En cosecha` a `aislamiento` cuando el veterinario completa correctamente la validación.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los veterinarios puede completar la validación del aislamiento en menos de 15 segundos.
- **SC-002**: El 95 % de las validaciones correctas establece el estado `aislamiento` en un máximo de 1 segundo después de su confirmación.
- **SC-003**: El 100 % de los intentos realizados por roles distintos al veterinario conserva el estado del galpón sin cambios.
- **SC-004**: Al menos el 95 % de los veterinarios identifica correctamente el resultado de la validación en el primer intento durante pruebas de usabilidad.
