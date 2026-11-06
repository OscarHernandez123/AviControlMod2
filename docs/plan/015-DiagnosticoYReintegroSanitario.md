# Implementation Plan: Diagnóstico y Reintegro Sanitario

**Created**: 25/10/2026

**Last Updated**: 29/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [011-DiagnosticarGalpon.md](../specs/011-DiagnosticarGalpon.md)

## Summary

Registrar y consultar el diagnóstico clínico veterinario de un galpón en estado `AISLAMIENTO` con lote activo, conservar una instantánea (_snapshot_) histórica e inmutable de la enfermedad y de la medicación aplicadas, calcular automáticamente la fecha de reintegro cuando la enfermedad sea tratable y devolver automáticamente el galpón a estado `PRODUCTIVO` al cumplirse el plazo, siempre que continúe en `AISLAMIENTO`. Cuando la enfermedad diagnosticada requiera sacrificio sanitario (`requiereSacrificioSanitario == true`), el diagnóstico prescinde de medicación y de fecha de reintegro, habilitando la emisión de la orden de sacrificio sanitario total en el [Plan 008](008-GestionDelSacrificioSanitario.md). Asimismo, el plan incorpora la protección de precedencia mortal y la reconciliación idempotente del scheduler para blindar la bioseguridad y consistencia operativa.

## Technical Context

**Performance Goals**: El 95 % de los registros y consultas de diagnósticos responde en máximo 1 segundo; el proceso programado de reintegro (`ReintegroSanitarioScheduler`) detecta y procesa vencimientos de forma idempotente y acumulativa sin duplicar transiciones de estado.

**Constraints**:

- Acceso y autorización: solo `ROLE_VETERINARIO` registra diagnósticos (`403 Forbidden` a otros roles). La consulta de diagnósticos (`GET`) está permitida a `ROLE_VETERINARIO` y `ROLE_ADMINISTRADOR`.
- Precondición de aislamiento: el galpón debe encontrarse obligatoriamente en estado vigente `AISLAMIENTO` en el Módulo 1 y contar con un lote activo alojado.
- Bifurcación clínica estricta:
  - Si `requiereSacrificioSanitario == false` (enfermedad tratable): exige seleccionar una `Medicacion` existente y compatible con esa enfermedad en el [Plan 006](006-GestionDeMedicaciones.md), y calcula `fechaReintegro = fechaDiagnostico + diasMedicacion` (`LocalDate` en zona `America/Bogota`).
  - Si `requiereSacrificioSanitario == true` (enfermedad mortal): prohíbe asociar medicación (`medicacionId = null`), deja `fechaReintegro = null` y expone `habilitaSacrificioSanitario = true` para el [Plan 008](008-GestionDelSacrificioSanitario.md).
- Reintegro seguro sin sobrescritura: al cumplirse `hoy >= fechaReintegro`, el scheduler consulta nuevamente el estado del galpón en el Módulo 1; solo solicita el cambio `AISLAMIENTO → PRODUCTIVO` si el galpón continúa en `AISLAMIENTO`. Si el galpón cambió a otro estado (ej. `VACIADO_SANITARIO`), no sobrescribe el estado del Módulo 1.
- Lock del scheduler según topología de despliegue: Si el servicio sanitario se despliega con **una sola réplica**, aplicar `@DisallowConcurrentExecution` de Spring en `ReintegroSanitarioScheduler`. Si se despliega con **múltiples réplicas**, aplicar `ShedLock` (`@SchedulerLock`) o `pg_try_advisory_lock` de PostgreSQL, e incluir la tabla de locks correspondiente en la migración Flyway. La decisión debe quedar registrada por el equipo de infraestructura antes de T018.
- Idempotencia, concurrencia e inmutabilidad: el endpoint `POST` exige la cabecera `X-Idempotency-Key`, la entidad incluye `@Version` (`version`) y se prohíbe el borrado físico (`DELETE`).

**Scale/Scope**: SPEC-011; dos historias principales (registrar/consultar diagnóstico con snapshot clínico y procesar reintegros vencidos automáticamente) más el cierre reactivo por sacrificio sanitario; tres endpoints REST (`1 POST` y `2 GET`), un scheduler idempotente y un listener de eventos internos de Spring Modulith.

**Dependencias funcionales**:

- **[Plan 005](005-CatalogoDeEnfermedades.md)**: consulta de enfermedad y extracción de su snapshot (`nombre`, `requiereSacrificioSanitario`).
- **[Plan 006](006-GestionDeMedicaciones.md)**: validación de compatibilidad de la medicación con la enfermedad, extracción del snapshot terapéutico (`medicamentoId`, `medicamentoNombre`, `dosis`, `diasTotales`, `descripcion`) y consumidor de diagnósticos activos para la consulta diaria (`SPEC-015`).
- **[Plan 014](014-SolicitudesYValidacionDeAislamiento.md)**: produce el estado `AISLAMIENTO` validado previo al diagnóstico.
- **[Plan 008](008-GestionDelSacrificioSanitario.md)**: consulta el diagnóstico mortal para emitir la orden de sacrificio sanitario y publica `SacrificioSanitarioEjecutadoEvent` para cerrar el diagnóstico como `CERRADO_POR_SACRIFICIO`.
- **[Plan 012](012-RegistrarConsumoDeMedicamentos.md)**: consulta el diagnóstico activo con medicación para registrar consumos reales en campo.
- **Módulo 1 / [Plan 001](001-ConsultaDeGalpones.md)**: mantiene el estado autoritativo del galpón, el lote activo y ejecuta la transición `AISLAMIENTO → PRODUCTIVO`[cite: 4, 6, 7].
- **[General.md](General.md)**: seguridad, actor autenticado, `Clock` (`America/Bogota`), idempotencia, errores RFC 9457 y eventos internos de Spring Modulith.

