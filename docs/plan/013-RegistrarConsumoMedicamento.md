# Implementation Plan: Registrar consumo de medicamento

**Date**: 05/11/2026
**Spec**: [013-RegistrarConsumoMedicamento.md](../specs/013-RegistrarConsumoMedicamento.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Permitir que un veterinario registre la cantidad real aplicada para un diagnóstico con medicación. El sistema obtiene medicamento, lote y galpón desde el diagnóstico, normaliza la cantidad, solicita al inventario una salida FEFO atómica y persiste el consumo con un detalle por cada recepción y su precio histórico para el Módulo 3.

## Technical Context

**Performance Goals**: confirmar descuento y disponibilidad de datos para Módulo 3 en dos segundos para el 95 % de consumos.
**Constraints**: `ROLE_VETERINARIO`; sólo unidades compatibles; los datos de lote, galpón y medicamento no llegan del cliente; sin descuentos parciales.
**Scale/Scope**: un consumo asociado a un diagnóstico, potencialmente distribuido en varias recepciones; no prescribe, diagnostica, recibe ni liquida.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 013-RegistrarConsumoMedicamento.md
└── plan/
    └── 013-RegistrarConsumoMedicamento.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/medicamento/
│   │   ├── ConsumoMedicamento.java
│   │   ├── DetalleConsumoMedicamento.java
│   │   └── UnidadConsumoMedicamento.java
│   ├── exception/medicamento/
│   │   ├── CantidadMedicamentoInvalidaException.java
│   │   ├── ConsumoMedicamentoNoEncontradoException.java
│   │   ├── DiagnosticoConsumoNoEncontradoException.java
│   │   ├── ExistenciasMedicamentoInsuficientesException.java
│   │   ├── IdempotenciaConsumoIncompatibleException.java
│   │   ├── TratamientoNoActivoException.java
│   │   └── UnidadMedicamentoIncompatibleException.java
│   └── port/out/medicamento/
│       ├── ConsumoMedicamentoRepositoryPort.java
│       ├── DiagnosticoQueryPort.java
│       └── InventarioMedicamentoPort.java
├── application/
│   └── medicamento/
│       ├── RegistrarConsumoMedicamentoCommand.java
│       ├── RegistrarConsumoMedicamentoUseCase.java
│       └── result/ConsumoMedicamentoResult.java
└── infrastructure/
    ├── adapter/in/rest/medicamento/
    │   ├── ConsumoMedicamentoController.java
    │   ├── dto/
    │   │   ├── ConsumoMedicamentoResponse.java
    │   │   ├── DetalleConsumoMedicamentoResponse.java
    │   │   └── RegistrarConsumoMedicamentoRequest.java
    │   └── mapper/ConsumoMedicamentoRestMapper.java
    ├── adapter/out/persistence/medicamento/
    │   ├── ConsumoMedicamentoPersistenceAdapter.java
    │   ├── DiagnosticoConsumoQueryAdapter.java
    │   └── InventarioMedicamentoFefoAdapter.java
    ├── adapter/out/persistence/inventario/
    │   ├── AjusteEntradaInventarioPersistenceAdapter.java
    │   └── InventarioMovimientoQueryAdapter.java
    └── config/ConsumoMedicamentoBeanConfiguration.java

src/main/resources/db/migration/
└── V13__crear_consumos_medicamento.sql

src/test/java/com/avicontrol/
├── domain/model/medicamento/ConsumoMedicamentoTest.java
├── application/medicamento/RegistrarConsumoMedicamentoUseCaseTest.java
└── infrastructure/adapter/in/rest/medicamento/ConsumoMedicamentoControllerTest.java
```

**Structure Decision**: consumo y detalle se persisten localmente como evidencia de costo. El diagnóstico es sólo fuente de lote, galpón y medicación. El puerto de inventario bloquea y selecciona recepciones FEFO; los detalles confirmados son también el registro de salida que resta saldo. Selección y persistencia comparten la misma transacción de base de datos, sin descuento parcial.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: acordar el contrato atómico de Inventario y la consulta de diagnóstico.

- [ ] T001 Confirmar que el diagnóstico expone medicamento, lote, galpón y vigencia de tratamiento.
- [ ] T002 Definir con Inventario una operación FEFO que devuelva el desglose por recepción y falle sin descuentos parciales.
- [ ] T003 Crear la migración de consumo y detalles con cantidad normalizada, unidad, recepción y precio histórico.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: implementar normalización, trazabilidad y límites transaccionales.

- [ ] T004 Crear `UnidadConsumoMedicamento` con conversiones `MILIGRAMO` a `GRAMO`, volumen a `MILILITRO` y conteo a `UNIDAD`.
- [ ] T005 Rechazar dimensiones incompatibles y cantidades no positivas.
- [ ] T006 Crear `ConsumoMedicamento` y detalles que conserven por recepción cantidad, precio neto histórico por unidad base y moneda.
- [ ] T007 Definir puertos, idempotencia y compensación/reintento para un fallo entre Inventario y persistencia local.

**Checkpoint**: toda distribución queda trazable por recepción y una falla no deja consumo ni descuento parcial.

---

## Phase 3: User Story 1 - Registrar consumo real de medicamento (Priority: P1)

**Goal**: descontar el medicamento realmente aplicado y dejar al Módulo 3 datos de costo auditables.

**Independent Test**: una solicitud de `250 MILILITRO` para un diagnóstico activo descuenta FEFO. Si cubre dos recepciones, genera dos detalles con sus precios respectivos; sin existencia suficiente no persiste ni descuenta nada.

### Tests for User Story 1

- [ ] T008 [P] [US1] Probar normalización, unidades incompatibles y detalles con precios diferentes.
- [ ] T009 [P] [US1] Probar FEFO, varias recepciones, existencias insuficientes e idempotencia.
- [ ] T010 [P] [US1] Probar MockMvc para éxito, validación, autorización y errores de Inventario.

### Implementation for User Story 1

- [ ] T011 [US1] Implementar `RegistrarConsumoMedicamentoUseCase` y obtener el diagnóstico vigente al confirmar.
- [ ] T012 [US1] Solicitar la salida FEFO usando el medicamento del diagnóstico y persistir consumo con todos los detalles retornados.
- [ ] T013 [US1] Exponer endpoint, DTO y mapper sin aceptar medicamento, lote ni galpón desde el cliente.

## Endpoints

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `POST /api/diagnosticos/{diagnosticoId}/consumos-medicamento` | `ROLE_VETERINARIO` | `RegistrarConsumoMedicamentoRequest` | `201 Created` con consumo y detalles |
| `GET /api/consumos-medicamento/{consumoId}` | `ROLE_VETERINARIO`, `ROLE_ADMINISTRADOR` | — | `200 OK` con consumo y detalles |
| `GET /api/lotes/{loteId}/consumos-medicamento` | `ROLE_VETERINARIO`, `ROLE_ADMINISTRADOR` | — | `200 OK` con consumos y detalles del lote |

El `POST` exige la cabecera `Idempotency-Key` con un UUID nuevo para cada aplicación real. Repetir la misma clave con el mismo cuerpo devuelve el consumo original sin volver a descontar; usarla con datos distintos devuelve `409`.

### Request

```json
{
  "cantidad": 250,
  "unidad": "MILILITRO",
  "fechaAplicacion": "2026-11-05"
}
```

### Response

```json
{
  "id": "b07f5fd4-3b77-4e9a-a4eb-5d54f907c8a4",
  "diagnosticoId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "loteId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "galponId": "72b8d1d1-79da-4d25-bb7a-28b76b1c1b5c",
  "medicamentoId": "c1f5f5eb-a84e-448c-b647-e86132928330",
  "cantidadIngresada": 250,
  "unidadIngresada": "MILILITRO",
  "cantidadUnidadBase": 250,
  "unidadBase": "MILILITRO",
  "fechaAplicacion": "2026-11-05",
  "fechaRegistro": "2026-11-05T15:30:00Z",
  "actor": "veterinario",
  "detallesInventario": [
    {
      "recepcionId": "d0f27182-3f07-41e0-bb98-c8cbe367333b",
      "cantidadUnidadBase": 250,
      "precioNetoHistoricoUnidadBase": 90.00,
      "moneda": "COP"
    }
  ]
}
```

**Errores**: `400` cantidad, fecha o unidad inválida; `401` no autenticado; `403` sin rol; `404` diagnóstico ausente; `409` tratamiento inactivo o unidad incompatible; `422` existencias insuficientes; `503` Inventario no disponible.

El costo para Módulo 3 se calcula por cada detalle como `cantidadUnidadBase × precioNetoHistoricoUnidadBase` y se suma por `loteId`. `GET /api/lotes/{loteId}/consumos-medicamento` conserva todos los detalles sin promediar precios. La autorización entre módulos deberá conectarse al mecanismo de identidad compartido cuando esté disponible.

**Checkpoint**: el consumo usa exclusivamente el medicamento del diagnóstico y conserva cada costo histórico sin promediarlo.

---

## Polish & Cross-Cutting Concerns

- [ ] T014 Documentar la semántica FEFO, las unidades y el contrato de costo para Módulo 3.
- [ ] T015 Añadir métricas de rechazos por falta de existencias y reintentos de integración.
- [ ] T016 Ejecutar pruebas y `git diff --check`.

---

## Dependencies & Execution Order

- **Setup** depende del Plan 011 e Inventario.
- **Foundational** bloquea la historia de consumo.
- **US1** normaliza, reserva/descuenta y persiste en ese orden lógico.
- **Polish** completa observabilidad y contrato externo.

## Notes

- El precio histórico pertenece al detalle de cada recepción, no a un promedio en el consumo.
- La fecha de aplicación es fecha de negocio; la fecha/hora de registro se genera internamente.
- Se aceptan sólo aplicaciones dentro de un tratamiento activo, desde la fecha del diagnóstico y antes de la fecha de reintegro. No se registran aplicaciones futuras.
- Las métricas `avicontrol.medicamento.consumo.rechazos` y `avicontrol.medicamento.consumo.reintentos` registran falta de existencias y claves repetidas.
- Si Inventario pasa a ser un servicio remoto, se necesitará una reserva idempotente y compensación o reintento coordinado antes de sustituir esta transacción local.
