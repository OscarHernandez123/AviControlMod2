CREATE TABLE modulo1_galpon_local (
  id UUID PRIMARY KEY,
  lote_id UUID NOT NULL,
  estado VARCHAR(30) NOT NULL,
  poblacion_viva INTEGER NOT NULL,
  fecha_ingreso DATE NOT NULL,
  CHECK (poblacion_viva >= 0)
);

CREATE TABLE solicitudes_aislamiento (
  id UUID PRIMARY KEY,
  galpon_id UUID NOT NULL,
  pending_galpon_id UUID,
  lote_id UUID NOT NULL,
  motivo VARCHAR(2000) NOT NULL,
  signos_clinicos VARCHAR(2000) NOT NULL,
  poblacion_viva_reportada INTEGER NOT NULL,
  edad_dias_reportada INTEGER NOT NULL,
  trabajador VARCHAR(100) NOT NULL,
  fecha_emision TIMESTAMP WITH TIME ZONE NOT NULL,
  estado VARCHAR(30) NOT NULL,
  observaciones VARCHAR(2000),
  veterinario VARCHAR(100),
  fecha_decision TIMESTAMP WITH TIME ZONE,
  version BIGINT NOT NULL DEFAULT 0,
  CHECK (LENGTH(TRIM(motivo)) > 0),
  CHECK (LENGTH(TRIM(signos_clinicos)) > 0),
  CHECK (poblacion_viva_reportada > 0),
  CHECK (edad_dias_reportada >= 1),
  CHECK (estado IN ('PENDIENTE_VALIDACION', 'ACEPTADA', 'RECHAZADA')),
  CHECK ((estado = 'PENDIENTE_VALIDACION' AND pending_galpon_id IS NOT NULL
      AND pending_galpon_id = galpon_id)
      OR (estado <> 'PENDIENTE_VALIDACION' AND pending_galpon_id IS NULL))
);

CREATE UNIQUE INDEX uq_solicitud_aislamiento_pendiente
  ON solicitudes_aislamiento (pending_galpon_id);
CREATE INDEX idx_solicitudes_aislamiento_estado_fecha
  ON solicitudes_aislamiento (estado, fecha_emision);
