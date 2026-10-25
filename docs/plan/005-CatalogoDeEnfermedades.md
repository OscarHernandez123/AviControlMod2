# Implementation Plan: Catálogo de Enfermedades

**Date**: 25/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [009-RegistrarEnfermedad.md](../specs/009-RegistrarEnfermedad.md)

## Summary

Permitir al veterinario registrar y editar enfermedades con su clasificación clínica y el indicador obligatorio que señala si la enfermedad requiere sacrificio sanitario. Los diagnósticos capturan una instantánea de los datos de enfermedad para conservar su significado histórico.

## Technical Context

**Performance Goals**: El 95 % de registros y ediciones válidos responde en menos de 1 segundo.

**Constraints**: Solo `ROLE_VETERINARIO` registra o edita. Todos los atributos son obligatorios; `requiereSacrificioSanitario` debe enviarse explícitamente. Las ediciones aplican a diagnósticos futuros y no alteran diagnósticos existentes.

**Scale/Scope**: SPEC-009, dos historias (registrar y editar), una entidad propia y endpoints de escritura/consulta interna.

**Dependencias funcionales**: General.md para seguridad, actor, auditoría, errores y transacciones; Plan 015 consulta y conserva snapshots de enfermedad al diagnosticar; Plan 008 consume la condición de sacrificio a través del diagnóstico de Plan 015.

### Decisiones específicas

1. `Enfermedad` almacena nombre, nivel de riesgo, tipo, descripción clínica y `requiereSacrificioSanitario` obligatorio.
2. La edición de una enfermedad no modifica snapshots de diagnósticos ya confirmados.
3. La compatibilidad de medicación y el diagnóstico pertenecen al Plan 015; este plan no prescribe tratamientos.
4. El indicador verdadero significa que la enfermedad habilita una orden sanitaria total, pero este plan no emite la orden.
5. Persistencia propia y migraciones coordinadas con la secuencia común; no se duplican entidades de otros planes.

### Alcance documental

- Este plan implementa SPEC-009 y es dueño del catálogo de enfermedades.
- [Plan 015](015-DiagnosticoYReintegroSanitario.md) consulta enfermedades y conserva los atributos usados en cada diagnóstico.
- [Plan 008](008-GestionDelSacrificioSanitario.md) solo recibe el diagnóstico resultante para ejecutar el flujo de sacrificio.

## Project Structure

### Documentation (this feature)

```text
docs/specs/009-RegistrarEnfermedad.md
docs/plan/005-CatalogoDeEnfermedades.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/model/sanitario/enfermedad/
│   ├── Enfermedad.java
│   ├── TipoEnfermedad.java
│   └── NivelRiesgo.java
├── domain/port/out/sanitario/EnfermedadRepositoryPort.java
├── application/sanitario/enfermedad/
│   ├── RegistrarEnfermedadUseCase.java
│   └── EditarEnfermedadUseCase.java
└── infrastructure/
    ├── adapter/in/rest/sanitario/EnfermedadController.java
    ├── adapter/out/persistence/sanitario/enfermedad/
    └── config/EnfermedadBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/sanitario/enfermedad/
├── application/sanitario/enfermedad/
└── infrastructure/adapter/in/rest/sanitario/
```

**Structure Decision**: El catálogo y sus reglas son propios del dominio sanitario. Los demás planes consultan por puerto y conservan sus snapshots sin modificar esta entidad.

### Entidades y relación

```text
Enfermedad
├── id: UUID
├── nombre: String
├── nivelRiesgo: NivelRiesgo
├── tipo: TipoEnfermedad
├── descripcionClinica: String
├── requiereSacrificioSanitario: boolean (obligatorio)
```

Un diagnóstico referencia la enfermedad y conserva los atributos de decisión utilizados al confirmarse; no mantiene una relación mutable que reescriba el historial.

### Contratos de los puertos

| Puerto | Responsabilidad |
| --- | --- |
| `EnfermedadRepositoryPort` | Crear, consultar por ID, editar y listar enfermedades registradas. |
| `IntegrationEventPublisherPort` | Publicar hechos de catálogo si hay consumidores externos acordados. |

### Contratos HTTP propuestos

| Método y ruta | Acceso | Responsabilidad |
| --- | --- | --- |
| `POST /api/enfermedades` | `ROLE_VETERINARIO` | Registrar enfermedad. |
| `PUT /api/enfermedades/{enfermedadId}` | `ROLE_VETERINARIO` | Editar para diagnósticos futuros. |

