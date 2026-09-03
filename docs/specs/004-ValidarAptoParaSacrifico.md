# Feature Specification: Validar aptitud de un galpón para sacrificio

**Created**: 2026-09-02  

## User Scenarios & Testing

### User Story 1 - Validar la aptitud de un galpón para sacrificio (Priority: P1)

Como administrador, quiero validar si el lote de aves alojado actualmente en un galpón es apto para sacrificio después de consultar su edad, para conocer si cumple las condiciones requeridas.

**Why this priority**: La validación permite confirmar de forma clara si el lote alcanzó la edad mínima y si el galpón se encuentra en el estado operativo requerido antes de continuar con el proceso de sacrificio.

**Independent Test**: Se puede probar consultando la edad de un lote y ejecutando posteriormente la validación. El sistema debe mostrar `Apto` únicamente cuando la edad sea de 45 días o más y el galpón esté en estado `En producción`; en cualquier otro caso debe mostrar `No apto` y las razones correspondientes.

**Acceptance Scenarios**:

1. **Scenario**: Galpón apto para sacrificio
   - **Given** que el administrador consultó correctamente la edad de un lote alojado actualmente, el lote tiene 45 días o más y el galpón está en estado `En producción`
   - **When** selecciona la opción de validar aptitud para sacrificio
   - **Then** el sistema muestra el resultado `Apto`

2. **Scenario**: Lote que no cumple la edad mínima
   - **Given** que el administrador consultó correctamente la edad de un lote de 44 días y el galpón está en estado `En producción`
   - **When** selecciona la opción de validar aptitud para sacrificio
   - **Then** el sistema muestra `No apto` e indica que el lote no cumple la edad mínima de 45 días

3. **Scenario**: Galpón que no está en producción
   - **Given** que el administrador consultó correctamente la edad de un lote de 45 días o más y el galpón no está en estado `En producción`
   - **When** selecciona la opción de validar aptitud para sacrificio
   - **Then** el sistema muestra `No apto` e indica que el galpón no se encuentra en producción

4. **Scenario**: Incumplimiento simultáneo de las condiciones
   - **Given** que el administrador consultó correctamente la edad de un lote menor de 45 días y el galpón no está en estado `En producción`
   - **When** selecciona la opción de validar aptitud para sacrificio
   - **Then** el sistema muestra `No apto` e informa que el lote no cumple la edad mínima y que el galpón no se encuentra en producción

5. **Scenario**: Validación por un usuario no autorizado
   - **Given** que un usuario sin rol de administrador intenta validar la aptitud de un galpón
   - **When** solicita ejecutar la validación
   - **Then** el sistema rechaza la operación y no muestra el resultado

### Edge Cases

- **Edge case #1 - Cambio de información después de consultar la edad**

  - ¿Cómo maneja el sistema un galpón cuyo lote o estado cambia después de consultar la edad y antes de ejecutar la validación?  
    El sistema debe comprobar nuevamente el lote alojado, su edad y el estado actual del galpón. Si ya no existe un lote alojado actualmente, debe informar que la validación no puede realizarse.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir la validación de aptitud para sacrificio exclusivamente a usuarios con rol de administrador.
- **FR-002**: La opción de validar aptitud para sacrificio DEBE estar disponible únicamente después de consultar correctamente la edad del lote alojado actualmente en un galpón.
- **FR-003**: El sistema DEBE comprobar que la edad calculada del lote sea igual o superior a 45 días.
- **FR-004**: El sistema DEBE comprobar que el galpón se encuentre en estado `En producción`.
- **FR-005**: El sistema DEBE mostrar `Apto` únicamente cuando se cumplan ambas condiciones; en caso contrario, DEBE mostrar `No apto` y todas las razones correspondientes.
- **FR-006**: La validación DEBE ser informativa y NO DEBE modificar ni guardar el estado del galpón, del lote o del proceso de sacrificio.

### Key Entities

- **Galpón**: Representa el espacio cuya condición operativa se evalúa.
  - **Atributos posibles**: nombre, aforo máximo y estado.
  - **Relaciones**: aloja el lote de aves evaluado.
- **Lote de aves**: Representa el grupo de aves cuya edad determina parte de la aptitud para sacrificio.
  - **Atributos posibles**: fecha de ingreso, edad actual calculada, población inicial y población actual.
  - **Relaciones**: se encuentra alojado en el galpón evaluado.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los administradores puede completar la validación en menos de 15 segundos después de consultar la edad.
- **SC-002**: El 95 % de las validaciones muestra el resultado y sus razones en un máximo de 1 segundo.
- **SC-003**: Al menos el 95 % de los administradores interpreta correctamente el resultado en el primer intento durante pruebas de usabilidad.
- **SC-004**: Al menos el 85 % de los administradores califica la claridad del resultado y sus razones con 4 o más puntos sobre 5.
