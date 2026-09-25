# Implementation Plan: Consulta y Seguimiento de Galpones

**Date**: 25/09/2026  
**Arquitectura y tecnologías**: [General.md](General.md)  
**Specs**:

- [003-ConsultarEdadPorGalpon.md](../specs/003-ConsultarEdadPorGalpon.md)
- [007-ConsultarGalpon.md](../specs/007-ConsultarGalpon.md)
- [019-ActualizarPoblacionActualPorGalpon.md](../specs/019-ActualizarPoblacionActualPorGalpon.md)

## Summary

El Módulo 2 debe permitir que administradores y usuarios autorizados consulten la información de los galpones y los lotes registrados para ellos. También debe permitir al administrador consultar la edad del lote vigente y visualizar el resumen general de galpones.

`Galpón` y `Lote` pertenecen exclusivamente al Módulo 1. El Módulo 2 no crea ni administra estas entidades: conserva proyecciones locales de solo lectura, sincronizadas mediante eventos de integración, para responder las consultas sin acceder directamente a la base de datos del Módulo 1. Cuando falte información vigente, se utilizará un puerto de consulta al Módulo 1.

Un galpón no contiene ni almacena directamente lotes. Cada lote conserva la llave foránea del galpón para el cual fue registrado. Para encontrar el lote vigente de un galpón, el Módulo 2 consulta sus proyecciones de lotes por `galponId` y aplica la regla funcional correspondiente, sin agregar una colección de lotes dentro del modelo de galpón.

Al confirmarse una mortalidad, el Módulo 2 emitirá una solicitud idempotente de descuento de población. El Módulo 1 realizará la modificación autoritativa del lote y publicará su resultado; el Módulo 2 actualizará su proyección local al consumir la confirmación.

## Technical Context

**Base técnica**: Definida en [General.md](General.md)  
**Integraciones específicas**: Eventos de galpón y lote provenientes del Módulo 1, puerto de consulta de respaldo y solicitud de descuento de población  
**Datos específicos**: Proyecciones locales de galpón y lote, más el seguimiento de solicitudes de descuento  
**Performance Goals**: El 95 % de las consultas de edad, detalle y resumen debe responder en máximo 1 segundo cuando las proyecciones estén sincronizadas  
**Constraints**: `Galpón` y `Lote` son propiedad del Módulo 1; no existen asignaciones entre trabajadores y galpones; el lote referencia al galpón mediante llave foránea; el día de ingreso cuenta como día 1; no se permiten edades ni poblaciones negativas  
**Scale/Scope**: Cuatro historias de usuario, tres endpoints REST de consulta, dos proyecciones locales y un flujo de eventos para actualizar la población viva

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   ├── 003-ConsultarEdadPorGalpon.md
│   ├── 007-ConsultarGalpon.md
│   └── 019-ActualizarPoblacionActualPorGalpon.md
└── plan/
    └── 001-ConsultaYSeguimientoDeGalpones.md    # Este archivo
