# Implementation Plan: Requerimientos y Seguimiento de Alimentación

**Date**: 03/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [014-ConsultarAlimentoPorGalponPorDia.md](../specs/014-ConsultarAlimentoPorGalponPorDia.md)
- [022-ConsultarAlimentoRequeridoPorLote.md](../specs/022-ConsultarAlimentoRequeridoPorLote.md)

## Summary

Implementar la capacidad unificada de requerimientos y seguimiento de alimentación de AviControl Módulo 2. Este plan integra la dimensión operativa diaria de campo para operarios de granja ([SPEC-014](../specs/014-ConsultarAlimentoPorGalponPorDia.md)) con la dimensión táctica y financiera de proyecciones por lote e integración con el Módulo 3 (Finanzas) y balance logístico de compras ([SPEC-022](../specs/022-ConsultarAlimentoRequeridoPorLote.md)).

En el plano operativo diario (SPEC-014), el trabajador consulta la cuota del día para su galpón seleccionado, deduciendo la etapa a partir de la edad del lote y el plan nutricional activo ([Plan 003](003-ConfiguracionYAsignacionDePlanesNutricionales.md)), calculando el consumo necesario en kilogramos netos y bultos equivalentes, contrastando con el suministro acumulado y verificando las existencias en bodega central ([SPEC-023](../specs/023-ConsultarInventario.md)) con alertas de déficit y cambios de dieta. Asimismo, provee el panel y tarjeta métrica consolidada para el dashboard ("Inicio de trabajador").

En el plano estratégico y financiero (SPEC-022), el sistema gestiona las proyecciones formales de alimento requerido por lote (`ProyeccionAlimentoEtapa`), gobernadas por el principio de **inmutabilidad progresiva**: al activarse una etapa se capturan la población viva al corte, la ración diaria, el alimento comercial asignado (blindado por Plan 003) y el costo unitario de referencia histórico (`costoUnitarioKg`). La consulta para el Módulo 3 entrega los requerimientos estrictamente discriminados por etapa con su costo unitario sin mezclar insumos heterogéneos, permitiendo además al administrador balancear la demanda activa consolidada contra el stock de bodega central para determinar el déficit real de compras sin apartar físicamente bultos.

Todas las consultas operan en modo estrictamente de **solo lectura**, garantizando cero deducciones o reservas físicas en bodega central. La capacidad reacciona a eventos de activación y prórroga de etapas provenientes del Plan 003 para alimentar y ajustar de forma atómica e inmutable el historial de proyecciones.

## Technical Context

**Performance Goals**:
- El 95 % de las consultas de requerimiento diario por galpón y el dashboard consolidado del trabajador responden en máximo 1 segundo.
- La consulta de alimento requerido por lote para el Módulo 3 (Finanzas) responde en máximo 1 segundo.
- El balance consolidado de demanda activa y compras para administración responde en máximo 2 segundos.
- La creación o actualización automática de la proyección al cambiar o prorrogar una etapa se persiste en menos de 2 segundos desde el evento de confirmación.

**Constraints**:
- Roles autorizados: `ROLE_TRABAJADOR` y `ROLE_ADMINISTRADOR` para consultas operativas (SPEC-014); `ROLE_ADMINISTRADOR`, `ROLE_NUTRICIONISTA` y credenciales de sistema para integración con Módulo 3 y balances (SPEC-022).
- Ausencia de asignaciones trabajador-galpón en el modelo: el operario visualiza el universo de galpones disponibles para consulta o seleccionados en su sesión de trabajo.
- Inmutabilidad absoluta en etapas completadas (`COMPLETADA`): una vez concluida una etapa o finalizado el lote, sus días, kilogramos, costos e historial quedan bloqueados contra cualquier modificación.
- Blindaje de insumo y costo en etapa activa (`ACTIVA`): el alimento comercial, cuota diaria, población base y costo unitario capturado no pueden ser alterados. El único cambio admitido es el recálculo de kilogramos por ajuste de duración auditado en Plan 003.
- Tratamiento de costo no disponible: si al activar la etapa no existe costo de referencia en recepciones activas, se persiste `costoUnitarioKg = null` con advertencia descriptiva; está estrictamente prohibido registrar cero (`0.00`). Se admite completado único auditado por el Administrador mientras la etapa esté activa.
- Separación estricta por etapa: la respuesta al Módulo 3 jamás suma kilogramos globales de alimentos diferentes.
- Operación libre de movimientos de almacén: ninguna consulta o proyección genera reservas físicas, deducciones de stock o movimientos de inventario en bodega central.
- Manejo numérico con `BigDecimal` y redondeo `HALF_UP`: kilogramos a 2 decimales, montos monetarios a 2 decimales y raciones hasta 4 decimales.

**Scale/Scope**: Seis historias de usuario, siete endpoints REST, dos entidades persistidas propias (`ProyeccionAlimentoEtapa` e `HistorialAjusteProyeccion`), Value Objects de consumo y costo, puertos de consulta hacia Galpón/Lote, Plan Nutricional, Inventario y Recepciones, y listeners de eventos de Spring Modulith.

**Dependencias funcionales**:
- Galpones, lotes, fecha de ingreso, edad y población viva actual desde Módulo 1 (Plan 001).
- Plan nutricional activo, etapas, cuotas diarias, alimentos comerciales y eventos de prórroga desde Plan 003 ([003-ConfiguracionYAsignacionDePlanesNutricionales.md](003-ConfiguracionYAsignacionDePlanesNutricionales.md)).
- Stock disponible en bodega central desde Plan 002 ([SPEC-023](../specs/023-ConsultarInventario.md)).
- Precio neto histórico de compra de recepciones activas desde Plan 002 ([SPEC-001](../specs/001-RegistrarRecepcionDeAlimento.md)).
- Suministros físicos parciales del día desde la capacidad de despacho diario ([SPEC-014](../specs/014-ConsultarAlimentoPorGalponPorDia.md)).
- Consumo por parte del Módulo 3 (Finanzas) para presupuestación y liquidación del lote.

### Decisiones específicas

1. **Unificación de Capacidad de Requerimientos**: Aunque SPEC-014 atiende la operativa diaria del galpón y SPEC-022 atiende la proyección consolidada del lote, ambos comparten las fórmulas zootécnicas de consumo, el peso nominal por bulto, el cruce contra bodega central y la dependencia del plan nutricional. Se consolidan en el paquete `alimentacion` bajo la misma capacidad arquitectónica.
2. **El Lote como Eje Contable y el Galpón como Eje Operativo**:
   - Para el trabajador de campo (014), el acceso es por `galponId`, obteniendo el lote activo actualmente alojado.
   - Para Finanzas y liquidación (022), el acceso es estrictamente por `loteId`. Las proyecciones pertenecen al lote y conservan su historia independiente de si el galpón recibe lotes posteriores.
