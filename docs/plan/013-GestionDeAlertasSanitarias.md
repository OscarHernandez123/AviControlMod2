# Implementation Plan: Gestión de Alertas Sanitarias

**Date**: 25/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [018-GenerarAlertaSanitaria.md](../specs/018-GenerarAlertaSanitaria.md)

## Summary

Generar una alerta inmutable por cada mortalidad anormal confirmada y proporcionar al veterinario una bandeja paginada y un detalle consolidado con contexto del galpón, lote y reporte que originó la alerta. El Plan 007 invoca la generación mediante un puerto público dentro del flujo transaccional de la mortalidad anormal.

## Technical Context

**Performance Goals**: Alta de alerta en la misma operación de confirmación de mortalidad anormal; el 95 % de consultas de bandeja y detalle responde en máximo 1 segundo.

**Constraints**: Estado inicial `PENDIENTE_ATENCION_VETERINARIA`; prioridad deriva de severidad; alertas inmutables, no se eliminan ni cierran desde este plan; solo veterinario y administrador autorizado consultan. La mortalidad anormal, la actualización de población y la creación de su alerta se confirman en una única transacción local; si falla la creación de la alerta, se revierte la operación completa.

**Scale/Scope**: SPEC-018, dos historias: generación automática y consulta contextual de alertas; dos endpoints GET y un puerto público de creación usado por Plan 007.

**Dependencias funcionales**:

- Plan 007: solicita la alerta tras registrar la mortalidad anormal y provee sus datos de origen.
- Plan 001 / Módulo 1: consultas actuales de galpón y lote.
- Plan 014 puede utilizar la alerta como contexto para priorizar la solicitud de aislamiento.
- General.md: modularidad, seguridad, transacciones, errores, reloj y eventos.

### Decisiones específicas

1. Cada alerta es propia de un registro anormal; eventos diferentes generan alertas diferentes aunque el galpón ya tenga pendientes.
2. La creación valida la existencia del registro de mortalidad anormal y persiste IDs de alerta, registro, galpón y lote, prioridad, estado y fecha de emisión.
   Se establece unicidad por `registroMortalidadAnormalId` para que una reentrega no duplique la alerta del mismo evento.
3. La prioridad se deriva de la severidad del evento: `ALTA`, `CRITICA` o `EMERGENCIA_SANITARIA`.
4. La alerta se crea sincrónicamente a través de un puerto público dentro del mismo límite transaccional local coordinado por Plan 007. Kafka/outbox publica hechos ya confirmados; no se usa entrega asíncrona para satisfacer la atomicidad de alta.
5. La consulta consolida atributos vigentes de galpón y lote con los datos históricos del evento anormal. La edad se calcula a la fecha de consulta.
6. Este plan no registra diagnósticos, prescribe medicamentos, ordena sacrificios ni resuelve/cierra alertas.

### Alcance documental

- Este plan implementa las dos historias de SPEC-018.
- [Plan 007](007-MortalidadAlertasSanitariasYActualizacionDelInventarioVivo.md) conserva mortalidad y solicita la creación de alertas.
- [Plan 014](014-SolicitudesYValidacionDeAislamiento.md) gestiona solicitud/validación; [Plan 015](015-DiagnosticoYReintegroSanitario.md) gestiona diagnóstico.
- Los datos de Galpón y Lote se consultan al Módulo 1 mediante sus puertos públicos.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/018-GenerarAlertaSanitaria.md
└── plan/013-GestionDeAlertasSanitarias.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/model/alerta/
│   ├── AlertaSanitaria.java
│   ├── PrioridadAlerta.java
│   └── EstadoAlertaSanitaria.java
├── domain/port/out/alerta/
│   ├── AlertaSanitariaRepositoryPort.java
│   ├── MortalidadAnormalQueryPort.java
│   ├── GalponQueryPort.java
│   └── LoteQueryPort.java
├── application/alerta/
│   ├── GenerarAlertaSanitariaUseCase.java
│   ├── ConsultarAlertasSanitariasUseCase.java
│   └── ConsultarDetalleAlertaSanitariaUseCase.java
└── infrastructure/
    ├── adapter/in/rest/alerta/AlertaSanitariaController.java
    ├── adapter/out/internal/alerta/
    ├── adapter/out/persistence/alerta/
    └── config/AlertaSanitariaBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/alerta/
├── application/alerta/
├── infrastructure/adapter/in/rest/alerta/
└── integration/AlertaSanitariaFlowIntegrationTest.java
```

**Structure Decision**: El agregado de alerta y el historial son propiedad de este plan. Galpón, lote y mortalidad anormal se leen por puertos; no se duplican sus tablas o agregados.

### Entidades y relación

```text
RegistroMortalidadAnormal (Plan 007) 1 ─── 1 AlertaSanitaria

