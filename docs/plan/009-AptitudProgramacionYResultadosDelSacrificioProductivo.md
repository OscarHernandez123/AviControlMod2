# Implementation Plan: Aptitud y Programación del Sacrificio Productivo

**Date**: 25/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [004-ValidarAptoParaSacrifico.md](../specs/004-ValidarAptoParaSacrifico.md)
- [005-OrdenarSacrificioPorGalpon.md](../specs/005-OrdenarSacrificioPorGalpon.md)

## Summary

Evaluar la aptitud zootécnica, programar/reprogramar/cancelar órdenes de sacrificio comercial, ejecutar la orden al vencer y consultar el resumen administrativo. La ejecución transiciona el galpón a `VACIADO_SANITARIO` y finaliza el alojamiento del lote. El pesaje final y los resultados que consume Módulo 3 se especifican por separado en Plan 016.

## Technical Context

**Performance Goals**: El 95 % de validaciones, programación, reprogramación, cancelación y consulta responde en máximo 1 segundo; el 99 % de órdenes vencidas se ejecuta en máximo 1 minuto.

**Constraints**: Solo `ROLE_ADMINISTRADOR`; aptitud exige galpón productivo y edad del lote mayor o igual a 45 días; programación en fecha futura; cambios solo antes de la ejecución; transición y finalización de alojamiento coordinadas con Módulo 1.

**Scale/Scope**: SPEC-004 y SPEC-005; cinco historias, cinco endpoints REST y una tarea programada de ejecución.

**Dependencias funcionales**: Módulo 1/Plan 001 para galpón, lote, edad, estados y alojamiento; General.md para seguridad, actor, reloj, transacciones, eventos y errores; Plan 016 consume el resultado de la orden ejecutada para registrar pesaje final.

### Decisiones específicas

1. La aptitud se calcula con edad de días calendario completos según la regla del Plan 001 y exige estado productivo.
2. Solo la aptitud `APTO` cambia el galpón a `EN_COSECHA`.
3. La orden es una entidad con fecha programada y estados `PROGRAMADA`, `REPROGRAMADA`, `EJECUTADA` o `CANCELADA`.
4. Cancelar no revierte automáticamente `EN_COSECHA`; permite al administrador reprogramar bajo reglas de SPEC-005.
5. Scheduler y ejecución manual coordinan el mismo caso de uso idempotente; estado de orden, transición de galpón y cierre del alojamiento se confirman de forma atómica.
6. El resumen administrativo es de solo lectura y no calcula costos.
7. El resultado de pesaje pertenece a Plan 016; este plan solo publica que la orden se ejecutó.

### Alcance documental

- Este plan implementa SPEC-004 y SPEC-005.
- Plan 016 implementa SPEC-006 para registrar/corregir resultados de pollos finales tras una orden ejecutada.
- Sacrificio sanitario es responsabilidad del Plan 008.
- Cálculos económicos y liquidación pertenecen a Módulo 3.

## Project Structure

### Documentation (this feature)

```text
docs/specs/004-ValidarAptoParaSacrifico.md
docs/specs/005-OrdenarSacrificioPorGalpon.md
docs/plan/009-AptitudProgramacionYResultadosDelSacrificioProductivo.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/model/sacrificio/productivo/
│   ├── ValidacionAptitudSacrificio.java
│   ├── OrdenSacrificioProductivo.java
│   └── EstadoOrdenProductiva.java
├── domain/port/out/sacrificio/productivo/
│   ├── OrdenSacrificioProductivoRepositoryPort.java
│   ├── GalponQueryPort.java
│   ├── LoteQueryPort.java
│   ├── CambiarEstadoOperativoGalponPort.java
│   └── FinalizarAlojamientoLotePort.java
├── application/sacrificio/productivo/
│   ├── ValidarAptitudSacrificioUseCase.java
│   ├── ProgramarSacrificioProductivoUseCase.java
│   ├── ReprogramarSacrificioProductivoUseCase.java
│   ├── CancelarSacrificioProductivoUseCase.java
│   ├── EjecutarSacrificioProductivoUseCase.java
│   └── ConsultarResumenOrdenesSacrificioUseCase.java
└── infrastructure/
    ├── adapter/in/rest/sacrificio/productivo/SacrificioProductivoController.java
    ├── adapter/in/scheduler/sacrificio/productivo/EjecucionSacrificioProductivoScheduler.java
    ├── adapter/out/persistence/sacrificio/productivo/
    └── config/SacrificioProductivoBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/sacrificio/productivo/
├── application/sacrificio/productivo/
└── integration/sacrificio/productivo/SacrificioProductivoIntegrationTest.java
```

