# Implementation Plan: Ajustar plan nutricional por etapa

**Date**: 05/11/2026
**Spec**: [021-AjustarPlanNutricionalPorEtapa.md](../specs/021-AjustarPlanNutricionalPorEtapa.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Gestionar plantillas maestras y planes desacoplados por lote/galpón; seleccionar alimentos compatibles para etapas futuras y ajustar duración de la etapa activa con trazabilidad. Las etapas completadas y los insumos/costos de la activa son inmutables; los cambios recalculan la cascada y la salida proyectada sin reservar ni descontar inventario.

## Technical Context

**Performance Goals**: guardar ajustes y recalcular el calendario de forma consistente en un segundo.
**Constraints**: `ROLE_NUTRICIONISTA`; un predeterminado por tipo de ave; rangos continuos; ración positiva; sin movimientos de inventario.
**Scale/Scope**: plantillas, instancias, sustitución, alimentos y ajustes auditados; no despacho ni consumo físico.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 021-AjustarPlanNutricionalPorEtapa.md
└── plan/
    └── 021-AjustarPlanNutricionalPorEtapa.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/model/nutricion/
│   ├── CalendarioEtapaGalpon.java
│   ├── EtapaCrianza.java
│   ├── HistorialAjusteEtapa.java
│   ├── PlanNutricionalGalpon.java
│   └── PlanNutricionalPlantilla.java
├── domain/port/out/nutricion/
│   ├── AlimentoCompatibleQueryPort.java
│   ├── PlanNutricionalGalponRepositoryPort.java
│   └── PlanNutricionalPlantillaRepositoryPort.java
├── application/nutricion/
│   ├── AjustarDuracionEtapaUseCase.java
│   ├── AsignarPlanNutricionalUseCase.java
│   ├── CrearPlantillaPlanNutricionalUseCase.java
│   ├── SustituirPlanNutricionalUseCase.java
│   └── result/PlanNutricionalResult.java
└── infrastructure/adapter/in/rest/nutricion/
    ├── PlanNutricionalGalponController.java
    ├── PlanNutricionalPlantillaController.java
    ├── dto/
    │   ├── AjustarDuracionEtapaRequest.java
    │   ├── CrearPlantillaPlanNutricionalRequest.java
    │   └── PlanNutricionalResponse.java
    └── mapper/PlanNutricionalRestMapper.java

src/main/resources/db/migration/
└── V__crear_planes_nutricionales.sql

src/test/java/com/avicontrol/
├── domain/model/nutricion/PlanNutricionalGalponTest.java
├── application/nutricion/AjustarDuracionEtapaUseCaseTest.java
└── infrastructure/adapter/in/rest/nutricion/PlanNutricionalControllerTest.java
```

**Structure Decision**: la plantilla es catálogo y la instancia por lote es independiente. La etapa activa conserva alimento, ración y costo; ajustes se anexan al historial y sólo cambian duración/cascada.

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Confirmar tipos de ave, catálogo de alimentos, stock de sólo lectura y sincronización con Plan 022.
- [ ] T002 Crear migraciones para plantilla, instancia, etapas e historial de ajustes.
- [ ] T003 Definir enums, estados y pruebas de etapas omitidas, activas y completadas.

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T004 Validar rangos contiguos, ración positiva y unicidad de predeterminado por tipo de ave.
- [ ] T005 Implementar reglas de inmutabilidad para etapa completada y alimento/costo de etapa activa.
- [ ] T006 Implementar recálculo en cascada y actualización de fecha proyectada de salida.
- [ ] T007 Persistir cada ajuste con motivo, justificación, responsable y valores antes/después.

**Checkpoint**: el dominio no permite solapamientos ni cambios históricos, y nunca descuenta inventario.

## Phase 3: User Story 1 - Gestionar plantillas maestras (Priority: P1)

**Goal**: crear y editar curvas estándar sin alterar instancias activas.

**Independent Test**: tres etapas 1-7, 8-21 y 22-45 se aceptan; una etapa solapada o ración cero se rechaza; otro predeterminado se desmarca.

- [ ] T008 [P] [US1] Probar continuidad, ración y predeterminado único.
- [ ] T009 [US1] Implementar creación, consulta y edición de plantillas.

## Phase 4: User Story 2 - Asignar o sustituir plan (Priority: P1)

**Goal**: crear instancia al alojar lote y sustituir sólo etapas futuras.

**Independent Test**: ingreso en día 14 omite Pre-inicio; sustitución en día 25 conserva etapas concluidas y bloqueos de activa.

- [ ] T010 [P] [US2] Probar asignación automática, ausencia de predeterminado y sustitución segura.
- [ ] T011 [US2] Implementar asignación y sustitución por lote activo.

## Phase 5: User Story 3 - Seleccionar alimento compatible (Priority: P1)

**Goal**: configurar alimento para etapa no iniciada, con stock informativo y recálculo opcional de duración.

**Independent Test**: alimento con duración recomendada de diez días reajusta una etapa futura; sin stock se permite con alerta; en etapa activa se rechaza.

- [ ] T012 [P] [US3] Probar compatibilidad, presentaciones, vencidos, sin stock y bloqueo activo.
- [ ] T013 [US3] Implementar consulta y selección de alimento futuro.

## Phase 6: User Story 4 - Ajustar duración activa (Priority: P1)

**Goal**: ampliar o acortar días con justificación, conservando alimento y desplazando las etapas siguientes.

**Independent Test**: extender Inicio de 14 a 19 días desplaza Engorde cinco días, actualiza fecha de salida y añade una entrada de historial.

- [ ] T014 [P] [US4] Probar cascada, ajustes sucesivos, justificación y etapa completada.
- [ ] T015 [US4] Implementar ajuste, auditoría y notificación/recalculo al Plan 022.

## Endpoints

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `POST /api/nutricion/plantillas` | `ROLE_NUTRICIONISTA` | plantilla con etapas | `201 Created` |
| `PUT /api/nutricion/plantillas/{id}` | `ROLE_NUTRICIONISTA` | plantilla con etapas | `200 OK` |
| `GET /api/galpones/{galponId}/plan-nutricional` | nutricionista, administrador | sin cuerpo | `200 OK` |
| `PUT /api/galpones/{galponId}/plan-nutricional/sustituir` | `ROLE_NUTRICIONISTA` | nuevo plan | `200 OK` |
| `GET /api/nutricion/alimentos-compatibles` | nutricionista, administrador | filtros | `200 OK` |
| `PUT /api/galpones/{galponId}/plan-nutricional/etapas/{etapaId}/alimento` | `ROLE_NUTRICIONISTA` | alimento | `200 OK` |
| `PUT /api/galpones/{galponId}/plan-nutricional/etapas/{etapaId}/ajuste-duracion` | `ROLE_NUTRICIONISTA` | ajuste | `200 OK` |

```json
{
  "nuevoDiaFin": 28,
  "motivo": "BAJO_PESO",
  "justificacion": "Extender recuperación"
}
```

**Errores**: `400` ración, rangos o justificación inválidos; `401` no autenticado; `403` sin rol; `404` plan o etapa; `409` etapa activa/completada incompatible; `422` calendario no contiguo.

## Polish & Cross-Cutting Concerns

- [ ] T016 Documentar fórmulas de demanda y que no existen reservas ni movimientos de inventario.
- [ ] T017 Ejecutar pruebas de dominio, HTTP y `git diff --check`.

## Dependencies & Execution Order

- **Foundational** bloquea las cuatro historias.
- **US1** provee plantillas; **US2** crea instancias; **US3** configura futuras; **US4** ajusta la activa.

## Notes

- Sin predeterminado, el lote queda pendiente de asignación y demanda cero hasta configuración manual.
- El Plan 022 consume proyecciones e historial, sin permitir reescribir costos bloqueados.
