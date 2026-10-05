# Implementation Plan: Gestión del Inventario

**Date**: 04/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [023-ConsultarInventario.md](../specs/023-ConsultarInventario.md)

## Summary

Implementar la bodega central, el libro de movimientos y las consultas de inventario de alimentos y medicamentos. El saldo disponible se calcula desde movimientos confirmados por recepción, salidas, consumos, vencimientos, anulaciones y ajustes.

Este plan es propietario de BodegaCentral, MovimientoAlimento y MovimientoMedicamento, saldos por recepción y resultados consolidados de inventario. Consume las recepciones creadas por el Plan 002 y ofrece un puerto para registrar o ajustar la entrada inicial de una recepción.

La consulta de inventario presenta alimentos y medicamentos en secciones separadas. Los alimentos se agrupan por nombre normalizado y tipo; los medicamentos por medicamento, presentación, contenido y unidad compatibles. La cobertura compara la demanda nutricional con el stock de alimentos, sin reservar ni descontar existencias.

## Technical Context

**Performance Goals**: El 95 % de las consultas consolidadas responde en máximo 2 segundos. El cálculo de una entrada, salida o consumo actualiza su saldo en una transacción local.

**Constraints**: Solo el administrador consulta este inventario. Los movimientos se confirman con cantidades y unidades compatibles. Vencidos y anulados no están disponibles. Las consultas son de solo lectura. La demanda nutricional solo aplica a alimentos.

**Scale/Scope**: Tres historias del spec 023, libro de movimientos para dos familias de productos, vencimientos, FEFO, resumen de bodega y cobertura de requerimientos.

**Dependencias funcionales**: Recepciones del Plan 002; catálogo de alimentos y medicamentos; requerimientos nutricionales y suministros diarios del Plan 004 (SPEC-014); consumos de medicamentos del Plan 006; consumo y costo del Módulo 3.

### Decisiones específicas

1. **Propiedad**: BodegaCentral, movimientos y saldos pertenecen a este plan. RecepcionAlimento y RecepcionMedicamento pertenecen al Plan 002.
2. **Saldo**: Existencia = entradas confirmadas - salidas - consumos - vencimientos - anulaciones +/− ajustes confirmados, conservando el saldo individual de cada recepción.
3. **Recepción confirmada**: RegistrarEntradaInventarioPort crea el movimiento inicial vinculado a la recepción. El puerto también permite ajustar esa entrada cuando el Plan 002 edita una recepción no utilizada.
4. **FEFO**: Las salidas y consumos usan primero el saldo vigente con vencimiento más próximo. Cada movimiento referencia la recepción afectada.
5. **Vencimiento**: Una recepción vencida queda fuera del disponible aunque el scheduler aún no haya creado el movimiento de vencimiento. El scheduler registra ese movimiento de forma idempotente.
6. **Unidades**: Alimentos se consolidan en kg; medicamentos se conservan en su unidad base y no se mezclan unidades incompatibles.
7. **Agrupación**: Alimentos se agrupan por nombre normalizado y tipo. Medicamentos por medicamento, presentación, contenido por presentación y unidad.
8. **Cobertura**: La demanda de alimento se consulta por puerto. La cobertura es informativa: no reserva, descuenta ni crea movimientos.
9. **Ocupación**: La ocupación registrada de la bodega se consulta con su unidad y se compara con su capacidad. No se infiere mezclando kg de alimentos con unidades de medicamentos.
10. **Eventos**: Se consumen recepciones confirmadas y consumos de otros planes. Los movimientos confirmados publican eventos para consumidores; los consumidores son idempotentes.
11. **Seguridad**: Todos los endpoints de este plan requieren ROLE_ADMINISTRADOR.
12. **Persistencia**: Este plan crea tablas de bodega, movimientos y eventos procesados. No crea tablas de recepción.
13. **Suministro diario de alimento**: Plan 004 solicita la salida al registrar alimento realmente suministrado a un lote. Esta capacidad valida y descuenta las recepciones compatibles mediante FEFO, guarda una operación de salida idempotente y devuelve su referencia y los movimientos generados. La llamada participa en la transacción local que confirma el suministro; no se confirma una deducción parcial.

### Alcance documental

- El Plan 002 conserva los datos comerciales de la recepción y llama a RegistrarEntradaInventarioPort.
- El Plan 006 consume InventarioMedicamentoPort para descontar medicamentos por recepción.
- El Plan 004 ofrece la demanda alimenticia proyectada.
- El Módulo 3 consume movimientos y detalles históricos para costos.
- La versión de migración debe ser la siguiente disponible y no debe duplicar las versiones existentes.

