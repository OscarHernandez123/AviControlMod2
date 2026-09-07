# Feature Specification: Registrar una enfermedad

**Created**: 2026-09-04  

## User Scenarios & Testing

### User Story 1 - Registrar una enfermedad (Priority: P1)

Como veterinario, quiero registrar una enfermedad con su nombre, nivel de riesgo, tipo de enfermedad, descripción clínica y la indicación de si requiere sacrificio sanitario, para que su información y el manejo sanitario requerido queden disponibles en el sistema.

**Why this priority**: El registro permite identificar y describir las enfermedades, además de establecer cuáles son mortales y exigen el sacrificio sanitario de toda la población del lote. También garantiza que únicamente el veterinario pueda incorporar esta información al sistema.

**Independent Test**: Se puede probar autenticando a un veterinario y registrando una enfermedad con nombre, nivel de riesgo, tipo de enfermedad, descripción clínica y una selección explícita que indique si requiere sacrificio sanitario. El sistema debe guardar todos los datos, incluido el indicador booleano. Si falta alguno de ellos o el usuario no tiene rol de veterinario, debe rechazar la operación y no crear la enfermedad.

**Acceptance Scenarios**:

1. **Scenario**: Registro correcto de una enfermedad
   - **Given** que un veterinario autenticado dispone del nombre, nivel de riesgo, tipo de enfermedad y descripción clínica, y determina si la enfermedad requiere sacrificio sanitario
   - **When** registra todos los datos de la enfermedad
   - **Then** el sistema crea la enfermedad, guarda el indicador de sacrificio sanitario y muestra la información registrada

2. **Scenario**: Intento de registro con datos incompletos
   - **Given** que el veterinario está registrando una enfermedad
   - **When** omite el nombre, nivel de riesgo, tipo de enfermedad, descripción clínica o la indicación de si requiere sacrificio sanitario
   - **Then** el sistema rechaza el registro, identifica los datos que debe completar y no crea la enfermedad

3. **Scenario**: Intento de registro con datos vacíos
   - **Given** que el veterinario está registrando una enfermedad
   - **When** ingresa uno o más datos compuestos únicamente por espacios en blanco
   - **Then** el sistema los considera vacíos, rechaza el registro e indica los datos que debe corregir

4. **Scenario**: Registro por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta registrar una enfermedad
   - **When** solicita confirmar el registro
   - **Then** el sistema rechaza la operación y no crea la enfermedad

5. **Scenario**: Registro de una enfermedad mortal
   - **Given** que el veterinario determina que una enfermedad exige sacrificar toda la población afectada
   - **When** selecciona `Sí` en el indicador de sacrificio sanitario y confirma el registro
   - **Then** el sistema registra la enfermedad con el indicador `requiere sacrificio sanitario` en verdadero

6. **Scenario**: Registro de una enfermedad que no requiere sacrificio sanitario
   - **Given** que el veterinario determina que una enfermedad no exige sacrificar toda la población afectada
   - **When** selecciona `No` en el indicador de sacrificio sanitario y confirma el registro
   - **Then** el sistema registra la enfermedad con el indicador `requiere sacrificio sanitario` en falso

---

### User Story 2 - Editar una enfermedad (Priority: P2)

Como veterinario, quiero editar una enfermedad existente para corregir o actualizar su nombre, nivel de riesgo, tipo de enfermedad, descripción clínica o indicador de sacrificio sanitario, de manera que los nuevos datos se utilicen únicamente en diagnósticos futuros.

**Why this priority**: La edición permite mantener actualizada la información sanitaria sin cambiar las enfermedades, medicaciones o decisiones de sacrificio que quedaron registradas en diagnósticos anteriores.

**Independent Test**: Se puede probar utilizando una enfermedad que ya está relacionada con un diagnóstico y cambiar su indicador `requiere sacrificio sanitario`. El diagnóstico existente debe conservar la decisión sanitaria original, mientras que los diagnósticos posteriores deben utilizar el valor actualizado.

**Acceptance Scenarios**:

