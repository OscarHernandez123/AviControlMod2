# Feature Specification: Consultar inventario

**Created**: 2026-09-22
**Updated**: 2026-09-25

## User Scenarios & Testing

### User Story 1 - Consultar existencias de alimentos y medicamentos (Priority: P1)

Como administrador, quiero consultar en un mismo lugar las existencias disponibles de alimentos y medicamentos de la bodega central para conocer el inventario vigente y apoyar las decisiones de abastecimiento sin modificar sus movimientos.

**Why this priority**: La consulta consolidada permite al administrador conocer la disponibilidad real de los insumos registrados mediante recepciones y afectados posteriormente por salidas, consumos o ajustes. Esta responsabilidad debe permanecer separada del registro y la edición de recepciones.

**Independent Test**: Se puede probar registrando recepciones y movimientos confirmados de alimentos y medicamentos, ingresando como administrador y verificando que las tarjetas de resumen y las dos tablas muestren los saldos disponibles consolidados en su unidad correspondiente, sin crear ni modificar datos.

**Acceptance Scenarios**:

1. **Scenario**: Consulta conjunta con existencias disponibles
   - **Given** que existen recepciones y movimientos confirmados de alimentos y medicamentos en la bodega central
   - **When** el administrador consulta el inventario
   - **Then** el sistema muestra primero el resumen y la tabla "Stock de alimentos"
   - **And** muestra a continuación el resumen y la tabla "Stock de medicamentos"
   - **And** calcula todas las existencias a partir de sus movimientos confirmados

2. **Scenario**: Consulta de existencias de alimentos
   - **Given** que existen alimentos con saldo disponible provenientes de una o varias recepciones
   - **When** el administrador consulta la sección de alimentos
   - **Then** el sistema muestra las tarjetas `Pre-inicio`, `Inicio`, `Broiler` y `Total alimento disponible`, expresadas en kilogramos
   - **And** presenta en la tabla las columnas `Etapa`, `Alimento`, `Cantidad`, `Peso por bulto`, `Stock actual` y `Demanda`
   - **And** expresa `Cantidad` en bultos y `Stock actual` en kilogramos

3. **Scenario**: Consulta de existencias de medicamentos
   - **Given** que existen medicamentos con saldo disponible provenientes de una o varias recepciones
   - **When** el administrador consulta la sección de medicamentos
   - **Then** el sistema muestra las tarjetas `Cantidad de Frascos`, `Cantidad de Bolsas`, `Cantidad de Cajas` y `Total de Presentaciones`
   - **And** presenta en la tabla las columnas `Medicamento`, `Presentación`, `Cantidad`, `Contenido por presentación` y `Stock actual`
   - **And** expresa el stock actual en la unidad de medida correspondiente (`gr`, `ml` o `unidades`)

4. **Scenario**: Producto registrado sin existencias disponibles
   - **Given** que un alimento o medicamento del catálogo no tiene recepciones confirmadas con saldo disponible
   - **When** el administrador consulta el inventario
   - **Then** el sistema muestra el producto con existencia disponible igual a cero, sin confundirlo con un error de consulta

5. **Scenario**: Exclusión de recepciones vencidas o anuladas
   - **Given** que existen recepciones vencidas o anuladas con saldo registrado
   - **When** el administrador consulta el inventario
   - **Then** el sistema excluye esas cantidades de la existencia disponible y las identifica como no disponibles

6. **Scenario**: Intento de consulta por un usuario no autorizado
   - **Given** que un usuario sin rol de administrador intenta consultar el inventario
   - **When** solicita acceder a la consulta
   - **Then** el sistema rechaza el acceso y no expone información de existencias

7. **Scenario**: Consulta sin modificar el inventario
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

**Independent Test**: Se puede probar configurando cuatro filas de alimento con demanda y stock que produzcan respectivamente los estados `Cumple`, `Cobertura Parcial`, `Sin Cobertura` y `Sin Requerimiento`; cada estado debe mostrarse como un botón del color definido y abrir el modal con los datos de la fila seleccionada.

**Acceptance Scenarios**:

1. **Scenario**: Cobertura total del requerimiento
   - **Given** una demanda consolidada mayor que `0 kg`
   - **And** un stock actual mayor o igual a la demanda
   - **When** el administrador consulta el stock de alimentos
   - **Then** el sistema muestra un botón verde con la etiqueta `Cumple` en la columna "Demanda"

2. **Scenario**: Cobertura parcial del requerimiento
   - **Given** una demanda consolidada mayor que `0 kg`
   - **And** un stock actual mayor que `0 kg` pero menor que la demanda
   - **When** el administrador consulta el stock de alimentos
   - **Then** el sistema muestra un botón naranja con la etiqueta `Cobertura Parcial` en la columna "Demanda"

3. **Scenario**: Ausencia de cobertura
   - **Given** una demanda consolidada mayor que `0 kg`
   - **And** un stock actual igual a `0 kg`
   - **When** el administrador consulta el stock de alimentos
   - **Then** el sistema muestra un botón rojo con la etiqueta `Sin Cobertura` en la columna "Demanda"

4. **Scenario**: Alimento sin requerimiento vigente
   - **Given** una demanda consolidada igual a `0 kg`
   - **When** el administrador consulta el stock de alimentos
   - **Then** el sistema muestra un botón gris con la etiqueta `Sin Requerimiento` en la columna "Demanda"

