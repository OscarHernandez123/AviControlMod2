# Feature Specification: Ordenar sacrificio sanitario

**Created**: 2026-09-04  

## User Scenarios & Testing

### User Story 1 - Emitir una orden de sacrificio sanitario (Priority: P1)

Como veterinario, quiero emitir una orden de sacrificio sanitario indicando la cantidad de pollos que deben sacrificarse cuando el diagnóstico corresponda a una enfermedad de tipo `plaga`, para definir la intervención sanitaria requerida sobre el lote.

**Why this priority**: La orden permite responder a un diagnóstico de plaga y establece cuántos pollos deben sacrificarse, garantizando que esta decisión sea tomada exclusivamente por el veterinario.

**Independent Test**: Se puede probar utilizando un diagnóstico relacionado con una enfermedad de tipo `plaga` y un lote cuya población actual sea de 1.000 pollos. Al ordenar el sacrificio de 200 pollos, el sistema debe crear la orden pendiente de ejecución y conservar la población del lote en 1.000. Si el diagnóstico no corresponde a una plaga, la cantidad no es válida o el usuario no es veterinario, debe rechazar la operación.

**Acceptance Scenarios**:

1. **Scenario**: Emisión correcta de una orden de sacrificio sanitario
   - **Given** que existe un diagnóstico relacionado con una enfermedad de tipo `plaga` y el lote tiene una población actual mayor que cero
   - **When** el veterinario ordena sacrificar una cantidad de pollos mayor que cero y menor o igual que la población actual
   - **Then** el sistema crea la orden asociada con el diagnóstico y el lote, registra la cantidad indicada y la deja pendiente de ejecución

2. **Scenario**: Diagnóstico que no corresponde a una plaga
   - **Given** que el diagnóstico se relaciona con una enfermedad cuyo tipo es diferente de `plaga`
   - **When** el veterinario intenta emitir la orden de sacrificio sanitario
   - **Then** el sistema rechaza la operación, informa que el diagnóstico no corresponde a una plaga y no crea la orden

3. **Scenario**: Cantidad mayor que la población actual
   - **Given** que el lote tiene una población actual de 500 pollos
   - **When** el veterinario intenta ordenar el sacrificio de más de 500 pollos
   - **Then** el sistema rechaza la operación, informa que la cantidad excede la población disponible y no crea la orden

4. **Scenario**: Cantidad igual a cero, negativa o no entera
   - **Given** que el veterinario está emitiendo una orden de sacrificio sanitario
   - **When** ingresa una cantidad igual a cero, negativa o no entera
   - **Then** el sistema rechaza la operación, indica que la cantidad debe ser un número entero mayor que cero y no crea la orden

5. **Scenario**: Emisión por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta emitir una orden de sacrificio sanitario
   - **When** solicita confirmar la orden
   - **Then** el sistema rechaza la operación, no crea la orden y no modifica la población del lote

---

### User Story 2 - Confirmar la ejecución del sacrificio sanitario (Priority: P2)

Como veterinario, quiero confirmar que una orden de sacrificio sanitario fue ejecutada, para descontar de la población actual del lote la cantidad de pollos sacrificados.

**Why this priority**: La confirmación permite actualizar la población únicamente cuando el sacrificio realmente se haya ejecutado, evitando descontar pollos por órdenes que todavía están pendientes.

**Independent Test**: Se puede probar confirmando la ejecución de una orden pendiente por 200 pollos sobre un lote cuya población vigente sea de 1.000. El sistema debe marcar la orden como ejecutada y actualizar la población del lote a 800. Si la orden comprende los 1.000 pollos, la nueva población debe ser cero.

**Acceptance Scenarios**:

1. **Scenario**: Confirmación de un sacrificio parcial
   - **Given** que existe una orden pendiente por 200 pollos y la población vigente del lote es de 1.000 pollos
   - **When** el veterinario confirma que el sacrificio fue ejecutado
   - **Then** el sistema marca la orden como ejecutada y actualiza la población del lote a 800 pollos

2. **Scenario**: Confirmación por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta confirmar la ejecución de una orden
   - **When** solicita completar la confirmación
   - **Then** el sistema rechaza la operación, conserva la orden pendiente y no modifica la población del lote

3. **Scenario**: Población vigente menor que la cantidad ordenada
   - **Given** que después de emitir la orden la población del lote disminuyó y ahora es menor que la cantidad indicada
   - **When** el veterinario intenta confirmar la ejecución
   - **Then** el sistema rechaza la confirmación, informa que la cantidad excede la población vigente y no actualiza el lote

### Edge Cases

- **Edge case #1 - Cambio de la población entre la emisión y la ejecución**

  - ¿Cómo maneja el sistema una población que cambia después de emitir la orden y antes de confirmar su ejecución?  
    El sistema debe utilizar la población vigente al momento de la confirmación. Si continúa siendo igual o superior a la cantidad ordenada, debe realizar el descuento sobre ese valor; si es inferior, debe rechazar la confirmación.

