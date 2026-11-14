# Implementation Plan: Gestionar suministro y consultar alimento diario por galpón

**Date**: 05/11/2026
**Spec**: [014-ConsultarAlimentoPorGalponPorDia.md](../specs/014-ConsultarAlimentoPorGalponPorDia.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Permitir consultar el requerimiento diario de alimento de un galpón, sus alertas y un resumen consolidado de galpones seleccionados; además, registrar suministros físicos confirmados. Las consultas son de sólo lectura y el suministro es una operación independiente, idempotente y atómica con la salida FEFO de Inventario.

## Technical Context

**Performance Goals**: consultas operativas en un segundo y suministros confirmados sin descuentos parciales.
**Constraints**: `ROLE_TRABAJADOR` o administrador autorizado; edad y etapa vigentes; disponibilidad excluye vencidos; suministro sólo para lote activo y alimento de etapa.
**Scale/Scope**: detalle diario, alertas, consolidado seleccionado y suministro real; no configura planes ni recepciona alimento.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 014-ConsultarAlimentoPorGalponPorDia.md
└── plan/
    └── 014-ConsultarAlimentoPorGalponPorDia.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/alimentacion/
│   │   ├── AlertaAlimentacion.java
│   │   ├── RequerimientoDiarioGalpon.java
│   │   └── SuministroDiarioAlimento.java
│   ├── exception/alimentacion/
│   │   ├── AlimentoNoCorrespondeEtapaException.java
│   │   ├── ExistenciasAlimentoInsuficientesException.java
│   │   ├── LoteActivoNoEncontradoException.java
│   │   └── SuministroDuplicadoException.java
│   ├── port/out/alimentacion/
│   │   ├── InventarioAlimentoPort.java
│   │   ├── ObservabilidadAlimentacionPort.java
│   │   ├── PlanNutricionalQueryPort.java
│   │   ├── SuministroDiarioQueryPort.java
│   │   └── SuministroDiarioRepositoryPort.java
│   └── port/out/sanitario/medicacion/GalponLoteQueryPort.java
├── application/
│   └── alimentacion/
│       ├── ConsultarAlimentoGalponDiaUseCase.java
│       ├── ConsultarResumenAlimentoDiarioUseCase.java
│       ├── RegistrarSuministroDiarioUseCase.java
│       └── result/
│           ├── AlimentoDiarioGalponResult.java
│           └── ResumenAlimentoDiarioResult.java
└── infrastructure/
    ├── adapter/in/rest/alimentacion/
    │   ├── AlimentacionDiariaController.java
    │   ├── dto/
    │   │   ├── AlimentoDiarioGalponResponse.java
    │   │   ├── RegistrarSuministroDiarioRequest.java
    │   │   ├── ResumenAlimentoDiarioResponse.java
    │   │   └── SuministroDiarioResponse.java
    │   └── mapper/AlimentacionDiariaRestMapper.java
    ├── adapter/out/persistence/alimentacion/
    │   ├── InventarioAlimentoFefoAdapter.java
    │   ├── ObservabilidadAlimentacionAdapter.java
    │   ├── PlanNutricionalDiarioQueryAdapter.java
    │   ├── ProyeccionAlimentoPersistenceAdapter.java
    │   └── SuministroDiarioPersistenceAdapter.java
    ├── adapter/out/persistence/inventario/
    │   ├── AjusteEntradaInventarioPersistenceAdapter.java
    │   └── InventarioMovimientoQueryAdapter.java
    ├── adapter/out/integration/modulo1/GalponLoteQueryAdapter.java
    └── config/AlimentacionDiariaBeanConfiguration.java

src/main/resources/db/migration/
└── V16__crear_suministros_diarios_alimento.sql

src/test/java/com/avicontrol/
├── domain/model/alimentacion/RequerimientoDiarioGalponTest.java
├── application/alimentacion/
│   ├── ConsultarAlimentoGalponDiaUseCaseTest.java
│   ├── ConsultarResumenAlimentoDiarioUseCaseTest.java
│   └── RegistrarSuministroDiarioUseCaseTest.java
├── infrastructure/adapter/in/rest/alimentacion/AlimentacionDiariaControllerTest.java
├── infrastructure/adapter/in/rest/recepcion/RecepcionAlimentoControllerTest.java
└── application/alimentacion/NutricionInventarioIntegrationTest.java
```

**Structure Decision**: requerimientos, alertas y resumen se calculan desde puertos de lectura. Se reutiliza `GalponLoteQueryPort` del Plan 015. El suministro es el único agregado local: conserva trazabilidad e idempotencia y registra cada porción FEFO en `detalles_suministro_alimento`. Ese detalle constituye la salida de inventario, porque el índice de `movimientos_inventario` no permite varias salidas para una misma recepción. La consulta de existencias lo descuenta y la edición de una recepción utilizada queda bloqueada.

## Phase 1: Setup (Shared Infrastructure)

- [x] T001 Confirmar contratos de galpón/lote activo, plan nutricional, stock compatible y salida FEFO de alimento.
- [x] T002 Crear migración de suministros con lote, galpón, alimento, trabajador, fecha de negocio y clave de idempotencia única.
- [x] T003 Definir `Clock` de negocio y fixtures de lote activo, sin aves, sin rango nutricional, stock vencido y stock insuficiente.

## Phase 2: Foundational (Blocking Prerequisites)

- [x] T004 Implementar cálculo de kg: población viva × ración en gramos / 1.000, y conversión a bultos por peso nominal.
- [x] T005 Implementar saldo `max(0, requerido - suministrado)` y alertas de déficit, pendiente y cambio de plan del día.
- [x] T006 Definir puertos de consulta estrictamente de lectura y salida de Inventario atómica con detalles FEFO.
- [x] T007 Proteger suministro con idempotencia y transacción/compensación ante fallos de Inventario.

**Checkpoint**: las consultas no escriben y un suministro nunca queda confirmado con una salida parcial.

## Phase 3: User Story 1 - Consultar requerimiento diario (Priority: P1)

**Goal**: mostrar lote activo, etapa, alimento, requerimiento, acumulado, saldo y disponibilidad.

**Independent Test**: 10.000 aves con ración de 120 g y bulto de 40 kg generan 1.200 kg y 30 bultos; con 400 kg suministrados, el saldo es 800 kg.

- [x] T008 [P] [US1] Probar edad, etapa, conversiones, lote ausente y población cero.
- [x] T009 [US1] Implementar `ConsultarAlimentoGalponDiaUseCase` y endpoint de detalle.

## Phase 4: User Story 2 - Identificar alertas (Priority: P2)

**Goal**: comunicar déficit de bodega, saldo pendiente y cambio nutricional del día.

**Independent Test**: disponibilidad de 600 kg frente a requerimiento de 1.200 kg devuelve alerta de stock insuficiente.

- [x] T010 [P] [US2] Probar alertas por déficit, pendiente y plan modificado.
- [x] T011 [US2] Incluir alertas calculadas en la respuesta de detalle sin efectos de escritura.

## Phase 5: User Story 3 - Consultar resumen consolidado (Priority: P2)

**Goal**: sumar requerimientos exclusivamente de los galpones seleccionados por el trabajador.

**Independent Test**: tres galpones seleccionados con 450, 430 y 370 kg devuelven 1.250 kg y las filas correspondientes; sin selección devuelve cero y lista vacía.

- [x] T012 [P] [US3] Probar consolidación, selección vacía y déficit de un tipo de alimento.
- [x] T013 [US3] Implementar `ConsultarResumenAlimentoDiarioUseCase`.

## Phase 6: User Story 4 - Registrar suministro diario (Priority: P1)

**Goal**: registrar alimento realmente entregado y descontarlo del inventario central.

**Independent Test**: un suministro parcial guarda lote, galpón, alimento, trabajador y fecha; tras salida FEFO confirmada, una nueva consulta actualiza acumulado y saldo. Sin stock, no guarda nada.

- [x] T014 [P] [US4] Probar alimento de etapa, FEFO de varias recepciones, idempotencia y reversión total.
- [x] T015 [US4] Implementar `RegistrarSuministroDiarioUseCase` y persistir sólo después de confirmar la salida completa.

## Endpoints

| Método y ruta | Roles | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `GET /api/galpones/{galponId}/alimento-diario?fecha=2026-11-06` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` | Sin cuerpo | `200 OK` con detalle y alertas |
| `GET /api/alimento-diario/resumen?galponIds={id1,id2}` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` | Sin cuerpo | `200 OK` con total y filas seleccionadas |
| `POST /api/galpones/{galponId}/suministros-alimento` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` | `RegistrarSuministroDiarioRequest` | `201 Created` con suministro confirmado |

```json
{
  "cantidadKg": 400,
  "fechaAplicacion": "2026-11-06",
  "idempotencyKey": "b07f5fd4-3b77-4e9a-a4eb-5d54f907c8a4"
}
```

```json
{
  "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "nombreGalpon": "Galpón 5",
  "aforoMaximo": 12000,
  "estadoGalpon": "PRODUCTIVA",
  "loteId": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
  "loteCodigo": "LDP-005",
  "poblacionViva": 10000,
  "edadDias": 18,
  "fecha": "2026-11-06",
  "estado": "CON_PLAN_NUTRICIONAL",
  "etapaCrianza": "INICIO",
  "tipoAlimento": "INICIO",
  "alimentoId": "3fa85f64-5717-4562-b3fc-2c963f66afa8",
  "nombreAlimento": "Inicio Premium",
  "racionGramosAve": 120,
  "pesoNominalBultoKg": 40,
  "requerimientoKg": 1200,
  "requerimientoBultos": 30,
  "suministradoKg": 400,
  "saldoKg": 800,
  "saldoBultos": 20,
  "disponibilidadKg": 1600,
  "disponibilidadBultos": 40,
  "alertas": [
    {
      "tipo": "SUMINISTRO_PENDIENTE",
      "mensaje": "Faltan 800 kg"
    }
  ]
}
```

Respuesta de `GET /api/alimento-diario/resumen?galponIds={id1,id2}`:

```json
{
  "fecha": "2026-11-06",
  "requerimientoTotalKg": 450,
  "requerimientoTotalBultos": 9,
  "estadoBodega": "BODEGA_CENTRAL_OK",
  "galpones": [
    {
      "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "nombreGalpon": "Galpón 5",
      "loteCodigo": "LDP-005",
      "tipoAlimento": "ENGORDE",
      "nombreAlimento": "Engorde Premium",
      "cuotaKg": 450,
      "cuotaBultos": 9,
      "estadoBodega": "SUFICIENTE"
    }
  ]
}
```

Respuesta de `POST /api/galpones/{galponId}/suministros-alimento`:

```json
{
  "id": "b07f5fd4-3b77-4e9a-a4eb-5d54f907c8a5",
  "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "loteId": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
  "alimentoId": "3fa85f64-5717-4562-b3fc-2c963f66afa8",
  "tipoAlimento": "INICIO",
  "cantidadKg": 400,
  "fechaAplicacion": "2026-11-06",
  "fechaRegistro": "2026-11-06T15:00:00Z",
  "trabajador": "trabajador",
  "idempotencyKey": "b07f5fd4-3b77-4e9a-a4eb-5d54f907c8a4",
  "detalles": [
    {
      "recepcionId": "3fa85f64-5717-4562-b3fc-2c963f66afa9",
      "cantidadKg": 400,
      "precioNetoKg": 10,
      "moneda": "COP"
    }
  ]
}
```

Un primer POST válido responde `201`; el reintento con los mismos datos, clave y trabajador responde `200` con el mismo suministro. Reutilizar la clave con datos distintos responde `409`.

**Errores**: `400` parámetros, cantidad o clave inválidos; `401` no autenticado; `403` sin rol; `404` galpón/lote ausente; `409` plan o alimento incompatible; `422` stock insuficiente; `503` dependencia no disponible.

## Polish & Cross-Cutting Concerns

- [x] T016 Documentar redondeo de bultos y semántica de fechas de negocio.
- [x] T017 Añadir métricas de déficit, suministros idempotentes y fallos de Inventario.
- [x] T018 Ejecutar pruebas y `git diff --check`.

## Dependencies & Execution Order

- **Setup y Foundational** bloquean las cuatro historias.
- **US1 y US2** comparten cálculo de requerimiento; **US3** agrega resultados de US1.
- **US4** utiliza la misma validación nutricional y añade escritura atómica.

## Notes

- El lote activo es el de fecha de ingreso más reciente que referencia el galpón.
- Los registros de suministro confirmados no se eliminan: una corrección futura debe ser un movimiento compensatorio trazable.
- `fecha` en las consultas y `fechaAplicacion` en el POST deben corresponder a la jornada actual del `Clock` de negocio (`America/Bogota`). Se evita presentar como histórico el cálculo basado en población viva y plan vigentes, porque el Módulo 1 provisional no conserva instantáneas diarias.
- Los bultos equivalentes se muestran con cuatro decimales y redondeo `HALF_UP`; no se redondea al bulto entero porque una entrega puede ser parcial.
- La salida FEFO y el suministro se guardan en una transacción local. La fuente provisional del Módulo 1 se bloquea por galpón al registrar para serializar reintentos concurrentes. Si Inventario o Módulo 1 pasan a servicios remotos, este contrato requiere reserva idempotente y coordinación explícita.
- Métricas: `avicontrol.alimentacion.consulta.deficit`, `avicontrol.alimentacion.suministro.reintentos` y `avicontrol.alimentacion.suministro.rechazos{motivo=existencias_insuficientes}`.
- Verificación local: `spotlessJavaCheck test` pasó con 93 pruebas y `git diff --check` no reportó errores. La migración se ejercitó con H2 en modo PostgreSQL; queda pendiente validarla en PostgreSQL real cuando esté disponible.
