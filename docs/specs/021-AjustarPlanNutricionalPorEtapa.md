# Feature Specification: Ajustar plan nutricional por etapa

**Created**: 2026-08-25  
**Updated**: 2026-09-24

## User Scenarios & Testing *(mandatory)*

### Arquitectura Conceptual: Modelo Híbrido y Principios Rectores

El sistema opera bajo los siguientes principios de arquitectura y reglas de negocio:

1. **Plantilla Maestra vs. Instancia Desacoplada:**
   - **Plantilla Maestra (Catálogo Global):** El nutricionista define modelos estándar (ej. *Plan Estándar Broiler*, *Crecimiento Rápido*) con etapas base, raciones por ave (`kg/pollo/día`), duraciones base estimadas y el bulto/alimento por defecto. Solo puede existir **un único plan marcado como predeterminado (`esPredeterminado = true`) por cada tipo de ave**.
   - **Instancia del Galpón:** Al alojar un lote, el galpón recibe su propia copia independiente del plan. Los ajustes en un galpón **jamás alteran la plantilla maestra ni a otros galpones**.
2. **Bloqueo e Inmutabilidad del Alimento durante la Etapa Activa (Blindaje con SPEC-022):**
   - Al activarse una etapa en un galpón, el alimento comercial seleccionado queda **estrictamente bloqueado e inmutable para dicha etapa y NO puede ser modificado durante su transcurso**.
   - Esta restricción previene inconsistencias zootécnicas y garantiza la integridad de la proyección contable de requerimientos generada para el Módulo 3 (Finanzas) según el [SPEC-022](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/022-ConsultarAlimentoRequeridoPorLote.md), la cual almacena de forma inmutable el alimento y costo unitario de la etapa.
   - Cualquier cambio de alimento comercial debe planificarse exclusivamente para las **etapas posteriores que aún no hayan comenzado**. Durante la etapa activa, el único ajuste permitido sobre el cronograma es la prórroga o modificación de su duración en días por contingencias (conforme a US-4).
3. **Cierre Matemático de Cascada y Fecha de Salida:**
   - Al modificarse el día final de una etapa `k`, todas las etapas posteriores `i` (donde `i > k`) **conservan su duración efectiva previamente configurada** (`diasEfectivos_i`) y recalculan su cronograma en cascada:
     $$\text{DiaInicio}_i = \text{DiaFin}_{i-1} + 1$$
     $$\text{DiaFin}_i = \text{DiaInicio}_i + \text{diasEfectivos}_i - 1$$
   - La última etapa del ciclo ("Finalización/Engorde") recalcula su `DiaFin`, el cual actualiza automáticamente la **fecha proyectada de salida/sacrificio** del lote y emite una notificación de advertencia zootécnica.
4. **Sustitución de Plan No Destructiva (Inmutabilidad Histórica):**
   - La sustitución de un plan en un lote activo **mantiene estrictamente inmutables las etapas ya transcurridas** (con sus raciones, fechas y consumos históricos registrados).
   - El nuevo plan se empareja a partir de la **etapa activa vigente** según la edad actual de las aves, proyectando exclusivamente las etapas presentes y futuras.
5. **Matriz de Excepciones en la Asignación Automática:**
   - Si no existe un plan predeterminado activo para el tipo de ave, el sistema marca el galpón en estado `"Pendiente de Asignación Nutricional"` y emite una alerta prioritaria al nutricionista sin bloquear el alojamiento.
   - Si el lote ingresa con una edad superior al inicio estándar (ej. día 14 de vida), las etapas previas se registran como `"Omitidas"` y se activa directamente la etapa que cubra la edad de recepción.

---

### User Story 1 - Creación y Configuración de Plantillas Maestras de Plan Nutricional (Priority: P1)

Como nutricionista de la granja, quiero crear y gestionar plantillas maestras de planes nutricionales en el catálogo global, definiendo sus etapas de crianza (Pre-inicio, Inicio, Crecimiento, Finalización/Engorde) con sus días de duración estimados, su dosificación de ración diaria (`kg/pollo/día`), su alimento sugerido y su condición de plan predeterminado por tipo de ave, para estandarizar las curvas de alimentación que se aplicarán a los lotes de la granja.

