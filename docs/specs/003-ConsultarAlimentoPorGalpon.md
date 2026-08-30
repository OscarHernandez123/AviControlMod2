# Feature Specification: Consulta de alimento por galpón

**Created**: 2026-08-30  

## User Scenarios & Testing 

### User Story 1 - Consultar requerimiento de alimento diario del galpón asignado (Priority: P1)

Como trabajador de granja, quiero consultar la información de alimentación del día para mis galpones asignados, visualizando el tipo de alimento, la etapa de crianza, el cálculo de consumo necesario, el saldo pendiente del día y la disponibilidad en bodega central, para garantizar la nutrición exacta y oportuna de las aves bajo mi cuidado.

**Why this priority**: Es la operación diaria crítica que determina qué y cuánto alimento debe suministrarse a las aves vivas en cada galpón, evitando desnutrición, desabastecimiento o errores de suministro en etapas inadecuadas.

**Independent Test**: Se puede probar seleccionando un galpón asignado con aves vivas y plan de ración activo, verificando que la vista presente la etapa actual, el tipo de alimento, la multiplicación detallada (`aves vivas × ración individual = consumo total diario en kg`), el saldo pendiente por suministrar y la disponibilidad de alimento compatible en bodega central.

**Acceptance Scenarios**:

1. **Scenario**: Consulta exitosa del requerimiento diario para un galpón asignado
   - **Given** que un trabajador autenticado tiene asignado el galpón seleccionado, el cual cuenta con aves vivas y un plan nutricional vigente para la fecha actual
   - **When** el trabajador consulta la información de alimento para la fecha actual
   - **Then** el sistema presenta el nombre/código del galpón, etapa de crianza actual, tipo de alimento correspondiente, cantidad de aves vivas, ración individual programada (gr/ave), consumo total del día en kg, saldo pendiente por suministrar y la cantidad disponible en bodega central para esa etapa

2. **Scenario**: Visualización del desglose del cálculo de consumo
   - **Given** un galpón con 10.000 aves vivas y una ración programada de 120 gramos por ave para el día
   - **When** el trabajador realiza la consulta del galpón
   - **Then** el sistema muestra explícitamente la operación `10.000 aves × 120 gr/ave = 1.200 kg` como consumo total necesario para el día

3. **Scenario**: Visualización del saldo pendiente de consumo diario
   - **Given** un requerimiento diario de 1.200 kg y un suministro parcial registrado previamente de 400 kg en el día
   - **When** el trabajador consulta el estado del galpón
   - **Then** el sistema muestra que se han suministrado 400 kg y que existe un saldo pendiente de 800 kg por entregar en la jornada

4. **Scenario**: Consulta de disponibilidad de alimento en bodega central
   - **Given** que la bodega central cuenta con 5.000 kg vigentes de alimento compatible con la etapa actual del galpón
   - **When** el trabajador consulta el galpón
   - **Then** el sistema muestra una disponibilidad en bodega central de 5.000 kg para dicho tipo de alimento y etapa

5. **Scenario**: Intento de consultar un galpón no asignado al trabajador
   - **Given** que un trabajador intenta consultar un galpón que no está asignado a su cargo
   - **When** solicita acceder a la información de dicho galpón
   - **Then** el sistema deniega el acceso, restringe la visualización y muestra únicamente los galpones bajo su responsabilidad

6. **Scenario**: Intento de consulta por un usuario no autenticado o sin rol autorizado
   - **Given** un usuario que no ha iniciado sesión o que no cuenta con un rol con permisos de consulta operativa
   - **When** intenta consultar la información de alimentación
   - **Then** el sistema bloquea la consulta y redirige a autenticación

---

### User Story 2 - Consultar historial de requerimientos y suministros diarios (Priority: P2)

Como trabajador de granja, quiero consultar el historial de requerimientos de alimentación de días anteriores para mis galpones asignados, para dar seguimiento al comportamiento de consumo y verificar el cumplimiento de jornadas previas.

**Why this priority**: Permite al trabajador y a los supervisores auditar el cumplimiento diario, comprobar si quedaron saldos pendientes y contextualizar el estado nutricional del lote.

**Independent Test**: Se puede probar seleccionando un galpón asignado y filtrando fechas anteriores, verificando que se muestren los registros históricos inalterados con etapa, tipo de alimento, aves vivas, ración programada, consumo total calculado, cantidad suministrada y saldo final de cada día.

**Acceptance Scenarios**:

1. **Scenario**: Consulta de historial de días anteriores de un galpón asignado
   - **Given** que existen registros diarios de alimentación de fechas pasadas para un galpón asignado al trabajador
   - **When** el trabajador selecciona una fecha o rango de fechas anteriores
   - **Then** el sistema muestra la lista histórica con la etapa en esa fecha, tipo de alimento, población de aves, ración programada, consumo total calculado, cantidad servida y saldo de cierre de cada jornada

