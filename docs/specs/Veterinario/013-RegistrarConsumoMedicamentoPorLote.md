# Feature Specification: Registrar consumo de medicamento por lote

**Created**: 2026-09-05  
**Last Updated**: 2026-09-22  
**Spec**: `docs/specs/013-RegistrarConsumoMedicamento.md`  
**Módulo**: 2 – Sanidad y Bioseguridad  
**Actor Principal**: `VETERINARIO`
**Precondición Obligatoria**: Diagnóstico clínico activo con pauta terapéutica vigente (`Spec 011` completado con `requiereSacrificioSanitario == false`)  
**Relación UML / Integración**: Inclusión directa desde el flujo de tratamiento clínico y comunicación asíncrona hacia el **Módulo 3** (Costos de Producción)

---

## 1. Summary

El módulo **Registrar consumo de medicamento por lote** permite al Médico Veterinario asentar la cantidad de medicamento realmente dosificada y aplicada en campo a una parvada que se encuentra bajo prescripción terapéutica activa (`Spec 011`, fundamentado en `Spec 008`).

El sistema obtiene de forma inmutable el medicamento, el galpón y el lote a partir del diagnóstico clínico. Convierte la cantidad suministrada a la unidad base del producto farmacéutico, descuenta las existencias físicas de las recepciones de bodega disponibles mediante el algoritmo **FIFO por fecha de vencimiento** y preserva de forma atómica el precio histórico de cada lote de compra afectado.

**La información resultante se publica de forma garantizada hacia el Módulo 3 (Costos de Producción)** mediante el broker Kafka. **El Módulo 2 no calcula costos contables totales**: su responsabilidad concluye en registrar la trazabilidad biológica del fármaco, descontar el inventario físico y despachar los datos crudos consolidados hacia los módulos correspondientes.

**Restricciones globales mandatorias**:

* 🔒 **Prohibición absoluta de borrado físico**: Ninguna sentencia `DELETE` SQL está permitida sobre consumos, detalles por recepción ni asientos de auditoría.
* 🔒 **Atomicidad e Integridad Transaccional**: El registro del consumo, el desglose de detalles, el descuento de existencias de bodega, la escritura en `san_outbox` y la firma en `san_auditoria` se ejecutan dentro de una única transacción local (`@Transactional`).
* 🔒 **Seguridad y Atribución**: Acceso reservado exclusivamente a usuarios autenticados con rol `VETERINARIO`.
* 🔒 **Concurrencia e Idempotencia**: Control optimista mediante `@Version` sobre el consumo y soporte de cabeceras `X-Idempotency-Key` retenidas durante 24 horas.
* 🔒 **Inmutabilidad de Atribución**: Los identificadores de `loteId`, `galponId`, `medicacionId` y `medicamentoId` son derivados automáticamente del diagnóstico; el sistema bloquea cualquier intento de selección o modificación manual.

---

## 2. User Scenarios & Testing *(mandatory)*

### User Story 1 - Registro y Descuento de Existencias del Fármaco Aplicado (Priority: P1)

**Como** Médico Veterinario de la granja,  
**Quiero** registrar la cantidad de medicamento administrada en campo a un lote bajo tratamiento terapéutico,  
**Para** descontar con exactitud las existencias físicas en bodega según su fecha de vencimiento y notificar al Módulo 3 los insumos aplicados junto con sus precios históricos de compra.

**Why this priority**: Es el acto que materializa la salida física del medicamento de bodega hacia las aves. Sin este registro, el stock de inventario permanece ficticio, se falsea el kardex de farmacia y el Módulo 3 no puede valorizar el costo real de crianza del lote.

**Independent Test**:
* **Caso A (Consumo abastecido por una sola recepción)**: `POST /api/v1/sanitary/consumos` con `diagnosticoId`, cantidad `500` y unidad `ml`. El sistema valida la conversión a la unidad base, descuenta la recepción con vencimiento más próximo, crea un único registro de detalle con su precio histórico y despacha el evento al outbox.
* **Caso B (Consumo fraccionado en múltiples recepciones - FIFO)**: `POST` con cantidad que excede el stock de la primera recepción. El sistema agota la recepción más próxima a vencer y toma el remanente de la siguiente, creando dos detalles de consumo con sus respectivos precios históricos sin promediar valores.

#### Acceptance Scenarios