```

### Source Code (repository root)

Clases nuevas que agrega este feature:

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/consulta/
│   │   ├── GalponConsultado.java
│   │   ├── LoteConsultado.java
│   │   ├── EdadLote.java
│   │   └── EstadoGalpon.java
│   ├── model/poblacion/
│   │   ├── SolicitudDescuentoPoblacion.java
│   │   └── EstadoSolicitudPoblacion.java
│   ├── exception/galpon/
│   │   ├── GalponNoEncontradoException.java
│   │   ├── LoteVigenteNoDisponibleException.java
│   │   ├── FechaIngresoInconsistenteException.java
│   │   └── ProyeccionNoDisponibleException.java
│   └── port/out/galpon/
│       ├── GalponQueryPort.java
│       ├── LoteQueryPort.java
│       ├── SolicitudDescuentoPoblacionPort.java
│       ├── Modulo1QueryPort.java
│       └── IntegrationEventPublisherPort.java
├── application/galpon/
│   ├── CalcularEdadLoteUseCase.java
│   ├── ConsultarEdadPorGalponUseCase.java
│   ├── ConsultarGalponUseCase.java
│   ├── ConsultarResumenGalponesUseCase.java
│   ├── SincronizarGalponYLoteUseCase.java
│   └── SolicitarDescuentoPoblacionUseCase.java
└── infrastructure/
    ├── adapter/in/rest/galpon/
    │   ├── GalponController.java
    │   ├── dto/
    │   │   ├── EdadLoteResponse.java
    │   │   ├── GalponDetalleResponse.java
    │   │   └── ResumenGalponesResponse.java
    │   └── mapper/GalponRestMapper.java
    ├── adapter/in/event/galpon/
    │   ├── Modulo1GalponEventListener.java
    │   ├── MortalidadConfirmadaEventListener.java
    │   └── dto/
    │       ├── GalponCreadoV1.java
    │       ├── GalponActualizadoV1.java
    │       ├── LoteRegistradoV1.java
    │       ├── PoblacionLoteActualizadaV1.java
    │       └── DescuentoPoblacionRechazadoV1.java
    ├── adapter/out/persistence/galpon/
    │   ├── entity/
    │   │   ├── GalponProjectionEntity.java
    │   │   ├── LoteProjectionEntity.java
    │   │   └── SolicitudDescuentoPoblacionEntity.java
    │   ├── repository/
    │   │   ├── GalponJpaRepository.java
    │   │   ├── LoteJpaRepository.java
    │   │   └── SolicitudDescuentoPoblacionJpaRepository.java
    │   ├── mapper/GalponPersistenceMapper.java
    │   └── GalponProjectionAdapter.java
    ├── adapter/out/integration/modulo1/
    │   └── Modulo1QueryAdapter.java
    ├── adapter/out/event/galpon/
    │   ├── GalponIntegrationEventPublisher.java
    │   └── dto/SolicitudDescuentoPoblacionV1.java
    └── config/
        ├── GalponBeanConfiguration.java
        └── ClockConfiguration.java

src/main/resources/
├── application.yml
└── db/migration/
    ├── V1__crear_proyecciones_galpon_y_lote.sql
    └── V2__crear_solicitud_descuento_poblacion.sql

src/test/java/com/avicontrol/
├── domain/consulta/
│   └── EdadLoteTest.java
├── application/galpon/
│   ├── CalcularEdadLoteUseCaseTest.java
│   ├── ConsultarEdadPorGalponUseCaseTest.java
│   ├── ConsultarGalponUseCaseTest.java
│   ├── ConsultarResumenGalponesUseCaseTest.java
│   └── SolicitarDescuentoPoblacionUseCaseTest.java
└── infrastructure/
    ├── adapter/in/rest/
    │   └── GalponControllerTest.java
    ├── adapter/in/event/
    │   └── Modulo1GalponEventListenerTest.java
    └── adapter/out/persistence/
        └── GalponProjectionAdapterTest.java
```

**Structure Decision**: La capacidad se distribuye en los paquetes comunes definidos en [General.md](General.md). `GalponConsultado` y `LoteConsultado` son modelos de lectura y sus nombres evitan confundirlos con las entidades autoritativas del Módulo 1. Los contratos recibidos se ubican en entrada; la solicitud de descuento producida por este feature se ubica en salida.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Preparar la estructura y configuración exclusiva de la capacidad de consulta y seguimiento de galpones.

- [ ] T001 Crear los paquetes de dominio, aplicación y adaptadores de galpones descritos en este plan.
- [ ] T002 Registrar la capacidad de galpones como módulo funcional y declarar su interfaz pública de aplicación.
- [ ] T003 Configurar las propiedades específicas de los topics y versiones de eventos intercambiados con el Módulo 1.
- [ ] T004 Configurar el endpoint de consulta de respaldo y el umbral de vigencia de las proyecciones.
- [ ] T005 Crear fixtures reutilizables de galpones, lotes, mortalidades y versiones de eventos para pruebas.
- [ ] T006 Crear la estructura de pruebas de dominio, aplicación, REST, persistencia y eventos propia del feature.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Crear el dominio, los puertos y la infraestructura base que necesitan todas las historias de usuario.

**⚠️ CRITICAL**: Ninguna user story puede comenzar hasta que esta fase esté completa.

- [ ] T007 Crear migraciones Flyway para las tablas:
  - `galpon_projection`: `galpon_id`, `nombre`, `aforo_maximo`, `estado`, `source_version`, `source_updated_at` y `synced_at`.
  - `lote_projection`: `lote_id`, `nombre`, `poblacion_inicial`, `poblacion_actual`, `fecha_ingreso`, `costo_total`, `galpon_id`, `source_version`, `source_updated_at` y `synced_at`.
  - `solicitud_descuento_poblacion`: `request_id`, `mortalidad_id`, `lote_id`, `galpon_id`, `cantidad_muertes`, `estado`, `motivo_rechazo`, `created_at` y `resolved_at`.
