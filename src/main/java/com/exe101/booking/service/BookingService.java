package com.exe101.booking.service;

import com.exe101.booking.dto.BookingDTO;
import com.exe101.booking.dto.BookingLineItemDTO;
import com.exe101.booking.dto.BookingListItemDTO;
import com.exe101.booking.dto.BookingStaffDTO;
import com.exe101.booking.entity.Booking;
import com.exe101.booking.entity.BookingItem;
import com.exe101.booking.entity.BookingItemType;
import com.exe101.booking.entity.BookingStaff;
import com.exe101.booking.entity.BookingStaffId;
import com.exe101.booking.entity.BookingSource;
import com.exe101.booking.entity.BookingStatus;
import com.exe101.booking.entity.BookingStatusEvent;
import com.exe101.booking.exception.BookingNotFound;
import com.exe101.booking.exception.BookingValidationException;
import com.exe101.booking.mapper.BookingMapper;
import com.exe101.booking.repository.IBookingItemRepository;
import com.exe101.booking.repository.IBookingRepository;
import com.exe101.booking.repository.IBookingStaffRepository;
import com.exe101.booking.repository.IBookingStatusEventRepository;
import com.exe101.common.IService;
import com.exe101.common.ScrollResponse;
import com.exe101.customer.entity.Customer;
import com.exe101.customer.repository.ICustomerRepository;
import com.exe101.invoice.entity.Invoice;
import com.exe101.invoice.repository.IInvoiceRepository;
import com.exe101.product.entity.Product;
import com.exe101.product.repository.IProductRepository;
import com.exe101.service_shop.repository.IServiceRepository;
import com.exe101.shop.entity.ShopRole;
import com.exe101.shopMember.dto.ShopMemberDTO;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.repository.IShopMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService implements IService<Booking, BookingDTO, Long> {

    private static final int MAX_SCROLL_SIZE = 50;

    private final IBookingRepository bookingRepository;
    private final IBookingItemRepository bookingItemRepository;
    private final IBookingStaffRepository bookingStaffRepository;
    private final IBookingStatusEventRepository bookingStatusEventRepository;
    private final ICustomerRepository customerRepository;
    private final IInvoiceRepository invoiceRepository;
    private final IProductRepository productRepository;
    private final IServiceRepository serviceRepository;
    private final IShopMemberRepository shopMemberRepository;

    @Override
    public List<BookingDTO> getAll() {
        List<Booking> bookings = bookingRepository.findAll();
        Map<Long, List<BookingStaffDTO>> assignedStaffsByBookingId = loadAssignedStaffsByBookingId(
                bookings.stream().map(Booking::getId).toList()
        );
        return bookings.stream()
                .map(booking -> toBookingDTO(
                        booking,
                        assignedStaffsByBookingId.getOrDefault(booking.getId(), List.of())
                ))
                .toList();
    }

    public ScrollResponse<BookingListItemDTO> getAllForScroll(
            Long shopId,
            Long customerId,
            String customerName,
            BookingStatus status,
            BookingSource source,
            Long cursor,
            int size
    ) {
        int normalizedSize = Math.min(Math.max(size, 1), MAX_SCROLL_SIZE);
        int pageCursor = normalizePageCursor(cursor);
        String normalizedCustomerName = normalizeCustomerName(customerName);

        Page<Booking> bookingPage = bookingRepository.findForScroll(
                shopId,
                customerId,
                normalizedCustomerName,
                status,
                source,
                PageRequest.of(pageCursor, normalizedSize)
        );

        List<BookingListItemDTO> content = toListItemDTOs(bookingPage.getContent());
        Long nextCursor = bookingPage.hasNext()
                ? (long) pageCursor + 1
                : null;

        return ScrollResponse.of(content, normalizedSize, nextCursor, bookingPage.hasNext());
    }

    private int normalizePageCursor(Long cursor) {
        if (cursor == null || cursor <= 0) {
            return 0;
        }
        return cursor > Integer.MAX_VALUE ? Integer.MAX_VALUE : cursor.intValue();
    }

    private String normalizeCustomerName(String customerName) {
        if (customerName == null || customerName.isBlank()) {
            return null;
        }
        return customerName.trim();
    }

    private List<BookingListItemDTO> toListItemDTOs(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return List.of();
        }

        List<Long> bookingIds = bookings.stream().map(Booking::getId).toList();
        List<Long> customerIds = bookings.stream()
                .map(Booking::getCustomerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Customer> customersById = customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        Map<Long, List<BookingItem>> itemsByBookingId = bookingItemRepository.findByBookingIdIn(bookingIds).stream()
                .collect(Collectors.groupingBy(BookingItem::getBookingId));
        Map<Long, Invoice> invoicesByBookingId = invoiceRepository.findByBookingIdIn(bookingIds).stream()
                .filter(invoice -> invoice.getBookingId() != null)
                .collect(Collectors.toMap(
                        Invoice::getBookingId,
                        Function.identity(),
                        (current, replacement) -> current.getId() >= replacement.getId() ? current : replacement
                ));

        Map<Long, Product> productsById = loadProductsById(itemsByBookingId);
        Map<Long, String> serviceNamesById = loadServiceNamesById(itemsByBookingId);
        Map<Long, List<BookingStaffDTO>> assignedStaffsByBookingId = loadAssignedStaffsByBookingId(bookingIds);

        return bookings.stream()
                .map(booking -> toListItemDTO(
                        booking,
                        customersById.get(booking.getCustomerId()),
                        assignedStaffsByBookingId.getOrDefault(booking.getId(), List.of()),
                        itemsByBookingId.getOrDefault(booking.getId(), Collections.emptyList()),
                        invoicesByBookingId.get(booking.getId()),
                        productsById,
                        serviceNamesById
                ))
                .toList();
    }

    private Map<Long, Product> loadProductsById(Map<Long, List<BookingItem>> itemsByBookingId) {
        List<Long> productIds = itemsByBookingId.values().stream()
                .flatMap(List::stream)
                .filter(item -> item.getItemType() == BookingItemType.PRODUCT)
                .map(BookingItem::getRefId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private Map<Long, String> loadServiceNamesById(Map<Long, List<BookingItem>> itemsByBookingId) {
        List<Long> serviceIds = itemsByBookingId.values().stream()
                .flatMap(List::stream)
                .filter(item -> item.getItemType() == BookingItemType.SERVICE)
                .map(BookingItem::getRefId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return serviceRepository.findAllById(serviceIds).stream()
                .collect(Collectors.toMap(
                        com.exe101.service_shop.entity.Service::getId,
                        com.exe101.service_shop.entity.Service::getName
                ));
    }

    private BookingListItemDTO toListItemDTO(
            Booking booking,
            Customer customer,
            List<BookingStaffDTO> assignedStaffs,
            List<BookingItem> items,
            Invoice invoice,
            Map<Long, Product> productsById,
            Map<Long, String> serviceNamesById
    ) {
        List<BookingLineItemDTO> lineItems = items.stream()
                .map(item -> toLineItemDTO(item, productsById, serviceNamesById))
                .toList();
        Long totalAmount = invoice != null
                ? invoice.getTotalAmount()
                : lineItems.stream().mapToLong(item -> item.getAmount() != null ? item.getAmount() : 0L).sum();
        BookingStaffDTO primaryAssignedStaff = getPrimaryAssignedStaff(assignedStaffs);

        return new BookingListItemDTO(
                booking.getId(),
                formatBookingCode(booking.getId()),
                booking.getShopId(),
                booking.getCustomerId(),
                customer != null ? customer.getFullName() : null,
                customer != null ? customer.getPhone() : null,
                lineItems,
                totalAmount,
                booking.getStatus(),
                toStatusLabel(booking.getStatus()),
                booking.getSource(),
                primaryAssignedStaff != null ? primaryAssignedStaff.getUserId() : null,
                primaryAssignedStaff != null ? primaryAssignedStaff.getFullName() : null,
                assignedStaffs,
                booking.getStartAt(),
                booking.getCreatedAt()
        );
    }

    private BookingLineItemDTO toLineItemDTO(
            BookingItem item,
            Map<Long, Product> productsById,
            Map<Long, String> serviceNamesById
    ) {
        return new BookingLineItemDTO(
                item.getItemType(),
                item.getRefId(),
                resolveItemName(item, productsById, serviceNamesById),
                item.getQty(),
                item.getUnitPrice(),
                item.getAmount()
        );
    }

    private String resolveItemName(
            BookingItem item,
            Map<Long, Product> productsById,
            Map<Long, String> serviceNamesById
    ) {
        if (item.getRefId() == null) {
            return item.getItemType().name();
        }
        if (item.getItemType() == BookingItemType.PRODUCT) {
            Product product = productsById.get(item.getRefId());
            return product != null ? product.getName() : "Product #" + item.getRefId();
        }
        if (item.getItemType() == BookingItemType.SERVICE) {
            return serviceNamesById.getOrDefault(item.getRefId(), "Service #" + item.getRefId());
        }
        return item.getItemType().name();
    }

    private String formatBookingCode(Long id) {
        return id != null ? "BKG-" + String.format("%03d", id) : null;
    }

    private String toStatusLabel(BookingStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case DRAFT -> "Chờ xác nhận";
            case CONFIRMED -> "Đã xác nhận";
            case IN_PROGRESS -> "Đang thực hiện";
            case COMPLETED -> "Hoàn thành";
            case REJECTED -> "Từ chối";
            case CANCELLED -> "Khách hủy";
        };
    }

    @Override
    public BookingDTO getById(Long id) {
        Booking booking = findBookingById(id);
        return toBookingDTO(
                booking,
                loadAssignedStaffsByBookingId(List.of(id)).getOrDefault(id, List.of())
        );
    }

    @Override
    @Transactional
    public BookingDTO create(BookingDTO dto) {
        Booking savedBooking = bookingRepository.save(BookingMapper.toEntity(dto));
        syncAssignedStaff(savedBooking, dto.getAssignedStaffIds());
        return getById(savedBooking.getId());
    }

    @Override
    @Transactional
    public BookingDTO update(Long id, BookingDTO dto) {
        Booking entity = findBookingById(id);
        BookingMapper.updateEntity(entity, dto);
        Booking savedBooking = bookingRepository.save(entity);
        if (dto.getAssignedStaffIds() != null) {
            syncAssignedStaff(savedBooking, dto.getAssignedStaffIds());
        }
        return getById(savedBooking.getId());
    }

    @Transactional
    public BookingDTO updateStatus(Long id, BookingStatus status) {
        if (status == null) {
            throw new BookingValidationException("BookingStatusRequired", "Booking status is required");
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFound("BookingNotFound", "Booking not found"));
        BookingStatus previousStatus = booking.getStatus();

        if (!Objects.equals(previousStatus, status)) {
            booking.setStatus(status);
            Booking saved = bookingRepository.save(booking);
            createStatusEvent(saved.getShopId(), saved.getId(), previousStatus, status);
            return getById(saved.getId());
        }

        return getById(booking.getId());
    }

    @Transactional
    public BookingDTO updateAssignedStaffs(Long id, Long shopId, List<Long> staffUserIds) {
        Booking booking = findBookingById(id);
        validateBookingShop(booking, shopId);
        syncAssignedStaff(booking, staffUserIds);
        return getById(id);
    }

    private void createStatusEvent(Long shopId, Long bookingId, BookingStatus fromStatus, BookingStatus toStatus) {
        BookingStatusEvent event = new BookingStatusEvent();
        event.setShopId(shopId);
        event.setBookingId(bookingId);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        bookingStatusEventRepository.save(event);
    }

    private Booking findBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFound("BookingNotFound", "Booking not found"));
    }

    private BookingDTO toBookingDTO(Booking booking, List<BookingStaffDTO> assignedStaffs) {
        BookingDTO dto = BookingMapper.toDTO(booking);
        List<BookingStaffDTO> normalizedAssignedStaffs = assignedStaffs == null ? List.of() : List.copyOf(assignedStaffs);
        dto.setAssignedStaffs(normalizedAssignedStaffs);
        dto.setAssignedStaffIds(normalizedAssignedStaffs.stream().map(BookingStaffDTO::getUserId).toList());
        return dto;
    }

    private Map<Long, List<BookingStaffDTO>> loadAssignedStaffsByBookingId(List<Long> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty()) {
            return Map.of();
        }

        return bookingStaffRepository.findDisplayByBookingIdIn(bookingIds).stream()
                .collect(Collectors.groupingBy(BookingStaffDTO::getBookingId));
    }

    private BookingStaffDTO getPrimaryAssignedStaff(List<BookingStaffDTO> assignedStaffs) {
        if (assignedStaffs == null || assignedStaffs.isEmpty()) {
            return null;
        }
        return assignedStaffs.get(0);
    }

    private void validateBookingShop(Booking booking, Long shopId) {
        if (shopId != null && !Objects.equals(booking.getShopId(), shopId)) {
            throw new BookingValidationException(
                    "BookingShopMismatch",
                    "Booking does not belong to the current shop"
            );
        }
    }

    private void syncAssignedStaff(Booking booking, List<Long> rawStaffUserIds) {
        List<Long> staffUserIds = normalizeStaffUserIds(rawStaffUserIds);
        validateAssignedStaff(booking.getShopId(), staffUserIds);

        bookingStaffRepository.deleteByBookingId(booking.getId());
        if (staffUserIds.isEmpty()) {
            return;
        }

        List<BookingStaff> assignments = staffUserIds.stream()
                .map(userId -> {
                    BookingStaff assignment = new BookingStaff();
                    assignment.setId(new BookingStaffId(booking.getId(), userId));
                    assignment.setShopId(booking.getShopId());
                    return assignment;
                })
                .toList();
        bookingStaffRepository.saveAll(assignments);
    }

    private List<Long> normalizeStaffUserIds(List<Long> rawStaffUserIds) {
        if (rawStaffUserIds == null) {
            return List.of();
        }
        return rawStaffUserIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private void validateAssignedStaff(Long shopId, List<Long> staffUserIds) {
        if (staffUserIds.isEmpty()) {
            return;
        }

        Set<Long> activeStaffIds = shopMemberRepository.findByShopIdAndRoleAndStatusForDisplay(
                        shopId,
                        ShopRole.STAFF,
                        MemberStatus.ACTIVE
                ).stream()
                .map(ShopMemberDTO::getUserId)
                .collect(Collectors.toSet());

        List<Long> invalidStaffIds = staffUserIds.stream()
                .filter(userId -> !activeStaffIds.contains(userId))
                .toList();

        if (!invalidStaffIds.isEmpty()) {
            throw new BookingValidationException(
                    "BookingStaffInvalid",
                    "Assigned staff must be active staff members of the shop: " + invalidStaffIds
            );
        }
    }

    @Override
    public void delete(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new BookingNotFound("BookingNotFound", "Booking not found");
        }
        bookingRepository.deleteById(id);
    }
}
