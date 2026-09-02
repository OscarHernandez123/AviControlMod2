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
   - **Then** el sistema crea una recepción independiente, calcula el contenido neto total y el costo neto por `gr`, `ml` o `unidad`, e incorpora el contenido neto al inventario de la bodega central

2. **Scenario**: Conversión de envases a contenido neto
   - **Given** que la presentación seleccionada corresponde a un frasco de 500 ml
   - **When** el administrador registra una recepción de 10 frascos
   - **Then** el sistema incorpora 5.000 ml al inventario operativo y calcula el costo neto por mililitro a partir del costo neto de cada frasco

3. **Scenario**: Conversión de una presentación cuya unidad base es `unidad`
   - **Given** que la presentación seleccionada corresponde a una caja de 20 tabletas y utiliza `unidad` como unidad base
   - **When** el administrador registra una recepción de 10 cajas
   - **Then** el sistema incorpora 200 unidades al inventario operativo y calcula el costo neto por tableta

4. **Scenario**: Registro de una compra con un código de lote existente
   - **Given** que ya existe una recepción con el mismo código de lote
   - **When** el administrador registra un nuevo ingreso
   - **Then** el sistema crea una recepción separada y conserva de forma independiente sus cantidades, fechas, costos, impuestos y saldo

5. **Scenario**: Intento de registro con datos incompletos o inválidos
   - **Given** que el administrador está registrando una recepción
   - **When** omite uno o más datos obligatorios, introduce valores no válidos o no selecciona una presentación del catálogo
   - **Then** el sistema rechaza el registro, identifica los datos que deben corregirse y no modifica el inventario

6. **Scenario**: Intento de registro con fechas no válidas
   - **Given** que el administrador está registrando una recepción
   - **When** utiliza una fecha de ingreso diferente de la fecha actual, una fecha de vencimiento anterior a la fecha de ingreso o un medicamento ya vencido
   - **Then** el sistema rechaza el registro e informa la causa sin modificar el inventario

7. **Scenario**: Intento de registro por un usuario no autorizado
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


## Requirements 

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro de recepciones de medicamentos exclusivamente a usuarios con rol de administrador.
- **FR-002**: Cada recepción DEBE registrar código de lote, nombre del medicamento, principio activo, marca, presentación, cantidad de unidades recibidas, costo neto por unidad física, impuesto, fecha de ingreso y fecha de vencimiento.
- **FR-003**: El nombre del medicamento y el principio activo DEBEN almacenarse como campos separados y obligatorios.
- **FR-004**: La recepción DEBE estar asociada a una presentación seleccionada de un catálogo estandarizado y NO DEBE admitir una presentación escrita libremente.
- **FR-005**: Cada presentación del catálogo DEBE definir nombre, tipo de empaque, contenido neto por envase y unidad base.
- **FR-006**: La unidad base de una presentación DEBE ser exactamente una de las siguientes: `gr`, `ml` o `unidad`.
- **FR-007**: Los medicamentos recibidos como tabletas, dosis o ampollas DEBEN contabilizarse mediante la unidad base `unidad`.
- **FR-008**: Toda concentración expresada en miligramos DEBE convertirse a su equivalente en gramos antes de ser utilizada como contenido neto de una presentación.
- **FR-009**: La cantidad de unidades recibidas DEBE representar el número entero de envases o empaques físicos adquiridos.
- **FR-010**: El sistema DEBE calcular el contenido neto total multiplicando la cantidad de unidades recibidas por el contenido neto por unidad física definido en la presentación.
- **FR-011**: El sistema DEBE calcular el costo neto por unidad base dividiendo el costo neto por unidad física entre su contenido neto.
- **FR-012**: El sistema DEBE calcular el subtotal neto de la recepción multiplicando la cantidad de unidades recibidas por el costo neto por unidad física.
- **FR-013**: El sistema DEBE manejar todos los valores monetarios en pesos colombianos (COP).
- **FR-014**: El costo por unidad física DEBE registrarse como valor neto, sin impuestos, y el porcentaje o valor del impuesto aplicable DEBE registrarse de forma independiente.
- **FR-015**: La fecha de ingreso DEBE coincidir con la fecha actual del sistema al momento de confirmar la recepción.
- **FR-016**: El sistema DEBE rechazar una recepción cuya fecha de vencimiento sea anterior a la fecha de ingreso o cuyo medicamento ya esté vencido al momento del registro.
- **FR-017**: Cada nuevo ingreso DEBE crear una recepción separada, incluso cuando ya exista otra recepción con el mismo código de lote.
- **FR-018**: Las recepciones que compartan un código de lote DEBEN conservar de manera independiente sus cantidades, fechas, costos, impuestos, vencimiento y saldo.
- **FR-019**: Una vez confirmado el registro, el sistema DEBE incorporar el contenido neto total de la recepción al inventario de la bodega central.
- **FR-020**: El sistema DEBE registrar el saldo inicial de la recepción en la unidad base definida por la presentación, con un valor igual al contenido neto total calculado y asociado con la recepción y el código de lote originales.
- **FR-021**: El sistema DEBE validar que la cantidad de unidades recibidas sea un número entero mayor que cero y que el contenido neto y el costo neto por unidad física sean mayores que cero.
- **FR-022**: El sistema solo DEBE permitir la edición directa de una recepción cuando esta no tenga movimientos de inventario asociados.
- **FR-023**: Si una recepción tiene movimientos asociados, el sistema DEBE bloquear su edición y eliminación directa y DEBE exigir un ajuste o una anulación formal.
- **FR-024**: Toda edición, ajuste o anulación DEBE ser realizada por un administrador y requerir una justificación por escrito.
- **FR-025**: El sistema DEBE registrar automáticamente en la auditoría el usuario responsable, fecha y hora, valores anteriores, valores nuevos y motivo del cambio.
- **FR-026**: Una modificación DEBE recalcular el contenido neto, los costos y las existencias afectadas sin alterar ni eliminar los movimientos históricos.
- **FR-027**: Los registros de recepciones, movimientos relacionados y auditoría NO DEBEN eliminarse silenciosamente como consecuencia de una edición, ajuste o anulación.
- **FR-028**: Ningún registro, edición, ajuste o anulación DEBE producir existencias negativas.
- **FR-029**: Toda validación de autorización y de reglas de negocio DEBE realizarse en el sistema, independientemente de las restricciones presentadas en la interfaz de usuario.

