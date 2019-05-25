package com.travian.task.client.request;

import java.io.Serializable;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class TradeRequest implements Serializable{
	
	private String userId;
	private String sourceVillage;
	private String destVillageName;
	private int wood;
	private int clay;
	private int iron;
	private int crop;
	private int interval;
	private String transactionId;
	private String lastUpdateTime;

}
