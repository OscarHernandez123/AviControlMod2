# Feature Specification: Registrar Medicación (CU-VET-007)

**Created**: 2026-09-03  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Prescripción y Registro Clínico de Medicación (Priority: P1)

Como Médico Veterinario autorizado en la granja, quiero prescribir y registrar un esquema de medicación terapéutico o profiláctico para un lote alojado en un galpón, indicando el fármaco, dosis, vía de administración, frecuencia y duración, para combatir cuadros infecciosos o carenciales y asegurar la captura inmutable del tiempo de retiro que garantice la inocuidad alimentaria.

**Why this priority**: Es la función terapéutica fundamental del Veterinario; sin esta capacidad, no existe trazabilidad toxicológica de los fármacos aplicados a las aves, impidiendo calcular los tiempos de carencia y provocando decomisos o riesgos para la salud pública.

**Independent Test**: Se valida enviando el comando de registro de medicación con un fármaco activo del catálogo, dosis mayor a cero y duración definida sobre un galpón con lote activo. Se verifica que se cree la entidad `Tratamiento` en estado `EN_CURSO`, se congele el snapshot del tiempo de retiro base en días, se calcule la fecha mínima de liberación[cite: 1], se emita `TratamientoRegistrado`[cite: 1] y se registre la auditoría inmutable[cite: 1].

**Acceptance Scenarios**:

1. **Scenario**: Registro exitoso de prescripción farmacológica
   - **Given** un Galpón "G-01" en Granja "GR-001" con Lote "L-100" activo y población viva mayor a 0 aves[cite: 1]
   - **And** un medicamento "MED-001" (Enrofloxacina) activo en el catálogo con tiempo de retiro base de 5 días[cite: 1]
   - **When** el Veterinario prescribe el tratamiento con dosis de 10.5 mg/kg, vía "AGUA_BEBIDA", frecuencia cada 24 horas, duración de 5 días y fecha de inicio "2026-09-03T08:00:00Z"[cite: 1]
   - **Then** el sistema persiste el tratamiento con estado inicial "EN_CURSO"[cite: 1]
   - **And** almacena una instantánea inmutable de 5 días como tiempo de retiro aplicado[cite: 1]
   - **And** fija la fecha mínima de liberación para dicho tratamiento en "2026-09-13T08:00:00Z" (fecha inicio + 5 días duración + 5 días retiro)[cite: 1]
   - **And** emite el evento de dominio "TratamientoRegistrado"[cite: 1]
   - **And** genera una entrada inmutable en "san_auditoria"[cite: 1]
   - **And** responde con código HTTP 201 Created con el payload del tratamiento

2. **Scenario**: Rechazo de prescripción con medicamento inactivo en el catálogo
   - **Given** un medicamento "MED-999" registrado con estado "INACTIVO" en el catálogo farmacológico[cite: 1]
   - **When** el Veterinario intenta seleccionarlo para prescribir una medicación en un galpón[cite: 1]
   - **Then** el sistema aborta la transacción[cite: 1]
   - **And** retorna el código de error "VET-006: MEDICAMENTO_INACTIVO" con código HTTP 422[cite: 1]
   - **And** no crea ningún tratamiento ni altera el calendario del lote[cite: 1]

---

### User Story 2 - Suspensión Terapéutica y Recálculo Inmediato de Retiro (Priority: P2)

Como Médico Veterinario, quiero suspender anticipadamente un tratamiento medicamentoso ante reacciones adversas, ineficacia terapéutica o recuperación biológica prematura[cite: 1], para detener la administración del producto y ordenar el recálculo estricto de la fecha de liberación a partir de la última dosis administrada[cite: 1].

**Why this priority**: Evita la sobremedicación del lote y ajusta con exactitud matemática el periodo de carencia real[cite: 1], asegurando que no se retenga innecesariamente a las aves si la última aplicación ocurrió antes de lo programado[cite: 1].

**Independent Test**: Se suspende un tratamiento activo en el día 2 de los 5 inicialmente pautados[cite: 1]. Se comprueba que el estado cambie a `SUSPENDIDO`, que se registre la justificación médica y que la fecha de liberación se recompute tomando la fecha efectiva de suspensión más los días de carencia del snapshot[cite: 1].

**Acceptance Scenarios**:

