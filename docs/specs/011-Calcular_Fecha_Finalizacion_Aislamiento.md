# Feature Specification: Calcular Fecha de Finalización de Aislamiento (CU-VET-004)

**Created**: 2026-09-03  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Determinación Algorítmica de Fecha de Liberación (Priority: P1)

Como Médico Veterinario (o Sistema automatizado), quiero calcular la fecha mínima y definitiva de finalización del aislamiento sanitario consolidando la fecha prevista de cuarentena y los tiempos de retiro toxicológico de todos los tratamientos administrados al lote[cite: 1], para garantizar que ningún galpón sea liberado o cosechado con trazas farmacológicas activas o riesgo de contagio[cite: 1].

**Why this priority**: Es la regla matemática e inmunológica central de la inocuidad alimentaria[cite: 1]; sin este cálculo unificado, existe riesgo de enviar aves con residuos de fármacos a la planta de faena o de levantar aislamientos prematuramente[cite: 1].

**Independent Test**: Se valida ejecutando el cálculo sobre un galpón con fecha fin de aislamiento clínico definida y múltiples tratamientos con distintos periodos de retiro asociados[cite: 1]. Se constata que la fecha resultante sea estrictamente el valor máximo entre la cuarentena clínica y el vencimiento del retiro más tardío[cite: 1].

**Acceptance Scenarios**:

1. **Scenario**: Prevalencia del retiro farmacológico más tardío
   - **Given** un galpón "G-01" con aislamiento cuya fecha fin estimada de cuarentena es "2026-09-05T08:00:00Z"[cite: 1]
   - **And** registra un tratamiento "TRAT-01" cuyo retiro finaliza el "2026-09-04T18:00:00Z"[cite: 1]
   - **And** registra un tratamiento "TRAT-02" cuyo retiro finaliza el "2026-09-08T18:00:00Z"[cite: 1]
   - **When** el sistema calcula la fecha de finalización y liberación sanitaria[cite: 1]
   - **Then** la fecha mínima de liberación calculada es exactamente "2026-09-08T18:00:00Z"[cite: 1]
   - **And** el galpón se declara bloqueado para certificación y cosecha hasta alcanzar dicha fecha[cite: 1]

2. **Scenario**: Prevalencia de la cuarentena clínica sobre el retiro médico
   - **Given** un galpón "G-02" con aislamiento cuya fecha fin estimada de cuarentena es "2026-09-12T08:00:00Z"[cite: 1]
   - **And** registra un tratamiento cuyo retiro farmacológico finaliza el "2026-09-06T12:00:00Z"[cite: 1]
   - **When** el sistema calcula la fecha de finalización sanitaria[cite: 1]
   - **Then** la fecha mínima de liberación calculada es exactamente "2026-09-12T08:00:00Z"[cite: 1]
   - **And** el indicador de aptitud para reintegro permanece en falso[cite: 1]

---

### User Story 2 - Recálculo Dinámico por Suspensión o Ajuste Terapéutico (Priority: P2)

Como Médico Veterinario, quiero que el sistema recalcule inmediatamente la fecha de liberación cuando se suspenda anticipadamente un tratamiento o se modifique la estimación clínica del aislamiento[cite: 1], para mantener actualizada la ventana real de bioseguridad del lote.

**Why this priority**: En campo ocurren reacciones adversas o recuperaciones clínicas imprevistas; congelar un cálculo estático causaría retenciones innecesarias o errores en la trazabilidad del lote[cite: 1].

**Independent Test**: Se simula la suspensión prematura de un tratamiento activo en el día 2 de 5 programados[cite: 1]. Se verifica que el recálculo tome como punto de partida la fecha de la última dosis real más el tiempo de retiro snapshot, actualizando la fecha mínima de liberación[cite: 1].

**Acceptance Scenarios**:

1. **Scenario**: Recálculo tras suspensión anticipada de medicación
   - **Given** un tratamiento programado hasta el "2026-09-10" con 5 días de retiro (liberación prevista "2026-09-15")[cite: 1]
   - **When** el Veterinario suspende el tratamiento el "2026-09-06T10:00:00Z" por indicación clínica[cite: 1]
   - **Then** el sistema recalcula la liberación sumando los 5 días de retiro a la fecha efectiva de suspensión ("2026-09-11T10:00:00Z")[cite: 1]
   - **And** emite el evento de dominio "TiempoRetiroCalculado"[cite: 1]
   - **And** actualiza la proyección de liberación del aislamiento[cite: 1]

---

### User Story 3 - Consulta Unificada de Inocuidad y Estado de Carencia (Priority: P3)

Como Responsable de Calidad y Cosecha (Módulo 3), quiero consultar en tiempo real el desglose de restricciones que componen la fecha de liberación del galpón[cite: 1], para confirmar si el lote está formalmente habilitado para faena o cuántas horas restan de bloqueo sanitario[cite: 1].

**Why this priority**: Asegura transparencia intermodular entre Sanidad y Liquidación/Venta[cite: 1], evitando que despachos comerciales dependan de consultas manuales fuera del sistema[cite: 1].