5. **Scenario**: Consulta del detalle del requerimiento
   - **Given** que el administrador visualiza uno de los botones de la columna "Demanda"
   - **When** selecciona el botón
   - **Then** el sistema abre el modal "Detalles requerimiento de alimento"
   - **And** muestra en modo de solo lectura los campos `Alimento`, `Etapa`, `Stock actual` y `Demanda`
   - **And** permite cerrarlo mediante el icono de cierre o el botón "Cancelar"

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

- **Edge case #2 - Alimentos con diferentes pesos nominales por bulto**

  - ¿Cómo presenta el sistema varias recepciones del mismo alimento con pesos nominales por bulto diferentes?
    El sistema debe agruparlas por alimento, tipo de alimento y peso nominal por bulto. No debe sumar como una sola cantidad de bultos presentaciones con pesos nominales diferentes; el total general de alimento sí puede consolidarse en kilogramos.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir la consulta del inventario exclusivamente a usuarios autenticados con rol de administrador.
- **FR-002**: El sistema DEBE presentar en una misma funcionalidad dos secciones diferenciadas: existencias de alimentos y existencias de medicamentos.
- **FR-003**: La existencia disponible DEBE calcularse exclusivamente a partir de movimientos confirmados de entrada, salida, consumo y ajuste vinculados con las recepciones de la bodega central.
- **FR-004**: La tabla "Stock de alimentos" DEBE mostrar exactamente las columnas `Etapa`, `Alimento`, `Cantidad`, `Peso por bulto`, `Stock actual` y `Demanda`; `Cantidad` se expresa en bultos y `Stock actual` en kilogramos.
- **FR-005**: El sistema DEBE agrupar los alimentos por etapa, alimento y peso nominal por bulto, sin mezclar en una misma cantidad de bultos recepciones con pesos nominales diferentes.
- **FR-006**: El sistema DEBE mostrar tarjetas con los kilogramos disponibles de las etapas `Pre-inicio`, `Inicio` y `Broiler`, además de la tarjeta `Total alimento disponible` con la suma de todas las etapas.
- **FR-007**: La tabla "Stock de medicamentos" DEBE mostrar exactamente las columnas `Medicamento`, `Presentación`, `Cantidad`, `Contenido por presentación` y `Stock actual`, conservando la unidad de medida correspondiente.
- **FR-008**: El sistema DEBE agrupar los medicamentos por medicamento, presentación y unidad base, sin sumar cantidades expresadas en unidades base incompatibles.
- **FR-009**: El sistema DEBE mostrar los productos activos del catálogo sin saldo disponible con existencia igual a cero.
- **FR-010**: El sistema DEBE excluir de la existencia disponible los saldos de recepciones vencidas o anuladas e identificarlos como no disponibles.

### Key Entities

- **Bodega central**: Representa el inventario principal cuyas existencias consulta el administrador.
  - **Atributos utilizados**: capacidad máxima de almacenamiento, ocupación actual y unidad de capacidad.
  - **Relaciones**: recibe las recepciones de alimentos y medicamentos y reúne los movimientos que determinan sus saldos disponibles y su ocupación.
- **Existencia de alimento**: Representa el resultado de consolidar los movimientos confirmados de alimento.
  - **Datos mostrados**: etapa, alimento, cantidad de bultos, peso nominal por bulto, stock actual en kilogramos y estado de demanda.
  - **Origen**: se calcula a partir de las recepciones de alimento definidas en el SPEC-001 y sus movimientos asociados.
- **Existencia de medicamento**: Representa el resultado de consolidar los movimientos confirmados de medicamento.
  - **Datos mostrados**: medicamento, presentación, cantidad física, contenido por presentación y stock actual en su unidad de medida.
  - **Origen**: se calcula a partir de las recepciones de medicamento definidas en el SPEC-002 y sus movimientos asociados.
- **Recepción de alimento**: Representa una entrega de alimento que origina un movimiento de entrada y conserva sus datos históricos.
- **Recepción de medicamento**: Representa una entrega de medicamento que origina un movimiento de entrada y conserva sus datos históricos.
- **Movimiento de inventario**: Representa una entrada, salida, consumo o ajuste confirmado que incrementa o disminuye el saldo de una recepción.
  - **Atributos utilizados**: tipo de movimiento, cantidad normalizada, unidad, estado, fecha y hora.
  - **Relaciones**: referencia una recepción de alimento o de medicamento y permite calcular la existencia sin modificar su historial.
- **Resumen de inventario**: Representa la información agregada mostrada en la pantalla de inicio del administrador.
  - **Datos mostrados**: porcentaje de ocupación y las cinco recepciones de alimento confirmadas más recientes.
- **Cobertura de demanda de alimento**: Representa la comparación de solo lectura entre el stock actual y la demanda proyectada consolidada proveniente del SPEC-022.
  - **Clave de agrupación**: etapa, alimento y peso nominal por bulto.
  - **Datos mostrados**: alimento, etapa, stock actual, demanda y estado de cobertura.
  - **Estados**: `Cumple`, `Cobertura Parcial`, `Sin Cobertura` y `Sin Requerimiento`.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los administradores pueden identificar las existencias disponibles de un alimento y un medicamento en menos de 2 minutos.
- **SC-002**: El 100 % de las existencias mostradas coincide con la consolidación de los movimientos confirmados y excluye recepciones vencidas o anuladas.
- **SC-003**: El 100 % de las consultas separa correctamente alimentos con pesos nominales diferentes y medicamentos con unidades base incompatibles.
- **SC-004**: El 95 % de las consultas presenta ambas secciones en un máximo de 2 segundos.
- **SC-005**: El 100 % de las consultas se ejecuta sin modificar recepciones, movimientos ni saldos de inventario.
