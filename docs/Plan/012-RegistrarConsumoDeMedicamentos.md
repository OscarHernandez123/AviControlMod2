# Implementation Plan: Registro de Consumo de Medicamentos

**Date**: 25/10/2026
**Last Updated**: 2026-09-26

**Arquitectura y tecnologías**: [General.md](General.md)

**Specs**:

- [013-RegistrarConsumoMedicamento.md](../specs/013-RegistrarConsumoMedicamento.md)

## Summary

Registrar la cantidad de medicamento realmente aplicada a partir de un diagnóstico, descontar existencias de forma atómica mediante el Plan 002 y conservar el detalle por recepción y su precio neto histórico para el costeo del Módulo 3. El diagnóstico determina medicamento, lote y galpón; el cliente no puede sustituirlos.

## Technical Context

**Performance Goals**: El 95 % de consumos confirmados y disponibles para Módulo 3 en máximo 2 segundos. Distribución y descuento de recepciones en una única operación transaccional acordada con Plan 002.

**Constraints**: Solo ROLE_VETERINARIO registra consumos. Se admiten `MILIGRAMO`, `GRAMO`, `MILILITRO` y `UNIDAD`; masa se normaliza a gramos (`1.000 mg = 1 g`), volumen a mililitros y conteo a unidades. No se convierten dimensiones incompatibles ni se aceptan descuentos parciales. No se calcula la liquidación final del lote.

**Scale/Scope**: SPEC-013, una historia; registro de consumo, normalización, distribución FEFO, descuento y trazabilidad histórica por recepción.

**Dependencias funcionales**: Plan 015 provee diagnóstico y medicación capturada; Plan 002 es dueño de medicamentos, recepciones y precios; Plan 010 es dueño de unidades e inventario y expone el descuento de existencias; Módulo 1 provee identidades de lote y galpón; Módulo 3 consume el detalle de costeo; General.md define seguridad, actor, transacciones y errores.

### Decisiones específicas

1. El consumo se asocia a un diagnóstico y obtiene de este galpón, lote, medicación y medicamento; no son seleccionables en el request.
2. La unidad se normaliza con el contrato de inventario del Plan 002. Una magnitud incompatible se rechaza.
3. El consumo se distribuye entre recepciones disponibles del mismo medicamento por vencimiento más próximo (FEFO); cada porción conserva recepción y precio neto histórico por unidad base.
4. Diagnóstico, recepciones y disponibilidad se vuelven a validar al confirmar. Si no se puede cubrir el total, no se guarda consumo ni se aplican descuentos parciales.
5. Los eventos se publican tras confirmar la operación local, con contrato acordado entre consumidores. El consumidor tolera reentregas sin duplicar efectos.
6. Consumo y detalles son registros históricos; cambios posteriores en medicación, recepción o precio no los recalculan.

### Alcance documental

- Este plan implementa SPEC-013 y depende del catálogo de medicaciones y consultas diarias del [Plan 006](006-GestionDeMedicaciones.md).
- El [Plan 002](002-GestionDeRecepciones.md) conserva la propiedad de recepciones y precios históricos.
- El [Plan 010](010-GestionDelInventario.md) conserva saldos, unidades y movimientos y expone el descuento transaccional por recepción.
- El [Plan 015](015-DiagnosticoYReintegroSanitario.md) conserva el diagnóstico y la medicación que lo originó.
- Módulo 3 calcula el costo real a partir de cantidad por recepción y precio neto histórico. Este plan entrega datos y no liquida el lote.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/013-RegistrarConsumoMedicamento.md
└── plan/012-RegistrarConsumoDeMedicamentos.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/model/medicamento/
│   ├── ConsumoMedicamento.java
│   └── DetalleConsumo.java
├── domain/exception/medicamento/
├── domain/port/out/medicamento/
│   ├── ConsumoMedicamentoRepositoryPort.java
│   ├── DiagnosticoQueryPort.java
│   └── InventarioMedicamentoPort.java
├── application/medicamento/
│   ├── RegistrarConsumoMedicamentoUseCase.java
│   └── result/ConsumoMedicamentoResult.java
└── infrastructure/
    ├── adapter/in/rest/medicamento/ConsumoMedicamentoController.java
    ├── adapter/out/internal/medicamento/
    ├── adapter/out/persistence/medicamento/ConsumoMedicamentoPersistenceAdapter.java
    └── config/MedicamentoBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/medicamento/
