package com.lsj.test.cache.interfaces;

import java.util.List;

public interface ICache<T> {
    public void init();

    // 要返回作为缓存的数据，用List<Map>方式缓存
    public List<T> getDataMaps();

    // 要作为key值的字段，用以区分同一个表不同的数据
    public String specialKey();

    // 通过key的value获取缓存数据
    public T getDataByKey(String keyValue);
}
