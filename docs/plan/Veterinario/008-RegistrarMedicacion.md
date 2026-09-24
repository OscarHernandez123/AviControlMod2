# Implementation Plan: Registrar medicación

**Date**: 2026-09-22  
**Specs**:  
- [008-RegistrarMedicacion.md](../specs/Veterinario/008-RegistrarMedicacion.md)

## 1. Summary

El módulo **Registrar medicación** permite al Médico Veterinario gestionar el catálogo de protocolos terapéuticos estándar de la granja avícola. Vincula una patología activa del catálogo nosológico (`Spec 009`), un medicamento disponible en el inventario central, dosis cuantitativa, duración en días enteros positivos (> 0) e instrucciones clínicas de administración. **No descuenta existencias de bodega ni altera tratamientos pasados**. La solución emplea **Java 21** con **Spring Boot 3.x (Spring MVC + Spring Data JPA)** bajo **arquitectura hexagonal pura**, el patrón **Transactional Outbox** para despacho garantizado de eventos hacia **Kafka**, control de **Concurrencia Optimista (`version`)**, trazabilidad inmutable en **`san_auditoria`** y validación de idempotencia técnica vía cabeceras HTTP.

## 2. Technical Context

- **Language/Version**: Java 21 (LTS - Virtual Threads habilitados)
- **Primary Dependencies**: Spring Boot 3.x (Spring MVC), Spring Cloud Stream (Kafka Binder), Spring Data JPA (Hibernate 6.x), PostgreSQL JDBC Driver, Lombok, MapStruct, Jakarta Validation, JUnit 5, Mockito, Testcontainers (PostgreSQL + Kafka)
- **Storage**: PostgreSQL 16+ relacional vía JDBC/JPA (`san_medicaciones`, `san_outbox`, `san_auditoria`).
- **Testing**: JUnit 5, Mockito, MockMvc, Testcontainers
- **Target Platform**: Contenedores Linux (Docker / Kubernetes)
- **Project Type**: Backend REST micro-service (Módulo 2: Sanidad y Bioseguridad)
- **Performance Goals**: Latencia < 250 ms en validación y persistencia (`SC-005`); disponibilidad de consulta < 1 s (`SC-001`).
- **Constraints**: 
  - Prohibido el borrado físico (`DELETE` SQL) (`FR-010`, `SC-006`).
  - Escritura atómica obligatoria de entidad + outbox + auditoría (`FR-008`, `FR-009`, `SC-007`).
  - Rol exclusivo `VETERINARIO` (`FR-001`, `SC-004`).
  - Verificación no bloqueante de inventario sin alteración de existencias (`FR-006`, `SC-002`).
  - Idempotencia HTTP mediante cabecera `X-Idempotency-Key` retenida por 24 horas (`FR-011`).

## 3. Project Structure

