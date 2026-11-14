CREATE TABLE ordenes_sacrificio_sanitario (
  id UUID PRIMARY KEY,
  diagnostico_id UUID NOT NULL UNIQUE REFERENCES diagnosticos_galpon(id),
  galpon_id UUID NOT NULL,
  lote_id UUID NOT NULL,
  estado VARCHAR(20) NOT NULL,
  poblacion_sacrificada INTEGER,
  fecha_emision TIMESTAMP WITH TIME ZONE NOT NULL,
  fecha_ejecucion TIMESTAMP WITH TIME ZONE,
  version BIGINT NOT NULL DEFAULT 0,
  CHECK (estado IN ('PENDIENTE', 'EJECUTADA')),
  CHECK (version >= 0),
  CHECK ((estado = 'PENDIENTE' AND poblacion_sacrificada IS NULL AND fecha_ejecucion IS NULL)
    OR (estado = 'EJECUTADA' AND poblacion_sacrificada > 0 AND fecha_ejecucion IS NOT NULL))
);
