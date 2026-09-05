# Feature Specification: Registrar medicación

**Created**: 2026-09-04  

## User Scenarios & Testing

### User Story 1 - Registrar una medicación (Priority: P1)

Como veterinario, quiero registrar una medicación indicando la enfermedad, el medicamento, la dosis, el número total de días y la descripción, para que pueda ser seleccionada posteriormente al diagnosticar un galpón.

**Why this priority**: El registro permite definir una medicación completa para una enfermedad utilizando un medicamento proveniente del inventario y garantiza que esta tarea sea realizada exclusivamente por el veterinario.

**Independent Test**: Se puede probar seleccionando una enfermedad disponible y un medicamento del inventario, e ingresando la dosis, el número total de días y la descripción. El sistema debe crear la medicación y dejarla disponible para su selección posterior en un diagnóstico de galpón, sin exigir un diagnóstico existente ni modificar el inventario.

**Acceptance Scenarios**:

1. **Scenario**: Registro correcto de una medicación
   - **Given** que la enfermedad está disponible y el medicamento seleccionado se encuentra en el inventario
   - **When** el veterinario registra la dosis, el número total de días y la descripción
   - **Then** el sistema crea la medicación con todos sus datos y la deja disponible para ser seleccionada posteriormente en un diagnóstico de galpón

2. **Scenario**: Intento de registro con datos incompletos
   - **Given** que el veterinario está registrando una medicación
   - **When** omite la enfermedad, el medicamento, la dosis, el número total de días o la descripción
   - **Then** el sistema rechaza el registro, identifica los datos que debe completar y no crea la medicación

3. **Scenario**: Número total de días no válido
   - **Given** que el veterinario está registrando una medicación
   - **When** ingresa un número total de días igual a cero, negativo o no entero
   - **Then** el sistema rechaza el registro, indica que la duración debe ser un número entero de días mayor que cero y no crea la medicación

4. **Scenario**: Enfermedad no registrada
   - **Given** que el veterinario está registrando una medicación
   - **When** selecciona una enfermedad que no existe o no está disponible
   - **Then** el sistema rechaza el registro, informa que la enfermedad no está disponible y no crea la medicación

5. **Scenario**: Medicamento no registrado en el inventario
   - **Given** que el veterinario está registrando una medicación
   - **When** selecciona un medicamento que no se encuentra en el inventario
   - **Then** el sistema rechaza el registro, informa que el medicamento no está disponible y no crea la medicación

6. **Scenario**: Registro por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta registrar una medicación
   - **When** solicita confirmar el registro
   - **Then** el sistema rechaza la operación y no crea la medicación

---

### User Story 2 - Editar una medicación (Priority: P2)

Como veterinario, quiero editar una medicación existente para corregir o actualizar su enfermedad, medicamento, dosis, número total de días o descripción, de manera que los nuevos datos se utilicen únicamente en diagnósticos futuros.

**Why this priority**: La edición permite mantener actualizada la medicación sin cambiar los tratamientos, enfermedades o fechas de reintegro que quedaron registrados en diagnósticos anteriores.

**Independent Test**: Se puede probar utilizando una medicación de 5 días que ya fue seleccionada en un diagnóstico y editar su duración a 7 días. El diagnóstico existente debe conservar la duración y la fecha de reintegro calculadas originalmente, mientras que un diagnóstico posterior debe utilizar la duración actualizada de 7 días.

**Acceptance Scenarios**:

1. **Scenario**: Edición correcta de una medicación
   - **Given** que existe una medicación registrada
   - **When** el veterinario modifica la enfermedad, el medicamento, la dosis, el número total de días o la descripción con datos válidos
   - **Then** el sistema guarda los cambios y deja la medicación actualizada disponible para diagnósticos futuros

2. **Scenario**: Edición de una medicación utilizada en diagnósticos existentes
   - **Given** que una medicación ya fue seleccionada en uno o varios diagnósticos
   - **When** el veterinario edita sus datos
   - **Then** el sistema aplica los cambios únicamente a diagnósticos futuros y conserva sin cambios los datos y cálculos de los diagnósticos existentes

