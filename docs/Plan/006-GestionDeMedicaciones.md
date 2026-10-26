# Implementation Plan: Gestión de Medicaciones y Consulta Diaria

**Date**: 02/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [008-RegistrarMedicacion.md](../specs/008-RegistrarMedicacion.md)
- [015-ConsultarMedicacionPorGalponPorDia.md](../specs/015-ConsultarMedicacionPorGalponPorDia.md)

## Summary

Implementar el registro y edición de medicaciones, la consulta diaria de tratamientos por galpón y el resumen consolidado permitido por rol. El registro del consumo real y su descuento de existencias pertenecen al Plan 012.

La medicación prescrita y el consumo real son conceptos diferentes. Registrar o editar una medicación no descuenta inventario. El consumo real se especifica en [Plan 012](012-RegistrarConsumoDeMedicamentos.md).

Galpon y Lote continúan siendo entidades propietarias del Módulo 1. Diagnóstico y enfermedad pertenecen a las capacidades sanitarias relacionadas. Medicamento, recepción, existencias, unidad base y precio de compra pertenecen al inventario del Plan 002. Este plan consulta esas capacidades mediante puertos internos y no crea copias autoritativas.

## Technical Context

**Performance Goals**: El 95 % de los registros de medicación queda disponible en máximo 2 segundos después de su confirmación. Las consultas diarias responden en máximo 2 segundos sin realizar escrituras.

**Constraints**: Solo el veterinario registra o edita medicaciones. La consulta diaria es de solo lectura. La medicación debe referenciar una enfermedad disponible y un medicamento existente en inventario.

**Scale/Scope**: Cuatro historias de usuario, catálogo de medicaciones, consultas diarias individuales y consolidadas, eventos y adaptadores internos.

**Dependencias funcionales**: enfermedades del Plan 005 y diagnósticos del Plan 015; Galpón y Lote del Módulo 1; catálogo de medicamentos del Plan 002; autenticación, actor, reloj y errores de General.md.

### Decisiones específicas

1. **Medicación**: Medicacion es una entidad propia con enfermedadId, medicamentoId, dosis, diasTotales y descripcion. Todos son obligatorios. Medicamento y enfermedad deben volver a validarse al confirmar.
2. **Edición histórica**: Editar una medicación solo afecta diagnósticos futuros. Un diagnóstico conserva la dosis, duración, medicamento y demás valores utilizados al registrarse.
3. **Inventario**: Registrar o editar una medicación no cambia existencias. El Plan 012 coordina el consumo real con el Plan 010, que registra los descuentos.
4. **Consultas diarias**: La consulta toma la fecha actual del Clock compartido y devuelve los tratamientos vigentes de esa fecha. No modifica diagnósticos ni inventario.
5. **Galpones y trabajadores**: Un trabajador autorizado puede consultar cualquier galpón existente por `galponId`; el resumen consolida únicamente los galpones que indique para esa consulta. La autorización se determina por el rol.
6. **Dosis**: La dosis es una instrucción textual; calendarios de días alternos o dosis variables requieren ampliar SPEC-008 antes de implementar su cálculo.
7. **Eventos**: Las escrituras publican eventos después de confirmar la transacción local. Las consultas no publican eventos.
8. **Persistencia**: Las migraciones utilizan la siguiente versión disponible del repositorio y no fijan números que puedan colisionar con el Plan 002.

### Alcance documental

- El Plan 015 consulta MedicacionQueryPort al registrar diagnósticos. El Plan 006 implementa ese puerto; Plan 005 provee el catálogo de enfermedades.
- El Plan 002 es dueño del catálogo de medicamentos; este plan solo lo consulta mediante un puerto público. El Plan 010 es dueño de las existencias y el Plan 012 registra su consumo.
- El Plan 012 es propietario del consumo real y de los detalles históricos para el costeo de Módulo 3.
- Los nombres definitivos de eventos y contratos deben acordarse con los consumidores antes de publicar integración.

## Project Structure

### Documentation (this feature)

    docs/
    ├── specs/
    │   ├── 008-RegistrarMedicacion.md
    │   ├── 015-ConsultarMedicacionPorGalponPorDia.md
    └── plan/
        └── 006-GestionDeMedicaciones.md

