# Implementation Plan: Registro de Consumo de Medicamentos

**Created**: 25/10/2026

**Last Updated**: 29/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [013-RegistrarConsumoMedicamento.md](../specs/013-RegistrarConsumoMedicamento.md)

## Summary

Registrar la cantidad de medicamento realmente aplicada en campo a partir de un diagnóstico clínico con medicación prescrita, descontar las existencias físicas de forma atómica en orden FEFO mediante el [Plan 010](010-GestionDelInventario.md), conservar el detalle desglosado por recepción con su precio neto histórico de compra (proveniente del [Plan 002](002-GestionDeRecepciones.md)) y publicar de forma confiable el evento de integración hacia Apache Kafka para el costeo real del lote en el Módulo 3 (Finanzas). El diagnóstico determina automáticamente el medicamento, el lote y el galpón; el cliente no puede sustituirlos manualmente.

## Technical Context

**Performance Goals**: El 95 % de los consumos confirmados (incluyendo validación diagnóstica, normalización dimensional, descuento FEFO en inventario y registro en Outbox) responde y queda disponible para el Módulo 3 en máximo 2 segundos. El 95 % de las consultas de consumos por diagnóstico responde en máximo 1 segundo.

**Constraints**:

- Acceso exclusivo: solo `ROLE_VETERINARIO` registra consumos de medicamentos (`403 Forbidden` a otros roles). La consulta de consumos por diagnóstico (`GET`) está permitida a `ROLE_VETERINARIO` y `ROLE_ADMINISTRADOR`.
- Derivación obligatoria desde el diagnóstico: `galponId`, `loteId`, `medicacionId` y `medicamentoId` se obtienen de forma inmutable desde el diagnóstico confirmado en el [Plan 015](015-DiagnosticoYReintegroSanitario.md); no se aceptan como campos editables en el request. Un diagnóstico de enfermedad mortal (sin medicación asociada) rechaza el registro con `409 Conflict`.
- Unidades y normalización estricta: se admiten únicamente `MILIGRAMO`, `GRAMO`, `MILILITRO` y `UNIDAD`. Las unidades de masa se normalizan a `GRAMO` (`1.000 mg = 1 g`), volumen a `MILILITRO` y conteo a `UNIDAD` utilizando `BigDecimal`. No se convierten dimensiones incompatibles (`400`/`409`) ni se aceptan cantidades menores o iguales a cero.
- Atomicidad, FEFO y Cero Descuentos Parciales: el descuento de existencias se ejecuta mediante el puerto del [Plan 010](010-GestionDelInventario.md) en la misma transacción local (`@Transactional`), priorizando recepciones vigentes no vencidas ni anuladas con fecha de vencimiento más próxima (FEFO) y bloqueo de concurrencia. Si la suma de existencias disponibles no cubre el 100 % de la cantidad normalizada, se revierte toda la operación (_rollback_) sin guardar el consumo ni aplicar descuentos parciales (`409 Conflict`).
- Trazabilidad histórica sin promedios: cada porción descontada genera un `DetalleConsumo` independiente que conserva el `recepcionId` y su `precioNetoHistoricoUnidadBase` original sin promediar precios entre recepciones distintas. Este plan no ejecuta la liquidación contable final del lote (responsabilidad exclusiva del Módulo 3).
- Idempotencia, concurrencia e inmutabilidad: el endpoint `POST` exige la cabecera `X-Idempotency-Key`, la entidad aplica `@Version` y los consumos confirmados son registros históricos inmutables donde queda prohibido el borrado físico (`DELETE`).

**Scale/Scope**: SPEC-013, una historia principal de escritura transaccional más consulta de historial por diagnóstico para UI; dos endpoints REST (`POST` y `GET`); un agregado raíz (`ConsumoMedicamento`) con su colección de entidades hijas (`DetalleConsumo`); integración síncrona en memoria con Planes 015 y 010/002, y publicación asíncrona vía Transactional Outbox hacia Apache Kafka para el Módulo 3.

