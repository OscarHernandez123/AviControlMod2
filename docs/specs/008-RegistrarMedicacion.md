# Feature Specification: Registrar medicación

**Created**: 2026-09-04  
**Last Updated**: 2026-09-22  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Alta de Protocolo Terapéutico Estandarizado (Priority: P1)

Como Médico Veterinario de la granja, quiero registrar una pauta de medicación vinculando una patología activa del catálogo, un medicamento disponible en inventario, la dosis exacta con su unidad de medida, los días totales de duración y las indicaciones clínicas de administración, para disponer de esquemas terapéuticos estandarizados que puedan ser seleccionados posteriormente al diagnosticar un galpón o medicar un lote.

**Why this priority**: Es el catálogo base de tratamientos clínicos de la granja. Sin este registro previo, el veterinario no puede formular esquemas oficiales al evaluar un galpón ni estandarizar técnicamente la dosimetría y los días de suministro para evitar subdosificaciones o toxicidad en la parvada.

**Independent Test**: Se prueba ingresando una medicación vinculada a una patología activa del catálogo nosológico y a un medicamento con registro vigente en inventario, especificando dosis positiva, duración en días enteros (> 0) y descripción del método de administración. El sistema debe crear la medicación en estado disponible para diagnósticos futuros, asentar la traza inmutable en auditoría y persistir el evento de integración en el Outbox para su publicación en el broker, sin descontar stock físico en bodega ni alterar el estado operativo de los galpones.

**Acceptance Scenarios**:

1. **AS-001 - Scenario**: Registro exitoso de una pauta de medicación
   - **Given** que la patología "Bronquitis Infecciosa Aviar" se encuentra en estado "ACTIVA" en el catálogo nosológico
   - **And** el fármaco "Tilosina Tartrato 50%" existe y está habilitado en el inventario central de medicamentos
   - **When** el Veterinario ingresa dosis "0.5 g/L de agua", duración de 5 días y la descripción técnica "Suministrar disuelto en tanques de agua de bebida exclusivamente durante las mañanas"
   - **Then** el sistema almacena la medicación asociando formalmente la patología y el fármaco
   - **And** persiste atómicamente el evento de integración "MedicacionRegistradaIntegrationEvent" en la tabla outbox para su publicación asíncrona
   - **And** registra el asiento inmutable en "san_auditoria" con la firma del Veterinario responsable
   - **And** la medicación queda disponible inmediatamente para ser seleccionada en futuros diagnósticos de galpón
   - **And** mantiene intacto el stock de existencias físicas del medicamento en bodega

2. **AS-002 - Scenario**: Rechazo de registro por datos obligatorios faltantes o con espacios en blanco
   - **Given** que el Veterinario inicia el formulario de registro de medicación
   - **When** intenta guardar omitiendo la dosis o ingresando una descripción compuesta únicamente por espacios en blanco ("   ")
   - **Then** el sistema detiene el procesamiento, resalta los campos mandatorios incompletos y no persiste la medicación

3. **AS-003 - Scenario**: Rechazo por duración en días no válida
   - **Given** que el Veterinario diligencia la configuración terapéutica
   - **When** ingresa un valor de duración igual a 0, un número negativo (-3) o un valor con decimales (4.5 días)
   - **Then** el sistema rechaza el valor indicando que la duración debe ser estrictamente un número entero mayor a cero
   - **And** cancela la creación del registro

4. **AS-004 - Scenario**: Rechazo por patología inactiva o inexistente en catálogo
   - **Given** una patología registrada en el catálogo en estado "INACTIVA" (o un identificador inexistente)
   - **When** el Veterinario intenta seleccionarla para asociar la medicación
   - **Then** el sistema bloquea la selección e informa que la enfermedad no se encuentra vigente en el catálogo

5. **AS-005 - Scenario**: Rechazo por medicamento no registrado o inactivo en inventario
   - **Given** un código de producto farmacéutico que no figura en el inventario de medicamentos de la granja
   - **When** el Veterinario intenta asignarlo a la pauta de tratamiento
   - **Then** el sistema interrumpe la operación informando que el medicamento no está disponible en inventario
   - **And** no guarda la medicación

