# Feature Specification: 011 - Diagnosticar un galpón

**Created**: 2026-09-04  
**Last Updated**: 2026-10-28

## User Scenarios & Testing

### User Story 1 - Registrar el diagnóstico de un galpón y gestionar su reintegro (Priority: P1)

Como veterinario, quiero registrar el diagnóstico clínico de un galpón en estado `aislamiento` identificando la enfermedad del catálogo y, si esta no requiere sacrificio sanitario, seleccionar una medicación compatible para calcular automáticamente la fecha de reintegro a producción, o si requiere sacrificio sanitario, habilitar la emisión de la orden de sacrificio sanitario total sin asignar medicación.

**Why this priority**: El diagnóstico formaliza la evaluación clínica del lote aislado. Permite vincular la enfermedad directamente conservando una instantánea histórica y, según su severidad, prescribir el tratamiento con retorno automático a `productivo` al vencer el plazo o habilitar el sacrificio sanitario total si la enfermedad es mortal.

**Independent Test**: Se puede probar diagnosticando un galpón en estado `aislamiento` con una enfermedad tratable y seleccionando una medicación compatible de 5 días (verificando el cálculo de la fecha de reintegro y el retorno automático a `productivo` al cumplirse el plazo); y alternativamente diagnosticando una enfermedad mortal con indicador de sacrificio sanitario verdadero, verificando que no acepte medicación ni fecha de reintegro y habilite la orden de sacrificio sanitario (Spec 012).

**Acceptance Scenarios**:

1. **Scenario**: Registro de diagnóstico con enfermedad tratable y medicación compatible
   - **Given** que el galpón continúa en estado `aislamiento`, tiene un lote activo alojado y se selecciona una enfermedad cuyo indicador `requiere sacrificio sanitario` es falso
   - **When** el veterinario confirma el diagnóstico seleccionando una medicación disponible y compatible con esa enfermedad
   - **Then** el sistema crea el diagnóstico asociado al galpón, al lote, a la enfermedad y a la medicación, guarda la instantánea histórica de la enfermedad y días de tratamiento, y calcula automáticamente la fecha de reintegro sumando los días totales de la medicación a la fecha del diagnóstico

2. **Scenario**: Registro de diagnóstico con enfermedad mortal que requiere sacrificio sanitario
   - **Given** que el galpón se encuentra en estado `aislamiento`, tiene un lote activo alojado y se selecciona una enfermedad cuyo indicador `requiere sacrificio sanitario` es verdadero
   - **When** el veterinario confirma el diagnóstico sin seleccionar medicación
   - **Then** el sistema crea el diagnóstico vinculado directamente a la enfermedad, guarda su instantánea histórica con medicación y fecha de reintegro nulas, y habilita la emisión de la orden de sacrificio sanitario total (Spec 012)

3. **Scenario**: Reintegro automático al cumplirse la fecha en diagnóstico con medicación
   - **Given** que el galpón se encuentra en estado `aislamiento` bajo un diagnóstico tratable y su fecha de reintegro es el 9 de septiembre
   - **When** la fecha actual del sistema alcanza o supera el 9 de septiembre y el galpón continúa en `aislamiento`
   - **Then** el sistema solicita automáticamente al módulo 1 cambiar el estado del galpón de `aislamiento` a `productivo` por única vez

4. **Scenario**: Permanencia en aislamiento antes de cumplirse la fecha de reintegro
   - **Given** que el galpón se encuentra en estado `aislamiento` y todavía no se ha cumplido la fecha de reintegro de su diagnóstico
   - **When** el proceso programado verifica la fecha del diagnóstico
   - **Then** el sistema conserva el galpón en estado `aislamiento`

5. **Scenario**: Galpón que no se encuentra en aislamiento
   - **Given** que el galpón tiene un estado vigente diferente de `aislamiento`
   - **When** el veterinario intenta registrar el diagnóstico
   - **Then** el sistema rechaza la operación, informa que el galpón debe estar en estado `aislamiento` y no crea el diagnóstico

6. **Scenario**: Intento de registro sin seleccionar enfermedad
   - **Given** que el galpón se encuentra en estado `aislamiento`
   - **When** el veterinario intenta registrar el diagnóstico sin seleccionar una enfermedad
   - **Then** el sistema rechaza la operación, indica que debe seleccionar una enfermedad registrada y no crea el diagnóstico

7. **Scenario**: Intento de registro con enfermedad tratable sin medicación o con medicación incompatible
   - **Given** que se selecciona una enfermedad cuyo indicador `requiere sacrificio sanitario` es falso
   - **When** el veterinario intenta registrar el diagnóstico sin seleccionar una medicación o seleccionando una medicación definida para otra enfermedad
   - **Then** el sistema rechaza la operación e indica que debe asociar una medicación compatible para enfermedades tratables

