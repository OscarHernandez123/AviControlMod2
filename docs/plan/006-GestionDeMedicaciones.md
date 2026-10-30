# Implementation Plan: Gestión de Medicaciones y Consulta Diaria

**Date**: 28/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [008-RegistrarMedicacion.md](../specs/008-RegistrarMedicacion.md)
- [015-ConsultarMedicacionPorGalponPorDia.md](../specs/015-ConsultarMedicacionPorGalponPorDia.md)

## Summary

Implementar el registro, edición y consulta de catálogo de medicaciones por parte del veterinario, así como la consulta diaria de tratamientos por galpón y el resumen consolidado permitido por rol para los trabajadores y administradores. El registro del consumo real y su descuento de existencias pertenecen al Plan 012.

La medicación prescrita y el consumo real son conceptos diferentes. Registrar o editar una medicación no descuenta inventario. El consumo real se especifica en [Plan 012](012-RegistrarConsumoDeMedicamentos.md).

`Galpon` y `Lote` continúan siendo entidades propietarias del Módulo 1. El diagnóstico pertenece al Plan 015 y el catálogo de enfermedades al Plan 005. El medicamento, su recepción, existencias, unidad base y precio de compra pertenecen a los Planes 002 y 010. Este plan consulta esas capacidades mediante puertos internos y no crea copias autoritativas.

## Technical Context

**Performance Goals**: El 95 % de los registros, ediciones y consultas del catálogo de medicaciones responde en máximo 1 segundo después de su confirmación. El 95 % de las consultas diarias individuales y consolidadas responde en máximo 2 segundos sin realizar escrituras.

**Constraints**: Solo `ROLE_VETERINARIO` registra, edita o consulta el catálogo de medicaciones. La medicación debe referenciar una enfermedad registrada como tratable (`requiereSacrificioSanitario == false`) y un medicamento existente en el catálogo/inventario. La consulta diaria operativa (`ROLE_TRABAJADOR` y `ROLE_ADMINISTRADOR`) es estrictamente de solo lectura. No se permite el borrado físico (`DELETE`) de medicaciones.

**Scale/Scope**: Cinco historias/capacidades (registrar medicación, editar medicación, listar catálogo de medicaciones para UI/diagnóstico, consultar medicación diaria por galpón y consultar resumen consolidado diario), cinco endpoints REST, una entidad raíz versionada y adaptadores internos de consulta.

**Dependencias funcionales**: Catálogo de enfermedades del [Plan 005](005-CatalogoDeEnfermedades.md); diagnósticos activos y snapshots del [Plan 015](015-DiagnosticoYReintegroSanitario.md); `Galpón` y `Lote` del Módulo 1 / [Plan 001](001-ConsultaDeGalpones.md); catálogo/inventario de medicamentos del [Plan 002](002-GestionDeRecepciones.md); autenticación, actor, reloj, idempotencia y errores de [General.md](General.md).

### Decisiones específicas

1. **Medicación**: `Medicacion` es una entidad propia con `id` (`UUID`), `enfermedadId`, `medicamentoId`, `dosis`, `diasTotales`, `descripcion` y `version` (`@Version`). Todos sus datos son obligatorios y los textos se normalizan con `trim()`. Al confirmar, se valida nuevamente que el medicamento exista y que la enfermedad exista y tenga `requiereSacrificioSanitario == false`.
2. **Edición histórica e inmutabilidad**: Editar una medicación eleva su `version` y solo afecta a diagnósticos futuros. Un diagnóstico ya confirmado en el Plan 015 conserva en su instantánea (_snapshot_) el medicamento, la dosis, la duración en días y la descripción/vía utilizados al momento de su registro.
3. **Consulta de catálogo para UI y Diagnóstico (`SPEC-008 FR-006`)**: Se expone `GET /api/medicaciones` (filtrable opcionalmente por `enfermedadId`) para poblar los selectores del módulo de diagnóstico clínico (Plan 015) y la tabla de gestión del veterinario.
4. **Cero impacto en inventario**: Registrar o editar una medicación no cambia existencias físicas. El [Plan 012](012-RegistrarConsumoDeMedicamentos.md) coordina el consumo real con el [Plan 010](010-GestionDelInventario.md), que ejecuta los descuentos FEFO.
5. **Consultas diarias de solo lectura**: La consulta diaria toma la fecha actual del `Clock` compartido (`America/Bogota`) y devuelve los tratamientos vigentes en esa fecha a partir de los diagnósticos activos del lote alojado. No modifica diagnósticos, galpones, lotes ni inventario, ni publica eventos de integración.
6. **Galpones y trabajadores**: Un trabajador autorizado (`ROLE_TRABAJADOR`) o administrador (`ROLE_ADMINISTRADOR`) puede consultar cualquier galpón existente por `galponId` sin restricción de asignación trabajador-galpón; el resumen consolidado incluye únicamente los galpones seleccionados para esa solicitud.
7. **Dosis e instrucciones**: La dosis y la vía de administración se manejan como expresiones textuales estructuradas conservadas desde la prescripción médica.
8. **Persistencia e idempotencia**: Las escrituras soportan `X-Idempotency-Key` para evitar duplicados ante reintentos de red y publican eventos internos después de confirmar la transacción local. Las migraciones Flyway utilizan la siguiente versión disponible del repositorio.

