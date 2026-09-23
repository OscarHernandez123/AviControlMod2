# Implementation Plan: Registrar consumo de medicamento por lote

**Date**: 2026-09-22  
**Specs**:  
- [013-RegistrarConsumoMedicamento.md](docs/specs/013-RegistrarConsumoMedicamento.md)

---

## 1. Summary

El módulo **Registrar consumo de medicamento por lote** permite al Médico Veterinario asentar la cantidad física real de medicamento administrada a una parvada bajo tratamiento clínico activo (`Spec 011`). El sistema deriva automáticamente `loteId`, `galponId`, `medicacionId` y `medicamentoId` desde el diagnóstico clínico, normaliza la cantidad ingresada a la unidad base del producto farmacéutico y descuenta las existencias físicas de las recepciones de bodega aplicando un algoritmo **FIFO por fecha de vencimiento**.

Cada consumo genera **una fila de detalle independiente por cada recepción afectada**, preservando el precio histórico unitario de compra sin promediar valores contables. Al confirmarse, el sistema publica de forma atómica el evento `ConsumoMedicamentoRegistradoIntegrationEvent` hacia el **Módulo 3 (Costos de Producción)** mediante el patrón **Transactional Outbox** hacia **Kafka**. **El Módulo 2 no calcula costos totales de producción**: su responsabilidad se circunscribe a registrar la salida física del fármaco y publicar los datos crudos consolidados hacia los módulos consumidores.

La solución se implementa en **Java 21** con **Spring Boot 3.x (Spring MVC + Spring Data JPA)** bajo **arquitectura hexagonal pura**, control de **Concurrencia Optimista (`@Version`)** complementado con bloqueo pesimista en inventario (`SELECT ... FOR UPDATE`), trazabilidad inmutable en **`san_auditoria`** y validación de idempotencia técnica vía `X-Idempotency-Key`.

---

## 2. Technical Context

- **Language/Version**: Java 21 (LTS - Virtual Threads habilitados mediante `spring.threads.virtual.enabled=true`).
- **Primary Dependencies**: Spring Boot 3.x (Spring MVC), Spring Cloud Stream (Kafka Binder), Spring Data JPA (Hibernate 6.x), PostgreSQL JDBC Driver, Lombok, MapStruct, Jakarta Validation, JUnit 5, Mockito, Testcontainers (PostgreSQL + Kafka).
- **Storage**: PostgreSQL 16+ relacional vía JDBC/JPA (`san_consumos`, `san_detalles_consumo`, `san_recepciones`, `san_outbox`, `san_auditoria`).
- **Testing**: JUnit 5, Mockito, MockMvc, Testcontainers.
- **Target Platform**: Contenedores Linux (Docker / Kubernetes).
- **Project Type**: Backend REST micro-service (Módulo 2: Sanidad y Bioseguridad).
- **Performance Goals**: Latencia < 250 ms en validación, registro y persistencia en outbox (`SC-005`); descuento y publicación al Módulo 3 < 2 s.
- **Constraints**: 
  - Prohibido el borrado físico (`DELETE` SQL) (`FR-013`, `SC-007`).
  - Escritura atómica obligatoria: consumo + detalles + descuento inventario + outbox + auditoría bajo un único `@Transactional` (`FR-010`, `FR-012`, `SC-008`).
  - Rol exclusivo `VETERINARIO` para transacciones de salida física (`FR-001`, `SC-004`).
  - Algoritmo FIFO obligatorio ordenado por fecha de vencimiento ascendente (`FR-006`).
  - Sin descuentos parciales: rechazo total de la transacción si el stock total disponible es menor al solicitado (`FR-008`, `SC-003`).
  - Sin promedios contables de precios: un detalle independiente por cada recepción (`FR-007`, `SC-002`).
  - Idempotencia HTTP mediante cabecera `X-Idempotency-Key` retenida por 24 horas (`FR-014`, `SC-006`).

---

## 3. Project Structure

