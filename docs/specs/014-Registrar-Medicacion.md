# Feature Specification: Registrar Medicación (CU-VET-007)

**Created**: 2026-09-03  
**Updated**: 2026-09-03 (Desacoplamiento de Códigos HTTP y Factor Dosimétrico Post-Diagnóstico)  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Prescripción y Registro Clínico de Medicación (Priority: P1)

Como Médico Veterinario autorizado en la granja, quiero prescribir y registrar un esquema de medicación terapéutico o profiláctico para un lote alojado en un galpón[cite: 8], indicando el fármaco, dosis, vía de administración, frecuencia y duración[cite: 8], para combatir cuadros infecciosos o carenciales, congelar la instantánea inmutable del tiempo de retiro y calcular el consumo proyectado en las unidades base del inventario[cite: 2, 8].

**Why this priority**: Es la función terapéutica fundamental del Veterinario[cite: 8]; sin esta capacidad, no existe trazabilidad toxicológica de los fármacos aplicados a las aves, impidiendo calcular los tiempos de carencia y provocando decomisos o riesgos para la salud pública[cite: 8].

**Independent Test**: Se valida enviando el comando de registro de medicación con un fármaco activo del catálogo, dosis mayor a cero y duración definida sobre un galpón con lote activo[cite: 8]. Se verifica que se cree la entidad `Tratamiento` en estado `EN_CURSO`[cite: 8], se congele el snapshot del tiempo de retiro base en días[cite: 8], se calcule la fecha mínima de liberación[cite: 8], se proyecte el consumo a la unidad base de bodega (`gr`, `ml` o `unidad`)[cite: 2], se emita `TratamientoRegistrado`[cite: 8] y se registre la auditoría inmutable[cite: 8].

**Acceptance Scenarios**:

1. **Scenario**: Registro exitoso de prescripción farmacológica y conversión de consumo
   - **Given** un Galpón "G-01" en Granja "GR-001" con Lote "L-100" activo y población viva de 10000 aves[cite: 8]
   - **And** un medicamento "MED-001" (Enrofloxacina 10%) activo en el catálogo con unidad base en "ml" y tiempo de retiro base de 5 días[cite: 2, 8]
   - **When** el Veterinario prescribe el tratamiento con dosis de 0.1 ml/ave/día, vía "AGUA_BEBIDA", frecuencia cada 24 horas, duración de 5 días y fecha de inicio "2026-09-03T08:00:00Z"[cite: 8]
   - **Then** el sistema persiste el tratamiento con estado inicial "EN_CURSO"[cite: 8]
   - **And** almacena una instantánea inmutable de 5 días como tiempo de retiro aplicado[cite: 8]
   - **And** calcula y registra el consumo teórico total proyectado de 5000 ml en la unidad base de inventario[cite: 2]
   - **And** fija la fecha mínima de liberación en "2026-09-13T08:00:00Z" (inicio + 5 días duración + 5 días retiro)[cite: 8]
   - **And** emite el evento de dominio "TratamientoRegistrado"[cite: 8]
   - **And** genera una entrada inmutable en "san_auditoria"[cite: 8]
   - **And** responde con código HTTP 201 Created con el recurso persistido[cite: 8]

2. **Scenario**: Rechazo de prescripción con medicamento inactivo en el catálogo
   - **Given** un medicamento "MED-999" registrado con estado "INACTIVO" en el catálogo farmacológico[cite: 8]
   - **When** el Veterinario intenta seleccionarlo para prescribir una medicación en un galpón[cite: 8]
   - **Then** el sistema aborta la transacción[cite: 8]
   - **And** retorna el código de error "VET-006: MEDICAMENTO_INACTIVO" con código HTTP 422 Unprocessable Entity[cite: 8]
   - **And** no crea ningún tratamiento ni altera el calendario del lote[cite: 8]

---

### User Story 2 - Suspensión Terapéutica y Recálculo Inmediato de Retiro (Priority: P2)

Como Médico Veterinario, quiero suspender anticipadamente un tratamiento medicamentoso ante reacciones adversas, ineficacia terapéutica o recuperación biológica prematura[cite: 8], para detener la administración del producto y ordenar el recálculo estricto de la fecha de liberación a partir de la última dosis administrada[cite: 8].

**Why this priority**: Evita la sobremedicación del lote y ajusta con exactitud matemática el periodo de carencia real[cite: 8], asegurando que no se retenga innecesariamente a las aves si la última aplicación ocurrió antes de lo programado[cite: 8].

**Independent Test**: Se suspende un tratamiento activo en el día 2 de los 5 inicialmente pautados[cite: 8]. Se comprueba que el estado cambie a `SUSPENDIDO`, que se registre la justificación médica y que la fecha de liberación se recompute tomando la fecha efectiva de suspensión más los días de carencia del snapshot[cite: 8].

**Acceptance Scenarios**:

