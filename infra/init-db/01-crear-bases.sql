-- =====================================================================
--  Teleticket - creacion de las bases de datos
--  Fase 1
-- =====================================================================
--
--  PostgreSQL ejecuta automaticamente todo archivo .sql que encuentre
--  en /docker-entrypoint-initdb.d, PERO solo la primera vez, cuando
--  el volumen de datos esta vacio.
--
--  Si cambias este archivo y quieres que vuelva a ejecutarse:
--      docker compose down -v
--      docker compose up -d
--  (ojo: '-v' borra TODOS los datos)
--
--  Patron aplicado: "database per service" (seccion 7 del documento
--  de arquitectura). Cada microservicio es dueno de sus tablas y
--  ningun otro las toca. Si un servicio necesita un dato ajeno, lo
--  pide por Feign o lo recibe en un mensaje de RabbitMQ.
-- =====================================================================

-- Fase 4 - registro de compradores y personal con su rol
CREATE DATABASE db_usuarios       OWNER teleticket;

-- Fase 3 - eventos, zonas, precios, preventas, hora y reglas de venta
CREATE DATABASE db_eventos        OWNER teleticket;

-- Fase 7 - sala de espera, sorteo de turnos y pases de compra
CREATE DATABASE db_cola           OWNER teleticket;

-- Fase 6 - stock por zona y reservas temporales
CREATE DATABASE db_inventario     OWNER teleticket;

-- Fase 8 - ordenes de compra y coordinacion de la saga
CREATE DATABASE db_ordenes        OWNER teleticket;

-- Fase 9 - cobros y reembolsos (pasarela simulada)
CREATE DATABASE db_pagos          OWNER teleticket;

-- Fase 10 - emision, nominacion y validacion de e-tickets
CREATE DATABASE db_tickets        OWNER teleticket;

-- Fase 13 - correos enviados al comprador
CREATE DATABASE db_notificaciones OWNER teleticket;

-- Fase 17 (opcional) - libro de reclamaciones y control de plazos
CREATE DATABASE db_reclamos       OWNER teleticket;


-- ---------------------------------------------------------------------
-- Comentarios descriptivos: aparecen al ejecutar '\l+' en psql y
-- sirven para recordar de quien es cada base.
-- ---------------------------------------------------------------------
COMMENT ON DATABASE db_usuarios       IS 'ms-usuarios :8081';
COMMENT ON DATABASE db_eventos        IS 'ms-eventos :8082';
COMMENT ON DATABASE db_cola           IS 'ms-cola :8083';
COMMENT ON DATABASE db_inventario     IS 'ms-inventario :8084';
COMMENT ON DATABASE db_ordenes        IS 'ms-ordenes :8085';
COMMENT ON DATABASE db_pagos          IS 'ms-pagos :8086';
COMMENT ON DATABASE db_tickets        IS 'ms-tickets :8087';
COMMENT ON DATABASE db_notificaciones IS 'ms-notificaciones :8088';
COMMENT ON DATABASE db_reclamos       IS 'ms-reclamos :8089 (opcional)';
