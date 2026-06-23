package com.skipq.core.webhook;

import com.skipq.core.order.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RazorpayWebhookServiceTest {

    private static final String SECRET = "test_webhook_secret";

    @Mock OrderService orderService;

    RazorpayWebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new RazorpayWebhookService(orderService, new ObjectMapper());
        ReflectionTestUtils.setField(webhookService, "webhookSecret", SECRET);
    }

    // ── signature verification ────────────────────────────────────────────────

    @Test
    void handle_invalidSignature_throwsSecurityException() {
        String payload = "{\"event\":\"payment.captured\"}";
        assertThatThrownBy(() -> webhookService.handle(payload, "bad_signature"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void handle_validSignature_doesNotThrow() throws Exception {
        String payload = "{\"event\":\"unknown.event\"}";
        webhookService.handle(payload, sign(payload));
        // no exception — dispatch to default branch
    }

    // ── payment.captured ──────────────────────────────────────────────────────

    @Test
    void handle_paymentCaptured_delegatesToConfirmPayment() throws Exception {
        String payload = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_abc456",
                        "order_id": "order_rzp123"
                      }
                    }
                  }
                }""";

        webhookService.handle(payload, sign(payload));

        verify(orderService).confirmPayment("order_rzp123", "pay_abc456");
    }

    @Test
    void handle_paymentCaptured_missingOrderId_noDelegate() throws Exception {
        String payload = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_abc456",
                        "order_id": ""
                      }
                    }
                  }
                }""";

        webhookService.handle(payload, sign(payload));

        verifyNoInteractions(orderService);
    }

    @Test
    void handle_paymentCaptured_missingPaymentId_noDelegate() throws Exception {
        String payload = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "",
                        "order_id": "order_rzp123"
                      }
                    }
                  }
                }""";

        webhookService.handle(payload, sign(payload));

        verifyNoInteractions(orderService);
    }

    // ── payment.failed ────────────────────────────────────────────────────────

    @Test
    void handle_paymentFailed_delegatesToHandlePaymentFailed() throws Exception {
        String payload = """
                {
                  "event": "payment.failed",
                  "payload": {
                    "payment": {
                      "entity": {
                        "order_id": "order_rzp123"
                      }
                    }
                  }
                }""";

        webhookService.handle(payload, sign(payload));

        verify(orderService).handlePaymentFailed("order_rzp123");
    }

    @Test
    void handle_paymentFailed_missingOrderId_noDelegate() throws Exception {
        String payload = """
                {
                  "event": "payment.failed",
                  "payload": {
                    "payment": {
                      "entity": {
                        "order_id": ""
                      }
                    }
                  }
                }""";

        webhookService.handle(payload, sign(payload));

        verifyNoInteractions(orderService);
    }

    // ── refund.processed ──────────────────────────────────────────────────────

    @Test
    void handle_refundProcessed_noServiceInteraction() throws Exception {
        String payload = """
                {
                  "event": "refund.processed",
                  "payload": {
                    "refund": {
                      "entity": {
                        "id": "rfnd_abc123",
                        "payment_id": "pay_xyz789",
                        "amount": 10300
                      }
                    }
                  }
                }""";

        webhookService.handle(payload, sign(payload));

        verifyNoInteractions(orderService);
    }

    // ── refund.failed ─────────────────────────────────────────────────────────

    @Test
    void handle_refundFailed_noServiceInteraction() throws Exception {
        String payload = """
                {
                  "event": "refund.failed",
                  "payload": {
                    "refund": {
                      "entity": {
                        "id": "rfnd_fail123",
                        "payment_id": "pay_xyz789",
                        "amount": 10300
                      }
                    }
                  }
                }""";

        webhookService.handle(payload, sign(payload));

        verifyNoInteractions(orderService);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String sign(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
