# Feature Specification: Consulta de alimento requerido por galpón

**Created**: 2026-09-02  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar el alimento requerido por etapa en el galpón para Módulo 3 (Finanzas) (Priority: P1)

Como sistema de Finanzas (Módulo 3) o administrador, quiero consultar la proyección del alimento requerido en kilogramos para un galpón en la etapa por la que esté pasando o el consolidado de etapas al finalizar el lote de pollos en el galpón, manteniendo los requerimientos separados por etapa e incluyendo el costo unitario por kilogramo de cada tipo de alimento, para efectuar la liquidación final y el control presupuestario sin sumar kilogramos globales de alimentos diferentes.

**Why this priority**: Representa la conexión oficial y directa entre el Módulo 2 y el Módulo 3 (Finanzas) modelada en el diagrama de casos de uso (`Modulo2_v4.drawio`). Permite a Finanzas disponer de los requerimientos físicos de alimento en kilogramos y sus costos unitarios correspondientes por etapa, facilitando la liquidación contable exacta sin mezclar insumos de diferente composición nutricional y precio.

**Independent Test**: Se puede probar sobre un galpón cuyo lote finalizó su ciclo completo (Pre-inicio de 7 días con 10.000 aves, cuota de 0.035 kg y costo de $1.800/kg; Inicio de 14 días con 9.750 aves, cuota de 0.045 kg y costo de $1.650/kg; y Engorde de 21 días con 9.600 aves, cuota de 0.070 kg y costo de $1.500/kg), verificando que la consulta entregue cada etapa discriminada de forma separada con sus respectivos kilogramos (2.450 kg, 6.142,50 kg y 14.112 kg) y costos unitarios, sin generar una sumatoria global de kilogramos entre etapas.

**Acceptance Scenarios**:

1. **Scenario**: Consulta de requerimiento para la etapa activa que está pasando el galpón
   - **Given** un galpón con lote activo en etapa "Inicio" con duración de 14 días, una población viva al inicio de la etapa de 9.750 aves, cuota nutricional de 0.045 kg/ave/día y alimento con costo de $1.650/kg
   - **When** el Módulo 3 (Finanzas) consulta el alimento requerido para dicho galpón y etapa
   - **Then** el sistema entrega los datos de la etapa "Inicio" con 6.142,50 kg de alimento Iniciador y un costo unitario de $1.650/kg, detallando la duración en días (14), la población viva al corte (9.750 aves) y la cuota diaria aplicada.

2. **Scenario**: Consulta del consolidado discriminado por etapas al finalizar la última etapa del lote
   - **Given** un galpón cuyo lote de pollos finalizó su última etapa productiva ("Engorde/Broiler") habiendo transitado por Pre-inicio, Inicio y Engorde
   - **When** el Módulo 3 (Finanzas) consulta el alimento requerido del lote al cierre del ciclo
   - **Then** el sistema retorna el consolidado con el estado "Finalizado", entregando el registro individual y separado de cada etapa con su tipo de alimento, población al inicio, cuota aplicada, días, kilogramos proyectados y costo unitario por kilogramo de cada alimento
   - **And** no incluye una sumatoria total de kilogramos entre las diferentes etapas.

3. **Scenario**: Consulta de requerimiento con etapa prorrogada por cuarentena o ajuste
   - **Given** un galpón en etapa "Inicio" con duración base de 14 días, una prórroga vigente de 4 días adicionales (por cuarentena sanitaria o bajo peso) y costo unitario de alimento de $1.650/kg
   - **When** el Módulo 3 consulta el requerimiento de alimento de dicha etapa
   - **Then** el sistema entrega la proyección calculada sobre los 18 días efectivos totales (14 base + 4 prórroga), entregando los kilogramos ajustados y el costo unitario por kilo de dicho alimento.

4. **Scenario**: Entrega de requerimientos separados por etapa con costo unitario para liquidación
   - **Given** una solicitud de consulta realizada desde el Módulo 3 para fines de liquidación
   - **When** se procesa y emite la respuesta
   - **Then** el sistema entrega los kilogramos requeridos de forma estrictamente separada por etapa junto con el costo unitario por kilogramo (`costoUnitarioKg`) de cada tipo de alimento, omitiendo sumatorias agregadas de kilogramos heterogéneos.

---

### User Story 2 - Adición automática de la proyección de alimento al cambiar de etapa (Priority: P2)

Como sistema de gestión nutricional y productiva del galpón, quiero que al registrarse el cambio de etapa en un galpón se calcule y agregue automáticamente la proyección de alimento requerido en kilogramos para ese galpón en esa nueva etapa con la población viva de ese momento y su costo unitario vigente, para mantener el historial acumulado disponible y actualizado para la consulta del Módulo 3 (Finanzas).

