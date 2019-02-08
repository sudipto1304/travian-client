package com.travian.task.client.response;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Status {
	
	private String status;
	private int statusCode;
	
	public Status(String status, int statusCode) {
		this.status = status;
		this.statusCode  = statusCode;
	}

}
