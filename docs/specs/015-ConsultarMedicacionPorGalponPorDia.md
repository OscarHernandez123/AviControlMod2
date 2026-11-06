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

### User Story 3 - Visualizar el resumen consolidado de tratamiento activo y medicación diaria de los galpones activos (Priority: P2)

Como trabajador u operario de granja, quiero visualizar en mi pantalla de inicio la tarjeta métrica de tratamientos activos del día y un panel con el desglose de medicación diaria para los galpones activos, detallando el galpón, lote, día de avance del tratamiento, enfermedad, medicamento, dosis de hoy y vía de administración, para conocer de inmediato qué tratamientos médicos debo preparar y aplicar en mi jornada de trabajo.

**Why this priority**: Permite al operario identificar al instante desde el dashboard ("Inicio de trabajador") si tiene galpones bajo medicación activa hoy, evitando omisiones de tratamientos veterinarios críticos y suministrando las dosis y vías de administración indicadas sin necesidad de ingresar individualmente a cada galpón.

**Independent Test**: Se puede probar autenticándose como un trabajador con acceso a galpones activos bajo tratamiento, donde el Galpón 5 (Lote #LDP-005) tiene un tratamiento activo para Coccidiosis Aviar con Amprolio 20% Solución en el día 2 de 5 (dosis hoy: 200g diluidos en 100L de agua potable, vía: agua de bebida en bebederos automáticos). Verificar que la tarjeta métrica superior "Tratamiento Activo Hoy" muestre `1 Galpón` y `Amprolio 20% en Galpón 5`, y que el panel inferior "Medicación Diaria (Spec 015)" muestre el badge `1 Tratamiento` y la tarjeta de detalle del Galpón 5 con sus 6 campos completos.

**Acceptance Scenarios**:

1. **Scenario**: Visualización de la tarjeta métrica y panel con un tratamiento activo
   - **Given** un trabajador autenticado que consulta galpones activos y observa que el Galpón 5 con Lote #LDP-005 tiene tratamiento activo de Coccidiosis Aviar (Día 2 de 5 con Amprolio 20% Solución, dosis 200g en 100L de agua potable, vía agua de bebida)
   - **When** accede a su pantalla de inicio ("Inicio de trabajador")
   - **Then** el sistema presenta la tarjeta métrica "Tratamiento Activo Hoy" indicando `1 Galpón` y el subtítulo `Amprolio 20% en Galpón 5`
   - **And** en el panel "Medicación Diaria (Spec 015)" muestra el badge `1 Tratamiento` y presenta la tarjeta detallando:
     - Encabezado: `Galpón 5 (Lote #LDP-005)`
     - Indicador de avance: `Día 2 de 5`
     - `Enfermedad`: Coccidiosis Aviar
     - `Medicamento`: Amprolio 20% Solución
     - `Dosis Hoy`: 200g diluidos en 100L de agua potable
     - `Vía de administración`: Agua de bebida en bebederos automáticos

2. **Scenario**: Visualización con múltiples tratamientos activos en galpones activos
   - **Given** que el trabajador tiene acceso a 2 galpones con tratamientos activos en la fecha actual
   - **When** el trabajador consulta su pantalla de inicio
   - **Then** la tarjeta métrica muestra `2 Galpones` y el resumen de los tratamientos
   - **And** el panel muestra el badge `2 Tratamientos` desplegando una tarjeta individual para cada tratamiento con sus respectivos 6 campos de detalle

3. **Scenario**: Trabajador sin tratamientos activos
   - **Given** que no existen tratamientos farmacológicos vigentes hoy en los galpones activos accesibles al trabajador
   - **When** el trabajador visualiza su pantalla de inicio
   - **Then** la tarjeta métrica muestra `0 Galpones` y el texto `Sin tratamientos hoy`
   - **And** el panel de medicación diaria presenta el badge `0 Tratamientos` junto con un mensaje visual informativo indicando que no hay tratamientos activos para la jornada

4. **Scenario**: Consulta sin restricciones de asignación
   - **Given** que el trabajador autenticado intenta consultar un galpón activo con tratamiento vigente sin requerir una validación previa de asignación trabajador-galpón
   - **When** consulta la información del galpón
   - **Then** el sistema permite la operación siempre que el usuario tenga el rol `TRABAJADOR` y el galpón esté activo

---

### Edge Cases

- **¿Qué sucede si un galpón no cuenta con un lote activo registrado?**
  - El sistema muestra los datos disponibles del galpón, informa que no posee lote activo y presenta el estado `"Sin medicación activa para hoy"`.
- **¿Qué sucede si la población actual de aves vivas en el lote es 0?**
  - El sistema muestra la información del lote con 0 aves vivas e indica que no hay medicación aplicable.
- **¿Qué sucede si un galpón tiene dos o más tratamientos activos simultáneos en la fecha?**
  - El sistema lista de forma independiente cada tratamiento activo con su respectiva enfermedad, medicamento, dosis del día y avance.
- **¿La consulta permite registrar el consumo real o descontar inventario de medicamentos?**
  - No, la funcionalidad es estrictamente de solo lectura; el registro del consumo real aplicado y el descuento de inventario corresponden exclusivamente al Spec 013.
- **¿Qué sucede si un usuario no autenticado o sin rol autorizado intenta acceder?**
  - El sistema bloquea la consulta y exige autenticación con un rol autorizado (`ROLE_TRABAJADOR` o administrador autorizado).

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir la consulta de medicación por galpón por día a usuarios autenticados con rol de trabajador y administradores autorizados.
- **FR-002**: El sistema DEBE exponer un mecanismo de consulta (endpoint GET) que permita obtener el detalle de la medicación diaria por `galponId` y el resumen consolidado para poblar los paneles del frontend.
- **FR-003**: Todo trabajador autorizado por rol DEBE poder consultar cualquier galpón existente sin restricción de asignación trabajador-galpón; la consulta consolidada DEBE incluir únicamente los galpones seleccionados para esa solicitud.
- **FR-004**: Para el galpón consultado, el sistema DEBE obtener y presentar sus atributos: UUID, Nombre, Aforo máximo y Estado.
- **FR-005**: El sistema DEBE identificar el lote actualmente alojado en el galpón mediante la consulta propietaria del módulo 1.
- **FR-006**: Para el lote activo, el sistema DEBE presentar: UUID único, Nombre, Población actual (aves vivas) y Edad en días calculada a partir de su fecha de ingreso.
- **FR-007**: Para la fecha actual del reloj del sistema, el sistema DEBE consultar los diagnósticos y medicaciones vigentes asociados al lote activo del galpón.
- **FR-008**: Si el galpón cuenta con tratamiento activo en la fecha, el sistema DEBE mostrar: enfermedad diagnosticada, nombre del medicamento, dosis a aplicar en el día, descripción/vía de administración y avance del tratamiento (`día actual` frente a `días totales`).
- **FR-009**: Si el galpón NO cuenta con tratamientos activos en la fecha, el sistema DEBE mostrar explícitamente el estado `Sin medicación activa para hoy` y NO DEBE mostrar dosis a aplicar.
- **FR-010**: El sistema DEBE calcular el avance del tratamiento determinando el número de día en curso respecto a la duración total en días conservada en el diagnóstico.
- **FR-011**: La funcionalidad de consulta DEBE operar estrictamente en modo de solo lectura y NO DEBE alterar inventarios de medicamentos, diagnósticos ni datos del galpón o lote, ni publicar eventos de integración.
- **FR-012**: El sistema DEBE presentar cada tratamiento en forma desglosada y separada si el galpón registra múltiples tratamientos activos concurrentes en el día.
- **FR-013**: El sistema DEBE calcular y presentar en la pantalla de inicio del trabajador la tarjeta métrica `"Tratamiento Activo Hoy"`, indicando la cantidad de galpones seleccionados que tienen al menos un tratamiento farmacológico activo y un resumen informativo.
- **FR-014**: El sistema DEBE presentar en la pantalla de inicio del trabajador el panel consolidado `"Medicación Diaria (Spec 015)"`, mostrando un badge con el total de tratamientos activos y listando tarjetas individuales con los 6 campos obligatorios (galpón/lote, avance, enfermedad, medicamento, dosis hoy, vía).

### Non-Functional Requirements

| ID          | Categoría   | Requerimiento                                                                      | Métrica                                 |
| :---------- | :---------- | :--------------------------------------------------------------------------------- | :-------------------------------------- |
| **NFR-001** | Rendimiento | Latencia de la consulta individual y consolidada                                   | < 2 segundos (p95) bajo carga normal    |
| **NFR-002** | Seguridad   | Acceso restringido exclusivamente a roles autorizados (TRABAJADOR / ADMINISTRADOR) | HTTP 403 Forbidden a otros roles        |
| **NFR-003** | Integridad  | Garantía de solo lectura estricta (sin transacciones de escritura)                 | 0% de modificaciones a la base de datos |

### Key Entities

- **Galpón**: Representa la unidad física de producción avícola consultada por el trabajador (Módulo 1).
  - **Atributos utilizados**: `UUID único`, `Nombre`, `Aforo máximo`, `Estado`.
- **Lote**: Representa el grupo de aves actualmente alojado que referencia al galpón mediante llave foránea (Módulo 1).
  - **Atributos utilizados**: `UUID único`, `Nombre`, `Población actual` (aves vivas), `Fecha de ingreso` (edad en días), `Llave foránea del galpón`.
- **Diagnóstico del galpón**: Representa el diagnóstico emitido por el veterinario (Spec 011) que asigna una medicación al lote activo.
  - **Atributos utilizados**: `UUID único`, `Enfermedad`, `Fecha de diagnóstico`, `Días de medicación`, `Estado`.
- **Medicación**: Representa la prescripción médica asociada al diagnóstico (Spec 008).
  - **Atributos utilizados**: `Medicamento` (nombre), `Dosis`, `Días totales`, `Descripción / Vía de administración`.
- **Resumen consolidado de medicación diaria del trabajador**: Agrupación de lectura para la pantalla de inicio de los tratamientos médicos activos hoy en los galpones seleccionados por el trabajador para esa consulta.
  - **Atributos calculados**: Total de galpones con tratamiento activo, Total de tratamientos vigentes, Resumen de medicamento y galpón principal, y Lista de tarjetas de tratamiento con galpón, lote, avance (`Día X de N`), enfermedad, medicamento, dosis de hoy y vía de administración.

## Success Criteria

### Measurable Outcomes

- **SC-001**: El 100 % de las consultas de un trabajador muestra los datos vigentes de cualquier galpón consultado sin exigir asignación previa trabajador-galpón.
- **SC-002**: El 100 % de los galpones con tratamiento vigente muestra con exactitud la enfermedad, medicamento, dosis a aplicar en el día, vía de administración y avance del tratamiento (`Día X de N`).
- **SC-003**: El 100 % de los galpones sin tratamientos activos en la fecha muestra de forma inequívoca el estado `Sin medicación activa para hoy`.
- **SC-004**: En el 100 % de los casos, la consulta identifica correctamente el lote actualmente alojado y muestra su población viva actual y edad en días.
- **SC-005**: El 95 % de las operaciones de consulta se ejecuta en un máximo de 2 segundos sin modificar existencias ni registros de la base de datos (garantía de solo lectura).
- **SC-006**: El 100 % de las consultas de galpones inexistentes o realizadas por usuarios sin rol autorizado es rechazado.
- **SC-007**: Si existen múltiples tratamientos concurrentes, el 100 % de ellos se muestra desglosado de forma independiente.
- **SC-008**: El 100 % de las pantallas de inicio de trabajadores refleja en la tarjeta métrica `"Tratamiento Activo Hoy"` el conteo exacto de galpones con tratamientos vigentes seleccionados y el resumen del fármaco y galpón.
- **SC-009**: El 100 % de las tarjetas de tratamiento presentadas en el panel de medicación diaria incluye con precisión los 6 datos requeridos restringidos a los galpones seleccionados.

---

## Out of Scope

- La prescripción, definición y registro inicial de medicaciones (cubierto en Spec 008 _Registrar medicación_).
- La emisión de diagnósticos y asignación de tratamientos por el veterinario (cubierto en Spec 011 _Diagnosticar un galpón_).
- El registro del consumo real y descuento de existencias en el inventario de medicamentos (cubierto en Spec 013 _Registrar consumo de medicamento_).
- El registro de recepciones de medicamentos en bodega central (cubierto en Spec 002 _Registro de recepción de medicamento_).
