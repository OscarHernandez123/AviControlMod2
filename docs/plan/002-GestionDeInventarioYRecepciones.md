# Implementation Plan: Gestión de Inventario y Recepciones

**Date**: 02/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [001-RegistrarRecepcionDeAlimento.md](../specs/001-RegistrarRecepcionDeAlimento.md)
- [002-RegistroRecepcionDeMedicamento.md](../specs/002-RegistroRecepcionDeMedicamento.md)
- [023-ConsultarInventario.md](../specs/023-ConsultarInventario.md)

## Summary

Este plan implementa la bodega central del Módulo 2: registrar, corregir y consultar recepciones de alimentos y medicamentos, y calcular las existencias desde movimientos confirmados. Las recepciones son hechos comerciales históricos; los movimientos son el libro que determina el saldo. Una recepción no se sobrescribe para recalcular inventario.

El inventario se presenta en dos secciones independientes. Los alimentos se consolidan por nombre normalizado y tipo de alimento, en kilogramos; los medicamentos por medicamento, presentación, contenido y unidad compatibles. Los productos activos sin saldo aparecen con cero. Los saldos vencidos o anulados se excluyen del disponible, pero conservan su trazabilidad.

La pantalla de inventario también muestra ocupación, recepciones recientes y cobertura del requerimiento alimenticio. La demanda solo aplica a alimentos y se obtiene mediante un puerto hacia nutrición; consultar cobertura no reserva existencias ni genera movimientos.

La capacidad es propietaria de `BodegaCentral`, recepciones, movimientos y auditorías. Los catálogos de alimentos y medicamentos y la demanda nutricional pertenecen a otras capacidades y se consultan mediante puertos. El plan no crea tablas ni entidades para esos catálogos.

## Technical Context

**Integraciones específicas**: Catálogos de alimentos y medicamentos, requerimientos nutricionales y contratos de recepción confirmada para el Módulo 3.

**Datos propios**: Una bodega central, recepciones, movimientos de alimento y medicamento y auditoría de ediciones.

**Performance Goals**: El 95 % de registros, ediciones, historiales y consultas consolidadas responde en máximo 2 segundos con el volumen acordado.

**Constraints**: Acceso exclusivo del administrador; recepción y movimiento inicial atómicos; códigos de lote no únicos; recepciones utilizadas no editables; vencimientos y anulaciones fuera del disponible; demanda únicamente para alimentos; unidades incompatibles nunca se mezclan.

**Scale/Scope**: Nueve historias de usuario, once endpoints REST, una bodega central, dos tipos de recepción, dos libros de movimientos y eventos versionados de recepción confirmada.

### Decisiones específicas

1. **Recepción frente a movimiento**: `RecepcionAlimento` y `RecepcionMedicamento` conservan la compra y sus cálculos históricos. `MovimientoAlimento` y `MovimientoMedicamento` registran entradas, salidas, despachos, consumos, vencimientos, anulaciones y ajustes. El saldo se calcula desde movimientos confirmados.
2. **Identidad**: cada recepción tiene UUID propio. El código de lote es un dato de búsqueda y puede repetirse; nunca es clave primaria ni restricción de unicidad.
3. **Edición**: solo se edita una recepción cuyo movimiento inicial no tenga movimientos confirmados posteriores. La edición ajusta el movimiento inicial y registra auditoría dentro de la misma transacción; no elimina historia.
4. **Unidades y cálculos**: cantidades, pesos, precios, impuestos, porcentajes y saldos usan `BigDecimal` con escala y redondeo centralizados. Las unidades incompatibles producen conflicto y no se convierten automáticamente.
5. **Vencimiento**: el saldo vencido se excluye inmediatamente de las consultas. El scheduler puede registrar un movimiento de vencimiento idempotente para conservar el libro completo; la consulta no depende de que el scheduler ya haya corrido.
6. **Eventos**: los registros confirmados publican eventos internos y contratos de integración versionados. Las consultas no publican eventos. Los consumidores del Módulo 3 reciben solo valores históricos necesarios y deben tolerar duplicados.
7. **Persistencia**: Flyway crea las tablas propias de esta capacidad. Entidades JPA, repositorios y mappers permanecen en infraestructura; el dominio no conoce JPA ni Spring.
8. **Seguridad**: todos los endpoints de este plan requieren `ROLE_ADMINISTRADOR`. El caso de uso recibe el actor y la autorización se comprueba también en aplicación.
9. **Ocupación**: `BodegaCentral.ocupacionActual` es el valor vigente registrado para la bodega y se consulta junto con su unidad. Este plan no infiere automáticamente la ocupación a partir de kilogramos de alimentos y contenidos de medicamentos de unidades incompatibles.
10. **Movimientos posteriores**: las salidas, despachos, consumos y aplicaciones pueden ser producidos por otras capacidades. Este plan conserva sus movimientos, aplica FEFO cuando el puerto lo requiere y los incluye en el cálculo, pero no inventa endpoints de despacho o consumo que no están en los tres specs incluidos.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   ├── 001-RegistrarRecepcionDeAlimento.md
│   ├── 002-RegistroRecepcionDeMedicamento.md
│   └── 023-ConsultarInventario.md
└── plan/
    └── 002-GestionDeInventarioYRecepciones.md    # Este archivo
