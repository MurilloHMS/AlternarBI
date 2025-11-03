package com.proautokimium.alternarpowerbi.infrastructure.services;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import com.proautokimium.alternarpowerbi.infrastructure.util.LoggerConfig;

public class SwitchPageService {

    private static final int KEY_PRESS_DELAY = 50;
    private static final int KEY_ACTION_DELAY = 100;
    
    private static final Logger LOGGER = LoggerConfig.getLogger(SwitchPageService.class.getName());

    private Robot robot;

    public SwitchPageService() {
        try {
            this.robot = new Robot();
            this.robot.setAutoDelay(10);
        } catch (AWTException e) {
        	LOGGER.severe("Erro ao inicializar Robot: " + e.getMessage());
            throw new RuntimeException("Erro ao inicializar Robot", e);
        }
    }

    public void nextPage() throws InterruptedException {
        pressTab();
        Thread.sleep(KEY_ACTION_DELAY);
        pressEnter();
    }

    public void goToFirstPage(int totalPages) throws InterruptedException {
        robot.keyPress(KeyEvent.VK_SHIFT);
        Thread.sleep(KEY_PRESS_DELAY);

        for (int i = 0; i < totalPages - 1; i++) {
            pressAndRelease(KeyEvent.VK_TAB, KEY_PRESS_DELAY);
        }

        robot.keyRelease(KeyEvent.VK_SHIFT);
        Thread.sleep(KEY_PRESS_DELAY);
        pressEnter();
    }

    public void firstPage() throws InterruptedException {
        pressEnter();
    }

    private void pressTab() throws InterruptedException {
        robot.keyPress(KeyEvent.VK_TAB);
        Thread.sleep(KEY_PRESS_DELAY);
        robot.keyRelease(KeyEvent.VK_TAB);
    }

    private void pressEnter() throws InterruptedException {
        robot.keyPress(KeyEvent.VK_ENTER);
        Thread.sleep(KEY_ACTION_DELAY);
        robot.keyRelease(KeyEvent.VK_ENTER);
    }

    private void pressAndRelease(int keyCode, long delayAfter) throws InterruptedException {
        robot.keyPress(keyCode);
        Thread.sleep(KEY_PRESS_DELAY);
        robot.keyRelease(keyCode);
        Thread.sleep(delayAfter);
    }
}
