package com.exe101.booking.service;

import com.exe101.booking.dto.*;
import com.exe101.booking.entity.*;
import com.exe101.booking.exception.BookingNotFound;
import com.exe101.booking.exception.BookingValidationException;
import com.exe101.booking.mapper.BookingMapper;
import com.exe101.booking.repository.IBookingItemRepository;
import com.exe101.booking.repository.IBookingRepository;
import com.exe101.booking.repository.IBookingStatusEventRepository;
import com.exe101.commission.service.CommissionService;
import com.exe101.common.ScrollResponse;
import com.exe101.customer.entity.Customer;
import com.exe101.customer.repository.ICustomerRepository;
import com.exe101.invoice.dto.InvoiceDTO;
import com.exe101.invoice.entity.Invoice;
import com.exe101.invoice.entity.InvoiceStatus;
import com.exe101.invoice.repository.IInvoiceRepository;
import com.exe101.invoice.service.InvoiceService;
import com.exe101.notification.dto.NotificationTargetType;
import com.exe101.notification.dto.NotificationType;
import com.exe101.notification.service.NotificationService;
import com.exe101.pet.entity.Pet;
import com.exe101.pet.repository.IPetRepository;
import com.exe101.product.entity.Product;
import com.exe101.product.repository.IProductRepository;
import com.exe101.service_shop.entity.ServiceType;
import com.exe101.service_shop.repository.IServiceRepository;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.repository.IShopMemberRepository;
import com.exe101.user.entity.User;
import com.exe101.user.entity.UserStatus;
import com.exe101.user.repository.IUserRepository;
import com.exe101.vaccine.entity.PetVaccination;
import com.exe101.vaccine.repository.IPetVaccinationRepository;
import com.exe101.veterinary.entity.PetMedicalRecord;
import com.exe101.veterinary.repository.IPetMedicalRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final int MAX_SCROLL_SIZE = 50;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final IBookingRepository bookingRepository;
    private final IBookingItemRepository bookingItemRepository;
    private final IBookingStatusEventRepository bookingStatusEventRepository;
    private final ICustomerRepository customerRepository;
    private final IInvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final IProductRepository productRepository;
    private final IServiceRepository serviceRepository;
    private final IShopMemberRepository shopMemberRepository;
    private final NotificationService notificationService;
    private final IUserRepository userRepository;
    private final IPetRepository petRepository;
    private final IPetMedicalRecordRepository petMedicalRecordRepository;
    private final IPetVaccinationRepository petVaccinationRepository;
    private final CommissionService commissionService;

    public List<BookingListItemDTO> getAll() {
        return toListItemDTOs(bookingRepository.findAll());
    }

    public ScrollResponse<BookingListItemDTO> getAllForScroll(
            Long shopId,
            Long userId,
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
                userId,
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

    public List<BookingListItemDTO> getByCurrentDate(Long shopId, LocalDate currentDate) {
        if (shopId == null) {
            throw new BookingValidationException(
                    "BookingShopRequired",
                    "Thiếu shopId để lấy danh sách lịch hẹn theo ngày"
            );
        }
        if (currentDate == null) {
            throw new BookingValidationException(
                    "BookingCurrentDateRequired",
                    "Ng\u00e0y hi\u1ec7n t\u1ea1i kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng"
            );
        }

        OffsetDateTime appointmentFrom = toStartOfDay(currentDate);
        OffsetDateTime appointmentTo = toStartOfNextDay(currentDate);
        return toListItemDTOs(
                bookingRepository.findByShopIdAndAppointmentDate(shopId, appointmentFrom, appointmentTo)
        );
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
        List<Long> userIds = bookings.stream()
                .map(Booking::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> customerIds = bookings.stream()
                .map(Booking::getCustomerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
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
        Map<Long, com.exe101.service_shop.entity.Service> servicesById = loadServicesById(itemsByBookingId);
        Map<Long, Pet> petsById = loadPetsById(bookings, itemsByBookingId);

        return bookings.stream()
                .map(booking -> toListItemDTO(
                        booking,
                        usersById.get(booking.getUserId()),
                        customersById.get(booking.getCustomerId()),
                        itemsByBookingId.getOrDefault(booking.getId(), Collections.emptyList()),
                        invoicesByBookingId.get(booking.getId()),
                        productsById,
                        servicesById,
                        petsById
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

    private Map<Long, com.exe101.service_shop.entity.Service> loadServicesById(Map<Long, List<BookingItem>> itemsByBookingId) {
        List<Long> serviceIds = itemsByBookingId.values().stream()
                .flatMap(List::stream)
                .filter(item -> item.getItemType() == BookingItemType.SERVICE)
                .map(BookingItem::getRefId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return serviceRepository.findAllById(serviceIds).stream()
                .collect(Collectors.toMap(com.exe101.service_shop.entity.Service::getId, Function.identity()));
    }

    private Map<Long, Pet> loadPetsById(List<Booking> bookings, Map<Long, List<BookingItem>> itemsByBookingId) {
        List<Long> petIds = java.util.stream.Stream.concat(
                        bookings.stream().map(Booking::getPetId),
                        itemsByBookingId.values().stream()
                                .flatMap(List::stream)
                                .map(BookingItem::getPetId)
                )
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return petRepository.findAllById(petIds).stream()
                .collect(Collectors.toMap(Pet::getId, Function.identity()));
    }

    private BookingListItemDTO toListItemDTO(
            Booking booking,
            User user,
            Customer customer,
            List<BookingItem> items,
            Invoice invoice,
            Map<Long, Product> productsById,
            Map<Long, com.exe101.service_shop.entity.Service> servicesById,
            Map<Long, Pet> petsById
    ) {
        List<BookingLineItemDTO> lineItems = items.stream()
                .map(item -> toLineItemDTO(item, productsById, servicesById, petsById))
                .toList();
        Pet bookingPet = booking.getPetId() != null ? petsById.get(booking.getPetId()) : null;
        Long totalAmount = invoice != null
                ? invoice.getTotalAmount()
                : lineItems.stream().mapToLong(item -> item.getAmount() != null ? item.getAmount() : 0L).sum();

        BookingListItemDTO dto = new BookingListItemDTO();
        dto.setId(booking.getId());
        dto.setBookingCode(formatBookingCode(booking.getId()));
        dto.setShopId(booking.getShopId());
        dto.setUserId(booking.getUserId());
        dto.setCustomerId(booking.getCustomerId());
        dto.setPetId(booking.getPetId());
        dto.setUserFullName(user != null ? user.getFullName() : null);
        dto.setUserPhone(user != null ? user.getPhone() : null);
        dto.setUserEmail(user != null ? user.getEmail() : null);
        dto.setUserAvatarUrlPreview(user != null ? user.getAvatarUrlPreview() : null);
        dto.setCustomerFullName(customer != null ? customer.getFullName() : null);
        dto.setCustomerPhone(customer != null ? customer.getPhone() : null);
        dto.setCustomerEmail(customer != null ? customer.getEmail() : null);
        dto.setPetName(bookingPet != null ? bookingPet.getName() : null);
        dto.setStartAt(booking.getStartAt());
        dto.setEndAt(booking.getEndAt());
        dto.setItems(lineItems);
        dto.setTotalAmount(totalAmount);
        dto.setStatus(booking.getStatus());
        dto.setStatusLabel(toStatusLabel(booking.getStatus()));
        dto.setSource(booking.getSource());
        dto.setNote(booking.getNote());
        dto.setCreatedBy(booking.getCreatedBy());
        dto.setTime(booking.getStartAt());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());
        return dto;
    }

    private BookingLineItemDTO toLineItemDTO(
            BookingItem item,
            Map<Long, Product> productsById,
            Map<Long, com.exe101.service_shop.entity.Service> servicesById,
            Map<Long, Pet> petsById
    ) {
        Product product = item.getItemType() == BookingItemType.PRODUCT
                ? productsById.get(item.getRefId())
                : null;
        com.exe101.service_shop.entity.Service service = item.getItemType() == BookingItemType.SERVICE
                ? servicesById.get(item.getRefId())
                : null;
        Pet pet = item.getPetId() != null ? petsById.get(item.getPetId()) : null;

        BookingLineItemDTO dto = new BookingLineItemDTO();
        dto.setBookingItemId(item.getId());
        dto.setItemType(item.getItemType());
        dto.setRefId(item.getRefId());
        dto.setProductId(item.getItemType() == BookingItemType.PRODUCT ? item.getRefId() : null);
        dto.setServiceId(item.getItemType() == BookingItemType.SERVICE ? item.getRefId() : null);
        dto.setName(resolveItemName(item, product, service));
        dto.setPetId(item.getPetId());
        dto.setPetName(pet != null ? pet.getName() : null);
        dto.setServiceType(service != null ? service.getServiceType() : null);
        dto.setVeterinaryServiceType(service != null ? service.getVeterinaryServiceType() : null);
        dto.setVaccineId(service != null ? service.getVaccineId() : null);
        dto.setVaccineName(service != null && service.getVaccine() != null ? service.getVaccine().getName() : null);
        dto.setQuantity(item.getQty());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setAmount(item.getAmount());
        return dto;
    }

    private String resolveItemName(
            BookingItem item,
            Product product,
            com.exe101.service_shop.entity.Service service
    ) {
        if (item.getRefId() == null) {
            return item.getItemType().name();
        }
        if (item.getItemType() == BookingItemType.PRODUCT) {
            return product != null ? product.getName() : "Product #" + item.getRefId();
        }
        if (item.getItemType() == BookingItemType.SERVICE) {
            return service != null ? service.getName() : "Service #" + item.getRefId();
        }
        return item.getItemType().name();
    }

    private String formatBookingCode(Long id) {
        return id != null ? "BKG-" + String.format("%03d", id) : null;
    }

    private void normalizePet(Booking booking) {
        if (booking.getPetId() == null) {
            return;
        }

        Pet pet = petRepository.findById(booking.getPetId())
                .orElseThrow(() -> new BookingValidationException(
                        "BookingPetNotFound",
                        "Kh\u00f4ng t\u00ecm th\u1ea5y th\u00fa c\u01b0ng"
                ));

        if (booking.getUserId() == null) {
            booking.setUserId(pet.getUserId());
            return;
        }

        if (!Objects.equals(booking.getUserId(), pet.getUserId())) {
            throw new BookingValidationException(
                    "BookingPetUserMismatch",
                    "Th\u00fa c\u01b0ng kh\u00f4ng thu\u1ed9c v\u1ec1 ng\u01b0\u1eddi d\u00f9ng c\u1ee7a l\u1ecbch h\u1eb9n"
            );
        }
    }

    private void normalizeUserAndCustomer(Booking booking) {
        Customer customer = null;
        if (booking.getCustomerId() != null) {
            customer = customerRepository.findByShopIdAndId(booking.getShopId(), booking.getCustomerId())
                    .orElseThrow(() -> new BookingValidationException(
                            "BookingCustomerNotFound",
                            "Kh\u00f4ng t\u00ecm th\u1ea5y kh\u00e1ch h\u00e0ng c\u1ee7a shop"
                    ));
            if (booking.getUserId() == null) {
                booking.setUserId(customer.getUserId());
            } else if (customer.getUserId() != null && !Objects.equals(booking.getUserId(), customer.getUserId())) {
                throw new BookingValidationException(
                        "BookingUserCustomerMismatch",
                        "userId kh\u00f4ng kh\u1edbp v\u1edbi customerId c\u1ee7a shop"
                );
            }
        }

        if (booking.getUserId() != null) {
            assertActiveUser(booking.getUserId());
            if (booking.getCustomerId() == null) {
                customerRepository.findFirstByShopIdAndUserIdOrderByIdDesc(booking.getShopId(), booking.getUserId())
                        .ifPresent(linkedCustomer -> booking.setCustomerId(linkedCustomer.getId()));
            }
            if (booking.getSource() == BookingSource.CUSTOMER && booking.getCreatedBy() == null) {
                booking.setCreatedBy(booking.getUserId());
            }
            return;
        }

        if (booking.getSource() == BookingSource.CUSTOMER) {
            throw new BookingValidationException(
                    "BookingUserRequired",
                    "L\u1ecbch h\u1eb9n t\u1eeb ng\u01b0\u1eddi d\u00f9ng c\u1ea7n c\u00f3 userId"
            );
        }
    }

    private void assertActiveUser(Long userId) {
        if (!userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)) {
            throw new BookingValidationException(
                    "BookingUserNotFound",
                    "Kh\u00f4ng t\u00ecm th\u1ea5y ng\u01b0\u1eddi d\u00f9ng \u0111ang ho\u1ea1t \u0111\u1ed9ng"
            );
        }
    }

    private void publishBookingCreatedNotification(Booking booking) {
        if (booking.getSource() != BookingSource.CUSTOMER) {
            return;
        }

        Customer customer = resolveNotificationCustomer(booking.getShopId(), booking.getCustomerId());
        User user = resolveNotificationUser(booking.getUserId());
        String displayName = user != null ? user.getFullName() : customer != null ? customer.getFullName() : null;

        notificationService.publishToShop(
                booking.getShopId(),
                NotificationType.BOOKING_CREATED,
                NotificationTargetType.BOOKING,
                booking.getId(),
                booking.getUserId(),
                "L\u1ecbch booking m\u1edbi",
                displayName != null && !displayName.isBlank()
                        ? "C\u00f3 l\u1ecbch booking m\u1edbi t\u1eeb " + displayName
                        : "C\u00f3 l\u1ecbch booking m\u1edbi",
                buildBookingNotificationMetadata(booking, user, customer)
        );
    }

    private Map<String, Object> buildBookingNotificationMetadata(
            Booking booking,
            User user,
            Customer customer
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("bookingId", booking.getId());
        metadata.put("bookingCode", formatBookingCode(booking.getId()));
        metadata.put("userId", booking.getUserId());
        metadata.put("userFullName", user != null ? user.getFullName() : null);
        metadata.put("customerId", booking.getCustomerId());
        metadata.put("customerName", customer != null ? customer.getFullName() : null);
        metadata.put("source", booking.getSource());
        metadata.put("startAt", booking.getStartAt());
        metadata.put("endAt", booking.getEndAt());
        metadata.put("senderUserId", booking.getUserId());
        metadata.put("senderAvatarUrl", user != null ? user.getAvatarUrlPreview() : null);
        return metadata;
    }

    private Customer resolveNotificationCustomer(Long shopId, Long customerId) {
        if (shopId == null || customerId == null) {
            return null;
        }
        return customerRepository.findByShopIdAndId(shopId, customerId).orElse(null);
    }

    private User resolveNotificationUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    private String toStatusLabel(BookingStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case DRAFT -> "Ch\u1edd x\u00e1c nh\u1eadn";
            case CONFIRMED -> "\u0110\u00e3 x\u00e1c nh\u1eadn";
            case IN_PROGRESS -> "\u0110ang th\u1ef1c hi\u1ec7n";
            case COMPLETED -> "Ho\u00e0n th\u00e0nh";
            case REJECTED -> "T\u1eeb ch\u1ed1i";
            case CANCELLED -> "Kh\u00e1ch h\u1ee7y";
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
    public BookingListItemDTO create(Long shopId, Long currentUserId, BookingCreateRequest request) {
        Booking booking = new Booking();
        booking.setShopId(shopId);
        booking.setUserId(currentUserId);
        booking.setPetId(request.getPetId());
        booking.setStartAt(request.getStartAt());
        booking.setStatus(BookingStatus.DRAFT);
        booking.setSource(BookingSource.CUSTOMER);
        booking.setNote(request.getNote());
        booking.setCreatedBy(currentUserId);

        Map<Long, Product> productsById = loadCreateProductsById(shopId, request.getItems());
        Map<Long, com.exe101.service_shop.entity.Service> servicesById = loadCreateServicesById(shopId, request.getItems());
        booking.setEndAt(calculateCreateEndAt(request.getStartAt(), request.getItems(), servicesById));
        normalizePet(booking);
        normalizeUserAndCustomer(booking);
        validateCreateItems(booking, request.getItems(), productsById, servicesById);

        Booking savedBooking = bookingRepository.save(booking);
        bookingItemRepository.saveAll(buildCreateBookingItems(savedBooking, request.getItems(), productsById, servicesById));
        BookingListItemDTO result = getById(savedBooking.getId());
        publishBookingCreatedNotification(savedBooking);
        return result;
    }

    private OffsetDateTime calculateCreateEndAt(
            OffsetDateTime startAt,
            List<BookingCreateItemRequest> items,
            Map<Long, com.exe101.service_shop.entity.Service> servicesById
    ) {
        int totalDurationMin = items.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getItemType() == BookingItemType.SERVICE)
                .map(item -> servicesById.get(item.getRefId()))
                .filter(Objects::nonNull)
                .map(com.exe101.service_shop.entity.Service::getDurationMin)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        if (totalDurationMin <= 0) {
            throw new BookingValidationException(
                    "BookingServiceDurationInvalid",
                    "Booking phải có ít nhất một dịch vụ hợp lệ để tính thời gian kết thúc"
            );
        }

        return startAt.plusMinutes(totalDurationMin);
    }

    @Transactional
    public BookingCheckoutResponse checkout(Long id, Long shopId, BookingCheckoutRequest request) {
        if (request == null) {
            throw new BookingValidationException(
                    "BookingCheckoutRequestRequired",
                    "Th\u00f4ng tin checkout l\u1ecbch h\u1eb9n kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng"
            );
        }

        Booking booking = bookingRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new BookingNotFound("BookingNotFound", "Kh\u00f4ng t\u00ecm th\u1ea5y l\u1ecbch h\u1eb9n"));
        validateBookingCanCheckout(booking);

        Invoice existingInvoice = invoiceRepository
                .findFirstByShopIdAndBookingIdOrderByIdDesc(shopId, booking.getId())
                .orElse(null);
        validateCheckoutInvoice(existingInvoice);

        List<BookingItem> items = buildCheckoutItems(booking, request.getItems());
        bookingItemRepository.deleteByBookingId(booking.getId());
        bookingItemRepository.saveAll(items);
        bookingItemRepository.flush();
        saveVeterinaryHistory(booking, request, items);

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
                    "L\u1ecbch h\u1eb9n \u0111\u00e3 ho\u00e0n th\u00e0nh, kh\u00f4ng th\u1ec3 checkout l\u1ea1i"
            );
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED) {
            throw new BookingValidationException(
                    "BookingClosed",
                    "Kh\u00f4ng th\u1ec3 checkout l\u1ecbch h\u1eb9n \u0111\u00e3 h\u1ee7y ho\u1eb7c \u0111\u00e3 t\u1eeb ch\u1ed1i"
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
                    "H\u00f3a \u0111\u01a1n c\u1ee7a l\u1ecbch h\u1eb9n \u0111\u00e3 \u0111\u01b0\u1ee3c thanh to\u00e1n, kh\u00f4ng th\u1ec3 checkout l\u1ea1i"
            );
        }
    }

    private Map<Long, Product> loadCreateProductsById(
            Long shopId,
            List<BookingCreateItemRequest> items
    ) {
        if (items == null || items.isEmpty()) {
            throw new BookingValidationException(
                    "BookingItemsRequired",
                    "Danh sách dịch vụ hoặc sản phẩm không được để trống"
            );
        }

        List<Long> productIds = items.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getItemType() == BookingItemType.PRODUCT)
                .map(BookingCreateItemRequest::getRefId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (productIds.isEmpty()) {
            return Map.of();
        }

        return productRepository.findAllById(productIds).stream()
                .filter(product -> Objects.equals(product.getShopId(), shopId))
                .collect(Collectors.toMap(
                        Product::getId,
                        Function.identity()
                ));
    }

    private Map<Long, com.exe101.service_shop.entity.Service> loadCreateServicesById(
            Long shopId,
            List<BookingCreateItemRequest> items
    ) {
        List<Long> serviceIds = items.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getItemType() == BookingItemType.SERVICE)
                .map(BookingCreateItemRequest::getRefId)
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

    private void validateCreateItems(
            Booking booking,
            List<BookingCreateItemRequest> items,
            Map<Long, Product> productsById,
            Map<Long, com.exe101.service_shop.entity.Service> servicesById
    ) {
        boolean hasVeterinaryService = false;

        for (BookingCreateItemRequest item : items) {
            if (item == null || item.getItemType() == null) {
                throw new BookingValidationException(
                        "BookingItemTypeRequired",
                        "Loại dòng booking không được để trống"
                );
            }

            switch (item.getItemType()) {
                case SERVICE -> {
                    com.exe101.service_shop.entity.Service service = servicesById.get(item.getRefId());
                    if (service == null) {
                        throw new BookingValidationException(
                                "BookingServiceNotFound",
                                "Không tìm thấy dịch vụ #" + item.getRefId() + " trong shop hiện tại"
                        );
                    }
                    if (!Boolean.TRUE.equals(service.getActive())) {
                        throw new BookingValidationException(
                                "BookingServiceInactive",
                                "Dịch vụ #" + item.getRefId() + " đang không hoạt động"
                        );
                    }
                    if (service.getServiceType() == ServiceType.VETERINARY) {
                        hasVeterinaryService = true;
                    }
                    if (item.getQty() != null && item.getQty() != 1) {
                        throw new BookingValidationException(
                                "BookingServiceQtyInvalid",
                                "Dịch vụ chỉ được có số lượng bằng 1"
                        );
                    }
                }
                case PRODUCT -> {
                    Product product = productsById.get(item.getRefId());
                    if (product == null) {
                        throw new BookingValidationException(
                                "BookingProductNotFound",
                                "Không tìm thấy sản phẩm #" + item.getRefId() + " trong shop hiện tại"
                        );
                    }
                    if (!Boolean.TRUE.equals(product.getActive())) {
                        throw new BookingValidationException(
                                "BookingProductInactive",
                                "Sản phẩm #" + item.getRefId() + " đang không hoạt động"
                        );
                    }
                    if (item.getQty() == null || item.getQty() <= 0) {
                        throw new BookingValidationException(
                                "BookingProductQtyInvalid",
                                "Số lượng sản phẩm phải lớn hơn 0"
                        );
                    }
                }
                default -> throw new BookingValidationException(
                        "BookingItemTypeUnsupported",
                        "Tạo booking hiện chỉ hỗ trợ dịch vụ và sản phẩm"
                );
            }
        }

        if (hasVeterinaryService && booking.getPetId() == null) {
            throw new BookingValidationException(
                    "BookingPetRequired",
                    "Booking có dịch vụ thú y bắt buộc phải có thú cưng"
            );
        }

    }

    private List<BookingItem> buildCreateBookingItems(
            Booking booking,
            List<BookingCreateItemRequest> items,
            Map<Long, Product> productsById,
            Map<Long, com.exe101.service_shop.entity.Service> servicesById
    ) {
        return items.stream()
                .map(item -> buildCreateBookingItem(booking, item, productsById, servicesById))
                .toList();
    }

    private BookingItem buildCreateBookingItem(
            Booking booking,
            BookingCreateItemRequest request,
            Map<Long, Product> productsById,
            Map<Long, com.exe101.service_shop.entity.Service> servicesById
    ) {
        BookingItem item = new BookingItem();
        item.setShopId(booking.getShopId());
        item.setBookingId(booking.getId());
        item.setItemType(request.getItemType());
        item.setRefId(request.getRefId());

        if (request.getItemType() == BookingItemType.SERVICE) {
            com.exe101.service_shop.entity.Service service = servicesById.get(request.getRefId());
            item.setPetId(service.getServiceType() == ServiceType.VETERINARY ? booking.getPetId() : null);
            item.setQty(1);
            item.setUnitPrice(service.getBasePrice() != null ? service.getBasePrice() : 0L);
        } else {
            Product product = productsById.get(request.getRefId());
            item.setPetId(null);
            item.setQty(request.getQty());
            item.setUnitPrice(product.getPrice() != null ? product.getPrice() : 0L);
        }

        item.setAmount(item.getUnitPrice() * item.getQty());
        return item;
    }

    private InvoiceDTO createOrUpdateCheckoutInvoice(
            Booking booking,
            OffsetDateTime issuedAt,
            Invoice existingInvoice
    ) {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setShopId(booking.getShopId());
        dto.setUserId(booking.getUserId());
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
                    "Danh s\u00e1ch d\u1ecbch v\u1ee5 ho\u1eb7c s\u1ea3n ph\u1ea9m thanh to\u00e1n kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng"
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
                    "Lo\u1ea1i d\u00f2ng thanh to\u00e1n kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng"
            );
        }

        Long unitPrice = resolveCheckoutUnitPrice(booking, request, productsById, servicesById);
        Integer qty = request.getQty();
        if (qty == null || qty <= 0) {
            throw new BookingValidationException(
                    "BookingCheckoutQtyInvalid",
                    "S\u1ed1 l\u01b0\u1ee3ng ph\u1ea3i l\u1edbn h\u01a1n 0"
            );
        }
        if (unitPrice == null || unitPrice < 0) {
            throw new BookingValidationException(
                    "BookingCheckoutUnitPriceInvalid",
                    "\u0110\u01a1n gi\u00e1 ph\u1ea3i l\u1edbn h\u01a1n ho\u1eb7c b\u1eb1ng 0"
            );
        }

        BookingItem item = new BookingItem();
        item.setShopId(booking.getShopId());
        item.setBookingId(booking.getId());
        item.setPetId(resolveCheckoutPetId(booking, request, servicesById));
        item.setItemType(request.getItemType());
        item.setRefId(request.getRefId());
        item.setQty(qty);
        item.setUnitPrice(unitPrice);
        item.setAmount(unitPrice * qty);
        return item;
    }

    private Long resolveCheckoutUnitPrice(
            Booking booking,
            BookingCheckoutItemRequest request,
            Map<Long, Product> productsById,
            Map<Long, com.exe101.service_shop.entity.Service> servicesById
    ) {
        if (request.getItemType() == BookingItemType.PRODUCT) {
            Product product = resolveCheckoutProduct(booking.getShopId(), request.getRefId(), productsById);
            return request.getUnitPrice() != null ? request.getUnitPrice() : product.getPrice();
        }

        if (request.getItemType() == BookingItemType.SERVICE) {
            com.exe101.service_shop.entity.Service service = resolveCheckoutService(
                    booking.getShopId(),
                    request.getRefId(),
                    servicesById
            );
            validateVeterinaryCheckoutRequest(booking, request, service);
            return request.getUnitPrice() != null ? request.getUnitPrice() : service.getBasePrice();
        }

        throw new BookingValidationException(
                "BookingCheckoutItemTypeUnsupported",
                "Checkout l\u1ecbch h\u1eb9n hi\u1ec7n ch\u1ec9 h\u1ed7 tr\u1ee3 d\u00f2ng d\u1ecbch v\u1ee5 v\u00e0 s\u1ea3n ph\u1ea9m"
        );
    }

    private Long resolveCheckoutPetId(
            Booking booking,
            BookingCheckoutItemRequest request,
            Map<Long, com.exe101.service_shop.entity.Service> servicesById
    ) {
        if (request.getItemType() != BookingItemType.SERVICE) {
            return request.getPetId();
        }
        com.exe101.service_shop.entity.Service service = request.getRefId() != null
                ? servicesById.get(request.getRefId())
                : null;
        if (service == null || service.getServiceType() != com.exe101.service_shop.entity.ServiceType.VETERINARY) {
            return request.getPetId();
        }
        return request.getPetId() != null ? request.getPetId() : booking.getPetId();
    }

    private void validateVeterinaryCheckoutRequest(
            Booking booking,
            BookingCheckoutItemRequest request,
            com.exe101.service_shop.entity.Service service
    ) {
        if (service.getServiceType() != com.exe101.service_shop.entity.ServiceType.VETERINARY) {
            return;
        }
        if (booking.getPetId() == null) {
            throw new BookingValidationException(
                    "BookingPetRequired",
                    "Booking c\u00f3 d\u1ecbch v\u1ee5 th\u00fa y b\u1eaft bu\u1ed9c ph\u1ea3i g\u1eafn th\u00fa c\u01b0ng"
            );
        }
        if (request.getPetId() != null && !Objects.equals(request.getPetId(), booking.getPetId())) {
            throw new BookingValidationException(
                    "BookingCheckoutPetMismatch",
                    "D\u1ecbch v\u1ee5 th\u00fa y trong booking ph\u1ea3i d\u00f9ng c\u00f9ng m\u1ed9t th\u00fa c\u01b0ng"
            );
        }
        if (request.getVeterinarianUserId() != null
                && !shopMemberRepository.existsByShopIdAndUserIdAndStatus(
                booking.getShopId(),
                request.getVeterinarianUserId(),
                MemberStatus.ACTIVE
        )) {
            throw new BookingValidationException(
                    "BookingCheckoutVeterinarianInvalid",
                    "B\u00e1c s\u0129/nh\u00e2n vi\u00ean th\u00fa y ph\u1ea3i thu\u1ed9c shop hi\u1ec7n t\u1ea1i"
            );
        }
        if (service.getVeterinaryServiceType() == com.exe101.service_shop.entity.VeterinaryServiceType.VACCINATION
                && service.getVaccineId() == null) {
            throw new BookingValidationException(
                    "BookingCheckoutVaccineMissing",
                    "D\u1ecbch v\u1ee5 vaccine ch\u01b0a \u0111\u01b0\u1ee3c g\u1eafn vaccine"
            );
        }
    }

    private void saveVeterinaryHistory(
            Booking booking,
            BookingCheckoutRequest request,
            List<BookingItem> items
    ) {
        petMedicalRecordRepository.deleteByBookingId(booking.getId());
        petVaccinationRepository.deleteByBookingId(booking.getId());

        Map<Long, com.exe101.service_shop.entity.Service> servicesById = items.stream()
                .filter(item -> item.getItemType() == BookingItemType.SERVICE)
                .map(BookingItem::getRefId)
                .filter(Objects::nonNull)
                .distinct()
                .map(serviceRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(
                        com.exe101.service_shop.entity.Service::getId,
                        Function.identity()
                ));

        OffsetDateTime performedAt = request.getIssuedAt() != null ? request.getIssuedAt() : OffsetDateTime.now();
        for (int index = 0; index < items.size(); index++) {
            BookingItem item = items.get(index);
            BookingCheckoutItemRequest itemRequest = request.getItems().get(index);
            if (item.getItemType() != BookingItemType.SERVICE || item.getRefId() == null) {
                continue;
            }

            com.exe101.service_shop.entity.Service service = servicesById.get(item.getRefId());
            if (service == null || service.getServiceType() != com.exe101.service_shop.entity.ServiceType.VETERINARY) {
                continue;
            }

            PetMedicalRecord medicalRecord = new PetMedicalRecord();
            medicalRecord.setShopId(booking.getShopId());
            medicalRecord.setPetId(item.getPetId());
            medicalRecord.setBookingId(booking.getId());
            medicalRecord.setBookingItemId(item.getId());
            medicalRecord.setServiceId(service.getId());
            medicalRecord.setVaccineId(service.getVaccineId());
            medicalRecord.setVeterinarianUserId(itemRequest.getVeterinarianUserId());
            medicalRecord.setRecordType(service.getServiceType());
            medicalRecord.setVeterinaryServiceType(service.getVeterinaryServiceType());
            medicalRecord.setPerformedAt(performedAt);
            medicalRecord.setSymptoms(itemRequest.getSymptoms());
            medicalRecord.setDiagnosis(itemRequest.getDiagnosis());
            medicalRecord.setTreatment(itemRequest.getTreatment());
            medicalRecord.setNote(itemRequest.getMedicalNote());
            medicalRecord.setFollowUpAt(itemRequest.getFollowUpAt());
            medicalRecord.setCreatedBy(booking.getCreatedBy());
            PetMedicalRecord savedRecord = petMedicalRecordRepository.save(medicalRecord);

            if (service.getVeterinaryServiceType() == com.exe101.service_shop.entity.VeterinaryServiceType.VACCINATION) {
                PetVaccination vaccination = new PetVaccination();
                vaccination.setShopId(booking.getShopId());
                vaccination.setPetId(item.getPetId());
                vaccination.setVaccineId(service.getVaccineId());
                vaccination.setBookingId(booking.getId());
                vaccination.setBookingItemId(item.getId());
                vaccination.setServiceId(service.getId());
                vaccination.setMedicalRecordId(savedRecord.getId());
                vaccination.setVaccinatedAt(
                        itemRequest.getVaccinatedAt() != null
                                ? itemRequest.getVaccinatedAt()
                                : performedAt.toLocalDate()
                );
                vaccination.setNextDueAt(itemRequest.getNextDueAt());
                vaccination.setClinicName(itemRequest.getClinicName());
                vaccination.setVetName(itemRequest.getVetName());
                vaccination.setBatchNo(itemRequest.getBatchNo());
                vaccination.setNote(itemRequest.getMedicalNote());
                vaccination.setCreatedBy(booking.getCreatedBy());
                petVaccinationRepository.save(vaccination);
            }
        }
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
                    "Kh\u00f4ng t\u00ecm th\u1ea5y s\u1ea3n ph\u1ea9m #" + productId + " trong shop hi\u1ec7n t\u1ea1i"
            );
        }
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BookingValidationException(
                    "BookingCheckoutProductInactive",
                    "S\u1ea3n ph\u1ea9m #" + productId + " \u0111ang kh\u00f4ng ho\u1ea1t \u0111\u1ed9ng"
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
                    "D\u00f2ng d\u1ecbch v\u1ee5 c\u1ea7n c\u00f3 m\u00e3 d\u1ecbch v\u1ee5"
            );
        }

        com.exe101.service_shop.entity.Service service = servicesById.get(serviceId);
        if (service == null || !Objects.equals(service.getShopId(), shopId)) {
            throw new BookingValidationException(
                    "BookingCheckoutServiceNotFound",
                    "Kh\u00f4ng t\u00ecm th\u1ea5y d\u1ecbch v\u1ee5 #" + serviceId + " trong shop hi\u1ec7n t\u1ea1i"
            );
        }
        if (!Boolean.TRUE.equals(service.getActive())) {
            throw new BookingValidationException(
                    "BookingCheckoutServiceInactive",
                    "D\u1ecbch v\u1ee5 #" + serviceId + " \u0111ang kh\u00f4ng ho\u1ea1t \u0111\u1ed9ng"
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
        if (dto.getUserId() != null || dto.getCustomerId() != null || dto.getPetId() != null) {
            normalizePet(entity);
            normalizeUserAndCustomer(entity);
        }
        Booking savedBooking = bookingRepository.save(entity);
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
            if (status == BookingStatus.COMPLETED) {
                commissionService.createCommissionIfAbsent(saved);
                publishReviewInvitationNotification(saved);
            }
            createStatusEvent(saved.getShopId(), saved.getId(), previousStatus, status);
            publishBookingStatusUpdatedNotification(saved);
            return getById(saved.getId());
        }

        return getById(booking.getId());
    }

    public void publishBookingStatusUpdatedNotification(Booking booking) {
        if (booking.getUserId() == null) {
            return;
        }

        Customer customer = resolveNotificationCustomer(booking.getShopId(), booking.getCustomerId());
        User user = resolveNotificationUser(booking.getUserId());
        String statusLabel = toStatusLabel(booking.getStatus());

        notificationService.publishToUser(
                booking.getUserId(),
                booking.getShopId(),
                NotificationType.BOOKING_STATUS_UPDATED,
                NotificationTargetType.BOOKING,
                booking.getId(),
                null,
                "Cập nhật lịch hẹn " + (formatBookingCode(booking.getId()) != null ? formatBookingCode(booking.getId()) : ""),
                "Lịch hẹn của bạn đã chuyển sang trạng thái: " + statusLabel,
                buildBookingNotificationMetadata(booking, user, customer)
        );
    }

    public void publishReviewInvitationNotification(Booking booking) {
        if (booking.getUserId() == null) {
            return;
        }

        Customer customer = resolveNotificationCustomer(booking.getShopId(), booking.getCustomerId());
        User user = resolveNotificationUser(booking.getUserId());

        notificationService.publishToUser(
                booking.getUserId(),
                booking.getShopId(),
                NotificationType.REVIEW_INVITATION,
                NotificationTargetType.BOOKING,
                booking.getId(),
                null,
                "Đánh giá dịch vụ đặt lịch",
                "Lịch hẹn " + (formatBookingCode(booking.getId()) != null ? formatBookingCode(booking.getId()) : "") + " đã hoàn thành. Hãy để lại đánh giá dịch vụ nhé!",
                buildBookingNotificationMetadata(booking, user, customer)
        );
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
                .orElseThrow(() -> new BookingNotFound("BookingNotFound", "Kh\u00f4ng t\u00ecm th\u1ea5y l\u1ecbch h\u1eb9n"));
    }

    public void delete(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new BookingNotFound("BookingNotFound", "Kh\u00f4ng t\u00ecm th\u1ea5y l\u1ecbch h\u1eb9n");
        }
        bookingRepository.deleteById(id);
    }
}


