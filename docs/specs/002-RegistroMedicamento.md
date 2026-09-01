# Feature Specification: Registro de medicamentos en bodega central

**Created**: 2026-08-28  

## User Scenarios & Testing 

### User Story 1 - Registrar una recepción de medicamento (Priority: P1)

Como administrador, quiero registrar cada recepción de medicamentos que ingresa a la bodega central para mantener un inventario exacto, valorizado y trazable por compra.

**Why this priority**: El registro de la recepción constituye la entrada oficial del medicamento al inventario. Sin este registro no es posible conocer las existencias disponibles en contenido neto, su costo ni su procedencia.

**Independent Test**: Se puede probar registrando una recepción de varios envases con una presentación existente en el catálogo y verificando que el sistema cree una entrada independiente, convierta la cantidad recibida a contenido neto, calcule el costo neto por unidad base y actualice las existencias de la bodega central.

**Acceptance Scenarios**:

1. **Scenario**: Registro correcto de una recepción
   - **Given** que un administrador autenticado dispone de los datos completos de una recepción y selecciona una presentación activa del catálogo
   - **When** registra el código de lote, nombre del medicamento, principio activo, marca, presentación, cantidad de unidades recibidas, costo neto por unidad física, impuesto, fecha actual como fecha de ingreso y una fecha de vencimiento válida
   - **Then** el sistema crea una recepción independiente, convierte la cantidad recibida a `gr`, `ml` o `unidad`, calcula el contenido neto total y sus costos, y actualiza el inventario de la bodega central

2. **Scenario**: Registro de una compra con un código de lote existente
   - **Given** que ya existe una recepción con el mismo código de lote
   - **When** el administrador registra un nuevo ingreso
   - **Then** el sistema crea una recepción separada y conserva de forma independiente sus cantidades, fechas, costos, impuestos y saldo

3. **Scenario**: Intento de registro con datos incompletos o inválidos
   - **Given** que el administrador está registrando una recepción
   - **When** omite datos obligatorios, introduce valores o fechas no válidos, o no selecciona una presentación activa del catálogo
   - **Then** el sistema rechaza el registro, identifica los datos que deben corregirse y no modifica el inventario

4. **Scenario**: Intento de registro por un usuario no autorizado
   - **Given** que un usuario sin rol de administrador intenta registrar una recepción de medicamento
   - **When** solicita confirmar el registro
   - **Then** el sistema rechaza la operación y no modifica el inventario

---

### User Story 2 - Corregir o anular una recepción de medicamento (Priority: P2)

Como administrador, quiero corregir o anular una recepción bajo reglas controladas para solucionar errores sin perder la trazabilidad ni alterar silenciosamente las existencias o los costos históricos.

**Why this priority**: Permite corregir errores de registro mientras protege la integridad del inventario y de cualquier movimiento que dependa de la recepción.

**Independent Test**: Se puede probar editando una recepción sin movimientos y solicitando después la modificación de otra recepción que sí los tenga, para verificar las restricciones, los recálculos y el historial de auditoría.

**Acceptance Scenarios**:

1. **Scenario**: Edición directa de una recepción sin movimientos
   - **Given** que una recepción no tiene movimientos de inventario asociados
   - **When** el administrador corrige sus datos y proporciona una justificación por escrito
   - **Then** el sistema actualiza la recepción, recalcula el contenido neto, los costos y las existencias afectadas, y registra la modificación en el historial de auditoría

2. **Scenario**: Intento de edición directa de una recepción con movimientos
   - **Given** que una recepción tiene al menos un movimiento de inventario asociado
   - **When** el administrador intenta editarla o eliminarla directamente
   - **Then** el sistema bloquea la operación y exige realizar un ajuste o una anulación formal

3. **Scenario**: Ajuste o anulación formal de una recepción con movimientos
   - **Given** que una recepción con movimientos asociados requiere una corrección
   - **When** el administrador registra un ajuste o una anulación formal con una justificación por escrito
   - **Then** el sistema conserva la recepción y sus movimientos históricos, aplica el cambio permitido y registra la auditoría completa

4. **Scenario**: Modificación sin justificación
   - **Given** que el administrador solicita editar, ajustar o anular una recepción
   - **When** no proporciona una justificación por escrito
   - **Then** el sistema rechaza la operación y conserva los datos sin cambios

