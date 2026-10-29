# Implementation Plan: Solicitudes y Validación de Aislamiento

**Created**: 25/10/2026

**Last Updated**: 29/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [020-SolicitarValidacionParaAislarGalpon.md](../specs/020-SolicitarValidacionParaAislarGalpon.md)
- [010-ValidarAislamiento.md](../specs/010-ValidarAislamiento.md)

## Summary

Permitir que los trabajadores u operarios envíen solicitudes inmutables de validación de aislamiento para cualquier galpón en estado `PRODUCTIVO` con lote activo (`poblacionActual > 0`), impedir solicitudes pendientes duplicadas para un mismo galpón, y permitir que los veterinarios consulten la bandeja de solicitudes pendientes y aprueben o rechacen el aislamiento.

Crear o consultar una solicitud nunca modifica el estado operativo del galpón: únicamente una decisión veterinaria aprobatoria (`APROBADA`) solicita de forma atómica al Módulo 1 la transición del galpón de `PRODUCTIVO` a `AISLAMIENTO`, habilitando el diagnóstico clínico posterior en el [Plan 015](015-DiagnosticoYReintegroSanitario.md). Si el veterinario rechaza la solicitud (`RECHAZADA`), se registra el dictamen con sus observaciones y el galpón conserva su estado `PRODUCTIVO`.

## Technical Context

**Performance Goals**: El 95 % de las creaciones de solicitudes, consultas paginadas de bandeja y decisiones de validación/rechazo responde en máximo 1 segundo.

**Constraints**:

- Segregación estricta de roles:
  - Crear solicitud (`POST /api/galpones/{galponId}/solicitudes-aislamiento`): exclusivo para `ROLE_TRABAJADOR` y `ROLE_ADMINISTRADOR` (sin requerir asignación previa trabajador-galpón).
  - Consultar solicitudes (`GET /api/solicitudes-aislamiento`): permitido para `ROLE_VETERINARIO`, `ROLE_ADMINISTRADOR` y `ROLE_TRABAJADOR` (para visualizar la bandeja o verificar si un galpón ya tiene solicitud en trámite).
  - Evaluar y decidir aislamiento (`POST /api/solicitudes-aislamiento/{solicitudId}/validacion`): exclusivo para `ROLE_VETERINARIO` (`403 Forbidden` a cualquier otro rol).
- Condición de creación: el galpón debe estar en estado `PRODUCTIVO` y contar con un lote activo cuya `poblacionActual > 0`.
- Unicidad de solicitud pendiente: no puede existir más de una solicitud en estado `PENDIENTE_VALIDACION` simultáneamente para el mismo galpón (`409 Conflict`). Una vez resuelta en estado terminal (`APROBADA` o `RECHAZADA`), el galpón puede recibir una solicitud futura si vuelve a cumplir las reglas.
- Revalidación en tiempo real y Atomicidad: antes de aprobar el aislamiento, el caso de uso vuelve a consultar en el Módulo 1 que el galpón continúe en estado `PRODUCTIVO`. La persistencia de la decisión, el cambio de estado de la solicitud y la transición `PRODUCTIVO → AISLAMIENTO` en el Módulo 1 se ejecutan dentro de una misma transacción local (`@Transactional`); si el Módulo 1 falla o el estado cambió, se realiza _rollback_ completo.
- Idempotencia, concurrencia e inmutabilidad: las operaciones `POST` soportan `X-Idempotency-Key` y `SolicitudAislamiento` incluye `@Version` (`version`). Las solicitudes enviadas y las decisiones emitidas son inmutables y se prohíbe el borrado físico (`DELETE`).

**Scale/Scope**: SPEC-020 y SPEC-010; tres historias de usuario (crear solicitud inmutable, consultar bandeja / prevenir duplicados, y aprobar o rechazar aislamiento); tres endpoints REST (`2 POST` y `1 GET`); un agregado raíz (`SolicitudAislamiento`) con su entidad/valor de decisión (`DecisionAislamiento`); integración por puertos internos con el Módulo 1 / [Plan 001](001-ConsultaDeGalpones.md).

**Dependencias funcionales**:

- **Módulo 1 / [Plan 001](001-ConsultaDeGalpones.md)**: fuente autoritativa para consultar el galpón (`GalponQueryPort`), el lote activo con su población y edad (`LoteQueryPort`), y ejecutar el cambio de estado operativo (`CambiarEstadoOperativoGalponPort`).
- **[Plan 013](013-GestionDeAlertasSanitarias.md)**: gestiona las alertas por mortalidad anormal (las cuales aportan contexto clínico al veterinario, pero no sustituyen la solicitud requerida por SPEC-010).
- **[Plan 015](015-DiagnosticoYReintegroSanitario.md)**: consume los galpones ya puestos en estado `AISLAMIENTO` para registrar el diagnóstico clínico.
- **[General.md](General.md)**: seguridad, actor autenticado, `Clock` (`America/Bogota` / UTC), idempotencia, errores RFC 9457 y eventos internos de Spring Modulith.

### Decisiones específicas

1. **Snapshot Operativo en `SolicitudAislamiento`**: Al registrar la solicitud, el servidor consulta el Módulo 1 y captura de forma inmutable `galponId`, `galponNombre`, `loteId`, `loteNombre`, `poblacionActual` y `edadLoteDias`, junto con el `motivo`, los `signosObservados`, el `trabajadorId`, el instante UTC `emitidaEn`, el estado inicial `PENDIENTE_VALIDACION` y `version = 1`.
2. **Candado en Base de Datos contra Duplicados Concurrentes**: La unicidad de solicitud pendiente por galpón se garantiza mediante un índice único parcial en PostgreSQL (`CREATE UNIQUE INDEX uk_solicitud_pendiente_galpon ON solicitud_aislamiento (galpon_id) WHERE estado = 'PENDIENTE_VALIDACION'`), mapeando cualquier colisión concurrente a `409 Conflict` (`SOLICITUD_PENDIENTE_DUPLICADA`).
3. **Inmutabilidad del Galpón al Solicitar**: Crear o listar solicitudes jamás invoca `CambiarEstadoOperativoGalponPort`. La transición `PRODUCTIVO → AISLAMIENTO` ocurre única y exclusivamente cuando un usuario con `ROLE_VETERINARIO` aprueba la solicitud.
4. **Doble Verificación y Rollback en la Validación Veterinaria**: Al ejecutar `ValidarAislamientoUseCase`, se verifica que la solicitud esté en `PENDIENTE_VALIDACION` (controlando colisiones entre dos veterinarios con `@Version`). Si la decisión es `APROBADA`, se reconsulta el estado del galpón en el Módulo 1; si ya no es `PRODUCTIVO`, se rechaza la operación con `409 Conflict` sin alterar el galpón. Si el puerto del Módulo 1 no está disponible, toda la transacción hace _rollback_ (`503 Service Unavailable`).
5. **Eventos Internos sin Sobre-Ingeniería (YAGNI)**: Al confirmarse la decisión veterinaria, se publica el evento interno de Spring Modulith (`AislamientoValidadoEvent` o `AislamientoRechazadoEvent`) después del _commit_ local. No se requiere publicar a Kafka hacia el Módulo 3 porque el aislamiento preventivo no genera movimientos financieros ni bajas contables.
6. **Autonomía de Despliegue (_Fallback_)**: Los adaptadores internos hacia `GalponQueryPort`, `LoteQueryPort` y `CambiarEstadoOperativoGalponPort` incluyen respaldo autónomo (_fallback/seed_) para pruebas locales, permitiendo que el equipo del actor Veterinario despliegue y verifique sus flujos sin bloquearse por pendientes en otras ramas.

### Alcance documental

- Este plan implementa [SPEC-020](../specs/020-SolicitarValidacionParaAislarGalpon.md) (creación de solicitud y control de duplicados pendientes) y [SPEC-010](../specs/010-ValidarAislamiento.md) (evaluación, aprobación o rechazo del aislamiento).
- El [Plan 005](005-CatalogoDeEnfermedades.md) mantiene el catálogo de enfermedades.
- El [Plan 015](015-DiagnosticoYReintegroSanitario.md) registra el diagnóstico clínico una vez que este plan ha dejado el galpón en estado `AISLAMIENTO`.
- El [Plan 013](013-GestionDeAlertasSanitarias.md) mantiene las alertas de mortalidad anormal de forma independiente.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   ├── 020-SolicitarValidacionParaAislarGalpon.md
│   └── 010-ValidarAislamiento.md
└── plan/
    └── 014-SolicitudesYValidacionDeAislamiento.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/sanitario/aislamiento/
