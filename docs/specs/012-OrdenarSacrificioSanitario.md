# Feature Specification: 012 - Ordenar sacrificio sanitario

**Created**: 2026-09-05

**Updated**: 2026-10-26

## User Scenarios & Testing

### User Story 1 - Emitir una orden de sacrificio sanitario total (Priority: P1)

Como veterinario, quiero emitir una orden de sacrificio sanitario total indicando el motivo clínico cuando el diagnóstico del galpón corresponda a una enfermedad mortal que requiere esta medida, para ordenar la erradicación sanitaria de toda la población del lote alojado dejándola pendiente de ejecución.

**Why this priority**: La orden formaliza la medida de contención biológica ante una enfermedad mortal sobre toda la población afectada y garantiza que esta decisión sea tomada exclusivamente por el veterinario sin alterar anticipadamente la población ni el estado del galpón.

**Independent Test**: Se puede probar utilizando el diagnóstico confirmado de un galpón en estado `aislamiento`, con un lote cuya población actual sea mayor que cero y una enfermedad cuyo indicador `requiere sacrificio sanitario` sea verdadero. El veterinario debe poder emitir una orden con su motivo clínico que abarque toda la población viva sin ingresar una cantidad parcial. La orden debe quedar en estado `PENDIENTE_EJECUCION` y no debe modificar el lote ni el galpón hasta que se confirme su ejecución.

**Acceptance Scenarios**:

1. **Scenario**: Emisión correcta de una orden de sacrificio sanitario total
   - **Given** que el diagnóstico del galpón corresponde a una enfermedad cuyo indicador `requiere sacrificio sanitario` es verdadero, el galpón está en estado `aislamiento`, el lote tiene población actual mayor que cero y no existe otra orden pendiente para el galpón
   - **When** el veterinario emite la orden de sacrificio sanitario ingresando el motivo clínico
   - **Then** el sistema crea una orden asociada con el diagnóstico, el galpón y el lote, captura la población afectada del momento, establece que comprende toda la población en estado `PENDIENTE_EJECUCION` y conserva sin cambios el lote y el galpón

2. **Scenario**: Enfermedad que no requiere sacrificio sanitario
   - **Given** que el diagnóstico corresponde a una enfermedad tratable cuyo indicador `requiere sacrificio sanitario` es falso
   - **When** el veterinario intenta emitir una orden de sacrificio sanitario
   - **Then** el sistema rechaza la operación, informa que la enfermedad diagnosticada no requiere esta medida y no crea la orden

3. **Scenario**: Galpón que no está en aislamiento o lote sin aves vivas
   - **Given** que el diagnóstico corresponde a una enfermedad que requiere sacrificio sanitario, pero el galpón tiene un estado diferente de `aislamiento` o la población actual del lote es igual a cero
   - **When** el veterinario intenta emitir la orden
   - **Then** el sistema rechaza la operación, informa el incumplimiento de la condición operativa y no crea la orden

4. **Scenario**: Intento de emitir una orden duplicada para el mismo galpón
   - **Given** que el galpón ya cuenta con una orden de sacrificio sanitario en estado `PENDIENTE_EJECUCION`
   - **When** el veterinario intenta emitir una segunda orden para el mismo galpón y lote
   - **Then** el sistema rechaza la solicitud informando que ya existe una orden pendiente de ejecución

5. **Scenario**: Emisión por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta emitir una orden de sacrificio sanitario
   - **When** solicita confirmar la orden
   - **Then** el sistema rechaza la operación, no crea la orden y no modifica el lote ni el galpón

---

### User Story 2 - Confirmar la ejecución del sacrificio sanitario total (Priority: P2)

Como veterinario, quiero confirmar que el sacrificio sanitario total fue ejecutado en campo, para establecer atómicamente en cero la población actual del lote, cambiar el estado del galpón de `aislamiento` a `vaciado sanitario` y marcar la orden como ejecutada.

**Why this priority**: La confirmación garantiza que el inventario vivo del lote y el estado operativo del galpón solo cambien después de haberse ejecutado realmente el sacrificio en campo, evitando alteraciones anticipadas o ejecuciones parciales.

**Independent Test**: Se puede probar confirmando una orden en estado `PENDIENTE_EJECUCION` asociada con un lote que tenga población actual mayor que cero y un galpón en estado `aislamiento`. El sistema debe establecer directamente la población actual del lote en cero, cambiar el galpón a `vaciado sanitario` y marcar la orden como `EJECUTADA` dentro de una única operación atómica.

**Acceptance Scenarios**:

