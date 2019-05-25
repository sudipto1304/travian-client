package com.travian.task.client.request;

import java.io.Serializable;
import java.util.List;

import lombok.Data;
import lombok.ToString;


@Data
@ToString
public class TradeRouteRequest extends GameWorld implements Serializable{
	
	private String destinationVillage;
	private String sourceVillage;
	private String wood="0";
	private String clay="0";
	private String iron="0";
	private String crop="0";
	private List<String> time;
	private String numberOfDelivery="1";
	private String gid;
	private String a;
	private String t;
	private String trid;
	private String option;
	

}