```

### Source Code (repository root)

Clases nuevas que agrega este feature:

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/inventario/
│   │   ├── BodegaCentral.java
│   │   ├── RecepcionAlimento.java
│   │   ├── RecepcionMedicamento.java
│   │   ├── MovimientoAlimento.java
│   │   ├── MovimientoMedicamento.java
│   │   ├── RegistroAuditoriaRecepcion.java
│   │   ├── ExistenciaAlimento.java
│   │   ├── ExistenciaMedicamento.java
│   │   ├── CoberturaRequerimientoAlimento.java
│   │   ├── EstadoRecepcion.java
│   │   ├── TipoMovimientoInventario.java
│   │   ├── EstadoMovimientoInventario.java
│   │   ├── EstadoCoberturaAlimento.java
│   │   ├── PresentacionMedicamento.java
│   │   └── UnidadMedida.java
│   ├── event/inventario/
│   │   ├── RecepcionAlimentoConfirmada.java
│   │   ├── RecepcionMedicamentoConfirmada.java
│   │   ├── RecepcionAlimentoEditada.java
│   │   ├── RecepcionMedicamentoEditada.java
│   │   └── RecepcionVencida.java
│   ├── exception/inventario/
│   │   ├── RecepcionNoEncontradaException.java
│   │   ├── RecepcionUtilizadaException.java
│   │   ├── ProductoInactivoException.java
│   │   ├── FechaVencimientoInvalidaException.java
│   │   ├── CantidadInvalidaException.java
│   │   ├── CalculoFueraDeRangoException.java
│   │   └── UnidadMedidaIncompatibleException.java
│   └── port/out/inventario/
│       ├── BodegaCentralRepositoryPort.java
│       ├── RecepcionAlimentoRepositoryPort.java
│       ├── RecepcionMedicamentoRepositoryPort.java
│       ├── MovimientoAlimentoRepositoryPort.java
│       ├── MovimientoMedicamentoRepositoryPort.java
│       ├── AuditoriaRecepcionRepositoryPort.java
│       ├── AlimentoCatalogoQueryPort.java
│       ├── MedicamentoCatalogoQueryPort.java
│       ├── RequerimientoAlimentoQueryPort.java
│       └── InventarioEventPublisherPort.java
├── application/inventario/
│   ├── RegistrarRecepcionAlimentoUseCase.java
│   ├── EditarRecepcionAlimentoUseCase.java
│   ├── ConsultarHistorialAlimentosUseCase.java
│   ├── ConsultarDetalleRecepcionAlimentoUseCase.java
│   ├── RegistrarRecepcionMedicamentoUseCase.java
│   ├── EditarRecepcionMedicamentoUseCase.java
│   ├── ConsultarHistorialMedicamentosUseCase.java
│   ├── ConsultarDetalleRecepcionMedicamentoUseCase.java
│   ├── ConsultarInventarioUseCase.java
│   ├── ConsultarResumenInventarioUseCase.java
│   ├── ConsultarCoberturaAlimentoUseCase.java
│   └── ProcesarRecepcionesVencidasUseCase.java
└── infrastructure/
    ├── adapter/in/rest/inventario/
    │   ├── RecepcionAlimentoController.java
    │   ├── RecepcionMedicamentoController.java
    │   ├── InventarioController.java
    │   ├── dto/
    │   │   ├── RegistrarRecepcionAlimentoRequest.java
    │   │   ├── EditarRecepcionAlimentoRequest.java
    │   │   ├── RecepcionAlimentoResponse.java
    │   │   ├── HistorialRecepcionAlimentoResponse.java
    │   │   ├── RegistrarRecepcionMedicamentoRequest.java
    │   │   ├── EditarRecepcionMedicamentoRequest.java
    │   │   ├── RecepcionMedicamentoResponse.java
    │   │   ├── HistorialRecepcionMedicamentoResponse.java
    │   │   ├── InventarioResponse.java
    │   │   ├── ResumenInventarioResponse.java
    │   │   └── CoberturaAlimentoResponse.java
    │   └── mapper/InventarioRestMapper.java
    ├── adapter/in/scheduling/
    │   └── RecepcionesVencidasScheduler.java
    ├── adapter/out/persistence/inventario/
    │   ├── entity/
    │   │   ├── BodegaCentralEntity.java
    │   │   ├── RecepcionAlimentoEntity.java
    │   │   ├── RecepcionMedicamentoEntity.java
    │   │   ├── MovimientoAlimentoEntity.java
    │   │   ├── MovimientoMedicamentoEntity.java
    │   │   └── AuditoriaRecepcionEntity.java
    │   ├── repository/
    │   │   ├── BodegaCentralJpaRepository.java
    │   │   ├── RecepcionAlimentoJpaRepository.java
    │   │   ├── RecepcionMedicamentoJpaRepository.java
    │   │   ├── MovimientoAlimentoJpaRepository.java
    │   │   ├── MovimientoMedicamentoJpaRepository.java
    │   │   └── AuditoriaRecepcionJpaRepository.java
    │   ├── mapper/InventarioPersistenceMapper.java
    │   └── InventarioPersistenceAdapter.java
    ├── adapter/out/catalogo/
    │   ├── AlimentoCatalogoQueryAdapter.java
    │   └── MedicamentoCatalogoQueryAdapter.java
    ├── adapter/out/nutricion/
    │   └── RequerimientoAlimentoQueryAdapter.java
    ├── adapter/out/event/inventario/
    │   ├── InventarioEventPublisher.java
    │   └── dto/
    │       ├── RecepcionAlimentoConfirmadaV1.java
    │       └── RecepcionMedicamentoConfirmadaV1.java
    └── config/
        ├── InventarioBeanConfiguration.java
        └── PoliticaCalculoConfiguration.java

src/main/resources/db/migration/
├── V3__crear_bodega_central.sql
├── V4__crear_recepciones.sql
├── V5__crear_movimientos_inventario.sql
└── V6__crear_auditoria_recepciones.sql

src/test/java/com/avicontrol/
├── domain/inventario/
│   ├── RecepcionAlimentoTest.java
│   ├── RecepcionMedicamentoTest.java
│   └── CoberturaRequerimientoAlimentoTest.java
├── application/inventario/
│   ├── RegistrarRecepcionAlimentoUseCaseTest.java
│   ├── EditarRecepcionAlimentoUseCaseTest.java
│   ├── ConsultarHistorialAlimentosUseCaseTest.java
│   ├── RegistrarRecepcionMedicamentoUseCaseTest.java
│   ├── EditarRecepcionMedicamentoUseCaseTest.java
│   ├── ConsultarHistorialMedicamentosUseCaseTest.java
│   ├── ConsultarInventarioUseCaseTest.java
│   ├── ConsultarResumenInventarioUseCaseTest.java
│   └── ConsultarCoberturaAlimentoUseCaseTest.java
└── infrastructure/
    ├── adapter/in/rest/
    │   ├── RecepcionAlimentoControllerTest.java
    │   ├── RecepcionMedicamentoControllerTest.java
    │   └── InventarioControllerTest.java
    └── adapter/out/persistence/
        └── InventarioPersistenceAdapterTest.java
```

**Structure Decision**: La capacidad se distribuye en los paquetes comunes definidos en [General.md](General.md). Las recepciones y los movimientos son entidades diferentes: la recepción conserva el hecho comercial histórico y los movimientos determinan el saldo disponible. El feature incorpora adaptadores propios para catálogo, nutrición, vencimientos y publicación de valores históricos.

### Entidades y relaciones

```text
BodegaCentral
├── id: UUID
├── capacidadMaxima: BigDecimal
├── ocupacionActual: BigDecimal
└── unidadCapacidad: UnidadMedida

RecepcionAlimento
├── id: UUID
├── alimentoId / nombreHistorico
├── tipoAlimento
├── codigoLote
├── fechaIngreso / fechaVencimiento
├── cantidadBultos / pesoPorBulto
├── precioNetoPorBulto / porcentajeImpuesto
└── valoresCalculadosHistoricos

RecepcionMedicamento
├── id: UUID
├── medicamentoId / nombreHistorico
├── codigoLote / presentacion
├── cantidad / contenidoPorPresentacion / unidadMedida
├── precioNetoPorPresentacion / porcentajeImpuesto
├── fechaIngreso / fechaVencimiento
└── valoresCalculadosHistoricos

MovimientoAlimento / MovimientoMedicamento
├── id: UUID
├── recepcionId
├── tipo / cantidadBase / unidad
├── estado / fechaHora / motivo
└── saldoPorRecepcion

RegistroAuditoriaRecepcion
└── recepcionId, actor, fechaHora, valoresAnteriores, valoresNuevos
```

La recepción referencia su movimiento de entrada por `recepcionId`, pero no contiene una colección de movimientos como parte de la respuesta REST. Los historiales de movimientos pueden tener muchos registros y se consultan por puerto. `ExistenciaAlimento`, `ExistenciaMedicamento`, `ResumenInventario` y `CoberturaRequerimientoAlimento` son resultados calculados de lectura, no tablas propietarias adicionales.

Los catálogos entregan la identidad vigente del alimento o medicamento y sus datos descriptivos; la recepción conserva una copia histórica de los nombres y precios necesarios para que cambios posteriores del catálogo no alteren la compra registrada.

### Contratos de los puertos

| Puerto | Responsabilidad |
| --- | --- |
| `BodegaCentralRepositoryPort` | Leer y actualizar capacidad u ocupación de la única bodega, dentro de casos de uso autorizados. |
| `RecepcionAlimentoRepositoryPort` / `RecepcionMedicamentoRepositoryPort` | Guardar, editar, buscar, filtrar y obtener detalles de recepciones. |
| `MovimientoAlimentoRepositoryPort` / `MovimientoMedicamentoRepositoryPort` | Registrar movimientos, detectar uso posterior, calcular saldos y aplicar FEFO. |
| `AuditoriaRecepcionRepositoryPort` | Registrar los valores anteriores y nuevos de cada edición. |
| `AlimentoCatalogoQueryPort` / `MedicamentoCatalogoQueryPort` | Validar producto activo y obtener datos descriptivos sin modificar el catálogo. |
| `RequerimientoAlimentoQueryPort` | Consultar demanda proyectada por alimento y tipo; nunca se usa para medicamentos. |
| `InventarioEventPublisherPort` | Publicar eventos internos y contratos versionados después de confirmar una transacción local. |

### Contratos HTTP propuestos

