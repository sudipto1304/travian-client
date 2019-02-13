package com.travian.task.client.request;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class VillageInfoRequest extends GameWorld implements Serializable{
	

	private List<String> link;

}
