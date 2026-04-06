package com.exe101.product.service;

import com.exe101.common.IService;
import com.exe101.product.dto.ProductDTO;
import com.exe101.product.entity.Product;
import com.exe101.product.exception.ProductNotFound;
import com.exe101.product.mapper.ProductMapper;
import com.exe101.product.repository.IProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService implements IService<Product, ProductDTO, Long> {

    private final IProductRepository productRepository;

    @Override
    public List<ProductDTO> getAll() {
        return productRepository.findAll().stream().map(ProductMapper::toDTO).toList();
    }

    @Override
    public ProductDTO getById(Long id) {
        return productRepository.findById(id)
                .map(ProductMapper::toDTO)
                .orElseThrow(() -> new ProductNotFound("ProductNotFound", "Product not found"));
    }

    @Override
    public ProductDTO create(ProductDTO dto) {
        return ProductMapper.toDTO(productRepository.save(ProductMapper.toEntity(dto)));
    }

    @Override
    public ProductDTO update(Long id, ProductDTO dto) {
        Product entity = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFound("ProductNotFound", "Product not found"));
        ProductMapper.updateEntity(entity, dto);
        return ProductMapper.toDTO(productRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFound("ProductNotFound", "Product not found");
        }
        productRepository.deleteById(id);
    }
}
