-- =========================================================
-- Datos iniciales de MuvU (se cargan al arrancar la app; la BD se recrea en cada arranque)
-- Admin:        admin@muvu.com / admin123
-- Arrendadores: rosa.quispe@muvu.com (y 4 más, verificados) / Clave123!
--               pedro.salas@muvu.com (SIN verificar)         / Clave123!
-- Estudiantes:  ana.ramos@utec.edu.pe (y 7 más)              / Clave123!
-- La colección de Postman usa estos usuarios y algunos ids fijos (ver comentarios abajo).
-- =========================================================

-- ---------- Admin ----------
INSERT INTO usuario (nombre, correo, contrasena, verificado, rol)
VALUES ('Administrador', 'admin@muvu.com',
        '$2a$10$i4yGias7IgHPGQz5YKF/auCBE.E1kJVCAFJElLSETYFlNaiCb4Mlu',
        true, 'ADMIN');

INSERT INTO admin (id)
SELECT id FROM usuario WHERE correo = 'admin@muvu.com';

-- ---------- Universidades ----------
INSERT INTO universidad (nombre, direccion, latitud, longitud) VALUES
('UTEC', 'Jr. Medrano Silva 165, Barranco, Lima', -12.1352, -77.0221),
('PUCP', 'Av. Universitaria 1801, San Miguel, Lima', -12.0694, -77.0799),
('UNI', 'Av. Túpac Amaru 210, Rímac, Lima', -12.0237, -77.0480),
('UNMSM', 'Av. Carlos Germán Amezaga 375, Cercado de Lima, Lima', -12.0560, -77.0840),
('Universidad de Lima', 'Av. Javier Prado Este 4600, Santiago de Surco, Lima', -12.0845, -76.9710),
('UPC', 'Av. Prolongación Primavera 2390, Santiago de Surco, Lima', -12.1040, -76.9630),
('Universidad del Pacífico', 'Av. Salaverry 2020, Jesús María, Lima', -12.0830, -77.0490),
('UPCH', 'Av. Honorio Delgado 430, San Martín de Porres, Lima', -12.0240, -77.0550),
('USIL', 'Av. La Fontana 550, La Molina, Lima', -12.0730, -76.9540),
('ESAN', 'Alonso de Molina 1652, Santiago de Surco, Lima', -12.1050, -76.9620),
('Universidad Ricardo Palma', 'Av. Benavides 5440, Santiago de Surco, Lima', -12.1320, -76.9870);

-- ---------- Arrendadores (5 verificados, 10 habitaciones cada uno) ----------
INSERT INTO usuario (nombre, correo, contrasena, verificado, rol) VALUES
('Rosa Quispe Huamán', 'rosa.quispe@muvu.com', '$2a$10$kw6salxyT3B72bfEqTtIRuswSONQZliys1lmkxWNtV55/6EfsWtI.', true, 'ARRENDADOR'),
('Carlos Mendoza Ríos', 'carlos.mendoza@muvu.com', '$2a$10$kw6salxyT3B72bfEqTtIRuswSONQZliys1lmkxWNtV55/6EfsWtI.', true, 'ARRENDADOR'),
('Lucía Fernández Soto', 'lucia.fernandez@muvu.com', '$2a$10$kw6salxyT3B72bfEqTtIRuswSONQZliys1lmkxWNtV55/6EfsWtI.', true, 'ARRENDADOR'),
('Jorge Paredes Villanueva', 'jorge.paredes@muvu.com', '$2a$10$kw6salxyT3B72bfEqTtIRuswSONQZliys1lmkxWNtV55/6EfsWtI.', true, 'ARRENDADOR'),
('María Torres Castillo', 'maria.torres@muvu.com', '$2a$10$kw6salxyT3B72bfEqTtIRuswSONQZliys1lmkxWNtV55/6EfsWtI.', true, 'ARRENDADOR');

INSERT INTO arrendador (id, dni_foto, puntaje_promedio, total_calificaciones, cantidad_habitaciones)
SELECT id, 'https://muvu-demo.s3.amazonaws.com/dni/' || id || '.jpg', 0.0, 0, 10
FROM usuario WHERE rol = 'ARRENDADOR';

