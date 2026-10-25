# Implementation Plan: Seguimiento Operativo de Alimentación

**Date**: 25/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [014-ConsultarAlimentoPorGalponPorDia.md](../specs/014-ConsultarAlimentoPorGalponPorDia.md)

## Summary

Implementar la operación diaria de alimentación por galpón: consulta del requerimiento para el lote alojado, alertas de disponibilidad y cumplimiento, resumen para el trabajador y registro del alimento realmente suministrado. Los suministros confirmados descuentan inventario mediante el contrato del Plan 010, con trazabilidad por recepción y sin deducciones parciales.

## Technical Context

**Performance Goals**: 95 % de consultas diarias y del resumen del trabajador en máximo 1 segundo.

**Constraints**: roles `ROLE_TRABAJADOR` y `ROLE_ADMINISTRADOR`; fechas de negocio en `America/Bogota`; consulta de solo lectura; suministro y salida FEFO confirmados atómicamente.

**Scale/Scope**: SPEC-014, cuatro historias; tres endpoints REST; un agregado persistido (`SuministroDiario`); consultas internas a Planes 001, 003 y 010.

**Datos propios**: `SuministroDiario`, asociado a lote, galpón, alimento, fecha de negocio, actor y operación idempotente de salida.

**Dependencias**:

- Plan 001: estado del galpón, lote activo, edad y población viva.
- Plan 003: etapa, ración, alimento asignado y cambios nutricionales.
- Plan 010: consulta de disponibilidad y comando de salida FEFO para suministros confirmados.
- [SPEC-023](../specs/023-ConsultarInventario.md): reglas del inventario de solo lectura y disponibilidad que provee Plan 010.
- [General.md](General.md): arquitectura, transacciones, autorización, errores, fechas y pruebas.

### Decisiones específicas

1. La consulta diaria y el dashboard son de solo lectura. El registro de suministro es la única operación de escritura de este plan.
2. Los cálculos diarios usan población viva actual y ración activa; la fecha de negocio es `America/Bogota` y el instante de auditoría es UTC.
3. `SuministroDiario` y la salida del Plan 010 se confirman atómicamente. Se usa clave idempotente; si no hay stock suficiente o falla Inventario, no se confirma ninguna parte.
4. El Plan 004 no accede a tablas ni repositorios del Plan 010. Las salidas y su asignación FEFO son propiedad del inventario.
5. La cuota puede recibirse en kg/ave/día desde Plan 003 y convertirse a gramos por ave para la presentación operativa. Las cantidades se calculan con `BigDecimal` y las escalas definidas en SPEC-014.

### Alcance documental

- Este plan implementa únicamente SPEC-014: consulta operativa por galpón, alertas del día, resumen para trabajador y registro del suministro diario.
- Las etapas, cuotas y selección de alimento pertenecen al [Plan 003](003-ConfiguracionYAsignacionDePlanesNutricionales.md); el Plan 004 las consulta y no modifica el plan nutricional.
- La propiedad del stock, vencimientos, FEFO y salidas pertenece al [Plan 010](010-GestionDelInventario.md). El Plan 004 persiste su suministro operativo y solicita una salida mediante un puerto.
- Proyección financiera por etapa, costo unitario capturado y balance de demanda para compras corresponden a SPEC-022 y al [Plan 011](011-ProyeccionesDeAlimentoPorLote.md).
- Cualquier trabajador autorizado puede seleccionar cualquier galpón existente; el resumen incluye únicamente los galpones indicados para esa consulta.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 014-ConsultarAlimentoPorGalponPorDia.md
└── plan/
    └── 004-SeguimientoOperativoDeAlimentacion.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/alimentacion/
