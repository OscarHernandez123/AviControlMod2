# Implementation Plan: Diagnosticar un galpón

**Date**: 2026-09-22  
**Specs**:  
- [011-DiagnosticarGalpon.md](docs/specs/011-DiagnosticarGalpon.md)

---

## 1. Summary

El módulo **Diagnosticar un galpón** permite al Médico Veterinario asentar el expediente clínico formal de una parvada que se encuentra bajo régimen de `aislamiento` (`Spec 010`)[cite: 4, 8]. La resolución del diagnóstico se bifurca en dos ramas clínicas determinísticas y mutuamente excluyentes según el indicador nosológico `requiereSacrificioSanitario` de la enfermedad (`Spec 009`)[cite: 4, 8]:

* **Rama Terapéutica** (`requiereSacrificioSanitario == false`): Exige una prescripción activa del catálogo de medicación (`Spec 008`) y calcula de forma automática la **fecha de reintegro** (`fechaDiagnostico + diasTratamiento`) normalizada al final del ciclo UTC[cite: 4].
* **Rama Mortal / Erradicación** (`requiereSacrificioSanitario == true`): Prescinde de medicación (`medicacionId = null`), no proyecta fecha de reintegro y **habilita de forma inmediata la emisión de la orden de sacrificio sanitario total** (`Spec 012`)[cite: 4, 8].

Adicionalmente, un job programado en segundo plano (`DiagnosticoReintegroScheduler`) evalúa los tratamientos vencidos y comanda la transición operativa del galpón a estado `productiva` en el Módulo 1 si el galpón continúa aislado[cite: 4, 7]. **No descuenta existencias en bodega ni altera historiales clínicos cerrados**[cite: 1, 4]. 

La solución técnica se implementa en **Java 21** con **Spring Boot 3.x (Spring MVC + Spring Data JPA)** bajo **arquitectura hexagonal pura**, el patrón **Transactional Outbox** hacia **Kafka**, control de **Concurrencia Optimista (`@Version`)** complementado con índice único parcial, trazabilidad inmutable en **`san_auditoria`** y validación de idempotencia técnica vía `X-Idempotency-Key`[cite: 1].

---

## 2. Technical Context

- **Language/Version**: Java 21 (LTS - Virtual Threads habilitados mediante `spring.threads.virtual.enabled=true`)[cite: 1].
- **Primary Dependencies**: Spring Boot 3.x (Spring MVC), Spring Cloud Stream (Kafka Binder), Spring Data JPA (Hibernate 6.x), PostgreSQL JDBC Driver, Lombok, MapStruct, Jakarta Validation, JUnit 5, Mockito, Testcontainers (PostgreSQL + Kafka)[cite: 1].
- **Storage**: PostgreSQL 16+ relacional vía JDBC/JPA (`san_diagnosticos`, `san_outbox`, `san_auditoria`)[cite: 1].
- **Testing**: JUnit 5, Mockito, MockMvc, Testcontainers[cite: 1].
- **Target Platform**: Contenedores Linux (Docker / Kubernetes)[cite: 1].
- **Project Type**: Backend REST micro-service (Módulo 2: Sanidad y Bioseguridad)[cite: 8].
- **Performance Goals**: Latencia < 250 ms en validación y persistencia (`SC-005`); disponibilidad de consulta < 1 s (`SC-006`)[cite: 1].
- **Constraints**: 
  - Prohibido el borrado físico (`DELETE` SQL) (`FR-015`, `SC-009`)[cite: 1].
  - Escritura atómica obligatoria: entidad + outbox + auditoría bajo `@Transactional` (`FR-013`, `FR-014`, `SC-010`)[cite: 1].
  - Rol exclusivo `VETERINARIO` para transacciones clínicas (`FR-001`, `SC-006`)[cite: 1].
  - Validación síncrona obligatoria contra el Módulo 1 comprobando estado `aislamiento` (`FR-002`, `FR-010`)[cite: 4, 8].
  - Idempotencia HTTP mediante cabecera `X-Idempotency-Key` retenida por 24 horas (`FR-016`, `SC-008`)[cite: 1].
  - Transición de estado a `productiva` automatizada e idempotente vía scheduler interno (`FR-009`, `FR-011`, `SC-007`)[cite: 4, 7].

---

## 3. Project Structure

