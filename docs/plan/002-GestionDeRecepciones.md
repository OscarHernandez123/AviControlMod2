# Implementation Plan: Gestión de Recepciones

**Date**: 04/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [001-RegistrarRecepcionDeAlimento.md](../specs/001-RegistrarRecepcionDeAlimento.md)
- [002-RegistroRecepcionDeMedicamento.md](../specs/002-RegistroRecepcionDeMedicamento.md)

## Summary

Implementar el registro, edición e historial de recepciones de alimentos y medicamentos que ingresan a la bodega central. Cada recepción conserva el hecho comercial de una entrega, sus datos históricos y sus valores calculados.

Una recepción confirmada solicita al Plan 010 la creación del movimiento inicial de entrada mediante un puerto público. Este plan no calcula existencias consolidadas ni mantiene un segundo libro de movimientos. La recepción se considera confirmada únicamente cuando la recepción y la entrada correspondiente se confirman dentro de la transacción definida por la capacidad.

El código de lote puede repetirse: cada entrega crea una recepción independiente. Una recepción utilizada por movimientos posteriores no puede editarse. Las ediciones permitidas ajustan sus valores derivados, actualizan la entrada inicial mediante el puerto de inventario y registran auditoría.

## Technical Context

**Performance Goals**: El 95 % de los registros, ediciones, búsquedas y detalles responde en máximo 2 segundos.

**Constraints**: Solo el administrador registra, edita y consulta recepciones. Las fechas, cantidades, precios e impuestos deben validarse con BigDecimal, precisión y redondeo centralizados. Los catálogos de alimentos y medicamentos son externos a este plan.

**Scale/Scope**: Seis historias de usuario, seis endpoints REST principales, dos tipos de recepción, cálculos comerciales, historial, auditoría y publicación de eventos de recepción confirmada.

**Dependencias funcionales**: Catálogos de alimentos y medicamentos; BodegaCentral y movimientos del Plan 010; autenticación, actor, errores y eventos de General.md.

### Decisiones específicas

1. **Recepción frente a inventario**: RecepcionAlimento y RecepcionMedicamento conservan la entrega. El movimiento inicial y el saldo pertenecen al Plan 010.
2. **Identidad**: Cada recepción tiene UUID propio. El código de lote es un dato de búsqueda y puede repetirse.
3. **Entrada confirmada**: Al confirmar, el caso de uso llama RegistrarEntradaInventarioPort con el peso o contenido total. Si la entrada no puede confirmarse, no se conserva una recepción confirmada parcial.
4. **Edición**: Solo se edita una recepción cuyo movimiento inicial no tenga movimientos posteriores confirmados. El Plan 010 informa si está utilizada y ajusta la entrada inicial.
5. **Historial**: La recepción conserva nombres, tipos, presentación, fechas y precios históricos aunque cambien los catálogos.
6. **Auditoría**: Cada edición registra actor, fecha, valores anteriores y valores nuevos. La auditoría no elimina el valor anterior.
7. **Eventos**: Se publican RecepcionAlimentoConfirmada, RecepcionMedicamentoConfirmada y sus eventos de edición después de confirmar. Los consumidores deben ser idempotentes.
8. **Seguridad**: Todos los endpoints requieren ROLE_ADMINISTRADOR y el caso de uso valida nuevamente el actor.
9. **Persistencia**: Este plan crea tablas de recepción y auditoría. No crea tablas de saldo, existencia ni movimiento; esas tablas pertenecen al Plan 010.
10. **Unidades**: Alimento conserva bultos, peso por bulto y peso total en kg. Medicamento conserva cantidad, contenido por presentación y unidad de medida. No se convierten unidades incompatibles.

### Alcance documental

- El movimiento inicial requerido por los specs se implementa mediante RegistrarEntradaInventarioPort, propiedad del Plan 010.
- Las consultas de existencias, vencimientos, FEFO, resumen y cobertura pertenecen al Plan 010 y al spec 023.
- El precio neto por kilogramo o unidad se conserva como valor calculado de la recepción; el consumo posterior conserva su propio precio histórico por recepción.
- Los nombres definitivos de puertos y eventos se acuerdan con el Plan 010 antes de implementar.