6. **AS-006 - Scenario**: Denegación de acceso a usuarios sin rol veterinario
   - **Given** un usuario autenticado en el sistema con rol "TRABAJADOR" o "ADMINISTRADOR"
   - **When** intenta ejecutar el comando de creación de una medicación
   - **Then** el sistema bloquea la acción por falta de privilegios sanitarios
   - **And** no crea ningún esquema terapéutico en la base de datos

---

### User Story 2 - Actualización de Pautas Terapéuticas y Protección de Históricos (Priority: P2)

Como Médico Veterinario de la granja, quiero editar los parámetros de una medicación existente para corregir la dosis, ajustar los días totales o actualizar las instrucciones de administración, garantizando que estos cambios apliquen exclusivamente a diagnósticos y prescripciones futuras sin alterar los tratamientos ni las fechas de retiro farmacológico calculadas en galpones diagnosticados con anterioridad.

**Why this priority**: La medicina veterinaria adapta protocolos según la resistencia o respuesta biológica observada en campo, pero es un imperativo de inocuidad alimentaria no alterar retroactivamente prescripciones pasadas para no falsear los periodos de carencia toxicológica ya auditados.

**Independent Test**: Se toma una medicación configurada originalmente con 5 días que fue utilizada en el diagnóstico de un lote previo y se actualiza su duración a 7 días. Se comprueba que el diagnóstico histórico conserve los 5 días y su fecha mínima de retiro original, mientras que una nueva prescripción use la duración actualizada de 7 días.

**Acceptance Scenarios**:

1. **AS-007 - Scenario**: Edición técnica exitosa de una medicación
   - **Given** una medicación registrada con duración de 5 días y dosis "0.5 g/L"
   - **When** el Veterinario modifica la duración a 7 días y ajusta la descripción técnica de dosificación
   - **Then** el sistema persiste la actualización de la entidad elevando su versión optimista
   - **And** persiste en el outbox el evento de integración "MedicacionActualizadaIntegrationEvent"
   - **And** registra el cambio en "san_auditoria"
   - **And** publica los nuevos valores únicamente para futuros diagnósticos

2. **AS-008 - Scenario**: Inmutabilidad de prescripciones y cálculos de retiro previos
   - **Given** un galpón diagnosticado previamente cuya prescripción médica utilizó la versión de 5 días de la medicación
   - **When** el Veterinario modifica en el catálogo la medicación elevándola a 7 días
   - **Then** el galpón previamente diagnosticado conserva intactos los 5 días originales y su fecha calculada de retiro toxicológico
   - **And** el sistema no recalcula ni altera ningún expediente clínico activo o cerrado

3. **AS-009 - Scenario**: Rechazo de edición por rol no autorizado
   - **Given** un usuario sin facultades clínicas (rol distinto a "VETERINARIO")
   - **When** intenta emitir la orden de modificación de una medicación
   - **Then** el sistema rechaza la solicitud y mantiene inalterados los datos de la medicación

---

### User Story 3 - Integridad Transaccional, Blindaje contra Borrado e Idempotencia (Priority: P3)

Como auditor de inocuidad y responsable técnico sanitario de la granja, quiero garantizar que los esquemas de medicación no puedan ser eliminados físicamente de la base de datos y que se procesen atómicamente ante reintentos de red, para salvaguardar la trazabilidad farmacológica y legal de la producción avícola.

**Why this priority**: Si se ejecutara un borrado físico en base de datos sobre una medicación, los tratamientos históricos aplicados a los lotes perderían su trazabilidad de origen, violando las regulaciones agropecuarias de control de residuos de medicamentos en carne.

**Independent Test**: Se ejecutan instrucciones de eliminación física directa (`DELETE`) sobre la tabla de medicaciones y se envían peticiones consecutivas idénticas bajo la misma clave de idempotencia técnica.

