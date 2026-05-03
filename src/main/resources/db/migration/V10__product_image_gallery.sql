alter table product add column if not exists image_urls text;

update product
   set image_urls = json_build_array(image_url)::text
 where image_urls is null
   and image_url is not null;