- [ ] T008 Crear modelos de consulta en `domain/model/consulta/`:
  - `GalponConsultado` con UUID, nombre, aforo máximo y estado.
  - `LoteConsultado` con UUID, nombre, población inicial, población actual, fecha de ingreso, costo total y UUID del galpón.
  - `EdadLote` como valor calculado, no como relación almacenada dentro del galpón.
  - `EstadoGalpon` como representación de los estados recibidos del Módulo 1.
- [ ] T009 Crear `EstadoGalpon` con los valores `DISPONIBLE`, `VACIADO_SANITARIO`, `PRODUCTIVO`, `EN_COSECHA`, `MANTENIMIENTO` y `AISLAMIENTO`.
- [ ] T010 Crear `SolicitudDescuentoPoblacion` y `EstadoSolicitudPoblacion` en `domain/model/poblacion/`.
- [ ] T011 Crear excepciones de dominio en `domain/exception/galpon/`: `GalponNoEncontradoException`, `LoteVigenteNoDisponibleException`, `FechaIngresoInconsistenteException` y `ProyeccionNoDisponibleException`.
- [ ] T012 Crear interfaces de puertos de salida en `domain/port/out/galpon/`:
  - `GalponQueryPort` para buscar y resumir galpones consultados.
  - `LoteQueryPort` para buscar lotes por `galponId` y obtener el lote vigente conforme a la regla funcional.
  - `SolicitudDescuentoPoblacionPort` para persistir y resolver solicitudes.
  - `Modulo1QueryPort` para recuperar información vigente sin acceder a tablas externas.
  - `IntegrationEventPublisherPort` para publicar solicitudes hacia el Módulo 1.
- [ ] T013 Crear entidades JPA y repositorios Spring Data para `galpon_projection`, `lote_projection` y `solicitud_descuento_poblacion`.
- [ ] T014 Implementar `GalponPersistenceMapper` y `GalponProjectionAdapter`, manteniendo independientes la proyección de galpón y la proyección de lote.
- [ ] T015 Implementar en `LoteJpaRepository` la consulta de lotes por `galponId`, ordenados por `fechaIngreso`, sin agregar una relación de colección dentro de `GalponProjectionEntity`.
- [ ] T016 Crear los contratos de eventos entrantes `GalponCreadoV1`, `GalponActualizadoV1`, `LoteRegistradoV1`, `PoblacionLoteActualizadaV1` y `DescuentoPoblacionRechazadoV1` en `adapter/in/event/galpon/dto/`.
- [ ] T017 Implementar `Modulo1GalponEventListener` y `SincronizarGalponYLoteUseCase` con idempotencia por `eventId` y control de orden por `sourceVersion`.
- [ ] T018 Implementar `Modulo1QueryAdapter` para recuperar galpones y lotes cuando una proyección requerida no exista o esté desactualizada.
- [ ] T019 Crear `ClockConfiguration`, `GalponBeanConfiguration`, autorización para `ROLE_ADMINISTRADOR` y `ROLE_USUARIO`, y manejo global de errores 400, 403, 404, 409 y 503.

**Checkpoint**: Gradle compila el proyecto, las migraciones crean las dos proyecciones independientes, los eventos del Módulo 1 pueden sincronizarlas y no existe ningún modelo, tabla o puerto de asignación entre trabajadores y galpones.

---

## Phase 3: User Story 1 — Consultar la Edad del Lote por Galpón (Priority: P1)

**Goal**: El administrador puede consultar la edad exacta del lote vigente registrado para un galpón, expresada en semanas completas, días restantes y días totales.

**Independent Test**: `GET /api/galpones/{galponId}/edad-lote` sobre un lote cuya fecha de ingreso fue hace 16 días retorna HTTP 200 y `2 semanas y 3 días (17 días)`. Si no existe un lote vigente para el galpón, informa esa condición sin fabricar una edad.

### Tests para User Story 1

