# Feature Specification: Registrar una enfermedad

**Created**: 2026-09-04  
**Last Updated**: 2026-09-22  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Alta de Patología en el Catálogo Nosológico (Priority: P1)

Como Médico Veterinario de la granja, quiero registrar una patología especificando su código oficial, nombre clínico, nivel de riesgo, tipo etiológico, descripción sintomática y el indicador binario de obligatoriedad de sacrificio sanitario, para consolidar el catálogo nosológico oficial que rige los diagnósticos clínicos (Spec 011), la prescripción de medicaciones (Spec 008) y las órdenes de contingencia sanitaria (Spec 012).

**Why this priority**: Es el catálogo maestro nosológico del sistema. Sin este registro previo, es inviable emitir diagnósticos en los galpones, asociar esquemas terapéuticos o activar los protocolos de sacrificio sanitario total de la parvada.

**Independent Test**: Se prueba enviando un comando de creación con todos los campos obligatorios completos y válidos, código único no repetido y selección obligatoria del indicador de sacrificio. El sistema debe persistir la entidad en san_enfermedades, asentar la firma inmutable en san_auditoria, registrar atómicamente el evento de integración en san_outbox para su despacho asíncrono hacia Kafka y dejar la patología disponible para selección clínica inmediata.

**Acceptance Scenarios**:

1. **AS-001 - Scenario**: Registro exitoso de una enfermedad de manejo terapéutico (sin sacrificio)
   - **Given** que un usuario autenticado con rol VETERINARIO accede al módulo de registro de enfermedades
   - **When** ingresa el código nosológico "ID-ENF-001", nombre "Bronquitis Infecciosa Aviar", nivel de riesgo "Medio", tipo "Viral", descripción clínica "Afecta vías respiratorias superiores, estertores y caída de postura" y selecciona explícitamente No en sacrificio sanitario
   - **Then** el sistema almacena la enfermedad en estado activa
   - **And** persiste atómicamente el evento de integración EnfermedadRegistradaIntegrationEvent en san_outbox bajo el tópico sanitary.disease.registered.v1
   - **And** registra el asiento inmutable en san_auditoria con la matrícula del Veterinario actuante
   - **And** la patología queda disponible inmediatamente para asociarle pautas de medicación (Spec 008) y emitir diagnósticos (Spec 011)

2. **AS-002 - Scenario**: Registro exitoso de una patología de notificación oficial (con sacrificio sanitario)
   - **Given** que el Veterinario identifica una patología crítica de alta contagiosidad (ej. Influenza Aviar de alta patogenicidad o Newcastle velogénico)
   - **When** ingresa código "ID-ENF-002", nombre "Newcastle Fuerte", nivel de riesgo "Alto", tipo "Viral", descripción clínica "Signos neurológicos, diarrea verdosa y alta mortalidad" y selecciona explícitamente Sí en sacrificio sanitario
   - **Then** el sistema almacena la enfermedad con el indicador requiereSacrificioSanitario = true
   - **And** emite el evento correspondiente mediante el Transactional Outbox
   - **And** habilita esta enfermedad como causal obligatoria para emitir órdenes de sacrificio sanitario total (Spec 012) al diagnosticarse en un galpón

3. **AS-003 - Scenario**: Rechazo de registro por código nosológico duplicado
   - **Given** que ya existe en el catálogo la enfermedad con código "ID-ENF-001"
   - **When** el Veterinario intenta registrar una nueva enfermedad usando el mismo código "ID-ENF-001"
   - **Then** el sistema aborta la operación, retorna un código de error de colisión nosológica y no persiste ningún registro en base de datos

4. **AS-004 - Scenario**: Rechazo por datos obligatorios faltantes o espacios en blanco
   - **Given** que el Veterinario inicia el formulario de registro
   - **When** intenta enviar la petición omitiendo el código, el nombre, el nivel de riesgo, el tipo, la descripción o enviando textos compuestos exclusivamente por espacios en blanco ("   ")
   - **Then** el sistema detiene la transacción, marca los campos mandatorios incompletos y no persiste la entidad

