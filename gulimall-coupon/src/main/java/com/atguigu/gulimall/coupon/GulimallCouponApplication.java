package com.atguigu.gulimall.coupon;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

//@EnableDiscoveryClient 这个注解是nacos注册中心的，作用的是开启微服务的注册与发现

/*
 * 如何使用nacos作为配置中心
 *
 * 1.引入依赖
 *
 * 2.新建bootstrap.properties文件，配置
 * spring.application.name=gulimall-coupon
   spring.cloud.nacos.config.server-addr=127.0.0.1:8848
 *
 * 3.在nacos页面的配置列表里添加与服务同名的properties，里面就是那些配置信息
 *
 * 4.在要修改的Controller加@RefreshScope，作用是动态刷新获取配置，通过@Value("${coupon.user.name}")获取值
 *
 * 配置中心大于配置文件
 * 就是说如果配置中心和配置文件都改了某个配置，则以配置中心的值为主
 *
 *
 * 细节
 * 命名空间：配置隔离：
 * 默认：public（保留空间）：默认新增的所有配合都在public空间
 * 1.开发，测试，生产：利用命名空间来做环境隔离
 * 注意，在bootstrap.properties文件里要配置使用哪个命名空间
 * spring.cloud.nacos.config.namespace=f3577160-4570-47fa-8878-0017a6ae96c8
 *
 * 每一个微服务还可以建立自己的命名空间，用来放自己的配置
 *
 * 配置集：就是所有的配置
 *
 * 配置集ID就是Data Id，一般都是与项目同名
 *
 *
 *
 * 同时加载多个配置集
 * 微服务任何配置信息，任何配置文件都可以放在配置中心中
 * 只需要在bootstrap.properties说明加载配置中心中哪些配置文件即可
 *
 *
 *
 *
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.atguigu.gulimall.coupon.dao")
public class GulimallCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallCouponApplication.class, args);
    }

}
