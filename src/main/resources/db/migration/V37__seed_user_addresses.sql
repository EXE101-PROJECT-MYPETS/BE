INSERT INTO user_addresses (
    user_id,
    name,
    tel,
    address,
    province,
    district,
    ward,
    hamlet,
    is_default
)
SELECT
    users.id,
    COALESCE(NULLIF(btrim(users.full_name), ''), 'Nguoi nhan ' || users.id),
    COALESCE(NULLIF(btrim(users.phone), ''), '090' || lpad(users.id::text, 7, '0')),
    CASE (users.id % 6)
        WHEN 0 THEN '123 Nguyen Chi Thanh'
        WHEN 1 THEN '45 Le Loi'
        WHEN 2 THEN '78 Nguyen Hue'
        WHEN 3 THEN '12 Cach Mang Thang 8'
        WHEN 4 THEN '89 Vo Van Tan'
        ELSE '56 Dien Bien Phu'
    END,
    'TP. Ho Chi Minh',
    CASE (users.id % 6)
        WHEN 0 THEN 'Quan 1'
        WHEN 1 THEN 'Quan 3'
        WHEN 2 THEN 'Quan Binh Thanh'
        WHEN 3 THEN 'Quan 10'
        WHEN 4 THEN 'Quan Phu Nhuan'
        ELSE 'Thanh pho Thu Duc'
    END,
    CASE (users.id % 6)
        WHEN 0 THEN 'Phuong Ben Nghe'
        WHEN 1 THEN 'Phuong Vo Thi Sau'
        WHEN 2 THEN 'Phuong 25'
        WHEN 3 THEN 'Phuong 12'
        WHEN 4 THEN 'Phuong 7'
        ELSE 'Phuong Thao Dien'
    END,
    'Khac',
    true
FROM users
WHERE NOT EXISTS (
    SELECT 1
    FROM user_addresses existing_address
    WHERE existing_address.user_id = users.id
);
