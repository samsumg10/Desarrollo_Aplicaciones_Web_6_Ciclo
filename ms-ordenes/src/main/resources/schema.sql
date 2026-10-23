CREATE TABLE IF NOT EXISTS orden (
    id               UUID          PRIMARY KEY,
    codigo           VARCHAR(20)   NOT NULL,
    usuario_id       UUID          NOT NULL,
    evento_id        UUID          NOT NULL,
    zona_id          UUID          NOT NULL,
    cantidad         INTEGER       NOT NULL,
    precio_unitario  NUMERIC(10,2) NOT NULL,
    total            NUMERIC(10,2) NOT NULL,
    estado           VARCHAR(20)   NOT NULL,
    pase_codigo      UUID          NOT NULL,
    reserva_id       UUID          NOT NULL,
    vence_en         TIMESTAMPTZ   NOT NULL,
    creada_en        TIMESTAMPTZ   NOT NULL,

    CONSTRAINT uq_orden_codigo    UNIQUE (codigo),
    CONSTRAINT uq_orden_pase      UNIQUE (pase_codigo),
    CONSTRAINT ck_orden_cantidad  CHECK (cantidad > 0),
    CONSTRAINT ck_orden_precio    CHECK (precio_unitario >= 0),
    CONSTRAINT ck_orden_total     CHECK (total = precio_unitario * cantidad),
    CONSTRAINT ck_orden_estado    CHECK (estado IN ('RESERVADA', 'PENDIENTE_PAGO', 'PAGADA',
                                                    'EMITIDA', 'EXPIRADA', 'REEMBOLSADA'))
);

CREATE INDEX IF NOT EXISTS ix_orden_usuario ON orden (usuario_id, creada_en DESC);

CREATE TABLE IF NOT EXISTS orden_historial (
    id               BIGSERIAL    PRIMARY KEY,
    orden_id         UUID         NOT NULL,
    estado_anterior  VARCHAR(20),
    estado_nuevo     VARCHAR(20)  NOT NULL,
    motivo           VARCHAR(300) NOT NULL,
    fecha            TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_historial_orden FOREIGN KEY (orden_id) REFERENCES orden (id)
);

CREATE INDEX IF NOT EXISTS ix_historial_orden ON orden_historial (orden_id, fecha);
