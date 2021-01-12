package com.jary.kill;

import com.jary.kill.KillApplication;
import com.jary.kill.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = KillApplication.class)
public class TestRabbit {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private UserService userService;

    @Test
    public void test(){ // 往名字为hello的队列发送消息you
        rabbitTemplate.convertAndSend("hello","you");
    }


    @Test
    public void test2(){
        String email = userService.findUserByUserid(10).getEmail();
    }


}
