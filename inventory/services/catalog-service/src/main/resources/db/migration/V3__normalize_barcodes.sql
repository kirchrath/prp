-- Barcodes are stored normalized (see Gtin): UPC-A (12 digits) and GTIN-14 with a leading 0 become EAN-13.
-- If an article already has the normalized code, the other spelling is dropped.
delete from article_barcode b
where b.ean ~ '^[0-9]{12}$'
  and exists (select 1 from article_barcode o where o.article_id = b.article_id and o.ean = '0' || b.ean);
update article_barcode set ean = '0' || ean where ean ~ '^[0-9]{12}$';
delete from article_barcode b
where b.ean ~ '^0[0-9]{13}$'
  and exists (select 1 from article_barcode o where o.article_id = b.article_id and o.ean = substring(b.ean from 2));
update article_barcode set ean = substring(ean from 2) where ean ~ '^0[0-9]{13}$';
