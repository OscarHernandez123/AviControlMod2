# Feature Specification: Registro de alimentos en bodega central

**Created**: 2026-08-28  

## User Scenarios & Testing 

### User Story 1 - Registrar una recepción de alimento (Priority: P1)

Como administrador, quiero registrar cada recepción de alimento que ingresa a la bodega central para mantener un inventario exacto, valorizado y trazable por entrega.

**Why this priority**: El registro de las recepciones constituye la entrada oficial del inventario. Sin esta información no es posible conocer las existencias, calcular sus costos ni aplicar correctamente las reglas posteriores de asignación.

**Independent Test**: Se puede probar registrando una recepción de varios bultos y verificando que el sistema cree una entrada independiente, calcule los kilogramos nominales y el costo neto por kilogramo, y actualice las existencias de la bodega central.

**Acceptance Scenarios**:

1. **Scenario**: Registro correcto de una recepción
   - **Given** que un administrador autenticado dispone de los datos completos de una entrega de alimento
   - **When** registra el código de lote, tipo de alimento, etapa de crianza, cantidad de bultos, peso nominal por bulto, marca, precio neto por bulto, impuesto, fecha de ingreso y fecha de vencimiento
   - **Then** el sistema crea una recepción en la bodega central, calcula sus kilogramos nominales totales y el precio neto por kilogramo, y actualiza el inventario

2. **Scenario**: Registro de una entrega con un código de lote existente
   - **Given** que ya existe una recepción con el mismo código de lote
   - **When** el administrador registra una nueva entrega
   - **Then** el sistema crea una recepción separada sin sumar sus cantidades al registro anterior y conserva de forma independiente las fechas, costos y vencimiento de cada entrega

3. **Scenario**: Intento de registro con datos obligatorios incompletos
   - **Given** que el administrador está registrando una recepción
   - **When** omite uno o más datos obligatorios o ingresa valores inválidos
   - **Then** el sistema rechaza el registro, informa los datos que deben corregirse y no modifica el inventario

4. **Scenario**: Intento de registro por un usuario no autorizado
   - **Given** que un usuario sin rol de administrador intenta registrar una recepción
   - **When** solicita confirmar el registro
   - **Then** el sistema rechaza la operación y no modifica el inventario

---

### User Story 2 - Corregir o anular una recepción (Priority: P2)

Como administrador, quiero corregir o anular una recepción bajo reglas controladas para solucionar errores sin perder la trazabilidad ni alterar silenciosamente los balances y costos históricos.

**Why this priority**: Permite corregir errores operativos mientras protege la integridad de las existencias y de los movimientos que dependan de la recepción.

**Independent Test**: Se puede probar editando una recepción sin salidas y solicitando después la modificación de otra recepción con movimientos, para verificar las restricciones y el registro de auditoría.

**Acceptance Scenarios**:

1. **Scenario**: Edición directa de una recepción sin movimientos de salida
   - **Given** que una recepción no registra ninguna salida ni despacho
   - **When** el administrador corrige sus datos y proporciona una justificación por escrito
   - **Then** el sistema actualiza la recepción, recalcula los valores afectados y registra la modificación en el historial de auditoría

2. **Scenario**: Intento de edición directa de una recepción con movimientos
   - **Given** que una recepción ya registra al menos una salida o despacho
   - **When** el administrador intenta editarla o eliminarla directamente
   - **Then** el sistema bloquea la operación y exige realizar un ajuste o una anulación formal

3. **Scenario**: Ajuste o anulación formal de una recepción con movimientos
   - **Given** que una recepción registra movimientos y requiere una corrección
   - **When** el administrador registra un ajuste o una anulación formal con una justificación por escrito
   - **Then** el sistema conserva la recepción y sus movimientos históricos, aplica el cambio permitido y registra automáticamente la auditoría completa

4. **Scenario**: Modificación sin justificación
   - **Given** que el administrador solicita editar, ajustar o anular una recepción
   - **When** no proporciona una justificación por escrito
   - **Then** el sistema rechaza la operación y conserva los datos sin cambios

### Edge Cases

