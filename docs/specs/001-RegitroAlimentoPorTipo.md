# Feature Specification: Registro de Alimento por Tipo e Inventario

**Created**: 2026-08-25  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ingreso de Alimento al Inventario General y Asignación a Galpón (Priority: P1)

Como encargado de la granja avícola, quiero registrar el ingreso de alimento por bultos al inventario especificando el tipo de alimento, precio por bulto, lote, fecha de vencimiento y el galpón destino, para mantener actualizado el inventario disponible según la etapa de crianza de las aves.

**Why this priority**: Es la funcionalidad base esencial (adición de inventario en valor positivo) sin la cual no se pueden realizar consumos ni control de insumos.

**Independent Test**: Puede probarse de forma independiente registrando un ingreso positivo de bultos de un tipo de alimento específico para un galpón en una etapa determinada, verificando que el stock aumente y se persistan la fecha de vencimiento y el precio por bulto del lote.

**Acceptance Scenarios**:

1. **Scenario**: Registro exitoso de ingreso positivo de alimento
   - **Given** que existe un galpón en etapa "Inicio" y un tipo de alimento compatible "Alimento Iniciador"
   - **When** el usuario registra un movimiento positivo de `+50` bultos, con fecha de vencimiento `2026-12-31` y precio de `$25.00` por bulto
   - **Then** el sistema crea un nuevo lote de alimento, incrementa el inventario disponible del galpón en 50 bultos y calcula el valor total de la entrada en `$1250.00`.

2. **Scenario**: Rechazo de ingreso de alimento no compatible con la etapa del galpón
   - **Given** que el galpón se encuentra en etapa "Inicio"
   - **When** el usuario intenta registrar un ingreso de alimento tipo "Alimento Engorde / Finalizador"
   - **Then** el sistema bloquea la transacción y muestra un mensaje de error indicando incompatibilidad entre el alimento y la etapa del galpón.

3. **Scenario**: Rechazo de ingreso con fecha de vencimiento expirada
   - **Given** el formulario de registro de alimento
   - **When** el usuario ingresa una fecha de vencimiento anterior o igual a la fecha actual
   - **Then** el sistema invalida el registro indicando que el lote a ingresar ya se encuentra vencido.

---

### User Story 2 - Gasto / Consumo de Alimento en Galpón (Priority: P2)

Como operario del galpón, quiero registrar el consumo diario de alimento (movimiento negativo en bultos) de un galpón, para descontar automáticamente del inventario usando la metodología FIFO (primer lote en vencer) y reflejar el costo real consumido.

**Why this priority**: Permite llevar el control del gasto diario de alimento en el galpón y calcular la conversión alimenticia y costos de producción.

**Independent Test**: Habiendo lotes previamente ingresados, registrar una cantidad negativa (ej. `-10` bultos), verificando que el sistema reduzca el stock disponible del lote más antiguo/próximo a vencer y asigne automáticamente el precio por bulto del lote consumido.

**Acceptance Scenarios**:

1. **Scenario**: Consumo exitoso con método FIFO automático
   - **Given** que el galpón tiene dos lotes disponibles de "Alimento Iniciador": Lote A (10 bultos, vence en 5 días, $25/bulto) y Lote B (20 bultos, vence en 20 días, $26/bulto)
   - **When** el usuario registra un consumo de `-15` bultos
   - **Then** el sistema consume totalmente los 10 bultos del Lote A y consume 5 bultos del Lote B, dejando el Lote B con 15 bultos restantes y registrando el costo exacto basado en cada lote.

2. **Scenario**: Bloqueo de consumo si la cantidad supera el inventario disponible
   - **Given** que el galpón cuenta con un stock total de 20 bultos de alimento
   - **When** el usuario intenta registrar un gasto de `-25` bultos
   - **Then** el sistema rechaza la transacción con un error de stock insuficiente y no permite que el inventario quede en negativo.

---

### User Story 3 - Consulta e Historial de Inventario y Movimientos (Priority: P3)

Como administrador de la granja, quiero consultar el saldo de inventario actual por tipo de alimento y galpón, así como la trazabilidad de lotes e historial de movimientos (ingresos y gastos), para tomar decisiones sobre reabastecimiento y control de costos.

**Why this priority**: Proporciona visibilidad estratégica e informes para la gestión de la granja.

**Independent Test**: Ejecutar consultas por filtro de galpón o tipo de alimento y verificar que el saldo actual sea exactamente igual a la suma de ingresos menos gastos.

