# Implementation Plan: Registrar una enfermedad

**Date**: 2026-09-22  
**Specs**:  
- [009-RegistrarEnfermedad.md](../specs/Veterinario/009-RegistrarEnfermedad.md)

---

## 1. Summary

El módulo **Registrar una enfermedad** permite al Médico Veterinario gestionar el catálogo nosológico oficial de la granja avícola[cite: 4]. Registra patologías con código único nosológico, nombre clínico, nivel de riesgo, tipo etiológico, descripción y un indicador binario obligatorio de sacrificio sanitario[cite: 4, 5]. **No permite borrado físico ni altera diagnósticos clínicos pasados**[cite: 1, 4]. 

La solución emplea **Java 21** con **Spring Boot 3.x (Spring MVC + Spring Data JPA)** bajo **arquitectura hexagonal pura**, el patrón **Transactional Outbox** para publicar eventos garantizados hacia Kafka, control de **Concurrencia Optimista (`version`)** y trazabilidad inmutable en **`san_auditoria`**[cite: 1].

---

## 2. Technical Context

- **Language/Version**: Java 21 (LTS - Virtual Threads habilitados mediante `spring.threads.virtual.enabled=true`).
- **Primary Dependencies**: Spring Boot 3.x (Spring MVC), Spring Cloud Stream (Kafka Binder), Spring Data JPA (Hibernate 6.x), PostgreSQL JDBC Driver, Lombok, MapStruct, Jakarta Validation, JUnit 5, Mockito, Testcontainers (PostgreSQL + Kafka).
- **Storage**: PostgreSQL 16+ relacional vía JDBC/JPA (`san_enfermedades`, `san_outbox`, `san_auditoria`).
- **Testing**: JUnit 5, Mockito, MockMvc, Testcontainers.
- **Target Platform**: Contenedores Linux (Docker / Kubernetes).
- **Project Type**: Backend REST micro-service (Módulo 2: Sanidad y Bioseguridad).
- **Performance Goals**: Latencia < 250 ms en validación y persistencia (`SC-006`); disponibilidad de consulta < 1 s (`SC-001`)[cite: 1].
- **Constraints**: 
  - Prohibido el borrado físico (`DELETE` SQL) (`FR-013`, `SC-007`)[cite: 1].
  - Código nosológico único a nivel de base de datos (`FR-003`, `SC-005`)[cite: 5].
  - Escritura atómica obligatoria de entidad + outbox + auditoría (`FR-011`, `FR-012`, `SC-008`)[cite: 1].
  - Rol exclusivo `VETERINARIO` (`FR-001`, `SC-004`)[cite: 1].
  - Selección explícita del indicador de sacrificio, sin valores por defecto (`FR-007`, `SC-003`).
  - Idempotencia HTTP mediante cabecera `X-Idempotency-Key` retenida por 24 horas (`FR-014`)[cite: 1].

---

## 3. Project Structure

