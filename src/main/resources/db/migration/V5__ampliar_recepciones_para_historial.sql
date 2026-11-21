ALTER TABLE recepciones_alimento ADD COLUMN nombre_alimento VARCHAR(255);
ALTER TABLE recepciones_alimento ADD COLUMN tipo_alimento VARCHAR(20);
ALTER TABLE recepciones_medicamento ADD COLUMN nombre_medicamento VARCHAR(255);
ALTER TABLE movimientos_inventario ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADO';

CREATE INDEX idx_recepciones_alimento_fecha ON recepciones_alimento (fecha_ingreso);
CREATE INDEX idx_recepciones_medicamento_fecha ON recepciones_medicamento (fecha_ingreso);
CREATE INDEX idx_movimientos_recepcion_estado ON movimientos_inventario (recepcion_id, estado);