```text
src/main/java/com/avicontrol/sanidad/
├── domain/                                # Núcleo Puro de Dominio (Sin dependencias Spring/JPA)
│   ├── model/
│   │   └── consumo/
│   │       ├── ConsumoMedicamento.java        # Aggregate Root
│   │       ├── ConsumoMedicamentoId.java      # Value Object UUID
│   │       ├── DetalleConsumo.java            # Entidad interna (1..N)
│   │       ├── DetalleConsumoId.java          # Value Object UUID
│   │       ├── DiagnosticoId.java             # Value Object UUID (Ref. Spec 011)
│   │       ├── GalponId.java                  # Value Object UUID (Ref. Módulo 1)
│   │       ├── LoteId.java                    # Value Object UUID (Ref. Módulo 1)
│   │       ├── MedicamentoId.java             # Value Object UUID (Ref. Spec 008)
│   │       ├── RecepcionId.java               # Value Object UUID (Ref. Bodega)
│   │       ├── CantidadFisica.java            # Value Object (BigDecimal positivo + unidad)
│   │       ├── UnidadBase.java                # Value Object (magnitud normalizada)
│   │       ├── PrecioHistorico.java           # Value Object (BigDecimal tarifa + moneda)
│   │       └── ConversionUnidadService.java   # Servicio de dominio para conversión métrica
│   ├── exception/
│   │   └── consumo/
│   │       ├── ConsumoNotFoundException.java
│   │       ├── DiagnosticoNotFoundException.java
│   │       ├── DiagnosticoSinMedicacionException.java
│   │       ├── StockInsuficienteException.java
│   │       ├── UnidadIncompatibleException.java
│   │       ├── RecepcionNoDisponibleException.java
│   │       └── ConsumoConcurrenciaException.java
│   └── repository/                        # Puertos de Salida (Driven Ports)
│       ├── ConsumoMedicamentoRepositoryPort.java # Persistencia del Agregado
│       ├── DiagnosticoQueryPort.java              # Consulta al expediente clínico (Spec 011)
│       ├── MedicamentoQueryPort.java              # Consulta al catálogo terapéutico (Spec 008)
│       ├── RecepcionQueryPort.java                # Consulta y descuento FIFO en bodega
│       ├── OutboxRepositoryPort.java              # Registro local para Transactional Outbox
│       └── AuditoriaSanitariaPort.java            # Bitácora inmutable en san_auditoria
├── application/
│   └── consumo/                           # Casos de Uso (Una clase por responsabilidad)
│       ├── RegistrarConsumoUseCase.java
│       ├── ConsultarConsumoUseCase.java
│       └── ListarConsumosPorDiagnosticoUseCase.java
├── infrastructure/
│   ├── adapter/in/rest/                   # Adaptador Primario REST (Spring MVC)
│   │   ├── ApiErrorResponse.java
│   │   ├── GlobalExceptionHandler.java    # @RestControllerAdvice
│   │   ├── filter/
│   │   │   ├── RoleValidationFilter.java
│   │   │   └── IdempotencyFilter.java
│   │   └── consumo/
│   │       ├── ConsumoMedicamentoController.java
│   │       ├── dto/
│   │       │   ├── RegistrarConsumoRequest.java
│   │       │   ├── ConsumoMedicamentoResponse.java
│   │       │   ├── DetalleConsumoResponse.java
│   │       │   └── ConsumoFiltroRequest.java
│   │       └── mapper/
│   │           └── ConsumoRestMapper.java
│   ├── adapter/out/
│   │   ├── persistence/                   # Adaptadores Secundarios JPA
│   │   │   ├── consumo/
│   │   │   │   ├── ConsumoMedicamentoEntity.java
│   │   │   │   ├── DetalleConsumoEntity.java
│   │   │   │   ├── ConsumoMedicamentoJpaRepository.java
│   │   │   │   ├── ConsumoMedicamentoRepositoryAdapter.java
│   │   │   │   └── mapper/ConsumoPersistenceMapper.java
│   │   │   ├── outbox/
│   │   │   │   ├── OutboxEntity.java
│   │   │   │   ├── OutboxJpaRepository.java
│   │   │   │   └── OutboxRepositoryAdapter.java
│   │   │   └── auditoria/
│   │   │       ├── AuditoriaEntity.java
│   │   │       ├── AuditoriaJpaRepository.java
│   │   │       └── AuditoriaRepositoryAdapter.java
│   │   ├── client/                        # Adaptadores Cross-Context
│   │   │   ├── DiagnosticoQueryAdapter.java      # Consulta síncrona al Spec 011
│   │   │   ├── MedicamentoQueryAdapter.java      # Consulta síncrona al Spec 008
│   │   │   └── RecepcionQueryAdapter.java        # Consulta FIFO y UPDATE bloqueante en bodega
│   │   └── event/                         # Transactional Outbox Relay
│   │       ├── OutboxRelayScheduler.java  # Polling worker hacia Kafka
│   │       └── KafkaEventPublisherAdapter.java
│   └── config/
│       ├── BeanConfiguration.java         # Inyección explícita de casos de uso
│       ├── UnidadConversionConfiguration.java # Tabla declarativa de equivalencias métricas
│       └── SecurityConfig.java            # Bloqueo estricto del verbo DELETE
└── events/                                # Contratos de Eventos de Integración
    └── ConsumoMedicamentoRegistradoIntegrationEvent.java
```

