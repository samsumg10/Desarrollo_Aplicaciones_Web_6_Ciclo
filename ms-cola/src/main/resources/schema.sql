CREATE TABLE IF NOT EXISTS venta_evento (
    evento_id            UUID         PRIMARY KEY,
    nombre               VARCHAR(200) NOT NULL,
    inicio_venta         TIMESTAMPTZ  NOT NULL,
    estado_sorteo        VARCHAR(12)  NOT NULL DEFAULT 'PENDIENTE',
    capacidad_por_ciclo  INTEGER      NOT NULL,
    ultima_posicion      INTEGER      NOT NULL DEFAULT 0,
    sorteo_en            TIMESTAMPTZ,
    creado_en            TIMESTAMPTZ  NOT NULL,

    CONSTRAINT ck_venta_sorteo    CHECK (estado_sorteo IN ('PENDIENTE', 'REALIZADO')),
    CONSTRAINT ck_venta_capacidad CHECK (capacidad_por_ciclo > 0),
    CONSTRAINT ck_venta_ultima    CHECK (ultima_posicion >= 0)
);

CREATE TABLE IF NOT EXISTS turno (
    id            UUID         PRIMARY KEY,
    evento_id     UUID         NOT NULL,
    usuario_id    UUID         NOT NULL,
    posicion      INTEGER,
    estado        VARCHAR(12)  NOT NULL,
    ingreso_en    TIMESTAMPTZ  NOT NULL,
    habilitado_en TIMESTAMPTZ,

    CONSTRAINT fk_turno_evento      FOREIGN KEY (evento_id) REFERENCES venta_evento (evento_id),
    CONSTRAINT uq_turno_usuario     UNIQUE (evento_id, usuario_id),
    CONSTRAINT uq_turno_posicion    UNIQUE (evento_id, posicion),
    CONSTRAINT ck_turno_estado      CHECK (estado IN ('EN_SALA', 'EN_COLA', 'HABILITADO', 'EXPIRADO')),
    CONSTRAINT ck_turno_posicion    CHECK (posicion IS NULL OR posicion > 0),
    CONSTRAINT ck_turno_sin_sorteo  CHECK ((estado = 'EN_SALA') = (posicion IS NULL))
);

CREATE INDEX IF NOT EXISTS ix_turno_cola ON turno (evento_id, estado, posicion);

CREATE TABLE IF NOT EXISTS pase_compra (
    codigo      UUID         PRIMARY KEY,
    turno_id    UUID         NOT NULL,
    usuario_id  UUID         NOT NULL,
    evento_id   UUID         NOT NULL,
    expira_en   TIMESTAMPTZ  NOT NULL,
    estado      VARCHAR(10)  NOT NULL,
    creado_en   TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_pase_turno   FOREIGN KEY (turno_id) REFERENCES turno (id),
    CONSTRAINT uq_pase_turno   UNIQUE (turno_id),
    CONSTRAINT ck_pase_estado  CHECK (estado IN ('VIGENTE', 'USADO', 'VENCIDO'))
);

CREATE TABLE IF NOT EXISTS mensaje_procesado (
    evento_id     UUID        PRIMARY KEY,
    procesado_en  TIMESTAMPTZ NOT NULL
);
