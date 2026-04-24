create table product (
    id uuid primary key,
    seller_id uuid not null,
    title varchar(180) not null,
    description varchar(600) not null,
    category varchar(80) not null,
    image_url varchar(400) not null,
    price numeric(12,2) not null,
    stock_available integer not null check (stock_available >= 0),
    status varchar(20) not null,
    version bigint not null default 0,
    created_at timestamp with time zone not null
);

create index idx_product_status_category on product(status, category);
create index idx_product_title on product(title);

create table marketplace_order (
    id uuid primary key,
    buyer_id uuid not null,
    status varchar(40) not null,
    shipment_status varchar(40) not null,
    total numeric(12,2) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    version bigint not null default 0
);

create index idx_order_buyer_created on marketplace_order(buyer_id, created_at desc);
create index idx_order_status on marketplace_order(status);

create table order_line (
    id uuid primary key,
    order_id uuid not null references marketplace_order(id),
    product_id uuid not null references product(id),
    quantity integer not null check (quantity > 0),
    unit_price numeric(12,2) not null
);

create index idx_order_line_order on order_line(order_id);
create index idx_order_line_product on order_line(product_id);

create table payment_transaction (
    id uuid primary key,
    order_id uuid not null references marketplace_order(id),
    idempotency_key varchar(160) not null unique,
    amount numeric(12,2) not null,
    status varchar(20) not null,
    external_reference varchar(180),
    created_at timestamp with time zone not null,
    processed_at timestamp with time zone
);

create index idx_payment_order on payment_transaction(order_id);
create index idx_payment_status on payment_transaction(status);

create table notification_outbox (
    id uuid primary key,
    aggregate_type varchar(80) not null,
    aggregate_id uuid not null,
    event_type varchar(80) not null,
    payload text not null,
    status varchar(20) not null,
    attempts integer not null,
    created_at timestamp with time zone not null,
    last_attempt_at timestamp with time zone
);

create index idx_outbox_status_created on notification_outbox(status, created_at);

create table app_user (
    id uuid primary key,
    email varchar(180) not null unique,
    password varchar(120) not null,
    display_name varchar(120) not null,
    roles varchar(240) not null
);

create index idx_app_user_email on app_user(email);
