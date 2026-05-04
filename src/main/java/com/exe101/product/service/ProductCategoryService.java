package com.exe101.product.service;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.product.dto.ProductCategoryDTO;
import com.exe101.product.entity.ProductCategory;
import com.exe101.product.exception.ProductAccessDenied;
import com.exe101.product.exception.ProductCategoryDuplicate;
import com.exe101.product.exception.ProductCategoryNotFound;
import com.exe101.product.mapper.ProductCategoryMapper;
import com.exe101.product.repository.IProductCategoryRepository;
import com.exe101.shop.entity.ShopRole;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.repository.IShopMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductCategoryService {

    private static final List<ShopRole> CATEGORY_MANAGE_ROLES = List.of(ShopRole.OWNER, ShopRole.MANAGER);

    private final IProductCategoryRepository productCategoryRepository;
    private final IShopMemberRepository shopMemberRepository;

    public List<ProductCategoryDTO> getAllByShop(Long shopId, Boolean active) {
        List<ProductCategory> categories = active == null
                ? productCategoryRepository.findByShopIdOrderBySortOrderAscNameAsc(shopId)
                : productCategoryRepository.findByShopIdAndActiveOrderBySortOrderAscNameAsc(shopId, active);

        return categories.stream().map(ProductCategoryMapper::toDTO).toList();
    }

    public ProductCategoryDTO getById(Long shopId, Long id) {
        return productCategoryRepository.findByIdAndShopId(id, shopId)
                .map(ProductCategoryMapper::toDTO)
                .orElseThrow(() -> new ProductCategoryNotFound("ProductCategoryNotFound", "Product category not found"));
    }

    public ProductCategoryDTO create(Long shopId, ProductCategoryDTO dto) {
        assertCanManageCategory(shopId);
        assertCategoryNameNotDuplicated(shopId, dto.getName(), null);

        ProductCategory entity = ProductCategoryMapper.toEntity(dto);
        entity.setShopId(shopId);
        return ProductCategoryMapper.toDTO(productCategoryRepository.save(entity));
    }

    public ProductCategoryDTO update(Long shopId, Long id, ProductCategoryDTO dto) {
        assertCanManageCategory(shopId);

        ProductCategory entity = productCategoryRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ProductCategoryNotFound("ProductCategoryNotFound", "Product category not found"));
        assertCategoryNameNotDuplicated(shopId, dto.getName(), id);
        ProductCategoryMapper.updateEntity(entity, dto);
        return ProductCategoryMapper.toDTO(productCategoryRepository.save(entity));
    }

    public void delete(Long shopId, Long id) {
        assertCanManageCategory(shopId);

        ProductCategory entity = productCategoryRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ProductCategoryNotFound("ProductCategoryNotFound", "Product category not found"));
        entity.setActive(false);
        productCategoryRepository.save(entity);
    }

    private void assertCanManageCategory(Long shopId) {
        Long userId = getCurrentUserId();
        boolean allowed = shopMemberRepository.existsByShopIdAndUserIdAndRoleInAndStatus(
                shopId,
                userId,
                CATEGORY_MANAGE_ROLES,
                MemberStatus.ACTIVE
        );

        if (!allowed) {
            throw new ProductAccessDenied(
                    "ProductAccessDenied",
                    "Only shop owner or manager can manage product categories"
            );
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new ProductAccessDenied(
                    "ProductAccessDenied",
                    "Authenticated user is required"
            );
        }
        return userPrincipal.getUser().getId();
    }

    private void assertCategoryNameNotDuplicated(Long shopId, String name, Long excludedId) {
        boolean duplicated = excludedId == null
                ? productCategoryRepository.existsByShopIdAndName(shopId, name)
                : productCategoryRepository.existsByShopIdAndNameAndIdNot(shopId, name, excludedId);

        if (duplicated) {
            throw new ProductCategoryDuplicate(
                    "ProductCategoryDuplicate",
                    "Product category name already exists in shop"
            );
        }
    }
}
