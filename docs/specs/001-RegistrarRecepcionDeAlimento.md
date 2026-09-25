# Feature Specification: Registrar recepción de alimento

**Created**: 2026-08-28  

## User Scenarios & Testing 

### User Story 1 - Registrar una recepción de alimento (Priority: P1)

Como administrador, quiero registrar cada recepción de alimento que ingresa a la bodega central para dejar formalizada y trazable cada entrega, generar su movimiento de entrada y conservar sus precios históricos de compra para que el módulo 3 pueda calcular el costo del alimento consumido por cada lote de aves.

**Why this priority**: El registro de cada recepción constituye la entrada oficial del alimento al inventario. Sin sus cantidades y precios históricos de compra no es posible mantener la trazabilidad de las entregas ni valorar en el módulo 3 el alimento realmente consumido por cada lote de aves.

**Independent Test**: Se puede probar registrando una recepción de varios bultos con un porcentaje de impuesto y verificando que el sistema cree una recepción independiente, obtenga el tipo del alimento seleccionado, calcule los kilogramos nominales totales, el precio neto de compra por kilogramo, el subtotal neto, el valor del impuesto y el total de la compra, genere el movimiento de entrada correspondiente y conserve los precios históricos asociados a la recepción.

**Acceptance Scenarios**:

1. **Scenario**: Registro correcto de una recepción
   - **Given** que un administrador autenticado dispone de los datos completos de una entrega y selecciona un alimento activo del catálogo
   - **When** registra el código de lote, cantidad de bultos, peso nominal por bulto, precio neto de compra por bulto, impuesto, fecha de ingreso y fecha de vencimientogit 
   - **Then** el sistema crea una recepción confirmada en la bodega central, obtiene el tipo del alimento, calcula los kilogramos nominales totales, el precio neto de compra por kilogramo, el subtotal neto, el valor del impuesto y el total de la compra, genera un movimiento confirmado de entrada y conserva los precios históricos asociados a la recepción para el módulo 3

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

### User Story 2 - Editar una recepción (Priority: P2)

Como administrador, quiero editar una recepción que todavía no tenga movimientos de salida para corregir errores de registro sin perder la trazabilidad ni alterar consumos o costos históricos.

**Why this priority**: Permite corregir errores operativos antes de que la recepción sea utilizada, mientras protege la integridad de las existencias y de los movimientos que dependan de ella.

**Independent Test**: Se puede probar editando, mediante los mismos campos disponibles en el registro, una recepción sin movimientos de salida y solicitando después la edición de otra que sí los tenga, para verificar los recálculos, el bloqueo y el registro de auditoría.

**Acceptance Scenarios**:

1. **Scenario**: Edición de una recepción sin movimientos de salida
   - **Given** que una recepción no registra ninguna salida ni despacho
   - **When** el administrador corrige sus datos y confirma la edición
   - **Then** el sistema actualiza la recepción, recalcula los kilogramos totales, los precios unitarios, el subtotal neto, el valor del impuesto y el total de la compra, y registra los valores anteriores y nuevos en el historial de auditoría

2. **Scenario**: Intento de edición de una recepción con movimientos de salida
   - **Given** que una recepción ya registra al menos una salida o despacho
   - **When** el administrador intenta editarla
   - **Then** el sistema bloquea la operación, informa que la recepción ya fue utilizada y conserva sus datos sin cambios

3. **Scenario**: Intento de edición por un usuario no autorizado
   - **Given** que un usuario sin rol de administrador intenta editar una recepción
   - **When** solicita guardar los cambios
   - **Then** el sistema rechaza la operación y conserva la recepción y el inventario sin cambios

---

### User Story 3 - Consultar el historial de recepciones de alimento (Priority: P2)

Como administrador, quiero consultar y filtrar el historial de recepciones de alimento, abrir el detalle de cada recepción y acceder a su edición cuando esté permitida, para mantener la trazabilidad de las entradas realizadas en la bodega central.

**Why this priority**: El historial permite verificar las recepciones registradas y localizar una entrega concreta sin convertir esta pantalla en una consulta de existencias, responsabilidad que corresponde al SPEC-023.

**Independent Test**: Se puede probar creando recepciones confirmadas en diferentes fechas y estados, ingresando al historial y verificando los indicadores del mes, la búsqueda por recepción o código de lote, los filtros por estado y periodo, la tabla de resultados, el modal de detalle y la disponibilidad de la acción de edición.