- [ ] T020 [P] [US1] Test unitario: una consulta realizada el mismo día del ingreso calcula una edad de 1 día — `CalcularEdadLoteUseCaseTest.java`.
- [ ] T021 [P] [US1] Test unitario: 16 días transcurridos producen 17 días y el formato `2 semanas y 3 días (17 días)` — `CalcularEdadLoteUseCaseTest.java`.
- [ ] T022 [P] [US1] Test unitario: cambios de mes, año y 29 de febrero usan días calendario — `CalcularEdadLoteUseCaseTest.java`.
- [ ] T023 [P] [US1] Test de contrato: galpón sin lote vigente retorna el mensaje funcional definido en el spec — `GalponControllerTest.java`.
- [ ] T024 [P] [US1] Test de contrato: fecha de ingreso futura retorna HTTP 409 y un usuario sin rol administrador recibe HTTP 403 — `GalponControllerTest.java`.

### Implementación de User Story 1

- [ ] T025 [US1] Implementar `CalcularEdadLoteUseCase` con `Clock` y `ChronoUnit.DAYS`, incluyendo el día de ingreso como día 1.
- [ ] T026 [US1] Implementar `ConsultarEdadPorGalponUseCase` usando `LoteQueryPort` para localizar el lote vigente por `galponId`.
- [ ] T027 [US1] Crear `EdadLoteResponse` y su conversión en `GalponRestMapper`.
- [ ] T028 [US1] Implementar `GET /api/galpones/{galponId}/edad-lote` en `GalponController`, restringido a `ROLE_ADMINISTRADOR`.

**Checkpoint**: US1 funciona de manera independiente y calcula correctamente la edad sin almacenar lotes dentro del galpón.

---

## Phase 4: User Story 2 — Consultar la Información de un Galpón (Priority: P1)

**Goal**: El administrador o usuario autorizado puede consultar el nombre, aforo máximo y estado de un galpón, además de la población actual y edad del lote vigente registrado para él.

**Independent Test**: `GET /api/galpones/{galponId}` retorna HTTP 200 con los datos del galpón y, mediante una consulta independiente por `galponId`, los datos de su lote vigente. Si no existe lote, mantiene los datos del galpón e indica que la población y la edad no están disponibles.

### Tests para User Story 2

- [ ] T029 [P] [US2] Test unitario de `ConsultarGalponUseCase` para un galpón con un lote vigente relacionado por llave foránea — `ConsultarGalponUseCaseTest.java`.
- [ ] T030 [P] [US2] Test unitario para un galpón sin lote y para una fecha de ingreso inconsistente — `ConsultarGalponUseCaseTest.java`.
- [ ] T031 [P] [US2] Test de contrato: administrador y usuario reciben HTTP 200; un rol no autorizado recibe HTTP 403 — `GalponControllerTest.java`.
- [ ] T032 [P] [US2] Test de contrato: galpón inexistente retorna HTTP 404 y proyección no disponible retorna HTTP 503 — `GalponControllerTest.java`.
- [ ] T033 [P] [US2] Test de integración: consulta por separado `galpon_projection` y `lote_projection` y compone la respuesta — `GalponProjectionAdapterTest.java`.

### Implementación de User Story 2

- [ ] T034 [US2] Implementar `ConsultarGalponUseCase` usando `GalponQueryPort`, `LoteQueryPort` y `CalcularEdadLoteUseCase`.
- [ ] T035 [US2] Crear `GalponDetalleResponse` con nombre, aforo máximo, estado y, cuando exista, UUID, nombre, población actual, fecha de ingreso y edad del lote.
- [ ] T036 [US2] Agregar el mapeo de detalle a `GalponRestMapper` sin exponer entidades JPA.
- [ ] T037 [US2] Implementar `GET /api/galpones/{galponId}` en `GalponController` para `ROLE_ADMINISTRADOR` y `ROLE_USUARIO`.

**Checkpoint**: US1 y US2 consultan galpones y lotes como recursos independientes relacionados mediante `galponId`.

---

## Phase 5: User Story 3 — Consultar el Resumen General de Galpones (Priority: P2)

**Goal**: El administrador puede visualizar el total de galpones y su distribución entre los seis estados permitidos.

**Independent Test**: `GET /api/galpones/resumen` con doce galpones válidos retorna total 12 y conteos por estado cuya suma también es 12. Los registros sin estado válido se informan por separado.

### Tests para User Story 3

