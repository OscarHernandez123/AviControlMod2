# Plan: Diagnosticar galpón

**Spec**: [011-DiagnosticarGalpon.md](../specs/011-DiagnosticarGalpon.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

El veterinario registra diagnóstico en un galpón aislado, captura snapshots clínicos y programa reintegro cuando corresponde.

## Fuera de alcance

Solicitud/validación de aislamiento, alta de enfermedad/medicación, consumo real y ejecución de sacrificio.

## Dependencias

Planes 009, 008 y 010; aislamiento validado; Módulo 1 para estado/lote.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/domain/model/sanitario/diagnostico/{DiagnosticoGalpon,EstadoDiagnostico}.java
src/main/java/com/avicontrol/domain/port/out/sanitario/diagnostico/{DiagnosticoGalponRepositoryPort,EnfermedadQueryPort,MedicacionQueryPort}.java
src/main/java/com/avicontrol/application/sanitario/diagnostico/RegistrarDiagnosticoUseCase.java
src/main/java/com/avicontrol/infrastructure/adapter/in/rest/sanitario/diagnostico/DiagnosticoController.java
src/main/resources/db/migration/V__crear_diagnosticos.sql
```

## User Story 1 — Registrar diagnóstico

| Método y ruta | Rol |
| --- | --- |
| `POST /api/galpones/{galponId}/diagnosticos` | `ROLE_VETERINARIO` |

```json
{"enfermedadId":"uuid","medicacionId":"uuid","fechaReintegro":"2026-11-12","observaciones":"Tratamiento prescrito"}
```

```json
{"id":"uuid","galponId":"uuid","estado":"ACTIVO","enfermedad":{"nombre":"Newcastle","requiereSacrificioSanitario":true},"fechaReintegro":null}
```

Errores: `404` dependencias, `409` galpón sin aislamiento o medicación incompatible, `503` Módulo 1 no disponible.

## Pruebas y Definition of Done

- Snapshot inmutable, enfermedad con sacrificio sin reintegro, medicación compatible y autorización.
- Eventos posteriores solo tras persistencia; pruebas y OpenAPI verdes.