3. **Inmutabilidad Progresiva de Proyecciones**:
   - `ProyeccionAlimentoEtapa` se crea al transicionar o activar la etapa con la población viva exacta a ese corte.
   - Mientras está `ACTIVA`, el alimento comercial y el costo capturado son inmutables. Si Plan 003 emite `DuracionEtapaAjustada`, la proyección actualiza sus `diasProrroga`, `diasEfectivos` y `proyeccionKg`, anexando una entrada a `HistorialAjusteProyeccion`.
   - Al pasar a `COMPLETADA`, la proyección se sella de forma irreversible; cualquier intento de edición retorna HTTP 409 (`PROYECCION_COMPLETADA_INMUTABLE`).
4. **Captura y Naturaleza Referencial del Costo Unitario**:
   - `costoUnitarioKg` es una captura presupuestaria tomada en el instante de activación de la etapa (asociando `fuenteCosto` y `fechaCapturaCosto`). No representa el costo real consumido, el cual es calculado por Finanzas con base en despachos diarios y precios de compra de recepciones.
   - Compras posteriores con nuevos precios no alteran el costo capturado en proyecciones activas o pasadas.
5. **Tratamiento de Costo Nulo y Completado Único**:
   - Si no hay recepciones con precio para el alimento al activar la etapa, `costoUnitarioKg = null` y se genera `advertenciaCosto`. Nunca se coloca cero (`0.00`).
   - El Administrador puede invocar `POST /api/proyecciones/{id}/completar-costo` una única vez durante el estado `ACTIVA` para registrar el costo histórico de activación. Una vez completado, queda bloqueado.
   - Si la etapa se completa con costo nulo, se archiva de forma inmutable con su advertencia.
6. **Decisión de Negocio Pendiente (Valoración de Inventario)**: Cuando coexistan recepciones heterogéneas del mismo alimento con precios de compra dispares, la regla de selección del costo de referencia (Promedio Ponderado, FIFO/PEPS o Última Compra) se documenta como pendiente a concertar con Finanzas. El sistema consulta la recepción activa de referencia disponible sin asumir heurísticas ocultas.
7. **Ausencia de Asignación Rígida de Trabajadores**: Conforme a General.md y Plan 001, no existen entidades de asignación fija operario-galpón. El resumen consolidado de inicio de trabajador (SPEC-014 US-3) opera sobre los galpones activos de la granja que el usuario seleccione o tenga habilitados para consulta en su contexto operativo.
8. **Consumo Diario y Saldo Pendiente de la Jornada**:
   - `consumoTotalKg = (poblacionActual * racionGrAveDia) / 1000`.
   - `consumoTotalBultos = consumoTotalKg / pesoNominalPorBulto`.
   - `saldoPendienteKg = max(0, consumoTotalKg - suministroAcumuladoDia)`.
   - Si la población es 0 o el galpón no tiene lote activo, el consumo y saldo son `0.00`.
9. **Exclusión de Existencias Vencidas o Incompatibles**: Al cruzar contra bodega central, se consultan existencias netas de recepciones vigentes (no vencidas y no anuladas) que pertenezcan estrictamente al producto comercial o `TipoAlimento` asignado a la etapa.
10. **Balance de Abastecimiento sin Reserva Física**: La consulta para administración suma la demanda proyectada de las etapas en curso de todos los lotes activos y la compara con el stock disponible de bodega central (`balanceKg = stockDisponibleKg - demandaConsolidadaKg`). No bloquea ni descuenta inventario; genera el indicador `REABASTECIMIENTO_NECESARIO` y bultos sugeridos a comprar si el balance es negativo.
11. **Manejo de Errores y Estados**: Galpón o lote inexistente responde 404; parámetros inválidos 400; conflicto de inmutabilidad 409; fallos de servicios imprescindibles 503. Todo bajo `application/problem+json` (RFC 9457).
12. **Consistencia Transaccional y Eventos**: Las proyecciones se persisten en base de datos PostgreSQL propia. El listener de eventos de activación (`EtapaNutricionalActivada`) ejecuta la creación de la proyección en la misma transacción o de forma desacoplada con idempotencia sobre el par `(loteId, nombreEtapa)`.

### Alcance documental

- **SPEC-014**: Define la consulta diaria por galpón, cálculo de raciones, conversión a bultos, saldo pendiente, alertas operativas y pantalla consolidada de inicio del operario.
- **SPEC-022**: Define el servicio de proyecciones para Finanzas, segregación por etapa, inmutabilidad progresiva, captura de costo unitario, historial acumulativo de ajustes y balance de compras.
- **Plan 003**: Es propietario del catálogo de planes nutricionales, la asignación a galpones, el bloqueo de alimento y los ajustes de duración en cascada. Plan 004 es consumidor de esos datos y escucha sus eventos.
- **Plan 002 (SPEC-001 / SPEC-023)**: Provee los precios de recepciones y las existencias físicas disponibles en bodega central. Plan 004 no crea tablas de inventario ni de recepciones.
- **Módulo 3 (Finanzas)**: Realiza la liquidación económica y contable final; Plan 004 provee la API de requerimientos estructurada sin calcular impuestos ni balances contables de la empresa.

---

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   ├── 014-ConsultarAlimentoPorGalponPorDia.md
│   └── 022-ConsultarAlimentoRequeridoPorLote.md
└── plan/
    └── 004-RequerimientosYSeguimientoDeAlimentacion.md    # Este archivo
