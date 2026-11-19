-- Bookings of button boxes have a fixed article and no barcode.
alter table booking alter column ean drop not null;
