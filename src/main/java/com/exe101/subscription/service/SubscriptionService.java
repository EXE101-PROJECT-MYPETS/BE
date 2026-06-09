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
            // Tam tat nghiep vu thu tien subscription: tra ve goi mien phi thay cho cac goi can thanh toan.
            // Khi can bat lai, doi subscription.payment-collection-enabled=true de chay lai code plan cu ben duoi.
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
            // Tam tat tao QR/ghi payment subscription cua shop.
            // Code SePay goc duoc giu nguyen ngay ben duoi de nang cap/bat lai sau.
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
            // Khong con thu tien subscription nen khong expose pending payment moi.
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
            // Endpoint status duoc giu de frontend cu khong vo, nhung payment collection da tat.
            return buildPaymentCollectionDisabledStatusResponse(paymentId);
        }

        SubscriptionPayment payment = paymentRepository.findByShopIdAndId(shopId, paymentId)
                .orElseThrow(() -> new SubscriptionNotFound(
                        "SubscriptionPaymentNotFound",
                        "Khong tim thay giao dich subscription"
                ));
        expirePendingPaymentIfNeeded(payment, OffsetDateTime.now());
        return toPaymentStatusResponse(payment);
    }

    @Transactional
    public SubscriptionCancelPaymentResponse cancelPendingPayment(Long requestedShopId, Long paymentId) {
        Long shopId = resolveCurrentShopId(requestedShopId);

        if (!isPaymentCollectionEnabled()) {
            // Khong tao pending payment khi collection tat, nen cancel chi tra ve trang thai disabled.
            return buildPaymentCollectionDisabledCancelResponse(paymentId);
        }

        SubscriptionPayment payment = paymentRepository.findByShopIdAndId(shopId, paymentId)
                .orElseThrow(() -> new SubscriptionNotFound(
                        "SubscriptionPaymentNotFound",
                        "Khong tim thay giao dich subscription"
                ));
        expirePendingPaymentIfNeeded(payment, OffsetDateTime.now());
        if (payment.getStatus() != SubscriptionPaymentStatus.PENDING) {
            throw new SubscriptionValidationException(
                    "SubscriptionPaymentStatusInvalid",
                    "Chi co the huy giao dich dang cho thanh toan"
            );
        }

        payment.setStatus(SubscriptionPaymentStatus.CANCELED);
        SubscriptionPayment saved = paymentRepository.save(payment);
        return new SubscriptionCancelPaymentResponse(
                saved.getId(),
                saved.getInvoiceNumber(),
                saved.getStatus().name(),
                "Da huy thanh toan."
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<SubscriptionPaymentHistoryItemDTO> getPaymentHistory(Long requestedShopId, int page, int size) {
        Long shopId = resolveCurrentShopId(requestedShopId);

        if (!isPaymentCollectionEnabled()) {
            // Lich su payment cu khong bi xoa trong DB/code, nhung nghiep vu thu tien dang tam an.
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
            // Webhook SePay duoc nhan va bo qua de khong ghi nhan/extend subscription trong giai doan mien phi.
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
                        // Shop moi duoc kich hoat goi mien phi khi da bo nghiep vu thu tien.
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
                        "Shop chua co subscription"
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
        // DB dang bat buoc current_period_end/expired_at NOT NULL; dung moc xa de bieu dien goi mien phi.
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
                "Shop dang duoc su dung mien phi, khong can thanh toan subscription."
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
                "Goi Monthly",
                properties.getSubscriptionMonthlyPrice(),
                properties.getSubscriptionMonthlyDays(),
                CURRENCY,
                List.of(
                        "Quan ly shop",
                        "Quan ly dich vu",
                        "Quan ly san pham",
                        "Quan ly lich dat",
                        "Tin nhan voi khach hang",
                        "AI chat ho tro khach hang"
                )
        );
    }

    private SubscriptionPlanResponse buildFreePlanResponse() {
        return new SubscriptionPlanResponse(
                FREE_PLAN_CODE,
                "Goi mien phi",
                0L,
                0,
                CURRENCY,
                List.of(
                        "Quan ly shop",
                        "Quan ly dich vu",
                        "Quan ly san pham",
                        "Quan ly lich dat",
                        "Tin nhan voi khach hang",
                        "AI chat ho tro khach hang"
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
                payment.getPlan() != null ? payment.getPlan().getName() : "Goi Monthly",
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
                "Nghiep vu thu tien subscription dang tam tat."
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
                        "Ban khong co quyen thao tac subscription cua shop nay"
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
                    "Tai khoan hien tai chua co shop dang hoat dong"
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
                        "Khong tim thay goi Monthly dang hoat dong"
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
            return "Goi su dung cua ban da het han.";
        }
        if ("TRIAL".equals(planType)) {
            return "Goi dung thu cua ban con " + remainingDays + " ngay.";
        }
        return "Goi Monthly cua ban con " + remainingDays + " ngay.";
    }

    private String generateInvoiceNumber(Long shopId) {
        return "SUB_" + shopId + "_" + System.currentTimeMillis();
    }

    private int validateAndResolveMonths(Integer months) {
        if (months == null || (months != 1 && months != 3 && months != 6)) {
            throw new SubscriptionValidationException(
                    "SubscriptionMonthsInvalid",
                    "So thang thanh toan chi ho tro 1, 3 hoac 6"
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
                    "Ban can dang nhap de thao tac subscription"
            );
        }
        return userPrincipal.getUser().getId();
    }
}
