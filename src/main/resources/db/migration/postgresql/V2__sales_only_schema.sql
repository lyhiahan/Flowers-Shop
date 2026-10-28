DO $$ BEGIN
IF EXISTS (SELECT 1 FROM orders WHERE order_type <> 'BUY' OR status IN ('RENTING','RETURNED','OVERDUE'))
 OR EXISTS (SELECT 1 FROM order_details WHERE order_type <> 'BUY')
 OR EXISTS (SELECT 1 FROM cart_items WHERE order_type <> 'BUY')
 OR EXISTS (SELECT 1 FROM promotions WHERE apply_type <> 'BUY') THEN
 RAISE EXCEPTION 'Unsupported historical transactions found. Export and reconcile them before the sales-only schema upgrade.';
END IF;
END $$;

CREATE TABLE schema_change_archive (
 migration_version varchar(20) NOT NULL, table_name varchar(64) NOT NULL,
 row_id bigint NOT NULL, payload jsonb NOT NULL,
 archived_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(migration_version,table_name,row_id)
);

INSERT INTO schema_change_archive (migration_version,table_name,row_id,payload) SELECT '2','products',s.id,jsonb_build_object('price_rent_per_day',s.price_rent_per_day,'deposit_fee',s.deposit_fee) FROM products s;

ALTER TABLE products DROP COLUMN price_rent_per_day, DROP COLUMN deposit_fee;

INSERT INTO schema_change_archive (migration_version,table_name,row_id,payload) SELECT '2','cart_items',s.id,jsonb_build_object('rent_start_date',s.rent_start_date,'rent_end_date',s.rent_end_date) FROM cart_items s;

ALTER TABLE cart_items DROP COLUMN rent_start_date, DROP COLUMN rent_end_date;

INSERT INTO schema_change_archive (migration_version,table_name,row_id,payload) SELECT '2','order_details',s.id,jsonb_build_object('deposit_fee',s.deposit_fee,'rent_start_date',s.rent_start_date,'rent_end_date',s.rent_end_date,'actual_return_date',s.actual_return_date) FROM order_details s;

ALTER TABLE order_details DROP COLUMN deposit_fee, DROP COLUMN rent_start_date, DROP COLUMN rent_end_date, DROP COLUMN actual_return_date;

INSERT INTO schema_change_archive (migration_version,table_name,row_id,payload) SELECT '2','orders',s.id,jsonb_build_object('return_store_id',s.return_store_id,'total_deposit',s.total_deposit,'fine_amount',s.fine_amount) FROM orders s;

ALTER TABLE orders DROP COLUMN return_store_id, DROP COLUMN total_deposit, DROP COLUMN fine_amount;

DO $$ DECLARE constraint_row record; BEGIN
FOR constraint_row IN SELECT c.conname FROM pg_constraint c WHERE c.conrelid='orders'::regclass AND c.contype='c'
 AND (SELECT a.attnum FROM pg_attribute a WHERE a.attrelid=c.conrelid AND a.attname='order_type')=ANY(c.conkey) LOOP
 EXECUTE format('ALTER TABLE orders DROP CONSTRAINT %I',constraint_row.conname);
END LOOP;
END $$;
ALTER TABLE orders ADD CONSTRAINT ck_orders_order_type_sales CHECK (order_type IN ('BUY'));

DO $$ DECLARE constraint_row record; BEGIN
FOR constraint_row IN SELECT c.conname FROM pg_constraint c WHERE c.conrelid='order_details'::regclass AND c.contype='c'
 AND (SELECT a.attnum FROM pg_attribute a WHERE a.attrelid=c.conrelid AND a.attname='order_type')=ANY(c.conkey) LOOP
 EXECUTE format('ALTER TABLE order_details DROP CONSTRAINT %I',constraint_row.conname);
END LOOP;
END $$;
ALTER TABLE order_details ADD CONSTRAINT ck_order_details_order_type_sales CHECK (order_type IN ('BUY'));

DO $$ DECLARE constraint_row record; BEGIN
FOR constraint_row IN SELECT c.conname FROM pg_constraint c WHERE c.conrelid='cart_items'::regclass AND c.contype='c'
 AND (SELECT a.attnum FROM pg_attribute a WHERE a.attrelid=c.conrelid AND a.attname='order_type')=ANY(c.conkey) LOOP
 EXECUTE format('ALTER TABLE cart_items DROP CONSTRAINT %I',constraint_row.conname);
END LOOP;
END $$;
ALTER TABLE cart_items ADD CONSTRAINT ck_cart_items_order_type_sales CHECK (order_type IN ('BUY'));

DO $$ DECLARE constraint_row record; BEGIN
FOR constraint_row IN SELECT c.conname FROM pg_constraint c WHERE c.conrelid='promotions'::regclass AND c.contype='c'
 AND (SELECT a.attnum FROM pg_attribute a WHERE a.attrelid=c.conrelid AND a.attname='apply_type')=ANY(c.conkey) LOOP
 EXECUTE format('ALTER TABLE promotions DROP CONSTRAINT %I',constraint_row.conname);
END LOOP;
END $$;
ALTER TABLE promotions ADD CONSTRAINT ck_promotions_apply_type_sales CHECK (apply_type IN ('BUY'));

DO $$ DECLARE constraint_row record; BEGIN
FOR constraint_row IN SELECT c.conname FROM pg_constraint c WHERE c.conrelid='orders'::regclass AND c.contype='c'
 AND (SELECT a.attnum FROM pg_attribute a WHERE a.attrelid=c.conrelid AND a.attname='status')=ANY(c.conkey) LOOP
 EXECUTE format('ALTER TABLE orders DROP CONSTRAINT %I',constraint_row.conname);
END LOOP;
END $$;
ALTER TABLE orders ADD CONSTRAINT ck_orders_status_sales CHECK (status IN ('PENDING','CONFIRMED','PREPARING','PICKED_UP','DELIVERING','DELIVERED','COMPLETED','CANCELLED','REFUND_REQUESTED','REFUNDED'));
