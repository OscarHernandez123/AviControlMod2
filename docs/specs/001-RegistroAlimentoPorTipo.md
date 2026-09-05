# Feature Specification: Registro de alimentos en bodega central

**Created**: 2026-08-28  

## User Scenarios & Testing 

### User Story 1 - Registrar una recepción de alimento (Priority: P1)

Como administrador, quiero registrar cada recepción de alimento que ingresa a la bodega central para mantener un inventario exacto, valorizado y trazable por entrega, y conservar sus precios históricos de compra para que el módulo 3 pueda calcular el costo del alimento consumido por cada lote de aves.

**Why this priority**: El registro de las recepciones constituye la entrada oficial del inventario. Sin sus cantidades y precios históricos de compra no es posible conocer las existencias ni valorar en el módulo 3 el alimento realmente consumido por cada lote de aves.

**Independent Test**: Se puede probar registrando una recepción de varios bultos y verificando que el sistema cree una entrada independiente, calcule los kilogramos nominales y el precio neto de compra por kilogramo, actualice las existencias de la bodega central y conserve ese precio asociado a la recepción para su posterior uso por el módulo 3.

**Acceptance Scenarios**:

1. **Scenario**: Registro correcto de una recepción
   - **Given** que un administrador autenticado dispone de los datos completos de una entrega de alimento
   - **When** registra el código de lote, tipo de alimento, cantidad de bultos, peso nominal por bulto, marca, precio neto de compra por bulto, impuesto, fecha de ingreso y fecha de vencimiento
   - **Then** el sistema crea una recepción en la bodega central, calcula sus kilogramos nominales totales y el precio neto de compra por kilogramo, actualiza el inventario y conserva los precios históricos (precio de compra registrado cuando ingresó una recepción específica al inventario) asociados a la recepción para el módulo 3

2. **Scenario**: Registro de una entrega con un código de lote existente
   - **Given** que ya existe una recepción con el mismo código de lote
   - **When** el administrador registra una nueva entrega
   - **Then** el sistema crea una recepción separada sin sumar sus cantidades al registro anterior y conserva de forma independiente las fechas, los precios de compra y el vencimiento de cada entrega

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

Como administrador, quiero corregir o anular una recepción bajo reglas controladas para solucionar errores sin perder la trazabilidad ni alterar silenciosamente los saldos y precios históricos de compra.

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

- **Edge case #2 - Precio de compra por kilogramo con resultado decimal periódico**

  - ¿Cómo maneja el sistema una recepción cuyo precio neto de compra por bulto dividido entre el peso nominal del bulto produce un resultado decimal periódico?
    El sistema debe calcular el precio neto de compra por kilogramo aplicando una precisión y una regla de redondeo uniformes, definidas para todos los registros. No debe producir valores diferentes entre la recepción y el inventario ni truncar el resultado de manera arbitraria.

## Requirements 

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro de recepciones de alimento exclusivamente a usuarios con rol de administrador.
- **FR-002**: Cada recepción DEBE registrar código de lote, tipo de alimento, cantidad de bultos, peso nominal por bulto, marca, precio neto de compra por bulto, impuesto, fecha de ingreso y fecha de vencimiento.
- **FR-003**: El sistema DEBE validar los datos obligatorios y rechazar el registro cuando estén incompletos, sean inválidos o los valores calculados excedan los límites admitidos.
- **FR-004**: El sistema DEBE calcular los kilogramos nominales totales y el precio neto de compra por kilogramo a partir de la cantidad, el peso nominal y el precio neto de compra por bulto, aplicando una regla uniforme de precisión y redondeo.
- **FR-005**: Cada entrega DEBE crear una recepción independiente, incluso si comparte código de lote con otra, conservando sus propios datos, precios de compra, fechas y saldo.
- **FR-006**: Al confirmar el registro, el sistema DEBE conservar y dejar disponibles para el módulo 3 el identificador de la recepción, el tipo de alimento, el código de lote del alimento, el precio neto de compra por bulto, el precio neto de compra por kilogramo y el impuesto.
- **FR-007**: La modificación posterior del precio de otra recepción o del precio vigente de un tipo de alimento NO DEBE alterar el precio histórico asociado a un consumo ya registrado.
- **FR-008**: La información suministrada DEBE permitir al módulo 3 calcular el costo del alimento mediante la suma de `kilogramos consumidos de cada recepción × precio neto histórico de compra por kilogramo`. El valor total de una recepción NO DEBE tratarse como costo de un lote de aves.

### Key Entities 

- **Alimento**: Representa el alimento registrado en el inventario.
  - **Atributos posibles**: lote, marca, cantidad de bultos, peso nominal, precios de compra, impuesto, fechas y kilogramos totales.
  - **Relaciones**: pertenece a un tipo de alimento, se almacena en la bodega central, se vincula con movimientos y registros de auditoría, y proporciona sus precios históricos de compra al módulo 3.
- **Tipo de alimento**: Representa la clasificación del alimento según la etapa productiva.
  - **Atributos posibles**: nombre, descripción y estado.
  - **Relaciones**: clasifica uno o varios alimentos registrados.
- **Bodega central**: Representa el inventario principal.
  - **Atributos posibles**: nombre, ubicación y estado.
  - **Relaciones**: almacena los alimentos registrados y consolida los movimientos que afectan sus existencias.
- **Registro de auditoría**: Representa el historial de cambios sobre un registro.
  - **Atributos posibles**: acción, fecha y hora, motivo, valores anteriores y valores nuevos.
  - **Relaciones**: identifica al usuario responsable y al alimento o movimiento afectado.

## Success Criteria 

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los administradores puede completar un registro de alimento válido en menos de 3 minutos.
- **SC-002**: El 95 % de los registros confirmados muestra los cálculos, actualiza el inventario y deja sus precios históricos de compra disponibles para el módulo 3 en un máximo de 2 segundos.
- **SC-003**: Al menos el 90 % de los usuarios completa correctamente el registro de alimento en el primer intento durante pruebas de usabilidad.
- **SC-004**: Al menos el 85 % de los administradores califica la experiencia de registro con 4 o más puntos sobre 5.
