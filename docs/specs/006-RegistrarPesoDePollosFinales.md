# Feature Specification: Registrar resultados finales del sacrificio

**Created**: 2026-09-02  

## User Scenarios & Testing

### User Story 1 - Registrar el peso y la cantidad final de pollos sacrificados (Priority: P1)

Como administrador, quiero registrar el peso total y la cantidad final de los pollos sacrificados de un lote para que estos datos queden disponibles para la liquidación y el análisis de rentabilidad del módulo 3.

**Why this priority**: El peso total y la cantidad final permiten al módulo 3 calcular los ingresos del lote y completar sus indicadores productivos y financieros.

**Independent Test**: Se puede probar utilizando una orden de sacrificio ejecutada, registrando una cantidad final entera y un peso total positivo con máximo dos decimales, y verificando que exista un único resultado asociado al lote y disponible para el módulo 3.

**Acceptance Scenarios**:

1. **Scenario**: Registro correcto de los resultados finales
   - **Given** que existe una orden de sacrificio ejecutada y el lote no tiene resultados finales registrados
   - **When** el administrador registra la cantidad final de pollos sacrificados y su peso total en kilogramos
   - **Then** el sistema crea un único resultado asociado al lote, al galpón y a la orden, y deja ambos valores disponibles para el módulo 3

2. **Scenario**: Intento de registro antes de ejecutar la orden
   - **Given** que la orden de sacrificio todavía está programada o fue cancelada
   - **When** el administrador intenta registrar los resultados finales
   - **Then** el sistema rechaza la operación e informa que la orden debe estar ejecutada

3. **Scenario**: Intento de registro con valores inválidos
   - **Given** que existe una orden de sacrificio ejecutada
   - **When** el administrador registra una cantidad que no es un entero positivo o un peso que no es positivo o tiene más de dos decimales
   - **Then** el sistema rechaza el registro, identifica los valores que deben corregirse y no comparte información con el módulo 3

4. **Scenario**: Intento de crear un segundo resultado para el mismo lote
   - **Given** que el lote ya tiene sus resultados finales registrados
   - **When** el administrador intenta crear otro registro
   - **Then** el sistema rechaza la operación e indica que debe corregir el registro existente si todavía está permitido

5. **Scenario**: Registro por un usuario no autorizado
   - **Given** que un usuario sin rol de administrador intenta registrar resultados finales
   - **When** solicita confirmar la operación
   - **Then** el sistema la rechaza y no crea ni comparte información

### User Story 2 - Corregir los resultados antes de su utilización (Priority: P2)

Como administrador, quiero corregir la cantidad o el peso final mientras el módulo 3 no los haya utilizado para solucionar errores de digitación sin afectar una liquidación existente.

**Why this priority**: Permite corregir datos equivocados y, al mismo tiempo, protege la consistencia de los cálculos realizados por el módulo 3.

**Independent Test**: Se puede probar corrigiendo un resultado disponible y solicitando después la modificación de otro que ya fue utilizado por el módulo 3, para verificar que únicamente el primero pueda cambiarse.

**Acceptance Scenarios**:

1. **Scenario**: Corrección antes de utilizar los datos
   - **Given** que los resultados finales están disponibles pero todavía no han sido utilizados por el módulo 3
   - **When** el administrador corrige la cantidad o el peso total con valores válidos
   - **Then** el sistema actualiza el mismo registro y deja los nuevos valores disponibles para el módulo 3

2. **Scenario**: Intento de corrección después de utilizar los datos
   - **Given** que el módulo 3 ya utilizó los resultados finales en una liquidación
   - **When** el administrador intenta corregir la cantidad o el peso total
   - **Then** el sistema bloquea la modificación e informa que los datos ya fueron utilizados por el módulo 3

### Edge Cases

- **Edge case #1 - Dos registros simultáneos para el mismo lote**

  - ¿Cómo maneja el sistema dos solicitudes que intentan registrar los resultados finales del mismo lote al mismo tiempo?  
    El sistema debe aceptar una sola solicitud y rechazar la otra, garantizando que exista un único resultado por lote.

- **Edge case #2 - Corrección y utilización simultáneas**

  - ¿Cómo maneja el sistema una corrección enviada al mismo tiempo que el módulo 3 utiliza los resultados?  
    El sistema debe completar una sola operación primero. Si el módulo 3 utiliza los datos antes de aplicar la corrección, debe bloquearla; si la corrección se completa primero, el módulo 3 debe recibir los valores actualizados.

- **Edge case #3 - Módulo 3 temporalmente no disponible**

  - ¿Cómo maneja el sistema un registro confirmado mientras el módulo 3 no está disponible?  
    El sistema debe conservar el resultado como disponible y permitir que el módulo 3 lo consulte posteriormente, sin duplicarlo ni perder sus valores.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir registrar y corregir los resultados finales del sacrificio exclusivamente a usuarios con rol de administrador.
- **FR-002**: El registro DEBE estar disponible únicamente para una orden de sacrificio ejecutada y DEBE existir una sola vez por lote.
- **FR-003**: El sistema DEBE registrar la cantidad final de pollos sacrificados como un número entero mayor que cero y el peso total como un valor en kilogramos mayor que cero con máximo dos decimales.
- **FR-004**: El resultado DEBE quedar asociado al lote de aves, al galpón y a la orden de sacrificio correspondientes.
- **FR-005**: Al confirmar el registro, el sistema DEBE dejar la cantidad final y el peso total disponibles para el módulo 3 como una única unidad de información.
- **FR-006**: El sistema DEBE permitir corregir el registro sin exigir justificación mientras el módulo 3 no haya utilizado sus datos.
- **FR-007**: Cuando el módulo 3 utilice los resultados en una liquidación, el sistema DEBE impedir cualquier corrección posterior.

### Key Entities

- **Resultado de sacrificio**: Representa los valores finales obtenidos después del sacrificio de un lote.
  - **Atributos posibles**: cantidad final de pollos, peso total en kilogramos, fecha de registro, fecha de corrección y estado de utilización.
  - **Relaciones**: pertenece a un lote de aves, a una orden de sacrificio y al galpón correspondiente; proporciona datos a la liquidación del módulo 3.
- **Orden de sacrificio**: Representa la orden cuya ejecución habilita el registro de resultados.
  - **Atributos posibles**: fecha y hora programadas, fecha y hora de ejecución y estado.
  - **Relaciones**: corresponde al lote y origina un único resultado de sacrificio.
- **Lote de aves**: Representa el grupo de pollos sacrificados.
  - **Atributos posibles**: fecha de ingreso, población inicial, población final y estado.
  - **Relaciones**: tiene una orden y un único resultado de sacrificio, cuyos datos utiliza el módulo 3.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los administradores puede registrar los resultados finales en menos de 30 segundos.
- **SC-002**: El 95 % de los registros confirmados queda disponible para el módulo 3 en un máximo de 2 segundos.
- **SC-003**: Al menos el 99 % de los resultados confirmados está disponible para el módulo 3 sin pérdidas ni duplicados.
- **SC-004**: Al menos el 90 % de los administradores puede identificar correctamente si un resultado todavía puede corregirse en el primer intento durante pruebas de usabilidad.
