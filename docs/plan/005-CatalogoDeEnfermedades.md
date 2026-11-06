# Implementation Plan: Catálogo de Enfermedades

**Date**: 28/10/2026

**Last Updated**: 28/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [009-RegistrarEnfermedad.md](../specs/009-RegistrarEnfermedad.md)

## Summary

Permitir al veterinario registrar, editar y consultar (listar y filtrar) enfermedades con su clasificación clínica y el indicador obligatorio que señala si la enfermedad requiere sacrificio sanitario. Los diagnósticos capturan una instantánea de los datos de esta enfermedad para conservar su significado histórico de forma inmutable.

## Technical Context

**Performance Goals**: El 95 % de registros, ediciones y consultas responde en menos de 1 segundo.

**Constraints**: Solo `ROLE_VETERINARIO` registra, edita o consulta. Todos los atributos son obligatorios; `requiereSacrificioSanitario` debe enviarse explícitamente. Las ediciones aplican a diagnósticos futuros, utilizando control de concurrencia optimista (`@Version`), y no alteran diagnósticos existentes. Prohibido el borrado físico (`DELETE`).

**Scale/Scope**: SPEC-009, dos historias de escritura (registrar y editar) y una operación de lectura filtrable para la UI y módulos internos; una entidad propia y tres endpoints REST (`POST`, `PUT`, `GET`).

**Dependencias funcionales**: [General.md](General.md) para seguridad, actor, auditoría, errores y transacciones; el Plan 015 consulta este catálogo y conserva snapshots de enfermedad al diagnosticar; el Plan 006 lo consulta al registrar medicaciones; el Plan 008 consume la condición de sacrificio a través del diagnóstico del Plan 015.

### Decisiones específicas

1. `Enfermedad` almacena identificador (`UUID`), nombre (único normalizado), nivel de riesgo (`NivelRiesgo`), tipo (`TipoEnfermedad`), descripción clínica, `requiereSacrificioSanitario` obligatorio y `version` para control de concurrencia optimista.
2. La edición de una enfermedad eleva su `version` y no modifica snapshots de diagnósticos ya confirmados (Plan 015).
3. **Ausencia de Sobre-Ingeniería (YAGNI)**: Este plan no utiliza `IntegrationEventPublisherPort` ni publica eventos en Kafka al crear o editar enfermedades, ya que ningún módulo externo los consume y los Planes 006 y 015 consultan este catálogo de forma síncrona en memoria mediante `ConsultarEnfermedadesUseCase`.
4. El indicador verdadero (`requiereSacrificioSanitario = true`) significa que la enfermedad habilita una orden sanitaria total, pero este plan no emite la orden (Plan 008).
5. **Autonomía UX/UI (`SPEC-009 FR-007`)**: Se expone `GET /api/enfermedades?search=texto&requiereSacrificioSanitario=true|false` devolviendo una lista ordenada alfabéticamente por `nombre` para alimentar directamente los selectores (dropdowns) y tablas del frontend sin llamadas adicionales.
6. **Operatividad y Datos Semilla**: Se incluyen datos semilla base (enfermedades tratables y mortales) para que los Planes 006 y 015 puedan probarse de forma inmediata y autónoma. Se soporta la cabecera `X-Idempotency-Key` en las escrituras mediante el filtro transversal del proyecto.

### Alcance documental

- Este plan implementa [SPEC-009](../specs/009-RegistrarEnfermedad.md) y es dueño absoluto del catálogo de enfermedades.
- [Plan 015](015-DiagnosticoYReintegroSanitario.md) consulta enfermedades y conserva los atributos usados en cada diagnóstico.
- [Plan 006](006-GestionDeMedicaciones.md) consulta este catálogo para asociar medicaciones exclusivamente a enfermedades tratables (`requiereSacrificioSanitario == false`).
- [Plan 008](008-GestionDelSacrificioSanitario.md) solo recibe el diagnóstico resultante para ejecutar el flujo de sacrificio sanitario.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 009-RegistrarEnfermedad.md
└── plan/
    └── 005-CatalogoDeEnfermedades.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/sanitario/enfermedad/
