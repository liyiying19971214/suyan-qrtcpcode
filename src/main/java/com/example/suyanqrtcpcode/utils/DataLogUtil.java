package com.example.suyanqrtcpcode.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DataLogUtil {

	private  static final String logFilePath ;
	public static final Logger LOGGER = LoggerFactory
			.getLogger(DataLogUtil.class);
	static {
		PropertiesUtil pu=new PropertiesUtil("suyan-config.properties");
		logFilePath=pu.readProperty("data_log_path");
//		org.apache.log4j.Logger logger = LogManager.getLogger(LOGGER.getName());
//		org.apache.log4j.DailyRollingFileAppender rfa;
//		try {
//			rfa = new org.apache.log4j.DailyRollingFileAppender(
//					new DataLogLayout(), logFilePath,
//					"'.'yyyy-MM-dd");
//
//			logger.addAppender(rfa);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

	}
	public static String getLogFilePath(){
		return logFilePath;
	}

}