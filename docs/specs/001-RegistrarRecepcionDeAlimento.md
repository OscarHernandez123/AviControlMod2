# Feature Specification: Registrar recepción de alimento

**Created**: 2026-08-28  
**Updated**: 2026-09-28

## User Scenarios & Testing

### User Story 1 - Registrar una recepción de alimento (Priority: P1)

Como administrador, quiero registrar cada recepción de alimento que ingresa a la bodega central, indicando directamente el alimento y su tipo, para formalizar la entrega, generar su movimiento de entrada y conservar los datos y precios históricos necesarios para valorar posteriormente el alimento consumido.

**Why this priority**: La recepción constituye la entrada oficial del alimento al inventario. Sin el alimento recibido, su tipo, cantidades, fechas y precios históricos no es posible mantener la trazabilidad de la entrega ni calcular correctamente sus existencias y costos.

**Independent Test**: Se puede probar registrando directamente el alimento, su tipo, el código de lote, las fechas y los datos de varios bultos con impuesto. El sistema debe crear una recepción independiente, calcular el peso total, el precio neto por kilogramo, el subtotal neto, el valor del impuesto y el total de la compra, generar el movimiento de entrada y conservar todos los valores históricos.

**Acceptance Scenarios**:

1. **Scenario**: Registro correcto de una recepción
   - **Given** que un administrador autenticado dispone de los datos completos de una entrega
   - **When** registra el alimento, tipo de alimento, código de lote, fecha de ingreso, fecha de vencimiento, cantidad de bultos, peso por bulto, precio neto por bulto y porcentaje de impuesto
   - **Then** el sistema crea una recepción confirmada, calcula sus valores derivados, genera un movimiento confirmado de entrada por el peso total y conserva el alimento, tipo de alimento y precios como valores históricos

2. **Scenario**: Registro de una entrega con un código de lote existente
   - **Given** que ya existe una recepción con el mismo código de lote
   - **When** el administrador registra una nueva entrega
   - **Then** el sistema crea una recepción independiente sin acumular sus cantidades en el registro anterior y conserva por separado el alimento, tipo de alimento, fechas, cantidades y precios

3. **Scenario**: Intento de registro con datos obligatorios incompletos
   - **Given** que el administrador está registrando una recepción
   - **When** omite el alimento, tipo de alimento o cualquiera de los demás campos obligatorios, o ingresa valores inválidos
   - **Then** el sistema rechaza el registro, informa los campos que deben corregirse y no modifica el inventario

4. **Scenario**: Intento de registro por un usuario no autorizado
   - **Given** que un usuario sin rol de administrador intenta registrar una recepción
   - **When** solicita confirmar el registro
   - **Then** el sistema rechaza la operación y no modifica el inventario

---

### User Story 2 - Editar una recepción de alimento (Priority: P2)

Como administrador, quiero corregir determinados datos de una recepción que todavía no tenga movimientos confirmados posteriores a su entrada, para solucionar errores sin modificar su identidad histórica ni afectar salidas, consumos o costos existentes.

**Why this priority**: La edición controlada permite corregir datos operativos de una recepción no utilizada, mientras protege el código de lote, la fecha de ingreso, el tipo de alimento y los movimientos que garantizan su trazabilidad.

**Independent Test**: Se puede probar editando una recepción que únicamente tenga su movimiento inicial de entrada. El modal debe permitir modificar alimento, fecha de vencimiento, cantidad de bultos, peso por bulto, precio neto por bulto e impuesto; después debe recalcular los valores derivados, ajustar la entrada y registrar auditoría. Una recepción con movimientos posteriores debe bloquearse.

**Acceptance Scenarios**:

1. **Scenario**: Edición de una recepción no utilizada
   - **Given** que una recepción solamente tiene su movimiento inicial de entrada y no registra salidas, despachos, consumos, ajustes, vencimientos ni anulaciones confirmadas
   - **When** el administrador modifica el alimento, fecha de vencimiento, cantidad de bultos, peso por bulto, precio neto por bulto o porcentaje de impuesto y confirma la edición
   - **Then** el sistema actualiza únicamente los campos permitidos, conserva los campos protegidos, recalcula los valores derivados, ajusta el movimiento inicial de entrada y registra los cambios en el historial de auditoría

