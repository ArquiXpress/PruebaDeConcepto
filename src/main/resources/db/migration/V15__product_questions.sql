create table if not exists product_question (
    id uuid primary key,
    product_id uuid not null references product(id),
    buyer_id uuid not null references app_user(id),
    seller_id uuid not null references app_user(id),
    question varchar(700) not null,
    answer varchar(1200),
    answered_by uuid references app_user(id),
    answered_at timestamp with time zone,
    created_at timestamp with time zone not null
);

create index if not exists idx_product_question_product_created
    on product_question(product_id, created_at desc);

create index if not exists idx_product_question_seller_created
    on product_question(seller_id, created_at desc);

create index if not exists idx_product_question_buyer_created
    on product_question(buyer_id, created_at desc);