**Dependencias funcionales**:

- [Plan 015](015-DiagnosticoYReintegroSanitario.md): provee el diagnóstico activo y el snapshot de la medicación prescrita (`galponId`, `loteId`, `medicamentoId`).
- [Plan 010](010-GestionDelInventario.md): es propietario de las unidades, saldos y movimientos de inventario; ejecuta la validación de unidad base y el descuento atómico FEFO por recepción.
- [Plan 002](002-GestionDeRecepciones.md): es propietario de las recepciones de medicamentos y de sus precios netos históricos por unidad base que el Plan 010 vincula en cada descuento.
- [Plan 006](006-GestionDeMedicaciones.md): propietario del catálogo de medicaciones y consultas diarias de solo lectura.
- **Módulo 3 (Finanzas)**: consumidor del evento Kafka con el desglose de cantidades y precios históricos por recepción para calcular el costo real del lote.
- [General.md](General.md): seguridad, actor autenticado, `Clock` (`America/Bogota`), idempotencia, formato de errores RFC 9457 y Transactional Outbox.

### Decisiones específicas

1. **Asociación Inmutable al Diagnóstico**: El consumo se registra sobre un `diagnosticoId` existente que posea medicación prescrita (`requiereSacrificioHistorico == false`). El caso de uso consulta `DiagnosticoQueryPort` para extraer `galponId`, `loteId` y `medicamentoId` del snapshot del diagnóstico.
2. **Normalización Dimensional Centralizada**: Antes de descontar, se valida que la `unidadIngresada` (`MILIGRAMO`, `GRAMO`, `MILILITRO`, `UNIDAD`) pertenezca a la misma dimensión física que la `unidadBase` del medicamento en el inventario (`GRAMO`, `MILILITRO`, `UNIDAD`). Si la unidad ingresada es `MILIGRAMO` y la base es `GRAMO`, se divide entre `1000` con `BigDecimal` (escala de 4 decimales y redondeo `HALF_UP`). Si las dimensiones son incompatibles (ej. `GRAMO` para un producto en `MILILITRO` o `UNIDAD`), se rechaza inmediatamente.
3. **Distribución FEFO y Bloqueo Concurrente en Plan 010**: A través de `InventarioMedicamentoPort`, se solicita al Plan 010 descontar la `cantidadNormalizada` de las recepciones confirmadas, no vencidas a la fecha actual y no anuladas del mismo `medicamentoId`, ordenadas por fecha de vencimiento ascendente (FEFO). El puerto retorna la lista de porciones descontadas con su `recepcionId`, `codigoLoteRecepcion`, `cantidadDescontadaUnidadBase`, `precioNetoHistoricoUnidadBase` y `moneda`.
4. **Invariante de Integridad Financiera**: La suma exacta de `cantidadDescontadaUnidadBase` de todos los `DetalleConsumo` debe ser idéntica a `cantidadNormalizada`. Nunca se promedian precios entre recepciones con tarifas diferentes.
5. **Ausencia de Sobre-Ingeniería Interna (YAGNI) y Outbox hacia Kafka**:
   - La comunicación con el Plan 015 (lectura de diagnóstico) y con el Plan 010 (descuento FEFO de inventario) es estrictamente síncrona mediante puertos en memoria dentro de la misma transacción local `@Transactional`. No se usan eventos de Kafka para coordinar el descuento interno.
   - Una vez persistido el consumo y confirmado el descuento local, se registra atómicamente en el Outbox de Spring Modulith el evento de integración `AviControl.Sanitario.ConsumoMedicamentoRegistrado.v1` utilizando **`loteId` como clave de partición (`partitionKey = loteId.toString()`)** para que el Módulo 3 procese en orden estricto todos los costos atribuidos al lote.
