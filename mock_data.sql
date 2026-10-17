-- ========================================================
-- Script de Datos de Prueba para HomeFixer
-- Ejecutar en el SQL Editor de Supabase
-- ========================================================

-- NOTA: Como la estrategia es JOINED, primero creamos el registro en la tabla padre (usuarios)
-- y luego el registro en la tabla hija correspondiente (clientes o tecnicos) usando el MISMO id.

-- 1. Insertamos un Cliente (Juan Pérez)
INSERT INTO usuarios (id, nombre, email, telefono, password) 
VALUES (101, 'Juan Pérez', 'juan.perez@homefixer.cl', '+56911112222', 'hashed_pass_123')
ON CONFLICT (id) DO NOTHING;

INSERT INTO clientes (id) 
VALUES (101)
ON CONFLICT (id) DO NOTHING;


-- 2. Insertamos un Técnico (Ana Gómez - Otros)
INSERT INTO usuarios (id, nombre, email, telefono, password) 
VALUES (102, 'Ana Gómez', 'ana.gomez@homefixer.cl', '+56933334444', 'hashed_pass_123')
ON CONFLICT (id) DO NOTHING;

INSERT INTO tecnicos (id, validado, calificacion_promedio, nivel_reputacion, especialidad, precio_base) 
VALUES (102, true, 4.8, 'EXPERTO', 'otros', 25000)
ON CONFLICT (id) DO NOTHING;


-- 3. Insertamos un Técnico Nuevo (Carlos Ruiz - Electricidad)
INSERT INTO usuarios (id, nombre, email, telefono, password) 
VALUES (103, 'Carlos Ruiz', 'carlos.ruiz@homefixer.cl', '+56955556666', 'hashed_pass_123')
ON CONFLICT (id) DO NOTHING;

INSERT INTO tecnicos (id, validado, calificacion_promedio, nivel_reputacion, especialidad, precio_base) 
VALUES (103, true, 4.2, 'BUENO', 'electricidad', 30000)
ON CONFLICT (id) DO NOTHING;

-- 5. Insertamos otro de Otros
INSERT INTO usuarios (id, nombre, email, telefono, password) 
VALUES (104, 'Pedro Soto', 'pedro.soto@homefixer.cl', '+56955558888', 'hashed_pass_123')
ON CONFLICT (id) DO NOTHING;

INSERT INTO tecnicos (id, validado, calificacion_promedio, nivel_reputacion, especialidad, precio_base) 
VALUES (104, true, 5.0, 'EXPERTO', 'otros', 28000)
ON CONFLICT (id) DO NOTHING;


-- 4. Ajustar la secuencia de PostgreSQL para que no falle al crear nuevos usuarios por ID repetido
SELECT setval('usuarios_id_seq', (SELECT MAX(id) FROM usuarios));
