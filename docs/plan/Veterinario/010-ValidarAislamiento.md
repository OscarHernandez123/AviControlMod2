# Implementation Plan: Validar aislamiento de un galpón

**Date**: 2026-09-22  
**Specs**:  
- [010-ValidarAislamiento.md](docs/specs/010-ValidarAislamiento.md)

---

## 1. Summary

El módulo **Validar aislamiento de un galpón** permite al Médico Veterinario evaluar las solicitudes de revisión de campo emitidas por los trabajadores, consultar de manera síncrona el estado vigente del galpón en el **Módulo 1** y resolver clínicamente la sospecha: confirmando el aislamiento preventivo cuando el estado del galpón sea estrictamente `productiva` o desestimándola mediante un rechazo fundamentado[cite: 4, 8].

**No modifica atributos estructurales del galpón ni descuenta inventarios físicos de bodega**[cite: 4]. La solución emplea **Java 21** con **Spring Boot 3.x (Spring MVC + Spring Data JPA)** bajo **arquitectura hexagonal pura**, el patrón **Transactional Outbox** para despacho garantizado de eventos hacia **Kafka**, control de **Concurrencia Optimista (`@Version`)** sobre la solicitud, trazabilidad inmutable en **`san_auditoria`** y validación de idempotencia técnica vía cabeceras HTTP[cite: 1]. Al confirmarse el aislamiento, el sistema habilita de forma directa la ejecución del caso de uso encadenado **Diagnosticar galpón (`Spec 011`)**[cite: 4, 8].

---

## 2. Technical Context

- **Language/Version**: Java 21 (LTS - Virtual Threads habilitados mediante `spring.threads.virtual.enabled=true`).
- **Primary Dependencies**: Spring Boot 3.x (Spring MVC), Spring Cloud Stream (Kafka Binder), Spring Data JPA (Hibernate 6.x), PostgreSQL JDBC Driver, Lombok, MapStruct, Jakarta Validation, JUnit 5, Mockito, Testcontainers (PostgreSQL + Kafka).
- **Storage**: PostgreSQL 16+ relacional vía JDBC/JPA (`san_solicitudes_revision`, `san_aislamientos`, `san_outbox`, `san_auditoria`).
- **Testing**: JUnit 5, Mockito, MockMvc, Testcontainers.
- **Target Platform**: Contenedores Linux (Docker / Kubernetes).
- **Project Type**: Backend REST micro-service (Módulo 2: Sanidad y Bioseguridad)[cite: 8].
- **Performance Goals**: Latencia < 250 ms en validación y persistencia (`SC-003`); disponibilidad y habilitación inmediata de diagnóstico en < 1 s (`SC-005`)[cite: 1, 4].
- **Constraints**: 
  - Prohibido el borrado físico (`DELETE` SQL) (`FR-013`, `SC-006`)[cite: 1].
  - Escritura atómica obligatoria de entidad + outbox + auditoría (`FR-010`, `FR-011`, `SC-007`)[cite: 1].
  - Rol exclusivo `VETERINARIO` (`FR-001`, `SC-002`)[cite: 1].
  - Validación síncrona obligatoria con el **Módulo 1** antes de confirmar (`FR-005`, `FR-006`).
  - Restricción única sobre `galpon_id` en aislamientos activos para impedir dobles cuarentenas concurrentes.
  - Idempotencia HTTP mediante cabecera `X-Idempotency-Key` retenida por 24 horas (`FR-012`, `SC-008`)[cite: 1].

---

## 3. Project Structure

