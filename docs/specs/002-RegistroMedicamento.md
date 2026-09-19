# Feature Specification: Registro de medicamentos en bodega central

**Created**: 2026-08-28  

## User Scenarios & Testing 

### User Story 1 - Registrar una recepción de medicamento (Priority: P1)

Como administrador, quiero registrar cada recepción de medicamentos que ingresa a la bodega central para mantener un inventario exacto, valorizado y trazable por compra, y conservar sus precios históricos de compra para que el módulo 3 pueda calcular el costo de los medicamentos aplicados a cada lote de aves.

**Why this priority**: El registro de la recepción constituye la entrada oficial del medicamento al inventario. Sin sus cantidades y precios históricos de compra no es posible conocer las existencias ni valorar en el módulo 3 los medicamentos realmente aplicados a cada lote de aves.

**Independent Test**: Se puede probar registrando una recepción de varios envases con una presentación existente en el catálogo y verificando que el sistema cree una entrada independiente, convierta la cantidad recibida a contenido neto, calcule el precio neto de compra por unidad base, actualice las existencias de la bodega central y conserve ese precio asociado a la recepción para su posterior uso por el módulo 3.

**Acceptance Scenarios**:

1. **Scenario**: Registro correcto de una recepción
   - **Given** que un administrador autenticado dispone de los datos completos de una recepción y selecciona un medicamento y una presentación activos del catálogo
   - **When** registra el código de lote, cantidad de unidades recibidas, precio neto de compra por unidad física, impuesto, fecha actual como fecha de ingreso y una fecha de vencimiento válida
   - **Then** el sistema crea una recepción independiente, convierte la cantidad recibida a `gr`, `ml` o `unidad`, calcula el contenido neto total, el precio neto de compra por unidad base y el subtotal neto de la recepción, actualiza el inventario y conserva los precios históricos asociados a la recepción para el módulo 3

2. **Scenario**: Registro de una compra con un código de lote existente
   - **Given** que ya existe una recepción con el mismo código de lote
   - **When** el administrador registra un nuevo ingreso
   - **Then** el sistema crea una recepción separada y conserva de forma independiente sus cantidades, fechas, precios de compra e impuestos.

3. **Scenario**: Intento de registro con datos incompletos o inválidos
   - **Given** que el administrador está registrando una recepción
   - **When** omite datos obligatorios, introduce valores o fechas no válidos, o no selecciona una presentación activa del catálogo
   - **Then** el sistema rechaza el registro, identifica los datos que deben corregirse y no modifica el inventario

4. **Scenario**: Intento de registro por un usuario no autorizado
   - **Given** que un usuario sin rol de administrador intenta registrar una recepción de medicamento
   - **When** solicita confirmar el registro
   - **Then** el sistema rechaza la operación y no modifica el inventario

---

### User Story 2 - Editar una recepción de medicamento (Priority: P2)

Como administrador, quiero editar una recepción que todavía no tenga movimientos de salida o consumo para corregir errores de registro sin perder la trazabilidad ni alterar aplicaciones o costos históricos.

**Why this priority**: Permite corregir errores antes de que la recepción sea utilizada, mientras protege la integridad del inventario y de cualquier movimiento que dependa de ella.

**Independent Test**: Se puede probar editando una recepción sin movimientos de salida o consumo y solicitando después la edición de otra que sí los tenga, para verificar los recálculos, el bloqueo y el historial de auditoría.

**Acceptance Scenarios**:

1. **Scenario**: Edición de una recepción sin movimientos de salida o consumo
   - **Given** que una recepción no tiene movimientos de salida, despacho o consumo asociados
   - **When** el administrador corrige sus datos y proporciona una observación que justifica el cambio
   - **Then** el sistema actualiza la recepción, recalcula el contenido neto, los precios unitarios, el subtotal neto y las existencias afectadas, y registra la modificación en el historial de auditoría

2. **Scenario**: Intento de edición de una recepción con movimientos de salida o consumo
   - **Given** que una recepción tiene al menos un movimiento de salida, despacho o consumo asociado
   - **When** el administrador intenta editarla
   - **Then** el sistema bloquea la operación, informa que la recepción ya fue utilizada y conserva sus datos sin cambios