**Why this priority**: Asegura que el historial de proyecciones se alimente de manera automática en cada transición de etapa, garantizando que cada fase calcule sus kilogramos con la población viva real al momento de su inicio y asocie el costo unitario correspondiente antes de que Finanzas consulte el cierre del lote.

**Independent Test**: Se puede probar registrando el cambio de etapa de un galpón de "Pre-inicio" a "Inicio" con 9.750 aves vivas registradas al corte, verificando que se agregue de inmediato el nuevo registro de proyección para la etapa "Inicio" (con sus kg y costo unitario) sin modificar el registro previo de "Pre-inicio".

**Acceptance Scenarios**:

1. **Scenario**: Registro de la proyección inicial al arrancar la primera etapa (Pre-inicio)
   - **Given** la recepción de un nuevo lote de 10.000 pollos en un galpón iniciando la etapa "Pre-inicio" (7 días con cuota de 0.035 kg/ave/día y alimento Pre-iniciador a $1.800/kg)
   - **When** se activa el lote en el galpón
   - **Then** el sistema calcula y almacena la proyección de la etapa "Pre-inicio" por 2.450 kg calculada con la población de recepción y registra su costo unitario de $1.800/kg.

2. **Scenario**: Adición automática de nueva proyección al cambiar a una etapa posterior
   - **Given** un galpón que concluye la etapa "Pre-inicio" y cuenta con 9.750 aves vivas registradas al corte
   - **When** se registra la transición a la etapa "Inicio" (14 días con cuota de 0.045 kg/ave/día y alimento Iniciador a $1.650/kg)
   - **Then** el sistema calcula la proyección de la nueva etapa con las 9.750 aves vivas (6.142,50 kg) asociando el costo unitario de $1.650/kg
   - **And** agrega este registro al historial del galpón manteniendo inalterada la proyección previa de "Pre-inicio".

---

### Edge Cases

- **Galpón o lote inexistente en la base de datos**: Si la petición de consulta del Módulo 3 envía un identificador de galpón o lote que no existe, el sistema retorna un código de respuesta estructurado de recurso no encontrado (HTTP 404) con mensaje descriptivo sin generar fallas internas.
- **Petición con parámetros vacíos o tipos de datos inválidos**: Si la solicitud carece de identificadores obligatorios o incluye formatos de datos incompatibles (ej. caracteres alfanuméricos en identificadores numéricos), el sistema rechaza la petición mediante código HTTP 400 (Bad Request).
- **Lote con población en cero por contingencia extrema**: Si por anomalía o contingencia extrema la población viva al corte del cambio de etapa es de cero aves, el sistema procesa el registro asignando 0.00 kg a la proyección sin incurrir en excepciones de división o desbordamiento numérico.
- **Alimento sin costo unitario configurado o nulo en inventario**: Si al momento de la consulta un tipo de alimento carece de costo unitario registrado en catálogo, el sistema retorna el valor del costo como nulo o no asignado acompañado de una advertencia informativa de metadatos, sin interrumpir la entrega de los kilogramos calculados para la etapa.
- **Interrupción o timeout en la integración API con Módulo 3**: Si ocurre una interrupción de red durante la invocación del servicio desde el Módulo 3, la consulta opera de forma idempotente de solo lectura, permitiendo reintentos seguros sin efectos secundarios ni duplicidades.
- **Redondeo y precisión matemática en la serialización**: Al serializar los valores en kilogramos y costos unitarios, el sistema aplica redondeo numérico estándar (*half-up*) a dos decimales para evitar discrepancias de redondeo en el transporte de datos.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE exponer un servicio de consulta exclusivo para que el Módulo 3 (Finanzas) y usuarios autorizados consulten la proyección del alimento requerido por galpón, correspondiendo formalmente al caso de uso `"Consultar alimento requerido por galpón"` del sistema.
- **FR-002**: Al registrarse un cambio de etapa en un galpón, el sistema DEBE calcular y agregar automáticamente la proyección de alimento requerido para ese galpón en esa nueva etapa al registro del lote.
- **FR-003**: El cálculo de la proyección de alimento de cada etapa DEBE regirse estrictamente por la fórmula:  
  `Kilogramos Requeridos = Población Viva al Iniciar la Etapa * Cuota Diaria (kg/ave/día) * Días Efectivos de la Etapa`.
