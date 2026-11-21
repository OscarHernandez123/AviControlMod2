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
    ├── adapter/out/integration/modulo1/
    │   ├── GalponLoteQueryAdapter.java
    │   └── GalponLoteNoDisponibleAdapter.java
    ├── adapter/out/persistence/sanitario/medicacion/DiagnosticoTratamientoQueryAdapter.java
    └── config/MedicacionDiariaBeanConfiguration.java

src/main/resources/db/migration/
├── V14__datos_consulta_medicacion_diaria.sql
└── V15__ampliar_prescripciones_y_diagnosticos.sql

src/test/java/com/avicontrol/
├── domain/model/sanitario/medicacion/ProgresoTratamientoTest.java
├── application/sanitario/medicacion/
│   ├── ConsultarMedicacionDiariaGalponUseCaseTest.java
│   └── ConsultarResumenMedicacionDiariaUseCaseTest.java
└── infrastructure/adapter/in/rest/medicacion/
    ├── MedicacionDiariaControllerTest.java
    └── MedicacionDiariaFlujoTest.java
```

**Structure Decision**: la consulta no escribe datos de negocio. La migración V14 amplía únicamente el galpón provisional del Módulo 1 con nombre, aforo y código del lote. El dominio calcula vigencia y avance; los puertos reúnen galpón/lote vigente y tratamientos registrados por el Plan 011.

## Phase 1: Setup (Shared Infrastructure)

- [x] T001 Confirmar contratos de lote activo, diagnóstico, medicación, enfermedad y vía de administración.
- [x] T002 Definir `Clock` y datos de prueba con tratamiento activo, finalizado, día sin aplicación y tratamientos concurrentes.
- [x] T003 Confirmar roles y la semántica del conjunto de galpones seleccionado por cada consulta.

## Phase 2: Foundational (Blocking Prerequisites)

- [x] T004 Crear `ProgresoTratamiento` para calcular día actual de N y detectar fecha fuera de vigencia.
- [x] T005 Crear `MedicacionDiaria` con enfermedad, medicamento, principio activo, dosis, vía y cantidad cero si corresponde día sin aplicación.
- [x] T006 Definir puertos exclusivamente de consulta y validar que no se invoquen salidas de inventario.

**Checkpoint**: la aplicación sabe listar cada tratamiento activo sin producir consumo alguno.

## Phase 3: User Story 1 - Consultar medicación activa del galpón (Priority: P1)

**Goal**: mostrar las instrucciones médicas activas del día.

**Independent Test**: un tratamiento de cinco días iniciado ayer muestra `Día 2 de 5`; sin tratamiento vigente devuelve `Sin medicación activa para hoy` y ninguna cantidad.

- [x] T007 [P] [US1] Probar tratamiento vigente, población cero y múltiples tratamientos.
- [x] T008 [US1] Implementar `ConsultarMedicacionDiariaGalponUseCase` y DTO de detalle.

## Phase 4: User Story 2 - Consultar galpón sin medicación activa (Priority: P1)

**Goal**: distinguir la ausencia de tratamiento activo de un error o de la ausencia de lote.

**Independent Test**: un galpón con lote y sin tratamientos vigentes devuelve `200 OK`, `estado: SIN_MEDICACION_ACTIVA` y una lista vacía; un galpón inexistente conserva `404`.

- [x] T009 [P] [US2] Probar tratamiento finalizado, día sin aplicación, galpón sin lote y ausencia de tratamientos.
- [x] T010 [US2] Devolver el estado explícito y una colección vacía sin registrar consumos ni movimientos.

## Phase 5: User Story 3 - Consultar resumen consolidado (Priority: P2)

**Goal**: presentar la tarjeta de galpones con tratamiento y tarjetas individuales para los galpones seleccionados.

**Independent Test**: si sólo un galpón seleccionado tiene tratamiento, el resumen muestra un galpón y un tratamiento; sin tratamientos muestra cero y lista vacía.

- [x] T011 [P] [US3] Probar selección de galpones, conteo de galpones distinto del conteo de tratamientos y selección vacía.
- [x] T012 [US3] Implementar `ConsultarResumenMedicacionDiariaUseCase`.

## Endpoints

| Método y ruta | Roles | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `GET /api/galpones/{galponId}/medicacion-diaria?fecha=2026-11-05` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` | Sin cuerpo | `200 OK` con tratamientos o estado sin medicación |
| `GET /api/medicacion-diaria/resumen?galponIds={id1,id2}` | `ROLE_TRABAJADOR`, `ROLE_ADMINISTRADOR` | Sin cuerpo | `200 OK` con tarjeta y tratamientos seleccionados |