1. **Scenario**: Suspensión justificada de tratamiento en curso
   - **Given** un tratamiento "TRAT-100" activo programado originalmente por 7 días con 4 días de retiro (liberación prevista al día 11)[cite: 1]
   - **When** el Veterinario suspende el tratamiento en el día 3 a las "10:00:00Z" justificando "Intolerancia digestiva en las aves"[cite: 1]
   - **Then** el estado del tratamiento "TRAT-100" cambia a "SUSPENDIDO"[cite: 1]
   - **And** el sistema fija la nueva fecha fin efectiva en el momento de la suspensión[cite: 1]
   - **And** recalcula la fecha de liberación sumando los 4 días de carencia a la fecha de suspensión[cite: 1]
   - **And** emite el evento de dominio "TratamientoSuspendido" y "TiempoRetiroCalculado"[cite: 1]
   - **And** registra el motivo clínico de suspensión en auditoría[cite: 1]

---

### User Story 3 - Integridad Dosimétrica, RBAC y Aislamiento Multi-Tenant (Priority: P3)

Como Auditor de Calidad y Farmacovigilancia, quiero asegurar que solo médicos veterinarios autorizados puedan prescribir medicamentos, validando que las magnitudes de dosis sean estrictamente positivas y limitando las prescripciones a galpones de la granja asignada[cite: 1].

**Why this priority**: Evita la prescripción no calificada por parte de personal sin credenciales médicas y elimina riesgos toxicológicos por errores de captura en magnitudes numéricas o accesos entre granjas[cite: 1].

**Independent Test**: Intentar registrar una medicación con dosis menor o igual a cero, con usuarios que no posean rol Veterinario o cruzando identificadores de granjas distintas[cite: 1].

**Acceptance Scenarios**:

1. **Scenario**: Rechazo de prescripción con dosis inválida
   - **Given** un Veterinario autenticado en la Granja "GR-001"[cite: 1]
   - **When** intenta enviar una prescripción con dosisCantidad = 0 o dosisCantidad = -5[cite: 1]
   - **Then** el sistema rechaza la solicitud con código HTTP 422[cite: 1]
   - **And** retorna el error "VET-007: TRATAMIENTO_PARAMETROS_INVALIDOS" indicando que la dosis debe ser estrictamente mayor a cero[cite: 1]

2. **Scenario**: Rechazo de prescripción por usuario sin rol Veterinario
   - **Given** un usuario autenticado con rol "TRABAJADOR" o "NUTRICIONISTA"[cite: 1]
   - **When** intenta invocar el servicio de registro de medicación[cite: 1]
   - **Then** el sistema deniega el acceso respondiendo HTTP 403 Forbidden[cite: 1]
   - **And** retorna el error "VET-008: USUARIO_NO_AUTORIZADO"[cite: 1]

3. **Scenario**: Rechazo por intento de prescripción cross-tenant
   - **Given** un galpón perteneciente a la Granja "GR-002"[cite: 1]
   - **When** un Veterinario con credenciales exclusivas en Granja "GR-001" intenta prescribir un tratamiento[cite: 1]
   - **Then** el sistema bloquea la transacción con código HTTP 403 Forbidden[cite: 1]
   - **And** retorna el error "VET-009: GRANJA_NO_AUTORIZADA"[cite: 1]

---

### Edge Cases

