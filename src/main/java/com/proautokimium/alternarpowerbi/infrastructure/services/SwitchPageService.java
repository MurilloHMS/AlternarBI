package com.proautokimium.alternarpowerbi.infrastructure.services;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;

public class SwitchPageService {

	public void nextPage(int pageActual, int pageTotal) throws AWTException, InterruptedException {
		Robot robot = new Robot();
		
		if(pageActual == pageTotal) {
			goToFirstPage(pageTotal);
		}else {
			robot.keyPress(KeyEvent.VK_TAB);
			Thread.sleep(50);
			robot.keyPress(KeyEvent.VK_ENTER);
			
			Thread.sleep(100);
			
			robot.keyRelease(KeyEvent.VK_ENTER);
			Thread.sleep(50);
			robot.keyRelease(KeyEvent.VK_TAB);
		}
	}
	
	public void updateDatabase() throws AWTException, InterruptedException {
		Thread.sleep(300);
		
		Robot robot = new Robot();
		
		robot.keyPress(KeyEvent.VK_ALT);
		Thread.sleep(300);
		robot.keyPress(KeyEvent.VK_H);
		Thread.sleep(300);
		robot.keyPress(KeyEvent.VK_R);
		Thread.sleep(300);
		robot.keyRelease(KeyEvent.VK_ALT);
		robot.keyRelease(KeyEvent.VK_H);
		robot.keyRelease(KeyEvent.VK_R);
	}
	
	public void goToFirstPage(int pages) throws AWTException, InterruptedException {
		Robot robot = new Robot();
		
		
		robot.keyPress(KeyEvent.VK_SHIFT);
		Thread.sleep(50);
		
		for(int i = 0; i < pages -1; i++) {
			robot.keyPress(KeyEvent.VK_TAB);
			Thread.sleep(50);
			robot.keyRelease(KeyEvent.VK_TAB);
		}
		Thread.sleep(50);
		robot.keyRelease(KeyEvent.VK_SHIFT);
	}
}
