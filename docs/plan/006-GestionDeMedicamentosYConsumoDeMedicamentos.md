# Implementation Plan: Gestión de Medicamentos y Consumo de Medicamentos

**Date**: 02/10/2026

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [008-RegistrarMedicacion.md](../specs/008-RegistrarMedicacion.md)
- [015-ConsultarMedicacionPorGalponPorDia.md](../specs/015-ConsultarMedicacionPorGalponPorDia.md)
- [013-RegistrarConsumoMedicamento.md](../specs/013-RegistrarConsumoMedicamento.md)

## Summary

Implementar el registro y edición de medicaciones, la consulta diaria de tratamientos por galpón y el registro del consumo real de medicamentos. El plan permite que el veterinario defina una medicación asociada a una enfermedad y a un medicamento del inventario, que el trabajador consulte las indicaciones vigentes y que el veterinario registre la cantidad realmente aplicada.

La medicación prescrita y el consumo real son conceptos diferentes. Registrar una medicación no descuenta inventario. Registrar un consumo sí descuenta existencias, distribuye la cantidad entre recepciones disponibles y conserva el precio neto histórico de cada recepción para el costeo posterior del Módulo 3.

Galpon y Lote continúan siendo entidades propietarias del Módulo 1. Diagnóstico y enfermedad pertenecen a las capacidades sanitarias relacionadas. Medicamento, recepción, existencias, unidad base y precio de compra pertenecen al inventario del Plan 002. Este plan consulta esas capacidades mediante puertos internos y no crea copias autoritativas.

## Technical Context

**Performance Goals**: El 95 % de los registros de medicación y consumo queda disponible en máximo 2 segundos después de su confirmación. Las consultas diarias responden en máximo 2 segundos sin realizar escrituras. La distribución de un consumo entre recepciones se ejecuta en una única transacción local.

**Constraints**: Solo el veterinario registra o edita medicaciones y registra consumos. La consulta diaria es de solo lectura. La medicación debe referenciar una enfermedad disponible y un medicamento existente en inventario. El consumo obtiene diagnóstico, lote, galpón, medicación y medicamento desde el diagnóstico; el veterinario no puede sustituir manualmente el medicamento durante el consumo.

**Scale/Scope**: Cinco historias de usuario, catálogo de medicaciones, consultas diarias individuales y consolidadas, normalización de unidades, distribución FEFO entre recepciones, descuento de inventario, detalle histórico por recepción, eventos y adaptadores internos.

**Dependencias funcionales**: Enfermedades y diagnósticos del Plan 005; Galpon y Lote del Módulo 1; medicamento, recepción, existencias, unidad base y precio del Plan 002; cálculo de costos del Módulo 3; autenticación, actor, reloj y errores de General.md.

### Decisiones específicas

