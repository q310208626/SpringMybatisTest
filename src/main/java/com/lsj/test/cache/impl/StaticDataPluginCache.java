package com.lsj.test.cache.impl;

import com.lsj.test.bean.StaticDataPluginBean;
import com.lsj.test.bean.StaticDataPluginBeanExample;
import com.lsj.test.dao.StaticDataPluginBeanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @descriptions:
 * @version: v1.0.0
 * @author: linsj3
 * @create: 2019-03-18 21:44
 **/
@Component(value = "StaticDataPluginCache")
public class StaticDataPluginCache extends AbstractCache<StaticDataPluginBean> {

    @Autowired
    StaticDataPluginBeanMapper staticDataPluginBeanMapper;



    @Override
    public void init() {

    }

    @Override
    public List<Map> getDataMaps() {
        List<StaticDataPluginBean> staticDataPluginBeans = staticDataPluginBeanMapper.selectByExample(new StaticDataPluginBeanExample());
        return beansToMaps(staticDataPluginBeans);
    }

    @Override
    public String specialKey() {
        return "dataType";
    }

    @Override
    public Object getDataIfNoInCache(String key) {
        StaticDataPluginBeanExample staticDataPluginBeanExample = new StaticDataPluginBeanExample();
        StaticDataPluginBeanExample.Criteria criteria =  staticDataPluginBeanExample.createCriteria();
        criteria.andDataTypeEqualTo(key);
        List<StaticDataPluginBean> staticDataPluginBeans = staticDataPluginBeanMapper.selectByExample(staticDataPluginBeanExample);
        if(null != staticDataPluginBeans && staticDataPluginBeans.size() > 0){
            return staticDataPluginBeans.get(0);
        }
            return null;
    }
}