```text
src/main/java/com/avicontrol/sanidad/
├── domain/                                # Núcleo Puro de Dominio (Sin dependencias Spring/JPA)
│   ├── model/
│   │   └── aislamiento/
│   │       ├── SolicitudRevision.java     # Aggregate Root (Antecedente administrativo)
│   │       ├── AislamientoSanitario.java  # Aggregate Root (Registro clínico de cuarentena)
│   │       ├── SolicitudRevisionId.java   # Value Object UUID
│   │       ├── AislamientoId.java         # Value Object UUID
│   │       ├── GalponId.java              # Value Object UUID (Referencia externa Módulo 1)
│   │       ├── EstadoSolicitud.java       # Enum: PENDIENTE, ATENDIDA, RECHAZADA
│   │       └── JustificacionClinica.java  # Value Object texto >= 10 chars
│   ├── exception/
│   │   └── aislamiento/
│   │       ├── SolicitudNotFoundException.java
│   │       ├── SolicitudNoPendienteException.java
│   │       ├── EstadoGalponIncompatibleException.java
│   │       ├── Modulo1NoDisponibleException.java
│   │       ├── JustificacionRechazoInvalidaException.java
│   │       └── AislamientoConcurrenciaException.java
│   └── repository/                        # Puertos de Salida (Driven Ports)
│       ├── SolicitudRevisionRepositoryPort.java # Persistencia de la solicitud
│       ├── AislamientoRepositoryPort.java       # Persistencia del aislamiento
│       ├── GalponQueryPort.java                 # Consulta y mutación síncrona de estado en Módulo 1
│       ├── OutboxRepositoryPort.java            # Registro local para Transactional Outbox
│       └── AuditoriaSanitariaPort.java          # Bitácora inmutable en san_auditoria
├── application/
│   └── aislamiento/                        # Casos de Uso (Una clase por responsabilidad)
│       ├── ValidarAislamientoUseCase.java
│       ├── RechazarAislamientoUseCase.java
│       └── ConsultarSolicitudesUseCase.java
├── infrastructure/
│   ├── adapter/in/rest/                   # Adaptador Primario REST (Spring MVC)
│   │   ├── ApiErrorResponse.java
│   │   ├── GlobalExceptionHandler.java    # @RestControllerAdvice
│   │   ├── filter/
│   │   │   ├── RoleValidationFilter.java
│   │   │   └── IdempotencyFilter.java
│   │   └── aislamiento/
│   │       ├── AislamientoController.java
│   │       ├── dto/
│   │       │   ├── ValidarAislamientoRequest.java
│   │       │   ├── RechazarAislamientoRequest.java
│   │       │   ├── SolicitudRevisionFiltroRequest.java
│   │       │   ├── SolicitudRevisionResponse.java
│   │       │   └── AislamientoResponse.java
│   │       └── mapper/
│   │           └── AislamientoRestMapper.java
│   ├── adapter/out/
│   │   ├── persistence/                   # Adaptadores Secundarios JPA
│   │   │   ├── solicitud/
│   │   │   │   ├── SolicitudRevisionEntity.java
│   │   │   │   ├── SolicitudRevisionJpaRepository.java
│   │   │   │   ├── SolicitudRevisionRepositoryAdapter.java
│   │   │   │   └── mapper/SolicitudPersistenceMapper.java
│   │   │   ├── aislamiento/
│   │   │   │   ├── AislamientoEntity.java
│   │   │   │   ├── AislamientoJpaRepository.java
│   │   │   │   ├── AislamientoRepositoryAdapter.java
│   │   │   │   └── mapper/AislamientoPersistenceMapper.java
│   │   │   ├── outbox/
│   │   │   │   ├── OutboxEntity.java
│   │   │   │   ├── OutboxJpaRepository.java
│   │   │   │   └── OutboxRepositoryAdapter.java
│   │   │   └── auditoria/
│   │   │       ├── AuditoriaEntity.java
│   │   │       ├── AuditoriaJpaRepository.java
│   │   │       └── AuditoriaRepositoryAdapter.java
│   │   ├── client/                        # Adaptador de Integración con Módulo 1
│   │   │   └── GalponQueryAdapter.java    # Consulta y comando síncrono al Módulo 1
│   │   └── event/                         # Transactional Outbox Relay
│   │       ├── OutboxRelayScheduler.java  # Worker periódico hacia Kafka
│   │       └── KafkaEventPublisherAdapter.java
│   └── config/
│       ├── BeanConfiguration.java         # Registro explícito de casos de uso
│       └── SecurityConfig.java            # Bloqueo estricto de DELETE
└── events/                                # Contratos de Eventos de Integración
    ├── AislamientoValidadoIntegrationEvent.java
    └── AislamientoRechazadoIntegrationEvent.java
```

