package com.jary.kill.service;

import com.jary.kill.util.MailClient;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;

@Service("Customer")
public class CustomerService {
    @Resource
    private AmqpTemplate amqpTemplate;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private UserService userService;

    @Autowired
    private MailClient mailClient;

    // 配置消息监听器，spring 会自动调用，不用手动，如果该方法正常执行不报错， 就会给队列自动确认该消息
    @RabbitListener(queues = {"bootDirectQueue"})
    public void directReceive(String content){
        System.out.println("监听器收到消息");
        String[] array=content.split("-");
        int id = Integer.parseInt(array[0]);
        String email = userService.findUserByUserid(id).getEmail();
        String orderNo = array[1];
        content = "恭喜抢购成功，您的抢购订单编号为："+orderNo;
        mailClient.sendMail(email, "订单", content);
    }
}