5. **AS-005 - Scenario**: Rechazo por falta de selección explícita en el indicador de sacrificio
   - **Given** que el sistema no aplica valores por defecto en decisiones clínicas críticas
   - **When** el Veterinario intenta enviar el formulario sin marcar de manera expresa Sí o No en la casilla de sacrificio sanitario
   - **Then** el sistema bloquea el guardado exigiendo al usuario una decisión clínica afirmativa o negativa

6. **AS-006 - Scenario**: Denegación de acceso a usuarios sin rol veterinario
   - **Given** un usuario autenticado con rol TRABAJADOR o ADMINISTRADOR
   - **When** intenta emitir el comando de registro de una enfermedad
   - **Then** el sistema bloquea la acción por insuficiencia de privilegios sanitarios y retorna HTTP 403 Forbidden

---

### User Story 2 - Actualización del Catálogo y Blindaje Histórico (Priority: P2)

Como Médico Veterinario de la granja, quiero editar los datos de una enfermedad existente (nombre, nivel de riesgo, tipo, descripción clínica o indicador de sacrificio), garantizando que las modificaciones rijan exclusivamente para diagnósticos y parametrizaciones futuras sin alterar ni recalcular retroactivamente los diagnósticos históricos ni los expedientes clínicos cerrados.

**Why this priority**: Las pautas nosológicas evolucionan por mutaciones o normativas del ICA; sin embargo, no se deben alterar retroactivamente las auditorías de galpones diagnosticados en el pasado para no viciar la trazabilidad legal sanitaria.

**Independent Test**: Se toma una enfermedad registrada con requiereSacrificioSanitario = false aplicada en el diagnóstico previo de un lote y se modifica en el catálogo a true. Se comprueba que el diagnóstico histórico conserve su estado original de tratamiento, mientras que diagnósticos subsiguientes exijan la derivación a sacrificio sanitario (Spec 012).

**Acceptance Scenarios**:

1. **AS-007 - Scenario**: Edición técnica exitosa de una patología
   - **Given** una enfermedad registrada en el catálogo nosológico
   - **When** el Veterinario actualiza su nivel de riesgo a "Crítico" y amplía las observaciones clínicas
   - **Then** el sistema incrementa la versión optimista de la entidad (version = version + 1)
   - **And** persiste el evento EnfermedadActualizadaIntegrationEvent en san_outbox
   - **And** registra el asiento en san_auditoria detallando los valores modificados
   - **And** aplica los nuevos parámetros exclusivamente a diagnósticos formulados con posterioridad

2. **AS-008 - Scenario**: Inmutabilidad de prescripciones y diagnósticos previos
   - **Given** un galpón con expediente clínico cerrado basado en la versión 1 de la enfermedad
   - **When** el Veterinario guarda una modificación de la enfermedad generando la versión 2
   - **Then** el expediente histórico del galpón mantiene intactos los parámetros diagnósticos originales
   - **And** el sistema no muta ni recalcula ninguna decisión sanitaria pasada

3. **AS-009 - Scenario**: Rechazo de edición por colisión de concurrencia optimista
   - **Given** un registro de enfermedad en versión 1
   - **When** un veterinario confirma una actualización elevando la versión a 2
   - **And** un segundo veterinario intenta guardar modificaciones basadas en la versión 1 previa
   - **Then** el sistema rechaza la segunda petición por versión obsoleta (HTTP 409 Conflict)
   - **And** exige al usuario recargar la información antes de volver a intentar

---

### User Story 3 - Integridad Transaccional, Blindaje contra Borrado e Idempotencia (Priority: P3)

Como auditor de inocuidad y responsable sanitario de la granja, quiero garantizar que ninguna patología pueda ser eliminada físicamente de la base de datos y que las operaciones de guardado sean idempotentes ante fallas de red, para asegurar la trazabilidad perpetua de los registros sanitarios.

**Why this priority**: La pérdida o destrucción de un registro de enfermedad en base de datos invalidaría los diagnósticos históricos vinculados a los galpones, vulnerando la trazabilidad del lote ante entes de control zoosanitario.

**Independent Test**: Se ejecutan órdenes de borrado directo (DELETE) sobre la tabla san_enfermedades y se envían peticiones consecutivas idénticas bajo la misma cabecera X-Idempotency-Key.

**Acceptance Scenarios**:

