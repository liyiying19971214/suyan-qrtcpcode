package com.example.suyanqrtcpcode.controller;


import cn.hutool.core.map.MapBuilder;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.suyanqrtcpcode.bean.*;
import com.example.suyanqrtcpcode.constants.HttpUrlFactory;
import com.example.suyanqrtcpcode.constants.WsConstant;
import com.example.suyanqrtcpcode.core.handle.CellPhoneHandler;
import com.example.suyanqrtcpcode.core.handle.PrinterHandler;
import com.example.suyanqrtcpcode.core.handle.WebSocketServerHandler;
import com.example.suyanqrtcpcode.exceptions.BusinessException;
import com.example.suyanqrtcpcode.service.RedisCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * @author tangtang
 */
@Controller
@RequestMapping("/printerController")
@PropertySource("classpath:suyan-config.properties")
@Slf4j
public class PrinterController {

    @Resource
    private RestTemplate  restTemplate;


    @Resource
    private RedisCodeService redisCodeService;


    // 定义几个类型
    public static final List<BucketType> bucketTypeList = new ArrayList<>();
    public static final List<SelectOptionData> bmList = new ArrayList<>();
    public static final List<SelectOptionData> btList = new ArrayList<>();

    static {
        bmList.add(new SelectOptionData("01", "芯片桶"));
        bmList.add(new SelectOptionData("02", "二维码桶"));
        bmList.add(new SelectOptionData("03", "二维码芯片桶"));
    }

    @Resource
    HttpServletRequest request;


    /**
     * 初始化数据,不是项目过大
     */
    @PostConstruct
    public void init() {
        try {
            JSONObject forObject = fetchBucketTypeList();
            if (forObject == null) {
                log.error("初始化找不到内容");
                return;
            }
            processBucketTypeList(forObject);
        } catch (Exception e) {
            log.error("初始化过程中发生异常", e);
        }
    }

    private JSONObject fetchBucketTypeList() {
        return restTemplate.getForObject(HttpUrlFactory.HTTPCILENURT + "/bucketLabelForMobile/getBucketTypeList.do", JSONObject.class);

    }


    private void processBucketTypeList(JSONObject forObject) {
        if (!forObject.containsKey("state") || !forObject.containsKey("BucketTypeList")) {
            log.error("返回内容缺少必要字段",forObject.get("msg"));
            return;
        }
        Integer state = forObject.getInteger("state");
        if (state == 1) {
            JSONArray jsonArray = forObject.getJSONArray("BucketTypeList");
            List<BucketType> bucketTypeList = JSON.parseArray(jsonArray.toString(), BucketType.class);
            PrinterController.bucketTypeList.addAll(bucketTypeList);

            //通过过滤拿到btList内容
            List<SelectOptionData>  sodList  = bucketTypeList.stream().map(m -> new SelectOptionData(m.getId(), m.getModelName())).collect(Collectors.toList());
            btList.addAll(sodList);

        } else {
            log.error("返回内容不正确,请检查接口");
        }
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
    public String toPage() throws Exception {

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
    public AjaxJson getSessionConfig() {
        // 使用链式构造响应对象
        AjaxJson.AjaxJsonBuilder responseBuilder = AjaxJson.builder()
                .success(false)
                .msg("初始化中");
        try {
            String path = request.getContextPath();
            String basePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + path;
            String bucketTypeName = PrinterConfigData.getBucketTypeName();//水桶名称
            String bucketMode = PrinterConfigData.getBucketMode();//二维码方式

            String bucketTypeIdValue = Optional.ofNullable(bucketTypeName)
                    .orElse("暂无选择");

            String bucketModeValue = Optional.ofNullable(bucketMode)
                    .orElse("暂无选择");

            String bucketDmValue = Optional.ofNullable(PrinterConfigData.getDomainName())
                    .orElse("暂无选择");

            //从redis里面取每日数据出来
            int todayCodeNum = redisCodeService.getTodayCodeNum();

            Map<String, Object> map = MapBuilder
                    .create(new HashMap<String, Object>())
                    .put("btList", bucketTypeList)
                    .put("bmList", bmList)
                    .put("bucketTypeIdValue", bucketTypeIdValue)
                    .put("bucketModeValue", bucketModeValue)
                    .put("bucketDmValue", bucketDmValue)
                    .put("basePath", basePath)
                    .put("print_cnt", todayCodeNum)//这里是从redis里面取内容
                    .build();

            responseBuilder
                    .success(true)
                    .msg("获取配置成功")
                    .obj(map);
        } catch (Exception e) {
            log.error("获取配置失败！: ", e);
            responseBuilder.msg(e.getMessage());
        }
        return responseBuilder.build();
    }

    @RequestMapping("/saveConfig")
    @ResponseBody
    public AjaxJson saveConfig(@RequestBody ConfigFormReq configFormReq) {
        // 使用链式构造响应对象
        AjaxJson.AjaxJsonBuilder responseBuilder = AjaxJson.builder()
                .success(false)
                .msg("初始化中");
        try {
            // 参数校验（使用Spring Validation）
            String dmc = validateParams(configFormReq);

            BucketType bucketType = bucketTypeList.stream().filter(b -> configFormReq.getBucketTypeId().equals(b.getId())).findFirst().orElseThrow(()-> new BusinessException("未找到对应的BucketType配置"));

            SelectOptionData bmData = bmList.stream().filter(b -> configFormReq.getBucketModeId().equals(b.getKey())).findFirst().orElseThrow(()-> new BusinessException("未找到对应的BucketMode配置"));

            //TODO 这里后续改的时候考虑下多线程，使用的是元空间，多线程可以考虑用 ThreadLocal
            PrinterConfigData.setBucketTypeName(bucketType.getModelName());//水桶名称
            PrinterConfigData.setBucketTypeId(bucketType.getId());//水桶id
            PrinterConfigData.setFactoryId(bucketType.getFactoryId());//水厂id
            PrinterConfigData.setBucketMode(bmData.getValue());
            PrinterConfigData.setDomainName(dmc);//前缀名称

            // 考虑多线程
            Map<String, Object> map = new ConcurrentHashMap<>();
            map.put("domainName",dmc);
            // 同步执行执行Redis条件检查
            redisCodeService.checkAndReloadCodes(bucketType.getFactoryId(),false);

            // 构造响应（防御性拷贝）
            responseBuilder
                    .success(true)
                    .msg("配置保存成功")
                    .obj(map);
        } catch (Exception e) {
            log.error("系统异常: ", e);
            responseBuilder.msg(e.getMessage());
        }
        return  responseBuilder.build();
    }

    /**
     * 参数校验
     */
    private String validateParams(ConfigFormReq configFormReq) {
        // 检查参数是否为空
        Optional.ofNullable(configFormReq)
                .filter(req -> !StrUtil.isBlank(req.getBucketTypeId()))
                .filter(req -> !StrUtil.isBlank(req.getBucketModeId()))
                .orElseThrow(() -> new BusinessException("参数不能为空"));

        BucketType bucketType = bucketTypeList.stream().filter(b -> configFormReq.getBucketTypeId().equals(b.getId())).findFirst().orElseThrow(()-> new BusinessException("未找到对应的BucketType配置"));

        //比较前缀
        String dmc = configFormReq.getMyCheckbox()
                ? bucketType.getDomainName() + bucketType.getFactoryCode()
                : bucketType.getDomainName();

        if(!dmc.equals(configFormReq.getDomainName())){
            throw new BusinessException("参数不一致重新设置");
        }

        return dmc;
    }

}
