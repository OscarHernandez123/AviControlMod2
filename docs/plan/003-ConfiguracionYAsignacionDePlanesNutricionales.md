# Implementation Plan: Configuración y Asignación de Planes Nutricionales

**Date**: 03/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [021-AjustarPlanNutricionalPorEtapa.md](../specs/021-AjustarPlanNutricionalPorEtapa.md)

## Summary

Implementar la gestión centralizada de plantillas maestras de planes nutricionales en el catálogo global de la granja, la asignación automática y desacoplada de planes a galpones con lotes alojados, el tratamiento de excepciones de alojamiento (ingreso por edad avanzada o ausencia de plan predeterminado), la selección de alimentos comerciales compatibles consultando stock en tiempo real, el blindaje de inmutabilidad en etapas activas y completadas (protegiendo las proyecciones de requerimientos y costos de [SPEC-022](../specs/022-ConsultarAlimentoRequeridoPorLote.md)), y el ajuste manual auditado de días de etapa por contingencias zootécnicas con cierre matemático de cascada y sincronización de la fecha proyectada de salida o sacrificio del lote.

El dominio desacopla estrictamente la **Plantilla Maestra** (`PlanNutricionalPlantilla`), administrada por el especialista nutricionista como catálogo de curvas estándar, de la **Instancia del Galpón** (`PlanNutricionalGalpon`), la cual es una copia independiente asignada a un galpón y lote específicos. Cualquier modificación posterior en la plantilla maestra afecta exclusivamente a futuros lotes y jamás altera instancias activas ni proyecciones vigentes. A su vez, los ajustes particulares realizados en un galpón no contaminan la plantilla maestra ni a otros galpones.

La arquitectura sigue el patrón hexagonal dentro del monolito modular de AviControl Módulo 2. Este plan define y gestiona sus propias entidades de negocio en el subdominio de nutrición (`PlanNutricionalPlantilla`, `PlanNutricionalEtapaPlantilla`, `PlanNutricionalGalpon`, `CalendarioEtapaGalpon`, `HistorialAjusteEtapa`). La información de galpones y lotes proviene del Módulo 1 y se consume mediante puertos de consulta ya establecidos (`GalponQueryPort`, `LoteQueryPort`), mientras que la existencia de alimentos en bodega se consulta en tiempo real desde la capacidad de inventario ([SPEC-023](../specs/023-ConsultarInventario.md)). La configuración de un plan o la asignación de alimentos opera como una parametrización lógica y no realiza deducciones, reservas ni movimientos de inventario en bodega central.

## Technical Context

**Performance Goals**: El 95 % de las consultas de catálogo, asignaciones de plan, validaciones de compatibilidad y ajustes de duración en cascada responde en máximo 1 segundo bajo concurrencia normal de operación.

**Constraints**:
- Acceso exclusivo para usuarios con roles `ROLE_NUTRICIONISTA` y `ROLE_ADMINISTRADOR`.
- Garantizar a lo sumo un único plan predeterminado activo (`esPredeterminado = true`) por cada tipo de ave.
- Ración diaria por ave estrictamente positiva (`> 0.0000 kg/ave/día`), admitiendo hasta 4 decimales.
- Continuidad temporal estricta de etapas sin solapamientos ni huecos (`DiaInicio_i = DiaFin_{i-1} + 1`).
- Blindaje absoluto en etapa activa: alimento asignado, ración, población base y costo unitario capturado son inmutables durante el curso de la etapa (no se permite cambiar de alimento a mitad de etapa).
- La sustitución de plan en un lote en curso no puede evadir el bloqueo de la etapa activa y mantiene inalteradas las etapas completadas; el nuevo plan rige exclusivamente para etapas futuras no iniciadas.
- Todo ajuste manual de duración sobre una etapa activa exige justificación técnica obligatoria y usuario responsable, acumulando cada evento cronológicamente en el historial sin sobreescribir ajustes anteriores.
- Inmutabilidad absoluta para etapas en estado `COMPLETADA` o lotes finalizados.
- Ninguna operación de este plan genera movimientos de almacén ni reservas físicas de bultos en bodega central.

**Scale/Scope**: Cuatro historias de usuario, ocho endpoints REST, cinco entidades de dominio, un Value Object de ración y demanda, puertos de consulta hacia galpones, catálogo e inventario, eventos internos de Spring Modulith para asignación automática y notificación de cambios de fecha de salida.

**Dependencias funcionales**:
- Galpones y lotes vigentes gestionados por Módulo 1 (reutilizando `GalponQueryPort` y `LoteQueryPort` de [001-ConsultaYSeguimientoDeGalpones.md](001-ConsultaYSeguimientoDeGalpones.md)).
- Catálogo de alimentos e inventario físico en bodega central ([002-GestionDeInventarioYRecepciones.md](002-GestionDeInventarioYRecepciones.md) y [SPEC-023](../specs/023-ConsultarInventario.md)).
- Requerimientos operativos diarios y proyección contable para Finanzas ([004-RequerimientosYSeguimientoDeAlimentacion.md](004-RequerimientosYSeguimientoDeAlimentacion.md), [SPEC-014](../specs/014-ConsultarAlimentoPorGalponPorDia.md) y [SPEC-022](../specs/022-ConsultarAlimentoRequeridoPorLote.md)).
- Programación de salida / sacrificio del lote (Módulo 1 y Planes 008/009).

### Decisiones específicas

1. **Desacoplamiento Plantilla vs. Instancia**: `PlanNutricionalPlantilla` representa el modelo estándar global. Al ingresar un lote, se genera un clon profundo (`PlanNutricionalGalpon`) con su propio `CalendarioEtapaGalpon`. Las mutaciones en la instancia del galpón nunca tocan la plantilla, y las modificaciones en la plantilla maestra solo aplican a ingresos futuros.
2. **Unicidad de Predeterminado por Tipo de Ave**: La base de datos y el caso de uso aplican la regla de que solo puede existir un plan predeterminado por `TipoAve`. Al marcar un plan como predeterminado, cualquier otro plan activo del mismo tipo de ave es desmarcado automáticamente de forma atómica dentro de la misma transacción.
3. **Manejo de Excepciones en Alojamiento**:
   - Si no existe plan predeterminado para el tipo de ave, el lote se aloja exitosamente, el galpón adopta el estado `PENDIENTE_ASIGNACION`, se publica el evento `PlanNutricionalPendienteAsignacion` para alertar al nutricionista y la demanda se reporta en `0.0 kg`.
   - Si el lote ingresa con edad avanzada (ej. 14 días), las etapas previas cuya duración culmina antes se registran con estado `OMITIDA`, y se activa de inmediato la etapa cuyo intervalo cubra el día 14, iniciando el cálculo efectivo a partir de la edad real de ingreso.
