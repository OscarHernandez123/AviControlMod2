# Implementation Plan: Consulta y Seguimiento de Galpones

**Date**: 01/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [003-ConsultarEdadPorGalpon.md](../specs/003-ConsultarEdadPorGalpon.md)
- [007-ConsultarGalpon.md](../specs/007-ConsultarGalpon.md)

## Summary

Implementar la consulta de edad del lote, el detalle de un galpón, el resumen general del administrador y el listado general de galpones. Un trabajador autorizado puede listar y consultar cualquier galpón; no existe una relación de asignación entre trabajadores y galpones.

`Galpon` y `Lote` se modelan como entidades propias del dominio, con identidad y reglas, organizadas en paquetes de negocio. `Edad` es un objeto de valor; `EstadoGalpon` es un enum. No se utiliza el paquete `consulta` para definir entidades ni nombres como `GalponConsultado` o `LoteConsultado`.

Este plan implementa operaciones de lectura. Los specs 003 y 007 requieren obtener datos vigentes del módulo 1 sin modificar galpones o lotes. Definir entidades de dominio no agrega operaciones de creación, cambios de estado o descuentos de población a este feature.

La arquitectura es hexagonal dentro de un monolito modular con eventos internos. Los casos de uso consultan puertos específicos; los adaptadores invocan interfaces públicas de aplicación dentro del mismo proceso. No se requieren copias persistidas ni sincronización de proyecciones para estas consultas.

## Technical Context

**Performance Goals**: El 95 % de las consultas de edad, detalle y resumen general responde en máximo 1 segundo, con un volumen representativo acordado para la entrega.

**Constraints**: Lecturas sin escrituras, datos vigentes, ingreso contado como día 1, población y edad no negativas y autorización por rol. Las consultas no se filtran por trabajador.

**Scale/Scope**: Cuatro historias, cuatro endpoints GET, entidades de dominio, puertos de consulta y adaptadores internos.

**Dependencias funcionales**: Galpones y lotes proporcionados por el módulo 1.

### Decisiones específicas

1. **Entidades**: `Galpon` contiene `UUID`, nombre, aforo máximo y `EstadoGalpon`. `Galpon` no recibe ni almacena una colección de lotes. `Lote` contiene `UUID`, nombre, población inicial, población actual, fecha de ingreso, costo total y `galponId`. `Edad` es un dato derivado y no un campo persistido. Reutilizar las entidades existentes en esta capacidad si las hay; los endpoints de este plan solo exponen los atributos necesarios para cada consulta.
2. **Relación**: El lote referencia al galpón mediante `galponId`; el galpón no tiene una lista de lotes. La interfaz propietaria identifica cuál lote está actualmente alojado; no se elige automáticamente el de fecha más reciente. Cero lotes actuales es válido; más de uno es una inconsistencia.
3. **Edad**: `Lote.calcularEdad(fechaConsulta)` devuelve `Edad`, usando días calendario e incluyendo el día de ingreso. Aplicación obtiene la fecha mediante el `Clock` compartido y la zona de negocio. El formato textual se resuelve en presentación.
4. **Puertos**: `GalponQueryPort` y `LoteQueryPort` expresan todas las consultas requeridas. No se agrega `Modulo1QueryPort`, porque duplicaría el acceso a los mismos recursos.
5. **Adaptadores internos**: Traducen los resultados de interfaces públicas de otras capacidades a los modelos requeridos aquí. No acceden a repositorios JPA privados ajenos ni llaman por HTTP al mismo monolito. Una entidad de dominio propia no implica una segunda tabla ni una segunda fuente de datos.
6. **Eventos**: Los procesos que cambian datos publican sus eventos internos. Este feature no publica eventos por leer ni consume mortalidades para descontar aves. Al consultar el estado vigente sin caché propia, no necesita listeners para copiar cambios. La integración debe comprobar que una consulta posterior a un proceso confirmado refleja su resultado.
7. **Persistencia**: No se crean tablas, migraciones, repositorios JPA ni entidades de proyección en este plan. Tampoco topics Kafka, mensajes versionados de integración o solicitudes de descuento. La persistencia permanece en la capacidad que gestiona cada entidad.
8. **Consistencia**: Obtener los datos relacionados mediante una lectura transaccional coherente, usando las interfaces internas participantes y la configuración del monolito. Verificar ante concurrencia; una transacción marcada como solo lectura no garantiza por sí sola una instantánea coherente. Consultar por conjuntos para evitar una llamada por galpón.

