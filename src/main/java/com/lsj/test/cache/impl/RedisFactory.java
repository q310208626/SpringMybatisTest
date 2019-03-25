package com.lsj.test.cache.impl;

import com.lsj.test.cache.interfaces.ICache;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.xml.sax.InputSource;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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


    private static ApplicationContext context;
    private static JedisConnectionFactory jedisConnectionFactory;
    private static RedisTemplate redisTemplate;
    private static RedisConnection redisConnection;
    static{
        try {
            // 初始化属性
            init();
            // 加载配置文件
            loadRedisConfig();
            // 初始化redis属性
            initAfterLoadConfig();
            // 缓存数据到redis
            cacheDatas();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initAfterLoadConfig() throws Exception{

    }

    public static void init() throws Exception{
        context = new ClassPathXmlApplicationContext("applicationContext.xml");
        jedisConnectionFactory = (JedisConnectionFactory) context.getBean("jedisConnectionFactory");
        redisTemplate = (RedisTemplate) context.getBean("redisTemplate");
        cacheList = new ArrayList<Class>();
    }

    public static void cacheDatas() {


        // 循环获取缓存类，通过ICache共有方法获取数据库数据
        for (Class cacheClass : cacheList) {
            try{
                Class beanClass = null;

                // 获取cache实现类的泛型,以获取到bean中的数据
                Type returnType = cacheClass.getGenericSuperclass();
                if (returnType instanceof ParameterizedType){
                    returnType = ((ParameterizedType) returnType).getActualTypeArguments()[0];
                    beanClass = (Class<?>) returnType;
                }


                // 获取要缓存的数据
                Object object = context.getBean(cacheClass.getSimpleName());
                Method getDataMapsMethod = cacheClass.getMethod("getDataMaps");
                List beans = (List) getDataMapsMethod.invoke(object);

                // 获取要作为redis map 中key的字段
                Method specialKeyMethod = cacheClass.getMethod("specialKey");
                String keyField = String.valueOf(specialKeyMethod.invoke(object));

                if( null != beanClass){
                    // 获取作为key的字段的get方法
                    PropertyDescriptor propertyDescriptor = new PropertyDescriptor(keyField,beanClass);
                    Method getSpecialValueMethod = propertyDescriptor.getReadMethod();

                    redisConnection = jedisConnectionFactory.getConnection();
                    // 循环缓存数据
                    for (Object bean  : beans) {
                        // 获取指定字段的值作为key
                        String specialValue = String.valueOf(getSpecialValueMethod.invoke(bean));
                        if(!StringUtils.isEmpty(specialValue)){
                            // 获取序列化bean对象
                            byte[] beanByte = redisTemplate.getHashValueSerializer().serialize(bean);
                            redisConnection.hSet(cacheClass.getName().getBytes(),specialValue.getBytes(),beanByte);
                        }
                    }


                }

            } catch (NoSuchMethodException e) {
                System.out.println("缓存类没有getMaps方法");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IntrospectionException e) {
                System.out.println(cacheClass+"::specialKey 返回的字段在Bean中不存在");
                e.printStackTrace();
            } finally {
                if (null != redisConnection){
                    redisConnection.close();
                }
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

//            // 初始化redisSource
//            Element sourceElement = rootElement.element("source");
//            redisServer = sourceElement.element("server").getStringValue();
//            redisPassword = sourceElement.element("password").getStringValue();

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

    public RedisConnection getRedisConnection(){
        redisConnection = jedisConnectionFactory.getConnection();
        return redisConnection;
    }

    public RedisTemplate getRedisTemplate(){
        if( null == redisTemplate){
            redisTemplate = (RedisTemplate) context.getBean("redisTemplate");
        }
        return redisTemplate;
    }
}
