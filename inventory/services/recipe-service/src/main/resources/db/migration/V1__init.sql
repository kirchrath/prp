create table recipe_category
(
    id                 uuid primary key,
    version            bigint       not null,
    created_date       timestamptz  not null,
    last_modified_date timestamptz  not null,
    deleted            boolean      not null default false,
    name               varchar(100) not null
);

create table recipe
(
    id                 uuid primary key,
    version            bigint        not null,
    created_date       timestamptz   not null,
    last_modified_date timestamptz   not null,
    deleted            boolean       not null default false,
    title              varchar(200)  not null,
    description        varchar(2000),
    servings           integer       not null check (servings > 0),
    prep_minutes       integer,
    cook_minutes       integer,
    image_url          varchar(1000),
    source_url         varchar(1000),
    category_id        uuid references recipe_category (id)
);

-- children of the recipe aggregate
create table recipe_tag
(
    recipe_id uuid        not null references recipe (id) on delete cascade,
    name      varchar(50) not null,
    primary key (recipe_id, name)
);

create table recipe_step
(
    recipe_id uuid          not null references recipe (id) on delete cascade,
    position  integer       not null,
    text      varchar(4000) not null,
    primary key (recipe_id, position)
);

create table recipe_ingredient
(
    recipe_id  uuid           not null references recipe (id) on delete cascade,
    position   integer        not null,
    name       varchar(200)   not null,
    amount     numeric(12, 3),
    unit       varchar(20),
    article_id uuid,
    basic      boolean        not null default false,
    primary key (recipe_id, position)
);

create table shopping_request
(
    id                 uuid primary key,
    version            bigint       not null,
    created_date       timestamptz  not null,
    last_modified_date timestamptz  not null,
    deleted            boolean      not null default false,
    recipe_id          uuid         not null references recipe (id),
    recipe_title       varchar(200) not null,
    servings           integer      not null
);

create table shopping_request_item
(
    request_id uuid         not null references shopping_request (id) on delete cascade,
    position   integer      not null,
    article_id uuid,
    text       varchar(300) not null,
    quantity   integer      not null,
    primary key (request_id, position)
);

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
    content_amount     numeric(14, 3),
    content_unit       varchar(20)
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
