# Feature Specification: Consultar alimento por galpón por día

**Created**: 2026-09-05  

## User Scenarios & Testing 

### User Story 1 - Consultar el requerimiento diario de alimento para un galpón y su lote activo (Priority: P1)

Como trabajador u operario de granja, quiero consultar qué alimento le corresponde a mi galpón asignado en el día actual, visualizando la etapa en que se encuentra deducida por la edad del lote, el tipo de alimento, la cantidad y consumo necesario (en kilogramos y bultos), el saldo pendiente y la disponibilidad en bodega central en ambas unidades, para saber con exactitud qué y cuánto alimento suministrar a las aves en la jornada.

**Why this priority**: Es la tarea operativa fundamental del día que asegura la correcta nutrición de las aves, determinando el tipo de alimento y la cantidad precisa según la población viva actual y la edad del lote, facilitando el manejo práctico en kilogramos y bultos.

**Independent Test**: Se puede probar seleccionando un galpón asignado con lote activo y plan nutricional vigente, verificando que el sistema calcule la edad en días del lote, identifique la etapa correspondiente del plan nutricional, calcule el consumo diario en kilogramos y bultos (`población actual × ración individual ÷ 1.000 = kg`; `kg ÷ peso nominal del bulto = bultos`), y consulte la existencia disponible en bodega central en ambas unidades.

**Acceptance Scenarios**:

1. **Scenario**: Consulta exitosa de alimento para galpón con lote activo
   - **Given** que un trabajador autenticado selecciona un galpón asignado a su cargo con un lote activo cuya llave foránea referencia al galpón
   - **When** ingresa a la consulta de alimento por galpón por día para la fecha actual
   - **Then** el sistema presenta los datos del galpón (nombre, aforo máximo, estado) y del lote (nombre, población actual, edad en días)
   - **And** presenta la etapa en que se encuentra deducida por la edad del lote y el tipo de alimento asignado
   - **And** presenta la ración individual en gramos por ave, el consumo necesario en kilogramos netos y en bultos, el saldo pendiente del día y la disponibilidad en bodega central en kilogramos y bultos

2. **Scenario**: Cálculo y conversión del consumo necesario a bultos y kilogramos
   - **Given** un lote con población actual de 10.000 aves vivas, una ración diaria de 120 gramos por ave y un tipo de alimento con peso nominal de 40 kg por bulto
   - **When** el trabajador consulta el requerimiento del galpón para el día
   - **Then** el sistema calcula y muestra un consumo total de 1.200 kg netos
   - **And** muestra su equivalencia de 30 bultos (`1.200 kg ÷ 40 kg/bulto`)

3. **Scenario**: Determinación automática de la etapa según la edad del lote
   - **Given** un lote activo con fecha de ingreso hace 18 días (edad = 18 días) y un plan nutricional donde el rango de 11 a 24 días corresponde a la etapa de "Crecimiento" (o "Engorde", según aplique)
   - **When** el trabajador consulta el galpón
   - **Then** el sistema identifica automáticamente la etapa en que se encuentra el lote y asigna el tipo de alimento configurado para dicha etapa

4. **Scenario**: Consulta de disponibilidad en bodega central en kilogramos y bultos
   - **Given** que en la bodega central existen 4.000 kg netos de alimento vigente compatible con la etapa del galpón, con presentación de 40 kg por bulto
   - **When** el trabajador realiza la consulta
   - **Then** el sistema muestra una disponibilidad en bodega central de 4.000 kg netos y 100 bultos disponibles

5. **Scenario**: Visualización del saldo pendiente de la jornada
   - **Given** un consumo diario de 1.200 kg (30 bultos) y un suministro parcial registrado en el día de 400 kg (10 bultos)
   - **When** el trabajador consulta el galpón
   - **Then** el sistema muestra 400 kg (10 bultos) suministrados y un saldo pendiente de 800 kg (20 bultos) por suministrar en la jornada

6. **Scenario**: Galpón sin lote activo o sin aves vivas
   - **Given** un galpón que no tiene lotes registrados que lo referencien o cuya población actual de aves vivas es 0
   - **When** el trabajador consulta el galpón
   - **Then** el sistema muestra los datos del galpón, informa que no posee lote activo o aves vivas, y muestra el consumo necesario en 0 kg (0 bultos)

7. **Scenario**: Intento de consulta de un galpón no asignado al trabajador
   - **Given** que un trabajador intenta consultar un galpón no asignado a su cargo
   - **When** solicita acceder a la información de dicho galpón
   - **Then** el sistema deniega el acceso y muestra únicamente los galpones asignados bajo su responsabilidad

---

### User Story 2 - Identificar alertas de disponibilidad, cumplimiento y cambios del día (Priority: P2)

