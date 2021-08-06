package com.atguigu.gulimall.coupon;

import com.atguigu.gulimall.coupon.dao.SpuBoundsDao;
import com.atguigu.gulimall.coupon.entity.SpuBoundsEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GulimallCouponApplicationTests {

    @Autowired
    SpuBoundsDao spuBoundsDao;

    @Test
    void contextLoads() {

        SpuBoundsEntity spuBoundsEntity = new SpuBoundsEntity();
        spuBoundsEntity.setId(1L);
        SpuBoundsEntity boundsEntity = spuBoundsDao.selectById(spuBoundsEntity);
        System.out.println(boundsEntity.getId());
    }

}