- **Prescripción simultánea de múltiples fármacos:** Si se registran dos o más medicamentos en la misma fecha para el lote, cada uno se modela como un Aggregate Root `Tratamiento` independiente; no se suman linealmente los días de carencia, sino que CU-VET-004 consolidará la fecha de liberación tomando el valor máximo más tardío[cite: 1].
- **Modificación posterior del retiro en catálogo:** Si el Administrador actualiza la ficha técnica del medicamento reduciendo o incrementando los días de retiro de referencia, los tratamientos ya registrados conservan inalterado su snapshot original, protegiendo la trazabilidad histórica[cite: 1].
- **Lote con población reducida a cero durante el tratamiento:** Si ocurre mortalidad total o sacrificio sanitario antes de concluir el cronograma, el tratamiento se suspende de forma automática en cascada y se cancela la proyección de liberación[cite: 1].
- **Duplicidad de envío por caída de red:** Si el cliente reenvía la misma prescripción debido a fallas de conexión, el backend valida la cabecera `X-Idempotency-Key`, evitando registrar dos tratamientos idénticos simultáneos[cite: 1].

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema debe permitir al Veterinario prescribir medicamentos registrando lote, galpón, fármaco, dosis, unidad, frecuencia, vía de administración, duración y fecha de inicio[cite: 1].
- **FR-002**: El sistema debe exigir que el usuario cuente con rol `VETERINARIO`, tarjeta profesional válida y pertenezca a la `granjaId` del lote a tratar[cite: 1].
- **FR-003**: El sistema debe comprobar que el lote esté en estado `ACTIVO` y con población viva mayor a 0 aves antes de permitir la prescripción (RN-VET-002)[cite: 1].
- **FR-004**: El sistema debe validar que el medicamento seleccionado exista y se encuentre en estado `ACTIVO` en el catálogo farmacológico (RN-VET-011)[cite: 1].
- **FR-005**: El sistema debe validar que la cantidad de dosis prescrita sea estrictamente mayor a 0 y con unidad de medida farmacológica soportada[cite: 1].
- **FR-006**: El sistema debe congelar una instantánea inmutable del `tiempo_retiro_dias` del medicamento en la entidad `Tratamiento` al momento de su creación (RN-VET-004)[cite: 1].
- **FR-007**: El sistema debe calcular algorítmicamente la fecha mínima de liberación específica del tratamiento: $\text{fechaLiberacion} = \text{fechaFin} + \text{tiempoRetiroAplicadoDias}$[cite: 1].
- **FR-008**: Al persistir la prescripción, el sistema debe asignar el estado `EN_CURSO` al tratamiento y publicar el evento de dominio `TratamientoRegistrado`[cite: 1].
- **FR-009**: El sistema debe permitir la suspensión anticipada del tratamiento mediante comando explícito, capturando la fecha efectiva, el motivo clínico y recalculando el retiro[cite: 1].
- **FR-010**: El sistema debe registrar una entrada inmutable append-only en `san_auditoria` con el detalle completo de cada prescripción o suspensión[cite: 1].
- **FR-011**: El sistema debe prohibir el borrado físico (`DELETE` relacional) de los tratamientos administrados al lote[cite: 1].

### Key Entities

- **Tratamiento** *(Aggregate Root Terapéutico)*: Representa el esquema farmacológico prescrito[cite: 1]. Atributos: `id` (UUID), `granjaId` (UUID), `galponId` (UUID), `loteId` (UUID), `medicamentoId` (UUID), `veterinarioId` (UUID), `dosisCantidad` (Decimal), `dosisUnidad` (String), `frecuenciaHoras` (Integer), `viaAdministracion` (String), `fechaInicio` (Timestamp), `fechaFin` (Timestamp), `duracionDias` (Integer), `tiempoRetiroAplicadoDias` (Integer snapshot), `fechaMinimaLiberacion` (Timestamp), `estado` (`PRESCRITO`, `EN_CURSO`, `SUSPENDIDO`, `FINALIZADO`) y `version` (Optimistic locking)[cite: 1].
- **Medicamento** *(Entidad de Catálogo)*: Producto farmacológico de referencia[cite: 1]. Atributos: `id`, `granjaId`, `codigo`, `nombreComercial`, `principioActivo`, `unidadDosis`, `tiempoRetiroDiasBase` y `activo`[cite: 1].
- **Dosis** *(Value Object)*: Encapsula la magnitud y unidad técnica (ej. mg/kg peso vivo, ml/litro de agua)[cite: 1].
- **Auditoria (`san_auditoria`)**: Registro inmutable append-only de trazabilidad farmacológica y auditorías toxicológicas[cite: 1].

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los tratamientos registrados congelan su snapshot de tiempo de retiro, permaneciendo inalterables ante variaciones posteriores del catálogo farmacológico[cite: 1].
- **SC-002**: Cero por ciento (0%) de prescripciones admitidas con dosis negativas o iguales a cero en las pruebas unitarias y de integración de dominio[cite: 1].
- **SC-003**: El recálculo de la fecha mínima de liberación ante suspensiones de medicación se ejecuta y persiste en menos de 300 milisegundos tras invocar el comando[cite: 1].
- **SC-004**: El tiempo de respuesta para el registro y consulta de tratamientos es inferior a 350 milisegundos en al menos el 95% de las solicitudes atendidas bajo condiciones normales de operación.
- **SC-005**: 100% de trazabilidad toxicológica: El total de medicamentos prescritos quedan vinculados a la matrícula profesional del veterinario en `san_auditoria` con su respectivo `correlationId`[cite: 1].