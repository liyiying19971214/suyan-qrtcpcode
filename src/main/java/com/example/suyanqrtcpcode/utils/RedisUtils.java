package com.example.suyanqrtcpcode.utils;

import java.util.*;

import com.example.suyanqrtcpcode.constants.HttpUrlFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


@Component
public class   RedisUtils {

    @Resource
    public StringRedisTemplate stringRedisTemplate ;



    public List<String> addLua(){
        String script =
                "local keyValue = KEYS[1] " +
                        "local keyList = KEYS[2] " +
                        "local incrResult = redis.call('INCRBY', keyValue, 1) " +
                        "if incrResult then " +
                        "  local poppedValue = redis.call('LPOP', keyList) " +
                        "  return poppedValue " +
                        "end";

        List<String> keys = Arrays.asList(HttpUrlFactory.QRCODEMANAGEMENTCOUNTERKEY, HttpUrlFactory. QRCODEMANAGEMENTGENERATEBUCKET);
        List<String> result = stringRedisTemplate.execute(new DefaultRedisScript<>(script, List.class), keys);
        return  result;
    }
    /**
     * 存入信息到缓存
     */
    public  void  getQrCodeforRedis(String key,String values){
        //加载数据缓存策略
        stringRedisTemplate.opsForValue().set(HttpUrlFactory.QRCODEMANAGEMENTKEYVALUS+key, values);
    }
   

    // 在列表头部插入元素
    public void pushFront(String key, String value) {
        stringRedisTemplate.opsForList().leftPush(key, value);
    }

    // 在列表尾部插入元素
    public  synchronized void pushBack(String key, String value) {
        stringRedisTemplate.opsForList().rightPush(key, value);
    }
    
    
    // 获取列表指定位置的元素
    public String getElement(String key, long index) {
        return stringRedisTemplate.opsForList().index(key, index);
    }

    // 获取列表的长度
    public long getListLength(String key) {
        return  stringRedisTemplate.execute(new RedisCallback<Long>() {
            @Override
            public Long doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.lLen(key.getBytes());
            }
        });
    }

    // 删除第一个元素
    public String removeElement(String key) {
       return stringRedisTemplate.opsForList().leftPop(key);
    }
    
    // 添加内容
    public void putAll(String key, Map<String, String> hash) {
        stringRedisTemplate.opsForHash().putAll("myHashKey", hash);
    }

    public List<String> getElement(String key) {
      return  stringRedisTemplate.opsForList().range(key, 0, -1);
    }
    
    public String getFirstElement (String key) {
        return  stringRedisTemplate.opsForList().index(key, 0);
      }
    
    public Long incr(String key) {
        return stringRedisTemplate.opsForValue().increment(key,1L);
    }

    public Long getCount(String key) {
        String countStr = stringRedisTemplate.opsForValue().get(key);
        if (countStr == null) {
            return 0L;
        }
        return Long.parseLong(countStr);
    }
}
