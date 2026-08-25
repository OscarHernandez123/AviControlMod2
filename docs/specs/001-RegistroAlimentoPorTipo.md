# Feature Specification: 001 - Gestión de Inventario y Registro de Alimento por Tipo

**Created**: 2026-08-25  
**Status**: Draft / Aprobación de Requisitos  
**Reference Document**: [sdd-guide.MD](../sdd-guide.MD) | [DiagramaClaseDeUso.jpeg](../DiagramaClaseDeUso.jpeg)

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Catálogo de Tipos de Alimento y Fases Compatibles (Priority: P1)

Como Administrador, quiero registrar y administrar de forma exclusiva los tipos de alimento con su peso estándar por bulto y las fases productivas compatibles de las aves, para asegurar que cada galpón reciba únicamente la dieta adecuada para su etapa biológica.

**Why this priority**: Es la base del sistema de inventario. Sin tipos de alimento categorizados con sus factores de conversión y reglas de compatibilidad de fases, no es posible registrar ingresos, traslados ni consumos válidos.

**Independent Test**: Puede probarse creando tipos de alimento (ej. "Iniciador Pollito", "Engorde Plus"), definiendo su peso por bulto (ej. 40 kg) y asignándoles sus fases compatibles ("Iniciación", "Crecimiento", etc.), verificando que queden disponibles para el flujo de inventario únicamente cuando la acción es realizada por un Administrador.

**Acceptance Scenarios**:

1. **Scenario**: Registro exitoso de un tipo de alimento con fases válidas por Administrador
   - **Given** que el usuario tiene rol exclusivo de Administrador
   - **When** registra un tipo de alimento con nombre "Iniciador Fase 1", peso estándar de 40 kg por bulto y fases compatibles ["Iniciación", "Recepción"]
   - **Then** el sistema guarda el tipo de alimento en estado activo y queda disponible para recibir ingresos de inventario.

2. **Scenario**: Rechazo por datos de conversión inválidos o fases vacías
   - **Given** que el Administrador intenta crear un tipo de alimento
   - **When** ingresa un peso por bulto menor o igual a 0 o no selecciona al menos una fase productiva compatible
   - **Then** el sistema rechaza el registro indicando el error de validación correspondiente y no persiste cambios.

3. **Scenario**: Rechazo de creación o modificación por usuario no administrador
   - **Given** que un usuario sin rol de Administrador (ej. Nutricionista, Trabajador) intenta crear o modificar un tipo de alimento
   - **When** envía la solicitud al catálogo de tipos de alimento
   - **Then** el sistema bloquea la acción indicando permiso denegado por exclusividad del rol Administrador.

---

### User Story 2 - Registro de Ingreso de Lotes de Alimento a Bodega Central (Priority: P1)

Como Administrador o Encargado de Bodega, quiero registrar la recepción de lotes de alimento en la Bodega Central especificando lote, fecha de vencimiento, costo unitario y cantidad (en bultos o kilogramos), para mantener el stock centralizado con trazabilidad financiera y de caducidad.

**Why this priority**: Alimenta la bodega central que abastece a todos los galpones, iniciando la cadena FIFO con su costo y fecha de vencimiento.

**Independent Test**: Se puede probar registrando un ingreso de 100 bultos (o 4,000 kg) de un alimento a un costo unitario con fecha de vencimiento futura, verificando que el stock de Bodega Central aumente exactamente en dicha cantidad y se cree el registro inmutable de lote.

**Acceptance Scenarios**:

1. **Scenario**: Ingreso exitoso especificando cantidad en bultos con cálculo dual de kilogramos
   - **Given** que existe el tipo de alimento "Engorde Fase 2" con 40 kg por bulto
   - **When** el usuario registra un ingreso a Bodega Central de 50 bultos con código de lote "LOT-2026-01", fecha de vencimiento en 60 días y costo unitario por bulto de $80,000
   - **Then** el sistema crea el lote en Bodega Central con 50 bultos (2,000 kg), calcula el costo total de $4,000,000 y actualiza el stock disponible de la bodega.

2. **Scenario**: Ingreso exitoso especificando cantidad en kilogramos con conversión a bultos
   - **Given** que existe el tipo de alimento "Iniciador" con 40 kg por bulto
   - **When** el usuario ingresa 1,000 kg con su costo y fecha de vencimiento válida
   - **Then** el sistema registra el lote con 25 bultos exactos y 1,000 kg en Bodega Central.