- **FR-004**: Para la primera etapa (`Pre-inicio`), la población viva base DEBE ser la población inicial de recepción; para cada etapa subsiguiente (`Inicio`, `Broiler/Engorde`), la población viva base DEBE corresponder a la población de aves vivas existente en el galpón al momento de registrar el cambio de etapa.
- **FR-005**: Si una etapa en el galpón contó con una prórroga o ajuste temporal (ej. por cuarentena sanitaria o bajo peso), la proyección de dicha etapa DEBE considerar los días totales efectivos (días base más días de prórroga).
- **FR-006**: La respuesta a la consulta para el Módulo 3 DEBE entregar los requerimientos de alimento estrictamente separados e individualizados por cada etapa, absteniéndose de sumar o consolidar un total global de kilogramos entre distintas etapas.
- **FR-007**: Por cada etapa incluida en la consulta, el sistema DEBE proporcionar el costo unitario vigente por kilogramo (`costoUnitarioKg`) correspondiente al tipo de alimento asignado a dicha etapa para posibilitar la liquidación en el Módulo 3.
- **FR-008**: La respuesta a la consulta del Módulo 3 DEBE detallar: identificador del galpón, código del lote, estado del ciclo (En progreso o Finalizado), fecha de consulta, y una lista discriminada por etapa conteniendo: nombre de la etapa, tipo de alimento asociado, población viva al inicio de etapa, cuota diaria aplicada, días efectivos de duración, total de kilogramos requeridos de esa etapa y costo unitario por kilogramo.
- **FR-009**: Las proyecciones de alimento agregadas por etapa DEBEN ser inmutables una vez persistidas para garantizar la integridad en auditorías y conciliaciones de liquidación del Módulo 3.

### Key Entities

- **ProyeccionAlimentoEtapa**: Registro persistido e inmutable de la proyección calculada para una etapa específica del galpón.
  - *Atributos*: ID, galponId, loteId, nombreEtapa (Pre-inicio, Inicio, Engorde), tipoAlimento, poblacionInicioEtapa, cuotaKgAveDia, diasEtapa, proyeccionKg, costoUnitarioKg, fechaRegistro.
- **ConsolidadoRequerimientoGalpon**: Estructura de transferencia de datos (DTO) entregada en la respuesta al Módulo 3.
  - *Atributos*: galponId, loteId, estadoCiclo (FINALIZADO, EN_PROGRESO), listaEtapasProyectadas (cada elemento con nombreEtapa, tipoAlimento, proyeccionKg, costoUnitarioKg, diasEtapa, poblacionInicioEtapa), fechaConsulta.
- **Galpón**: Espacio físico que alberga el lote y registra la etapa activa y condiciones del ciclo.
- **LotePollos**: Agrupación de aves que mantiene el historial de población viva al inicio de cada fase.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las consultas de requerimiento para el Módulo 3 entregan los cálculos exactos en kilogramos basados en la población viva al iniciar cada etapa y la duración efectiva.
- **SC-002**: El 100 % de las transiciones de etapa generan y persisten automáticamente su proyección de alimento en kilogramos y su costo unitario asociado en menos de 2 segundos desde la confirmación del cambio de etapa.
- **SC-003**: El tiempo de respuesta de la consulta para el Módulo 3 es inferior a 1 segundo por petición.
- **SC-004**: El 100 % de las respuestas entregadas al Módulo 3 mantienen los requerimientos de alimento separados por etapa e incluyen el costo unitario por kilogramo de cada insumo, con 0 % de sumatorias globales de kilogramos heterogéneos.
- **SC-005**: 0 % de registros de proyecciones previas son sobreescritos, eliminados o alterados cuando se agrega una nueva etapa al historial del galpón.

---

## Out of Scope *(Fuera de alcance de esta especificación)*

- **Cálculo contable final de liquidación del lote**: La multiplicación final de los kilogramos por costo unitario, la determinación del costo total del lote, balances económicos e impuestos corresponden a los procesos internos del Módulo 3 (Finanzas).
- **Definición y ajuste de cuotas nutricionales**: La configuración de la ración diaria por ave (`kg/ave/día`) y su asociación por etapa corresponde al [Spec 022](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/022-AjustePlanNutricionalPorEtapa.md).
- **Personalización de calendarios y días de etapas**: La duración estándar de etapas y desplazamientos en cascada corresponde al [Spec 022](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/022-AjustePlanNutricionalPorEtapa.md).
- **Gestión operativa veterinaria y cuarentenas**: La emisión, registro y diagnóstico de órdenes sanitarias corresponden al módulo de Sanidad.
- **Despachos y movimientos físicos de almacén**: El traslado físico de bultos y control de inventarios en bodegas corresponde a los Módulos 1 y 3 de Logística.