**Structure Decision**: Microservicio estándar basado en Spring MVC imperativo con Virtual Threads de Java 21, desacoplando el dominio de JPA mediante mappers específicos. La comunicación con el Módulo 1 se realiza exclusivamente a través del puerto `GalponQueryPort` para garantizar el bajo acoplamiento entre contextos delimitados[cite: 1, 8].

---

## 4. Implementation Phases

### Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicializar el proyecto base, configuración estándar de Spring Boot y herramientas de calidad.

- [ ] **T001** Inicializar proyecto Spring Boot 3.x con Java 21 y dependencias estándar (Spring Web, Spring Data JPA, Kafka Stream, Validation, Testcontainers, PostgreSQL Driver).
- [ ] **T002** Generar la estructura de paquetes hexagonal según la convención del proyecto (`domain`, `application`, `infrastructure`, `events`).
- [ ] **T003** Configurar `application.yml` con pool de conexiones HikariCP, parámetros de Kafka (`sanitary.isolation.validated.v1`, `sanitary.isolation.rejected.v1`) y habilitar Virtual Threads (`spring.threads.virtual.enabled=true`).
- [ ] **T004** Configurar Docker Compose local con servicios `postgres:16-alpine` y broker `kafka` (bitnami/kafka:latest con KRaft).
- [ ] **T005** Configurar Checkstyle, SpotBugs y Jacoco con umbral mínimo de cobertura del 85%.
- [ ] **T006** Configurar pipeline de integración continua (CI) en GitHub Actions para compilar, ejecutar tests con Testcontainers y validar linters.

---

### Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Construir el modelo de dominio puro, la persistencia JPA, el Transactional Outbox y la auditoría inmutable.  
⚠️ **CRITICAL**: Ningún caso de uso funcional debe implementarse antes de validar esta fase.

- [ ] **T007** Crear scripts DDL en PostgreSQL para tablas `san_solicitudes_revision`, `san_aislamientos`, `san_outbox` y `san_auditoria` con llaves primarias UUID. Añadir restricción `UNIQUE(galpon_id)` en `san_aislamientos` donde `activo = true` para prevenir aislamientos duplicados (`FR-013`, `SC-008`)[cite: 1].
- [ ] **T008** Definir Value Objects de Dominio con validación estricta:
  - `SolicitudRevisionId.java`: Envoltorio inmutable sobre UUID.
  - `AislamientoId.java`: Envoltorio inmutable sobre UUID.
  - `GalponId.java`: Envoltorio inmutable que representa el identificador externo provisto por el Módulo 1.
  - `EstadoSolicitud.java`: Enum con valores `PENDIENTE`, `ATENDIDA`, `RECHAZADA`.
  - `JustificacionClinica.java`: Texto obligatorio, mínimo 10 caracteres tras normalización con `.trim()`.
- [ ] **T009** Crear Aggregate Roots:
  - `SolicitudRevision.java`: Atributos `id`, `galponId`, `trabajadorId`, `estado`, `observacionesTrabajador`, `justificacionRechazo`, `fechaSolicitud`, `version`. Métodos de mutación controlada `marcarComoAtendida()` y `marcarComoRechazada(JustificacionClinica justificacion)`[cite: 4].
  - `AislamientoSanitario.java`: Atributos `id`, `solicitudId`, `galponId`, `veterinarioId`, `observacionesClinicas`, `fechaInicio`, `activo`. Método de fábrica `crear(...)`.
