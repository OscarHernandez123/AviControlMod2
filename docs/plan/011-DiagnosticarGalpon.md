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
│   │   ├── DatosDiagnosticoInvalidosException.java
│   │   ├── DiagnosticoActivoExistenteException.java
│   │   ├── GalponNoAisladoException.java
│   │   ├── MedicacionRequeridaException.java
│   │   └── MedicacionNoCompatibleException.java
│   └── port/out/
│       ├── sanitario/aislamiento/GalponLoteAislamientoQueryPort.java
│       └── sanitario/diagnostico/
│           ├── DiagnosticoGalponRepositoryPort.java
│           ├── EnfermedadQueryPort.java
│           ├── GalponReintegroPort.java
│           └── MedicacionQueryPort.java
├── application/
│   └── sanitario/diagnostico/
│       ├── RegistrarDiagnosticoCommand.java
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
    ├── adapter/out/persistence/sanitario/diagnostico/
    │   ├── DiagnosticoCatalogoQueryAdapter.java
    │   └── DiagnosticoGalponPersistenceAdapter.java
    ├── adapter/out/integration/modulo1/
    │   ├── GalponReintegroLocalAdapter.java
    │   └── GalponReintegroNoDisponibleAdapter.java
    ├── scheduler/sanitario/diagnostico/ReintegroGalponesScheduler.java
    └── config/DiagnosticoBeanConfiguration.java

src/main/resources/db/migration/
├── V12__crear_diagnosticos_galpon.sql
└── V15__ampliar_prescripciones_y_diagnosticos.sql

src/test/java/com/avicontrol/
├── domain/model/sanitario/diagnostico/DiagnosticoGalponTest.java
├── application/sanitario/diagnostico/
│   ├── RegistrarDiagnosticoUseCaseTest.java
│   └── ReintegrarGalponesTratadosUseCaseTest.java
└── infrastructure/adapter/in/rest/sanitario/diagnostico/DiagnosticoControllerTest.java
```

**Structure Decision**: diagnóstico es un agregado persistido que guarda referencias e instantáneas necesarias para conservar su decisión clínica. Se reutiliza el puerto de lectura de galpón/lote del Plan 020 y se añade un puerto específico de reintegro, sin cambiar las firmas compartidas con otros planes. El scheduler nunca sobrescribe un estado distinto de `aislamiento` ni otro lote.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: confirmar contratos de los Planes 008, 009 y 010, y preparar la ejecución de reintegro.

- [x] T001 Confirmar consultas de enfermedad, medicación compatible, galpón/lote actual y actualización de estado.
- [x] T002 Crear migración de diagnósticos con enfermedad obligatoria, medicación y fecha de reintegro opcionales.
- [x] T003 Configurar `Clock` y scheduler de reintegro recuperable tras indisponibilidad.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: consolidar reglas clínicas, persistencia y transiciones automáticas.

- [x] T004 Crear `DiagnosticoGalpon` y calcular `fechaReintegro = fechaDiagnostico + duracionDias` para enfermedad tratable.
- [x] T005 Exigir medicación de la misma enfermedad si no requiere sacrificio sanitario; prohibir medicación y reintegro si lo requiere.
- [x] T006 Definir repositorio de diagnósticos, puertos de consulta y excepciones de galpón no aislado o datos incompatibles.
- [x] T007 Implementar búsqueda de diagnósticos vencidos que aún no se hayan reintegrado.

**Checkpoint**: el dominio distingue sin ambigüedad tratamiento de sacrificio sanitario y conserva los datos del momento de confirmar.

---

## Phase 3: User Story 1 - Registrar el diagnóstico de un galpón (Priority: P1)

**Goal**: crear el diagnóstico de un lote aislado, calcular reintegro tratable o habilitar sacrificio sanitario.

**Independent Test**: una medicación de cinco días genera reintegro cinco días después. Una enfermedad mortal crea diagnóstico sin medicación ni fecha y queda habilitada para el Plan 012.

### Tests for User Story 1

- [x] T008 [P] [US1] Probar enfermedad tratable, enfermedad mortal, medicación incompatible y galpón fuera de aislamiento.
- [x] T009 [P] [US1] Probar caso de uso con estado cambiado antes de confirmar y MockMvc de autorización.

### Implementation for User Story 1

- [x] T010 [US1] Implementar `RegistrarDiagnosticoUseCase` y volver a consultar galpón y lote antes de persistir.
- [x] T011 [US1] Consultar enfermedad y medicación vigentes al confirmar, y persistir el diagnóstico en una transacción.
- [x] T012 [US1] Exponer `POST /api/galpones/{galponId}/diagnosticos` sólo para `ROLE_VETERINARIO`.

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
  "loteId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "enfermedadId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "enfermedadNombre": "Newcastle",
  "medicacionId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "medicamentoId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "medicamentoNombre": "Amprolio 20% Solución",
  "principioActivo": "Amprolio",
  "dosis": "0.5 ml por ave",
  "duracionDias": 5,
  "diasAplicacion": [
    1,
    3,
    5
  ],
  "descripcionMedicacion": "Administrar según indicación veterinaria",
  "observaciones": "Tratamiento prescrito",
  "fechaDiagnostico": "2026-11-07",
  "estado": "ACTIVO",
  "requiereSacrificioSanitario": false,
  "fechaReintegro": "2026-11-12"
}
```

