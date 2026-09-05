# Feature Specification: Ordenar sacrificio sanitario

**Created**: 2026-09-05  

## User Scenarios & Testing

### User Story 1 - Emitir una orden de sacrificio sanitario total (Priority: P1)

Como veterinario, quiero emitir una orden de sacrificio sanitario total cuando el diagnóstico corresponda a una enfermedad mortal que requiere esta medida, para ordenar el sacrificio de toda la población del lote alojado en el galpón.

**Why this priority**: La orden permite responder a una enfermedad mortal mediante el sacrificio sanitario de toda la población afectada y garantiza que esta decisión sea tomada exclusivamente por el veterinario.

**Independent Test**: Se puede probar utilizando el diagnóstico de un galpón en estado `aislamiento`, con un lote cuya población actual sea mayor que cero y una enfermedad cuyo indicador `requiere sacrificio sanitario` sea verdadero. El veterinario debe poder emitir una orden que abarque toda la población sin ingresar una cantidad. La orden debe quedar pendiente y no debe modificar el lote ni el galpón hasta que se confirme su ejecución.

**Acceptance Scenarios**:

1. **Scenario**: Emisión correcta de una orden de sacrificio sanitario total
   - **Given** que el diagnóstico corresponde a una enfermedad cuyo indicador `requiere sacrificio sanitario` es verdadero, el galpón está en estado `aislamiento` y el lote tiene población actual mayor que cero
   - **When** el veterinario emite la orden de sacrificio sanitario
   - **Then** el sistema crea una orden asociada con el diagnóstico, el galpón y el lote, establece que comprende toda la población y la deja pendiente de ejecución

2. **Scenario**: Enfermedad que no requiere sacrificio sanitario
   - **Given** que el diagnóstico corresponde a una enfermedad cuyo indicador `requiere sacrificio sanitario` es falso
   - **When** el veterinario intenta emitir una orden de sacrificio sanitario
   - **Then** el sistema rechaza la operación, informa que la enfermedad no requiere esta medida y no crea la orden

3. **Scenario**: Galpón que no está en aislamiento
   - **Given** que el diagnóstico corresponde a una enfermedad que requiere sacrificio sanitario, pero el galpón tiene un estado diferente de `aislamiento`
   - **When** el veterinario intenta emitir la orden
   - **Then** el sistema rechaza la operación, informa que el galpón debe estar en estado `aislamiento` y no crea la orden

4. **Scenario**: Emisión por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta emitir una orden de sacrificio sanitario
   - **When** solicita confirmar la orden
   - **Then** el sistema rechaza la operación, no crea la orden y no modifica el lote ni el galpón

---

### User Story 2 - Confirmar la ejecución del sacrificio sanitario total (Priority: P2)

Como veterinario, quiero confirmar que el sacrificio sanitario total fue ejecutado, para establecer en cero la población actual del lote y cambiar el estado del galpón de `aislamiento` a `vaciado sanitario`.

**Why this priority**: La confirmación garantiza que la población y el estado del galpón solo cambien después de ejecutar realmente el sacrificio, evitando que una orden pendiente produzca modificaciones anticipadas.

**Independent Test**: Se puede probar confirmando una orden pendiente asociada con un lote que tenga población actual mayor que cero y un galpón en estado `aislamiento`. El sistema debe establecer directamente la población actual del lote en cero, cambiar el galpón a `vaciado sanitario` y marcar la orden como ejecutada.

**Acceptance Scenarios**:

1. **Scenario**: Confirmación correcta del sacrificio sanitario total
   - **Given** que existe una orden pendiente, el lote tiene población actual mayor que cero y el galpón continúa en estado `aislamiento`
   - **When** el veterinario confirma que el sacrificio sanitario total fue ejecutado
   - **Then** el sistema establece la población actual del lote en cero, cambia el estado del galpón a `vaciado sanitario` y marca la orden como ejecutada

2. **Scenario**: Orden pendiente sin ejecución confirmada
   - **Given** que existe una orden de sacrificio sanitario pendiente
   - **When** su ejecución todavía no ha sido confirmada
   - **Then** el sistema conserva la población actual del lote y el estado `aislamiento` del galpón sin cambios

3. **Scenario**: Nueva confirmación de una orden ejecutada
   - **Given** que una orden ya fue confirmada como ejecutada, el lote tiene población cero y el galpón está en `vaciado sanitario`
   - **When** el veterinario intenta confirmar nuevamente la misma orden
   - **Then** el sistema rechaza la nueva confirmación y no aplica cambios adicionales

