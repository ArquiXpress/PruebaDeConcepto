create table if not exists seller_application (
    id uuid primary key,
    user_id uuid not null references app_user(id),
    seller_type varchar(20) not null,
    legal_document_type varchar(20) not null,
    legal_document_number varchar(80) not null,
    document_file_name varchar(240),
    company_name varchar(180),
    company_description varchar(800),
    contact_phone varchar(40),
    category varchar(80) not null,
    products_json text not null,
    status varchar(30) not null,
    created_at timestamp with time zone not null
);

create index if not exists idx_seller_application_user on seller_application(user_id, created_at desc);
