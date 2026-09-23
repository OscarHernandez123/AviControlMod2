# Implementation Plan: Ordenar sacrificio sanitario

**Date**: 2026-09-22  
**Specs**:  
- [012-OrdenarSacrificioSanitario.md](docs/specs/012-OrdenarSacrificioSanitario.md)

---

## 1. Summary

El módulo **Ordenar sacrificio sanitario** permite al Médico Veterinario emitir y confirmar la orden de erradicación biológica total de un lote cuando el diagnóstico clínico (`Spec 011`) ha identificado una enfermedad con `requiereSacrificioSanitario == true` (`Spec 009`). El procedimiento se compone de **dos etapas secuenciales desacopladas**:

- **Etapa 1 — Emisión** (`PENDIENTE`): La orden se crea sin alterar la población del lote ni el estado del galpón en el Módulo 1.
- **Etapa 2 — Confirmación** (`EJECUTADA`): Se fija la población del lote en cero (`0`) y se transiciona el galpón a `vaciado_sanitario` de forma atómica.

La solución emplea **Java 21** con **Spring Boot 3.x (Spring MVC + Spring Data JPA)** bajo **arquitectura hexagonal pura**, patrón **Transactional Outbox** hacia **Kafka**, control de **Concurrencia Optimista (`@Version`)**, trazabilidad inmutable en **`san_auditoria`** y validación de idempotencia técnica vía `X-Idempotency-Key`. **No admite sacrificios parciales ni descuenta inventarios de bodega**.

---

## 2. Technical Context

- **Language/Version**: Java 21 (LTS - Virtual Threads habilitados mediante `spring.threads.virtual.enabled=true`).
- **Primary Dependencies**: Spring Boot 3.x (Spring MVC), Spring Cloud Stream (Kafka Binder), Spring Data JPA (Hibernate 6.x), PostgreSQL JDBC Driver, Lombok, MapStruct, Jakarta Validation, JUnit 5, Mockito, Testcontainers (PostgreSQL + Kafka).
- **Storage**: PostgreSQL 16+ relacional vía JDBC/JPA (`san_ordenes_sacrificio`, `san_outbox`, `san_auditoria`).
- **Testing**: JUnit 5, Mockito, MockMvc, Testcontainers.
- **Target Platform**: Contenedores Linux (Docker / Kubernetes).
- **Project Type**: Backend REST micro-service (Módulo 2: Sanidad y Bioseguridad).
- **Performance Goals**: Latencia < 1 s en confirmación (`SC-006`); disponibilidad de consulta < 1 s.
- **Constraints**: 
  - Prohibido el borrado físico (`DELETE` SQL) (`FR-014`, `SC-008`).
  - Escritura atómica obligatoria: orden + outbox + auditoría (`FR-011`, `FR-013`, `SC-009`).
  - Rol exclusivo `VETERINARIO` (`FR-001`, `SC-005`).
  - Validación síncrona obligatoria contra Módulo 1 (estado `aislamiento` y `poblacion > 0`) (`FR-003`, `FR-004`, `FR-007`).
  - Alcance `TOTAL` no negociable (`FR-016`, `SC-002`).
  - Idempotencia HTTP mediante `X-Idempotency-Key` retenida 24 horas (`FR-015`, `SC-007`).

---

## 3. Project Structure

