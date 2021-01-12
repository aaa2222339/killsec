package com.jary.kill.service;

//import com.google.common.collect.Maps;
import com.jary.kill.dao.ItemKillMapper;
import com.jary.kill.dao.ItemKillSuccessMapper;
import com.jary.kill.entity.ItemKill;
import com.jary.kill.entity.ItemKillSuccess;
import com.jary.kill.util.RandomUtil;
import com.jary.kill.util.SnowFlake;
//import org.apache.curator.framework.CuratorFramework;
//import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import com.jary.kill.util.SysConstant;
import org.joda.time.DateTime;
//import org.redisson.api.RLock;
//import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.core.env.Environment;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class KillService{

    private static final Logger log= LoggerFactory.getLogger(KillService.class);

    private SnowFlake snowFlake=new SnowFlake(2,3);

    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;

    @Autowired
    private ItemKillMapper itemKillMapper;

    @Resource
    private AmqpTemplate amqpTemplate;
//
//    @Autowired
//    private Environment env;

//    /**
//     * 商品秒杀核心业务逻辑的处理
//     * @param killId
//     * @param userId
//     * @return
//     * @throws Exception
//     */
//
//    // 是否抢购成功：修改数据库，让库存减1
//    public Boolean killItem(Integer killId, Integer userId) throws Exception {
//        Boolean result=false;
//        //TODO:判断当前用户userid是否已经抢购了该类商品killid
//        if (itemKillSuccessMapper.countByKillUserId(killId,userId) <= 0){
//            //TODO:判断当前代抢购的商品库存是否充足、以及是否出在可抢的时间段内 - canKill
//            ItemKill itemKill=itemKillMapper.selectById(killId);
//            if (itemKill!=null && 1==itemKill.getCanKill()){
//                //TODO:扣减库存-减1
//                int res=itemKillMapper.updateKillItem(killId);
//                if (res>0){
//                    //TODO:判断是否扣减成功了?是-生成秒杀成功的订单、同时通知用户秒杀已经成功（在一个通用的方法里面实现）
//                    this.commonRecordKillSuccessInfo(itemKill,userId);
//                    result=true;
//                }
//            }
//        }else{
//            throw new Exception("您已经抢购过该商品了！");
//        }
//
//        return result;
//    }


    /**
     * 通用的方法-记录用户秒杀成功后生成的订单-并进行异步邮件消息的通知
     * @param kill
     * @param userId
     * @throws Exception
     */
    private void commonRecordKillSuccessInfo(ItemKill kill, Integer userId) throws Exception{
        // 生成订单记录 entity
        ItemKillSuccess entity=new ItemKillSuccess();
        String orderNo=String.valueOf(snowFlake.nextId());
        entity.setCode(orderNo); //雪花算法作为订单编号
        entity.setItemId(kill.getItemId());
        entity.setKillId(kill.getId());
        entity.setUserId(userId.toString());
        entity.setStatus(SysConstant.OrderStatus.SuccessNotPayed.getCode().byteValue());
        entity.setCreateTime(DateTime.now().toDate());
        // 将订单插入到数据库
        int res=itemKillSuccessMapper.insertSelective(entity);

        // 将订单人和订单编号放到一起作为消息发到队列中
        String content = userId + "-" +orderNo;
        // 发送邮件（交换机名，RoutingKey，消息:orderNo）
        amqpTemplate.convertAndSend("bootDirectExchange", "bootDirectRoutingKey", content);
    }

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 商品秒杀核心业务逻辑的处理-redis的分布式锁
     * @param killId
     * @param userId
     * @return
     * @throws Exception
     */
    public Map<String, Object> killItemV3(Integer killId, Integer userId) throws Exception {
        Map<String,Object> map = new HashMap<>();
        if (itemKillSuccessMapper.countByKillUserId(killId,userId) <= 0){

            //TODO:借助Redis的原子操作实现分布式锁-对共享操作-资源进行控制
            ValueOperations valueOperations=stringRedisTemplate.opsForValue();
            final String key=new StringBuffer().append(killId).append(userId).append("-RedisLock").toString();
            final String value= RandomUtil.generateOrderCode();
            Boolean cacheRes=valueOperations.setIfAbsent(key,value); //lua脚本提供“分布式锁服务”，就可以写在一起
            if (cacheRes){
                stringRedisTemplate.expire(key,30, TimeUnit.SECONDS);
                try {
                    ItemKill itemKill=itemKillMapper.selectByIdV2(killId);
                    if (itemKill!=null && 1==itemKill.getCanKill() && itemKill.getTotal()>0){
                        int res=itemKillMapper.updateKillItemV2(killId);
                        if (res>0){
                            commonRecordKillSuccessInfo(itemKill,userId);
                            map.put("msg","抢购成功!");
                        }
                    }
                }catch (Exception e){
                    throw new Exception("还没到抢购日期、已过了抢购时间或已被抢购完毕！");
                }finally {
                    if (value.equals(valueOperations.get(key).toString())){
                        stringRedisTemplate.delete(key);
                    }
                }
            }
        }else{
            map.put("msg","您已经抢购过该商品了!");
        }
        return map;
    }




