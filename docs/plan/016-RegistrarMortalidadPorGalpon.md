# Implementation Plan: Registrar mortalidad por galpón

**Date**: 05/11/2026
**Spec**: [016-RegistrarMortalidadPorGalpon.md](../specs/016-RegistrarMortalidadPorGalpon.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Registrar eventos inmutables de mortalidad para el lote activo de un galpón y solicitar la actualización atómica de su población viva. Cada evento se valida contra población, fecha de ingreso y fecha/hora actual; si supera el umbral, se clasifica como anormal y publica la extensión sanitaria de los Planes 017 y 018.

## Technical Context

**Performance Goals**: confirmar población y registro de forma consistente en un segundo.
**Constraints**: `ROLE_TRABAJADOR` o administrador autorizado; cantidad entera positiva no superior a población; fecha entre ingreso y ahora; sin edición ni borrado.
**Scale/Scope**: mortalidad, actualización de inventario vivo y detección; no procesa la alerta ni diagnostica la causa clínica.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 016-RegistrarMortalidadPorGalpon.md
└── plan/
    └── 016-RegistrarMortalidadPorGalpon.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/mortalidad/
│   │   ├── CausaMortalidad.java
│   │   ├── ClasificacionMortalidad.java
│   │   └── RegistroMortalidad.java
│   ├── exception/mortalidad/
│   │   ├── CantidadMortalidadInvalidaException.java
│   │   ├── FechaMortalidadInvalidaException.java
│   │   └── LoteActivoNoEncontradoException.java
│   └── port/out/mortalidad/
│       ├── ActualizarInventarioVivoPort.java
│       ├── RegistroMortalidadRepositoryPort.java
│       └── UmbralMortalidadQueryPort.java
├── application/
│   └── mortalidad/
│       ├── RegistrarMortalidadCommand.java
│       ├── RegistrarMortalidadUseCase.java
│       └── result/RegistroMortalidadResult.java
└── infrastructure/
    ├── adapter/in/rest/mortalidad/
    │   ├── MortalidadController.java
    │   ├── dto/
    │   │   ├── RegistrarMortalidadRequest.java
    │   │   └── RegistroMortalidadResponse.java
    │   └── mapper/MortalidadRestMapper.java
    ├── adapter/out/persistence/mortalidad/RegistroMortalidadPersistenceAdapter.java
    └── config/MortalidadBeanConfiguration.java

src/main/resources/db/migration/
└── V__crear_registros_mortalidad.sql

src/test/java/com/avicontrol/
├── domain/model/mortalidad/RegistroMortalidadTest.java
├── application/mortalidad/RegistrarMortalidadUseCaseTest.java
└── infrastructure/adapter/in/rest/mortalidad/MortalidadControllerTest.java
```

**Structure Decision**: el evento de mortalidad es evidencia local inmutable. El lote y su población son propiedad del Módulo 1 y se actualizan a través de un puerto; la clasificación anormal sólo genera un evento o puerto de extensión, no implementa la alerta aquí.

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Confirmar el puerto del Plan 019 para disminuir población con control de concurrencia.
- [ ] T002 Acordar umbral diario y contrato de extensión hacia los Planes 017/018.
- [ ] T003 Crear migración de eventos inmutables con usuario, lote, galpón, fecha de evento y clasificación.

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T004 Crear `RegistroMortalidad` y validar cantidad, fecha/hora, causa y observaciones.
- [ ] T005 Consultar el lote activo más reciente y validar cantidad contra población vigente.
- [ ] T006 Calcular clasificación normal/anormal según el umbral sobre población viva.
- [ ] T007 Definir la operación atómica o compensable entre guardar evento y disminuir población.

**Checkpoint**: ningún evento inválido o parcialmente confirmado altera la población.

## Phase 3: User Story 1 - Registrar evento de mortalidad (Priority: P1)

**Goal**: crear un registro trazable y actualizar el inventario vivo del lote activo.

**Independent Test**: registrar cuatro muertes para población 7.960 crea el evento y deja 7.956; cantidad superior, fecha futura o sin lote no crean ni actualizan nada.

- [ ] T008 [P] [US1] Probar cantidad, fechas, población igual a cantidad, lote ausente e idempotencia.
- [ ] T009 [US1] Implementar `RegistrarMortalidadUseCase`, persistencia y actualización de inventario vivo.

## Phase 4: User Story 2 - Detectar mortalidad anormal (Priority: P2)

**Goal**: clasificar un evento que supera el umbral y activar el flujo sanitario sin bloquear el registro válido.

**Independent Test**: mortalidad igual al 100 % queda registrada como anormal y se emite una única extensión sanitaria; un evento normal no la emite.

- [ ] T010 [P] [US2] Probar umbral, múltiples eventos del día y publicación sólo después de confirmación.
- [ ] T011 [US2] Publicar el evento de mortalidad anormal para consumo del Plan 017/018.

## Endpoints

| Método y ruta | Roles | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `POST /api/galpones/{galponId}/mortalidad` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` | `RegistrarMortalidadRequest` | `201 Created` con evento y población resultante |

```json
{
  "fechaHoraEvento": "2026-11-05T08:00:00-05:00",
  "cantidad": 4,
  "causa": "ENFERMEDAD",
  "observaciones": "Hallazgo diario"
}
```

```json
{
  "id": "b07f5fd4-3b77-4e9a-a4eb-5d54f907c8a4",
  "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "cantidad": 4,
  "clasificacion": "NORMAL",
  "poblacionResultante": 7956
}
```

**Errores**: `400` cantidad, fecha o causa inválidas; `401` no autenticado; `403` sin rol; `404` galpón o lote ausente; `409` población insuficiente; `503` Módulo 1 no disponible.

## Polish & Cross-Cutting Concerns

- [ ] T012 Documentar inmutabilidad, umbral y enlace con mortalidad anormal.
- [ ] T013 Añadir métricas de eventos anormales y fallos de actualización de población.
- [ ] T014 Ejecutar pruebas y `git diff --check`.

## Dependencies & Execution Order

- **Setup** depende del contrato de población viva y umbral.
- **Foundational** bloquea ambas historias.
- **US1** confirma el evento; **US2** actúa sólo después de esa confirmación.

## Notes

- Registros confirmados no se editan ni eliminan; las correcciones requieren un procedimiento compensatorio futuro.
- El Plan 017 define la gestión de la extensión sanitaria y el Plan 019 la implementación detallada de inventario vivo.