```text
src/main/java/com/avicontrol/sanidad/
├── domain/                                # Núcleo Puro de Dominio (Sin dependencias Spring/JPA)
│   ├── model/
│   │   └── diagnostico/
│   │       ├── Diagnostico.java           # Aggregate Root
│   │       ├── DiagnosticoId.java         # Value Object UUID
│   │       ├── GalponId.java              # Value Object UUID (Ref. Módulo 1)
│   │       ├── LoteId.java                # Value Object UUID (Ref. Módulo 1)
│   │       ├── EnfermedadId.java          # Value Object UUID (Ref. Spec 009)
│   │       ├── MedicacionId.java          # Value Object UUID (Ref. Spec 008, Nullable)
│   │       ├── FechaReintegro.java        # Value Object Instant normalizado (UTC)
│   │       └── EstadoDiagnostico.java     # Enum: ACTIVO, CERRADO
│   ├── exception/
│   │   └── diagnostico/
│   │       ├── DiagnosticoNotFoundException.java
│   │       ├── GalponNoAisladoException.java
│   │       ├── EstadoGalponIncompatibleException.java
│   │       ├── MedicacionRequeridaException.java
│   │       ├── DiagnosticoActivoExistenteException.java
│   │       ├── Modulo1NoDisponibleException.java
│   │       └── DiagnosticoConcurrenciaException.java
│   └── repository/                        # Puertos de Salida (Driven Ports)
│       ├── DiagnosticoRepositoryPort.java # Persistencia del Agregado
│       ├── GalponQueryPort.java           # Consulta y transición de estado en Módulo 1
│       ├── EnfermedadQueryPort.java       # Consulta al catálogo nosológico (Spec 009)
│       ├── MedicacionQueryPort.java       # Consulta al catálogo terapéutico (Spec 008)
│       ├── OutboxRepositoryPort.java      # Registro local para Transactional Outbox
│       └── AuditoriaSanitariaPort.java    # Bitácora inmutable en san_auditoria
├── application/
│   └── diagnostico/                       # Casos de Uso (Una clase por responsabilidad)
│       ├── RegistrarDiagnosticoUseCase.java
│       ├── ConsultarDiagnosticoUseCase.java
│       └── ReintegroAutomaticoUseCase.java # Invocado por el worker programado
├── infrastructure/
│   ├── adapter/in/rest/                   # Adaptador Primario REST (Spring MVC)
│   │   ├── ApiErrorResponse.java
│   │   ├── GlobalExceptionHandler.java    # @RestControllerAdvice
│   │   ├── filter/
│   │   │   ├── RoleValidationFilter.java
│   │   │   └── IdempotencyFilter.java
│   │   └── diagnostico/
│   │       ├── DiagnosticoController.java
│   │       ├── dto/
│   │       │   ├── CrearDiagnosticoRequest.java
│   │       │   ├── DiagnosticoResponse.java
│   │       │   └── DiagnosticoFiltroRequest.java
│   │       └── mapper/
│   │           └── DiagnosticoRestMapper.java
│   ├── adapter/out/
│   │   ├── persistence/                   # Adaptadores Secundarios JPA
│   │   │   ├── diagnostico/
│   │   │   │   ├── DiagnosticoEntity.java
│   │   │   │   ├── DiagnosticoJpaRepository.java
│   │   │   │   ├── DiagnosticoRepositoryAdapter.java
│   │   │   │   └── mapper/DiagnosticoPersistenceMapper.java
│   │   │   ├── outbox/
│   │   │   │   ├── OutboxEntity.java
│   │   │   │   ├── OutboxJpaRepository.java
│   │   │   │   └── OutboxRepositoryAdapter.java
│   │   │   └── auditoria/
│   │   │       ├── AuditoriaEntity.java
│   │   │       ├── AuditoriaJpaRepository.java
│   │   │       └── AuditoriaRepositoryAdapter.java
│   │   ├── client/                        # Adaptadores de Integración Cross-Context
│   │   │   ├── GalponQueryAdapter.java    # Integración síncrona con Módulo 1
│   │   │   ├── EnfermedadQueryAdapter.java# Consulta a san_enfermedades
│   │   │   └── MedicacionQueryAdapter.java# Consulta a san_medicaciones
│   │   └── event/                         # Workers Asíncronos y Outbox Relay
│   │       ├── OutboxRelayScheduler.java  # Polling worker hacia Kafka
│   │       ├── KafkaEventPublisherAdapter.java
│   │       └── DiagnosticoReintegroScheduler.java # Job periódico de reintegro
│   └── config/
│       ├── BeanConfiguration.java         # Inyección explícita de Use Cases
│       ├── SchedulerConfiguration.java    # Configuración de pool @Scheduled
│       └── SecurityConfig.java            # Bloqueo estricto del verbo DELETE
└── events/                                # Contratos de Eventos de Integración
    ├── DiagnosticoRegistradoIntegrationEvent.java
    ├── DiagnosticoMortalRegistradoIntegrationEvent.java
    └── GalponReintegradoIntegrationEvent.java
```