```text
src/main/java/com/avicontrol/sanidad/
├── domain/                                # Núcleo Puro de Dominio (Sin dependencias Spring/JPA)
│   ├── model/
│   │   └── sacrificio/
│   │       ├── OrdenSacrificioSanitario.java  # Aggregate Root
│   │       ├── OrdenSacrificioId.java         # Value Object UUID
│   │       ├── DiagnosticoId.java             # Value Object UUID (Ref. Spec 011)
│   │       ├── GalponId.java                  # Value Object UUID (Ref. Módulo 1)
│   │       ├── LoteId.java                    # Value Object UUID (Ref. Módulo 1)
│   │       ├── AlcanceOrden.java              # Enum: TOTAL (único valor admisible)
│   │       ├── EstadoOrden.java               # Enum: PENDIENTE, EJECUTADA
│   │       └── ObservacionBioseguridad.java   # Value Object texto >= 10 chars
│   ├── exception/
│   │   └── sacrificio/
│   │       ├── OrdenSacrificioNotFoundException.java
│   │       ├── EnfermedadNoRequiereSacrificioException.java
│   │       ├── EstadoGalponIncompatibleException.java
│   │       ├── LoteSinPoblacionException.java
│   │       ├── OrdenYaEjecutadaException.java
│   │       ├── Modulo1NoDisponibleException.java
│   │       └── OrdenSacrificioConcurrenciaException.java
│   └── repository/                        # Puertos de Salida (Driven Ports)
│       ├── OrdenSacrificioRepositoryPort.java # Persistencia del Agregado
│       ├── DiagnosticoQueryPort.java          # Consulta al expediente clínico (Spec 011)
│       ├── EnfermedadQueryPort.java           # Consulta al catálogo nosológico (Spec 009)
│       ├── GalponQueryPort.java               # Consulta y mutación de estado en Módulo 1
│       ├── LoteQueryPort.java                 # Consulta y mutación de población en Módulo 1
│       ├── OutboxRepositoryPort.java          # Registro local para Transactional Outbox
│       └── AuditoriaSanitariaPort.java        # Bitácora inmutable en san_auditoria
├── application/
│   └── sacrificio/                        # Casos de Uso (Una clase por responsabilidad)
│       ├── EmitirOrdenSacrificioUseCase.java
│       ├── ConfirmarSacrificioUseCase.java
│       └── ConsultarOrdenSacrificioUseCase.java
├── infrastructure/
│   ├── adapter/in/rest/                   # Adaptador Primario REST (Spring MVC)
│   │   ├── ApiErrorResponse.java
│   │   ├── GlobalExceptionHandler.java    # @RestControllerAdvice
│   │   ├── filter/
│   │   │   ├── RoleValidationFilter.java
│   │   │   └── IdempotencyFilter.java
│   │   └── sacrificio/
│   │       ├── OrdenSacrificioController.java
│   │       ├── dto/
│   │       │   ├── EmitirOrdenSacrificioRequest.java
│   │       │   ├── ConfirmarSacrificioRequest.java
│   │       │   ├── OrdenSacrificioResponse.java
│   │       │   └── OrdenSacrificioFiltroRequest.java
│   │       └── mapper/
│   │           └── OrdenSacrificioRestMapper.java
│   ├── adapter/out/
│   │   ├── persistence/                   # Adaptadores Secundarios JPA
│   │   │   ├── sacrificio/
│   │   │   │   ├── OrdenSacrificioEntity.java
│   │   │   │   ├── OrdenSacrificioJpaRepository.java
│   │   │   │   ├── OrdenSacrificioRepositoryAdapter.java
│   │   │   │   └── mapper/OrdenSacrificioPersistenceMapper.java
│   │   │   ├── outbox/
│   │   │   │   ├── OutboxEntity.java
│   │   │   │   ├── OutboxJpaRepository.java
│   │   │   │   └── OutboxRepositoryAdapter.java
│   │   │   └── auditoria/
│   │   │       ├── AuditoriaEntity.java
│   │   │       ├── AuditoriaJpaRepository.java
│   │   │       └── AuditoriaRepositoryAdapter.java
│   │   ├── client/                        # Adaptadores Cross-Context (Módulo 1 y Specs)
│   │   │   ├── DiagnosticoQueryAdapter.java
│   │   │   ├── EnfermedadQueryAdapter.java
│   │   │   ├── GalponQueryAdapter.java
│   │   │   └── LoteQueryAdapter.java
│   │   └── event/                         # Transactional Outbox Relay
│   │       ├── OutboxRelayScheduler.java  # Polling worker hacia Kafka
│   │       └── KafkaEventPublisherAdapter.java
│   └── config/
│       ├── BeanConfiguration.java         # Registro explícito de casos de uso
│       └── SecurityConfig.java            # Bloqueo estricto de DELETE
└── events/                                # Contratos de Eventos de Integración
    ├── OrdenSacrificioEmitidaIntegrationEvent.java
    └── SacrificioEjecutadoIntegrationEvent.java
```