2. **Scenario**: Visualización del modal de edición
   - **Given** que la recepción cumple las condiciones para editarse
   - **When** el administrador abre el modal `Editar recepción de alimento`
   - **Then** el sistema muestra precargados únicamente `Alimento`, `Fecha de vencimiento`, `Cantidad de bultos`, `Peso por bulto`, `Precio neto por bulto` e `Impuesto`, sin presentar como editables el código de lote, fecha de ingreso, tipo de alimento ni los valores calculados

3. **Scenario**: Intento de edición de una recepción utilizada
   - **Given** que una recepción tiene al menos un movimiento confirmado de salida, despacho, consumo, ajuste, vencimiento o anulación
   - **When** el administrador intenta editarla
   - **Then** el sistema bloquea la operación, informa que la recepción ya tiene movimientos asociados y conserva sus datos y existencias sin cambios

4. **Scenario**: Intento de edición por un usuario no autorizado
   - **Given** que un usuario sin rol de administrador intenta editar una recepción
   - **When** solicita guardar los cambios
   - **Then** el sistema rechaza la operación y conserva la recepción y el inventario sin cambios

---

### User Story 3 - Consultar el historial de recepciones de alimento (Priority: P2)

Como administrador, quiero consultar y filtrar el historial de recepciones de alimento, abrir el detalle completo de cada recepción y acceder a su edición cuando esté permitida, para mantener la trazabilidad de las entradas realizadas en la bodega central.

**Why this priority**: El historial permite localizar y revisar una entrega concreta, incluyendo el alimento y su tipo, sin convertir esta pantalla en una consulta de existencias, responsabilidad que corresponde al SPEC-023.

**Independent Test**: Se puede probar creando recepciones con diferentes alimentos, tipos, fechas y estados. El historial debe mostrar sus indicadores, búsqueda, filtros y tabla; el detalle debe incluir el tipo de alimento y los cálculos de la recepción; la edición debe mostrar únicamente los seis campos permitidos cuando no existan movimientos posteriores.

**Acceptance Scenarios**:

1. **Scenario**: Visualización del historial de recepciones
   - **Given** que existen recepciones de alimento registradas
   - **When** el administrador abre la pantalla de recepciones de alimentos
   - **Then** el sistema muestra para cada recepción el código de lote, alimento, cantidad de bultos, precio neto por bulto y peso por bulto, junto con las acciones `Detalles` y `Editar`

2. **Scenario**: Indicadores de recepciones del mes
   - **Given** que existen recepciones confirmadas cuya fecha de ingreso pertenece al mes calendario actual
   - **When** el administrador abre el historial
   - **Then** el sistema muestra la cantidad de recepciones del mes y la suma de sus pesos totales en kilogramos

3. **Scenario**: Búsqueda por recepción o lote
   - **Given** que existe una recepción identificada o asociada con un código de lote conocido
   - **When** el administrador ingresa el identificador de la recepción o el código de lote en el buscador y aplica el filtro
   - **Then** el sistema muestra únicamente las recepciones coincidentes

4. **Scenario**: Filtrado por estado y periodo
   - **Given** que existen recepciones con diferentes estados y fechas de ingreso
   - **When** el administrador selecciona un estado, un periodo y aplica los filtros
   - **Then** el sistema muestra las recepciones que cumplen simultáneamente ambos criterios

5. **Scenario**: Consulta del detalle de una recepción
   - **Given** que el administrador selecciona la acción `Detalles` de una recepción
   - **When** el sistema abre el modal `Detalles recepción de alimento`
   - **Then** muestra en modo de solo lectura `Alimento`, `Código del lote`, `Fecha de ingreso`, `Fecha de vencimiento`, `Cantidad de bultos`, `Peso por bulto`, `Precio neto por bulto`, `Impuesto`, `Peso total`, `Tipo de alimento`, `Valor del impuesto` y `Total de la compra`, expresando el peso total en kilogramos y los valores económicos en su formato correspondiente

