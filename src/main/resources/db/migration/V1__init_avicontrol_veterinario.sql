1. SOBRE EL PROTOTIPO P-01:
Por favor, NO modifiques, NO corrijas y NO actualices bajo ninguna circunstancia el archivo `docs/prototype/Veterinario/P-01-login-selector-rol.html`. Si realizaste algún cambio en él, reviértelo de inmediato a su estado original (ejecuta `git checkout -- docs/prototype/Veterinario/P-01-login-selector-rol.html` o descarta los cambios). Los prototipos HTML son solo especificaciones visuales de lectura y deben permanecer 100% inalterados.

2. SOBRE CORS Y HEADERS MOCK:
Aprobado. Mantén la política de CORS limitada a `http://localhost:5173` y `http://localhost:3000`, y restringe los headers `X-Mock-Role` y `X-Mock-Actor-Id` exclusivamente a los perfiles de Spring `dev` y `test` (`@Profile({"dev", "test"})`).

3. SOBRE LA TABLA idempotency_record Y EL SQL DEFINITIVO:
La tabla `idempotency_record` SÍ debe existir: es la tabla técnica transversal requerida por General.md y los Planes 008/012 para garantizar la idempotencia de los endpoints POST/PUT con la cabecera X-Idempotency-Key. Además, se incluye la tabla técnica `event_publication` requerida por Spring Modulith para el Transactional Outbox.

Crea el archivo `src/main/resources/db/migration/V1__init_avicontrol_veterinario.sql` y pega en él exactamente el siguiente script SQL definitivo (el cual ya está sincronizado con nuestro Diagrama de Clases UML v3, el modelo ER y los planes técnicos):

