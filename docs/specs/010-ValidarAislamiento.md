# Feature Specification: 010 - Validar aislamiento de un galpón

**Created**: 2026-09-04  
**Last Updated**: 2026-10-28

## User Scenarios & Testing

### User Story 1 - Evaluar y validar o rechazar el aislamiento solicitado (Priority: P1)

Como veterinario, quiero recibir y consultar las solicitudes de revisión enviadas por los trabajadores y evaluar el galpón proporcionado por el módulo 1, para aprobar su aislamiento cuando corresponda cambiando su estado de `productivo` a `aislamiento`, o rechazar la solicitud si se descarta el riesgo conservando su estado `productivo`.

**Why this priority**: La solicitud del trabajador permite alertar una sospecha en campo, pero la evaluación clínica y la decisión de aislar el galpón o desestimar la solicitud son responsabilidades exclusivas del veterinario antes de proceder al diagnóstico (Spec 011).

**Independent Test**: Se puede probar utilizando una solicitud de revisión en estado `PENDIENTE_VALIDACION` enviada por un trabajador para un galpón proporcionado por el módulo 1 en estado `productivo`. El sistema debe permitir que únicamente un veterinario evalúe la solicitud: si la aprueba y el galpón continúa en estado `productivo`, cambia el estado del galpón a `aislamiento`; si la rechaza registrando observaciones, la solicitud queda rechazada y el galpón conserva su estado `productivo`.

**Acceptance Scenarios**:

1. **Scenario**: Inicio de la evaluación a partir de la solicitud del trabajador
   - **Given** que un trabajador envió al veterinario una solicitud de revisión asociada con un galpón proporcionado por el módulo 1
   - **When** el veterinario consulta la bandeja de solicitudes pendientes y comienza la evaluación
   - **Then** el sistema le permite evaluar el galpón y conserva su estado sin cambios hasta que el veterinario registre su decisión

2. **Scenario**: Validación (aprobación) correcta del aislamiento
   - **Given** que el veterinario está evaluando una solicitud pendiente enviada por un trabajador y el módulo 1 confirma que el galpón continúa en estado `productivo`
   - **When** el veterinario aprueba el aislamiento registrando sus observaciones clínicas
   - **Then** el sistema registra la decisión aprobada, solicita al módulo 1 cambiar y guardar el estado del galpón como `aislamiento` y habilita el registro del diagnóstico del galpón (Spec 011)

3. **Scenario**: Rechazo clínico de la solicitud de aislamiento
   - **Given** que el veterinario evalúa una solicitud pendiente sobre un galpón en estado `productivo` y determina que no amerita aislamiento
   - **When** el veterinario rechaza la solicitud registrando sus observaciones técnicas
   - **Then** el sistema registra la decisión de rechazo sobre la solicitud y conserva el estado del galpón como `productivo` sin alterarlo

4. **Scenario**: Intento de validación sin solicitud previa
   - **Given** que el veterinario no cuenta con una solicitud de revisión pendiente enviada por un trabajador para el galpón
   - **When** intenta validar el aislamiento directamente
   - **Then** el sistema rechaza la operación, informa que se requiere una solicitud previa pendiente y conserva el estado actual del galpón

5. **Scenario**: Intento de validación por el trabajador u otro usuario no autorizado
   - **Given** que un trabajador u otro usuario sin rol de veterinario intenta evaluar o decidir sobre una solicitud de aislamiento
   - **When** solicita ejecutar la acción
   - **Then** el sistema rechaza la operación y no modifica la solicitud ni el estado del galpón

6. **Scenario**: Galpón que ya no está en estado productivo al momento de decidir
   - **Given** que el veterinario recibió la solicitud, pero el módulo 1 informa que el estado vigente del galpón es diferente de `productivo`
   - **When** intenta aprobar el aislamiento
   - **Then** el sistema rechaza la validación, informa que el galpón no se encuentra en el estado requerido (`productivo`) y conserva su estado actual

7. **Scenario**: Información del galpón incompleta o módulo 1 no disponible
   - **Given** que el módulo 1 no está disponible o no permite conocer el estado vigente del galpón asociado con la solicitud
   - **When** el veterinario intenta completar la validación
   - **Then** el sistema rechaza la operación, informa que no pudo verificar el galpón y revierte cualquier cambio parcial

---

### Edge Cases

- **Edge case #1 - Cambio del estado del galpón después de recibir la solicitud**
  - ¿Cómo maneja el sistema un galpón cuyo estado cambia en el módulo 1 después de que el veterinario recibe la solicitud y antes de confirmar la validación?  
    El sistema debe consultar nuevamente en el módulo 1 el estado vigente del galpón justo antes de confirmar. Solo debe permitir la transición a `aislamiento` si el estado continúa siendo `productivo`; en caso contrario, debe rechazar la operación con código de conflicto e informar la inconsistencia.

- **Edge case #2 - Repetición de una decisión sobre una solicitud ya resuelta o galpón ya aislado**
  - ¿Cómo maneja el sistema un nuevo intento de validación sobre una solicitud que ya fue decidida o sobre un galpón que ya se encuentra en estado `aislamiento`?  
    El sistema debe rechazar el nuevo intento de forma idempotente, indicar que la solicitud ya fue procesada o que el galpón ya se encuentra en `aislamiento`, y conservar su estado sin cambios adicionales.

- **Edge case #3 - Información incompleta o fallo de comunicación con el módulo 1**
  - ¿Cómo maneja el sistema la evaluación cuando el módulo 1 no está disponible o devuelve información incompleta del galpón?  
    El sistema debe impedir que el veterinario complete la validación, conservar la solicitud en estado pendiente y no cambiar el estado del galpón utilizando datos desactualizados.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir que únicamente los usuarios autenticados con rol de veterinario evalúen solicitudes y aprueben o rechacen el aislamiento de un galpón.
