package com.travian.task.client.response;

import java.io.Serializable;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Task implements Serializable{
	
	private String villageId;
	private String path;
	private int level;
	private int resourceId;
	private int buildingId;
	private TaskType taskType;
	private String taskId;

}
