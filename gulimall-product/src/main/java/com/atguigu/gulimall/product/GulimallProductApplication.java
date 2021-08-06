package com.atguigu.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/*
   整合mybatis-plus
   1.在pom.xml里添加依赖

   2.配置数据源
      1.导入数据库的驱动

      2.配置mybatis-plus



模板引擎
1.thymeleaf-starter：并暂时关闭缓存
2.静态资源都放在static文件夹下就可以按照路径直接访问
3.页面放在templates，直接访问
4.页面修改不重启服务器实时更新
   1）引入dev-tools
   2)修改完页面 crtl+F9重新编译页面就可以，不过要先关闭thymeleaf缓存,但是如果是改了代码还是得重启



整合springcache简化缓存开发
  引入spring-boot-starter-cache引入缓存依赖

  常用注解
  @Cacheable:触发将数据保存到缓存的操作
  @CacheEvict:触发将数据从缓存删除的操作
  @CachePut:不影响方法执行更新缓存
  @Caching:组合，以上多个操作都可以
  @CacheConfig：在类级别共享缓存的相同配置

  想要用缓存，要先开启缓存功能，@EnableCaching







 */
//@EnableCaching
@EnableFeignClients(basePackages = {"com.atguigu.gulimall.product.feign"})
@MapperScan("com.atguigu.gulimall.product.dao")
@SpringBootApplication
@EnableDiscoveryClient
public class GulimallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
