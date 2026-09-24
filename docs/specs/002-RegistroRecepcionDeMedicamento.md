# Feature Specification: Registro de recepción de medicamento

**Created**: 2026-08-28  

## User Scenarios & Testing 

### User Story 1 - Registrar una recepción de medicamento (Priority: P1)

Como administrador, quiero registrar cada recepción de medicamento que ingresa a la bodega central para dejar formalizada y trazable cada compra, generar su movimiento de entrada y conservar sus precios históricos para que el módulo 3 pueda calcular el costo de los medicamentos aplicados a cada lote de aves.

**Why this priority**: El registro de la recepción constituye la entrada oficial del medicamento al inventario. Sin sus cantidades y precios históricos de compra no es posible mantener la trazabilidad de las compras ni valorar en el módulo 3 los medicamentos realmente aplicados a cada lote de aves.

**Independent Test**: Se puede probar registrando una recepción con medicamento, presentación, contenido por presentación y unidad de medida definidos de forma independiente, y verificando que el sistema cree una recepción, calcule el contenido neto total, el precio neto por unidad de medida, el subtotal neto y el precio total con impuesto, genere el movimiento de entrada correspondiente y conserve sus valores históricos.

**Acceptance Scenarios**:

1. **Scenario**: Registro correcto de una recepción
   - **Given** que un administrador autenticado selecciona un medicamento activo y dispone de los datos completos de una recepción
   - **When** registra el código de lote, presentación, cantidad, contenido por presentación, unidad de medida, precio neto por presentación, porcentaje de impuesto, fecha de ingreso y fecha de vencimiento
   - **Then** el sistema crea una recepción confirmada, calcula el contenido neto total, el precio neto por unidad de medida, el subtotal neto y el precio total, genera un movimiento confirmado de entrada y conserva sus valores históricos para el módulo 3

2. **Scenario**: Registro de una compra con un código de lote existente
   - **Given** que ya existe una recepción con el mismo código de lote
   - **When** el administrador registra un nuevo ingreso
   - **Then** el sistema crea una recepción separada y conserva de forma independiente sus cantidades, fechas, precios de compra e impuestos.

3. **Scenario**: Intento de registro con datos incompletos o inválidos
   - **Given** que el administrador está registrando una recepción
   - **When** omite datos obligatorios o introduce cantidades, contenidos, precios, impuestos o fechas no válidos
   - **Then** el sistema rechaza el registro, identifica los datos que deben corregirse y no modifica el inventario

4. **Scenario**: Intento de registro por un usuario no autorizado
   - **Given** que un usuario sin rol de administrador intenta registrar una recepción de medicamento
   - **When** solicita confirmar el registro
   - **Then** el sistema rechaza la operación y no modifica el inventario

---

### User Story 2 - Editar una recepción de medicamento (Priority: P2)

Como administrador, quiero editar una recepción que todavía no tenga movimientos de salida o consumo para corregir errores de registro sin perder la trazabilidad ni alterar aplicaciones o costos históricos.

**Why this priority**: Permite corregir errores antes de que la recepción sea utilizada, mientras protege la integridad del inventario y de cualquier movimiento que dependa de ella.

**Independent Test**: Se puede probar editando, mediante los mismos campos disponibles en el registro, una recepción sin movimientos de salida o consumo y solicitando después la edición de otra que sí los tenga, para verificar los recálculos, el bloqueo y el historial de auditoría.

**Acceptance Scenarios**:

1. **Scenario**: Edición de una recepción sin movimientos de salida o consumo
   - **Given** que una recepción no tiene movimientos de salida, despacho o consumo asociados
   - **When** el administrador corrige sus datos y confirma la edición
   - **Then** el sistema actualiza la recepción, recalcula el contenido neto total, el precio neto por unidad de medida, el subtotal neto y el precio total, actualiza las existencias afectadas y registra los valores anteriores y nuevos en el historial de auditoría

2. **Scenario**: Intento de edición de una recepción con movimientos de salida o consumo
   - **Given** que una recepción tiene al menos un movimiento de salida, despacho o consumo asociado
   - **When** el administrador intenta editarla
   - **Then** el sistema bloquea la operación, informa que la recepción ya fue utilizada y conserva sus datos sin cambios

3. **Scenario**: Intento de edición por un usuario no autorizado
   - **Given** que un usuario sin rol de administrador intenta editar una recepción
   - **When** solicita guardar los cambios
   - **Then** el sistema rechaza la operación y conserva la recepción y el inventario sin cambios

---

### User Story 3 - Consultar el historial de recepciones de medicamentos (Priority: P2)