**Acceptance Scenarios**:

1. **AS-010 - Scenario**: Prohibición absoluta de borrado físico
   - **Given** una medicación registrada en el catálogo de tratamientos
   - **When** se intenta ejecutar una instrucción de borrado físico directo (`DELETE`)
   - **Then** el sistema intercepta y rechaza la operación
   - **And** preserva intacto el registro en la base de datos

2. **AS-011 - Scenario**: Prevención de duplicados por reintentos de red (Idempotencia)
   - **Given** un comando de registro de medicación procesado con éxito bajo la cabecera "X-Idempotency-Key: IDEMP-MED-9910"
   - **When** la aplicación cliente reenvía la misma petición idéntica debido a una pérdida temporal de conexión
   - **Then** el sistema reconoce la clave de idempotencia previa
   - **And** devuelve la respuesta original almacenada sin duplicar el registro de medicación en el catálogo

3. **AS-012 - Scenario**: Control de concurrencia optimista en modificaciones simultáneas
   - **Given** un registro de medicación en versión 1 en la base de datos
   - **When** un veterinario guarda una modificación elevando la versión a 2
   - **And** un segundo veterinario intenta guardar cambios basados en la versión 1 desactualizada
   - **Then** el sistema rechaza la segunda petición por colisión concurrente
   - **And** exige al usuario recargar la información vigente antes de editar

---

### Edge Cases

- **Medicamento o enfermedad inactivados durante el guardado**: Si una patología es inactivada en el catálogo o un medicamento es dado de baja del inventario mientras el Veterinario está diligenciando el formulario, el sistema valida la disponibilidad inmediatamente antes de abrir la transacción y aborta el registro con un mensaje explicativo.
- **Campos de texto con caracteres de espacio exclusivamente**: Si la dosis o la descripción contienen únicamente tabulaciones o espacios en blanco, el sistema los evalúa como datos no provistos y detiene el alta.
- **Acción aislada de catálogo (Cero impacto en inventario o galpones)**: El caso de uso representa estrictamente la definición de un esquema terapéutico. No descuenta stock físico de fármacos en bodega ni altera estados de galpones; el descuento real de inventario ocurre cuando se registra el consumo del medicamento en campo (`Spec 013: Registrar consumo medicamento`)[cite: 9].
- **Falla de conexión en persistencia o broker (Transactional Outbox)**: La transacción local de base de datos es estrictamente atómica; si la base de datos falla, ni la medicación se crea ni se genera el evento outbox. Si el broker de mensajería (RabbitMQ/Kafka) está caído, el evento permanece almacenado localmente en `san_outbox` para ser retransmitido sin pérdida de información.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El registro y actualización de medicaciones DEBE estar reservado exclusivamente a usuarios autenticados con rol `VETERINARIO`.
- **FR-002**: Toda medicación DEBE registrar obligatoriamente: patología asociada, medicamento de inventario, dosis con formato `<valor><unidad>` (ej. `'0.5 g/L de agua'`), duración en días enteros positivos, y descripción de administración. El sistema DEBE normalizar los campos de texto con `trim()` y rechazar valores nulos, vacíos o compuestos únicamente por espacios. La descripción DEBE tener entre 10 y 500 caracteres tras normalización.
- **FR-003**: La duración del tratamiento DEBE ser validada como un número entero estrictamente mayor a cero ($\text{diasTratamiento} \in \mathbb{Z}^+$).
- **FR-004**: El sistema DEBE validar que la patología referenciada exista en el catálogo de enfermedades de la granja y esté en estado `ACTIVA` (`Spec 009: Registrar enfermedad`)[cite: 9].
- **FR-005**: El sistema DEBE validar que el medicamento referenciado exista y se encuentre habilitado para su uso en el inventario central de medicamentos.
- **FR-006**: El registro de una medicación NO DEBE requerir la existencia de un diagnóstico previo, no debe alterar el estado operativo de ningún galpón y NO DEBE descontar existencias físicas del inventario.
- **FR-007**: Toda modificación sobre una medicación DEBE aplicar exclusivamente a prescripciones y diagnósticos futuros, manteniendo inalterables las dosis, duraciones y cálculos de retiro de diagnósticos pasados.
- **FR-008**: Al completarse el guardado, el sistema DEBE registrar en la tabla outbox el evento `MedicacionRegistradaIntegrationEvent` para su publicación en el broker bajo el tópico `sanitary.medication.registered.v1`, dejando el esquema disponible para selección en el caso de uso `Diagnosticar galpón`[cite: 9].
- **FR-009**: Cada creación o edición de medicación DEBE persistir un asiento histórico inmutable en la tabla `san_auditoria` con usuario responsable, fecha, hora y justificación de cambio.
- **FR-010**: Queda ESTRICTAMENTE PROHIBIDO el borrado físico (`DELETE` en base de datos) de cualquier registro de medicación.
- **FR-011**: El sistema DEBE aplicar control de concurrencia optimista (`version`) y soportar cabeceras de idempotencia técnica para evitar tratamientos duplicados ante reconexiones de red.
- **FR-012**: El sistema DEBE permitir desactivar lógicamente una medicación estableciendo `activa = false`, sin eliminar físicamente el registro. Las medicaciones inactivas NO DEBEN poder seleccionarse en nuevos diagnósticos, pero los diagnósticos existentes conservan su referencia histórica a la medicación inactiva.

