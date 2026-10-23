-- =====================================================================
--  ms-cola - esquema de db_cola
--  Fase 7
-- =====================================================================
--
--  Spring ejecuta este archivo al arrancar el servicio
--  (spring.sql.init.mode=always). Usa CREATE TABLE IF NOT EXISTS, asi
--  que repetirlo no falla ni borra datos.
--
--  Tablas segun la seccion 7.1 del documento de arquitectura.
-- =====================================================================

-- ---------------------------------------------------------------------
-- venta_evento: copia local de lo que ms-cola necesita de cada evento.
--
-- Llega en el mensaje evento.publicado. ms-cola NUNCA llama a ms-eventos:
-- es el servicio que recibe la avalancha y no puede depender de otro en
-- el peor momento.
--
-- estado_sorteo pasa de PENDIENTE a REALIZADO UNA sola vez y nunca vuelve
-- atras: es lo que garantiza que el sorteo es unico (PRB-02).
--
-- ultima_posicion es el contador de posiciones. Despues del sorteo, quien
-- llega tarde toma ultima_posicion + 1 (RN-02). Al ser un solo UPDATE,
-- dos llegadas simultaneas nunca reciben la misma posicion.
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- turno: el lugar de una persona en la sala de espera de un evento.
--
-- Estados:  EN_SALA   entro, pero el sorteo aun no ocurre (sin posicion)
--           EN_COLA   ya tiene posicion y espera su ciclo
--           HABILITADO  tiene un pase de compra
--           EXPIRADO  su pase vencio sin usarse
--
-- UNIQUE(evento_id, usuario_id): una persona, un turno por evento. Es lo
-- que hace idempotente el ingreso (RN-04): volver a entrar, o hacerlo desde
-- otro dispositivo, devuelve el mismo turno.
--
-- UNIQUE(evento_id, posicion): no hay dos personas con la misma posicion.
-- (Varios NULL si caben: EN_SALA aun no tiene posicion.)
--
-- El ultimo CHECK impone que solo EN_SALA no tiene posicion.
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- pase_compra: el permiso de un solo uso para comprar (PRB-03).
--
-- El codigo es un UUID aleatorio: no se puede adivinar ni construir un
-- enlace de compra directa. UNIQUE(turno_id): un turno, un pase.
--
-- Estados: VIGENTE -> USADO (lo consumio ms-ordenes)
--          VIGENTE -> VENCIDO (paso su expira_en sin usarse)
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- mensaje_procesado: los mensajes que este servicio ya atendio, para
-- descartar los repetidos (misma tabla que en ms-inventario).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mensaje_procesado (
    evento_id     UUID        PRIMARY KEY,
    procesado_en  TIMESTAMPTZ NOT NULL
);
