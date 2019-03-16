import com.lsj.test.bean.StaticDataBean;
import com.lsj.test.dao.StaticDataMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
    @Test
    public void test(){
        StaticDataBean staticDataBean = new StaticDataBean();
        staticDataBean.setDataType("ABC");
        staticDataBean.setDataValue("1234");
        staticDataMapper.insertStaticData(staticDataBean);
        
        StaticDataBean[] staticDataBeans = staticDataMapper.selectByType("ABC");
        System.out.println(staticDataBeans.length);
    }
}
