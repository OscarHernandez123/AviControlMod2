# Feature Specification: 002 - Gestión de Inventario y Registro de Medicamentos

**Created**: 2026-08-25  
**Status**: Draft / Aprobación de Requisitos  
**Reference Document**: [sdd-guide.MD](../sdd-guide.MD) | [DiagramaClaseDeUso.jpeg](../DiagramaClaseDeUso.jpeg) | [001-RegistroAlimentoPorTipo.md](001-RegistroAlimentoPorTipo.md)

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Catálogo de Medicamentos, Presentaciones y Tiempo de Retiro (Priority: P1)

Como Administrador, quiero registrar y administrar de forma exclusiva el catálogo de medicamentos veterinarios, definiendo su principio activo, vía de administración, presentación comercial, unidad base clínica ($ml$, $g$, dosis/unidades), factor de contenido neto y días de retiro sanitario, para estandarizar los insumos clínicos y habilitar el control de inventario y bioseguridad.

**Why this priority**: Es la base del módulo. Sin el catálogo maestro con factores de conversión y tiempos de retiro, no es posible registrar compras, controlar dosificaciones clínicas ni proteger la inocuidad alimentaria de las aves.

**Independent Test**: Probar la creación de medicamentos (ej. "Enrofloxacina 10%" en presentación "Frasco 500 ml", unidad base $ml$, factor 500, vía "Agua de bebida", tiempo de retiro 7 días) exclusivamente con usuario Administrador, verificando que quede activo para recepción de lotes y que usuarios no administradores sean rechazados.

**Acceptance Scenarios**:

1. **Scenario**: Creación exitosa de un medicamento por el Administrador
   - **Given** que el usuario tiene rol exclusivo de Administrador
   - **When** registra un medicamento con nombre "Amoxicilina Polvo", vía "Agua de bebida", presentación "Bolsa 1,000 g", unidad base "g", contenido neto 1000 y tiempo de retiro de 5 días
   - **Then** el sistema guarda el medicamento en estado activo disponible para la gestión de inventario y compras.

2. **Scenario**: Rechazo por datos de presentación o tiempo de retiro inválidos
   - **Given** que el Administrador intenta registrar un medicamento
   - **When** ingresa un factor de contenido neto menor o igual a 0, o un tiempo de retiro negativo
   - **Then** el sistema rechaza la solicitud indicando los errores de validación de campos obligatorios.

3. **Scenario**: Rechazo de acceso a usuarios sin rol de Administrador
   - **Given** que un usuario sin rol de Administrador (ej. Veterinario, Trabajador) intenta crear o editar un medicamento en el catálogo
   - **When** envía la solicitud de guardado
   - **Then** el sistema deniega el acceso con un error de autorización (403 / No autorizado) protegiendo la exclusividad del rol Administrador.

---

### User Story 2 - Registro de Ingreso de Lotes de Medicamentos a Farmacia Central (Priority: P1)

Como Administrador o Encargado de Farmacia, quiero registrar el ingreso de lotes de medicamentos a la Farmacia Central especificando número de lote, fecha de vencimiento obligatoria, cantidad de envases/presentaciones y costo unitario de compra, para mantener el inventario valorizado y alimentar la cadena FIFO.

**Why this priority**: Permite el abastecimiento oficial de fármacos en la granja, registrando las fechas de caducidad críticas y los costos reales de adquisición.

**Independent Test**: Registrar la compra de 10 frascos de un medicamento a $50,000 c/u con fecha de caducidad a 6 meses; verificar que el stock de Farmacia Central aumente a 10 frascos (5,000 ml en unidad base) y se genere el registro inmutable del lote.

**Acceptance Scenarios**:

1. **Scenario**: Ingreso exitoso de lote en envases con conversión a unidad base
   - **Given** que existe el medicamento "Complejo B Inyectable" (Frasco 250 ml, unidad base $ml$, factor 250)
   - **When** el usuario registra el ingreso a Farmacia Central de 8 frascos con lote "MED-2026-09", vencimiento en 180 días y costo de $30,000 por frasco
   - **Then** el sistema registra el lote con 8 envases (2,000 ml totales), costo total de $240,000 ($120/ml) y actualiza el stock disponible de Farmacia Central.

2. **Scenario**: Rechazo de ingreso de lote con fecha de vencimiento caducada
   - **Given** que el usuario intenta ingresar un nuevo lote a Farmacia Central
   - **When** la fecha de vencimiento es anterior o igual a la fecha actual del sistema
   - **Then** el sistema bloquea el ingreso notificando que no se permite recibir medicamentos caducados.

---

