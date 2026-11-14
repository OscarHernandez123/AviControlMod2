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
│       ├── ConsultarResultadoSacrificioUseCase.java
│       ├── CorregirResultadoSacrificioUseCase.java
│       ├── MarcarResultadoUtilizadoUseCase.java
│       ├── RegistrarResultadoSacrificioUseCase.java
│       └── result/ResultadoSacrificioResult.java
└── infrastructure/
    ├── adapter/in/event/sacrificio/productivo/
    │   ├── Modulo3LiquidacionLoteEventListener.java
    │   └── ResultadoSacrificioUtilizadoV1.java
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
└── V18__crear_resultados_sacrificio_productivo.sql

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

- [x] T001 Confirmar el puerto de consulta de órdenes ejecutadas del Plan 005 y definir un contrato provisional versionado para la confirmación del Módulo 3.
- [x] T002 Crear la migración con unicidad por lote y control de versión para concurrencia.
- [x] T003 Preparar fixtures para orden ejecutada, orden programada, resultado existente y resultado utilizado.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: implementar reglas de integridad y persistencia antes de los endpoints.

**⚠️ CRITICAL**: no se registra ni corrige un resultado sin confirmar primero el estado de la orden.

- [x] T004 Crear `ResultadoSacrificioProductivo` con cantidad, peso, relaciones, fecha de registro, fecha de corrección y estado de utilización.
- [x] T005 Validar cantidad entera mayor que cero y peso mayor que cero con escala máxima de dos decimales.
- [x] T006 Definir el repositorio y consulta de orden para impedir duplicados por lote, incluso con solicitudes simultáneas.
- [x] T007 Implementar el listener que marca el resultado como utilizado de manera idempotente.

**Checkpoint**: el modelo impide valores inválidos, duplicados y correcciones de datos utilizados.

---

## Phase 3: User Story 1 - Registrar resultados finales (Priority: P1)

**Goal**: crear un resultado único sólo para una orden ejecutada y dejarlo disponible como una unidad de información para el Módulo 3.

**Independent Test**: registrar cantidad `7800` y peso `18720.50` para una orden ejecutada crea un resultado. Un segundo registro del mismo lote o una orden no ejecutada devuelve conflicto sin crear datos.

### Tests for User Story 1

- [x] T008 [P] [US1] Probar validaciones de cantidad, peso, orden ejecutada y unicidad por lote.
- [x] T009 [P] [US1] Probar concurrencia: dos solicitudes para el mismo lote sólo crean un resultado.
- [x] T010 [P] [US1] Probar con MockMvc autorización, solicitud y respuesta del endpoint.

### Implementation for User Story 1

- [x] T011 [US1] Implementar `RegistrarResultadoSacrificioUseCase` y consultar la orden antes de persistir.
- [x] T012 [US1] Crear DTO, mapper y `POST /api/ordenes-sacrificio/{ordenId}/resultados`.
- [x] T013 [US1] Publicar o dejar disponible el resultado mediante el contrato acordado con el Módulo 3, sin fallar ni duplicar si ese módulo está temporalmente fuera de servicio.

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
  "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "loteId": "b82a9df4-d61a-4b03-ac8a-2f39e74ec413",
  "cantidadFinalPollos": 7800,
  "pesoTotalKg": 18720.50,
  "estado": "DISPONIBLE",
  "fechaRegistro": "2026-11-07T14:00:00Z",
  "fechaCorreccion": null
}
```

**Checkpoint**: existe exactamente un resultado válido por lote de una orden ejecutada.

---

## Phase 4: User Story 2 - Corregir resultados antes de su utilización (Priority: P2)

**Goal**: modificar el mismo resultado mientras no se haya utilizado; bloquearlo después.

**Independent Test**: corregir un resultado disponible actualiza sus valores. Al procesar primero la confirmación de Módulo 3, la corrección devuelve conflicto y conserva valores previos.

### Tests for User Story 2

- [x] T014 [P] [US2] Probar corrección permitida, resultado utilizado y carrera entre corrección y utilización.
- [x] T015 [P] [US2] Probar por HTTP el endpoint y el estado de conflicto.

### Implementation for User Story 2

- [x] T016 [US2] Implementar `CorregirResultadoSacrificioUseCase` con bloqueo optimista o actualización condicional.
- [x] T017 [US2] Exponer `PUT /api/ordenes-sacrificio/{ordenId}/resultados` y reutilizar las validaciones de cantidad y peso.
- [x] T018 [US2] Mantener fecha de corrección y los valores actualizados disponibles para Módulo 3.

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

### Consultar resultado para liquidación

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `GET /api/ordenes-sacrificio/{ordenId}/resultados` | `ROLE_ADMINISTRADOR`, `ROLE_MODULO3` | Sin cuerpo | `200 OK` con resultado vigente y datos de lote |

### Evento provisional de utilización del Módulo 3

```json
{
  "eventId": "4e2f0145-e9c1-4e72-89a5-f631ed8d68f4",
  "eventType": "ResultadoSacrificioUtilizado",
  "eventVersion": 1,
  "occurredAt": "2026-11-07T15:00:00Z",
  "publishedAt": "2026-11-07T15:00:01Z",
  "producer": "modulo3",
  "correlationId": "f348b8df-e23c-49c1-b2c8-62ca64e7c594",
  "causationId": "c0c92a45-e45a-4e3b-9bac-26ac22f3457f",
  "aggregateType": "ResultadoSacrificio",
  "aggregateId": "b07f5fd4-3b77-4e9a-a4eb-5d54f907c8a4",
  "payload": {
    "resultadoId": "b07f5fd4-3b77-4e9a-a4eb-5d54f907c8a4",
    "loteId": "b82a9df4-d61a-4b03-ac8a-2f39e74ec413"
  }
}
```

**Errores**: `400` cantidad o peso inválidos; `401` no autenticado; `403` sin rol; `404` orden o resultado ausente; `409` orden no ejecutada, duplicado o resultado ya utilizado.

---

## Polish & Cross-Cutting Concerns

**Purpose**: completar contrato y seguridad.

- [x] T019 Traducir valores inválidos a `400`, orden o resultado ausente a `404`, orden no ejecutada, duplicado o resultado usado a `409`.
- [x] T020 Restringir registro y corrección a `ROLE_ADMINISTRADOR`, permitir consulta a `ROLE_ADMINISTRADOR` y `ROLE_MODULO3`, y probar `401` y `403`.
- [x] T021 Documentar el contrato de lectura o evento para Módulo 3 y ejecutar pruebas más `git diff --check`.

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
- El evento provisional `ResultadoSacrificioUtilizadoV1` usa el sobre estándar de [General.md](General.md) y lleva `resultadoId` y `loteId` en `payload`. La confirmación se registra una sola vez por `eventId` y bloquea correcciones mediante estado y versión. El listener Kafka permanece desactivado (`avicontrol.modulo3.resultado-utilizado.enabled=false`) hasta acordar el tópico, esquema y credenciales con el Módulo 3; el mismo contrato se puede probar mediante el listener interno.
- La respuesta GET conserva `id`, `ordenId`, `galponId`, `loteId`, `cantidadFinalPollos`, `pesoTotalKg`, `estado`, `fechaRegistro` y `fechaCorreccion`. El rol `ROLE_MODULO3` se habilitará al conectar la identidad de servicio compartida.
