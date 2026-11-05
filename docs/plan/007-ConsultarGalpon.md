# Plan: Consultar galpón

**Spec**: [007-ConsultarGalpon.md](../specs/007-ConsultarGalpon.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

Detalle, resumen y listado paginado de galpones con datos vigentes del Módulo 1.

## Fuera de alcance

Edad exclusiva de administrador (Plan 003), escrituras de galpón/lote, mortalidad y filtros por trabajador.

## Dependencias

Puertos públicos de Módulo 1 y modelo/valor `Edad` reutilizable del Plan 003.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/model/galpon/{Galpon.java,EstadoGalpon.java}
├── domain/port/out/modulo1/{GalponQueryPort.java,LoteQueryPort.java}
├── application/galpon/{ConsultarGalponUseCase,ConsultarResumenGalponesUseCase,ConsultarListadoGalponesUseCase}.java
├── application/galpon/result/{GalponDetalleResult,ResumenGalponesResult,GalponListadoResult,ListadoGalponesResult}.java
├── infrastructure/adapter/in/rest/galpon/GalponController.java
├── infrastructure/adapter/in/rest/galpon/dto/{GalponDetalleResponse,ResumenGalponesResponse,ListadoGalponesResponse}.java
└── infrastructure/adapter/in/rest/galpon/mapper/GalponRestMapper.java
```

## User Story 1 — Detalle

| Método y ruta | Roles |
| --- | --- |
| `GET /api/galpones/{galponId}` | `ADMINISTRADOR`, `TRABAJADOR`, `USUARIO` |

```json
{"id":"uuid","nombre":"Galpón 1","aforoMaximo":8000,"estado":"PRODUCTIVO","loteActual":{"id":"uuid","poblacionActual":7960,"fechaIngreso":"2026-09-15","edad":{"diasTotales":17}},"incidencias":[]}
```

## User Story 2 — Resumen

| Método y ruta | Rol |
| --- | --- |
| `GET /api/galpones/resumen` | `ADMINISTRADOR` |

```json
{"totalRegistrado":13,"totalValido":12,"registrosInconsistentes":1,"porEstado":{"DISPONIBLE":2,"VACIADO_SANITARIO":1,"PRODUCTIVO":6,"EN_COSECHA":1,"MANTENIMIENTO":1,"AISLAMIENTO":1}}
```

## User Story 3 — Listado

| Método y ruta | Roles |
| --- | --- |
| `GET /api/galpones?page=0&size=20&sort=nombre,asc` | `ADMINISTRADOR`, `TRABAJADOR`, `USUARIO` |

```json
{"content":[{"id":"uuid","nombre":"Galpón 1","estado":"PRODUCTIVO"}],"page":0,"size":20,"totalElements":1,"totalPages":1,"first":true,"last":true}
```

Errores: `400` paginación/orden inválida, `404` detalle inexistente, `409` conteos incoherentes, `503` fuente no disponible.

## Pruebas y Definition of Done

- Seis estados presentes incluso con cero; totales coherentes.
- Listado vacío, orden permitido y los tres roles autorizados.
- DTOs REST separados, lecturas sin eventos ni escrituras.
