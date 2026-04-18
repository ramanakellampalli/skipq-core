package com.skipq.core.config;

import com.razorpay.Account;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
