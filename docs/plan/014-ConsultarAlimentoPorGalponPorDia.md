# Plan: Consultar alimento por galpón por día

**Spec**: [014-ConsultarAlimentoPorGalponPorDia.md](../specs/014-ConsultarAlimentoPorGalponPorDia.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

Consulta operativa diaria de requerimiento, suministro acumulado, saldo y alertas de alimento para el lote alojado.

## Fuera de alcance

Registro de suministro, salida FEFO, ajuste de plan nutricional, proyección financiera y cobertura de compras.

## Dependencias

Galpón/lote vigente, etapa/ración de nutrición e inventario de solo lectura.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/domain/model/alimentacion/{ConsumoDiario,RequerimientoDiarioGalpon,AlertaAlimentacion}.java
src/main/java/com/avicontrol/domain/port/out/alimentacion/{SuministroDiarioQueryPort,PlanNutricionalQueryPort,StockBodegaQueryPort}.java
src/main/java/com/avicontrol/application/alimentacion/ConsultarAlimentoGalponDiaUseCase.java
src/main/java/com/avicontrol/infrastructure/adapter/in/rest/alimentacion/AlimentacionDiariaController.java
```

## User Story 1 — Consultar requerimiento diario

| Método y ruta | Roles |
| --- | --- |
| `GET /api/galpones/{galponId}/alimento-diario` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` |

```json
{"galponId":"uuid","fecha":"2026-11-05","requerimientoKg":320,"suministradoKg":200,"saldoKg":120,"disponibilidadKg":1600,"alertas":[{"tipo":"SUMINISTRO_PENDIENTE","severidad":"MEDIA","mensaje":"Faltan 120 kg"}]}
```

Errores: `404` galpón/lote, `409` plan sin etapa/alimento, `503` dependencia no disponible.

## Pruebas y Definition of Done

- Población, ración, acumulado, faltante y disponibilidad; lecturas sin escritura.
- Autorización, errores y OpenAPI verdes.