3. **Scenario**: Rechazo de ingreso con fecha de vencimiento caducada
   - **Given** que el usuario intenta ingresar un nuevo lote a Bodega Central
   - **When** la fecha de vencimiento ingresada es anterior o igual a la fecha actual del sistema
   - **Then** el sistema bloquea el ingreso con un mensaje indicando que no se pueden recibir productos vencidos.

---

### User Story 3 - Traslado de Alimento de Bodega Central a Galpón con Validación de Fase (Priority: P1)

Como Trabajador o Administrador, quiero trasladar bultos/kilogramos de alimento desde la Bodega Central hacia la sub-bodega de un Galpón específico, para que el galpón disponga de stock físico validando que el alimento sea compatible con la fase productiva activa del galpón.

**Why this priority**: Conecta el inventario central con la sub-bodega operativa del galpón, aplicando la regla de negocio de compatibilidad biológica y evitando inventario negativo en bodega.

**Independent Test**: Se traslada una cantidad de alimento hacia un galpón en fase "Iniciación". Si el alimento es compatible y hay stock suficiente en Bodega Central, se descuenta de bodega y se suma al stock del galpón bajo el mismo lote; si la fase no coincide o no hay stock, se rechaza.

**Acceptance Scenarios**:

1. **Scenario**: Traslado exitoso con alimento compatible y stock suficiente
   - **Given** que la Bodega Central tiene 50 bultos del lote "LOT-01" de "Iniciador Fase 1" y el Galpón 3 se encuentra en fase activa "Iniciación" (compatible)
   - **When** el usuario solicita el traslado de 10 bultos hacia el Galpón 3
   - **Then** el sistema descuenta 10 bultos de Bodega Central, acredita 10 bultos al stock del Galpón 3 conservando la trazabilidad del lote "LOT-01" y registra el movimiento de traslado.

2. **Scenario**: Rechazo de traslado por incompatibilidad con la fase productiva del galpón
   - **Given** que el Galpón 2 se encuentra en fase activa "Engorde"
   - **When** el usuario intenta trasladar alimento de tipo "Preiniciador" (compatible solo con "Iniciación")
   - **Then** el sistema bloquea la operación y notifica que el tipo de alimento es incompatible con la fase productiva actual del galpón.

3. **Scenario**: Rechazo por stock insuficiente en Bodega Central (bloqueo de inventario negativo)
   - **Given** que la Bodega Central solo posee 5 bultos del alimento solicitado
   - **When** el usuario intenta trasladar 10 bultos al galpón
   - **Then** el sistema rechaza la solicitud impidiendo que el stock de Bodega Central quede en valores negativos.

---

### User Story 4 - Registro de Consumo Diario en Galpón con FIFO Automático y Costo Inmutable (Priority: P1)

Como Trabajador de Galpón, quiero registrar la cantidad diaria de alimento consumido por las aves (en bultos o kilos), para que el sistema descuente automáticamente del stock del galpón usando FIFO (lotes más próximos a vencer), omitiendo lotes caducados y calculando el costo real inmutable consumido.

**Why this priority**: Es el núcleo operativo diario. Asegura la alimentación de las aves, la correcta valuación del costo de producción y la protección contra el consumo accidental de alimento vencido.

**Independent Test**: En un galpón con 2 lotes trasladados con distintas fechas de vencimiento y costos, se registra un consumo. Se verifica que se consuma primero del lote con vencimiento más cercano, se calcule el costo exacto consumido de forma inalterable y no se permita consumir más del stock disponible en el galpón.

**Acceptance Scenarios**:

1. **Scenario**: Consumo con descuento FIFO automático de un solo lote
   - **Given** que el Galpón 1 tiene en su stock 20 bultos del Lote A (vence en 20 días, costo $80,000/bulto) y 30 bultos del Lote B (vence en 50 días, costo $85,000/bulto)
   - **When** el trabajador registra un consumo diario de 5 bultos (o 200 kg) para el Galpón 1
   - **Then** el sistema descuenta los 5 bultos íntegramente del Lote A por ser el más próximo a vencer, calcula un costo total consumido inmutable de $400,000 y actualiza el stock del Lote A a 15 bultos.

