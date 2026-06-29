BEGIN;

SET LOCAL search_path TO prod;

-- Fake Vietnamese customer accounts
-- Password for all accounts: 123456
-- 15 accounts in Ha Noi, 16 accounts in nearby provinces
WITH seed_users (email, full_name, address, age) AS (
    VALUES
-- Ha Noi addresses
('[nguyenduongphu@gmail.com](mailto:nguyenduongphu@gmail.com)', 'Nguyễn Dương Phú', '118 Nguyễn Khuyến, Phường Văn Miếu, Quận Đống Đa, Hà Nội', 24),
('[tranminhchau@gmail.com](mailto:tranminhchau@gmail.com)', 'Trần Minh Châu', '22 Hàng Bông, Phường Hàng Gai, Quận Hoàn Kiếm, Hà Nội', 25),
('[lebaonghi@gmail.com](mailto:lebaonghi@gmail.com)', 'Lê Bảo Nghi', '7 Phan Chu Trinh, Phường Phan Chu Trinh, Quận Hoàn Kiếm, Hà Nội', 23),
('[phamkhanhduy@gmail.com](mailto:phamkhanhduy@gmail.com)', 'Phạm Khánh Duy', '46 Nguyễn Hoàng, Phường Mỹ Đình 2, Quận Nam Từ Liêm, Hà Nội', 27),
('[dohoanganhthu@gmail.com](mailto:dohoanganhthu@gmail.com)', 'Đỗ Hoàng Anh Thư', '12 Nguyễn Cơ Thạch, Phường Cầu Diễn, Quận Nam Từ Liêm, Hà Nội', 22),
('[buithienan@gmail.com](mailto:buithienan@gmail.com)', 'Bùi Thiên Ân', '99 Tân Mai, Phường Tân Mai, Quận Hoàng Mai, Hà Nội', 26),
('[vominhnhat@gmail.com](mailto:vominhnhat@gmail.com)', 'Võ Minh Nhật', '28 Vũ Tông Phan, Phường Khương Đình, Quận Thanh Xuân, Hà Nội', 28),
('[danghuyenmy@gmail.com](mailto:danghuyenmy@gmail.com)', 'Đặng Huyền My', '63 Nguyễn Sơn, Phường Ngọc Lâm, Quận Long Biên, Hà Nội', 24),
('[hoanggiakhang@gmail.com](mailto:hoanggiakhang@gmail.com)', 'Hoàng Gia Khang', '41 Trần Cung, Phường Nghĩa Tân, Quận Cầu Giấy, Hà Nội', 29),
('[ngocamly@gmail.com](mailto:ngocamly@gmail.com)', 'Ngô Cẩm Ly', '18 Phạm Ngọc Thạch, Phường Kim Liên, Quận Đống Đa, Hà Nội', 23),
('[duongvietdung@gmail.com](mailto:duongvietdung@gmail.com)', 'Dương Việt Dũng', '72 Tây Sơn, Phường Quang Trung, Quận Đống Đa, Hà Nội', 30),
('[lyngocdiep@gmail.com](mailto:lyngocdiep@gmail.com)', 'Lý Ngọc Diệp', '35 Nguyễn Hữu Huân, Phường Lý Thái Tổ, Quận Hoàn Kiếm, Hà Nội', 25),
('[maianhquan@gmail.com](mailto:maianhquan@gmail.com)', 'Mai Anh Quân', '27 Lạc Trung, Phường Vĩnh Tuy, Quận Hai Bà Trưng, Hà Nội', 27),
('[phanhoaitam@gmail.com](mailto:phanhoaitam@gmail.com)', 'Phan Hoài Tâm', '52 Hoàng Cầu, Phường Ô Chợ Dừa, Quận Đống Đa, Hà Nội', 24),
('[caotuanlinh@gmail.com](mailto:caotuanlinh@gmail.com)', 'Cao Tuấn Linh', '10 Nguyễn Khang, Phường Yên Hòa, Quận Cầu Giấy, Hà Nội', 28),

        ```
-- Nearby provinces around Ha Noi
        ('trinhbaonguyen@gmail.com', 'Trịnh Bảo Nguyên', '36 Lý Thái Tổ, Phường Võ Cường, Thành phố Bắc Ninh, Bắc Ninh', 26),
        ('haanhthu@gmail.com', 'Hà Anh Thư', '14 Nguyễn Gia Thiều, Phường Suối Hoa, Thành phố Bắc Ninh, Bắc Ninh', 23),
        ('nguyenhoangphuc@gmail.com', 'Nguyễn Hoàng Phúc', '25 Điện Biên, Phường Lê Lợi, Thành phố Hưng Yên, Hưng Yên', 29),
        ('tranthuytien@gmail.com', 'Trần Thủy Tiên', '8 Nguyễn Văn Linh, Phường Hiến Nam, Thành phố Hưng Yên, Hưng Yên', 24),
        ('lequangvinh@gmail.com', 'Lê Quang Vinh', '42 Trần Hưng Đạo, Phường Sao Đỏ, Thành phố Chí Linh, Hải Dương', 27),
        ('phamngoctram@gmail.com', 'Phạm Ngọc Trâm', '19 Lê Thanh Nghị, Phường Hải Tân, Thành phố Hải Dương, Hải Dương', 22),
        ('dothienbao@gmail.com', 'Đỗ Thiên Bảo', '31 Biên Hòa, Phường Minh Khai, Thành phố Phủ Lý, Hà Nam', 28),
        ('buithaochi@gmail.com', 'Bùi Thảo Chi', '6 Lê Công Thanh, Phường Lam Hạ, Thành phố Phủ Lý, Hà Nam', 25),
        ('vominhkhang@gmail.com', 'Võ Minh Khang', '55 Mê Linh, Phường Khai Quang, Thành phố Vĩnh Yên, Vĩnh Phúc', 30),
        ('dangngochan@gmail.com', 'Đặng Ngọc Hân', '21 Nguyễn Tất Thành, Phường Liên Bảo, Thành phố Vĩnh Yên, Vĩnh Phúc', 23),
        ('hoangbaolam@gmail.com', 'Hoàng Bảo Lâm', '10 Cù Chính Lan, Phường Đồng Tiến, Thành phố Hòa Bình, Hòa Bình', 26),
        ('ngophuongvy@gmail.com', 'Ngô Phương Vy', '29 Trần Hưng Đạo, Phường Phương Lâm, Thành phố Hòa Bình, Hòa Bình', 24),
        ('duongminhtri@gmail.com', 'Dương Minh Trí', '77 Lương Ngọc Quyến, Phường Hoàng Văn Thụ, Thành phố Thái Nguyên, Thái Nguyên', 28),
        ('lybaohan@gmail.com', 'Lý Bảo Hân', '16 Hoàng Văn Thụ, Phường Ngô Quyền, Thành phố Bắc Giang, Bắc Giang', 22),
        ('maithanhphong@gmail.com', 'Mai Thanh Phong', '5 Nguyễn Tất Thành, Phường Tân Dân, Thành phố Việt Trì, Phú Thọ', 27),
        ('phanquynhchi@gmail.com', 'Phan Quỳnh Chi', '24 Đinh Tiên Hoàng, Phường Đông Thành, Thành phố Ninh Bình, Ninh Bình', 25)
        ```

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
        ('[nguyenduongphu@gmail.com](mailto:nguyenduongphu@gmail.com)'),
        ('[tranminhchau@gmail.com](mailto:tranminhchau@gmail.com)'),
        ('[lebaonghi@gmail.com](mailto:lebaonghi@gmail.com)'),
        ('[phamkhanhduy@gmail.com](mailto:phamkhanhduy@gmail.com)'),
        ('[dohoanganhthu@gmail.com](mailto:dohoanganhthu@gmail.com)'),
        ('[buithienan@gmail.com](mailto:buithienan@gmail.com)'),
        ('[vominhnhat@gmail.com](mailto:vominhnhat@gmail.com)'),
        ('[danghuyenmy@gmail.com](mailto:danghuyenmy@gmail.com)'),
        ('[hoanggiakhang@gmail.com](mailto:hoanggiakhang@gmail.com)'),
        ('[ngocamly@gmail.com](mailto:ngocamly@gmail.com)'),
        ('[duongvietdung@gmail.com](mailto:duongvietdung@gmail.com)'),
        ('[lyngocdiep@gmail.com](mailto:lyngocdiep@gmail.com)'),
        ('[maianhquan@gmail.com](mailto:maianhquan@gmail.com)'),
        ('[phanhoaitam@gmail.com](mailto:phanhoaitam@gmail.com)'),
        ('[caotuanlinh@gmail.com](mailto:caotuanlinh@gmail.com)'),
        ('[trinhbaonguyen@gmail.com](mailto:trinhbaonguyen@gmail.com)'),
        ('[haanhthu@gmail.com](mailto:haanhthu@gmail.com)'),
        ('[nguyenhoangphuc@gmail.com](mailto:nguyenhoangphuc@gmail.com)'),
        ('[tranthuytien@gmail.com](mailto:tranthuytien@gmail.com)'),
        ('[lequangvinh@gmail.com](mailto:lequangvinh@gmail.com)'),
        ('[phamngoctram@gmail.com](mailto:phamngoctram@gmail.com)'),
        ('[dothienbao@gmail.com](mailto:dothienbao@gmail.com)'),
        ('[buithaochi@gmail.com](mailto:buithaochi@gmail.com)'),
        ('[vominhkhang@gmail.com](mailto:vominhkhang@gmail.com)'),
        ('[dangngochan@gmail.com](mailto:dangngochan@gmail.com)'),
        ('[hoangbaolam@gmail.com](mailto:hoangbaolam@gmail.com)'),
        ('[ngophuongvy@gmail.com](mailto:ngophuongvy@gmail.com)'),
        ('[duongminhtri@gmail.com](mailto:duongminhtri@gmail.com)'),
        ('[lybaohan@gmail.com](mailto:lybaohan@gmail.com)'),
        ('[maithanhphong@gmail.com](mailto:maithanhphong@gmail.com)'),
        ('[phanquynhchi@gmail.com](mailto:phanquynhchi@gmail.com)')
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