**Structure Decision**: Microservicio estándar basado en Spring MVC imperativo con Virtual Threads de Java 21, desacoplando el dominio de JPA mediante mappers específicos. La comunicación con el Módulo 1 se aísla en los puertos `GalponQueryPort` y `LoteQueryPort`. El ciclo de vida de la orden se modela dentro del Aggregate Root `OrdenSacrificioSanitario`, garantizando la invariante del alcance `TOTAL`.

---

## 4. Implementation Phases

### Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicializar el proyecto base, configuración estándar de Spring Boot y herramientas de calidad.

- [ ] **T001** Inicializar proyecto Spring Boot 3.x con Java 21 y dependencias estándar (Spring Web, Spring Data JPA, Kafka Stream, Validation, Testcontainers, PostgreSQL Driver).
- [ ] **T002** Generar la estructura de paquetes hexagonal según la convención del proyecto (`domain`, `application`, `infrastructure`, `events`).
- [ ] **T003** Configurar `application.yml` con pool HikariCP, parámetros de Kafka (`sanitary.sacrifice.issued.v1`, `sanitary.sacrifice.executed.v1`) y habilitar Virtual Threads (`spring.threads.virtual.enabled=true`).
- [ ] **T004** Configurar Docker Compose local con servicios `postgres:16-alpine` y broker `kafka` (bitnami/kafka:latest con KRaft).
- [ ] **T005** Configurar Checkstyle, SpotBugs y Jacoco con umbral mínimo de cobertura del 85%.
- [ ] **T006** Configurar pipeline de integración continua (CI) en GitHub Actions para compilar, ejecutar tests con Testcontainers y validar linters.

---

### Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Construir el modelo de dominio puro, la persistencia JPA, el Transactional Outbox y la auditoría inmutable.  
⚠️ **CRITICAL**: Ningún caso de uso funcional debe implementarse antes de validar esta fase.

- [ ] **T007** Crear scripts DDL en PostgreSQL para tablas `san_ordenes_sacrificio`, `san_outbox` y `san_auditoria` con llaves primarias UUID. Añadir índice único parcial `CREATE UNIQUE INDEX uq_orden_pendiente_lote ON san_ordenes_sacrificio(lote_id) WHERE estado = 'PENDIENTE'` para garantizar que un lote no tenga dos órdenes pendientes simultáneas (`FR-005`, `FR-016`).
- [ ] **T008** Definir Value Objects de Dominio con validación estricta:
  - `OrdenSacrificioId.java`: Envoltorio inmutable sobre UUID.
  - `DiagnosticoId.java`: Referencia al expediente clínico (`Spec 011`).
  - `GalponId.java`: Referencia externa al Módulo 1.
  - `LoteId.java`: Referencia externa al Módulo 1.
  - `AlcanceOrden.java`: Enum con único valor `TOTAL` (invariante no negociable).
  - `EstadoOrden.java`: Enum `PENDIENTE`, `EJECUTADA`.
  - `ObservacionBioseguridad.java`: Texto obligatorio, mínimo 10 caracteres tras `.trim()`.
- [ ] **T009** Crear Aggregate Root `OrdenSacrificioSanitario.java` con constructor privado y métodos de fábrica estáticos:
  - `emitir(...)`: Crea la orden en estado `PENDIENTE`, `alcance = TOTAL`, con `fechaEmision = Instant.now()`. Valida que la observación de emisión cumpla la longitud mínima.
  - `confirmarEjecucion(ObservacionBioseguridad obs)`: Transiciona el estado a `EJECUTADA`, asigna `fechaEjecucion = Instant.now()` y registra la observación. Falla con `OrdenYaEjecutadaException` si la orden ya estaba ejecutada.
