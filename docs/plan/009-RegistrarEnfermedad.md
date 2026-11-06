# Implementation Plan: Registrar una enfermedad

**Date**: 05/11/2026
**Spec**: [009-RegistrarEnfermedad.md](../specs/009-RegistrarEnfermedad.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Crear y editar el catálogo de enfermedades que usará el veterinario en diagnósticos futuros. Cada enfermedad conserva nombre, nivel de riesgo, tipo, descripción clínica y una decisión explícita sobre sacrificio sanitario; editarla no altera los diagnósticos ni decisiones sanitarias ya registrados.

## Technical Context

**Performance Goals**: confirmar el 95 % de registros y ediciones válidos en un segundo.
**Constraints**: acceso exclusivo de `ROLE_VETERINARIO`; todos los campos son obligatorios, incluidos el booleano explícito y los textos no vacíos.
**Scale/Scope**: catálogo local sanitario; no diagnostica, medica, aísla ni emite una orden de sacrificio.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 009-RegistrarEnfermedad.md
└── plan/
    └── 009-RegistrarEnfermedad.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/sanitario/enfermedad/
│   │   ├── Enfermedad.java
│   │   ├── NivelRiesgo.java
│   │   └── TipoEnfermedad.java
│   ├── exception/sanitario/enfermedad/
│   │   ├── DatosEnfermedadInvalidosException.java
│   │   └── EnfermedadNoEncontradaException.java
│   └── port/out/sanitario/enfermedad/EnfermedadRepositoryPort.java
├── application/
│   └── sanitario/enfermedad/
│       ├── EditarEnfermedadCommand.java
│       ├── EditarEnfermedadUseCase.java
│       ├── RegistrarEnfermedadCommand.java
│       ├── RegistrarEnfermedadUseCase.java
│       └── result/EnfermedadResult.java
└── infrastructure/
    ├── adapter/in/rest/sanitario/enfermedad/
    │   ├── EnfermedadController.java
    │   ├── dto/
    │   │   ├── EditarEnfermedadRequest.java
    │   │   ├── EnfermedadResponse.java
    │   │   └── RegistrarEnfermedadRequest.java
    │   └── mapper/EnfermedadRestMapper.java
    ├── adapter/out/persistence/sanitario/enfermedad/EnfermedadPersistenceAdapter.java
    └── config/EnfermedadBeanConfiguration.java

src/main/resources/db/migration/
└── V__crear_catalogo_enfermedades.sql

src/test/java/com/avicontrol/
├── domain/model/sanitario/enfermedad/EnfermedadTest.java
├── application/sanitario/enfermedad/
│   ├── EditarEnfermedadUseCaseTest.java
│   └── RegistrarEnfermedadUseCaseTest.java
└── infrastructure/adapter/in/rest/sanitario/enfermedad/EnfermedadControllerTest.java
```

**Structure Decision**: `Enfermedad` es un agregado de catálogo persistido localmente. Diagnósticos conservan sus propias instantáneas: este plan no requiere ni debe actualizar esos registros al editar el catálogo.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: preparar el esquema y valores controlados del catálogo.

- [ ] T001 Definir valores permitidos de `NivelRiesgo` y `TipoEnfermedad` con el vocabulario acordado.
- [ ] T002 Crear la migración con todos los campos obligatorios y sin valor predeterminado para `requiereSacrificioSanitario`.
- [ ] T003 Preparar fixtures con textos vacíos, valores de enum inválidos y ambas decisiones de sacrificio.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: implementar las invariantes de la enfermedad y la persistencia atómica.

- [ ] T004 Crear `Enfermedad` y validar nombre, tipo y descripción después de eliminar espacios exteriores.
- [ ] T005 Exigir que riesgo, tipo e indicador de sacrificio estén presentes y sean válidos.
- [ ] T006 Definir repositorio, excepciones y configuración de beans sin dependencias de diagnóstico.

**Checkpoint**: no existe forma de persistir una enfermedad parcial ni de inferir la decisión sanitaria por defecto.

---

## Phase 3: User Story 1 - Registrar una enfermedad (Priority: P1)

**Goal**: permitir que el veterinario cree una enfermedad completa, incluida su decisión explícita sobre sacrificio sanitario.

**Independent Test**: una solicitud válida crea la enfermedad con `requiereSacrificioSanitario` verdadero o falso según lo enviado; un campo ausente, vacío o un usuario no veterinario no crea nada.

### Tests for User Story 1

- [ ] T007 [P] [US1] Probar dominio para textos vacíos, enums, booleano ausente y ambos valores del indicador.
- [ ] T008 [P] [US1] Probar caso de uso y MockMvc de éxito, validación y autorización.

### Implementation for User Story 1

- [ ] T009 [US1] Implementar `RegistrarEnfermedadUseCase` y su comando.
- [ ] T010 [US1] Exponer `POST /api/enfermedades` sólo para `ROLE_VETERINARIO`.
- [ ] T011 [US1] Crear DTO, respuesta y mapper REST.

```json
{
  "nombre": "Newcastle",
  "nivelRiesgo": "CRITICO",
  "tipo": "VIRAL",
  "descripcionClinica": "Signos respiratorios y nerviosos",
  "requiereSacrificioSanitario": true
}
```

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "nombre": "Newcastle",
  "nivelRiesgo": "CRITICO",
  "tipo": "VIRAL",
  "requiereSacrificioSanitario": true
}
```

---

## Phase 4: User Story 2 - Editar una enfermedad (Priority: P2)

**Goal**: actualizar el catálogo para diagnósticos futuros sin modificar registros clínicos previos.

**Independent Test**: al cambiar el indicador de una enfermedad vinculada a un diagnóstico, el diagnóstico existente conserva su decisión y los futuros consultan el nuevo valor.

### Tests for User Story 2

- [ ] T012 [P] [US2] Probar edición válida, enfermedad inexistente y que no se consulta ni escribe diagnóstico.
- [ ] T013 [P] [US2] Probar autorización del endpoint.

### Implementation for User Story 2

- [ ] T014 [US2] Implementar `EditarEnfermedadUseCase` reutilizando todas las validaciones.
- [ ] T015 [US2] Exponer `PUT /api/enfermedades/{enfermedadId}` para `ROLE_VETERINARIO`.

**Checkpoint**: el catálogo se actualiza, pero los diagnósticos históricos permanecen inmutables.

---

## Endpoints

### Registrar enfermedad

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `POST /api/enfermedades` | `ROLE_VETERINARIO` | `RegistrarEnfermedadRequest` | `201 Created` con la enfermedad |

### Editar enfermedad

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `PUT /api/enfermedades/{enfermedadId}` | `ROLE_VETERINARIO` | `EditarEnfermedadRequest` | `200 OK` con la enfermedad actualizada |

**Errores**: `400` campos obligatorios, textos o enums inválidos; `401` no autenticado; `403` sin rol; `404` enfermedad inexistente.

---

## Polish & Cross-Cutting Concerns

- [ ] T016 Mapear `400` datos inválidos, `401` no autenticado, `403` rol no autorizado y `404` enfermedad inexistente.
- [ ] T017 Documentar los enums y la obligatoriedad del booleano en OpenAPI.
- [ ] T018 Ejecutar pruebas y `git diff --check`.

---

## Dependencies & Execution Order

- **Setup** define esquema y vocabulario.
- **Foundational** bloquea registro y edición.
- **US1** crea el catálogo; **US2** reutiliza su entidad y sus reglas.
- **Polish** completa el contrato y seguridad.

## Notes

- `true` identifica una enfermedad que puede sustentar sacrificio sanitario total; no crea esa orden por sí mismo.
- La relación de una medicación con la enfermedad se resolverá en el Plan 008, sin acoplar este catálogo a medicaciones o diagnósticos.
