# Plan: Registrar medicación

**Spec**: [008-RegistrarMedicacion.md](../specs/008-RegistrarMedicacion.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

El veterinario registra y edita medicaciones para diagnósticos futuros, asociadas a enfermedad y medicamento del catálogo.

## Fuera de alcance

Diagnóstico, consumo real, descuento de inventario y consultas diarias.

## Dependencias

Catálogo de enfermedades (Plan 009) y catálogo de medicamentos.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/model/sanitario/medicacion/{Medicacion.java,EstadoMedicacion.java}.java
├── domain/port/out/sanitario/medicacion/{MedicacionRepositoryPort,EnfermedadQueryPort,MedicamentoCatalogoQueryPort}.java
├── application/sanitario/medicacion/{RegistrarMedicacionUseCase,EditarMedicacionUseCase}.java
├── infrastructure/adapter/in/rest/medicacion/MedicacionController.java
├── infrastructure/adapter/in/rest/medicacion/dto/{RegistrarMedicacionRequest,EditarMedicacionRequest}.java
└── infrastructure/adapter/out/persistence/sanitario/medicacion/MedicacionPersistenceAdapter.java
src/main/resources/db/migration/V__crear_medicaciones.sql
```

## User Story 1 — Crear medicación

| Método y ruta | Rol |
| --- | --- |
| `POST /api/medicaciones` | `ROLE_VETERINARIO` |

```json
{"enfermedadId":"uuid","medicamentoId":"uuid","dosisPorAve":0.5,"unidad":"MILILITRO","duracionDias":5}
```

## User Story 2 — Editar medicación

| Método y ruta | Rol |
| --- | --- |
| `PUT /api/medicaciones/{medicacionId}` | `ROLE_VETERINARIO` |

Errores: `400` dosis/duración inválida, `404` dependencias inexistentes, `409` unidad incompatible.

## Pruebas y Definition of Done

- Validar dosis, unidad y duración; edición solo afecta diagnósticos futuros.
- MockMvc de éxito, validación y autorización; migración y OpenAPI verdes.