- [ ] T038 [P] [US3] Test unitario: cada galpón válido se contabiliza exactamente una vez — `ConsultarResumenGalponesUseCaseTest.java`.
- [ ] T039 [P] [US3] Test unitario: una colección vacía produce total y conteos en cero — `ConsultarResumenGalponesUseCaseTest.java`.
- [ ] T040 [P] [US3] Test unitario: un estado ausente o desconocido no se asigna a otro estado y aumenta el conteo de inconsistencias — `ConsultarResumenGalponesUseCaseTest.java`.
- [ ] T041 [P] [US3] Test de contrato: el administrador recibe HTTP 200 y otros roles reciben HTTP 403 — `GalponControllerTest.java`.
- [ ] T042 [P] [US3] Test de integración de la consulta agregada por estado en PostgreSQL — `GalponProjectionAdapterTest.java`.

### Implementación de User Story 3

- [ ] T043 [US3] Agregar a `GalponQueryPort` la consulta agregada de totales por estado.
- [ ] T044 [US3] Implementar `ConsultarResumenGalponesUseCase`, incluyendo conteos en cero para los seis estados y registros inconsistentes.
- [ ] T045 [US3] Crear `ResumenGalponesResponse` y su mapeo REST.
- [ ] T046 [US3] Implementar `GET /api/galpones/resumen` en `GalponController`, restringido a `ROLE_ADMINISTRADOR`.

**Checkpoint**: US3 presenta un resumen de solo lectura cuyos conteos son consistentes con las proyecciones de galpones.

---

## Phase 6: User Story 4 — Actualizar la Población Actual por Mortalidad (Priority: P1)

**Goal**: Una mortalidad confirmada genera una solicitud de descuento para que el Módulo 1 actualice la población del lote y el Módulo 2 refleje el resultado en su proyección sin modificar la población inicial.

**Independent Test**: Al confirmar 40 muertes sobre un lote con 8.000 aves, se publica una única solicitud y, tras recibir la confirmación del Módulo 1, la proyección muestra 7.960 aves. Una solicitud que produciría una población negativa es rechazada y la reentrega del mismo evento no descuenta dos veces.

### Tests para User Story 4

- [ ] T047 [P] [US4] Test unitario: cero muertes o una cantidad negativa se rechaza sin publicar una solicitud — `SolicitarDescuentoPoblacionUseCaseTest.java`.
- [ ] T048 [P] [US4] Test unitario: solicitudes repetidas con el mismo `mortalidadId` son idempotentes — `SolicitarDescuentoPoblacionUseCaseTest.java`.
- [ ] T049 [P] [US4] Test unitario: la confirmación actualiza únicamente la población actual de `LoteConsultado` y conserva la población inicial — `SolicitarDescuentoPoblacionUseCaseTest.java`.
- [ ] T050 [P] [US4] Test unitario: el rechazo conserva el motivo y no altera la proyección — `SolicitarDescuentoPoblacionUseCaseTest.java`.
- [ ] T051 [P] [US4] Test de integración: solicitud y publicación se registran de manera transaccional — `Modulo1GalponEventListenerTest.java`.
- [ ] T052 [P] [US4] Test con Kafka Testcontainers: duplicados no producen efectos repetidos y `loteId` conserva el orden de eventos — `Modulo1GalponEventListenerTest.java`.

### Implementación de User Story 4

- [ ] T053 [US4] Implementar `SolicitudDescuentoPoblacion`, sus transiciones `PENDIENTE`, `CONFIRMADA` y `RECHAZADA`, y las validaciones de cantidad positiva.
- [ ] T054 [US4] Implementar `SolicitudDescuentoPoblacionPort` y su persistencia JPA con unicidad por `mortalidadId`.
- [ ] T055 [US4] Implementar `SolicitarDescuentoPoblacionUseCase` para registrar la solicitud y publicar el evento en la misma transacción local.
- [ ] T056 [US4] Crear `SolicitudDescuentoPoblacionV1` en `adapter/out/event/galpon/dto/` y publicarlo mediante `GalponIntegrationEventPublisher` usando `loteId` como clave de partición.
- [ ] T057 [US4] Implementar `MortalidadConfirmadaEventListener` para activar el caso de uso desde el registro de mortalidad del Módulo 2.
- [ ] T058 [US4] Consumir `PoblacionLoteActualizadaV1`, confirmar la solicitud y actualizar `lote_projection` solo si `sourceVersion` es posterior.
- [ ] T059 [US4] Consumir `DescuentoPoblacionRechazadoV1`, marcar la solicitud como rechazada y conservar código y motivo.

