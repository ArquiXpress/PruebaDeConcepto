insert into app_user (id, email, password, display_name, roles, city)
values
    ('00000000-0000-0000-0000-000000000021', 'vendedor.tech@arquixpress.com', 'vendedor123', 'Nicolas Tech Store', 'SELLER', 'Bogota'),
    ('00000000-0000-0000-0000-000000000022', 'vendedor.hogar@arquixpress.com', 'vendedor123', 'Valeria Hogar Market', 'SELLER', 'Medellin'),
    ('00000000-0000-0000-0000-000000000023', 'vendedor.gaming@arquixpress.com', 'vendedor123', 'Andres Gaming Hub', 'SELLER', 'Cali'),
    ('00000000-0000-0000-0000-000000000024', 'vendedor.moda@arquixpress.com', 'vendedor123', 'Camila Moda Urbana', 'SELLER', 'Barranquilla'),
    ('00000000-0000-0000-0000-000000000025', 'vendedor.deportes@arquixpress.com', 'vendedor123', 'Mateo Sport Center', 'SELLER', 'Bucaramanga'),
    ('00000000-0000-0000-0000-000000000026', 'vendedor.cocina@arquixpress.com', 'vendedor123', 'Sofia Cocina Plus', 'SELLER', 'Cartagena'),
    ('00000000-0000-0000-0000-000000000027', 'vendedor.belleza@arquixpress.com', 'vendedor123', 'Laura Belleza Pro', 'SELLER', 'Pereira'),
    ('00000000-0000-0000-0000-000000000028', 'vendedor.auto@arquixpress.com', 'vendedor123', 'Diego Auto Parts', 'SELLER', 'Manizales')
on conflict (email) do update
set
    display_name = excluded.display_name,
    roles = excluded.roles,
    city = excluded.city;

