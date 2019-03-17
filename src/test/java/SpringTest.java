import com.lsj.test.bean.StaticDataBean;
import com.lsj.test.bean.StaticDataPluginBean;
import com.lsj.test.bean.StaticDataPluginBeanExample;
import com.lsj.test.dao.StaticDataMapper;
import com.lsj.test.dao.StaticDataPluginBeanMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

/**
 * @descriptions:
 * @version: v1.0.0
 * @author: linsj3
 * @create: 2019-03-16 16:57
 **/
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/applicationContext.xml")
public class SpringTest {

    @Autowired
    StaticDataMapper staticDataMapper;

    @Autowired
    StaticDataPluginBeanMapper staticDataPluginBeanMapper;

    @Test
    public void test(){
        StaticDataBean staticDataBean = new StaticDataBean();
        staticDataBean.setDataType("ABC");
        staticDataBean.setDataValue("1234");
        staticDataMapper.insertStaticData(staticDataBean);
        
        StaticDataBean[] staticDataBeans = staticDataMapper.selectByType("ABC");
        System.out.println(staticDataBeans.length);
    }

    @Test
    public void testPlugin(){
        StaticDataPluginBeanExample staticDataPluginBeanExample = new StaticDataPluginBeanExample();
        StaticDataPluginBeanExample.Criteria criteria = staticDataPluginBeanExample.createCriteria();
        criteria.andDataTypeEqualTo("ABC");
        List<StaticDataPluginBean> staticDataPluginBeans = staticDataPluginBeanMapper.selectByExample(staticDataPluginBeanExample);
        staticDataPluginBeans.stream().forEach(x->{
            System.out.println(x.getDataValue());
        });
    }
}
