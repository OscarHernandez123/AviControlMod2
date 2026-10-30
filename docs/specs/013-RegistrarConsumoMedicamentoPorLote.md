# Feature Specification: 013 - Registrar consumo de medicamento

**Created**: 2026-09-05

**Last Updated**: 2026-10-28

## User Scenarios & Testing

### User Story 1 - Registrar el consumo real de un medicamento (Priority: P1)

Como veterinario, quiero registrar la cantidad de medicamento realmente aplicada a un lote a partir de su diagnóstico activo, para descontar atómicamente las existencias utilizadas del inventario por orden de vencimiento (FEFO) y dejar disponibles para el módulo 3 los datos y precios históricos por recepción necesarios para calcular el costo de los medicamentos aplicados al lote.

**Why this priority**: El consumo permite diferenciar el tratamiento prescrito (Spec 008) de la cantidad física realmente utilizada en campo. Sin este registro no es posible descontar el inventario de medicamentos (Spec 023) ni suministrar al módulo 3 el desglose exacto con precios históricos de compra (Spec 002) atribuible al lote y al galpón.

**Independent Test**: Se puede probar seleccionando un diagnóstico tratable asociado con un lote, un galpón y una medicación, e ingresando una cantidad válida y unidad compatible del medicamento definido en dicha medicación. El sistema debe derivar automáticamente el galpón, lote y medicamento desde el diagnóstico, normalizar la cantidad a la unidad base, descontarla de las recepciones disponibles por fecha de vencimiento más próxima (FEFO), conservar el precio neto histórico de cada recepción afectada y dejar el desglose disponible para el módulo 3.

**Acceptance Scenarios**:

1. **Scenario**: Registro correcto utilizando una sola recepción
   - **Given** que un diagnóstico corresponde a un lote y a un galpón, define un medicamento mediante su medicación y existe cantidad disponible suficiente en la recepción con vencimiento más próximo del inventario
   - **When** el veterinario registra la cantidad aplicada, su unidad compatible y la fecha de aplicación
   - **Then** el sistema crea el consumo asociado al diagnóstico, obtiene automáticamente el lote, el galpón y el medicamento, convierte la cantidad a la unidad base, descuenta las existencias utilizadas y conserva en el detalle la recepción afectada y su precio neto histórico por unidad base

2. **Scenario**: Consumo abastecido por varias recepciones (FEFO)
   - **Given** que la recepción vigente con vencimiento más próximo no tiene cantidad suficiente para cubrir todo el consumo, pero la suma de existencias con otras recepciones vigentes del mismo medicamento sí cubre el total
   - **When** el veterinario confirma la cantidad aplicada
   - **Then** el sistema completa el consumo utilizando las recepciones necesarias en estricto orden de fecha de vencimiento (FEFO), crea un detalle independiente por cada recepción afectada y conserva en cada detalle la cantidad descontada y su propio precio neto histórico por unidad base sin promediar valores

3. **Scenario**: Rechazo por existencias insuficientes en el inventario
   - **Given** que la suma de existencias disponibles en las recepciones vigentes del medicamento no alcanza a cubrir la cantidad solicitada
   - **When** el veterinario intenta confirmar el consumo
   - **Then** el sistema rechaza la operación con error de conflicto, informa que las existencias son insuficientes y no crea el consumo ni aplica descuentos parciales

4. **Scenario**: Rechazo por unidad de medida incompatible
   - **Given** que el medicamento del diagnóstico se controla en una dimensión específica de unidad base (`GRAMO`, `MILILITRO` o `UNIDAD`)
   - **When** el veterinario ingresa el consumo en una unidad de dimensión incompatible (por ejemplo, mililitros para un medicamento controlado por unidades o gramos)
   - **Then** el sistema rechaza el registro, solicita una unidad compatible y no altera el inventario