```text
src/main/java/com/avicontrol/sanidad/
├── domain/                                # Núcleo Puro de Dominio (Sin dependencias Spring/JPA)
│   ├── model/
│   │   └── enfermedad/
│   │       ├── Enfermedad.java            # Aggregate Root
│   │       ├── EnfermedadId.java          # Value Object UUID
│   │       ├── CodigoNosologico.java      # Value Object String único ("ID-ENF-XXX")
│   │       ├── NombreClinico.java         # Value Object String no vacío
│   │       ├── NivelRiesgo.java           # Enum: BAJO, MEDIO, ALTO, CRITICO
│   │       ├── TipoEtiologico.java        # Enum: VIRAL, BACTERIANA, PARASITARIA, FUNGICA
│   │       └── DescripcionClinica.java    # Value Object texto >= 10 chars
│   ├── exception/
│   │   └── enfermedad/
│   │       ├── EnfermedadNotFoundException.java
│   │       ├── CodigoNosologicoDuplicadoException.java
│   │       ├── DescripcionInvalidaException.java
│   │       ├── SacrificioNoSeleccionadoException.java
│   │       └── ConcurrenciaOptimistaException.java
│   └── repository/                        # Puertos de Salida (Driven Ports)
│       ├── EnfermedadRepositoryPort.java  # Persistencia del Agregado
│       ├── OutboxRepositoryPort.java      # Registro local para Transactional Outbox
│       └── AuditoriaSanitariaPort.java    # Bitácora inmutable en san_auditoria
├── application/
│   └── enfermedad/                        # Casos de Uso (Una clase por responsabilidad)
│       ├── RegistrarEnfermedadUseCase.java
│       ├── EditarEnfermedadUseCase.java
│       ├── ConsultarEnfermedadUseCase.java
│       └── ListarEnfermedadesUseCase.java
├── infrastructure/
│   ├── adapter/in/rest/                   # Adaptador Primario REST (Spring MVC)
│   │   ├── ApiErrorResponse.java
│   │   ├── GlobalExceptionHandler.java    # @RestControllerAdvice
│   │   ├── filter/
│   │   │   ├── RoleValidationFilter.java
│   │   │   └── IdempotencyFilter.java
│   │   └── enfermedad/
│   │       ├── EnfermedadController.java
│   │       ├── dto/
│   │       │   ├── CrearEnfermedadRequest.java
│   │       │   ├── EditarEnfermedadRequest.java
│   │       │   ├── EnfermedadFiltroRequest.java
│   │       │   └── EnfermedadResponse.java
│   │       └── mapper/
│   │           └── EnfermedadRestMapper.java
│   ├── adapter/out/
│   │   ├── persistence/                   # Adaptadores Secundarios JPA
│   │   │   ├── enfermedad/
│   │   │   │   ├── EnfermedadEntity.java
│   │   │   │   ├── EnfermedadJpaRepository.java
│   │   │   │   ├── EnfermedadRepositoryAdapter.java
│   │   │   │   └── mapper/EnfermedadPersistenceMapper.java
│   │   │   ├── outbox/
│   │   │   │   ├── OutboxEntity.java
│   │   │   │   ├── OutboxJpaRepository.java
│   │   │   │   └── OutboxRepositoryAdapter.java
│   │   │   └── auditoria/
│   │   │       ├── AuditoriaEntity.java
│   │   │       ├── AuditoriaJpaRepository.java
│   │   │       └── AuditoriaRepositoryAdapter.java
│   │   └── event/                         # Transactional Outbox Relay
│   │       ├── OutboxRelayScheduler.java  # Worker periódico hacia Kafka
│   │       └── KafkaEventPublisherAdapter.java
│   └── config/
│       ├── BeanConfiguration.java         # Registro explícito de casos de uso
│       └── SecurityConfig.java            # Bloqueo estricto de DELETE
└── events/                                # Contratos de Eventos de Integración
    ├── EnfermedadRegistradaIntegrationEvent.java
    └── EnfermedadActualizadaIntegrationEvent.java
```

> **Decisión de Estructura**: Microservicio estándar basado en Spring MVC imperativo con Virtual Threads de Java 21, desacoplando el dominio de JPA mediante mappers específicos. Cada caso de uso en `application/enfermedad/` es una clase concreta con responsabilidad única[cite: 1].

---

## 4. Implementation Phases

### Phase 1: Setup (Shared Infrastructure)
**Propósito**: Inicializar el proyecto base, configuración estándar de Spring Boot y herramientas de calidad.