### Alcance documental

- El [Plan 015](015-DiagnosticoYReintegroSanitario.md) consulta `MedicacionQueryPort` al registrar diagnósticos para verificar compatibilidad con la enfermedad y copiar los datos al snapshot del diagnóstico. El Plan 006 implementa ese contrato.
- El [Plan 005](005-CatalogoDeEnfermedades.md) es dueño del catálogo de enfermedades; este plan lo consulta mediante `EnfermedadQueryPort`.
- El [Plan 002](002-GestionDeRecepciones.md) y el [Plan 010](010-GestionDelInventario.md) son dueños del catálogo y existencias de medicamentos; este plan solo verifica la disponibilidad del medicamento mediante `MedicamentoCatalogoQueryPort`.
- El [Plan 012](012-RegistrarConsumoDeMedicamentos.md) es propietario del consumo real de medicamentos y de los detalles históricos para el costeo del Módulo 3.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   ├── 008-RegistrarMedicacion.md
│   └── 015-ConsultarMedicacionPorGalponPorDia.md
└── plan/
    └── 006-GestionDeMedicaciones.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/medicamento/
│   │   └── Medicacion.java
│   ├── exception/medicamento/
│   │   ├── MedicacionNoEncontradaException.java
│   │   ├── MedicamentoNoDisponibleException.java
│   │   ├── EnfermedadNoEncontradaException.java
│   │   ├── EnfermedadNoAdmiteMedicacionException.java
│   │   └── ConcurrenciaMedicacionException.java
│   └── port/out/medicamento/
│       ├── MedicacionRepositoryPort.java
│       ├── EnfermedadQueryPort.java
│       ├── MedicamentoCatalogoQueryPort.java
│       ├── DiagnosticoQueryPort.java
│       ├── GalponQueryPort.java
│       ├── LoteQueryPort.java
│       └── IntegrationEventPublisherPort.java
├── application/medicamento/
│   ├── RegistrarMedicacionUseCase.java
│   ├── EditarMedicacionUseCase.java
│   ├── ConsultarMedicacionesUseCase.java
│   ├── ConsultarMedicacionDiariaUseCase.java
│   ├── ConsultarResumenMedicacionDiariaUseCase.java
│   └── result/
│       ├── MedicacionResult.java
│       ├── TratamientoDiarioItemResult.java
│       ├── MedicacionDiariaResult.java
│       └── ResumenMedicacionDiariaResult.java
└── infrastructure/
    ├── adapter/in/rest/medicamento/
    │   ├── MedicacionController.java
    │   ├── MedicacionDiariaController.java
    │   ├── dto/
    │   │   ├── RegistrarMedicacionRequest.java
    │   │   ├── EditarMedicacionRequest.java
    │   │   ├── MedicacionResponse.java
    │   │   ├── MedicacionDiariaResponse.java
    │   │   └── ResumenMedicacionDiariaResponse.java
    │   └── mapper/MedicacionRestMapper.java
    ├── adapter/out/internal/medicamento/
    │   ├── EnfermedadQueryAdapter.java
    │   ├── MedicamentoCatalogoQueryAdapter.java
    │   ├── DiagnosticoQueryAdapter.java
    │   ├── GalponQueryAdapter.java
    │   └── LoteQueryAdapter.java
    ├── adapter/out/persistence/medicamento/
    │   ├── entity/MedicacionEntity.java
    │   ├── repository/SpringDataMedicacionRepository.java
    │   ├── mapper/MedicacionPersistenceMapper.java
    │   └── MedicacionPersistenceAdapter.java
    └── config/
        └── MedicamentoBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/medicamento/
