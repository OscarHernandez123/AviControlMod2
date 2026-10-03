# Feature Specification: Registro de recepción de medicamento

**Created**: 2026-08-28  
**Updated**: 2026-09-28

## User Scenarios & Testing

### User Story 1 - Registrar una recepción de medicamento (Priority: P1)

Como administrador, quiero registrar cada recepción de medicamento que ingresa a la bodega central, indicando directamente el medicamento y los datos de su presentación, para formalizar la entrada, generar su movimiento de inventario y conservar sus valores históricos.

**Why this priority**: La recepción constituye la entrada oficial del medicamento al inventario. Sin sus cantidades, contenido, fechas y precios no es posible mantener la trazabilidad de la compra ni valorar posteriormente los medicamentos aplicados.

**Independent Test**: Se puede probar registrando directamente el medicamento, código de lote, presentación, cantidad, contenido por presentación, unidad de medida, precio neto por presentación, impuesto, fecha de ingreso y fecha de vencimiento. El sistema debe crear una recepción independiente, calcular sus valores derivados, generar el movimiento de entrada y conservar los datos históricos.

**Acceptance Scenarios**:

1. **Scenario**: Registro correcto de una recepción
   - **Given** que un administrador autenticado dispone de los datos completos de una entrega
   - **When** registra el medicamento, código de lote, presentación, cantidad, contenido por presentación, unidad de medida, precio neto por presentación, porcentaje de impuesto, fecha de ingreso y fecha de vencimiento
   - **Then** el sistema crea una recepción confirmada, calcula el contenido total, precio neto por unidad de medida, subtotal neto, valor del impuesto y precio total, genera un movimiento confirmado de entrada y conserva los valores históricos

2. **Scenario**: Registro de una compra con un código de lote existente
   - **Given** que ya existe una recepción con el mismo código de lote
   - **When** el administrador registra un nuevo ingreso
   - **Then** el sistema crea una recepción independiente y conserva por separado su medicamento, presentación, cantidades, fechas, precios e impuesto

3. **Scenario**: Intento de registro con datos incompletos o inválidos
   - **Given** que el administrador está registrando una recepción
   - **When** omite un campo obligatorio o introduce cantidades, contenidos, precios, impuestos o fechas inválidos
   - **Then** el sistema rechaza el registro, identifica los campos que deben corregirse y no modifica el inventario

4. **Scenario**: Intento de registro por un usuario no autorizado
   - **Given** que un usuario sin rol de administrador intenta registrar una recepción de medicamento
   - **When** solicita confirmar el registro
   - **Then** el sistema rechaza la operación y no modifica el inventario

---

### User Story 2 - Editar una recepción de medicamento (Priority: P2)

Como administrador, quiero corregir determinados datos de una recepción que todavía no tenga movimientos confirmados posteriores a su entrada, para solucionar errores sin modificar su identidad histórica ni afectar aplicaciones o costos existentes.

**Why this priority**: La edición controlada permite corregir una recepción no utilizada, mientras protege el código de lote, la presentación, la fecha de ingreso y los movimientos que garantizan su trazabilidad.

**Independent Test**: Se puede probar editando una recepción que únicamente tenga su movimiento inicial de entrada. El modal debe permitir modificar medicamento, fecha de vencimiento, cantidad, contenido por presentación, unidad de medida, impuesto y precio neto por presentación; después debe recalcular los valores derivados, ajustar la entrada y registrar auditoría. Una recepción con movimientos posteriores debe bloquearse.

**Acceptance Scenarios**:

1. **Scenario**: Edición de una recepción no utilizada
   - **Given** que una recepción solamente tiene su movimiento inicial de entrada y no registra salidas, despachos, aplicaciones, consumos, ajustes, vencimientos ni anulaciones confirmadas
   - **When** el administrador modifica el medicamento, fecha de vencimiento, cantidad, contenido por presentación, unidad de medida, impuesto o precio neto por presentación y confirma la edición
   - **Then** el sistema actualiza únicamente los campos permitidos, conserva los campos protegidos, recalcula los valores derivados, ajusta el movimiento inicial de entrada y registra los cambios en el historial de auditoría

2. **Scenario**: Visualización del modal de edición
   - **Given** que la recepción cumple las condiciones para editarse
   - **When** el administrador abre el modal `Editar recepción de medicamento`
   - **Then** el sistema muestra precargados únicamente `Medicamento`, `Fecha de vencimiento`, `Cantidad`, `Contenido por presentación`, `Unidad de medida`, `Impuesto` y `Precio neto por presentación`, sin presentar como editables el código de lote, presentación, fecha de ingreso ni los valores calculados

3. **Scenario**: Intento de edición de una recepción utilizada
   - **Given** que una recepción tiene al menos un movimiento confirmado de salida, despacho, aplicación, consumo, ajuste, vencimiento o anulación
   - **When** el administrador intenta editarla
   - **Then** el sistema bloquea la operación, informa que la recepción ya tiene movimientos asociados y conserva sus datos y existencias sin cambios