-- ---------- Habitaciones (50: 10 por arrendador, algunas destacadas) ----------
INSERT INTO habitacion (direccion, latitud, longitud, precio, area, es_destacada, arrendador_id) VALUES
('Jr. Colina 940 Dpto. 504, Barranco, Lima', -12.1419, -77.0181, 830.0, 15, true, (SELECT id FROM usuario WHERE correo = 'rosa.quispe@muvu.com')),
('Av. Grau 1120 Dpto. 302, Barranco, Lima', -12.1364, -77.0307, 860.0, 15, true, (SELECT id FROM usuario WHERE correo = 'rosa.quispe@muvu.com')),
('Av. Grau 200 Dpto. 404, Barranco, Lima', -12.1286, -77.0145, 850.0, 18, true, (SELECT id FROM usuario WHERE correo = 'rosa.quispe@muvu.com')),
('Av. Grau 520 Dpto. 604, Barranco, Lima', -12.1470, -77.0318, 600.0, 10, false, (SELECT id FROM usuario WHERE correo = 'rosa.quispe@muvu.com')),
('Calle Domeyer 710, Barranco, Lima', -12.1392, -77.0229, 700.0, 10, false, (SELECT id FROM usuario WHERE correo = 'rosa.quispe@muvu.com')),
('Jr. Colina 1580 Dpto. 103, Barranco, Lima', -12.1451, -77.0178, 790.0, 16, false, (SELECT id FROM usuario WHERE correo = 'rosa.quispe@muvu.com')),
('Av. Grau 610 Dpto. 804, Barranco, Lima', -12.1324, -77.0300, 950.0, 18, false, (SELECT id FROM usuario WHERE correo = 'rosa.quispe@muvu.com')),
('Av. Pedro de Osma 690, Barranco, Lima', -12.1318, -77.0116, 900.0, 20, false, (SELECT id FROM usuario WHERE correo = 'rosa.quispe@muvu.com')),
('Av. Pedro de Osma 710 Dpto. 203, Barranco, Lima', -12.1358, -77.0103, 820.0, 18, false, (SELECT id FROM usuario WHERE correo = 'rosa.quispe@muvu.com')),
('Av. Grau 330, Barranco, Lima', -12.1490, -77.0166, 630.0, 10, false, (SELECT id FROM usuario WHERE correo = 'rosa.quispe@muvu.com')),
('Av. Caminos del Inca 1450, Santiago de Surco, Lima', -12.0980, -76.9609, 1010.0, 25, true, (SELECT id FROM usuario WHERE correo = 'carlos.mendoza@muvu.com')),
('Calle Los Álamos 1150, Santiago de Surco, Lima', -12.0986, -76.9679, 1210.0, 28, true, (SELECT id FROM usuario WHERE correo = 'carlos.mendoza@muvu.com')),
('Calle Los Álamos 1740, Santiago de Surco, Lima', -12.1033, -76.9801, 850.0, 20, true, (SELECT id FROM usuario WHERE correo = 'carlos.mendoza@muvu.com')),
('Av. Caminos del Inca 490, Santiago de Surco, Lima', -12.1035, -76.9621, 830.0, 18, false, (SELECT id FROM usuario WHERE correo = 'carlos.mendoza@muvu.com')),
('Av. Primavera 300, Santiago de Surco, Lima', -12.0914, -76.9780, 820.0, 16, false, (SELECT id FROM usuario WHERE correo = 'carlos.mendoza@muvu.com')),
('Jr. Monte Rosa 600 Dpto. 104, Santiago de Surco, Lima', -12.0885, -76.9796, 940.0, 18, false, (SELECT id FROM usuario WHERE correo = 'carlos.mendoza@muvu.com')),
('Calle Los Álamos 1300 Dpto. 102, Santiago de Surco, Lima', -12.1012, -76.9614, 910.0, 16, false, (SELECT id FROM usuario WHERE correo = 'carlos.mendoza@muvu.com')),
('Calle Los Álamos 1170, Santiago de Surco, Lima', -12.1054, -76.9741, 760.0, 12, false, (SELECT id FROM usuario WHERE correo = 'carlos.mendoza@muvu.com')),
('Calle Los Álamos 880, Santiago de Surco, Lima', -12.1051, -76.9708, 890.0, 20, false, (SELECT id FROM usuario WHERE correo = 'carlos.mendoza@muvu.com')),
('Av. El Derby 410 Dpto. 403, Santiago de Surco, Lima', -12.1087, -76.9791, 860.0, 20, false, (SELECT id FROM usuario WHERE correo = 'carlos.mendoza@muvu.com')),
('Av. Universitaria 1020 Dpto. 803, San Miguel, Lima', -12.0679, -77.0748, 920.0, 16, true, (SELECT id FROM usuario WHERE correo = 'lucia.fernandez@muvu.com')),
('Calle Federico Gallese 1660, San Miguel, Lima', -12.0757, -77.0876, 1180.0, 30, true, (SELECT id FROM usuario WHERE correo = 'lucia.fernandez@muvu.com')),
('Av. Universitaria 1190, San Miguel, Lima', -12.0809, -77.0840, 950.0, 18, true, (SELECT id FROM usuario WHERE correo = 'lucia.fernandez@muvu.com')),
('Calle Federico Gallese 1850, San Miguel, Lima', -12.0763, -77.0837, 830.0, 14, false, (SELECT id FROM usuario WHERE correo = 'lucia.fernandez@muvu.com')),
('Av. Riva Agüero 1780 Dpto. 602, San Miguel, Lima', -12.0731, -77.0757, 1250.0, 30, false, (SELECT id FROM usuario WHERE correo = 'lucia.fernandez@muvu.com')),
('Av. Riva Agüero 840 Dpto. 401, San Miguel, Lima', -12.0845, -77.0943, 1030.0, 22, false, (SELECT id FROM usuario WHERE correo = 'lucia.fernandez@muvu.com')),
('Calle Federico Gallese 1510 Dpto. 901, San Miguel, Lima', -12.0674, -77.0776, 1210.0, 30, false, (SELECT id FROM usuario WHERE correo = 'lucia.fernandez@muvu.com')),
('Av. Universitaria 1420 Dpto. 302, San Miguel, Lima', -12.0815, -77.0771, 830.0, 16, false, (SELECT id FROM usuario WHERE correo = 'lucia.fernandez@muvu.com')),
('Av. La Marina 1680, San Miguel, Lima', -12.0784, -77.0737, 930.0, 18, false, (SELECT id FROM usuario WHERE correo = 'lucia.fernandez@muvu.com')),
('Av. Universitaria 900 Dpto. 502, San Miguel, Lima', -12.0843, -77.0734, 960.0, 22, false, (SELECT id FROM usuario WHERE correo = 'lucia.fernandez@muvu.com')),
('Jr. Mariscal Miller 1070 Dpto. 702, Jesús María, Lima', -12.0688, -77.0453, 640.0, 10, true, (SELECT id FROM usuario WHERE correo = 'jorge.paredes@muvu.com')),
('Av. Brasil 1850 Dpto. 201, Jesús María, Lima', -12.0677, -77.0472, 890.0, 15, true, (SELECT id FROM usuario WHERE correo = 'jorge.paredes@muvu.com')),
('Av. Brasil 1390, Jesús María, Lima', -12.0892, -77.0558, 1170.0, 30, true, (SELECT id FROM usuario WHERE correo = 'jorge.paredes@muvu.com')),
('Av. Salaverry 890, Jesús María, Lima', -12.0706, -77.0570, 1230.0, 30, false, (SELECT id FROM usuario WHERE correo = 'jorge.paredes@muvu.com')),
('Jr. Mariscal Miller 290, Jesús María, Lima', -12.0865, -77.0438, 690.0, 12, false, (SELECT id FROM usuario WHERE correo = 'jorge.paredes@muvu.com')),
('Av. Salaverry 640, Jesús María, Lima', -12.0881, -77.0577, 810.0, 15, false, (SELECT id FROM usuario WHERE correo = 'jorge.paredes@muvu.com')),
('Av. Salaverry 880, Jesús María, Lima', -12.0832, -77.0522, 680.0, 12, false, (SELECT id FROM usuario WHERE correo = 'jorge.paredes@muvu.com')),
('Jr. Huiracocha 890, Jesús María, Lima', -12.0712, -77.0470, 1020.0, 22, false, (SELECT id FROM usuario WHERE correo = 'jorge.paredes@muvu.com')),
('Av. Arnaldo Márquez 850, Jesús María, Lima', -12.0811, -77.0556, 760.0, 15, false, (SELECT id FROM usuario WHERE correo = 'jorge.paredes@muvu.com')),
('Jr. Mariscal Miller 1250, Jesús María, Lima', -12.0825, -77.0437, 840.0, 18, false, (SELECT id FROM usuario WHERE correo = 'jorge.paredes@muvu.com')),
('Av. Javier Prado Este 1700 Dpto. 603, La Molina, Lima', -12.0890, -76.9500, 1280.0, 30, true, (SELECT id FROM usuario WHERE correo = 'maria.torres@muvu.com')),
('Av. Javier Prado Este 1770, La Molina, Lima', -12.0759, -76.9440, 1240.0, 30, true, (SELECT id FROM usuario WHERE correo = 'maria.torres@muvu.com')),
('Jr. Las Retamas 1490, La Molina, Lima', -12.0825, -76.9437, 580.0, 10, true, (SELECT id FROM usuario WHERE correo = 'maria.torres@muvu.com')),
('Jr. Las Retamas 1260 Dpto. 603, La Molina, Lima', -12.0884, -76.9449, 790.0, 18, false, (SELECT id FROM usuario WHERE correo = 'maria.torres@muvu.com')),
('Calle Los Fresnos 1390, La Molina, Lima', -12.0839, -76.9394, 1100.0, 28, false, (SELECT id FROM usuario WHERE correo = 'maria.torres@muvu.com')),
('Av. Raúl Ferrero 840 Dpto. 203, La Molina, Lima', -12.0764, -76.9602, 1000.0, 20, false, (SELECT id FROM usuario WHERE correo = 'maria.torres@muvu.com')),
('Av. La Fontana 1070 Dpto. 602, La Molina, Lima', -12.0854, -76.9484, 820.0, 14, false, (SELECT id FROM usuario WHERE correo = 'maria.torres@muvu.com')),
('Av. La Fontana 1820 Dpto. 803, La Molina, Lima', -12.0832, -76.9525, 770.0, 16, false, (SELECT id FROM usuario WHERE correo = 'maria.torres@muvu.com')),
('Jr. Las Retamas 1710 Dpto. 103, La Molina, Lima', -12.0777, -76.9453, 1190.0, 28, false, (SELECT id FROM usuario WHERE correo = 'maria.torres@muvu.com')),
('Jr. Las Retamas 1220 Dpto. 803, La Molina, Lima', -12.0721, -76.9547, 930.0, 20, false, (SELECT id FROM usuario WHERE correo = 'maria.torres@muvu.com'));