### Decisiones específicas

1. **Snapshot Clínico y Terapéutico Completo en `DiagnosticoGalpon`**:
   `DiagnosticoGalpon` conserva las referencias `galponId`, `loteId`, `enfermedadId` y opcionalmente `medicacionId`, copiando de forma inmutable al confirmar:
   - Datos del galpón y lote: `galponNombreHistorico`, `loteNombreHistorico`.
   - Datos de la enfermedad (Plan 005): `nombreEnfermedadHistorico`, `requiereSacrificioHistorico`.
   - Datos de la medicación (Plan 006, cuando `requiereSacrificioHistorico == false`): `medicamentoIdHistorico`, `medicamentoNombreHistorico`, `dosisHistorica`, `viaAdministracionHistorica`, `descripcionHistorica` y `diasMedicacion`.
     Esto garantiza que los Planes `006` y `012` obtengan toda la información histórica desde el diagnóstico sin verse afectados por ediciones futuras en los catálogos.
2. **Validación en Tiempo Real del Estado `AISLAMIENTO`**:
   Antes de persistir el diagnóstico, el caso de uso verifica síncronamente contra el Módulo 1 que el galpón esté en `AISLAMIENTO` y tenga un lote activo alojado.
3. **Cálculo Exacto de Reintegro para Enfermedades Tratables**:
   Cuando `requiereSacrificioHistorico == false`, se valida que `medicacionId` no sea nulo y que su `enfermedadId` coincida con la enfermedad diagnosticada (`409 Conflict` si es incompatible). La `fechaDiagnostico` se toma del `Clock` en zona `America/Bogota` y se calcula `fechaReintegro = fechaDiagnostico.plusDays(diasMedicacion)`.
4. **Rama Mortal y Cierre por Sacrificio Sanitario (`Plan 008`)**:
   Cuando `requiereSacrificioHistorico == true`, `medicacionId`, `medicamentoIdHistorico` y `fechaReintegro` se guardan en `null` y la respuesta incluye `habilitaSacrificioSanitario = true`. Cuando el Plan 008 confirma la ejecución del sacrificio y publica el evento interno `SacrificioSanitarioEjecutadoEvent`, este plan lo escucha mediante `SacrificioSanitarioEjecutadoEventListener` y actualiza el estado del diagnóstico a `CERRADO_POR_SACRIFICIO`.
5. **Ejecución Idempotente del Scheduler de Reintegro (`AISLAMIENTO → PRODUCTIVO`)**:
   `ReintegroSanitarioScheduler` busca periódicamente diagnósticos en estado `ACTIVO` con `requiereSacrificioHistorico == false` y `fechaReintegro <= hoy`. Por cada uno:
   - Consulta el estado actual del galpón en el Módulo 1.
   - Antes de invocar `CambiarEstadoOperativoGalponPort` para reintegrar, el caso de uso verifica que no exista ningún otro diagnóstico `ACTIVO` con `requiereSacrificioHistorico == true` para el mismo `galponId`. Si existe, el diagnóstico tratable vencido se marca como `CERRADO_POR_REINTEGRO` **sin** invocar la transición al Módulo 1, dejando el galpón en `AISLAMIENTO` a la espera del sacrificio sanitario del Plan 008.
   - Si el galpón continúa en `AISLAMIENTO` y no existen otros diagnósticos `ACTIVO` con fecha de reintegro futura para ese mismo galpón, invoca `CambiarEstadoOperativoGalponPort.cambiarEstado(galponId, AISLAMIENTO, PRODUCTIVO)` y marca el diagnóstico como `CERRADO_POR_REINTEGRO`.
   - Si el galpón ya no está en `AISLAMIENTO`, cierra el diagnóstico sin alterar el estado vigente en el Módulo 1.
   - Si el Módulo 1 confirma la transición `AISLAMIENTO → PRODUCTIVO` pero falla el commit local del estado `CERRADO_POR_REINTEGRO`, el siguiente ciclo del scheduler encontrará el diagnóstico aún `ACTIVO`; al consultar el Módulo 1 verá el galpón ya en `PRODUCTIVO` (distinto de `AISLAMIENTO`) y cerrará el diagnóstico sin invocar nuevamente el puerto. Esto garantiza convergencia eventual sin Saga ni Outbox.
