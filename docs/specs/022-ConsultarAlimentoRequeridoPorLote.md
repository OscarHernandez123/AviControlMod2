# Feature Specification: Consultar alimento requerido por lote

**Created**: 2026-09-02  
**Updated**: 2026-09-24

## User Scenarios & Testing *(mandatory)*

### Principio de Dominio: El Lote como Entidad Central de Consumo y Liquidación

En la arquitectura del sistema, el **Galpón** representa la infraestructura física de alojamiento, la cual permanece en la granja y alberga a múltiples lotes sucesivos a lo largo del tiempo (un lote finaliza su ciclo, el galpón entra en descanso sanitario y posteriormente recibe un nuevo lote). 

Por tanto:
- Las curvas nutricionales, la población viva de aves, la mortalidad, las proyecciones de demanda de alimento y la liquidación contable pertenecen al **Lote de pollos** (ej. `#LDP-003`).
- El galpón actúa como la **ubicación física de alojamiento** del lote durante su ciclo.
- La consulta de alimento requerido y su integración con el Módulo 3 (Finanzas) se realiza por **Lote**, permitiendo auditar la historia nutricional y financiera de cada parvada de manera independiente de los lotes anteriores o futuros que hayan pasado por el mismo galpón.

---

### Principio Rector: Inmutabilidad Progresiva y Ajustes Auditados de la Proyección

La proyección de alimento requerido de un lote no es un documento estático desde su concepción ni un cálculo volátil que se sobreescribe sin control. Se rige por el principio de **inmutabilidad progresiva**:

1. **Parámetros Estrictamente Inmutables en Etapa Activa:**
   Durante el transcurso de una etapa activa, la población viva base de inicio (`poblacionInicioEtapa`), la cuota nutricional diaria (`cuotaKgAveDia`), el alimento comercial asignado (`alimentoComercial`), la presentación del bulto (`pesoNominalPorBulto`) y el costo unitario de referencia capturado (`costoUnitarioKg`) **permanecen estrictamente inmutables**. Está prohibido cambiar el alimento comercial o alterar la cuota a mitad de etapa.
2. **Ajustes de Duración y Recálculo de Kilogramos:**
   El único parámetro que puede modificarse durante la etapa activa es su **duración en días** (`diasProrroga` y `diasEfectivos`), estrictamente bajo las causales zootécnicas de contingencia reguladas en el [SPEC-021](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/021-AjustarPlanNutricionalPorEtapa.md) (cuarentena sanitaria o bajo peso). Dicho ajuste recalcula automáticamente los kilogramos proyectados (`proyeccionKg = poblacionInicioEtapa * cuotaKgAveDia * diasEfectivos`), manteniendo exactamente el mismo alimento comercial y el mismo costo unitario capturado.
3. **Historial Acumulativo de Ajustes (Sin Sobreescritura):**
   Cada ajuste o prórroga genera un registro inmutable en el historial de ajustes (`historialAjustes`), guardando marca de tiempo (`fechaAjuste`), usuario responsable, motivo de contingencia, justificación técnica y los valores previos y nuevos de duración y kilogramos. Múltiples ajustes sucesivos se anexan de manera cronológica y **jamás sobreescriben los ajustes previos**.
4. **Inmutabilidad Absoluta al Completar la Etapa:**
   Una vez que el lote concluye la etapa y esta pasa a estado `COMPLETADA` (o cuando el lote finaliza su ciclo productivo), la proyección de dicha etapa adquiere **inmutabilidad absoluta**: se bloquea de forma irreversible cualquier modificación en días, kilogramos, costos o historial.
5. **Blindaje ante Sustitución de Plan Nutricional:**
   La sustitución del plan nutricional de un lote activo ([SPEC-021](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/021-AjustarPlanNutricionalPorEtapa.md) US-2) jamás puede utilizarse para evadir las restricciones de la etapa activa. La etapa en curso preserva su alimento bloqueado, su costo unitario capturado, sus kilogramos y su historial acumulado; el nuevo plan aplica exclusivamente a las etapas posteriores que aún no hayan comenzado.

---

### Principio Rector: Captura, Conservación y Naturaleza Referencial del Costo Unitario

1. **Definición de `costoUnitarioKg`:**
   El costo unitario por kilogramo (`costoUnitarioKg`) registrado en la proyección representa el **costo de referencia capturado en el momento exacto de activación de la etapa**, no el precio vigente en catálogo o en compras al momento de realizar la consulta.
2. **Naturaleza Referencial vs. Costo Real Consumido:**
   Este costo es un **valor de referencia presupuestaria** diseñado para que el Módulo 3 (Finanzas) estime y valore la demanda proyectada del lote. **No demuestra ni constituye el costo real del alimento físicamente consumido**, el cual es calculado por el Módulo 3 a partir de los despachos físicos diarios y la suma de $\text{kg consumidos} \times \text{precio neto histórico de compra de cada recepción}$ según el [SPEC-001](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/001-RegistrarRecepcionDeAlimento.md) (FR-008).
3. **Trazabilidad de la Captura:**
   El sistema almacena obligatoriamente la fuente del costo (`fuenteCosto`, ej. número de recepción o precio de referencia de compra) y el instante exacto de captura (`fechaCapturaCosto`).
4. **Invariabilidad ante Cambios Posteriores:**
   Nuevas compras de alimento, fluctuaciones de precio en catálogo, recepciones posteriores y prórrogas de duración en la etapa activa **jamás alteran el costo unitario ya capturado**.
5. **Tratamiento de Costo No Disponible al Activar la Etapa:**
   Si al momento de activar la etapa no existe un costo de referencia disponible en el inventario o recepciones, el sistema registra el campo como nulo (`costoUnitarioKg = null`) y asocia una advertencia explícita (`advertenciaCosto`). **Está estrictamente prohibido sustituir el valor faltante por cero (`0.00`)** o por valores arbitrarios.
