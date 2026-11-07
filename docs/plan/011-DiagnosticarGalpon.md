# Implementation Plan: Diagnosticar un galpón

**Date**: 05/11/2026
**Spec**: [011-DiagnosticarGalpon.md](../specs/011-DiagnosticarGalpon.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Permitir que un veterinario diagnostique el lote actual de un galpón en `aislamiento`. Una enfermedad tratable exige medicación compatible y calcula la fecha de reintegro; una enfermedad que exige sacrificio sanitario no exige medicación ni reintegro y habilita la orden del Plan 012. Un proceso programado devuelve a `productiva` sólo los galpones que sigan aislados cuando vence su reintegro.

## Technical Context

**Performance Goals**: registrar diagnósticos válidos en un segundo y reintegrar automáticamente los galpones elegibles al recuperarse de una caída.
**Constraints**: `ROLE_VETERINARIO`; el estado vigente debe ser `aislamiento`; enfermedad obligatoria; medicación obligatoria sólo para enfermedad tratable.
**Scale/Scope**: diagnóstico de un galpón/lote actual, reintegro automático y habilitación de sacrificio sanitario; no administra medicamento ni ejecuta sacrificio.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 011-DiagnosticarGalpon.md
└── plan/
    └── 011-DiagnosticarGalpon.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/sanitario/diagnostico/
│   │   ├── DiagnosticoGalpon.java
│   │   └── EstadoDiagnostico.java
│   ├── exception/sanitario/diagnostico/
│   │   ├── GalponNoAisladoException.java
│   │   ├── MedicacionRequeridaException.java
│   │   └── MedicacionNoCompatibleException.java
│   └── port/out/
│       ├── modulo1/
│       │   ├── GalponQueryPort.java
│       │   ├── GalponUpdatePort.java
│       │   └── LoteQueryPort.java
│       └── sanitario/diagnostico/
│           ├── DiagnosticoGalponRepositoryPort.java
│           ├── EnfermedadQueryPort.java
│           └── MedicacionQueryPort.java
├── application/
│   └── sanitario/diagnostico/
│       ├── RegistrarDiagnosticoUseCase.java
│       ├── ReintegrarGalponesTratadosUseCase.java
│       └── result/DiagnosticoGalponResult.java
└── infrastructure/
    ├── adapter/in/rest/sanitario/diagnostico/
    │   ├── DiagnosticoController.java
    │   ├── dto/
    │   │   ├── DiagnosticoGalponResponse.java
    │   │   └── RegistrarDiagnosticoRequest.java
    │   └── mapper/DiagnosticoRestMapper.java
    ├── adapter/out/persistence/sanitario/diagnostico/DiagnosticoGalponPersistenceAdapter.java
    ├── scheduler/sanitario/diagnostico/ReintegroGalponesScheduler.java
    └── config/DiagnosticoBeanConfiguration.java

src/main/resources/db/migration/
└── V__crear_diagnosticos.sql

src/test/java/com/avicontrol/
├── domain/model/sanitario/diagnostico/DiagnosticoGalponTest.java
├── application/sanitario/diagnostico/
│   ├── RegistrarDiagnosticoUseCaseTest.java
│   └── ReintegrarGalponesTratadosUseCaseTest.java
└── infrastructure/adapter/in/rest/sanitario/diagnostico/DiagnosticoControllerTest.java
```

**Structure Decision**: diagnóstico es un agregado persistido que guarda referencias e instantáneas necesarias para conservar su decisión clínica. Catálogos y galpón/lote se verifican mediante puertos; el scheduler nunca sobrescribe un estado distinto de `aislamiento`.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: confirmar contratos de los Planes 008, 009 y 010, y preparar la ejecución de reintegro.

- [ ] T001 Confirmar consultas de enfermedad, medicación compatible, galpón/lote actual y actualización de estado.
- [ ] T002 Crear migración de diagnósticos con enfermedad obligatoria, medicación y fecha de reintegro opcionales.
- [ ] T003 Configurar `Clock` y scheduler de reintegro recuperable tras indisponibilidad.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: consolidar reglas clínicas, persistencia y transiciones automáticas.

- [ ] T004 Crear `DiagnosticoGalpon` y calcular `fechaReintegro = fechaDiagnostico + duracionDias` para enfermedad tratable.
- [ ] T005 Exigir medicación de la misma enfermedad si no requiere sacrificio sanitario; prohibir medicación y reintegro si lo requiere.
- [ ] T006 Definir repositorio de diagnósticos, puertos de consulta y excepciones de galpón no aislado o datos incompatibles.
- [ ] T007 Implementar búsqueda de diagnósticos vencidos que aún no se hayan reintegrado.

**Checkpoint**: el dominio distingue sin ambigüedad tratamiento de sacrificio sanitario y conserva los datos del momento de confirmar.

---

## Phase 3: User Story 1 - Registrar el diagnóstico de un galpón (Priority: P1)

**Goal**: crear el diagnóstico de un lote aislado, calcular reintegro tratable o habilitar sacrificio sanitario.

**Independent Test**: una medicación de cinco días genera reintegro cinco días después. Una enfermedad mortal crea diagnóstico sin medicación ni fecha y queda habilitada para el Plan 012.

### Tests for User Story 1

- [ ] T008 [P] [US1] Probar enfermedad tratable, enfermedad mortal, medicación incompatible y galpón fuera de aislamiento.
- [ ] T009 [P] [US1] Probar caso de uso con estado cambiado antes de confirmar y MockMvc de autorización.

### Implementation for User Story 1

- [ ] T010 [US1] Implementar `RegistrarDiagnosticoUseCase` y volver a consultar galpón y lote antes de persistir.
- [ ] T011 [US1] Consultar enfermedad y medicación vigentes al confirmar, y persistir el diagnóstico en una transacción.
- [ ] T012 [US1] Exponer `POST /api/galpones/{galponId}/diagnosticos` sólo para `ROLE_VETERINARIO`.

```json
{
  "enfermedadId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "medicacionId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "observaciones": "Tratamiento prescrito"
}
```

```json
{
  "id": "b07f5fd4-3b77-4e9a-a4eb-5d54f907c8a4",
  "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "estado": "ACTIVO",
  "requiereSacrificioSanitario": false,
  "fechaReintegro": "2026-11-12"
}
```

---

## Phase 4: User Story 2 - Reintegrar galpón tratado (Priority: P2)

**Goal**: cambiar automáticamente a `productiva` los galpones que siguen aislados al llegar la fecha de reintegro.

**Independent Test**: un diagnóstico vencido con galpón aún aislado lo actualiza una vez; si cambió a otro estado, no lo sobrescribe.

### Tests for User Story 2

- [ ] T013 [P] [US2] Probar ejecución puntual, recuperación posterior a caída, idempotencia y estado modificado externamente.

### Implementation for User Story 2

- [ ] T014 [US2] Implementar `ReintegrarGalponesTratadosUseCase` y scheduler.
- [ ] T015 [US2] Consultar el estado vigente antes de actualizar a `productiva` y marcar el reintegro sólo tras éxito.

**Checkpoint**: la automatización reintegra una sola vez y no sustituye decisiones posteriores sobre el galpón.

---

## Endpoints

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `POST /api/galpones/{galponId}/diagnosticos` | `ROLE_VETERINARIO` | `RegistrarDiagnosticoRequest` | `201 Created` con diagnóstico y fecha de reintegro opcional |

**Errores**: `400` datos inválidos; `401` no autenticado; `403` sin rol; `404` galpón, lote, enfermedad o medicación ausente; `409` galpón no aislado o medicación incompatible; `503` Módulo 1 no disponible.

---

## Polish & Cross-Cutting Concerns

- [ ] T016 Mapear `400` datos inválidos, `401` no autenticado, `403` sin rol, `404` referencias ausentes, `409` precondición o compatibilidad incumplida y `503` Módulo 1 no disponible.
- [ ] T017 Documentar los dos caminos clínicos y el comportamiento del reintegro en OpenAPI y operación.
- [ ] T018 Ejecutar pruebas y `git diff --check`.

---

## Dependencies & Execution Order

- **Setup** depende de los catálogos y de aislamiento validado.
- **Foundational** implementa reglas y bloquea ambas historias.
- **US1** registra decisiones; **US2** usa diagnósticos tratables ya persistidos.
- **Polish** completa contrato y monitoreo del scheduler.

## Notes

- El Plan 012 consume la habilitación de sacrificio sanitario, pero este plan no emite ni ejecuta la orden.
- La fecha de diagnóstico procede del `Clock`, no del cliente; la duración se consulta al momento de confirmar.
