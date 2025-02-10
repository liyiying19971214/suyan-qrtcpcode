package com.example.suyanqrtcpcode.runner;

import com.alibaba.fastjson.JSONObject;
import com.example.suyanqrtcpcode.utils.DataLogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.*;


@Component
public class LoadDataRunner implements ApplicationRunner {


	private static Map<String,Integer> scanDataMap = new HashMap<String,Integer>();
	private static List<JSONObject> scanDataList=new ArrayList<JSONObject>();
	private static final String syncLock=new String("syyncLock");

	private final static Logger logger = LoggerFactory.getLogger(LoadDataRunner.class);

	@Override
	public void run(ApplicationArguments arg0) throws Exception {
		File file=new File(DataLogUtil.getLogFilePath());
		SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
		if(file==null || !fmt.format(new Date(file.lastModified())).equals(fmt.format(new Date()))){
			return;
		}
		BufferedReader br=null;
		try{
			br=new BufferedReader(new FileReader(file));
			String line="";
			while((line = br.readLine()) != null){
				String[] os=line.split(",");
				JSONObject jo=new JSONObject();
				jo.put("bucketId",os[1]);
				jo.put("QRCodeStr", os[2]);
				setScanDataMap(jo,false);
			}
		}catch(Exception e){
			logger.error("出错内容",e);
		}finally{
			if(br!=null){
				try{
					br.close();
				}catch(Exception e){
					logger.error("出错内容",e);
				}

			}
		}


	}


	public static void setScanDataMap(JSONObject jo,boolean isLog){
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		//sdf.format(new Date())+","+jo.getString("bucketId")+","+jo.getString("QRCodeStr");
		synchronized (syncLock) {
			String dupFlagStr=null;
			Integer idx=scanDataMap.get(jo.getString("bucketId"));
			if(idx!=null){
				if(!scanDataList.get(idx).getString("QRCodeStr").equals(jo.getString("QRCodeStr"))){
						scanDataList.set(idx, jo);
				}
				dupFlagStr=",duplicate,"+idx;
			}else{
				scanDataList.add(jo);
				idx=scanDataList.size()-1;
				scanDataMap.put(jo.getString("bucketId"),idx);
			}
			if(isLog)
				DataLogUtil.LOGGER.info(sdf.format(new Date())+","+jo.getString("bucketId")+","+jo.getString("QRCodeStr")+","+idx+(dupFlagStr==null?"":dupFlagStr));
		}
	}
	public static int getDataMapCnt(){
		synchronized(syncLock){
			return scanDataList.size();
		}
	}
}
