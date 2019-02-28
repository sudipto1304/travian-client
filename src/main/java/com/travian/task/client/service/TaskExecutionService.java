package com.travian.task.client.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import com.travian.task.client.request.AccountInfoRequest;

@Service
public class TaskExecutionService {

	private static final Logger Log = LoggerFactory.getLogger(TaskExecutionService.class);


	@Autowired
    private TaskExecutor taskExecutor;
    @Autowired
    private ApplicationContext applicationContext;
    
    
    public void executeAsynchronously(AccountInfoRequest request) {
        AsyncService service = applicationContext.getBean(AsyncService.class, request);
        taskExecutor.execute(service);
    }
	

}
