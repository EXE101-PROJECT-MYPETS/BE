package com.exe101.commission.service;

import com.exe101.commission.dto.*;
import com.exe101.commission.entity.*;
import com.exe101.commission.repository.IPlatformCommissionInvoiceItemRepository;
import com.exe101.commission.repository.IPlatformCommissionInvoiceRepository;
import com.exe101.commission.repository.IPlatformCommissionRepository;
import com.exe101.common.PageResponse;
import com.exe101.email.service.EmailService;
import com.exe101.notification.dto.NotificationTargetType;
import com.exe101.notification.dto.NotificationType;
import com.exe101.notification.service.NotificationService;
import com.exe101.order.entity.CustomerOrder;
import com.exe101.order.repository.IOrderRepository;
import com.exe101.shop.entity.Shop;
import com.exe101.shop.entity.ShopRole;
import com.exe101.shop.repository.IShopRepository;
import com.exe101.shopMember.dto.ShopMemberDTO;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.repository.IShopMemberRepository;
import com.exe101.subscription.config.SubscriptionPaymentProperties;
import com.exe101.subscription.dto.SepayWebhookRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommissionInvoiceService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<CommissionInvoiceStatus> UNPAID_INVOICE_STATUSES = List.of(
            CommissionInvoiceStatus.PENDING,
            CommissionInvoiceStatus.OVERDUE
    );
    private static final List<CommissionStatus> DEBT_COMMISSION_STATUSES = List.of(
            CommissionStatus.PENDING,
            CommissionStatus.INVOICED
    );

    private final IPlatformCommissionRepository commissionRepository;
    private final IPlatformCommissionInvoiceRepository invoiceRepository;
    private final IPlatformCommissionInvoiceItemRepository invoiceItemRepository;
    private final IOrderRepository orderRepository;
    private final IShopRepository shopRepository;
    private final IShopMemberRepository shopMemberRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final SubscriptionPaymentProperties paymentProperties;

    @Value("${platform.commission-min-invoice-amount:100000}")
    private long minimumInvoiceAmount;

    @Transactional(readOnly = true)
    public CommissionSummaryDTO getSummary(Long shopId, Long currentUserId) {
        assertActiveShopMember(shopId, currentUserId);

        long unpaidInvoiceAmount = valueOrZero(invoiceRepository.sumUnpaidAmountByShopId(
                shopId,
                UNPAID_INVOICE_STATUSES
        ));
        long unpaidInvoiceCount = invoiceRepository.countByShopIdAndStatusIn(shopId, UNPAID_INVOICE_STATUSES);
        long pendingCommissionAmount = valueOrZero(commissionRepository.sumCommissionAmountByShopIdAndStatusIn(
                shopId,
                List.of(CommissionStatus.PENDING)
        ));
        long pendingCommissionCount = commissionRepository.countByShopIdAndStatus(shopId, CommissionStatus.PENDING);
        long paidInvoiceAmount = valueOrZero(invoiceRepository.sumAmountByShopIdAndStatus(
                shopId,
                CommissionInvoiceStatus.PAID
        ));
        long paidInvoiceCount = invoiceRepository.countByShopIdAndStatus(shopId, CommissionInvoiceStatus.PAID);
        long overdueInvoiceAmount = valueOrZero(invoiceRepository.sumAmountByShopIdAndStatus(
                shopId,
                CommissionInvoiceStatus.OVERDUE
        ));
        long overdueInvoiceCount = invoiceRepository.countByShopIdAndStatus(shopId, CommissionInvoiceStatus.OVERDUE);
        List<PlatformCommissionInvoice> unpaidInvoices = invoiceRepository.findUnpaidByShopId(
                shopId,
                UNPAID_INVOICE_STATUSES
        );
        OffsetDateTime nextDueDate = unpaidInvoices.isEmpty() ? null : unpaidInvoices.get(0).getDueAt();
        Long nearestUnpaidInvoiceId = unpaidInvoices.isEmpty() ? null : unpaidInvoices.get(0).getId();
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate currentPeriodFrom = resolveCurrentPeriodFrom(today);
        LocalDate currentPeriodTo = resolveCurrentPeriodTo(today);
        LocalDate nextInvoiceDate = resolveNextInvoiceDate(today);
        boolean hasUnpaidInvoice = unpaidInvoiceAmount > 0;

        return new CommissionSummaryDTO(
                hasUnpaidInvoice,
                unpaidInvoiceAmount,
                unpaidInvoiceCount,
                pendingCommissionAmount,
                pendingCommissionCount,
                paidInvoiceAmount,
                paidInvoiceCount,
                overdueInvoiceAmount,
                overdueInvoiceCount,
                nearestUnpaidInvoiceId,
                currentPeriodFrom,
                currentPeriodTo,
                nextInvoiceDate,
                nextDueDate,
                buildSummaryMessage(hasUnpaidInvoice, unpaidInvoiceAmount, nextDueDate)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<CommissionDTO> getShopCommissions(
            Long shopId,
            Long currentUserId,
            CommissionStatus status,
            CommissionSourceType sourceType,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    ) {
        assertActiveShopMember(shopId, currentUserId);
        OffsetDateTime fromDateTime = from != null ? from.atStartOfDay(BUSINESS_ZONE).toOffsetDateTime() : null;
        OffsetDateTime toDateTime = to != null ? to.plusDays(1).atStartOfDay(BUSINESS_ZONE).toOffsetDateTime() : null;
        Page<CommissionDTO> commissions = commissionRepository
                .findShopCommissionsForFilter(
                        shopId,
                        status,
                        sourceType,
                        fromDateTime,
                        toDateTime,
                        PageRequest.of(page, size)
                )
                .map(this::toCommissionDTO);
        return PageResponse.from(commissions);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommissionInvoiceDTO> getShopInvoices(Long shopId, Long currentUserId, int page, int size) {
        assertActiveShopMember(shopId, currentUserId);
        Page<CommissionInvoiceDTO> invoices = invoiceRepository
                .findByShopIdOrderByCreatedAtDescIdDesc(shopId, PageRequest.of(page, size))
                .map(invoice -> toInvoiceDTO(invoice, false));
        return PageResponse.from(invoices);
    }

    @Transactional(readOnly = true)
    public CommissionInvoiceDTO getShopInvoice(Long shopId, Long currentUserId, Long invoiceId) {
        assertActiveShopMember(shopId, currentUserId);
        PlatformCommissionInvoice invoice = invoiceRepository.findByShopIdAndId(shopId, invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn phí nền tảng"));
        return toInvoiceDTO(invoice, true);
    }

    @Transactional(readOnly = true)
    public CommissionPaymentInfoDTO getShopInvoicePaymentInfo(Long shopId, Long currentUserId, Long invoiceId) {
        assertActiveShopMember(shopId, currentUserId);
        PlatformCommissionInvoice invoice = invoiceRepository.findByShopIdAndId(shopId, invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn phí nền tảng"));
        return new CommissionPaymentInfoDTO(
                invoice.getId(),
                invoice.getInvoiceCode(),
                invoice.getTotalCommissionAmount(),
                invoice.getBankCode(),
                invoice.getAccountNumber(),
                invoice.getAccountName(),
                invoice.getTransferContent(),
                invoice.getQrUrl(),
                invoice.getDueAt()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<CommissionDTO> getAdminCommissions(int page, int size) {
        Page<CommissionDTO> commissions = commissionRepository
                .findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(page, size))
                .map(this::toCommissionDTO);
        return PageResponse.from(commissions);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommissionInvoiceDTO> getAdminInvoices(int page, int size) {
        Page<CommissionInvoiceDTO> invoices = invoiceRepository
                .findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(page, size))
                .map(invoice -> toInvoiceDTO(invoice, false));
        return PageResponse.from(invoices);
    }

    @Transactional
    public List<PlatformCommissionInvoice> generateInvoicesForClosedPeriodIfNeeded() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (today.getDayOfMonth() == 16) {
            return generateInvoiceForPeriod(today.withDayOfMonth(1), today.withDayOfMonth(15), null);
        }
        if (today.getDayOfMonth() == 1) {
            LocalDate previousMonth = today.minusMonths(1);
            return generateInvoiceForPeriod(
                    previousMonth.withDayOfMonth(16),
                    previousMonth.with(TemporalAdjusters.lastDayOfMonth()),
                    null
            );
        }
        return List.of();
    }

    @Transactional
    public List<PlatformCommissionInvoice> generateInvoiceForPeriod(
            LocalDate periodFrom,
            LocalDate periodTo,
            Long requestedShopId
    ) {
        validatePeriod(periodFrom, periodTo);
        OffsetDateTime toExclusive = periodTo.plusDays(1).atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
        List<Long> shopIds = requestedShopId != null
                ? List.of(requestedShopId)
                : commissionRepository.findShopIdsWithStatusBefore(CommissionStatus.PENDING, toExclusive);

        return shopIds.stream()
                .map(shopId -> generateInvoiceForShopPeriod(shopId, periodFrom, periodTo, toExclusive))
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    public CommissionInvoiceDTO markInvoicePaid(Long invoiceId) {
        PlatformCommissionInvoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn phí nền tảng"));
        return toInvoiceDTO(markInvoicePaidInternal(invoice), true);
    }

    @Transactional
    public CommissionInvoiceDTO cancelInvoice(Long invoiceId) {
        PlatformCommissionInvoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn phí nền tảng"));
        if (invoice.getStatus() == CommissionInvoiceStatus.PAID) {
            throw new IllegalArgumentException("Không thể hủy hóa đơn đã thanh toán");
        }
        if (invoice.getStatus() != CommissionInvoiceStatus.CANCELED) {
            invoice.setStatus(CommissionInvoiceStatus.CANCELED);
            invoice = invoiceRepository.save(invoice);
        }
        return toInvoiceDTO(invoice, true);
    }

    @Transactional
    public int markOverdueInvoices() {
        OffsetDateTime now = OffsetDateTime.now(BUSINESS_ZONE);
        List<PlatformCommissionInvoice> invoices = invoiceRepository.findOverdueCandidates(
                List.of(CommissionInvoiceStatus.PENDING),
                now
        );
        if (invoices.isEmpty()) {
            return 0;
        }
        invoices.forEach(invoice -> invoice.setStatus(CommissionInvoiceStatus.OVERDUE));
        invoiceRepository.saveAll(invoices);
        return invoices.size();
    }

    @Transactional
    public void handleSepayIpn(SepayWebhookRequest request) {
        if (request == null) {
            log.warn("Received empty SePay platform commission webhook");
            return;
        }
        if (!"in".equalsIgnoreCase(trimToEmpty(request.getTransferType()))) {
            return;
        }
        BigDecimal transferAmount = request.getTransferAmount();
        if (transferAmount == null || transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        String searchableContent = (trimToEmpty(request.getCode()) + " " + trimToEmpty(request.getContent())).trim();
        if (!StringUtils.hasText(searchableContent)) {
            return;
        }

        PlatformCommissionInvoice invoice = invoiceRepository.findLatestBySearchableContent(searchableContent)
                .orElse(null);
        if (invoice == null) {
            log.warn("Unmatched SePay platform commission webhook id={}", request.getId());
            return;
        }
        if (transferAmount.compareTo(BigDecimal.valueOf(invoice.getTotalCommissionAmount())) < 0) {
            log.warn(
                    "SePay platform commission amount mismatch. invoiceId={}, received={}, expected={}",
                    invoice.getId(),
                    transferAmount,
                    invoice.getTotalCommissionAmount()
            );
            return;
        }
        markInvoicePaidInternal(invoice);
    }

    public boolean isSepayWebhookAuthorized(Map<String, String> headers) {
        if (!paymentProperties.isSepayWebhookAuthEnabled()) {
            return true;
        }
        String configuredKey = paymentProperties.getSepayIpnSecretKey();
        if (!StringUtils.hasText(configuredKey)) {
            log.warn("SePay webhook auth is enabled but sepay.ipn-secret-key is blank");
            return false;
        }
        String actualKey = resolveWebhookSecret(headers);
        return StringUtils.hasText(actualKey) && configuredKey.trim().equals(actualKey.trim());
    }

    private PlatformCommissionInvoice generateInvoiceForShopPeriod(
            Long shopId,
            LocalDate periodFrom,
            LocalDate periodTo,
            OffsetDateTime toExclusive
    ) {
        if (invoiceRepository.findByShopIdAndPeriodFromAndPeriodTo(shopId, periodFrom, periodTo).isPresent()) {
            return null;
        }

        List<PlatformCommission> commissions = commissionRepository.findByShopIdAndStatusBeforeForUpdate(
                shopId,
                CommissionStatus.PENDING,
                toExclusive
        );
        if (commissions.isEmpty()) {
            return null;
        }

        long totalCommissionAmount = commissions.stream().mapToLong(c -> valueOrZero(c.getCommissionAmount())).sum();
        if (totalCommissionAmount < minimumInvoiceAmount) {
            return null;
        }

        long totalGrossAmount = commissions.stream().mapToLong(c -> valueOrZero(c.getGrossAmount())).sum();
        PlatformCommissionInvoice invoice = new PlatformCommissionInvoice();
        invoice.setShopId(shopId);
        invoice.setPeriodFrom(periodFrom);
        invoice.setPeriodTo(periodTo);
        invoice.setTotalGrossAmount(totalGrossAmount);
        invoice.setTotalCommissionAmount(totalCommissionAmount);
        invoice.setStatus(CommissionInvoiceStatus.PENDING);
        invoice.setInvoiceCode(buildInvoiceCode(shopId, periodFrom, periodTo));
        invoice.setTransferContent(invoice.getInvoiceCode());
        invoice.setBankCode(paymentProperties.getSepayBankCode());
        invoice.setAccountNumber(paymentProperties.getSepayAccountNumber());
        invoice.setAccountName(paymentProperties.getSepayAccountName());
        invoice.setQrUrl(buildQrUrl(invoice));
        invoice.setDueAt(resolveDueAt(periodTo));
        try {
            invoice = invoiceRepository.save(invoice);
        } catch (DataIntegrityViolationException ex) {
            return invoiceRepository
                    .findByShopIdAndPeriodFromAndPeriodTo(shopId, periodFrom, periodTo)
                    .orElseThrow(() -> ex);
        }

        PlatformCommissionInvoice savedInvoice = invoice;
        List<PlatformCommissionInvoiceItem> items = commissions.stream()
                .map(commission -> {
                    PlatformCommissionInvoiceItem item = new PlatformCommissionInvoiceItem();
                    item.setInvoiceId(savedInvoice.getId());
                    item.setCommissionId(commission.getId());
                    item.setCommissionAmount(commission.getCommissionAmount());
                    return item;
                })
                .toList();
        invoiceItemRepository.saveAll(items);

        commissionRepository.markInvoiced(
                commissions.stream().map(PlatformCommission::getId).toList(),
                CommissionStatus.INVOICED,
                OffsetDateTime.now(BUSINESS_ZONE)
        );
        publishInvoiceCreatedNotification(savedInvoice);
        sendInvoiceCreatedEmailsAfterCommit(savedInvoice);
        return savedInvoice;
    }

    private PlatformCommissionInvoice markInvoicePaidInternal(PlatformCommissionInvoice invoice) {
        if (invoice.getStatus() == CommissionInvoiceStatus.PAID) {
            return invoice;
        }
        if (invoice.getStatus() == CommissionInvoiceStatus.CANCELED) {
            throw new IllegalArgumentException("Không thể thanh toán hóa đơn đã hủy");
        }

        OffsetDateTime now = OffsetDateTime.now(BUSINESS_ZONE);
        invoice.setStatus(CommissionInvoiceStatus.PAID);
        invoice.setPaidAt(now);
        PlatformCommissionInvoice saved = invoiceRepository.save(invoice);
        List<Long> commissionIds = invoiceItemRepository.findCommissionIdsByInvoiceId(saved.getId());
        if (!commissionIds.isEmpty()) {
            commissionRepository.markCollected(commissionIds, CommissionStatus.COLLECTED, now);
        }
        return saved;
    }

    private void publishInvoiceCreatedNotification(PlatformCommissionInvoice invoice) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("invoiceId", invoice.getId());
        metadata.put("invoiceCode", invoice.getInvoiceCode());
        metadata.put("periodFrom", invoice.getPeriodFrom());
        metadata.put("periodTo", invoice.getPeriodTo());
        metadata.put("amount", invoice.getTotalCommissionAmount());
        metadata.put("dueAt", invoice.getDueAt());

        notificationService.publishToShop(
                invoice.getShopId(),
                NotificationType.PLATFORM_COMMISSION_INVOICE,
                NotificationTargetType.INVOICE,
                invoice.getId(),
                null,
                "Hóa đơn phí nền tảng đã được tạo",
                "Hóa đơn phí nền tảng kỳ " + invoice.getPeriodFrom() + " - " + invoice.getPeriodTo()
                        + " đã được tạo. Vui lòng thanh toán trước ngày " + invoice.getDueAt().toLocalDate() + ".",
                metadata
        );
    }

    private void sendInvoiceCreatedEmailsAfterCommit(PlatformCommissionInvoice invoice) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendInvoiceCreatedEmails(invoice);
                }
            });
            return;
        }
        sendInvoiceCreatedEmails(invoice);
    }

    private void sendInvoiceCreatedEmails(PlatformCommissionInvoice invoice) {
        try {
            Shop shop = shopRepository.findById(invoice.getShopId()).orElse(null);
            String shopName = shop != null ? shop.getName() : null;
            Map<String, String> recipients = new LinkedHashMap<>();

            List<ShopMemberDTO> owners = shopMemberRepository.findByShopIdAndRoleAndStatusForDisplay(
                    invoice.getShopId(),
                    ShopRole.OWNER,
                    MemberStatus.ACTIVE
            );
            owners.forEach(owner -> putRecipient(
                    recipients,
                    owner.getUserEmail(),
                    owner.getUserFullName()
            ));

            if (recipients.isEmpty() && shop != null) {
                putRecipient(recipients, shop.getEmail(), null);
            }

            recipients.forEach((email, ownerName) -> emailService.sendPlatformCommissionInvoiceCreated(
                    email,
                    ownerName,
                    shopName,
                    invoice.getInvoiceCode(),
                    invoice.getPeriodFrom(),
                    invoice.getPeriodTo(),
                    invoice.getTotalCommissionAmount(),
                    invoice.getDueAt(),
                    invoice.getBankCode(),
                    invoice.getAccountNumber(),
                    invoice.getAccountName(),
                    invoice.getTransferContent()
            ));
        } catch (Exception ex) {
            log.warn("Cannot send platform commission invoice email. invoiceId={}", invoice.getId(), ex);
        }
    }

    private void putRecipient(Map<String, String> recipients, String email, String ownerName) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        recipients.putIfAbsent(email.trim().toLowerCase(), ownerName);
    }

    private CommissionDTO toCommissionDTO(PlatformCommission commission) {
        InvoiceRef invoiceRef = resolveInvoiceRef(commission.getId());
        return new CommissionDTO(
                commission.getId(),
                commission.getShopId(),
                commission.getSourceType(),
                commission.getSourceId(),
                resolveSourceCode(commission),
                invoiceRef.invoiceId(),
                invoiceRef.invoiceCode(),
                commission.getCreatedAt(),
                commission.getGrossAmount(),
                commission.getDiscountAmount(),
                commission.getShippingFee(),
                commission.getCommissionBase(),
                commission.getCommissionRateBps(),
                commission.getCommissionAmount(),
                commission.getStatus(),
                commission.getCreatedAt(),
                commission.getInvoicedAt(),
                commission.getCollectedAt(),
                commission.getRefundedAt()
        );
    }

    public CommissionInvoiceDTO toInvoiceDTO(PlatformCommissionInvoice invoice, boolean includeItems) {
        List<CommissionInvoiceItemDTO> items = includeItems
                ? toInvoiceItemDTOs(invoice.getId())
                : List.of();
        return new CommissionInvoiceDTO(
                invoice.getId(),
                invoice.getShopId(),
                invoice.getInvoiceCode(),
                invoice.getPeriodFrom(),
                invoice.getPeriodTo(),
                invoice.getTotalGrossAmount(),
                invoice.getTotalCommissionAmount(),
                invoice.getStatus(),
                null,
                null,
                null,
                null,
                null,
                invoice.getCreatedAt(),
                invoice.getDueAt(),
                invoice.getPaidAt(),
                items
        );
    }

    private List<CommissionInvoiceItemDTO> toInvoiceItemDTOs(Long invoiceId) {
        List<PlatformCommissionInvoiceItem> items = invoiceItemRepository.findByInvoiceIdOrderByIdAsc(invoiceId);
        if (items.isEmpty()) {
            return List.of();
        }
        Map<Long, PlatformCommission> commissionsById = commissionRepository
                .findAllById(items.stream().map(PlatformCommissionInvoiceItem::getCommissionId).toList())
                .stream()
                .collect(Collectors.toMap(PlatformCommission::getId, Function.identity()));
        return items.stream()
                .map(item -> toInvoiceItemDTO(item, commissionsById.get(item.getCommissionId())))
                .toList();
    }

    private CommissionInvoiceItemDTO toInvoiceItemDTO(
            PlatformCommissionInvoiceItem item,
            PlatformCommission commission
    ) {
        return new CommissionInvoiceItemDTO(
                item.getId(),
                item.getInvoiceId(),
                item.getCommissionId(),
                commission != null ? commission.getSourceType() : null,
                commission != null ? commission.getSourceId() : null,
                commission != null ? resolveSourceCode(commission) : null,
                commission != null ? commission.getCreatedAt() : null,
                commission != null ? commission.getCommissionBase() : null,
                item.getCommissionAmount(),
                item.getCreatedAt()
        );
    }

    private InvoiceRef resolveInvoiceRef(Long commissionId) {
        if (commissionId == null) {
            return new InvoiceRef(null, null);
        }
        return invoiceItemRepository.findByCommissionIdIn(List.of(commissionId)).stream()
                .findFirst()
                .flatMap(item -> invoiceRepository.findById(item.getInvoiceId()))
                .map(invoice -> new InvoiceRef(invoice.getId(), invoice.getInvoiceCode()))
                .orElse(new InvoiceRef(null, null));
    }

    private String resolveSourceCode(PlatformCommission commission) {
        if (commission == null || commission.getSourceType() == null || commission.getSourceId() == null) {
            return null;
        }
        if (commission.getSourceType() == CommissionSourceType.ORDER) {
            return orderRepository.findById(commission.getSourceId())
                    .map(CustomerOrder::getOrderCode)
                    .filter(StringUtils::hasText)
                    .orElse("ORDER-" + commission.getSourceId());
        }
        return "BKG-" + String.format("%03d", commission.getSourceId());
    }

    private LocalDate resolveCurrentPeriodFrom(LocalDate date) {
        return date.getDayOfMonth() <= 15 ? date.withDayOfMonth(1) : date.withDayOfMonth(16);
    }

    private LocalDate resolveCurrentPeriodTo(LocalDate date) {
        return date.getDayOfMonth() <= 15
                ? date.withDayOfMonth(15)
                : date.with(TemporalAdjusters.lastDayOfMonth());
    }

    private LocalDate resolveNextInvoiceDate(LocalDate date) {
        return date.getDayOfMonth() <= 15 ? date.withDayOfMonth(16) : date.plusMonths(1).withDayOfMonth(1);
    }

    private record InvoiceRef(Long invoiceId, String invoiceCode) {
    }

    private OffsetDateTime resolveDueAt(LocalDate periodTo) {
        LocalDate dueDate = periodTo.getDayOfMonth() == 15
                ? periodTo.withDayOfMonth(20)
                : periodTo.plusDays(1).withDayOfMonth(5);
        return dueDate.atTime(23, 59, 59).atZone(BUSINESS_ZONE).toOffsetDateTime();
    }

    private String buildInvoiceCode(Long shopId, LocalDate periodFrom, LocalDate periodTo) {
        return "PC" + compactDate(periodTo) + String.format("%04d", shopId);
    }

    private String buildQrUrl(PlatformCommissionInvoice invoice) {
        return paymentProperties.getSepayQrBaseUrl()
                + "?bank=" + encode(paymentProperties.getSepayBankCode())
                + "&acc=" + encode(paymentProperties.getSepayAccountNumber())
                + "&amount=" + invoice.getTotalCommissionAmount()
                + "&des=" + encode(invoice.getTransferContent());
    }

    private String buildSummaryMessage(boolean hasUnpaidInvoice, long unpaidAmount, OffsetDateTime nextDueDate) {
        if (!hasUnpaidInvoice) {
            return "Shop chưa có hóa đơn phí nền tảng cần thanh toán.";
        }
        return "Bạn có hóa đơn phí nền tảng cần thanh toán trước ngày "
                + nextDueDate.toLocalDate()
                + ". Tổng số tiền: "
                + unpaidAmount;
    }

    private void validatePeriod(LocalDate periodFrom, LocalDate periodTo) {
        if (periodFrom == null || periodTo == null || periodTo.isBefore(periodFrom)) {
            throw new IllegalArgumentException("Kỳ hóa đơn không hợp lệ");
        }
    }

    private void assertActiveShopMember(Long shopId, Long currentUserId) {
        if (shopId == null) {
            throw new IllegalArgumentException("Cần chọn shop để xem công nợ");
        }
        boolean allowed = currentUserId != null && shopMemberRepository.existsByShopIdAndUserIdAndStatus(
                shopId,
                currentUserId,
                MemberStatus.ACTIVE
        );
        if (!allowed) {
            throw new IllegalArgumentException("Bạn không có quyền xem công nợ của shop này");
        }
    }

    private String compactDate(LocalDate date) {
        return date.toString().replace("-", "");
    }

    private String encode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }

    private String resolveWebhookSecret(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        String apiKey = firstHeader(headers, "X-API-KEY", "API-KEY", "apikey");
        if (StringUtils.hasText(apiKey)) {
            return apiKey;
        }
        String authorization = firstHeader(headers, "Authorization");
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length());
        }
        if (authorization.startsWith("Apikey ")) {
            return authorization.substring("Apikey ".length());
        }
        return authorization;
    }

    private String firstHeader(Map<String, String> headers, String... names) {
        for (String name : names) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                if (name.equalsIgnoreCase(header.getKey()) && StringUtils.hasText(header.getValue())) {
                    return header.getValue();
                }
            }
        }
        return null;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private long valueOrZero(Long value) {
        return value != null ? value : 0L;
    }
}
