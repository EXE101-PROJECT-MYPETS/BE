package com.exe101.serviceReview.service;

import com.exe101.booking.entity.Booking;
import com.exe101.booking.entity.BookingStatus;
import com.exe101.booking.repository.IBookingRepository;
import com.exe101.common.IService;
import com.exe101.review.exception.ReviewDuplicate;
import com.exe101.review.exception.ReviewNotFound;
import com.exe101.review.exception.ReviewValidationException;
import com.exe101.serviceReview.dto.ServiceReviewDTO;
import com.exe101.serviceReview.entity.ServiceReview;
import com.exe101.serviceReview.entity.ServiceReviewReaction;
import com.exe101.serviceReview.mapper.ServiceReviewMapper;
import com.exe101.serviceReview.repository.IServiceReviewRepository;
import com.exe101.serviceReview.repository.IServiceReviewReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServiceReviewService implements IService<ServiceReview, ServiceReviewDTO, Long> {

    private final IServiceReviewRepository serviceReviewRepository;
    private final IBookingRepository bookingRepository;
    private final IServiceReviewReactionRepository serviceReviewReactionRepository;

    @Override
    public List<ServiceReviewDTO> getAll() {
        return serviceReviewRepository.findAll().stream().map(ServiceReviewMapper::toDTO).toList();
    }

    public List<ServiceReviewDTO> getAllByShopId(Long shopId, Long serviceId, Long customerId) {
        if (serviceId != null) {
            return serviceReviewRepository.findByShopIdAndServiceIdOrderByIdDesc(shopId, serviceId).stream()
                    .map(ServiceReviewMapper::toDTO)
                    .toList();
        }
        return serviceReviewRepository.findAll().stream()
                .filter(r -> r.getShopId().equals(shopId))
                .map(ServiceReviewMapper::toDTO)
                .toList();
    }

    @Override
    public ServiceReviewDTO getById(Long id) {
        return serviceReviewRepository.findById(id)
                .map(ServiceReviewMapper::toDTO)
                .orElseThrow(() -> new ReviewNotFound("ReviewNotFound", "Không tìm thấy đánh giá dịch vụ"));
    }

    @Override
    @Transactional
    public ServiceReviewDTO create(ServiceReviewDTO dto) {
        assertUniqueReview(dto.getShopId(), dto.getServiceId(), dto.getCustomerId(), null);
        return ServiceReviewMapper.toDTO(serviceReviewRepository.save(ServiceReviewMapper.toEntity(dto)));
    }

    @Override
    @Transactional
    public ServiceReviewDTO update(Long id, ServiceReviewDTO dto) {
        ServiceReview entity = serviceReviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFound("ReviewNotFound", "Không tìm thấy đánh giá dịch vụ"));
        assertUniqueReview(dto.getShopId(), dto.getServiceId(), dto.getCustomerId(), id);
        ServiceReviewMapper.updateEntity(entity, dto);
        return ServiceReviewMapper.toDTO(serviceReviewRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!serviceReviewRepository.existsById(id)) {
            throw new ReviewNotFound("ReviewNotFound", "Không tìm thấy đánh giá dịch vụ");
        }
        serviceReviewRepository.deleteById(id);
    }

    @Transactional
    public ServiceReviewDTO createCustomerReview(Long userId, ServiceReviewDTO dto) {
        List<Booking> completedBookings = bookingRepository.findCompletedBookingsByUserAndService(
                userId,
                dto.getServiceId(),
                com.exe101.booking.entity.BookingItemType.SERVICE,
                BookingStatus.COMPLETED
        );

        if (completedBookings.isEmpty()) {
            throw new ReviewValidationException(
                    "ServiceReviewRequiresCompletedBooking",
                    "Chỉ có thể đánh giá dịch vụ sau khi lịch hẹn đặt dịch vụ này đã hoàn thành"
            );
        }

        Booking booking = completedBookings.get(0);
        dto.setShopId(booking.getShopId());
        dto.setCustomerId(booking.getCustomerId());

        assertUniqueReview(dto.getShopId(), dto.getServiceId(), dto.getCustomerId(), null);

        ServiceReview entity = ServiceReviewMapper.toEntity(dto);
        return ServiceReviewMapper.toDTO(serviceReviewRepository.save(entity));
    }

    @Transactional
    public void toggleReaction(Long serviceReviewId, Long userId, Boolean isLike) {
        if (!serviceReviewRepository.existsById(serviceReviewId)) {
            throw new ReviewNotFound("ReviewNotFound", "Không tìm thấy đánh giá dịch vụ");
        }

        Optional<ServiceReviewReaction> reactionOpt = serviceReviewReactionRepository
                .findByServiceReviewIdAndUserId(serviceReviewId, userId);

        if (reactionOpt.isPresent()) {
            ServiceReviewReaction reaction = reactionOpt.get();
            if (reaction.getIsLike().equals(isLike)) {
                // If same action, toggle/remove the reaction
                serviceReviewReactionRepository.delete(reaction);
            } else {
                // If different, update the reaction (like -> dislike or vice versa)
                reaction.setIsLike(isLike);
                serviceReviewReactionRepository.save(reaction);
            }
        } else {
            ServiceReviewReaction reaction = new ServiceReviewReaction();
            reaction.setServiceReviewId(serviceReviewId);
            reaction.setUserId(userId);
            reaction.setIsLike(isLike);
            serviceReviewReactionRepository.save(reaction);
        }
    }

    @Transactional
    public ServiceReviewDTO replyToReview(Long id, Long shopId, String replyText) {
        ServiceReview entity = serviceReviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFound("ReviewNotFound", "Không tìm thấy đánh giá dịch vụ"));

        if (!entity.getShopId().equals(shopId)) {
            throw new ReviewValidationException("ReviewAccessDenied", "Bạn không có quyền phản hồi đánh giá này");
        }

        entity.setReply(replyText);
        entity.setReplyAt(replyText != null && !replyText.isBlank() ? OffsetDateTime.now() : null);

        return ServiceReviewMapper.toDTO(serviceReviewRepository.save(entity));
    }

    private void assertUniqueReview(Long shopId, Long serviceId, Long customerId, Long excludedId) {
        if (shopId == null || serviceId == null || customerId == null) {
            return;
        }

        // Check if there is already a review by this customer for this service
        Optional<ServiceReview> existingReview = serviceReviewRepository
                .findByShopIdAndServiceIdOrderByIdDesc(shopId, serviceId).stream()
                .filter(r -> r.getCustomerId().equals(customerId) && (excludedId == null || !r.getId().equals(excludedId)))
                .findFirst();

        if (existingReview.isPresent()) {
            throw new ReviewDuplicate("ReviewDuplicate", "Khách hàng đã đánh giá dịch vụ này trong shop");
        }
    }
}