8. **Scenario**: Registro por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta diagnosticar un galpón
   - **When** solicita confirmar el registro
   - **Then** el sistema rechaza la operación y no crea el diagnóstico

---

### Edge Cases

- **Edge case #1 - Cambio del estado del galpón antes de confirmar el diagnóstico**
  - ¿Cómo maneja el sistema un galpón que deja de estar en estado `aislamiento` después de ser seleccionado y antes de confirmar el diagnóstico?  
    El sistema debe verificar nuevamente en el módulo 1 el estado vigente del galpón y la existencia del lote activo justo antes de confirmar, rechazar el registro si cambió e informar que ya no cumple la condición requerida.

- **Edge case #2 - Cambio de la duración de la medicación o de la enfermedad en el catálogo**
  - ¿Cómo maneja el sistema una edición en el catálogo de enfermedades o medicaciones antes o después de confirmar el diagnóstico?  
    Al confirmar el diagnóstico, el sistema debe consultar los valores vigentes en ese instante, calcular la fecha de reintegro con los días vigentes de la medicación y guardar una instantánea (_snapshot_) en el diagnóstico. Ediciones posteriores en los catálogos no alteran diagnósticos ya confirmados.

- **Edge case #3 - Galpón con un estado diferente al llegar la fecha de reintegro**
  - ¿Cómo maneja el sistema un galpón que deja de estar en `aislamiento` (por ejemplo, si pasó a `vaciado sanitario`) antes de alcanzar su fecha de reintegro?  
    El sistema no debe sobrescribir el estado vigente del galpón. La transición automática a `productivo` solo debe ejecutarse si el galpón continúa en estado `aislamiento`.

- **Edge case #4 - Sistema temporalmente fuera de servicio en la fecha de reintegro**
  - ¿Cómo maneja el sistema una fecha de reintegro que se cumple mientras el servicio no está disponible?  
    Al restablecerse, el proceso programado debe identificar de forma idempotente los diagnósticos cuya fecha de reintegro ya se cumplió y solicitar el cambio a `productivo` únicamente para los galpones que todavía se encuentren en `aislamiento`.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro y consulta de diagnósticos de galpón exclusivamente a usuarios autenticados con rol de veterinario.
- **FR-002**: El sistema DEBE permitir el diagnóstico únicamente cuando el módulo 1 confirme que la entidad Galpón se encuentra en estado vigente `aislamiento` y cuenta con un lote activo alojado.
- **FR-003**: Cada diagnóstico DEBE quedar asociado al identificador único del galpón (`galponId`) y al identificador del lote alojado actualmente en él (`loteId`).
- **FR-004**: El diagnóstico DEBE asociarse directamente a una entidad Enfermedad existente en el catálogo (`enfermedadId`) y almacenar una instantánea histórica de su nombre (`nombreEnfermedadHistorico`) y de su indicador de sacrificio (`requiereSacrificioHistorico`).
- **FR-005**: Cuando la enfermedad diagnosticada NO requiera sacrificio sanitario (`requiere sacrificio sanitario == false`), el sistema DEBE exigir la selección de una Medicación compatible con dicha enfermedad, conservar la referencia y los días de tratamiento aplicados, y calcular automáticamente la fecha de reintegro (`fechaDiagnostico + diasTotalesMedicacion`).
- **FR-006**: Cuando la enfermedad diagnosticada requiera sacrificio sanitario (`requiere sacrificio sanitario == true`), el sistema NO DEBE exigir ni aceptar una Medicación (`medicacionId = null`), NO DEBE calcular fecha de reintegro (`fechaReintegro = null`) y DEBE habilitar la emisión de una orden de sacrificio sanitario total (Spec 012).
- **FR-007**: El sistema DEBE registrar como fecha del diagnóstico la fecha calendario de negocio en la que el veterinario confirma el registro.
- **FR-008**: Para diagnósticos con medicación, mientras la fecha actual sea anterior a la fecha de reintegro, el sistema DEBE conservar el galpón en estado `aislamiento`.
- **FR-009**: Cuando la fecha actual sea igual o posterior a la fecha de reintegro de un diagnóstico tratable y el galpón continúe en estado `aislamiento`, el sistema DEBE solicitar automáticamente al módulo 1 el cambio de su estado a `productivo`.
- **FR-010**: La transición automática a `productivo` DEBE ser idempotente, ejecutarse una sola vez por diagnóstico vencido y NO DEBE requerir una acción manual adicional del veterinario.
- **FR-011**: Si el galpón tiene un estado diferente de `aislamiento` al cumplirse la fecha de reintegro, el sistema NO DEBE sobrescribir su estado vigente.
- **FR-012**: El sistema DEBE exponer un mecanismo de consulta (endpoint `GET`) que permita listar los diagnósticos activos de un galpón específico para que otros módulos (como el registro de consumo de medicamentos o la orden de sacrificio) puedan seleccionarlo.
- **FR-013**: Al confirmar el registro de un diagnóstico, el sistema DEBE publicar un evento de integración (`DiagnosticoRegistradoIntegrationEvent`) para notificar a los módulos interesados la nueva condición sanitaria del galpón.