### Alcance documental

- El spec 019 corresponde al plan de mortalidad y actualización del inventario vivo. Aquí se muestra la población vigente sin descontarla.
- La historia 3 del spec 007 permite al trabajador listar y seleccionar cualquier galpón. No existen modelos, puertos, filtros ni reglas de asignación entre trabajadores y galpones.
- General.md y este plan coinciden en que no existen asignaciones entre trabajadores y galpones. La arquitectura monolítica indicada para esta revisión se aplica mediante interfaces internas.
- El resumen de galpones y aves vivas corresponde a este plan. El resumen de alimentos y medicamentos en bodega permanece en el plan 002 y el spec 023.
- Los nombres concretos de las interfaces ofrecidas por otras capacidades se acuerdan antes de implementar sus adaptadores; los puertos de este documento pertenecen al consumidor.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   ├── 003-ConsultarEdadPorGalpon.md
│   └── 007-ConsultarGalpon.md
└── plan/
    └── 001-ConsultaYSeguimientoDeGalpones.md
```

### Source Code (repository root)

Estructura objetivo de los componentes del feature. Reutilizar actor autenticado, reloj y manejo de errores existentes. Alinear el paquete raíz con el configurado en la aplicación al implementar.

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/
│   │   ├── galpon/
│   │   │   ├── Galpon.java
│   │   │   └── EstadoGalpon.java
│   │   └── lote/
│   │       ├── Lote.java
│   │       └── Edad.java
│   ├── exception/galpon/
│   │   ├── GalponNoEncontradoException.java
│   │   ├── FechaIngresoInconsistenteException.java
│   │   ├── LoteActualInconsistenteException.java
│   │   └── InformacionGalponNoDisponibleException.java
│   └── port/out/galpon/
│       ├── GalponQueryPort.java
│       └── LoteQueryPort.java
├── application/galpon/
│   ├── ConsultarEdadPorGalponUseCase.java
│   ├── ConsultarGalponUseCase.java
│   ├── ConsultarResumenGalponesUseCase.java
│   ├── ConsultarListadoGalponesUseCase.java
│   └── result/
│       ├── EdadLoteResult.java
│       ├── GalponDetalleResult.java
│       ├── ResumenGalponesResult.java
│       ├── GalponListadoResult.java
│       └── ListadoGalponesResult.java
└── infrastructure/
    ├── adapter/in/rest/galpon/
    │   ├── GalponController.java
    │   ├── dto/
    │   │   ├── EdadLoteResponse.java
    │   │   ├── GalponDetalleResponse.java
    │   │   ├── ResumenGalponesResponse.java
    │   │   ├── GalponListadoResponse.java
    │   │   └── ListadoGalponesResponse.java
    │   └── mapper/GalponRestMapper.java
    ├── adapter/out/internal/galpon/
    │   ├── GalponQueryAdapter.java
    │   ├── LoteQueryAdapter.java
    │   └── mapper/GalponLoteMapper.java
    └── config/
        └── GalponBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/
│   ├── galpon/GalponTest.java
│   └── lote/LoteTest.java
├── application/galpon/
│   ├── ConsultarEdadPorGalponUseCaseTest.java
│   ├── ConsultarGalponUseCaseTest.java
│   ├── ConsultarResumenGalponesUseCaseTest.java
│   └── ConsultarListadoGalponesUseCaseTest.java
└── infrastructure/
    ├── adapter/in/rest/GalponControllerTest.java
    ├── adapter/out/internal/GalponLoteQueryAdaptersTest.java
    └── integration/ConsultaGalponesIntegrationTest.java
```

**Structure Decision**: Entidades organizadas por negocio; casos de uso como entradas públicas de aplicación; puertos de salida implementados por adaptadores internos. Los resultados de aplicación y los DTOs HTTP permanecen separados del dominio. No se agregan listeners sin un efecto propio que deban ejecutar.