6. **Ausencia de Sobre-Ingeniería (YAGNI) y Autonomía de Despliegue (_Fallback_)**:
   - No se requieren tópicos externos de Kafka en este plan, ya que el Módulo 3 no liquida costos por el solo hecho de diagnosticar (liquida consumos del Plan 012 o sacrificios del Plan 008). Se utilizan eventos internos de Spring Modulith (`DiagnosticoGalponRegistradoEvent`, `GalponReintegradoEvent`).
   - Se exponen los endpoints `GET /api/galpones/{galponId}/diagnosticos` y `GET /api/diagnosticos/{diagnosticoId}` (`SPEC-011 FR-012`) y adaptadores con respaldo (_fallback/seed_) para pruebas locales autónomas.

### Alcance documental

- Este plan implementa [SPEC-011](../specs/011-DiagnosticarGalpon.md): registro y consulta de diagnóstico, snapshot histórico, reintegro automático y habilitación de sacrificio sanitario.
- El [Plan 005](005-CatalogoDeEnfermedades.md) mantiene el catálogo de enfermedades.
- El [Plan 006](006-GestionDeMedicaciones.md) mantiene el catálogo de medicaciones y consulta los diagnósticos activos para la hoja diaria del trabajador.
- El [Plan 014](014-SolicitudesYValidacionDeAislamiento.md) gestiona la solicitud y validación que coloca al galpón en `AISLAMIENTO`.
- El [Plan 008](008-GestionDelSacrificioSanitario.md) emite y confirma la orden de sacrificio sanitario habilitada por este plan.
- El [Plan 012](012-RegistrarConsumoDeMedicamentos.md) registra los consumos físicos de medicamento a partir de los diagnósticos tratables de este plan.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 011-DiagnosticarGalpon.md
└── plan/
    └── 015-DiagnosticoYReintegroSanitario.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/sanitario/diagnostico/
│   │   ├── DiagnosticoGalpon.java
│   │   └── EstadoDiagnostico.java
│   ├── event/sanitario/diagnostico/
│   │   ├── DiagnosticoGalponRegistradoEvent.java
│   │   └── GalponReintegradoEvent.java
│   ├── exception/sanitario/diagnostico/
│   │   ├── DiagnosticoNoEncontradoException.java
│   │   ├── GalponNoAisladoParaDiagnosticoException.java
│   │   ├── MedicacionRequeridaParaEnfermedadTratableException.java
│   │   ├── MedicacionIncompatibleConEnfermedadException.java
│   │   ├── MedicacionNoPermitidaEnEnfermedadMortalException.java
│   │   └── DiagnosticoActivoDuplicadoException.java
│   └── port/out/sanitario/diagnostico/
│       ├── DiagnosticoGalponRepositoryPort.java
│       ├── EnfermedadQueryPort.java
│       ├── MedicacionQueryPort.java
│       ├── GalponQueryPort.java
│       ├── LoteQueryPort.java
│       ├── CambiarEstadoOperativoGalponPort.java
│       └── IntegrationEventPublisherPort.java
├── application/sanitario/diagnostico/
│   ├── RegistrarDiagnosticoUseCase.java
│   ├── ConsultarDiagnosticosUseCase.java
│   ├── ProcesarReintegrosVencidosUseCase.java
│   ├── CerrarDiagnosticoPorSacrificioUseCase.java
│   ├── command/
│   │   └── RegistrarDiagnosticoCommand.java
│   └── result/
│       └── DiagnosticoGalponResult.java
└── infrastructure/
    ├── adapter/in/
    │   ├── rest/sanitario/diagnostico/
    │   │   ├── DiagnosticoController.java
    │   │   ├── dto/
    │   │   │   ├── RegistrarDiagnosticoRequest.java
    │   │   │   └── DiagnosticoGalponResponse.java
    │   │   └── mapper/DiagnosticoRestMapper.java
    │   ├── scheduling/
    │   │   └── ReintegroSanitarioScheduler.java
    │   └── event/sanitario/diagnostico/
    │       └── SacrificioSanitarioEjecutadoEventListener.java
    ├── adapter/out/
    │   ├── internal/sanitario/diagnostico/
    │   │   ├── EnfermedadDiagnosticoQueryAdapter.java
    │   │   ├── MedicacionDiagnosticoQueryAdapter.java
    │   │   └── Modulo1DiagnosticoAdapter.java
    │   └── persistence/sanitario/diagnostico/
    │       ├── entity/DiagnosticoGalponEntity.java
    │       ├── repository/SpringDataDiagnosticoGalponRepository.java
    │       ├── mapper/DiagnosticoPersistenceMapper.java
    │       └── DiagnosticoPersistenceAdapter.java
    └── config/
        └── DiagnosticoBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/sanitario/diagnostico/
│   └── DiagnosticoGalponTest.java
├── application/sanitario/diagnostico/
│   ├── RegistrarDiagnosticoUseCaseTest.java
│   ├── ConsultarDiagnosticosUseCaseTest.java
│   └── ProcesarReintegrosVencidosUseCaseTest.java
├── infrastructure/adapter/in/rest/sanitario/diagnostico/
│   └── DiagnosticoControllerTest.java
└── integration/sanitario/diagnostico/
    └── DiagnosticoReintegroIntegrationTest.java
