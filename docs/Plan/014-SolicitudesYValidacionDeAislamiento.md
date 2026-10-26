# Implementation Plan: Solicitudes y Validación de Aislamiento

**Date**: 25/10/2026
**Last Updated**: 2026-09-26

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [020-SolicitarValidacionParaAislarGalpon.md](../specs/020-SolicitarValidacionParaAislarGalpon.md)
- [010-ValidarAislamiento.md](../specs/010-ValidarAislamiento.md)

## Summary

Permitir que trabajadores envíen solicitudes inmutables de validación para un galpón productivo con lote activo, y que veterinarios revisen solicitudes y validen o rechacen el aislamiento. Crear una solicitud no cambia el estado del galpón; solo una validación afirmativa solicita al Módulo 1 la transición a `AISLAMIENTO`.

## Technical Context

**Performance Goals**: El 95 % de solicitudes y validaciones responde en máximo 1 segundo; la consulta de pendientes responde en máximo 1 segundo.

**Constraints**: No más de una solicitud pendiente por galpón; solicitudes enviadas inmutables; trabajador no cambia estado; veterinario valida únicamente si el estado vigente sigue siendo productivo.

**Scale/Scope**: SPEC-020 y SPEC-010; tres historias: solicitar aislamiento, listar solicitudes/prevenir duplicados, validar o rechazar solicitud; dos operaciones POST y una consulta GET.

**Dependencias funcionales**: Módulo 1 / Plan 001 para consulta de galpón/lote y cambio de estado; Plan 013 proporciona contexto de alertas cuando corresponda; Plan 015 recibe galpones ya aislados para diagnóstico; General.md define seguridad, actor, reloj, errores y eventos.

### Decisiones específicas

1. `SolicitudAislamiento` conserva galpón, lote, población/edad al solicitar, motivo, signos observados, actor, fecha y estado.
2. Unicidad de solicitud pendiente se protege con restricción/operación atómica por galpón, no solo con validación de interfaz.
3. La solicitud nunca cambia el estado operativo; el cambio PRODUCTIVO → AISLAMIENTO ocurre solo al validar con éxito.
4. El veterinario consulta nuevamente el estado de Módulo 1 antes de validar. Si no puede comprobar el estado o este cambió, se rechaza sin sobrescribirlo.
5. Decisión de validación conserva actor, timestamp, resultado y observaciones; no se edita ni elimina.
6. Las alertas sanitarias del Plan 013 aportan contexto para priorización, pero no sustituyen la solicitud requerida por SPEC-010.

### Alcance documental

- Este plan implementa SPEC-020 (solicitud y control de pendientes) y SPEC-010 (evaluación y validación).
- [Plan 005](005-CatalogoDeEnfermedades.md) es dueño del catálogo de enfermedades.
- [Plan 015](015-DiagnosticoYReintegroSanitario.md) diagnostica después de que el galpón está en aislamiento.
- [Plan 013](013-GestionDeAlertasSanitarias.md) mantiene alertas de mortalidad anormal; este plan no duplica sus registros.

## Project Structure

### Documentation (this feature)

```text
docs/specs/020-SolicitarValidacionParaAislarGalpon.md
docs/specs/010-ValidarAislamiento.md
docs/plan/014-SolicitudesYValidacionDeAislamiento.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/model/sanitario/aislamiento/
│   ├── SolicitudAislamiento.java
│   ├── EstadoSolicitudAislamiento.java
│   └── DecisionAislamiento.java
├── domain/port/out/sanitario/aislamiento/
│   ├── SolicitudAislamientoRepositoryPort.java
│   ├── GalponQueryPort.java
│   └── CambiarEstadoOperativoGalponPort.java
├── application/sanitario/aislamiento/
│   ├── CrearSolicitudAislamientoUseCase.java
│   ├── ConsultarSolicitudesPendientesUseCase.java
│   └── ValidarAislamientoUseCase.java
└── infrastructure/
    ├── adapter/in/rest/sanitario/aislamiento/AislamientoController.java
    ├── adapter/out/internal/sanitario/aislamiento/
    ├── adapter/out/persistence/sanitario/aislamiento/
    └── config/AislamientoBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/sanitario/aislamiento/
├── application/sanitario/aislamiento/
└── integration/AislamientoIntegrationTest.java
```

