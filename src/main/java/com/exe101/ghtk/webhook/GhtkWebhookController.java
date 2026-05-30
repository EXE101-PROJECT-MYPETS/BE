package com.exe101.ghtk.webhook;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GhtkWebhookController {

    private final GhtkWebhookService ghtkWebhookService;

    @PostMapping(
            value = "/api/public/webhooks/ghtk",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public ResponseEntity<Void> handleWebhook(
            @RequestParam(value = "hash", required = false) String hash,
            @RequestParam MultiValueMap<String, String> formData
    ) {
        if (!ghtkWebhookService.isValidHash(hash)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ghtkWebhookService.handle(hash, formData);
        return ResponseEntity.ok().build();
    }
}