## Project Structure

### Documentation (this feature)

    docs/
    ├── specs/
    │   ├── 001-RegistrarRecepcionDeAlimento.md
    │   └── 002-RegistroRecepcionDeMedicamento.md
    └── plan/
        └── 002-GestionDeRecepciones.md

### Source Code (repository root)

    src/main/java/com/avicontrol/
    ├── domain/
    │   ├── model/recepcion/
    │   │   ├── RecepcionAlimento.java
    │   │   ├── RecepcionMedicamento.java
    │   │   ├── EstadoRecepcion.java
    │   │   ├── TipoAlimento.java
    │   │   └── RegistroAuditoriaRecepcion.java
    │   ├── exception/recepcion/
    │   │   ├── RecepcionNoEncontradaException.java
    │   │   ├── RecepcionUtilizadaException.java
    │   │   ├── FechaVencimientoInvalidaException.java
    │   │   ├── CantidadInvalidaException.java
    │   │   └── ProductoNoDisponibleException.java
    │   └── port/out/recepcion/
    │       ├── RecepcionAlimentoRepositoryPort.java
    │       ├── RecepcionMedicamentoRepositoryPort.java
    │       ├── AuditoriaRecepcionRepositoryPort.java
    │       ├── AlimentoCatalogoQueryPort.java
    │       ├── MedicamentoCatalogoQueryPort.java
    │       ├── RegistrarEntradaInventarioPort.java
    │       ├── VerificarRecepcionUtilizadaPort.java
    │       └── ReceptionEventPublisherPort.java
    ├── application/recepcion/
    │   ├── RegistrarRecepcionAlimentoUseCase.java
    │   ├── EditarRecepcionAlimentoUseCase.java
    │   ├── ConsultarHistorialAlimentosUseCase.java
    │   ├── ConsultarDetalleRecepcionAlimentoUseCase.java
    │   ├── RegistrarRecepcionMedicamentoUseCase.java
    │   ├── EditarRecepcionMedicamentoUseCase.java
    │   ├── ConsultarHistorialMedicamentosUseCase.java
    │   └── ConsultarDetalleRecepcionMedicamentoUseCase.java
    └── infrastructure/
        ├── adapter/in/rest/recepcion/
        │   ├── RecepcionAlimentoController.java
        │   ├── RecepcionMedicamentoController.java
        │   └── mapper/RecepcionRestMapper.java
        ├── adapter/out/internal/recepcion/
        │   ├── RegistrarEntradaInventarioAdapter.java
        │   └── VerificarRecepcionUtilizadaAdapter.java
        ├── adapter/out/persistence/recepcion/
        │   ├── RecepcionPersistenceAdapter.java
        │   └── AuditoriaRecepcionPersistenceAdapter.java
        └── config/RecepcionBeanConfiguration.java

    src/main/resources/db/migration/
    ├── V__crear_recepciones.sql
    └── V__crear_auditoria_recepciones.sql

    src/test/java/com/avicontrol/
    ├── domain/recepcion/
    ├── application/recepcion/
    ├── infrastructure/adapter/in/rest/recepcion/
    └── integration/RecepcionesIntegrationTest.java

**Structure Decision**: Las entidades de recepción y auditoría pertenecen a este plan. El inventario se invoca mediante puertos públicos; ningún controlador ni repositorio de recepción accede directamente a tablas del Plan 010.

