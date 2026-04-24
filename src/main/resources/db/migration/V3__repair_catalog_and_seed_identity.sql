alter table product add column if not exists image_url varchar(400);

update product
   set image_url = coalesce(image_url, 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=900&q=80')
 where image_url is null;

alter table product alter column image_url set not null;

create table if not exists app_user (
    id uuid primary key,
    email varchar(180) not null unique,
    password varchar(120) not null,
    display_name varchar(120) not null,
    roles varchar(240) not null
);

create index if not exists idx_app_user_email on app_user(email);

insert into app_user (id, email, password, display_name, roles)
select '00000000-0000-0000-0000-000000000001', 'cliente@arquixpress.com', 'cliente123', 'Ana Cliente', 'CLIENT'
where not exists (select 1 from app_user where email = 'cliente@arquixpress.com');

insert into app_user (id, email, password, display_name, roles)
select '00000000-0000-0000-0000-000000000002', 'vendedor@arquixpress.com', 'vendedor123', 'Luis Vendedor', 'SELLER'
where not exists (select 1 from app_user where email = 'vendedor@arquixpress.com');

insert into app_user (id, email, password, display_name, roles)
select '00000000-0000-0000-0000-000000000003', 'admin@arquixpress.com', 'admin123', 'Marta Admin', 'ADMIN'
where not exists (select 1 from app_user where email = 'admin@arquixpress.com');

insert into app_user (id, email, password, display_name, roles)
select '00000000-0000-0000-0000-000000000004', 'logistica@arquixpress.com', 'logistica123', 'Laura Logistica', 'LOGISTICS'
where not exists (select 1 from app_user where email = 'logistica@arquixpress.com');

insert into app_user (id, email, password, display_name, roles)
select '00000000-0000-0000-0000-000000000005', 'superadmin@arquixpress.com', 'superadmin123', 'Sofia Superadmin', 'SUPERADMIN'
where not exists (select 1 from app_user where email = 'superadmin@arquixpress.com');