1. **AS-010 - Scenario**: Prohibición absoluta de borrado físico
   - **Given** una enfermedad existente en el catálogo nosológico
   - **When** se intenta ejecutar una instrucción HTTP DELETE o sentencia SQL DELETE directa
   - **Then** el sistema intercepta y rechaza la operación (HTTP 405 Method Not Allowed)
   - **And** mantiene inalterada la tupla en la base de datos

2. **AS-011 - Scenario**: Prevención de duplicación ante reintentos de red (Idempotencia)
   - **Given** un comando de alta procesado exitosamente bajo la cabecera "X-Idempotency-Key: IDEMP-ENF-1020"
   - **When** el cliente retransmite la misma solicitud debido a un timeout transitorio de la red
   - **Then** el filtro de idempotencia reconoce la clave previamente procesada
   - **And** responde con la información almacenada originalmente sin crear una entidad duplicada ni encolar un segundo evento en el outbox

---

### Edge Cases

- **Intento de desactivación con esquemas terapéuticos activos**: Si el Veterinario inhabilita una enfermedad en el catálogo, pero esta tiene medicaciones vigentes asociadas (Spec 008), las medicaciones existentes se conservan, pero el sistema bloquea la creación de nuevos esquemas de medicación para dicha patología.
- **Falla en el broker de mensajería (Kafka caído)**: Gracias al patrón Transactional Outbox, si el broker de mensajería no está disponible al momento del guardado, la transacción de base de datos se completa con éxito (san_enfermedades + san_outbox + san_auditoria), garantizando que el worker en segundo plano reintente el envío cuando el broker se restablezca.
- **Campos de texto con caracteres invisibles**: Cualquier texto compuesto por espacios, tabulaciones o saltos de línea se normaliza mediante .trim(); si la longitud resultante es cero, se rechaza la transacción de inmediato.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El registro y actualización de enfermedades DEBE estar restringido exclusivamente a usuarios autenticados con rol VETERINARIO.
- **FR-002**: Toda enfermedad DEBE registrar de forma obligatoria: código nosológico único, nombre clínico, nivel de riesgo, tipo etiológico, descripción clínica y el indicador binario requiereSacrificioSanitario.
- **FR-003**: El código nosológico DEBE ser único en todo el catálogo de la granja (ej. ID-ENF-001).
- **FR-004**: Los campos de texto nombre, tipo y descripcion NO DEBEN ser nulos, vacíos ni estar compuestos únicamente por espacios en blanco. La descripción clínica DEBE tener un mínimo de 10 caracteres.
- **FR-005**: El nivel de riesgo DEBE pertenecer estrictamente a la enumeración clínica: BAJO, MEDIO, ALTO o CRITICO.
- **FR-006**: El tipo de patología DEBE clasificarse según su origen etiológico: VIRAL, BACTERIANA, PARASITARIA o FUNGICA.
- **FR-007**: El Veterinario DEBE seleccionar explícitamente el indicador requiereSacrificioSanitario (true o false); el sistema NO DEBE admitir valores nulos ni inferir valores por defecto.
- **FR-008**: Si requiereSacrificioSanitario es verdadero, el sistema DEBE clasificar la enfermedad como letal y habilitar la orden de vaciado sanitario total (Spec 012: Ordenar sacrificio sanitario) al emitirse un diagnóstico.
- **FR-009**: Si requiereSacrificioSanitario es falso, el sistema DEBE exigir la selección de un protocolo de tratamiento (Spec 008: Registrar medicación) al confirmarse un diagnóstico clínico (Spec 011).
- **FR-010**: Toda modificación sobre los atributos de una enfermedad DEBE elevar la versión optimista (version = version + 1) y aplicar exclusivamente a diagnósticos y prescripciones futuras, manteniendo la inmutabilidad de los expedientes históricos.
- **FR-011**: Al confirmarse la persistencia, el sistema DEBE registrar de forma atómica el evento EnfermedadRegistradaIntegrationEvent en la tabla san_outbox para su despacho asíncrono al tópico sanitary.disease.registered.v1.
- **FR-012**: Cada creación o actualización DEBE registrar un asiento inmutable en la tabla san_auditoria con usuario responsable, fecha, hora y detalle técnico del cambio.
- **FR-013**: Queda ESTRICTAMENTE PROHIBIDO el borrado físico (DELETE en SQL) sobre las tuplas de la tabla san_enfermedades.
- **FR-014**: El sistema DEBE implementar control de concurrencia optimista (@Version) y admitir cabeceras de idempotencia técnica X-Idempotency-Key retenidas durante 24 horas.
- **FR-015**: El sistema DEBE permitir desactivar lógicamente una enfermedad estableciendo `activa = false`, sin eliminar físicamente el registro. Las enfermedades inactivas NO DEBEN poder seleccionarse en nuevos diagnósticos (Spec 011) ni en nuevas medicaciones (Spec 008), pero los expedientes históricos existentes conservan su referencia. Si existen medicaciones vigentes asociadas a la enfermedad, se conservan, pero el sistema bloquea la creación de nuevos esquemas de medicación.

