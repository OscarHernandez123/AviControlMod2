# Implementation Plan: Registro de Resultados del Sacrificio Productivo

**Date**: 25/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [006-RegistrarPesoDePollosFinales.md](../specs/006-RegistrarPesoDePollosFinales.md)

## Summary

Registrar los resultados finales del lote después de ejecutar una orden de sacrificio productivo, publicar el peso y la cantidad para la liquidación del Módulo 3 y permitir correcciones auditadas hasta que Finanzas confirme su uso.

## Technical Context

**Performance Goals**: El 95 % de registros y correcciones queda disponible para Módulo 3 en máximo 2 segundos.

**Constraints**: Solo `ROLE_ADMINISTRADOR`; la orden debe estar ejecutada; cantidad entera y peso positivo con dos decimales; un único resultado por orden/lote; corrección permitida solo en `DISPONIBLE_PARA_LIQUIDACION`; confirmación de liquidación bloquea cambios.

**Scale/Scope**: SPEC-006, dos historias (registrar y corregir resultado); dos endpoints REST y un listener de confirmación de Módulo 3.

**Dependencias funcionales**: Plan 009 provee órdenes ejecutadas; Módulo 3 consume y confirma uso de datos finales; General.md define autenticación, actor, eventos, errores, concurrencia y outbox.

### Decisiones específicas

1. `ResultadoSacrificioProductivo` queda asociado de forma única a una orden ejecutada y conserva `ordenId`, `loteId` y `galponId`.
2. El peso promedio es derivado de `pesoTotalKg / cantidadFinalPollos` con precisión definida en SPEC-006.
3. Cada corrección conserva actor, fecha y valores anteriores/nuevos para auditoría; no se exige justificación y no se elimina el historial.
4. Resultado disponible se publica mediante contrato versionado/outbox para Módulo 3.
5. El listener de liquidación cambia el estado a `UTILIZADO_EN_LIQUIDACION`; bloqueo optimista resuelve carreras contra correcciones.

### Alcance documental

- Este plan implementa SPEC-006.
- [Plan 009](009-AptitudProgramacionYResultadosDelSacrificioProductivo.md) es dueño de órdenes comerciales y ejecución.
- Módulo 3 liquida y notifica cuándo consumió los resultados.

## Project Structure

### Documentation (this feature)

```text
docs/specs/006-RegistrarPesoDePollosFinales.md
docs/plan/016-ResultadosDelSacrificioProductivo.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/model/sacrificio/productivo/
│   ├── ResultadoSacrificioProductivo.java
│   └── EstadoResultadoSacrificio.java
├── domain/port/out/sacrificio/productivo/
│   ├── ResultadoSacrificioProductivoRepositoryPort.java
│   └── OrdenSacrificioProductivoQueryPort.java
├── application/sacrificio/productivo/
│   ├── RegistrarResultadosSacrificioUseCase.java
│   └── CorregirResultadosSacrificioUseCase.java
└── infrastructure/
    ├── adapter/in/rest/sacrificio/productivo/ResultadosSacrificioController.java
    ├── adapter/in/event/sacrificio/productivo/Modulo3LiquidacionLoteEventListener.java
    ├── adapter/out/persistence/sacrificio/productivo/
    └── config/ResultadoSacrificioBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/sacrificio/productivo/
├── application/sacrificio/productivo/
└── integration/sacrificio/productivo/ResultadosSacrificioIntegrationTest.java
```

**Structure Decision**: Este plan es dueño del resultado final. La orden se consulta al Plan 009; el evento y el estado de liquidación coordinan la integración con Módulo 3.

### Entidades y relación

```text
OrdenSacrificioProductivo (Plan 009, EJECUTADA) 1 ─── 0..1 ResultadoSacrificioProductivo

ResultadoSacrificioProductivo: id, ordenId, loteId, galponId,
  cantidadFinalPollos, pesoTotalKg, pesoPromedioAveKg,
  registradoPor, registradoEn, corregidoPor?, corregidoEn?,
  estadoUtilizacion, version
```

### Contratos de los puertos

