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

### Key Entities 

- **Galpón**: Representa la unidad física de producción avícola asignada al trabajador.
  - Atributos utilizados: `UUID único`, `Nombre`, `Aforo máximo`, `Estado`.
- **Lote**: Representa el grupo de aves registrado que referencia al galpón mediante llave foránea.
  - Atributos utilizados: `UUID único`, `Nombre`, `Población actual` (aves vivas), `Fecha de ingreso` (edad en días), `Llave foránea del galpón`.
- **Diagnóstico / Tratamiento de galpón**: Representa el diagnóstico emitido por el veterinario que asigna una medicación al lote activo.
  - Atributos utilizados: `UUID único`, `Enfermedad`, `Fecha de inicio`, `Duración en días`, `Estado del tratamiento`.
- **Medicación**: Representa la prescripción médica asociada al diagnóstico.
  - Atributos utilizados: `Medicamento` (nombre y principio activo), `Dosis prescrita`, `Cantidad a aplicar en el día`, `Descripción / Vía de administración`.
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

---

## Out of Scope

- La prescripción, definición y registro inicial de medicaciones (cubierto en SPEC-008 *Registrar medicación*).
- La emisión de diagnósticos y asignación de tratamientos por el veterinario (cubierto en SPEC-011 *Diagnosticar galpón*).
- El registro del consumo real y descuento de existencias en el inventario de medicamentos (cubierto en SPEC-013 *Registrar consumo de medicamento*).
- El registro de recepciones de medicamentos en bodega central (cubierto en SPEC-002 *Registro de medicamentos en bodega central*).
- La creación o administración de medicamentos maestros, galpones y lotes.
