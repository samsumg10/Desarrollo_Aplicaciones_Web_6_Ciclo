CREATE TABLE IF NOT EXISTS evento (
    id                   UUID         PRIMARY KEY,
    nombre               VARCHAR(200) NOT NULL,
    fecha                TIMESTAMPTZ  NOT NULL,
    recinto              VARCHAR(200) NOT NULL,
    inicio_venta         TIMESTAMPTZ  NOT NULL,
    estado               VARCHAR(20)  NOT NULL,
    max_por_cliente      INTEGER      NOT NULL,
    condiciones_ingreso  TEXT,
    reglas_cola          TEXT,
    creado_en            TIMESTAMPTZ  NOT NULL,
    publicado_en         TIMESTAMPTZ,

    CONSTRAINT ck_evento_estado      CHECK (estado IN ('BORRADOR', 'EN_VENTA', 'FINALIZADO', 'CANCELADO')),
    CONSTRAINT ck_evento_max_cliente CHECK (max_por_cliente BETWEEN 1 AND 20)
);

CREATE TABLE IF NOT EXISTS zona (
    id         UUID          PRIMARY KEY,
    evento_id  UUID          NOT NULL,
    nombre     VARCHAR(100)  NOT NULL,
    precio     NUMERIC(10,2) NOT NULL,
    aforo      INTEGER       NOT NULL,

    CONSTRAINT fk_zona_evento   FOREIGN KEY (evento_id) REFERENCES evento (id) ON DELETE CASCADE,
    CONSTRAINT uq_zona_nombre   UNIQUE (evento_id, nombre),
    CONSTRAINT ck_zona_precio   CHECK (precio >= 0),
    CONSTRAINT ck_zona_aforo    CHECK (aforo > 0)
);

CREATE INDEX IF NOT EXISTS ix_zona_evento ON zona (evento_id);

CREATE TABLE IF NOT EXISTS preventa (
    id         UUID          PRIMARY KEY,
    evento_id  UUID          NOT NULL,
    zona_id    UUID          NOT NULL,
    banco      VARCHAR(100)  NOT NULL,
    inicio     TIMESTAMPTZ   NOT NULL,
    fin        TIMESTAMPTZ   NOT NULL,
    descuento  NUMERIC(5,2),
    stock      INTEGER       NOT NULL,

    CONSTRAINT fk_preventa_evento FOREIGN KEY (evento_id) REFERENCES evento (id) ON DELETE CASCADE,
    CONSTRAINT fk_preventa_zona   FOREIGN KEY (zona_id)   REFERENCES zona (id)   ON DELETE CASCADE,
    CONSTRAINT ck_preventa_fechas CHECK (fin > inicio),
    CONSTRAINT ck_preventa_stock  CHECK (stock >= 0)
);

CREATE INDEX IF NOT EXISTS ix_preventa_evento ON preventa (evento_id);