with seed_products as (
    select *
    from (
        values
        (1, 'tecnologia', 'Laptop ultraligera Ryzen 7', 'Portatil de aluminio con 16 GB de RAM, SSD NVMe y pantalla antirreflejo para trabajo movil.', 3890000, 'laptop,computer'),
        (2, 'tecnologia', 'Monitor profesional 27 pulgadas 4K', 'Panel IPS calibrado para diseno, productividad y edicion con base ajustable.', 1490000, 'monitor,desk'),
        (3, 'tecnologia', 'Tablet grafica con lapiz activo', 'Pantalla tactil precisa para ilustracion, notas y edicion de contenido.', 980000, 'tablet,stylus'),
        (4, 'tecnologia', 'Audifonos ANC premium', 'Cancelacion activa de ruido, modo ambiente y autonomia extendida para viajes.', 620000, 'headphones,audio'),
        (5, 'tecnologia', 'Camara mirrorless compacta', 'Sensor APS-C, video 4K y conectividad WiFi para creadores de contenido.', 3150000, 'camera,photography'),
        (6, 'tecnologia', 'Teclado mecanico compacto', 'Switches tactiles, retroiluminacion blanca y chasis metalico para escritorio.', 310000, 'keyboard,mechanical'),
        (7, 'tecnologia', 'Mouse ergonomico vertical', 'Sensor preciso y postura natural para jornadas largas de oficina.', 180000, 'mouse,ergonomic'),
        (8, 'tecnologia', 'Router WiFi 6 mesh', 'Cobertura amplia para hogares conectados con baja latencia y gestion por app.', 540000, 'router,wifi'),
        (9, 'tecnologia', 'Dock USB-C multipuerto', 'HDMI, Ethernet, lectores SD y carga rapida para portatiles modernos.', 260000, 'usb,dock'),
        (10, 'tecnologia', 'Disco SSD externo 2 TB', 'Almacenamiento portatil resistente con transferencia de alta velocidad.', 710000, 'ssd,storage'),
        (11, 'tecnologia', 'Webcam 4K con microfono', 'Imagen nitida, enfoque automatico y audio claro para videollamadas.', 430000, 'webcam,office'),
        (12, 'tecnologia', 'Mini proyector inteligente', 'Proyector portatil con conectividad inalambrica y parlante integrado.', 890000, 'projector,home'),

        (13, 'hogar', 'Sofa modular gris', 'Sofa configurable con tela resistente, cojines removibles y estructura reforzada.', 2380000, 'sofa,livingroom'),
        (14, 'hogar', 'Lampara LED de pie', 'Iluminacion regulable con diseno minimalista para sala o estudio.', 260000, 'lamp,home'),
        (15, 'hogar', 'Aspiradora robot inteligente', 'Mapeo por sensores, control desde app y base de carga automatica.', 1390000, 'robot,vacuum'),
        (16, 'hogar', 'Purificador de aire HEPA', 'Filtro de alta eficiencia para polvo, polen y particulas finas.', 760000, 'air,purifier'),
        (17, 'hogar', 'Mesa auxiliar nordica', 'Superficie en madera clara y patas metalicas para sala o habitacion.', 230000, 'table,furniture'),
        (18, 'hogar', 'Alfombra lavable geometrica', 'Textura suave, base antideslizante y patron moderno.', 310000, 'rug,decor'),
        (19, 'hogar', 'Set organizador de closet', 'Cajas plegables, separadores y bolsas para optimizar almacenamiento.', 145000, 'closet,storage'),
        (20, 'hogar', 'Cortinas blackout dobles', 'Bloqueo de luz y aislamiento termico para dormitorios.', 198000, 'curtains,home'),
        (21, 'hogar', 'Espejo redondo decorativo', 'Marco metalico delgado y acabado elegante para recibidor.', 275000, 'mirror,decor'),
        (22, 'hogar', 'Biblioteca industrial', 'Estanteria de madera y acero con cinco niveles resistentes.', 640000, 'bookshelf,furniture'),
        (23, 'hogar', 'Ventilador torre silencioso', 'Oscilacion amplia, temporizador y control remoto.', 380000, 'fan,home'),
        (24, 'hogar', 'Colchon ortopedico queen', 'Soporte firme con espuma de alta densidad y tela respirable.', 1580000, 'mattress,bedroom'),

        (25, 'gaming', 'Consola PlayStation 5 Slim', 'Consola de nueva generacion con unidad SSD y control DualSense.', 2790000, 'playstation,console'),
        (26, 'gaming', 'Xbox Series X 1 TB', 'Consola 4K con alto rendimiento para juegos actuales.', 2680000, 'xbox,console'),
        (27, 'gaming', 'Nintendo Switch OLED', 'Pantalla OLED, base renovada y controles desmontables.', 1540000, 'nintendo,switch'),
        (28, 'gaming', 'Control inalambrico pro', 'Botones programables, gatillos precisos y agarre texturizado.', 310000, 'gamepad,controller'),
        (29, 'gaming', 'Silla gamer ergonomica', 'Reclinable con cojines lumbar y cervical para sesiones largas.', 890000, 'gaming,chair'),
        (30, 'gaming', 'Monitor gamer 165 Hz', 'Panel rapido con baja latencia y compatibilidad adaptive sync.', 1190000, 'gaming,monitor'),
        (31, 'gaming', 'Headset gamer 7.1', 'Sonido envolvente, microfono desmontable e iluminacion discreta.', 360000, 'gaming,headset'),
        (32, 'gaming', 'Mouse RGB ultraligero', 'Sensor de alta precision, cable flexible y botones configurables.', 220000, 'gaming,mouse'),
        (33, 'gaming', 'Volante racing force feedback', 'Pedales metalicos y respuesta realista para simuladores.', 1320000, 'racing,wheel'),
        (34, 'gaming', 'SSD NVMe para consola 1 TB', 'Expansion rapida compatible con consolas y PC gamer.', 520000, 'ssd,gaming'),
        (35, 'gaming', 'Stream deck compacto', 'Botones LCD configurables para streaming y productividad.', 480000, 'streaming,studio'),
        (36, 'gaming', 'Microfono USB para streaming', 'Captura cardioide, soporte de mesa y control de ganancia.', 390000, 'microphone,streaming'),

        (37, 'moda', 'Chaqueta urbana impermeable', 'Corte moderno con capucha, bolsillos internos y tela repelente.', 280000, 'jacket,fashion'),
        (38, 'moda', 'Tenis running amortiguados', 'Suela ligera y malla transpirable para uso diario y entrenamiento.', 340000, 'sneakers,running'),
        (39, 'moda', 'Reloj minimalista acero', 'Caja delgada, correa intercambiable y resistencia al agua.', 410000, 'watch,fashion'),
        (40, 'moda', 'Bolso crossbody cuero', 'Compartimentos internos y correa ajustable para ciudad.', 260000, 'bag,leather'),
        (41, 'moda', 'Gafas de sol polarizadas', 'Filtro UV400 y montura liviana para uso exterior.', 185000, 'sunglasses,fashion'),
        (42, 'moda', 'Sudadera oversize premium', 'Algodon pesado, fit amplio y acabados reforzados.', 210000, 'hoodie,fashion'),
        (43, 'moda', 'Pantalon cargo tecnico', 'Bolsillos funcionales y tela resistente de secado rapido.', 230000, 'cargo,pants'),
        (44, 'moda', 'Vestido casual lino', 'Tela fresca, silueta comoda y acabado natural.', 250000, 'dress,fashion'),
        (45, 'moda', 'Camisa oxford slim', 'Algodon suave y corte versatil para oficina o fin de semana.', 170000, 'shirt,fashion'),
        (46, 'moda', 'Mochila urbana antirrobo', 'Compartimento para laptop, cierre oculto y puerto USB.', 295000, 'backpack,fashion'),
        (47, 'moda', 'Cinturon cuero reversible', 'Hebilla metalica y doble acabado para uso formal o casual.', 120000, 'belt,leather'),
        (48, 'moda', 'Botas casuales en cuero', 'Suela antideslizante y plantilla acolchada.', 430000, 'boots,fashion'),

        (49, 'deportes', 'Bicicleta urbana aluminio', 'Marco liviano, cambios de precision y frenos de disco.', 1680000, 'bicycle,urban'),
        (50, 'deportes', 'Mancuernas ajustables 24 kg', 'Sistema de seleccion rapida para entrenamiento en casa.', 780000, 'dumbbells,fitness'),
        (51, 'deportes', 'Tapete yoga antideslizante', 'Superficie estable, material eco y correa de transporte.', 115000, 'yoga,mat'),
        (52, 'deportes', 'Balon profesional futbol', 'Cubierta cosida y camara de alta retencion.', 160000, 'football,soccer'),
        (53, 'deportes', 'Raqueta carbono tenis', 'Marco liviano con buen control y potencia equilibrada.', 690000, 'tennis,racket'),
        (54, 'deportes', 'Casco ciclismo ventilado', 'Ajuste micrometrico y proteccion certificada.', 240000, 'cycling,helmet'),
        (55, 'deportes', 'Guantes training cuero', 'Palma reforzada y muneca ajustable para pesas.', 98000, 'fitness,gloves'),
        (56, 'deportes', 'Banda elastica set x5', 'Diferentes resistencias para movilidad y fuerza.', 85000, 'resistance,bands'),
        (57, 'deportes', 'Bicicleta spinning magnetica', 'Volante silencioso, pantalla y resistencia ajustable.', 1290000, 'spinning,bike'),
        (58, 'deportes', 'Morral hidratacion trail', 'Bolsa de agua incluida y bolsillos para ruta.', 210000, 'hydration,trail'),
        (59, 'deportes', 'Rodillo abdominal doble', 'Rueda estable con agarres antideslizantes.', 76000, 'ab,roller'),
        (60, 'deportes', 'Tabla balance entrenamiento', 'Base curva para equilibrio, rehabilitacion y core.', 145000, 'balance,board'),

        (61, 'telefonia', 'Smartphone Pro 256 GB', 'Pantalla OLED, triple camara y carga rapida para uso intensivo.', 3290000, 'smartphone,phone'),
        (62, 'telefonia', 'Smartphone Lite 128 GB', 'Equipo liviano con bateria de larga duracion y camara nitida.', 980000, 'android,phone'),
        (63, 'telefonia', 'Power bank 20000 mAh', 'Carga rapida USB-C y doble salida para viajes.', 165000, 'powerbank,phone'),
        (64, 'telefonia', 'Cargador GaN 65 W', 'Adaptador compacto para celular, tablet y portatil.', 145000, 'charger,phone'),
        (65, 'telefonia', 'Audifonos Bluetooth compactos', 'Estuche de carga, baja latencia y resistencia a salpicaduras.', 210000, 'earbuds,phone'),
        (66, 'telefonia', 'Funda magnetica transparente', 'Proteccion contra golpes y compatibilidad con carga magnetica.', 85000, 'phone,case'),
        (67, 'telefonia', 'Smartwatch deportivo', 'GPS, medicion cardiaca y modos de entrenamiento.', 620000, 'smartwatch,phone'),
        (68, 'telefonia', 'Smartband AMOLED', 'Seguimiento de actividad, sueno y notificaciones.', 180000, 'smartband,phone'),
        (69, 'telefonia', 'Soporte celular para auto', 'Brazo ajustable y agarre firme para tablero o vidrio.', 70000, 'phone,car'),
        (70, 'telefonia', 'Cable USB-C reforzado', 'Cable trenzado con carga rapida y transferencia de datos.', 52000, 'usb,cable'),
        (71, 'telefonia', 'Lente clip para celular', 'Gran angular y macro para fotografia movil creativa.', 95000, 'phone,lens'),
        (72, 'telefonia', 'Gimbal estabilizador movil', 'Estabilizacion de tres ejes y modos de seguimiento.', 540000, 'gimbal,phone'),

        (73, 'oficina', 'Escritorio sit stand electrico', 'Altura ajustable, memoria de posiciones y superficie amplia.', 1450000, 'desk,office'),
        (74, 'oficina', 'Silla ejecutiva ergonomica', 'Soporte lumbar, brazos ajustables y malla transpirable.', 920000, 'office,chair'),
        (75, 'oficina', 'Impresora laser WiFi', 'Impresion rapida, duplex automatico y conectividad inalambrica.', 690000, 'printer,office'),
        (76, 'oficina', 'Lampara escritorio articulada', 'Luz regulable, brazo flexible y base estable.', 185000, 'desk,lamp'),
        (77, 'oficina', 'Microfono USB conferencias', 'Captura clara para reuniones y clases virtuales.', 330000, 'microphone,office'),
        (78, 'oficina', 'Organizador escritorio bambu', 'Bandejas y compartimentos para mantener el puesto ordenado.', 125000, 'desk,organizer'),
        (79, 'oficina', 'Archivador metalico compacto', 'Tres gavetas con cerradura para documentos.', 430000, 'filing,cabinet'),
        (80, 'oficina', 'Monitor portatil USB-C', 'Pantalla liviana para doble monitor en movilidad.', 760000, 'portable,monitor'),
        (81, 'oficina', 'Soporte laptop aluminio', 'Elevacion estable para postura y ventilacion.', 135000, 'laptop,stand'),
        (82, 'oficina', 'Kit teclado y mouse silencioso', 'Combo inalambrico para oficina con teclas de bajo ruido.', 170000, 'keyboard,mouse'),
        (83, 'oficina', 'Pizarra magnetica vidrio', 'Superficie borrable con soporte para marcadores.', 390000, 'whiteboard,office'),
        (84, 'oficina', 'Hub reuniones 360', 'Camara y audio integrados para salas pequenas.', 1280000, 'conference,office'),

        (85, 'cocina', 'Freidora de aire digital', 'Canasta antiadherente, programas automaticos y bajo consumo.', 430000, 'airfryer,kitchen'),
        (86, 'cocina', 'Cafetera espresso compacta', 'Bomba de presion, vaporizador y portafiltro metalico.', 780000, 'espresso,kitchen'),
        (87, 'cocina', 'Batidora planetaria 5 L', 'Motor potente, bowl acero y accesorios para reposteria.', 620000, 'mixer,kitchen'),
        (88, 'cocina', 'Juego cuchillos acero', 'Set profesional con taco de madera y afilador.', 290000, 'knives,kitchen'),
        (89, 'cocina', 'Sartenes antiadherentes set', 'Tres tamanos con mango frio y recubrimiento duradero.', 260000, 'pans,kitchen'),
        (90, 'cocina', 'Extractor de jugos lento', 'Prensado en frio para mayor rendimiento y menos espuma.', 590000, 'juicer,kitchen'),
        (91, 'cocina', 'Olla multifuncion inteligente', 'Coccion a presion, lenta, arroz y vapor en un equipo.', 560000, 'cooker,kitchen'),
        (92, 'cocina', 'Bascula digital precision', 'Medicion exacta para recetas, cafe y porciones.', 78000, 'scale,kitchen'),
        (93, 'cocina', 'Tostadora acero inoxidable', 'Ranuras anchas y niveles de dorado ajustables.', 180000, 'toaster,kitchen'),
        (94, 'cocina', 'Refrigerador minibar', 'Capacidad compacta con congelador pequeno y bajo ruido.', 720000, 'minifridge,kitchen'),
        (95, 'cocina', 'Molino cafe electrico', 'Molienda ajustable para espresso, prensa y filtrados.', 240000, 'coffee,grinder'),
        (96, 'cocina', 'Tabla cortar bambu set', 'Tres tablas resistentes con canales para liquidos.', 95000, 'cutting,board'),

        (97, 'belleza', 'Secador ionico profesional', 'Motor potente, boquilla concentradora y control de temperatura.', 320000, 'hairdryer,beauty'),
        (98, 'belleza', 'Plancha cabello ceramica', 'Placas flotantes, calentamiento rapido y apagado automatico.', 250000, 'hair,straightener'),
        (99, 'belleza', 'Kit skincare hidratante', 'Limpiador, serum y crema para rutina diaria.', 210000, 'skincare,beauty'),
        (100, 'belleza', 'Perfume signature 100 ml', 'Fragancia elegante con notas frescas y amaderadas.', 360000, 'perfume,beauty'),
        (101, 'belleza', 'Rizador cabello automatico', 'Barril ceramico y temporizador para ondas definidas.', 290000, 'curling,iron'),
        (102, 'belleza', 'Afeitadora electrica recargable', 'Cuchillas flexibles y uso seco o humedo.', 260000, 'shaver,beauty'),
        (103, 'belleza', 'Cepillo facial sonico', 'Limpieza profunda con cerdas suaves de silicona.', 170000, 'facial,brush'),
        (104, 'belleza', 'Kit manicure semipermanente', 'Lampara UV, esmaltes y herramientas esenciales.', 230000, 'manicure,beauty'),
        (105, 'belleza', 'Mascarilla LED facial', 'Terapia de luz con modos para cuidado de piel.', 440000, 'led,mask'),
        (106, 'belleza', 'Serum vitamina C', 'Formula antioxidante para luminosidad y textura uniforme.', 125000, 'serum,beauty'),
        (107, 'belleza', 'Cepillo secador volumizador', 'Cepilla y seca en una sola pasada con control de calor.', 270000, 'hair,brush'),
        (108, 'belleza', 'Organizador cosmeticos acrilico', 'Cajones transparentes y divisiones para maquillaje.', 135000, 'makeup,organizer'),

        (109, 'auto', 'Compresor aire portatil', 'Inflador digital para llantas con apagado automatico.', 220000, 'air,compressor'),
        (110, 'auto', 'Escaner OBD2 Bluetooth', 'Diagnostico desde app para revisar codigos del vehiculo.', 160000, 'obd,car'),
        (111, 'auto', 'Aspiradora auto inalambrica', 'Boquillas multiples y filtro lavable para interiores.', 190000, 'car,vacuum'),
        (112, 'auto', 'Kit emergencia carretera', 'Triangulos, cables, linterna y herramientas basicas.', 210000, 'car,emergency'),
        (113, 'auto', 'Soporte celular magnetico', 'Base firme para rejilla con rotacion de 360 grados.', 65000, 'phone,car'),
        (114, 'auto', 'Camara tablero dashcam', 'Grabacion Full HD, sensor de impacto y vision nocturna.', 340000, 'dashcam,car'),
        (115, 'auto', 'Cargador bateria inteligente', 'Mantenimiento automatico para baterias de carro y moto.', 260000, 'battery,charger'),
        (116, 'auto', 'Luces LED auxiliares', 'Par de luces impermeables para mejor visibilidad.', 180000, 'led,car'),
        (117, 'auto', 'Aceite sintetico 5W30', 'Lubricante de alto rendimiento para motores modernos.', 145000, 'motor,oil'),
        (118, 'auto', 'Tapetes carro universales', 'Set resistente al agua con base antideslizante.', 130000, 'car,mats'),
        (119, 'auto', 'Hidrolavadora compacta', 'Presion ajustable para limpieza de vehiculos y patios.', 520000, 'pressure,washer'),
        (120, 'auto', 'Pulidora orbital auto', 'Velocidad variable y mango ergonomico para detallado.', 390000, 'car,polisher')
    ) as p(n, category, title, description, price, image_query)
),
seller_pool as (
    select *
    from (
        values
        (0, '00000000-0000-0000-0000-000000000021'::uuid),
        (1, '00000000-0000-0000-0000-000000000022'::uuid),
        (2, '00000000-0000-0000-0000-000000000023'::uuid),
        (3, '00000000-0000-0000-0000-000000000024'::uuid),
        (4, '00000000-0000-0000-0000-000000000025'::uuid),
        (5, '00000000-0000-0000-0000-000000000026'::uuid),
        (6, '00000000-0000-0000-0000-000000000027'::uuid),
        (7, '00000000-0000-0000-0000-000000000028'::uuid)
    ) as s(slot, seller_id)
),
catalog as (
    select
        ('00000000-0000-0000-0000-' || lpad(p.n::text, 12, '0'))::uuid as id,
        s.seller_id,
        p.title,
        p.description,
        p.category,
        ('https://loremflickr.com/900/700/' || replace(p.image_query, ' ', '-') || '?lock=' || (7000 + p.n)) as image_url,
        json_build_array(
            'https://loremflickr.com/900/700/' || replace(p.image_query, ' ', '-') || '?lock=' || (7000 + p.n),
            'https://loremflickr.com/900/700/' || replace(p.category || ',product', ' ', '-') || '?lock=' || (8000 + p.n),
            'https://loremflickr.com/900/700/' || replace(p.category || ',detail', ' ', '-') || '?lock=' || (9000 + p.n)
        )::text as image_urls,
        p.price::numeric(12,2) as price,
        (8 + (p.n % 43)) as stock_available,
        now() - (p.n || ' hours')::interval as created_at
    from seed_products p
    join seller_pool s on s.slot = ((p.n - 1) % 8)
)
insert into product (
    id, seller_id, title, description, category, image_url, image_urls,
    price, stock_available, status, version, created_at
)
select
    id, seller_id, title, description, category, image_url, image_urls,
    price, stock_available, 'ACTIVE', 0, created_at
