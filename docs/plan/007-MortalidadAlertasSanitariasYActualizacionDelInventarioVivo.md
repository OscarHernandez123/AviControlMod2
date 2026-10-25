# Implementation Plan: Registro de Mortalidad e Inventario Vivo

**Date**: 25/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [016-RegistrarMortalidadPorGalpon.md](../specs/016-RegistrarMortalidadPorGalpon.md)
- [017-RegistrarMortalidadAnormal.md](../specs/017-RegistrarMortalidadAnormal.md)
- [019-ActualizarInventarioVivoPorGalpon.md](../specs/019-ActualizarInventarioVivoPorGalpon.md)

## Summary

Registrar las bajas ordinarias y anormales por galpón y actualizar atómicamente la población viva del lote activo. El Plan 001 provee las consultas de galpón/lote y el Módulo 1 mantiene la propiedad y actualización autoritativa de población. Cuando se detecta mortalidad anormal, este plan solicita la creación de alerta al Plan 013 mediante puerto público dentro del mismo límite transaccional.

## Technical Context

**Performance Goals**: Confirmar mortalidad y actualización de población en menos de 1 segundo para el 95 % de operaciones; evaluar y persistir una condición anormal en menos de 2 segundos;

**Constraints**: Población actual nunca negativa; población inicial inmutable; concurrencia serializada por lote; registros confirmados inmutables; no se registran bajas sin lote activo productivo; usuario trabajador o administrador autorizado registra mortalidad.

**Scale/Scope**: SPEC-016, SPEC-017 y SPEC-019; tres historias (actualizar inventario, registrar mortalidad ordinaria y registrar mortalidad anormal); dos endpoints REST y operaciones internas con Módulo 1 y Plan 013.

**Dependencias funcionales**:

- Plan 001 / Módulo 1: galpón, lote activo, estado, población actual y operación de descuento atómico.
- Plan 013: puerto de generación de alerta cuando una mortalidad resulta anormal.
- Planes 005 y 008: consumen alertas o hechos sanitarios según sus responsabilidades.
- Plan 006: consulta la población vigente al presentar medicación diaria.
- General.md: roles, actor, reloj, errores y eventos.

### Decisiones específicas

1. `RegistroMortalidad` identifica lote, galpón, actor, hora del evento y registro, cantidad y causa. Es inmutable.
2. `RegistroMortalidadAnormal` conserva el reporte, población previa, porcentaje calculado, severidad, causa probable y observaciones. Es inmutable.
3. El porcentaje se calcula como `cantidadMuertes / poblacionActualAnterior × 100`, con BigDecimal, escala y redondeo definidos con SPEC-017.
4. La actualización del lote se solicita al propietario del Módulo 1 en la misma operación lógica. Si el saldo es insuficiente o falla el descuento, no queda registro de mortalidad.
5. Registros concurrentes del mismo lote se serializan con bloqueo o control de versión en el propietario del inventario vivo.
6. Los umbrales y la clasificación de severidad siguen SPEC-016 y SPEC-017; no se duplican reglas de alerta en este plan.
7. Ante mortalidad anormal, el caso de uso invoca `GenerarAlertaSanitariaPort` del Plan 013 antes de confirmar la transacción.
8. El lote activo se obtiene por la relación vigente del Módulo 1. No se crean copias persistidas autoritativas de galpones o lotes.

### Alcance documental

- Este plan implementa registro ordinario/anormal e inventario vivo conforme a SPEC-016, SPEC-017 y SPEC-019.
- [Plan 013](013-GestionDeAlertasSanitarias.md) implementa SPEC-018: alta de alerta y consulta contextual veterinaria.
- Módulo 1 conserva la propiedad de galpones, lotes y población viva.
- La solicitud y validación de aislamiento pertenecen al Plan 014; el diagnóstico pertenece al Plan 015; el sacrificio sanitario al Plan 008.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   ├── 016-RegistrarMortalidadPorGalpon.md
│   ├── 017-RegistrarMortalidadAnormal.md
│   └── 019-ActualizarInventarioVivoPorGalpon.md
└── plan/007-MortalidadAlertasSanitariasYActualizacionDelInventarioVivo.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/model/mortalidad/
│   ├── RegistroMortalidad.java
│   ├── RegistroMortalidadAnormal.java
│   ├── CausaMortalidad.java
│   └── NivelSeveridad.java
├── domain/port/out/mortalidad/
│   ├── RegistroMortalidadRepositoryPort.java
│   ├── RegistroMortalidadAnormalRepositoryPort.java
│   ├── ActualizarInventarioVivoPort.java
│   └── GenerarAlertaSanitariaPort.java
├── application/mortalidad/
│   ├── RegistrarMortalidadUseCase.java
│   ├── RegistrarMortalidadAnormalUseCase.java
│   └── ConsultarHistorialMortalidadUseCase.java
└── infrastructure/
    ├── adapter/in/rest/mortalidad/MortalidadController.java
    ├── adapter/out/persistence/mortalidad/
    ├── adapter/out/internal/modulo1/
    └── config/MortalidadBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/mortalidad/
