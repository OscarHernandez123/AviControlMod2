# Implementation Plan: Gestión de Enfermedades, Aislamiento y Diagnóstico

**Date:** 02/10/2026  
**Arquitectura y tecnologías:** `General.md`  
**Specs:**
- `009-RegistrarEnfermedad.md`
- `020-SolicitarValidacionParaAislarGalpon.md`
- `010-ValidarAislamiento.md`
- `011-DiagnosticarGalpon.md`

---

## 1. Summary

Implementar el catálogo de enfermedades, el registro y envío de solicitudes para aislar un galpón, la evaluación y validación veterinaria del aislamiento, y el diagnóstico posterior del galpón aislado. Para enfermedades tratables, el diagnóstico consulta la medicación seleccionada y calcula la fecha de reintegro a producción (`LocalDate`); para enfermedades que requieren sacrificio sanitario, habilita la emisión de la orden correspondiente sin crear una fecha de reintegro.

El plan conserva como historial las enfermedades, solicitudes, decisiones y diagnósticos. `Galpon` y `Lote` continúan siendo entidades propietarias del Módulo 1; este plan utiliza sus identificadores y puertos públicos. El cambio del estado operativo del galpón se solicita mediante un puerto de salida del Módulo 1 y no mediante una segunda copia persistida de `Galpon`.

No se implementan aquí el catálogo ni el consumo de medicamentos, la mortalidad, la actualización del inventario vivo ni la orden o ejecución del sacrificio sanitario. El plan publica los hechos que esos procesos necesitan y deja sus operaciones a los planes correspondientes.

---

## 2. Technical Context

- **Performance Goals:** El 95 % de los registros, validaciones y diagnósticos válidos queda disponible en máximo 1 segundo después de su confirmación. La transición automática de reintegro procesa los diagnósticos vencidos sin duplicar cambios de estado mediante bloqueo optimista e idempotencia transaccional.
- **Constraints:** Solo el veterinario registra o edita enfermedades, valida aislamientos y registra diagnósticos. El trabajador autorizado puede crear solicitudes, pero no puede cambiar el estado del galpón. Las solicitudes son inmutables después de enviarse. La validación consulta nuevamente el estado vigente del Módulo 1 antes de cambiarlo. El diagnóstico solo procede con estado `AISLAMIENTO` y lote activo.
- **Scale/Scope:** Seis historias de usuario, un catálogo de enfermedades, solicitudes y decisiones de aislamiento, diagnósticos, cuatro operaciones principales de escritura, una consulta de solicitudes pendientes, eventos internos, adaptadores internos y una tarea programada segura para reintegros.
- **Dependencias funcionales:** Galpones y lotes del Módulo 1; catálogo y consulta de medicamentos del Plan 006; orden de sacrificio sanitario del Plan 008; autenticación, actor, reloj y formato de errores definidos en `General.md`.

---

## 3. Decisiones Específicas