6. **Autonomía UX/UI y Adaptadores con Respaldo (_Fallback_)**:
   - Se expone `GET /api/diagnosticos/{diagnosticoId}/consumos-medicamento` para que la interfaz del veterinario muestre el historial de consumos ya registrados en ese diagnóstico junto con nombres legibles (`galponNombre`, `loteNombre`, `medicamentoNombre`, `enfermedadNombre`).
   - Los adaptadores hacia el Plan 015 y el Plan 010 incluyen respaldo autónomo (_fallback/seed_) para pruebas locales, garantizando que el equipo del actor Veterinario pueda desplegar y validar su flujo completo sin bloquearse si otro actor tiene pendientes en su rama.

### Alcance documental

- Este plan implementa [SPEC-013](../specs/013-RegistrarConsumoMedicamento.md) y es propietario de `ConsumoMedicamento` y `DetalleConsumo`.
- El [Plan 006](006-GestionDeMedicaciones.md) gestiona el catálogo de prescripciones y la consulta diaria operativa (que no descuenta stock).
- El [Plan 002](002-GestionDeRecepciones.md) conserva la propiedad de las recepciones y precios históricos de compra.
- El [Plan 010](010-GestionDelInventario.md) conserva la propiedad de los saldos y movimientos de bodega y ejecuta el descuento transaccional FEFO.
- El [Plan 015](015-DiagnosticoYReintegroSanitario.md) conserva el diagnóstico clínico que origina el tratamiento.
- El Módulo 3 calcula el costo contable real a partir de los detalles enviados por este plan (`suma(cantidadDescontadaUnidadBase × precioNetoHistoricoUnidadBase)`).

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 013-RegistrarConsumoMedicamento.md
└── plan/
    └── 012-RegistrarConsumoDeMedicamentos.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/medicamento/consumo/
│   │   ├── ConsumoMedicamento.java
│   │   ├── DetalleConsumo.java
│   │   └── UnidadMedidaMedicamento.java
│   ├── event/medicamento/consumo/
│   │   └── ConsumoMedicamentoRegistradoEvent.java
│   ├── exception/medicamento/consumo/
│   │   ├── DiagnosticoParaConsumoNoEncontradoException.java
│   │   ├── DiagnosticoSinMedicacionPrescritaException.java
│   │   ├── UnidadMedidaIncompatibleException.java
│   │   ├── ExistenciasMedicamentoInsuficientesException.java
│   │   └── FechaAplicacionInvalidaException.java
│   └── port/out/medicamento/consumo/
│       ├── ConsumoMedicamentoRepositoryPort.java
│       ├── DiagnosticoQueryPort.java
│       ├── InventarioMedicamentoPort.java
│       └── IntegrationEventPublisherPort.java
├── application/medicamento/consumo/
│   ├── RegistrarConsumoMedicamentoUseCase.java
│   ├── ConsultarConsumosPorDiagnosticoUseCase.java
│   ├── command/
│   │   └── RegistrarConsumoMedicamentoCommand.java
│   └── result/
│       ├── ConsumoMedicamentoResult.java
│       └── DetalleConsumoResult.java
└── infrastructure/
    ├── adapter/in/rest/medicamento/consumo/
    │   ├── ConsumoMedicamentoController.java
    │   ├── dto/
    │   │   ├── RegistrarConsumoMedicamentoRequest.java
    │   │   ├── ConsumoMedicamentoResponse.java
    │   │   └── DetalleConsumoResponse.java
    │   └── mapper/ConsumoMedicamentoRestMapper.java
    ├── adapter/out/
    │   ├── persistence/medicamento/consumo/
    │   │   ├── entity/
    │   │   │   ├── ConsumoMedicamentoEntity.java
    │   │   │   └── DetalleConsumoEntity.java
    │   │   ├── repository/SpringDataConsumoMedicamentoRepository.java
    │   │   ├── mapper/ConsumoMedicamentoPersistenceMapper.java
    │   │   └── ConsumoMedicamentoPersistenceAdapter.java
    │   ├── event/medicamento/consumo/
    │   │   ├── ConsumoMedicamentoKafkaPublisherAdapter.java
    │   │   └── payload/ConsumoMedicamentoRegistradoV1Payload.java
    │   └── internal/medicamento/consumo/
    │       ├── DiagnosticoConsumoQueryAdapter.java
    │       └── InventarioMedicamentoAdapter.java
    └── config/
        └── ConsumoMedicamentoBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/medicamento/consumo/
