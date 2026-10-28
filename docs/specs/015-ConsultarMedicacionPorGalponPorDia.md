# Feature Specification: 015 - Consultar medicación por galpón por día

**Created**: 2026-09-05

**Last Updated**: 2026-10-28

## User Scenarios & Testing

### User Story 1 - Consultar medicación y dosis a aplicar en el día (Priority: P1)

Como trabajador u operario de granja, quiero consultar qué medicación corresponde a cualquier galpón existente que seleccione para el día actual, visualizando la enfermedad diagnosticada, el medicamento prescrito, la dosis a aplicar hoy, la vía/instrucciones de administración y el avance del tratamiento, para suministrar el tratamiento médico exacto a las aves.

**Why this priority**: Es la consulta operativa diaria que guía la correcta administración de medicamentos en granja. Permite al trabajador conocer oportunamente si el lote alojado tiene un tratamiento activo en la fecha actual y cómo debe aplicarlo.

**Independent Test**: Se puede probar seleccionando cualquier galpón existente con lote activo y diagnóstico con medicación vigente en la fecha actual, verificando que la pantalla muestre los datos del galpón y del lote, la enfermedad, el medicamento, la dosis del día, la descripción/vía de administración y el progreso del tratamiento (`Día X de N`).

**Acceptance Scenarios**:

1. **Scenario**: Consulta exitosa de galpón con medicación activa para el día
   - **Given** que un trabajador autenticado selecciona cualquier galpón existente con un lote activo que cuenta con un tratamiento médico vigente para hoy
   - **When** ingresa a la consulta de medicación del galpón para la fecha actual
   - **Then** el sistema presenta los datos del galpón (nombre, aforo máximo, estado) y del lote (nombre, población actual, edad en días)
   - **And** presenta la enfermedad diagnosticada y el nombre del medicamento prescrito
   - **And** presenta la dosis de medicamento que se debe aplicar en el día, las instrucciones/vía de administración y el avance del tratamiento (ej. `"Día 2 de 5"`)

2. **Scenario**: Visualización de la dosis del día y avance del tratamiento
   - **Given** un tratamiento prescrito para 5 días que inició ayer (hoy es el día 2 de 5) con una dosis definida
   - **When** el trabajador realiza la consulta del galpón
   - **Then** el sistema indica claramente que el tratamiento está en curso en el `"Día 2 de 5"` y muestra la dosis e instrucciones a aplicar en la jornada

3. **Scenario**: Consulta de un galpón inexistente
   - **Given** que el identificador del galpón consultado no existe
   - **When** el trabajador solicita la consulta de medicación
   - **Then** el sistema informa que el galpón no fue encontrado y no devuelve tratamientos

---

### User Story 2 - Consultar galpón sin medicación activa para la fecha (Priority: P2)

Como trabajador u operario de granja, quiero que el sistema me informe con claridad cuando el galpón que seleccioné no tenga ningún tratamiento farmacológico prescrito o vigente para el día, para tener la certeza de no suministrar medicamentos innecesarios al lote.

**Why this priority**: Evita la administración errónea o accidental de fármacos en lotes sanos o cuyos tratamientos ya finalizaron.

**Independent Test**: Se puede probar seleccionando cualquier galpón existente sin diagnósticos activos o con tratamientos ya finalizados, verificando que el sistema muestre los datos disponibles del galpón/lote y el mensaje explícito `"Sin medicación activa para hoy"`, sin presentar dosis de aplicación.

**Acceptance Scenarios**:

1. **Scenario**: Consulta de galpón sin tratamientos activos
   - **Given** un galpón seleccionado con lote activo que no tiene diagnósticos ni tratamientos médicos vigentes en la fecha actual
   - **When** el trabajador consulta la medicación del galpón
   - **Then** el sistema presenta los datos del galpón y lote, y muestra el estado `"Sin medicación activa para hoy"`, sin indicar dosis a aplicar

2. **Scenario**: Consulta de galpón cuyo tratamiento ya finalizó en días anteriores
   - **Given** un lote activo cuyo tratamiento de 5 días finalizó ayer
   - **When** el trabajador consulta la medicación en la fecha actual
   - **Then** el sistema muestra que no existen tratamientos vigentes para el día y clasifica el estado como `"Sin medicación activa para hoy"`

---

