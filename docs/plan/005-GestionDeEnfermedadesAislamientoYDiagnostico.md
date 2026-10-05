# Implementation Plan: Gestión de Enfermedades, Aislamiento y Diagnóstico

**Date**: 02/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [009-RegistrarEnfermedad.md](../specs/009-RegistrarEnfermedad.md)
- [020-SolicitarValidacionParaAislarGalpon.md](../specs/020-SolicitarValidacionParaAislarGalpon.md)
- [010-ValidarAislamiento.md](../specs/010-ValidarAislamiento.md)
- [011-DiagnosticarGalpon.md](../specs/011-DiagnosticarGalpon.md)

## Summary

Implementar el catálogo de enfermedades, el registro y envío de solicitudes para aislar un galpón, la evaluación y validación veterinaria del aislamiento y el diagnóstico posterior del galpón aislado. Para enfermedades tratables, el diagnóstico consulta la medicación seleccionada y calcula la fecha de reintegro a producción; para enfermedades que requieren sacrificio sanitario, habilita la emisión de la orden correspondiente sin crear una fecha de reintegro.

El plan conserva como historial las enfermedades, solicitudes, decisiones y diagnósticos. Galpon y Lote continúan siendo entidades propietarias del Módulo 1; este plan utiliza sus identificadores y puertos públicos. El cambio del estado operativo del galpón se solicita mediante un puerto de salida del Módulo 1 y no mediante una segunda copia persistida de Galpon.

No se implementan aquí el catálogo ni el consumo de medicamentos, la mortalidad, la actualización del inventario vivo ni la orden o ejecución del sacrificio sanitario. El plan publica los hechos que esos procesos necesitan y deja sus operaciones a los planes correspondientes.

## Technical Context

**Performance Goals**: El 95 % de los registros, validaciones y diagnósticos válidos queda disponible en máximo 1 segundo después de su confirmación. La transición automática de reintegro procesa los diagnósticos vencidos sin duplicar cambios de estado.

**Constraints**: Solo el veterinario registra o edita enfermedades, valida aislamientos y registra diagnósticos. El trabajador autorizado puede crear solicitudes, pero no puede cambiar el estado del galpón. Las solicitudes son inmutables después de enviarse. La validación consulta nuevamente el estado vigente del Módulo 1 antes de cambiarlo. El diagnóstico solo procede con estado AISLAMIENTO y lote activo.

**Scale/Scope**: Seis historias de usuario, un catálogo de enfermedades, solicitudes y decisiones de aislamiento, diagnósticos, cuatro operaciones principales de escritura, una consulta de solicitudes pendientes, eventos internos, adaptadores internos y una tarea programada para reintegros.

**Dependencias funcionales**: Galpones y lotes del Módulo 1; catálogo y consulta de medicamentos del Plan 006; orden de sacrificio sanitario del Plan 008; autenticación, actor, reloj y formato de errores definidos en General.md.

### Decisiones específicas

1. **Enfermedad**: Enfermedad es una entidad propia del módulo sanitario con UUID, nombre, nivel de riesgo, tipo, descripción clínica y requiereSacrificioSanitario. Todos los campos son obligatorios; el indicador debe recibirse explícitamente y no tiene valor predeterminado.
2. **Versionado de decisiones**: Editar una enfermedad solo afecta diagnósticos futuros. Un diagnóstico conserva una instantánea de los datos y la decisión utilizados al confirmarse.
3. **Galpón y lote**: No se duplican ni se crean tablas paralelas para Galpon o Lote. La solicitud y el diagnóstico almacenan sus UUID, población actual y edad capturadas en el momento requerido.
4. **Asignación de trabajadores**: No se consulta ni se valida una asignación trabajador-galpón. Cualquier trabajador con el permiso correspondiente puede crear una solicitud para cualquier galpón que cumpla las condiciones.
5. **Solicitud**: Solo se crea para un galpón en estado PRODUCTIVO, con lote activo y población actual mayor que cero. Una solicitud pendiente por galpón impide otra solicitud pendiente. Una solicitud enviada no se edita ni se retira.
6. **Validación**: El veterinario debe tener una solicitud previa. Antes de completar la validación se consulta nuevamente el estado del galpón. Solo se permite la transición PRODUCTIVO a AISLAMIENTO.
7. **Diagnóstico**: El diagnóstico se registra para el galpón que continúa en AISLAMIENTO y para el lote alojado al momento de confirmar. Una enfermedad tratable exige una medicación compatible; una enfermedad que requiere sacrificio no acepta medicación ni calcula reintegro.
8. **Reintegro**: La fecha de reintegro es fechaDiagnostico + diasTotalesMedicacion. Un proceso programado busca diagnósticos vencidos y solicita AISLAMIENTO a PRODUCTIVO solo si el galpón conserva el estado AISLAMIENTO. La transición debe ser idempotente.
9. **Puertos**: Se reutilizan GalponQueryPort y LoteQueryPort si ya están publicados por la capacidad de consulta. No se agrega un Modulo1QueryPort redundante. CambiarEstadoOperativoGalponPort representa una operación distinta y sí es necesario.
10. **Eventos**: Las escrituras publican eventos internos después de confirmar la transacción local. Los eventos que atraviesan límites de módulo usan IntegrationEventPublisherPort y outbox si el proyecto lo requiere. Las consultas no publican eventos.
11. **Persistencia**: Las entidades sanitarias y sus relaciones se persisten en tablas propias. Galpon y Lote permanecen en la capacidad propietaria. Las versiones de migración se asignan con la siguiente versión disponible del repositorio para no duplicar números usados por el Plan 002.
12. **Consistencia**: La validación y el diagnóstico deben leer el estado vigente justo antes de confirmar. Si una dependencia no está disponible o entrega datos incompletos, se rechaza la escritura y no se guardan registros parciales.

