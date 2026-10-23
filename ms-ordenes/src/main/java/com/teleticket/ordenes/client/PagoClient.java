package com.teleticket.ordenes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.teleticket.ordenes.client.dto.CobrarRequest;
import com.teleticket.ordenes.client.dto.PagoResponse;

@FeignClient(name = "ms-pagos", url = "${servicios.pagos.url}")
public interface PagoClient {

	@PostMapping("/internal/pagos")
	PagoResponse cobrar(@RequestBody CobrarRequest req);
}
