-- =====================================================================
--  ms-ordenes - esquema de db_ordenes
--  Fase 8
-- =====================================================================
--
--  Spring ejecuta este archivo al arrancar el servicio
--  (spring.sql.init.mode=always). Usa CREATE TABLE IF NOT EXISTS, asi
--  que repetirlo no falla ni borra datos.
--
--  Tablas segun la seccion 7.1 del documento de arquitectura.
-- =====================================================================

-- ---------------------------------------------------------------------
-- orden: una compra, desde que se reserva hasta que se emiten las entradas.
--
-- Estados (seccion 8.5 del diseno):
--   RESERVADA -> PENDIENTE_PAGO -> PAGADA -> EMITIDA
--   y los finales EXPIRADA (reserva vencida) y REEMBOLSADA.
-- Esta fase solo crea ordenes RESERVADAS; el resto lo mueven las fases 9 a 11.
--
-- UNIQUE(pase_codigo): un pase, una orden. Es el candado de PRB-03 del lado de
-- ms-ordenes (ms-cola tiene el suyo: un pase solo se consume una vez).
--
-- 'codigo' es el numero legible que ve el comprador. UNIQUE para que no se repita.
--
-- precio_unitario se COPIA aqui y no se consulta despues: si el operador cambiara
-- el precio de la zona, las ordenes ya creadas conservan el que vio el comprador.
--
-- vence_en es hasta cuando puede pagar: viene de la reserva de ms-inventario.
--
-- reserva_id y zona_id NO tienen FK: son datos de otros servicios y otras bases.
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- orden_historial: auditoria de cada cambio de estado de una orden.
--
-- Solo se inserta, nunca se modifica ni se borra. La primera fila de cada orden
-- tiene estado_anterior NULL (nacio). Responde, en una reclamacion, a la pregunta
-- "que le paso a mi orden y cuando" (PRB-07).
-- ---------------------------------------------------------------------
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
