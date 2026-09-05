# Feature Specification: Actualizar inventario vivo por galpón

**Created**: 2026-09-05  

## User Scenarios & Testing 

### User Story 1 - Actualizar la población actual de aves vivas en el lote activo (Priority: P1)

Como sistema de control avícola, quiero actualizar automáticamente el atributo de población actual del lote activo al registrarse un evento de mortalidad en el galpón, descontando con exactitud la cantidad de muertes confirmadas, para mantener en tiempo real el conteo oficial de aves vivas en la granja.

**Why this priority**: Es el caso de uso incluido (`<<include>>`) indispensable que garantiza la consistencia del inventario biológico. La población actual es el insumo fundamental para el cálculo del consumo de alimento diario, control de densidad y proyecciones de producción.

**Independent Test**: Se puede probar invocando la actualización con una cantidad de bajas confirmada sobre un lote activo con población actual conocida, verificando que el atributo `población actual` en la entidad Lote se actualice de forma inmediata y exacta, conservando inalterada la `población inicial`.

**Acceptance Scenarios**:

1. **Scenario**: Actualización exitosa de la población actual por bajas registradas
   - **Given** un galpón con un lote activo cuya llave foránea referencia al galpón y cuenta con una población actual de 8.000 aves vivas
   - **When** se confirma un registro de mortalidad de 40 aves en dicho galpón
   - **Then** el sistema descuenta 40 aves del lote activo
   - **And** actualiza el atributo `población actual` a 7.960 aves vivas
   - **And** mantiene inalterado el valor del atributo `población inicial`

2. **Scenario**: Actualización que reduce la población viva a cero (mortalidad total)
   - **Given** un lote activo con una población actual de 25 aves vivas
   - **When** se confirma un registro de mortalidad de 25 aves
   - **Then** el sistema descuenta las 25 aves y fija la `población actual` en exactamente 0 aves vivas sin permitir valores negativos

3. **Scenario**: Intento de actualización con cantidad de bajas superior a la población actual
   - **Given** un lote activo con una población actual de 50 aves vivas
   - **When** se solicita una actualización de inventario por 60 bajas
   - **Then** el sistema bloquea la actualización, rechaza la transacción y conserva la `población actual` en 50 aves vivas

---

### Edge Cases

- **¿Qué sucede si la cantidad de bajas a descontar es 0 o un valor negativo?**
  - El sistema rechaza la operación y no realiza ninguna modificación sobre la `población actual` del lote.

- **¿Puede la población actual del lote ser un número negativo?**
  - No, bajo ninguna circunstancia el sistema permite valores negativos; el límite mínimo posible de `población actual` es 0.

- **¿Qué sucede si la actualización intenta ejecutarse sobre un lote inexistente o sin llave foránea de galpón?**
  - El sistema cancela la operación e informa una inconsistencia en la referencia del lote.

- **¿La actualización modifica la población inicial del lote?**
  - No, el atributo `población inicial` es inmutable y conserva siempre el número original de aves con el que ingresó el lote al galpón.

- **¿Cómo se garantiza la consistencia concurrente si ocurren múltiples registros en el mismo lote?**
  - El sistema aplica bloqueo transaccional a nivel de registro del lote para asegurar que cada descuento de bajas se aplique secuencial y exactamente sobre el saldo vigente.

---

## Requirements 

### Functional Requirements

- **FR-001**: El sistema DEBE ejecutar la actualización de inventario vivo de forma automática e inmediata cada vez que se confirme un registro de mortalidad en el galpón.
- **FR-002**: El sistema DEBE identificar el lote activo del galpón mediante la llave foránea que referencia al galpón, seleccionando el lote con la fecha de ingreso más reciente.
- **FR-003**: El sistema DEBE obtener los atributos del lote activo: UUID único, Nombre, Población inicial, Población actual y Llave foránea del galpón.
- **FR-004**: El sistema DEBE calcular la nueva población actual mediante la resta: `Población actual nueva = Población actual anterior - Cantidad de muertes`.
- **FR-005**: El sistema DEBE validar que la cantidad de muertes sea menor o igual a la `población actual anterior` antes de aplicar el descuento.
- **FR-006**: El sistema DEBE persistir el nuevo valor calculado en el atributo `población actual` de la entidad Lote.
- **FR-007**: El sistema NO DEBE permitir que el atributo `población actual` tome valores negativos (límite inferior estricto = 0).
- **FR-008**: El sistema DEBE conservar inalterado el valor del atributo `población inicial` de la entidad Lote en toda actualización por bajas.

### Key Entities 

- **Lote**: Representa el grupo de aves registrado para el galpón cuyo inventario vivo es actualizado.
  - Atributos utilizados: `UUID único`, `Nombre`, `Población inicial` (inmutable), `Población actual` (atributo actualizado de aves vivas), `Fecha de ingreso`, `Llave foránea del galpón`.
- **Galpón**: Representa la unidad física de producción referenciada por el lote activo.
  - Atributos utilizados: `UUID único`, `Nombre`, `Aforo máximo`, `Estado`.
- **Registro de mortalidad**: Evento detonante que suministra la cantidad de bajas y la referencia del lote a descontar.
  - Atributos utilizados: `UUID único`, `Cantidad de muertes`, `Llave foránea del lote`, `Llave foránea del galpón`.

---

## Success Criteria 

### Measurable Outcomes

- **SC-001**: El 100 % de las actualizaciones descuenta con exactitud matemática la cantidad de bajas del atributo `población actual` del lote activo.
- **SC-002**: El 100 % de las actualizaciones mantiene inalterado el valor del atributo `población inicial`.
- **SC-003**: En el 100 % de los casos, la `población actual` resultante es un número entero mayor o igual a 0 (0 % de registros con valores negativos).
- **SC-004**: El 100 % de los intentos de descontar cantidades mayores a la población actual es bloqueado sin modificar el lote.
- **SC-005**: La actualización de la población actual se completa de forma inmediata dentro de la misma transacción del registro de bajas.

---

## Out of Scope

- El registro y captura de la fecha, causa y cantidad de muertes (cubierto en SPEC-016 *Registrar mortalidad por galpón*).
- La detección de mortalidad anormal y generación de alertas sanitarias (cubierto en SPEC-017 y SPEC-018).
- El registro de la entrada inicial de aves al lote y fijación de la población inicial.
- La salida o disminución de aves por concepto de sacrificio final o cosecha comercial.
- La creación, edición o administración de galpones y lotes.
