# Feature Specification: Consultar inventario

**Created**: 2026-09-22
**Updated**: 2026-09-28

## User Scenarios & Testing

### User Story 1 - Consultar existencias de alimentos y medicamentos (Priority: P1)

Como administrador, quiero consultar en un mismo lugar las existencias disponibles de alimentos y medicamentos de la bodega central para conocer el inventario vigente y apoyar las decisiones de abastecimiento sin modificar sus movimientos.

**Why this priority**: La consulta consolidada permite al administrador conocer la disponibilidad real de los insumos registrados mediante recepciones y afectados posteriormente por salidas, consumos o ajustes. Esta responsabilidad debe permanecer separada del registro y la edición de recepciones.

**Independent Test**: Se puede probar registrando dos recepciones del mismo alimento y tipo, verificando que cada recepción conserve su trazabilidad y que el inventario muestre una sola existencia consolidada con la suma de sus kilogramos. Después se registra un consumo sobre una recepción y se comprueba que el stock consolidado disminuya sin modificar los datos históricos.

**Acceptance Scenarios**:

1. **Scenario**: Consulta conjunta con existencias disponibles
   - **Given** que existen recepciones y movimientos confirmados de alimentos y medicamentos en la bodega central
   - **When** el administrador consulta el inventario
   - **Then** el sistema muestra el resumen y la tabla `Stock de alimentos`, seguidos por el resumen y la tabla `Stock de medicamentos`, calculando las existencias a partir de los movimientos confirmados

2. **Scenario**: Consulta de existencias de alimentos
   - **Given** que existen alimentos con saldo disponible provenientes de una o varias recepciones
   - **When** el administrador consulta la sección de alimentos
   - **Then** el sistema muestra las tarjetas `Pre-inicio`, `Inicio`, `Broiler` y `Total alimento disponible` en kilogramos, además de una tabla consolidada con las columnas `Etapa`, `Alimento`, `Recepciones activas`, `Próximo vencimiento`, `Stock actual` y `Demanda`

3. **Scenario**: Consulta de existencias de medicamentos
   - **Given** que existen medicamentos con saldo disponible provenientes de una o varias recepciones
   - **When** el administrador consulta la sección de medicamentos
   - **Then** el sistema muestra las tarjetas `Cantidad de Frascos`, `Cantidad de Bolsas`, `Cantidad de Cajas` y `Total de Presentaciones`, además de la tabla con las columnas `Medicamento`, `Presentación`, `Cantidad`, `Contenido por presentación` y `Stock actual` en su unidad de medida

4. **Scenario**: Acumulación de varias recepciones del mismo alimento
   - **Given** que existen dos o más recepciones confirmadas con el mismo nombre normalizado de alimento y el mismo tipo de alimento
   - **When** el administrador consulta el inventario
   - **Then** el sistema conserva las recepciones como registros independientes y muestra una sola fila cuyo stock actual corresponde a la suma de sus entradas menos las salidas, consumos, vencimientos y ajustes confirmados

5. **Scenario**: Exclusión de recepciones vencidas o anuladas
   - **Given** que existen recepciones vencidas o anuladas con saldo registrado
   - **When** el administrador consulta el inventario
   - **Then** el sistema excluye esas cantidades de la existencia disponible y las identifica como no disponibles

6. **Scenario**: Insumo registrado sin existencias disponibles
   - **Given** que un alimento o medicamento previamente recibido tiene saldo disponible igual a cero
   - **When** el administrador consulta el inventario
   - **Then** el sistema muestra el insumo con stock actual igual a cero, sin confundirlo con un error de consulta

7. **Scenario**: Intento de consulta por un usuario no autorizado
   - **Given** que un usuario sin rol de administrador intenta consultar el inventario
   - **When** solicita acceder a la consulta
   - **Then** el sistema rechaza el acceso y no expone información de existencias

8. **Scenario**: Consulta sin modificar el inventario
   - **Given** que el administrador visualiza las existencias de alimentos y medicamentos
   - **When** navega entre las secciones o actualiza la consulta
   - **Then** el sistema vuelve a calcular la información vigente sin crear recepciones, movimientos ni ajustes, y sin modificar los saldos existentes

---

### User Story 2 - Consultar resumen del inventario en la pantalla de inicio (Priority: P2)