│   │   ├── Enfermedad.java
│   │   ├── TipoEnfermedad.java
│   │   └── NivelRiesgo.java
│   ├── exception/sanitario/enfermedad/
│   │   ├── EnfermedadNoEncontradaException.java
│   │   ├── NombreEnfermedadDuplicadoException.java
│   │   └── ConcurrenciaEnfermedadException.java
│   └── port/out/sanitario/enfermedad/
│       └── EnfermedadRepositoryPort.java
├── application/sanitario/enfermedad/
│   ├── RegistrarEnfermedadUseCase.java
│   ├── EditarEnfermedadUseCase.java
│   ├── ConsultarEnfermedadesUseCase.java
│   └── result/
│       └── EnfermedadResult.java
└── infrastructure/
    ├── adapter/in/rest/sanitario/enfermedad/
    │   ├── EnfermedadController.java
    │   ├── dto/
    │   │   ├── RegistrarEnfermedadRequest.java
    │   │   ├── EditarEnfermedadRequest.java
    │   │   └── EnfermedadResponse.java
    │   └── mapper/EnfermedadRestMapper.java
    ├── adapter/out/persistence/sanitario/enfermedad/
    │   ├── entity/EnfermedadEntity.java
    │   ├── repository/SpringDataEnfermedadRepository.java
    │   ├── mapper/EnfermedadPersistenceMapper.java
    │   └── EnfermedadPersistenceAdapter.java
    └── config/
        └── EnfermedadBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/sanitario/enfermedad/
│   └── EnfermedadTest.java
├── application/sanitario/enfermedad/
│   ├── RegistrarEnfermedadUseCaseTest.java
│   ├── EditarEnfermedadUseCaseTest.java
│   └── ConsultarEnfermedadesUseCaseTest.java
├── infrastructure/adapter/in/rest/sanitario/enfermedad/
│   └── EnfermedadControllerTest.java
└── integration/sanitario/enfermedad/
    └── EnfermedadIntegrationTest.java
