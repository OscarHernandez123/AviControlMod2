# Feature Specification: Validar aptitud de un galpón para sacrificio

**Created**: 2026-09-02  

## User Scenarios & Testing

### User Story 1 - Validar la aptitud de un galpón para sacrificio (Priority: P1)

Como administrador, quiero validar si un lote de aves y el galpón en el que está alojado actualmente, ambos proporcionados por el módulo 1, son aptos para sacrificio después de consultar la edad del lote, para conocer si cumplen las condiciones requeridas y establecer el estado del galpón como `En cosecha` cuando corresponda.

**Why this priority**: La validación permite confirmar de forma clara si el lote alcanzó la edad mínima y si el galpón se encuentra en el estado operativo requerido, además de dejar registrado que está apto antes de continuar con el proceso de sacrificio.

**Independent Test**: Se puede probar utilizando un galpón y su lote actualmente alojado proporcionados por el módulo 1, consultando la edad del lote y ejecutando posteriormente la validación. El sistema debe mostrar `Apto` y cambiar el estado de la entidad Galpón a `En cosecha` únicamente cuando la edad sea de 45 días o más y su estado vigente sea `En producción`; en cualquier otro caso debe mostrar `No apto`, indicar las razones correspondientes y conservar el estado actual del galpón.

**Acceptance Scenarios**:

1. **Scenario**: Galpón apto para sacrificio
   - **Given** que el módulo 1 proporciona un galpón en estado `En producción` con un lote alojado actualmente cuya edad consultada es de 45 días o más
   - **When** selecciona la opción de validar aptitud para sacrificio
   - **Then** el sistema muestra el resultado `Apto` y cambia el estado del galpón a `En cosecha`

2. **Scenario**: Lote que no cumple la edad mínima
   - **Given** que el administrador consultó correctamente la edad de un lote de 44 días y el galpón está en estado `En producción`
   - **When** selecciona la opción de validar aptitud para sacrificio
   - **Then** el sistema muestra `No apto`, indica que el lote no cumple la edad mínima de 45 días y conserva el estado actual del galpón

3. **Scenario**: Galpón que no está en producción
   - **Given** que el administrador consultó correctamente la edad de un lote de 45 días o más y el galpón no está en estado `En producción`
   - **When** selecciona la opción de validar aptitud para sacrificio
   - **Then** el sistema muestra `No apto`, indica que el galpón no se encuentra en producción y conserva su estado actual

4. **Scenario**: Incumplimiento simultáneo de las condiciones
   - **Given** que el administrador consultó correctamente la edad de un lote menor de 45 días y el galpón no está en estado `En producción`
   - **When** selecciona la opción de validar aptitud para sacrificio
   - **Then** el sistema muestra `No apto`, informa que el lote no cumple la edad mínima y que el galpón no se encuentra en producción, y conserva su estado actual

5. **Scenario**: Validación por un usuario no autorizado
   - **Given** que un usuario sin rol de administrador intenta validar la aptitud de un galpón
   - **When** solicita ejecutar la validación
   - **Then** el sistema rechaza la operación, no muestra el resultado y no modifica el estado del galpón

### Edge Cases

- **Edge case #1 - Cambio de información después de consultar la edad**

  - ¿Cómo maneja el sistema un galpón cuyo lote o estado cambia después de consultar la edad y antes de ejecutar la validación?  
    El sistema debe consultar nuevamente en el módulo 1 el lote alojado y el estado actual del galpón, y recalcular la edad con la fecha de ingreso vigente del lote. Si ya no existe un lote alojado actualmente, debe informar que la validación no puede realizarse.

- **Edge case #2 - Información incompleta o no disponible desde el módulo 1**

  - ¿Cómo maneja el sistema una validación cuando el módulo 1 no está disponible o devuelve datos incompletos del galpón o de su lote actualmente alojado?
    El sistema debe rechazar la validación, informar que no pudo verificar las condiciones y no modificar el estado del galpón ni el lote.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir la validación de aptitud para sacrificio exclusivamente a usuarios con rol de administrador.
- **FR-002**: La opción de validar aptitud para sacrificio DEBE estar disponible únicamente después de consultar correctamente la edad del lote alojado actualmente en un galpón.
- **FR-003**: El sistema DEBE obtener del módulo 1 la entidad Lote alojada actualmente y su fecha de ingreso, recalcular su edad y comprobar que sea igual o superior a 45 días.
- **FR-004**: El sistema DEBE obtener del módulo 1 el estado vigente de la entidad Galpón y comprobar que se encuentre en estado `En producción`.
- **FR-005**: El sistema DEBE mostrar `Apto` únicamente cuando se cumplan ambas condiciones; en caso contrario, DEBE mostrar `No apto` y todas las razones correspondientes.
- **FR-006**: Cuando el resultado sea `Apto`, el sistema DEBE cambiar y guardar como `En cosecha` el estado de la entidad Galpón proporcionada por el módulo 1.
- **FR-007**: Cuando el resultado sea `No apto`, el sistema DEBE conservar el estado actual del galpón y NO DEBE modificar el lote ni el proceso de sacrificio.
- **FR-008**: La validación DEBE utilizar la información vigente de las entidades Galpón y Lote proporcionadas por el módulo 1 y DEBE rechazar la operación si no puede verificarla completamente.

### Key Entities

- **Galpón**: Representa el espacio cuya condición operativa se evalúa y es proporcionado por el módulo 1.
  - **Atributos utilizados**: estado.
  - **Relaciones**: aloja el lote de aves evaluado.
- **Lote de aves**: Representa el grupo de aves cuya edad determina parte de la aptitud para sacrificio y es proporcionado por el módulo 1.
  - **Atributos utilizados**: fecha de ingreso.
  - **Datos derivados**: edad actual calculada por el sistema.
  - **Relaciones**: se encuentra alojado en el galpón evaluado.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los administradores puede completar la validación en menos de 15 segundos después de consultar la edad.
- **SC-002**: El 95 % de las validaciones muestra el resultado y sus razones y, cuando corresponde, establece el estado `En cosecha` en un máximo de 1 segundo.
- **SC-003**: Al menos el 95 % de los administradores interpreta correctamente el resultado en el primer intento durante pruebas de usabilidad.
- **SC-004**: Al menos el 85 % de los administradores califica la claridad del resultado y sus razones con 4 o más puntos sobre 5.
