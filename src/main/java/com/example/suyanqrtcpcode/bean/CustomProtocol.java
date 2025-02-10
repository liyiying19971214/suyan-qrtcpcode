package com.example.suyanqrtcpcode.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 客户端编码器
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomProtocol implements Serializable {
    private  String id; //发送用户的的id
    private  String content;
}
