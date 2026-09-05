# Feature Specification: Diagnosticar un galpón

**Created**: 2026-09-04  

## User Scenarios & Testing

### User Story 1 - Registrar el diagnóstico de un galpón (Priority: P1)

Como veterinario, quiero registrar el diagnóstico de un galpón en estado `aislamiento` identificando la enfermedad y, si esta no requiere sacrificio sanitario, seleccionar la medicación correspondiente para calcular automáticamente la fecha de reintegro a producción, o si requiere sacrificio sanitario, habilitar la emisión de la orden de sacrificio sanitario.

**Why this priority**: El diagnóstico formaliza la evaluación clínica del lote aislado. Permite vincular la enfermedad directamente y, según su severidad, aplicar un tratamiento con fecha de retorno a producción o habilitar el sacrificio sanitario total si la enfermedad es mortal.

**Independent Test**: Se puede probar diagnosticando un galpón en aislamiento con una enfermedad tratable y seleccionando una medicación de 5 días (verificando el cálculo de reintegro y retorno a producción); y alternativamente diagnosticando una enfermedad mortal con indicador de sacrificio sanitario verdadero, verificando que no exija medicación ni retorno a producción y habilite el sacrificio sanitario.

**Acceptance Scenarios**:

1. **Scenario**: Registro de diagnóstico con enfermedad tratable y medicación
   - **Given** que el galpón continúa en estado `aislamiento`, tiene un lote alojado y se selecciona una enfermedad que no requiere sacrificio sanitario
   - **When** el veterinario confirma el diagnóstico y selecciona una medicación activa
   - **Then** el sistema crea el diagnóstico asociado a la enfermedad y a la medicación, y calcula automáticamente la fecha de reintegro sumando los días de medicación a la fecha del diagnóstico

2. **Scenario**: Registro de diagnóstico con enfermedad mortal que requiere sacrificio sanitario
   - **Given** que el galpón se encuentra en `aislamiento`, tiene un lote alojado y se selecciona una enfermedad cuyo indicador `requiere sacrificio sanitario` es verdadero
   - **When** el veterinario confirma el diagnóstico sin seleccionar medicación
   - **Then** el sistema crea el diagnóstico vinculado directamente a la enfermedad, no calcula fecha de reintegro a producción y habilita la emisión de la orden de sacrificio sanitario total

3. **Scenario**: Reintegro automático al cumplirse la fecha en diagnóstico con medicación
   - **Given** que el galpón se encuentra en estado `aislamiento` y la fecha de reintegro de su diagnóstico con medicación es el 9 de septiembre
   - **When** la fecha actual alcanza el 9 de septiembre
   - **Then** el sistema cambia automáticamente el estado del galpón a `en producción`

4. **Scenario**: Permanencia en aislamiento antes de la fecha de reintegro
   - **Given** que el galpón se encuentra en estado `aislamiento` y todavía no se ha cumplido la fecha de reintegro
   - **When** el sistema verifica la fecha del diagnóstico
   - **Then** conserva el galpón en estado `aislamiento`

5. **Scenario**: Galpón sin aislamiento validado
   - **Given** que el galpón tiene un estado vigente diferente de `aislamiento`
   - **When** el veterinario intenta registrar el diagnóstico
   - **Then** el sistema rechaza la operación, informa que el galpón debe estar en estado `aislamiento` y no crea el diagnóstico

6. **Scenario**: Intento de registro sin seleccionar enfermedad
   - **Given** que el galpón se encuentra en estado `aislamiento`
   - **When** el veterinario intenta registrar el diagnóstico sin seleccionar una enfermedad
   - **Then** el sistema rechaza la operación, indica que debe seleccionar una enfermedad y no crea el diagnóstico

7. **Scenario**: Intento de registro con enfermedad tratable sin medicación
   - **Given** que se selecciona una enfermedad cuyo indicador `requiere sacrificio sanitario` es falso
   - **When** el veterinario intenta registrar el diagnóstico sin seleccionar una medicación
   - **Then** el sistema rechaza la operación e indica que debe asociar una medicación para enfermedades tratables