```

### Source Code (repository root)

Estructura de clases de la capacidad de requerimientos y seguimiento de alimentación:

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/alimentacion/
│   │   ├── ProyeccionAlimentoEtapa.java
│   │   ├── HistorialAjusteProyeccion.java
│   │   ├── RequerimientoDiarioGalpon.java
│   │   ├── ResumenAlimentoDiarioTrabajador.java
│   │   ├── ItemResumenGalponAlimento.java
│   │   ├── BalanceAbastecimientoAlimento.java
│   │   ├── ConsolidadoRequerimientoLote.java
│   │   ├── EtapaRequerimientoLote.java
│   │   ├── ConsumoDiario.java
│   │   ├── CostoUnitarioCapturado.java
│   │   ├── EstadoAbastecimiento.java
│   │   ├── EstadoDisponibilidadBodega.java
│   │   └── AlertaAlimentacion.java
│   ├── event/alimentacion/
│   │   ├── ProyeccionAlimentoRegistrada.java
│   │   ├── ProyeccionAlimentoActualizadaPorProrroga.java
│   │   └── CostoUnitarioProyeccionCompletado.java
│   ├── exception/alimentacion/
│   │   ├── ProyeccionNoEncontradaException.java
│   │   ├── ProyeccionCompletadaInmutableException.java
│   │   ├── CostoYaCompletadoException.java
│   │   ├── EtapaSinPlanNutricionalException.java
│   │   ├── OperacionNoPermitidaEnEtapaInactivaException.java
│   │   └── GalponSinLoteActivoException.java
│   └── port/out/alimentacion/
│       ├── ProyeccionAlimentoRepositoryPort.java
│       ├── GalponQueryPort.java
│       ├── LoteQueryPort.java
│       ├── PlanNutricionalQueryPort.java
│       ├── StockBodegaQueryPort.java
│       ├── CostoAlimentoReferenciaQueryPort.java
│       ├── SuministroDiarioQueryPort.java
│       └── AlimentacionEventPublisherPort.java
├── application/alimentacion/
│   ├── ConsultarAlimentoGalponDiaUseCase.java
│   ├── ConsultarResumenAlimentoTrabajadorUseCase.java
│   ├── ConsultarRequerimientoLoteFinanzasUseCase.java
│   ├── RegistrarProyeccionEtapaUseCase.java
│   ├── ActualizarProyeccionPorProrrogaUseCase.java
│   ├── CompletarCostoReferenciaProyeccionUseCase.java
│   ├── ConsultarBalanceAbastecimientoUseCase.java
│   └── result/
│       ├── RequerimientoDiarioGalponResult.java
│       ├── ResumenAlimentoTrabajadorResult.java
│       ├── ConsolidadoRequerimientoLoteResult.java
│       └── BalanceAbastecimientoResult.java
└── infrastructure/
    ├── adapter/in/rest/alimentacion/
    │   ├── AlimentacionDiariaController.java
    │   ├── ProyeccionAlimentoLoteController.java
    │   ├── BalanceAbastecimientoController.java
    │   ├── dto/
    │   │   ├── RequerimientoDiarioGalponResponse.java
    │   │   ├── ResumenAlimentoTrabajadorResponse.java
    │   │   ├── ConsolidadoRequerimientoLoteResponse.java
    │   │   ├── EtapaRequerimientoLoteResponse.java
    │   │   ├── BalanceAbastecimientoResponse.java
    │   │   ├── ItemBalanceAbastecimientoResponse.java
    │   │   ├── CompletarCostoReferenciaRequest.java
    │   │   └── HistorialAjusteProyeccionResponse.java
    │   └── mapper/AlimentacionRestMapper.java
    ├── adapter/in/event/alimentacion/
    │   └── TransicionEtapaNutricionalEventListener.java
    ├── adapter/out/persistence/alimentacion/
    │   ├── entity/
    │   │   ├── ProyeccionAlimentoEtapaEntity.java
    │   │   └── HistorialAjusteProyeccionEntity.java
    │   ├── repository/
    │   │   ├── SpringDataProyeccionAlimentoJpaRepository.java
    │   │   └── SpringDataHistorialAjusteProyeccionJpaRepository.java
    │   ├── mapper/AlimentacionPersistenceMapper.java
    │   └── AlimentacionPersistenceAdapter.java
    ├── adapter/out/internal/alimentacion/
    │   ├── SuministroDiarioQueryAdapter.java
    │   └── CostoAlimentoReferenciaQueryAdapter.java
    └── config/
        └── AlimentacionBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/alimentacion/
│   ├── ProyeccionAlimentoEtapaTest.java
│   ├── ConsumoDiarioTest.java
│   └── CostoUnitarioCapturadoTest.java
├── application/alimentacion/
│   ├── ConsultarAlimentoGalponDiaUseCaseTest.java
│   ├── ConsultarResumenAlimentoTrabajadorUseCaseTest.java
│   ├── ConsultarRequerimientoLoteFinanzasUseCaseTest.java
│   ├── RegistrarProyeccionEtapaUseCaseTest.java
│   ├── ActualizarProyeccionPorProrrogaUseCaseTest.java
│   ├── CompletarCostoReferenciaProyeccionUseCaseTest.java
│   └── ConsultarBalanceAbastecimientoUseCaseTest.java
└── infrastructure/
    ├── adapter/in/rest/alimentacion/
    │   ├── AlimentacionDiariaControllerTest.java
    │   ├── ProyeccionAlimentoLoteControllerTest.java
    │   └── BalanceAbastecimientoControllerTest.java
    ├── adapter/in/event/alimentacion/
    │   └── TransicionEtapaNutricionalEventListenerTest.java
    └── integration/
        └── RequerimientosAlimentacionIntegrationTest.java
```

**Structure Decision**: El paquete `alimentacion` encapsula tanto las lecturas operativas de campo como las proyecciones y balances persistidos. Se mantiene independiente de los repositorios de compras y galpones consumiendo puertos internos, y reacciona a los eventos de ciclo de vida del plan nutricional para garantizar que las proyecciones nunca se desincronicen.

---

### Entidades y relación

```text
ProyeccionAlimentoEtapa (1) ───< (0..*) HistorialAjusteProyeccion
  id: UUID                                id: UUID
  loteId: UUID                            proyeccionEtapaId: UUID
  galponId: UUID                          fechaAjuste: Instant
  nombreEtapa: String                     usuarioAjuste: UUID
  tipoAlimentoId: UUID                    motivoAjuste: MotivoAjusteEtapa
  tipoAlimentoNombre: String              justificacionAjuste: String
  alimentoId: UUID                        diasBaseAnterior: Integer
  alimentoNombreComercial: String         diasProrrogaAnterior: Integer
  pesoNominalPorBulto: BigDecimal         diasEfectivosAnterior: Integer
  poblacionInicioEtapa: Integer           proyeccionKgAnterior: BigDecimal
  cuotaKgAveDia: BigDecimal               diasBaseNuevo: Integer
  diasBase: Integer                       diasProrrogaNuevo: Integer
  diasProrroga: Integer                   diasEfectivosNuevo: Integer
  diasEfectivos: Integer                  proyeccionKgNuevo: BigDecimal
  proyeccionKg: BigDecimal
  costoUnitarioKg: BigDecimal (nullable)
  fuenteCosto: String (nullable)
  fechaCapturaCosto: Instant (nullable)
  advertenciaCosto: String (nullable)
  costoCompletadoManualmente: boolean
  estadoEtapa: EstadoEtapa
  fechaRegistro: Instant
  fechaActualizacion: Instant
```

#### Modelos de cálculo en tiempo de consulta (Value Objects y Results):

1. **`ConsumoDiario`**: Encapsula el cálculo operativo:
   - `consumoTotalKg = (poblacionActual * racionGrAveDia) / 1000`
   - `consumoTotalBultos = consumoTotalKg / pesoNominalPorBulto`
   - `saldoPendienteKg = max(0, consumoTotalKg - suministroAcumuladoDia)`
   - `saldoPendienteBultos = saldoPendienteKg / pesoNominalPorBulto`