**Structure Decision**: El agregado de orden coordina reglas de programación; el scheduler solo dispara el caso de uso. Módulo 1 sigue siendo propietario de estados de galpón y alojamiento.

### Entidades y relación

```text
ValidacionAptitudSacrificio: esApto, edadEnDias, estadoPrevio, motivos, evaluadaEn
OrdenSacrificioProductivo: id, galponId, loteId, fechaProgramada, fechaEjecucion,
  estado, programadaPor, motivoCancelacion, version
```

Una orden ejecutada habilita que Plan 016 registre un resultado único asociado a la misma orden.

### Contratos de los puertos

| Puerto | Responsabilidad |
| --- | --- |
| `OrdenSacrificioProductivoRepositoryPort` | Guardar y consultar órdenes; listar órdenes vencidas y resumir estados. |
| `GalponQueryPort` / `LoteQueryPort` | Validar estado productivo, lote activo, fecha de ingreso y edad. |
| `CambiarEstadoOperativoGalponPort` | Transicionar a `EN_COSECHA` y luego `VACIADO_SANITARIO`. |
| `FinalizarAlojamientoLotePort` | Finalizar alojamiento al ejecutar la orden. |
| `IntegrationEventPublisherPort` | Publicar orden ejecutada para consumidores acordados. |

### Contratos HTTP propuestos

| Método y ruta | Acceso | Responsabilidad |
| --- | --- | --- |
| `POST /api/galpones/{galponId}/validacion-aptitud-sacrificio` | `ROLE_ADMINISTRADOR` | Evaluar aptitud y transicionar si es apto. |
| `POST /api/galpones/{galponId}/ordenes-sacrificio` | `ROLE_ADMINISTRADOR` | Programar fecha futura. |
| `PUT /api/ordenes-sacrificio/{ordenId}/reprogramacion` | `ROLE_ADMINISTRADOR` | Cambiar fecha antes de ejecución. |
| `POST /api/ordenes-sacrificio/{ordenId}/cancelacion` | `ROLE_ADMINISTRADOR` | Cancelar antes de ejecución. |
| `GET /api/ordenes-sacrificio/resumen` | `ROLE_ADMINISTRADOR` | Consultar totales y pendientes. |

Errores usan RFC 9457 y General.md.

### JSON común de errores

```json
{
  "type": "https://avicontrol/errors/orden-no-ejecutable",
  "title": "Orden no ejecutable",
  "status": 409,
  "detail": "La orden no se encuentra vencida o está en un estado terminal",
  "instance": "/api/ordenes-sacrificio/{ordenId}/ejecucion",
  "code": "ORDEN_NO_EJECUTABLE",
  "correlationId": "uuid",
  "fieldErrors": []
}
```
## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Preparar configuraciones de seguridad, contratos con Módulo 1 y Módulo 3, y fixtures del feature.

- [ ] T001 Revisar contratos de `CambiarEstadoOperativoGalponPort` y `FinalizarAlojamientoLotePort` con el Módulo 1 para soportar `EN_COSECHA`, `VACIADO_SANITARIO` y desvinculación del lote.
- [ ] T002 Acordar transiciones y eventos de ejecución de la orden con el Módulo 1 y el scheduler, incluida la idempotencia ante reintentos.
- [ ] T003 Configurar en Spring Security la autorización exclusiva de `ROLE_ADMINISTRADOR` para todos los endpoints de sacrificio productivo.
- [ ] T004 Crear fixtures de prueba: lote con 44 días (no apto), lote con 45 días (apto), galpón en producción, galpón en cosecha, órdenes programadas y estados disponibles.

**Checkpoint**: Entorno listo y contratos de integración definidos.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Construir entidades, objetos de valor, migraciones Flyway y adaptadores de persistencia base.

