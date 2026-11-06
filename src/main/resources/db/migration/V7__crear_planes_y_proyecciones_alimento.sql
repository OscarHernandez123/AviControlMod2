CREATE TABLE plantillas_nutricionales (
  id UUID PRIMARY KEY,
  nombre VARCHAR(150) NOT NULL,
  tipo_ave VARCHAR(60) NOT NULL,
  es_predeterminado BOOLEAN NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE alimentos_comerciales_nutricion (
  id UUID PRIMARY KEY,
  nombre VARCHAR(150) NOT NULL,
  tipo_alimento VARCHAR(20) NOT NULL,
  peso_nominal_bulto_kg NUMERIC(12, 4) NOT NULL,
  proteina_porcentaje NUMERIC(7, 2) NOT NULL,
  energia_kcal_kg NUMERIC(12, 2) NOT NULL,
  duracion_recomendada_etapa_dias INTEGER,
  activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE etapas_plantilla_nutricional (
  id UUID PRIMARY KEY,
  plantilla_id UUID NOT NULL REFERENCES plantillas_nutricionales(id),
  etapa_crianza VARCHAR(20) NOT NULL,
  dia_inicio INTEGER NOT NULL,
  dia_fin INTEGER NOT NULL,
  racion_kg_ave_dia NUMERIC(12, 4) NOT NULL,
  tipo_alimento VARCHAR(20) NOT NULL,
  alimento_id UUID,
  peso_nominal_bulto_kg NUMERIC(12, 4) NOT NULL,
  UNIQUE (plantilla_id, etapa_crianza),
  CHECK (dia_inicio > 0 AND dia_fin >= dia_inicio),
  CHECK (racion_kg_ave_dia > 0 AND peso_nominal_bulto_kg > 0)
);

CREATE TABLE planes_nutricionales_lote (
  id UUID PRIMARY KEY,
  galpon_id UUID NOT NULL,
  lote_id UUID NOT NULL UNIQUE,
  codigo_lote VARCHAR(100),
  plantilla_id UUID REFERENCES plantillas_nutricionales(id),
  tipo_ave VARCHAR(60) NOT NULL,
  fecha_ingreso DATE NOT NULL,
  edad_ingreso_dias INTEGER NOT NULL,
  poblacion_viva INTEGER NOT NULL,
  fecha_salida_proyectada DATE NOT NULL,
  estado VARCHAR(40) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  UNIQUE (galpon_id, lote_id),
  CHECK (edad_ingreso_dias > 0 AND poblacion_viva >= 0)
);

CREATE TABLE etapas_plan_nutricional (
  id UUID PRIMARY KEY,
  plan_id UUID NOT NULL REFERENCES planes_nutricionales_lote(id),
  etapa_crianza VARCHAR(20) NOT NULL,
  estado VARCHAR(20) NOT NULL,
  dia_inicio INTEGER NOT NULL,
  dia_fin INTEGER NOT NULL,
  racion_kg_ave_dia NUMERIC(12, 4) NOT NULL,
  tipo_alimento VARCHAR(20) NOT NULL,
  alimento_id UUID,
  peso_nominal_bulto_kg NUMERIC(12, 4) NOT NULL,
  UNIQUE (plan_id, etapa_crianza)
);

CREATE TABLE ajustes_etapa_nutricional (
  id UUID PRIMARY KEY,
  etapa_id UUID NOT NULL REFERENCES etapas_plan_nutricional(id),
  actor VARCHAR(100) NOT NULL,
  motivo VARCHAR(40) NOT NULL,
  justificacion VARCHAR(1000) NOT NULL,
  dia_fin_anterior INTEGER NOT NULL,
  dia_fin_nuevo INTEGER NOT NULL,
  fecha_registro TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE auditoria_planes_nutricionales (
  id UUID PRIMARY KEY,
  plan_id UUID NOT NULL REFERENCES planes_nutricionales_lote(id),
  etapa_id UUID,
  accion VARCHAR(40) NOT NULL,
  actor VARCHAR(100) NOT NULL,
  justificacion VARCHAR(1000),
  valor_anterior VARCHAR(200),
  valor_nuevo VARCHAR(200),
  fecha_registro TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE proyecciones_alimento_etapa (
  id UUID PRIMARY KEY,
  etapa_id UUID NOT NULL UNIQUE REFERENCES etapas_plan_nutricional(id),
  lote_id UUID NOT NULL,
  galpon_id UUID NOT NULL,
  alimento_id UUID,
  etapa_crianza VARCHAR(20) NOT NULL,
  tipo_alimento VARCHAR(20) NOT NULL,
  estado VARCHAR(20) NOT NULL,
  poblacion_inicio_etapa INTEGER NOT NULL,
  cuota_kg_ave_dia NUMERIC(12, 4) NOT NULL,
  peso_nominal_bulto_kg NUMERIC(12, 4) NOT NULL,
  dias_base INTEGER NOT NULL,
  dias_prorroga INTEGER NOT NULL DEFAULT 0,
  proyeccion_kg NUMERIC(18, 4) NOT NULL,
  costo_unitario_kg NUMERIC(18, 2),
  fuente_costo VARCHAR(50),
  fecha_captura_costo TIMESTAMP WITH TIME ZONE,
  advertencia_costo VARCHAR(500),
  costo_completado_manualmente BOOLEAN NOT NULL DEFAULT FALSE,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE ajustes_proyeccion_alimento (
  id UUID PRIMARY KEY,
  proyeccion_id UUID NOT NULL REFERENCES proyecciones_alimento_etapa(id),
  actor VARCHAR(100) NOT NULL,
  motivo VARCHAR(40) NOT NULL,
  justificacion VARCHAR(1000) NOT NULL,
  dias_anteriores INTEGER NOT NULL,
  dias_nuevos INTEGER NOT NULL,
  kilogramos_anteriores NUMERIC(18, 4) NOT NULL,
  kilogramos_nuevos NUMERIC(18, 4) NOT NULL,
  fecha_registro TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE auditoria_costo_proyeccion (
  id UUID PRIMARY KEY,
  proyeccion_id UUID NOT NULL REFERENCES proyecciones_alimento_etapa(id),
  actor VARCHAR(100) NOT NULL,
  justificacion VARCHAR(1000) NOT NULL,
  costo_unitario_kg NUMERIC(18, 2) NOT NULL,
  fecha_registro TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_proyecciones_lote ON proyecciones_alimento_etapa (lote_id);
CREATE INDEX idx_planes_galpon ON planes_nutricionales_lote (galpon_id);
