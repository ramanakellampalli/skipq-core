package com.skipq.core.notification;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final Resend resend;
    private final String from;
    private final String deepLinkBaseUrl;

    public EmailService(
            @Value("${app.resend.api-key}") String apiKey,
            @Value("${app.resend.from}") String from,
            @Value("${app.deep-link.base-url}") String deepLinkBaseUrl) {
        this.resend = new Resend(apiKey);
        this.from = from;
        this.deepLinkBaseUrl = deepLinkBaseUrl;
    }

    public void sendVendorInvite(String toEmail, String vendorName, String token) {
        String setupLink = deepLinkBaseUrl + "?token=" + token;

        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>Welcome to SkipQ</title>
                </head>
                <body style="margin:0;padding:0;background-color:#F4F4F5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                    <tr>
                      <td align="center" style="padding:40px 16px;">

                        <!-- Card -->
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0"
                               style="max-width:560px;background:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">

                          <!-- Header -->
                          <tr>
                            <td style="background:linear-gradient(135deg,#FF6B35 0%%,#FF8C42 100%%);padding:40px 40px 32px;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                                <tr>
                                  <td>
                                    <span style="font-size:28px;font-weight:800;color:#FFFFFF;letter-spacing:-0.5px;">Skip<span style="color:#FFE4D6;">Q</span></span>
                                  </td>
                                </tr>
                                <tr>
                                  <td style="padding-top:20px;">
                                    <p style="margin:0;font-size:22px;font-weight:700;color:#FFFFFF;line-height:1.3;">
                                      You're invited to join SkipQ
                                    </p>
                                    <p style="margin:8px 0 0;font-size:14px;color:rgba(255,255,255,0.85);line-height:1.5;">
                                      Your vendor account is ready to set up
                                    </p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Body -->
                          <tr>
                            <td style="padding:36px 40px 0;">
                              <p style="margin:0 0 8px;font-size:14px;color:#6B7280;font-weight:500;text-transform:uppercase;letter-spacing:0.5px;">Hello,</p>
                              <p style="margin:0 0 20px;font-size:24px;font-weight:700;color:#111827;">%s</p>
                              <p style="margin:0;font-size:15px;color:#4B5563;line-height:1.7;">
                                Welcome to SkipQ — the smart campus food ordering platform. Your vendor account has been created and is ready for you to take ownership.
                              </p>
                              <p style="margin:16px 0 0;font-size:15px;color:#4B5563;line-height:1.7;">
                                Tap the button below in the <strong style="color:#111827;">SkipQ Vendor app</strong> to set your password and go live.
                              </p>
                            </td>
                          </tr>

                          <!-- CTA Button -->
                          <tr>
                            <td style="padding:32px 40px 0;">
                              <table role="presentation" cellspacing="0" cellpadding="0" border="0">
                                <tr>
                                  <td style="border-radius:10px;background:linear-gradient(135deg,#FF6B35 0%%,#FF8C42 100%%);box-shadow:0 4px 12px rgba(255,107,53,0.35);">
                                    <a href="%s"
                                       style="display:inline-block;padding:14px 32px;font-size:15px;font-weight:700;color:#FFFFFF;text-decoration:none;letter-spacing:0.2px;">
                                      Set Up My Account →
                                    </a>
                                  </td>
                                </tr>
                              </table>
                              <p style="margin:12px 0 0;font-size:12px;color:#9CA3AF;">
                                Button not working? Open the SkipQ Vendor app and paste the token below.
                              </p>
                            </td>
                          </tr>

                          <!-- Token Box -->
                          <tr>
                            <td style="padding:24px 40px 0;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0"
                                     style="background:#F9FAFB;border:1px dashed #E5E7EB;border-radius:10px;">
                                <tr>
                                  <td style="padding:20px 24px;">
                                    <p style="margin:0 0 6px;font-size:11px;font-weight:600;color:#9CA3AF;text-transform:uppercase;letter-spacing:0.8px;">Your Setup Token</p>
                                    <p style="margin:0;font-size:14px;font-weight:700;color:#374151;font-family:'Courier New',Courier,monospace;word-break:break-all;">%s</p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Expiry Warning -->
                          <tr>
                            <td style="padding:20px 40px 0;">
                              <table role="presentation" cellspacing="0" cellpadding="0" border="0"
                                     style="background:#FFF7ED;border-left:3px solid #FF6B35;border-radius:0 6px 6px 0;">
                                <tr>
                                  <td style="padding:12px 16px;">
                                    <p style="margin:0;font-size:13px;color:#92400E;line-height:1.5;">
                                      ⏱ This invitation expires in <strong>24 hours</strong>. Set up your account before it expires.
                                    </p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Divider + Footer -->
                          <tr>
                            <td style="padding:36px 40px 32px;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                                <tr>
                                  <td style="border-top:1px solid #F3F4F6;padding-top:24px;">
                                    <p style="margin:0;font-size:13px;color:#9CA3AF;line-height:1.6;">
                                      If you weren't expecting this email, you can safely ignore it. This invite was sent by the SkipQ admin team.
                                    </p>
                                    <p style="margin:16px 0 0;font-size:13px;color:#D1D5DB;">
                                      © 2025 SkipQ &nbsp;·&nbsp; <span style="color:#FF6B35;">Campus Food, Reimagined</span>
                                    </p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                        </table>
                        <!-- End Card -->

                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(vendorName, setupLink, token);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(from)
                .to(toEmail)
                .subject("You're invited to SkipQ — Set up your vendor account")
                .html(html)
                .build();

        try {
            resend.emails().send(params);
            log.info("Vendor invite sent to {}", toEmail);
        } catch (ResendException e) {
            log.error("Failed to send vendor invite to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send invite email", e);
        }
    }
}
