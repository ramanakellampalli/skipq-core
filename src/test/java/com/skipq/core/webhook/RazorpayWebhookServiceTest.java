package com.skipq.core.webhook;

import com.skipq.core.order.OrderService;
import com.skipq.core.vendor.Vendor;
import com.skipq.core.vendor.VendorRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RazorpayWebhookServiceTest {

    private static final String SECRET = "test_webhook_secret";

    @Mock VendorRepository vendorRepository;
    @Mock OrderService orderService;

    RazorpayWebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new RazorpayWebhookService(vendorRepository, orderService, new ObjectMapper());
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

    // ── account.instantly_activated ───────────────────────────────────────────

    @Test
    void handle_accountInstantlyActivated_setsKycApprovedOnVendor() throws Exception {
        String linkedId = "acc_linked123";
        Vendor vendor = Vendor.builder().id(UUID.randomUUID()).name("Stall").kycApproved(false).build();
        when(vendorRepository.findByRazorpayLinkedAccountId(linkedId)).thenReturn(Optional.of(vendor));

        String payload = """
                {
                  "event": "account.instantly_activated",
                  "payload": {
                    "account": {
                      "entity": {
                        "id": "%s"
                      }
                    }
                  }
                }""".formatted(linkedId);

        webhookService.handle(payload, sign(payload));

        assertThat(vendor.isKycApproved()).isTrue();
        verify(vendorRepository).save(vendor);
    }

    @Test
    void handle_accountInstantlyActivated_vendorNotFound_noSave() throws Exception {
        when(vendorRepository.findByRazorpayLinkedAccountId(anyString())).thenReturn(Optional.empty());

        String payload = """
                {
                  "event": "account.instantly_activated",
                  "payload": {
                    "account": {
                      "entity": {
                        "id": "acc_unknown"
                      }
                    }
                  }
                }""";

        webhookService.handle(payload, sign(payload));

        verify(vendorRepository, never()).save(any());
    }

    @Test
    void handle_accountInstantlyActivated_missingId_noLookup() throws Exception {
        String payload = """
                {
                  "event": "account.instantly_activated",
                  "payload": {
                    "account": {
                      "entity": {
                        "id": ""
                      }
                    }
                  }
                }""";

        webhookService.handle(payload, sign(payload));

        verifyNoInteractions(vendorRepository);
    }

    // ── account.activated_kyc_pending ─────────────────────────────────────────

    @Test
    void handle_accountActivatedKycPending_noServiceInteraction() throws Exception {
        String payload = """
                {
                  "event": "account.activated_kyc_pending",
                  "payload": {
                    "account": {
                      "entity": {
                        "id": "acc_linked123"
                      }
                    }
                  }
                }""";

        webhookService.handle(payload, sign(payload));

        verifyNoInteractions(orderService, vendorRepository);
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

        verifyNoInteractions(orderService, vendorRepository);
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

        verifyNoInteractions(orderService, vendorRepository);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String sign(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