2. **Scenario**: Consumo FIFO distribuido entre múltiples lotes
   - **Given** que el Galpón 1 tiene 5 bultos del Lote A ($80,000/bulto, vence en 15 días) y 20 bultos del Lote B ($90,000/bulto, vence en 40 días)
   - **When** se registra un consumo de 8 bultos para el Galpón 1
   - **Then** el sistema agota los 5 bultos del Lote A ($400,000) y toma 3 bultos del Lote B ($270,000), registrando un costo total consumido inmutable de $670,000 y dejando el stock del Galpón 1 en 17 bultos.

3. **Scenario**: Omisión automática de lote vencido y alerta de bloqueo
   - **Given** que el Galpón 1 tiene 4 bultos del Lote VENCIDO (venció hace 2 días) y 15 bultos del Lote VIGENTE (vence en 25 días, costo $82,000/bulto)
   - **When** el trabajador registra un consumo diario de 6 bultos
   - **Then** el sistema omite y bloquea el Lote VENCIDO, descuenta los 6 bultos del Lote VIGENTE, calcula el costo con base exclusiva en el lote vigente y genera una alerta inmediata de lote caducado requiriendo baja.

4. **Scenario**: Bloqueo de consumo por stock insuficiente en el galpón
   - **Given** que el stock total vigente del Galpón 1 es de 3 bultos
   - **When** el usuario intenta registrar un consumo de 5 bultos
   - **Then** el sistema rechaza la transacción, bloquea cualquier saldo negativo y muestra el saldo máximo disponible.

---

### User Story 5 - Monitoreo de Caducidad con Semáforo de Alertas y Bloqueo (Priority: P2)

Como Administrador, Nutricionista o Trabajador, quiero visualizar el estado de caducidad de todos los lotes de alimento mediante un semáforo preventivo, para priorizar el uso del alimento o gestionar el descarte oportuno.

**Why this priority**: Previene pérdidas económicas por vencimiento desapercibido y evita riesgos sanitarios en las aves.

**Independent Test**: Consultar el panel de lotes y verificar que cada lote muestre el color e indicador correcto según sus días restantes de vida útil.

**Acceptance Scenarios**:

1. **Scenario**: Clasificación semafórica de lotes
   - **Given** que existen lotes en inventario con diferentes fechas de caducidad
   - **When** el usuario consulta el panel de vencimientos
   - **Then** el sistema clasifica visualmente cada lote según:
     - 🟢 **Verde**: Más de 30 días restantes para su vencimiento.
     - 🟡 **Amarillo (Alerta preventiva)**: Entre 10 y 30 días restantes para su vencimiento (ambos inclusive).
     - 🔴 **Rojo (Crítico / Vencido)**: Menos de 10 días restantes o fecha de vencimiento ya expirada.

2. **Scenario**: Bloqueo visual y funcional de lotes en estado crítico vencido
   - **Given** un lote clasificado en semáforo Rojo por fecha expirada
   - **When** se visualiza en los paneles de inventario
   - **Then** se muestra con etiqueta de "Bloqueado para consumo" y con opción directa para registrar orden de baja o descarte.

---

### User Story 6 - Panel de Trazabilidad, Kardex e Historial de Movimientos (Priority: P2)

Como Administrador o Auditor, quiero consultar el historial completo e inmutable de movimientos de alimento por bodega y por galpón, para auditar ingresos, traslados, consumos y costos asociados con trazabilidad de lotes.

**Why this priority**: Proporciona transparencia operativa y financiera, permitiendo evaluar la eficiencia del alimento y detectar discrepancias.

**Independent Test**: Filtrar el historial por rango de fechas, galpón o tipo de alimento, comprobando que cada registro refleje fecha/hora, usuario responsable, tipo de movimiento, lote, cantidad en bultos/kg y costo monetario inmutable.

**Acceptance Scenarios**:

1. **Scenario**: Consulta de Kardex por Galpón
   - **Given** que se han efectuado traslados y consumos en el Galpón 2
   - **When** el usuario consulta el kardex del Galpón 2 filtrando por el mes actual
   - **Then** el sistema muestra la cronología detallada de ingresos por traslado, consumos diarios con desglose de lotes FIFO y saldo resultante acumulado en bultos y kg.

