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
│   ├── ExistenciaAlimento.java
│   ├── ExistenciaMedicamento.java
│   └── InventarioActual.java
├── domain/port/out/inventario/
│   ├── DemandaAlimentoQueryPort.java
│   ├── InventarioMovimientoQueryPort.java
│   └── ResumenInventarioQueryPort.java
├── application/inventario/
│   ├── ConsultarCoberturaAlimentoUseCase.java
│   ├── ConsultarInventarioUseCase.java
│   └── ConsultarResumenInventarioUseCase.java
└── infrastructure/
    ├── adapter/in/rest/inventario/InventarioController.java
    ├── adapter/out/persistence/inventario/
    │   ├── InventarioMovimientoQueryAdapter.java
    │   └── ResumenInventarioQueryAdapter.java
    └── adapter/out/integration/inventario/DemandaAlimentoQueryAdapter.java

src/main/resources/db/migration/V8__permitir_movimientos_repetidos.sql

src/test/java/com/avicontrol/
├── application/alimentacion/NutricionInventarioIntegrationTest.java
├── application/inventario/ConsultarCoberturaAlimentoUseCaseTest.java
├── application/inventario/ConsultarResumenInventarioUseCaseTest.java
└── infrastructure/adapter/in/rest/inventario/InventarioNutricionControllerTest.java
```

**Structure Decision**: son modelos de lectura; no hay repositorio ni migración para existencias consolidadas. Los puertos calculan una vista consistente desde recepciones y movimientos, y consultan demanda del Plan 022.

La migración V8 elimina la unicidad antigua de tipo de movimiento por recepción para permitir más de una salida o consumo confirmado contra la misma recepción. No crea una tabla de existencias derivadas.

La capacidad y ocupación física de la bodega se reciben de la fuente de bodega mediante `AVICONTROL_BODEGA_CAPACIDAD_KG` y `AVICONTROL_BODEGA_OCUPACION_KG` en la revisión local. Si alguna no está disponible, `porcentajeOcupacion` es `null`; no se presenta una ocupación parcial de alimento como ocupación total de bodega. La integración con la fuente compartida sustituirá esta configuración.

## Phase 1: Setup (Shared Infrastructure)

- [x] T001 Confirmar la vista consistente de movimientos y saldos por recepción.
- [x] T002 Acordar la consulta de demanda consolidada con el Plan 022.
- [x] T003 Preparar fixtures con vencidos, anulados, saldo cero, presentaciones distintas y los cuatro estados de cobertura.

## Phase 2: Foundational (Blocking Prerequisites)

- [x] T004 Consolidar alimentos por nombre normalizado y tipo, en kilogramos, conservando próximo vencimiento y recepciones activas.
- [x] T005 Consolidar medicamentos sólo por combinación compatible de medicamento, presentación, contenido y unidad.
- [x] T006 Implementar estados `CUMPLE`, `COBERTURA_PARCIAL`, `SIN_COBERTURA` y `SIN_REQUERIMIENTO`.
- [x] T007 Garantizar que cada respuesta procede de un único corte consistente y no invoca puertos de escritura.

**Checkpoint**: vencidos/anulados no aparecen como disponibles y ninguna lectura altera el inventario.

## Phase 3: User Story 1 - Consultar existencias (Priority: P1)

**Goal**: presentar alimentos y medicamentos consolidados con sus unidades correctas y tarjetas de presentaciones de medicamento.

**Independent Test**: dos recepciones del mismo alimento forman una fila con suma en kg; un consumo disminuye el saldo sin modificar historia; un medicamento incompatible no se combina.

- [x] T008 [P] [US1] Probar agrupación, saldo cero, vencidos, anulados y unidades incompatibles.
- [x] T009 [US1] Implementar `ConsultarInventarioUseCase`.

## Phase 4: User Story 2 - Consultar resumen de bodega (Priority: P2)

**Goal**: mostrar porcentaje de ocupación y cinco recepciones de alimento más recientes.

**Independent Test**: más de cinco recepciones devuelve sólo cinco en orden descendente y el porcentaje usa unidades compatibles.

- [x] T010 [P] [US2] Probar ocupación, lista vacía y límite de cinco.
- [x] T011 [US2] Implementar `ConsultarResumenInventarioUseCase`.

## Phase 5: User Story 3 - Consultar cobertura de alimento (Priority: P1)

**Goal**: comparar demanda proyectada frente a stock sin reservarlo.

**Independent Test**: stock mayor/igual, menor/mayor a cero, cero y demanda cero devuelven respectivamente los cuatro estados definidos.

- [x] T012 [P] [US3] Probar los cuatro estados y detalle de requerimiento.
- [x] T013 [US3] Implementar `ConsultarCoberturaAlimentoUseCase`.

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
      "alimento": "Alimento Inicio Broiler",
      "etapaCrianza": "INICIO",
      "recepcionesActivas": 2,
      "proximoVencimiento": "2027-01-15",
      "stockActualKg": 1000,
      "demandaKg": 1250,
      "estadoCobertura": "COBERTURA_PARCIAL"
    }
  ],
  "medicamentos": [
    {
      "medicamentoId": "550e8400-e29b-41d4-a716-446655440000",
      "medicamento": "Amprolio",
      "presentacion": "Frasco",
      "cantidad": 10,
      "contenidoPorPresentacion": 100,
      "unidad": "MILILITRO",
      "stockActual": 1000
    }
  ],
  "tarjetasAlimentoKg": {
    "PRE_INICIO": 0,
    "INICIO": 1000,
    "ENGORDE": 0,
    "TOTAL": 1000
  },
  "tarjetasMedicamento": {
    "FRASCOS": 10,
    "BOLSAS": 0,
    "CAJAS": 0,
    "TOTAL": 10
  }
}
```