-- ---------- Pagos de publicidad de las habitaciones destacadas (vigentes) ----------
-- 30 = 30 días, 54 = 60 días. Las fechas son relativas al día en que arranca la app.
INSERT INTO pago_publicidad (habitacion_id, monto, metodo_pago, fecha_inicio, fecha_fin) VALUES
((SELECT id FROM habitacion WHERE direccion = 'Jr. Colina 940 Dpto. 504, Barranco, Lima'), 30.0, 'TARJETA', CURRENT_DATE - 8, CURRENT_DATE + 22),
((SELECT id FROM habitacion WHERE direccion = 'Av. Grau 1120 Dpto. 302, Barranco, Lima'), 54.0, 'TARJETA', CURRENT_DATE - 54, CURRENT_DATE + 6),
((SELECT id FROM habitacion WHERE direccion = 'Av. Grau 200 Dpto. 404, Barranco, Lima'), 30.0, 'TARJETA', CURRENT_DATE - 8, CURRENT_DATE + 22),
((SELECT id FROM habitacion WHERE direccion = 'Av. Caminos del Inca 1450, Santiago de Surco, Lima'), 54.0, 'TARJETA', CURRENT_DATE - 55, CURRENT_DATE + 5),
((SELECT id FROM habitacion WHERE direccion = 'Calle Los Álamos 1150, Santiago de Surco, Lima'), 30.0, 'TARJETA', CURRENT_DATE - 23, CURRENT_DATE + 7),
((SELECT id FROM habitacion WHERE direccion = 'Calle Los Álamos 1740, Santiago de Surco, Lima'), 54.0, 'TARJETA', CURRENT_DATE - 15, CURRENT_DATE + 45),
((SELECT id FROM habitacion WHERE direccion = 'Av. Universitaria 1020 Dpto. 803, San Miguel, Lima'), 30.0, 'TARJETA', CURRENT_DATE - 17, CURRENT_DATE + 13),
((SELECT id FROM habitacion WHERE direccion = 'Calle Federico Gallese 1660, San Miguel, Lima'), 54.0, 'TARJETA', CURRENT_DATE - 39, CURRENT_DATE + 21),
((SELECT id FROM habitacion WHERE direccion = 'Av. Universitaria 1190, San Miguel, Lima'), 30.0, 'TARJETA', CURRENT_DATE - 12, CURRENT_DATE + 18),
((SELECT id FROM habitacion WHERE direccion = 'Jr. Mariscal Miller 1070 Dpto. 702, Jesús María, Lima'), 54.0, 'TARJETA', CURRENT_DATE - 40, CURRENT_DATE + 20),
((SELECT id FROM habitacion WHERE direccion = 'Av. Brasil 1850 Dpto. 201, Jesús María, Lima'), 30.0, 'TARJETA', CURRENT_DATE - 12, CURRENT_DATE + 18),
((SELECT id FROM habitacion WHERE direccion = 'Av. Brasil 1390, Jesús María, Lima'), 54.0, 'TARJETA', CURRENT_DATE - 22, CURRENT_DATE + 38),
((SELECT id FROM habitacion WHERE direccion = 'Av. Javier Prado Este 1700 Dpto. 603, La Molina, Lima'), 30.0, 'TARJETA', CURRENT_DATE - 4, CURRENT_DATE + 26),
((SELECT id FROM habitacion WHERE direccion = 'Av. Javier Prado Este 1770, La Molina, Lima'), 54.0, 'TARJETA', CURRENT_DATE - 35, CURRENT_DATE + 25),
((SELECT id FROM habitacion WHERE direccion = 'Jr. Las Retamas 1490, La Molina, Lima'), 30.0, 'TARJETA', CURRENT_DATE - 2, CURRENT_DATE + 28);