### Entidades y relación

`Galpon` y `Lote` son entidades distintas, cada una con su propio UUID. La relación se expresa únicamente en `Lote.galponId`; `Galpon` no contiene una colección ni una referencia directa a lotes. Esto permite conservar el historial de lotes sin convertir al galpón en el propietario de esos registros.

Estas clases representan el modelo de dominio que usa el Módulo 2 para consultar y aplicar sus reglas de lectura; la propiedad autoritativa de los datos y sus operaciones de escritura continúa en el Módulo 1. Por eso este plan no crea tablas ni métodos de alta, edición o eliminación para estas entidades.

```text
Galpon
├── id: UUID
├── nombre: String
├── aforoMaximo: Integer
└── estado: EstadoGalpon

Lote
├── id: UUID
├── nombre: String
├── poblacionInicial: Integer
├── poblacionActual: Integer
├── fechaIngreso: LocalDate
├── costoTotal: BigDecimal
└── galponId: UUID

Edad = datos derivados de Lote.fechaIngreso y la fecha de consulta
```

`EstadoGalpon` debe representar exactamente `DISPONIBLE`, `VACIADO_SANITARIO`, `PRODUCTIVO`, `EN_COSECHA`, `MANTENIMIENTO` y `AISLAMIENTO`. Sus valores de presentación pueden usar espacios y minúsculas, por ejemplo `vaciado sanitario` y `productivo`.

`poblacionInicial` es inmutable; `poblacionActual` puede cambiar mediante los procesos propietarios de mortalidad o inventario vivo. `costoTotal` se conserva en el modelo del lote para mantener la definición de la entidad, aunque no se calcula ni se expone en las respuestas de este plan.

### Contratos de los puertos

| Puerto | Responsabilidad |
| --- | --- |
| `GalponQueryPort` | Buscar un galpón, listar todos los galpones con límite, desplazamiento y orden, contar el total y consultar los totales por `EstadoGalpon`, además de los registros inconsistentes. |
| `LoteQueryPort` | Obtener el lote actualmente alojado por galpón, individualmente o por conjunto. Distinguir ausencia, datos incompletos y múltiples lotes actuales. |

El resumen no requiere otra clase de dominio: `GalponQueryPort` entrega los conteos necesarios y `ConsultarResumenGalponesUseCase` compone `ResumenGalponesResult` en la capa de aplicación. El resultado distingue total registrado, total válido y registros inconsistentes. Los estados inválidos se contabilizan antes de construir entidades válidas; no se convierten en un estado de galpón inventado.

### Contratos HTTP propuestos

| Endpoint | Acceso | Resultado |
| --- | --- | --- |
| `GET /api/galpones/{galponId}/edad-lote` | Administrador | Días totales, semanas completas, días restantes y texto de edad. |
| `GET /api/galpones/{galponId}` | Administrador, trabajador o usuario autorizado | Nombre, aforo, estado y datos disponibles del lote actual. |
| `GET /api/galpones/resumen` | Administrador | Total registrado, total válido, seis conteos e inconsistencias. |
| `GET /api/galpones` | Administrador, trabajador o usuario autorizado | Página del listado general con ID, nombre y estado de cada galpón. |

Una consulta válida responde 200 incluso sin lote o con un listado vacío. Un galpón inexistente responde 404; múltiples lotes actuales o una fecha futura en la consulta exclusiva de edad responden 409; una dependencia imprescindible no disponible responde 503. Autenticación y autorización usan los errores de General.md.

En el detalle, una fecha futura conserva los demás datos válidos, omite la edad e informa la inconsistencia. Los resultados parciales incluyen incidencias explícitas y no sustituyen valores faltantes por cero.

### JSON común de errores

Todos los endpoints utilizan `Content-Type: application/problem+json` y la estructura definida en General.md. Ejemplo para un galpón inexistente:

