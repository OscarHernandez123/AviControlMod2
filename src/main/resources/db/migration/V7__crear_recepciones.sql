CREATE TABLE recepcion_alimento (
  id UUID PRIMARY KEY, alimento_id UUID NOT NULL, nombre_alimento_historico VARCHAR(160) NOT NULL,
  tipo_alimento_historico VARCHAR(30) NOT NULL, codigo_lote VARCHAR(120) NOT NULL,
  fecha_ingreso DATE NOT NULL, fecha_vencimiento DATE NOT NULL,
  cantidad_bultos NUMERIC(19,4) NOT NULL, peso_por_bulto_kg NUMERIC(19,4) NOT NULL,
  precio_neto_por_bulto NUMERIC(19,2) NOT NULL, porcentaje_impuesto NUMERIC(9,4) NOT NULL,
  peso_total_kg NUMERIC(19,4) NOT NULL, precio_neto_por_kg NUMERIC(19,4) NOT NULL,
  subtotal_neto NUMERIC(19,2) NOT NULL, valor_impuesto NUMERIC(19,2) NOT NULL,
  total_compra NUMERIC(19,2) NOT NULL, estado VARCHAR(20) NOT NULL
);
CREATE TABLE recepcion_medicamento (
  id UUID PRIMARY KEY, medicamento_id UUID NOT NULL, nombre_medicamento_historico VARCHAR(160) NOT NULL,
  codigo_lote VARCHAR(120) NOT NULL, presentacion VARCHAR(120) NOT NULL,
  cantidad NUMERIC(19,4) NOT NULL, contenido_por_presentacion NUMERIC(19,4) NOT NULL,
  unidad_medida VARCHAR(20) NOT NULL, fecha_ingreso DATE NOT NULL, fecha_vencimiento DATE NOT NULL,
  precio_neto_por_presentacion NUMERIC(19,2) NOT NULL, porcentaje_impuesto NUMERIC(9,4) NOT NULL,
  contenido_total_base NUMERIC(19,4) NOT NULL, precio_neto_por_unidad_base NUMERIC(19,4) NOT NULL,
  subtotal_neto NUMERIC(19,2) NOT NULL, valor_impuesto NUMERIC(19,2) NOT NULL,
  total_compra NUMERIC(19,2) NOT NULL, estado VARCHAR(20) NOT NULL
);