│   └── MedicacionTest.java
├── application/medicamento/
│   ├── RegistrarMedicacionUseCaseTest.java
│   ├── EditarMedicacionUseCaseTest.java
│   ├── ConsultarMedicacionesUseCaseTest.java
│   ├── ConsultarMedicacionDiariaUseCaseTest.java
│   └── ConsultarResumenMedicacionDiariaUseCaseTest.java
├── infrastructure/adapter/in/rest/medicamento/
│   ├── MedicacionControllerTest.java
│   └── MedicacionDiariaControllerTest.java
└── integration/medicamento/
    └── MedicamentoIntegrationTest.java
```

**Structure Decision**: Las reglas de prescripción de medicación y armado de consultas diarias se mantienen en `domain/` y `application/`. Los controladores solo traducen HTTP y validan sintaxis; las consultas a otras capacidades utilizan puertos de salida implementados por adaptadores internos sin acoplarse a entidades JPA ajenas.

### Entidades y relación

```text
Medicacion (Aggregate Root)
├── id: UUID
├── enfermedadId: UUID (Ref. Plan 005 - debe tener requiereSacrificioSanitario == false)
├── medicamentoId: UUID (Ref. Plan 002 - catálogo/inventario de medicamentos)
├── dosis: String
├── diasTotales: Integer (> 0)
├── descripcion: String
└── version: Integer (@Version para Optimistic Locking)
```

`Medicacion` puede editarse para tratamientos futuros. Cuando el Plan 015 registra un diagnóstico, copia los valores vigentes de la medicación a su snapshot para que cambios posteriores no alteren los tratamientos en curso ni los históricos.

### Contratos de los puertos

| Puerto                          | Responsabilidad                                                                                                                   |
| ------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| `MedicacionRepositoryPort`      | Crear, buscar por ID, editar (con bloqueo optimista) y listar medicaciones disponibles (filtrables por `enfermedadId`).           |
| `EnfermedadQueryPort`           | Confirmar en el Plan 005 que la enfermedad existe y que su indicador `requiereSacrificioSanitario` es `false`.                    |
| `MedicamentoCatalogoQueryPort`  | Confirmar en el Plan 002 que el medicamento existe y está registrado en el inventario/catálogo, obteniendo su nombre.             |
| `DiagnosticoQueryPort`          | Consultar en el Plan 015 los diagnósticos con tratamiento vigentes en la fecha actual para un lote/galpón o conjunto de galpones. |
| `GalponQueryPort`               | Consultar en el Módulo 1 / Plan 001 los datos vigentes del galpón (UUID, nombre, aforo máximo y estado).                          |
| `LoteQueryPort`                 | Consultar en el Módulo 1 / Plan 001 el lote actualmente alojado, su población actual y su edad en días.                           |
| `IntegrationEventPublisherPort` | Publicar eventos internos (`MedicacionRegistrada`, `MedicacionActualizada`) tras confirmar la transacción local.                  |

### Contratos HTTP propuestos

| Método y ruta                                    | Acceso                                  | Responsabilidad                                                                               |
| ------------------------------------------------ | --------------------------------------- | --------------------------------------------------------------------------------------------- |
| `POST /api/medicaciones`                         | `ROLE_VETERINARIO`                      | Registrar una medicación para una enfermedad tratable (soporta `X-Idempotency-Key`).          |
| `PUT /api/medicaciones/{medicacionId}`           | `ROLE_VETERINARIO`                      | Editar una medicación existente para diagnósticos futuros (valida `version`).                 |
| `GET /api/medicaciones`                          | `ROLE_VETERINARIO`                      | Listar medicaciones registradas (filtro opcional `?enfermedadId=uuid`) para UI y diagnóstico. |
| `GET /api/galpones/{galponId}/medicacion-diaria` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` | Consultar tratamientos vigentes para el galpón en la fecha actual.                            |
| `GET /api/medicacion-diaria`                     | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` | Consultar el resumen consolidado diario para los `galponIds` seleccionados.                   |

Las consultas diarias responden `200 OK` incluso cuando no hay tratamientos activos, indicando el estado `SIN_MEDICACION_ACTIVA_HOY` y el mensaje `"Sin medicación activa para hoy"`. Las escrituras responden `201 Created` al crear y `200 OK` al editar. Las respuestas de error utilizan `application/problem+json` conforme a [General.md](General.md).

### JSON común de errores

```json
{
  "type": "https://avicontrol/errors/enfermedad-no-admite-medicacion",
  "title": "Enfermedad no admite medicación",
  "status": 409,
  "detail": "La enfermedad seleccionada requiere sacrificio sanitario y no admite registro de medicación",
  "instance": "/api/medicaciones",
  "code": "ENFERMEDAD_NO_ADMITE_MEDICACION",
  "correlationId": "uuid",
  "fieldErrors": []
}
```

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Acordar roles, contratos de consulta con Planes 002, 005 y 015, y preparar fixtures del subdominio.

- [ ] T001 Acordar con el Plan 015 el contrato de `MedicacionQueryPort` (para validar compatibilidad al diagnosticar) y de `DiagnosticoQueryPort` (para leer tratamientos vigentes en la fecha actual).
- [ ] T002 Confirmar con el Plan 005 la consulta de enfermedad y su indicador `requiereSacrificioSanitario`, y con el Plan 002 el contrato de `MedicamentoCatalogoQueryPort`.
- [ ] T003 Confirmar que la SPEC-015 permite a cualquier usuario con `ROLE_TRABAJADOR` o `ROLE_ADMINISTRADOR` consultar cualquier galpón existente sin filtro de asignación trabajador-galpón.
- [ ] T004 Acordar la representación textual de `dosis` y `descripcion` (vía de administración) conforme a SPEC-008 y SPEC-015.
- [ ] T005 Preparar fixtures de enfermedades tratables y mortales, medicamentos en inventario, medicaciones, diagnósticos activos/finalizados y galpones con y sin lote activo.

**Checkpoint**: Contratos entre capacidades y reglas de autorización definidos sin ambigüedades.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Preparar dominio, persistencia versionada y adaptadores internos para prescripción y consulta.

- [ ] T006 Implementar la entidad `Medicacion` en Java puro con validación de campos obligatorios, normalización `trim()`, `diasTotales > 0` y atributo `version`.
- [ ] T007 Definir las excepciones de dominio (`MedicacionNoEncontradaException`, `MedicamentoNoDisponibleException`, `EnfermedadNoEncontradaException`, `EnfermedadNoAdmiteMedicacionException`, `ConcurrenciaMedicacionException`).
- [ ] T008 Definir los puertos de salida `MedicacionRepositoryPort`, `EnfermedadQueryPort`, `MedicamentoCatalogoQueryPort`, `DiagnosticoQueryPort`, `GalponQueryPort` y `LoteQueryPort`.
- [ ] T009 Crear la migración Flyway para la tabla `medicacion` usando la siguiente versión disponible del repositorio, incluyendo columna `version` e índices por `enfermedad_id` y `medicamento_id`.
- [ ] T010 Implementar `MedicacionEntity`, `SpringDataMedicacionRepository`, `MedicacionPersistenceMapper` y `MedicacionPersistenceAdapter` sin permitir borrado físico.
- [ ] T011 Implementar los adaptadores internos de consulta hacia los Planes 001, 002, 005 y 015 sin acceder a repositorios JPA privados de otros módulos.
- [ ] T012 Configurar las transacciones locales (`@Transactional`), soporte de idempotencia y publicación confiable de eventos en `MedicamentoBeanConfiguration`.

**Checkpoint**: Modelo de dominio, persistencia y puertos listos para soportar las historias de usuario.

## Phase 3: User Story 1 — Registrar una Medicación (Priority: P1)

**Spec**: SPEC-008, historia 1.

**Goal**: El veterinario crea una medicación válida asociada a una enfermedad tratable y a un medicamento del inventario, sin descontar existencias físicas.

**Independent Test**: Se registra una medicación con datos completos sobre una enfermedad con `requiereSacrificioSanitario == false` (`201 Created`); una enfermedad inexistente o mortal, un medicamento no disponible, días `<= 0` o un usuario sin `ROLE_VETERINARIO` son rechazados sin crear el registro ni alterar el inventario.

### Definición del evento para User Story 1

**Evento producido**: `MedicacionRegistrada`.

**Evento consumido**: Ninguno.

Se publica después de confirmar la transacción local. Incluye `eventId`, `eventVersion`, `occurredAt`, `producer`, `correlationId`, `aggregateId`, `medicacionId`, `enfermedadId`, `medicamentoId`, `dosis`, `diasTotales` y `descripcion`.

### Definición del endpoint REST para User Story 1

| Elemento      | Definición                                                                                                                                                                                                                                                      |
| ------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Método y ruta | `POST /api/medicaciones`                                                                                                                                                                                                                                        |
| Autorización  | `ROLE_VETERINARIO`                                                                                                                                                                                                                                              |
| Entrada       | `enfermedadId`, `medicamentoId`, `dosis`, `diasTotales` (entero `> 0`) y `descripcion`. Soporta `X-Idempotency-Key`.                                                                                                                                            |
| Respuesta 201 | Medicación creada con su `version` inicial y disponible para diagnósticos futuros.                                                                                                                                                                              |
| Errores       | `400` por datos faltantes, textos en blanco o duración inválida; `403` por rol no autorizado; `404` por enfermedad o medicamento inexistente; `409` si la enfermedad requiere sacrificio sanitario; `503` si una dependencia imprescindible no está disponible. |

#### JSON de solicitud

```json
{
  "enfermedadId": "uuid",
  "medicamentoId": "uuid",
  "dosis": "200 g diluidos en 100 L de agua potable",
  "diasTotales": 5,
  "descripcion": "Agua de bebida en bebederos automáticos"
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
  "descripcion": "Agua de bebida en bebederos automáticos",
  "version": 1
}
```

### Tests e implementación

- [ ] T013 [US1] Probar campos obligatorios, rechazo de espacios en blanco (`trim`), `diasTotales > 0` y autorización exclusiva `ROLE_VETERINARIO`.
- [ ] T014 [US1] Probar validación de enfermedad existente y tratable (`requiereSacrificioSanitario == false`) y medicamento disponible en inventario.
- [ ] T015 [US1] Probar idempotencia ante reintentos de red y verificar que registrar una medicación no modifica las existencias del inventario.
- [ ] T016 [US1] Implementar `RegistrarMedicacionUseCase`, DTOs, mapper, publicación de evento y endpoint `POST /api/medicaciones`.

**Checkpoint**: La medicación queda registrada en menos de 1 segundo y disponible para diagnósticos futuros sin tocar el inventario.

## Phase 4: User Story 2 — Editar una Medicación y Consultar Catálogo (Priority: P2)

**Spec**: SPEC-008, historia 2 y FR-006.

**Goal**: El veterinario actualiza una medicación existente aplicando los cambios únicamente a diagnósticos futuros, y consulta el catálogo de medicaciones registradas (filtrable por `enfermedadId`).

**Independent Test**: Una medicación de 5 días utilizada en un diagnóstico previo se edita a 7 días enviando su `version` vigente; el diagnóstico anterior conserva su duración de 5 días y su fecha de reintegro original, mientras que `GET /api/medicaciones` devuelve la duración actualizada de 7 días para diagnósticos futuros.

### Definición del evento para User Story 2

**Evento producido**: `MedicacionActualizada`.

**Evento consumido**: Ninguno.

El evento comunica la nueva versión de la medicación. No recalcula diagnósticos ni fechas de reintegro ya registradas en el Plan 015.

### Definición de endpoints REST para User Story 2 y Catálogo

| Elemento      | Definición                                                                                                         |
| ------------- | ------------------------------------------------------------------------------------------------------------------ |
| Método y ruta | `PUT /api/medicaciones/{medicacionId}`                                                                             |
| Autorización  | `ROLE_VETERINARIO`                                                                                                 |
| Entrada       | `enfermedadId`, `medicamentoId`, `dosis`, `diasTotales`, `descripcion` y `version` esperada.                       |
| Respuesta 200 | Medicación actualizada con `version` incrementada.                                                                 |
| Errores       | `400`, `403`, `404` y `409` por colisión de concurrencia optimista o enfermedad que requiere sacrificio sanitario. |

| Elemento      | Definición                                                                          |
| ------------- | ----------------------------------------------------------------------------------- |
| Método y ruta | `GET /api/medicaciones`                                                             |
| Autorización  | `ROLE_VETERINARIO`                                                                  |
| Entrada       | Query param opcional `enfermedadId` (`UUID`) para filtrar medicaciones compatibles. |
| Respuesta 200 | Lista de medicaciones registradas con sus atributos vigentes.                       |

#### JSON de solicitud (`PUT /api/medicaciones/{medicacionId}`)

```json
{
  "enfermedadId": "uuid",
  "medicamentoId": "uuid",
  "dosis": "250 g diluidos en 100 L de agua potable",
  "diasTotales": 7,
  "descripcion": "Agua de bebida en bebederos automáticos",
  "version": 1
}
```

### Tests e implementación

- [ ] T017 [US2] Probar edición válida, rechazo por rol distinto a veterinario y rechazo por versión desactualizada (`409 Conflict`).
- [ ] T018 [US2] Probar que diagnósticos existentes en el Plan 015 conservan sus valores históricos tras editar la medicación.
- [ ] T019 [US2] Probar `GET /api/medicaciones` con y sin filtro `enfermedadId` para poblar selectores de UI.
- [ ] T020 [US2] Implementar `EditarMedicacionUseCase`, `ConsultarMedicacionesUseCase` y los endpoints `PUT /api/medicaciones/{medicacionId}` y `GET /api/medicaciones`.

**Checkpoint**: La edición protege diagnósticos históricos y el catálogo puede consultarse desde el frontend en menos de 1 segundo.

## Phase 5: User Story 3 — Consultar Medicación Diaria por Galpón (Priority: P1)

**Spec**: SPEC-015, historias 1 y 2.

**Goal**: El trabajador o administrador consulta el tratamiento vigente para cualquier galpón existente en la fecha actual y recibe un resultado explícito cuando no existe medicación activa.

**Independent Test**: Un galpón con lote activo y diagnóstico vigente en el día 2 de 5 devuelve los datos del galpón/lote, enfermedad, medicamento, dosis hoy, vía de administración y avance (`Día 2 de 5`); un galpón sin tratamiento activo o con tratamiento finalizado ayer devuelve `"Sin medicación activa para hoy"`.

### Definición del evento para User Story 3

**Evento producido**: Ninguno.

**Evento consumido**: Ninguno. La consulta obtiene el galpón, lote y diagnósticos vigentes mediante puertos internos en modo de solo lectura.

### Definición del endpoint REST para User Story 3

| Elemento                      | Definición                                                                                                                                                                                                                                                 |
| ----------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Método y ruta                 | `GET /api/galpones/{galponId}/medicacion-diaria`                                                                                                                                                                                                           |
| Autorización                  | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR`                                                                                                                                                                                                                    |
| Entrada                       | `galponId` (`UUID` en ruta); utiliza siempre la fecha actual del `Clock` compartido (`America/Bogota`).                                                                                                                                                    |
| Respuesta 200 con tratamiento | Datos del galpón (UUID, nombre, aforo máximo, estado), datos del lote (UUID, nombre, población actual, edad en días) y lista de tratamientos activos con enfermedad, medicamento, dosis hoy, vía de administración, `diaActual`, `diasTotales` y `avance`. |
| Respuesta 200 sin tratamiento | Datos disponibles del galpón/lote, lista vacía de tratamientos, estado `SIN_MEDICACION_ACTIVA_HOY` y mensaje `"Sin medicación activa para hoy"`.                                                                                                           |
| Errores                       | `400` por UUID inválido; `401`/`403` por seguridad; `404` por galpón inexistente; `503` por dependencia imprescindible no disponible.                                                                                                                      |

