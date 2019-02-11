package com.travian.task.client.config;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.travian.task.client.response.Task;

@FeignClient(name = "travian-task-list")
public interface TaskClient {

	@RequestMapping(method = RequestMethod.GET, path = "/task/getTask/{villageId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	Task getTask(@PathVariable("villageId") String villageId);
}
