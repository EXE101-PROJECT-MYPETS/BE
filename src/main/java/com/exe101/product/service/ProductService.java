package com.exe101.product.service;

import com.exe101.common.IService;
import com.exe101.common.ScrollResponse;
import com.exe101.file.FileUploadUtil;
import com.exe101.inventory.entity.Inventory;
import com.exe101.inventory.entity.InventoryId;
import com.exe101.inventory.repository.IInventoryRepository;
import com.exe101.product.dto.ProductCreateRequest;
import com.exe101.product.dto.ProductDTO;
import com.exe101.product.entity.Product;
import com.exe101.product.entity.ProductCategory;
import com.exe101.product.entity.ProductImage;
import com.exe101.product.exception.ProductDuplicate;
import com.exe101.product.exception.ProductCategoryNotFound;
import com.exe101.product.exception.ProductNotFound;
import com.exe101.product.exception.ProductValidationException;
import com.exe101.product.mapper.ProductMapper;
import com.exe101.product.repository.IProductCategoryRepository;
import com.exe101.product.repository.IProductImageRepository;
import com.exe101.product.repository.IProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductService implements IService<Product, ProductDTO, Long> {

    private static final int MAX_SCROLL_SIZE = 20;

    private final IProductRepository productRepository;
    private final IProductCategoryRepository productCategoryRepository;
    private final IProductImageRepository productImageRepository;
    private final IInventoryRepository inventoryRepository;
    private final FileUploadUtil fileUploadUtil;

    @Override
    public List<ProductDTO> getAll() {
        return toProductDTOs(productRepository.findAll());
    }

    public List<ProductDTO> getAllByShopId(Long shopId) {
        return toProductDTOs(productRepository.findByShopIdOrderByIdDesc(shopId));
    }

    public ScrollResponse<ProductDTO> getAllForScroll(Long shopId, String keyword, Boolean active, Long cursor, int size) {
        int normalizedSize = Math.min(Math.max(size, 1), MAX_SCROLL_SIZE);
        Long normalizedCursor = cursor != null && cursor > 0 ? cursor : null;
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();

        List<Product> products = productRepository.findForScroll(
                shopId,
                normalizedKeyword,
                active,
                normalizedCursor,
                PageRequest.of(0, normalizedSize + 1)
        );

        boolean hasNext = products.size() > normalizedSize;
        List<Product> content = products.stream()
                .limit(normalizedSize)
                .toList();
        Long nextCursor = hasNext && !content.isEmpty()
                ? content.get(content.size() - 1).getId()
                : null;

        return ScrollResponse.of(toProductDTOs(content), normalizedSize, nextCursor, hasNext);
    }

    @Override
    public ProductDTO getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFound("ProductNotFound", "Không tìm thấy sản phẩm"));
        return toProductDTOs(List.of(product)).get(0);
    }

    @Override
    @Transactional
    public ProductDTO create(ProductDTO dto) {
        return createInternal(dto.getShopId(), toProductWriteData(dto));
    }

    @Override
    @Transactional
    public ProductDTO update(Long id, ProductDTO dto) {
        return updateInternal(id, dto.getShopId(), toProductWriteData(dto));
    }

    @Transactional
    public ProductDTO create(Long shopId, ProductCreateRequest request) {
        return createInternal(shopId, toProductWriteData(request));
    }

    @Transactional
    public ProductDTO update(Long id, Long shopId, ProductCreateRequest request) {
        return updateInternal(id, shopId, toProductWriteData(request));
    }

    private ProductDTO createInternal(Long shopId, ProductWriteData data) {
        validateCategory(shopId, data.categoryId(), null);
        validateSkuUniqueness(shopId, data.sku(), null);

        Product entity = new Product();
        entity.setShopId(shopId);
        applyWriteData(entity, data);

        Product saved = productRepository.save(entity);
        syncInventory(saved, data.stockQty(), true);
        syncProductImages(saved, resolveFinalImageUrls(shopId, saved.getId(), data));
        return getById(saved.getId());
    }

    private ProductDTO updateInternal(Long id, Long shopId, ProductWriteData data) {
        Product entity = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFound("ProductNotFound", "Không tìm thấy sản phẩm"));
        validateCategory(shopId, data.categoryId(), entity.getCategoryId());
        validateSkuUniqueness(shopId, data.sku(), id);
        entity.setShopId(shopId);
        applyWriteData(entity, data);
        Product saved = productRepository.save(entity);
        if (data.stockQty() != null) {
            syncInventory(saved, data.stockQty(), false);
        }
        if (data.imageUrlsProvided()) {
            syncProductImages(saved, resolveFinalImageUrls(shopId, saved.getId(), data));
        }
        return getById(saved.getId());
    }

    @Override
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFound("ProductNotFound", "Không tìm thấy sản phẩm");
        }
        productRepository.deleteById(id);
    }

    private List<ProductDTO> toProductDTOs(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }

        Map<Long, String> categoryNamesById = loadCategoryNamesById(products);
        Map<Long, Long> stockQtyByProductId = loadStockQtyByProductId(products);
        Map<Long, List<String>> imageUrlsByProductId = loadImageUrlsByProductId(products);

        return products.stream()
                .map(product -> {
                    ProductDTO dto = ProductMapper.toDTO(product);
                    dto.setCategoryName(categoryNamesById.get(product.getCategoryId()));
                    dto.setStockQty(stockQtyByProductId.getOrDefault(product.getId(), 0L));
                    dto.setImageUrls(imageUrlsByProductId.getOrDefault(product.getId(), List.of()));
                    return dto;
                })
                .toList();
    }

    private Map<Long, String> loadCategoryNamesById(List<Product> products) {
        List<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return productCategoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getName));
    }

    private Map<Long, Long> loadStockQtyByProductId(List<Product> products) {
        Map<Long, Long> stockQtyByProductId = new HashMap<>();
        Map<Long, List<Product>> productsByShopId = products.stream()
                .collect(Collectors.groupingBy(Product::getShopId));

        for (Map.Entry<Long, List<Product>> entry : productsByShopId.entrySet()) {
            Long shopId = entry.getKey();
            List<Long> productIds = entry.getValue().stream().map(Product::getId).distinct().toList();
            List<Inventory> inventories = inventoryRepository.findByShopIdAndProductIdIn(shopId, productIds);
            for (Inventory inventory : inventories) {
                stockQtyByProductId.put(inventory.getProductId(), inventory.getOnHand());
            }
        }

        return stockQtyByProductId;
    }

    private Map<Long, List<String>> loadImageUrlsByProductId(List<Product> products) {
        Map<Long, List<String>> imageUrlsByProductId = new HashMap<>();
        Map<Long, List<Product>> productsByShopId = products.stream()
                .collect(Collectors.groupingBy(Product::getShopId));

        for (Map.Entry<Long, List<Product>> entry : productsByShopId.entrySet()) {
            Long shopId = entry.getKey();
            List<Long> productIds = entry.getValue().stream().map(Product::getId).distinct().toList();
            List<ProductImage> images = productImageRepository.findByShopIdAndProductIdInOrderByProductIdAscSortOrderAscIdAsc(
                    shopId,
                    productIds
            );

            for (ProductImage image : images) {
                imageUrlsByProductId
                        .computeIfAbsent(image.getProductId(), ignored -> new ArrayList<>())
                        .add(fileUploadUtil.normalizeProductImagePath(image.getImageUrl()));
            }
        }

        return imageUrlsByProductId;
    }

    private void validateCategory(Long shopId, Long categoryId, Long currentCategoryId) {
        if (categoryId == null) {
            return;
        }

        if (Objects.equals(categoryId, currentCategoryId)
                && productCategoryRepository.existsByIdAndShopId(categoryId, shopId)) {
            return;
        }

        if (productCategoryRepository.findByIdAndShopIdAndActiveTrue(categoryId, shopId).isEmpty()) {
            throw new ProductCategoryNotFound("ProductCategoryNotFound", "Không tìm thấy nhóm sản phẩm");
        }
    }

    private void validateSkuUniqueness(Long shopId, String sku, Long excludedId) {
        boolean duplicated = excludedId == null
                ? productRepository.existsByShopIdAndSku(shopId, sku)
                : productRepository.existsByShopIdAndSkuAndIdNot(shopId, sku, excludedId);

        if (duplicated) {
            throw new ProductDuplicate(
                    "ProductDuplicate",
                    "SKU sản phẩm đã tồn tại trong shop"
            );
        }
    }

    private void syncInventory(Product product, Long stockQty, boolean initializeWhenMissing) {
        InventoryId inventoryId = new InventoryId(product.getShopId(), product.getId());
        Inventory inventory = inventoryRepository.findById(inventoryId).orElse(null);

        if (inventory == null) {
            if (!initializeWhenMissing && stockQty == null) {
                return;
            }
            inventory = new Inventory();
            inventory.setId(inventoryId);
            inventory.setOnHand(0L);
            inventory.setReserved(0L);
        }

        long targetOnHand = stockQty != null ? stockQty : inventory.getOnHand() != null ? inventory.getOnHand() : 0L;
        long reserved = inventory.getReserved() != null ? inventory.getReserved() : 0L;
        if (targetOnHand < reserved) {
            throw new ProductValidationException(
                    "ProductStockInvalid",
                    "Số lượng tồn phải lớn hơn hoặc bằng số lượng đã giữ"
            );
        }

        inventory.setOnHand(targetOnHand);
        inventoryRepository.save(inventory);
    }

    private void syncProductImages(Product product, List<String> rawImageUrls) {
        List<String> imageUrls = normalizeImageUrls(rawImageUrls);
        productImageRepository.deleteByShopIdAndProductId(product.getShopId(), product.getId());
        if (imageUrls.isEmpty()) {
            return;
        }

        List<ProductImage> images = new ArrayList<>();
        for (int index = 0; index < imageUrls.size(); index++) {
            ProductImage image = new ProductImage();
            image.setShopId(product.getShopId());
            image.setProductId(product.getId());
            image.setImageUrl(imageUrls.get(index));
            image.setSortOrder(index);
            images.add(image);
        }
        productImageRepository.saveAll(images);
    }

    private List<String> normalizeImageUrls(List<String> rawImageUrls) {
        if (rawImageUrls == null) {
            return List.of();
        }
        return rawImageUrls.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(url -> !url.isBlank())
                .map(fileUploadUtil::normalizeProductImagePath)
                .distinct()
                .toList();
    }

    private ProductWriteData toProductWriteData(ProductDTO dto) {
        return new ProductWriteData(
                dto.getCategoryId(),
                dto.getSku(),
                dto.getName(),
                dto.getUnit(),
                dto.getPrice(),
                dto.getWeightKg(),
                dto.getActive(),
                dto.getStockQty(),
                dto.getImageUrls(),
                List.of(),
                dto.getImageUrls() != null
        );
    }

    private ProductWriteData toProductWriteData(ProductCreateRequest request) {
        List<MultipartFile> imageFiles = request.getImageFiles() == null
                ? List.of()
                : request.getImageFiles().stream()
                .filter(Objects::nonNull)
                .filter(file -> !file.isEmpty())
                .toList();

        boolean shouldSyncImages = Boolean.TRUE.equals(request.getReplaceImages())
                || request.getImageUrls() != null
                || !imageFiles.isEmpty();

        return new ProductWriteData(
                request.getCategoryId(),
                request.getSku(),
                request.getName(),
                request.getUnit(),
                request.getPrice(),
                request.getWeightKg(),
                request.getActive(),
                request.getStockQty(),
                request.getImageUrls(),
                imageFiles,
                shouldSyncImages
        );
    }

    private void applyWriteData(Product entity, ProductWriteData data) {
        entity.setCategoryId(data.categoryId());
        entity.setSku(data.sku());
        entity.setName(data.name());
        entity.setUnit(data.unit());
        entity.setPrice(data.price() != null ? data.price() : 0L);
        entity.setWeightKg(data.weightKg() != null ? data.weightKg() : new BigDecimal("0.100"));
        entity.setActive(data.active() != null ? data.active() : Boolean.TRUE);
    }

    private List<String> resolveFinalImageUrls(Long shopId, Long productId, ProductWriteData data) {
        List<String> finalUrls = new ArrayList<>(normalizeImageUrls(data.imageUrls()));
        if (data.imageFiles() == null || data.imageFiles().isEmpty()) {
            return finalUrls;
        }

        for (MultipartFile imageFile : data.imageFiles()) {
            finalUrls.add(fileUploadUtil.uploadProductImage(shopId, productId, imageFile));
        }
        return finalUrls;
    }

    private record ProductWriteData(
            Long categoryId,
            String sku,
            String name,
            String unit,
            Long price,
            BigDecimal weightKg,
            Boolean active,
            Long stockQty,
            List<String> imageUrls,
            List<MultipartFile> imageFiles,
            boolean imageUrlsProvided
    ) {
    }
}