## Project Structure

### Documentation (this feature)

    docs/
    ├── specs/
    │   └── 023-ConsultarInventario.md
    └── plan/
        └── 010-GestionDelInventario.md

### Source Code (repository root)

    src/main/java/com/avicontrol/
    ├── domain/
    │   ├── model/inventario/
    │   │   ├── BodegaCentral.java
    │   │   ├── MovimientoAlimento.java
    │   │   ├── MovimientoMedicamento.java
    │   │   ├── ExistenciaAlimento.java
    │   │   ├── ExistenciaMedicamento.java
    │   │   ├── CoberturaRequerimientoAlimento.java
    │   │   ├── TipoMovimientoInventario.java
    │   │   ├── EstadoMovimientoInventario.java
    │   │   └── EstadoCoberturaAlimento.java
    │   ├── exception/inventario/
    │   │   ├── MovimientoInvalidoException.java
    │   │   ├── RecepcionNoEncontradaException.java
    │   │   ├── ExistenciasInsuficientesException.java
    │   │   ├── UnidadIncompatibleException.java
    │   │   └── BodegaNoDisponibleException.java
    │   └── port/out/inventario/
    │       ├── BodegaCentralRepositoryPort.java
    │       ├── MovimientoAlimentoRepositoryPort.java
    │       ├── MovimientoMedicamentoRepositoryPort.java
    │       ├── RecepcionQueryPort.java
    │       ├── AlimentoCatalogoQueryPort.java
    │       ├── MedicamentoCatalogoQueryPort.java
    │       ├── RequerimientoAlimentoQueryPort.java
    │       └── InventarioEventPublisherPort.java
    ├── application/inventario/
    │   ├── RegistrarEntradaInventarioUseCase.java
    │   ├── AjustarEntradaRecepcionUseCase.java
    │   ├── ConsultarInventarioUseCase.java
    │   ├── ConsultarResumenInventarioUseCase.java
    │   ├── ConsultarCoberturaAlimentoUseCase.java
    │   └── ProcesarRecepcionesVencidasUseCase.java
    └── infrastructure/
        ├── adapter/in/rest/inventario/
        │   └── InventarioController.java
        ├── adapter/in/scheduling/
        │   └── RecepcionesVencidasScheduler.java
        ├── adapter/in/event/inventario/
        │   └── InventarioEventListener.java
        ├── adapter/out/internal/inventario/
        │   ├── RecepcionQueryAdapter.java
        │   ├── AlimentoCatalogoQueryAdapter.java
        │   ├── MedicamentoCatalogoQueryAdapter.java
        │   └── RequerimientoAlimentoQueryAdapter.java
        ├── adapter/out/persistence/inventario/
        │   ├── BodegaPersistenceAdapter.java
        │   ├── MovimientoAlimentoPersistenceAdapter.java
        │   └── MovimientoMedicamentoPersistenceAdapter.java
        └── config/InventarioBeanConfiguration.java

    src/main/resources/db/migration/
    ├── V__crear_bodega_central.sql
    ├── V__crear_movimientos_inventario.sql
    └── V__crear_eventos_procesados_inventario.sql

    src/test/java/com/avicontrol/
    ├── domain/inventario/
    ├── application/inventario/
    ├── infrastructure/adapter/in/rest/inventario/
    └── integration/InventarioIntegrationTest.java

**Structure Decision**: El libro de movimientos es independiente de las recepciones, pero cada movimiento conserva la referencia a la recepción afectada. Los resultados de existencia, resumen y cobertura son modelos de lectura y no tablas adicionales.

### Entidades y relaciones

    BodegaCentral
    ├── id: UUID
    ├── capacidadMaxima: BigDecimal
    ├── ocupacionActual: BigDecimal
    └── unidadCapacidad: String

    MovimientoAlimento
    ├── id: UUID
    ├── recepcionId: UUID
    ├── tipo: ENTRADA | SALIDA | CONSUMO | VENCIMIENTO | ANULACION | AJUSTE
    ├── cantidadKg: BigDecimal
    ├── estado: CONFIRMADO | ANULADO
    ├── fechaHora: Instant
    └── actorId: UUID

    MovimientoMedicamento
    ├── id: UUID
    ├── recepcionId: UUID
    ├── tipo: ENTRADA | SALIDA | APLICACION | CONSUMO | VENCIMIENTO | ANULACION | AJUSTE
    ├── cantidadUnidadBase: BigDecimal
    ├── unidadBase: String
    ├── estado: CONFIRMADO | ANULADO
    └── fechaHora: Instant