> **Decisión de Estructura**: Microservicio basado en Spring MVC imperativo con Virtual Threads de Java 21. El dominio puro no posee dependencias de frameworks ni de JPA. La comunicación con otros contextos se canaliza a través de puertos driven dedicados (`DiagnosticoQueryPort`, `MedicamentoQueryPort`, `RecepcionQueryPort`). El algoritmo FIFO y el descuento se encapsulan en `RecepcionQueryPort` como una transacción atómica local con bloqueo a nivel de fila (`SELECT ... FOR UPDATE`).

---

## 4. Implementation Phases

### Phase 1: Setup (Shared Infrastructure)
**Propósito**: Inicializar el proyecto base, configuración estándar de Spring Boot y herramientas de calidad.

- [ ] **T001** Inicializar proyecto Spring Boot 3.x con Java 21 y dependencias estándar (Spring Web, Spring Data JPA, Kafka Stream, Validation, Testcontainers, PostgreSQL Driver).
- [ ] **T002** Generar la estructura de paquetes hexagonal según la convención del proyecto (`domain`, `application`, `infrastructure`, `events`).
- [ ] **T003** Configurar `application.yml` con pool HikariCP, parámetros de Kafka (`sanitary.medication.consumed.v1`) y habilitar Virtual Threads (`spring.threads.virtual.enabled=true`).
- [ ] **T004** Configurar Docker Compose local con servicios `postgres:16-alpine` y broker `kafka` (bitnami/kafka:latest con KRaft).
- [ ] **T005** Configurar Checkstyle, SpotBugs y Jacoco con umbral mínimo de cobertura del 85%.
- [ ] **T006** Configurar pipeline de integración continua (CI) en GitHub Actions para compilar, ejecutar tests con Testcontainers y validar linters.

---

### Phase 2: Foundational (Blocking Prerequisites)
**Propósito**: Construir el modelo de dominio puro, la persistencia JPA, el Transactional Outbox y la auditoría inmutable.  
> ⚠️ **CRÍTICO**: Ningún caso de uso funcional debe implementarse antes de validar esta fase.

- [ ] **T007** Crear scripts DDL en PostgreSQL para tablas `san_consumos`, `san_detalles_consumo`, `san_outbox` y `san_auditoria` con llaves primarias UUID:
  - Clave foránea de `san_detalles_consumo.consumo_id` hacia `san_consumos.id` con `ON DELETE NO ACTION` (prohibición de cascada de borrado).
  - Índice funcional `idx_consumo_diagnostico` sobre `san_consumos(diagnostico_id)`.
  - Índice `idx_detalle_recepcion` sobre `san_detalles_consumo(recepcion_id)` para auditoría y trazabilidad de compras.
- [ ] **T008** Definir Value Objects de Dominio con validación estricta:
  - `ConsumoMedicamentoId.java`, `DetalleConsumoId.java`: Envoltorios inmutables sobre UUID.
  - `DiagnosticoId.java`, `GalponId.java`, `LoteId.java`, `MedicamentoId.java`, `RecepcionId.java`: Envoltorios inmutables sobre UUID.
  - `CantidadFisica.java`: `BigDecimal` estrictamente mayor que cero con unidad asociada (no nulo, no negativo, no cero).
  - `UnidadBase.java`: Identificador normalizado de la magnitud base (`ml`, `g`, `unidades`, `dosis`).
  - `PrecioHistorico.java`: `BigDecimal` no negativo con código de moneda ISO-4217 (`COP`, `USD`).
  - `ConversionUnidadService.java`: Servicio de dominio que valida la compatibilidad dimensional y ejecuta la conversión métrica hacia la unidad base.
- [ ] **T009** Crear Aggregate Root `ConsumoMedicamento.java` y entidad interna `DetalleConsumo.java`:
  - `ConsumoMedicamento.registrar(...)`: Constructor de fábrica que exige los datos derivados (`diagnosticoId`, `loteId`, `galponId`, `medicamentoId`), la cantidad normalizada en unidad base, la fecha de aplicación y una lista no vacía de `DetalleConsumo`.
  - `DetalleConsumo.crear(...)`: Constructor de fábrica que encapsula `recepcionId`, `cantidadDescontada`, `precioHistoricoUnidadBase` y moneda.
