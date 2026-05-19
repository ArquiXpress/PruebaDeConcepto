alter table product add column if not exists moderation_reason varchar(600);
alter table product add column if not exists moderation_by uuid;
alter table product add column if not exists moderation_at timestamp with time zone;
alter table product add column if not exists appeal_note varchar(800);
alter table product add column if not exists appeal_requested_at timestamp with time zone;
alter table product add column if not exists appeal_resolution_note varchar(800);
alter table product add column if not exists appeal_resolved_at timestamp with time zone;

update product
   set status = 'INACTIVE'
 where stock_available <= 0
   and status = 'ACTIVE';