ExistenciaAlimento, ExistenciaMedicamento y CoberturaRequerimientoAlimento son resultados calculados. La recepción conserva los precios comerciales; el movimiento conserva cantidad y trazabilidad del saldo.

### Contratos de los puertos

| Puerto | Responsabilidad |
| --- | --- |
| BodegaCentralRepositoryPort | Leer capacidad y ocupación de la bodega. |
| MovimientoAlimentoRepositoryPort | Crear entradas, salidas, vencimientos y ajustes; consultar saldos y FEFO de alimento. |
| SalidaAlimentoCommand | Comando interno | Registrar una salida atómica solicitada por Plan 004, con clave de idempotencia, producto, cantidad, actor y referencia al suministro; devolver la operación y sus movimientos por recepción. |
| MovimientoMedicamentoRepositoryPort | Crear entradas, consumos, vencimientos y ajustes; consultar saldos por unidad base. |
| RecepcionQueryPort | Confirmar recepción existente, fechas, producto y estado. |
| AlimentoCatalogoQueryPort | Obtener nombre normalizado, tipo y peso nominal. |
| MedicamentoCatalogoQueryPort | Obtener unidad base y presentación compatible. |
| RequerimientoAlimentoQueryPort | Consultar demanda proyectada por alimento y tipo. |
| InventarioEventPublisherPort | Publicar movimientos confirmados y cambios de disponibilidad. |

### Contratos HTTP propuestos

| Endpoint | Acceso | Resultado |
| --- | --- | --- |
| GET /api/inventario | Administrador | Existencias consolidadas de alimentos y medicamentos. |
| GET /api/inventario/resumen | Administrador | Ocupación y cinco recepciones de alimento más recientes. |
| GET /api/inventario/alimentos/{alimentoId}/requerimiento | Administrador | Cobertura de stock frente a demanda. |

Las consultas son de solo lectura y responden 200 incluso con inventario vacío. Errores de dependencia responden 503; inconsistencias de datos o unidades 409. Se usa application/problem+json.

### JSON común de errores

```json
    {
      "type": "https://avicontrol/errors/inventario-inconsistente",
      "title": "Inventario no disponible",
      "status": 409,
      "detail": "La unidad del movimiento no es compatible con la existencia",
      "instance": "/api/inventario",
      "code": "UNIDAD_INCOMPATIBLE",
      "correlationId": "uuid",
      "fieldErrors": []
    }
```

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Preparar el libro de movimientos y sus contratos con recepción, consumo y nutrición.

- [ ] T001 Crear paquetes de dominio, aplicación y adaptadores de inventario.
- [ ] T002 Acordar con Plan 002 los contratos de RegistrarEntradaInventarioPort, AjustarEntradaRecepcionPort y RecepcionQueryPort.
- [ ] T003 Acordar con Plan 006 el descuento de medicamentos, unidad base y distribución FEFO.
- [ ] T004 Acordar con Plan 004 la demanda de alimento y su clave de agrupación.
- [ ] T005 Acordar eventos de movimiento, vencimiento y disponibilidad, con consumidores idempotentes.
- [ ] T006 Preparar fixtures de múltiples recepciones, vencimientos, anulaciones, salidas, consumos y unidades incompatibles.
- [ ] T007 Acordar con Plan 004 el contrato de salida de alimento para suministros diarios, la propagación de su clave idempotente y la atomicidad de suministro más inventario.

**Checkpoint**: Contratos y responsabilidades de inventario definidos.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Preparar entidades, movimientos, saldos, persistencia y puertos.

- [ ] T007 Implementar BodegaCentral y reglas de capacidad/ocupación.
- [ ] T008 Implementar MovimientoAlimento y MovimientoMedicamento con estados, tipos y cantidades base.
- [ ] T009 Implementar cálculo de saldo por recepción, consolidación y FEFO.
- [ ] T010 Definir puertos de recepción, catálogo, nutrición y publicación.
- [ ] T011 Crear migraciones de bodega, movimientos y eventos procesados.
- [ ] T012 Implementar listener/adaptador para entradas confirmadas por el Plan 002 y consumos del Plan 006.
- [ ] T013 Implementar scheduler idempotente de vencimientos.
- [ ] T014 Verificar que las consultas no escriban movimientos ni saldos.

**Checkpoint**: Libro de movimientos listo para las tres historias de consulta.