1. **Medicación**: Medicacion es una entidad propia con enfermedadId, medicamentoId, dosis, diasTotales y descripcion. Todos son obligatorios. Medicamento y enfermedad deben volver a validarse al confirmar.
2. **Edición histórica**: Editar una medicación solo afecta diagnósticos futuros. Un diagnóstico conserva la dosis, duración, medicamento y demás valores utilizados al registrarse.
3. **Inventario**: Registrar o editar una medicación no cambia existencias. Solo RegistrarConsumoUseCase descuenta recepciones.
4. **Consumo**: ConsumoMedicamento pertenece a un diagnóstico y obtiene de él galponId, loteId y medicamentoId. Esos datos no se reciben como selección editable del cliente.
5. **Unidades**: El consumo acepta una unidad compatible con la unidad base del medicamento. La normalización debe utilizar el contrato del inventario; magnitudes incompatibles se rechazan.
6. **Recepciones**: El descuento usa primero la recepción con vencimiento más próximo entre las disponibles del mismo medicamento. Si una no alcanza, continúa con las siguientes. Cada porción genera un DetalleConsumo con cantidad, recepción y precio neto histórico por unidad base.
7. **Atomicidad**: Si las existencias dejan de ser suficientes, una recepción vence o se anula antes de confirmar, se recalcula la distribución. Si no puede cubrirse el total, se rechaza sin descuentos parciales.
8. **Consultas diarias**: La consulta toma la fecha actual del Clock compartido y devuelve los tratamientos vigentes de esa fecha. No modifica diagnóstico, consumo ni inventario.
9. **Galpones y trabajadores**: No se consulta una asignación trabajador-galpón. La consulta individual recibe galponId y el resumen devuelve el universo permitido por el rol.
10. **Cantidad diaria**: La cantidad a aplicar debe provenir de los datos de dosis de la medicación/diagnóstico. Si se requieren calendarios de días alternos, descansos o cantidades variables, esos datos deben agregarse explícitamente al spec 008 antes de implementar el cálculo.
11. **Eventos**: Las escrituras publican eventos después de confirmar la transacción local. Los consumidores son idempotentes por eventId. Las consultas no publican eventos.
12. **Persistencia**: Se crean tablas para medicaciones, consumos y detalles. Las versiones de migración utilizan la siguiente versión disponible del repositorio y no se fijan números que puedan colisionar con el Plan 002.
13. **Consistencia**: La confirmación de consumo vuelve a consultar diagnóstico, medicamento y recepciones dentro de la transacción. No se usan existencias capturadas previamente por la interfaz.
14. **Costeo**: El consumo deja disponibles para Módulo 3 loteId, galponId, fecha, cantidad normalizada, recepción y precio neto histórico. No calcula la liquidación final del lote en este plan.

### Alcance documental

- El Plan 005 consulta MedicacionQueryPort para registrar diagnósticos. El Plan 006 implementa ese puerto y conserva la información histórica que necesita el diagnóstico.
- El Plan 002 es dueño de medicamentos, recepciones, existencias, unidades y precios de compra. Este plan solicita operaciones de lectura y descuento mediante puertos públicos.
- Módulo 3 consume los consumos y detalles para calcular costos. Este plan no modifica precios históricos ni promedia recepciones.
- Los nombres definitivos de eventos y contratos deben acordarse con los consumidores antes de publicar integración.

## Project Structure

### Documentation (this feature)

    docs/
    ├── specs/
    │   ├── 008-RegistrarMedicacion.md
    │   ├── 015-ConsultarMedicacionPorGalponPorDia.md
    │   └── 013-RegistrarConsumoMedicamento.md
    └── plan/
        └── 006-GestionDeMedicamentosYConsumoDeMedicamentos.md

### Source Code (repository root)

    src/main/java/com/avicontrol/
    ├── domain/
    │   ├── model/medicamento/
    │   │   ├── Medicacion.java
    │   │   ├── ConsumoMedicamento.java
    │   │   ├── DetalleConsumo.java
    │   │   └── EstadoConsumo.java
    │   ├── exception/medicamento/
    │   │   ├── MedicacionInvalidaException.java
    │   │   ├── MedicacionNoEncontradaException.java
    │   │   ├── MedicamentoNoDisponibleException.java
    │   │   ├── UnidadIncompatibleException.java
    │   │   ├── ExistenciasInsuficientesException.java
    │   │   └── DiagnosticoNoEncontradoException.java
    │   └── port/out/medicamento/
    │       ├── MedicacionRepositoryPort.java
    │       ├── ConsumoMedicamentoRepositoryPort.java
    │       ├── EnfermedadQueryPort.java
    │       ├── DiagnosticoQueryPort.java
    │       ├── GalponQueryPort.java
    │       ├── LoteQueryPort.java
    │       ├── InventarioMedicamentoPort.java
    │       └── IntegrationEventPublisherPort.java
    ├── application/medicamento/
    │   ├── RegistrarMedicacionUseCase.java
    │   ├── EditarMedicacionUseCase.java
    │   ├── ConsultarMedicacionDiariaUseCase.java
    │   ├── ConsultarResumenMedicacionDiariaUseCase.java
    │   ├── RegistrarConsumoMedicamentoUseCase.java
    │   └── result/
    │       ├── MedicacionResult.java
    │       ├── MedicacionDiariaResult.java
    │       ├── ResumenMedicacionDiariaResult.java
    │       └── ConsumoMedicamentoResult.java
    └── infrastructure/
        ├── adapter/in/rest/medicamento/
        │   ├── MedicacionController.java
        │   ├── MedicacionDiariaController.java
        │   └── ConsumoMedicamentoController.java
        ├── adapter/out/internal/medicamento/
        │   ├── EnfermedadQueryAdapter.java
        │   ├── DiagnosticoQueryAdapter.java
        │   ├── GalponQueryAdapter.java
        │   ├── LoteQueryAdapter.java
        │   └── InventarioMedicamentoAdapter.java
        ├── adapter/out/persistence/medicamento/
        │   ├── MedicacionPersistenceAdapter.java
        │   └── ConsumoMedicamentoPersistenceAdapter.java
        └── config/
            └── MedicamentoBeanConfiguration.java

    src/test/java/com/avicontrol/
    ├── domain/model/medicamento/
    ├── application/medicamento/
    ├── infrastructure/adapter/in/rest/medicamento/
    ├── infrastructure/adapter/out/internal/medicamento/
    └── integration/MedicamentoIntegrationTest.java