- **FR-002**: El sistema DEBE exponer un mecanismo de consulta (endpoint `GET`) para que el veterinario pueda listar y filtrar las solicitudes de aislamiento en estado `PENDIENTE_VALIDACION`.
- **FR-003**: El veterinario DEBE disponer de una solicitud de revisión en estado pendiente enviada previamente por un trabajador (Spec 020) para poder ejecutar la validación del aislamiento.
- **FR-004**: El envío o la consulta de la solicitud únicamente DEBE iniciar el flujo de revisión y NO DEBE permitir que el trabajador evalúe, valide ni cambie el estado del galpón.
- **FR-005**: La solicitud de revisión DEBE estar asociada con la entidad Galpón y su lote activo proporcionados por el módulo 1.
- **FR-006**: Antes de confirmar la decisión, el sistema DEBE consultar nuevamente en el módulo 1 la entidad Galpón asociada con la solicitud y verificar su estado vigente en tiempo real.
- **FR-007**: El sistema DEBE permitir aprobar el aislamiento únicamente cuando la entidad Galpón proporcionada por el módulo 1 se encuentre en estado `productivo`.
- **FR-008**: Cuando el veterinario apruebe la validación, el sistema DEBE registrar la decisión con sus observaciones, actualizar el estado de la solicitud y solicitar al módulo 1 cambiar el estado de la entidad Galpón de `productivo` a `aislamiento`.
- **FR-009**: Cuando el veterinario rechace la solicitud, el sistema DEBE registrar la decisión de rechazo junto con las observaciones del veterinario y conservar el estado del galpón en `productivo`.
- **FR-010**: Cuando el galpón no se encuentre en estado `productivo` o el módulo 1 no esté disponible, el sistema DEBE rechazar la validación, informar la razón y no guardar cambios parciales.
- **FR-011**: La validación NO DEBE modificar ningún dato de la entidad Galpón distinto de su estado operativo ni alterar la población del lote.
- **FR-012**: Al confirmar una decisión (aprobación o rechazo), el sistema DEBE publicar un evento de integración (`AislamientoValidadoEvent` o `AislamientoRechazadoEvent`) para garantizar la consistencia con otros módulos.
- **FR-013**: El sistema DEBE soportar cabeceras de idempotencia técnica (`X-Idempotency-Key`) y control de concurrencia optimista para evitar el procesamiento duplicado de decisiones ante reintentos de red.

### Non-Functional Requirements

| ID          | Categoría      | Requerimiento                                                                                     | Métrica                                 |
| :---------- | :------------- | :------------------------------------------------------------------------------------------------ | :-------------------------------------- |
| **NFR-001** | Rendimiento    | Latencia de validación, rechazo y persistencia                                                    | < 1 segundo (p95) bajo carga normal     |
| **NFR-002** | Seguridad      | Acceso restringido exclusivamente al rol VETERINARIO                                              | HTTP 403 Forbidden a otros roles        |
| **NFR-003** | Integridad     | Escritura atómica de la decisión y actualización de estado del galpón                             | 100% de consistencia local              |
| **NFR-004** | Disponibilidad | Si el Módulo 1 no responde al verificar o mutar el estado, la transacción se aborta completamente | 0% de inconsistencias por fallo cruzado |

### Key Entities

- **Solicitud de aislamiento**: Representa la petición inmutable enviada por el trabajador (Spec 020) que actúa como antecedente obligatorio para la evaluación veterinaria.
  - **Relaciones**: corresponde a un único galpón y a su lote activo proporcionados por el módulo 1.
  - **Origen y efecto**: es enviada por un trabajador en estado `PENDIENTE_VALIDACION` y evaluada por el veterinario; su creación o consulta no cambia el estado del galpón.
- **Decisión de aislamiento**: Representa el dictamen inmutable emitido por el veterinario sobre una solicitud pendiente.
  - **Atributos**: identificador UUID, referencia a la solicitud (`solicitudId`), veterinario responsable (`veterinarioId`), fecha y hora de decisión, resultado (`APROBADA` o `RECHAZADA`) y observaciones.
- **Galpón**: Representa la unidad física cuya necesidad de aislamiento evalúa el veterinario y es propiedad del módulo 1.
  - **Atributos utilizados**: estado (`productivo`, `aislamiento`).
  - **Transición de estado**: pasa de `productivo` a `aislamiento` únicamente cuando el veterinario aprueba la solicitud de aislamiento.

## Success Criteria

### Measurable Outcomes

- **SC-001**: El 100 % de las decisiones de aislamiento se ejecuta sobre una solicitud previa enviada por un trabajador.
- **SC-002**: El 100 % de los intentos de validación o rechazo realizados por roles distintos al veterinario es rechazado sin modificar la solicitud ni el estado del galpón.
- **SC-003**: Al menos el 90 % de los veterinarios puede completar la decisión sobre una solicitud en menos de 15 segundos después de revisarla.
- **SC-004**: El 95 % de las validaciones aprobadas establece el estado `aislamiento` en un máximo de 1 segundo después de su confirmación.
- **SC-005**: El 100 % de las solicitudes enviadas, consultadas o rechazadas conserva el estado del galpón sin cambios no autorizados.
- **SC-006**: Cero duplicados (0%) en publicaciones de eventos o cambios de estado gracias al uso de la cabecera `X-Idempotency-Key`.
