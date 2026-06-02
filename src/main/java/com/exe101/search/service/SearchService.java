package com.exe101.search.service;

import com.exe101.file.FileUploadUtil;
import com.exe101.search.dto.*;
import com.exe101.search.entity.SearchHistory;
import com.exe101.search.repository.ISearchHistoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_SUGGESTION_SIZE = 20;
    private static final int MAX_RECOMMENDED_SIZE = 20;
    private static final List<String> DEFAULT_SUGGESTED_KEYWORDS = List.of(
            "khách sạn thú cưng",
            "tắm chó",
            "khám thú y",
            "phụ kiện thú cưng"
    );

    private final EntityManager entityManager;
    private final ISearchHistoryRepository searchHistoryRepository;
    private final FileUploadUtil fileUploadUtil;

    @Transactional(readOnly = true)
    public SearchInitialResponse getInitial(Long userId, Double lat, Double lng, int recommendedSize) {
        validateLocationParams(lat, lng, null);
        int normalizedRecommendedSize = Math.min(Math.max(recommendedSize, 1), MAX_RECOMMENDED_SIZE);
        List<String> recentKeywords = userId == null ? List.of() : getRecentKeywords(userId);
        List<String> suggestedKeywords = getTrendingKeywords(8);
        if (suggestedKeywords.isEmpty()) {
            suggestedKeywords = DEFAULT_SUGGESTED_KEYWORDS;
        }
        List<SearchItemDTO> recommendedItems = searchRecommendedItems(lat, lng, normalizedRecommendedSize);
        return new SearchInitialResponse(recentKeywords, suggestedKeywords, recommendedItems);
    }

    @Transactional(readOnly = true)
    public SearchSuggestionsResponse getSuggestions(String keyword, Double lat, Double lng, Double radiusKm, int size) {
        validateLocationParams(lat, lng, radiusKm);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_SUGGESTION_SIZE);
        String normalizedKeyword = normalizeKeyword(keyword, false);
        if (normalizedKeyword == null) {
            return new SearchSuggestionsResponse(List.of());
        }
        Map<String, String> dedup = new LinkedHashMap<>();
        for (String value : getKeywordSuggestionsFromHistory(normalizedKeyword, normalizedSize)) {
            dedup.putIfAbsent(normalizeSearchKeyword(value), value);
        }
        if (dedup.size() < normalizedSize) {
            for (String value : getKeywordSuggestionsFromCatalog(normalizedKeyword, lat, lng, radiusKm, normalizedSize * 2)) {
                dedup.putIfAbsent(normalizeSearchKeyword(value), value);
                if (dedup.size() >= normalizedSize) {
                    break;
                }
            }
        }
        if (dedup.isEmpty() && radiusKm != null) {
            for (String value : getKeywordSuggestionsFromCatalog(normalizedKeyword, lat, lng, null, normalizedSize * 2)) {
                dedup.putIfAbsent(normalizeSearchKeyword(value), value);
                if (dedup.size() >= normalizedSize) {
                    break;
                }
            }
        }
        return new SearchSuggestionsResponse(dedup.values().stream().limit(normalizedSize).toList());
    }

    @Transactional(readOnly = true)
    public SearchPageResponse<SearchItemDTO> search(
            String keyword,
            SearchItemType type,
            Double lat,
            Double lng,
            Double radiusKm,
            SearchSortType sort,
            int page,
            int size
    ) {
        validatePaging(page, size);
        validateLocationParams(lat, lng, radiusKm);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = page * normalizedSize;
        SearchItemType normalizedType = type == null ? SearchItemType.ALL : type;
        SearchSortType normalizedSort = sort == null ? SearchSortType.RELEVANT : sort;
        String normalizedKeyword = normalizeKeyword(keyword, false);

        SqlPack sqlPack = buildSearchSql(normalizedType, normalizedSort, normalizedKeyword, lat, lng, radiusKm, offset, normalizedSize);
        SearchResult result = executeSearch(sqlPack);

        if (result.totalElements() == 0 && radiusKm != null) {
            SqlPack fallbackSqlPack = buildSearchSql(
                    normalizedType,
                    SearchSortType.NEAREST,
                    normalizedKeyword,
                    lat,
                    lng,
                    null,
                    offset,
                    normalizedSize
            );
            result = executeSearch(fallbackSqlPack);
        }

        List<SearchItemDTO> content = result.rows().stream().map(this::mapSearchItem).toList();
        long totalElements = result.totalElements();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / normalizedSize);
        boolean hasNext = (long) (page + 1) * normalizedSize < totalElements;

        return new SearchPageResponse<>(content, page, normalizedSize, totalElements, totalPages, hasNext);
    }

    @Transactional(readOnly = true)
    public SearchHistoryResponse getHistory(Long userId) {
        return new SearchHistoryResponse(getRecentKeywords(userId));
    }

    @Transactional
    public void saveHistory(Long userId, String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword, true);
        OffsetDateTime now = OffsetDateTime.now();
        Optional<SearchHistory> existed = searchHistoryRepository.findByUserIdAndKeywordIgnoreCase(userId, normalizedKeyword);
        if (existed.isPresent()) {
            SearchHistory entity = existed.get();
            entity.setKeyword(normalizedKeyword);
            entity.setSearchCount((entity.getSearchCount() == null ? 0 : entity.getSearchCount()) + 1);
            entity.setLastSearchedAt(now);
            searchHistoryRepository.save(entity);
            return;
        }
        SearchHistory entity = new SearchHistory();
        entity.setUserId(userId);
        entity.setKeyword(normalizedKeyword);
        entity.setSearchCount(1);
        entity.setCreatedAt(now);
        entity.setLastSearchedAt(now);
        searchHistoryRepository.save(entity);
    }

    @Transactional
    public void deleteHistory(Long userId, String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword, false);
        if (normalizedKeyword == null) {
            searchHistoryRepository.deleteByUserId(userId);
            return;
        }
        searchHistoryRepository.deleteByUserIdAndKeywordIgnoreCase(userId, normalizedKeyword);
    }

    private List<SearchItemDTO> searchRecommendedItems(Double lat, Double lng, int size) {
        String distanceExpr = buildDistanceExpr("sh", lat, lng);
        String distanceFilter = buildRadiusFilter(distanceExpr, lat, lng, null);
        String sql = """
                WITH items AS (
                    SELECT
                        p.id,
                        'PRODUCT' AS type,
                        p.name,
                        (
                            SELECT pi.image_url
                            FROM prod.product_images pi
                            WHERE pi.shop_id = p.shop_id
                              AND pi.product_id = p.id
                            ORDER BY pi.sort_order ASC, pi.id ASC
                            LIMIT 1
                        ) AS image,
                        p.price,
                        NULL::bigint AS original_price,
                        p.shop_id,
                        sh.name AS shop_name,
                        COALESCE(pr.avg_rating, 0) AS rating,
                        COALESCE(ps.sold_count, 0) AS sold_count,
                        sh.address_text AS address,
                        %s AS distance_km
                    FROM prod.products p
                    JOIN prod.shops sh
                        ON sh.id = p.shop_id
                    LEFT JOIN (
                        SELECT r.shop_id, r.product_id, AVG(r.rating) AS avg_rating
                        FROM prod.reviews r
                        GROUP BY r.shop_id, r.product_id
                    ) pr
                        ON pr.shop_id = p.shop_id
                       AND pr.product_id = p.id
                    LEFT JOIN (
                        SELECT oi.shop_id, oi.product_id, COALESCE(SUM(oi.qty), 0) AS sold_count
                        FROM prod.order_items oi
                        JOIN prod.orders o
                            ON o.shop_id = oi.shop_id
                           AND o.id = oi.order_id
                        WHERE o.status = 'COMPLETED'
                        GROUP BY oi.shop_id, oi.product_id
                    ) ps
                        ON ps.shop_id = p.shop_id
                       AND ps.product_id = p.id
                    WHERE p.active = true
                    %s
                    UNION ALL
                    SELECT
                        s.id,
                        'SERVICE' AS type,
                        s.name,
                        s.image_url AS image,
                        s.base_price AS price,
                        NULL::bigint AS original_price,
                        s.shop_id,
                        sh.name AS shop_name,
                        COALESCE(sr.avg_rating, 0) AS rating,
                        COALESCE(ss.sold_count, 0) AS sold_count,
                        sh.address_text AS address,
                        %s AS distance_km
                    FROM prod.services s
                    JOIN prod.shops sh
                        ON sh.id = s.shop_id
                    LEFT JOIN (
                        SELECT r.shop_id, r.service_id, AVG(r.rating) AS avg_rating
                        FROM prod.service_reviews r
                        GROUP BY r.shop_id, r.service_id
                    ) sr
                        ON sr.shop_id = s.shop_id
                       AND sr.service_id = s.id
                    LEFT JOIN (
                        SELECT bi.shop_id, bi.ref_id AS service_id, COALESCE(SUM(bi.qty), 0) AS sold_count
                        FROM prod.booking_items bi
                        JOIN prod.bookings b
                            ON b.shop_id = bi.shop_id
                           AND b.id = bi.booking_id
                        WHERE b.status = 'COMPLETED'
                          AND bi.item_type = 'SERVICE'
                        GROUP BY bi.shop_id, bi.ref_id
                    ) ss
                        ON ss.shop_id = s.shop_id
                       AND ss.service_id = s.id
                    WHERE s.active = true
                    %s
                )
                SELECT id, type, name, image, price, original_price, shop_id, shop_name, rating, sold_count, address, distance_km
                FROM items
                ORDER BY rating DESC, sold_count DESC, id DESC
                LIMIT :limit
                """.formatted(distanceExpr, distanceFilter, distanceExpr, distanceFilter);

        Query query = entityManager.createNativeQuery(sql);
        if (lat != null && lng != null) {
            query.setParameter("lat", lat);
            query.setParameter("lng", lng);
        }
        query.setParameter("limit", size);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(this::mapSearchItem).toList();
    }

    private SqlPack buildSearchSql(
            SearchItemType type,
            SearchSortType sort,
            String keyword,
            Double lat,
            Double lng,
            Double radiusKm,
            int offset,
            int limit
    ) {
        boolean hasKeyword = keyword != null;
        boolean hasLocation = lat != null && lng != null;
        boolean hasRadius = hasLocation && radiusKm != null;
        List<String> subQueries = new ArrayList<>();
        if (type == SearchItemType.ALL || type == SearchItemType.PRODUCT) {
            subQueries.add(buildProductSubQuery(hasKeyword, hasLocation, hasRadius));
        }
        if (type == SearchItemType.ALL || type == SearchItemType.SERVICE) {
            subQueries.add(buildServiceSubQuery(hasKeyword, hasLocation, hasRadius));
        }
        if (type == SearchItemType.SHOP) {
            subQueries.add(buildShopSubQuery(hasKeyword, hasLocation, hasRadius));
        }
        if (subQueries.isEmpty()) {
            throw new IllegalArgumentException("type không hợp lệ");
        }

        String unionSql = String.join("\nUNION ALL\n", subQueries);
        String orderBy = switch (sort) {
            case NEAREST -> "CASE WHEN distance_km IS NULL THEN 1 ELSE 0 END ASC, distance_km ASC, relevance_score DESC, rating DESC, id DESC";
            case PRICE_ASC -> "CASE WHEN price IS NULL THEN 1 ELSE 0 END ASC, price ASC, relevance_score DESC, rating DESC, id DESC";
            case PRICE_DESC -> "CASE WHEN price IS NULL THEN 1 ELSE 0 END ASC, price DESC, relevance_score DESC, rating DESC, id DESC";
            case RATING_DESC -> "rating DESC, sold_count DESC, relevance_score DESC, id DESC";
            case RELEVANT -> "relevance_score DESC, rating DESC, sold_count DESC, id DESC";
        };

        String baseSql = "WITH items AS (\n" + unionSql + "\n)\n";
        String contentSql = baseSql + """
                SELECT id, type, name, image, price, original_price, shop_id, shop_name, rating, sold_count, address, distance_km
                FROM items
                ORDER BY %s
                LIMIT :limit OFFSET :offset
                """.formatted(orderBy);
        String countSql = baseSql + "SELECT COUNT(*) FROM items";
        return new SqlPack(contentSql, countSql, hasKeyword, hasLocation, hasRadius, keyword, lat, lng, radiusKm, offset, limit);
    }

    private String buildProductSubQuery(boolean hasKeyword, boolean hasLocation, boolean hasRadius) {
        String keywordFilter = hasKeyword
                ? """
                AND unaccent(LOWER(p.name)) LIKE :keywordLike
                        """
                : "";
        String distanceExpr = buildDistanceExpr("sh", hasLocation);
        String radiusFilter = hasRadius ? "AND %s <= :radiusKm".formatted(distanceExpr) : "";
        String relevanceExpr = hasKeyword
                ? """
                        CASE
                    WHEN unaccent(LOWER(p.name)) = :keywordExact THEN 100
                    WHEN unaccent(LOWER(p.name)) LIKE :keywordPrefix THEN 90
                    WHEN unaccent(LOWER(p.name)) LIKE :keywordLike THEN 80
                            ELSE 10
                        END
                        """
                : "0";
        return """
                SELECT
                    p.id,
                    'PRODUCT' AS type,
                    p.name,
                    (
                        SELECT pi.image_url
                        FROM prod.product_images pi
                        WHERE pi.shop_id = p.shop_id
                          AND pi.product_id = p.id
                        ORDER BY pi.sort_order ASC, pi.id ASC
                        LIMIT 1
                    ) AS image,
                    p.price,
                    NULL::bigint AS original_price,
                    p.shop_id,
                    sh.name AS shop_name,
                    COALESCE(pr.avg_rating, 0) AS rating,
                    COALESCE(ps.sold_count, 0) AS sold_count,
                    sh.address_text AS address,
                    %s AS distance_km,
                    %s AS relevance_score
                FROM prod.products p
                JOIN prod.shops sh
                    ON sh.id = p.shop_id
                LEFT JOIN (
                    SELECT r.shop_id, r.product_id, AVG(r.rating) AS avg_rating
                    FROM prod.reviews r
                    GROUP BY r.shop_id, r.product_id
                ) pr
                    ON pr.shop_id = p.shop_id
                   AND pr.product_id = p.id
                LEFT JOIN (
                    SELECT oi.shop_id, oi.product_id, COALESCE(SUM(oi.qty), 0) AS sold_count
                    FROM prod.order_items oi
                    JOIN prod.orders o
                        ON o.shop_id = oi.shop_id
                       AND o.id = oi.order_id
                    WHERE o.status = 'COMPLETED'
                    GROUP BY oi.shop_id, oi.product_id
                ) ps
                    ON ps.shop_id = p.shop_id
                   AND ps.product_id = p.id
                WHERE p.active = true
                %s
                %s
                """.formatted(distanceExpr, relevanceExpr, keywordFilter, radiusFilter);
    }

    private String buildServiceSubQuery(boolean hasKeyword, boolean hasLocation, boolean hasRadius) {
        String keywordFilter = hasKeyword
                ? """
                AND unaccent(LOWER(s.name)) LIKE :keywordLike
                        """
                : "";
        String distanceExpr = buildDistanceExpr("sh", hasLocation);
        String radiusFilter = hasRadius ? "AND %s <= :radiusKm".formatted(distanceExpr) : "";
        String relevanceExpr = hasKeyword
                ? """
                        CASE
                    WHEN unaccent(LOWER(s.name)) = :keywordExact THEN 100
                    WHEN unaccent(LOWER(s.name)) LIKE :keywordPrefix THEN 90
                    WHEN unaccent(LOWER(s.name)) LIKE :keywordLike THEN 80
                            ELSE 10
                        END
                        """
                : "0";
        return """
                SELECT
                    s.id,
                    'SERVICE' AS type,
                    s.name,
                    s.image_url AS image,
                    s.base_price AS price,
                    NULL::bigint AS original_price,
                    s.shop_id,
                    sh.name AS shop_name,
                    COALESCE(sr.avg_rating, 0) AS rating,
                    COALESCE(ss.sold_count, 0) AS sold_count,
                    sh.address_text AS address,
                    %s AS distance_km,
                    %s AS relevance_score
                FROM prod.services s
                JOIN prod.shops sh
                    ON sh.id = s.shop_id
                LEFT JOIN (
                    SELECT r.shop_id, r.service_id, AVG(r.rating) AS avg_rating
                    FROM prod.service_reviews r
                    GROUP BY r.shop_id, r.service_id
                ) sr
                    ON sr.shop_id = s.shop_id
                   AND sr.service_id = s.id
                LEFT JOIN (
                    SELECT bi.shop_id, bi.ref_id AS service_id, COALESCE(SUM(bi.qty), 0) AS sold_count
                    FROM prod.booking_items bi
                    JOIN prod.bookings b
                        ON b.shop_id = bi.shop_id
                       AND b.id = bi.booking_id
                    WHERE b.status = 'COMPLETED'
                      AND bi.item_type = 'SERVICE'
                    GROUP BY bi.shop_id, bi.ref_id
                ) ss
                    ON ss.shop_id = s.shop_id
                   AND ss.service_id = s.id
                WHERE s.active = true
                %s
                %s
                """.formatted(distanceExpr, relevanceExpr, keywordFilter, radiusFilter);
    }

    private String buildShopSubQuery(boolean hasKeyword, boolean hasLocation, boolean hasRadius) {
        String keywordFilter = hasKeyword
                ? """
                        AND (
                            EXISTS (
                                SELECT 1
                                FROM prod.products p
                                WHERE p.shop_id = sh.id
                                  AND p.active = true
                          AND unaccent(LOWER(p.name)) LIKE :keywordLike
                            )
                            OR EXISTS (
                                SELECT 1
                                FROM prod.services s
                                WHERE s.shop_id = sh.id
                                  AND s.active = true
                          AND unaccent(LOWER(s.name)) LIKE :keywordLike
                            )
                        )
                        """
                : "";
        String distanceExpr = buildDistanceExpr("sh", hasLocation);
        String radiusFilter = hasRadius ? "AND %s <= :radiusKm".formatted(distanceExpr) : "";
        String relevanceExpr = hasKeyword
                ? """
                        CASE
                            WHEN EXISTS (
                                SELECT 1
                                FROM prod.products p
                                WHERE p.shop_id = sh.id
                                  AND p.active = true
                          AND unaccent(LOWER(p.name)) LIKE :keywordLike
                            ) THEN 80
                            WHEN EXISTS (
                                SELECT 1
                                FROM prod.services s
                                WHERE s.shop_id = sh.id
                                  AND s.active = true
                          AND unaccent(LOWER(s.name)) LIKE :keywordLike
                            ) THEN 70
                            ELSE 10
                        END
                        """
                : "0";
        return """
                SELECT
                    sh.id,
                    'SHOP' AS type,
                    sh.name,
                    sh.image_url AS image,
                    NULL::bigint AS price,
                    NULL::bigint AS original_price,
                    sh.id AS shop_id,
                    sh.name AS shop_name,
                    (COALESCE(pr.avg_rating, 0) + COALESCE(sr.avg_rating, 0)) / 2.0 AS rating,
                    COALESCE(pc.product_count, 0) AS sold_count,
                    sh.address_text AS address,
                    %s AS distance_km,
                    %s AS relevance_score
                FROM prod.shops sh
                LEFT JOIN (
                    SELECT r.shop_id, AVG(r.rating) AS avg_rating
                    FROM prod.reviews r
                    GROUP BY r.shop_id
                ) pr
                    ON pr.shop_id = sh.id
                LEFT JOIN (
                    SELECT r.shop_id, AVG(r.rating) AS avg_rating
                    FROM prod.service_reviews r
                    GROUP BY r.shop_id
                ) sr
                    ON sr.shop_id = sh.id
                LEFT JOIN (
                    SELECT p.shop_id, COUNT(*) AS product_count
                    FROM prod.products p
                    WHERE p.active = true
                    GROUP BY p.shop_id
                ) pc
                    ON pc.shop_id = sh.id
                WHERE sh.status = 'ACTIVE'
                %s
                %s
                """.formatted(distanceExpr, relevanceExpr, keywordFilter, radiusFilter);
    }

    private String buildDistanceExpr(String shopAlias, Double lat, Double lng) {
        return buildDistanceExpr(shopAlias, lat != null && lng != null);
    }

    private String buildDistanceExpr(String shopAlias, boolean hasLocation) {
        if (!hasLocation) {
            return "NULL";
        }
        return """
                (
                    6371 * ACOS(
                        LEAST(
                            1,
                            GREATEST(
                                -1,
                                COS(RADIANS(:lat)) * COS(RADIANS(%s.lat))
                                * COS(RADIANS(%s.lng) - RADIANS(:lng))
                                + SIN(RADIANS(:lat)) * SIN(RADIANS(%s.lat))
                            )
                        )
                    )
                )
                """.formatted(shopAlias, shopAlias, shopAlias);
    }

    private String buildRadiusFilter(String distanceExpr, Double lat, Double lng, Double radiusKm) {
        if (lat == null || lng == null || radiusKm == null) {
            return "";
        }
        return "AND %s <= :radiusKm".formatted(distanceExpr);
    }

    private SearchResult executeSearch(SqlPack sqlPack) {
        Query contentQuery = entityManager.createNativeQuery(sqlPack.contentSql());
        applySqlParameters(contentQuery, sqlPack, true);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = contentQuery.getResultList();

        Query countQuery = entityManager.createNativeQuery(sqlPack.countSql());
        applySqlParameters(countQuery, sqlPack, false);
        Number total = (Number) countQuery.getSingleResult();
        long totalElements = total == null ? 0L : total.longValue();
        return new SearchResult(rows, totalElements);
    }

    private void applySqlParameters(Query query, SqlPack sqlPack, boolean includePaging) {
        if (sqlPack.hasKeyword()) {
            String searchKeyword = normalizeSearchKeyword(sqlPack.keyword());
            query.setParameter("keywordExact", searchKeyword);
            query.setParameter("keywordPrefix", searchKeyword + "%");
            query.setParameter("keywordLike", "%" + searchKeyword + "%");
        }
        if (sqlPack.hasLocation()) {
            query.setParameter("lat", sqlPack.lat());
            query.setParameter("lng", sqlPack.lng());
        }
        if (sqlPack.hasRadius()) {
            query.setParameter("radiusKm", sqlPack.radiusKm());
        }
        if (includePaging) {
            query.setParameter("limit", sqlPack.limit());
            query.setParameter("offset", sqlPack.offset());
        }
    }

    private SearchItemDTO mapSearchItem(Object[] row) {
        Long id = toLong(row[0]);
        SearchItemType type = SearchItemType.valueOf((String) row[1]);
        String name = (String) row[2];
        String image = normalizeImage(type, (String) row[3]);
        Long price = toLong(row[4]);
        Long originalPrice = toLong(row[5]);
        Long shopId = toLong(row[6]);
        String shopName = (String) row[7];
        Double rating = toDouble(row[8]);
        Long soldCount = toLong(row[9]);
        String address = (String) row[10];
        Double distanceKm = toDouble(row[11]);
        return new SearchItemDTO(
                id,
                type,
                name,
                image,
                price,
                originalPrice,
                shopId,
                shopName,
                rating,
                soldCount,
                address,
                distanceKm
        );
    }

    private List<String> getRecentKeywords(Long userId) {
        return searchHistoryRepository.findTop20ByUserIdOrderByLastSearchedAtDescIdDesc(userId).stream()
                .map(SearchHistory::getKeyword)
                .toList();
    }

    private List<String> getTrendingKeywords(int limit) {
        Query query = entityManager.createNativeQuery("""
                SELECT MIN(s.keyword) AS keyword
                FROM prod.search_histories s
                GROUP BY LOWER(TRIM(s.keyword))
                ORDER BY SUM(s.search_count) DESC, MAX(s.last_searched_at) DESC
                LIMIT :limit
                """);
        query.setParameter("limit", limit);
        @SuppressWarnings("unchecked")
        List<Object> rows = query.getResultList();
        return rows.stream().map(String::valueOf).toList();
    }

    private List<String> getKeywordSuggestionsFromHistory(String keyword, int limit) {
        Query query = entityManager.createNativeQuery("""
                SELECT MIN(s.keyword) AS keyword
                FROM prod.search_histories s
                WHERE unaccent(LOWER(s.keyword)) LIKE :keywordLike
                GROUP BY LOWER(TRIM(s.keyword))
                ORDER BY SUM(s.search_count) DESC, MAX(s.last_searched_at) DESC
                LIMIT :limit
                """);
        query.setParameter("keywordLike", "%" + normalizeSearchKeyword(keyword) + "%");
        query.setParameter("limit", limit);
        @SuppressWarnings("unchecked")
        List<Object> rows = query.getResultList();
        return rows.stream().map(String::valueOf).toList();
    }

    private List<String> getKeywordSuggestionsFromCatalog(
            String keyword,
            Double lat,
            Double lng,
            Double radiusKm,
            int limit
    ) {
        boolean hasLocation = lat != null && lng != null;
        Query query;
        if (!hasLocation) {
            query = entityManager.createNativeQuery("""
                    SELECT value
                    FROM (
                        SELECT DISTINCT p.name AS value
                        FROM prod.products p
                        WHERE p.active = true
                          AND unaccent(LOWER(p.name)) LIKE :keywordLike
                        UNION
                        SELECT DISTINCT s.name AS value
                        FROM prod.services s
                        WHERE s.active = true
                          AND unaccent(LOWER(s.name)) LIKE :keywordLike
                    ) suggestion_values
                    ORDER BY value ASC
                    LIMIT :limit
                    """);
        } else {
            String distanceExpr = buildDistanceExpr("sh", true);
            String radiusFilter = buildRadiusFilter(distanceExpr, lat, lng, radiusKm);
            String sql = """
                    WITH candidate_values AS (
                        SELECT p.name AS value, %s AS distance_km
                        FROM prod.products p
                        JOIN prod.shops sh ON sh.id = p.shop_id
                        WHERE p.active = true
                          AND unaccent(LOWER(p.name)) LIKE :keywordLike
                        %s
                        UNION ALL
                        SELECT s.name AS value, %s AS distance_km
                        FROM prod.services s
                        JOIN prod.shops sh ON sh.id = s.shop_id
                        WHERE s.active = true
                          AND unaccent(LOWER(s.name)) LIKE :keywordLike
                        %s
                    ),
                    ranked AS (
                        SELECT value, MIN(distance_km) AS min_distance, COUNT(*) AS frequency
                        FROM candidate_values
                        GROUP BY value
                    )
                    SELECT value
                    FROM ranked
                    ORDER BY
                        CASE WHEN min_distance IS NULL THEN 1 ELSE 0 END ASC,
                        min_distance ASC,
                        frequency DESC,
                        value ASC
                    LIMIT :limit
                    """.formatted(distanceExpr, radiusFilter, distanceExpr, radiusFilter);
            query = entityManager.createNativeQuery(sql);
            query.setParameter("lat", lat);
            query.setParameter("lng", lng);
            if (radiusKm != null) {
                query.setParameter("radiusKm", radiusKm);
            }
        }
        query.setParameter("keywordLike", "%" + normalizeSearchKeyword(keyword) + "%");
        query.setParameter("limit", limit);
        @SuppressWarnings("unchecked")
        List<Object> rows = query.getResultList();
        return rows.stream().map(String::valueOf).toList();
    }

    private String normalizeKeyword(String keyword, boolean required) {
        if (keyword == null) {
            if (required) {
                throw new IllegalArgumentException("keyword không được để trống");
            }
            return null;
        }
        String normalized = keyword.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            if (required) {
                throw new IllegalArgumentException("keyword không được để trống");
            }
            return null;
        }
        if (normalized.length() > 255) {
            throw new IllegalArgumentException("keyword không được vượt quá 255 ký tự");
        }
        return normalized;
    }

    private String normalizeSearchKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }
        String normalized = Normalizer.normalize(keyword, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return normalized.toLowerCase(Locale.ROOT);
    }

    private void validatePaging(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page phải lớn hơn hoặc bằng 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size phải lớn hơn 0");
        }
    }

    private void validateLocationParams(Double lat, Double lng, Double radiusKm) {
        boolean hasLat = lat != null;
        boolean hasLng = lng != null;
        if (hasLat != hasLng) {
            throw new IllegalArgumentException("lat và lng phải được truyền cùng nhau");
        }
        if (!hasLat) {
            if (radiusKm != null) {
                throw new IllegalArgumentException("radiusKm chỉ hợp lệ khi có cả lat và lng");
            }
            return;
        }
        if (lat < -90D || lat > 90D) {
            throw new IllegalArgumentException("lat phải trong khoảng [-90, 90]");
        }
        if (lng < -180D || lng > 180D) {
            throw new IllegalArgumentException("lng phải trong khoảng [-180, 180]");
        }
        if (radiusKm != null && radiusKm <= 0D) {
            throw new IllegalArgumentException("radiusKm phải lớn hơn 0");
        }
    }

    private String normalizeImage(SearchItemType type, String image) {
        if (image == null) {
            return null;
        }
        if (type == SearchItemType.PRODUCT) {
            return fileUploadUtil.normalizeProductImagePath(image);
        }
        if (type == SearchItemType.SERVICE) {
            return fileUploadUtil.normalizeServiceImagePath(image);
        }
        return image;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).longValue();
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).doubleValue();
    }

    private record SqlPack(
            String contentSql,
            String countSql,
            boolean hasKeyword,
            boolean hasLocation,
            boolean hasRadius,
            String keyword,
            Double lat,
            Double lng,
            Double radiusKm,
            int offset,
            int limit
    ) {
    }

    private record SearchResult(
            List<Object[]> rows,
            long totalElements
    ) {
    }
}

