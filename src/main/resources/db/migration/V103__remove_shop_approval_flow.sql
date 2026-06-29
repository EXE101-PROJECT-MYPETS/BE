UPDATE shop_members member
SET status = 'ACTIVE'
WHERE member.role = 'OWNER'
  AND EXISTS (
      SELECT 1
      FROM shops shop
      WHERE shop.id = member.shop_id
        AND shop.status IN ('PENDING_APPROVAL', 'REJECTED')
  );

UPDATE shops
SET status = 'ACTIVE'
WHERE status IN ('PENDING_APPROVAL', 'REJECTED');