1. **Scenario: Consumo exitoso cubierto por una única recepción de inventario**
   * **Given** que existe un diagnóstico clínico activo (`Spec 011`) con una pauta terapéutica (`Spec 008`) que define el medicamento
   * **And** la recepción de inventario con vencimiento más próximo posee existencias suficientes para cubrir la dosis
   * **When** el Veterinario ingresa la cantidad aplicada, la unidad de medida y la fecha de aplicación
   * **Then** el sistema deriva automáticamente `loteId`, `galponId`, `medicacionId` y `medicamentoId` desde el expediente clínico
   * **And** convierte la cantidad ingresada a la unidad base del inventario farmacéutico
   * **And** descuenta las existencias físicas de dicha recepción
   * **And** crea una fila en `san_detalles_consumo` registrando `recepcionId`, la cantidad descontada y el precio histórico unitario de compra
   * **And** persiste atómicamente el evento `ConsumoMedicamentoRegistradoIntegrationEvent` en `san_outbox` bajo el tópico `sanitary.medication.consumed.v1`
   * **And** registra el asiento inmutable en `san_auditoria` con firma profesional del Veterinario

2. **Scenario: Consumo exitoso distribuido en múltiples recepciones mediante FIFO**
   * **Given** que la cantidad total a suministrar excede el saldo de la recepción con vencimiento más próximo
   * **And** la suma total de existencias disponibles en otras recepciones vigentes del mismo medicamento cubre la dosis requerida
   * **When** el Veterinario confirma el consumo
   * **Then** el sistema agota primero la recepción más próxima a caducar
   * **And** descuenta el saldo restante de las recepciones subsiguientes ordenadas por fecha de vencimiento
   * **And** crea un registro independiente en `san_detalles_consumo` por cada recepción afectada
   * **And** conserva en cada detalle su propio precio histórico de compra sin aplicar promedios ponderados
   * **And** despacha el evento hacia el Módulo 3 conteniendo el desglose íntegro por recepción

3. **Scenario: Rechazo total por existencias insuficientes en bodega**
   * **Given** que la sumatoria de existencias en todas las recepciones activas del medicamento es inferior a la cantidad solicitada
   * **When** el Veterinario intenta registrar el consumo
   * **Then** el sistema interrumpe la operación, responde con HTTP `409 Conflict` (`StockInsuficienteException`)
   * **And** no aplica descuentos parciales en ninguna recepción ni persiste filas en `san_consumos`

4. **Scenario: Rechazo por unidad de medida incompatible con la unidad base**
   * **Given** que el medicamento tiene configurada una unidad base dimensionalmente fija (ej. unidades físicas o litros)
   * **When** el Veterinario intenta ingresar una cantidad expresada en una magnitud física incompatible (ej. gramos para un producto líquido sin factor de densidad)
   * **Then** el sistema aborta el procesamiento con HTTP `400 Bad Request` (`UnidadIncompatibleException`)
   * **And** no altera el inventario

5. **Scenario: Rechazo por diagnóstico inexistente o perteneciente a rama mortal**
   * **Given** un identificador `diagnosticoId` que no existe o cuyo diagnóstico dictaminó una patología letal (`requiereSacrificioSanitario == true`, sin medicación asociada)[cite: 4]
   * **When** el Veterinario intenta enviar una orden de consumo
   * **Then** el sistema bloquea la acción respondiendo con HTTP `404 Not Found` (`DiagnosticoNotFoundException`) o HTTP `409 Conflict` (`DiagnosticoSinMedicacionException`)

6. **Scenario: Denegación de acceso a usuarios sin rol veterinario**
   * **Given** un usuario autenticado con rol `TRABAJADOR` o `ADMINISTRADOR`
   * **When** intenta emitir una orden de consumo de medicamentos
   * **Then** el sistema responde con HTTP `403 Forbidden` y no genera movimientos de bodega

---

### User Story 2 - Idempotencia, Concurrencia y Blindaje Transaccional (Priority: P2)

**Como** auditor de inocuidad y control interno de la granja,  
**Quiero** garantizar que el consumo de medicamentos sea estrictamente atómico e idempotente,  
**Para** evitar duplicidades de salidas en el inventario farmacéutico ante reintentos de red o colisiones concurrentes.

**Why this priority**: Un descuento duplicado desfasaría el inventario físico de medicamentos, generaría pérdidas aparentes de insumos y duplicaría indebidamente el costo imputado al lote en el Módulo 3.

