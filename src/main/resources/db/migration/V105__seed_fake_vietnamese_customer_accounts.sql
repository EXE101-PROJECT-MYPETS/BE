BEGIN;

SET LOCAL search_path TO prod;

-- Fake Vietnamese customer accounts
-- Password for all accounts: 123456
WITH seed_users (email, phone, full_name, address, age) AS (
    VALUES
        ('nguyenminhanh@gmail.com', '0987000001', 'Nguyễn Minh Anh', '12 Nguyễn Trãi, Số 12, Phường Thanh Xuân Trung, Quận Thanh Xuân, Hà Nội', 24),
        ('trangiabao@gmail.com', '0987000002', 'Trần Gia Bảo', '25 Chùa Bộc, Số 25, Phường Quang Trung, Quận Đống Đa, Hà Nội', 25),
        ('lehoanglong@gmail.com', '0987000003', 'Lê Hoàng Long', '8 Láng Hạ, Số 8, Phường Thành Công, Quận Ba Đình, Hà Nội', 26),
        ('phamthuha@gmail.com', '0987000004', 'Phạm Thu Hà', '41 Trần Duy Hưng, Số 41, Phường Trung Hòa, Quận Cầu Giấy, Hà Nội', 23),
        ('doquanghuy@gmail.com', '0987000005', 'Đỗ Quang Huy', '19 Xuân Thủy, Số 19, Phường Dịch Vọng Hậu, Quận Cầu Giấy, Hà Nội', 27),
        ('buikhanhlinh@gmail.com', '0987000006', 'Bùi Khánh Linh', '33 Hoàng Quốc Việt, Số 33, Phường Nghĩa Đô, Quận Cầu Giấy, Hà Nội', 22),
        ('vuducanh@gmail.com', '0987000007', 'Vũ Đức Anh', '6 Kim Mã, Số 6, Phường Kim Mã, Quận Ba Đình, Hà Nội', 28),
        ('dangngocmai@gmail.com', '0987000008', 'Đặng Ngọc Mai', '72 Nguyễn Văn Cừ, Số 72, Phường Bồ Đề, Quận Long Biên, Hà Nội', 24),
        ('hoangnamphuong@gmail.com', '0987000009', 'Hoàng Nam Phương', '15 Minh Khai, Số 15, Phường Minh Khai, Quận Hai Bà Trưng, Hà Nội', 25),
        ('ngotuanviet@gmail.com', '0987000010', 'Ngô Tuấn Việt', '54 Bạch Mai, Số 54, Phường Bạch Mai, Quận Hai Bà Trưng, Hà Nội', 29),
        ('duongbaongoc@gmail.com', '0987000011', 'Dương Bảo Ngọc', '9 Lạc Long Quân, Số 9, Phường Bưởi, Quận Tây Hồ, Hà Nội', 23),
        ('lythanhdat@gmail.com', '0987000012', 'Lý Thành Đạt', '88 Âu Cơ, Số 88, Phường Nhật Tân, Quận Tây Hồ, Hà Nội', 26),
        ('maiphuongthao@gmail.com', '0987000013', 'Mai Phương Thảo', '37 Giải Phóng, Số 37, Phường Giáp Bát, Quận Hoàng Mai, Hà Nội', 24),
        ('phanviethoang@gmail.com', '0987000014', 'Phan Việt Hoàng', '23 Trường Chinh, Số 23, Phường Khương Thượng, Quận Đống Đa, Hà Nội', 27),
        ('tangcohan@gmail.com', '0987000015', 'Tăng Cổ Hân', '61 Tố Hữu, Số 61, Phường Trung Văn, Quận Nam Từ Liêm, Hà Nội', 22),
        ('caominhquan@gmail.com', '0987000016', 'Cao Minh Quân', '14 Lê Đức Thọ, Số 14, Phường Mỹ Đình 2, Quận Nam Từ Liêm, Hà Nội', 28),
        ('trinhhutrang@gmail.com', '0987000017', 'Trịnh Hữu Trang', '29 Nguyễn Văn Huyên, Số 29, Phường Quan Hoa, Quận Cầu Giấy, Hà Nội', 25),
        ('haquoctiet@gmail.com', '0987000018', 'Hà Quốc Tiến', '101 Nguyễn Xiển, Số 101, Phường Hạ Đình, Quận Thanh Xuân, Hà Nội', 30),
        ('nguyenthanhtruc@gmail.com', '0987000019', 'Nguyễn Thanh Trúc', '45 Phan Đình Phùng, Phường Quán Thánh, Quận Ba Đình, Hà Nội', 23),
        ('tranhoangyen@gmail.com', '0987000020', 'Trần Hoàng Yến', '18 Hai Bà Trưng, Phường Tràng Tiền, Quận Hoàn Kiếm, Hà Nội', 24),
        ('leminhkhue@gmail.com', '0987000021', 'Lê Minh Khuê', '16 Nguyễn Văn Cừ, Phường Bồ Đề, Quận Long Biên, Hà Nội', 26),
        ('phamducminh@gmail.com', '0987000022', 'Phạm Đức Minh', '77 Nguyễn Chí Thanh, Phường Láng Thượng, Quận Đống Đa, Hà Nội', 27),
        ('dothanhnga@gmail.com', '0987000023', 'Đỗ Thanh Nga', '32 Tôn Đức Thắng, Phường Hàng Bột, Quận Đống Đa, Hà Nội', 25),
        ('buianhtu@gmail.com', '0987000024', 'Bùi Anh Tú', '11 Phạm Văn Đồng, Phường Mai Dịch, Quận Cầu Giấy, Hà Nội', 28),
        ('vuthuyduong@gmail.com', '0987000025', 'Vũ Thùy Dương', '90 Nguyễn Khánh Toàn, Phường Quan Hoa, Quận Cầu Giấy, Hà Nội', 24),
        ('dangvietanh@gmail.com', '0987000026', 'Đặng Việt Anh', '5 Nguyễn Đình Chiểu, Phường Lê Đại Hành, Quận Hai Bà Trưng, Hà Nội', 29),
        ('hoanglinhchi@gmail.com', '0987000027', 'Hoàng Linh Chi', '68 Trần Khát Chân, Phường Thanh Nhàn, Quận Hai Bà Trưng, Hà Nội', 22),
        ('ngobachduong@gmail.com', '0987000028', 'Ngô Bạch Dương', '39 Đội Cấn, Phường Đội Cấn, Quận Ba Đình, Hà Nội', 26),
        ('duongquynhnhu@gmail.com', '0987000029', 'Dương Quỳnh Như', '24 Vạn Phúc, Phường Liễu Giai, Quận Ba Đình, Hà Nội', 23),
        ('lyanhkhoa@gmail.com', '0987000030', 'Lý Anh Khoa', '56 Hoàng Hoa Thám, Phường Ngọc Hà, Quận Ba Đình, Hà Nội', 27),
        ('maithanhson@gmail.com', '0987000031', 'Mai Thanh Sơn', '83 Trần Phú, Phường Văn Quán, Quận Hà Đông, Hà Nội', 30)
)
INSERT INTO users (
    email,
    phone,
    full_name,
    status,
    address,
    age,
    role,
    created_at,
    updated_at
)
SELECT
    seed_users.email,
    NULL,
    seed_users.full_name,
    'ACTIVE'::user_status,
    seed_users.address,
    seed_users.age,
    'CUSTOMER'::user_role,
    now(),
    now()
