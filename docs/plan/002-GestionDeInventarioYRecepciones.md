# Implementation Plan: Gestión de Inventario y Recepciones

**Date**: 25/09/2026  
**Arquitectura y tecnologías**: [General.md](General.md)  
**Specs**:

- [001-RegistrarRecepcionDeAlimento.md](../specs/001-RegistrarRecepcionDeAlimento.md)
- [002-RegistroRecepcionDeMedicamento.md](../specs/002-RegistroRecepcionDeMedicamento.md)
- [023-ConsultarInventario.md](../specs/023-ConsultarInventario.md)

## Summary

El administrador debe poder registrar, editar y consultar el historial de las recepciones de alimentos y medicamentos que ingresan a la bodega central. Cada recepción confirmada conserva sus datos comerciales históricos, realiza los cálculos correspondientes y genera un movimiento de entrada que permite conocer las existencias disponibles sin alterar el registro original de la compra.

El inventario se calcula a partir de movimientos confirmados y se presenta en dos secciones independientes: alimentos y medicamentos. Los alimentos se consolidan por etapa, producto y peso nominal por bulto; los medicamentos se consolidan por producto, presentación y unidad de medida. Las cantidades vencidas o anuladas no forman parte del stock disponible y los productos activos sin saldo se presentan con existencia cero.

La pantalla de inventario también muestra la ocupación de la bodega, las recepciones recientes y la cobertura del requerimiento alimenticio. La demanda solo aplica a alimentos y se consulta mediante un puerto hacia el contexto nutricional; consultar la cobertura no reserva existencias ni genera movimientos.

## Technical Context

**Base técnica**: Definida en [General.md](General.md)  
**Integraciones específicas**: Catálogos de alimentos y medicamentos, requerimientos nutricionales y publicación de valores históricos para el Módulo 3  
**Datos específicos**: Bodega central, recepciones, movimientos de inventario y auditoría  
**Performance Goals**: El 95 % de los registros y ediciones debe completarse en máximo 2 segundos; el 95 % de los historiales y la consulta consolidada debe responder en máximo 2 segundos  
**Constraints**: Acceso exclusivo del administrador; recepción y movimiento de entrada atómicos; los códigos de lote no son identificadores únicos; las recepciones utilizadas no se editan; vencimientos y anulaciones no aportan stock; la demanda solo existe para alimentos  
**Scale/Scope**: Nueve historias de usuario, once endpoints REST, una bodega central, dos tipos de recepción, dos libros de movimientos y eventos de integración para precios históricos

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

**Independent Test**: `GET /api/inventario` consolida entradas, salidas, consumos y ajustes; separa alimentos por etapa, producto y peso por bulto; separa medicamentos por producto, presentación y unidad; muestra cero para productos sin saldo y excluye recepciones vencidas o anuladas.

### Tests para User Story 7

- [ ] T081 [P] [US7] Test unitario de consolidación de movimientos confirmados y exclusión de pendientes o rechazados — `ConsultarInventarioUseCaseTest.java`.
- [ ] T082 [P] [US7] Test unitario de agrupación de alimentos sin mezclar pesos por bulto diferentes — `ConsultarInventarioUseCaseTest.java`.
- [ ] T083 [P] [US7] Test unitario de agrupación de medicamentos sin mezclar unidades incompatibles — `ConsultarInventarioUseCaseTest.java`.
- [ ] T084 [P] [US7] Test unitario de productos activos sin saldo, recepciones anuladas y vencidas — `ConsultarInventarioUseCaseTest.java`.
- [ ] T085 [P] [US7] Test de contrato de tarjetas, columnas exactas, unidades y acceso exclusivo del administrador — `InventarioControllerTest.java`.
- [ ] T086 [P] [US7] Test de integración de vista consistente ante movimientos concurrentes — `InventarioPersistenceAdapterTest.java`.

### Implementación de User Story 7

- [ ] T087 [US7] Crear `ExistenciaAlimento` y `ExistenciaMedicamento` como modelos de consulta calculados.
- [ ] T088 [US7] Implementar consultas agregadas de alimentos por etapa, alimento y peso por bulto, y de medicamentos por medicamento, presentación y unidad.
- [ ] T089 [US7] Implementar `ProcesarRecepcionesVencidasUseCase` y `RecepcionesVencidasScheduler` para registrar de forma idempotente movimientos de vencimiento; la consulta debe excluir vencidos incluso si el proceso aún no se ha ejecutado.
- [ ] T090 [US7] Implementar `ConsultarInventarioUseCase` dentro de una vista transaccional consistente y de solo lectura.
- [ ] T091 [US7] Crear `InventarioResponse` con tarjetas y las dos tablas, sin clasificación `Disponible`, `Stock bajo` o `Sin stock`.
- [ ] T092 [US7] Implementar `GET /api/inventario` en `InventarioController`.

**Checkpoint**: US7 muestra existencias vigentes y reproducibles desde el libro de movimientos, sin modificar inventario durante la consulta.

---

## Phase 10: User Story 8 — Consultar el Resumen del Inventario en Inicio (Priority: P2)

**Goal**: El administrador puede visualizar el porcentaje de ocupación de la bodega y las cinco recepciones de alimento más recientes.

**Independent Test**: `GET /api/inventario/resumen` con capacidad y ocupación en la misma unidad calcula `ocupación ÷ capacidad × 100` y devuelve únicamente las cinco recepciones confirmadas más recientes.

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

### Tests para User Story 9

- [ ] T101 [P] [US9] Test unitario de los cuatro estados de cobertura y del cálculo de faltante — `CoberturaRequerimientoAlimentoTest.java`.
- [ ] T102 [P] [US9] Test unitario de consolidación de demanda por etapa, alimento y peso nominal por bulto — `ConsultarCoberturaAlimentoUseCaseTest.java`.
- [ ] T103 [P] [US9] Test de contrato de etiquetas, campos del detalle y ausencia de demanda en medicamentos — `InventarioControllerTest.java`.
- [ ] T104 [P] [US9] Test de contrato que verifica que consultar o cerrar el detalle no crea reservas ni movimientos — `InventarioControllerTest.java`.

### Implementación de User Story 9

- [ ] T105 [US9] Crear `EstadoCoberturaAlimento` con `CUMPLE`, `COBERTURA_PARCIAL`, `SIN_COBERTURA` y `SIN_REQUERIMIENTO`.
- [ ] T106 [US9] Implementar `CoberturaRequerimientoAlimento` con stock, demanda, faltante y estado derivado.
- [ ] T107 [US9] Implementar `ConsultarCoberturaAlimentoUseCase` usando existencias y `RequerimientoAlimentoQueryPort` sin generar reservas.
- [ ] T108 [US9] Incorporar el estado de cobertura en cada fila alimenticia de `InventarioResponse` sin agregarlo a medicamentos.
- [ ] T109 [US9] Implementar `GET /api/inventario/alimentos/{alimentoId}/requerimiento` con etapa y peso por bulto como criterios de la agrupación.

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
- **Eventos**: las recepciones confirmadas publican contratos versionados con los valores históricos mínimos que requiere el Módulo 3; los consumidores deben ser idempotentes.