**Independent Test**: Invocar la consulta de cálculo de retiro sobre un galpón aislado y verificar que la respuesta contenga el desglose pormenorizado de la cuarentena clínica, los tratamientos activos, el tiempo restante en horas y el flag booleano de aptitud[cite: 1].

**Acceptance Scenarios**:

1. **Scenario**: Consulta de galpón con periodo de retiro pendiente
   - **Given** un galpón con fecha mínima de liberación futura[cite: 1]
   - **When** se consulta el estado de finalización del aislamiento[cite: 1]
   - **Then** el sistema retorna el desglose detallado con tiempo restante en horas[cite: 1]
   - **And** reporta "aptoParaReintegro = false" y "bloqueadoParaCosecha = true"[cite: 1]

---

### Edge Cases

- **Galpón sin tratamientos administrados:** Si el aislamiento fue únicamente por cuarentena preventiva (sin fármacos), la fecha mínima de liberación es idéntica a la fecha estimada de finalización del aislamiento[cite: 1].
- **Tratamiento extendido o dosis adicional:** Si el Veterinario prolonga la duración del tratamiento, la fecha de liberación se desplaza automáticamente recalculando la nueva fecha fin más el tiempo de retiro snapshot[cite: 1].
- **Tratamientos simultáneos con principios activos distintos:** No se suman acumulativamente los días de retiro entre medicamentos independientes; cada uno define su propia fecha de liberación y prevalece la más tardía en el tiempo[cite: 1].
- **Consulta en galpón sin aislamiento activo:** Si se solicita el cálculo sobre un galpón sin medidas sanitarias vigentes, el servicio responde indicando la ausencia de restricciones activas.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema debe calcular la fecha mínima de liberación aplicando la fórmula canónica de dominio:  
  $$\text{FechaMinimaLiberacion} = \max\left(\text{FechaFinAislamientoEstimada}, \max_{i \in T}(\text{FechaFinEfectivaTratamiento}_i + \text{TiempoRetiroAplicadoDias}_i)\right)$$  
  donde $T$ es el conjunto de tratamientos farmacológicos registrados para el lote[cite: 1].
- **FR-002**: El sistema debe ejecutar el recálculo de forma automática cada vez que ocurra un evento `AislamientoConfirmado`, `AislamientoValidado`, `TratamientoRegistrado` o `TratamientoSuspendido`[cite: 1].
- **FR-003**: El sistema debe calcular y exponer el tiempo restante en horas hasta la liberación efectiva del lote[cite: 1].
- **FR-004**: El sistema debe determinar y exponer el indicador booleano `aptoParaReintegro`, el cual solo es verdadero si la fecha actual es mayor o igual a la fecha mínima de liberación[cite: 1].
- **FR-005**: El sistema debe asegurar que el tiempo de retiro en días aplicado para el cálculo provenga del snapshot inmutable registrado en cada tratamiento, ignorando alteraciones posteriores en el catálogo maestro de medicamentos[cite: 1].
- **FR-006**: El sistema debe emitir el evento de dominio `TiempoRetiroCalculado` cada vez que la fecha mínima de liberación cambie de valor[cite: 1].
- **FR-007**: El sistema debe persistir el resultado consolidado del cálculo en la entidad correspondiente y registrar la trazabilidad en auditoría[cite: 1].

### Key Entities

- **CalculadorRetiroSanitarioService** *(Domain Service)*: Servicio de dominio puro encargado de orquestar la agregación matemática de fechas entre el aislamiento clínico y los esquemas terapéuticos[cite: 1].
- **Aislamiento** *(Aggregate Root)*: Aporta la fecha estimada de finalización y el contexto biológico (`granjaId`, `galponId`, `loteId`)[cite: 1].
- **Tratamiento** *(Aggregate Root)*: Aporta la fecha fin, la fecha de suspensión (si aplica) y el Value Object inmutable `TiempoRetiroAplicado`[cite: 1].
- **PeriodoRetiro / TiempoRetiroCalculado** *(Value Object)*: Encapsula la fecha mínima de liberación, el desglose de restricciones componentes y la condición booleana de aptitud sanitaria[cite: 1].
- **Auditoria (`san_auditoria`)**: Registro inmutable append-only de variaciones de fecha de liberación y disparadores de recálculo[cite: 1].

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cero incidencias de autorización de reintegro o cosecha comercial antes de cumplirse el 100% del tiempo de retiro o cuarentena calculada[cite: 1].
- **SC-002**: El 100% de las suspensiones o prórrogas de tratamientos actualizan la fecha mínima de liberación calculada en menos de 500 milisegundos tras persistirse la mutación[cite: 1].
- **SC-003**: El tiempo de respuesta para la consulta de cálculo de retiro y desglose de liberación es inferior a 200 milisegundos en al menos el 95% de las solicitudes atendidas.
- **SC-004**: Exactitud matemática del 100% entre las fechas de última dosis registrada, días de carencia del fármaco y la fecha resultante de liberación[cite: 1].