- [ ] **T010** Crear excepciones de dominio en `domain/exception/aislamiento/` (`SolicitudNotFoundException`, `SolicitudNoPendienteException`, `EstadoGalponIncompatibleException`, `Modulo1NoDisponibleException`, `JustificacionRechazoInvalidaException`, `AislamientoConcurrenciaException`).
- [ ] **T011** Definir puertos secundarios en `domain/repository/`:
  - `SolicitudRevisionRepositoryPort.java`: métodos `guardar(SolicitudRevision s)`, `buscarPorId(SolicitudRevisionId id)`, `listarPendientes()`.
  - `AislamientoRepositoryPort.java`: métodos `guardar(AislamientoSanitario a)`, `buscarPorGalponId(GalponId id)`, `existeActivoPorGalponId(GalponId id)`.
  - `GalponQueryPort.java`: métodos `String obtenerEstadoVigente(GalponId id)`, `void actualizarEstado(GalponId id, String nuevoEstado)`.
  - `OutboxRepositoryPort.java`: método `guardarEvento(OutboxEntity evento)`.
  - `AuditoriaSanitariaPort.java`: método inmutable `registrarTraza(AuditoriaEntity traza)`.
- [ ] **T012** Implementar entidades JPA (`SolicitudRevisionEntity.java`, `AislamientoEntity.java`, `OutboxEntity.java`, `AuditoriaEntity.java`) y repositorios `JpaRepository`.
- [ ] **T013** Implementar adaptadores JPA que cumplan los puertos secundarios garantizando el mapeo de dominio vía `AislamientoPersistenceMapper` y `SolicitudPersistenceMapper`.
- [ ] **T014** Implementar `GlobalExceptionHandler` con `@RestControllerAdvice` para transformar excepciones de dominio y validación a esquemas RFC-7807 (`ApiErrorResponse`) con códigos HTTP adecuados (400, 403, 404, 409, 503).
- [ ] **T015** Implementar `IdempotencyFilter` (`OncePerRequestFilter`) para validar y cachear respuestas asociadas a la cabecera `X-Idempotency-Key` durante 24 horas (`FR-012`)[cite: 1].
- [ ] **T016** Implementar `RoleValidationFilter` para verificar el claim `VETERINARIO` en todas las peticiones a `/api/v1/sanitary/aislamientos/**` (`FR-001`)[cite: 1].
- **Checkpoint**: Modelo de dominio puro compilando, tablas creadas, infraestructura JPA operativa y filtros transversales listos.

---

## Phase 3: User Story 1 – Evaluación y Confirmación de Aislamiento Sanitario (P1)

**Goal**: Permitir al Médico Veterinario evaluar una solicitud de revisión pendiente, validar el estado `productiva` contra el Módulo 1, y confirmar el aislamiento del galpón o desestimarlo, persistiendo atómicamente la entidad, la auditoría y el evento Outbox, habilitando además el diagnóstico posterior (`Spec 011`)[cite: 4, 8].  
**Independent Test**: `POST /api/v1/sanitary/aislamientos/validar` con datos válidos y rol `VETERINARIO` retorna `201 Created`[cite: 1]. El estado del galpón en el Módulo 1 cambia a `aislamiento`, la solicitud pasa a `ATENDIDA`, se genera traza en `san_auditoria`, el evento `AislamientoValidadoIntegrationEvent` se persiste en `san_outbox` y el sistema habilita el flujo de diagnóstico clínico (`Spec 011`)[cite: 4, 8].

### Tests para User Story 1