**Structure Decision**: Las reglas de normalización, distribución y trazabilidad se mantienen en dominio y aplicación. Los controladores solo traducen HTTP. El adaptador de inventario realiza la operación transaccional que el Plan 002 exponga y no accede a repositorios privados ajenos.

### Entidades y relación

    Medicacion
    ├── id: UUID
    ├── enfermedadId: UUID
    ├── medicamentoId: UUID
    ├── dosis: String
    ├── diasTotales: Integer
    └── descripcion: String

    ConsumoMedicamento
    ├── id: UUID
    ├── diagnosticoId: UUID
    ├── galponId: UUID
    ├── loteId: UUID
    ├── medicamentoId: UUID
    ├── cantidadIngresada: BigDecimal
    ├── unidadIngresada: String
    ├── cantidadNormalizada: BigDecimal
    ├── unidadBase: String
    ├── fechaAplicacion: LocalDate
    ├── registradoEn: Instant
    └── detalles: List<DetalleConsumo>

    DetalleConsumo
    ├── id: UUID
    ├── consumoId: UUID
    ├── recepcionId: UUID
    ├── cantidadDescontadaUnidadBase: BigDecimal
    ├── precioNetoHistoricoUnidadBase: BigDecimal
    └── moneda: String

Medicacion puede editarse para tratamientos futuros. ConsumoMedicamento y DetalleConsumo son históricos y no se recalculan si cambia posteriormente una medicación, una recepción o el precio del inventario.

### Contratos de los puertos

| Puerto | Responsabilidad |
| --- | --- |
| MedicacionRepositoryPort | Crear, buscar, editar y listar medicaciones disponibles. |
| ConsumoMedicamentoRepositoryPort | Guardar consumos y detalles, consultar consumos para costeo y evitar duplicados. |
| EnfermedadQueryPort | Confirmar que la enfermedad existe y está disponible. |
| DiagnosticoQueryPort | Obtener diagnóstico, lote, galpón, medicación y medicamento que originan el consumo. |
| GalponQueryPort | Consultar datos actuales para las respuestas diarias. |
| LoteQueryPort | Consultar lote activo, población y edad para las respuestas diarias. |
| InventarioMedicamentoPort | Confirmar medicamento, unidad base, disponibilidad, recepciones ordenadas por vencimiento, normalizar unidades y descontar existencias con precio histórico. |
| IntegrationEventPublisherPort | Publicar eventos versionados para Plan 002, Módulo 3 y otros consumidores. |

### Contratos HTTP propuestos

| Endpoint | Acceso | Resultado |
| --- | --- | --- |
| POST /api/medicaciones | Veterinario | Registra una medicación. |
| PUT /api/medicaciones/{medicacionId} | Veterinario | Edita una medicación para diagnósticos futuros. |
| GET /api/galpones/{galponId}/medicacion-diaria | Trabajador autorizado, administrador autorizado | Consulta tratamientos vigentes para la fecha actual. |
| GET /api/medicacion-diaria | Trabajador autorizado, administrador autorizado | Consulta el resumen consolidado permitido por el rol. |
| POST /api/diagnosticos/{diagnosticoId}/consumos-medicamento | Veterinario | Registra el consumo real y descuenta inventario. |