---

### User Story 7 - Registro de Ajustes y Bajas por Merma o Descarte (Priority: P3)

Como Administrador, quiero registrar la baja o descarte justificado de bultos/kilogramos de alimento (por caducidad, humedad o daño físico), para ajustar el stock real sin alterar los registros de consumo de las aves.

**Why this priority**: Mantiene la exactitud del inventario ante mermas imprevistas o lotes caducados sin distorsionar la métrica de consumo de los animales.

**Independent Test**: Se realiza la baja de 2 bultos de un lote vencido indicando motivo "Caducidad"; se verifica que el stock disminuya y el motivo quede asentado en el historial de mermas.

**Acceptance Scenarios**:

1. **Scenario**: Baja exitosa de lote vencido o dañado
   - **Given** un lote con saldo remanente bloqueado por caducidad en Bodega o Galpón
   - **When** el administrador registra la baja indicando cantidad y motivo "Vencimiento"
   - **Then** el sistema descuenta el saldo del lote, asienta la pérdida contable y remueve el lote de la lista de activos.

---

## Edge Cases

- **Consumo con stock en cero o menor al solicitado**: El sistema bloquea de forma atómica cualquier decremento que resulte en un valor menor a cero tanto en bultos como en kilogramos.
- **Galpón sin fase activa definida o en descanso sanitario**: Si un galpón no tiene un lote de aves activo o está en mantenimiento/desinfección, el sistema bloquea traslados y consumos diarios hacia dicho galpón.
- **Todos los lotes del galpón se encuentran vencidos**: El algoritmo FIFO detecta que ningún lote disponible está vigente, bloquea la transacción de consumo diario y notifica una alerta crítica de "Sin stock vigente disponible".
- **Conversión de decimales y redondeo en bultos/kilos**: Los cálculos de conversión entre bultos y kilogramos deben conservar precisión de hasta 2 decimales para evitar discrepancias por fracciones residuales.
- **Cambio de fase productiva del galpón con stock remanente**: Si un galpón cambia de fase (ej. de "Iniciación" a "Crecimiento") y aún tiene stock del alimento anterior, el sistema impide nuevos consumos de ese alimento anterior y solicita el traslado de retorno a bodega o ajuste correspondiente.
- **Registros concurrentes de consumo o traslado**: El sistema debe manejar concurrencia para que dos transacciones simultáneas no consuman el mismo saldo de lote ni generen saldos negativos.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir de forma exclusiva al **Administrador** la creación, consulta, actualización y desactivación de **Tipos de Alimento**, registrando: nombre comercial, descripción, peso estándar por bulto (en kg) y lista de **Fases Productivas Compatibles**. Otros roles tendrán acceso de solo lectura si se requiere para operaciones derivadas.
- **FR-002**: El sistema DEBE soportar **Control Dual de Medida** (bultos y kilogramos), aplicando la fórmula `Kilogramos = Bultos × PesoEstándarPorBulto` para todas las operaciones de inventario.
- **FR-003**: El sistema DEBE permitir el registro de **Ingresos de Alimento a Bodega Central**, capturando: tipo de alimento, código de lote, fecha de fabricación (opcional), fecha de vencimiento obligatoria, cantidad (bultos o kg) y costo unitario de adquisición.
- **FR-004**: El sistema DEBE rechazar cualquier ingreso de lote cuya fecha de vencimiento sea menor o igual a la fecha del registro.
- **FR-005**: El sistema DEBE permitir registrar **Traslados de Alimento** desde la Bodega Central hacia la sub-bodega de un Galpón destino.
- **FR-006**: El sistema DEBE validar de forma obligatoria que el tipo de alimento a trasladar sea compatible con la **Fase Productiva Activa** del galpón receptor antes de procesar el traslado.
- **FR-007**: El sistema DEBE bloquear cualquier traslado si la cantidad solicitada supera el stock disponible en la Bodega Central (bloqueo de inventario negativo).
- **FR-008**: El sistema DEBE permitir el registro de **Consumo Diario de Alimento por Galpón**, indicando galpón, fecha de consumo y cantidad consumida (en bultos o kilos).
- **FR-009**: El sistema DEBE implementar un **Algoritmo FIFO Automático** para los consumos en galpón, seleccionando y descontando primero del lote con la fecha de vencimiento más próxima.
- **FR-010**: El algoritmo FIFO DEBE **omitir automáticamente cualquier lote cuya fecha de vencimiento haya expirado** a la fecha del consumo, tomando del siguiente lote vigente y generando una alerta de lote caducado.
- **FR-011**: El sistema DEBE calcular el **Costo Real Consumido** multiplicando las unidades/kilos descontados de cada lote por su respectivo costo unitario de adquisición, registrándolo de forma **inmutable**.
- **FR-012**: El sistema DEBE bloquear la transacción de consumo si el stock total vigente disponible en el galpón es menor a la cantidad solicitada.
- **FR-013**: El sistema DEBE clasificar dinámicamente el estado de vencimiento de cada lote según un **Semáforo Preventivo**:
  - 🟢 **Verde**: Fecha de vencimiento $> 30$ días.
  - 🟡 **Amarillo**: Fecha de vencimiento entre $10$ y $30$ días (ambos inclusive).
  - 🔴 **Rojo**: Fecha de vencimiento $< 10$ días o ya vencido.