### Source Code (repository root)

    src/main/java/com/avicontrol/
    ├── domain/
    │   ├── model/medicamento/
    │   │   ├── Medicacion.java
    │   │   └── EstadoMedicacion.java
    │   ├── exception/medicamento/
    │   │   ├── MedicacionNoEncontradaException.java
    │   │   ├── MedicamentoNoDisponibleException.java
    │   │   └── EnfermedadNoEncontradaException.java
    │   └── port/out/medicamento/
    │       ├── MedicacionRepositoryPort.java
    │       ├── EnfermedadQueryPort.java
    │       ├── GalponQueryPort.java
    │       ├── LoteQueryPort.java
    │       └── IntegrationEventPublisherPort.java
    ├── application/medicamento/
    │   ├── RegistrarMedicacionUseCase.java
    │   ├── EditarMedicacionUseCase.java
    │   ├── ConsultarMedicacionDiariaUseCase.java
    │   ├── ConsultarResumenMedicacionDiariaUseCase.java
    │   └── result/
    │       ├── MedicacionResult.java
    │       ├── MedicacionDiariaResult.java
    │       ├── ResumenMedicacionDiariaResult.java
    └── infrastructure/
        ├── adapter/in/rest/medicamento/
        │   ├── MedicacionController.java
        │   ├── MedicacionDiariaController.java
        ├── adapter/out/internal/medicamento/
        │   ├── EnfermedadQueryAdapter.java
        │   ├── DiagnosticoQueryAdapter.java
        │   ├── GalponQueryAdapter.java
        │   ├── LoteQueryAdapter.java
        │   └── InventarioMedicamentoAdapter.java
        ├── adapter/out/persistence/medicamento/
        │   ├── MedicacionPersistenceAdapter.java
        └── config/
            └── MedicamentoBeanConfiguration.java

    src/test/java/com/avicontrol/
    ├── domain/model/medicamento/
    ├── application/medicamento/
    ├── infrastructure/adapter/in/rest/medicamento/
    ├── infrastructure/adapter/out/internal/medicamento/
    └── integration/MedicamentoIntegrationTest.java

**Structure Decision**: Las reglas de medicación y consulta se mantienen en dominio y aplicación. Los controladores solo traducen HTTP; las consultas a otras capacidades usan puertos públicos.

### Entidades y relación

    Medicacion
    ├── id: UUID
    ├── enfermedadId: UUID
    ├── medicamentoId: UUID
    ├── dosis: String
    ├── diasTotales: Integer
    └── descripcion: String

Medicacion puede editarse para tratamientos futuros. El diagnóstico conserva los valores usados aunque cambie posteriormente una medicación.

### Contratos de los puertos

| Puerto | Responsabilidad |
| --- | --- |
| MedicacionRepositoryPort | Crear, buscar, editar y listar medicaciones disponibles. |
| EnfermedadQueryPort | Confirmar que la enfermedad existe y está disponible. |
| GalponQueryPort | Consultar datos actuales para las respuestas diarias. |
| LoteQueryPort | Consultar lote activo, población y edad para las respuestas diarias. |
| IntegrationEventPublisherPort | Publicar eventos de medicación según contratos acordados con Plan 015. |

### Contratos HTTP propuestos

| Endpoint | Acceso | Resultado |
| --- | --- | --- |
| POST /api/medicaciones | Veterinario | Registra una medicación. |
| PUT /api/medicaciones/{medicacionId} | Veterinario | Edita una medicación para diagnósticos futuros. |
| GET /api/galpones/{galponId}/medicacion-diaria | Trabajador autorizado, administrador autorizado | Consulta tratamientos vigentes para la fecha actual. |
| GET /api/medicacion-diaria | Trabajador autorizado, administrador autorizado | Consulta el resumen consolidado permitido por el rol. |

Las consultas responden 200 sin tratamientos con el mensaje Sin medicación activa para hoy. Las escrituras responden 201 al crear y 200 al editar. Las respuestas de error utilizan application/problem+json.

### JSON común de errores

Los errores de validación y autorización usan application/problem+json según General.md.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Acordar roles, estados y dependencias de diagnóstico y catálogo de medicamentos.

- [ ] T001 Contrastar los estados del tratamiento y la forma de obtener el diagnóstico vigente con el Plan 015.
- [ ] T002 Confirmar que la SPEC-015 permite consultar cualquier galpón existente a los trabajadores autorizados por rol.
- [ ] T003 Acordar roles y permisos para veterinario, trabajador/operario y administrador autorizado.
- [ ] T004 Confirmar con Plan 002 el contrato de consulta del catálogo de medicamentos.