| Endpoint | Acceso | Propósito |
| --- | --- | --- |
| `POST /api/recepciones/alimentos` | Administrador | Registrar recepción y entrada de alimento. |
| `PUT /api/recepciones/alimentos/{id}` | Administrador | Editar recepción no utilizada. |
| `GET /api/recepciones/alimentos` | Administrador | Historial paginado y filtrado de alimentos. |
| `GET /api/recepciones/alimentos/{id}` | Administrador | Detalle de recepción de alimento. |
| `POST /api/recepciones/medicamentos` | Administrador | Registrar recepción y entrada de medicamento. |
| `PUT /api/recepciones/medicamentos/{id}` | Administrador | Editar recepción no utilizada. |
| `GET /api/recepciones/medicamentos` | Administrador | Historial paginado y filtrado de medicamentos. |
| `GET /api/recepciones/medicamentos/{id}` | Administrador | Detalle de recepción de medicamento. |
| `GET /api/inventario` | Administrador | Existencias consolidadas de alimentos y medicamentos. |
| `GET /api/inventario/resumen` | Administrador | Ocupación y cinco recepciones recientes. |
| `GET /api/inventario/alimentos/{alimentoId}/requerimiento` | Administrador | Cobertura de demanda de un alimento. |

Todos usan JSON y `application/problem+json` para errores. Las consultas no crean movimientos ni modifican saldos.

### JSON común de errores

```json
{
  "type": "https://avicontrol/errors/recepcion-utilizada",
  "title": "Recepción utilizada",
  "status": 409,
  "detail": "La recepción ya tiene movimientos posteriores y no puede editarse",
  "instance": "/api/recepciones/alimentos/550e8400-e29b-41d4-a716-446655440000",
  "code": "RECEPCION_UTILIZADA",
  "correlationId": "c9a13035-a18a-4fbd-afb3-a3bfcd15f088",
  "fieldErrors": []
}
```

Los errores de validación pueden incluir `fieldErrors` con `field`, `code` y `message`. `status` coincide con el HTTP y `code` es estable para los clientes.

### JSON común de eventos de integración

Los eventos `RecepcionAlimentoConfirmadaV1` y `RecepcionMedicamentoConfirmadaV1` usan el sobre definido en General.md:

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440099",
  "eventType": "RecepcionAlimentoConfirmada",
  "eventVersion": 1,
  "occurredAt": "2026-10-01T14:30:00Z",
  "publishedAt": "2026-10-01T14:30:01Z",
  "producer": "inventario",
  "correlationId": "c9a13035-a18a-4fbd-afb3-a3bfcd15f088",
  "causationId": "550e8400-e29b-41d4-a716-446655440000",
  "aggregateType": "RecepcionAlimento",
  "aggregateId": "550e8400-e29b-41d4-a716-446655440000",
  "payload": {
    "recepcionId": "550e8400-e29b-41d4-a716-446655440000",
    "alimentoId": "a1b2c3d4-e5f6-47a8-9012-abcdef123456",
    "nombreHistorico": "Alimento A",
    "tipoAlimento": "INICIO",
    "kilogramosTotales": 2500.00,
    "precioNetoPorKilogramo": 1000.00,
    "subtotalNeto": 2500000.00,
    "valorImpuesto": 125000.00,
    "totalCompra": 2625000.00
  }
}
```

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Preparar la estructura y configuración exclusiva de la capacidad de inventario y recepciones.

- [ ] T001 Crear los paquetes de dominio, aplicación y adaptadores de inventario descritos en este plan.
- [ ] T002 Registrar la capacidad de inventario como módulo funcional y declarar su interfaz pública de aplicación.
- [ ] T003 Configurar las propiedades específicas de precisión, escala y redondeo de recepciones e inventario.
- [ ] T004 Configurar la frecuencia y el control de idempotencia del procesamiento de vencimientos.
- [ ] T005 Configurar los destinos de catálogo, requerimientos nutricionales y eventos históricos dirigidos al Módulo 3.
- [ ] T006 Crear fixtures reutilizables de recepciones, movimientos, productos, vencimientos y demanda para pruebas.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Crear las entidades, reglas, puertos y persistencia compartidos por todas las historias.

**⚠️ CRITICAL**: Ninguna user story puede comenzar hasta que esta fase esté completa.

- [ ] T007 Crear migraciones Flyway para `bodega_central`, `recepcion_alimento`, `recepcion_medicamento`, `movimiento_alimento`, `movimiento_medicamento` y `auditoria_recepcion`.
- [ ] T008 Crear `BodegaCentral` con capacidad máxima, ocupación actual y unidad de capacidad; validar que capacidad y ocupación utilicen la misma unidad.
- [ ] T009 Crear `RecepcionAlimento` con identificador UUID independiente del código de lote, datos históricos, estado y operaciones de cálculo y validación.
- [ ] T010 Crear `RecepcionMedicamento` con identificador UUID, presentación, contenido por presentación, unidad de medida, datos históricos, estado y operaciones de cálculo y validación.
- [ ] T011 Crear `MovimientoAlimento` y `MovimientoMedicamento` con tipo, cantidad base, estado, fecha, motivo y recepción de origen.
- [ ] T012 Crear `EstadoRecepcion`, `TipoMovimientoInventario`, `EstadoMovimientoInventario`, `PresentacionMedicamento` y `UnidadMedida`; los valores definitivos de presentación y unidad se establecerán cuando el catálogo sea acordado.
- [ ] T013 Crear una política central de cálculos con `BigDecimal`, detección de desbordamiento, escala y redondeo uniforme para cantidades, precios unitarios, impuestos y totales.
- [ ] T014 Crear las excepciones de recepción inexistente, recepción utilizada, producto inactivo, fecha inválida, cantidad inválida, cálculo fuera de rango y unidad incompatible.
- [ ] T015 Crear los puertos de persistencia para bodega, recepciones, movimientos y auditoría en `domain/port/out/inventario/`.
- [ ] T016 Crear `AlimentoCatalogoQueryPort` y `MedicamentoCatalogoQueryPort` para validar productos activos y recuperar los datos históricos requeridos.
- [ ] T017 Crear `RequerimientoAlimentoQueryPort` para obtener la demanda consolidada del contexto nutricional sin introducir demanda para medicamentos.
- [ ] T018 Crear `InventarioEventPublisherPort` y los eventos de dominio de recepción confirmada, recepción editada y vencimiento.
- [ ] T019 Crear entidades JPA, repositorios Spring Data, mappers y `InventarioPersistenceAdapter` para las seis tablas.
- [ ] T020 Implementar consultas de saldo basadas exclusivamente en movimientos confirmados y protegidas contra la mezcla de unidades incompatibles.
- [ ] T021 Implementar autorización para `ROLE_ADMINISTRADOR` y respuestas `application/problem+json` para errores 400, 403, 404, 409 y 422.
- [ ] T022 Crear `InventarioBeanConfiguration` para registrar cada caso de uso con sus puertos y política de cálculo.

**Checkpoint**: El esquema, dominio, puertos, persistencia, cálculos y seguridad compartidos están disponibles; las user stories pueden comenzar.

---

## Phase 3: User Story 1 — Registrar una Recepción de Alimento (Priority: P1)

**Goal**: El administrador puede registrar una entrega de alimento, obtener todos sus valores calculados y generar atómicamente el movimiento confirmado de entrada.

**Independent Test**: `POST /api/recepciones/alimentos` con 50 bultos de 50 kg, precio neto por bulto de 50.000 e impuesto del 5 % crea una recepción confirmada con 2.500 kg, subtotal 2.500.000, impuesto 125.000 y total 2.625.000, además de un movimiento de entrada por 2.500 kg.

### Definición del evento para User Story 1

**Evento producido**: `RecepcionAlimentoConfirmada` (interno) y `RecepcionAlimentoConfirmadaV1` (integración).

Se publican después de confirmar recepción y movimiento en la misma transacción local. El contrato incluye `eventId`, `eventVersion`, `occurredAt`, `producer`, `correlationId`, `aggregateId`, `recepcionId`, `alimentoId`, nombre y tipo históricos, código de lote, fecha de ingreso, fecha de vencimiento, kilogramos totales, precio por kilogramo, subtotal, impuesto y total. El consumidor debe ser idempotente.

### Definición del endpoint REST para User Story 1

| Elemento | Definición |
| --- | --- |
| Método y ruta | `POST /api/recepciones/alimentos` |
| Autorización | `ROLE_ADMINISTRADOR` |
| Entrada | JSON con alimento, tipo, código de lote, fechas, cantidad de bultos, peso por bulto, precio neto por bulto e impuesto. No acepta valores calculados. |
| Respuesta | HTTP 201 con recepción, cálculos históricos, estado `CONFIRMADA` y movimiento de entrada. |
| Errores | 400, 403, 409 para conflicto de producto/estado y 422 para regla de negocio inválida. |

```json
{
  "alimentoId": "a1b2c3d4-e5f6-47a8-9012-abcdef123456",
  "tipoAlimento": "INICIO",
  "codigoLote": "L-2026-09",
  "fechaIngreso": "2026-10-01",
  "fechaVencimiento": "2027-01-01",
  "cantidadBultos": 50,
  "pesoPorBulto": 50.00,
  "precioNetoPorBulto": 50000.00,
  "porcentajeImpuesto": 5.00
}
```

La respuesta incluye `id`, `estado`, `kilogramosTotales`, `precioNetoPorKilogramo`, `subtotalNeto`, `valorImpuesto`, `totalCompra` y `movimientoEntradaId`.

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "estado": "CONFIRMADA",
  "kilogramosTotales": 2500.00,
  "precioNetoPorKilogramo": 1000.00,
  "subtotalNeto": 2500000.00,
  "valorImpuesto": 125000.00,
  "totalCompra": 2625000.00,
  "movimientoEntradaId": "550e8400-e29b-41d4-a716-446655440099"
}
```

