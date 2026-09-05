# Feature Specification: Consulta de alimento requerido por galpón

**Created**: 2026-09-02  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar el alimento requerido por etapa en el galpón (Priority: P1)

Como nutricionista o administrador (o sistema de gestión de abastecimiento en Módulo 3), quiero consultar la cantidad total de alimento en kilogramos requerido por un galpón para una etapa de alimentación determinada, calculada a partir de la población inicial de aves, la cuota diaria por ave y la duración de la etapa, para planificar y garantizar el suministro nutricional oportuno.

**Why this priority**: Es la funcionalidad esencial del caso de uso. Sin el cálculo exacto de la demanda de alimento por etapa en función de la población y cuota nutricional, no es posible abastecer adecuadamente el galpón ni proyectar los insumos necesarios para la cría.

**Independent Test**: Se puede probar seleccionando un galpón con un lote activo en una etapa de alimentación (por ejemplo, etapa de Pre-inicio de 7 días, con 10.000 aves iniciales y una cuota de 0.035 kg/ave/día) y comprobando que el sistema entregue exactamente 2.450 kg de alimento requerido, el tipo de alimento correspondiente a la etapa y el desglose de parámetros empleados.

**Acceptance Scenarios**:

1. **Scenario**: Cálculo estándar de requerimiento para una etapa predeterminada
   - **Given** que un galpón tiene un lote activo en etapa "Pre-inicio" con duración predeterminada de 7 días (días 1 a 7), una población inicial de 10.000 aves y una cuota establecida por el nutricionista de 0.035 kg/ave/día
   - **When** se consulta el alimento requerido para dicho galpón y etapa
   - **Then** el sistema calcula y entrega 2.450 kg de alimento de tipo pre-inicio, indicando la duración en días (7), la población inicial base (10.000) y la cuota diaria aplicada

2. **Scenario**: Cálculo para etapas con duración modificada por el nutricionista
   - **Given** que el nutricionista modificó los rangos de la etapa "Inicio" para un galpón específico estableciendo una duración de 16 días (en lugar de los 14 días predeterminados)
   - **When** se consulta el requerimiento de alimento para dicha etapa
   - **Then** el sistema calcula la cantidad total requerida multiplicando la población inicial por la cuota de inicio por los 16 días ajustados

3. **Scenario**: Consulta de requerimiento realizada por el Módulo 3 (Integración)
   - **Given** que el servicio de integración del Módulo 3 solicita el alimento requerido para un galpón con lote activo
   - **When** envía el identificador del galpón
   - **Then** el sistema retorna los kilogramos totales de alimento requerido, el tipo de alimento según la etapa y las fechas estimadas de inicio y fin de la etapa

4. **Scenario**: Intento de consulta en un galpón sin lote activo o sin cuota configurada
   - **Given** que se consulta el requerimiento para un galpón vacío o cuya etapa no tiene una cuota diaria configurada
   - **When** se procesa la solicitud de consulta
   - **Then** el sistema rechaza la operación e informa de manera clara la causa (galpón inactivo o falta de cuota nutricional), sin entregar valores calculados erróneos

---

### User Story 2 - Adición de requerimiento por extensión de etapa o cuarentena (Priority: P2)

Como nutricionista o administrador, quiero que el sistema adicione automáticamente los días y kilogramos de alimento requeridos cuando un galpón presente un aislamiento sanitario o cuarentena que prolongue la permanencia en una etapa de alimentación, para evitar el desabastecimiento nutricional durante la contingencia.

**Why this priority**: Los eventos sanitarios imponen restricciones de movimiento y aislamiento que extienden los días de permanencia de las aves en una etapa antes de permitir la transición a la siguiente o al sacrificio. El requerimiento de alimento debe adaptarse dinámicamente a dicha contingencia.

**Independent Test**: Se puede probar configurando una orden de aislamiento sanitario de 4 días adicionales para un galpón en etapa de inicio, verificando que la consulta de requerimiento sume los 4 días a la duración base de la etapa y calcule los kilogramos adicionales según la cuota y población inicial.

**Acceptance Scenarios**:

1. **Scenario**: Cálculo de requerimiento con días adicionales por cuarentena activa
   - **Given** que un galpón en etapa "Inicio" tiene una duración base de 14 días y un registro de aislamiento sanitario activo que añade 4 días a la etapa actual
   - **When** se consulta el alimento requerido para dicho galpón
   - **Then** el sistema calcula el requerimiento total sobre 18 días (14 base + 4 cuarentena), presentando el desglose de los kilogramos base y los kilogramos adicionales por cuarentena