> **Decisión de Estructura**: Microservicio basado en Spring MVC imperativo con Virtual Threads de Java 21, desacoplando el dominio de los frameworks mediante mappers dedicados[cite: 1]. La comunicación con el Módulo 1 se centraliza en `GalponQueryPort`[cite: 8]. El reintegro automático se ejecuta mediante un scheduler autónomo (`DiagnosticoReintegroScheduler`) que invoca a `ReintegroAutomaticoUseCase`[cite: 4, 7].

---

## 4. Implementation Phases

### Phase 1: Setup (Shared Infrastructure)
**Propósito**: Inicializar el proyecto base, configuración estándar de Spring Boot y herramientas de calidad.

- [ ] **T001** Inicializar proyecto Spring Boot 3.x con Java 21 y dependencias estándar (Spring Web, Spring Data JPA, Kafka Stream, Validation, Testcontainers, PostgreSQL Driver).
- [ ] **T002** Generar la estructura de paquetes hexagonal según la convención del proyecto (`domain`, `application`, `infrastructure`, `events`).
- [ ] **T003** Configurar `application.yml` con pool HikariCP, parámetros de Kafka (`sanitary.diagnosis.registered.v1`, `sanitary.galpon.reintegrated.v1`) y habilitar Virtual Threads (`spring.threads.virtual.enabled=true`).
- [ ] **T004** Configurar Docker Compose local con servicios `postgres:16-alpine` y broker `kafka` (bitnami/kafka:latest con KRaft).
- [ ] **T005** Configurar Checkstyle, SpotBugs y Jacoco con umbral mínimo de cobertura del 85%.
- [ ] **T006** Configurar pipeline de integración continua (CI) en GitHub Actions para compilar, ejecutar tests con Testcontainers y validar linters.

---

### Phase 2: Foundational (Blocking Prerequisites)
**Propósito**: Construir el modelo de dominio puro, la persistencia JPA, el Transactional Outbox y la auditoría inmutable.  
> ⚠️ **CRÍTICO**: Ningún caso de uso funcional debe implementarse antes de validar esta fase.

- [ ] **T007** Crear scripts DDL en PostgreSQL para tablas `san_diagnosticos`, `san_outbox` y `san_auditoria` con llaves primarias UUID. Añadir índice único parcial `CREATE UNIQUE INDEX uq_galpon_diagnostico_activo ON san_diagnosticos(galpon_id) WHERE estado = 'ACTIVO'` para garantizar a nivel de motor que un galpón solo posea un diagnóstico abierto a la vez (`FR-016`, `SC-008`)[cite: 1].
- [ ] **T008** Definir Value Objects de Dominio con validación estricta:
  - `DiagnosticoId.java`: Envoltorio inmutable sobre UUID.
  - `GalponId.java`: Identificador externo del Módulo 1.
  - `LoteId.java`: Identificador externo del lote alojado provisto por Módulo 1.
  - `EnfermedadId.java`: Clave nosológica del `Spec 009`[cite: 4, 8].
  - `MedicacionId.java`: Clave terapéutica del `Spec 008` (nullable)[cite: 4, 8].
  - `FechaReintegro.java`: Envoltorio sobre Instant normalizado a las 23:59:59 UTC del último día de tratamiento[cite: 4].
  - `EstadoDiagnostico.java`: Enum con valores `ACTIVO` y `CERRADO`.
- [ ] **T009** Crear Aggregate Root `Diagnostico.java` con constructor privado y métodos de fábrica estáticos:
  - `registrarTerapeutico(...)`: Exige medicación no nula, asigna `fechaDiagnostico = Instant.now()`, calcula `fechaReintegro` y fija `estado = ACTIVO`[cite: 4].
  - `registrarMortal(...)`: Valida ausencia de medicación (`medicacionId = null`), fija `fechaReintegro = null`, marca `estado = ACTIVO` y levanta el flag de derivación a sacrificio (`Spec 012`)[cite: 4, 8].
  - `cerrar()`: Mutador que transiciona el expediente a `CERRADO` al reintegrar o dar de baja el lote.
