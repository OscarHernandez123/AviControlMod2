# Feature Specification: 009 - Registrar una enfermedad

**Created**: 2026-09-04  
**Last Updated**: 2026-10-28

## User Scenarios & Testing

### User Story 1 - Registrar una enfermedad (Priority: P1)

Como veterinario, quiero registrar una enfermedad con su nombre, nivel de riesgo, tipo de enfermedad, descripción clínica y la indicación explícita de si requiere sacrificio sanitario, para que su información y el manejo sanitario requerido queden disponibles en el catálogo del sistema.

**Why this priority**: El registro permite identificar y clasificar las patologías en la granja, además de establecer cuáles son tratables y cuáles son mortales y exigen el sacrificio sanitario de toda la población del lote. También garantiza que únicamente el veterinario pueda incorporar esta información al sistema.

**Independent Test**: Se puede probar autenticando a un veterinario y registrando una enfermedad con nombre, nivel de riesgo, tipo de enfermedad, descripción clínica y una selección explícita que indique si requiere sacrificio sanitario. El sistema debe guardar todos los datos, incluido el indicador booleano. Si falta alguno de ellos o el usuario no tiene rol de veterinario, debe rechazar la operación y no crear la enfermedad.

**Acceptance Scenarios**:

1. **Scenario**: Registro correcto de una enfermedad
   - **Given** que un veterinario autenticado dispone del nombre, nivel de riesgo, tipo de enfermedad y descripción clínica, y determina si la enfermedad requiere sacrificio sanitario
   - **When** registra todos los datos de la enfermedad
   - **Then** el sistema crea la enfermedad, guarda el indicador de sacrificio sanitario y deja la información disponible en el catálogo

2. **Scenario**: Intento de registro con datos incompletos
   - **Given** que el veterinario está registrando una enfermedad
   - **When** omite el nombre, nivel de riesgo, tipo de enfermedad, descripción clínica o la indicación explícita de si requiere sacrificio sanitario
   - **Then** el sistema rechaza el registro, identifica los datos que debe completar y no crea la enfermedad

3. **Scenario**: Intento de registro con datos vacíos o espacios en blanco
   - **Given** que el veterinario está registrando una enfermedad
   - **When** ingresa uno o más datos compuestos únicamente por espacios en blanco
   - **Then** el sistema los considera vacíos, rechaza el registro e indica los datos que debe corregir

4. **Scenario**: Registro por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta registrar una enfermedad
   - **When** solicita confirmar el registro
   - **Then** el sistema rechaza la operación y no crea la enfermedad

5. **Scenario**: Registro de una enfermedad mortal que requiere sacrificio sanitario
   - **Given** que el veterinario determina que una enfermedad exige sacrificar toda la población afectada
   - **When** selecciona `Sí` (`true`) en el indicador de sacrificio sanitario y confirma el registro
   - **Then** el sistema registra la enfermedad con el indicador `requiere sacrificio sanitario` en verdadero, habilitándola como causal de sacrificio sanitario total al diagnosticarse

6. **Scenario**: Registro de una enfermedad tratable que no requiere sacrificio sanitario
   - **Given** que el veterinario determina que una enfermedad admite tratamiento médico y no exige sacrificar la población afectada
   - **When** selecciona `No` (`false`) en el indicador de sacrificio sanitario y confirma el registro
   - **Then** el sistema registra la enfermedad con el indicador `requiere sacrificio sanitario` en falso, permitiendo asociarle medicaciones

---

### User Story 2 - Editar una enfermedad (Priority: P2)

Como veterinario, quiero editar una enfermedad existente para corregir o actualizar su nombre, nivel de riesgo, tipo de enfermedad, descripción clínica o indicador de sacrificio sanitario, de manera que los nuevos datos se utilicen únicamente en diagnósticos futuros.

