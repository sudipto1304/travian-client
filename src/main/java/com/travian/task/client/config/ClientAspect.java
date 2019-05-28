package com.travian.task.client.config;

import java.util.Random;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Aspect
@Configuration
public class ClientAspect {

	private static final Logger Log = LoggerFactory.getLogger(ClientAspect.class);

	

	@Around("execution(* org.openqa.selenium.htmlunit.HtmlUnitWebElement.*(..)) && execution(* org.openqa.selenium.support.events.EventFiringWebDriver.*(..)) && execution(* org.openqa.selenium.remote.RemoteWebElement.*(..))")
	public void arountClick(ProceedingJoinPoint joinPoint) throws Throwable {
		long startTime = System.currentTimeMillis();
		Random r = new Random();
		int rand = r.ints(1, (5 + 1)).limit(1).findFirst().getAsInt();
		Thread.sleep(rand * 1000);
		joinPoint.proceed();
		long timeTaken = System.currentTimeMillis() - startTime;
		Log.info("Time Taken by {} is {}", joinPoint, timeTaken);
	}

}