8. **Scenario**: Registro por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta diagnosticar un galpón
   - **When** solicita confirmar el registro
   - **Then** el sistema rechaza la operación y no crea el diagnóstico


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
- **FR-005**: El diagnóstico DEBE asociarse directamente a una entidad Enfermedad registrada y activa.
- **FR-006**: Cuando la enfermedad diagnosticada NO requiera sacrificio sanitario (`requiere sacrificio sanitario == false`), el sistema DEBE exigir la selección de una Medicación compatible, calcular automáticamente la fecha de reintegro (`fecha del diagnóstico + número total de días de la medicación`) y guardar dicha fecha en el diagnóstico.
- **FR-007**: Cuando la enfermedad diagnosticada requiera sacrificio sanitario (`requiere sacrificio sanitario == true`), el sistema NO DEBE exigir la selección de una Medicación ni calcular fecha de reintegro a producción, y DEBE habilitar la emisión de una orden de sacrificio sanitario total (SPEC-012).
- **FR-008**: El sistema DEBE registrar como fecha del diagnóstico la fecha calendario en la que el veterinario confirma el registro.
- **FR-009**: Para diagnósticos con medicación, mientras la fecha actual sea anterior a la fecha de reintegro, el sistema DEBE conservar el galpón en estado `aislamiento`.
- **FR-010**: Cuando la fecha actual sea igual o posterior a la fecha de reintegro de un diagnóstico con medicación y el galpón continúe en estado `aislamiento`, el sistema DEBE cambiar automáticamente su estado a `en producción`.
- **FR-011**: La transición a `en producción` DEBE ejecutarse una sola vez y NO DEBE requerir una nueva acción del veterinario.
- **FR-012**: Si el galpón tiene un estado diferente de `aislamiento` al cumplirse la fecha de reintegro, el sistema NO DEBE sobrescribir su estado vigente.

### Key Entities

- **Diagnóstico del galpón**: Representa el resultado registrado por el veterinario después de validar el aislamiento del galpón.
  - **Atributos**: fecha del diagnóstico y fecha de reintegro calculada (cuando aplica tratamiento).
  - **Relaciones**: corresponde a un único galpón, al lote alojado actualmente, almacena la clave foránea directa de la Enfermedad diagnosticada y opcionalmente la clave foránea de una Medicación (para enfermedades tratables).
  - **Datos accesibles mediante sus relaciones**: accede directamente a los datos y severidad de la Enfermedad, y al tratamiento prescrito si posee Medicación asociada.
- **Enfermedad**: Representa la patología clínica diagnosticada en el galpón.
  - **Atributos utilizados**: nombre, indicador booleano `requiere sacrificio sanitario`.
  - **Relaciones**: es referenciada directamente por el diagnóstico del galpón y cuenta con medicaciones registradas para su tratamiento.
- **Medicación**: Representa el tratamiento seleccionado para el diagnóstico cuando la enfermedad es tratable.
  - **Atributos utilizados**: número total de días, dosis.
  - **Relaciones**: es referenciada opcionalmente por el diagnóstico y permite acceder al medicamento y posología correspondientes.
- **Galpón**: Representa el espacio diagnosticado y es recibido de la validación de aislamiento.
  - **Atributos utilizados**: estado.
  - **Datos derivados**: fecha de reintegro calculada y almacenada en su diagnóstico (si aplica medicación).
  - **Relaciones**: tiene un lote alojado actualmente y queda asociado con el diagnóstico.
  - **Transición de estado**: permanece en `aislamiento` durante el tratamiento y cambia automáticamente a `en producción` cuando se cumple la fecha de reintegro; o pasa a `vaciado sanitario` si se ejecuta una orden de sacrificio sanitario.
- **Lote**: Representa el grupo de aves alojado en el galpón al momento del diagnóstico.
  - **Relaciones**: está alojado en el galpón y queda asociado con el diagnóstico para identificar las aves evaluadas.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los veterinarios puede registrar un diagnóstico válido en menos de 2 minutos.
- **SC-002**: El 100 % de los diagnósticos se registra únicamente para galpones cuyo estado vigente es `aislamiento`.
- **SC-003**: El 100 % de los diagnósticos creados almacena la clave foránea directa de la Enfermedad diagnosticada.
- **SC-004**: El 100 % de los diagnósticos con enfermedades tratables almacena la referencia a su Medicación y calcula la fecha de reintegro con exactitud.
- **SC-005**: El 100 % de los diagnósticos con enfermedades de sacrificio sanitario total prescinde de medicación y habilita la orden de sacrificio sanitario.
- **SC-006**: El 95 % de los diagnósticos válidos queda disponible en un máximo de 1 segundo después de la confirmación.
- **SC-007**: El 100 % de los intentos realizados por roles distintos al veterinario es rechazado sin crear un diagnóstico.
- **SC-008**: El 100 % de los galpones con tratamiento que continúan en `aislamiento` cambia automáticamente a `en producción` al cumplirse su fecha de reintegro.
