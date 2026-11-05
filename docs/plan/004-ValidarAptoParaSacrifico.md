# Plan: Validar aptitud para sacrificio

**Spec**: [004-ValidarAptoParaSacrifico.md](../specs/004-ValidarAptoParaSacrifico.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

El administrador evalúa si el lote actual cumple las condiciones productivas y sanitarias para sacrificio. Una evaluación apta registra el resultado y habilita la programación; una no apta conserva motivos sin modificar inventario ni ejecutar sacrificio.

## Fuera de alcance

Programar, reprogramar, cancelar o ejecutar una orden; resultados finales, liquidación y sacrificio sanitario.

## Dependencias

Consultas vigentes de galpón/lote del Módulo 1 y puertos sanitarios para bloqueos activos.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/model/sacrificio/productivo/ValidacionAptitudSacrificio.java
├── domain/port/out/sacrificio/productivo/ValidacionAptitudRepositoryPort.java
├── domain/port/out/modulo1/{GalponQueryPort.java,LoteQueryPort.java}
├── application/sacrificio/productivo/ValidarAptitudSacrificioUseCase.java
├── application/sacrificio/productivo/result/ValidacionAptitudResult.java
├── infrastructure/adapter/in/rest/sacrificio/productivo/AptitudSacrificioController.java
├── infrastructure/adapter/in/rest/sacrificio/productivo/dto/ValidarAptitudRequest.java
└── infrastructure/adapter/out/persistence/sacrificio/productivo/ValidacionAptitudPersistenceAdapter.java
src/main/resources/db/migration/V__crear_validaciones_aptitud.sql
```

## User Story 1 — Evaluar aptitud

| Método y ruta | Rol | Respuesta |
| --- | --- | --- |
| `POST /api/galpones/{galponId}/validacion-aptitud-sacrificio` | `ROLE_ADMINISTRADOR` | Evaluación y estado resultante |

```json
{"fechaEvaluacion":"2026-11-05","pesoPromedioKg":2.4,"observaciones":"Lote apto"}
```

```json
{"id":"uuid","galponId":"uuid","loteId":"uuid","apto":true,"motivosNoAptitud":[],"fechaEvaluacion":"2026-11-05"}
```

Errores: `400` datos inválidos, `404` galpón/lote inexistente, `409` estado incompatible o bloqueo sanitario, `503` dependencia no disponible.

## Pruebas y Definition of Done

- Aptitud válida, lote sin población, estado no productivo y bloqueo sanitario.
- Autorización de administrador y ausencia de escritura fuera de la validación.
- Migración, pruebas unitarias/HTTP y OpenAPI verdes.