│   │   ├── SuministroDiario.java
│   │   ├── ConsumoDiario.java
│   │   ├── RequerimientoDiarioGalpon.java
│   │   ├── ResumenAlimentoDiarioTrabajador.java
│   │   └── AlertaAlimentacion.java
│   ├── exception/alimentacion/
│   └── port/out/alimentacion/
│       ├── SuministroDiarioRepositoryPort.java
│       ├── SuministroDiarioQueryPort.java
│       ├── GalponQueryPort.java
│       ├── LoteQueryPort.java
│       ├── PlanNutricionalQueryPort.java
│       ├── StockBodegaQueryPort.java
│       └── RegistrarSalidaInventarioPort.java
├── application/alimentacion/
│   ├── ConsultarAlimentoGalponDiaUseCase.java
│   ├── ConsultarResumenAlimentoTrabajadorUseCase.java
│   ├── RegistrarSuministroDiarioUseCase.java
│   └── result/
└── infrastructure/
    ├── adapter/in/rest/alimentacion/
    │   ├── AlimentacionDiariaController.java
    │   ├── dto/
    │   └── mapper/AlimentacionRestMapper.java
    ├── adapter/out/persistence/alimentacion/
    │   ├── entity/SuministroDiarioEntity.java
    │   ├── repository/
    │   └── AlimentacionPersistenceAdapter.java
    ├── adapter/out/internal/alimentacion/
    │   ├── GalponLoteQueryAdapter.java
    │   ├── PlanNutricionalQueryAdapter.java
    └── config/AlimentacionOperativaBeanConfiguration.java
```

```text
src/test/java/com/avicontrol/
├── domain/model/alimentacion/
├── application/alimentacion/
├── infrastructure/adapter/in/rest/alimentacion/
└── infrastructure/integration/SeguimientoOperativoAlimentacionIntegrationTest.java
```

**Structure Decision**: El paquete `alimentacion` contiene las reglas y casos de uso de seguimiento operativo. Los adaptadores respetan las capas y dependencias definidas en [General.md](General.md); DTO REST, entidades JPA y modelos de dominio permanecen separados.

### Entidades y relación

```text
Lote (1) ───< (0..*) SuministroDiario >─── (1) Galpon
                            ├── (1) Alimento asignado
                            ├── (1) actor
                            └── (1) salida idempotente de Plan 010

SuministroDiario
├── id: UUID
├── loteId: UUID
├── galponId: UUID
├── alimentoId: UUID
├── fechaSuministro: LocalDate (America/Bogota)
├── cantidadKg: BigDecimal
├── registradoEn: Instant (UTC)
├── registradoPor: UUID
└── claveIdempotencia: String (única)
```

`SuministroDiario` registra únicamente cantidades físicas confirmadas. No almacena el requerimiento calculado, el saldo ni el stock; son valores de consulta derivados de Planes 001, 003, 010 y de los suministros confirmados.

`ConsumoDiario` calcula `kgRequeridos = poblacionActual × racionGrAveDia ÷ 1000`, `bultosRequeridos = kgRequeridos ÷ pesoNominalPorBulto` y `saldoPendienteKg = max(0, kgRequeridos − suministroConfirmadoKg)`. El cálculo usa las reglas de precisión de SPEC-014.

### Contratos de puertos

| Puerto | Tipo | Responsabilidad |
| --- | --- | --- |
| `GalponQueryPort` / `LoteQueryPort` | Consulta interna, Plan 001 | Obtener galpón, lote alojado, edad y población actual. |
| `PlanNutricionalQueryPort` | Consulta interna, Plan 003 | Obtener etapa activa, cuota, alimento compatible y peso nominal de bulto. |
| `StockBodegaQueryPort` | Consulta interna, Plan 010 | Consultar disponibilidad vigente en kg y bultos. |
| `SuministroDiarioRepositoryPort` | Persistencia propia | Guardar suministros confirmados y consultar acumulado por lote/alimento/fecha. |
| `RegistrarSalidaInventarioPort` | Comando interno, Plan 010 | Solicitar salida FEFO idempotente y atómica con el suministro. |
| `SuministroDiarioQueryPort` | Consulta propia | Leer acumulados confirmados por galpón/lote, alimento y fecha de negocio. |

Los puertos de Planes 001, 003 y 010 son interfaces de consulta/comando internas. Este plan no crea clientes HTTP al mismo monolito ni usa repositorios privados de esas capacidades.

### Contratos HTTP propuestos

| Método y ruta | Acceso | Responsabilidad |
| --- | --- | --- |
| `GET /api/galpones/{galponId}/alimento-diario` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` | Requerimiento, suministro acumulado, saldo, disponibilidad y alertas del día. |
| `GET /api/trabajador/resumen-alimento-diario` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` | Resumen diario de los galpones seleccionados. |
| `POST /api/galpones/{galponId}/suministros-alimento` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` | Registrar suministro y solicitar salida atómica FEFO al Plan 010. |