```text
src/main/java/com/avicontrol/sanidad/
├── domain/                                # Núcleo Puro de Dominio (Sin dependencias Spring/JPA)
│   ├── model/
│   │   └── medicacion/
│   │       ├── Medicacion.java            # Aggregate Root
│   │       ├── MedicacionId.java          # Value Object UUID
│   │       ├── Dosis.java                 # Value Object cuantitativo
│   │       ├── DuracionTratamiento.java   # Value Object entero > 0
│   │       └── IndicacionClinica.java     # Value Object texto >= 10 chars
│   ├── exception/
│   │   └── medicacion/
│   │       ├── MedicacionNotFoundException.java
│   │       ├── DosisInvalidaException.java
│   │       ├── DuracionInvalidaException.java
│   │       ├── IndicacionClinicaInvalidaException.java
│   │       ├── EnfermedadNoDisponibleException.java
│   │       ├── MedicamentoNoDisponibleException.java
│   │       ├── AccesoSanitarioDenegadoException.java
│   │       └── MedicacionConcurrenciaException.java
│   └── repository/                        # Puertos de Salida (Driven Ports)
│       ├── MedicacionRepositoryPort.java  # Persistencia del Agregado
│       ├── EnfermedadQueryPort.java       # Consulta hacia Catálogo Nosológico (Spec 009)
│       ├── MedicamentoQueryPort.java      # Consulta hacia Inventario Central
│       ├── OutboxRepositoryPort.java      # Registro local para Transactional Outbox
│       └── AuditoriaSanitariaPort.java    # Bitácora inmutable en san_auditoria
├── application/
│   └── medicacion/                        # Casos de Uso (Una clase por responsabilidad)
│       ├── RegistrarMedicacionUseCase.java
│       ├── EditarMedicacionUseCase.java
│       ├── ConsultarMedicacionUseCase.java
│       └── ListarMedicacionesUseCase.java
├── infrastructure/
│   ├── adapter/in/rest/                   # Adaptador Primario REST (Spring MVC)
│   │   ├── ApiErrorResponse.java
│   │   ├── GlobalExceptionHandler.java    # @RestControllerAdvice
│   │   ├── filter/
│   │   │   ├── RoleValidationFilter.java
│   │   │   └── IdempotencyFilter.java
│   │   └── medicacion/
│   │       ├── MedicacionController.java
│   │       ├── dto/
│   │       │   ├── CrearMedicacionRequest.java
│   │       │   ├── EditarMedicacionRequest.java
│   │       │   ├── MedicacionFiltroRequest.java
│   │       │   └── MedicacionResponse.java
│   │       └── mapper/
│   │           └── MedicacionRestMapper.java
│   ├── adapter/out/
│   │   ├── persistence/                   # Adaptadores Secundarios JPA
│   │   │   ├── medicacion/
│   │   │   │   ├── MedicacionEntity.java
│   │   │   │   ├── MedicacionJpaRepository.java
│   │   │   │   ├── MedicacionRepositoryAdapter.java
│   │   │   │   └── mapper/MedicacionPersistenceMapper.java
│   │   │   ├── outbox/
│   │   │   │   ├── OutboxEntity.java
│   │   │   │   ├── OutboxJpaRepository.java
│   │   │   │   └── OutboxRepositoryAdapter.java
│   │   │   └── auditoria/
│   │   │       ├── AuditoriaEntity.java
│   │   │       ├── AuditoriaJpaRepository.java
│   │   │       └── AuditoriaRepositoryAdapter.java
│   │   ├── client/                        # Adaptadores de Consulta Externa (Cross-Context)
│   │   │   ├── EnfermedadQueryAdapter.java
│   │   │   └── MedicamentoQueryAdapter.java
│   │   └── event/                         # Transactional Outbox Relay
│   │       ├── OutboxRelayScheduler.java  # Polling worker hacia Kafka
│   │       └── KafkaEventPublisherAdapter.java
│   └── config/
│       ├── BeanConfiguration.java         # Registro explícito de casos de uso
│       └── SecurityConfig.java            # Bloqueo estricto de DELETE
└── events/                                # Contratos de Eventos de Integración
    ├── MedicacionRegistradaIntegrationEvent.java
    └── MedicacionActualizadaIntegrationEvent.java
```

**Structure Decision**: Microservicio estándar basado en Spring MVC imperativo con Virtual Threads de Java 21, desacoplando el dominio de JPA mediante mappers específicos. Cada caso de uso en `application/medicacion/` es una clase concreta que ejecuta una única responsabilidad.

---

## 4. Implementation Phases

### Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicializar el proyecto base, configuración estándar de Spring Boot y herramientas de calidad.

- [ ] **T001** Inicializar proyecto Spring Boot 3.x con Java 21 y dependencias estándar (Spring Web, Spring Data JPA, Kafka Stream, Validation, Testcontainers, PostgreSQL Driver).
- [ ] **T002** Generar la estructura de paquetes hexagonal según la convención del proyecto (`domain`, `application`, `infrastructure`, `events`).
- [ ] **T003** Configurar `application.yml` con pool de conexiones HikariCP, parámetros de Kafka (`sanitary.medication.registered.v1`) y habilitar Virtual Threads (`spring.threads.virtual.enabled=true`).
- [ ] **T004** Configurar Docker Compose local con servicios `postgres:16-alpine` y broker `kafka` (bitnami/kafka:latest con KRaft).
- [ ] **T005** Configurar Checkstyle, SpotBugs y Jacoco con umbral mínimo de cobertura del 85%.
- [ ] **T006** Configurar pipeline de integración continua (CI) en GitHub Actions para compilar, ejecutar tests con Testcontainers y validar linters.

