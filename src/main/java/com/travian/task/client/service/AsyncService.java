package com.travian.task.client.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import com.travian.task.client.config.ServiceClient;
import com.travian.task.client.config.TaskClient;
import com.travian.task.client.request.AccountInfoRequest;
import com.travian.task.client.request.GameWorld;
import com.travian.task.client.request.TroopTrainRequest;
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
import com.travian.task.client.response.TroopTrain;
import com.travian.task.client.response.TroopTrainResponse;
import com.travian.task.client.response.UpgradeStatus;
import com.travian.task.client.response.Village;
import com.travian.task.client.util.AccountUtils;
import com.travian.task.client.util.AppConstant;
import com.travian.task.client.util.BaseProfile;

@Component
@Scope("prototype")
public class AsyncService implements Runnable{
	private static final Logger Log = LoggerFactory.getLogger(AsyncService.class);
	
	@Autowired
	private TaskClient taskClient;
	
	private AccountInfoRequest request;
	
	@Autowired
	private ServiceClient serviceClient;

	
	public Task getTask(String villageId){
		Task task = taskClient.getTask(villageId);
		try {
			return task;
		} catch (Exception e) {
			if(Log.isErrorEnabled())
				Log.error("", e);
		}
		return null;
	}
	
	public AsyncService(AccountInfoRequest request) {
		this.request = request;
	}
	

	public void completeTask(String villageId, String taskId){
		taskClient.completeTask(villageId, taskId);
	}
	

	public void skipTask(String villageId, String taskId){
		taskClient.skipTask(villageId, taskId);
	}
	
	public List<TroopTrain> getTrainTasks(){
		return taskClient.getTrainTask();
	}
	
	public void updateTroopCount(List<TroopTrain> request){
		taskClient.updateTrainingCount(request);
	}


