package com.exe101.subscription.service;

import com.exe101.auth.dto.AuthenticatedShopDTO;
import com.exe101.auth.model.UserPrincipal;
import com.exe101.common.PageResponse;
import com.exe101.shop.entity.ShopStatus;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.repository.IShopMemberRepository;
import com.exe101.subscription.config.SubscriptionPaymentProperties;
import com.exe101.subscription.dto.*;
import com.exe101.subscription.entity.*;
import com.exe101.subscription.exception.SubscriptionAccessDenied;
import com.exe101.subscription.exception.SubscriptionNotFound;
import com.exe101.subscription.exception.SubscriptionValidationException;
import com.exe101.subscription.repository.IShopSubscriptionRepository;
import com.exe101.subscription.repository.ISubscriptionPaymentRepository;
import com.exe101.subscription.repository.ISubscriptionPlanRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private static final String CURRENCY = "VND";
    private static final String FREE_PLAN_CODE = "FREE";
    private static final String MONTHLY_PLAN_CODE = "MONTHLY";
    private static final String PROVIDER_SEPAY = "SEPAY";
    private static final String PAYMENT_COLLECTION_DISABLED_STATUS = "DISABLED";
    private static final int FREE_ACCESS_YEARS = 100;

    private final ISubscriptionPlanRepository planRepository;
    private final IShopSubscriptionRepository subscriptionRepository;
    private final ISubscriptionPaymentRepository paymentRepository;
    private final IShopMemberRepository shopMemberRepository;
    private final SubscriptionPaymentProperties properties;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getActivePlans() {
        if (!isPaymentCollectionEnabled()) {
            // Tạm tắt nghiệp vụ thu tiền subscription: trả về gói miễn phí thay cho các gói cần thanh toán.
            // Khi cần bật lại, đổi subscription.payment-collection-enabled=true để chạy lại code plan cũ bên dưới.
            return List.of(buildFreePlanResponse());
        }

        return planRepository.findByActiveTrueOrderByDurationMonthsAscIdAsc().stream()
                .map(this::toPlanResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionOverviewResponse getOverview(Long requestedShopId) {
        Long shopId = resolveCurrentShopId(requestedShopId);
        ShopSubscription subscription = resolveOrCreateSubscription(shopId);
        return toOverview(subscription);
    }

    @Transactional
    public SepayQrPaymentResponse createSepayQrPayment(Long requestedShopId, SepayQrPaymentRequest request) {
        Long shopId = resolveCurrentShopId(requestedShopId);
        int months = validateAndResolveMonths(request.getMonths());

        if (!isPaymentCollectionEnabled()) {
            // Tạm tắt tạo QR/ghi payment subscription của shop.
            // Code SePay gốc được giữ nguyên ngay bên dưới để nâng cấp/bật lại sau.
            resolveOrCreateSubscription(shopId);
            return buildPaymentCollectionDisabledResponse(months);
        }

        int durationDays = properties.getSubscriptionMonthlyDays() * months;
        long amount = properties.getSubscriptionMonthlyPrice() * months;
        OffsetDateTime now = OffsetDateTime.now();
        paymentRepository.expirePendingPaymentsByShopId(shopId, now);

        SubscriptionPayment existingPayment = paymentRepository
                .findFirstByShopIdAndStatusAndExpiredAtAfterOrderByCreatedAtDesc(
                        shopId,
                        SubscriptionPaymentStatus.PENDING,
                        now
                )
                .filter(payment -> Objects.equals(payment.getDurationMonths(), months))
                .orElse(null);
        if (existingPayment != null) {
            return toSepayQrPaymentResponse(existingPayment);
        }
        paymentRepository.cancelActivePendingPaymentsByShopId(shopId, now);

        ShopSubscription subscription = resolveOrCreateSubscription(shopId);
        SubscriptionPlan monthlyPlan = resolveMonthlyPlan();
        OffsetDateTime expiredAt = now.plusMinutes(properties.getPaymentExpireMinutes());

        SubscriptionPayment payment = new SubscriptionPayment();
        payment.setShopId(shopId);
        payment.setSubscriptionId(subscription.getId());
        payment.setPlanId(monthlyPlan.getId());
        payment.setPlanCode(MONTHLY_PLAN_CODE);
        payment.setInvoiceNumber(generateInvoiceNumber(shopId));
        payment.setTransferContent(payment.getInvoiceNumber());
        payment.setAmount(amount);
        payment.setDurationMonths(months);
        payment.setDurationDays(durationDays);
        payment.setStatus(SubscriptionPaymentStatus.PENDING);
        payment.setProvider(PROVIDER_SEPAY);
        payment.setPaymentMethod(SubscriptionPaymentMethod.SEPAY);
        payment.setPeriodStart(resolveSubscriptionBaseTime(subscription, now));
        payment.setPeriodEnd(payment.getPeriodStart().plusDays(durationDays));
        payment.setExpiredAt(expiredAt);

        return toSepayQrPaymentResponse(paymentRepository.save(payment));
    }

    @Transactional
    public SepayQrPaymentResponse getCurrentPendingPayment(Long requestedShopId) {
        Long shopId = resolveCurrentShopId(requestedShopId);

        if (!isPaymentCollectionEnabled()) {
            // Không còn thu tiền subscription nên không expose pending payment mới.
            return null;
        }

        paymentRepository.expirePendingPaymentsByShopId(shopId, OffsetDateTime.now());
        return paymentRepository
                .findFirstByShopIdAndStatusAndExpiredAtAfterOrderByCreatedAtDesc(
                        shopId,
                        SubscriptionPaymentStatus.PENDING,
                        OffsetDateTime.now()
                )
                .map(this::toSepayQrPaymentResponse)
                .orElse(null);
    }

    @Transactional
    public SubscriptionPaymentStatusResponse getPaymentStatus(Long requestedShopId, Long paymentId) {
        Long shopId = resolveCurrentShopId(requestedShopId);

        if (!isPaymentCollectionEnabled()) {
            // Endpoint status được giữ để frontend cũ không vỡ, nhưng payment collection đã tắt.
            return buildPaymentCollectionDisabledStatusResponse(paymentId);
        }

        SubscriptionPayment payment = paymentRepository.findByShopIdAndId(shopId, paymentId)
                .orElseThrow(() -> new SubscriptionNotFound(
                        "SubscriptionPaymentNotFound",
                        "Không tìm thấy giao dịch subscription"
                ));
        expirePendingPaymentIfNeeded(payment, OffsetDateTime.now());
        return toPaymentStatusResponse(payment);
    }

    @Transactional
    public SubscriptionCancelPaymentResponse cancelPendingPayment(Long requestedShopId, Long paymentId) {
        Long shopId = resolveCurrentShopId(requestedShopId);

        if (!isPaymentCollectionEnabled()) {
            // Không tạo pending payment khi collection tắt, nên cancel chỉ trả về trạng thái disabled.
            return buildPaymentCollectionDisabledCancelResponse(paymentId);
        }

        SubscriptionPayment payment = paymentRepository.findByShopIdAndId(shopId, paymentId)
                .orElseThrow(() -> new SubscriptionNotFound(
                        "SubscriptionPaymentNotFound",
                        "Không tìm thấy giao dịch subscription"
                ));
        expirePendingPaymentIfNeeded(payment, OffsetDateTime.now());
        if (payment.getStatus() != SubscriptionPaymentStatus.PENDING) {
            throw new SubscriptionValidationException(
                    "SubscriptionPaymentStatusInvalid",
                    "Chỉ có thể hủy giao dịch đang chờ thanh toán"
            );
        }

        payment.setStatus(SubscriptionPaymentStatus.CANCELED);
        SubscriptionPayment saved = paymentRepository.save(payment);
        return new SubscriptionCancelPaymentResponse(
                saved.getId(),
                saved.getInvoiceNumber(),
                saved.getStatus().name(),
                "Đã hủy thanh toán."
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<SubscriptionPaymentHistoryItemDTO> getPaymentHistory(Long requestedShopId, int page, int size) {
        Long shopId = resolveCurrentShopId(requestedShopId);

        if (!isPaymentCollectionEnabled()) {
            // Lịch sử payment cũ không bị xóa trong DB/code, nhưng nghiệp vụ thu tiền đang tạm ẩn.
            return new PageResponse<>(List.of(), page, size, 0, 0, false, false);
        }

        Page<SubscriptionPaymentHistoryItemDTO> payments = paymentRepository
                .findByShopIdOrderByCreatedAtDesc(shopId, PageRequest.of(page, size))
                .map(this::toHistoryItem);
        return PageResponse.from(payments);
    }

    public boolean isSepayWebhookAuthorized(Map<String, String> headers) {
        if (!properties.isSepayWebhookAuthEnabled()) {
            return true;
        }
        String configuredKey = properties.getSepayIpnSecretKey();
        if (!StringUtils.hasText(configuredKey)) {
            log.warn("SePay webhook auth is enabled but sepay.ipn-secret-key is blank");
            return false;
        }
        String actualKey = resolveWebhookSecret(headers);
        return StringUtils.hasText(actualKey) && configuredKey.trim().equals(actualKey.trim());
    }

    @Transactional
    public void handleSepayIpn(SepayWebhookRequest request) {
        if (!isPaymentCollectionEnabled()) {
            // Webhook SePay được nhận và bỏ qua để không ghi nhận/extend subscription trong giai đoạn miễn phí.
            log.info(
                    "SePay subscription webhook ignored because subscription payment collection is disabled. webhookId={}",
                    request != null ? request.getId() : null
            );
            return;
        }

        if (request == null) {
            log.warn("Received empty SePay webhook");
            return;
        }

        log.info(
                "Received SePay webhook id={}, referenceCode={}, transferType={}, amount={}, contentLength={}",
                request.getId(),
                maskSensitiveValue(request.getReferenceCode()),
                request.getTransferType(),
                request.getTransferAmount(),
                safeLength(request.getContent())
        );

        if (!"in".equalsIgnoreCase(trimToEmpty(request.getTransferType()))) {
            log.info("Skipped SePay webhook id={} because transferType={}", request.getId(), request.getTransferType());
            return;
        }

        BigDecimal transferAmount = request.getTransferAmount();
        if (transferAmount == null || transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Skipped SePay webhook id={} because transferAmount is invalid", request.getId());
            return;
        }

        if (StringUtils.hasText(properties.getSepayAccountNumber())
                && !properties.getSepayAccountNumber().trim().equals(trimToEmpty(request.getAccountNumber()))) {
            log.warn(
                    "Skipped SePay webhook id={} because accountNumber mismatch. received={}, expected={}",
                    request.getId(),
                    maskAccountNumber(request.getAccountNumber()),
                    maskAccountNumber(properties.getSepayAccountNumber())
            );
            return;
        }

        String searchableContent = (trimToEmpty(request.getCode()) + " " + trimToEmpty(request.getContent())).trim();
        if (!StringUtils.hasText(searchableContent)) {
            log.info("Skipped SePay webhook id={} because content is blank", request.getId());
            return;
        }

        Optional<SubscriptionPayment> matchedPayment = paymentRepository.findLatestBySearchableContent(searchableContent);
        if (matchedPayment.isEmpty()) {
            log.warn(
                    "Unmatched SePay webhook id={}, referenceCode={}, amount={}, contentLength={}",
                    request.getId(),
                    maskSensitiveValue(request.getReferenceCode()),
                    request.getTransferAmount(),
                    searchableContent.length()
            );
            return;
        }

        SubscriptionPayment payment = matchedPayment.get();
        String transactionId = resolveTransactionId(request);
        log.info(
                "Matched SePay webhook id={} with paymentId={}, invoiceNumber={}, status={}",
                request.getId(),
                payment.getId(),
                payment.getInvoiceNumber(),
                payment.getStatus()
        );

        if (payment.getStatus() == SubscriptionPaymentStatus.SUCCESS
                || payment.getStatus() == SubscriptionPaymentStatus.SUCCESS_LATE) {
            log.info("Duplicate SePay webhook ignored for successful paymentId={}", payment.getId());
            return;
        }

        if (StringUtils.hasText(transactionId)
                && paymentRepository.findFirstByProviderAndProviderTransactionId(PROVIDER_SEPAY, transactionId)
                .filter(existing -> !Objects.equals(existing.getId(), payment.getId()))
                .isPresent()) {
            log.info("Duplicate SePay reference ignored. referenceCode={}, paymentId={}",
                    maskSensitiveValue(transactionId),
                    payment.getId());
            return;
        }

        if (transferAmount.compareTo(BigDecimal.valueOf(payment.getAmount())) < 0) {
            log.warn(
                    "Insufficient SePay amount for paymentId={}. received={}, expected={}",
                    payment.getId(),
                    transferAmount,
                    payment.getAmount()
            );
            recordIgnoredIpn(payment, transactionId, request);
            return;
        }

        handleMatchedIpnPayment(payment, transactionId, request);
    }

    @Transactional
    public void createTrialIfAbsent(Long shopId) {
        subscriptionRepository.findByShopId(shopId).ifPresentOrElse(
                ignored -> {
                },
                () -> {
                    if (isPaymentCollectionEnabled()) {
                        createTrial(shopId);
                    } else {
                        // Shop mới được kích hoạt gói miễn phí khi đã bỏ nghiệp vụ thu tiền.
                        createFreeAccessSubscription(shopId);
                    }
                }
        );
    }

    private void handleMatchedIpnPayment(SubscriptionPayment payment, String transactionId, SepayWebhookRequest request) {
        SubscriptionPaymentStatus status = payment.getStatus();
        if (status == SubscriptionPaymentStatus.SUCCESS || status == SubscriptionPaymentStatus.SUCCESS_LATE) {
            return;
        }
        if (status == SubscriptionPaymentStatus.CANCELED) {
            log.warn("SePay payment received after cancel. paymentId={}, referenceCode={}",
                    payment.getId(),
                    maskSensitiveValue(transactionId));
            markPaidAfterCancel(payment, transactionId, request);
            return;
        }
        if (status == SubscriptionPaymentStatus.EXPIRED && !isExpiredPaymentWithinLateGrace(payment)) {
            log.warn("SePay payment received after late grace. paymentId={}, referenceCode={}",
                    payment.getId(),
                    maskSensitiveValue(transactionId));
            recordIgnoredIpn(payment, transactionId, request);
            return;
        }
        if (status != SubscriptionPaymentStatus.PENDING && status != SubscriptionPaymentStatus.EXPIRED) {
            log.warn("SePay payment ignored because status is {}. paymentId={}", status, payment.getId());
            recordIgnoredIpn(payment, transactionId, request);
            return;
        }

        markPaymentSuccess(payment, transactionId, request);
    }

    private void markPaymentSuccess(SubscriptionPayment payment, String transactionId, SepayWebhookRequest request) {

        OffsetDateTime now = OffsetDateTime.now();
        ShopSubscription subscription = subscriptionRepository.findById(payment.getSubscriptionId())
                .orElseThrow(() -> new SubscriptionNotFound(
                        "ShopSubscriptionNotFound",
                        "Shop chưa có subscription"
                ));

        payment.setStatus(payment.getStatus() == SubscriptionPaymentStatus.EXPIRED
                ? SubscriptionPaymentStatus.SUCCESS_LATE
                : SubscriptionPaymentStatus.SUCCESS);
        payment.setPaidAt(now);
        if (StringUtils.hasText(transactionId)) {
            payment.setProviderTransactionId(transactionId.trim());
        }
        payment.setRawPayload(toRawPayload(request));
        paymentRepository.save(payment);

        OffsetDateTime baseTime = resolveSubscriptionBaseTime(subscription, now);
        subscription.setPlanId(payment.getPlanId());
        subscription.setPlanType(MONTHLY_PLAN_CODE);
        subscription.setStatus(ShopSubscriptionStatus.ACTIVE);
        if (subscription.getStartedAt() == null) {
            subscription.setStartedAt(now);
        }
        subscription.setCurrentPeriodStart(baseTime);
        subscription.setCurrentPeriodEnd(baseTime.plusDays(payment.getDurationDays()));
        subscription.setExpiredAt(baseTime.plusDays(payment.getDurationDays()));
        subscription.setCancelledAt(null);
        subscriptionRepository.save(subscription);

        log.info(
                "SePay payment success. paymentId={}, subscriptionId={}, extendedTo={}",
                payment.getId(),
                subscription.getId(),
                subscription.getExpiredAt()
        );
    }

    private void recordIgnoredIpn(SubscriptionPayment payment, String transactionId, SepayWebhookRequest request) {
        if (StringUtils.hasText(transactionId) && !StringUtils.hasText(payment.getProviderTransactionId())) {
            payment.setProviderTransactionId(transactionId.trim());
        }
        payment.setRawPayload(toRawPayload(request));
        paymentRepository.save(payment);
    }

    private void markPaidAfterCancel(SubscriptionPayment payment, String transactionId, SepayWebhookRequest request) {
        payment.setStatus(SubscriptionPaymentStatus.PAID_AFTER_CANCEL);
        payment.setPaidAt(OffsetDateTime.now());
        if (StringUtils.hasText(transactionId)) {
            payment.setProviderTransactionId(transactionId.trim());
        }
        payment.setRawPayload(toRawPayload(request));
        paymentRepository.save(payment);
    }

    private boolean isExpiredPaymentWithinLateGrace(SubscriptionPayment payment) {
        Integer graceMinutes = properties.getPaymentLateGraceMinutes();
        return payment.getExpiredAt() != null
                && graceMinutes != null
                && OffsetDateTime.now().isBefore(payment.getExpiredAt().plusMinutes(graceMinutes).plusNanos(1));
    }

    private ShopSubscription createTrial(Long shopId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiredAt = now.plusDays(properties.getSubscriptionTrialDays());
        ShopSubscription subscription = new ShopSubscription();
        subscription.setShopId(shopId);
        subscription.setPlanId(null);
        subscription.setPlanType("TRIAL");
        subscription.setStatus(ShopSubscriptionStatus.ACTIVE);
        subscription.setStartedAt(now);
        subscription.setTrialEndsAt(expiredAt);
        subscription.setCurrentPeriodStart(now);
        subscription.setCurrentPeriodEnd(expiredAt);
        subscription.setExpiredAt(expiredAt);
        return subscriptionRepository.save(subscription);
    }

    private ShopSubscription createFreeAccessSubscription(Long shopId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime technicalExpiredAt = now.plusYears(FREE_ACCESS_YEARS);
        ShopSubscription subscription = new ShopSubscription();
        subscription.setShopId(shopId);
        subscription.setPlanId(null);
        subscription.setPlanType(FREE_PLAN_CODE);
        subscription.setStatus(ShopSubscriptionStatus.ACTIVE);
        subscription.setStartedAt(now);
        subscription.setTrialEndsAt(null);
        subscription.setCurrentPeriodStart(now);
        // DB đang bắt buộc current_period_end/expired_at NOT NULL; dùng mốc xa để biểu diễn gói miễn phí.
        subscription.setCurrentPeriodEnd(technicalExpiredAt);
        subscription.setExpiredAt(technicalExpiredAt);
        return subscriptionRepository.save(subscription);
    }

    private ShopSubscription resolveOrCreateSubscription(Long shopId) {
        return subscriptionRepository.findByShopId(shopId)
                .orElseGet(() -> isPaymentCollectionEnabled()
                        ? createTrial(shopId)
                        : createFreeAccessSubscription(shopId));
    }

    private void expirePendingPaymentIfNeeded(SubscriptionPayment payment, OffsetDateTime now) {
        if (payment.getStatus() == SubscriptionPaymentStatus.PENDING
                && payment.getExpiredAt() != null
                && payment.getExpiredAt().isBefore(now)) {
            payment.setStatus(SubscriptionPaymentStatus.EXPIRED);
            paymentRepository.save(payment);
        }
    }

    private SubscriptionOverviewResponse toOverview(ShopSubscription subscription) {
        if (!isPaymentCollectionEnabled()) {
            return toFreeOverview(subscription);
        }

        OffsetDateTime now = OffsetDateTime.now();
        boolean expired = subscription.getExpiredAt() == null || !subscription.getExpiredAt().isAfter(now);
        String status = expired ? "EXPIRED" : toApiSubscriptionStatus(subscription.getStatus());
        long remainingDays = expired ? 0 : Duration.between(now, subscription.getExpiredAt()).toDays();
        String planType = StringUtils.hasText(subscription.getPlanType()) ? subscription.getPlanType() : "TRIAL";
        OffsetDateTime currentPlanStart = resolveCurrentPlanStart(subscription, planType);
        OffsetDateTime currentPlanEnd = resolveCurrentPlanEnd(subscription, planType);
        long planTotalDays = daysBetween(currentPlanStart, currentPlanEnd);
        long usedDays = resolveUsedDays(now, currentPlanStart, currentPlanEnd, planTotalDays);
        long currentPeriodRemainingDays = resolveCurrentPeriodRemainingDays(now, currentPlanStart, currentPlanEnd);

        return new SubscriptionOverviewResponse(
                subscription.getShopId(),
                planType,
                status,
                currentPlanStart,
                subscription.getExpiredAt(),
                remainingDays,
                properties.getSubscriptionTrialDays(),
                usedDays,
                planTotalDays,
                subscription.getStartedAt(),
                subscription.getTrialEndsAt(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                currentPeriodRemainingDays,
                properties.getSubscriptionMonthlyPrice(),
                CURRENCY,
                true,
                buildOverviewMessage(planType, status, remainingDays)
        );
    }

    private SubscriptionOverviewResponse toFreeOverview(ShopSubscription subscription) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startedAt = subscription.getStartedAt() != null
                ? subscription.getStartedAt()
                : now;
        OffsetDateTime expiredAt = subscription.getExpiredAt() != null && subscription.getExpiredAt().isAfter(now)
                ? subscription.getExpiredAt()
                : now.plusYears(FREE_ACCESS_YEARS);
        long freeDays = Math.max(1, Duration.between(now, expiredAt).toDays());
        return new SubscriptionOverviewResponse(
                subscription.getShopId(),
                FREE_PLAN_CODE,
                "ACTIVE",
                startedAt,
                expiredAt,
                freeDays,
                properties.getSubscriptionTrialDays(),
                0,
                freeDays,
                startedAt,
                null,
                startedAt,
                expiredAt,
                freeDays,
                0L,
                CURRENCY,
                false,
                "Shop đang được sử dụng miễn phí, không cần thanh toán subscription."
        );
    }

    private OffsetDateTime resolveCurrentPlanStart(ShopSubscription subscription, String planType) {
        if ("TRIAL".equals(planType)) {
            return subscription.getStartedAt();
        }
        return subscription.getCurrentPeriodStart() != null
                ? subscription.getCurrentPeriodStart()
                : subscription.getStartedAt();
    }

    private OffsetDateTime resolveCurrentPlanEnd(ShopSubscription subscription, String planType) {
        if ("TRIAL".equals(planType) && subscription.getTrialEndsAt() != null) {
            return subscription.getTrialEndsAt();
        }
        return subscription.getCurrentPeriodEnd() != null
                ? subscription.getCurrentPeriodEnd()
                : subscription.getExpiredAt();
    }

    private long resolveUsedDays(
            OffsetDateTime now,
            OffsetDateTime currentPlanStart,
            OffsetDateTime currentPlanEnd,
            long planTotalDays
    ) {
        if (currentPlanStart == null || currentPlanEnd == null || now.isBefore(currentPlanStart)) {
            return 0;
        }
        OffsetDateTime usageEnd = now.isAfter(currentPlanEnd) ? currentPlanEnd : now;
        return Math.min(planTotalDays, Math.max(0, Duration.between(currentPlanStart, usageEnd).toDays()));
    }

    private long resolveCurrentPeriodRemainingDays(
            OffsetDateTime now,
            OffsetDateTime currentPlanStart,
            OffsetDateTime currentPlanEnd
    ) {
        if (currentPlanStart == null || currentPlanEnd == null || !currentPlanEnd.isAfter(now)) {
            return 0;
        }
        if (now.isBefore(currentPlanStart)) {
            return daysBetween(currentPlanStart, currentPlanEnd);
        }
        return Duration.between(now, currentPlanEnd).toDays();
    }

    private long daysBetween(OffsetDateTime start, OffsetDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            return 0;
        }
        return Math.max(1, Duration.between(start, end).toDays());
    }

    private SubscriptionPlanResponse toPlanResponse(SubscriptionPlan plan) {
        return new SubscriptionPlanResponse(
                toApiPlanCode(plan),
                "Gói Monthly",
                properties.getSubscriptionMonthlyPrice(),
                properties.getSubscriptionMonthlyDays(),
                CURRENCY,
                List.of(
                        "Quản lý shop",
                        "Quản lý dịch vụ",
                        "Quản lý sản phẩm",
                        "Quản lý lịch đặt",
                        "Tin nhắn với khách hàng",
                        "AI chat hỗ trợ khách hàng"
                )
        );
    }

    private SubscriptionPlanResponse buildFreePlanResponse() {
        return new SubscriptionPlanResponse(
                FREE_PLAN_CODE,
                "Gói miễn phí",
                0L,
                0,
                CURRENCY,
                List.of(
                        "Quản lý shop",
                        "Quản lý dịch vụ",
                        "Quản lý sản phẩm",
                        "Quản lý lịch đặt",
                        "Tin nhắn với khách hàng",
                        "AI chat hỗ trợ khách hàng"
                )
        );
    }

    private SepayQrPaymentResponse toSepayQrPaymentResponse(SubscriptionPayment payment) {
        return new SepayQrPaymentResponse(
                payment.getId(),
                payment.getInvoiceNumber(),
                payment.getDurationMonths(),
                payment.getDurationDays(),
                payment.getAmount(),
                payment.getStatus().name(),
                PROVIDER_SEPAY,
                properties.getSepayBankCode(),
                properties.getSepayBankName(),
                properties.getSepayAccountNumber(),
                properties.getSepayAccountName(),
                payment.getTransferContent(),
                buildQrUrl(payment),
                payment.getExpiredAt(),
                payment.getPeriodEnd(),
                payment.getCreatedAt()
        );
    }

    private SubscriptionPaymentStatusResponse toPaymentStatusResponse(SubscriptionPayment payment) {
        return new SubscriptionPaymentStatusResponse(
                payment.getId(),
                payment.getInvoiceNumber(),
                payment.getStatus().name(),
                payment.getPaidAt(),
                payment.getExpiredAt(),
                payment.getPeriodEnd()
        );
    }

    private SubscriptionPaymentHistoryItemDTO toHistoryItem(SubscriptionPayment payment) {
        return new SubscriptionPaymentHistoryItemDTO(
                payment.getId(),
                payment.getInvoiceNumber(),
                payment.getPlan() != null ? payment.getPlan().getName() : "Gói Monthly",
                payment.getDurationMonths(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getProvider(),
                payment.getPaidAt(),
                payment.getPeriodEnd(),
                payment.getCreatedAt()
        );
    }

    private SepayQrPaymentResponse buildPaymentCollectionDisabledResponse(Integer months) {
        return new SepayQrPaymentResponse(
                null,
                null,
                months,
                0,
                0L,
                PAYMENT_COLLECTION_DISABLED_STATUS,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private SubscriptionPaymentStatusResponse buildPaymentCollectionDisabledStatusResponse(Long paymentId) {
        return new SubscriptionPaymentStatusResponse(
                paymentId,
                null,
                PAYMENT_COLLECTION_DISABLED_STATUS,
                null,
                null,
                null
        );
    }

    private SubscriptionCancelPaymentResponse buildPaymentCollectionDisabledCancelResponse(Long paymentId) {
        return new SubscriptionCancelPaymentResponse(
                paymentId,
                null,
                PAYMENT_COLLECTION_DISABLED_STATUS,
                "Nghiệp vụ thu tiền subscription đang tạm tắt."
        );
    }

    private Long resolveCurrentShopId(Long requestedShopId) {
        Long userId = getCurrentUserId();
        if (requestedShopId != null) {
            boolean allowed = shopMemberRepository.existsByShopIdAndUserIdAndStatus(
                    requestedShopId,
                    userId,
                    MemberStatus.ACTIVE
            );
            if (!allowed) {
                throw new SubscriptionAccessDenied(
                        "SubscriptionAccessDenied",
                        "Bạn không có quyền thao tác subscription của shop này"
                );
            }
            return requestedShopId;
        }

        List<AuthenticatedShopDTO> shops = shopMemberRepository.findAuthenticatedShopsByUserIdAndStatus(
                userId,
                MemberStatus.ACTIVE,
                ShopStatus.ACTIVE
        );
        if (shops.isEmpty()) {
            throw new SubscriptionAccessDenied(
                    "SubscriptionShopNotFound",
                    "Tài khoản hiện tại chưa có shop đang hoạt động"
            );
        }
        return shops.get(0).getId();
    }

    private boolean isPaymentCollectionEnabled() {
        return properties.isSubscriptionPaymentCollectionEnabled();
    }

    private SubscriptionPlan resolveMonthlyPlan() {
        return planRepository.findByActiveTrueOrderByDurationMonthsAscIdAsc().stream()
                .filter(plan -> Objects.equals(plan.getDurationMonths(), 1))
                .findFirst()
                .orElseThrow(() -> new SubscriptionNotFound(
                        "SubscriptionPlanNotFound",
                        "Không tìm thấy gói Monthly đang hoạt động"
                ));
    }

    private OffsetDateTime resolveSubscriptionBaseTime(ShopSubscription subscription, OffsetDateTime now) {
        return subscription.getExpiredAt() != null && subscription.getExpiredAt().isAfter(now)
                ? subscription.getExpiredAt()
                : now;
    }

    private String toApiSubscriptionStatus(ShopSubscriptionStatus status) {
        return switch (status) {
            case TRIALING, ACTIVE -> "ACTIVE";
            case EXPIRED -> "EXPIRED";
            case CANCELED -> "CANCELED";
        };
    }

    private String toApiPlanCode(SubscriptionPlan plan) {
        return Objects.equals(plan.getDurationMonths(), 1) ? MONTHLY_PLAN_CODE : plan.getCode();
    }

    private String buildOverviewMessage(String planType, String status, long remainingDays) {
        if ("EXPIRED".equals(status)) {
            return "Gói sử dụng của bạn đã hết hạn.";
        }
        if ("TRIAL".equals(planType)) {
            return "Gói dùng thử của bạn còn " + remainingDays + " ngày.";
        }
        return "Gói Monthly của bạn còn " + remainingDays + " ngày.";
    }

    private String generateInvoiceNumber(Long shopId) {
        return "SUB_" + shopId + "_" + System.currentTimeMillis();
    }

    private int validateAndResolveMonths(Integer months) {
        if (months == null || (months != 1 && months != 3 && months != 6)) {
            throw new SubscriptionValidationException(
                    "SubscriptionMonthsInvalid",
                    "Số tháng thanh toán chỉ hỗ trợ 1, 3 hoặc 6"
            );
        }
        return months;
    }

    private String buildQrUrl(SubscriptionPayment payment) {
        return properties.getSepayQrBaseUrl()
                + "?bank=" + encode(properties.getSepayBankCode())
                + "&acc=" + encode(properties.getSepayAccountNumber())
                + "&amount=" + payment.getAmount()
                + "&des=" + encode(payment.getTransferContent());
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

    private String resolveTransactionId(SepayWebhookRequest request) {
        if (StringUtils.hasText(request.getReferenceCode())) {
            return request.getReferenceCode().trim();
        }
        return request.getId() != null ? String.valueOf(request.getId()) : null;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private int safeLength(String value) {
        return value != null ? value.length() : 0;
    }

    private String maskAccountNumber(String value) {
        String normalized = trimToEmpty(value);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        int visible = Math.min(4, normalized.length());
        return "*".repeat(Math.max(0, normalized.length() - visible)) + normalized.substring(normalized.length() - visible);
    }

    private String maskSensitiveValue(String value) {
        String normalized = trimToEmpty(value);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        if (normalized.length() <= 8) {
            return "***";
        }
        return normalized.substring(0, 4) + "***" + normalized.substring(normalized.length() - 4);
    }

    private String toRawPayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return payload != null ? payload.toString() : null;
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new SubscriptionAccessDenied(
                    "SubscriptionAccessDenied",
                    "Bạn cần đăng nhập để thao tác subscription"
            );
        }
        return userPrincipal.getUser().getId();
    }
}