### Non-Functional Requirements

| ID | Categoría | Requerimiento | Métrica |
| :--- | :--- | :--- | :--- |
| **NFR-001** | Rendimiento | Latencia de validación, registro y persistencia | < 250 ms (p95) bajo carga normal |
| **NFR-002** | Seguridad | Acceso restringido exclusivamente al rol VETERINARIO | HTTP 403 Forbidden a otros roles |
| **NFR-003** | Integridad | Escritura atómica (entidad + outbox + auditoría) bajo `@Transactional` | 100% de consistencia local |
| **NFR-004** | Persistencia | Prohibición estricta de sentencias `DELETE` SQL en base de datos | 0% borrados físicos |

### Key Entities

- **Enfermedad**: Aggregate Root nosológico. Atributos: id (UUID), codigo (String único), nombre (String), nivelRiesgo (Enum: BAJO, MEDIO, ALTO, CRITICO), tipo (Enum: VIRAL, BACTERIANA, PARASITARIA, FUNGICA), descripcion (Texto), requiereSacrificioSanitario (Boolean), activa (Boolean), version (Integer), createdAt (Timestamp) y updatedAt (Timestamp).
- **Medicación**: Entidad de protocolo terapéutico (Spec 008) que vincula una enfermedad no letal con un fármaco de inventario.
- **Diagnóstico**: Expediente clínico de lote/galpón (Spec 011) que referencia la patología dictaminada.
- **Orden de Sacrificio Sanitario**: Expediente de contingencia biosegura (Spec 012) habilitado únicamente si la patología diagnosticada tiene requiereSacrificioSanitario = true.
- **Auditoría Sanitaria (san_auditoria)**: Bitácora inmutable append-only donde queda resguardada la trazabilidad técnica veterinaria.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las enfermedades creadas quedan disponibles para su selección en medicaciones (Spec 008) y diagnósticos (Spec 011) en menos de 1 segundo tras la confirmación de la transacción.
- **SC-002**: Cero casos (0%) de modificación retroactiva en diagnósticos clínicos o prescripciones previas tras actualizar una patología en el catálogo.
- **SC-003**: Cero incidentes (0%) de patologías registradas sin una decisión binaria explícita (Sí / No) sobre el sacrificio sanitario.
- **SC-004**: El 100% de los intentos de creación o modificación ejecutados por roles distintos a VETERINARIO son bloqueados con HTTP 403 Forbidden.
- **SC-005**: Cero duplicados (0%) en códigos nosológicos gracias a la restricción única a nivel de dominio y base de datos.
- **SC-006**: El tiempo de respuesta del sistema para validar y registrar una patología es inferior a 250 milisegundos en condiciones normales de operación.
- **SC-007**: Cero registros (0%) de enfermedades eliminados físicamente de la base de datos a lo largo de todo el ciclo de vida del software.
- **SC-008**: El 100% de los eventos de integración son guardados atómicamente en la tabla Outbox dentro de la misma transacción local de base de datos.
- **SC-009**: El 100% de las enfermedades desactivadas lógicamente quedan excluidas de selección en diagnósticos y medicaciones futuras, conservando de forma íntegra su referencia histórica en expedientes previos.

## UI Component Mapping (Prototipo ↔ Spec)

