# Implementation Plan: Registrar, editar y consultar recepciones de alimento

**Date**: 05/11/2026
**Spec**: [001-RegistrarRecepcionDeAlimento.md](../specs/001-RegistrarRecepcionDeAlimento.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

El administrador registra cada entrega con alimento y `tipoAlimento` (`PRE_INICIO`, `INICIO` o `ENGORDE`), conserva sus valores históricos y confirma una entrada en kg. Puede editar seis campos mientras no existan movimientos posteriores confirmados. El historial ofrece indicadores mensuales, búsqueda, filtros, detalle y acceso condicionado a la edición.

## Technical Context

**Performance Goals**: El 95 % de registros y consultas válidos debe responder en menos de 2 segundos.
**Constraints**: `ROLE_ADMINISTRADOR`; cantidades y dinero con `BigDecimal`; vencimiento posterior al ingreso; lote repetido permitido; `tipoAlimento`, lote y fecha de ingreso protegidos al editar; recepción, entrada y auditoría se confirman o revierten juntas.
**Scale/Scope**: SPEC-001, historias US1, US2 y US3. La consulta es de recepciones, no de existencias actuales.

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
│   ├── model/catalogo/EtapaCrianza.java
│   ├── model/recepcion/
│   │   ├── EstadoRecepcion.java
│   │   ├── FiltroRecepciones.java
│   │   ├── PaginaRecepciones.java
│   │   ├── RecepcionCalculos.java
│   │   └── RecepcionAlimento.java
│   ├── exception/recepcion/
│   │   ├── CantidadInvalidaException.java
│   │   ├── FechaVencimientoInvalidaException.java
│   │   ├── RecepcionNoEditableException.java
│   │   └── RecursoNoEncontradoException.java
│   └── port/out/recepcion/RecepcionAlimentoRepositoryPort.java
├── application/
│   ├── inventario/port/
│   │   ├── AjustarEntradaInventarioPort.java
│   │   └── RegistrarEntradaInventarioPort.java
│   └── recepcion/
│       ├── AuditoriaRecepcionPort.java
│       ├── CatalogoRecepcionPort.java
│       ├── ConsultarRecepcionesAlimentoUseCase.java
│       ├── EditarRecepcionAlimentoCommand.java
│       ├── EditarRecepcionAlimentoUseCase.java
│       ├── HistorialRecepcionesAlimento.java
│       ├── RegistrarRecepcionAlimentoCommand.java
│       └── RegistrarRecepcionAlimentoUseCase.java
└── infrastructure/
    ├── adapter/in/rest/recepcion/
    │   ├── RecepcionAlimentoController.java
    │   ├── dto/
    │   │   ├── EditarRecepcionAlimentoRequest.java
    │   │   ├── HistorialAlimentoResponse.java
    │   │   ├── RecepcionAlimentoResponse.java
    │   │   ├── RecepcionAlimentoResumenResponse.java
    │   │   └── RegistrarRecepcionAlimentoRequest.java
    │   └── mapper/
    │       ├── RecepcionAlimentoRestMapper.java
    │       └── RecepcionFiltroRestMapper.java
    ├── adapter/out/persistence/inventario/
    │   ├── AjusteEntradaInventarioPersistenceAdapter.java
    │   ├── EntradaInventarioPersistenceAdapter.java
    │   ├── MovimientoInventarioEntity.java
    │   └── MovimientoInventarioJpaRepository.java
    ├── adapter/out/persistence/recepcion/
    │   ├── AuditoriaRecepcionEntity.java
    │   ├── AuditoriaRecepcionJpaRepository.java
    │   ├── AuditoriaRecepcionPersistenceAdapter.java
    │   ├── RecepcionAlimentoEntity.java
    │   ├── RecepcionAlimentoJpaRepository.java
    │   └── RecepcionAlimentoPersistenceAdapter.java
    └── config/RecepcionBeanConfiguration.java

src/main/resources/db/migration/
├── V1__crear_recepciones_alimento.sql
├── V3__crear_movimientos_inventario.sql
├── V5__ampliar_recepciones_para_historial.sql
└── V6__crear_auditoria_recepciones.sql

src/test/java/com/avicontrol/
├── domain/model/recepcion/RecepcionAlimentoTest.java
├── application/recepcion/
│   ├── RecepcionAtomicidadIntegrationTest.java
│   ├── EdicionRecepcionAtomicidadIntegrationTest.java
│   └── RegistrarRecepcionAlimentoUseCaseTest.java
└── infrastructure/adapter/in/rest/recepcion/
    ├── RecepcionAlimentoControllerTest.java
    └── RecepcionFlujosControllerTest.java
```

**Structure Decision**: Recepción posee los datos comerciales y su historial. Inventario posee los movimientos; Recepción lo usa por puertos. Los DTO REST, el dominio y las entidades JPA permanecen separados. Las migraciones V3, V5 y V6 son compartidas con el plan 002 y se ejecutan una sola vez.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Definir el contrato de Catálogo, Inventario y la representación histórica.

- [x] T001 Definir consulta del alimento, nombre histórico y tipo de crianza informado por el administrador.
- [x] T002 Definir puertos de entrada inicial y ajuste de Inventario, con detección de movimientos posteriores confirmados.
- [x] T003 Definir escalas de kg, dinero e impuesto y fixtures válidos e inválidos.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Preparar persistencia, transacción y consultas antes de exponer HTTP.

- [x] T004 Implementar `RecepcionAlimento`, cálculos y protección de campos históricos.
- [x] T005 Crear repositorios JPA y migraciones para recepción, movimiento y auditoría.
- [x] T006 Implementar consultas paginadas y bloqueo de la recepción al editar.
- [x] T007 Mantener recepción, entrada y auditoría en una transacción local.

**Checkpoint**: Dominio y puertos pueden registrar, consultar y editar sin un controlador.

---

## Phase 3: User Story 1 - Registrar recepción de alimento (Priority: P1)

**Goal**: Registrar una entrega independiente, aunque el lote se repita, y crear su entrada inicial.

**Independent Test**: Con datos válidos responde 201, conserva `tipoAlimento`, calcula  kg y dinero y crea una sola entrada; ante fallo de Inventario revierte la recepción.

### Endpoints

| Elemento | Definición |
| --- | --- |
| Método y ruta | `POST /api/recepciones/alimentos` |
| Autorización | `ROLE_ADMINISTRADOR` |
| Respuesta | `201 Created`, `Location` y recepción confirmada |
| Errores | `400` datos inválidos, `401` sin sesión, `403` rol, `404` alimento, `409` vencimiento, `503` persistencia o Inventario |

#### JSON de solicitud

```json
{
  "alimentoId": "550e8400-e29b-41d4-a716-446655440000",
  "tipoAlimento": "INICIO",
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
  "alimentoId": "550e8400-e29b-41d4-a716-446655440000",
  "nombreAlimento": "Alimento de prueba",
  "tipoAlimento": "INICIO",
  "estado": "CONFIRMADA",
  "codigoLote": "AL-2026-001",
  "cantidadBultos": 40,
  "pesoPorBultoKg": 40,
  "precioNetoPorBulto": 82000,
  "impuestoPorcentaje": 19,
  "cantidadTotalKg": 1600,
  "precioNetoKg": 2050,
  "precioNetoTotal": 3280000,
  "valorImpuesto": 623200,
  "precioTotal": 3903200,
  "fechaIngreso": "2026-11-05",
  "fechaVencimiento": "2027-03-05"
}
```

### Tests for User Story 1

- [x] T008 [US1] Probar cálculo, vencimiento, cantidad y `tipoAlimento` obligatorio.
- [x] T009 [US1] Probar 201, 400, 403, 404, 409 y 503, además de la reversión transaccional.

### Implementation for User Story 1

- [x] T010 [US1] Implementar comando, caso de uso, DTO, mapper y POST documentado en OpenAPI.

**Checkpoint**: Una recepción confirmada puede consultarse por su ID.

---

## Phase 4: User Story 2 - Editar recepción de alimento (Priority: P2)

**Goal**: Corregir seis campos mientras no haya movimientos posteriores confirmados.

**Independent Test**: La edición conserva lote, fecha de ingreso y `tipoAlimento`; recalcula totales, ajusta la entrada y audita el antes/después. Un movimiento posterior produce 409 sin cambios.

### Endpoints

| Método y ruta | Resultado |
| --- | --- |
| `GET /api/recepciones/alimentos/{id}/edicion` | `200` con solo los seis campos editables; `404` inexistente; `409` si ya fue utilizada |
| `PUT /api/recepciones/alimentos/{id}` | `200` con recepción actualizada; `400`, `403`, `404`, `409` o `503` según el fallo |

Ambas rutas requieren `ROLE_ADMINISTRADOR`. El GET es una precarga; el PUT vuelve a comprobar los movimientos dentro de su transacción.

#### JSON de precarga y solicitud PUT

```json
{
  "alimentoId": "550e8400-e29b-41d4-a716-446655440000",
  "fechaVencimiento": "2027-04-05",
  "cantidadBultos": 40,
  "pesoPorBultoKg": 40,
  "precioNetoPorBulto": 82000,
  "impuestoPorcentaje": 19
}
```

El PUT devuelve el JSON completo de la recepción con los valores recalculados, como en US1.

### Tests for User Story 2

- [x] T011 [US2] Probar campos protegidos, ajuste de entrada y auditoría.
- [x] T012 [US2] Probar bloqueo por movimiento posterior y ausencia de cambios.

### Implementation for User Story 2

- [x] T013 [US2] Implementar formulario, comando, caso de uso, ajuste y endpoints.

**Checkpoint**: Solo una recepción no utilizada es editable.

---

## Phase 5: User Story 3 - Historial y detalle (Priority: P2)

**Goal**: Consultar entregas sin modificar existencias.

**Independent Test**: El listado filtra simultáneamente por lote/ID, estado y fechas; devuelve indicador mensual y una página vacía si no hay resultados. El detalle conserva `tipoAlimento` y valores históricos.

### Endpoints

| Método y ruta | Parámetros y resultado |
| --- | --- |
| `GET /api/recepciones/alimentos` | `busqueda`, `estado`, `desde`, `hasta`, `pagina` (0), `tamano` (20, máximo 100); `200` paginado |
| `GET /api/recepciones/alimentos/{id}` | `200` con el JSON completo de US1; `404` si no existe |

Ambas rutas requieren `ROLE_ADMINISTRADOR`. Sin fechas, el periodo inicial son los últimos 30 días. Los indicadores usan el mes calendario actual.

#### JSON de historial

```json
{
  "contenido": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "alimentoId": "550e8400-e29b-41d4-a716-446655440000",
      "nombreAlimento": "Alimento de prueba",
      "tipoAlimento": "INICIO",
      "codigoLote": "AL-2026-001",
      "estado": "CONFIRMADA",
      "fechaIngreso": "2026-11-05",
      "cantidadBultos": 40,
      "pesoPorBultoKg": 40,
      "precioNetoPorBulto": 82000
    }
  ],
  "total": 1,
  "pagina": 0,
  "tamano": 20,
  "recepcionesMes": 1,
  "pesoMesKg": 1600
}
```

### Tests for User Story 3

- [x] T014 [US3] Probar filtros, indicador, detalle y resultado vacío.

### Implementation for User Story 3

- [x] T015 [US3] Implementar consulta paginada, DTO de resumen y endpoints GET.

**Checkpoint**: Las tres historias de SPEC-001 están disponibles por API.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [x] T016 Verificar rol, contrato OpenAPI, errores `application/problem+json` y `correlationId`.
- [x] T017 Ejecutar Gradle, Spotless y reglas de arquitectura.

---

## Dependencies & Execution Order

### Phase Dependencies

- Setup precede Foundational; Foundational bloquea US1, US2 y US3.
- US2 depende de la entrada inicial de US1. US3 puede consultar cualquier recepción registrada.
- Polish cierra el plan después de verificar las tres historias.

### Within Each User Story

- Dominio y puertos antes de casos de uso; casos de uso antes de HTTP.
- Las pruebas de dominio, aplicación, HTTP y transacción verifican el checkpoint de cada historia.

## Notes

- Un lote repetido crea otra recepción; no representa un duplicado.
- Las filas anteriores a la migración V5 no tienen `tipoAlimento` histórico recuperable automáticamente y deben completarse con datos verificables antes de editarse.
- El adaptador local de Catálogo ofrece productos de ejemplo; la integración real del Catálogo sustituirá ese adaptador sin cambiar el contrato de Recepción.