### Entidades y relaciones

    RecepcionAlimento
    ├── id: UUID
    ├── alimentoId: UUID
    ├── nombreAlimentoHistorico: String
    ├── tipoAlimentoHistorico: String
    ├── codigoLote: String
    ├── fechaIngreso: LocalDate
    ├── fechaVencimiento: LocalDate
    ├── cantidadBultos: BigDecimal
    ├── pesoPorBultoKg: BigDecimal
    ├── precioNetoPorBulto: BigDecimal
    ├── porcentajeImpuesto: BigDecimal
    ├── pesoTotalKg: BigDecimal
    ├── precioNetoPorKg: BigDecimal
    ├── subtotalNeto: BigDecimal
    ├── valorImpuesto: BigDecimal
    └── totalCompra: BigDecimal

    RecepcionMedicamento
    ├── id: UUID
    ├── medicamentoId: UUID
    ├── nombreMedicamentoHistorico: String
    ├── codigoLote: String
    ├── presentacion: String
    ├── cantidad: BigDecimal
    ├── contenidoPorPresentacion: BigDecimal
    ├── unidadMedida: String
    ├── fechaIngreso: LocalDate
    ├── fechaVencimiento: LocalDate
    ├── precioNetoPorPresentacion: BigDecimal
    ├── porcentajeImpuesto: BigDecimal
    ├── contenidoTotal: BigDecimal
    ├── subtotalNeto: BigDecimal
    ├── valorImpuesto: BigDecimal
    └── totalCompra: BigDecimal

    RegistroAuditoriaRecepcion
    ├── id: UUID
    ├── recepcionId: UUID
    ├── actorId: UUID
    ├── fechaHora: Instant
    ├── valoresAnteriores: JSON
    └── valoresNuevos: JSON

La recepción no contiene una colección de movimientos. Su entrada y uso posterior se consultan mediante los puertos del Plan 010.

### Contratos de los puertos

| Puerto | Responsabilidad |
| --- | --- |
| RecepcionAlimentoRepositoryPort | Crear, editar, buscar, filtrar y obtener detalles de recepciones de alimento. |
| RecepcionMedicamentoRepositoryPort | Crear, editar, buscar, filtrar y obtener detalles de recepciones de medicamento. |
| AuditoriaRecepcionRepositoryPort | Guardar los valores anteriores y nuevos de cada edición. |
| AlimentoCatalogoQueryPort | Confirmar alimento activo y obtener nombre/tipo vigentes. |
| MedicamentoCatalogoQueryPort | Confirmar medicamento activo y obtener nombre/presentación vigentes. |
| RegistrarEntradaInventarioPort | Crear o ajustar el movimiento inicial de una recepción en el Plan 010. |
| VerificarRecepcionUtilizadaPort | Indicar si existen movimientos posteriores que bloquean la edición. |
| ReceptionEventPublisherPort | Publicar eventos versionados después de la confirmación local. |

### Contratos HTTP propuestos

| Endpoint | Acceso | Resultado |
| --- | --- | --- |
| POST /api/recepciones/alimentos | Administrador | Registra una recepción y solicita su entrada inicial. |
| PUT /api/recepciones/alimentos/{recepcionId} | Administrador | Edita una recepción no utilizada. |
| GET /api/recepciones/alimentos | Administrador | Historial paginado y filtrado. |
| GET /api/recepciones/alimentos/{recepcionId} | Administrador | Detalle de recepción. |
| POST /api/recepciones/medicamentos | Administrador | Registra una recepción y solicita su entrada inicial. |
| PUT /api/recepciones/medicamentos/{recepcionId} | Administrador | Edita una recepción no utilizada. |
| GET /api/recepciones/medicamentos | Administrador | Historial paginado y filtrado. |
| GET /api/recepciones/medicamentos/{recepcionId} | Administrador | Detalle de recepción. |

Las consultas responden 200, los registros 201 y las ediciones 200. Los errores usan application/problem+json. Una recepción utilizada responde 409 y no cambia sus datos.

### JSON común de errores

```json
    {
      "type": "https://avicontrol/errors/recepcion-utilizada",
      "title": "Recepción utilizada",
      "status": 409,
      "detail": "La recepción tiene movimientos posteriores y no puede editarse",
      "instance": "/api/recepciones/alimentos/{recepcionId}",
      "code": "RECEPCION_UTILIZADA",
      "correlationId": "uuid",
      "fieldErrors": []
    }
```

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Preparar contratos y configuración de recepción.