4. **Blindaje de Etapa Activa (Integridad con SPEC-022)**: Durante una etapa activa, está estrictamente bloqueado modificar el alimento comercial asignado, la presentación del bulto o la ración diaria. Esto previene inconsistencias de insumo y protege la proyección contable de requerimientos y el costo unitario de referencia capturado para el Módulo 3 (Finanzas). Cualquier cambio de alimento debe planificarse exclusivamente para etapas posteriores no iniciadas (`PROGRAMADA`).
5. **Ajuste de Días y Cierre Matemático de Cascada**:
   - En una etapa activa, el único ajuste permitido sobre el cronograma es modificar su día final (`nuevoDiaFin >= edadActualLote`), justificado por `CUARENTENA_SANITARIA` o `BAJO_PESO`.
   - Al alterar la etapa activa $k$, las etapas posteriores $i > k$ conservan intactos sus días de duración efectiva configurados ($\text{diasEfectivos}_i$) y recalculan en cascada sus límites:
     $$\text{DiaInicio}_i = \text{DiaFin}_{i-1} + 1$$
     $$\text{DiaFin}_i = \text{DiaInicio}_i + \text{diasEfectivos}_i - 1$$
   - La última etapa del ciclo ("Finalización/Engorde") actualiza automáticamente la fecha proyectada de salida o sacrificio del lote, invocando `ActualizarFechaSalidaLotePort` y publicando `FechaProyectadaSalidaLoteActualizada`.
6. **Historial Acumulativo de Auditoría**: Cada ajuste manual genera un registro inmutable en `HistorialAjusteEtapa` que almacena marca de tiempo, usuario, motivo, justificación textual, duración previa y nueva duración. Múltiples prórrogas sucesivas se anexan cronológicamente sin sobreescribir registros anteriores.
7. **Sustitución No Destructiva**: Un plan puede sustituirse en un galpón con lote en producción, pero la operación respeta la inmutabilidad histórica: las etapas `COMPLETADA` permanecen intocadas, la etapa `ACTIVA` conserva su alimento, cuota y bloqueo, y el nuevo plan se acopla exclusivamente a partir de las etapas futuras no iniciadas.
8. **Compatibilidad y Consulta de Stock en Tiempo Real**: Al configurar una etapa, se consulta el catálogo de alimentos compatibles con el `TipoAlimento` de la etapa y el inventario disponible en bodega central mediante `AlimentoCatalogoQueryPort` y `StockBodegaQueryPort`. Si un producto tiene 0 kg disponibles, se etiqueta como "Sin stock", permitiendo su selección con advertencia para etapas futuras sin bloquear la planificación.
9. **Recálculo por Perfil Nutricional**: Si el alimento seleccionado posee `duracionRecomendadaEtapaDias` configurada en su ficha técnica, la interfaz prellena este valor y el caso de uso ajusta la duración de la etapa no iniciada a dicha recomendación al confirmar la selección, desplazando en cascada las etapas posteriores. Si es nulo, se preserva la duración base de la etapa.
10. **Ausencia de Movimientos de Bodega**: La confirmación o ajuste de un plan nutricional es una acción estrictamente de planeación zootécnica. No genera salidas, reservas físicas ni deducciones en el inventario de bodega central; las deducciones reales corresponden exclusivamente a los despachos diarios de alimentación ([SPEC-014](../specs/014-ConsultarAlimentoPorGalponPorDia.md)).
11. **Precisión Numérica**: Las raciones diarias y demandas se manejan con `BigDecimal` y redondeo `HALF_UP`. La ración por ave admite hasta 4 decimales (`0.0001 kg/ave/día`), y la demanda diaria en bultos se calcula y expone con decimales exactos junto con el total neto en kilogramos.
12. **Inmutabilidad Absoluta en Etapa Completada**: Una vez que una etapa pasa a estado `COMPLETADA` o el lote finaliza su ciclo, cualquier solicitud de ajuste o edición sobre dicha etapa es rechazada con código HTTP 409 (`ETAPA_COMPLETADA_INMUTABLE`).

### Alcance documental

- **SPEC-021** (este plan): Define plantillas maestras, asignación a galpones, excepciones de ingreso, selección de alimentos compatibles, bloqueo en etapa activa, ajuste de duración en cascada y actualización de la fecha proyectada de salida.
- **[Plan 004](004-RequerimientosYSeguimientoDeAlimentacion.md) (SPEC-014 y SPEC-022)**: Implementa la consulta diaria operativa de alimento por galpón y la proyección de requerimientos por lote para el Módulo 3 (Finanzas). Este Plan 003 garantiza la inmutabilidad del insumo, emite los eventos `EtapaNutricionalActivada` y `DuracionEtapaAjustada`, y provee las raciones y días efectivos que el Plan 004 consume.
- **[Plan 002](002-GestionDeInventarioYRecepciones.md) (SPEC-023)**: Provee la consulta de stock físico consolidado en bodega central. Plan 003 consume las existencias mediante `StockBodegaQueryPort` de forma pasiva (solo lectura).
- **Módulo 1**: Es propietario de los agregados `Galpon` y `Lote`. Plan 003 consulta sus datos vigentes y notifica desplazamientos de la fecha estimada de salida/sacrificio.

---

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 021-AjustarPlanNutricionalPorEtapa.md
└── plan/
    └── 003-ConfiguracionYAsignacionDePlanesNutricionales.md    # Este archivo
