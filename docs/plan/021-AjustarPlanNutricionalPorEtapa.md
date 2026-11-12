# Implementation Plan: Ajustar plan nutricional por etapa

**Date**: 05/11/2026
**Spec**: [021-AjustarPlanNutricionalPorEtapa.md](../specs/021-AjustarPlanNutricionalPorEtapa.md)
**Arquitectura y tecnologías**: [General.md](General.md)

## Summary

Gestionar plantillas maestras y planes desacoplados por lote/galpón; seleccionar alimentos compatibles para etapas futuras y ajustar duración de la etapa activa con trazabilidad. Las etapas completadas y los insumos/costos de la activa son inmutables; los cambios recalculan la cascada y la salida proyectada sin reservar ni descontar inventario.

## Technical Context

**Performance Goals**: guardar ajustes y recalcular el calendario de forma consistente en un segundo.
**Constraints**: `ROLE_NUTRICIONISTA`; un predeterminado por tipo de ave; rangos continuos; ración positiva; sin movimientos de inventario.
**Scale/Scope**: plantillas, instancias, sustitución, alimentos y ajustes auditados; no despacho ni consumo físico.

## Project Structure

### Documentation (this feature)

```text
docs/
├── specs/
│   └── 021-AjustarPlanNutricionalPorEtapa.md
└── plan/
    └── 021-AjustarPlanNutricionalPorEtapa.md
```

### Source Code (repository root)

```text
src/main/java/com/avicontrol/
├── domain/model/nutricion/
│   ├── AuditoriaPlanNutricional.java
│   ├── PlanNutricionalGalpon.java
│   └── PlanNutricionalPlantilla.java
├── domain/port/out/nutricion/
│   ├── AlimentoComercialRepositoryPort.java
│   ├── AlimentoCompatibleQueryPort.java
│   ├── PlanNutricionalGalponRepositoryPort.java
│   └── PlanNutricionalPlantillaRepositoryPort.java
├── application/nutricion/
│   ├── GestionarPlantillasUseCase.java
│   ├── PlanNutricionalLoteUseCase.java
│   └── RegistrarAlimentoComercialUseCase.java
└── infrastructure/
    ├── adapter/in/rest/nutricion/
    │   ├── AlimentoComercialController.java
    │   ├── PlanNutricionalGalponController.java
    │   └── PlanNutricionalPlantillaController.java
    └── adapter/out/persistence/nutricion/
        ├── AlimentoComercialPersistenceAdapter.java
        ├── AlimentoCompatibleQueryAdapter.java
        ├── PlanNutricionalGalponPersistenceAdapter.java
        └── PlanNutricionalPlantillaPersistenceAdapter.java

src/main/resources/db/migration/
└── V7__crear_planes_y_proyecciones_alimento.sql

src/test/java/com/avicontrol/
├── application/alimentacion/NutricionInventarioIntegrationTest.java
├── domain/model/nutricion/PlanNutricionalPlantillaTest.java
└── infrastructure/adapter/in/rest/inventario/InventarioNutricionControllerTest.java
```

**Structure Decision**: la plantilla es catálogo y la instancia por lote es independiente. La etapa activa conserva alimento, ración y costo; ajustes se anexan al historial y sólo cambian duración/cascada.

## Phase 1: Setup (Shared Infrastructure)

- [x] T001 Confirmar tipos de ave, catálogo de alimentos, stock de sólo lectura y sincronización con Plan 022.
- [x] T002 Crear migraciones para plantilla, instancia, etapas e historial de ajustes.
- [x] T003 Definir enums, estados y pruebas de etapas omitidas, activas y completadas.

## Phase 2: Foundational (Blocking Prerequisites)

- [x] T004 Validar rangos contiguos, ración positiva y unicidad de predeterminado por tipo de ave.
- [x] T005 Implementar reglas de inmutabilidad para etapa completada y alimento/costo de etapa activa.
- [x] T006 Implementar recálculo en cascada y actualización de fecha proyectada de salida.
- [x] T007 Persistir cada ajuste con motivo, justificación, responsable y valores antes/después.

**Checkpoint**: el dominio no permite solapamientos ni cambios históricos, y nunca descuenta inventario.

## Phase 3: User Story 1 - Gestionar plantillas maestras (Priority: P1)

**Goal**: crear y editar curvas estándar sin alterar instancias activas.

**Independent Test**: tres etapas 1-7, 8-21 y 22-45 se aceptan; una etapa solapada o ración cero se rechaza; otro predeterminado se desmarca.

- [x] T008 [P] [US1] Probar continuidad, ración y predeterminado único.
- [x] T009 [US1] Implementar creación, listado, detalle y edición de plantillas.