---

### Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Construir el modelo de dominio puro, la persistencia JPA, el Transactional Outbox y la auditoría inmutable.  
⚠️ **CRITICAL**: Ningún caso de uso funcional debe implementarse antes de validar esta fase.

- [ ] **T007** Crear scripts DDL en PostgreSQL para tablas `san_medicaciones`, `san_outbox` y `san_auditoria` con llaves primarias UUID e índices funcionales.
- [ ] **T008** Definir Value Objects de Dominio con validación estricta:
  - `MedicacionId.java`: Envoltorio inmutable de UUID.
  - `Dosis.java`: Cadena obligatoria con formato y unidad (no vacía, sin puros espacios).
  - `DuracionTratamiento.java`: Validación estricta $\text{dias} \in \mathbb{Z}^+ > 0$.
  - `IndicacionClinica.java`: Validación de texto $\ge 10$ caracteres.
- [ ] **T009** Crear Aggregate Root `Medicacion.java` con constructor privado, métodos de fábrica estáticos `registrar()` y mutador `actualizarPauta()` que incremente la versión optimista.
- [ ] **T010** Crear excepciones de dominio en `domain/exception/medicacion/` (`DuracionInvalidaException`, `EnfermedadNoDisponibleException`, `MedicamentoNoDisponibleException`, `MedicacionConcurrenciaException`).
- [ ] **T011** Definir puertos secundarios en `domain/repository/`:
  - `MedicacionRepositoryPort.java`: métodos `guardar(Medicacion m)`, `buscarPorId(MedicacionId id)`, `listarTodas()`, `listarActivas()`.
  - `EnfermedadQueryPort.java`: método `estaActiva(UUID enfermedadId)`.
  - `MedicamentoQueryPort.java`: método `existeYDisponible(UUID medicamentoId)`.
  - `OutboxRepositoryPort.java`: método `guardarEvento(OutboxEntity evento)`.
  - `AuditoriaSanitariaPort.java`: método `registrarTraza(AuditoriaEntity traza)`.
- [ ] **T012** Implementar entidades JPA (`MedicacionEntity.java`, `OutboxEntity.java`, `AuditoriaEntity.java`) y repositorios `JpaRepository`.
- [ ] **T013** Implementar adaptadores JPA que cumplan los puertos secundarios garantizando el mapeo de dominio vía `MedicacionPersistenceMapper`.
- [ ] **T014** Implementar `GlobalExceptionHandler` con `@RestControllerAdvice` para transformar excepciones de dominio y validación a esquemas RFC-7807 (`ApiErrorResponse`) con códigos HTTP adecuados (400, 403, 404, 409).
- [ ] **T015** Implementar `IdempotencyFilter` (`OncePerRequestFilter`) para validar y cachear respuestas asociadas a la cabecera `X-Idempotency-Key` durante 24 horas (`FR-011`).
- **Checkpoint**: Modelo de dominio puro compilando, tablas creadas, infraestructura JPA operativa y filtros transversales listos.

---

### Phase 3: User Story 1 – Alta de Protocolo Terapéutico Estandarizado (P1)

**Goal**: Permitir al Médico Veterinario dar de alta un protocolo terapéutico asociando enfermedad activa y medicamento disponible, persistiendo atómicamente la entidad, la auditoría y el evento Outbox sin descontar existencias de bodega.  
**Independent Test**: `POST /api/v1/sanitary/medicaciones` con datos válidos y rol `VETERINARIO` retorna `201 Created` y el recurso aparece en `GET /api/v1/sanitary/medicaciones`. `POST` con campos en blanco o duración $\le 0$ retorna `400 Bad Request` con el detalle de campos inválidos.

### Tests para User Story 1

