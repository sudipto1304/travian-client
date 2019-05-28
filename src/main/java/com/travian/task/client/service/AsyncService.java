package com.travian.task.client.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
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
import com.travian.task.client.request.TradeRequest;
import com.travian.task.client.request.TradeRouteRequest;
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

@Component
@Scope("prototype")
public class AsyncService implements Runnable {
	private static final Logger Log = LoggerFactory.getLogger(AsyncService.class);

	@Autowired
	private TaskClient taskClient;
	
	@Autowired
	private BrowserService browserService;

	private AccountInfoRequest request;
	private GameWorld gameWorld = new GameWorld();

	private Map<String, Boolean> busyStatusMap = new HashMap<String, Boolean>();

	@Autowired
	private TaskExecutor taskExecutor;
	@Autowired
	private ApplicationContext applicationContext;

	private Map<String, String> preference;

	@Autowired
	private ServiceClient serviceClient;

	public Task getTask(String villageId) {
		Task task = taskClient.getTask(this.request.getUserUUID(), villageId);
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
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void executeTask() throws InterruptedException, MalformedURLException {
		int pauseCount = 0;
		int errorCount = 0;
		int troopTrainIntervalCount = 0;
		Map<String, Integer> celebrationMap = new HashMap<String, Integer>();
		WebDriver driver = null;
		try {
			while (true) {
				Random r = new Random();
				int rand = r.ints(60, (120 + 1)).limit(1).findFirst().getAsInt();
				pauseCount = 0; // reset pause count if incremented
				this.preference = taskClient.getAccountPreference(request.getUserUUID());
				celebrationMap.clear();
				busyStatusMap.clear();
				try {
					if (Boolean.valueOf(this.preference.get("executionEnable"))) {
						if (Log.isInfoEnabled())
							Log.info("**********Task execution start/resumed*********");
						if (driver == null) {
							driver = new RemoteWebDriver(new URL("http://127.0.0.1:9515"),
									DesiredCapabilities.chrome());
							driver.get("https://" + request.getHost());
							driver.manage().window().maximize();
						}
						if (driver.findElement(By.name("password")) != null) {
							driver.findElement(By.name("name")).sendKeys("Thunder Bird");
							driver.findElement(By.name("password")).sendKeys("Antaheen@4813");
							driver.findElement(By.name("s1")).click();
						}
						if (Log.isInfoEnabled())
							Log.info("Cookies present::getting account info without login");

						/*
						 * if (troopTrainIntervalCount == 15) { if (Log.isInfoEnabled())
						 * Log.info("Troop Train Count is " + troopTrainIntervalCount +
						 * " initiate troop train"); executeTaskList(accountResponse, true,
						 * celebrationMap); troopTrainIntervalCount = 0; } else {
						 */
						if (Log.isInfoEnabled())
							Log.info("Troop Train Count is " + troopTrainIntervalCount + " skip troop train");
						executeTaskList(driver, false, celebrationMap);
						troopTrainIntervalCount++;
						/* } */

						errorCount = 0; // Execution success, reset error count if incremented
						if (Log.isInfoEnabled())
							Log.info("Next call in ::" + rand * 2 + " sec");
					} else {
						if (Log.isInfoEnabled())
							Log.info("**********Task execution paused*********");
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
		} finally {
			driver.quit();
		}

	}

	private void executeTaskList(WebDriver driver, boolean trainTroop,
			Map<String, Integer> celebrationMap) {
		// 1. check for pending adventure
		browserService.initiateAdventure(driver);
		List<Village> villages = browserService.getVillageOverview(driver);
		villages.forEach(e->{
			browserService.executeVillageTaskes(this.request, driver, e, this.getTask(e.getVillageId()));
		});
		// 2. get village info
		/*List<String> villageList = null;
		Map<String, Task> tasks = new HashMap<String, Task>();
		List<TroopTrain> troopTasks = null;
		villageList.forEach(e -> {
			tasks.put(e.substring(e.indexOf("=") + 1, e.length() - 1),
					this.getTask(e.substring(e.indexOf("=") + 1, e.length() - 1)));
		});

		List<Village> villages = this.getVillageList(villageList);
		// attackResolution(villages); // check for any incoming attack
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

		this.resourceManagement(villages);*/

	}

	private void attackResolution(List<Village> villages) {
		villages.forEach(e -> {
			if (e.getIncomingAttack() != null && e.getIncomingAttack().getAttackCount() > 0) {
				this.busyStatusMap.put(e.getVillageId(), true);
				if (Log.isInfoEnabled())
					Log.info("********Incoming attack detected******" + e.getIncomingAttack());
				AttackResolutionService service = applicationContext.getBean(AttackResolutionService.class, e,
						this.gameWorld);
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
				try {
					if (Log.isInfoEnabled())
						Log.info("upgradeStatus for village id " + e.getVillageId() + " is::" + upgradeStatus.name()
								+ "::TroopTrain task skip");
				} catch (Exception e2) {
					Log.error("Handled Exeption::::", e2);
				}

			}

		});
		if (Log.isInfoEnabled())
			Log.info("Troop Train update request---->" + troopTrainResponse);
		this.updateTroopCount(troopTrainResponse);

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
			this.busyStatusMap.put(e.getVillageId(), false);
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
							this.busyStatusMap.put(e.getVillageId(), true);
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
						|| task.getTaskType() == TaskType.BUILDING_UPDATE)) { // Task Present in DB
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
					if (e.getOngoingConstruction() == AppConstant.MAX_UPGRADE_TASK) { // Max task in progress
						upgradeStatus.put(e.getVillageId(), UpgradeStatus.MAX_WORK_IN_PROGRESS);
						if (Log.isInfoEnabled())
							Log.info("Maximum number of tasks are already in progress:::unable to execute task now");
						return;
					} else { // Task slot empty
						if (resource.getWood() > field.getNextLevelWood()
								&& resource.getClay() > field.getNextLevelClay()
								&& resource.getIron() > field.getNextLevelIron()
								&& resource.getCrop() > field.getNextLevelCrop()) { // Enough resource present
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
								this.busyStatusMap.put(e.getVillageId(), true);
							} else {
								int constructionCount = Integer.valueOf(status.getStatus());
								if (constructionCount > e.getOngoingConstruction()) {
									if (Log.isInfoEnabled())
										Log.info("Upgrade done successfully");
									upgradeStatus.put(e.getVillageId(), UpgradeStatus.UPGRADE_SUCCESS);
									this.completeTask(e.getVillageId(), task.getTaskId());
								}
							}
						} else { // not enough resource
							upgradeStatus.put(e.getVillageId(), UpgradeStatus.NOT_ENOUGH_RESOURCE);
							if (Log.isInfoEnabled())
								Log.info("Not enough resources to complete the task");
							this.busyStatusMap.put(e.getVillageId(), true);
							return;
						}
					}

				} else { // no task in db
					upgradeStatus.put(e.getVillageId(), UpgradeStatus.NO_TASK);
				}
			}

			catch (Exception ex) {
				if (Log.isErrorEnabled())
					Log.error("Error to execute task", e);
				this.busyStatusMap.put(e.getVillageId(), true);
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

	private void resourceManagement(List<Village> villages) {
		if (Log.isInfoEnabled()) {
			Log.info("Initiated resourceManagement");
			Log.info("Village Busy Status:::" + this.busyStatusMap);
		}
		List<TradeRequest> trades = taskClient.getTrades(this.gameWorld.getUserUUID());
		trades.forEach(e -> {
			boolean isBusy = this.busyStatusMap.get(e.getSourceVillage());
			if (!isBusy) {
				if (Log.isInfoEnabled())
					Log.info(e.getSourceVillage() + " is not busy::Checking schedule transfer time");
				TradeRouteRequest transferRequest = new TradeRouteRequest();
				long timeStampDiff = new Date().getTime() - Long.valueOf(e.getLastUpdateTime());
				int min = (int) (timeStampDiff / (1000 * 60));
				if (Log.isInfoEnabled())
					Log.info(e.getSourceVillage() + " Last updated " + min + " min ago");
				if (min >= e.getInterval()) {
					if (Log.isInfoEnabled())
						Log.info("Last updated " + min + " min ago::interval " + e.getInterval()
								+ " :::Initiating transfer");
					transferRequest.setGameWorld(this.gameWorld);
					transferRequest.setClay(String.valueOf(e.getClay()));
					transferRequest.setWood(String.valueOf(e.getWood()));
					transferRequest.setCrop(String.valueOf(e.getCrop()));
					transferRequest.setIron(String.valueOf(e.getIron()));
					transferRequest.setDestinationVillage(e.getDestVillageName());
					transferRequest.setSourceVillage(e.getSourceVillage());
					transferRequest.setNumberOfDelivery("1");
					if (Log.isInfoEnabled())
						Log.info("Transfer request for" + e.getSourceVillage() + " is::" + transferRequest);
					Status status = null;
					try {
						status = serviceClient.transferResource(transferRequest);
					} catch (Exception e2) {
						if (Log.isErrorEnabled())
							Log.error("transfer Failed for " + e.getTransactionId());
						taskClient.updateTrades(e.getTransactionId());
						return;
					}

					if (Log.isInfoEnabled())
						Log.info("Transfer status for ::" + e.getSourceVillage() + " is::" + status);
					if (status.getStatusCode() == 200) {
						taskClient.updateTrades(e.getTransactionId());
					}
				} else {
					if (Log.isInfoEnabled())
						Log.info("Last updated " + min + " min ago::interval " + e.getInterval() + " :::transfer skip");
				}

			} else {
				if (Log.isInfoEnabled())
					Log.info(e.getSourceVillage() + " has busy status:: no transfer");
			}
		});

		this.safeTransfer(villages);

	}

	private void safeTransfer(List<Village> villages) {
		villages.forEach(e -> {
			boolean transferRequired = false;
			TradeRouteRequest transferRequest = new TradeRouteRequest();
			int wareHouseCapacity = e.getResource().getWarehouseCapacity();
			int granCapacity = e.getResource().getGranaryCapacity();
			int wareHouseCapacity85 = (wareHouseCapacity * 85) / 100;
			int granCapacity85 = (granCapacity * 85) / 100;
			int woodPercent = (e.getResource().getWood() / wareHouseCapacity) * 100;
			int ironPercent = (e.getResource().getIron() / wareHouseCapacity) * 100;
			int clayPercent = (e.getResource().getClay() / wareHouseCapacity) * 100;
			int cropPercent = (e.getResource().getCrop() / granCapacity) * 100;
			if (woodPercent > 85) {
				transferRequired = true;
				if (Log.isInfoEnabled())
					Log.info(e.getVillageId() + " woodPercent::" + woodPercent);
				transferRequest.setWood(String.valueOf(e.getResource().getWood() - wareHouseCapacity85));
			}
			if (clayPercent > 85) {
				transferRequired = true;
				if (Log.isInfoEnabled())
					Log.info(e.getVillageId() + " clayPercent::" + clayPercent);
				transferRequest.setClay(String.valueOf(e.getResource().getClay() - wareHouseCapacity85));
			}
			if (ironPercent > 85) {
				transferRequired = true;
				if (Log.isInfoEnabled())
					Log.info(e.getVillageId() + " ironPercent::" + ironPercent);
				transferRequest.setIron(String.valueOf(e.getResource().getIron() - wareHouseCapacity85));
			}
			if (cropPercent > 85) {
				transferRequired = true;
				if (Log.isInfoEnabled())
					Log.info(e.getVillageId() + " cropPercent::" + cropPercent);
				transferRequest.setCrop(String.valueOf(e.getResource().getCrop() - granCapacity85));
			}
			transferRequest.setDestinationVillage(this.preference.get("resourceTransferVillage"));
			transferRequest.setSourceVillage(e.getVillageId());
			transferRequest.setNumberOfDelivery("1");
			transferRequest.setGameWorld(this.gameWorld);
			if (transferRequired) {
				if (Log.isInfoEnabled())
					Log.info("Transfer request for" + e.getVillageId() + " is::" + transferRequest);
				try {
					Status status = serviceClient.transferResource(transferRequest);
					if (Log.isInfoEnabled())
						Log.info("Transfer success for " + e.getVillageId());
				} catch (Exception e2) {
					if (Log.isErrorEnabled())
						Log.error("transfer Failed for " + e.getVillageId());
					return;
				}
			}

		});
	}
}