Como administrador, quiero visualizar en la pantalla de inicio el porcentaje de ocupación de la bodega central y las recepciones de alimento registradas recientemente para conocer rápidamente la situación del inventario y decidir si debo revisar su detalle.

**Why this priority**: El resumen permite conocer la ocupación general y las entradas recientes sin reemplazar la consulta detallada del inventario ni el registro de recepciones.

**Independent Test**: Se puede probar configurando la capacidad máxima de la bodega central, registrando su ocupación vigente y más de cinco recepciones confirmadas. La pantalla debe calcular el porcentaje de ocupación y mostrar únicamente las cinco recepciones de alimento más recientes.

**Acceptance Scenarios**:

1. **Scenario**: Cálculo del porcentaje de ocupación
   - **Given** que la bodega central tiene una capacidad máxima de almacenamiento y una ocupación actual expresadas en la misma unidad
   - **When** el administrador ingresa a la pantalla de inicio
   - **Then** el sistema muestra el porcentaje de ocupación calculado como `ocupación actual ÷ capacidad máxima × 100`

2. **Scenario**: Visualización de recepciones recientes de alimento
   - **Given** que existen más de cinco recepciones de alimento confirmadas
   - **When** el administrador ingresa a la pantalla de inicio
   - **Then** el sistema muestra las cinco recepciones más recientes ordenadas desde la más nueva, indicando código de lote, alimento, cantidad de bultos, precio por bulto, peso por bulto y fecha de ingreso

3. **Scenario**: Resumen sin alterar el inventario
   - **Given** que el administrador visualiza o actualiza el resumen del inventario
   - **When** el sistema recalcula sus indicadores
   - **Then** no crea ni modifica la bodega, los productos, las recepciones, los movimientos ni sus existencias

---

### User Story 3 - Consultar cobertura del requerimiento de alimento (Priority: P1)

Como administrador, quiero identificar desde la tabla de stock si las existencias cubren el requerimiento proyectado de alimento de los lotes activos y abrir su detalle, para conocer oportunamente cuándo existe cobertura total, parcial o nula.

**Why this priority**: La comparación permite convertir el inventario y los requerimientos nutricionales en una señal inmediata para la toma de decisiones de abastecimiento, sin reservar ni descontar alimento.

**Independent Test**: Se puede probar configurando cuatro alimentos con demanda y stock consolidado que produzcan respectivamente los estados `Cumple`, `Cobertura Parcial`, `Sin Cobertura` y `Sin Requerimiento`; cada estado debe mostrarse como un botón del color definido en la columna `Demanda` y abrir un modal que presente la cantidad exacta requerida en kilogramos.

**Acceptance Scenarios**:

1. **Scenario**: Cobertura total del requerimiento
   - **Given** una demanda consolidada mayor que `0 kg` y un stock actual mayor o igual a la demanda
   - **When** el administrador consulta el stock de alimentos
   - **Then** el sistema muestra un botón verde con la etiqueta `Cumple` en la columna "Demanda"

2. **Scenario**: Cobertura parcial del requerimiento
   - **Given** una demanda consolidada mayor que `0 kg` y un stock actual mayor que `0 kg` pero menor que la demanda
   - **When** el administrador consulta el stock de alimentos
   - **Then** el sistema muestra un botón naranja con la etiqueta `Cobertura Parcial` en la columna "Demanda"

3. **Scenario**: Ausencia de cobertura
   - **Given** una demanda consolidada mayor que `0 kg` y un stock actual igual a `0 kg`
   - **When** el administrador consulta el stock de alimentos
   - **Then** el sistema muestra un botón rojo con la etiqueta `Sin Cobertura` en la columna "Demanda"

4. **Scenario**: Alimento sin requerimiento vigente
   - **Given** una demanda consolidada igual a `0 kg`
   - **When** el administrador consulta el stock de alimentos
   - **Then** el sistema muestra un botón gris con la etiqueta `Sin Requerimiento` en la columna "Demanda"

5. **Scenario**: Consulta del detalle del requerimiento
   - **Given** que el administrador visualiza uno de los botones de la columna "Demanda"
   - **When** selecciona el botón
   - **Then** el sistema abre el modal `Detalles requerimiento de alimento` y muestra en modo de solo lectura `Alimento`, `Etapa`, `Stock actual` y `Demanda`, indicando en este último campo la cantidad exacta requerida en kilogramos y permitiendo cerrar el modal mediante el icono de cierre o el botón `Cancelar`

