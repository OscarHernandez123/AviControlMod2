# Implementation Plan: Consultar información de un galpón

**Date**: 05/11/2026
**Spec**: [007-ConsultarGalpon.md](../specs/007-ConsultarGalpon.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Exponer el detalle de cualquier galpón, un resumen por estado para administradores y un listado paginado para roles autorizados. Las tres consultas leen datos vigentes del Módulo 1, calculan la edad inclusiva del lote actual cuando exista y no modifican galpones ni lotes.

## Technical Context

**Performance Goals**: completar el 95 % de las consultas en un segundo.
**Constraints**: sólo lectura; detalle y listado para roles autorizados, resumen sólo para `ROLE_ADMINISTRADOR`; estados inválidos se reportan como inconsistencias.
**Scale/Scope**: detalle por UUID, resumen de seis estados permitidos y listado con paginación y orden explícito.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 007-ConsultarGalpon.md
└── plan/
    └── 007-ConsultarGalpon.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/galpon/
│   │   ├── EstadoGalpon.java
│   │   └── ResumenGalpones.java
│   ├── model/lote/Edad.java
│   ├── exception/galpon/
│   │   ├── EstadoGalponInvalidoException.java
│   │   └── FechaIngresoInconsistenteException.java
│   └── port/out/modulo1/
│       ├── GalponQueryPort.java
│       └── LoteQueryPort.java
├── application/
│   └── galpon/
│       ├── ConsultarGalponUseCase.java
│       ├── ConsultarListadoGalponesUseCase.java
│       ├── ConsultarResumenGalponesUseCase.java
│       └── result/
│           ├── GalponDetalleResult.java
│           ├── GalponListadoResult.java
│           ├── ListadoGalponesResult.java
│           └── ResumenGalponesResult.java
└── infrastructure/
    ├── adapter/in/rest/galpon/
    │   ├── GalponController.java
    │   ├── dto/
    │   │   ├── GalponDetalleResponse.java
    │   │   ├── ListadoGalponesResponse.java
    │   │   └── ResumenGalponesResponse.java
    │   └── mapper/GalponRestMapper.java
    ├── adapter/out/integration/modulo1/
    │   ├── GalponQueryAdapter.java
    │   └── LoteQueryAdapter.java
    └── config/GalponConsultaBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/galpon/ResumenGalponesTest.java
├── application/galpon/ConsultarGalponUseCaseTest.java
├── application/galpon/ConsultarListadoGalponesUseCaseTest.java
├── application/galpon/ConsultarResumenGalponesUseCaseTest.java
└── infrastructure/adapter/in/rest/galpon/GalponControllerTest.java
```

**Structure Decision**: no hay persistencia ni migración local. El dominio contiene los estados permitidos, el cálculo de resumen y edad; los adaptadores consultan, sin actualizar, las interfaces públicas del Módulo 1.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: acordar contratos de lectura, roles y ordenamiento permitido.

- [ ] T001 Confirmar que el Módulo 1 permite consultar galpón, lote actual y listado paginado sin efectos de escritura.
- [ ] T002 Definir los roles autorizados para detalle y listado, y `ROLE_ADMINISTRADOR` para resumen.
- [ ] T003 Preparar fixtures con los seis estados, estado ausente, galpón sin lote, lote con fecha futura y listado vacío.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: centralizar reglas de estados e integración de sólo lectura.

- [ ] T004 Crear `EstadoGalpon` con los seis estados permitidos y `ResumenGalpones` que cuenta válidos e inconsistentes.
- [ ] T005 Reutilizar `Edad` del Plan 003 para el cálculo inclusivo y reportar una fecha de ingreso futura sin fabricar una edad.
- [ ] T006 Definir los puertos de consulta y adaptadores del Módulo 1 sin métodos de actualización.
- [ ] T007 Validar paginación y orden explícito antes de invocar al puerto.

**Checkpoint**: la aplicación puede obtener datos vigentes sin escribir en ninguna dependencia.

---

## Phase 3: User Story 1 - Consultar detalle del galpón (Priority: P1)

**Goal**: mostrar nombre, aforo, estado y datos del lote actual cuando estén disponibles.

**Independent Test**: un lote ingresado hace 16 días muestra población y `2 semanas y 3 días (17 días)`; un galpón sin lote conserva sus datos y devuelve lote actual no disponible.

### Tests for User Story 1

- [ ] T008 [P] [US1] Probar detalle con lote, sin lote y fecha de ingreso inconsistente.
- [ ] T009 [P] [US1] Probar autorización y contrato HTTP del detalle.

### Implementation for User Story 1

- [ ] T010 [US1] Implementar `ConsultarGalponUseCase` y `GalponDetalleResult`.
- [ ] T011 [US1] Exponer `GET /api/galpones/{galponId}` mediante DTO y mapper REST.

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "nombre": "Galpón 1",
  "aforoMaximo": 8000,
  "estado": "PRODUCTIVO",
  "loteActual": {
    "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
    "poblacionActual": 7960,
    "edad": {
      "diasTotales": 17,
      "texto": "2 semanas y 3 días (17 días)"
    }
  }
}
```

---

## Phase 4: User Story 2 - Consultar resumen general (Priority: P2)

**Goal**: mostrar total, conteo por estado e inconsistencias sin modificar información.

**Independent Test**: con doce galpones válidos, la suma de los seis conteos es doce; los estados ausentes o inválidos se excluyen y se cuentan como inconsistentes.

### Tests for User Story 2

- [ ] T012 [P] [US2] Probar los seis estados, ceros e inconsistencias.
- [ ] T013 [P] [US2] Probar que el resumen no llama puertos de escritura.

### Implementation for User Story 2

- [ ] T014 [US2] Implementar `ConsultarResumenGalponesUseCase`.
- [ ] T015 [US2] Exponer `GET /api/galpones/resumen` sólo para administrador.

```json
{
  "totalRegistrado": 13,
  "totalValido": 12,
  "registrosInconsistentes": 1,
  "porEstado": {
    "DISPONIBLE": 2,
    "VACIADO_SANITARIO": 1,
    "PRODUCTIVO": 6,
    "EN_COSECHA": 1,
    "MANTENIMIENTO": 1,
    "AISLAMIENTO": 1
  }
}
```

---

## Phase 5: User Story 3 - Listar galpones (Priority: P2)

**Goal**: devolver todos los galpones accesibles en páginas estables y seleccionables para abrir el detalle.

**Independent Test**: un trabajador recibe doce elementos paginados con identificador, nombre y estado; sin elementos recibe una página vacía.

### Tests for User Story 3

- [ ] T016 [P] [US3] Probar listado vacío, paginación, orden y roles autorizados.

### Implementation for User Story 3

- [ ] T017 [US3] Implementar `ConsultarListadoGalponesUseCase`.
- [ ] T018 [US3] Exponer `GET /api/galpones?page=0&size=20&sort=nombre,asc`.

**Checkpoint**: detalle, resumen y listado son independientes y estrictamente de lectura.

---

## Endpoints

### Consultar detalle

| Método y ruta | Roles | Respuesta |
| --- | --- | --- |
| `GET /api/galpones/{galponId}` | `ROLE_ADMINISTRADOR`, `ROLE_TRABAJADOR`, roles autorizados | `200 OK` con detalle y lote actual opcional |

### Consultar resumen

| Método y ruta | Rol | Respuesta |
| --- | --- | --- |
| `GET /api/galpones/resumen` | `ROLE_ADMINISTRADOR` | `200 OK` con total, conteos por estado e inconsistencias |

### Listar galpones

| Método y ruta | Roles | Respuesta |
| --- | --- | --- |
| `GET /api/galpones?page=0&size=20&sort=nombre,asc` | `ROLE_ADMINISTRADOR`, `ROLE_TRABAJADOR`, roles autorizados | `200 OK` con página ordenada |

**Errores**: `400` UUID, paginación u orden inválidos; `401` no autenticado; `403` sin rol; `404` galpón inexistente; `503` Módulo 1 no disponible.

---

## Polish & Cross-Cutting Concerns

- [ ] T019 Mapear `400` parámetros inválidos, `401` no autenticado, `403` sin rol, `404` galpón inexistente y `503` Módulo 1 no disponible.
- [ ] T020 Documentar roles, paginación, orden y estados en OpenAPI.
- [ ] T021 Ejecutar pruebas y `git diff --check`.

---

## Dependencies & Execution Order

- **Setup** confirma fuente, roles y datos de prueba.
- **Foundational** crea reglas y puertos y bloquea las historias.
- **US1, US2 y US3** pueden avanzar en paralelo una vez terminada la base.
- **Polish** se realiza tras completar las historias deseadas.

## Notes

- La relación de lote se obtiene por `galponId`; no debe agregarse ni persistirse en este módulo.
- Los datos faltantes se comunican explícitamente; nunca se reemplazan con valores inventados o desactualizados.