Los errores siguen el contrato `application/problem+json` de [General.md](General.md). Los detalles de campos y escenarios de respuesta permanecen en SPEC-014 y OpenAPI.

### JSON común de errores

```json
{
  "type": "https://avicontrol/errors/stock-insuficiente",
  "title": "Stock insuficiente",
  "status": 409,
  "detail": "No se puede confirmar el suministro completo con el stock vigente",
  "instance": "/api/galpones/{galponId}/suministros-alimento",
  "code": "STOCK_INSUFICIENTE",
  "correlationId": "uuid",
  "fieldErrors": []
}
```

Los errores conservan los códigos y estructura de [General.md](General.md); `code` es estable y el `status` coincide con el HTTP retornado.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Acordar contratos específicos de lectura nutricional e inventario y preparar los componentes de alimentación diaria.

- [ ] T001 Confirmar las interfaces internas de Planes 001, 003 y 010, unidades devueltas, errores y semántica de lote activo.
- [ ] T002 Acordar con Plan 010 la clave idempotente, el resultado de la salida y la coordinación transaccional con `SuministroDiario`.
- [ ] T003 Definir roles de las rutas y fixtures de lotes, planes, existencias y suministros parciales.

**Checkpoint**: contratos definidos sin repetir el stack ni crear una segunda fuente para lotes, planes o inventario.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Preparar el modelo operativo y los puertos compartidos por las cuatro historias.

- [ ] T004 Implementar `SuministroDiario`, `ConsumoDiario` y `AlertaAlimentacion` en dominio puro; validar cantidades positivas, escala y clave idempotente.
- [ ] T005 Definir `SuministroDiarioRepositoryPort`, `SuministroDiarioQueryPort`, puertos de consulta y el comando `RegistrarSalidaInventarioPort`.
- [ ] T006 Crear migración de `suministro_diario` con UUIDs de referencia, fecha de negocio, actor, cantidad y restricción única de idempotencia. No crear tablas del Plan 010.
- [ ] T007 Implementar mappers/adaptadores de persistencia e interfaces públicas internas; configurar casos de uso bajo transacción según General.md.
- [ ] T008 Probar consistencia de fecha local/UTC y ausencia de escrituras durante consultas.

**Checkpoint**: modelo y puertos operativos listos; se pueden implementar historias sin duplicar propiedad de otros planes.

## Phase 3: User Story 1 — Consultar el requerimiento diario (Priority: P1)

**Spec**: SPEC-014, historia 1.

**Goal**: Mostrar el alimento asignado al lote alojado, edad, población, ración, requerimiento diario, bultos, suministro confirmado, saldo y stock disponible.

**Independent Test**: Con 10.000 aves, ración 120 g y bulto de 40 kg, la respuesta calcula 1.200 kg y 30 bultos; con 400 kg suministrados muestra 800 kg pendientes. Sin lote activo devuelve datos del galpón y cantidades cero.

### Definición del evento

**Evento producido**: Ninguno. La consulta no cambia datos ni publica hechos.

**Evento consumido**: Ninguno. El caso de uso lee los estados vigentes de Planes 001 y 003; consulta los suministros confirmados del día en su propio repositorio. La consulta diaria no publica un evento `RequerimientoConsultado`.

**Eventos consumidos**: Ninguno para la lectura; se consulta el estado vigente de Planes 001 y 003. Los suministros de este plan se obtienen del repositorio local por fecha de negocio.

### Definición del endpoint REST