4. **Scenario**: Intento de edición por un usuario no autorizado
   - **Given** que un usuario sin rol de administrador intenta editar una recepción
   - **When** solicita guardar los cambios
   - **Then** el sistema rechaza la operación y conserva la recepción y el inventario sin cambios

---

### User Story 3 - Consultar el historial de recepciones de medicamentos (Priority: P2)

Como administrador, quiero consultar y filtrar el historial de recepciones de medicamentos, abrir el detalle de cada recepción y acceder a su edición cuando esté permitida, para mantener la trazabilidad de las entradas realizadas en la bodega central.

**Why this priority**: El historial permite verificar y localizar una entrada específica sin convertir esta pantalla en una consulta de existencias, responsabilidad que corresponde al SPEC-023.

**Independent Test**: Se puede probar creando recepciones de diferentes medicamentos, presentaciones y fechas. El historial debe mostrar su indicador, búsqueda, filtros y tabla; el detalle debe presentar los datos completos; la edición debe mostrar únicamente los siete campos permitidos cuando no existan movimientos posteriores.

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
   - **When** el administrador ingresa el código de lote o nombre del medicamento y aplica el filtro
   - **Then** el sistema muestra únicamente las recepciones coincidentes

4. **Scenario**: Filtrado por presentación y periodo
   - **Given** que existen recepciones con diferentes presentaciones y fechas de ingreso
   - **When** el administrador selecciona una presentación, un periodo y aplica los filtros
   - **Then** el sistema muestra las recepciones que cumplen simultáneamente ambos criterios

5. **Scenario**: Consulta del detalle de una recepción
   - **Given** que el administrador selecciona la acción `Detalles` de una recepción
   - **When** el sistema abre el modal de detalle
   - **Then** muestra en modo de solo lectura el medicamento, código de lote, presentación, cantidad, contenido por presentación, unidad de medida, precio neto por presentación, impuesto, fecha de ingreso, fecha de vencimiento, subtotal neto y precio total

6. **Scenario**: Acceso a la edición desde el historial
   - **Given** que el administrador consulta el historial
   - **When** selecciona `Editar` sobre una recepción
   - **Then** el sistema abre el modal con los siete campos editables precargados cuando no existen movimientos posteriores confirmados, o bloquea la edición e informa la causa cuando la recepción ya fue utilizada

7. **Scenario**: Historial sin resultados
   - **Given** que no existen recepciones o ninguna cumple los filtros aplicados
   - **When** el administrador consulta el historial
   - **Then** el sistema muestra un estado vacío informativo y no presenta datos inventados

### Edge Cases

- **Edge case #1 - Cantidad y contenido cuyo producto excede la capacidad numérica**

  - ¿Cómo maneja el sistema una recepción cuya cantidad y contenido por presentación son válidos individualmente, pero su producto excede el límite admitido?  
    El sistema debe detectar el desbordamiento antes de guardar, rechazar la operación e informar que el contenido total excede el límite permitido. No debe almacenar valores truncados o incorrectos ni modificar el inventario.

- **Edge case #2 - Precio por unidad de medida con resultado decimal periódico**

  - ¿Cómo maneja el sistema una división del precio neto por presentación entre el contenido por presentación que produce un decimal periódico?  
    El sistema debe aplicar una precisión y una regla de redondeo uniformes. El valor utilizado en la recepción y el inventario debe coincidir y no debe truncarse arbitrariamente.

- **Edge case #3 - Fecha de vencimiento anterior a la fecha de ingreso**

  - ¿Cómo maneja el sistema una recepción cuya fecha de vencimiento es anterior o igual a la fecha de ingreso?  
    El sistema debe rechazar el registro o la edición, indicar que la fecha de vencimiento debe ser posterior a la fecha de ingreso y no modificar el inventario.

- **Edge case #4 - Impuesto con resultado decimal**

  - ¿Cómo maneja el sistema un porcentaje de impuesto cuyo cálculo produce fracciones monetarias?  
    El sistema debe aplicar una regla uniforme de precisión y redondeo. Los valores mostrados, almacenados y auditados deben coincidir.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir registrar, editar y consultar recepciones de medicamentos exclusivamente a usuarios con rol de administrador.
