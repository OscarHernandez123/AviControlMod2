# Feature Specification: Consultar alimento requerido por lote

**Created**: 2026-09-02  
**Updated**: 2026-09-24

## User Scenarios & Testing *(mandatory)*

### Principio de Dominio: El Lote como Entidad Central de Consumo y Liquidación

En la arquitectura del sistema, el **Galpón** representa la infraestructura física de alojamiento, la cual permanece en la granja y alberga a múltiples lotes sucesivos a lo largo del tiempo (un lote finaliza su ciclo, el galpón entra en descanso sanitario y posteriormente recibe un nuevo lote). 

Por tanto:
- Las curvas nutricionales, la población viva de aves, la mortalidad, las proyecciones de demanda de alimento y la liquidación contable pertenecen al **Lote de pollos** (ej. `#LDP-003`).
- El galpón actúa como la **ubicación física de alojamiento** del lote durante su ciclo.
- La consulta de alimento requerido y su integración con el Módulo 3 (Finanzas) se realiza por **Lote**, permitiendo auditar la historia nutricional y financiera de cada parvada de manera independiente de los lotes anteriores o futuros que hayan pasado por el mismo galpón.

---

### User Story 1 - Consultar el alimento requerido por etapa en el lote para Módulo 3 (Finanzas) (Priority: P1)

Como sistema de Finanzas (Módulo 3) o administrador, quiero consultar la proyección del alimento requerido en kilogramos para un lote específico en la etapa por la que esté pasando o el consolidado de etapas al finalizar su ciclo productivo, manteniendo los requerimientos separados por etapa e incluyendo el costo unitario por kilogramo de cada tipo de alimento, para efectuar la liquidación final y el control presupuestario sin sumar kilogramos globales de alimentos diferentes.

**Why this priority**: Representa la conexión oficial y directa entre el Módulo 2 y el Módulo 3 (Finanzas) modelada en el diagrama de casos de uso (`Modulo2_v4.drawio`). Permite a Finanzas disponer de los requerimientos físicos de alimento en kilogramos y sus costos unitarios correspondientes por etapa para un lote determinado, facilitando la liquidación contable exacta sin mezclar insumos de diferente composición nutricional y precio.

**Independent Test**: Se puede probar sobre un lote que finalizó su ciclo completo (Pre-inicio de 7 días con 10.000 aves, cuota de 0.035 kg y costo de $1.800/kg; Inicio de 14 días con 9.750 aves, cuota de 0.045 kg y costo de $1.650/kg; y Engorde de 21 días con 9.600 aves, cuota de 0.070 kg y costo de $1.500/kg), verificando que la consulta entregue cada etapa discriminada de forma separada con sus respectivos kilogramos (2.450 kg, 6.142,50 kg y 14.112 kg) y costos unitarios, sin generar una sumatoria global de kilogramos entre etapas.

**Acceptance Scenarios**:

1. **Scenario**: Consulta de requerimiento para la etapa activa que está cursando el lote
   - **Given** un lote activo alojado en un galpón cursando la etapa "Inicio" con duración de 14 días, una población viva al inicio de la etapa de 9.750 aves, cuota nutricional de 0.045 kg/ave/día y alimento con costo de $1.650/kg
   - **When** el Módulo 3 (Finanzas) consulta el alimento requerido para dicho lote y etapa
   - **Then** el sistema entrega los datos de la etapa "Inicio" con 6.142,50 kg de alimento Iniciador y un costo unitario de $1.650/kg, detallando el código del lote, galpón de alojamiento, duración en días (14), población viva al corte (9.750 aves) y cuota diaria aplicada.

