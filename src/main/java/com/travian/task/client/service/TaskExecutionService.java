package com.travian.task.client.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
import com.travian.task.client.request.UpgradeRequest;
import com.travian.task.client.request.VillageInfoRequest;
import com.travian.task.client.response.AccountInfoResponse;
import com.travian.task.client.response.Adventure;
import com.travian.task.client.response.Building;
import com.travian.task.client.response.Fields;
import com.travian.task.client.response.Resource;
import com.travian.task.client.response.Status;
import com.travian.task.client.response.Task;
import com.travian.task.client.response.TaskType;
import com.travian.task.client.response.Village;
import com.travian.task.client.util.AccountUtils;
import com.travian.task.client.util.AppConstant;
import com.travian.task.client.util.BaseProfile;

@Service
public class TaskExecutionService {

	private static final Logger Log = LoggerFactory.getLogger(TaskExecutionService.class);

	@Autowired
	private ServiceClient client;

	@Autowired
	private AsyncService service;
	
	private int errorCount = 0;
	private int pauseCount = 0;

	@Async
	public void execute(AccountInfoRequest request) throws InterruptedException {
		Map<String, String> cookies = null;


		while (true) {
			if (BaseProfile.isExecutionEnable) {
				pauseCount=0; //reset pause count if incremented
				try {
					AccountInfoResponse accountResponse = null;
					if (cookies == null) {
						if (Log.isInfoEnabled())
							Log.info("Cookies not present::getting account info with login");
						accountResponse = client.getAccountInfo(request);
						if (Log.isDebugEnabled())
							Log.debug("received AccountInfoResponse:::" + accountResponse);
						BaseProfile.profile.put("HOST", request.getHost());
						BaseProfile.profile.put("USERID", request.getUserId());
						BaseProfile.profile.put("COOKIES", accountResponse.getCookies());
						cookies = accountResponse.getCookies();
					} else {
						if (Log.isInfoEnabled())
							Log.info("Cookies present::getting account info without login");
						GameWorld accountInfoRequest = new GameWorld();
						accountResponse = client.getAccountInfo(accountInfoRequest);
					}
					executeTaskList(accountResponse);
					Random r = new Random();
					int rand =  r.ints(60, (100 + 1)).limit(1).findFirst().getAsInt();
					if(Log.isInfoEnabled())
						Log.info("Next call in ::"+rand+" sec");
					Thread.sleep(2000 * rand);
				} catch (Exception e) {
					if (Log.isErrorEnabled())
						Log.error("", e);
					errorCount++;
					cookies = null;
					if(errorCount>=10) {
						break;
					}
				}
			} else {
				pauseCount++;
				if (Log.isErrorEnabled()) {
					Log.error("::::isExecutionEnable false:::::execution paused" + System.currentTimeMillis());
				}
				Thread.sleep(1000 * 60);
				if(pauseCount>60) {
					if (Log.isErrorEnabled()) 
						Log.error("Execution is paused more than 60 mins::stopping execution");
					break;
				}
			}
		}

	}

	public void toggleExecution(boolean enable) {
		BaseProfile.isExecutionEnable = enable;
	}

	private void executeTaskList(AccountInfoResponse accountResponse) {
		// 1. check for pending adventure
		this.initiateAdventure(accountResponse);
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
		
		List<Village> villages = this.getVillageList(villageList);
		if(!tasks.isEmpty()) {
			this.findAndExecuteTask(villages, tasks);
		}
		
		errorCount=0; //Execution success, reset error count if incremented
	}

	private void initiateAdventure(AccountInfoResponse accountResponse) {
		if (Log.isInfoEnabled())
			Log.info("Pending adventure::" + accountResponse.getPendingAdventure());
		if (accountResponse.getPendingAdventure() > 0 && "in home village".equals(accountResponse.getHeroStatus())) {
			if (Log.isInfoEnabled())
				Log.info("Pending adventure count is ::" + accountResponse.getPendingAdventure()
						+ "--Hero is in home::Initiating adventure");
			GameWorld adventureRequest = new GameWorld();
			List<Adventure> adventures = client.getAdventures(adventureRequest);
			Status status = AccountUtils.initiateAdventure(adventures, client);
			if ("SUCCESS".equals(status.getStatus())) {
				if (Log.isInfoEnabled())
					Log.info("Adventure initiated");
			}
		}
	}

	private List<Village> getVillageList(List<String> villageList) {
		VillageInfoRequest villageInfoRequest = new VillageInfoRequest();
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
			if(task!=null && (task.getTaskType()==TaskType.RESOURCE_UPDATE || task.getTaskType()==TaskType.BUILDING_UPDATE)) {
					Resource resource = e.getResource();
					Fields field = searchResourceField(task.getId(), resource.getFields());
					if(field==null) {
						field = new Fields();
						Building building = searchBuildingField(task.getId(), e.getBuildings());
						field.setNextLevelClay(building.getNextLevelClay());
						field.setNextLevelCrop(building.getNextLevelCrop());
						field.setNextLevelIron(building.getNextLevelIron());
						field.setNextLevelWood(building.getNextLevelWood());
						field.setId(building.getId());
					}
					if(resource.getWood() > field.getNextLevelWood() && resource.getClay() > field.getNextLevelClay() && resource.getIron() > field.getNextLevelIron() && resource.getCrop() > field.getNextLevelCrop()) {
						if(Log.isInfoEnabled())
							Log.info("Enough resources are present to complete the task");
						if(e.getOngoingConstruction()==AppConstant.MAX_UPGRADE_TASK) {
							if(Log.isInfoEnabled())
								Log.info("Maximum number of tasks are already in progress:::unable to execute task now");
							return;
						}else {
							UpgradeRequest resourceUpgradeRequest = new UpgradeRequest();
							resourceUpgradeRequest.setVillageId(e.getVillageId());
							resourceUpgradeRequest.setId(String.valueOf(field.getId()));
							Status status = client.upgrade(resourceUpgradeRequest);
							int constructionCount = Integer.valueOf(status.getStatus());
							if(constructionCount>e.getOngoingConstruction()) {
								if(Log.isInfoEnabled())
									Log.info("Upgrade done successfully");
								service.completeTask(e.getVillageId(), task.getTaskId());
							}
						}
					}else {
						if(Log.isInfoEnabled())
							Log.info("Not enough resources to complete the task");
						return;
					}
			}
		});
	}
	
	
	
	
	private Fields searchResourceField(int id, List<Fields> fields) {
		int start=0;
		int end = fields.size()-1;
		
		while(end>=start) {
			int mid  = (start+end)/2;
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
	
	private Building searchBuildingField(int id, List<Building> building) {
		int start=0;
		int end = building.size()-1;
		
		while(end>=start) {
			int mid  = (start+end)/2;
			if(building.get(mid).getId()==id) {
				return building.get(mid);
			}else if(building.get(mid).getId()>id) {
				end = mid-1;
			}else {
				start = mid+1;
			}
		}
		return null;
	}

}
