package com.travian.task.client.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import lombok.ToString;


@Data
@ToString
@JsonInclude(Include.NON_NULL)
public class TroopTrain implements Serializable{
	
	private String taskId;
	private String userId;
	private  int villageId;
	private String troopType;
	private String building;
	private TaskStatus status;
	private int count;
	private int targetCount;
	private String link;

}
