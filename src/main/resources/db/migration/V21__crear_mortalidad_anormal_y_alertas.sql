CREATE TABLE registros_mortalidad_anormal (
  id UUID PRIMARY KEY,
  mortalidad_id UUID UNIQUE REFERENCES registros_mortalidad(id),
  galpon_id UUID NOT NULL,
  lote_id UUID NOT NULL,
  fecha_hora_evento TIMESTAMP WITH TIME ZONE NOT NULL,
  fecha_hora_registro TIMESTAMP WITH TIME ZONE NOT NULL,
  cantidad INTEGER NOT NULL CHECK (cantidad > 0),
  poblacion_base INTEGER NOT NULL CHECK (poblacion_base > 0),
  porcentaje_mortalidad NUMERIC(10,4) NOT NULL,
  severidad VARCHAR(30) NOT NULL,
  causa_probable VARCHAR(200) NOT NULL,
  observaciones VARCHAR(2000),
  responsable VARCHAR(100) NOT NULL
);
CREATE TABLE alertas_sanitarias (
  id UUID PRIMARY KEY,
  reporte_id UUID NOT NULL UNIQUE REFERENCES registros_mortalidad_anormal(id),
  galpon_id UUID NOT NULL,
  lote_id UUID NOT NULL,
  prioridad VARCHAR(30) NOT NULL,
  estado VARCHAR(50) NOT NULL,
  fecha_emision TIMESTAMP WITH TIME ZONE NOT NULL
);