6. **Completado Único Auditado durante Etapa Activa:**
   Mientras la etapa permanezca en estado `ACTIVA`, el sistema permite a un usuario autorizado (Administrador) completar dicho costo pendiente por **una única vez** mediante una operación explícita y auditada. El valor ingresado debe corresponder al costo de referencia histórico aplicable al momento en que se activó la etapa, no tomar ciegamente el precio del día. Una vez completado, el campo queda bloqueado contra nuevas ediciones.
7. **Cierre de Etapa con Costo Pendiente:**
   Si la etapa finaliza y pasa a estado `COMPLETADA` manteniendo el costo en `null`, dicho estado se conserva de manera inmutable junto con su advertencia. La proyección cerrada no se altera posteriormente en este módulo.
8. **Decisión de Negocio Pendiente (Método de Valoración de Inventario):**
   Tras revisar el catálogo, recepciones ([SPEC-001](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/001-RegistrarRecepcionDeAlimento.md)) y la consulta de inventario ([SPEC-023](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/023-ConsultarInventario.md)), se constata que SPEC-001 almacena precios netos de compra por kilogramo para cada recepción individual, pero SPEC-023 consolida únicamente existencias físicas sin fijar una regla de valoración contable de inventario. En consecuencia, cuando existan recepciones coexistentes del mismo alimento comercial con precios de compra heterogéneos en bodega central, la regla exacta para determinar el `costoUnitarioKg` de referencia (ej. Costo Promedio Ponderado, PEPS/FIFO o Última Compra) queda definida explícitamente como una **Decisión de Negocio Pendiente** a concertar formalmente con los responsables de Finanzas (Módulo 3) y Logística (Módulo 1). El sistema no asume ninguna regla silenciosa.

---

### User Story 1 - Consultar el alimento requerido por etapa en el lote para Módulo 3 (Finanzas) (Priority: P1)

Como sistema de Finanzas (Módulo 3) o administrador, quiero consultar la proyección del alimento requerido en kilogramos para un lote específico en la etapa por la que esté pasando o el consolidado de etapas al finalizar su ciclo productivo, manteniendo los requerimientos separados por etapa e incluyendo el costo unitario por kilogramo de referencia capturado para cada tipo de alimento, para efectuar la liquidación final y el control presupuestario sin sumar kilogramos globales de alimentos diferentes.

**Why this priority**: Representa la conexión oficial y directa entre el Módulo 2 y el Módulo 3 (Finanzas) modelada en el diagrama de casos de uso (`Modulo2_v4.drawio`). Permite a Finanzas disponer de los requerimientos físicos de alimento en kilogramos y sus costos unitarios de referencia correspondientes por etapa para un lote determinado, facilitando la liquidación contable exacta sin mezclar insumos de diferente composición nutricional y precio.

**Independent Test**: Se puede probar sobre un lote que finalizó su ciclo completo (Pre-inicio de 7 días con 10.000 aves, cuota de 0.035 kg y costo capturado de $1.800/kg; Inicio de 14 días con 9.750 aves, cuota de 0.045 kg y costo capturado de $1.650/kg; y Engorde de 21 días con 9.600 aves, cuota de 0.070 kg y costo capturado de $1.500/kg), verificando que la consulta entregue cada etapa discriminada de forma separada con sus respectivos kilogramos (2.450 kg, 6.142,50 kg y 14.112 kg) y costos unitarios de referencia, sin generar una sumatoria global de kilogramos entre etapas.

**Acceptance Scenarios**:

1. **Scenario**: Consulta de requerimiento para la etapa activa que está cursando el lote
   - **Given** un lote activo alojado en un galpón cursando la etapa "Inicio" con duración de 14 días, una población viva al inicio de la etapa de 9.750 aves, cuota nutricional de 0.045 kg/ave/día y alimento con costo de referencia capturado de $1.650/kg
   - **When** el Módulo 3 (Finanzas) consulta el alimento requerido para dicho lote y etapa
   - **Then** el sistema entrega los datos de la etapa "Inicio" con 6.142,50 kg de alimento Iniciador y un costo unitario capturado de $1.650/kg, detallando el código del lote, galpón de alojamiento, duración en días (14), población viva al corte (9.750 aves), cuota diaria aplicada, fuente del costo y fecha/hora de captura.

2. **Scenario**: Consulta del consolidado discriminado por etapas al finalizar el ciclo productivo del lote
   - **Given** un lote de pollos que finalizó su última etapa productiva ("Engorde/Broiler") habiendo transitado por Pre-inicio, Inicio y Engorde
   - **When** el Módulo 3 (Finanzas) consulta el alimento requerido del lote al cierre del ciclo
   - **Then** el sistema retorna el consolidado con el estado "Finalizado", entregando el registro individual y separado de cada etapa con su tipo de alimento, población al inicio, cuota aplicada, días base y de prórroga, kilogramos proyectados y costo unitario de referencia capturado de cada alimento
   - **And** no incluye una sumatoria total de kilogramos entre las diferentes etapas.

3. **Scenario**: Consulta de requerimiento con etapa prorrogada por contingencia zootécnica
   - **Given** un lote en etapa "Inicio" con duración base de 14 días (6.142,50 kg presupuestados originalmente), una prórroga vigente de 4 días adicionales autorizada por contingencia (sea cuarentena sanitaria preventiva o retraso en ganancia de peso según SPEC-021) y alimento Iniciador con costo de referencia capturado de $1.650/kg
   - **When** el Módulo 3 consulta el requerimiento de alimento de dicha etapa
   - **Then** el sistema entrega la proyección calculada sobre los 18 días efectivos totales (14 base + 4 prórroga), reportando un total ajustado de 7.897,50 kg
   - **And** desglosa los días base (14), los días de prórroga (4), el motivo de contingencia (`CUARENTENA_SANITARIA` o `BAJO_PESO`), la justificación técnica registrada, el costo unitario invariable de $1.650/kg y la entrada correspondiente en el historial de ajustes.

