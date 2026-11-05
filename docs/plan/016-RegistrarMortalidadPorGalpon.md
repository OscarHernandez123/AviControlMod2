# Plan: Registrar mortalidad por galpón

**Spec**: [016-RegistrarMortalidadPorGalpon.md](../specs/016-RegistrarMortalidadPorGalpon.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

Registro de baja ordinaria y actualización atómica de población viva mediante puerto de Módulo 1.

## Fuera de alcance

Mortalidad anormal, alertas, diagnóstico y sacrificio.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/domain/model/mortalidad/{RegistroMortalidad,CausaMortalidad}.java
src/main/java/com/avicontrol/domain/port/out/mortalidad/{RegistroMortalidadRepositoryPort,ActualizarInventarioVivoPort}.java
src/main/java/com/avicontrol/application/mortalidad/RegistrarMortalidadUseCase.java
src/main/java/com/avicontrol/infrastructure/adapter/in/rest/mortalidad/MortalidadController.java
```

## User Story 1 — Registrar baja ordinaria

| Método y ruta | Roles |
| --- | --- |
| `POST /api/galpones/{galponId}/mortalidad` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` |

```json
{"fechaHora":"2026-11-05T08:00:00-05:00","cantidad":4,"causa":"ENFERMEDAD","observaciones":"Hallazgo diario"}
```

```json
{"id":"uuid","galponId":"uuid","cantidad":4,"poblacionResultante":7956}
```

Errores: `400` cantidad/fecha, `404` galpón/lote, `409` población insuficiente, `503` Módulo 1 no disponible.

## Pruebas y Definition of Done

- Idempotencia, descuento atómico, población no negativa y autorización.
- Evento posterior a confirmación; pruebas/OpenAPI verdes.