- [ ] **T010** Crear excepciones de dominio en `domain/exception/consumo/` (`ConsumoNotFoundException`, `DiagnosticoNotFoundException`, `DiagnosticoSinMedicacionException`, `StockInsuficienteException`, `UnidadIncompatibleException`, `RecepcionNoDisponibleException`, `ConsumoConcurrenciaException`).
- [ ] **T011** Definir puertos secundarios en `domain/repository/`:
  - `ConsumoMedicamentoRepositoryPort.java`: métodos `guardar(ConsumoMedicamento c)`, `buscarPorId(ConsumoMedicamentoId id)`, `listarPorDiagnostico(DiagnosticoId id)`.
  - `DiagnosticoQueryPort.java`: método `DiagnosticoSnapshot obtenerPorId(DiagnosticoId id)` (retorna `loteId`, `galponId`, `medicacionId`, `medicamentoId`, `requiereSacrificioSanitario`).
  - `MedicamentoQueryPort.java`: método `String obtenerUnidadBase(MedicamentoId id)`, `boolean existeYActivo(MedicamentoId id)`.
  - `RecepcionQueryPort.java`: métodos `List<RecepcionSnapshot> listarDisponiblesFIFO(MedicamentoId id)`, `void descontarExistencias(List<DescuentoRecepcion> descuentos)`.
  - `OutboxRepositoryPort.java`: método `guardarEvento(OutboxEntity evento)`.
  - `AuditoriaSanitariaPort.java`: método inmutable `registrarTraza(AuditoriaEntity traza)`.
- [ ] **T012** Implementar entidades JPA (`ConsumoMedicamentoEntity.java`, `DetalleConsumoEntity.java`, `OutboxEntity.java`, `AuditoriaEntity.java`) y repositorios `JpaRepository`.
- [ ] **T013** Implementar adaptadores JPA que cumplan los puertos secundarios garantizando el mapeo de dominio vía `ConsumoPersistenceMapper`.
- [ ] **T014** Implementar `GlobalExceptionHandler` con `@RestControllerAdvice` para transformar excepciones de dominio a esquemas RFC-7807 (`ApiErrorResponse`) con códigos HTTP adecuados (400, 403, 404, 409, 503).
- [ ] **T015** Implementar `IdempotencyFilter` (`OncePerRequestFilter`) para cachear respuestas asociadas a `X-Idempotency-Key` durante 24 horas (`FR-014`).
- [ ] **T016** Implementar `RoleValidationFilter` para verificar el claim `VETERINARIO` en todas las peticiones a `/api/v1/sanitary/consumos/**` (`FR-001`).
- [ ] **T017** Configurar `UnidadConversionConfiguration.java` con tabla declarativa de conversiones admitidas (`ml` $\leftrightarrow$ `L`: 1000, `g` $\leftrightarrow$ `kg`: 1000, `unidades`: 1, `dosis`: 1). Las unidades incompatibles se rechazan con `UnidadIncompatibleException`.

> **Checkpoint**: Modelo de dominio puro compilando, tablas creadas, infraestructura JPA operativa y filtros transversales listos.

---

### Phase 3: User Story 1 – Registro del Consumo Real y Descuento FIFO (Priority: P1)
**Propósito**: Permitir al Veterinario registrar la cantidad real aplicada a un lote, derivando automáticamente los identificadores desde el diagnóstico, ejecutando el descuento FIFO por fecha de vencimiento y creando un detalle independiente por cada recepción afectada sin promediar precios.  
**Test Independiente**: Petición `POST /api/v1/sanitary/consumos` con `diagnosticoId` y cantidad válida retorna `201 Created` con el consumo y sus detalles. El inventario queda descontado, el evento se persiste en `san_outbox` y la auditoría registrada.