- [ ] **T017** [P] [US1] Test de contrato: `POST /api/v1/sanitary/aislamientos/validar` con solicitud `PENDIENTE` y galpón `productiva` → HTTP 201 Created con `AislamientoResponse` — `AislamientoControllerTest.java`.
- [ ] **T018** [P] [US1] Test de contrato: `POST /api/v1/sanitary/aislamientos/validar` sin rol `VETERINARIO` → HTTP 403 Forbidden — `AislamientoControllerTest.java`[cite: 1].
- [ ] **T019** [P] [US1] Test de contrato: `POST /api/v1/sanitary/aislamientos/validar` con galpón en estado distinto a `productiva` (ej. `en_cosecha`) → HTTP 409 Conflict (`EstadoGalponIncompatibleException`) — `AislamientoControllerTest.java`.
- [ ] **T020** [P] [US1] Test de contrato: `POST /api/v1/sanitary/aislamientos/rechazar` con justificación < 10 caracteres → HTTP 400 Bad Request — `AislamientoControllerTest.java`.
- [ ] **T021** [P] [US1] Test unitario de `ValidarAislamientoUseCase` validando orquestación, consulta al Módulo 1, persistencia atómica y generación de evento — `ValidarAislamientoUseCaseTest.java`.
- [ ] **T022** [P] [US1] Test unitario de `RechazarAislamientoUseCase` comprobando actualización a `RECHAZADA` y conservación de estado en Módulo 1 — `RechazarAislamientoUseCaseTest.java`[cite: 4].
- [ ] **T023** [P] [US1] Test de integración con Testcontainers (PostgreSQL JDBC): Confirmar atomicidad transaccional con `@Transactional` (si falla auditoría u outbox, la solicitud y el galpón no se alteran)[cite: 1].

### Implementación de User Story 1

- [ ] **T024** [US1] Crear DTOs de entrada y salida: `ValidarAislamientoRequest.java` (`solicitudId`, `observacionesClinicas`), `RechazarAislamientoRequest.java` (`solicitudId`, `justificacion` con validación `@Size(min=10)`), `AislamientoResponse.java` y `SolicitudRevisionResponse.java`.
- [ ] **T025** [US1] Definir contratos de eventos `AislamientoValidadoIntegrationEvent.java` y `AislamientoRechazadoIntegrationEvent.java` con schema JSON normalizado (`eventId`, `aggregateId`, `galponId`, `veterinarioId`, `occurredOn`).
- [ ] **T026** [US1] Implementar `GalponQueryAdapter` que realice la llamada síncrona al Módulo 1 para verificar el estado vigente y solicitar la transición a `aislamiento` en caso de confirmación.
- [ ] **T027** [US1] Implementar `ValidarAislamientoUseCase.java` en `application/aislamiento/`:
  - Validar claim de rol `VETERINARIO`[cite: 1].
  - Obtener `SolicitudRevision` por ID; si no existe lanzar `SolicitudNotFoundException` y si no está en `PENDIENTE` lanzar `SolicitudNoPendienteException`.
  - Consultar `GalponQueryPort.obtenerEstadoVigente(galponId)`; si es distinto a `productiva` lanzar `EstadoGalponIncompatibleException`.
  - Mutar aggregate root `solicitud.marcarComoAtendida()`.
  - Instanciar aggregate root `AislamientoSanitario.crear(...)`.
  - Invocar `GalponQueryPort.actualizarEstado(galponId, "aislamiento")`.
  - Ejecutar `@Transactional`: persistir entidades, encolar `AislamientoValidadoIntegrationEvent` en `san_outbox` y registrar firma profesional en `san_auditoria`[cite: 1].
  - Retornar `AislamientoResponse` conteniendo el flag que habilita la navegación inmediata al diagnóstico (`Spec 011`)[cite: 4, 8].
- [ ] **T028** [US1] Implementar `RechazarAislamientoUseCase.java` en `application/aislamiento/`:
  - Validar rol `VETERINARIO`[cite: 1].
  - Validar que la justificación cumpla $\ge 10$ caracteres vía Value Object.
  - Mutar solicitud a `RECHAZADA`.
  - Ejecutar `@Transactional`: actualizar solicitud, encolar `AislamientoRechazadoIntegrationEvent` en `san_outbox` y asentar traza técnica en `san_auditoria` manteniendo inalterado el galpón en el Módulo 1[cite: 4].
