# Implementation Plan: Consultar edad por galpón

**Date**: 05/11/2026
**Spec**: [003-ConsultarEdadPorGalpon.md](../specs/003-ConsultarEdadPorGalpon.md)
**Architecture**: Arquitectura hexagonal (puertos y adaptadores)

## Summary

Implementar la consulta de la edad del lote alojado actualmente en un galpón. La operación es de solo lectura: recibe el identificador del galpón, obtiene su lote actual desde las interfaces públicas del Módulo 1 y calcula una edad inclusiva con la fecha de ingreso.

## Technical Context

**Language/Version**: Java 21
**Framework**: Spring Boot 3
**Persistence**: No aplica; la información de galpón y lote pertenece al Módulo 1
**Testing**: JUnit 5, Mockito y MockMvc
**Authorization**: `ROLE_ADMINISTRADOR`
**Time zone**: `America/Bogota`, inyectada mediante `Clock`
**External dependency**: interfaces públicas de consulta del Módulo 1

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 003-ConsultarEdadPorGalpon.md
└── plan/
    └── 003-ConsultarEdadPorGalpon.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/lote/
│   │   ├── Edad.java
│   │   └── Lote.java
│   ├── exception/galpon/
│   │   ├── FechaIngresoInconsistenteException.java
│   │   ├── GalponNoEncontradoException.java
│   │   ├── InformacionGalponNoDisponibleException.java
│   │   └── LoteActualInconsistenteException.java
│   └── port/out/modulo1/
│       ├── GalponQueryPort.java
│       └── LoteQueryPort.java
├── application/
│   └── galpon/
│       ├── ConsultarEdadPorGalponUseCase.java
│       └── result/EdadLoteResult.java
└── infrastructure/
    ├── adapter/in/rest/galpon/
    │   ├── GalponEdadController.java
    │   ├── dto/EdadLoteResponse.java
    │   └── mapper/GalponRestMapper.java
    ├── adapter/out/integration/modulo1/
    │   ├── GalponQueryAdapter.java
    │   └── LoteQueryAdapter.java
    └── config/GalponConsultaBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/lote/LoteTest.java