- [ ] **T010** Crear excepciones de dominio en `domain/exception/sacrificio/` (`OrdenSacrificioNotFoundException`, `EnfermedadNoRequiereSacrificioException`, `EstadoGalponIncompatibleException`, `LoteSinPoblacionException`, `OrdenYaEjecutadaException`, `Modulo1NoDisponibleException`, `OrdenSacrificioConcurrenciaException`).
- [ ] **T011** Definir puertos secundarios en `domain/repository/`:
  - `OrdenSacrificioRepositoryPort.java`: `guardar(OrdenSacrificioSanitario o)`, `buscarPorId(OrdenSacrificioId id)`, `buscarPendientePorLote(LoteId id)`, `listarOrdenes(OrdenSacrificioFiltro filtro)`.
  - `DiagnosticoQueryPort.java`: `boolean existeDiagnosticoVigente(DiagnosticoId id)`, `UUID obtenerEnfermedadId(DiagnosticoId id)`.
  - `EnfermedadQueryPort.java`: `boolean requiereSacrificioSanitario(UUID enfermedadId)`.
  - `GalponQueryPort.java`: `String obtenerEstadoVigente(GalponId id)`, `void actualizarEstado(GalponId id, String nuevoEstado)`.
  - `LoteQueryPort.java`: `long obtenerPoblacionActual(LoteId id)`, `void fijarPoblacionEnCero(LoteId id)`.
  - `OutboxRepositoryPort.java`: `guardarEvento(OutboxEntity evento)`.
  - `AuditoriaSanitariaPort.java`: `registrarTraza(AuditoriaEntity traza)`.
- [ ] **T012** Implementar entidades JPA (`OrdenSacrificioEntity.java`, `OutboxEntity.java`, `AuditoriaEntity.java`) y repositorios `JpaRepository`.
- [ ] **T013** Implementar adaptadores JPA que cumplan los puertos secundarios garantizando el mapeo de dominio vía `OrdenSacrificioPersistenceMapper`.
- [ ] **T014** Implementar `GlobalExceptionHandler` con `@RestControllerAdvice` para transformar excepciones de dominio a esquemas RFC-7807 (`ApiErrorResponse`) con códigos HTTP adecuados (400, 403, 404, 409, 503).
- [ ] **T015** Implementar `IdempotencyFilter` (`OncePerRequestFilter`) para cachear respuestas asociadas a `X-Idempotency-Key` durante 24 horas (`FR-015`).
- [ ] **T016** Implementar `RoleValidationFilter` para verificar el claim `VETERINARIO` en todas las peticiones a `/api/v1/sanitary/sacrificios/**` (`FR-001`).
- **Checkpoint**: Modelo de dominio puro compilando, tablas creadas, infraestructura JPA operativa y filtros transversales listos.

---

## Phase 3: User Story 1 – Emisión de la Orden de Sacrificio Sanitario Total (P1)

**Goal**: Permitir al Veterinario emitir la orden de sacrificio total asociada a un diagnóstico mortal vigente, persistiendo atómicamente la entidad, la auditoría y el evento Outbox sin alterar la población del lote ni el estado del galpón.  
**Independent Test**: `POST /api/v1/sanitary/sacrificios` con datos válidos y rol `VETERINARIO` retorna `201 Created` con la orden en estado `PENDIENTE`. La población del lote y el estado del galpón permanecen intactos en el Módulo 1.

### Tests para User Story 1

- [ ] **T017** [P] [US1] Test de contrato: `POST /api/v1/sanitary/sacrificios` con diagnóstico mortal vigente y galpón en aislamiento retorna HTTP 201 con la orden `PENDIENTE` — `OrdenSacrificioControllerTest.java`.
- [ ] **T018** [P] [US1] Test de contrato: `POST` sobre enfermedad con `requiereSacrificioSanitario == false` retorna HTTP 409 (`EnfermedadNoRequiereSacrificioException`) — `OrdenSacrificioControllerTest.java`.
- [ ] **T019** [P] [US1] Test de contrato: `POST` sobre galpón con estado distinto a aislamiento retorna HTTP 409 (`EstadoGalponIncompatibleException`) — `OrdenSacrificioControllerTest.java`.
- [ ] **T020** [P] [US1] Test de contrato: `POST` sobre lote con población cero retorna HTTP 409 (`LoteSinPoblacionException`) — `OrdenSacrificioControllerTest.java`.
- [ ] **T021** [P] [US1] Test de contrato: `POST` sin rol `VETERINARIO` retorna HTTP 403 Forbidden — `OrdenSacrificioControllerTest.java`.
- [ ] **T022** [P] [US1] Test unitario de `EmitirOrdenSacrificioUseCase` verificando orquestación de puertos y la invariante del alcance `TOTAL` — `EmitirOrdenSacrificioUseCaseTest.java`.
- [ ] **T023** [P] [US1] Test de integración con Testcontainers (PostgreSQL): confirmar atomicidad transaccional (si falla auditoría u outbox, la orden no se persiste).

### Implementación de User Story 1