- **Edge case #2 - Ejecuciones o confirmaciones simultáneas**

  - ¿Cómo maneja el sistema dos intentos de confirmar simultáneamente la misma orden?  
    El sistema debe aceptar una sola confirmación, marcar la orden como ejecutada una sola vez y realizar un único descuento sobre la población del lote.

- **Edge case #3 - Diagnóstico o enfermedad no disponible**

  - ¿Cómo maneja el sistema una orden cuando no puede verificar el diagnóstico o el tipo de la enfermedad relacionada?  
    El sistema debe rechazar la emisión de la orden, informar que no pudo confirmar que la enfermedad sea una plaga y conservar la población del lote sin cambios.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir la emisión y la confirmación de órdenes de sacrificio sanitario exclusivamente a usuarios con rol de veterinario.
- **FR-002**: El sistema DEBE permitir la orden únicamente cuando el diagnóstico se relacione con una enfermedad cuyo tipo sea `plaga`.
- **FR-003**: Cada orden DEBE quedar asociada con el diagnóstico y el lote correspondientes.
- **FR-004**: Al emitir la orden, el veterinario DEBE indicar la cantidad de pollos que deben sacrificarse.
- **FR-005**: La cantidad indicada DEBE ser un número entero mayor que cero y menor o igual que la población actual del lote.
- **FR-006**: Al emitir una orden válida, el sistema DEBE registrar la cantidad indicada y mantener la orden pendiente de ejecución.
- **FR-007**: Emitir una orden NO DEBE modificar la población actual del lote.
- **FR-008**: El sistema DEBE actualizar la población del lote únicamente cuando el veterinario confirme que el sacrificio fue ejecutado.
- **FR-009**: Antes de confirmar la ejecución, el sistema DEBE comprobar nuevamente la población vigente del lote y rechazar la operación si es menor que la cantidad ordenada.
- **FR-010**: Al confirmar la ejecución, el sistema DEBE calcular la nueva población mediante la fórmula `población actual − cantidad sacrificada`.
- **FR-011**: El sistema DEBE permitir que la nueva población del lote sea igual a cero cuando la cantidad sacrificada sea igual a su población vigente.
- **FR-012**: El sistema DEBE marcar la orden como ejecutada y actualizar la población del lote como una sola operación.
- **FR-013**: Una orden ejecutada NO DEBE poder confirmarse nuevamente ni producir descuentos adicionales sobre la población.
- **FR-014**: La emisión o ejecución de la orden NO DEBE modificar el diagnóstico, la enfermedad, la medicación ni el galpón relacionados.

### Key Entities

- **Orden de sacrificio sanitario**: Representa la instrucción emitida para sacrificar una cantidad determinada de pollos por causa sanitaria.
  - **Atributos**: cantidad de pollos y estado de ejecución.
  - **Relaciones**: corresponde a un único diagnóstico y al lote cuya población debe actualizarse.
- **Diagnóstico del galpón**: Representa el diagnóstico que sustenta la orden de sacrificio sanitario.
  - **Relaciones**: corresponde al galpón y al lote evaluados, y permite acceder a la enfermedad por medio de su medicación.
- **Enfermedad**: Representa la enfermedad relacionada con el diagnóstico.
  - **Atributos utilizados**: tipo de enfermedad.
  - **Relaciones**: debe ser de tipo `plaga` para permitir la orden de sacrificio sanitario.
- **Lote**: Representa el grupo de pollos afectado por la orden.
  - **Atributos utilizados**: población actual.
  - **Relaciones**: corresponde al diagnóstico y a la orden; su población se reduce únicamente cuando se confirma la ejecución.
- **Galpón**: Representa el galpón relacionado con el diagnóstico y el lote afectado.
  - **Relaciones**: contiene el lote cuya población es actualizada después de ejecutar la orden.

## Success Criteria

### Measurable Outcomes

- **SC-001**: El 100 % de las órdenes creadas corresponde a diagnósticos relacionados con enfermedades de tipo `plaga`.
- **SC-002**: El 100 % de las órdenes pendientes conserva la población actual del lote sin cambios.
- **SC-003**: El 100 % de las órdenes confirmadas calcula la nueva población como `población actual − cantidad sacrificada` sin producir valores negativos.
- **SC-004**: El 100 % de las órdenes por la totalidad de la población establece la población actual del lote en cero después de confirmar su ejecución.
- **SC-005**: El 100 % de los intentos realizados por roles distintos al veterinario es rechazado sin crear órdenes ni modificar la población.
- **SC-006**: El 95 % de las confirmaciones válidas actualiza la orden y la población del lote en un máximo de 1 segundo.
