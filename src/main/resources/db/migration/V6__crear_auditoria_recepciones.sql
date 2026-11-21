CREATE TABLE auditoria_recepciones (
  id UUID PRIMARY KEY,
  recepcion_id UUID NOT NULL,
  tipo_recepcion VARCHAR(20) NOT NULL,
  accion VARCHAR(20) NOT NULL,
  actor VARCHAR(100) NOT NULL,
  fecha TIMESTAMP WITH TIME ZONE NOT NULL,
  valores_anteriores TEXT NOT NULL,
  valores_nuevos TEXT NOT NULL
);

CREATE INDEX idx_auditoria_recepcion ON auditoria_recepciones (recepcion_id, fecha);