## Phase 3: User Story 1 — Consultar Existencias de Alimentos y Medicamentos (Priority: P1)

**Spec**: 023, historia 1.

**Goal**: El administrador consulta existencias consolidadas y trazables.

**Independent Test**: Varias recepciones del mismo alimento se consolidan en kg; medicamentos incompatibles no se mezclan; vencidos y anulados se excluyen.

### Definición del evento

**Evento producido**: Ninguno.

**Evento consumido**: RecepcionAlimentoConfirmadaV1, RecepcionMedicamentoConfirmadaV1 y movimientos confirmados de consumos, salidas, vencimientos y ajustes.

La consulta no crea eventos ni modifica saldo.

### Definición del endpoint REST

| Elemento | Definición |
| --- | --- |
| Método y ruta | GET /api/inventario |
| Autorización | ROLE_ADMINISTRADOR |
| Respuesta 200 | Secciones Stock de alimentos y Stock de medicamentos. |
| Alimentos | Agrupados por nombre normalizado y tipo, en kg, con recepciones activas, próximo vencimiento y demanda. |
| Medicamentos | Agrupados por medicamento, presentación, contenido y unidad compatible. |
| Errores | 401/403, 409 por datos incompatibles y 503 por fuente no disponible. |

#### JSON de respuesta

```json
    {
      "alimentos": [
        {
          "tipoAlimento": "INICIO",
          "alimento": "Alimento A",
          "recepcionesActivas": 2,
          "proximoVencimiento": "2027-01-04",
          "stockActualKg": 2000,
          "demandaKg": 1250,
          "estadoDemanda": "CUMPLE"
        }
      ],
      "medicamentos": [
        {
          "medicamento": "Amprolio",
          "presentacion": "FRASCO",
          "cantidad": 20,
          "contenidoPorPresentacion": 500,
          "unidadMedida": "MILILITRO",
          "stockActual": 8000
        }
      ]
    }
```

### Tests e implementación

- [ ] T015 [US1] Probar consolidación de alimentos por nombre normalizado y tipo.
- [ ] T016 [US1] Probar medicamentos por presentación/contenido/unidad sin mezclar incompatibles.
- [ ] T017 [US1] Probar vencidos, anulados y productos con stock cero.
- [ ] T018 [US1] Probar consulta de solo lectura y autorización.
- [ ] T019 [US1] Implementar ConsultarInventarioUseCase, resultados, DTOs y endpoint.

**Checkpoint**: Existencias vigentes calculadas desde movimientos confirmados.

## Phase 4: User Story 2 — Consultar Resumen del Inventario (Priority: P2)

**Spec**: 023, historia 2.

**Goal**: El administrador visualiza ocupación y recepciones recientes.

**Independent Test**: La ocupación se calcula con capacidad y valor vigente, y solo se muestran cinco recepciones de alimento ordenadas de la más nueva a la más antigua.

### Definición del evento

**Evento producido**: Ninguno.

**Evento consumido**: Ninguno obligatorio; el resumen consulta recepciones y bodega vigentes.

### Definición del endpoint REST

| Elemento | Definición |
| --- | --- |
| Método y ruta | GET /api/inventario/resumen |
| Autorización | ROLE_ADMINISTRADOR |
| Respuesta 200 | Porcentaje de ocupación y cinco recepciones de alimento recientes. |
| Regla | No crea ni modifica recepciones, movimientos o saldos. |

#### JSON de respuesta

```json
    {
      "capacidadMaxima": 10000,
      "ocupacionActual": 6500,
      "unidadCapacidad": "KILOGRAMO",
      "porcentajeOcupacion": 65,
      "recepcionesAlimentoRecientes": [
        {
          "recepcionId": "uuid",
          "codigoLote": "LOTE-2026-01",
          "alimento": "Alimento A",
          "cantidadBultos": 50,
          "precioNetoPorBulto": 125000,
          "pesoPorBultoKg": 40,
          "fechaIngreso": "2026-10-04"
        }
      ]
    }
```

### Tests e implementación

- [ ] T020 [US2] Probar cálculo de ocupación, capacidad cero y valores fuera de rango.
- [ ] T021 [US2] Probar exactamente cinco recepciones y orden descendente.
- [ ] T022 [US2] Probar consulta sin escrituras.
- [ ] T023 [US2] Implementar ConsultarResumenInventarioUseCase y endpoint.

**Checkpoint**: El resumen es una lectura rápida y consistente de la bodega.