├── application/mortalidad/
├── infrastructure/adapter/in/rest/mortalidad/
└── integration/MortalidadInventarioIntegrationTest.java
```

**Structure Decision**: El dominio conserva reglas de mortalidad; el puerto del Módulo 1 aplica la mutación autoritativa de población. Los adaptadores no acceden a repositorios privados de otros módulos.

### Entidades y relación

```text
Galpon 1 ─── 0..1 LoteActivo (propietario: Módulo 1)
LoteActivo 1 ─── 0..* RegistroMortalidad 1 ─── 0..1 RegistroMortalidadAnormal

RegistroMortalidad: id, galponId, loteId, fechaHoraEvento, registradoEn,
  cantidadMuertes, causa, observaciones, esAnormal, registradoPor
RegistroMortalidadAnormal: id, registroMortalidadId, poblacionVivaMomento,
  porcentajeMortalidad, nivelSeveridad, causaProbable, observaciones
```

Cada baja confirmada reduce una vez la población actual. La población inicial no cambia. El caso anormal queda asociado al registro que lo originó; la alerta pertenece al Plan 013.

### Contratos de los puertos

| Puerto | Responsabilidad |
| --- | --- |
| `RegistroMortalidadRepositoryPort` | Guardar registros inmutables y recuperar eventos por ID. |
| `RegistroMortalidadAnormalRepositoryPort` | Guardar reportes anormales y consultar su detalle. |
| `ActualizarInventarioVivoPort` | Solicitar al Módulo 1 el descuento atómico con validación de población y concurrencia. |
| `GenerarAlertaSanitariaPort` | Solicitar al Plan 013 una alerta vinculada a la mortalidad anormal dentro de la transacción. |
| `GalponQueryPort` / `LoteQueryPort` | Consultar estado productivo, lote activo y población vigente. |

### Contratos HTTP propuestos

| Método y ruta | Acceso | Responsabilidad |
| --- | --- | --- |
| `POST /api/galpones/{galponId}/mortalidad` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` | Registrar baja ordinaria, actualizar población y evaluar umbral. |
| `POST /api/galpones/{galponId}/mortalidad-anormal` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` | Registrar el reporte anormal explícito y activar alerta. |

Errores y autorizaciones siguen General.md y RFC 9457. Población insuficiente responde 409; galpón o lote inexistente 404.

### JSON común de errores

```json
{
  "type": "https://avicontrol/errors/poblacion-insuficiente",
  "title": "Población insuficiente",
  "status": 409,
  "detail": "La cantidad de bajas supera la población viva actual del lote",
  "instance": "/api/galpones/{galponId}/mortalidad",
  "code": "POBLACION_INSUFICIENTE",
  "correlationId": "uuid",
  "fieldErrors": []
}
```

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Acordar puertos, concurrencia, roles y validaciones con el Módulo 1 y el Plan 013.

- [ ] T001 Confirmar consultas públicas de galpón y lote activo con estado, población y fecha de ingreso.
- [ ] T002 Acordar `ActualizarInventarioVivoPort`, bloqueo y tratamiento de saldo insuficiente con Módulo 1.
- [ ] T003 Acordar `GenerarAlertaSanitariaPort` y atomicidad con Plan 013.
- [ ] T004 Confirmar roles, Clock, causas, umbrales y severidades según specs.
- [ ] T005 Preparar fixtures de lote activo, lote sin población, galpón no productivo y concurrencia.

**Checkpoint**: Contratos e invariantes propietarios acordados.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Preparar dominio, persistencia y adaptadores requeridos por las historias.

- [ ] T006 Implementar entidades de mortalidad y sus invariantes sin dependencias de infraestructura.
- [ ] T007 Definir puertos de consulta, actualización de población, persistencia y generación de alerta.
- [ ] T008 Crear migraciones para mortalidad ordinaria y anormal, sin duplicar tablas de Módulo 1 ni Plan 013.
- [ ] T009 Implementar adaptadores, mappers y límites transaccionales.
- [ ] T010 Verificar no negatividad, población inicial inmutable e idempotencia de operación.

**Checkpoint**: Base de dominio lista para registrar mortalidad e invocar al propietario del lote.

## Phase 3: User Story 1 — Actualizar población viva por bajas (Priority: P1)

**Spec**: SPEC-019, historia 1.

**Goal**: Descontar la cantidad confirmada del lote activo sin afectar la población inicial ni permitir saldos negativos.

**Independent Test**: Deducir 40 de 8.000 aves deja 7.960; deducir todas deja cero; superar el saldo rechaza la operación y revierte cambios.

### Definición del evento

El evento interno `InventarioVivoActualizadoEvent` se publica únicamente tras confirmar el cambio en el propietario del lote. Campos y versión se acuerdan con Módulo 1.

### Contrato de aplicación

| Entrada | `loteId`, cantidad positiva, actor y clave idempotente derivados del registro confirmado. |
| Resultado | Nueva población actual y referencia de operación. |
| Error | Saldo insuficiente, lote inactivo o conflicto de concurrencia; sin cambio parcial. |

### Tests e implementación

- [ ] T011 Probar descuento exacto, reducción a cero, rechazo de saldo insuficiente e inmutabilidad de población inicial.
- [ ] T012 Probar dos actualizaciones concurrentes del mismo lote sin saldo negativo ni pérdida de actualización.
- [ ] T013 Implementar el adaptador de actualización del Módulo 1 y traducir errores de concurrencia.

**Checkpoint**: El puerto de población ofrece descuento atómico y verificable.

## Phase 4: User Story 2 — Registrar mortalidad ordinaria por galpón (Priority: P1)

**Spec**: SPEC-016, historia 1.

**Goal**: El trabajador registra fecha, causa y cantidad de bajas asociadas al lote activo, actualizando población en la misma operación.

**Independent Test**: Un registro válido crea historial y descuenta población; datos inválidos, galpón inexistente o población insuficiente no producen registro ni descuento.

### Definición del evento y endpoint

Se publica `MortalidadRegistradaEvent` tras persistir baja y descuento. La solicitud `POST /api/galpones/{galponId}/mortalidad` contiene fecha/hora, cantidad entera positiva, causa y observaciones opcionales. Respuesta 201 incluye registro, lote y población resultante.

### JSON de solicitud

```json
{"fechaHoraEvento":"2026-10-25T08:30:00","cantidadMuertes":4,"causa":"OTRA","observaciones":"Recorrido matutino"}
```

### Tests e implementación

- [ ] T014 Probar autorización, validación temporal, galpón/lote y causa.
- [ ] T015 Probar persistencia y actualización indivisible, incluso ante fallo del puerto.
- [ ] T016 Implementar caso de uso, DTOs, mapper y endpoint.

**Checkpoint**: Registro ordinario e inventario vivo permanecen consistentes.

## Phase 5: User Story 3 — Detectar y registrar mortalidad anormal (Priority: P1)

**Specs**: SPEC-016, historia 2; SPEC-017, historias 1 y 2.

**Goal**: Clasificar bajas por umbral o reporte explícito, calcular porcentaje/severidad y solicitar alerta al Plan 013.

**Independent Test**: El registro bajo umbral queda normal; al superar umbral se conserva evento anormal, actualiza población y se crea alerta; si falla cualquiera de las operaciones, no quedan efectos parciales.

### Definición del evento y endpoint

`MortalidadAnormalRegistradaEvent` contiene registroId, galponId, loteId, cantidad, población al momento, porcentaje, causa probable, severidad, observaciones y actor. El endpoint `POST /api/galpones/{galponId}/mortalidad-anormal` recibe fecha/hora, cantidad, causa probable y observaciones. La creación de alerta es una llamada al puerto público de Plan 013 dentro del mismo límite transaccional.

### Tests e implementación

- [ ] T017 Probar fórmula, escala, redondeo y clasificación de severidad conforme a SPEC-017.
- [ ] T018 Probar detección automática desde mortalidad ordinaria y reporte anormal explícito.
- [ ] T019 Probar rechazo por lote ausente, estado no productivo, fecha inválida o bajas superiores al saldo.
- [ ] T020 Implementar registro anormal, descuento, publicación del hecho y llamada transaccional al Plan 013.

**Checkpoint**: Toda mortalidad anormal confirmada actualiza población y queda asociada a una alerta.

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T021 Documentar contratos REST e internos en OpenAPI y documentación de arquitectura.
- [ ] T022 Probar atomicidad completa y concurrencia con base de datos de integración.
- [ ] T023 Verificar eventos, outbox, roles y límites de módulos conforme a General.md.
- [ ] T024 Medir objetivos de rendimiento de registro conforme a General.md.

## Dependencies & Execution Order

### Phase Dependencies

- Setup precede a Foundational.
- Foundational habilita las cuatro historias.
- Actualización de población precede a mortalidad ordinaria y anormal.
- Mortalidad anormal depende del descuento y del puerto del Plan 013.

- Polish depende de las historias y acuerdos de integración.

### Dependencias con otros planes

- **Módulo 1 / Plan 001**: propietario de galpones, lotes y población viva.
- **Plan 013**: genera alertas y proporciona el puerto de creación síncrona.
- **Plan 014**: puede usar alertas sanitarias como contexto para flujos de aislamiento.
- **Plan 006**: utiliza la población vigente para indicaciones diarias.
- **Plan 008**: atiende decisiones de sacrificio sanitario.
- **General.md**: seguridad, actor, tiempo, errores y eventos.

### Dentro de cada User Story

- Dominio antes de casos de uso, puertos antes de adaptadores y casos de uso antes de REST.
- Bajas, actualización de lote y alerta deben confirmarse de forma atómica según contrato.
- Tests y checkpoint cierran cada historia.

## Notes

- El inventario vivo es propiedad de Módulo 1; no se crean tablas paralelas de lote.
- Los registros confirmados son inmutables.
- SPEC-018 y sus consultas pertenecen al Plan 013.
