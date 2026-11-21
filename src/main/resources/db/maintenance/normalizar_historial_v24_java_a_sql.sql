-- PostgreSQL. Ejecutar solo al sustituir la V24 Java ya aplicada por su equivalente SQL.
-- No es una migración automática. Respaldar antes y ejecutar Flyway repair/validate después.
DO $$
DECLARE
  version_actual record;
BEGIN
  SELECT type,script,checksum,success INTO STRICT version_actual
    FROM flyway_schema_history WHERE version='24';

  IF version_actual.type='SQL'
    AND version_actual.script='V24__permitir_orden_tras_cancelacion.sql'
    AND version_actual.success THEN
    RETURN;
  END IF;

  IF version_actual.type<>'JDBC'
    OR version_actual.script<>'db.migration.V24__permitir_orden_tras_cancelacion'
    OR version_actual.checksum IS NOT NULL
    OR NOT version_actual.success THEN
    RAISE EXCEPTION 'El historial V24 no corresponde a la migración Java esperada';
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema=CURRENT_SCHEMA AND table_name='ordenes_sacrificio_productivo'
        AND column_name='lote_reservado_id' AND data_type='uuid')
    OR NOT EXISTS (
      SELECT 1 FROM pg_constraint
      WHERE conrelid='ordenes_sacrificio_productivo'::regclass
        AND conname='ck_orden_reserva_lote' AND contype='c' AND convalidated)
    OR NOT EXISTS (
      SELECT 1 FROM pg_index i JOIN pg_class c ON c.oid=i.indexrelid
      WHERE i.indrelid='ordenes_sacrificio_productivo'::regclass
        AND c.relname='uq_orden_no_cancelada_galpon_lote'
        AND i.indisunique AND i.indisvalid) THEN
    RAISE EXCEPTION 'El esquema no contiene los objetos de V24; no se modifica el historial';
  END IF;

  IF EXISTS (
    SELECT 1 FROM ordenes_sacrificio_productivo
    WHERE (estado='CANCELADA' AND lote_reservado_id IS NOT NULL)
      OR (estado<>'CANCELADA'
        AND (lote_reservado_id IS NULL OR lote_reservado_id<>lote_id))) THEN
    RAISE EXCEPTION 'Hay reservas de lote inconsistentes; no se modifica el historial';
  END IF;

  UPDATE flyway_schema_history
    SET type='SQL',script='V24__permitir_orden_tras_cancelacion.sql'
    WHERE version='24';
END $$;