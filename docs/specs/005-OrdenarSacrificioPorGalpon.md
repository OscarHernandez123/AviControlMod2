# Feature Specification: Ordenar sacrificio por galpón

**Created**: 2026-09-02  

## User Scenarios & Testing

### User Story 1 - Programar el sacrificio de un lote por galpón (Priority: P1)

Como administrador, quiero programar la fecha y hora del sacrificio de un lote alojado en un galpón, ambos proporcionados por el módulo 1, después de validar su aptitud, para que el sistema gestione la orden y cambie el estado del galpón a `vaciado sanitario` en el momento programado.

**Why this priority**: La programación permite establecer cuándo se realizará el sacrificio y mantener el galpón ocupado hasta ese momento, evitando que se libere antes de finalizar el ciclo de crianza.

**Independent Test**: Se puede probar utilizando un galpón y su lote actualmente alojado proporcionados por el módulo 1, validando el lote para que el galpón quede en estado `En cosecha`, programando una fecha y hora futuras y verificando que la orden pueda reprogramarse o cancelarse antes de su ejecución. Al llegar el momento programado, el sistema debe cambiar el estado de la entidad Galpón a `vaciado sanitario` y finalizar la relación de alojamiento del lote.

**Acceptance Scenarios**:

1. **Scenario**: Programación correcta del sacrificio
   - **Given** que el módulo 1 proporciona un galpón en estado `En cosecha` y el lote validado continúa alojado en él
   - **When** programa el sacrificio para una fecha y hora futuras
   - **Then** el sistema crea la orden asociada al lote y al galpón, y mantiene el galpón en estado `En cosecha` hasta el momento programado

2. **Scenario**: Ejecución de la orden en la fecha programada
   - **Given** que existe una orden de sacrificio programada y vigente
   - **When** llega la fecha y hora establecidas
   - **Then** el sistema marca la orden como ejecutada, cambia el estado del galpón a `vaciado sanitario` y finaliza el alojamiento del lote

3. **Scenario**: Intento de programación sin una validación apta
   - **Given** que el galpón no se encuentra en estado `En cosecha`
   - **When** intenta programar un sacrificio
   - **Then** el sistema rechaza la operación e indica que primero debe validar la aptitud del lote

4. **Scenario**: Intento de programación con una fecha u hora no futura
   - **Given** que el lote fue validado y el galpón se encuentra en estado `En cosecha`
   - **When** intenta programar el sacrificio para la fecha y hora actuales o anteriores
   - **Then** el sistema rechaza la programación e indica que debe seleccionar una fecha y hora futuras

5. **Scenario**: Reprogramación antes de la ejecución
   - **Given** que existe una orden programada cuya fecha y hora aún no han llegado
   - **When** el administrador selecciona una nueva fecha y hora futuras
   - **Then** el sistema actualiza la programación y conserva el galpón en estado `En cosecha`

6. **Scenario**: Cancelación antes de la ejecución
   - **Given** que existe una orden programada cuya fecha y hora aún no han llegado
   - **When** el administrador cancela la orden
   - **Then** el sistema marca la orden como cancelada y conserva el estado del galpón y el alojamiento del lote sin cambios

7. **Scenario**: Operación por un usuario no autorizado
   - **Given** que un usuario sin rol de administrador intenta programar, reprogramar o cancelar una orden de sacrificio
   - **When** solicita confirmar la operación
   - **Then** el sistema la rechaza y no modifica la orden, el galpón ni el lote

### Edge Cases

- **Edge case #1 - Cambio del lote o del estado del galpón antes de confirmar la orden**

  - ¿Cómo maneja el sistema un galpón cuyo lote alojado o estado cambia después de quedar `En cosecha` y antes de confirmar la programación?
    El sistema debe consultar nuevamente en el módulo 1 que el mismo lote continúa alojado y que el galpón permanece en estado `En cosecha`. Si alguno de estos datos cambió, debe rechazar la programación e informar la inconsistencia.

