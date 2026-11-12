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
src/main/java/com/avicontrol/application/mortalidad/RegistrarMortalidadAnormalCommand.java
src/main/java/com/avicontrol/application/mortalidad/RegistrarMortalidadAnormalUseCase.java
src/main/java/com/avicontrol/application/mortalidad/result/RegistroMortalidadAnormalResult.java
src/main/java/com/avicontrol/domain/model/mortalidad/NivelSeveridadMortalidad.java
src/main/java/com/avicontrol/domain/model/mortalidad/RegistroMortalidadAnormal.java
src/main/java/com/avicontrol/domain/port/out/mortalidad/GenerarAlertaSanitariaPort.java
src/main/java/com/avicontrol/domain/port/out/mortalidad/RegistroMortalidadAnormalRepositoryPort.java
src/main/java/com/avicontrol/infrastructure/adapter/in/rest/mortalidad/dto/RegistrarMortalidadAnormalRequest.java
src/main/java/com/avicontrol/infrastructure/adapter/in/rest/mortalidad/dto/RegistroMortalidadAnormalResponse.java
src/main/java/com/avicontrol/infrastructure/adapter/in/rest/mortalidad/mapper/MortalidadAnormalRestMapper.java
src/main/java/com/avicontrol/infrastructure/adapter/in/rest/mortalidad/MortalidadAnormalController.java
src/main/java/com/avicontrol/infrastructure/adapter/out/persistence/mortalidad/RegistroMortalidadAnormalPersistenceAdapter.java
src/main/java/com/avicontrol/infrastructure/config/MortalidadBeanConfiguration.java
src/main/resources/db/migration/V21__crear_mortalidad_anormal_y_alertas.sql
src/test/java/com/avicontrol/infrastructure/adapter/in/rest/mortalidad/MortalidadAlertaIntegrationTest.java
src/test/java/com/avicontrol/application/mortalidad/MortalidadAlertaRollbackTest.java
src/test/java/com/avicontrol/domain/model/mortalidad/RegistroMortalidadAnormalTest.java
```

**Structure Decision**: el reporte anormal es un agregado local e inmutable. El puerto de alerta desacopla el Plan 018; la población, lote y estado se verifican en la fuente vigente antes de guardar.

## Phase 1: Setup (Shared Infrastructure)

- [x] T001 Confirmar umbrales, niveles de severidad y el contrato de generación de alertas.
- [x] T002 Crear migración con porcentaje, severidad, referencias y usuario responsable.
- [x] T003 Preparar fixtures para galpón productivo, estado no productivo, lote ausente y mortalidad de 3 %.

## Phase 2: Foundational (Blocking Prerequisites)

- [x] T004 Crear `RegistroMortalidadAnormal` y calcular porcentaje `muertes / población × 100` con precisión decimal.
- [x] T005 Validar causa probable, cantidad, fecha y condiciones operativas del galpón.
- [x] T006 Mapear porcentaje contra umbral para asignar `NivelSeveridadMortalidad`.
- [x] T007 Coordinar persistencia y generación de alerta mediante transacción, outbox o reintento idempotente.

**Checkpoint**: no se conserva un reporte sin una alerta trazable ni una alerta sin reporte confirmado.

## Phase 3: User Story 1 - Registrar mortalidad anormal y alerta (Priority: P1)

**Goal**: documentar el evento crítico y activar inmediatamente la alerta sanitaria.

**Independent Test**: 18 muertes de una población de 5.000 producen 0.36 %, severidad correspondiente y una alerta; cantidad superior a población no crea ningún recurso.

- [x] T008 [P] [US1] Probar cálculos, umbral, validaciones de estado y generación única de alerta.
- [x] T009 [US1] Implementar `RegistrarMortalidadAnormalUseCase` consultando galpón y lote vigentes.
- [x] T010 [US1] Persistir el reporte y enviar al puerto de alerta con reporte, lote, galpón y severidad.

## Phase 4: User Story 2 - Calcular porcentaje y severidad (Priority: P2)

**Goal**: entregar datos objetivos de impacto para priorizar la respuesta sanitaria.

**Independent Test**: 150 muertes sobre 5.000 aves resulta en 3.0 % y el nivel de severidad definido para ese rango.

- [x] T011 [P] [US2] Probar límites exactos del umbral y redondeo mostrado.
- [x] T012 [US2] Incluir porcentaje y severidad inmutables en la respuesta y alerta.

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

La ruta explícita permite reportar una causa sospechosa observada por el trabajador aun si la cantidad no supera el umbral diario de 0.10 %. En ese caso se conserva el porcentaje calculado y se asigna severidad `ALTA`. Este reporte no descuenta población: el descuento corresponde al registro de bajas del Plan 016.

## Polish & Cross-Cutting Concerns

- [x] T013 Documentar fórmula, severidades e inmutabilidad.
- [x] T014 Probar idempotencia y fallo de alerta sin perder trazabilidad.
- [x] T015 Ejecutar pruebas y `git diff --check`.

## Dependencies & Execution Order

- **Setup** confirma umbrales y contrato del Plan 018.
- **Foundational** bloquea ambas historias.
- **US1** registra y alerta; **US2** provee cálculo que US1 usa.

## Notes

- El reporte y la alerta se guardan en una sola transacción local. Si falla la generación de la alerta, se revierte el reporte completo y la misma clave puede reintentarse sin duplicar registros.
- En `local` y `test`, la fila del galpón se bloquea antes de consultar una clave de idempotencia para serializar reportes simultáneos. La integración remota con Módulo 1 requerirá un mecanismo equivalente.
- Verificado el 12/11/2026 con límites de severidad, reintentos simultáneos, rechazo sin lote y reversión del reporte si falla la alerta. La suite completa pasó con 173 pruebas.

- La actualización de población viva se gestiona por el Plan 019 y no debe duplicarse aquí.
- El Plan 018 recibe el reporte; su visualización, asignación y resolución quedan fuera de este plan.