- [ ] T005 Definir cómo se representa una dosis diaria, días alternos, periodos de descanso y cantidades variables; si no se necesitan, documentar dosis como instrucción textual.
- [ ] T006 Preparar fixtures de enfermedad, medicación, diagnósticos y galpones con y sin tratamiento.

**Checkpoint**: Contratos externos y permisos definidos para las consultas diarias.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Preparar dominio, persistencia y puertos para registro, edición y consulta.

- [ ] T008 Implementar Medicacion con validación de enfermedad, medicamento, dosis, días positivos y descripción.
- [ ] T009 Implementar los modelos y reglas de Medicacion.
- [ ] T010 Definir excepciones y puertos para enfermedad, galpón y lote.
- [ ] T011 Crear migraciones propias de medicación usando la siguiente versión Flyway disponible.
- [ ] T012 Implementar adaptadores internos sin acceder a repositorios JPA privados de otros módulos.
- [ ] T013 Configurar transacciones locales y publicación confiable de eventos.

**Checkpoint**: Modelo, persistencia y contratos listos para prescripción y consulta.

## Phase 3: User Story 1 — Registrar una Medicación (Priority: P1)

**Spec**: 008, historia 1.

**Goal**: El veterinario crea una medicación válida asociada a una enfermedad y un medicamento del inventario, sin descontar existencias.

**Independent Test**: Se registra una medicación con datos completos; una enfermedad o medicamento no disponible y un usuario no veterinario son rechazados.

### Definición del evento para User Story 1

**Evento producido**: MedicacionRegistrada.

**Evento consumido**: Ninguno.

Se publica después de guardar todas las relaciones. Incluye eventId, eventVersion, occurredAt, producer, correlationId, aggregateId, medicacionId, enfermedadId, medicamentoId, dosis, diasTotales y descripcion.

### Definición del endpoint REST para User Story 1

| Elemento | Definición |
| --- | --- |
| Método y ruta | POST /api/medicaciones |
| Autorización | ROLE_VETERINARIO |
| Entrada | Enfermedad, medicamento, dosis, días totales positivos y descripción. |
| Respuesta 201 | Medicación creada y disponible para diagnósticos futuros. |
| Errores | 400 por datos faltantes o duración inválida, 403 por rol, 404 por enfermedad/medicamento y 503 si no se puede confirmar la escritura. |

#### JSON de solicitud

```json
    {
      "enfermedadId": "uuid",
      "medicamentoId": "uuid",
      "dosis": "200 g diluidos en 100 L de agua potable",
      "diasTotales": 5,
      "descripcion": "Administrar por agua de bebida"
    }
```

#### JSON de respuesta

```json
    {
      "medicacionId": "uuid",
      "enfermedadId": "uuid",
      "medicamentoId": "uuid",
      "dosis": "200 g diluidos en 100 L de agua potable",
      "diasTotales": 5,
      "descripcion": "Administrar por agua de bebida"
    }
```

### Tests e implementación

- [ ] T015 [US1] Probar campos obligatorios, espacios, días positivos y rol veterinario.
- [ ] T016 [US1] Probar validación de enfermedad y medicamento disponibles.
- [ ] T017 [US1] Probar que registrar una medicación no modifica existencias.
- [ ] T018 [US1] Implementar RegistrarMedicacionUseCase, persistencia, evento y endpoint.

**Checkpoint**: La medicación se encuentra disponible para diagnósticos futuros sin tocar inventario.

## Phase 4: User Story 2 — Editar una Medicación (Priority: P2)

**Spec**: 008, historia 2.

**Goal**: El veterinario actualiza una medicación solo para diagnósticos futuros.

**Independent Test**: Una medicación utilizada por un diagnóstico se edita; el diagnóstico anterior conserva su duración y fecha de reintegro.

### Definición del evento para User Story 2

**Evento producido**: MedicacionActualizada.

**Evento consumido**: Ninguno.

El evento comunica la nueva versión. No recalcula diagnósticos ni fechas ya registradas.

### Definición del endpoint REST para User Story 2

| Elemento | Definición |
| --- | --- |
| Método y ruta | PUT /api/medicaciones/{medicacionId} |
| Autorización | ROLE_VETERINARIO |
| Entrada | Enfermedad, medicamento, dosis, días, descripción y versión esperada. |
| Respuesta 200 | Medicación actualizada. |
| Errores | 400, 403, 404 y 409 por concurrencia o versión incompatible. |

#### JSON de solicitud