2. **`BalanceAbastecimientoAlimento`**:
   - `demandaConsolidadaKg = sum(proyeccionKg de lotes en etapa activa)`
   - `demandaConsolidadaBultos = demandaConsolidadaKg / pesoNominalPorBulto`
   - `balanceKg = stockDisponibleBodegaKg - demandaConsolidadaKg`
   - `deficitCompraKg = abs(min(0, balanceKg))`
   - `deficitCompraBultos = ceil(deficitCompraKg / pesoNominalPorBulto)`
   - `estadoAbastecimiento = (balanceKg >= 0) ? SUFICIENTE : REABASTECIMIENTO_NECESARIO`
3. **`Invariante de Proyección`**:
   $$\text{proyeccionKg} = \text{poblacionInicioEtapa} \times \text{cuotaKgAveDia} \times \text{diasEfectivos}$$
   donde $\text{diasEfectivos} = \text{diasBase} + \text{diasProrroga}$.

---

### Contratos de los puertos

| Puerto | Tipo | Responsabilidad |
| --- | --- | --- |
| `ProyeccionAlimentoRepositoryPort` | Salida (Persistencia) | Guardar y buscar proyecciones por lote y etapa, listar proyecciones activas de todos los lotes y anexar auditorías al historial. |
| `GalponQueryPort` | Salida (Consulta interna) | Consultar identidad, nombre, aforo y estado del galpón. |
| `LoteQueryPort` | Salida (Consulta interna) | Consultar lote activo, código, población viva actual, fecha de ingreso y edad en días. |
| `PlanNutricionalQueryPort` | Salida (Consulta interna) | Consultar etapa actual, ración diaria, tipo de alimento, alimento comercial asignado, peso nominal del bulto y si el alimento está bloqueado. |
| `StockBodegaQueryPort` | Salida (Consulta interna) | Consultar existencias netas disponibles en bodega central (en kg y bultos) de alimentos compatibles no vencidos (SPEC-023). |
| `CostoAlimentoReferenciaQueryPort` | Salida (Consulta interna) | Consultar el precio de compra neto de referencia vigente de la recepción activa del alimento en bodega central (SPEC-001). |
| `SuministroDiarioQueryPort` | Salida (Consulta interna) | Consultar los kilogramos y bultos de alimento ya servidos físicamente en el galpón durante la jornada actual. |
| `AlimentacionEventPublisherPort` | Salida (Eventos) | Publicar eventos de dominio internos mediante Spring Modulith. |

---

### Contratos HTTP propuestos

| Endpoint | Método | Acceso | Descripción |
| --- | --- | --- | --- |
| `/api/galpones/{galponId}/alimento-diario` | `GET` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` | Consulta operativa del día para un galpón: cuota en kg y bultos, saldo pendiente, stock en bodega y alertas (SPEC-014 US-1 y US-2). |
| `/api/trabajador/resumen-alimento-diario` | `GET` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` | Resumen consolidado para dashboard de inicio: sumatoria total de kg y bultos requeridos hoy y tabla de galpones con disponibilidad de bodega (SPEC-014 US-3). |
| `/api/lotes/{loteId}/alimento-requerido` | `GET` | `ROLE_ADMINISTRADOR`, `ROLE_NUTRICIONISTA` | Consulta oficial de requerimientos de alimento para Módulo 3 (Finanzas): entregado discriminado por etapa con costo unitario de referencia (SPEC-022 US-1). |
| `/api/proyecciones/{id}/completar-costo` | `POST` | `ROLE_ADMINISTRADOR` | Registro único auditado de costo de referencia para una proyección activa que inició con costo `null` (SPEC-022 US-2). |
| `/api/administracion/balance-abastecimiento` | `GET` | `ROLE_ADMINISTRADOR`, `ROLE_NUTRICIONISTA` | Balance consolidado de demanda proyectada de lotes activos vs stock de bodega central para planificación de compras (SPEC-022 US-3). |

---

### JSON común de errores

Todos los endpoints retornan `Content-Type: application/problem+json` conforme a [General.md](General.md) y RFC 9457. Ejemplo para intento de completar costo en etapa completada:

```json
{
  "type": "https://avicontrol/errors/proyeccion-completada-inmutable",
  "title": "Proyección completada inmutable",
  "status": 409,
  "detail": "No se puede completar ni alterar el costo unitario de una proyección cuya etapa se encuentra en estado COMPLETADA",
  "instance": "/api/proyecciones/b8c19f2a-7140-4f51-b892-3a8190de4411/completar-costo",
  "code": "PROYECCION_COMPLETADA_INMUTABLE",
  "correlationId": "f1a2b3c4-9876-4321-bbbb-112233445566",
  "fieldErrors": []
}
```

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Preparar paquetes, fixtures y verificar compatibilidad de contratos de consulta con Módulo 1, Plan 002 y Plan 003.

- [ ] T001 Contrastar los modelos `RacionDiaria` de Plan 003 y `ExistenciaAlimento` de Plan 002 con los requerimientos de cálculo operativo y financiero.
- [ ] T002 Crear la estructura de paquetes para `domain/model/alimentacion`, `application/alimentacion` e `infrastructure/.../alimentacion`.
- [ ] T003 Configurar permisos de seguridad: lectura operativa de `/api/galpones/{galponId}/alimento-diario` para `ROLE_TRABAJADOR` y `ROLE_ADMINISTRADOR`; acceso gerencial y de Finanzas a proyecciones para `ROLE_ADMINISTRADOR` y `ROLE_NUTRICIONISTA`.
- [ ] T004 Preparar fixtures de prueba con lotes en diferentes etapas (Pre-inicio, Inicio, Engorde), galpones con suministros parciales y escenarios de bodega central con stock suficiente y deficitario.

**Checkpoint**: Estructura de paquetes, seguridad y fixtures preparada para implementar el dominio.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Implementar el modelo de dominio de proyecciones, el historial inmutable de ajustes, los Value Objects de consumo y costo, y la persistencia relacional.

**⚠️ CRITICAL**: Las historias de usuario de consulta y cálculo dependen de que estas entidades y puertos estén completamente operativos.