2. **Scenario**: Consulta de una fecha histórica sin registros de alimentación
   - **Given** que el trabajador selecciona una fecha anterior en la que el galpón se encontraba vacío o sin lote activo
   - **When** solicita la consulta de dicha fecha
   - **Then** el sistema informa claramente que no existen registros de alimentación para la fecha seleccionada sin producir errores

---

### User Story 3 - Visualización de alertas de desabastecimiento, suministros pendientes y cambios nutricionales (Priority: P2)

Como trabajador de granja, quiero recibir alertas visuales inmediatas y claras ante insuficiencia de alimento en bodega central, consumos diarios no cumplidos o modificaciones en la dieta/etapa, para reaccionar oportunamente y proteger la supervivencia y bienestar de las aves.

**Why this priority**: La alimentación avícola exige precisión y continuidad; la falta de stock, la omisión de raciones o cambios no advertidos en el alimento comprometen directamente la salud y supervivencia del lote.

**Independent Test**: Se puede probar configurando condiciones de inventario insuficiente en bodega central, saldos pendientes de ración y modificaciones en la ración/etapa por parte del nutricionista, verificando que la interfaz resalte prominentemente las alertas correspondientes.

**Acceptance Scenarios**:

1. **Scenario**: Alerta visual por desabastecimiento en bodega central
   - **Given** que el galpón requiere 1.200 kg para el día y la bodega central solo dispone de 800 kg de alimento compatible con la etapa
   - **When** el trabajador consulta el galpón
   - **Then** el sistema muestra una alerta visual destacada indicando déficit de inventario en bodega central para cubrir la ración del día

2. **Scenario**: Alerta visual por suministro diario incompleto / pendiente
   - **Given** que el galpón presenta un saldo pendiente por suministrar durante la jornada
   - **When** el trabajador consulta el galpón
   - **Then** el sistema resalta el estado pendiente de suministro para recordar la ración faltante

3. **Scenario**: Alerta visual por cambio en tipo de alimento, ración o etapa
   - **Given** que el nutricionista ha modificado recientemente la ración diaria, el tipo de alimento o la etapa de crianza del galpón
   - **When** el trabajador realiza la consulta
   - **Then** el sistema presenta una alerta/notificación visual destacada informando sobre el cambio aplicado para evitar errores operativos en la granja

---

### Edge Cases

- **Galpón sin aves vivas (población = 0 por mortalidad o vacío sanitario)**: El sistema debe calcular un consumo de 0 kg, indicar el estado inactivo o en vacío sanitario del galpón y deshabilitar cálculos de ración sin generar fallos.
- **Galpón sin plan nutricional programado para el día**: Si el nutricionista no ha fijado una ración para la fecha, el sistema debe alertar la ausencia de configuración nutricional y mostrar consumo requerido en 0 kg.
- **Bodega central con existencias en cero o insuficientes**: El sistema debe reflejar exactamente las existencias reales (incluso si es 0 kg) y disparar inmediatamente la alerta de desabastecimiento.
- **Existencias de alimento vencido en bodega central**: El sistema no debe contabilizar como disponible ningún lote de alimento vencido o en cuarentena para la consulta del galpón.
- **Existencias de alimento de etapas incompatibles**: El sistema solo debe totalizar y mostrar como disponible el alimento cuya etapa de crianza coincida exactamente con la etapa actual del galpón consultado.
- **Mortalidad registrada durante el día**: Si se registra una baja de aves vivas en la fecha actual, el cálculo del consumo total diario y el saldo pendiente deben actualizarse en tiempo real o al refrescar la consulta.
- **Modificación de ración por el nutricionista durante el día**: La consulta debe reflejar el nuevo requerimiento de inmediato y marcar la alerta de cambio nutricional para alertar al trabajador.
- **Suministros que igualan o superan el requerimiento**: Si la cantidad suministrada cubre el consumo total diario, el saldo pendiente debe ser 0 kg y el estado debe figurar como ración completada.
- **Trabajador sin galpones asignados**: Si el usuario no tiene galpones asociados en su perfil, el sistema debe mostrar un mensaje amigable indicando la situación sin exponer datos de otros galpones.
- **Naturaleza de solo lectura**: Ninguna interacción dentro de esta pantalla debe permitir modificar inventarios, alterar raciones ni cambiar datos del galpón.

---

## Requirements 

### Functional Requirements