- [ ] **T016** [P] [US1] [TC-001] Test de contrato: `POST /api/v1/sanitary/medicaciones` con datos válidos retorna HTTP 201 con la medicación creada y cabecera `Location` — `MedicacionControllerTest.java`.
- [ ] **T017** [P] [US1] [TC-002] Test de contrato: `POST /api/v1/sanitary/medicaciones` omitiendo dosis o con descripción vacía retorna HTTP 400 — `MedicacionControllerTest.java`.
- [ ] **T018** [P] [US1] [TC-003] Test de contrato: `POST /api/v1/sanitary/medicaciones` con duración = 0 o negativa retorna HTTP 400 — `MedicacionControllerTest.java`.
- [ ] **T019** [P] [US1] [TC-004] Test de contrato: `POST /api/v1/sanitary/medicaciones` ejecutado con rol `TRABAJADOR` retorna HTTP 403 Forbidden — `MedicacionControllerTest.java`.
- [ ] **T020** [P] [US1] [TC-005] Test unitario de `RegistrarMedicacionUseCase` validando orquestación, verificación de puertos mockeados y creación de agregados — `RegistrarMedicacionUseCaseTest.java`.
- [ ] **T021** [P] [US1] [TC-006] Test de integración con Testcontainers (PostgreSQL JDBC): Confirmar atomicidad transaccional con `@Transactional` (si falla auditoría u outbox, la medicación no se persiste).

### Implementación de User Story 1

- [ ] **T022** [US1] Crear DTOs de entrada y salida: `CrearMedicacionRequest.java` (validaciones Jakarta `@NotBlank`, `@Min(1)`) y `MedicacionResponse.java`.
- [ ] **T023** [US1] Definir el contrato de evento `MedicacionRegistradaIntegrationEvent.java` con schema JSON normalizado (`eventId`, `aggregateId`, `enfermedadId`, `medicamentoId`, `dosis`, `diasTratamiento`).
- [ ] **T024** [US1] Implementar `EnfermedadQueryAdapter` consultando la disponibilidad nosológica (consulta síncrona a tabla `san_enfermedades`).
- [ ] **T025** [US1] Implementar `MedicamentoQueryAdapter` consultando la disponibilidad de insumos en bodega sin emitir comandos de descuento o reserva de stock.
- [ ] **T026** [US1] Implementar `RegistrarMedicacionUseCase.java` en `application/medicacion/`:
  - Validar claim de rol `VETERINARIO`.
  - Verificar que `EnfermedadQueryPort.estaActiva` sea `true`.
  - Verificar que `MedicamentoQueryPort.existeYDisponible` sea `true`.
  - Instanciar agregado `Medicacion` ejecutando invariantes de dominio.
  - Ejecutar `@Transactional`: persistir en `san_medicaciones`, encolar registro en `san_outbox` y guardar firma profesional en `san_auditoria`.
- [ ] **T027** [US1] Implementar `ConsultarMedicacionUseCase.java` en `application/medicacion/` para obtener el detalle de un tratamiento por su UUID (`buscarPorId`).
- [ ] **T028** [US1] Implementar endpoints `POST /api/v1/sanitary/medicaciones` y `GET /api/v1/sanitary/medicaciones/{id}` en `MedicacionController.java`.
- **Checkpoint**: US1 completamente funcional — alta y consulta individual de medicación operativa, atómica, auditada y testeable de forma independiente.

---

### Phase 4: User Story 2 – Actualización de Pautas Terapéuticas y Protección de Históricos (P2)

**Goal**: Permitir al Veterinario actualizar dosis, duración o descripción de un esquema terapéutico, garantizando control de concurrencia optimista y protegiendo la inmutabilidad de prescripciones previas.  
**Independent Test**: Modificar una medicación de 5 a 7 días vía `PUT /api/v1/sanitary/medicaciones/{id}` eleva su versión a 2 y retorna HTTP 200. Los tratamientos históricos asociados a diagnósticos previos conservan sus valores originales sin recálculo.

### Tests para User Story 2