**Why this priority**: Es la base del catálogo nutricional. Permite centralizar las curvas de alimentación estándar y garantizar que el sistema siempre cuente con una referencia predeterminada unívoca por tipo de ave.

**Independent Test**: Se puede probar creando una plantilla maestra (ej. "Plan Estándar Broiler 45 días") con 3 etapas estándar (Pre-inicio: días 1-7 a 0.015 kg, Inicio: días 8-21 a 0.045 kg, Engorde: días 22-45 a 0.090 kg) y marcándola como predeterminada para aves tipo "Broiler", verificando que no existan solapamientos de días y que el catálogo impida duplicidad de predeterminados.

**Acceptance Scenarios**:

1. **Scenario**: Creación exitosa de plantilla maestra con validación de continuidad de etapas
   - **Given** que el nutricionista accede al formulario de "Nuevo Plan Nutricional"
   - **When** ingresa el nombre del plan, selecciona el tipo de ave ("Broiler"), marca la casilla "Plan Predeterminado" y define las etapas con rangos continuos (Pre-inicio: 1-7, Inicio: 8-21, Engorde: 22-45) y raciones mayores a cero
   - **Then** el sistema valida que las etapas sean estrictamente contiguas (`DiaInicio_i = DiaFin_{i-1} + 1`), guarda la plantilla maestra y desmarca automáticamente cualquier otro plan predeterminado previo para el tipo de ave "Broiler" garantizando unicidad.

2. **Scenario**: Rechazo de ración no válida (cero o negativa) en cualquier etapa
   - **Given** el formulario de creación o edición de plantilla
   - **When** el nutricionista ingresa un valor menor o igual a cero (ej. `0.0` o `-0.02` kg/pollo/día) en cualquiera de las etapas
   - **Then** el sistema rechaza la operación y muestra un mensaje de error indicando que la ración diaria por ave debe ser un número estrictamente positivo.

3. **Scenario**: Aislamiento de modificaciones en la plantilla maestra
   - **Given** una plantilla maestra previamente asignada a galpones con lotes actualmente en producción
   - **When** el nutricionista edita la ración o duración de una etapa en la plantilla maestra
   - **Then** el sistema guarda los cambios en la plantilla para los futuros lotes que ingresen
   - **And** mantiene inalteradas las instancias activas y proyecciones de los galpones en curso.

---

### User Story 2 - Asignación de Plan a Galpón, Excepciones y Sustitución Históricamente Segura (Priority: P1)

Como nutricionista o administrador, quiero que al ingresar un lote a un galpón se le asigne automáticamente el plan predeterminado según el tipo de ave (gestionando excepciones de ingreso por edad o ausencia de plan), y disponer de la facultad de sustituir el plan en lotes activos preservando el historial transcurrido, para garantizar que ningún lote opere sin pauta alimenticia y que los cambios de plan respeten la trazabilidad zootécnica.

**Why this priority**: Evita galpones huérfanos de plan, maneja ingresos atípicos y permite corregir o cambiar el régimen alimenticio a mitad de ciclo sin corromper los datos históricos de consumo.

**Independent Test**: Se puede probar alojando un lote de 1 día (asignación automática inmediata), alojando un lote que llega a día 14 (omisión de Pre-inicio y activación en Inicio), forzando el ingreso sin plan predeterminado (alerta y estado Pendiente), y sustituyendo el plan en día 25 (conservando etapas de Pre-inicio e Inicio inmutables).

**Acceptance Scenarios**:

1. **Scenario**: Asignación automática exitosa al ingresar lote estándar (día 1)
   - **Given** el alojamiento de un nuevo lote de 1 día de edad de tipo "Broiler" en un galpón
   - **When** el sistema procesa el registro de entrada
   - **Then** crea una instancia desacoplada del plan predeterminado activo de "Broiler" para ese galpón
   - **And** activa la etapa "Pre-inicio" a partir del día 1.

2. **Scenario**: Excepción - Ingreso de lote con edad avanzada (edad > 1 día)
   - **Given** la recepción de un lote de aves que ingresa a la granja con 14 días de edad cumplidos
   - **When** el sistema asigna el plan nutricional al galpón
   - **Then** marca las etapas anteriores ("Pre-inicio" días 1-7) como `"Omitida"`
   - **And** activa inmediatamente la etapa cuyo rango cubra la edad actual ("Inicio" días 8-21)
   - **And** fija el inicio de cálculo efectivo del galpón a partir del día 14.