4. **Scenario**: Entrega de requerimientos separados por etapa con costo unitario para liquidación
   - **Given** una solicitud de consulta realizada desde el Módulo 3 para fines de liquidación de un lote
   - **When** se procesa y emite la respuesta
   - **Then** el sistema entrega los kilogramos requeridos de forma estrictamente separada por etapa junto con el costo unitario por kilogramo (`costoUnitarioKg`) de referencia de cada tipo de alimento, omitiendo sumatorias agregadas de kilogramos heterogéneos.

5. **Scenario**: Prórrogas sucesivas sobre la etapa activa acumuladas en el historial de ajustes
   - **Given** un lote en etapa activa "Inicio" con duración base de 14 días (6.142,50 kg) que recibió una primera prórroga de 3 días por `BAJO_PESO` (quedando en 17 días y 7.458,75 kg)
   - **When** surge una segunda contingencia y se aprueba una prórroga adicional de 2 días por `CUARENTENA_SANITARIA`
   - **Then** el sistema actualiza la proyección activa a 19 días efectivos y 8.336,25 kg manteniendo inmutable el alimento y el costo unitario ($1.650/kg)
   - **And** anexa un segundo registro al historial de ajustes (`historialAjustes`) con su propia fecha/hora, usuario, motivo (`CUARENTENA_SANITARIA`), justificación técnica y valores anteriores (17 días, 7.458,75 kg) y nuevos (19 días, 8.336,25 kg), sin sobreescribir ni eliminar el primer ajuste registrado.

6. **Scenario**: Invariabilidad del costo unitario capturado ante compras o cambios de precio posteriores
   - **Given** una etapa activa "Inicio" cuya proyección capturó un costo de referencia de $1.650/kg proveniente de la recepción activa al momento del inicio
   - **When** diez días después ingresa a bodega central una nueva recepción del mismo alimento con un precio de compra de $1.850/kg ([SPEC-001](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/001-RegistrarRecepcionDeAlimento.md)) o se aprueba una prórroga de días
   - **Then** la proyección de la etapa activa conserva inalterado su `costoUnitarioKg = 1650.00`, su `fuenteCosto` y su `fechaCapturaCosto` originales, sin adoptar el nuevo precio de compra.

7. **Scenario**: Consulta de etapa con costo de referencia no disponible (`null`) y advertencia
   - **Given** un lote cursando una etapa activa donde al momento de su activación no existían compras ni recepciones con precio registrado en el catálogo
   - **When** el Módulo 3 o el administrador consulta la proyección de dicha etapa
   - **Then** el sistema entrega los kilogramos calculados normalmente pero reporta `costoUnitarioKg = null`
   - **And** adjunta una advertencia explícita en los metadatos de la respuesta: `"Costo de referencia no disponible al momento de activación de la etapa"`, sin asignar en ningún momento el valor cero ($0.00).

8. **Scenario**: Rechazo estricto de modificaciones o prórrogas en etapas completadas
   - **Given** un lote que completó la etapa "Pre-inicio" hace 10 días (`estadoEtapa = COMPLETADA`) y actualmente cursa la etapa "Inicio"
   - **When** un usuario intenta modificar la duración, prórroga, costo o alimento de la etapa "Pre-inicio"
   - **Then** el sistema rechaza la solicitud indicando que las etapas completadas son completamente inmutables
   - **And** mantiene intactos la proyección persistida y el historial de dicha etapa.

---

### User Story 2 - Adición automática de la proyección de alimento al cambiar de etapa en el lote (Priority: P2)

Como sistema de gestión nutricional y productiva del lote, quiero que al registrarse el cambio de etapa de un lote se capture el alimento asignado a la nueva etapa (el cual queda bloqueado e inmutable para dicha etapa según el [SPEC-021](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/021-AjustarPlanNutricionalPorEtapa.md)) y se calcule y agregue automáticamente la proyección de alimento requerido en kilogramos para ese lote en esa nueva etapa con la población viva de ese momento y su costo unitario de referencia capturado en dicha activación, para mantener el historial acumulado disponible y actualizado para la consulta del Módulo 3 (Finanzas).

**Why this priority**: Asegura que el historial de proyecciones se alimente de manera automática en cada transición de etapa del lote, garantizando que cada fase calcule sus kilogramos con la población viva real al momento de su inicio y asocie el costo unitario correspondiente antes de que Finanzas consulte el cierre del ciclo. Al bloquearse el cambio de alimento durante la etapa en el SPEC-021, se previene la mezcla de costos unitarios e inconsistencias contables.

**Independent Test**: Se puede probar registrando el cambio de etapa de un lote de "Pre-inicio" a "Inicio" con 9.750 aves vivas registradas al corte, verificando que se agregue de inmediato el nuevo registro de proyección para la etapa "Inicio" (con sus kg y costo unitario capturado) sin modificar el registro previo de "Pre-inicio", y comprobando que el alimento de la etapa activa no pueda ser alterado a mitad de fase.

**Acceptance Scenarios**:

