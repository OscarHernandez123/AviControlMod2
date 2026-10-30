# Feature Specification: Consultar información de un galpón

**Created**: 2026-09-03  

## User Scenarios & Testing

### User Story 1 - Consultar la información de un galpón (Priority: P1)

Como administrador o usuario, quiero consultar la información actual de un galpón para conocer su capacidad, estado y las condiciones del lote de aves que tiene alojado.

**Why this priority**: Esta consulta reúne la información operativa del galpón y de su lote actual en una sola vista, permitiendo que los roles autorizados conozcan su ocupación y avance sin revisar datos por separado.

**Independent Test**: Se puede probar seleccionando un galpón que tenga un lote alojado actualmente cuya fecha de ingreso fue hace 16 días. El sistema debe mostrar el nombre, aforo máximo y estado recibidos del módulo 1, además de la población actual del lote y una edad calculada de `2 semanas y 3 días (17 días)`.

**Acceptance Scenarios**:

1. **Scenario**: Consulta de un galpón con un lote alojado actualmente
   - **Given** que un administrador o usuario autenticado selecciona un galpón con un único lote alojado actualmente
   - **When** consulta la información del galpón
   - **Then** el sistema muestra el nombre, aforo máximo, estado, población actual y edad del lote

2. **Scenario**: Datos obtenidos y calculados correctamente
   - **Given** que el módulo 1 proporciona el galpón, su lote actualmente alojado y la fecha de ingreso del lote
   - **When** el sistema presenta la consulta
   - **Then** obtiene el nombre, aforo máximo y estado de la entidad Galpón, obtiene la población actual de la entidad Lote y calcula la edad del lote a partir de su fecha de ingreso

3. **Scenario**: Consulta de un galpón sin lote alojado actualmente
   - **Given** que un administrador o usuario autenticado selecciona un galpón que no tiene un lote alojado actualmente
   - **When** consulta la información del galpón
   - **Then** el sistema muestra el nombre, aforo máximo y estado del galpón, e indica que la población actual y la edad del lote no están disponibles porque no existe un lote alojado actualmente

4. **Scenario**: Consulta por un rol no autorizado
   - **Given** que una persona sin rol de administrador ni de usuario intenta consultar un galpón
   - **When** solicita la consulta
   - **Then** el sistema rechaza la operación y no muestra la información del galpón ni del lote

---

### User Story 2 - Consultar resumen general de galpones (Priority: P2)

Como administrador, quiero visualizar en la pantalla de inicio la cantidad total de galpones y su distribución por estado para conocer rápidamente la situación general de la operación sin consultar cada galpón por separado.

**Why this priority**: El resumen ofrece una vista operativa inmediata, mientras que la consulta individual continúa proporcionando el detalle de un galpón y su lote.

**Independent Test**: Se puede probar utilizando doce galpones proporcionados por el módulo 1, distribuidos entre los estados permitidos, y verificando que la pantalla de inicio muestre un total de doce y que la suma de los conteos por estado también sea doce.

**Acceptance Scenarios**:

1. **Scenario**: Visualización correcta del resumen de galpones
   - **Given** que el módulo 1 proporciona galpones con un único estado vigente cada uno
   - **When** el administrador ingresa a la pantalla de inicio
   - **Then** el sistema muestra la cantidad total de galpones y el número correspondiente a cada estado: `disponible`, `vaciado sanitario`, `productiva`, `en cosecha`, `mantenimiento` y `aislamiento`

2. **Scenario**: Resumen cuando no existen galpones
   - **Given** que el módulo 1 no tiene galpones registrados
   - **When** el administrador ingresa a la pantalla de inicio
   - **Then** el sistema muestra un total de cero y un conteo de cero para cada estado permitido

3. **Scenario**: Consulta del resumen por un usuario no autorizado
   - **Given** que una persona sin rol de administrador intenta consultar el resumen general
   - **When** solicita acceder a la pantalla de inicio del administrador
   - **Then** el sistema rechaza el acceso y no muestra los conteos de galpones

4. **Scenario**: Resumen estrictamente de solo lectura
   - **Given** que el administrador visualiza el resumen general de galpones
   - **When** actualiza o vuelve a abrir la pantalla de inicio
   - **Then** el sistema obtiene los estados vigentes desde el módulo 1 sin modificar ningún galpón ni lote

---

### User Story 3 - Visualizar galpones asignados al trabajador (Priority: P2)

Como trabajador u operario de granja, quiero visualizar en mi pantalla de inicio la cantidad total de galpones asignados a mi cargo, la sumatoria de aves vivas bajo mi responsabilidad y el listado de mis galpones asignados detallando el nombre del galpón, identificador del lote, su estado, población actual, edad del lote en días, la dieta del día de hoy y su ración diaria requerida, para tener un control general e inmediato de mi asignación antes de realizar las labores de la jornada.

**Why this priority**: Es la pantalla de entrada operativa del trabajador ("Inicio de trabajador"), permitiéndole conocer en una sola vista cuántos galpones tiene a su cargo y cuántas aves vivas gestiona en total, además del estado nutricional y operativo de cada galpón, restringiendo el acceso exclusivamente a los galpones asignados bajo su responsabilidad formal.