Las consultas responden 200 sin tratamientos con el mensaje Sin medicación activa para hoy. Las escrituras responden 201 al crear y 200 al editar. Un consumo con existencias insuficientes, unidad incompatible o diagnóstico inexistente responde 409 o 404 según el caso. Las respuestas de error utilizan application/problem+json.

### JSON común de errores

```json
    {
      "type": "https://avicontrol/errors/consumo-medicamento-no-permitido",
      "title": "Consumo de medicamento no permitido",
      "status": 409,
      "detail": "Las existencias disponibles no cubren la cantidad solicitada",
      "instance": "/api/diagnosticos/{diagnosticoId}/consumos-medicamento",
      "code": "EXISTENCIAS_INSUFICIENTES",
      "correlationId": "uuid",
      "fieldErrors": []
    }
```

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Acordar contratos, unidades, roles y dependencias de inventario y diagnóstico.

- [ ] T001 Contrastar los estados del tratamiento y la forma de obtener el diagnóstico vigente con el Plan 005.
- [ ] T002 Confirmar que no existe asignación trabajador-galpón y registrar la corrección correspondiente en el spec 015.
- [ ] T003 Acordar roles y permisos para veterinario, trabajador/operario y administrador autorizado.
- [ ] T004 Acordar con Plan 002 la unidad base, conversiones, orden FEFO, precio neto histórico y operación transaccional de descuento.
- [ ] T005 Acordar con Módulo 3 el contrato de consumo y detalle requerido para calcular costos.
- [ ] T006 Definir cómo se representa una dosis diaria, días alternos, periodos de descanso y cantidades variables; si no se necesitan, documentar dosis como instrucción textual.
- [ ] T007 Preparar fixtures de enfermedad, diagnóstico tratable, diagnóstico de sacrificio, recepciones con vencimientos y precios diferentes, y galpones sin tratamiento.

**Checkpoint**: Contratos externos, reglas de unidad y discrepancias de asignación resueltos.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Preparar dominio, persistencia y puertos compartidos.

- [ ] T008 Implementar Medicacion con validación de enfermedad, medicamento, dosis, días positivos y descripción.
- [ ] T009 Implementar ConsumoMedicamento y DetalleConsumo con cantidades normalizadas y precio histórico.
- [ ] T010 Definir excepciones y puertos de diagnóstico, enfermedad, galpón, lote e inventario.
- [ ] T011 Crear migraciones de medicaciones, consumos, detalles y eventos procesados usando la siguiente versión Flyway disponible.
- [ ] T012 Implementar adaptadores internos sin acceder a repositorios JPA privados de otros módulos.
- [ ] T013 Configurar transacciones locales, outbox e idempotencia.
- [ ] T014 Verificar invariantes: cantidad normalizada positiva, suma de detalles igual al consumo normalizado y ningún detalle con recepción vencida o anulada.

**Checkpoint**: Modelo, persistencia y contratos listos para prescripción, consulta y consumo.

## Phase 3: User Story 1 — Registrar una Medicación (Priority: P1)

**Spec**: 008, historia 1.

**Goal**: El veterinario crea una medicación válida asociada a una enfermedad y un medicamento del inventario, sin descontar existencias.

**Independent Test**: Se registra una medicación con datos completos; una enfermedad o medicamento no disponible y un usuario no veterinario son rechazados.

### Definición del evento para User Story 1

**Evento producido**: MedicacionRegistrada.

**Evento consumido**: Ninguno.

Se publica después de guardar todas las relaciones. Incluye eventId, eventVersion, occurredAt, producer, correlationId, aggregateId, medicacionId, enfermedadId, medicamentoId, dosis, diasTotales y descripcion.

### Definición del endpoint REST para User Story 1