| Puerto | Responsabilidad |
| --- | --- |
| `ResultadoSacrificioProductivoRepositoryPort` | Persistir y consultar el resultado único por orden, y corregirlo bajo control de versión. |
| `OrdenSacrificioProductivoQueryPort` | Confirmar que la orden existe, está ejecutada y proveer IDs de lote/galpón. |
| `IntegrationEventPublisherPort` | Publicar resultado confirmado para liquidación mediante outbox. |

### Contratos HTTP propuestos

| Método y ruta | Acceso | Responsabilidad |
| --- | --- | --- |
| `POST /api/ordenes-sacrificio/{ordenId}/resultados` | `ROLE_ADMINISTRADOR` | Registrar peso y cantidad final una vez. |
| `PUT /api/ordenes-sacrificio/{ordenId}/resultados` | `ROLE_ADMINISTRADOR` | Corregir resultado no utilizado. |

Errores siguen RFC 9457 y General.md; orden no ejecutada o resultado bloqueado responde 409.

### JSON común de errores

```json
{
  "type": "https://avicontrol/errors/resultado-sacrificio-bloqueado",
  "title": "Resultado bloqueado",
  "status": 409,
  "detail": "El resultado ya fue utilizado en la liquidación",
  "instance": "/api/ordenes-sacrificio/{ordenId}/resultados",
  "code": "RESULTADO_YA_UTILIZADO_EN_LIQUIDACION",
  "correlationId": "uuid",
  "fieldErrors": []
}
```

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Acordar contrato de consulta de orden ejecutada con Plan 009.
- [ ] T002 Acordar contrato de resultados y confirmación de liquidación con Módulo 3.
- [ ] T003 Definir precisión, redondeo, auditoría, roles y errores conforme a SPEC-006/General.md.

**Checkpoint**: Contratos de resultado y liquidación definidos.

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T004 Implementar modelo y validaciones de `ResultadoSacrificioProductivo`.
- [ ] T005 Definir puertos de orden, persistencia y publicación.
- [ ] T006 Crear migración propia para resultados con unicidad por orden y control de versión.
- [ ] T007 Implementar adaptadores, mappers y bloqueo optimista.
- [ ] T008 Configurar transacción y outbox para publicación confiable.

**Checkpoint**: Persistencia y contratos listos para registro/corrección.
## Phase 3: User Story 1 — Registrar Peso Total y Cantidad Final de Pollos Sacrificados (Priority: P1)

**Spec**: SPEC-006, historia 1.

**Goal**: El administrador registra la cantidad final entera y el peso total en kilogramos de los pollos sacrificados de una orden ejecutada, dejándolos disponibles para la liquidación del Módulo 3.

**Independent Test**: Para una orden ejecutada, registrar 4.850 pollos y 12.125,50 kg; verificar creación del resultado con estado `DISPONIBLE_PARA_LIQUIDACION`, cálculo de peso promedio (2.500 kg/ave) y publicación a Kafka. Intentar registrar sobre orden programada o cancelada y comprobar rechazo.

### Definición del endpoint REST para User Story 1

| Elemento | Definición |
| --- | --- |
| Método y ruta | `POST /api/ordenes-sacrificio/{ordenId}/resultados` |
| Autorización | `ROLE_ADMINISTRADOR` |
| Entrada (Body) | `cantidadFinalPollos` (int > 0), `pesoTotalKg` (decimal > 0, máx 2 decimales) |
| Respuesta 201 | `ResultadoSacrificioResponse` con datos registrados y peso promedio |
| Errores | 400 por valores inválidos; 409 si la orden no está ejecutada o si el lote ya tiene resultado |

#### JSON de solicitud (Request)

```json
{
  "cantidadFinalPollos": 4850,
  "pesoTotalKg": 12125.50
}
```

#### JSON de respuesta exitosa (Response 201)

```json
{
  "id": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
  "ordenId": "8a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d",
  "loteId": "2fc03a21-84c4-4c83-bb15-6607bb75cb90",
  "galponId": "550e8400-e29b-41d4-a716-446655440001",
  "cantidadFinalPollos": 4850,
  "pesoTotalKg": 12125.50,
  "pesoPromedioAveKg": 2.500,
  "estadoUtilizacion": "DISPONIBLE_PARA_LIQUIDACION",
  "registradoPor": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "registradoEn": "2026-10-08T10:15:00Z"
}
```