- [ ] **T029** [US1] Implementar endpoints `POST /api/v1/sanitary/aislamientos/validar` y `POST /api/v1/sanitary/aislamientos/rechazar` en `AislamientoController.java`.
- **Checkpoint**: US1 completamente funcional — validación y rechazo de aislamiento operativos, atómicos y auditados.

---

## Phase 4: User Story 2 – Idempotencia y Blindaje de Trazabilidad Operativa (P2)

**Goal**: Asegurar que la validación del aislamiento se procese de manera estrictamente idempotente y atómica, previniendo dobles aislamientos, eventos redundantes hacia Kafka o transiciones duplicadas ante reconexiones de red[cite: 1].  
**Independent Test**: Enviar el mismo comando de confirmación de aislamiento dos veces consecutivas bajo la misma cabecera `X-Idempotency-Key`[cite: 1]. Se verifica que el segundo llamado retorna la respuesta HTTP idéntica almacenada en caché sin volver a mutar el Módulo 1, sin duplicar registros en `san_auditoria` y sin registrar mensajes redundantes en `san_outbox`[cite: 1].

### Tests para User Story 2

- [ ] **T030** [P] [US2] Test de contrato: `POST /api/v1/sanitary/aislamientos/validar` con una `X-Idempotency-Key` ya procesada retorna la respuesta original sin duplicar entidades — `AislamientoControllerTest.java`[cite: 1].
- [ ] **T031** [P] [US2] Test de contrato: `POST /api/v1/sanitary/aislamientos/validar` sobre un galpón que ya está en estado `aislamiento` retorna HTTP 409 Conflict — `AislamientoControllerTest.java`.
- [ ] **T032** [P] [US2] Test de integración con Testcontainers: verificar que la ejecución concurrente de dos veterinarios sobre la misma solicitud provoque colisión optimista (`AislamientoConcurrenciaException`) en la segunda transacción.

### Implementación de User Story 2

- [ ] **T033** [US2] Configurar `IdempotencyFilter` para almacenar en caché la respuesta de los endpoints de resolución clínica (`/validar` y `/rechazar`) mapeada a la cabecera `X-Idempotency-Key` (retención de 24 horas).
- [ ] **T034** [US2] Añadir anotación `@Version` en `SolicitudRevisionEntity.java` y verificar en los casos de uso que la versión optimista coincida antes de commitear la transición.
- [ ] **T035** [US2] Implementar `ConsultarSolicitudesUseCase.java` en `application/aislamiento/` para obtener las solicitudes de revisión, soportando paginación y filtros por estado (`PENDIENTE`, `ATENDIDA`, `RECHAZADA`)[cite: 4, 8].
- [ ] **T036** [US2] Añadir endpoint `GET /api/v1/sanitary/aislamientos/solicitudes` y `GET /api/v1/sanitary/aislamientos/solicitudes/{id}` en `AislamientoController.java` para poblar la bandeja de entrada del Veterinario[cite: 4, 8].
- **Checkpoint**: US1 y US2 funcionales — validación, rechazo, idempotencia y consulta de solicitudes operativos.

---

## Phase 5: Integridad Transaccional, Outbox Relay e Inmutabilidad (P3)

**Goal**: Asegurar que los registros de aislamiento no se puedan eliminar físicamente (`DELETE`), despachar eventos hacia Kafka desde la tabla Outbox de forma resiliente y garantizar la entrega confiable[cite: 1].  
**Independent Test**: Ejecutar `DELETE /api/v1/sanitary/aislamientos/{id}` retorna `405 Method Not Allowed`[cite: 1]. Se verifica mediante worker en background que los eventos en `san_outbox` en estado `PENDING` se publiquen en Kafka y cambien a `PROCESSED`[cite: 1].

### Tests para User Story 3