- [ ] T005 Implementar los Value Objects `ConsumoDiario` y `CostoUnitarioCapturado` en Java puro con validación estricta de precisión decimal y reglas de nulidad sin sustitución por cero.
- [ ] T006 Implementar la entidad `ProyeccionAlimentoEtapa` con su fórmula matemática de kilogramos requeridos, control de estado (`ACTIVA`, `COMPLETADA`) y reglas de inmutabilidad.
- [ ] T007 Implementar la entidad inmutable `HistorialAjusteProyeccion` para auditar prórrogas de duración y variaciones de kilogramos.
- [ ] T008 Definir las excepciones de dominio específicas: `ProyeccionNoEncontradaException`, `ProyeccionCompletadaInmutableException`, `CostoYaCompletadoException`, etc.
- [ ] T009 Definir los puertos de salida: `ProyeccionAlimentoRepositoryPort`, `GalponQueryPort`, `LoteQueryPort`, `PlanNutricionalQueryPort`, `StockBodegaQueryPort`, `CostoAlimentoReferenciaQueryPort` y `SuministroDiarioQueryPort`.
- [ ] T010 Crear migración Flyway `V8__crear_proyeccion_alimento.sql` (siguiente versión disponible tras la migración V7 del Plan 003) para tablas `proyeccion_alimento_etapa` e `historial_ajuste_proyeccion`, con índices por `lote_id`, `galpon_id` y restricción única sobre `(lote_id, nombre_etapa)`.
- [ ] T011 Implementar entidades JPA, repositorios Spring Data y `AlimentacionPersistenceAdapter` con mappers bidireccionales dominio-JPA.
- [ ] T012 Implementar `AlimentoCatalogoQueryAdapter`, `StockBodegaQueryAdapter` y `CostoAlimentoReferenciaQueryAdapter` resolviendo las llamadas a interfaces públicas de inventario y compras.
- [ ] T013 Registrar beans y transacciones en `AlimentacionBeanConfiguration`.

**Checkpoint**: Modelo de persistencia, puertos y reglas base de proyección probados y listos.

---

## Phase 3: User Story 1 — Consultar Requerimiento Diario de Alimento por Galpón (Priority: P1)

**Spec**: 014, historia 1.

**Goal**: El trabajador consulta en tiempo real qué alimento, ración, cantidad en kg y bultos, saldo pendiente y existencias en bodega corresponden a su galpón para la jornada.

**Independent Test**: Consultar un galpón con lote de 10.000 aves en día 18 de vida (etapa Crecimiento, ración 120g/ave = 1.200 kg = 30 bultos de 40kg) con suministro parcial de 400 kg (10 bultos); comprobar que muestra 800 kg (20 bultos) de saldo pendiente y las existencias exactas en bodega.

### Definición del evento para User Story 1

- **Evento producido**: Ninguno (operación estrictamente de lectura).
- **Evento consumido**: Ninguno.

### Definición del endpoint REST para User Story 1

| Elemento | Definición |
| --- | --- |
| Método y ruta | `GET /api/galpones/{galponId}/alimento-diario` |
| Autorización | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` |
| Entrada | `galponId` (UUID en ruta). |
| Respuesta 200 | `RequerimientoDiarioGalponResponse`: galpón (nombre, aforo, estado), lote (nombre, población viva, edad días), etapa actual, tipo y nombre de alimento, ración gr/ave, consumo requerido (kg y bultos), suministro del día (kg y bultos), saldo pendiente (kg y bultos), disponibilidad en bodega central (kg y bultos). |
| Errores | 400 por UUID inválido, 401 sin autenticación, 403 sin rol, 404 si el galpón no existe. |

#### JSON de respuesta (`GET /api/galpones/{galponId}/alimento-diario`)

```json
{
  "galponId": "550e8400-e29b-41d4-a716-446655440000",
  "galponNombre": "Galpón 1",
  "aforoMaximo": 10000,
  "galponEstado": "PRODUCTIVO",
  "lote": {
    "loteId": "2fc03a21-84c4-4c83-bb15-6607bb75cb90",
    "loteNombre": "Lote Broiler #2026-09",
    "poblacionActual": 10000,
    "edadDias": 18
  },
  "etapaActual": "CRECIMIENTO",
  "alimento": {
    "alimentoId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "nombreComercial": "Crecimiento Plus 40kg",
    "tipoAlimento": "Alimento Crecimiento",
    "pesoNominalPorBulto": 40.00
  },
  "racionGrAveDia": 120.00,
  "consumoRequerido": {
    "totalKg": 1200.00,
    "totalBultos": 30.00
  },
  "suministroHoy": {
    "suministradoKg": 400.00,
    "suministradoBultos": 10.00
  },
  "saldoPendienteHoy": {
    "pendienteKg": 800.00,
    "pendienteBultos": 20.00
  },
  "disponibilidadBodegaCentral": {
    "disponibleKg": 4000.00,
    "disponibleBultos": 100.00,
    "estado": "SUFICIENTE"
  },
  "alertas": []
}
```

### Tests para User Story 1

- [ ] T014 [US1] Probar en `ConsumoDiarioTest` el cálculo en kg, conversión a bultos exactos y cálculo de saldo pendiente.
- [ ] T015 [US1] Probar en `ConsultarAlimentoGalponDiaUseCaseTest` la deducción de etapa a partir de la edad del lote y plan nutricional.
- [ ] T016 [US1] Probar galpón sin lote activo o población 0: retorna consumo en 0.00 kg y 0.00 bultos sin emitir alertas erróneas de déficit.
- [ ] T017 [US1] Probar en `AlimentacionDiariaControllerTest` contrato HTTP 200, 404 y verificación de permisos para trabajador y administrador.

### Implementación de User Story 1

- [ ] T018 [US1] Implementar `ConsultarAlimentoGalponDiaUseCase` orquestando consultas a galpón, lote, plan nutricional, suministro del día y stock de bodega central.
- [ ] T019 [US1] Implementar DTOs y mappers en `AlimentacionRestMapper`.
- [ ] T020 [US1] Implementar endpoint en `AlimentacionDiariaController`.

**Checkpoint**: Consulta diaria operativa individual por galpón funcionando con cálculo de raciones y bultos.

---

## Phase 4: User Story 2 — Alertas Operativas de Alimentación, Desabastecimiento y Cambios (Priority: P2)

**Spec**: 014, historia 2.

**Goal**: Identificar y exhibir alertas visuales destacadas si el stock en bodega es insuficiente para el requerimiento del día, si existe saldo pendiente por suministrar o si hubo un cambio de dieta en la fecha.

**Independent Test**: Configurar galpón con requerimiento de 1.200 kg (30 bultos) y bodega central con solo 600 kg (15 bultos); verificar que la respuesta incorpore la alerta `DESABASTECIMIENTO_BODEGA` y la alerta `SUMINISTRO_PENDIENTE`.

### Definición del evento para User Story 2

- **Evento producido**: Ninguno (evaluación dinámica en la consulta).

### Definición del endpoint REST para User Story 2

Los campos de alertas se incorporan dinámicamente en la lista `alertas` de `RequerimientoDiarioGalponResponse`:

```json
"alertas": [
  {
    "tipo": "DESABASTECIMIENTO_BODEGA",
    "mensaje": "Stock en bodega central (600.00 kg) insuficiente para cubrir el requerimiento diario (1200.00 kg)",
    "severidad": "ALTA"
  },
  {
    "tipo": "SUMINISTRO_PENDIENTE",
    "mensaje": "Ración pendiente por suministrar en la jornada: 800.00 kg (20.00 bultos)",
    "severidad": "MEDIA"
  }
]
```

### Tests para User Story 2

- [ ] T021 [US2] Probar emisión de alerta `DESABASTECIMIENTO_BODEGA` cuando existencias netas < requerimiento total del día.
- [ ] T022 [US2] Probar alerta `CAMBIO_PLAN_NUTRICIONAL_HOY` cuando la fecha de activación de la etapa o cambio de alimento coincide con la fecha del sistema.
- [ ] T023 [US2] Probar alerta de saldo pendiente mientras `saldoPendienteKg > 0`.

### Implementación de User Story 2

- [ ] T024 [US2] Extender `ConsultarAlimentoGalponDiaUseCase` con evaluador de reglas de alerta operativa.
- [ ] T025 [US2] Mapear alertas en `AlimentacionRestMapper` y validar en pruebas de controlador.

**Checkpoint**: Alertas de déficit, saldo pendiente y cambios nutricionales operativas.

---

## Phase 5: User Story 3 — Resumen Consolidado de Alimento Diario en Inicio de Trabajador (Priority: P2)

**Spec**: 014, historia 3.

**Goal**: Presentar en el dashboard del trabajador la tarjeta métrica consolidada de alimento requerido hoy (kg y bultos) y la tabla detallada de galpones con disponibilidad en bodega central.

**Independent Test**: Con 3 galpones seleccionados con requerimientos de 450 kg (9 bultos), 430 kg (8.6 bultos) y 370 kg (7.4 bultos) y stock suficiente; verificar que la tarjeta presente `1.250 kg` y `25 Bultos (Bodega Central OK)` y la tabla liste las 3 filas con estado `Suficiente`.

### Definición del endpoint REST para User Story 3

| Elemento | Definición |
| --- | --- |
| Método y ruta | `GET /api/trabajador/resumen-alimento-diario` |
| Autorización | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` |
| Entrada | Parámetro opcional `galponIds` (lista de UUIDs). Si no se envía, consolida todos los galpones con lotes activos de la granja disponibles para consulta. |
| Respuesta 200 | `ResumenAlimentoTrabajadorResponse`: métrica global (totalKgRequeridos, totalBultosRequeridos, estadoGlobalBodega), listado de `ItemResumenGalponAlimento` (galponNombre, tipoAlimento, cuotaKg, cuotaBultos, estadoBodega). |

