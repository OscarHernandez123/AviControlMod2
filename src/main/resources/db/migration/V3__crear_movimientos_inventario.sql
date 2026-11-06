CREATE TABLE movimientos_inventario (
  id UUID PRIMARY KEY,
  recepcion_id UUID NOT NULL,
  producto_id UUID NOT NULL,
  tipo_producto VARCHAR(20) NOT NULL,
  tipo_movimiento VARCHAR(20) NOT NULL,
  cantidad NUMERIC(18, 4) NOT NULL,
  unidad VARCHAR(20) NOT NULL,
  actor VARCHAR(100) NOT NULL,
  fecha_registro TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE (recepcion_id, tipo_producto, tipo_movimiento)
);