- [ ] **T001** Inicializar proyecto Spring Boot 3.x con Java 21 y dependencias estándar (Spring Web, Spring Data JPA, Kafka Stream, Validation, Testcontainers, PostgreSQL Driver).
- [ ] **T002** Generar la estructura de paquetes hexagonal según la convención del proyecto (`domain`, `application`, `infrastructure`, `events`).
- [ ] **T003** Configurar `application.yml` con pool HikariCP, parámetros de Kafka (`sanitary.disease.registered.v1`) y habilitar Virtual Threads (`spring.threads.virtual.enabled=true`).
- [ ] **T004** Configurar Docker Compose local con servicios `postgres:16-alpine` y broker `kafka` (bitnami/kafka:latest con KRaft).
- [ ] **T005** Configurar Checkstyle, SpotBugs y Jacoco con umbral mínimo de cobertura del 85%.
- [ ] **T006** Configurar pipeline de CI en GitHub Actions para compilar, ejecutar tests con Testcontainers y validar linters.

---

### Phase 2: Foundational (Blocking Prerequisites)
**Propósito**: Construir el modelo de dominio puro, la persistencia JPA, el Transactional Outbox y la auditoría inmutable.  
> ⚠️ **CRÍTICO**: Ningún caso de uso funcional debe implementarse antes de validar esta fase.

- [ ] **T007** Crear scripts de migración DDL para tablas `san_enfermedades`, `san_outbox` y `san_auditoria` con llaves primarias UUID. Crear índice único sobre la columna `codigo` de `san_enfermedades` (`FR-003`)[cite: 5].
- [ ] **T008** Definir Value Objects de Dominio con validación estricta:
  - `EnfermedadId.java`: Envoltorio inmutable sobre UUID.
  - `CodigoNosologico.java`: Cadena obligatoria con formato `ID-ENF-XXX`[cite: 5].
  - `NombreClinico.java`: String obligatorio, no vacío, sin espacios puros.
  - `NivelRiesgo.java`: Enum estricto `BAJO`, `MEDIO`, `ALTO`, `CRITICO`[cite: 5].
  - `TipoEtiologico.java`: Enum estricto `VIRAL`, `BACTERIANA`, `PARASITARIA`, `FUNGICA`[cite: 5].
  - `DescripcionClinica.java`: Texto obligatorio, mínimo 10 caracteres tras normalización con `.trim()`.
- [ ] **T009** Crear Aggregate Root `Enfermedad.java` con constructor privado, métodos de fábrica estáticos `registrar()` y mutador `actualizarDatos()` que incremente la versión optimista.
- [ ] **T010** Crear excepciones de dominio en `domain/exception/enfermedad/` (`EnfermedadNotFoundException`, `CodigoNosologicoDuplicadoException`, `DescripcionInvalidaException`, `SacrificioNoSeleccionadoException`, `ConcurrenciaOptimistaException`).
- [ ] **T011** Definir puertos secundarios en `domain/repository/`:
  - `EnfermedadRepositoryPort.java`: métodos `guardar(Enfermedad e)`, `buscarPorId(EnfermedadId id)`, `buscarPorCodigo(CodigoNosologico codigo)`, `existePorCodigo(CodigoNosologico codigo)`, `listarTodas()`, `listarActivas()`.
  - `OutboxRepositoryPort.java`: método `guardarEvento(OutboxEntity evento)`.
  - `AuditoriaSanitariaPort.java`: método inmutable `registrarTraza(AuditoriaEntry traza)`.
- [ ] **T012** Implementar entidades JPA (`EnfermedadEntity.java`, `OutboxEntity.java`, `AuditoriaEntity.java`) y repositorios `JpaRepository`.
- [ ] **T013** Implementar adaptadores JPA que cumplan los puertos secundarios garantizando el mapeo de dominio vía `EnfermedadPersistenceMapper`.
- [ ] **T014** Implementar `GlobalExceptionHandler` con `@RestControllerAdvice` para transformar excepciones de dominio y validación a esquemas RFC-7807 (`ApiErrorResponse`) con códigos HTTP adecuados (400, 403, 404, 409).
- [ ] **T015** Implementar `IdempotencyFilter` (`OncePerRequestFilter`) para validar y cachear respuestas asociadas a la cabecera `X-Idempotency-Key` durante 24 horas (`FR-014`)[cite: 1].
- [ ] **T016** Implementar `RoleValidationFilter` para verificar el claim `VETERINARIO` en todas las peticiones a `/api/v1/sanitary/enfermedades/**` (`FR-001`)[cite: 1].