Como administrador, quiero consultar y filtrar el historial de recepciones de medicamentos, abrir el detalle de cada recepción y acceder a su edición cuando esté permitida, para mantener la trazabilidad de las entradas realizadas en la bodega central.

**Why this priority**: El historial permite verificar las recepciones registradas y localizar una entrada específica sin convertir esta pantalla en una consulta de existencias, responsabilidad que corresponde al SPEC-023.

**Independent Test**: Se puede probar creando recepciones de diferentes medicamentos, presentaciones y fechas, ingresando al historial y verificando el indicador del mes, la búsqueda por lote o medicamento, los filtros por presentación y periodo, la tabla de resultados, el modal de detalle y la disponibilidad de la acción de edición.

**Acceptance Scenarios**:

1. **Scenario**: Visualización del historial de recepciones
   - **Given** que existen recepciones de medicamentos registradas
   - **When** el administrador abre la pantalla de recepciones de medicamentos
   - **Then** el sistema muestra para cada recepción el código de lote, medicamento, presentación, cantidad, precio neto por presentación y contenido por presentación, junto con las acciones `Detalles` y `Editar`

2. **Scenario**: Indicador de recepciones del mes
   - **Given** que existen recepciones confirmadas cuya fecha de ingreso pertenece al mes calendario actual
   - **When** el administrador abre el historial
   - **Then** el sistema muestra la cantidad de recepciones registradas durante el mes

3. **Scenario**: Búsqueda por lote o medicamento
   - **Given** que existe una recepción asociada con un código de lote o medicamento conocido
   - **When** el administrador ingresa el código de lote o el nombre del medicamento y aplica el filtro
   - **Then** el sistema muestra únicamente las recepciones coincidentes

4. **Scenario**: Filtrado por presentación y periodo
   - **Given** que existen recepciones con diferentes presentaciones y fechas de ingreso
   - **When** el administrador selecciona una presentación, un periodo y aplica los filtros
   - **Then** el sistema muestra las recepciones que cumplen simultáneamente ambos criterios

5. **Scenario**: Consulta del detalle de una recepción
   - **Given** que el administrador selecciona la acción `Detalles` de una recepción
   - **When** el sistema abre el modal de detalle
   - **Then** muestra en modo de solo lectura el medicamento, código de lote, presentación, cantidad, contenido por presentación, unidad de medida, precio neto por presentación, porcentaje de impuesto, fecha de ingreso, fecha de vencimiento, subtotal neto y precio total

6. **Scenario**: Acceso a la edición desde el historial
   - **Given** que el administrador consulta el historial
   - **When** selecciona `Editar` sobre una recepción
   - **Then** el sistema abre el modal de edición con los mismos campos del registro precargados si la recepción no tiene movimientos asociados; en caso contrario, bloquea la edición e informa la causa

7. **Scenario**: Historial sin resultados
   - **Given** que no existen recepciones o ninguna cumple los filtros aplicados
   - **When** el administrador consulta el historial
   - **Then** el sistema muestra un estado vacío informativo y no presenta datos inventados

### Edge Cases

- **Edge case #1 - Cantidad y contenido neto válidos cuyo producto excede la capacidad numérica**

  - ¿Cómo maneja el sistema un registro cuya cantidad recibida y contenido neto por envase son válidos individualmente, pero al multiplicarlos producen un contenido neto total superior al límite admitido?
    El sistema debe detectar el desbordamiento antes de crear el registro, rechazar la operación e informar que el contenido total calculado excede el límite permitido. No debe guardar valores truncados o incorrectos ni modificar el inventario.

- **Edge case #2 - Precio por unidad base con resultado decimal periódico**

  - ¿Cómo maneja el sistema un medicamento cuyo precio neto de compra por envase, dividido entre su contenido neto, produce un resultado decimal periódico?
    El sistema debe calcular el precio neto de compra por unidad base aplicando una precisión y una regla de redondeo uniformes. El valor utilizado en el registro y en el inventario debe ser el mismo y no debe truncarse arbitrariamente.

- **Edge case #3 - Fecha de vencimiento anterior a la fecha de ingreso**

  - ¿Cómo maneja el sistema una recepción cuya fecha de vencimiento es anterior o igual a la fecha de ingreso?
    El sistema debe rechazar el registro o la edición, indicar que la fecha de vencimiento debe ser posterior a la fecha de ingreso y no modificar el inventario.

- **Edge case #4 - Impuesto con resultado decimal**

  - ¿Cómo maneja el sistema un porcentaje de impuesto cuyo cálculo produce fracciones monetarias?
    El sistema debe calcular el valor del impuesto y el precio total aplicando una regla uniforme de precisión y redondeo. Los valores mostrados, almacenados y auditados deben coincidir.