**Independent Test**: Se envía el mismo comando de consumo dos veces consecutivas bajo la misma cabecera `X-Idempotency-Key`. El sistema retorna la respuesta original almacenada en caché sin ejecutar un segundo descuento sobre las recepciones de bodega ni registrar mensajes duplicados en el broker.

#### Acceptance Scenarios

1. **Scenario: Reintento transparente bajo clave técnica de idempotencia**
   * **Given** un consumo registrado exitosamente bajo la cabecera `X-Idempotency-Key: IDEMP-CONS-7701`
   * **When** el cliente HTTP retransmite la misma solicitud debido a un timeout transitorio de la red
   * **Then** el filtro de idempotencia intercepta la petición
   * **And** devuelve la respuesta almacenada (HTTP 201 Created con el payload original)
   * **And** no altera nuevamente los saldos de las recepciones ni encola eventos adicionales en el outbox

2. **Scenario: Rechazo por colisión de concurrencia optimista**
   * **Given** dos veterinarios procesando consumos sobre recepciones concurrentes con saldos limitados
   * **When** ambos confirman la transacción casi en el mismo milisegundo
   * **Then** la segunda transacción colisiona por versión de concurrencia (`OptimisticLockException` / `ConsumoConcurrenciaException`)
   * **And** se exige al usuario recargar las existencias vigentes antes de reintentar

---

### Edge Cases

* **Caducidad sobrevenida durante el diligenciamiento**: Si una recepción vence o es bloqueada administrativamente mientras el Veterinario diligencia la cantidad, el sistema revalida el inventario inmediatamente antes de abrir el `@Transactional`; si el remanente no cubre la dosis, aborta con `StockInsuficienteException` sin aplicar descuentos parciales.
* **Preservación estricta de precios históricos**: Si el consumo se abastece de dos recepciones compradas a tarifas distintas (ej. Lote A a \$12.50/ml y Lote B a \$14.00/ml), el Módulo 2 genera dos detalles independientes sin promediar precios. El Módulo 3 recibe ambos valores crudos para liquidar el costo real.
* **Indisponibilidad del Módulo 3 o caída de Kafka**: Gracias al Transactional Outbox, la transacción local de base de datos (`san_consumos` + `san_detalles_consumo` + descuento de existencias + `san_outbox` + `san_auditoria`) se ejecuta de manera indivisible. El scheduler de relevo reintentará la publicación a Kafka cuando el broker se reanude.

---

## 3. Requirements *(mandatory)*

### Functional Requirements