│   │   ├── SolicitudAislamiento.java
│   │   ├── EstadoSolicitudAislamiento.java
│   │   ├── DecisionAislamiento.java
│   │   └── ResultadoValidacionAislamiento.java
│   ├── event/sanitario/aislamiento/
│   │   ├── AislamientoValidadoEvent.java
│   │   └── AislamientoRechazadoEvent.java
│   ├── exception/sanitario/aislamiento/
│   │   ├── SolicitudAislamientoNoEncontradaException.java
│   │   ├── SolicitudPendienteDuplicadaException.java
│   │   ├── SolicitudAislamientoYaResueltaException.java
│   │   ├── EstadoGalponIncompatibleAislamientoException.java
│   │   └── LoteActivoRequeridoParaAislamientoException.java
│   └── port/out/sanitario/aislamiento/
│       ├── SolicitudAislamientoRepositoryPort.java
│       ├── GalponQueryPort.java
│       ├── LoteQueryPort.java
│       ├── CambiarEstadoOperativoGalponPort.java
│       └── IntegrationEventPublisherPort.java
├── application/sanitario/aislamiento/
│   ├── CrearSolicitudAislamientoUseCase.java
│   ├── ConsultarSolicitudesAislamientoUseCase.java
│   ├── ValidarAislamientoUseCase.java
│   ├── command/
│   │   ├── CrearSolicitudAislamientoCommand.java
│   │   └── ValidarAislamientoCommand.java
│   └── result/
│       ├── SolicitudAislamientoResult.java
│       └── ValidacionAislamientoResult.java
└── infrastructure/
    ├── adapter/in/rest/sanitario/aislamiento/
    │   ├── AislamientoController.java
    │   ├── dto/
    │   │   ├── CrearSolicitudAislamientoRequest.java
    │   │   ├── ValidarAislamientoRequest.java
    │   │   ├── SolicitudAislamientoResponse.java
    │   │   └── ValidacionAislamientoResponse.java
    │   └── mapper/AislamientoRestMapper.java
    ├── adapter/out/
    │   ├── internal/sanitario/aislamiento/
    │   │   └── Modulo1AislamientoAdapter.java
    │   └── persistence/sanitario/aislamiento/
    │       ├── entity/SolicitudAislamientoEntity.java
    │       ├── repository/SpringDataSolicitudAislamientoRepository.java
    │       ├── mapper/SolicitudAislamientoPersistenceMapper.java
    │       └── SolicitudAislamientoPersistenceAdapter.java
    └── config/
        └── AislamientoBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/sanitario/aislamiento/
│   └── SolicitudAislamientoTest.java
├── application/sanitario/aislamiento/
│   ├── CrearSolicitudAislamientoUseCaseTest.java
│   ├── ConsultarSolicitudesAislamientoUseCaseTest.java
│   └── ValidarAislamientoUseCaseTest.java
├── infrastructure/adapter/in/rest/sanitario/aislamiento/
│   └── AislamientoControllerTest.java
└── integration/sanitario/aislamiento/
    └── AislamientoIntegrationTest.java
```

**Structure Decision**: Las solicitudes y decisiones pertenecen al subdominio sanitario de aislamiento. El estado del `Galpon` se lee y modifica exclusivamente a través de los puertos hacia el Módulo 1 (`GalponQueryPort`, `CambiarEstadoOperativoGalponPort`), sin duplicar tablas de galpones o lotes.

### Entidades y relación

```text
Galpon (Módulo 1) 1 ─── 0..* SolicitudAislamiento 1 ─── 0..1 DecisionAislamiento

