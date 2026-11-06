# Implementation Plan: Consultar inventario

**Date**: 05/11/2026
**Spec**: [023-ConsultarInventario.md](../specs/023-ConsultarInventario.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Mostrar al administrador el inventario consolidado de alimentos y medicamentos, el resumen de ocupación de bodega y la cobertura frente a demanda proyectada. Todas las operaciones son lecturas consistentes de movimientos confirmados: no crean recepciones, movimientos, reservas ni ajustes.

## Technical Context

**Performance Goals**: mostrar ambas secciones en dos segundos.
**Constraints**: exclusivo de `ROLE_ADMINISTRADOR`; excluye saldos vencidos o anulados; alimentos se consolidan por nombre normalizado y tipo; medicamentos nunca mezclan unidades incompatibles.
**Scale/Scope**: inventario, resumen y cobertura; no registra recepciones ni descuenta existencias.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 023-ConsultarInventario.md
└── plan/
    └── 023-ConsultarInventario.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/model/inventario/
│   ├── CoberturaRequerimientoAlimento.java
│   ├── EstadoCoberturaAlimento.java
│   ├── ExistenciaAlimento.java
│   └── ExistenciaMedicamento.java
├── domain/port/out/inventario/
│   ├── DemandaAlimentoQueryPort.java
│   └── InventarioMovimientoQueryPort.java
├── application/inventario/
│   ├── ConsultarCoberturaAlimentoUseCase.java
│   ├── ConsultarInventarioUseCase.java
│   ├── ConsultarResumenInventarioUseCase.java
│   └── result/
│       ├── InventarioResult.java
│       └── ResumenInventarioResult.java
└── infrastructure/adapter/in/rest/inventario/
    ├── InventarioController.java
    ├── dto/
    │   ├── CoberturaAlimentoResponse.java
    │   ├── InventarioResponse.java
    │   └── ResumenInventarioResponse.java
    └── mapper/InventarioRestMapper.java

src/test/java/com/avicontrol/
├── domain/model/inventario/ExistenciaAlimentoTest.java
├── application/inventario/ConsultarInventarioUseCaseTest.java
└── infrastructure/adapter/in/rest/inventario/InventarioControllerTest.java
```

**Structure Decision**: son modelos de lectura; no hay repositorio ni migración para existencias consolidadas. Los puertos calculan una vista consistente desde recepciones y movimientos, y consultan demanda del Plan 022.

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Confirmar la vista consistente de movimientos y saldos por recepción.
- [ ] T002 Acordar la consulta de demanda consolidada con el Plan 022.
- [ ] T003 Preparar fixtures con vencidos, anulados, saldo cero, presentaciones distintas y los cuatro estados de cobertura.

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T004 Consolidar alimentos por nombre normalizado y tipo, en kilogramos, conservando próximo vencimiento y recepciones activas.
- [ ] T005 Consolidar medicamentos sólo por combinación compatible de medicamento, presentación, contenido y unidad.
- [ ] T006 Implementar estados `CUMPLE`, `COBERTURA_PARCIAL`, `SIN_COBERTURA` y `SIN_REQUERIMIENTO`.
- [ ] T007 Garantizar que cada respuesta procede de un único corte consistente y no invoca puertos de escritura.

**Checkpoint**: vencidos/anulados no aparecen como disponibles y ninguna lectura altera el inventario.

## Phase 3: User Story 1 - Consultar existencias (Priority: P1)

**Goal**: presentar alimentos y medicamentos consolidados con sus unidades correctas.

**Independent Test**: dos recepciones del mismo alimento forman una fila con suma en kg; un consumo disminuye el saldo sin modificar historia; un medicamento incompatible no se combina.

- [ ] T008 [P] [US1] Probar agrupación, saldo cero, vencidos, anulados y unidades incompatibles.
- [ ] T009 [US1] Implementar `ConsultarInventarioUseCase`.

## Phase 4: User Story 2 - Consultar resumen de bodega (Priority: P2)

**Goal**: mostrar porcentaje de ocupación y cinco recepciones de alimento más recientes.

**Independent Test**: más de cinco recepciones devuelve sólo cinco en orden descendente y el porcentaje usa unidades compatibles.

- [ ] T010 [P] [US2] Probar ocupación, lista vacía y límite de cinco.
- [ ] T011 [US2] Implementar `ConsultarResumenInventarioUseCase`.

## Phase 5: User Story 3 - Consultar cobertura de alimento (Priority: P1)

**Goal**: comparar demanda proyectada frente a stock sin reservarlo.

**Independent Test**: stock mayor/igual, menor/mayor a cero, cero y demanda cero devuelven respectivamente los cuatro estados definidos.

- [ ] T012 [P] [US3] Probar los cuatro estados y detalle de requerimiento.
- [ ] T013 [US3] Implementar `ConsultarCoberturaAlimentoUseCase`.

## Endpoints

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `GET /api/inventario` | `ROLE_ADMINISTRADOR` | Sin cuerpo | `200 OK` con alimentos y medicamentos |
| `GET /api/inventario/resumen` | `ROLE_ADMINISTRADOR` | Sin cuerpo | `200 OK` con ocupación y cinco recepciones |
| `GET /api/inventario/alimentos/{alimentoId}/requerimiento` | `ROLE_ADMINISTRADOR` | Sin cuerpo | `200 OK` con stock, demanda y cobertura |

```json
{
  "alimentos": [
    {
      "alimentoId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "etapaCrianza": "INICIO",
      "stockActualKg": 1000,
      "estadoCobertura": "COBERTURA_PARCIAL"
    }
  ],
  "medicamentos": []
}
```

```json
{
  "alimentoId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "stockActualKg": 1000,
  "demandaKg": 1250,
  "estado": "COBERTURA_PARCIAL"
}
```

**Errores**: `400` filtro o UUID inválido; `401` no autenticado; `403` sin rol; `404` alimento inexistente; `409` unidad incompatible; `503` movimientos o demanda no disponibles.

## Polish & Cross-Cutting Concerns

- [ ] T014 Documentar normalización de nombres, FEFO y que el detalle es sólo lectura.
- [ ] T015 Ejecutar pruebas, OpenAPI y `git diff --check`.

## Dependencies & Execution Order

- **Foundational** bloquea las tres historias.
- **US1** consolida existencias; **US2** obtiene el resumen; **US3** añade demanda del Plan 022.

## Notes

- Los bultos de pesos diferentes se conservan por recepción; la tabla consolidada de alimentos suma kilogramos.
- Abrir o cerrar el detalle de cobertura no reserva ni descuenta stock.
