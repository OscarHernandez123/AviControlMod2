# Feature Specification: Generar alerta sanitaria

**Created**: 2026-09-05  

## User Scenarios & Testing 

### User Story 1 - Generar automáticamente la alerta sanitaria para atención veterinaria (Priority: P1)

Como sistema de gestión avícola, quiero generar de forma automática e inmediata una alerta sanitaria cuando se registre un evento de mortalidad anormal en un galpón, asignándola al rol de veterinario en estado pendiente de atención, para que el profesional de salud evalúe el lote, realice el diagnóstico clínico y decida las medidas sanitarias o de sacrificio correspondientes.

**Why this priority**: Es la respuesta reactiva inmediata ante riesgos epidemiológicos. Asegura que ninguna anomalía sanitaria pase desapercibida y que el equipo veterinario sea notificado al instante con la información del galpón y lote afectado.

**Independent Test**: Se puede probar confirmando un registro de mortalidad anormal en un galpón con lote activo y verificando que el sistema cree automáticamente la entidad de alerta sanitaria en estado "Pendiente de atención veterinaria", vinculada al lote, galpón y registro de origen, y disponible para el veterinario.

**Acceptance Scenarios**:

1. **Scenario**: Generación automática y exitosa de la alerta sanitaria
   - **Given** que se ha confirmado un registro de mortalidad anormal para un galpón y su lote activo
   - **When** el caso de uso base de mortalidad anormal invoca el proceso de generación de alerta sanitaria
   - **Then** el sistema crea un registro de alerta sanitaria con UUID único y fecha/hora actual de emisión
   - **And** asocia la alerta al UUID del galpón, al UUID del lote activo y al UUID del registro de mortalidad anormal
   - **And** establece el estado de la alerta como "Pendiente de atención veterinaria" con nivel de prioridad asignado según la severidad

2. **Scenario**: Generación de múltiples alertas independientes por eventos distintos
   - **Given** que un galpón ya tiene una alerta sanitaria previa en estado pendiente
   - **When** se confirma un nuevo registro de mortalidad anormal en dicho galpón
   - **Then** el sistema crea una nueva alerta sanitaria independiente con su propio UUID y timestamp
   - **And** conserva el historial de ambas alertas vinculadas al lote

3. **Scenario**: Intento de generación sin un registro de mortalidad anormal válido
   - **Given** una solicitud de generación de alerta sin referencia a un registro de mortalidad anormal existente
   - **When** el sistema intenta procesar la alerta
   - **Then** el sistema rechaza la creación de la alerta y registra un error de inconsistencia de datos

---

### User Story 2 - Consolidar información clínica y de población para el veterinario (Priority: P2)

Como veterinario de granja, quiero que la alerta sanitaria generada consolide automáticamente los datos del galpón, los atributos del lote activo y los síntomas reportados en la mortalidad anormal, para disponer de todo el contexto clínico antes de acudir al galpón.

**Why this priority**: Permite al veterinario conocer de inmediato la magnitud de las bajas, el porcentaje de mortalidad respecto a la población viva, la edad de las aves y los signos observados para preparar el protocolo de bioseguridad adecuado.

**Independent Test**: Se puede probar consultando la alerta sanitaria generada y verificando que presente el nombre y aforo del galpón, el nombre, edad en días y población actual del lote, y los síntomas/causas reportadas en la mortalidad anormal.

**Acceptance Scenarios**:

1. **Scenario**: Visualización de la alerta consolidada por el veterinario
   - **Given** una alerta sanitaria generada automáticamente en estado "Pendiente de atención veterinaria"
   - **When** el veterinario accede a su bandeja de alertas sanitarias
   - **Then** el sistema muestra los datos del galpón (nombre, aforo, estado) y del lote (nombre, población actual, edad en días)
   - **And** muestra la cantidad de muertes anormales, porcentaje de mortalidad calculado, causa sospechosa, síntomas clínicos y observaciones del trabajador

---

### Edge Cases

- **¿Qué sucede si ocurre una falla técnica durante la generación de la alerta?**
  - El sistema debe asegurar transaccionalidad o reintento inmediato para garantizar que ningún registro de mortalidad anormal quede sin su correspondiente alerta sanitaria.

- **¿Qué sucede si el lote activo del galpón se queda con 0 aves vivas tras la mortalidad anormal?**
  - El sistema genera la alerta sanitaria con prioridad máxima ("Emergencia sanitaria") indicando mortalidad total del lote para la intervención inmediata del veterinario.

- **¿Cómo se determina el nivel de prioridad de la alerta sanitaria?**
  - El sistema asigna automáticamente la prioridad ("Alta", "Crítica" o "Emergencia sanitaria") según el porcentaje de mortalidad y la severidad clínica transferida desde el registro de mortalidad anormal.

- **¿La generación de la alerta permite al trabajador o a usuarios no veterinarios diagnosticar o cerrar la alerta?**
  - No, la alerta nace estrictamente en estado "Pendiente de atención veterinaria" y solo los usuarios con rol de veterinario tienen atribuciones para diagnosticar, prescribir o resolver la alerta en sus respectivos casos de uso.