```

### Source Code (repository root)

Estructura de clases y paquetes nuevos para la capacidad de nutrición:

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/nutricion/
│   │   ├── PlanNutricionalPlantilla.java
│   │   ├── PlanNutricionalEtapaPlantilla.java
│   │   ├── PlanNutricionalGalpon.java
│   │   ├── CalendarioEtapaGalpon.java
│   │   ├── HistorialAjusteEtapa.java
│   │   ├── TipoAve.java
│   │   ├── EtapaCrianza.java
│   │   ├── EstadoPlanGalpon.java
│   │   ├── EstadoEtapa.java
│   │   ├── MotivoAjusteEtapa.java
│   │   ├── RacionDiaria.java
│   │   ├── AlimentoCompatibleInfo.java
│   │   └── DemandaNutricionalEstimada.java
│   ├── event/nutricion/
│   │   ├── PlanNutricionalAsignado.java
│   │   ├── PlanNutricionalPendienteAsignacion.java
│   │   ├── PlanNutricionalSustituido.java
│   │   ├── EtapaNutricionalActivada.java
│   │   ├── DuracionEtapaAjustada.java
│   │   └── FechaProyectadaSalidaLoteActualizada.java
│   ├── exception/nutricion/
│   │   ├── PlanNutricionalNoEncontradoException.java
│   │   ├── EtapaNutricionalNoEncontradaException.java
│   │   ├── RacionInvalidaException.java
│   │   ├── EtapasDiscontinuasException.java
│   │   ├── EtapaActivaInmutableException.java
│   │   ├── EtapaCompletadaInmutableException.java
│   │   ├── TipoAlimentoIncompatibleException.java
│   │   ├── JustificacionAjusteRequeridaException.java
│   │   └── DuracionEtapaInvalidaException.java
│   └── port/out/nutricion/
│       ├── PlanNutricionalPlantillaRepositoryPort.java
│       ├── PlanNutricionalGalponRepositoryPort.java
│       ├── AlimentoCatalogoQueryPort.java
│       ├── StockBodegaQueryPort.java
│       ├── GalponQueryPort.java
│       ├── LoteQueryPort.java
│       ├── ActualizarFechaSalidaLotePort.java
│       └── NutricionEventPublisherPort.java
├── application/nutricion/
│   ├── CrearPlantillaPlanNutricionalUseCase.java
│   ├── ModificarPlantillaPlanNutricionalUseCase.java
│   ├── ConsultarPlantillasPlanNutricionalUseCase.java
│   ├── ConsultarDetallePlantillaUseCase.java
│   ├── AsignarPlanNutricionalLoteUseCase.java
│   ├── SustituirPlanNutricionalGalponUseCase.java
│   ├── ConsultarAlimentosCompatiblesEtapaUseCase.java
│   ├── ConfigurarAlimentoEtapaFuturaUseCase.java
│   ├── AjustarDuracionEtapaActivaUseCase.java
│   ├── ConsultarPlanNutricionalGalponUseCase.java
│   └── result/
│       ├── PlantillaPlanNutricionalResult.java
│       ├── PlanNutricionalGalponResult.java
│       ├── CalendarioEtapaResult.java
│       ├── AlimentoCompatibleResult.java
│       └── HistorialAjusteResult.java
└── infrastructure/
    ├── adapter/in/rest/nutricion/
    │   ├── PlanNutricionalPlantillaController.java
    │   ├── PlanNutricionalGalponController.java
    │   ├── AlimentoCompatibleController.java
    │   ├── dto/
    │   │   ├── CrearPlantillaRequest.java
    │   │   ├── ModificarPlantillaRequest.java
    │   │   ├── EtapaPlantillaRequest.java
    │   │   ├── PlantillaResponse.java
    │   │   ├── PlantillaDetalleResponse.java
    │   │   ├── AsignarPlanManualRequest.java
    │   │   ├── SustituirPlanRequest.java
    │   │   ├── ConfigurarAlimentoEtapaRequest.java
    │   │   ├── AjustarDuracionEtapaRequest.java
    │   │   ├── PlanNutricionalGalponResponse.java
    │   │   ├── CalendarioEtapaResponse.java
    │   │   ├── AlimentoCompatibleResponse.java
    │   │   └── HistorialAjusteResponse.java
    │   └── mapper/NutricionRestMapper.java
    ├── adapter/in/event/nutricion/
    │   └── LoteAlojadoEventListener.java
    ├── adapter/out/persistence/nutricion/
    │   ├── entity/
    │   │   ├── PlanNutricionalPlantillaEntity.java
    │   │   ├── PlanNutricionalEtapaPlantillaEntity.java
    │   │   ├── PlanNutricionalGalponEntity.java
    │   │   ├── CalendarioEtapaGalponEntity.java
    │   │   └── HistorialAjusteEtapaEntity.java
    │   ├── repository/
    │   │   ├── SpringDataPlanPlantillaJpaRepository.java
    │   │   ├── SpringDataPlanGalponJpaRepository.java
    │   │   └── SpringDataHistorialAjusteJpaRepository.java
    │   ├── mapper/NutricionPersistenceMapper.java
    │   └── NutricionPersistenceAdapter.java
    ├── adapter/out/internal/nutricion/
    │   ├── AlimentoCatalogoQueryAdapter.java
    │   ├── StockBodegaQueryAdapter.java
    │   └── ActualizarFechaSalidaLoteAdapter.java
    └── config/
        └── NutricionBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/nutricion/
│   ├── PlanNutricionalPlantillaTest.java
│   ├── PlanNutricionalGalponTest.java
│   ├── CalendarioEtapaGalponTest.java
│   └── RacionDiariaTest.java
├── application/nutricion/
│   ├── CrearPlantillaPlanNutricionalUseCaseTest.java
│   ├── AsignarPlanNutricionalLoteUseCaseTest.java
│   ├── SustituirPlanNutricionalGalponUseCaseTest.java
│   ├── ConfigurarAlimentoEtapaFuturaUseCaseTest.java
│   └── AjustarDuracionEtapaActivaUseCaseTest.java
└── infrastructure/
    ├── adapter/in/rest/nutricion/
    │   ├── PlanNutricionalPlantillaControllerTest.java
    │   ├── PlanNutricionalGalponControllerTest.java
    │   └── AlimentoCompatibleControllerTest.java
    ├── adapter/in/event/nutricion/
    │   └── LoteAlojadoEventListenerTest.java
    └── integration/
        └── PlanNutricionalIntegrationTest.java
```

**Structure Decision**: El subdominio de nutrición encapsula completamente el catálogo de plantillas y las instancias activas por galpón. Se conecta de forma reactiva con el Módulo 1 mediante el listener `LoteAlojadoEventListener` para la asignación inmediata, y consulta los catálogos e inventarios mediante adaptadores internos sin acoplarse a repositorios JPA ajenos.

---

### Entidades y relación

```text
PlanNutricionalPlantilla (1) ───< (1..*) PlanNutricionalEtapaPlantilla
  id: UUID                                id: UUID
  nombre: String                          etapaCrianza: EtapaCrianza
  tipoAve: TipoAve                        diaInicioBase: Integer
  esPredeterminado: boolean               diaFinBase: Integer
  activo: boolean                         duracionDiasBase: Integer
                                          racionKgPolloDia: BigDecimal
                                          tipoAlimentoId: UUID
                                          alimentoSugeridoId: UUID (opcional)

PlanNutricionalGalpon (1) ───< (1..*) CalendarioEtapaGalpon (1) ───< (0..*) HistorialAjusteEtapa
  id: UUID                              id: UUID                               id: UUID
  galponId: UUID                        etapaCrianza: EtapaCrianza             fechaAjuste: Instant
  loteId: UUID                          diaInicio: Integer                     usuarioId: UUID
  plantillaOrigenId: UUID               diaFin: Integer                        motivo: MotivoAjusteEtapa
  estado: EstadoPlanGalpon              diasBase: Integer                      justificacion: String
  fechaAsignacion: LocalDate            diasProrroga: Integer                  diasPrevios: Integer
  usuarioAsignador: UUID                diasEfectivos: Integer                 diasNuevos: Integer
                                        estadoEtapa: EstadoEtapa
                                        alimentoBloqueado: boolean
                                        racionKgPolloDia: BigDecimal
                                        tipoAlimentoId: UUID
                                        alimentoId: UUID
                                        pesoNetoKgPorBulto: BigDecimal
                                        fechaActivacion: LocalDate
```

#### Invariantes del modelo:

1. **Continuidad de Etapas**: Para cualquier lista de etapas ordenadas cronológicamente en plantilla o galpón:
   $$\text{diaInicio}_0 = 1 \quad (\text{en lote estándar}) \qquad \text{y} \qquad \text{diaInicio}_i = \text{diaFin}_{i-1} + 1 \quad \forall i > 0$$
   $$\text{duracionDias} = \text{diaFin}_i - \text{diaInicio}_i + 1$$
