-- Operator-run only; the application does not execute this script at startup.
-- Select the intended schema in the connection search_path before running with
-- psql -v ON_ERROR_STOP=1 -f this-file.sql. Do not apply to shared environments
-- without the database owner's authorization.
BEGIN;

DO $migration$
DECLARE
    target_schema text := current_schema();
    target_table regclass;
    existing_type oid;
BEGIN
    IF target_schema IS NULL THEN
        RAISE EXCEPTION 'An explicit existing target schema is required';
    END IF;
    target_table := to_regclass(format('%I.users', target_schema));
    IF target_table IS NULL THEN
        RAISE EXCEPTION 'The selected schema must contain users';
    END IF;

    EXECUTE format('LOCK TABLE %I.users IN ACCESS EXCLUSIVE MODE', target_schema);
    SELECT atttypid INTO existing_type
      FROM pg_catalog.pg_attribute
     WHERE attrelid = target_table AND attname = 'is_emergency_available'
       AND attnum > 0 AND NOT attisdropped;
    IF existing_type IS NOT NULL AND existing_type <> 'pg_catalog.bool'::regtype THEN
        RAISE EXCEPTION 'Existing emergency availability column must have boolean type';
    END IF;

    IF existing_type IS NULL THEN
        EXECUTE format('ALTER TABLE %I.users ADD COLUMN is_emergency_available boolean', target_schema);
    END IF;
    EXECUTE format('UPDATE %I.users SET is_emergency_available = false WHERE is_emergency_available IS NULL', target_schema);
    EXECUTE format('ALTER TABLE %I.users ALTER COLUMN is_emergency_available SET DEFAULT false', target_schema);
    EXECUTE format('ALTER TABLE %I.users ALTER COLUMN is_emergency_available SET NOT NULL', target_schema);
END
$migration$;

COMMIT;
