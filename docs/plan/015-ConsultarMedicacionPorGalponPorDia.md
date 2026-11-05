# Plan: Consultar medicación por galpón por día

**Spec**: [015-ConsultarMedicacionPorGalponPorDia.md](../specs/015-ConsultarMedicacionPorGalponPorDia.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

Consulta de tratamientos vigentes de un galpón y resumen diario permitido por rol.

## Fuera de alcance

Crear/editar medicación, diagnóstico, consumo real o descuento de inventario.

## Dependencias

Diagnósticos activos, medicaciones vigentes, galpones/lotes y `Clock`.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/application/sanitario/medicacion/{ConsultarMedicacionDiariaGalponUseCase,ConsultarResumenMedicacionDiariaUseCase}.java
src/main/java/com/avicontrol/application/sanitario/medicacion/result/{MedicacionDiariaResult,ResumenMedicacionDiariaResult}.java
src/main/java/com/avicontrol/infrastructure/adapter/in/rest/medicacion/MedicacionDiariaController.java
```

## User Story 1 — Consultar por galpón

| Método y ruta | Roles |
| --- | --- |
| `GET /api/galpones/{galponId}/medicacion-diaria` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` |

## User Story 2 — Consultar resumen

| Método y ruta | Roles |
| --- | --- |
| `GET /api/medicacion-diaria` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` |

```json
{"fecha":"2026-11-05","galpones":[{"galponId":"uuid","tratamientos":[{"medicacion":"Antibiótico","dosis":0.5,"unidad":"MILILITRO"}]}]}
```

Errores: `404` galpón, `503` datos clínicos no disponibles. Las consultas no crean consumos.

## Pruebas y Definition of Done

- Solo tratamientos vigentes en fecha de negocio; resumen por rol y cero tratamientos.
- MockMvc, seguridad y OpenAPI verdes.
