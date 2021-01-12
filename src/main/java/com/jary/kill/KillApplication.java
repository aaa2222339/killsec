package com.jary.kill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KillApplication {
    public static void main(String[] args) {
        // 启动spring ioc容器
        SpringApplication.run(KillApplication.class, args);
    }
}