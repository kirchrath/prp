create table scanner
(
    id                 uuid primary key,
    version            bigint      not null,
    created_date       timestamptz not null,
    last_modified_date timestamptz not null,
    deleted            boolean     not null default false,
    device_id          varchar(64) not null unique,
    name               varchar(100),
    location_id        uuid,
    online             boolean     not null default false,
    firmware_version   varchar(50),
    ip_address         varchar(64),
    last_seen          timestamptz,
    last_boot_id       varchar(64),
    last_seq           bigint
);

create table scan
(
    id                 uuid primary key,
    version            bigint       not null,
    created_date       timestamptz  not null,
    last_modified_date timestamptz  not null,
    deleted            boolean      not null default false,
    scanner_id         uuid         not null references scanner (id),
    device_id          varchar(64)  not null,
    ean                varchar(64)  not null,
    direction          varchar(10)  not null,
    location_id        uuid,
    seq                bigint       not null,
    boot_id            varchar(64)  not null,
    status             varchar(20)  not null,
    result             varchar(30),
    article_name       varchar(200),
    quantity           bigint,
    message            varchar(500),
    booking_id         uuid
);