- [ ] **T037** [P] [US3] Test de contrato y seguridad: Comprobar rechazo a llamadas HTTP `DELETE` → 405 Method Not Allowed — `AislamientoControllerTest.java`[cite: 1].
- [ ] **T038** [P] [US3] Test de base de datos: Verificar mediante test de repositorio la inexistencia de sentencias o métodos de borrado físico directo (`DELETE`) en la capa de persistencia[cite: 1].
- [ ] **T039** [P] [US3] Test de integración Outbox Relay con Testcontainers (Kafka): Verificar lectura por lotes de `san_outbox` y publicación efectiva en el tópico `sanitary.isolation.validated.v1`[cite: 1].

### Implementación de User Story 3

- [ ] **T040** [US3] Configurar `SecurityConfig.java` bloqueando explícitamente cualquier verbo `DELETE` sobre rutas `/api/v1/sanitary/**` (`FR-013`)[cite: 1].
- [ ] **T041** [US3] Implementar `KafkaEventPublisherAdapter.java` publicando mensajes tipados hacia los tópicos de Kafka mediante `StreamBridge` o `KafkaTemplate`[cite: 1].
- [ ] **T042** [US3] Implementar `OutboxRelayScheduler.java`:
  - Polling periódico con `@Scheduled(fixedDelay = 2000)` sobre `san_outbox` donde `status = 'PENDING'` usando `SELECT ... FOR UPDATE SKIP LOCKED`[cite: 1].
  - Despacho a Kafka mediante `KafkaEventPublisherAdapter`[cite: 1].
  - Actualización atómica de estado a `PROCESSED` con marca temporal `processed_at`[cite: 1].
  - Manejo de reintentos y marcado a `FAILED` si supera 5 reintentos con backoff exponencial[cite: 1].
- [ ] **T043** [US3] Documentar contratos de mensajería asíncrona mediante especificación AsyncAPI 3.0 en `docs/asyncapi/sanitary-events.yml`[cite: 1].
- [ ] **T044** [US3] Configurar especificación OpenAPI 3.0 (Swagger UI) exponiendo documentación de endpoints REST[cite: 1].
- **Checkpoint**: Sistema de mensajería Outbox confiable, tolerancia a caídas de red y blindaje absoluto contra borrado físico.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] **T045** Configurar logging estructurado en formato JSON incorporando `traceId`, `spanId` y `correlationId` vía MDC de Slf4j[cite: 1].
- [ ] **T046** Exponer métricas Prometheus con Micrometer (`sanitary_isolation_validated_total`, `sanitary_isolation_rejected_total`, `sanitary_outbox_lag_seconds`).
- [ ] **T047** Implementar pruebas de carga con Gatling/k6 validando latencia < 250 ms bajo concurrencia sostenida de 200 req/s[cite: 1].
- [ ] **T048** Auditoría de dependencias: Validar mediante ArchUnit que el paquete `domain/` mantenga cero imports de Spring, JPA/Hibernate, Jackson o librerías externas[cite: 1].
- [ ] **T049** Verificar correspondencia campo a campo entre el DTO de respuesta y la vista Figma importada (`docs/prototype/gestion-sanitaria/aislamiento-y-diagnostico/`)[cite: 4].

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: Sin dependencias — inicia de inmediato.
- **Phase 2 (Foundational)**: Requiere Fase 1 completa — **bloquea todas las user stories**.
- **Phase 3 (User Story 1)**: Requiere Fase 2 completada (bloqueante).
- **Phase 4 (User Story 2)**: Requiere Fase 3 (necesita existir la lógica de validación base).
- **Phase 5 (Integridad / Outbox Relay)**: Puede desarrollarse en paralelo con Fase 4 tras concluir Fase 3.
- **Phase 6 (Polish)**: Requiere todas las fases funcionales implementadas.

### User Story Dependencies

