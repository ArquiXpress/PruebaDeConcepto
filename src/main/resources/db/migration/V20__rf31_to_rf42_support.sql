alter table app_user
    add column if not exists status varchar(20) not null default 'ACTIVE';

alter table marketplace_order
    add column if not exists delivery_address_id uuid,
    add column if not exists delivery_address_snapshot varchar(500);

create table if not exists delivery_address (
    id uuid primary key,
    user_id uuid not null references app_user(id),
    label varchar(80) not null,
    recipient varchar(140) not null,
    address_line varchar(240) not null,
    city varchar(120) not null,
    phone varchar(40) not null,
    active boolean not null default true,
    created_at timestamp with time zone not null
);

create index if not exists idx_delivery_address_user_active
    on delivery_address(user_id, active);