- [ ] **T024** [US1] Crear DTOs de entrada y salida: `EmitirOrdenSacrificioRequest.java` (con `diagnosticoId`, observaciones, validaciones Jakarta `@NotBlank`, `@Size(min=10)`) y `OrdenSacrificioResponse.java`.
- [ ] **T025** [US1] Definir contrato de evento `OrdenSacrificioEmitidaIntegrationEvent.java` con schema JSON normalizado (`eventId`, `aggregateId`, `diagnosticoId`, `galponId`, `loteId`, `veterinarioId`, `occurredOn`).
- [ ] **T026** [US1] Implementar `DiagnosticoQueryAdapter`, `EnfermedadQueryAdapter`, `GalponQueryAdapter` y `LoteQueryAdapter` como adaptadores de salida (solo lectura en esta fase).
- [ ] **T027** [US1] Implementar `EmitirOrdenSacrificioUseCase.java` en `application/sacrificio/`:
  - Validar claim `VETERINARIO`.
  - Consultar `DiagnosticoQueryPort.existeDiagnosticoVigente(diagnosticoId)`; abortar si no existe.
  - Obtener `enfermedadId` y verificar con `EnfermedadQueryPort.requiereSacrificioSanitario(...)`; abortar si es false.
  - Verificar `GalponQueryPort.obtenerEstadoVigente(galponId) == aislamiento`.
  - Verificar `LoteQueryPort.obtenerPoblacionActual(loteId) > 0`.
  - Invocar `OrdenSacrificioSanitario.emitir(...)` (`alcance = TOTAL`, `estado = PENDIENTE`).
  - Ejecutar `@Transactional`: persistir en `san_ordenes_sacrificio`, encolar `OrdenSacrificioEmitidaIntegrationEvent` en `san_outbox` y registrar traza en `san_auditoria`.
  - Retornar `OrdenSacrificioResponse`.
- [ ] **T028** [US1] Implementar `ConsultarOrdenSacrificioUseCase.java` para obtener el detalle de una orden por su UUID.
- [ ] **T029** [US1] Implementar endpoints `POST /api/v1/sanitary/sacrificios` y `GET /api/v1/sanitary/sacrificios/{id}` en `OrdenSacrificioController.java`.
- **Checkpoint**: US1 funcional — emisión de orden operativa, sin efectos colaterales en el Módulo 1, con evento Outbox y auditoría.

---

## Phase 4: User Story 2 – Confirmación de la Ejecución del Sacrificio Sanitario (P2)

**Goal**: Permitir al Veterinario confirmar la ejecución material del sacrificio, fijando la población del lote en cero y transicionando el galpón a `vaciado_sanitario` de forma atómica.  
**Independent Test**: `PATCH /api/v1/sanitary/sacrificios/{id}/confirmar` sobre una orden `PENDIENTE` retorna `200 OK`. La población del lote en el Módulo 1 queda en cero, el galpón pasa a `vaciado_sanitario` y se publica `SacrificioEjecutadoIntegrationEvent`.

### Tests para User Story 2

- [ ] **T030** [P] [US2] Test de contrato: `PATCH /api/v1/sanitary/sacrificios/{id}/confirmar` con orden `PENDIENTE` retorna HTTP 200 con la orden `EJECUTADA` — `OrdenSacrificioControllerTest.java`.
- [ ] **T031** [P] [US2] Test de contrato: `PATCH` sobre orden `EJECUTADA` retorna HTTP 409 (`OrdenYaEjecutadaException`) — `OrdenSacrificioControllerTest.java`.
- [ ] **T032** [P] [US2] Test de contrato: `PATCH` cuando el galpón ya no está en aislamiento retorna HTTP 409 (`EstadoGalponIncompatibleException`) — `OrdenSacrificioControllerTest.java`.
- [ ] **T033** [P] [US2] Test unitario de `ConfirmarSacrificioUseCase` verificando la transición de estado y la invocación de mutaciones al Módulo 1 — `ConfirmarSacrificioUseCaseTest.java`.
- [ ] **T034** [P] [US2] Test de integración con Testcontainers: verificar que la confirmación actualiza atómicamente `san_ordenes_sacrificio`, `san_outbox` y `san_auditoria` — `OrdenSacrificioRepositoryAdapterTest.java`.
- [ ] **T035** [P] [US2] Test de integración con Testcontainers (Kafka): verificar la publicación efectiva en `sanitary.sacrifice.executed.v1`.

