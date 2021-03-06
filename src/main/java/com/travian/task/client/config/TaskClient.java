package com.travian.task.client.config;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.travian.task.client.request.TradeRequest;
import com.travian.task.client.response.Status;
import com.travian.task.client.response.Task;
import com.travian.task.client.response.TroopTrain;

@FeignClient(name = "travian-task-list")
public interface TaskClient {

	@RequestMapping(method = RequestMethod.GET, path = "/travian-task-queue/task/getTask/{userId}/{villageId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	Task getTask(@PathVariable("userId") String userId, @PathVariable("villageId") String villageId);
	
	@RequestMapping(method = RequestMethod.GET, path = "/travian-task-queue/task/completeTask/{villageId}/{taskId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	Task completeTask(@PathVariable("villageId") String villageId, @PathVariable("taskId") String taskId);
	
	@RequestMapping(method = RequestMethod.GET, path = "/travian-task-queue/task/skipTask/{villageId}/{taskId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	Task skipTask(@PathVariable("villageId") String villageId, @PathVariable("taskId") String taskId);
	
	@RequestMapping(method = RequestMethod.GET, path = "/travian-task-queue/task/getTroopTrainTasks/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	List<TroopTrain> getTrainTask(@PathVariable("userId") String userId);
	
	@RequestMapping(method = RequestMethod.GET, path = "/travian-task-queue/preference/getAccountPreference/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	Map<String, String> getAccountPreference(@PathVariable("userId") String userId);
	
	@RequestMapping(method = RequestMethod.POST, path = "/travian-task-queue/task/updateTroopTask", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	Status updateTrainingCount(@RequestBody List<TroopTrain> request);
	
	
	@RequestMapping(method = RequestMethod.GET, path = "/travian-task-queue/task/getTrades/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	List<TradeRequest> getTrades(@PathVariable("userId") String userId);
	
	@RequestMapping(method = RequestMethod.GET, path = "/travian-task-queue/task/updateTrade/{transactionId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	Status updateTrades(@PathVariable("transactionId") String transactionId);
}