-- ---------- Arrendador SIN verificar (id 7): para probar PATCH /arrendadores/{id}/verificar ----------
INSERT INTO usuario (nombre, correo, contrasena, verificado, rol)
VALUES ('Pedro Salas Gutiérrez', 'pedro.salas@muvu.com', '$2a$10$kw6salxyT3B72bfEqTtIRuswSONQZliys1lmkxWNtV55/6EfsWtI.', false, 'ARRENDADOR');

INSERT INTO arrendador (id, dni_foto, puntaje_promedio, total_calificaciones, cantidad_habitaciones)
SELECT id, 'https://muvu-demo.s3.amazonaws.com/dni/' || id || '.jpg', 0.0, 0, 0
FROM usuario WHERE correo = 'pedro.salas@muvu.com';

-- ---------- Estudiantes (ids 8 a 15; ana.ramos = 8) ----------
INSERT INTO usuario (nombre, correo, contrasena, verificado, rol) VALUES
('Ana Lucía Ramos Pérez', 'ana.ramos@utec.edu.pe', '$2a$10$kw6salxyT3B72bfEqTtIRuswSONQZliys1lmkxWNtV55/6EfsWtI.', true, 'ESTUDIANTE'),
('Diego Alonso Chávez Rojas', 'diego.chavez@utec.edu.pe', '$2a$10$kw6salxyT3B72bfEqTtIRuswSONQZliys1lmkxWNtV55/6EfsWtI.', true, 'ESTUDIANTE'),
('Valeria Sofía Núñez Díaz', 'valeria.nunez@pucp.edu.pe', '$2a$10$kw6salxyT3B72bfEqTtIRuswSONQZliys1lmkxWNtV55/6EfsWtI.', true, 'ESTUDIANTE'),
('Sebastián Torres Medina', 'sebastian.torres@pucp.edu.pe', '$2a$10$kw6salxyT3B72bfEqTtIRuswSONQZliys1lmkxWNtV55/6EfsWtI.', true, 'ESTUDIANTE'),
('Camila Andrea Flores Vega', 'camila.flores@uni.edu.pe', '$2a$10$kw6salxyT3B72bfEqTtIRuswSONQZliys1lmkxWNtV55/6EfsWtI.', true, 'ESTUDIANTE'),
('Matías Gonzalo Herrera León', 'matias.herrera@uni.edu.pe', '$2a$10$kw6salxyT3B72bfEqTtIRuswSONQZliys1lmkxWNtV55/6EfsWtI.', true, 'ESTUDIANTE'),
('Fernanda Ruiz Castro', 'fernanda.ruiz@unmsm.edu.pe', '$2a$10$kw6salxyT3B72bfEqTtIRuswSONQZliys1lmkxWNtV55/6EfsWtI.', true, 'ESTUDIANTE'),
('Renato Salazar Ortiz', 'renato.salazar@ulima.edu.pe', '$2a$10$kw6salxyT3B72bfEqTtIRuswSONQZliys1lmkxWNtV55/6EfsWtI.', true, 'ESTUDIANTE');

