# Plan: Registrar peso de pollos finales

**Spec**: [006-RegistrarPesoDePollosFinales.md](../specs/006-RegistrarPesoDePollosFinales.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

El administrador registra una vez el peso total y cantidad final de una orden productiva ya ejecutada; puede corregirlos hasta que Módulo 3 confirme su uso.

## Fuera de alcance

Programación/ejecución de la orden, liquidación comercial o modificación de población.

## Dependencias

Plan 005/ejecución productiva y contrato de confirmación de uso de Módulo 3.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/model/sacrificio/productivo/{ResultadoSacrificioProductivo.java,EstadoResultadoSacrificio.java}
├── domain/port/out/sacrificio/productivo/{ResultadoSacrificioRepositoryPort,OrdenSacrificioProductivoQueryPort}.java
├── application/sacrificio/productivo/{RegistrarResultadosSacrificioUseCase,CorregirResultadosSacrificioUseCase}.java
├── infrastructure/adapter/in/rest/sacrificio/productivo/ResultadosSacrificioController.java
├── infrastructure/adapter/in/event/sacrificio/productivo/Modulo3LiquidacionLoteEventListener.java
└── infrastructure/adapter/out/persistence/sacrificio/productivo/ResultadoSacrificioPersistenceAdapter.java
src/main/resources/db/migration/V__crear_resultados_sacrificio_productivo.sql
```

## User Story 1 — Registrar resultado

| Método y ruta | Rol |
| --- | --- |
| `POST /api/ordenes-sacrificio/{ordenId}/resultados` | `ROLE_ADMINISTRADOR` |

```json
{"cantidadFinalPollos":7800,"pesoTotalKg":18720.50}
```

```json
{"id":"uuid","ordenId":"uuid","cantidadFinalPollos":7800,"pesoTotalKg":18720.50,"estado":"REGISTRADO"}
```

## User Story 2 — Corregir resultado

| Método y ruta | Rol |
| --- | --- |
| `PUT /api/ordenes-sacrificio/{ordenId}/resultados` | `ROLE_ADMINISTRADOR` |

Errores: `400` valores no positivos, `404` orden/resultado inexistente, `409` orden no ejecutada, resultado duplicado o ya usado por Módulo 3.

## Pruebas y Definition of Done

- Registro único, corrección permitida y bloqueo tras evento de liquidación.
- Concurrencia, auditoría y publicación posterior a confirmación.
- Migración, OpenAPI y pruebas verdes.
