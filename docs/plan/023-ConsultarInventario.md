# Plan: Consultar inventario

**Spec**: [023-ConsultarInventario.md](../specs/023-ConsultarInventario.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

Lecturas consolidadas de existencias, resumen de bodega y cobertura de demanda; no crean movimientos.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/domain/model/inventario/{BodegaCentral,ExistenciaAlimento,ExistenciaMedicamento,CoberturaRequerimientoAlimento}.java
src/main/java/com/avicontrol/application/inventario/{ConsultarInventarioUseCase,ConsultarResumenInventarioUseCase,ConsultarCoberturaAlimentoUseCase}.java
src/main/java/com/avicontrol/infrastructure/adapter/in/rest/inventario/InventarioController.java
```

## User Stories y endpoints

| Método y ruta | Rol |
| --- | --- |
| `GET /api/inventario` | `ROLE_ADMINISTRADOR` |
| `GET /api/inventario/resumen` | `ROLE_ADMINISTRADOR` |
| `GET /api/inventario/alimentos/{alimentoId}/requerimiento` | `ROLE_ADMINISTRADOR` |

```json
{"alimentos":[{"alimentoId":"uuid","stockActualKg":1000}],"medicamentos":[]}
```

```json
{"alimentoId":"uuid","stockActualKg":1000,"demandaKg":1250,"estado":"COBERTURA_PARCIAL"}
```

Errores: `404` alimento, `409` unidad incompatible, `503` demanda no disponible. Pruebas: vacío, agrupación, cinco recepciones recientes y cuatro estados de cobertura.