6. **Scenario**: Medicamentos sin demanda nutricional
   - **Given** que el administrador consulta el stock de medicamentos
   - **When** se presenta la tabla de medicamentos
   - **Then** el sistema no muestra una columna de demanda ni estados de cobertura nutricional

7. **Scenario**: Consulta de cobertura sin movimientos de inventario
   - **Given** que el administrador abre un detalle de requerimiento
   - **When** consulta o cierra el modal
   - **Then** el sistema no reserva alimento, no descuenta existencias y no crea movimientos de inventario

### Edge Cases

- **Edge case #1 - Movimientos concurrentes durante la consulta**

  - ¿Qué sucede si se confirma una entrada, salida, consumo o ajuste mientras el administrador tiene abierta la consulta?
    El sistema debe obtener una vista consistente de los movimientos confirmados al momento de ejecutar o actualizar la consulta. No debe combinar saldos calculados en momentos diferentes dentro de una misma respuesta.

- **Edge case #2 - Recepciones del mismo alimento con diferentes pesos por bulto**

  - ¿Cómo presenta el sistema varias recepciones del mismo alimento con pesos por bulto diferentes?
    El sistema debe sumar sus saldos en kilogramos dentro de la misma existencia de alimento. La cantidad de bultos y el peso por bulto permanecen en el historial de cada recepción y no se suman ni se promedian en la tabla consolidada.

- **Edge case #3 - Identificación de recepciones del mismo alimento**

  - ¿Cómo determina el sistema que dos recepciones corresponden al mismo alimento?
    El sistema debe comparar el nombre normalizado y el tipo de alimento. La normalización debe ignorar diferencias de mayúsculas, minúsculas y espacios externos, pero no debe unir nombres con distinta escritura; cada diferencia restante se considera otro alimento.

- **Edge case #4 - Vencimiento de una recepción dentro de una existencia consolidada**

  - ¿Cómo descuenta el sistema una recepción vencida cuando el mismo alimento tiene otras recepciones vigentes?
    El sistema debe conservar el saldo por recepción, retirar únicamente el saldo remanente de la recepción vencida y recalcular el stock consolidado. Las salidas y consumos deben asignarse primero a la recepción vigente con fecha de vencimiento más próxima.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir la consulta del inventario exclusivamente a usuarios autenticados con rol de administrador.
- **FR-002**: El sistema DEBE presentar en una misma funcionalidad dos secciones diferenciadas: existencias de alimentos y existencias de medicamentos.
- **FR-003**: La existencia disponible DEBE calcularse como la suma de entradas confirmadas menos salidas, consumos, vencimientos y anulaciones confirmadas, incorporando los ajustes con el signo que corresponda y conservando el saldo individual de cada recepción.
- **FR-004**: La tabla `Stock de alimentos` DEBE mostrar las columnas `Etapa`, `Alimento`, `Recepciones activas`, `Próximo vencimiento`, `Stock actual` y `Demanda`; el stock actual debe expresarse en kilogramos.
- **FR-005**: El sistema DEBE consolidar en una sola existencia todas las recepciones con el mismo nombre normalizado y tipo de alimento, incluso cuando tengan diferente cantidad de bultos o peso por bulto; estos datos deben permanecer disponibles únicamente en el historial de recepciones.
- **FR-006**: El sistema DEBE mostrar tarjetas con los kilogramos disponibles de las etapas `Pre-inicio`, `Inicio` y `Broiler`, además de la tarjeta `Total alimento disponible` con la suma de todas las etapas.
- **FR-007**: La tabla "Stock de medicamentos" DEBE mostrar exactamente las columnas `Medicamento`, `Presentación`, `Cantidad`, `Contenido por presentación` y `Stock actual`, conservando la unidad de medida correspondiente.
- **FR-008**: El sistema DEBE agrupar los medicamentos por medicamento, presentación, contenido por presentación y unidad de medida, sin sumar cantidades expresadas en unidades o contenidos incompatibles.
- **FR-009**: El sistema DEBE mostrar con stock igual a cero los alimentos y medicamentos previamente recibidos cuyo saldo se haya agotado.
- **FR-010**: El sistema DEBE excluir de la existencia disponible los saldos de recepciones vencidas o anuladas e identificarlos como no disponibles.
- **FR-011**: Las salidas y consumos de alimento DEBEN asignarse a recepciones específicas siguiendo el criterio FEFO, utilizando primero el saldo vigente con fecha de vencimiento más próxima.
- **FR-012**: La columna `Demanda` DEBE mostrar un botón cuyo texto y color representen el estado obtenido al comparar la demanda con el stock consolidado del mismo alimento y tipo: `Cumple`, `Cobertura Parcial`, `Sin Cobertura` o `Sin Requerimiento`.
- **FR-013**: Al seleccionar el botón de la columna `Demanda`, el sistema DEBE abrir el modal `Detalles requerimiento de alimento` con los campos de solo lectura `Alimento`, `Etapa`, `Stock actual` y `Demanda`; este último DEBE mostrar la cantidad exacta requerida en kilogramos.

