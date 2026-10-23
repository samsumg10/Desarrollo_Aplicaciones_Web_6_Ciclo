package com.teleticket.mensajeria;

public interface PublicadorEventos {

	void publicar(String routingKey, SobreMensaje<?> mensaje);
}