2. **Ración Estrictamente Positiva**: `racionKgPolloDia > 0.0000`. Escala máxima de 4 decimales.
3. **Predeterminado Único**: $\sum [esPredeterminado == true \land activo == true \land tipoAve == T] \le 1$.
4. **Blindaje de Etapa Activa**: Si `estadoEtapa == ACTIVA`:
   - `alimentoId`, `pesoNetoKgPorBulto`, `racionKgPolloDia` y `tipoAlimentoId` son **estrictamente de solo lectura**.
   - No se permite invocar `cambiarAlimento(...)`.
   - Modificación de duración permitida únicamente con `nuevoDiaFin >= edadActualLote`, `motivo != null` y `justificacion` no vacía.
5. **Inmutabilidad de Etapa Completada**: Si `estadoEtapa == COMPLETADA` u `OMITIDA`, cualquier operación de mutación genera `EtapaCompletadaInmutableException`.

---

### Contratos de los puertos

| Puerto | Tipo | Responsabilidad |
| --- | --- | --- |
| `PlanNutricionalPlantillaRepositoryPort` | Salida (Persistencia) | Crear, actualizar, buscar por ID, listar plantillas y desmarcar predeterminado previo por `TipoAve`. |
| `PlanNutricionalGalponRepositoryPort` | Salida (Persistencia) | Guardar y recuperar la instancia del plan del galpón, sus etapas del calendario y anexar registros al historial de ajustes. |
| `AlimentoCatalogoQueryPort` | Salida (Consulta interna) | Consultar alimentos comerciales activos compatibles con un `TipoAlimento`, obteniendo nombre comercial, marca, peso nominal por bulto y `duracionRecomendadaEtapaDias`. |
| `StockBodegaQueryPort` | Salida (Consulta interna) | Consultar existencias físicas disponibles en bodega central (en kg y bultos) según SPEC-023, excluyendo recepciones vencidas o anuladas. |
| `GalponQueryPort` | Salida (Consulta interna) | Consultar estado operativo, nombre y aforo de un galpón. |
| `LoteQueryPort` | Salida (Consulta interna) | Consultar lote alojado en un galpón, tipo de ave, fecha de ingreso, edad actual en días y población viva actual. |
| `ActualizarFechaSalidaLotePort` | Salida (Comando Módulo 1) | Notificar al Módulo 1 el desplazamiento en días de la fecha proyectada de salida/sacrificio del lote. |
| `NutricionEventPublisherPort` | Salida (Eventos) | Publicar eventos de dominio internos mediante Spring Modulith. |

---

### Contratos HTTP propuestos

| Endpoint | Método | Acceso | Descripción |
| --- | --- | --- | --- |
| `/api/nutricion/plantillas` | `POST` | `ROLE_NUTRICIONISTA` | Crea una nueva plantilla maestra con sus etapas contiguas en el catálogo global. |
| `/api/nutricion/plantillas/{id}` | `PUT` | `ROLE_NUTRICIONISTA` | Modifica una plantilla maestra existente (afecta solo a futuros lotes). |
| `/api/nutricion/plantillas` | `GET` | `ROLE_NUTRICIONISTA`, `ROLE_ADMINISTRADOR` | Lista todas las plantillas maestras del catálogo global (filtrables por `tipoAve` y `activo`). |
| `/api/nutricion/plantillas/{id}` | `GET` | `ROLE_NUTRICIONISTA`, `ROLE_ADMINISTRADOR` | Consulta el detalle completo de una plantilla maestra y sus etapas base. |
| `/api/galpones/{galponId}/plan-nutricional` | `GET` | `ROLE_NUTRICIONISTA`, `ROLE_ADMINISTRADOR` | Consulta el plan nutricional asignado al galpón, etapas, estado actual, alimento e historial. |
| `/api/galpones/{galponId}/plan-nutricional/sustituir` | `PUT` | `ROLE_NUTRICIONISTA` | Sustituye el plan del galpón preservando etapas concluidas y blindando la etapa activa. |
| `/api/nutricion/alimentos-compatibles` | `GET` | `ROLE_NUTRICIONISTA`, `ROLE_ADMINISTRADOR` | Consulta alimentos comerciales compatibles con la etapa y sus existencias en bodega central. |
| `/api/galpones/{galponId}/plan-nutricional/etapas/{etapaId}/alimento` | `PUT` | `ROLE_NUTRICIONISTA` | Asigna o cambia el alimento comercial para una etapa futura no iniciada. Rechaza etapa activa. |
| `/api/galpones/{galponId}/plan-nutricional/etapas/{etapaId}/ajuste-duracion` | `PUT` | `ROLE_NUTRICIONISTA` | Ajusta los días de una etapa activa con justificación obligatoria y recálculo en cascada. |

---

### JSON común de errores

Todos los errores retornan `Content-Type: application/problem+json` conforme a [General.md](General.md) y RFC 9457. Ejemplo para intento de modificación en etapa activa:

```json
{
  "type": "https://avicontrol/errors/etapa-activa-inmutable",
  "title": "Etapa activa inmutable",
  "status": 409,
  "detail": "El alimento comercial asignado a la etapa activa no puede modificarse para proteger la integridad contable y de proyección del lote",
  "instance": "/api/galpones/550e8400-e29b-41d4-a716-446655440000/plan-nutricional/etapas/3fa85f64-5717-4562-b3fc-2c963f66afa6/alimento",
  "code": "ETAPA_ACTIVA_INMUTABLE",
  "correlationId": "d7a8e210-91b3-4f51-b845-8120e2ef71a2",
  "fieldErrors": []
}
```

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Preparar los cimientos de configuración, permisos y contratos específicos del subdominio de nutrición sin reinstalar la base del proyecto.

- [ ] T001 Contrastar los modelos `TipoAve`, `EtapaCrianza` y los identificadores de catálogo de alimentos con el Módulo 1 y Plan 002.
- [ ] T002 Crear la estructura de paquetes para `domain/model/nutricion`, `application/nutricion` e `infrastructure/.../nutricion`.
- [ ] T003 Configurar las autoridades de seguridad específicas: asignar permisos de administración y ajuste de dietas a `ROLE_NUTRICIONISTA` y de lectura gerencial a `ROLE_ADMINISTRADOR`.
- [ ] T004 Crear fixtures de prueba con plantillas estándar (Broiler 45 días), alimentos con diferente perfil nutricional y existencias simuladas en bodega central.

**Checkpoint**: Infraestructura base de paquetes, seguridad y fixtures lista para soportar el dominio.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Modelar entidades de dominio puras, reglas matemáticas de cascada, puertos de persistencia y adaptadores internos.

**⚠️ CRITICAL**: Ninguna historia de usuario puede implementarse hasta culminar esta fase.