├── application/medicamento/
├── infrastructure/adapter/in/rest/medicamento/
└── integration/ConsumoMedicamentoIntegrationTest.java
```

**Structure Decision**: El dominio y el caso de uso coordinan trazabilidad e invariantes. El adaptador del Plan 002 ejecuta la operación propietaria de inventario; este plan no modifica directamente sus repositorios ni tablas.

### Entidades y relación

```text
Diagnostico (referencia externa) ───< ConsumoMedicamento ───< DetalleConsumo

ConsumoMedicamento: id, diagnosticoId, galponId, loteId, medicamentoId,
  cantidadIngresada, unidadIngresada, cantidadNormalizada, unidadBase,
  fechaAplicacion, registradoEn
DetalleConsumo: id, consumoId, recepcionId, cantidadDescontadaUnidadBase,
  precioNetoHistoricoUnidadBase, moneda
```

La suma de los detalles debe coincidir con la cantidad normalizada. Diagnóstico, lote, galpón, medicamento y recepción son identidades externas; no se crean copias autoritativas.

### Contratos de los puertos

| Puerto | Responsabilidad |
| --- | --- |
| `ConsumoMedicamentoRepositoryPort` | Guardar consumo y detalles; consultar datos para costeo y evitar duplicados. |
| `DiagnosticoQueryPort` | Obtener diagnóstico y datos de medicación, lote y galpón conservados al registrarlo. |
| `InventarioMedicamentoPort` | Consultar unidad, normalizar cantidad y solicitar al Plan 010 el descuento atómico por recepción con su precio histórico. |
| `IntegrationEventPublisherPort` | Publicar el hecho confirmado según contrato acordado con Plan 002 y Módulo 3. |

### Contratos HTTP propuestos

| Método y ruta | Acceso | Responsabilidad |
| --- | --- | --- |
| `POST /api/diagnosticos/{diagnosticoId}/consumos-medicamento` | `ROLE_VETERINARIO` | Registrar consumo real, normalizar unidad y descontar existencias. |

La respuesta y errores usan `application/problem+json` conforme a General.md y SPEC-013.

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

**Purpose**: Acordar contratos y reglas con diagnóstico, inventario y costeo.

- [ ] T001 Acordar con Plan 015 los campos del diagnóstico necesarios para resolver medicamento, lote y galpón.
- [ ] T002 Acordar con Plan 010 normalización de unidades, FEFO y operación atómica de descuento; con Plan 002, recepción y precio neto histórico.
- [ ] T003 Acordar con Módulo 3 el contrato de consumo y detalle requerido para calcular costos.
- [ ] T004 Confirmar roles, errores, reloj y claves de idempotencia conforme a General.md.

**Checkpoint**: Contratos entre propietarios de datos acordados antes de implementar.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Preparar el modelo histórico, persistencia y puertos de salida.

- [ ] T005 Implementar ConsumoMedicamento y DetalleConsumo con cantidades normalizadas y precio histórico.
- [ ] T006 Definir excepciones y puertos de diagnóstico, inventario y persistencia propios.
- [ ] T007 Crear migraciones de consumo y detalles usando la siguiente versión Flyway disponible.
- [ ] T008 Implementar adaptadores internos sin acceder a repositorios privados de otros módulos.
- [ ] T009 Configurar límite transaccional, publicación confiable e idempotencia.
- [ ] T010 Verificar invariantes: cantidad positiva, suma de detalles igual al consumo y ninguna recepción vencida o anulada.

**Checkpoint**: Modelo y contratos listos para registrar el consumo sin escrituras parciales.

## Phase 3: User Story 1 — Registrar el consumo real de un medicamento (Priority: P1)

**Spec**: SPEC-013, historia 1.

**Goal**: El veterinario registra la cantidad aplicada a partir de un diagnóstico y deja el descuento y el detalle histórico disponibles para Módulo 3.

**Independent Test**: Un consumo válido normaliza la unidad, descuenta una o varias recepciones en orden FEFO y almacena el precio histórico de cada porción. Sin inventario suficiente, unidad compatible o autorización, no hay consumo ni descuento parcial.

### Definición del evento para User Story 1

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
      "cantidadNormalizada": 200,
      "unidadBase": "GRAMO",
      "fechaAplicacion": "2026-10-02",
      "detalles": [
        {
          "recepcionId": "uuid",
          "cantidadDescontadaUnidadBase": 200,
          "precioNetoHistoricoUnidadBase": 12.5,
          "moneda": "COP"
        }
      ]
    }
```