INSERT INTO estudiante (id, universidad_id) VALUES
((SELECT id FROM usuario WHERE correo = 'ana.ramos@utec.edu.pe'), (SELECT id FROM universidad WHERE nombre = 'UTEC')),
((SELECT id FROM usuario WHERE correo = 'diego.chavez@utec.edu.pe'), (SELECT id FROM universidad WHERE nombre = 'UTEC')),
((SELECT id FROM usuario WHERE correo = 'valeria.nunez@pucp.edu.pe'), (SELECT id FROM universidad WHERE nombre = 'PUCP')),
((SELECT id FROM usuario WHERE correo = 'sebastian.torres@pucp.edu.pe'), (SELECT id FROM universidad WHERE nombre = 'PUCP')),
((SELECT id FROM usuario WHERE correo = 'camila.flores@uni.edu.pe'), (SELECT id FROM universidad WHERE nombre = 'UNI')),
((SELECT id FROM usuario WHERE correo = 'matias.herrera@uni.edu.pe'), (SELECT id FROM universidad WHERE nombre = 'UNI')),
((SELECT id FROM usuario WHERE correo = 'fernanda.ruiz@unmsm.edu.pe'), (SELECT id FROM universidad WHERE nombre = 'UNMSM')),
((SELECT id FROM usuario WHERE correo = 'renato.salazar@ulima.edu.pe'), (SELECT id FROM universidad WHERE nombre = 'Universidad de Lima'));

