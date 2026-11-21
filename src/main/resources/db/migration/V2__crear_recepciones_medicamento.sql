CREATE TABLE recepciones_medicamento (
  id UUID PRIMARY KEY,
  medicamento_id UUID NOT NULL,
  codigo_lote VARCHAR(100) NOT NULL,
  presentacion VARCHAR(100) NOT NULL,
  fecha_ingreso DATE NOT NULL,
  fecha_vencimiento DATE NOT NULL,
  cantidad_presentaciones INTEGER NOT NULL,
  contenido_por_presentacion NUMERIC(18, 4) NOT NULL,
  unidad_contenido VARCHAR(20) NOT NULL,
  precio_neto_por_presentacion NUMERIC(18, 2) NOT NULL,
  impuesto_porcentaje NUMERIC(7, 4) NOT NULL,
  cantidad_unidad_base NUMERIC(18, 4) NOT NULL,
  unidad_base VARCHAR(20) NOT NULL,
  precio_neto_unidad_base NUMERIC(18, 4) NOT NULL,
  precio_neto_total NUMERIC(18, 2) NOT NULL,
  valor_impuesto NUMERIC(18, 2) NOT NULL,
  precio_total NUMERIC(18, 2) NOT NULL,
  estado VARCHAR(30) NOT NULL,
  actor VARCHAR(100) NOT NULL,
  CHECK (fecha_vencimiento > fecha_ingreso)
);