3. **Scenario**: Excepción - Ausencia de plan predeterminado activo para el tipo de ave
   - **Given** un tipo de ave para el cual no existe ningún plan nutricional marcado como predeterminado activo en el catálogo
   - **When** se confirma el ingreso del lote al galpón
   - **Then** el sistema confirma el alojamiento del lote pero asigna al galpón el estado `"Pendiente de Asignación Nutricional"`
   - **And** genera una notificación de alta prioridad en la bandeja del nutricionista requiriendo la asignación manual del plan
   - **And** registra demanda en kg igual a `0.0` hasta que se asigne un plan formal.

4. **Scenario**: Sustitución de plan en lote activo preservando inmutabilidad histórica
   - **Given** un galpón en el día 25 de vida (cursando la etapa "Engorde") con etapas "Pre-inicio" (días 1-7) e "Inicio" (días 8-21) ya completadas
   - **When** el nutricionista abre el modal de asignación y sustituye el plan actual por "Plan Crecimiento Rápido"
   - **Then** el sistema conserva inalterado el historial y consumos de las etapas transcurridas "Pre-inicio" e "Inicio"
   - **And** acopla el nuevo plan para iniciar a partir de la etapa activa vigente ("Engorde"), respetando el alimento bloqueado para la fase en curso y aplicando cambios únicamente a las etapas subsiguientes.

---

### User Story 3 - Selección de Alimento Compatible, Bloqueo en Etapa Activa e Integridad con SPEC-022 (Priority: P1)

Como nutricionista, quiero consultar los alimentos compatibles del catálogo y sus existencias en bodega central al planificar una etapa de crianza (diferenciando presentaciones de bulto y excluyendo vencidos/anulados según SPEC-023), asegurando que una vez iniciada la etapa el alimento seleccionado quede estrictamente bloqueado para garantizar que la proyección registrada para el Módulo 3 ([SPEC-022](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/022-ConsultarAlimentoRequeridoPorLote.md)) no sufra distorsiones de insumo o costo unitario.

**Why this priority**: Conecta la formulación teórica con la disponibilidad real de inventario y establece una regla unívoca y segura: una etapa se alimenta con un único producto comercial para blindar las proyecciones y liquidaciones contables del Módulo 3.

**Independent Test**: Se puede probar seleccionando una etapa "Inicio" previa a su inicio (duración base 14 días), seleccionando un alimento concentrado que especifica 10 días recomendados, comprobando que se ajuste la etapa a 10 días y desplace la siguiente en cascada, verificando que al activarse la etapa se bloquee el cambio de alimento para esa fase activa, y comprobando que no se descuente inventario de bodega en la planificación.

**Acceptance Scenarios**:

1. **Scenario**: Visualización de alimentos compatibles, presentaciones y stock según SPEC-023
   - **Given** que el nutricionista planifica una etapa de un galpón o plantilla maestra
   - **When** consulta las opciones de alimento en el formulario
   - **Then** el sistema muestra únicamente alimentos comerciales activos clasificados bajo el `TipoAlimento` compatible con la etapa
   - **And** detalla para cada opción: nombre comercial, peso nominal por bulto, proteína/perfil nutricional, duración recomendada en días, existencia disponible en kg y bultos equivalentes
   - **And** separa en filas independientes las presentaciones con pesos de bulto heterogéneos y excluye recepciones vencidas o anuladas.

2. **Scenario**: Selección de alimento sin existencias ("Sin stock") con advertencia
   - **Given** un alimento comercial activo compatible con 0 kg disponibles en bodega central
   - **When** el nutricionista lo selecciona para la etapa en planificación
   - **Then** el sistema lo exhibe con la etiqueta "Sin stock"
   - **And** permite seleccionarlo emitiendo una advertencia informativa de abastecimiento requerido sin bloquear la parametrización.