```json
    {
      "enfermedadId": "uuid",
      "medicamentoId": "uuid",
      "dosis": "250 g diluidos en 100 L de agua potable",
      "diasTotales": 7,
      "descripcion": "Administrar por agua de bebida",
      "version": 2
    }
```

### Tests e implementación

- [ ] T019 [US2] Probar edición válida y rechazo por rol.
- [ ] T020 [US2] Probar que diagnósticos existentes conservan valores históricos.
- [ ] T021 [US2] Implementar EditarMedicacionUseCase, control de versión, evento y endpoint.

**Checkpoint**: La edición no modifica tratamientos ya diagnosticados.

## Phase 5: User Story 3 — Consultar Medicación Diaria por Galpón (Priority: P1)

**Spec**: 015, historias 1 y 2.

**Goal**: El trabajador consulta el tratamiento vigente para un galpón en la fecha actual y recibe un resultado explícito cuando no existe medicación activa.

**Independent Test**: Un diagnóstico vigente devuelve enfermedad, medicamento, dosis, vía y avance; un tratamiento finalizado devuelve Sin medicación activa para hoy.

### Definición del evento para User Story 3

**Evento producido**: Ninguno.

**Evento consumido**: Ninguno obligatorio. La consulta obtiene el diagnóstico, galpón y lote vigentes mediante puertos internos sin crear una proyección.

La consulta no descuenta inventario, no registra consumo y no publica MedicacionConsultada.

### Definición del endpoint REST para User Story 3

| Elemento | Definición |
| --- | --- |
| Método y ruta | GET /api/galpones/{galponId}/medicacion-diaria |
| Autorización | ROLE_TRABAJADOR o administrador autorizado |
| Entrada | galponId y sin parámetro de fecha; siempre utiliza la fecha actual del Clock compartido. |
| Respuesta 200 con tratamiento | Galpón, lote, enfermedad, medicamento, dosis del día, vía, día actual y días totales. |
| Respuesta 200 sin tratamiento | Datos disponibles del galpón/lote y estado Sin medicación activa para hoy, sin cantidad aplicable. |
| Errores | 400 por identificador inválido, 404 por galpón inexistente, 409 por datos inconsistentes y 503 por dependencia no disponible. |

#### JSON de respuesta con tratamiento

```json
    {
      "galponId": "uuid",
      "galponNombre": "Galpón 5",
      "loteId": "uuid",
      "loteNombre": "LDP-005",
      "poblacionActual": 7960,
      "edadDias": 17,
      "tratamientos": [
        {
          "diagnosticoId": "uuid",
          "enfermedad": "Coccidiosis aviar",
          "medicamento": "Amprolio 20% Solución",
          "dosisHoy": "200 g diluidos en 100 L de agua potable",
          "viaAdministracion": "Agua de bebida en bebederos automáticos",
          "diaActual": 2,
          "diasTotales": 5
        }
      ],
      "estado": "MEDICACION_ACTIVA"
    }
```

#### JSON de respuesta sin tratamiento

```json
    {
      "galponId": "uuid",
      "galponNombre": "Galpón 2",
      "loteId": null,
      "tratamientos": [],
      "estado": "SIN_MEDICACION_ACTIVA_HOY",
      "mensaje": "Sin medicación activa para hoy"
    }
```

### Tests e implementación

- [ ] T022 [US3] Probar tratamiento activo, tratamiento finalizado, ausencia de diagnóstico y lote sin población.
- [ ] T023 [US3] Probar cálculo de día actual frente a días totales y diagnóstico concurrente.
- [ ] T024 [US3] Probar que la consulta no escribe ni descuenta inventario.
- [ ] T025 [US3] Implementar ConsultarMedicacionDiariaUseCase, resultados, DTOs y endpoint.

**Checkpoint**: La consulta diaria solo lee datos vigentes y no genera consumos.

## Phase 6: User Story 4 — Consultar Resumen Consolidado Diario (Priority: P2)

**Spec**: 015, historia 3.

**Goal**: El trabajador obtiene el total de tratamientos activos y el desglose diario de los galpones existentes que seleccionó para esa consulta.

**Independent Test**: Con uno, varios o ningún tratamiento vigente, el resumen muestra conteos y tarjetas coherentes sin incluir galpones fuera del universo autorizado.

### Definición del evento para User Story 4

**Evento producido**: Ninguno.

**Evento consumido**: Ninguno obligatorio.

El resumen es una agregación de lectura. No se persiste ni publica un evento de consulta.

### Definición del endpoint REST

