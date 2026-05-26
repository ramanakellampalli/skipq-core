-- General audience expansion
-- a) vendors.campus_id becomes nullable (general vendors have no campus)
ALTER TABLE vendors ALTER COLUMN campus_id DROP NOT NULL;

-- b) vendor city — location label shown on customer app for general vendors
ALTER TABLE vendors ADD COLUMN city VARCHAR(100);

-- c) vendor business contact phone — shown to customers on vendor menu screen
ALTER TABLE vendors ADD COLUMN phone VARCHAR(20);

-- d) user personal phone — required at customer registration; set by admin for vendor owners
--    shown to vendor on incoming order detail
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