1. **Enfermedad:** Entidad propia del módulo sanitario con UUID, código, nombre, nivel de riesgo, tipo, descripción clínica y `requiereSacrificioSanitario`. Todos los campos son obligatorios; el indicador debe recibirse explícitamente y no tiene valor predeterminado.
2. **Versionado de decisiones y Snapshots:** Editar una enfermedad solo afecta diagnósticos futuros. Un diagnóstico conserva una instantánea (`snapshot`) de los datos (`nombreEnfermedadHistorico`, `requiereSacrificioHistorico`) y la decisión utilizados al confirmarse.
3. **Galpón y lote:** No se duplican ni se crean tablas paralelas para `Galpon` o `Lote`. La solicitud y el diagnóstico almacenan sus UUID, población actual y edad capturadas en el momento requerido.
4. **Asignación de trabajadores:** No se consulta ni se valida una asignación trabajador-galpón. Cualquier trabajador con el permiso correspondiente puede crear una solicitud para cualquier galpón que cumpla las condiciones.
5. **Solicitud:** Solo se crea para un galpón en estado `En cosecha`, con lote activo y población actual mayor que cero. Una solicitud pendiente por galpón impide otra solicitud pendiente. Una solicitud enviada no se edita ni se retira.
6. **Validación:** El veterinario debe tener una solicitud previa en estado `PENDIENTE`. Antes de completar la validación se consulta nuevamente el estado del galpón en el Módulo 1. Solo se permite la transición `productiva` a `aislamiento`.
7. **Diagnóstico:** El diagnóstico se registra para el galpón que continúa en `aislamiento` y para el lote alojado al momento de confirmar. Una enfermedad tratable exige una medicación compatible; una enfermedad que requiere sacrificio no acepta medicación ni calcula reintegro.
8. **Reintegro y Manejo de Concurrencia:** La fecha de reintegro es `fechaDiagnostico + diasTratamiento` (calculado en `LocalDate`). Un proceso programado (`Scheduler`) busca diagnósticos vencidos (`LocalDate.now() >= fechaReintegro`) aplicando bloqueo o exclusión por estado para evitar condiciones de carrera. Solicita la transición de `aislamiento` a `productiva` solo si el galpón conserva el estado `aislamiento`.
9. **Puertos:** Se reutilizan `GalponQueryPort` y `LoteQueryPort` si ya están publicados por la capacidad de consulta. No se agrega un `Modulo1QueryPort` redundante. `CambiarEstadoOperativoGalponPort` representa una operación distinta y sí es necesario.
10. **Eventos:** Las escrituras publican eventos internos después de confirmar la transacción local. Los eventos que atraviesan límites de módulo usan `IntegrationEventPublisherPort` y outbox. Las consultas no publican eventos.
11. **Persistencia:** Las entidades sanitarias y sus relaciones se persisten en tablas propias (`san_enfermedades`, `san_solicitudes`, `san_diagnosticos`, `san_auditoria`, `san_outbox`). `Galpon` y `Lote` permanecen en la capacidad propietaria.
12. **Consistencia:** La validación y el diagnóstico deben leer el estado vigente justo antes de confirmar. Si una dependencia no está disponible o entrega datos incompletos, se rechaza la escritura y no se guardan registros parciales.

---

## 4. Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   ├── 009-RegistrarEnfermedad.md
│   ├── 020-SolicitarValidacionParaAislarGalpon.md
│   ├── 010-ValidarAislamiento.md
│   └── 011-DiagnosticarGalpon.md
└── plan/
    └── 005-GestionDeEnfermedadesAislamientoYDiagnostico.md

    Source Code (repository root)

    src/main/java/com/avicontrol/
├── domain/
│   ├── model/sanitario/
│   │   ├── enfermedad/
│   │   │   ├── Enfermedad.java
│   │   │   ├── TipoEnfermedad.java
│   │   │   └── NivelRiesgo.java
│   │   ├── aislamiento/
│   │   │   ├── SolicitudAislamiento.java
│   │   │   ├── EstadoSolicitudAislamiento.java
│   │   │   └── DecisionAislamiento.java
│   │   └── diagnostico/
│   │       ├── DiagnosticoGalpon.java
│   │       └── ResultadoDiagnostico.java
│   ├── exception/sanitario/
│   └── port/out/sanitario/
│       ├── EnfermedadRepositoryPort.java
│       ├── SolicitudAislamientoRepositoryPort.java
│       ├── DiagnosticoGalponRepositoryPort.java
│       ├── GalponQueryPort.java
│       ├── LoteQueryPort.java
│       ├── CambiarEstadoOperativoGalponPort.java
│       ├── MedicacionQueryPort.java
│       └── IntegrationEventPublisherPort.java
├── application/sanitario/
│   ├── RegistrarEnfermedadUseCase.java
│   ├── EditarEnfermedadUseCase.java
│   ├── SolicitarAislamientoUseCase.java
│   ├── ConsultarSolicitudesAislamientoUseCase.java
│   ├── ValidarAislamientoUseCase.java
│   ├── RegistrarDiagnosticoUseCase.java
│   ├── ProcesarReintegrosVencidosUseCase.java
│   └── result/
└── infrastructure/
    ├── adapter/in/rest/sanitario/
    │   ├── EnfermedadController.java
    │   ├── SolicitudAislamientoController.java
    │   ├── ValidacionAislamientoController.java
    │   └── DiagnosticoGalponController.java
    ├── adapter/in/scheduler/sanitario/
    │   └── ReintegroSanitarioScheduler.java
    ├── adapter/out/internal/sanitario/
    ├── adapter/out/persistence/sanitario/
    └── config/SanitarioBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/sanitario/
