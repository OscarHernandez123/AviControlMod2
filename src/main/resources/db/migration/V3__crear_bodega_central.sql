CREATE TABLE bodega_central (id UUID PRIMARY KEY, capacidad_maxima_kg NUMERIC(19,4) NOT NULL CHECK (capacidad_maxima_kg > 0));
