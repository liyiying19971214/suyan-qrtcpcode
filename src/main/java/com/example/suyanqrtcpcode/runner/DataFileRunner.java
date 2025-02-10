package com.example.suyanqrtcpcode.runner;

import com.example.suyanqrtcpcode.utils.DataLogUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Date;

@PropertySource("classpath:suyan-config.properties")
@Component
public class DataFileRunner implements ApplicationRunner {

	@Value("${date_delete_day}")
	private  String  path;

	@Value("${date_delete_day}")
	private int delelteDay;
	@Override
	public void run(ApplicationArguments arg0) throws Exception {
		File f=new File(DataLogUtil.getLogFilePath());
		final String fileName=DataLogUtil.getLogFilePath().substring(DataLogUtil.getLogFilePath().lastIndexOf("\\")+1);


		File[] files=f.getParentFile().listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				if(name.startsWith(fileName))
					return true;
				else
					return false;
			}
		});
		long nowL=new Date().getTime();
		long deldaymil=delelteDay*24*60*60*1000;
		for(File file:files){

			if(nowL-file.lastModified()>deldaymil){
				file.delete();
			}
		}
	}

}