SolicitudAislamiento (Aggregate Root)
├── id: UUID
├── galponId: UUID (Ref. Módulo 1)
├── galponNombreHistorico: String
├── loteId: UUID (Ref. Módulo 1)
├── loteNombreHistorico: String
├── poblacionActualReportada: Integer (> 0)
├── edadLoteDias: Integer (>= 1)
├── motivo: String (obligatorio, no vacío)
├── signosObservados: String (obligatorio, no vacío)
├── trabajadorId: UUID
├── emitidaEn: Instant (UTC)
├── estado: EstadoSolicitudAislamiento (PENDIENTE_VALIDACION, APROBADA, RECHAZADA)
├── decision: DecisionAislamiento (0..1, opcional hasta que el veterinario decida)
└── version: Integer (@Version para Optimistic Locking)

DecisionAislamiento (Value Object / Entidad Inmutable Asociada)
├── veterinarioId: UUID
├── decididaEn: Instant (UTC)
├── resultado: ResultadoValidacionAislamiento (APROBADA, RECHAZADA)
└── observaciones: String (obligatorio, no vacío)
```

### Contratos de los puertos

| Puerto                               | Tipo                          | Responsabilidad                                                                                                                                                                                   |
| ------------------------------------ | ----------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `SolicitudAislamientoRepositoryPort` | Salida (Persistencia)         | Guardar solicitud inmutable, bloquear duplicados en `PENDIENTE_VALIDACION` por galpón, buscar por ID, actualizar con `DecisionAislamiento` bajo `@Version` y consultar bandeja paginada/filtrada. |
| `GalponQueryPort`                    | Salida (Integración Módulo 1) | Comprobar existencia del galpón, obtener su nombre y verificar que su estado vigente sea estrictamente `PRODUCTIVO`.                                                                              |
| `LoteQueryPort`                      | Salida (Integración Módulo 1) | Obtener el lote actualmente alojado en el galpón, su nombre, su `poblacionActual` (`> 0`) y calcular su `edadLoteDias`.                                                                           |
| `CambiarEstadoOperativoGalponPort`   | Salida (Integración Módulo 1) | Solicitar de forma atómica la transición autorizada `PRODUCTIVO → AISLAMIENTO` en el Módulo 1 cuando la solicitud es aprobada.                                                                    |
| `IntegrationEventPublisherPort`      | Salida (Eventos Internos)     | Publicar el evento interno `AislamientoValidadoEvent` o `AislamientoRechazadoEvent` en Spring Modulith tras confirmar la decisión.                                                                |

### Contratos HTTP propuestos

| Método y ruta                                                | Acceso                                                      | Parámetros / Cabeceras                                                   | Responsabilidad                                                                                        |
| ------------------------------------------------------------ | ----------------------------------------------------------- | ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------ |
| `POST /api/galpones/{galponId}/solicitudes-aislamiento`      | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR`                     | Header: `X-Idempotency-Key`                                              | Crear solicitud en `PENDIENTE_VALIDACION` sin alterar el estado del galpón.                            |
| `GET /api/solicitudes-aislamiento`                           | `ROLE_VETERINARIO`, `ROLE_ADMINISTRADOR`, `ROLE_TRABAJADOR` | Query (opc): `?estado=PENDIENTE_VALIDACION&galponId=uuid&page=0&size=20` | Consultar bandeja paginada de solicitudes o verificar solicitud pendiente por galpón.                  |
| `POST /api/solicitudes-aislamiento/{solicitudId}/validacion` | `ROLE_VETERINARIO`                                          | Header: `X-Idempotency-Key`                                              | Aprobar (`PRODUCTIVO → AISLAMIENTO`) o rechazar (conserva `PRODUCTIVO`) tras revalidar estado vigente. |

Errores utilizan `application/problem+json` conforme a RFC 9457 y [General.md](General.md). Duplicado pendiente, solicitud ya resuelta, colisión de `@Version` o estado de galpón incompatible responden `409 Conflict`; solicitud o galpón inexistente responden `404 Not Found`; fallo de comunicación con el Módulo 1 responde `503 Service Unavailable`.

### JSON común de errores