> **Checkpoint**: Dominio puro compilando, tablas creadas, infraestructura JPA operativa y filtros transversales listos.

---

### Phase 3: User Story 1 – Alta de Patología en el Catálogo Nosológico (Priority: P1)
**Propósito**: Permitir al Veterinario registrar una enfermedad con código único, nombre, nivel de riesgo, tipo etiológico, descripción e indicador explícito de sacrificio sanitario, persistiendo atómicamente la entidad, la auditoría y el evento Outbox[cite: 4, 5].  
**Test Independiente**: `POST /api/v1/sanitary/enfermedades` con datos válidos y rol `VETERINARIO` retorna `201 Created`[cite: 1]. La enfermedad queda visible en la consulta, se genera una traza en `san_auditoria`, se almacena el mensaje en `san_outbox` y queda disponible para selección clínica inmediata[cite: 1].

#### Tests para User Story 1
- [ ] **T017** `[P]` `[US1]` `[TC-001]` Test de contrato: `POST /api/v1/sanitary/enfermedades` → 201 Created con cabecera `Location` y payload JSON (`EnfermedadControllerTest` vía MockMvc).
- [ ] **T018** `[P]` `[US1]` `[TC-002]` Test de contrato: `POST /api/v1/sanitary/enfermedades` sin rol `VETERINARIO` → 403 Forbidden[cite: 1].
- [ ] **T019** `[P]` `[US1]` `[TC-003]` Test de contrato: `POST /api/v1/sanitary/enfermedades` con código duplicado "ID-ENF-001" → 409 Conflict[cite: 5].
- [ ] **T020** `[P]` `[US1]` `[TC-004]` Test de contrato: `POST /api/v1/sanitary/enfermedades` omitiendo campos obligatorios o enviando espacios en blanco → 400 Bad Request.
- [ ] **T021** `[P]` `[US1]` `[TC-005]` Test de contrato: `POST /api/v1/sanitary/enfermedades` sin seleccionar explícitamente el indicador de sacrificio (`null`) → 400 Bad Request.
- [ ] **T022** `[P]` `[US1]` `[TC-006]` Test unitario de `RegistrarEnfermedadUseCase` validando orquestación, unicidad de código y creación del agregado con invariantes de dominio.
- [ ] **T023** `[P]` `[US1]` `[TC-007]` Test de integración con Testcontainers (PostgreSQL): Confirmar atomicidad transaccional (si falla auditoría u outbox, la enfermedad no se persiste).
- [ ] **T024** `[P]` `[US1]` `[TC-008]` Test de integración con Testcontainers (Kafka): Verificar que el evento `EnfermedadRegistradaIntegrationEvent` se publique en el tópico `sanitary.disease.registered.v1`.

#### Implementación de User Story 1
- [ ] **T025** `[US1]` Crear DTOs de entrada y salida: `CrearEnfermedadRequest.java` con validaciones Jakarta (`@NotBlank`, `@Size(min=10)` para descripción, `@NotNull` para el booleano) y `EnfermedadResponse.java`.
- [ ] **T026** `[US1]` Definir el contrato de evento `EnfermedadRegistradaIntegrationEvent.java` con schema JSON normalizado (`eventId`, `aggregateId`, `codigo`, `nombre`, `nivelRiesgo`, `tipo`, `requiereSacrificioSanitario`).
- [ ] **T027** `[US1]` Crear `EnfermedadRestMapper.java` con MapStruct para transformar entre DTOs y comandos/modelos de dominio.
- [ ] **T028** `[US1]` Implementar `RegistrarEnfermedadUseCase.java` en `application/enfermedad/`:
  - Validar claim de rol `VETERINARIO`[cite: 1].
  - Verificar unicidad del código nosológico vía `EnfermedadRepositoryPort.existePorCodigo()`.
  - Instanciar aggregate root `Enfermedad.registrar(...)` ejecutando invariantes de los Value Objects.
  - Ejecutar `@Transactional`: persistir en `san_enfermedades`, encolar evento en `san_outbox` y guardar firma profesional en `san_auditoria`.
  - Retornar el modelo de dominio `Enfermedad`.
