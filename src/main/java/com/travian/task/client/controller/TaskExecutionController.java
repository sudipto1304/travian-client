package com.travian.task.client.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.travian.task.client.request.AccountInfoRequest;
import com.travian.task.client.response.Status;
import com.travian.task.client.service.TaskExecutionService;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping("/task")
public class TaskExecutionController {
	
	private static final Logger Log = LoggerFactory.getLogger(TaskExecutionController.class);
	
	@Autowired
	private TaskExecutionService service;
	
	@ApiResponses(value = {
            @ApiResponse(code = 201, message = "Created", response = String.class),
            @ApiResponse(code = 412, message = "Precondition Failed"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
	@RequestMapping(value="/execute", method=RequestMethod.POST, produces=MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Status> execute(@RequestBody AccountInfoRequest request, HttpServletRequest servletRequest, @RequestHeader HttpHeaders headers) throws IOException, InterruptedException {
		service.execute(request);
		return new ResponseEntity<>(new Status("SUCCESS", 200), HttpStatus.CREATED);
	}
	
	
	@ApiResponses(value = {
            @ApiResponse(code = 201, message = "Created", response = String.class),
            @ApiResponse(code = 412, message = "Precondition Failed"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
	@RequestMapping(value="/toggleExecution/{enableExecution}", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Status> toggleExecution(@PathVariable("enableExecution") String enableExecution, HttpServletRequest servletRequest, @RequestHeader HttpHeaders headers)  {
		service.toggleExecution(Boolean.valueOf(enableExecution));
		return new ResponseEntity<>(new Status("SUCCESS", 200), HttpStatus.CREATED);
	}

}
