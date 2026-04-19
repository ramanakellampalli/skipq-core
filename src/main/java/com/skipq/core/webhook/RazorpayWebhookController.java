package com.skipq.core.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/webhooks/razorpay")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private final RazorpayWebhookService webhookService;

    @PostMapping
    public ResponseEntity<Void> handle(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        if (signature == null || signature.isBlank()) {
            log.warn("Razorpay webhook received with no signature — rejected");
            return ResponseEntity.badRequest().build();
        }

        try {
            webhookService.handle(payload, signature);
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            log.warn("Razorpay webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            log.error("Razorpay webhook processing error", e);
            // Return 200 so Razorpay doesn't keep retrying for non-signature errors
            return ResponseEntity.ok().build();
        }
    }
}
