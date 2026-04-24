insert into app_user (id, email, password, display_name, roles) values
('00000000-0000-0000-0000-000000000001', 'cliente@arquixpress.com', 'cliente123', 'Ana Cliente', 'CLIENT'),
('00000000-0000-0000-0000-000000000002', 'vendedor@arquixpress.com', 'vendedor123', 'Luis Vendedor', 'SELLER'),
('00000000-0000-0000-0000-000000000003', 'admin@arquixpress.com', 'admin123', 'Marta Admin', 'ADMIN'),
('00000000-0000-0000-0000-000000000004', 'logistica@arquixpress.com', 'logistica123', 'Laura Logistica', 'LOGISTICS'),
('00000000-0000-0000-0000-000000000005', 'superadmin@arquixpress.com', 'superadmin123', 'Sofia Superadmin', 'SUPERADMIN')
on conflict (email) do nothing;

with categories as (
    select * from unnest(
        array['tecnologia', 'hogar', 'gaming', 'moda', 'deportes', 'telefonia', 'oficina', 'cocina', 'belleza', 'auto'],
        array['https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=900&q=80',
              'https://images.unsplash.com/photo-1593784991095-a205069470b6?auto=format&fit=crop&w=900&q=80',
              'https://images.unsplash.com/photo-1607853202273-797f1c22a38e?auto=format&fit=crop&w=900&q=80',
              'https://images.unsplash.com/photo-1523398002811-999ca8dec234?auto=format&fit=crop&w=900&q=80',
              'https://images.unsplash.com/photo-1502744688674-c619d1586c9a?auto=format&fit=crop&w=900&q=80',
              'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=900&q=80',
              'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=900&q=80',
              'https://images.unsplash.com/photo-1517668808822-9ebb02f2a0e6?auto=format&fit=crop&w=900&q=80',
              'https://images.unsplash.com/photo-1487412912498-0447578fcca8?auto=format&fit=crop&w=900&q=80',
              'https://images.unsplash.com/photo-1553440569-bcc63803a83d?auto=format&fit=crop&w=900&q=80']
    ) as t(category, image_url)
),
numbers as (
    select generate_series(1, 100) as n
),
products as (
    select
        n,
        categories.category,
        categories.image_url,
        case categories.category
            when 'tecnologia' then array['Laptop Ultra','Monitor Pro','Tablet X','Auriculares ANC','Smartwatch','Cámara Mirrorless','Teclado Mecánico','Mouse Gamer','Router WiFi 6','Dock USB-C']
            when 'hogar' then array['Sofá Modular','Lámpara LED','Aspiradora Robot','Freidora de Aire','Cafetera Express','Silla Ergonómica','Mesa Auxiliar','Alfombra Premium','Smart TV 4K','Purificador de Aire']
            when 'gaming' then array['PlayStation 5','Xbox Series X','Nintendo Switch OLED','Control Elite','Volante Racing','Headset Gaming','Silla Gamer','SSD NVMe','Monitor 144Hz','Mouse RGB']
            when 'moda' then array['Chaqueta Urbana','Zapatillas Running','Reloj Minimal','Camiseta Premium','Bolso Crossbody','Gafas de Sol','Sudadera Oversize','Pantalón Cargo','Vestido Casual','Tenis Urbanos']
            when 'deportes' then array['Bicicleta Urbana','Mancuernas Ajustables','Tapete Yoga','Balón Profesional','Rodilleras Pro','Banda Elástica','Bicicleta Spinning','Raqueta Carbono','Casco Deportivo','Guantes Training']
            when 'telefonia' then array['Smartphone Pro','Smartphone Lite','Power Bank','Cargador GaN','Audífonos BT','Funda MagSafe','Apple Watch','Smartband','Cable USB-C','Soporte Auto']
            when 'oficina' then array['Escritorio Sit-Stand','Silla Ejecutiva','Lámpara Escritorio','Impresora Laser','Webcam 4K','Micrófono USB','Organizador Desk','Monitor 27','Laptop Dock','Portátil Office']
            when 'cocina' then array['Olla Inteligente','Batidora Pro','Horno Air Fry','Cafetera Moka','Juego Cuchillos','Extractor Jugo','Sartenes Set','Tostadora','Refrigerador Mini','Báscula Digital']
            when 'belleza' then array['Secador Iónico','Plancha Cabello','Kit Skincare','Perfume Signature','Rizador Pro','Afeitadora','Cepillo Facial','Kit Manicure','Mascarilla Spa','Serum Vitamina C']
            else array['Mantenimiento Auto','Cargador Batería','Aspiradora Auto','Inflador Portátil','Soporte Celular','Escáner OBD','Kit Emergencia','Aceite Sintético','Luz LED Auto','Compresor Mini']
        end as title_pool
    from numbers
    join categories on true
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
from products
on conflict (id) do nothing;