#### Tests para User Story 1
- [ ] **T018** `[P]` `[US1]` Test de contrato: `POST /api/v1/sanitary/consumos` con dosis cubierta por una sola recepción retorna HTTP 201 Created con un único `DetalleConsumoResponse` — `ConsumoMedicamentoControllerTest.java`.
- [ ] **T019** `[P]` `[US1]` Test de contrato: `POST` con dosis que supera el saldo de la primera recepción retorna HTTP 201 Created con múltiples detalles ordenados por vencimiento ascendente — `ConsumoMedicamentoControllerTest.java`.
- [ ] **T020** `[P]` `[US1]` Test de contrato: `POST` con stock total disponible menor al solicitado retorna HTTP 409 Conflict (`StockInsuficienteException`) sin realizar descuentos parciales — `ConsumoMedicamentoControllerTest.java`.
- [ ] **T021** `[P]` `[US1]` Test de contrato: `POST` con unidad no convertible a la unidad base del medicamento retorna HTTP 400 Bad Request (`UnidadIncompatibleException`) — `ConsumoMedicamentoControllerTest.java`.
- [ ] **T022** `[P]` `[US1]` Test de contrato: `POST` con `diagnosticoId` inexistente retorna HTTP 404 Not Found (`DiagnosticoNotFoundException`) — `ConsumoMedicamentoControllerTest.java`.
- [ ] **T023** `[P]` `[US1]` Test de contrato: `POST` sobre diagnóstico perteneciente a rama mortal (`requiereSacrificioSanitario == true`, sin medicación) retorna HTTP 409 Conflict (`DiagnosticoSinMedicacionException`) — `ConsumoMedicamentoControllerTest.java`.
- [ ] **T024** `[P]` `[US1]` Test de contrato: `POST` sin rol `VETERINARIO` retorna HTTP 403 Forbidden — `ConsumoMedicamentoControllerTest.java`.
- [ ] **T025** `[P]` `[US1]` Test unitario de `RegistrarConsumoUseCase` verificando:
  - Derivación automática de `loteId`, `galponId`, `medicamentoId`.
  - Normalización exacta de cantidades mediante `ConversionUnidadService`.
  - Distribución FIFO estricta en detalles separados sin promediar precios.
  - Aborto completo ante stock insuficiente.
- [ ] **T026** `[P]` `[US1]` Test de integración con Testcontainers (PostgreSQL JDBC): Confirmar atomicidad transaccional con `@Transactional` (si falla auditoría u outbox, el consumo y los descuentos de bodega se revierten completamente).

#### Implementación de User Story 1
- [ ] **T027** `[US1]` Crear DTOs de entrada y salida: `RegistrarConsumoRequest.java` (`diagnosticoId`, `cantidadIngresada`, `unidadIngresada`, `fechaAplicacion`, validaciones Jakarta `@NotNull`, `@Positive`, `@NotBlank`) y `ConsumoMedicamentoResponse.java` conteniendo la lista de `DetalleConsumoResponse.java`.
- [ ] **T028** `[US1]` Definir contrato de evento `ConsumoMedicamentoRegistradoIntegrationEvent.java` con schema JSON normalizado (`eventId`, `aggregateId`, `loteId`, `galponId`, `fechaAplicacion`, `medicamentoId`, `cantidadTotalBase`, `detalles[]` con `recepcionId`, `cantidadDescontada`, `precioHistoricoUnidadBase`, `moneda`, `occurredOn`).
- [ ] **T029** `[US1]` Implementar `DiagnosticoQueryAdapter` consultando el diagnóstico vigente y extrayendo de forma segura `loteId`, `galponId`, `medicacionId` y `medicamentoId`.
- [ ] **T030** `[US1]` Implementar `MedicamentoQueryAdapter` obteniendo la unidad base del producto farmacéutico desde el catálogo del `Spec 008`.
- [ ] **T031** `[US1]` Implementar `RecepcionQueryAdapter`:
  - `listarDisponiblesFIFO(medicamentoId)`: Consulta recepciones con `saldo > 0` ordenadas por `fecha_vencimiento ASC`.
  - `descontarExistencias(descuentos)`: Ejecuta actualizaciones atómicas `UPDATE san_recepciones SET saldo = saldo - ? WHERE id = ? AND saldo >= ?` verificando el número exacto de filas afectadas.
- [ ] **T032** `[US1]` Implementar `RegistrarConsumoUseCase.java` en `application/consumo/`:
  - Validar claim de rol `VETERINARIO`.
  - Consultar `DiagnosticoQueryPort.obtenerPorId(diagnosticoId)`; abortar si no existe (`DiagnosticoNotFoundException`).
  - Verificar que el diagnóstico no pertenezca a la rama de sacrificio mortal (`DiagnosticoSinMedicacionException`).
  - Obtener `medicamentoId` y `unidadBase` correspondientes.
  - Normalizar la cantidad ingresada a la unidad base mediante `ConversionUnidadService`; abortar si hay incompatibilidad dimensional (`UnidadIncompatibleException`).
  - Consultar `RecepcionQueryPort.listarDisponiblesFIFO(medicamentoId)` y computar la partición FIFO de recepciones.
  - Si la sumatoria total disponible es menor que la cantidad normalizada, lanzar `StockInsuficienteException` (sin aplicar descuentos parciales).
  - Instanciar aggregate root `ConsumoMedicamento.registrar(...)` asignando la colección inmutable de `DetalleConsumo` con su respectivo precio histórico individual.
  - Ejecutar `@Transactional`: persistir consumo y detalles, ejecutar `RecepcionQueryPort.descontarExistencias(...)`, encolar `ConsumoMedicamentoRegistradoIntegrationEvent` en `san_outbox` y registrar firma médica en `san_auditoria`.
  - Retornar `ConsumoMedicamentoResponse`.