4. **Scenario**: Confirmación por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta confirmar la ejecución de una orden
   - **When** solicita completar la confirmación
   - **Then** el sistema rechaza la operación, conserva la orden pendiente y no modifica el lote ni el galpón

### Edge Cases

- **Edge case #1 - Cambio de la población entre la emisión y la ejecución**

  - ¿Cómo maneja el sistema una población que disminuye después de emitir la orden y antes de confirmar su ejecución?  
    Como la orden comprende el sacrificio total, el sistema debe utilizar la población vigente al confirmar y establecerla directamente en cero.

- **Edge case #2 - Diagnóstico o indicador no disponible**

  - ¿Cómo maneja el sistema una orden cuando no puede verificar el diagnóstico o el indicador `requiere sacrificio sanitario` de la enfermedad?  
    El sistema debe rechazar la emisión, informar que no pudo comprobar la necesidad del sacrificio y conservar el lote y el galpón sin cambios.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir la emisión y la confirmación de órdenes de sacrificio sanitario exclusivamente a usuarios con rol de veterinario.
- **FR-002**: El sistema DEBE permitir la orden únicamente cuando el diagnóstico se relacione con una enfermedad cuyo indicador `requiere sacrificio sanitario` sea verdadero.
- **FR-003**: El galpón relacionado con el diagnóstico DEBE encontrarse en estado `aislamiento` y el lote DEBE tener una población actual mayor que cero al emitir la orden.
- **FR-004**: Cada orden DEBE quedar asociada con un único diagnóstico, el galpón diagnosticado y el lote alojado en él.
- **FR-005**: Antes de confirmar la ejecución, el sistema DEBE verificar que la orden continúa pendiente, el galpón permanece en estado `aislamiento` y el lote conserva población actual mayor que cero.
- **FR-006**: Al confirmar la ejecución, el sistema DEBE cambiar el estado del galpón de `aislamiento` a `vaciado sanitario`.
- **FR-007**: El sistema DEBE establecer la población en cero, cambiar el estado del galpón y marcar la orden como ejecutada como una sola operación.

### Key Entities

- **Orden de sacrificio sanitario**: Representa la instrucción de sacrificar toda la población de un lote por causa de una enfermedad mortal.
  - **Atributos**: alcance total y estado de ejecución.
  - **Relaciones**: corresponde a un único diagnóstico, al galpón diagnosticado y al lote cuya población debe establecerse en cero.
- **Diagnóstico del galpón**: Representa el diagnóstico que sustenta la orden de sacrificio sanitario.
  - **Relaciones**: corresponde al galpón y al lote evaluados, y permite acceder a la enfermedad por medio de su medicación.
- **Enfermedad**: Representa la enfermedad relacionada con el diagnóstico.
  - **Atributos utilizados**: indicador booleano `requiere sacrificio sanitario`.
  - **Relaciones**: cuando el indicador es verdadero, permite que el diagnóstico sustente una orden de sacrificio sanitario total.
- **Lote**: Representa la totalidad de pollos afectados por la orden.
  - **Atributos utilizados**: población actual.
  - **Transición**: su población pasa directamente a cero al confirmar la ejecución.
- **Galpón**: Representa el galpón relacionado con el diagnóstico y el lote afectado.
  - **Atributos utilizados**: estado.
  - **Transición de estado**: pasa de `aislamiento` a `vaciado sanitario` al confirmar la ejecución.

## Success Criteria

### Measurable Outcomes

- **SC-001**: El 100 % de las órdenes creadas corresponde a diagnósticos relacionados con enfermedades cuyo indicador `requiere sacrificio sanitario` es verdadero.
- **SC-002**: El 100 % de las órdenes emitidas comprende toda la población del lote sin solicitar una cantidad al veterinario.
- **SC-003**: El 100 % de las órdenes pendientes conserva la población del lote y el estado `aislamiento` del galpón sin cambios.
- **SC-004**: El 100 % de las órdenes ejecutadas establece la población actual del lote en cero y cambia el galpón a `vaciado sanitario`.
- **SC-005**: El 100 % de los intentos realizados por roles distintos al veterinario es rechazado sin crear órdenes ni modificar el lote o el galpón.
- **SC-006**: El 95 % de las confirmaciones válidas actualiza la orden, el lote y el galpón en un máximo de 1 segundo.