| Elemento | Definición |
| --- | --- |
| Método y ruta | POST /api/medicaciones |
| Autorización | ROLE_VETERINARIO |
| Entrada | Enfermedad, medicamento, dosis, días totales positivos y descripción. |
| Respuesta 201 | Medicación creada y disponible para diagnósticos futuros. |
| Errores | 400 por datos faltantes o duración inválida, 403 por rol, 404 por enfermedad/medicamento y 503 si no se puede confirmar la escritura. |

#### JSON de solicitud

```json
    {
      "enfermedadId": "uuid",
      "medicamentoId": "uuid",
      "dosis": "200 g diluidos en 100 L de agua potable",
      "diasTotales": 5,
      "descripcion": "Administrar por agua de bebida"
    }
```

#### JSON de respuesta

```json
    {
      "medicacionId": "uuid",
      "enfermedadId": "uuid",
      "medicamentoId": "uuid",
      "dosis": "200 g diluidos en 100 L de agua potable",
      "diasTotales": 5,
      "descripcion": "Administrar por agua de bebida"
    }
```

### Tests e implementación

- [ ] T015 [US1] Probar campos obligatorios, espacios, días positivos y rol veterinario.
- [ ] T016 [US1] Probar validación de enfermedad y medicamento disponibles.
- [ ] T017 [US1] Probar que registrar una medicación no modifica existencias.
- [ ] T018 [US1] Implementar RegistrarMedicacionUseCase, persistencia, evento y endpoint.

**Checkpoint**: La medicación se encuentra disponible para diagnósticos futuros sin tocar inventario.

## Phase 4: User Story 2 — Editar una Medicación (Priority: P2)

**Spec**: 008, historia 2.

**Goal**: El veterinario actualiza una medicación solo para diagnósticos futuros.

**Independent Test**: Una medicación utilizada por un diagnóstico se edita; el diagnóstico anterior conserva su duración y fecha de reintegro.

### Definición del evento para User Story 2

**Evento producido**: MedicacionActualizada.

**Evento consumido**: Ninguno.

El evento comunica la nueva versión. No recalcula diagnósticos ni fechas ya registradas.

### Definición del endpoint REST para User Story 2

| Elemento | Definición |
| --- | --- |
| Método y ruta | PUT /api/medicaciones/{medicacionId} |
| Autorización | ROLE_VETERINARIO |
| Entrada | Enfermedad, medicamento, dosis, días, descripción y versión esperada. |
| Respuesta 200 | Medicación actualizada. |
| Errores | 400, 403, 404 y 409 por concurrencia o versión incompatible. |

#### JSON de solicitud

```json
    {
      "enfermedadId": "uuid",
      "medicamentoId": "uuid",
      "dosis": "250 g diluidos en 100 L de agua potable",
      "diasTotales": 7,
      "descripcion": "Administrar por agua de bebida",
      "version": 2
    }
```

### Tests e implementación

- [ ] T019 [US2] Probar edición válida y rechazo por rol.
- [ ] T020 [US2] Probar que diagnósticos existentes conservan valores históricos.
- [ ] T021 [US2] Implementar EditarMedicacionUseCase, control de versión, evento y endpoint.

**Checkpoint**: La edición no modifica tratamientos ya diagnosticados.

## Phase 5: User Story 3 — Consultar Medicación Diaria por Galpón (Priority: P1)

**Spec**: 015, historias 1 y 2.

**Goal**: El trabajador consulta el tratamiento vigente para un galpón en la fecha actual y recibe un resultado explícito cuando no existe medicación activa.

**Independent Test**: Un diagnóstico vigente devuelve enfermedad, medicamento, dosis, vía y avance; un tratamiento finalizado devuelve Sin medicación activa para hoy.

### Definición del evento para User Story 3

**Evento producido**: Ninguno.

**Evento consumido**: Ninguno obligatorio. La consulta obtiene el diagnóstico, galpón y lote vigentes mediante puertos internos sin crear una proyección.

La consulta no descuenta inventario, no registra consumo y no publica MedicacionConsultada.

### Definición del endpoint REST para User Story 3