```sql
-- ==========================================================================================================
-- AVICONTROL MÓDULO 2 (ACTOR VETERINARIO) - ESQUEMA DEFINITIVO POSTGRESQL / SUPABASE
-- 100% Alineado con Diagrama_Clases_ACTOR-VET_v3.png, ER_AviControl, General.md y Planes 005, 006, 008, 012, 013, 014, 015
-- ==========================================================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ----------------------------------------------------------------------------------------------------------
-- 1. FUNCIÓN DE SEGURIDAD E INMUTABILIDAD: PROHIBICIÓN ESTRICTA DE BORRADO FÍSICO (DELETE)
-- ----------------------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_prohibir_borrado_fisico()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'OPERACION_PROHIBIDA: El borrado físico (DELETE) está estrictamente prohibido en la tabla % por trazabilidad sanitaria.', TG_TABLE_NAME
        USING ERRCODE = 'P0001';
END;
$$ LANGUAGE plpgsql;

-- ----------------------------------------------------------------------------------------------------------
-- 2. TABLAS EXTERNAS DE CONTEXTO DEL ER (MÓDULO 1, INVENTARIO Y MORTALIDAD)
-- ----------------------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS galpon (
    id UUID PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    aforo_maximo INTEGER NOT NULL CHECK (aforo_maximo > 0),
    estado VARCHAR(30) NOT NULL CHECK (estado IN ('DISPONIBLE', 'VACIADO_SANITARIO', 'PRODUCTIVO', 'EN_COSECHA', 'MANTENIMIENTO', 'AISLAMIENTO')),
    version INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS lote (
    id UUID PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    poblacion_inicial INTEGER NOT NULL CHECK (poblacion_inicial > 0),
    poblacion_actual INTEGER NOT NULL CHECK (poblacion_actual >= 0 AND poblacion_actual <= poblacion_inicial),
    fecha_ingreso DATE NOT NULL,
    costo_total NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    galpon_id UUID NOT NULL REFERENCES galpon(id),
    alojado_actualmente BOOLEAN NOT NULL DEFAULT TRUE,
    version INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS bodega_central (
    id UUID PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    capacidad NUMERIC(14, 2) NOT NULL CHECK (capacidad > 0),
    unidad_capacidad VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS medicamento (
    id UUID PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL UNIQUE,
    principio_activo VARCHAR(150) NOT NULL,
    unidad_base VARCHAR(20) NOT NULL CHECK (unidad_base IN ('GRAMO', 'MILILITRO', 'UNIDAD')),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
);

CREATE TABLE IF NOT EXISTS recepcion_medicamento (
    id UUID PRIMARY KEY,
    bodega_id UUID NOT NULL REFERENCES bodega_central(id),
    medicamento_id UUID NOT NULL REFERENCES medicamento(id),
    codigo_lote VARCHAR(60) NOT NULL,
    presentacion VARCHAR(60) NOT NULL,
    cantidad_presentacion NUMERIC(12, 2) NOT NULL CHECK (cantidad_presentacion > 0),
    contenido_por_presentacion NUMERIC(12, 4) NOT NULL CHECK (contenido_por_presentacion > 0),
    unidad_medida VARCHAR(20) NOT NULL CHECK (unidad_medida IN ('MILIGRAMO', 'GRAMO', 'MILILITRO', 'UNIDAD')),
    precio_neto_presentacion NUMERIC(14, 2) NOT NULL CHECK (precio_neto_presentacion >= 0),
    precio_neto_unidad_base NUMERIC(14, 4) NOT NULL CHECK (precio_neto_unidad_base >= 0),
    saldo_disponible_unidad_base NUMERIC(14, 4) NOT NULL CHECK (saldo_disponible_unidad_base >= 0),
    fecha_ingreso DATE NOT NULL,
    fecha_vencimiento DATE NOT NULL CHECK (fecha_vencimiento > fecha_ingreso),
    estado VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADA' CHECK (estado IN ('CONFIRMADA', 'ANULADA', 'VENCIDA')),
    registrada_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS auditoria_recepcion_medicamento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recepcion_medicamento_id UUID NOT NULL REFERENCES recepcion_medicamento(id),
    actor_id UUID NOT NULL,
    accion VARCHAR(50) NOT NULL DEFAULT 'EDICION_RECEPCION',
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    valores_anteriores JSONB NOT NULL,
    valores_nuevos JSONB NOT NULL
);

CREATE TABLE IF NOT EXISTS movimiento_medicamento (
    id UUID PRIMARY KEY,
    recepcion_medicamento_id UUID NOT NULL REFERENCES recepcion_medicamento(id),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('ENTRADA', 'SALIDA', 'APLICACION', 'CONSUMO', 'VENCIMIENTO', 'ANULACION', 'AJUSTE')),
    estado VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADO',
    cantidad_unidad_base NUMERIC(14, 4) NOT NULL CHECK (cantidad_unidad_base > 0),
    ocurrido_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    motivo VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS registro_mortalidad (
    id UUID PRIMARY KEY,
    galpon_id UUID NOT NULL REFERENCES galpon(id),
    lote_id UUID NOT NULL REFERENCES lote(id),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('ORDINARIA', 'ANORMAL')),
    fecha_evento TIMESTAMPTZ NOT NULL,
    cantidad_muertes INTEGER NOT NULL CHECK (cantidad_muertes > 0),
    poblacion_viva_previa INTEGER NOT NULL CHECK (poblacion_viva_previa > 0),
    porcentaje_mortalidad NUMERIC(6, 2) NOT NULL CHECK (porcentaje_mortalidad >= 0),
    causa VARCHAR(150) NOT NULL,
    causa_probable VARCHAR(200),
    nivel_severidad VARCHAR(30) CHECK (nivel_severidad IN ('NORMAL', 'ALTA', 'CRITICA', 'EMERGENCIA_SANITARIA')),
    observaciones TEXT,
    reportado_por UUID NOT NULL
);

-- ----------------------------------------------------------------------------------------------------------
-- 3. LAS 8 TABLAS PROPIETARIAS DEL ACTOR VETERINARIO (100% ALINEADAS CON DIAGRAMA UML v3)
-- ----------------------------------------------------------------------------------------------------------

-- 3.1. PLAN 005 (SPEC 009): ENFERMEDAD
CREATE TABLE IF NOT EXISTS enfermedad (
    id UUID PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    nivel_riesgo VARCHAR(20) NOT NULL CHECK (nivel_riesgo IN ('BAJO', 'MEDIO', 'ALTO', 'CRITICO')),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('VIRAL', 'BACTERIANA', 'PARASITARIA', 'FUNGICA')),
    descripcion_clinica TEXT NOT NULL,
    requiere_sacrificio_sanitario BOOLEAN NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    idempotency_key VARCHAR(120) UNIQUE,
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT chk_enfermedad_nombre_no_vacio CHECK ( length(btrim(nombre)) > 0 ),
    CONSTRAINT chk_enfermedad_desc_no_vacia CHECK ( length(btrim(descripcion_clinica)) > 0 )
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_enfermedad_nombre_norm ON enfermedad (LOWER(BTRIM(nombre)));

DROP TRIGGER IF EXISTS trg_no_delete_enfermedad ON enfermedad;
CREATE TRIGGER trg_no_delete_enfermedad
BEFORE DELETE ON enfermedad FOR EACH ROW EXECUTE FUNCTION fn_prohibir_borrado_fisico();

-- 3.2. PLAN 006 (SPECS 008 Y 015): MEDICACION
CREATE TABLE IF NOT EXISTS medicacion (
    id UUID PRIMARY KEY,
    enfermedad_id UUID NOT NULL REFERENCES enfermedad(id),
    medicamento_id UUID NOT NULL REFERENCES medicamento(id),
    dosis VARCHAR(255) NOT NULL,
    dias_totales INTEGER NOT NULL CHECK (dias_totales > 0),
    via_administracion VARCHAR(150) NOT NULL DEFAULT 'Agua de bebida en bebederos automáticos',
    descripcion TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    idempotency_key VARCHAR(120) UNIQUE,
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT chk_medicacion_dosis_no_vacia CHECK ( length(btrim(dosis)) > 0 ),
    CONSTRAINT chk_medicacion_desc_no_vacia CHECK ( length(btrim(descripcion)) > 0 )
);
CREATE INDEX IF NOT EXISTS idx_medicacion_enfermedad ON medicacion(enfermedad_id);
CREATE INDEX IF NOT EXISTS idx_medicacion_medicamento ON medicacion(medicamento_id);

DROP TRIGGER IF EXISTS trg_no_delete_medicacion ON medicacion;
CREATE TRIGGER trg_no_delete_medicacion
BEFORE DELETE ON medicacion FOR EACH ROW EXECUTE FUNCTION fn_prohibir_borrado_fisico();

-- 3.3. PLAN 013 (SPEC 018): ALERTA_SANITARIA
CREATE TABLE IF NOT EXISTS alerta_sanitaria (
    id UUID PRIMARY KEY,
    registro_mortalidad_anormal_id UUID NOT NULL UNIQUE REFERENCES registro_mortalidad(id),
    galpon_id UUID NOT NULL REFERENCES galpon(id),
    lote_id UUID NOT NULL REFERENCES lote(id),
    prioridad VARCHAR(30) NOT NULL CHECK (prioridad IN ('ALTA', 'CRITICA', 'EMERGENCIA_SANITARIA')),
    estado VARCHAR(40) NOT NULL DEFAULT 'PENDIENTE_ATENCION_VETERINARIA' CHECK (estado IN ('PENDIENTE_ATENCION_VETERINARIA', 'ATENDIDA')),
    destinatario_rol VARCHAR(30) NOT NULL DEFAULT 'ROLE_VETERINARIO',
    fecha_hora_emision TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atendida_en TIMESTAMPTZ,
    observaciones_atencion TEXT
);
CREATE INDEX IF NOT EXISTS idx_alerta_estado_emision ON alerta_sanitaria(estado, fecha_hora_emision DESC);

DROP TRIGGER IF EXISTS trg_no_delete_alerta_sanitaria ON alerta_sanitaria;
CREATE TRIGGER trg_no_delete_alerta_sanitaria
BEFORE DELETE ON alerta_sanitaria FOR EACH ROW EXECUTE FUNCTION fn_prohibir_borrado_fisico();

-- 3.4. PLAN 014 (SPECS 020 Y 010): SOLICITUD_AISLAMIENTO + @Embedded DecisionAislamiento
CREATE TABLE IF NOT EXISTS solicitud_aislamiento (
    id UUID PRIMARY KEY,
    galpon_id UUID NOT NULL REFERENCES galpon(id),
    galpon_nombre_historico VARCHAR(100) NOT NULL,
    lote_id UUID NOT NULL REFERENCES lote(id),
    lote_nombre_historico VARCHAR(100) NOT NULL,
    poblacion_actual_reportada INTEGER NOT NULL CHECK (poblacion_actual_reportada > 0),
    edad_lote_dias INTEGER NOT NULL CHECK (edad_lote_dias >= 1),
    motivo TEXT NOT NULL,
    signos_observados TEXT NOT NULL,
    trabajador_id UUID NOT NULL,
    emitida_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_VALIDACION' CHECK (estado IN ('PENDIENTE_VALIDACION', 'APROBADA', 'RECHAZADA')),
    veterinario_id UUID,
    decidida_en TIMESTAMPTZ,
    resultado VARCHAR(30) CHECK (resultado IS NULL OR resultado IN ('APROBADA', 'RECHAZADA')),
    observaciones TEXT,
    idempotency_key VARCHAR(120) UNIQUE,
    idempotency_key_validacion VARCHAR(120) UNIQUE,
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT chk_solicitud_motivo_no_vacio CHECK ( length(btrim(motivo)) > 0 ),
    CONSTRAINT chk_solicitud_signos_no_vacios CHECK ( length(btrim(signos_observados)) > 0 )
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_solicitud_pendiente_galpon
    ON solicitud_aislamiento (galpon_id)
    WHERE estado = 'PENDIENTE_VALIDACION';

DROP TRIGGER IF EXISTS trg_no_delete_solicitud_aislamiento ON solicitud_aislamiento;
CREATE TRIGGER trg_no_delete_solicitud_aislamiento
BEFORE DELETE ON solicitud_aislamiento FOR EACH ROW EXECUTE FUNCTION fn_prohibir_borrado_fisico();

-- 3.5. PLAN 015 (SPEC 011): DIAGNOSTICO_GALPON
CREATE TABLE IF NOT EXISTS diagnostico_galpon (
    id UUID PRIMARY KEY,
    solicitud_aislamiento_id UUID REFERENCES solicitud_aislamiento(id),
    galpon_id UUID NOT NULL REFERENCES galpon(id),
    galpon_nombre_historico VARCHAR(100) NOT NULL,
    lote_id UUID NOT NULL REFERENCES lote(id),
    lote_nombre_historico VARCHAR(100) NOT NULL,
    enfermedad_id UUID NOT NULL REFERENCES enfermedad(id),
    nombre_enfermedad_historico VARCHAR(150) NOT NULL,
    requiere_sacrificio_historico BOOLEAN NOT NULL,
    medicacion_id UUID REFERENCES medicacion(id),
    medicamento_id_historico UUID REFERENCES medicamento(id),
    medicamento_nombre_historico VARCHAR(150),
    dosis_historica VARCHAR(255),
    via_administracion_historica VARCHAR(150),
    descripcion_historica TEXT,
    dias_medicacion INTEGER,
    fecha_diagnostico DATE NOT NULL,
    fecha_reintegro DATE,
    veterinario_id UUID NOT NULL,
    registrado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'CERRADO_POR_REINTEGRO', 'CERRADO_POR_SACRIFICIO')),
    idempotency_key VARCHAR(120) UNIQUE,
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT chk_bifurcacion_clinica_diagnostico CHECK (
        (requiere_sacrificio_historico = FALSE AND medicacion_id IS NOT NULL AND dias_medicacion > 0 AND fecha_reintegro IS NOT NULL)
        OR
        (requiere_sacrificio_historico = TRUE AND medicacion_id IS NULL AND dias_medicacion IS NULL AND fecha_reintegro IS NULL)
    )
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_diagnostico_activo_lote_enfermedad
    ON diagnostico_galpon (lote_id, enfermedad_id)
    WHERE estado = 'ACTIVO';
CREATE INDEX IF NOT EXISTS idx_diagnostico_reintegro_vencido
    ON diagnostico_galpon (estado, requiere_sacrificio_historico, fecha_reintegro);
CREATE INDEX IF NOT EXISTS idx_diagnostico_galpon_estado
    ON diagnostico_galpon (galpon_id, estado);

DROP TRIGGER IF EXISTS trg_no_delete_diagnostico_galpon ON diagnostico_galpon;
CREATE TRIGGER trg_no_delete_diagnostico_galpon
BEFORE DELETE ON diagnostico_galpon FOR EACH ROW EXECUTE FUNCTION fn_prohibir_borrado_fisico();

-- 3.6. PLAN 012 (SPEC 013): CONSUMO_MEDICAMENTO Y DETALLE_CONSUMO_MEDICAMENTO
CREATE TABLE IF NOT EXISTS consumo_medicamento (
    id UUID PRIMARY KEY,
    diagnostico_id UUID NOT NULL REFERENCES diagnostico_galpon(id),
    galpon_id UUID NOT NULL REFERENCES galpon(id),
    lote_id UUID NOT NULL REFERENCES lote(id),
    medicamento_id UUID NOT NULL REFERENCES medicamento(id),
    medicamento_nombre_historico VARCHAR(150) NOT NULL,
    cantidad_ingresada NUMERIC(12, 4) NOT NULL CHECK (cantidad_ingresada > 0),
    unidad_ingresada VARCHAR(20) NOT NULL CHECK (unidad_ingresada IN ('MILIGRAMO', 'GRAMO', 'MILILITRO', 'UNIDAD')),
    cantidad_normalizada NUMERIC(12, 4) NOT NULL CHECK (cantidad_normalizada > 0),
    unidad_base VARCHAR(20) NOT NULL CHECK (unidad_base IN ('GRAMO', 'MILILITRO', 'UNIDAD')),
    fecha_aplicacion DATE NOT NULL,
    registrado_por UUID NOT NULL,
    registrado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    idempotency_key VARCHAR(120) UNIQUE,
    version INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS detalle_consumo_medicamento (
    id UUID PRIMARY KEY,
    consumo_id UUID NOT NULL REFERENCES consumo_medicamento(id),
    recepcion_id UUID NOT NULL REFERENCES recepcion_medicamento(id),
    codigo_lote_recepcion VARCHAR(60) NOT NULL,
    cantidad_descontada_unidad_base NUMERIC(12, 4) NOT NULL CHECK (cantidad_descontada_unidad_base > 0),
    precio_neto_historico_unidad_base NUMERIC(14, 4) NOT NULL CHECK (precio_neto_historico_unidad_base >= 0),
    moneda VARCHAR(10) NOT NULL DEFAULT 'COP'
);
CREATE INDEX IF NOT EXISTS idx_consumo_diagnostico ON consumo_medicamento(diagnostico_id);
CREATE INDEX IF NOT EXISTS idx_detalle_consumo_id ON detalle_consumo_medicamento(consumo_id);

DROP TRIGGER IF EXISTS trg_no_delete_consumo_medicamento ON consumo_medicamento;
CREATE TRIGGER trg_no_delete_consumo_medicamento
BEFORE DELETE ON consumo_medicamento FOR EACH ROW EXECUTE FUNCTION fn_prohibir_borrado_fisico();

DROP TRIGGER IF EXISTS trg_no_delete_detalle_consumo ON detalle_consumo_medicamento;
CREATE TRIGGER trg_no_delete_detalle_consumo
BEFORE DELETE ON detalle_consumo_medicamento FOR EACH ROW EXECUTE FUNCTION fn_prohibir_borrado_fisico();

-- 3.7. PLAN 008 (SPEC 012): ORDEN_SACRIFICIO_SANITARIO
CREATE TABLE IF NOT EXISTS orden_sacrificio_sanitario (
    id UUID PRIMARY KEY,
    diagnostico_id UUID NOT NULL REFERENCES diagnostico_galpon(id),
    galpon_id UUID NOT NULL REFERENCES galpon(id),
    lote_id UUID NOT NULL REFERENCES lote(id),
    enfermedad_nombre_historico VARCHAR(150) NOT NULL,
    alcance_total BOOLEAN NOT NULL DEFAULT TRUE CHECK (alcance_total = TRUE),
    poblacion_afectada_momento INTEGER NOT NULL CHECK (poblacion_afectada_momento > 0),
    poblacion_sacrificada INTEGER CHECK (poblacion_sacrificada IS NULL OR poblacion_sacrificada > 0),
    motivo_clinico TEXT NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_EJECUCION' CHECK (estado IN ('PENDIENTE_EJECUCION', 'EJECUTADA')),
    emitida_por UUID NOT NULL,
    emitida_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ejecutada_por UUID,
    ejecutada_en TIMESTAMPTZ,
    observaciones_ejecucion TEXT,
    idempotency_key VARCHAR(120) UNIQUE,
    idempotency_key_ejecucion VARCHAR(120) UNIQUE,
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT chk_orden_motivo_no_vacio CHECK ( length(btrim(motivo_clinico)) > 0 )
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_orden_sacrificio_pendiente_galpon
    ON orden_sacrificio_sanitario (galpon_id)
    WHERE estado = 'PENDIENTE_EJECUCION';

DROP TRIGGER IF EXISTS trg_no_delete_orden_sacrificio ON orden_sacrificio_sanitario;
CREATE TRIGGER trg_no_delete_orden_sacrificio
BEFORE DELETE ON orden_sacrificio_sanitario FOR EACH ROW EXECUTE FUNCTION fn_prohibir_borrado_fisico();

-- 3.8. TABLAS TRANSVERSALES: IDEMPOTENCIA HTTP Y OUTBOX DE SPRING MODULITH
CREATE TABLE IF NOT EXISTS idempotency_record (
    idempotency_key VARCHAR(120) PRIMARY KEY,
    http_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    response_status INTEGER NOT NULL,
    response_body JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS event_publication (
    id UUID NOT NULL PRIMARY KEY,
    listener_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMPTZ NOT NULL,
    completion_date TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS event_publication_by_completion_date_idx ON event_publication (completion_date);

-- 3.9. VISTA OPTIMIZADA PARA PLAN 006 (SPEC 015): CONSULTA DIARIA Y RESUMEN DE MEDICACIÓN
CREATE OR REPLACE VIEW vw_medicacion_diaria_activa AS
SELECT
    d.id AS diagnostico_id,
    d.galpon_id,
    g.nombre AS galpon_nombre,
    g.aforo_maximo,
    g.estado AS estado_galpon,
    d.lote_id,
    l.nombre AS lote_nombre,
    l.poblacion_actual,
    l.fecha_ingreso,
    d.nombre_enfermedad_historico AS enfermedad,
    d.medicamento_nombre_historico AS medicamento,
    d.dosis_historica AS dosis_hoy,
    COALESCE(d.via_administracion_historica, d.descripcion_historica) AS via_administracion,
    d.fecha_diagnostico,
    d.dias_medicacion AS dias_totales,
    d.fecha_reintegro
FROM diagnostico_galpon d
JOIN galpon g ON g.id = d.galpon_id
JOIN lote l ON l.id = d.lote_id AND l.alojado_actualmente = TRUE
WHERE d.estado = 'ACTIVO'
  AND d.requiere_sacrificio_historico = FALSE;

-- ==========================================================================================================
-- 4. PROCEDIMIENTOS Y FUNCIONES ALMACENADAS EN POSTGRESQL (INVOCABLES DESDE SPRING BOOT)
-- ==========================================================================================================

-- 4.1. Procedimiento Almacenado para Validar o Rechazar Aislamiento Atómicamente (Plan 014 / Spec 010)
CREATE OR REPLACE FUNCTION sp_validar_aislamiento_galpon(
    p_solicitud_id UUID,
    p_veterinario_id UUID,
    p_resultado VARCHAR,
    p_observaciones TEXT,
    p_version_esperada INTEGER
) RETURNS TABLE (
    solicitud_id UUID,
    galpon_id UUID,
    estado_solicitud VARCHAR,
    estado_resultante_galpon VARCHAR,
    habilita_diagnostico BOOLEAN
) AS $$
DECLARE
    v_galpon_id UUID;
    v_estado_solicitud VARCHAR;
    v_estado_galpon VARCHAR;
    v_version_actual INTEGER;
BEGIN
    SELECT s.galpon_id, s.estado, s.version
      INTO v_galpon_id, v_estado_solicitud, v_version_actual
      FROM solicitud_aislamiento s
     WHERE s.id = p_solicitud_id
     FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'SOLICITUD_NO_ENCONTRADA' USING ERRCODE = 'P0002';
    END IF;

    IF v_estado_solicitud <> 'PENDIENTE_VALIDACION' THEN
        RAISE EXCEPTION 'SOLICITUD_YA_RESUELTA' USING ERRCODE = 'P0003';
    END IF;

    IF v_version_actual <> p_version_esperada THEN
        RAISE EXCEPTION 'CONCURRENCIA_OPTIMISTA' USING ERRCODE = 'P0004';
    END IF;

    SELECT g.estado INTO v_estado_galpon FROM galpon g WHERE g.id = v_galpon_id FOR UPDATE;

    IF p_resultado = 'APROBADA' THEN
        IF v_estado_galpon <> 'PRODUCTIVO' THEN
            RAISE EXCEPTION 'ESTADO_GALPON_INCOMPATIBLE: El galpón debe estar en PRODUCTIVO (actual: %)', v_estado_galpon USING ERRCODE = 'P0005';
        END IF;

        UPDATE galpon SET estado = 'AISLAMIENTO', version = version + 1 WHERE id = v_galpon_id;
        v_estado_galpon := 'AISLAMIENTO';
    END IF;

    UPDATE solicitud_aislamiento
       SET estado = p_resultado,
           veterinario_id = p_veterinario_id,
           decidida_en = NOW(),
           resultado = p_resultado,
           observaciones = btrim(p_observaciones),
           version = version + 1
     WHERE id = p_solicitud_id;

    RETURN QUERY SELECT p_solicitud_id, v_galpon_id, p_resultado, v_estado_galpon, (p_resultado = 'APROBADA');
END;
$$ LANGUAGE plpgsql;

-- 4.2. Función Almacenada para Descuento Atómico FEFO de Medicamentos (Plan 012 / Spec 013)
CREATE OR REPLACE FUNCTION fn_descontar_medicamento_fefo(
    p_medicamento_id UUID,
    p_cantidad_requerida_base NUMERIC(12, 4),
    p_fecha_actual DATE
) RETURNS TABLE (
    recepcion_id UUID,
    codigo_lote VARCHAR,
    cantidad_descontada NUMERIC(12, 4),
    precio_neto_historico NUMERIC(14, 4)
) AS $$
DECLARE
    v_total_disponible NUMERIC(14, 4);
    v_restante NUMERIC(12, 4) := p_cantidad_requerida_base;
    v_descuento NUMERIC(12, 4);
    r RECORD;
BEGIN
    SELECT COALESCE(SUM(rm.saldo_disponible_unidad_base), 0)
      INTO v_total_disponible
      FROM recepcion_medicamento rm
     WHERE rm.medicamento_id = p_medicamento_id
       AND rm.estado = 'CONFIRMADA'
       AND rm.fecha_vencimiento > p_fecha_actual
       AND rm.saldo_disponible_unidad_base > 0;

    IF v_total_disponible < p_cantidad_requerida_base THEN
        RAISE EXCEPTION 'EXISTENCIAS_INSUFICIENTES: Stock disponible (%) menor al requerido (%)', v_total_disponible, p_cantidad_requerida_base
            USING ERRCODE = 'P0006';
    END IF;

    FOR r IN
        SELECT rm.id, rm.codigo_lote, rm.saldo_disponible_unidad_base, rm.precio_neto_unidad_base
          FROM recepcion_medicamento rm
         WHERE rm.medicamento_id = p_medicamento_id
           AND rm.estado = 'CONFIRMADA'
           AND rm.fecha_vencimiento > p_fecha_actual
           AND rm.saldo_disponible_unidad_base > 0
         ORDER BY rm.fecha_vencimiento ASC, rm.fecha_ingreso ASC
         FOR UPDATE
    LOOP
        EXIT WHEN v_restante <= 0;

        v_descuento := LEAST(v_restante, r.saldo_disponible_unidad_base);

        UPDATE recepcion_medicamento
           SET saldo_disponible_unidad_base = saldo_disponible_unidad_base - v_descuento,
               version = version + 1
         WHERE id = r.id;

        INSERT INTO movimiento_medicamento (id, recepcion_medicamento_id, tipo, estado, cantidad_unidad_base, ocurrido_en, motivo)
        VALUES (gen_random_uuid(), r.id, 'CONSUMO', 'CONFIRMADO', v_descuento, NOW(), 'Consumo veterinario FEFO (Spec 013)');

        v_restante := v_restante - v_descuento;

        recepcion_id := r.id;
        codigo_lote := r.codigo_lote;
        cantidad_descontada := v_descuento;
        precio_neto_historico := r.precio_neto_unidad_base;
        RETURN NEXT;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- 4.3. Función Almacenada para Confirmar Ejecución Física de Sacrificio Sanitario Total (Plan 008 / Spec 012)
CREATE OR REPLACE FUNCTION sp_confirmar_sacrificio_sanitario(
    p_orden_id UUID,
    p_veterinario_id UUID,
    p_observaciones TEXT,
    p_version_esperada INTEGER
) RETURNS INTEGER AS $$
DECLARE
    v_galpon_id UUID;
    v_lote_id UUID;
    v_diagnostico_id UUID;
    v_estado_orden VARCHAR;
    v_version_orden INTEGER;
    v_estado_galpon VARCHAR;
    v_poblacion_viva INTEGER;
BEGIN
    SELECT o.galpon_id, o.lote_id, o.diagnostico_id, o.estado, o.version
      INTO v_galpon_id, v_lote_id, v_diagnostico_id, v_estado_orden, v_version_orden
      FROM orden_sacrificio_sanitario o
     WHERE o.id = p_orden_id
     FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'ORDEN_NO_ENCONTRADA' USING ERRCODE = 'P0007';
    END IF;

    IF v_estado_orden = 'EJECUTADA' THEN
        RAISE EXCEPTION 'ORDEN_YA_EJECUTADA' USING ERRCODE = 'P0008';
    END IF;

    IF v_version_orden <> p_version_esperada THEN
        RAISE EXCEPTION 'CONCURRENCIA_OPTIMISTA' USING ERRCODE = 'P0004';
    END IF;

    SELECT g.estado INTO v_estado_galpon FROM galpon g WHERE g.id = v_galpon_id FOR UPDATE;
    IF v_estado_galpon <> 'AISLAMIENTO' THEN
        RAISE EXCEPTION 'GALPON_NO_ESTA_EN_AISLAMIENTO' USING ERRCODE = 'P0009';
    END IF;

    SELECT l.poblacion_actual INTO v_poblacion_viva FROM lote l WHERE l.id = v_lote_id FOR UPDATE;
    IF v_poblacion_viva <= 0 THEN
        RAISE EXCEPTION 'LOTE_SIN_POBLACION_VIVA' USING ERRCODE = 'P0010';
    END IF;

    -- 1. Poner población viva en exactamente 0 (FR-008)
    UPDATE lote SET poblacion_actual = 0, version = version + 1 WHERE id = v_lote_id;

    -- 2. Cambiar galpón de AISLAMIENTO a VACIADO_SANITARIO (FR-008)
    UPDATE galpon SET estado = 'VACIADO_SANITARIO', version = version + 1 WHERE id = v_galpon_id;

    -- 3. Cerrar el diagnóstico en Plan 015 como CERRADO_POR_SACRIFICIO
    UPDATE diagnostico_galpon SET estado = 'CERRADO_POR_SACRIFICIO', version = version + 1 WHERE id = v_diagnostico_id;

    -- 4. Marcar la orden como EJECUTADA
    UPDATE orden_sacrificio_sanitario
       SET estado = 'EJECUTADA',
           poblacion_sacrificada = v_poblacion_viva,
           ejecutada_por = p_veterinario_id,
           ejecutada_en = NOW(),
           observaciones_ejecucion = p_observaciones,
           version = version + 1
     WHERE id = p_orden_id;

    RETURN v_poblacion_viva;
END;
$$ LANGUAGE plpgsql;

-- ==========================================================================================================
-- 5. DATOS SEMILLA IDEMPOTENTES (PARA TODAS LAS PANTALLAS P-01 A P-07 Y M-01 A M-07)
-- ==========================================================================================================

INSERT INTO bodega_central (id, nombre, capacidad, unidad_capacidad)
VALUES ('00000000-0000-0000-0000-000000000001', 'Bodega Central AviControl', 50000.00, 'KILOGRAMO')
ON CONFLICT (id) DO NOTHING;

INSERT INTO galpon (id, nombre, aforo_maximo, estado, version) VALUES
('550e8400-e29b-41d4-a716-446655440001', 'Galpón 1', 8000, 'PRODUCTIVO', 1),
('550e8400-e29b-41d4-a716-446655440002', 'Galpón 2', 6000, 'PRODUCTIVO', 1),
('550e8400-e29b-41d4-a716-446655440003', 'Galpón 3', 5000, 'AISLAMIENTO', 1),
('550e8400-e29b-41d4-a716-446655440004', 'Galpón 4', 7500, 'PRODUCTIVO', 1),
('550e8400-e29b-41d4-a716-446655440005', 'Galpón 5', 8000, 'AISLAMIENTO', 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO lote (id, nombre, poblacion_inicial, poblacion_actual, fecha_ingreso, costo_total, galpon_id, alojado_actualmente, version) VALUES
('2fc03a21-84c4-4c83-bb15-6607bb75cb01', 'LDP-001', 8000, 7920, CURRENT_DATE - INTERVAL '20 days', 0.00, '550e8400-e29b-41d4-a716-446655440001', TRUE, 1),
('2fc03a21-84c4-4c83-bb15-6607bb75cb02', 'LDP-002', 6000, 5950, CURRENT_DATE - INTERVAL '11 days', 0.00, '550e8400-e29b-41d4-a716-446655440002', TRUE, 1),
('3ab04b32-95d5-5d94-cc26-7718cc86dc03', 'LDP-003', 4000, 3500, CURRENT_DATE - INTERVAL '25 days', 0.00, '550e8400-e29b-41d4-a716-446655440003', TRUE, 1),
('2fc03a21-84c4-4c83-bb15-6607bb75cb04', 'LDP-004', 7500, 7380, CURRENT_DATE - INTERVAL '18 days', 0.00, '550e8400-e29b-41d4-a716-446655440004', TRUE, 1),
('2fc03a21-84c4-4c83-bb15-6607bb75cb90', 'LDP-005', 8000, 7960, CURRENT_DATE - INTERVAL '16 days', 0.00, '550e8400-e29b-41d4-a716-446655440005', TRUE, 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO medicamento (id, nombre, principio_activo, unidad_base, estado) VALUES
('99999999-8888-7777-6666-555555555555', 'Amprolio 20% Solución', 'Amprolio HCl', 'GRAMO', 'ACTIVO'),
('99999999-8888-7777-6666-555555555556', 'Tilosina Tartrato 50%', 'Tilosina', 'GRAMO', 'ACTIVO'),
('99999999-8888-7777-6666-555555555557', 'Enrofloxacina 10% Oral', 'Enrofloxacina', 'MILILITRO', 'ACTIVO')
ON CONFLICT (id) DO NOTHING;

INSERT INTO recepcion_medicamento (id, bodega_id, medicamento_id, codigo_lote, presentacion, cantidad_presentacion, contenido_por_presentacion, unidad_medida, precio_neto_presentacion, precio_neto_unidad_base, saldo_disponible_unidad_base, fecha_ingreso, fecha_vencimiento, estado) VALUES
('aaaa1111-2222-3333-4444-555555555501', '00000000-0000-0000-0000-000000000001', '99999999-8888-7777-6666-555555555555', 'MED-2026-01', 'SOBRE', 10, 150, 'GRAMO', 1875.00, 12.5000, 150.0000, CURRENT_DATE - INTERVAL '30 days', CURRENT_DATE + INTERVAL '90 days', 'CONFIRMADA'),
('aaaa1111-2222-3333-4444-555555555502', '00000000-0000-0000-0000-000000000001', '99999999-8888-7777-6666-555555555555', 'MED-2026-02', 'FRASCO', 20, 500, 'GRAMO', 7000.00, 14.0000, 1000.0000, CURRENT_DATE - INTERVAL '10 days', CURRENT_DATE + INTERVAL '180 days', 'CONFIRMADA')
ON CONFLICT (id) DO NOTHING;

INSERT INTO enfermedad (id, nombre, nivel_riesgo, tipo, descripcion_clinica, requiere_sacrificio_sanitario, estado, version) VALUES
('11111111-2222-3333-4444-555555555555', 'Coccidiosis Aviar', 'MEDIO', 'PARASITARIA', 'Infección parasitaria intestinal con diarrea sanguinolenta y baja conversión.', FALSE, 'ACTIVA', 1),
('22222222-3333-4444-5555-666666666666', 'Influenza Aviar', 'CRITICO', 'VIRAL', 'Enfermedad viral altamente patógena de erradicación obligatoria.', TRUE, 'ACTIVA', 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO medicacion (id, enfermedad_id, medicamento_id, dosis, dias_totales, via_administracion, descripcion, estado, version) VALUES
('66666666-7777-8888-9999-000000000000', '11111111-2222-3333-4444-555555555555', '99999999-8888-7777-6666-555555555555', '200 g diluidos en 100 L de agua potable', 5, 'Agua de bebida en bebederos automáticos', 'Administrar continuamente en el agua de bebida durante 5 días, renovando cada 24 horas.', 'ACTIVA', 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO registro_mortalidad (id, galpon_id, lote_id, tipo, fecha_evento, cantidad_muertes, poblacion_viva_previa, porcentaje_mortalidad, causa, causa_probable, nivel_severidad, observaciones, reportado_por) VALUES
('bbbb1111-2222-3333-4444-555555555501', '550e8400-e29b-41d4-a716-446655440001', '2fc03a21-84c4-4c83-bb15-6607bb75cb01', 'ANORMAL', NOW() - INTERVAL '2 hours', 32, 7952, 0.40, 'MORTALIDAD_ANORMAL', 'Problemas digestivos / entéricos agudos', 'ALTA', 'Bajas concentradas cerca de bebederos del sector norte en recorrido matutino.', 'c1d2e3f4-a5b6-7c8d-9e0f-1a2b3c4d5e6f')
ON CONFLICT (id) DO NOTHING;

INSERT INTO alerta_sanitaria (id, registro_mortalidad_anormal_id, galpon_id, lote_id, prioridad, estado, destinatario_rol, fecha_hora_emision) VALUES
('cccc1111-2222-3333-4444-555555555501', 'bbbb1111-2222-3333-4444-555555555501', '550e8400-e29b-41d4-a716-446655440001', '2fc03a21-84c4-4c83-bb15-6607bb75cb01', 'ALTA', 'PENDIENTE_ATENCION_VETERINARIA', 'ROLE_VETERINARIO', NOW() - INTERVAL '2 hours')
ON CONFLICT (id) DO NOTHING;

INSERT INTO solicitud_aislamiento (id, galpon_id, galpon_nombre_historico, lote_id, lote_nombre_historico, poblacion_actual_reportada, edad_lote_dias, motivo, signos_observados, trabajador_id, emitida_en, estado, version) VALUES
('7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d', '550e8400-e29b-41d4-a716-446655440001', 'Galpón 1', '2fc03a21-84c4-4c83-bb15-6607bb75cb01', 'LDP-001', 7920, 21, 'Sospecha de cuadro respiratorio agudo en el sector norte del galpón', 'Estertores traqueales, decaimiento general, plumas erizadas y disminución en el consumo de agua', 'c1d2e3f4-a5b6-7c8d-9e0f-1a2b3c4d5e6f', NOW() - INTERVAL '1 hour', 'PENDIENTE_VALIDACION', 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO diagnostico_galpon (id, galpon_id, galpon_nombre_historico, lote_id, lote_nombre_historico, enfermedad_id, nombre_enfermedad_historico, requiere_sacrificio_historico, medicacion_id, medicamento_id_historico, medicamento_nombre_historico, dosis_historica, via_administracion_historica, descripcion_historica, dias_medicacion, fecha_diagnostico, fecha_reintegro, veterinario_id, registrado_en, estado, version) VALUES
('d1a2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', '550e8400-e29b-41d4-a716-446655440005', 'Galpón 5', '2fc03a21-84c4-4c83-bb15-6607bb75cb90', 'LDP-005', '11111111-2222-3333-4444-555555555555', 'Coccidiosis Aviar', FALSE, '66666666-7777-8888-9999-000000000000', '99999999-8888-7777-6666-555555555555', 'Amprolio 20% Solución', '200 g diluidos en 100 L de agua potable', 'Agua de bebida en bebederos automáticos', 'Administrar continuamente en el agua de bebida durante 5 días, renovando cada 24 horas.', 5, CURRENT_DATE - INTERVAL '1 day', CURRENT_DATE + INTERVAL '4 days', 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', NOW() - INTERVAL '1 day', 'ACTIVO', 1),
('b1a2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', '550e8400-e29b-41d4-a716-446655440003', 'Galpón 3', '3ab04b32-95d5-5d94-cc26-7718cc86dc03', 'LDP-003', '22222222-3333-4444-5555-666666666666', 'Influenza Aviar', TRUE, NULL, NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_DATE, NULL, 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', NOW() - INTERVAL '30 minutes', 'ACTIVO', 1)
ON CONFLICT (id) DO NOTHING;