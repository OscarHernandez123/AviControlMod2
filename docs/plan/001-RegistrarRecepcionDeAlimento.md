# Plan: Registrar recepción de alimento

**Spec**: [001-RegistrarRecepcionDeAlimento.md](../specs/001-RegistrarRecepcionDeAlimento.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

El administrador registra una recepción comercial de alimento y confirma su entrada inicial mediante el puerto de Inventario. La operación guarda datos históricos, calcula valores comerciales y no deja una recepción confirmada sin movimiento de entrada.

## Fuera de alcance

Edición, historial, recepción de medicamentos, consultas de inventario, FEFO, vencimientos, salidas y cobertura.

## Dependencias

Catálogo de alimentos y `RegistrarEntradaInventarioPort` disponibles. Inventario sigue siendo dueño de movimientos y saldos.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/model/recepcion/RecepcionAlimento.java
├── domain/model/recepcion/EstadoRecepcion.java
├── domain/port/out/recepcion/RecepcionAlimentoRepositoryPort.java
├── application/recepcion/RegistrarRecepcionAlimentoUseCase.java
├── application/recepcion/RegistrarRecepcionAlimentoCommand.java
├── application/inventario/port/RegistrarEntradaInventarioPort.java
├── infrastructure/adapter/in/rest/recepcion/RecepcionAlimentoController.java
├── infrastructure/adapter/in/rest/recepcion/dto/RegistrarRecepcionAlimentoRequest.java
├── infrastructure/adapter/in/rest/recepcion/dto/RecepcionAlimentoResponse.java
├── infrastructure/adapter/in/rest/recepcion/mapper/RecepcionAlimentoRestMapper.java
└── infrastructure/adapter/out/persistence/recepcion/RecepcionAlimentoPersistenceAdapter.java
src/main/resources/db/migration/V__crear_recepciones_alimento.sql
src/test/java/com/avicontrol/{domain,application,infrastructure}/recepcion/
```

## Contratos

`RegistrarEntradaInventarioPort` recibe `recepcionId`, producto, cantidad total en kg, fecha, actor y referencia de trazabilidad. Debe confirmar o fallar dentro de la misma transacción local. El controlador no accede a repositorios ni inventario.

## User Story 1 — Registrar recepción de alimento

Como administrador, registro una recepción de alimento para que quede disponible en bodega con trazabilidad comercial.

### Endpoint

| Método y ruta | Rol | Respuesta |
| --- | --- | --- |
| `POST /api/recepciones/alimentos` | `ROLE_ADMINISTRADOR` | `201 Created` con la recepción confirmada |

```json
{
  "alimentoId": "uuid",
  "codigoLote": "AL-2026-001",
  "fechaIngreso": "2026-11-05",
  "fechaVencimiento": "2027-03-05",
  "cantidadBultos": 40,
  "pesoPorBultoKg": 40,
  "precioNetoPorBulto": 82000,
  "impuestoPorcentaje": 19
}
```

```json
{
  "id": "uuid",
  "estado": "CONFIRMADA",
  "cantidadTotalKg": 1600,
  "precioNetoTotal": 3280000,
  "fechaIngreso": "2026-11-05"
}
```

Errores: `400` campos o cantidades inválidas, `404` alimento inexistente, `409` vencimiento inválido o entrada duplicada, `503` inventario no disponible.

## Pruebas y Definition of Done

- Validar cantidades positivas, vencimiento posterior, cálculo de kg y valores comerciales.
- Probar éxito, validación, autorización y fallo atómico del puerto de inventario con MockMvc.
- Migración, pruebas de dominio/caso de uso/HTTP y OpenAPI en verde.