```json
{
  "type": "https://avicontrol/errors/galpon-no-encontrado",
  "title": "Galpón no encontrado",
  "status": 404,
  "detail": "No existe el galpón solicitado",
  "instance": "/api/galpones/550e8400-e29b-41d4-a716-446655440000",
  "code": "GALPON_NO_ENCONTRADO",
  "correlationId": "c9a13035-a18a-4fbd-afb3-a3bfcd15f088",
  "fieldErrors": []
}
```

`status` coincide con el código HTTP; `code` es estable para clientes; `detail` puede aportar contexto sin exponer información sensible; `correlationId` permite rastrear la solicitud. Los errores de validación pueden incluir elementos en `fieldErrors` con `field`, `code` y `message`.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Preparar estructura y contratos propios de la capacidad.

- [ ] T001 Contrastar las interfaces internas disponibles de galpones y lotes con los dos puertos definidos; documentar datos faltantes y responsable.
- [ ] T002 Crear los paquetes del feature y comprobar el paquete raíz real; reutilizar configuración transversal de actor, reloj y errores.
- [ ] T003 Acordar las autoridades de seguridad para administrador, usuario y trabajador / operario.
- [ ] T004 Preparar fixtures de galpones vacíos, listados paginados, lotes actuales e históricos y estados inconsistentes.

**Checkpoint**: Contratos definidos sin repetir la base técnica ni introducir infraestructura distribuida.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Preparar entidades y accesos compartidos por las historias.

- [ ] T005 Implementar `Galpon`, `EstadoGalpon`, `Lote` y `Edad` en Java puro; incluir en `Galpon` UUID, nombre, aforo máximo y estado, y en `Lote` UUID, nombre, población inicial, población actual, fecha de ingreso, costo total y `galponId`. No agregar una colección de lotes dentro de `Galpon`.
- [ ] T006 Implementar `Lote.calcularEdad(fechaConsulta)` y validaciones de identidad, población inicial, población actual, fecha de ingreso y costo total. Detectar una fecha futura al calcular la edad sin perder los demás datos del detalle.
- [ ] T007 Definir excepciones y los dos puertos con operaciones individuales, por conjunto, paginadas y con semántica de ausencia e inconsistencia. El listado y el resumen se representan mediante resultados de aplicación.
- [ ] T008 Implementar `GalponQueryAdapter`, `LoteQueryAdapter` y `GalponLoteMapper` sobre interfaces públicas internas. No seleccionar el lote actual únicamente por fecha de ingreso.
- [ ] T009 Implementar el agregado: `totalRegistrado = totalValido + inconsistentes`; la suma de los seis estados debe igualar `totalValido`.
- [ ] T010 Registrar dependencias en `GalponBeanConfiguration` y aplicar desde infraestructura el contexto transaccional de lectura a los casos de uso.
- [ ] T011 Verificar contratos de adaptadores, ausencia de escrituras y consistencia de lectura ante cambios concurrentes.

**Checkpoint**: Datos vigentes y dominio preparados sin proyecciones duplicadas.

## Phase 3: User Story 1 — Consultar Edad del Lote por Galpón (Priority: P1)

**Spec**: 003, historia 1.

**Goal**: El administrador obtiene la edad exacta del lote actual.

**Independent Test**: Un ingreso hace 16 días muestra `2 semanas y 3 días (17 días)`; el mismo día de ingreso muestra `1 día`.

### Definición del evento para User Story 1

**Evento producido**: Ninguno.

**Evento consumido**: Ninguno.

La consulta calcula la edad a partir de la fecha vigente del lote y no representa un cambio de estado del negocio. Por esa razón no se define un evento como `EdadLoteConsultada`. Los eventos que creen, alojen o actualicen un lote pertenecen a la capacidad propietaria de lotes; una vez confirmado uno de esos procesos, esta consulta debe reflejar la nueva fecha o asociación mediante `LoteQueryPort`, sin agregar un listener en este feature.

### Definición del endpoint REST para User Story 1