- [ ] T005 Implementar en Java puro `ValidacionAptitudSacrificio`, `OrdenSacrificioProductivo` y el enum `EstadoOrdenProductiva`.
- [ ] T006 Implementar validaciones de invariantes de negocio: fecha futura y estados válidos de la orden.
- [ ] T007 Definir las excepciones de negocio del paquete `com.avicontrol.domain.exception.sacrificio.productivo`.
- [ ] T008 Definir las interfaces de salida `OrdenSacrificioProductivoRepositoryPort`.
- [ ] T009 Crear la migración Flyway para órdenes de sacrificio con índices y control de concurrencia (`version`); usar la siguiente versión Flyway disponible.
- [ ] T010 Implementar entidad JPA, repositorio Spring Data y mapper de persistencia para órdenes.
- [ ] T011 Configurar beans en `SacrificioProductivoBeanConfiguration` y habilitar `@EnableScheduling`.

**Checkpoint**: Base de persistencia y dominio completada.

---

## Phase 3: User Story 1 — Validar Aptitud de un Galpón para Sacrificio (Priority: P1)

**Spec**: [004-ValidarAptoParaSacrifico.md](../specs/004-ValidarAptoParaSacrifico.md), historia 1.

**Goal**: El administrador valida si un lote cumple la edad mínima ($\ge 45$ días) y el galpón está en estado `PRODUCTIVO`; si es apto, el galpón cambia a `EN_COSECHA`. Si no, muestra las razones y no altera el estado.

**Independent Test**: Evaluar galpón en estado `PRODUCTIVO` con lote de 45 días; verificar resultado `Apto` y cambio del galpón a `EN_COSECHA`. Evaluar con lote de 44 días; verificar `No apto` con motivo "El lote no cumple la edad mínima de 45 días" y galpón intacto.

### Definición del endpoint REST para User Story 1

| Elemento | Definición |
| --- | --- |
| Método y ruta | `POST /api/galpones/{galponId}/validacion-aptitud-sacrificio` |
| Autorización | `ROLE_ADMINISTRADOR` |
| Entrada (Path) | `galponId` (UUID obligatorio, no recibe body) |
| Respuesta 200 | `ValidarAptitudResponse` con `resultado: "APTO"` o `"NO_APTO"`, edad en días, estado previo, nuevo estado y lista de motivos |
| Errores | 401 sin autenticación; 403 sin rol administrador; 404 si el galpón o lote no existen; 503 si Módulo 1 no responde |

#### JSON de respuesta (Apto)

```json
{
  "galponId": "550e8400-e29b-41d4-a716-446655440001",
  "loteId": "2fc03a21-84c4-4c83-bb15-6607bb75cb90",
  "resultado": "APTO",
  "edadEnDias": 46,
  "estadoPrevioGalpon": "PRODUCTIVO",
  "nuevoEstadoGalpon": "EN_COSECHA",
  "motivos": []
}
```

### Tests para User Story 1

- [ ] T012 [P] [US1] Unit test en `ValidarAptitudSacrificioUseCaseTest`: lote de 44 días (no apto), lote de 45 días (apto), galpón no productivo (no apto), y fallo simultáneo de ambas condiciones.
- [ ] T013 [P] [US1] Contract test en `SacrificioProductivoControllerTest`: códigos HTTP 200, 403 y 404.

### Implementación para User Story 1

- [ ] T014 [P] [US1] Implementar DTOs `ValidarAptitudResponse` y mapeo en `SacrificioProductivoRestMapper`.
- [ ] T015 [P] [US1] Implementar `ValidarAptitudSacrificioUseCase` consultando `LoteQueryPort` y `GalponQueryPort`, e invocando `CambiarEstadoOperativoGalponPort` ante dictamen apto.
- [ ] T016 [US1] Implementar endpoint `POST /api/galpones/{galponId}/validacion-aptitud-sacrificio`.

**Checkpoint**: Validación zootécnica operativa con transición controlada a cosecha.

---

## Phase 4: User Story 2 — Programar el Sacrificio de un Lote por Galpón (Priority: P1)

**Spec**: [005-OrdenarSacrificioPorGalpon.md](../specs/005-OrdenarSacrificioPorGalpon.md), historia 1.

**Goal**: El administrador programa la fecha y hora futura del sacrificio comercial para un galpón en estado `EN_COSECHA`, creando la orden en estado `PROGRAMADA`.

**Independent Test**: Con galpón en `EN_COSECHA`, programar para una fecha/hora 2 días adelante; verificar que la orden se cree en estado `PROGRAMADA` y que el galpón se conserve en `EN_COSECHA`. Intentar con fecha pasada y verificar el rechazo con `400 Bad Request`.

