# Implementation Plan: Proyecciones de Alimento por Lote

**Date**: 25/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [022-ConsultarAlimentoRequeridoPorLote.md](../specs/022-ConsultarAlimentoRequeridoPorLote.md)

## Summary

Implementar el registro histórico de alimento proyectado por lote y etapa, la consulta que consume el Módulo 3 y el balance de demanda activa para planificación de abastecimiento. La activación y prórroga de etapas provienen del Plan 003; las lecturas de stock y costo consultan las capacidades propietarias correspondientes.

## Technical Context

**Performance Goals**: consulta por lote en máximo 1 segundo; balance consolidado en máximo 2 segundos; procesamiento de activación/ajuste en máximo 2 segundos.

**Constraints**: historial por `loteId`; autorización de administrador/nutricionista y credencial de sistema autorizada para integración; costo nulo explícito si no hay fuente; consultas sin reservas ni salidas.

**Scale/Scope**: SPEC-022, tres historias; tres endpoints REST; dos entidades propias (`ProyeccionAlimentoEtapa`, `HistorialAjusteProyeccion`); consumo de eventos internos de Plan 003.

**Datos propios**: `ProyeccionAlimentoEtapa` y `HistorialAjusteProyeccion`. Las proyecciones se asocian al `loteId`, conservando historia aunque el galpón aloje lotes posteriores.

**Dependencias**:

- Plan 001: identidad del lote, galpón y estado del ciclo; la población de inicio se conserva en el evento de activación.
- Plan 003: `EtapaNutricionalActivada` y `DuracionEtapaAjustada`, con alimento, cuota y calendario definidos.
- Plan 010: existencia vigente y agrupación de inventario para el balance de abastecimiento.
- Plan 002: recepciones y precios netos por kg para calcular el promedio ponderado del stock elegible.
- Módulo 3: consumidor de la consulta por lote; el cálculo contable final queda fuera de este plan.
- [General.md](General.md): eventos, idempotencia, persistencia, seguridad, errores, tiempo y pruebas.

### Decisiones específicas

1. Las proyecciones se crean idempotentemente al activar una etapa y quedan separadas por lote y etapa.
2. Kilogramos proyectados = población viva al inicio × cuota diaria en kg/ave/día × días efectivos. Los ajustes permitidos por Plan 003 recalculan kg y agregan historial inmutable; no cambian alimento, población base ni costo capturado.
3. El costo es referencial y se captura al activar la etapa. Si falta, se guarda `null` con advertencia; el completado manual único solo se admite mientras la etapa siga activa y se audita.
4. La consulta financiera entrega cada etapa por separado. El balance de compras agrupa solo productos y presentaciones compatibles y no reserva ni descuenta inventario.
5. El costo de referencia es el promedio ponderado por kg disponible de las recepciones confirmadas, no vencidas al activar la etapa y no anuladas: `suma(cantidadDisponibleKg × precioNetoKg) ÷ suma(cantidadDisponibleKg)`. Si no hay cantidad elegible, se conserva `null` con advertencia.

### Alcance documental

- Este plan implementa SPEC-022: consulta financiera por lote, creación/actualización automática por eventos de etapa y balance de abastecimiento.
- El [Plan 003](003-ConfiguracionYAsignacionDePlanesNutricionales.md) conserva la propiedad de las etapas, cuotas, alimento asignado y ajustes de calendario; el Plan 011 consume sus eventos.
- El [Plan 010](010-GestionDelInventario.md) es propietario de saldos y movimientos. Este plan consulta disponibilidad para calcular cobertura de demanda.
- El [Plan 002](002-GestionDeRecepciones.md) es propietario de recepciones y precios comerciales; este plan consulta sus datos para el promedio ponderado del stock elegible.
- El Módulo 3 consume la consulta discriminada por etapa y determina la liquidación contable real.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 022-ConsultarAlimentoRequeridoPorLote.md
└── plan/
    └── 011-ProyeccionesDeAlimentoPorLote.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/alimentacion/