#### JSON de respuesta con tratamiento activo

```json
{
  "galponId": "uuid",
  "galponNombre": "Galpón 5",
  "aforoMaximo": 8000,
  "estadoGalpon": "AISLAMIENTO",
  "loteId": "uuid",
  "loteNombre": "LDP-005",
  "poblacionActual": 7960,
  "edadDias": 17,
  "tratamientos": [
    {
      "diagnosticoId": "uuid",
      "enfermedad": "Coccidiosis Aviar",
      "medicamento": "Amprolio 20% Solución",
      "dosisHoy": "200 g diluidos en 100 L de agua potable",
      "viaAdministracion": "Agua de bebida en bebederos automáticos",
      "diaActual": 2,
      "diasTotales": 5,
      "avance": "Día 2 de 5"
    }
  ],
  "estado": "MEDICACION_ACTIVA",
  "mensaje": null
}
```

#### JSON de respuesta sin tratamiento activo

```json
{
  "galponId": "uuid",
  "galponNombre": "Galpón 2",
  "aforoMaximo": 6000,
  "estadoGalpon": "PRODUCTIVO",
  "loteId": "uuid",
  "loteNombre": "LDP-002",
  "poblacionActual": 5950,
  "edadDias": 12,
  "tratamientos": [],
  "estado": "SIN_MEDICACION_ACTIVA_HOY",
  "mensaje": "Sin medicación activa para hoy"
}
```