2. **Scenario**: Consulta del consolidado discriminado por etapas al finalizar el ciclo productivo del lote
   - **Given** un lote de pollos que finalizó su última etapa productiva ("Engorde/Broiler") habiendo transitado por Pre-inicio, Inicio y Engorde
   - **When** el Módulo 3 (Finanzas) consulta el alimento requerido del lote al cierre del ciclo
   - **Then** el sistema retorna el consolidado con el estado "Finalizado", entregando el registro individual y separado de cada etapa con su tipo de alimento, población al inicio, cuota aplicada, días, kilogramos proyectados y costo unitario por kilogramo de cada alimento
   - **And** no incluye una sumatoria total de kilogramos entre las diferentes etapas.

3. **Scenario**: Consulta de requerimiento con etapa prorrogada por cuarentena sanitaria o bajo peso
   - **Given** un lote en etapa "Inicio" con duración base de 14 días (6.142,50 kg presupuestados originalmente), una prórroga vigente de 4 días adicionales autorizada por contingencia (sea cuarentena sanitaria preventiva o retraso en ganancia de peso zootécnico según SPEC-021) y alimento Iniciador con costo de $1.650/kg
   - **When** el Módulo 3 consulta el requerimiento de alimento de dicha etapa
   - **Then** el sistema entrega la proyección calculada sobre los 18 días efectivos totales (14 base + 4 prórroga), reportando un total ajustado de 7.897,50 kg
   - **And** desglosa los días base (14), los días de prórroga (4), el motivo de contingencia (`CUARENTENA_SANITARIA` o `BAJO_PESO`), la justificación técnica registrada y el costo unitario invariable de $1.650/kg, permitiendo a Finanzas auditar y justificar contablemente el sobrecosto de la extensión.

4. **Scenario**: Entrega de requerimientos separados por etapa con costo unitario para liquidación
   - **Given** una solicitud de consulta realizada desde el Módulo 3 para fines de liquidación de un lote
   - **When** se procesa y emite la respuesta
   - **Then** el sistema entrega los kilogramos requeridos de forma estrictamente separada por etapa junto con el costo unitario por kilogramo (`costoUnitarioKg`) de cada tipo de alimento, omitiendo sumatorias agregadas de kilogramos heterogéneos.

---

### User Story 2 - Adición automática de la proyección de alimento al cambiar de etapa en el lote (Priority: P2)

Como sistema de gestión nutricional y productiva del lote, quiero que al registrarse el cambio de etapa de un lote se capture el alimento asignado a la nueva etapa (el cual queda bloqueado e inmutable para dicha etapa según el [SPEC-021](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/021-AjustarPlanNutricionalPorEtapa.md)) y se calcule y agregue automáticamente la proyección de alimento requerido en kilogramos para ese lote en esa nueva etapa con la población viva de ese momento y su costo unitario vigente, para mantener el historial acumulado disponible y actualizado para la consulta del Módulo 3 (Finanzas).

**Why this priority**: Asegura que el historial de proyecciones se alimente de manera automática en cada transición de etapa del lote, garantizando que cada fase calcule sus kilogramos con la población viva real al momento de su inicio y asocie el costo unitario correspondiente antes de que Finanzas consulte el cierre del ciclo. Al bloquearse el cambio de alimento durante la etapa en el SPEC-021, se previene la mezcla de costos unitarios e inconsistencias contables.

**Independent Test**: Se puede probar registrando el cambio de etapa de un lote de "Pre-inicio" a "Inicio" con 9.750 aves vivas registradas al corte, verificando que se agregue de inmediato el nuevo registro de proyección para la etapa "Inicio" (con sus kg y costo unitario) sin modificar el registro previo de "Pre-inicio", y comprobando que el alimento de la etapa activa no pueda ser alterado a mitad de fase.

**Acceptance Scenarios**:

1. **Scenario**: Registro de la proyección inicial al arrancar la primera etapa del lote (Pre-inicio)
   - **Given** la recepción de un nuevo lote de 10.000 pollos alojado en un galpón iniciando la etapa "Pre-inicio" (7 días con cuota de 0.035 kg/ave/día y alimento Pre-iniciador a $1.800/kg)
   - **When** se activa el lote en el sistema
   - **Then** el sistema calcula y almacena la proyección de la etapa "Pre-inicio" por 2.450 kg calculada con la población de recepción y registra su costo unitario de $1.800/kg
   - **And** bloquea la modificación del producto alimenticio asignado para esa etapa activa según las directrices de SPEC-021.