```json
{
  "type": "https://avicontrol/errors/solicitud-aislamiento-pendiente",
  "title": "Ya existe una solicitud pendiente",
  "status": 409,
  "detail": "El galpón ya tiene una solicitud de validación de aislamiento en evaluación",
  "instance": "/api/galpones/550e8400-e29b-41d4-a716-446655440001/solicitudes-aislamiento",
  "code": "SOLICITUD_PENDIENTE_DUPLICADA",
  "correlationId": "uuid",
  "fieldErrors": []
}
```

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Acordar los estados operativos (`PRODUCTIVO`, `AISLAMIENTO`) y los contratos de `GalponQueryPort`, `LoteQueryPort` y `CambiarEstadoOperativoGalponPort` con el Módulo 1 (incluyendo respaldo autónomo _fallback_ para desarrollo local).
- [ ] T002 Confirmar en Spring Security los permisos de `ROLE_TRABAJADOR`, `ROLE_VETERINARIO` y `ROLE_ADMINISTRADOR` para cada endpoint del flujo.
- [ ] T003 Acordar el índice único parcial de solicitudes pendientes por galpón y el control de concurrencia `@Version`.
- [ ] T004 Alinear reloj compartido (`Clock`), filtro transversal `X-Idempotency-Key` y formato de errores RFC 9457 con [General.md](General.md).

**Checkpoint**: Contratos de solicitud, bandeja y transición aprobados.

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T005 Implementar en Java puro `SolicitudAislamiento`, `DecisionAislamiento`, `EstadoSolicitudAislamiento` (`PENDIENTE_VALIDACION`, `APROBADA`, `RECHAZADA`) y `ResultadoValidacionAislamiento` con normalización `trim()`, invariantes e inmutabilidad.
- [ ] T006 Definir las excepciones de dominio y los puertos `SolicitudAislamientoRepositoryPort`, `GalponQueryPort`, `LoteQueryPort`, `CambiarEstadoOperativoGalponPort` e `IntegrationEventPublisherPort`.
- [ ] T007 Crear la migración Flyway usando la siguiente versión disponible del repositorio para la tabla `solicitud_aislamiento`, incluyendo columna `version`, campos de decisión y el índice único parcial `WHERE estado = 'PENDIENTE_VALIDACION'` por `galpon_id`.
- [ ] T008 Implementar `SolicitudAislamientoPersistenceAdapter`, `Modulo1AislamientoAdapter` y la configuración transaccional en `AislamientoBeanConfiguration`.

**Checkpoint**: Solicitudes y decisiones listas en dominio y persistencia para soportar los flujos operativos.

## Phase 3: User Story 1 — Crear y enviar solicitud de aislamiento (Priority: P1)

**Spec**: SPEC-020, historia 1.

**Goal**: El trabajador solicita la revisión de un galpón en estado `PRODUCTIVO` con lote activo (`poblacionActual > 0`), registrando el motivo y los signos clínicos observados sin alterar el estado del galpón.

**Independent Test**: Una solicitud válida se guarda en estado `PENDIENTE_VALIDACION` capturando población, edad del lote y trabajador emisor, mientras el galpón permanece en `PRODUCTIVO`. Un galpón sin lote activo, con población cero, en estado distinto a `PRODUCTIVO` o con campos en blanco es rechazado sin crear el registro.

### Definición del endpoint REST para User Story 1