Como trabajador u operario de granja, quiero que el sistema me alerte visualmente si las existencias en bodega central son insuficientes para el requerimiento del día, si hay raciones pendientes por suministrar o si hubo un cambio en la dieta o etapa en la jornada, para tomar acciones preventivas inmediatas en beneficio del lote.

**Why this priority**: Evita desabastecimientos imprevistos, omisión de raciones o errores operativos por cambios nutricionales realizados durante la jornada.

**Independent Test**: Se puede probar configurando una bodega central con existencias inferiores al consumo diario del lote y verificando que la interfaz resalte las alertas correspondientes.

**Acceptance Scenarios**:

1. **Scenario**: Alerta por insuficiencia de alimento en bodega central
   - **Given** que el galpón requiere 1.200 kg (30 bultos) en el día y la bodega central solo dispone de 600 kg (15 bultos) compatibles con su etapa
   - **When** el trabajador consulta el galpón
   - **Then** el sistema muestra una alerta visual destacada indicando que el stock en bodega central es insuficiente para cubrir la ración del día

2. **Scenario**: Alerta por saldo diario pendiente de suministro
   - **Given** que el galpón registra un saldo pendiente mayor a 0 kg en la jornada
   - **When** el trabajador consulta el galpón
   - **Then** el sistema muestra un indicador visual de suministro pendiente

3. **Scenario**: Alerta por modificación de plan nutricional o etapa en el día
   - **Given** que el nutricionista modificó la ración o el tipo de alimento para la etapa del lote en la fecha actual
   - **When** el trabajador consulta el galpón
   - **Then** el sistema presenta una alerta visual informando la actualización realizada

---

### Edge Cases

- **¿Qué sucede si un galpón no tiene un lote activo o su población actual es 0?**
  - El sistema muestra los datos del galpón, indica que no posee lote activo o que la población viva es 0, calcula el consumo en 0 kg (0 bultos) y no genera alertas de déficit de alimento.

- **¿Qué sucede si la edad en días del lote no coincide con ningún rango configurado en el plan nutricional?**
  - El sistema muestra un mensaje informativo indicando que no existe un rango nutricional configurado para la edad actual del lote, mostrando el consumo en 0 kg sin bloquear la consulta.

- **¿Qué sucede si las existencias en bodega central para la etapa son inferiores al requerimiento del día?**
  - El sistema muestra las existencias reales disponibles en kg y bultos y emite una alerta visual destacada de desabastecimiento en bodega central.

- **¿Cómo se manejan los lotes de alimento vencidos en bodega central?**
  - El sistema los excluye automáticamente del cómputo de existencias disponibles para la consulta del galpón.

- **¿Qué sucede si el trabajador intenta consultar un galpón que no tiene asignado?**
  - El sistema bloquea el acceso a dicho galpón y permite visualizar únicamente los galpones asignados bajo su responsabilidad.

- **¿La consulta permite modificar existencias o datos de galpón/lote?**
  - No, la consulta es estrictamente de solo lectura y no altera existencias de inventario, registros de galpón ni datos del lote.

---

## Requirements 

### Functional Requirements

- **FR-001**: El sistema DEBE permitir la consulta de alimento por galpón por día a usuarios autenticados con rol de trabajador / operario de granja y administradores.
- **FR-002**: El sistema DEBE restringir la selección y visualización de galpones para el trabajador exclusivamente a aquellos que tenga formalmente asignados a su cargo.
- **FR-003**: Para el galpón consultado, el sistema DEBE obtener y presentar sus atributos: UUID, Nombre, Aforo máximo y Estado.
- **FR-004**: El sistema DEBE identificar el lote activo del galpón mediante la llave foránea que referencia al galpón consultado, seleccionando el lote con la fecha de ingreso más reciente.
- **FR-005**: Para el lote activo, el sistema DEBE utilizar sus atributos: UUID único, Nombre, Población actual (aves vivas) y Fecha de ingreso.
- **FR-006**: El sistema DEBE calcular la edad del lote en días a partir de la diferencia entre la fecha actual del sistema y la fecha de ingreso del lote.
- **FR-007**: El sistema DEBE determinar la etapa en que se encuentra el lote y el tipo de alimento aplicable comparando la edad en días del lote contra los rangos de días establecidos en el plan nutricional.
- **FR-008**: El sistema DEBE obtener la ración individual diaria programada en gramos por ave (`gr/ave`) correspondiente a la etapa y edad del lote.
- **FR-009**: El sistema DEBE calcular el consumo total necesario para el día en kilogramos netos mediante la fórmula: `(Población actual de aves vivas × Ración individual en gr/ave) ÷ 1.000`.
- **FR-010**: El sistema DEBE convertir el consumo diario requerido a bultos dividiendo el total de kilogramos entre el peso nominal por bulto del tipo de alimento asignado.
- **FR-011**: El sistema DEBE presentar en pantalla el consumo necesario del día mostrando tanto los kilogramos netos totales como la cantidad equivalente en bultos.
- **FR-012**: El sistema DEBE mostrar la cantidad acumulada de alimento suministrado en el día y el saldo pendiente por suministrar en kilogramos y bultos (`Consumo total requerido - Suministro acumulado del día`).
- **FR-013**: El sistema DEBE consultar y mostrar la disponibilidad total de alimento en bodega central compatible con la etapa del galpón, expresada simultáneamente en kilogramos netos y en bultos.
- **FR-014**: El sistema DEBE excluir del cálculo de disponibilidad en bodega central cualquier lote de alimento vencido o perteneciente a una etapa distinta a la del galpón.
- **FR-015**: El sistema DEBE emitir una alerta visual de desabastecimiento si la disponibilidad en bodega central para la etapa es menor al consumo necesario del día.
- **FR-016**: El sistema DEBE emitir una alerta visual de cumplimiento pendiente mientras exista un saldo de alimento pendiente por suministrar en la jornada.
- **FR-017**: El sistema DEBE emitir una alerta visual si en la fecha actual se registra un cambio en el tipo de alimento, en la ración diaria o en la etapa en que se encuentra el galpón.
- **FR-018**: La funcionalidad de consulta de alimento por galpón por día DEBE operar estrictamente en modo de solo lectura, sin alterar inventarios, parámetros nutricionales ni registros de galpones o lotes.

