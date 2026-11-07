# Implementation Plan: Validar aptitud de un galpón para sacrificio

**Date**: 05/11/2026
**Spec**: [004-ValidarAptoParaSacrifico.md](../specs/004-ValidarAptoParaSacrifico.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Permitir que un administrador valide si el lote alojado actualmente en un galpón puede pasar a sacrificio. La operación vuelve a consultar el estado vigente del galpón y el lote en el Módulo 1, calcula la edad del lote y sólo cambia el galpón a `en cosecha` cuando la edad es de al menos 45 días y su estado es `productiva`.

## Technical Context

**Performance Goals**: responder y actualizar el estado cuando corresponda en máximo un segundo para al menos el 95 % de las solicitudes.
**Constraints**: requiere información completa y vigente del Módulo 1; la operación es exclusiva de `ROLE_ADMINISTRADOR` y no puede modificar el lote.
**Scale/Scope**: una validación para un galpón por solicitud; no crea ni programa órdenes de sacrificio.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 004-ValidarAptoParaSacrifico.md
└── plan/
    └── 004-ValidarAptoParaSacrifico.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/sacrificio/productivo/
│   │   ├── EstadoGalpon.java
│   │   └── ValidacionAptitudSacrificio.java
│   ├── exception/sacrificio/productivo/
│   │   ├── GalponNoAptoParaSacrificioException.java
│   │   ├── InformacionAptitudNoDisponibleException.java
│   │   └── LoteActualNoEncontradoException.java
│   └── port/out/modulo1/
│       ├── GalponQueryPort.java
│       ├── GalponUpdatePort.java
│       └── LoteQueryPort.java
├── application/
│   └── sacrificio/productivo/
│       ├── ValidarAptitudSacrificioCommand.java
│       ├── ValidarAptitudSacrificioUseCase.java
│       └── result/ValidacionAptitudResult.java
└── infrastructure/
    ├── adapter/in/rest/sacrificio/productivo/
    │   ├── AptitudSacrificioController.java
    │   ├── dto/ValidacionAptitudSacrificioResponse.java
    │   └── mapper/AptitudSacrificioRestMapper.java
    ├── adapter/out/integration/modulo1/
    │   ├── GalponQueryAdapter.java
    │   ├── GalponUpdateAdapter.java
    │   └── LoteQueryAdapter.java
    └── config/SacrificioProductivoBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/sacrificio/productivo/ValidacionAptitudSacrificioTest.java
├── application/sacrificio/productivo/ValidarAptitudSacrificioUseCaseTest.java
└── infrastructure/adapter/in/rest/sacrificio/productivo/AptitudSacrificioControllerTest.java
```

**Structure Decision**: la regla que determina la aptitud vive en el dominio. Los puertos de salida aíslan los contratos del Módulo 1; los adaptadores son los únicos componentes que conocen cómo consultar o actualizar allí el galpón.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: confirmar contratos, autorización y datos de prueba antes de implementar la validación.

- [ ] T001 Verificar en el Módulo 1 los contratos para obtener el galpón, el lote actual y actualizar el estado del galpón.
- [ ] T002 Confirmar la representación de los estados `productiva` y `en cosecha` en el contrato externo.
- [ ] T003 Preparar fixtures para lote de 45 días, lote de 44 días, galpón no productivo, galpón sin lote e indisponibilidad del Módulo 1.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: construir las reglas y puertos que bloquean la historia de usuario.

**⚠️ CRITICAL**: no se inicia el endpoint hasta completar esta fase.

- [ ] T004 Crear `ValidacionAptitudSacrificio` con las reglas de edad mínima de 45 días y estado `productiva`.
- [ ] T005 Hacer que la validación acumule todas las razones de no aptitud en lugar de detenerse en la primera.
- [ ] T006 Definir `GalponQueryPort`, `LoteQueryPort` y `GalponUpdatePort` con modelos de aplicación o dominio, nunca DTO HTTP.
- [ ] T007 Implementar excepciones para lote ausente e información incompleta o no disponible.
- [ ] T008 Implementar los adaptadores del Módulo 1 y la configuración de beans.

**Checkpoint**: la validación recibe información vigente mediante puertos y sólo puede actualizar un galpón apto.

---

## Phase 3: User Story 1 - Validar la aptitud de un galpón para sacrificio (Priority: P1)

**Goal**: mostrar `Apto` o `No apto` con todas las razones y cambiar el estado a `en cosecha` únicamente al cumplirse ambas condiciones.

**Independent Test**: con un galpón `productiva` y un lote ingresado hace 44 días, la respuesta debe ser apta y el adaptador de actualización debe recibir `en cosecha`. Con 43 días o con un estado distinto, no debe invocarse la actualización.

### Tests for User Story 1

- [ ] T009 [P] [US1] Probar la regla de dominio para los cuatro escenarios: apto, edad insuficiente, estado inválido e incumplimiento simultáneo.
- [ ] T010 [P] [US1] Probar el caso de uso con mocks de los tres puertos del Módulo 1.
- [ ] T011 [P] [US1] Crear pruebas MockMvc del endpoint, autorización y contrato JSON.

### Implementation for User Story 1

- [ ] T012 [US1] Implementar `ValidarAptitudSacrificioUseCase` para volver a consultar galpón y lote en cada solicitud.
- [ ] T013 [US1] Calcular la edad con la fecha de ingreso vigente y evaluar las dos condiciones sin reutilizar una consulta anterior del cliente.
- [ ] T014 [US1] Actualizar el estado del galpón a `en cosecha` sólo cuando el resultado sea apto.
- [ ] T015 [US1] Crear `ValidacionAptitudSacrificioResponse` y `AptitudSacrificioRestMapper`.
- [ ] T016 [US1] Exponer el endpoint y restringirlo a `ROLE_ADMINISTRADOR`.

## Endpoints

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `POST /api/galpones/{galponId}/validacion-aptitud-sacrificio` | `ROLE_ADMINISTRADOR` | Sin cuerpo | `200 OK` con aptitud, motivos y estado resultante |

**Errores**: `400` UUID inválido; `401` no autenticado; `403` sin rol; `404` galpón inexistente; `409` información inconsistente; `503` Módulo 1 no disponible.

### Ejemplo de operación

```http
POST /api/galpones/{galponId}/validacion-aptitud-sacrificio
Authorization: Bearer <token>
```

### Response: galpón apto

```json
{
  "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "loteId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "apto": true,
  "motivosNoAptitud": [],
  "estadoGalponResultante": "en cosecha"
}
```

### Response: galpón no apto

```json
{
  "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "loteId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "apto": false,
  "motivosNoAptitud": [
    "El lote no cumple la edad mínima de 45 días",
    "El galpón no se encuentra en estado productiva"
  ],
  "estadoGalponResultante": "mantenimiento"
}
```

**Checkpoint**: el administrador puede validar el galpón de forma independiente y sólo una respuesta apta cambia su estado.

---

## Polish & Cross-Cutting Concerns

**Purpose**: reforzar el contrato, seguridad y mantenibilidad.

- [ ] T017 Añadir manejo HTTP para `400` UUID inválido, `401` sin autenticación, `403` sin rol, `404` galpón inexistente, `409` información inconsistente y `503` Módulo 1 no disponible.
- [ ] T018 Verificar que una falla durante la consulta no ejecute actualización alguna en el Módulo 1.
- [ ] T019 Documentar en OpenAPI el endpoint, sus respuestas y el rol requerido.
- [ ] T020 Ejecutar pruebas unitarias, pruebas HTTP y `git diff --check`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no tiene dependencias.
- **Foundational (Phase 2)**: depende de los contratos confirmados y bloquea la historia de usuario.
- **User Story 1 (Phase 3)**: depende de las reglas, puertos y adaptadores fundamentales.
- **Polish**: depende de la historia de usuario completa.

### Within the User Story

1. Consultar de nuevo galpón y lote.
2. Calcular edad y evaluar condiciones en el dominio.
3. Actualizar el estado sólo si el resultado es apto.
4. Mapear el resultado y exponerlo por HTTP.
5. Validar escenarios y errores mediante pruebas.

## Notes

- No se persiste una validación local ni se crea una migración: la especificación sólo exige cambiar el estado del galpón apto en el Módulo 1.
- La edad debe calcularse con el mismo criterio inclusivo definido en el Plan 003.
- La ausencia de lote, un dato incompleto o una dependencia no disponible impiden la validación y no modifican galpón ni lote.