2. **Scenario**: Adición automática de nueva proyección al cambiar a una etapa posterior
   - **Given** un lote que concluye la etapa "Pre-inicio" y cuenta con 9.750 aves vivas registradas al corte
   - **When** se registra la transición a la etapa "Inicio" (14 días con cuota de 0.045 kg/ave/día y alimento Iniciador a $1.650/kg)
   - **Then** el sistema calcula la proyección de la nueva etapa con las 9.750 aves vivas (6.142,50 kg) asociando el costo unitario de $1.650/kg
   - **And** agrega este registro al historial del lote manteniendo inalterada la proyección previa de "Pre-inicio".

3. **Scenario**: Actualización auditada de la proyección al aplicarse una prórroga por contingencia (cuarentena o bajo peso)
   - **Given** un lote con su proyección inicial de etapa activa persistida en el sistema (ej. 14 días base y 6.142,50 kg)
   - **When** se aprueba y registra una prórroga de días en el lote conforme a SPEC-021 US-4 (sea por cuarentena sanitaria o retraso en curva de peso)
   - **Then** el sistema actualiza el registro de la proyección de dicha etapa activa: registra los `diasProrroga` (ej. 4 días), recalcula los `diasEfectivos = diasBase + diasProrroga` (18 días) y actualiza los kilogramos requeridos a 7.897,50 kg
   - **And** mantiene inmutable el alimento comercial y el costo unitario asignados
   - **And** registra el motivo de contingencia (`CUARENTENA_SANITARIA` o `BAJO_PESO`), la justificación técnica y el usuario responsable para auditoría contable y zootécnica.

---

### User Story 3 - Consulta consolidada de demanda proyectada para lotes activos y planificación de compras (Priority: P2)

Como administrador de la granja (o encargado de compras y logística), quiero consultar una vista consolidada de la demanda de alimento proyectada para todos los lotes activos en su etapa vigente, agrupada por producto comercial y comparada contra el inventario disponible en bodega central ([SPEC-023](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/023-ConsultarInventario.md)), para conocer con exactitud cuánto alimento se requiere para sostener la producción activa, determinar los faltantes de stock y planificar oportunamente las órdenes de compra sin necesidad de apartar físicamente los bultos en bodega.

**Why this priority**: Resuelve la necesidad logística de compras sin incurrir en bloqueos físicos de stock. Al no apartar el alimento desde el día 1, el administrador requiere una herramienta de inteligencia operativa que sume la demanda de todas las etapas en curso de los lotes vivos y la compare contra las existencias reales, identificando compras urgentes antes de que ocurra un desabastecimiento.

**Independent Test**: Se puede probar con múltiples lotes activos (ej. Lote 1 con demanda de 6.142,50 kg de Iniciador, Lote 2 con 5.800 kg de Iniciador y Lote 3 con 14.000 kg de Engorde), verificando que el sistema totalice 11.942,50 kg de Iniciador (299 bultos de 40 kg), lo compare contra un stock en bodega de 8.000 kg (200 bultos) y reporte un déficit exacto de compra de 3.942,50 kg (99 bultos) con alerta de reabastecimiento, sin alterar ni descontar inventario.

**Acceptance Scenarios**:

1. **Scenario**: Consolidación de demanda proyectada de todos los lotes activos por producto
   - **Given** varios lotes activos en etapas productivas vigentes en diferentes galpones
   - **When** el administrador consulta el consolidado de demanda de lotes activos
   - **Then** el sistema presenta el total demandado agrupado por alimento comercial y peso nominal de bulto, detallando la suma de kilogramos requeridos y su equivalente en bultos
   - **And** permite expandir cada producto para visualizar la lista de lotes y los galpones donde están alojados que contribuyen a dicha demanda.

