package com.example.suyanqrtcpcode.bean;


import lombok.Data;

@Data
public class SelectOptionData {
    private  String key;
    private  String value;

    public SelectOptionData(String key, String value) {
        this.key = key;
        this.value = value;
    }

}