| Elemento | Definición |
| --- | --- |
| Método y ruta | GET /api/galpones/{galponId}/medicacion-diaria |
| Autorización | ROLE_TRABAJADOR o administrador autorizado |
| Entrada | galponId y sin parámetro de fecha; siempre utiliza la fecha actual del Clock compartido. |
| Respuesta 200 con tratamiento | Galpón, lote, enfermedad, medicamento, dosis del día, vía, día actual y días totales. |
| Respuesta 200 sin tratamiento | Datos disponibles del galpón/lote y estado Sin medicación activa para hoy, sin cantidad aplicable. |
| Errores | 400 por identificador inválido, 404 por galpón inexistente, 409 por datos inconsistentes y 503 por dependencia no disponible. |

#### JSON de respuesta con tratamiento

```json
    {
      "galponId": "uuid",
      "galponNombre": "Galpón 5",
      "loteId": "uuid",
      "loteNombre": "LDP-005",
      "poblacionActual": 7960,
      "edadDias": 17,
      "tratamientos": [
        {
          "diagnosticoId": "uuid",
          "enfermedad": "Coccidiosis aviar",
          "medicamento": "Amprolio 20% Solución",
          "dosisHoy": "200 g diluidos en 100 L de agua potable",
          "viaAdministracion": "Agua de bebida en bebederos automáticos",
          "diaActual": 2,
          "diasTotales": 5
        }
      ],
      "estado": "MEDICACION_ACTIVA"
    }
```

#### JSON de respuesta sin tratamiento

```json
    {
      "galponId": "uuid",
      "galponNombre": "Galpón 2",
      "loteId": null,
      "tratamientos": [],
      "estado": "SIN_MEDICACION_ACTIVA_HOY",
      "mensaje": "Sin medicación activa para hoy"
    }
```

### Tests e implementación

- [ ] T022 [US3] Probar tratamiento activo, tratamiento finalizado, ausencia de diagnóstico y lote sin población.
- [ ] T023 [US3] Probar cálculo de día actual frente a días totales y diagnóstico concurrente.
- [ ] T024 [US3] Probar que la consulta no escribe ni descuenta inventario.
- [ ] T025 [US3] Implementar ConsultarMedicacionDiariaUseCase, resultados, DTOs y endpoint.

**Checkpoint**: La consulta diaria solo lee datos vigentes y no genera consumos.

## Phase 6: User Story 4 — Consultar Resumen Consolidado Diario (Priority: P2)

**Spec**: 015, historia 3.

**Goal**: El trabajador obtiene el total de tratamientos activos y el desglose diario de todos los galpones que puede consultar.

**Independent Test**: Con uno, varios o ningún tratamiento vigente, el resumen muestra conteos y tarjetas coherentes sin incluir galpones fuera del universo autorizado.

### Definición del evento para User Story 4

**Evento producido**: Ninguno.

**Evento consumido**: Ninguno obligatorio.

El resumen es una agregación de lectura. No se persiste ni publica un evento de consulta.

### Definición del endpoint REST

| Elemento | Definición |
| --- | --- |
| Método y ruta | GET /api/medicacion-diaria |
| Autorización | ROLE_TRABAJADOR o administrador autorizado |
| Respuesta 200 | totalGalponesConTratamiento, totalTratamientos y tarjetas con galpón, lote, avance, enfermedad, medicamento, dosis y vía. |
| Listado vacío | 0 galpones, 0 tratamientos y mensaje Sin tratamientos hoy. |
| Errores | 401/403 por seguridad, 409 por inconsistencias y 503 por dependencia no disponible. |

#### JSON de respuesta

```json
    {
      "totalGalponesConTratamiento": 1,
      "totalTratamientos": 1,
      "resumen": "Amprolio 20% en Galpón 5",
      "tratamientos": [
        {
          "galponId": "uuid",
          "galponNombre": "Galpón 5",
          "loteId": "uuid",
          "loteNombre": "LDP-005",
          "avance": "Día 2 de 5",
          "enfermedad": "Coccidiosis aviar",
          "medicamento": "Amprolio 20% Solución",
          "dosisHoy": "200 g diluidos en 100 L de agua potable",
          "viaAdministracion": "Agua de bebida en bebederos automáticos"
        }
      ]
    }
```