### Tests para User Story 1

- [ ] T023 [P] [US1] Test unitario de kilogramos totales, precio por kilogramo, subtotal, impuesto y total — `RecepcionAlimentoTest.java`.
- [ ] T024 [P] [US1] Test unitario de precisión decimal, redondeo y detección de desbordamiento — `RecepcionAlimentoTest.java`.
- [ ] T025 [P] [US1] Test de contrato: registro válido retorna HTTP 201 y valores calculados — `RecepcionAlimentoControllerTest.java`.
- [ ] T026 [P] [US1] Test de contrato: campos inválidos, fecha de vencimiento no posterior y producto inactivo no modifican el inventario — `RecepcionAlimentoControllerTest.java`.
- [ ] T027 [P] [US1] Test de contrato: código de lote repetido crea una recepción independiente y un usuario no administrador recibe HTTP 403 — `RecepcionAlimentoControllerTest.java`.
- [ ] T028 [P] [US1] Test de integración: recepción y movimiento de entrada se guardan en la misma transacción — `InventarioPersistenceAdapterTest.java`.

### Implementación de User Story 1

- [ ] T029 [US1] Implementar `RegistrarRecepcionAlimentoUseCase` validando el alimento mediante `AlimentoCatalogoQueryPort` y obteniendo su tipo sin permitir edición independiente.
- [ ] T030 [US1] Calcular y persistir kilogramos totales, precio por kilogramo, subtotal, impuesto y total como valores históricos.
- [ ] T031 [US1] Crear un movimiento `ENTRADA` confirmado por los kilogramos totales dentro de la misma transacción de la recepción.
- [ ] T032 [US1] Publicar `RecepcionAlimentoConfirmada` después del registro transaccional.
- [ ] T033 [US1] Crear `RegistrarRecepcionAlimentoRequest`, `RecepcionAlimentoResponse` y sus mapeos.
- [ ] T034 [US1] Implementar `POST /api/recepciones/alimentos` en `RecepcionAlimentoController`.

**Checkpoint**: US1 registra una recepción de alimento trazable y deja su entrada disponible para el cálculo de inventario.

---

## Phase 4: User Story 2 — Editar una Recepción de Alimento (Priority: P2)

**Goal**: El administrador puede corregir una recepción de alimento que todavía no tenga movimientos de salida, despacho o consumo.

**Independent Test**: `PUT /api/recepciones/alimentos/{id}` actualiza una recepción que solo posee su entrada, recalcula todos sus valores, ajusta el movimiento inicial y registra auditoría; la misma operación sobre una recepción utilizada retorna HTTP 409 sin cambios.

### Definición del evento para User Story 2

**Evento producido**: `RecepcionAlimentoEditada` interno. No se publica un contrato externo de edición mientras el Módulo 3 no lo requiera explícitamente.

El evento contiene recepción, actor, fecha de edición, valores anteriores y nuevos, y el movimiento de entrada ajustado. Se publica después de actualizar recepción, movimiento y auditoría de forma atómica. No se publica cuando la edición es rechazada.

### Definición del endpoint REST para User Story 2

| Elemento | Definición |
| --- | --- |
| Método y ruta | `PUT /api/recepciones/alimentos/{id}` |
| Autorización | `ROLE_ADMINISTRADOR` |
| Entrada | JSON únicamente con alimento, fecha de vencimiento, cantidad de bultos, peso por bulto, precio neto por bulto e impuesto. |
| Respuesta | HTTP 200 con nuevos cálculos, estado y auditoría resumida; no devuelve valores editables como autoridad externa. |
| Errores | 400, 403, 404, 409 si existen movimientos posteriores y 422 para fechas o cálculos inválidos. |

```json
{
  "alimentoId": "a1b2c3d4-e5f6-47a8-9012-abcdef123456",
  "fechaVencimiento": "2027-01-15",
  "cantidadBultos": 48,
  "pesoPorBulto": 50.00,
  "precioNetoPorBulto": 51000.00,
  "porcentajeImpuesto": 5.00
}
```