- [ ] T005 Implementar en Java puro los enums `TipoAve`, `EtapaCrianza`, `EstadoPlanGalpon`, `EstadoEtapa` y `MotivoAjusteEtapa`.
- [ ] T006 Implementar el Value Object `RacionDiaria` con validación de valor estrictamente positivo (`> 0.0000`), precisión de hasta 4 decimales y operaciones aritméticas de demanda.
- [ ] T007 Implementar las entidades `PlanNutricionalPlantilla` y `PlanNutricionalEtapaPlantilla` con validación estricta de contigüidad (`DiaInicio_i = DiaFin_{i-1} + 1`).
- [ ] T008 Implementar las entidades `PlanNutricionalGalpon`, `CalendarioEtapaGalpon` e `HistorialAjusteEtapa` con sus reglas de transición de estado, blindaje de etapa activa e inmutabilidad de etapa completada.
- [ ] T009 Implementar en `CalendarioEtapaGalpon` el método de cierre matemático de cascada: desplazar etapas posteriores preservando días efectivos y calcular la variación total de días de salida.
- [ ] T010 Definir excepciones de dominio específicas (`PlanNutricionalNoEncontradoException`, `EtapaActivaInmutableException`, `EtapasDiscontinuasException`, etc.).
- [ ] T011 Definir los puertos de salida `PlanNutricionalPlantillaRepositoryPort`, `PlanNutricionalGalponRepositoryPort`, `AlimentoCatalogoQueryPort`, `StockBodegaQueryPort`, `ActualizarFechaSalidaLotePort` y `NutricionEventPublisherPort`.
- [ ] T012 Crear migración Flyway `V7__crear_plan_nutricional.sql` (siguiente versión disponible tras las migraciones V3-V6 del Plan 002) con tablas para plantillas, etapas de plantilla, planes de galpón, calendarios de etapa e historial de ajustes, incluyendo restricción única parcial para `esPredeterminado = true` por `tipo_ave`.
- [ ] T013 Implementar entidades JPA, repositorios Spring Data y `NutricionPersistenceAdapter` con mappers bidireccionales dominio-JPA.

**Checkpoint**: Entidades de dominio, invariantes, persistencia relacional y puertos listos y verificados con pruebas unitarias de dominio.

---

## Phase 3: User Story 1 — Creación y Configuración de Plantillas Maestras de Plan Nutricional (Priority: P1)

**Spec**: 021, historia 1.

**Goal**: El nutricionista crea y gestiona plantillas maestras en el catálogo global, con etapas contiguas, raciones válidas y designación única de plan predeterminado por tipo de ave, sin afectar lotes en producción.

**Independent Test**: Crear una plantilla "Plan Estándar Broiler 45 días" con 3 etapas contiguas y marcarla como predeterminada; verificar que desmarque cualquier otra plantilla predeterminada de Broiler, rechace etapas con raciones <= 0 o solapadas, y que editarla posteriormente no altere los galpones activos.

### Definición del evento para User Story 1

- **Evento producido**: `PlantillaNutricionalCreada(plantillaId, nombre, tipoAve, esPredeterminado, occurredAt)`.
- **Evento consumido**: Ninguno.

### Definición de endpoints REST para User Story 1

| Elemento | Definición |
| --- | --- |
| Método y ruta | `POST /api/nutricion/plantillas` |
| Autorización | `ROLE_NUTRICIONISTA` |
| Entrada | `CrearPlantillaRequest`: nombre, descripcion, tipoAve, esPredeterminado, lista de `EtapaPlantillaRequest` (etapaCrianza, diaInicioBase, diaFinBase, racionKgPolloDia, tipoAlimentoId, alimentoSugeridoId). |
| Respuesta 201 | `PlantillaDetalleResponse` con ID generado, detalle de etapas, duraciones calculadas y estado activo. |
| Errores | 400 por etapas no contiguas o ración inválida, 401 sin autenticación, 403 sin rol, 409 por conflicto de duplicidad de nombre. |

| Elemento | Definición |
| --- | --- |
| Método y ruta | `PUT /api/nutricion/plantillas/{id}` |
| Autorización | `ROLE_NUTRICIONISTA` |
| Entrada | `ModificarPlantillaRequest`: nombre, descripcion, esPredeterminado, lista actualizada de etapas. |
| Respuesta 200 | `PlantillaDetalleResponse` actualizada. Aplica exclusivamente para futuros lotes. |
| Errores | 400 por validación, 404 si no existe la plantilla. |

| Elemento | Definición |
| --- | --- |
| Método y ruta | `GET /api/nutricion/plantillas` y `GET /api/nutricion/plantillas/{id}` |
| Autorización | `ROLE_NUTRICIONISTA`, `ROLE_ADMINISTRADOR` |
| Respuesta 200 | Listado o detalle completo de la plantilla maestra solicitada. |

#### JSON de creación exitosa (`POST /api/nutricion/plantillas`)

```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "nombre": "Plan Estándar Broiler 45 días",
  "descripcion": "Curva estándar para pollos de engorde pesados",
  "tipoAve": "BROILER",
  "esPredeterminado": true,
  "activo": true,
  "etapas": [
    {
      "id": "a1b2c3d4-0001-4000-8000-000000000001",
      "etapaCrianza": "PRE_INICIO",
      "diaInicioBase": 1,
      "diaFinBase": 7,
      "duracionDiasBase": 7,
      "racionKgPolloDia": 0.0150,
      "tipoAlimentoId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
      "alimentoSugeridoId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
    },
    {
      "id": "a1b2c3d4-0002-4000-8000-000000000002",
      "etapaCrianza": "INICIO",
      "diaInicioBase": 8,
      "diaFinBase": 21,
      "duracionDiasBase": 14,
      "racionKgPolloDia": 0.0450,
      "tipoAlimentoId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6e",
      "alimentoSugeridoId": null
    },
    {
      "id": "a1b2c3d4-0003-4000-8000-000000000003",
      "etapaCrianza": "FINALIZACION_ENGORDE",
      "diaInicioBase": 22,
      "diaFinBase": 45,
      "duracionDiasBase": 24,
      "racionKgPolloDia": 0.0900,
      "tipoAlimentoId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6f",
      "alimentoSugeridoId": null
    }
  ]
}
```

### Tests para User Story 1

- [ ] T014 [US1] Probar en `PlanNutricionalPlantillaTest` validaciones de continuidad de etapas (`DiaInicio_i = DiaFin_{i-1} + 1`), rechazo de inicio != 1, solapamientos y raciones negativas o cero.
- [ ] T015 [US1] Probar en `CrearPlantillaPlanNutricionalUseCaseTest` la desmarcación automática de otros planes predeterminados del mismo `TipoAve` y publicación del evento.
- [ ] T016 [US1] Probar en `ModificarPlantillaPlanNutricionalUseCaseTest` el aislamiento estricto: la modificación de la plantilla no altera las instancias de galpón ya persistidas.
- [ ] T017 [US1] Probar en `PlanNutricionalPlantillaControllerTest` códigos HTTP 201, 200, 400, 403 y 404, y serialización de `racionKgPolloDia` con 4 decimales.

### Implementación de User Story 1