│   └── ConsumoMedicamentoTest.java
├── application/medicamento/consumo/
│   ├── RegistrarConsumoMedicamentoUseCaseTest.java
│   └── ConsultarConsumosPorDiagnosticoUseCaseTest.java
├── infrastructure/adapter/in/rest/medicamento/consumo/
│   └── ConsumoMedicamentoControllerTest.java
└── integration/medicamento/consumo/
    └── ConsumoMedicamentoIntegrationTest.java
```

**Structure Decision**: El dominio y el caso de uso coordinan las invariantes de conversión dimensional, integridad de la suma de detalles y trazabilidad histórica. `InventarioMedicamentoAdapter` invoca la operación propietaria de descuento FEFO del Plan 010 dentro de la misma transacción local sin modificar directamente tablas ajenas, y `ConsumoMedicamentoKafkaPublisherAdapter` publica hacia el Módulo 3 mediante el Outbox de Spring Modulith.

### Entidades y relación

```text
DiagnosticoGalpon (Ref. Plan 015) 1 ───< 0..* ConsumoMedicamento 1 ───< 1..* DetalleConsumo

ConsumoMedicamento (Aggregate Root)
├── id: UUID
├── diagnosticoId: UUID (Ref. Plan 015)
├── galponId: UUID (Derivado del diagnóstico - Ref. Módulo 1)
├── loteId: UUID (Derivado del diagnóstico - Ref. Módulo 1)
├── medicamentoId: UUID (Derivado del snapshot del diagnóstico - Ref. Plan 002)
├── medicamentoNombreHistorico: String
├── cantidadIngresada: BigDecimal (> 0)
├── unidadIngresada: UnidadMedidaMedicamento (MILIGRAMO, GRAMO, MILILITRO, UNIDAD)
├── cantidadNormalizada: BigDecimal (> 0, escala 4 decimales)
├── unidadBase: UnidadMedidaMedicamento (GRAMO, MILILITRO, UNIDAD)
├── fechaAplicacion: LocalDate (America/Bogota)
├── registradoPor: UUID (Veterinario autenticado)
├── registradoEn: Instant (UTC)
└── version: Integer (@Version para Optimistic Locking)

DetalleConsumo (Entidad Hija Inmutable)
├── id: UUID
├── consumoId: UUID
├── recepcionId: UUID (Ref. Plan 002)
├── codigoLoteRecepcion: String
├── cantidadDescontadaUnidadBase: BigDecimal (> 0)
├── precioNetoHistoricoUnidadBase: BigDecimal (>= 0)
└── moneda: String ("COP")
```

Invariante de dominio: $\sum \text{detalle.cantidadDescontadaUnidadBase} == \text{consumo.cantidadNormalizada}$. Diagnóstico, lote, galpón, medicamento y recepción son identidades externas; no se crean copias autoritativas de esas tablas.

### Contratos de los puertos

| Puerto                             | Tipo                                     | Responsabilidad                                                                                                                                                                                                                             |
| ---------------------------------- | ---------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ConsumoMedicamentoRepositoryPort` | Salida (Persistencia)                    | Guardar de forma atómica e inmutable el consumo junto con sus detalles; listar los consumos registrados por `diagnosticoId` o `loteId`.                                                                                                     |
| `DiagnosticoQueryPort`             | Salida (Integración Plan 015)            | Obtener el diagnóstico, verificar que tenga medicación prescrita (`requiereSacrificioHistorico == false`) y recuperar `galponId`, `galponNombre`, `loteId`, `loteNombre`, `medicamentoId`, `medicamentoNombre` y `fechaDiagnostico`.        |
| `InventarioMedicamentoPort`        | Salida (Integración Plan 010 / Plan 002) | Obtener la `unidadBase` del medicamento y ejecutar en el Plan 010 el descuento atómico FEFO con bloqueo de recepciones vigentes, devolviendo las porciones descontadas con su `recepcionId` y `precioNetoHistoricoUnidadBase` del Plan 002. |
| `IntegrationEventPublisherPort`    | Salida (Outbox / Kafka)                  | Registrar en el Outbox de Spring Modulith y despachar a Kafka el contrato `AviControl.Sanitario.ConsumoMedicamentoRegistrado.v1` con `partitionKey = loteId` para el Módulo 3.                                                              |

