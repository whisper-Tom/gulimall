package com.atguigu.gulimall.product.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

//2级分类vo，包含的属性
@NoArgsConstructor //快速生成无参构造
@AllArgsConstructor //快速生成有参构造
@Data
public class Catelog2Vo {

    private String catalog1Id;//1级父分类id
    private List<Catelog3Vo> catalog3List;//三级子分类
    private String id;
    private String name;


    /**
     * 三级分类vo，静态类
     */
    @NoArgsConstructor //快速生成无参构造
    @AllArgsConstructor //快速生成有参构造
    @Data
    public static class Catelog3Vo{
        private String catalog2Id;//父分类，2级分类id
        private String id;
        private String name;


    }

}
