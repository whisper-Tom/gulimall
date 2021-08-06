package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.service.SkuInfoService;
import com.atguigu.gulimall.product.vo.SkuItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ItemController {

    @Autowired
    SkuInfoService skuInfoService;

    @GetMapping("/ItemContr/getSkuItem/{skuId}.html")
    public String getSkuItem(@PathVariable("skuId") Long skuId){
        SkuItemVo skuItemVo =   skuInfoService.item(skuId);

        System.out.println("skuId============>"+skuId);
        return "item.html";
    }

}
