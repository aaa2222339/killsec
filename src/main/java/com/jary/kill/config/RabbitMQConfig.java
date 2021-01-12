package com.jary.kill.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 让springboot配置好
@Configuration
public class RabbitMQConfig {
    // 配置一个名为bootDirectExchange的交换机
    @Bean
    public DirectExchange directExchange(){
        return new DirectExchange("bootDirectExchange");
    }

    // 配置一个名为bootDirectQueue的队列
    @Bean
    public Queue directQueue(){
        return new Queue("bootDirectQueue");
    }

    // 配置队列和交换机绑定，同时指定routingKey为bootDirectRoutingKey，这两个参数要和上面的方法一致就能自动注入了
    @Bean
    public Binding directBinding(Queue directQueue,DirectExchange directExchange){
        return BindingBuilder.bind(directQueue).to(directExchange).with("bootDirectRoutingKey");
    }
}