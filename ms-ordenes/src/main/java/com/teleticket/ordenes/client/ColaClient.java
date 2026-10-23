package com.teleticket.ordenes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.teleticket.ordenes.client.dto.ConsumirPaseRequest;
import com.teleticket.ordenes.client.dto.DevolverPaseRequest;
import com.teleticket.ordenes.client.dto.PaseConsumidoResponse;

@FeignClient(name = "ms-cola", url = "${servicios.cola.url}")
public interface ColaClient {

	@PostMapping("/internal/pases/{codigo}/consumir")
	PaseConsumidoResponse consumirPase(@PathVariable("codigo") String codigo, @RequestBody ConsumirPaseRequest req);

	@PostMapping("/internal/pases/{codigo}/devolver")
	PaseConsumidoResponse devolverPase(@PathVariable("codigo") String codigo, @RequestBody DevolverPaseRequest req);
}