6. **Scenario**: Acceso a la edición desde el historial
   - **Given** que el administrador consulta el historial
   - **When** selecciona `Editar` sobre una recepción
   - **Then** el sistema abre el modal con los seis campos editables precargados cuando no existen movimientos posteriores confirmados, o bloquea la edición e informa la causa cuando la recepción ya fue utilizada

7. **Scenario**: Historial sin resultados
   - **Given** que no existen recepciones o ninguna cumple los filtros aplicados
   - **When** el administrador consulta el historial
   - **Then** el sistema muestra un estado vacío informativo, mantiene los indicadores correspondientes y no presenta datos inventados

### Edge Cases

- **Edge case #1 - Cantidad y peso cuyo producto excede la capacidad numérica**

  - ¿Cómo maneja el sistema una recepción cuya cantidad de bultos y peso por bulto son válidos individualmente, pero su producto excede el límite admitido?  
    El sistema debe detectar el desbordamiento antes de guardar, rechazar la operación e informar que el peso total excede el límite permitido. No debe almacenar valores truncados, negativos o diferentes del resultado real.

- **Edge case #2 - Precio por kilogramo con resultado decimal periódico**

  - ¿Cómo maneja el sistema una división del precio neto por bulto entre el peso por bulto que produce un decimal periódico?  
    El sistema debe aplicar una precisión y una regla de redondeo uniformes. Aunque este valor no se edite ni se muestre obligatoriamente en los nuevos modales, debe conservarse correctamente para los cálculos históricos.

- **Edge case #3 - Fecha de vencimiento anterior a la fecha de ingreso**

  - ¿Cómo maneja el sistema una recepción cuya fecha de vencimiento es anterior o igual a la fecha de ingreso?  
    El sistema debe rechazar el registro o la edición, indicar que la fecha de vencimiento debe ser posterior a la fecha de ingreso y no modificar el inventario.

- **Edge case #4 - Impuesto con resultado decimal**

  - ¿Cómo maneja el sistema un porcentaje de impuesto cuyo cálculo produce fracciones monetarias?  
    El sistema debe aplicar una regla uniforme de precisión y redondeo. Los valores mostrados, almacenados y auditados deben coincidir.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir registrar, editar y consultar recepciones de alimento exclusivamente a usuarios con rol de administrador.
- **FR-002**: Cada recepción DEBE registrar como datos propios y obligatorios el alimento, tipo de alimento, código de lote, fecha de ingreso, fecha de vencimiento, cantidad de bultos, peso por bulto, precio neto por bulto y porcentaje de impuesto.
- **FR-003**: El sistema DEBE validar los datos ingresados, exigir que la fecha de vencimiento sea posterior a la fecha de ingreso y crear una recepción independiente por cada entrega, aunque varias compartan el mismo código de lote.
- **FR-004**: El sistema DEBE calcular `peso total = cantidad de bultos × peso por bulto`, `precio neto por kilogramo = precio neto por bulto ÷ peso por bulto`, `subtotal neto = cantidad de bultos × precio neto por bulto`, `valor del impuesto = subtotal neto × porcentaje de impuesto ÷ 100` y `total de la compra = subtotal neto + valor del impuesto`, aplicando precisión y redondeo uniformes.
- **FR-005**: El sistema DEBE conservar el alimento, tipo de alimento, fechas y precios de cada recepción como valores históricos, sin permitir que cambios en otras recepciones alteren esos datos ni los costos de consumos ya registrados.
- **FR-006**: Al confirmar una recepción, el sistema DEBE dejarla en estado `CONFIRMADA` y generar un movimiento confirmado de entrada por su peso total, vinculado con la recepción y disponible para la consulta de inventario del SPEC-023.
- **FR-007**: Una recepción solo DEBE poder editarse mientras no tenga movimientos confirmados posteriores a su entrada inicial; una salida, despacho, consumo, ajuste, vencimiento o anulación DEBE bloquear la edición.
- **FR-008**: El modal de edición DEBE permitir modificar únicamente `Alimento`, `Fecha de vencimiento`, `Cantidad de bultos`, `Peso por bulto`, `Precio neto por bulto` e `Impuesto`; los demás datos y valores calculados DEBEN permanecer protegidos.
- **FR-009**: Al guardar una edición válida, el sistema DEBE recalcular los valores derivados, ajustar atómicamente el movimiento inicial de entrada y registrar en auditoría los valores anteriores, los nuevos, el usuario y la fecha y hora.
- **FR-010**: El historial DEBE mostrar los indicadores mensuales y una tabla con código de lote, alimento, cantidad de bultos, precio neto por bulto, peso por bulto y las acciones `Detalles` y `Editar`, sin modificar datos ni existencias.
- **FR-011**: El historial DEBE permitir buscar por identificador de recepción o código de lote y filtrar simultáneamente por estado y periodo de fecha de ingreso, usando inicialmente los últimos 30 días.
- **FR-012**: El modal de detalle DEBE mostrar en modo de solo lectura alimento, código de lote, fecha de ingreso, fecha de vencimiento, cantidad de bultos, peso por bulto, precio neto por bulto, impuesto, peso total, tipo de alimento, valor del impuesto y total de la compra; el peso total DEBE expresarse en kilogramos y sin símbolo monetario.
- **FR-013**: El precio neto por kilogramo y el subtotal neto DEBEN calcularse y conservarse internamente, aunque no se muestren en los nuevos modales.

