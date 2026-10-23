-- =====================================================================
--  ms-pagos - esquema de db_pagos
--  Fase 9
-- =====================================================================
--
--  Spring ejecuta este archivo al arrancar el servicio
--  (spring.sql.init.mode=always). Usa CREATE TABLE IF NOT EXISTS, asi
--  que repetirlo no falla ni borra datos.
--
--  Tablas segun la seccion 7.1 del documento de arquitectura.
-- =====================================================================

-- ---------------------------------------------------------------------
-- pago: el cobro de una orden. UNA fila por orden.
--
-- idempotency_key UNIQUE es el candado contra el doble cobro: es el ordenId,
-- asi que dos cobros de la misma orden no pueden coexistir ni siquiera si
-- llegan al mismo tiempo. La base de datos lo garantiza aunque el codigo
-- tuviera un error.
--
-- Estados:
--   PENDIENTE   se reservo la clave y se esta cobrando (la pasarela puede tardar)
--   APROBADO    cobrado. Definitivo: repetir el cobro devuelve este mismo pago
--   RECHAZADO   la pasarela dijo no. Se puede reintentar con otra tarjeta: la MISMA fila
--               vuelve a PENDIENTE (por eso 'intentos'), asi sigue habiendo un solo pago por orden
--   REEMBOLSADO lo usara la fase 11; se incluye ahora porque CREATE TABLE IF NOT EXISTS
--               no modifica una tabla que ya existe
--
-- Del numero de tarjeta solo se guardan los ultimos 4 digitos.
--
-- orden_id NO tiene FK: la orden vive en otra base de datos (ms-ordenes).
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- reembolso: la devolucion de un pago (fase 11).
--
-- UNIQUE(pago_id): un pago se devuelve a lo sumo una vez. Es el mismo
-- candado que idempotency_key, aplicado a la devolucion.
-- ---------------------------------------------------------------------
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
