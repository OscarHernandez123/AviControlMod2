CREATE TABLE modulo1_operaciones (
  id UUID PRIMARY KEY REFERENCES modulo1_event_outbox(alerta_id),
  tipo VARCHAR(40) NOT NULL,
  galpon_id UUID NOT NULL,
  lote_id UUID NOT NULL,
  datos TEXT NOT NULL,
  creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
