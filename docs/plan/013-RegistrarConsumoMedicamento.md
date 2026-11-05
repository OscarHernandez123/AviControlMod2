# Plan: Registrar consumo de medicamento

**Spec**: [013-RegistrarConsumoMedicamento.md](../specs/013-RegistrarConsumoMedicamento.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

El veterinario registra consumo real de una medicación diagnóstica y solicita descuento atómico de inventario.

## Fuera de alcance

Prescribir medicación, crear diagnóstico, recibir medicamentos o liquidar costos.

## Dependencias

Plan 011, Inventario con descuento FEFO, catálogo de medicamentos.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/domain/model/medicamento/{ConsumoMedicamento,DetalleConsumo}.java
src/main/java/com/avicontrol/domain/port/out/medicamento/{ConsumoMedicamentoRepositoryPort,DiagnosticoQueryPort,InventarioMedicamentoPort}.java
src/main/java/com/avicontrol/application/medicamento/RegistrarConsumoMedicamentoUseCase.java
src/main/java/com/avicontrol/infrastructure/adapter/in/rest/medicamento/ConsumoMedicamentoController.java
src/main/resources/db/migration/V__crear_consumos_medicamento.sql
```

## User Story 1 — Registrar consumo

| Método y ruta | Rol |
| --- | --- |
| `POST /api/diagnosticos/{diagnosticoId}/consumos-medicamento` | `ROLE_VETERINARIO` |

```json
{"cantidad":250,"unidad":"MILILITRO","fechaHora":"2026-11-05T09:00:00-05:00"}
```

```json
{"id":"uuid","diagnosticoId":"uuid","cantidadUnidadBase":250,"unidadBase":"MILILITRO","detallesInventario":[{"recepcionId":"uuid","cantidad":250}]}
```

Errores: `400` cantidad/unidad, `404` diagnóstico, `409` tratamiento inactivo/unidad incompatible, `422` existencias insuficientes.

## Pruebas y Definition of Done

- Normalización, idempotencia, descuento FEFO y reversión completa ante fallo.
- Pruebas de autorización, integración con inventario y OpenAPI verdes.