### Definición del endpoint REST para User Story 2

| Elemento | Definición |
| --- | --- |
| Método y ruta | `POST /api/galpones/{galponId}/ordenes-sacrificio` |
| Autorización | `ROLE_ADMINISTRADOR` |
| Entrada (Body) | `fechaHoraProgramada` (ISO-8601 estrictamente futura) |
| Respuesta 201 | `OrdenSacrificioResponse` con estado `PROGRAMADA` |
| Errores | 400 por fecha no futura; 409 si el galpón no está en `EN_COSECHA` |

#### JSON de solicitud (Request)

```json
{
  "fechaHoraProgramada": "2026-10-08T06:00:00"
}
```

### Tests para User Story 2

- [ ] T017 [P] [US2] Unit test en `ProgramarSacrificioProductivoUseCaseTest`: verificación de estado `EN_COSECHA`, fecha futura y rechazo ante galpón en otro estado.
- [ ] T018 [P] [US2] Contract test en `SacrificioProductivoControllerTest`.

### Implementación para User Story 2

- [ ] T019 [P] [US2] Crear DTOs `ProgramarSacrificioRequest`, `OrdenSacrificioResponse` y mappers.
- [ ] T020 [P] [US2] Implementar `ProgramarSacrificioProductivoUseCase` persistiendo en `OrdenSacrificioProductivoRepositoryPort`.
- [ ] T021 [US2] Implementar endpoint `POST /api/galpones/{galponId}/ordenes-sacrificio`.

**Checkpoint**: Programación de órdenes comerciales completada.

---

## Phase 5: User Story 3 — Reprogramar y Cancelar Órdenes de Sacrificio (Priority: P2)

**Spec**: [005-OrdenarSacrificioPorGalpon.md](../specs/005-OrdenarSacrificioPorGalpon.md), historia 1 (escenarios 5 y 6).

**Goal**: El administrador reprograma la fecha/hora futura o cancela una orden antes de que se cumpla el horario establecido.

**Independent Test**: Reprogramar una orden pendiente fijando nueva fecha futura válida. Cancelar otra orden pendiente y verificar que pase a estado `CANCELADA` conservando el galpón en `EN_COSECHA`. Intentar reprogramar una orden vencida o cancelada y comprobar el rechazo con `409 Conflict`.

### Definición de endpoints REST para User Story 3

- `PUT /api/ordenes-sacrificio/{ordenId}/reprogramacion` (Body: `nuevaFechaHoraProgramada`)
- `POST /api/ordenes-sacrificio/{ordenId}/cancelacion` (Body: `motivoCancelacion`)

### Tests para User Story 3

- [ ] T022 [P] [US3] Unit tests en `ReprogramarSacrificioProductivoUseCaseTest` y `CancelarSacrificioProductivoUseCaseTest`.
- [ ] T023 [P] [US3] Contract tests en `SacrificioProductivoControllerTest`.

### Implementación para User Story 3

- [ ] T024 [P] [US3] Implementar casos de uso `ReprogramarSacrificioProductivoUseCase` y `CancelarSacrificioProductivoUseCase`.
- [ ] T025 [US3] Implementar endpoints de reprogramación y cancelación en `SacrificioProductivoController`.

**Checkpoint**: Flexibilidad operativa en la gestión previa al sacrificio.

---

## Phase 6: User Story 4 — Ejecución Programada del Sacrificio y Vaciado Sanitario (Priority: P1)

**Spec**: [005-OrdenarSacrificioPorGalpon.md](../specs/005-OrdenarSacrificioPorGalpon.md), historia 1 (escenario 2).

**Goal**: Al llegar la fecha y hora programadas, el sistema marca la orden como ejecutada, cambia el estado del galpón a `VACIADO_SANITARIO` y finaliza el alojamiento del lote en Módulo 1.

**Independent Test**: Programar una orden cuya fecha venza; verificar que el scheduler la pase a `EJECUTADA`, cambie el galpón a `VACIADO_SANITARIO` e invoque `FinalizarAlojamientoLotePort`, también después de una interrupción del servicio.

### Tests para User Story 4

- [ ] T026 [P] [US4] Unit test en `EjecutarSacrificioProductivoUseCaseTest`: transición atómica de orden, galpón y lote.
- [ ] T027 [US4] Integration test con scheduler comprobando ejecución desatendida de órdenes cuya fecha ya venció.

