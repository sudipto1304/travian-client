package com.travian.task.client.service;

import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import com.travian.task.client.config.TaskClient;
import com.travian.task.client.response.Task;
import com.travian.task.client.response.TroopTrain;

@Service
public class AsyncService {
	private static final Logger Log = LoggerFactory.getLogger(AsyncService.class);
	
	@Autowired
	private TaskClient client;
	
	@Async
	public Future<Task> getTask(String villageId){
		Task task = client.getTask(villageId);
		try {
			return new AsyncResult<Task>(task);
		} catch (Exception e) {
			if(Log.isErrorEnabled())
				Log.error("", e);
		}
		return null;
	}
	
	
	@Async
	public void completeTask(String villageId, String taskId){
		client.completeTask(villageId, taskId);
	}
	
	@Async
	public void skipTask(String villageId, String taskId){
		client.skipTask(villageId, taskId);
	}
	
	public List<TroopTrain> getTrainTasks(){
		return client.getTrainTask();
	}
	@Async
	public void updateTroopCount(List<TroopTrain> request){
		client.updateTrainingCount(request);
	}

}
