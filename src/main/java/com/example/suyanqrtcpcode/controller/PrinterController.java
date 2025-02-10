package com.example.suyanqrtcpcode.controller;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;


import cn.hutool.extra.mail.MailUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.suyanqrtcpcode.bean.AjaxJson;
import com.example.suyanqrtcpcode.bean.BucketType;
import com.example.suyanqrtcpcode.bean.PrinterConfigData;
import com.example.suyanqrtcpcode.bean.SelectOptionData;
import com.example.suyanqrtcpcode.constants.HttpUrlFactory;
import com.example.suyanqrtcpcode.constants.WsConstant;
import com.example.suyanqrtcpcode.exceptions.BusinessException;
import com.example.suyanqrtcpcode.core.handle.CellPhoneHandler;
import com.example.suyanqrtcpcode.core.handle.PrinterHandler;
import com.example.suyanqrtcpcode.core.handle.WebSocketServerHandler;
import com.example.suyanqrtcpcode.runner.LoadDataRunner;
import com.example.suyanqrtcpcode.utils.RedisUtils;
import com.example.suyanqrtcpcode.utils.RestTemplateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;


/**
 * @author tangtang
 */
@Controller
@RequestMapping("/printerController")
@PropertySource("classpath:suyan-config.properties")
public class PrinterController {

    private final static Logger LOGGER = LoggerFactory.getLogger(PrinterController.class);

    @Autowired
    private RestTemplate  restTemplate;

    @Autowired
    private RedisUtils redisUtils;

    private final ReentrantLock lock = new ReentrantLock();


    @Value("${redis.maxListLength}")
    private int maxListLength;

    @Value("${redis.surListLength}")
    private int surListLength;


    @Value("${taskEnabled}")
    private  Boolean  taskEnabled;


    // 定义几个类型
    public static final List<SelectOptionData> btList = new ArrayList<>();
    public static final List<SelectOptionData> bmList = new ArrayList<>();

    static {
        bmList.add(new SelectOptionData("01", "芯片桶"));
        bmList.add(new SelectOptionData("02", "二维码桶"));
        bmList.add(new SelectOptionData("03", "二维码芯片桶"));
    }

    @Autowired
    HttpServletRequest request;


    /**
     * 初始化数据,不是项目过大
     */
    @PostConstruct
    public void init() {
        // 调用其他服务器接口获取数据
        JSONObject forObject = restTemplate.getForObject(HttpUrlFactory.HTTPCILENURT+"/bucketLabelForMobile/getBucketTypeList.do", JSONObject.class);
        LOGGER.info("初始化内容" + forObject.toString());
        // 对获取到的数据进行处理
        if (forObject == null)  {LOGGER.info("初始化找不到内容" );return;}
        Integer integer = forObject.getInteger("state");
        if (integer == 1) {
            JSONArray jsonArray = forObject.getJSONArray("BucketTypeList");
            List<BucketType> bucketTypeList = JSON.parseArray(jsonArray.toString(), BucketType.class);
            for (BucketType bucketType : bucketTypeList) {
                btList.add(new SelectOptionData(bucketType.getId(),bucketType.getModelName()));
            }
        }else LOGGER.info("返回内容不正确,请检查接口" );return;

    }


    @RequestMapping("/bindingBucket")
    @ResponseBody
    public void  bindingBucket(@RequestParam String qrCode){
        String url = HttpUrlFactory.getHttpUrl("createQrCodeBucket");
        // 由于注入的问题只能先收取的拿对应的
        HttpHeaders headers = new HttpHeaders();
        // 这里由于设置的是消息头不是消息体所有需要这个设置
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        // 请求参数 param 设置header之后等同于
        // http://xxx/xxx?password=xxxxx
        MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();
        String bucketTypeId = PrinterConfigData.getBucketTypeId();
        paramMap.add("bizBucketTypeId", bucketTypeId);// 设备号，
        paramMap.add("terminalSign", "990008450019737");// 账号密码
        paramMap.add("qrCode", PrinterConfigData.getQrCode());// 账号密码
        paramMap.add("qno", PrinterConfigData.getQrNumber());// 账号密码
        paramMap.add("userName", "shuitongchang");
        paramMap.add("userPwd", "123456");
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(paramMap, headers);
        JSONObject result = restTemplate.postForObject(url, httpEntity, JSONObject.class);
        JSONObject jsonObject = JSON.parseObject(result.toString());
        if (jsonObject != null) {
            JSONObject jo=new JSONObject();
            Integer state = jsonObject.getInteger("state");
            String message = jsonObject.getString("message");
            if (state == 1) {
                //清空内容表示当个二维码完成
                PrinterConfigData.setQrCode(null);
                PrinterConfigData.setQrNumber(null);
                jo.put(WsConstant.msgtype.GENERATE_BUCKET_FLAG, false);
            }
            //需要用到scoker通讯内容
            jo.put(WsConstant.msgtype.GENERATE_BUCKET_MSG,message);
            WebSocketServerHandler.sendAll(jo.toString());
        }
    }

