# Feature Specification: Registrar consumo de medicamento

**Created**: 2026-09-05  

## User Scenarios & Testing

### User Story 1 - Registrar el consumo real de un medicamento (Priority: P1)

Como veterinario, quiero registrar la cantidad de medicamento realmente aplicada a un lote a partir de su diagnóstico, para descontar las existencias utilizadas y dejar disponibles para el módulo 3 los datos necesarios para calcular el costo de los medicamentos aplicados al lote.

**Why this priority**: El consumo permite diferenciar el tratamiento indicado de la cantidad realmente utilizada. Sin este registro no es posible actualizar correctamente el inventario ni determinar el costo de medicamentos atribuible al lote y al galpón durante ese ciclo productivo.

**Independent Test**: Se puede probar seleccionando un diagnóstico asociado con un lote y un galpón, e ingresando una cantidad válida del medicamento definido en su medicación. El sistema debe convertir la cantidad a la unidad base, descontarla de las recepciones disponibles, conservar el precio histórico de cada recepción y dejar el consumo disponible para que el módulo 3 calcule su costo.

**Acceptance Scenarios**:

1. **Scenario**: Registro correcto utilizando una sola recepción
   - **Given** que un diagnóstico corresponde a un lote y a un galpón, define un medicamento mediante su medicación y existe cantidad suficiente en una recepción del inventario
   - **When** el veterinario registra la cantidad aplicada, su unidad y la fecha de aplicación
   - **Then** el sistema crea el consumo, lo asocia con el diagnóstico, obtiene automáticamente el lote y el galpón, convierte la cantidad a la unidad base, descuenta las existencias utilizadas y conserva la recepción y su precio neto histórico por unidad base

2. **Scenario**: Consumo abastecido por varias recepciones
   - **Given** que la recepción con vencimiento más próximo no tiene cantidad suficiente para cubrir todo el consumo y existen otras recepciones disponibles del mismo medicamento
   - **When** el veterinario confirma la cantidad aplicada
   - **Then** el sistema completa el consumo utilizando las recepciones necesarias en orden de vencimiento, crea un detalle independiente por cada recepción y conserva en cada detalle la cantidad descontada y su precio neto histórico por unidad base

3. **Scenario**: Intento de registro por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta registrar un consumo de medicamento
   - **When** solicita confirmar la operación
   - **Then** el sistema rechaza la operación y no crea el consumo ni modifica las existencias


### Edge Cases

- **Edge case #1 - Una recepción vence o cambia su disponibilidad antes de confirmar**

  - ¿Cómo maneja el sistema una recepción que estaba disponible al iniciar el registro, pero vence, es anulada o deja de tener existencias suficientes antes de confirmar?  
    El sistema debe consultar nuevamente las existencias y fechas de vencimiento al confirmar, excluir las recepciones no disponibles y recalcular la distribución. Si la cantidad total ya no puede cubrirse, debe rechazar la operación sin realizar descuentos parciales.

- **Edge case #2 - Recepciones con diferentes precios históricos**

  - ¿Cómo maneja el sistema un consumo abastecido por recepciones del mismo medicamento compradas a diferentes precios?  
    El sistema debe conservar un detalle por recepción y no calcular un precio único que elimine la trazabilidad. El costo se obtiene multiplicando la cantidad tomada de cada recepción por su propio precio neto histórico por unidad base.


- **Edge case #3 - Consumos expresados en unidades incompatibles**

  - ¿Cómo maneja el sistema un consumo ingresado en una unidad que no puede convertirse a la unidad base del medicamento, como kilogramos para un medicamento controlado por unidades físicas?  
    El sistema debe rechazar el registro y solicitar una unidad compatible. No debe convertir ni sumar magnitudes incompatibles.


## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro de consumos de medicamentos exclusivamente a usuarios con rol de veterinario.
- **FR-002**: Cada consumo DEBE estar asociado con un único diagnóstico existente.
- **FR-003**: El sistema DEBE obtener automáticamente desde el diagnóstico el lote, el galpón y la medicación correspondientes; estos datos NO DEBEN ser seleccionados manualmente durante el registro del consumo.
- **FR-004**: El sistema DEBE identificar el medicamento a partir de la medicación conservada por el diagnóstico y NO DEBE permitir que el veterinario lo sustituya por otro medicamento durante el registro del consumo.
- **FR-005**: Cada consumo DEBE registrar cantidad ingresada, unidad ingresada, cantidad normalizada, unidad base y fecha de aplicación.
- **FR-006**: El sistema DEBE utilizar primero las existencias disponibles con fecha de vencimiento más próxima y DEBE poder distribuir un consumo entre varias recepciones del mismo medicamento cuando sea necesario.


### Key Entities

- **Consumo de medicamento**: Representa la cantidad de medicamento realmente aplicada como parte del tratamiento de un lote.
  - **Atributos**: cantidad ingresada, unidad ingresada, cantidad total normalizada, unidad base, fecha de aplicación y fecha y hora de registro.
  - **Relaciones**: pertenece a un único diagnóstico y contiene uno o varios detalles de consumo.
  - **Datos accesibles mediante sus relaciones**: obtiene del diagnóstico el lote, el galpón y la medicación; obtiene de la medicación conservada por el diagnóstico el medicamento aplicado.
- **Detalle de consumo**: Representa la parte de un consumo descontada de una recepción específica del inventario.
  - **Atributos**: cantidad descontada en unidad base, precio neto histórico de compra por unidad base y moneda.
  - **Relaciones**: pertenece a un único consumo y referencia una única recepción de medicamento.
- **Diagnóstico del galpón**: Representa el diagnóstico que origina el tratamiento al cual corresponde el consumo.
  - **Relaciones**: corresponde a un único lote y a un único galpón, conserva una única medicación y puede tener cero o varios consumos de medicamento.
- **Medicación**: Representa el tratamiento seleccionado en el diagnóstico.
  - **Relaciones**: determina el medicamento que puede consumirse para el diagnóstico; el consumo utiliza los datos conservados al registrar ese diagnóstico.
- **Medicamento**: Representa el producto aplicado y controlado en el inventario.
  - **Atributos utilizados**: nombre y unidad base definida por su presentación.
  - **Relaciones**: puede tener múltiples recepciones y puede aparecer en diferentes consumos mediante las medicaciones de los diagnósticos.
- **Lote**: Representa el grupo de aves que recibió el medicamento.
  - **Relaciones**: queda identificado por el diagnóstico y puede acumular consumos provenientes de uno o varios diagnósticos durante su vida productiva.
- **Galpón**: Representa el espacio en el que se encuentra el lote tratado.
  - **Relaciones**: queda identificado por el diagnóstico y permite atribuir los consumos al galpón sin mezclar los diferentes lotes que puede alojar a lo largo del tiempo.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los veterinarios puede registrar un consumo válido en menos de 5 minuto.
