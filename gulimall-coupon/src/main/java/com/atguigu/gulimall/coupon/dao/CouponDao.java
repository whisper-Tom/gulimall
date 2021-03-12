package com.atguigu.gulimall.coupon.dao;

import com.atguigu.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author qkj
 * @email 413661554@qq.com
 * @date 2021-01-15 22:26:28
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