### User Story 3 - Registro de Aplicación Clínica con FIFO y Costo Inmutable (Priority: P1)

Como Veterinario, quiero registrar la aplicación/consumo de medicamentos en un galpón indicando la dosis (en $ml$, gramos o unidades), para que el sistema descuente automáticamente del lote más próximo a vencer (FIFO), omita lotes caducados, calcule el costo real consumido de forma inmutable y actualice la fecha de retiro sanitario del galpón.

**Why this priority**: Es la labor clínica central que garantiza el tratamiento de las aves, descuenta el stock exacto en unidades clínicas y asegura la imputación financiera al galpón.

**Independent Test**: En un galpón con aves activas, registrar la aplicación de 600 ml de un medicamento que posee 2 lotes en stock (Lote 1: vence en 20 días, saldo 500 ml; Lote 2: vence en 60 días, saldo 1000 ml). Verificar que el FIFO agote los 500 ml del Lote 1 y tome 100 ml del Lote 2, calculando el costo exacto y actualizando el período de retiro.

**Acceptance Scenarios**:

1. **Scenario**: Aplicación clínica directa desde Farmacia Central con FIFO de un solo lote
   - **Given** que el usuario tiene rol exclusivo de Veterinario y la Farmacia Central tiene 2,000 ml del Lote A (vence en 30 días, costo $100/ml)
   - **When** el veterinario registra la aplicación de 300 ml para el Galpón 4
   - **Then** el sistema descuenta los 300 ml del Lote A, calcula un costo consumido inmutable de $30,000 imputado al Galpón 4 y actualiza el saldo del lote a 1,700 ml.

2. **Scenario**: Aplicación clínica que consume de múltiples lotes por FIFO
   - **Given** que existen 200 ml del Lote A ($100/ml, vence en 15 días) y 800 ml del Lote B ($120/ml, vence en 45 días)
   - **When** el veterinario registra una aplicación de 500 ml para el Galpón 1
   - **Then** el sistema descuenta 200 ml del Lote A ($20,000) y 300 ml del Lote B ($36,000), registrando un costo inmutable consumido de $56,000.

3. **Scenario**: Omisión automática de lote farmacéutico vencido
   - **Given** que en el inventario existe un Lote VENCIDO de 500 ml y un Lote VIGENTE de 1,500 ml
   - **When** el veterinario registra una dosis de 400 ml
   - **Then** el sistema salta y bloquea el lote vencido, descuenta del lote vigente, calcula el costo con el lote vigente y genera una alerta de lote caducado requiriendo baja.

4. **Scenario**: Bloqueo de aplicación por saldo insuficiente (bloqueo de inventario negativo)
   - **Given** que el stock total vigente del medicamento es de 150 ml
   - **When** se intenta registrar una aplicación de 200 ml
   - **Then** el sistema rechaza la transacción de forma atómica impidiendo cualquier saldo negativo.

---

### User Story 4 - Cálculo de Tiempo de Retiro Sanitario y Bloqueo para Sacrificio (Priority: P1)

Como Veterinario y Administrador, quiero que al registrar una medicación se calcule automáticamente la Fecha de Finalización de Retiro Sanitario del galpón, para que el sistema bloquee preventivamente la orden de sacrificio de las aves hasta que el fármaco haya sido completamente eliminado de su organismo.

**Why this priority**: Garantiza el cumplimiento normativo sanitario y la inocuidad alimentaria. El envío de aves con trazas de antibióticos/medicamentos a matadero conlleva clausuras legales y riesgos para la salud pública.

**Independent Test**: Aplicar un medicamento con 7 días de retiro en el Galpón 1 el día 10. Intentar ejecutar el caso de uso "Validar apto para sacrificio" o "Ordenar sacrificio" en el Galpón 1 el día 14 (debe ser bloqueado). Probar nuevamente el día 18 (debe ser permitido).

**Acceptance Scenarios**:

1. **Scenario**: Activación de período de retiro tras aplicación médica
   - **Given** que el Galpón 2 recibe una aplicación del antibiótico "Tilosina" con tiempo de retiro de 5 días en la fecha 2026-09-01
   - **When** se completa el registro de la medicación
   - **Then** el sistema establece la *Fecha de Fin de Retiro Sanitario* del Galpón 2 en 2026-09-06 y marca al galpón en estado "En Período de Retiro Sanitario".

2. **Scenario**: Bloqueo de aptitud para sacrificio durante período de retiro vigente
   - **Given** que el Galpón 2 tiene fecha de fin de retiro vigente hasta el 2026-09-06 y la fecha actual es 2026-09-03
   - **When** el Administrador intenta ejecutar la acción "Validar apto para sacrificio" u "Ordenar sacrificio" sobre el Galpón 2
   - **Then** el sistema bloquea la acción, rechaza la autorización de sacrificio y emite una alerta sanitaria indicando los días restantes de carencia.

