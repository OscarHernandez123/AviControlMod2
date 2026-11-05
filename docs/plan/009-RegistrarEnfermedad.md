# Plan: Registrar enfermedad

**Spec**: [009-RegistrarEnfermedad.md](../specs/009-RegistrarEnfermedad.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

Catálogo sanitario de enfermedades: registro y edición para decisiones clínicas futuras.

## Fuera de alcance

Diagnóstico, prescripción, aislamiento, consumo o emisión de sacrificio sanitario.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/model/sanitario/enfermedad/{Enfermedad,NivelRiesgo,TipoEnfermedad}.java
├── domain/port/out/sanitario/EnfermedadRepositoryPort.java
├── application/sanitario/enfermedad/{RegistrarEnfermedadUseCase,EditarEnfermedadUseCase}.java
├── infrastructure/adapter/in/rest/sanitario/EnfermedadController.java
└── infrastructure/adapter/out/persistence/sanitario/enfermedad/EnfermedadPersistenceAdapter.java
src/main/resources/db/migration/V__crear_catalogo_enfermedades.sql
```

## User Story 1 — Registrar enfermedad

| Método y ruta | Rol |
| --- | --- |
| `POST /api/enfermedades` | `ROLE_VETERINARIO` |

```json
{"nombre":"Newcastle","nivelRiesgo":"CRITICO","tipo":"VIRAL","descripcionClinica":"Signos respiratorios y nerviosos","requiereSacrificioSanitario":true}
```

```json
{"id":"uuid","nombre":"Newcastle","nivelRiesgo":"CRITICO","tipo":"VIRAL","requiereSacrificioSanitario":true}
```

## User Story 2 — Editar enfermedad

| Método y ruta | Rol |
| --- | --- |
| `PUT /api/enfermedades/{enfermedadId}` | `ROLE_VETERINARIO` |

Errores: `400` campos obligatorios, `404` enfermedad inexistente. Diagnósticos previos conservan snapshots.

## Pruebas y Definition of Done

- Campos vacíos, booleano explícito, riesgo/tipo y autorización.
- Edición sin reescribir snapshots; migración, MockMvc y OpenAPI verdes.