2. **Scenario**: Balance comparativo contra el stock disponible en bodega central (Cálculo de déficit de compra)
   - **Given** una demanda consolidada de 11.942,50 kg (299 bultos) para un alimento comercial en los lotes activos y un stock disponible en bodega central de 8.000 kg (200 bultos) según el SPEC-023
   - **When** el administrador consulta el balance de abastecimiento
   - **Then** el sistema calcula y exhibe un déficit de compra de 3.942,50 kg (99 bultos) requeridos para completar las etapas en curso de los lotes
   - **And** clasifica el producto con la alerta `"Reabastecimiento Necesario"`.

3. **Scenario**: Cobertura suficiente para la etapa activa sin requerimiento de compra inmediata
   - **Given** una demanda consolidada de 5.000 kg (125 bultos) y un stock disponible en bodega central de 8.000 kg (200 bultos)
   - **When** el administrador consulta el balance
   - **Then** el sistema reporta un saldo cubierto con superávit de 3.000 kg (75 bultos) para la etapa de los lotes
   - **And** clasifica el estado de abastecimiento como `"Stock Suficiente para Etapa Activa"`.

4. **Scenario**: Exclusión de lotes finalizados o galpones en descanso sanitario
   - **Given** lotes en estado finalizado o galpones en vacío sanitario sin aves
   - **When** el sistema genera la consolidación de demanda de lotes activos
   - **Then** excluye dichos registros del cálculo, considerando únicamente lotes en etapas productivas en progreso.

---

### Edge Cases

- **Edge case #1 - Inmutabilidad del alimento asignado durante la etapa activa del lote**

  - ¿Cómo garantiza el sistema que la proyección no mezcle insumos ni costos heterogéneos si ocurre una contingencia durante la etapa activa?  
    El sistema se apoya en la regla de bloqueo del SPEC-021, impidiendo el cambio de alimento comercial durante una etapa activa. Si se aprueba una prórroga por cuarentena sanitaria o bajo peso, el sistema recalcula los kilogramos sobre los nuevos días efectivos totales, conservando estrictamente el mismo alimento y costo unitario histórico.

- **Edge case #2 - Convivencia de múltiples lotes históricos alojados sucesivamente en un mismo galpón**

  - ¿Cómo evita el sistema que las proyecciones de un nuevo lote que ingresa a un galpón sobreescriban o se mezclen con las proyecciones del lote anterior que ya concluyó su ciclo en ese mismo galpón?  
    El sistema vincula todas las proyecciones al identificador único e inmutable del lote (`loteId`). Al consultar la historia o liquidación, filtra exclusivamente por el lote correspondiente, garantizando que los registros del lote anterior permanezcan inalterados y archivados con su respectivo código de lote.

- **Edge case #3 - Consulta de requerimientos para un lote inexistente en la base de datos**

  - ¿Cómo responde el servicio de consulta si el Módulo 3 (Finanzas) solicita la proyección enviando un identificador de lote no registrado o eliminado?  
    El sistema debe interceptar la solicitud, evitar fallas internas no controladas y retornar un código de respuesta HTTP 404 (Not Found) estructurado con un mensaje descriptivo que informe que el lote solicitado no existe en el sistema.

- **Edge case #4 - Petición con parámetros vacíos o tipos de datos inválidos**

  - ¿Qué respuesta emite el sistema si la invocación API carece de identificadores obligatorios o incluye formatos incompatibles (ej. caracteres alfanuméricos en identificadores numéricos)?  
    El sistema debe validar los parámetros de entrada antes de ejecutar la consulta y responder con un código HTTP 400 (Bad Request), detallando el parámetro erróneo sin ejecutar cálculos innecesarios ni comprometer la disponibilidad del servicio.

