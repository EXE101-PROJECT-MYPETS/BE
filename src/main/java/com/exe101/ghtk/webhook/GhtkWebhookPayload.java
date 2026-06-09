package com.exe101.ghtk.webhook;

import org.springframework.util.MultiValueMap;

public record GhtkWebhookPayload(
        String partnerId,
        String labelId,
        String statusId,
        String actionTime,
        String reasonCode,
        String reason,
        String weight,
        String fee,
        String pickMoney,
        String returnPartPackage
) {

    public static GhtkWebhookPayload fromForm(MultiValueMap<String, String> formData) {
        return new GhtkWebhookPayload(
                first(formData, "partner_id"),
                first(formData, "label_id"),
                first(formData, "status_id"),
                first(formData, "action_time"),
                first(formData, "reason_code"),
                first(formData, "reason"),
                first(formData, "weight"),
                first(formData, "fee"),
                first(formData, "pick_money"),
                first(formData, "return_part_package")
        );
    }

    private static String first(MultiValueMap<String, String> formData, String key) {
        String value = formData.getFirst(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
