-- Keep the internal branch identifier so existing orders, cash sessions and reports remain intact.
-- Do not merge or delete independent restaurants automatically: an installation with more than
-- one branch must be separated and backed up before this migration can be applied.
DO $$
BEGIN
    IF (SELECT COUNT(*) FROM branches) > 1 THEN
        RAISE EXCEPTION 'Essential local admite un solo restaurante. Esta base contiene varias sucursales; separa sus datos en instalaciones independientes antes de aplicar V10.';
    END IF;
END
$$;

INSERT INTO branches (code, name, address, phone, timezone, sort_order)
SELECT 'PRINCIPAL', display_name, address, phone, timezone, 0
FROM business_settings
WHERE id = 1 AND NOT EXISTS (SELECT 1 FROM branches);

UPDATE branches SET active = TRUE WHERE active = FALSE;

UPDATE branches AS branch
SET name = settings.display_name,
    address = settings.address,
    phone = settings.phone,
    timezone = settings.timezone
FROM business_settings AS settings
WHERE settings.id = 1;

ALTER TABLE branches ADD CONSTRAINT ck_branches_local_active CHECK (active);
CREATE UNIQUE INDEX uk_branches_one_local_restaurant ON branches ((true));
