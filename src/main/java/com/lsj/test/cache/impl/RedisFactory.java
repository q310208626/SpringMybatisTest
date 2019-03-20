package com.lsj.test.cache.impl;

import com.google.gson.Gson;
import com.lsj.test.cache.interfaces.ICache;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.xml.sax.InputSource;
import redis.clients.jedis.Jedis;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @descriptions:
 * @version: v1.0.0
 * @author: linsj3
 * @create: 2019-03-18 21:06
 **/
@Component
public class RedisFactory{

    private static String redisServer;
    private static String redisPassword;
    // 需要缓存的缓存类，一个类对应一个表
    private static List<Class> cacheList;

    @Autowired
    private static ApplicationContext context;
    private static Jedis jedisClient;
    static{
        try {
            // 初始化属性
            init();
            // 加载配置文件
            loadRedisConfig();
            // 初始化redis属性
            initAfterLocalConfig();
            // 缓存数据到redis
            cacheDatas();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initAfterLocalConfig() throws Exception{
        if(!StringUtils.isEmpty(redisServer) && !StringUtils.isEmpty(redisPassword)){
            jedisClient = new Jedis(redisServer);
            jedisClient.auth(redisPassword);

            try{
                // 测试redis连通性
                jedisClient.ping();
            }catch (Exception e){
                System.out.println("Redis服务不通["+redisServer+"]");
            }
        }else{
            throw new Exception("redisServer 或 redisPassword 为空");
        }
    }

    public static void init() throws Exception{
        context = new ClassPathXmlApplicationContext("applicationContext.xml");
        cacheList = new ArrayList<Class>();
    }

    public static void cacheDatas() {

        // 用以Map与String间的转换
        Gson gson = new Gson();
        // 循环获取缓存类，通过ICache共有方法获取数据库数据
        for (Class cacheClass : cacheList) {
            try {
                Object object = context.getBean(cacheClass.getSimpleName());
                Method getDataMapsMethod = cacheClass.getMethod("getDataMaps");

                List<Map<String,String>> dataMaps = (List<Map<String, String>>) getDataMapsMethod.invoke(object);

                // 获取要作为redis map 中key的字段
                Method specialKeyMethod = cacheClass.getMethod("specialKey");
                String keyField = String.valueOf(specialKeyMethod.invoke(object));

                for (Map<String, String> dataMap : dataMaps) {
                    String key = dataMap.get(keyField);
                    // 由于Map实现了序列化接口，直接toString()作为value
                    if(!StringUtils.isEmpty(key)){
                        jedisClient.hset(cacheClass.getName(),key,gson.toJson(dataMap));
                    }
                }

            } catch (NoSuchMethodException e) {
                System.out.println("缓存类没有getMaps方法");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public static void loadRedisConfig() throws Exception{
        String redisConfigPath = "src/main/resources/redisConfig.xml";
        InputSource redisInputSource = new InputSource(redisConfigPath);
        SAXReader saxReader = new SAXReader();
        try {
            Document document = saxReader.read(redisInputSource);
            Element rootElement = document.getRootElement();

            // 初始化redisSource
            Element sourceElement = rootElement.element("source");
            redisServer = sourceElement.element("server").getStringValue();
            redisPassword = sourceElement.element("password").getStringValue();

            //初始化需要缓存的ICache对象
            Element tableElement = rootElement.element("tables");
            Iterator<Element> tableIterator = tableElement.elementIterator("table");

            while(tableIterator.hasNext()){
                String cacheImplClass = tableIterator.next().attributeValue("value");
                Class cacheClass = null;
                try {
                    cacheClass = RedisFactory.class.getClassLoader()
                            .loadClass(cacheImplClass);
                    cacheClass.asSubclass(ICache.class);
                    cacheList.add(cacheClass);
                } catch (ClassNotFoundException e) {
                    System.out.println("缓存类["+cacheImplClass+"]找不到");
                }catch (ClassCastException e){
                    System.out.println("类["+cacheImplClass+"]不是缓存类，不加载");
                }
            }

        } catch (DocumentException e) {
            System.out.println("构造redis配置文件出错");
        }
    }

    public Jedis getJedisClient(){
        return jedisClient;
    }
}