Errores usan `application/problem+json` según General.md. Campos incompletos responden 400; enfermedad inexistente, 404.

### JSON común de errores

```json
{
  "type": "https://avicontrol/errors/enfermedad-invalida",
  "title": "Enfermedad inválida",
  "status": 400,
  "detail": "Debe indicar si la enfermedad requiere sacrificio sanitario",
  "instance": "/api/enfermedades",
  "code": "CAMPO_OBLIGATORIO",
  "correlationId": "uuid",
  "fieldErrors": []
}
```

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Acordar catálogo de tipos, niveles de riesgo y campos obligatorios conforme a SPEC-009.
- [ ] T002 Confirmar roles, actor, auditoría y respuestas de error con General.md.
- [ ] T003 Acordar con Plan 015 la consulta y snapshot de enfermedad para diagnóstico.

**Checkpoint**: Esquema de enfermedad y frontera de consumo acordados.

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T004 Implementar `Enfermedad` y validaciones de dominio.
- [ ] T005 Definir el puerto de persistencia y excepciones del catálogo.
- [ ] T006 Crear migración y adaptador de persistencia.
- [ ] T007 Configurar autorización y transacción para escrituras.

**Checkpoint**: Catálogo listo para registrar y editar enfermedades.

## Phase 3: User Story 1 — Registrar una enfermedad (Priority: P1)

**Spec**: SPEC-009, historia 1.

**Goal**: El veterinario crea enfermedad con clasificación e indicador de sacrificio explícito.

**Independent Test**: Registro válido devuelve recurso creado; campos ausentes, indicador no enviado o actor sin rol veterinario se rechazan sin persistencia.

### Definición del endpoint

`POST /api/enfermedades` recibe nombre, nivel de riesgo, tipo, descripción clínica y `requiereSacrificioSanitario`. Responde 201 con identificador y representación registrada.

### Tests e implementación

- [ ] T008 Probar campos obligatorios, tipos, riesgo, booleano explícito y autorización.
- [ ] T009 Implementar caso de uso, DTOs, mappers y endpoint.

**Checkpoint**: Enfermedad válida disponible para diagnóstico.

## Phase 4: User Story 2 — Editar una enfermedad (Priority: P2)

**Spec**: SPEC-009, historia 2.

**Goal**: El veterinario actualiza clasificación o indicador para diagnósticos posteriores sin alterar snapshots previos.

**Independent Test**: Editar enfermedad asociada a diagnóstico preserva valores históricos en ese diagnóstico; diagnóstico nuevo consulta los valores actualizados.

### Definición del endpoint

`PUT /api/enfermedades/{enfermedadId}` valida todos los campos y responde 200. No elimina la enfermedad ni modifica diagnósticos ya registrados.

### Tests e implementación

- [ ] T010 Probar edición válida, enfermedad inexistente y autorización.
- [ ] T011 Probar que los diagnósticos existentes conservan la decisión original.
- [ ] T012 Implementar actualización versionada, mapeo y endpoint.

**Checkpoint**: Las ediciones afectan únicamente diagnósticos futuros.

## Phase 5: Polish & Cross-Cutting Concerns

- [ ] T013 Documentar API, roles y errores en OpenAPI.
- [ ] T014 Verificar snapshots consumidos por Plan 015 y límites de arquitectura.
- [ ] T015 Completar criterios de SPEC-009 y objetivos de respuesta.

## Dependencies & Execution Order

### Phase Dependencies

- Setup precede a Foundational.
- Foundational habilita registro y edición.
- Edición depende del catálogo y conserva historia para Plan 015.
- Polish depende de ambas historias.

### Dependencias con otros planes

- **Plan 015**: Consulta enfermedad y captura snapshot al diagnosticar.
- **Plan 008**: Consume el resultado clínico que identifica condición de sacrificio.
- **General.md**: Seguridad, auditoría, errores, persistencia y transacciones.

### Dentro de cada User Story

- Validar reglas de dominio antes de persistir.
- Mantener controladores como adaptadores HTTP, sin reglas de negocio.
- Probar que cambios no reescriben diagnósticos existentes.

## Notes

- El plan no solicita aislamiento, no registra diagnósticos y no emite órdenes de sacrificio.
- El comportamiento histórico del diagnóstico se define en Plan 015.
- El catálogo no incorpora un ciclo de activación/desactivación. Plan 015 valida que la enfermedad seleccionada exista en el catálogo y conserva en el diagnóstico los datos clínicos usados al confirmarlo.