3. **Scenario**: Habilitación automática para sacrificio al finalizar el período de retiro
   - **Given** que la fecha actual es 2026-09-07 y el período de retiro del Galpón 2 finalizó el 2026-09-06
   - **When** el Administrador valida el galpón para sacrificio
   - **Then** el sistema confirma que el galpón está libre de restricciones farmacológicas y permite continuar con el flujo de faena.

---

### User Story 5 - Traslado de Medicamentos de Farmacia Central a Galpón (Priority: P2)

Como Encargado de Farmacia o Veterinario, quiero trasladar envases de medicamentos de administración masiva (ej. solubles en agua o premezclas) hacia la sub-bodega de un Galpón, para que el galpón disponga del stock físico necesario durante tratamientos continuos de varios días.

**Why this priority**: Facilita el modelo híbrido donde ciertos fármacos masivos requieren custodia local en el galpón.

**Independent Test**: Trasladar 2 bolsas de electrolitos desde Farmacia Central al Galpón 3; verificar que se descuenten de Farmacia Central y se acrediten al stock del Galpón 3 conservando lote y caducidad.

**Acceptance Scenarios**:

1. **Scenario**: Traslado exitoso a sub-bodega de galpón
   - **Given** que existen 10 bolsas del Lote M-1 en Farmacia Central y el Galpón 3 tiene aves activas
   - **When** el usuario solicita el traslado de 2 bolsas al Galpón 3
   - **Then** el sistema descuenta 2 bolsas de Farmacia Central, las asigna al stock del Galpón 3 y genera el comprobante de movimiento.

---

### User Story 6 - Semáforo de Caducidad de Fármacos y Alertas Preventivas (Priority: P2)

Como Administrador, Veterinario o Trabajador, quiero visualizar el estado de vencimiento de todos los lotes de medicamentos mediante un semáforo visual preventivo, para priorizar su consumo clínico o programar su descarte oportuno.

**Why this priority**: Evita la pérdida económica por medicamentos vencidos en anaquel y previene la ineficacia de tratamientos por principios activos degradados.

**Independent Test**: Consultar el panel de farmacia y validar la clasificación de colores según la fecha de caducidad.

**Acceptance Scenarios**:

1. **Scenario**: Clasificación semafórica de lotes farmacológicos
   - **Given** lotes de medicamentos registrados en Farmacia Central y galpones
   - **When** el usuario consulta el panel de vencimientos de medicamentos
   - **Then** el sistema clasifica cada lote según:
     - 🟢 **Verde**: Más de 30 días restantes para su caducidad.
     - 🟡 **Amarillo (Alerta preventiva)**: Entre 10 y 30 días restantes (ambos inclusive).
     - 🔴 **Rojo (Crítico / Vencido)**: Menos de 10 días restantes ($\le 10$ días) o fecha de vencimiento expirada.

2. **Scenario**: Bloqueo visual de lotes vencidos
   - **Given** un lote de antibiótico con fecha de vencimiento expirada
   - **When** se consulta en el inventario
   - **Then** se muestra con distintivo rojo y estado "Bloqueado para aplicación clínica", impidiendo su selección en nuevas recetas o consumos.

---

### User Story 7 - Kardex Farmacéutico y Auditoría de Tratamientos (Priority: P2)

Como Administrador o Auditor Sanitario, quiero consultar el historial inmutable de movimientos y aplicaciones de medicamentos por galpón, lote y fecha, para auditar los costos de medicación y el historial clínico de cada parvada.

**Why this priority**: Permite la trazabilidad integral requerida por certificaciones sanitarias, auditorías de costos y seguimiento de salud avícola.

**Independent Test**: Filtrar el kardex por Galpón y medicamento, comprobando fecha, hora, veterinario responsable, lote FIFO consumido, dosis aplicada en unidad base y costo monetario imputado.

**Acceptance Scenarios**:

1. **Scenario**: Consulta de auditoría de tratamientos por galpón
   - **Given** aplicaciones registradas previamente en el Galpón 1
   - **When** el usuario genera el reporte de kardex farmacéutico para el Galpón 1
   - **Then** el sistema muestra el detalle cronológico de cada aplicación clínica, lote, dosis, costo inmutable y fecha calculada de retiro sanitario.

---

### User Story 8 - Registro de Bajas y Descarte de Medicamentos Vencidos o Deteriorados (Priority: P3)