- [ ] T001 Crear paquetes de dominio, aplicación y adaptadores de recepción.
- [ ] T002 Confirmar contratos públicos del Plan 010 para registrar y ajustar entradas.
- [ ] T003 Configurar precisión, escala y redondeo de cantidades, pesos, porcentajes y valores monetarios.
- [ ] T004 Confirmar catálogos activos y roles de administrador.
- [ ] T005 Preparar fixtures de alimentos, medicamentos, recepciones repetidas, vencimientos y recepciones utilizadas.

**Checkpoint**: Contratos de recepción, inventario y catálogos definidos.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Preparar entidades, validaciones, persistencia y puertos compartidos.

- [ ] T006 Implementar RecepcionAlimento, RecepcionMedicamento y sus cálculos derivados.
- [ ] T007 Implementar RegistroAuditoriaRecepcion y serialización de valores anteriores/nuevos.
- [ ] T008 Definir repositorios, catálogos y RegistrarEntradaInventarioPort.
- [ ] T009 Crear migraciones de recepciones y auditoría con la siguiente versión Flyway disponible.
- [ ] T010 Implementar validaciones de fechas, cantidades, precios, impuestos, desbordamiento y unidades.
- [ ] T011 Configurar transacciones y publicación idempotente de eventos.

**Checkpoint**: Las recepciones pueden validarse y persistirse sin duplicar el inventario.

## Phase 3: User Story 1 — Registrar Recepción de Alimento (Priority: P1)

**Spec**: 001, historia 1.

**Goal**: El administrador registra una entrega de alimento, calcula sus valores y crea su entrada en inventario.

**Independent Test**: Una recepción completa crea un registro independiente, calcula peso/precios/impuestos y deja confirmada la entrada.

### Definición del evento

**Evento producido**: RecepcionAlimentoConfirmadaV1.

**Evento consumido**: Confirmación de entrada del Plan 010, si el contrato es asíncrono.

El evento conserva alimento, tipo, lote, fechas, kilogramos totales, precio por kilogramo, subtotal, impuesto y total históricos.

### Definición del endpoint REST

| Elemento | Definición |
| --- | --- |
| Método y ruta | POST /api/recepciones/alimentos |
| Autorización | ROLE_ADMINISTRADOR |
| Respuesta 201 | Recepción confirmada y entrada inicial asociada. |
| Errores | 400 por datos inválidos, 404 por alimento no disponible, 409 por entrada no confirmable y 503 por dependencia no disponible. |

#### JSON de solicitud

```json
    {
      "alimentoId": "uuid",
      "tipoAlimento": "INICIO",
      "codigoLote": "LOTE-2026-01",
      "fechaIngreso": "2026-10-04",
      "fechaVencimiento": "2027-01-04",
      "cantidadBultos": 50,
      "pesoPorBultoKg": 40,
      "precioNetoPorBulto": 125000,
      "porcentajeImpuesto": 5
    }
```

#### JSON de respuesta

```json
    {
      "recepcionId": "uuid",
      "estado": "CONFIRMADA",
      "pesoTotalKg": 2000,
      "precioNetoPorKg": 3125,
      "subtotalNeto": 6250000,
      "valorImpuesto": 312500,
      "totalCompra": 6562500,
      "movimientoEntradaId": "uuid"
    }
```

### Tests e implementación

- [ ] T012 [US1] Probar validaciones, código de lote repetido y usuario no administrador.
- [ ] T013 [US1] Probar cálculos con precisión y redondeo.
- [ ] T014 [US1] Probar confirmación coordinada con RegistrarEntradaInventarioPort.
- [ ] T015 [US1] Implementar caso de uso, DTOs, mapper, evento y endpoint.

**Checkpoint**: Recepción y entrada inicial quedan confirmadas sin saldo calculado en este plan.

## Phase 4: User Story 2 — Editar Recepción de Alimento (Priority: P2)

**Spec**: 001, historia 2.

**Goal**: Corregir una recepción no utilizada sin modificar su identidad protegida ni su historial.

**Independent Test**: Una recepción con solo entrada inicial se edita; una utilizada responde 409.

### Definición del evento

**Evento producido**: RecepcionAlimentoActualizadaV1.

**Evento consumido**: Confirmación del ajuste de entrada del Plan 010.

