CREATE TABLE suministros_diarios_alimento (
  id UUID PRIMARY KEY,
  galpon_id UUID NOT NULL,
  lote_id UUID NOT NULL,
  alimento_id UUID NOT NULL,
  tipo_alimento VARCHAR(20) NOT NULL,
  cantidad_kg NUMERIC(18, 4) NOT NULL,
  fecha_aplicacion DATE NOT NULL,
  fecha_registro TIMESTAMP WITH TIME ZONE NOT NULL,
  trabajador VARCHAR(100) NOT NULL,
  idempotencia_clave UUID NOT NULL,
  UNIQUE (galpon_id, idempotencia_clave),
  CHECK (cantidad_kg > 0),
  CHECK (tipo_alimento IN ('PRE_INICIO', 'INICIO', 'ENGORDE'))
);

CREATE TABLE detalles_suministro_alimento (
  suministro_id UUID NOT NULL REFERENCES suministros_diarios_alimento(id),
  recepcion_id UUID NOT NULL REFERENCES recepciones_alimento(id),
  cantidad_kg NUMERIC(18, 4) NOT NULL,
  precio_neto_kg NUMERIC(18, 4) NOT NULL,
  moneda VARCHAR(3) NOT NULL DEFAULT 'COP',
  PRIMARY KEY (suministro_id, recepcion_id),
  CHECK (cantidad_kg > 0),
  CHECK (precio_neto_kg >= 0)
);

CREATE INDEX idx_suministros_lote_alimento_fecha
  ON suministros_diarios_alimento (lote_id, alimento_id, fecha_aplicacion);
CREATE INDEX idx_detalles_suministro_recepcion
  ON detalles_suministro_alimento (recepcion_id);