1. **Scenario**: Confirmación correcta de la ejecución del sacrificio sanitario total
   - **Given** que existe una orden en estado `PENDIENTE_EJECUCION`, el lote tiene población actual mayor que cero y el galpón continúa en estado `aislamiento`
   - **When** el veterinario confirma que el sacrificio sanitario total fue ejecutado (ingresando observaciones de ejecución opcionales)
   - **Then** el sistema establece la población actual del lote en cero en el módulo 1, cambia el estado del galpón a `vaciado sanitario`, marca la orden como `EJECUTADA` y publica el evento de sacrificio sanitario ejecutado para el módulo 3

2. **Scenario**: Orden pendiente sin ejecución confirmada
   - **Given** que existe una orden de sacrificio sanitario en estado `PENDIENTE_EJECUCION`
   - **When** su ejecución todavía no ha sido confirmada por el veterinario
   - **Then** el sistema conserva la población actual del lote y el estado `aislamiento` del galpón sin cambios

3. **Scenario**: Nueva confirmación sobre una orden ya ejecutada
   - **Given** que una orden ya fue confirmada previamente como `EJECUTADA`, el lote tiene población cero y el galpón está en `vaciado sanitario`
   - **When** el veterinario intenta confirmar nuevamente la misma orden
   - **Then** el sistema rechaza la nueva confirmación con error de conflicto (HTTP 409) y no aplica cambios ni eventos adicionales

4. **Scenario**: Confirmación por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta confirmar la ejecución de una orden
   - **When** solicita completar la confirmación
   - **Then** el sistema rechaza la operación, conserva la orden en estado `PENDIENTE_EJECUCION` y no modifica el lote ni el galpón

---

### Edge Cases

- **Edge case #1 - Cambio de la población entre la emisión y la ejecución**
  - ¿Cómo maneja el sistema una población viva que disminuye por mortalidad entre la emisión de la orden y la confirmación de su ejecución?  
    Como la orden comprende el sacrificio sanitario total (100 % de las aves alojadas), el sistema debe verificar que aún existan aves vivas al confirmar y establecer la población actual del lote directamente en cero.

- **Edge case #2 - Diagnóstico o indicador de sacrificio no disponible**
  - ¿Cómo maneja el sistema una solicitud de emisión cuando no puede verificar el diagnóstico o el indicador `requiere sacrificio sanitario`?  
    El sistema debe rechazar la emisión, informar que no pudo comprobar la condición diagnóstica requerida y conservar el lote y el galpón sin cambios.

- **Edge case #3 - Fallo de disponibilidad al actualizar el lote o el galpón en el módulo 1**
  - ¿Cómo maneja el sistema una interrupción o indisponibilidad del módulo 1 durante la confirmación de la ejecución?  
    La confirmación es una transacción atómica indivisible: si no se puede fijar la población en cero o cambiar el estado del galpón a `vaciado sanitario`, toda la operación se revierte (rollback) y la orden permanece en `PENDIENTE_EJECUCION`.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir la emisión y la confirmación de órdenes de sacrificio sanitario exclusivamente a usuarios autenticados con rol de veterinario (permitiendo la consulta de estado y detalle a veterinarios y administradores).
- **FR-002**: El sistema DEBE exponer un mecanismo de consulta (endpoint GET) que permita obtener el detalle y estado de una orden de sacrificio sanitario específica.
- **FR-003**: El sistema DEBE permitir emitir la orden únicamente cuando exista un diagnóstico confirmado en el galpón cuyo indicador `requiere sacrificio sanitario` sea estrictamente verdadero.
- **FR-004**: El galpón relacionado con el diagnóstico DEBE encontrarse en estado `aislamiento` y el lote activo DEBE tener una población actual mayor que cero tanto al emitir la orden como al confirmar su ejecución.
- **FR-005**: Cada orden DEBE quedar asociada con un único diagnóstico (`diagnosticoId`), el galpón diagnosticado (`galponId`), el lote alojado (`loteId`) y el motivo clínico obligatorio, impidiendo emitir una segunda orden pendiente para el mismo galpón.
- **FR-006**: La orden DEBE comprender invariablemente el 100 % de la población del lote alojado (alcance total), sin solicitar ni permitir ingresar cantidades parciales ni pesajes comerciales.
- **FR-007**: Mientras la orden se encuentre en estado `PENDIENTE_EJECUCION`, el sistema NO DEBE modificar la población del lote ni cambiar el estado `aislamiento` del galpón.
- **FR-008**: Al confirmar la ejecución, el sistema DEBE establecer la población actual del lote en exactamente cero, cambiar el estado del galpón de `aislamiento` a `vaciado sanitario` y marcar la orden como `EJECUTADA` dentro de una sola operación atómica local.
- **FR-009**: Una orden en estado `EJECUTADA` DEBE ser inmutable y rechazar cualquier intento posterior de re-confirmación o edición.
- **FR-010**: Tras confirmar la ejecución, el sistema DEBE publicar el evento de integración (`SacrificioEjecutadoEvent`) para que el módulo 3 registre la baja contable del lote como merma sanitaria sin liquidación de venta comercial.
- **FR-011**: El sistema DEBE soportar cabeceras de idempotencia técnica (`X-Idempotency-Key`) y control de concurrencia optimista (`@Version`) para evitar la emisión duplicada de órdenes o la doble ejecución de una misma orden ante reintentos de red.

