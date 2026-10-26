# Implementation Plan: Diagnóstico y Reintegro Sanitario

**Date**: 25/10/2026
**Last Updated**: 2026-09-26

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [011-DiagnosticarGalpon.md](../specs/011-DiagnosticarGalpon.md)

## Summary

Registrar el diagnóstico veterinario de un galpón ya aislado, conservar el snapshot de enfermedad y medicación aplicadas, calcular reintegro cuando corresponda y devolver automáticamente el galpón a producción al vencer el plazo, siempre que siga aislado. Una enfermedad que exige sacrificio habilita Plan 008 sin crear medicación ni fecha de reintegro.

## Technical Context

**Performance Goals**: El 95 % de diagnósticos responde en máximo 1 segundo; el proceso de reintegro detecta y procesa vencimientos sin duplicar transición.

**Constraints**: Solo veterinario registra diagnósticos. Requiere estado AISLAMIENTO, lote activo y enfermedad existente en el catálogo. Enfermedad tratable exige medicación compatible; enfermedad mortal no exige medicación ni fecha de reintegro. No sobrescribir un estado de galpón distinto de AISLAMIENTO.

**Scale/Scope**: SPEC-011, diagnóstico y procesamiento idempotente de reintegros; un POST de diagnóstico y una ejecución programada interna.

**Dependencias funcionales**: Plan 005 consulta catálogo de enfermedad; Plan 006 consulta compatibilidad y días de medicación; Plan 014 produce el estado de aislamiento validado; Plan 008 consume diagnóstico elegible para sacrificio; Módulo 1 mantiene estado y lote; General.md aporta seguridad, Clock, errores y transacciones.

### Decisiones específicas

1. `DiagnosticoGalpon` referencia galpón, lote y enfermedad; conserva snapshot de nombre y flag de sacrificio de enfermedad, y los datos/días de medicación utilizados.
2. Solo se diagnostica cuando el estado vigente es AISLAMIENTO y el lote actual está asociado al galpón.
3. Para enfermedad tratable, `fechaReintegro = fechaDiagnostico + diasTotalesMedicacion`; la medicación se valida compatible en Plan 006.
4. Para enfermedad con sacrificio, `medicacionId` y `fechaReintegro` son nulos; se publica el hecho que habilita Plan 008, sin emitir orden aquí.
5. El scheduler encuentra diagnósticos vencidos y solicita transición AISLAMIENTO → PRODUCTIVO al Módulo 1. Revisa estado en el momento de la transición, procesa una sola vez y no altera otros estados.
6. Eventos internos y de integración se publican después de confirmar la operación con entrega confiable conforme a General.md.

### Alcance documental

- Este plan implementa SPEC-011: diagnóstico, snapshot histórico, reintegro y habilitación de sacrificio.
- [Plan 005](005-CatalogoDeEnfermedades.md) mantiene enfermedades y sus atributos vigentes.
- [Plan 006](006-GestionDeMedicaciones.md) mantiene medicaciones y su compatibilidad/duración.
- [Plan 014](014-SolicitudesYValidacionDeAislamiento.md) establece el aislamiento antes del diagnóstico.
- [Plan 008](008-GestionDelSacrificioSanitario.md) ejecuta una orden habilitada por diagnóstico elegible.

## Project Structure

### Documentation (this feature)

```text
docs/specs/011-DiagnosticarGalpon.md
docs/plan/015-DiagnosticoYReintegroSanitario.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/model/sanitario/diagnostico/
│   ├── DiagnosticoGalpon.java
│   └── EstadoDiagnostico.java
├── domain/port/out/sanitario/diagnostico/
│   ├── DiagnosticoGalponRepositoryPort.java
│   ├── EnfermedadQueryPort.java
│   ├── MedicacionQueryPort.java
│   ├── GalponQueryPort.java
│   ├── LoteQueryPort.java
│   └── CambiarEstadoOperativoGalponPort.java
├── application/sanitario/diagnostico/
│   ├── RegistrarDiagnosticoUseCase.java
│   └── ProcesarReintegrosVencidosUseCase.java
└── infrastructure/
    ├── adapter/in/rest/sanitario/diagnostico/DiagnosticoController.java
    ├── adapter/in/scheduling/ReintegroSanitarioScheduler.java
    ├── adapter/out/internal/sanitario/diagnostico/
    ├── adapter/out/persistence/sanitario/diagnostico/
    └── config/DiagnosticoBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/sanitario/diagnostico/
├── application/sanitario/diagnostico/
└── integration/DiagnosticoReintegroIntegrationTest.java
```