3. **Scenario**: Edición por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta editar una medicación
   - **When** solicita confirmar los cambios
   - **Then** el sistema rechaza la operación y conserva la medicación sin cambios

### Edge Cases

- **Edge case #1 - Enfermedad o medicamento no disponible al confirmar**

  - ¿Cómo maneja el sistema una enfermedad o un medicamento que estaba disponible al ser seleccionado, pero deja de estarlo antes de confirmar la medicación?  
    El sistema debe comprobar nuevamente que la enfermedad esté disponible y que el medicamento pertenezca al inventario. Si no puede verificar alguno, debe rechazar el registro.

- **Edge case #2 - Datos de texto compuestos únicamente por espacios**

  - ¿Cómo maneja el sistema una dosis o descripción compuesta únicamente por espacios en blanco?  
    El sistema debe considerar el dato como vacío, indicar que debe corregirse y no crear la medicación.

- **Edge case #3 - Interrupción durante el registro**

  - ¿Cómo maneja el sistema una interrupción mientras guarda la medicación?  
    El sistema debe evitar registros parciales: la medicación debe guardarse con todos sus datos y relaciones o no debe crearse.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro de medicaciones exclusivamente a usuarios con rol de veterinario.
- **FR-002**: Cada medicación DEBE registrar enfermedad, medicamento, dosis, número total de días y descripción.
- **FR-003**: La enfermedad utilizada en la medicación DEBE existir y estar disponible.
- **FR-004**: El medicamento utilizado en la medicación DEBE provenir del inventario de medicamentos.
- **FR-005**: Cuando los datos sean válidos, el sistema DEBE crear la medicación y dejarla disponible para su selección en el registro posterior de un diagnóstico de galpón.
- **FR-006**: Registrar una medicación NO DEBE requerir un diagnóstico existente ni crear o modificar un diagnóstico de galpón.
- **FR-007**: Registrar una medicación NO DEBE representar la administración del medicamento ni descontar existencias del inventario.
- **FR-008**: Cuando el registro sea rechazado o interrumpido, el sistema NO DEBE crear una medicación incompleta ni guardar parcialmente sus relaciones.
- **FR-009**: El sistema DEBE permitir la edición de medicaciones exclusivamente a usuarios con rol de veterinario.
- **FR-010**: La edición de una medicación NO DEBE modificar diagnósticos existentes ni recalcular sus fechas de reintegro.

### Key Entities

- **Medicación**: Representa el tratamiento registrado por el veterinario y disponible para su selección posterior en un diagnóstico.
  - **Atributos**: enfermedad, medicamento, dosis, número total de días y descripción.
  - **Relaciones**: referencia una enfermedad y un medicamento proveniente del inventario; posteriormente puede ser referenciada por los diagnósticos de galpón que la seleccionen.
  - **Comportamiento ante ediciones**: los datos actualizados se utilizan en diagnósticos futuros, mientras que los diagnósticos existentes conservan los valores utilizados al momento de su registro.
- **Enfermedad**: Representa la enfermedad para la cual se registra la medicación.
  - **Relaciones**: puede tener una o varias medicaciones registradas; cada medicación referencia una enfermedad.
- **Medicamento**: Representa el medicamento utilizado por la medicación y proviene del inventario.
  - **Relaciones**: puede ser utilizado en varias medicaciones sin que el registro modifique sus existencias.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los veterinarios puede registrar una medicación válida en menos de 2 minutos.
- **SC-002**: El 95 % de las medicaciones válidas queda disponible para su selección en un máximo de 1 segundo después de la confirmación.
- **SC-003**: El 100 % de las medicaciones creadas conserva correctamente la referencia a su enfermedad y al medicamento del inventario.
- **SC-004**: El 100 % de los intentos realizados por roles distintos al veterinario es rechazado sin crear una medicación.
- **SC-005**: El 100 % de los registros de medicación conserva las existencias del inventario sin cambios.
- **SC-006**: El 100 % de las ediciones realizadas por el veterinario conserva sin cambios los diagnósticos y las fechas de reintegro existentes.
- **SC-007**: El 95 % de las ediciones válidas queda disponible para diagnósticos futuros en un máximo de 1 segundo después de su confirmación.
