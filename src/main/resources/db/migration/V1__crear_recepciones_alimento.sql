CREATE TABLE recepciones_alimento (
  id UUID PRIMARY KEY,
  alimento_id UUID NOT NULL,
  codigo_lote VARCHAR(100) NOT NULL,
  fecha_ingreso DATE NOT NULL,
  fecha_vencimiento DATE NOT NULL,
  cantidad_bultos INTEGER NOT NULL,
  peso_por_bulto_kg NUMERIC(18, 4) NOT NULL,
  precio_neto_por_bulto NUMERIC(18, 2) NOT NULL,
  impuesto_porcentaje NUMERIC(7, 4) NOT NULL,
  cantidad_total_kg NUMERIC(18, 4) NOT NULL,
  precio_neto_kg NUMERIC(18, 4) NOT NULL,
  precio_neto_total NUMERIC(18, 2) NOT NULL,
  valor_impuesto NUMERIC(18, 2) NOT NULL,
  precio_total NUMERIC(18, 2) NOT NULL,
  estado VARCHAR(30) NOT NULL,
  actor VARCHAR(100) NOT NULL,
  CHECK (fecha_vencimiento > fecha_ingreso)
);