**Structure Decision**: El diagnóstico y su historial son propiedad de este plan. Enfermedad, medicación, galpón y lote se leen mediante puertos y no se persisten como agregados duplicados.

### Entidades y relación

```text
Galpon aislado + Lote activo ─── DiagnosticoGalpon
DiagnosticoGalpon ─── Enfermedad (snapshot)
DiagnosticoGalpon ─── Medicacion (snapshot opcional)

DiagnosticoGalpon: id, galponId, loteId, enfermedadId, nombreEnfermedadHistorico,
  requiereSacrificioHistorico, medicacionId?, diasMedicacion?, fechaDiagnostico,
  fechaReintegro?, estado
```

### Contratos de los puertos

| Puerto | Responsabilidad |
| --- | --- |
| `DiagnosticoGalponRepositoryPort` | Persistir snapshots, consultar diagnóstico y buscar reintegros vencidos pendientes. |
| `EnfermedadQueryPort` | Consultar por ID una enfermedad registrada y sus atributos de decisión vigentes. |
| `MedicacionQueryPort` | Consultar compatibilidad y duración de medicación. |
| `GalponQueryPort` / `LoteQueryPort` | Validar estado AISLAMIENTO y lote alojado actual. |
| `CambiarEstadoOperativoGalponPort` | Solicitar cambio AISLAMIENTO → PRODUCTIVO bajo validación vigente. |
| `IntegrationEventPublisherPort` | Publicar diagnóstico, reintegro o habilitación de sacrificio a consumidores acordados. |

### Contratos HTTP propuestos

| Método y ruta | Acceso | Responsabilidad |
| --- | --- | --- |
| `POST /api/galpones/{galponId}/diagnosticos` | `ROLE_VETERINARIO` | Registrar diagnóstico y fecha de reintegro opcional. |

La ejecución del scheduler no expone endpoint público. Errores siguen RFC 9457: estado incompatible o medicación incompatible 409, enfermedad/medicación inexistente 404, dependencia indisponible 503.

### JSON común de errores

```json
{
  "type": "https://avicontrol/errors/estado-galpon-incompatible",
  "title": "Estado incompatible con el diagnóstico",
  "status": 409,
  "detail": "El galpón debe continuar aislado para registrar el diagnóstico",
  "instance": "/api/galpones/{galponId}/diagnosticos",
  "code": "GALPON_NO_AISLADO",
  "correlationId": "uuid",
  "fieldErrors": []
}
```

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Acordar estados y transición con Módulo 1; revisar interacción con Plan 014.
- [ ] T002 Acordar con Plan 005 el contrato de consulta y los datos de enfermedad que se copian al snapshot del diagnóstico.
- [ ] T003 Acordar consulta de compatibilidad y duración de medicación con Plan 006.
- [ ] T004 Acordar evento `SacrificioSanitarioHabilitado` con Plan 008.
- [ ] T005 Definir Clock, zona de negocio, scheduler, idempotencia y errores con General.md.

**Checkpoint**: Contratos clínicos, transición y reintegro acordados.

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T006 Implementar modelo y reglas de `DiagnosticoGalpon`.
- [ ] T007 Definir repositorio y puertos de enfermedad, medicación, galpón y lote.
- [ ] T008 Crear migración de diagnósticos y snapshots sin tablas duplicadas externas.
- [ ] T009 Implementar persistencia, mappers y casos de uso transaccionales.
- [ ] T010 Configurar scheduler reentrante e idempotente y eventos confiables.

**Checkpoint**: Diagnóstico y procesamiento temporal listos para historias.

## Phase 3: User Story 1 — Registrar diagnóstico y definir el resultado sanitario (Priority: P1)

**Spec**: SPEC-011, historia 1.

