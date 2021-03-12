package com.atguigu.gulimall.order.dao;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author qkj
 * @email 413661554@qq.com
 * @date 2021-01-15 22:17:37
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
