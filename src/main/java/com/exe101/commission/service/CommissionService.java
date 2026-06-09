package com.exe101.commission.service;

import com.exe101.booking.entity.Booking;
import com.exe101.booking.entity.BookingItem;
import com.exe101.booking.entity.BookingItemType;
import com.exe101.booking.repository.IBookingItemRepository;
import com.exe101.commission.entity.CommissionSourceType;
import com.exe101.commission.entity.CommissionStatus;
import com.exe101.commission.entity.PlatformCommission;
import com.exe101.commission.repository.IPlatformCommissionRepository;
import com.exe101.invoice.entity.Invoice;
import com.exe101.invoice.repository.IInvoiceRepository;
import com.exe101.order.entity.CustomerOrder;
import com.exe101.service_shop.entity.ServiceType;
import com.exe101.service_shop.repository.IServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommissionService {

    @Value("${platform.commission-rate-bps:1500}")
    private int commissionRateBps;

    private final IPlatformCommissionRepository commissionRepository;
    private final IBookingItemRepository bookingItemRepository;
    private final IInvoiceRepository invoiceRepository;
    private final IServiceRepository serviceRepository;

    @Transactional
    public PlatformCommission createCommissionIfAbsent(CustomerOrder order) {
        if (order == null || order.getId() == null) {
            return null;
        }
        return createCommissionIfAbsent(
                order.getShopId(),
                CommissionSourceType.ORDER,
                order.getId(),
                valueOrZero(order.getSubtotalAmount()) + valueOrZero(order.getShippingFee()),
                valueOrZero(order.getDiscountAmount()),
                valueOrZero(order.getShippingFee())
        );
    }

    @Transactional
    public PlatformCommission createCommissionIfAbsent(Booking booking) {
        if (booking == null || booking.getId() == null) {
            return null;
        }

        List<BookingItem> items = bookingItemRepository.findByBookingId(booking.getId());
        CommissionSourceType sourceType = resolveBookingSourceType(items);
        long finalAmount = resolveBookingFinalAmount(booking, items);
        return createCommissionIfAbsent(
                booking.getShopId(),
                sourceType,
                booking.getId(),
                finalAmount,
                0L,
                0L
        );
    }

    @Transactional
    public PlatformCommission createCommissionIfAbsent(
            Long shopId,
            CommissionSourceType sourceType,
            Long sourceId,
            Long grossAmount,
            Long discountAmount,
            Long shippingFee
    ) {
        if (shopId == null || sourceType == null || sourceId == null) {
            return null;
        }

        Optional<PlatformCommission> existing = commissionRepository.findBySourceTypeAndSourceId(sourceType, sourceId);
        if (existing.isPresent()) {
            return existing.get();
        }

        long normalizedGross = valueOrZero(grossAmount);
        long normalizedDiscount = valueOrZero(discountAmount);
        long normalizedShipping = valueOrZero(shippingFee);
        long commissionBase = Math.max(0, normalizedGross - normalizedShipping - normalizedDiscount);
        long commissionAmount = calculateCommissionAmount(commissionBase);

        PlatformCommission commission = new PlatformCommission();
        commission.setShopId(shopId);
        commission.setSourceType(sourceType);
        commission.setSourceId(sourceId);
        commission.setGrossAmount(normalizedGross);
        commission.setDiscountAmount(normalizedDiscount);
        commission.setShippingFee(normalizedShipping);
        commission.setCommissionBase(commissionBase);
        commission.setCommissionRateBps(commissionRateBps);
        commission.setCommissionAmount(commissionAmount);
        commission.setStatus(CommissionStatus.PENDING);

        try {
            return commissionRepository.save(commission);
        } catch (DataIntegrityViolationException ex) {
            return commissionRepository.findBySourceTypeAndSourceId(sourceType, sourceId).orElseThrow(() -> ex);
        }
    }

    private CommissionSourceType resolveBookingSourceType(List<BookingItem> items) {
        List<Long> serviceIds = items.stream()
                .filter(item -> item.getItemType() == BookingItemType.SERVICE)
                .map(BookingItem::getRefId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (serviceIds.isEmpty()) {
            return CommissionSourceType.SERVICE_BOOKING;
        }

        Map<Long, com.exe101.service_shop.entity.Service> servicesById = serviceRepository.findAllById(serviceIds)
                .stream()
                .collect(Collectors.toMap(com.exe101.service_shop.entity.Service::getId, Function.identity()));
        boolean hasVetService = serviceIds.stream()
                .map(servicesById::get)
                .filter(Objects::nonNull)
                .anyMatch(service -> service.getServiceType() == ServiceType.VETERINARY);

        return hasVetService ? CommissionSourceType.VET_BOOKING : CommissionSourceType.SERVICE_BOOKING;
    }

    private long resolveBookingFinalAmount(Booking booking, List<BookingItem> items) {
        Invoice invoice = invoiceRepository.findFirstByShopIdAndBookingIdOrderByIdDesc(
                booking.getShopId(),
                booking.getId()
        ).orElse(null);
        if (invoice != null && invoice.getTotalAmount() != null) {
            return invoice.getTotalAmount();
        }
        return items.stream().mapToLong(item -> valueOrZero(item.getAmount())).sum();
    }

    private long calculateCommissionAmount(long commissionBase) {
        if (commissionBase <= 0 || commissionRateBps <= 0) {
            return 0;
        }
        return Math.round(commissionBase * (commissionRateBps / 10000.0));
    }

    private long valueOrZero(Long value) {
        return value != null ? value : 0L;
    }
}