1. **Scenario**: Edición correcta de una enfermedad
   - **Given** que existe una enfermedad registrada
   - **When** el veterinario modifica el nombre, nivel de riesgo, tipo de enfermedad, descripción clínica o indicador de sacrificio sanitario con datos válidos
   - **Then** el sistema guarda los cambios y deja la información actualizada disponible para diagnósticos futuros

2. **Scenario**: Edición de una enfermedad utilizada en diagnósticos existentes
   - **Given** que una enfermedad ya está relacionada con uno o varios diagnósticos
   - **When** el veterinario edita sus datos
   - **Then** el sistema aplica los cambios únicamente a diagnósticos futuros y conserva sin cambios la información y las decisiones sanitarias de los diagnósticos existentes

3. **Scenario**: Edición por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta editar una enfermedad
   - **When** solicita confirmar los cambios
   - **Then** el sistema rechaza la operación y conserva la enfermedad sin cambios

### Edge Cases

- **Edge case #1 - Datos obligatorios que contienen únicamente espacios**

  - ¿Cómo maneja el sistema un nombre, tipo de enfermedad o descripción clínica que contiene únicamente espacios en blanco?  
    El sistema debe considerar el dato como vacío, informar que debe corregirse y no crear la enfermedad.

- **Edge case #3 - Interrupción durante el registro**

  - ¿Cómo maneja el sistema una interrupción mientras se guarda la enfermedad?  
    El sistema debe evitar registros parciales: la enfermedad debe guardarse con todos sus datos o no debe crearse.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro de enfermedades exclusivamente a usuarios con rol de veterinario.
- **FR-002**: Cada enfermedad DEBE registrar nombre, nivel de riesgo, tipo de enfermedad, descripción clínica y el indicador booleano `requiere sacrificio sanitario`.
- **FR-003**: El sistema DEBE considerar obligatorios todos los datos de la enfermedad y rechazar el registro cuando alguno esté ausente o vacío.
- **FR-004**: El veterinario DEBE seleccionar explícitamente si la enfermedad requiere sacrificio sanitario; el sistema NO DEBE asignar un valor predeterminado.
- **FR-005**: El valor verdadero del indicador DEBE señalar que la enfermedad es mortal y requiere el sacrificio sanitario de toda la población del lote afectado.
- **FR-006**: El valor falso del indicador DEBE señalar que la enfermedad no habilita una orden de sacrificio sanitario.

### Key Entities

- **Enfermedad**: Representa una enfermedad registrada por el veterinario.
  - **Atributos**: nombre, nivel de riesgo, tipo de enfermedad, descripción clínica y el indicador booleano `requiere sacrificio sanitario`.
  - **Relación con Medicación**: una enfermedad puede tener varias medicaciones registradas y cada medicación referencia la enfermedad para la cual fue definida.
  - **Relación con Diagnóstico del galpón**: se relaciona indirectamente con el diagnóstico mediante la medicación seleccionada. Cada diagnóstico referencia una única medicación y obtiene de ella la enfermedad correspondiente sin duplicar sus datos.
  - **Relación con Orden de sacrificio sanitario**: cuando el indicador es verdadero, la enfermedad permite que el diagnóstico sustente una orden para sacrificar toda la población del lote.
  - **Comportamiento ante ediciones**: los datos actualizados se utilizan en diagnósticos futuros, mientras que los diagnósticos existentes conservan la información y la decisión sanitaria utilizadas al momento de su registro.
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
- **SC-005**: El 100 % de las enfermedades registra una selección explícita para el indicador `requiere sacrificio sanitario`.
- **SC-006**: El 100 % de las enfermedades con el indicador verdadero queda identificado como causa de sacrificio sanitario total.
- **SC-007**: El 100 % de las ediciones realizadas por el veterinario conserva sin cambios los diagnósticos y las decisiones sanitarias existentes.
- **SC-008**: El 95 % de las ediciones válidas queda disponible para diagnósticos futuros en un máximo de 1 segundo después de su confirmación.