La respuesta 200 conserva el mismo identificador y devuelve los valores recalculados, por ejemplo:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "estado": "CONFIRMADA",
  "kilogramosTotales": 2400.00,
  "subtotalNeto": 2448000.00,
  "valorImpuesto": 122400.00,
  "totalCompra": 2570400.00,
  "auditoriaId": "550e8400-e29b-41d4-a716-446655440100"
}
```

### Tests para User Story 2

- [ ] T035 [P] [US2] Test unitario de edición permitida y recálculo completo — `EditarRecepcionAlimentoUseCaseTest.java`.
- [ ] T036 [P] [US2] Test unitario de bloqueo cuando existe salida, despacho o consumo — `EditarRecepcionAlimentoUseCaseTest.java`.
- [ ] T037 [P] [US2] Test de contrato: el formulario acepta los mismos campos editables que el registro y no acepta valores calculados — `RecepcionAlimentoControllerTest.java`.
- [ ] T038 [P] [US2] Test de contrato: rol no autorizado recibe HTTP 403 y recepción utilizada recibe HTTP 409 — `RecepcionAlimentoControllerTest.java`.
- [ ] T039 [P] [US2] Test de integración: recepción, movimiento inicial y auditoría se actualizan atómicamente — `InventarioPersistenceAdapterTest.java`.

### Implementación de User Story 2

- [ ] T040 [US2] Implementar en `MovimientoAlimentoRepositoryPort` la consulta que determina si la recepción tiene movimientos posteriores a su entrada.
- [ ] T041 [US2] Implementar `EditarRecepcionAlimentoUseCase` con las mismas validaciones y cálculos de US1.
- [ ] T042 [US2] Ajustar el movimiento de entrada original sin eliminar movimientos históricos y registrar valores anteriores y nuevos en auditoría.
- [ ] T043 [US2] Crear `EditarRecepcionAlimentoRequest` sin campos calculados editables.
- [ ] T044 [US2] Implementar `PUT /api/recepciones/alimentos/{id}` en `RecepcionAlimentoController`.

**Checkpoint**: US2 corrige recepciones no utilizadas y protege los saldos y costos históricos de las recepciones utilizadas.

---

## Phase 5: User Story 3 — Consultar el Historial de Recepciones de Alimento (Priority: P2)

**Goal**: El administrador puede consultar, buscar y filtrar las recepciones de alimento, abrir su detalle y acceder a la edición cuando esté permitida.

**Independent Test**: `GET /api/recepciones/alimentos` aplica simultáneamente búsqueda, estado y periodo, devuelve los indicadores del mes y permite consultar el detalle de una recepción sin modificar existencias.

### Definición del evento para User Story 3

**Evento producido**: Ninguno. **Evento consumido**: Ninguno.

El historial y su detalle son consultas de solo lectura. Reflejan recepciones y movimientos confirmados ya persistidos; no publican eventos por abrir una pantalla ni por aplicar filtros.

### Definición de los endpoints REST para User Story 3

| Elemento | Historial | Detalle |
| --- | --- | --- |
| Método y ruta | `GET /api/recepciones/alimentos` | `GET /api/recepciones/alimentos/{id}` |
| Entrada | `q`, `estado`, `desde`, `hasta`, `page`, `size`, `sort` opcionales | `id` UUID en la ruta |
| Respuesta | HTTP 200 paginado, indicadores mensuales, filas y acciones | HTTP 200 con todos los campos históricos en solo lectura |
| Errores | 400 filtros inválidos, 403 y 503 | 403, 404 y 503 |

```json
{
  "indicadores": { "recepcionesMes": 12, "kilogramosMes": 2500.00 },
  "content": [{ "id": "550e8400-e29b-41d4-a716-446655440000", "codigoLote": "L-2026-09", "alimento": "Alimento A", "cantidadBultos": 50, "pesoPorBulto": 50.00, "precioNetoPorBulto": 50000.00, "acciones": ["DETALLES", "EDITAR"] }],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

El detalle de una recepción se solicita con `GET /api/recepciones/alimentos/{id}` y devuelve los datos comerciales y calculados en solo lectura:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "alimento": "Alimento A",
  "tipoAlimento": "INICIO",
  "codigoLote": "L-2026-09",
  "fechaIngreso": "2026-10-01",
  "fechaVencimiento": "2027-01-01",
  "cantidadBultos": 50,
  "pesoPorBulto": 50.00,
  "kilogramosTotales": 2500.00,
  "precioNetoPorBulto": 50000.00,
  "porcentajeImpuesto": 5.00,
  "valorImpuesto": 125000.00,
  "totalCompra": 2625000.00,
  "puedeEditar": true
}
```

### Tests para User Story 3

- [ ] T045 [P] [US3] Test unitario de indicadores: cantidad del mes y suma de kilogramos nominales — `ConsultarHistorialAlimentosUseCaseTest.java`.
- [ ] T046 [P] [US3] Test de contrato para búsqueda por identificador o lote y filtros por estado y periodo — `RecepcionAlimentoControllerTest.java`.
- [ ] T047 [P] [US3] Test de contrato del periodo inicial de 30 días, tabla, estado vacío y acción de edición condicionada — `RecepcionAlimentoControllerTest.java`.
- [ ] T048 [P] [US3] Test de contrato del detalle con todos los campos calculados en modo de solo lectura — `RecepcionAlimentoControllerTest.java`.
- [ ] T049 [P] [US3] Test de integración de filtros combinados y orden descendente por fecha de ingreso — `InventarioPersistenceAdapterTest.java`.

### Implementación de User Story 3

- [ ] T050 [US3] Implementar en `RecepcionAlimentoRepositoryPort` búsqueda por recepción o lote, filtros combinados, paginación e indicadores mensuales.
- [ ] T051 [US3] Implementar `ConsultarHistorialAlimentosUseCase` y `ConsultarDetalleRecepcionAlimentoUseCase`.
- [ ] T052 [US3] Crear `HistorialRecepcionAlimentoResponse` con las seis columnas y las acciones `Detalles` y `Editar`.
- [ ] T053 [US3] Implementar `GET /api/recepciones/alimentos` y `GET /api/recepciones/alimentos/{id}`.

**Checkpoint**: US3 ofrece trazabilidad de recepciones sin convertirse en una consulta de existencias.

---

## Phase 6: User Story 4 — Registrar una Recepción de Medicamento (Priority: P1)

**Goal**: El administrador puede registrar una recepción de medicamento con presentación, contenido y unidad independientes, generar su movimiento de entrada y conservar sus valores históricos.

**Independent Test**: `POST /api/recepciones/medicamentos` con 50 frascos de 100 ml, precio neto por presentación de 20.000 e impuesto del 5 % crea una recepción con 5.000 ml, precio de 200 por ml, subtotal 1.000.000 y total 1.050.000, además de su movimiento de entrada.

### Definición del evento para User Story 4

**Evento producido**: `RecepcionMedicamentoConfirmada` (interno) y `RecepcionMedicamentoConfirmadaV1` (integración).

Se publica después de confirmar recepción y movimiento. Incluye el sobre común del evento, recepción, medicamento y nombre históricos, lote, presentación, cantidad, contenido total, unidad, fechas y valores comerciales históricos. El consumidor debe tolerar reentregas mediante `eventId`.

### Definición del endpoint REST para User Story 4

| Elemento | Definición |
| --- | --- |
| Método y ruta | `POST /api/recepciones/medicamentos` |
| Autorización | `ROLE_ADMINISTRADOR` |
| Entrada | JSON con medicamento, lote, presentación, cantidad, contenido por presentación, unidad, precio neto por presentación, impuesto y fechas. |
| Respuesta | HTTP 201 con cálculos, estado `CONFIRMADA` y movimiento de entrada. |
| Errores | 400, 403, 409 para producto o unidad incompatible y 422 para reglas de negocio inválidas. |

```json
{
  "medicamentoId": "b1c2d3e4-f5a6-47b8-9012-bcdef1234567",
  "codigoLote": "M-2026-04",
  "presentacion": "FRASCO",
  "cantidad": 50,
  "contenidoPorPresentacion": 100.00,
  "unidadMedida": "ML",
  "precioNetoPorPresentacion": 20000.00,
  "porcentajeImpuesto": 5.00,
  "fechaIngreso": "2026-10-01",
  "fechaVencimiento": "2027-10-01"
}
```

La respuesta 201 devuelve los campos calculados y el identificador del movimiento de entrada:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440010",
  "estado": "CONFIRMADA",
  "contenidoTotal": 5000.00,
  "precioNetoPorUnidad": 200.00,
  "subtotalNeto": 1000000.00,
  "valorImpuesto": 50000.00,
  "precioTotal": 1050000.00,
  "movimientoEntradaId": "550e8400-e29b-41d4-a716-446655440011"
}
```

### Tests para User Story 4

- [ ] T054 [P] [US4] Test unitario de contenido total, precio por unidad, subtotal, impuesto y total — `RecepcionMedicamentoTest.java`.
- [ ] T055 [P] [US4] Test unitario de presentación, contenido y unidad como campos independientes, positivos y obligatorios — `RecepcionMedicamentoTest.java`.
- [ ] T056 [P] [US4] Test de contrato: registro válido retorna HTTP 201 y valores calculados — `RecepcionMedicamentoControllerTest.java`.
- [ ] T057 [P] [US4] Test de contrato: valores inválidos, fecha incorrecta, producto inactivo y rol no autorizado no modifican inventario — `RecepcionMedicamentoControllerTest.java`.
- [ ] T058 [P] [US4] Test de integración: recepción y movimiento de entrada se persisten atómicamente y un lote repetido crea otra recepción — `InventarioPersistenceAdapterTest.java`.

### Implementación de User Story 4

- [ ] T059 [US4] Implementar `RegistrarRecepcionMedicamentoUseCase` validando el producto con `MedicamentoCatalogoQueryPort`.
- [ ] T060 [US4] Calcular y persistir contenido total, precio por unidad base, subtotal, impuesto y total como valores históricos.
- [ ] T061 [US4] Crear un movimiento `ENTRADA` confirmado en la unidad base dentro de la misma transacción.
- [ ] T062 [US4] Publicar `RecepcionMedicamentoConfirmada` después del registro transaccional.
- [ ] T063 [US4] Crear los DTOs, mapeos e implementar `POST /api/recepciones/medicamentos`.

