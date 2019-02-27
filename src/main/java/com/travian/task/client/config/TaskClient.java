package com.travian.task.client.config;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.travian.task.client.response.Status;
import com.travian.task.client.response.Task;
import com.travian.task.client.response.TroopTrain;

@FeignClient(name = "travian-task-list")
public interface TaskClient {

	@RequestMapping(method = RequestMethod.GET, path = "/travian-task-queue/task/getTask/{villageId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	Task getTask(@PathVariable("villageId") String villageId);
	
	@RequestMapping(method = RequestMethod.GET, path = "/travian-task-queue/task/completeTask/{villageId}/{taskId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	Task completeTask(@PathVariable("villageId") String villageId, @PathVariable("taskId") String taskId);
	
	@RequestMapping(method = RequestMethod.GET, path = "/travian-task-queue/task/skipTask/{villageId}/{taskId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	Task skipTask(@PathVariable("villageId") String villageId, @PathVariable("taskId") String taskId);
	
	@RequestMapping(method = RequestMethod.GET, path = "/travian-task-queue/task/getTroopTrainTasks", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	List<TroopTrain> getTrainTask();
	
	@RequestMapping(method = RequestMethod.POST, path = "/travian-task-queue/task/updateTroopTask", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	Status updateTrainingCount(@RequestBody List<TroopTrain> request);
}