Respuesta de `GET /api/inventario/resumen`:

```json
{
  "capacidadMaximaKg": 10000,
  "ocupacionActualKg": 2500,
  "porcentajeOcupacion": 25.00,
  "recepcionesAlimentoRecientes": [
    {
      "codigoLote": "L-2026-11",
      "alimento": "Alimento Inicio Broiler",
      "cantidadBultos": 25,
      "precioPorBulto": 66000,
      "pesoPorBultoKg": 40,
      "fechaIngreso": "2026-11-05"
    }
  ]
}
```

```json
{
  "alimentoId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "alimento": "Alimento Inicio Broiler",
  "etapaCrianza": "INICIO",
  "stockActualKg": 1000,
  "demandaKg": 1250,
  "estado": "COBERTURA_PARCIAL"
}
```

**Errores**: `400` filtro o UUID inválido; `401` no autenticado; `403` sin rol; `404` alimento inexistente; `409` unidad incompatible; `503` movimientos o demanda no disponibles.

## Polish & Cross-Cutting Concerns

- [x] T014 Documentar normalización de nombres, FEFO y que el detalle es sólo lectura.
- [x] T015 Ejecutar pruebas, OpenAPI y `git diff --check`.

## Dependencies & Execution Order

- **Foundational** bloquea las tres historias.
- **US1** consolida existencias; **US2** obtiene el resumen; **US3** añade demanda del Plan 022.

## Notes

- Los bultos de pesos diferentes se conservan por recepción; la tabla consolidada de alimentos suma kilogramos.
- El nombre de alimento se normaliza quitando espacios sobrantes y unificando mayúsculas; las salidas y consumos se asignan por recepción según FEFO en sus operaciones de escritura. Esta consulta lee sus saldos confirmados y no crea movimientos.
- El saldo de medicamentos se calcula en la unidad base y se convierte a la unidad declarada por la presentación para `stockActual`; `cantidad` divide el saldo base por el contenido base de cada presentación.
- Las tarjetas de medicamentos suman presentaciones equivalentes por frasco, bolsa y caja; `TOTAL` incluye también otras presentaciones. No se suman miligramos con mililitros ni con unidades.
- Abrir o cerrar el detalle de cobertura no reserva ni descuenta stock.
- Verificación del bloque 021–023 (12/11/2026): 181 pruebas de la suite en H2 y 9 pruebas integradas y HTTP en PostgreSQL aislado, sin fallos; rutas presentes en OpenAPI, `spotlessJavaCheck` y `git diff --check` correctos.
