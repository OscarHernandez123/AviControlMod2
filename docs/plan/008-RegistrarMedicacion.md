# Implementation Plan: Registrar medicación

**Date**: 05/11/2026
**Spec**: [008-RegistrarMedicacion.md](../specs/008-RegistrarMedicacion.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Permitir que un veterinario registre y edite medicaciones reutilizables para diagnósticos futuros. Cada medicación referencia una enfermedad disponible y un medicamento del inventario, sin administrar producto ni alterar existencias. Las ediciones nunca modifican diagnósticos ya registrados.

## Technical Context

**Performance Goals**: registrar o editar una medicación válida en máximo un segundo para el 95 % de solicitudes.
**Constraints**: acceso exclusivo de `ROLE_VETERINARIO`; enfermedad y medicamento se vuelven a verificar al confirmar; nombre y principio activo del fármaco son obligatorios en REST; `diasAplicacion` opcional indica días ordinales del tratamiento y su ausencia significa aplicación diaria.
**Scale/Scope**: catálogo local de medicaciones para diagnósticos futuros; excluye diagnóstico, consumo y descuento de inventario.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 008-RegistrarMedicacion.md
└── plan/
    └── 008-RegistrarMedicacion.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/sanitario/medicacion/Medicacion.java
│   ├── exception/sanitario/medicacion/
│   │   ├── DatosMedicacionInvalidosException.java
│   │   ├── EnfermedadNoDisponibleException.java
│   │   ├── MedicacionNoEncontradaException.java
│   │   └── MedicamentoNoDisponibleException.java
│   └── port/out/sanitario/medicacion/
│       ├── EnfermedadQueryPort.java
│       ├── MedicacionRepositoryPort.java
│       └── MedicamentoCatalogoQueryPort.java
├── application/
│   └── sanitario/medicacion/
│       ├── EditarMedicacionCommand.java
│       ├── EditarMedicacionUseCase.java
│       ├── RegistrarMedicacionCommand.java
│       ├── RegistrarMedicacionUseCase.java
│       └── result/MedicacionResult.java
└── infrastructure/
    ├── adapter/in/rest/medicacion/
    │   ├── MedicacionController.java
    │   ├── dto/
    │   │   ├── EditarMedicacionRequest.java
    │   │   ├── MedicacionResponse.java
    │   │   └── RegistrarMedicacionRequest.java
    │   └── mapper/MedicacionRestMapper.java
    ├── adapter/out/persistence/sanitario/medicacion/
    │   ├── EnfermedadDisponibleAdapter.java
    │   ├── MedicacionPersistenceAdapter.java
    │   └── MedicamentoDisponibleAdapter.java
    └── config/MedicacionBeanConfiguration.java

src/main/resources/db/migration/
├── V10__crear_medicaciones.sql
└── V15__ampliar_prescripciones_y_diagnosticos.sql

src/test/java/com/avicontrol/
├── domain/model/sanitario/medicacion/MedicacionTest.java
├── application/sanitario/medicacion/
│   ├── EditarMedicacionUseCaseTest.java
│   └── RegistrarMedicacionUseCaseTest.java
└── infrastructure/adapter/in/rest/medicacion/MedicacionControllerTest.java
```

**Structure Decision**: la medicación es un agregado propio persistido localmente. Enfermedad y medicamento son referencias verificadas mediante puertos; el inventario no expone ningún puerto de movimiento en esta funcionalidad.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: confirmar contratos de disponibilidad y persistencia de catálogo.

- [x] T001 Confirmar los puertos de consulta para enfermedad disponible y medicamento existente en inventario.
- [x] T002 Crear la migración de medicaciones con referencias, dosis, duración y descripción obligatorias.
- [x] T003 Preparar datos de prueba para enfermedad o medicamento no disponibles y textos en blanco.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: crear reglas de consistencia antes de registrar o editar.

- [x] T004 Crear `Medicacion` y validar enfermedad, medicamento, dosis, duración positiva entera y descripción no vacía.
- [x] T005 Definir repositorio y puertos de consulta separados de DTO REST.
- [x] T006 Asegurar la transacción: se guardan todos los datos de la medicación o no se guarda ninguno.

**Checkpoint**: el dominio y los puertos impiden crear medicaciones incompletas o con referencias no disponibles.

---

## Phase 3: User Story 1 - Registrar una medicación (Priority: P1)

**Goal**: registrar una medicación disponible para diagnósticos futuros sin requerir diagnóstico ni modificar inventario.

**Independent Test**: con enfermedad y medicamento disponibles, dosis, días y descripción válidos se crea una medicación; una referencia no disponible o duración cero no produce ningún registro.

### Tests for User Story 1

- [x] T007 [P] [US1] Probar reglas de dominio y referencias verificadas al confirmar.
- [x] T008 [P] [US1] Probar el caso de uso y MockMvc de éxito, validación y rol.

### Implementation for User Story 1

- [x] T009 [US1] Implementar `RegistrarMedicacionUseCase` y su comando.
- [x] T010 [US1] Exponer `POST /api/medicaciones` para `ROLE_VETERINARIO`.
- [x] T011 [US1] Crear DTO de solicitud, respuesta y mapper REST.

```json
{
  "enfermedadId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "medicamentoId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "dosis": "0.5 ml por ave",
  "duracionDias": 5,
  "descripcion": "Administrar según indicación veterinaria",
  "nombreMedicamento": "Amprolio 20% Solución",
  "principioActivo": "Amprolio",
  "diasAplicacion": [
    1,
    3,
    5
  ]
}
```

La respuesta `201` y la respuesta `200` de edición contienen el siguiente contrato. `PUT /api/medicaciones/{medicacionId}` recibe el mismo JSON completo del registro.

```json
{
  "id": "d2a216f9-358a-482f-b735-d794d6431944",
  "enfermedadId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "medicamentoId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "dosis": "0.5 ml por ave",
  "duracionDias": 5,
  "descripcion": "Administrar según indicación veterinaria",
  "nombreMedicamento": "Amprolio 20% Solución",
  "principioActivo": "Amprolio",
  "diasAplicacion": [
    1,
    3,
    5
  ]
}
```

El medicamento debe tener saldo positivo en una recepción confirmada y vigente al momento de registrar o editar. Esta verificación no descuenta el saldo.

---

## Phase 4: User Story 2 - Editar una medicación (Priority: P2)

**Goal**: actualizar los datos para diagnósticos futuros sin recalcular ni modificar diagnósticos existentes.

**Independent Test**: una medicación de cinco días se edita a siete; el valor anterior no se muta y la nueva lectura devuelve siete. Cuando el Plan 011 implemente diagnósticos, su prueba de integración verificará que las instantáneas previas conservan cinco días.

### Tests for User Story 2

- [x] T012 [P] [US2] Probar edición válida sin dependencia de puertos de diagnóstico; la conservación de instantáneas se verificará al implementar el Plan 011.
- [x] T013 [P] [US2] Probar autorización y referencias no disponibles.

### Implementation for User Story 2

- [x] T014 [US2] Implementar `EditarMedicacionUseCase` con las mismas validaciones de registro.
- [x] T015 [US2] Exponer `PUT /api/medicaciones/{medicacionId}` para `ROLE_VETERINARIO`.

**Checkpoint**: el catálogo actualizado sirve sólo a diagnósticos futuros.

---

## Endpoints

### Registrar medicación

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `POST /api/medicaciones` | `ROLE_VETERINARIO` | `RegistrarMedicacionRequest` | `201 Created` con la medicación |

### Editar medicación

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `PUT /api/medicaciones/{medicacionId}` | `ROLE_VETERINARIO` | `EditarMedicacionRequest` | `200 OK` con la medicación actualizada |

**Errores**: `400` campos, duración o textos inválidos; `401` no autenticado; `403` sin rol; `404` medicación o referencia ausente; `503` catálogo externo no disponible.

---

## Polish & Cross-Cutting Concerns

- [x] T016 Mapear `400` datos inválidos, `401` no autenticado, `403` sin rol, `404` medicación o referencia ausente y `503` catálogo externo no disponible.
- [x] T017 Documentar endpoints y el hecho de que registrar medicación no descuenta inventario.
- [x] T018 Ejecutar pruebas y `git diff --check`.

---

## Dependencies & Execution Order

- **Setup** confirma enfermedad y catálogo de medicamentos.
- **Foundational** crea entidad, validaciones y puertos.
- **US1** registra la base; **US2** reutiliza sus reglas y no depende de diagnósticos.
- **Polish** cierra contrato y seguridad.

## Notes

- Una edición no muta instantáneas ya guardadas en diagnósticos ni sus fechas de reintegro.
- `nombreMedicamento` y `principioActivo` se capturan al prescribir y quedan congelados en cada diagnóstico futuro. El identificador del medicamento sigue verificándose contra inventario. Las prescripciones antiguas sin estos datos conservan `null` y pueden obtener el nombre desde la recepción al consultarse.
- `diasAplicacion` contiene días ordinales únicos entre 1 y `duracionDias`. Si se omite, se aplica todos los días; en un día no programado, la consulta del Plan 015 devuelve dosis `0`.
- No se añade consumo, recepción ni salida de inventario: registrar una medicación es definir un tratamiento, no administrarlo.
