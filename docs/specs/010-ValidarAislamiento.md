# Feature Specification: Validar Aislamiento

**Created**: 2026-09-01

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Diagnóstico Clínico y Confirmación de Aislamiento (Priority: P1)

Como Médico Veterinario de la granja, quiero evaluar las solicitudes de aislamiento preventivo de campo e incorporar de forma obligatoria el diagnóstico clínico del galpón seleccionando una patología activa del catálogo, para emitir un dictamen formal de confirmación que bloquee de inmediato las operaciones comerciales y de cosecha en el Módulo 3 mientras persista el riesgo biológico.

**Why this priority**: Es la compuerta de bioseguridad primaria de la granja. Si el veterinario no diagnostica y confirma formalmente el aislamiento, el galpón permanece habilitado comercialmente y el Módulo 3 podría programar la faena de aves enfermas para consumo humano.

**Independent Test**: Se prueba seleccionando un galpón con lote activo y aves vivas que registre una solicitud pendiente de revisión. Se ejecuta el diagnóstico y confirmación médica ingresando tarjeta profesional, patología activa y criterio técnico. Se comprueba que el aislamiento pase a `ACTIVO`, el galpón a `AISLADO`, se despache el evento de integración hacia el Módulo 3 y se registre la traza inmutable en auditoría.

**Acceptance Scenarios**:

1. **Scenario**: Diagnóstico y confirmación a partir de una solicitud de campo
   - **Given** que en el galpón "G-01" habita un lote activo con 8.500 pollos vivos comprobados en inventario
   - **And** existe una solicitud de aislamiento preventivo en estado "SOLICITADO"
   - **And** la patología "Bronquitis Infecciosa Aviar" se encuentra registrada y en estado "ACTIVA" en el catálogo nosológico
   - **When** el Veterinario evalúa el galpón, diagnostica la patología y confirma el aislamiento con severidad "CRÍTICO", fecha fin estimada a 7 días, su tarjeta profesional "MP-VET-88492-CO" y la justificación clínica de los síntomas observados
   - **Then** el expediente de aislamiento pasa al estado "ACTIVO"
   - **And** el galpón transiciona a estado "AISLADO"
   - **And** el sistema emite el evento de dominio "AislamientoConfirmado"
   - **And** se despacha el evento de integración "GalponAisladoSanitariamenteIntegrationEvent" para bloquear cosechas en el Módulo 3
   - **And** se registra de manera inmutable el dictamen médico y el diagnóstico en "san_auditoria"

2. **Scenario**: Diagnóstico e inicio directo de aislamiento en visita presencial (sin solicitud previa)
   - **Given** que el Médico Veterinario realiza una inspección presencial en un galpón con lote activo y aves vivas
   - **And** el galpón no cuenta con ninguna solicitud abierta en el sistema
   - **When** el Veterinario identifica signos clínicos patológicos e ingresa directamente el diagnóstico y la confirmación médica
   - **Then** el sistema inicializa el aislamiento, adjunta el dictamen diagnóstico y lo confirma en una sola transacción
   - **And** el galpón queda catalogado inmediatamente como "AISLADO" bajo las mismas restricciones sanitarias

---

### User Story 2 - Desestimación Médica tras Diagnóstico Diferencial (Priority: P2)

Como Médico Veterinario de la granja, quiero rechazar formalmente solicitudes de aislamiento improcedentes tras constatar mediante diagnóstico diferencial que los signos no corresponden a una patología infectocontagiosa, garantizando que el galpón vuelva a su rutina habitual de producción únicamente si no tiene medicamentos administrados que deban cumplir tiempos de retiro farmacológico.

**Why this priority**: Previene paradas operativas innecesarias ante falsas alarmas, garantizando al mismo tiempo que un descarte infeccioso no levante por error un bloqueo toxicológico originado por tratamientos antibióticos activos.

**Independent Test**: Se desestiman solicitudes en galpones libres de medicación (retornan a `En producción`) y en galpones con aves bajo periodo de carencia (el aislamiento se descarta, pero el galpón se mantiene en `EN_TIEMPO_DE_RETIRO`).

**Acceptance Scenarios**:

1. **Scenario**: Desestimación clínica en galpón sin tratamientos pendientes
   - **Given** que el galpón "G-02" tiene una solicitud de aislamiento pendiente en estado "SOLICITADO"
   - **And** el lote no registra tratamientos farmacológicos vigentes ni periodos de retiro pendientes
   - **When** el Veterinario dictamina que los signos observados correspondieron a estrés calórico transitorio y firma la desestimación técnica
   - **Then** el expediente de aislamiento pasa a estado "DESESTIMADO"
   - **And** el galpón retorna a su estado operativo habitual "En producción"
   - **And** se emite el evento de dominio "AislamientoDesestimado" registrando el dictamen en auditoría

