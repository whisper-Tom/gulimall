package com.atguigu.gulimall.product.feign;

import com.atguigu.common.to.SkuReductionTo;
import com.atguigu.common.to.SpuBoundTo;
import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient("gulimall-coupon")
@Service
public interface CouponFeignService {


    /**
     * 如果有远程地方调用了CouponFeignService.saveSpuBounds(spuBoundTo)
     * 1.springcloud是先将@RequestBody将这个对象转为json
     * 2.找到gulimall-coupon服务，给/coupon/spubounds/save发送请求
     * 3.对方服务收到请求。请求体里有json数据。
     *    （@RequestBody SpuBoundsEntity spuBounds）将请求体的json转为SpuBoundsEntity；
     *    只要json数据模型是兼容的。双方服务无需使用同一个to
     *
     * @param spuBoundTo
     * @return
     */
    @PostMapping("/coupon/spubounds/save")
    R saveSpuBounds(@RequestBody SpuBoundTo spuBoundTo);


    @PostMapping("/coupon/skufullreduction/saveInfo")
    R saveSkuReduction(@RequestBody SkuReductionTo skuReductionTo);
}
