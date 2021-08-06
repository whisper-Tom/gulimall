package com.atguigu.gulimall.product.app;

import java.util.Arrays;
import java.util.Map;

import com.atguigu.common.valid.AddGroup;
import com.atguigu.common.valid.UpdateGroup;
import com.atguigu.common.valid.UpdateStatusGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;


/**
 * 品牌
 *
 * @author qkj
 * @email 413661554@qq.com
 * @date 2021-01-15 20:59:07
 */
@RestController
@RequestMapping("product/brand")
public class BrandController {
    @Autowired
    private BrandService brandService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = brandService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{brandId}")
    public R info(@PathVariable("brandId") Long brandId) {
        BrandEntity brand = brandService.getById(brandId);

        return R.ok().put("brand", brand);
    }

    /**
     * 保存
     *
     * @Valid这个注解用告诉spring这个对象要进行参数校验，某个参数不能为空等等 BindingResult result这个一定要紧跟校验对象之后，BindingResult是用来获取错误信息的
     @Validated指定校验分组
     */
    @RequestMapping("/save")
    public R save(@Validated({AddGroup.class}) @RequestBody BrandEntity brand/* BindingResult result*/) {
//        if (result.hasErrors()) {//如果有错误信息了
//            Map<String, String> map = new HashMap<>();
//            //获取错误信息的结果
//            result.getFieldErrors().forEach((itemDetail) -> {
//                //FieldError 获取到错误提示
//                String message = itemDetail.getDefaultMessage();
//                //获取错误的属性的名字
//                String field = itemDetail.getField();
//                map.put(field, message);
//
//            });
//           return R.error(400,"提交的数据不合法").put("data",map);
//        }

        brandService.save(brand);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@Validated({UpdateGroup.class})@RequestBody BrandEntity brand) {
        brandService.updateDetail(brand);

        return R.ok();
    }

    /**
     * 只修改显示状态
     */
    @RequestMapping("/updateStatus")
    public R updateStatus(@Validated({UpdateStatusGroup.class})@RequestBody BrandEntity brand) {
        brandService.updateById(brand);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] brandIds) {
        brandService.removeByIds(Arrays.asList(brandIds));

        return R.ok();
    }

}