### Non-Functional Requirements

| ID | Categoría | Requerimiento | Métrica |
| :--- | :--- | :--- | :--- |
| **NFR-001** | Rendimiento | Latencia de validación, registro y persistencia | < 250 ms (p95) bajo carga normal |
| **NFR-002** | Seguridad | Acceso restringido exclusivamente al rol VETERINARIO | HTTP 403 Forbidden a otros roles |
| **NFR-003** | Integridad | Escritura atómica (entidad + outbox + auditoría) bajo `@Transactional` | 100% de consistencia local |
| **NFR-004** | Persistencia | Prohibición estricta de sentencias `DELETE` SQL en base de datos | 0% borrados físicos |

### Key Entities

- **Medicación**: Aggregate Root que define el esquema de tratamiento clínico estándar. Atributos: `id` (UUID), `enfermedadId` (UUID), `medicamentoId` (UUID), `dosis` (String estructurado con valor y unidad), `diasTratamiento` (Integer > 0), `descripcion` (Texto con pautas de administración), `activa` (Boolean), `version` (control concurrente) y marcas temporales de auditoría (`createdAt`, `updatedAt`).
- **Enfermedad**: Entidad del catálogo nosológico de la granja (`Spec 009`)[cite: 9] que actúa como la patología causal del esquema terapéutico.
- **Medicamento**: Insumo farmacéutico registrado en el inventario central que provee el principio activo y la concentración.
- **Auditoría Sanitaria (`san_auditoria`)**: Registro inmutable append-only donde queda resguardada la firma, fecha y autoría del Veterinario que formula el protocolo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las medicaciones creadas con éxito quedan disponibles para selección en diagnósticos de galpón en menos de 1 segundo tras la confirmación.
- **SC-002**: Cero por ciento (0%) de alteración o descuento de existencias en el inventario de medicamentos durante la creación o edición de medicaciones en catálogo.
- **SC-003**: Cero casos (0%) de modificación retroactiva en dosis, días o periodos de carencia en expedientes clínicos ya diagnosticados tras editar una medicación.
- **SC-004**: El 100% de los intentos de creación o modificación ejecutados por roles distintos a `VETERINARIO` son bloqueados por el sistema.
- **SC-005**: El tiempo de respuesta del sistema para validar y registrar una medicación es inferior a 250 milisegundos en condiciones operativas habituales.
- **SC-006**: Cero incidentes (0%) de registros de medicación eliminados físicamente de la base de datos a lo largo del ciclo de vida del sistema.
- **SC-007**: El 100% de los eventos de integración son respaldados en la tabla Outbox dentro de la misma transacción de base de datos, garantizando entrega confiable al broker de colas.
- **SC-008**: El 100% de las medicaciones desactivadas lógicamente quedan excluidas de selección en diagnósticos futuros y conservan su referencia histórica en diagnósticos existentes.

