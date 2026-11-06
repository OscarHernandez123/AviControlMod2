# Implementation Plan: Solicitar validación para aislar galpón

**Date**: 05/11/2026
**Spec**: [020-SolicitarValidacionParaAislarGalpon.md](../specs/020-SolicitarValidacionParaAislarGalpon.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Permitir que un trabajador registre una solicitud formal de validación de aislamiento para un galpón `productiva` con lote activo y aves vivas. La solicitud conserva el contexto y queda pendiente para el veterinario; no modifica el estado del galpón ni permite duplicados pendientes.

## Technical Context

**Performance Goals**: dejar la solicitud disponible al veterinario en un segundo.
**Constraints**: `ROLE_TRABAJADOR` o administrador; motivo y signos obligatorios; galpón productivo, lote activo y población positiva; solicitud inmutable.
**Scale/Scope**: creación y consulta interna de pendientes; no valida el aislamiento ni diagnostica.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 020-SolicitarValidacionParaAislarGalpon.md
└── plan/
    └── 020-SolicitarValidacionParaAislarGalpon.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/sanitario/aislamiento/
│   │   ├── EstadoSolicitudAislamiento.java
│   │   ├── GalponLoteAislamiento.java
│   │   └── SolicitudAislamiento.java
│   ├── exception/sanitario/aislamiento/
│   │   ├── EstadoGalponNoAislableException.java
│   │   ├── GalponNoEncontradoException.java
│   │   ├── LoteActivoNoEncontradoException.java
│   │   ├── Modulo1NoDisponibleException.java
│   │   ├── SolicitudAislamientoDuplicadaException.java
│   │   └── SolicitudAislamientoInvalidaException.java
│   └── port/out/sanitario/aislamiento/
│       ├── GalponLoteAislamientoQueryPort.java
│       └── SolicitudAislamientoRepositoryPort.java
├── application/sanitario/aislamiento/
│   ├── CrearSolicitudAislamientoCommand.java
│   ├── CrearSolicitudAislamientoUseCase.java
│   └── result/SolicitudAislamientoResult.java
└── infrastructure/
    ├── adapter/in/rest/sanitario/aislamiento/
    │   ├── SolicitudAislamientoController.java
    │   ├── dto/
    │   │   ├── CrearSolicitudAislamientoRequest.java
    │   │   └── SolicitudAislamientoResponse.java
    │   └── mapper/SolicitudAislamientoRestMapper.java
    ├── adapter/out/persistence/sanitario/aislamiento/SolicitudAislamientoPersistenceAdapter.java
    ├── adapter/out/integration/modulo1/
    │   ├── GalponLocalAdapter.java
    │   └── GalponNoDisponibleAdapter.java
    └── config/SolicitudAislamientoBeanConfiguration.java

src/main/resources/db/migration/
└── V11__crear_solicitudes_aislamiento.sql

src/test/java/com/avicontrol/
├── domain/model/sanitario/aislamiento/SolicitudAislamientoTest.java
├── application/sanitario/aislamiento/CrearSolicitudAislamientoUseCaseTest.java
└── infrastructure/adapter/in/rest/sanitario/aislamiento/SolicitudAislamientoControllerTest.java
```

**Structure Decision**: la solicitud es un agregado local inmutable. Galpón y lote se consultan mediante un puerto y nunca se actualizan desde este caso de uso.

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Confirmar contrato de galpón/lote activo y estado `productiva` del Módulo 1.
- [ ] T002 Crear migración con unicidad parcial de solicitud pendiente por galpón.
- [ ] T003 Preparar fixtures de lote ausente, población cero, estado no productivo y solicitud pendiente.

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T004 Crear `SolicitudAislamiento` con estado inicial `PENDIENTE_VALIDACION` e inmutabilidad.
- [ ] T005 Validar textos no vacíos, población positiva y snapshot de edad calculada con `Clock`.
- [ ] T006 Definir repositorio que impida dos solicitudes pendientes simultáneas bajo concurrencia.

**Checkpoint**: la solicitud no puede ser parcial, duplicada ni alterar el estado del galpón.

## Phase 3: User Story 1 - Crear y enviar solicitud (Priority: P1)

**Goal**: registrar una petición válida y disponible para la evaluación veterinaria.

**Independent Test**: un galpón productivo con lote activo crea una solicitud pendiente; un galpón no productivo, sin aves o texto vacío no crea ninguna solicitud.

- [ ] T007 [P] [US1] Probar snapshot, validaciones y que no se llama ningún puerto de actualización.
- [ ] T008 [US1] Implementar `CrearSolicitudAislamientoUseCase`.
- [ ] T009 [US1] Exponer endpoint y DTO REST.

## Phase 4: User Story 2 - Evitar solicitudes duplicadas (Priority: P2)

**Goal**: bloquear una segunda solicitud pendiente para el mismo galpón.

**Independent Test**: tras una solicitud pendiente, un nuevo POST devuelve conflicto e informa el estado existente.

- [ ] T010 [P] [US2] Probar carrera de dos solicitudes y restricción única.
- [ ] T011 [US2] Traducir duplicado a error de negocio sin crear un registro adicional.

## Endpoints

| Método y ruta | Roles | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `POST /api/galpones/{galponId}/solicitudes-aislamiento` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` | `CrearSolicitudAislamientoRequest` | `201 Created` con solicitud pendiente |
| `GET /api/solicitudes-aislamiento/pendientes` | `ROLE_VETERINARIO` | — | `200 OK` con solicitudes pendientes |
| `GET /api/solicitudes-aislamiento/{solicitudId}` | `ROLE_VETERINARIO` | — | `200 OK` con detalle de solicitud |

```json
{
  "motivo": "Signos respiratorios",
  "signosClinicos": "Tos y decaimiento"
}
```

```json
{
  "id": "b07f5fd4-3b77-4e9a-a4eb-5d54f907c8a4",
  "estado": "PENDIENTE_VALIDACION",
  "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "loteId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "motivo": "Signos respiratorios",
  "signosClinicos": "Tos y decaimiento",
  "poblacionVivaReportada": 7960,
  "edadDiasReportada": 6,
  "trabajador": "trabajador",
  "fechaEmision": "2026-11-06T12:00:00Z",
  "observaciones": null,
  "veterinario": null,
  "fechaDecision": null
}
```

En `local` y `test`, el Módulo 1 se sustituye por un galpón de revisión con UUID `550e8400-e29b-41d4-a716-446655440101` y lote `550e8400-e29b-41d4-a716-446655440102`. El estado se guarda en la tabla local `modulo1_galpon_local`. Fuera de esos perfiles la consulta responde `503` hasta conectar el contrato real del Módulo 1.

**Errores**: `400` textos o UUID inválidos; `401` no autenticado; `403` sin rol; `404` galpón o lote ausente; `409` estado, población o solicitud pendiente incompatible; `503` Módulo 1 no disponible.

## Polish & Cross-Cutting Concerns

- [ ] T012 Documentar que sólo el Plan 010 puede resolver la solicitud y cambiar a aislamiento.
- [ ] T013 Ejecutar pruebas, OpenAPI y `git diff --check`.

## Dependencies & Execution Order

- **Setup** confirma fuentes externas.
- **Foundational** bloquea creación y prevención de duplicados.
- **US1** crea la solicitud; **US2** refuerza su unicidad.

## Notes

- Los datos enviados y sus snapshots no se editan, anulan ni retiran; únicamente el Plan 010 resuelve el estado de la solicitud.
- El Plan 010 consume la solicitud pendiente y decide la validación veterinaria.