Como Administrador, quiero registrar la baja de medicamentos caducados o dañados con justificación obligatoria, para mantener el inventario contable y físico saneado.

**Why this priority**: Asegura que los fármacos descartados salgan del inventario sin registrarse erróneamente como dosis aplicadas a las aves.

**Independent Test**: Dar de baja 1 frasco de medicamento vencido indicando "Caducidad"; verificar la salida de inventario y la exclusión de métricas de consumo de aves.

**Acceptance Scenarios**:

1. **Scenario**: Baja exitosa de medicamento por caducidad
   - **Given** un lote bloqueado en semáforo rojo por caducidad
   - **When** el Administrador registra la baja con motivo "Caducidad de producto"
   - **Then** el sistema descuenta el saldo del lote, asienta la pérdida contable y registra el movimiento de descarte en el historial de auditoría.

---

## Edge Cases

- **Intento de aplicación por personal no veterinario**: Cualquier intento de registrar una aplicación clínica por parte de trabajadores u otros roles sin perfil de Veterinario es rechazado con error de autorización.
- **Intento de registro de medicamento por rol no Administrador**: El catálogo de medicamentos solo puede ser creado o modificado por el Administrador.
- **Galpón vacío o sin aves activas**: El sistema bloquea cualquier intento de registrar una aplicación de medicamento en un galpón que se encuentre desocupado o en descanso sanitario.
- **Medicamento con tiempo de retiro 0 días**: Si el fármaco (ej. complejo vitamínico o electrolito) tiene 0 días de retiro, el sistema no impone restricción ni bloqueo para el sacrificio.
- **Superposición de tiempos de retiro**: Si un galpón recibe dos tratamientos con diferentes tiempos de retiro, la *Fecha de Fin de Retiro Sanitario* del galpón será siempre la fecha más lejana entre los tratamientos activos.
- **Conversión fraccional exacta**: Las dosis aplicadas en fracciones de envase (ej. 75 ml de un frasco de 500 ml = 0.15 envases) deben calcularse con precisión de 2 decimales en costo y en saldo sin generar mermas fantasmas.
- **Concurrencia en descuento FIFO**: Las aplicaciones simultáneas deben bloquearse a nivel de transacción para asegurar que dos veterinarios no descuenten el mismo remanente de lote simultáneamente.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir de forma exclusiva al **Administrador** la creación, edición, consulta y desactivación de **Medicamentos** en el catálogo general.
- **FR-002**: El registro de un medicamento DEBE contener: nombre comercial, principio activo, vía de administración (Agua, Alimento, Inyectable, Tópico/Ocular), presentación comercial (nombre del envase), unidad base ($ml$, $g$, dosis/unidades), factor de contenido neto en unidad base y **Días de Retiro Sanitario** ($\ge 0$).
- **FR-003**: El sistema DEBE permitir registrar **Ingresos de Lotes de Medicamentos a Farmacia Central**, capturando: medicamento, código de lote, fecha de fabricación (opcional), fecha de vencimiento obligatoria, cantidad de envases/presentaciones y costo unitario de adquisición por envase.
- **FR-004**: El sistema DEBE calcular automáticamente el costo unitario por unidad base dividiendo el costo del envase entre el factor de contenido neto (`CostoUnidadBase = CostoEnvase / ContenidoNeto`).
- **FR-005**: El sistema DEBE rechazar cualquier ingreso de lote de medicamento cuya fecha de vencimiento sea menor o igual a la fecha actual del sistema.
- **FR-006**: El sistema DEBE soportar un **Modelo de Inventario Híbrido**, permitiendo:
  - Aplicación clínica directa desde Farmacia Central imputando el costo al Galpón.
  - Traslado previo de lotes de Farmacia Central a la sub-bodega del Galpón para tratamientos continuos.