### Key Entities 

- **Galpón**: Representa la unidad física de producción avícola asignada al trabajador. No recibe ni almacena directamente el lote.
  - Atributos utilizados: `UUID único`, `Nombre`, `Aforo máximo`, `Estado`.
- **Lote**: Representa el grupo de aves registrado para un galpón mediante llave foránea.
  - Atributos utilizados: `UUID único`, `Nombre`, `Población actual` (aves vivas), `Fecha de ingreso` (para calcular la edad en días), `Llave foránea del galpón`.
- **Plan nutricional**: Definición establecida por el nutricionista que asocia rangos de días de edad del lote con una etapa (ej. iniciación, crecimiento, engorde, finalización), un tipo de alimento y una ración diaria en `gr/ave`.
- **Tipo de alimento**: Clasificación del alimento asociada a una etapa, que define el nombre y el peso nominal por bulto utilizado para la conversión.
- **Consumo diario requerido**: Cantidad total calculada para el lote en la fecha actual, expresada simultáneamente en kilogramos netos y en bultos equivalentes.
- **Saldo pendiente del día**: Diferencia calculada en kilogramos y bultos entre el consumo requerido y el alimento suministrado en la fecha.
- **Bodega central**: Inventario principal del cual se consulta la disponibilidad de existencias netas en kilogramos y bultos compatibles con la etapa del galpón.
- **Alerta de alimentación**: Indicador visual emitido ante déficit de existencias en bodega central, saldo pendiente de entrega o inconsistencias en la asignación nutricional.

---

## Success Criteria 

### Measurable Outcomes

- **SC-001**: El 100 % de las consultas realizadas por un trabajador muestra únicamente los galpones asignados a su cargo con su nombre, aforo y estado.
- **SC-002**: El 100 % de las consultas a un galpón con lote activo identifica correctamente el lote más reciente por fecha de ingreso mediante su llave foránea.
- **SC-003**: En el 100 % de los casos, la etapa en que se encuentra y el tipo de alimento se determinan con exactitud a partir del rango de edad en días del lote activo.
- **SC-004**: En el 100 % de las consultas, el consumo total diario se presenta en kilogramos y bultos utilizando el peso nominal por bulto del alimento asignado.
- **SC-005**: En el 100 % de las consultas, la disponibilidad en bodega central se muestra en kilogramos netos y bultos considerando únicamente existencias no vencidas y compatibles con la etapa.
- **SC-006**: El 100 % de los casos con disponibilidad en bodega central menor al requerimiento diario genera una alerta visible de desabastecimiento.
- **SC-007**: El 100 % de las modificaciones de ración, alimento o etapa realizadas en el día genera una alerta visual inmediata para el trabajador.
- **SC-008**: El 100 % de las operaciones de consulta se ejecuta sin modificar ningún dato en la base de datos (garantía de solo lectura).

---

## Out of Scope

- El registro o ingreso de recepciones de alimento en la bodega central (cubierto en SPEC-001).
- La consulta y listado maestro de galpones y lotes (cubierto en SPEC de consultar galpón-lote).
- La configuración o ajuste de planes nutricionales y rangos de edades por parte del nutricionista.
- El registro operativo de suministros físicos de alimento servido en el galpón o la captura de mortalidad de aves.
- El traslado físico, remisión o despacho de bultos desde bodega central hacia sub-bodegas.
- La creación, edición o administración de usuarios, galpones y lotes.