### Non-Functional Requirements

| ID          | Categoría      | Requerimiento                                                                                             | Métrica                                       |
| :---------- | :------------- | :-------------------------------------------------------------------------------------------------------- | :-------------------------------------------- |
| **NFR-001** | Rendimiento    | Latencia de validación, cálculo de reintegro y persistencia                                               | < 1 segundo (p95) bajo carga normal           |
| **NFR-002** | Seguridad      | Acceso restringido exclusivamente al rol VETERINARIO                                                      | HTTP 403 Forbidden a otros roles              |
| **NFR-003** | Integridad     | Escritura atómica del diagnóstico y sus referencias históricas                                            | 100% de consistencia local                    |
| **NFR-004** | Disponibilidad | Si el Módulo 1 no responde, la transacción se aborta completamente                                        | 0% de inconsistencias por fallo cruzado       |
| **NFR-005** | Automatización | El proceso de reintegro debe procesar diagnósticos vencidos de forma acumulativa tras caídas del servicio | 100% de transiciones pendientes regularizadas |

### Key Entities

- **Diagnóstico del galpón**: Representa el dictamen clínico registrado por el veterinario sobre un galpón aislado.
  - **Atributos**: identificador UUID, `galponId`, `loteId`, `enfermedadId`, `nombreEnfermedadHistorico`, `requiereSacrificioHistorico`, `medicacionId` (opcional), `diasMedicacion` (opcional), `fechaDiagnostico`, `fechaReintegro` (opcional) y `estado`.
  - **Relaciones**: referencia directamente a la Enfermedad diagnosticada y opcionalmente a la Medicación prescrita; vincula al Galpón y al Lote activo del módulo 1.
- **Enfermedad**: Representa la patología clínica diagnosticada en el galpón (Spec 009).
  - **Atributos utilizados**: identificador UUID, nombre e indicador booleano `requiere sacrificio sanitario`.
- **Medicación**: Representa el tratamiento seleccionado cuando la enfermedad es tratable (Spec 008).
  - **Atributos utilizados**: identificador UUID, enfermedad asociada, medicamento, dosis y número total de días.
- **Galpón**: Representa la unidad física diagnosticada, propiedad del módulo 1.
  - **Atributos utilizados**: estado (`aislamiento`, `productivo`, `vaciado sanitario`).
  - **Transición de estado**: permanece en `aislamiento` durante el tratamiento y pasa automáticamente a `productivo` al cumplirse la fecha de reintegro, o pasa a `vaciado sanitario` si se confirma la ejecución de un sacrificio sanitario (Spec 012).
- **Lote**: Representa el grupo de aves alojado en el galpón al momento del diagnóstico, propiedad del módulo 1.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los veterinarios puede registrar un diagnóstico válido en menos de 2 minutos.
- **SC-002**: El 100 % de los diagnósticos se registra únicamente para galpones cuyo estado vigente es `aislamiento` y poseen lote activo.
- **SC-003**: El 100 % de los diagnósticos creados almacena la referencia directa y la instantánea histórica de la Enfermedad diagnosticada.
- **SC-004**: El 100 % de los diagnósticos con enfermedades tratables valida la compatibilidad de su Medicación y calcula la fecha de reintegro con exactitud.
- **SC-005**: El 100 % de los diagnósticos con enfermedades de sacrificio sanitario total prescinde de medicación y fecha de reintegro, y habilita la orden de sacrificio sanitario.
- **SC-006**: El 95 % de los diagnósticos válidos queda disponible en un máximo de 1 segundo después de la confirmación.
- **SC-007**: El 100 % de los intentos realizados por roles distintos al veterinario es rechazado sin crear un diagnóstico.
- **SC-008**: El 100 % de los galpones con tratamiento vencido que continúan en `aislamiento` cambia automáticamente a `productivo` una sola vez.