- **FR-014**: El sistema DEBE proveer un panel de **Alertas Preventivas** que liste todos los lotes en estado Amarillo y Rojo, diferenciando su ubicación (Bodega Central o Galpón).
- **FR-015**: El sistema DEBE mantener un **Kardex / Historial de Movimientos Inmutable** que registre cada ingreso, traslado, consumo y ajuste, incluyendo marca de tiempo, usuario responsable, lote, cantidad (bultos y kg), costo asociado y saldos resultantes.
- **FR-016**: El sistema DEBE permitir el registro de **Bajas y Ajustes de Inventario** por merma, daño o caducidad, requiriendo un motivo explícito y autorización de perfil Administrador.

---

### Key Entities *(conceptual)*

- **TipoAlimento**: Representa la categorización del alimento (ej. Iniciador, Crecimiento, Engorde). Contiene nombre, factor de peso por bulto (kg) y relación con las fases productivas autorizadas.
- **FaseProductiva**: Catálogo de etapas de desarrollo de las aves (ej. Recepción, Iniciación, Crecimiento, Engorde, Postura, Retiro).
- **LoteAlimento**: Identifica una partida específica de alimento recibida. Contiene código de lote, fecha de vencimiento, costo unitario de compra y tipo de alimento.
- **StockBodegaCentral**: Representa las existencias físicas disponibles de cada lote en la Bodega Central (bultos y kg).
- **StockGalpon**: Representa las existencias físicas disponibles de cada lote en la sub-bodega de un galpón específico.
- **MovimientoInventario**: Registro inmutable de cada transacción (Tipo: INGRESO_BODEGA, TRASLADO_GALPON, CONSUMO_DIARIO, BAJA_MERMA). Registra fecha/hora, usuario, galpón/bodega origen y destino, lote, bultos, kg, costo unitario y costo total imputado.
- **ConsumoDiario**: Registro consolidado del consumo diario de un galpón, asociado al detalle de los lotes consumidos mediante FIFO y su costo total acumulado.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: **Cero inventarios negativos**: El 100% de las transacciones que excedan el stock disponible en Bodega o Galpón son bloqueadas de forma preventiva.
- **SC-002**: **Cero consumos de alimento vencido o incompatible**: El 100% de los consumos diarios descuentan exclusivamente de lotes vigentes y compatibles con la fase del galpón.
- **SC-003**: **Exactitud en valuación de costos (100%)**: El costo calculado mediante FIFO coincide con la sumatoria exacta del producto `(cantidad_descontada_lote_i × costo_unitario_lote_i)` de forma inmutable.
- **SC-004**: **Eficiencia operativa en registro de consumo**: Un operario puede registrar el consumo diario de un galpón en menos de 15 segundos mediante la selección automática FIFO.
- **SC-005**: **Anticipación de alertas de vencimiento**: El 100% de los lotes que ingresen a la ventana $\le 30$ días y $\le 10$ días se reflejan inmediatamente en el panel semafórico sin desfase.
- **SC-006**: **Trazabilidad total e inmutable**: El 100% de los kilogramos/bultos consumidos pueden rastrearse retrospectivamente hasta el lote de ingreso y proveedor de origen en el Kardex.