### Contratos HTTP propuestos

| Método y ruta                                                 | Acceso                                   | Parámetros / Cabeceras                  | Responsabilidad                                                                                 |
| ------------------------------------------------------------- | ---------------------------------------- | --------------------------------------- | ----------------------------------------------------------------------------------------------- |
| `POST /api/diagnosticos/{diagnosticoId}/consumos-medicamento` | `ROLE_VETERINARIO`                       | Header obligatorio: `X-Idempotency-Key` | Registrar consumo real, normalizar unidad, descontar existencias FEFO y publicar a Módulo 3.    |
| `GET /api/diagnosticos/{diagnosticoId}/consumos-medicamento`  | `ROLE_VETERINARIO`, `ROLE_ADMINISTRADOR` | Path: `diagnosticoId`                   | Listar el historial de consumos y detalles por recepción aplicados a partir de ese diagnóstico. |

La respuesta y los errores usan `application/problem+json` conforme a [General.md](General.md) y SPEC-013.

### JSON común de errores

```json
{
  "type": "https://avicontrol/errors/consumo-medicamento-no-permitido",
  "title": "Consumo de medicamento no permitido",
  "status": 409,
  "detail": "Las existencias disponibles en recepciones vigentes no cubren la cantidad solicitada",
  "instance": "/api/diagnosticos/b1a2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d/consumos-medicamento",
  "code": "EXISTENCIAS_INSUFICIENTES",
  "correlationId": "uuid",
  "fieldErrors": []
}
```

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Acordar contratos y reglas con diagnóstico (Plan 015), inventario/recepciones (Planes 010 y 002) y costeo (Módulo 3).

- [ ] T001 Acordar con el Plan 015 el contrato de `DiagnosticoQueryPort` para resolver automáticamente `medicamentoId`, `medicamentoNombre`, `loteId`, `galponId` y validar que el diagnóstico posea medicación prescrita.
- [ ] T002 Acordar con el Plan 010 la normalización de unidades, el criterio FEFO y el comando atómico de descuento por recepción (incluyendo el `precioNetoHistoricoUnidadBase` conservado por el Plan 002 y respaldo _fallback_ para desarrollo local autónomo).
- [ ] T003 Acordar con el Módulo 3 el esquema de payload del evento `AviControl.Sanitario.ConsumoMedicamentoRegistrado.v1` particionado por `loteId`.
- [ ] T004 Confirmar roles (`ROLE_VETERINARIO`), reloj (`America/Bogota`), filtro transversal de `X-Idempotency-Key` y catálogo de errores RFC 9457 conforme a [General.md](General.md).

**Checkpoint**: Contratos entre propietarios de datos y respaldos locales acordados antes de implementar.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Preparar el modelo histórico, normalización dimensional, persistencia y puertos de salida.