- **Edge case #5 - Lote activo con población viva en cero por contingencia extrema de mortalidad**

  - ¿Cómo calcula la proyección el sistema si al registrar el cambio de etapa la población viva del lote es igual a cero aves por mortalidad total o anomalía zootécnica?  
    El sistema debe registrar la etapa asignando exactamente `0.00 kg` a la proyección de requerimiento, evitando excepciones de división por cero o desbordamientos numéricos, y registrando el costo unitario correspondiente para fines de auditoría.

- **Edge case #6 - Alimento comercial sin costo unitario registrado o con valor nulo en inventario**

  - ¿Qué información retorna la consulta de liquidación si al momento de la consulta un tipo de alimento carece de costo de compra registrado en el catálogo o inventario?  
    El sistema debe entregar los kilogramos calculados para la etapa sin interrupciones, retornando el costo unitario como nulo o no asignado (`null`) e incluyendo una advertencia descriptiva en los metadatos para que Finanzas gestione el ingreso del precio antes del cierre formal del lote.

- **Edge case #7 - Interrupción de red o timeout durante la integración API con el Módulo 3**

  - ¿Cómo se garantiza la consistencia si la comunicación entre el Módulo 2 y el Módulo 3 se corta mientras se transfiere la consulta de requerimientos?  
    El servicio de consulta debe ser estrictamente de solo lectura e idempotente. Ante un fallo de red o tiempo de espera agotado, el Módulo 3 puede reintentar la solicitud cuantas veces sea necesario sin generar duplicidades, cambios de estado ni bloqueos en los registros del lote.

- **Edge case #8 - Redondeo y precisión matemática en la serialización de kilogramos y costos**

  - ¿Cómo maneja el sistema las discrepancias de decimales al calcular y serializar los kilogramos requeridos y los costos unitarios?  
    El sistema debe aplicar redondeo numérico estándar (*half-up*) a dos decimales (`0.01`) tanto para los kilogramos de alimento proyectados como para los montos monetarios de costo unitario, evitando discrepancias de centavos o fracciones acumuladas en el transporte JSON hacia el Módulo 3.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE exponer un servicio de consulta exclusivo para que el Módulo 3 (Finanzas) y usuarios autorizados consulten la proyección del alimento requerido por lote, correspondiendo formalmente al caso de uso `"Consultar alimento requerido por lote"` del sistema.
- **FR-002**: Al registrarse un cambio de etapa en un lote, el sistema DEBE calcular y agregar automáticamente la proyección de alimento requerido para ese lote en esa nueva etapa al registro histórico de dicho lote, capturando el alimento y cuota fijados en el plan nutricional ([SPEC-021]).
- **FR-003**: El cálculo de la proyección de alimento de cada etapa del lote DEBE regirse estrictamente por la fórmula:  
  `Kilogramos Requeridos = Población Viva al Iniciar la Etapa * Cuota Diaria (kg/ave/día) * Días Efectivos de la Etapa`.
