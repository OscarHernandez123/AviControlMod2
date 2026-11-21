CREATE TABLE medicaciones (
  id UUID PRIMARY KEY,
  enfermedad_id UUID NOT NULL REFERENCES enfermedades(id),
  medicamento_id UUID NOT NULL,
  dosis VARCHAR(300) NOT NULL,
  duracion_dias INTEGER NOT NULL,
  descripcion VARCHAR(2000) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL,
  fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL,
  CHECK (LENGTH(TRIM(dosis)) > 0),
  CHECK (duracion_dias > 0),
  CHECK (LENGTH(TRIM(descripcion)) > 0)
);

CREATE INDEX idx_medicaciones_enfermedad ON medicaciones (enfermedad_id);
CREATE INDEX idx_medicaciones_medicamento ON medicaciones (medicamento_id);
