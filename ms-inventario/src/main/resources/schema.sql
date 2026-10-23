CREATE TABLE IF NOT EXISTS stock_zona (
    zona_id     UUID    PRIMARY KEY,
    evento_id   UUID    NOT NULL,
    total       INTEGER NOT NULL,
    disponible  INTEGER NOT NULL,
    version     BIGINT  NOT NULL DEFAULT 0,

    CONSTRAINT ck_stock_total       CHECK (total > 0),
    CONSTRAINT ck_stock_disponible  CHECK (disponible >= 0),
    CONSTRAINT ck_stock_tope        CHECK (disponible <= total)
);

CREATE INDEX IF NOT EXISTS ix_stock_evento ON stock_zona (evento_id);

CREATE TABLE IF NOT EXISTS reserva (
    id         UUID         PRIMARY KEY,
    orden_id   UUID         NOT NULL,
    zona_id    UUID         NOT NULL,
    cantidad   INTEGER      NOT NULL,
    estado     VARCHAR(20)  NOT NULL,
    vence_en   TIMESTAMPTZ  NOT NULL,
    creada_en  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_reserva_zona     FOREIGN KEY (zona_id) REFERENCES stock_zona (zona_id),
    CONSTRAINT uq_reserva_orden    UNIQUE (orden_id),
    CONSTRAINT ck_reserva_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_reserva_estado   CHECK (estado IN ('ACTIVA', 'EN_PAGO', 'CONFIRMADA', 'LIBERADA'))
);

CREATE INDEX IF NOT EXISTS ix_reserva_zona ON reserva (zona_id);

CREATE TABLE IF NOT EXISTS mensaje_procesado (
    evento_id     UUID        PRIMARY KEY,
    procesado_en  TIMESTAMPTZ NOT NULL
);
