# Feature Specification: Diagnosticar un galpón

**Created**: 2026-09-04  

## User Scenarios & Testing

### User Story 1 - Registrar el diagnóstico de un galpón (Priority: P1)

Como veterinario, quiero diagnosticar un galpón que se encuentra en estado `aislamiento` seleccionando una única medicación, para registrar el tratamiento correspondiente, calcular automáticamente la fecha de reintegro y permitir que el galpón vuelva al estado `en producción` cuando se cumpla esa fecha.

**Why this priority**: El diagnóstico permite establecer el tratamiento después de validar el aislamiento, calcular la fecha de reintegro utilizando el número total de días definido en la medicación y devolver automáticamente el galpón a producción cuando finalice el tratamiento. La decisión de diagnosticar corresponde exclusivamente al veterinario.

**Independent Test**: Se puede probar utilizando el galpón recibido de la validación de aislamiento y el lote actualmente asociado con él. Si el galpón está en estado `aislamiento`, el veterinario registra el diagnóstico el 4 de septiembre y selecciona una medicación cuya duración es de 5 días. El sistema debe crear el diagnóstico, guardar la clave foránea de esa única medicación, calcular el 9 de septiembre como fecha de reintegro y mantener el galpón en `aislamiento` hasta esa fecha. Al llegar el 9 de septiembre, debe cambiar automáticamente su estado a `en producción`.

**Acceptance Scenarios**:

1. **Scenario**: Registro correcto del diagnóstico y cálculo de la fecha de reintegro
   - **Given** que el galpón recibido de la validación de aislamiento continúa en estado `aislamiento`, tiene un lote alojado actualmente y existe una medicación disponible
   - **When** el veterinario registra el diagnóstico y selecciona una única medicación
   - **Then** el sistema crea el diagnóstico, lo asocia con el galpón y el lote, guarda la clave foránea de la medicación y calcula automáticamente la fecha de reintegro

2. **Scenario**: Reintegro automático al cumplirse la fecha
   - **Given** que el galpón se encuentra en estado `aislamiento` y la fecha de reintegro de su diagnóstico es el 9 de septiembre
   - **When** la fecha actual alcanza el 9 de septiembre
   - **Then** el sistema cambia automáticamente el estado del galpón a `en producción`

3. **Scenario**: Permanencia en aislamiento antes de la fecha de reintegro
   - **Given** que el galpón se encuentra en estado `aislamiento` y todavía no se ha cumplido la fecha de reintegro
   - **When** el sistema verifica la fecha del diagnóstico
   - **Then** conserva el galpón en estado `aislamiento`

4. **Scenario**: Galpón sin aislamiento validado
   - **Given** que el galpón recibido de la validación de aislamiento tiene un estado vigente diferente de `aislamiento`
   - **When** el veterinario intenta registrar el diagnóstico
   - **Then** el sistema rechaza la operación, informa que el galpón debe estar en estado `aislamiento` y no crea el diagnóstico ni calcula una fecha de reintegro

5. **Scenario**: Intento de registro sin seleccionar una medicación
   - **Given** que el galpón se encuentra en estado `aislamiento`
   - **When** el veterinario intenta registrar el diagnóstico sin seleccionar una medicación
   - **Then** el sistema rechaza la operación, indica que debe seleccionar una medicación y no crea el diagnóstico ni calcula una fecha de reintegro

6. **Scenario**: Medicación no disponible
   - **Given** que el galpón se encuentra en estado `aislamiento`
   - **When** el veterinario selecciona una medicación que no existe o ya no está disponible
   - **Then** el sistema rechaza la operación, informa que no pudo verificar la medicación y no crea el diagnóstico ni calcula una fecha de reintegro

7. **Scenario**: Registro por un usuario no autorizado
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

- **Edge case #3 - Galpón con un estado diferente al llegar la fecha de reintegro**

  - ¿Cómo maneja el sistema un galpón que deja de estar en `aislamiento` antes de alcanzar su fecha de reintegro?  
    El sistema no debe sobrescribir el estado vigente del galpón. La transición automática a `en producción` solo debe realizarse si el galpón continúa en `aislamiento`.

- **Edge case #4 - Sistema no disponible en la fecha de reintegro**

  - ¿Cómo maneja el sistema una fecha de reintegro que se cumple mientras el sistema no está disponible?  
    Al restablecerse, el sistema debe identificar los diagnósticos cuya fecha de reintegro ya se cumplió y cambiar a `en producción` los galpones que todavía se encuentren en `aislamiento`.


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
- **FR-009**: Mientras la fecha actual sea anterior a la fecha de reintegro, el sistema DEBE conservar el galpón en estado `aislamiento`.
- **FR-010**: Cuando la fecha actual sea igual o posterior a la fecha de reintegro y el galpón continúe en estado `aislamiento`, el sistema DEBE cambiar automáticamente su estado a `en producción`.
- **FR-011**: La transición a `en producción` DEBE ejecutarse una sola vez y NO DEBE requerir una nueva acción del veterinario.
- **FR-012**: Si el galpón tiene un estado diferente de `aislamiento` al cumplirse la fecha de reintegro, el sistema NO DEBE sobrescribir su estado vigente.

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
  - **Transición de estado**: permanece en `aislamiento` durante el tratamiento y cambia automáticamente a `en producción` cuando se cumple la fecha de reintegro.
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
- **SC-007**: El 100 % de los galpones que continúan en `aislamiento` cambia automáticamente a `en producción` al cumplirse su fecha de reintegro.
- **SC-008**: El 100 % de los galpones conserva el estado `aislamiento` mientras su fecha de reintegro no se haya cumplido.
