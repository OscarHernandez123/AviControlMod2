# Implementation Plan: Registrar, editar y consultar recepciones de medicamento

**Date**: 05/11/2026
**Spec**: [002-RegistroRecepcionDeMedicamento.md](../specs/002-RegistroRecepcionDeMedicamento.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

El administrador registra entregas de medicamento con presentación, contenido, unidad y precio histórico. El sistema normaliza la cantidad a la unidad base compatible y confirma una entrada de Inventario. Puede editar siete campos antes de que existan movimientos posteriores confirmados, y consultar historial, indicador mensual y detalle.

## Technical Context

**Performance Goals**: El 95 % de registros y consultas válidos responde en menos de 2 segundos.
**Constraints**: `ROLE_ADMINISTRADOR`; `BigDecimal`; unidades `MILIGRAMO`, `GRAMO`, `MILILITRO` y `UNIDAD`; masa normalizada a gramos; vencimiento posterior al ingreso; lote repetido permitido; lote, presentación y fecha de ingreso protegidos al editar.
**Scale/Scope**: SPEC-002, historias US1, US2 y US3. La entrada, su ajuste y la auditoría comparten transacción con Recepción.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/002-RegistroRecepcionDeMedicamento.md
└── plan/002-RegistroRecepcionDeMedicamento.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/catalogo/UnidadMedidaMedicamento.java
│   ├── model/recepcion/
│   │   ├── EstadoRecepcion.java
│   │   ├── FiltroRecepciones.java
│   │   ├── PaginaRecepciones.java
│   │   ├── RecepcionCalculos.java
│   │   └── RecepcionMedicamento.java
│   ├── exception/recepcion/
│   │   ├── CantidadInvalidaException.java
│   │   ├── FechaVencimientoInvalidaException.java
│   │   ├── RecepcionNoEditableException.java
│   │   ├── RecursoNoEncontradoException.java
│   │   └── UnidadIncompatibleException.java
│   └── port/out/recepcion/RecepcionMedicamentoRepositoryPort.java
├── application/
│   ├── inventario/port/
│   │   ├── AjustarEntradaInventarioPort.java
│   │   └── RegistrarEntradaInventarioPort.java
│   └── recepcion/
│       ├── AuditoriaRecepcionPort.java
│       ├── CatalogoRecepcionPort.java
│       ├── ConsultarRecepcionesMedicamentoUseCase.java
│       ├── EditarRecepcionMedicamentoCommand.java
│       ├── EditarRecepcionMedicamentoUseCase.java
│       ├── HistorialRecepcionesMedicamento.java
│       ├── RegistrarRecepcionMedicamentoCommand.java
│       └── RegistrarRecepcionMedicamentoUseCase.java
└── infrastructure/
    ├── adapter/in/rest/recepcion/
    │   ├── RecepcionMedicamentoController.java
    │   ├── dto/
    │   │   ├── EditarRecepcionMedicamentoRequest.java
    │   │   ├── HistorialMedicamentoResponse.java
    │   │   ├── RecepcionMedicamentoResponse.java
    │   │   ├── RecepcionMedicamentoResumenResponse.java
    │   │   └── RegistrarRecepcionMedicamentoRequest.java
    │   └── mapper/
    │       ├── RecepcionFiltroRestMapper.java
    │       └── RecepcionMedicamentoRestMapper.java
    ├── adapter/out/persistence/inventario/
    │   ├── AjusteEntradaInventarioPersistenceAdapter.java
    │   ├── EntradaInventarioPersistenceAdapter.java
    │   ├── MovimientoInventarioEntity.java
    │   └── MovimientoInventarioJpaRepository.java
    ├── adapter/out/persistence/recepcion/
    │   ├── AuditoriaRecepcionEntity.java
    │   ├── AuditoriaRecepcionJpaRepository.java
    │   ├── AuditoriaRecepcionPersistenceAdapter.java
    │   ├── RecepcionMedicamentoEntity.java
    │   ├── RecepcionMedicamentoJpaRepository.java
    │   └── RecepcionMedicamentoPersistenceAdapter.java
    └── config/RecepcionBeanConfiguration.java

src/main/resources/db/migration/
├── V2__crear_recepciones_medicamento.sql
├── V3__crear_movimientos_inventario.sql
├── V5__ampliar_recepciones_para_historial.sql
└── V6__crear_auditoria_recepciones.sql

src/test/java/com/avicontrol/
├── domain/model/recepcion/RecepcionMedicamentoTest.java
├── application/recepcion/
│   ├── RecepcionAtomicidadIntegrationTest.java
│   └── RegistrarRecepcionMedicamentoUseCaseTest.java
└── infrastructure/adapter/in/rest/recepcion/
    ├── RecepcionFlujosControllerTest.java
    └── RecepcionMedicamentoControllerTest.java
```

**Structure Decision**: Recepción conserva el hecho comercial y el nombre y presentación históricos. Inventario posee sus movimientos. Catálogo aporta nombre y unidad base mediante puerto. Las migraciones V3, V5 y V6 son compartidas con el plan 001 y no se duplican.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Definir unidades, cantidad normalizada y contratos de integración.

- [x] T001 Definir consulta de nombre histórico y unidad base del medicamento.
- [x] T002 Definir entrada inicial y ajuste de Inventario en unidad base.
- [x] T003 Preparar casos de gramos, miligramos, mililitros, unidades y dimensiones incompatibles.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Implementar persistencia, normalización y atomicidad antes de HTTP.

- [x] T004 Implementar `RecepcionMedicamento` y conversión mg a g.
- [x] T005 Crear entidad JPA, repositorio, migraciones y puerto de recepción.
- [x] T006 Crear auditoría y consulta de movimientos posteriores confirmados.
- [x] T007 Confirmar o revertir recepción, entrada y auditoría en una transacción local.

**Checkpoint**: Las operaciones básicas se ejecutan mediante casos de uso sin depender del controlador.

---

## Phase 3: User Story 1 - Registrar recepción de medicamento (Priority: P1)

**Goal**: Conservar una entrega independiente y su cantidad normalizada para Inventario.

**Independent Test**: Un registro en mililitros responde 201 y crea una entrada equivalente; una unidad incompatible o fallo de Inventario no deja una recepción confirmada.

### Endpoints

| Elemento | Definición |
| --- | --- |
| Método y ruta | `POST /api/recepciones/medicamentos` |
| Autorización | `ROLE_ADMINISTRADOR` |
| Respuesta | `201 Created`, `Location` y recepción confirmada |
| Errores | `400` datos, `401` sin sesión, `403` rol, `404` medicamento, `409` unidad o vencimiento, `503` persistencia o Inventario |

#### JSON de solicitud

```json
{
  "medicamentoId": "550e8400-e29b-41d4-a716-446655440000",
  "codigoLote": "MED-2026-001",
  "presentacion": "Frasco",
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
  "medicamentoId": "550e8400-e29b-41d4-a716-446655440000",
  "nombreMedicamento": "Medicamento de prueba",
  "estado": "CONFIRMADA",
  "codigoLote": "MED-2026-001",
  "presentacion": "Frasco",
  "cantidadPresentaciones": 20,
  "contenidoPorPresentacion": 500,
  "unidadContenido": "MILILITRO",
  "precioNetoPorPresentacion": 45000,
  "impuestoPorcentaje": 19,
  "cantidadUnidadBase": 10000,
  "unidadBase": "MILILITRO",
  "precioNetoUnidadBase": 90,
  "precioNetoTotal": 900000,
  "valorImpuesto": 171000,
  "precioTotal": 1071000,
  "fechaIngreso": "2026-11-05",
  "fechaVencimiento": "2027-05-05"
}
```

### Tests for User Story 1

- [x] T008 [US1] Probar conversión, cantidad, vencimiento y unidad incompatible.
- [x] T009 [US1] Probar 201, 400, 403, 404, 409, 503 y reversión de la entrada.

### Implementation for User Story 1

- [x] T010 [US1] Implementar comando, caso de uso, DTO, mapper y POST documentado en OpenAPI.

**Checkpoint**: La recepción creada puede recuperarse por su ID.

---

## Phase 4: User Story 2 - Editar recepción de medicamento (Priority: P2)

**Goal**: Corregir siete campos antes de que la recepción tenga movimientos posteriores confirmados.

**Independent Test**: El PUT conserva lote, presentación y fecha de ingreso; recalcula importes, ajusta la entrada y audita el cambio. Un movimiento posterior provoca 409 sin cambios.

### Endpoints

| Método y ruta | Resultado |
| --- | --- |
| `GET /api/recepciones/medicamentos/{id}/edicion` | `200` con siete campos editables; `404` inexistente; `409` si ya fue utilizada |
| `PUT /api/recepciones/medicamentos/{id}` | `200` con recepción actualizada; `400`, `403`, `404`, `409` o `503` según el fallo |

Ambas rutas requieren `ROLE_ADMINISTRADOR`. El PUT comprueba de nuevo los movimientos y la unidad base dentro de la transacción.

#### JSON de precarga y solicitud PUT

```json
{
  "medicamentoId": "550e8400-e29b-41d4-a716-446655440000",
  "fechaVencimiento": "2027-06-05",
  "cantidadPresentaciones": 20,
  "contenidoPorPresentacion": 500,
  "unidadContenido": "MILILITRO",
  "precioNetoPorPresentacion": 45000,
  "impuestoPorcentaje": 19
}
```

El PUT devuelve el JSON completo de US1 con los valores recalculados.

### Tests for User Story 2

- [x] T011 [US2] Probar campos protegidos, normalización, ajuste de entrada y auditoría.
- [x] T012 [US2] Probar 409 por movimiento posterior y unidad incompatible.

### Implementation for User Story 2

- [x] T013 [US2] Implementar formulario, comando, caso de uso, auditoría y endpoints.

**Checkpoint**: Solo una recepción no utilizada y de unidad compatible es editable.

---

## Phase 5: User Story 3 - Historial y detalle (Priority: P2)

**Goal**: Consultar entradas comerciales sin consultar ni alterar existencias.

**Independent Test**: La búsqueda por lote o nombre histórico y los filtros de presentación y fechas se combinan; el detalle muestra los datos de la compra; el resultado vacío informa `total: 0`.

### Endpoints

| Método y ruta | Parámetros y resultado |
| --- | --- |
| `GET /api/recepciones/medicamentos` | `busqueda`, `presentacion`, `estado`, `desde`, `hasta`, `pagina` (0), `tamano` (20, máximo 100); `200` paginado |
| `GET /api/recepciones/medicamentos/{id}` | `200` con JSON completo de US1; `404` si no existe |

Ambas rutas requieren `ROLE_ADMINISTRADOR`. Sin fechas se usan los últimos 30 días; `recepcionesMes` corresponde al mes calendario actual.

#### JSON de historial

```json
{
  "contenido": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "medicamentoId": "550e8400-e29b-41d4-a716-446655440000",
      "nombreMedicamento": "Medicamento de prueba",
      "codigoLote": "MED-2026-001",
      "presentacion": "Frasco",
      "estado": "CONFIRMADA",
      "fechaIngreso": "2026-11-05",
      "cantidadPresentaciones": 20,
      "contenidoPorPresentacion": 500,
      "precioNetoPorPresentacion": 45000
    }
  ],
  "total": 1,
  "pagina": 0,
  "tamano": 20,
  "recepcionesMes": 1
}
```

### Tests for User Story 3

- [x] T014 [US3] Probar búsqueda, filtros, indicador, detalle y resultado vacío.

### Implementation for User Story 3

- [x] T015 [US3] Implementar consultas paginadas, DTO de resumen y endpoints GET.

**Checkpoint**: Las tres historias de SPEC-002 están disponibles por API.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [x] T016 Verificar seguridad, contrato OpenAPI, errores `application/problem+json` y `correlationId`.
- [x] T017 Ejecutar Gradle, Spotless y reglas de arquitectura.

---

## Dependencies & Execution Order

### Phase Dependencies

- Setup precede Foundational; Foundational bloquea US1, US2 y US3.
- US2 requiere la entrada inicial creada por US1; US3 lee recepciones históricas.
- Polish cierra el plan tras verificar las tres historias.

### Within Each User Story

- Unidad y dominio antes del caso de uso; caso de uso antes de REST.
- Pruebas de dominio, HTTP, persistencia y reversión validan cada checkpoint.

## Notes

- El mismo código de lote puede repetirse en entregas independientes.
- El nombre histórico es una copia de Catálogo al registrar o editar. El adaptador local usa productos de ejemplo hasta integrar el Catálogo real.
- Las filas anteriores a V5 no tienen nombre histórico recuperable automáticamente; la migración no inventa ese dato.