5. **Scenario**: Intento de modificación por un usuario no autorizado
   - **Given** que un usuario sin rol de administrador intenta editar, ajustar o anular una recepción
   - **When** solicita confirmar la operación
   - **Then** el sistema rechaza la operación y conserva la recepción y el inventario sin cambios

### Edge Cases

- **Edge case #1 - Cantidad y contenido neto válidos cuyo producto excede la capacidad numérica**

  - ¿Cómo maneja el sistema un registro cuya cantidad recibida y contenido neto por envase son válidos individualmente, pero al multiplicarlos producen un contenido neto total superior al límite admitido?  
    El sistema debe detectar el desbordamiento antes de crear el registro, rechazar la operación e informar que el contenido total calculado excede el límite permitido. No debe guardar valores truncados o incorrectos ni modificar el inventario.

- **Edge case #2 - Costo por unidad base con resultado decimal periódico**

  - ¿Cómo maneja el sistema un medicamento cuyo costo neto por envase, dividido entre su contenido neto, produce un resultado decimal periódico?  
    El sistema debe calcular el costo por unidad base aplicando una precisión y una regla de redondeo uniformes. El valor utilizado en el registro y en el inventario debe ser el mismo y no debe truncarse arbitrariamente.

- **Edge case #3 - Presentación modificada o desactivada antes de confirmar el registro**

  - ¿Cómo maneja el sistema una presentación que estaba activa cuando el administrador la seleccionó, pero fue modificada o desactivada antes de confirmar el registro?  
    El sistema debe validar nuevamente la presentación al confirmar. Si ya no está activa o sus datos cambiaron, debe rechazar el registro, informar al administrador y no modificar el inventario.

## Requirements 

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro de recepciones de medicamentos exclusivamente a usuarios con rol de administrador.
- **FR-002**: Cada recepción DEBE registrar código de lote, nombre del medicamento, principio activo, marca, presentación del catálogo, cantidad recibida, costo neto por unidad física, impuesto, fecha de ingreso y fecha de vencimiento.
- **FR-003**: El sistema DEBE validar los datos obligatorios y rechazar el registro cuando estén incompletos, sean inválidos o los valores calculados excedan los límites admitidos.
- **FR-004**: El sistema DEBE calcular el contenido neto total, el costo neto por unidad base y el subtotal neto a partir de la presentación y la cantidad recibida; la unidad base DEBE ser `gr`, `ml` o `unidad`.
- **FR-004**: Cada ingreso DEBE crear una recepción independiente, incluso si comparte código de lote con otra, e incorporar su contenido neto al inventario de la bodega central.

### Key Entities 

- **Medicamento**: Representa el medicamento registrado en el inventario.
  - **Atributos posibles**: lote, nombre, principio activo, marca, cantidad, costos, impuesto, fechas, contenido neto y saldo.
  - **Relaciones**: utiliza una presentación, se almacena en la bodega central y se vincula con movimientos y registros de auditoría.
- **Presentación de medicamento**: Representa la forma comercial del medicamento.
  - **Atributos posibles**: nombre, tipo de empaque, contenido neto por envase, unidad base y estado.
  - **Relaciones**: puede ser utilizada por uno o varios medicamentos registrados.
- **Bodega central**: Representa el inventario principal.
  - **Atributos posibles**: nombre, ubicación y estado.
  - **Relaciones**: almacena los medicamentos registrados y consolida los movimientos que afectan sus existencias.
- **Movimiento de inventario**: Representa un cambio en las existencias.
  - **Atributos posibles**: tipo, cantidad, unidad, fecha, motivo y saldo resultante.
  - **Relaciones**: corresponde a un medicamento, afecta la bodega central y puede generar un registro de auditoría.
- **Registro de auditoría**: Representa el historial de cambios sobre un registro.
  - **Atributos posibles**: acción, fecha y hora, motivo, valores anteriores y valores nuevos.
  - **Relaciones**: identifica al usuario responsable y al medicamento o movimiento afectado.

## Success Criteria 

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los administradores puede completar un registro de medicamento válido en menos de 3 minutos.
- **SC-002**: El 95 % de los registros confirmados muestra los cálculos y la actualización del inventario en un máximo de 2 segundos.
- **SC-003**: Al menos el 90 % de los usuarios completa correctamente el registro de medicamento en el primer intento durante pruebas de usabilidad.
- **SC-004**: Al menos el 85 % de los administradores califica la experiencia de registro con 4 o más puntos sobre 5.

