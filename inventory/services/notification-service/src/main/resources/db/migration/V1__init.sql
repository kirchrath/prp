create table notification_settings
(
    id                 uuid primary key,
    version            bigint      not null,
    created_date       timestamptz not null,
    last_modified_date timestamptz not null,
    deleted            boolean     not null default false,
    enabled            boolean     not null,
    send_time          time        not null,
    default_days       int         not null
);

create table category_threshold
(
    settings_id uuid not null references notification_settings (id) on delete cascade,
    category_id uuid not null,
    days        int  not null,
    primary key (settings_id, category_id)
);

-- the only settings instance (NotificationSettings.ID): daily at 08:00, "expiring soon" = 3 days
insert into notification_settings (id, version, created_date, last_modified_date, deleted, enabled, send_time,
                                   default_days)
values ('00000000-0000-0000-0000-000000000001', 0, now(), now(), false, true, '08:00', 3);

create table digest
(
    id                 uuid primary key,
    version            bigint      not null,
    created_date       timestamptz not null,
    last_modified_date timestamptz not null,
    deleted            boolean     not null default false,
    day                date        not null unique,
    status             varchar(20) not null,
    attempts           int         not null,
    expired            int         not null,
    due_today          int         not null,
    due_soon           int         not null,
    error              varchar(2000)
);

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
    category_id        uuid
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
    best_before        date,
    quantity           int         not null
);

create index idx_batch_copy_due on batch_copy (best_before) where not deleted and quantity > 0;
