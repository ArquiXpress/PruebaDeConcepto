create table if not exists app_notification (
    id uuid primary key,
    user_id uuid not null references app_user(id),
    type varchar(80) not null,
    title varchar(180) not null,
    body varchar(800) not null,
    action_url varchar(300),
    read_at timestamp with time zone,
    created_at timestamp with time zone not null
);

create index if not exists idx_app_notification_user_created
    on app_notification(user_id, created_at desc);

create index if not exists idx_app_notification_user_unread
    on app_notification(user_id, read_at)
    where read_at is null;
