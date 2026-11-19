-- Barcodes are stored normalized (see Gtin): UPC-A (12 digits) and GTIN-14 with a leading 0 become EAN-13.
update barcode_copy set ean = '0' || ean where ean ~ '^[0-9]{12}$';
update barcode_copy set ean = substring(ean from 2) where ean ~ '^0[0-9]{13}$';
