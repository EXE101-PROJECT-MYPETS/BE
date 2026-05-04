WITH ranked_active_configs AS (
    SELECT
        id,
        row_number() OVER (PARTITION BY shop_id ORDER BY id) AS active_rank
    FROM shop_payment_configs
    WHERE active = true
)
UPDATE shop_payment_configs config
SET active = false
FROM ranked_active_configs ranked
WHERE config.id = ranked.id
  AND ranked.active_rank > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_shop_payment_configs_shop_active_true
    ON shop_payment_configs(shop_id)
    WHERE active = true;
