package com.skipq.core.config;

import com.razorpay.Account;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RazorpayService {

    @Value("${app.razorpay.key-id}")
    private String keyId;

    @Value("${app.razorpay.key-secret}")
    private String keySecret;

    private RazorpayClient client;

    @PostConstruct
    public void init() throws RazorpayException {
        client = new RazorpayClient(keyId, keySecret);
    }

    public String createOrder(long amountPaise, String receiptId) throws RazorpayException {
        JSONObject options = new JSONObject();
        options.put("amount", amountPaise);
        options.put("currency", "INR");
        options.put("receipt", receiptId);
        options.put("payment_capture", 1);
        com.razorpay.Order order = client.orders.create(options);
        log.info("Razorpay order created: {} receipt={}", order.get("id"), receiptId);
        return order.get("id");
    }

    public void refund(String razorpayPaymentId, long amountPaise) throws RazorpayException {
        JSONObject options = new JSONObject();
        options.put("amount", amountPaise);
        client.payments.refund(razorpayPaymentId, options);
        log.info("Razorpay refund initiated for payment {}", razorpayPaymentId);
    }

    public void transferToVendor(String razorpayPaymentId,
                                  RazorpayTransferRequest request) throws RazorpayException {
        JSONObject options = new JSONObject();
        options.put("account",  request.linkedAccountId());
        options.put("amount",   request.amountPaise());
        options.put("currency", "INR");
        options.put("on_hold",  0);
        client.payments.transfer(razorpayPaymentId, options);
        log.info("Razorpay transfer initiated: payment={} account={} amount={}",
                razorpayPaymentId, request.linkedAccountId(), request.amountPaise());
    }

    public String createLinkedAccount(String businessName, String pan,
                                      String bankAccount, String ifsc) throws RazorpayException {
        JSONObject request = new JSONObject();
        request.put("email", businessName.toLowerCase().replaceAll("\\s+", "") + "@skipq.vendor");
        request.put("profile", new JSONObject()
                .put("category", "food_and_beverage")
                .put("subcategory", "food_court")
                .put("addresses", new JSONObject()
                        .put("registered", new JSONObject()
                                .put("street1", "Campus")
                                .put("city", "Hyderabad")
                                .put("state", "AP")
                                .put("postal_code", "500001")
                                .put("country", "IN")
                        )
                )
        );
        request.put("legal_info", new JSONObject()
                .put("pan", pan)
        );
        request.put("legal_business_name", businessName);
        request.put("business_type", "individual");

        // Bank account details for settlements
        request.put("settlements", new JSONObject()
                .put("account_number", bankAccount)
                .put("ifsc_code", ifsc)
                .put("beneficiary_name", businessName)
        );

        Account account = client.account.create(request);
        return account.get("id");
    }
}