### Key Entities 

- **Recepción de medicamento**: Entrada independiente de inventario que contiene el código de lote, nombre, principio activo, marca, presentación, cantidad de unidades físicas recibidas, costos, impuesto, fechas, contenido neto, saldo y estado.
- **Presentación de medicamento**: Elemento de un catálogo estandarizado que define el nombre de la presentación, tipo de empaque, contenido neto por envase y unidad base utilizada para convertir los envases físicos a inventario operativo.
- **Bodega central**: Inventario principal al que se incorpora el contenido neto de cada recepción confirmada.
- **Movimiento de inventario**: Registro trazable asociado a una recepción cuya existencia restringe la edición directa del registro original.
- **Registro de auditoría**: Evidencia inmutable de una edición, ajuste o anulación, con usuario, fecha y hora, valores anteriores, valores nuevos y justificación.

## Success Criteria 

### Measurable Outcomes

- **SC-001**: El 100 % de las recepciones confirmadas contiene todos los campos obligatorios y utiliza una presentación válida del catálogo.
- **SC-002**: En el 100 % de las recepciones, el contenido neto total y el costo neto por unidad base coinciden con los valores calculados a partir de la cantidad de unidades físicas, la presentación y el costo neto por unidad física registrados.
- **SC-003**: El 100 % de las recepciones confirmadas utiliza exclusivamente `gr`, `ml` o `unidad` como unidad base.
- **SC-004**: El 100 % de las nuevas compras crea una recepción independiente, incluso cuando el código de lote ya existe.
- **SC-005**: El 100 % de los intentos de registro o modificación realizados por usuarios sin rol de administrador se rechaza sin modificar la recepción ni el inventario.
- **SC-006**: El 100 % de los intentos de registrar una fecha de ingreso diferente de la fecha actual, una fecha de vencimiento anterior al ingreso o un medicamento ya vencido se rechaza sin modificar el inventario.
- **SC-007**: El 100 % de las recepciones crea un saldo inicial igual al contenido neto total, expresado en la unidad base y asociado con la recepción y el código de lote originales.
- **SC-008**: El 100 % de las ediciones, ajustes y anulaciones exige justificación y registra usuario, fecha y hora, valores anteriores, valores nuevos y motivo.
- **SC-009**: Ninguna operación confirmada produce existencias negativas ni elimina recepciones, movimientos relacionados o auditorías históricas.

## Measurable Outcomes

- El despacho de medicamentos.
- La aplicación, dosificación o consumo de medicamentos.
- La devolución de medicamentos.
- La priorización FEFO o FIFO para seleccionar existencias.
- El tratamiento, cuarentena, baja o disposición física de medicamentos vencidos o deteriorados.
- La creación, edición o administración del catálogo de presentaciones; esta especificación considera el catálogo como un dato previamente disponible.
- La creación o administración de medicamentos maestros, bodegas, usuarios y roles.
