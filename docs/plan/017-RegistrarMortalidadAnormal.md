# Plan: Registrar mortalidad anormal

**Spec**: [017-RegistrarMortalidadAnormal.md](../specs/017-RegistrarMortalidadAnormal.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

Registro de reporte anormal y creación interna atómica de alerta sanitaria.

## Fuera de alcance

Baja ordinaria, consulta/cierre de alertas, aislamiento y diagnóstico.

## Dependencias

Plan 018 expone `GenerarAlertaSanitariaPort`; Módulo 1 aporta población vigente.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/domain/model/mortalidad/RegistroMortalidadAnormal.java
src/main/java/com/avicontrol/domain/port/out/mortalidad/{RegistroMortalidadAnormalRepositoryPort,GenerarAlertaSanitariaPort}.java
src/main/java/com/avicontrol/application/mortalidad/RegistrarMortalidadAnormalUseCase.java
src/main/java/com/avicontrol/infrastructure/adapter/in/rest/mortalidad/MortalidadController.java
```

## User Story 1 — Reportar mortalidad anormal

| Método y ruta | Roles |
| --- | --- |
| `POST /api/galpones/{galponId}/mortalidad-anormal` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` |

```json
{"fechaHora":"2026-11-05T08:00:00-05:00","cantidad":35,"causaProbable":"RESPIRATORIA","observaciones":"Aumento súbito"}
```

```json
{"id":"uuid","galponId":"uuid","porcentaje":0.44,"alertaId":"uuid"}
```

Errores: `400` datos, `404` galpón/lote, `409` estado incompatible, `503` alerta/Módulo 1 no disponible.

## Pruebas y Definition of Done

- Cálculo de porcentaje y creación única de alerta en la misma transacción.
- Fallo de alerta revierte reporte; seguridad y OpenAPI verdes.
