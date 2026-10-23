package com.teleticket.pagos.dto;

/**
 * El resultado de cobrar, y si se hizo un cobro nuevo.
 *
 * @param pago  el pago (aprobado o rechazado)
 * @param nuevo true si esta peticion paso por la pasarela; false si se devolvio el pago que
 *              ya existia (la idempotencia en accion)
 */
public record ResultadoCobro(PagoResponse pago, boolean nuevo) {
}
