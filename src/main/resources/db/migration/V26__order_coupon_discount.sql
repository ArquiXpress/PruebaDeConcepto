alter table marketplace_order
    add column if not exists coupon_code varchar(80),
    add column if not exists discount_total numeric(12,2) not null default 0;
