create table if not exists marketing_coupon (
    id uuid primary key,
    code varchar(80) not null,
    title varchar(180) not null,
    description text not null,
    discount_percent integer not null,
    target_type varchar(40) not null,
    target_value varchar(180),
    created_by uuid not null,
    created_at timestamp not null
);

create unique index if not exists idx_marketing_coupon_code on marketing_coupon(upper(code));

create table if not exists coupon_redemption (
    id uuid primary key,
    coupon_id uuid not null references marketing_coupon(id),
    buyer_id uuid not null,
    order_id uuid not null,
    discount_amount numeric(12,2) not null,
    redeemed_at timestamp not null,
    unique (coupon_id, buyer_id)
);

create table if not exists seller_offer_request (
    id uuid primary key,
    seller_id uuid not null,
    title varchar(180) not null,
    message text not null,
    discount_percent integer not null,
    status varchar(40) not null,
    created_by uuid not null,
    starts_at timestamp not null,
    ends_at timestamp not null,
    decided_at timestamp,
    created_at timestamp not null
);

create table if not exists seller_offer_product (
    offer_request_id uuid not null references seller_offer_request(id) on delete cascade,
    product_id uuid not null,
    primary key (offer_request_id, product_id)
);

create index if not exists idx_seller_offer_request_seller on seller_offer_request(seller_id, created_at desc);
create index if not exists idx_seller_offer_request_active on seller_offer_request(status, starts_at, ends_at);

with seeded_offers as (
    insert into seller_offer_request (
        id, seller_id, title, message, discount_percent, status, created_by, starts_at, ends_at, decided_at, created_at
    )
    values
        ('91000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000021', 'Semana tech autorizada', 'Campaña demo aprobada por el vendedor para productos de tecnologia.', 20, 'ACCEPTED', '00000000-0000-0000-0000-000000000003', now() - interval '2 days', now() + interval '14 days', now() - interval '1 day', now() - interval '3 days'),
        ('91000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000023', 'Gaming days', 'Campaña demo aprobada por el vendedor para accesorios gamer.', 18, 'ACCEPTED', '00000000-0000-0000-0000-000000000003', now() - interval '1 day', now() + interval '10 days', now() - interval '18 hours', now() - interval '2 days'),
        ('91000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000027', 'Belleza con descuento', 'Campaña demo aprobada por el vendedor para productos seleccionados.', 15, 'ACCEPTED', '00000000-0000-0000-0000-000000000003', now() - interval '3 days', now() + interval '9 days', now() - interval '2 days', now() - interval '4 days'),
        ('91000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000028', 'Auto weekend', 'Campaña demo aprobada por el vendedor para accesorios de auto.', 12, 'ACCEPTED', '00000000-0000-0000-0000-000000000003', now() - interval '1 day', now() + interval '7 days', now() - interval '12 hours', now() - interval '2 days')
    on conflict (id) do nothing
    returning id
)
insert into seller_offer_product (offer_request_id, product_id)
select offer_id, product_id
from (
    select '91000000-0000-0000-0000-000000000001'::uuid as offer_id, id as product_id from product where category = 'tecnologia' order by created_at desc limit 8
    union all
    select '91000000-0000-0000-0000-000000000002'::uuid, id from product where category = 'gaming' order by created_at desc limit 6
    union all
    select '91000000-0000-0000-0000-000000000003'::uuid, id from product where category = 'belleza' order by created_at desc limit 6
    union all
    select '91000000-0000-0000-0000-000000000004'::uuid, id from product where category = 'auto' order by created_at desc limit 6
) seeded_products
on conflict do nothing;
