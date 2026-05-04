package com.exe101.invoice.service;

import com.exe101.booking.entity.Booking;
import com.exe101.booking.entity.BookingItem;
import com.exe101.booking.entity.BookingItemType;
import com.exe101.booking.entity.BookingStatus;
import com.exe101.booking.repository.IBookingItemRepository;
import com.exe101.booking.repository.IBookingRepository;
import com.exe101.common.IService;
import com.exe101.invoice.dto.InvoiceDTO;
import com.exe101.invoice.dto.InvoiceLineDTO;
import com.exe101.invoice.entity.Invoice;
import com.exe101.invoice.entity.InvoiceLine;
import com.exe101.invoice.exception.InvoiceNotFound;
import com.exe101.invoice.exception.InvoiceValidationException;
import com.exe101.invoice.mapper.InvoiceLineMapper;
import com.exe101.invoice.mapper.InvoiceMapper;
import com.exe101.invoice.repository.IInvoiceLineRepository;
import com.exe101.invoice.repository.IInvoiceRepository;
import com.exe101.order.entity.CustomerOrder;
import com.exe101.order.entity.OrderItem;
import com.exe101.order.exception.OrderNotFound;
import com.exe101.order.repository.IOrderItemRepository;
import com.exe101.order.repository.IOrderRepository;
import com.exe101.product.entity.Product;
import com.exe101.product.repository.IProductRepository;
import com.exe101.service_shop.repository.IServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService implements IService<Invoice, InvoiceDTO, Long> {

    private final IInvoiceRepository invoiceRepository;
    private final IInvoiceLineRepository invoiceLineRepository;
    private final IOrderRepository orderRepository;
    private final IOrderItemRepository orderItemRepository;
    private final IBookingRepository bookingRepository;
    private final IBookingItemRepository bookingItemRepository;
    private final IProductRepository productRepository;
    private final IServiceRepository serviceRepository;

    @Override
    public List<InvoiceDTO> getAll() {
        return toDTOs(invoiceRepository.findAll());
    }

    public List<InvoiceDTO> getAllByShopId(Long shopId) {
        return toDTOs(invoiceRepository.findByShopIdOrderByIdDesc(shopId));
    }

    @Override
    @Transactional
    public InvoiceDTO getById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFound("InvoiceNotFound", "Không tìm thấy hóa đơn"));
        return toDTO(invoice);
    }

    @Transactional
    public InvoiceDTO getById(Long shopId, Long id) {
        Invoice invoice = invoiceRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new InvoiceNotFound("InvoiceNotFound", "Không tìm thấy hóa đơn"));
        return toDTO(invoice);
    }

    @Transactional
    public InvoiceDTO getByOrderId(Long shopId, Long orderId) {
        Invoice invoice = invoiceRepository.findFirstByShopIdAndOrderIdOrderByIdDesc(shopId, orderId)
                .orElseThrow(() -> new InvoiceNotFound(
                        "InvoiceNotFound",
                        "Không tìm thấy hóa đơn của đơn hàng"
                ));
        return toDTO(invoice);
    }

    @Transactional
    public InvoiceDTO getByBookingId(Long shopId, Long bookingId) {
        Booking booking = bookingRepository.findByIdAndShopId(bookingId, shopId)
                .orElseThrow(() -> new InvoiceValidationException(
                        "InvoiceBookingNotFound",
                        "Không tìm thấy lịch hẹn"
                ));
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new InvoiceValidationException(
                    "InvoiceBookingNotCompleted",
                    "Chỉ có thể xem hóa đơn khi lịch hẹn đã hoàn thành"
            );
        }

        Invoice invoice = invoiceRepository.findFirstByShopIdAndBookingIdOrderByIdDesc(shopId, bookingId)
                .orElseThrow(() -> new InvoiceNotFound(
                        "InvoiceNotFound",
                        "Không tìm thấy hóa đơn của lịch hẹn"
                ));
        return toDTO(invoice);
    }

    @Override
    @Transactional
    public InvoiceDTO create(InvoiceDTO dto) {
        assertSingleSource(dto);

        Invoice entity = InvoiceMapper.toEntity(dto);
        List<InvoiceLine> lines = prepareLines(entity, dto.getLines());

        Invoice savedInvoice = invoiceRepository.save(entity);
        saveLines(savedInvoice, lines);
        return toDTO(savedInvoice);
    }

    @Override
    @Transactional
    public InvoiceDTO update(Long id, InvoiceDTO dto) {
        assertSingleSource(dto);

        Invoice entity = invoiceRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFound("InvoiceNotFound", "Không tìm thấy hóa đơn"));
        InvoiceMapper.updateEntity(entity, dto);

        List<InvoiceLine> lines = prepareLines(entity, dto.getLines());
        Invoice savedInvoice = invoiceRepository.save(entity);
        invoiceLineRepository.deleteByInvoiceId(savedInvoice.getId());
        saveLines(savedInvoice, lines);
        return toDTO(savedInvoice);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!invoiceRepository.existsById(id)) {
            throw new InvoiceNotFound("InvoiceNotFound", "Không tìm thấy hóa đơn");
        }
        invoiceRepository.deleteById(id);
    }

    private List<InvoiceDTO> toDTOs(List<Invoice> invoices) {
        if (invoices.isEmpty()) {
            return List.of();
        }

        List<Long> invoiceIds = invoices.stream().map(Invoice::getId).toList();
        Map<Long, List<InvoiceLineDTO>> linesByInvoiceId = invoiceLineRepository.findByInvoiceIdIn(invoiceIds)
                .stream()
                .collect(Collectors.groupingBy(
                        InvoiceLine::getInvoiceId,
                        Collectors.mapping(InvoiceLineMapper::toDTO, Collectors.toList())
                ));

        return invoices.stream()
                .map(invoice -> InvoiceMapper.toDTO(
                        invoice,
                        linesByInvoiceId.getOrDefault(invoice.getId(), Collections.emptyList())
                ))
                .toList();
    }

    private InvoiceDTO toDTO(Invoice invoice) {
        List<InvoiceLine> invoiceLines = getOrCreateLines(invoice);
        List<InvoiceLineDTO> lines = invoiceLines
                .stream()
                .map(InvoiceLineMapper::toDTO)
                .toList();
        return InvoiceMapper.toDTO(invoice, lines);
    }

    private List<InvoiceLine> getOrCreateLines(Invoice invoice) {
        List<InvoiceLine> existingLines = invoiceLineRepository.findByInvoiceId(invoice.getId());
        if (!existingLines.isEmpty() || invoice.getId() == null) {
            return existingLines;
        }
        if (invoice.getOrderId() == null && invoice.getBookingId() == null) {
            return existingLines;
        }

        List<InvoiceLine> generatedLines = prepareLines(invoice, null);
        saveLines(invoice, generatedLines);
        return generatedLines;
    }

    private List<InvoiceLine> prepareLines(Invoice invoice, List<InvoiceLineDTO> requestedLines) {
        if (invoice.getOrderId() != null) {
            CustomerOrder order = orderRepository.findByIdAndShopId(invoice.getOrderId(), invoice.getShopId())
                    .orElseThrow(() -> new OrderNotFound("OrderNotFound", "Không tìm thấy đơn hàng"));
            invoice.setCustomerId(order.getCustomerId());
            invoice.setTotalAmount(order.getTotalAmount());
            return buildLinesFromOrder(invoice);
        }

        if (invoice.getBookingId() != null) {
            Booking booking = bookingRepository.findByIdAndShopId(invoice.getBookingId(), invoice.getShopId())
                    .orElseThrow(() -> new InvoiceValidationException(
                            "InvoiceBookingNotFound",
                            "Không tìm thấy lịch hẹn"
                    ));
            invoice.setCustomerId(booking.getCustomerId());
            return buildLinesFromBooking(invoice);
        }

        return buildManualLines(invoice, requestedLines);
    }

    private List<InvoiceLine> buildLinesFromOrder(Invoice invoice) {
        List<OrderItem> items = orderItemRepository.findByOrderId(invoice.getOrderId());
        if (items.isEmpty()) {
            throw new InvoiceValidationException(
                    "InvoiceOrderItemsRequired",
                    "Đơn hàng không có sản phẩm để tạo dòng hóa đơn"
            );
        }

        Map<Long, String> productNamesById = loadProductNamesById(
                invoice.getShopId(),
                items.stream().map(OrderItem::getProductId).toList()
        );

        return items.stream()
                .map(item -> {
                    InvoiceLine line = new InvoiceLine();
                    line.setShopId(invoice.getShopId());
                    line.setLineType("PRODUCT");
                    line.setRefId(item.getProductId());
                    line.setItemName(productNamesById.getOrDefault(
                            item.getProductId(),
                            "Sản phẩm #" + item.getProductId()
                    ));
                    line.setQty(item.getQty());
                    line.setUnitPrice(item.getUnitPrice());
                    line.setAmount(item.getAmount());
                    return line;
                })
                .toList();
    }

    private List<InvoiceLine> buildLinesFromBooking(Invoice invoice) {
        List<BookingItem> items = bookingItemRepository.findByBookingId(invoice.getBookingId());
        if (items.isEmpty()) {
            throw new InvoiceValidationException(
                    "InvoiceBookingItemsRequired",
                    "Lịch hẹn không có dịch vụ hoặc sản phẩm để tạo dòng hóa đơn"
            );
        }

        long totalAmount = items.stream().mapToLong(item -> item.getAmount() != null ? item.getAmount() : 0L).sum();
        invoice.setTotalAmount(totalAmount);

        Map<Long, String> productNamesById = loadProductNamesById(
                invoice.getShopId(),
                items.stream()
                        .filter(item -> item.getItemType() == BookingItemType.PRODUCT)
                        .map(BookingItem::getRefId)
                        .toList()
        );
        Map<Long, String> serviceNamesById = loadServiceNamesById(
                invoice.getShopId(),
                items.stream()
                        .filter(item -> item.getItemType() == BookingItemType.SERVICE)
                        .map(BookingItem::getRefId)
                        .toList()
        );

        return items.stream()
                .map(item -> {
                    InvoiceLine line = new InvoiceLine();
                    line.setShopId(invoice.getShopId());
                    line.setLineType(item.getItemType().name());
                    line.setRefId(item.getRefId());
                    line.setItemName(resolveBookingItemName(item, productNamesById, serviceNamesById));
                    line.setQty(item.getQty());
                    line.setUnitPrice(item.getUnitPrice());
                    line.setAmount(item.getAmount());
                    return line;
                })
                .toList();
    }

    private List<InvoiceLine> buildManualLines(Invoice invoice, List<InvoiceLineDTO> requestedLines) {
        if (requestedLines == null || requestedLines.isEmpty()) {
            return List.of();
        }

        List<InvoiceLine> lines = requestedLines.stream()
                .map(lineDto -> {
                    InvoiceLine line = InvoiceLineMapper.toEntity(lineDto);
                    line.setShopId(invoice.getShopId());
                    return line;
                })
                .toList();

        long totalAmount = lines.stream().mapToLong(line -> line.getAmount() != null ? line.getAmount() : 0L).sum();
        invoice.setTotalAmount(totalAmount);
        return lines;
    }

    private void saveLines(Invoice invoice, List<InvoiceLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }

        for (InvoiceLine line : lines) {
            line.setShopId(invoice.getShopId());
            line.setInvoiceId(invoice.getId());
        }
        invoiceLineRepository.saveAll(lines);
    }

    private void assertSingleSource(InvoiceDTO dto) {
        if (dto.getBookingId() != null && dto.getOrderId() != null) {
            throw new InvoiceValidationException(
                    "InvoiceSingleSourceRequired",
                    "Hóa đơn chỉ được gắn với một đơn hàng hoặc một lịch hẹn"
            );
        }
    }

    private Map<Long, String> loadProductNamesById(Long shopId, List<Long> rawProductIds) {
        List<Long> productIds = rawProductIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }

        return productRepository.findAllById(productIds).stream()
                .filter(product -> Objects.equals(product.getShopId(), shopId))
                .collect(Collectors.toMap(Product::getId, Product::getName));
    }

    private Map<Long, String> loadServiceNamesById(Long shopId, List<Long> rawServiceIds) {
        List<Long> serviceIds = rawServiceIds.stream()
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
                        com.exe101.service_shop.entity.Service::getName
                ));
    }

    private String resolveBookingItemName(
            BookingItem item,
            Map<Long, String> productNamesById,
            Map<Long, String> serviceNamesById
    ) {
        if (item.getRefId() == null) {
            return item.getItemType().name();
        }
        if (item.getItemType() == BookingItemType.PRODUCT) {
            return productNamesById.getOrDefault(item.getRefId(), "Sản phẩm #" + item.getRefId());
        }
        if (item.getItemType() == BookingItemType.SERVICE) {
            return serviceNamesById.getOrDefault(item.getRefId(), "Dịch vụ #" + item.getRefId());
        }
        return item.getItemType().name() + " #" + item.getRefId();
    }
}
