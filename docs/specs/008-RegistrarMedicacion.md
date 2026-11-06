# Feature Specification: 008 - Registrar medicación

**Created**: 2026-09-04

**Last Updated**: 2026-10-27

## User Scenarios & Testing

### User Story 1 - Registrar una medicación (Priority: P1)

Como veterinario, quiero registrar una medicación indicando la enfermedad tratable, el medicamento, la dosis, el número total de días y la descripción de administración, para disponer de tratamientos estandarizados que puedan ser seleccionados posteriormente al diagnosticar un galpón.

**Why this priority**: El registro permite definir una pauta terapéutica completa para una enfermedad utilizando un medicamento proveniente del inventario central y garantiza que esta tarea sea realizada exclusivamente por el veterinario.

**Independent Test**: Se puede probar seleccionando una enfermedad registrada que no requiera sacrificio sanitario y un medicamento disponible en el inventario, e ingresando la dosis, el número total de días (entero mayor que cero) y la descripción. El sistema debe crear la medicación y dejarla disponible para su selección posterior en un diagnóstico de galpón, sin exigir un diagnóstico existente ni descontar existencias físicas del inventario.

**Acceptance Scenarios**:

1. **Scenario**: Registro correcto de una medicación
   - **Given** que la enfermedad está registrada como tratable (`requiere sacrificio sanitario` en falso) y el medicamento seleccionado se encuentra en el inventario
   - **When** el veterinario registra la dosis, el número total de días y la descripción
   - **Then** el sistema crea la medicación con todos sus datos y la deja disponible para ser seleccionada posteriormente en un diagnóstico de galpón

2. **Scenario**: Intento de registro con datos incompletos o compuestos por espacios
   - **Given** que el veterinario está registrando una medicación
   - **When** omite la enfermedad, el medicamento, la dosis, el número total de días o la descripción, o ingresa textos compuestos únicamente por espacios en blanco
   - **Then** el sistema rechaza el registro, identifica los datos que debe completar y no crea la medicación

3. **Scenario**: Número total de días no válido
   - **Given** que el veterinario está registrando una medicación
   - **When** ingresa un número total de días igual a cero, negativo o no entero
   - **Then** el sistema rechaza el registro, indica que la duración debe ser un número entero de días mayor que cero y no crea la medicación

4. **Scenario**: Enfermedad no registrada o que requiere sacrificio sanitario
   - **Given** que el veterinario está registrando una medicación
   - **When** selecciona una enfermedad que no existe o cuyo indicador `requiere sacrificio sanitario` es verdadero
   - **Then** el sistema rechaza el registro, informa que la enfermedad no admite medicación y no crea la medicación

5. **Scenario**: Medicamento no registrado en el inventario
   - **Given** que el veterinario está registrando una medicación
   - **When** selecciona un medicamento que no se encuentra registrado en el inventario
   - **Then** el sistema rechaza el registro, informa que el medicamento no está disponible y no crea la medicación

6. **Scenario**: Registro por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta registrar una medicación
   - **When** solicita confirmar el registro
   - **Then** el sistema rechaza la operación con error de autorización y no crea la medicación

---

### User Story 2 - Editar una medicación (Priority: P2)

Como veterinario, quiero editar una medicación existente para corregir o actualizar su enfermedad, medicamento, dosis, número total de días o descripción, de manera que los nuevos datos se utilicen únicamente en diagnósticos futuros.

**Why this priority**: La edición permite mantener actualizado el catálogo terapéutico sin alterar los tratamientos, medicamentos o fechas de reintegro que quedaron registrados históricamente en diagnósticos anteriores.

**Independent Test**: Se puede probar utilizando una medicación de 5 días que ya fue seleccionada en un diagnóstico previo y editar su duración a 7 días. El diagnóstico existente debe conservar la duración de 5 días y la fecha de reintegro calculada originalmente, mientras que un diagnóstico posterior debe utilizar la duración actualizada de 7 días.

**Acceptance Scenarios**:

1. **Scenario**: Edición correcta de una medicación
   - **Given** que existe una medicación registrada
   - **When** el veterinario modifica la enfermedad, el medicamento, la dosis, el número total de días o la descripción con datos válidos
   - **Then** el sistema guarda los cambios, actualiza la versión del registro y deja la medicación actualizada disponible para diagnósticos futuros

2. **Scenario**: Edición de una medicación utilizada en diagnósticos existentes
   - **Given** que una medicación ya fue seleccionada en uno o varios diagnósticos previos
   - **When** el veterinario edita sus datos
   - **Then** el sistema aplica los cambios únicamente a diagnósticos futuros y conserva sin cambios los valores históricos y las fechas de reintegro de los diagnósticos existentes

3. **Scenario**: Edición por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta editar una medicación
   - **When** solicita confirmar los cambios
   - **Then** el sistema rechaza la operación y conserva la medicación sin cambios

---

### Edge Cases