### Key Entities

- **Recepción de alimento**: Representa una entrega concreta de alimento que ingresa a la bodega central.
  - **Atributos registrados**: identificador, alimento, tipo de alimento, código de lote, estado, fecha de ingreso, fecha de vencimiento, cantidad de bultos, peso por bulto, precio neto por bulto y porcentaje de impuesto.
  - **Atributos calculados**: peso total, precio neto por kilogramo, subtotal neto, valor del impuesto y total de la compra.
  - **Relaciones**: pertenece a la bodega central, origina un movimiento de entrada, puede tener movimientos posteriores y se vincula con registros de auditoría.
  - **Comportamiento histórico**: conserva directamente el alimento, tipo de alimento y valores comerciales necesarios para reconstruir la recepción.
- **Tipo de alimento**: Representa la clasificación registrada para la recepción, vinculada a la etapa de crianza correspondiente (`etapaCrianza`: enum `EtapaCrianza`: `PRE_INICIO`, `INICIO`, `ENGORDE`).
  - **Comportamiento**: se registra con la recepción, se conserva como valor histórico, se muestra en el detalle y no se modifica desde el modal de edición.
- **Bodega central**: Representa el inventario principal que recibe las entregas y consolida los movimientos que determinan las existencias.
- **Movimiento de inventario de alimento**: Representa una entrada, salida, despacho, consumo, ajuste, vencimiento o anulación que afecta la existencia de una recepción.
  - **Atributos utilizados**: identificador, recepción, tipo, estado, cantidad en kilogramos, fecha, hora, motivo y usuario responsable.
  - **Comportamiento**: la entrada inicial no bloquea la edición; cualquier movimiento posterior confirmado sí la bloquea.
- **Registro de auditoría**: Representa los cambios realizados sobre una recepción.
  - **Atributos utilizados**: acción, fecha, hora, valores anteriores, valores nuevos y usuario responsable.
- **Historial de recepciones de alimento**: Representa la consulta de las entradas registradas.
  - **Datos mostrados**: indicadores del mes, filtros, resultados resumidos y acceso al detalle y a la edición condicionada.
  - **Comportamiento**: no modifica recepciones, movimientos ni existencias.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los administradores puede completar en menos de 3 minutos un registro válido que incluya el alimento y su tipo.
- **SC-002**: El 95 % de los registros confirmados calcula sus valores, genera el movimiento de entrada y conserva alimento, tipo y precios históricos en un máximo de 2 segundos.
- **SC-003**: El 100 % de los modales de edición presenta únicamente los seis campos editables definidos y conserva los demás valores sin cambios.
- **SC-004**: El 100 % de los registros y ediciones válidos calcula correctamente peso total, precio por kilogramo, subtotal, impuesto y total.
- **SC-005**: El 95 % de las búsquedas y filtros del historial presenta resultados en un máximo de 2 segundos.
- **SC-006**: El 100 % de los detalles muestra el tipo de alimento y valores consistentes con la recepción sin modificar información.
- **SC-007**: El 100 % de los intentos de editar una recepción con movimientos posteriores confirmados es bloqueado.