FROM seed_users
ON CONFLICT (email) DO UPDATE SET
    phone = NULL,
    full_name = EXCLUDED.full_name,
    status = 'ACTIVE'::user_status,
    address = EXCLUDED.address,
    age = EXCLUDED.age,
    role = 'CUSTOMER'::user_role,
    updated_at = now();

WITH seed_users (email) AS (
    VALUES
        ('nguyenminhanh@gmail.com'),
        ('trangiabao@gmail.com'),
        ('lehoanglong@gmail.com'),
        ('phamthuha@gmail.com'),
        ('doquanghuy@gmail.com'),
        ('buikhanhlinh@gmail.com'),
        ('vuducanh@gmail.com'),
        ('dangngocmai@gmail.com'),
        ('hoangnamphuong@gmail.com'),
        ('ngotuanviet@gmail.com'),
        ('duongbaongoc@gmail.com'),
        ('lythanhdat@gmail.com'),
        ('maiphuongthao@gmail.com'),
        ('phanviethoang@gmail.com'),
        ('tangcohan@gmail.com'),
        ('caominhquan@gmail.com'),
        ('trinhhutrang@gmail.com'),
        ('haquoctiet@gmail.com'),
        ('nguyenthanhtruc@gmail.com'),
        ('tranhoangyen@gmail.com'),
        ('leminhkhue@gmail.com'),
        ('phamducminh@gmail.com'),
        ('dothanhnga@gmail.com'),
        ('buianhtu@gmail.com'),
        ('vuthuyduong@gmail.com'),
        ('dangvietanh@gmail.com'),
        ('hoanglinhchi@gmail.com'),
        ('ngobachduong@gmail.com'),
        ('duongquynhnhu@gmail.com'),
        ('lyanhkhoa@gmail.com'),
        ('maithanhson@gmail.com')
)
INSERT INTO user_credentials (
    user_id,
    provider,
    password_hash,
    provider_user_id,
    created_at,
    updated_at
)
SELECT
    users.id,
    'LOCAL'::credential_provider,
    '$2a$10$c0zDM/R/EIaH/emGTzoqSuJNSg1.4XZuSkVlZGDt4p6TAn8ER/Aya',
    NULL,
    now(),
    now()
FROM users
JOIN seed_users ON seed_users.email = users.email
ON CONFLICT (user_id) DO UPDATE SET
    provider = 'LOCAL'::credential_provider,
    password_hash = EXCLUDED.password_hash,
    provider_user_id = NULL,
    updated_at = now();

COMMIT;