### Alcance documental

- El spec 009 relaciona enfermedad, medicación y diagnóstico. El catálogo y la consulta de medicaciones pertenecen al Plan 006; este plan solo consulta una medicación compatible al confirmar el diagnóstico.
- El spec 011 habilita una orden de sacrificio sanitario. La emisión y ejecución de esa orden pertenece al Plan 008; aquí se publica el hecho que la habilita.
- La transición automática a producción usa el puerto propietario del Módulo 1 y no modifica directamente una entidad de persistencia ajena.
- Los nombres finales de eventos y contratos se deben acordar con los consumidores antes de publicar mensajes de integración.

## Project Structure

### Documentation (this feature)

    docs/
    ├── specs/
    │   ├── 009-RegistrarEnfermedad.md
    │   ├── 020-SolicitarValidacionParaAislarGalpon.md
    │   ├── 010-ValidarAislamiento.md
    │   └── 011-DiagnosticarGalpon.md
    └── plan/
        └── 005-GestionDeEnfermedadesAislamientoYDiagnostico.md

### Source Code (repository root)

Estructura objetivo del feature. Reutilizar el paquete raíz, actor autenticado, reloj, errores, transacciones y configuración de eventos existentes.

    src/main/java/com/avicontrol/
    ├── domain/
    │   ├── model/sanitario/
    │   │   ├── enfermedad/
    │   │   │   ├── Enfermedad.java
    │   │   │   ├── TipoEnfermedad.java
    │   │   │   └── NivelRiesgo.java
    │   │   ├── aislamiento/
    │   │   │   ├── SolicitudAislamiento.java
    │   │   │   ├── EstadoSolicitudAislamiento.java
    │   │   │   └── DecisionAislamiento.java
    │   │   └── diagnostico/
    │   │       ├── DiagnosticoGalpon.java
    │   │       └── ResultadoDiagnostico.java
    │   ├── exception/sanitario/
    │   └── port/out/sanitario/
    │       ├── EnfermedadRepositoryPort.java
    │       ├── SolicitudAislamientoRepositoryPort.java
    │       ├── DiagnosticoGalponRepositoryPort.java
    │       ├── GalponQueryPort.java
    │       ├── LoteQueryPort.java
    │       ├── CambiarEstadoOperativoGalponPort.java
    │       ├── MedicacionQueryPort.java
    │       └── IntegrationEventPublisherPort.java
    ├── application/sanitario/
    │   ├── RegistrarEnfermedadUseCase.java
    │   ├── EditarEnfermedadUseCase.java
    │   ├── SolicitarAislamientoUseCase.java
    │   ├── ConsultarSolicitudesAislamientoUseCase.java
    │   ├── ValidarAislamientoUseCase.java
    │   ├── RegistrarDiagnosticoUseCase.java
    │   ├── ProcesarReintegrosVencidosUseCase.java
    │   └── result/
    └── infrastructure/
        ├── adapter/in/rest/sanitario/
        │   ├── EnfermedadController.java
        │   ├── SolicitudAislamientoController.java
        │   ├── ValidacionAislamientoController.java
        │   └── DiagnosticoGalponController.java
        ├── adapter/in/scheduler/sanitario/
        │   └── ReintegroSanitarioScheduler.java
        ├── adapter/out/internal/sanitario/
        ├── adapter/out/persistence/sanitario/
        └── config/SanitarioBeanConfiguration.java

    src/test/java/com/avicontrol/
    ├── domain/model/sanitario/
    ├── application/sanitario/
    ├── infrastructure/adapter/in/rest/sanitario/
    ├── infrastructure/adapter/out/internal/sanitario/
    └── integration/SanitarioIntegrationTest.java