## Phase 5: User Story 3 — Consultar Cobertura del Requerimiento de Alimento (Priority: P1)

**Spec**: 023, historia 3.

**Goal**: El administrador compara stock consolidado y demanda nutricional sin reservar alimento.

**Independent Test**: Se obtienen los estados Cumple, Cobertura Parcial, Sin Cobertura y Sin Requerimiento según stock y demanda.

### Definición del evento

**Evento producido**: Ninguno.

**Evento consumido**: Demanda proyectada del Plan 004 mediante RequerimientoAlimentoQueryPort.

La cobertura es solo lectura y nunca genera un movimiento.

### Definición del endpoint REST

| Elemento | Definición |
| --- | --- |
| Método y ruta | GET /api/inventario/alimentos/{alimentoId}/requerimiento |
| Autorización | ROLE_ADMINISTRADOR |
| Respuesta 200 | Alimento, etapa, stock actual, demanda exacta y estado de cobertura. |
| Estados | CUMPLE, COBERTURA_PARCIAL, SIN_COBERTURA, SIN_REQUERIMIENTO. |
| Errores | 404 por alimento no encontrado, 409 por clave/unidad incompatible y 503 por demanda no disponible. |

#### JSON de respuesta

```json
    {
      "alimentoId": "uuid",
      "alimento": "Alimento A",
      "tipoAlimento": "INICIO",
      "etapa": "INICIO",
      "stockActualKg": 1000,
      "demandaKg": 1250,
      "estado": "COBERTURA_PARCIAL"
    }
```

### Tests e implementación

- [ ] T024 [US3] Probar los cuatro estados de cobertura y cantidades exactas.
- [ ] T025 [US3] Probar diferencias de pesos por bulto y consolidación en kg.
- [ ] T026 [US3] Probar que medicamentos no tengan demanda nutricional.
- [ ] T027 [US3] Probar modal/consulta de detalle en solo lectura.
- [ ] T028 [US3] Implementar ConsultarCoberturaAlimentoUseCase y endpoint.

**Checkpoint**: Cobertura calculada sin reservar ni descontar existencias.

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verificar integración, vencimientos, FEFO y contratos con otros planes.

- [ ] T029 Documentar endpoints, roles, estados y errores en OpenAPI.
- [ ] T030 Completar InventarioIntegrationTest con recepción, consumo, vencimiento y consulta.
- [ ] T031 Verificar idempotencia de entradas confirmadas y movimientos consumidos.
- [ ] T032 Verificar scheduler de vencimientos y exclusión inmediata de saldos vencidos.
- [ ] T033 Verificar FEFO para consumos y salidas por recepción.
- [ ] T034 Verificar contrato de costo histórico para el Módulo 3.
- [ ] T035 Verificar que el Plan 002 no tenga repositorios ni tablas de movimiento.
- [ ] T036 Verificar que Plan 006 pueda descontar medicamentos usando InventarioMedicamentoPort.
- [ ] T037 Extender comprobaciones arquitectónicas y ejecutar pruebas disponibles.

**Checkpoint**: Inventario, recepción, consumo y cobertura integrados.

## Dependencies & Execution Order

### Phase Dependencies

- Setup precede a Foundation.
- Foundation requiere los contratos del Plan 002 y habilita las consultas.
- La entrada inicial depende de la recepción confirmada.
- El consumo de medicamentos depende de movimientos y recepciones disponibles.
- La cobertura depende de demanda del Plan 004.
- Polish depende de todos los contratos.

### Dependencias con otros planes

- **Plan 002**: Recepciones y auditoría; usa RegistrarEntradaInventarioPort.
- **Plan 004**: Demanda alimenticia proyectada.
- **Plan 006**: Consumo real de medicamentos y descuento de existencias.
- **Módulo 3**: Costos históricos por movimiento y recepción.
- **General.md**: Seguridad, actor, reloj, errores y eventos.

### Dentro de cada User Story

- Dominio y cálculo antes que aplicación.
- Puertos antes que adaptadores.
- Casos de uso antes que controladores.
- Consultas sin escrituras.
- Pruebas junto con implementación y checkpoint.

## Notes

- T001 a T037 identifican tareas; US1 a US3 identifican historias.
- Este plan no crea ni edita recepciones.
- El saldo siempre deriva de movimientos confirmados.
- Las existencias vencidas se excluyen aunque el scheduler no haya procesado todavía el movimiento.
- FEFO conserva la trazabilidad por recepción.
- Las versiones Flyway deben asignarse según el siguiente número disponible del repositorio.