**Acceptance Scenarios**:

1. **Scenario**: Consulta de stock actual y alertas de vencimiento
   - **Given** inventario existente en la granja
   - **When** el usuario consulta el inventario del Galpón 1
   - **Then** el sistema despliega el saldo total en bultos por tipo de alimento, detalle de lotes activos y resalta lotes próximos a vencer (menos de 7 días).

---

### Edge Cases

- **Registro de cantidad cero**: Si se ingresa una cantidad de `0` bultos, el sistema debe rechazar el registro indicando que la cantidad debe ser mayor o menor a cero.
- **Precio por bulto en movimientos negativos**: El precio por bulto en movimientos negativos no debe ser editable por el usuario; se calcula automáticamente a partir del costo unitario del lote procesado por FIFO.
- **Agotamiento exacto de stock**: Cuando un consumo deja un lote en `0` bultos, el lote cambia de estado a "Agotado" y no se considera para futuros consumos FIFO.
- **Múltiples lotes con misma fecha de vencimiento**: En caso de coincidencia de fecha de vencimiento en distintos lotes, el FIFO desempatará seleccionando el lote creado más antiguo en la base de datos (por timestamp).
- **Cambio de etapa del galpón con alimento remanente**: Si un galpón cambia de etapa (ej. de Inicio a Crecimiento), el sistema debe permitir gestionar o transferir el remanente de alimento no compatible antes de iniciar nuevos registros de la nueva etapa.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir registrar movimientos de alimento en bultos especificando valores positivos (adición/ingreso al inventario) y valores negativos (gasto/consumo del inventario).
- **FR-002**: El sistema DEBE asociar cada movimiento de alimento a un Tipo de Alimento y a un Galpón específico.
- **FR-003**: El sistema DEBE validar de forma estricta que el Tipo de Alimento registrado sea compatible con la Etapa de Crecimiento actual del Galpón destino.
- **FR-004**: Para movimientos positivos (ingresos), el sistema DEBE requerir obligatoriamente la fecha de vencimiento del lote y el precio por bulto (mayor a 0).
- **FR-005**: Para movimientos negativos (gastos), el sistema DEBE aplicar el algoritmo FIFO (First In, First Out) basándose en la fecha de vencimiento del lote disponible en la granja/galpón.
- **FR-006**: Para movimientos negativos, el sistema DEBE asignar automáticamente el precio unitario del lote del cual se descuenta el alimento, impidiendo el ingreso manual del precio en salidas.
- **FR-007**: El sistema DEBE validar y bloquear cualquier registro negativo que supere la cantidad total de bultos disponibles en stock (prohibido stock negativo).
- **FR-008**: El sistema DEBE mantener un historial inmutable de cada transacción de alimento (audit logs con timestamp, usuario, tipo de movimiento, cantidad, precio, lote y galpón).

### Key Entities

- **TipoAlimento**: Representa el catálogo de alimentos (ej. Iniciador, Crecimiento, Engorde, Pre-postura). Atributos: ID, nombre, descripción, etapaAsociada, unidadMedida (Bultos).
- **Galpon**: Representa la unidad de alojamiento de aves. Atributos: ID, nombre/código, capacidad, etapaActual (ej. Inicio, Crecimiento, Finalización).
- **LoteAlimento**: Representa la remesa/lote de alimento ingresada. Atributos: ID, tipoAlimento, fechaVencimiento, cantidadInicialBultos, cantidadDisponibleBultos, precioPorBulto, estado (Activo, Agotado, Vencido).
- **MovimientoInventarioAlimento**: Representa la transacción individual de ingreso o gasto. Atributos: ID, loteAlimento, galpon, tipoMovimiento (INGRESO / GASTO), cantidadBultos (positivo/negativo), precioUnitarioAplicado, fechaMovimiento.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% de las transacciones de consumo (negativas) aplican correctamente la regla FIFO descontando primero del lote con fecha de vencimiento más próxima.
- **SC-002**: 0% de ocurrencia de stock negativo en la base de datos gracias a las validaciones antes de persistir la transacción.
- **SC-003**: 100% de coincidencia de compatibilidad entre la etapa del galpón y el tipo de alimento registrado.
- **SC-004**: Los usuarios pueden completar el registro de un ingreso o gasto de alimento en menos de 30 segundos a través de la interfaz.
