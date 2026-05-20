alter table marketplace_order add column if not exists shipping_cost numeric(12,2) not null default 0;
alter table marketplace_order add column if not exists shipping_address varchar(240);
alter table marketplace_order add column if not exists shipping_city varchar(120);

update marketplace_order mo
   set shipping_address = coalesce(mo.shipping_address, au.address),
       shipping_city = coalesce(mo.shipping_city, au.city)
  from app_user au
 where au.id = mo.buyer_id;

update app_user
   set address = coalesce(address, 'Sede principal ArquiXpress ' || coalesce(city, 'Bogota')),
       city = coalesce(city, 'Bogota')
 where roles like '%SELLER%';
