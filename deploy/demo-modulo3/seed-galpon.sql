-- Solo para la base de demostracion. No es una migracion Flyway.
BEGIN;
DO $$
BEGIN
  IF current_database() <> 'avicontrol_modulo2_demo' THEN
    RAISE EXCEPTION 'Este script solo se ejecuta en avicontrol_modulo2_demo';
  END IF;
END $$;
INSERT INTO modulo1_galpon_local
  (id,lote_id,estado,poblacion_viva,fecha_ingreso,nombre,aforo_maximo,lote_codigo)
VALUES
  ('a402a368-cd4d-4e27-8d48-9e0d7eb8d374',
   '7331eec8-e447-4868-bdef-8eca3fff8d44',
   'PRODUCTIVA',1000,(CURRENT_TIMESTAMP AT TIME ZONE 'America/Bogota')::date-44,
   'DEMO - Galpon integracion M3',5000,'DEMO - Lote integracion M3')
ON CONFLICT (id) DO UPDATE SET
  lote_id=EXCLUDED.lote_id,estado=EXCLUDED.estado,poblacion_viva=EXCLUDED.poblacion_viva,
  fecha_ingreso=EXCLUDED.fecha_ingreso,nombre=EXCLUDED.nombre,
  aforo_maximo=EXCLUDED.aforo_maximo,lote_codigo=EXCLUDED.lote_codigo
WHERE modulo1_galpon_local.nombre='Galpón local'
  AND modulo1_galpon_local.lote_codigo='LDP-LOCAL'
  AND modulo1_galpon_local.version=0;
COMMIT;
