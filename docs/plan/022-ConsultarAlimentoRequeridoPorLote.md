# Implementation Plan: Consultar alimento requerido por lote

**Date**: 05/11/2026
**Spec**: [022-ConsultarAlimentoRequeridoPorLote.md](../specs/022-ConsultarAlimentoRequeridoPorLote.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Persistir la proyección de alimento de cada etapa del lote al activarse, consultar sus etapas separadas para Finanzas y calcular el balance consolidado de abastecimiento. La proyección conserva población, alimento, cuota y costo de referencia capturados; las prórrogas sólo actualizan duración y kilogramos con historial inmutable.

## Technical Context

**Performance Goals**: generar una proyección en dos segundos y consultar lote o balance en un segundo.
**Constraints**: las etapas completadas son inmutables; costo capturado no cambia por compras posteriores; costo ausente es `null`, nunca cero; no se reservan existencias.
**Scale/Scope**: proyección por etapa, ajuste auditado, consulta financiera y balance de compras; no calcula liquidación real ni consume alimento físico.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 022-ConsultarAlimentoRequeridoPorLote.md
└── plan/
    └── 022-ConsultarAlimentoRequeridoPorLote.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── application/alimentacion/ProyeccionAlimentoUseCase.java
├── application/alimentacion/port/ProyeccionAlimentoRepositoryPort.java
├── infrastructure/adapter/in/rest/alimentacion/ProyeccionAlimentoController.java
├── infrastructure/adapter/out/persistence/alimentacion/ProyeccionAlimentoPersistenceAdapter.java
└── infrastructure/adapter/out/integration/inventario/DemandaAlimentoQueryAdapter.java

src/main/resources/db/migration/
└── V7__crear_planes_y_proyecciones_alimento.sql

src/test/java/com/avicontrol/
└── application/alimentacion/NutricionInventarioIntegrationTest.java
```

**Structure Decision**: cada proyección pertenece a una etapa y lote, nunca al galpón como unidad financiera. Inventario sólo proporciona stock y costo de referencia de lectura; el costo real sigue siendo responsabilidad del Módulo 3 a partir de consumos reales.

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Confirmar eventos de activación y prórroga del Plan 021.
- [ ] T002 Definir consulta de promedio ponderado de saldos elegibles a la fecha de activación.
- [ ] T003 Crear migración para proyección, estado, costo nullable y historial de ajustes.

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T004 Crear `ProyeccionAlimentoEtapa` con fórmula población × cuota × días efectivos.
- [ ] T005 Capturar alimento, población, cuota, presentación y costo como snapshot inmutable al activar etapa.
- [ ] T006 Calcular promedio ponderado half-up a dos decimales; si no hay stock elegible guardar `null` y advertencia.
- [ ] T007 Implementar historial append-only y bloqueo de etapas completadas.

**Checkpoint**: proyecciones de distintas etapas permanecen separadas y los cambios futuros de precio no alteran snapshots.

## Phase 3: User Story 1 - Consultar requerimiento por lote (Priority: P1)

**Goal**: entregar a Finanzas proyecciones individualizadas por etapa, sin sumatoria de alimentos heterogéneos.

**Independent Test**: un lote cerrado devuelve Pre-inicio, Inicio y Engorde con kg y costos de referencia separados.

- [ ] T008 [P] [US1] Probar fórmula, etapa activa/finalizada, costo nulo y respuestas por etapa.
- [ ] T009 [US1] Implementar consulta de requerimiento y DTO financiero.

## Phase 4: User Story 2 - Generar y ajustar proyección (Priority: P2)

**Goal**: crear la proyección al activar etapa y actualizar sólo días/kg ante prórroga válida.

**Independent Test**: una prórroga de cuatro días cambia 14 a 18 días y conserva alimento, población y costo, agregando historial.

- [ ] T010 [P] [US2] Probar transición de etapa, costo capturado, prórrogas sucesivas e inmutabilidad completada.
- [ ] T011 [US2] Implementar casos de uso internos de registro y ajuste desde Plan 021.
- [ ] T012 [US2] Permitir completar una vez el costo nulo mientras la etapa sigue activa y con auditoría de administrador.

## Phase 5: User Story 3 - Consultar balance de abastecimiento (Priority: P2)

**Goal**: agrupar demanda de etapas activas por alimento/presentación y compararla con stock vigente.

**Independent Test**: demanda de 11.942,50 kg frente a stock de 8.000 kg devuelve déficit de 3.942,50 kg y bultos sugeridos, sin escribir en inventario.

- [ ] T013 [P] [US3] Probar agrupación, déficit, stock vencido excluido y consulta de sólo lectura.
- [ ] T014 [US3] Implementar la consulta de balance en `ProyeccionAlimentoUseCase`.

## Endpoints

| Método y ruta | Roles | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `GET /api/lotes/{loteId}/alimento-requerido` | administrador, nutricionista, sistema Finanzas autorizado | sin cuerpo | `200 OK` con etapas separadas |
| `POST /api/proyecciones/{proyeccionId}/completar-costo` | `ROLE_ADMINISTRADOR` | costo y justificación | `200 OK` |
| `GET /api/administracion/balance-abastecimiento` | administrador, nutricionista | sin cuerpo | `200 OK` por alimento/presentación |

```json
{
  "codigoLote": "LDP-005",
  "loteId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "galponId": "cdb7993a-e208-4ea8-9f90-a611e3e82a4c",
  "estadoCiclo": "EN_PROGRESO",
  "fechaConsulta": "2026-11-05T10:30:00-05:00",
  "etapas": [
    {
      "etapaCrianza": "INICIO",
      "tipoAlimento": "INICIO",
      "alimentoComercial": "Alimento Inicio Broiler",
      "pesoNominalPorBultoKg": 40,
      "poblacionInicioEtapa": 10000,
      "cuotaKgAveDia": 0.0430,
      "estadoEtapa": "ACTIVA",
      "kilogramosProyectados": 6020.00,
      "costoUnitarioKg": 1650.00,
      "fuenteCosto": "PROMEDIO_PONDERADO_STOCK",
      "fechaCapturaCosto": "2026-11-05T08:00:00-05:00",
      "advertenciaCosto": null,
      "diasBase": 14,
      "diasProrroga": 0,
      "diasEfectivos": 14,
      "motivoProrroga": null,
      "justificacion": null,
      "historialAjustes": []
    }
  ]
}
```

Solicitud y respuesta de `completar-costo` (solo una vez en etapa activa):

```json
{
  "costoUnitarioKg": 1650.00,
  "justificacion": "Precio histórico de las recepciones vigentes al activar la etapa"
}
```

```json
{
  "proyeccionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "costoUnitarioKg": 1650.00,
  "fuenteCosto": "COMPLETADO_MANUALMENTE",
  "costoCompletadoManualmente": true
}
```

Respuesta de `balance-abastecimiento`:

```json
{
  "alimentos": [
    {
      "alimentoId": "550e8400-e29b-41d4-a716-446655440000",
      "pesoNominalPorBultoKg": 40,
      "demandaConsolidadaKg": 11942.50,
      "existenciaDisponibleKg": 8000,
      "balanceKg": -3942.50,
      "estado": "REABASTECIMIENTO_NECESARIO",
      "bultosSugeridos": 99
    }
  ]
}
```

**Errores**: `400` datos inválidos; `401` no autenticado; `403` sin rol; `404` lote o proyección ausente; `409` costo ya completado o etapa cerrada; `503` stock o precio no disponible.

## Polish & Cross-Cutting Concerns

- [ ] T015 Documentar costo de referencia frente a costo real y la ausencia de reservas.
- [ ] T016 Ejecutar pruebas de eventos, concurrencia, HTTP y `git diff --check`.

## Dependencies & Execution Order

- **Foundational** depende del Plan 021 y bloquea las tres historias.
- **US1** consulta proyecciones; **US2** las genera o ajusta; **US3** agrega sólo las activas.

## Notes

- La finalización de etapa fija su costo `null` o capturado para siempre.
- La liquidación final y costo real corresponden al Módulo 3.
- Si una etapa transcurrió completamente durante una caída y no se capturó el precio al activarse, la recuperación la registra con `costoUnitarioKg = null` y advertencia. No usa el precio vigente como si fuera histórico; una etapa ya cerrada no admite completar ese costo.