### Implementación de User Story 2

- [ ] **T036** [US2] Crear DTO `ConfirmarSacrificioRequest.java` (con observaciones, `@Size(min=10)`) y extender `OrdenSacrificioResponse.java` con `fechaEjecucion` nullable.
- [ ] **T037** [US2] Definir contrato de evento `SacrificioEjecutadoIntegrationEvent.java` (`eventId`, `aggregateId`, `galponId`, `loteId`, `poblacionExtinguida`, `occurredOn`).
- [ ] **T038** [US2] Implementar método `LoteQueryAdapter.fijarPoblacionEnCero(LoteId id)` y `GalponQueryAdapter.actualizarEstado(GalponId id, "vaciado_sanitario")`.
- [ ] **T039** [US2] Implementar `ConfirmarSacrificioUseCase.java` en `application/sacrificio/`:
  - Validar claim `VETERINARIO`.
  - Obtener `OrdenSacrificioSanitario` por ID; si no existe → `OrdenSacrificioNotFoundException`.
  - Verificar `estado == PENDIENTE`; si es `EJECUTADA` → `OrdenYaEjecutadaException`.
  - Verificar `GalponQueryPort.obtenerEstadoVigente(galponId) == aislamiento`.
  - Verificar `LoteQueryPort.obtenerPoblacionActual(loteId) > 0`.
  - Invocar `LoteQueryPort.fijarPoblacionEnCero(loteId)`.
  - Invocar `GalponQueryPort.actualizarEstado(galponId, "vaciado_sanitario")`.
  - Invocar `orden.confirmarEjecucion(observacion)`.
  - Ejecutar `@Transactional`: persistir la orden, encolar `SacrificioEjecutadoIntegrationEvent` en `san_outbox` y registrar traza en `san_auditoria`.
  - Retornar `OrdenSacrificioResponse`.
- [ ] **T040** [US2] Añadir endpoint `PATCH /api/v1/sanitary/sacrificios/{id}/confirmar` en `OrdenSacrificioController.java`.
- [ ] **T041** [US2] Implementar endpoint `GET /api/v1/sanitary/sacrificios` con paginación y filtros por estado, galpón y rango de fechas.
- **Checkpoint**: US1 y US2 funcionales — emisión y confirmación de órdenes operativas, atómicas y auditadas.

---

## Phase 5: Integridad Transaccional, Outbox Relay e Inmutabilidad (P3)

**Goal**: Asegurar que las órdenes no se puedan eliminar físicamente (`DELETE`), despachar eventos hacia Kafka desde la tabla Outbox de forma resiliente y garantizar idempotencia técnica en ambos endpoints.  
**Independent Test**: `DELETE /api/v1/sanitary/sacrificios/{id}` retorna `405 Method Not Allowed`. Los eventos en `san_outbox` en estado `PENDING` se publican en Kafka y cambian a `PROCESSED` mediante el worker.

### Tests para User Story 3

- [ ] **T042** [P] [US3] Test de contrato y seguridad: `DELETE /api/v1/sanitary/sacrificios/{id}` retorna HTTP 405 Method Not Allowed — `OrdenSacrificioControllerTest.java`.
- [ ] **T043** [P] [US3] Test de base de datos: verificar ausencia de sentencias o métodos `DELETE` en la capa de persistencia.
- [ ] **T044** [P] [US3] Test de integración Outbox Relay con Testcontainers (Kafka): verificar lectura por lotes y publicación efectiva en `sanitary.sacrifice.issued.v1` y `sanitary.sacrifice.executed.v1`.
- [ ] **T045** [P] [US3] Test de idempotencia: enviar dos requests consecutivas idénticas con el mismo `X-Idempotency-Key` en `POST` y `PATCH` y verificar que la segunda retorna la respuesta original sin duplicar entidades ni eventos.

### Implementación de User Story 3

