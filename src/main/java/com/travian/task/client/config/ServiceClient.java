package com.travian.task.client.config;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.travian.task.client.request.AccountInfoRequest;
import com.travian.task.client.request.GameWorld;
import com.travian.task.client.request.InitiateAdventureRequest;
import com.travian.task.client.request.TroopTrainRequest;
import com.travian.task.client.request.UpgradeRequest;
import com.travian.task.client.request.VillageInfoRequest;
import com.travian.task.client.response.AccountInfoResponse;
import com.travian.task.client.response.Adventure;
import com.travian.task.client.response.Status;
import com.travian.task.client.response.TroopTrainResponse;
import com.travian.task.client.response.Village;

@FeignClient(name = "travian-service")
public interface ServiceClient {
	
	
	@RequestMapping(method = RequestMethod.POST, path = "/travian-service/account/getInfo", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	AccountInfoResponse getAccountInfo(@RequestBody AccountInfoRequest request);
	
	@RequestMapping(method = RequestMethod.POST, path = "/travian-service/account/getInfoOnly", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	AccountInfoResponse getAccountInfo(@RequestBody GameWorld request);
	
	
	@RequestMapping(method = RequestMethod.GET, path = "/travian-service/village/getInfo", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	List<Village> getVillageInfo(@RequestBody VillageInfoRequest request);
	
	@RequestMapping(method = RequestMethod.POST, path = "/travian-service/account/getAdventureList", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	List<Adventure> getAdventures(@RequestBody GameWorld request);
	
	@RequestMapping(method = RequestMethod.POST, path = "/travian-service/account/hero/sendToAdventure", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	Status initiateAdventure(@RequestBody InitiateAdventureRequest request);
	
	@RequestMapping(method = RequestMethod.POST, path = "/travian-service/upgrade/nextLevel", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	Status upgrade(@RequestBody UpgradeRequest request);
	
	@RequestMapping(method = RequestMethod.POST, path = "/travian-service/troop/training", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	TroopTrainResponse trainTroop(@RequestBody TroopTrainRequest request);

}