│   │   ├── ProyeccionAlimentoEtapa.java
│   │   ├── HistorialAjusteProyeccion.java
│   │   ├── BalanceAbastecimientoAlimento.java
│   │   └── EstadoAbastecimiento.java
│   ├── event/alimentacion/
│   │   ├── ProyeccionAlimentoRegistrada.java
│   │   ├── ProyeccionAlimentoActualizadaPorProrroga.java
│   │   └── CostoUnitarioProyeccionCompletado.java
│   ├── exception/alimentacion/
│   └── port/out/alimentacion/
│       ├── ProyeccionAlimentoRepositoryPort.java
│       ├── LoteQueryPort.java
│       ├── StockBodegaQueryPort.java
│       └── RecepcionAlimentoQueryPort.java
├── application/alimentacion/
│   ├── RegistrarProyeccionEtapaUseCase.java
│   ├── ActualizarProyeccionPorProrrogaUseCase.java
│   ├── CompletarCostoReferenciaProyeccionUseCase.java
│   ├── ConsultarRequerimientoLoteFinanzasUseCase.java
│   ├── ConsultarBalanceAbastecimientoUseCase.java
│   └── result/
└── infrastructure/
    ├── adapter/in/rest/alimentacion/
    │   ├── ProyeccionAlimentoLoteController.java
    │   ├── BalanceAbastecimientoController.java
    │   ├── dto/
    │   └── mapper/ProyeccionAlimentoRestMapper.java
    ├── adapter/in/event/alimentacion/TransicionEtapaNutricionalEventListener.java
    ├── adapter/out/persistence/alimentacion/
    │   ├── entity/
    │   ├── repository/
    │   └── ProyeccionAlimentoPersistenceAdapter.java
    ├── adapter/out/internal/alimentacion/
    └── config/ProyeccionAlimentoBeanConfiguration.java
```

```text
src/test/java/com/avicontrol/
├── domain/model/alimentacion/
├── application/alimentacion/
├── infrastructure/adapter/in/rest/alimentacion/
├── infrastructure/adapter/in/event/alimentacion/
└── infrastructure/integration/ProyeccionesAlimentoIntegrationTest.java
```

**Structure Decision**: El paquete `alimentacion` administra el historial proyectado por lote. Las clases concretas y migraciones se ajustan a las capas de [General.md](General.md); no se accede directamente a datos propiedad de otros planes.

### Entidades y relación

```text
Lote (1) ───< (1..*) ProyeccionAlimentoEtapa (1) ───< (0..*) HistorialAjusteProyeccion

ProyeccionAlimentoEtapa
├── id, loteId, galponId, etapaId: UUID
├── nombreEtapa, tipoAlimento, alimentoId
├── poblacionInicioEtapa: Integer
├── cuotaKgAveDia: BigDecimal
├── diasBase, diasProrroga, diasEfectivos: Integer
├── proyeccionKg: BigDecimal
├── costoUnitarioKg: BigDecimal (nullable)
├── fuenteCosto, fechaCapturaCosto, advertenciaCosto (nullable)
├── costoCompletadoManualmente: boolean
├── estadoEtapa: ACTIVA | COMPLETADA
└── fechaRegistro, fechaActualizacion: Instant