- La cantidad de bultos, el peso nominal por bulto y el precio neto por bulto deben ser mayores que cero.
- La fecha de vencimiento no puede ser anterior a la fecha de ingreso.
- No se puede confirmar una recepción sin código de lote, tipo de alimento, etapa de crianza, cantidad de bultos, peso nominal por bulto, marca, precio neto, impuesto y fechas.
- Un tipo de alimento y cada recepción deben estar asociados exactamente a una etapa de crianza.
- Una nueva entrega con un código de lote existente debe crear una recepción independiente; sus cantidades, fechas, costos y vencimiento no deben fusionarse con las entradas anteriores.
- El inventario debe manejar internamente las existencias en kilogramos netos, incluidos los saldos procedentes del fraccionamiento de bultos.
- Cuando se utilice solo una parte de un bulto, el saldo restante debe conservar su relación con el lote, la recepción y la etapa original.
- La selección automática de existencias debe priorizar la fecha de vencimiento más próxima y, si existe empate, la fecha de ingreso más antigua.
- El sistema no debe asignar alimento vencido.
- El sistema debe bloquear cualquier asignación cuando la etapa del alimento no coincida exactamente con la etapa actual del galpón de destino.
- El alimento sobrante en una sub-bodega debe reincorporarse a la bodega central en kilogramos netos, conservando su lote, recepción y etapa original.
- Cuando un galpón cambie de etapa, el sobrante correspondiente a la etapa anterior debe devolverse obligatoriamente a la bodega central.
- El alimento devuelto solo puede volver a asignarse a galpones que se encuentren en la misma etapa compatible.
- Una edición, ajuste o anulación no puede producir existencias negativas ni romper la trazabilidad de movimientos anteriores.
- Ningún cambio puede borrar silenciosamente una recepción, un movimiento o un registro de auditoría.

## Requirements 

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro de recepciones de alimento exclusivamente a usuarios con rol de administrador.
- **FR-002**: Cada recepción DEBE registrar código de lote, tipo de alimento, etapa de crianza, cantidad de bultos, peso nominal por bulto, marca, precio neto por bulto, impuesto, fecha de ingreso y fecha de vencimiento.
- **FR-003**: Cada recepción DEBE quedar asociada exactamente a una etapa de crianza.
- **FR-004**: El sistema DEBE calcular los kilogramos nominales totales multiplicando la cantidad de bultos por el peso nominal de cada bulto.
- **FR-005**: El sistema DEBE calcular el precio neto por kilogramo dividiendo el precio neto por bulto entre el peso nominal del bulto.
- **FR-006**: El sistema DEBE manejar los valores monetarios en pesos colombianos (COP).
- **FR-007**: El precio por bulto DEBE registrarse neto, sin impuestos, y el porcentaje o valor del impuesto DEBE registrarse de forma independiente.
- **FR-008**: Cada nueva entrega DEBE crear una recepción separada, incluso cuando ya exista otra con el mismo código de lote.
- **FR-009**: Las recepciones que compartan un código de lote DEBEN conservar de manera independiente sus cantidades, fechas, costos, impuestos, vencimiento y saldo.
- **FR-010**: Una vez confirmado el registro, el sistema DEBE incorporar los kilogramos nominales de la recepción al inventario de la bodega central.
- **FR-011**: El sistema DEBE manejar internamente las existencias y asignaciones de alimento en kilogramos netos.
- **FR-012**: El sistema DEBE permitir que un bulto sea fraccionado y DEBE mantener el saldo restante en kilogramos asociado al lote, recepción y etapa original.
- **FR-013**: Para toda asignación, el sistema DEBE seleccionar automáticamente primero las existencias con la fecha de vencimiento más próxima.
- **FR-014**: Cuando dos o más recepciones tengan la misma fecha de vencimiento, el sistema DEBE priorizar la recepción con la fecha de ingreso más antigua.
- **FR-015**: El sistema NO DEBE permitir la asignación de alimento vencido.
- **FR-016**: El sistema DEBE bloquear cualquier asignación o despacho si la etapa del alimento no coincide exactamente con la etapa actual del galpón de destino.
- **FR-017**: El sistema DEBE reincorporar a la bodega central el alimento sobrante de las sub-bodegas en kilogramos netos.
- **FR-018**: Todo alimento reincorporado DEBE conservar su asociación con el lote, la recepción y la etapa original.
- **FR-019**: Cuando un galpón cambie de etapa, el sistema DEBE exigir la devolución a la bodega central del sobrante asociado a la etapa anterior.
- **FR-020**: El alimento devuelto solo DEBE poder asignarse posteriormente a galpones cuya etapa actual coincida con la etapa original del alimento.
- **FR-021**: El sistema solo DEBE permitir la edición directa de una recepción cuando esta no registre ninguna salida o despacho.
- **FR-022**: Si una recepción registra movimientos, el sistema DEBE bloquear su edición y eliminación directa y DEBE exigir un ajuste o una anulación formal.
- **FR-023**: Toda edición, ajuste o anulación DEBE requerir una justificación por escrito.
- **FR-024**: El sistema DEBE registrar automáticamente en la auditoría el usuario responsable, fecha y hora, valores anteriores, valores nuevos y motivo del cambio.
- **FR-025**: Una modificación DEBE recalcular las cantidades y costos afectados sin alterar ni eliminar los movimientos históricos.
- **FR-026**: Los registros de recepciones, movimientos y auditoría NO DEBEN eliminarse silenciosamente como consecuencia de una edición, ajuste o anulación.
- **FR-027**: Ningún registro, edición, ajuste, anulación, asignación o devolución DEBE producir existencias negativas.