**Independent Test**: Se puede probar autenticándose como un trabajador que tiene asignados tres galpones en el módulo 1 (por ejemplo, Galpón 1 con 5.000 aves, 38 días, dieta Engorde Stage 2 y ración de 450 kg / 9 bultos; Galpón 3 con 4.800 aves, 35 días, dieta Engorde Stage 2 y ración de 430 kg / 8.6 bultos; y Galpón 5 con 4.000 aves y 41 días), verificando que la interfaz presente la tarjeta "Mis Galpones Asignados" con el valor 3 y "13.800 Aves Vivas", y liste exclusivamente las tarjetas de esos 3 galpones con sus respectivos nombres, lotes activos, estados, poblaciones vivas, edades en días, dietas de hoy y raciones diarias (kg y bultos), excluyendo galpones no asignados.

**Acceptance Scenarios**:

1. **Scenario**: Visualización de métricas de galpones asignados y aves vivas
   - **Given** que un trabajador autenticado tiene asignados 3 galpones a su cargo en el módulo 1 con lotes activos cuyas poblaciones son 5.000, 4.800 y 4.000 aves vivas
   - **When** accede a su pantalla de inicio ("Inicio de trabajador")
   - **Then** el sistema muestra la tarjeta métrica "Mis Galpones Asignados" con la cantidad de 3 galpones
   - **And** muestra la sumatoria consolidada de 13.800 aves vivas bajo su cargo

2. **Scenario**: Listado de tarjetas de galpones a su cargo
   - **Given** que el trabajador autenticado tiene galpones asignados con lotes activos
   - **When** consulta la sección "Galpones a su Cargo"
   - **Then** el sistema presenta una tarjeta por cada galpón asignado mostrando: nombre del galpón, identificador del lote activo, estado vigente, población viva actual, edad del lote en días, dieta del día de hoy y ración diaria requerida (expresada en kilogramos y bultos)
   - **And** restringe la visualización exclusivamente a los galpones asignados a dicho trabajador

3. **Scenario**: Trabajador sin galpones asignados
   - **Given** un trabajador autenticado que no tiene galpones asignados formalmente a su cargo
   - **When** ingresa a su pantalla de inicio
   - **Then** el sistema muestra 0 galpones asignados, 0 aves vivas y presenta un mensaje indicando que no tiene galpones a su cargo actualmente

4. **Scenario**: Intento de acceso a galpones no asignados
   - **Given** que existen galpones registrados en la granja asignados a otros operarios
   - **When** el trabajador visualiza su pantalla de inicio
   - **Then** el sistema filtra rigurosamente la consulta y no muestra ningún galpón ajeno a su asignación

---

### Edge Cases

- **Edge case #1 - Fecha de ingreso posterior a la fecha actual**

  - ¿Cómo maneja el sistema un lote actualmente alojado cuya fecha de ingreso es posterior a la fecha actual?  
    El sistema debe mostrar los demás datos disponibles, impedir el cálculo de una edad negativa e indicar que no puede calcular la edad porque la fecha de ingreso es inconsistente.

- **Edge case #2 - Información incompleta o no disponible desde el módulo 1**

  - ¿Cómo maneja el sistema una consulta cuando el módulo 1 no proporciona alguno de los datos requeridos?  
    El sistema debe identificar los datos que no están disponibles, no sustituirlos por información inventada ni desactualizada y comunicar que la consulta no pudo completarse.

- **Edge case #3 - Cálculo entre meses, años o durante un año bisiesto**

  - ¿Cómo maneja el sistema una edad cuyo periodo atraviesa meses con diferente cantidad de días, un cambio de año o el 29 de febrero?  
    El sistema debe calcular los días calendario transcurridos desde la fecha de ingreso, incluyendo el día de ingreso como el primer día, sin asumir que todos los meses o años tienen la misma duración.

- **Edge case #4 - Galpón sin estado o con un estado no permitido**

  - ¿Cómo se contabiliza un galpón cuyo estado está ausente o no corresponde a uno de los seis estados permitidos?
    El sistema debe informar una inconsistencia, excluirlo de los conteos por estado y mostrar por separado la cantidad de registros inconsistentes. No debe asignarlo arbitrariamente a otro estado ni modificarlo desde la consulta.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir la consulta de información de un galpón exclusivamente a personas autenticadas con rol de administrador o de usuario.
