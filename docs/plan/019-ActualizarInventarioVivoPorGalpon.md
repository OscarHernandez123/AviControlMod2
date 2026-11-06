# Implementation Plan: Actualizar inventario vivo por galpón

**Date**: 05/11/2026
**Spec**: [019-ActualizarInventarioVivoPorGalpon.md](../specs/019-ActualizarInventarioVivoPorGalpon.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Exponer un caso de uso interno que disminuye la población actual del lote activo tras una mortalidad confirmada. El Módulo 1 sigue siendo propietario del lote: este módulo no duplica tablas, valida el comando y solicita una actualización idempotente y concurrentemente segura.

## Technical Context

**Performance Goals**: aplicar el descuento dentro de la confirmación de mortalidad.
**Constraints**: cantidad positiva, saldo no negativo, población inicial inmutable, lote y galpón consistentes, sin endpoint público.
**Scale/Scope**: actualización interna por bajas confirmadas; no captura mortalidad, no realiza sacrificio ni crea alertas.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 019-ActualizarInventarioVivoPorGalpon.md
└── plan/
    └── 019-ActualizarInventarioVivoPorGalpon.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── exception/modulo1/
│   │   ├── PoblacionVivaInsuficienteException.java
│   │   └── ReferenciaLoteInconsistenteException.java
│   └── port/out/modulo1/ActualizarInventarioVivoPort.java
├── application/modulo1/
│   ├── ActualizarInventarioVivoCommand.java
│   ├── ActualizarInventarioVivoUseCase.java
│   └── result/InventarioVivoActualizadoResult.java
└── infrastructure/
    ├── adapter/out/integration/modulo1/InventarioVivoModulo1Adapter.java
    └── config/InventarioVivoBeanConfiguration.java

src/test/java/com/avicontrol/
├── application/modulo1/ActualizarInventarioVivoUseCaseTest.java
└── infrastructure/adapter/out/integration/modulo1/InventarioVivoModulo1AdapterTest.java
```

**Structure Decision**: se usa un puerto de actualización del Módulo 1 y no hay entidad, repositorio ni migración local para `Lote`. El adaptador transmite versión e idempotency key para que el propietario proteja la concurrencia.

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Acordar con el Módulo 1 un comando de descuento que reciba lote, galpón, bajas, causa, actor, versión y clave de idempotencia.
- [ ] T002 Confirmar que el contrato mantiene inmutable `poblacionInicial` y devuelve población anterior y resultante.
- [ ] T003 Preparar pruebas de saldo total, saldo insuficiente, reintento y versión obsoleta.

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T004 Crear `ActualizarInventarioVivoCommand` con causa trazable e idempotency key obligatoria.
- [ ] T005 Validar cantidad positiva, lote/galpón presentes y coherencia de referencias.
- [ ] T006 Implementar el puerto y adaptador con actualización condicional o bloqueo a nivel del lote.

**Checkpoint**: una misma causa confirmada no se descuenta dos veces y nunca deja población negativa.

## Phase 3: User Story 1 - Actualizar población viva (Priority: P1)

**Goal**: disminuir exactamente las bajas de la población actual y conservar la población inicial.

**Independent Test**: sobre 8.000 aves, 40 bajas dejan 7.960; sobre 25, 25 dejan cero; 60 sobre 50 se rechaza sin modificar nada.

- [ ] T007 [P] [US1] Probar cálculo, cero, saldo negativo, idempotencia y concurrencia.
- [ ] T008 [US1] Implementar `ActualizarInventarioVivoUseCase` y adaptador de integración.
- [ ] T009 [US1] Integrar el caso de uso con el Plan 016 después de confirmar el registro de mortalidad.

## Endpoints

No expone endpoints HTTP públicos: es un contrato interno de aplicación invocado por casos de uso autorizados.

### Contrato interno

```json
{
  "loteId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "cantidadBajas": 4,
  "causa": "MORTALIDAD",
  "idempotencyKey": "b07f5fd4-3b77-4e9a-a4eb-5d54f907c8a4"
}
```

```json
{
  "loteId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "poblacionAnterior": 7960,
  "variacion": -4,
  "poblacionResultante": 7956,
  "causa": "MORTALIDAD"
}
```

**Errores**: `404` lote ausente; `409` saldo insuficiente, referencia inconsistente o versión obsoleta; `503` Módulo 1 no disponible.

## Polish & Cross-Cutting Concerns

- [ ] T010 Añadir métricas de reintentos, conflictos de versión y descuentos aplicados.
- [ ] T011 Documentar consumidores permitidos y ejecutar pruebas más `git diff --check`.

## Dependencies & Execution Order

- **Setup** depende del contrato del Módulo 1.
- **Foundational** bloquea el consumo desde Plan 016.
- **US1** se integra sólo después de que el evento de mortalidad sea válido.

## Notes

- `poblacionActual` puede llegar a cero, nunca ser negativa.
- La actualización por sacrificio tiene reglas propias; no debe reutilizar este caso de uso sin un contrato explícito.