- [ ] T005 Implementar en Java puro `ConsumoMedicamento`, `DetalleConsumo` y `UnidadMedidaMedicamento` con conversión exacta (`1.000 mg = 1 g`), validación de dimensiones compatibles e invariante de suma de detalles igual a `cantidadNormalizada`.
- [ ] T006 Definir las excepciones de dominio (`DiagnosticoParaConsumoNoEncontradoException`, `DiagnosticoSinMedicacionPrescritaException`, `UnidadMedidaIncompatibleException`, `ExistenciasMedicamentoInsuficientesException`, `FechaAplicacionInvalidaException`) y los puertos de salida.
- [ ] T007 Crear la migración Flyway usando la siguiente versión disponible del repositorio para las tablas `consumo_medicamento` (con columna `version` e índices por `diagnostico_id` y `lote_id`) y `detalle_consumo_medicamento` (con `recepcion_id` y `precio_neto_historico_unidad_base`), prohibiendo el borrado físico (`DELETE`).
- [ ] T008 Implementar `ConsumoMedicamentoPersistenceAdapter` y los adaptadores internos `DiagnosticoConsumoQueryAdapter` e `InventarioMedicamentoAdapter` sin acceder a repositorios JPA privados de otros módulos.
- [ ] T009 Implementar `ConsumoMedicamentoKafkaPublisherAdapter` utilizando el registro de publicaciones de Spring Modulith (Transactional Outbox) con `partitionKey = loteId.toString()`.
- [ ] T010 Configurar el límite transaccional `@Transactional` y la composición de beans en `ConsumoMedicamentoBeanConfiguration`.

**Checkpoint**: Modelo, conversión de unidades, persistencia e integración Outbox listos sin escrituras parciales.

## Phase 3: User Story 1 — Registrar y Consultar el Consumo Real de un Medicamento (Priority: P1)

**Spec**: SPEC-013, historia 1.

**Goal**: El veterinario registra la cantidad de medicamento realmente aplicada a partir de un diagnóstico activo, descuenta atómicamente una o varias recepciones en orden FEFO conservando el precio histórico de cada porción, publica el desglose hacia el Módulo 3 y consulta los consumos aplicados al diagnóstico.

**Independent Test**: Un consumo válido en miligramos o gramos normaliza la unidad a `GRAMO`, descuenta una o varias recepciones en orden FEFO, almacena el precio neto histórico de cada porción sin promediar y registra el evento en el Outbox. Si el stock es insuficiente, la unidad es incompatible, el diagnóstico es de rama mortal (sin medicación) o el usuario no tiene `ROLE_VETERINARIO`, se rechaza la operación y se hace _rollback_ total sin descuentos parciales.

### Definición del evento de integración para User Story 1

**Evento de integración (Outbox -> Apache Kafka)**: `AviControl.Sanitario.ConsumoMedicamentoRegistrado.v1`

- **Partition Key en Kafka**: `loteId` (asegura el orden cronológico de todos los eventos de costos del lote hacia el Módulo 3).
- Estructura conforme a [General.md](General.md): `eventId`, `eventType`, `eventVersion` (`"v1"`), `occurredAt`, `publishedAt`, `producer`, `correlationId`, `causationId`, `aggregateType` (`"ConsumoMedicamento"`), `aggregateId` (`consumoId`) y `payload` (`consumoId`, `diagnosticoId`, `galponId`, `loteId`, `medicamentoId`, `medicamentoNombre`, `cantidadNormalizada`, `unidadBase`, `fechaAplicacion`, `registradoPor`, `detalles` con `recepcionId`, `cantidadDescontadaUnidadBase`, `precioNetoHistoricoUnidadBase`, `moneda`).
- Se registra en la tabla Outbox de Spring Modulith dentro de la misma transacción local `@Transactional` que guarda el consumo y descuenta el inventario, despachándose a Kafka después del _commit_.

### Definición de endpoints REST

