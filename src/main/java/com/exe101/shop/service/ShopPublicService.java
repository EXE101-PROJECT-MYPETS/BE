package com.exe101.shop.service;

import com.exe101.product.repository.IProductRepository;
import com.exe101.review.repository.IReviewRepository;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopPublicService {

    private final IShopRepository shopRepository;
    private final IProductRepository productRepository;
    private final IReviewRepository reviewRepository;
    private final IShopMemberRepository shopMemberRepository;

    public ShopPublicDTO getById(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFound("ShopNotFound", "Không tìm thấy shop"));

        long productCount = productRepository.countByShopIdAndActiveTrue(shopId);
        List<Object[]> shopRatingStatsRows = reviewRepository.aggregateRatingAndTotalByShopId(shopId);
        Object[] shopRatingStats = shopRatingStatsRows.isEmpty() ? null : shopRatingStatsRows.get(0);
        double rating = shopRatingStats == null || shopRatingStats[0] == null
                ? 0.0
                : ((Number) shopRatingStats[0]).doubleValue();

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
}
