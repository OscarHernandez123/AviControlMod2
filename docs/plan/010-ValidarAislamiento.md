# Implementation Plan: Validar aislamiento de un galpón

**Date**: 05/11/2026
**Spec**: [010-ValidarAislamiento.md](../specs/010-ValidarAislamiento.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Permitir que un veterinario evalúe una solicitud de revisión enviada por un trabajador y, si el galpón continúa en estado `productiva`, valide su aislamiento cambiándolo a `aislamiento` en el Módulo 1. Recibir la solicitud o iniciar la evaluación no cambia el galpón.

## Technical Context

**Performance Goals**: completar el 95 % de validaciones correctas en un segundo.
**Constraints**: sólo `ROLE_VETERINARIO`; exige una solicitud previa y estado vigente `productiva`; sólo se modifica el estado del galpón.
**Scale/Scope**: decidir una solicitud por galpón; no crea solicitudes, diagnósticos, tratamientos ni reintegros.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 010-ValidarAislamiento.md
└── plan/
    └── 010-ValidarAislamiento.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/sanitario/aislamiento/
│   │   ├── DecisionAislamiento.java
│   │   ├── EstadoSolicitudAislamiento.java
│   │   └── SolicitudAislamiento.java
│   ├── exception/sanitario/aislamiento/
│   │   ├── EstadoGalponNoAislableException.java
│   │   ├── Modulo1NoDisponibleException.java
│   │   ├── SolicitudAislamientoNoEncontradaException.java
│   │   └── SolicitudAislamientoNoPendienteException.java
│   └── port/out/
│       ├── modulo1/
│       │   ├── GalponQueryPort.java
│       │   └── GalponUpdatePort.java
│       └── sanitario/aislamiento/SolicitudAislamientoRepositoryPort.java
├── application/
│   └── sanitario/aislamiento/
│       ├── ValidarAislamientoCommand.java
│       ├── ValidarAislamientoUseCase.java
│       └── result/ValidacionAislamientoResult.java
└── infrastructure/
    ├── adapter/in/rest/sanitario/aislamiento/
    │   ├── AislamientoController.java
    │   ├── dto/
    │   │   ├── ValidacionAislamientoResponse.java
    │   │   └── ValidarAislamientoRequest.java
    │   └── mapper/AislamientoRestMapper.java
    ├── adapter/out/persistence/sanitario/aislamiento/SolicitudAislamientoPersistenceAdapter.java
    ├── adapter/out/integration/modulo1/
    │   ├── GalponLocalAdapter.java
    │   └── GalponNoDisponibleAdapter.java
    └── config/AislamientoBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/sanitario/aislamiento/SolicitudAislamientoTest.java
├── application/sanitario/aislamiento/ValidarAislamientoUseCaseTest.java
└── infrastructure/adapter/in/rest/sanitario/aislamiento/AislamientoControllerTest.java
```

**Structure Decision**: la solicitud y su decisión se administran localmente. Galpón sigue siendo propiedad del Módulo 1: se consulta de nuevo antes de validar y se actualiza sólo mediante su puerto.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: confirmar el contrato de la solicitud del Plan 020 y las transiciones del Módulo 1.

- [ ] T001 Acordar los estados de solicitud pendientes y resueltos con el Plan 020.
- [ ] T002 Verificar el contrato para leer y cambiar solamente el estado operativo del galpón.
- [ ] T003 Preparar fixtures de solicitud pendiente, solicitud resuelta, galpón productivo, aislamiento y módulo no disponible.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: establecer transiciones de solicitud y validaciones de estado.

- [ ] T004 Crear `SolicitudAislamiento` con decisión única desde estado pendiente.
- [ ] T005 Definir excepciones de solicitud ausente o resuelta y de estado de galpón incompatible.
- [ ] T006 Implementar repositorio y puertos del Módulo 1; proteger la solicitud de dobles decisiones con versión o actualización condicional.

**Checkpoint**: una solicitud sólo puede ser resuelta una vez y no puede cambiar un galpón sin verificarlo nuevamente.

---

## Phase 3: User Story 1 - Evaluar y validar el aislamiento solicitado (Priority: P1)

**Goal**: permitir al veterinario iniciar desde una solicitud y validar el aislamiento sólo para un galpón productivo vigente.

**Independent Test**: una solicitud pendiente asociada a galpón `productiva` cambia éste a `aislamiento`; una solicitud ausente, resuelta o un estado distinto no produce cambios.

### Tests for User Story 1

- [ ] T007 [P] [US1] Probar aceptación, rechazo, decisión duplicada y cambio concurrente de estado.
- [ ] T008 [P] [US1] Probar el caso de uso con puertos simulados y que se consulta el estado justo antes de actualizar.
- [ ] T009 [P] [US1] Probar MockMvc para autorización y contrato JSON.

### Implementation for User Story 1

- [ ] T010 [US1] Implementar `ValidarAislamientoUseCase` y consultar solicitud y galpón vigente.
- [ ] T011 [US1] Al aceptar, actualizar el galpón a `aislamiento` y registrar la decisión de manera consistente.
- [ ] T012 [US1] Al rechazar, registrar sólo la decisión y conservar el estado del galpón.
- [ ] T013 [US1] Exponer `POST /api/solicitudes-aislamiento/{solicitudId}/validacion` para `ROLE_VETERINARIO`.

```json
{
  "decision": "ACEPTAR",
  "observaciones": "Aislamiento requerido"
}
```

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "estado": "ACEPTADA",
  "galponId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "estadoGalponResultante": "AISLAMIENTO"
}
```

**Checkpoint**: la solicitud por sí sola no altera el galpón; sólo la decisión veterinaria aceptada puede hacerlo.

---

## Endpoints

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `POST /api/solicitudes-aislamiento/{solicitudId}/validacion` | `ROLE_VETERINARIO` | `ValidarAislamientoRequest` | `200 OK` con decisión y estado resultante |

**Errores**: `400` decisión inválida; `401` no autenticado; `403` sin rol; `404` solicitud inexistente; `409` solicitud resuelta o estado incompatible; `503` Módulo 1 no disponible.

---

## Polish & Cross-Cutting Concerns

- [ ] T014 Mapear `400` decisión inválida, `401` no autenticado, `403` sin rol, `404` solicitud ausente, `409` solicitud resuelta o estado incompatible y `503` Módulo 1 no disponible.
- [ ] T015 Documentar que se vuelve a consultar el estado y que no se alteran otras entidades del Módulo 1.
- [ ] T016 Ejecutar pruebas y `git diff --check`.

---

## Dependencies & Execution Order

- **Setup** depende de los contratos del Plan 020 y Módulo 1.
- **Foundational** crea el ciclo de vida de la solicitud y bloquea la validación.
- **US1** consulta, decide y actualiza sólo tras cumplir precondiciones.
- **Polish** cierra seguridad y contrato.

## Notes

- Un galpón ya en `aislamiento` no se valida de nuevo.
- Si la actualización remota falla, la solicitud no debe permanecer incorrectamente como aceptada; se requiere transacción distribuida, compensación o reintento seguro documentado.
- La implementación de `local` y `test` usa una única base de datos: el cambio condicional de estado y la resolución condicional de solicitud comparten transacción. Fuera de esos perfiles el adaptador del Módulo 1 devuelve `503`; para conectar el servicio remoto debe definirse un mecanismo de compensación o reintento antes de habilitar la aceptación.
