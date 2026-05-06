package com.exe101.booking.service;

import com.exe101.booking.dto.BookingCheckoutItemRequest;
import com.exe101.booking.dto.BookingCheckoutRequest;
import com.exe101.booking.dto.BookingCheckoutResponse;
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
import com.exe101.common.ScrollResponse;
import com.exe101.customer.entity.Customer;
import com.exe101.customer.repository.ICustomerRepository;
import com.exe101.invoice.dto.InvoiceDTO;
import com.exe101.invoice.entity.Invoice;
import com.exe101.invoice.entity.InvoiceStatus;
import com.exe101.invoice.repository.IInvoiceRepository;
import com.exe101.invoice.service.InvoiceService;
import com.exe101.product.entity.Product;
import com.exe101.product.repository.IProductRepository;
import com.exe101.service_shop.repository.IServiceRepository;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.repository.IShopMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final int MAX_SCROLL_SIZE = 50;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final IBookingRepository bookingRepository;
    private final IBookingItemRepository bookingItemRepository;
    private final IBookingStaffRepository bookingStaffRepository;
    private final IBookingStatusEventRepository bookingStatusEventRepository;
    private final ICustomerRepository customerRepository;
    private final IInvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final IProductRepository productRepository;
    private final IServiceRepository serviceRepository;
    private final IShopMemberRepository shopMemberRepository;

    public List<BookingListItemDTO> getAll() {
        return toListItemDTOs(bookingRepository.findAll());
    }

    public ScrollResponse<BookingListItemDTO> getAllForScroll(
            Long shopId,
            Long customerId,
            String customerName,
            BookingStatus status,
            BookingSource source,
            LocalDate createDate,
            LocalDate appointmentDate,
            Long cursor,
            int size
    ) {
        int normalizedSize = Math.min(Math.max(size, 1), MAX_SCROLL_SIZE);
        int pageCursor = normalizePageCursor(cursor);
        String normalizedCustomerName = normalizeCustomerName(customerName);
        OffsetDateTime createdFrom = toStartOfDay(createDate);
        OffsetDateTime createdTo = toStartOfNextDay(createDate);
        OffsetDateTime appointmentFrom = toStartOfDay(appointmentDate);
        OffsetDateTime appointmentTo = toStartOfNextDay(appointmentDate);

        Page<Booking> bookingPage = bookingRepository.findForScroll(
                shopId,
                customerId,
                normalizedCustomerName,
                status,
                source,
                createdFrom,
                createdTo,
                appointmentFrom,
                appointmentTo,
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

    private OffsetDateTime toStartOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay(BUSINESS_ZONE).toOffsetDateTime() : null;
    }

    private OffsetDateTime toStartOfNextDay(LocalDate date) {
        return date != null ? date.plusDays(1).atStartOfDay(BUSINESS_ZONE).toOffsetDateTime() : null;
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
        List<BookingStaffDTO> normalizedAssignedStaffs = assignedStaffs == null ? List.of() : List.copyOf(assignedStaffs);
        BookingStaffDTO primaryAssignedStaff = getPrimaryAssignedStaff(normalizedAssignedStaffs);

        BookingListItemDTO dto = new BookingListItemDTO();
        dto.setId(booking.getId());
        dto.setBookingCode(formatBookingCode(booking.getId()));
        dto.setShopId(booking.getShopId());
        dto.setCustomerId(booking.getCustomerId());
        dto.setCustomerName(customer != null ? customer.getFullName() : null);
        dto.setCustomerPhone(customer != null ? customer.getPhone() : null);
        dto.setStartAt(booking.getStartAt());
        dto.setEndAt(booking.getEndAt());
        dto.setItems(lineItems);
        dto.setTotalAmount(totalAmount);
        dto.setStatus(booking.getStatus());
        dto.setStatusLabel(toStatusLabel(booking.getStatus()));
        dto.setSource(booking.getSource());
        dto.setNote(booking.getNote());
        dto.setCreatedBy(booking.getCreatedBy());
        dto.setAssigneeId(primaryAssignedStaff != null ? primaryAssignedStaff.getUserId() : null);
        dto.setAssigneeName(primaryAssignedStaff != null ? primaryAssignedStaff.getFullName() : null);
        dto.setAssignedStaffIds(normalizedAssignedStaffs.stream().map(BookingStaffDTO::getUserId).toList());
        dto.setAssignedStaffs(normalizedAssignedStaffs);
        dto.setTime(booking.getStartAt());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());
        return dto;
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

    public BookingListItemDTO getById(Long id) {
        Booking booking = findBookingById(id);
        return toListItemDTOs(List.of(booking)).get(0);
    }

    @Transactional
    public InvoiceDTO getInvoice(Long shopId, Long id) {
        return invoiceService.getByBookingId(shopId, id);
    }

    @Transactional
    public BookingListItemDTO create(BookingDTO dto) {
        Booking savedBooking = bookingRepository.save(BookingMapper.toEntity(dto));
        syncAssignedStaff(savedBooking, dto.getAssignedStaffIds());
        return getById(savedBooking.getId());
    }

    @Transactional
    public BookingCheckoutResponse checkout(Long id, Long shopId, BookingCheckoutRequest request) {
        if (request == null) {
            throw new BookingValidationException(
                    "BookingCheckoutRequestRequired",
                    "Thông tin checkout lịch hẹn không được để trống"
            );
        }

        Booking booking = bookingRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new BookingNotFound("BookingNotFound", "Không tìm thấy lịch hẹn"));
        validateBookingCanCheckout(booking);

        Invoice existingInvoice = invoiceRepository
                .findFirstByShopIdAndBookingIdOrderByIdDesc(shopId, booking.getId())
                .orElse(null);
        validateCheckoutInvoice(existingInvoice);

        List<BookingItem> items = buildCheckoutItems(booking, request.getItems());
        bookingItemRepository.deleteByBookingId(booking.getId());
        bookingItemRepository.saveAll(items);
        bookingItemRepository.flush();

        InvoiceDTO invoice = createOrUpdateCheckoutInvoice(
                booking,
                request.getIssuedAt(),
                existingInvoice
        );
        return new BookingCheckoutResponse(getById(booking.getId()), invoice);
    }

    private void validateBookingCanCheckout(Booking booking) {
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BookingValidationException(
                    "BookingAlreadyCompleted",
                    "Lịch hẹn đã hoàn thành, không thể checkout lại"
            );
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED) {
            throw new BookingValidationException(
                    "BookingClosed",
                    "Không thể checkout lịch hẹn đã hủy hoặc đã từ chối"
            );
        }
    }

    private void validateCheckoutInvoice(Invoice invoice) {
        if (invoice == null) {
            return;
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BookingValidationException(
                    "BookingInvoiceAlreadyPaid",
                    "Hóa đơn của lịch hẹn đã được thanh toán, không thể checkout lại"
            );
        }
    }

    private InvoiceDTO createOrUpdateCheckoutInvoice(
            Booking booking,
            OffsetDateTime issuedAt,
            Invoice existingInvoice
    ) {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setShopId(booking.getShopId());
        dto.setCustomerId(booking.getCustomerId());
        dto.setBookingId(booking.getId());
        dto.setStatus(InvoiceStatus.ISSUED);
        dto.setIssuedAt(issuedAt != null ? issuedAt : OffsetDateTime.now());

        if (existingInvoice == null
                || existingInvoice.getStatus() == InvoiceStatus.CANCELLED
                || existingInvoice.getStatus() == InvoiceStatus.VOID) {
            return invoiceService.create(dto);
        }

        return invoiceService.update(existingInvoice.getId(), dto);
    }

    private List<BookingItem> buildCheckoutItems(
            Booking booking,
            List<BookingCheckoutItemRequest> rawItems
    ) {
        if (rawItems == null || rawItems.isEmpty()) {
            throw new BookingValidationException(
                    "BookingCheckoutItemsRequired",
                    "Danh sách dịch vụ hoặc sản phẩm thanh toán không được để trống"
            );
        }

        Map<Long, Product> productsById = loadCheckoutProductsById(booking.getShopId(), rawItems);
        Map<Long, com.exe101.service_shop.entity.Service> servicesById = loadCheckoutServicesById(
                booking.getShopId(),
                rawItems
        );

        return rawItems.stream()
                .map(item -> buildCheckoutItem(booking, item, productsById, servicesById))
                .toList();
    }

    private BookingItem buildCheckoutItem(
            Booking booking,
            BookingCheckoutItemRequest request,
            Map<Long, Product> productsById,
            Map<Long, com.exe101.service_shop.entity.Service> servicesById
    ) {
        if (request == null || request.getItemType() == null) {
            throw new BookingValidationException(
                    "BookingCheckoutItemTypeRequired",
                    "Loại dòng thanh toán không được để trống"
            );
        }

        Long unitPrice = resolveCheckoutUnitPrice(booking.getShopId(), request, productsById, servicesById);
        Integer qty = request.getQty();
        if (qty == null || qty <= 0) {
            throw new BookingValidationException(
                    "BookingCheckoutQtyInvalid",
                    "Số lượng phải lớn hơn 0"
            );
        }
        if (unitPrice == null || unitPrice < 0) {
            throw new BookingValidationException(
                    "BookingCheckoutUnitPriceInvalid",
                    "Đơn giá phải lớn hơn hoặc bằng 0"
            );
        }

        BookingItem item = new BookingItem();
        item.setShopId(booking.getShopId());
        item.setBookingId(booking.getId());
        item.setPetId(request.getPetId());
        item.setItemType(request.getItemType());
        item.setRefId(request.getRefId());
        item.setQty(qty);
        item.setUnitPrice(unitPrice);
        item.setAmount(unitPrice * qty);
        return item;
    }

    private Long resolveCheckoutUnitPrice(
            Long shopId,
            BookingCheckoutItemRequest request,
            Map<Long, Product> productsById,
            Map<Long, com.exe101.service_shop.entity.Service> servicesById
    ) {
        if (request.getItemType() == BookingItemType.PRODUCT) {
            Product product = resolveCheckoutProduct(shopId, request.getRefId(), productsById);
            return request.getUnitPrice() != null ? request.getUnitPrice() : product.getPrice();
        }

        if (request.getItemType() == BookingItemType.SERVICE) {
            com.exe101.service_shop.entity.Service service = resolveCheckoutService(
                    shopId,
                    request.getRefId(),
                    servicesById
            );
            return request.getUnitPrice() != null ? request.getUnitPrice() : service.getBasePrice();
        }

        throw new BookingValidationException(
                "BookingCheckoutItemTypeUnsupported",
                "Checkout lịch hẹn hiện chỉ hỗ trợ dòng dịch vụ và sản phẩm"
        );
    }

    private Product resolveCheckoutProduct(
            Long shopId,
            Long productId,
            Map<Long, Product> productsById
    ) {
        if (productId == null) {
            throw new BookingValidationException(
                    "BookingCheckoutProductRequired",
                    "Dòng sản phẩm cần có mã sản phẩm"
            );
        }

        Product product = productsById.get(productId);
        if (product == null || !Objects.equals(product.getShopId(), shopId)) {
            throw new BookingValidationException(
                    "BookingCheckoutProductNotFound",
                    "Không tìm thấy sản phẩm #" + productId + " trong shop hiện tại"
            );
        }
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BookingValidationException(
                    "BookingCheckoutProductInactive",
                    "Sản phẩm #" + productId + " đang không hoạt động"
            );
        }
        return product;
    }

    private com.exe101.service_shop.entity.Service resolveCheckoutService(
            Long shopId,
            Long serviceId,
            Map<Long, com.exe101.service_shop.entity.Service> servicesById
    ) {
        if (serviceId == null) {
            throw new BookingValidationException(
                    "BookingCheckoutServiceRequired",
                    "Dòng dịch vụ cần có mã dịch vụ"
            );
        }

        com.exe101.service_shop.entity.Service service = servicesById.get(serviceId);
        if (service == null || !Objects.equals(service.getShopId(), shopId)) {
            throw new BookingValidationException(
                    "BookingCheckoutServiceNotFound",
                    "Không tìm thấy dịch vụ #" + serviceId + " trong shop hiện tại"
            );
        }
        if (!Boolean.TRUE.equals(service.getActive())) {
            throw new BookingValidationException(
                    "BookingCheckoutServiceInactive",
                    "Dịch vụ #" + serviceId + " đang không hoạt động"
            );
        }
        return service;
    }

    private Map<Long, Product> loadCheckoutProductsById(
            Long shopId,
            List<BookingCheckoutItemRequest> items
    ) {
        List<Long> productIds = items.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getItemType() == BookingItemType.PRODUCT)
                .map(BookingCheckoutItemRequest::getRefId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }

        return productRepository.findAllById(productIds).stream()
                .filter(product -> Objects.equals(product.getShopId(), shopId))
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private Map<Long, com.exe101.service_shop.entity.Service> loadCheckoutServicesById(
            Long shopId,
            List<BookingCheckoutItemRequest> items
    ) {
        List<Long> serviceIds = items.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getItemType() == BookingItemType.SERVICE)
                .map(BookingCheckoutItemRequest::getRefId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (serviceIds.isEmpty()) {
            return Map.of();
        }

        return serviceRepository.findAllById(serviceIds).stream()
                .filter(service -> Objects.equals(service.getShopId(), shopId))
                .collect(Collectors.toMap(
                        com.exe101.service_shop.entity.Service::getId,
                        Function.identity()
                ));
    }

    @Transactional
    public BookingListItemDTO update(Long id, BookingDTO dto) {
        Booking entity = findBookingById(id);
        BookingMapper.updateEntity(entity, dto);
        Booking savedBooking = bookingRepository.save(entity);
        if (dto.getAssignedStaffIds() != null) {
            syncAssignedStaff(savedBooking, dto.getAssignedStaffIds());
        }
        return getById(savedBooking.getId());
    }

    @Transactional
    public BookingListItemDTO updateStatus(Long id, BookingStatus status) {
        if (status == null) {
            throw new BookingValidationException("BookingStatusRequired", "Trạng thái lịch hẹn không được để trống");
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFound("BookingNotFound", "Không tìm thấy lịch hẹn"));
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
    public BookingListItemDTO updateAssignedStaffs(Long id, Long shopId, List<Long> staffUserIds) {
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
                .orElseThrow(() -> new BookingNotFound("BookingNotFound", "Không tìm thấy lịch hẹn"));
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
                    "Lịch hẹn không thuộc shop hiện tại"
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

        List<Long> invalidStaffIds = staffUserIds.stream()
                .filter(userId -> !shopMemberRepository.existsByShopIdAndUserIdAndStatus(
                        shopId,
                        userId,
                        MemberStatus.ACTIVE
                ))
                .toList();

        if (!invalidStaffIds.isEmpty()) {
            throw new BookingValidationException(
                    "BookingStaffInvalid",
                    "Nhân viên được gán phải là nhân viên đang hoạt động của shop: " + invalidStaffIds
            );
        }
    }

    public void delete(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new BookingNotFound("BookingNotFound", "Không tìm thấy lịch hẹn");
        }
        bookingRepository.deleteById(id);
    }
}