| Elemento | Definición |
| --- | --- |
| Método y ruta | `GET /api/galpones/{galponId}/edad-lote` |
| Autorización | `ROLE_ADMINISTRADOR` |
| Entrada | `galponId`, UUID obligatorio en la ruta. No recibe body. |
| Respuesta 200 con lote | `galponId`, `loteId` y `edad` con `diasTotales`, `semanasCompletas`, `diasRestantes` y `texto`. |
| Respuesta 200 sin lote | `galponId`, `loteId: null`, `edad: null` y `mensaje: "El galpón no tiene lote alojado actualmente"`. |
| Errores | 400 para UUID inválido, 401 sin autenticación, 403 sin rol, 404 si no existe el galpón, 409 para fecha futura o múltiples lotes actuales y 503 si la información imprescindible no está disponible. |

Los errores utilizan `application/problem+json`. Los valores de edad son derivados y no se aceptan como parámetros del cliente.

#### JSON de respuesta con lote actual

```json
{
  "galponId": "550e8400-e29b-41d4-a716-446655440000",
  "loteId": "2fc03a21-84c4-4c83-bb15-6607bb75cb90",
  "edad": {
    "diasTotales": 17,
    "semanasCompletas": 2,
    "diasRestantes": 3,
    "texto": "2 semanas y 3 días (17 días)"
  },
  "mensaje": null
}
```

#### JSON de respuesta sin lote actual

```json
{
  "galponId": "550e8400-e29b-41d4-a716-446655440000",
  "loteId": null,
  "edad": null,
  "mensaje": "El galpón no tiene lote alojado actualmente"
}
```

### Tests para User Story 1

- [ ] T012 [US1] Probar en `LoteTest` día de ingreso, 16 días transcurridos, cambios de mes y año, año bisiesto y fecha futura.
- [ ] T013 [US1] Probar en `ConsultarEdadPorGalponUseCaseTest` galpón inexistente, ausencia de lote, múltiples lotes actuales y datos no disponibles.
- [ ] T014 [US1] Probar en `GalponControllerTest` autorización, campos numéricos, singular/plural y mensaje exacto `El galpón no tiene lote alojado actualmente`.

### Implementación de User Story 1

- [ ] T015 [US1] Implementar `ConsultarEdadPorGalponUseCase` con puertos de galpón y lote, actor y `Clock`; delegar cálculo a `Lote`.
- [ ] T016 [US1] Crear `EdadLoteResult`, `EdadLoteResponse` y mapeo, diferenciando ausencia de lote de edad disponible.
- [ ] T017 [US1] Implementar endpoint de edad con respuestas 200, 404, 409 y 503 y controles de seguridad.

**Checkpoint**: Edad correcta sin persistirla ni modificar entidades.

## Phase 4: User Story 2 — Consultar Información de un Galpón (Priority: P1)

**Spec**: 007, historia 1.

**Goal**: Administrador o usuario consulta nombre, aforo, estado, población y edad.

**Independent Test**: Sin lote se conservan los datos del galpón y se explica la ausencia de población y edad.

### Definición del evento para User Story 2

**Evento producido**: Ninguno.

**Evento consumido**: Ninguno.

Consultar un galpón no modifica su estado ni el del lote. Los eventos de creación o actualización de galpón, alojamiento de lote y actualización de población son responsabilidad de los casos de uso que realizan esos cambios. Después de su confirmación, `GalponQueryPort` y `LoteQueryPort` deben devolver el estado vigente. Esta historia verifica esa visibilidad, pero no duplica dichos eventos ni mantiene una proyección propia.

### Definición del endpoint REST para User Story 2

| Elemento | Definición |
| --- | --- |
| Método y ruta | `GET /api/galpones/{galponId}` |
| Autorización | `ROLE_ADMINISTRADOR`, `ROLE_TRABAJADOR` o `ROLE_USUARIO` |
| Entrada | `galponId`, UUID obligatorio en la ruta. No recibe body. |
| Respuesta 200 | `id`, `nombre`, `aforoMaximo`, `estado` y `loteActual`. Cuando existe, `loteActual` contiene `id`, `poblacionActual`, `fechaIngreso` y `edad`; cuando no existe es `null` y se incluye la incidencia correspondiente. |
| Datos parciales | `incidencias` identifica fecha futura o información no disponible sin reemplazarla por valores inventados. |
| Errores | 400 para UUID inválido, 401 sin autenticación, 403 sin rol, 404 si no existe el galpón, 409 para múltiples lotes actuales y 503 cuando no se puede recuperar información imprescindible. |