5. **Scenario**: Rechazo por diagnóstico inexistente o sin medicación prescrita
   - **Given** un diagnóstico que no existe o que corresponde a una enfermedad mortal sin medicación asociada
   - **When** el veterinario intenta registrar un consumo de medicamento
   - **Then** el sistema rechaza la operación e informa que se requiere un diagnóstico válido con medicación prescrita

6. **Scenario**: Intento de registro por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta registrar un consumo de medicamento
   - **When** solicita confirmar la operación
   - **Then** el sistema rechaza la operación y no crea el consumo ni modifica las existencias

---

### Edge Cases

- **Edge case #1 - Una recepción vence o cambia su disponibilidad antes de confirmar**
  - ¿Cómo maneja el sistema una recepción que estaba disponible al iniciar el registro, pero vence, es anulada o deja de tener existencias suficientes antes de confirmar?  
    El sistema debe consultar nuevamente las existencias y fechas de vencimiento dentro de la transacción al confirmar, excluir las recepciones vencidas o anuladas y recalcular la distribución FEFO. Si la cantidad total ya no puede cubrirse, debe rechazar toda la operación sin realizar descuentos parciales.

- **Edge case #2 - Recepciones con diferentes precios históricos**
  - ¿Cómo maneja el sistema un consumo abastecido por recepciones del mismo medicamento compradas a diferentes precios?  
    El sistema debe conservar un detalle independiente por cada recepción y nunca calcular un precio promedio que elimine la trazabilidad. El módulo 3 obtendrá el costo real multiplicando la cantidad tomada de cada recepción por su propio precio neto histórico por unidad base.

- **Edge case #3 - Consumos expresados en miligramos frente a gramos**
  - ¿Cómo maneja el sistema un consumo ingresado en miligramos para un medicamento cuya unidad base de masa es el gramo?  
    Dentro de la dimensión de masa, el sistema convierte automáticamente `MILIGRAMO` a `GRAMO` (`1.000 mg = 1 g`) conservando la precisión decimal con `BigDecimal`, y rechaza cualquier cruce entre dimensiones distintas (masa, volumen o conteo).

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro de consumos de medicamentos exclusivamente a usuarios autenticados con rol de veterinario.
- **FR-002**: Cada consumo DEBE estar asociado con un único diagnóstico existente que cuente con una medicación prescrita (Spec 011).
- **FR-003**: El sistema DEBE obtener automáticamente desde el diagnóstico el lote (`loteId`), el galpón (`galponId`), la medicación y el medicamento (`medicamentoId`) correspondientes; estos datos NO DEBEN ser seleccionados ni sustituidos manualmente por el cliente durante el registro del consumo.
- **FR-004**: Cada consumo DEBE registrar cantidad ingresada (positiva), unidad ingresada, cantidad normalizada, unidad base y fecha de aplicación. Las unidades permitidas son `MILIGRAMO`, `GRAMO`, `MILILITRO` y `UNIDAD`; las unidades de masa se normalizan a `GRAMO` (`1.000 mg = 1 g`), volumen a `MILILITRO` y conteo a `UNIDAD`. El sistema DEBE rechazar unidades de dimensiones incompatibles.
- **FR-005**: El sistema DEBE descontar primero las existencias disponibles no vencidas ni anuladas con fecha de vencimiento más próxima (FEFO) y DEBE distribuir el consumo entre varias recepciones del mismo medicamento cuando una sola no sea suficiente.
- **FR-006**: Si las existencias disponibles no cubren el 100 % de la cantidad normalizada o falla el descuento en inventario, el sistema DEBE revertir la operación completa sin guardar el consumo ni aplicar descuentos parciales.
- **FR-007**: Al confirmar el consumo, el sistema DEBE registrar y dejar disponibles para el módulo 3 el identificador del lote de aves, el identificador del galpón, la fecha de aplicación, el medicamento, la cantidad consumida en unidad base y el desglose de cada detalle de consumo (`recepcionId`, `cantidadDescontadaUnidadBase` y `precioNetoHistoricoUnidadBase`).
- **FR-008**: La información suministrada DEBE permitir al módulo 3 calcular el costo real de los medicamentos del lote mediante la suma de `cantidad descontada de cada recepción en unidad base × precio neto histórico de compra por unidad base`, sin que el módulo 2 ejecute la liquidación contable del lote.
- **FR-009**: El sistema DEBE soportar cabeceras de idempotencia técnica (`X-Idempotency-Key`) para garantizar que los reintentos de red no generen un doble descuento de las existencias en el inventario.
- **FR-010**: Al confirmar el consumo, el sistema DEBE publicar un evento de integración (`ConsumoMedicamentoRegistradoIntegrationEvent`) para notificar al módulo de inventario (para el descuento físico) y al módulo 3 (para el costeo) con los datos y precios históricos.

