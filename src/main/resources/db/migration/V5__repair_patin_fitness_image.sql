update product
   set title = 'Rodilleras Pro',
       description = 'Producto de marketplace para la categoría deportes con stock real, imagen y disponibilidad activa.',
       image_url = 'https://images.unsplash.com/photo-1518609878373-06d740f60d8b?auto=format&fit=crop&w=900&q=80',
       price = 124000.00,
       stock_available = 18,
       status = 'ACTIVE',
       version = version + 1,
       created_at = now()
 where title ilike '%Patines Fitness%';