- **FR-002**: El sistema DEBE mostrar el nombre, aforo máximo, estado, población actual y edad del lote correspondiente al galpón seleccionado.
- **FR-003**: El sistema DEBE obtener el nombre, aforo máximo y estado desde la entidad Galpón proporcionada por el módulo 1.
- **FR-004**: El sistema DEBE identificar el único lote alojado actualmente en el galpón y obtener su población actual desde la entidad Lote proporcionada por el módulo 1.
- **FR-005**: El sistema DEBE calcular la edad del lote utilizando su fecha de ingreso, proporcionada como atributo de la entidad Lote por el módulo 1, y la fecha actual del sistema.
- **FR-006**: Cuando el galpón no tenga un lote alojado actualmente, el sistema DEBE mostrar los datos disponibles de la entidad Galpón e indicar que la población actual y la edad del lote no están disponibles.
- **FR-007**: La consulta DEBE utilizar la información vigente proporcionada por el módulo 1 y NO DEBE modificar las entidades Galpón o Lote.
- **FR-008**: El sistema DEBE permitir la consulta del resumen general de galpones exclusivamente a usuarios autenticados con rol de administrador.
- **FR-009**: Cada galpón DEBE tener un único estado vigente entre `disponible`, `vaciado sanitario`, `productiva`, `en cosecha`, `mantenimiento` o `aislamiento`.
- **FR-010**: El resumen DEBE mostrar la cantidad total de galpones y el conteo individual de galpones para cada uno de los seis estados permitidos.
- **FR-011**: La suma de los conteos por estado DEBE coincidir con el total de galpones válidos incluidos en el resumen.
- **FR-012**: El resumen DEBE obtener la información vigente del módulo 1 y operar estrictamente en modo de solo lectura.
- **FR-013**: Los galpones sin estado o con un estado no permitido NO DEBEN incluirse en otro estado; el sistema DEBE informar su cantidad como registros inconsistentes.
- **FR-014**: El sistema DEBE permitir la consulta de galpones asignados a usuarios autenticados con rol de trabajador / operario de granja.
- **FR-015**: El sistema DEBE restringir la visualización de galpones en la pantalla de inicio del trabajador exclusivamente a aquellos formalmente asignados a su cargo.
- **FR-016**: El sistema DEBE calcular y presentar en la pantalla de inicio del trabajador la cantidad total de galpones asignados a su cargo.
- **FR-017**: El sistema DEBE calcular y mostrar la sumatoria consolidada de aves vivas (población actual) de todos los lotes activos correspondientes a los galpones asignados al trabajador.
- **FR-018**: Para cada galpón asignado presentado en la sección de galpones a su cargo, el sistema DEBE mostrar obligatoriamente: nombre del galpón, identificador del lote activo, estado operativo vigente, población actual de aves vivas, edad del lote calculada en días, la dieta correspondiente a la fecha actual (tipo de alimento o etapa) y la ración diaria requerida expresada simultáneamente en kilogramos netos y en bultos equivalentes.

### Key Entities

- **Galpón**: Representa el espacio consultado y es proporcionado por el módulo 1.
  - **Atributos utilizados**: nombre, aforo máximo y un único estado entre `disponible`, `vaciado sanitario`, `productiva`, `en cosecha`, `mantenimiento` o `aislamiento`.
  - **Relaciones**: puede alojar un lote de aves actualmente y diferentes lotes a lo largo del tiempo.
- **Lote**: Representa el grupo de aves alojado actualmente en el galpón y es proporcionado por el módulo 1.
  - **Atributos utilizados**: población actual y fecha de ingreso.
  - **Datos derivados**: edad actual calculada por el sistema.
  - **Relaciones**: se encuentra alojado en un galpón durante su ciclo de crianza.
- **Resumen general de galpones**: Representa la vista agregada de los galpones vigentes para la pantalla de inicio del administrador.
  - **Datos mostrados**: cantidad total de galpones, conteo por cada estado permitido y cantidad de registros inconsistentes.
  - **Origen**: se calcula en cada consulta a partir de los galpones proporcionados por el módulo 1 y no se utiliza para modificar sus estados.
- **Resumen de galpones asignados al trabajador**: Representa la consolidación de galpones bajo responsabilidad del operario para su pantalla de inicio.
  - **Datos mostrados**: cantidad total de galpones asignados, población total acumulada de aves vivas y listado de tarjetas de galpones asignados con nombre, lote, estado, población actual, edad en días, dieta de hoy y ración diaria (en kg y bultos).
  - **Restricción**: filtrado estricto por el identificador del trabajador autenticado.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los administradores y usuarios puede consultar la información de un galpón en menos de 20 segundos.
- **SC-002**: El 95 % de las consultas muestra todos los datos disponibles en un máximo de 1 segundo después de seleccionar el galpón.
- **SC-003**: El 100 % de las edades mostradas coincide con los días calendario transcurridos desde la fecha de ingreso, incluyendo el día de ingreso como el primer día.
- **SC-004**: Al menos el 95 % de los administradores y usuarios identifica correctamente el nombre, aforo máximo, estado, población actual y edad del lote en el primer intento durante pruebas de usabilidad.
- **SC-005**: El 100 % de los resúmenes contabiliza cada galpón válido exactamente una vez según su único estado vigente.
- **SC-006**: El 95 % de los resúmenes muestra el total y los conteos por estado en un máximo de 1 segundo.
- **SC-007**: El 100 % de las consultas del resumen se ejecuta sin modificar galpones ni lotes del módulo 1.
- **SC-008**: El 100 % de las pantallas de inicio de trabajadores muestra únicamente los galpones asignados a su cargo, calculando con exactitud la cantidad total de galpones, la sumatoria acumulada de aves vivas y detallando en cada tarjeta: nombre, lote, estado, población viva, edad, dieta de hoy y ración diaria en kilogramos y bultos.
