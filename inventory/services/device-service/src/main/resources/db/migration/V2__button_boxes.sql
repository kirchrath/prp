-- Button boxes: devices of kind BUTTONS, every button books one unit of a fixed article.
alter table scanner add column kind varchar(20) not null default 'SCANNER';

create table scanner_button
(
    scanner_id uuid not null references scanner (id) on delete cascade,
    button     int  not null,
    article_id uuid not null,
    primary key (scanner_id, button)
);

alter table scan alter column ean drop not null;
alter table scan add column button int;
alter table scan add column article_id uuid;
