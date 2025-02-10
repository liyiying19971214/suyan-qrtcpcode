package com.example.suyanqrtcpcode.core.service;

import com.alibaba.fastjson.JSONObject;

public interface StorageService {

	public void save2Xls(String bucketId,String QRCodeStr) throws Exception;
	
	public void save2MyLog(JSONObject jo) throws Exception;
}