1. **Scenario**: Registro de la proyección inicial al arrancar la primera etapa del lote (Pre-inicio)
   - **Given** la recepción de un nuevo lote de 10.000 pollos alojado en un galpón iniciando la etapa "Pre-inicio" (7 días con cuota de 0.035 kg/ave/día y alimento Pre-iniciador con costo de referencia disponible de $1.800/kg)
   - **When** se activa el lote en el sistema
   - **Then** el sistema calcula y almacena la proyección de la etapa "Pre-inicio" por 2.450 kg calculada con la población de recepción
   - **And** captura y persiste `costoUnitarioKg = 1800.00`, la fuente de dicho costo y la marca de tiempo de captura
   - **And** bloquea la modificación del producto alimenticio asignado para esa etapa activa según las directrices de SPEC-021.

2. **Scenario**: Adición automática de nueva proyección al cambiar a una etapa posterior
   - **Given** un lote que concluye la etapa "Pre-inicio" y cuenta con 9.750 aves vivas registradas al corte
   - **When** se registra la transición a la etapa "Inicio" (14 días con cuota de 0.045 kg/ave/día y alimento Iniciador con costo de referencia de $1.650/kg)
   - **Then** el sistema marca la etapa "Pre-inicio" como `COMPLETADA` fijando su inmutabilidad absoluta
   - **And** calcula la proyección de la nueva etapa "Inicio" con las 9.750 aves vivas (6.142,50 kg), captura el costo unitario de $1.650/kg en su fecha de activación
   - **And** agrega este registro al historial del lote manteniendo inalterada la proyección previa de "Pre-inicio".

3. **Scenario**: Actualización auditada de la proyección al aplicarse una prórroga por contingencia
   - **Given** un lote con su proyección inicial de etapa activa persistida en el sistema (ej. 14 días base y 6.142,50 kg)
   - **When** se aprueba y registra una prórroga de días en el lote conforme a SPEC-021 US-4 (sea por cuarentena sanitaria o retraso en curva de peso)
   - **Then** el sistema actualiza el registro de la proyección de dicha etapa activa: registra los `diasProrroga` (ej. 4 días), recalcula los `diasEfectivos = diasBase + diasProrroga` (18 días) y actualiza los kilogramos requeridos a 7.897,50 kg
   - **And** mantiene inmutables la población inicial, la cuota diaria, el alimento comercial y el costo unitario asignados
   - **And** genera una entrada en el historial de ajustes guardando la fecha/hora, el motivo de contingencia (`CUARENTENA_SANITARIA` o `BAJO_PESO`), la justificación técnica y el usuario responsable para auditoría contable y zootécnica.

4. **Scenario**: Activación de etapa con costo no disponible (`null`), advertencia y completado único auditado
   - **Given** la activación de una etapa cuyo alimento comercial no posee precio de referencia registrado en recepciones activas
   - **When** se crea la proyección de dicha etapa
   - **Then** el sistema guarda `costoUnitarioKg = null`, `fuenteCosto = null` y emite una advertencia de costo pendiente
   - **And** cuando un usuario administrador ejecuta una operación explícita de captura tardía ingresando el costo de referencia histórico aplicable a la fecha de activación ($1.620/kg) con su debida justificación
   - **Then** el sistema persiste dicho valor, registra el usuario, fecha y fuente, y bloquea irreversiblemente cualquier edición posterior de ese costo para la etapa.

5. **Scenario**: Cierre de etapa con costo de referencia no completado
   - **Given** una etapa activa que operó con `costoUnitarioKg = null` y cuya contingencia de costo no fue completada durante su transcurso
   - **When** la etapa concluye y se registra el paso a la siguiente etapa o finalización del lote
   - **Then** el sistema marca la etapa finalizada como `COMPLETADA` conservando de forma inmutable `costoUnitarioKg = null` y la advertencia asociada
   - **And** bloquea cualquier intento posterior de completar o modificar el costo de dicha etapa cerrada.

6. **Scenario**: Blindaje contra evasión de restricciones mediante sustitución de plan nutricional
   - **Given** un lote activo en etapa "Inicio" con proyección calculada de 6.142,50 kg, alimento Iniciador bloqueado y costo capturado de $1.650/kg
   - **When** el nutricionista ejecuta una sustitución del plan nutricional del lote conforme a SPEC-021 US-2
   - **Then** el sistema preserva inalterados el alimento bloqueado, la proyección de kilogramos, el costo unitario capturado y el historial de ajustes de la etapa "Inicio" en curso
   - **And** aplica la nueva pauta del plan exclusivamente a las etapas futuras que no hayan iniciado ("Engorde").

---

### User Story 3 - Consulta consolidada de demanda proyectada para lotes activos y planificación de compras (Priority: P2)

Como administrador de la granja (o encargado de compras y logística), quiero consultar una vista consolidada de la demanda de alimento proyectada para todos los lotes activos en su etapa vigente, agrupada por producto comercial y comparada contra el inventario disponible en bodega central ([SPEC-023](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/023-ConsultarInventario.md)), para conocer con exactitud cuánto alimento se requiere para sostener la producción activa, determinar los faltantes de stock y planificar oportunamente las órdenes de compra sin necesidad de apartar físicamente los bultos en bodega.

**Why this priority**: Resuelve la necesidad logística de compras sin incurrir en bloqueos físicos de stock. Al no apartar el alimento desde el día 1, el administrador requiere una herramienta de inteligencia operativa que sume la demanda de todas las etapas en curso de los lotes vivos y la compare contra las existencias reales, identificando compras urgentes antes de que ocurra un desabastecimiento.

**Independent Test**: Se puede probar con múltiples lotes activos (ej. Lote 1 con demanda de 6.142,50 kg de Iniciador, Lote 2 con 5.800 kg de Iniciador y Lote 3 con 14.000 kg de Engorde), verificando que el sistema totalice 11.942,50 kg de Iniciador (299 bultos de 40 kg), lo compare contra un stock en bodega de 8.000 kg (200 bultos) y reporte un déficit exacto de compra de 3.942,50 kg (99 bultos) con alerta de reabastecimiento, sin alterar ni descontar inventario.

