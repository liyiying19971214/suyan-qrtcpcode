package com.example.suyanqrtcpcode.constants;

import java.util.HashMap;
import java.util.Map;

public class HttpUrlFactory {

    private  static  final  Map<String,String> HTTP_METHOD_MAP=new  HashMap<>();
    
    //public static  final  String  HTTPCILENURT="http://wf.suyantek.com/suyan-web/rest/";
    
    public  static final String  DOMAIN_NAME="http://cww.96656.com.cn/";

    public static  final  String  HTTPCILENURT="http://127.0.0.1:8088/suyan-web/rest/";

    public static final String  QRCODEMANAGEMENTKEYVALUS ="qrCodeManagement:keyValus";
    
    public static final String  QRCODEMANAGEMENTGENERATEBUCKET ="qrCodeManagement:generateBucket";
    
    public static final String  QRCODEMANAGEMENTCOUNTERKEY ="qrCodeManagement:counterKey";


    public static final String  QRCODEMANAGEMENTERRORBUCKET ="qrCodeManagement:errorBucket";


    //需要销毁的key
    public static final String  QRCODEMANAGEMENTDESTRUCTIONKEY ="qrCodeManagement:destructionKey";

    //采用工厂模式
     static {
         HTTP_METHOD_MAP.put("getQrCodeIndex",HTTPCILENURT+ "/bucketLabelForMobile/getQrCodeIndex.do");
         HTTP_METHOD_MAP.put("getQrCode", HTTPCILENURT+"/bucketLabelForMobile/getQrCode.do");
         HTTP_METHOD_MAP.put("createQrCodeBucket", HTTPCILENURT+"/bucketLabelForMobile/createQrCodeBucket.do");
         HTTP_METHOD_MAP.put("scrapBucket", HTTPCILENURT+"/bucketLabelForMobile/save.do");
         HTTP_METHOD_MAP.put("getQrCodeList", HTTPCILENURT+"/bucketLabelForMobile/getQrCodeList.do");
     }
     
     
     
    public static String getHttpUrl(String  name) {
        return HTTP_METHOD_MAP.get(name);
    }
}
