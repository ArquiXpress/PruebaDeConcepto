alter table seller_application add column if not exists document_file_content text;
alter table seller_application add column if not exists document_file_mime_type varchar(120);
