package com.exe101.service_shop.service;

import com.exe101.common.ScrollResponse;
import com.exe101.file.FileUploadUtil;
import com.exe101.serviceReview.repository.IServiceReviewRepository;
import com.exe101.service_shop.dto.ServicePublicDTO;
import com.exe101.service_shop.entity.Service;
import com.exe101.service_shop.entity.ServiceType;
import com.exe101.service_shop.repository.IServiceRepository;
import com.exe101.shop.entity.Shop;
import com.exe101.shop.repository.IShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServicePublicService {

    private static final int MAX_SCROLL_SIZE = 50;
    private static final int MAX_PER_SHOP_LIMIT = 20;

    private final IServiceRepository serviceRepository;
    private final IServiceReviewRepository serviceReviewRepository;
    private final IShopRepository shopRepository;
    private final FileUploadUtil fileUploadUtil;

    public ScrollResponse<ServicePublicDTO> getAllForScroll(
            Long shopId,
            String search,
            Long categoryId,
            Boolean active,
            Double minRating,
            Double lat,
            Double lng,
            Double radiusKm,
            int perShopLimit,
            Long cursor,
            int size
    ) {
        return getAllForScrollByServiceType(
                ServiceType.GENERAL,
                shopId,
                search,
                categoryId,
                active,
                minRating,
                lat,
                lng,
                radiusKm,
                perShopLimit,
                cursor,
                size
        );
    }

    public ScrollResponse<ServicePublicDTO> getVeterinaryServicesForScroll(
            Long shopId,
            String search,
            Long categoryId,
            Boolean active,
            Double minRating,
            Double lat,
            Double lng,
            Double radiusKm,
            int perShopLimit,
            Long cursor,
            int size
    ) {
        return getAllForScrollByServiceType(
                ServiceType.VETERINARY,
                shopId,
                search,
                categoryId,
                active,
                minRating,
                lat,
                lng,
                radiusKm,
                perShopLimit,
                cursor,
                size
        );
    }

    private ScrollResponse<ServicePublicDTO> getAllForScrollByServiceType(
            ServiceType serviceType,
            Long shopId,
            String search,
            Long categoryId,
            Boolean active,
            Double minRating,
            Double lat,
            Double lng,
            Double radiusKm,
            int perShopLimit,
            Long cursor,
            int size
    ) {
        int normalizedSize = Math.min(Math.max(size, 1), MAX_SCROLL_SIZE);
        int normalizedPerShopLimit = Math.min(Math.max(perShopLimit, 1), MAX_PER_SHOP_LIMIT);
        Long normalizedCursor = cursor != null && cursor > 0 ? cursor : null;
        String normalizedSearch = StringUtils.hasText(search) ? search.trim() : null;
        validateLocationParams(lat, lng, radiusKm);
        List<Service> services;
        if (lat != null) {
            List<Long> orderedServiceIds = serviceRepository.findIdsForPublicOrderByDistanceThenRating(
                    shopId,
                    normalizedSearch,
                    categoryId,
                    serviceType == null ? null : serviceType.name(),
                    active,
                    minRating,
                    normalizedCursor,
                    lat,
                    lng,
                    normalizedPerShopLimit,
                    normalizedSize + 1
            );
            if (orderedServiceIds.isEmpty()) {
                services = List.of();
            } else {
                Map<Long, Service> serviceById = new HashMap<>();
                for (Service service : serviceRepository.findAllById(orderedServiceIds)) {
                    serviceById.put(service.getId(), service);
                }
                services = new ArrayList<>();
                for (Long serviceId : orderedServiceIds) {
                    Service service = serviceById.get(serviceId);
                    if (service != null) {
                        services.add(service);
                    }
                }
            }
        } else {
            services = serviceRepository.findTopRatedForPublic(
                    shopId,
                    normalizedSearch,
                    categoryId,
                    serviceType,
                    active,
                    minRating,
                    normalizedCursor,
                    PageRequest.of(0, normalizedSize + 1)
            );
        }

        boolean hasNext = services.size() > normalizedSize;
        List<Service> content = services.stream()
                .limit(normalizedSize)
                .toList();
        Long nextCursor = hasNext && !content.isEmpty()
                ? content.get(content.size() - 1).getId()
                : null;

        return ScrollResponse.of(
                toPublicDTOs(content, lat, lng),
                normalizedSize,
                nextCursor,
                hasNext
        );
    }

    private void validateLocationParams(Double lat, Double lng, Double radiusKm) {
        boolean hasLat = lat != null;
        boolean hasLng = lng != null;
        if (hasLat != hasLng) {
            throw new IllegalArgumentException("lat và lng phải được truyền cùng nhau");
        }
        if (!hasLat) {
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

    private List<ServicePublicDTO> toPublicDTOs(List<Service> services, Double userLat, Double userLng) {
        if (services.isEmpty()) {
            return List.of();
        }

        List<Long> serviceIds = services.stream()
                .map(Service::getId)
                .toList();
        Map<Long, RatingStats> ratingByServiceId = loadRatingByServiceId(serviceIds);
        Map<Long, ShopSnapshot> shopById = loadShopById(services);

        return services.stream()
                .map(service -> {
                    RatingStats stats = ratingByServiceId.getOrDefault(service.getId(), RatingStats.ZERO);
                    ShopSnapshot shop = shopById.getOrDefault(service.getShopId(), ShopSnapshot.EMPTY);
                    Double distanceKm = calculateDistanceKm(userLat, userLng, shop.lat(), shop.lng());
                    return new ServicePublicDTO(
                            new ServicePublicDTO.ServiceInfoDTO(
                                    service.getId(),
                                    service.getName(),
                                    service.getDurationMin(),
                                    service.getBasePrice(),
                                    service.getCategoryId(),
                                    service.getServiceType(),
                                    service.getVeterinaryServiceType(),
                                    service.getVaccineId(),
                                    service.getVaccine() != null ? service.getVaccine().getName() : null,
                                    fileUploadUtil.normalizeServiceImagePath(service.getImageUrl()),
                                    service.getActive(),
                                    stats.avgRating(),
                                    stats.ratingCount()
                            ),
                            new ServicePublicDTO.ShopInfoDTO(
                                    service.getShopId(),
                                    shop.name(),
                                    shop.imageUrl(),
                                    shop.address(),
                                    shop.province(),
                                    shop.lat(),
                                    shop.lng()
                            ),
                            distanceKm
                    );
                })
                .toList();
    }

    private Map<Long, ShopSnapshot> loadShopById(List<Service> services) {
        List<Long> shopIds = services.stream()
                .map(Service::getShopId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (shopIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, ShopSnapshot> result = new HashMap<>();
        for (Shop shop : shopRepository.findAllById(shopIds)) {
            result.put(
                    shop.getId(),
                    new ShopSnapshot(
                            shop.getName(),
                            fileUploadUtil.normalizeServiceImagePath(shop.getImageUrl()),
                            shop.getAddressText(),
                            extractProvince(shop.getAddressText()),
                            shop.getLat(),
                            shop.getLng()
                    )
            );
        }
        return result;
    }

    private String extractProvince(String addressText) {
        if (!StringUtils.hasText(addressText)) {
            return null;
        }
        String normalized = addressText.trim();
        int lastComma = normalized.lastIndexOf(',');
        if (lastComma < 0) {
            return normalized;
        }
        String province = normalized.substring(lastComma + 1).trim();
        return province.isEmpty() ? normalized : province;
    }

    private Map<Long, RatingStats> loadRatingByServiceId(List<Long> serviceIds) {
        if (serviceIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, RatingStats> result = new HashMap<>();
        for (Object[] row : serviceReviewRepository.aggregateRatingAndTotalByServiceIds(serviceIds)) {
            Long serviceId = row[0] == null ? null : ((Number) row[0]).longValue();
            if (serviceId == null) {
                continue;
            }
            double avg = row[1] == null ? 0D : ((Number) row[1]).doubleValue();
            long total = row[2] == null ? 0L : ((Number) row[2]).longValue();
            result.put(serviceId, new RatingStats(avg, total));
        }
        return result;
    }

    private Double calculateDistanceKm(Double userLat, Double userLng, Double shopLat, Double shopLng) {
        if (userLat == null || userLng == null || shopLat == null || shopLng == null) {
            return null;
        }
        final double earthRadiusKm = 6371D;
        double latDistanceRad = Math.toRadians(shopLat - userLat);
        double lngDistanceRad = Math.toRadians(shopLng - userLng);
        double a = Math.sin(latDistanceRad / 2) * Math.sin(latDistanceRad / 2)
                + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(shopLat))
                * Math.sin(lngDistanceRad / 2) * Math.sin(lngDistanceRad / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private record RatingStats(double avgRating, long ratingCount) {
        private static final RatingStats ZERO = new RatingStats(0D, 0L);
    }

    private record ShopSnapshot(
            String name,
            String imageUrl,
            String address,
            String province,
            Double lat,
            Double lng
    ) {
        private static final ShopSnapshot EMPTY = new ShopSnapshot(null, null, null, null, null, null);
    }
}
