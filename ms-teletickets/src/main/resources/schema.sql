CREATE TABLE IF NOT EXISTS ticket (
    id                   UUID         PRIMARY KEY,
    orden_id             UUID         NOT NULL,
    numero               INTEGER      NOT NULL,
    usuario_id           UUID         NOT NULL,
    evento_id            UUID         NOT NULL,
    zona_id              UUID         NOT NULL,
    codigo               UUID         NOT NULL,
    firma                VARCHAR(64)  NOT NULL,
    condiciones_ingreso  TEXT         NOT NULL,
    dni_nominado         VARCHAR(12),
    nombre_nominado      VARCHAR(150),
    estado               VARCHAR(10)  NOT NULL,
    emitido_en           TIMESTAMPTZ  NOT NULL,
    usado_en             TIMESTAMPTZ,

    CONSTRAINT uq_ticket_codigo       UNIQUE (codigo),
    CONSTRAINT uq_ticket_orden_numero UNIQUE (orden_id, numero),
    CONSTRAINT ck_ticket_numero       CHECK (numero > 0),
    CONSTRAINT ck_ticket_estado       CHECK (estado IN ('EMITIDO', 'USADO', 'ANULADO'))
);

CREATE INDEX IF NOT EXISTS ix_ticket_usuario ON ticket (usuario_id);

CREATE TABLE IF NOT EXISTS validacion (
    id            BIGSERIAL    PRIMARY KEY,
    ticket_id     UUID,
    codigo_leido  VARCHAR(200) NOT NULL,
    resultado     VARCHAR(10)  NOT NULL,
    puerta        VARCHAR(50),
    fecha         TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_validacion_ticket FOREIGN KEY (ticket_id) REFERENCES ticket (id),
    CONSTRAINT ck_validacion_resultado CHECK (resultado IN ('ACEPTADA', 'RECHAZADA'))
);

CREATE INDEX IF NOT EXISTS ix_validacion_ticket ON validacion (ticket_id);

CREATE TABLE IF NOT EXISTS mensaje_procesado (
    evento_id     UUID        PRIMARY KEY,
    procesado_en  TIMESTAMPTZ NOT NULL
);
