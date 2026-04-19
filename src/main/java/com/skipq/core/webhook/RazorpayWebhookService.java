package com.skipq.core.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipq.core.vendor.Vendor;
import com.skipq.core.vendor.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayWebhookService {

    private final VendorRepository vendorRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.razorpay.webhook-secret}")
    private String webhookSecret;

    @Transactional
    public void handle(String payload, String signature) {
        verifySignature(payload, signature);

        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText();
            log.info("Razorpay webhook received: {}", event);

            switch (event) {
                case "account.activated" -> handleAccountActivated(root);
                case "account.rejected"  -> handleAccountRejected(root);
                default -> log.debug("Unhandled Razorpay webhook event: {}", event);
            }
        } catch (Exception e) {
            log.error("Failed to parse Razorpay webhook payload", e);
            throw new RuntimeException("Webhook payload parsing failed", e);
        }
    }

    private void handleAccountActivated(JsonNode root) {
        String linkedAccountId = root.path("payload").path("account").path("entity").path("id").asText();

        if (linkedAccountId.isBlank()) {
            log.warn("account.activated webhook missing account id");
            return;
        }

        Vendor vendor = vendorRepository.findByRazorpayLinkedAccountId(linkedAccountId)
                .orElse(null);

        if (vendor == null) {
            log.warn("account.activated: no vendor found for linked account {}", linkedAccountId);
            return;
        }

        vendor.setKycApproved(true);
        vendorRepository.save(vendor);
        log.info("KYC approved for vendor {} (linked account {})", vendor.getId(), linkedAccountId);
    }

    private void handleAccountRejected(JsonNode root) {
        String linkedAccountId = root.path("payload").path("account").path("entity").path("id").asText();

        if (linkedAccountId.isBlank()) {
            log.warn("account.rejected webhook missing account id");
            return;
        }

        log.warn("KYC rejected by Razorpay for linked account {} — manual follow-up needed", linkedAccountId);
        // kycApproved stays false — vendor sees Pending status
        // Future: send email notification to vendor
    }

    private void verifySignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hash);

            if (!expected.equals(signature)) {
                throw new SecurityException("Webhook signature mismatch");
            }
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException("Webhook signature verification failed: " + e.getMessage());
        }
    }
}
