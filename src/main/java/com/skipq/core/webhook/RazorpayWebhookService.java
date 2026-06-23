package com.skipq.core.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipq.core.order.OrderService;
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

    private final OrderService orderService;
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
                case "payment.captured" -> handlePaymentCaptured(root);
                case "payment.failed"   -> handlePaymentFailed(root);
                case "refund.processed" -> handleRefundProcessed(root);
                case "refund.failed"    -> handleRefundFailed(root);
                default -> log.debug("Unhandled Razorpay webhook event: {}", event);
            }
        } catch (Exception e) {
            log.error("Failed to parse Razorpay webhook payload", e);
            throw new RuntimeException("Webhook payload parsing failed", e);
        }
    }

    private void handlePaymentCaptured(JsonNode root) {
        JsonNode entity = root.path("payload").path("payment").path("entity");
        String razorpayOrderId   = entity.path("order_id").asText();
        String razorpayPaymentId = entity.path("id").asText();

        if (razorpayOrderId.isBlank() || razorpayPaymentId.isBlank()) {
            log.warn("payment.captured webhook missing order_id or payment id");
            return;
        }

        orderService.confirmPayment(razorpayOrderId, razorpayPaymentId);
        log.info("Payment confirmed: razorpay_order={} payment={}", razorpayOrderId, razorpayPaymentId);
    }

    private void handlePaymentFailed(JsonNode root) {
        String razorpayOrderId = root.path("payload").path("payment").path("entity").path("order_id").asText();

        if (razorpayOrderId.isBlank()) {
            log.warn("payment.failed webhook missing order_id");
            return;
        }

        orderService.handlePaymentFailed(razorpayOrderId);
        log.info("Payment failed, order draft deleted: razorpay_order={}", razorpayOrderId);
    }

    private void handleRefundProcessed(JsonNode root) {
        JsonNode entity = root.path("payload").path("refund").path("entity");
        String refundId  = entity.path("id").asText();
        String paymentId = entity.path("payment_id").asText();
        long   amount    = entity.path("amount").asLong();
        log.info("Refund processed: refund_id={} payment_id={} amount_paise={}", refundId, paymentId, amount);
    }

    private void handleRefundFailed(JsonNode root) {
        JsonNode entity = root.path("payload").path("refund").path("entity");
        String refundId  = entity.path("id").asText();
        String paymentId = entity.path("payment_id").asText();
        long   amount    = entity.path("amount").asLong();
        log.error("REFUND FAILED — manual intervention required: refund_id={} payment_id={} amount_paise={}", refundId, paymentId, amount);
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