- [ ] **T033** `[US1]` Implementar `ConsultarConsumoUseCase.java` para recuperar un consumo con su desglose de detalles por su UUID.
- [ ] **T034** `[US1]` Implementar endpoints `POST /api/v1/sanitary/consumos` y `GET /api/v1/sanitary/consumos/{id}` en `ConsumoMedicamentoController.java`.

> **Checkpoint**: US1 completamente funcional — registro y descuento FIFO operativos, atómicos y auditados.

---

### Phase 4: User Story 2 – Idempotencia, Concurrencia y Prevención de Duplicados (Priority: P2)
**Propósito**: Asegurar que el registro del consumo sea estrictamente idempotente y atómico, previniendo descuentos duplicados de inventario ante reintentos de red o intentos concurrentes de dos veterinarios.  
**Test Independiente**: Enviar el mismo comando dos veces con la misma cabecera `X-Idempotency-Key` retorna la respuesta original sin descuentos adicionales en bodega ni duplicación de detalles.

#### Tests para User Story 2
- [ ] **T035** `[P]` `[US2]` Test de contrato: `POST /api/v1/sanitary/consumos` con `X-Idempotency-Key` ya procesada retorna la respuesta original sin alterar los saldos de `san_recepciones` — `ConsumoMedicamentoControllerTest.java`.
- [ ] **T036** `[P]` `[US2]` Test de integración con Testcontainers: verificar que la ejecución concurrente de dos consumos sobre la misma recepción provoca colisión optimista (`ConsumoConcurrenciaException` o `DataIntegrityViolationException`) en la segunda transacción.
- [ ] **T037** `[P]` `[US2]` Test de integración con Testcontainers: verificar que el bloqueo pesimista en base de datos sobre las recepciones previene sobreventas de stock farmacéutico cuando dos transacciones compiten por el mismo saldo.

#### Implementación de User Story 2
- [ ] **T038** `[US2]` Configurar `IdempotencyFilter` para almacenar en caché la respuesta del endpoint `POST /api/v1/sanitary/consumos` mapeada a `X-Idempotency-Key` (retención de 24 horas).
- [ ] **T039** `[US2]` Añadir anotación `@Version` en `ConsumoMedicamentoEntity.java` para control de concurrencia optimista.
- [ ] **T040** `[US2]` En `RecepcionQueryAdapter.descontarExistencias(...)`, aplicar sentencia `SELECT ... FOR UPDATE` sobre las recepciones afectadas durante la transacción, asegurando consistencia inmediata del saldo en bodega.
- [ ] **T041** `[US2]` Implementar `ListarConsumosPorDiagnosticoUseCase.java` y endpoint `GET /api/v1/sanitary/consumos` en `ConsumoMedicamentoController.java` con paginación y filtros por diagnóstico, lote, medicamento y fechas.

> **Checkpoint**: US1 y US2 funcionales — registro, idempotencia, concurrencia y consulta de consumos plenamente operativos.

---

### Phase 5: Integridad Transaccional, Outbox Relay e Inmutabilidad (Priority: P3)
**Propósito**: Asegurar que los consumos no se puedan eliminar físicamente (`DELETE`), despachar eventos hacia Kafka desde la tabla Outbox de forma resiliente y garantizar la entrega al Módulo 3.  
**Test Independiente**: Petición `DELETE /api/v1/sanitary/consumos/{id}` retorna `405 Method Not Allowed`. Los eventos en `san_outbox` en estado `PENDING` se publican en Kafka y cambian a `PROCESSED`.

#### Tests para User Story 3
- [ ] **T042** `[P]` `[US3]` Test de contrato y seguridad: `DELETE /api/v1/sanitary/consumos/{id}` retorna HTTP 405 Method Not Allowed — `ConsumoMedicamentoControllerTest.java`.
- [ ] **T043** `[P]` `[US3]` Test de base de datos: verificar ausencia de sentencias o métodos de borrado físico directo (`DELETE`) en repositorios y adaptadores.
- [ ] **T044** `[P]` `[US3]` Test de integración Outbox Relay con Testcontainers (Kafka): verificar lectura por lotes de `san_outbox` y publicación efectiva en el tópico `sanitary.medication.consumed.v1`.
- [ ] **T045** `[P]` `[US3]` Test de idempotencia del Outbox Relay: comprobar que ejecuciones repetidas del worker no duplican publicaciones gracias al uso de `SELECT ... FOR UPDATE SKIP LOCKED` y actualización a `PROCESSED`.