**Checkpoint**: US4 registra una recepción de medicamento trazable y conserva la presentación, unidad y precio históricos.

---

## Phase 7: User Story 5 — Editar una Recepción de Medicamento (Priority: P2)

**Goal**: El administrador puede corregir una recepción de medicamento que todavía no tenga salidas, despachos o consumos asociados.

**Independent Test**: `PUT /api/recepciones/medicamentos/{id}` recalcula y audita una recepción no utilizada; una recepción con consumo asociado retorna HTTP 409 y conserva todos sus valores.

### Definición del evento para User Story 5

**Evento producido**: `RecepcionMedicamentoEditada` interno.

Se publica únicamente después de actualizar recepción, movimiento inicial y auditoría. Contiene actor, fecha, valores anteriores y nuevos. Una edición bloqueada no publica ningún evento.

### Definición del endpoint REST para User Story 5

| Elemento | Definición |
| --- | --- |
| Método y ruta | `PUT /api/recepciones/medicamentos/{id}` |
| Autorización | `ROLE_ADMINISTRADOR` |
| Entrada | Medicamento, fecha de vencimiento, cantidad, contenido por presentación, unidad, impuesto y precio neto por presentación. Código de lote, presentación, fecha de ingreso y derivados no son editables. |
| Respuesta | HTTP 200 con los valores recalculados y estado de la recepción. |
| Errores | 400, 403, 404, 409 si existe uso posterior y 422 para fechas, unidades o cálculos inválidos. |

```json
{
  "medicamentoId": "b1c2d3e4-f5a6-47b8-9012-bcdef1234567",
  "fechaVencimiento": "2027-11-01",
  "cantidad": 48,
  "contenidoPorPresentacion": 100.00,
  "unidadMedida": "ML",
  "porcentajeImpuesto": 5.00,
  "precioNetoPorPresentacion": 21000.00
}
```

La respuesta 200 conserva el identificador de la recepción y retorna los nuevos valores calculados y `auditoriaId`.

### Tests para User Story 5

- [ ] T064 [P] [US5] Test unitario de edición permitida con recálculo de contenido, precio unitario, subtotal, impuesto y total — `EditarRecepcionMedicamentoUseCaseTest.java`.
- [ ] T065 [P] [US5] Test unitario de bloqueo cuando existe salida, despacho o consumo — `EditarRecepcionMedicamentoUseCaseTest.java`.
- [ ] T066 [P] [US5] Test de contrato de campos editables, fecha válida, rol administrador y conflictos — `RecepcionMedicamentoControllerTest.java`.
- [ ] T067 [P] [US5] Test de integración de actualización atómica de recepción, entrada y auditoría — `InventarioPersistenceAdapterTest.java`.

### Implementación de User Story 5

- [ ] T068 [US5] Implementar la consulta de movimientos posteriores a la entrada en `MovimientoMedicamentoRepositoryPort`.
- [ ] T069 [US5] Implementar `EditarRecepcionMedicamentoUseCase` reutilizando las validaciones y cálculos de US4.
- [ ] T070 [US5] Ajustar el movimiento inicial y registrar valores anteriores y nuevos en auditoría.
- [ ] T071 [US5] Crear `EditarRecepcionMedicamentoRequest` e implementar `PUT /api/recepciones/medicamentos/{id}`.

**Checkpoint**: US5 permite correcciones seguras sin alterar aplicaciones ni costos históricos ya utilizados.

---

## Phase 8: User Story 6 — Consultar el Historial de Recepciones de Medicamentos (Priority: P2)

**Goal**: El administrador puede consultar, buscar y filtrar recepciones de medicamentos, abrir su detalle y editar cuando sea válido.

**Independent Test**: `GET /api/recepciones/medicamentos` filtra simultáneamente por lote o medicamento, presentación y periodo, muestra el indicador mensual y permite consultar el detalle completo.

### Definición del evento para User Story 6

**Evento producido**: Ninguno. **Evento consumido**: Ninguno.

El historial y el detalle son consultas de solo lectura; no alteran recepciones, movimientos, saldos ni auditorías.

### Definición de los endpoints REST para User Story 6

| Elemento | Historial | Detalle |
| --- | --- | --- |
| Método y ruta | `GET /api/recepciones/medicamentos` | `GET /api/recepciones/medicamentos/{id}` |
| Entrada | `q`, `presentacion`, `desde`, `hasta`, `page`, `size`, `sort` opcionales | `id` UUID en la ruta |
| Respuesta | HTTP 200 paginado, indicador mensual, filas y acciones | HTTP 200 con campos históricos en solo lectura |
| Errores | 400 filtros inválidos, 403 y 503 | 403, 404 y 503 |

```json
{
  "indicador": { "recepcionesMes": 8 },
  "content": [{ "id": "550e8400-e29b-41d4-a716-446655440010", "codigoLote": "M-2026-04", "medicamento": "Medicamento A", "presentacion": "FRASCO", "cantidad": 50, "contenidoPorPresentacion": 100.00, "acciones": ["DETALLES", "EDITAR"] }],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

El detalle de medicamento se obtiene con `GET /api/recepciones/medicamentos/{id}` y devuelve, por ejemplo:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440010",
  "medicamento": "Medicamento A",
  "codigoLote": "M-2026-04",
  "presentacion": "FRASCO",
  "cantidad": 50,
  "contenidoPorPresentacion": 100.00,
  "unidadMedida": "ML",
  "contenidoTotal": 5000.00,
  "fechaIngreso": "2026-10-01",
  "fechaVencimiento": "2027-10-01",
  "precioTotal": 1050000.00,
  "puedeEditar": true
}
```

### Tests para User Story 6

- [ ] T072 [P] [US6] Test unitario del indicador de recepciones confirmadas del mes — `ConsultarHistorialMedicamentosUseCaseTest.java`.
- [ ] T073 [P] [US6] Test de contrato de búsqueda por lote o medicamento y filtros por presentación y periodo — `RecepcionMedicamentoControllerTest.java`.
- [ ] T074 [P] [US6] Test de contrato del periodo inicial de 30 días, columnas, estado vacío y edición condicionada — `RecepcionMedicamentoControllerTest.java`.
- [ ] T075 [P] [US6] Test de contrato del detalle de solo lectura con todos los campos exigidos — `RecepcionMedicamentoControllerTest.java`.
- [ ] T076 [P] [US6] Test de integración de filtros combinados y orden descendente — `InventarioPersistenceAdapterTest.java`.

### Implementación de User Story 6

- [ ] T077 [US6] Implementar búsqueda por lote o medicamento, filtros combinados, paginación e indicador mensual en `RecepcionMedicamentoRepositoryPort`.
- [ ] T078 [US6] Implementar `ConsultarHistorialMedicamentosUseCase` y `ConsultarDetalleRecepcionMedicamentoUseCase`.
- [ ] T079 [US6] Crear `HistorialRecepcionMedicamentoResponse` con las siete columnas y acciones definidas.
- [ ] T080 [US6] Implementar `GET /api/recepciones/medicamentos` y `GET /api/recepciones/medicamentos/{id}`.

**Checkpoint**: US6 proporciona trazabilidad de medicamentos sin modificar sus existencias.

---

## Phase 9: User Story 7 — Consultar Existencias de Alimentos y Medicamentos (Priority: P1)

**Goal**: El administrador puede consultar en una sola pantalla los saldos disponibles de alimentos y medicamentos calculados desde movimientos confirmados.

**Independent Test**: `GET /api/inventario` consolida entradas, salidas, consumos y ajustes; agrupa alimentos por nombre normalizado y tipo aunque sus recepciones tengan pesos por bulto diferentes; separa medicamentos por producto, presentación, contenido y unidad; muestra cero para productos sin saldo y excluye recepciones vencidas o anuladas.

### Definición del evento para User Story 7

