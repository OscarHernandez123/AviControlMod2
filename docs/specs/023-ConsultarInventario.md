# Feature Specification: Consultar inventario

**Created**: 2026-09-22

## User Scenarios & Testing

### User Story 1 - Consultar existencias de alimentos y medicamentos (Priority: P1)

Como administrador, quiero consultar en un mismo lugar las existencias disponibles de alimentos y medicamentos de la bodega central para conocer el inventario vigente y apoyar las decisiones de abastecimiento sin modificar sus movimientos.

**Why this priority**: La consulta consolidada permite al administrador conocer la disponibilidad real de los insumos registrados mediante recepciones y afectados posteriormente por salidas, consumos o ajustes. Esta responsabilidad debe permanecer separada del registro y la edición de recepciones.

**Independent Test**: Se puede probar registrando recepciones y movimientos confirmados de alimentos y medicamentos, ingresando como administrador a la consulta y verificando que cada sección muestre los saldos disponibles consolidados en su unidad correspondiente, sin crear ni modificar datos.

**Acceptance Scenarios**:

1. **Scenario**: Consulta conjunta con existencias disponibles
   - **Given** que existen recepciones y movimientos confirmados de alimentos y medicamentos en la bodega central
   - **When** el administrador consulta el inventario
   - **Then** el sistema muestra una sección de alimentos y otra de medicamentos con las existencias disponibles calculadas a partir de sus movimientos confirmados

2. **Scenario**: Consulta de existencias de alimentos
   - **Given** que existen alimentos con saldo disponible provenientes de una o varias recepciones
   - **When** el administrador consulta la sección de alimentos
   - **Then** el sistema agrupa las existencias por alimento, tipo de alimento y peso nominal por bulto, y muestra la cantidad disponible en kilogramos y su equivalente en bultos

3. **Scenario**: Consulta de existencias de medicamentos
   - **Given** que existen medicamentos con saldo disponible provenientes de una o varias recepciones
   - **When** el administrador consulta la sección de medicamentos
   - **Then** el sistema agrupa las existencias por medicamento, presentación y unidad base, y muestra la cantidad física equivalente y el contenido neto total disponible en `gr`, `ml` o `unidad`

4. **Scenario**: Producto registrado sin existencias disponibles
   - **Given** que un alimento o medicamento del catálogo no tiene recepciones confirmadas con saldo disponible
   - **When** el administrador consulta el inventario
   - **Then** el sistema muestra el producto con existencia disponible igual a cero, sin confundirlo con un error de consulta

5. **Scenario**: Exclusión de recepciones vencidas o anuladas
   - **Given** que existen recepciones vencidas o anuladas con saldo registrado
   - **When** el administrador consulta el inventario
   - **Then** el sistema excluye esas cantidades de la existencia disponible y las identifica como no disponibles

6. **Scenario**: Intento de consulta por un usuario no autorizado
   - **Given** que un usuario sin rol de administrador intenta consultar el inventario
   - **When** solicita acceder a la consulta
   - **Then** el sistema rechaza el acceso y no expone información de existencias

7. **Scenario**: Consulta sin modificar el inventario
   - **Given** que el administrador visualiza las existencias de alimentos y medicamentos
   - **When** navega entre las secciones o actualiza la consulta
   - **Then** el sistema vuelve a calcular la información vigente sin crear recepciones, movimientos ni ajustes, y sin modificar los saldos existentes

### Edge Cases

- **Edge case #1 - Movimientos concurrentes durante la consulta**

  - ¿Qué sucede si se confirma una entrada, salida, consumo o ajuste mientras el administrador tiene abierta la consulta?
    El sistema debe obtener una vista consistente de los movimientos confirmados al momento de ejecutar o actualizar la consulta. No debe combinar saldos calculados en momentos diferentes dentro de una misma respuesta.

- **Edge case #2 - Alimentos con diferentes pesos nominales por bulto**

  - ¿Cómo presenta el sistema varias recepciones del mismo alimento con pesos nominales por bulto diferentes?
    El sistema debe agruparlas por alimento, tipo de alimento y peso nominal por bulto. No debe sumar como una sola cantidad de bultos presentaciones con pesos nominales diferentes; el total general de alimento sí puede consolidarse en kilogramos.

- **Edge case #3 - Medicamentos con unidades base incompatibles**

  - ¿Cómo presenta el sistema existencias de un medicamento registradas en presentaciones con unidades base diferentes?
    El sistema debe mantener grupos separados por medicamento, presentación y unidad base. No debe sumar entre sí cantidades expresadas en `gr`, `ml` y `unidad`.

- **Edge case #4 - Saldo calculado negativo**

  - ¿Qué sucede si la consolidación de movimientos produce un saldo negativo para una recepción o producto?
    El sistema debe informar una inconsistencia de inventario, no presentar el valor negativo como existencia disponible y conservar el modo de solo lectura. La consulta no debe crear ajustes automáticos.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir la consulta del inventario exclusivamente a usuarios autenticados con rol de administrador.
