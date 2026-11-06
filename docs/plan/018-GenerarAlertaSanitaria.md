# Implementation Plan: Generar alerta sanitaria

**Date**: 05/11/2026
**Spec**: [018-GenerarAlertaSanitaria.md](../specs/018-GenerarAlertaSanitaria.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Generar automáticamente una alerta sanitaria inmutable para cada mortalidad anormal confirmada y ofrecer su consulta contextual al equipo veterinario. La generación sólo ocurre a través del puerto de aplicación usado por el Plan 017, nunca mediante un endpoint manual.

## Technical Context

**Performance Goals**: generar alertas en menos de dos segundos.
**Constraints**: una alerta por reporte anormal; estado inicial pendiente; prioridad derivada de severidad y porcentaje; consulta sólo para veterinario o administrador.
**Scale/Scope**: generar, listar y detallar alertas; no cerrarlas, diagnosticarlas ni cambiar estados de galpón.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 018-GenerarAlertaSanitaria.md
└── plan/
    └── 018-GenerarAlertaSanitaria.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/alerta/
│   │   ├── AlertaSanitaria.java
│   │   ├── EstadoAlertaSanitaria.java
│   │   └── PrioridadAlerta.java
│   ├── exception/alerta/RegistroMortalidadAnormalInvalidoException.java
│   └── port/out/alerta/
│       ├── AlertaSanitariaRepositoryPort.java
│       ├── ContextoAlertaSanitariaQueryPort.java
│       └── MortalidadAnormalQueryPort.java
├── application/alerta/
│   ├── ConsultarAlertasSanitariasUseCase.java
│   ├── ConsultarDetalleAlertaSanitariaUseCase.java
│   ├── GenerarAlertaSanitariaUseCase.java
│   └── result/
│       ├── AlertaSanitariaDetalleResult.java
│       └── AlertaSanitariaResult.java
└── infrastructure/
    ├── adapter/in/rest/alerta/
    │   ├── AlertaSanitariaController.java
    │   ├── dto/
    │   │   ├── AlertaSanitariaDetalleResponse.java
    │   │   └── AlertaSanitariaPageResponse.java
    │   └── mapper/AlertaSanitariaRestMapper.java
    ├── adapter/out/persistence/alerta/AlertaSanitariaPersistenceAdapter.java
    └── config/AlertaSanitariaBeanConfiguration.java

src/main/resources/db/migration/
└── V__crear_alertas_sanitarias.sql

src/test/java/com/avicontrol/
├── domain/model/alerta/AlertaSanitariaTest.java
├── application/alerta/GenerarAlertaSanitariaUseCaseTest.java
└── infrastructure/adapter/in/rest/alerta/AlertaSanitariaControllerTest.java
```

**Structure Decision**: alerta es un agregado local inmutable. El reporte anormal es la fuente obligatoria y sus datos se conservan como contexto; galpón/lote se consultan sólo para complementar la vista veterinaria.

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Acordar con el Plan 017 el comando interno idempotente de generación.
- [ ] T002 Crear migración con unicidad por reporte de mortalidad anormal y referencias de lote y galpón.
- [ ] T003 Definir rangos de prioridad alta, crítica y emergencia sanitaria.

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T004 Crear `AlertaSanitaria` con estado inicial `PENDIENTE_ATENCION_VETERINARIA` e inmutabilidad.
- [ ] T005 Derivar prioridad desde porcentaje y severidad, elevando a emergencia la mortalidad total.
- [ ] T006 Definir repositorio, consulta de reporte y contexto de galpón/lote.
- [ ] T007 Implementar idempotencia por identificador del reporte y transacción/outbox con Plan 017.

**Checkpoint**: una repetición del mismo reporte devuelve la misma alerta; reportes distintos generan alertas distintas.

## Phase 3: User Story 1 - Generar alerta automática (Priority: P1)

**Goal**: crear una alerta pendiente para el veterinario inmediatamente tras un reporte anormal válido.

**Independent Test**: un reporte confirmado crea una alerta asociada; un reintento no duplica, y un reporte inexistente no crea ninguna.

- [ ] T008 [P] [US1] Probar idempotencia, prioridad, mortalidad total y reversión ante fallo.
- [ ] T009 [US1] Implementar `GenerarAlertaSanitariaUseCase` como puerto de aplicación sin controlador público de creación.

## Phase 4: User Story 2 - Consultar contexto de alerta (Priority: P2)

**Goal**: entregar al veterinario datos de galpón, lote y evento para atender la alerta.

**Independent Test**: una alerta pendiente muestra nombre/aforo/estado, lote con edad y población, y causa, observaciones, cantidad y porcentaje del reporte.

- [ ] T010 [P] [US2] Probar página vacía, detalle, contexto no disponible y autorización.
- [ ] T011 [US2] Implementar casos de uso de listado y detalle con DTO y mapper REST.

## Endpoints

| Método y ruta | Roles | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `GET /api/alertas-sanitarias?estado=PENDIENTE_ATENCION_VETERINARIA&page=0&size=20` | `ROLE_VETERINARIO`, `ROLE_ADMINISTRADOR` | Sin cuerpo | `200 OK` con página de alertas |
| `GET /api/alertas-sanitarias/{alertaId}` | `ROLE_VETERINARIO`, `ROLE_ADMINISTRADOR` | Sin cuerpo | `200 OK` con contexto consolidado |

```json
{
  "id": "b07f5fd4-3b77-4e9a-a4eb-5d54f907c8a4",
  "prioridad": "CRITICA",
  "estado": "PENDIENTE_ATENCION_VETERINARIA",
  "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "cantidadMuertes": 150,
  "porcentajeMortalidad": 3.0,
  "causaProbable": "RESPIRATORIA"
}
```

**Errores**: `400` paginación o filtro inválidos; `401` no autenticado; `403` sin rol; `404` alerta ausente; `503` contexto no disponible.

## Polish & Cross-Cutting Concerns

- [ ] T012 Documentar que la creación es interna y que la alerta no se cierra ni elimina aquí.
- [ ] T013 Añadir auditoría de generación y métricas de prioridad.
- [ ] T014 Ejecutar pruebas y `git diff --check`.

## Dependencies & Execution Order

- **Setup** depende del Plan 017.
- **Foundational** bloquea generación y consulta.
- **US1** genera contexto base; **US2** lo presenta de sólo lectura.

## Notes

- No hay endpoint público para crear alertas manualmente.
- Múltiples reportes del mismo galpón pueden generar alertas independientes y conservar su historial.