### Definición del endpoint REST

| Elemento | Definición |
| --- | --- |
| Método y ruta | PUT /api/recepciones/alimentos/{recepcionId} |
| Autorización | ROLE_ADMINISTRADOR |
| Entrada | Alimento, vencimiento, cantidad, peso, precio e impuesto. |
| Errores | 400, 403, 404 y 409 si existen movimientos posteriores. |

#### JSON de solicitud

```json
    {
      "alimentoId": "uuid",
      "fechaVencimiento": "2027-02-01",
      "cantidadBultos": 52,
      "pesoPorBultoKg": 40,
      "precioNetoPorBulto": 125000,
      "porcentajeImpuesto": 5
    }
```

### Tests e implementación

- [ ] T016 [US2] Probar campos editables/protegidos y recepción utilizada.
- [ ] T017 [US2] Probar auditoría y ajuste atómico de entrada.
- [ ] T018 [US2] Implementar caso de uso, endpoint y evento.

**Checkpoint**: Las ediciones no destruyen historia ni alteran consumos posteriores.

## Phase 5: User Story 3 — Consultar Historial de Alimentos (Priority: P2)

**Spec**: 001, historia 3.

**Goal**: Consultar, filtrar y abrir el detalle de recepciones de alimento.

### Definición del evento

**Evento producido**: Ninguno.

**Evento consumido**: Ninguno.

### Definición de endpoints

| Endpoint | Acceso | Resultado |
| --- | --- | --- |
| GET /api/recepciones/alimentos | Administrador | Historial con paginación, búsqueda por recepción/lote, estado y periodo. |
| GET /api/recepciones/alimentos/{recepcionId} | Administrador | Detalle solo lectura. |

#### JSON de respuesta

```json
    {
      "content": [
        {
          "recepcionId": "uuid",
          "codigoLote": "LOTE-2026-01",
          "tipoAlimento": "INICIO",
          "cantidadBultos": 50,
          "pesoPorBultoKg": 40,
          "fechaIngreso": "2026-10-04",
          "estado": "CONFIRMADA"
        }
      ],
      "page": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1
    }
```

### Tests e implementación

- [ ] T019 [US3] Probar indicadores mensuales, búsqueda, filtros y estado vacío.
- [ ] T020 [US3] Probar detalle de solo lectura y acceso condicionado a edición.
- [ ] T021 [US3] Implementar casos de consulta y endpoints.

**Checkpoint**: El historial expone trazabilidad sin calcular existencias.

## Phase 6: User Story 4 — Registrar Recepción de Medicamento (Priority: P1)

**Spec**: 002, historia 1.

**Goal**: Registrar una entrega de medicamento con presentación, unidad, cálculos y entrada inicial.

### Definición del evento

**Evento producido**: RecepcionMedicamentoConfirmadaV1.

**Evento consumido**: Confirmación de entrada del Plan 010, si aplica.

### Definición del endpoint REST

| Elemento | Definición |
| --- | --- |
| Método y ruta | POST /api/recepciones/medicamentos |
| Autorización | ROLE_ADMINISTRADOR |
| Respuesta 201 | Recepción confirmada y movimiento inicial asociado. |
| Errores | 400, 404, 409 y 503 según la validación. |

#### JSON de solicitud

```json
    {
      "medicamentoId": "uuid",
      "codigoLote": "MED-2026-01",
      "presentacion": "FRASCO",
      "cantidad": 20,
      "contenidoPorPresentacion": 500,
      "unidadMedida": "MILILITRO",
      "precioNetoPorPresentacion": 35000,
      "porcentajeImpuesto": 19,
      "fechaIngreso": "2026-10-04",
      "fechaVencimiento": "2028-10-04"
    }
```

### Tests e implementación

- [ ] T022 [US4] Probar cantidades, contenidos, unidades, impuestos, fechas y medicamento activo.
- [ ] T023 [US4] Probar código de lote repetido como recepción independiente.
- [ ] T024 [US4] Implementar caso de uso, entrada, evento y endpoint.

**Checkpoint**: La recepción de medicamento queda histórica y disponible para inventario.

