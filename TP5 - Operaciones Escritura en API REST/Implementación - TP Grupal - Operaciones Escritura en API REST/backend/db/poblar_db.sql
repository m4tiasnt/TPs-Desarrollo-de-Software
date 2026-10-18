-- =======================================================================================
-- SCRIPT COMPLETO DE POBLACIÓN DE DATOS (SEED)
-- =======================================================================================

SET search_path TO model;

TRUNCATE TABLE 
    usuarios,
    contactos,
    domicilios,
    clientes,
    condiciones_iva,
    tipos_moneda,
    puntos_venta,
    rubros,
    marcas,
    articulos,
    listas_precio,
    lista_precio_articulo,
    factura_venta,
    factura_venta_detalle
RESTART IDENTITY CASCADE;


-- =======================================================================================
-- SCRIPT COMPLETO DE POBLACIÓN DE DATOS (SEED)
-- =======================================================================================

-- 1. USUARIOS
INSERT INTO usuarios (id, nombre, apellido, usuario, clave) VALUES
(1, 'Juan', 'Perez', 'jperez', '1234'),
(2, 'Maria', 'Garcia', 'mgarcia', '1234'),
(3, 'Carlos', 'Lopez', 'clopez', '1234'),
(4, 'Ana', 'Martinez', 'amartinez', '1234'),
(5, 'Laura', 'Gomez', 'lgomez', '1234'),
(6, 'Pedro', 'Diaz', 'pdiaz', '1234'),
(7, 'Sofia', 'Ruiz', 'sruiz', '1234');

-- 2. CONTACTOS
INSERT INTO contactos (id, celular, email, telefono) VALUES
(1, '2615123456', 'empresademos.a.@mail.com', '4261000'),
(2, '2615231123', 'juangomez@mail.com', '4261137'),
(3, '2615338790', 'democentersrl@mail.com', '4261274'),
(4, '2615446457', 'ferreteriaeltornillo@mail.com', '4261411'),
(5, '2615555555', 'tecnologicamendoza@mail.com', '4261555'),
(6, '2615666666', 'libreriaestudiantil@mail.com', '4261666'),
(7, '2615777777', 'supermercado@mail.com', '4261777'),
(8, '2615888888', 'ropa_deportiva@mail.com', '4261888'),
(9, '2615999999', 'estudio_contable@mail.com', '4261999'),
(10, '2615000000', 'perez_juan_hijo@mail.com', '4262000'),
(11, '2615111222', 'electrohogar@mail.com', '4262111');

-- 3. DOMICILIOS
INSERT INTO domicilios (id, nombre_calle, numero_calle) VALUES
(1, 'Av. San Martín', '456'),
(2, 'Belgrano', '789'),
(3, 'Sarmiento', '1234'),
(4, 'Las Heras', '880'),
(5, 'Aristóbulo del Valle', '150'),
(6, 'San Juan', '1020'),
(7, 'Paso de los Andes', '500'),
(8, 'San Martin', '2000'),
(9, 'Pedro Molina', '150'),
(10, 'Peru', '400'),
(11, 'Colón', '750');

-- 4. CLIENTES
INSERT INTO clientes (id, fecha_alta, fecha_modificacion, cuit_cuil, denominacion, usuario_carga_id, usuario_modificacion_id, contacto_id, domicilio_id) VALUES
(1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '30-12345678-9', 'Empresa Demo S.A.', 1, 1, 1, 1),
(2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '20-12345678-5', 'Juan Gomez', 1, 1, 2, 2),
(3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '20-87654321-0', 'Demo Center SRL', 1, 1, 3, 3),
(4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '27-11111111-2', 'Ferreteria El Tornillo', 1, 1, 4, 4),
(5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '30-99999999-9', 'Tecnologica Mendoza', 2, 2, 5, 5),
(6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '27-88888888-8', 'Libreria Estudiantil', 2, 2, 6, 6),
(7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '30-55555555-5', 'Supermercado Todo', 1, 1, 7, 7),
(8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '30-44444444-4', 'Ropa Deportiva SA', 2, 2, 8, 8),
(9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '27-33333333-3', 'Estudio Contable Diaz', 5, 5, 9, 9),
(10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '20-22222222-2', 'Juan Perez Jr', 6, 6, 10, 10),
(11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '30-11111111-1', 'Electrohogar', 5, 5, 11, 11);

-- 5. CONDICIONES IVA
INSERT INTO condiciones_iva (id, fecha_alta, fecha_modificacion, codigo_afip, denominacion, usuario_carga_id, usuario_modificacion_id) VALUES
(1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'Responsable Inscripto', 1, 1),
(2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 6, 'Monotributista', 1, 1),
(3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 4, 'Consumidor Final', 1, 1),
(4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 5, 'Exento', 1, 1);