### Tests e implementación

- [ ] T011 [US1] Probar autorización, diagnóstico existente y obtención automática de galpón, lote y medicamento.
- [ ] T012 [US1] Probar normalización de unidades compatibles y rechazo de unidades incompatibles.
- [ ] T013 [US1] Probar distribución FEFO en una y varias recepciones.
- [ ] T014 [US1] Probar vencimiento, anulación, falta de existencias y ausencia de descuentos parciales.
- [ ] T015 [US1] Probar precios históricos diferentes sin promediar y suma de detalles igual a cantidad normalizada.
- [ ] T016 [US1] Implementar RegistrarConsumoMedicamentoUseCase, persistencia, descuento transaccional, evento y endpoint.
- [ ] T017 [US1] Verificar contrato de datos para el costeo del Módulo 3.

**Checkpoint**: El consumo y el inventario se confirman juntos, con trazabilidad por recepción y sin pérdida del precio histórico.


## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Verificar contratos, permisos, atomicidad, idempotencia y disponibilidad para costeo.

- [ ] T018 Documentar endpoint, autorización, unidades y errores en OpenAPI.
- [ ] T019 Probar concurrencia, recepción vencida/anulada y reversión completa si falla el descuento.
- [ ] T020 Verificar el contrato de datos de costeo con Módulo 3 y el acceso por puertos de Plan 002.
- [ ] T021 Verificar consumidor idempotente y trazabilidad con correlationId.

## Dependencies & Execution Order

### Phase Dependencies

- **Setup**: Define contratos con Planes 002/005 y Módulo 3.
- **Foundational**: Depende del cierre de Setup y habilita la historia.
- **US1**: Depende del diagnóstico con medicación del Plan 015 y el descuento atómico del Plan 010.
- **Polish**: Depende de la historia completa y del contrato de costeo.

### Dependencias con otros planes

- **Plan 002**: Propietario de medicamentos, recepciones y precios de compra.
- **Plan 010**: Propietario de unidades, saldos y movimientos de inventario; ejecuta el descuento.
- **Plan 015**: Propietario del diagnóstico y de la medicación seleccionada.
- **Plan 006**: Propietario del catálogo de medicaciones y consultas diarias.
- **Módulo 3**: Consumidor del detalle de costo real.
- **Módulo 1**: Identidades de lote y galpón.
- **General.md**: Seguridad, transacciones, errores y eventos.

### Dentro de cada User Story

- Definir invariantes antes del caso de uso.
- Acordar los puertos propietarios antes de los adaptadores.
- Implementar el caso de uso antes del controlador.
- Probar autorización, idempotencia y atomicidad junto con la implementación.

## Notes

- El consumo representa lo realmente aplicado; la prescripción no genera descuento de inventario.
- El precio histórico por recepción se conserva y no se promedia.
- Las tareas describen trabajo pendiente; no afirman que el código exista.