1. **Scenario**: Suspensión justificada de tratamiento en curso
   - **Given** un tratamiento "TRAT-100" activo programado originalmente por 7 días con 4 días de retiro (liberación prevista al día 11)[cite: 8]
   - **When** el Veterinario suspende el tratamiento en el día 3 a las "10:00:00Z" justificando "Intolerancia digestiva en las aves"[cite: 8]
   - **Then** el estado del tratamiento "TRAT-100" cambia a "SUSPENDIDO"[cite: 8]
   - **And** el sistema fija la nueva fecha fin efectiva en el momento exacto de la suspensión[cite: 8]
   - **And** recalcula la fecha de liberación sumando los 4 días de carencia a la fecha de suspensión[cite: 8]
   - **And** emite los eventos de dominio "TratamientoSuspendido" y "TiempoRetiroCalculado"[cite: 8]
   - **And** registra el motivo clínico de suspensión en auditoría[cite: 8]

---

### User Story 3 - Integridad Dosimétrica, RBAC y Aislamiento Multi-Tenant (Priority: P3)

Como Auditor de Calidad y Farmacovigilancia, quiero asegurar que solo médicos veterinarios autorizados puedan prescribir medicamentos, diferenciando errores de formato sintáctico de fallas en las reglas biológicas, y limitando las prescripciones a galpones de la granja asignada[cite: 8].

**Why this priority**: Evita la prescripción no calificada por parte de personal sin credenciales médicas y garantiza un manejo de errores estandarizado que no enmascare errores de contrato con excepciones de dominio[cite: 8].

**Independent Test**: Intentar registrar una medicación con tipos de datos inválidos en el DTO (HTTP 400), con usuarios sin rol Veterinario (HTTP 403) o enviando tratamientos con carencias que colisionen con las fechas biológicas del lote (HTTP 422)[cite: 8].

**Acceptance Scenarios**:

1. **Scenario**: Rechazo sintáctico por payload corrupto o dosis no positiva
   - **Given** un Veterinario autenticado en la Granja "GR-001"[cite: 8]
   - **When** envía una solicitud con dosisCantidad = 0, dosisCantidad negativa o campo "fármacoId" omitido[cite: 8]
   - **Then** el framework intercepta el fallo en la capa de validación de esquema
   - **And** rechaza la solicitud con código HTTP 400 Bad Request y formato RFC 7807 sin invocar la capa de dominio

2. **Scenario**: Rechazo semántico por violación de reglas biológicas del lote
   - **Given** un lote cuya fecha de liquidación productiva está programada para dentro de 3 días
   - **When** el Veterinario intenta prescribir un tratamiento cuya duración más tiempo de retiro exige 10 días de permanencia
   - **Then** el Aggregate Root detecta la inconsistencia del ciclo biológico
   - **And** rechaza la operación respondiendo HTTP 422 Unprocessable Entity con el código "VET-016: RETIRO_EXCEDE_CICLO_LOTE"

3. **Scenario**: Rechazo de prescripción por usuario sin rol Veterinario
   - **Given** un usuario autenticado con rol "TRABAJADOR" o "NUTRICIONISTA"[cite: 8]
   - **When** intenta invocar el servicio de registro de medicación[cite: 8]
   - **Then** el sistema deniega el acceso respondiendo HTTP 403 Forbidden[cite: 8]
   - **And** retorna el error "VET-008: USUARIO_NO_AUTORIZADO"[cite: 8]

4. **Scenario**: Rechazo por intento de prescripción cross-tenant
   - **Given** un galpón perteneciente a la Granja "GR-002"[cite: 8]
   - **When** un Veterinario con credenciales exclusivas en Granja "GR-001" intenta prescribir un tratamiento[cite: 8]
   - **Then** el sistema bloquea la transacción con código HTTP 403 Forbidden[cite: 8]
   - **And** retorna el error "VET-009: GRANJA_NO_AUTORIZADA"[cite: 8]

---

### Edge Cases

