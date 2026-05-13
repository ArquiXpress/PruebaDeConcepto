with ordered_products as (
    select
        id,
        row_number() over (order by created_at desc, id) as rn,
        lower(regexp_replace(category || ',' || title, '[^a-zA-Z0-9,]+', '-', 'g')) as image_query
    from product
    where status = 'ACTIVE'
)
update product p
   set image_url = 'https://loremflickr.com/900/700/' || op.image_query || '?lock=' || (20000 + op.rn),
       image_urls = json_build_array(
           'https://loremflickr.com/900/700/' || op.image_query || '?lock=' || (20000 + op.rn),
           'https://loremflickr.com/900/700/' || op.image_query || ',detail?lock=' || (30000 + op.rn),
           'https://loremflickr.com/900/700/' || op.image_query || ',marketplace?lock=' || (40000 + op.rn)
       )::text
  from ordered_products op
 where p.id = op.id;
