CREATE TABLE auditoria_recepcion (
  id UUID PRIMARY KEY, recepcion_id UUID NOT NULL, actor_id VARCHAR(100) NOT NULL,
  fecha_hora TIMESTAMP WITH TIME ZONE NOT NULL, valores_anteriores TEXT NOT NULL, valores_nuevos TEXT NOT NULL
);
CREATE INDEX ix_auditoria_recepcion ON auditoria_recepcion (recepcion_id, fecha_hora);