### Key Entities 

- **Recepción de alimento**: Entrada independiente de inventario que contiene el código de lote, tipo de alimento, etapa, marca, presentación, cantidad, precio neto, impuesto, fechas, saldo y estado.
- **Tipo de alimento**: Clasificación del alimento disponible en inventario, asociada a una etapa de crianza.
- **Etapa de crianza**: Fase que determina la compatibilidad entre el alimento registrado y el galpón al que pueda asignarse.
- **Bodega central**: Inventario principal en el que se incorporan las recepciones y al que regresan los sobrantes.
- **Sub-bodega**: Inventario asociado a un galpón que conserva existencias en kilogramos netos.
- **Galpón**: Unidad productiva con una etapa actual de crianza utilizada para validar la compatibilidad del alimento.
- **Movimiento de inventario**: Registro trazable de una asignación, devolución, ajuste o anulación que afecta las existencias de una recepción.
- **Registro de auditoría**: Evidencia inmutable de una modificación, con usuario, fecha y hora, valores anteriores, valores nuevos y justificación.

## Success Criteria 

### Measurable Outcomes

- **SC-001**: El 100 % de las recepciones confirmadas contiene todos los campos obligatorios y queda asociado a una única etapa de crianza.
- **SC-002**: En el 100 % de las recepciones, los kilogramos nominales totales y el precio neto por kilogramo coinciden con los valores calculados a partir de la cantidad y presentación registradas.
- **SC-003**: El 100 % de las nuevas entregas crea una recepción independiente, incluso cuando el código de lote ya existe.
- **SC-004**: El 100 % de los intentos de registrar una recepción por usuarios sin rol de administrador se rechaza sin modificar el inventario.
- **SC-005**: El 100 % de las selecciones automáticas de inventario prioriza la fecha de vencimiento más próxima y utiliza la fecha de ingreso más antigua como desempate.
- **SC-006**: El 100 % de las asignaciones con una etapa incompatible o alimento vencido se bloquea sin modificar las existencias.
- **SC-007**: El 100 % de los saldos fraccionados y alimentos devueltos conserva su cantidad en kilogramos y su asociación con el lote, recepción y etapa original.
- **SC-008**: El 100 % de las ediciones, ajustes y anulaciones exige justificación y registra usuario, fecha y hora, valores anteriores, valores nuevos y motivo.
- **SC-009**: Ninguna operación confirmada produce existencias negativas ni elimina recepciones, movimientos o auditorías históricas.

## Measurable Outcomes

- La ejecución operativa del traslado físico de alimento entre bodegas.
- La definición o ajuste de dosificaciones de alimento por pollo y por día.
- Los cálculos nutricionales de consumo de los galpones.
- La creación o administración de etapas de crianza, tipos de alimento, galpones, usuarios y roles; esta especificación los considera datos previamente disponibles.
- El consumo diario del alimento dentro del galpón, salvo las reglas de control aplicables a los saldos y retornos.