- [ ] T018 [US1] Implementar `CrearPlantillaPlanNutricionalUseCase` garantizando la transacción atómica de guardado y desmarcación del predeterminado previo.
- [ ] T019 [US1] Implementar `ModificarPlantillaPlanNutricionalUseCase`, `ConsultarPlantillasPlanNutricionalUseCase` y `ConsultarDetallePlantillaUseCase`.
- [ ] T020 [US1] Crear DTOs de entrada y salida, mappers REST en `NutricionRestMapper` y validaciones Jakarta.
- [ ] T021 [US1] Implementar `PlanNutricionalPlantillaController` y asegurar las restricciones de seguridad por rol.

**Checkpoint**: Catálogo global de plantillas operando con validaciones de contigüidad y unicidad de predeterminado.

---

## Phase 4: User Story 2 — Asignación de Plan a Galpón, Excepciones y Sustitución Históricamente Segura (Priority: P1)

**Spec**: 021, historia 2.

**Goal**: Asignar automáticamente una copia desacoplada del plan predeterminado al ingresar un lote, tratar excepciones por ausencia de plan o ingreso por edad > 1, y permitir sustituciones en lotes en curso preservando etapas históricas y blindando la etapa activa.

**Independent Test**: Alojar un lote de día 1 (asigna y activa Pre-inicio); alojar un lote de 14 días (omite Pre-inicio y activa Inicio en día 14); alojar sin plan predeterminado (marca galpón en `PENDIENTE_ASIGNACION` y demanda 0.0); sustituir plan en día 25 (mantiene etapas concluidas y activa inmutables, aplicando nuevo plan solo a futuras).

### Definición del evento para User Story 2

- **Evento consumido**: `LoteAlojado(galponId, loteId, tipoAve, edadDiasIngreso, fechaIngreso, poblacionInicial, occurredAt)` (desde Módulo 1).
- **Eventos producidos**:
  - `PlanNutricionalAsignado(planGalponId, galponId, loteId, plantillaId, occurredAt)`
  - `PlanNutricionalPendienteAsignacion(galponId, loteId, tipoAve, mensaje, occurredAt)`
  - `PlanNutricionalSustituido(planGalponId, galponId, loteId, nuevaPlantillaId, occurredAt)`

### Definición de endpoints REST para User Story 2

| Elemento | Definición |
| --- | --- |
| Método y ruta | `GET /api/galpones/{galponId}/plan-nutricional` |
| Autorización | `ROLE_NUTRICIONISTA`, `ROLE_ADMINISTRADOR` |
| Respuesta 200 | `PlanNutricionalGalponResponse`: ID de plan, galpón, lote, estado (`ACTIVO`, `PENDIENTE_ASIGNACION`), listado de `CalendarioEtapaGalponResponse` con días, estados (`OMITIDA`, `ACTIVA`, `COMPLETADA`, `PROGRAMADA`), ración, alimento asignado y flag `alimentoBloqueado`. |

| Elemento | Definición |
| --- | --- |
| Método y ruta | `PUT /api/galpones/{galponId}/plan-nutricional/sustituir` |
| Autorización | `ROLE_NUTRICIONISTA` |
| Entrada | `SustituirPlanRequest`: `nuevaPlantillaId`, justificación. |
| Respuesta 200 | Plan actualizado reflejando etapas completadas intactas, etapa activa con insumos y bloqueos inalterados, y etapas futuras sustituidas según la nueva plantilla. |
| Errores | 400 por datos inválidos, 404 si galpón/plan no existe, 409 si el galpón no tiene lote activo o está finalizado. |

#### JSON de respuesta (`GET /api/galpones/{galponId}/plan-nutricional`)

```json
{
  "planGalponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "galponId": "550e8400-e29b-41d4-a716-446655440000",
  "loteId": "2fc03a21-84c4-4c83-bb15-6607bb75cb90",
  "estado": "ACTIVO",
  "fechaAsignacion": "2026-09-15",
  "etapas": [
    {
      "id": "e001-4562-b3fc-2c963f66afa1",
      "etapaCrianza": "PRE_INICIO",
      "diaInicio": 1,
      "diaFin": 7,
      "diasEfectivos": 7,
      "estadoEtapa": "COMPLETADA",
      "alimentoBloqueado": true,
      "racionKgPolloDia": 0.0150,
      "alimentoNombre": "Pre-iniciador Premium 40kg",
      "pesoNetoKgPorBulto": 40.00
    },
    {
      "id": "e002-4562-b3fc-2c963f66afa2",
      "etapaCrianza": "INICIO",
      "diaInicio": 8,
      "diaFin": 21,
      "diasEfectivos": 14,
      "estadoEtapa": "ACTIVA",
      "alimentoBloqueado": true,
      "racionKgPolloDia": 0.0450,
      "alimentoNombre": "Iniciador Fuerte 40kg",
      "pesoNetoKgPorBulto": 40.00
    },
    {
      "id": "e003-4562-b3fc-2c963f66afa3",
      "etapaCrianza": "FINALIZACION_ENGORDE",
      "diaInicio": 22,
      "diaFin": 45,
      "diasEfectivos": 24,
      "estadoEtapa": "PROGRAMADA",
      "alimentoBloqueado": false,
      "racionKgPolloDia": 0.0900,
      "alimentoNombre": "Engorde Pellet 40kg",
      "pesoNetoKgPorBulto": 40.00
    }
  ]
}
```

### Tests para User Story 2

- [ ] T022 [US2] Probar en `AsignarPlanNutricionalLoteUseCaseTest` la clonación desacoplada del plan predeterminado al recibir `LoteAlojado`.
- [ ] T023 [US2] Probar la regla de edad de ingreso > 1: verificar que etapas anteriores se marquen como `OMITIDA` y la etapa correspondiente se active de inmediato.
- [ ] T024 [US2] Probar la ausencia de plan predeterminado: galpón pasa a `PENDIENTE_ASIGNACION`, publicación de alerta prioritaria y demanda diaria en `0.0`.
- [ ] T025 [US2] Probar en `SustituirPlanNutricionalGalponUseCaseTest` que las etapas `COMPLETADA` y `ACTIVA` se mantengan rigurosamente intactas y la nueva plantilla solo se acople a etapas futuras `PROGRAMADA`.
- [ ] T026 [US2] Probar en `LoteAlojadoEventListenerTest` el manejo idempotente ante reentregas del evento de alojamiento.

### Implementación de User Story 2

- [ ] T027 [US2] Implementar `AsignarPlanNutricionalLoteUseCase` contemplando asignación automática por evento y asignación manual de contingencia.
- [ ] T028 [US2] Implementar `LoteAlojadoEventListener` consumiendo el evento interno de Spring Modulith.
- [ ] T029 [US2] Implementar `SustituirPlanNutricionalGalponUseCase` con lógica de acoplamiento no destructivo.
- [ ] T030 [US2] Implementar endpoints de consulta y sustitución en `PlanNutricionalGalponController`.

**Checkpoint**: Asignación automática por evento, control de excepciones por edad/ausencia y sustitución segura completadas.

---

