package com.example.suyanqrtcpcode.bean;


import lombok.Data;

@Data
public class ConfigFormReq  implements java.io.Serializable{
    private   String  bucketTypeId;
    private   String  bucketModeId;
    private   String  domainName;
    private   Boolean myCheckbox;
}