- **FR-004**: Para la primera etapa (`Pre-inicio`), la población viva base DEBE ser la población inicial de recepción del lote; para cada etapa subsiguiente (`Inicio`, `Broiler/Engorde`), la población viva base DEBE corresponder a la población de aves vivas existente en el lote al momento de registrar el cambio de etapa.
- **FR-005**: Si una etapa del lote contó con una prórroga o ajuste temporal por contingencia (sea cuarentena sanitaria preventiva o retraso en peso zootécnico conforme a SPEC-021), la proyección de dicha etapa DEBE actualizar sus kilogramos considerando los días totales efectivos (`diasEfectivos = diasBase + diasProrroga`) multiplicados por la cuota y utilizando el costo unitario del alimento inmutable asignado a dicha etapa. La respuesta DEBE desglosar los días base, días de prórroga, motivo zootécnico (`CUARENTENA_SANITARIA` o `BAJO_PESO`) y justificación registrada para permitir la auditoría de costos en el Módulo 3.
- **FR-006**: La respuesta a la consulta para el Módulo 3 DEBE entregar los requerimientos de alimento estrictamente separados e individualizados por cada etapa del lote, absteniéndose de sumar o consolidar un total global de kilogramos entre distintas etapas.
- **FR-007**: Por cada etapa incluida en la consulta, el sistema DEBE proporcionar el costo unitario vigente por kilogramo (`costoUnitarioKg`) correspondiente al tipo de alimento asignado a dicha etapa para posibilitar la liquidación en el Módulo 3.
- **FR-008**: La respuesta a la consulta del Módulo 3 DEBE detallar: código del lote, galpón de alojamiento actual, estado del ciclo (En progreso o Finalizado), fecha de consulta, y una lista discriminada por etapa conteniendo: nombre de la etapa, tipo de alimento asociado, población viva al inicio de etapa, cuota diaria aplicada, días base, días de prórroga, días efectivos de duración, motivo de contingencia (si aplica), total de kilogramos requeridos de esa etapa y costo unitario por kilogramo.
- **FR-009**: Las proyecciones de alimento agregadas por etapa DEBEN ser inmutables una vez persistidas para garantizar la integridad en auditorías y conciliaciones de liquidación del Módulo 3, respaldadas por la regla del SPEC-021 que prohíbe cambiar el alimento comercial durante el transcurso de una etapa activa.
- **FR-010**: El sistema DEBE rechazar cualquier intento de modificar o reemplazar el producto comercial asignado a una etapa activa en curso de un lote, previniendo discrepancias entre la proyección contable de Finanzas y los consumos reales.
- **FR-011**: El sistema DEBE proveer al Administrador y al Nutricionista una vista consolidada de la demanda de alimento proyectada para todos los lotes activos en su etapa vigente, incorporando de forma inmediata los días efectivos totales con sus prórrogas activas.
- **FR-012**: La vista consolidada DEBE agrupar la demanda por producto comercial (`Alimento`) y presentación (`pesoNominalPorBulto`), calculando la sumatoria total en kilogramos y bultos equivalentes requeridos para sostener la etapa en curso de los lotes.
- **FR-013**: El sistema DEBE cruzar en tiempo real la demanda consolidada de cada producto contra su existencia disponible vigente en bodega central obtenida desde el SPEC-023 (excluyendo recepciones vencidas o anuladas).
- **FR-014**: El sistema DEBE calcular el balance de abastecimiento para cada alimento como: `BalanceKg = ExistenciaDisponibleKg - DemandaConsolidadaKg`. Si el balance es negativo, el sistema DEBE clasificar el producto bajo la alerta `"Reabastecimiento Necesario"` y mostrar la cantidad exacta de bultos sugeridos a comprar.
- **FR-015**: La consulta consolidada de abastecimiento DEBE operar estrictamente en modo de solo lectura y NO DEBE descontar, reservar ni bloquear existencias físicas en el inventario de bodega central.

---

### Key Entities

- **ProyeccionAlimentoEtapa**: Registro persistido de la proyección calculada para una etapa específica del lote.
  - *Atributos*: ID, loteId (clave principal de la parvada), galponId (galpón donde se encuentra alojado), nombreEtapa (Pre-inicio, Inicio, Engorde), tipoAlimento, alimentoComercial, poblacionInicioEtapa, cuotaKgAveDia, diasBase, diasProrroga, diasEfectivos, tieneProrroga (booleano), motivoProrroga (`CUARENTENA_SANITARIA`, `BAJO_PESO`), justificacionAjuste, proyeccionKg, costoUnitarioKg, fechaRegistro, fechaActualizacion.