3. **Scenario**: Aplicación de la regla de recálculo por perfil nutricional en planificación previa
   - **Given** una etapa "Inicio" con duración base de 14 días aún no iniciada
   - **When** el nutricionista selecciona un alimento comercial cuya ficha técnica registra `duracionRecomendadaEtapaDias = 10`
   - **Then** el sistema prellena el campo de duración con `10 días` (permitiendo ajuste manual si el especialista lo requiere)
   - **And** al confirmar/guardar, establece la duración total de la etapa en 10 días (días 8 al 17)
   - **And** recalcula en cascada la siguiente etapa ("Engorde") fijando su inicio en el día 18.

4. **Scenario**: Precedencia ante alimento sin duración recomendada (`null` o no configurada)
   - **Given** un alimento comercial activo cuya ficha no especifica duración recomendada (`duracionRecomendadaEtapaDias = null`)
   - **When** el nutricionista selecciona dicho alimento para la etapa
   - **Then** el sistema mantiene la duración base configurada en la plantilla de la etapa sin forzar recálculo
   - **And** permite al nutricionista editar manualmente la duración si lo considera necesario.

5. **Scenario**: Bloqueo estricto de cambio de alimento en etapa activa (blindaje con SPEC-022)
   - **Given** un galpón cursando activamente la etapa "Inicio" con su alimento comercial y su proyección oficial ya persistida en el sistema conforme a SPEC-022
   - **When** el usuario intenta cambiar o sustituir el alimento comercial asignado a dicha etapa activa
   - **Then** el sistema bloquea los controles de modificación del alimento para la etapa activa e informa que el insumo no puede cambiarse durante el curso de la etapa para preservar la integridad de la proyección de consumo y el costo unitario del lote
   - **And** orienta al usuario a que cualquier transición hacia un nuevo alimento comercial debe programarse exclusivamente para las etapas posteriores no iniciadas.

6. **Scenario**: Planificación de nuevo alimento para etapas futuras no iniciadas
   - **Given** un galpón cursando activamente la etapa "Inicio" y una etapa posterior "Engorde" que aún no ha comenzado
   - **When** el nutricionista selecciona un alimento diferente para la etapa "Engorde" con duración recomendada de 20 días
   - **Then** el sistema permite la selección, actualiza la duración prevista de dicha etapa futura y ajusta en cascada el día final proyectado de salida del lote.

7. **Scenario**: Persistencia lógica sin movimientos de inventario
   - **Given** la confirmación de la asignación del alimento y presentación para la etapa
   - **When** se guardan los cambios en el sistema
   - **Then** persiste la configuración en la instancia del plan
   - **And** no crea movimientos de almacén, consumos ni descuentos de stock en bodega central (el consumo corresponde a despachos diarios de SPEC-014).

---

### User Story 4 - Ajuste Manual de Días de Etapa, Cierre de Cascada y Sincronización de Salida (Priority: P1)

Como nutricionista, quiero ajustar manualmente los días de duración de una etapa activa por contingencias (bajo peso o cuarentena sanitaria) manteniendo su alimento inmutable y registrando una justificación obligatoria, para que el sistema cierre matemáticamente la cascada conservando las duraciones de las etapas posteriores y actualice la fecha proyectada de salida/sacrificio sin generar errores contables en la proyección del lote.

**Why this priority**: Las contingencias zootécnicas exigen prorrogar o acortar fases. El ajuste exclusivo sobre los días (sin alterar el insumo) permite actualizar los kilogramos en SPEC-022 con el mismo costo unitario, manteniendo cerrada la cascada y sincronizada la faena.

**Independent Test**: Se puede probar en un lote de 45 días totales, extendiendo la etapa activa "Inicio" en 5 días con justificación técnica (sin modificar el alimento), comprobando que "Engorde" mantenga sus 24 días efectivos iniciando 5 días después, y verificando que la fecha proyectada de sacrificio del lote se desplace automáticamente en 5 días.

**Acceptance Scenarios**:

1. **Scenario**: Ajuste de días de etapa activa con cierre completo de cascada
   - **Given** un galpón con cronograma: Pre-inicio (días 1-7, 7 días), Inicio (días 8-21, 14 días), Engorde (días 22-45, 24 días) cursando actualmente la etapa "Inicio"
   - **When** el nutricionista amplía la etapa "Inicio" hasta el día 26 (duración total 19 días) por bajo peso y registra la justificación obligatoria
   - **Then** el sistema actualiza la etapa "Inicio" para abarcar los días 8 al 26 manteniendo estrictamente el mismo alimento comercial asignado
   - **And** desplaza automáticamente la etapa "Engorde" aplicando la fórmula de cascada: conserva sus 24 días efectivos, fijando `DiaInicio = 27` y `DiaFin = 27 + 24 - 1 = Día 50`
   - **And** actualiza automáticamente la fecha proyectada de salida/sacrificio del lote sumando 5 días al cronograma general.

2. **Scenario**: Rechazo de ajuste sin justificación obligatoria
   - **Given** el formulario de ajuste de etapa de un galpón
   - **When** el nutricionista modifica el día final pero deja el campo "Justificación del Ajuste" vacío o con espacios en blanco
   - **Then** el sistema bloquea el guardado e informa que la justificación técnica es obligatoria para auditoría.

3. **Scenario**: Bloqueo de edición de etapas históricas transcurridas
   - **Given** un galpón con lote en el día 28 de vida (etapa "Engorde")
   - **When** el usuario intenta modificar las etapas "Pre-inicio" o "Inicio"
   - **Then** el sistema bloquea los controles de edición de dichas etapas e informa que las etapas transcurridas son inmutables.

4. **Scenario**: Reducción de etapa activa con avance en cascada de fecha de sacrificio
   - **Given** un lote con etapa activa "Inicio" acortada en 3 días debido a ganancia acelerada de peso
   - **When** se confirma la reducción de la etapa respetando `DiaFin >= EdadActualLote`
   - **Then** la etapa "Engorde" adelanta su inicio en 3 días manteniendo su duración efectiva
   - **And** el día final del lote se adelanta en 3 días, actualizando la fecha estimada de sacrificio y emitiendo una notificación informativa.

---

### Edge Cases

- **Edge case #1 - Intento de sustitución o cambio de alimento durante una etapa activa**

  - ¿Cómo responde el sistema si un usuario intenta cambiar el alimento comercial o presentación asignado a una etapa que ya se encuentra en curso en un galpón?  
    El sistema debe bloquear los controles de edición del producto para la etapa activa e informar que el insumo es inmutable durante el curso de dicha fase para proteger la proyección de consumo y costo unitario registrada en el SPEC-022. Debe orientar al usuario a programar cualquier cambio de alimento comercial exclusivamente para las etapas futuras que aún no hayan comenzado.

- **Edge case #2 - Alimento comercial sin duración recomendada registrada en el catálogo**

  - ¿Qué duración adopta la etapa si el nutricionista selecciona un alimento comercial cuya ficha técnica no especifica `duracionRecomendadaEtapaDias` (valor nulo o no configurado)?  
    El sistema debe mantener la duración base de días configurada en la plantilla maestra de la etapa sin forzar recálculos automáticos. Debe presentar dicho valor base en el campo de duración de la interfaz y permitir al nutricionista ajustarlo manualmente si su criterio técnico así lo requiere.

- **Edge case #3 - Lote alojado con población viva igual a cero (galpón en descanso o vacío)**

  - ¿Cómo calcula el sistema la demanda en kilogramos y bultos cuando un galpón se encuentra vacío, en descanso sanitario o con población activa de cero aves?  
    El sistema debe calcular y mostrar `0.0 kg` y `0.0 bultos` de demanda diaria sin generar errores de división por cero ni excepciones aritméticas, manteniendo disponibles los parámetros de ración configurados para cuando se aloje un nuevo lote.

- **Edge case #4 - Ración diaria individual con alta precisión decimal**

  - ¿Cómo maneja el sistema la parametrización de raciones de pollitos en sus primeros días de vida que requieren múltiples cifras decimales (ej. 12.5 gramos = 0.0125 kg/ave/día)?  
    El sistema debe admitir una precisión numérica de hasta 4 cifras decimales en kilogramos (`0.0001 kg/ave/día`) en los campos de ración y aplicar redondeo estándar uniforme en los cálculos de demanda, impidiendo el truncamiento arbitrario que subestime el alimento necesario.

