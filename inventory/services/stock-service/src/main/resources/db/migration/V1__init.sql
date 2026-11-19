create table location
(
    id                 uuid primary key,
    version            bigint       not null,
    created_date       timestamptz  not null,
    last_modified_date timestamptz  not null,
    deleted            boolean      not null default false,
    name               varchar(100) not null,
    description        varchar(500),
    parent_id          uuid references location (id)
);

create table stock_batch
(
    id                 uuid primary key,
    version            bigint      not null,
    created_date       timestamptz not null,
    last_modified_date timestamptz not null,
    deleted            boolean     not null default false,
    article_id         uuid        not null,
    location_id        uuid        not null references location (id),
    best_before        date,
    quantity           integer     not null check (quantity >= 0)
);

create index idx_batch_article_location on stock_batch (article_id, location_id) where not deleted;

create table movement
(
    id                 uuid primary key,
    version            bigint      not null,
    created_date       timestamptz not null,
    last_modified_date timestamptz not null,
    deleted            boolean     not null default false,
    article_id         uuid        not null,
    location_id        uuid        not null references location (id),
    best_before        date,
    delta              integer     not null,
    type               varchar(20) not null,
    source             varchar(20) not null,
    scanner_id         uuid,
    username           varchar(100),
    booking_id         uuid
);

create index idx_movement_created on movement (created_date desc);

create table booking
(
    id                   uuid primary key,
    version              bigint       not null,
    created_date         timestamptz  not null,
    last_modified_date   timestamptz  not null,
    deleted              boolean      not null default false,
    ean                  varchar(64)  not null,
    location_id          uuid         not null,
    direction            varchar(10)  not null,
    source               varchar(20)  not null,
    scan_id              uuid unique,
    scanner_id           uuid,
    username             varchar(100),
    status               varchar(30)  not null,
    article_id           uuid,
    article_name         varchar(200),
    quantity_at_location bigint,
    message              varchar(500) not null
);

create index idx_booking_pending on booking (ean) where status = 'PENDING_ARTICLE';

-- local copies of catalog articles (topic catalog_article)
create table article_copy
(
    id                 uuid primary key,
    version            bigint       not null,
    created_date       timestamptz  not null,
    last_modified_date timestamptz  not null,
    deleted            boolean      not null default false,
    article_id         uuid         not null unique,
    source_version     bigint       not null,
    name               varchar(200) not null,
    brand              varchar(200),
    image_url          varchar(1000),
    status             varchar(20)  not null
);

create table barcode_copy
(
    id                 uuid primary key,
    version            bigint      not null,
    created_date       timestamptz not null,
    last_modified_date timestamptz not null,
    deleted            boolean     not null default false,
    ean                varchar(64) not null,
    article_id         uuid        not null
);

create index idx_barcode_copy_ean on barcode_copy (ean) where not deleted;