**Acceptance Scenarios**:

1. **Scenario**: Visualización del historial de recepciones
   - **Given** que existen recepciones de alimento registradas
   - **When** el administrador abre la pantalla de recepciones de alimentos
   - **Then** el sistema muestra para cada recepción el código de lote, alimento, cantidad de bultos, precio neto por bulto y peso por bulto, junto con las acciones `Detalles` y `Editar`

2. **Scenario**: Indicadores de recepciones del mes
   - **Given** que existen recepciones confirmadas cuya fecha de ingreso pertenece al mes calendario actual
   - **When** el administrador abre el historial
   - **Then** el sistema muestra la cantidad de recepciones del mes y la suma de sus kilogramos nominales totales

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
   - **When** el sistema abre el modal de detalle
   - **Then** muestra en modo de solo lectura el alimento, código de lote, fecha de ingreso, fecha de vencimiento, cantidad de bultos, peso por bulto, precio neto por bulto, porcentaje de impuesto, kilogramos totales, subtotal neto, valor del impuesto y total de la compra

6. **Scenario**: Acceso a la edición desde el historial
   - **Given** que el administrador consulta el historial
   - **When** selecciona `Editar` sobre una recepción
   - **Then** el sistema abre el modal de edición con los mismos campos del registro precargados si la recepción no tiene movimientos asociados; en caso contrario, bloquea la edición e informa la causa

7. **Scenario**: Historial sin resultados
   - **Given** que no existen recepciones o ninguna cumple los filtros aplicados
   - **When** el administrador consulta el historial
   - **Then** el sistema muestra un estado vacío informativo, mantiene los indicadores correspondientes y no presenta datos inventados

### Edge Cases

- **Edge case #1 - Cantidad y peso válidos individualmente, pero cuyo producto excede la capacidad numérica**

  - ¿Cómo maneja el sistema una recepción cuya cantidad de bultos y peso nominal por bulto son válidos de forma individual, pero al multiplicarlos generan una cantidad de kilogramos superior a la capacidad numérica admitida?  
    El sistema debe detectar el desbordamiento antes de crear la recepción, rechazar el registro e informar que la cantidad total calculada excede el límite permitido. No debe almacenar una cantidad truncada, negativa o diferente del resultado real, ni modificar el inventario.

- **Edge case #2 - Precio de compra por kilogramo con resultado decimal periódico**

  - ¿Cómo maneja el sistema una recepción cuyo precio neto de compra por bulto dividido entre el peso nominal del bulto produce un resultado decimal periódico?
    El sistema debe calcular el precio neto de compra por kilogramo aplicando una precisión y una regla de redondeo uniformes, definidas para todos los registros. No debe producir valores diferentes entre la recepción y el inventario ni truncar el resultado de manera arbitraria.

- **Edge case #3 - Fecha de vencimiento anterior a la fecha de ingreso**

  - ¿Cómo maneja el sistema una recepción cuya fecha de vencimiento es anterior o igual a la fecha de ingreso?
    El sistema debe rechazar el registro o la edición, indicar que la fecha de vencimiento debe ser posterior a la fecha de ingreso y no modificar el inventario.

- **Edge case #4 - Impuesto con resultado decimal**

  - ¿Cómo maneja el sistema un porcentaje de impuesto cuyo cálculo produce fracciones monetarias?
    El sistema debe calcular el valor del impuesto y el total de la compra aplicando una regla uniforme de precisión y redondeo. Los valores mostrados, almacenados y auditados deben coincidir.