- **Prescripción simultánea de múltiples fármacos:** Si se registran dos o más medicamentos en la misma fecha para el lote, cada uno se modela como un Aggregate Root `Tratamiento` independiente; no se suman linealmente los días de carencia, sino que CU-VET-004 consolidará la fecha de liberación tomando el valor máximo más tardío[cite: 8].
- **Modificación posterior del retiro en catálogo:** Si el Administrador actualiza la ficha técnica del medicamento reduciendo o incrementando los días de retiro de referencia, los tratamientos ya registrados conservan inalterado su snapshot original, protegiendo la trazabilidad histórica[cite: 8].
- **Lote con población reducida a cero durante el tratamiento:** Si ocurre mortalidad total o sacrificio sanitario antes de concluir el cronograma, el tratamiento se suspende de forma automática en cascada y se cancela la proyección de liberación[cite: 8].
- **Reintento de red con idempotencia:** Si el cliente reenvía la misma prescripción debido a fallas de conexión utilizando la misma `X-Idempotency-Key`, el sistema retorna la respuesta almacenada con HTTP 200 OK y la cabecera `Idempotent-Replayed: true`[cite: 8].

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema debe permitir al Veterinario prescribir medicamentos registrando lote, galpón, fármaco, dosis, unidad, frecuencia, vía de administración, duración y fecha de inicio[cite: 8].
- **FR-002**: El sistema debe exigir que el usuario cuente con rol `VETERINARIO`, tarjeta profesional válida y pertenezca a la `granjaId` del lote a tratar[cite: 8].
- **FR-003**: El sistema debe comprobar que el lote esté en estado `ACTIVO` y con población viva mayor a 0 aves antes de permitir la prescripción (RN-VET-002)[cite: 8].
- **FR-004**: El sistema debe validar que el medicamento seleccionado exista y se encuentre en estado `ACTIVO` en el catálogo farmacológico (RN-VET-011)[cite: 8].
- **FR-005**: El sistema debe validar en la capa de interfaz que los parámetros numéricos de entrada sean sintácticamente válidos (dosis > 0, duración $\ge$ 1), retornando HTTP 400 Bad Request ante inconsistencias de contrato.
- **FR-006**: El sistema debe congelar una instantánea inmutable del `tiempo_retiro_dias` del medicamento en la entidad `Tratamiento` al momento de su creación (RN-VET-004)[cite: 8].
- **FR-007**: El sistema debe calcular algorítmicamente la fecha mínima de liberación específica del tratamiento: $\text{fechaLiberacion} = \text{fechaFin} + \text{tiempoRetiroAplicadoDias}$[cite: 8].
- **FR-008**: Al persistir la prescripción, el sistema debe asignar el estado `EN_CURSO` al tratamiento y publicar el evento de dominio `TratamientoRegistrado`[cite: 8].
- **FR-009**: El sistema debe permitir la suspensión anticipada del tratamiento mediante comando explícito, capturando la fecha efectiva, el motivo clínico y recalculando el retiro[cite: 8].
- **FR-010**: El sistema debe registrar una entrada inmutable append-only en `san_auditoria` con el detalle completo de cada prescripción o suspensión[cite: 8].
- **FR-011**: El sistema debe prohibir el borrado físico (`DELETE` relacional) de los tratamientos administrados al lote[cite: 8].
- **FR-012**: El sistema debe calcular y registrar el consumo total teórico proyectado convirtiendo la dosis prescrita a la unidad base estandarizada del medicamento (`gr`, `ml` o `unidad`) definida en la presentación de bodega central (`002-RegistroMedicamento.md`)[cite: 2].
- **FR-013**: Cuando se reciba una solicitud con una `X-Idempotency-Key` ya procesada y payload idéntico, el sistema debe retornar el payload almacenado con código HTTP 200 OK y la cabecera `Idempotent-Replayed: true` sin duplicar el tratamiento.

### Key Entities

- **Tratamiento** *(Aggregate Root Terapéutico)*: Representa el esquema farmacológico prescrito[cite: 8]. Atributos: `id` (UUID), `granjaId` (UUID), `galponId` (UUID), `loteId` (UUID), `medicamentoId` (UUID), `veterinarioId` (UUID), `dosis` (Value Object), `frecuenciaHoras` (Integer), `viaAdministracion` (String), `fechaInicio` (Timestamp), `fechaFin` (Timestamp), `duracionDias` (Integer), `tiempoRetiroAplicadoDias` (Integer snapshot), `fechaMinimaLiberacion` (Timestamp), `consumoProyectadoUnidadBase` (Decimal), `unidadBase` (`gr`, `ml`, `unidad`), `estado` (`EN_CURSO`, `SUSPENDIDO`, `FINALIZADO`) y `version` (Optimistic locking)[cite: 2, 8].
- **Medicamento** *(Entidad de Catálogo)*: Producto farmacológico de referencia estructurado conforme a la recepción en bodega central[cite: 2, 8]. Atributos: `id`, `granjaId` (nullable), `codigo`, `nombreComercial`, `principioActivo`, `presentacionId`, `unidadBase` (`gr`, `ml`, `unidad`), `tiempoRetiroDiasBase` y `activo`[cite: 2, 8].
- **Dosis** *(Value Object)*: Encapsula la magnitud y la unidad técnica de dosificación clínica (ej. `mg/kg peso vivo`, `ml/litro de agua`)[cite: 8].
- **Auditoria (`san_auditoria`)**: Registro inmutable append-only de trazabilidad farmacológica y auditorías toxicológicas[cite: 8].

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los tratamientos registrados congelan su snapshot de tiempo de retiro, permaneciendo inalterables ante variaciones posteriores del catálogo farmacológico[cite: 8].
- **SC-002**: Cero por ciento (0%) de prescripciones admitidas con datos sintácticamente incorrectos (dosis $\le 0$, campos nulos), garantizando el rechazo temprano con HTTP 400 Bad Request.
- **SC-003**: El 100% de las prescripciones calculan de forma exacta el consumo total proyectado en la unidad base correspondiente (`gr`, `ml`, `unidad`) para su conciliación con inventario[cite: 2].
- **SC-004**: El recálculo de la fecha mínima de liberación ante suspensiones de medicación se ejecuta y persiste en menos de 300 milisegundos tras invocar el comando[cite: 8].
- **SC-005**: El tiempo de respuesta para el registro y consulta de tratamientos es inferior a 350 milisegundos en al menos el 95% de las solicitudes atendidas bajo condiciones normales de operación.