**Evento producido**: Ninguno. **Evento consumido**: Ninguno directamente.

La consulta lee movimientos confirmados y puede consultar eventos ya procesados por las capacidades propietarias. No crea movimientos para calcular el saldo ni publica un evento de inventario consultado. El scheduler de vencimientos es un proceso independiente y debe ser idempotente.

### Definición del endpoint REST para User Story 7

| Elemento | Definición |
| --- | --- |
| Método y ruta | `GET /api/inventario` |
| Autorización | `ROLE_ADMINISTRADOR` |
| Entrada | Sin body; filtros y orden opcionales si el contrato de interfaz los habilita. |
| Respuesta | HTTP 200 con tarjetas, tabla de alimentos y tabla de medicamentos. |
| Errores | 401, 403, 409 ante unidades o movimientos incoherentes y 503 si una dependencia imprescindible no está disponible. |

```json
{
  "tarjetasAlimento": { "PRE_INICIO": 1200.00, "INICIO": 2500.00, "BROILER": 800.00, "TOTAL": 4500.00 },
  "alimentos": [{ "etapa": "INICIO", "alimento": "Alimento A", "recepcionesActivas": 2, "proximoVencimiento": "2027-01-01", "stockActualKg": 2500.00, "demanda": "Cumple" }],
  "medicamentos": [{ "medicamento": "Medicamento A", "presentacion": "FRASCO", "cantidad": 50, "contenidoPorPresentacion": 100.00, "unidadMedida": "ML", "stockActual": 5000.00 }],
  "incidencias": []
}
```

### Tests para User Story 7

- [ ] T081 [P] [US7] Test unitario de consolidación de movimientos confirmados y exclusión de pendientes o rechazados — `ConsultarInventarioUseCaseTest.java`.
- [ ] T082 [P] [US7] Test unitario de consolidación de alimentos con pesos por bulto diferentes en una sola existencia por nombre normalizado y tipo — `ConsultarInventarioUseCaseTest.java`.
- [ ] T083 [P] [US7] Test unitario de agrupación de medicamentos sin mezclar unidades incompatibles — `ConsultarInventarioUseCaseTest.java`.
- [ ] T084 [P] [US7] Test unitario de productos activos sin saldo, recepciones anuladas y vencidas — `ConsultarInventarioUseCaseTest.java`.
- [ ] T085 [P] [US7] Test de contrato de tarjetas, columnas exactas, unidades y acceso exclusivo del administrador — `InventarioControllerTest.java`.
- [ ] T086 [P] [US7] Test de integración de vista consistente ante movimientos concurrentes — `InventarioPersistenceAdapterTest.java`.

### Implementación de User Story 7

- [ ] T087 [US7] Crear `ExistenciaAlimento` y `ExistenciaMedicamento` como modelos de consulta calculados.
- [ ] T088 [US7] Implementar consultas agregadas de alimentos por nombre normalizado y tipo, sin agrupar por peso de bulto, y de medicamentos por medicamento, presentación, contenido y unidad compatible.
- [ ] T089 [US7] Implementar `ProcesarRecepcionesVencidasUseCase` y `RecepcionesVencidasScheduler` para registrar de forma idempotente movimientos de vencimiento; la consulta debe excluir vencidos incluso si el proceso aún no se ha ejecutado.
- [ ] T090 [US7] Implementar `ConsultarInventarioUseCase` dentro de una vista transaccional consistente y de solo lectura.
- [ ] T091 [US7] Crear `InventarioResponse` con tarjetas y las dos tablas, sin clasificación `Disponible`, `Stock bajo` o `Sin stock`.
- [ ] T092 [US7] Implementar `GET /api/inventario` en `InventarioController`.

**Checkpoint**: US7 muestra existencias vigentes y reproducibles desde el libro de movimientos, sin modificar inventario durante la consulta.

---

## Phase 10: User Story 8 — Consultar el Resumen del Inventario en Inicio (Priority: P2)

**Goal**: El administrador puede visualizar el porcentaje de ocupación de la bodega y las cinco recepciones de alimento más recientes.

**Independent Test**: `GET /api/inventario/resumen` con capacidad y ocupación en la misma unidad calcula `ocupación ÷ capacidad × 100` y devuelve únicamente las cinco recepciones confirmadas más recientes.

### Definición del evento para User Story 8

**Evento producido**: Ninguno. **Evento consumido**: Ninguno directamente.

El resumen se calcula como lectura de la bodega y de recepciones confirmadas; abrir o actualizar la pantalla no modifica ocupación ni genera movimientos.

### Definición del endpoint REST para User Story 8

| Elemento | Definición |
| --- | --- |
| Método y ruta | `GET /api/inventario/resumen` |
| Autorización | `ROLE_ADMINISTRADOR` |
| Entrada | Sin body. |
| Respuesta | HTTP 200 con capacidad, ocupación, unidad, porcentaje y hasta cinco recepciones recientes. |
| Errores | 403, 409 si las unidades son incompatibles o capacidad es inválida y 503 si la bodega no está disponible. |

```json
{
  "capacidadMaxima": 10000.00,
  "ocupacionActual": 4500.00,
  "unidad": "KG",
  "porcentajeOcupacion": 45.00,
  "recepcionesRecientes": [{ "id": "550e8400-e29b-41d4-a716-446655440000", "alimento": "Alimento A", "kilogramosTotales": 2500.00, "fechaIngreso": "2026-10-01" }]
}
```

### Tests para User Story 8

- [ ] T093 [P] [US8] Test unitario de porcentaje de ocupación, capacidad cero y unidades incompatibles — `ConsultarResumenInventarioUseCaseTest.java`.
- [ ] T094 [P] [US8] Test unitario de selección y orden de las cinco recepciones recientes — `ConsultarResumenInventarioUseCaseTest.java`.
- [ ] T095 [P] [US8] Test de contrato de campos del resumen y acceso exclusivo del administrador — `InventarioControllerTest.java`.
- [ ] T096 [P] [US8] Test que verifica que actualizar el resumen no crea movimientos ni modifica la bodega — `InventarioControllerTest.java`.

### Implementación de User Story 8

- [ ] T097 [US8] Implementar en `BodegaCentralRepositoryPort` la consulta de capacidad y ocupación vigentes en una unidad común.
- [ ] T098 [US8] Implementar la consulta de las cinco recepciones de alimento confirmadas más recientes.
- [ ] T099 [US8] Implementar `ConsultarResumenInventarioUseCase` y crear `ResumenInventarioResponse`.
- [ ] T100 [US8] Implementar `GET /api/inventario/resumen` en `InventarioController`.

**Checkpoint**: US8 ofrece un resumen de solo lectura sin reemplazar la consulta detallada del inventario.

---

## Phase 11: User Story 9 — Consultar Cobertura del Requerimiento de Alimento (Priority: P1)

**Goal**: El administrador puede comparar el stock alimenticio con la demanda consolidada y abrir el detalle de la cobertura sin reservar ni descontar existencias.

**Independent Test**: Filas con combinaciones de stock y demanda muestran respectivamente `Cumple`, `Cobertura Parcial`, `Sin Cobertura` y `Sin Requerimiento`; el detalle presenta alimento, etapa, stock y demanda en modo de solo lectura.

### Definición del evento para User Story 9

**Evento producido**: Ninguno. **Evento consumido**: Ninguno directamente.

La cobertura es una comparación efímera entre stock y demanda. Consultarla o cerrar el detalle no reserva bultos, no descuenta saldo y no crea movimientos.

### Definición del endpoint REST para User Story 9

| Elemento | Definición |
| --- | --- |
| Método y ruta | `GET /api/inventario/alimentos/{alimentoId}/requerimiento` |
| Autorización | `ROLE_ADMINISTRADOR` |
| Entrada | `alimentoId` UUID y parámetro `tipoAlimento` cuando sea necesario para identificar la agrupación. No recibe body. |
| Respuesta | HTTP 200 con alimento, etapa, stock actual, demanda, faltante y estado de cobertura. |
| Errores | 400 criterios inválidos, 403, 404 alimento inexistente y 503 si demanda o inventario no están disponibles. |