### Implementación para User Story 4

- [ ] T028 [P] [US4] Implementar `EjecutarSacrificioProductivoUseCase` orquestando `CambiarEstadoOperativoGalponPort` y `FinalizarAlojamientoLotePort`.
- [ ] T029 [US4] Implementar `EjecucionSacrificioProductivoScheduler` con ejecución periódica cada minuto.
- [ ] T030 [US4] Asegurar que el scheduler recupere y procese idempotentemente las órdenes vencidas después de una interrupción.

**Checkpoint**: Conclusión física y operativa del lote con vaciado sanitario.

---

## Phase 7: User Story 5 — Consultar Resumen de Órdenes de Sacrificio en Pantalla de Inicio (Priority: P2)

**Spec**: [005-OrdenarSacrificioPorGalpon.md](../specs/005-OrdenarSacrificioPorGalpon.md), historia 2.

**Goal**: El administrador consulta en la pantalla de inicio el total de órdenes de sacrificio registradas y cuántas están pendientes de ejecución (`PROGRAMADA`, `REPROGRAMADA`).

**Independent Test**: Registrar 6 órdenes (2 programadas, 3 ejecutadas, 1 cancelada); verificar que `GET /api/ordenes-sacrificio/resumen` devuelva `totalOrdenes: 6` y `ordenesPendientes: 2`.

### Definición del endpoint REST para User Story 5

| Elemento | Definición |
| --- | --- |
| Método y ruta | `GET /api/ordenes-sacrificio/resumen` |
| Autorización | `ROLE_ADMINISTRADOR` |
| Respuesta 200 | `ResumenOrdenesResponse` (`totalOrdenes`, `ordenesPendientes`) |

#### JSON de respuesta

```json
{
  "totalOrdenes": 6,
  "ordenesPendientes": 2
}
```

### Tests para User Story 5

- [ ] T031 [P] [US5] Unit test en `ConsultarResumenOrdenesSacrificioUseCaseTest`.
- [ ] T032 [P] [US5] Contract test en `SacrificioProductivoControllerTest`.

### Implementación para User Story 5

- [ ] T033 [P] [US5] Implementar `ConsultarResumenOrdenesSacrificioUseCase` consultando `OrdenSacrificioProductivoRepositoryPort`.
- [ ] T034 [US5] Implementar endpoint `GET /api/ordenes-sacrificio/resumen`.

**Checkpoint**: Indicadores de inicio operativos y de solo lectura.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [ ] T035 Documentar los cinco endpoints y errores RFC 9457 en OpenAPI.
- [ ] T036 Completar prueba de integración aptitud → programación → ejecución.
- [ ] T037 Verificar atomicidad entre orden, estado del galpón y finalización de alojamiento.
- [ ] T038 Verificar permisos, scheduler idempotente y límites de arquitectura.
- [ ] T039 Medir objetivos de respuesta y ejecución vencida.

## Dependencies & Execution Order

### Phase Dependencies

- Setup precede a Foundational.
- Aptitud habilita programación.
- Reprogramación/cancelación y resumen dependen de órdenes persistidas.
- Ejecución depende de orden vencida y confirma transición del galpón/cierre de alojamiento.
- Plan 016 registra resultados después de la ejecución confirmada.

### Dependencias con otros planes

- **Módulo 1 / Plan 001**: Provee galpón/lote y aplica transiciones y finalización de alojamiento.
- **Plan 008**: Mantiene separado el sacrificio sanitario.
- **Plan 016**: Registra resultados finales después de una orden ejecutada.
- **Módulo 3**: Consume los resultados que publica Plan 016.
- **General.md**: Seguridad, reloj, eventos, errores y transacciones.

### Dentro de cada User Story

- Validar estado vigente y actor antes de cambiar orden o galpón.
- Usar un caso de uso idempotente para la ejecución programada y recuperación de órdenes vencidas.
- Confirmar cambios de orden, galpón y alojamiento de forma atómica.

## Notes

- Este plan implementa SPEC-004 y SPEC-005; SPEC-006 está en Plan 016.
- La ejecución del sacrificio no registra ni corrige pesos finales.
- Todos los endpoints exigen `ROLE_ADMINISTRADOR`.
