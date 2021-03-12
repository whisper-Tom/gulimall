package com.atguigu.gulimall.member.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

//@FeignClient这个注解表示这个接口是调用的远程服务，括号里填的是注册中心的服务名
@FeignClient("gulimall-coupon")
public interface CouponFeignService {

    /**
     * 这里只要返参和路径一致就行了，但是dubbo要求完全一致
     */
    @RequestMapping("coupon/coupon/member/list")
    public R membercoupons();

}