```json
{
  "alimento": "Alimento A",
  "etapa": "INICIO",
  "stockActualKg": 2500.00,
  "demandaKg": 2200.00,
  "faltanteKg": 0.00,
  "estado": "Cumple"
}
```

### Tests para User Story 9

- [ ] T101 [P] [US9] Test unitario de los cuatro estados de cobertura y del cálculo de faltante — `CoberturaRequerimientoAlimentoTest.java`.
- [ ] T102 [P] [US9] Test unitario de consolidación de demanda por alimento y tipo, manteniendo la etapa mostrada en el detalle — `ConsultarCoberturaAlimentoUseCaseTest.java`.
- [ ] T103 [P] [US9] Test de contrato de etiquetas, campos del detalle y ausencia de demanda en medicamentos — `InventarioControllerTest.java`.
- [ ] T104 [P] [US9] Test de contrato que verifica que consultar o cerrar el detalle no crea reservas ni movimientos — `InventarioControllerTest.java`.

### Implementación de User Story 9

- [ ] T105 [US9] Crear `EstadoCoberturaAlimento` con `CUMPLE`, `COBERTURA_PARCIAL`, `SIN_COBERTURA` y `SIN_REQUERIMIENTO`.
- [ ] T106 [US9] Implementar `CoberturaRequerimientoAlimento` con stock, demanda, faltante y estado derivado.
- [ ] T107 [US9] Implementar `ConsultarCoberturaAlimentoUseCase` usando existencias y `RequerimientoAlimentoQueryPort` sin generar reservas.
- [ ] T108 [US9] Incorporar el estado de cobertura en cada fila alimenticia de `InventarioResponse` sin agregarlo a medicamentos.
- [ ] T109 [US9] Implementar `GET /api/inventario/alimentos/{alimentoId}/requerimiento` con el tipo de alimento como criterio de la agrupación; el peso por bulto no forma parte de la identidad de la existencia.

**Checkpoint**: US9 informa cobertura y faltantes sin modificar los saldos ni introducir demanda para medicamentos.

---

## Phase 12: Polish & Cross-Cutting Concerns

**Purpose**: Completar documentación, calidad, rendimiento, eventos y seguridad del feature.

- [ ] T110 [P] Documentar los once endpoints, filtros, paginación, respuestas y errores mediante OpenAPI.
- [ ] T111 [P] Crear `RecepcionAlimentoConfirmadaV1` y `RecepcionMedicamentoConfirmadaV1` con los precios históricos requeridos por el Módulo 3.
- [ ] T112 [P] Crear una prueba de arquitectura que impida dependencias de Spring, JPA y Kafka dentro de `domain/`.
- [ ] T113 Verificar publicación idempotente y reintentos de eventos sin duplicar recepciones ni movimientos.
- [ ] T114 Ejecutar todas las pruebas, formato y análisis estático mediante `gradlew test`.
- [ ] T115 Ejecutar pruebas end-to-end con PostgreSQL y Kafka Testcontainers para registro, edición, historial, vencimiento e inventario.
- [ ] T116 Verificar los objetivos de 2 segundos con el volumen de recepciones y movimientos acordado.
- [ ] T117 Revisar autorización, auditoría, logs, correlación y ausencia de datos comerciales sensibles innecesarios en eventos.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias; utiliza el proyecto Gradle existente.
- **Foundational (Phase 2)**: Depende de Setup y bloquea todas las user stories.
- **US1 - Registrar alimento (Phase 3)**: Depende de Foundational.
- **US2 - Editar alimento (Phase 4)**: Depende de US1 porque modifica una recepción existente y su entrada.
- **US3 - Historial de alimentos (Phase 5)**: Depende de US1; su consulta puede desarrollarse en paralelo con US2.
- **US4 - Registrar medicamento (Phase 6)**: Depende de Foundational y puede desarrollarse en paralelo con US1.
- **US5 - Editar medicamento (Phase 7)**: Depende de US4.
- **US6 - Historial de medicamentos (Phase 8)**: Depende de US4; puede desarrollarse en paralelo con US5.
- **US7 - Consultar inventario (Phase 9)**: Depende de US1 y US4 para disponer de movimientos de entrada.
- **US8 - Resumen de inventario (Phase 10)**: Depende de US1 y de la configuración de bodega; puede desarrollarse en paralelo con US7.
- **US9 - Cobertura alimenticia (Phase 11)**: Depende de US7 y del contrato de requerimientos nutricionales.
- **Polish (Phase 12)**: Depende de todas las user stories incluidas en la entrega.

### User Story Dependencies

- **US1 (P1)**: Inicia después de Foundational.
- **US2 (P2)**: Depende de US1.
- **US3 (P2)**: Depende de US1 y reutiliza la regla de edición de US2 para habilitar la acción correspondiente.
- **US4 (P1)**: Inicia después de Foundational sin depender de las stories de alimento.
- **US5 (P2)**: Depende de US4.
- **US6 (P2)**: Depende de US4 y reutiliza la regla de edición de US5.
- **US7 (P1)**: Depende de los movimientos generados por US1 y US4.
- **US8 (P2)**: Depende de la bodega central y de las recepciones de US1.
- **US9 (P1)**: Depende de las existencias de US7 y del puerto de demanda nutricional.

### Dentro de cada User Story

- Modelo y regla de dominio antes que caso de uso.
- Puerto de salida antes que adaptador.
- Caso de uso antes que controlador o tarea programada.
- DTOs y mappers junto al adaptador que los utiliza.
- Tests escritos junto a la implementación de cada tarea.
- Checkpoint verificado antes de considerar completada la fase.

---

## Notes

- El tag `[P]` identifica tareas que pueden ejecutarse en paralelo porque no modifican los mismos archivos.
- Los tags `[US1]` a `[US9]` relacionan cada tarea con una user story para mantener trazabilidad.
- **Recepción frente a existencia**: una recepción es un hecho comercial histórico; el stock se calcula desde movimientos confirmados y nunca sobrescribiendo la recepción.
- **Identidad**: cada recepción usa UUID propio. El código de lote puede repetirse y no se utiliza como llave primaria.
- **Edición**: solo se permite mientras no existan movimientos de salida, despacho o consumo; los valores calculados nunca llegan como campos editables.
- **Vencimientos**: el saldo vencido se excluye inmediatamente de la consulta y se regulariza mediante un movimiento idempotente de vencimiento para conservar trazabilidad.
- **Cálculos**: cantidades y valores monetarios usan `BigDecimal`; la escala y el redondeo se centralizan para evitar diferencias entre registro, persistencia, inventario y auditoría.
- **Medicamentos**: presentación, contenido por presentación y unidad de medida son campos independientes. Presentación y unidad se modelan como enums, pero sus valores concretos no se definen en este plan.
- **Demanda**: únicamente los alimentos muestran cobertura. Los medicamentos no tienen demanda nutricional ni estados de cobertura.
- **Stock**: no se utilizan clasificaciones generales `Disponible`, `Stock bajo` ni `Sin stock`; los únicos estados visuales corresponden a la cobertura del requerimiento de alimento.
- **Ocupación**: el porcentaje solo se calcula cuando capacidad máxima y ocupación actual están expresadas en una unidad compatible.
- **Agrupación de alimentos**: el stock consolida por nombre normalizado y tipo, aunque el peso por bulto sea diferente. Cantidad y peso por bulto permanecen en cada recepción del historial.
- **Construcción**: General.md declara Gradle Wrapper, mientras el repositorio inspeccionado contiene `pom.xml` y `mvnw.cmd`. Debe resolverse esa diferencia antes de fijar el comando de entrega; no se migra la herramienta de construcción dentro de este plan.
- **Eventos**: las recepciones confirmadas publican contratos versionados con los valores históricos mínimos que requiere el Módulo 3; los consumidores deben ser idempotentes.