Esta sección documenta la correspondencia estricta entre los controles del prototipo visual oficial (`009-RegistrarEnfermedad.png`) y los requerimientos funcionales del sistema. Todo componente visual debe responder a un FR y ningún comportamiento fuera de este catálogo está permitido.

### Pantalla: Catálogo de Enfermedades
- **Prototipo de Referencia**: `docs/prototype/Veterinario/009-RegistrarEnfermedad.png`
- **Actor Exclusivo**: `VETERINARIO` (FR-001)

| Componente UI | Tipo | FR Asociado | Comportamiento Técnico y Validación |
| :--- | :--- | :--- | :--- |
| **Tabla de Patologías** | Data Grid | FR-002 | Lista código, nombre, nivel de riesgo, tipo y estado de sacrificio |
| **Stat Cards de Resumen** | Metric Cards (4) | FR-002, FR-005 | Muestran: Total registradas, Riesgo Alto/Crítico, Riesgo Medio, Riesgo Bajo (agregados de la tabla) |
| **Barra de Filtros** | Filter Bar | FR-002, FR-005, FR-006, FR-007 | Búsqueda por nombre/código + filtros por Nivel de Riesgo, Tipo Etiológico y Sacrificio Sanitario |
| **Banner Informativo** | Info Banner | FR-002 | Muestra "Catálogo clínico maestro: sustenta prescripciones (Spec 008) y sacrificios (Spec 012)" |
| **Warning Box en Formulario** | Alert Box | FR-010, FR-013 | Advierte que la modificación incrementará la versión optimista y prohíbe el borrado físico |
| **Botón "+ Registrar enfermedad"** | Button (Primary) | FR-001, FR-002 | Abre el formulario lateral de alta (bloqueado para roles no autorizados) |
| **Input "Código Nosológico Oficial"** | Text Input | FR-003 | Formato obligatorio `ID-ENF-XXX`, validación de unicidad en base de datos |
| **Input "Nombre Clínico"** | Text Input | FR-002, FR-004 | Obligatorio, normalizado con `.trim()`, rechaza espacios en blanco |
| **Select "Nivel de Riesgo"** | Dropdown | FR-005 | Valores permitidos: `BAJO`, `MEDIO`, `ALTO`, `CRITICO` |
| **Select "Tipo Etiológico"** | Dropdown | FR-006 | Valores permitidos: `VIRAL`, `BACTERIANA`, `PARASITARIA`, `FUNGICA` |
| **Textarea "Descripción Sintomática"** | Textarea | FR-004 | Longitud mínima de 10 caracteres tras normalización |
| **Radios "¿Requiere Sacrificio?"** | Radio Group | FR-007, FR-008, FR-009 | Selección explícita (`Sí` / `No`) obligatoria; sin valor por defecto |
| **Badge "Activa" / "Inactiva"** | Status Badge | FR-015 | Indica el estado de disponibilidad operativa del catálogo |
| **Acción "Ver detalle" (Ícono ojo)** | Action Button | FR-002 | Carga el expediente clínico en modalidad de solo lectura |
| **Acción "Editar" (Ícono lápiz)** | Action Button | FR-010, FR-014 | Permite mutación controlada respetando el control optimista `@Version` |

### Elementos Prohibidos en la Pantalla (Guardrails Sanitarios)
- ❌ **Botón "Eliminar" / "Borrar"**: Terminantemente prohibido (`FR-013`). No existe eliminación física.
- ❌ **Selección por defecto en Sacrificio**: Prohibido preseleccionar opciones (`FR-007`); exige decisión humana.
- ❌ **Campos huérfanos**: Prohibido añadir campos de fechas de auditoría o autores que no pertenezcan al dominio funcional.

### Estados Operativos del Formulario
- **Validación inline**: Resaltado de campos obligatorios con asterisco rojo y validación de longitud mínima ≥ 10 caracteres en descripción.
- **Transacción en progreso**: Bloqueo de controles de envío y despliegue de indicador de carga durante el commit atómico (`san_enfermedades` + `san_outbox` + `san_auditoria`).
- **Colisión de Concurrencia**: Despliegue de modal informativo ante HTTP 409 (`ConcurrenciaOptimistaException`) solicitando recarga de datos.