create index if not exists idx_payment_created_at_desc
    on payment_transaction(created_at desc);

create index if not exists idx_payment_order_created_at_desc
    on payment_transaction(order_id, created_at desc);
