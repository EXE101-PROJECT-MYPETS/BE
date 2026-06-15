package com.exe101.subscription.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class SubscriptionPaymentProperties {

    @Value("${sepay.ipn-secret-key:}")
    private String sepayIpnSecretKey;

    @Value("${sepay.bank-code:}")
    private String sepayBankCode;

    @Value("${sepay.bank-name:MB Bank}")
    private String sepayBankName;

    @Value("${sepay.account-number:}")
    private String sepayAccountNumber;

    @Value("${sepay.account-name:}")
    private String sepayAccountName;

    @Value("${sepay.qr-base-url:https://qr.sepay.vn/img}")
    private String sepayQrBaseUrl;

    @Value("${sepay.webhook-auth-enabled:true}")
    private boolean sepayWebhookAuthEnabled;

    @Value("${subscription.payment-collection-enabled:false}")
    private boolean subscriptionPaymentCollectionEnabled;

    @Value("${subscription.monthly-price:500000}")
    private Long subscriptionMonthlyPrice;

    @Value("${subscription.monthly-days:30}")
    private Integer subscriptionMonthlyDays;

    @Value("${subscription.trial-days:15}")
    private Integer subscriptionTrialDays;

    @Value("${payment.expire-minutes:15}")
    private Integer paymentExpireMinutes;

    @Value("${payment.grace-minutes:10}")
    private Integer paymentGraceMinutes;

    @Value("${payment.late-grace-minutes:60}")
    private Integer paymentLateGraceMinutes;
}