**Structure Decision**: Las entidades sanitarias se organizan por negocio; los casos de uso son entradas públicas de aplicación; los puertos de salida aíslan la persistencia y las capacidades externas; los DTOs y mappers permanecen fuera del dominio. El scheduler invoca un caso de uso y no contiene reglas sanitarias.

### Entidades y relación

Enfermedad es la entidad configurable para diagnósticos futuros. SolicitudAislamiento conserva la petición inmutable del trabajador. DiagnosticoGalpon conserva la decisión veterinaria y las referencias capturadas al confirmar.

    Enfermedad
    ├── id: UUID
    ├── nombre: String
    ├── nivelRiesgo: NivelRiesgo
    ├── tipo: TipoEnfermedad
    ├── descripcionClinica: String
    └── requiereSacrificioSanitario: boolean

    SolicitudAislamiento
    ├── id: UUID
    ├── galponId: UUID
    ├── loteId: UUID
    ├── emitidaPor: UUID
    ├── emitidaEn: Instant
    ├── motivo: String
    ├── signosClinicos: String
    ├── poblacionActualReportada: Integer
    ├── edadLoteDias: Integer
    └── estado: PENDIENTE_VALIDACION

    DiagnosticoGalpon
    ├── id: UUID
    ├── galponId: UUID
    ├── loteId: UUID
    ├── enfermedadId: UUID
    ├── nombreEnfermedadHistorico: String
    ├── requiereSacrificioHistorico: boolean
    ├── medicacionId: UUID opcional
    ├── diasMedicacion: Integer opcional
    ├── fechaDiagnostico: LocalDate
    ├── fechaReintegro: LocalDate opcional
    └── estado: ACTIVO | REINTEGRADO | HABILITA_SACRIFICIO

La enfermedad es editable para diagnósticos futuros. El diagnóstico conserva los datos históricos necesarios para que una edición posterior no cambie una decisión ya tomada. Galpon y Lote no contienen colecciones sanitarias dentro de este módulo.

### Contratos de los puertos

| Puerto | Responsabilidad |
| --- | --- |
| EnfermedadRepositoryPort | Crear, buscar, editar y listar enfermedades activas. |
| SolicitudAislamientoRepositoryPort | Crear solicitudes inmutables, buscar una solicitud, consultar pendientes por galpón y listar solicitudes para el veterinario. |
| DiagnosticoGalponRepositoryPort | Crear diagnósticos, buscar diagnósticos con reintegro vencido y garantizar que un reintegro no se procese dos veces. |
| GalponQueryPort | Consultar existencia y estado vigente del galpón. Reutilizar el contrato del Módulo 1/Plan 001. |
| LoteQueryPort | Obtener el lote activo y sus datos vigentes. No seleccionar silenciosamente un lote histórico. |
| CambiarEstadoOperativoGalponPort | Solicitar la transición permitida del estado del galpón al Módulo 1. |
| MedicacionQueryPort | Consultar una medicación activa, compatible con la enfermedad, sus días totales y datos vigentes. |
| IntegrationEventPublisherPort | Publicar eventos versionados para consumidores externos al módulo. |

### Contratos HTTP propuestos

| Endpoint | Acceso | Resultado |
| --- | --- | --- |
| POST /api/enfermedades | Veterinario | Registra una enfermedad. |
| PUT /api/enfermedades/{enfermedadId} | Veterinario | Edita una enfermedad para diagnósticos futuros. |
| POST /api/galpones/{galponId}/solicitudes-aislamiento | Trabajador autorizado, administrador autorizado | Crea una solicitud pendiente. |
| GET /api/solicitudes-aislamiento?estado=PENDIENTE_VALIDACION | Veterinario | Lista solicitudes disponibles para evaluación. |
| POST /api/solicitudes-aislamiento/{solicitudId}/validacion | Veterinario | Valida o rechaza la solicitud. |
| POST /api/galpones/{galponId}/diagnosticos | Veterinario | Registra diagnóstico y calcula reintegro cuando aplica. |