- [ ] **T029** [P] [US2] [TC-007] Test de contrato: `PUT /api/v1/sanitary/medicaciones/{id}` con datos válidos y versión coincidente retorna HTTP 200 con la medicación actualizada — `MedicacionControllerTest.java`.
- [ ] **T030** [P] [US2] [TC-008] Test de contrato: `PUT /api/v1/sanitary/medicaciones/{id}` enviando una versión desactualizada retorna HTTP 409 Conflict por concurrencia (`MedicacionConcurrenciaException`) — `MedicacionControllerTest.java`.
- [ ] **T031** [P] [US2] [TC-009] Test de contrato: `PUT /api/v1/sanitary/medicaciones/{id}` con ID inexistente retorna HTTP 404 Not Found — `MedicacionControllerTest.java`.
- [ ] **T032** [P] [US2] [TC-010] Test unitario de `EditarMedicacionUseCase` validando elevación de versión y despacho de evento `MedicacionActualizadaIntegrationEvent` — `EditarMedicacionUseCaseTest.java`.
- [ ] **T033** [P] [US2] [TC-011] Test de integración con Testcontainers: verificar que la edición modifique únicamente la tupla seleccionada y genere su respectiva traza histórica en `san_auditoria` — `MedicacionRepositoryAdapterTest.java`.

### Implementación de User Story 2

- [ ] **T034** [US2] Crear DTO `EditarMedicacionRequest.java` con campos mutables (`dosis`, `diasTratamiento`, `descripcion`, `versionActual`).
- [ ] **T035** [US2] Definir contrato de evento `MedicacionActualizadaIntegrationEvent.java`.
- [ ] **T036** [US2] Implementar `EditarMedicacionUseCase.java` en `application/medicacion/`:
  - Obtener agregado por ID; si no existe lanzar `MedicacionNotFoundException` (404).
  - Verificar versión optimista para prevenir sobreescrituras concurrentes.
  - Invocar `medicacion.actualizarPauta(...)`.
  - Persistir cambios, escribir evento en Outbox y asentar modificación técnica en `san_auditoria`.
- [ ] **T037** [US2] Implementar `ListarMedicacionesUseCase.java` en `application/medicacion/` para obtener el catálogo filtrable de pautas terapéuticas vigentes.
- [ ] **T038** [US2] Añadir endpoints `PUT /api/v1/sanitary/medicaciones/{id}` y `GET /api/v1/sanitary/medicaciones` en `MedicacionController.java`.
- **Checkpoint**: US1 y US2 funcionales — alta, detalle, edición versionada y catálogo de medicaciones operativos.

---

### Phase 5: User Story 3 – Integridad Transaccional, Outbox Relay e Inmutabilidad (P3)

**Goal**: Asegurar que las medicaciones no se puedan eliminar físicamente (`DELETE`), despachar eventos hacia Kafka desde la tabla Outbox de forma resiliente y garantizar idempotencia.  
**Independent Test**: Ejecutar `DELETE /api/v1/sanitary/medicaciones/{id}` retorna `405 Method Not Allowed`. Se verifica mediante worker en background que los eventos en `san_outbox` en estado `PENDING` se publiquen en Kafka y cambien a `PROCESSED`.

### Tests para User Story 3

- [ ] **T039** [P] [US3] [TC-012] Test de contrato y seguridad: Comprobar rechazo a llamadas HTTP `DELETE` → 405 Method Not Allowed — `MedicacionControllerTest.java`.
- [ ] **T040** [P] [US3] [TC-013] Test de base de datos: Verificar mediante test de repositorio la inexistencia de sentencias o métodos de borrado físico directo (`DELETE`) en la capa de persistencia.
- [ ] **T041** [P] [US3] [TC-014] Test de integración Outbox Relay con Testcontainers (Kafka): Verificar lectura por lotes de `san_outbox` y publicación efectiva en el tópico `sanitary.medication.registered.v1`.
- [ ] **T042** [P] [US3] [TC-015] Test de idempotencia: Enviar dos requests consecutivas idénticas con el mismo `X-Idempotency-Key` y verificar que la segunda retorna la respuesta original sin duplicar inserciones.

### Implementación de User Story 3