Para una enfermedad que requiere sacrificio sanitario, `medicacionId` se omite de la solicitud. En la respuesta, `medicacionId`, `medicamentoId`, `medicamentoNombre`, `principioActivo`, `dosis`, `duracionDias`, `diasAplicacion`, `descripcionMedicacion` y `fechaReintegro` son `null`; `requiereSacrificioSanitario` es `true`. Se admiten diagnósticos tratables simultáneos de enfermedades distintas en un galpón. El índice único impide duplicar una misma enfermedad activa. El diagnóstico sanitario excluye cualquier otro activo y bloquea nuevos tratamientos.

---

## Phase 4: User Story 2 - Reintegrar galpón tratado (Priority: P2)

**Goal**: cambiar automáticamente a `productiva` los galpones que siguen aislados cuando venza el último tratamiento activo.

**Independent Test**: un diagnóstico vencido con galpón aún aislado lo actualiza una vez; si cambió a otro estado, no lo sobrescribe.

### Tests for User Story 2

- [x] T013 [P] [US2] Probar ejecución puntual, recuperación posterior a caída, idempotencia y estado modificado externamente.

### Implementation for User Story 2

- [x] T014 [US2] Implementar `ReintegrarGalponesTratadosUseCase` y scheduler.
- [x] T015 [US2] Consultar el estado vigente antes de actualizar a `productiva` y marcar el reintegro sólo tras éxito.

**Checkpoint**: la automatización reintegra una sola vez y no sustituye decisiones posteriores sobre el galpón.

---

## Endpoints

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `POST /api/galpones/{galponId}/diagnosticos` | `ROLE_VETERINARIO` | `RegistrarDiagnosticoRequest` | `201 Created` con diagnóstico y fecha de reintegro opcional |
| `GET /api/diagnosticos/{diagnosticoId}` | `ROLE_VETERINARIO` | — | `200 OK` con la instantánea clínica y estado vigente del diagnóstico |

**Errores**: `400` datos inválidos; `401` no autenticado; `403` sin rol; `404` galpón, lote, enfermedad o medicación ausente; `409` galpón no aislado o medicación incompatible; `503` Módulo 1 no disponible.

---

## Polish & Cross-Cutting Concerns

- [x] T016 Mapear `400` datos inválidos, `401` no autenticado, `403` sin rol, `404` referencias ausentes, `409` precondición o compatibilidad incumplida y `503` Módulo 1 no disponible.
- [x] T017 Documentar los dos caminos clínicos y el comportamiento del reintegro en OpenAPI y operación.
- [x] T018 Ejecutar pruebas y `git diff --check`.

---

## Dependencies & Execution Order

- **Setup** depende de los catálogos y de aislamiento validado.
- **Foundational** implementa reglas y bloquea ambas historias.
- **US1** registra decisiones; **US2** usa diagnósticos tratables ya persistidos.
- **Polish** completa contrato y monitoreo del scheduler.

## Notes

- El Plan 012 consume la habilitación de sacrificio sanitario, pero este plan no emite ni ejecuta la orden.
- La fecha de diagnóstico procede del `Clock`, no del cliente; la duración se consulta al momento de confirmar.
- En `local` y `test`, el reintegro usa la tabla de revisión `modulo1_galpon_local` del Plan 020; fuera de esos perfiles el puerto del Módulo 1 responde `503` hasta conectar el contrato real.
- El scheduler se ejecuta al iniciar y cada minuto. Reintenta diagnósticos vencidos tras indisponibilidad; si el galpón ya no está aislado o contiene otro lote, marca el reintegro `REINTEGRO_OMITIDO` sin cambiar el galpón. La actualización del galpón local y la resolución del diagnóstico comparten transacción.
- Si queda otro diagnóstico activo, el scheduler resuelve el vencido como `REINTEGRO_OMITIDO` y conserva el aislamiento; cuando vence el último, reintegra el galpón. El registro de diagnósticos usa aislamiento transaccional serializable para proteger la decisión frente a altas concurrentes.
- La instantánea clínica ahora incluye nombre, principio activo y calendario de aplicación de la prescripción. Los diagnósticos anteriores a V15 pueden conservar esos campos en `null`.
- Las instantáneas de nombre de enfermedad, decisión sanitaria, medicamento, dosis y duración prueban que las ediciones posteriores de los Planes 008 y 009 no alteran el diagnóstico previo.