- [ ] **T029** `[US1]` Implementar `ConsultarEnfermedadUseCase.java` en `application/enfermedad/` para obtener el detalle de una patología por su UUID (`buscarPorId`).
- [ ] **T030** `[US1]` Implementar endpoints `POST /api/v1/sanitary/enfermedades` y `GET /api/v1/sanitary/enfermedades/{id}` en `EnfermedadController.java`.

> **Checkpoint**: Registro de enfermedad funcional, atómico y validado bajo pruebas automáticas.

---

### Phase 4: User Story 2 – Actualización del Catálogo y Blindaje Histórico (Priority: P2)
**Propósito**: Permitir al Veterinario actualizar los datos de una enfermedad existente, garantizando control de concurrencia optimista y protegiendo la inmutabilidad de diagnósticos pasados[cite: 4].  
**Test Independiente**: Modificar el nivel de riesgo de una enfermedad de "Medio" a "Crítico" vía `PUT /api/v1/sanitary/enfermedades/{id}` eleva su versión a 2[cite: 5]. Los diagnósticos históricos asociados conservan los valores originales sin recálculo[cite: 4].

#### Tests para User Story 2
- [ ] **T031** `[P]` `[US2]` `[TC-009]` Test de contrato: `PUT /api/v1/sanitary/enfermedades/{id}` con datos válidos y versión coincidente → 200 OK.
- [ ] **T032** `[P]` `[US2]` `[TC-010]` Test de contrato: `PUT /api/v1/sanitary/enfermedades/{id}` con modificación concurrente y versión desactualizada → 409 Conflict (`ConcurrenciaOptimistaException`).
- [ ] **T033** `[P]` `[US2]` `[TC-011]` Test de contrato: `PUT /api/v1/sanitary/enfermedades/{id}` con ID inexistente → 404 Not Found.
- [ ] **T034** `[P]` `[US2]` `[TC-012]` Test unitario de `EditarEnfermedadUseCase`: Verificar que los cambios eleven la versión y generen el evento `EnfermedadActualizadaIntegrationEvent`.
- [ ] **T035** `[P]` `[US2]` `[TC-013]` Test de integración con Testcontainers: Verificar que la actualización modifique únicamente la tupla seleccionada y genere su respectiva traza histórica en `san_auditoria`.

#### Implementación de User Story 2
- [ ] **T036** `[US2]` Crear DTO `EditarEnfermedadRequest.java` con campos mutables (`nombre`, `nivelRiesgo`, `tipo`, `descripcion`, `requiereSacrificioSanitario`, `versionActual`).
- [ ] **T037** `[US2]` Definir contrato de evento `EnfermedadActualizadaIntegrationEvent.java`.
- [ ] **T038** `[US2]` Implementar `EditarEnfermedadUseCase.java` en `application/enfermedad/`:
  - Obtener agregado por ID vía `EnfermedadRepositoryPort.buscarPorId(id)`; si no existe lanzar `EnfermedadNotFoundException` (404).
  - Verificar que la versión suministrada coincida con la versión actual del agregado; lanzar `ConcurrenciaOptimistaException` si difieren.
  - Invocar método de dominio `enfermedad.actualizarDatos(...)`.
  - Ejecutar `@Transactional`: persistir cambios, escribir evento en `san_outbox` y asentar modificación técnica en `san_auditoria`.
- [ ] **T039** `[US2]` Implementar `ListarEnfermedadesUseCase.java` en `application/enfermedad/` para soportar consultas paginadas con filtros por nombre, riesgo, tipo y sacrificio sanitario[cite: 5].
- [ ] **T040** `[US2]` Añadir endpoints `PUT /api/v1/sanitary/enfermedades/{id}` y `GET /api/v1/sanitary/enfermedades` en `EnfermedadController.java`.

