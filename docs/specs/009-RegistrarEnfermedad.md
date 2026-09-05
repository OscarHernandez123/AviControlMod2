# Feature Specification: Registrar una enfermedad

**Created**: 2026-09-04  

## User Scenarios & Testing

### User Story 1 - Registrar una enfermedad (Priority: P1)

Como veterinario, quiero registrar una enfermedad con su nombre, nivel de riesgo, tipo de enfermedad y descripción clínica, para que su información quede disponible en el sistema.

**Why this priority**: El registro permite conservar la información necesaria para identificar y describir las enfermedades utilizadas en los procesos veterinarios, y garantiza que únicamente el veterinario pueda incorporarlas al sistema.

**Independent Test**: Se puede probar autenticando a un veterinario y registrando una enfermedad con nombre, nivel de riesgo, tipo de enfermedad y descripción clínica. El sistema debe guardar la enfermedad y mostrar la información registrada. Si falta alguno de los datos o el usuario no tiene rol de veterinario, debe rechazar la operación y no crear la enfermedad.

**Acceptance Scenarios**:

1. **Scenario**: Registro correcto de una enfermedad
   - **Given** que un veterinario autenticado dispone del nombre, nivel de riesgo, tipo de enfermedad y descripción clínica
   - **When** registra todos los datos de la enfermedad
   - **Then** el sistema crea la enfermedad y muestra la información registrada

2. **Scenario**: Intento de registro con datos incompletos
   - **Given** que el veterinario está registrando una enfermedad
   - **When** omite el nombre, nivel de riesgo, tipo de enfermedad o descripción clínica
   - **Then** el sistema rechaza el registro, identifica los datos que debe completar y no crea la enfermedad

3. **Scenario**: Intento de registro con datos vacíos
   - **Given** que el veterinario está registrando una enfermedad
   - **When** ingresa uno o más datos compuestos únicamente por espacios en blanco
   - **Then** el sistema los considera vacíos, rechaza el registro e indica los datos que debe corregir

4. **Scenario**: Registro por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta registrar una enfermedad
   - **When** solicita confirmar el registro
   - **Then** el sistema rechaza la operación y no crea la enfermedad

### Edge Cases

- **Edge case #2 - Datos obligatorios que contienen únicamente espacios**

  - ¿Cómo maneja el sistema un nombre, tipo de enfermedad o descripción clínica que contiene únicamente espacios en blanco?  
    El sistema debe considerar el dato como vacío, informar que debe corregirse y no crear la enfermedad.

- **Edge case #3 - Interrupción durante el registro**

  - ¿Cómo maneja el sistema una interrupción mientras se guarda la enfermedad?  
    El sistema debe evitar registros parciales: la enfermedad debe guardarse con todos sus datos o no debe crearse.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro de enfermedades exclusivamente a usuarios con rol de veterinario.
- **FR-002**: Cada enfermedad DEBE registrar nombre, nivel de riesgo, tipo de enfermedad y descripción clínica.
- **FR-003**: El sistema DEBE considerar obligatorios todos los datos de la enfermedad y rechazar el registro cuando alguno esté ausente o vacío.
- **FR-004**: El sistema DEBE identificar los datos incompletos o vacíos para que el veterinario pueda corregirlos.
- **FR-005**: Cuando los datos sean válidos, el sistema DEBE crear la enfermedad, conservar toda la información registrada y mostrar la confirmación del registro.
- **FR-006**: Cuando el registro sea rechazado, el sistema NO DEBE crear una enfermedad incompleta ni guardar parcialmente sus datos.
- **FR-007**: El sistema DEBE verificar que el usuario conserve el rol de veterinario al momento de confirmar el registro.

### Key Entities

- **Enfermedad**: Representa una enfermedad registrada por el veterinario.
  - **Atributos**: nombre, nivel de riesgo, tipo de enfermedad y descripción clínica.
  - **Relación con Medicación**: una enfermedad puede tener varias medicaciones registradas y cada medicación referencia la enfermedad para la cual fue definida.
  - **Relación con Diagnóstico del galpón**: se relaciona indirectamente con el diagnóstico mediante la medicación seleccionada. Cada diagnóstico referencia una única medicación y obtiene de ella la enfermedad correspondiente sin duplicar sus datos.
- **Medicación**: Representa el tratamiento registrado para una enfermedad.
  - **Relaciones**: referencia una enfermedad y posteriormente puede ser seleccionada por uno o varios diagnósticos de galpón.
- **Diagnóstico del galpón**: Representa el resultado de la evaluación de la condición sanitaria de un galpón.
  - **Relaciones**: corresponde a un galpón, referencia una única medicación y obtiene la enfermedad por medio de esa medicación.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los veterinarios puede completar el registro de una enfermedad válida en menos de 2 minutos.
- **SC-002**: El 95 % de los registros válidos queda disponible en un máximo de 1 segundo después de su confirmación.
- **SC-003**: El 100 % de los intentos realizados por roles distintos al veterinario es rechazado sin crear una enfermedad.
- **SC-004**: Al menos el 95 % de los veterinarios identifica y corrige los datos obligatorios faltantes en el primer intento durante pruebas de usabilidad.
