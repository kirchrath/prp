-- content of one package in a base unit (GRAM, MILLILITER, PIECE); used by the recipe service
alter table article add column content_amount numeric(14, 3) check (content_amount > 0);
alter table article add column content_unit varchar(20);
alter table article add constraint chk_article_content check ((content_amount is null) = (content_unit is null));