- [ ] **T010** Crear excepciones de dominio en `domain/exception/diagnostico/` (`DiagnosticoNotFoundException`, `GalponNoAisladoException`, `EstadoGalponIncompatibleException`, `MedicacionRequeridaException`, `DiagnosticoActivoExistenteException`, `Modulo1NoDisponibleException`, `DiagnosticoConcurrenciaException`).
- [ ] **T011** Definir puertos secundarios en `domain/repository/`:
  - `DiagnosticoRepositoryPort.java`: métodos `guardar(Diagnostico d)`, `buscarPorId(DiagnosticoId id)`, `buscarActivoPorGalpon(GalponId id)`, `listarPendientesReintegro(Instant fechaActual)`.
  - `GalponQueryPort.java`: métodos `String obtenerEstadoVigente(GalponId id)`, `void actualizarEstado(GalponId id, String nuevoEstado)`.
  - `EnfermedadQueryPort.java`: métodos `boolean estaActiva(UUID id)`, `boolean requiereSacrificioSanitario(UUID id)`.
  - `MedicacionQueryPort.java`: métodos `boolean estaActiva(UUID id)`, `int obtenerDiasTratamiento(UUID id)`.
  - `OutboxRepositoryPort.java`: método `guardarEvento(OutboxEntity evento)`.
  - `AuditoriaSanitariaPort.java`: método inmutable `registrarTraza(AuditoriaEntity traza)`.
- [ ] **T012** Implementar entidades JPA (`DiagnosticoEntity.java`, `OutboxEntity.java`, `AuditoriaEntity.java`) y repositorios `JpaRepository`.
- [ ] **T013** Implementar adaptadores JPA que cumplan los puertos secundarios garantizando el mapeo de dominio vía `DiagnosticoPersistenceMapper`.
- [ ] **T014** Implementar `GlobalExceptionHandler` con `@RestControllerAdvice` para transformar excepciones de dominio y validación a esquemas RFC-7807 (`ApiErrorResponse`) con códigos HTTP adecuados (400, 403, 404, 409, 503).
- [ ] **T015** Implementar `IdempotencyFilter` (`OncePerRequestFilter`) para cachear respuestas asociadas a `X-Idempotency-Key` durante 24 horas (`FR-016`)[cite: 1].
- [ ] **T016** Implementar `RoleValidationFilter` para verificar el claim `VETERINARIO` en todas las peticiones a `/api/v1/sanitary/diagnosticos/**` (`FR-001`)[cite: 1].

> **Checkpoint**: Modelo de dominio puro compilando, tablas creadas, infraestructura JPA operativa y filtros transversales listos.

---

### Phase 3: User Story 1 – Registro del Dictamen Clínico del Lote Aislado (Priority: P1)
**Propósito**: Permitir al Veterinario registrar un diagnóstico sobre un galpón en estado aislamiento, resolviendo la rama terapéutica o la rama mortal, persistiendo atómicamente la entidad, la auditoría y el evento Outbox[cite: 4, 8].  
**Test Independiente**: `POST /api/v1/sanitary/diagnosticos` con datos válidos y rol `VETERINARIO` retorna `201 Created`[cite: 1]. Verifica que la rama terapéutica calcula `fechaReintegro` y que la rama mortal retorna `habilitaSacrificio = true`[cite: 4, 8].

#### Tests para User Story 1
- [ ] **T017** `[P]` `[US1]` Test de contrato: `POST /api/v1/sanitary/diagnosticos` con enfermedad tratable y medicación válida retorna HTTP 201 Created, payload con `fechaReintegro` calculada y cabecera `Location` — `DiagnosticoControllerTest.java`[cite: 4].
- [ ] **T018** `[P]` `[US1]` Test de contrato: `POST` con enfermedad letal retorna HTTP 201 Created con `medicacionId = null`, `fechaReintegro = null` y `habilitaSacrificio = true` — `DiagnosticoControllerTest.java`[cite: 4, 8].
- [ ] **T019** `[P]` `[US1]` Test de contrato: `POST` sin claim `VETERINARIO` retorna HTTP 403 Forbidden — `DiagnosticoControllerTest.java`[cite: 1].
- [ ] **T020** `[P]` `[US1]` Test de contrato: `POST` sobre galpón cuyo estado vigente en Módulo 1 sea distinto a `aislamiento` retorna HTTP 409 Conflict (`GalponNoAisladoException`) — `DiagnosticoControllerTest.java`[cite: 4, 8].
- [ ] **T021** `[P]` `[US1]` Test de contrato: `POST` con patología tratable omitiendo `medicacionId` retorna HTTP 400 Bad Request (`MedicacionRequeridaException`) — `DiagnosticoControllerTest.java`.
- [ ] **T022** `[P]` `[US1]` Test unitario de `RegistrarDiagnosticoUseCase` validando orquestación, cómputo temporal de reintegro y bifurcación clínica con puertos mockeados — `RegistrarDiagnosticoUseCaseTest.java`[cite: 4].
- [ ] **T023** `[P]` `[US1]` Test de integración con Testcontainers (PostgreSQL JDBC): Confirmar atomicidad transaccional con `@Transactional` (si falla auditoría u outbox, el diagnóstico no se persiste)[cite: 1].

