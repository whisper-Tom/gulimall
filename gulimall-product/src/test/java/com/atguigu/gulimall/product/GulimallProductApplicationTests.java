package com.atguigu.gulimall.product;


import com.atguigu.gulimall.product.dao.AttrDao;
import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.feign.CouponFeignService;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.gulimall.product.vo.SpuItemAttrGroupVo;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@SpringBootTest
class GulimallProductApplicationTests {

    @Autowired
    BrandService brandService;


    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    AttrDao attrDao;


    @Autowired
    RedissonClient redissonClient;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Test
    public void getAttr(){
        List<SpuItemAttrGroupVo> attrGroupWithAttrsBySpuId = attrGroupDao.getAttrGroupWithAttrsBySpuId(13L, 225L);

        System.out.println("attrGroupDao=====>"+attrGroupWithAttrsBySpuId);

    }

    @Test
    public void setRedissonClient(){
        System.out.println("redissonClient=="+redissonClient);
    }


    @Test
    public void teststringRedisTemplate(){
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();

        ops.set("hello","world_"+ UUID.randomUUID().toString());

        String hello = ops.get("hello");
        System.out.println("保存的数据是："+hello);

    }



    @Test
    void contextLoads() {
         AttrEntity attrEntity = new AttrEntity();
        AttrEntity entity = attrDao.selectById(1);
        System.out.println(entity.getAttrName());
        // System.out.println(couponFeignService);
//// Endpoint以杭州为例，其它Region请按实际情况填写。
//        String endpoint = "oss-cn-beijing.aliyuncs.com";
//// 云账号AccessKey有所有API访问权限，建议遵循阿里云安全最佳实践，创建并使用RAM子账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建。
//        String accessKeyId = "LTAI4GEuyK9sLhTNa2caD6fF";
//        String accessKeySecret = "ghY9rfzt2daUwPljF8vowXVwXcr8TD";
//
//// 创建OSSClient实例。
//        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);


    }





}
