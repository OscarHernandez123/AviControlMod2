# Plan: Actualizar inventario vivo por galpón

**Spec**: [019-ActualizarInventarioVivoPorGalpon.md](../specs/019-ActualizarInventarioVivoPorGalpon.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

Contrato de aplicación para actualizar población viva de un lote por causa trazable; lo usan mortalidad y sacrificio, pero Módulo 1 conserva la propiedad.

## Fuera de alcance

Registro HTTP de mortalidad, alertas, diagnóstico y tablas espejo de lote.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/application/modulo1/ActualizarInventarioVivoUseCase.java
src/main/java/com/avicontrol/application/modulo1/ActualizarInventarioVivoCommand.java
src/main/java/com/avicontrol/domain/port/out/modulo1/ActualizarInventarioVivoPort.java
src/main/java/com/avicontrol/infrastructure/adapter/out/integration/modulo1/InventarioVivoModulo1Adapter.java
```

## User Story 1 — Actualizar población viva

No expone endpoint público en Módulo 2. Consumidores internos entregan `galponId`, `loteId`, cantidad, causa, clave idempotente y actor.

```json
{"loteId":"uuid","poblacionAnterior":7960,"variacion":-4,"poblacionResultante":7956,"causa":"MORTALIDAD"}
```

Errores: `404` lote, `409` población negativa/versión obsoleta, `503` Módulo 1 no disponible.

## Pruebas y Definition of Done

- Idempotencia, concurrencia y rechazo de saldo negativo.
- Ninguna tabla propia duplica lote; integración de consumidores verdes.