#### Implementación de User Story 1
- [ ] **T024** `[US1]` Crear DTOs de entrada y salida: `CrearDiagnosticoRequest.java` (validaciones Jakarta `@NotNull`, `@NotBlank`) y `DiagnosticoResponse.java` (incluye `fechaReintegro` calculada y el booleano `habilitaSacrificio`)[cite: 4, 8].
- [ ] **T025** `[US1]` Definir contratos de eventos de integración: `DiagnosticoRegistradoIntegrationEvent.java` (rama terapéutica), `DiagnosticoMortalRegistradoIntegrationEvent.java` (rama mortal) y `GalponReintegradoIntegrationEvent.java` con schemas normalizados[cite: 1].
- [ ] **T026** `[US1]` Implementar los adaptadores de consulta externa `GalponQueryAdapter`, `EnfermedadQueryAdapter` y `MedicacionQueryAdapter` consumiendo las fuentes correspondientes sin acoplamiento[cite: 8].
- [ ] **T027** `[US1]` Implementar `RegistrarDiagnosticoUseCase.java` en `application/diagnostico/`:
  - Validar claim de rol `VETERINARIO`[cite: 1].
  - Consultar `GalponQueryPort.obtenerEstadoVigente(galponId)`; abortar si difiere de `aislamiento`[cite: 4, 8].
  - Consultar `EnfermedadQueryPort.estaActiva(enfermedadId)` y obtener `requiereSacrificioSanitario`[cite: 4].
  - **Bifurcación Clínica**:
    - Si `requiereSacrificioSanitario == false`: Verificar medicación activa en `MedicacionQueryPort`, obtener `diasTratamiento`, invocar `Diagnostico.registrarTerapeutico(...)` y preparar `DiagnosticoRegistradoIntegrationEvent`[cite: 4].
    - Si `requiereSacrificioSanitario == true`: Invocar `Diagnostico.registrarMortal(...)` y preparar `DiagnosticoMortalRegistradoIntegrationEvent`[cite: 4].
  - Ejecutar `@Transactional`: persistir entidad en `san_diagnosticos`, encolar evento en `san_outbox` y registrar firma médica en `san_auditoria`[cite: 1].
  - Retornar `DiagnosticoResponse` con `fechaReintegro` y `habilitaSacrificio`[cite: 4, 8].
- [ ] **T028** `[US1]` Implementar `ConsultarDiagnosticoUseCase.java` en `application/diagnostico/` para obtener el expediente clínico detallado por su UUID.
- [ ] **T029** `[US1]` Implementar endpoints `POST /api/v1/sanitary/diagnosticos` y `GET /api/v1/sanitary/diagnosticos/{id}` en `DiagnosticoController.java`.

> **Checkpoint**: US1 completamente funcional — ambas ramas clínicas (terapéutica y mortal) operativas, atómicas y auditadas[cite: 4, 8].

---

### Phase 4: User Story 2 – Idempotencia y Prevención de Diagnósticos Duplicados (Priority: P2)
**Propósito**: Asegurar que el registro del diagnóstico se procese de manera idempotente y atómica, previniendo duplicados ante reintentos de red o intentos concurrentes de dos veterinarios[cite: 1].  
**Test Independiente**: Enviar el mismo comando dos veces con la misma `X-Idempotency-Key` retorna la respuesta original sin duplicar la entidad ni el evento[cite: 1].

#### Tests para User Story 2
- [ ] **T030** `[P]` `[US2]` Test de contrato: `POST /api/v1/sanitary/diagnosticos` con `X-Idempotency-Key` ya procesada retorna la respuesta original almacenada sin duplicar registros — `DiagnosticoControllerTest.java`[cite: 1].
- [ ] **T031** `[P]` `[US2]` Test de contrato: `POST` sobre un galpón que ya cuenta con un diagnóstico en estado `ACTIVO` retorna HTTP 409 Conflict (`DiagnosticoActivoExistenteException`) — `DiagnosticoControllerTest.java`.
- [ ] **T032** `[P]` `[US2]` Test de integración con Testcontainers: verificar que la ejecución concurrente de dos veterinarios sobre el mismo lote provoca colisión optimista (`DiagnosticoConcurrenciaException` o `DataIntegrityViolationException`) en la segunda transacción.