**Why this priority**: La edición permite mantener actualizado el catálogo de enfermedades sin alterar los diagnósticos, tratamientos o decisiones de sacrificio que quedaron registrados históricamente en evaluaciones anteriores.

**Independent Test**: Se puede probar utilizando una enfermedad que ya fue empleada en un diagnóstico previo y cambiar su indicador `requiere sacrificio sanitario`. El diagnóstico existente debe conservar la decisión sanitaria original capturada en su instantánea histórica, mientras que los diagnósticos posteriores deben utilizar el valor actualizado.

**Acceptance Scenarios**:

1. **Scenario**: Edición correcta de una enfermedad
   - **Given** que existe una enfermedad registrada en el catálogo
   - **When** el veterinario modifica el nombre, nivel de riesgo, tipo de enfermedad, descripción clínica o indicador de sacrificio sanitario con datos válidos
   - **Then** el sistema guarda los cambios y deja la información actualizada disponible para diagnósticos futuros

2. **Scenario**: Edición de una enfermedad utilizada en diagnósticos existentes
   - **Given** que una enfermedad ya está relacionada con uno o varios diagnósticos confirmados previamente
   - **When** el veterinario edita sus datos
   - **Then** el sistema aplica los cambios únicamente a diagnósticos futuros y conserva sin cambios la instantánea histórica y las decisiones sanitarias de los diagnósticos existentes

3. **Scenario**: Edición por un usuario no autorizado
   - **Given** que un usuario sin rol de veterinario intenta editar una enfermedad
   - **When** solicita confirmar los cambios
   - **Then** el sistema rechaza la operación y conserva la enfermedad sin cambios

---

### Edge Cases

- **Edge case #1 - Datos obligatorios que contienen únicamente espacios**
  - ¿Cómo maneja el sistema un nombre, tipo de enfermedad o descripción clínica que contiene únicamente espacios en blanco?  
    El sistema debe normalizar el texto (`trim`), considerar el dato como vacío, informar que debe corregirse y no crear ni actualizar la enfermedad.

- **Edge case #2 - Interrupción o concurrencia durante el guardado**
  - ¿Cómo maneja el sistema una interrupción mientras se guarda la enfermedad o dos ediciones simultáneas?  
    El sistema debe evitar registros parciales mediante una transacción local atómica: la enfermedad debe guardarse con todos sus datos o no crearse, aplicando control de concurrencia optimista en las actualizaciones.

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro, la consulta del catálogo y la edición de enfermedades exclusivamente a usuarios con rol de veterinario.
- **FR-002**: Cada enfermedad DEBE registrar obligatoriamente nombre, nivel de riesgo, tipo de enfermedad, descripción clínica y el indicador booleano `requiere sacrificio sanitario`.
- **FR-003**: El sistema DEBE considerar obligatorios todos los datos de la enfermedad y rechazar el registro o la edición cuando alguno esté ausente o compuesto únicamente por espacios.
- **FR-004**: El veterinario DEBE seleccionar explícitamente si la enfermedad requiere sacrificio sanitario; el sistema NO DEBE asignar ni inferir un valor predeterminado.
- **FR-005**: El valor verdadero del indicador DEBE señalar que la enfermedad es mortal y requiere el sacrificio sanitario de toda la población del lote afectado al ser diagnosticada (Spec 012).
- **FR-006**: El valor falso del indicador DEBE señalar que la enfermedad es tratable, exige seleccionar una medicación compatible al diagnosticarse (Spec 011) y no habilita una orden de sacrificio sanitario.
- **FR-007**: El sistema DEBE exponer un mecanismo de consulta (endpoint `GET`) que permita listar y filtrar las enfermedades registradas para poblar los selectores del módulo de medicación (Spec 008) y diagnóstico clínico (Spec 011).
- **FR-008**: La edición de una enfermedad DEBE aplicarse únicamente a diagnósticos futuros y NO DEBE recalcular ni alterar los diagnósticos confirmados con anterioridad.
- **FR-009**: Las enfermedades registradas NO DEBEN eliminarse físicamente para proteger la trazabilidad de los diagnósticos históricos.