    @RequestMapping("/toPage")
    public String toPage(HttpServletRequest request) throws Exception {
        String path = request.getContextPath();
        String basePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + path;
        String bucketTypeId = PrinterConfigData.getBucketTypeId();
        String bucketMode = PrinterConfigData.getBucketMode();
        SelectOptionData selectOptionData1 = bmList.stream().filter(o -> o.getKey().equals(bucketMode)).findAny()
                .orElse(null);
        SelectOptionData selectOptionData2 = btList.stream().filter(o -> o.getKey().equals(bucketTypeId)).findAny()
                .orElse(null);
        request.setAttribute("bucketTypeId", selectOptionData1==null?"暂无选择":selectOptionData1.getValue());
        request.setAttribute("bucketMode", selectOptionData2==null?"暂无选择":selectOptionData2.getValue());
        request.setAttribute("basePath", basePath);
        request.setAttribute("print_cnt", LoadDataRunner.getDataMapCnt());
        return "printer/toPage";
    }

    @RequestMapping("/getDeviceStatus")
    @ResponseBody
    public Map<String, Object> getDeviceStatus(HttpServletRequest request) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(WsConstant.msgtype.CELLPHONE_CONNECTION_STATUS, CellPhoneHandler.isConnected());
        map.put(WsConstant.msgtype.PRINTER_CONNECTION_STATUS, PrinterHandler.isConnected());
        map.put(WsConstant.msgtype.CELLPHONE_CONNECTED_IP, CellPhoneHandler.getConnectedIp());
        map.put(WsConstant.msgtype.PRINTER_CONNECTED_IP, PrinterHandler.getConnectedIp());
        //检查当前是否有绑定动作,重新绑定内容
        PrinterHandler.clearPrintedContent();
        return map;
    }

    @RequestMapping("/getSessionConfig")
    @ResponseBody
    public Map<String, Object> getSessionConfig() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("soList", btList);
        map.put("bmList", bmList);
        return map;
    }

    @RequestMapping("/saveConfig")
    @ResponseBody
    public AjaxJson saveConfig(@RequestParam String bucketTypeId, @RequestParam final String bucketMode) {
        AjaxJson ajaxJson = new AjaxJson();
        try {
            // 存入到session
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("bucketTypeId", bucketTypeId);
            map.put("bucketMode", bucketMode);
            // 这里未来方便niam进行获取有一定的不安全性质
            PrinterConfigData.setBucketMode(bucketMode);
            PrinterConfigData.setBucketTypeId(bucketTypeId);
            long listLength = redisUtils.getListLength(HttpUrlFactory.QRCODEMANAGEMENTKEYVALUS);
            if(listLength<=surListLength){
                //开始加载数据
                saveRedisCode();
            }
        } catch (Exception e) {
            e.printStackTrace();
            ajaxJson.setSuccess(false);
            ajaxJson.setMsg(e.getMessage());
        }
        return ajaxJson;
    }



    /**
     * @throws Exception
     *
     */
    public void saveRedisCode() throws Exception {
        LOGGER.info("开始加载code");
        // 主机的redis才进行加载数据
        if (!taskEnabled) {
            return;
        }

        // 插入可重入锁

        lock.lock();
        long listLength = redisUtils.getListLength(HttpUrlFactory.QRCODEMANAGEMENTKEYVALUS);
        if(listLength>=maxListLength){
            //减少不必要的锁竞争
            return ;
        }
        LOGGER.info("开始访问调用");

        try {
            // 加载水桶内容信息
            String httpUrl = HttpUrlFactory.getHttpUrl("getQrCodeList");
            RestTemplateUtil restTemplateUtil = new RestTemplateUtil(restTemplate);
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("bizBucketTypeId", PrinterConfigData.getBucketTypeId());
            params.put("num", maxListLength);

            JSONObject result = restTemplateUtil.getRestTemplate(httpUrl, params);

            LOGGER.info("加载完成");
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
                        redisUtils.pushBack(HttpUrlFactory.QRCODEMANAGEMENTKEYVALUS, JSON.toJSONString(map));
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




}