| ID | Requerimiento Funcional | Justificación Técnica / Trazabilidad |
| :--- | :--- | :--- |
| **FR-001** | El registro de consumos de medicamentos DEBE estar restringido exclusivamente a usuarios autenticados con rol `VETERINARIO`. | Restricción legal sobre prescripción y manejo de fármacos. |
| **FR-002** | Cada consumo DEBE asociarse formalmente a un único `diagnosticoId` activo que posea una pauta de tratamiento vigente (`Spec 011`). | Precondición clínica obligatoria. |
| **FR-003** | El sistema DEBE derivar de forma automática desde el diagnóstico: `loteId`, `galponId`, `medicacionId` y `medicamentoId`, bloqueando cualquier ingreso manual de estos datos. | Consistencia de atribución y trazabilidad de origen. |
| **FR-004** | El medicamento consumido DEBE coincidir estrictamente con el principio activo y presentación de la medicación del diagnóstico; se prohíbe cualquier sustitución no prescrita. | Rigor terapéutico veterinario. |
| **FR-005** | Cada consumo DEBE registrar: cantidad ingresada, unidad de medida ingresada, cantidad normalizada a unidad base, unidad base y fecha real de aplicación en campo. | Estandarización de magnitudes físicas. |
| **FR-006** | El descuento de existencias DEBE ejecutarse mediante algoritmo FIFO estricto priorizando la recepción con fecha de vencimiento más próxima y distribuyendo el saldo si una sola no basta. | Buenas prácticas de almacenamiento y control de caducidades farmacéuticas. |
| **FR-007** | Cada consumo DEBE generar una fila en `san_detalles_consumo` por cada recepción afectada, registrando: `recepcionId`, cantidad descontada en unidad base, precio histórico de compra y moneda. | Trazabilidad exacta sin distorsión por promedios contables. |
| **FR-008** | Si la suma total de existencias disponibles en bodega no cubre el 100% de la cantidad requerida, el sistema DEBE rechazar la transacción sin realizar descuentos parciales. | Integridad transaccional del inventario. |
| **FR-009** | El sistema DEBE validar factores de conversión métrica y rechazar unidades incompatibles con la unidad base del medicamento con HTTP `400 Bad Request`. | Prevención de errores dimensionales. |
| **FR-010** | Al confirmarse el guardado, el sistema DEBE persistir de forma atómica en `san_outbox` el evento `ConsumoMedicamentoRegistradoIntegrationEvent` bajo el tópico `sanitary.medication.consumed.v1`. | Entrega garantizada mediante Transactional Outbox. |
| **FR-011** | El evento publicado DEBE contener: `loteId`, `galponId`, `fechaAplicacion`, `medicamentoId`, `cantidadTotalBase` y el desglose de cada detalle con `recepcionId`, `cantidadDescontada` y `precioHistoricoUnidadBase`. | Información cruda requerida por el Módulo 3 para el costeo de lotes. |
| **FR-012** | Cada registro DEBE asentar una traza inmutable en `san_auditoria` con usuario, matrícula, `consumoId`, `diagnosticoId`, `loteId` y cantidad total consumida. | Trazabilidad legal zoosanitaria. |
| **FR-013** | Queda ESTRICTAMENTE PROHIBIDO el borrado físico (`DELETE` en SQL) sobre las tablas `san_consumos`, `san_detalles_consumo` y `san_auditoria`. | Resguardo inmutable de movimientos de insumos. |
| **FR-014** | El sistema DEBE implementar control de concurrencia optimista (`@Version`) y admitir cabeceras `X-Idempotency-Key` retenidas durante 24 horas. | Prevención de descuentos duplicados por caídas de red. |
| **FR-015** | El Módulo 2 NO DEBE calcular costos totales ni valorizaciones contables; su alcance termina en registrar las cantidades físicas y publicar los datos crudos al Módulo 3. | Desacoplamiento y separación de Bounded Contexts. |

---

### Key Entities

```text
+---------------------------------------------------------------------------------+
|                                 <<Aggregate Root>>                              |
|                            ConsumoMedicamentoPorLote                            |
+---------------------------------------------------------------------------------+
| - id: UUID                                                                      |
| - diagnosticoId: UUID                                (Ref. Spec 011)            |
| - loteId: UUID                                       (Ref. Externa Módulo 1)    |
| - galponId: UUID                                     (Ref. Externa Módulo 1)    |
| - medicamentoId: UUID                                (Derivado de Spec 008)     |
| - veterinarioId: UUID                                                           |
| - cantidadIngresada: BigDecimal                                                 |
| - unidadIngresada: String                                                       |
| - cantidadNormalizada: BigDecimal                    (En unidad base)           |
| - unidadBase: String                                                            |
| - fechaAplicacion: Instant                                                      |
| - fechaRegistro: Instant                             (UTC al confirmar)         |
| - version: Integer                                   (@Version Optimistic Lock) |
+---------------------------------------------------------------------------------+
                                   │
                                   │ 1..N
                                   ▼
+---------------------------------------------------------------------------------+
|                                 DetalleConsumo                                  |
+---------------------------------------------------------------------------------+
| - id: UUID                                                                      |
| - consumoId: UUID                                    (FK al agregado)           |
| - recepcionId: UUID                                  (Ref. Inventario Bodega)   |
| - cantidadDescontada: BigDecimal                     (En unidad base)           |
| - precioHistoricoUnidadBase: BigDecimal              (Tarifa compra del lote)   |
| - moneda: String                                     (COP / USD)                |
+---------------------------------------------------------------------------------+
```

