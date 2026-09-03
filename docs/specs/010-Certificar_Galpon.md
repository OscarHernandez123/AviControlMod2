# Feature Specification: Certificar Galpón (CU-VET-003)

**Created**: 2026-09-03  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Emisión del Dictamen Formal de Certificación Sanitaria (Priority: P1)

Como Médico Veterinario autorizado en la granja, quiero evaluar un galpón con aislamiento cumplido y emitir formalmente la Certificación Sanitaria vinculando mi matrícula profesional y dictamen clínico favorable[cite: 1], para hacer constar la recuperación del lote, el vencimiento de los tiempos de retiro y habilitar el reintegro operativo del galpón[cite: 1].

**Why this priority**: Es el acto legal y técnico indispensable para liberar un galpón; sin esta certificación, el sistema bloquea de manera definitiva cualquier intento de reintegro o despacho comercial del lote hacia faena[cite: 1].

**Independent Test**: Se valida ejecutando el comando de certificación sobre un galpón aislado cuya fecha mínima de liberación ya se haya cumplido y no registre tratamientos en curso[cite: 1]. Se verifica que el aislamiento transicione a `CERTIFICADO`, se guarde la entidad inmutable `CertificacionSanitaria`, se emita `GalponCertificado` y se registre la auditoría correspondiente[cite: 1].

**Acceptance Scenarios**:

1. **Scenario**: Certificación exitosa de galpón con retiro cumplido
   - **Given** un Galpón "G-01" en Granja "GR-001" con aislamiento en estado "PENDIENTE_CERTIFICACION"
   - **And** la fecha mínima de liberación calculada es anterior o igual a la fecha actual[cite: 1]
   - **And** el lote no tiene tratamientos farmacológicos activos en curso[cite: 1]
   - **When** el Veterinario emite la certificación con dictamen favorable "Lote asintomático, parámetros biológicos normalizados y carencia cumplida", matrícula profesional "MP-VET-88492-CO" y vigencia de 48 horas[cite: 1]
   - **Then** el estado del aislamiento cambia a "CERTIFICADO"[cite: 1]
   - **And** se almacena la entidad inmutable "CertificacionSanitaria" con vigencia hasta la fecha calculada
   - **And** se despacha el evento de dominio "GalponCertificado"[cite: 1]
   - **And** se registra una entrada inmutable en "san_auditoria"[cite: 1]
   - **And** el sistema responde con código HTTP 201 Created

2. **Scenario**: Rechazo de certificación por tiempo de retiro activo
   - **Given** un Galpón "G-02" con aislamiento y tratamientos farmacológicos administrados[cite: 1]
   - **And** la fecha mínima de liberación calculada vence en 24 horas (retiro aún activo)[cite: 1]
   - **When** el Veterinario intenta emitir la certificación sanitaria[cite: 1]
   - **Then** el sistema aborta la transacción
   - **And** retorna el código de error "VET-005: TIEMPO_RETIRO_ACTIVO" con código HTTP 422
   - **And** no altera el estado del aislamiento ni emite certificaciones[cite: 1]

---

### User Story 2 - Control de Caducidad y Re-evaluación Clínica (Priority: P2)

Como Sistema y Auditor Sanitario, quiero que las certificaciones sanitarias tengan una vigencia temporal limitada de 48 horas y expiren automáticamente si no se ejecuta el reintegro en esa ventana, para evitar que se reintegren o faenen aves cuya condición clínica haya cambiado tras una evaluación obsoleta[cite: 1].

**Why this priority**: Protege la inocuidad alimentaria garantizando que el aval médico corresponda al estado biológico inmediato de las aves antes de su procesamiento comercial[cite: 1].

**Independent Test**: Se emite una certificación y se simula el transcurso de más de 48 horas sin reintegro[cite: 1]. Se verifica que el estado de validez caduque, que el sistema bloquee el reintegro por certificación vencida y exija una nueva inspección médica[cite: 1].

**Acceptance Scenarios**:

1. **Scenario**: Expiración automática de certificación no utilizada
   - **Given** una certificación sanitaria emitida hace más de 48 horas sin que se haya ejecutado el comando de reintegro[cite: 1]
   - **When** se intenta procesar el reintegro operativo del galpón
   - **Then** el sistema rechaza la operación informando "VET-012: CERTIFICACION_EXPIRADA" (HTTP 422)
   - **And** transiciona el aislamiento nuevamente a "PENDIENTE_CERTIFICACION" para exigir una nueva inspección veterinaria[cite: 1]

2. **Scenario**: Evaluación clínica desfavorable durante la inspección de certificación
   - **Given** un Galpón "G-03" con tiempo de retiro cumplido pero con presencia de signología clínica residual
   - **When** el Veterinario registra un dictamen desfavorable por recaída infecciosa
   - **Then** el sistema no emite certificación
   - **And** el aislamiento retorna al estado "ACTIVO" con prórroga del periodo de observación
   - **And** se registra la novedad en el expediente clínico del lote

---

### User Story 3 - Integridad Transaccional, RBAC y Aislamiento Multi-Tenant (Priority: P3)

Como Responsable de Seguridad del Sistema, quiero asegurar que solo médicos veterinarios colegiados puedan certificar instalaciones y que la operación esté blindada contra accesos entre granjas y duplicidad concurrente[cite: 1].

**Why this priority**: La certificación es un documento técnico-legal con implicaciones sanitarias y de exportación; requiere absoluta certeza de identidad y tenant[cite: 1].

**Independent Test**: Se intenta certificar con roles no autorizados (Administrador o Trabajador), tokens JWT de otra granja o solicitudes concurrentes sobre el mismo aislamiento[cite: 1].