-- ---------- Imágenes (fotos reales de dormitorios y baños, Unsplash: uso libre) ----------
-- Cada habitación recibe 2 fotos de dormitorio distintas; las de 18 m² o más (con baño propio) también una del baño.
-- Las URLs son de images.unsplash.com; w=1200 deja un tamaño liviano para la galería.
WITH fotos AS (
    SELECT ARRAY[
        '1646148707098-a27b818d5c95',
        '1646490577169-2be2bba96c86',
        '1646743742987-cf69915d21cf',
        '1646572961739-58557003b037',
        '1646838116343-1fd356d7e4e8',
        '1647521088714-a9952b135c1e',
        '1647705722868-3ff8e93b73b2',
        '1647949849240-143a5da605ca',
        '1647870988181-5298e3b9036e',
        '1648125700798-d3233f118ec6',
        '1648668425549-84b695d2ec2a',
        '1648690979774-a36ae455bacd',
        '1648726660921-7413e1b0f93b',
        '1648949416036-cf80a1335cce',
        '1649265825072-f7dd6942baed',
        '1649817018876-c05473c7e5a4',
        '1650637472771-16fedc3c6668',
        '1652363499754-ec1ef28f88ac',
        '1651869835364-9686f86159c1',
        '1522771739844-6a9f6d5f14af',
        '1552558636-f6a8f071c2b3',
        '1617325247661-675ab4b64ae2',
        '1699942681763-d1da9f692489',
        '1757344454333-cc666252e596',
        '1765862835319-18fb6f8caff6',
        '1718717722247-26f4c6c09192',
        '1610307522657-8c0304960189',
        '1760072513376-67a46aab0fd1',
        '1779903726446-6f796d533d47',
        '1555854877-bab0e564b8d5',
        '1564273795917-fe399b763988',
        '1549881567-c622c1080d78',
        '1572496973076-dc34056ceb87',
        '1588939349575-7ab15c8bd1ef',
        '1634120830231-4d9dabb67e2d',
        '1721743169043-dda0212ce3d4'
    ] AS dormitorios,
    ARRAY[
        '1584622650111-993a426fbf0a',
        '1631889993959-41b4e9c6e3c5',
        '1629079447777-1e605162dc8d',
        '1552321554-5fefe8c9ef14',
        '1643949719317-4342d8d4031e',
        '1576698483491-8c43f0862543',
        '1646592491820-49bafd614c0e'
    ] AS banos
), habs AS (
    SELECT id, area, (row_number() OVER (ORDER BY id) - 1)::int AS n FROM habitacion
)
INSERT INTO imagen (habitacion_id, url)
SELECT h.id, 'https://images.unsplash.com/photo-' || f.dormitorios[1 + h.n % 36] || '?w=1200&q=80&auto=format&fit=crop'
FROM habs h, fotos f
UNION ALL
SELECT h.id, 'https://images.unsplash.com/photo-' || f.dormitorios[1 + (h.n + 18) % 36] || '?w=1200&q=80&auto=format&fit=crop'
FROM habs h, fotos f
UNION ALL
SELECT h.id, 'https://images.unsplash.com/photo-' || f.banos[1 + h.n % 7] || '?w=1200&q=80&auto=format&fit=crop'
FROM habs h, fotos f
WHERE h.area >= 18;

-- ---------- Reservas (ids 1 a 16, sin cruces de fechas en una misma habitación) ----------
-- 1-8: confirmadas y ya calificadas | 9: confirmada SIN calificar (Postman la califica como ana.ramos)
-- 10: confirmada futura | 11-14: pendientes (rosa.quispe confirma la 11) | 15-16: canceladas
INSERT INTO reserva (fecha_inicio, fecha_fin, estado, estudiante_id, habitacion_id) VALUES
(CURRENT_DATE - 120, CURRENT_DATE - 60, 'CONFIRMADO', (SELECT id FROM usuario WHERE correo = 'ana.ramos@utec.edu.pe'), (SELECT id FROM habitacion WHERE direccion = 'Jr. Colina 940 Dpto. 504, Barranco, Lima')),
(CURRENT_DATE - 150, CURRENT_DATE - 90, 'CONFIRMADO', (SELECT id FROM usuario WHERE correo = 'diego.chavez@utec.edu.pe'), (SELECT id FROM habitacion WHERE direccion = 'Av. Grau 1120 Dpto. 302, Barranco, Lima')),
(CURRENT_DATE - 100, CURRENT_DATE - 40, 'CONFIRMADO', (SELECT id FROM usuario WHERE correo = 'valeria.nunez@pucp.edu.pe'), (SELECT id FROM habitacion WHERE direccion = 'Av. Universitaria 1020 Dpto. 803, San Miguel, Lima')),
(CURRENT_DATE - 200, CURRENT_DATE - 140, 'CONFIRMADO', (SELECT id FROM usuario WHERE correo = 'sebastian.torres@pucp.edu.pe'), (SELECT id FROM habitacion WHERE direccion = 'Calle Federico Gallese 1660, San Miguel, Lima')),
(CURRENT_DATE - 90, CURRENT_DATE - 30, 'CONFIRMADO', (SELECT id FROM usuario WHERE correo = 'camila.flores@uni.edu.pe'), (SELECT id FROM habitacion WHERE direccion = 'Av. Brasil 1850 Dpto. 201, Jesús María, Lima')),
(CURRENT_DATE - 200, CURRENT_DATE - 120, 'CONFIRMADO', (SELECT id FROM usuario WHERE correo = 'fernanda.ruiz@unmsm.edu.pe'), (SELECT id FROM habitacion WHERE direccion = 'Av. Brasil 1850 Dpto. 201, Jesús María, Lima')),
(CURRENT_DATE - 80, CURRENT_DATE - 20, 'CONFIRMADO', (SELECT id FROM usuario WHERE correo = 'renato.salazar@ulima.edu.pe'), (SELECT id FROM habitacion WHERE direccion = 'Av. Javier Prado Este 1700 Dpto. 603, La Molina, Lima')),
(CURRENT_DATE - 60, CURRENT_DATE - 10, 'CONFIRMADO', (SELECT id FROM usuario WHERE correo = 'matias.herrera@uni.edu.pe'), (SELECT id FROM habitacion WHERE direccion = 'Calle Los Álamos 1150, Santiago de Surco, Lima')),
(CURRENT_DATE - 70, CURRENT_DATE - 15, 'CONFIRMADO', (SELECT id FROM usuario WHERE correo = 'ana.ramos@utec.edu.pe'), (SELECT id FROM habitacion WHERE direccion = 'Av. Caminos del Inca 1450, Santiago de Surco, Lima')),
(CURRENT_DATE + 5, CURRENT_DATE + 95, 'CONFIRMADO', (SELECT id FROM usuario WHERE correo = 'diego.chavez@utec.edu.pe'), (SELECT id FROM habitacion WHERE direccion = 'Jr. Colina 940 Dpto. 504, Barranco, Lima')),
(CURRENT_DATE + 30, CURRENT_DATE + 120, 'PENDIENTE', (SELECT id FROM usuario WHERE correo = 'ana.ramos@utec.edu.pe'), (SELECT id FROM habitacion WHERE direccion = 'Av. Grau 1120 Dpto. 302, Barranco, Lima')),
(CURRENT_DATE + 10, CURRENT_DATE + 100, 'PENDIENTE', (SELECT id FROM usuario WHERE correo = 'valeria.nunez@pucp.edu.pe'), (SELECT id FROM habitacion WHERE direccion = 'Av. Universitaria 1020 Dpto. 803, San Miguel, Lima')),
(CURRENT_DATE + 15, CURRENT_DATE + 75, 'PENDIENTE', (SELECT id FROM usuario WHERE correo = 'camila.flores@uni.edu.pe'), (SELECT id FROM habitacion WHERE direccion = 'Calle Los Álamos 1150, Santiago de Surco, Lima')),
(CURRENT_DATE + 20, CURRENT_DATE + 110, 'PENDIENTE', (SELECT id FROM usuario WHERE correo = 'sebastian.torres@pucp.edu.pe'), (SELECT id FROM habitacion WHERE direccion = 'Av. Javier Prado Este 1700 Dpto. 603, La Molina, Lima')),
(CURRENT_DATE + 10, CURRENT_DATE + 40, 'CANCELADO', (SELECT id FROM usuario WHERE correo = 'matias.herrera@uni.edu.pe'), (SELECT id FROM habitacion WHERE direccion = 'Calle Federico Gallese 1660, San Miguel, Lima')),
(CURRENT_DATE + 5, CURRENT_DATE + 35, 'CANCELADO', (SELECT id FROM usuario WHERE correo = 'fernanda.ruiz@unmsm.edu.pe'), (SELECT id FROM habitacion WHERE direccion = 'Av. Caminos del Inca 1450, Santiago de Surco, Lima'));

