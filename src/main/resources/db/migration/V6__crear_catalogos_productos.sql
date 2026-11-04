CREATE TABLE alimento (
  id UUID PRIMARY KEY,
  nombre_comercial VARCHAR(160) NOT NULL,
  marca VARCHAR(120) NOT NULL,
  tipo VARCHAR(30) NOT NULL,
  peso_nominal_bulto_kg NUMERIC(19,4) NOT NULL CHECK (peso_nominal_bulto_kg > 0),
  duracion_recomendada_etapa_dias INTEGER,
  estado VARCHAR(20) NOT NULL,
  CONSTRAINT ck_alimento_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);
CREATE TABLE medicamento (
  id UUID PRIMARY KEY,
  nombre_comercial VARCHAR(160) NOT NULL,
  presentacion_compatible VARCHAR(120) NOT NULL,
  unidad_base VARCHAR(20) NOT NULL,
  estado VARCHAR(20) NOT NULL,
  CONSTRAINT ck_medicamento_unidad CHECK (unidad_base IN ('MILIGRAMO', 'GRAMO', 'MILILITRO', 'UNIDAD')),
  CONSTRAINT ck_medicamento_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);