### Non-Functional Requirements

| ID          | Categoría      | Requerimiento                                                                              | Métrica                                 |
| :---------- | :------------- | :----------------------------------------------------------------------------------------- | :-------------------------------------- |
| **NFR-001** | Rendimiento    | Latencia de emisión, confirmación y persistencia                                           | < 1 segundo (p95) bajo carga normal     |
| **NFR-002** | Seguridad      | Acceso restringido exclusivamente al rol VETERINARIO (emisión/confirmación)                | HTTP 403 Forbidden a otros roles        |
| **NFR-003** | Integridad     | Escritura atómica (orden + actualización Módulo 1) bajo `@Transactional`                   | 100% de consistencia local              |
| **NFR-004** | Disponibilidad | Si el Módulo 1 no responde durante la confirmación, la transacción se aborta completamente | 0% de inconsistencias por fallo cruzado |

### Key Entities

- **Orden de sacrificio sanitario**: Representa la instrucción veterinaria de sacrificar toda la población de un lote por causa de una enfermedad mortal.
  - **Atributos**: identificador UUID, `diagnosticoId`, `galponId`, `loteId`, `poblacionAfectadaMomento`, `motivoClinico`, `emitidaPor`, `emitidaEn`, `ejecutadaPor` (opcional), `ejecutadaEn` (opcional), `observacionesEjecucion` (opcional) y `estado` (`PENDIENTE_EJECUCION`, `EJECUTADA`).
  - **Relaciones**: corresponde de forma unívoca a un diagnóstico elegible (Spec 011), al galpón en `aislamiento` y al lote activo cuya población se extinguirá a cero.
- **Diagnóstico del galpón**: Representa el diagnóstico clínico que habilita la orden de sacrificio sanitario.
  - **Relaciones**: corresponde al galpón y al lote evaluados, referencia directamente a la Enfermedad diagnosticada sin medicación asociada y conserva el indicador `requiere sacrificio sanitario` en verdadero.
- **Enfermedad**: Representa la patología mortal diagnosticada (Spec 009).
  - **Atributos utilizados**: indicador booleano `requiere sacrificio sanitario` en verdadero.
- **Lote**: Representa la totalidad de aves afectadas por la orden, propiedad del módulo 1.
  - **Atributos utilizados**: población actual (`> 0` antes de ejecutar; pasa a `0` al confirmar la ejecución).
- **Galpón**: Representa la instalación física afectada, propiedad del módulo 1.
  - **Atributos utilizados**: estado (pasa de `aislamiento` a `vaciado sanitario` al confirmar la ejecución).

## Success Criteria

### Measurable Outcomes

- **SC-001**: El 100 % de las órdenes creadas corresponde a diagnósticos confirmados cuyo indicador `requiere sacrificio sanitario` es verdadero.
- **SC-002**: El 100 % de las órdenes emitidas comprende toda la población del lote sin solicitar una cantidad parcial al veterinario.
- **SC-003**: El 100 % de las órdenes en estado `PENDIENTE_EJECUCION` conserva la población del lote y el estado `aislamiento` del galpón sin cambios.
- **SC-004**: El 100 % de las órdenes confirmadas como `EJECUTADA` establece la población actual del lote en cero y cambia el galpón a `vaciado sanitario` de forma atómica.
- **SC-005**: El 100 % de los intentos de emisión o confirmación realizados por roles distintos al veterinario es rechazado sin crear órdenes ni modificar el lote o el galpón.
- **SC-006**: El 95 % de las emisiones y confirmaciones válidas se procesa y actualiza en un máximo de 1 segundo.
- **SC-007**: Cero duplicados (0%) en la creación de órdenes o ejecuciones gracias al uso de la cabecera `X-Idempotency-Key`.
