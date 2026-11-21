CREATE TABLE resultados_sacrificio_productivo (
  id UUID PRIMARY KEY,
  orden_id UUID NOT NULL UNIQUE REFERENCES ordenes_sacrificio_productivo(id),
  galpon_id UUID NOT NULL,
  lote_id UUID NOT NULL UNIQUE,
  cantidad_final_pollos INTEGER NOT NULL,
  peso_total_kg NUMERIC(18, 2) NOT NULL,
  estado VARCHAR(20) NOT NULL,
  fecha_registro TIMESTAMP WITH TIME ZONE NOT NULL,
  fecha_correccion TIMESTAMP WITH TIME ZONE,
  version BIGINT NOT NULL DEFAULT 0,
  CHECK (cantidad_final_pollos > 0),
  CHECK (peso_total_kg > 0),
  CHECK (estado IN ('DISPONIBLE', 'UTILIZADO')),
  CHECK (version >= 0)
);

CREATE TABLE confirmaciones_uso_resultado_sacrificio (
  evento_id UUID PRIMARY KEY,
  resultado_id UUID NOT NULL REFERENCES resultados_sacrificio_productivo(id)
);