### Tests e implementación

- [ ] T026 [US4] Probar uno, varios y cero tratamientos, con conteos y tarjetas correctas.
- [ ] T027 [US4] Probar que no se aplica filtro por asignación inexistente y que se respetan permisos.
- [ ] T028 [US4] Probar que múltiples tratamientos del mismo galpón aparecen separados.
- [ ] T029 [US4] Implementar ConsultarResumenMedicacionDiariaUseCase y endpoint consolidado.

**Checkpoint**: El resumen se calcula en lectura sin crear una proyección ni modificar datos.

## Phase 7: User Story 5 — Registrar Consumo Real de Medicamento (Priority: P1)

**Spec**: 013, historia 1.

**Goal**: El veterinario registra la cantidad realmente aplicada, la normaliza y la descuenta de las recepciones disponibles conservando el precio histórico.

**Independent Test**: Un consumo usa una recepción; otro se distribuye entre varias por vencimiento. En ambos casos el total descontado coincide con la cantidad normalizada y el desglose queda disponible para Módulo 3.

### Definición del evento para User Story 5

**Evento producido**: ConsumoMedicamentoRegistrado y, si el contrato del Plan 002 lo requiere, ExistenciasMedicamentoDescontadas.

El evento incluye eventId, eventVersion, occurredAt, producer, correlationId, aggregateId, consumoId, diagnosticoId, galponId, loteId, medicamentoId, cantidadNormalizada, unidadBase, fechaAplicacion y detalles de recepción con cantidad y precio histórico.

**Evento consumido**: DiagnosticoRegistrado y contratos de inventario solo si se utiliza integración asíncrona. La operación autoritativa vuelve a consultar diagnóstico y recepciones antes de confirmar.

### Definición del endpoint REST

| Elemento | Definición |
| --- | --- |
| Método y ruta | POST /api/diagnosticos/{diagnosticoId}/consumos-medicamento |
| Autorización | ROLE_VETERINARIO |
| Entrada | Cantidad aplicada, unidad compatible y fecha de aplicación. No recibe galponId, loteId ni medicamentoId editable. |
| Respuesta 201 | Consumo, cantidad normalizada, unidad base y detalles por recepción. |
| Errores | 400 por cantidad/unidad/fecha inválida, 403 por rol, 404 por diagnóstico, 409 por unidad incompatible, recepción no disponible o existencias insuficientes. |

#### JSON de solicitud

```json
    {
      "cantidad": 200,
      "unidad": "GRAMO",
      "fechaAplicacion": "2026-10-02"
    }
```

#### JSON de respuesta

```json
    {
      "consumoId": "uuid",
      "diagnosticoId": "uuid",
      "galponId": "uuid",
      "loteId": "uuid",
      "medicamentoId": "uuid",
      "cantidadIngresada": 200,
      "unidadIngresada": "GRAMO",
      "cantidadNormalizada": 0.2,
      "unidadBase": "KILOGRAMO",
      "fechaAplicacion": "2026-10-02",
      "detalles": [
        {
          "recepcionId": "uuid",
          "cantidadDescontadaUnidadBase": 0.2,
          "precioNetoHistoricoUnidadBase": 12500,
          "moneda": "COP"
        }
      ]
    }
```

### Tests e implementación

- [ ] T030 [US5] Probar autorización, diagnóstico existente y obtención automática de galpón, lote y medicamento.
- [ ] T031 [US5] Probar normalización de unidades compatibles y rechazo de unidades incompatibles.
- [ ] T032 [US5] Probar distribución FEFO en una y varias recepciones.
- [ ] T033 [US5] Probar vencimiento, anulación, falta de existencias y ausencia de descuentos parciales.
- [ ] T034 [US5] Probar precios históricos diferentes sin promediar y suma de detalles igual a cantidad normalizada.
- [ ] T035 [US5] Implementar RegistrarConsumoMedicamentoUseCase, persistencia, descuento transaccional, evento y endpoint.
- [ ] T036 [US5] Verificar contrato de datos para el costeo del Módulo 3.

