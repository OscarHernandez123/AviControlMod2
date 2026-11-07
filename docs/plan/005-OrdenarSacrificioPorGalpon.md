# Implementation Plan: Ordenar sacrificio por galpón

**Date**: 05/11/2026
**Spec**: [005-OrdenarSacrificioPorGalpon.md](../specs/005-OrdenarSacrificioPorGalpon.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Permitir que un administrador programe, reprograme o cancele una orden de sacrificio productivo para el lote actual de un galpón en `en cosecha`. Al llegar la fecha se ejecuta una sola vez: la orden pasa a ejecutada, el galpón pasa a `vaciado sanitario` y finaliza el alojamiento del lote. También se expone un resumen de total y órdenes pendientes, sin efectos de escritura.

## Technical Context

**Performance Goals**: confirmar cada gestión en un segundo y ejecutar al menos el 99 % de las órdenes dentro del primer minuto posterior a su fecha.
**Constraints**: sólo `ROLE_ADMINISTRADOR`; fecha y hora futuras; las validaciones de galpón y lote se realizan de nuevo contra el Módulo 1.
**Scale/Scope**: una orden por galpón/lote activo; ejecución recuperable tras una indisponibilidad y resumen consistente de sólo lectura.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 005-OrdenarSacrificioPorGalpon.md
└── plan/
    └── 005-OrdenarSacrificioPorGalpon.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/sacrificio/productivo/
│   │   ├── EstadoOrdenSacrificio.java
│   │   └── OrdenSacrificioProductivo.java
│   ├── exception/sacrificio/productivo/
│   │   ├── FechaProgramadaInvalidaException.java
│   │   ├── OrdenNoModificableException.java
│   │   └── PrecondicionSacrificioNoCumplidaException.java
│   └── port/out/
│       ├── modulo1/
│       │   ├── GalponQueryPort.java
│       │   ├── GalponUpdatePort.java
│       │   ├── LoteQueryPort.java
│       │   └── LoteUpdatePort.java
│       └── sacrificio/productivo/OrdenSacrificioProductivoRepositoryPort.java
├── application/
│   └── sacrificio/productivo/
│       ├── CancelarOrdenSacrificioUseCase.java
│       ├── ConsultarResumenOrdenesSacrificioUseCase.java
│       ├── EjecutarOrdenSacrificioUseCase.java
│       ├── EjecutarOrdenesSacrificioVencidasUseCase.java
│       ├── ProgramarOrdenSacrificioUseCase.java
│       ├── ReprogramarOrdenSacrificioUseCase.java
│       └── result/
│           ├── OrdenSacrificioResult.java
│           └── ResumenOrdenesSacrificioResult.java
└── infrastructure/
    ├── adapter/in/rest/sacrificio/productivo/
    │   ├── SacrificioProductivoController.java
    │   ├── dto/
    │   │   ├── OrdenSacrificioResponse.java
    │   │   ├── ProgramarOrdenSacrificioRequest.java
    │   │   ├── ReprogramarOrdenSacrificioRequest.java
    │   │   └── ResumenOrdenesSacrificioResponse.java
    │   └── mapper/SacrificioProductivoRestMapper.java
    ├── adapter/out/integration/modulo1/
    │   ├── GalponUpdateAdapter.java
    │   ├── GalponUpdateNoDisponibleAdapter.java
    │   ├── LoteUpdateAdapter.java
    │   └── LoteUpdateNoDisponibleAdapter.java
    ├── adapter/out/persistence/sacrificio/productivo/OrdenSacrificioProductivoPersistenceAdapter.java
    ├── scheduler/sacrificio/productivo/EjecutorOrdenesSacrificioScheduler.java
    └── config/SacrificioProductivoBeanConfiguration.java

src/main/resources/db/migration/
└── V17__crear_ordenes_sacrificio_productivo.sql

src/test/java/com/avicontrol/
├── domain/model/sacrificio/productivo/OrdenSacrificioProductivoTest.java
├── application/sacrificio/productivo/
│   ├── GestionarOrdenSacrificioUseCaseTest.java
│   ├── EjecutarOrdenesSacrificioVencidasUseCaseTest.java
│   ├── EjecucionSacrificioRollbackTest.java
│   └── ConsultarResumenOrdenesSacrificioUseCaseTest.java
└── infrastructure/adapter/in/rest/sacrificio/productivo/SacrificioProductivoControllerTest.java
```

**Structure Decision**: la orden y sus transiciones son dominio propio y se persisten localmente. Los galpones y lotes permanecen en el Módulo 1; los puertos de integración permiten verificar y actualizar ese módulo sin acoplar el dominio a HTTP o JPA.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: definir los contratos externos, el esquema y el mecanismo de ejecución diferida.

- [x] T001 Definir puertos de consulta y actualización sobre la fuente provisional mientras se conecta el Módulo 1 real.
- [x] T002 Crear la migración de órdenes con identificadores de galpón y lote, fecha programada, estado y marcas de creación, reprogramación, cancelación y ejecución.
- [x] T003 Configurar un `Clock` y el scheduler para procesar órdenes vencidas después de una caída.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: establecer ciclo de vida, persistencia transaccional y puertos antes de las historias.

**⚠️ CRITICAL**: ninguna operación HTTP ni tarea programada debe iniciar sin estas reglas.

- [x] T004 Crear `OrdenSacrificioProductivo` y `EstadoOrdenSacrificio` con transiciones programada, reprogramada, cancelada y ejecutada.
- [x] T005 Validar que programar y reprogramar siempre reciban una fecha futura, y que cancelar o reprogramar sólo sea posible antes de la fecha programada.
- [x] T006 Definir el repositorio con bloqueo o actualización condicional para que sólo una operación gane al mismo tiempo.
- [x] T007 Implementar puertos y adaptadores del Módulo 1; rechazar datos ausentes, lote distinto o galpón fuera de `en cosecha`.
- [x] T008 Implementar una consulta de resumen que calcule total y pendientes desde la misma vista consistente.

**Checkpoint**: el ciclo de vida no permite doble ejecución ni modificaciones de órdenes ya ejecutadas.

---

## Phase 3: User Story 1 - Gestionar una orden de sacrificio (Priority: P1)

**Goal**: programar, reprogramar o cancelar una orden válida, y ejecutarla una vez cuando venza.

**Independent Test**: con galpón en `en cosecha` y el mismo lote activo se crea una orden futura; antes de vencer puede reprogramarse o cancelarse. Al vencer, la orden se ejecuta una vez, el galpón pasa a `vaciado sanitario` y el alojamiento termina.

### Tests for User Story 1

- [x] T009 [P] [US1] Probar transiciones válidas e inválidas de `OrdenSacrificioProductivo`.
- [x] T010 [P] [US1] Probar programación, reprogramación, cancelación y ejecución con repositorio y puertos simulados.
- [x] T011 [P] [US1] Probar idempotencia de la ejecución y la carrera entre ejecución, cancelación y reprogramación.
- [x] T012 [P] [US1] Crear pruebas MockMvc de los tres endpoints y la autorización.

### Implementation for User Story 1

- [x] T013 [US1] Implementar `ProgramarOrdenSacrificioUseCase` y verificar nuevamente galpón, lote y estado `en cosecha`.
- [x] T014 [US1] Implementar `ReprogramarOrdenSacrificioUseCase` y `CancelarOrdenSacrificioUseCase`.
- [x] T015 [US1] Implementar `EjecutarOrdenesSacrificioVencidasUseCase` y el scheduler; procesar cada orden de manera idempotente.
- [x] T016 [US1] En ejecución, marcar la orden, actualizar el galpón y finalizar el alojamiento en una operación consistente con compensación o reintento documentado.
- [x] T017 [US1] Exponer los endpoints y mapear solicitudes y respuestas REST.

## Endpoints

### Gestionar órdenes

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `POST /api/galpones/{galponId}/ordenes-sacrificio` | `ROLE_ADMINISTRADOR` | `ProgramarOrdenSacrificioRequest` | `201 Created` con orden programada |
| `PUT /api/ordenes-sacrificio/{ordenId}/reprogramacion` | `ROLE_ADMINISTRADOR` | `ReprogramarOrdenSacrificioRequest` | `200 OK` con orden reprogramada |
| `POST /api/ordenes-sacrificio/{ordenId}/cancelacion` | `ROLE_ADMINISTRADOR` | Sin cuerpo | `200 OK` con orden cancelada |

### Consultar resumen

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `GET /api/ordenes-sacrificio/resumen` | `ROLE_ADMINISTRADOR` | Sin cuerpo | `200 OK` con total y pendientes |

**Errores**: `400` fecha u UUID inválidos; `401` no autenticado; `403` sin rol; `404` orden o galpón ausente; `409` orden no modificable o precondición incumplida; `503` Módulo 1 no disponible.

### Ejemplos de operación

```http
POST /api/galpones/{galponId}/ordenes-sacrificio
PUT /api/ordenes-sacrificio/{ordenId}/reprogramacion
POST /api/ordenes-sacrificio/{ordenId}/cancelacion
Authorization: Bearer <token>
```

### Request: programar o reprogramar

```json
{
  "fechaHoraProgramada": "2026-12-01T08:00:00-05:00"
}
```

### Response: orden programada

```json
{
  "id": "b07f5fd4-3b77-4e9a-a4eb-5d54f907c8a4",
  "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "loteId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "estado": "PROGRAMADA",
  "fechaHoraProgramada": "2026-12-01T08:00:00-05:00"
}
```

**Checkpoint**: la orden puede gestionarse de forma independiente y una ejecución tardía se recupera sin duplicar cambios.

---

## Phase 4: User Story 2 - Consultar resumen de órdenes (Priority: P2)

**Goal**: mostrar total de órdenes y cantidad pendiente de ejecución sin modificar ningún recurso.

**Independent Test**: con seis órdenes, dos programadas o reprogramadas y no vencidas, la respuesta devuelve total seis y pendientes dos; sin órdenes devuelve cero y cero.

### Tests for User Story 2

- [x] T018 [P] [US2] Probar la consulta del resumen con órdenes programadas, reprogramadas, canceladas y ejecutadas.
- [x] T019 [P] [US2] Probar con MockMvc el contrato y que la consulta no invoque operaciones de escritura.

### Implementation for User Story 2

- [x] T020 [US2] Implementar `ConsultarResumenOrdenesSacrificioUseCase` con una consulta consistente del repositorio.
- [x] T021 [US2] Exponer `GET /api/ordenes-sacrificio/resumen` para `ROLE_ADMINISTRADOR`.
- [x] T022 [US2] Crear el DTO, mapper y respuesta del resumen.

### Ejemplo de consulta

```http
GET /api/ordenes-sacrificio/resumen
Authorization: Bearer <token>
```

```json
{
  "totalOrdenes": 6,
  "ordenesPendientes": 2
}
```

**Checkpoint**: el resumen es independiente de la gestión y nunca altera el ciclo de vida de una orden.

---

## Polish & Cross-Cutting Concerns

**Purpose**: completar seguridad, observabilidad y contrato.

- [x] T023 Traducir UUID o fecha inválidos a `400`, ausencia a `404`, transición o precondición inválida a `409` e indisponibilidad del Módulo 1 a `503`.
- [x] T024 Proteger todos los endpoints con `ROLE_ADMINISTRADOR` y probar `401` y `403`.
- [x] T025 Registrar eventos de ejecución, reintentos y fallos de integración sin exponer datos técnicos al cliente.
- [x] T026 Documentar endpoints y respuestas en OpenAPI, estados en este plan; ejecutar pruebas y `git diff --check`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: depende del esquema y contratos, y bloquea ambas historias.
- **User Story 1 (Phase 3)**: depende del ciclo de vida y puertos.
- **User Story 2 (Phase 4)**: depende del repositorio, pero es de sólo lectura e independiente de la ejecución.
- **Polish**: depende de las historias deseadas.

### Within Each User Story

1. Reglas y pruebas de dominio.
2. Caso de uso y persistencia.
3. Integración con el Módulo 1 o consulta consistente.
4. Endpoint, DTO y mapper.
5. Pruebas de contrato y autorización.

## Notes

- Una orden pendiente es únicamente una orden programada o reprogramada, no cancelada ni ejecutada.
- La ejecución debe ser idempotente para tolerar reintentos y recuperación posterior a una caída.
- Cancelar no modifica el galpón ni el alojamiento; reprogramar mantiene el galpón en `en cosecha`.
- Reprogramar o cancelar sólo se permite antes de `fechaHoraProgramada`, incluso si el scheduler todavía no ha ejecutado la orden vencida.
- La ejecución local bloquea la orden, cambia galpón, finaliza alojamiento y marca la orden dentro de una transacción; un fallo revierte todo y el scheduler reintenta al iniciar y cada 30 segundos. El adaptador remoto del Módulo 1 requerirá coordinación idempotente antes de activarse; mientras no exista, otros perfiles responden `503`.
- La unicidad `(galpon_id, lote_id)` permite una sola orden histórica por alojamiento, incluso si se cancela. Una nueva orden requiere un nuevo lote; la reprogramación se hace antes de cancelar.
