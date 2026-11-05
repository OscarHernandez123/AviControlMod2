# Plan: Validar aislamiento

**Spec**: [010-ValidarAislamiento.md](../specs/010-ValidarAislamiento.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

El veterinario acepta o rechaza una solicitud pendiente, verifica el estado vigente y aplica el cambio de estado mediante el puerto del Módulo 1.

## Fuera de alcance

Creación de solicitud, diagnóstico, medicación, alerta automática o reintegro.

## Dependencias

Plan 020 crea solicitudes; Módulo 1 es propietario del cambio de estado.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/model/sanitario/aislamiento/{SolicitudAislamiento,DecisionAislamiento,EstadoSolicitudAislamiento}.java
├── domain/port/out/sanitario/SolicitudAislamientoRepositoryPort.java
├── domain/port/out/modulo1/{GalponQueryPort,CambiarEstadoOperativoGalponPort}.java
├── application/sanitario/aislamiento/ValidarAislamientoUseCase.java
├── infrastructure/adapter/in/rest/sanitario/aislamiento/AislamientoController.java
└── infrastructure/adapter/out/persistence/sanitario/aislamiento/SolicitudAislamientoPersistenceAdapter.java
```

## User Story 1 — Validar solicitud

| Método y ruta | Rol |
| --- | --- |
| `POST /api/solicitudes-aislamiento/{solicitudId}/validacion` | `ROLE_VETERINARIO` |

```json
{"decision":"ACEPTAR","observaciones":"Aislamiento requerido"}
```

```json
{"id":"uuid","estado":"ACEPTADA","galponId":"uuid","estadoGalponResultante":"AISLAMIENTO"}
```

Errores: `400` decisión inválida, `404` solicitud inexistente, `409` solicitud ya resuelta/estado vigente incompatible, `503` Módulo 1 no disponible.

## Pruebas y Definition of Done

- Aceptar, rechazar, duplicado y cambio concurrente de estado.
- Cambio de estado y decisión se confirman atómicamente; pruebas HTTP/OpenAPI verdes.