	@Override
	public void run() {
		try {
			executeTask();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	
	public void executeTask() throws InterruptedException {
		Map<String, String> cookies = null;
		int pauseCount = 0;
		int errorCount = 0;
		int troopTrainIntervalCount = 0;
		while (true) {
			if (BaseProfile.isExecutionEnable) {
				pauseCount = 0; // reset pause count if incremented
				try {
					AccountInfoResponse accountResponse = null;
					if (cookies == null) {
						if (Log.isInfoEnabled())
							Log.info("Cookies not present::getting account info with login");
						accountResponse = serviceClient.getAccountInfo(request);
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
						accountResponse = serviceClient.getAccountInfo(accountInfoRequest);
					}

					if (troopTrainIntervalCount == 15) {
						if (Log.isInfoEnabled())
							Log.info("Troop Train Count is " + troopTrainIntervalCount + " initiate troop train");
						executeTaskList(accountResponse, true);
						troopTrainIntervalCount = 0;
					} else {
						if (Log.isInfoEnabled())
							Log.info("Troop Train Count is " + troopTrainIntervalCount + " skip troop train");
						executeTaskList(accountResponse, false);
						troopTrainIntervalCount++;
					}
					Random r = new Random();
					int rand = r.ints(60, (100 + 1)).limit(1).findFirst().getAsInt();
					errorCount = 0; // Execution success, reset error count if incremented
					if (Log.isInfoEnabled())
						Log.info("Next call in ::" + rand + " sec");
					Thread.sleep(2000 * rand);
				} catch (Exception e) {
					if (Log.isErrorEnabled())
						Log.error("", e);
					errorCount++;
					cookies = null;
					if (errorCount >= 10) {
						break;
					}
				}
			} else {
				pauseCount++;
				if (Log.isErrorEnabled()) {
					Log.error("::::isExecutionEnable false:::::execution paused" + System.currentTimeMillis());
				}
				Thread.sleep(1000 * 60);
				if (pauseCount > 60) {
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

	private void executeTaskList(AccountInfoResponse accountResponse, boolean trainTroop) {
		// 1. check for pending adventure
		this.initiateAdventure(accountResponse);
		// 2. get village info
		List<String> villageList = accountResponse.getVillages().stream().map(p -> p.getLink())
				.collect(Collectors.toList());
		Map<String, Task> tasks = new HashMap<String, Task>();
		List<TroopTrain> troopTasks = null;
		villageList.forEach(e -> {
			tasks.put(e.substring(e.indexOf("=") + 1, e.length() - 1),
					this.getTask(e.substring(e.indexOf("=") + 1, e.length() - 1)));
		});

		List<Village> villages = this.getVillageList(villageList);
		Map<String, UpgradeStatus> upgradeStatus = null;
		if (!tasks.isEmpty()) {
			upgradeStatus = this.findAndExecuteTask(villages, tasks);
			if (Log.isInfoEnabled())
				Log.info("upgradeStatus:::" + upgradeStatus);
		}

		if (trainTroop) {
			troopTasks = this.getTrainTasks();
			this.trainTroop(villages, troopTasks, upgradeStatus);
		}

	}

	private void trainTroop(List<Village> villages, List<TroopTrain> troopTasks,
			Map<String, UpgradeStatus> upgradeStatusMap) {
		List<TroopTrain> troopTrainResponse = new ArrayList<TroopTrain>();
		troopTasks.forEach(e -> {
			UpgradeStatus upgradeStatus = upgradeStatusMap.get(String.valueOf(e.getVillageId()));
			if (upgradeStatus == UpgradeStatus.MAX_WORK_IN_PROGRESS || upgradeStatus == UpgradeStatus.NO_TASK
					|| upgradeStatus == UpgradeStatus.UPGRADE_SUCCESS) {
				if (Log.isInfoEnabled())
					Log.info("upgradeStatus for village id " + e.getVillageId() + " is::" + upgradeStatus.name()
							+ "::TroopTrain task continue");
				if (StringUtils.isEmpty(e.getLink())) {
					for (Village village : villages) {
						if (village.getVillageId().equals(String.valueOf(e.getVillageId()))) {
							List<Building> buildings = village.getBuildings();
							for (Building building : buildings) {
								if (building.getBuildingName().equals(e.getBuilding())) {
									TroopTrainRequest trainRequest = new TroopTrainRequest();
									trainRequest.setBuildingId(String.valueOf(building.getId()));
									trainRequest.setTroopType(e.getTroopType());
									trainRequest.setVillageId(String.valueOf(e.getVillageId()));
									trainRequest.setPath(
											"/build.php?newdid=" + e.getVillageId() + "&id=" + building.getId());
									if (Log.isInfoEnabled())
										Log.info("Training troop for villageID::" + village.getVillageName()
												+ "::Troop Type ::" + e.getTroopType() + "::In::" + e.getBuilding());
									TroopTrainResponse response = serviceClient.trainTroop(trainRequest);
									if (Log.isInfoEnabled())
										Log.info("Troop is traing in village::" + village.getVillageName() + "::In::"
												+ e.getBuilding() + "::Total train count::" + response.getCount()
												+ " will end on " + response.getTimeRequired());
									TroopTrain trainResponse = new TroopTrain();
									trainResponse.setCount(response.getCount());
									trainResponse.setLink(
											"/build.php?newdid=" + e.getVillageId() + "&id=" + building.getId());
									trainResponse.setTaskId(e.getTaskId());
									if(trainResponse.getCount()>0)
										troopTrainResponse.add(trainResponse);
									break;
								}
							}
						}
					}
				} else {
					String path = e.getLink();
					TroopTrainRequest trainRequest = new TroopTrainRequest();
					trainRequest.setBuildingId(
							String.valueOf(path.replace("/build.php?newdid=" + e.getVillageId() + "&id=", "")));
					trainRequest.setTroopType(e.getTroopType());
					trainRequest.setVillageId(String.valueOf(e.getVillageId()));
					trainRequest.setPath(e.getLink());
					if (Log.isInfoEnabled())
						Log.info("Training troop for villageID::" + e.getVillageId() + "::Troop Type ::"
								+ e.getTroopType() + "::In::" + e.getBuilding());
					TroopTrainResponse response = serviceClient.trainTroop(trainRequest);
					if (Log.isInfoEnabled())
						Log.info("Troop is traing in village::" + e.getVillageId() + "::In::" + e.getBuilding()
								+ "::Total train count::" + response.getCount() + " will end on "
								+ response.getTimeRequired());
					TroopTrain trainResponse = new TroopTrain();
					trainResponse.setCount(response.getCount());
					trainResponse.setLink(e.getLink());
					trainResponse.setTaskId(e.getTaskId());
					if(trainResponse.getCount()>0)
						troopTrainResponse.add(trainResponse);
				}
				this.updateTroopCount(troopTrainResponse);
			} else {
				if (Log.isInfoEnabled())
					Log.info("upgradeStatus for village id " + e.getVillageId() + " is::" + upgradeStatus.name()
							+ "::TroopTrain task skip");
			}

		});

	}

	private void initiateAdventure(AccountInfoResponse accountResponse) {
		if (Log.isInfoEnabled())
			Log.info("Pending adventure::" + accountResponse.getPendingAdventure());
		if (accountResponse.getPendingAdventure() > 0 && "in home village".equals(accountResponse.getHeroStatus())) {
			if (Log.isInfoEnabled())
				Log.info("Pending adventure count is ::" + accountResponse.getPendingAdventure()
						+ "--Hero is in home::Initiating adventure");
			GameWorld adventureRequest = new GameWorld();
			List<Adventure> adventures = serviceClient.getAdventures(adventureRequest);
			Status status = AccountUtils.initiateAdventure(adventures, serviceClient);
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
		List<Village> villages = serviceClient.getVillageInfo(villageInfoRequest);
		if (Log.isInfoEnabled())
			Log.info("Number of village:::" + villages.size());
		return villages;
	}

	private Map<String, UpgradeStatus> findAndExecuteTask(List<Village> villages, Map<String, Task> tasks) {
		final Map<String, UpgradeStatus> upgradeStatus = new HashMap<String, UpgradeStatus>();
		villages.forEach(e -> {
			Task task = tasks.get(e.getVillageId());
			try {
				
				if (Log.isInfoEnabled())
					Log.info("Task to be executed:::" + task);
				if (task != null && (task.getTaskType() == TaskType.RESOURCE_UPDATE
						|| task.getTaskType() == TaskType.BUILDING_UPDATE)) {
					Resource resource = e.getResource();
					Fields field = searchResourceField(task.getId(), resource.getFields());
					if (field == null) {
						field = new Fields();
						Building building = searchBuildingField(task.getId(), e.getBuildings());
						field.setNextLevelClay(building.getNextLevelClay());
						field.setNextLevelCrop(building.getNextLevelCrop());
						field.setNextLevelIron(building.getNextLevelIron());
						field.setNextLevelWood(building.getNextLevelWood());
						field.setId(building.getId());
					}
					if (resource.getWood() > field.getNextLevelWood() && resource.getClay() > field.getNextLevelClay()
							&& resource.getIron() > field.getNextLevelIron()
							&& resource.getCrop() > field.getNextLevelCrop()) {
						if (Log.isInfoEnabled())
							Log.info("Enough resources are present to complete the task");
						if (e.getOngoingConstruction() == AppConstant.MAX_UPGRADE_TASK) {
							upgradeStatus.put(e.getVillageId(), UpgradeStatus.MAX_WORK_IN_PROGRESS);
							if (Log.isInfoEnabled())
								Log.info(
										"Maximum number of tasks are already in progress:::unable to execute task now");
							return;
						} else {
							UpgradeRequest resourceUpgradeRequest = new UpgradeRequest();
							resourceUpgradeRequest.setVillageId(e.getVillageId());
							resourceUpgradeRequest.setId(String.valueOf(field.getId()));
							Status status = serviceClient.upgrade(resourceUpgradeRequest);
							if (status.getStatusCode() == 400) {
								if (Log.isInfoEnabled())
									Log.info("Unable to execute task. Skipping the task");
								upgradeStatus.put(e.getVillageId(), UpgradeStatus.TASK_SKIP);
								this.skipTask(e.getVillageId(), task.getTaskId());
							} else {
								int constructionCount = Integer.valueOf(status.getStatus());
								if (constructionCount > e.getOngoingConstruction()) {
									if (Log.isInfoEnabled())
										Log.info("Upgrade done successfully");
									upgradeStatus.put(e.getVillageId(), UpgradeStatus.UPGRADE_SUCCESS);
									this.completeTask(e.getVillageId(), task.getTaskId());
								}
							}
						}
					} else {
						upgradeStatus.put(e.getVillageId(), UpgradeStatus.NOT_ENOUGH_RESOURCE);
						if (Log.isInfoEnabled())
							Log.info("Not enough resources to complete the task");
						return;
					}
				} else {
					upgradeStatus.put(e.getVillageId(), UpgradeStatus.NO_TASK);
				}
			}

			catch (Exception ex) {
				if (Log.isErrorEnabled())
					Log.error("Error to execute task", e);
				this.skipTask(e.getVillageId(), task.getTaskId());
			}
		});

		return upgradeStatus;
	}

	private Fields searchResourceField(int id, List<Fields> fields) {
		int start = 0;
		int end = fields.size() - 1;

		while (end >= start) {
			int mid = (start + end) / 2;
			if (fields.get(mid).getId() == id) {
				return fields.get(mid);
			} else if (fields.get(mid).getId() > id) {
				end = mid - 1;
			} else {
				start = mid + 1;
			}
		}
		return null;
	}

	private Building searchBuildingField(int id, List<Building> building) {
		int start = 0;
		int end = building.size() - 1;

		while (end >= start) {
			int mid = (start + end) / 2;
			if (building.get(mid).getId() == id) {
				return building.get(mid);
			} else if (building.get(mid).getId() > id) {
				end = mid - 1;
			} else {
				start = mid + 1;
			}
		}
		return null;
	}

}
