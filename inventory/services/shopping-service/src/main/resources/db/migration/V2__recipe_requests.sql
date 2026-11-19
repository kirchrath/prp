-- what an entry is for, e.g. "für Lasagne (4 Pers.)"
alter table shopping_item add column note varchar(500);

-- recipe shopping requests (topic recipe_shopping_request) already added to the list
create table processed_request
(
    id                 uuid primary key,
    version            bigint      not null,
    created_date       timestamptz not null,
    last_modified_date timestamptz not null,
    deleted            boolean     not null default false,
    request_id         uuid        not null unique
);
