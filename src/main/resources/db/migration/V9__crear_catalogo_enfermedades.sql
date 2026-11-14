CREATE TABLE enfermedades (
  id UUID PRIMARY KEY,
  nombre VARCHAR(150) NOT NULL,
  nivel_riesgo VARCHAR(20) NOT NULL,
  tipo VARCHAR(20) NOT NULL,
  descripcion_clinica VARCHAR(4000) NOT NULL,
  requiere_sacrificio_sanitario BOOLEAN NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL,
  fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL,
  CHECK (LENGTH(TRIM(nombre)) > 0),
  CHECK (LENGTH(TRIM(descripcion_clinica)) > 0),
  CHECK (nivel_riesgo IN ('BAJO', 'MODERADO', 'ALTO', 'CRITICO')),
  CHECK (tipo IN ('VIRAL', 'BACTERIANA', 'PARASITARIA', 'FUNGICA', 'OTRA'))
);
