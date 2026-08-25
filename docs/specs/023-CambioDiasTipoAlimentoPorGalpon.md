# Feature Specification: Cambio y Ajuste de Días de Tipos de Alimento por Etapa y Galpón

**Created**: 2026-08-25  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ajuste Personalizado del Rango de Días de Alimentación por Galpón (Priority: P1)

Como nutricionista de la granja, quiero ajustar el rango de días de duración de las etapas de alimentación para un galpón específico (modificando la etapa actual y desplazando en cascada las etapas posteriores), cuando ocurran eventos excepcionales como cuarentena sanitaria, retraso en peso o desviaciones nutricionales, para asegurar que las aves reciban el alimento adecuado a su condición real antes de avanzar de etapa.

**Why this priority**: Es la funcionalidad central requerida por el negocio. Permite flexibilizar el calendario de alimentación estándar frente a contingencias sanitarias o productivas individuales de cada galpón.

**Independent Test**: Teniendo un galpón en el día 15 (etapa "Inicio" estándar días 8 a 21), modificar el fin de la etapa "Inicio" al día 28 por encontrarse en cuarentena. Verificar que "Inicio" cubra los días 8 a 28, que la etapa "Engorde/Broiler" se desplace automáticamente para iniciar en el día 29, y que las etapas ya transcurridas (Pre-inicio días 1 a 7) no sufran alteraciones.

**Acceptance Scenarios**:

1. **Scenario**: Ajuste de días de etapa actual con desplazamiento en cascada
   - **Given** un galpón en el día 14 de crianza con cronograma estándar: Pre-inicio (días 1-7), Inicio (días 8-21), Engorde (días 22-sacrificio)
   - **When** el nutricionista extiende la etapa "Inicio" hasta el día 26 debido a un retraso de desarrollo
   - **Then** el sistema actualiza la etapa "Inicio" para abarcar los días 8 al 26
   - **And** desplaza automáticamente el inicio de la etapa "Engorde" al día 27 manteniendo la continuidad sin solapamientos ni huecos.

2. **Scenario**: Bloqueo de edición de etapas pasadas
   - **Given** un galpón cuya edad actual es de 18 días
   - **When** el usuario intenta modificar el rango de días de la etapa "Pre-inicio" (días 1 al 7)
   - **Then** el sistema bloquea los campos de esa etapa indicando que las etapas anteriores a la edad actual del lote son inmutables.

3. **Scenario**: Validación de coherencia temporal con la edad actual del lote
   - **Given** un galpón cuya edad actual es de 15 días en etapa "Inicio"
   - **When** el nutricionista intenta fijar el fin de la etapa "Inicio" en el día 12 (anterior a la edad actual)
   - **Then** el sistema rechaza el cambio con un mensaje de error indicando que la fecha de fin de la etapa actual debe ser igual o mayor al día de vida actual de las aves.

---

### User Story 2 - Recepción de Alertas del Sistema para Intervención Nutricional (Priority: P2)

Como nutricionista, quiero que el sistema me notifique oportunamente mediante alertas cuando un galpón ingrese en cuarentena/aislamiento sanitario o presente desviaciones de bajo peso respecto a la curva de crecimiento esperada, para evaluar de forma proactiva si es necesario ajustar los días de la etapa de alimentación.

**Why this priority**: Conecta las alertas automáticas del sistema generadas por eventos veterinarios o registros de pesaje con la toma de decisiones oportuna del nutricionista.

**Independent Test**: Al cambiar el estado de un galpón a "En Cuarentena" o al registrar un pesaje de lote con desviación inferior al 10% del estándar, verificar que el sistema genere y liste una alerta nutricional pendiente con acceso directo a la edición del calendario del galpón afectado.

**Acceptance Scenarios**:

1. **Scenario**: Generación de alerta por ingreso a cuarentena
   - **Given** que un veterinario o trabajador registra el ingreso del Galpón 2 a "Aislamiento / Cuarentena"
   - **When** el sistema procesa el cambio de estado sanitario
   - **Then** genera automáticamente una alerta de alta prioridad dirigida al Nutricionista recomendando la revisión del calendario de alimentación del Galpón 2.

2. **Scenario**: Generación de alerta por detección de bajo peso
   - **Given** que se registra un pesaje de muestreo en un galpón de 14 días con peso promedio inferior al rango óptimo de la etapa
   - **When** el sistema evalúa la curva de crecimiento
   - **Then** emite una alerta nutricional de "Desviación de Peso" sugiriendo evaluar la extensión de la etapa de "Inicio".

3. **Scenario**: Atención y cierre de alerta tras el ajuste nutricional
   - **Given** una alerta activa de cuarentena para el Galpón 3
   - **When** el nutricionista ingresa al galpón desde la notificación y guarda el nuevo rango de días de la etapa
   - **Then** el sistema marca la alerta como "Atendida" y registra en la bitácora el motivo del ajuste.

---

### User Story 3 - Asignación Automática del Calendario Estándar a Nuevos Galpones (Priority: P3)

Como administrador o nutricionista, quiero que todo galpón que inicie un nuevo ciclo de crianza herede automáticamente la plantilla estándar de alimentación por días, para que no requiera configuración manual inicial a menos que surjan excepciones.

**Why this priority**: Establece el comportamiento por defecto del sistema garantizando la continuidad operativa desde el día 1 de recepción del lote.

**Independent Test**: Crear o poblar un nuevo galpón en día 1 y verificar que su calendario de alimentación inicial quede configurado exactamente con: Pre-inicio (1-7), Inicio (8-21) y Engorde/Broiler (22-sacrificio).

**Acceptance Scenarios**:

1. **Scenario**: Inicialización exitosa de ciclo estándar
   - **Given** la recepción de un nuevo lote de pollitos en el Galpón 1 (Día 1)
   - **When** se activa el galpón
   - **Then** el sistema asigna automáticamente las etapas:
     - Etapa 1: "Pre-inicio" (Día 1 al Día 7) -> Alimento Pre-iniciador
     - Etapa 2: "Inicio" (Día 8 al Día 21) -> Alimento Iniciador
     - Etapa 3: "Engorde / Broiler" (Día 22 hasta Sacrificio) -> Alimento Engorde/Finalizador.

---

### Edge Cases

- **Etapa extendida que sobrepasa la edad proyectada de sacrificio**: Si el nutricionista extiende una etapa previa más allá de la fecha estimada de sacrificio (ej. extender Inicio hasta el día 45 cuando el sacrificio estaba previsto para el día 42), el sistema debe emitir una advertencia solicitando confirmar o extender también la fecha proyectada de salida/sacrificio del lote.
- **Reducción de días de etapa**: Si un lote presenta crecimiento acelerado y se desea acortar una etapa (ej. finalizar Inicio en el día 18 en lugar del 21), el sistema debe permitirlo siempre y cuando el día final sea mayor o igual al día actual de vida del galpón, desplazando el inicio de la siguiente etapa al día 19.
- **Galpón con múltiples alertas concurrentes**: Si un galpón entra en cuarentena y además tiene bajo peso, el sistema debe consolidar las alertas bajo el mismo galpón para evitar duplicidad de intervenciones.
- **Alimento físico en tolva/galpón al momento del cambio**: Si se cambia la duración de la etapa mientras el galpón tiene alimento remanente en inventario local, el sistema actualizará los cálculos de demanda teórica para el tipo de alimento de la etapa prolongada.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE manejar una plantilla estándar de alimentación por defecto basada en la edad del lote:
  - Días 1 al 7: Etapa `Pre-inicio` (Alimento Pre-iniciador).
  - Días 8 al 21: Etapa `Inicio` (Alimento Iniciador).
  - Días 22 hasta el Sacrificio: Etapa `Engorde / Broiler` (Alimento Engorde/Finalizador).