## Phase 5: User Story 3 — Selección de Alimento Compatible, Bloqueo en Etapa Activa e Integridad con SPEC-022 (Priority: P1)

**Spec**: 021, historia 3.

**Goal**: Consultar alimentos compatibles y stock real en bodega central, permitir seleccionar alimentos para etapas futuras ajustando la duración por perfil nutricional en cascada, y bloquear estrictamente cualquier cambio de alimento en la etapa activa para blindar SPEC-022.

**Independent Test**: Consultar alimentos compatibles para etapa "Inicio" viendo stock en kg y bultos; seleccionar un producto con `duracionRecomendadaEtapaDias = 10` para una etapa futura y verificar ajuste a 10 días y desplazamiento en cascada; intentar cambiar el alimento de una etapa activa y comprobar que el sistema lo rechaza con 409.

### Definición del evento para User Story 3

- **Evento producido**: `AlimentoEtapaConfigurado(planGalponId, etapaId, alimentoId, duracionDias, occurredAt)`.
- **Evento consumido**: Ninguno.

### Definición de endpoints REST para User Story 3

| Elemento | Definición |
| --- | --- |
| Método y ruta | `GET /api/nutricion/alimentos-compatibles` |
| Autorización | `ROLE_NUTRICIONISTA`, `ROLE_ADMINISTRADOR` |
| Parámetros | `tipoAlimentoId` (UUID obligatorio), `etapaCrianza` (opcional). |
| Respuesta 200 | Listado de `AlimentoCompatibleResponse`: ID, nombreComercial, marca, pesoNominalPorBulto, duracionRecomendadaEtapaDias, stockDisponibleKg, stockDisponibleBultos, sinStock (booleano). Excluye vencidos o anulados. |

| Elemento | Definición |
| --- | --- |
| Método y ruta | `PUT /api/galpones/{galponId}/plan-nutricional/etapas/{etapaId}/alimento` |
| Autorización | `ROLE_NUTRICIONISTA` |
| Entrada | `ConfigurarAlimentoEtapaRequest`: `alimentoId`, `aplicarDuracionRecomendada` (booleano). |
| Respuesta 200 | Etapa actualizada con el nuevo alimento y cascada recalculada si cambió la duración. |
| Errores | 400 por tipo de alimento incompatible, 404 si etapa o alimento no existe, **409 `ETAPA_ACTIVA_INMUTABLE`** si la etapa está en curso, **409 `ETAPA_COMPLETADA_INMUTABLE`** si la etapa ya concluyó. |

#### JSON de respuesta de compatibilidad (`GET /api/nutricion/alimentos-compatibles`)

```json
[
  {
    "alimentoId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "nombreComercial": "Iniciador Pollito Fuerte",
    "marca": "NutriAvícola",
    "pesoNominalPorBulto": 40.00,
    "duracionRecomendadaEtapaDias": 10,
    "stockDisponibleKg": 2400.00,
    "stockDisponibleBultos": 60.00,
    "sinStock": false
  },
  {
    "alimentoId": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
    "nombreComercial": "Iniciador Plus Concentrado",
    "marca": "Campiña",
    "pesoNominalPorBulto": 50.00,
    "duracionRecomendadaEtapaDias": null,
    "stockDisponibleKg": 0.00,
    "stockDisponibleBultos": 0.00,
    "sinStock": true
  }
]
```

### Tests para User Story 3

- [ ] T031 [US3] Probar en `ConsultarAlimentosCompatiblesEtapaUseCaseTest` el filtrado por `TipoAlimento`, obtención de stock disponible desde bodega central y cálculo de bultos equivalentes.
- [ ] T032 [US3] Probar en `ConfigurarAlimentoEtapaFuturaUseCaseTest` el recálculo en cascada al aplicar `duracionRecomendadaEtapaDias = 10` en una etapa programada.
- [ ] T033 [US3] Probar que el guardado de la asignación NO genera movimientos de inventario ni transacciones en bodega.
- [ ] T034 [US3] Probar el rechazo estricto (código 409) con excepción `EtapaActivaInmutableException` al intentar modificar el alimento en una etapa activa o completada.
- [ ] T035 [US3] Probar en `AlimentoCompatibleControllerTest` respuestas HTTP y contratos.

### Implementación de User Story 3

- [ ] T036 [US3] Implementar `AlimentoCatalogoQueryAdapter` y `StockBodegaQueryAdapter` conectando con los contratos de catálogo y bodega central.
- [ ] T037 [US3] Implementar `ConsultarAlimentosCompatiblesEtapaUseCase`.
- [ ] T038 [US3] Implementar `ConfigurarAlimentoEtapaFuturaUseCase` con validación de estado de etapa (`PROGRAMADA`), compatibilidad de tipo de alimento y actualización de duración.
- [ ] T039 [US3] Exponer endpoints correspondientes en `AlimentoCompatibleController` y `PlanNutricionalGalponController`.

**Checkpoint**: Consulta de stock real, selección de alimentos en etapas futuras con recálculo de días y blindaje absoluto de etapa activa implementados.

---

## Phase 6: User Story 4 — Ajuste Manual de Días de Etapa, Cierre de Cascada y Sincronización de Salida (Priority: P1)

**Spec**: 021, historia 4.

**Goal**: Ajustar manualmente la duración en días de una etapa activa por bajo peso o cuarentena sanitaria, cerrando matemáticamente la cascada sobre etapas futuras, actualizando la fecha proyectada de salida/sacrificio y acumulando el historial de auditoría sin modificar el alimento ni el costo capturado.

**Independent Test**: En un plan con Inicio (días 8-21, 14 días) y Engorde (días 22-45, 24 días), prorrogar Inicio hasta el día 26 (+5 días) con justificación técnica; verificar que Engorde inicie en día 27 conservando sus 24 días (termina en día 50), la fecha de salida del lote se postergue 5 días y se cree un registro auditable en el historial manteniendo intacto el alimento comercial.

### Definición del evento para User Story 4

- **Eventos producidos**:
  - `DuracionEtapaAjustada(planGalponId, etapaId, diasProrroga, diasEfectivosNuevos, motivo, justificacion, occurredAt)`
  - `FechaProyectadaSalidaLoteActualizada(galponId, loteId, nuevaFechaSalida, diasDesplazamiento, occurredAt)`

### Definición de endpoints REST para User Story 4

| Elemento | Definición |
| --- | --- |
| Método y ruta | `PUT /api/galpones/{galponId}/plan-nutricional/etapas/{etapaId}/ajuste-duracion` |
| Autorización | `ROLE_NUTRICIONISTA` |
| Entrada | `AjustarDuracionEtapaRequest`: `nuevoDiaFin` (Integer), `motivo` (`BAJO_PESO` o `CUARENTENA_SANITARIA`), `justificacion` (String obligatorio, no blanco). |
| Respuesta 200 | `CalendarioEtapaResponse` actualizado con la nueva duración, etapas subsiguientes recalculadas y resumen de la nueva fecha de salida proyectada. |
| Errores | 400 si `justificacion` está vacía o `nuevoDiaFin < edadActualLote`, 404 si no existe la etapa, 409 si la etapa es `COMPLETADA` u `OMITIDA`. |