#### JSON de respuesta (`GET /api/trabajador/resumen-alimento-diario`)

```json
{
  "totalKgRequeridos": 1250.00,
  "totalBultosRequeridos": 25.00,
  "estadoGlobalBodega": "Bodega Central OK",
  "galpones": [
    {
      "galponId": "550e8400-e29b-41d4-a716-446655440001",
      "galponNombre": "Galpón 1",
      "tipoAlimento": "Engorde Stage 2",
      "cuotaKg": 450.00,
      "cuotaBultos": 9.00,
      "estadoBodega": "Suficiente"
    },
    {
      "galponId": "550e8400-e29b-41d4-a716-446655440003",
      "galponNombre": "Galpón 3",
      "tipoAlimento": "Engorde Stage 2",
      "cuotaKg": 430.00,
      "cuotaBultos": 8.60,
      "estadoBodega": "Suficiente"
    },
    {
      "galponId": "550e8400-e29b-41d4-a716-446655440005",
      "galponNombre": "Galpón 5",
      "tipoAlimento": "Engorde Finisher",
      "cuotaKg": 370.00,
      "cuotaBultos": 7.40,
      "estadoBodega": "Suficiente"
    }
  ]
}
```

### Tests para User Story 3

- [ ] T026 [US3] Probar en `ConsultarResumenAlimentoTrabajadorUseCaseTest` sumatorias matemáticas exactas en kg y bultos.
- [ ] T027 [US3] Probar que si al menos un alimento tiene déficit en bodega, el estado global reporte alerta de déficit (`Déficit en Bodega Central`).
- [ ] T028 [US3] Probar trabajador sin galpones activos: retorna 0 kg, 0 bultos y lista vacía.
- [ ] T029 [US3] Probar en `AlimentacionDiariaControllerTest` contrato del dashboard.

### Implementación de User Story 3

- [ ] T030 [US3] Implementar `ConsultarResumenAlimentoTrabajadorUseCase`.
- [ ] T031 [US3] Exponer endpoint `GET /api/trabajador/resumen-alimento-diario` en `AlimentacionDiariaController`.

**Checkpoint**: Dashboard consolidado del trabajador operando con sumatorias exactas y evaluación de suficiencia.

---

## Phase 6: User Story 4 — Consulta de Alimento Requerido por Lote para Finanzas (Priority: P1)

**Spec**: 022, historia 1.

**Goal**: Exponer la consulta oficial para Módulo 3 (Finanzas), entregando los requerimientos de alimento estrictamente separados por etapa, con el costo unitario por kilogramo de referencia capturado e historial de prórrogas, sin sumar kilogramos globales heterogéneos.

**Independent Test**: Consultar un lote finalizado con Pre-inicio (2.450 kg a $1.800/kg), Inicio (6.142,50 kg a $1.650/kg) y Engorde (14.112 kg a $1.500/kg); verificar que cada etapa se entregue individualizada con su costo y sin totalizar una sumatoria global de kilogramos.

### Definición del endpoint REST para User Story 4

| Elemento | Definición |
| --- | --- |
| Método y ruta | `GET /api/lotes/{loteId}/alimento-requerido` |
| Autorización | `ROLE_ADMINISTRADOR`, `ROLE_NUTRICIONISTA` |
| Entrada | `loteId` (UUID en ruta). |
| Respuesta 200 | `ConsolidadoRequerimientoLoteResponse`: loteId, galponId, estadoCiclo (`EN_PROGRESO`, `FINALIZADO`), fechaConsulta, lista `etapas` (nombreEtapa, tipoAlimento, proyeccionKg, costoUnitarioKg, fuenteCosto, fechaCapturaCosto, advertenciaCosto, diasBase, diasProrroga, diasEfectivos, estadoEtapa, poblacionInicioEtapa, historialAjustes). **Sin sumatoria agregada de kilogramos**. |
| Errores | 400 por UUID inválido, 401 sin autenticación, 403 sin rol, 404 si el lote no existe. |

#### JSON de respuesta (`GET /api/lotes/{loteId}/alimento-requerido`)