-- ---------- Calificaciones (sobre las reservas confirmadas 1 a 8) ----------
INSERT INTO calificacion (puntuacion, descripcion, autor_id, receptor_id, reserva_id)
SELECT v.puntuacion, v.descripcion, r.estudiante_id, r.habitacion_id, r.id
FROM (VALUES
    (1, 5, 'Muy buena ubicación, cerca de la universidad y la dueña fue muy amable.'),
    (2, 4, 'Cuarto cómodo y limpio, el internet a veces falla.'),
    (3, 5, 'Excelente, tal cual las fotos. La recomiendo.'),
    (4, 3, 'Cumple, pero es un poco ruidosa en las noches.'),
    (5, 4, 'Buena relación calidad-precio y buen transporte cerca.'),
    (6, 5, 'Me quedé todo el ciclo, todo impecable.'),
    (7, 4, 'Zona tranquila y segura, el cuarto es algo pequeño.'),
    (8, 5, 'Muy iluminado y el arrendador responde rápido.')
) AS v(reserva_id, puntuacion, descripcion)
JOIN reserva r ON r.id = v.reserva_id;

-- ---------- Estadísticas de los arrendadores (en la app las recalculan los eventos; aquí se cargan por SQL) ----------
UPDATE arrendador a SET
    puntaje_promedio = COALESCE((SELECT ROUND(AVG(c.puntuacion), 1) FROM calificacion c
                                 JOIN habitacion h ON h.id = c.receptor_id WHERE h.arrendador_id = a.id), 0),
    total_calificaciones = (SELECT COUNT(*) FROM calificacion c
                            JOIN habitacion h ON h.id = c.receptor_id WHERE h.arrendador_id = a.id);

-- ---------- Fotos de perfil y DNI de demo ----------
-- Pedro Salas y Renato Salazar quedan sin foto para mostrar el avatar con la inicial
UPDATE usuario u SET foto_perfil = v.url
FROM (VALUES
    ('rosa.quispe@muvu.com', 'https://randomuser.me/api/portraits/women/44.jpg'),
    ('carlos.mendoza@muvu.com', 'https://randomuser.me/api/portraits/men/32.jpg'),
    ('lucia.fernandez@muvu.com', 'https://randomuser.me/api/portraits/women/68.jpg'),
    ('jorge.paredes@muvu.com', 'https://randomuser.me/api/portraits/men/45.jpg'),
    ('maria.torres@muvu.com', 'https://randomuser.me/api/portraits/women/65.jpg'),
    ('ana.ramos@utec.edu.pe', 'https://randomuser.me/api/portraits/women/26.jpg'),
    ('diego.chavez@utec.edu.pe', 'https://randomuser.me/api/portraits/men/22.jpg'),
    ('valeria.nunez@pucp.edu.pe', 'https://randomuser.me/api/portraits/women/31.jpg'),
    ('sebastian.torres@pucp.edu.pe', 'https://randomuser.me/api/portraits/men/36.jpg'),
    ('camila.flores@uni.edu.pe', 'https://randomuser.me/api/portraits/women/52.jpg'),
    ('matias.herrera@uni.edu.pe', 'https://randomuser.me/api/portraits/men/18.jpg'),
    ('fernanda.ruiz@unmsm.edu.pe', 'https://randomuser.me/api/portraits/women/90.jpg')
) AS v(correo, url)
WHERE u.correo = v.correo;

