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
src/main/java/com/avicontrol/application/modulo1/ActualizarInventarioVivoCommand.java
src/main/java/com/avicontrol/application/modulo1/ActualizarInventarioVivoUseCase.java
src/main/java/com/avicontrol/application/modulo1/result/InventarioVivoActualizadoResult.java
src/main/java/com/avicontrol/domain/exception/modulo1/PoblacionVivaInsuficienteException.java
src/main/java/com/avicontrol/domain/exception/modulo1/ReferenciaLoteInconsistenteException.java
src/main/java/com/avicontrol/domain/port/out/modulo1/ActualizarInventarioVivoPort.java
src/main/java/com/avicontrol/infrastructure/adapter/out/integration/modulo1/InventarioVivoModulo1Adapter.java
src/main/java/com/avicontrol/infrastructure/adapter/out/integration/modulo1/InventarioVivoNoDisponibleAdapter.java
src/main/java/com/avicontrol/infrastructure/config/InventarioVivoBeanConfiguration.java
src/main/resources/db/migration/V20__crear_inventario_vivo_y_mortalidad.sql
src/test/java/com/avicontrol/infrastructure/adapter/in/rest/mortalidad/MortalidadAlertaIntegrationTest.java
```

**Structure Decision**: se usa un puerto de actualización del Módulo 1 y no hay entidad ni repositorio local para `Lote`. Mientras no esté disponible la integración real, el perfil local/test usa una migración de soporte y un adaptador transaccional con bloqueo del registro, versión y clave de idempotencia. Fuera de ese perfil, el adaptador responde que el Módulo 1 no está disponible.

## Phase 1: Setup (Shared Infrastructure)

- [x] T001 Definir el contrato del puerto hacia el Módulo 1 con lote, galpón, bajas, causa, actor, versión y clave de idempotencia; la conexión real se hará cuando esté disponible.
- [x] T002 Confirmar que el adaptador local sólo modifica población actual y devuelve población anterior y resultante; `poblacionInicial` pertenece al Módulo 1.
- [x] T003 Preparar pruebas de saldo total, saldo insuficiente, reintento y versión obsoleta.

## Phase 2: Foundational (Blocking Prerequisites)

- [x] T004 Crear `ActualizarInventarioVivoCommand` con causa trazable e idempotency key obligatoria.
- [x] T005 Validar cantidad positiva, lote/galpón presentes y coherencia de referencias.
- [x] T006 Implementar el puerto y adaptador con actualización condicional o bloqueo a nivel del lote.

**Checkpoint**: una misma causa confirmada no se descuenta dos veces y nunca deja población negativa.

## Phase 3: User Story 1 - Actualizar población viva (Priority: P1)

**Goal**: disminuir exactamente las bajas de la población actual y conservar la población inicial.

**Independent Test**: sobre 8.000 aves, 40 bajas dejan 7.960; sobre 25, 25 dejan cero; 60 sobre 50 se rechaza sin modificar nada.

- [x] T007 [P] [US1] Probar cálculo, cero, saldo negativo, idempotencia y concurrencia.
- [x] T008 [US1] Implementar `ActualizarInventarioVivoUseCase` y adaptador de integración.
- [x] T009 [US1] Integrar el caso de uso con el Plan 016 después de confirmar el registro de mortalidad.

## Endpoints

No expone endpoints HTTP públicos: es un contrato interno de aplicación invocado por casos de uso autorizados.

### Contrato interno

```json
{
  "loteId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "cantidadBajas": 4,
  "causa": "MORTALIDAD",
  "actor": "operario",
  "versionEsperada": 0,
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

- [x] T010 Añadir métricas de reintentos, conflictos de versión y descuentos aplicados.
- [x] T011 Documentar consumidores permitidos y ejecutar pruebas más `git diff --check`.

## Dependencies & Execution Order

- **Setup** depende del contrato del Módulo 1.
- **Foundational** bloquea el consumo desde Plan 016.
- **US1** se integra sólo después de que el evento de mortalidad sea válido.

## Notes

- `poblacionActual` puede llegar a cero, nunca ser negativa.
- La actualización por sacrificio tiene reglas propias; no debe reutilizar este caso de uso sin un contrato explícito.
- Consumidor autorizado: `RegistrarMortalidadUseCase` del Plan 016. El puerto valida lote y galpón, cantidad positiva, versión y clave de idempotencia; el adaptador local sólo actualiza `poblacion_viva` y conserva el resto de los datos del galpón.
- Los contadores `avicontrol.inventario_vivo.reintentos`, `avicontrol.inventario_vivo.conflictos_version` y `avicontrol.inventario_vivo.descuentos_aplicados` cubren reintentos, versión obsoleta y descuentos confirmados.
- La conexión con el Módulo 1 real sigue pendiente; fuera de `local` y `test`, el adaptador responde `503` y no simula descuentos.
- Verificado el 12/11/2026 con saldo cero, saldo insuficiente, versión obsoleta, reintento y descuentos concurrentes en H2 y PostgreSQL.
