package com.travian.task.client.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import com.travian.task.client.config.ServiceClient;
import com.travian.task.client.config.TaskClient;
import com.travian.task.client.request.AccountInfoRequest;
import com.travian.task.client.request.CelebrationRequest;
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
public class AsyncService implements Runnable {
	private static final Logger Log = LoggerFactory.getLogger(AsyncService.class);

	@Autowired
	private TaskClient taskClient;

	private AccountInfoRequest request;
	private GameWorld gameWorld = new GameWorld();

	@Autowired
    private TaskExecutor taskExecutor;
    @Autowired
    private ApplicationContext applicationContext;
    
	@Autowired
	private ServiceClient serviceClient;

	public Task getTask(String villageId) {
		Task task = taskClient.getTask(this.gameWorld.getUserUUID(), villageId);
		try {
			return task;
		} catch (Exception e) {
			if (Log.isErrorEnabled())
				Log.error("", e);
		}
		return null;
	}

	public AsyncService(AccountInfoRequest request) {
		this.request = request;
	}

	public void completeTask(String villageId, String taskId) {
		taskClient.completeTask(villageId, taskId);
	}

	public void skipTask(String villageId, String taskId) {
		taskClient.skipTask(villageId, taskId);
	}

	public List<TroopTrain> getTrainTasks() {
		return taskClient.getTrainTask(this.gameWorld.getUserUUID());
	}

	public void updateTroopCount(List<TroopTrain> request) {
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
		int pauseCount = 0;
		int errorCount = 0;
		int troopTrainIntervalCount = 0;
		Map<String, Integer> celebrationMap = new HashMap<String, Integer>();
		while (true) {
			Random r = new Random();
			int rand = r.ints(60, (120 + 1)).limit(1).findFirst().getAsInt();
			pauseCount = 0; // reset pause count if incremented
			try {
				if(BaseProfile.isExecutionEnable) {
					AccountInfoResponse accountResponse = null;
					if (gameWorld.getCookies() == null) {
						if (Log.isInfoEnabled())
							Log.info("Cookies not present::getting account info with login");
						accountResponse = serviceClient.getAccountInfo(request);
						if (Log.isDebugEnabled())
							Log.debug("received AccountInfoResponse:::" + accountResponse);
						gameWorld.setCookies(accountResponse.getCookies());
						gameWorld.setHost(request.getHost());
						gameWorld.setUserId(request.getUserId());
						gameWorld.setUserUUID(request.getUserUUID());
						if (Log.isInfoEnabled())
							Log.info("Game World Data:::" + gameWorld);
					} else {
						if (Log.isInfoEnabled())
							Log.info("Cookies present::getting account info without login");
						accountResponse = serviceClient.getAccountInfo(gameWorld);
					}
	
					if (troopTrainIntervalCount == 15) {
						if (Log.isInfoEnabled())
							Log.info("Troop Train Count is " + troopTrainIntervalCount + " initiate troop train");
						executeTaskList(accountResponse, true, celebrationMap);
						troopTrainIntervalCount = 0;
					} else {
						if (Log.isInfoEnabled())
							Log.info("Troop Train Count is " + troopTrainIntervalCount + " skip troop train");
						executeTaskList(accountResponse, false, celebrationMap);
						troopTrainIntervalCount++;
					}
	
					errorCount = 0; // Execution success, reset error count if incremented
					if (Log.isInfoEnabled())
						Log.info("Next call in ::" + rand * 2 + " sec");
				}
				Thread.sleep(2000 * rand);
			} catch (Exception e) {
				if (Log.isErrorEnabled())
					Log.error("", e);
				errorCount++;
				if (errorCount >= 5) {
					if (Log.isErrorEnabled())
						Log.error("Error count 5. waiting for 5 mins");
					Thread.sleep(1000 * 60 * 5);
				}
				gameWorld.setCookies(null);
				
			}

		}

	}

	public void toggleExecution(boolean enable) {
		BaseProfile.isExecutionEnable = enable;
	}

	private void executeTaskList(AccountInfoResponse accountResponse, boolean trainTroop,
			Map<String, Integer> celebrationMap) {
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
		attackResolution(villages); //check for any incoming  attack
		Map<String, UpgradeStatus> upgradeStatus = null;
		if (!tasks.isEmpty()) {
			upgradeStatus = this.findAndExecuteTask(villages, tasks, celebrationMap);
			if (Log.isInfoEnabled())
				Log.info("upgradeStatus:::" + upgradeStatus);
		}

		if (trainTroop) {
			troopTasks = this.getTrainTasks();
			this.trainTroop(villages, troopTasks, upgradeStatus);
		}

	}
	