```json
{
  "loteId": "2fc03a21-84c4-4c83-bb15-6607bb75cb90",
  "galponId": "550e8400-e29b-41d4-a716-446655440000",
  "estadoCiclo": "EN_PROGRESO",
  "fechaConsulta": "2026-10-03T11:30:00Z",
  "etapas": [
    {
      "etapaId": "b1c2d3e4-0001-4000-8000-000000000001",
      "nombreEtapa": "PRE_INICIO",
      "tipoAlimento": "Pre-iniciador",
      "alimentoComercial": "Pre-iniciador Fuerte 40kg",
      "poblacionInicioEtapa": 10000,
      "cuotaKgAveDia": 0.0350,
      "diasBase": 7,
      "diasProrroga": 0,
      "diasEfectivos": 7,
      "proyeccionKg": 2450.00,
      "costoUnitarioKg": 1800.00,
      "fuenteCosto": "RECEPCION_REC-2026-001",
      "fechaCapturaCosto": "2026-09-15T08:00:00Z",
      "advertenciaCosto": null,
      "estadoEtapa": "COMPLETADA",
      "historialAjustes": []
    },
    {
      "etapaId": "b1c2d3e4-0002-4000-8000-000000000002",
      "nombreEtapa": "INICIO",
      "tipoAlimento": "Iniciador",
      "alimentoComercial": "Iniciador Pollito 40kg",
      "poblacionInicioEtapa": 9750,
      "cuotaKgAveDia": 0.0450,
      "diasBase": 14,
      "diasProrroga": 4,
      "diasEfectivos": 18,
      "proyeccionKg": 7897.50,
      "costoUnitarioKg": 1650.00,
      "fuenteCosto": "RECEPCION_REC-2026-015",
      "fechaCapturaCosto": "2026-09-22T08:00:00Z",
      "advertenciaCosto": null,
      "estadoEtapa": "ACTIVA",
      "historialAjustes": [
        {
          "fechaAjuste": "2026-10-01T14:20:00Z",
          "usuarioAjuste": "e8a9b0c1-2222-3333-4444-555566667777",
          "motivoAjuste": "BAJO_PESO",
          "justificacionAjuste": "Retraso en ganancia de peso según curva Ross 308",
          "diasEfectivosAnterior": 14,
          "proyeccionKgAnterior": 6142.50,
          "diasEfectivosNuevo": 18,
          "proyeccionKgNuevo": 7897.50
        }
      ]
    }
  ]
}
```

### Tests para User Story 4

- [ ] T032 [US4] Probar en `ConsultarRequerimientoLoteFinanzasUseCaseTest` que las etapas se entreguen estrictamente separadas sin sumatorias globales de kilogramos heterogéneos.
- [ ] T033 [US4] Probar que el costo unitario de referencia permanezca inmutable aunque existan compras posteriores a precios superiores.
- [ ] T034 [US4] Probar consulta con etapa donde `costoUnitarioKg = null`: retorna la advertencia `"Costo de referencia no disponible al momento de activación de la etapa"` sin asignar cero.
- [ ] T035 [US4] Probar en `ProyeccionAlimentoLoteControllerTest` contrato HTTP 200 y 404 para lotes inexistentes.

### Implementación de User Story 4

- [ ] T036 [US4] Implementar `ConsultarRequerimientoLoteFinanzasUseCase`.
- [ ] T037 [US4] Exponer endpoint `GET /api/lotes/{loteId}/alimento-requerido` en `ProyeccionAlimentoLoteController`.

**Checkpoint**: Servicio oficial para Finanzas entregando requerimientos separados por etapa con costos de referencia capturados.

---

## Phase 7: User Story 5 — Registro y Actualización Automática de Proyección de Etapa y Captura de Costo (Priority: P2)

**Spec**: 022, historia 2.

**Goal**: Registrar automáticamente la proyección al transicionar de etapa capturando el costo de referencia histórico, actualizarla auditadamente ante eventos de prórroga y permitir el completado único de costos pendientes por parte del Administrador.

**Independent Test**: Al disparar el evento `EtapaNutricionalActivada`, verificar que se cree la proyección con la población viva exacta al corte y el costo capturado; al disparar `DuracionEtapaAjustada`, verificar el recálculo de kg y adición a `HistorialAjusteProyeccion`; y permitir completar una sola vez un costo en `null` denegando segundos intentos.

### Definición del evento para User Story 5

- **Eventos consumidos**:
  - `EtapaNutricionalActivada(planGalponId, galponId, loteId, etapaCrianza, diaInicio, diaFin, racionKgPolloDia, alimentoId, occurredAt)`
  - `DuracionEtapaAjustada(planGalponId, etapaId, diasProrroga, diasEfectivosNuevos, motivo, justificacion, occurredAt)`
- **Eventos producidos**:
  - `ProyeccionAlimentoRegistrada(proyeccionId, loteId, nombreEtapa, proyeccionKg, costoUnitarioKg, occurredAt)`
  - `CostoUnitarioProyeccionCompletado(proyeccionId, loteId, costoUnitarioKg, usuarioId, occurredAt)`

### Definición del endpoint REST para User Story 5 (Completado Único)

| Elemento | Definición |
| --- | --- |
| Método y ruta | `POST /api/proyecciones/{id}/completar-costo` |
| Autorización | `ROLE_ADMINISTRADOR` |
| Entrada | `CompletarCostoReferenciaRequest`: `costoUnitarioKg` (positivo), `fuenteCosto`, `justificacion`. |
| Respuesta 200 | Proyección actualizada con el costo completado y bandera `costoCompletadoManualmente = true`. |
| Errores | 400 por valor inválido, 404 si la proyección no existe, **409 `COSTO_YA_COMPLETADO`** si ya tenía costo, **409 `PROYECCION_COMPLETADA_INMUTABLE`** si la etapa ya concluyó. |

### Tests para User Story 5

- [ ] T038 [US5] Probar en `TransicionEtapaNutricionalEventListenerTest` la creación automática de proyección al consumir `EtapaNutricionalActivada`.
- [ ] T039 [US5] Probar en `ActualizarProyeccionPorProrrogaUseCaseTest` la anexión acumulativa sin sobreescritura en `HistorialAjusteProyeccion`.
- [ ] T040 [US5] Probar en `CompletarCostoReferenciaProyeccionUseCaseTest` el completado único de costo y bloqueo irreversible ante un segundo intento.
- [ ] T041 [US5] Probar rechazo con 409 si la etapa ya se encuentra en estado `COMPLETADA`.

### Implementación de User Story 5

- [ ] T042 [US5] Implementar `RegistrarProyeccionEtapaUseCase` consultando la población viva al corte en `LoteQueryPort` y el costo vigente en `CostoAlimentoReferenciaQueryPort`.
- [ ] T043 [US5] Implementar `ActualizarProyeccionPorProrrogaUseCase`.
- [ ] T044 [US5] Implementar `CompletarCostoReferenciaProyeccionUseCase`.
- [ ] T045 [US5] Implementar `TransicionEtapaNutricionalEventListener` consumiendo los eventos de Spring Modulith de Plan 003.
- [ ] T046 [US5] Exponer endpoint `POST /api/proyecciones/{id}/completar-costo` en `ProyeccionAlimentoLoteController`.

**Checkpoint**: Sincronización automática reactiva de proyecciones y completado único auditado de costos pendientes.

