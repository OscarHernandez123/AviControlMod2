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
src/main/java/com/avicontrol/application/alerta/ConsultarAlertasSanitariasUseCase.java
src/main/java/com/avicontrol/application/alerta/GenerarAlertaSanitariaUseCase.java
src/main/java/com/avicontrol/application/alerta/result/AlertaSanitariaResult.java
src/main/java/com/avicontrol/domain/model/alerta/AlertaSanitaria.java
src/main/java/com/avicontrol/domain/port/out/alerta/AlertaSanitariaQueryPort.java
src/main/java/com/avicontrol/domain/port/out/alerta/AlertaSanitariaRepositoryPort.java
src/main/java/com/avicontrol/infrastructure/adapter/in/rest/alerta/AlertaSanitariaController.java
src/main/java/com/avicontrol/infrastructure/adapter/in/rest/alerta/dto/AlertaSanitariaResponse.java
src/main/java/com/avicontrol/infrastructure/adapter/out/persistence/alerta/AlertaSanitariaPersistenceAdapter.java
src/main/java/com/avicontrol/infrastructure/adapter/out/persistence/alerta/AlertaSanitariaQueryAdapter.java
src/main/java/com/avicontrol/infrastructure/config/MortalidadBeanConfiguration.java
src/main/resources/db/migration/V21__crear_mortalidad_anormal_y_alertas.sql
src/test/java/com/avicontrol/infrastructure/adapter/in/rest/mortalidad/MortalidadAlertaIntegrationTest.java
src/test/java/com/avicontrol/application/mortalidad/MortalidadAlertaRollbackTest.java
src/test/java/com/avicontrol/domain/model/mortalidad/RegistroMortalidadAnormalTest.java
```

**Structure Decision**: alerta es un agregado local inmutable. El reporte anormal es la fuente obligatoria y sus datos se conservan como contexto; galpón/lote se consultan sólo para complementar la vista veterinaria.

## Phase 1: Setup (Shared Infrastructure)

- [x] T001 Acordar con el Plan 017 el comando interno idempotente de generación.
- [x] T002 Crear migración con unicidad por reporte de mortalidad anormal y referencias de lote y galpón.
- [x] T003 Definir rangos de prioridad alta, crítica y emergencia sanitaria.

## Phase 2: Foundational (Blocking Prerequisites)

- [x] T004 Crear `AlertaSanitaria` con estado inicial `PENDIENTE_ATENCION_VETERINARIA` e inmutabilidad.
- [x] T005 Derivar prioridad desde porcentaje y severidad, elevando a emergencia la mortalidad total.
- [x] T006 Definir repositorio, consulta de reporte y contexto de galpón/lote.
- [x] T007 Implementar idempotencia por identificador del reporte y transacción/outbox con Plan 017.

**Checkpoint**: una repetición del mismo reporte devuelve la misma alerta; reportes distintos generan alertas distintas.

## Phase 3: User Story 1 - Generar alerta automática (Priority: P1)

**Goal**: crear una alerta pendiente para el veterinario inmediatamente tras un reporte anormal válido.

**Independent Test**: un reporte confirmado crea una alerta asociada; un reintento no duplica, y un reporte inexistente no crea ninguna.

- [x] T008 [P] [US1] Probar idempotencia, prioridad, mortalidad total y reversión ante fallo.
- [x] T009 [US1] Implementar `GenerarAlertaSanitariaUseCase` como puerto de aplicación sin controlador público de creación.

## Phase 4: User Story 2 - Consultar contexto de alerta (Priority: P2)

**Goal**: entregar al veterinario datos de galpón, lote y evento para atender la alerta.

**Independent Test**: una alerta pendiente muestra nombre/aforo/estado, lote con edad y población, y causa, observaciones, cantidad y porcentaje del reporte.

- [x] T010 [P] [US2] Probar página vacía, detalle, contexto no disponible y autorización.
- [x] T011 [US2] Implementar casos de uso de listado y detalle con DTO y mapper REST.

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

- [x] T012 Documentar que la creación es interna y que la alerta no se cierra ni elimina aquí.
- [x] T013 Añadir auditoría de generación y métricas de prioridad.
- [x] T014 Ejecutar pruebas y `git diff --check`.

## Dependencies & Execution Order

- **Setup** depende del Plan 017.
- **Foundational** bloquea generación y consulta.
- **US1** genera contexto base; **US2** lo presenta de sólo lectura.

## Notes

- No hay endpoint público de creación. El reporte y la alerta comparten transacción; el índice único por `reporte_id` evita más de una alerta para el mismo reporte.
- La auditoría conserva fecha de emisión, identificador del reporte y responsable mediante la referencia al reporte inmutable. El contador `avicontrol.alertas_sanitarias.generadas` se incrementa por prioridad después de confirmar la transacción.
- Verificado el 12/11/2026 con generación única, página vacía, detalle, autorización, contexto no disponible y OpenAPI; `git diff --check` limpio.

- No hay endpoint público para crear alertas manualmente.
- Múltiples reportes del mismo galpón pueden generar alertas independientes y conservar su historial.
