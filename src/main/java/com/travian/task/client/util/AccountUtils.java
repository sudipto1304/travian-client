package com.travian.task.client.util;

import java.util.List;
import java.util.Map;

import com.travian.task.client.config.ServiceClient;
import com.travian.task.client.request.InitiateAdventureRequest;
import com.travian.task.client.response.Adventure;
import com.travian.task.client.response.Status;



public class AccountUtils {
	
	
	public static Status initiateAdventure(List<Adventure> adventures, Map<String, String> cookies, String host, ServiceClient client) {
		InitiateAdventureRequest adventureStartRequest = new InitiateAdventureRequest();
		adventureStartRequest.setA("1");
		adventureStartRequest.setCookies(cookies);
		adventureStartRequest.setFrom("list");
		adventureStartRequest.setHost(host);
		String link = adventures.get(0).getLink();
		link = link.substring(0, link.indexOf("?"));
		adventureStartRequest.setPath("/"+link);
		link = adventures.get(0).getLink();
		String kid = link.substring(link.indexOf("kid=")+4, link.length());
		adventureStartRequest.setKid(kid);
		adventureStartRequest.setSend("1");
		return client.initiateAdventure(adventureStartRequest);
	}

}