AlertaSanitaria
├── id: UUID
├── registroMortalidadAnormalId: UUID
├── galponId, loteId: UUID (referencias externas)
├── fechaHoraEmision: Instant
├── prioridad: ALTA | CRITICA | EMERGENCIA_SANITARIA
├── estado: PENDIENTE_ATENCION_VETERINARIA
└── destinatarioRol: ROLE_VETERINARIO
```

Cada alerta es un registro independiente e inmutable. El contexto mostrado se obtiene del evento y de consultas vigentes al Módulo 1.

### Contratos de los puertos

| Puerto | Responsabilidad |
| --- | --- |
| `AlertaSanitariaRepositoryPort` | Persistir alerta inmutable; consultar bandeja paginada y alerta por ID. |
| `MortalidadAnormalQueryPort` | Validar y recuperar causa, observaciones, severidad, población y actor del evento origen. |
| `GalponQueryPort` / `LoteQueryPort` | Obtener contexto actual y edad del lote. |
| `IntegrationEventPublisherPort` | Publicar alerta confirmada mediante outbox según el contrato de integración acordado. |

**Puerto público de entrada**: `GenerarAlertaSanitariaPort.generar(datosEvento)` recibe el ID y los atributos mínimos del evento anormal, y devuelve el ID de alerta. Plan 007 lo invoca sincrónicamente; la transacción compartida y sus límites deben verificarse en Setup.

### Contratos HTTP propuestos

| Método y ruta | Acceso | Responsabilidad |
| --- | --- | --- |
| `GET /api/alertas-sanitarias` | `ROLE_VETERINARIO`, `ROLE_ADMINISTRADOR` | Consultar bandeja filtrada por estado/prioridad y paginada. |
| `GET /api/alertas-sanitarias/{alertaId}` | `ROLE_VETERINARIO`, `ROLE_ADMINISTRADOR` | Consultar alerta con contexto del galpón, lote y reporte anormal. |

La alta no tiene endpoint público: solo se invoca por el puerto desde Plan 007. Errores siguen RFC 9457 y General.md.

### JSON común de errores

```json
{
  "type": "https://avicontrol/errors/alerta-no-encontrada",
  "title": "Alerta sanitaria no encontrada",
  "status": 404,
  "detail": "No existe una alerta con el identificador indicado",
  "instance": "/api/alertas-sanitarias/{alertaId}",
  "code": "ALERTA_SANITARIA_NO_ENCONTRADA",
  "correlationId": "uuid",
  "fieldErrors": []
}
```

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Acordar el puerto sincrónico, frontera transaccional, consultas y seguridad.

- [ ] T001 Acordar con Plan 007 el contrato del puerto, atributos mínimos del reporte y manejo de fallo de creación.
- [ ] T002 Confirmar y probar que la creación de alerta comparte la transacción local de mortalidad anormal y actualización de población, con rollback completo ante fallo.
- [ ] T003 Acordar contratos de GalponQueryPort/LoteQueryPort y datos de edad/aforo con Módulo 1.
- [ ] T004 Confirmar roles, Clock, formato RFC 9457 y contrato del evento de integración.
- [ ] T005 Preparar fixtures de alertas múltiples, severidades, lote con cero aves y galpón/lote inexistente.

**Checkpoint**: Atomicidad, datos y roles acordados con productores y consumidores.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Preparar dominio, persistencia y puertos propios.

- [ ] T006 Implementar `AlertaSanitaria` con prioridad, estado inicial e inmutabilidad.
- [ ] T007 Definir puertos de persistencia, mortalidad anormal y contexto de Módulo 1.
- [ ] T008 Crear migración para alertas con índice único por evento de origen e índices por lote, galpón, estado y fecha.
- [ ] T009 Implementar adaptador de persistencia, mapper y puerto público de generación.
- [ ] T010 Configurar publicación outbox posterior a confirmación y autorización REST.

**Checkpoint**: Generación y consulta quedan disponibles sin copiar datos propietarios.

## Phase 3: User Story 1 — Generar automáticamente una alerta sanitaria (Priority: P1)

**Spec**: SPEC-018, historia 1.

**Goal**: Crear una alerta individual por cada mortalidad anormal confirmada, en estado pendiente y con prioridad correspondiente.

**Independent Test**: Confirmar dos eventos anormales para un mismo galpón crea dos alertas independientes; evento inválido o fallo de persistencia revierte mortalidad, actualización de población y alerta como una sola operación.

### Definición del evento y puerto

El Plan 007 llama `GenerarAlertaSanitariaPort` con `registroMortalidadAnormalId`, `galponId`, `loteId`, prioridad/severidad, porcentaje, causa, observaciones y actor. `AlertaSanitariaGeneradaEvent` se publica luego del commit con `alertaId`, IDs de lote/galpón/origen, prioridad, estado y `fechaHoraEmision`.

### Tests e implementación

- [ ] T011 Probar estado inicial, mapeo de prioridad y creación independiente por evento.
- [ ] T012 Probar rechazo si no existe registro de mortalidad anormal.
- [ ] T013 Probar rollback completo ante fallo y que los reintentos no dupliquen la alerta.
- [ ] T014 Implementar entidad, caso de uso, puerto de entrada y persistencia.
- [ ] T015 Publicar evento confiable posterior a la confirmación.

**Checkpoint**: Cada evento anormal confirmado genera exactamente una alerta pendiente.

## Phase 4: User Story 2 — Consultar alertas con contexto para triaje veterinario (Priority: P2)

**Spec**: SPEC-018, historia 2.

**Goal**: Permitir que veterinario y administrador autorizado consulten bandeja y detalle consolidado con datos del evento, galpón y lote.

**Independent Test**: Veterinario consulta alerta y recibe prioridad/estado, galpón (nombre, aforo, estado), lote (nombre, población vigente, fecha de ingreso y edad), cantidad, porcentaje, severidad, causa, observaciones y actor. Usuario trabajador recibe 403; alerta inexistente, 404.

### Definición del endpoint REST

`GET /api/alertas-sanitarias?estado=PENDIENTE_ATENCION_VETERINARIA&page=0&size=20` devuelve resumen paginado ordenado por emisión descendente. `GET /api/alertas-sanitarias/{alertaId}` devuelve contexto consolidado sin modificar estado.

### JSON de respuesta de detalle

```json
{
  "alertaId": "uuid",
  "fechaHoraEmision": "2026-10-25T12:15:30Z",
  "prioridad": "ALTA",
  "estado": "PENDIENTE_ATENCION_VETERINARIA",
  "galpon": {"galponId":"uuid","nombre":"Galpón 1","aforoMaximo":8000,"estado":"PRODUCTIVO"},
  "lote": {"loteId":"uuid","nombre":"Lote 1","poblacionActual":4982,"edadEnDias":38},
  "mortalidadAnormal": {"registroId":"uuid","cantidadMuertes":18,"porcentajeMortalidad":0.36,"nivelSeveridad":"ALTA","causaProbable":"Problemas digestivos","observaciones":"Inspección matutina","reportadoPor":"uuid"}
}
```

### Tests e implementación

- [ ] T016 Probar filtros, paginación, orden descendente y lista vacía.
- [ ] T017 Probar ensamblado de contexto y alerta asociada a lote ya no activo.
- [ ] T018 Probar roles de veterinario/administrador y rechazo de trabajador.
- [ ] T019 Implementar casos de consulta, DTOs, mappers y endpoints.

**Checkpoint**: Veterinario dispone de contexto completo sin editar ni cerrar la alerta.

## Phase 5: Polish & Cross-Cutting Concerns

- [ ] T020 Documentar los dos endpoints y el puerto interno en OpenAPI/documentación de arquitectura.
- [ ] T021 Verificar atomicidad/idempotencia, publicación outbox y límites de módulos.
- [ ] T022 Medir objetivos de consulta y completar criterios de SPEC-018.

## Dependencies & Execution Order

### Phase Dependencies

- Setup precede a Foundational y ambas historias.
- Generación depende de mortalidad anormal del Plan 007 y del límite transaccional acordado.
- Consulta depende de alertas persistidas y puertos vigentes de Módulo 1.
- Polish depende de generación, consulta y acuerdos de integración.

### Dependencias con otros planes

- **Plan 007**: Productor de mortalidad anormal; invoca el puerto de generación.
- **Plan 001 / Módulo 1**: Provee contexto actual de galpón y lote.
- **Plan 014**: Puede usar alertas como contexto; **Plan 015**: registra diagnóstico y toma decisiones clínicas.
- **Plan 008**: Puede ejecutar decisiones de sacrificio desde su flujo propio.
- **General.md**: Modularidad, permisos, eventos, errores y transacciones.

### Dentro de cada User Story

- Validar el evento origen y reglas de prioridad antes de persistir.
- Mantener los datos históricos de origen y consultar por puertos el contexto vigente.
- Asegurar idempotencia/atomicidad en generación y autorización en consulta.

## Notes

- SPEC-018 no incluye diagnóstico, prescripción, cierre ni eliminación de alertas.
- La generación es interna; no se expone POST al trabajador ni se permite alta manual.
- La consulta contextual usa IDs y datos de origen sin duplicar entidades de Plan 007 o Módulo 1.