| Elemento | Definición |
| --- | --- |
| Método y ruta | GET /api/medicacion-diaria |
| Autorización | ROLE_TRABAJADOR o administrador autorizado |
| Respuesta 200 | totalGalponesConTratamiento, totalTratamientos y tarjetas con galpón, lote, avance, enfermedad, medicamento, dosis y vía. |
| Listado vacío | 0 galpones, 0 tratamientos y mensaje Sin tratamientos hoy. |
| Errores | 401/403 por seguridad, 409 por inconsistencias y 503 por dependencia no disponible. |

#### JSON de respuesta

```json
    {
      "totalGalponesConTratamiento": 1,
      "totalTratamientos": 1,
      "resumen": "Amprolio 20% en Galpón 5",
      "tratamientos": [
        {
          "galponId": "uuid",
          "galponNombre": "Galpón 5",
          "loteId": "uuid",
          "loteNombre": "LDP-005",
          "avance": "Día 2 de 5",
          "enfermedad": "Coccidiosis aviar",
          "medicamento": "Amprolio 20% Solución",
          "dosisHoy": "200 g diluidos en 100 L de agua potable",
          "viaAdministracion": "Agua de bebida en bebederos automáticos"
        }
      ]
    }
```

### Tests e implementación

- [ ] T026 [US4] Probar uno, varios y cero tratamientos, con conteos y tarjetas correctas.
- [ ] T027 [US4] Probar que no se aplica filtro por asignación inexistente y que se respetan permisos.
- [ ] T028 [US4] Probar que múltiples tratamientos del mismo galpón aparecen separados.
- [ ] T029 [US4] Implementar ConsultarResumenMedicacionDiariaUseCase y endpoint consolidado.

**Checkpoint**: El resumen se calcula en lectura sin crear una proyección ni modificar datos.

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Verificar contratos, seguridad y rendimiento de registro y consulta de medicaciones.

- [ ] T030 Documentar endpoints, roles y errores en OpenAPI.
- [ ] T031 Completar pruebas de integración de registro, edición y consultas diarias.
- [ ] T032 Verificar que registrar o editar medicación no descuente existencias ni modifique diagnósticos existentes.
- [ ] T033 Verificar que las consultas diarias sean de solo lectura y acepten cualquier galpón existente para roles autorizados.
- [ ] T034 Verificar que Plan 015 consuma MedicacionQueryPort sin duplicar el catálogo.
- [ ] T035 Extender comprobaciones arquitectónicas conforme a General.md.
- [ ] T036 Medir objetivos de respuesta del resumen consolidado.
## Dependencies & Execution Order

### Phase Dependencies

- **Setup**: Acordar disponibilidad de enfermedad y medicamento, roles y estados de tratamiento.
- **Foundational**: Depende de Setup y habilita registro, edición y consulta.
- **US1 y US2**: Definen la medicación que consume el diagnóstico del Plan 015.
- **US3 y US4**: Dependen de diagnóstico, galpón, lote y reloj; son consultas de solo lectura.
- **Polish**: Depende de los contratos y las cuatro historias.

### Dependencias con otros planes

- **Plan 002**: Proporciona el catálogo de medicamentos.
- **Plan 005**: Proporciona enfermedades. **Plan 015**: Proporciona diagnósticos y consume MedicacionQueryPort.
- **Plan 012**: Registra el consumo real mediante el puerto de inventario del Plan 010.
- **Plan 007**: Puede cambiar población viva; las consultas leen la población vigente.
- **Módulo 1**: Proporciona Galpón y Lote.
- **General.md**: Proporciona seguridad, actor, reloj, errores y eventos.

### Dentro de cada User Story

- Entidades y reglas de dominio antes que casos de uso.
- Puertos antes que adaptadores.
- Casos de uso antes que controladores.
- Pruebas junto con implementación y checkpoint antes de cerrar la historia.
## Notes

- T001 a T036 identifican tareas; US1 a US4 identifican las historias.
- Este documento describe componentes por implementar; no afirma que ya existan.
- La medicación prescrita no representa administración ni descuento de inventario.
- El Plan 012 obtiene diagnóstico, lote, galpón y medicamento desde el diagnóstico.
- La consulta de cualquier galpón existente depende del rol autorizado.
- Si se requieren días alternos, descansos o dosis variables, el contrato de Medicacion debe ampliarse antes de implementar la consulta diaria.
- Los eventos entre módulos deben tener contrato versionado, consumidor idempotente y trazabilidad con correlationId.