//    @Autowired
//    private RedissonClient redissonClient;
//
//    /**
//     * 商品秒杀核心业务逻辑的处理-redisson的分布式锁
//     * @param killId
//     * @param userId
//     * @return
//     * @throws Exception
//     */
//    @Override
//    public Boolean killItemV4(Integer killId, Integer userId) throws Exception {
//        Boolean result=false;
//
//        final String lockKey=new StringBuffer().append(killId).append(userId).append("-RedissonLock").toString();
//        RLock lock=redissonClient.getLock(lockKey);
//
//        try {
//            //TODO:第一个参数30s=表示尝试获取分布式锁，并且最大的等待获取锁的时间为30s
//            //TODO:第二个参数10s=表示上锁之后，10s内操作完毕将自动释放锁
//            Boolean cacheRes=lock.tryLock(30,10,TimeUnit.SECONDS);
//            if (cacheRes){
//                //TODO:核心业务逻辑的处理
//                if (itemKillSuccessMapper.countByKillUserId(killId,userId) <= 0){
//                    ItemKill itemKill=itemKillMapper.selectByIdV2(killId);
//                    if (itemKill!=null && 1==itemKill.getCanKill() && itemKill.getTotal()>0){
//                        int res=itemKillMapper.updateKillItemV2(killId);
//                        if (res>0){
//                            commonRecordKillSuccessInfo(itemKill,userId);
//
//                            result=true;
//                        }
//                    }
//                }else{
//                    //throw new Exception("redisson-您已经抢购过该商品了!");
//                    log.error("redisson-您已经抢购过该商品了!");
//                }
//            }
//        }finally {
//            //TODO:释放锁
//            lock.unlock();
//            //lock.forceUnlock();
//        }
//        return result;
//    }
//
//
//
//    @Autowired
//    private CuratorFramework curatorFramework;
//
//    private static final String pathPrefix="/kill/zkLock/";
//
//    /**
//     * 商品秒杀核心业务逻辑的处理-基于ZooKeeper的分布式锁
//     * @param killId
//     * @param userId
//     * @return
//     * @throws Exception
//     */
//    @Override
//    public Boolean killItemV5(Integer killId, Integer userId) throws Exception {
//        Boolean result=false;
//
//        InterProcessMutex mutex=new InterProcessMutex(curatorFramework,pathPrefix+killId+userId+"-lock");
//        try {
//            if (mutex.acquire(10L,TimeUnit.SECONDS)){
//
//                //TODO:核心业务逻辑
//                if (itemKillSuccessMapper.countByKillUserId(killId,userId) <= 0){
//                    ItemKill itemKill=itemKillMapper.selectByIdV2(killId);
//                    if (itemKill!=null && 1==itemKill.getCanKill() && itemKill.getTotal()>0){
//                        int res=itemKillMapper.updateKillItemV2(killId);
//                        if (res>0){
//                            commonRecordKillSuccessInfo(itemKill,userId);
//                            result=true;
//                        }
//                    }
//                }else{
//                    throw new Exception("zookeeper-您已经抢购过该商品了!");
//                }
//            }
//        }catch (Exception e){
//            throw new Exception("还没到抢购日期、已过了抢购时间或已被抢购完毕！");
//        }finally {
//            if (mutex!=null){
//                mutex.release();
//            }
//        }
//        return result;
//    }
//
//
//
//
//
//
//
//
//
//    /**
//     * 检查用户的秒杀结果
//     * @param killId
//     * @param userId
//     * @return
//     * @throws Exception
//     */
//    @Override
//    public Map<String,Object> checkUserKillResult(Integer killId, Integer userId) throws Exception {
//        Map<String,Object> dataMap= Maps.newHashMap();
//        KillSuccessUserInfo info=itemKillSuccessMapper.selectByKillIdUserId(killId,userId);
//        if (info!=null){
//            dataMap.put("executeResult",String.format(env.getProperty("notice.kill.item.success.content"),info.getItemName()));
//            dataMap.put("info",info);
//        }else{
//            throw new Exception(env.getProperty("notice.kill.item.fail.content"));
//        }
//        return dataMap;
//    }
}








