## Requirements 

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro de recepciones de medicamentos exclusivamente a usuarios con rol de administrador.
- **FR-002**: Cada recepción DEBE referenciar un medicamento activo y registrar código de lote, presentación, cantidad, contenido por presentación, unidad de medida, precio neto por presentación, porcentaje de impuesto, fecha de ingreso y fecha de vencimiento.
- **FR-003**: El sistema DEBE validar los datos obligatorios y rechazar el registro cuando estén incompletos, sean inválidos o los valores calculados excedan los límites admitidos.
- **FR-004**: El sistema DEBE calcular el contenido neto total como `cantidad × contenido por presentación` y el precio neto por unidad de medida como `precio neto por presentación ÷ contenido por presentación`.
- **FR-005**: Cada ingreso DEBE crear una recepción independiente, incluso si comparte código de lote con otra, conservando sus propios datos, precios de compra y fechas.
- **FR-006**: Al confirmar el registro, el sistema DEBE conservar y dejar disponibles para el módulo 3 el identificador de la recepción, medicamento, código de lote, presentación, contenido por presentación, unidad de medida, precio neto por presentación, precio neto por unidad de medida, subtotal neto y porcentaje de impuesto.
- **FR-007**: La modificación posterior del precio de otra recepción o del precio vigente de un medicamento NO DEBE alterar el precio histórico asociado a una aplicación ya registrada.
- **FR-008**: La información suministrada DEBE permitir al módulo 3 calcular el costo de los medicamentos mediante la suma de `cantidad aplicada de cada recepción × precio neto histórico de compra por unidad base`. El subtotal de una recepción NO DEBE tratarse como costo de un lote de aves.
- **FR-009**: Los cambios posteriores realizados sobre el medicamento NO DEBEN modificar los datos históricos de una recepción confirmada. La recepción DEBE conservar como valores históricos la presentación, el contenido por presentación, la unidad de medida y los datos comerciales registrados.
- **FR-010**: El código de lote NO DEBE utilizarse como identificador único de la recepción. Cada recepción DEBE tener un identificador propio y puede compartir el código de lote con otras recepciones.
- **FR-011**: Al confirmar una recepción, el sistema DEBE generar un movimiento de entrada por su contenido neto total, vincularlo con la recepción y dejarlo disponible como fuente para la consulta de inventario definida en el SPEC-023.
- **FR-012**: El sistema DEBE permitir editar una recepción exclusivamente a usuarios con rol de administrador y únicamente cuando no tenga movimientos de salida, despacho o consumo asociados.
- **FR-013**: El modal de edición DEBE presentar los mismos campos que el modal de registro, con los valores actuales precargados, y permitir corregir medicamento, código de lote, presentación, cantidad, contenido por presentación, unidad de medida, precio neto por presentación, porcentaje de impuesto, fecha de ingreso y fecha de vencimiento.
- **FR-014**: Al guardar una edición válida, el sistema DEBE recalcular el contenido neto total, el precio neto por unidad de medida, el subtotal neto, el valor del impuesto y el precio total, y actualizar las existencias afectadas.
- **FR-015**: Toda edición confirmada DEBE registrar en la auditoría los valores anteriores, los valores nuevos, el usuario responsable y la fecha y hora.
- **FR-016**: Los valores calculados de la recepción NO DEBEN ser editables directamente por el usuario.
- **FR-017**: Presentación, contenido por presentación y unidad de medida DEBEN tratarse como campos obligatorios e independientes de la recepción; el contenido por presentación DEBE ser un valor numérico mayor que cero.
- **FR-018**: Presentación y unidad de medida DEBEN seleccionarse de sus respectivos conjuntos de valores permitidos; la enumeración concreta de esos valores queda fuera del alcance de esta especificación.
- **FR-019**: El sistema DEBE calcular el subtotal neto como `cantidad × precio neto por presentación`.
- **FR-020**: El sistema DEBE calcular el valor del impuesto como `subtotal neto × porcentaje de impuesto ÷ 100`.
- **FR-021**: El sistema DEBE calcular el precio total como `subtotal neto + valor del impuesto`.
- **FR-022**: La fecha de vencimiento DEBE ser posterior a la fecha de ingreso tanto en el registro como en la edición.
- **FR-023**: El sistema DEBE permitir al administrador consultar el historial de recepciones de medicamentos sin modificar sus datos ni las existencias.
- **FR-024**: La tabla del historial DEBE mostrar código de lote, medicamento, presentación, cantidad, precio neto por presentación, contenido por presentación y las acciones `Detalles` y `Editar`.
- **FR-025**: El historial DEBE mostrar la cantidad de recepciones confirmadas cuya fecha de ingreso pertenece al mes calendario actual.
- **FR-026**: El sistema DEBE permitir buscar por código de lote o nombre del medicamento y filtrar simultáneamente por presentación y periodo de fecha de ingreso; el periodo inicial DEBE ser los últimos 30 días.
- **FR-027**: El modal de detalle DEBE mostrar en modo de solo lectura el medicamento, código de lote, presentación, cantidad, contenido por presentación, unidad de medida, precio neto por presentación, porcentaje de impuesto, fecha de ingreso, fecha de vencimiento, subtotal neto y precio total.
- **FR-028**: La acción `Editar` del historial DEBE aplicar las mismas restricciones de edición definidas en FR-012 y NO DEBE permitir eludirlas.