**Goal**: Veterinario registra enfermedad sobre galpón aislado con lote activo y snapshot de la decisión clínica.

**Independent Test**: Enfermedad tratable exige medicación compatible y calcula reintegro; enfermedad con sacrificio no exige medicación ni reintegro y habilita Plan 008; estado no aislado o falta de lote se rechaza.

### Definición del evento y endpoint

`POST /api/galpones/{galponId}/diagnosticos` recibe `enfermedadId` y, cuando corresponde, `medicacionId`. Respuesta 201 incluye diagnóstico, snapshot y fecha de reintegro opcional. Se publican `DiagnosticoGalponRegistrado` y, para sacrificio, `SacrificioSanitarioHabilitado` después de confirmar.

### JSON de solicitud tratable

```json
{"enfermedadId":"uuid","medicacionId":"uuid"}
```

### Tests e implementación

- [ ] T011 Probar autorización, estado AISLAMIENTO, lote activo y enfermedad vigente.
- [ ] T012 Probar medicación obligatoria/compatible y cálculo de fecha de reintegro.
- [ ] T013 Probar snapshot y enfermedad que requiere sacrificio sin medicación ni fecha.
- [ ] T014 Implementar caso de uso, persistencia, eventos y endpoint.

**Checkpoint**: Diagnósticos reflejan la decisión clínica sin mutar catálogos fuente.

## Phase 4: User Story 2 — Procesar reintegros vencidos automáticamente (Priority: P1)

**Spec**: SPEC-011, historias 1 y sus FR de reintegro.

**Goal**: Al llegar la fecha de reintegro, devolver a producción únicamente galpones que sigan en aislamiento.

**Independent Test**: Diagnóstico vencido con estado AISLAMIENTO pasa una vez a PRODUCTIVO; estado modificado por otra causa se conserva; un fallo puede reintentarse sin duplicar transición.

### Definición del proceso

El scheduler obtiene diagnósticos con `fechaReintegro <= hoy` y reintegro pendiente, consulta estado vigente y solicita la transición al Módulo 1. El resultado se marca procesado solo tras confirmar el cambio o registrar que el estado ya no es elegible.

### Tests e implementación

- [ ] T015 Probar vencimiento, zona horaria y no ejecución antes de la fecha.
- [ ] T016 Probar idempotencia, recuperación ante indisponibilidad y no sobrescritura de otro estado.
- [ ] T017 Implementar búsqueda de vencidos, caso de uso programado y scheduler.
- [ ] T018 Publicar hecho de reintegro solo después de confirmar el cambio.

**Checkpoint**: Reintegros se procesan una vez y respetan el estado autoritativo.

## Phase 5: Polish & Cross-Cutting Concerns

- [ ] T019 Documentar endpoint y transiciones en OpenAPI/arquitectura.
- [ ] T020 Verificar integración con Planes 005, 006, 008 y 014.
- [ ] T021 Verificar scheduler, concurrencia y métricas conforme a General.md.

## Dependencies & Execution Order

### Phase Dependencies

- Setup precede a Foundational.
- Diagnóstico depende del aislamiento validado por Plan 014 y puertos de Planes 005/006.
- Reintegro depende de diagnósticos tratables con fecha vencida.
- Polish depende de ambos flujos.

### Dependencias con otros planes

- **Plan 005**: Catálogo de enfermedades.
- **Plan 006**: Compatibilidad y duración de medicación.
- **Plan 014**: Solicitud y validación de aislamiento.
- **Plan 008**: Ejecuta sacrificio sanitario habilitado por diagnóstico.
- **Módulo 1 / Plan 001**: Galpones, lotes y transición operativa.
- **General.md**: Seguridad, reloj, eventos, errores y transacciones.

### Dentro de cada User Story

- Consultar estado vigente antes de escribir.
- Guardar snapshot clínico antes de publicar eventos.
- Reintentar reintegros sin duplicar transiciones.

## Notes

- Este plan no solicita ni valida aislamiento y no registra enfermedades.
- El estado del galpón solo se cambia mediante el puerto del Módulo 1.
- Una enfermedad con sacrificio no produce fecha de reintegro.