**Acceptance Scenarios**:

1. **Scenario**: Consolidación de demanda proyectada de todos los lotes activos por producto
   - **Given** varios lotes activos en etapas productivas vigentes en diferentes galpones
   - **When** el administrador consulta el consolidado de demanda de lotes activos
   - **Then** el sistema presenta el total demandado agrupado por alimento comercial y peso nominal de bulto, detallando la suma de kilogramos requeridos y su equivalente en bultos
   - **And** permite expandir cada producto para visualizar la lista de lotes y los galpones donde están alojados que contribuyen a dicha demanda.

2. **Scenario**: Balance comparativo contra el stock disponible en bodega central (Cálculo de déficit de compra)
   - **Given** una demanda consolidada de 11.942,50 kg (299 bultos) para un alimento comercial en los lotes activos y un stock disponible en bodega central de 8.000 kg (200 bultos) según el SPEC-023
   - **When** el administrador consulta el balance de abastecimiento
   - **Then** el sistema calcula y exhibe un déficit de compra de 3.942,50 kg (99 bultos) requeridos para completar las etapas en curso de los lotes
   - **And** clasifica el producto con la alerta `"Reabastecimiento Necesario"`.

3. **Scenario**: Cobertura suficiente para la etapa activa sin requerimiento de compra inmediata
   - **Given** una demanda consolidada de 5.000 kg (125 bultos) y un stock disponible en bodega central de 8.000 kg (200 bultos)
   - **When** el administrador consulta el balance
   - **Then** el sistema reporta un saldo cubierto con superávit de 3.000 kg (75 bultos) para la etapa de los lotes
   - **And** clasifica el estado de abastecimiento como `"Stock Suficiente para Etapa Activa"`.

4. **Scenario**: Exclusión de lotes finalizados o galpones en descanso sanitario
   - **Given** lotes en estado finalizado o galpones en vacío sanitario sin aves
   - **When** el sistema genera la consolidación de demanda de lotes activos
   - **Then** excluye dichos registros del cálculo, considerando únicamente lotes en etapas productivas en progreso.

---

### Edge Cases

- **Edge case #1 - Inmutabilidad de parámetros base y flexibilidad auditada de duración en etapa activa**

  - ¿Cómo garantiza el sistema que la proyección no mezcle insumos ni costos heterogéneos si ocurre una contingencia durante la etapa activa?  
    El sistema mantiene estrictamente inmutables la población base al corte, la ración diaria, el alimento comercial, la presentación del bulto y el costo unitario de referencia capturado al activar la etapa. El único cambio permitido durante la etapa activa es la extensión o ajuste de duración en días bajo las reglas de contingencia del SPEC-021. Al registrarse una prórroga, se recalculan los kilogramos requeridos sobre los nuevos días efectivos totales, registrando el ajuste en el historial acumulativo y manteniendo invariable el costo unitario original.

- **Edge case #2 - Convivencia de múltiples lotes históricos alojados sucesivamente en un mismo galpón**

  - ¿Cómo evita el sistema que las proyecciones de un nuevo lote que ingresa a un galpón sobreescriban o se mezclen con las proyecciones del lote anterior que ya concluyó su ciclo en ese mismo galpón?  
    El sistema vincula todas las proyecciones al identificador único e inmutable del lote (`loteId`). Al consultar la historia o liquidación, filtra exclusivamente por el lote correspondiente, garantizando que los registros del lote anterior permanezcan inalterados y archivados con su respectivo código de lote.

- **Edge case #3 - Consulta de requerimientos para un lote inexistente en la base de datos**

  - ¿Cómo responde el servicio de consulta si el Módulo 3 (Finanzas) solicita la proyección enviando un identificador de lote no registrado o eliminado?  
    El sistema debe interceptar la solicitud, evitar fallas internas no controladas y retornar un código de respuesta HTTP 404 (Not Found) estructurado con un mensaje descriptivo que informe que el lote solicitado no existe en el sistema.

- **Edge case #4 - Petición con parámetros vacíos o tipos de datos inválidos**

  - ¿Qué respuesta emite el sistema si la invocación API carece de identificadores obligatorios o incluye formatos incompatibles (ej. caracteres alfanuméricos en identificadores numéricos)?  
    El sistema debe validar los parámetros de entrada antes de ejecutar la consulta y responder con un código HTTP 400 (Bad Request), detallando el parámetro erróneo sin ejecutar cálculos innecesarios ni comprometer la disponibilidad del servicio.

- **Edge case #5 - Lote activo con población viva en cero por contingencia extrema de mortalidad**

  - ¿Cómo calcula la proyección el sistema si al registrar el cambio de etapa la población viva del lote es igual a cero aves por mortalidad total o anomalía zootécnica?  
    El sistema debe registrar la etapa asignando exactamente `0.00 kg` a la proyección de requerimiento, evitando excepciones de división por cero o desbordamientos numéricos, y registrando el costo unitario correspondiente para fines de auditoría.

- **Edge case #6 - Alimento comercial sin costo de referencia al activar la etapa (`null` y completado único)**

  - ¿Qué información retorna la consulta y cómo se subsana si al momento de activar la etapa el alimento no tiene costo de compra disponible?  
    El sistema persiste `costoUnitarioKg = null` y asocia una advertencia descriptiva en los metadatos. Está prohibido registrar `0.00`. Mientras la etapa permanezca activa, un usuario administrador puede completar dicho valor por una única vez mediante una acción auditada indicando el costo de referencia aplicable a la activación. Si la etapa concluye sin haber completado el costo, la proyección se cierra conservando `null` y la advertencia de forma irreversible.