- **¿Se permite la eliminación directa de una alerta sanitaria generada?**
  - No, las alertas sanitarias forman parte de la bitácora inmutable de bioseguridad y auditoría de la granja.

---

## Requirements 

### Functional Requirements

- **FR-001**: El sistema DEBE generar automáticamente una alerta sanitaria de forma inmediata tras la confirmación de cualquier registro de mortalidad anormal.
- **FR-002**: Cada alerta sanitaria generada DEBE registrar un UUID único, fecha y hora de emisión del sistema, nivel de prioridad y estado inicial `Pendiente de atención veterinaria`.
- **FR-003**: La alerta sanitaria DEBE quedar asociada mediante llaves foráneas al galpón afectado, al lote activo correspondiente y al registro de mortalidad anormal que la originó.
- **FR-004**: El sistema DEBE obtener y vincular los atributos del galpón: UUID, Nombre, Aforo máximo y Estado.
- **FR-005**: El sistema DEBE obtener y vincular los atributos del lote activo: UUID único, Nombre, Población actual (aves vivas), Fecha de ingreso y edad en días calculada.
- **FR-006**: La alerta sanitaria DEBE incorporar los datos clínicos del registro de mortalidad anormal: cantidad de muertes anormales, porcentaje de mortalidad calculado, causa sospechosa, síntomas y signos observados y observaciones del trabajador.
- **FR-007**: El sistema DEBE asignar el nivel de prioridad de la alerta (`Alta`, `Crítica` o `Emergencia sanitaria`) con base en el porcentaje de mortalidad y la severidad del evento.
- **FR-008**: La alerta sanitaria generada DEBE dirigirse y ponerse a disposición exclusiva del usuario o equipo con rol de `Veterinario` para su diagnóstico y resolución.
- **FR-009**: El sistema DEBE garantizar que la alerta sanitaria no pueda ser cerrada, editada ni eliminada durante el proceso de generación automática.
- **FR-010**: El sistema DEBE rechazar cualquier intento de generación manual de alerta sanitaria que no provenga de un registro válido de mortalidad anormal.
- **FR-011**: El sistema DEBE registrar la trazabilidad completa del trabajador que originó el reporte de mortalidad anormal asociado a la alerta.

### Key Entities 

- **Alerta sanitaria**: Representa la notificación de emergencia bioseguridad generada por el sistema para el equipo veterinario.
  - Atributos: `UUID único`, `Fecha y hora de emisión`, `Nivel de prioridad` (Alta, Crítica, Emergencia sanitaria), `Estado` (`Pendiente de atención veterinaria`), `Llave foránea del registro de mortalidad anormal`, `Llave foránea del galpón`, `Llave foránea del lote`.
- **Galpón**: Unidad física de producción avícola donde se presenta la alerta.
  - Atributos utilizados: `UUID único`, `Nombre`, `Aforo máximo`, `Estado`.
- **Lote**: Grupo de aves afectado que referencia al galpón mediante llave foránea.
  - Atributos utilizados: `UUID único`, `Nombre`, `Población actual`, `Fecha de ingreso` (edad en días), `Llave foránea del galpón`.
- **Registro de mortalidad anormal**: Evento detonante que originó la alerta sanitaria.
  - Atributos utilizados: `UUID único`, `Cantidad de muertes anormales`, `Porcentaje de mortalidad (%)`, `Causa sospechosa`, `Síntomas y signos clínicos`, `Observaciones`.
- **Veterinario**: Rol destinatario responsable de atender la alerta, emitir el diagnóstico clínico y determinar el tratamiento o sacrificio sanitario.

---

## Success Criteria 

### Measurable Outcomes

- **SC-001**: El 100 % de los registros de mortalidad anormal confirmados genera automáticamente una alerta sanitaria con estado `Pendiente de atención veterinaria`.
- **SC-002**: El 100 % de las alertas sanitarias generadas vincula con exactitud el UUID del galpón, el UUID del lote activo y el UUID del registro de mortalidad anormal.
- **SC-003**: En el 100 % de las alertas, se incluye la cantidad de muertes, el porcentaje de mortalidad calculado, la causa sospechosa y los síntomas clínicos reportados.
- **SC-004**: El 100 % de las alertas sanitarias se pone a disposición inmediata del rol Veterinario.
- **SC-005**: El tiempo de generación automática de la alerta sanitaria tras la confirmación de la mortalidad anormal es menor a 2 segundos en el sistema.
- **SC-006**: Ninguna alerta sanitaria generada puede ser eliminada del historial del sistema.

---

## Out of Scope

- El diagnóstico clínico, toma de muestras de laboratorio y confirmación de enfermedades (cubierto en el SPEC *Diagnosticar galpón* / *Añadir enfermedad*).
- La validación y certificación del aislamiento del galpón (cubierto en los casos de uso del veterinario).
- La orden y ejecución de tratamiento farmacológico o medicación (cubierto en los casos de uso de medicación).
- La orden y ejecución de sacrificio sanitario o vaciado del galpón (cubierto en el SPEC *Ordenar sacrificio sanitario*).
- El cierre o cambio de estado de la alerta sanitaria tras la intervención veterinaria.
- La integración o comunicación con módulos externos (Módulo 3).