- **ConsolidadoRequerimientoLote**: Estructura de transferencia de datos (DTO) entregada en la respuesta al Módulo 3.
  - *Atributos*: loteId, galponId, estadoCiclo (FINALIZADO, EN_PROGRESO), listaEtapasProyectadas (cada elemento con nombreEtapa, tipoAlimento, proyeccionKg, costoUnitarioKg, diasBase, diasProrroga, diasEfectivos, motivoProrroga, poblacionInicioEtapa), fechaConsulta.
- **BalanceAbastecimientoAlimento**: Modelo consolidado de proyección y compra para el administrador.
  - *Atributos*: alimentoId, nombreComercial, tipoAlimento, pesoNominalPorBulto, demandaConsolidadaKg, demandaConsolidadaBultos, stockDisponibleKg, stockDisponibleBultos, deficitCompraKg, deficitCompraBultos, estadoAbastecimiento (SUFICIENTE, REABASTECIMIENTO_NECESARIO).
- **LotePollos**: Agrupación biológica de aves que constituye la unidad de producción, costo, proyección y liquidación.
  - *Atributos*: ID, codigoLote, tipoAve, galponAlojamientoId, poblacionInicial, poblacionActual, fechaIngreso, fechaProyectadaSalida, estadoCiclo.
- **Galpón**: Espacio físico que alberga transitoriamente a un lote durante su ciclo productivo.
  - *Atributos*: ID, nombre, capacidad, loteActivoId, estadoOcupacion.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las consultas de requerimiento para el Módulo 3 entregan los cálculos exactos en kilogramos asociados al lote consultado basados en la población viva al iniciar cada etapa y la duración efectiva.
- **SC-002**: El 100 % de las transiciones de etapa de los lotes generan y persisten automáticamente su proyección de alimento en kilogramos y su costo unitario asociado en menos de 2 segundos desde la confirmación del cambio de etapa.
- **SC-003**: El tiempo de respuesta de la consulta por lote para el Módulo 3 es inferior a 1 segundo por petición.
- **SC-004**: El 100 % de las respuestas entregadas al Módulo 3 mantienen los requerimientos de alimento separados por etapa del lote e incluyen el costo unitario por kilogramo de cada insumo, con 0 % de sumatorias globales de kilogramos heterogéneos.
- **SC-005**: 0 % de registros de proyecciones previas son sobreescritos, eliminados o alterados cuando se agrega una nueva etapa al historial del lote o cuando un nuevo lote ingresa a un galpón previamente ocupado.
- **SC-006**: El 100 % de las consultas de balance de abastecimiento consolidado para el administrador calculan con exactitud el déficit de compra en bultos y kilogramos de los lotes activos en menos de 2 segundos.
- **SC-007**: 0 % de existencias físicas en bodega central son descontadas, bloqueadas o alteradas por la consulta o generación de las proyecciones consolidadas.

---

## Out of Scope *(Fuera de alcance de esta especificación)*

- **Cálculo contable final de liquidación del lote**: La multiplicación final de los kilogramos por costo unitario, la determinación del costo total del lote, balances económicos e impuestos corresponden a los procesos internos del Módulo 3 (Finanzas).
- **Definición y ajuste de cuotas nutricionales**: La configuración de la ración diaria por ave (`kg/ave/día`) y su asociación por etapa corresponde al [Spec 021](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/021-AjustarPlanNutricionalPorEtapa.md).
- **Personalización de calendarios y días de etapas**: La duración estándar de etapas y desplazamientos en cascada corresponde al [Spec 021](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/021-AjustarPlanNutricionalPorEtapa.md).
- **Gestión operativa veterinaria y cuarentenas**: La emisión, registro y diagnóstico de órdenes sanitarias corresponden al módulo de Sanidad.
- **Despachos y movimientos físicos de almacén**: El traslado físico de bultos y control de inventarios en bodegas corresponde a los Módulos 1 y 3 de Logística.
- **Emisión de órdenes de compra a proveedores**: La generación de órdenes de compra, cotizaciones y pagos corresponde al Módulo de Compras/Finanzas.
