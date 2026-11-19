-- Firmware images (uploaded in the web app) and over-the-air updates of the devices.
create table firmware
(
    id                 uuid primary key,
    version            bigint       not null,
    created_date       timestamptz  not null,
    last_modified_date timestamptz  not null,
    deleted            boolean      not null default false,
    kind               varchar(20)  not null,
    firmware_version   varchar(32)  not null,
    size               bigint       not null,
    sha256             varchar(64)  not null,
    file_name          varchar(200) not null
);

create table firmware_content
(
    id                 uuid primary key,
    version            bigint      not null,
    created_date       timestamptz not null,
    last_modified_date timestamptz not null,
    deleted            boolean     not null default false,
    firmware_id        uuid        not null unique references firmware (id),
    content            bytea       not null
);

create table ota_job
(
    id                 uuid primary key,
    version            bigint       not null,
    created_date       timestamptz  not null,
    last_modified_date timestamptz  not null,
    deleted            boolean      not null default false,
    scanner_id         uuid         not null references scanner (id),
    device_id          varchar(64)  not null,
    firmware_id        uuid         not null references firmware (id),
    target_version     varchar(32)  not null,
    token              varchar(64)  not null unique,
    expires_at         timestamptz  not null,
    state              varchar(20)  not null,
    error              varchar(500)
);

create index idx_ota_job_scanner on ota_job (scanner_id, created_date desc);