## Phase 4: User Story 2 - Asignar o sustituir plan (Priority: P1)

**Goal**: crear instancia al alojar lote y sustituir sólo etapas futuras.

**Independent Test**: ingreso en día 14 omite Pre-inicio; sustitución en día 25 conserva etapas concluidas y bloqueos de activa.

- [x] T010 [P] [US2] Probar asignación automática, ausencia de predeterminado y sustitución segura.
- [x] T011 [US2] Implementar asignación y sustitución por lote activo.

## Phase 5: User Story 3 - Seleccionar alimento compatible (Priority: P1)

**Goal**: configurar alimento para etapa no iniciada, con stock informativo y recálculo opcional de duración.

**Independent Test**: alimento con duración recomendada de diez días reajusta una etapa futura; sin stock se permite con alerta; en etapa activa se rechaza.

- [x] T012 [P] [US3] Probar compatibilidad, presentaciones, vencidos, sin stock y bloqueo activo.
- [x] T013 [US3] Implementar consulta y selección de alimento futuro.

## Phase 6: User Story 4 - Ajustar duración activa (Priority: P1)

**Goal**: ampliar o acortar días con justificación, conservando alimento y desplazando las etapas siguientes.

**Independent Test**: extender Inicio de 14 a 19 días desplaza Engorde cinco días, actualiza fecha de salida y añade una entrada de historial.

- [x] T014 [P] [US4] Probar cascada, ajustes sucesivos, justificación y etapa completada.
- [x] T015 [US4] Implementar ajuste, auditoría y notificación/recalculo al Plan 022.
- [x] T016 [US3] Calcular demanda diaria en kg y bultos equivalentes, y días de autonomía con stock vigente; demanda cero no divide por cero.

## Endpoints

| Método y ruta | Rol | Solicitud | Respuesta |
| --- | --- | --- | --- |
| `POST /api/nutricion/plantillas` | `ROLE_NUTRICIONISTA` | plantilla con etapas | `201 Created` |
| `POST /api/nutricion/alimentos` | `ROLE_NUTRICIONISTA` | producto comercial y perfil | `201 Created`; permite cargar el catálogo local para revisión |
| `PUT /api/nutricion/plantillas/{id}` | `ROLE_NUTRICIONISTA` | plantilla con etapas | `200 OK` |
| `GET /api/nutricion/plantillas` | nutricionista, administrador | sin cuerpo | `200 OK` con plantillas |
| `GET /api/nutricion/plantillas/{id}` | nutricionista, administrador | sin cuerpo | `200 OK` con plantilla y etapas |
| `GET /api/galpones/{galponId}/plan-nutricional` | nutricionista, administrador | sin cuerpo | `200 OK` |
| `PUT /api/galpones/{galponId}/plan-nutricional/asignacion` | `ROLE_NUTRICIONISTA` | plantilla y lote vigentes | `200 OK` con plan asignado; resuelve estado pendiente |
| `PUT /api/galpones/{galponId}/plan-nutricional/sustituir` | `ROLE_NUTRICIONISTA` | nuevo plan | `200 OK` |
| `GET /api/nutricion/alimentos-compatibles` | nutricionista, administrador | filtros | `200 OK` |
| `PUT /api/galpones/{galponId}/plan-nutricional/etapas/{etapaId}/alimento` | `ROLE_NUTRICIONISTA` | alimento | `200 OK` |
| `PUT /api/galpones/{galponId}/plan-nutricional/etapas/{etapaId}/ajuste-duracion` | `ROLE_NUTRICIONISTA` | ajuste | `200 OK` |

```json
{
  "nuevoDiaFin": 28,
  "motivo": "BAJO_PESO",
  "justificacion": "Extender recuperación"
}
```

La creación y edición de plantillas usan este cuerpo; listado y detalle devuelven la misma estructura por plantilla:

```json
{
  "nombre": "Plan Estándar Broiler",
  "tipoAve": "BROILER",
  "esPredeterminado": true,
  "etapas": [
    {
      "etapaCrianza": "PRE_INICIO",
      "diaInicio": 1,
      "diaFin": 7,
      "racionKgAveDia": 0.0250,
      "tipoAlimento": "PRE_INICIO",
      "alimentoId": null,
      "pesoNominalPorBultoKg": 40
    },
    {
      "etapaCrianza": "INICIO",
      "diaInicio": 8,
      "diaFin": 21,
      "racionKgAveDia": 0.0800,
      "tipoAlimento": "INICIO",
      "alimentoId": "550e8400-e29b-41d4-a716-446655440000",
      "pesoNominalPorBultoKg": 40
    },
    {
      "etapaCrianza": "ENGORDE",
      "diaInicio": 22,
      "diaFin": 45,
      "racionKgAveDia": 0.1200,
      "tipoAlimento": "ENGORDE",
      "alimentoId": null,
      "pesoNominalPorBultoKg": 40
    }
  ]
}
```

