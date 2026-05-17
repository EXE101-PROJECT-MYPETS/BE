package com.exe101.dashboard.service;

import com.exe101.dashboard.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopOwnerDashboardService {

    private static final ZoneId DASHBOARD_ZONE = ZoneId.of("Asia/Bangkok");
    private static final long LOW_STOCK_THRESHOLD = 5L;

    private final JdbcTemplate jdbcTemplate;

    public DashboardSummaryDTO getSummary(Long shopId, int year, int month) {
        OffsetDateTime monthStart = startOfMonth(year, month);
        OffsetDateTime nextMonthStart = monthStart.plusMonths(1);
        OffsetDateTime yearStart = startOfYear(year);
        OffsetDateTime nextYearStart = yearStart.plusYears(1);
        OffsetDateTime todayStart = LocalDate.now(DASHBOARD_ZONE).atStartOfDay(DASHBOARD_ZONE).toOffsetDateTime();
        OffsetDateTime tomorrowStart = todayStart.plusDays(1);

        Long monthlyRevenue = queryRevenue(shopId, monthStart, nextMonthStart);
        Long monthlyOrderCount = queryOrderCount(shopId, monthStart, nextMonthStart);
        Long yearlyRevenue = queryRevenue(shopId, yearStart, nextYearStart);
        Long todayRevenue = queryRevenue(shopId, todayStart, tomorrowStart);

        return new DashboardSummaryDTO(monthlyRevenue, monthlyOrderCount, yearlyRevenue, todayRevenue);
    }

    public List<MonthlyRevenueDTO> getRevenueByMonth(Long shopId, int year) {
        OffsetDateTime yearStart = startOfYear(year);
        OffsetDateTime nextYearStart = yearStart.plusYears(1);
        List<MonthlyRevenueDTO> result = createEmptyRevenueMonths();

        jdbcTemplate.query("""
                        SELECT EXTRACT(MONTH FROM COALESCE(issued_at, created_at))::int AS month,
                               COALESCE(SUM(total_amount), 0)::bigint AS revenue
                        FROM prod.invoices
                        WHERE shop_id = ?
                          AND status = 'PAID'
                          AND COALESCE(issued_at, created_at) >= ?
                          AND COALESCE(issued_at, created_at) < ?
                        GROUP BY EXTRACT(MONTH FROM COALESCE(issued_at, created_at))::int
                        ORDER BY month
                        """,
                ps -> {
                    ps.setLong(1, shopId);
                    ps.setTimestamp(2, toTimestamp(yearStart));
                    ps.setTimestamp(3, toTimestamp(nextYearStart));
                },
                rs -> {
                    int month = rs.getInt("month");
                    result.set(month - 1, new MonthlyRevenueDTO(month, rs.getLong("revenue")));
                });

        return result;
    }

    public List<MonthlyOrderDTO> getOrdersByMonth(Long shopId, int year) {
        OffsetDateTime yearStart = startOfYear(year);
        OffsetDateTime nextYearStart = yearStart.plusYears(1);
        List<MonthlyOrderDTO> result = createEmptyOrderMonths();

        jdbcTemplate.query("""
                        SELECT EXTRACT(MONTH FROM created_at)::int AS month,
                               COUNT(*)::bigint AS order_count
                        FROM prod.orders
                        WHERE shop_id = ?
                          AND status <> 'CANCELLED'
                          AND created_at >= ?
                          AND created_at < ?
                        GROUP BY EXTRACT(MONTH FROM created_at)::int
                        ORDER BY month
                        """,
                ps -> {
                    ps.setLong(1, shopId);
                    ps.setTimestamp(2, toTimestamp(yearStart));
                    ps.setTimestamp(3, toTimestamp(nextYearStart));
                },
                rs -> {
                    int month = rs.getInt("month");
                    result.set(month - 1, new MonthlyOrderDTO(month, rs.getLong("order_count")));
                });

        return result;
    }

    public List<BookingCategoryStatDTO> getBookingsByCategory(Long shopId, int year, Integer month) {
        OffsetDateTime from = month == null ? startOfYear(year) : startOfMonth(year, month);
        OffsetDateTime to = month == null ? from.plusYears(1) : from.plusMonths(1);

        return jdbcTemplate.query("""
                        SELECT COALESCE(sc.id::text, 'UNCATEGORIZED') AS category_id,
                               COALESCE(sc.name, 'Chưa phân loại') AS category_name,
                               COALESCE(SUM(bi.qty), 0)::bigint AS booking_count
                        FROM prod.booking_items bi
                        JOIN prod.bookings b
                          ON b.shop_id = bi.shop_id
                         AND b.id = bi.booking_id
                        JOIN prod.services s
                          ON s.shop_id = bi.shop_id
                         AND s.id = bi.ref_id
                        LEFT JOIN prod.service_categories sc
                          ON sc.shop_id = s.shop_id
                         AND sc.id = s.category_id
                        WHERE bi.shop_id = ?
                          AND bi.item_type = 'SERVICE'
                          AND b.status NOT IN ('CANCELLED', 'REJECTED')
                          AND b.start_at >= ?
                          AND b.start_at < ?
                        GROUP BY sc.id, sc.name
                        ORDER BY booking_count DESC, category_name ASC
                        """,
                (rs, rowNum) -> new BookingCategoryStatDTO(
                        rs.getString("category_id"),
                        rs.getString("category_name"),
                        rs.getLong("booking_count")
                ),
                shopId,
                toTimestamp(from),
                toTimestamp(to));
    }

    public List<ServiceCategoryStatDTO> getServicesByCategory(Long shopId) {
        return jdbcTemplate.query("""
                        SELECT COALESCE(sc.id::text, 'UNCATEGORIZED') AS category_id,
                               COALESCE(sc.name, 'Chưa phân loại') AS category_name,
                               COUNT(s.id)::bigint AS service_count
                        FROM prod.services s
                        LEFT JOIN prod.service_categories sc
                          ON sc.shop_id = s.shop_id
                         AND sc.id = s.category_id
                        WHERE s.shop_id = ?
                          AND s.active = true
                        GROUP BY sc.id, sc.name
                        ORDER BY service_count DESC, category_name ASC
                        """,
                (rs, rowNum) -> new ServiceCategoryStatDTO(
                        rs.getString("category_id"),
                        rs.getString("category_name"),
                        rs.getLong("service_count")
                ),
                shopId);
    }

    public InventoryStatusDTO getInventoryStatus(Long shopId) {
        return jdbcTemplate.queryForObject("""
                        SELECT
                            COALESCE(SUM(CASE WHEN GREATEST(i.on_hand - i.reserved, 0) > ? THEN 1 ELSE 0 END), 0)::bigint AS in_stock_count,
                            COALESCE(SUM(CASE WHEN GREATEST(i.on_hand - i.reserved, 0) BETWEEN 1 AND ? THEN 1 ELSE 0 END), 0)::bigint AS low_stock_count,
                            COALESCE(SUM(CASE WHEN GREATEST(i.on_hand - i.reserved, 0) = 0 THEN 1 ELSE 0 END), 0)::bigint AS out_of_stock_count
                        FROM prod.inventory i
                        JOIN prod.products p
                          ON p.shop_id = i.shop_id
                         AND p.id = i.product_id
                        WHERE i.shop_id = ?
                          AND p.active = true
                        """,
                (rs, rowNum) -> new InventoryStatusDTO(
                        rs.getLong("in_stock_count"),
                        rs.getLong("low_stock_count"),
                        rs.getLong("out_of_stock_count")
                ),
                LOW_STOCK_THRESHOLD,
                LOW_STOCK_THRESHOLD,
                shopId);
    }

    public InventoryAlertDashboardDTO getInventoryAlerts(Long shopId, int limit) {
        int normalizedLimit = Math.min(Math.max(limit, 1), 50);
        Long reorderPoint = LOW_STOCK_THRESHOLD;

        InventoryAlertSummaryDTO summary = jdbcTemplate.queryForObject("""
                        WITH product_alerts AS (
                            SELECT i.on_hand - i.reserved AS available
                            FROM prod.inventory i
                            JOIN prod.products p
                              ON p.shop_id = i.shop_id
                             AND p.id = i.product_id
                            WHERE i.shop_id = ?
                              AND p.active = true
                        )
                        SELECT
                            COALESCE(SUM(CASE WHEN available <= ? THEN 1 ELSE 0 END), 0)::bigint AS out_of_stock_count,
                            COALESCE(SUM(CASE WHEN available BETWEEN 1 AND ? THEN 1 ELSE 0 END), 0)::bigint AS low_stock_count,
                            COALESCE(SUM(CASE WHEN available <= ? THEN 1 WHEN available BETWEEN 1 AND ? THEN 1 ELSE 0 END), 0)::bigint AS total_alert_count
                        FROM product_alerts
                        """,
                (rs, rowNum) -> new InventoryAlertSummaryDTO(
                        rs.getLong("total_alert_count"),
                        rs.getLong("low_stock_count"),
                        rs.getLong("out_of_stock_count")
                ),
                shopId,
                0L,
                reorderPoint,
                0L,
                reorderPoint);

        List<InventoryAlertItemDTO> items = jdbcTemplate.query("""
                        SELECT
                            p.id::text AS item_id,
                            'PRODUCT' AS item_type,
                            p.name AS item_name,
                            pi.image_url,
                            p.sku,
                            i.on_hand,
                            i.reserved,
                            i.on_hand - i.reserved AS available,
                            ?::bigint AS reorder_point,
                            CASE
                                WHEN i.on_hand - i.reserved <= 0 THEN 'OUT_OF_STOCK'
                                ELSE 'LOW_STOCK'
                            END AS status
                        FROM prod.inventory i
                        JOIN prod.products p
                          ON p.shop_id = i.shop_id
                         AND p.id = i.product_id
                        LEFT JOIN LATERAL (
                            SELECT image_url
                            FROM prod.product_images
                            WHERE shop_id = p.shop_id
                              AND product_id = p.id
                            ORDER BY sort_order ASC, id ASC
                            LIMIT 1
                        ) pi ON true
                        WHERE i.shop_id = ?
                          AND p.active = true
                          AND i.on_hand - i.reserved <= ?
                        ORDER BY
                            CASE WHEN i.on_hand - i.reserved <= 0 THEN 0 ELSE 1 END ASC,
                            i.on_hand - i.reserved ASC,
                            p.name ASC
                        LIMIT ?
                        """,
                (rs, rowNum) -> new InventoryAlertItemDTO(
                        rs.getString("item_id"),
                        rs.getString("item_type"),
                        rs.getString("item_name"),
                        rs.getString("image_url"),
                        rs.getString("sku"),
                        rs.getLong("on_hand"),
                        rs.getLong("reserved"),
                        rs.getLong("available"),
                        rs.getLong("reorder_point"),
                        rs.getString("status")
                ),
                reorderPoint,
                shopId,
                reorderPoint,
                normalizedLimit);

        return new InventoryAlertDashboardDTO(summary, items);
    }

    private Long queryRevenue(Long shopId, OffsetDateTime from, OffsetDateTime to) {
        return jdbcTemplate.queryForObject("""
                        SELECT COALESCE(SUM(total_amount), 0)::bigint
                        FROM prod.invoices
                        WHERE shop_id = ?
                          AND status = 'PAID'
                          AND COALESCE(issued_at, created_at) >= ?
                          AND COALESCE(issued_at, created_at) < ?
                        """,
                Long.class,
                shopId,
                toTimestamp(from),
                toTimestamp(to));
    }

    private Long queryOrderCount(Long shopId, OffsetDateTime from, OffsetDateTime to) {
        return jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)::bigint
                        FROM prod.orders
                        WHERE shop_id = ?
                          AND status <> 'CANCELLED'
                          AND created_at >= ?
                          AND created_at < ?
                        """,
                Long.class,
                shopId,
                toTimestamp(from),
                toTimestamp(to));
    }

    private List<MonthlyRevenueDTO> createEmptyRevenueMonths() {
        List<MonthlyRevenueDTO> months = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            months.add(new MonthlyRevenueDTO(month, 0L));
        }
        return months;
    }

    private List<MonthlyOrderDTO> createEmptyOrderMonths() {
        List<MonthlyOrderDTO> months = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            months.add(new MonthlyOrderDTO(month, 0L));
        }
        return months;
    }

    private OffsetDateTime startOfYear(int year) {
        return LocalDate.of(year, 1, 1).atStartOfDay(DASHBOARD_ZONE).toOffsetDateTime();
    }

    private OffsetDateTime startOfMonth(int year, int month) {
        return LocalDate.of(year, month, 1).atStartOfDay(DASHBOARD_ZONE).toOffsetDateTime();
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return Timestamp.from(value.toInstant());
    }
}
