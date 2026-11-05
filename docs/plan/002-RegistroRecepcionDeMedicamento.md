# Plan: Registro de recepción de medicamento

**Spec**: [002-RegistroRecepcionDeMedicamento.md](../specs/002-RegistroRecepcionDeMedicamento.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

El administrador registra una recepción de medicamento, normaliza la cantidad a unidad base y confirma atómicamente su entrada inicial en inventario.

## Fuera de alcance

Edición, historial, consumo real, FEFO, vencimientos y consultas de inventario.

## Dependencias

Catálogo de medicamentos y `RegistrarEntradaInventarioPort` para `GRAMO`, `MILILITRO` o `UNIDAD`.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/domain/model/recepcion/RecepcionMedicamento.java
src/main/java/com/avicontrol/domain/port/out/recepcion/RecepcionMedicamentoRepositoryPort.java
src/main/java/com/avicontrol/application/recepcion/RegistrarRecepcionMedicamentoUseCase.java
src/main/java/com/avicontrol/application/inventario/port/RegistrarEntradaInventarioPort.java
src/main/java/com/avicontrol/infrastructure/adapter/in/rest/recepcion/RecepcionMedicamentoController.java
src/main/java/com/avicontrol/infrastructure/adapter/in/rest/recepcion/dto/RegistrarRecepcionMedicamentoRequest.java
src/main/java/com/avicontrol/infrastructure/adapter/out/persistence/recepcion/RecepcionMedicamentoPersistenceAdapter.java
src/main/resources/db/migration/V__crear_recepciones_medicamento.sql
```

## User Story 1 — Registrar recepción

| Método y ruta | Rol | Respuesta |
| --- | --- | --- |
| `POST /api/recepciones/medicamentos` | `ROLE_ADMINISTRADOR` | `201 Created` |

```json
{"medicamentoId":"uuid","codigoLote":"MED-2026-001","fechaIngreso":"2026-11-05","fechaVencimiento":"2027-05-05","cantidadPresentaciones":20,"contenidoPorPresentacion":500,"unidadContenido":"MILILITRO","precioNetoPorPresentacion":45000,"impuestoPorcentaje":19}
```

```json
{"id":"uuid","estado":"CONFIRMADA","cantidadUnidadBase":10000,"unidadBase":"MILILITRO","fechaIngreso":"2026-11-05"}
```

Errores: `400` datos/unidad inválidos, `404` medicamento, `409` vencimiento o unidad incompatible, `503` inventario no disponible.

## Pruebas y Definition of Done

- Normalizar mg a g, rechazar dimensiones incompatibles y revertir ante fallo de entrada.
- Pruebas de dominio, caso de uso, MockMvc, migración y OpenAPI verdes.
