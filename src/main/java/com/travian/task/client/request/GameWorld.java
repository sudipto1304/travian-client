package com.travian.task.client.request;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.databind.deser.Deserializers.Base;
import com.travian.task.client.util.BaseProfile;

import lombok.Data;
import lombok.ToString;


@Data
@ToString
public class GameWorld implements Serializable{
	
	private String host;
	private String path;
	private String userId;
	private Map<String, String> cookies;
	
	
	public void setGameWorld(GameWorld gameWorld) {
		this.host = gameWorld.getHost();
		this.userId = gameWorld.getUserId();
		this.cookies = gameWorld.getCookies();
		
	}

}
