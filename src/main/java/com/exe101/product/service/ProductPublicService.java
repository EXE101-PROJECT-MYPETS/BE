package com.exe101.product.service;

import com.exe101.common.ScrollResponse;
import com.exe101.order.entity.OrderStatus;
import com.exe101.order.repository.IOrderItemRepository;
import com.exe101.product.dto.*;
import com.exe101.product.entity.Product;
import com.exe101.product.exception.ProductNotFound;
import com.exe101.product.repository.IProductRepository;
import com.exe101.review.repository.IReviewRepository;
import com.exe101.shop.dto.ShopPublicDTO;
import com.exe101.shop.service.ShopPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductPublicService {

    private static final int MAX_RELATED_SIZE = 20;
    private static final int MAX_REVIEW_SIZE = 100;

    private final ProductService productService;
    private final IProductRepository productRepository;
    private final IReviewRepository reviewRepository;
    private final IOrderItemRepository orderItemRepository;
    private final ShopPublicService shopPublicService;
    private final com.exe101.review.repository.IReviewReactionRepository reviewReactionRepository;

    private Long getCurrentUserId() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.exe101.auth.model.UserPrincipal userPrincipal) {
            return userPrincipal.getUser().getId();
        }
        return null;
    }

    public ScrollResponse<ProductDTO> getAllForMobile(
            String keyword,
            Boolean active,
            Long cursor,
            int size
    ) {
        return productService.getAllForMobileAllShops(keyword, active, cursor, size);
    }

    public ScrollResponse<ProductDTO> getAllForMobileByShop(
            Long shopId,
            String keyword,
            Boolean active,
            Long cursor,
            int size
    ) {
        return productService.getAllForMobile(shopId, keyword, active, cursor, size);
    }

    public ProductPublicDetailDTO getMobileProductDetail(Long productId) {
        ProductDTO dto = productService.getById(productId);
        ProductPublicCategoryDTO category = dto.getCategoryId() == null && dto.getCategoryName() == null
                ? null
                : new ProductPublicCategoryDTO(dto.getCategoryId(), dto.getCategoryName());
        ShopPublicDTO shop = shopPublicService.getById(dto.getShopId());

        Long soldCount = orderItemRepository.sumSoldQtyByShopAndProductIdAndOrderStatus(
                dto.getShopId(),
                dto.getId(),
                OrderStatus.COMPLETED
        );

        return new ProductPublicDetailDTO(
                dto.getId(),
                dto.getName(),
                dto.getPrice(),
                dto.getWeightKg(),
                null,
                category,
                dto.getShopId(),
                shop.getName(),
                shop.getImageUrl(),
                shop.getBadges() != null && !shop.getBadges().isEmpty(),
                shop.getRating(),
                shop.getProductCount(),
                shop.getAddress(),
                shop.getContact() != null ? shop.getContact().getName() : null,
                shop.getContact() != null ? shop.getContact().getPhone() : null,
                shop.getContact() != null ? shop.getContact().getEmail() : null,
                dto.getImageUrls(),
                dto.getReviewAvg(),
                dto.getReviewCount(),
                soldCount == null ? 0L : soldCount,
                dto.getStockQty(),
                dto.getUnit(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    public List<ProductDTO> getRelatedProducts(Long productId, int size) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFound("ProductNotFound", "Không tìm thấy sản phẩm"));
        int normalizedSize = Math.min(Math.max(size, 1), MAX_RELATED_SIZE);
        ScrollResponse<ProductDTO> scrollResponse = productService.getAllForMobile(
                product.getShopId(),
                null,
                true,
                null,
                normalizedSize + 1
        );
        return scrollResponse.getContent().stream()
                .filter(item -> !Objects.equals(item.getId(), productId))
                .limit(normalizedSize)
                .toList();
    }

    public List<ProductPublicReviewDTO> getProductReviews(Long productId, int size) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFound("ProductNotFound", "Không tìm thấy sản phẩm"));
        int normalizedSize = Math.min(Math.max(size, 1), MAX_REVIEW_SIZE);
        Long currentUserId = getCurrentUserId();
        
        return reviewRepository.findByShopIdAndProductIdOrderByIdDesc(product.getShopId(), productId).stream()
                .limit(normalizedSize)
                .map(review -> {
                    long likes = reviewReactionRepository.countByReviewIdAndIsLike(review.getId(), true);
                    long dislikes = reviewReactionRepository.countByReviewIdAndIsLike(review.getId(), false);
                    String userReaction = null;
                    if (currentUserId != null) {
                        userReaction = reviewReactionRepository.findByReviewIdAndUserId(review.getId(), currentUserId)
                                .map(react -> react.getIsLike() ? "LIKE" : "DISLIKE")
                                .orElse(null);
                    }
                    
                    ProductPublicReviewDTO reviewDto = new ProductPublicReviewDTO();
                    reviewDto.setId(review.getId());
                    reviewDto.setStar(review.getRating());
                    reviewDto.setContent(review.getComment());
                    reviewDto.setImageUrls(List.of());
                    reviewDto.setUser(new ProductPublicReviewUserDTO(
                            review.getCustomerId(),
                            review.getCustomer() != null ? review.getCustomer().getFullName() : null
                    ));
                    reviewDto.setDate(review.getCreatedAt());
                    reviewDto.setUsefulCount(likes);
                    reviewDto.setReply(review.getReply());
                    reviewDto.setLikeCount(likes);
                    reviewDto.setDislikeCount(dislikes);
                    reviewDto.setUserReaction(userReaction);
                    return reviewDto;
                })
                .toList();
    }
}
