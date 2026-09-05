# Feature Specification: Diagnosticar un galpón

**Created**: 2026-09-04  

## User Scenarios & Testing

### User Story 1 - Registrar el diagnóstico de un galpón (Priority: P1)

Como veterinario, quiero diagnosticar un galpón que se encuentra en estado `aislamiento` seleccionando una única medicación, para registrar el tratamiento correspondiente y calcular automáticamente la fecha de reintegro del galpón.

**Why this priority**: El diagnóstico permite establecer el tratamiento después de validar el aislamiento y conocer desde qué fecha puede considerarse el reintegro, utilizando el número total de días definido en la medicación. Esta decisión corresponde exclusivamente al veterinario.

**Independent Test**: Se puede probar utilizando el galpón recibido de la validación de aislamiento y el lote actualmente asociado con él. Si el galpón está en estado `aislamiento`, el veterinario registra el diagnóstico el 4 de septiembre y selecciona una medicación cuya duración es de 5 días. El sistema debe crear el diagnóstico, guardar la clave foránea de esa única medicación y calcular el 9 de septiembre como fecha de reintegro.

**Acceptance Scenarios**:

1. **Scenario**: Registro correcto del diagnóstico y cálculo de la fecha de reintegro
   - **Given** que el galpón recibido de la validación de aislamiento continúa en estado `aislamiento`, tiene un lote alojado actualmente y existe una medicación disponible
   - **When** el veterinario registra el diagnóstico y selecciona una única medicación
   - **Then** el sistema crea el diagnóstico, lo asocia con el galpón y el lote, guarda la clave foránea de la medicación y calcula automáticamente la fecha de reintegro

2. **Scenario**: Galpón sin aislamiento validado
   - **Given** que el galpón recibido de la validación de aislamiento tiene un estado vigente diferente de `aislamiento`
   - **When** el veterinario intenta registrar el diagnóstico
   - **Then** el sistema rechaza la operación, informa que el galpón debe estar en estado `aislamiento` y no crea el diagnóstico ni calcula una fecha de reintegro

3. **Scenario**: Intento de registro sin seleccionar una medicación
   - **Given** que el galpón se encuentra en estado `aislamiento`
   - **When** el veterinario intenta registrar el diagnóstico sin seleccionar una medicación
   - **Then** el sistema rechaza la operación, indica que debe seleccionar una medicación y no crea el diagnóstico ni calcula una fecha de reintegro

4. **Scenario**: Medicación no disponible
   - **Given** que el galpón se encuentra en estado `aislamiento`
   - **When** el veterinario selecciona una medicación que no existe o ya no está disponible
   - **Then** el sistema rechaza la operación, informa que no pudo verificar la medicación y no crea el diagnóstico ni calcula una fecha de reintegro

5. **Scenario**: Registro por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta diagnosticar un galpón
   - **When** solicita confirmar el registro
   - **Then** el sistema rechaza la operación y no crea el diagnóstico ni calcula una fecha de reintegro


### Edge Cases

- **Edge case #1 - Cambio del estado antes de confirmar el diagnóstico**

  - ¿Cómo maneja el sistema un galpón que deja de estar en estado `aislamiento` después de ser seleccionado y antes de confirmar el diagnóstico?  
    El sistema debe verificar nuevamente el estado vigente del galpón, rechazar el registro e informar que ya no cumple la condición requerida.

- **Edge case #2 - Cambio de la duración de la medicación antes de confirmar**

  - ¿Cómo maneja el sistema una medicación cuyo número total de días cambia después de ser seleccionada y antes de confirmar el diagnóstico?  
    El sistema debe verificar nuevamente la medicación y calcular la fecha de reintegro utilizando el número total de días vigente al momento de confirmar el diagnóstico.

- **Edge case #3 - Cálculo entre meses, años o durante un año bisiesto**

  - ¿Cómo maneja el sistema una fecha de reintegro cuyo tratamiento atraviesa meses con diferente cantidad de días, un cambio de año o el 29 de febrero?  
    El sistema debe sumar a la fecha del diagnóstico el número total de días calendario definido en la medicación, respetando la duración real de cada mes y año.


## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro de diagnósticos de galpón exclusivamente a usuarios con rol de veterinario.
- **FR-002**: El sistema DEBE permitir el diagnóstico únicamente después de validar el aislamiento, lo cual se comprueba cuando la entidad Galpón recibida de esa acción se encuentra en estado `aislamiento`.
- **FR-003**: El sistema DEBE utilizar la entidad Galpón recibida de la validación de aislamiento y el lote alojado actualmente en ella.
- **FR-004**: Cada diagnóstico DEBE corresponder a un único galpón y al lote que este tenga alojado al momento del registro.
- **FR-005**: El diagnóstico DEBE obtener la enfermedad por medio de la medicación seleccionada y NO DEBE almacenar una referencia adicional que duplique esa relación.
- **FR-006**: El sistema DEBE registrar como fecha del diagnóstico la fecha calendario en la que el veterinario confirma el registro.
- **FR-007**: El sistema DEBE calcular automáticamente la fecha de reintegro mediante la fórmula `fecha del diagnóstico + número total de días de la medicación`.
- **FR-008**: El sistema DEBE guardar la fecha de reintegro calculada como parte del diagnóstico.

### Key Entities

- **Diagnóstico del galpón**: Representa el resultado registrado por el veterinario después de validar el aislamiento del galpón.
  - **Atributos**: fecha del diagnóstico y fecha de reintegro calculada.
  - **Relaciones**: corresponde a un único galpón, al lote alojado actualmente y almacena la clave foránea de una única medicación.
  - **Datos accesibles mediante sus relaciones**: obtiene la enfermedad y el número total de días mediante la medicación seleccionada.
- **Medicación**: Representa el tratamiento seleccionado para el diagnóstico.
  - **Atributos utilizados**: número total de días.
  - **Relaciones**: es referenciada por el diagnóstico y permite acceder a la enfermedad y al medicamento correspondientes.
- **Enfermedad**: Representa la enfermedad asociada con la medicación.
  - **Relaciones**: el diagnóstico accede a ella por medio de la medicación seleccionada.
- **Galpón**: Representa el espacio diagnosticado y es recibido de la validación de aislamiento.
  - **Atributos utilizados**: estado.
  - **Datos derivados**: fecha de reintegro calculada y almacenada en su diagnóstico.
  - **Relaciones**: tiene un lote alojado actualmente y queda asociado con el diagnóstico.
- **Lote**: Representa el grupo de aves alojado en el galpón al momento del diagnóstico.
  - **Relaciones**: está alojado en el galpón y queda asociado con el diagnóstico para identificar las aves que reciben el tratamiento.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los veterinarios puede registrar un diagnóstico válido en menos de 2 minutos.
- **SC-002**: El 100 % de los diagnósticos se registra únicamente para galpones cuyo estado vigente es `aislamiento`.
- **SC-003**: El 100 % de los diagnósticos creados almacena la clave foránea de una única medicación.
- **SC-004**: El 100 % de las fechas de reintegro coincide con la suma de la fecha del diagnóstico y el número total de días de la medicación.
- **SC-005**: El 95 % de los diagnósticos válidos y sus fechas de reintegro queda disponible en un máximo de 1 segundo después de la confirmación.
- **SC-006**: El 100 % de los intentos realizados por roles distintos al veterinario es rechazado sin crear un diagnóstico.