- [ ] **T043** [US3] Configurar `SecurityConfig.java` bloqueando explícitamente cualquier verbo `DELETE` sobre rutas `/api/v1/sanitary/**`.
- [ ] **T044** [US3] Implementar `KafkaEventPublisherAdapter.java` publicando mensajes tipados hacia el tópico Kafka mediante `StreamBridge` o `KafkaTemplate`.
- [ ] **T045** [US3] Implementar `OutboxRelayScheduler.java`:
  - Polling periódico con `@Scheduled(fixedDelay = 2000)` sobre `san_outbox` donde `status = 'PENDING'` usando `SELECT ... FOR UPDATE SKIP LOCKED`.
  - Despacho a Kafka mediante `KafkaEventPublisherAdapter`.
  - Actualización atómica de estado a `PROCESSED` con marca temporal `processed_at`.
  - Manejo de reintentos y marcado a `FAILED` si supera 5 reintentos con backoff exponencial.
- [ ] **T046** [US3] Documentar contratos de mensajería asíncrona mediante especificación AsyncAPI 3.0 en `docs/asyncapi/sanitary-events.yml`.
- [ ] **T047** [US3] Configurar especificación OpenAPI 3.0 (Swagger UI) exponiendo documentación de endpoints REST.
- **Checkpoint**: Sistema de mensajería Outbox confiable, tolerancia a caídas de red y blindaje absoluto contra borrado físico.

---

### Phase 6: Polish & Cross-Cutting Concerns

- [ ] **T048** Configurar logging estructurado en formato JSON incorporando `traceId`, `spanId` y `correlationId` vía MDC de Slf4j.
- [ ] **T049** Exponer métricas Prometheus con Micrometer (`sanitary_medication_created_total`, `sanitary_outbox_lag_seconds`).
- [ ] **T050** [TC-016] Implementar pruebas de carga con Gatling/k6 validando latencia < 250 ms bajo concurrencia sostenida de 200 req/s.
- [ ] **T051** Auditoría de dependencias: Validar mediante ArchUnit que el paquete `domain/` mantenga cero imports de Spring, JPA/Hibernate, Jackson o librerías externas.
- [ ] **T052** Verificar correspondencia campo a campo entre el DTO de respuesta y la vista Figma importada (Catálogo de Medicación), incluyendo que el campo `activa` (Boolean) se muestre visualmente como estado de la medicación y que NO exista botón de borrado físico en la interfaz.

---

## 5. Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: Sin dependencias.
- **Phase 2 (Foundational)**: Requiere Fase 1 completa.
- **Phase 3 (User Story 1)**: Requiere Fase 2 completada (bloqueante).
- **Phase 4 (User Story 2)**: Requiere Fase 3 (necesita existir la medicación base).
- **Phase 5 (User Story 3 / Outbox Relay)**: Puede desarrollarse en paralelo con Fase 4 tras concluir Fase 3.
- **Phase 6 (Polish)**: Requiere todas las fases funcionales implementadas.

### User Story Dependencies

- **US1 (Alta)**: Depende de infraestructura base y catálogos de consulta (Enfermedades e Inventario).
- **US2 (Edición)**: Depende de US1 (la entidad debe existir previamente en el catálogo).
- **US3 (Outbox & Idempotencia)**: Transversal a US1 y US2; procesa los eventos generados por ambas historias.

---

## 6. API Contracts (Endpoints ↔ FR ↔ DTOs)

Contratos REST formales que vinculan cada endpoint con los requerimientos 
funcionales que cubre y sus modelos de transferencia de datos.

| Endpoint | Método | FRs Cubiertos | Request DTO | Response DTO | Código Éxito |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `/api/v1/sanitary/medicaciones` | POST | FR-001, FR-002, FR-003, FR-004, FR-005, FR-006, FR-008, FR-009, FR-011 | `CrearMedicacionRequest` | `MedicacionResponse` | 201 Created |
| `/api/v1/sanitary/medicaciones/{id}` | PUT | FR-007, FR-011, FR-012 | `EditarMedicacionRequest` | `MedicacionResponse` | 200 OK |
| `/api/v1/sanitary/medicaciones/{id}` | GET | FR-002 | — | `MedicacionResponse` | 200 OK |
| `/api/v1/sanitary/medicaciones` | GET | FR-002, FR-012 | `MedicacionFiltroRequest` | `Page<MedicacionResponse>` | 200 OK |

---

## 7. Traceability Matrix Unificada (3-Vías)

### 7.1 Trazabilidad Funcional (FR ↔ AS ↔ Tareas ↔ TC ↔ UI)

