CREATE TABLE diagnosticos_galpon (
  id UUID PRIMARY KEY,
  galpon_id UUID NOT NULL,
  active_galpon_id UUID,
  lote_id UUID NOT NULL,
  enfermedad_id UUID NOT NULL REFERENCES enfermedades(id),
  enfermedad_nombre VARCHAR(150) NOT NULL,
  requiere_sacrificio_sanitario BOOLEAN NOT NULL,
  medicacion_id UUID REFERENCES medicaciones(id),
  medicamento_id UUID,
  dosis VARCHAR(300),
  duracion_dias INTEGER,
  descripcion_medicacion VARCHAR(2000),
  observaciones VARCHAR(2000) NOT NULL,
  fecha_diagnostico DATE NOT NULL,
  fecha_reintegro DATE,
  estado VARCHAR(30) NOT NULL,
  fecha_resolucion DATE,
  version BIGINT NOT NULL DEFAULT 0,
  CHECK (LENGTH(TRIM(observaciones)) > 0),
  CHECK (estado IN ('ACTIVO', 'REINTEGRADO', 'REINTEGRO_OMITIDO')),
  CHECK ((estado = 'ACTIVO' AND fecha_resolucion IS NULL)
      OR (estado <> 'ACTIVO' AND fecha_resolucion IS NOT NULL)),
  CHECK ((estado = 'ACTIVO' AND active_galpon_id IS NOT NULL AND active_galpon_id = galpon_id)
      OR (estado <> 'ACTIVO' AND active_galpon_id IS NULL)),
  CHECK ((requiere_sacrificio_sanitario = TRUE AND medicacion_id IS NULL
      AND medicamento_id IS NULL AND dosis IS NULL AND duracion_dias IS NULL
      AND descripcion_medicacion IS NULL
      AND fecha_reintegro IS NULL)
      OR (requiere_sacrificio_sanitario = FALSE AND medicacion_id IS NOT NULL
      AND medicamento_id IS NOT NULL AND dosis IS NOT NULL AND LENGTH(TRIM(dosis)) > 0
      AND duracion_dias IS NOT NULL AND duracion_dias > 0
      AND descripcion_medicacion IS NOT NULL AND LENGTH(TRIM(descripcion_medicacion)) > 0
      AND fecha_reintegro IS NOT NULL))
);

CREATE UNIQUE INDEX uq_diagnostico_activo_galpon ON diagnosticos_galpon (active_galpon_id);
CREATE INDEX idx_diagnosticos_reintegro ON diagnosticos_galpon (estado, fecha_reintegro);
CREATE INDEX idx_diagnosticos_lote ON diagnosticos_galpon (lote_id);