- **US1 (Validación/Rechazo)**: Depende de infraestructura base, `GalponQueryPort` y persistencia de solicitudes.
- **US2 (Idempotencia/Consulta)**: Depende de US1 (la entidad de aislamiento debe existir para validar idempotencia y estados).
- **US3 (Outbox & Integridad)**: Transversal a US1 y US2; procesa los eventos generados por ambas historias[cite: 1].

---

## Traceability Matrix (Spec 010 vs Implementation Plan)

| **Requerimiento Spec 010** | **Tarea(s) en Implementation Plan** | **Componente Técnico Responsable** |
|---|---|---|
| **FR-001** (Exclusivo Veterinario)[cite: 1] | T016, T018, T027 | `RoleValidationFilter`, `ValidarAislamientoUseCase` |
| **FR-002** (Requiere solicitud previa del trabajador)[cite: 4, 8] | T011, T017, T027 | `SolicitudRevisionRepositoryPort`, `ValidarAislamientoUseCase` |
| **FR-003** (La solicitud no modifica el galpón)[cite: 4] | T009, T027 | `SolicitudRevision.java` (separación de ciclo de vida) |
| **FR-004** (Asociación a galpón del Módulo 1) | T008, T011, T026 | `GalponId.java`, `GalponQueryPort` |
| **FR-005** (Validación síncrona del estado productiva) | T011, T026, T027 | `GalponQueryAdapter`, `ValidarAislamientoUseCase` |
| **FR-006** (Rechazo si estado != productiva) | T010, T019, T027 | `EstadoGalponIncompatibleException`, `GlobalExceptionHandler` |
| **FR-007** (Rechazo clínico con justificación $\ge 10$ chars)[cite: 4] | T010, T020, T028 | `RechazarAislamientoUseCase`, `JustificacionClinica` |
| **FR-008** (Confirmación: galpón a aislamiento y paso a Spec 011)[cite: 4, 8] | T009, T027 | `AislamientoSanitario.java`, `ValidarAislamientoUseCase` |
| **FR-009** (No altera atributos estructurales ni bodega)[cite: 4] | T026, T027 | Invocación de solo estado en `GalponQueryPort` |
| **FR-010** (Evento Outbox Kafka)[cite: 1] | T025, T027, T042 | `san_outbox`, `OutboxRelayScheduler`, Kafka Topic |
| **FR-011** (Trazabilidad auditoría)[cite: 1] | T007, T013, T027 | `san_auditoria`, `AuditoriaSanitariaPort` |
| **FR-012** (Concurrencia e Idempotencia)[cite: 1] | T015, T030, T034 | `IdempotencyFilter`, `@Version` en entidad JPA |
| **FR-013** (Prohibido borrado físico)[cite: 1] | T037, T038, T040 | `SecurityConfig`, ausencia de sentencias SQL `DELETE` |
| **SC-001 a SC-008** (Métricas de éxito)[cite: 1, 4] | T021, T039, T046, T047 | Pruebas de integración, métricas Prometheus y Jacoco |

---

## Notes

- Cada tarea cuenta con su identificador único `T0xx` para seguimiento en tableros Kanban o Jira.
- Las escrituras que involucran `san_solicitudes_revision`, `san_aislamientos`, `san_outbox` y `san_auditoria` se ejecutan dentro del mismo bloque `@Transactional` imperativo de Spring Data JPA[cite: 1].
- El uso de **Virtual Threads** (Project Loom) permite manejar la concurrencia de peticiones HTTP de forma eficiente sin la complejidad de la programación reactiva, manteniendo el modelo imperativo de Spring MVC[cite: 1].
- La comunicación con el Módulo 1 se realiza exclusivamente a través del puerto `GalponQueryPort` para garantizar el desacoplamiento de bounded contexts[cite: 8].
- La habilitación automática del caso de uso incluido **Diagnosticar galpón (`Spec 011`)** se realiza mediante el retorno en el payload de confirmación del estado operativo actualizado, permitiendo al frontend la redirección directa al expediente de diagnóstico clínico[cite: 4, 8].