### Key Entities

- **Bodega central**: Representa el inventario principal cuyas existencias consulta el administrador.
  - **Atributos utilizados**: capacidad máxima de almacenamiento, ocupación actual y unidad de capacidad.
  - **Relaciones**: recibe las recepciones de alimentos y medicamentos y reúne los movimientos que determinan sus saldos disponibles y su ocupación.
- **Existencia de alimento**: Representa el resultado de consolidar los movimientos confirmados de alimento.
  - **Clave de agrupación**: nombre normalizado del alimento y tipo de alimento.
  - **Datos mostrados**: etapa, alimento, cantidad de recepciones activas, próximo vencimiento, stock actual en kilogramos y estado de demanda.
  - **Origen**: se calcula a partir de las recepciones de alimento definidas en el SPEC-001 y sus movimientos asociados.
- **Existencia de medicamento**: Representa el resultado de consolidar los movimientos confirmados de medicamento.
  - **Datos mostrados**: medicamento, presentación, cantidad física, contenido por presentación y stock actual en su unidad de medida.
  - **Origen**: se calcula a partir de las recepciones de medicamento definidas en el SPEC-002 y sus movimientos asociados.
- **Recepción de alimento**: Representa una entrega de alimento que origina un movimiento de entrada y conserva sus datos históricos.
- **Recepción de medicamento**: Representa una entrega de medicamento que origina un movimiento de entrada y conserva sus datos históricos.
- **Movimiento de inventario**: Representa una entrada, salida, consumo, vencimiento, anulación o ajuste confirmado que incrementa o disminuye el saldo de una recepción.
  - **Atributos utilizados**: tipo de movimiento, cantidad normalizada, unidad, estado, fecha y hora.
  - **Relaciones**: referencia una recepción de alimento o de medicamento, conserva el saldo individual necesario para controlar vencimientos y permite calcular la existencia consolidada sin modificar su historial.
- **Resumen de inventario**: Representa la información agregada mostrada en la pantalla de inicio del administrador.
  - **Datos mostrados**: porcentaje de ocupación y las cinco recepciones de alimento confirmadas más recientes.
- **Cobertura de demanda de alimento**: Representa la comparación de solo lectura entre el stock actual y la demanda proyectada consolidada proveniente del SPEC-022.
  - **Clave de agrupación**: nombre normalizado del alimento y tipo de alimento.
  - **Datos mostrados**: botón de estado en la tabla y, en el modal, alimento, etapa, stock actual y cantidad exacta de la demanda.
  - **Estados**: `Cumple`, `Cobertura Parcial`, `Sin Cobertura` y `Sin Requerimiento`.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los administradores pueden identificar las existencias disponibles de un alimento y un medicamento en menos de 2 minutos.
- **SC-002**: El 100 % de las existencias mostradas coincide con la consolidación de los movimientos confirmados y excluye recepciones vencidas o anuladas.
- **SC-003**: El 100 % de las consultas acumula correctamente las recepciones del mismo alimento y tipo en kilogramos, aunque tengan pesos por bulto diferentes, y mantiene separados los medicamentos con unidades incompatibles.
- **SC-004**: El 95 % de las consultas presenta ambas secciones en un máximo de 2 segundos.
- **SC-005**: El 100 % de las consultas se ejecuta sin modificar recepciones, movimientos ni saldos de inventario.
