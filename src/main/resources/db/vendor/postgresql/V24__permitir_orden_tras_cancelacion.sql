-- Conserva el historial cancelado y reserva el lote solo para órdenes no canceladas.
ALTER TABLE ordenes_sacrificio_productivo ADD COLUMN lote_reservado_id UUID;

UPDATE ordenes_sacrificio_productivo
SET lote_reservado_id=lote_id
WHERE estado<>'CANCELADA';
-- La restricción de V17 no tenía nombre explícito; se identifica por sus columnas.
DO $$
DECLARE
  restriccion_anterior text;
BEGIN
  SELECT tc.constraint_name INTO STRICT restriccion_anterior
  FROM information_schema.table_constraints tc
  JOIN information_schema.key_column_usage k
    ON tc.constraint_catalog=k.constraint_catalog
    AND tc.constraint_schema=k.constraint_schema
    AND tc.constraint_name=k.constraint_name
    AND tc.table_name=k.table_name
  WHERE tc.table_schema=CURRENT_SCHEMA
    AND tc.table_name='ordenes_sacrificio_productivo'
    AND tc.constraint_type='UNIQUE'
  GROUP BY tc.constraint_name
  HAVING COUNT(*)=2
    AND SUM(CASE WHEN k.column_name IN ('galpon_id','lote_id') THEN 1 ELSE 0 END)=2;

  EXECUTE format('ALTER TABLE ordenes_sacrificio_productivo DROP CONSTRAINT %I', restriccion_anterior);
END $$;
ALTER TABLE ordenes_sacrificio_productivo ADD CONSTRAINT ck_orden_reserva_lote
  CHECK ((estado='CANCELADA' AND lote_reservado_id IS NULL)
    OR (estado<>'CANCELADA' AND lote_reservado_id IS NOT NULL AND lote_reservado_id=lote_id));

CREATE UNIQUE INDEX uq_orden_no_cancelada_galpon_lote
  ON ordenes_sacrificio_productivo (galpon_id,lote_reservado_id);