#### Implementación de User Story 2
- [ ] **T033** `[US2]` Configurar `IdempotencyFilter` para almacenar en caché la respuesta del endpoint `POST /api/v1/sanitary/diagnosticos` mapeada a `X-Idempotency-Key` (retención de 24 horas).
- [ ] **T034** `[US2]` Añadir anotación `@Version` en `DiagnosticoEntity.java` y validar en el caso de uso que la versión optimista coincida antes de commitear.
- [ ] **T035** `[US2]` Implementar endpoint `GET /api/v1/sanitary/diagnosticos` en `DiagnosticoController.java` soportando paginación y filtros por galpón, estado del diagnóstico y rango de fechas.

> **Checkpoint**: US1 y US2 funcionales — registro, idempotencia y consulta de diagnósticos plenamente operativos.

---

### Phase 5: User Story 3 – Reintegro Automático, Outbox Relay e Inmutabilidad (Priority: P3)
**Propósito**: Ejecutar el reintegro automático del galpón al cumplirse la fecha proyectada, garantizar el despacho confiable de eventos hacia Kafka y blindar contra borrado físico[cite: 1, 4, 7].  
**Test Independiente**: Insertar un diagnóstico terapéutico con `fechaReintegro` vencida y ejecutar el scheduler; verificar que el galpón transiciona a `productiva` en el Módulo 1 y se publica `GalponReintegradoIntegrationEvent`[cite: 4, 7].

#### Tests para User Story 3
- [ ] **T036** `[P]` `[US3]` Test unitario de `ReintegroAutomaticoUseCase` verificando transición idempotente a `productiva` cuando `hoy >= fechaReintegro` y el galpón continúa en `aislamiento` — `ReintegroAutomaticoUseCaseTest.java`[cite: 4, 7].
- [ ] **T037** `[P]` `[US3]` Test de integración de `DiagnosticoReintegroScheduler` con Testcontainers: verificar que consulta diagnósticos vencidos, cierra expedientes y encola `GalponReintegradoIntegrationEvent` en `san_outbox`[cite: 1, 4, 7].
- [ ] **T038** `[P]` `[US3]` Test de contrato y seguridad: Petición `DELETE /api/v1/sanitary/diagnosticos/{id}` retorna HTTP 405 Method Not Allowed — `DiagnosticoControllerTest.java`[cite: 1].
- [ ] **T039** `[P]` `[US3]` Test de integración Outbox Relay con Testcontainers (Kafka): verificar publicación efectiva hacia los tópicos `sanitary.diagnosis.registered.v1` y `sanitary.galpon.reintegrated.v1`[cite: 1].
- [ ] **T040** `[P]` `[US3]` Test de idempotencia del scheduler: comprobar que ejecuciones consecutivas del job no duplican transiciones ni emiten eventos redundantes.

#### Implementación de User Story 3
- [ ] **T041** `[US3]` Implementar `ReintegroAutomaticoUseCase.java` en `application/diagnostico/`:
  - Consultar `DiagnosticoRepositoryPort.listarPendientesReintegro(Instant.now())`[cite: 4].
  - Por cada diagnóstico: consultar `GalponQueryPort.obtenerEstadoVigente(galponId)`[cite: 4, 8].
  - Si el galpón continúa en `aislamiento`: invocar `GalponQueryPort.actualizarEstado(galponId, "productiva")`, mutar aggregate root `diagnostico.cerrar()`, encolar `GalponReintegradoIntegrationEvent` en `san_outbox` y registrar asiento automático en `san_auditoria`[cite: 1, 4, 7].
  - Si el galpón reporta un estado distinto: preservar el estado en Módulo 1, marcar diagnóstico como `CERRADO` y registrar alerta de discrepancia en `san_auditoria`[cite: 1].
- [ ] **T042** `[US3]` Implementar `DiagnosticoReintegroScheduler.java` en `infrastructure/adapter/out/event/`:
  - Anotación `@Scheduled(cron = "${sanitary.reintegro.cron:0 0 1 * * ?}")` configurable desde `application.yml`.
  - Invocar `ReintegroAutomaticoUseCase` con bloque try/catch por registro para evitar abortar el lote completo ante inconsistencias aisladas.