| Elemento | Definición |
| --- | --- |
| Método y ruta | `GET /api/galpones/{galponId}/plan-nutricional/etapas/{etapaId}/historial-ajustes` |
| Autorización | `ROLE_NUTRICIONISTA`, `ROLE_ADMINISTRADOR` |
| Respuesta 200 | Listado cronológico acumulativo de `HistorialAjusteResponse` con fecha/hora, usuario, motivo, justificación, días previos y días nuevos. |

#### JSON de solicitud de ajuste (`PUT /api/galpones/.../ajuste-duracion`)

```json
{
  "nuevoDiaFin": 26,
  "motivo": "BAJO_PESO",
  "justificacion": "Lote presenta 80g por debajo del peso estándar de la tabla Ross 308; se prorroga etapa de inicio 5 días adicionales para consolidar ganancia compensatoria."
}
```

#### JSON de respuesta de ajuste exitoso

```json
{
  "etapaAjustada": {
    "id": "e002-4562-b3fc-2c963f66afa2",
    "etapaCrianza": "INICIO",
    "diaInicio": 8,
    "diaFin": 26,
    "diasBase": 14,
    "diasProrroga": 5,
    "diasEfectivos": 19,
    "estadoEtapa": "ACTIVA",
    "alimentoBloqueado": true,
    "alimentoNombre": "Iniciador Fuerte 40kg"
  },
  "etapasPosterioresDesplazadas": [
    {
      "etapaCrianza": "FINALIZACION_ENGORDE",
      "diaInicio": 27,
      "diaFin": 50,
      "diasEfectivos": 24,
      "estadoEtapa": "PROGRAMADA"
    }
  ],
  "desplazamientoTotalDias": 5,
  "nuevaFechaProyectadaSalida": "2026-11-04"
}
```

### Tests para User Story 4

- [ ] T040 [US4] Probar en `CalendarioEtapaGalponTest` la fórmula de cascada: etapas posteriores conservan exactamente sus días efectivos configurados.
- [ ] T041 [US4] Probar la acumulación histórica: registrar dos prórrogas sucesivas sobre la misma etapa activa y verificar que ambas existan en `HistorialAjusteEtapa` sin sobreescrituras.
- [ ] T042 [US4] Probar el rechazo de ajustes con justificación vacía o en blanco (`JustificacionAjusteRequeridaException`).
- [ ] T043 [US4] Probar el rechazo estricto ante intentos de ajustar etapas en estado `COMPLETADA`.
- [ ] T044 [US4] Probar la sincronización con el Módulo 1 a través de `ActualizarFechaSalidaLotePort` y la emisión del evento `FechaProyectadaSalidaLoteActualizada`.

### Implementación de User Story 4

- [ ] T045 [US4] Implementar `AjustarDuracionEtapaActivaUseCase` coordinando la validación de negocio, recálculo de cascada, anexado al historial y notificación al puerto de fecha de salida.
- [ ] T046 [US4] Implementar `ActualizarFechaSalidaLoteAdapter` comunicando la nueva fecha estimada al agregado de lote en el Módulo 1.
- [ ] T047 [US4] Implementar endpoints de ajuste de duración y consulta de historial en `PlanNutricionalGalponController`.

**Checkpoint**: Ajustes de duración auditados, cascada matemática consistente y sincronización de fecha de salida operativa.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Verificaciones transversales, documentación OpenAPI, pruebas de integración y consistencia arquitectónica.

- [ ] T048 Documentar todos los endpoints, esquemas JSON y códigos de error (400, 401, 403, 404, 409) en OpenAPI 3 / Swagger.
- [ ] T049 Implementar `PlanNutricionalIntegrationTest` con Testcontainers y base de datos PostgreSQL real: recorrer ciclo completo desde creación de plantilla, alojamiento de lote, consulta de stock, blindaje de etapa activa y ajuste de duración en cascada.
- [ ] T050 Probar que las consultas y configuraciones de plan nutricional no generen movimientos de stock ni bloqueos de concurrencia en la tabla de inventario de bodega central.
- [ ] T051 Verificar reglas de arquitectura con ArchUnit: asegurar que `domain/model/nutricion` no dependa de Spring, JPA ni librerías de infraestructura.
- [ ] T052 Validar tiempos de respuesta: asegurar que el 95 % de las operaciones de ajuste en cascada y consultas de compatibilidad respondan en menos de 1 segundo.

**Checkpoint**: Integración end-to-end verificada, documentación OpenAPI completada y cumplimiento de calidad arquitectónica asegurado.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Fase 1)**: Inmediata, no posee dependencias.
- **Foundational (Fase 2)**: Depende de Setup. Bloquea absolutamente el inicio de todas las historias de usuario.
- **US1 (Fase 3)**: Depende de Foundational. Establece el catálogo maestro.
- **US2 (Fase 4)**: Depende de Foundational y requiere el catálogo de US1 para realizar la clonación y asignación automática.
- **US3 (Fase 5)**: Depende de Foundational y de la existencia de calendarios de galpón (US2).
- **US4 (Fase 6)**: Depende de US2 (etapas en galpón) y reutiliza el blindaje definido en US3.
- **Polish (Fase 7)**: Depende de la finalización de US1 a US4.

### Dependencias con otros planes

- **[Plan 001](001-ConsultaYSeguimientoDeGalpones.md)**: Reutiliza `GalponQueryPort` y `LoteQueryPort` para consultar el estado del galpón, lote alojado y edad actual.
- **[Plan 002](002-GestionDeInventarioYRecepciones.md)**: Provee la información de stock físico disponible en bodega central mediante `StockBodegaQueryPort`.
- **[Plan 004](004-RequerimientosYSeguimientoDeAlimentacion.md)**: Consume las etapas nutricionales activas, raciones diarias, productos asignados y los eventos `EtapaNutricionalActivada` y `DuracionEtapaAjustada` emitidos por este Plan 003 para calcular la cuota operativa del día (SPEC-014), registrar la proyección oficial por lote para Finanzas y consolidar el balance de compras (SPEC-022).

### Dentro de cada User Story

1. Pruebas unitarias de dominio antes o junto con la implementación de reglas.
2. Casos de uso orquestando puertos e inyectando actor y `Clock`.
3. Adaptadores y mappers desacoplados del dominio.
4. Pruebas de integración MockMvc para verificar códigos HTTP y seguridad.
5. Checkpoint completado antes de iniciar la siguiente historia.

---

## Notes

- T001 a T052 identifican las tareas concretas de implementación.
- `RacionDiaria` es un Value Object inmutable que previene inconsistencias de cálculo al exigir valores mayores a 0 y controlar la escala decimal.
- La inmutabilidad de la etapa activa es un requisito crítico de diseño para evitar la corrupción de las proyecciones financieras requeridas por el Módulo 3 en el SPEC-022.
- Este plan garantiza la trazabilidad total mediante auditoría inmutable en `HistorialAjusteEtapa`.