## UI Component Mapping (Prototipo ↔ Spec)

Esta sección documenta la correspondencia estricta entre los controles del prototipo visual oficial (`008-RegistrarMedicacion.png`) y los requerimientos funcionales del sistema. Todo componente visual debe responder a un FR y ningún comportamiento fuera de este catálogo está permitido.

### Pantalla: Catálogo de Medicación
- **Prototipo de Referencia**: `docs/prototype/Veterinario/008-RegistrarMedicacion.png`
- **Actor Exclusivo**: `VETERINARIO` (FR-001)

| Componente UI | Tipo | FR Asociado | Comportamiento Técnico y Validación |
| :--- | :--- | :--- | :--- |
| **Tabla de Medicaciones** | Data Grid | FR-002 | Lista enfermedad asociada, fármaco, dosis/vía, duración, instrucciones, estado |
| **Stat Cards de Resumen** | Metric Cards (4) | FR-002, FR-012 | Muestran: Pautas registradas, Pautas activas, Fármacos vinculados, Patologías cubiertas |
| **Barra de Filtros** | Filter Bar | FR-002, FR-004 | Búsqueda por patología/fármaco + filtros por patología y estado (activa/inactiva) |
| **Banner Informativo** | Info Banner | FR-006 | Muestra "Las pautas aquí configuradas no descuentan stock ni alteran prescripciones previas" |
| **Botón "+ Registrar medicación"** | Button (Primary) | FR-001, FR-002 | Abre el formulario lateral de alta (restringido a rol VETERINARIO) |
| **Dropdown "Patología vinculada"** | Dropdown | FR-004 | Solo patologías en estado ACTIVA (integración con Spec 009) |
| **Dropdown "Medicamento activo"** | Dropdown | FR-005 | Solo fármacos habilitados en inventario central |
| **Input "Dosis y Unidad"** | Text Input | FR-002 | Formato `<valor><unidad>` (ej. "0.5 g/L de agua"), no vacío tras trim() |
| **Input "Duración (Días enteros)"** | Number Input | FR-003 | Entero estrictamente mayor a cero |
| **Textarea "Indicaciones clínicas"** | Textarea | FR-002 | Entre 10 y 500 caracteres tras trim() |
| **Badge "Activa" / "Inactiva"** | Status Badge | FR-012 | Refleja el estado lógico `activa == true/false` |
| **Acción "Ver detalle" (Ícono ojo)** | Action Button | FR-002 | Carga el detalle en modo solo lectura |
| **Acción "Editar" (Ícono lápiz)** | Action Button | FR-007, FR-011 | Permite mutación controlada respetando `@Version` |
| **Warning Box en Formulario** | Alert Box | FR-010 | Advierte que la pauta quedará disponible para diagnósticos y prohíbe la eliminación física |

### Elementos Prohibidos en la Pantalla (Guardrails Sanitarios)
- ❌ **Botón "Eliminar" / "Borrar"**: Terminantemente prohibido (`FR-010`). No existe eliminación física de protocolos.
- ❌ **Campos huérfanos**: Prohibido añadir campos de fechas de auditoría o autores que no pertenezcan al dominio funcional.
- ❌ **Selectores que permitan patologías inactivas**: Solo patologías ACTIVAS (`FR-004`).

### Estados Operativos del Formulario
- **Validación inline**: Resaltado de campos obligatorios con asterisco rojo y validación de formato de dosis y duración.
- **Transacción en progreso**: Bloqueo de controles de envío durante el commit atómico (`san_medicaciones` + `san_outbox` + `san_auditoria`).
- **Colisión de Concurrencia**: Modal informativo ante HTTP 409 (`ConcurrenciaOptimistaException`) solicitando recarga de datos.