- **FR-002**: Cada recepción DEBE registrar como datos propios y obligatorios el medicamento, código de lote, presentación, cantidad, contenido por presentación, unidad de medida, precio neto por presentación, porcentaje de impuesto, fecha de ingreso y fecha de vencimiento.
- **FR-003**: Presentación y unidad de medida DEBEN seleccionarse de sus respectivos conjuntos de valores permitidos, cuya enumeración queda fuera del alcance de esta especificación; el sistema DEBE validar los demás datos y exigir que la fecha de vencimiento sea posterior a la fecha de ingreso.
- **FR-004**: Cada entrega DEBE crear una recepción independiente, aunque varias compartan el código de lote, y el sistema DEBE calcular `contenido total = cantidad × contenido por presentación`, `precio neto por unidad de medida = precio neto por presentación ÷ contenido por presentación`, `subtotal neto = cantidad × precio neto por presentación`, `valor del impuesto = subtotal neto × porcentaje de impuesto ÷ 100` y `precio total = subtotal neto + valor del impuesto`.
- **FR-005**: El sistema DEBE conservar el medicamento, presentación, contenido, unidad de medida, fechas y precios de cada recepción como valores históricos, sin permitir que cambios en otras recepciones alteren esos datos ni los costos de aplicaciones ya registradas.
- **FR-006**: Al confirmar una recepción, el sistema DEBE dejarla en estado `CONFIRMADA` y generar un movimiento confirmado de entrada por su contenido total, vinculado con la recepción y disponible para la consulta de inventario del SPEC-023.
- **FR-007**: Una recepción solo DEBE poder editarse mientras no tenga movimientos confirmados posteriores a su entrada inicial; una salida, despacho, aplicación, consumo, ajuste, vencimiento o anulación DEBE bloquear la edición.
- **FR-008**: El modal de edición DEBE permitir modificar únicamente `Medicamento`, `Fecha de vencimiento`, `Cantidad`, `Contenido por presentación`, `Unidad de medida`, `Impuesto` y `Precio neto por presentación`; el código de lote, presentación, fecha de ingreso y los valores calculados DEBEN permanecer protegidos.
- **FR-009**: Al guardar una edición válida, el sistema DEBE recalcular los valores derivados, ajustar atómicamente el movimiento inicial de entrada y registrar en auditoría los valores anteriores, los nuevos, el usuario y la fecha y hora.
- **FR-010**: El historial DEBE mostrar el indicador mensual y una tabla con código de lote, medicamento, presentación, cantidad, precio neto por presentación, contenido por presentación y las acciones `Detalles` y `Editar`, sin modificar datos ni existencias.
- **FR-011**: El historial DEBE permitir buscar por código de lote o medicamento y filtrar simultáneamente por presentación y periodo de fecha de ingreso, usando inicialmente los últimos 30 días.
- **FR-012**: El modal de detalle DEBE mostrar en modo de solo lectura medicamento, código de lote, presentación, cantidad, contenido por presentación, unidad de medida, precio neto por presentación, impuesto, fecha de ingreso, fecha de vencimiento, subtotal neto y precio total.
- **FR-013**: El contenido total, precio neto por unidad de medida, valor del impuesto y demás valores derivados DEBEN calcularse y conservarse internamente aunque no se muestren en los nuevos modales.

### Key Entities

- **Recepción de medicamento**: Representa una compra o entrega concreta de medicamento que ingresa a la bodega central.
  - **Atributos registrados**: identificador, medicamento, código de lote, presentación, cantidad, contenido por presentación, unidad de medida, precio neto por presentación, porcentaje de impuesto, fecha de ingreso, fecha de vencimiento y estado.
  - **Atributos calculados**: contenido total, precio neto por unidad de medida, subtotal neto, valor del impuesto y precio total.
  - **Relaciones**: pertenece a la bodega central, origina un movimiento de entrada, puede tener movimientos posteriores y se vincula con registros de auditoría.
  - **Comportamiento histórico**: conserva directamente el medicamento y los datos de la presentación necesarios para reconstruir la recepción.
- **Presentación**: Representa la forma en que se recibe el medicamento. Sus valores permitidos se definirán posteriormente.
- **Unidad de medida**: Representa la unidad usada para expresar el contenido de cada presentación. Sus valores permitidos se definirán posteriormente.
- **Bodega central**: Representa el inventario principal que recibe las entregas y consolida los movimientos que determinan las existencias.
- **Movimiento de inventario de medicamento**: Representa una entrada, salida, despacho, aplicación, consumo, ajuste, vencimiento o anulación que afecta la existencia de una recepción.
  - **Atributos utilizados**: identificador, recepción, tipo, estado, cantidad en la unidad de medida registrada, fecha, hora, motivo y usuario responsable.
  - **Comportamiento**: la entrada inicial no bloquea la edición; cualquier movimiento posterior confirmado sí la bloquea.
- **Registro de auditoría**: Representa los cambios realizados sobre una recepción.
  - **Atributos utilizados**: acción, fecha, hora, valores anteriores, valores nuevos y usuario responsable.
- **Historial de recepciones de medicamentos**: Representa la consulta de las entradas registradas.
  - **Datos mostrados**: indicador del mes, filtros, resultados resumidos y acceso al detalle y a la edición condicionada.
  - **Comportamiento**: no modifica recepciones, movimientos ni existencias.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los administradores puede completar un registro válido de medicamento en menos de 3 minutos.
- **SC-002**: El 95 % de los registros confirmados calcula sus valores, genera el movimiento de entrada y conserva los datos históricos en un máximo de 2 segundos.
- **SC-003**: El 100 % de los modales de edición presenta únicamente los siete campos editables definidos y conserva los demás valores sin cambios.
- **SC-004**: El 100 % de los registros y ediciones válidos calcula correctamente contenido total, precio por unidad de medida, subtotal, impuesto y precio total.
- **SC-005**: El 100 % de los intentos de editar una recepción con movimientos posteriores confirmados es bloqueado.
