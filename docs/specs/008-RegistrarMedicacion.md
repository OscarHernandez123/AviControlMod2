# Feature Specification: Registrar Medicación

**Created**: 2026-09-04

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Alta de Tratamiento Médico para Diagnóstico (Priority: P1)

Como Veterinario de la granja, quiero registrar una pauta de medicación vinculando una enfermedad activa, un medicamento disponible en inventario, la dosis recomendada, los días totales de duración y una descripción clara de aplicación, para que quede lista y pueda ser seleccionada cuando se diagnostique un galpón o se requiera medicar un lote.

**Why this priority**: Es el catálogo de tratamientos de la granja. Sin este registro previo, el veterinario no puede elegir un tratamiento oficial al diagnosticar galpones enfermos ni estandarizar cómo y durante cuántos días se debe aplicar un medicamento.

**Independent Test**: Se prueba ingresando una medicación con una enfermedad activa del catálogo y un medicamento con existencia en inventario, colocando dosis, días enteros mayores a cero y la descripción. El sistema debe guardar el tratamiento, dejarlo disponible para futuros diagnósticos y no debe descontar unidades del inventario ni modificar ningún galpón.

**Acceptance Scenarios**:

1. **Scenario**: Registro exitoso de una medicación
   - **Given** que la enfermedad "Bronquitis Infecciosa Aviar" está activa en el catálogo
   - **And** el medicamento "Tilosina 50%" existe en el inventario
   - **When** el Veterinario ingresa dosis "0.5 g por litro de agua", duración de 5 días y la descripción "Suministrar en agua de bebida por las mañanas"
   - **Then** el sistema guarda la medicación
   - **And** la deja disponible para ser seleccionada en futuros diagnósticos de galpón
   - **And** mantiene intactas las existencias del medicamento en inventario

2. **Scenario**: Intento de registro con campos obligatorios vacíos o incompletos
   - **Given** que el Veterinario abre la pantalla de registrar medicación
   - **When** intenta guardar sin indicar la dosis o dejando la descripción vacía (o solo con espacios en blanco)
   - **Then** el sistema detiene el guardado, resalta los campos faltantes y no crea la medicación

3. **Scenario**: Duración en días no válida
   - **Given** que el Veterinario ingresa los datos de una medicación
   - **When** escribe 0 días, un número negativo o un decimal (ejemplo: 3.5 días)
   - **Then** el sistema rechaza el valor indicando que la duración debe ser un número entero de días mayor a cero
   - **And** no crea el registro

4. **Scenario**: Enfermedad no vigente en el catálogo
   - **Given** una enfermedad registrada en estado "INACTIVA" o inexistente
   - **When** el Veterinario intenta seleccionarla para crear la medicación
   - **Then** el sistema no permite la selección e informa que la enfermedad no está disponible

5. **Scenario**: Medicamento no disponible en inventario
   - **Given** un medicamento agotado, dado de baja o que no existe en el catálogo de insumos
   - **When** el Veterinario intenta asignarlo a la medicación
   - **Then** el sistema bloquea el registro informando que el medicamento no se encuentra disponible

6. **Scenario**: Intento de registro por personal no facultado
   - **Given** un usuario autenticado con rol distinto a "VETERINARIO" (ejemplo: "TRABAJADOR")
   - **When** intenta crear una medicación
   - **Then** el sistema deniega el acceso por falta de permisos médicos y no guarda nada

---

### User Story 2 - Actualización de una Medicación sin Afectar Históricos (Priority: P2)

Como Veterinario de la granja, quiero editar una medicación existente para ajustar la dosis, los días de duración o las instrucciones de uso, garantizando que estos cambios apliquen únicamente a los diagnósticos nuevos y no alteren los tratamientos ni las fechas de retiro que ya se aplicaron en el pasado.

**Why this priority**: La medicina avícola exige ajustar dosis y tiempos según la respuesta clínica, pero nunca se deben sobreescribir los tratamientos viejos porque se adulteraría el historial de inocuidad y retiro farmacológico de lotes anteriores.

**Independent Test**: Se toma una medicación configurada en 5 días que ya fue aplicada a un galpón en una fecha previa y se actualiza a 7 días. El sistema debe comprobar que el galpón histórico conserve sus 5 días y su fecha de retiro original, mientras que un galpón nuevo diagnosticado hoy reciba los 7 días actualizados.

**Acceptance Scenarios**:

1. **Scenario**: Edición exitosa de pauta de medicación
   - **Given** una medicación registrada con duración de 5 días
   - **When** el Veterinario actualiza la duración a 7 días y complementa las instrucciones de dosificación
   - **Then** el sistema guarda la nueva versión del tratamiento
   - **And** queda disponible con 7 días para diagnósticos futuros

2. **Scenario**: Protección inmutable de tratamientos ya aplicados
   - **Given** un galpón que recibió tratamiento con la medicación cuando duraba 5 días
   - **When** el Veterinario modifica esa medicación en el sistema para que dure 7 días
   - **Then** el galpón previamente diagnosticado mantiene inalterados sus 5 días y su fecha de retiro calculada
   - **And** el cambio rige exclusivamente para los diagnósticos que se registren a partir de este momento

3. **Scenario**: Intento de modificación no autorizado
   - **Given** un usuario sin rol de Veterinario
   - **When** intenta modificar los parámetros de una medicación
   - **Then** el sistema bloquea la acción y conserva la medicación sin cambios

---

