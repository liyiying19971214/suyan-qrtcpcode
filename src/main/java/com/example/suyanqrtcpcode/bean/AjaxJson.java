package com.example.suyanqrtcpcode.bean;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import lombok.Builder;
import lombok.Data;

/**
 * $.ajax后需要接受的JSON
 * 
 * @author
 * 
 */

@Builder
@Data
public class AjaxJson {

	private boolean success = true;// 是否成功
	private String msg = "操作成功";// 提示信息
	private Object obj = null;// 其他信息
	private Map<String, Object> attributes;// 其他参数

	public String getJsonStr(){
		JSONObject obj = new JSONObject();
		obj.put("success", this.isSuccess());
		obj.put("msg", this.getMsg());
		obj.put("obj", this.obj);
		obj.put("attributes", this.attributes);
		return obj.toJSONString();
	}
}
