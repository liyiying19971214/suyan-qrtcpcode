package com.example.suyanqrtcpcode.bean;


/**
 * 
 * @author Lyy	
 * @package（包）com.eazyjob.suyan.bean
 * @version 
 * @descriptionTODO这里由于mina无法和Servlet AP，由不影想映入一些redis等大框架内容
 * 进行共享这里暂时用sitic来解决问题,后续大佬找到了解决办法进行告知一下
 */
public class PrinterConfigData {
    private  static   String  bucketTypeId;
    private  static    String   bucketMode;
    private  static   String   qrCode;
    private  static   String   qrNumber;
    private static    String    bId;

    //最近保存的一个num
    private static    String    lastqrNumber;

    public static String getLastqrNumber() {
        return lastqrNumber;
    }

    public static void setLastqrNumber(String lastqrNumber) {
        PrinterConfigData.lastqrNumber = lastqrNumber;
    }

    public static String getbId() {
        return bId;
    }

    public static void setbId(String bId) {
        PrinterConfigData.bId = bId;
    }

    public static synchronized String getBucketTypeId() {
        return bucketTypeId;
    }

    public  static synchronized void setBucketTypeId(String newBucketTypeId) {
        bucketTypeId = newBucketTypeId;
    }

    public   static synchronized  String getBucketMode() {
        return bucketMode;
    }

    public  static synchronized  void setBucketMode(String  newBucketMode) {
        bucketMode = newBucketMode;
    }

    public static String getQrCode() {
        return qrCode;
    }

    public static void setQrCode(String qrCode) {
        PrinterConfigData.qrCode = qrCode;
    }

    public static String getQrNumber() {
        return qrNumber;
    }

    public static void setQrNumber(String qrNumber) {
        PrinterConfigData.qrNumber = qrNumber;
    }
    
    
}
