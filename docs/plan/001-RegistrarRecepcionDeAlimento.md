# Implementation Plan: Registrar recepción de alimento

**Date**: 05/11/2026
**Spec**: [001-RegistrarRecepcionDeAlimento.md](../specs/001-RegistrarRecepcionDeAlimento.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

El administrador registra una recepción comercial de alimento. La recepción conserva datos históricos y cálculos comerciales; Inventario confirma la entrada inicial en kg dentro de la misma transacción local.

## Technical Context

**Performance Goals**: 95 % de registros válidos en menos de 1 segundo.
**Constraints**: `ROLE_ADMINISTRADOR`; `BigDecimal`; vencimiento posterior al ingreso; sin recepción confirmada parcialmente.
**Scale/Scope**: SPEC-001; una historia, un POST y un contrato de entrada a Inventario.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/001-RegistrarRecepcionDeAlimento.md
└── plan/001-RegistrarRecepcionDeAlimento.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/recepcion/
│   │   ├── RecepcionAlimento.java
│   │   └── EstadoRecepcion.java
│   ├── exception/recepcion/
│   │   ├── CantidadInvalidaException.java
│   │   └── FechaVencimientoInvalidaException.java
│   └── port/out/recepcion/RecepcionAlimentoRepositoryPort.java
├── application/
│   ├── recepcion/
│   │   ├── RegistrarRecepcionAlimentoCommand.java
│   │   └── RegistrarRecepcionAlimentoUseCase.java
│   └── inventario/port/RegistrarEntradaInventarioPort.java
└── infrastructure/
    ├── adapter/in/rest/recepcion/
    │   ├── RecepcionAlimentoController.java
    │   ├── dto/
    │   │   ├── RegistrarRecepcionAlimentoRequest.java
    │   │   └── RecepcionAlimentoResponse.java
    │   └── mapper/RecepcionAlimentoRestMapper.java
    └── adapter/out/persistence/recepcion/RecepcionAlimentoPersistenceAdapter.java
src/main/resources/db/migration/V__crear_recepciones_alimento.sql
src/test/java/com/avicontrol/
├── domain/model/recepcion/
│   └── RecepcionAlimentoTest.java
├── application/recepcion/
│   └── RegistrarRecepcionAlimentoUseCaseTest.java
└── infrastructure/adapter/in/rest/recepcion/
    └── RecepcionAlimentoControllerTest.java
```

**Structure Decision**: Recepción es dueña del hecho comercial; Inventario es dueño de movimientos y saldos. DTO, dominio y persistencia se mantienen separados.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Acordar contratos y fixtures de esta recepción.

- [ ] T001 Acordar `AlimentoCatalogoQueryPort` y `RegistrarEntradaInventarioPort` (kg, actor y fallo).
- [ ] T002 Crear fixtures de alimento activo y casos válidos/inválidos de fecha, cantidad y precio.
- [ ] T003 Centralizar escala y redondeo de bultos, kg, precio e impuesto.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Construir dominio, persistencia y transacción antes del endpoint.

- [ ] T004 Crear `RecepcionAlimento` y reglas de cantidad, vencimiento, kg y precio total.
- [ ] T005 Crear puerto, migración Flyway y adaptador de persistencia.
- [ ] T006 Implementar comando y caso de uso transaccional con actor.
- [ ] T007 Confirmar o revertir recepción y entrada inicial como una sola operación.
- [ ] T008 Configurar errores 400, 404, 409 y 503 según `General.md`.

**Checkpoint**: Dominio y entrada inicial confirmables sin HTTP.

---

## Phase 3: User Story 1 - Registrar recepción de alimento (Priority: P1)

**Goal**: Registrar una entrega trazable y disponible para Inventario.

**Independent Test**: Con alimento activo y datos válidos responde 201 e invoca una sola entrada; si Inventario falla, no persiste recepción.

## Endpoints

### Registrar recepción de alimento

| Elemento | Definición |
| --- | --- |
| Método y ruta | `POST /api/recepciones/alimentos` |
| Autorización | `ROLE_ADMINISTRADOR` |
| Respuesta | `201 Created`, `Location` y representación confirmada |
| Errores | `400` campos/cantidades, `404` alimento, `409` vencimiento/duplicado, `503` Inventario |

#### JSON de solicitud

```json
{
  "alimentoId": "550e8400-e29b-41d4-a716-446655440000",
  "codigoLote": "AL-2026-001",
  "fechaIngreso": "2026-11-05",
  "fechaVencimiento": "2027-03-05",
  "cantidadBultos": 40,
  "pesoPorBultoKg": 40,
  "precioNetoPorBulto": 82000,
  "impuestoPorcentaje": 19
}
```

#### JSON de respuesta

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "estado": "CONFIRMADA",
  "codigoLote": "AL-2026-001",
  "cantidadBultos": 40,
  "cantidadTotalKg": 1600,
  "precioNetoTotal": 3280000,
  "fechaIngreso": "2026-11-05",
  "fechaVencimiento": "2027-03-05"
}
```

### Tests for User Story 1

- [ ] T009 [P] [US1] Probar cantidades, vencimiento y cálculos en `RecepcionAlimentoTest`.
- [ ] T010 [P] [US1] Probar éxito, alimento inexistente y reversión atómica en `RegistrarRecepcionAlimentoUseCaseTest`.
- [ ] T011 [P] [US1] Probar 201, 400, 403, 404, 409 y 503 en `RecepcionAlimentoControllerTest`.

### Implementation for User Story 1

- [ ] T012 [US1] Crear DTOs REST y mapper sin exponer el dominio.
- [ ] T013 [US1] Implementar POST, validación Bean Validation y `Location`.
- [ ] T014 [US1] Documentar operación, rol, esquemas y errores en OpenAPI.

**Checkpoint**: La historia funciona y se prueba independientemente.

---

## Phase 4: Polish & Cross-Cutting Concerns

- [ ] T015 Verificar que Recepción no acceda a repositorios privados de Inventario.
- [ ] T016 Ejecutar Gradle, formato y reglas de arquitectura.
- [ ] T017 Verificar auditoría de actor, `correlationId` y logs sin datos sensibles.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup** inicia con contratos acordados.
- **Foundational** depende de Setup y bloquea la historia.
- **User Story 1** depende de Foundational.
- **Polish** depende de la historia completa.

### Within Each User Story

- Dominio antes de caso de uso; puertos/transacción antes de REST; DTOs antes de controlador.
- Pruebas de dominio, aplicación y HTTP antes del checkpoint.

## Notes

- Este plan no edita ni lista recepciones.
- Un contrato incompatible de Inventario requiere actualizar ambos planes antes de código.