El endpoint no expone entidades JPA ni acepta atributos editables porque su contrato es estrictamente de lectura.

#### JSON de respuesta con lote actual

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "nombre": "Galpón 1",
  "aforoMaximo": 8000,
  "estado": "PRODUCTIVO",
  "loteActual": {
    "id": "2fc03a21-84c4-4c83-bb15-6607bb75cb90",
    "poblacionActual": 7960,
    "fechaIngreso": "2026-09-15",
    "edad": {
      "diasTotales": 17,
      "semanasCompletas": 2,
      "diasRestantes": 3,
      "texto": "2 semanas y 3 días (17 días)"
    }
  },
  "incidencias": []
}
```

#### JSON de respuesta sin lote actual

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "nombre": "Galpón 2",
  "aforoMaximo": 6000,
  "estado": "DISPONIBLE",
  "loteActual": null,
  "incidencias": [
    {
      "codigo": "GALPON_SIN_LOTE_ACTUAL",
      "mensaje": "El galpón no tiene lote alojado actualmente"
    }
  ]
}
```

### Tests para User Story 2

- [ ] T018 [US2] Probar en `ConsultarGalponUseCaseTest` detalle completo, ausencia de lote, fecha futura e información incompleta sin inventar valores.
- [ ] T019 [US2] Probar en `GalponControllerTest` acceso de administrador y usuario, rechazo de otros roles y errores por inexistencia o inconsistencia.
- [ ] T020 [US2] Probar en `GalponLoteQueryAdaptersTest` selección de la asociación actualmente alojada y rechazo de múltiples lotes actuales.

### Implementación de User Story 2

- [ ] T021 [US2] Implementar `ConsultarGalponUseCase` reutilizando la regla de edad del dominio, sin invocar el caso de uso restringido al administrador.
- [ ] T022 [US2] Crear `GalponDetalleResult`, `GalponDetalleResponse` e incidencias de datos incompletos o inconsistentes.
- [ ] T023 [US2] Implementar endpoint de detalle y autorización en controlador y aplicación.

**Checkpoint**: Detalle válido con faltantes informados explícitamente.

## Phase 5: User Story 3 — Consultar Resumen General de Galpones (Priority: P2)

**Spec**: 007, historia 2.

**Goal**: El administrador obtiene totales y distribución por estado.

**Independent Test**: Doce galpones válidos producen total válido 12 y conteos que suman 12; los registros inválidos se informan por separado.

### Definición del evento para User Story 3

**Evento producido**: Ninguno.

**Evento consumido**: Ninguno.

El resumen es una agregación calculada durante la consulta y no constituye un hecho nuevo del dominio. Los cambios de estado de un galpón son publicados por el caso de uso que los ejecuta. Una consulta posterior debe incorporarlos a los conteos mediante `GalponQueryPort`; no se publica un evento `ResumenGalponesConsultado` ni se conserva el resumen como estado persistido.

### Definición del endpoint REST para User Story 3

| Elemento | Definición |
| --- | --- |
| Método y ruta | `GET /api/galpones/resumen` |
| Autorización | `ROLE_ADMINISTRADOR` |
| Entrada | Sin parámetros y sin body. |
| Respuesta 200 | `totalRegistrado`, `totalValido`, `registrosInconsistentes` y `porEstado`, con claves para `DISPONIBLE`, `VACIADO_SANITARIO`, `PRODUCTIVO`, `EN_COSECHA`, `MANTENIMIENTO` y `AISLAMIENTO`, incluso cuando su valor sea cero. |
| Invariantes | `totalRegistrado = totalValido + registrosInconsistentes` y la suma de `porEstado` es igual a `totalValido`. |
| Errores | 401 sin autenticación, 403 sin rol, 409 si los conteos recuperados son incoherentes y 503 si la fuente de galpones no está disponible. |

La ruta fija `/resumen` debe declararse sin ambigüedad frente a `/{galponId}` en el controlador.

#### JSON de respuesta

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

Cuando no existen galpones, los tres totales y los seis conteos se devuelven en cero; no se omiten claves del objeto `porEstado`.

