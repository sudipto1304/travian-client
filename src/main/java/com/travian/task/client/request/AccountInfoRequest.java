package com.travian.task.client.request;

import java.io.Serializable;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class AccountInfoRequest extends GameWorld implements Serializable{
	private String password;
	private int maxTask;
	

}
