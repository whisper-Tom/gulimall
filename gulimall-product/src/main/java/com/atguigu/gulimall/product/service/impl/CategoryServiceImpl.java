package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.gulimall.product.dao.CategoryBrandRelationDao;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

   /* @Autowired
    CategoryDao categoryDao;*/

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //2.组装成父子的树形结构

        //2.1 找出所有一级分类
        List<CategoryEntity> level1Menus = entities.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == 0;
        }).map((menu) -> {
            menu.setChildren(getChildrens(menu, entities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());

        return level1Menus;
    }


    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {

        //递归查找所有菜单的子菜单
        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            //1.找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            //2.菜单的排序
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());


        return children;
    }


    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 1.检查当前删除的菜单是否被别的地方引用
        baseMapper.deleteBatchIds(asList);
    }

    //[2,25,225]
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);
        Collections.reverse(parentPath);//这是集合工具类，逆序排序一下
        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联的数据
     *
     * @param category
     */
     //失效模式
//    @Caching(evict ={
//            @CacheEvict(value = "category",key = "'getLevel1Categorys'"),
//            @CacheEvict(value = "category",key = "'getCatalogJson'")
//    } )
    //这个value就是分区源，缓存的key是方法名，缓存的值是方法结果
    @CacheEvict(value = "category",allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());

    }

    /*
    //代表当前方法的结果需要缓存，如果缓存中有，方法不用调用，如果缓存中没有，就调用并将结果放入缓存
        注意：：每一个需要缓存的数据都要指定要放到哪个名字的缓存，并且要按照缓存的分区来分配
     默认行为
         1.如果缓存存在，方法不调用
         2.key自动生成，缓存的名字::SimpleKey[](自主生成的key值)
         3.缓存的value值，默认使用jdk序列化机制，将序列化之后的数据存到redis
         4.默认ttl时间 -1s

     自定义
         1.指定生成的缓存使用的key   key属性指定，接受一个SpEL表达式，所以只是字符串名字要加单引号
         2.指定缓存的数据存活时间   配置文件中修改ttl  用spring.cache.redis.time-to-live
         3.希望数据保存为json格式
     */
    //@Cacheable(value={"category"},key = "#root.methodName")
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        System.out.println("调用了getLevel1Categorys（）。。。。");
        long l = System.currentTimeMillis();
        List<CategoryEntity> entities = baseMapper.selectList
                (new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return entities;
    }


    //使用redission
    /*
        面临的问题：缓存里面的数据如何和数据库保持一致

        缓存数据一致性：
          1.双写模式
          2.失效模式

       我们系统的解决方案：
          1.给所有缓存设置过期时间，过期之后的下次请求会更新数据库
          2.读写数据的时候加上分布式读写锁（给所有的机器都加上），这适用于读多写少的情况，如果是读多写多，那就不适合放在缓存里，如果必须的话，就考虑用cancal中间件
          canal中间件会把自己伪装为从数据库，当mysql发生数据变动，cacal会实时的读取mysql二进制日志，将更新的数据同步到redis里
     */
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedissionLock() {


        //1.锁的名字一定要细致。这关系到锁的粒度，越细越快，
        //锁的粒度，具体缓存的是某个数据，11号商品
        RLock lock = redissonClient.getLock("catalogJson-lock");
        lock.lock();//加锁

        Map<String, List<Catelog2Vo>> dataFromDb;
        try {
            dataFromDb = getDataFromDb();
        } finally {
            lock.unlock();//解锁
        }
        return dataFromDb;


    }

    //使用redis分布式锁,核心就是原子加锁，原子解锁
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedisLock() {

        String uuid = UUID.randomUUID().toString();
        //1.占分布式锁，去redis占坑,占锁的同时设置好过期时间
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if (lock) {
            System.out.println("获取分布式锁成功。。。");
            //加锁成功,执行任务
            Map<String, List<Catelog2Vo>> dataFromDb;
            try {
                dataFromDb = getDataFromDb();
            } finally {
                //这个是lua脚本，删锁也得是一个原子性操作，所以用redis+lua脚本来实现
                String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1] then\n" +
                        "    return redis.call(\"del\",KEYS[1])\n" +
                        "else\n" +
                        "    return 0\n" +
                        "end";
                //删除锁
                Long lock1 = stringRedisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                        Arrays.asList("lock"), uuid);
            }
            return dataFromDb;
        } else {
            //加锁失败。。。重试，synchronized()
            //或者加休眠
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("获取分布式锁失败。。。等待重试");
            return getCatelogJsonFromDbWithRedisLock();//自旋的方式，自己调自己

        }


    }

    //先看缓存有没有，没有就是查数据库，顺便再添加到缓存
    @Override
    public Map<String, List<Catelog2Vo>> getDataFromDb() {
        String catalogJSON = stringRedisTemplate.opsForValue().get("catalogJSON");
        if (StringUtils.isEmpty(catalogJSON)) {
            System.out.println("缓存不命中，准备查询数据库。。。。");
            synchronized (this) {
                Map<String, List<Catelog2Vo>> catelogJsonFromDb = getCatelogJsonFromDb();
                //将查出的数据转为json放在缓存中。
                //另外缓存中的数据都是json字符串，因为json数据是跨平台的
                //存缓存要转为json对象，取也要转为可用对象
                String s = JSON.toJSONString(catelogJsonFromDb);
                stringRedisTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.DAYS);
                return catelogJsonFromDb;
            }
        }
        /**
         * 这是类型转换操作，因为是protected受保护的，所以得用匿名内部类
         * TypeReference<Map<String,List<Catelog2Vo>>>(){}
         */
        System.out.println("缓存命中。。。。");
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON,
                new TypeReference<Map<String, List<Catelog2Vo>>>() {
                });
        return result;
    }


    //使用本地锁
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromLocalLock() {

        return getDataFromDb();

    }


    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDb() {


        /**
         * 三级分类优化
         * 将数据库的多次查询变成一次查询，就是把分类一下次都查出来
         */
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        //1.查出所有一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);


        //2.封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //1.每一个的一级分类，要查出这个一级分类的二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            //2.封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(level2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, level2.getCatId().toString(), level2.getName());
                    //1.找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catelog = getParent_cid(selectList, level2.getCatId());
                    if (level3Catelog != null) {
                        List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map(level3 -> {
                            //2.封装指定格式
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(level2.getCatId().toString(), level3.getCatId().toString(), level3.getName());

                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(collect);

                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());

            }

            return catelog2Vos;
        }));

        return parent_cid;
    }


    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parent_cid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());

        return collect;
    }

    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        //递归收集节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);//this.getById是调sql去查的，这是封装好的
        if (byId.getParentCid() != 0) {//如果有父节点就再查
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;//[225,25,2]这里是倒序的，在上面再逆序排一下
    }


}