> **Checkpoint**: Actualización de enfermedades operativa, concurrencia protegida e integridad histórica garantizada.

---

### Phase 5: User Story 3 – Integridad Transaccional, Blindaje contra Borrado e Idempotencia (Priority: P3)
**Propósito**: Asegurar que las enfermedades no se puedan eliminar físicamente (`DELETE`), despachar eventos hacia Kafka desde la tabla Outbox de forma resiliente y garantizar idempotencia[cite: 1].  
**Test Independiente**: Ejecutar `DELETE /api/v1/sanitary/enfermedades/{id}` retorna `405 Method Not Allowed`[cite: 1]. Los eventos en `san_outbox` en estado `PENDING` se publican en Kafka y cambian a `PROCESSED`.

#### Tests para User Story 3
- [ ] **T041** `[P]` `[US3]` `[TC-014]` Test de contrato y seguridad: Comprobar rechazo a llamadas HTTP `DELETE` → 405 Method Not Allowed[cite: 1].
- [ ] **T042** `[P]` `[US3]` `[TC-015]` Test de base de datos: Verificar mediante test de repositorio la inexistencia de sentencias o métodos de borrado físico directo (`DELETE`) en la capa de persistencia[cite: 1].
- [ ] **T043** `[P]` `[US3]` `[TC-016]` Test de integración Outbox Relay con Testcontainers (Kafka): Verificar lectura por lotes de `san_outbox` y publicación efectiva en el tópico `sanitary.disease.registered.v1`.
- [ ] **T044** `[P]` `[US3]` `[TC-017]` Test de idempotencia: Enviar dos requests consecutivas idénticas con el mismo `X-Idempotency-Key` y verificar que la segunda retorna la respuesta original sin duplicar inserciones.

#### Implementación de User Story 3
- [ ] **T045** `[US3]` Configurar `SecurityConfig.java` bloqueando explícitamente cualquier verbo `DELETE` sobre rutas `/api/v1/sanitary/**`[cite: 1].
- [ ] **T046** `[US3]` Implementar `KafkaEventPublisherAdapter.java` publicando mensajes tipados hacia el tópico Kafka mediante `StreamBridge` o `KafkaTemplate`.
- [ ] **T047** `[US3]` Implementar `OutboxRelayScheduler.java`:
  - Polling periódico con `@Scheduled(fixedDelay = 2000)` sobre `san_outbox` donde `status = 'PENDING'` usando `SELECT ... FOR UPDATE SKIP LOCKED`.
  - Despacho a Kafka mediante `KafkaEventPublisherAdapter`.
  - Actualización atómica de estado a `PROCESSED` con marca temporal `processed_at`.
  - Manejo de reintentos y marcado a `FAILED` si supera 5 reintentos con backoff exponencial.
- [ ] **T048** `[US3]` Documentar contratos de mensajería asíncrona mediante especificación AsyncAPI 3.0 en `docs/asyncapi/sanitary-events.yml`.
- [ ] **T049** `[US3]` Configurar especificación OpenAPI 3.0 (Swagger UI) exponiendo documentación de endpoints REST.

> **Checkpoint**: Sistema de mensajería Outbox confiable, tolerancia a caídas de red y blindaje absoluto contra borrado físico.

---

### Phase 6: Polish & Cross-Cutting Concerns
**Propósito**: Asegurar la calidad técnica, observabilidad y correspondencia exacta con las pantallas del sistema.

