package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.vo.Catelog2Vo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author qkj
 * @email 413661554@qq.com
 * @date 2021-01-15 20:59:07
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> listWithTree();

    void removeMenuByIds(List<Long> asList);

    //根据catelogId的完整路径
    //[父/子/孙]
    Long[] findCatelogPath(Long catelogId);

    void updateCascade(CategoryEntity category);

    List<CategoryEntity> getLevel1Categorys();

    Map<String, List<Catelog2Vo>> getCatelogJsonFromDb();

    Map<String, List<Catelog2Vo>> getDataFromDb();

    //Map<String, List<Catelog2Vo>> getCatelogJson();

    Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedisLock();

}

