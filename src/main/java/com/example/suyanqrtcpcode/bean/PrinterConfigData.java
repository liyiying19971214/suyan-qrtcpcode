package com.example.suyanqrtcpcode.bean;


import lombok.Builder;
import lombok.Data;

/**
 *
 * @author Lyy
 * @package（包）com.eazyjob.suyan.bean
 * @version
 * @descriptio
 * TODO 进行共享这里暂时用sitic来解决问题,后续接入多线程后使用Threadloch来存储这里单机环境不考虑
 *
 */
public class PrinterConfigData {
    private  static   String  bucketTypeId;
    private  static   String  bucketMode;
    private  static   String  bucketTypeName;
    private  static   String  qrCode;
    private  static   String  qrNumber;
    private  static   String  factoryId;
    private  static   String  domainName;
    private  static   String  lastqrNumber;

    public static String getBucketTypeId() {
        return bucketTypeId;
    }

    public static String getBucketTypeName() {
        return bucketTypeName;
    }

    public static void setBucketTypeName(String bucketTypeName) {
        PrinterConfigData.bucketTypeName = bucketTypeName;
    }

    public static void setBucketTypeId(String bucketTypeId) {
        PrinterConfigData.bucketTypeId = bucketTypeId;
    }

    public static String getBucketMode() {
        return bucketMode;
    }

    public static void setBucketMode(String bucketMode) {
        PrinterConfigData.bucketMode = bucketMode;
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



    public static String getFactoryId() {
        return factoryId;
    }

    public static void setFactoryId(String factoryId) {
        PrinterConfigData.factoryId = factoryId;
    }

    public static String getDomainName() {
        return domainName;
    }

    public static void setDomainName(String domainName) {
        PrinterConfigData.domainName = domainName;
    }

    public static String getLastqrNumber() {
        return lastqrNumber;
    }

    public static void setLastqrNumber(String lastqrNumber) {
        PrinterConfigData.lastqrNumber = lastqrNumber;
    }
}
