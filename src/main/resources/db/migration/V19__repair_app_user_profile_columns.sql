alter table app_user add column if not exists avatar_url varchar(500);
alter table app_user add column if not exists phone varchar(40);
alter table app_user add column if not exists address varchar(240);
alter table app_user add column if not exists city varchar(120);
alter table app_user add column if not exists document_number varchar(80);
alter table app_user add column if not exists reset_token varchar(120);
alter table app_user add column if not exists reset_token_expires_at timestamp with time zone;

create index if not exists idx_app_user_reset_token on app_user(reset_token);
