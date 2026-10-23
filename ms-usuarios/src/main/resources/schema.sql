CREATE TABLE IF NOT EXISTS usuario (
    id             UUID         PRIMARY KEY,
    dni            VARCHAR(8)   NOT NULL,
    nombres        VARCHAR(150) NOT NULL,
    email          VARCHAR(150) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    rol            VARCHAR(20)  NOT NULL,
    creado_en      TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uq_usuario_dni   UNIQUE (dni),
    CONSTRAINT uq_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_dni   CHECK (dni ~ '^[0-9]{8}$'),
    CONSTRAINT ck_usuario_rol   CHECK (rol IN ('COMPRADOR', 'OPERADOR', 'CONTROLADOR', 'ATENCION'))
);
