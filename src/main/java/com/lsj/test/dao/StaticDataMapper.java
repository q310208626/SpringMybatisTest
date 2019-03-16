package com.lsj.test.dao;

import com.lsj.test.bean.StaticDataBean;

/**
 * @descriptions: 静态数据Dao
 * @version: v1.0.0
 * @author: linsj3
 * @create: 2019-03-16 16:30
 **/
public interface StaticDataMapper {
    public StaticDataBean[] selectByType(String dataType);
    public void insertStaticData(StaticDataBean staticDataBean);
}