| Elemento | Definición |
| --- | --- |
| Método y ruta | `GET /api/galpones/{galponId}/alimento-diario` |
| Autorización | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` |
| Entrada | UUID `galponId`; fecha actual del negocio. No recibe body. |
| Respuesta 200 | Galpón y lote, edad, etapa, alimento, ración, kg/bultos requeridos, suministro acumulado, saldo pendiente, disponibilidad y alertas. |
| Errores | 400 UUID/formato inválido; 401 no autenticado; 403 sin rol; 404 galpón inexistente; 409 inconsistencia de más de un lote actual; 503 dependencia imprescindible no disponible. |

#### JSON de respuesta

```json
{
  "galponId": "550e8400-e29b-41d4-a716-446655440000",
  "galponNombre": "Galpón 1",
  "loteId": "2fc03a21-84c4-4c83-bb15-6607bb75cb90",
  "poblacionActual": 10000,
  "edadDias": 18,
  "etapa": "CRECIMIENTO",
  "alimentoId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "racionGrAveDia": 120.0000,
  "requerimientoKg": 1200.00,
  "requerimientoBultos": 30.00,
  "suministradoKg": 400.00,
  "saldoPendienteKg": 800.00,
  "stockDisponibleKg": 4000.00,
  "stockDisponibleBultos": 100.00,
  "alertas": []
}
```

### Tests para User Story 1

- [ ] T009 Probar fórmula en kg, equivalencia en bultos y redondeo para varios pesos nominales.
- [ ] T010 Probar lote inexistente, galpón sin lote, población cero, plan sin etapa y dependencia indisponible.
- [ ] T011 Probar que el suministro acumulado use solo registros confirmados del mismo lote, alimento y fecha local.
- [ ] T012 Probar autorización y contrato JSON/HTTP en `AlimentacionDiariaControllerTest`.

### Implementación para User Story 1

- [ ] T013 Implementar `ConsultarAlimentoGalponDiaUseCase` consultando galpón/lote, etapa, plan, suministros y stock.
- [ ] T014 Crear `RequerimientoDiarioGalponResult`, DTOs y mapper; representar ausencia de lote sin inventar datos.
- [ ] T015 Exponer el GET con validación, seguridad y manejo común de errores.

**Checkpoint**: consulta diaria consistente, de solo lectura y verificable de forma independiente.

## Phase 4: User Story 2 — Alertas operativas (Priority: P2)

**Spec**: SPEC-014, historia 2.

**Goal**: Incorporar alertas de stock insuficiente, ración pendiente y cambios de etapa/dieta durante la fecha consultada.

**Independent Test**: Para 1.200 kg requeridos con 600 kg disponibles y 400 kg suministrados, respuesta muestra alertas de déficit y suministro pendiente; un día sin incidencias no muestra alertas.

### Definición del evento y endpoint REST

**Evento producido**: Ninguno; las alertas se calculan al consultar.

**Evento consumido**: Cambios de etapa/dieta solo si Plan 003 ofrece una consulta de cambios efectivos para la fecha; acordar el contrato interno en Setup. No agregar listener ni tabla local para duplicar el plan.

El endpoint es `GET /api/galpones/{galponId}/alimento-diario`, definido en US1. La respuesta agrega `alertas` con tipo, mensaje y severidad; no se crea una ruta separada.

```json
"alertas": [
  {
    "tipo": "DESABASTECIMIENTO_BODEGA",
    "mensaje": "Stock disponible inferior al requerimiento del día",
    "severidad": "ALTA"
  },
  {
    "tipo": "SUMINISTRO_PENDIENTE",
    "mensaje": "Queda alimento pendiente por suministrar en la jornada",
    "severidad": "MEDIA"
  }
]
```

### Tests e implementación

- [ ] T016 Probar `DESABASTECIMIENTO_BODEGA`, `SUMINISTRO_PENDIENTE`, `CAMBIO_PLAN_NUTRICIONAL_HOY` y ausencia de alertas.
- [ ] T017 Implementar el evaluador de alertas en el caso de uso de consulta diaria, respetando severidad y umbrales de SPEC-014.
- [ ] T018 Verificar que las alertas no creen escrituras ni eventos.

**Checkpoint**: alertas reproducibles a partir de datos vigentes y sin efectos laterales.

## Phase 5: User Story 3 — Resumen diario del trabajador (Priority: P2)

**Spec**: SPEC-014, historia 3.

**Goal**: Mostrar tarjeta agregada y detalle por galpón para el conjunto solicitado por el trabajador.

**Independent Test**: Para galpones con 450, 430 y 370 kg requeridos, devolver 1.250 kg totales y tres filas; una selección vacía devuelve lista y total cero.

### Definición del evento y endpoint REST

**Evento producido/consumido**: Ninguno; se consulta por conjunto y se compone un resultado de lectura.

| Elemento | Definición |
| --- | --- |
| Método y ruta | `GET /api/trabajador/resumen-alimento-diario` |
| Autorización | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` |
| Entrada | Lista `galponIds` seleccionados en la consulta; puede estar vacía. |
| Respuesta 200 | Totales requeridos en kg/bultos, estado global de disponibilidad y filas por galpón. |
| Errores | 400 IDs inválidos; 401/403 seguridad; 503 si una dependencia imprescindible impide una respuesta consistente. |