- [ ] **T050** Configurar logging estructurado en formato JSON incorporando `traceId`, `spanId` y `correlationId` vía MDC de Slf4j.
- [ ] **T051** Exponer métricas Prometheus con Micrometer (`sanitary_enfermedad_created_total`, `sanitary_outbox_lag_seconds`).
- [ ] **T052** `[TC-018]` Implementar pruebas de carga con Gatling/k6 validando latencia < 250 ms bajo concurrencia sostenida de 200 req/s.
- [ ] **T053** Auditoría de dependencias: Validar mediante ArchUnit que el paquete `domain/` mantenga cero imports de Spring, JPA/Hibernate, Jackson o librerías externas.
- [ ] **T054** Verificar correspondencia campo a campo entre el DTO de respuesta y la vista Figma importada (`5.1 Listado y búsqueda de enfermedades`), asegurando que el estado `activa` (Boolean) se refleje visualmente en la tabla y confirmando la inexistencia de controles o botones de borrado físico en la UI.

---

## 5. Dependencies & Execution Order

### Phase Dependencies
- **Phase 1 (Setup)**: Sin dependencias — inicia de inmediato.
- **Phase 2 (Foundational)**: Requiere Fase 1 completa — **bloquea todas las user stories**.
- **Phase 3 (User Story 1)**: Requiere Fase 2 completada (bloqueante).
- **Phase 4 (User Story 2)**: Requiere Fase 3 (necesita existir la enfermedad base).
- **Phase 5 (User Story 3 / Outbox Relay)**: Puede desarrollarse en paralelo con Fase 4 tras concluir Fase 3.
- **Phase 6 (Polish)**: Requiere todas las fases funcionales implementadas.

### User Story Dependencies
- **US1 (Alta)**: Depende de infraestructura base y validaciones de unicidad.
- **US2 (Edición)**: Depende de US1 (la entidad debe existir previamente en el catálogo).
- **US3 (Outbox & Idempotencia)**: Transversal a US1 y US2; procesa los eventos generados por ambas historias.

---

## 6. API Contracts (Endpoints ↔ FR ↔ DTOs)

Contratos REST formales que vinculan cada endpoint con los requerimientos funcionales que cubre y sus modelos de transferencia de datos.

| Endpoint | Método | FRs Cubiertos | Request DTO | Response DTO | Código Éxito |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `/api/v1/sanitary/enfermedades` | POST | FR-001, FR-002, FR-003, FR-004, FR-005, FR-006, FR-007, FR-008, FR-009, FR-011, FR-012, FR-014 | `CrearEnfermedadRequest` | `EnfermedadResponse` | 201 Created |
| `/api/v1/sanitary/enfermedades/{id}` | PUT | FR-010, FR-014, FR-015 | `EditarEnfermedadRequest` | `EnfermedadResponse` | 200 OK |
| `/api/v1/sanitary/enfermedades/{id}` | GET | FR-002 | — | `EnfermedadResponse` | 200 OK |
| `/api/v1/sanitary/enfermedades` | GET | FR-002, FR-015 | `EnfermedadFiltroRequest` | `Page<EnfermedadResponse>` | 200 OK |

## 7. Traceability Matrix Unificada (3-Vías)

### 7.1 Trazabilidad Funcional (FR ↔ AS ↔ Tareas ↔ TC ↔ UI)