```json
{
  "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "nombreGalpon": "Galpón 5",
  "aforoMaximo": 10000,
  "estadoGalpon": "AISLAMIENTO",
  "loteId": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
  "loteCodigo": "LDP-005",
  "poblacionViva": 8000,
  "edadDias": 12,
  "fecha": "2026-11-05",
  "estado": "CON_MEDICACION_ACTIVA",
  "mensaje": null,
  "cantidadTratamientos": 1,
  "tratamientos": [
    {
      "diagnosticoId": "3fa85f64-5717-4562-b3fc-2c963f66afa8",
      "loteCodigo": "LDP-005",
      "progreso": "Día 2 de 5",
      "enfermedad": "Coccidiosis Aviar",
      "medicamento": "Amprolio 20% Solución",
      "principioActivo": "Amprolio",
      "dosisHoy": "200 g diluidos en 100 L de agua potable",
      "viaAdministracion": "Agua de bebida en bebederos automáticos",
      "aplicaHoy": true
    }
  ]
}
```

Respuesta de US2 para un galpón existente sin tratamiento vigente:

```json
{
  "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "nombreGalpon": "Galpón 5",
  "aforoMaximo": 10000,
  "estadoGalpon": "PRODUCTIVA",
  "loteId": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
  "loteCodigo": "LDP-005",
  "poblacionViva": 8000,
  "edadDias": 12,
  "fecha": "2026-11-05",
  "estado": "SIN_MEDICACION_ACTIVA",
  "mensaje": "Sin medicación activa para hoy",
  "cantidadTratamientos": 0,
  "tratamientos": []
}
```

Respuesta de US3:

```json
{
  "fecha": "2026-11-05",
  "galponesConTratamiento": 1,
  "cantidadTratamientos": 1,
  "resumen": "Amprolio 20% Solución en Galpón 5",
  "galpones": [
    {
      "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "estado": "CON_MEDICACION_ACTIVA",
      "cantidadTratamientos": 1,
      "tratamientos": [
        {
          "diagnosticoId": "3fa85f64-5717-4562-b3fc-2c963f66afa8",
          "loteCodigo": "LDP-005",
          "progreso": "Día 2 de 5",
          "enfermedad": "Coccidiosis Aviar",
          "medicamento": "Amprolio 20% Solución",
          "principioActivo": "Amprolio",
          "dosisHoy": "200 g diluidos en 100 L de agua potable",
          "viaAdministracion": "Agua de bebida en bebederos automáticos",
          "aplicaHoy": true
        }
      ]
    }
  ]
}
```

**Errores**: `400` identificadores o fecha inválidos; `401` no autenticado; `403` sin rol; `404` galpón inexistente; `503` datos clínicos o Módulo 1 no disponibles.

## Polish & Cross-Cutting Concerns

- [x] T013 Documentar que esta consulta no registra el consumo real ni descuenta inventario.
- [x] T014 Probar `401`, `403`, listado vacío y formato JSON mediante MockMvc.
- [x] T015 Ejecutar pruebas y `git diff --check`.

### Contratos integrados de otros planes

- [x] T016 El Plan 008 guarda nombre y principio activo del medicamento; el Plan 011 los conserva en el diagnóstico.
- [x] T017 El Plan 008 guarda los días ordinales de aplicación y el Plan 011 conserva el calendario para mostrar dosis cero en días de descanso.
- [x] T018 El Plan 011 permite diagnósticos tratables simultáneos para enfermedades distintas y reintegra al vencer el último.

## Dependencies & Execution Order

- **Setup** confirma las fuentes clínicas y de galpón.
- **Foundational** crea cálculo y puertos de sólo lectura.
- **US1** ofrece detalle; **US2** explicita la ausencia de medicación; **US3** agrega únicamente los resultados de los galpones seleccionados.

## Notes

- La fecha se permite como parámetro para pruebas y auditoría, con valor predeterminado del `Clock` si se omite.
- El Plan 013 es el único responsable de registrar consumo real y solicitar descuento de existencias.
- El Plan 011 conserva nombre, principio activo y calendario en la instantánea clínica. Para diagnósticos anteriores a V15, el nombre puede obtenerse de la recepción más reciente y el principio activo puede seguir siendo `null` porque no existía en el contrato antiguo.
- Un `diasAplicacion` ausente significa todos los días de la vigencia. Si la fecha consultada cae fuera del calendario, el tratamiento sigue activo y `dosisHoy` es `"0"` con `aplicaHoy: false`.
- Los tratamientos concurrentes corresponden a enfermedades distintas; el Plan 011 impide duplicar la misma enfermedad activa y el reintegro espera al último tratamiento.
