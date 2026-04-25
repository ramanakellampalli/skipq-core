-- UPI-only: Razorpay charges 0% for UPI; payment_terminal_fee is always 0
-- Backfill existing rows so total_service_fee = platform_fee, total_amount is corrected
UPDATE orders
SET total_service_fee = platform_fee,
    total_amount      = subtotal + tax_amount + platform_fee
WHERE payment_terminal_fee != 0;

ALTER TABLE orders DROP COLUMN payment_terminal_fee;