- [ ] **T043** `[US3]` Crear `SchedulerConfiguration.java` con `@EnableScheduling` y asignación de pool dedicado de hilos.
- [ ] **T044** `[US3]` Configurar `SecurityConfig.java` bloqueando explícitamente cualquier verbo `DELETE` sobre rutas `/api/v1/sanitary/**` (`FR-015`)[cite: 1].
- [ ] **T045** `[US3]` Implementar `KafkaEventPublisherAdapter.java` despachando eventos tipados hacia Kafka mediante `StreamBridge` o `KafkaTemplate`[cite: 1].
- [ ] **T046** `[US3]` Implementar `OutboxRelayScheduler.java`:
  - Polling periódico con `@Scheduled(fixedDelay = 2000)` sobre `san_outbox` donde `status = 'PENDING'` usando `SELECT ... FOR UPDATE SKIP LOCKED`[cite: 1].
  - Despacho a Kafka mediante `KafkaEventPublisherAdapter`[cite: 1].
  - Actualización atómica de estado a `PROCESSED` con marca temporal `processed_at`[cite: 1].
  - Manejo de reintentos y marcado a `FAILED` si supera 5 reintentos con backoff exponencial[cite: 1].
- [ ] **T047** `[US3]` Documentar contratos de mensajería asíncrona mediante especificación AsyncAPI 3.0 en `docs/asyncapi/sanitary-events.yml`[cite: 1].
- [ ] **T048** `[US3]` Configurar especificación OpenAPI 3.0 (Swagger UI) exponiendo documentación de endpoints REST[cite: 1].

> **Checkpoint**: Sistema de reintegro automático confiable, Outbox Relay resiliente y blindaje absoluto contra borrado físico[cite: 1, 4, 7].

---

### Phase 6: Polish & Cross-Cutting Concerns
**Propósito**: Asegurar la calidad técnica, observabilidad, rendimiento y correspondencia con la interfaz de usuario.

- [ ] **T049** Configurar logging estructurado en formato JSON incorporando `traceId`, `spanId` y `correlationId` vía MDC de Slf4j[cite: 1].
- [ ] **T050** Exponer métricas Prometheus con Micrometer (`sanitary_diagnostico_registrado_total`, `sanitary_reintegro_ejecutado_total`, `sanitary_outbox_lag_seconds`).
- [ ] **T051** Implementar pruebas de carga con Gatling/k6 validando latencia < 250 ms bajo concurrencia sostenida de 200 req/s[cite: 1].
- [ ] **T052** Auditoría de dependencias: Validar mediante ArchUnit que el paquete `domain/` mantenga cero imports de Spring, JPA/Hibernate, Jackson o librerías externas[cite: 1].
- [ ] **T053** Verificar correspondencia campo a campo entre el DTO de respuesta y la vista Figma importada (`docs/prototype/gestion-sanitaria/aislamiento-y-diagnostico/`)[cite: 4].

---

## 5. Dependencies & Execution Order

### Phase Dependencies
- **Phase 1 (Setup)**: Sin dependencias — inicia de inmediato.
- **Phase 2 (Foundational)**: Requiere Fase 1 completa — **bloquea todas las user stories**.
- **Phase 3 (User Story 1)**: Requiere Fase 2 completada (bloqueante).
- **Phase 4 (User Story 2)**: Requiere Fase 3 (necesita existir la lógica de registro base).
- **Phase 5 (User Story 3 / Scheduler + Outbox)**: Puede desarrollarse en paralelo con Fase 4 tras concluir Fase 3.
- **Phase 6 (Polish)**: Requiere todas las fases funcionales implementadas.

### User Story Dependencies
- **US1 (Registro del Diagnóstico)**: Depende de infraestructura base y de los puertos de consulta cross-context (`GalponQueryPort`, `EnfermedadQueryPort`, `MedicacionQueryPort`)[cite: 8].
- **US2 (Idempotencia / Consulta)**: Depende de US1 (la entidad debe existir para validar duplicados y estados)[cite: 1].
- **US3 (Reintegro + Outbox)**: Transversal a US1; consume los diagnósticos generados en US1 mediante el scheduler[cite: 4, 7].

---

## 6. Traceability Matrix (Spec 011 vs Implementation Plan)

