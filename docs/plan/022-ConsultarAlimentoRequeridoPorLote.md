# Plan: Consultar alimento requerido por lote

**Spec**: [022-ConsultarAlimentoRequeridoPorLote.md](../specs/022-ConsultarAlimentoRequeridoPorLote.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

Proyección histórica por lote y etapa, costo referencial y balance de abastecimiento sin reservar stock.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/domain/model/alimentacion/{ProyeccionAlimentoEtapa,HistorialAjusteProyeccion,BalanceAbastecimientoAlimento}.java
src/main/java/com/avicontrol/application/alimentacion/{RegistrarProyeccionEtapaUseCase,ActualizarProyeccionPorProrrogaUseCase,ConsultarRequerimientoLoteFinanzasUseCase,ConsultarBalanceAbastecimientoUseCase}.java
src/main/java/com/avicontrol/infrastructure/adapter/in/rest/alimentacion/{ProyeccionAlimentoLoteController,BalanceAbastecimientoController}.java
```

## User Stories y endpoints

| Método y ruta | Roles |
| --- | --- |
| `GET /api/lotes/{loteId}/alimento-requerido` | ADMINISTRADOR, NUTRICIONISTA, sistema autorizado |
| `POST /api/proyecciones/{id}/completar-costo` | ADMINISTRADOR |
| `GET /api/administracion/balance-abastecimiento` | ADMINISTRADOR, NUTRICIONISTA |

```json
{"loteId":"uuid","etapas":[{"etapa":"INICIO","kilogramosProyectados":1250,"costoUnitarioReferencia":3200}]}
```

Errores: `404` lote/proyección, `409` costo ya completado, `503` stock/precio no disponible. Pruebas: eventos idempotentes, cálculo exacto y concurrencia.