| FR (Spec) | AS (BDD) | Tarea(s) Plan | Test Case (TC) | Componente UI (Prototipo) |
| :--- | :--- | :--- | :--- | :--- |
| **FR-001** (Rol VETERINARIO) | AS-006, AS-009 | T019, T026 | TC-004 | Botón "+ Registrar medicación" |
| **FR-002** (Campos obligatorios) | AS-001, AS-002 | T008, T017, T022 | TC-002 | Inputs formulario / Data Grid / Stat Cards |
| **FR-003** (Duración entera > 0) | AS-003 | T008, T018 | TC-003 | Input "Duración (Días enteros)" |
| **FR-004** (Patología ACTIVA Spec 009) | AS-004 | T011, T024, T026 | TC-005 | Dropdown "Patología vinculada" / Filtro |
| **FR-005** (Fármaco inventario central) | AS-005 | T011, T025, T026 | TC-005 | Dropdown "Medicamento activo" |
| **FR-006** (Sin descuento de bodega) | AS-001 | T025, T026 | TC-001 | Banner Informativo superior |
| **FR-007** (Inmutabilidad histórica) | AS-008 | T009, T036 | TC-011 | Acción "Editar" (ícono lápiz) |
| **FR-008** (Evento Outbox Kafka) | AS-001 | T023, T026, T045 | TC-001, TC-014 | (Evento asíncrono en BD) |
| **FR-009** (Trazabilidad auditoría) | AS-001, AS-007 | T007, T013, T026 | TC-006, TC-011 | (Asiento append-only en BD) |
| **FR-010** (Prohibido DELETE) | AS-010 | T039, T040, T043 | TC-012, TC-013 | ❌ Ausencia de botón Eliminar / Warning Box |
| **FR-011** (Concurrencia e Idempotencia) | AS-011, AS-012 | T015, T030, T042 | TC-008, TC-015 | Control concurrente `@Version` |
| **FR-012** (Desactivación lógica) | AS-007 | T009, T036, T043, T052 | TC-007 | Badge "Activa" / "Inactiva" |

### 7.2 Trazabilidad de Métricas de Éxito (SC ↔ Tareas ↔ TC ↔ Verificación)

| SC (Spec) | Tarea(s) Plan | Test Case (TC) | Verificación Técnica |
| :--- | :--- | :--- | :--- |
| **SC-001** (Disponibilidad inmediata) | T016, T028 | TC-001 | Test contrato POST 201 + verificación de consulta en GET |
| **SC-002** (Cero descuento en inventario) | T025, T026 | TC-001, TC-005 | Verificación de solo lectura en adaptadores de bodega |
| **SC-003** (Inmutabilidad prescripciones) | T033, T036 | TC-011 | Test integración Testcontainers sobre diagnósticos previos |
| **SC-004** (Bloqueo por rol no autorizado) | T019 | TC-004 | RoleValidationFilter denegando acceso HTTP 403 |
| **SC-005** (Latencia < 250 ms) | T050 | TC-016 | Pruebas de carga Gatling/k6 bajo concurrencia sostenida |
| **SC-006** (Prohibición DELETE) | T039, T040, T043 | TC-012, TC-013 | SecurityConfig bloqueando DELETE (HTTP 405) + ausencia SQL DELETE |
| **SC-007** (Atomicidad Outbox) | T021, T041 | TC-006, TC-014 | Test integración transaccional rollback + Outbox Relay Kafka |
| **SC-008** (Exclusión inactivas) | T009, T011, T036, T052 | TC-007 | Invariante activa = false + método listarActivas() |

---

## 8. Notes

- Cada tarea cuenta con su identificador único `T0xx` para seguimiento en tableros Kanban o Jira.
- Las escrituras que involucran `san_medicaciones`, `san_outbox` y `san_auditoria` se ejecutan dentro del mismo `@Transactional` de Spring estándar.
- El uso de **Virtual Threads** (Project Loom) permite manejar la concurrencia de peticiones HTTP de forma eficiente sin la complejidad de la programación reactiva, manteniendo el modelo imperativo de Spring MVC.
- La separación estricta de puertos y adaptadores garantiza que el dominio sea testeable de forma aislada y que la lógica de negocio no dependa de frameworks.