#### Implementación de User Story 3
- [ ] **T046** `[US3]` Configurar `SecurityConfig.java` bloqueando explícitamente cualquier verbo `DELETE` sobre rutas `/api/v1/sanitary/**` (`FR-013`).
- [ ] **T047** `[US3]` Implementar `KafkaEventPublisherAdapter.java` publicando mensajes tipados hacia el tópico `sanitary.medication.consumed.v1` mediante `StreamBridge` o `KafkaTemplate`.
- [ ] **T048** `[US3]` Implementar `OutboxRelayScheduler.java`:
  - Polling periódico con `@Scheduled(fixedDelay = 2000)` sobre `san_outbox` donde `status = 'PENDING'` usando `SELECT ... FOR UPDATE SKIP LOCKED`.
  - Despacho a Kafka mediante `KafkaEventPublisherAdapter`.
  - Actualización atómica de estado a `PROCESSED` con marca temporal `processed_at`.
  - Manejo de reintentos y marcado a `FAILED` si supera 5 reintentos con backoff exponencial.
- [ ] **T049** `[US3]` Documentar contratos de mensajería asíncrona mediante especificación AsyncAPI 3.0 en `docs/asyncapi/sanitary-events.yml`.
- [ ] **T050** `[US3]` Configurar especificación OpenAPI 3.0 (Swagger UI) exponiendo documentación de endpoints REST.

> **Checkpoint**: Sistema de mensajería Outbox confiable, tolerancia a caídas de red y blindaje absoluto contra borrado físico.

---

### Phase 6: Polish & Cross-Cutting Concerns
**Propósito**: Asegurar la calidad técnica, observabilidad, rendimiento y correspondencia con las pantallas del sistema.

- [ ] **T051** Configurar logging estructurado en formato JSON incorporando `traceId`, `spanId` y `correlationId` vía MDC de Slf4j.
- [ ] **T052** Exponer métricas Prometheus con Micrometer (`sanitary_consumo_registrado_total`, `sanitary_descuento_recepciones_total`, `sanitary_outbox_lag_seconds`).
- [ ] **T053** Implementar pruebas de carga con Gatling/k6 validando latencia < 250 ms bajo concurrencia sostenida de 200 req/s.
- [ ] **T054** Auditoría de dependencias: validar mediante ArchUnit que el paquete `domain/` mantenga cero imports de Spring, JPA/Hibernate, Jackson o librerías externas.
- [ ] **T055** Verificar correspondencia campo a campo entre el DTO de respuesta y la vista Figma importada (`docs/prototype/gestion-sanitaria/consumo-medicamento/`).

---

## 5. Dependencies & Execution Order

### Phase Dependencies
- **Phase 1 (Setup)**: Sin dependencias — inicia de inmediato.
- **Phase 2 (Foundational)**: Requiere Fase 1 completa — **bloquea todas las user stories**.
- **Phase 3 (User Story 1)**: Requiere Fase 2 completada (bloqueante).
- **Phase 4 (User Story 2)**: Requiere Fase 3 (necesita existir la lógica de registro base).
- **Phase 5 (US3 / Outbox + Inmutabilidad)**: Puede desarrollarse en paralelo con Fase 4 tras concluir Fase 3.
- **Phase 6 (Polish)**: Requiere todas las fases funcionales implementadas.

### User Story Dependencies
- **US1 (Registro + FIFO)**: Depende de infraestructura base y de los puertos cross-context (`DiagnosticoQueryPort`, `MedicamentoQueryPort`, `RecepcionQueryPort`).
- **US2 (Idempotencia / Concurrencia)**: Depende de US1 (la entidad debe existir para validar duplicados y colisiones).
- **US3 (Outbox & Inmutabilidad)**: Transversal a US1 y US2; procesa los eventos generados por ambas historias.

---

## 6. Traceability Matrix (Spec 013 vs Implementation Plan)