### User Story 3 - Visualizar el resumen consolidado de tratamiento activo y medicación diaria de los galpones seleccionados (Priority: P2)

Como trabajador u operario de granja, quiero visualizar en mi pantalla de inicio la tarjeta métrica de tratamientos activos del día y un panel con el desglose de medicación diaria para los galpones que seleccione en esa consulta, detallando el galpón, lote, día de avance del tratamiento, enfermedad, medicamento, dosis de hoy y vía de administración, para conocer de inmediato qué tratamientos médicos debo preparar y aplicar en mi jornada de trabajo.

**Why this priority**: Permite al operario identificar al instante desde su pantalla de inicio si tiene galpones bajo medicación activa hoy, evitando omisiones de tratamientos veterinarios sin necesidad de ingresar individualmente a cada galpón.

**Independent Test**: Se puede probar autenticándose como un trabajador que selecciona los galpones 1, 3 y 5. Si únicamente el Galpón 5 (`Lote #LDP-005`) tiene un tratamiento activo para `Coccidiosis Aviar` con `Amprolio 20% Solución` en el día 2 de 5 (dosis hoy: `200 g diluidos en 100 L de agua potable`, vía: `Agua de bebida en bebederos automáticos`), la tarjeta métrica superior `"Tratamiento Activo Hoy"` debe mostrar `1 Galpón` y `Amprolio 20% en Galpón 5`, y el panel inferior `"Medicación Diaria (Spec 015)"` debe mostrar el badge `1 Tratamiento` y la tarjeta del Galpón 5 con sus 6 campos completos.

**Acceptance Scenarios**:

1. **Scenario**: Visualización de la tarjeta métrica y panel con un tratamiento activo
   - **Given** un trabajador autenticado que selecciona cualquier galpón existente, por ejemplo el Galpón 5 con `Lote #LDP-005` bajo tratamiento activo de `Coccidiosis Aviar` (`Día 2 de 5` con `Amprolio 20% Solución`, dosis `200 g diluidos en 100 L de agua potable`, vía `Agua de bebida en bebederos automáticos`)
   - **When** accede a su pantalla de inicio (`"Inicio de trabajador"`)
   - **Then** el sistema presenta la tarjeta métrica `"Tratamiento Activo Hoy"` indicando `1 Galpón` y el resumen `Amprolio 20% en Galpón 5`
   - **And** en el panel `"Medicación Diaria (Spec 015)"` muestra el badge `1 Tratamiento` y presenta la tarjeta detallando:
     - Encabezado: `Galpón 5 (Lote #LDP-005)`
     - Indicador de avance: `Día 2 de 5`
     - `Enfermedad`: Coccidiosis Aviar
     - `Medicamento`: Amprolio 20% Solución
     - `Dosis Hoy`: 200 g diluidos en 100 L de agua potable
     - `Vía de administración`: Agua de bebida en bebederos automáticos

2. **Scenario**: Visualización con múltiples tratamientos activos en galpones seleccionados
   - **Given** que el trabajador selecciona 2 galpones existentes con tratamientos activos en la fecha actual
   - **When** el trabajador consulta su pantalla de inicio
   - **Then** la tarjeta métrica muestra `2 Galpones` y el resumen de los tratamientos
   - **And** el panel muestra el badge `2 Tratamientos` desplegando una tarjeta individual para cada tratamiento con sus respectivos 6 campos de detalle

3. **Scenario**: Trabajador sin tratamientos activos en los galpones seleccionados para la consulta
   - **Given** que ninguno de los galpones que el trabajador seleccionó para esta consulta tiene tratamientos farmacológicos vigentes hoy
   - **When** el trabajador visualiza su pantalla de inicio
   - **Then** la tarjeta métrica muestra `0 Galpones` y el texto `Sin tratamientos hoy`
   - **And** el panel de medicación diaria presenta el badge `0 Tratamientos` junto con un mensaje visual informativo indicando que no hay tratamientos activos para la jornada

4. **Scenario**: Consulta de cualquier galpón existente sin restricción de asignación
   - **Given** que un galpón existente tiene tratamiento médico activo
   - **When** un trabajador autenticado con rol autorizado consulta ese galpón
   - **Then** el sistema muestra el tratamiento sin exigir una validación de asignación trabajador-galpón

---

### Edge Cases

- \*\*¿Qué sucede si un galp
