package com.exe101.order.service;

import com.exe101.common.IService;
import com.exe101.common.ScrollResponse;
import com.exe101.customer.entity.Customer;
import com.exe101.customer.repository.ICustomerRepository;
import com.exe101.order.dto.OrderDTO;
import com.exe101.order.dto.OrderItemDTO;
import com.exe101.order.dto.OrderListItemDTO;
import com.exe101.order.entity.CustomerOrder;
import com.exe101.order.entity.OrderItem;
import com.exe101.order.entity.OrderSource;
import com.exe101.order.entity.OrderStatus;
import com.exe101.order.entity.OrderStatusEvent;
import com.exe101.order.exception.OrderNotFound;
import com.exe101.order.exception.OrderValidationException;
import com.exe101.order.mapper.OrderMapper;
import com.exe101.order.repository.IOrderItemRepository;
import com.exe101.order.repository.IOrderRepository;
import com.exe101.order.repository.IOrderStatusEventRepository;
import com.exe101.product.entity.Product;
import com.exe101.product.repository.IProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService implements IService<CustomerOrder, OrderDTO, Long> {

    private static final int MAX_SCROLL_SIZE = 50;

    private final IOrderRepository orderRepository;
    private final IOrderItemRepository orderItemRepository;
    private final IOrderStatusEventRepository orderStatusEventRepository;
    private final ICustomerRepository customerRepository;
    private final IProductRepository productRepository;

    @Override
    public List<OrderDTO> getAll() {
        return toDTOs(orderRepository.findAll());
    }

    public ScrollResponse<OrderListItemDTO> getAllForScroll(
            Long shopId,
            Long customerId,
            OrderStatus status,
            OrderSource source,
            Long cursor,
            int size
    ) {
        int normalizedSize = Math.min(Math.max(size, 1), MAX_SCROLL_SIZE);
        Long normalizedCursor = cursor != null && cursor > 0 ? cursor : null;

        List<CustomerOrder> orders = orderRepository.findForScroll(
                shopId,
                customerId,
                status,
                source,
                normalizedCursor,
                PageRequest.of(0, normalizedSize + 1)
        );

        boolean hasNext = orders.size() > normalizedSize;
        List<CustomerOrder> pageOrders = orders.stream()
                .limit(normalizedSize)
                .toList();
        List<OrderListItemDTO> content = toListItemDTOs(pageOrders);
        Long nextCursor = hasNext && !content.isEmpty()
                ? content.get(content.size() - 1).getId()
                : null;

        return ScrollResponse.of(content, normalizedSize, nextCursor, hasNext);
    }

    @Override
    public OrderDTO getById(Long id) {
        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFound("OrderNotFound", "Order not found"));
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        Map<Long, Product> productsById = loadProductsById(items);
        return OrderMapper.toDTO(order, toItemDTOs(items, productsById));
    }

    @Override
    @Transactional
    public OrderDTO create(OrderDTO dto) {
        CustomerOrder entity = OrderMapper.toEntity(dto);
        List<OrderItem> items = buildItems(dto.getItems(), entity.getShopId(), null);
        applyTotals(entity, items, dto);

        CustomerOrder saved = orderRepository.save(entity);
        if (isBlank(saved.getOrderCode())) {
            saved.setOrderCode(formatOrderCode(saved.getId()));
            saved = orderRepository.save(saved);
        }

        for (OrderItem item : items) {
            item.setOrderId(saved.getId());
        }
        orderItemRepository.saveAll(items);
        createStatusEvent(saved.getShopId(), saved.getId(), null, saved.getStatus());

        return getById(saved.getId());
    }

    @Override
    @Transactional
    public OrderDTO update(Long id, OrderDTO dto) {
        CustomerOrder entity = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFound("OrderNotFound", "Order not found"));
        OrderStatus previousStatus = entity.getStatus();

        OrderMapper.updateEntity(entity, dto);
        if (dto.getItems() != null) {
            orderItemRepository.deleteByOrderId(id);
            List<OrderItem> replacementItems = buildItems(dto.getItems(), entity.getShopId(), id);
            applyTotals(entity, replacementItems, dto);
            orderItemRepository.saveAll(replacementItems);
        } else {
            recalculateTotal(entity);
        }

        CustomerOrder saved = orderRepository.save(entity);
        if (!Objects.equals(previousStatus, saved.getStatus())) {
            createStatusEvent(saved.getShopId(), saved.getId(), previousStatus, saved.getStatus());
        }

        return getById(saved.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new OrderNotFound("OrderNotFound", "Order not found");
        }
        orderRepository.deleteById(id);
    }

    private List<OrderDTO> toDTOs(List<CustomerOrder> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = orders.stream().map(CustomerOrder::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrderId = orderItemRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        Map<Long, Product> productsById = loadProductsById(
                itemsByOrderId.values().stream().flatMap(List::stream).toList()
        );

        return orders.stream()
                .map(order -> OrderMapper.toDTO(
                        order,
                        toItemDTOs(itemsByOrderId.getOrDefault(order.getId(), Collections.emptyList()), productsById)
                ))
                .toList();
    }

    private List<OrderListItemDTO> toListItemDTOs(List<CustomerOrder> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = orders.stream().map(CustomerOrder::getId).toList();
        List<Long> customerIds = orders.stream()
                .map(CustomerOrder::getCustomerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Customer> customersById = customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        Map<Long, List<OrderItem>> itemsByOrderId = orderItemRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        Map<Long, Product> productsById = loadProductsById(
                itemsByOrderId.values().stream().flatMap(List::stream).toList()
        );

        return orders.stream()
                .map(order -> OrderMapper.toListItemDTO(
                        order,
                        customersById.get(order.getCustomerId()),
                        toItemDTOs(itemsByOrderId.getOrDefault(order.getId(), Collections.emptyList()), productsById),
                        toStatusLabel(order.getStatus())
                ))
                .toList();
    }

    private List<OrderItem> buildItems(List<OrderItemDTO> itemDtos, Long shopId, Long orderId) {
        if (itemDtos == null || itemDtos.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = itemDtos.stream()
                .map(OrderItemDTO::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Product> productsById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return itemDtos.stream()
                .map(dto -> buildItem(dto, shopId, orderId, productsById))
                .toList();
    }

    private OrderItem buildItem(
            OrderItemDTO dto,
            Long shopId,
            Long orderId,
            Map<Long, Product> productsById
    ) {
        if (dto.getProductId() == null) {
            throw new OrderValidationException("OrderProductRequired", "Order item productId is required");
        }

        Product product = productsById.get(dto.getProductId());
        if (product == null || !Objects.equals(product.getShopId(), shopId)) {
            throw new OrderValidationException("OrderProductInvalid", "Order item product does not belong to shop");
        }

        int qty = dto.getQty() != null ? dto.getQty() : 1;
        if (qty <= 0) {
            throw new OrderValidationException("OrderItemQtyInvalid", "Order item qty must be greater than 0");
        }

        long unitPrice = dto.getUnitPrice() != null ? dto.getUnitPrice() : product.getPrice();
        if (unitPrice < 0) {
            throw new OrderValidationException("OrderItemPriceInvalid", "Order item unitPrice must be >= 0");
        }

        OrderItem item = new OrderItem();
        item.setShopId(shopId);
        item.setOrderId(orderId);
        item.setProductId(dto.getProductId());
        item.setQty(qty);
        item.setUnitPrice(unitPrice);
        item.setAmount(unitPrice * qty);
        return item;
    }

    private void applyTotals(CustomerOrder entity, List<OrderItem> items, OrderDTO dto) {
        long subtotal = !items.isEmpty()
                ? items.stream().mapToLong(OrderItem::getAmount).sum()
                : valueOrZero(dto.getSubtotalAmount());
        entity.setSubtotalAmount(subtotal);
        entity.setShippingFee(valueOrZero(dto.getShippingFee()));
        entity.setDiscountAmount(valueOrZero(dto.getDiscountAmount()));
        recalculateTotal(entity);
    }

    private void recalculateTotal(CustomerOrder entity) {
        long total = valueOrZero(entity.getSubtotalAmount())
                + valueOrZero(entity.getShippingFee())
                - valueOrZero(entity.getDiscountAmount());
        if (total < 0) {
            throw new OrderValidationException("OrderTotalInvalid", "Order totalAmount must be >= 0");
        }
        entity.setTotalAmount(total);
    }

    private Map<Long, Product> loadProductsById(List<OrderItem> items) {
        List<Long> productIds = items.stream()
                .map(OrderItem::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private List<OrderItemDTO> toItemDTOs(List<OrderItem> items, Map<Long, Product> productsById) {
        return items.stream()
                .map(item -> {
                    Product product = productsById.get(item.getProductId());
                    return new OrderItemDTO(
                            item.getId(),
                            item.getShopId(),
                            item.getOrderId(),
                            item.getProductId(),
                            product != null ? product.getName() : "Product #" + item.getProductId(),
                            item.getQty(),
                            item.getUnitPrice(),
                            item.getAmount(),
                            item.getCreatedAt()
                    );
                })
                .toList();
    }

    private void createStatusEvent(Long shopId, Long orderId, OrderStatus fromStatus, OrderStatus toStatus) {
        OrderStatusEvent event = new OrderStatusEvent();
        event.setShopId(shopId);
        event.setOrderId(orderId);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        orderStatusEventRepository.save(event);
    }

    private String formatOrderCode(Long id) {
        return id != null ? "ORD-" + String.format("%03d", id) : null;
    }

    private String toStatusLabel(OrderStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case PENDING -> "Cho xac nhan";
            case CONFIRMED -> "Da xac nhan";
            case PACKING -> "Dang dong goi";
            case SHIPPING -> "Dang giao";
            case COMPLETED -> "Hoan thanh";
            case CANCELLED -> "Da huy";
        };
    }

    private long valueOrZero(Long value) {
        return value != null ? value : 0L;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