Si `galponIds` está vacío o no se seleccionaron galpones, el caso de uso devuelve total cero, lista vacía y mensaje informativo. No amplía implícitamente la solicitud a todos los galpones.

#### JSON de respuesta

```json
{
  "totalKgRequeridos": 1250.00,
  "totalBultosRequeridos": 25.00,
  "estadoGlobalBodega": "BODEGA_CENTRAL_OK",
  "galpones": [
    {
      "galponId": "550e8400-e29b-41d4-a716-446655440001",
      "galponNombre": "Galpón 1",
      "tipoAlimento": "Engorde Stage 2",
      "cuotaKg": 450.00,
      "cuotaBultos": 9.00,
      "estadoBodega": "SUFICIENTE"
    }
  ]
}
```

### Tests e implementación

- [ ] T019 Probar sumas, selección parcial, galpón sin lote, lista vacía y estado global ante déficit en uno o varios galpones.
- [ ] T020 Implementar `ConsultarResumenAlimentoTrabajadorUseCase` con consultas por conjunto para evitar llamada por galpón.
- [ ] T021 Exponer DTOs y endpoint con paginación/límite si el tamaño potencial del universo lo requiere.

**Checkpoint**: dashboard consistente con las consultas individuales y sin escrituras.

## Phase 6: User Story 4 — Registrar suministro diario (Priority: P1)

**Spec**: SPEC-014, historia 4.

**Goal**: Registrar cantidades realmente suministradas y descontarlas de forma trazable a través del Plan 010.

**Independent Test**: Un suministro de 400 kg crea un registro y salida de inventario de 400 kg; la consulta posterior refleja el acumulado. Si el stock solo permite 300 kg o falla Inventario, no hay suministro ni descuento parcial.

### Definición del evento y endpoint REST

**Evento producido**: ninguno por defecto. Si se acuerda un consumidor interno que lo necesite, podrá publicarse `SuministroDiarioConfirmado` después de confirmar atómicamente el suministro y la salida; nunca antes del commit.

**Evento consumido**: Ninguno; el caso de uso llama al puerto interno del Plan 010.

La salida del inventario y el suministro confirmado ya representan los registros requeridos por SPEC-014.