- [ ] **T046** [US3] Configurar `SecurityConfig.java` bloqueando explícitamente cualquier verbo `DELETE` sobre rutas `/api/v1/sanitary/**` (`FR-014`).
- [ ] **T047** [US3] Implementar `KafkaEventPublisherAdapter.java` publicando eventos tipados hacia los tópicos configurados.
- [ ] **T048** [US3] Implementar `OutboxRelayScheduler.java`:
  - Polling periódico con `@Scheduled(fixedDelay = 2000)` sobre `san_outbox` donde `status = 'PENDING'` usando `SELECT ... FOR UPDATE SKIP LOCKED`.
  - Despacho a Kafka mediante `KafkaEventPublisherAdapter`.
  - Actualización atómica de estado a `PROCESSED` con marca temporal `processed_at`.
  - Manejo de reintentos y marcado a `FAILED` si supera 5 reintentos con backoff exponencial.
- [ ] **T049** [US3] Documentar contratos de mensajería asíncrona mediante especificación AsyncAPI 3.0 en `docs/asyncapi/sanitary-events.yml`.
- [ ] **T050** [US3] Configurar especificación OpenAPI 3.0 (Swagger UI) exponiendo documentación de endpoints REST.
- **Checkpoint**: Sistema de mensajería Outbox confiable, tolerancia a caídas de red y blindaje absoluto contra borrado físico.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] **T051** Configurar logging estructurado en formato JSON incorporando `traceId`, `spanId` y `correlationId` vía MDC de Slf4j.
- [ ] **T052** Exponer métricas Prometheus con Micrometer (`sanitary_sacrificio_emitido_total`, `sanitary_sacrificio_ejecutado_total`, `sanitary_outbox_lag_seconds`).
- [ ] **T053** Implementar pruebas de carga con Gatling/k6 validando latencia < 1 s en confirmación bajo concurrencia sostenida.
- [ ] **T054** Auditoría de dependencias: validar mediante ArchUnit que el paquete `domain/` mantenga cero imports de Spring, JPA/Hibernate, Jackson o librerías externas.
- [ ] **T055** Verificar correspondencia campo a campo entre el DTO de respuesta y la vista Figma importada (`docs/prototype/gestion-sanitaria/sacrificio-sanitario/`).

---

## 5. Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: Sin dependencias — inicia de inmediato.
- **Phase 2 (Foundational)**: Requiere Fase 1 completa — **bloquea todas las user stories**.
- **Phase 3 (User Story 1)**: Requiere Fase 2 completada (bloqueante).
- **Phase 4 (User Story 2)**: Requiere Fase 3 (necesita existir la orden emitida).
- **Phase 5 (US3 / Outbox + Inmutabilidad)**: Puede desarrollarse en paralelo con Fase 4 tras concluir Fase 3.
- **Phase 6 (Polish)**: Requiere todas las fases funcionales implementadas.

### User Story Dependencies

- **US1 (Emisión)**: Depende de infraestructura base y de los puertos cross-context (`DiagnosticoQueryPort`, `EnfermedadQueryPort`, `GalponQueryPort`, `LoteQueryPort`).
- **US2 (Confirmación)**: Depende de US1 (la orden debe existir previamente en estado `PENDIENTE`).
- **US3 (Outbox & Inmutabilidad)**: Transversal a US1 y US2; procesa los eventos generados por ambas historias.

---

## 6. Traceability Matrix (Spec 012 vs Implementation Plan)

