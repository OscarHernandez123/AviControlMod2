ALTER TABLE modulo1_galpon_local ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE descuentos_inventario_vivo (
  idempotency_key UUID PRIMARY KEY,
  lote_id UUID NOT NULL,
  galpon_id UUID NOT NULL,
  cantidad INTEGER NOT NULL CHECK (cantidad > 0),
  causa VARCHAR(100) NOT NULL,
  actor VARCHAR(100) NOT NULL,
  poblacion_anterior INTEGER NOT NULL,
  poblacion_resultante INTEGER NOT NULL CHECK (poblacion_resultante >= 0)
);

CREATE TABLE registros_mortalidad (
  id UUID PRIMARY KEY,
  galpon_id UUID NOT NULL,
  lote_id UUID NOT NULL,
  fecha_hora_evento TIMESTAMP WITH TIME ZONE NOT NULL,
  fecha_hora_registro TIMESTAMP WITH TIME ZONE NOT NULL,
  cantidad INTEGER NOT NULL CHECK (cantidad > 0),
  causa VARCHAR(100) NOT NULL,
  observaciones VARCHAR(2000),
  responsable VARCHAR(100) NOT NULL,
  clasificacion VARCHAR(20) NOT NULL CHECK (clasificacion IN ('NORMAL','ANORMAL')),
  poblacion_anterior INTEGER NOT NULL,
  poblacion_resultante INTEGER NOT NULL
);
