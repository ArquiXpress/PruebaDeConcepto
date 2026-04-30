insert into logistics_center (id, city, display_name) values
    ('20000000-0000-0000-0000-000000000004', 'Barranquilla', 'Centro logístico Barranquilla'),
    ('20000000-0000-0000-0000-000000000005', 'Cartagena',    'Centro logístico Cartagena')
on conflict (city) do nothing;

insert into app_user (id, email, password, display_name, roles, city)
select '30000000-0000-0000-0000-000000000004', 'operador.cali.1@arquixpress.com', 'logistica123', 'Operador Cali 1', 'LOGISTICS', 'Cali'
where not exists (select 1 from app_user where email = 'operador.cali.1@arquixpress.com');

insert into app_user (id, email, password, display_name, roles, city)
select '30000000-0000-0000-0000-000000000005', 'operador.barranquilla.1@arquixpress.com', 'logistica123', 'Operador Barranquilla 1', 'LOGISTICS', 'Barranquilla'
where not exists (select 1 from app_user where email = 'operador.barranquilla.1@arquixpress.com');

insert into app_user (id, email, password, display_name, roles, city)
select '30000000-0000-0000-0000-000000000006', 'operador.cartagena.1@arquixpress.com', 'logistica123', 'Operador Cartagena 1', 'LOGISTICS', 'Cartagena'
where not exists (select 1 from app_user where email = 'operador.cartagena.1@arquixpress.com');

insert into logistics_operator (id, app_user_id, center_id)
select '40000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000003'
where not exists (select 1 from logistics_operator where id = '40000000-0000-0000-0000-000000000004');

insert into logistics_operator (id, app_user_id, center_id)
select '40000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000004'
where not exists (select 1 from logistics_operator where id = '40000000-0000-0000-0000-000000000005');

insert into logistics_operator (id, app_user_id, center_id)
select '40000000-0000-0000-0000-000000000006', '30000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000005'
where not exists (select 1 from logistics_operator where id = '40000000-0000-0000-0000-000000000006');

update marketplace_order
   set logistics_center_id = (
       array['20000000-0000-0000-0000-000000000001',
             '20000000-0000-0000-0000-000000000002',
             '20000000-0000-0000-0000-000000000003',
             '20000000-0000-0000-0000-000000000004',
             '20000000-0000-0000-0000-000000000005']::uuid[]
       )[1 + (abs(hashtext(id::text)) % 5)]
 where logistics_center_id is not null;