├── application/galpon/ConsultarEdadPorGalponUseCaseTest.java
└── infrastructure/adapter/in/rest/galpon/GalponEdadControllerTest.java
```

**Structure Decision**: el dominio conserva la regla de cálculo de edad. Los puertos de salida sólo representan las consultas públicas del Módulo 1; este módulo no replica tablas, entidades JPA ni repositorios de galpones o lotes.

## Phase 1: Setup

- [ ] T001 Verificar los contratos públicos disponibles en el Módulo 1 para consultar un galpón y su lote actual.
- [ ] T002 Confirmar que el rol `ROLE_ADMINISTRADOR` esté definido en la configuración de seguridad.
- [ ] T003 Definir datos de prueba: galpón con lote activo, galpón sin lote, galpón inexistente y lote con fecha futura.

**Checkpoint**: contratos externos, autorización y escenarios de prueba definidos antes de escribir la lógica.

## Phase 2: Foundational

- [ ] T004 Crear `Edad` como objeto de valor con días totales, semanas completas, días restantes y texto legible.
- [ ] T005 Implementar en `Lote` el cálculo inclusivo `calcularEdad(fechaConsulta)`; el día de ingreso equivale a un día de edad.
- [ ] T006 Rechazar una fecha de ingreso posterior a la fecha de consulta con `FechaIngresoInconsistenteException`.
- [ ] T007 Definir `GalponQueryPort` y `LoteQueryPort` sin tipos propios de infraestructura.
- [ ] T008 Implementar los adaptadores de integración del Módulo 1 y traducir indisponibilidad, ausencia e inconsistencia a las excepciones del dominio.
- [ ] T009 Registrar los adaptadores, el caso de uso y el `Clock` configurado para `America/Bogota` en `GalponConsultaBeanConfiguration`.

**Checkpoint**: el caso de uso puede consultar las dependencias mediante puertos y calcula la edad sin HTTP ni base de datos local.

## Phase 3: User Story 1 - Consultar la edad del lote actual (Priority: P1)

**User Story**: Como administrador, quiero consultar la edad del lote de un galpón para conocer su etapa de producción.

**Independent Test**: con un lote ingresado hace 16 días, la consulta devuelve 17 días totales, 2 semanas completas y 3 días restantes.

- [ ] T010 Implementar `ConsultarEdadPorGalponUseCase` para validar la existencia del galpón, obtener su lote actual y delegar el cálculo al dominio.
- [ ] T011 Retornar un resultado vacío, sin error, cuando el galpón exista pero no tenga lote alojado actualmente.
- [ ] T012 Crear `EdadLoteResult` como respuesta interna del caso de uso.
- [ ] T013 Exponer `GET /api/galpones/{galponId}/edad-lote` en `GalponEdadController`.
- [ ] T014 Crear `EdadLoteResponse` y `GalponRestMapper` para no exponer objetos de dominio.
- [ ] T015 Proteger el endpoint con `ROLE_ADMINISTRADOR`.
- [ ] T016 Implementar las pruebas de dominio, aplicación y controlador del escenario exitoso y del galpón sin lote.

## Endpoints

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `GET /api/galpones/{galponId}/edad-lote` | `ROLE_ADMINISTRADOR` | Sin cuerpo | `200 OK` con edad o mensaje de lote ausente |

**Errores**: `400` UUID inválido; `401` no autenticado; `403` sin rol; `404` galpón inexistente; `409` lote inconsistente; `503` Módulo 1 no disponible.

### Ejemplo de consulta

```http
GET /api/galpones/{galponId}/edad-lote
Authorization: Bearer <token>
```

### Response: galpón con lote actual

```json
{
  "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "loteId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "edad": {
    "diasTotales": 17,
    "semanasCompletas": 2,
    "diasRestantes": 3,
    "texto": "2 semanas y 3 días (17 días)"
  },
  "mensaje": null
}
```

### Response: galpón sin lote actual

```json
{
  "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "loteId": null,
  "edad": null,
  "mensaje": "El galpón no tiene lote alojado actualmente"
}
```

## Phase 4: Error handling and contract tests

- [ ] T017 Responder `400 Bad Request` si `galponId` no es un UUID válido.
- [ ] T018 Responder `401 Unauthorized` sin autenticación y `403 Forbidden` si el usuario no es administrador.
- [ ] T019 Responder `404 Not Found` si el galpón no existe.
- [ ] T020 Responder `409 Conflict` si el lote actual es inconsistente o su fecha de ingreso es futura.
- [ ] T021 Responder `503 Service Unavailable` si el Módulo 1 no puede proporcionar la información requerida.
- [ ] T022 Añadir pruebas MockMvc para cada código de respuesta y el formato JSON del contrato.

**Checkpoint**: el contrato HTTP devuelve respuestas estables y ningún detalle del Módulo 1 llega al cliente.

## Polish & Cross-Cutting Concerns

- [ ] T023 Aplicar nombres de paquetes y clases coherentes con los demás planes.
- [ ] T024 Documentar la semántica inclusiva y la zona horaria en Javadoc del cálculo.
- [ ] T025 Ejecutar pruebas unitarias y de controlador.
- [ ] T026 Ejecutar `git diff --check` antes de cerrar la implementación.

## Dependencies & Execution Order

1. Confirmar interfaces del Módulo 1 y seguridad.
2. Implementar el objeto de valor, la regla de dominio y los puertos.
3. Implementar adaptadores y configuración.
4. Implementar caso de uso, DTO, mapper y endpoint.
5. Completar pruebas de éxito, ausencia de lote, errores y autorización.

## Notes

- No se crea migración ni modelo de persistencia local para este caso de uso.
- La fecha de consulta procede del `Clock`; no se debe invocar `LocalDate.now()` directamente en el dominio o caso de uso.
- Si el contrato público del Módulo 1 difiere de estos puertos, se ajustan los adaptadores, no la regla de cálculo ni el endpoint público de este módulo.