Una escritura válida responde 201 al crear y 200 al editar o decidir. Se utiliza application/problem+json. Un estado vigente incompatible responde 409; una dependencia imprescindible no disponible responde 503; una solicitud o enfermedad inexistente responde 404.

### JSON común de errores

```json
{
  "type": "https://avicontrol/errors/operacion-sanitaria-no-permitida",
  "title": "Operación sanitaria no permitida",
  "status": 409,
  "detail": "El galpón debe estar en estado AISLAMIENTO",
  "instance": "/api/galpones/{galponId}/diagnosticos",
  "code": "ESTADO_GALPON_INCOMPATIBLE",
  "correlationId": "uuid",
  "fieldErrors": []
}
```

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Acordar contratos, corregir discrepancias documentales y preparar dependencias compartidas.

- [ ] T001 Contrastar los estados del galpón ofrecidos por Módulo 1 y confirmar la representación común de PRODUCTIVO, AISLAMIENTO y VACIADO_SANITARIO.
- [ ] T002 Confirmar que no existe asignación trabajador-galpón y registrar la corrección correspondiente en el spec 020.
- [ ] T003 Acordar roles y permisos: veterinario, trabajador/operario y administrador autorizado.
- [ ] T004 Acordar los nombres y versiones de los eventos con Módulo 1, Plan 006 y Plan 008.
- [ ] T005 Crear fixtures de galpón productivo con lote activo, galpón sin lote, población cero, galpón aislado y diagnósticos vencidos.
- [ ] T006 Alinear el paquete raíz, actor autenticado, reloj, manejo de errores, outbox y transacciones con General.md.

**Checkpoint**: Roles, estados, dependencias y discrepancias documentales resueltos antes de implementar reglas.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Preparar dominio, persistencia y puertos compartidos por todas las historias.

- [ ] T007 Implementar Enfermedad, sus enums y validaciones de datos obligatorios en Java puro.
- [ ] T008 Implementar SolicitudAislamiento, estados, inmutabilidad y validación de una solicitud pendiente por galpón.
- [ ] T009 Implementar DiagnosticoGalpon, cálculo de fecha de reintegro y estados de seguimiento.
- [ ] T010 Definir excepciones y puertos de repositorio, consulta de galpón/lote, medicación y cambio de estado.
- [ ] T011 Crear migraciones de enfermedades, solicitudes, diagnósticos, decisiones y eventos procesados usando la siguiente versión Flyway disponible.
- [ ] T012 Implementar adaptadores internos para galpón, lote y medicación sin acceder a repositorios privados ajenos ni usar HTTP dentro del monolito.
- [ ] T013 Configurar transacciones, outbox e idempotencia de eventos.
- [ ] T014 Verificar que las entidades sanitarias no introduzcan una segunda tabla o escritura para Galpon y Lote.

**Checkpoint**: Modelo, persistencia y contratos compartidos listos sin duplicar la propiedad del Módulo 1 ni del Plan 006.

## Phase 3: User Story 1 — Registrar una Enfermedad (Priority: P1)

**Spec**: 009, historia 1.

**Goal**: El veterinario registra una enfermedad completa y determina explícitamente si requiere sacrificio sanitario.

**Independent Test**: Un veterinario registra una enfermedad tratable y otra que requiere sacrificio; los datos se almacenan y los usuarios no veterinarios son rechazados.

### Definición del evento para User Story 1

**Evento producido**: EnfermedadRegistrada.

**Evento consumido**: Ninguno.

El evento se publica después de guardar todos los campos obligatorios. Incluye eventId, eventVersion, occurredAt, producer, correlationId, aggregateId, enfermedadId, nombre, nivelRiesgo, tipo, descripcionClinica y requiereSacrificioSanitario.

### Definición del endpoint REST para User Story 1

| Elemento | Definición |
| --- | --- |
| Método y ruta | POST /api/enfermedades |
| Autorización | ROLE_VETERINARIO |
| Entrada | Nombre, nivel de riesgo, tipo, descripción clínica e indicador booleano obligatorio. |
| Respuesta 201 | Enfermedad creada y sus datos vigentes. |
| Errores | 400 por datos ausentes o espacios, 401/403 por seguridad y 503 si no se puede confirmar la escritura. |

#### JSON de solicitud

```json
{
  "nombre": "Bronquitis infecciosa",
  "nivelRiesgo": "ALTO",
  "tipo": "RESPIRATORIA",
  "descripcionClinica": "Afección respiratoria de rápida propagación",
  "requiereSacrificioSanitario": false
}
```