- **FR-002**: El sistema DEBE presentar en una misma funcionalidad dos secciones diferenciadas: existencias de alimentos y existencias de medicamentos.
- **FR-003**: La existencia disponible DEBE calcularse exclusivamente a partir de movimientos confirmados de entrada, salida, consumo y ajuste vinculados con las recepciones de la bodega central.
- **FR-004**: Para cada grupo de alimentos, el sistema DEBE mostrar el alimento, el tipo de alimento, el peso nominal por bulto, la existencia disponible en kilogramos y la cantidad equivalente en bultos.
- **FR-005**: El sistema DEBE agrupar los alimentos por alimento, tipo de alimento y peso nominal por bulto, sin mezclar en una misma cantidad de bultos recepciones con pesos nominales diferentes.
- **FR-006**: El sistema DEBE mostrar el total general de alimento disponible en kilogramos y los subtotales por tipo de alimento.
- **FR-007**: Para cada grupo de medicamentos, el sistema DEBE mostrar el medicamento, la presentación, la unidad base, la cantidad física equivalente y el contenido neto total disponible.
- **FR-008**: El sistema DEBE agrupar los medicamentos por medicamento, presentación y unidad base, sin sumar cantidades expresadas en unidades base incompatibles.
- **FR-009**: El sistema DEBE mostrar los productos activos del catálogo sin saldo disponible con existencia igual a cero.
- **FR-010**: El sistema DEBE excluir de la existencia disponible los saldos de recepciones vencidas o anuladas e identificarlos como no disponibles.
- **FR-011**: Cada ejecución o actualización de la consulta DEBE utilizar una vista consistente y vigente de los movimientos confirmados.
- **FR-012**: Si la consolidación produce un saldo negativo, el sistema DEBE informar una inconsistencia y NO DEBE presentar dicho valor como existencia disponible ni generar ajustes automáticos.
- **FR-013**: La consulta DEBE operar estrictamente en modo de solo lectura y NO DEBE crear, editar o eliminar recepciones, movimientos, consumos, despachos ni ajustes.
- **FR-014**: El sistema DEBE rechazar la consulta de inventario realizada por cualquier usuario que no tenga el rol de administrador.

### Key Entities

- **Bodega central**: Representa el inventario principal cuyas existencias consulta el administrador.
  - **Relaciones**: recibe las recepciones de alimentos y medicamentos y reúne los movimientos que determinan sus saldos disponibles.
- **Existencia de alimento**: Representa el resultado de consolidar los movimientos confirmados de alimento.
  - **Datos mostrados**: alimento, tipo de alimento, peso nominal por bulto, kilogramos disponibles y bultos equivalentes.
  - **Origen**: se calcula a partir de las recepciones de alimento definidas en el SPEC-001 y sus movimientos asociados.
- **Existencia de medicamento**: Representa el resultado de consolidar los movimientos confirmados de medicamento.
  - **Datos mostrados**: medicamento, presentación, unidad base, cantidad física equivalente y contenido neto total disponible.
  - **Origen**: se calcula a partir de las recepciones de medicamento definidas en el SPEC-002 y sus movimientos asociados.
- **Recepción de alimento**: Representa una entrega de alimento que origina un movimiento de entrada y conserva sus datos históricos.
- **Recepción de medicamento**: Representa una entrega de medicamento que origina un movimiento de entrada y conserva sus datos históricos.
- **Movimiento de inventario**: Representa una entrada, salida, consumo o ajuste confirmado que incrementa o disminuye el saldo de una recepción.
  - **Atributos utilizados**: tipo de movimiento, cantidad normalizada, unidad, estado, fecha y hora.
  - **Relaciones**: referencia una recepción de alimento o de medicamento y permite calcular la existencia sin modificar su historial.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los administradores puede identificar las existencias disponibles de un alimento y un medicamento en menos de 2 minutos.
- **SC-002**: El 100 % de las existencias mostradas coincide con la consolidación de los movimientos confirmados y excluye recepciones vencidas o anuladas.
- **SC-003**: El 100 % de las consultas separa correctamente alimentos con pesos nominales diferentes y medicamentos con unidades base incompatibles.
- **SC-004**: El 95 % de las consultas presenta ambas secciones en un máximo de 2 segundos.
- **SC-005**: El 100 % de las consultas se ejecuta sin modificar recepciones, movimientos ni saldos de inventario.
- **SC-006**: El 100 % de los intentos de acceso realizados por usuarios sin rol de administrador es rechazado.

## Out of Scope

- El registro y la edición de recepciones de alimento, cubiertos por el SPEC-001 *Registrar recepción de alimento*.
- El registro y la edición de recepciones de medicamento, cubiertos por el SPEC-002 *Registro de recepción de medicamento*.
- El registro de salidas, despachos, consumos o ajustes de inventario.
- La creación o administración de los catálogos de alimentos, tipos de alimento, medicamentos y presentaciones.
- La consulta operativa de alimento o medicación requerida por un galpón para una fecha específica.