## Requirements 

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro de recepciones de alimento exclusivamente a usuarios con rol de administrador.
- **FR-002**: Cada recepción DEBE referenciar un alimento activo del catálogo y registrar código de lote, fecha de ingreso, fecha de vencimiento, cantidad de bultos, peso nominal por bulto, precio neto de compra por bulto y porcentaje de impuesto.
- **FR-003**: El sistema DEBE validar los datos obligatorios y rechazar el registro cuando estén incompletos, sean inválidos o los valores calculados excedan los límites admitidos.
- **FR-004**: El sistema DEBE calcular los kilogramos nominales totales como `cantidad de bultos × peso nominal por bulto` y el precio neto de compra por kilogramo como `precio neto por bulto ÷ peso nominal por bulto`.
- **FR-005**: Cada entrega DEBE crear una recepción independiente, incluso si comparte código de lote con otra, conservando sus propios datos, precios de compra y fechas.
- **FR-006**: Al confirmar el registro, el sistema DEBE conservar y dejar disponibles para el módulo 3 el identificador de la recepción, el alimento, el tipo de alimento, el código de lote, el precio neto de compra por bulto, el precio neto de compra por kilogramo y el porcentaje de impuesto.
- **FR-007**: La modificación posterior del precio de otra recepción o del precio vigente de un tipo de alimento NO DEBE alterar el precio histórico asociado a un consumo ya registrado.
- **FR-008**: La información suministrada DEBE permitir al módulo 3 calcular el costo del alimento mediante la suma de `kilogramos consumidos de cada recepción × precio neto histórico de compra por kilogramo`. El valor total de una recepción NO DEBE tratarse como costo de un lote de aves.
- **FR-009**: Los cambios posteriores realizados sobre el alimento o su tipo en el catálogo NO DEBEN modificar los datos históricos de una recepción confirmada. La recepción DEBE conservar los datos comerciales necesarios como valores históricos de referencia.
- **FR-010**: El código de lote NO DEBE utilizarse como identificador único de la recepción. Cada recepción DEBE tener un identificador propio y puede compartir el código de lote con otras recepciones.
- **FR-011**: Al confirmar una recepción, el sistema DEBE generar un movimiento de entrada por sus kilogramos nominales totales, vincularlo con la recepción y dejarlo disponible como fuente para la consulta de inventario definida en el SPEC-023.
- **FR-012**: El sistema DEBE permitir editar una recepción exclusivamente a usuarios con rol de administrador y únicamente cuando no tenga movimientos de salida, despacho o consumo asociados.
- **FR-013**: El modal de edición DEBE presentar los mismos campos que el modal de registro, con los valores actuales precargados, y permitir corregir el alimento, código de lote, fecha de ingreso, fecha de vencimiento, cantidad de bultos, peso nominal por bulto, precio neto de compra por bulto y porcentaje de impuesto.
- **FR-014**: Al guardar una edición válida, el sistema DEBE recalcular los kilogramos nominales totales, el precio neto de compra por kilogramo, el subtotal neto, el valor del impuesto y el total de la compra, y actualizar las existencias afectadas.
- **FR-015**: Toda edición confirmada DEBE registrar en la auditoría los valores anteriores, los valores nuevos, el usuario responsable y la fecha y hora.
- **FR-016**: Los valores calculados de la recepción NO DEBEN ser editables directamente por el usuario.
- **FR-017**: El tipo de alimento DEBE obtenerse del alimento seleccionado y mostrarse sin permitir que el administrador lo modifique independientemente.
- **FR-018**: El sistema DEBE calcular el subtotal neto como `cantidad de bultos × precio neto de compra por bulto`.
- **FR-019**: El sistema DEBE calcular el valor del impuesto sobre el subtotal neto mediante `subtotal neto × porcentaje de impuesto ÷ 100`.
- **FR-020**: El sistema DEBE calcular el total de la compra como `subtotal neto + valor del impuesto`.
- **FR-021**: La fecha de vencimiento DEBE ser posterior a la fecha de ingreso tanto en el registro como en la edición.
- **FR-022**: El sistema DEBE permitir al administrador consultar el historial de recepciones de alimento sin modificar sus datos ni las existencias.
- **FR-023**: La tabla del historial DEBE mostrar código de lote, alimento, cantidad de bultos, precio neto de compra por bulto, peso nominal por bulto y las acciones `Detalles` y `Editar`.
- **FR-024**: El historial DEBE mostrar la cantidad de recepciones confirmadas del mes calendario actual y la suma de sus kilogramos nominales totales.
- **FR-025**: El sistema DEBE permitir buscar recepciones por su identificador único o código de lote y filtrar simultáneamente por estado y periodo de fecha de ingreso; el periodo inicial DEBE ser los últimos 30 días.
- **FR-026**: Toda recepción creada correctamente DEBE quedar en estado `confirmada`; el filtro de estado DEBE permitir como mínimo `Todos`, `Confirmada` y `Anulada`.
- **FR-027**: El modal de detalle DEBE mostrar en modo de solo lectura el alimento, código de lote, fecha de ingreso, fecha de vencimiento, cantidad de bultos, peso nominal por bulto, precio neto por bulto, porcentaje de impuesto, kilogramos nominales totales, subtotal neto, valor del impuesto y total de la compra.
- **FR-028**: La acción `Editar` del historial DEBE aplicar las mismas restricciones de edición definidas en FR-012 y NO DEBE permitir eludirlas.
- **FR-029**: Los modales de registro y edición DEBEN mostrar el precio neto por kilogramo como un valor calculado y no editable; este campo NO DEBE etiquetarse ni interpretarse como un segundo precio neto por bulto.

