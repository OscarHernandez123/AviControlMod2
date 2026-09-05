# Feature Specification: Registrar mortalidad anormal

**Created**: 2026-09-05  

## User Scenarios & Testing 

### User Story 1 - Registrar evento de mortalidad anormal e invocar alerta sanitaria (Priority: P1)

Como trabajador u operario de granja, quiero registrar una novedad de mortalidad anormal en mi galpón asignado cuando detecte una cantidad atípica de bajas o signos clínicos sospechosos, para que el sistema almacene el evento e invoque la generación de una alerta sanitaria inmediata.

**Why this priority**: Es la acción de bioseguridad más urgente ante sospechas epidemiológicas o muertes masivas. Permite documentar el evento crítico y alertar sin demora al equipo veterinario y administrativo.

**Independent Test**: Se puede probar seleccionando un galpón asignado con lote activo, registrando un evento de mortalidad anormal con síntomas clínicos y causa sospechosa, y verificando que el sistema guarde el registro e invoque el caso de uso incluido de generar alerta sanitaria.

**Acceptance Scenarios**:

1. **Scenario**: Registro exitoso de mortalidad anormal por síntomas sospechosos o mortalidad atípica
   - **Given** que un trabajador autenticado observa una situación atípica en su galpón asignado con lote activo
   - **When** ingresa la fecha y hora del evento, la cantidad de muertes anormales, la causa sospechosa, los síntomas/signos clínicos observados y sus observaciones
   - **Then** el sistema almacena el registro de mortalidad anormal con su UUID único, datos clínicos y referencias de galpón y lote
   - **And** invoca el caso de uso incluido (`<<include>>`) *Generar alerta sanitaria*

2. **Scenario**: Intento de registro con cantidad de bajas mayor a la población viva
   - **Given** un lote activo con una población actual de aves vivas conocida
   - **When** el trabajador intenta registrar una cantidad de muertes anormales que supera dicha población
   - **Then** el sistema rechaza el registro, informa que la cantidad supera las aves vivas y no crea el registro

3. **Scenario**: Intento de registro en un galpón no asignado al trabajador
   - **Given** que un trabajador intenta registrar una mortalidad anormal en un galpón que no tiene a su cargo
   - **When** solicita confirmar el registro
   - **Then** el sistema bloquea la operación y no almacena ningún registro

---

### User Story 2 - Cálculo automático del porcentaje de mortalidad y severidad (Priority: P2)

Como trabajador u operario de granja, quiero que el sistema calcule automáticamente el porcentaje diario de mortalidad respecto a la población viva del lote y determine el nivel de severidad, para dimensionar con exactitud el impacto sanitario del evento.

**Why this priority**: Proporciona datos cuantitativos objetivos e inmediatos para que los veterinarios y administradores prioricen la respuesta ante emergencias graves.

**Independent Test**: Se puede probar registrando un número de muertes sobre una población conocida y verificando que el sistema calcule exactamente el porcentaje (`muertes ÷ población viva × 100`) y clasifique la severidad del evento.

**Acceptance Scenarios**:

1. **Scenario**: Cálculo exacto del porcentaje de mortalidad anormal
   - **Given** un lote activo con 5.000 aves vivas
   - **When** el trabajador registra 150 muertes anormales súbitas
   - **Then** el sistema calcula y muestra un porcentaje de mortalidad del 3.0 % (`150 ÷ 5.000 × 100`)
   - **And** asigna el nivel de severidad correspondiente para la alerta sanitaria

---

### Edge Cases

- **¿Qué sucede si un galpón no cuenta con un lote activo registrado?**
  - El sistema bloquea el registro de mortalidad anormal e informa que el galpón no tiene un lote activo para reportar novedades sanitarias.

- **¿Qué sucede si la cantidad de muertes anormales ingresada es menor o igual a cero?**
  - El sistema rechaza el registro y exige un número entero positivo mayor que cero.

- **¿Qué sucede si el trabajador omite registrar los síntomas o la causa sospechosa?**
  - El sistema bloquea la confirmación del registro y exige describir obligatoriamente los síntomas/signos observados y la causa sospechosa.

- **¿Cómo valida el sistema la fecha y hora digitadas por el trabajador?**
  - El sistema valida que la fecha y hora del evento no sean posteriores al momento actual del servidor, ni anteriores a la fecha de ingreso del lote.

- **¿Se permite anular o editar un registro de mortalidad anormal una vez confirmado?**
  - No, el registro es inmutable para asegurar la estricta trazabilidad de eventos sanitarios en la granja.

- **¿Qué sucede si el galpón se encuentra en estado "vaciado sanitario", "mantenimiento" o "disponible"?**
  - El sistema rechaza el registro informando que el galpón no se encuentra en un estado productivo con aves alojadas.

---

