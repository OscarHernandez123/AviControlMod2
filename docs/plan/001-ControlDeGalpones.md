# Implementation Plan: Control de Galpones

**Date**: 2026-09-21
**Specs**:
- [001-RegistroGalpon.md](https://github.com/your-org/AviControlMod2/blob/main/docs/spec/001-RegistroGalpon.md)  
- [007-ConsultarGalpon.md](https://github.com/your-org/AviControlMod2/blob/main/docs/specs/007-ConsultarGalpon.md)

## Summary

El módulo **Control de Galpones** permite registrar galpones y consultar su información operativa (capacidad, estado y datos del lote alojado). La solución usa **Java 21**, arquitectura **hexagonal basada en eventos** y un bus de eventos (Kafka) para comunicar cambios de estado entre módulos. No existen sensores ni sistema de riego; la única interacción con el módulo es la consulta de datos proporcionados por el módulo 1.

## Technical Context

- **Language/Version**: Java 21
- **Primary Dependencies**: Spring Boot 3.x, Spring WebFlux, Spring Cloud Stream (Kafka), Spring Data R2DBC, Lombok, MapStruct, Jakarta Validation, JUnit 5, Testcontainers, Reactor
- **Storage**: PostgreSQL (R2DBC), esquema manual
- **Testing**: JUnit 5, Mockito, Spring Boot Test, WebTestClient, Testcontainers (Kafka + PostgreSQL)
- **Target Platform**: Linux containers (Docker / Kubernetes)
- **Project Type**: Backend micro‑service (reactivo)
- **Performance Goals**: Consulta < 1 s; latencia < 200 ms para procesamiento de eventos críticos
- **Constraints**: Sólo soft‑delete de galpones; operación en entornos con conectividad intermitente al bus de eventos
- **Scale/Scope**: Base para todos los módulos de gestión de granjas; bloquea funcionalidades dependientes del módulo 1.

## Project Structure

```text
src/main/java/com/avicontrolegalpones/
├─ domain/                # Entidades y puertos de salida
│   ├─ model/            # Galpon, Lote (solo lectura)
│   └─ port/out/         # Repositorios y publicador de eventos
├─ application/          # Casos de uso (un caso por responsabilidad)
│   └─ galpon/           # Registro, consulta, actualización, desactivación
├─ infrastructure/        # Adaptadores externos
│   ├─ adapter/in/rest/  # Controladores REST
│   └─ adapter/out/persistence/ # R2DBC repositorios
└─ events/               # Definiciones de eventos y DTOs
    └─ GalponCreatedEvent.java
```

**Structure Decision**: Single‑module micro‑service (Option 1) porque el dominio es autocontenido y los adaptadores son pocos.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicializar proyecto y preparar infraestructura base.

- [ ] **T001** Crear proyecto Spring Boot 3.x con Java 21 (WebFlux, R2DBC, Cloud Stream).
- [ ] **T002** Generar la estructura de paquetes hexagonal según la tabla anterior.
- [ ] **T003** Configurar `application.yml` con conexión a PostgreSQL (R2DBC) y Kafka (bootstrap servers).
- [ ] **T004** Añadir Docker Compose con servicios **postgres** y **kafka**.
- [ ] **T005** Configurar Checkstyle y SpotBugs.
- [ ] **T006** Añadir CI pipeline (GitHub Actions) que compile, ejecute tests y valide lint.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Proveer el núcleo de dominio y la infraestructura de persistencia/eventos.

- **⚠️ CRITICAL**: Ningún user story debe comenzar hasta completar esta fase.

- [ ] **T007** Definir tablas en PostgreSQL (`galpones`).
- [ ] **T008** Implementar entidades de dominio en `domain/model/`:
  - `Galpon.java` (id UUID, nombre, aforoMáximo, estado, activo, datos de lote opcional).
- [ ] **T009** Crear excepciones en `domain/exception/` (`GalponNotFoundException`, `AccesoNoAutorizadoException`).
- [ ] **T010** Definir puertos de salida (`GalponRepositoryPort`, `EventPublisherPort`).
- [ ] **T011** Implementar entidades R2DBC y repositorios Spring Data en `infrastructure/adapter/out/persistence/`.
- [ ] **T012** Implementar adapters que cumplen los puertos (repositorio y publicador Kafka).
- [ ] **T013** Implementar mappers de dominio ↔ persistence (`GalponPersistenceMapper`).
- [ ] **T014** Configurar bean `EventBusConfiguration` (Kafka producer/consumer).
- [ ] **T015** Implementar `GlobalExceptionHandler` para mapear excepciones a respuestas HTTP.
- **Checkpoint**: Dominio modelado, persistencia operativa y bus de eventos activo.

---

## Phase 3: User Story 1 – Registro y Gestión Básica de Galpones (P1)

**Goal**: Permitir al administrador crear, listar, actualizar y desactivar galpones (soft‑delete).

**Independent Test**: `POST /api/galpones` con datos válidos → 201 y el galpón aparece en `GET /api/galpones`. `PATCH /api/galpones/{id}/estado` desactiva el galpón.

### Tests

- [ ] **T016** Contract test: `POST /api/galpones` → 201 (`GalponControllerTest`).
- [ ] **T017** Contract test: `GET /api/galpones` lista galpones activos.
- [ ] **T018** Unit test `RegistrarGalponUseCase` con mock de `GalponRepositoryPort`.
- [ ] **T019** Integration test con Testcontainers: registro → persistencia → consulta.

### Implementation

- [ ] **T020** `RegistrarGalponUseCase` (validaciones, publicación `GalponCreatedEvent`).
- [ ] **T021** `ListarGalponesUseCase` (listado filtrado por activo).
- [ ] **T022** `ActualizarGalponUseCase` (edición de campos descriptivos; campos estructurales bloqueados si hay dependencias futuras).
- [ ] **T023** `DesactivarGalponUseCase` (soft‑delete, verifica permisos). 
- [ ] **T024** DTOs `GalponRequest.java`, `GalponResponse.java` con validaciones Jakarta.
- [ ] **T025** Mapper `GalponRestMapper`.
- [ ] **T026** `GalponController` con endpoints **POST**, **GET**, **PUT**, **PATCH /estado**.
- **Checkpoint**: Registro y gestión básica de galpones funcional.

---

## Phase 4: User Story 2 – Consulta de Información de Galpón (P1)

**Goal**: Permitir a administradores y usuarios consultar la información del galpón, incluyendo datos del lote alojado y cálculo de edad del lote.

**Independent Test**: `GET /api/galpones/{id}` devuelve nombre, aforo máximo, estado, población actual y edad del lote (cuando existe). Cuando no hay lote, indica indisponibilidad de población y edad.

### Tests

- [ ] **T027** Contract test: `GET /api/galpones/{id}` con lote activo → 200 y datos completos (`GalponControllerTest`).
- [ ] **T028** Contract test: `GET /api/galpones/{id}` sin lote → 200 y campos de población/edad marcados como no disponible.
- [ ] **T029** Contract test: acceso con rol no autorizado → 403.
- [ ] **T030** Unit test `ConsultarGalponUseCase` (cálculo de edad, manejo de ausencia de lote, manejo de fechas inconsistentes).
- [ ] **T031** Integration test con Testcontainers: consulta contra datos reales de galpón y lote.

### Implementation

- [ ] **T032** Definir `ConsultarGalponUseCase` en `application/galpon/` que:
  - Verifica autorización (rol admin/usuario).
  - Obtiene galpón vía `GalponRepositoryPort`.
  - Obtiene lote activo (si existe) mediante llamada al módulo 1 (simulada por puerto de salida `LoteInfoPort`).
  - Calcula edad del lote a partir de `fechaIngreso` y la fecha del sistema, manejando casos de fecha futura y años bisiestos.
- [ ] **T033** Crear interfaz `LoteInfoPort` (método `obtenerLoteActivo(UUID galponId)` devuelve opcional `LoteDto`).
- [ ] **T034** DTOs `LoteDto` (población, fechaIngreso) y `GalponDetailResponse` que incluye campos de lote y edad.
- [ ] **T035** Mapper `GalponDetailMapper` para combinar datos de galpón y lote.
- [ ] **T036** Añadir endpoint `GET /api/galpones/{id}` en `GalponController` que invoque `ConsultarGalponUseCase`.
- **Checkpoint**: Consulta de galpón completa y robusta.

---

## Phase 5: Polish & Cross‑Cutting Concerns

- [ ] **T037** Documentar eventos con **AsyncAPI** y generar UI Swagger/OpenAPI para endpoints REST.
- [ ] **T038** Añadir logging estructurado (MDC con correlation‑id de petición).
- [ ] **T039** Exponer métricas Prometheus (`galpon_query_seconds`, `galpones_total`).
- [ ] **T040** Implementar pruebas de carga para validar tiempo de respuesta < 1 s bajo 500 galpones concurrentes.
- [ ] **T041** Refactorizar listeners a clase base `BaseEventListener` (aunque en este módulo solo se usa `GalponCreatedEvent`).
- [ ] **T042** Verificar que ninguna clase bajo `domain/` importe Spring/R2DBC.

---

## Dependencies & Execution Order

**Phase Dependencies**
- **Setup** → none.
- **Foundational** → depends on Setup.
- **User Stories** (3‑4) → depend on Foundational; can run in parallel after it.
- **Polish** → depends on all user stories.

**User Story Dependencies**
- **US1** (Registro) → no dependencies.
- **US2** (Consulta) → depends on US1 (el galpón debe existir) y en tiempo de ejecución necesita datos del módulo 1 (`LoteInfoPort`).

---

## Open Questions / Clarifications

> [!QUESTION] **Interfaz con Módulo 1**: ¿Cuál es la firma exacta del método para obtener la información del lote (nombre del puerto, DTO estructurado, manejo de errores)?
>
> >!QUESTION **Formato de la edad**: ¿Se debe presentar la edad como `X semanas y Y días` o como número total de días?
>
> >!QUESTION **Autorización**: ¿Qué mecanismo de seguridad (JWT, roles en la base) se utiliza para validar los roles de administrador y usuario?
>
> >!QUESTION **Retención de datos**: ¿Cuánto tiempo se guardan los registros de galpones en la base antes de archivarse?
>
> >!QUESTION **Recursos de despliegue**: ¿Hay límites de CPU/memoria para el contenedor del micro‑service en Kubernetes?

---

## Notes

- Cada tarea lleva prefijo `T0xx` para trazabilidad.
- Los checkpoints indican validación antes de avanzar.
- Los casos de uso retornan `Mono<T>` o `Flux<T>` siguiendo la arquitectura reactiva.
- Mantener la **responsabilidad única**: dominio puro, casos de uso aislados, adaptadores externos.

---