- **Edge case #5 - Demanda calculada con fracciones de bultos requeridos**

  - ¿Qué resultado presenta el sistema cuando la división entre la demanda diaria en kilogramos y el peso nominal del bulto produce un resultado con decimales (ej. 11.25 bultos)?  
    El sistema debe presentar el valor exacto fraccionario en bultos acompañado del total exacto en kilogramos netos, sin forzar redondeos a números enteros en la planificación para no distorsionar el saldo real de inventario ni los días de autonomía calculados.

- **Edge case #6 - Intento de fijar manualmente rangos de días con solapamientos o fechas invertidas**

  - ¿Cómo actúa el sistema si el usuario intenta ingresar un día de inicio menor o igual al día final de la etapa anterior, o un día final menor al día de inicio?  
    El sistema debe validar la secuencia temporal estricta y rechazar la configuración, reajustando automáticamente el inicio según la regla `DiaInicio = DiaFinEtapaAnterior + 1`. Asimismo, debe validar que `DiaFin >= DiaInicio` y que en etapas activas `DiaFin >= EdadActualLote`.

- **Edge case #7 - Desabastecimiento físico sobrevenido de un alimento planificado**

  - ¿Qué ocurre si un alimento comercial se planificó con estado "Sin stock" o sus existencias se agotan en bodega central antes de que el lote termine su etapa?  
    El sistema debe conservar inalterada la parametrización lógica y la proyección en el plan nutricional, sin invalidarla. La alerta y validación de disponibilidad física se traslada a la operación diaria de despacho ([SPEC-014]) y consulta de inventario ([SPEC-023]), permitiendo a bodega gestionar el reabastecimiento sin corromper la planificación.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir a los usuarios con rol de Nutricionista crear, consultar y actualizar plantillas maestras de planes nutricionales en el catálogo global de la granja (ej. Plan Estándar Broiler, Crecimiento Rápido).