3. **Scenario**: Edición sin observaciones
   - **Given** que el administrador solicita editar una recepción sin movimientos de salida o consumo
   - **When** no proporciona una observación que justifique el cambio
   - **Then** el sistema rechaza la operación y conserva los datos sin cambios

4. **Scenario**: Intento de edición por un usuario no autorizado
   - **Given** que un usuario sin rol de administrador intenta editar una recepción
   - **When** solicita guardar los cambios
   - **Then** el sistema rechaza la operación y conserva la recepción y el inventario sin cambios

### Edge Cases

- **Edge case #1 - Cantidad y contenido neto válidos cuyo producto excede la capacidad numérica**

  - ¿Cómo maneja el sistema un registro cuya cantidad recibida y contenido neto por envase son válidos individualmente, pero al multiplicarlos producen un contenido neto total superior al límite admitido?
    El sistema debe detectar el desbordamiento antes de crear el registro, rechazar la operación e informar que el contenido total calculado excede el límite permitido. No debe guardar valores truncados o incorrectos ni modificar el inventario.

- **Edge case #2 - Precio por unidad base con resultado decimal periódico**

  - ¿Cómo maneja el sistema un medicamento cuyo precio neto de compra por envase, dividido entre su contenido neto, produce un resultado decimal periódico?
    El sistema debe calcular el precio neto de compra por unidad base aplicando una precisión y una regla de redondeo uniformes. El valor utilizado en el registro y en el inventario debe ser el mismo y no debe truncarse arbitrariamente.

- **Edge case #3 - Presentación modificada o desactivada antes de confirmar el registro**

  - ¿Cómo maneja el sistema una presentación que estaba activa cuando el administrador la seleccionó, pero fue modificada o desactivada antes de confirmar el registro?
    El sistema debe validar nuevamente la presentación al confirmar. Si ya no está activa o sus datos cambiaron, debe rechazar el registro, informar al administrador y no modificar el inventario.

## Requirements 

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro de recepciones de medicamentos exclusivamente a usuarios con rol de administrador.
- **FR-002**: Cada recepción DEBE referenciar un medicamento y una presentación compatibles y activos del catálogo y registrar código de lote, cantidad recibida, precio neto de compra por unidad física, impuesto, fecha de ingreso y fecha de vencimiento.
- **FR-003**: El sistema DEBE validar los datos obligatorios y rechazar el registro cuando estén incompletos, sean inválidos o los valores calculados excedan los límites admitidos.
- **FR-004**: El sistema DEBE calcular el contenido neto total, el precio neto de compra por unidad base y el subtotal neto de la recepción a partir de la presentación, la cantidad recibida y el precio neto de compra por unidad física; la unidad base DEBE ser `gr`, `ml` o `unidad`.
- **FR-005**: Cada ingreso DEBE crear una recepción independiente, incluso si comparte código de lote con otra, e incorporar su contenido neto al inventario de la bodega central.
- **FR-006**: Al confirmar el registro, el sistema DEBE conservar y dejar disponibles para el módulo 3 el identificador de la recepción, el medicamento, el código de lote del medicamento, la presentación, el precio neto de compra por unidad física, el precio neto de compra por unidad base, el subtotal neto de la recepción, el impuesto y la moneda.
- **FR-007**: La modificación posterior del precio de otra recepción o del precio vigente de un medicamento NO DEBE alterar el precio histórico asociado a una aplicación ya registrada.
- **FR-008**: La información suministrada DEBE permitir al módulo 3 calcular el costo de los medicamentos mediante la suma de `cantidad aplicada de cada recepción × precio neto histórico de compra por unidad base`. El subtotal de una recepción NO DEBE tratarse como costo de un lote de aves.
- **FR-009**: Los cambios posteriores realizados sobre el medicamento o su presentación en el catálogo NO DEBEN modificar los datos históricos de una recepción confirmada. La recepción DEBE conservar los datos comerciales y de presentación necesarios como valores históricos de referencia.
- **FR-010**: El código de lote NO DEBE utilizarse como identificador único de la recepción. Cada recepción DEBE tener un identificador propio y puede compartir el código de lote con otras recepciones.
- **FR-011**: El sistema DEBE calcular automáticamente la existencia disponible de cada medicamento consolidando los movimientos confirmados asociados con sus recepciones y dejar este resultado disponible para las consultas de inventario.
- **FR-012**: El sistema DEBE permitir editar una recepción exclusivamente a usuarios con rol de administrador y únicamente cuando no tenga movimientos de salida, despacho o consumo asociados.
- **FR-013**: Durante la edición, el administrador DEBE poder corregir el medicamento, la presentación, el código de lote, la cantidad recibida, el precio neto de compra por unidad física, el impuesto, la fecha de ingreso y la fecha de vencimiento.
- **FR-014**: Para guardar una edición, el administrador DEBE proporcionar una observación que justifique el cambio.
- **FR-015**: Al guardar una edición válida, el sistema DEBE recalcular el contenido neto total, el precio neto de compra por unidad base y el subtotal neto, actualizar las existencias afectadas y registrar en la auditoría los valores anteriores, los valores nuevos, la observación, el usuario y la fecha y hora.
- **FR-016**: Los valores calculados de la recepción NO DEBEN ser editables directamente por el usuario.