#### JSON de respuesta

```json
{
  "enfermedadId": "uuid",
  "nombre": "Bronquitis infecciosa",
  "nivelRiesgo": "ALTO",
  "tipo": "RESPIRATORIA",
  "descripcionClinica": "Afección respiratoria de rápida propagación",
  "requiereSacrificioSanitario": false
}
```

### Tests para User Story 1

- [ ] T015 [US1] Probar campos obligatorios, espacios, indicador explícito y autorización veterinaria.
- [ ] T016 [US1] Probar que una interrupción no deja una enfermedad parcialmente registrada.
- [ ] T017 [US1] Probar publicación de EnfermedadRegistrada después de confirmar la transacción.

### Implementación de User Story 1

- [ ] T018 [US1] Implementar RegistrarEnfermedadUseCase.
- [ ] T019 [US1] Crear EnfermedadResult, request, response y mapper.
- [ ] T020 [US1] Implementar POST /api/enfermedades y manejo de errores.

**Checkpoint**: Solo un veterinario puede crear enfermedades completas sin valores implícitos.

## Phase 4: User Story 2 — Editar una Enfermedad (Priority: P2)

**Spec**: 009, historia 2.

**Goal**: El veterinario actualiza una enfermedad para diagnósticos futuros sin cambiar diagnósticos existentes.

**Independent Test**: Se edita el indicador de una enfermedad ya utilizada; el diagnóstico anterior conserva su decisión y un diagnóstico posterior usa el nuevo valor.

### Definición del evento para User Story 2

**Evento producido**: EnfermedadActualizada.

**Evento consumido**: Ninguno.

El evento incluye la nueva versión de la enfermedad. Los diagnósticos existentes no se recalculan ni reciben una actualización retroactiva.

### Definición del endpoint REST para User Story 2

| Elemento | Definición |
| --- | --- |
| Método y ruta | PUT /api/enfermedades/{enfermedadId} |
| Autorización | ROLE_VETERINARIO |
| Entrada | Todos los campos editables y el indicador explícito. |
| Respuesta 200 | Enfermedad actualizada. |
| Errores | 400 por datos inválidos, 403 por rol, 404 si no existe y 409 por versión concurrente incompatible. |

#### JSON de solicitud

```json
{
  "nombre": "Bronquitis infecciosa aviar",
  "nivelRiesgo": "ALTO",
  "tipo": "RESPIRATORIA",
  "descripcionClinica": "Descripción actualizada",
  "requiereSacrificioSanitario": false,
  "version": 2
}
```

### Tests e implementación

- [ ] T021 [US2] Probar edición válida y rechazo por rol.
- [ ] T022 [US2] Probar que diagnósticos existentes conservan snapshots históricos.
- [ ] T023 [US2] Implementar EditarEnfermedadUseCase, endpoint, control de versión y evento.

**Checkpoint**: Las ediciones solo afectan diagnósticos futuros.

## Phase 5: User Story 3 — Crear y Enviar Solicitud de Aislamiento (Priority: P1)

**Spec**: 020, historia 1.

**Goal**: Un trabajador autorizado registra una sospecha sobre un galpón productivo con lote activo, sin cambiar el estado del galpón.

**Independent Test**: Se solicita aislamiento para un galpón productivo con población mayor que cero y se obtiene una solicitud pendiente disponible para el veterinario.

### Definición del evento para User Story 3

**Evento producido**: AislamientoSolicitado.

**Evento consumido**: Eventos de cambios de galpón/lote solo si ya existen contratos publicados; no se requiere listener para crear una copia. La solicitud consulta el estado vigente mediante GalponQueryPort y LoteQueryPort.

El evento incluye eventId, eventVersion, occurredAt, producer, correlationId, aggregateId, solicitudId, galponId, loteId, poblacionActualReportada, edadLoteDias, motivo, signosClinicos y solicitadoPor.

### Definición del endpoint REST para User Story 3

| Elemento | Definición |
| --- | --- |
| Método y ruta | POST /api/galpones/{galponId}/solicitudes-aislamiento |
| Autorización | ROLE_TRABAJADOR o administrador autorizado |
| Entrada | Motivo y signos clínicos no vacíos. No recibe trabajadorId ni exige galponId asignado. |
| Respuesta 201 | Solicitud inmutable en PENDIENTE_VALIDACION, con lote, población y edad capturados. |
| Errores | 400 por texto vacío, 404 por galpón/lote inexistente, 409 por estado distinto de productivo, población cero o solicitud pendiente duplicada, 503 si Módulo 1 no está disponible. |

