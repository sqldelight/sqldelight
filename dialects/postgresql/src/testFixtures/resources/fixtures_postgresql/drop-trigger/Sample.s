DROP TRIGGER orders_set_updated_at ON Order;

DROP TRIGGER IF EXISTS orders_set_updated_at ON Order CASCADE;
