# Implementation Plan: Registro de recepción de medicamento

**Date**: 05/11/2026
**Spec**: [002-RegistroRecepcionDeMedicamento.md](../specs/002-RegistroRecepcionDeMedicamento.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Implementar el registro de una recepción comercial de medicamento. El administrador informa lote, vencimiento, presentaciones, contenido y precio; el sistema normaliza la cantidad a la unidad base del medicamento y confirma la entrada inicial con Inventario dentro de la misma transacción local.

## Technical Context

**Performance Goals**: El 95 % de registros válidos responde en menos de 1 segundo.
**Constraints**: Solo `ROLE_ADMINISTRADOR`; todos los valores comerciales usan `BigDecimal`; las unidades permitidas son `GRAMO`, `MILILITRO` y `UNIDAD`; no se mezclan dimensiones incompatibles.
**Scale/Scope**: SPEC-002; una historia, un endpoint POST, una entidad de recepción y una entrada inicial de medicamento.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 002-RegistroRecepcionDeMedicamento.md
└── plan/
    └── 002-RegistroRecepcionDeMedicamento.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/catalogo/UnidadMedidaMedicamento.java
│   ├── model/recepcion/
│   │   ├── EstadoRecepcion.java
│   │   └── RecepcionMedicamento.java
│   ├── exception/recepcion/
│   │   ├── CantidadInvalidaException.java
│   │   ├── FechaVencimientoInvalidaException.java
│   │   └── UnidadIncompatibleException.java
│   └── port/out/recepcion/RecepcionMedicamentoRepositoryPort.java
├── application/
│   ├── inventario/port/RegistrarEntradaInventarioPort.java
│   └── recepcion/
│       ├── RegistrarRecepcionMedicamentoCommand.java
│       └── RegistrarRecepcionMedicamentoUseCase.java
└── infrastructure/
    ├── adapter/in/rest/recepcion/
    │   ├── RecepcionMedicamentoController.java
    │   ├── dto/
    │   │   ├── RecepcionMedicamentoResponse.java
    │   │   └── RegistrarRecepcionMedicamentoRequest.java
    │   └── mapper/RecepcionMedicamentoRestMapper.java
    ├── adapter/out/persistence/recepcion/RecepcionMedicamentoPersistenceAdapter.java
    └── config/RecepcionBeanConfiguration.java

src/main/resources/db/migration/V__crear_recepciones_medicamento.sql

src/test/java/com/avicontrol/
├── domain/model/recepcion/RecepcionMedicamentoTest.java
├── application/recepcion/RegistrarRecepcionMedicamentoUseCaseTest.java
└── infrastructure/adapter/in/rest/recepcion/RecepcionMedicamentoControllerTest.java
```

**Structure Decision**: Recepción conserva el hecho comercial, presentación, contenido y vencimiento. Inventario recibe únicamente la cantidad normalizada y conserva el movimiento y saldo. Ninguna capa REST expone entidades de dominio o JPA.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Definir contrato de normalización y fixtures propios de la recepción de medicamento.

- [ ] T001 Acordar con Catálogo la unidad base y presentación compatible del medicamento.
- [ ] T002 Acordar con Inventario el comando de entrada de medicamento y sus unidades permitidas.
- [ ] T003 Preparar fixtures para gramos, miligramos, mililitros, unidades y dimensiones incompatibles.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Implementar normalización, persistencia y atomicidad antes del endpoint.

- [ ] T004 Crear `RecepcionMedicamento` y sus reglas de cantidad, vencimiento y unidad base.
- [ ] T005 Implementar conversión de miligramos a gramos sin convertir masa, volumen y conteo entre sí.
- [ ] T006 Crear puerto, migración Flyway y adaptador de persistencia.
- [ ] T007 Implementar comando y caso de uso transaccional con actor y puerto de Inventario.
- [ ] T008 Configurar errores 400, 404, 409 y 503 de acuerdo con `General.md`.

**Checkpoint**: La recepción y su entrada normalizada se confirman o revierten juntas, sin HTTP.

---

## Phase 3: User Story 1 - Registrar recepción de medicamento (Priority: P1)

**Goal**: Registrar medicamento trazable por lote, presentación y vencimiento para que Inventario pueda administrarlo por unidad base.

**Independent Test**: Una recepción en mililitros responde 201 y registra la cantidad normalizada; una unidad incompatible o un fallo de Inventario no deja ningún registro confirmado.

## Endpoints

### Registrar recepción de medicamento

| Elemento | Definición |
| --- | --- |
| Método y ruta | `POST /api/recepciones/medicamentos` |
| Autorización | `ROLE_ADMINISTRADOR` |
| Respuesta | `201 Created`, encabezado `Location` y recepción confirmada |
| Errores | `400` datos/cantidad, `404` medicamento, `409` vencimiento/unidad incompatible, `503` Inventario |

#### JSON de solicitud

```json
{
  "medicamentoId": "550e8400-e29b-41d4-a716-446655440000",
  "codigoLote": "MED-2026-001",
  "fechaIngreso": "2026-11-05",
  "fechaVencimiento": "2027-05-05",
  "cantidadPresentaciones": 20,
  "contenidoPorPresentacion": 500,
  "unidadContenido": "MILILITRO",
  "precioNetoPorPresentacion": 45000,
  "impuestoPorcentaje": 19
}
```

#### JSON de respuesta

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "estado": "CONFIRMADA",
  "codigoLote": "MED-2026-001",
  "cantidadPresentaciones": 20,
  "cantidadUnidadBase": 10000,
  "unidadBase": "MILILITRO",
  "precioNetoTotal": 900000,
  "fechaIngreso": "2026-11-05",
  "fechaVencimiento": "2027-05-05"
}
```

### Tests for User Story 1

- [ ] T009 [P] [US1] Probar conversión mg-g, cantidades positivas, vencimiento y dimensiones incompatibles en `RecepcionMedicamentoTest`.
- [ ] T010 [P] [US1] Probar éxito, medicamento inexistente y reversión atómica en `RegistrarRecepcionMedicamentoUseCaseTest`.
- [ ] T011 [P] [US1] Probar 201, 400, 403, 404, 409 y 503 en `RecepcionMedicamentoControllerTest`.

### Implementation for User Story 1

- [ ] T012 [US1] Crear DTOs REST y mapper de request, comando y response.
- [ ] T013 [US1] Implementar el endpoint POST con Bean Validation y `Location`.
- [ ] T014 [US1] Documentar esquemas, unidad base, rol y errores en OpenAPI.

**Checkpoint**: La recepción de medicamento es independiente, trazable y verificable.

---

## Phase 4: Polish & Cross-Cutting Concerns

- [ ] T015 Verificar que no se acceda a repositorios privados de Catálogo ni Inventario.
- [ ] T016 Ejecutar pruebas, formato y reglas de arquitectura mediante Gradle Wrapper.
- [ ] T017 Verificar auditoría del actor, `correlationId` y logs sin contenido clínico o comercial sensible.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup** inicia al acordar unidades y contrato de entrada.
- **Foundational** depende de Setup y bloquea la historia.
- **User Story 1** depende de Foundational.
- **Polish** depende de la historia completa.

### Within Each User Story

- Reglas de unidad y dominio antes del caso de uso.
- Puertos y transacción antes de REST.
- DTOs y mapper antes del controlador.
- Pruebas de dominio, aplicación y HTTP antes del checkpoint.

## Notes

- Este plan no edita, lista ni consume medicamentos.
- La recepción no es dueña de movimientos ni de saldos de inventario.
- Si cambia la unidad base del catálogo, la recepción histórica no se reescribe.