Registro de alimento comercial para revisión local (el identificador debe estar habilitado también en el catálogo de recepciones):

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "nombre": "Alimento Inicio Broiler",
  "tipoAlimento": "INICIO",
  "pesoNominalPorBultoKg": 40,
  "proteinaPorcentaje": 20.0,
  "energiaKcalKg": 3100,
  "duracionRecomendadaEtapaDias": 14
}
```

Asignación manual local cuando el galpón quedó pendiente. El contexto del lote se reemplazará por la consulta confiable del Módulo 1 al integrarlo:

```json
{
  "plantillaId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "loteId": "1f6f2df9-24af-4f12-8671-cd1cfbdf2020",
  "codigoLote": "LDP-005",
  "tipoAve": "BROILER",
  "fechaIngreso": "2026-11-05",
  "edadIngresoDias": 8,
  "poblacionViva": 10000,
  "justificacion": "Asignación nutricional inicial"
}
```

Sustitución de etapas futuras:

```json
{
  "plantillaId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "justificacion": "Ajuste del programa nutricional"
}
```

Consulta de alimentos compatibles (`GET /api/nutricion/alimentos-compatibles?tipoAlimento=INICIO`) y selección de una presentación futura:

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "nombre": "Alimento Inicio Broiler",
    "tipoAlimento": "INICIO",
    "pesoNominalPorBultoKg": 40,
    "proteinaPorcentaje": 20.0,
    "energiaKcalKg": 3100,
    "duracionRecomendadaEtapaDias": 14,
    "stockDisponibleKg": 800,
    "stockDisponibleBultos": 20,
    "estadoStock": "DISPONIBLE"
  }
]
```

```json
{
  "alimentoId": "550e8400-e29b-41d4-a716-446655440000"
}
```

La consulta del plan devuelve etapas, historial y, para la etapa activa, demanda diaria en kg y bultos y días de autonomía:

```json
{
  "galponId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "loteId": "1f6f2df9-24af-4f12-8671-cd1cfbdf2020",
  "estado": "ACTIVO",
  "fechaSalidaProyectada": "2026-12-12",
  "etapas": [
    {
      "id": "edb5bd75-0a93-4347-a3fb-2e5e7080bd48",
      "etapaCrianza": "INICIO",
      "estadoEtapa": "ACTIVA",
      "diaInicio": 8,
      "diaFin": 21,
      "racionKgAveDia": 0.0800,
      "alimentoId": "550e8400-e29b-41d4-a716-446655440000",
      "demandaDiariaKg": 800,
      "demandaDiariaBultos": 20,
      "diasAutonomia": 1
    }
  ],
  "historialAjustes": []
}
```

**Errores**: `400` ración, calendario no contiguo o justificación inválidos; `401` no autenticado; `403` sin rol; `404` plan o etapa; `409` etapa activa/completada incompatible.

## Polish & Cross-Cutting Concerns

- [x] T017 Documentar fórmulas de demanda y que no existen reservas ni movimientos de inventario.
- [x] T018 Ejecutar pruebas de dominio, HTTP y `git diff --check`.

## Dependencies & Execution Order

- **Foundational** bloquea las cuatro historias.
- **US1** provee plantillas; **US2** crea instancias; **US3** configura futuras; **US4** ajusta la activa.

## Notes

- La demanda diaria es población viva × ración kg/ave/día; los bultos equivalentes dividen los kilogramos por el peso nominal, y la autonomía divide el stock vigente por la demanda diaria cuando ésta es positiva. Son cálculos de lectura: no reservan ni descuentan inventario.
- Sin predeterminado, el lote queda pendiente de asignación y demanda cero hasta configuración manual.
- El Plan 022 consume proyecciones e historial, sin permitir reescribir costos bloqueados.
- El Módulo 1 aún no está conectado: `PlanNutricionalLoteUseCase.asignarAlAlojar` recibe el alojamiento confirmado y publica el evento de pendiente cuando falta plantilla; el endpoint de asignación manual acepta contexto del lote para revisión local. La integración definitiva deberá sustituir ese contexto por una consulta confiable al Módulo 1.
- El catálogo `alimentos_comerciales_nutricion` conserva producto, perfil y duración. El endpoint de carga permite revisarlo localmente; el catálogo compartido deberá sustituirlo o sincronizarlo al integrar el sistema.