### Tests para User Story 1

- [ ] T011 [P] [US1] Unit test en `RegistrarResultadosSacrificioUseCaseTest`: orden ejecutada, pesajes válidos, cálculo de promedio y bloqueo ante segundo registro duplicado.
- [ ] T012 [P] [US1] Contract test en `SacrificioProductivoControllerTest`.

### Implementación para User Story 1

- [ ] T013 [P] [US1] Implementar DTOs `RegistrarResultadosRequest`, `ResultadoSacrificioResponse` y mappers.
- [ ] T014 [P] [US1] Implementar `RegistrarResultadosSacrificioUseCase` persistiendo en `ResultadoSacrificioProductivoRepositoryPort` y publicando a Kafka.
- [ ] T015 [US1] Implementar endpoint `POST /api/ordenes-sacrificio/{ordenId}/resultados`.

**Checkpoint**: Captura de resultados comerciales habilitada para Módulo 3.

---

## Phase 4: User Story 2 — Corregir Resultados Finales antes de su Utilización por Módulo 3 (Priority: P2)

**Spec**: SPEC-006, historia 2.

**Goal**: El administrador corrige errores de digitación en peso o cantidad mientras el estado sea `DISPONIBLE_PARA_LIQUIDACION`. Una vez utilizado por Módulo 3 (`UTILIZADO_EN_LIQUIDACION`), la corrección se bloquea definitivamente.

**Independent Test**: Corregir un resultado disponible; verificar que actualice valores y registre fecha y usuario de corrección. Marcar el resultado como utilizado en liquidación contable e intentar una corrección posterior; comprobar el rechazo con `409 Conflict`.

### Definición del endpoint REST para User Story 2

- `PUT /api/ordenes-sacrificio/{ordenId}/resultados` (Body: `cantidadFinalPollos`, `pesoTotalKg`)

### Tests para User Story 2

- [ ] T016 [P] [US2] Unit test en `CorregirResultadosSacrificioUseCaseTest`: corrección permitida ante `DISPONIBLE_PARA_LIQUIDACION` y rechazo estricto ante `UTILIZADO_EN_LIQUIDACION`.
- [ ] T017 [US2] Integration test probando concurrencia entre solicitud de corrección y evento de liquidación contable de Módulo 3.

### Implementación para User Story 2

- [ ] T018 [P] [US2] Implementar `CorregirResultadosSacrificioUseCase` consultando el resultado por orden y aplicando control de versión.
- [ ] T019 [US2] Implementar listener `Modulo3LiquidacionLoteEventListener` para transicionar a `UTILIZADO_EN_LIQUIDACION`.
- [ ] T020 [US2] Implementar el endpoint `PUT` en `SacrificioProductivoController`.

**Checkpoint**: Blindaje de integridad contable con ventana de corrección operativa.

---

## Phase 5: Polish & Cross-Cutting Concerns

- [ ] T021 Documentar los dos endpoints, integración y RFC 9457 en OpenAPI.
- [ ] T022 Probar la carrera entre corrección y confirmación de liquidación.
- [ ] T023 Verificar publicación idempotente por outbox y criterios de SPEC-006.

## Dependencies & Execution Order

### Phase Dependencies

- Setup precede a Foundational.
- Registrar resultado depende de que Plan 009 haya ejecutado la orden.
- Corregir/consultar depende de resultado registrado y respeta su estado de utilización.
- Polish depende del contrato Módulo 3 y ambas historias.

### Dependencias con otros planes

- **Plan 009**: Propietario de aptitud, programación y ejecución de órdenes; provee identificador de orden ejecutada.
- **Módulo 3**: Consume resultados y comunica la confirmación de liquidación.
- **General.md**: Seguridad, actor, concurrencia, eventos, errores y outbox.

### Dentro de cada User Story

- Validar estado de orden y resultado antes de escribir.
- Conservar auditoría completa y publicar solo después de confirmar.
- Probar el bloqueo concurrente con evento de Módulo 3.

## Notes

- Este plan no programa ni ejecuta sacrificios.
- Peso y cantidad se conservan en el registro; este plan no realiza liquidación contable.
- Las correcciones quedan bloqueadas al confirmarse su uso por Módulo 3.
