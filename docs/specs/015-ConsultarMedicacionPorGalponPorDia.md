# Feature Specification: Consultar medicación por galpón por día

**Created**: 2026-09-05  

## User Scenarios & Testing 

### User Story 1 - Consultar medicación y cantidad a aplicar en el día (Priority: P1)

Como trabajador u operario de granja, quiero consultar qué medicación le corresponde a mi galpón asignado en el día actual, visualizando la enfermedad diagnosticada, el medicamento prescrito, la cantidad a aplicar hoy, la vía de administración y el avance del tratamiento, para suministrar el tratamiento médico exacto a las aves.

**Why this priority**: Es la consulta operativa crítica que guía la correcta administración de fármacos en granja. Permite al trabajador conocer oportunamente si el lote tiene un tratamiento activo en la fecha y cuánto medicamento debe aplicar.

**Independent Test**: Se puede probar seleccionando un galpón asignado con lote activo y diagnóstico con medicación vigente, verificando que la pantalla muestre los datos del galpón y lote, la enfermedad, el medicamento, la dosis/cantidad del día, la descripción de administración y el progreso del tratamiento (día X de N).

**Acceptance Scenarios**:

1. **Scenario**: Consulta exitosa de galpón con medicación activa para el día
   - **Given** que un trabajador autenticado selecciona un galpón asignado a su cargo con un lote activo que cuenta con un tratamiento médico vigente para hoy
   - **When** ingresa a la consulta de medicación del galpón para la fecha actual
   - **Then** el sistema presenta los datos del galpón (nombre, aforo, estado) y del lote (nombre, población actual, edad en días)
   - **And** presenta la enfermedad diagnosticada, el nombre del medicamento y su principio activo
   - **And** presenta la cantidad de medicamento que se debe aplicar en el día, las instrucciones de aplicación y el avance del tratamiento (ej. "Día 2 de 5")

2. **Scenario**: Visualización de la cantidad del día y avance del tratamiento
   - **Given** un tratamiento prescrito para 5 días que inició ayer (hoy es el día 2 de 5) con una dosis programada
   - **When** el trabajador realiza la consulta del galpón
   - **Then** el sistema indica claramente que el tratamiento está en curso en el "Día 2 de 5" y muestra la cantidad total de medicamento a aplicar en la jornada

3. **Scenario**: Intento de consulta de un galpón no asignado al trabajador
   - **Given** que un trabajador intenta consultar la medicación de un galpón que no tiene a su cargo
   - **When** solicita acceder a la información de dicho galpón
   - **Then** el sistema deniega el acceso y muestra únicamente los galpones asignados bajo su responsabilidad

---

### User Story 2 - Consultar galpón sin medicación activa para la fecha (Priority: P2)

Como trabajador u operario de granja, quiero que el sistema me informe con claridad cuando un galpón asignado no tenga ningún tratamiento farmacológico prescrito o vigente para el día, para tener la certeza de no suministrar medicamentos innecesarios al lote.

**Why this priority**: Evita la administración errónea o accidental de fármacos en lotes sanos o en periodos de retiro previos al sacrificio.

**Independent Test**: Se puede probar seleccionando un galpón asignado sin diagnósticos activos o con tratamientos finalizados, verificando que el sistema muestre los datos del galpón/lote y el mensaje explícito "Sin medicación activa para hoy", sin presentar cantidades de aplicación.

**Acceptance Scenarios**:

1. **Scenario**: Consulta de galpón sin tratamientos activos
   - **Given** un galpón asignado con lote activo que no tiene diagnósticos ni tratamientos médicos vigentes en la fecha
   - **When** el trabajador consulta la medicación del galpón
   - **Then** el sistema presenta los datos del galpón y lote, y muestra el estado "Sin medicación activa para hoy", sin indicar cantidades a aplicar

2. **Scenario**: Consulta de galpón cuyo tratamiento ya finalizó en días anteriores
   - **Given** un lote activo cuyo tratamiento de 5 días finalizó ayer
   - **When** el trabajador consulta la medicación en la fecha actual
   - **Then** el sistema muestra que no existen tratamientos vigentes para el día y clasifica el estado como "Sin medicación activa para hoy"

---

### User Story 3 - Visualizar el resumen consolidado de tratamiento activo y medicación diaria de los galpones asignados (Priority: P2)