- **FR-007**: El sistema DEBE permitir de forma exclusiva al **Veterinario** el registro de **Aplicaciones / Consumo Clínico de Medicamentos** en los galpones.
- **FR-008**: El sistema DEBE implementar un **Algoritmo FIFO Automático** para las aplicaciones de medicamentos, descontando primero del lote con la fecha de vencimiento más próxima disponible.
- **FR-009**: El algoritmo FIFO DEBE **omitir automáticamente cualquier lote de medicamento caducado** a la fecha de la aplicación, tomando del siguiente lote vigente y generando una alerta de lote vencido.
- **FR-010**: El sistema DEBE calcular el **Costo Real Consumido** multiplicando la dosis aplicada por el costo unitario de los lotes descontados mediante FIFO, almacenando este valor de forma **inmutable**.
- **FR-011**: El sistema DEBE bloquear cualquier aplicación o traslado que resulte en un saldo negativo de inventario (bloqueo atómico contra inventario negativo).
- **FR-012**: Al registrar una aplicación de medicamento con días de retiro $> 0$, el sistema DEBE calcular y actualizar la **Fecha de Fin de Retiro Sanitario** del galpón (`FechaAplicación + DíasRetiro`).
- **FR-013**: Si existen múltiples aplicaciones con tiempos de retiro solapados, el sistema DEBE fijar como fecha límite de retiro del galpón la fecha más tardía (`MAX(FechaFinRetiro)`).
- **FR-014**: El sistema DEBE **bloquear de forma estricta las acciones de "Validar apto para sacrificio" y "Ordenar sacrificio"** sobre un galpón si la fecha actual es menor o igual a su Fecha de Fin de Retiro Sanitario activa.
- **FR-015**: El sistema DEBE clasificar dinámicamente el estado de caducidad de los lotes farmacéuticos según un **Semáforo Preventivo**:
  - 🟢 **Verde**: Fecha de vencimiento $> 30$ días.
  - 🟡 **Amarillo**: Fecha de vencimiento entre $10$ y $30$ días (ambos inclusive).
  - 🔴 **Rojo**: Fecha de vencimiento $< 10$ días o ya caducado.
- **FR-016**: El sistema DEBE proveer un panel de **Alertas Preventivas Farmacéuticas** que liste los lotes en amarillo/rojo y los galpones actualmente en período de retiro sanitario.
- **FR-017**: El sistema DEBE mantener un **Kardex / Historial de Movimientos Farmacéuticos Inmutable** que registre cada ingreso, traslado, aplicación clínica y baja con su respectivo responsable, lote, dosis, costo y saldos resultantes.
- **FR-018**: El sistema DEBE permitir al Administrador registrar **Bajas y Descarte de Medicamentos**, requiriendo justificación obligatoria y descontando el stock sin afectar las métricas de consumo de los animales.

---

### Key Entities *(conceptual)*

- **Medicamento**: Define el fármaco en catálogo. Contiene nombre, principio activo, vía de administración, presentación comercial, unidad base clínica ($ml$, $g$, unidad), factor de conversión y días de retiro sanitario.
- **LoteMedicamento**: Partida específica de medicamento. Contiene código de lote, fecha de vencimiento, costo de compra por envase, costo por unidad base y medicamento asociado.
- **StockFarmaciaCentral**: Existencias físicas disponibles de cada lote en la Farmacia Central (envases y unidades base equivalentes).
- **StockMedicamentoGalpon**: Existencias físicas de medicamentos trasladados a la sub-bodega de un galpón.
- **AplicacionMedicamento**: Registro de la aplicación clínica realizada por el Veterinario. Contiene fecha, galpón tratado, medicamento, dosis total aplicada en unidad base, desglose de lotes FIFO descontados, costo real inmutable y fecha calculada de fin de retiro.
- **RetiroSanitarioGalpon**: Registro de la restricción sanitaria activa de un galpón. Contiene fecha de inicio, fecha de finalización de retiro y medicamentos causantes del bloqueo.
- **MovimientoFarmacia**: Registro inmutable de auditoría para cada transacción (INGRESO_COMPRA, TRASLADO_GALPON, APLICACION_CLINICA, BAJA_DESCARTE).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: **Cero sacrificios con trazas de medicamentos (100% de cumplimiento sanitario)**: El 100% de los intentos de validar o autorizar sacrificio en galpones con período de retiro vigente son bloqueados por el sistema.
- **SC-002**: **Cero inventarios negativos**: El 100% de las aplicaciones, consumos o traslados que excedan el stock disponible en Farmacia Central o Galpón son rechazados atómicamente.
- **SC-003**: **Cero aplicaciones de medicamentos vencidos**: El 100% de las aplicaciones clínicas consumen exclusivamente de lotes vigentes gracias al desvío automático FIFO.
- **SC-004**: **Exactitud en costos clínicos (100%)**: La sumatoria del costo de los lotes consumidos coincide exactamente con el valor imputado al galpón en el Kardex de forma inmutable.
- **SC-005**: **Trazabilidad clínica total**: El 100% de las aplicaciones registradas identifican al veterinario responsable, el galpón tratado, el lote farmacéutico y la fecha de vencimiento del insumo.
- **SC-006**: **Anticipación en alertas de vencimiento**: El 100% de los lotes de fármacos con caducidad $\le 30$ y $\le 10$ días se visualizan oportunamente en el panel semafórico preventivo.