from catalog
on conflict (id) do update
set
    seller_id = excluded.seller_id,
    title = excluded.title,
    description = excluded.description,
    category = excluded.category,
    image_url = excluded.image_url,
    image_urls = excluded.image_urls,
    price = excluded.price,
    stock_available = excluded.stock_available,
    status = excluded.status,
    version = product.version + 1,
    created_at = excluded.created_at;

update product
   set seller_id = case (abs(('x' || substr(md5(id::text), 1, 8))::bit(32)::int) % 8)
        when 0 then '00000000-0000-0000-0000-000000000021'::uuid
        when 1 then '00000000-0000-0000-0000-000000000022'::uuid
        when 2 then '00000000-0000-0000-0000-000000000023'::uuid
        when 3 then '00000000-0000-0000-0000-000000000024'::uuid
        when 4 then '00000000-0000-0000-0000-000000000025'::uuid
        when 5 then '00000000-0000-0000-0000-000000000026'::uuid
        when 6 then '00000000-0000-0000-0000-000000000027'::uuid
        else '00000000-0000-0000-0000-000000000028'::uuid
    end
 where seller_id not in (select id from app_user where roles like '%SELLER%');

do $$
begin
    if not exists (
        select 1
          from information_schema.table_constraints
         where constraint_schema = current_schema()
           and table_name = 'product'
           and constraint_name = 'fk_product_seller'
    ) then
        alter table product
            add constraint fk_product_seller
            foreign key (seller_id) references app_user(id);
    end if;
end $$;
