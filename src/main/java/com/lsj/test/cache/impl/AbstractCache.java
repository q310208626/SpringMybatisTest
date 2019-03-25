package com.lsj.test.cache.impl;

import com.lsj.test.cache.interfaces.ICache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

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


    @Override
    public T getDataByKey(String key) {
        RedisConnection redisConnection = redisFactory.getRedisConnection();
        RedisTemplate redisTemplate = redisFactory.getRedisTemplate();

        // 获取实现类
        Class clazz = this.getClass();
        byte[] beanBytes = redisConnection.hGet(clazz.getName().getBytes(),key.getBytes());
        // 从redis获取序列化数据
        T t = (T) redisTemplate.getHashValueSerializer().deserialize(beanBytes);

        // 获取不到缓存需要查询数据库，这个由子类实现
        if(null == t){
            t = getDataIfNoInCache(key);
        }
        return t;
    }

    // 缓存获取不到数据时，查数据库，这个有子类实现
    public abstract T getDataIfNoInCache(String keyValue);
}
