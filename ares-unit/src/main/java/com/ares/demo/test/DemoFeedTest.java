package com.ares.demo.test;

import com.ares.demo.spring.service.IAnotherService;
import com.ares.unit.SpringContextConfiguration;
import com.ares.demo.spring.service.AnotherService;
import com.ares.demo.spring.service.UserService;
import mockit.Mock;
import mockit.MockUp;
import org.databene.benerator.anno.Source;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ares.unit.AresUnitExtendsRunner;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 不能在类内部点右键启动 JUnit4TestRunnerUtil 为idea实现的junit插件，会导致不兼容
 *
 * @author liyebing created on  15/9/22.
 * @version $Id$
 */
@RunWith(AresUnitExtendsRunner.class)
@SpringContextConfiguration(locations = "spring/spring-config.xml")
@Service
public class DemoFeedTest {

    AnotherService anotherService;
//bh468282
    @Resource
    private UserService userService;

    @Test
    @Source("dataset/data.csv")
    public void testParamsDrive(String name, String password) {

        new MockUp<AnotherService>(){
            @Mock
            public String sayHi(String hi){
                return "this is mocked result :"+hi;
            }

        };

        System.out.println("name:" + name + " password:" + password);
        System.out.println(userService.add(12,100));
    }

}
