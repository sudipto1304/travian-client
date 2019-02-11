package com.travian.task.client.request;

import java.io.Serializable;
import java.util.Map;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class InitiateAdventureRequest extends GameWorld implements Serializable{
	

	private String kid;
	private String from;
	private String send;
	private String a;

}