**Acceptance Scenarios**:

1. **Scenario**: Rechazo de certificación por rol no autorizado
   - **Given** un usuario autenticado con rol "ADMINISTRADOR" o "TRABAJADOR"[cite: 1]
   - **When** intenta invocar el endpoint de certificación sanitaria[cite: 1]
   - **Then** el sistema deniega la acción respondiendo HTTP 403 Forbidden
   - **And** retorna el error "VET-008: USUARIO_NO_AUTORIZADO"

2. **Scenario**: Rechazo por violación multi-tenant
   - **Given** un aislamiento perteneciente a la Granja "GR-002"[cite: 1]
   - **When** un Veterinario autenticado en Granja "GR-001" intenta certificarlo[cite: 1]
   - **Then** el sistema deniega el acceso con HTTP 403 Forbidden
   - **And** retorna el error "VET-009: GRANJA_NO_AUTORIZADA"[cite: 1]

---

### Edge Cases

- **Tratamiento farmacológico en curso al momento de certificar:** Si existe al menos un tratamiento que no haya sido marcado como finalizado o suspendido, el sistema bloquea la certificación con error `VET-007: TRATAMIENTO_ACTIVO` sin importar la fecha clínica[cite: 1].
- **Mortalidad total ocurrida previo a certificar:** Si la población viva del lote es 0 aves, el sistema impide certificar emitiendo `VET-013: POBLACION_CERO_NO_OPERABLE`[cite: 1].
- **Intentos de certificación simultánea:** Si dos veterinarios intentan certificar el mismo aislamiento concurrentemente, el mecanismo de Optimistic Locking (`version`) permite persistir al primero y rechaza al segundo con HTTP 409 Conflict (`VET-014: CONCURRENCIA_DETECTADA`)[cite: 1].
- **Aislamiento ya certificado:** Si se intenta volver a certificar un aislamiento en estado `CERTIFICADO`, el sistema rechaza la operación por estado inválido (`VET-010: TRANSICION_ESTADO_INVALIDA`)[cite: 1].

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema debe exigir que el usuario autenticado posea rol `VETERINARIO`, matrícula profesional válida y pertenezca a la `granjaId` del galpón a certificar[cite: 1].
- **FR-002**: El sistema debe verificar que el aislamiento esté en estado `PENDIENTE_CERTIFICACION` o `ACTIVO` con cuarentena cumplida[cite: 1].
- **FR-003**: El sistema debe comprobar que $\text{FechaActual} \ge \text{FechaMinimaLiberacion}$ consultando el servicio de dominio de retiro antes de permitir la emisión de la certificación[cite: 1].
- **FR-004**: El sistema debe verificar que no existan tratamientos farmacológicos en estado `EN_CURSO` asociados al lote[cite: 1].
- **FR-005**: El sistema debe exigir el registro obligatorio de dictamen clínico estructurado (mínimo 10 caracteres) y número de tarjeta/matrícula profesional[cite: 1].
- **FR-006**: El sistema debe calcular y fijar la vigencia del certificado: $\text{validoHasta} = \text{fechaCertificacion} + 48\text{ horas}$[cite: 1].
- **FR-007**: Al certificar exitosamente, el sistema debe mutar el estado del aislamiento a `CERTIFICADO` y emitir el evento de dominio `GalponCertificado`[cite: 1].
- **FR-008**: El sistema debe registrar una entrada inmutable append-only en `san_auditoria` con el contenido del aval médico y el `correlationId`[cite: 1].
- **FR-009**: El sistema debe prohibir la eliminación física (`DELETE` relacional) de los certificados sanitarios emitidos[cite: 1].
- **FR-010**: El sistema debe invalidar la certificación sanitaria para el reintegro si la fecha actual supera el valor de `validoHasta` (RN-VET-007)[cite: 1].

### Key Entities

- **CertificacionSanitaria** *(Entidad Interna del Agregado Aislamiento)*: Aval técnico-sanitario emitido. Atributos: `id` (UUID), `aislamientoId` (UUID), `veterinarioId` (UUID), `tarjetaProfesional` (String), `dictamenClinico` (Text), `fechaCertificacion` (Timestamp UTC), `validoHasta` (Timestamp UTC) y `reintegroEjecutado` (Boolean)[cite: 1].
- **Aislamiento** *(Aggregate Root)*: Entidad raíz transaccional que muta su estado a `CERTIFICADO` al consolidarse la certificación médica[cite: 1].
- **Tratamiento** *(Referencia de Dominio)*: Entidad consultada para verificar la inexistencia de tratamientos activos y el cumplimiento de tiempos de retiro[cite: 1].
- **Auditoria (`san_auditoria`)**: Registro inmutable de la emisión de la certificación con snapshot de datos médicos[cite: 1].

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cero por ciento (0%) de galpones certificados antes de cumplirse el tiempo de retiro farmacológico calculado ($\text{FechaActual} < \text{FechaMinimaLiberacion}$)[cite: 1].
- **SC-002**: El 100% de las certificaciones sanitarias emitidas habilitan de manera inmediata el comando de reintegro en el sistema para el lote evaluado[cite: 1].
- **SC-003**: Cero por ciento (0%) de reintegros ejecutados con certificaciones sanitarias que hayan superado su ventana de vigencia de 48 horas[cite: 1].
- **SC-004**: El tiempo de procesamiento de la emisión de certificación es inferior a 350 milisegundos en al menos el 95% de las solicitudes atendidas bajo carga normal.
- **SC-005**: El 100% de las certificaciones emitidas persisten de forma inmutable la matrícula profesional del veterinario y su justificación técnica en `san_auditoria`[cite: 1].