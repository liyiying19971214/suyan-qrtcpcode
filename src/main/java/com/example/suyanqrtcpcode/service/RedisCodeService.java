package com.example.suyanqrtcpcode.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.suyanqrtcpcode.bean.PrinterConfigData;
import com.example.suyanqrtcpcode.constants.HttpUrlFactory;
import com.example.suyanqrtcpcode.exceptions.BusinessException;
import com.example.suyanqrtcpcode.schedule.LoadingQrCodeExecutor;
import com.example.suyanqrtcpcode.utils.RedisUtils;
import com.example.suyanqrtcpcode.utils.RestTemplateUtil;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;


@Slf4j
@Service
public class RedisCodeService {

    @Resource
    private RedisUtils redisUtils;

    private final ReentrantLock lock = new ReentrantLock();

    @Value("${taskEnabled}")
    private  Boolean  taskEnabled;

    @Value("${redis.maxListLength}")
    private int maxListLength;

    @Resource
    private RestTemplate restTemplate;

    @Value("${redis.surListLength}")
    private int surListLength;


    @Setter
    private  LoadingQrCodeExecutor loadingQrCodeExecutor;

    public boolean needReloadCodes(String factoryId) {
        long currentSize = redisUtils.getListLength(HttpUrlFactory.QRCODEMANAGEMENTKEYVALUS + ":" + factoryId);
        return currentSize <= surListLength;
    }


    /**
     * 取出拿到并删除第一个元素
     */
    public  String   removeFirstCode(String  factoryId){
        String firstElement= redisUtils.removeElement(HttpUrlFactory.QRCODEMANAGEMENTKEYVALUS +":"+ factoryId);
        if(StringUtils.isEmpty(firstElement)){
            throw new BusinessException("电脑二维码内容已用完,重试");
        }
        return  firstElement;
    }



    public  void    pushCodeByMap(String  factoryId) throws Exception{
        Map<String, String> map=new HashMap<>();
        map.put("terminalSign", "358406100096392");
        map.put("bizBucketTypeld", PrinterConfigData.getBucketTypeId());
        map.put("qno", PrinterConfigData.getQrNumber());
        map.put("qrCode", PrinterConfigData.getQrCode());
        map.put("userName", "laoshanadmin");
        map.put("userPwd", "123456");
        redisUtils.pushBack(HttpUrlFactory.QRCODEMANAGEMENTGENERATEBUCKET, JSON.toJSONString(map));
    }


    /**
     * Redis队列检查与重载
     */
    public void checkAndReloadCodes(String factoryId, Boolean isAsyn) {
        if (StrUtil.isBlank(factoryId)) throw new BusinessException("没有找到打印的水桶类型信息请重新配置！");
        try {
            if (needReloadCodes(factoryId)) {
                if (isAsyn) {
                    loadingQrCodeExecutor.myMethod(factoryId);
                } else {
                    saveRedisCode(factoryId);
                }
            }
        } catch (Exception e) {
            throw new BusinessException(e);
        }
    }


    /**
     * 加载code内容同步方法
     *
     */
    public void saveRedisCode(String factoryId) throws Exception {
        log.info("开始加载code");
        // 主机的redis才进行加载数据
        if (!taskEnabled) {
            return;
        }
        // 插入可重入锁
        lock.lock();
        log.info("开始访问调用");
        try {
            // 加载水桶内容信息
            String httpUrl = HttpUrlFactory.getHttpUrl("getQrCodeList");
            RestTemplateUtil restTemplateUtil = new RestTemplateUtil(restTemplate);
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("bizBucketTypeId", PrinterConfigData.getBucketTypeId());
            params.put("num", maxListLength);

            JSONObject result = restTemplateUtil.getRestTemplate(httpUrl, params);

            log.info("加载完成");
            if (result != null) {
                int intValue = result.getIntValue("state");
                if (intValue == 1) {
                    JSONObject grCodeListObj = result.getJSONObject("qrCodeList");
                    // 遍历键和值
                    List<String> list = new ArrayList<>();
                    for (String key : grCodeListObj.keySet()) {
                        //把key放入list中
                        list.add(key);
                    }
                    // 排序取值
                    Collections.sort(list);
                    for (String key : list) {
                        Map<String, String> map = new HashMap<>();
                        map.put("index", key);
                        map.put("code", grCodeListObj.getString(key));
                        redisUtils.pushBack(HttpUrlFactory.QRCODEMANAGEMENTKEYVALUS +":"+factoryId , JSON.toJSONString(map));
                    }
                } else {
                    String message = result.getString("message");
                    throw new BusinessException(message);
                }
            } else {
                throw new BusinessException("加载二维码请求出错");
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 拿到今天的打码数量
     */
    public int getTodayCodeNum() throws  Exception{
        LocalDate now = LocalDate.now();
        String format = now.format(DateTimeFormatter.ISO_DATE);
        return redisUtils.getTodayCodeNum(format);
    }
}