## Phase 7: User Story 5 — Editar Recepción de Medicamento (Priority: P2)

**Spec**: 002, historia 2.

**Goal**: Editar una recepción no utilizada y proteger diagnósticos/consumos posteriores.

### Definición del evento

**Evento producido**: RecepcionMedicamentoActualizadaV1.

**Evento consumido**: Confirmación del ajuste de entrada del Plan 010.

### Definición del endpoint REST

| Elemento | Definición |
| --- | --- |
| Método y ruta | PUT /api/recepciones/medicamentos/{recepcionId} |
| Autorización | ROLE_ADMINISTRADOR |
| Errores | 400, 403, 404 y 409 si existen movimientos posteriores. |

### Tests e implementación

- [ ] T025 [US5] Probar edición de campos permitidos, auditoría y bloqueo por uso.
- [ ] T026 [US5] Verificar que diagnósticos y consumos existentes conservan sus snapshots.
- [ ] T027 [US5] Implementar caso de uso, endpoint y evento.

**Checkpoint**: La edición solo afecta futuras recepciones utilizadas en diagnósticos.

## Phase 8: User Story 6 — Consultar Historial de Medicamentos (Priority: P2)

**Spec**: 002, historia 3.

**Goal**: Consultar y filtrar recepciones de medicamentos y abrir su detalle.

### Definición del evento

**Evento producido**: Ninguno.

**Evento consumido**: Ninguno.

### Definición de endpoints

| Endpoint | Acceso | Resultado |
| --- | --- | --- |
| GET /api/recepciones/medicamentos | Administrador | Historial filtrado por lote, medicamento, presentación y periodo. |
| GET /api/recepciones/medicamentos/{recepcionId} | Administrador | Detalle de solo lectura. |

### Tests e implementación

- [ ] T028 [US6] Probar indicador mensual, búsquedas, filtros, detalle y estado vacío.
- [ ] T029 [US6] Implementar consultas, DTOs y endpoints.

**Checkpoint**: El historial de medicamentos conserva trazabilidad sin calcular stock.

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Verificar integración entre recepción e inventario.

- [ ] T030 Documentar endpoints, roles, validaciones y errores en OpenAPI.
- [ ] T031 Completar RecepcionesIntegrationTest con alimento y medicamento.
- [ ] T032 Verificar que los eventos de recepción sean idempotentes.
- [ ] T033 Verificar que una recepción confirmada siempre tenga entrada inicial o quede rechazada completa.
- [ ] T034 Verificar que no existan tablas ni repositorios de movimientos en este plan.
- [ ] T035 Ejecutar pruebas y tareas de calidad disponibles.

**Checkpoint**: Recepciones, auditoría y contratos con el Plan 010 verificados.

## Dependencies & Execution Order

### Phase Dependencies

- Setup precede a Foundation.
- Foundation habilita las seis historias.
- Las historias de registro preceden a sus historias de edición e historial.
- El Plan 010 debe ofrecer RegistrarEntradaInventarioPort y VerificarRecepcionUtilizadaPort antes de cerrar las historias de escritura.
- Polish depende de los contratos publicados por ambos planes.

### Dependencias con otros planes

- **Plan 010**: Registra entradas, movimientos, saldos y uso posterior de recepciones.
- **Plan 005**: Puede consultar medicamentos y enfermedades para tratamientos futuros.
- **Plan 006**: Consulta recepciones de medicamentos para aplicar consumos.
- **General.md**: Seguridad, actor, reloj, errores y eventos.

### Dentro de cada User Story

- Dominio antes que aplicación.
- Puertos antes que adaptadores.
- Casos de uso antes que controladores.
- Auditoría y eventos dentro de la transacción.
- Pruebas junto con implementación y checkpoint.

## Notes

- T001 a T035 identifican tareas; US1 a US6 identifican historias.
- Este plan conserva recepciones; no calcula inventario consolidado.
- Los movimientos iniciales se crean mediante el contrato público del Plan 010.
- Código de lote repetido genera recepciones independientes.
- Una recepción utilizada no se edita.