HistorialAjusteProyeccion
├── id, proyeccionEtapaId: UUID
├── fechaAjuste: Instant, usuarioAjuste: UUID
├── motivoAjuste, justificacionAjuste
├── motivoAjuste: CUARENTENA_SANITARIA | BAJO_PESO
├── justificacionAjuste: String
└── diasBase/diasProrroga/diasEfectivos/proyeccionKg anteriores y nuevos
```

Una proyección es única por `(loteId, etapaId)` y se sella al completar la etapa. El historial es append-only. La invariante es `proyeccionKg = poblacionInicioEtapa × cuotaKgAveDia × (diasBase + diasProrroga)`, con precisión y redondeo definidos por SPEC-022.

La identidad del lote y del galpón es referencia externa; este plan no crea entidades de escritura ni claves foráneas físicas hacia tablas de otros módulos.

### Contratos de puertos y eventos

| Contrato | Tipo | Responsabilidad |
| --- | --- | --- |
| `ProyeccionAlimentoRepositoryPort` | Persistencia propia | Guardar, consultar y actualizar proyecciones por lote/etapa, anexando historial. |
| `EtapaNutricionalActivada` | Evento interno, Plan 003 | Iniciar la proyección con datos de etapa y población viva al corte; `eventoId` identifica la activación. |
| `DuracionEtapaAjustada` | Evento interno, Plan 003 | Recalcular kg y anexar el ajuste auditado; `ajusteId` identifica de forma estable el ajuste. |
| `LoteQueryPort` | Consulta interna, Plan 001 | Obtener identidad y estado del ciclo para la respuesta; la población de inicio proviene del evento capturado. |
| `StockBodegaQueryPort` | Consulta interna, Plan 010 | Obtener saldos disponibles por recepción para el promedio ponderado y existencias para el balance, sin reservarlas. |
| `RecepcionAlimentoQueryPort` | Consulta interna, Plan 002 | Obtener precio neto histórico por kg y fecha de vencimiento de cada recepción. |

Los eventos se consumen idempotentemente por `eventoId` y `ajusteId`; además, la proyección es única por `(loteId, etapaId)`. El evento de activación lleva la población viva capturada al inicio y el evento de ajuste identifica el registro de historial creado por Plan 003. En un monolito modular se usa Spring Modulith y la transacción local; no se introduce Kafka ni una transacción distribuida para esta comunicación interna.

### Contratos HTTP propuestos

| Método y ruta | Acceso | Responsabilidad |
| --- | --- | --- |
| `GET /api/lotes/{loteId}/alimento-requerido` | `ROLE_ADMINISTRADOR`, `ROLE_NUTRICIONISTA` y credencial de sistema autorizada | Consultar historial por lote, separado por etapa, para Finanzas. |
| `POST /api/proyecciones/{id}/completar-costo` | `ROLE_ADMINISTRADOR` | Completar una vez el costo nulo de una etapa activa, con fuente y justificación. |
| `GET /api/administracion/balance-abastecimiento` | `ROLE_ADMINISTRADOR`, `ROLE_NUTRICIONISTA` | Comparar demanda proyectada activa y stock disponible sin reservarlo. |

Los errores usan `application/problem+json` según [General.md](General.md). La forma detallada de respuestas y errores se especifica en SPEC-022 y OpenAPI.

### JSON común de errores

```json
{
  "type": "https://avicontrol/errors/proyeccion-completada-inmutable",
  "title": "Proyección completada inmutable",
  "status": 409,
  "detail": "La etapa ya fue completada y su proyección está sellada",
  "instance": "/api/proyecciones/{id}/completar-costo",
  "code": "PROYECCION_COMPLETADA_INMUTABLE",
  "correlationId": "uuid",
  "fieldErrors": []
}
```

Los errores conservan códigos estables de negocio y estructura RFC 9457 de [General.md](General.md). Una etapa sin costo de referencia se representa como dato `null` con advertencia, no como un error ni como cero.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verificar los contratos de eventos, los datos de corte y la consulta del costo promedio que se captura al activar proyecciones.

- [ ] T001 Verificar que Plan 003 publique `EtapaNutricionalActivada` y `DuracionEtapaAjustada` con los campos, momento de emisión e identidad idempotente documentados en ambos planes.
- [ ] T002 Verificar que `poblacionInicioEtapa` en `EtapaNutricionalActivada` sea la población viva capturada por Plan 001 al instante de activación.
- [ ] T003 Acordar con Plan 010 la agrupación por alimento/presentación y el contrato de disponibilidad vigente para el balance.
- [ ] T004 Implementar la consulta del promedio ponderado de costo según SPEC-022, conservando `null` y advertencia cuando no exista stock elegible.
- [ ] T005 Preparar fixtures de etapas, lotes, poblaciones, prórrogas, recepciones con precios heterogéneos y saldos.

**Checkpoint**: contratos de eventos y fuente del costo comprobados; no hay reglas de valoración implícitas.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Crear el agregado de proyección, su historial append-only y la persistencia necesaria.

- [ ] T006 Implementar `ProyeccionAlimentoEtapa`, `HistorialAjusteProyeccion` y estados en Java puro.
- [ ] T007 Implementar invariantes: unicidad `(loteId, etapaId)`, población no negativa, cuota/duración válidas, fórmula de proyección y sello de etapa completada.
- [ ] T008 Definir `ProyeccionAlimentoRepositoryPort`, consultas de lote/stock/costo y excepciones de negocio.
- [ ] T009 Crear migraciones Flyway para proyecciones e historial, índices por lote/estado y versión de concurrencia; no crear tablas de Planes 001/002/003/010.
- [ ] T010 Implementar persistencia, mappers y configuración bajo el límite de transacción indicado en General.md.
- [ ] T011 Probar precisión `BigDecimal`, idempotencia de `(loteId, etapaId)` y anexión de auditoría sin sobrescritura.

**Checkpoint**: dominio y persistencia protegen historia y unicidad antes de consumir eventos.

## Phase 3: User Story 2 — Registrar proyecciones al cambiar de etapa (Priority: P2)

**Spec**: SPEC-022, historia 2.

**Goal**: Capturar datos de la etapa al activarse, registrar cada extensión de duración y completar una única vez un costo faltante mientras la etapa esté activa.

**Independent Test**: Activar una etapa de 7 días con 10.000 aves y cuota 0,035 kg; verificar 2.450 kg. Al extenderla 2 días, verificar 3.150 kg y una entrada nueva de historial. Reentregar cada evento no crea proyecciones ni ajustes duplicados.

### Definición de eventos

- **Eventos consumidos de Plan 003**:
- `EtapaNutricionalActivada`: `eventoId`, `planGalponId`, `galponId`, `loteId`, `etapaId`, `nombreEtapa`, `diasBase`, `cuotaKgAveDia`, `alimentoId`, `pesoNominalPorBulto`, `poblacionInicioEtapa` y `occurredAt`. Se emite cuando una etapa entra en estado `ACTIVA`, incluida la primera etapa del plan.
- `DuracionEtapaAjustada`: `ajusteId`, `planGalponId`, `galponId`, `loteId`, `etapaId`, `diasProrrogaAcumulados`, `diasEfectivosNuevos`, `motivo`, `justificacion`, `usuarioResponsable` y `occurredAt`. `ajusteId` corresponde al registro de historial persistido por Plan 003.
- **Eventos producidos**: hechos internos `ProyeccionAlimentoRegistrada` y `ProyeccionAlimentoActualizadaPorProrroga` después de confirmar el agregado, solo si otra capacidad declara consumidor.

Plan 003 publica estos eventos después de confirmar la activación o el ajuste. La población se incluye como dato capturado al activar la etapa; no se reconstruye con una consulta posterior. Las reentregas se reconocen por `eventoId` o `ajusteId` y no duplican proyecciones ni entradas de historial.

### Definición del endpoint REST para completar costo

| Elemento | Definición |
| --- | --- |
| Método y ruta | `POST /api/proyecciones/{id}/completar-costo` |
| Autorización | `ROLE_ADMINISTRADOR` |
| Entrada | `costoUnitarioKg > 0`, fuente y justificación obligatoria. |
| Respuesta 200 | Proyección con costo, fuente, usuario/fecha de completado y bandera `costoCompletadoManualmente`. |
| Errores | 400 datos inválidos; 401/403 seguridad; 404 proyección inexistente; 409 costo ya completado o etapa inmutable. |

#### JSON de solicitud

```json
{
  "costoUnitarioKg": 1620.00,
  "fuenteCosto": "RECEPCION_REC-2026-015",
  "justificacion": "Costo de referencia confirmado para la fecha de activación"
}
```

### Tests para User Story 2

- [ ] T012 Probar proyección inicial con población inicial y transiciones posteriores con población viva al corte.
- [ ] T013 Probar fórmula de kg con y sin prórrogas, motivos permitidos y actualización de los valores anterior/nuevo del historial.
- [ ] T014 Probar evento repetido, fuera de orden, desconocido o con payload inválido; ninguna reentrega duplica efectos.
- [ ] T015 Probar que alimento, población base, cuota y costo no cambian por una prórroga.
- [ ] T016 Probar costo faltante como null más advertencia y completado único auditado; etapa completada rechaza la operación.
- [ ] T017 Probar carrera entre completar costo, ajustar etapa y completar etapa mediante control optimista.

### Implementación para User Story 2

- [ ] T018 Implementar `RegistrarProyeccionEtapaUseCase` y `ActualizarProyeccionPorProrrogaUseCase`.
- [ ] T019 Implementar listener de eventos internos de Plan 003 y registro idempotente de eventos procesados según mecanismo común.
- [ ] T020 Implementar `CompletarCostoReferenciaProyeccionUseCase` y su endpoint auditado.
- [ ] T021 Publicar eventos internos de proyección solo después del commit, si los consumidores quedan acordados.

**Checkpoint**: historial por etapa es completo, auditable e idempotente; el costo automático aplica el promedio ponderado acordado.

## Phase 4: User Story 1 — Consultar proyección de un lote (Priority: P1)

**Spec**: SPEC-022, historia 1.

**Goal**: Entregar al Módulo 3 la historia de proyecciones de un lote, discriminada por etapa, con el costo referencial y ajustes acumulados.

**Independent Test**: Consultar un lote finalizado con tres etapas y comprobar que cada una lleva población, cuota, días efectivos, kg y costo; no existe total global de kg entre etapas distintas.

### Definición del evento y endpoint REST

**Evento producido/consumido**: Ninguno para la consulta; la respuesta refleja proyecciones persistidas y no modifica estado.

| Elemento | Definición |
| --- | --- |
| Método y ruta | `GET /api/lotes/{loteId}/alimento-requerido` |
| Autorización | `ROLE_ADMINISTRADOR`, `ROLE_NUTRICIONISTA` o credencial de sistema autorizada para Finanzas, según mecanismo de General.md. |
| Entrada | UUID `loteId`; sin body. |
| Respuesta 200 | Identidad de lote/galpón, estado de ciclo, fecha de consulta y lista de etapas con kg, metadatos de costo e historial. Sin sumatoria heterogénea. |
| Errores | 400 UUID inválido; 401/403 seguridad; 404 lote inexistente; 503 consulta propietaria imprescindible no disponible. |

#### JSON de respuesta

```json
{
  "loteId": "2fc03a21-84c4-4c83-bb15-6607bb75cb90",
  "galponId": "550e8400-e29b-41d4-a716-446655440000",
  "estadoCiclo": "EN_PROGRESO",
  "fechaConsulta": "2026-10-25T15:00:00Z",
  "etapas": [
    {
      "etapaId": "b1c2d3e4-0002-4000-8000-000000000002",
      "nombreEtapa": "INICIO",
      "alimentoId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "poblacionInicioEtapa": 9750,
      "cuotaKgAveDia": 0.0450,
      "diasBase": 14,
      "diasProrroga": 4,
      "diasEfectivos": 18,
      "proyeccionKg": 7897.50,
      "costoUnitarioKg": null,
      "fuenteCosto": null,
      "fechaCapturaCosto": null,
      "advertenciaCosto": "Costo de referencia no disponible al activar la etapa",
      "estadoEtapa": "ACTIVA",
      "historialAjustes": []
    }
  ]
}
```

### Tests e implementación

- [ ] T022 Probar proyección activa y lote finalizado con orden estable por etapa y sin suma global.
- [ ] T023 Probar costo null/advertencia, metadatos de captura y costo inmutable ante recepciones posteriores.
- [ ] T024 Probar historial con uno o varios ajustes en orden cronológico y campos anteriores/nuevos.
- [ ] T025 Implementar `ConsultarRequerimientoLoteFinanzasUseCase`, DTOs y endpoint de consulta.

**Checkpoint**: Finanzas recibe un contrato completo, estable y separado por lote/etapa.

## Phase 5: User Story 3 — Consultar demanda para abastecimiento (Priority: P2)

**Spec**: SPEC-022, historia 3.

**Goal**: Comparar proyecciones de etapas activas por alimento y presentación compatible contra stock vigente, mostrando faltantes sugeridos sin reserva física.

**Independent Test**: Demanda de 11.942,50 kg y stock de 8.000 kg producen déficit 3.942,50 kg y 99 bultos de 40 kg, sin crear movimientos.

### Definición del evento y endpoint REST

**Evento producido/consumido**: Ninguno; balance de solo lectura calculado con proyecciones activas y consulta de stock de Plan 010.

| Elemento | Definición |
| --- | --- |
| Método y ruta | `GET /api/administracion/balance-abastecimiento` |
| Autorización | `ROLE_ADMINISTRADOR`, `ROLE_NUTRICIONISTA` |
| Entrada | Sin body; filtros de producto/paginación solo si son acordados en SPEC-022/OpenAPI. |
| Respuesta 200 | Fecha del balance, productos/presentaciones, demanda en kg/bultos, stock vigente, balance, déficit y lotes que contribuyen. |
| Errores | 401/403 seguridad; 409 unidad o clave de agrupación incompatible; 503 stock o proyecciones no disponibles. |

#### JSON de respuesta

```json
{
  "fechaBalance": "2026-10-25T15:00:00Z",
  "items": [
    {
      "alimentoId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "nombreComercial": "Iniciador Pollito 40kg",
      "tipoAlimento": "INICIO",
      "pesoNominalPorBulto": 40.00,
      "demandaConsolidadaKg": 11942.50,
      "demandaConsolidadaBultos": 298.56,
      "stockDisponibleKg": 8000.00,
      "balanceKg": -3942.50,
      "deficitCompraKg": 3942.50,
      "deficitCompraBultos": 99,
      "estadoAbastecimiento": "REABASTECIMIENTO_NECESARIO"
    }
  ]
}
```

### Tests e implementación

- [ ] T026 Probar agrupación por producto y peso nominal; no mezclar productos o presentaciones incompatibles.
- [ ] T027 Probar `balanceKg`, déficit en kg y conversión a bultos con redondeo techo.
- [ ] T028 Probar exclusión de etapas/lotes completados y stock vencido o anulado según Plan 010.
- [ ] T029 Probar consulta de solo lectura sin salidas ni reservas.
- [ ] T030 Implementar `ConsultarBalanceAbastecimientoUseCase`, DTOs y endpoint.

**Checkpoint**: demanda operativa comparable con existencias sin apropiarse del inventario.

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Cerrar contratos, integración y controles comunes.

- [ ] T031 Documentar los endpoints y esquemas de eventos internos en OpenAPI/documentación del módulo.
- [ ] T032 Ejecutar pruebas de integración con PostgreSQL, reentregas de eventos y listener de Plan 003.
- [ ] T033 Verificar reglas de arquitectura con ArchUnit y ausencia de escritura en las consultas.
- [ ] T034 Medir tiempos de consulta y procesamiento contra SPEC-022.
- [ ] T035 Probar promedio ponderado con recepciones a precios distintos, exclusión de recepciones no elegibles, redondeo y captura inmutable de costo/fuente.

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Fase 1)** verifica los contratos de eventos y los datos de stock y precio para el promedio ponderado.
- **Foundational (Fase 2)** bloquea las historias mediante modelo, persistencia e idempotencia.
- **US2 (Fase 3)** crea y mantiene proyecciones; habilita la consulta US1 y el balance US3.
- **US1 (Fase 4)** requiere proyecciones persistidas y consultas de ciclo de Plan 001.
- **US3 (Fase 5)** requiere proyecciones activas y disponibilidad del Plan 010.
- **Polish** depende de las tres historias.

### Dependencias con otros planes

- [Plan 001](001-ConsultaDeGalpones.md): identidad y estado vigente del lote y galpón; la población base se conserva en el evento de activación.
- [Plan 002](002-GestionDeRecepciones.md): identidad y precio de recepciones; fuente sujeta a política de valoración acordada.
- [Plan 003](003-ConfiguracionYAsignacionDePlanesNutricionales.md): propietario de etapas, alimento, cuota, fechas y eventos de ciclo de vida.
- [Plan 004](004-SeguimientoOperativoDeAlimentacion.md): registra suministros reales; no es fuente de proyección financiera.
- [Plan 010](010-GestionDelInventario.md): propietario de stock y movimientos; solo se consulta para balance.
- Módulo 3: consumidor de consulta por lote y propietario de liquidación real.

### Dentro de cada User Story

1. Pruebas de dominio y casos de uso antes de integrar endpoints o listeners.
2. Un listener solo traduce el evento y activa un caso de uso; no implementa reglas de proyección.
3. El historial se agrega dentro de la misma transacción local que actualiza la proyección.
4. Pruebas de integración verifican idempotencia, concurrencia y límites de propiedad de datos.

## Notes

- T001 a T035 identifican tareas; US1 a US3 corresponden a las historias de SPEC-022.
- Este plan describe componentes por implementar, no afirma que ya existan.
- El costo de referencia usa el promedio ponderado de saldos disponibles y precios netos por kg de recepciones elegibles, como define SPEC-022.
- Los ajustes de duración pertenecen al Plan 003; este plan consume el evento y actualiza su propio historial de proyección.
- Ninguna consulta de este plan crea salidas, reservas ni movimientos de bodega.