| Requerimiento Spec 013 | Tarea(s) en Implementation Plan | Componente Técnico Responsable |
| :--- | :--- | :--- |
| **FR-001** (Exclusivo Veterinario) | **T016**, **T024**, **T032** | `RoleValidationFilter`, `RegistrarConsumoUseCase` |
| **FR-002** (Asociación a diagnosticoId vigente) | **T011**, **T029**, **T032** | `DiagnosticoQueryPort`, `DiagnosticoQueryAdapter` |
| **FR-003** (Derivación automática de lote/galpón/medicamento) | **T008**, **T009**, **T032** | `DiagnosticoSnapshot`, `ConsumoMedicamento.registrar()` |
| **FR-004** (Medicamento no sustituible) | **T009**, **T032** | Invariante de dominio en `ConsumoMedicamento` |
| **FR-005** (Cantidad, unidad, normalización, fecha) | **T008**, **T009**, **T027** | `CantidadFisica`, `UnidadBase`, DTO `RegistrarConsumoRequest` |
| **FR-006** (FIFO por vencimiento en bodega) | **T011**, **T031**, **T032** | `RecepcionQueryPort.listarDisponiblesFIFO()` |
| **FR-007** (Un detalle por recepción, sin promedios) | **T009**, **T031**, **T032** | `DetalleConsumo`, `ConsumoMedicamento.registrar()` |
| **FR-008** (Rechazo total si stock insuficiente) | **T010**, **T020**, **T032** | `StockInsuficienteException`, validación pre-descuento |
| **FR-009** (Validación de unidades compatibles) | **T008**, **T017**, **T021** | `ConversionUnidadService`, `UnidadConversionConfiguration` |
| **FR-010** (Evento Outbox Kafka hacia Módulo 3) | **T028**, **T032**, **T048** | `san_outbox`, `OutboxRelayScheduler`, tópico Kafka |
| **FR-011** (Evento con desglose completo) | **T028**, **T032** | `ConsumoMedicamentoRegistradoIntegrationEvent` |
| **FR-012** (Trazabilidad auditoría) | **T007**, **T013**, **T032** | `san_auditoria`, `AuditoriaSanitariaPort` |
| **FR-013** (Prohibido borrado físico) | **T042**, **T043**, **T046** | `SecurityConfig`, revocación de sentencias SQL `DELETE` |
| **FR-014** (Concurrencia e Idempotencia) | **T015**, **T035**, **T039**, **T040** | `IdempotencyFilter`, `@Version`, `SELECT ... FOR UPDATE` |
| **FR-015** (Módulo 2 no calcula costos totales) | **T028**, **T032** | Publicación exclusiva de cantidades y precios crudos |
| **SC-001 a SC-008** (Métricas de éxito) | **T025**, **T026**, **T044**, **T052**, **T053** | Pruebas de integración, Prometheus, Gatling y Jacoco |

---

## 7. Notes

- Cada tarea cuenta con su identificador único `T0xx` para seguimiento en tableros Kanban o Jira.
- Las escrituras que involucran `san_consumos`, `san_detalles_consumo`, `san_recepciones`, `san_outbox` y `san_auditoria` se ejecutan dentro del mismo bloque `@Transactional` imperativo de Spring Data JPA.
- **Algoritmo FIFO**: La selección de recepciones se ejecuta en el adaptador `RecepcionQueryAdapter` filtrando por `medicamento_id` con `saldo > 0` y ordenando por `fecha_vencimiento ASC`. Cada lote físico afectado genera un `DetalleConsumo` independiente.
- **Sin promedios de precios**: Cada detalle almacena la tarifa unitaria de compra histórica de su recepción. El Módulo 3 recibe los datos crudos y asume la responsabilidad exclusiva de liquidar el costo del tratamiento.
- **Bloqueo pesimista en descuentos**: El uso de `SELECT ... FOR UPDATE` sobre las recepciones previene sobreventas concurrentes y asegura consistencia atómica del inventario farmacéutico.
- **Sin descuentos parciales**: Si la sumatoria total disponible en todas las recepciones activas no cubre la cantidad solicitada, el caso de uso aborta lanzando `StockInsuficienteException` antes de mutar cualquier tupla.
- **Normalización de magnitudes**: El servicio `ConversionUnidadService` valida que la unidad ingresada coincida dimensionalmente con la unidad base configurada para el medicamento; unidades no equivalentes se rechazan inmediatamente con `UnidadIncompatibleException`.
- **Desacoplamiento Módulo 2 / Módulo 3**: El Módulo 2 publica el evento `ConsumoMedicamentoRegistradoIntegrationEvent` conteniendo la matriz de detalles (recepciones, cantidades descontadas y precios históricos). El Módulo 3 procesa dicho contrato para liquidar el costo total acumulado del lote en sus propios esquemas contables.
- El uso de **Virtual Threads** (Project Loom) permite manejar la concurrencia de peticiones HTTP de forma eficiente sin la complejidad de la programación reactiva, manteniendo el modelo imperativo de Spring MVC.