### Tests e implementación

- [ ] T021 [US3] Probar galpón con tratamiento activo, tratamiento finalizado en días anteriores, galpón sin lote activo y lote con población cero.
- [ ] T022 [US3] Probar cálculo exacto del día de avance (`diaActual` de `diasTotales`) y desglose independiente cuando existen múltiples tratamientos concurrentes.
- [ ] T023 [US3] Probar que la consulta no requiere asignación trabajador-galpón, rechaza roles no autorizados y no realiza escrituras ni descuentos de inventario.
- [ ] T024 [US3] Implementar `ConsultarMedicacionDiariaUseCase`, resultados, DTOs y endpoint `GET /api/galpones/{galponId}/medicacion-diaria`.

**Checkpoint**: La consulta diaria por galpón entrega las indicaciones exactas a partir del snapshot del diagnóstico sin generar consumos.

## Phase 6: User Story 4 — Consultar Resumen Consolidado Diario (Priority: P2)

**Spec**: SPEC-015, historia 3.

**Goal**: El trabajador visualiza en su pantalla de inicio la tarjeta métrica de tratamientos activos hoy y el panel consolidado de medicación diaria para los galpones seleccionados en la consulta.

**Independent Test**: Seleccionando los galpones 1, 3 y 5 donde solo el Galpón 5 (`Lote #LDP-005`) tiene tratamiento vigente hoy, la respuesta entrega `totalGalponesConTratamiento: 1`, `totalTratamientos: 1`, `resumen: "Amprolio 20% en Galpón 5"` y la tarjeta con los 6 campos obligatorios; si ningún galpón seleccionado tiene tratamiento activo, devuelve conteos en `0` y `"Sin tratamientos hoy"`.