### Key Entities 

- **Medicamento**: Representa un producto del catálogo de medicamentos, independientemente de sus compras y existencias.
  - **Atributos posibles**: nombre, principio activo, marca y descripción.
  - **Relaciones**: puede estar asociado con cero o varias recepciones de medicamento.
- **Recepción de medicamento**: Representa una compra o entrega concreta de un medicamento que ingresa a la bodega central.
  - **Atributos posibles**: identificador, código de lote, presentación, cantidad, contenido por presentación, unidad de medida, contenido neto total, precio neto por presentación, precio neto por unidad de medida, porcentaje de impuesto, valor del impuesto, subtotal neto, precio total, fecha de ingreso y fecha de vencimiento.
  - **Relaciones**: referencia un único medicamento y una única bodega central; origina el movimiento de entrada, puede tener movimientos posteriores de salida o ajuste y se vincula con registros de auditoría. Conserva los valores históricos requeridos por el módulo 3.
- **Bodega central**: Representa el inventario principal.
  - **Atributos posibles**: nombre, ubicación y estado.
  - **Relaciones**: recibe las recepciones de medicamentos y consolida los movimientos que afectan sus existencias.
- **Movimiento de inventario de medicamento**: Representa una entrada, salida, consumo o ajuste que afecta las existencias de una recepción.
  - **Atributos posibles**: tipo de movimiento, cantidad en unidad base, fecha y hora, motivo y estado.
  - **Relaciones**: pertenece a una recepción de medicamento, identifica al usuario responsable y permite obtener el saldo disponible sin modificar el registro histórico de la recepción.
- **Registro de auditoría**: Representa el historial de cambios sobre un registro.
  - **Atributos posibles**: acción, fecha y hora, valores anteriores y valores nuevos.
  - **Relaciones**: identifica al usuario responsable y a la recepción o movimiento afectado.
- **Historial de recepciones de medicamentos**: Representa la consulta de las entradas registradas en la bodega central.
  - **Datos mostrados**: indicador del mes, filtros, resultados resumidos y acceso al detalle y a la edición condicionada.
  - **Comportamiento**: utiliza las recepciones registradas como fuente y no altera sus movimientos ni existencias.

## Success Criteria 

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los administradores puede completar un registro de medicamento válido en menos de 3 minutos.
- **SC-002**: El 95 % de los registros confirmados muestra los cálculos, genera el movimiento de entrada y deja sus precios históricos de compra disponibles para el módulo 3 en un máximo de 2 segundos.
- **SC-003**: Al menos el 90 % de los usuarios completa correctamente el registro de medicamento en el primer intento durante pruebas de usabilidad.
- **SC-004**: Al menos el 85 % de los administradores califica la experiencia de registro con 4 o más puntos sobre 5.
- **SC-005**: El 100 % de los registros y ediciones válidos calcula correctamente el contenido neto total, el precio neto por unidad de medida, el subtotal neto, el valor del impuesto y el precio total.
- **SC-006**: El 95 % de las búsquedas y filtros del historial presenta los resultados en un máximo de 2 segundos.
- **SC-007**: El 100 % de los detalles presenta valores consistentes con la recepción seleccionada y no modifica información.
- **SC-008**: El 100 % de los intentos de editar desde el historial una recepción con movimientos asociados es bloqueado.

## Out of Scope

- La consulta consolidada de existencias de alimentos y medicamentos en la bodega central, cubierta por el SPEC-023 *Consultar inventario*.
- El registro de salidas, despachos, aplicaciones, consumos o ajustes de medicamentos.
- La creación o administración del catálogo de medicamentos y sus presentaciones.
- La definición de los valores concretos permitidos para presentación y unidad de medida.

