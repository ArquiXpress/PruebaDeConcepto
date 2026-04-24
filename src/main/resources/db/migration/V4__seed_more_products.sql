with numbers as (
    select generate_series(1, 100) as n
),
picked as (
    select
        n,
        case ((n - 1) % 10)
            when 0 then 'tecnologia'
            when 1 then 'hogar'
            when 2 then 'gaming'
            when 3 then 'moda'
            when 4 then 'deportes'
            when 5 then 'telefonia'
            when 6 then 'oficina'
            when 7 then 'cocina'
            when 8 then 'belleza'
            else 'auto'
        end as category,
        case ((n - 1) % 10)
            when 0 then 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=900&q=80'
            when 1 then 'https://images.unsplash.com/photo-1593784991095-a205069470b6?auto=format&fit=crop&w=900&q=80'
            when 2 then 'https://images.unsplash.com/photo-1607853202273-797f1c22a38e?auto=format&fit=crop&w=900&q=80'
            when 3 then 'https://images.unsplash.com/photo-1523398002811-999ca8dec234?auto=format&fit=crop&w=900&q=80'
            when 4 then 'https://images.unsplash.com/photo-1502744688674-c619d1586c9a?auto=format&fit=crop&w=900&q=80'
            when 5 then 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=900&q=80'
            when 6 then 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=900&q=80'
            when 7 then 'https://images.unsplash.com/photo-1517668808822-9ebb02f2a0e6?auto=format&fit=crop&w=900&q=80'
            when 8 then 'https://images.unsplash.com/photo-1487412912498-0447578fcca8?auto=format&fit=crop&w=900&q=80'
            else 'https://images.unsplash.com/photo-1553440569-bcc63803a83d?auto=format&fit=crop&w=900&q=80'
        end as image_url,
        case ((n - 1) % 10)
            when 0 then array['Laptop Ultra','Monitor Pro','Tablet X','Auriculares ANC','Smartwatch','Cámara Mirrorless','Teclado Mecánico','Mouse Gamer','Router WiFi 6','Dock USB-C']
            when 1 then array['Sofá Modular','Lámpara LED','Aspiradora Robot','Freidora de Aire','Cafetera Express','Silla Ergonómica','Mesa Auxiliar','Alfombra Premium','Smart TV 4K','Purificador de Aire']
            when 2 then array['PlayStation 5','Xbox Series X','Nintendo Switch OLED','Control Elite','Volante Racing','Headset Gaming','Silla Gamer','SSD NVMe','Monitor 144Hz','Mouse RGB']
            when 3 then array['Chaqueta Urbana','Zapatillas Running','Reloj Minimal','Camiseta Premium','Bolso Crossbody','Gafas de Sol','Sudadera Oversize','Pantalón Cargo','Vestido Casual','Tenis Urbanos']
            when 4 then array['Bicicleta Urbana','Mancuernas Ajustables','Tapete Yoga','Balón Profesional','Rodilleras Pro','Banda Elástica','Bicicleta Spinning','Raqueta Carbono','Casco Deportivo','Guantes Training']
            when 5 then array['Smartphone Pro','Smartphone Lite','Power Bank','Cargador GaN','Audífonos BT','Funda MagSafe','Smartwatch iOS','Smartband','Cable USB-C','Soporte Auto']
            when 6 then array['Escritorio Sit-Stand','Silla Ejecutiva','Lámpara Escritorio','Impresora Laser','Webcam 4K','Micrófono USB','Organizador Desk','Monitor 27','Laptop Dock','Portátil Office']
            when 7 then array['Olla Inteligente','Batidora Pro','Horno Air Fry','Cafetera Moka','Juego Cuchillos','Extractor Jugo','Sartenes Set','Tostadora','Refrigerador Mini','Báscula Digital']
            when 8 then array['Secador Iónico','Plancha Cabello','Kit Skincare','Perfume Signature','Rizador Pro','Afeitadora','Cepillo Facial','Kit Manicure','Mascarilla Spa','Serum Vitamina C']
            else array['Mantenimiento Auto','Cargador Batería','Aspiradora Auto','Inflador Portátil','Soporte Celular','Escáner OBD','Kit Emergencia','Aceite Sintético','Luz LED Auto','Compresor Mini']
        end as title_pool
    from numbers
)
insert into product (id, seller_id, title, description, category, image_url, price, stock_available, status, version, created_at)
select
    ('00000000-0000-0000-0000-' || lpad(to_hex(n), 12, '0'))::uuid,
    '00000000-0000-0000-0000-000000000002',
    title_pool[((n - 1) % 10) + 1] || ' ' || n,
    'Producto de marketplace para la categoría ' || category || ' con stock real, imagen y disponibilidad activa.',
    category,
    image_url,
    (case category
        when 'tecnologia' then 450000 + (n * 17500)
        when 'hogar' then 120000 + (n * 9500)
        when 'gaming' then 250000 + (n * 22000)
        when 'moda' then 80000 + (n * 6500)
        when 'deportes' then 70000 + (n * 8000)
        when 'telefonia' then 150000 + (n * 18000)
        when 'oficina' then 90000 + (n * 9000)
        when 'cocina' then 65000 + (n * 7000)
        when 'belleza' then 60000 + (n * 5500)
        else 110000 + (n * 10000)
    end)::numeric(12,2),
    (5 + (n % 37)),
    'ACTIVE',
    0,
    now() - (n || ' days')::interval
from picked
on conflict (id) do update
set
    title = excluded.title,
    description = excluded.description,
    category = excluded.category,
    image_url = excluded.image_url,
    price = excluded.price,
    stock_available = excluded.stock_available,
    status = excluded.status,
    version = product.version + 1,
    created_at = excluded.created_at;