**Checkpoint**: US4 mantiene un seguimiento auditable e idempotente; el Módulo 1 continúa siendo el único que modifica autoritativamente la población del lote.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Completar la documentación, calidad, seguridad y operación de todas las historias.

- [ ] T060 [P] Documentar los tres endpoints, respuestas y errores mediante OpenAPI.
- [ ] T061 [P] Documentar los topics, productores, consumidores, versiones y claves de partición acordados con el Módulo 1.
- [ ] T062 [P] Crear una prueba de arquitectura que impida dependencias de Spring, JPA y Kafka dentro de `domain/`.
- [ ] T063 Ejecutar todas las pruebas con `gradlew test` y corregir fallos de formato o análisis estático.
- [ ] T064 Ejecutar pruebas end-to-end con PostgreSQL y Kafka Testcontainers, incluyendo reinicios, duplicados y eventos fuera de orden.
- [ ] T065 Verificar el objetivo de respuesta de 1 segundo con el volumen de galpones y lotes acordado para el proyecto.
- [ ] T066 Verificar que no existan asignaciones de trabajadores, relaciones de colección de lotes dentro de galpones ni repositorios que modifiquen las entidades autoritativas del Módulo 1.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias; puede comenzar de inmediato.
- **Foundational (Phase 2)**: Depende de Phase 1 y bloquea todas las user stories.
- **US1 - Consultar edad (Phase 3)**: Depende de Phase 2; no depende de otra story.
- **US2 - Consultar galpón (Phase 4)**: Depende de Phase 2 y reutiliza el cálculo de edad de US1.
- **US3 - Resumen de galpones (Phase 5)**: Depende de Phase 2 y puede avanzar en paralelo con US1 y US2.
- **US4 - Actualizar población (Phase 6)**: Depende de Phase 2 y del evento de mortalidad confirmada; puede avanzar en paralelo con las consultas.
- **Polish (Phase 7)**: Depende de todas las user stories incluidas en la entrega.

### User Story Dependencies

- **US1 (P1)**: Inicia cuando termine Foundational.
- **US2 (P1)**: Reutiliza `CalcularEdadLoteUseCase` de US1; el resto del detalle puede desarrollarse en paralelo.
- **US3 (P2)**: Sin dependencias con otras stories después de Foundational.
- **US4 (P1)**: No depende de las stories de consulta; su activación productiva se integra con el registro de mortalidad.

### Dentro de cada User Story

- Puerto de salida antes que caso de uso.
- Caso de uso antes que controlador, listener o publicador.
- DTOs y mappers junto al adaptador que los utiliza.
- Tests escritos junto a la implementación de cada tarea.
- Checkpoint verificado antes de considerar completada la fase.

---

## Notes

- El tag `[P]` identifica tareas que pueden ejecutarse en paralelo porque no modifican los mismos archivos.
- Los tags `[US1]` a `[US4]` relacionan cada tarea con una user story para mantener trazabilidad.
- **Propiedad de datos**: `GalponConsultado` y `LoteConsultado` son modelos de lectura; las entidades autoritativas pertenecen al Módulo 1.
- **Relación galpón-lote**: `GalponConsultado` no contiene lotes. `LoteConsultado` conserva `galponId` y se consulta independientemente.
- **Sin asignaciones de trabajadores**: este plan no crea modelos, tablas, puertos, eventos, endpoints ni casos de uso de asignación entre trabajadores y galpones.
- **Eventos**: los contratos recibidos se ubican en `adapter/in/event`; `SolicitudDescuentoPoblacionV1`, producido por este módulo, se ubica en `adapter/out/event`.
- **Consistencia**: las consultas usan proyecciones locales; si una proyección imprescindible no existe o no puede verificarse, el sistema informa la indisponibilidad en lugar de inventar valores.
- **Población viva**: el Módulo 2 registra y sigue la solicitud, pero el Módulo 1 valida y modifica el lote para conservar la propiedad del dato.
- **Trazabilidad documental**: la historia de galpones asignados al trabajador todavía aparece en el SPEC-007, pero se excluye de este plan porque contradice el modelo confirmado del Módulo 1 y la decisión de que no existen tales asignaciones.
