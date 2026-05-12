create table if not exists logistics_center (
    id uuid primary key,
    city varchar(120) not null unique,
    display_name varchar(180) not null
);

create table if not exists logistics_operator (
    id uuid primary key,
    app_user_id uuid not null unique references app_user(id),
    center_id uuid not null references logistics_center(id)
);

create index if not exists idx_logistics_operator_center on logistics_operator(center_id);

alter table app_user add column if not exists city varchar(120);
alter table marketplace_order add column if not exists logistics_center_id uuid references logistics_center(id);
alter table marketplace_order add column if not exists logistics_operator_id uuid references logistics_operator(id);

create index if not exists idx_order_logistics_center on marketplace_order(logistics_center_id);
create index if not exists idx_order_logistics_operator on marketplace_order(logistics_operator_id);

insert into logistics_center (id, city, display_name) values
    ('20000000-0000-0000-0000-000000000001', 'Bogota',       'Centro logístico Bogotá'),
    ('20000000-0000-0000-0000-000000000002', 'Medellin',     'Centro logístico Medellín'),
    ('20000000-0000-0000-0000-000000000003', 'Cali',         'Centro logístico Cali')
on conflict (city) do nothing;

insert into app_user (id, email, password, display_name, roles, city)
select '30000000-0000-0000-0000-000000000001', 'operador.bogota.1@arquixpress.com', 'logistica123', 'Operador Bogotá 1', 'LOGISTICS', 'Bogota'
where not exists (select 1 from app_user where email = 'operador.bogota.1@arquixpress.com');

insert into app_user (id, email, password, display_name, roles, city)
select '30000000-0000-0000-0000-000000000002', 'operador.bogota.2@arquixpress.com', 'logistica123', 'Operador Bogotá 2', 'LOGISTICS', 'Bogota'
where not exists (select 1 from app_user where email = 'operador.bogota.2@arquixpress.com');

insert into app_user (id, email, password, display_name, roles, city)
select '30000000-0000-0000-0000-000000000003', 'operador.medellin.1@arquixpress.com', 'logistica123', 'Operador Medellín 1', 'LOGISTICS', 'Medellin'
where not exists (select 1 from app_user where email = 'operador.medellin.1@arquixpress.com');

insert into logistics_operator (id, app_user_id, center_id)
select '40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001'
where not exists (select 1 from logistics_operator where id = '40000000-0000-0000-0000-000000000001');

insert into logistics_operator (id, app_user_id, center_id)
select '40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001'
where not exists (select 1 from logistics_operator where id = '40000000-0000-0000-0000-000000000002');

insert into logistics_operator (id, app_user_id, center_id)
select '40000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000002'
where not exists (select 1 from logistics_operator where id = '40000000-0000-0000-0000-000000000003');

update marketplace_order
   set logistics_center_id = (
       array['20000000-0000-0000-0000-000000000001',
             '20000000-0000-0000-0000-000000000002',
             '20000000-0000-0000-0000-000000000003']::uuid[]
       )[1 + (abs(hashtext(id::text)) % 3)]
 where logistics_center_id is null;
