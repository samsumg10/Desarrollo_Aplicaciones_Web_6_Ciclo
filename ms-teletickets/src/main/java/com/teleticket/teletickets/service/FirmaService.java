package com.teleticket.teletickets.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * La firma digital de las entradas: un sello con clave secreta (PRB-05).
 *
 *   firma = HMAC-SHA256(codigo + eventoId, clave-secreta)
 *
 * Cualquiera puede ver el codigo de una entrada, pero nadie puede calcular una firma valida sin la clave. Una
 * entrada falsificada se detecta al recalcular la firma, antes incluso de consultar la base de datos.
 *
 * Incluir el eventoId en lo firmado impide reutilizar una entrada autentica de un evento en otro.
 */
@Service
public class FirmaService {

	private static final String ALGORITMO = "HmacSHA256";

	/** Una clave corta se adivina por fuerza bruta. HMAC-SHA256 pide al menos tantos bytes como su salida (32). */
	private static final int LARGO_MINIMO_CLAVE = 32;

	private final SecretKeySpec clave;

	public FirmaService(@Value("${teleticket.tickets.clave-firma}") String claveFirma) {
		if (claveFirma == null || claveFirma.length() < LARGO_MINIMO_CLAVE) {
			throw new IllegalStateException(
					"teleticket.tickets.clave-firma debe tener al menos %d caracteres".formatted(LARGO_MINIMO_CLAVE));
		}
		this.clave = new SecretKeySpec(claveFirma.getBytes(StandardCharsets.UTF_8), ALGORITMO);
	}

	/** La firma de la entrada, en hexadecimal (64 caracteres). */
	public String firmar(UUID codigo, UUID eventoId) {
		try {
			Mac mac = Mac.getInstance(ALGORITMO);
			mac.init(clave);
			byte[] firma = mac.doFinal((codigo.toString() + eventoId).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(firma);
		} catch (java.security.GeneralSecurityException e) {
			// HmacSHA256 existe en todo JDK y la clave ya se valido: esto no puede pasar.
			throw new IllegalStateException("No se pudo calcular la firma HMAC", e);
		}
	}

	/** Comprueba una firma sin filtrar, por el tiempo que tarda, cuantos caracteres acerto. */
	public boolean esValida(UUID codigo, UUID eventoId, String firma) {
		if (firma == null) {
			return false;
		}
		return MessageDigest.isEqual(firmar(codigo, eventoId).getBytes(StandardCharsets.UTF_8),
				firma.getBytes(StandardCharsets.UTF_8));
	}
}
