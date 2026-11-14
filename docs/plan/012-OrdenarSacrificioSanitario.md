# Implementation Plan: Ordenar sacrificio sanitario

**Date**: 05/11/2026
**Spec**: [012-OrdenarSacrificioSanitario.md](../specs/012-OrdenarSacrificioSanitario.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Permitir a un veterinario emitir una orden de sacrificio sanitario total a partir de un diagnóstico elegible. La orden queda pendiente sin cambiar lote ni galpón; al confirmar la ejecución usa la población vigente, la establece en cero, cambia el galpón de `aislamiento` a `vaciado sanitario` y marca la orden como ejecutada una sola vez.

## Technical Context

**Performance Goals**: completar el 95 % de confirmaciones válidas en un segundo.
**Constraints**: exclusivo de `ROLE_VETERINARIO`; diagnóstico con enfermedad que requiere sacrificio sanitario, galpón aislado y población mayor que cero.
**Scale/Scope**: orden total por diagnóstico, emisión y confirmación; no registra diagnóstico, sacrificio productivo, peso final ni liquidación.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 012-OrdenarSacrificioSanitario.md
└── plan/
    └── 012-OrdenarSacrificioSanitario.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/sacrificio/sanitario/
│   │   ├── EstadoOrdenSacrificioSanitario.java
│   │   └── OrdenSacrificioSanitario.java
│   ├── exception/sacrificio/sanitario/
│   │   ├── DiagnosticoNoElegibleException.java
│   │   ├── OrdenSanitariaNoPendienteException.java
│   │   └── PoblacionNoSacrificableException.java
│   └── port/out/
│       ├── modulo1/
│       │   ├── GalponQueryPort.java
│       │   ├── GalponUpdatePort.java
│       │   └── LoteUpdatePort.java
│       └── sacrificio/sanitario/
│           ├── DiagnosticoSanitarioQueryPort.java
│           └── OrdenSacrificioSanitarioRepositoryPort.java
├── application/
│   └── sacrificio/sanitario/
│       ├── ConfirmarEjecucionSacrificioSanitarioUseCase.java
│       ├── ConsultarOrdenesSacrificioSanitarioUseCase.java
│       ├── EmitirOrdenSacrificioSanitarioUseCase.java
│       └── result/OrdenSacrificioSanitarioResult.java
└── infrastructure/
    ├── adapter/in/rest/sacrificio/sanitario/
    │   ├── OrdenSacrificioSanitarioController.java
    │   ├── dto/OrdenSacrificioSanitarioResponse.java
    │   └── mapper/OrdenSacrificioSanitarioRestMapper.java
    ├── adapter/out/persistence/sacrificio/sanitario/
    │   ├── DiagnosticoSanitarioQueryAdapter.java
    │   └── OrdenSacrificioSanitarioPersistenceAdapter.java
    ├── adapter/out/integration/modulo1/
    │   ├── GalponUpdateAdapter.java
    │   ├── GalponUpdateNoDisponibleAdapter.java
    │   ├── LoteUpdateAdapter.java
    │   └── LoteUpdateNoDisponibleAdapter.java
    └── config/SacrificioSanitarioBeanConfiguration.java

src/main/resources/db/migration/
└── V19__crear_ordenes_sacrificio_sanitario.sql

src/test/java/com/avicontrol/
├── domain/model/sacrificio/sanitario/OrdenSacrificioSanitarioTest.java
├── application/sacrificio/sanitario/
│   ├── ConfirmarEjecucionSacrificioSanitarioUseCaseTest.java
│   └── EmitirOrdenSacrificioSanitarioUseCaseTest.java
└── infrastructure/adapter/in/rest/sacrificio/sanitario/OrdenSacrificioSanitarioControllerTest.java
```

**Structure Decision**: la orden sanitaria es un agregado local con transición pendiente a ejecutada. Diagnóstico, lote y galpón se verifican por puertos del Plan 011/Módulo 1; las actualizaciones remotas se coordinan de forma idempotente.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: definir contratos y persistencia de la orden sanitaria.

- [x] T001 Confirmar el puerto de diagnóstico elegible del Plan 011 y contratos de consulta/actualización de lote y galpón.
- [x] T002 Crear la migración con referencias a diagnóstico, lote y galpón, estado y control de versión.
- [x] T003 Preparar fixtures para diagnóstico mortal, tratable, galpón fuera de aislamiento, población cero y orden ejecutada.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: proteger el ciclo de vida y la ejecución única.

- [x] T004 Crear `OrdenSacrificioSanitario` con alcance total implícito y transición única pendiente a ejecutada.
- [x] T005 Definir repositorio con restricción de orden por diagnóstico y bloqueo para confirmaciones simultáneas.
- [x] T006 Implementar excepciones y puertos de diagnóstico, galpón y lote.

**Checkpoint**: ninguna orden puede ejecutar dos veces ni abarcar una cantidad parcial.

---

## Phase 3: User Story 1 - Emitir una orden sanitaria total (Priority: P1)

**Goal**: crear una orden pendiente sin alterar población ni estado.

**Independent Test**: con diagnóstico mortal, galpón aislado y población positiva se crea una orden pendiente; un diagnóstico tratable o galpón fuera de aislamiento no crea orden.

### Tests for User Story 1

- [x] T007 [P] [US1] Probar elegibilidad, población, orden duplicada y autorización.
- [x] T008 [P] [US1] Probar el endpoint de emisión con MockMvc.

### Implementation for User Story 1

- [x] T009 [US1] Implementar `EmitirOrdenSacrificioSanitarioUseCase` y verificar diagnóstico, estado y población vigentes.
- [x] T010 [US1] Exponer `POST /api/galpones/{galponId}/ordenes-sacrificio-sanitario` para veterinario.

**Checkpoint**: la orden pendiente no modifica lote ni galpón.

---

## Phase 4: User Story 2 - Confirmar ejecución sanitaria total (Priority: P2)

**Goal**: al confirmar, establecer población en cero, estado en `vaciado sanitario` y orden ejecutada como una única operación.

**Independent Test**: una orden pendiente con galpón aislado y población vigente positiva termina ejecutada; otra confirmación no hace cambios adicionales.

### Tests for User Story 2

- [x] T011 [P] [US2] Probar ejecución única, población que cambió tras emisión y fallo parcial de integración.
- [x] T012 [P] [US2] Probar HTTP, autorización y orden ya ejecutada.

### Implementation for User Story 2

- [x] T013 [US2] Implementar `ConfirmarEjecucionSacrificioSanitarioUseCase` consultando estado y población vigentes.
- [x] T014 [US2] Coordinar actualización del lote a cero, del galpón a `vaciado sanitario` y de la orden con compensación o reintento seguro.
- [x] T015 [US2] Exponer `POST /api/ordenes-sacrificio-sanitario/{ordenId}/ejecucion`.

```json
{
  "id": "b07f5fd4-3b77-4e9a-a4eb-5d54f907c8a4",
  "estado": "EJECUTADA",
  "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "poblacionSacrificada": 7960
}
```

**Checkpoint**: sólo la confirmación cambia los recursos y toda repetición es segura.

---

## Endpoints

### Consultar órdenes

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `GET /api/ordenes-sacrificio-sanitario` | `ROLE_VETERINARIO` | Sin cuerpo | `200 OK` con órdenes persistidas, ordenadas por fecha de emisión descendente |

```json
[
  {
    "id": "66e81178-df4d-4c99-96ea-20c267c4e7b0",
    "diagnosticoId": "06bf1256-80e3-4b71-8f5c-788284d68915",
    "galponId": "550e8400-e29b-41d4-a716-446655440201",
    "loteId": "550e8400-e29b-41d4-a716-446655440202",
    "estado": "EJECUTADA",
    "poblacionSacrificada": 5000
  }
]
```

### Emitir orden

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `POST /api/galpones/{galponId}/ordenes-sacrificio-sanitario` | `ROLE_VETERINARIO` | Sin cuerpo | `201 Created` con orden pendiente |

### Confirmar ejecución

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `POST /api/ordenes-sacrificio-sanitario/{ordenId}/ejecucion` | `ROLE_VETERINARIO` | Sin cuerpo | `200 OK` con orden ejecutada |

**Errores**: `400` identificador inválido; `401` no autenticado; `403` sin rol; `404` orden o diagnóstico ausente; `409` diagnóstico no elegible, población inválida o ejecución repetida; `503` Módulo 1 no disponible.

---

## Polish & Cross-Cutting Concerns

- [x] T016 Mapear `400` dato inválido, `401` no autenticado, `403` sin rol, `404` orden/diagnóstico ausente, `409` precondición, población o ejecución repetida y `503` dependencia no disponible.
- [x] T017 Documentar consistencia, idempotencia y los tres endpoints en OpenAPI.
- [x] T018 Ejecutar pruebas y `git diff --check`.

---

## Dependencies & Execution Order

- **Setup** depende del diagnóstico del Plan 011 y del Módulo 1.
- **Foundational** bloquea la emisión y ejecución.
- **US1** crea la orden; **US2** sólo opera sobre una orden pendiente.
- **Polish** termina seguridad y observabilidad.

## Notes

- La población sacrificial corresponde a la población vigente al confirmar, no a una cantidad indicada por el veterinario ni a un valor congelado al emitir.
- La orden pendiente nunca cambia por sí sola el galpón o el lote.
- En los perfiles `local` y `test`, la confirmación bloquea la orden y actualiza población, galpón y orden en una sola transacción. Si una actualización condicional falla, se revierte toda la operación. En otros perfiles los adaptadores del Módulo 1 responden `503` hasta integrar su contrato real.
- La emisión verifica el indicador vigente de la enfermedad y crea como máximo una orden por diagnóstico; la confirmación vuelve a leer la población y no admite una segunda ejecución.
