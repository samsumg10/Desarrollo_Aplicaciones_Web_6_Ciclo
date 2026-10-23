-- =====================================================================
--  ms-eventos - esquema de db_eventos
--  Fase 3
-- =====================================================================
--
--  Spring ejecuta este archivo al arrancar el servicio
--  (spring.sql.init.mode=always). Usa CREATE TABLE IF NOT EXISTS, asi
--  que repetirlo no falla ni borra datos.
--
--  Tablas segun la seccion 7.1 del documento de arquitectura.
--  Hibernate NO crea tablas: solo valida que coincidan con las
--  entidades Java (spring.jpa.hibernate.ddl-auto=validate).
-- =====================================================================

-- ---------------------------------------------------------------------
-- evento: el espectaculo que se vende.
--
-- Los tres campos que atacan PRB-02 y PRB-06 son inicio_venta,
-- reglas_cola y condiciones_ingreso: una vez publicado el evento ya no
-- se pueden cambiar (RN-01). Eso responde al incidente de 2017, cuando
-- la cola se anuncio para las 6:00 y el sistema reordeno desde las 5:00.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS evento (
    id                   UUID         PRIMARY KEY,
    nombre               VARCHAR(200) NOT NULL,
    fecha                TIMESTAMPTZ  NOT NULL,
    recinto              VARCHAR(200) NOT NULL,
    inicio_venta         TIMESTAMPTZ  NOT NULL,
    estado               VARCHAR(20)  NOT NULL,
    max_por_cliente      INTEGER      NOT NULL,
    condiciones_ingreso  TEXT,
    reglas_cola          TEXT,
    creado_en            TIMESTAMPTZ  NOT NULL,
    publicado_en         TIMESTAMPTZ,

    -- BORRADOR: se puede editar.  EN_VENTA: congelado (RN-01).
    CONSTRAINT ck_evento_estado      CHECK (estado IN ('BORRADOR', 'EN_VENTA', 'FINALIZADO', 'CANCELADO')),
    CONSTRAINT ck_evento_max_cliente CHECK (max_por_cliente BETWEEN 1 AND 20)
);

-- ---------------------------------------------------------------------
-- zona: cada sector del recinto, con su precio y su aforo.
--
-- El aforo de aqui es el que ms-inventario convertira en stock cuando
-- reciba el mensaje evento.publicado (fase 6). Por eso 'aforo' no baja
-- nunca: es el total, no lo disponible.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS zona (
    id         UUID          PRIMARY KEY,
    evento_id  UUID          NOT NULL,
    nombre     VARCHAR(100)  NOT NULL,
    precio     NUMERIC(10,2) NOT NULL,
    aforo      INTEGER       NOT NULL,

    CONSTRAINT fk_zona_evento   FOREIGN KEY (evento_id) REFERENCES evento (id) ON DELETE CASCADE,
    -- No puede haber dos zonas "VIP" en el mismo evento.
    CONSTRAINT uq_zona_nombre   UNIQUE (evento_id, nombre),
    CONSTRAINT ck_zona_precio   CHECK (precio >= 0),
    CONSTRAINT ck_zona_aforo    CHECK (aforo > 0)
);

CREATE INDEX IF NOT EXISTS ix_zona_evento ON zona (evento_id);

-- ---------------------------------------------------------------------
-- preventa: venta anticipada con stock propio, normalmente de un banco.
--
-- Por RN-07 el stock de preventa es independiente por zona y se agota
-- por separado. Se deja la tabla creada desde ahora; los endpoints de
-- preventa no entran en el alcance de esta fase.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS preventa (
    id         UUID          PRIMARY KEY,
    evento_id  UUID          NOT NULL,
    zona_id    UUID          NOT NULL,
    banco      VARCHAR(100)  NOT NULL,
    inicio     TIMESTAMPTZ   NOT NULL,
    fin        TIMESTAMPTZ   NOT NULL,
    descuento  NUMERIC(5,2),
    stock      INTEGER       NOT NULL,

    CONSTRAINT fk_preventa_evento FOREIGN KEY (evento_id) REFERENCES evento (id) ON DELETE CASCADE,
    CONSTRAINT fk_preventa_zona   FOREIGN KEY (zona_id)   REFERENCES zona (id)   ON DELETE CASCADE,
    CONSTRAINT ck_preventa_fechas CHECK (fin > inicio),
    CONSTRAINT ck_preventa_stock  CHECK (stock >= 0)
);

CREATE INDEX IF NOT EXISTS ix_preventa_evento ON preventa (evento_id);