| Requerimiento Spec 012 | Tarea(s) en Implementation Plan | Componente Técnico Responsable |
| :--- | :--- | :--- |
| **FR-001** (Exclusivo Veterinario) | **T016**, **T021**, **T027**, **T039** | `RoleValidationFilter`, `EmitirOrdenSacrificioUseCase`, `ConfirmarSacrificioUseCase` |
| **FR-002** (Enfermedad con `requiereSacrificioSanitario == true`) | **T011**, **T026**, **T027** | `EnfermedadQueryPort`, `EnfermedadQueryAdapter` |
| **FR-003** (Galpón en aislamiento) | **T011**, **T026**, **T027**, **T039** | `GalponQueryPort`, `GalponQueryAdapter` |
| **FR-004** (Lote con población > 0) | **T011**, **T026**, **T027**, **T039** | `LoteQueryPort`, `LoteQueryAdapter` |
| **FR-005** (Asociación a `diagnosticoId` + `galponId` + `loteId`) | **T008**, **T009**, **T027** | `DiagnosticoId`, `GalponId`, `LoteId`, `OrdenSacrificioSanitario.java` |
| **FR-006** (Emisión NO modifica población ni estado) | **T009**, **T027** | `OrdenSacrificioSanitario.emitir()` (solo lectura en cross-context) |
| **FR-007** (Verificación pre-confirmación) | **T039** | `ConfirmarSacrificioUseCase` (verificación de estado `PENDIENTE`) |
| **FR-008** (Población a cero) | **T038**, **T039** | `LoteQueryPort.fijarPoblacionEnCero()` |
| **FR-009** (Transición a `vaciado_sanitario`) | **T038**, **T039** | `GalponQueryPort.actualizarEstado()` |
| **FR-010** (Estado `EJECUTADA` + timestamp UTC) | **T009**, **T039** | `OrdenSacrificioSanitario.confirmarEjecucion()` |
| **FR-011** (Atomicidad transaccional) | **T039** | `@Transactional` sobre `ConfirmarSacrificioUseCase` |
| **FR-012** (Evento Outbox Kafka) | **T025**, **T037**, **T048** | `san_outbox`, `OutboxRelayScheduler`, tópicos Kafka |
| **FR-013** (Trazabilidad auditoría) | **T007**, **T013**, **T027**, **T039** | `san_auditoria`, `AuditoriaSanitariaPort` |
| **FR-014** (Prohibido borrado físico) | **T042**, **T043**, **T046** | `SecurityConfig`, ausencia de SQL `DELETE` |
| **FR-015** (Concurrencia e Idempotencia) | **T015**, **T045** | `IdempotencyFilter`, `@Version` en entidad JPA |
| **FR-016** (Alcance `TOTAL` no negociable) | **T008**, **T009** | Enum `AlcanceOrden` (único valor) y `orden.emitir()` |
| **SC-001 a SC-009** (Métricas de éxito) | **T022**, **T033**, **T052**, **T053** | Pruebas de integración, Prometheus, Gatling y Jacoco |

---

## 7. Notes

- Cada tarea cuenta con su identificador único `T0xx` para seguimiento en tableros Kanban o Jira.
- Las escrituras que involucran `san_ordenes_sacrificio`, `san_outbox` y `san_auditoria` se ejecutan dentro del mismo bloque `@Transactional` imperativo de Spring Data JPA.
- **Separación Emisión / Ejecución**: La emisión es un acto clínico que formaliza la instrucción legal sin alterar inventarios; la ejecución es el acto material en campo. Este desacoplamiento en dos tiempos previene que contingencias logísticas alteren prematuramente los inventarios biológicos de la granja.
- **Alcance total no negociable**: El Spec 012 no admite sacrificios parciales. La invariante del enum `AlcanceOrden.TOTAL` se valida en el Aggregate Root, no en la capa REST.
- **Inhibición de Reintegros Automáticos**: Si un lote tiene una orden de sacrificio en estado `PENDIENTE`, el `DiagnosticoReintegroScheduler` (`Spec 011`) debe omitir cualquier intento de reintegro automático mientras la orden no sea cerrada. Esta coordinación se realiza mediante un puerto compartido `DiagnosticoQueryPort` que expone el estado de la orden de sacrificio.
- **Uso de Virtual Threads (Project Loom)** para manejar la concurrencia de peticiones HTTP sin la complejidad de la programación reactiva, manteniendo el modelo imperativo de Spring MVC.
- **Comunicación con el Módulo 1** exclusivamente a través de los puertos `GalponQueryPort` y `LoteQueryPort` para garantizar el desacoplamiento de bounded contexts.
- **Normalización temporal**: Tanto `fechaEmision` como `fechaEjecucion` se registran en UTC (`Instant.now()`) para evitar ambigüedades en auditorías legales.
- **Índice único parcial**: `UNIQUE(lote_id) WHERE estado = 'PENDIENTE'` previene a nivel de motor la existencia de dos órdenes pendientes simultáneas sobre el mismo lote, complementando la validación de concurrencia optimista.
- **Notificación al frontend**: El payload `OrdenSacrificioResponse` expone `estado` (`PENDIENTE` / `EJECUTADA`) y `fechaEjecucion` (nullable), permitiendo a la interfaz habilitar el botón de confirmación únicamente cuando la orden esté pendiente.