2. **Scenario**: Actualización de requerimiento tras levantamiento anticipado de aislamiento
   - **Given** que el veterinario certifica el galpón y levanta el aislamiento antes de los días inicialmente proyectados
   - **When** se actualiza la condición sanitaria y se consulta nuevamente el requerimiento
   - **Then** el sistema recalcula los kilogramos requeridos considerando únicamente los días efectivamente aplicados de aislamiento

3. **Scenario**: Registro de aislamiento con cantidad de días inválida
   - **Given** un registro de aislamiento o ajuste con días adicionales menores o iguales a cero
   - **When** se procesa el cálculo de requerimiento
   - **Then** el sistema no incrementa la duración de la etapa y genera una advertencia sobre la inconsistencia del registro sanitario

---

### User Story 3 - Trazabilidad histórica y preservación del gasto del lote (Priority: P3)

Como administrador, quiero consultar el historial consolidado de todo el alimento que se requirió durante la vida del lote en el galpón, garantizando que el gasto imputado al lote se mantenga íntegro aun si al final de una etapa se envía alimento sobrante a la bodega central por disminución de población (mortalidad).

**Why this priority**: Asegura la consistencia contable y de costos del lote. El presupuesto y gasto asignado al lote se basa en la proyección inicial; el sobrante físico derivado de la merma de aves debe reintegrarse físicamente sin alterar retroactivamente el registro histórico del gasto incurrido.

**Independent Test**: Se puede probar finalizando una etapa de alimentación en un lote donde hubo mortalidad registrada; se verifica que el histórico del lote conserve el 100% de los kilogramos y costos proyectados originalmente, registrando la devolución física como evento de inventario sin descontar el valor del historial de gastos del lote.

**Acceptance Scenarios**:

1. **Scenario**: Consulta del historial acumulado de requerimientos del lote
   - **Given** que un lote de pollos ha transitado por una o más etapas de alimentación
   - **When** el administrador consulta el consolidado histórico de requerimientos del lote
   - **Then** el sistema detalla los kilogramos requeridos por cada etapa, las cuotas diarias aplicadas, los días de duración (incluyendo extensiones por cuarentena) y el costo total imputado al lote

2. **Scenario**: Preservación del gasto histórico frente al retorno de sobrante a bodega central
   - **Given** que al final de una etapa se identifica un sobrante de alimento físico en la sub-bodega del galpón debido a la mortalidad de aves, y este sobrante se remite a la bodega central
   - **When** se consulta el balance y el historial de gastos del lote
   - **Then** el sistema conserva íntegro el gasto total calculado originalmente sobre la población inicial, sin descontar el valor monetario ni las cantidades históricas imputadas al lote

---

### Edge Cases

- **Transición de etapa bloqueada por aislamiento sanitario**:
  Si las aves alcanzan la edad límite de una etapa pero el galpón continúa en aislamiento sanitario o cuarentena obligatoria, el sistema debe mantener el suministro de la etapa y tipo de alimento actual durante los días de cuarentena, impidiendo el cambio de etapa hasta que exista certificación veterinaria de reintegro.

- **Cuota nutricional no configurada o configurada en cero**:
  Si para una etapa no se ha configurado la cuota de kg/ave/día o se introduce un valor menor o igual a cero, el sistema debe bloquear el cálculo, rechazar la confirmación de la cuota y notificar al usuario que la cuota debe ser un número positivo mayor que cero.

- **Galpón con población inicial inconsistente o nula**:
  Si el lote asociado al galpón registra una población inicial de cero aves o presenta inconsistencias en los datos del lote, el sistema debe rechazar la consulta indicando que no hay población base válida para el cálculo.

- **Manejo de redondeo y precisión numérica en kilogramos**:
  Al multiplicar cuotas unitarias pequeñas (ej. 0.0355 kg/ave/día) por poblaciones elevadas, el sistema debe mantener una precisión de cálculo de al menos dos decimales y aplicar redondeo estándar uniforme (*half-up*) para evitar discrepancias entre los totales entregados a la bodega central y al galpón.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE calcular el requerimiento de alimento en kilogramos para un galpón específico en función de la etapa de alimentación correspondiente.
- **FR-002**: Las etapas de alimentación predeterminadas DEBEN ser:
  - **Pre-inicio**: desde el día 0 hasta el día 7 de edad.
  - **Inicio**: desde el día 8 hasta el día 21 de edad.
  - **Broiler (Finalización)**: desde el día 22 de edad hasta el sacrificio.
- **FR-003**: El sistema DEBE permitir que un usuario con rol de Nutricionista modifique los rangos de días de duración de cualquiera de las etapas de alimentación para un galpón o lote específico.
- **FR-004**: El sistema DEBE permitir que el Nutricionista defina una cuota diaria de alimento por ave en kilogramos (`kg/ave/día`) para cada etapa de alimentación.
- **FR-005**: El sistema DEBE calcular la cantidad total en kilogramos requerida para una etapa mediante la fórmula:  
  `Kilogramos Requeridos = Población Inicial del Lote * Cuota Diaria (kg/ave/día) * Días de la Etapa`.
