package com.travian.task.client.request;

import java.io.Serializable;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class TroopEvasionRequest extends GameWorld implements Serializable{
	
	private String villageId;
	private String destinationName;
	

}
