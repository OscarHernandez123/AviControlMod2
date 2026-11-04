CREATE TABLE movimiento_alimento (id UUID PRIMARY KEY, recepcion_id UUID NOT NULL, tipo VARCHAR(20) NOT NULL, cantidad_kg NUMERIC(19,4) NOT NULL, estado VARCHAR(20) NOT NULL, ocurrido_en TIMESTAMP WITH TIME ZONE NOT NULL, actor_id VARCHAR(100));
CREATE INDEX ix_movimiento_alimento_recepcion ON movimiento_alimento (recepcion_id);
CREATE TABLE movimiento_medicamento (id UUID PRIMARY KEY, recepcion_id UUID NOT NULL, tipo VARCHAR(20) NOT NULL, cantidad_unidad_base NUMERIC(19,4) NOT NULL, unidad_base VARCHAR(20) NOT NULL, estado VARCHAR(20) NOT NULL, ocurrido_en TIMESTAMP WITH TIME ZONE NOT NULL, actor_id VARCHAR(100));
CREATE INDEX ix_movimiento_medicamento_recepcion ON movimiento_medicamento (recepcion_id);
