# Implementation Plan: Consultar medicación por galpón por día

**Date**: 05/11/2026
**Spec**: [015-ConsultarMedicacionPorGalponPorDia.md](../specs/015-ConsultarMedicacionPorGalponPorDia.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Ofrecer al trabajador la medicación prescrita para el día en cualquier galpón seleccionado y un resumen consolidado de tratamientos activos. Las respuestas muestran galpón, lote, enfermedad, medicamento, dosis, vía y avance, pero nunca registran consumo ni descuentan inventario.

## Technical Context

**Performance Goals**: completar consultas en menos de dos segundos.
**Constraints**: sólo lectura; `ROLE_TRABAJADOR` y administradores autorizados; la fecha procede del `Clock`; se muestran tratamientos concurrentes por separado.
**Scale/Scope**: detalle por galpón y resumen de galpones seleccionados; no prescribe, diagnostica, administra ni descuenta medicamento.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 015-ConsultarMedicacionPorGalponPorDia.md
└── plan/
    └── 015-ConsultarMedicacionPorGalponPorDia.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/sanitario/medicacion/
│   │   ├── MedicacionDiaria.java
│   │   └── ProgresoTratamiento.java
│   └── port/out/sanitario/medicacion/
│       ├── DiagnosticoTratamientoQueryPort.java
│       └── GalponLoteQueryPort.java
├── application/
│   └── sanitario/medicacion/
│       ├── ConsultarMedicacionDiariaGalponUseCase.java
│       ├── ConsultarResumenMedicacionDiariaUseCase.java
│       └── result/
│           ├── MedicacionDiariaGalponResult.java
│           └── ResumenMedicacionDiariaResult.java
└── infrastructure/
    ├── adapter/in/rest/medicacion/
    │   ├── MedicacionDiariaController.java
    │   ├── dto/
    │   │   ├── MedicacionDiariaGalponResponse.java
    │   │   └── ResumenMedicacionDiariaResponse.java
    │   └── mapper/MedicacionDiariaRestMapper.java
    ├── adapter/out/integration/modulo1/GalponLoteQueryAdapter.java
    └── config/MedicacionDiariaBeanConfiguration.java

src/test/java/com/avicontrol/
├── domain/model/sanitario/medicacion/ProgresoTratamientoTest.java
├── application/sanitario/medicacion/
│   ├── ConsultarMedicacionDiariaGalponUseCaseTest.java
│   └── ConsultarResumenMedicacionDiariaUseCaseTest.java
└── infrastructure/adapter/in/rest/medicacion/MedicacionDiariaControllerTest.java
```

**Structure Decision**: no hay migración ni adaptador de escritura. El dominio calcula vigencia y avance; los puertos reúnen galpón/lote vigente y tratamientos ya registrados por el Plan 011.

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Confirmar contratos de lote activo, diagnóstico, medicación, enfermedad y vía de administración.
- [ ] T002 Definir `Clock` y datos de prueba con tratamiento activo, finalizado, día sin aplicación y tratamientos concurrentes.
- [ ] T003 Confirmar roles y la semántica del conjunto de galpones seleccionado por cada consulta.

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T004 Crear `ProgresoTratamiento` para calcular día actual de N y detectar fecha fuera de vigencia.
- [ ] T005 Crear `MedicacionDiaria` con enfermedad, medicamento, principio activo, dosis, vía y cantidad cero si corresponde día sin aplicación.
- [ ] T006 Definir puertos exclusivamente de consulta y validar que no se invoquen salidas de inventario.

**Checkpoint**: la aplicación sabe listar cada tratamiento activo sin producir consumo alguno.

## Phase 3: User Story 1 - Consultar medicación del galpón (Priority: P1)

**Goal**: mostrar las instrucciones médicas activas del día o comunicar claramente que no existen.

**Independent Test**: un tratamiento de cinco días iniciado ayer muestra `Día 2 de 5`; sin tratamiento vigente devuelve `Sin medicación activa para hoy` y ninguna cantidad.

- [ ] T007 [P] [US1] Probar tratamiento vigente, finalizado, galpón sin lote, población cero y múltiples tratamientos.
- [ ] T008 [US1] Implementar `ConsultarMedicacionDiariaGalponUseCase` y DTO de detalle.

## Phase 4: User Story 2 - Consultar resumen consolidado (Priority: P2)

**Goal**: presentar la tarjeta de galpones con tratamiento y tarjetas individuales para los galpones seleccionados.

**Independent Test**: si sólo un galpón seleccionado tiene tratamiento, el resumen muestra un galpón y un tratamiento; sin tratamientos muestra cero y lista vacía.

- [ ] T009 [P] [US2] Probar selección de galpones, conteo de galpones distinto del conteo de tratamientos y selección vacía.
- [ ] T010 [US2] Implementar `ConsultarResumenMedicacionDiariaUseCase`.

## Endpoints

| Método y ruta | Roles | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `GET /api/galpones/{galponId}/medicacion-diaria?fecha=2026-11-05` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` | Sin cuerpo | `200 OK` con tratamientos o estado sin medicación |
| `GET /api/medicacion-diaria/resumen?galponIds={id1,id2}` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` | Sin cuerpo | `200 OK` con tarjeta y tratamientos seleccionados |

```json
{
  "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "estado": "CON_MEDICACION_ACTIVA",
  "tratamientos": [
    {
      "loteCodigo": "LDP-005",
      "progreso": "Día 2 de 5",
      "enfermedad": "Coccidiosis Aviar",
      "medicamento": "Amprolio 20% Solución",
      "dosisHoy": "200 g diluidos en 100 L de agua potable",
      "viaAdministracion": "Agua de bebida en bebederos automáticos"
    }
  ]
}
```

**Errores**: `400` identificadores o fecha inválidos; `401` no autenticado; `403` sin rol; `404` galpón inexistente; `503` datos clínicos o Módulo 1 no disponibles.

## Polish & Cross-Cutting Concerns

- [ ] T011 Documentar que esta consulta no registra el consumo real ni descuenta inventario.
- [ ] T012 Probar `401`, `403`, listado vacío y formato JSON mediante MockMvc.
- [ ] T013 Ejecutar pruebas y `git diff --check`.

## Dependencies & Execution Order

- **Setup** confirma las fuentes clínicas y de galpón.
- **Foundational** crea cálculo y puertos de sólo lectura.
- **US1** ofrece detalle; **US2** agrega únicamente los resultados de los galpones seleccionados.

## Notes

- La fecha se permite como parámetro para pruebas y auditoría, con valor predeterminado del `Clock` si se omite.
- El Plan 013 es el único responsable de registrar consumo real y solicitar descuento de existencias.
