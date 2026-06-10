package com.exe101.email.service;

import com.exe101.email.dto.EmailSendRequest;
import com.exe101.email.entity.EmailVerificationPurpose;
import com.exe101.email.entity.EmailVerificationToken;
import com.exe101.email.exception.EmailSendException;
import com.exe101.email.exception.EmailValidationException;
import com.exe101.email.repository.IEmailVerificationTokenRepository;
import com.exe101.order.entity.CustomerOrder;
import com.exe101.user.entity.User;
import com.exe101.user.exception.UserDuplicate;
import com.exe101.user.repository.IUserRepository;
import jakarta.mail                                                             .MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MIN_CODE = 100000;
    private static final int CODE_RANGE = 900000;
    private static final String TEMPLATE_DIR = "email-templates/";

    private final JavaMailSender mailSender;
    private final IEmailVerificationTokenRepository tokenRepository;
    private final IUserRepository userRepository;
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    @Value("${app.email.from:}")
    private String fromAddress;

    @Value("${app.email.brand-name:Pawly}")
    private String brandName;

    @Value("${app.email.support-email:${MAIL_FROM:${MAIL_USERNAME:}}}")
    private String supportEmail;

    @Value("${app.email.verification-code-exp-minutes:10}")
    private long verificationCodeExpMinutes;

    public void send(EmailSendRequest request) {
        if (Boolean.TRUE.equals(request.getHtml())) {
            sendHtml(request.getTo(), request.getSubject(), renderStandardEmail(request.getSubject(), request.getBody()));
            return;
        }
        sendText(request.getTo(), request.getSubject(), request.getBody());
    }

    public void sendText(String to, String subject, String body) {
        sendMime(to, subject, body, false);
    }

    public void sendHtml(String to, String subject, String htmlBody) {
        sendMime(to, subject, htmlBody, true);
    }

    public void sendShopRegistrationMessage(
            String to,
            String title,
            String ownerName,
            String shopName,
            String content
    ) {
        String subject = title.trim();
        sendHtml(to, subject, renderShopRegistrationEmail(
                subject,
                ownerName,
                shopName,
                renderPlainTextContent(content)
        ));
    }

    public void sendShopRegistrationApproved(String to, String ownerName, String shopName) {
        String subject = "Đăng ký shop đã được chấp nhận";
        String content = renderTemplate("shop-registration-approved-content.html", Map.of());
        sendHtml(to, subject, renderShopRegistrationEmail(subject, ownerName, shopName, content));
    }

    public void sendShopRegistrationRejected(String to, String ownerName, String shopName) {
        String subject = "Đăng ký shop chưa được chấp nhận";
        String content = renderTemplate("shop-registration-rejected-content.html", Map.of());
        sendHtml(to, subject, renderShopRegistrationEmail(subject, ownerName, shopName, content));
    }

    public void sendPlatformCommissionInvoiceCreated(
            String to,
            String ownerName,
            String shopName,
            String invoiceCode,
            LocalDate periodFrom,
            LocalDate periodTo,
            Long totalCommissionAmount,
            OffsetDateTime dueAt,
            String bankCode,
            String accountNumber,
            String accountName,
            String transferContent
    ) {
        String subject = "Hóa đơn phí nền tảng " + safeText(invoiceCode);
        String greetingName = StringUtils.hasText(ownerName) ? ownerName.trim() : "chu shop";
        String displayShopName = StringUtils.hasText(shopName) ? shopName.trim() : "shop cua ban";
        Map<String, String> values = new LinkedHashMap<>();
        values.put("ownerName", escapeHtml(greetingName));
        values.put("shopName", escapeHtml(displayShopName));
        values.put("invoiceCode", escapeHtml(safeText(invoiceCode)));
        values.put("periodFrom", escapeHtml(periodFrom != null ? periodFrom.toString() : ""));
        values.put("periodTo", escapeHtml(periodTo != null ? periodTo.toString() : ""));
        values.put("totalCommissionAmount", escapeHtml(formatVnd(totalCommissionAmount)));
        values.put("dueAt", escapeHtml(dueAt != null ? dueAt.toLocalDate().toString() : ""));
        values.put("bankCode", escapeHtml(safeText(bankCode)));
        values.put("accountNumber", escapeHtml(safeText(accountNumber)));
        values.put("accountName", escapeHtml(safeText(accountName)));
        values.put("transferContent", escapeHtml(safeText(transferContent)));
        values.put("brandName", escapeHtml(brandName));
        String body = renderTemplate("platform-commission-invoice-created.html", values);
        sendHtml(to, subject, renderStandardEmail(subject, body));
    }

    public void sendOrderShipping(User user, CustomerOrder order) {
        String subject = "Đơn hàng đang được giao " + resolveOrderCode(order);
        sendHtml(user.getEmail(), subject, renderStandardEmail(
                subject,
                renderOrderStatusEmail("order-shipping.html", user, order)
        ));
    }

    public void sendOrderCompleted(User user, CustomerOrder order) {
        String subject = "Đơn hàng đã hoàn thành " + resolveOrderCode(order);
        sendHtml(user.getEmail(), subject, renderStandardEmail(
                subject,
                renderOrderStatusEmail("order-completed.html", user, order)
        ));
    }

    @Transactional
    public EmailVerificationToken createAndSendRegisterVerificationCode(String email) {
        EmailVerificationToken token = createRegisterVerificationToken(email);
        sendVerificationCode(token);
        return token;
    }

    @Transactional
    public EmailVerificationToken createRegisterVerificationToken(String email) {
        String normalizedEmail = normalizeRequiredEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new UserDuplicate("EmailUserDuplicate", "Email đã tồn tại");
        }

        return createVerificationToken(null, normalizedEmail, EmailVerificationPurpose.REGISTER_VERIFY);
    }

    @Transactional
    public EmailVerificationToken createAndSendVerificationCode(
            Long userId,
            String email,
            EmailVerificationPurpose purpose
    ) {
        EmailVerificationToken token = createVerificationToken(userId, email, purpose);
        sendVerificationCode(token);
        return token;
    }

    @Transactional
    public EmailVerificationToken createVerificationToken(
            Long userId,
            String email,
            EmailVerificationPurpose purpose
    ) {
        if (purpose == null) {
            throw new EmailValidationException("EmailTokenPurposeRequired", "Mã xác thực cần có mục đích sử dụng");
        }

        String normalizedEmail = normalizeRequiredEmail(email);
        User user = resolveTokenUser(userId, normalizedEmail, purpose);
        OffsetDateTime now = OffsetDateTime.now();

        tokenRepository.invalidateActiveTokens(normalizedEmail, purpose, now);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId(user != null ? user.getId() : null);
        token.setEmail(normalizedEmail);
        token.setCode(generateCode());
        token.setPurpose(purpose);
        token.setVerified(false);
        token.setExpiresAt(now.plusMinutes(verificationCodeExpMinutes));

        return tokenRepository.save(token);
    }

    public void sendVerificationCode(EmailVerificationToken token) {
        EmailTemplateContent content = toVerificationContent(token);
        sendHtml(token.getEmail(), content.subject(), renderVerificationEmail(content));
    }

    @Transactional
    public EmailVerificationToken verifyCode(
            String email,
            String code,
            EmailVerificationPurpose purpose
    ) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(code) || purpose == null) {
            throw new EmailValidationException("EmailTokenVerifyRequestInvalid", "Thông tin xác thực email không hợp lệ");
        }

        EmailVerificationToken token = tokenRepository
                .findFirstByEmailAndPurposeAndCodeAndVerifiedFalseAndUsedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(
                        email.trim().toLowerCase(),
                        purpose,
                        code.trim()
                )
                .orElseThrow(() -> new EmailValidationException(
                        "EmailTokenInvalid",
                        "Mã xác thực không đúng hoặc đã bị thay thế"
                ));

        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new EmailValidationException("EmailTokenExpired", "Mã xác thực đã hết hạn");
        }

        token.setVerified(true);
        token.setUsedAt(OffsetDateTime.now());
        return tokenRepository.save(token);
    }

    private void sendMime(String to, String subject, String body, boolean html) {
        if (!StringUtils.hasText(to)) {
            throw new EmailValidationException("EmailRecipientRequired", "Email người nhận không được để trống");
        }
        if (!StringUtils.hasText(subject)) {
            throw new EmailValidationException("EmailSubjectRequired", "Tiêu đề email không được để trống");
        }
        if (!StringUtils.hasText(body)) {
            throw new EmailValidationException("EmailBodyRequired", "Nội dung email không được để trống");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    StandardCharsets.UTF_8.name()
            );
            if (StringUtils.hasText(fromAddress)) {
                helper.setFrom(fromAddress.trim());
            }
            helper.setTo(to.trim());
            helper.setSubject(subject.trim());
            helper.setText(body, html);
            mailSender.send(message);
        } catch (MailException | MessagingException ex) {
            throw new EmailSendException("EmailSendFailed", "Không thể gửi email");
        }
    }

    private String renderVerificationEmail(EmailTemplateContent content) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("intro", escapeHtml(content.intro()));
        values.put("code", escapeHtml(content.code()));
        values.put("expirationMinutes", String.valueOf(verificationCodeExpMinutes));
        String body = renderTemplate("verification-code.html", values);
        return renderStandardEmail(content.title(), body);
    }

    private String renderShopRegistrationEmail(
            String title,
            String ownerName,
            String shopName,
            String contentHtml
    ) {
        String greetingName = StringUtils.hasText(ownerName) ? ownerName.trim() : "chu shop";
        String displayShopName = StringUtils.hasText(shopName) ? shopName.trim() : "shop cua ban";
        Map<String, String> values = new LinkedHashMap<>();
        values.put("ownerName", escapeHtml(greetingName));
        values.put("shopName", escapeHtml(displayShopName));
        values.put("contentHtml", contentHtml);
        values.put("brandName", escapeHtml(brandName));
        String body = renderTemplate("shop-registration.html", values);
        return renderStandardEmail(title, body);
    }

    private String renderPlainTextContent(String content) {
        String escaped = escapeHtml(content == null ? "" : content.trim());
        return "<p>" + escaped.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "<br>") + "</p>";
    }

    private String renderStandardEmail(String title, String bodyHtml) {
        String support = StringUtils.hasText(supportEmail) ? supportEmail.trim() : "";
        String supportLine = StringUtils.hasText(support)
                ? "Cần hỗ trợ? Liên hệ " + escapeHtml(support)
                : "Cần hỗ trợ? Vui lòng liên hệ đội ngũ chăm sóc khách hàng.";

        Map<String, String> values = new LinkedHashMap<>();
        values.put("title", escapeHtml(title));
        values.put("brandName", escapeHtml(brandName));
        values.put("body", bodyHtml);
        values.put("supportLine", supportLine);
        return renderTemplate("layout.html", values);
    }

    private String renderOrderStatusEmail(String templateName, User user, CustomerOrder order) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("customerName", escapeHtml(resolveCustomerName(user, order)));
        values.put("orderCode", escapeHtml(resolveOrderCode(order)));
        values.put("receiverName", escapeHtml(safeText(order.getReceiverName())));
        values.put("shippingAddress", escapeHtml(safeText(order.getShippingAddress())));
        values.put("totalAmount", escapeHtml(formatVnd(order.getTotalAmount())));
        values.put("brandName", escapeHtml(brandName));
        return renderTemplate(templateName, values);
    }

    private String renderTemplate(String templateName, Map<String, String> values) {
        String result = loadTemplate(templateName);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    private String loadTemplate(String templateName) {
        return templateCache.computeIfAbsent(templateName, name -> {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_DIR + name);
            try (InputStream inputStream = resource.getInputStream()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new EmailSendException("EmailTemplateLoadFailed", "Không thể đọc template email");
            }
        });
    }

    private EmailTemplateContent toVerificationContent(EmailVerificationToken token) {
        return switch (token.getPurpose()) {
            case REGISTER_VERIFY -> new EmailTemplateContent(
                    "Xác thực đăng ký tài khoản",
                    "Xác thực đăng ký tài khoản",
                    "Cảm ơn bạn đã đăng ký tài khoản. Nhập mã dưới đây để hoàn tất xác thực email.",
                    token.getCode()
            );
            case RESET_PASSWORD -> new EmailTemplateContent(
                    "Xác thực đặt lại mật khẩu",
                    "Xác thực đặt lại mật khẩu",
                    "Bạn vừa yêu cầu đặt lại mật khẩu. Nhập mã dưới đây để tiếp tục.",
                    token.getCode()
            );
            case CHANGE_EMAIL -> new EmailTemplateContent(
                    "Xác thực đổi email",
                    "Xác thực đổi email",
                    "Bạn vừa yêu cầu đổi email. Nhập mã dưới đây để xác nhận địa chỉ email mới.",
                    token.getCode()
            );
        };
    }

    private String generateCode() {
        return String.valueOf(MIN_CODE + SECURE_RANDOM.nextInt(CODE_RANGE));
    }

    private User resolveTokenUser(Long userId, String email, EmailVerificationPurpose purpose) {
        if (userId != null) {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new EmailValidationException(
                            "EmailTokenUserNotFound",
                            "Không tìm thấy người dùng để gửi mã xác thực"
                    ));
        }

        if (purpose == EmailVerificationPurpose.REGISTER_VERIFY) {
            if (userRepository.existsByEmail(email)) {
                throw new UserDuplicate("EmailUserDuplicate", "Email đã tồn tại");
            }
            return null;
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EmailValidationException(
                        "EmailTokenUserNotFound",
                        "Không tìm thấy người dùng với email này"
                ));
    }

    private String normalizeRequiredEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new EmailValidationException("EmailTokenEmailRequired", "Mã xác thực cần có email");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String formatVnd(Long amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        return formatter.format(amount != null ? amount : 0L) + " VND";
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String resolveOrderCode(CustomerOrder order) {
        if (order == null || order.getId() == null) {
            return "";
        }
        return StringUtils.hasText(order.getOrderCode()) ? order.getOrderCode().trim() : "#" + order.getId();
    }

    private String resolveCustomerName(User user, CustomerOrder order) {
        if (user != null && StringUtils.hasText(user.getFullName())) {
            return user.getFullName().trim();
        }
        if (order != null && StringUtils.hasText(order.getReceiverName())) {
            return order.getReceiverName().trim();
        }
        return "bạn";
    }

    /**
     * Tìm token hợp lệ mà không thay đổi trạng thái
     */
    public EmailVerificationToken findValidToken(
            String email,
            String code,
            EmailVerificationPurpose purpose
    ) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(code) || purpose == null) {
            return null;
        }

        return tokenRepository
                .findFirstByEmailAndPurposeAndCodeAndUsedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(
                        email.trim().toLowerCase(),
                        purpose,
                        code.trim()
                )
                .orElse(null);
    }

    /**
     * Lưu token
     */
    public EmailVerificationToken saveToken(EmailVerificationToken token) {
        return tokenRepository.save(token);
    }

    /**
     * Gửi email xác nhận thay đổi mật khẩu thành công
     */
    public void sendResetPasswordConfirmationEmail(String email, String fullName) {
        String subject = "Mật khẩu của bạn đã được thay đổi";
        Map<String, String> values = new LinkedHashMap<>();
        values.put("fullName", escapeHtml(fullName));
        values.put("brandName", escapeHtml(brandName));
        String body = renderTemplate("reset-password-confirmation.html", values);
        
        sendHtml(email, subject, renderStandardEmail(subject, body));
    }

    private record EmailTemplateContent(
            String subject,
            String title,
            String intro,
            String code
    ) {
    }
}