| Elemento      | Definición                                                                                                                                                                                                                                                                                        |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Método y ruta | `POST /api/diagnosticos/{diagnosticoId}/consumos-medicamento`                                                                                                                                                                                                                                     |
| Autorización  | `ROLE_VETERINARIO`                                                                                                                                                                                                                                                                                |
| Cabeceras     | `X-Idempotency-Key` (obligatoria para impedir dobles descuentos ante reintentos de red)                                                                                                                                                                                                           |
| Entrada       | `cantidad` (`BigDecimal > 0`), `unidad` (`MILIGRAMO`, `GRAMO`, `MILILITRO`, `UNIDAD`) y `fechaAplicacion` (`LocalDate`). No recibe `galponId`, `loteId` ni `medicamentoId` editables.                                                                                                             |
| Respuesta 201 | `ConsumoMedicamentoResponse` enriquecido con nombres legibles, cantidad normalizada, unidad base y desglose de `detalles` por recepción con su precio neto histórico.                                                                                                                             |
| Errores       | `400` por cantidad/unidad/fecha inválida o unidad de dimensión incompatible; `401`/`403` por seguridad; `404` por diagnóstico inexistente; `409` por diagnóstico sin medicación, recepción no disponible o existencias insuficientes; `503` si una dependencia imprescindible no está disponible. |

| Elemento      | Definición                                                                                                                        |
| ------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| Método y ruta | `GET /api/diagnosticos/{diagnosticoId}/consumos-medicamento`                                                                      |
| Autorización  | `ROLE_VETERINARIO`, `ROLE_ADMINISTRADOR`                                                                                          |
| Respuesta 200 | Lista de `ConsumoMedicamentoResponse` registrados para ese diagnóstico, ordenados por fecha de aplicación y registro descendente. |

#### JSON de solicitud (`POST /api/diagnosticos/{diagnosticoId}/consumos-medicamento`)

```json
{
  "cantidad": 200,
  "unidad": "GRAMO",
  "fechaAplicacion": "2026-10-02"
}
```

#### JSON de respuesta exitosa (Response 201)

```json
{
  "consumoId": "uuid",
  "diagnosticoId": "uuid",
  "galponId": "uuid",
  "galponNombre": "Galpón 5",
  "loteId": "uuid",
  "loteNombre": "LDP-005",
  "medicamentoId": "uuid",
  "medicamentoNombre": "Amprolio 20% Solución",
  "cantidadIngresada": 200.0,
  "unidadIngresada": "GRAMO",
  "cantidadNormalizada": 200.0,
  "unidadBase": "GRAMO",
  "fechaAplicacion": "2026-10-02",
  "registradoPor": "uuid",
  "registradoEn": "2026-10-02T14:20:00Z",
  "version": 1,
  "detalles": [
    {
      "recepcionId": "uuid",
      "codigoLoteRecepcion": "MED-2026-01",
      "cantidadDescontadaUnidadBase": 200.0,
      "precioNetoHistoricoUnidadBase": 12.5,
      "moneda": "COP"
    }
  ]
}
```

### Tests e implementación

- [ ] T011 [US1] Probar autorización exclusiva `ROLE_VETERINARIO`, diagnóstico existente con medicación y derivación automática de `galponId`, `loteId` y `medicamentoId` (rechazando diagnósticos de enfermedades mortales sin medicación con `409`).
- [ ] T012 [US1] Probar normalización de unidades compatibles (`1.000 mg = 1 g`) y rechazo de unidades de dimensiones incompatibles (`GRAMO` vs. `MILILITRO` o `UNIDAD`).
- [ ] T013 [US1] Probar distribución FEFO abastecida por una sola recepción y fraccionada entre múltiples recepciones ordenadas por vencimiento.
- [ ] T014 [US1] Probar exclusión de recepciones vencidas o anuladas, rechazo por falta de existencias (`409 Conflict`) y _rollback_ total sin descuentos parciales ni eventos en Outbox.
- [ ] T015 [US1] Probar conservación de precios netos históricos diferentes sin promediar, suma exacta de detalles igual a `cantidadNormalizada` e idempotencia con `X-Idempotency-Key`.
- [ ] T016 [US1] Implementar `RegistrarConsumoMedicamentoUseCase`, `ConsultarConsumosPorDiagnosticoUseCase`, persistencia, descuento transaccional con Plan 010, registro Outbox hacia Kafka y endpoints `POST` y `GET` en `ConsumoMedicamentoController`.
- [ ] T017 [US1] Verificar el contrato del evento `AviControl.Sanitario.ConsumoMedicamentoRegistrado.v1` (`partitionKey = loteId`) para el costeo del Módulo 3.

