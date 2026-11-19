create table shopping_item
(
    id                 uuid primary key,
    version            bigint      not null,
    created_date       timestamptz not null,
    last_modified_date timestamptz not null,
    deleted            boolean     not null default false,
    article_id         uuid,
    text               varchar(200),
    quantity           integer     not null check (quantity > 0),
    automatic          boolean     not null default false,
    done               boolean     not null default false,
    constraint chk_item_content check (article_id is not null or text is not null)
);

create index idx_item_auto_article on shopping_item (article_id) where automatic and not deleted;

-- local copies (topics catalog_article and stock_batch)
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
    min_stock          integer      not null
);

create table batch_copy
(
    id                 uuid primary key,
    version            bigint      not null,
    created_date       timestamptz not null,
    last_modified_date timestamptz not null,
    deleted            boolean     not null default false,
    batch_id           uuid        not null unique,
    source_version     bigint      not null,
    article_id         uuid        not null,
    quantity           integer     not null
);

create index idx_batch_copy_article on batch_copy (article_id) where not deleted;
