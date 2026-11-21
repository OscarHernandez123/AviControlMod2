ALTER TABLE modulo1_galpon_local ADD COLUMN nombre VARCHAR(150) DEFAULT 'Galpón sin nombre' NOT NULL;
ALTER TABLE modulo1_galpon_local ADD COLUMN aforo_maximo INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE modulo1_galpon_local ADD COLUMN lote_codigo VARCHAR(100) DEFAULT 'Lote sin código' NOT NULL;
ALTER TABLE modulo1_galpon_local ADD CONSTRAINT ck_modulo1_aforo CHECK (aforo_maximo >= 0);
ALTER TABLE modulo1_galpon_local ALTER COLUMN lote_id DROP NOT NULL;
ALTER TABLE modulo1_galpon_local ALTER COLUMN fecha_ingreso DROP NOT NULL;
ALTER TABLE modulo1_galpon_local ALTER COLUMN lote_codigo DROP NOT NULL;