| Requerimiento Spec 011 | Tarea(s) en Implementation Plan | Componente Técnico Responsable |
| :--- | :--- | :--- |
| **FR-001** (Exclusivo Veterinario)[cite: 1] | **T016**, **T019**, **T027** | `RoleValidationFilter`, `RegistrarDiagnosticoUseCase` |
| **FR-002** (Estado aislamiento obligatorio)[cite: 4, 8] | **T011**, **T026**, **T027** | `GalponQueryPort`, `GalponQueryAdapter` |
| **FR-003** (Vínculo a galpón y lote únicos)[cite: 4, 8] | **T008**, **T009**, **T027** | `GalponId`, `LoteId`, `Diagnostico.java` |
| **FR-004** (Enfermedad activa del Spec 009)[cite: 4, 8] | **T011**, **T026**, **T027** | `EnfermedadQueryPort`, `EnfermedadQueryAdapter` |
| **FR-005** (Medicación obligatoria si tratable)[cite: 4, 8] | **T009**, **T021**, **T027** | `MedicacionRequeridaException`, `Diagnostico.registrarTerapeutico()` |
| **FR-006** (Cálculo exacto de fechaReintegro)[cite: 4] | **T008**, **T009**, **T017** | `FechaReintegro.java`, `Diagnostico.registrarTerapeutico()` |
| **FR-007** (Rama mortal sin medicación, habilita Spec 012)[cite: 4, 8] | **T009**, **T018**, **T027** | `Diagnostico.registrarMortal()`, `DiagnosticoResponse.habilitaSacrificio` |
| **FR-008** (fechaDiagnostico UTC al confirmar)[cite: 1] | **T009**, **T027** | `Diagnostico.java` (`Instant.now()`) |
| **FR-009** (Scheduler de reintegro en segundo plano) | **T042**, **T043** | `DiagnosticoReintegroScheduler`, `SchedulerConfiguration` |
| **FR-010** (Transición idempotente si sigue aislado)[cite: 4, 7] | **T036**, **T041** | `ReintegroAutomaticoUseCase`, `GalponQueryPort` |
| **FR-011** (Cierre de expediente sin intervención manual) | **T009**, **T041** | `Diagnostico.cerrar()` |
| **FR-012** (No sobrescribir estado anómalo en Módulo 1) | **T036**, **T041** | `ReintegroAutomaticoUseCase` (verificación de precondición) |
| **FR-013** (Evento Outbox garantizado hacia Kafka)[cite: 1] | **T025**, **T027**, **T046** | `san_outbox`, `OutboxRelayScheduler`, tópicos Kafka |
| **FR-014** (Trazabilidad inmutable de auditoría)[cite: 1] | **T007**, **T013**, **T027** | `san_auditoria`, `AuditoriaSanitariaPort` |
| **FR-015** (Prohibido borrado físico)[cite: 1] | **T038**, **T044** | `SecurityConfig`, revocación de permisos SQL `DELETE` |
| **FR-016** (Concurrencia e Idempotencia técnica)[cite: 1] | **T015**, **T030**, **T034** | `IdempotencyFilter`, `@Version`, índice único parcial |
| **SC-001 a SC-010** (Métricas de éxito)[cite: 1, 4] | **T022**, **T032**, **T050**, **T051** | Pruebas de integración, Prometheus, Gatling y Jacoco |

---

## 7. Notes

- Cada tarea cuenta con su identificador único `T0xx` para seguimiento en tableros Kanban o Jira.
- Las escrituras que involucran `san_diagnosticos`, `san_outbox` y `san_auditoria` se ejecutan dentro del mismo bloque `@Transactional` imperativo de Spring Data JPA[cite: 1].
- El scheduler de reintegro opera como un caso de uso interno desacoplado del API HTTP; su invocación se realiza mediante `@Scheduled` configurable desde `application.yml` (por defecto `cron = "0 0 1 * * ?"`).
- El uso de **Virtual Threads** (Project Loom) permite manejar la concurrencia de peticiones HTTP de forma eficiente sin la complejidad de la programación reactiva, manteniendo el modelo imperativo de Spring MVC[cite: 1].
- La comunicación con el Módulo 1 se realiza exclusivamente a través del puerto `GalponQueryPort` para garantizar el desacoplamiento de bounded contexts[cite: 8].
- La normalización de la fecha de reintegro a las 23:59:59 UTC garantiza que la parvada complete las 24 horas del último ciclo antes de retornar a producción[cite: 4].
- El índice único parcial `UNIQUE(galpon_id) WHERE estado = 'ACTIVO'` previene diagnósticos duplicados simultáneos a nivel de base de datos, complementando la validación de concurrencia optimista.
- La notificación al frontend de la habilitación del `Spec 012` se realiza mediante el atributo booleano `habilitaSacrificio` en el `DiagnosticoResponse`, permitiendo al frontend redirigir al veterinario a la pantalla de orden de sacrificio sanitario[cite: 4, 8].