---

## Phase 8: User Story 6 — Consulta Consolidada de Demanda Proyectada y Balance de Abastecimiento (Priority: P2)

**Spec**: 022, historia 3.

**Goal**: Exponer para el Administrador el balance consolidado de demanda proyectada de todas las etapas activas en curso vs existencias disponibles en bodega central, determinando el déficit de compra en kg y bultos sin reservar stock físico.

**Independent Test**: Con dos lotes requiriendo 11.942,50 kg (299 bultos de 40 kg) de Iniciador y un stock en bodega de 8.000 kg (200 bultos); verificar que reporte un déficit de 3.942,50 kg (99 bultos), estado `REABASTECIMIENTO_NECESARIO` y cero deducciones de inventario.

### Definición del endpoint REST para User Story 6

| Elemento | Definición |
| --- | --- |
| Método y ruta | `GET /api/administracion/balance-abastecimiento` |
| Autorización | `ROLE_ADMINISTRADOR`, `ROLE_NUTRICIONISTA` |
| Respuesta 200 | `BalanceAbastecimientoResponse`: fechaBalance, lista de `ItemBalanceAbastecimientoResponse` (alimentoId, nombreComercial, tipoAlimento, pesoNominalPorBulto, demandaConsolidadaKg, demandaConsolidadaBultos, stockDisponibleBodegaKg, stockDisponibleBodegaBultos, balanceKg, deficitCompraKg, deficitCompraBultos, estadoAbastecimiento, lotesAfectados). |

#### JSON de respuesta (`GET /api/administracion/balance-abastecimiento`)

```json
{
  "fechaBalance": "2026-10-03T11:30:00Z",
  "items": [
    {
      "alimentoId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "nombreComercial": "Iniciador Pollito 40kg",
      "tipoAlimento": "Iniciador",
      "pesoNominalPorBulto": 40.00,
      "demandaConsolidadaKg": 11942.50,
      "demandaConsolidadaBultos": 299.00,
      "stockDisponibleBodegaKg": 8000.00,
      "stockDisponibleBodegaBultos": 200.00,
      "balanceKg": -3942.50,
      "deficitCompraKg": 3942.50,
      "deficitCompraBultos": 99.00,
      "estadoAbastecimiento": "REABASTECIMIENTO_NECESARIO",
      "lotesAfectados": [
        { "loteId": "2fc03a21-84c4-4c83-bb15-6607bb75cb90", "galponNombre": "Galpón 1", "demandaKg": 6142.50 },
        { "loteId": "3ac03a21-84c4-4c83-bb15-6607bb75cb91", "galponNombre": "Galpón 2", "demandaKg": 5800.00 }
      ]
    }
  ]
}
```

### Tests para User Story 6

- [ ] T047 [US6] Probar en `ConsultarBalanceAbastecimientoUseCaseTest` la agrupación por producto comercial y cálculo de déficit exacto en bultos (redondeo techo).
- [ ] T048 [US6] Probar la exclusión de lotes finalizados o galpones en vacío sanitario.
- [ ] T049 [US6] Probar que la consulta de balance no genere movimientos de almacén ni reservas físicas de bultos.
- [ ] T050 [US6] Probar en `BalanceAbastecimientoControllerTest` contrato HTTP 200.

### Implementación de User Story 6

- [ ] T051 [US6] Implementar `ConsultarBalanceAbastecimientoUseCase`.
- [ ] T052 [US6] Implementar `BalanceAbastecimientoController` y sus mappers asociados.

**Checkpoint**: Herramienta de balance de compras y abastecimiento activo operativa.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Verificaciones transversales, documentación OpenAPI, pruebas de integración y consistencia arquitectónica.

- [ ] T053 Documentar los endpoints de requerimientos diarios, dashboard de operario, proyecciones para Finanzas y balance de abastecimiento en OpenAPI 3.
- [ ] T054 Implementar `RequerimientosAlimentacionIntegrationTest` con Testcontainers y PostgreSQL real: verificar el flujo completo desde activación de etapa por evento, consulta de cuota diaria, prórroga de etapa auditada y entrega para liquidación del Módulo 3.
- [ ] T055 Verificar reglas de arquitectura con ArchUnit: asegurar que `domain/model/alimentacion` no dependa de Spring, JPA ni Jackson.
- [ ] T056 Verificar cumplimiento de metas de rendimiento: respuestas inferiores a 1 segundo para consultas operativas y por lote, e inferiores a 2 segundos para balances consolidados.
- [ ] T057 Documentar la Decisión de Negocio Pendiente sobre la política de valoración de inventario (Promedio Ponderado vs FIFO/PEPS) en el glosario de acuerdos de integración con el Módulo 3.

**Checkpoint**: Capacidad de requerimientos y seguimiento de alimentación completamente integrada y verificada.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Fase 1)**: Base inicial, no posee dependencias.
- **Foundational (Fase 2)**: Depende de Setup. Bloquea todas las historias de usuario.
- **US1, US2, US3 (Fases 3, 4 y 5 - Operativa diaria)**: Dependen de Foundational y de la existencia de planes nutricionales activos en Plan 003. Pueden avanzar en paralelo.
- **US4, US5, US6 (Fases 6, 7 y 8 - Proyecciones y Finanzas)**: Dependen de Foundational y de los eventos emitidos por Plan 003 (`EtapaNutricionalActivada`, `DuracionEtapaAjustada`).
- **Polish (Fase 9)**: Depende de la finalización de todas las historias de usuario.

### Dependencias con otros planes

- **[Plan 001](001-ConsultaYSeguimientoDeGalpones.md)**: Provee identificación del galpón, lote activo, edad en días y población viva actual.
- **[Plan 002](002-GestionDeInventarioYRecepciones.md)**: Provee existencias disponibles en bodega central (SPEC-023) y costo de recepciones (SPEC-001).
- **[Plan 003](003-ConfiguracionYAsignacionDePlanesNutricionales.md)**: Emite los eventos de activación y prórroga de etapas y asegura el blindaje de alimentos comerciales.

### Dentro de cada User Story

1. Pruebas unitarias de dominio y casos de uso con mocks de puertos.
2. Implementación de casos de uso y mappers REST.
3. Controladores con seguridad Jakarta y validación de tipos.
4. Pruebas de integración MockMvc.
5. Checkpoint validado antes de proceder.

---

## Notes

- T001 a T057 identifican las tareas de implementación de este plan.
- Este plan concilia la operativa diaria en el galpón con el control presupuestario y financiero por lote, evitando la duplicación de cálculos zootécnicos.
- La segregación estricta de requerimientos por etapa en la respuesta para el Módulo 3 responde a la directriz fundamental de evitar sumatorias monetarias de insumos nutricionales heterogéneos.
- El principio de inmutabilidad progresiva protege la trazabilidad legal y contable de la granja avícola.