- **Edge case #7 - Interrupción de red o timeout durante la integración API con el Módulo 3**

  - ¿Cómo se garantiza la consistencia si la comunicación entre el Módulo 2 y el Módulo 3 se corta mientras se transfiere la consulta de requerimientos?  
    El servicio de consulta debe ser estrictamente de solo lectura e idempotente. Ante un fallo de red o tiempo de espera agotado, el Módulo 3 puede reintentar la solicitud cuantas veces sea necesario sin generar duplicidades, cambios de estado ni bloqueos en los registros del lote.

- **Edge case #8 - Redondeo y precisión matemática en la serialización de kilogramos y costos**

  - ¿Cómo maneja el sistema las discrepancias de decimales al calcular y serializar los kilogramos requeridos y los costos unitarios?  
    El sistema debe aplicar redondeo numérico estándar (*half-up*) a dos decimales (`0.01`) tanto para los kilogramos de alimento proyectados como para los montos monetarios de costo unitario, evitando discrepancias de centavos o fracciones acumuladas en el transporte JSON hacia el Módulo 3.

- **Edge case #9 - Coexistencia de recepciones con precios heterogéneos para el mismo alimento (Decisión de Negocio Pendiente)**

  - ¿Qué precio unitario de referencia adopta el sistema al activar la etapa si existen varias recepciones confirmadas del mismo alimento con precios netos de compra diferentes en bodega central?  
    Dado que las especificaciones actuales de recepciones ([SPEC-001](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/001-RegistrarRecepcionDeAlimento.md)) e inventario ([SPEC-023](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/023-ConsultarInventario.md)) no fijan una política de valoración de inventario (Promedio Ponderado, FIFO/PEPS o Última Compra), este método queda explícitamente documentado como una decisión de negocio pendiente a concertar con Finanzas y Logística. El sistema no aplicará reglas heurísticas ocultas.

- **Edge case #10 - Intento de modificación sobre proyecciones de etapas completadas o cerradas**

  - ¿Qué sucede si se intenta registrar una prórroga, modificar días o alterar el costo de una etapa que ya fue completada o de un lote finalizado?  
    El sistema rechaza la solicitud de forma inmediata emitiendo un código de error de negocio (HTTP 422 / 409), notificando que las proyecciones de etapas completadas son totalmente inmutables para garantizar la trazabilidad contable del Módulo 3.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE exponer un servicio de consulta exclusivo para que el Módulo 3 (Finanzas) y usuarios autorizados consulten la proyección del alimento requerido por lote, correspondiendo formalmente al caso de uso `"Consultar alimento requerido por lote"` del sistema.
- **FR-002**: Al registrarse un cambio de etapa en un lote, el sistema DEBE calcular y agregar automáticamente la proyección de alimento requerido para ese lote en esa nueva etapa al registro histórico de dicho lote, capturando el alimento y cuota fijados en el plan nutricional ([SPEC-021](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/021-AjustarPlanNutricionalPorEtapa.md)).
- **FR-003**: El cálculo de la proyección de alimento de cada etapa del lote DEBE regirse estrictamente por la fórmula:  
  `Kilogramos Requeridos = Población Viva al Iniciar la Etapa * Cuota Diaria (kg/ave/día) * Días Efectivos de la Etapa`.
