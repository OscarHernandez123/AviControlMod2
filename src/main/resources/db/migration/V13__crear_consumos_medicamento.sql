CREATE TABLE consumos_medicamento (
  id UUID PRIMARY KEY,
  diagnostico_id UUID NOT NULL REFERENCES diagnosticos_galpon(id),
  galpon_id UUID NOT NULL,
  lote_id UUID NOT NULL,
  medicamento_id UUID NOT NULL,
  cantidad_ingresada NUMERIC(18, 4) NOT NULL,
  unidad_ingresada VARCHAR(20) NOT NULL,
  cantidad_unidad_base NUMERIC(18, 4) NOT NULL,
  unidad_base VARCHAR(20) NOT NULL,
  fecha_aplicacion DATE NOT NULL,
  fecha_registro TIMESTAMP WITH TIME ZONE NOT NULL,
  actor VARCHAR(100) NOT NULL,
  idempotencia_clave UUID NOT NULL,
  CHECK (cantidad_ingresada > 0),
  CHECK (cantidad_unidad_base > 0),
  CHECK (unidad_ingresada IN ('MILIGRAMO', 'GRAMO', 'MILILITRO', 'UNIDAD')),
  CHECK (unidad_base IN ('GRAMO', 'MILILITRO', 'UNIDAD')),
  UNIQUE (diagnostico_id, idempotencia_clave)
);

CREATE TABLE detalles_consumo_medicamento (
  consumo_id UUID NOT NULL REFERENCES consumos_medicamento(id),
  recepcion_id UUID NOT NULL REFERENCES recepciones_medicamento(id),
  cantidad_unidad_base NUMERIC(18, 4) NOT NULL,
  precio_neto_historico_unidad_base NUMERIC(18, 4) NOT NULL,
  moneda VARCHAR(3) NOT NULL,
  PRIMARY KEY (consumo_id, recepcion_id),
  CHECK (cantidad_unidad_base > 0),
  CHECK (precio_neto_historico_unidad_base >= 0),
  CHECK (moneda = 'COP')
);

CREATE INDEX idx_detalles_consumo_recepcion ON detalles_consumo_medicamento (recepcion_id);
CREATE INDEX idx_consumos_lote_fecha ON consumos_medicamento (lote_id, fecha_aplicacion);
