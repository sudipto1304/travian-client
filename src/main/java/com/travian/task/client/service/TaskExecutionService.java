package com.travian.task.client.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.travian.task.client.config.ServiceClient;
import com.travian.task.client.request.AccountInfoRequest;
import com.travian.task.client.request.GameWorld;
import com.travian.task.client.request.VillageInfoRequest;
import com.travian.task.client.response.AccountInfoResponse;
import com.travian.task.client.response.Adventure;
import com.travian.task.client.response.Fields;
import com.travian.task.client.response.Resource;
import com.travian.task.client.response.Status;
import com.travian.task.client.response.Task;
import com.travian.task.client.response.TaskType;
import com.travian.task.client.response.Village;
import com.travian.task.client.util.AccountUtils;
import com.travian.task.client.util.BaseProfile;

@Service
public class TaskExecutionService {

	private static final Logger Log = LoggerFactory.getLogger(TaskExecutionService.class);

	@Autowired
	private ServiceClient client;

	@Autowired
	private AsyncService service;

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
						GameWorld accountInfoRequest = new GameWorld();
						accountInfoRequest.setCookies(cookies);
						accountInfoRequest.setHost(request.getHost());
						accountInfoRequest.setUserId(request.getUserId());
						accountResponse = client.getAccountInfo(accountInfoRequest);
					}
					executeTaskList(cookies, accountResponse, request.getHost(), request.getUserId());

					Thread.sleep(2000 * 60);
				} catch (Exception e) {
					if (Log.isErrorEnabled())
						Log.error("", e);
				}
			} else {
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
		// 1. check for pending adventure
		this.initiateAdventure(cookies, accountResponse, host, userId);
		// 2. get village info
		List<String> villageList = accountResponse.getVillages().stream().map(p -> p.getLink())
				.collect(Collectors.toList());
		Map<String, Task> tasks = new HashMap<String, Task>();
		villageList.forEach(e->{
			try {
				tasks.put(e.substring(e.indexOf("=")+1, e.length() - 1), service.getTask(e.substring(e.indexOf("=")+1, e.length() - 1)).get());
			} catch (InterruptedException e1) {
				if(Log.isErrorEnabled())
					Log.error("", e1);
			} catch (ExecutionException e1) {
				if(Log.isErrorEnabled())
					Log.error("", e1);
			}
		});
		
		List<Village> villages = this.getVillageList(cookies, villageList, host, userId);
		if(!tasks.isEmpty()) {
			this.findAndExecuteTask(villages, tasks);
		}
		

	}

	private void initiateAdventure(Map<String, String> cookies, AccountInfoResponse accountResponse, String host,
			String userId) {
		if (Log.isInfoEnabled())
			Log.info("Pending adventure::" + accountResponse.getPendingAdventure());
		if (accountResponse.getPendingAdventure() > 0 && "in home village".equals(accountResponse.getHeroStatus())) {
			if (Log.isInfoEnabled())
				Log.info("Pending adventure count is ::" + accountResponse.getPendingAdventure()
						+ "--Hero is in home::Initiating adventure");
			GameWorld adventureRequest = new GameWorld();
			adventureRequest.setCookies(cookies);
			adventureRequest.setHost(host);
			adventureRequest.setUserId(userId);
			List<Adventure> adventures = client.getAdventures(adventureRequest);
			Status status = AccountUtils.initiateAdventure(adventures, cookies, host, client);
			if ("SUCCESS".equals(status.getStatus())) {
				if (Log.isInfoEnabled())
					Log.info("Adventure initiated");
			}
		}
	}

	private List<Village> getVillageList(Map<String, String> cookies, List<String> villageList, String host,
			String userId) {
		VillageInfoRequest villageInfoRequest = new VillageInfoRequest();
		villageInfoRequest.setCookies(cookies);
		villageInfoRequest.setHost(host);
		villageInfoRequest.setUserId(userId);
		villageInfoRequest.setLink(villageList);
		if (Log.isInfoEnabled())
			Log.info("No of village:::" + villageList.size() + ":::villages link:::" + villageList);
		List<Village> villages =  client.getVillageInfo(villageInfoRequest);
		if (Log.isInfoEnabled())
			Log.info("Number of village:::" + villages.size());
		return villages;
	}
	
	private void findAndExecuteTask(List<Village> villages, Map<String, Task> tasks) {
		villages.forEach(e->{
			Task task = tasks.get(e.getVillageId());
			if(Log.isInfoEnabled())
				Log.info("Task to be executed:::"+task);
			if(task.getTaskType()==TaskType.RESOURCE_UPDATE) {
					Resource resource = e.getResource();
					Fields field = searchResourceField(task.getResourceId(), resource.getFields());
			}
		});
	}
	
	private Fields searchResourceField(int id, List<Fields> fields) {
		int start=0;
		int end = fields.size()-1;
		int mid  = start+(end-1)/2;
		
		while(end>=start) {
			if(fields.get(mid).getId()==id) {
				return fields.get(mid);
			}else if(fields.get(mid).getId()>id) {
				end = mid-1;
			}else {
				start = mid+1;
			}
		}
		return null;
	}

}
