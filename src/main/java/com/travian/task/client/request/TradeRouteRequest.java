package com.travian.task.client.request;

import java.io.Serializable;
import java.util.List;

import lombok.Data;
import lombok.ToString;


@Data
@ToString
public class TradeRouteRequest extends GameWorld implements Serializable{
	
	private String villageId;
	private int wood;
	private int clay;
	private int iron;
	private int crop;
	private List<Integer> time;
	private int numberOfDelivery;
	

}
