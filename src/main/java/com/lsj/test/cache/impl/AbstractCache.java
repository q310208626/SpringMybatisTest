package com.lsj.test.cache.impl;

import com.google.gson.Gson;
import com.lsj.test.cache.interfaces.ICache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;

import java.lang.reflect.*;
import java.util.*;

/**
 * @descriptions: 缓存抽象类
 * @version: v1.0.0
 * @author: linsj3
 * @create: 2019-03-18 20:06
 **/
@Component
public abstract class AbstractCache<T> implements ICache {

    @Autowired
    RedisFactory redisFactory;

    // bean 转换成 Map
    public Map beanToMap(T t) {
        Map<String,String> map = new HashMap();
        ;
        // 获取实现类的泛型，不是子类，是子类要操作的bean对象
        Type type = this.getClass().getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            Type paramType = ((ParameterizedType) type).getActualTypeArguments()[0];
            Class<T> clazz = (Class) paramType;
            Field[] fields = clazz.getDeclaredFields();

            List<Field> fieldList = Arrays.asList(fields);
            Iterator<Field> fieldIterator = fieldList.iterator();

            // 循环遍历所有字段，对有对应的public的getField()方法的字段,调用方法放入map中
            while (fieldIterator.hasNext()) {
                Field field = fieldIterator.next();
                String fieldName = field.getName();
                String getMethodName = "get" + fieldName.substring(0, 1).toUpperCase()
                        + fieldName.substring(1, fieldName.length());
                try {
                    Method getMethod = clazz.getMethod(getMethodName);
                    String fieldValueString = String.valueOf(getMethod.invoke(t));
                    map.put(field.getName(), fieldValueString);
                } catch (NoSuchMethodException e) {
                    System.out.println("没有[" + getMethodName + "]该方法");
                } catch (IllegalAccessException e) {
                    System.out.println("无法访问[" + getMethodName + "]方法");
                } catch (InvocationTargetException e) {
                    System.out.println("调用方法出错[" + getMethodName + "]");
                }
            }


        }
        return map;
    }

    public List<Map> beansToMaps(List<T> ts) {
        List<Map> beanMaps = null;
        if (null != ts) {
            beanMaps = new ArrayList<Map>(ts.size());

            for (T t : ts) {
                beanMaps.add(beanToMap(t));
            }
        }
        return beanMaps;
    }

    // map转换成Bean
    public T mapToBean(Map<String,String> beanMap){
        T t = null;
        if (null != beanMap && beanMap.size() > 0) {
            // 获取实现类的泛型
            Type type = this.getClass().getGenericSuperclass();
            if (type instanceof ParameterizedType) {
                Type paramType = ((ParameterizedType) type).getActualTypeArguments()[0];
                Class<T> clazz = (Class) paramType;
                try {
                    t = clazz.newInstance();
                    // 扫描map的字段
                    for (String x : beanMap.keySet()) {
                        try {
                            Field field = clazz.getDeclaredField(x);
                            String getMethodName = "set" + fieldConvToMethod(field.getName());

                            Type setterParamType = field.getType();
                            Method getMethod = clazz.getMethod(getMethodName, (Class) setterParamType);
                            getMethod.invoke(t, objectConvertType(beanMap.get(x), setterParamType));
                        } catch (NoSuchFieldException e) {
                            System.out.println("没有该字段[" + x + "],跳过处理");
                        } catch (NoSuchMethodException e) {
                            System.out.println("没有该字段的set方法[" + x + "],跳过处理");
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }

                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        return t;
    }

    // field--->Field
    public String fieldConvToMethod(String field){
        StringBuffer methodName = new StringBuffer(field);
        if(!StringUtils.isEmpty(field) && field.length() > 0){
            methodName.replace(0,1,field.substring(0,1).toUpperCase());
        }
        return methodName.toString();
    }

    @Override
    public T getDataByKey(String key) {
        Jedis jedis = redisFactory.getJedisClient();
        Gson gson = new Gson();

        // 获取实现类
        Class clazz = this.getClass();
        // 从redis获取序列化数据
        String seriablizedString = jedis.hget(clazz.getName(), key);
        Map<String,String> beanMap = gson.fromJson(seriablizedString, Map.class);
        T t = mapToBean(beanMap);

        // 获取不到缓存需要查询数据库，这个由子类实现
        if(null == t){
            t = getDataIfNoInCache(key);
        }
        return t;
    }

    // 缓存获取不到数据时，查数据库，这个有子类实现
    public abstract T getDataIfNoInCache(String keyValue);

    // 提供字段对String转成对应类型的功能,在通过invoke时需要用到对应的参数类型，不然会报错
    private Object objectConvertType(Object object,Type type){
        try {
            if (type.getTypeName().equals(String.class.getName())) {
                String result = String.valueOf(object);
                if ("null".equals(result)) {
                    result = null;
                }
                return result;
            } else if (type.getTypeName().equals(Integer.class.getName())) {
                return Integer.valueOf(String.valueOf(object));
            } else if (type.getTypeName().equals(Double.class.getName())) {
                return Double.valueOf(String.valueOf(object));
            } else if(type.getTypeName().equals(Long.class.getName())){
                return Long.valueOf(String.valueOf(object));
            } else if(type.getTypeName().equals(Date.class.getName())){
                // 日期转换情况很多
            }
        }catch (Exception e){
//            e.printStackTrace();
            return null;
        }
        return null;
    }

}
