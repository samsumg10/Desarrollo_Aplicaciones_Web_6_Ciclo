package com.teleticket.teletickets.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FirmaService {

	private static final String ALGORITMO = "HmacSHA256";

	private static final int LARGO_MINIMO_CLAVE = 32;

	private final SecretKeySpec clave;

	public FirmaService(@Value("${teleticket.tickets.clave-firma}") String claveFirma) {
		if (claveFirma == null || claveFirma.length() < LARGO_MINIMO_CLAVE) {
			throw new IllegalStateException(
					"teleticket.tickets.clave-firma debe tener al menos %d caracteres".formatted(LARGO_MINIMO_CLAVE));
		}
		this.clave = new SecretKeySpec(claveFirma.getBytes(StandardCharsets.UTF_8), ALGORITMO);
	}

	public String firmar(UUID codigo, UUID eventoId) {
		try {
			Mac mac = Mac.getInstance(ALGORITMO);
			mac.init(clave);
			byte[] firma = mac.doFinal((codigo.toString() + eventoId).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(firma);
		} catch (java.security.GeneralSecurityException e) {
			throw new IllegalStateException("No se pudo calcular la firma HMAC", e);
		}
	}

	public boolean esValida(UUID codigo, UUID eventoId, String firma) {
		if (firma == null) {
			return false;
		}
		return MessageDigest.isEqual(firmar(codigo, eventoId).getBytes(StandardCharsets.UTF_8),
				firma.getBytes(StandardCharsets.UTF_8));
	}
}