- **FR-004**: Para la primera etapa (`Pre-inicio`), la población viva base DEBE ser la población inicial de recepción del lote; para cada etapa subsiguiente (`Inicio`, `Broiler/Engorde`), la población viva base DEBE corresponder a la población de aves vivas existente en el lote al momento de registrar el cambio de etapa.
- **FR-005**: Si una etapa activa del lote cuenta con una prórroga o ajuste temporal por contingencia (cuarentena sanitaria preventiva o retraso en peso zootécnico conforme a SPEC-021), la proyección de dicha etapa DEBE actualizar sus kilogramos considerando los días totales efectivos (`diasEfectivos = diasBase + diasProrroga`) multiplicados por la cuota y utilizando el costo unitario de referencia capturado al inicio de la etapa (el cual permanece invariable). La respuesta DEBE desglosar los días base, días de prórroga, motivo zootécnico (`CUARENTENA_SANITARIA` o `BAJO_PESO`), justificación registrada y registrar el evento en el historial de ajustes de proyección (`historialAjustes`).
- **FR-006**: La respuesta a la consulta para el Módulo 3 DEBE entregar los requerimientos de alimento estrictamente separados e individualizados por cada etapa del lote, absteniéndose de sumar o consolidar un total global de kilogramos entre distintas etapas.
- **FR-007**: Por cada etapa incluida en la consulta, el sistema DEBE proporcionar el costo unitario de referencia por kilogramo (`costoUnitarioKg`) capturado en el momento de activación de dicha etapa (no el precio vigente al momento de consultar), aclarando que dicho costo es un valor de referencia para valorar la proyección y no demuestra el costo real del alimento consumido (el cual es calculado por el Módulo 3 a partir de los consumos diarios reales conforme a SPEC-001 FR-008).
- **FR-008**: La respuesta a la consulta del Módulo 3 DEBE detallar: código del lote, galpón de alojamiento actual, estado del ciclo (En progreso o Finalizado), fecha de consulta, y una lista discriminada por etapa conteniendo: nombre de la etapa, tipo de alimento asociado, población viva al inicio de etapa, cuota diaria aplicada, días base, días de prórroga, días efectivos de duración, motivo de contingencia (si aplica), total de kilogramos requeridos de esa etapa, costo unitario por kilogramo de referencia (`costoUnitarioKg`), fuente del costo (`fuenteCosto`), fecha/hora de captura (`fechaCapturaCosto`), advertencia de costo (si aplica) y la lista de ajustes históricos acumulados.
- **FR-009**: Durante el transcurso de una etapa activa, el sistema DEBE mantener estrictamente inmutables la población base de corte, la cuota diaria, el alimento comercial, la presentación del bulto y el costo unitario capturado; únicamente se permite modificar la duración en días bajo las reglas de contingencia del SPEC-021, recalculando la proyección de kilogramos. Al completarse la etapa (`COMPLETADA`), su proyección DEBE quedar completamente inmutable, bloqueando cualquier modificación posterior de días, kilogramos o costos.
- **FR-010**: El sistema DEBE rechazar cualquier intento de modificar o reemplazar el producto comercial asignado a una etapa activa en curso de un lote, así como cualquier intento de evadir esta restricción mediante la sustitución del plan nutricional del lote.
- **FR-011**: El sistema DEBE proveer al Administrador y al Nutricionista una vista consolidada de la demanda de alimento proyectada para todos los lotes activos en su etapa vigente, incorporando de forma inmediata los días efectivos totales con sus prórrogas activas.
- **FR-012**: La vista consolidada DEBE agrupar la demanda por producto comercial (`Alimento`) y presentación (`pesoNominalPorBulto`), calculando la sumatoria total en kilogramos y bultos equivalentes requeridos para sostener la etapa en curso de los lotes.
- **FR-013**: El sistema DEBE cruzar en tiempo real la demanda consolidada de cada producto contra su existencia disponible vigente en bodega central obtenida desde el SPEC-023 (excluyendo recepciones vencidas o anuladas).
- **FR-014**: El sistema DEBE calcular el balance de abastecimiento para cada alimento como: `BalanceKg = ExistenciaDisponibleKg - DemandaConsolidadaKg`. Si el balance es negativo, el sistema DEBE clasificar el producto bajo la alerta `"Reabastecimiento Necesario"` y mostrar la cantidad exacta de bultos sugeridos a comprar.
- **FR-015**: La consulta consolidada de abastecimiento DEBE operar estrictamente en modo de solo lectura y NO DEBE descontar, reservar ni bloquear existencias físicas en el inventario de bodega central.
- **FR-016**: Al activar una etapa, el sistema DEBE capturar y persistir en la proyección el `costoUnitarioKg`, la fuente de procedencia (`fuenteCosto`, ej. identificación de recepción de compra o catálogo) y el momento exacto de captura (`fechaCapturaCosto`). Las compras posteriores, recepciones con nuevos precios y prórrogas de duración NO DEBEN alterar el costo capturado.
- **FR-017**: Si al momento de activar la etapa no existe un costo de referencia disponible, el sistema DEBE registrar `costoUnitarioKg = null` y asociar una advertencia descriptiva (`advertenciaCosto`). El sistema NO DEBE sustituir el costo faltante por cero (`0.00`).
- **FR-018**: El sistema DEBE permitir a un usuario autorizado con rol de Administrador completar el costo unitario pendiente (`null`) por una única vez mientras la etapa continúe activa (`ACTIVA`), mediante una operación explícita y auditada que capture el costo de referencia correspondiente a la activación, la justificación y el usuario. Una vez completado, el campo queda bloqueado contra posteriores modificaciones.
- **FR-019**: Si una etapa finaliza y transiciona a estado `COMPLETADA` manteniendo `costoUnitarioKg = null`, el sistema DEBE conservar de forma inmutable dicho valor nulo y su advertencia, rechazando cualquier intento posterior de completar o alterar la proyección cerrada.
- **FR-020**: El sistema DEBE documentar explícitamente como una Decisión de Negocio Pendiente la regla y método de valoración de inventario (Promedio Ponderado, PEPS/FIFO o Última Compra) para obtener el costo de referencia cuando coexistan recepciones con precios heterogéneos para un mismo alimento en bodega central.
- **FR-021**: El sistema DEBE registrar cada ajuste de duración de una etapa activa en una entidad histórica inmutable (`HistorialAjusteProyeccion`), conservando fecha/hora, usuario responsable, motivo de contingencia, justificación técnica y los valores anteriores y nuevos de días y kilogramos. Los ajustes sucesivos DEBEN anexarse al historial sin sobreescribir los registros anteriores.
- **FR-022**: El sistema DEBE bloquear y rechazar de forma absoluta cualquier solicitud de ajuste de días, modificación de cuotas o cambio de costos sobre proyecciones de etapas cuyo estado sea `COMPLETADA` o pertenezcan a lotes finalizados.

---

### Key Entities

- **ProyeccionAlimentoEtapa**: Registro persistido de la proyección calculada para una etapa específica del lote.
  - *Atributos*: ID, loteId (clave principal de la parvada), galponId (galpón donde se encuentra alojado), nombreEtapa (Pre-inicio, Inicio, Engorde), tipoAlimento, alimentoComercial, pesoNominalPorBulto, poblacionInicioEtapa, cuotaKgAveDia, diasBase, diasProrroga, diasEfectivos, estadoEtapa (`ACTIVA`, `COMPLETADA`), proyeccionKg, costoUnitarioKg (numérico con 2 decimales, nullable), fuenteCosto (nullable), fechaCapturaCosto (timestamp, nullable), advertenciaCosto (nullable), costoCompletadoManualmente (booleano), fechaRegistro, fechaActualizacion.
  - *Relaciones*: contiene una lista de `HistorialAjusteProyeccion`.
- **HistorialAjusteProyeccion**: Registro histórico e inmutable de los ajustes aplicados a la duración y kilogramos de una proyección de etapa activa.
  - *Atributos*: ID, proyeccionEtapaId, fechaAjuste, usuarioAjuste, motivoAjuste (`CUARENTENA_SANITARIA`, `BAJO_PESO`), justificacionAjuste, diasBaseAnterior, diasProrrogaAnterior, diasEfectivosAnterior, proyeccionKgAnterior, diasBaseNuevo, diasProrrogaNuevo, diasEfectivosNuevo, proyeccionKgNuevo.
