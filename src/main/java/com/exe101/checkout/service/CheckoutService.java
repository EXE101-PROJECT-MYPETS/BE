package com.exe101.checkout.service;

import com.exe101.booking.entity.Booking;
import com.exe101.booking.entity.BookingItem;
import com.exe101.booking.entity.BookingItemType;
import com.exe101.booking.entity.BookingPet;
import com.exe101.booking.entity.BookingPetId;
import com.exe101.booking.entity.BookingSource;
import com.exe101.booking.entity.BookingStatus;
import com.exe101.booking.repository.IBookingItemRepository;
import com.exe101.booking.repository.IBookingPetRepository;
import com.exe101.booking.repository.IBookingRepository;
import com.exe101.checkout.dto.CheckoutRequestDTO;
import com.exe101.checkout.dto.CheckoutResponseDTO;
import com.exe101.checkout.dto.ProductOrderDTO;
import com.exe101.checkout.dto.ServiceBookingDTO;
import com.exe101.customer.entity.Customer;
import com.exe101.customer.service.CustomerService;
import com.exe101.customer.repository.ICustomerRepository;
import com.exe101.order.entity.CustomerOrder;
import com.exe101.order.entity.OrderItem;
import com.exe101.order.entity.OrderSource;
import com.exe101.order.entity.OrderStatus;
import com.exe101.order.repository.IOrderItemRepository;
import com.exe101.order.repository.IOrderRepository;
import com.exe101.pet.entity.Pet;
import com.exe101.pet.repository.IPetRepository;
import com.exe101.product.entity.Product;
import com.exe101.product.repository.IProductRepository;
import com.exe101.service_shop.entity.Service;
import com.exe101.service_shop.repository.IServiceRepository;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.repository.IShopMemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class CheckoutService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final IOrderRepository orderRepository;
    private final IOrderItemRepository orderItemRepository;
    private final IBookingRepository bookingRepository;
    private final IBookingItemRepository bookingItemRepository;
    private final IBookingPetRepository bookingPetRepository;
    private final ICustomerRepository customerRepository;
    private final CustomerService customerService;
    private final IProductRepository productRepository;
    private final IServiceRepository serviceRepository;
    private final IPetRepository petRepository;
    private final IShopMemberRepository shopMemberRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public CheckoutService(
            IOrderRepository orderRepository,
            IOrderItemRepository orderItemRepository,
            IBookingRepository bookingRepository,
            IBookingItemRepository bookingItemRepository,
            IBookingPetRepository bookingPetRepository,
            ICustomerRepository customerRepository,
            CustomerService customerService,
            IProductRepository productRepository,
            IServiceRepository serviceRepository,
            IPetRepository petRepository,
            IShopMemberRepository shopMemberRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.bookingPetRepository = bookingPetRepository;
        this.customerRepository = customerRepository;
        this.customerService = customerService;
        this.productRepository = productRepository;
        this.serviceRepository = serviceRepository;
        this.petRepository = petRepository;
        this.shopMemberRepository = shopMemberRepository;
    }

    @Transactional
    public CheckoutResponseDTO checkout(CheckoutRequestDTO request) {
        validateRequest(request);

        Customer customer = resolveCustomer(request);
        Long shopId = request.getShopId();

        long productSubtotalAmount = 0L;
        long serviceSubtotalAmount = 0L;
        Long orderId = null;
        String orderCode = null;
        List<Long> bookingIds = new ArrayList<>();

        if (hasProducts(request)) {
            ProductOrderBuildResult orderResult = buildAndSaveOrder(request, customer);
            orderId = orderResult.orderId();
            orderCode = orderResult.orderCode();
            productSubtotalAmount = orderResult.subtotalAmount();
        }

        if (hasServices(request)) {
            ServiceBookingBuildResult serviceResult = buildAndSaveServiceBookings(request, customer);
            List<Long> serviceBookingIds = serviceResult.bookingIds();
            bookingIds.addAll(serviceBookingIds);
            serviceSubtotalAmount = serviceResult.subtotalAmount();
        }

        long shippingFee = hasProducts(request) ? valueOrZero(request.getShippingFee()) : 0L;
        long pickupFee = hasServices(request) ? valueOrZero(request.getPickupFee()) : 0L;
        long discountAmount = valueOrZero(request.getDiscountAmount());
        long subtotalAmount = productSubtotalAmount + serviceSubtotalAmount;
        long totalAmount = subtotalAmount + shippingFee + pickupFee - discountAmount;
        ensureNonNegative(totalAmount, "Tổng thanh toán không được nhỏ hơn 0");

        return new CheckoutResponseDTO(
                orderId,
                orderCode,
                bookingIds,
                productSubtotalAmount,
                serviceSubtotalAmount,
                subtotalAmount,
                shippingFee,
            pickupFee,
                discountAmount,
                totalAmount
        );
    }

    private ProductOrderBuildResult buildAndSaveOrder(CheckoutRequestDTO request, Customer customer) {
        List<ProductOrderDTO> productOrders = normalizeProductOrders(request.getProductOrders());
        Map<Long, Product> productsById = loadProductsById(request.getShopId(), productOrders);

        List<OrderItem> items = productOrders.stream()
                .map(dto -> toOrderItem(request.getShopId(), productsById, dto))
                .toList();

        long subtotalAmount = items.stream().mapToLong(item -> item.getAmount() != null ? item.getAmount() : 0L).sum();

        CustomerOrder order = new CustomerOrder();
        order.setShopId(request.getShopId());
        order.setUserId(customer.getUserId());
        order.setCustomerId(customer.getId());
        order.setOrderCode(null);
        order.setStatus(OrderStatus.PENDING);
        order.setSource(OrderSource.ONLINE);
        order.setSubtotalAmount(subtotalAmount);
        order.setShippingFee(valueOrZero(request.getShippingFee()));
        order.setDiscountAmount(valueOrZero(request.getDiscountAmount()));
        long orderTotalAmount = subtotalAmount + valueOrZero(request.getShippingFee()) - valueOrZero(request.getDiscountAmount());
        ensureNonNegative(orderTotalAmount, "Tổng tiền đơn hàng không được nhỏ hơn 0");
        order.setTotalAmount(orderTotalAmount);
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setShippingAddress(request.getShippingAddress());
        order.setNote(request.getNote());

        CustomerOrder savedOrder = orderRepository.save(order);
        savedOrder.setOrderCode(formatOrderCode(savedOrder.getId()));
        savedOrder = orderRepository.save(savedOrder);

        for (OrderItem item : items) {
            item.setOrderId(savedOrder.getId());
        }
        orderItemRepository.saveAll(items);

        return new ProductOrderBuildResult(savedOrder.getId(), savedOrder.getOrderCode(), subtotalAmount);
    }

    private ServiceBookingBuildResult buildAndSaveServiceBookings(CheckoutRequestDTO request, Customer customer) {
        List<ServiceBookingDTO> serviceBookings = normalizeServiceBookings(request.getServiceBookings());
        Map<Long, Service> servicesById = loadServicesById(request.getShopId(), serviceBookings);
        Map<Long, Pet> petsById = loadPetsById(serviceBookings);

        List<Long> bookingIds = new ArrayList<>();
        long subtotalAmount = 0L;

        for (ServiceBookingDTO dto : serviceBookings) {
            Service service = servicesById.get(dto.getServiceId());
            Pet pet = petsById.get(dto.getPetId());

            Booking booking = new Booking();
            booking.setShopId(request.getShopId());
            booking.setUserId(customer.getUserId());
            booking.setCustomerId(customer.getId());
            booking.setPetId(pet.getId());
            booking.setStartAt(dto.getBookingDate().atTime(dto.getBookingTime()).atZone(BUSINESS_ZONE).toOffsetDateTime());
            booking.setEndAt(booking.getStartAt().plusMinutes(service.getDurationMin()));
            booking.setStatus(BookingStatus.DRAFT);
            booking.setSource(BookingSource.CUSTOMER);
            booking.setNote(mergeNotes(request.getNote(), dto.getNote()));
            booking.setCreatedBy(customer.getUserId());

            Booking savedBooking = bookingRepository.save(booking);

            BookingPet bookingPet = new BookingPet();
            bookingPet.setId(new BookingPetId(savedBooking.getId(), pet.getId()));
            bookingPetRepository.save(bookingPet);

            BookingItem bookingItem = new BookingItem();
            bookingItem.setShopId(request.getShopId());
            bookingItem.setBookingId(savedBooking.getId());
            bookingItem.setPetId(pet.getId());
            bookingItem.setItemType(BookingItemType.SERVICE);
            bookingItem.setRefId(service.getId());
            bookingItem.setQty(1);
            bookingItem.setUnitPrice(service.getBasePrice());
            bookingItem.setAmount(service.getBasePrice());
            bookingItemRepository.save(bookingItem);

            if (dto.getStaffUserId() != null) {
                validateBookingStaff(request.getShopId(), dto.getStaffUserId());
                insertBookingStaff(savedBooking.getId(), request.getShopId(), dto.getStaffUserId());
            }

            bookingIds.add(savedBooking.getId());
            subtotalAmount += valueOrZero(service.getBasePrice());
        }

        return new ServiceBookingBuildResult(bookingIds, subtotalAmount);
    }

    private void validateBookingStaff(Long shopId, Long staffUserId) {
        boolean activeMember = shopMemberRepository.existsByShopIdAndUserIdAndStatus(shopId, staffUserId, MemberStatus.ACTIVE);
        if (!activeMember) {
            throw new IllegalArgumentException("Nhân viên được chọn không thuộc shop hoặc đang không hoạt động");
        }
    }

    private void insertBookingStaff(Long bookingId, Long shopId, Long staffUserId) {
        entityManager.createNativeQuery("INSERT INTO booking_staff (shop_id, booking_id, user_id) VALUES (:shopId, :bookingId, :userId)")
                .setParameter("shopId", shopId)
                .setParameter("bookingId", bookingId)
                .setParameter("userId", staffUserId)
                .executeUpdate();
    }

    private List<ProductOrderDTO> normalizeProductOrders(List<ProductOrderDTO> productOrders) {
        if (productOrders == null) {
            return List.of();
        }
        return productOrders.stream().filter(Objects::nonNull).toList();
    }

    private List<ServiceBookingDTO> normalizeServiceBookings(List<ServiceBookingDTO> serviceBookings) {
        if (serviceBookings == null) {
            return List.of();
        }
        return serviceBookings.stream().filter(Objects::nonNull).toList();
    }

    private Map<Long, Product> loadProductsById(Long shopId, List<ProductOrderDTO> productOrders) {
        List<Long> productIds = productOrders.stream()
                .map(ProductOrderDTO::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Product> productsById = productRepository.findAllById(productIds).stream()
                .filter(product -> Objects.equals(product.getShopId(), shopId))
            .filter(product -> Boolean.TRUE.equals(product.getActive()))
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        for (Long productId : productIds) {
            if (!productsById.containsKey(productId)) {
                throw new IllegalArgumentException("Không tìm thấy sản phẩm " + productId + " trong shop hiện tại");
            }
        }

        return productsById;
    }

    private Map<Long, Service> loadServicesById(Long shopId, List<ServiceBookingDTO> serviceBookings) {
        List<Long> serviceIds = serviceBookings.stream()
                .map(ServiceBookingDTO::getServiceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Service> servicesById = serviceRepository.findAllById(serviceIds).stream()
                .filter(service -> Objects.equals(service.getShopId(), shopId))
            .filter(service -> Boolean.TRUE.equals(service.getActive()))
                .collect(Collectors.toMap(Service::getId, Function.identity()));

        for (Long serviceId : serviceIds) {
            if (!servicesById.containsKey(serviceId)) {
                throw new IllegalArgumentException("Không tìm thấy dịch vụ " + serviceId + " trong shop hiện tại");
            }
        }

        return servicesById;
    }

    private Map<Long, Pet> loadPetsById(List<ServiceBookingDTO> serviceBookings) {
        List<Long> petIds = serviceBookings.stream()
                .map(ServiceBookingDTO::getPetId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Pet> petsById = petRepository.findAllById(petIds).stream()
                .collect(Collectors.toMap(Pet::getId, Function.identity()));

        for (Long petId : petIds) {
            if (!petsById.containsKey(petId)) {
                throw new IllegalArgumentException("Không tìm thấy thú cưng " + petId);
            }
        }

        return petsById;
    }

    private OrderItem toOrderItem(Long shopId, Map<Long, Product> productsById, ProductOrderDTO dto) {
        Product product = productsById.get(dto.getProductId());
        int qty = dto.getQty() != null ? dto.getQty() : 1;
        long unitPrice = dto.getUnitPrice() != null ? dto.getUnitPrice() : valueOrZero(product.getPrice());
        long expectedUnitPrice = valueOrZero(product.getPrice());

        if (unitPrice != expectedUnitPrice) {
            throw new IllegalArgumentException("Đơn giá sản phẩm " + product.getId() + " không khớp với dữ liệu hiện tại");
        }

        OrderItem item = new OrderItem();
        item.setShopId(shopId);
        item.setProductId(product.getId());
        item.setQty(qty);
        item.setUnitPrice(expectedUnitPrice);
        item.setAmount(expectedUnitPrice * qty);
        return item;
    }

    private Customer resolveCustomer(CheckoutRequestDTO request) {
        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findByShopIdAndId(request.getShopId(), request.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng trong shop hiện tại"));
            if (request.getUserId() != null && customer.getUserId() != null && !Objects.equals(customer.getUserId(), request.getUserId())) {
                throw new IllegalArgumentException("userId không khớp với customerId");
            }
            return customer;
        }

        if (request.getUserId() != null) {
            return customerService.getOrCreateCustomerForUser(request.getShopId(), request.getUserId());
        }

        throw new IllegalArgumentException("Phải có customerId hoặc userId để checkout");
    }

    private void validateRequest(CheckoutRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Thông tin checkout không được để trống");
        }
        if (request.getShopId() == null || request.getShopId() <= 0) {
            throw new IllegalArgumentException("shopId không hợp lệ");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new IllegalArgumentException("userId không hợp lệ");
        }
        if (!hasProducts(request) && !hasServices(request)) {
            throw new IllegalArgumentException("Danh sách sản phẩm hoặc dịch vụ không được để trống");
        }
        if (hasProducts(request)) {
            requireText(request.getReceiverName(), "receiverName không được để trống khi có sản phẩm");
            requireText(request.getReceiverPhone(), "receiverPhone không được để trống khi có sản phẩm");
            requireText(request.getShippingAddress(), "shippingAddress không được để trống khi có sản phẩm");
        }
    }

    private boolean hasProducts(CheckoutRequestDTO request) {
        return request.getProductOrders() != null && !request.getProductOrders().isEmpty();
    }

    private boolean hasServices(CheckoutRequestDTO request) {
        return request.getServiceBookings() != null && !request.getServiceBookings().isEmpty();
    }

    private String formatOrderCode(Long id) {
        return id != null ? "ORD-" + String.format("%03d", id) : null;
    }

    private String mergeNotes(String requestNote, String itemNote) {
        List<String> notes = new ArrayList<>();
        if (requestNote != null && !requestNote.isBlank()) {
            notes.add(requestNote.trim());
        }
        if (itemNote != null && !itemNote.isBlank()) {
            notes.add(itemNote.trim());
        }
        return notes.isEmpty() ? null : String.join(" | ", notes);
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private long valueOrZero(Long value) {
        return value != null ? value : 0L;
    }

    private record ServiceBookingBuildResult(List<Long> bookingIds, long subtotalAmount) {}

    private void ensureNonNegative(long value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private record ProductOrderBuildResult(Long orderId, String orderCode, long subtotalAmount) {}
}