* **ConsumoMedicamentoPorLote** *(Aggregate Root)*: Entidad raíz que modela el acto clínico de aplicación del fármaco en campo sobre la parvada.
* **DetalleConsumo**: Entidad hija inmutable que formaliza el desglose físico por lote de compra/recepción de bodega, preservando la tarifa unitaria de adquisición original.
* **Diagnóstico**: Expediente clínico del `Spec 011` que provee de forma inmutable el `galponId`, `loteId` y la prescripción terapéutica.
* **Medicación y Medicamento**: Catálogos nosológicos y de bodega (`Spec 008`) que definen el principio activo, presentación y factor métrico hacia la unidad base.
* **Recepción de Inventario**: Lote físico almacenado en bodega que expone existencias disponibles, fecha de caducidad y precio de adquisición.
* **ConsumoMedicamentoRegistradoIntegrationEvent**: Contrato tipado despachado a Kafka con destino al Módulo 3.
* **Auditoría Sanitaria (`san_auditoria`)**: Bitácora inmutable append-only de responsabilidad profesional veterinaria.
* **Transactional Outbox (`san_outbox`)**: Tabla transaccional relacional para despacho garantizado sin dependencias 2PC.

---

## 4. Success Criteria *(mandatory)*

### Measurable Outcomes

| ID | Criterio de Éxito | Métrica Objetivo | Validación Técnica |
| :--- | :--- | :--- | :--- |
| **SC-001** | El 100% de los consumos toma de forma automática los datos de galpón, lote y medicamento desde el diagnóstico sin inputs manuales. | Cero discrepancias de origen | Verificación de invariantes en `RegistrarConsumoUseCase`. |
| **SC-002** | El 100% de los consumos con múltiples recepciones conserva el desglose exacto sin promediar precios de compra. | Cero promedios ponderados | Test de persistencia de filas en `san_detalles_consumo`. |
| **SC-003** | El 100% de las transacciones rechazadas por falta de stock deja intactas las existencias de bodega. | Cero descuentos huérfanos | Test de rollback transaccional con Testcontainers. |
| **SC-004** | El 100% de los intentos de registro ejecutados por roles distintos a `VETERINARIO` son bloqueados. | Cero accesos no autorizados | Prueba de seguridad HTTP 403 Forbidden. |
| **SC-005** | La latencia del sistema para registrar el consumo y persistir en outbox es inferior a 250 milisegundos. | Latencia < 250 ms | Pruebas de rendimiento con Gatling bajo carga. |
| **SC-006** | Cero duplicidad (0%) en descuentos de bodega o eventos ante reintentos de red. | Cero duplicados | Validación con `X-Idempotency-Key` y `@Version`. |
| **SC-007** | Cero incidentes (0%) de borrado físico (`DELETE` SQL) sobre consumos, detalles o auditorías. | 100% de inmutabilidad | Verificación de permisos y triggers en PostgreSQL. |
| **SC-008** | El 100% de los eventos dirigidos al Módulo 3 se persiste en `san_outbox` dentro de la misma transacción local. | Atomicidad total | Prueba de integración de Transactional Outbox con Testcontainers. |

---

## 5. Dependencies & Cross-References

* **`Spec 008 – Registrar medicación`**: Provee el catálogo terapéutico que parametriza el fármaco y su dosimetría estándar.
* **`Spec 011 – Diagnosticar galpón`**: Precondición directa. El diagnóstico clínico contiene el tratamiento aprobado, el lote y el galpón.
* **Módulo 1 (Control de Galpones y Lotes)**: Bounded Context propietario de las aves y su ubicación física.
* **Módulo 3 (Costos de Producción)**: Receptor asíncrono del evento `ConsumoMedicamentoRegistradoIntegrationEvent`. Es el **único responsable** de calcular el costo total de crianza multiplicando cada cantidad por su precio histórico.

---

## 6. Notes

* **Trazabilidad Bidireccional**: Cada requerimiento `FR-001` a `FR-015` mapea punto a punto contra una tarea del plan técnico (`T0xx`) y contra una clase o invariante en la arquitectura hexagonal.
* **Separación de Responsabilidades**: El Módulo 2 no gestiona cuentas contables ni libros de costos; su misión se circunscribe a la sanidad animal, asegurando que las aves reciban la dosis prescrita y reflejando la salida física en bodega.
* **Validación de Existencias en Dos Tiempos**: El sistema lee el saldo disponible al renderizar el formulario y vuelve a verificar el saldo bloqueando la tupla (`SELECT ... FOR UPDATE`) dentro de la transacción final para evitar sobreventas concurrentes entre distintos galpones.
* **Notificación al Cliente Frontend**: El payload `ConsumoMedicamentoResponse` devuelve el detalle de las recepciones afectadas con sus fechas de vencimiento y precios históricos, permitiendo al veterinario revisar el desglose antes de la confirmación definitiva.