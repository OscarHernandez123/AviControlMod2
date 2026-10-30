# Feature Specification: Consultar edad por galpón

**Created**: 2026-09-02  

## User Scenarios & Testing

### User Story 1 - Consultar la edad del lote alojado en un galpón (Priority: P1)

Como administrador, quiero consultar la edad exacta del lote de aves alojado actualmente en un galpón utilizando la información proporcionada por el módulo 1, para conocer cuántos días y semanas lleva en crianza.

**Why this priority**: La edad del lote permite al administrador conocer de forma inmediata su avance desde el ingreso al galpón y consultar este dato sin realizar cálculos manuales.

**Independent Test**: Se puede probar seleccionando un galpón proporcionado por el módulo 1 que tenga un lote alojado actualmente. El sistema debe obtener la fecha de ingreso de la entidad Lote suministrada por ese módulo, calcular la edad contando la fecha de ingreso como el primer día y mostrarla en semanas, días restantes y días totales.

**Acceptance Scenarios**:

1. **Scenario**: Consulta correcta de la edad de un lote alojado actualmente
   - **Given** que el módulo 1 proporciona un galpón con un único lote alojado actualmente cuya fecha de ingreso fue hace 16 días
   - **When** el administrador consulta la edad del lote por galpón
   - **Then** el sistema obtiene la fecha de ingreso de la entidad Lote, calcula la edad y muestra `2 semanas y 3 días (17 días)`

2. **Scenario**: Consulta de un galpón sin lote alojado actualmente
   - **Given** que el galpón seleccionado no tiene un lote alojado actualmente
   - **When** el administrador consulta la edad por galpón
   - **Then** el sistema muestra el mensaje `El galpón no tiene lote alojado actualmente`

3. **Scenario**: Consulta por un usuario no autorizado
   - **Given** que un usuario sin rol de administrador intenta consultar la edad de un lote
   - **When** selecciona un galpón o solicita la consulta
   - **Then** el sistema rechaza la operación y no muestra la información del lote

### Edge Cases

- **Edge case #1 - Fecha de ingreso posterior a la fecha actual**

  - ¿Cómo maneja el sistema un lote alojado actualmente cuya fecha de ingreso al galpón es posterior a la fecha actual?  
    El sistema debe impedir el cálculo de una edad negativa, informar que la fecha de ingreso es inconsistente y no mostrar una edad para el lote.

- **Edge case #2 - Cálculo entre meses, años o durante un año bisiesto**

  - ¿Cómo maneja el sistema una edad cuyo periodo atraviesa meses con diferente cantidad de días, un cambio de año o el 29 de febrero?  
    El sistema debe calcular los días calendario transcurridos desde la fecha de ingreso, incluyendo el día de ingreso, sin asumir que todos los meses o años tienen la misma duración.

- **Edge case #3 - Información incompleta o no disponible desde el módulo 1**

  - ¿Cómo maneja el sistema una consulta cuando el módulo 1 no está disponible o devuelve datos incompletos del galpón o de un lote registrado como alojado actualmente?
    El sistema debe identificar la información que no está disponible, no calcular la edad con datos inventados o desactualizados e informar que la consulta no pudo completarse.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir la consulta de la edad del lote alojado en un galpón exclusivamente a usuarios con rol de administrador.
- **FR-002**: El sistema DEBE obtener del módulo 1 el galpón seleccionado y la entidad Lote alojada actualmente en él.
- **FR-003**: El sistema DEBE obtener la fecha de ingreso desde la entidad Lote proporcionada por el módulo 1 y calcular la edad utilizando la fecha actual del sistema, incluyendo el día de ingreso como el primer día.
- **FR-004**: El sistema DEBE mostrar la edad en semanas completas, días restantes y días totales con un formato equivalente a `2 semanas y 3 días (17 días)`, utilizando singular o plural según corresponda.
- **FR-005**: Cuando la consulta se realice el mismo día del ingreso, el sistema DEBE mostrar una edad de `1 día`.
- **FR-006**: Cuando el galpón no tenga un lote alojado actualmente, el sistema DEBE mostrar el mensaje `El galpón no tiene lote alojado actualmente`.
- **FR-007**: La consulta DEBE utilizar la información vigente proporcionada por el módulo 1 y NO DEBE modificar las entidades Galpón o Lote.

### Key Entities

- **Galpón**: Representa el espacio en el que se aloja un lote de aves y es proporcionado por el módulo 1.
  - **Atributos relevantes**: nombre, aforo máximo y estado.
  - **Relaciones**: puede alojar cero o un lote de aves actualmente y diferentes lotes a lo largo del tiempo.
- **Lote de aves**: Representa el grupo de aves cuya edad se calcula y es proporcionado por el módulo 1.
  - **Atributos utilizados**: fecha de ingreso.
  - **Datos derivados**: edad actual calculada por el sistema.
  - **Relaciones**: se encuentra alojado en un único galpón durante su ciclo de crianza.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los administradores puede consultar la edad de un lote en menos de 30 segundos.
- **SC-002**: El 95 % de las consultas muestra el resultado en un máximo de 1 segundo después de seleccionar el galpón.
- **SC-003**: Al menos el 95 % de los administradores identifica correctamente la edad del lote en el primer intento durante pruebas de usabilidad.
- **SC-004**: Al menos el 85 % de los administradores califica la claridad de la edad mostrada con 4 o más puntos sobre 5.