### Tests para User Story 3

- [ ] T024 [US3] Probar en `ConsultarResumenGalponesUseCaseTest` conteo único, conjunto vacío y estados ausentes o desconocidos.
- [ ] T025 [US3] Probar en `GalponControllerTest` seis estados incluso en cero, ecuaciones de totales y acceso exclusivo del administrador.

### Implementación de User Story 3

- [ ] T026 [US3] Implementar `ConsultarResumenGalponesUseCase` sobre los conteos entregados por `GalponQueryPort`, componer `ResumenGalponesResult` y validar la coherencia de sus totales.
- [ ] T027 [US3] Crear `ResumenGalponesResult`, `ResumenGalponesResponse` y mapeo.
- [ ] T028 [US3] Implementar endpoint de resumen sin movimientos ni publicaciones de eventos.

**Checkpoint**: Resumen coherente sin alterar estados.

## Phase 6: User Story 4 — Listar Galpones Disponibles para Consulta (Priority: P2)

**Spec**: 007, historia 3.

**Goal**: El trabajador visualiza el listado general y puede seleccionar cualquier galpón para consultar su detalle.

**Independent Test**: Con doce galpones registrados, un trabajador autenticado obtiene los doce mediante las páginas correspondientes y puede abrir cualquiera de ellos. La consulta no requiere ni aplica asignaciones.

### Definición del evento para User Story 4

**Evento producido**: Ninguno.

**Evento consumido**: Ninguno.

El listado es una consulta del estado vigente y no representa un nuevo hecho del dominio. Los casos de uso que crean o actualizan galpones publican sus propios eventos internos; después de confirmarse, el listado debe reflejar esos cambios mediante `GalponQueryPort`. Este feature no crea listeners ni copias locales.

### Definición del endpoint REST para User Story 4

| Elemento | Definición |
| --- | --- |
| Método y ruta | `GET /api/galpones` |
| Autorización | `ROLE_ADMINISTRADOR`, `ROLE_TRABAJADOR` o `ROLE_USUARIO` |
| Entrada | Query params opcionales `page` (por defecto 0), `size` (por defecto 20, máximo 100) y `sort` (por defecto `nombre,asc`). No recibe body ni `trabajadorId`. |
| Respuesta 200 | `content` con `id`, `nombre` y `estado` de cada galpón; además `page`, `size`, `totalElements`, `totalPages`, `first` y `last`. |
| Listado vacío | Respuesta 200 con `content: []`, totales en cero y `first: true`, `last: true`. |
| Errores | 400 para paginación u orden inválidos, 401 sin autenticación, 403 sin rol y 503 si la fuente de galpones no está disponible. |

El endpoint devuelve el mismo universo de galpones para todos los roles autorizados. Al seleccionar un elemento, el cliente utiliza `GET /api/galpones/{galponId}` para consultar el detalle definido en User Story 2.

#### JSON de respuesta con galpones

```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "nombre": "Galpón 1",
      "estado": "PRODUCTIVO"
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "nombre": "Galpón 2",
      "estado": "DISPONIBLE"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

#### JSON de respuesta sin galpones

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true,
  "mensaje": "No existen galpones disponibles"
}
```

### Tests para User Story 4

- [ ] T029 [US4] Probar en `ConsultarListadoGalponesUseCaseTest` listado completo a través de sus páginas, página vacía y metadatos correctos.
- [ ] T030 [US4] Probar que administrador, trabajador y usuario autorizado reciben el mismo universo de galpones, sin filtros por identidad o asignación.
- [ ] T031 [US4] Probar límites de `page` y `size`, tamaño máximo y criterios de orden permitidos.
- [ ] T032 [US4] Probar en `GalponLoteQueryAdaptersTest` orden estable y ausencia de una consulta adicional por cada elemento listado.
- [ ] T033 [US4] Probar en `GalponControllerTest` el contrato de FR-014 a FR-018, el listado vacío y la selección posterior del detalle.

### Implementación de User Story 4