| Elemento       | Definición                                                                                                                                                                                           |
| -------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Método y ruta  | `POST /api/galpones/{galponId}/solicitudes-aislamiento`                                                                                                                                              |
| Autorización   | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR`                                                                                                                                                              |
| Cabeceras      | `X-Idempotency-Key`                                                                                                                                                                                  |
| Entrada (Path) | `galponId` (`UUID` obligatorio)                                                                                                                                                                      |
| Entrada (Body) | `motivo` (`String` obligatorio no vacío), `signosObservados` (`String` obligatorio no vacío)                                                                                                         |
| Respuesta 201  | `SolicitudAislamientoResponse` en estado `PENDIENTE_VALIDACION` con el snapshot del lote y galpón                                                                                                    |
| Errores        | `400` por campos vacíos; `401`/`403` por seguridad; `404` si el galpón no existe; `409` si el galpón no está en `PRODUCTIVO`, no tiene lote activo con aves vivas o ya tiene una solicitud pendiente |

#### JSON de solicitud (Request)

```json
{
  "motivo": "Sospecha de cuadro respiratorio agudo en el sector norte del galpón",
  "signosObservados": "Estertores traqueales, decaimiento general, plumas erizadas y disminución en el consumo de agua"
}
```

#### JSON de respuesta exitosa (Response 201)

```json
{
  "solicitudId": "7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d",
  "galponId": "550e8400-e29b-41d4-a716-446655440001",
  "galponNombre": "Galpón 1",
  "estadoActualGalpon": "PRODUCTIVO",
  "loteId": "2fc03a21-84c4-4c83-bb15-6607bb75cb90",
  "loteNombre": "LDP-001",
  "poblacionActualReportada": 7920,
  "edadLoteDias": 21,
  "motivo": "Sospecha de cuadro respiratorio agudo en el sector norte del galpón",
  "signosObservados": "Estertores traqueales, decaimiento general, plumas erizadas y disminución en el consumo de agua",
  "estado": "PENDIENTE_VALIDACION",
  "trabajadorId": "c1d2e3f4-a5b6-7c8d-9e0f-1a2b3c4d5e6f",
  "emitidaEn": "2026-10-25T13:10:00Z",
  "decision": null,
  "version": 1
}
```

### Tests e implementación

- [ ] T009 [US1] Probar autorización (`ROLE_TRABAJADOR` / `ROLE_ADMINISTRADOR`), normalización `trim()` de campos obligatorios y captura automática del snapshot del lote (`poblacionActualReportada` y `edadLoteDias`).
- [ ] T010 [US1] Probar validación de estado `PRODUCTIVO` y rechazo ante galpón en otro estado, sin lote activo o con `poblacionActual == 0`.
- [ ] T011 [US1] Verificar que el envío de la solicitud jamás invoca el cambio de estado del galpón y respeta `X-Idempotency-Key`.
- [ ] T012 [US1] Implementar `CrearSolicitudAislamientoUseCase`, DTOs, mapper y endpoint `POST /api/galpones/{galponId}/solicitudes-aislamiento`.

**Checkpoint**: Solicitud inmutable creada y disponible de inmediato para la bandeja veterinaria.

## Phase 4: User Story 2 — Consultar bandeja de pendientes y prevenir solicitudes duplicadas (Priority: P2)

**Spec**: SPEC-020, historia 2; SPEC-010, FR-002.

**Goal**: Mostrar la bandeja paginada y filtrable de solicitudes de aislamiento e impedir la creación de una segunda solicitud abierta para un mismo galpón.

**Independent Test**: Una segunda solicitud enviada para un galpón que ya tiene una solicitud en `PENDIENTE_VALIDACION` es rechazada con `409 Conflict` (`SOLICITUD_PENDIENTE_DUPLICADA`); una vez que el veterinario la resuelve (por ejemplo, como `RECHAZADA`), el sistema permite registrar una nueva solicitud futura si el galpón sigue en `PRODUCTIVO`.

### Definición del endpoint REST para User Story 2

| Elemento      | Definición                                                                                                                |
| ------------- | ------------------------------------------------------------------------------------------------------------------------- |
| Método y ruta | `GET /api/solicitudes-aislamiento`                                                                                        |
| Autorización  | `ROLE_VETERINARIO`, `ROLE_ADMINISTRADOR`, `ROLE_TRABAJADOR`                                                               |
| Parámetros    | Query params opcionales: `estado` (por defecto `PENDIENTE_VALIDACION`), `galponId` (`UUID`), `page` (`0`), `size` (`20`). |
| Respuesta 200 | Página de `SolicitudAislamientoResponse` ordenada por `emitidaEn` descendente con metadatos de paginación.                |

### Tests e implementación

- [ ] T013 [US2] Probar unicidad ante dos solicitudes concurrentes sobre el mismo galpón verificando el bloqueo por índice único parcial (`409 Conflict`).
- [ ] T014 [US2] Probar que tras una decisión terminal (`RECHAZADA`) se permite crear una nueva solicitud para el galpón.
- [ ] T015 [US2] Probar filtros (`estado`, `galponId`), paginación y permisos en `GET /api/solicitudes-aislamiento`.
- [ ] T016 [US2] Implementar `ConsultarSolicitudesAislamientoUseCase` y el endpoint `GET /api/solicitudes-aislamiento`.

**Checkpoint**: No existen dos solicitudes pendientes simultáneas por galpón y la bandeja responde en menos de 1 segundo.

## Phase 5: User Story 3 — Evaluar y validar o rechazar el aislamiento solicitado (Priority: P1)

**Spec**: SPEC-010, historia 1.

**Goal**: El veterinario evalúa una solicitud pendiente y aprueba o rechaza el aislamiento; si aprueba y el galpón continúa en `PRODUCTIVO`, el sistema cambia atómicamente el estado del galpón a `AISLAMIENTO` en el Módulo 1 y habilita el diagnóstico (Plan 015); si rechaza, conserva el galpón en `PRODUCTIVO`.

**Independent Test**: Solo `ROLE_VETERINARIO` puede decidir (`403` para trabajador o administrador); aprobar con galpón en `PRODUCTIVO` cambia el estado del galpón a `AISLAMIENTO` y marca la solicitud como `APROBADA`; rechazar con observaciones marca la solicitud como `RECHAZADA` y conserva el galpón en `PRODUCTIVO`; si el estado del galpón cambió antes de confirmar o el Módulo 1 falla, se rechaza o hace _rollback_ sin sobrescribir datos.

### Definición del endpoint REST para User Story 3

| Elemento       | Definición                                                                                                                                                                                                                                                                         |
| -------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Método y ruta  | `POST /api/solicitudes-aislamiento/{solicitudId}/validacion`                                                                                                                                                                                                                       |
| Autorización   | `ROLE_VETERINARIO`                                                                                                                                                                                                                                                                 |
| Cabeceras      | `X-Idempotency-Key`                                                                                                                                                                                                                                                                |
| Entrada (Path) | `solicitudId` (`UUID` obligatorio)                                                                                                                                                                                                                                                 |
| Entrada (Body) | `resultado` (`APROBADA` o `RECHAZADA`), `observaciones` (`String` obligatorio no vacío), `version` (`Integer` obligatorio para control `@Version`)                                                                                                                                 |
| Respuesta 200  | `ValidacionAislamientoResponse` con la solicitud resuelta, la decisión registrada, el `estadoResultanteGalpon` (`AISLAMIENTO` o `PRODUCTIVO`) y la bandera `habilitaDiagnostico` (`true` si fue aprobada)                                                                          |
| Errores        | `400` por observaciones vacías o resultado inválido; `403` si el usuario no es veterinario; `404` si la solicitud no existe; `409` si la solicitud ya fue resuelta, colisión de `@Version` o el galpón ya no está en `PRODUCTIVO`; `503` con _rollback_ si el Módulo 1 no responde |

#### JSON de solicitud (`POST /api/solicitudes-aislamiento/{solicitudId}/validacion`)

```json
{
  "resultado": "APROBADA",
  "observaciones": "Se confirma cuadro respiratorio infeccioso en inspección clínica. Se ordena aislamiento preventivo inmediato para diagnóstico.",
  "version": 1
}
```

#### JSON de respuesta exitosa (Response 200)

```json
{
  "solicitudId": "7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d",
  "galponId": "550e8400-e29b-41d4-a716-446655440001",
  "galponNombre": "Galpón 1",
  "loteId": "2fc03a21-84c4-4c83-bb15-6607bb75cb90",
  "loteNombre": "LDP-001",
  "estadoSolicitud": "APROBADA",
  "estadoResultanteGalpon": "AISLAMIENTO",
  "habilitaDiagnostico": true,
  "decision": {
    "veterinarioId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "decididaEn": "2026-10-25T13:25:00Z",
    "resultado": "APROBADA",
    "observaciones": "Se confirma cuadro respiratorio infeccioso en inspección clínica. Se ordena aislamiento preventivo inmediato para diagnóstico."
  },
  "version": 2
}
```

### Tests e implementación

- [ ] T017 [US3] Probar autorización exclusiva `ROLE_VETERINARIO` (`403` a trabajador/administrador), existencia de solicitud pendiente y obligatoriedad de `observaciones`.
- [ ] T018 [US3] Probar decisión `APROBADA` verificando reconsulta del estado `PRODUCTIVO`, transición `PRODUCTIVO → AISLAMIENTO` en el Módulo 1 y `habilitaDiagnostico = true`.
- [ ] T019 [US3] Probar decisión `RECHAZADA` verificando que la solicitud pase a `RECHAZADA` y el galpón permanezca intacto en `PRODUCTIVO`.
- [ ] T020 [US3] Probar rechazo (`409 Conflict`) si el estado del galpón cambió antes de confirmar, si la solicitud ya había sido decidida o si ocurre colisión de `@Version`.
- [ ] T021 [US3] Probar idempotencia con `X-Idempotency-Key` y _rollback_ transaccional completo si falla `CambiarEstadoOperativoGalponPort`.
- [ ] T022 [US3] Implementar `ValidarAislamientoUseCase`, adaptador de cambio de estado, publicación de evento interno y endpoint `POST /api/solicitudes-aislamiento/{solicitudId}/validacion`.

**Checkpoint**: La decisión veterinaria y el estado operativo del galpón en el Módulo 1 se mantienen 100 % consistentes y atómicos.

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T023 Documentar los tres endpoints, parámetros de consulta, cabecera `X-Idempotency-Key`, roles y errores RFC 9457 en OpenAPI 3.
- [ ] T024 Completar `AislamientoIntegrationTest` con PostgreSQL real verificando el flujo completo: solicitud de trabajador, bloqueo de duplicado, bandeja veterinaria, rechazo, nueva solicitud y aprobación con transición a `AISLAMIENTO`.
- [ ] T025 Verificar reglas de arquitectura hexagonal con ArchUnit y medir tiempos de respuesta (`p95 < 1 segundo`) conforme a [General.md](General.md).

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)** precede a **Foundational (Phase 2)**.
- **Foundational (Phase 2)** habilita las tres historias de usuario.
- **US1 (Phase 3)** y **US2 (Phase 4)** crean y listan las solicitudes pendientes que evalúa **US3 (Phase 5)**.
- **US3 (Phase 5)** depende de la solicitud previa en estado `PENDIENTE_VALIDACION` y de la revalidación síncrona con el Módulo 1.
- El diagnóstico clínico del [Plan 015](015-DiagnosticoYReintegroSanitario.md) inicia únicamente después de una validación aprobada en **US3**.

### Dependencias con otros planes

- **Módulo 1 / [Plan 001](001-ConsultaDeGalpones.md)**: fuente autoritativa de galpón, lote activo y transición operativa `PRODUCTIVO → AISLAMIENTO`.
- **[Plan 013](013-GestionDeAlertasSanitarias.md)**: las alertas de mortalidad anormal pueden aportar contexto clínico al veterinario, sin sustituir la solicitud formal de este plan.
- **[Plan 015](015-DiagnosticoYReintegroSanitario.md)**: consume el estado `AISLAMIENTO` confirmado por este plan para registrar el diagnóstico clínico.
- **[Plan 005](005-CatalogoDeEnfermedades.md)**: catálogo de enfermedades utilizado posteriormente en el diagnóstico.
- **[General.md](General.md)**: seguridad, reloj, idempotencia, eventos internos, errores RFC 9457 y transacciones.

### Dentro de cada User Story

- Consultar datos vigentes del Módulo 1 justo antes de crear o aprobar una solicitud.
- Persistir la decisión y cambiar el estado del galpón dentro de la misma transacción local usando los puertos propietarios.
- Probar permisos, bloqueo de duplicados, concurrencia (`@Version`) y _rollback_ ante fallos de dependencias junto con la implementación.

## Notes

- Crear una solicitud en `SPEC-020` nunca aísla el galpón por sí mismo.
- Solo `ROLE_VETERINARIO` aprueba o rechaza el aislamiento (`SPEC-010`); solo el [Plan 015](015-DiagnosticoYReintegroSanitario.md) diagnostica después de que el galpón está en `AISLAMIENTO`.
- Las tareas `T001` a `T025` cubren la totalidad de [SPEC-020](../specs/020-SolicitarValidacionParaAislarGalpon.md) y [SPEC-010](../specs/010-ValidarAislamiento.md).
