package com.travian.task.client.config;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.travian.task.client.request.AccountInfoRequest;
import com.travian.task.client.response.AccountInfoResponse;

@FeignClient(name = "travian-service")
public interface ServiceClient {
	
	
	@RequestMapping(method = RequestMethod.POST, path = "/account/getInfo", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	AccountInfoResponse getAccountInfo(@RequestBody AccountInfoRequest request);

}