- **FR-001**: El sistema DEBE permitir la consulta de alimento por galpón exclusivamente a usuarios autenticados con rol de trabajador (y roles de supervisión/administración).
- **FR-002**: El sistema DEBE restringir la visualización y consulta de galpones para el trabajador exclusivamente a aquellos que tenga asignados a su cargo.
- **FR-003**: Para cada galpón asignado, la consulta DEBE mostrar el nombre o identificador del galpón, la etapa de crianza actual, la edad del lote (en días o semanas) y el tipo de alimento correspondiente.
- **FR-004**: El sistema DEBE mostrar la población actual de aves vivas del galpón, considerando las disminuciones por mortalidad registradas hasta el momento de la consulta.
- **FR-005**: El sistema DEBE mostrar la ración individual diaria programada por el nutricionista expresada en gramos por ave (`gr/ave`).
- **FR-006**: El sistema DEBE mostrar el desglose explícito de la multiplicación del cálculo de consumo diario requerido: `Aves vivas × Ración individual (gr/ave) = Consumo total diario requerido (kg)`.
- **FR-007**: El sistema DEBE mostrar la cantidad acumulada de alimento suministrado en el día y el saldo pendiente por suministrar en kilogramos (`Consumo total requerido - Suministro acumulado del día`).
- **FR-008**: El sistema DEBE consultar y mostrar la cantidad total disponible en kilogramos netos en la bodega central correspondiente al tipo de alimento compatible con la etapa actual del galpón.
- **FR-009**: El sistema DEBE excluir del cálculo de disponibilidad de bodega central cualquier lote de alimento que se encuentre vencido o no apto.
- **FR-010**: El sistema DEBE emitir una alerta visual de desabastecimiento cuando la disponibilidad en bodega central sea menor al consumo total diario requerido por el galpón.
- **FR-011**: El sistema DEBE emitir una alerta visual de cumplimiento pendiente mientras exista un saldo pendiente por suministrar durante la jornada diaria.
- **FR-012**: El sistema DEBE emitir una alerta visual destacada cuando se registre un cambio reciente en el tipo de alimento, en la ración asignada por el nutricionista o en la etapa de crianza del galpón.
- **FR-013**: El sistema DEBE permitir al trabajador consultar el historial de requerimientos y consumos de días anteriores para sus galpones asignados.
- **FR-014**: En la consulta de historial de días anteriores, el sistema DEBE mostrar fecha, etapa de crianza, tipo de alimento, aves vivas, ración programada, consumo calculado, alimento suministrado y saldo final de la jornada.
- **FR-015**: La funcionalidad de consulta de alimento por galpón DEBE ser estrictamente de solo lectura, sin alterar inventarios, parámetros nutricionales ni registros de galpones.

### Key Entities 

- **Galpón**: Unidad productiva asignada al trabajador, con atributos de identificación, etapa actual de crianza, lote activo y conteo de aves vivas.
- **Asignación de galpón**: Relación que vincula a un usuario trabajador con los galpones específicos bajo su responsabilidad operativa.
- **Plan nutricional diario**: Configuración establecida por el nutricionista que define el tipo de alimento y la ración en `gr/ave` por día para la etapa y edad del lote.
- **Requerimiento diario de alimento**: Registro que consolida el consumo total calculado en kg, el desglose de la multiplicación, el alimento suministrado y el saldo pendiente de la jornada.
- **Disponibilidad en bodega central**: Existencia neta consolidada en kilogramos de alimento vigente, no vencido y compatible con la etapa de crianza del galpón.
- **Historial de alimentación por galpón**: Registro cronológico diario de raciones programadas, consumos calculados y suministros efectuados en fechas pasadas.
- **Alerta operativa de alimentación**: Indicador visual generado ante condiciones críticas (déficit de stock en bodega central, saldo pendiente de entrega o cambios en la dieta/etapa).

---

## Success Criteria 

### Measurable Outcomes

- **SC-001**: El 100 % de las consultas realizadas por un trabajador muestra únicamente los galpones asignados a su cargo.
- **SC-002**: En el 100 % de las consultas, el cálculo de consumo diario total en kilogramos refleja con exactitud la fórmula `Aves vivas × Ración individual (gr/ave) ÷ 1.000`.
- **SC-003**: En el 100 % de los casos, la consulta muestra el desglose visual de la multiplicación y el saldo pendiente actualizado según los suministros del día.
- **SC-004**: La disponibilidad de bodega central presentada corresponde en el 100 % de los casos a existencias no vencidas y compatibles con la etapa del galpón.
- **SC-005**: El 100 % de los casos en que la disponibilidad en bodega central sea insuficiente para cubrir el consumo diario genera una alerta visible de desabastecimiento.
- **SC-006**: El 100 % de los cambios en el plan nutricional o etapa del galpón genera una alerta visual inmediata para el trabajador.
- **SC-007**: El trabajador puede acceder al historial de días anteriores de sus galpones asignados visualizando los datos históricos inalterados.
- **SC-008**: El 100 % de las operaciones de consulta se ejecuta sin modificar estados ni registros de la base de datos (garantía de solo lectura).

---

## Fuera de Alcance (Out of Scope)

- El registro o ingreso de recepciones de alimento en la bodega central (cubierto en SPEC-001).
- El ajuste, configuración o cambio de planes de ración y tipos de alimento (responsabilidad del nutricionista).
- El registro del suministro físico de alimento servido en el galpón o el registro de mortalidad (cubiertos en sus casos de uso específicos del trabajador).
- El traslado físico, remisión o despacho de bultos/kilogramos desde la bodega central hacia las sub-bodegas de los galpones.
- La administración de usuarios, creación de galpones y la asignación manual de trabajadores a galpones.
