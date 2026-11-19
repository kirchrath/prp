create table category
(
    id                 uuid primary key,
    version            bigint       not null,
    created_date       timestamptz  not null,
    last_modified_date timestamptz  not null,
    deleted            boolean      not null default false,
    name               varchar(100) not null
);

create table article
(
    id                 uuid primary key,
    version            bigint       not null,
    created_date       timestamptz  not null,
    last_modified_date timestamptz  not null,
    deleted            boolean      not null default false,
    name               varchar(200) not null,
    brand              varchar(200),
    package_size       varchar(100),
    image_url          varchar(1000),
    category_id        uuid references category (id),
    min_stock          integer      not null default 0 check (min_stock >= 0),
    status             varchar(20)  not null,
    source             varchar(30)  not null,
    merged_into        uuid references article (id)
);

create index idx_article_deleted_status on article (deleted, status);

-- part of the article aggregate
create table article_barcode
(
    article_id uuid        not null references article (id) on delete cascade,
    ean        varchar(64) not null,
    primary key (article_id, ean)
);

create index idx_article_barcode_ean on article_barcode (ean);
