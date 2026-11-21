CREATE TABLE ordenes_sacrificio_productivo (
  id UUID PRIMARY KEY,
  galpon_id UUID NOT NULL,
  lote_id UUID NOT NULL,
  estado VARCHAR(20) NOT NULL,
  fecha_hora_programada TIMESTAMP WITH TIME ZONE NOT NULL,
  fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL,
  fecha_reprogramacion TIMESTAMP WITH TIME ZONE,
  fecha_cancelacion TIMESTAMP WITH TIME ZONE,
  fecha_ejecucion TIMESTAMP WITH TIME ZONE,
  version BIGINT NOT NULL DEFAULT 0,
  UNIQUE (galpon_id, lote_id),
  CHECK (estado IN ('PROGRAMADA', 'REPROGRAMADA', 'CANCELADA', 'EJECUTADA')),
  CHECK (version >= 0)
);

CREATE INDEX idx_ordenes_sacrificio_vencidas
  ON ordenes_sacrificio_productivo (estado, fecha_hora_programada);
