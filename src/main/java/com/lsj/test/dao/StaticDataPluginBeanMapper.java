package com.lsj.test.dao;

import com.lsj.test.bean.StaticDataPluginBean;
import com.lsj.test.bean.StaticDataPluginBeanExample;
import java.util.List;

public interface StaticDataPluginBeanMapper {
    int countByExample(StaticDataPluginBeanExample example);

    int insert(StaticDataPluginBean record);

    int insertSelective(StaticDataPluginBean record);

    List<StaticDataPluginBean> selectByExample(StaticDataPluginBeanExample example);
}