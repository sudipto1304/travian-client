package com.travian.task.client.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.travian.task.client.config.ServiceClient;
import com.travian.task.client.controller.TaskExecutionController;
import com.travian.task.client.request.AccountInfoRequest;
import com.travian.task.client.response.AccountInfoResponse;
import com.travian.task.client.util.ExecutionFlags;

@Service
public class TaskExecutionService {
	
	
	private static final Logger Log = LoggerFactory.getLogger(TaskExecutionService.class);
	
	@Autowired
	private ServiceClient client;
	
	@Async
	public void execute(AccountInfoRequest request) throws InterruptedException {
		
		
			try {
				AccountInfoResponse accountResponse = client.getAccountInfo(request);
				if(Log.isDebugEnabled())
					Log.debug("received AccountInfoResponse:::"+accountResponse);
				Thread.sleep(1000*30);
			} catch (Exception e) {
				if(Log.isErrorEnabled())
					Log.error("",e);
			}
			
		
		if(Log.isErrorEnabled()) {
			Log.error("*******************Execution stopped**********************"+System.currentTimeMillis());
		}
		
	}

}