## Requirements 

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro de mortalidad anormal a usuarios autenticados con rol de trabajador (y administradores autorizados).
- **FR-002**: El sistema DEBE restringir el registro de mortalidad anormal exclusivamente a los galpones que el trabajador tenga formalmente asignados a su cargo.
- **FR-003**: El sistema DEBE identificar el lote activo del galpón mediante la llave foránea almacenada en la entidad Lote, seleccionando el lote con la fecha de ingreso más reciente.
- **FR-004**: Para registrar la mortalidad anormal, el sistema DEBE exigir obligatoriamente: identificador del galpón, fecha y hora del evento, cantidad de muertes anormales, causa sospechosa, síntomas/signos clínicos observados y observaciones del trabajador.
- **FR-005**: El sistema DEBE validar que la cantidad de muertes anormales sea un número entero mayor que cero y menor o igual a la población actual del lote activo.
- **FR-006**: El sistema DEBE calcular automáticamente el porcentaje de mortalidad respecto a la población actual del lote mediante la fórmula: `(Cantidad de muertes anormales ÷ Población actual) × 100`.
- **FR-007**: El sistema DEBE validar que la fecha y hora del evento no sean posteriores a la fecha actual del sistema ni anteriores a la fecha de ingreso del lote.
- **FR-008**: Al confirmar el registro, el sistema DEBE almacenar una entrada de mortalidad anormal con UUID único, fecha y hora del evento, fecha y hora de registro, cantidad de muertes, porcentaje calculado, causa sospechosa, síntomas clínicos, nivel de severidad, observaciones, llave foránea del lote, llave foránea del galpón y usuario responsable.
- **FR-009**: Al confirmar el registro de mortalidad anormal, el sistema DEBE invocar el caso de uso incluido (`<<include>>`) *Generar alerta sanitaria*, transmitiendo el detalle del evento anormal, galpón y lote afectado.
- **FR-010**: El sistema DEBE garantizar que los registros de mortalidad anormal sean inmutables y no puedan ser modificados ni eliminados.
- **FR-011**: El sistema NO DEBE permitir registrar mortalidad anormal en galpones que no cuenten con un lote activo o que se encuentren en estados no productivos (disponible, vaciado sanitario o mantenimiento).

### Key Entities 

- **Galpón**: Representa la unidad física de producción avícola donde ocurre la novedad sanitaria.
  - Atributos utilizados: `UUID único`, `Nombre`, `Aforo máximo`, `Estado`.
- **Lote**: Representa el grupo de aves registrado que referencia al galpón mediante llave foránea.
  - Atributos utilizados: `UUID único`, `Nombre`, `Población actual` (usada para validar y calcular el porcentaje de mortalidad), `Fecha de ingreso`, `Llave foránea del galpón`.
- **Registro de mortalidad anormal**: Representa el reporte de emergencia sanitaria registrado por el trabajador.
  - Atributos: `UUID único`, `Fecha y hora del evento`, `Fecha y hora de registro`, `Cantidad de muertes anormales`, `Porcentaje de mortalidad (%)`, `Causa sospechosa`, `Síntomas y signos clínicos observados`, `Nivel de severidad`, `Observaciones`, `Llave foránea del lote`, `Llave foránea del galpón`, `Usuario responsable`.
- **Trabajador / Operario de granja**: Usuario autenticado responsable del reporte y detección de anomalías en el galpón.

---

## Success Criteria 

### Measurable Outcomes

- **SC-001**: El 100 % de los registros de mortalidad anormal confirmados almacena todos los campos obligatorios (fecha/hora evento, cantidad, causa sospechosa, síntomas clínicos, lote, galpón y usuario).
- **SC-002**: El 100 % de los registros de mortalidad anormal confirmados invoca exitosamente el caso de uso incluido (`<<include>>`) *Generar alerta sanitaria*.
- **SC-003**: En el 100 % de los registros, el porcentaje de mortalidad se calcula con exactitud respecto a la población viva actual del lote.
- **SC-004**: El 100 % de los intentos de registro con cantidades superiores a la población viva del lote o en galpones sin lote activo es rechazado por el sistema.
- **SC-005**: El 100 % de los intentos de registro en galpones no asignados al trabajador es bloqueado por el sistema.
- **SC-006**: El 100 % de los registros de mortalidad anormal permanece inmutable en el sistema para fines de trazabilidad y auditoría sanitaria.

---

## Out of Scope

- El proceso formal y cambio de estado de aislamiento del galpón (cubierto en el SPEC independiente *Registrar aislamiento por galpón*).
- El procesamiento, clasificación diagnóstica, toma de muestras y tratamiento veterinario de la alerta sanitaria (cubierto en los casos de uso del veterinario).
- La orden y ejecución de sacrificio sanitario o vaciado del galpón (cubierto en el SPEC de sacrificio sanitario).
- La actualización directa del inventario de pollos vivos (manejada por el caso de uso de actualización de inventario vivo).
- La creación o administración de galpones, lotes y usuarios.
