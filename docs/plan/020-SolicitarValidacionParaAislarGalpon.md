# Plan: Solicitar validación para aislar galpón

**Spec**: [020-SolicitarValidacionParaAislarGalpon.md](../specs/020-SolicitarValidacionParaAislarGalpon.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

Trabajador o administrador crea una solicitud pendiente; no cambia el estado del galpón.

## Fuera de alcance

Validación veterinaria, diagnóstico, alerta o transición a aislamiento.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/domain/model/sanitario/aislamiento/{SolicitudAislamiento,EstadoSolicitudAislamiento}.java
src/main/java/com/avicontrol/application/sanitario/aislamiento/{CrearSolicitudAislamientoUseCase,ConsultarSolicitudesPendientesUseCase}.java
src/main/java/com/avicontrol/infrastructure/adapter/in/rest/sanitario/aislamiento/AislamientoController.java
src/main/resources/db/migration/V__crear_solicitudes_aislamiento.sql
```

## User Story 1 — Crear solicitud

| Método y ruta | Roles |
| --- | --- |
| `POST /api/galpones/{galponId}/solicitudes-aislamiento` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` |

```json
{"motivo":"Signos respiratorios","signosClinicos":"Tos y decaimiento"}
```

```json
{"id":"uuid","estado":"PENDIENTE_VALIDACION","galponId":"uuid"}
```

Errores: `404` galpón/lote, `409` solicitud pendiente existente, `503` Módulo 1 no disponible.

## Pruebas y Definition of Done

- Snapshot de galpón/lote, duplicados y ausencia de transición de estado.
- Pruebas HTTP, seguridad y migración verdes.
