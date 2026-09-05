# Feature Specification: Ajuste de Plan Nutricional y Ración Diaria por Etapa

**Created**: 2026-08-25  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Configuración de la Ración Diaria por Etapa de Crianza (Priority: P1)

Como nutricionista de la granja, quiero ingresar y ajustar la cantidad de alimento en kilogramos que necesita un pollo al día para cada etapa de crianza (ej. Inicio, Crecimiento, Finalización/Engorde) asociándolo al tipo de alimento correspondiente disponible en inventario, para garantizar que la alimentación cubra los requerimientos nutricionales específicos de cada fase.

**Why this priority**: Es la funcionalidad central y base de la regla de negocio solicitada. Permite al especialista en nutrición definir la dosificación exacta (kg/pollo/día) sin depender de cálculos manuales dispersos.

**Independent Test**: Se puede probar registrando o actualizando el valor de ración diaria (en kg) para una etapa determinada (ej. 0.045 kg/pollo/día para "Inicio" con "Alimento Iniciador"), verificando que el valor quede persistido, validado y disponible para los cálculos del sistema.

**Acceptance Scenarios**:

1. **Scenario**: Registro exitoso de ración diaria para una etapa de crianza
   - **Given** que el usuario con rol de Nutricionista accede al módulo de Plan Nutricional
   - **When** ingresa una ración de `0.045` kg/pollo/día para la etapa "Inicio" asociada al tipo de alimento "Alimento Iniciador" (con peso estándar de 40 kg/bulto)
   - **Then** el sistema valida que el valor sea positivo, guarda la configuración del plan nutricional y confirma la actualización para dicha etapa.

2. **Scenario**: Rechazo de ración no válida (cero o negativa)
   - **Given** el formulario de configuración de ración por etapa
   - **When** el nutricionista ingresa un valor menor o igual a cero (ej. `0.0` o `-0.05` kg)
   - **Then** el sistema rechaza la operación y muestra un mensaje de error indicando que la ración diaria por ave debe ser un número estrictamente positivo.

---

### User Story 2 - Proyección de Cobertura de Inventario y Alertas de Disponibilidad (Priority: P3)

Como nutricionista o administrador, quiero visualizar los días de cobertura de alimento restantes en el inventario para cada galpón en función de la ración diaria y el stock actual de bultos, para anticipar compras y evitar el desabastecimiento.

**Why this priority**: Brinda valor preventivo y optimización de compras, asegurando que el plan nutricional sea sostenible con el stock existente.

**Independent Test**: Con un stock de 80 bultos y una demanda diaria calculada de 8 bultos/día para un galpón, verificar que el sistema indique exactamente 10 días de autonomía de alimento.

**Acceptance Scenarios**:

1. **Scenario**: Cálculo correcto de días de autonomía
   - **Given** que un galpón requiere `10 bultos/día` del tipo de alimento "Alimento Finalizador"
   - **And** el inventario disponible en almacén para ese tipo de alimento es de `50 bultos`
   - **Then** el sistema reporta `5 días` de cobertura estimada de alimento.

2. **Scenario**: Alerta por inventario insuficiente según el plan nutricional
   - **Given** que un galpón requiere `10 bultos/día`
   - **And** el inventario disponible es inferior a la demanda de los próximos 3 días (ej. 15 bultos disponibles)
   - **Then** el sistema genera una advertencia visual de inventario crítico para la etapa activa.

---

### Edge Cases

