# Implementation Plan: Registrar resultados finales del sacrificio

**Date**: 05/11/2026
**Spec**: [006-RegistrarPesoDePollosFinales.md](../specs/006-RegistrarPesoDePollosFinales.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Registrar una única cantidad final y peso total para una orden de sacrificio ejecutada. El resultado se asocia al lote, galpón y orden, queda disponible para el Módulo 3 y puede corregirse hasta que ese módulo confirme que lo utilizó en una liquidación.

## Technical Context

**Performance Goals**: resultado disponible para el Módulo 3 en máximo dos segundos para el 95 % de los registros.
**Constraints**: sólo `ROLE_ADMINISTRADOR`; cantidad entera positiva; peso positivo con máximo dos decimales; un resultado por lote.
**Scale/Scope**: registrar, corregir y bloquear correcciones tras la utilización externa; no calcula liquidaciones ni modifica población.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 006-RegistrarPesoDePollosFinales.md
└── plan/
    └── 006-RegistrarPesoDePollosFinales.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/sacrificio/productivo/
│   │   ├── EstadoResultadoSacrificio.java
│   │   └── ResultadoSacrificioProductivo.java
│   ├── exception/sacrificio/productivo/
│   │   ├── ResultadoSacrificioDuplicadoException.java
│   │   ├── ResultadoSacrificioNoModificableException.java
│   │   └── ValoresResultadoSacrificioInvalidosException.java
│   └── port/out/sacrificio/productivo/
│       ├── OrdenSacrificioProductivoQueryPort.java
│       └── ResultadoSacrificioRepositoryPort.java
├── application/
│   └── sacrificio/productivo/
│       ├── CorregirResultadoSacrificioUseCase.java
│       ├── RegistrarResultadoSacrificioUseCase.java
│       └── result/ResultadoSacrificioResult.java
└── infrastructure/
    ├── adapter/in/event/sacrificio/productivo/Modulo3LiquidacionLoteEventListener.java
    ├── adapter/in/rest/sacrificio/productivo/
    │   ├── ResultadosSacrificioController.java
    │   ├── dto/
    │   │   ├── CorregirResultadoSacrificioRequest.java
    │   │   ├── RegistrarResultadoSacrificioRequest.java
    │   │   └── ResultadoSacrificioResponse.java
    │   └── mapper/ResultadoSacrificioRestMapper.java
    ├── adapter/out/persistence/sacrificio/productivo/ResultadoSacrificioPersistenceAdapter.java
    └── config/ResultadoSacrificioBeanConfiguration.java

src/main/resources/db/migration/
└── V__crear_resultados_sacrificio_productivo.sql

src/test/java/com/avicontrol/
├── domain/model/sacrificio/productivo/ResultadoSacrificioProductivoTest.java
├── application/sacrificio/productivo/
│   ├── CorregirResultadoSacrificioUseCaseTest.java
│   └── RegistrarResultadoSacrificioUseCaseTest.java
└── infrastructure/adapter/in/rest/sacrificio/productivo/ResultadosSacrificioControllerTest.java
```

**Structure Decision**: el resultado y la restricción de modificación viven en el dominio y se persisten localmente. La orden se consulta mediante un puerto; el listener traduce la confirmación del Módulo 3 sin exponer su contrato al dominio.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: preparar el esquema y los contratos de la orden ejecutada y del Módulo 3.

- [ ] T001 Confirmar el puerto de consulta de órdenes ejecutadas del Plan 005 y el evento que confirma utilización en Módulo 3.
- [ ] T002 Crear la migración con unicidad por lote y control de versión para concurrencia.
- [ ] T003 Preparar fixtures para orden ejecutada, orden programada, resultado existente y resultado utilizado.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: implementar reglas de integridad y persistencia antes de los endpoints.

**⚠️ CRITICAL**: no se registra ni corrige un resultado sin confirmar primero el estado de la orden.

- [ ] T004 Crear `ResultadoSacrificioProductivo` con cantidad, peso, relaciones, fecha de registro, fecha de corrección y estado de utilización.
- [ ] T005 Validar cantidad entera mayor que cero y peso mayor que cero con escala máxima de dos decimales.
- [ ] T006 Definir el repositorio y consulta de orden para impedir duplicados por lote, incluso con solicitudes simultáneas.
- [ ] T007 Implementar el listener que marca el resultado como utilizado de manera idempotente.

**Checkpoint**: el modelo impide valores inválidos, duplicados y correcciones de datos utilizados.

---

## Phase 3: User Story 1 - Registrar resultados finales (Priority: P1)

**Goal**: crear un resultado único sólo para una orden ejecutada y dejarlo disponible como una unidad de información para el Módulo 3.

**Independent Test**: registrar cantidad `7800` y peso `18720.50` para una orden ejecutada crea un resultado. Un segundo registro del mismo lote o una orden no ejecutada devuelve conflicto sin crear datos.

### Tests for User Story 1

- [ ] T008 [P] [US1] Probar validaciones de cantidad, peso, orden ejecutada y unicidad por lote.
- [ ] T009 [P] [US1] Probar concurrencia: dos solicitudes para el mismo lote sólo crean un resultado.
- [ ] T010 [P] [US1] Probar con MockMvc autorización, solicitud y respuesta del endpoint.

### Implementation for User Story 1

- [ ] T011 [US1] Implementar `RegistrarResultadoSacrificioUseCase` y consultar la orden antes de persistir.
- [ ] T012 [US1] Crear DTO, mapper y `POST /api/ordenes-sacrificio/{ordenId}/resultados`.
- [ ] T013 [US1] Publicar o dejar disponible el resultado mediante el contrato acordado con el Módulo 3, sin fallar ni duplicar si ese módulo está temporalmente fuera de servicio.

### Request

```json
{
  "cantidadFinalPollos": 7800,
  "pesoTotalKg": 18720.50
}
```

### Response

```json
{
  "id": "b07f5fd4-3b77-4e9a-a4eb-5d54f907c8a4",
  "ordenId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "cantidadFinalPollos": 7800,
  "pesoTotalKg": 18720.50,
  "estado": "DISPONIBLE"
}
```

**Checkpoint**: existe exactamente un resultado válido por lote de una orden ejecutada.

---

## Phase 4: User Story 2 - Corregir resultados antes de su utilización (Priority: P2)

**Goal**: modificar el mismo resultado mientras no se haya utilizado; bloquearlo después.

**Independent Test**: corregir un resultado disponible actualiza sus valores. Al procesar primero la confirmación de Módulo 3, la corrección devuelve conflicto y conserva valores previos.

### Tests for User Story 2

- [ ] T014 [P] [US2] Probar corrección permitida, resultado utilizado y carrera entre corrección y utilización.
- [ ] T015 [P] [US2] Probar por HTTP el endpoint y el estado de conflicto.

### Implementation for User Story 2

- [ ] T016 [US2] Implementar `CorregirResultadoSacrificioUseCase` con bloqueo optimista o actualización condicional.
- [ ] T017 [US2] Exponer `PUT /api/ordenes-sacrificio/{ordenId}/resultados` y reutilizar las validaciones de cantidad y peso.
- [ ] T018 [US2] Mantener fecha de corrección y los valores actualizados disponibles para Módulo 3.

**Checkpoint**: una corrección nunca sobrescribe información ya usada en liquidación.

---

## Endpoints

### Registrar resultado final

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `POST /api/ordenes-sacrificio/{ordenId}/resultados` | `ROLE_ADMINISTRADOR` | `RegistrarResultadoSacrificioRequest` | `201 Created` con resultado disponible |

### Corregir resultado final

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `PUT /api/ordenes-sacrificio/{ordenId}/resultados` | `ROLE_ADMINISTRADOR` | `CorregirResultadoSacrificioRequest` | `200 OK` con resultado corregido |

**Errores**: `400` cantidad o peso inválidos; `401` no autenticado; `403` sin rol; `404` orden o resultado ausente; `409` orden no ejecutada, duplicado o resultado ya utilizado.

---

## Polish & Cross-Cutting Concerns

**Purpose**: completar contrato y seguridad.

- [ ] T019 Traducir valores inválidos a `400`, orden o resultado ausente a `404`, orden no ejecutada, duplicado o resultado usado a `409`.
- [ ] T020 Restringir ambos endpoints a `ROLE_ADMINISTRADOR` y probar `401` y `403`.
- [ ] T021 Documentar el contrato de lectura o evento para Módulo 3 y ejecutar pruebas más `git diff --check`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup**: depende del contrato del Plan 005 y Módulo 3.
- **Foundational**: bloquea registro y corrección.
- **User Story 1**: crea el resultado base.
- **User Story 2**: depende de que exista un resultado, pero conserva pruebas independientes.
- **Polish**: cierra seguridad y documentación.

## Notes

- El listener de utilización debe ser idempotente y procesar mensajes duplicados sin cambiar nuevamente el resultado.
- La disponibilidad para Módulo 3 no requiere que éste esté en línea al confirmar el registro; debe poder consultarlo o recibirlo posteriormente.
- La liquidación comercial y cualquier cambio de población están fuera de este plan.