```

**Structure Decision**: El diagnóstico y su snapshot histórico son propiedad exclusiva de este plan. `ConsultarDiagnosticosUseCase` expone las consultas internas que consumen los Planes `006`, `008` y `012`. `SacrificioSanitarioEjecutadoEventListener` escucha el evento del Plan 008 para cerrar el diagnóstico cuando se ejecuta un sacrificio sanitario, y `ReintegroSanitarioScheduler` dispara el caso de uso de reintegro sin contener lógica de negocio.

### Entidades y relación

```text
Galpon (AISLAMIENTO) + Lote activo ───< DiagnosticoGalpon
DiagnosticoGalpon ─── Enfermedad (snapshot obligatorio)
DiagnosticoGalpon ─── Medicacion (snapshot obligatorio si requiereSacrificioHistorico == false)

DiagnosticoGalpon (Aggregate Root)
├── id: UUID
├── galponId: UUID (Ref. Módulo 1)
├── galponNombreHistorico: String
├── loteId: UUID (Ref. Módulo 1)
├── loteNombreHistorico: String
├── enfermedadId: UUID (Ref. Plan 005)
├── nombreEnfermedadHistorico: String
├── requiereSacrificioHistorico: boolean
├── medicacionId: UUID (opcional, nulo si requiereSacrificioHistorico == true)
├── medicamentoIdHistorico: UUID (opcional, nulo si requiereSacrificioHistorico == true)
├── medicamentoNombreHistorico: String (opcional)
├── dosisHistorica: String (opcional)
├── viaAdministracionHistorica: String (opcional)
├── descripcionHistorica: String (opcional, null si requiereSacrificioHistorico == true)
├── diasMedicacion: Integer (opcional, > 0 si es tratable)
├── fechaDiagnostico: LocalDate (America/Bogota)
├── fechaReintegro: LocalDate (opcional, fechaDiagnostico + diasMedicacion)
├── veterinarioId: UUID
├── registradoEn: Instant (UTC)
├── estado: EstadoDiagnostico (ACTIVO, CERRADO_POR_REINTEGRO, CERRADO_POR_SACRIFICIO)
└── version: Integer (@Version para Optimistic Locking)
```

### Contratos de los puertos

| Puerto                              | Tipo                          | Responsabilidad                                                                                                                                                                                                                                                                             |
| ----------------------------------- | ----------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `DiagnosticoGalponRepositoryPort`   | Salida (Persistencia)         | Persistir diagnósticos con su snapshot completo, buscar por ID, listar por `galponId`/`loteId`/`estado`, consultar diagnósticos `ACTIVO` con `fechaReintegro <= hoy` pendientes de reintegro y verificar precedencia mortal mediante `boolean existsActiveMortalByGalponId(UUID galponId)`. |
| `EnfermedadQueryPort`               | Salida (Integración Plan 005) | Consultar por ID una enfermedad registrada y obtener su `nombre` y su indicador `requiereSacrificioSanitario` vigentes.                                                                                                                                                                     |
| `MedicacionQueryPort`               | Salida (Integración Plan 006) | Consultar por ID una medicación, verificar que su `enfermedadId` coincida con la enfermedad diagnosticada y obtener `medicamentoId`, `medicamentoNombre`, `dosis`, `diasTotales` y `descripcion`.                                                                                           |
| `GalponQueryPort` / `LoteQueryPort` | Salida (Integración Módulo 1) | Validar que el galpón esté en estado `AISLAMIENTO`, obtener su nombre y recuperar el lote activo alojado (`loteId`, `loteNombre`, `poblacionActual > 0`).                                                                                                                                   |
| `CambiarEstadoOperativoGalponPort`  | Salida (Integración Módulo 1) | Solicitar el cambio `AISLAMIENTO → PRODUCTIVO` al cumplirse la fecha de reintegro tras verificar que el galpón sigue aislado.                                                                                                                                                               |
| `IntegrationEventPublisherPort`     | Salida (Eventos Internos)     | Publicar `DiagnosticoGalponRegistradoEvent` y `GalponReintegradoEvent` en Spring Modulith tras confirmar la transacción local.                                                                                                                                                              |

### Contratos HTTP propuestos

| Método y ruta                                | Acceso                                   | Parámetros / Cabeceras                  | Responsabilidad                                                                                                 |
| -------------------------------------------- | ---------------------------------------- | --------------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| `POST /api/galpones/{galponId}/diagnosticos` | `ROLE_VETERINARIO`                       | Header obligatorio: `X-Idempotency-Key` | Registrar diagnóstico clínico, guardar snapshot y calcular fecha de reintegro o habilitar sacrificio.           |
| `GET /api/galpones/{galponId}/diagnosticos`  | `ROLE_VETERINARIO`, `ROLE_ADMINISTRADOR` | Query (opc): `?estado=ACTIVO`           | Listar los diagnósticos de un galpón para la UI y para selección en consumo (Plan 012) o sacrificio (Plan 008). |
| `GET /api/diagnosticos/{diagnosticoId}`      | `ROLE_VETERINARIO`, `ROLE_ADMINISTRADOR` | Path: `diagnosticoId`                   | Consultar el detalle completo y snapshot de un diagnóstico específico.                                          |

La ejecución del scheduler no expone endpoint público. Los errores siguen RFC 9457 (`application/problem+json`): datos faltantes `400`, rol no autorizado `403`, galpón/enfermedad/medicación inexistente `404`, estado de galpón distinto de `AISLAMIENTO`, medicación incompatible o diagnóstico activo duplicado para la misma enfermedad `409`, dependencia indisponible `503`.

### JSON común de errores

```json
{
  "type": "https://avicontrol/errors/estado-galpon-incompatible",
  "title": "Estado incompatible con el diagnóstico",
  "status": 409,
  "detail": "El galpón debe encontrarse en estado AISLAMIENTO para registrar el diagnóstico",
  "instance": "/api/galpones/550e8400-e29b-41d4-a716-446655440001/diagnosticos",
  "code": "GALPON_NO_AISLADO",
  "correlationId": "uuid",
  "fieldErrors": []
}
```

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Acordar los estados (`AISLAMIENTO`, `PRODUCTIVO`) y contratos de consulta/transición con el Módulo 1 (incluyendo respaldo autónomo _fallback_ para desarrollo local) y revisar la interacción con el Plan 014.
- [ ] T002 Acordar con el Plan 005 el contrato de `EnfermedadQueryPort` y los atributos que se copian al snapshot del diagnóstico.
- [ ] T003 Acordar con el Plan 006 el contrato de `MedicacionQueryPort` (para validar compatibilidad y copiar `medicamentoId`, `medicamentoNombre`, `dosis`, `diasTotales`, y aclarando que `MedicacionQueryPort` devuelve `descripcion` y se mapea a `descripcionHistorica` en el snapshot) y el contrato de lectura para las consultas diarias de `SPEC-015`.
- [ ] T004 Acordar con el Plan 008 la consulta de diagnóstico elegible para sacrificio y la escucha del evento interno `SacrificioSanitarioEjecutadoEvent`, y con el Plan 012 la consulta de diagnóstico para consumo de medicamentos.
- [ ] T005 Definir `Clock` (`America/Bogota`), intervalo del scheduler, filtro transversal `X-Idempotency-Key` y catálogo de errores RFC 9457 con [General.md](General.md).

**Checkpoint**: Contratos clínicos, snapshot completo, transición y reintegro acordados.

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T006 Implementar en Java puro el modelo `DiagnosticoGalpon` y el enum `EstadoDiagnostico` (`ACTIVO`, `CERRADO_POR_REINTEGRO`, `CERRADO_POR_SACRIFICIO`) con sus invariantes (exigir medicación si `requiereSacrificioHistorico == false`, prohibirla si es `true` y calcular `fechaReintegro`).
- [ ] T007 Definir las excepciones de dominio y los puertos `DiagnosticoGalponRepositoryPort`, `EnfermedadQueryPort`, `MedicacionQueryPort`, `GalponQueryPort`, `LoteQueryPort`, `CambiarEstadoOperativoGalponPort` e `IntegrationEventPublisherPort`.
- [ ] T008 Crear la migración Flyway usando la siguiente versión disponible del repositorio para la tabla `diagnostico_galpon`, incluyendo todas las columnas del snapshot (incluida `descripcion_historica TEXT`), columna `version`, índices por `galpon_id`, `lote_id`, `estado`, `fecha_reintegro` y restricción única parcial `(lote_id, enfermedad_id) WHERE estado = 'ACTIVO'`.
- [ ] T009 Implementar `DiagnosticoPersistenceAdapter` y los adaptadores internos hacia los Planes 001, 005 y 006 sin acoplarse a entidades JPA externas.
- [ ] T010 Configurar el scheduler reentrante e idempotente (`ReintegroSanitarioScheduler`), el listener `SacrificioSanitarioEjecutadoEventListener` y los límites transaccionales en `DiagnosticoBeanConfiguration`. Si el servicio sanitario se despliega con **una sola réplica**, aplicar `@DisallowConcurrentExecution` de Spring en `ReintegroSanitarioScheduler`. Si se despliega con **múltiples réplicas**, aplicar `ShedLock` (`@SchedulerLock`) o `pg_try_advisory_lock` de PostgreSQL, e incluir la tabla de locks correspondiente en la migración Flyway. La decisión debe quedar registrada por el equipo de infraestructura antes de T018.

**Checkpoint**: Modelo de dominio, snapshot y procesamiento temporal listos para las historias de usuario.

## Phase 3: User Story 1 — Registrar y consultar diagnóstico definiendo el resultado sanitario (Priority: P1)

**Spec**: SPEC-011, historia 1 y FR-012.

**Goal**: El veterinario registra la enfermedad sobre un galpón en `AISLAMIENTO` con lote activo, conservando el snapshot de la decisión clínica y terapéutica, y consulta los diagnósticos registrados del galpón.

**Independent Test**:

- **Caso Tratable (`requiereSacrificioSanitario == false`)**: Exige medicación compatible, copia el snapshot completo del tratamiento y calcula `fechaReintegro = fechaDiagnostico + diasMedicacion` conservando el galpón en `AISLAMIENTO`.
- **Caso Mortal (`requiereSacrificioSanitario == true`)**: No exige ni acepta medicación, deja `fechaReintegro = null` y devuelve `habilitaSacrificioSanitario = true`.
- **Rechazos**: Galpón que no está en `AISLAMIENTO`, falta de lote activo, medicación de otra enfermedad o usuario sin `ROLE_VETERINARIO` se rechazan sin crear el registro.

### Definición de endpoints REST para User Story 1

| Elemento       | Definición                                                                                                                                                                                                                                                                                                                                                          |
| -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Método y ruta  | `POST /api/galpones/{galponId}/diagnosticos`                                                                                                                                                                                                                                                                                                                        |
| Autorización   | `ROLE_VETERINARIO`                                                                                                                                                                                                                                                                                                                                                  |
| Cabeceras      | `X-Idempotency-Key`                                                                                                                                                                                                                                                                                                                                                 |
| Entrada (Path) | `galponId` (`UUID` obligatorio)                                                                                                                                                                                                                                                                                                                                     |
| Entrada (Body) | `enfermedadId` (`UUID` obligatorio), `medicacionId` (`UUID` obligatorio si la enfermedad es tratable; `null` si requiere sacrificio sanitario)                                                                                                                                                                                                                      |
| Respuesta 201  | `DiagnosticoGalponResponse` con el snapshot completo, `fechaReintegro` (si aplica) y bandera `habilitaSacrificioSanitario`                                                                                                                                                                                                                                          |
| Errores        | `400` por datos faltantes; `403` por rol; `404` por galpón, enfermedad o medicación inexistente; `409` si el galpón no está en `AISLAMIENTO`, si falta medicación en enfermedad tratable, si la medicación es incompatible, si se envía medicación en enfermedad mortal o si ya existe diagnóstico activo para esa misma enfermedad; `503` si una dependencia falla |

| Elemento      | Definición                                                                                             |
| ------------- | ------------------------------------------------------------------------------------------------------ |
| Método y ruta | `GET /api/galpones/{galponId}/diagnosticos` y `GET /api/diagnosticos/{diagnosticoId}`                  |
| Autorización  | `ROLE_VETERINARIO`, `ROLE_ADMINISTRADOR`                                                               |
| Parámetros    | En listado por galpón: query param opcional `?estado=ACTIVO`. En detalle: `diagnosticoId` en ruta.     |
| Respuesta 200 | Lista o detalle de `DiagnosticoGalponResponse` con todos los datos del snapshot clínico y terapéutico. |

#### JSON de solicitud — Caso Enfermedad Tratable

```json
{
  "enfermedadId": "11111111-2222-3333-4444-555555555555",
  "medicacionId": "66666666-7777-8888-9999-000000000000"
}
```

#### JSON de respuesta exitosa (Response 201 — Caso Enfermedad Tratable)

```json
{
  "diagnosticoId": "d1a2g3n4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "galponId": "550e8400-e29b-41d4-a716-446655440005",
  "galponNombre": "Galpón 5",
  "loteId": "2fc03a21-84c4-4c83-bb15-6607bb75cb90",
  "loteNombre": "LDP-005",
  "enfermedadId": "11111111-2222-3333-4444-555555555555",
  "nombreEnfermedad": "Coccidiosis Aviar",
  "requiereSacrificioSanitario": false,
  "habilitaSacrificioSanitario": false,
  "medicacionId": "66666666-7777-8888-9999-000000000000",
  "medicamentoId": "99999999-8888-7777-6666-555555555555",
  "medicamentoNombre": "Amprolio 20% Solución",
  "dosis": "200 g diluidos en 100 L de agua potable",
  "viaAdministracion": "Agua de bebida en bebederos automáticos",
  "descripcionHistorica": "Administrar continuamente en el agua de bebida durante 5 días, renovando la preparación cada 24 horas",
  "diasMedicacion": 5,
  "fechaDiagnostico": "2026-09-04",
  "fechaReintegro": "2026-09-09",
  "estado": "ACTIVO",
  "veterinarioId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "registradoEn": "2026-09-04T15:00:00Z",
  "version": 1
}
```

#### JSON de solicitud — Caso Enfermedad Mortal (Sacrificio Sanitario)

```json
{
  "enfermedadId": "22222222-3333-4444-5555-666666666666",
  "medicacionId": null
}
```

#### JSON de respuesta exitosa (Response 201 — Caso Enfermedad Mortal)

```json
{
  "diagnosticoId": "b1a2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "galponId": "550e8400-e29b-41d4-a716-446655440003",
  "galponNombre": "Galpón 3",
  "loteId": "3ab04b32-95d5-5d94-cc26-7718cc86dc01",
  "loteNombre": "LDP-003",
  "enfermedadId": "22222222-3333-4444-5555-666666666666",
  "nombreEnfermedad": "Influenza Aviar",
  "requiereSacrificioSanitario": true,
  "habilitaSacrificioSanitario": true,
  "medicacionId": null,
  "medicamentoId": null,
  "medicamentoNombre": null,
  "dosis": null,
  "viaAdministracion": null,
  "descripcionHistorica": null,
  "diasMedicacion": null,
  "fechaDiagnostico": "2026-10-05",
  "fechaReintegro": null,
  "estado": "ACTIVO",
  "veterinarioId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "registradoEn": "2026-10-05T14:15:00Z",
  "version": 1
}
```

### Tests e implementación

- [ ] T011 [US1] Probar autorización exclusiva `ROLE_VETERINARIO`, validación de estado `AISLAMIENTO`, lote activo y enfermedad existente.
- [ ] T012 [US1] Probar enfermedad tratable: obligatoriedad y compatibilidad de medicación, captura del snapshot completo (`medicamentoId`, `medicamentoNombre`, `dosis`, `viaAdministracion`, `descripcionHistorica`, `diasMedicacion`) y cálculo exacto de `fechaReintegro`.
- [ ] T013 [US1] Probar enfermedad mortal (`requiereSacrificioSanitario == true`): rechazo si se envía medicación, `fechaReintegro = null` y `habilitaSacrificioSanitario = true`.
- [ ] T014 [US1] Probar idempotencia con `X-Idempotency-Key`, consulta de diagnósticos (`GET`) y cierre reactivo a `CERRADO_POR_SACRIFICIO` al recibir `SacrificioSanitarioEjecutadoEvent`.
- [ ] T015 [US1] Implementar `RegistrarDiagnosticoUseCase`, `ConsultarDiagnosticosUseCase`, `CerrarDiagnosticoPorSacrificioUseCase`, DTOs, mappers y endpoints `POST` y `GET` en `DiagnosticoController`.

**Checkpoint**: Los diagnósticos reflejan la decisión clínica de forma inmutable sin verse afectados por cambios posteriores en los catálogos fuente.

## Phase 4: User Story 2 — Procesar reintegros vencidos automáticamente (Priority: P1)

**Spec**: SPEC-011, historia 1 (escenarios 3 y 4) y FR-008 a FR-011.

**Goal**: Al alcanzarse o superarse la `fechaReintegro` de un diagnóstico tratable activo, devolver automáticamente a estado `PRODUCTIVO` únicamente a los galpones que continúen en estado `AISLAMIENTO`.

**Independent Test**: Un diagnóstico vencido (`fechaReintegro <= hoy`) cuyo galpón continúa en `AISLAMIENTO` transiciona una sola vez a `PRODUCTIVO` y el diagnóstico pasa a `CERRADO_POR_REINTEGRO`; si la fecha aún no se cumple (`hoy < fechaReintegro`), el galpón permanece en `AISLAMIENTO`; si el galpón ya cambió a otro estado (ej. `VACIADO_SANITARIO`), no se sobrescribe su estado en el Módulo 1; tras una caída del servicio, el scheduler procesa de forma acumulativa e idempotente los reintegros vencidos pendientes.

### Definición del proceso programado

`ReintegroSanitarioScheduler` invoca `ProcesarReintegrosVencidosUseCase` tomando la fecha actual del `Clock` (`America/Bogota`). El caso de uso obtiene los diagnósticos en estado `ACTIVO` con `fechaReintegro <= hoy`, verifica en el Módulo 1 el estado vigente del galpón y la ausencia de diagnósticos mortales activos (`existsActiveMortalByGalponId`), y solicita la transición `AISLAMIENTO → PRODUCTIVO` cuando corresponda, marcando el diagnóstico como `CERRADO_POR_REINTEGRO` bajo control `@Version`.

### Tests e implementación

- [ ] T016 [US2] Probar evaluación de vencimiento con `Clock` en zona `America/Bogota`, permanencia en `AISLAMIENTO` antes de la fecha de reintegro, y escenario de precedencia: diagnóstico mortal `ACTIVO` + diagnóstico tratable vencido sobre el mismo galpón → no se reintegra; el tratable se cierra como `CERRADO_POR_REINTEGRO` sin transición remota.
- [ ] T017 [US2] Probar idempotencia del scheduler, procesamiento acumulativo tras indisponibilidad temporal, no sobrescritura cuando el galpón tiene un estado distinto de `AISLAMIENTO` o pasó a `CERRADO_POR_SACRIFICIO`, y escenario de reconciliación: falla de commit local tras transición remota exitosa → siguiente ciclo cierra el diagnóstico sin doble transición.
- [ ] T018 [US2] Implementar consulta de vencidos en repositorio, `ProcesarReintegrosVencidosUseCase` y `ReintegroSanitarioScheduler`.
- [ ] T019 [US2] Publicar el evento interno `GalponReintegradoEvent` únicamente después de confirmar la transición a `PRODUCTIVO`.

**Checkpoint**: Los reintegros vencidos se procesan exactamente una vez, respetan siempre el estado autoritativo del Módulo 1 y garantizan 0 % de reintegros ejecutados sobre galpones con diagnóstico mortal `ACTIVO` pendiente.

## Phase 5: Polish & Cross-Cutting Concerns

- [ ] T020 Documentar los tres endpoints (`POST` y `2 GET`), cabecera `X-Idempotency-Key`, estados del diagnóstico y errores RFC 9457 en OpenAPI 3.
- [ ] T021 Completar `DiagnosticoReintegroIntegrationTest` con PostgreSQL real verificando la integración con los Planes 005, 006, 008, 012 y 014.
- [ ] T022 Verificar reglas de arquitectura con ArchUnit, concurrencia optimista (`@Version`) y latencia `p95 < 1 segundo` conforme a [General.md](General.md).

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)** precede a **Foundational (Phase 2)**.
- **Foundational (Phase 2)** habilita el registro/consulta de diagnósticos y el scheduler de reintegro.
- **US1 (Phase 3)** depende del estado `AISLAMIENTO` validado por el Plan 014 y de los puertos de consulta de los Planes 005 y 006.
- **US2 (Phase 4)** depende de los diagnósticos tratables activos registrados en **US1**.
- **Polish (Phase 5)** depende de ambos flujos y verifica la integración transversal.

### Dependencias con otros planes

- **[Plan 005](005-CatalogoDeEnfermedades.md)**: Provee el catálogo de enfermedades y el indicador `requiereSacrificioSanitario`.
- **[Plan 006](006-GestionDeMedicaciones.md)**: Provee la validación de compatibilidad y duración de la medicación, y consume los diagnósticos activos para la consulta diaria (`SPEC-015`).
- **[Plan 014](014-SolicitudesYValidacionDeAislamiento.md)**: Establece el estado `AISLAMIENTO` requerido para poder diagnosticar.
- **[Plan 008](008-GestionDelSacrificioSanitario.md)**: Consume el diagnóstico con `requiereSacrificioHistorico == true` para emitir la orden de sacrificio sanitario y emite `SacrificioSanitarioEjecutadoEvent` para cerrarlo como `CERRADO_POR_SACRIFICIO`.
- **[Plan 012](012-RegistrarConsumoDeMedicamentos.md)**: Consume el diagnóstico activo con medicación para registrar los consumos reales en campo.
- **Módulo 1 / [Plan 001](001-ConsultaDeGalpones.md)**: Propietario de galpones, lotes y de la transición operativa `AISLAMIENTO → PRODUCTIVO`.
- **[General.md](General.md)**: Seguridad, reloj compartido, idempotencia, eventos internos, errores RFC 9457 y transacciones.

### Dentro de cada User Story

- Consultar el estado vigente del galpón y de los catálogos justo antes de escribir.
- Guardar el snapshot clínico y terapéutico completo dentro de la transacción local.
- Reintentar reintegros vencidos de forma idempotente sin duplicar transiciones en el Módulo 1.

## Notes

- Este plan no solicita ni valida el aislamiento inicial (Plan 014), no registra enfermedades (Plan 005), no descuenta inventario de medicamentos (Plan 012) y no emite órdenes de sacrificio sanitario (Plan 008).
- El estado del galpón solo se modifica mediante el puerto público hacia el Módulo 1.
- Una enfermedad con `requiereSacrificioSanitario == true` nunca produce fecha de reintegro a producción.
- Las tareas `T001` a `T022` cubren la totalidad del [SPEC-011](../specs/011-DiagnosticarGalpon.md).
- **Mejoras post-MVP**: Como mejoras evolutivas que **no bloquean el despliegue del MVP**, se contempla incorporar paginación (`page`/`size`) en el listado `GET /api/galpones/{galponId}/diagnosticos` y añadir el test unitario aislado `SacrificioSanitarioEjecutadoEventListenerTest.java` de forma complementaria a la cobertura de integración ya establecida en `T014`.

---

## Registro de cambios aplicados

- **Ajuste A (snapshot `descripcionHistorica`)**: agregado en secciones `Decisiones específicas #1`, `Entidades y relación` (`DiagnosticoGalpon`), tareas `T003`, `T008` (`descripcion_historica TEXT`), `T012` y bloques `JSON de respuesta exitosa` (tratable y mortal).
- **Ajuste B (precedencia mortal)**: agregado en `Decisiones específicas #5`, tabla `Contratos de los puertos` (`DiagnosticoGalponRepositoryPort.existsActiveMortalByGalponId`), `Definición del proceso programado` y tarea `T016`.
- **Ajuste C (lock condicional según topología)**: agregado en sección `Technical Context (Constraints)` y tarea `T010` (`@DisallowConcurrentExecution` para réplica única vs `ShedLock`/`pg_try_advisory_lock` para múltiples réplicas antes de `T018`).
- **Ajuste D (reconciliación idempotente sin Saga/Outbox)**: agregado en `Decisiones específicas #5` y tarea `T017`.
- **Ajuste E (mejoras post-MVP no bloqueantes)**: agregado en sección `Notes` (paginación en `GET` de diagnósticos y `SacrificioSanitarioEjecutadoEventListenerTest.java`).
- **Ajuste F (metadatos y criterio de éxito)**: actualizado `Last Updated` (`29/10/2026`), ampliado `Summary` y añadido criterio de `0 % de reintegros ejecutados sobre galpones con diagnóstico mortal ACTIVO pendiente` en `Checkpoint` de `Phase 4`.
