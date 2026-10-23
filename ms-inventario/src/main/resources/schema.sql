-- =====================================================================
--  ms-inventario - esquema de db_inventario
--  Fase 6
-- =====================================================================
--
--  Spring ejecuta este archivo al arrancar el servicio
--  (spring.sql.init.mode=always). Usa CREATE TABLE IF NOT EXISTS, asi
--  que repetirlo no falla ni borra datos.
--
--  Tablas segun la seccion 7.1 del documento de arquitectura.
-- =====================================================================

-- ---------------------------------------------------------------------
-- stock_zona: cuantas entradas quedan en cada zona.
--
-- Una fila por zona, creada cuando llega evento.publicado, con
-- disponible = total = aforo de la zona.
--
-- El CHECK (disponible >= 0) es la ULTIMA linea de defensa contra la
-- sobreventa: aunque el codigo tuviera un error, la base de datos
-- rechazaria cualquier UPDATE que dejara el stock en negativo.
-- La primera defensa es el UPDATE condicional del repositorio.
--
-- 'version' sube en cada descuento. No la usa JPA (el descuento es un
-- UPDATE directo, no un save), pero queda como rastro de cuantas veces
-- cambio el stock.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stock_zona (
    zona_id     UUID    PRIMARY KEY,
    evento_id   UUID    NOT NULL,
    total       INTEGER NOT NULL,
    disponible  INTEGER NOT NULL,
    version     BIGINT  NOT NULL DEFAULT 0,

    CONSTRAINT ck_stock_total       CHECK (total > 0),
    CONSTRAINT ck_stock_disponible  CHECK (disponible >= 0),
    -- Nunca puede haber mas entradas disponibles que el aforo (Ley 32415).
    CONSTRAINT ck_stock_tope        CHECK (disponible <= total)
);

CREATE INDEX IF NOT EXISTS ix_stock_evento ON stock_zona (evento_id);

-- ---------------------------------------------------------------------
-- reserva: entradas apartadas para una orden, por tiempo limitado.
--
-- Estados: ACTIVA -> EN_PAGO -> CONFIRMADA, y ACTIVA/CONFIRMADA -> LIBERADA.
--
-- UNIQUE(orden_id): una orden tiene a lo sumo una reserva. Es lo que
-- hace seguro reintentar POST /internal/reservas: el segundo intento
-- no puede apartar el stock dos veces.
--
-- No hay FK hacia ms-ordenes (otra base de datos, otro servicio):
-- orden_id es solo un dato que se guarda.
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- mensaje_procesado: los mensajes que este servicio ya atendio.
--
-- RabbitMQ garantiza "al menos una entrega": en ciertos fallos (por
-- ejemplo, el servicio guarda pero se cae antes de confirmar) el mismo
-- mensaje llega dos veces. El consumidor anota aqui el eventoId (el id
-- del MENSAJE) en la misma transaccion en que guarda su trabajo, y si
-- ya esta, descarta el repetido.
--
-- PRIMARY KEY(evento_id) hace la comprobacion atomica: dos copias del
-- mismo mensaje llegando a la vez no pueden registrarse las dos.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mensaje_procesado (
    evento_id     UUID        PRIMARY KEY,
    procesado_en  TIMESTAMPTZ NOT NULL
);