- **FR-002**: Cada plantilla maestra DEBE contener la definición estructurada de sus etapas de crianza (Pre-inicio, Inicio, Crecimiento, Finalización/Engorde), estableciendo para cada una: rango de días base contiguos, ración diaria por ave en kilogramos (`kg/pollo/día`), tipo de alimento y bulto sugerido por defecto.
- **FR-003**: El sistema DEBE garantizar que para cada tipo de ave (ej. Broiler, Ponedora) exista a lo sumo UN ÚNICO plan nutricional marcado como predeterminado (`esPredeterminado = true`). Al marcar un plan como predeterminado, el sistema DEBE retirar automáticamente dicha marca de cualquier otro plan del mismo tipo de ave.
- **FR-004**: Al confirmar el ingreso de un nuevo lote a un galpón, el sistema DEBE asignar automáticamente una instancia desacoplada del plan nutricional predeterminado del tipo de ave correspondiente.
- **FR-005**: Si no existe un plan predeterminado activo para el tipo de ave del lote entrante, el sistema DEBE registrar el alojamiento, marcar el galpón en estado `"Pendiente de Asignación Nutricional"`, emitir una alerta prioritaria en la bandeja del nutricionista y fijar la demanda proyectada en 0.0 hasta su asignación manual.
- **FR-006**: Si el lote ingresa con una edad superior al día 1 de vida, el sistema DEBE marcar como `"Omitida"` toda etapa cuyo `DiaFin` sea menor a la edad de ingreso, activar inmediatamente la etapa que cubra la edad actual y computar el consumo a partir de dicho día.
- **FR-007**: El sistema DEBE permitir sustituir el plan nutricional de un galpón con lote activo mediante selección en modal. La sustitución DEBE preservar inmutables las etapas y consumos históricos transcurridos y acoplar el nuevo plan a partir de la etapa activa vigente, respetando el alimento asignado a la fase en curso.
- **FR-008**: Las modificaciones realizadas en la instancia de un galpón NO DEBEN alterar la plantilla maestra ni a los demás galpones, y los cambios en una plantilla maestra aplicarán exclusivamente a futuros lotes.
- **FR-009**: Cada producto comercial del catálogo de alimentos DEBE incluir su peso nominal por bulto (`pesoNominalPorBulto`), su perfil nutricional base (% proteína, energía) y, opcionalmente, la duración recomendada de la etapa en días (`duracionRecomendadaEtapaDias`).
- **FR-010**: Al configurar una etapa, el sistema DEBE consultar en tiempo real y mostrar únicamente los alimentos comerciales activos compatibles con el `TipoAlimento` de dicha etapa.
- **FR-011**: Para cada alimento compatible, el sistema DEBE mostrar: nombre comercial, peso nominal por bulto, perfil nutricional, duración recomendada en días, existencias disponibles en kg y bultos equivalentes en bodega central.
- **FR-012**: El sistema DEBE separar en filas independientes las presentaciones con diferentes pesos nominales por bulto y excluir recepciones vencidas o anuladas según las reglas del SPEC-023.
- **FR-013**: Los alimentos compatibles con existencia disponible igual a 0 kg DEBEN mostrarse identificados como "Sin stock", permitiendo su selección con una advertencia visual de reabastecimiento requerido en etapas no iniciadas.
- **FR-014**: El sistema DEBE bloquear estrictamente la asignación de cualquier alimento comercial cuyo `TipoAlimento` no corresponda a la etapa configurada.
- **FR-015**: Al seleccionar un alimento para una etapa no iniciada con `duracionRecomendadaEtapaDias` configurado, el sistema DEBE prellenar dicho valor en el campo de duración de la etapa en la interfaz y aplicarlo como duración total de la etapa al guardar la selección. Si dicho atributo es nulo, el sistema DEBE mantener la duración base de la etapa.
- **FR-016**: Al activarse una etapa en el galpón y generarse su proyección en el SPEC-022, el alimento comercial asignado DEBE quedar estrictamente bloqueado como inmutable para dicha etapa, prohibiendo cambios de insumo comercial durante el transcurso de la etapa activa para evitar inconsistencias de costo unitario y zootécnicas.
- **FR-017**: Las modificaciones sobre una etapa activa DEBEN limitarse exclusivamente al ajuste o prórroga de su duración en días (bajo las reglas de justificación del FR-021), conservando invariable el alimento asignado.
- **FR-018**: Al modificarse el día final de una etapa `k`, el sistema DEBE cerrar la cascada recalculando todas las etapas posteriores `i > k`: conservando sus días efectivos configurados (`diasEfectivos_i`), fijando `DiaInicio_i = DiaFin_{i-1} + 1` y `DiaFin_i = DiaInicio_i + diasEfectivos_i - 1`.
- **FR-019**: El recálculo de la última etapa del ciclo DEBE actualizar automáticamente la fecha proyectada de salida/sacrificio del lote en el sistema y emitir una notificación informativa.
- **FR-020**: El guardado de la asignación del alimento y las raciones en el plan nutricional DEBE operar como configuración lógica y NO DEBE descontar inventario, registrar consumos ni reservar bultos físicos en bodega central.
- **FR-021**: El sistema DEBE validar que la ración diaria por ave sea estrictamente positiva mayor a 0 (admitiendo hasta 4 decimales en kilogramos).
- **FR-022**: El sistema DEBE permitir ajustar manualmente los días de una etapa en un galpón ante contingencias, exigiendo obligatoriamente el registro de la justificación técnica del ajuste y el usuario responsable.
- **FR-023**: El sistema DEBE bloquear la modificación de etapas transcurridas en el galpón para proteger la integridad de los datos históricos.
- **FR-024**: El sistema DEBE calcular automáticamente la demanda diaria en kg mediante la fórmula: `DemandaKg = PoblacionAves * RacionKgPorPolloDia`.
- **FR-025**: El sistema DEBE calcular la demanda equivalente en bultos mediante la fórmula: `DemandaBultos = DemandaKg / PesoNetoKgPorBulto`.
- **FR-026**: El sistema DEBE calcular los días de autonomía de alimento comparando el stock disponible del alimento seleccionado en bodega central contra la demanda diaria del galpón.
- **FR-027**: El sistema DEBE mantener un registro auditable de todas las asignaciones y ajustes de planes nutricionales (timestamp, usuario, galpón, lote, valores anteriores y nuevos valores).

---

### Key Entities

