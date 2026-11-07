ALTER TABLE medicaciones ADD COLUMN nombre_medicamento VARCHAR(255);
ALTER TABLE medicaciones ADD COLUMN principio_activo VARCHAR(255);
ALTER TABLE medicaciones ADD COLUMN dias_aplicacion VARCHAR(500);

ALTER TABLE diagnosticos_galpon ADD COLUMN medicamento_nombre VARCHAR(255);
ALTER TABLE diagnosticos_galpon ADD COLUMN principio_activo VARCHAR(255);
ALTER TABLE diagnosticos_galpon ADD COLUMN dias_aplicacion VARCHAR(500);
ALTER TABLE diagnosticos_galpon ADD CONSTRAINT ck_diagnostico_metadata
  CHECK ((medicamento_nombre IS NULL AND principio_activo IS NULL)
      OR (medicamento_nombre IS NOT NULL AND principio_activo IS NOT NULL));

ALTER TABLE diagnosticos_galpon DROP CONSTRAINT IF EXISTS uq_diagnostico_activo_galpon;
DROP INDEX IF EXISTS uq_diagnostico_activo_galpon;
CREATE UNIQUE INDEX uq_diagnostico_activo_galpon_enfermedad
  ON diagnosticos_galpon (active_galpon_id, enfermedad_id);
CREATE INDEX idx_diagnosticos_activos_galpon
  ON diagnosticos_galpon (active_galpon_id);
