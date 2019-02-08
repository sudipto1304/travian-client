package com.travian.task.client.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.travian.task.client.config.ServiceClient;
import com.travian.task.client.request.AccountInfoRequest;
import com.travian.task.client.request.AccountInfoWL;
import com.travian.task.client.request.VillageInfoRequest;
import com.travian.task.client.response.AccountInfoResponse;
import com.travian.task.client.response.Adventure;
import com.travian.task.client.response.Village;
import com.travian.task.client.util.BaseProfile;

@Service
public class TaskExecutionService {

	private static final Logger Log = LoggerFactory.getLogger(TaskExecutionService.class);

	@Autowired
	private ServiceClient client;

	@Async
	public void execute(AccountInfoRequest request) throws InterruptedException {
	

		if (BaseProfile.isExecutionEnable) {
			try {
				Map<String, String> cookies = null;
		
					AccountInfoResponse accountResponse = client.getAccountInfo(request);
					if (Log.isDebugEnabled())
						Log.debug("received AccountInfoResponse:::" + accountResponse);
					cookies = accountResponse.getCookies();
					executeTaskList(cookies, accountResponse, request.getServerUri(), request.getUserId());

				Thread.sleep(1000 * 60);
			} catch (Exception e) {
				if (Log.isErrorEnabled())
					Log.error("", e);
			}
		}

		if (Log.isErrorEnabled()) {
			Log.error("*******************Execution stopped**********************" + System.currentTimeMillis());
		}

	}
	
	public void toggleExecution(boolean enable) {
			BaseProfile.isExecutionEnable = enable;
	}
	
	private void executeTaskList(Map<String, String> cookies, AccountInfoResponse accountResponse, String host, String userId) {
		//1. check for pending adventure
		if(accountResponse.getPendingAdventure()>0) {
			AccountInfoWL adventureRequest = new AccountInfoWL();
			adventureRequest.setCookies(cookies);
			adventureRequest.setServerUri(host);
			adventureRequest.setUserId(userId);
			List<Adventure> adventures = client.getAdventures(adventureRequest);
			
		}
		//2. get village info
		List<String> villageList = accountResponse.getVillages().stream().map(p -> p.getLink())
				.collect(Collectors.toList());
		VillageInfoRequest villageInfoRequest = new VillageInfoRequest();
		villageInfoRequest.setCookies(cookies);
		villageInfoRequest.setHost(host);
		villageInfoRequest.setUserId(userId);
		villageInfoRequest.setLink(villageList);
		if (Log.isDebugEnabled())
			Log.debug("No of village:::" + villageList.size() + ":::villages link:::" + villageList);
		List<Village> villages = client.getVillageInfo(villageInfoRequest);
		
	}

}