| Elemento | Definición |
| --- | --- |
| Método y ruta | `POST /api/galpones/{galponId}/suministros-alimento` |
| Autorización | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` |
| Entrada | `alimentoId`, `cantidadKg > 0` y clave idempotente; lote, fecha local y actor salen del contexto vigente. |
| Respuesta 201 | Suministro confirmado, cantidad, bultos, fecha y referencia de salida. Repetición idempotente devuelve el resultado original. |
| Errores | 400 datos inválidos; 401/403; 404 galpón/lote; 409 alimento incompatible, clave reutilizada con otro payload o stock insuficiente; 503 inventario no disponible. |

#### JSON de solicitud

```json
{
  "alimentoId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "cantidadKg": 400.00
}
```

#### JSON de respuesta 201

```json
{
  "suministroId": "6ac9c7c3-3aa9-4a59-a946-85ed3c85be31",
  "galponId": "550e8400-e29b-41d4-a716-446655440000",
  "loteId": "2fc03a21-84c4-4c83-bb15-6607bb75cb90",
  "alimentoId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "cantidadKg": 400.00,
  "fechaSuministro": "2026-10-25",
  "salidaInventarioId": "7375e4b0-cc81-4c4d-920b-3d2c7f0d1342"
}
```

### Tests e implementación

- [ ] T022 Probar actor, lote activo, fecha `America/Bogota`, alimento de la etapa y cantidad positiva.
- [ ] T023 Probar salida FEFO distribuida por recepción y que los detalles sumen la cantidad solicitada.
- [ ] T024 Probar idempotencia de igual clave/payload y conflicto si la misma clave se reusa con contenido diferente.
- [ ] T025 Probar rollback conjunto por stock insuficiente, fallo del Plan 010 o error al guardar suministro.
- [ ] T026 Implementar `RegistrarSuministroDiarioUseCase`, persistencia, endpoint y adaptador al comando de salida del Plan 010.

**Checkpoint**: cada suministro confirmado tiene salida equivalente, trazable e idempotente.

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Verificar contratos, integración y reglas compartidas.

- [ ] T027 Documentar los tres endpoints, parámetros, DTOs, roles y errores en OpenAPI 3.
- [ ] T028 Ejecutar pruebas de integración con PostgreSQL para consulta, suministro y reversión de fallo de Inventario.
- [ ] T029 Verificar arquitectura con ArchUnit y ausencia de accesos a repositorios privados de Planes 001, 003 y 010.
- [ ] T030 Verificar consistencia de consultas por conjunto y métricas de rendimiento de SPEC-014.
- [ ] T031 Confirmar que ningún endpoint de consulta escribe movimientos, reservas o suministros.

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Fase 1)** habilita Foundational y define contratos de integración.
- **Foundational (Fase 2)** bloquea todas las historias al preparar entidad, persistencia y puertos.
- **US1 y US2** dependen de Planes 001 y 003; US2 extiende el resultado calculado por US1.
- **US3** reutiliza los cálculos de US1 y requiere consulta por conjunto.
- **US4** depende además del comando atómico de salida de Plan 010; US1 consume los registros creados por US4.
- **Polish** depende de las cuatro historias.

### Dependencias con otros planes

- [Plan 001](001-ConsultaDeGalpones.md): galpón y lote alojado, edad y población viva actual.
- [Plan 003](003-ConfiguracionYAsignacionDePlanesNutricionales.md): etapa vigente, ración, alimento asignado y cambios del día.
- [Plan 010](010-GestionDelInventario.md): disponibilidad actual, compatibilidad, salida FEFO y trazabilidad por recepción.
- [Plan 011](011-ProyeccionesDeAlimentoPorLote.md): responsabilidad distinta de proyección financiera y balance de demanda.
- [General.md](General.md): contratos internos, transacciones, actor, reloj, errores y pruebas.

### Dentro de cada User Story

1. Acordar interfaces de salida antes de adaptar la implementación de capacidades propietarias.
2. Implementar dominio/caso de uso y su prueba antes de exponer DTO/controlador.
3. Verificar permisos, errores y no-escritura en consultas.
4. No cerrar US4 hasta probar atomicidad con Plan 010 y el camino de reintento idempotente.

## Notes

- T001 a T031 identifican tareas; US1 a US4 identifican las historias de SPEC-014.
- Este plan describe componentes por implementar y no afirma que ya existan.
- El alcance funcional es SPEC-014. Las proyecciones por etapa, su costo de referencia y el balance de demanda corresponden a SPEC-022 y al [Plan 011](011-ProyeccionesDeAlimentoPorLote.md).
- La propiedad de movimientos, saldos, disponibilidad FEFO y salidas permanece en el [Plan 010](010-GestionDelInventario.md).
- Los cambios de etapa y de duración son propiedad del [Plan 003](003-ConfiguracionYAsignacionDePlanesNutricionales.md).