Como trabajador u operario de granja, quiero visualizar en mi pantalla de inicio la tarjeta métrica de tratamientos activos del día y un panel con el desglose de medicación diaria para mis galpones asignados, detallando el galpón, lote, día de avance del tratamiento, enfermedad, medicamento, dosis de hoy y vía de administración, para conocer de inmediato qué tratamientos médicos debo preparar y aplicar en mi jornada de trabajo.

**Why this priority**: Permite al operario identificar al instante desde el dashboard ("Inicio de trabajador") si tiene galpones bajo medicación activa hoy, evitando omisiones de tratamientos veterinarios críticos y suministrando las dosis y vías de administración indicadas sin necesidad de ingresar individualmente a cada galpón.

**Independent Test**: Se puede probar autenticándose como un trabajador que tiene asignados los galpones 1, 3 y 5, donde únicamente el Galpón 5 (Lote #LDP-005) tiene un tratamiento activo para Coccidiosis Aviar con Amprolio 20% Solución en el día 2 de 5 (dosis hoy: 200g diluidos en 100L de agua potable, vía: agua de bebida en bebederos automáticos). Verificar que la tarjeta métrica superior "Tratamiento Activo Hoy" muestre `1 Galpón` y `Amprolio 20% en Galpón 5`, y que el panel inferior "Medicación Diaria (Spec 015)" muestre el badge `1 Tratamiento` y la tarjeta de detalle del Galpón 5 con sus 6 campos completos.

**Acceptance Scenarios**:

1. **Scenario**: Visualización de la tarjeta métrica y panel con un tratamiento activo
   - **Given** un trabajador autenticado que tiene asignado el Galpón 5 con Lote #LDP-005 bajo tratamiento activo de Coccidiosis Aviar (Día 2 de 5 con Amprolio 20% Solución, dosis 200g en 100L de agua potable, vía agua de bebida)
   - **When** accede a su pantalla de inicio ("Inicio de trabajador")
   - **Then** el sistema presenta la tarjeta métrica "Tratamiento Activo Hoy" indicando `1 Galpón` y el subtítulo `Amprolio 20% en Galpón 5`
   - **And** en el panel "Medicación Diaria (Spec 015)" muestra el badge `1 Tratamiento` y presenta la tarjeta detallando:
     - Encabezado: `Galpón 5 (Lote #LDP-005)`
     - Indicador de avance: `Día 2 de 5`
     - `Enfermedad`: Coccidiosis Aviar
     - `Medicamento`: Amprolio 20% Solución
     - `Dosis Hoy`: 200g diluidos en 100L de agua potable
     - `Vía de administración`: Agua de bebida en bebederos automáticos

2. **Scenario**: Visualización con múltiples tratamientos activos en galpones asignados
   - **Given** que el trabajador tiene asignados 2 galpones con tratamientos activos en la fecha actual
   - **When** el trabajador consulta su pantalla de inicio
   - **Then** la tarjeta métrica muestra `2 Galpones` y el resumen de los tratamientos
   - **And** el panel muestra el badge `2 Tratamientos` desplegando una tarjeta individual para cada tratamiento con sus respectivos 6 campos de detalle

3. **Scenario**: Trabajador sin tratamientos activos en sus galpones asignados
   - **Given** que ninguno de los galpones asignados al trabajador tiene tratamientos farmacológicos vigentes hoy
   - **When** el trabajador visualiza su pantalla de inicio
   - **Then** la tarjeta métrica muestra `0 Galpones` y el texto `Sin tratamientos hoy`
   - **And** el panel de medicación diaria presenta el badge `0 Tratamientos` junto con un mensaje visual informativo indicando que no hay tratamientos activos para la jornada

4. **Scenario**: Aislamiento estricto de galpones no asignados
   - **Given** que existen otros galpones en la granja con tratamientos médicos activos pero asignados a otros operarios
   - **When** el trabajador ingresa a su pantalla de inicio
   - **Then** el sistema filtra rigurosamente y no muestra ningún tratamiento perteneciente a galpones que no estén bajo su responsabilidad directa

---

### Edge Cases

- **¿Qué sucede si un galpón no cuenta con un lote activo registrado?**
  - El sistema muestra los datos del galpón, informa que no posee lote activo y presenta el estado sin medicación activa.

- **¿Qué sucede si la población actual de aves vivas en el lote es 0?**
  - El sistema muestra la información del lote con 0 aves vivas e indica que no hay medicación aplicable.

- **¿Qué sucede si un galpón tiene dos o más tratamientos activos simultáneos en la fecha?**
  - El sistema lista de forma independiente cada tratamiento activo con su respectiva enfermedad, medicamento, cantidad del día y avance.

- **¿La consulta permite registrar el consumo real o descontar inventario de medicamentos?**
  - No, la funcionalidad es estrictamente de solo lectura; el registro del consumo real aplicado y el descuento de inventario corresponden al SPEC-013.

- **¿Qué sucede si el tratamiento prescrito se encuentra en periodo de descanso o días alternos?**
  - El sistema indica que hoy corresponde día sin aplicación dentro del tratamiento y muestra 0 de cantidad a aplicar en la jornada.

- **¿Qué sucede si un usuario no autenticado o sin rol de trabajador intenta acceder?**
  - El sistema bloquea la consulta y exige autenticación con un usuario autorizado.

---

## Requirements 

### Functional Requirements

- **FR-001**: El sistema DEBE permitir la consulta de medicación por galpón por día a usuarios autenticados con rol de trabajador (y administradores autorizados).
- **FR-002**: El sistema DEBE restringir la visualización y consulta de galpones para el trabajador exclusivamente a aquellos que tenga formalmente asignados a su cargo.
- **FR-003**: Para el galpón consultado, el sistema DEBE obtener y presentar sus atributos: UUID, Nombre, Aforo máximo y Estado.
- **FR-004**: El sistema DEBE identificar el lote activo del galpón mediante la llave foránea que referencia al galpón, seleccionando el lote con la fecha de ingreso más reciente.
- **FR-005**: Para el lote activo, el sistema DEBE presentar: UUID único, Nombre, Población actual (aves vivas) y Fecha de ingreso (edad en días).
- **FR-006**: Para la fecha actual, el sistema DEBE consultar los diagnósticos y medicaciones vigentes asociados al lote activo del galpón.
- **FR-007**: Si el galpón cuenta con tratamiento activo en la fecha, el sistema DEBE mostrar: enfermedad diagnosticada, nombre del medicamento, principio activo, cantidad de medicamento a aplicar en el día, instrucciones de aplicación y avance del tratamiento (día actual vs total de días).
- **FR-008**: Si el galpón NO cuenta con tratamientos activos en la fecha, el sistema DEBE mostrar explícitamente el estado `Sin medicación activa para hoy` y NO DEBE mostrar cantidades a aplicar.
- **FR-009**: El sistema DEBE calcular el avance del tratamiento determinando el número de día en curso respecto a la duración total prescrita por el veterinario.
- **FR-010**: La consulta de medicación por galpón por día DEBE enfocarse exclusivamente en la información correspondiente a la fecha actual del sistema.
- **FR-011**: La funcionalidad de consulta DEBE operar estrictamente en modo de solo lectura y NO DEBE alterar inventarios de medicamentos, diagnósticos ni datos del galpón o lote.
- **FR-012**: El sistema DEBE presentar cada tratamiento en forma desglosada y separada si el galpón registra múltiples tratamientos activos concurrentes en el día.
- **FR-013**: El sistema DEBE denegar el acceso a la consulta a usuarios sin permisos o que intenten acceder a galpones no asignados.
- **FR-014**: El sistema DEBE calcular y presentar en la pantalla de inicio del trabajador la tarjeta métrica "Tratamiento Activo Hoy", indicando la cantidad de galpones asignados que tienen al menos un tratamiento farmacológico activo en la fecha actual (ej. `1 Galpón`) y un resumen informativo del tratamiento y galpón correspondiente (ej. `Amprolio 20% en Galpón 5`), o `0 Galpones` si no registra medicaciones vigentes.
- **FR-015**: El sistema DEBE presentar en la pantalla de inicio del trabajador el panel consolidado "Medicación Diaria (Spec 015)", mostrando un badge con el total de tratamientos activos en sus galpones (ej. `1 Tratamiento`) y listando tarjetas individuales para cada tratamiento vigente de sus galpones asignados.
- **FR-016**: Cada tarjeta de tratamiento dentro del panel de medicación diaria DEBE detallar de manera obligatoria:
  1. Identificador del galpón y código del lote activo (ej. `Galpón 5 (Lote #LDP-005)`).
  2. Indicador visual o badge con el avance del tratamiento expresado como `Día X de N` (días transcurridos vs duración prescrita).
  3. `Enfermedad`: nombre de la patología diagnosticada (ej. `Coccidiosis Aviar`).
  4. `Medicamento`: denominación y presentación o concentración prescrita (ej. `Amprolio 20% Solución`).
  5. `Dosis Hoy`: cantidad y forma de dilución o preparación prescrita para el día (ej. `200g diluidos en 100L de agua potable`).
  6. `Vía de administración`: método específico de aplicación en el galpón (ej. `Agua de bebida en bebederos automáticos`).
- **FR-017**: El sistema DEBE filtrar la información de la tarjeta métrica y del panel de medicación diaria para mostrar exclusivamente tratamientos aplicables a galpones que se encuentren formalmente asignados al trabajador autenticado.

### Key Entities 

- **Galpón**: Representa la unidad física de producción avícola asignada al trabajador.
  - Atributos utilizados: `UUID único`, `Nombre`, `Aforo máximo`, `Estado`.
- **Lote**: Representa el grupo de aves registrado que referencia al galpón mediante llave foránea.
  - Atributos utilizados: `UUID único`, `Nombre`, `Población actual` (aves vivas), `Fecha de ingreso` (edad en días), `Llave foránea del galpón`.
- **Diagnóstico / Tratamiento de galpón**: Representa el diagnóstico emitido por el veterinario que asigna una medicación al lote activo.
  - Atributos utilizados: `UUID único`, `Enfermedad`, `Fecha de inicio`, `Duración en días`, `Estado del tratamiento`.
- **Medicación**: Representa la prescripción médica asociada al diagnóstico.
  - Atributos utilizados: `Medicamento` (nombre y principio activo), `Dosis prescrita`, `Cantidad a aplicar en el día`, `Descripción / Vía de administración`.
- **Resumen consolidado de medicación diaria del trabajador**: Agrupación para la pantalla de inicio de los tratamientos médicos activos hoy en los galpones asignados al operario.
  - *Atributos calculados*: Total de galpones con tratamiento activo, Total de tratamientos vigentes, Resumen de medicamento y galpón principal, y Lista de tarjetas de tratamiento con galpón, lote, avance (`Día X de N`), enfermedad, medicamento, dosis de hoy y vía de administración.
- **Trabajador / Operario de granja**: Usuario autenticado que consulta las indicaciones médicas para su suministro en granja.

---

## Success Criteria 

### Measurable Outcomes

- **SC-001**: El 100 % de las consultas de un trabajador muestra únicamente los galpones asignados a su cargo con su nombre, aforo y estado.
- **SC-002**: El 100 % de los galpones con tratamiento vigente muestra con exactitud la enfermedad, medicamento, cantidad a aplicar en el día, instrucciones y avance del tratamiento (día X de N).
- **SC-003**: El 100 % de los galpones sin tratamientos activos en la fecha muestra de forma inequívoca el estado `Sin medicación activa para hoy`.
- **SC-004**: En el 100 % de los casos, la consulta identifica correctamente el lote activo más reciente mediante su llave foránea y muestra su población viva actual.
- **SC-005**: El 100 % de las operaciones de consulta se ejecuta en menos de 2 segundos sin modificar existencias ni registros de la base de datos (garantía de solo lectura).
- **SC-006**: El 100 % de los intentos de consulta en galpones no asignados es bloqueado por el sistema.
- **SC-007**: Si existen múltiples tratamientos concurrentes, el 100 % de ellos se muestra desglosado de forma independiente.
- **SC-008**: El 100 % de las pantallas de inicio de trabajadores refleja en la tarjeta métrica "Tratamiento Activo Hoy" el conteo exacto de galpones con tratamientos vigentes a su cargo y el resumen del fármaco y galpón.
- **SC-009**: El 100 % de las tarjetas de tratamiento presentadas en el panel de medicación diaria incluye con precisión los 6 datos requeridos: galpón/lote, avance (`Día X de N`), enfermedad, medicamento, dosis de hoy y vía de administración, restringidas a los galpones asignados.

---

## Out of Scope

- La prescripción, definición y registro inicial de medicaciones (cubierto en SPEC-008 *Registrar medicación*).
- La emisión de diagnósticos y asignación de tratamientos por el veterinario (cubierto en SPEC-011 *Diagnosticar galpón*).
- El registro del consumo real y descuento de existencias en el inventario de medicamentos (cubierto en SPEC-013 *Registrar consumo de medicamento*).
- El registro de recepciones de medicamentos en bodega central (cubierto en SPEC-002 *Registro de recepción de medicamento*).
- La creación o administración de medicamentos maestros, galpones y lotes.
