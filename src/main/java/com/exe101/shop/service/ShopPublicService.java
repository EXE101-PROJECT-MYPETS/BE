package com.exe101.shop.service;

import com.exe101.product.repository.IProductRepository;
import com.exe101.review.repository.IReviewRepository;
import com.exe101.serviceReview.repository.IServiceReviewRepository;
import com.exe101.shop.dto.ShopMarkerDTO;
import com.exe101.shop.dto.ShopPublicContactDTO;
import com.exe101.shop.dto.ShopPublicDTO;
import com.exe101.shop.entity.Shop;
import com.exe101.shop.entity.ShopRole;
import com.exe101.shop.exception.ShopNotFound;
import com.exe101.shop.repository.IShopRepository;
import com.exe101.shopMember.dto.ShopMemberDTO;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.repository.IShopMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShopPublicService {

    private final IShopRepository shopRepository;
    private final IProductRepository productRepository;
    private final IReviewRepository reviewRepository;
    private final IServiceReviewRepository serviceReviewRepository;
    private final IShopMemberRepository shopMemberRepository;
    public List<ShopMarkerDTO> getAllMarkers() {
        List<Shop> shops = shopRepository.findAllByOrderByIdAsc();
        List<Long> shopIds = shops.stream()
                .map(Shop::getId)
                .toList();
        if (shopIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Double> productRatingByShopId = new HashMap<>();
        for (Object[] row : reviewRepository.aggregateRatingByShopIds(shopIds)) {
            Long ratedShopId = row[0] == null ? null : ((Number) row[0]).longValue();
            if (ratedShopId == null) {
                continue;
            }
            double rating = row[1] == null ? 0D : ((Number) row[1]).doubleValue();
            productRatingByShopId.put(ratedShopId, rating);
        }

        Map<Long, Double> serviceRatingByShopId = new HashMap<>();
        for (Object[] row : serviceReviewRepository.aggregateRatingByShopIds(shopIds)) {
            Long ratedShopId = row[0] == null ? null : ((Number) row[0]).longValue();
            if (ratedShopId == null) {
                continue;
            }
            double rating = row[1] == null ? 0D : ((Number) row[1]).doubleValue();
            serviceRatingByShopId.put(ratedShopId, rating);
        }

        return shops.stream()
                .map(shop -> new ShopMarkerDTO(
                        shop.getId(),
                        shop.getName(),
                        shop.getLat(),
                        shop.getLng(),
                        shop.getAddressText(),
                        shop.getImageUrl(),
                        calculateCombinedRating(
                                productRatingByShopId.getOrDefault(shop.getId(), 0D),
                                serviceRatingByShopId.getOrDefault(shop.getId(), 0D)
                        )
                ))
                .toList();
    }

    public ShopPublicDTO getById(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFound("ShopNotFound", "Không tìm thấy shop"));

        long productCount = productRepository.countByShopIdAndActiveTrue(shopId);
        double productAvgRating = extractAvgRating(reviewRepository.aggregateRatingAndTotalByShopId(shopId));
        double serviceAvgRating = extractAvgRating(serviceReviewRepository.aggregateRatingAndTotalByShopId(shopId));
        double rating = calculateCombinedRating(productAvgRating, serviceAvgRating);

        List<ShopMemberDTO> owners = shopMemberRepository.findByShopIdAndRoleAndStatusForDisplay(
                shopId,
                ShopRole.OWNER,
                MemberStatus.ACTIVE
        );
        ShopMemberDTO owner = owners.isEmpty() ? null : owners.get(0);

        return new ShopPublicDTO(
                shop.getId(),
                shop.getName(),
                shop.getImageUrl() != null ? shop.getImageUrl() : (owner != null ? owner.getAvatarUrlPreview() : null),
                shop.getCoverImageUrl(),
                rating,
                productCount,
                List.of(),
                shop.getAddressText(),
                new ShopPublicContactDTO(
                        owner != null ? owner.getUserFullName() : null,
                        owner != null ? owner.getUserPhone() : null,
                        owner != null ? owner.getUserEmail() : null
                )
        );
    }

    private double extractAvgRating(List<Object[]> rows) {
        Object[] stats = rows.isEmpty() ? null : rows.get(0);
        return stats == null || stats[0] == null
                ? 0D
                : ((Number) stats[0]).doubleValue();
    }

    private double calculateCombinedRating(double productAvgRating, double serviceAvgRating) {
        return (productAvgRating + serviceAvgRating) / 2D;
    }

}