├── application/sanitario/
├── infrastructure/adapter/in/rest/sanitario/
├── infrastructure/adapter/out/internal/sanitario/
└── integration/SanitarioIntegrationTest.java

5. Entidades Clave (Modelado DDD)

+---------------------------------------------------------------------------------+
|                                 <<Aggregate Root>>                              |
|                                    Diagnostico                                  |
+---------------------------------------------------------------------------------+
| - id: DiagnosticoId (UUID)                                                      |
| - galponId: GalponId (UUID)                   [Ref. Externa - Módulo 1]         |
| - loteId: LoteId (UUID)                       [Ref. Externa - Módulo 1]         |
| - enfermedadId: EnfermedadId (UUID)           [Ref. Catálogo - Spec 009]        |
| - nombreEnfermedadHistorico: String           [Snapshot]                        |
| - requiereSacrificioHistorico: boolean        [Snapshot]                        |
| - medicacionId: MedicacionId [0..1] (UUID)    [Ref. Catálogo - Spec 008]        |
| - diasMedicacion: Integer [0..1]                                                |
| - veterinarioId: VeterinarioId (UUID)                                           |
| - fechaDiagnostico: LocalDate                                                   |
| - fechaReintegro: LocalDate [0..1]            [Calculado / Nullable]            |
| - observaciones: String [0..1]                                                  |
| - estado: EstadoDiagnostico                   [ACTIVO, CERRADO, HABILITA_SACRIFICIO] |
| - version: Long                               [@Version - Concurrencia]         |
+---------------------------------------------------------------------------------+

## 6. Contratos de Puertos

| Puerto | Responsabilidad |
| :--- | :--- |
| `EnfermedadRepositoryPort` | Crear, buscar, editar y listar enfermedades activas. |
| `SolicitudAislamientoRepositoryPort` | Crear solicitudes inmutables, buscar una solicitud, consultar pendientes por galpón y listar solicitudes para el veterinario. |
| `DiagnosticoGalponRepositoryPort` | Crear diagnósticos, buscar diagnósticos con reintegro vencido (`LocalDate`) y garantizar exclusión transaccional para evitar procesamiento duplicado. |
| `GalponQueryPort` | Consultar existencia y estado vigente del galpón en el Módulo 1. |
| `LoteQueryPort` | Obtener el lote activo y sus datos vigentes. |
| `CambiarEstadoOperativoGalponPort` | Solicitar la transición permitida del estado del galpón al Módulo 1. |
| `MedicacionQueryPort` | Consultar una medicación activa, compatible con la enfermedad, sus días totales y datos vigentes (Plan 006). |
| `IntegrationEventPublisherPort` | Publicar eventos versionados con `correlationId` para consumidores externos. |

## 7. Contratos HTTP y Comunicación REST

La arquitectura expone endpoints REST bajo el prefijo `/api/v1/sanitary` (o `/api/`) orientados a JSON utilizando `application/problem+json` para el manejo estándar de errores. Los métodos de comunicación e interacción cliente-servidor se estructuran de la siguiente forma:

| Endpoint | Método | Acceso | Propósito y Comunicación |
| :--- | :--- | :--- | :--- |
| `/api/enfermedades` | POST | `ROLE_VETERINARIO` | Registra una enfermedad con campos obligatorios explícitos. |
| `/api/enfermedades/{enfermedadId}` | PUT | `ROLE_VETERINARIO` | Actualiza atributos de la enfermedad para diagnósticos futuros. |
| `/api/galpones/{galponId}/solicitudes-aislamiento` | POST | `ROLE_TRABAJADOR` | Envía una solicitud de aislamiento sobre un galpón en cosecha. |
| `/api/solicitudes-aislamiento` | GET | `ROLE_VETERINARIO` | Consulta la bandeja de solicitudes pendientes de validación. |
| `/api/solicitudes-aislamiento/{solicitudId}/validacion` | POST | `ROLE_VETERINARIO` | Aprueba o rechaza la solicitud de aislamiento y muta el estado del galpón. |
| `/api/galpones/{galponId}/diagnosticos` | POST | `ROLE_VETERINARIO` | Registra diagnóstico clínico, calcula reintegro o habilita sacrificio. |

### Formato estándar de errores HTTP

Ante un conflicto de negocio (ej. galpón no aislado, estado incompatible, solicitud duplicada), la API responde con el código HTTP correspondiente (`409 Conflict`, `400 Bad Request`, `403 Forbidden`, `404 Not Found`) estructurado bajo el estándar *problem details*:

```json
{
  "type": "https://avicontrol/errors/operacion-sanitaria-no-permitida",
  "title": "Operación sanitaria no permitida",
  "status": 409,
  "detail": "El galpón debe estar en estado AISLAMIENTO",
  "instance": "/api/galpones/{galponId}/diagnosticos",
  "code": "ESTADO_GALPON_INCOMPATIBLE",
  "correlationId": "uuid",
  "fieldErrors": []
}

8. Plan de Ejecución por Fases (Tareas)
Phase 1: Setup (Shared Infrastructure)
T001: Contrastar los estados del galpón ofrecidos por Módulo 1 (productiva, aislamiento, en_cosecha, vaciado_sanitario).

T002: Confirmar que no existe asignación trabajador-galpón y registrar la regla en el spec 020.

T003: Acordar roles y permisos técnicos (ROLE_VETERINARIO, ROLE_TRABAJADOR).

T004: Acordar nombres y versiones de eventos de integración con Módulo 1, Plan 006 y Plan 008.

T005: Crear fixtures de prueba para galpón productivo, sin lote, población cero, aislado y diagnósticos vencidos (LocalDate).

T006: Alinear paquete raíz, actor autenticado, reloj (LocalDate), manejo de errores, outbox y transacciones con General.md [cite: 4].

Phase 2: Foundational (Blocking Prerequisites)
T007: Implementar la entidad de dominio Enfermedad, enums y validaciones en Java puro.

T008: Implementar SolicitudAislamiento, estados, inmutabilidad y validación de una solicitud pendiente por galpón.

T009: Implementar DiagnosticoGalpon, cálculo de fecha de reintegro (LocalDate) y estados de seguimiento [cite: 4].

T010: Definir excepciones de dominio y puertos de salida (RepositoryPort, QueryPort).

T011: Crear migraciones Flyway secuenciales para tablas sanitarias y outbox.

T012: Implementar adaptadores internos para galpón, lote y medicación respetando los límites de los módulos [cite: 4].

T013: Configurar transacciones locales (@Transactional), outbox e idempotencia de eventos vía X-Idempotency-Key.

T014: Verificar aislamiento estructural (el módulo sanitario no introduce tablas paralelas de Galpon o Lote) [cite: 4].

Phase 3: User Story 1 — Registrar una Enfermedad (Priority: P1)
T015: Probar validaciones de campos obligatorios, espacios y selección explícita del indicador de sacrificio.

T016: Probar atomicidad ante interrupciones en el registro de enfermedades.

T017: Probar publicación del evento EnfermedadRegistradaIntegrationEvent.

T018: Implementar RegistrarEnfermedadUseCase.

T019: Crear DTOs, mappers y resultados asociados.

T020: Implementar controlador REST POST /api/enfermedades.

Phase 4: User Story 2 — Editar una Enfermedad (Priority: P2)
T021: Probar edición válida y restricciones de seguridad por rol.

T022: Probar que diagnósticos anteriores conservan sus snapshots históricos intactos.

T023: Implementar EditarEnfermedadUseCase, endpoint PUT, control de versión optimista y evento EnfermedadActualizadaIntegrationEvent [cite: 1].

Phase 5: User Story 3 — Crear y Enviar Solicitud de Aislamiento (Priority: P1)
T024: Probar validaciones de galpón en cosecha, estados incompatibles y población mayor a cero.

T025: Probar captura correcta de población y edad vigentes del lote activo.

T026: Validar la ausencia de restricciones por asignación trabajador-galpón.

T027: Implementar SolicitarAislamientoUseCase, persistencia, evento AislamientoSolicitado y endpoint.

Phase 6: User Story 4 — Consultar Solicitudes Pendientes y Evitar Duplicados (Priority: P2)
T028: Probar restricción única por galpón ante peticiones concurrentes de aislamiento.

T029: Probar consulta paginada de solicitudes para el rol veterinario.

T030: Implementar consulta, restricciones de unicidad en base de datos y DTOs de paginación [cite: 2].

Phase 7: User Story 5 — Validar el Aislamiento (Priority: P1)
T031: Probar seguridad por rol y obligatoriedad de solicitud previa en estado PENDIENTE.

T032: Probar re-lectura síncrona del estado vigente del galpón justo antes de confirmar.

T033: Probar transición de estado a través de CambiarEstadoOperativoGalponPort.

T034: Implementar ValidarAislamientoUseCase, decisiones, eventos de validación/rechazo y endpoint REST [cite: 3].

Phase 8: User Story 6 — Registrar Diagnóstico y Gestionar Reintegro (Priority: P1)
T035: Probar restricciones de veterinario, galpón aislado, lote activo y enfermedad activa [cite: 4].

T036: Probar obligatoriedad y compatibilidad de medicación para enfermedades tratables [cite: 4].

T037: Probar reglas de negocio para patologías letales que exigen sacrificio (sin medicación ni reintegro) [cite: 4].

T038: Probar snapshots históricos y rechazo ante cambios de estado concurrentes del galpón [cite: 4].

T039: Implementar RegistrarDiagnosticoUseCase, MedicacionQueryPort, persistencia y eventos [cite: 4].

T040: Implementar ProcesarReintegrosVencidosUseCase y ReintegroSanitarioScheduler con control de concurrencia seguro basado en LocalDate [cite: 4].

T041: Probar reintegro idempotente, resiliencia ante caídas del microservicio y protección contra sobreescritura de galpones que ya no están aislados [cite: 4].

Phase 9: Polish & Cross-Cutting Concerns
T042: Documentar endpoints, roles, códigos de error y esquemas en OpenAPI.

T043: Completar pruebas de integración de punta a punta (SanitarioIntegrationTest) cubriendo el flujo completo.

T044: Verificar inmutabilidad histórica ante ediciones posteriores en el catálogo de enfermedades [cite: 1, 4].

T045: Verificar que las solicitudes no alteren estados por sí mismas y que las validaciones utilicen correctamente los puertos del Módulo 1.

T046: Comprobar idempotencia general en outbox, validaciones y reintegros del scheduler.

T047: Verificar integración limpia con el Plan 006 (consultas de medicación) [cite: 4].

T048: Verificar consumo correcto del evento de habilitación de sacrificio por parte del Plan 008 [cite: 4].

T049: Auditoría de arquitectura limpia (dominio libre de frameworks, adaptadores desacoplados de persistencia externa).

T050: Ejecutar suite completa de pruebas unitarias y de integración.

T051: Medir cumplimiento del objetivo de latencia (< 250 ms / < 1s) y ejecución limpia de tareas programadas.