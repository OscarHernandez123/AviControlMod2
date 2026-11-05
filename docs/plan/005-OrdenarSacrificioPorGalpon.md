# Plan: Ordenar sacrificio por galpón

**Spec**: [005-OrdenarSacrificioPorGalpon.md](../specs/005-OrdenarSacrificioPorGalpon.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

El administrador programa una orden productiva futura para un lote previamente apto. También puede reprogramarla o cancelarla antes de la ejecución.

## Fuera de alcance

Evaluación de aptitud, ejecución física, resultado de peso/cantidad, liquidación y sacrificio sanitario.

## Dependencias

Plan 004 con validación apta vigente, Módulo 1 para estado de galpón/lote y `Clock`.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/model/sacrificio/productivo/{OrdenSacrificioProductivo.java,EstadoOrdenProductiva.java}
├── domain/port/out/sacrificio/productivo/OrdenSacrificioProductivoRepositoryPort.java
├── application/sacrificio/productivo/{ProgramarSacrificioProductivoUseCase,ReprogramarSacrificioProductivoUseCase,CancelarSacrificioProductivoUseCase}.java
├── infrastructure/adapter/in/rest/sacrificio/productivo/SacrificioProductivoController.java
├── infrastructure/adapter/in/rest/sacrificio/productivo/dto/{ProgramarOrdenRequest,ReprogramarOrdenRequest,CancelarOrdenRequest}.java
└── infrastructure/adapter/out/persistence/sacrificio/productivo/OrdenSacrificioProductivoPersistenceAdapter.java
src/main/resources/db/migration/V__crear_ordenes_sacrificio_productivo.sql
```

## User Story 1 — Programar orden

| Método y ruta | Rol |
| --- | --- |
| `POST /api/galpones/{galponId}/ordenes-sacrificio` | `ROLE_ADMINISTRADOR` |

```json
{"fechaHoraProgramada":"2026-12-01T08:00:00-05:00"}
```

```json
{"id":"uuid","galponId":"uuid","estado":"PROGRAMADA","fechaHoraProgramada":"2026-12-01T13:00:00Z"}
```

## User Story 2 — Reprogramar o cancelar

| Método y ruta | Rol |
| --- | --- |
| `PUT /api/ordenes-sacrificio/{ordenId}/reprogramacion` | `ROLE_ADMINISTRADOR` |
| `POST /api/ordenes-sacrificio/{ordenId}/cancelacion` | `ROLE_ADMINISTRADOR` |

Errores: `400` fecha/motivo inválido, `404` orden inexistente, `409` orden no programada, aptitud vencida o fecha no futura.

## Pruebas y Definition of Done

- Programación futura, reprogramación, cancelación y rechazo tras ejecución.
- Idempotencia y concurrencia de cambios de estado.
- Migración, OpenAPI, pruebas de caso de uso y MockMvc verdes.
