package com.exe101.email.service;

import com.exe101.email.dto.EmailSendRequest;
import com.exe101.email.entity.EmailVerificationPurpose;
import com.exe101.email.entity.EmailVerificationToken;
import com.exe101.email.exception.EmailSendException;
import com.exe101.email.exception.EmailValidationException;
import com.exe101.email.repository.IEmailVerificationTokenRepository;
import com.exe101.user.entity.User;
import com.exe101.user.exception.UserDuplicate;
import com.exe101.user.repository.IUserRepository;
import jakarta.mail                                                             .MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MIN_CODE = 100000;
    private static final int CODE_RANGE = 900000;

    private final JavaMailSender mailSender;
    private final IEmailVerificationTokenRepository tokenRepository;
    private final IUserRepository userRepository;

    @Value("${app.email.from:}")
    private String fromAddress;

    @Value("${app.email.brand-name:EXE101 Pet Spa}")
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
        String subject = "Dang ky shop da duoc chap nhan";
        String content = """
                <p>Ho so dang ky shop cua ban da duoc chap nhan.</p>
                <p>Ban co the dang nhap vao trang quan ly shop va bat dau cap nhat thong tin, dich vu, san pham cua shop.</p>
                """;
        sendHtml(to, subject, renderShopRegistrationEmail(subject, ownerName, shopName, content));
    }

    public void sendShopRegistrationRejected(String to, String ownerName, String shopName) {
        String subject = "Dang ky shop chua duoc chap nhan";
        String content = """
                <p>Ho so dang ky shop cua ban chua duoc chap nhan.</p>
                <p>Vui long kiem tra lai thong tin dang ky hoac lien he bo phan ho tro de duoc huong dan them.</p>
                """;
        sendHtml(to, subject, renderShopRegistrationEmail(subject, ownerName, shopName, content));
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
        String body = """
                <p>%s</p>
                <div style="margin:28px 0;text-align:center;">
                  <div style="display:inline-block;padding:18px 28px;border-radius:8px;background:#f4f7fb;border:1px solid #d8e0ec;font-size:30px;font-weight:700;letter-spacing:8px;color:#111827;">%s</div>
                </div>
                <p>Mã này có hiệu lực trong <strong>%d phút</strong>. Vui lòng không chia sẻ mã này với bất kỳ ai.</p>
                <p>Nếu bạn không thực hiện yêu cầu này, bạn có thể bỏ qua email.</p>
                """.formatted(
                escapeHtml(content.intro()),
                escapeHtml(content.code()),
                verificationCodeExpMinutes
        );
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
        String body = """
                <p>Xin chao <strong>%s</strong>,</p>
                <p>Day la thong bao lien quan den ho so dang ky shop <strong>%s</strong>.</p>
                <div style="margin:22px 0;padding:18px 20px;border:1px solid #d8e0ec;border-radius:8px;background:#f8fafc;">
                  %s
                </div>
                <p>Tran trong,<br>%s</p>
                """.formatted(
                escapeHtml(greetingName),
                escapeHtml(displayShopName),
                contentHtml,
                escapeHtml(brandName)
        );
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

        return """
                <!doctype html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background:#f3f6fa;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f3f6fa;padding:28px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:620px;background:#ffffff;border:1px solid #dfe6ef;border-radius:10px;overflow:hidden;">
                          <tr>
                            <td style="background:#0f766e;padding:24px 30px;color:#ffffff;">
                              <div style="font-size:20px;font-weight:700;">%s</div>
                              <div style="font-size:13px;margin-top:6px;opacity:.9;">Dịch vụ chăm sóc thú cưng</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:30px;">
                              <h1 style="margin:0 0 18px;font-size:22px;line-height:1.35;color:#111827;">%s</h1>
                              <div style="font-size:15px;line-height:1.7;color:#374151;">%s</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:18px 30px;background:#f9fafb;border-top:1px solid #e5e7eb;color:#6b7280;font-size:12px;line-height:1.6;">
                              <div>%s</div>
                              <div style="margin-top:8px;">Email này được gửi tự động từ %s. Vui lòng không trả lời trực tiếp email này.</div>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                escapeHtml(title),
                escapeHtml(brandName),
                escapeHtml(title),
                bodyHtml,
                supportLine,
                escapeHtml(brandName)
        );
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

    private record EmailTemplateContent(
            String subject,
            String title,
            String intro,
            String code
    ) {
    }
}