- **ConsolidadoRequerimientoLote**: Estructura de transferencia de datos (DTO) entregada en la respuesta al Módulo 3.
  - *Atributos*: loteId, galponId, estadoCiclo (FINALIZADO, EN_PROGRESO), listaEtapasProyectadas (cada elemento con nombreEtapa, tipoAlimento, proyeccionKg, costoUnitarioKg, fuenteCosto, fechaCapturaCosto, advertenciaCosto, diasBase, diasProrroga, diasEfectivos, estadoEtapa, motivoProrroga, poblacionInicioEtapa, historialAjustes), fechaConsulta.
- **BalanceAbastecimientoAlimento**: Modelo consolidado de proyección y compra para el administrador.
  - *Atributos*: alimentoId, nombreComercial, tipoAlimento, pesoNominalPorBulto, demandaConsolidadaKg, demandaConsolidadaBultos, stockDisponibleKg, stockDisponibleBultos, deficitCompraKg, deficitCompraBultos, estadoAbastecimiento (SUFICIENTE, REABASTECIMIENTO_NECESARIO).
- **LotePollos**: Agrupación biológica de aves que constituye la unidad de producción, costo, proyección y liquidación.
  - *Atributos*: ID, codigoLote, tipoAve, galponAlojamientoId, poblacionInicial, poblacionActual, fechaIngreso, fechaProyectadaSalida, estadoCiclo.
- **Galpón**: Espacio físico que alberga transitoriamente a un lote durante su ciclo productivo.
  - *Atributos*: ID, nombre, capacidad, loteActivoId, estadoOcupacion.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las consultas de requerimiento para el Módulo 3 entregan los cálculos exactos en kilogramos asociados al lote consultado basados en la población viva al iniciar cada etapa y la duración efectiva.
- **SC-002**: El 100 % de las transiciones de etapa de los lotes generan y persisten automáticamente su proyección de alimento en kilogramos y su costo unitario de referencia capturado en menos de 2 segundos desde la confirmación del cambio de etapa.
- **SC-003**: El tiempo de respuesta de la consulta por lote para el Módulo 3 es inferior a 1 segundo por petición.
- **SC-004**: El 100 % de las respuestas entregadas al Módulo 3 mantienen los requerimientos de alimento separados por etapa del lote e incluyen el costo unitario por kilogramo de referencia de cada insumo, con 0 % de sumatorias globales de kilogramos heterogéneos.
- **SC-005**: 0 % de registros de proyecciones previas son sobreescritos, eliminados o alterados cuando se agrega una nueva etapa al historial del lote o cuando un nuevo lote ingresa a un galpón previamente ocupado.
- **SC-006**: El 100 % de las consultas de balance de abastecimiento consolidado para el administrador calculan con exactitud el déficit de compra en bultos y kilogramos de los lotes activos en menos de 2 segundos.
- **SC-007**: 0 % de existencias físicas en bodega central son descontadas, bloqueadas o alteradas por la consulta o generación de las proyecciones consolidadas.
- **SC-008**: El 100 % de los ajustes sucesivos de duración en etapas activas se agregan al historial de auditoría de forma cronológica sin sobreescribir ni eliminar ajustes precedentes.
- **SC-009**: El 100 % de las variaciones de precio de compra posteriores en bodega central mantienen inmutable el costo unitario de referencia capturado al activar la etapa.
- **SC-010**: El 100 % de los intentos de alterar o prorrogar proyecciones de etapas en estado `COMPLETADA` son rechazados por el sistema.
- **SC-011**: 0 % de casos de sustitución de costo no disponible con valor cero (`0.00`), garantizando la asignación de `null` y advertencia descriptiva.

---

## Out of Scope *(Fuera de alcance de esta especificación)*

- **Cálculo contable final de liquidación del lote**: La multiplicación final de los kilogramos por costo unitario, la determinación del costo total del lote, balances económicos e impuestos corresponden a los procesos internos del Módulo 3 (Finanzas).
- **Cálculo del costo real del alimento consumido**: El costo efectivo del alimento consumido por el lote se calcula en el Módulo 3 mediante el registro de los consumos diarios reales valorados con el precio neto de compra histórico de cada recepción física ([SPEC-001](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/001-RegistrarRecepcionDeAlimento.md) FR-008).
- **Definición de regla de valoración contable de inventario**: La concertación del método contable (Promedio Ponderado, FIFO/PEPS o Última Compra) para determinar el costo de referencia unitario ante recepciones heterogéneas queda formalmente fuera del alcance de este documento como una Decisión de Negocio Pendiente.
- **Definición y ajuste de cuotas nutricionales**: La configuración de la ración diaria por ave (`kg/ave/día`) y su asociación por etapa corresponde al [SPEC-021](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/021-AjustarPlanNutricionalPorEtapa.md).
- **Personalización de calendarios y días de etapas**: La duración estándar de etapas y desplazamientos en cascada corresponde al [SPEC-021](file:///C:/Users/ESTUDIANTE/IdeaProjects/practicaweb/AviControlMod2/docs/specs/021-AjustarPlanNutricionalPorEtapa.md).
- **Gestión operativa veterinaria y cuarentenas**: La emisión, registro y diagnóstico de órdenes sanitarias corresponden al módulo de Sanidad.
- **Despachos y movimientos físicos de almacén**: El traslado físico de bultos y control de inventarios en bodegas corresponde a los Módulos 1 y 3 de Logística.
- **Emisión de órdenes de compra a proveedores**: La generación de órdenes de compra, cotizaciones y pagos corresponde al Módulo de Compras/Finanzas.
