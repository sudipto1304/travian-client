package com.travian.task.client.service;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.travian.task.client.config.ServiceClient;
import com.travian.task.client.request.GameWorld;
import com.travian.task.client.request.TroopEvasionRequest;
import com.travian.task.client.request.VillageInfoRequest;
import com.travian.task.client.response.EvasionResponse;
import com.travian.task.client.response.IncomingAttack;
import com.travian.task.client.response.Status;
import com.travian.task.client.response.Village;
import com.travian.task.client.util.BaseProfile;

@Component
@Scope("prototype")
public class AttackResolutionService implements Runnable {

	private static final Logger Log = LoggerFactory.getLogger(AttackResolutionService.class);
	
	private Village village;
	private GameWorld gameWorld;
	private boolean isResolved=false;
	private boolean isEvasionSuccess=false;
	@Autowired
	private ServiceClient serviceClient;

	
	public AttackResolutionService(Village village, GameWorld gameWorld) {
		this.village = village;
		this.gameWorld = gameWorld;
	}
	
	
	@Override
	public void run() {
		try {
			resolveAttacks();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private void resolveAttacks() throws InterruptedException {
		IncomingAttack attack = this.village.getIncomingAttack();
		Long attackDuration = attack.getDuration();
		if(attackDuration<300) {
			if(Log.isInfoEnabled())
				Log.info("Attack is coming less then 5 min to village "+this.village.getVillageName()+" initiating attack resolution");
			BaseProfile.isExecutionEnable = false;
			while(!isResolved) {
				if(Log.isInfoEnabled())
					Log.info("Current attack time "+attackDuration+" sec....Attack check in another 20 sec for village "+this.village.getVillageName());
				if(!isEvasionSuccess && attackDuration<60) {
					if(Log.isInfoEnabled())
						Log.info("attack duration is "+attackDuration+". Initiating evasion");
					TroopEvasionRequest request = new TroopEvasionRequest();
					request.setVillageId(this.village.getVillageId());
					request.setDestinationName("TB1 (4446)");
					request.setGameWorld(this.gameWorld);
					if(Log.isDebugEnabled())
						Log.debug("evasion request"+request);
					EvasionResponse response = serviceClient.initiateTroopEvasion(request);
					if(response.getStatusCode()==412) {
						if(Log.isDebugEnabled())
							Log.debug("There is no troop present in the village..so attack is resolved");
						isResolved=true;
						isEvasionSuccess=false;
					}else {
						if(Log.isInfoEnabled())
							Log.info("Troop evasion success::"+response);
						isEvasionSuccess=true;
					}
					
				}else if(isEvasionSuccess){
					Thread.sleep(1000*20);
					VillageInfoRequest villageInfoRequest = new VillageInfoRequest();
					villageInfoRequest.setGameWorld(this.gameWorld);
					villageInfoRequest.setLink(Arrays.asList(new String[]{this.village.getLink()}));
					List<Village> villages = serviceClient.getVillageInfo(villageInfoRequest);
					IncomingAttack inAttack = villages.get(0).getIncomingAttack();
					if(inAttack!=null) {
						attackDuration = inAttack.getDuration();
						if(Log.isInfoEnabled())
							Log.info("Troop are evaded:: Attack duration::"+attackDuration+" sec for village "+this.village.getVillageName());
					}else {
						if(Log.isInfoEnabled())
							Log.info("There is no attack :: Attack is resoluted::Returning troops");
						TroopEvasionRequest resolverequest = new TroopEvasionRequest();
						resolverequest.setVillageId(this.village.getVillageId());
						resolverequest.setGameWorld(this.gameWorld);
						if(Log.isDebugEnabled())
							Log.debug("evasion resolve request"+resolverequest);
						Status status = serviceClient.resolveEvasion(resolverequest);
						if("SUCCESS".equals(status.getStatus())) {
							if(Log.isInfoEnabled())
								Log.info("Troop return successfull::evasion complete");
						}
						isResolved=true;
					}
				}else {
					Thread.sleep(1000*20);
				}
			}
		}else {
			if(Log.isInfoEnabled())
				Log.info("Attack time more than 5 mins:::skipping attack resolution now");
		}
		if(Log.isInfoEnabled())
			Log.info("Attack Resolution task done::Back to main thread");
		BaseProfile.isExecutionEnable = true;
		
	}
	
	

}