**Checkpoint**: El consumo y el inventario se confirman juntos, con trazabilidad por recepción y sin pérdida del precio histórico.

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Verificar contratos, seguridad, integración e invariantes del inventario.

- [ ] T037 Documentar endpoints, roles, unidades, mensajes y errores en OpenAPI.
- [ ] T038 Completar MedicamentoIntegrationTest con registro, edición, consulta diaria y consumo.
- [ ] T039 Verificar que registrar o editar medicación no descuente existencias ni modifique diagnósticos existentes.
- [ ] T040 Verificar que una consulta diaria no escriba diagnósticos, consumos ni inventario.
- [ ] T041 Verificar atomicidad del descuento y ausencia de consumos parciales ante concurrencia.
- [ ] T042 Verificar idempotencia de ConsumoMedicamentoRegistrado y eventos de inventario.
- [ ] T043 Verificar que Módulo 3 pueda calcular costo como suma de cantidad descontada por precio histórico de cada recepción.
- [ ] T044 Verificar que Plan 005 consuma MedicacionQueryPort y no duplique el catálogo.
- [ ] T045 Extender comprobaciones arquitectónicas: dominio sin frameworks, aplicación sin JPA/HTTP y adaptadores sin acceso a repositorios privados.
- [ ] T046 Ejecutar pruebas y tareas de calidad disponibles, resolviendo primero la herramienta de construcción declarada por el repositorio.
- [ ] T047 Medir objetivos de respuesta y garantizar que el resumen no haga una llamada interna por cada galpón cuando pueda consultar por conjunto.

**Checkpoint**: Prescripción, consulta y consumo integrados sin mezclar sus responsabilidades.

## Dependencies & Execution Order

### Phase Dependencies

- **Setup**: Debe resolver unidades, FEFO, roles, asignaciones y contratos de inventario.
- **Foundational**: Depende de Setup y habilita todas las historias.
- **US1 y US2**: Definen la medicación que consume Plan 005.
- **US3 y US4**: Dependen de diagnóstico, galpón, lote y reloj; no dependen de registrar consumos.
- **US5**: Depende de diagnósticos con medicación y de las operaciones transaccionales del Plan 002.
- **Polish**: Depende de todas las historias y de los contratos de Módulo 3.

### Dependencias con otros planes

- **Plan 002**: Proporciona medicamentos, recepciones, existencias, conversiones, vencimientos y precios históricos.
- **Plan 005**: Proporciona enfermedades, diagnósticos y la medicación seleccionada en cada diagnóstico.
- **Plan 007**: Puede cambiar población viva; las consultas leen la población vigente.
- **Módulo 3**: Consume los consumos y detalles para costear medicamentos por lote y galpón.
- **Módulo 1**: Proporciona Galpon y Lote.
- **General.md**: Proporciona seguridad, actor, reloj, errores, eventos y configuración transversal.

### Dentro de cada User Story

- Entidades y reglas de dominio antes que casos de uso.
- Puertos antes que adaptadores.
- Casos de uso antes que controladores.
- Persistencia, descuento y eventos dentro de la transacción definida.
- Pruebas junto con implementación y checkpoint antes de cerrar la historia.

## Notes

- T001 a T047 identifican tareas; US1 a US5 identifican las historias.
- Este documento describe componentes por implementar; no afirma que ya existan.
- La medicación prescrita no representa administración ni descuento de inventario.
- El consumo obtiene diagnóstico, lote, galpón y medicamento desde el diagnóstico.
- Los detalles de consumo conservan precio histórico por recepción y no deben promediarse.
- No existen asignaciones trabajador-galpón en este plan; debe corregirse el FR correspondiente del spec 015.
- Si se requieren días alternos, descansos o dosis variables, el contrato de Medicacion debe ampliarse antes de implementar la consulta diaria.
- Los eventos entre módulos deben tener contrato versionado, consumidor idempotente y trazabilidad con correlationId.



