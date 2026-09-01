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
   - **When** registra el código de lote, tipo de alimento, cantidad de bultos, peso nominal por bulto, marca, precio neto por bulto, impuesto, fecha de ingreso y fecha de vencimiento
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

- **Edge case #1 - Cantidad y peso válidos individualmente, pero cuyo producto excede la capacidad numérica**

  - ¿Cómo maneja el sistema una recepción cuya cantidad de bultos y peso nominal por bulto son válidos de forma individual, pero al multiplicarlos generan una cantidad de kilogramos superior a la capacidad numérica admitida?  
    El sistema debe detectar el desbordamiento antes de crear la recepción, rechazar el registro e informar que la cantidad total calculada excede el límite permitido. No debe almacenar una cantidad truncada, negativa o diferente del resultado real, ni modificar el inventario.

- **Edge case #2 - Precio por kilogramo con resultado decimal periódico**

  - ¿Cómo maneja el sistema una recepción cuyo precio neto por bulto dividido entre el peso nominal del bulto produce un resultado decimal periódico?  
    El sistema debe calcular el precio neto por kilogramo aplicando una precisión y una regla de redondeo uniformes, definidas para todos los registros. No debe producir valores diferentes entre la recepción y el inventario ni truncar el resultado de manera arbitraria.

## Requirements 

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro de recepciones de alimento exclusivamente a usuarios con rol de administrador.
- **FR-002**: Cada recepción DEBE registrar código de lote, tipo de alimento, cantidad de bultos, peso nominal por bulto, marca, precio neto por bulto, impuesto, fecha de ingreso y fecha de vencimiento.
- **FR-003**: El sistema DEBE validar los datos obligatorios y rechazar el registro cuando estén incompletos, sean inválidos o los valores calculados excedan los límites admitidos.
- **FR-004**: El sistema DEBE calcular los kilogramos nominales totales y el precio neto por kilogramo a partir de la cantidad, el peso nominal y el precio neto por bulto, aplicando una regla uniforme de precisión y redondeo.
- **FR-005**: Cada entrega DEBE crear una recepción independiente, incluso si comparte código de lote con otra, conservando sus propios datos, costos, fechas y saldo.

### Key Entities 

- **Recepción de alimento**: Registro de una entrega con sus datos de lote, tipo, cantidad, costo y fechas.
- **Tipo de alimento**: Clasificación del alimento que identifica la etapa para la cual está destinado.
- **Bodega central**: Inventario en el que se incorpora cada recepción registrada.

## Success Criteria 

### Measurable Outcomes

- **SC-001**: El 100 % de las recepciones confirmadas contiene todos los campos obligatorios y queda asociado a un tipo de alimento.
- **SC-002**: En el 100 % de las recepciones, los kilogramos nominales totales y el precio neto por kilogramo coinciden con los valores calculados a partir de la cantidad y presentación registradas.
- **SC-003**: El 100 % de las nuevas entregas crea una recepción independiente, incluso cuando el código de lote ya existe.
- **SC-004**: El 100 % de los intentos de registrar una recepción por usuarios sin rol de administrador se rechaza sin modificar el inventario.
