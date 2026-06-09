import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SeedServiceReviews {

    private record ServiceSeed(long serviceId, String[] comments, int[] ratings) {
    }

    public static void main(String[] args) throws Exception {
        String dbUrl = requireEnv("DB_URL");
        String dbUser = requireEnv("DB_USER");
        String dbPass = requireEnv("DB_PASS");
        String dbSchema = System.getenv().getOrDefault("DB_SCHEMA", "prod");

        List<ServiceSeed> seeds = List.of(
                new ServiceSeed(
                        65L,
                        new String[]{
                                "Dịch vụ rất tốt, nhân viên tư vấn kỹ và thao tác cẩn thận.",
                                "Trải nghiệm ổn, thú cưng được chăm sóc nhẹ nhàng và sạch sẽ.",
                                "Rất hài lòng với chất lượng dịch vụ, sẽ quay lại lần sau."
                        },
                        new int[]{5, 4, 5}
                ),
                new ServiceSeed(
                        71L,
                        new String[]{
                                "Bác sĩ kiểm tra kỹ, giải thích rõ tình trạng của thú cưng.",
                                "Dịch vụ tốt, quy trình nhanh gọn và thái độ phục vụ ổn.",
                                "Shop chăm sóc rất chuyên nghiệp, mình hoàn toàn yên tâm."
                        },
                        new int[]{5, 4, 5}
                )
        );

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            connection.setAutoCommit(false);
            setSearchPath(connection, dbSchema);

            for (ServiceSeed seed : seeds) {
                Long shopId = findShopId(connection, seed.serviceId());
                if (shopId == null) {
                    throw new IllegalStateException("Khong tim thay service_id=" + seed.serviceId());
                }
                List<Long> customerIds = ensureCustomers(connection, shopId);
                upsertReviews(connection, shopId, seed, customerIds);
                System.out.println("Seeded reviews for service_id=" + seed.serviceId() + ", shop_id=" + shopId);
            }

            connection.commit();
        }
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing env: " + name);
        }
        return value;
    }

    private static void setSearchPath(Connection connection, String schema) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SET search_path TO " + schema + ", public")) {
            statement.execute();
        }
    }

    private static Long findShopId(Connection connection, long serviceId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT shop_id FROM services WHERE id = ?"
        )) {
            statement.setLong(1, serviceId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return null;
            }
        }
    }

    private static List<Long> ensureCustomers(Connection connection, long shopId) throws SQLException {
        String[][] customers = {
                {"Nguyễn Hoàng Minh", "0906500071", "nguyen.hoang.minh.service.review@example.com"},
                {"Trần Bảo Ngọc", "0906500072", "tran.bao.ngoc.service.review@example.com"},
                {"Lê Gia Huy", "0906500073", "le.gia.huy.service.review@example.com"}
        };

        List<Long> customerIds = new ArrayList<>();
        for (String[] customer : customers) {
            try (PreparedStatement upsert = connection.prepareStatement("""
                    INSERT INTO customers (shop_id, full_name, phone, email)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (shop_id, phone) WHERE phone IS NOT NULL DO UPDATE SET
                        full_name = EXCLUDED.full_name,
                        email = EXCLUDED.email
                    """)) {
                upsert.setLong(1, shopId);
                upsert.setString(2, customer[0]);
                upsert.setString(3, customer[1]);
                upsert.setString(4, customer[2]);
                upsert.executeUpdate();
            }

            try (PreparedStatement select = connection.prepareStatement("""
                    SELECT id
                    FROM customers
                    WHERE shop_id = ? AND phone = ?
                    """)) {
                select.setLong(1, shopId);
                select.setString(2, customer[1]);
                try (ResultSet rs = select.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalStateException("Khong tim thay customer sau khi upsert: " + customer[1]);
                    }
                    customerIds.add(rs.getLong(1));
                }
            }
        }
        return customerIds;
    }

    private static void upsertReviews(
            Connection connection,
            long shopId,
            ServiceSeed seed,
            List<Long> customerIds
    ) throws SQLException {
        for (int i = 0; i < customerIds.size(); i++) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO service_reviews (
                        shop_id,
                        service_id,
                        customer_id,
                        rating,
                        comment,
                        created_at
                    )
                    VALUES (?, ?, ?, ?, ?, now() - (? * interval '1 day'))
                    ON CONFLICT (shop_id, service_id, customer_id) DO UPDATE SET
                        rating = EXCLUDED.rating,
                        comment = EXCLUDED.comment,
                        updated_at = now()
                    """)) {
                statement.setLong(1, shopId);
                statement.setLong(2, seed.serviceId());
                statement.setLong(3, customerIds.get(i));
                statement.setInt(4, seed.ratings()[i]);
                statement.setString(5, seed.comments()[i]);
                statement.setInt(6, i + 1);
                statement.executeUpdate();
            }
        }
    }
}
