package com.proautokimium.alternarpowerbi.infrastructure.util;

import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggerConfig {
	private static boolean configured = false;
	private static Handler fileHandler;
	private static Handler consoleHandler;
	
	public static Logger getLogger(String className) {
		Logger logger = Logger.getLogger(className);
		
		if(!className.startsWith("com.proautokimium"))
			return logger;
		
		if(!configured) {
			try {
				String logDirPath = System.getProperty("user.home") + File.separator + "java-logs";
				File logDir = new File(logDirPath);
				
				if(!logDir.exists() && logDir.mkdirs()) {
					System.err.println("Falha ao criar o diretório de logs: " + logDirPath);
				}
				
				fileHandler = new FileHandler(logDirPath + File.separator + "AlternarBI.log", true);
				fileHandler.setFormatter(new SimpleFormatter());
				fileHandler.setLevel(Level.ALL);
				
				consoleHandler = new ConsoleHandler();
				consoleHandler.setFormatter(new SimpleFormatter());
				consoleHandler.setLevel(Level.ALL);
				
				configured = true;
			} catch (Exception e) {
				System.err.println("Erro ao configurar o logger: " + e.getMessage());
			}
		}
		
		logger.setUseParentHandlers(false);
		logger.setLevel(Level.ALL);
		
		for(Handler h: logger.getHandlers())
			logger.removeHandler(h);
		
		logger.addHandler(fileHandler);
		logger.addHandler(consoleHandler);
		
		return logger;
	}
}
