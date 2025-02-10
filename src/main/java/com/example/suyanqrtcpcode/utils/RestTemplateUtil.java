package com.example.suyanqrtcpcode.utils;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONObject;

public class RestTemplateUtil {

    private final RestTemplate restTemplate;

    public RestTemplateUtil(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 直接绑定水桶信息
     * @param url 接口URL
     * @param paramMap 请求参数
     * @return JSONObject类型的响应结果
     */
    public JSONObject postRestTemplate(String url, MultiValueMap<String, String> paramMap) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(paramMap, headers);
        return restTemplate.postForObject(url, httpEntity, JSONObject.class);
    }

    /**
     * 发送GET请求
     * @param url 接口URL
     * @param params 请求参数
     * @return JSONObject类型的响应结果
     */
    public JSONObject getRestTemplate(String baseUrl, Map<String, Object> params) throws Exception {
        // 参数校验
        if (baseUrl == null || baseUrl.isEmpty() || params == null || params.isEmpty()) {
            throw new IllegalArgumentException("Invalid input parameters");
        }
        // 拼接参数
        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        urlBuilder.append("?");
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            urlBuilder.append(entry.getKey()).append("=").append(entry.getValue().toString()).append("&");
        }
        String url = urlBuilder.toString();
        url = url.substring(0, url.length() - 1);
        return restTemplate.getForObject(url, JSONObject.class);
    }
}