### Key Entities 

- **Alimento**: Representa un producto o referencia comercial del catálogo de alimentos, independientemente de sus compras y existencias.
  - **Atributos posibles**: nombre comercial, marca, descripción y estado.
  - **Relaciones**: pertenece a un tipo de alimento y puede estar asociado con cero o varias recepciones de alimento.
- **Recepción de alimento**: Representa una entrega concreta de un alimento que ingresa a la bodega central.
  - **Atributos posibles**: identificador, código de lote, estado, fecha de ingreso, fecha de vencimiento, cantidad de bultos, peso nominal por bulto, kilogramos nominales totales, precio neto de compra por bulto, precio neto de compra por kilogramo, subtotal neto, porcentaje de impuesto, valor del impuesto, total de la compra y cantidad inicial.
  - **Relaciones**: referencia un único alimento y una única bodega central, origina el movimiento de entrada, puede tener movimientos posteriores de salida o ajuste y se vincula con registros de auditoría. Conserva los precios y datos comerciales históricos requeridos por el módulo 3.
- **Tipo de alimento**: Representa la clasificación del alimento según la etapa productiva.
  - **Atributos posibles**: nombre, descripción y estado.
  - **Relaciones**: clasifica uno o varios alimentos registrados.
- **Bodega central**: Representa el inventario principal.
  - **Atributos posibles**: nombre, ubicación y estado.
  - **Relaciones**: recibe las recepciones de alimento y consolida los movimientos que afectan sus existencias.
- **Movimiento de inventario de alimento**: Representa una entrada, salida, consumo o ajuste que afecta las existencias de una recepción.
  - **Atributos posibles**: tipo de movimiento, cantidad en kilogramos, fecha y hora.
  - **Relaciones**: pertenece a una recepción de alimento, identifica al usuario responsable y permite obtener el saldo disponible sin modificar el registro histórico de la recepción.
- **Registro de auditoría**: Representa el historial de cambios sobre un registro.
  - **Atributos posibles**: acción, fecha y hora, valores anteriores y valores nuevos.
  - **Relaciones**: identifica al usuario responsable y a la recepción o movimiento afectado.
- **Historial de recepciones de alimento**: Representa la consulta de las entradas registradas en la bodega central.
  - **Datos mostrados**: indicadores del mes, filtros, resultados resumidos y acceso al detalle y a la edición condicionada.
  - **Comportamiento**: utiliza las recepciones registradas como fuente y no altera sus movimientos ni existencias.

## Success Criteria 

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los administradores puede completar un registro de alimento válido en menos de 3 minutos.
- **SC-002**: El 95 % de los registros confirmados muestra los cálculos, genera el movimiento de entrada y deja sus precios históricos de compra disponibles para el módulo 3 en un máximo de 2 segundos.
- **SC-003**: Al menos el 90 % de los usuarios completa correctamente el registro de alimento en el primer intento durante pruebas de usabilidad.
- **SC-004**: Al menos el 85 % de los administradores califica la experiencia de registro con 4 o más puntos sobre 5.
- **SC-005**: El 100 % de los registros y ediciones válidos calcula correctamente los kilogramos totales, el precio neto por kilogramo, el subtotal neto, el valor del impuesto y el total de la compra.
- **SC-006**: El 95 % de las búsquedas y filtros del historial presenta los resultados en un máximo de 2 segundos.
- **SC-007**: El 100 % de los detalles presenta valores consistentes con la recepción seleccionada y no modifica información.
- **SC-008**: El 100 % de los intentos de editar desde el historial una recepción con movimientos asociados es bloqueado.
