# Feature Specification: Registrar mortalidad por galpón

**Created**: 2026-09-05  

## User Scenarios & Testing 

### User Story 1 - Registrar evento de mortalidad por galpón (Priority: P1)

Como trabajador u operario de granja, quiero registrar las bajas de aves ocurridas en mi galpón asignado, indicando la fecha y hora del evento, la causa y la cantidad de muertes, para mantener el registro formal de mortalidad asociado al galpón y a su lote activo.

**Why this priority**: Es el registro operativo oficial de bajas en granja. Permite documentar cada evento de mortalidad con su fecha, causa y cantidad para fines de control sanitario, auditoría e invocación de los procesos de actualización de inventario.

**Independent Test**: Se puede probar seleccionando un galpón asignado con lote activo, ingresando la fecha y hora del evento, la causa y la cantidad de muertes, y verificando que el sistema cree el registro de mortalidad con sus relaciones correspondientes y active la inclusión del caso de uso de actualización de inventario vivo.

**Acceptance Scenarios**:

1. **Scenario**: Registro exitoso de mortalidad en un galpón con lote activo
   - **Given** que un trabajador autenticado selecciona un galpón asignado a su cargo con un lote activo cuya llave foránea referencia al galpón
   - **When** ingresa la fecha y hora del evento, la causa de la muerte y una cantidad válida de muertes
   - **Then** el sistema crea y almacena el registro de mortalidad con UUID único, fecha y hora del evento, causa, cantidad de muertes, identificador del lote y del galpón
   - **And** invoca el caso de uso incluido (`<<include>>`) para actualizar el inventario vivo del galpón

2. **Scenario**: Múltiples registros de mortalidad en el mismo día
   - **Given** que ya existe un registro de mortalidad en la fecha para el lote activo
   - **When** el trabajador registra un nuevo evento de bajas ocurrido en otro horario
   - **Then** el sistema almacena un nuevo registro de mortalidad independiente con su respectiva fecha, hora y causa
   - **And** invoca el caso de uso incluido (`<<include>>`) correspondiente

3. **Scenario**: Intento de registrar una cantidad de muertes superior a la población viva actual
   - **Given** un lote activo con una población actual conocida
   - **When** el trabajador intenta registrar una cantidad de muertes que supera dicha población
   - **Then** el sistema rechaza el registro, informa que la cantidad no puede exceder las aves vivas del lote y no crea el registro

4. **Scenario**: Intento de registro en un galpón no asignado al trabajador
   - **Given** que un trabajador intenta registrar mortalidad en un galpón que no tiene asignado
   - **When** solicita confirmar el registro
   - **Then** el sistema deniega el acceso y no almacena ningún registro

5. **Scenario**: Intento de registro en un galpón sin lote activo
   - **Given** un galpón que no cuenta con un lote activo registrado
   - **When** el trabajador intenta registrar un evento de mortalidad
   - **Then** el sistema bloquea la operación e informa que el galpón no tiene un lote activo para registrar bajas

---

### User Story 2 - Detectar condición de mortalidad anormal para extensión sanitaria (Priority: P2)

Como trabajador u operario de granja, quiero que el sistema evalúe la cantidad de muertes registradas respecto a los umbrales diarios del lote, para identificar situaciones de mortalidad anormal y activar la extensión del flujo sanitario.

**Why this priority**: Permite detectar a tiempo eventos atípicos o masivos de mortalidad para activar oportunamente los protocolos de contingencia y alertas sanitarias.

**Independent Test**: Se puede probar registrando una cantidad de bajas que supere el umbral porcentual diario establecido, verificando que el sistema guarde el registro de mortalidad y dispare la extensión hacia el caso de uso de alerta sanitaria.

**Acceptance Scenarios**:

1. **Scenario**: Registro de mortalidad dentro de los parámetros normales
   - **Given** un lote activo donde la cantidad de muertes no supera el umbral diario normal
   - **When** el trabajador confirma el registro
   - **Then** el sistema almacena el registro de mortalidad como evento normal sin activar extensiones sanitarias

2. **Scenario**: Detección de mortalidad anormal que activa extensión sanitaria
   - **Given** un lote activo donde la cantidad de muertes supera el umbral diario establecido
   - **When** el trabajador confirma el registro
   - **Then** el sistema almacena el registro de mortalidad clasificado como anormal
   - **And** activa la extensión (`<<extend>>`) hacia el caso de uso de registrar mortalidad anormal / generar alerta sanitaria

---

### Edge Cases

- **¿Qué sucede si la cantidad de muertes ingresada es 0, un número negativo o un valor no entero?**
  - El sistema rechaza el registro, exige una cantidad entera mayor que cero y no almacena el evento.

- **¿Qué sucede si la cantidad de muertes iguala exactamente a la población actual de aves vivas (mortalidad del 100 %)?**
  - El sistema almacena el registro de mortalidad, lo clasifica como anormal y activa la extensión de alerta sanitaria.

- **¿Cómo valida el sistema la fecha y hora digitadas por el trabajador?**
  - El sistema valida que la fecha y hora del evento no sean posteriores a la fecha y hora actual del servidor, ni anteriores a la fecha de ingreso del lote activo.

- **¿Qué sucede si los campos de fecha/hora, causa o cantidad de muertes se dejan vacíos?**
  - El sistema bloquea el registro, resalta los campos obligatorios faltantes y no crea el registro.

