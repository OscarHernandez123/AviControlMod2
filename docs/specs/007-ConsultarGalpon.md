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

### Edge Cases

- **Edge case #1 - Fecha de ingreso posterior a la fecha actual**

  - ¿Cómo maneja el sistema un lote actualmente alojado cuya fecha de ingreso es posterior a la fecha actual?  
    El sistema debe mostrar los demás datos disponibles, impedir el cálculo de una edad negativa e indicar que no puede calcular la edad porque la fecha de ingreso es inconsistente.

- **Edge case #2 - Más de un lote alojado actualmente en el mismo galpón**

  - ¿Cómo maneja el sistema un galpón que tiene más de un lote registrado como alojado actualmente?  
    El sistema debe mostrar los datos propios del galpón, informar la inconsistencia y no seleccionar arbitrariamente un lote para mostrar su población o calcular su edad.

- **Edge case #3 - Información incompleta o no disponible desde el módulo 1**

  - ¿Cómo maneja el sistema una consulta cuando el módulo 1 no proporciona alguno de los datos requeridos?  
    El sistema debe identificar los datos que no están disponibles, no sustituirlos por información inventada ni desactualizada y comunicar que la consulta no pudo completarse.

- **Edge case #4 - Cálculo entre meses, años o durante un año bisiesto**

  - ¿Cómo maneja el sistema una edad cuyo periodo atraviesa meses con diferente cantidad de días, un cambio de año o el 29 de febrero?  
    El sistema debe calcular los días calendario transcurridos desde la fecha de ingreso, incluyendo el día de ingreso como el primer día, sin asumir que todos los meses o años tienen la misma duración.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir la consulta de información de un galpón exclusivamente a personas autenticadas con rol de administrador o de usuario.
- **FR-002**: El sistema DEBE permitir al administrador o usuario seleccionar el galpón que desea consultar.
- **FR-003**: El sistema DEBE mostrar el nombre, aforo máximo, estado, población actual y edad del lote correspondiente al galpón seleccionado.
- **FR-004**: El sistema DEBE obtener el nombre, aforo máximo y estado desde la entidad Galpón proporcionada por el módulo 1.
- **FR-005**: El sistema DEBE identificar el único lote alojado actualmente en el galpón y obtener su población actual desde la entidad Lote proporcionada por el módulo 1.
- **FR-006**: El sistema DEBE calcular la edad del lote utilizando su fecha de ingreso, proporcionada como atributo de la entidad Lote por el módulo 1, y la fecha actual del sistema.
- **FR-007**: El cálculo de la edad DEBE incluir la fecha de ingreso como el primer día y mostrarse en semanas completas, días restantes y días totales, con un formato equivalente a `2 semanas y 3 días (17 días)` y singular o plural según corresponda.
- **FR-008**: Cuando el galpón no tenga un lote alojado actualmente, el sistema DEBE mostrar los datos disponibles de la entidad Galpón e indicar que la población actual y la edad del lote no están disponibles.
- **FR-009**: La consulta DEBE utilizar la información vigente proporcionada por el módulo 1 y NO DEBE modificar las entidades Galpón o Lote.

### Key Entities

- **Galpón**: Representa el espacio consultado y es proporcionado por el módulo 1.
  - **Atributos utilizados**: nombre, aforo máximo y estado.
  - **Relaciones**: puede alojar un lote de aves actualmente y diferentes lotes a lo largo del tiempo.
- **Lote**: Representa el grupo de aves alojado actualmente en el galpón y es proporcionado por el módulo 1.
  - **Atributos utilizados**: población actual y fecha de ingreso.
  - **Datos derivados**: edad actual calculada por el sistema.
  - **Relaciones**: se encuentra alojado en un galpón durante su ciclo de crianza.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los administradores y usuarios puede consultar la información de un galpón en menos de 20 segundos.
- **SC-002**: El 95 % de las consultas muestra todos los datos disponibles en un máximo de 1 segundo después de seleccionar el galpón.
- **SC-003**: El 100 % de las edades mostradas coincide con los días calendario transcurridos desde la fecha de ingreso, incluyendo el día de ingreso como el primer día.
- **SC-004**: Al menos el 95 % de los administradores y usuarios identifica correctamente el nombre, aforo máximo, estado, población actual y edad del lote en el primer intento durante pruebas de usabilidad.
