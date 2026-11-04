CREATE TABLE enfermedad (
  id UUID PRIMARY KEY,
  nombre VARCHAR(120) NOT NULL,
  nivel_riesgo VARCHAR(20) NOT NULL,
  tipo VARCHAR(20) NOT NULL,
  descripcion_clinica VARCHAR(2000) NOT NULL,
  requiere_sacrificio_sanitario BOOLEAN NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT ck_enfermedad_nivel_riesgo CHECK (nivel_riesgo IN ('BAJO', 'MEDIO', 'ALTO')),
  CONSTRAINT ck_enfermedad_tipo CHECK (tipo IN ('VIRAL', 'BACTERIANA', 'PARASITARIA', 'FUNGICA', 'NUTRICIONAL', 'OTRA'))
);
