package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
public class IndexController {


    @Autowired
    CategoryService categoryService;


    @Autowired
    RedissonClient redissonClient;


    @Autowired
    StringRedisTemplate stringRedisTemplate;


    @GetMapping({"/", "index.html"})
    public String indexPage(Model model) {
        //TODO 1.查出所有的一级分类,就是parent_id是0的就是一级分类
        List<CategoryEntity> categoryEntityList = categoryService.getLevel1Categorys();

        model.addAttribute("categorys", categoryEntityList);

        //视图解析器进行拼串
        //classpath:/templates/+返回值+.html
        return "index";
    }


    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatelogJson() {
        Map<String, List<Catelog2Vo>> catelogJson = categoryService.getCatelogJsonFromDb();

        return catelogJson;
    }


    @ResponseBody
    @GetMapping("/hello")
    public String hello() {

        //1.获取一把锁，只要锁的名字一样，就是同一把锁
        RLock lock = redissonClient.getLock("my-lock");

        //2.加锁
        //lock.lock();//这是阻塞式等待，这步如果拿不到，就只一直等.默认的锁是30秒时间
        //1.锁的自动续期，如果业务超长，就是业务还没执行完，锁就快过期了，此时redis会给锁自动续期
        //2.加锁的业务只要运行完成，就不会给当前锁续期，即使不手动解锁，锁默认在30s以后自动删除
        /*
        3.redis里面有一个看门狗机制，业务没执行完，会自动给锁续期，看门狗底层就是一个while(true)死循环
        不停取锁

        如果我们传递了锁的超时时间，就发送给redis执行脚本，进行占锁，默认超时就是我们指定的时间
        如果我们未指定锁的超时时间，就使用30*100【lockWatchdogTimeout看门狗的默认时间】
        只要占锁成功，就会启动一个定时任务【重新给锁设置过期时间，新的过期时间就是看门的默认时间】，每隔10s都会自动再次续期，续成30s
        internallockLeaseTine【看门狗时间】 3，10s
         */
        lock.lock(10, TimeUnit.SECONDS);//10秒以后自动解锁，推荐使用这种，但是自动解锁时间一定要大于业务的执行时间，如果一个业务要30s以上，那说明业务有问题
        //lock.lock(10, TimeUnit.SECONDS);但是这种方式没有看门狗机制，到时间不会自动续期
        System.out.println("加锁成功，执行业务。。。");
        try {
            System.out.println("加锁成功，执行业务。。。" + Thread.currentThread().getId());
            Thread.sleep(15000);
        } catch (InterruptedException e) {

        } finally {

            //3.解锁
            System.out.println("释放锁，执行业务。。。" + Thread.currentThread().getId());
            lock.unlock();
        }

        return "hello";
    }



    //模拟读写锁，写操作,读写锁可以保证一定能读到最新数据
    //写锁是一个排他锁（互斥锁）。读锁是共享锁
    /*
    写锁会阻塞读锁，但是读锁不会阻塞读锁，但读锁会阻塞写锁
    总之含有写的过程都会被阻塞，只有读读不会被阻塞
     */
    @GetMapping("/read")
    @ResponseBody
    public String read() {
        RReadWriteLock lock = redissonClient.getReadWriteLock("ReadWrite-Lock");
        RLock rLock = lock.readLock();
        String s = "";
        try {
            rLock.lock();
            System.out.println("读锁加锁"+Thread.currentThread().getId());
            Thread.sleep(5000);
            s= stringRedisTemplate.opsForValue().get("lock-value");
        }finally {
            rLock.unlock();
            return "读取完成:"+s;
        }
    }

    @GetMapping("/write")
    @ResponseBody
    public String write() {
        RReadWriteLock lock = redissonClient.getReadWriteLock("ReadWrite-Lock");
        RLock wLock = lock.writeLock();
        String s = UUID.randomUUID().toString();
        try {
            wLock.lock();
            System.out.println("写锁加锁"+Thread.currentThread().getId());
            Thread.sleep(10000);
            stringRedisTemplate.opsForValue().set("lock-value",s);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            wLock.unlock();
            return "写入完成:"+s;
        }
    }

}