### User Story 3 - Inmutabilidad y Blindaje contra Borrado (Priority: P3)

Como auditor de bioseguridad y responsable de sanidad de la granja, quiero que las medicaciones registradas no puedan ser eliminadas físicamente de la base de datos y que se eviten registros duplicados por fallas de conexión, para respaldar legalmente las decisiones farmacológicas de la empresa.

**Why this priority**: Si se borrara físicamente un registro de medicación, las órdenes médicas de campo quedarían sin sustento técnico y la granja no podría justificar ante inspectores oficiales por qué se aplicó ese producto.

**Independent Test**: Se intenta ejecutar una orden de borrado directo (`DELETE`) en base de datos sobre una medicación y se simulan reenvíos de red duplicados con la misma clave de operación.

**Acceptance Scenarios**:

1. **Scenario**: Prohibición de eliminación física
   - **Given** una medicación guardada en el sistema
   - **When** se intenta borrar físicamente el registro
   - **Then** el sistema bloquea y prohíbe la eliminación
   - **And** mantiene la información intacta en la base de datos

2. **Scenario**: Evitar registros dobles por problemas de red (Idempotencia)
   - **Given** una medicación enviada y guardada correctamente
   - **When** el dispositivo o navegador reenvía la misma petición de guardado por un corte momentáneo de red
   - **Then** el sistema detecta que ya fue creada y no genera un registro duplicado

---

### Edge Cases

- **Medicamento que se agota o desactiva durante la selección**: Si un medicamento es dado de baja del inventario mientras el veterinario redacta la medicación, el sistema valida el estado justo antes de guardar y rechaza el registro indicando que el producto ya no está habilitado.
- **Campos de texto con solo espacios**: Si la dosis o la descripción contienen únicamente espacios en blanco, el sistema los considera campos vacíos y exige escribir texto válido.
- **Acción aislada sin consumo ni movimiento de galpón**: Registrar una medicación no descuenta botellas ni kilos del almacén, no requiere que haya un galpón enfermo en ese instante ni cambia el estado de ningún lote. El consumo real se procesa cuando las aves reciben el medicamento en campo.

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: El registro y edición de medicaciones DEBE ser una función exclusiva de usuarios con rol `VETERINARIO`.
- **FR-002**: Cada registro de medicación DEBE incluir obligatoriamente: enfermedad, medicamento, dosis, número total de días y descripción de aplicación.
- **FR-003**: La enfermedad seleccionada DEBE existir en el catálogo nosológico y encontrarse en estado `ACTIVA` (CU-VET-002).
- **FR-004**: El medicamento seleccionado DEBE provenir del inventario de insumos y estar habilitado para su uso.
- **FR-005**: La duración del tratamiento DEBE ser un número entero estrictamente mayor a cero ($\text{dias} > 0$).
- **FR-006**: El registro de una medicación NO DEBE requerir un diagnóstico previo, no debe alterar el estado de ningún galpón y NO DEBE descontar existencias del inventario de medicamentos.
- **FR-007**: Toda edición de una medicación DEBE aplicar únicamente hacia diagnósticos futuros, manteniendo inmutables los tratamientos, dosis y fechas de retiro fijadas en galpones ya diagnosticados.
- **FR-008**: Al guardar una medicación válida, el sistema DEBE publicarla como disponible para el flujo de diagnóstico de galpones y emitir el evento `MedicacionRegistrada`.
- **FR-009**: Cada creación o edición DEBE guardar un asiento histórico e inmutable en `san_auditoria` con usuario, fecha, hora y detalle de la acción.
- **FR-010**: Queda ESTRICTAMENTE PROHIBIDO el borrado físico (`DELETE` en base de datos) de cualquier medicación registrada.
- **FR-011**: El sistema DEBE aplicar control de concurrencia optimista (`version`) y mecanismos de idempotencia técnica para evitar registros duplicados ante reintentos de red.

### Key Entities

- **Medicación**: Aggregate Root que define la pauta médica estándar de tratamiento. Atributos: `id` (UUID), `enfermedadId` (UUID), `medicamentoId` (UUID), `dosis` (Texto descriptivo con unidad), `diasTratamiento` (Entero positivo), `descripcion` (Texto explicativo), `activa` (Booleano), `version` (Control concurrente) y marcas de tiempo de creación y actualización.
- **Enfermedad**: Entidad del catálogo nosológico de la granja que representa la patología a combatir con este tratamiento.
- **Medicamento**: Insumo provisto por el inventario que aporta el compuesto terapéutico a administrar.
- **Auditoría Sanitaria (`san_auditoria`)**: Registro inmutable donde queda asentada la firma y responsabilidad del veterinario sobre el tratamiento creado.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: El 100% de las medicaciones guardadas quedan disponibles de inmediato para ser seleccionadas en el flujo de diagnóstico de galpón.
- **SC-002**: 0% de impacto o descuento en las cantidades del inventario de medicamentos durante la creación o edición de medicaciones.
- **SC-003**: 0% de alteraciones en los tratamientos pasados y fechas de retiro de galpones previamente diagnosticados tras editar una medicación.
- **SC-004**: El tiempo de guardado y validación de una medicación es menor a 300 milisegundos en condiciones normales.
- **SC-005**: El 100% de los intentos de registro por parte de usuarios con roles diferentes a `VETERINARIO` son bloqueados por el sistema.
- **SC-006**: Cero incidentes (0%) de registros de medicación borrados físicamente de la base de datos durante toda la vida útil del sistema.
