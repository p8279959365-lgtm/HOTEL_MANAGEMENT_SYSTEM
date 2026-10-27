-- HotelOS functionality upgrade: run this ONCE in MySQL Workbench after selecting HOTEL_MANAGEMENT_SYSTEM.
-- This script adds columns only; it does not drop or recreate tables or delete existing records.

ALTER TABLE customers
  ADD COLUMN id_proof_type VARCHAR(40) NULL;

ALTER TABLE bookings
  ADD COLUMN id_proof_type VARCHAR(40) NULL,
  ADD COLUMN id_proof VARCHAR(120) NULL,
  ADD COLUMN id_verified BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN id_verified_at DATETIME NULL;

-- Check duplicate phone numbers before optionally enforcing uniqueness:
-- SELECT phone, COUNT(*) AS duplicates FROM customers GROUP BY phone HAVING COUNT(*) > 1;
-- If no duplicates exist, you may run:
-- ALTER TABLE customers ADD UNIQUE KEY uq_customers_phone (phone);