| FR (Spec) | AS (BDD) | Tarea(s) Plan | Test Case (TC) | Componente UI (Prototipo) |
| :--- | :--- | :--- | :--- | :--- |
| **FR-001** (Rol VETERINARIO) | AS-006 | T016, T018, T028 | TC-002 | Botón "+ Registrar enfermedad" |
| **FR-002** (Campos obligatorios) | AS-001, AS-004 | T008, T020, T025 | TC-004 | Inputs formulario / Data Grid |
| **FR-003** (Código único) | AS-003 | T007, T019, T028 | TC-003 | Input "Código Nosológico Oficial" |
| **FR-004** (Texto ≥ 10 chars) | AS-004 | T008, T020 | TC-004 | Textarea "Descripción Sintomática" |
| **FR-005** (Enum Riesgo) | AS-001 | T008, T020 | TC-001 | Select "Nivel de Riesgo" |
| **FR-006** (Enum Tipo) | AS-001 | T008, T020 | TC-001 | Select "Tipo Etiológico" |
| **FR-007** (Sacrificio explícito) | AS-005 | T008, T021, T025 | TC-005 | Radios "¿Requiere Sacrificio?" |
| **FR-008** (Habilita Spec 012) | AS-002 | T026, T028 | TC-001, TC-005 | Radio "Sí (Letal)" seleccionado |
| **FR-009** (Exige Spec 008) | AS-001 | T026, T028 | TC-001, TC-005 | Radio "No (Tratable)" seleccionado |
| **FR-010** (@Version optimista) | AS-007, AS-009 | T009, T032, T038 | TC-010 | Botón "Editar" (ícono lápiz) |
| **FR-011** (Outbox Kafka) | AS-001, AS-002 | T026, T028, T047 | TC-008, TC-016 | (Evento asíncrono en BD) |
| **FR-012** (Auditoría inmutable) | AS-001, AS-007 | T007, T013, T028 | TC-007, TC-013 | (Asiento append-only en BD) |
| **FR-013** (Prohibido DELETE) | AS-010 | T041, T042, T045 | TC-014, TC-015 | ❌ Ausencia de botón Eliminar |
| **FR-014** (Idempotencia) | AS-011 | T015, T032, T044 | TC-010, TC-017 | Control de doble clic |
| **FR-015** (Desactivación lógica) | AS-007 | T009, T011, T038, T054 | TC-009, TC-012 | Badge "Activa" / "Inactiva" |

### 7.2 Trazabilidad de Métricas de Éxito (SC ↔ Tareas ↔ TC ↔ Verificación)

| SC (Spec) | Tarea(s) Plan | Test Case (TC) | Verificación Técnica |
| :--- | :--- | :--- | :--- |
| **SC-001** (Disponibilidad inmediata) | T017, T030 | TC-001 | Test contrato POST 201 + verificación de consulta en GET |
| **SC-002** (Inmutabilidad histórica) | T035 | TC-013 | Test integración Testcontainers sobre diagnósticos previos |
| **SC-003** (Selección binaria sacrificio) | T021 | TC-005 | Test contrato sin selección de booleano → HTTP 400 |
| **SC-004** (Bloqueo por rol no autorizado) | T018 | TC-002 | Test contrato con rol TRABAJADOR → HTTP 403 |
| **SC-005** (Código nosológico único) | T007, T019 | TC-003 | Índice único en DDL + test contrato código duplicado → HTTP 409 |
| **SC-006** (Latencia < 250 ms) | T052 | TC-018 | Pruebas de carga Gatling/k6 bajo concurrencia de 200 req/s |
| **SC-007** (Prohibición DELETE) | T041, T042, T045 | TC-014, TC-015 | SecurityConfig bloqueando DELETE (HTTP 405) + ausencia SQL DELETE |
| **SC-008** (Atomicidad Outbox) | T023, T043 | TC-007, TC-016 | Test integración transaccional rollback + Outbox Relay Kafka |
| **SC-009** (Exclusión inactivas) | T009, T011, T038, T054 | TC-009, TC-012 | Invariante activa = false + método listarActivas() |

---

## 8. Notes

- Cada tarea cuenta con su identificador único `T0xx` para seguimiento en tableros Kanban o Jira.
- Las escrituras que involucran `san_enfermedades`, `san_outbox` y `san_auditoria` se ejecutan dentro del mismo bloque `@Transactional` de Spring Data JPA.
- El uso de **Virtual Threads** (Project Loom) permite manejar la concurrencia de peticiones HTTP de forma eficiente sin la complejidad de la programación reactiva, manteniendo el modelo imperativo de Spring MVC.
- La separación estricta de puertos y adaptadores garantiza que el dominio sea testeable de forma aislada y que la lógica de negocio no dependa de frameworks.
- El código nosológico (`ID-ENF-XXX`) es la clave de negocio que garantiza la unicidad del catálogo ante entes de control zoosanitario[cite: 5].