- **Edge case #1 - Enfermedad o medicamento no disponible al confirmar**: ¿Cómo maneja el sistema una enfermedad o un medicamento que estaba disponible al ser seleccionado, pero deja de estarlo antes de confirmar la medicación? El sistema debe comprobar nuevamente al confirmar que la enfermedad exista y sea tratable y que el medicamento pertenezca al inventario. Si no puede verificar alguno, debe rechazar el registro sin guardar datos parciales.
- **Edge case #2 - Datos de texto compuestos únicamente por espacios**: ¿Cómo maneja el sistema una dosis o descripción compuesta únicamente por espacios en blanco? El sistema debe normalizar el texto eliminando espacios extremos, considerar el dato como vacío, indicar que debe corregirse y no crear ni actualizar la medicación.
- **Edge case #3 - Interrupción o colisión concurrente durante el guardado**: ¿Cómo maneja el sistema una interrupción mientras guarda la medicación o dos ediciones simultáneas sobre el mismo registro? El sistema debe operar dentro de una transacción atómica para evitar registros parciales y aplicar control de versión optimista para rechazar ediciones basadas en una versión desactualizada.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro, la consulta de catálogo y la edición de medicaciones exclusivamente a usuarios autenticados con rol de veterinario.
- **FR-002**: Cada medicación DEBE registrar obligatoriamente enfermedad, medicamento, dosis, número total de días (entero mayor que cero) y descripción de administración.
- **FR-003**: La enfermedad asociada a la medicación DEBE existir en el catálogo sanitario y tener el indicador `requiere sacrificio sanitario` en falso.
- **FR-004**: El medicamento utilizado en la medicación DEBE existir en el catálogo/inventario de medicamentos.
- **FR-005**: Cuando los datos sean válidos, el sistema DEBE crear la medicación y dejarla disponible para su selección en el registro posterior de un diagnóstico de galpón.
- **FR-006**: El sistema DEBE exponer un mecanismo de consulta (endpoint GET) que permita listar las medicaciones registradas (filtrables por `enfermedadId`) para poblar los selectores del módulo de diagnóstico clínico (Spec 011).
- **FR-007**: Registrar una medicación NO DEBE requerir un diagnóstico existente ni crear o modificar un diagnóstico de galpón.
- **FR-008**: Registrar o editar una medicación NO DEBE representar la administración física del medicamento ni descontar existencias del inventario (el descuento real corresponde al Spec 013).
- **FR-009**: Cuando el registro sea rechazado o interrumpido, el sistema NO DEBE crear una medicación incompleta ni guardar parcialmente sus relaciones.
- **FR-010**: La edición de una medicación NO DEBE modificar diagnósticos existentes ni recalcular sus fechas de reintegro; los diagnósticos conservan los valores históricos vigentes al momento de su confirmación.
- **FR-011**: Las medicaciones registradas NO DEBEN eliminarse físicamente para preservar la integridad referencial e histórica de los diagnósticos.

### Non-Functional Requirements

| ID          | Categoría    | Requerimiento                                                                           | Métrica                             |
| :---------- | :----------- | :-------------------------------------------------------------------------------------- | :---------------------------------- |
| **NFR-001** | Rendimiento  | Latencia de validación, registro y persistencia                                         | < 1 segundo (p95) bajo carga normal |
| **NFR-002** | Seguridad    | Acceso restringido exclusivamente al rol VETERINARIO                                    | HTTP 403 Forbidden a otros roles    |
| **NFR-003** | Integridad   | Escritura atómica de la entidad y sus relaciones                                        | 100% de consistencia local          |
| **NFR-004** | Persistencia | Los registros históricos de diagnósticos conservan los datos de la medicación inmutable | 0% de alteración retroactiva        |

### Key Entities

- **Medicación**: Representa el protocolo terapéutico registrado por el veterinario y disponible para su selección posterior al diagnosticar un galpón con una enfermedad tratable.
  - **Atributos**: identificador UUID, enfermedad (`enfermedadId`), medicamento (`medicamentoId`), dosis, número total de días (`diasTotales`) y descripción.
  - **Relaciones**: referencia una enfermedad del catálogo (Spec 009) y un medicamento proveniente del inventario (Spec 002); posteriormente puede ser seleccionada en los diagnósticos de galpón (Spec 011).
  - **Comportamiento ante ediciones**: los datos actualizados se utilizan únicamente en diagnósticos futuros, mientras que los diagnósticos existentes conservan una instantánea (_snapshot_) de los valores utilizados al momento de su registro.
- **Enfermedad**: Representa la patología tratable para la cual se registra la medicación. Puede tener una o varias medicaciones registradas.
- **Medicamento**: Representa el insumo farmacéutico utilizado por la medicación y proveniente del inventario. Puede ser utilizado en varias medicaciones sin que el registro modifique sus existencias físicas.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los veterinarios puede registrar una medicación válida en menos de 2 minutos.
- **SC-002**: El 95 % de las medicaciones válidas queda disponible para su selección en un máximo de 1 segundo después de la confirmación.
- **SC-003**: El 100 % de las medicaciones creadas conserva correctamente la referencia a su enfermedad y al medicamento del inventario.
- **SC-004**: El 100 % de los intentos realizados por roles distintos al veterinario es rechazado sin crear ni modificar una medicación.
- **SC-005**: El 100 % de los registros y ediciones de medicación conserva las existencias del inventario sin cambios.
- **SC-006**: El 100 % de las ediciones realizadas por el veterinario conserva sin cambios los diagnósticos y las fechas de reintegro existentes.
- **SC-007**: El 95 % de las ediciones válidas queda disponible para diagnósticos futuros en un máximo de 1 segundo después de su confirmación.
