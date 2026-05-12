alter table payment_transaction
    add column if not exists payment_method varchar(80);

update payment_transaction
   set payment_method = 'Pago simulado'
 where payment_method is null;

alter table payment_transaction
    alter column payment_method set not null;
