# Plan: Ordenar sacrificio sanitario

**Spec**: [012-OrdenarSacrificioSanitario.md](../specs/012-OrdenarSacrificioSanitario.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

El veterinario emite y confirma una orden sanitaria total basada en diagnóstico elegible.

## Fuera de alcance

Diagnóstico, sacrificio productivo, peso final y liquidación comercial.

## Dependencias

Plan 011 y Módulo 1 para estado/población vigente.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/domain/model/sacrificio/sanitario/{OrdenSacrificioSanitario,EstadoOrdenSanitaria}.java
src/main/java/com/avicontrol/application/sacrificio/sanitario/{EmitirOrdenSacrificioSanitarioUseCase,ConfirmarEjecucionSacrificioSanitarioUseCase,ConsultarOrdenSacrificioSanitarioUseCase}.java
src/main/java/com/avicontrol/infrastructure/adapter/in/rest/sacrificio/sanitario/OrdenSacrificioSanitarioController.java
src/main/resources/db/migration/V__crear_ordenes_sacrificio_sanitario.sql
```

## User Story 1 — Emitir orden

| Método y ruta | Rol |
| --- | --- |
| `POST /api/galpones/{galponId}/ordenes-sacrificio-sanitario` | `ROLE_VETERINARIO` |

## User Story 2 — Ejecutar y consultar

| Método y ruta | Roles |
| --- | --- |
| `POST /api/ordenes-sacrificio-sanitario/{ordenId}/ejecucion` | `ROLE_VETERINARIO` |
| `GET /api/ordenes-sacrificio-sanitario/{ordenId}` | `ROLE_VETERINARIO`, `ROLE_ADMINISTRADOR` |

```json
{"id":"uuid","estado":"EJECUTADA","galponId":"uuid","poblacionSacrificada":7960}
```

Errores: `404` orden/diagnóstico, `409` diagnóstico no elegible, sin población o ejecución repetida.

## Pruebas y Definition of Done

- Orden pendiente, ejecución única, población a cero y transición `VACIADO_SANITARIO` atómica.
- Integración de evento y pruebas HTTP/OpenAPI verdes.