- **Edge case #2 - Reprogramación o cancelación al mismo tiempo que se ejecuta la orden**

  - ¿Cómo maneja el sistema una solicitud de reprogramación o cancelación recibida cuando ya se alcanzó la fecha y hora programadas?  
    El sistema debe ejecutar una sola operación de forma consistente. Si la orden ya comenzó a ejecutarse o fue ejecutada, debe rechazar la modificación y conservar el resultado de la ejecución.

- **Edge case #3 - El sistema no está disponible en el momento programado**

  - ¿Cómo maneja el sistema una orden cuya fecha y hora se cumplen mientras el sistema se encuentra temporalmente fuera de servicio?  
    Al restablecerse, el sistema debe identificar la orden vencida, ejecutarla una sola vez, cambiar el galpón a `vaciado sanitario` y finalizar el alojamiento del lote.

- **Edge case #4 - Información incompleta o no disponible desde el módulo 1**

  - ¿Cómo maneja el sistema una programación cuando el módulo 1 no está disponible o no permite verificar el galpón y su lote actualmente alojado?
    El sistema debe rechazar la programación, informar que no pudo verificar las condiciones y no crear ni modificar la orden.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir programar, reprogramar y cancelar órdenes de sacrificio exclusivamente a usuarios con rol de administrador.
- **FR-002**: La opción de programar el sacrificio DEBE estar disponible únicamente cuando el módulo 1 confirme que el lote validado continúa alojado en el galpón y que la entidad Galpón se encuentra en estado `En cosecha`.
- **FR-003**: Toda programación o reprogramación DEBE establecer una fecha y hora futuras.
- **FR-004**: La orden DEBE quedar asociada a las entidades Lote y Galpón correspondientes proporcionadas por el módulo 1.
- **FR-005**: El sistema DEBE permitir reprogramar o cancelar una orden únicamente antes de la fecha y hora programadas.
- **FR-006**: La entidad Galpón proporcionada por el módulo 1 DEBE permanecer en estado `En cosecha` mientras la orden esté programada o sea reprogramada, y la cancelación NO DEBE modificar el galpón ni el alojamiento del lote.
- **FR-007**: Al llegar la fecha y hora programadas, el sistema DEBE marcar la orden como ejecutada, cambiar a `vaciado sanitario` el estado de la entidad Galpón proporcionada por el módulo 1 y finalizar la relación de alojamiento del lote.
- **FR-008**: Antes de crear o reprogramar una orden, el sistema DEBE utilizar la información vigente de las entidades Galpón y Lote proporcionadas por el módulo 1 y rechazar la operación si no puede verificarla completamente.

### Key Entities

- **Orden de sacrificio**: Representa la programación del sacrificio de un lote de aves.
  - **Atributos posibles**: fecha y hora programadas, estado, fecha de creación, fecha de reprogramación y fecha de cancelación.
  - **Relaciones**: corresponde a un lote de aves, a su galpón y al administrador responsable.
- **Galpón**: Representa el espacio ocupado por el lote hasta la ejecución de la orden y es proporcionado por el módulo 1.
  - **Atributos utilizados**: estado.
  - **Relaciones**: aloja el lote asociado y tiene una orden de sacrificio programada.
- **Lote de aves**: Representa el grupo de aves cuyo sacrificio se programa y es proporcionado por el módulo 1.
  - **Atributos relevantes**: fecha de ingreso, población actual y estado.
  - **Relaciones**: se encuentra alojado en el galpón y está asociado con la orden de sacrificio.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los administradores puede programar el sacrificio en menos de 30 segundos después de que el galpón quede en estado `En cosecha`.
- **SC-002**: El 95 % de las operaciones de programación, reprogramación y cancelación confirma su resultado en un máximo de 1 segundo.
- **SC-003**: Al menos el 99 % de las órdenes ejecutadas cambia el estado del galpón a `vaciado sanitario` dentro del primer minuto posterior a la fecha y hora programadas.
- **SC-004**: Al menos el 90 % de los administradores puede reprogramar o cancelar una orden correctamente en el primer intento durante pruebas de usabilidad.