**Checkpoint**: El consumo y el descuento de inventario se confirman juntos en una sola transacción local, con trazabilidad exacta por recepción y sin pérdida del precio histórico.

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Verificar contratos OpenAPI, concurrencia, atomicidad, idempotencia y disponibilidad para costeo en Módulo 3.

- [ ] T018 Documentar los dos endpoints (`POST` y `GET`), cabecera `X-Idempotency-Key`, autorización, unidades permitidas y errores RFC 9457 en OpenAPI 3.
- [ ] T019 Completar `ConsumoMedicamentoIntegrationTest` con Testcontainers (PostgreSQL y Kafka reales): probar concurrencia sobre recepciones con saldo limitado, reversión completa si falla el descuento y publicación Outbox tras el _commit_.
- [ ] T020 Verificar con ArchUnit el aislamiento de capas y que no existan accesos directos a repositorios JPA privados de los Planes 002, 010 o 015.
- [ ] T021 Medir objetivos de respuesta verificando `p95 < 2 segundos` en registro de consumo y `p95 < 1 segundo` en consulta.

**Checkpoint**: Plan 012 completado, probado e integrado con Inventario y Finanzas.

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Define contratos con Planes 015, 010/002 y Módulo 3.
- **Foundational (Phase 2)**: Depende del cierre de Setup y habilita el modelo, la conversión dimensional y el Outbox.
- **US1 (Phase 3)**: Depende del diagnóstico con medicación del Plan 015 y del descuento atómico FEFO del Plan 010.
- **Polish (Phase 4)**: Depende de la historia completa y verifica concurrencia e integración end-to-end.

### Dependencias con otros planes

- **Plan 015 (Diagnóstico y Reintegro Sanitario)**: Propietario del diagnóstico clínico y del snapshot de la medicación prescrita que determina el medicamento, el lote y el galpón.
- **Plan 010 (Gestión del Inventario)**: Propietario de unidades, saldos y movimientos de inventario; ejecuta el descuento atómico FEFO por recepción.
- **Plan 002 (Gestión de Recepciones)**: Propietario de las recepciones de medicamentos y de los precios netos históricos de compra asociados a cada recepción descontada.
- **Plan 006 (Gestión de Medicaciones)**: Propietario del catálogo de medicaciones y consultas diarias de solo lectura.
- **Módulo 3 (Finanzas)**: Consumidor del evento `AviControl.Sanitario.ConsumoMedicamentoRegistrado.v1` (`partitionKey = loteId`) para liquidar el costo real de los medicamentos aplicados al lote.
- **Módulo 1 / Plan 001**: Propietario de las identidades de lote y galpón.
- **General.md**: Seguridad, transacciones, idempotencia, errores RFC 9457 y Transactional Outbox.

### Dentro de cada User Story

- Definir invariantes dimensionales y de suma de detalles en el dominio antes del caso de uso.
- Acordar los puertos propietarios (con respaldo _fallback_ local) antes de los adaptadores.
- Implementar el caso de uso transaccional antes del controlador REST.
- Probar autorización, idempotencia (`X-Idempotency-Key`), concurrencia y _rollback_ atómico junto con la implementación.

## Notes

- El consumo representa lo realmente aplicado en campo; la prescripción en el Plan 006 y el diagnóstico en el Plan 015 no descuentan inventario.
- El precio neto histórico por recepción se conserva en cada `DetalleConsumo` y jamás se promedia.
- Las tareas `T001` a `T021` cubren la totalidad del [SPEC-013](../specs/013-RegistrarConsumoMedicamento.md).
