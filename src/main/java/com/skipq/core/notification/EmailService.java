package com.skipq.core.notification;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.skipq.core.auth.OtpPurpose;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final Resend resend;
    private final String from;

    public EmailService(
            @Value("${app.resend.api-key}") String apiKey,
            @Value("${app.resend.from}") String from) {
        this.resend = new Resend(apiKey);
        this.from = from;
    }

    public void sendOtp(String toEmail, String name, String code, OtpPurpose purpose) {
        String subject = switch (purpose) {
            case VERIFY_EMAIL  -> "Verify your SkipQ account — " + code;
            case STUDENT_RESET -> "Reset your SkipQ password — " + code;
            case VENDOR_RESET  -> "Reset your SkipQ Vendor password — " + code;
        };

        String html = switch (purpose) {
            case VERIFY_EMAIL  -> verifyEmailHtml(name, code);
            case STUDENT_RESET -> resetHtml(name, code, "SkipQ");
            case VENDOR_RESET  -> resetHtml(name, code, "SkipQ Vendor Hub");
        };

        send(toEmail, subject, html);
    }

    // -------------------------------------------------------------------------

    private String verifyEmailHtml(String name, String code) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"></head>
                <body style="margin:0;padding:0;background-color:#F5F5F4;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                    <tr>
                      <td align="center" style="padding:48px 16px;">
                        <table role="presentation" cellspacing="0" cellpadding="0" border="0"
                               style="width:100%%;max-width:520px;background:#FFFFFF;border-radius:20px;overflow:hidden;box-shadow:0 8px 40px rgba(0,0,0,0.10);">

                          <!-- Header -->
                          <tr>
                            <td style="background:linear-gradient(140deg,#E8521F 0%%,#FF6B35 45%%,#FF9A5C 100%%);padding:40px 48px 36px;">
                              <p style="margin:0;font-size:26px;font-weight:900;color:#FFFFFF;letter-spacing:-0.5px;">
                                Skip<span style="color:rgba(255,255,255,0.50);">Q</span>
                              </p>
                              <p style="margin:22px 0 0;font-size:28px;font-weight:800;color:#FFFFFF;line-height:1.2;letter-spacing:-0.3px;">Verify your email</p>
                              <p style="margin:8px 0 0;font-size:14px;color:rgba(255,255,255,0.75);line-height:1.5;">You're one step away from ordering.</p>
                            </td>
                          </tr>

                          <!-- Body -->
                          <tr>
                            <td style="padding:36px 48px 0;">
                              <p style="margin:0;font-size:15px;color:#374151;line-height:1.8;">
                                Hey <strong style="color:#111827;">%s</strong> — use the code below to confirm your email address. It expires in <strong>10 minutes</strong>.
                              </p>
                            </td>
                          </tr>

                          <!-- OTP Box -->
                          <tr>
                            <td style="padding:24px 48px 0;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0"
                                     style="background:#FFF7ED;border:1.5px solid #FED7AA;border-radius:14px;">
                                <tr>
                                  <td align="center" style="padding:30px 24px;">
                                    <p style="margin:0 0 12px;font-size:11px;font-weight:700;color:#92400E;text-transform:uppercase;letter-spacing:2.5px;">Verification Code</p>
                                    <p style="margin:0;font-size:46px;font-weight:900;color:#C2410C;letter-spacing:12px;font-family:'Courier New',Courier,monospace;">%s</p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Timer pill -->
                          <tr>
                            <td align="center" style="padding:16px 48px 0;">
                              <table role="presentation" cellspacing="0" cellpadding="0" border="0">
                                <tr>
                                  <td style="background:#F3F4F6;border-radius:20px;padding:6px 16px;">
                                    <p style="margin:0;font-size:12px;color:#6B7280;font-weight:600;">Expires in 10 minutes</p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Security note -->
                          <tr>
                            <td style="padding:24px 48px 0;">
                              <p style="margin:0;font-size:13px;color:#9CA3AF;line-height:1.7;padding-left:14px;border-left:3px solid #E5E7EB;">
                                Didn't create a SkipQ account? You can safely ignore this email — the code will expire on its own.
                              </p>
                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="padding:32px 48px 36px;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                                <tr>
                                  <td style="border-top:1px solid #F3F4F6;padding-top:24px;">
                                    <p style="margin:0;font-size:13px;color:#D1D5DB;">
                                      © 2025 SkipQ &nbsp;·&nbsp; <span style="color:#FF6B35;font-weight:600;">Campus Food, Reimagined</span>
                                    </p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(name, code);
    }

    private String resetHtml(String name, String code, String appName) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"></head>
                <body style="margin:0;padding:0;background-color:#F5F5F4;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                    <tr>
                      <td align="center" style="padding:48px 16px;">
                        <table role="presentation" cellspacing="0" cellpadding="0" border="0"
                               style="width:100%%;max-width:520px;background:#FFFFFF;border-radius:20px;overflow:hidden;box-shadow:0 8px 40px rgba(0,0,0,0.10);">

                          <!-- Header: dark — signals a security action -->
                          <tr>
                            <td style="background:linear-gradient(140deg,#1A1A1C 0%%,#2C2C2F 50%%,#3A3A3E 100%%);padding:40px 48px 36px;">
                              <p style="margin:0;font-size:26px;font-weight:900;letter-spacing:-0.5px;">
                                <span style="color:#FF6B35;">Skip</span><span style="color:rgba(255,107,53,0.45);">Q</span>
                              </p>
                              <p style="margin:22px 0 0;font-size:28px;font-weight:800;color:#FFFFFF;line-height:1.2;letter-spacing:-0.3px;">Password reset</p>
                              <p style="margin:8px 0 0;font-size:14px;color:rgba(255,255,255,0.50);line-height:1.5;">We received your request.</p>
                            </td>
                          </tr>

                          <!-- Body -->
                          <tr>
                            <td style="padding:36px 48px 0;">
                              <p style="margin:0;font-size:15px;color:#374151;line-height:1.8;">
                                Hey <strong style="color:#111827;">%s</strong> — here's your one-time code to reset your <strong>%s</strong> account password. It expires in <strong>10 minutes</strong>.
                              </p>
                            </td>
                          </tr>

                          <!-- OTP Box -->
                          <tr>
                            <td style="padding:24px 48px 0;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0"
                                     style="background:#F9FAFB;border:1.5px solid #E5E7EB;border-radius:14px;">
                                <tr>
                                  <td align="center" style="padding:30px 24px;">
                                    <p style="margin:0 0 12px;font-size:11px;font-weight:700;color:#6B7280;text-transform:uppercase;letter-spacing:2.5px;">Password Reset Code</p>
                                    <p style="margin:0;font-size:46px;font-weight:900;color:#111827;letter-spacing:12px;font-family:'Courier New',Courier,monospace;">%s</p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Timer pill -->
                          <tr>
                            <td align="center" style="padding:16px 48px 0;">
                              <table role="presentation" cellspacing="0" cellpadding="0" border="0">
                                <tr>
                                  <td style="background:#F3F4F6;border-radius:20px;padding:6px 16px;">
                                    <p style="margin:0;font-size:12px;color:#6B7280;font-weight:600;">Expires in 10 minutes</p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Security warning -->
                          <tr>
                            <td style="padding:24px 48px 0;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0"
                                     style="background:#FFFBEB;border:1px solid #FDE68A;border-radius:10px;">
                                <tr>
                                  <td style="padding:14px 18px;">
                                    <p style="margin:0;font-size:13px;color:#92400E;line-height:1.7;">
                                      <strong>Didn't request this?</strong> Your account is safe — no action needed. This code is useless without your new password.
                                    </p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="padding:32px 48px 36px;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                                <tr>
                                  <td style="border-top:1px solid #F3F4F6;padding-top:24px;">
                                    <p style="margin:0;font-size:13px;color:#D1D5DB;">
                                      © 2025 SkipQ &nbsp;·&nbsp; <span style="color:#FF6B35;font-weight:600;">Campus Food, Reimagined</span>
                                    </p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(name, appName, code);
    }

    private void send(String toEmail, String subject, String html) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(from)
                .to(toEmail)
                .subject(subject)
                .html(html)
                .build();
        try {
            resend.emails().send(params);
            log.info("Email sent to {}", toEmail);
        } catch (ResendException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendVendorInvite(String toEmail, String vendorName, String token) {
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
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0"
                               style="max-width:560px;background:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
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
                                    <p style="margin:0;font-size:22px;font-weight:700;color:#FFFFFF;line-height:1.3;">You're invited to join SkipQ</p>
                                    <p style="margin:8px 0 0;font-size:14px;color:rgba(255,255,255,0.85);line-height:1.5;">Your vendor account is ready to set up</p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:36px 40px 0;">
                              <p style="margin:0 0 8px;font-size:14px;color:#6B7280;font-weight:500;text-transform:uppercase;letter-spacing:0.5px;">Hello,</p>
                              <p style="margin:0 0 20px;font-size:24px;font-weight:700;color:#111827;">%s</p>
                              <p style="margin:0;font-size:15px;color:#4B5563;line-height:1.7;">
                                Welcome to SkipQ — the smart campus food ordering platform. Your vendor account has been created and is ready for you to take ownership.
                              </p>
                              <p style="margin:16px 0 0;font-size:15px;color:#4B5563;line-height:1.7;">
                                To get started: open the <strong style="color:#111827;">SkipQ Vendor app</strong>, tap <strong style="color:#111827;">Set up your account</strong> on the login screen, and enter the token below.
                              </p>
                            </td>
                          </tr>
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
                          <tr>
                            <td style="padding:20px 40px 0;">
                              <table role="presentation" cellspacing="0" cellpadding="0" border="0"
                                     style="background:#FFF7ED;border-left:3px solid #FF6B35;border-radius:0 6px 6px 0;">
                                <tr>
                                  <td style="padding:12px 16px;">
                                    <p style="margin:0;font-size:13px;color:#92400E;line-height:1.5;">
                                      This invitation expires in <strong>24 hours</strong>. Set up your account before it expires.
                                    </p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
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
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(vendorName, token);

        send(toEmail, "You're invited to SkipQ — Set up your vendor account", html);
    }
}