-- El DNI de demo era un link que no existía: ahora es una imagen de prueba que sí carga
UPDATE arrendador SET dni_foto = 'https://placehold.co/640x400/png?text=DNI+de+prueba+%23' || id;

-- ---------- Descripciones de demo ----------
-- Habitaciones: texto armado con su área y distrito; 1 de cada 7 queda sin descripción a propósito
UPDATE habitacion SET descripcion =
    'Habitación de ' || area || ' m² en ' || split_part(direccion, ', ', 2) || '. '
    || CASE WHEN area >= 18 THEN 'Amplia, con baño propio, closet empotrado y buena luz natural. '
            ELSE 'Acogedora, con escritorio de estudio, closet y ventana a la calle. ' END
    || CASE id % 4
        WHEN 0 THEN 'Incluye wifi, agua y luz. Zona tranquila, a pocos minutos de paraderos y supermercados.'
        WHEN 1 THEN 'Wifi de fibra óptica, cocina compartida equipada y lavandería en el edificio.'
        WHEN 2 THEN 'Ingreso independiente y servicios incluidos. Ideal para estudiantes: no se permiten fiestas.'
        ELSE 'Edificio con vigilancia 24 h, ascensor y áreas comunes. Se aceptan mascotas pequeñas.' END
WHERE id % 7 <> 0;

-- "Sobre mí" de algunos estudiantes (el resto queda vacío para ver el caso sin descripción)
UPDATE estudiante e SET descripcion = v.texto
FROM usuario u, (VALUES
    ('ana.ramos@utec.edu.pe', 'Estudio Ingeniería de la Computación en UTEC (5.º ciclo). Soy ordenada, no fumo y paso la mayor parte del día en la universidad.'),
    ('diego.chavez@utec.edu.pe', 'Estudiante de Bioingeniería. Vengo de Arequipa, me gusta cocinar y correr por las mañanas. Busco un lugar tranquilo para estudiar.'),
    ('valeria.nunez@pucp.edu.pe', 'Estudio Arquitectura en la PUCP. Trabajo en maquetas, así que valoro tener un escritorio amplio. Respetuosa con los horarios.'),
    ('camila.flores@uni.edu.pe', 'Ingeniería Civil en la UNI. Soy de Trujillo y es mi primer año en Lima. Tranquila y responsable con los pagos.'),
    ('sebastian.torres@pucp.edu.pe', 'Estudiante de Economía. Practico básquet y paso las tardes en la biblioteca. Sin mascotas.')
) AS v(correo, texto)
WHERE u.id = e.id AND u.correo = v.correo;

-- Mensajes de las solicitudes: el arrendador los lee antes de confirmar
UPDATE reserva r SET mensaje = v.texto
FROM usuario u, habitacion h, (VALUES
    ('ana.ramos@utec.edu.pe', 'Av. Grau 1120 Dpto. 302, Barranco, Lima', 'Hola, soy Ana de UTEC. Me interesa la habitación para el próximo ciclo. ¿El precio incluye wifi? ¿Podría visitarla esta semana?'),
    ('valeria.nunez@pucp.edu.pe', 'Av. Universitaria 1020 Dpto. 803, San Miguel, Lima', 'Buenas tardes, estudio en la PUCP y la habitación me queda a pocas cuadras. ¿Hay espacio para un escritorio grande?'),
    ('camila.flores@uni.edu.pe', 'Calle Los Álamos 1150, Santiago de Surco, Lima', 'Hola, es mi primer año en Lima. ¿Es posible ingresar unos días antes de la fecha de inicio?'),
    ('sebastian.torres@pucp.edu.pe', 'Av. Javier Prado Este 1700 Dpto. 603, La Molina, Lima', 'Hola, busco una habitación tranquila para todo el semestre. Puedo pagar el primer mes por adelantado.'),
    ('diego.chavez@utec.edu.pe', 'Jr. Colina 940 Dpto. 504, Barranco, Lima', 'Hola Rosa, soy Diego de UTEC. Me encantó la habitación, ¿podemos coordinar la entrega de llaves?')
) AS v(correo, direccion, texto)
WHERE u.id = r.estudiante_id AND h.id = r.habitacion_id
  AND u.correo = v.correo AND h.direccion = v.direccion AND r.fecha_inicio > CURRENT_DATE;