	private void attackResolution(List<Village> villages) {
		villages.forEach(e->{
			if(e.getIncomingAttack()!=null && e.getIncomingAttack().getAttackCount()>0) {
				if(Log.isInfoEnabled())
					Log.info("********Incoming attack detected******"+e.getIncomingAttack());
				AttackResolutionService service = applicationContext.getBean(AttackResolutionService.class, e, this.gameWorld);
				taskExecutor.execute(service);
			}
		});
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
									trainRequest.setGameWorld(this.gameWorld);
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
									trainResponse.setUserId(this.gameWorld.getUserUUID());
									if (trainResponse.getCount() > 0)
										troopTrainResponse.add(trainResponse);
									return;
								}
							}
						}
					}
				} else {
					String path = e.getLink();
					TroopTrainRequest trainRequest = new TroopTrainRequest();
					trainRequest.setGameWorld(this.gameWorld);
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
					trainResponse.setUserId(this.gameWorld.getUserUUID());
					if (trainResponse.getCount() > 0)
						troopTrainResponse.add(trainResponse);
					return;
				}
			} else {
				if (Log.isInfoEnabled())
					Log.info("upgradeStatus for village id " + e.getVillageId() + " is::" + upgradeStatus.name()
							+ "::TroopTrain task skip");
			}

		});
		if (Log.isInfoEnabled())
			Log.info("Troop Train update request---->"+troopTrainResponse);
		this.updateTroopCount(troopTrainResponse);

	}

	private void initiateAdventure(AccountInfoResponse accountResponse) {
		if (Log.isInfoEnabled())
			Log.info("Pending adventure::" + accountResponse.getPendingAdventure());
		if (accountResponse.getPendingAdventure() > 0 && "in home village".equals(accountResponse.getHeroStatus())) {
			if (Log.isInfoEnabled())
				Log.info("Pending adventure count is ::" + accountResponse.getPendingAdventure()
						+ "--Hero is in home::Initiating adventure");
			List<Adventure> adventures = serviceClient.getAdventures(this.gameWorld);
			Status status = AccountUtils.initiateAdventure(adventures, serviceClient, this.gameWorld);
			if ("SUCCESS".equals(status.getStatus())) {
				if (Log.isInfoEnabled())
					Log.info("Adventure initiated");
			}
		}
	}

	private List<Village> getVillageList(List<String> villageList) {
		VillageInfoRequest villageInfoRequest = new VillageInfoRequest();
		villageInfoRequest.setGameWorld(this.gameWorld);
		villageInfoRequest.setLink(villageList);
		if (Log.isInfoEnabled())
			Log.info("No of village:::" + villageList.size() + ":::villages link:::" + villageList);
		List<Village> villages = serviceClient.getVillageInfo(villageInfoRequest);
		if (Log.isInfoEnabled())
			Log.info("Number of village:::" + villages.size());
		return villages;
	}

	private Map<String, UpgradeStatus> findAndExecuteTask(List<Village> villages, Map<String, Task> tasks,
			Map<String, Integer> celebrationMap) {
		final Map<String, UpgradeStatus> upgradeStatus = new HashMap<String, UpgradeStatus>();
		villages.forEach(e -> {
			// Check for celebration eligibility
			if (e.isTownHallPresent()) {
				if (Log.isInfoEnabled())
					Log.info("TownHall Present");
				Status status = null;
				if (celebrationMap.containsKey(e.getVillageId())) {
					int celebrationTime = celebrationMap.get(e.getVillageId());
					if (celebrationTime <= 0) {
						if (Log.isInfoEnabled())
							Log.info("celebration counter::" + celebrationTime + " for vilalgeId::" + e.getVillageId()
									+ "::Going to check for active celebration");
						status = initiateCelebration(e);
					} else {
						if (Log.isInfoEnabled())
							Log.info("celebration counter::" + celebrationTime + " for vilalgeId::" + e.getVillageId()
									+ "::skip check for active celebration");
						celebrationMap.put(e.getVillageId(), celebrationMap.get(e.getVillageId()) - 1);
					}
				} else {
					status = initiateCelebration(e);
				}
				if (status != null) {
					if (status.getStatusCode() == 400) {
						if ("NOT.ENOUGH.RESOURCE".equals(status.getStatus())) {
							celebrationMap.put(e.getVillageId(), 0); // Checking again next time
							if (Log.isInfoEnabled())
								Log.info(
										"not enough resource to initiate celebration:::Waiting for enough resource::skipping all tasks");
							return;
						} else {
							celebrationMap.put(e.getVillageId(), 60); // Checking again after around 2 hrs
							if (Log.isInfoEnabled())
								Log.info("Celebration is going on::Not skipping tasks");
						}
					} else if (status.getStatusCode() == 200) {
						celebrationMap.put(e.getVillageId(), 150); // Checking again after around 5 hrs
						if (Log.isInfoEnabled())
							Log.info("Celebration initiated::end time::" + status.getStatus());
					}

				}
			}

			Task task = tasks.get(e.getVillageId());
			try {

				if (Log.isInfoEnabled())
					Log.info("Task to be executed:::" + task);
				if (task != null && (task.getTaskType() == TaskType.RESOURCE_UPDATE
						|| task.getTaskType() == TaskType.BUILDING_UPDATE)) { //Task Present in DB
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
					if (e.getOngoingConstruction() == AppConstant.MAX_UPGRADE_TASK) { //Max task in progress
						upgradeStatus.put(e.getVillageId(), UpgradeStatus.MAX_WORK_IN_PROGRESS);
						if (Log.isInfoEnabled())
							Log.info(
									"Maximum number of tasks are already in progress:::unable to execute task now");
						return;
					}else { //Task slot empty
						if (resource.getWood() > field.getNextLevelWood() && resource.getClay() > field.getNextLevelClay()
								&& resource.getIron() > field.getNextLevelIron()
								&& resource.getCrop() > field.getNextLevelCrop()) {  //Enough resource present
							if (Log.isInfoEnabled())
								Log.info("Enough resources are present to complete the task");
							
								UpgradeRequest resourceUpgradeRequest = new UpgradeRequest();
								resourceUpgradeRequest.setGameWorld(this.gameWorld);
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
						} else { //not enough resource
							upgradeStatus.put(e.getVillageId(), UpgradeStatus.NOT_ENOUGH_RESOURCE);
							if (Log.isInfoEnabled())
								Log.info("Not enough resources to complete the task");
							return;
						}
					}
					
				} else { //no task in db
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

	private Status initiateCelebration(Village village) {
		int townHallId = village.getThId();
		CelebrationRequest celebrationRequest = new CelebrationRequest();
		celebrationRequest.setGameWorld(this.gameWorld);
		celebrationRequest.setThId(String.valueOf(townHallId));
		celebrationRequest.setVillageId(village.getVillageId());
		return serviceClient.initiateCelebration(celebrationRequest);

	}

}
