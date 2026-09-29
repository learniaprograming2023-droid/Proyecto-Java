-- Base de Datos e Inventario
CREATE DATABASE tienda_inventario;

\c tienda_inventario;

CREATE TABLE marcas (
                        id SERIAL PRIMARY KEY,
                        nombre VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE productos (
                           id SERIAL PRIMARY KEY,
                           marca_id INT REFERENCES marcas(id) ON DELETE RESTRICT,
                           referencia VARCHAR(100) UNIQUE NOT NULL,
                           nombre VARCHAR(200) NOT NULL,
                           foto_ruta TEXT
);

CREATE TABLE inventario (
                            id SERIAL PRIMARY KEY,
                            producto_id INT REFERENCES productos(id) ON DELETE CASCADE,
                            talla INT NOT NULL CHECK (talla BETWEEN 30 AND 50),
                            cantidad INT NOT NULL DEFAULT 0 CHECK (cantidad >= 0),
                            UNIQUE(producto_id, talla)
);

CREATE TABLE movimientos (
                             id SERIAL PRIMARY KEY,
                             producto_id INT REFERENCES productos(id) ON DELETE CASCADE,
                             talla INT NOT NULL,
                             tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('ENTRADA', 'SALIDA', 'AJUSTE', 'CAMBIO_DEVOLUCION', 'CAMBIO_ENTREGA')),
                             cantidad INT NOT NULL CHECK (cantidad > 0),
                             stock_anterior INT NOT NULL,
                             stock_nuevo INT NOT NULL,
                             observacion TEXT,
                             fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_productos_ref_nom ON productos(referencia ILIKE, nombre ILIKE);
CREATE INDEX idx_inventario_prod_talla ON inventario(producto_id, talla);
CREATE INDEX idx_movimientos_prod_fecha ON movimientos(producto_id, fecha DESC);

-- Datos iniciales
INSERT INTO marcas (nombre) VALUES ('NIKE'), ('ADIDAS');

INSERT INTO productos (marca_id, referencia, nombre) VALUES
                                                         (1, 'ART: 001-808', 'FOR ONE BLANCO'),
                                                         (1, 'ART: 002 BO4', 'FOR ONE NEGRO'),
                                                         (2, 'ART: BA2092', '3 BANDAS NEGRO / BLANCO');

INSERT INTO inventario (producto_id, talla, cantidad) VALUES
                                                          (1, 35,2),(1,36,3),(1,37,1),(1,39,3),(1,40,2),(1,42,3),(1,44,1),
                                                          (2, 34,1),(2,37,3),(2,38,2),(2,39,1),(2,40,2),(2,42,3),(2,44,2),
                                                          (3, 35,2),(3,36,3),(3,37,2),(3,38,1),(3,40,3),(3,41,2);