- **FR-002**: El sistema DEBE asignar automáticamente el calendario estándar a todo galpón que inicie un nuevo ciclo de producción.
- **FR-003**: El sistema DEBE permitir a los usuarios con rol de Nutricionista personalizar y modificar el rango de días de duración de las etapas de alimentación para un galpón específico.
- **FR-004**: Al modificar el día final de una etapa en un galpón, el sistema DEBE aplicar un desplazamiento automático en cascada, recalculando el inicio de la siguiente etapa como `DiaInicio = DiaFinEtapaAnterior + 1` para asegurar continuidad sin solapamientos ni días sin asignar.
- **FR-005**: El sistema DEBE restringir la edición de etapas pasadas (aquellas cuyo día final es menor al día actual de vida del lote), permitiendo modificar únicamente la etapa activa y las etapas futuras.
- **FR-006**: El sistema DEBE validar que el día de finalización de la etapa activa sea mayor o igual al día actual de vida de las aves del galpón.
- **FR-007**: El sistema DEBE generar alertas automáticas dirigidas al Nutricionista en los siguientes eventos:
  - Ingreso de un galpón al estado de "Aislamiento / Cuarentena".
  - Registro de peso promedio del lote por debajo del estándar técnico para la edad.
  - Alertas veterinarias o sanitarias que afecten el galpón.
- **FR-008**: Cada ajuste de días por galpón DEBE requerir el registro de una justificación/motivo (ej. "Cuarentena sanitaria", "Retraso en peso", "Ajuste técnico") y el usuario responsable.
- **FR-009**: El sistema DEBE mantener un historial inmutable de auditoría con todas las versiones y ajustes del calendario de alimentación de cada galpón.
- **FR-010**: El cálculo de requerimiento diario y despacho de alimento de un galpón DEBE basarse estrictamente en la etapa y tipo de alimento vigentes según el calendario ajustado del galpón y su día de vida actual.

### Key Entities

- **PlantillaCalendarioEstandar**: Configuración global del ciclo de alimentación.
  - *Atributos*: ID, nombre ("Estándar Broiler"), lista de etapas (Pre-inicio [1-7], Inicio [8-21], Engorde [22-fin]).
- **CalendarioAlimentacionGalpon**: Calendario específico y personalizado asignado a un galpón/lote.
  - *Atributos*: ID, galponId, loteAvesId, fechaInicioCiclo, estado (Vigente, Finalizado), version.
- **EtapaAlimentacionGalpon**: Cada rango de días configurado dentro del calendario del galpón.
  - *Atributos*: ID, calendarioId, nombreEtapa (Pre-inicio, Inicio, Engorde), diaInicio, diaFin, tipoAlimentoAsociado, esModificado.
- **AlertaNutricionalGalpon**: Notificación generada por el sistema para el nutricionista.
  - *Atributos*: ID, galponId, tipoAlerta (CUARENTENA, BAJO_PESO, CRITERIO_VETERINARIO), fechaGeneracion, estado (PENDIENTE, ATENDIDA, DESCARTADA), descripcion.
- **HistorialAjusteCalendario**: Auditoría de cambios aplicados a un galpón.
  - *Atributos*: ID, calendarioId, etapaModificada, diaFinAnterior, diaFinNuevo, motivo, usuarioNutricionista, timestamp.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% de los desplazamientos de etapas posteriores se calculan automáticamente sin solapamientos de fechas ni días desprovistos de etapa.
- **SC-002**: 0% de modificaciones permitidas sobre etapas ya transcurridas en el historial del galpón.
- **SC-003**: Las alertas de cuarentena y bajo peso se generan y notifican al nutricionista en menos de 5 segundos tras el registro del evento desencadenante.
- **SC-004**: El nutricionista puede completar el ajuste de días de un galpón y registrar el motivo en menos de 20 segundos.
