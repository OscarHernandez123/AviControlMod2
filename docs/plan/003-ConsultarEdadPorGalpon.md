# Plan: Consultar edad por galpón

**Spec**: [003-ConsultarEdadPorGalpon.md](../specs/003-ConsultarEdadPorGalpon.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

Consulta de edad del lote actualmente alojado, sin modificar galpón, lote ni inventario vivo.

## Fuera de alcance

Detalle general de galpón, listado, resumen, alojamiento y actualización de población.

## Dependencias

`GalponQueryPort` y `LoteQueryPort` del Módulo 1; `Clock` con zona `America/Bogota`.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/model/lote/Edad.java
├── domain/model/lote/Lote.java
├── domain/port/out/modulo1/GalponQueryPort.java
├── domain/port/out/modulo1/LoteQueryPort.java
├── application/galpon/ConsultarEdadPorGalponUseCase.java
├── application/galpon/result/EdadLoteResult.java
├── infrastructure/adapter/in/rest/galpon/GalponEdadController.java
├── infrastructure/adapter/in/rest/galpon/dto/EdadLoteResponse.java
└── infrastructure/adapter/in/rest/galpon/mapper/GalponRestMapper.java
src/test/java/com/avicontrol/{domain,application,infrastructure}/galpon/
```

## User Story 1 — Consultar edad del lote alojado

Como administrador, consulto la edad exacta del lote actual de un galpón.

| Método y ruta | Rol | Respuesta |
| --- | --- | --- |
| `GET /api/galpones/{galponId}/edad-lote` | `ROLE_ADMINISTRADOR` | Edad derivada o ausencia de lote |

```json
{"galponId":"uuid","loteId":"uuid","edad":{"diasTotales":17,"semanasCompletas":2,"diasRestantes":3,"texto":"2 semanas y 3 días (17 días)"},"mensaje":null}
```

Sin lote: `loteId` y `edad` son `null` y `mensaje` es `El galpón no tiene lote alojado actualmente`.

Errores: `400` UUID inválido, `404` galpón inexistente, `409` fecha futura o múltiples lotes actuales, `503` dependencia no disponible.

## Pruebas y Definition of Done

- Día de ingreso equivale a un día; incluir cambios de mes/año y bisiestos.
- MockMvc para administrador, no autorizado, ausencia e inconsistencias.
- No hay persistencia, eventos ni escrituras.
