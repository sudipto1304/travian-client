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
import com.travian.task.client.response.Status;
import com.travian.task.client.response.Village;
import com.travian.task.client.util.AccountUtils;
import com.travian.task.client.util.BaseProfile;

@Service
public class TaskExecutionService {

	private static final Logger Log = LoggerFactory.getLogger(TaskExecutionService.class);

	@Autowired
	private ServiceClient client;

	@Async
	public void execute(AccountInfoRequest request) throws InterruptedException {
		Map<String, String> cookies = null;

		while (true) {
			if (BaseProfile.isExecutionEnable) {
				try {
					AccountInfoResponse accountResponse = null;
					if (cookies == null) {
						if (Log.isInfoEnabled())
							Log.info("Cookies not present::getting account info with login");
						accountResponse = client.getAccountInfo(request);
						if (Log.isDebugEnabled())
							Log.debug("received AccountInfoResponse:::" + accountResponse);
						cookies = accountResponse.getCookies();
					} else {
						if (Log.isInfoEnabled())
							Log.info("Cookies present::getting account info without login");
						AccountInfoWL accountInfoRequest = new AccountInfoWL();
						accountInfoRequest.setCookies(cookies);
						accountInfoRequest.setServerUri(request.getServerUri());
						accountInfoRequest.setUserId(request.getUserId());
						accountResponse = client.getAccountInfo(accountInfoRequest);
					}
					executeTaskList(cookies, accountResponse, request.getServerUri(), request.getUserId());

					Thread.sleep(2000 * 60);
				} catch (Exception e) {
					if (Log.isErrorEnabled())
						Log.error("", e);
				}
			}else {
				if (Log.isErrorEnabled()) {
					Log.error("::::isExecutionEnable false:::::execution paused" + System.currentTimeMillis());
				}
			}
		}

		

	}

	public void toggleExecution(boolean enable) {
		BaseProfile.isExecutionEnable = enable;
	}

	private void executeTaskList(Map<String, String> cookies, AccountInfoResponse accountResponse, String host,
			String userId) {
		if (Log.isInfoEnabled())
			Log.info("Pending adventure::" + accountResponse.getPendingAdventure());
		// 1. check for pending adventure
		if (accountResponse.getPendingAdventure() > 0 && "in home village".equals(accountResponse.getHeroStatus())) {
			if (Log.isInfoEnabled())
				Log.info("Pending adventure count is ::" + accountResponse.getPendingAdventure()
						+ "--Hero is in home::Initiating adventure");
			AccountInfoWL adventureRequest = new AccountInfoWL();
			adventureRequest.setCookies(cookies);
			adventureRequest.setServerUri(host);
			adventureRequest.setUserId(userId);
			List<Adventure> adventures = client.getAdventures(adventureRequest);
			Status status = AccountUtils.initiateAdventure(adventures, cookies, host, client);
			if ("SUCCESS".equals(status.getStatus())) {
				if (Log.isInfoEnabled())
					Log.info("Adventure initiated");
			}
		}
		// 2. get village info
		List<String> villageList = accountResponse.getVillages().stream().map(p -> p.getLink())
				.collect(Collectors.toList());
		VillageInfoRequest villageInfoRequest = new VillageInfoRequest();
		villageInfoRequest.setCookies(cookies);
		villageInfoRequest.setHost(host);
		villageInfoRequest.setUserId(userId);
		villageInfoRequest.setLink(villageList);
		if (Log.isInfoEnabled())
			Log.info("No of village:::" + villageList.size() + ":::villages link:::" + villageList);
		List<Village> villages = client.getVillageInfo(villageInfoRequest);
		if (Log.isInfoEnabled())
			Log.info("Number of village:::" + villages.size());

	}

}