#### JSON de solicitud

```json
{
  "motivo": "Sospecha de enfermedad respiratoria",
  "signosClinicos": "Tos y disminución del consumo"
}
```

#### JSON de respuesta

```json
{
  "solicitudId": "uuid",
  "galponId": "uuid",
  "loteId": "uuid",
  "poblacionActualReportada": 7960,
  "edadLoteDias": 17,
  "estado": "PENDIENTE_VALIDACION",
  "emitidaEn": "2026-10-02T14:30:00Z"
}
```

### Tests e implementación

- [ ] T024 [US3] Probar galpón productivo, estados incompatibles, lote ausente y población cero.
- [ ] T025 [US3] Probar captura de población y edad vigentes, sin seleccionar un lote histórico por fecha arbitraria.
- [ ] T026 [US3] Probar que no existe validación de asignación trabajador-galpón.
- [ ] T027 [US3] Implementar SolicitarAislamientoUseCase, persistencia, evento y endpoint.

**Checkpoint**: La solicitud queda pendiente, trazable e inmutable sin cambiar el estado del galpón.

## Phase 6: User Story 4 — Consultar Solicitudes Pendientes y Evitar Duplicados (Priority: P2)

**Spec**: 020, historia 2.

**Goal**: El sistema impide solicitudes pendientes duplicadas y permite al veterinario consultar las solicitudes recibidas.

**Independent Test**: Una segunda solicitud para el mismo galpón es rechazada y la solicitud existente aparece en la consulta veterinaria.

### Definición del evento para User Story 4

**Evento producido**: Ninguno para la consulta.

**Evento consumido**: Ninguno obligatorio.

La unicidad se garantiza en aplicación y con una restricción de persistencia. La consulta no publica SolicitudConsultada.

### Definición de los endpoints REST

| Elemento | Definición |
| --- | --- |
| Método y ruta | GET /api/solicitudes-aislamiento?estado=PENDIENTE_VALIDACION |
| Autorización | ROLE_VETERINARIO |
| Respuesta 200 | Lista paginada de solicitudes pendientes. |
| Regla de creación | Si existe una pendiente, POST responde 409 y no crea otra. |

#### JSON de respuesta