- **Galpón con población cero**: Si un galpón no tiene aves activas asignadas (vacío o en descanso sanitario), la demanda calculada en kg y bultos debe ser `0.0` sin generar errores de división por cero.
- **Raciones con múltiples decimales**: El sistema debe aceptar precisiones de hasta 4 decimales en kilogramos (ej. `0.0235` kg = 23.5 gramos/ave/día) para reflejar con exactitud las etapas iniciales de pollitos.
- **Peso estándar del bulto no definido o cero**: Si un tipo de alimento no tiene especificado el peso neto por bulto, el sistema debe exigir su configuración (mayor a 0 kg) antes de realizar conversiones a bultos de inventario.
- **Cambio de etapa del galpón**: Al realizar la transición de etapa de un galpón (ej. de Inicio a Crecimiento), el sistema debe adoptar inmediatamente la ración configurada para la nueva etapa.
- **Fracciones de bultos requeridos**: Cuando la demanda diaria no sea un número entero de bultos (ej. 11.25 bultos), el sistema debe mostrar el valor exacto fraccionario y el total neto en kilogramos.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir a los usuarios con rol de Nutricionista registrar y actualizar la cantidad de ración en kilogramos que requiere un pollo por día (`kg/pollo/día`) para cada etapa de crianza (Inicio, Crecimiento, Finalización/Engorde, etc.).
- **FR-002**: El sistema DEBE asociar cada etapa de crianza con su respectivo Tipo de Alimento compatible configurado en el catálogo.
- **FR-003**: El catálogo de `TipoAlimento` DEBE incluir el peso neto estándar en kilogramos por bulto (ej. 40 kg o 50 kg) y una descripción/resumen del valor nutricional (ej. % de proteína cruda, energía estimada o notas técnicas).
- **FR-004**: El sistema DEBE validar que la ración diaria por ave sea un valor numérico positivo mayor a 0 (admitiendo hasta 4 cifras decimales).
- **FR-005**: El sistema DEBE calcular automáticamente la demanda diaria en kilogramos para un galpón mediante la fórmula: `DemandaKg = PoblacionAves * RacionKgPorPolloDia`.
- **FR-006**: El sistema DEBE calcular la demanda equivalente en bultos de inventario mediante la fórmula: `DemandaBultos = DemandaKg / PesoNetoKgPorBulto`.
- **FR-007**: El sistema DEBE permitir proyectar la demanda acumulada en kg y bultos para periodos configurables (ej. diario, semanal o duración total estimada de la etapa).
- **FR-008**: El sistema DEBE calcular los días de autonomía/cobertura de alimento comparando el stock disponible del tipo de alimento en inventario contra la demanda diaria del galpón.
- **FR-009**: El sistema DEBE mantener un registro auditable e histórico de las modificaciones realizadas al plan nutricional (fecha, usuario nutricionista, etapa, valor anterior y nuevo valor de ración).

### Key Entities

- **PlanNutricionalEtapa**: Representa la regla de ración alimenticia configurada por etapa.
  - *Atributos*: ID, etapaCrianza (Inicio, Crecimiento, Finalización), racionKgPolloDia, tipoAlimento, fechaActualizacion, usuarioNutricionista.
- **TipoAlimento**: Representa el alimento comercial en almacén (ampliación de la entidad base).
  - *Atributos*: ID, nombre, etapaAsociada, pesoNetoKgPorBulto, descripcionNutricional, unidadMedida (Bultos).
- **Galpon**: Representa el galpón con su población y estado de ciclo.
  - *Atributos*: ID, nombre, capacidad, poblacionAvesActivas, etapaActual.
- **ProyeccionConsumoGalpon**: Modelo de cálculo en tiempo de consulta.
  - *Atributos*: galpon, etapaActual, poblacionAves, demandaDiariaKg, demandaDiariaBultos, stockDisponibleBultos, diasCoberturaRestantes.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% de los cálculos de demanda diaria (kg y bultos) son exactos y consistentes en función de la población del galpón y la ración configurada.
- **SC-002**: El nutricionista puede consultar, ajustar y guardar la ración de cualquier etapa en menos de 15 segundos a través de la interfaz.
- **SC-003**: 0% de registros con valores de ración negativos, en cero o incompatibles con la etapa de crianza.
- **SC-004**: Los operarios y encargados pueden visualizar inmediatamente la equivalencia en bultos a despachar por galpón sin necesidad de conversiones manuales.
