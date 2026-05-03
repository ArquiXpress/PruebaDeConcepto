alter table seller_application add column if not exists reviewed_by uuid;
alter table seller_application add column if not exists reviewed_at timestamp with time zone;
alter table seller_application add column if not exists review_note varchar(600);
alter table seller_application add column if not exists approved_product_count integer not null default 0;

create index if not exists idx_seller_application_status on seller_application(status, created_at desc);
