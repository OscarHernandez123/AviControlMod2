# Implementation Plan: Registrar mortalidad anormal

**Date**: 05/11/2026
**Spec**: [017-RegistrarMortalidadAnormal.md](../specs/017-RegistrarMortalidadAnormal.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Registrar un reporte inmutable de mortalidad anormal, calcular su porcentaje y severidad según la población viva vigente, y generar una alerta sanitaria inmediata. El reporte sólo es válido para un galpón productivo con lote activo; no cambia directamente población, aislamiento ni diagnóstico.

## Technical Context

**Performance Goals**: registrar evento y alerta de forma consistente en un segundo.
**Constraints**: `ROLE_TRABAJADOR` o administrador autorizado; cantidad positiva no mayor a población; fecha válida; causa probable obligatoria; sin edición ni eliminación.
**Scale/Scope**: reporte anormal, cálculo y alerta; no administra la alerta ni actualiza el inventario vivo.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 017-RegistrarMortalidadAnormal.md
└── plan/
    └── 017-RegistrarMortalidadAnormal.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/mortalidad/
│   │   ├── NivelSeveridadMortalidad.java
│   │   └── RegistroMortalidadAnormal.java
│   ├── exception/mortalidad/
│   │   ├── GalponNoProductivoException.java
│   │   └── MortalidadAnormalInvalidaException.java
│   └── port/out/mortalidad/
│       ├── GenerarAlertaSanitariaPort.java
│       ├── RegistroMortalidadAnormalRepositoryPort.java
│       └── UmbralMortalidadQueryPort.java
├── application/mortalidad/
│   ├── RegistrarMortalidadAnormalCommand.java
│   ├── RegistrarMortalidadAnormalUseCase.java
│   └── result/RegistroMortalidadAnormalResult.java
└── infrastructure/
    ├── adapter/in/rest/mortalidad/
    │   ├── MortalidadAnormalController.java
    │   ├── dto/
    │   │   ├── RegistrarMortalidadAnormalRequest.java
    │   │   └── RegistroMortalidadAnormalResponse.java
    │   └── mapper/MortalidadAnormalRestMapper.java
    ├── adapter/out/persistence/mortalidad/RegistroMortalidadAnormalPersistenceAdapter.java
    └── config/MortalidadAnormalBeanConfiguration.java

src/main/resources/db/migration/
└── V__crear_registros_mortalidad_anormal.sql

src/test/java/com/avicontrol/
├── domain/model/mortalidad/RegistroMortalidadAnormalTest.java
├── application/mortalidad/RegistrarMortalidadAnormalUseCaseTest.java
└── infrastructure/adapter/in/rest/mortalidad/MortalidadAnormalControllerTest.java
```

**Structure Decision**: el reporte anormal es un agregado local e inmutable. El puerto de alerta desacopla el Plan 018; la población, lote y estado se verifican en la fuente vigente antes de guardar.

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Confirmar umbrales, niveles de severidad y el contrato de generación de alertas.
- [ ] T002 Crear migración con porcentaje, severidad, referencias y usuario responsable.
- [ ] T003 Preparar fixtures para galpón productivo, estado no productivo, lote ausente y mortalidad de 3 %.

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T004 Crear `RegistroMortalidadAnormal` y calcular porcentaje `muertes / población × 100` con precisión decimal.
- [ ] T005 Validar causa probable, cantidad, fecha y condiciones operativas del galpón.
- [ ] T006 Mapear porcentaje contra umbral para asignar `NivelSeveridadMortalidad`.
- [ ] T007 Coordinar persistencia y generación de alerta mediante transacción, outbox o reintento idempotente.

**Checkpoint**: no se conserva un reporte sin una alerta trazable ni una alerta sin reporte confirmado.

## Phase 3: User Story 1 - Registrar mortalidad anormal y alerta (Priority: P1)

**Goal**: documentar el evento crítico y activar inmediatamente la alerta sanitaria.

**Independent Test**: 18 muertes de una población de 5.000 producen 0.36 %, severidad correspondiente y una alerta; cantidad superior a población no crea ningún recurso.

- [ ] T008 [P] [US1] Probar cálculos, umbral, validaciones de estado y generación única de alerta.
- [ ] T009 [US1] Implementar `RegistrarMortalidadAnormalUseCase` consultando galpón y lote vigentes.
- [ ] T010 [US1] Persistir el reporte y enviar al puerto de alerta con reporte, lote, galpón y severidad.

## Phase 4: User Story 2 - Calcular porcentaje y severidad (Priority: P2)

**Goal**: entregar datos objetivos de impacto para priorizar la respuesta sanitaria.

**Independent Test**: 150 muertes sobre 5.000 aves resulta en 3.0 % y el nivel de severidad definido para ese rango.

- [ ] T011 [P] [US2] Probar límites exactos del umbral y redondeo mostrado.
- [ ] T012 [US2] Incluir porcentaje y severidad inmutables en la respuesta y alerta.

## Endpoints

| Método y ruta | Roles | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `POST /api/galpones/{galponId}/mortalidad-anormal` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` | `RegistrarMortalidadAnormalRequest` | `201 Created` con reporte y alerta |

```json
{
  "fechaHoraEvento": "2026-11-05T08:00:00-05:00",
  "cantidad": 18,
  "causaProbable": "PROBLEMAS_DIGESTIVOS",
  "observaciones": "Aumento súbito"
}
```

```json
{
  "id": "b07f5fd4-3b77-4e9a-a4eb-5d54f907c8a4",
  "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "porcentajeMortalidad": 0.36,
  "severidad": "ALTA",
  "alertaId": "d290f1ee-6c54-4b01-90e6-d701748f0851"
}
```

**Errores**: `400` datos inválidos; `401` no autenticado; `403` sin rol; `404` galpón o lote ausente; `409` estado no productivo; `503` Módulo 1 o alertas no disponible.

## Polish & Cross-Cutting Concerns

- [ ] T013 Documentar fórmula, severidades e inmutabilidad.
- [ ] T014 Probar idempotencia y fallo de alerta sin perder trazabilidad.
- [ ] T015 Ejecutar pruebas y `git diff --check`.

## Dependencies & Execution Order

- **Setup** confirma umbrales y contrato del Plan 018.
- **Foundational** bloquea ambas historias.
- **US1** registra y alerta; **US2** provee cálculo que US1 usa.

## Notes

- La actualización de población viva se gestiona por el Plan 019 y no debe duplicarse aquí.
- El Plan 018 recibe el reporte; su visualización, asignación y resolución quedan fuera de este plan.