### Non-Functional Requirements

| ID          | Categoría     | Requerimiento                                                                          | Métrica                                    |
| :---------- | :------------ | :------------------------------------------------------------------------------------- | :----------------------------------------- |
| **NFR-001** | Rendimiento   | Latencia de validación, normalización, descuento FEFO y persistencia                   | < 2 segundos (p95) bajo carga normal       |
| **NFR-002** | Seguridad     | Acceso restringido exclusivamente al rol VETERINARIO                                   | HTTP 403 Forbidden a otros roles           |
| **NFR-003** | Integridad    | Escritura atómica (consumo + detalles + descuento de inventario) bajo `@Transactional` | 100% de consistencia local                 |
| **NFR-004** | Concurrencia  | Bloqueo a nivel de recepción para evitar sobreventa concurrente                        | 0% de descuentos huérfanos o negativos     |
| **NFR-005** | Inmutabilidad | Conservación estricta del precio histórico por recepción sin promedios                 | 100% de los precios originales conservados |

### Key Entities

- **Consumo de medicamento**: Representa la cantidad de medicamento realmente aplicada en campo como parte del tratamiento de un lote.
  - **Atributos**: identificador UUID, `diagnosticoId`, `galponId`, `loteId`, `medicamentoId`, `cantidadIngresada`, `unidadIngresada`, `cantidadNormalizada`, `unidadBase`, `fechaAplicacion` y `registradoEn`.
  - **Relaciones**: pertenece a un único diagnóstico, contiene uno o varios detalles de consumo (cuya suma iguala a `cantidadNormalizada`) y provee los datos de costeo al módulo 3.
- **Detalle de consumo**: Representa la fracción o totalidad de un consumo descontada de una recepción específica del inventario.
  - **Atributos**: identificador UUID, `consumoId`, `recepcionId`, `cantidadDescontadaUnidadBase`, `precioNetoHistoricoUnidadBase` y `moneda`.
  - **Relaciones**: pertenece a un único consumo y referencia una única recepción de medicamento (Spec 002).
- **Diagnóstico del galpón**: Representa el diagnóstico clínico que origina el tratamiento al cual corresponde el consumo (Spec 011).
  - **Relaciones**: provee de forma inmutable el lote, el galpón y la medicación prescrita.
- **Medicamento**: Representa el producto aplicado y controlado en el inventario central (Spec 002 y Spec 023) en su unidad base compatible (`GRAMO`, `MILILITRO` o `UNIDAD`).

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los veterinarios puede registrar un consumo válido en menos de 3 minutos.
- **SC-002**: El 95 % de los consumos confirmados descuenta el inventario y deja sus datos y precios históricos disponibles para el módulo 3 en un máximo de 2 segundos.
- **SC-003**: El 100 % de los consumos abastecidos por recepciones con precios diferentes conserva el desglose exacto por recepción sin promediar precios ni perder trazabilidad para el módulo 3.
- **SC-004**: El 100 % de los intentos realizados por usuarios sin rol de veterinario o con stock insuficiente es rechazado sin alterar el inventario ni generar descuentos parciales.
- **SC-005**: Cero duplicidad (0%) en descuentos de inventario o eventos generados ante reintentos de red.