**Structure Decision**: Las solicitudes y decisiones pertenecen al dominio sanitario. El estado de Galpón se modifica solo mediante puerto de salida del Módulo 1.

### Entidades y relación

```text
Galpon (Módulo 1) 1 ─── 0..* SolicitudAislamiento 1 ─── 0..1 DecisionAislamiento

SolicitudAislamiento: id, galponId, loteId, emitidaEn, motivo, signos,
  poblacionActual, edadLoteDias, trabajadorId, estado
DecisionAislamiento: id, solicitudId, veterinarioId, decididaEn,
  resultado, observaciones
```

### Contratos de los puertos

| Puerto | Responsabilidad |
| --- | --- |
| `SolicitudAislamientoRepositoryPort` | Crear solicitud, bloquear duplicados pendientes y consultar bandeja. |
| `GalponQueryPort` / `LoteQueryPort` | Comprobar estado, lote activo y snapshot poblacional al solicitar. |
| `CambiarEstadoOperativoGalponPort` | Solicitar transición autorizada PRODUCTIVO → AISLAMIENTO al Módulo 1. |
| `IntegrationEventPublisherPort` | Publicar solicitud/decisión confirmada si hay consumidores acordados. |

### Contratos HTTP propuestos

| Método y ruta | Acceso | Responsabilidad |
| --- | --- | --- |
| `POST /api/galpones/{galponId}/solicitudes-aislamiento` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` | Crear solicitud pendiente sin alterar estado del galpón. |
| `GET /api/solicitudes-aislamiento?estado=PENDIENTE_VALIDACION` | `ROLE_VETERINARIO`, `ROLE_ADMINISTRADOR` | Consultar bandeja pendiente paginada. |
| `POST /api/solicitudes-aislamiento/{solicitudId}/validacion` | `ROLE_VETERINARIO` | Aceptar o rechazar tras validar estado vigente. |

Errores utilizan RFC 9457. Duplicado pendiente o estado incompatible responden 409; solicitud/galpón inexistente 404.

### JSON común de errores

```json
{
  "type": "https://avicontrol/errors/solicitud-aislamiento-pendiente",
  "title": "Ya existe una solicitud pendiente",
  "status": 409,
  "detail": "El galpón ya tiene una solicitud en evaluación",
  "instance": "/api/galpones/{galponId}/solicitudes-aislamiento",
  "code": "SOLICITUD_PENDIENTE_DUPLICADA",
  "correlationId": "uuid",
  "fieldErrors": []
}
```

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Acordar estados y puertos de consulta/cambio con Módulo 1.
- [ ] T002 Confirmar los roles de trabajador, veterinario y administrador para el flujo de solicitudes.
- [ ] T003 Acordar política de duplicados y concurrencia para solicitudes pendientes.
- [ ] T004 Alinear eventos, reloj y errores con General.md.

**Checkpoint**: Contratos de solicitud y transición aprobados por propietarios.

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T005 Implementar entidades de solicitud y decisión con invariantes e inmutabilidad.
- [ ] T006 Definir repositorio, puertos de consulta y cambio de estado.
- [ ] T007 Crear migraciones con unicidad de solicitud pendiente por galpón.
- [ ] T008 Implementar adaptadores y transacción de validación.

**Checkpoint**: Solicitudes y decisiones listas para los flujos operativos.

## Phase 3: User Story 1 — Crear y enviar solicitud de aislamiento (Priority: P1)

**Spec**: SPEC-020, historia 1.

**Goal**: Trabajador solicita revisión de galpón productivo y lote activo, adjuntando motivo y signos observados.

**Independent Test**: Solicitud válida queda pendiente, guarda población/edad/actor y no cambia estado; galpón sin lote, población cero o estado no productivo se rechaza.

### Definición del endpoint

`POST /api/galpones/{galponId}/solicitudes-aislamiento` recibe motivo y signos clínicos; galpón/lote/snapshot se validan en servidor y responde 201.

### Tests e implementación

- [ ] T009 Probar rol, campos obligatorios y snapshot de lote.
- [ ] T010 Probar estado productivo y rechazo de galpón/lote inválido.
- [ ] T011 Verificar que el envío no cambia el estado del galpón.
- [ ] T012 Implementar caso de uso, DTOs, persistencia y endpoint.

**Checkpoint**: Solicitud inmutable y disponible para bandeja veterinaria.

## Phase 4: User Story 2 — Consultar pendientes y prevenir solicitudes duplicadas (Priority: P2)

**Spec**: SPEC-020, historia 2.

**Goal**: Mostrar la bandeja pendiente e impedir concurrencia de solicitudes abiertas para el mismo galpón.

**Independent Test**: Segunda solicitud pendiente se rechaza con estado actual; tras decisión terminal se permite una solicitud nueva si cumple reglas.

### Tests e implementación

- [ ] T013 Probar unicidad con dos solicitudes concurrentes del mismo galpón.
- [ ] T014 Probar filtros, paginación y permisos de la bandeja.
- [ ] T015 Implementar consulta paginada y restricción de duplicados.

**Checkpoint**: No hay dos solicitudes pendientes simultáneas por galpón.

## Phase 5: User Story 3 — Evaluar y validar el aislamiento solicitado (Priority: P1)

**Spec**: SPEC-010, historia 1.

**Goal**: Veterinario acepta o rechaza una solicitud; si acepta, el estado vigente PRODUCTIVO cambia a AISLAMIENTO mediante Módulo 1.

**Independent Test**: Solo veterinario puede decidir; aprobación con estado productivo cambia únicamente estado; solicitud ausente, estado cambiado o Módulo 1 indisponible no sobrescribe datos.

### Definición del endpoint

`POST /api/solicitudes-aislamiento/{solicitudId}/validacion` recibe decisión y observaciones. Se vuelve a consultar estado vigente antes de confirmar. Responde 200 con decisión y estado resultante.

### Tests e implementación

- [ ] T016 Probar autorización veterinaria, solicitud previa y decisiones.
- [ ] T017 Probar PRODUCTIVO → AISLAMIENTO y rechazo si estado actual difiere.
- [ ] T018 Probar idempotencia y reversión si falla el puerto Módulo 1.
- [ ] T019 Implementar caso de uso, adapter de estado, eventos y endpoint.

**Checkpoint**: Solicitud y cambio de estado se mantienen consistentes.

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T020 Documentar contratos y roles en OpenAPI.
- [ ] T021 Completar integración de solicitud, bandeja y decisión con Módulo 1.
- [ ] T022 Verificar unicidad, atomicidad y límites de módulo conforme a General.md.

## Dependencies & Execution Order

### Phase Dependencies

- Setup precede a Foundational.
- Foundational habilita las historias.
- Solicitud precede a bandeja/decisión.
- Validación depende de solicitud previa y consulta vigente de Módulo 1.
- Diagnóstico del Plan 015 solo inicia después de la validación afirmativa.

### Dependencias con otros planes

- **Plan 001 / Módulo 1**: fuente autoritativa de galpón/lote y cambio de estado.
- **Plan 013**: alertas de mortalidad pueden aportar contexto clínico, sin sustituir la solicitud.
- **Plan 015**: consume estado AISLAMIENTO confirmado para diagnosticar.
- **Plan 005**: catálogo de enfermedades usado por el diagnóstico posterior.
- **General.md**: seguridad, reloj, eventos, errores y transacciones.

### Dentro de cada User Story

- Consultar datos vigentes justo antes de aceptar una solicitud.
- Persistir decisión y cambiar estado usando puertos propietarios.
- Probar permisos, duplicados y fallos de dependencias junto con implementación.

## Notes

- Crear una solicitud no implica aislar el galpón.
- Solo veterinario valida o rechaza; solo Plan 015 diagnostica después de aislamiento.
- La alerta del Plan 013 no crea automáticamente una solicitud ni cambia el estado operativo.