```json
{
  "content": [
    {
      "solicitudId": "uuid",
      "galponId": "uuid",
      "loteId": "uuid",
      "motivo": "Sospecha de enfermedad respiratoria",
      "signosClinicos": "Tos y disminución del consumo",
      "estado": "PENDIENTE_VALIDACION",
      "emitidaEn": "2026-10-02T14:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### Tests e implementación

- [ ] T028 [US4] Probar restricción única para una solicitud pendiente por galpón bajo concurrencia.
- [ ] T029 [US4] Probar consulta veterinaria paginada y solicitud duplicada con respuesta 409.
- [ ] T030 [US4] Implementar consulta, restricción de base de datos y DTOs.

**Checkpoint**: El veterinario recibe una sola solicitud pendiente por galpón.

## Phase 7: User Story 5 — Validar el Aislamiento (Priority: P1)

**Spec**: 010, historia 1.

**Goal**: Solo un veterinario puede validar una solicitud previa y cambiar el galpón de PRODUCTIVO a AISLAMIENTO.

**Independent Test**: Una solicitud pendiente se valida con estado productivo vigente y cambia de estado; una solicitud inexistente, un rol no veterinario o un estado diferente son rechazados.

### Definición del evento para User Story 5

**Evento producido**: AislamientoValidado al aprobar y AislamientoRechazado al rechazar una solicitud evaluada.

El evento aprobado incluye solicitudId, galponId, estado anterior, estado nuevo, veterinario, fecha y correlationId. El consumidor debe ser idempotente usando eventId.

**Evento consumido**: AislamientoSolicitado si se utiliza integración asíncrona; el registro autoritativo se consulta mediante repositorio.

### Definición del endpoint REST

| Elemento | Definición |
| --- | --- |
| Método y ruta | POST /api/solicitudes-aislamiento/{solicitudId}/validacion |
| Autorización | ROLE_VETERINARIO |
| Entrada | Decisión APROBAR o RECHAZAR y observación. |
| Respuesta 200 | Decisión guardada y estado vigente del galpón. |
| Errores | 403 por rol, 404 por solicitud, 409 por solicitud ya decidida o galpón que ya no está productivo, 503 si no se puede verificar o cambiar el estado en Módulo 1. |

#### JSON de solicitud

```json
{
  "decision": "APROBAR",
  "observaciones": "Se confirma la necesidad de aislamiento preventivo"
}
```

#### JSON de respuesta

```json
{
  "solicitudId": "uuid",
  "decision": "APROBADA",
  "galponId": "uuid",
  "estadoAnterior": "PRODUCTIVO",
  "estadoNuevo": "AISLAMIENTO",
  "validadaPor": "uuid",
  "validadaEn": "2026-10-02T15:00:00Z"
}
```

### Tests e implementación

- [ ] T031 [US5] Probar autorización exclusiva del veterinario y solicitud previa obligatoria.
- [ ] T032 [US5] Probar segunda lectura del estado vigente y rechazo de estados diferentes de productivo.
- [ ] T033 [US5] Probar transición mediante CambiarEstadoOperativoGalponPort sin modificar otros datos de Galpon.
- [ ] T034 [US5] Implementar ValidarAislamientoUseCase, decisión, evento y endpoint.

**Checkpoint**: Solo una solicitud pendiente válida produce la transición a aislamiento.

## Phase 8: User Story 6 — Registrar Diagnóstico y Gestionar Reintegro (Priority: P1)

**Spec**: 011, historia 1.

**Goal**: El veterinario diagnostica un galpón aislado, exige medicación cuando corresponde y habilita sacrificio sanitario cuando la enfermedad lo requiere.

**Independent Test**: Un diagnóstico tratable calcula reintegro; uno que requiere sacrificio no exige medicación; un galpón no aislado es rechazado; un diagnóstico vencido reintegra únicamente un galpón que aún está aislado.

### Definición del evento para User Story 6

**Eventos producidos**:

- DiagnosticoGalponRegistrado.
- ReintegroSanitarioDisponible cuando se cumple la fecha y el galpón sigue aislado.
- GalponReintegradoAProduccion después de confirmar la transición.
- SacrificioSanitarioHabilitado cuando la enfermedad requiere sacrificio.

El diagnóstico incluye la instantánea de nombre, indicador de sacrificio y días de medicación utilizados. Los eventos incluyen eventId, eventVersion, occurredAt, producer, correlationId y aggregateId.

**Eventos consumidos**: AislamientoValidado es opcional como notificación; el caso de uso verifica el estado vigente mediante GalponQueryPort.

### Definición de los endpoints REST

| Elemento | Definición |
| --- | --- |
| Método y ruta | POST /api/galpones/{galponId}/diagnosticos |
| Autorización | ROLE_VETERINARIO |
| Entrada | Enfermedad obligatoria y medicación obligatoria solo si la enfermedad no requiere sacrificio. |
| Respuesta 201 | Diagnóstico, enfermedad histórica, medicación opcional y fecha de reintegro opcional. |
| Errores | 403 por rol, 404 por enfermedad/medicación, 409 si el galpón no está aislado, no tiene lote, la medicación no es compatible o falta cuando es obligatoria. |

#### JSON de solicitud tratable

```json
{
  "enfermedadId": "uuid",
  "medicacionId": "uuid"
}
```

#### JSON de respuesta tratable

```json
{
  "diagnosticoId": "uuid",
  "galponId": "uuid",
  "loteId": "uuid",
  "enfermedadId": "uuid",
  "nombreEnfermedadHistorico": "Bronquitis infecciosa",
  "requiereSacrificioHistorico": false,
  "medicacionId": "uuid",
  "diasMedicacion": 5,
  "fechaDiagnostico": "2026-10-02",
  "fechaReintegro": "2026-10-07",
  "estado": "ACTIVO"
}
```

#### JSON de respuesta que habilita sacrificio

```json
{
  "diagnosticoId": "uuid",
  "galponId": "uuid",
  "loteId": "uuid",
  "enfermedadId": "uuid",
  "requiereSacrificioHistorico": true,
  "medicacionId": null,
  "fechaReintegro": null,
  "estado": "HABILITA_SACRIFICIO"
}
```

### Tests e implementación

- [ ] T035 [US6] Probar diagnóstico exclusivo para veterinario, galpón aislado, lote activo y enfermedad activa.
- [ ] T036 [US6] Probar medicación obligatoria y compatible para enfermedades tratables.
- [ ] T037 [US6] Probar enfermedad que requiere sacrificio sin medicación ni fecha de reintegro.
- [ ] T038 [US6] Probar snapshots históricos y rechazo si el estado cambia antes de confirmar.
- [ ] T039 [US6] Implementar RegistrarDiagnosticoUseCase, MedicacionQueryPort, persistencia, eventos y endpoint.
- [ ] T040 [US6] Implementar ProcesarReintegrosVencidosUseCase y ReintegroSanitarioScheduler.
- [ ] T041 [US6] Probar reintegro idempotente, recuperación tras indisponibilidad y no sobrescritura de estados diferentes de aislamiento.

**Checkpoint**: Diagnósticos y reintegros respetan la condición sanitaria vigente y dejan disponible el siguiente proceso sin ejecutarlo dentro de este plan.

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Verificar contratos, seguridad, eventos e integración con los planes relacionados.

- [ ] T042 Documentar endpoints, roles, estados y errores en OpenAPI.
- [ ] T043 Completar SanitarioIntegrationTest con el flujo enfermedad → solicitud → validación → diagnóstico.
- [ ] T044 Verificar que el registro de enfermedad no modifica diagnósticos existentes.
- [ ] T045 Verificar que una solicitud no cambia el estado del galpón y que la validación sí utiliza el puerto del Módulo 1.
- [ ] T046 Verificar idempotencia de validación, eventos, outbox y reintegros.
- [ ] T047 Verificar que Plan 006 provea la consulta de medicación sin que Plan 005 persista medicamentos duplicados.
- [ ] T048 Verificar que SacrificioSanitarioHabilitado sea consumido por Plan 008 sin emitir una orden desde este plan.
- [ ] T049 Extender comprobaciones arquitectónicas: dominio sin frameworks, aplicación sin JPA/HTTP y adaptadores sin acceso a repositorios privados ajenos.
- [ ] T050 Ejecutar pruebas y tareas de calidad disponibles, resolviendo primero la herramienta de construcción declarada por el repositorio.
- [ ] T051 Medir los objetivos de 1 segundo y la ejecución del scheduler sin reprocesamiento duplicado.

**Checkpoint**: Flujo sanitario completo, contratos publicados y responsabilidades entre planes verificadas.

## Dependencies & Execution Order

### Phase Dependencies

- **Setup**: Debe resolver roles, estados y la discrepancia de asignación del spec 020.
- **Foundational**: Depende de Setup y habilita todas las historias.
- **US1 y US2**: Dependen de Foundational; definen el catálogo de enfermedades utilizado por diagnósticos.
- **US3 y US4**: Dependen de los puertos de Galpon/Lote y de la regla de ausencia de asignaciones.
- **US5**: Depende de solicitudes persistidas y del puerto para cambiar el estado operativo.
- **US6**: Depende de validación de aislamiento, catálogo de enfermedades y consulta de medicaciones.
- **Polish**: Depende del flujo completo y de los contratos de Plan 006, Plan 008 y Módulo 1.

### Dependencias con otros planes

- **Módulo 1**: Proporciona Galpon/Lote y ejecuta cambios de estado mediante su interfaz pública.
- **Plan 006**: Proporciona medicaciones activas, compatibilidad y duración del tratamiento.
- **Plan 007**: Puede actualizar población viva; las solicitudes capturan la población vigente al momento de emisión.
- **Plan 008**: Consume la habilitación de sacrificio y gestiona orden y ejecución.
- **General.md**: Proporciona seguridad, actor, reloj, errores, eventos y configuración transversal.

### Dentro de cada User Story

- Entidades y reglas de dominio antes que casos de uso.
- Puertos antes que adaptadores.
- Casos de uso antes que controladores o schedulers.
- Persistencia y eventos dentro de la transacción definida.
- Pruebas junto con implementación y checkpoint antes de cerrar la historia.

## Notes

- T001 a T051 identifican tareas; US1 a US6 identifican las historias.
- Este documento describe componentes por implementar; no afirma que ya existan.
- Las solicitudes no se filtran por trabajador ni requieren asignación a un galpón.
- Galpon y Lote siguen siendo entidades del Módulo 1; este plan solo conserva sus referencias y snapshots necesarios.
- La fecha de reintegro se calcula con la cantidad de días vigente al confirmar el diagnóstico.
- Un diagnóstico con sacrificio habilitado no ejecuta el sacrificio ni cambia por sí mismo el estado a VACIADO_SANITARIO.
- Los eventos que cruzan módulos deben tener contrato versionado, consumidor idempotente y trazabilidad con correlationId.
- Antes de implementar debe resolverse la discrepancia entre el spec 020 y la decisión del proyecto sobre asignaciones de trabajadores.

