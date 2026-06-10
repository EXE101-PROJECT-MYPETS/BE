package com.exe101.review.service;

import com.exe101.common.IService;
import com.exe101.review.dto.ReviewDTO;
import com.exe101.review.entity.Review;
import com.exe101.review.exception.ReviewDuplicate;
import com.exe101.review.exception.ReviewNotFound;
import com.exe101.review.mapper.ReviewMapper;
import com.exe101.review.repository.IReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService implements IService<Review, ReviewDTO, Long> {

    private final IReviewRepository reviewRepository;

    @Override
    public List<ReviewDTO> getAll() {
        return reviewRepository.findAll().stream().map(ReviewMapper::toDTO).toList();
    }

    public List<ReviewDTO> getAllByShopId(Long shopId, Long productId, Long customerId) {
        if (productId != null && customerId != null) {
            return reviewRepository.findByShopIdAndProductIdAndCustomerId(shopId, productId, customerId).stream()
                    .map(ReviewMapper::toDTO)
                    .toList();
        }
        if (productId != null) {
            return reviewRepository.findByShopIdAndProductIdOrderByIdDesc(shopId, productId).stream()
                    .map(ReviewMapper::toDTO)
                    .toList();
        }
        if (customerId != null) {
            return reviewRepository.findByShopIdAndCustomerIdOrderByIdDesc(shopId, customerId).stream()
                    .map(ReviewMapper::toDTO)
                    .toList();
        }
        return reviewRepository.findByShopIdOrderByIdDesc(shopId).stream()
                .map(ReviewMapper::toDTO)
                .toList();
    }

    @Override
    public ReviewDTO getById(Long id) {
        return reviewRepository.findById(id)
                .map(ReviewMapper::toDTO)
                .orElseThrow(() -> new ReviewNotFound("ReviewNotFound", "Không tìm thấy đánh giá"));
    }

    @Override
    public ReviewDTO create(ReviewDTO dto) {
        assertUniqueReview(dto.getShopId(), dto.getProductId(), dto.getCustomerId(), null);
        return ReviewMapper.toDTO(reviewRepository.save(ReviewMapper.toEntity(dto)));
    }

    @Override
    public ReviewDTO update(Long id, ReviewDTO dto) {
        Review entity = reviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFound("ReviewNotFound", "Không tìm thấy đánh giá"));
        assertUniqueReview(dto.getShopId(), dto.getProductId(), dto.getCustomerId(), id);
        ReviewMapper.updateEntity(entity, dto);
        return ReviewMapper.toDTO(reviewRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new ReviewNotFound("ReviewNotFound", "Không tìm thấy đánh giá");
        }
        reviewRepository.deleteById(id);
    }

    private void assertUniqueReview(Long shopId, Long productId, Long customerId, Long excludedId) {
        if (shopId == null || productId == null || customerId == null) {
            return;
        }

        boolean duplicated = excludedId == null
                ? reviewRepository.existsByShopIdAndProductIdAndCustomerId(shopId, productId, customerId)
                : reviewRepository.existsByShopIdAndProductIdAndCustomerIdAndIdNot(
                shopId,
                productId,
                customerId,
                excludedId
        );

        if (duplicated) {
            throw new ReviewDuplicate("ReviewDuplicate", "Khách hàng đã đánh giá sản phẩm này trong shop");
        }
    }
}
