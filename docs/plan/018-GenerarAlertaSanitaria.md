# Plan: Generar alerta sanitaria

**Spec**: [018-GenerarAlertaSanitaria.md](../specs/018-GenerarAlertaSanitaria.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

Creación interna idempotente y consulta contextual de alertas sanitarias de mortalidad anormal.

## Fuera de alcance

Alta manual, diagnóstico, aislamiento, cierre de alertas o cambio de estado de galpón.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/domain/model/alerta/{AlertaSanitaria,PrioridadAlerta,EstadoAlertaSanitaria}.java
src/main/java/com/avicontrol/domain/port/out/alerta/{AlertaSanitariaRepositoryPort,MortalidadAnormalQueryPort}.java
src/main/java/com/avicontrol/application/alerta/{GenerarAlertaSanitariaUseCase,ConsultarAlertasSanitariasUseCase,ConsultarDetalleAlertaSanitariaUseCase}.java
src/main/java/com/avicontrol/infrastructure/adapter/in/rest/alerta/AlertaSanitariaController.java
```

## User Story 1 — Generar internamente

Sin endpoint público; `RegistrarMortalidadAnormal` invoca el puerto público de aplicación.

## User Story 2 — Consultar alertas

| Método y ruta | Roles |
| --- | --- |
| `GET /api/alertas-sanitarias?estado=PENDIENTE_ATENCION_VETERINARIA&page=0&size=20` | `ROLE_VETERINARIO`, `ROLE_ADMINISTRADOR` |
| `GET /api/alertas-sanitarias/{alertaId}` | `ROLE_VETERINARIO`, `ROLE_ADMINISTRADOR` |

```json
{"content":[{"id":"uuid","prioridad":"ALTA","estado":"PENDIENTE_ATENCION_VETERINARIA","galponId":"uuid"}],"page":0,"size":20,"totalElements":1}
```

Errores: `404` alerta, `503` contexto no disponible. Pruebas incluyen idempotencia, prioridad y autorización.
