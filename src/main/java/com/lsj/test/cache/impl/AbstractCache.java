package com.lsj.test.cache.impl;

import com.google.gson.Gson;
import com.lsj.test.cache.interfaces.ICache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.beans.BeanDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
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
    public Map beanToMap(T t){
        Map<String,String> map = new HashMap();
        ;
        // 获取实现类的泛型，不是子类，是子类要操作的bean对象
        Type type = this.getClass().getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            Type paramType = ((ParameterizedType) type).getActualTypeArguments()[0];
            Class<T> clazz = (Class) paramType;

            // 获取bean所有字段
            BeanDescriptor cacheBeanDescriptor = new BeanDescriptor(clazz);
            Field[] fields = cacheBeanDescriptor.getBeanClass().getDeclaredFields();
            List<Field> fieldList = Arrays.asList(fields);
            Iterator<Field> fieldIterator = fieldList.iterator();

            // 循环遍历所有字段，对有对应的public的getField()方法的字段,调用方法放入map中
            while (fieldIterator.hasNext()) {
                Field field = fieldIterator.next();
                String fieldName = field.getName();
                PropertyDescriptor fieldDescriptor = null;
                try {
                    fieldDescriptor = new PropertyDescriptor(fieldName,clazz);
                } catch (IntrospectionException e) {
                    System.out.println("类["+clazz+"]获取不到字段("+fieldName+")");
                }

                // 获取字段的get方法
                Method getFieldMethod = fieldDescriptor.getReadMethod();
                if(!getFieldMethod.isAccessible()){
                    continue;
                }

                try {
                    String fieldValueString = String.valueOf(getFieldMethod.invoke(t));
                    map.put(field.getName(), fieldValueString);
                }catch (IllegalAccessException e) {
                    System.out.println("无法访问[" + getFieldMethod.getName()+ "]方法");
                } catch (InvocationTargetException e) {
                    System.out.println("调用方法出错[" + getFieldMethod.getName()+ "]");
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

                // 创建类实例，映射mpa属性
                try {
                    t = clazz.newInstance();
                    // 扫描map的字段
                    for (String x : beanMap.keySet()) {
                        try {
                            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(x,clazz);
                            Method setMethod = propertyDescriptor.getWriteMethod();
                            setMethod.invoke(t, objectConvertType(beanMap.get(x), propertyDescriptor.getPropertyType()));
                        }catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        } catch (IntrospectionException e) {
                            System.out.println("class["+clazz.getSimpleName()+"]没有这个字段"+x);
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