- **PlanNutricionalPlantilla**: Plantilla maestra del plan en el catálogo global.
  - *Atributos*: ID, nombre (ej. Plan Estándar Broiler), descripcion, tipoAve, esPredeterminado (booleano, único por tipo de ave), activo, fechaCreacion, usuarioNutricionista.
  - *Relaciones*: contiene una lista de `PlanNutricionalEtapaPlantilla`.
- **PlanNutricionalEtapaPlantilla**: Configuración base de una etapa dentro de la plantilla maestra.
  - *Atributos*: ID, plantillaId, etapaCrianza (Pre-inicio, Inicio, Crecimiento, Finalización/Engorde), racionKgPolloDia, duracionDiasBase, tipoAlimentoId, alimentoSugeridoId.
- **PlanNutricionalGalpon**: Instancia activa del plan asignada y desacoplada para un galpón y lote específicos.
  - *Atributos*: ID, galponId, loteId, plantillaOrigenId, estado (ACTIVO, PENDIENTE_ASIGNACION, FINALIZADO), fechaAsignacion, usuarioAsignador.
  - *Relaciones*: contiene la lista de `CalendarioEtapaGalpon`.
- **CalendarioEtapaGalpon**: Configuración de la etapa para un galpón específico.
  - *Atributos*: ID, planGalponId, etapaCrianza, diaInicio, diaFin, diasEfectivos, estadoEtapa (OMITIDA, ACTIVA, COMPLETADA), alimentoBloqueado (booleano), racionKgPolloDia, alimentoId, pesoNetoKgPorBulto, justificacionAjuste, usuarioAjuste, fechaActualizacion.
  - *Relaciones*: referencia a `Alimento` y a `TipoAlimento`.
- **TipoAlimento**: Clasificación zootécnica vinculada a la etapa productiva (consistente con SPEC-001).
  - *Atributos*: ID, nombre (ej. Pre-iniciador, Iniciador, Engorde), etapaAsociada, descripcionNutricional, estado.
- **Alimento**: Producto comercial específico del catálogo de insumos (consistente con SPEC-001).
  - *Atributos*: ID, nombreComercial, marca, descripcion, duracionRecomendadaEtapaDias (entero opcional), pesoNominalPorBulto, estado.
  - *Relaciones*: pertenece a un `TipoAlimento` y se vincula con recepciones de bodega central.
- **Galpon**: Representa el galpón físico y el lote alojado.
  - *Atributos*: ID, nombre, capacidad, loteActivoId, poblacionAvesActivas, edadDiasLote, fechaProyectadaSalida.
- **ProyeccionConsumoGalpon**: Modelo de cálculo en tiempo de consulta.
  - *Atributos*: galpon, etapaActual, poblacionAves, alimentoSeleccionado, demandaDiariaKg, demandaDiariaBultos, stockDisponibleBultos, diasCoberturaRestantes.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% de los lotes nuevos de edad estándar reciben automáticamente la instancia de su plan predeterminado sin intervención manual obligatoria.
- **SC-002**: 100% de los alojamientos sin plan predeterminado activo activan el estado `"Pendiente de Asignación Nutricional"` con notificación prioritaria sin bloquear el registro del lote.
- **SC-003**: 100% de los lotes recibidos con edad > 1 día omiten las etapas previas e inician su cómputo estrictamente en la etapa correspondiente a su edad.
- **SC-004**: 100% de las etapas en estado activo mantienen su alimento comercial estrictamente bloqueado, registrando 0% de modificaciones de insumo a mitad de etapa.
- **SC-005**: 100% de las sustituciones de plan en lotes en curso conservan inalteradas las etapas y consumos históricos transcurridos.
- **SC-006**: 100% de los recálculos por perfil nutricional en etapas en planificación aplican la duración recomendada al confirmar la selección y cierran la cascada.
- **SC-007**: 100% de los desplazamientos en cascada recalculan el inicio y fin de etapas posteriores preservando sus días efectivos y actualizando la fecha proyectada de sacrificio.
- **SC-008**: 0% de movimientos o deducciones de inventario físico generados por configurar o guardar un plan nutricional.
- **SC-009**: 100% de las prórrogas y ajustes manuales de días en etapas activas exigen y persisten la justificación técnica obligatoria del usuario responsable conservando el mismo alimento comercial.