- **¿Se permite la edición o eliminación directa de un registro de mortalidad confirmado?**
  - No, los registros de mortalidad son inmutables una vez confirmados para proteger la trazabilidad histórica del lote.

- **¿Qué sucede si un usuario no autenticado o sin rol de trabajador intenta registrar una mortalidad?**
  - El sistema bloquea el acceso a la operación y exige autenticación con un usuario autorizado.

---

## Requirements 

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro de mortalidad por galpón exclusivamente a usuarios autenticados con rol de trabajador (y administradores autorizados).
- **FR-002**: El sistema DEBE restringir el registro de mortalidad únicamente a los galpones que el trabajador tenga formalmente asignados a su cargo.
- **FR-003**: El sistema DEBE identificar el lote activo del galpón mediante la llave foránea almacenada en la entidad Lote, seleccionando el lote con la fecha de ingreso más reciente.
- **FR-004**: Para registrar la mortalidad, el sistema DEBE exigir obligatoriamente: identificador del galpón, fecha y hora del evento, causa de la muerte y cantidad de muertes.
- **FR-005**: El sistema DEBE validar que la cantidad de muertes sea un número entero mayor que cero.
- **FR-006**: El sistema DEBE validar que la cantidad de muertes sea menor o igual a la población actual de aves vivas del lote activo.
- **FR-007**: El sistema DEBE validar que la fecha y hora del evento no sean posteriores a la fecha y hora actual del sistema, ni anteriores a la fecha de ingreso del lote.
- **FR-008**: Al confirmar el registro, el sistema DEBE almacenar una entrada de mortalidad con UUID único, fecha y hora del evento, fecha y hora de registro, causa, cantidad de muertes, llave foránea del lote, llave foránea del galpón y usuario responsable.
- **FR-009**: Al confirmar el registro de mortalidad, el sistema DEBE invocar el caso de uso incluido (`<<include>>`) *Actualizar inventario vivo por galpón* comunicando la cantidad de muertes y la referencia del lote.
- **FR-010**: El sistema DEBE evaluar si la cantidad de muertes registradas supera el umbral diario establecido para clasificar el evento como mortalidad anormal.
- **FR-011**: Cuando se detecte una mortalidad anormal, el sistema DEBE activar la extensión (`<<extend>>`) hacia el caso de uso *Registrar mortalidad anormal* / *Generar alerta sanitaria*.
- **FR-012**: El sistema DEBE garantizar que los registros de mortalidad confirmados sean inmutables y no puedan ser eliminados ni editados directamente.
- **FR-013**: El sistema NO DEBE permitir registrar eventos de mortalidad en galpones que no cuenten con un lote activo registrado.

### Key Entities 

- **Galpón**: Representa la unidad física de producción avícola donde ocurre el evento.
  - Atributos utilizados: `UUID único`, `Nombre`, `Aforo máximo`, `Estado`.
- **Lote**: Representa el grupo de aves registrado que referencia al galpón mediante llave foránea.
  - Atributos utilizados: `UUID único`, `Nombre`, `Población actual` (consultada para validación de límite), `Fecha de ingreso`, `Llave foránea del galpón`.
- **Registro de mortalidad**: Representa el evento individual de bajas registrado por el trabajador.
  - Atributos: `UUID único`, `Fecha y hora del evento`, `Fecha y hora de registro`, `Cantidad de muertes`, `Causa de mortalidad`, `Llave foránea del lote`, `Llave foránea del galpón`, `Usuario responsable`.
- **Trabajador / Operario de granja**: Usuario autenticado responsable del cuidado del galpón y del registro de sus novedades.

---

## Success Criteria 

### Measurable Outcomes

- **SC-001**: El 100 % de los registros de mortalidad confirmados almacena todos los campos obligatorios (fecha/hora evento, causa, cantidad, lote, galpón y usuario responsable).
- **SC-002**: El 100 % de los registros de mortalidad confirmados invoca correctamente el caso de uso incluido (`<<include>>`) *Actualizar inventario vivo por galpón*.
- **SC-003**: El 100 % de los intentos de registrar cantidades mayores a la población viva actual es rechazado por el sistema.
- **SC-004**: El 100 % de los registros de mortalidad queda vinculado al UUID del galpón y al UUID del lote activo referenciado por llave foránea.
- **SC-005**: El 100 % de los eventos de mortalidad que superen el umbral establecido activa correctamente la extensión (`<<extend>>`) de mortalidad anormal.
- **SC-006**: El 100 % de los intentos de registro en galpones no asignados o sin lote activo es bloqueado por el sistema.
- **SC-007**: El 100 % de los registros de mortalidad confirmados permanece inmutable en el historial del sistema.

---

## Out of Scope

- La lógica detallada y las reglas de actualización del inventario de pollos vivos (cubierto en el SPEC del caso de uso incluido *Actualizar inventario vivo por galpón*).
- El flujo extendido de procesamiento, clasificación diagnóstica y resolución de alertas sanitarias (cubierto en los casos de uso sanitarios).
- La orden de sacrificio sanitario o vaciado del galpón (cubierto en el SPEC de sacrificio sanitario).
- La disposición final, enterramiento o compostaje de las aves muertas.
- La creación, edición o administración de galpones, lotes y usuarios.