-- 6. TIPOS MONEDA
INSERT INTO tipos_moneda (id, fecha_alta, fecha_modificacion, codigo_afip, denominacion, simbolo, usuario_carga_id, usuario_modificacion_id) VALUES
(1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'Peso Argentino', '$', 1, 1),
(2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2, 'Dolar Estadounidense', 'USD', 1, 1),
(3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 3, 'Euro', 'EUR', 1, 1);

-- 7. PUNTOS DE VENTA
INSERT INTO puntos_venta (id, fecha_alta, fecha_modificacion, descripcion, domicilio_comercial, numero, tipo_emision, usuario_carga_id, usuario_modificacion_id) VALUES
(1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Casa Central', 'Av. Siempre Viva 123', 1, 'Electronica', 1, 1),
(2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Sucursal Norte', 'Av. San Martin 456', 2, 'Electronica', 1, 1),
(3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Sucursal Sur', 'Belgrano 789', 3, 'Electronica', 1, 1),
(4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Deposito', 'Ruta 40 Km 12', 5, 'Electronica', 1, 1),
(5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Sucursal Este', 'San Juan 1020', 6, 'Electronica', 2, 2),
(6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Sucursal Oeste', 'Paso de los Andes 500', 7, 'Electronica', 5, 5),
(7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Punto Online', 'Internet', 8, 'Electronica', 6, 6);

-- 8. RUBROS
INSERT INTO rubros (id, fecha_alta, fecha_modificacion, codigo, denominacion, usuario_carga_id, usuario_modificacion_id) VALUES
(1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'Electrónica', 1, 1),
(2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2, 'Muebles', 1, 1),
(3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 3, 'Librería e Insumos', 2, 2),
(4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 4, 'Ferretería', 2, 2),
(5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 5, 'Indumentaria', 1, 1),
(6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 6, 'Alimentos', 5, 5),
(7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 7, 'Computación', 6, 6);

-- 9. MARCAS
INSERT INTO marcas (id, fecha_alta, fecha_modificacion, codigo, denominacion, usuario_carga_id, usuario_modificacion_id) VALUES
(1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'Generica', 1, 1),
(2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2, 'Samsung', 1, 1),
(3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 3, 'LG', 1, 1),
(4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 4, 'Sony', 1, 1),
(5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 5, 'Logitech', 2, 2),
(6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 6, 'Bic', 2, 2),
(7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 7, 'Nike', 1, 1),
(8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 8, 'Adidas', 1, 1),
(9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 9, 'Arcor', 5, 5),
(10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 10, 'HP', 6, 6),
(11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 11, 'Lenovo', 6, 6);

-- 10. ARTÍCULOS
INSERT INTO articulos (id, fecha_alta, fecha_modificacion, codigo, denominacion, usuario_carga_id, usuario_modificacion_id, marca_id, rubro_id) VALUES
(1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ART001', 'Mouse Inalambrico', 1, 1, 1, 1),
(2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ART002', 'TV Samsung 55', 1, 1, 2, 1),
(3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ART003', 'Celular Samsung A54', 1, 1, 2, 1),
(4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ART004', 'Heladera LG', 1, 1, 3, 1),
(5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ART005', 'Monitor LG 27', 1, 1, 3, 1),
(6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ART006', 'Silla de Madera', 1, 1, 1, 2),
(7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ART007', 'Parlante Sony', 1, 1, 4, 1),
(8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ART008', 'Mesa de Luz', 1, 1, NULL, 2),
(9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ART009', 'Teclado Mecanico', 1, 1, 1, 1),
(10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ART010', 'Teclado Inalambrico', 2, 2, 5, 1),
(11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ART011', 'Auriculares Bluetooth', 2, 2, 4, 1),
(12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ART012', 'Caja de Lapiceras', 2, 2, 6, 3),
(13, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ART013', 'Martillo', 2, 2, 1, 4),
(14, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ART014', 'Notebook HP 15', 6, 6, 10, 7),
(15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ART015', 'Notebook Lenovo ThinkPad', 6, 6, 11, 7),
(16, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ART016', 'Zapatillas Running', 1, 1, 7, 5),
(17, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ART017', 'Remera Deportiva', 1, 1, 8, 5),
(18, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ART018', 'Caja de Alfajores', 5, 5, 9, 6),
(19, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ART019', 'Caramelos 1kg', 5, 5, 9, 6),
(20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ART020', 'Impresora HP Laser', 6, 6, 10, 7);

-- 11. LISTAS DE PRECIOS
INSERT INTO listas_precio (id, fecha_alta, fecha_modificacion, codigo, denominacion, usuario_carga_id, usuario_modificacion_id) VALUES
(1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'LP001', 'Lista General', 1, 1),
(2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'LP002', 'Lista Mayorista', 2, 2),
(3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'LP003', 'Lista Empleados', 1, 1);

-- 12. PRECIOS POR ARTÍCULO EN LISTA DE PRECIOS
INSERT INTO lista_precio_articulo (id, fecha_alta, fecha_modificacion, precio_venta, usuario_carga_id, usuario_modificacion_id, articulo_id, lista_precio_id) VALUES
(1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1500.0, 1, 1, 1, 1),
(2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 75000.0, 1, 1, 2, 1),
(3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 30000.0, 1, 1, 3, 1),
(4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 25000.0, 1, 1, 4, 1),
(5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 30000.0, 1, 1, 5, 1),
(6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2500.0, 1, 1, 6, 1),
(7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2000.0, 1, 1, 9, 1),
(8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 4500.0, 2, 2, 10, 1),
(9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 8500.0, 2, 2, 11, 1),
(10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1200.0, 2, 2, 12, 1),
(11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 3500.0, 2, 2, 13, 1),
(12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 65000.0, 2, 2, 2, 2),
(13, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 150000.0, 6, 6, 14, 1),
(14, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 180000.0, 6, 6, 15, 1),
(15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 35000.0, 1, 1, 16, 1),
(16, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 12000.0, 1, 1, 17, 1),
(17, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 6000.0, 5, 5, 18, 1),
(18, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 4500.0, 5, 5, 19, 1),
(19, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 45000.0, 6, 6, 20, 1),
(20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 120000.0, 1, 1, 14, 3),
(21, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 28000.0, 1, 1, 16, 3),
(22, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 4500.0, 1, 1, 18, 3);

-- 13. FACTURAS DE VENTA
INSERT INTO factura_venta (id, fecha_alta, fecha_modificacion, cae, cae_fecha_vencimiento, estado, fecha_anulacion, fecha_emision, importe_cobrado, importe_saldo, importe_total, motivo_rechazo, numero, observaciones, resultado_afip, usuario_carga_id, usuario_modificacion_id, cliente_id, condicion_iva_id, punto_venta_id, tipo_moneda_id) VALUES
(1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 'EMITIDA', NULL, '2026-01-05 10:00:00', 3500.0, 0.0, 3500.0, NULL, 1, NULL, NULL, 1, 1, 1, 1, 1, 1),
(2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 'EMITIDA', NULL, '2026-02-10 10:00:00', 75000.0, 0.0, 75000.0, NULL, 2, NULL, NULL, 1, 1, 1, 1, 1, 1),
(3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 'EMITIDA', NULL, '2026-03-15 10:00:00', 25000.0, 0.0, 25000.0, NULL, 3, NULL, NULL, 1, 1, 1, 1, 1, 1),
(4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 'EMITIDA', NULL, '2026-04-20 10:00:00', 5000.0, 0.0, 5000.0, NULL, 4, NULL, NULL, 1, 1, 2, 1, 2, 1),
(5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 'EMITIDA', NULL, '2026-05-05 10:00:00', 60000.0, 0.0, 60000.0, NULL, 5, NULL, NULL, 1, 1, 3, 1, 2, 1),
(6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 'ANULADA', '2026-06-20 10:00:00', '2026-06-12 10:00:00', 30000.0, 0.0, 30000.0, NULL, 6, NULL, NULL, 1, 1, 1, 1, 4, 1),
(7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 'EMITIDA', NULL, '2026-07-01 10:00:00', 12000.0, 0.0, 12000.0, NULL, 7, NULL, NULL, 1, 1, 4, 1, 1, 1),
(8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 'PENDIENTE', NULL, '2026-08-19 10:00:00', 8000.0, 0.0, 8000.0, NULL, 8, NULL, NULL, 1, 1, 2, 1, 2, 1),
(9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 'EMITIDA', NULL, '2026-09-10 10:00:00', 45000.0, 0.0, 45000.0, NULL, 9, NULL, NULL, 2, 2, 3, 1, 3, 1),
(10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 'EMITIDA', NULL, '2025-12-15 10:00:00', 1500.0, 0.0, 1500.0, NULL, 10, NULL, NULL, 2, 2, 4, 1, 1, 1),
(11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 'EMITIDA', NULL, '2026-10-09 10:00:00', 17000.0, 0.0, 17000.0, NULL, 11, NULL, NULL, 2, 2, 5, 1, 5, 1),
(12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 'EMITIDA', NULL, '2026-10-09 12:00:00', 3500.0, 0.0, 3500.0, NULL, 12, NULL, NULL, 2, 2, 6, 3, 5, 1),
(13, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 'EMITIDA', NULL, '2026-10-10 09:15:00', 150000.0, 0.0, 150000.0, NULL, 13, 'Venta corporativa', NULL, 6, 6, 9, 1, 7, 1),
(14, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 'EMITIDA', NULL, '2026-10-11 11:30:00', 180000.0, 0.0, 180000.0, NULL, 14, NULL, NULL, 6, 6, 11, 1, 6, 1),
(15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 'PENDIENTE', NULL, '2026-10-12 14:00:00', 0.0, 47000.0, 47000.0, NULL, 15, 'A la espera de transferencia', NULL, 1, 1, 8, 1, 7, 1),
(16, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 'ANULADA', '2026-10-13 10:00:00', '2026-10-12 16:45:00', 6000.0, 0.0, 6000.0, NULL, 16, 'Error en pedido', NULL, 5, 5, 7, 1, 6, 1),
(17, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 'EMITIDA', NULL, '2026-10-14 08:20:00', 10500.0, 0.0, 10500.0, NULL, 17, NULL, NULL, 5, 5, 10, 3, 6, 1),
(18, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 'EMITIDA', NULL, '2026-10-15 12:00:00', 45000.0, 0.0, 45000.0, NULL, 18, NULL, NULL, 6, 6, 9, 1, 7, 1),
(19, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 'EMITIDA', NULL, '2026-10-16 10:10:00', 152500.0, 0.0, 152500.0, NULL, 19, 'Venta a empleado', NULL, 1, 1, 2, 3, 1, 1);

-- 14. FACTURAS VENTA DETALLE
INSERT INTO factura_venta_detalle (id, cantidad, descripcion, importe_iva, importe_neto, importe_subtotal, porcentaje_bonificacion, precio_unitario, factura_venta_id, lista_precio_articulo_id) VALUES
(1, 1, 'Mouse Inalambrico', 260.33, 1239.67, 1500.0, 0.0, 1500.0, 1, 1),
(2, 1, 'Teclado Gamer', 347.11, 1652.89, 2000.0, 0.0, 2000.0, 1, 7),
(3, 1, 'TV Samsung 55', 13016.53, 61983.47, 75000.0, 0.0, 75000.0, 2, 2),
(4, 1, 'Heladera LG', 4338.84, 20661.16, 25000.0, 0.0, 25000.0, 3, 4),
(5, 2, 'Silla de Madera', 867.77, 4132.23, 5000.0, 0.0, 2500.0, 4, 6),
(6, 2, 'Celular Samsung A54', 10413.22, 49586.78, 60000.0, 0.0, 30000.0, 5, 3),
(7, 1, 'Monitor LG 27', 5206.61, 24793.39, 30000.0, 0.0, 30000.0, 6, 5),
(8, 8, 'Mouse Inalambrico', 2082.64, 9917.36, 12000.0, 0.0, 1500.0, 7, 1),
(9, 1, 'Silla de Madera', 1388.43, 6611.57, 8000.0, 0.0, 8000.0, 8, 6),
(10, 1, 'TV Samsung 55', 7809.92, 37190.08, 45000.0, 0.0, 45000.0, 9, 2),
(11, 1, 'Mouse Inalambrico', 260.33, 1239.67, 1500.0, 0.0, 1500.0, 10, 1),
(12, 2, 'Auriculares Bluetooth', 2950.41, 14049.59, 17000.0, 0.0, 8500.0, 11, 9),
(13, 1, 'Martillo', 607.44, 2892.56, 3500.0, 0.0, 3500.0, 12, 11),
(14, 1, 'Notebook HP 15', 26033.06, 123966.94, 150000.0, 0.0, 150000.0, 13, 13),
(15, 1, 'Notebook Lenovo ThinkPad', 31239.67, 148760.33, 180000.0, 0.0, 180000.0, 14, 14),
(16, 1, 'Zapatillas Running', 6074.38, 28925.62, 35000.0, 0.0, 35000.0, 15, 15),
(17, 1, 'Remera Deportiva', 2082.64, 9917.36, 12000.0, 0.0, 12000.0, 15, 16),
(18, 1, 'Caja de Alfajores', 1041.32, 4958.68, 6000.0, 0.0, 6000.0, 16, 17),
(19, 1, 'Caja de Alfajores', 1041.32, 4958.68, 6000.0, 0.0, 6000.0, 17, 17),
(20, 1, 'Caramelos 1kg', 780.99, 3719.01, 4500.0, 0.0, 4500.0, 17, 18),
(21, 1, 'Impresora HP Laser', 7809.92, 37190.08, 45000.0, 0.0, 45000.0, 18, 19),
(22, 1, 'Notebook HP 15', 20826.45, 99173.55, 120000.0, 0.0, 120000.0, 19, 20),
(23, 1, 'Zapatillas Running', 4859.50, 23140.50, 28000.0, 0.0, 28000.0, 19, 21),
(24, 1, 'Caja de Alfajores', 780.99, 3719.01, 4500.0, 0.0, 4500.0, 19, 22);

-- FIN DEL SCRIPT