package com.lsj.test.cache.interfaces;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public interface ICache {
    public void init();

    // 要返回作为缓存的数据，用List<Map>方式缓存
    public List<Map> getDataMaps();

    // 要作为key值的字段，用以区分同一个表不同的数据
    public String specialKey();

    // 通过key的value获取缓存数据
    public Object getDataByKey(String keyValue);
}
