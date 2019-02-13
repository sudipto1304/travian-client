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
	
	public GameWorld() {
		this.host = (String) BaseProfile.profile.get("HOST");
		this.userId = (String) BaseProfile.profile.get("USERID");
		this.cookies = (Map<String, String>) BaseProfile.profile.get("COOKIES");
	}

}