2. **Scenario**: Desestimación de sospecha con tiempo de retiro farmacológico concurrente
   - **Given** que el galpón "G-03" tiene una solicitud de aislamiento pendiente
   - **And** las aves recibieron previamente un fármaco que vence su periodo de carencia toxicológica en 3 días
   - **When** el Veterinario desestima la sospecha patológica mediante diagnóstico diferencial
   - **Then** la solicitud de aislamiento se cierra en estado "DESESTIMADO"
   - **And** el galpón permanece en estado "EN_TIEMPO_DE_RETIRO"
   - **And** el sistema mantiene bloqueada la salida de aves hacia el Módulo 3 hasta que transcurran los 3 días de carencia restantes

---

### User Story 3 - Integridad Poblacional, Blindaje Legal y Control Concurrente (Priority: P3)

Como auditor técnico y responsable de sanidad avícola de la granja, quiero asegurar que no se procesen aislamientos sobre galpones desocupados, que los diagnósticos queden blindados contra borrado físico y que las transacciones simultáneas se resuelvan atómicamente, para salvaguardar la validez jurídica del expediente sanitario.

**Why this priority**: Un expediente médico alterado, borrado o expedido sobre aves inexistentes destruye la validez técnica de las certificaciones de la granja y expone la operación a sanciones regulatorias severas ante las autoridades agropecuarias.

**Independent Test**: Se evalúan intentos de confirmación médica sobre galpones con población en cero aves, peticiones simultáneas sobre el mismo expediente y reintentos de red bajo claves de idempotencia.

**Acceptance Scenarios**:

1. **Scenario**: Intento de validar un galpón sin población de aves vivas
   - **Given** que el lote de un galpón registra una población viva de 0 aves en el inventario biológico
   - **When** el Veterinario intenta asentar un diagnóstico y confirmación de aislamiento
   - **Then** el sistema interrumpe la transacción informando que no es viable aislar un lote sin aves vivas
   - **And** no altera el estado del galpón ni crea ningún expediente

2. **Scenario**: Detección de operaciones concurrentes simultáneas
   - **Given** un aislamiento en revisión con versión 1 en la base de datos
   - **When** un primer veterinario guarda una confirmación diagnóstica elevando la versión a 2
   - **And** un segundo profesional intenta enviar un dictamen simultáneo basado en la versión 1
   - **Then** el sistema detecta la inconsistencia mediante control de concurrencia optimista
   - **And** rechaza la segunda solicitud exigiendo recargar la pantalla para consultar el dictamen vigente

3. **Scenario**: Idempotencia técnica ante reenvíos de red
   - **Given** una confirmación de aislamiento procesada con éxito bajo la cabecera "X-Idempotency-Key: IDEMP-VET-001"
   - **When** el cliente reenvía la misma petición idéntica por una reconexión de red
   - **Then** el sistema detecta que la solicitud ya fue atendida
   - **And** entrega la respuesta original almacenada con la cabecera "Idempotent-Replayed: true" sin duplicar dictámenes ni generar nuevos eventos

---

### Edge Cases

