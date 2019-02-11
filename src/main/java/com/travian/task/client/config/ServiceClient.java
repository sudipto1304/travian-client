package com.travian.task.client.config;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.travian.task.client.request.AccountInfoRequest;
import com.travian.task.client.request.AccountInfoWL;
import com.travian.task.client.request.InitiateAdventureRequest;
import com.travian.task.client.request.VillageInfoRequest;
import com.travian.task.client.response.AccountInfoResponse;
import com.travian.task.client.response.Adventure;
import com.travian.task.client.response.Status;
import com.travian.task.client.response.Village;

@FeignClient(name = "travian-service")
public interface ServiceClient {
	
	
	@RequestMapping(method = RequestMethod.POST, path = "/account/getInfo", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	AccountInfoResponse getAccountInfo(@RequestBody AccountInfoRequest request);
	
	@RequestMapping(method = RequestMethod.POST, path = "/account/getInfoOnly", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	AccountInfoResponse getAccountInfo(@RequestBody AccountInfoWL request);
	
	
	@RequestMapping(method = RequestMethod.GET, path = "/village/getInfo", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	List<Village> getVillageInfo(@RequestBody VillageInfoRequest request);
	
	@RequestMapping(method = RequestMethod.POST, path = "/account/getAdventureList", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	List<Adventure> getAdventures(@RequestBody AccountInfoWL request);
	
	@RequestMapping(method = RequestMethod.POST, path = "/account/hero/sendToAdventure", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	Status initiateAdventure(@RequestBody InitiateAdventureRequest request);

}
