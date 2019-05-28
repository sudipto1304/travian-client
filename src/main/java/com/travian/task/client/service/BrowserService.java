package com.travian.task.client.service;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.travian.task.client.request.AccountInfoRequest;
import com.travian.task.client.response.Fields;
import com.travian.task.client.response.Resource;
import com.travian.task.client.response.Task;
import com.travian.task.client.response.Village;

@Service
public class BrowserService {

	private static final Logger Log = LoggerFactory.getLogger(BrowserService.class);

	public void initiateAdventure(WebDriver driver) {
		String heroStatus = driver.findElement(By.className("heroStatusMessage")).getText();
		if (heroStatus.contains("in home village")) {
			driver.findElement(By.className("adventureWhite")).click();
			driver.findElement(By.xpath("(//a[@class='gotoAdventure arrow'])[1]")).click();
			driver.findElement(By.className("startAdventure")).click();
			if (Log.isInfoEnabled())
				Log.info("Hero sent to adventure");
		} else {
			if (Log.isInfoEnabled())
				Log.info("Hero not in home village::Skip Adventure");
			return;
		}
	}

	public List<Village> getVillageOverview(WebDriver driver) {
		List<Village> villages = new ArrayList<Village>();
		driver.findElement(By.className("overviewWhite")).click();
		List<WebElement> elements = driver.findElements(By.xpath("(//table[@id='overview']//tbody//tr)"));
		elements.forEach(e -> {
			Village village = new Village();
			village.setVillageName(e.findElement(By.xpath("(//td[@class='vil fc']//a)")).getText());
			String link = e.findElement(By.xpath("(//td[@class='vil fc']//a)")).getAttribute("href");
			link = link.substring(link.indexOf("=") + 1, link.length());
			village.setVillageId(link);
			village.setOngoingConstruction(e.findElements(By.xpath("(//td[@class='bui']//a)")).size());
			villages.add(village);
		});
		return villages;

	}

	public void executeVillageTaskes(AccountInfoRequest request, WebDriver driver, Village village, Task task) {
		driver.get("https://" + request.getHost() + "/dorf1.php?newdid=" + village.getVillageId() + "&");
		Resource resource = new Resource();
		resource.setWarehouseCapacity(
				Integer.valueOf(replaceChar(driver.findElement(By.id("stockBarWarehouse")).getText())));
		resource.setGranaryCapacity(
				Integer.valueOf(replaceChar(driver.findElement(By.id("stockBarGranary")).getText())));
		resource.setClay(Integer.valueOf(replaceChar(driver.findElement(By.id("l2")).getText())));
		resource.setWood(Integer.valueOf(replaceChar(driver.findElement(By.id("l1")).getText())));
		resource.setIron(Integer.valueOf(replaceChar(driver.findElement(By.id("l2")).getText())));
		resource.setCrop(Integer.valueOf(replaceChar(driver.findElement(By.id("l4")).getText())));
		resource.setWoodProduction(Integer.valueOf(replaceChar(driver
				.findElement(By.xpath("(//table[@id='production']//tbody//tr//td[@class='num'])[1]")).getText())));
		resource.setClayProduction(Integer.valueOf(replaceChar(driver
				.findElement(By.xpath("(//table[@id='production']//tbody//tr//td[@class='num'])[2]")).getText())));
		resource.setIronProduction(Integer.valueOf(replaceChar(driver
				.findElement(By.xpath("(//table[@id='production']//tbody//tr//td[@class='num'])[3]")).getText())));
		resource.setCropProduction(Integer.valueOf(replaceChar(driver
				.findElement(By.xpath("(//table[@id='production']//tbody//tr//td[@class='num'])[4]")).getText())));
		getVillageFields(resource, driver, request.getHost());
		village.setResource(resource);
		getBuildings(driver, village, request.getHost());
		
	}

	private void getVillageFields(Resource resource, WebDriver driver, String host) {
		List<Fields> fields = new ArrayList<Fields>();
		List<WebElement> fieldElm = driver.findElements(By.xpath("(//map[@id='rx']//area)"));
		fieldElm.forEach(e -> {
			try {
				Fields field = new Fields();
				field.setLink(e.getAttribute("href").replace("https://" + host, ""));
				field.setId(Integer.valueOf(e.getAttribute("href").replace("https://" + host + "/build.php?id=", "")));
				String alt = e.getAttribute("alt");
				alt = alt.substring(alt.indexOf("Level") + 5, alt.length()).trim();

				field.setLevel(Integer.valueOf(alt));
				fields.add(field);
			} catch (Exception e2) {
				// TODO: handle exception
			}

		});
		resource.setFields(fields);
	}
	
	
	private void getBuildings(WebDriver driver, Village village, String host) {
		driver.findElement(By.className("villageBuildings")).click();
		
	}

	private String replaceChar(String input) {
		return input.replace(")", "").replaceAll("\\u202C", "").replaceAll("\\u202D", "").replaceAll("\\u2212", "-")
				.replaceAll(",", "");
	}

}