```

**Structure Decision**: El catálogo y sus reglas son propios del dominio sanitario. Los demás planes (Plan 006 y Plan 015) consultan este catálogo mediante la interfaz pública de aplicación (`ConsultarEnfermedadesUseCase`) y conservan sus snapshots sin modificar esta entidad ni acceder a su repositorio JPA privado.

### Entidades y relación

```text
Enfermedad (Aggregate Root)
├── id: UUID
├── nombre: String (único normalizado)
├── nivelRiesgo: NivelRiesgo (BAJO, MEDIO, ALTO, CRITICO)
├── tipo: TipoEnfermedad (VIRAL, BACTERIANA, PARASITARIA, FUNGICA)
├── descripcionClinica: String
├── requiereSacrificioSanitario: boolean (obligatorio)
└── version: Integer (@Version para Optimistic Locking)
```

Un diagnóstico (Plan 015) referencia la enfermedad mediante `enfermedadId` y conserva una instantánea (`nombreEnfermedadHistorico`, `requiereSacrificioHistorico`) de los atributos de decisión utilizados al confirmarse; no mantiene una relación mutable que reescriba el historial.

### Contratos de los puertos

| Puerto                     | Responsabilidad                                                                                                                                                                                            |
| -------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `EnfermedadRepositoryPort` | Crear, consultar por ID, verificar unicidad de nombre, editar (con bloqueo optimista) y listar/filtrar enfermedades por texto (`search`) e indicador (`requiereSacrificioSanitario`) ordenadas por nombre. |

### Contratos HTTP propuestos

| Método y ruta                          | Acceso             | Parámetros / Cabeceras                                               | Responsabilidad                                                              |
| -------------------------------------- | ------------------ | -------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| `POST /api/enfermedades`               | `ROLE_VETERINARIO` | Header: `X-Idempotency-Key`                                          | Registrar enfermedad en el catálogo.                                         |
| `PUT /api/enfermedades/{enfermedadId}` | `ROLE_VETERINARIO` | Path: `enfermedadId`                                                 | Editar enfermedad para diagnósticos futuros (valida `version`).              |
| `GET /api/enfermedades`                | `ROLE_VETERINARIO` | Query (opc): `?search=texto&requiereSacrificioSanitario=true\|false` | Listar y filtrar el catálogo de enfermedades para selectores y tablas en UI. |

Errores usan `application/problem+json` según [General.md](General.md). Campos incompletos o compuestos por espacios responden `400`; usuario no veterinario responde `403`; enfermedad inexistente responde `404`; nombre duplicado o colisión de concurrencia (`version`) responde `409`.

### JSON común de errores

```json
{
  "type": "https://avicontrol/errors/enfermedad-invalida",
  "title": "Enfermedad inválida",
  "status": 400,
  "detail": "Debe indicar explícitamente si la enfermedad requiere sacrificio sanitario",
  "instance": "/api/enfermedades",
  "code": "CAMPO_OBLIGATORIO",
  "correlationId": "uuid",
  "fieldErrors": []
}
```

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Acordar catálogo de tipos (`TipoEnfermedad`), niveles de riesgo (`NivelRiesgo`) y campos obligatorios conforme a SPEC-009.
- [ ] T002 Confirmar roles (`ROLE_VETERINARIO`), filtro transversal de `X-Idempotency-Key` y respuestas de error con General.md.
- [ ] T003 Acordar con Plan 006 y Plan 015 el contrato público de consulta (`ConsultarEnfermedadesUseCase`) y los datos que se copian al snapshot del diagnóstico.

**Checkpoint**: Esquema de enfermedad, concurrencia y frontera de consumo en memoria acordados.

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T004 Implementar `Enfermedad`, `TipoEnfermedad` y `NivelRiesgo` con validaciones de dominio en Java puro (normalización `trim()`, campos obligatorios no vacíos e indicador booleano explícito).
- [ ] T005 Definir `EnfermedadRepositoryPort` y las excepciones del catálogo (`EnfermedadNoEncontradaException`, `NombreEnfermedadDuplicadoException`, `ConcurrenciaEnfermedadException`).
- [ ] T006 Crear la migración Flyway usando la siguiente versión disponible del repositorio, incluyendo columna `version`, restricción única de nombre normalizado y datos semilla iniciales (ej. Coccidiosis Aviar e Influenza Aviar) para pruebas autónomas.
- [ ] T007 Implementar `EnfermedadEntity`, `SpringDataEnfermedadRepository`, `EnfermedadPersistenceMapper` y `EnfermedadPersistenceAdapter` con soporte para `@Version` y sin borrado físico.
- [ ] T008 Configurar autorización, beans en `EnfermedadBeanConfiguration` y límites de transacción (`@Transactional`) para escrituras.

**Checkpoint**: Catálogo listo para registrar, editar y listar enfermedades a nivel de base de datos con semillas iniciales.

## Phase 3: User Story 1 — Registrar una enfermedad (Priority: P1)

**Spec**: SPEC-009, historia 1.

**Goal**: El veterinario crea una enfermedad con clasificación e indicador de sacrificio explícito.

**Independent Test**: Registro válido devuelve recurso creado (`201 Created`); campos ausentes, textos en blanco, indicador no enviado, nombre duplicado o actor sin rol veterinario se rechazan sin persistencia; reintentos de red con la misma clave de idempotencia no duplican la enfermedad.

### Definición del endpoint

`POST /api/enfermedades` recibe `nombre`, `nivelRiesgo`, `tipo`, `descripcionClinica` y `requiereSacrificioSanitario`. Evalúa `X-Idempotency-Key`. Responde `201 Created` con identificador, versión inicial y representación registrada.

#### JSON de solicitud

```json
{
  "nombre": "Bronquitis Infecciosa Aviar",
  "nivelRiesgo": "MEDIO",
  "tipo": "VIRAL",
  "descripcionClinica": "Afección respiratoria aguda con estertores y caída de consumo",
  "requiereSacrificioSanitario": false
}
```

#### JSON de respuesta

```json
{
  "enfermedadId": "uuid",
  "nombre": "Bronquitis Infecciosa Aviar",
  "nivelRiesgo": "MEDIO",
  "tipo": "VIRAL",
  "descripcionClinica": "Afección respiratoria aguda con estertores y caída de consumo",
  "requiereSacrificioSanitario": false,
  "version": 1
}
```

### Tests e implementación

- [ ] T009 [US1] Probar campos obligatorios, normalización de espacios (`trim`), tipos, riesgo, booleano explícito, rechazo de nombre duplicado (`409`) y autorización `ROLE_VETERINARIO`.
- [ ] T010 [US1] Probar la protección de idempotencia ante reintentos de red.
- [ ] T011 [US1] Implementar `RegistrarEnfermedadUseCase`, DTOs, mappers y endpoint `POST /api/enfermedades`.

**Checkpoint**: Enfermedad válida queda persistida, disponible para consulta y asegurada contra duplicidad.

## Phase 4: User Story 2 — Editar una enfermedad (Priority: P2)

**Spec**: SPEC-009, historia 2.

**Goal**: El veterinario actualiza la clasificación o el indicador para diagnósticos posteriores sin alterar snapshots previos.

**Independent Test**: Editar una enfermedad exige el número de `version` vigente; fallos de concurrencia responden `409 Conflict`. Editar la enfermedad preserva los valores históricos en los diagnósticos ya existentes y aplica los nuevos valores en diagnósticos posteriores.

### Definición del endpoint

`PUT /api/enfermedades/{enfermedadId}` valida todos los campos y el número de `version`. Responde `200 OK` con la entidad actualizada. No elimina la enfermedad ni modifica diagnósticos ya registrados.

#### JSON de solicitud

```json
{
  "nombre": "Bronquitis Infecciosa Aviar",
  "nivelRiesgo": "ALTO",
  "tipo": "VIRAL",
  "descripcionClinica": "Afección respiratoria severa con alta morbilidad en lote",
  "requiereSacrificioSanitario": false,
  "version": 1
}
```

### Tests e implementación

- [ ] T012 [US2] Probar edición válida, enfermedad inexistente (`404`) y autorización (`403` para roles distintos a veterinario).
- [ ] T013 [US2] Probar control de concurrencia optimista (actualización con versión obsoleta retorna `409 Conflict`).
- [ ] T014 [US2] Probar que la mutación aísla correctamente el cambio de los diagnósticos ya confirmados en el Plan 015.
- [ ] T015 [US2] Implementar `EditarEnfermedadUseCase`, actualización versionada, mapeo y endpoint `PUT /api/enfermedades/{enfermedadId}`.

**Checkpoint**: Las ediciones son consistentes y afectan únicamente diagnósticos futuros.

## Phase 5: Consulta de Catálogo para la Interfaz y Módulos Internos

**Spec**: SPEC-009, FR-001 y FR-007.

**Goal**: Proveer el listado y filtrado de enfermedades registradas para poblar los selectores y tablas en la interfaz del veterinario (Spec 008 y Spec 011).

### Definición del endpoint

`GET /api/enfermedades` permite consultar el catálogo de enfermedades con parámetros opcionales `?search=texto&requiereSacrificioSanitario=true|false`, devolviendo la lista ordenada por `nombre` con estado `200 OK`.

### Tests e implementación

- [ ] T016 Implementar `ConsultarEnfermedadesUseCase` soportando filtrado opcional por texto (`search`), por indicador `requiereSacrificioSanitario` y consulta individual por ID.
- [ ] T017 Implementar endpoint `GET /api/enfermedades` asegurando acceso exclusivo para `ROLE_VETERINARIO`.
- [ ] T018 Probar que la consulta opera estrictamente en modo de solo lectura y devuelve el catálogo vigente en menos de 1 segundo.

**Checkpoint**: El frontend puede renderizar y filtrar los selectores de enfermedades para prescribir medicaciones y registrar diagnósticos.

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T019 Documentar los tres endpoints (`POST`, `PUT`, `GET`), parámetros de filtrado (`search`, `requiereSacrificioSanitario`), seguridad, `X-Idempotency-Key` y códigos de error (`400`, `403`, `404`, `409`) en OpenAPI 3.
- [ ] T020 Verificar que el Plan 015 consuma adecuadamente los snapshots y que el Plan 006 valide las enfermedades tratables a través del contrato público en memoria.
- [ ] T021 Verificar reglas de arquitectura con ArchUnit conforme a [General.md](General.md).
- [ ] T022 Medir tiempos de latencia y asegurar el objetivo de `p95 < 1 segundo` para registros, ediciones y consultas.

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)** precede a **Foundational (Phase 2)**.
- **Foundational (Phase 2)** habilita el registro, la edición y la consulta del catálogo.
- **US1 (Phase 3)**, **US2 (Phase 4)** y **Consulta (Phase 5)** habilitan la integración con el frontend y con los Planes 006 y 015.
- **Polish (Phase 6)** consolida las validaciones y pruebas de integración del subdominio.

### Dependencias con otros planes

- **Plan 015 (Diagnóstico)**: Consulta la enfermedad en memoria para validar su existencia y captura el snapshot (`nombreEnfermedadHistorico` y `requiereSacrificioHistorico`) al confirmar el diagnóstico.
- **Plan 006 (Medicación)**: Consulta este catálogo en memoria para asegurarse de que la enfermedad referenciada existe y es tratable (`requiereSacrificioSanitario == false`) antes de permitir el registro o edición de una medicación.
- **Plan 008 (Sacrificio Sanitario)**: Consume de forma indirecta la condición de sacrificio a través del diagnóstico confirmado en el Plan 015.
- **General.md**: Seguridad, auditoría, errores RFC 9457, persistencia, idempotencia y control de transacciones.

### Dentro de cada User Story

- Validar reglas de dominio y normalización (`trim()`) antes de persistir.
- Mantener los controladores como adaptadores HTTP puros, delegando la lógica de negocio y concurrencia a los Casos de Uso.
- Probar la concurrencia optimista (`@Version`) para evitar sobrescrituras silenciosas en el catálogo.

## Notes

- Este plan no solicita aislamiento, no registra diagnósticos y no emite órdenes de sacrificio sanitario.
- El comportamiento histórico de protección a diagnósticos pasados se garantiza mediante la inmutabilidad de los snapshots almacenados por el Plan 015.
- El catálogo no incorpora un ciclo de activación/desactivación ni permite borrado físico (`DELETE`), preservando la integridad referencial histórica.