### Definición del evento para User Story 4

**Evento producido**: Ninguno.

**Evento consumido**: Ninguno.

El resumen es una agregación de lectura en tiempo real sobre los galpones solicitados.

### Definición del endpoint REST para User Story 4

| Elemento      | Definición                                                                                                                                                                                                               |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Método y ruta | `GET /api/medicacion-diaria`                                                                                                                                                                                             |
| Autorización  | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR`                                                                                                                                                                                  |
| Entrada       | Query param `galponIds` (lista de UUIDs seleccionados para la consulta).                                                                                                                                                 |
| Respuesta 200 | `totalGalponesConTratamiento`, `totalTratamientos`, `resumen` y lista `tratamientos` con los 6 campos requeridos (`galponNombre`, `loteNombre`, `avance`, `enfermedad`, `medicamento`, `dosisHoy`, `viaAdministracion`). |
| Listado vacío | `0` galpones, `0` tratamientos, `resumen: "Sin tratamientos hoy"` y lista vacía.                                                                                                                                         |
| Errores       | `400` por IDs inválidos; `401`/`403` por seguridad; `503` por dependencia no disponible.                                                                                                                                 |

#### JSON de respuesta

```json
{
  "totalGalponesConTratamiento": 1,
  "totalTratamientos": 1,
  "resumen": "Amprolio 20% en Galpón 5",
  "tratamientos": [
    {
      "diagnosticoId": "uuid",
      "galponId": "uuid",
      "galponNombre": "Galpón 5",
      "loteId": "uuid",
      "loteNombre": "LDP-005",
      "avance": "Día 2 de 5",
      "enfermedad": "Coccidiosis Aviar",
      "medicamento": "Amprolio 20% Solución",
      "dosisHoy": "200 g diluidos en 100 L de agua potable",
      "viaAdministracion": "Agua de bebida en bebederos automáticos"
    }
  ]
}
```

### Tests e implementación

- [ ] T025 [US4] Probar resumen consolidado con uno, varios y cero tratamientos vigentes sobre los galpones seleccionados.
- [ ] T026 [US4] Probar que el resultado se limite estrictamente a los `galponIds` enviados en la consulta y no exija asignación trabajador-galpón.
- [ ] T027 [US4] Probar que múltiples tratamientos concurrentes en un mismo galpón se listen en tarjetas separadas.
- [ ] T028 [US4] Implementar `ConsultarResumenMedicacionDiariaUseCase` con lectura por conjunto y exponer el endpoint `GET /api/medicacion-diaria`.

**Checkpoint**: El panel consolidado del trabajador responde en menos de 2 segundos sin crear proyecciones ni modificar datos.

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Verificar contratos, seguridad, arquitectura y rendimiento de prescripción y consulta diaria.

- [ ] T029 Documentar los cinco endpoints, parámetros, `X-Idempotency-Key`, roles y errores RFC 9457 en OpenAPI 3.
- [ ] T030 Completar `MedicamentoIntegrationTest` abarcando registro, edición, bloqueo de enfermedad mortal y consultas diarias.
- [ ] T031 Verificar que registrar o editar una medicación no descuente existencias del Plan 010 ni modifique diagnósticos existentes del Plan 015.
- [ ] T032 Verificar que las consultas diarias operen estrictamente en solo lectura.
- [ ] T033 Verificar que el Plan 015 consuma el contrato de medicaciones sin duplicar tablas.
- [ ] T034 Comprobar reglas de arquitectura hexagonal con ArchUnit conforme a [General.md](General.md).
- [ ] T035 Medir tiempos de respuesta verificando `< 1 segundo` (p95) en catálogo de medicaciones y `< 2 segundos` (p95) en consultas diarias.

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)** precede a **Foundational (Phase 2)**.
- **Foundational (Phase 2)** habilita el catálogo de medicaciones y las consultas diarias.
- **US1 (Phase 3)** y **US2 (Phase 4)** definen y mantienen el catálogo de medicaciones que consume el diagnóstico del Plan 015.
- **US3 (Phase 5)** y **US4 (Phase 6)** dependen de los diagnósticos registrados en el Plan 015 y de los datos de Galpón/Lote del Plan 001.
- **Polish (Phase 7)** depende de la finalización de todas las historias.

### Dependencias con otros planes

- **Plan 005 (Catálogo de Enfermedades)**: Provee la validación de existencia de la enfermedad y de su indicador `requiereSacrificioSanitario == false`.
- **Plan 002 (Gestión de Recepciones)**: Provee la validación de existencia del medicamento en el catálogo/inventario.
- **Plan 015 (Diagnóstico y Reintegro Sanitario)**: Consume el catálogo de medicaciones al diagnosticar y provee los diagnósticos activos con sus snapshots para las consultas diarias de SPEC-015.
- **Plan 012 (Registro de Consumo de Medicamentos)**: Registra el consumo físico real en campo a partir del diagnóstico y coordina el descuento FEFO con el Plan 010.
- **Módulo 1 / Plan 001**: Provee los datos vigentes de `Galpón` y `Lote`.
- **General.md**: Provee reglas transversales de seguridad, actor, reloj, idempotencia, errores y eventos.

### Dentro de cada User Story

- Entidades e invariantes de dominio antes que casos de uso.
- Puertos de salida antes que adaptadores internos y de persistencia.
- Casos de uso antes que controladores REST.
- Pruebas unitarias y de contrato junto con cada implementación.

## Notes

- `T001` a `T035` identifican las tareas del plan; `US1` a `US4` identifican las historias de usuario de SPEC-008 y SPEC-015.
- La medicación prescrita no representa administración física ni descuento de inventario.
- El diagnóstico en el Plan 015 captura una instantánea (_snapshot_) de la medicación para garantizar que las ediciones en este catálogo nunca alteren tratamientos pasados o en curso.
