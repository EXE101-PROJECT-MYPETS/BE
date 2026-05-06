WITH ranked_active_members AS (
    SELECT
        shop_id,
        user_id,
        ROW_NUMBER() OVER (
            PARTITION BY shop_id
            ORDER BY
                CASE WHEN role = 'OWNER'::shop_role THEN 0 ELSE 1 END,
                created_at ASC,
                user_id ASC
        ) AS row_number
    FROM shop_members
    WHERE status = 'ACTIVE'::member_status
)
UPDATE shop_members member
SET status = 'REMOVED'::member_status
FROM ranked_active_members ranked
WHERE member.shop_id = ranked.shop_id
  AND member.user_id = ranked.user_id
  AND ranked.row_number > 1;

UPDATE shop_members
SET role = 'OWNER'::shop_role
WHERE status = 'ACTIVE'::member_status
  AND role <> 'OWNER'::shop_role;

CREATE UNIQUE INDEX IF NOT EXISTS uq_shop_members_one_active_account
    ON shop_members(shop_id)
    WHERE status = 'ACTIVE'::member_status;