- **FR-006**: Si el galpón presenta un aislamiento sanitario o cuarentena activo que incremente los días de alimentación en la etapa, el sistema DEBE adicionar dichos días a la duración de la etapa para el cálculo del alimento requerido.
- **FR-007**: La respuesta a la consulta de alimento requerido DEBE incluir: identificador del galpón, código del lote, etapa de alimentación, tipo de alimento, días base de la etapa, días adicionales por cuarentena, cuota diaria aplicada y kilogramos totales requeridos.
- **FR-008**: El sistema DEBE permitir la consulta del requerimiento de alimento tanto a usuarios internos autorizados (Nutricionista y Administrador) como al sistema externo Módulo 3 mediante integración de consulta.
- **FR-009**: El sistema DEBE registrar y conservar el histórico de todos los requerimientos de alimento calculados para el lote a lo largo de su ciclo de vida en el galpón.
- **FR-010**: En caso de que se identifique un sobrante de alimento físico al final de una etapa debido a la disminución de población (mortalidad) y dicho sobrante se envíe de vuelta a la bodega central, el sistema NO DEBE descontar dicho sobrante del historial de gastos ni del total de insumos calculados e imputados al lote.
- **FR-011**: El sistema DEBE validar que la población inicial, la cuota diaria por ave y la cantidad de días de la etapa sean valores numéricos estrictamente mayores que cero.
- **FR-012**: El sistema DEBE rechazar la consulta de requerimiento con un mensaje explicativo si el galpón no tiene un lote activo asignado o si la etapa no tiene configurada su cuota nutricional.

### Key Entities

- **Galpón**: Espacio físico productivo que alberga un lote de aves y que opera operativamente con un stock o requerimiento asignado desde la bodega central.
- **Lote de Pollos**: Agrupación de aves alojadas en el galpón, caracterizado por su población inicial, edad en días, estado sanitario y registro de trazabilidad.
- **Etapa de Alimentación**: Fase nutricional del ciclo productivo (Pre-inicio, Inicio, Broiler) asociada a un tipo de alimento específico y a un rango de días base o modificado.
- **Cuota Nutricional**: Cantidad fija de alimento en kilogramos por ave al día (`kg/ave/día`) determinada por el nutricionista para una etapa específica.
- **Requerimiento de Alimento por Galpón**: Registro del cálculo en kilogramos de alimento proyectado para cubrir las necesidades del galpón durante una etapa (incluyendo extensiones sanitarias).
- **Aislamiento Sanitario / Cuarentena**: Medida veterinaria que suspende la salida o transición del galpón y puede extender la duración de la etapa de alimentación en un número determinado de días.
- **Historial de Gasto del Lote**: Registro acumulado e inmutable de los requerimientos y costos de alimento asignados al lote durante toda su permanencia en el galpón.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las consultas de requerimiento de alimento entrega el total exacto en kilogramos aplicando estrictamente la fórmula basada en población inicial, cuota diaria y días totales de la etapa.
- **SC-002**: El 100 % de los cambios en los rangos de días realizados por el nutricionista o adiciones por cuarentena sanitaria se reflejan inmediatamente en el cálculo de kilogramos requeridos.
- **SC-003**: El sistema entrega los resultados de la consulta de requerimiento en menos de 1 segundo para consultas tanto de usuarios internos como del Módulo 3.
- **SC-004**: En el 100 % de los lotes evaluados, el historial de gastos acumulados por concepto de alimento permanece inalterable y no sufre reducciones cuando se retornan sobrantes físicos a la bodega central por efectos de mortalidad.
- **SC-005**: El 100 % de los intentos de calcular requerimientos con cuotas o poblaciones menores o iguales a cero, o sobre galpones sin lote activo, es rechazado con mensajes claros de validación.

---

## Out of Scope *(Fuera de alcance de esta especificación)*

- Los movimientos físicos de inventario en la sub-bodega del galpón (recepción de despachos, control de stock físico diario y registros de consumo por parte del trabajador).
- El proceso operativo de transporte, pesaje físico y recepción de devoluciones dentro de la bodega central (cubierto en la gestión de movimientos de bodega).
- La formulación bromatológica, composición química o mezcla de materias primas para la elaboración del alimento.
- La gestión y registro veterinario del diagnóstico de enfermedades o emisión de órdenes de cuarentena (cubiertos en las especificaciones de sanidad y aislamiento).
