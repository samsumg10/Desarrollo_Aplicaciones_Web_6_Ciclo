CREATE TABLE IF NOT EXISTS pago (
    id                   UUID          PRIMARY KEY,
    orden_id             UUID          NOT NULL,
    monto                NUMERIC(10,2) NOT NULL,
    estado               VARCHAR(20)   NOT NULL,
    referencia_pasarela  VARCHAR(40),
    motivo_rechazo       VARCHAR(200),
    tarjeta_ultimos4     VARCHAR(4)    NOT NULL,
    idempotency_key      VARCHAR(64)   NOT NULL,
    intentos             INTEGER       NOT NULL DEFAULT 1,
    creado_en            TIMESTAMPTZ   NOT NULL,
    actualizado_en       TIMESTAMPTZ   NOT NULL,

    CONSTRAINT uq_pago_idempotency  UNIQUE (idempotency_key),
    CONSTRAINT ck_pago_monto        CHECK (monto >= 0),
    CONSTRAINT ck_pago_intentos     CHECK (intentos >= 1),
    CONSTRAINT ck_pago_estado       CHECK (estado IN ('PENDIENTE', 'APROBADO', 'RECHAZADO', 'REEMBOLSADO'))
);

CREATE INDEX IF NOT EXISTS ix_pago_orden ON pago (orden_id);

CREATE TABLE IF NOT EXISTS reembolso (
    id          UUID          PRIMARY KEY,
    pago_id     UUID          NOT NULL,
    monto       NUMERIC(10,2) NOT NULL,
    motivo      VARCHAR(200),
    creado_en   TIMESTAMPTZ   NOT NULL,

    CONSTRAINT fk_reembolso_pago   FOREIGN KEY (pago_id) REFERENCES pago (id),
    CONSTRAINT uq_reembolso_pago   UNIQUE (pago_id),
    CONSTRAINT ck_reembolso_monto  CHECK (monto >= 0)
);