- [ ] T034 [US4] Extender `GalponQueryPort` y `GalponQueryAdapter` con listado mediante límite, desplazamiento y orden permitido, más consulta del total de elementos.
- [ ] T035 [US4] Implementar `ConsultarListadoGalponesUseCase` validando paginación, tamaño máximo, orden y autorización por rol, sin aplicar filtros por trabajador.
- [ ] T036 [US4] Crear `GalponListadoResult`, `ListadoGalponesResult`, `GalponListadoResponse` y `ListadoGalponesResponse` con metadatos de paginación.
- [ ] T037 [US4] Incorporar el mapeo del listado en `GalponRestMapper` sin exponer entidades JPA.
- [ ] T038 [US4] Implementar `GET /api/galpones` y asegurar que las rutas fijas `/resumen` y `/{galponId}/edad-lote` no sean absorbidas por `/{galponId}`.

**Checkpoint**: Cualquier trabajador autorizado puede listar y consultar cualquier galpón sin modelos ni reglas de asignación.

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Verificar contratos e integración dentro del monolito.

- [ ] T039 Documentar los cuatro endpoints, roles, datos parciales y errores en OpenAPI.
- [ ] T040 Completar `ConsultaGalponesIntegrationTest`: ejecutar cambios desde los procesos propietarios, incluyendo población confirmada mediante el flujo de eventos interno, y verificar que una nueva consulta refleja el resultado sin descontarlo otra vez.
- [ ] T041 Verificar ausencia de escrituras y publicaciones durante consultas, y que los adaptadores no accedan a repositorios privados ajenos.
- [ ] T042 Extender comprobaciones arquitectónicas existentes: dominio sin frameworks, aplicación sin JPA/HTTP y dependencias internas sin ciclos.
- [ ] T043 Ejecutar pruebas y tareas de calidad disponibles. El repositorio inspeccionado contiene `pom.xml` y `mvnw.cmd`, mientras General.md declara Gradle: resolver esta diferencia antes de fijar el comando de entrega, sin migrar el sistema de construcción dentro de este plan funcional.
- [ ] T044 Medir el objetivo del 95 % en máximo 1 segundo y verificar consultas por conjuntos sin una llamada interna por cada galpón.
- [ ] T045 Verificar criterios de presentación y usabilidad de los specs en la interfaz consumidora cuando esté disponible; las pruebas de API no acreditan por sí solas tiempos de interacción ni satisfacción.

**Checkpoint**: Historias verificadas, dependencias reales integradas y diferencias documentales identificadas.

## Dependencies & Execution Order

### Phase Dependencies

- **Setup**: Identificación de interfaces proveedoras y base existente.
- **Foundational**: Depende de Setup y habilita las historias.
- **US1, US2 y US3**: Dependen de Foundational; reutilizan la regla de edad del dominio cuando corresponde. No dependen de invocar otro caso de uso con permisos diferentes.
- **US4**: Depende de Foundational y del listado paginado ofrecido por `GalponQueryPort`; no depende de información del trabajador más allá de su rol autorizado.
- **Polish**: Depende de las cuatro historias y de los procesos internos que permitan verificar lecturas posteriores a cambios.

### Dependencias con otros planes

- **Mortalidad e inventario vivo**: Cambios de población que se muestran en las consultas. El spec 019 permanece en ese plan.
- **Gestión de galpones y lotes**: Interfaces públicas y asociación actualmente alojada.

### Dentro de cada User Story

- Entidades y puertos antes que casos de uso; casos de uso antes que controladores.
- Mapeos en los adaptadores; reglas de negocio en dominio.
- Pruebas junto a implementación y checkpoint antes de cerrar la historia.
- Coordinar cambios en controlador, mappers y pruebas compartidas; las historias no son automáticamente tareas paralelas sobre esos archivos.

## Notes

- T001 a T045 identifican tareas; US1 a US4 identifican las historias.
- Este documento describe componentes por implementar, no afirma que ya existan.
- Usar eventos en un monolito no exige un listener en cada feature. Aquí no existe una reacción persistente propia que lo justifique.
- `Galpon` y `Lote` son entidades del dominio; el límite de escritura de estas consultas sigue siendo el de los specs 003 y 007.
- El plan no reproduce el stack de General.md; explicita decisiones y diferencias necesarias para este alcance.
