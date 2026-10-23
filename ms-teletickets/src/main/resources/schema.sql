-- =====================================================================
--  ms-tickets - esquema de db_tickets
--  Fase 10
-- =====================================================================
--
--  Spring ejecuta este archivo al arrancar el servicio
--  (spring.sql.init.mode=always). Usa CREATE TABLE IF NOT EXISTS, asi
--  que repetirlo no falla ni borra datos.
--
--  Tablas segun la seccion 7.1 del documento de arquitectura.
-- =====================================================================

-- ---------------------------------------------------------------------
-- ticket: una entrada firmada.
--
-- Estados: EMITIDO -> USADO (fase 14, al validarla en la puerta) y
-- EMITIDO -> ANULADO (fase 11, si la orden se reembolsa).
--
-- UNIQUE(codigo): el UUID que va en el QR no se repite nunca.
--
-- UNIQUE(orden_id, numero): ES la idempotencia de la emision. Las entradas
-- de una orden son la 1, la 2... la N. Si orden.pagada llega dos veces, el
-- segundo INSERT de la entrada 1 choca con esta restriccion y no hace nada:
-- no hay entradas duplicadas aunque el mensaje se repita.
--
-- firma = HMAC-SHA256(codigo + evento_id, clave secreta), en hexadecimal.
-- Se guarda para poder comparar, pero lo que da seguridad es que nadie sin la
-- clave puede calcular una firma valida.
--
-- condiciones_ingreso se COPIA del evento en el momento de emitir (PRB-06):
-- lo que se informo en la compra es lo que se valida en la puerta, aunque el
-- evento cambiara despues.
--
-- usuario_id: de quien es la entrada (la fase 14 lista "mis entradas").
-- dni_nominado y nombre_nominado quedan vacios hasta la nominacion (fase 14).
--
-- orden_id, evento_id, zona_id y usuario_id NO tienen FK: son datos de otros
-- servicios y otras bases.
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- validacion: cada vez que alguien escanea una entrada en la puerta.
--
-- La llena la fase 14. Se crea ahora porque el esquema del servicio es uno
-- solo. Registra TAMBIEN los intentos rechazados (entrada falsa, repetida):
-- por eso ticket_id puede ser NULL, ya que un codigo falsificado no
-- corresponde a ninguna entrada.
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- mensaje_procesado: los mensajes orden.pagada que ya se emitieron y anunciaron.
--
-- Se anota DESPUES de publicar tickets.emitidos (no junto con las entradas):
-- si el servicio se cae entre guardar y publicar, el mensaje se reentrega, no
-- aparece aqui, y se vuelve a anunciar sin duplicar entradas (eso lo impide
-- UNIQUE(orden_id, numero)).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mensaje_procesado (
    evento_id     UUID        PRIMARY KEY,
    procesado_en  TIMESTAMPTZ NOT NULL
);
