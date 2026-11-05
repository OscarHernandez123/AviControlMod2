# Plan: Ajustar plan nutricional por etapa

**Spec**: [021-AjustarPlanNutricionalPorEtapa.md](../specs/021-AjustarPlanNutricionalPorEtapa.md)  
**Arquitectura y tecnologías**: [General.md](General.md)

## Alcance

Plantillas maestras, asignación por galpón, alimento compatible, sustitución y ajuste auditado de duración de etapa.

## Fuera de alcance

Salidas de bodega, suministros diarios, proyección financiera y cobertura.

## Source Code (repository root)

```text
src/main/java/com/avicontrol/domain/model/nutricion/{PlanNutricionalPlantilla,PlanNutricionalGalpon,CalendarioEtapaGalpon,HistorialAjusteEtapa,RacionDiaria}.java
src/main/java/com/avicontrol/application/nutricion/{CrearPlantillaPlanNutricionalUseCase,AsignarPlanNutricionalUseCase,AjustarDuracionEtapaUseCase}.java
src/main/java/com/avicontrol/infrastructure/adapter/in/rest/nutricion/{PlanNutricionalPlantillaController,PlanNutricionalGalponController}.java
src/main/resources/db/migration/V__crear_planes_nutricionales.sql
```

## User Stories y endpoints

| Ruta | Método | Rol |
| --- | --- | --- |
| `/api/nutricion/plantillas` | POST, GET | NUTRICIONISTA / ADMINISTRADOR lectura |
| `/api/nutricion/plantillas/{id}` | PUT, GET | NUTRICIONISTA / ADMINISTRADOR lectura |
| `/api/galpones/{galponId}/plan-nutricional` | GET | NUTRICIONISTA, ADMINISTRADOR |
| `/api/galpones/{galponId}/plan-nutricional/sustituir` | PUT | NUTRICIONISTA |
| `/api/nutricion/alimentos-compatibles` | GET | NUTRICIONISTA, ADMINISTRADOR |
| `/api/galpones/{galponId}/plan-nutricional/etapas/{etapaId}/alimento` | PUT | NUTRICIONISTA |
| `/api/galpones/{galponId}/plan-nutricional/etapas/{etapaId}/ajuste-duracion` | PUT | NUTRICIONISTA |
| `/api/galpones/{galponId}/plan-nutricional/etapas/{etapaId}/historial-ajustes` | GET | NUTRICIONISTA, ADMINISTRADOR |

```json
{"nuevoDiaFin":28,"motivo":"BAJO_PESO","justificacion":"Extender recuperación"}
```

Errores: `404` plan/etapa, `409` etapa activa/completada incompatible, `422` calendario no contiguo.

## Definition of Done

- Plantilla e instancia separadas; etapa activa/completada inmutable.
- Ajuste en cascada auditado, eventos y pruebas HTTP/dominio verdes.