- **Galpón liquidado o cerrado durante la inspección**: Si el lote fue cerrado formalmente en el sistema antes de asentar el dictamen, la validación se cancela informando que las instalaciones se encuentran desocupadas.
- **Mortalidad total sobrevenida antes de confirmar**: Si una mortandad súbita deja la población en cero aves mientras el veterinario evalúa el galpón, el sistema bloquea la confirmación exigiendo la actualización del inventario biológico.
- **Incoherencia cronológica en fechas**: Si la fecha de inicio del aislamiento es anterior al día en que las aves ingresaron físicamente al galpón, la operación se rechaza por inconsistencia biológica.
- **Galpón con aislamiento ya activo**: Si un galpón ya se encuentra en estado `AISLADO`, el sistema prohíbe abrir otro aislamiento simultáneo; cualquier novedad debe gestionarse como prórroga o ajuste sobre el expediente vigente.
- **Extensión hacia Sacrificio Sanitario**: Si durante el acto obligatorio de diagnóstico el Veterinario concluye que el brote patológico es fulminante, incurable o de reporte oficial obligatorio, no procede el aislamiento regular y el flujo se extiende hacia el caso de uso `CU-VET-006: Ordenar sacrificio sanitario`.
- **Datos obligatorios incompletos**: Si no se especifica el criterio clínico descriptivo (mínimo 10 caracteres) o se omite la tarjeta profesional, el sistema no permite firmar el dictamen médico.

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al Médico Veterinario consultar la población viva de aves, la edad del lote y los antecedentes sanitarios antes de emitir su dictamen.
- **FR-002**: La confirmación o desestimación de un aislamiento DEBE ser efectuada exclusivamente por usuarios autenticados con rol `VETERINARIO` que cuenten con tarjeta o matrícula profesional registrada.
- **FR-003**: El sistema DEBE validar que el lote esté activo y que la población de aves vivas en el inventario sea estrictamente mayor a cero antes de formalizar el aislamiento.
- **FR-004**: Toda confirmación de aislamiento DEBE incluir de forma obligatoria el diagnóstico clínico (`Diagnosticar galpón`), exigiendo la selección de una patología en estado `ACTIVA` del catálogo nosológico de la granja (CU-VET-002).
- **FR-005**: La duración estimada del aislamiento clínico DEBE ser de al menos 24 horas a partir del inicio. En casos clasificados médicamente bajo severidad de `EMERGENCIA_SANITARIA`, se admite fijar un periodo mínimo de 12 horas.
- **FR-006**: Al confirmarse el aislamiento, el sistema DEBE transicionar el expediente a `ACTIVO`, el galpón a `AISLADO`, publicar el evento de dominio `AislamientoConfirmado` y despachar el evento de integración `GalponAisladoSanitariamenteIntegrationEvent` para bloquear inmediatamente órdenes de cosecha y faena comercial en el Módulo 3.
- **FR-007**: Al desestimarse un aislamiento, el sistema DEBE verificar si existen tratamientos farmacológicos en curso o en periodo de carencia; si el lote está limpio, el galpón retorna a `En producción`, pero si registra medicamentos con retiro pendiente, el galpón DEBE mantenerse en `EN_TIEMPO_DE_RETIRO`.
- **FR-008**: El sistema DEBE almacenar e inmortalizar el dictamen clínico, la decisión tomada, la matrícula profesional del veterinario y el criterio técnico (mínimo 10 caracteres explicativos), prohibiendo modificaciones directas sobre dictámenes emitidos.
- **FR-009**: Cada confirmación, desestimación o intento fallido DEBE registrar un asiento permanente en la tabla `san_auditoria` con fecha, hora, usuario responsable y código de correlación.
- **FR-010**: Queda ESTRICTAMENTE PROHIBIDO el borrado físico (`DELETE` en base de datos) de cualquier expediente de aislamiento o de sus dictámenes clínicos asociados.
- **FR-011**: El sistema DEBE aplicar control de concurrencia optimista (`version`) para evitar sobreescrituras simultáneas en el expediente.
- **FR-012**: El sistema DEBE implementar mecanismos de idempotencia técnica para evitar procesar dictámenes duplicados ante reconexiones de red, conservando la respuesta durante un período de 24 horas.

### Key Entities

- **Aislamiento**: Aggregate Root que representa la contención biológica en el galpón. Atributos: `id` (UUID), `galponId` (UUID), `loteId` (UUID), `enfermedadId` (UUID), `veterinarioId` (UUID), `periodo` (Value Object con fechaInicio y fechaFinEstimada), `severidad` (`LEVE`, `MODERADO`, `CRÍTICO`, `EMERGENCIA_SANITARIA`), `estado` (`SOLICITADO`, `ACTIVO`, `PENDIENTE_CERTIFICACION`, `CERTIFICADO`, `DESESTIMADO`, `SACRIFICIO_SANITARIO`), `version` (control concurrente) y su entidad interna de dictamen clínico.
- **Dictamen Clínico**: Entidad interna inmutable del Aislamiento que captura la resolución técnica y diagnóstica emitida por el profesional. Comparte la clave primaria del expediente raíz (`aislamientoId`). Atributos: `decision` (`CONFIRMADO` o `DESESTIMADO`), `criterioTecnico` (Texto descriptivo), `tarjetaProfesional` (String) y `fechaDictamen` (Timestamp UTC).
- **Enfermedad**: Entidad de catálogo nosológico de la granja que identifica la patología causante asociada al diagnóstico clínico.
- **Galpón y Lote de Aves**: Entidades operativas de la granja que aportan la ubicación física, la población real de pollos y el estado operativo (`En producción`, `AISLADO`, `EN_TIEMPO_DE_RETIRO`, `En cosecha`, `Vaciado Sanitario`).
- **Auditoría Sanitaria (`san_auditoria`)**: Registro inmutable append-only que respalda legalmente las decisiones técnicas veterinarias tomadas en la granja.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: El 100% de los galpones con aislamiento confirmado bloquean de forma automática la programación de órdenes de cosecha o faena comercial en el Módulo 3 desde el instante en que se emite el dictamen.
- **SC-002**: Cero casos (0%) de galpones con tratamientos farmacológicos en curso o en periodo de carencia reactivados indebidamente a estado normal de producción tras desestimar una sospecha de aislamiento.
- **SC-003**: El tiempo de respuesta para registrar el diagnóstico y firmar la validación médica en el sistema es menor a 400 milisegundos en condiciones operativas habituales.
- **SC-004**: El 100% de los dictámenes emitidos persisten la tarjeta profesional del veterinario y la justificación técnica en el registro histórico inmutable.
- **SC-005**: Cero incidentes (0%) de expedientes clínicos o registros de aislamiento eliminados físicamente de la base de datos a lo largo de la operación del sistema.