### Key Entities 

- **Medicamento**: Representa un producto del catálogo de medicamentos, independientemente de sus compras y existencias.
  - **Atributos posibles**: nombre, principio activo, marca y descripción.
  - **Relaciones**: puede disponer de una o varias presentaciones y estar asociado con cero o varias recepciones de medicamento.
- **Presentación de medicamento**: Representa la forma comercial del medicamento.
  - **Atributos posibles**: nombre, tipo de empaque, contenido neto por envase y unidad base.
  - **Relaciones**: puede estar asociada con uno o varios medicamentos y ser utilizada por cero o varias recepciones de medicamento.
- **Recepción de medicamento**: Representa una compra o entrega concreta de un medicamento que ingresa a la bodega central.
  - **Atributos posibles**: identificador, código de lote, cantidad de unidades físicas recibidas, contenido neto total, precio neto de compra por unidad física, precio neto de compra por unidad base, subtotal neto, fecha de ingreso y fecha de vencimiento.
  - **Relaciones**: referencia un único medicamento, una única presentación y una única bodega central; origina el movimiento de entrada, puede tener movimientos posteriores de salida o ajuste y se vincula con registros de auditoría. Conserva los precios y datos comerciales históricos requeridos por el módulo 3.
- **Bodega central**: Representa el inventario principal.
  - **Atributos posibles**: nombre, ubicación y estado.
  - **Relaciones**: recibe las recepciones de medicamentos y consolida los movimientos que afectan sus existencias.
- **Movimiento de inventario de medicamento**: Representa una entrada, salida, consumo o ajuste que afecta las existencias de una recepción.
  - **Atributos posibles**: tipo de movimiento, cantidad en unidad base, fecha y hora, motivo y estado.
  - **Relaciones**: pertenece a una recepción de medicamento, identifica al usuario responsable y permite obtener el saldo disponible sin modificar el registro histórico de la recepción.
- **Registro de auditoría**: Representa el historial de cambios sobre un registro.
  - **Atributos posibles**: acción, fecha y hora, motivo, valores anteriores y valores nuevos.
  - **Relaciones**: identifica al usuario responsable y a la recepción o movimiento afectado.

## Success Criteria 

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los administradores puede completar un registro de medicamento válido en menos de 3 minutos.
- **SC-002**: El 95 % de los registros confirmados muestra los cálculos, actualiza el inventario y deja sus precios históricos de compra disponibles para el módulo 3 en un máximo de 2 segundos.
- **SC-003**: Al menos el 90 % de los usuarios completa correctamente el registro de medicamento en el primer intento durante pruebas de usabilidad.
- **SC-004**: Al menos el 85 % de los administradores califica la experiencia de registro con 4 o más puntos sobre 5.