### Non-Functional Requirements

| ID          | Categoría    | Requerimiento                                                                            | Métrica                             |
| :---------- | :----------- | :--------------------------------------------------------------------------------------- | :---------------------------------- |
| **NFR-001** | Rendimiento  | Latencia de validación, registro y persistencia                                          | < 1 segundo (p95) bajo carga normal |
| **NFR-002** | Seguridad    | Acceso restringido exclusivamente al rol VETERINARIO                                     | HTTP 403 Forbidden a otros roles    |
| **NFR-003** | Integridad   | Escritura atómica de la entidad y sus relaciones                                         | 100% de consistencia local          |
| **NFR-004** | Persistencia | Los registros históricos de diagnósticos conservan los datos de la enfermedad inmutables | 0% de alteración retroactiva        |

### Key Entities

- **Enfermedad**: Representa una patología registrada por el veterinario en el catálogo sanitario.
  - **Atributos**: identificador UUID, nombre, nivel de riesgo (`NivelRiesgo`), tipo de enfermedad (`TipoEnfermedad`), descripción clínica y el indicador booleano obligatorio `requiere sacrificio sanitario`.
  - **Relación con Medicación**: una enfermedad tratable (`requiere sacrificio sanitario == false`) puede tener varias medicaciones registradas (Spec 008), y cada medicación referencia la enfermedad para la cual fue definida.
  - **Relación con Diagnóstico del galpón**: se relaciona directamente con el diagnóstico (Spec 011) mediante su llave foránea (`enfermedadId`). Al confirmarse un diagnóstico, este captura una instantánea (_snapshot_) del nombre y del indicador de sacrificio de la enfermedad.
  - **Relación con Orden de sacrificio sanitario**: cuando el indicador es verdadero, la enfermedad diagnosticada en el galpón habilita la emisión de una orden para sacrificar toda la población del lote (Spec 012) sin requerir medicación.
  - **Comportamiento ante ediciones**: los datos actualizados se utilizan únicamente en diagnósticos futuros, mientras que los diagnósticos existentes conservan la información y la decisión sanitaria capturadas al momento de su registro.
- **Medicación**: Representa el tratamiento registrado para una enfermedad tratable.
  - **Relaciones**: referencia una enfermedad y posteriormente puede ser seleccionada en los diagnósticos de galpón cuando la patología no requiere sacrificio sanitario.
- **Diagnóstico del galpón**: Representa el resultado de la evaluación clínica de un galpón en estado `aislamiento`.
  - **Relaciones**: corresponde a un galpón y a su lote activo, referencia directamente una única enfermedad (`enfermedadId`) y, cuando esta es tratable, referencia una medicación compatible (`medicacionId`).

## Success Criteria

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los veterinarios puede completar el registro de una enfermedad válida en menos de 2 minutos.
- **SC-002**: El 95 % de los registros válidos queda disponible en un máximo de 1 segundo después de su confirmación.
- **SC-003**: El 100 % de los intentos realizados por roles distintos al veterinario es rechazado sin crear ni editar una enfermedad.
- **SC-004**: Al menos el 95 % de los veterinarios identifica y corrige los datos obligatorios faltantes en el primer intento durante pruebas de usabilidad.
- **SC-005**: El 100 % de las enfermedades registra una selección explícita para el indicador `requiere sacrificio sanitario`.
- **SC-006**: El 100 % de las enfermedades con el indicador verdadero queda identificado como causa de sacrificio sanitario total.
- **SC-007**: El 100 % de las ediciones realizadas por el veterinario conserva sin cambios los diagnósticos y las decisiones sanitarias existentes.
- **SC-008**: El 95 % de las ediciones válidas queda disponible para diagnósticos futuros en un máximo de 1 segundo después de su confirmación.
