package com.core;

import com.annotation.RedisLock;
import com.core.inter.RedisLockService;
import com.enums.RedisEnum;
import com.enums.RedisLockSuffixKeyTypeEnum;
import com.exception.RedisLockException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 分布式锁，切面处理类
 */
@Aspect
@Component
public class RedisLockAspect {

    @Resource
    private RedisLockService redisLockService;

    @Pointcut("@annotation(com.annotation.RedisLock)")
    public void lockPointCut() {
    }

    @Around("lockPointCut() && @annotation(redisLock)")
    public Object around(ProceedingJoinPoint joinPoint, RedisLock redisLock) throws Throwable {
        String lockKey;
        try {
            RedisLockSuffixKeyTypeEnum suffixKeyTypeEnum = RedisLockSuffixKeyTypeEnum.of(redisLock.suffixKeyTypeEnum());
            switch (suffixKeyTypeEnum) {
                case PARAM:
                    lockKey = redisLockService.getKeyWithParam(joinPoint, redisLock);
                    break;
                case NO_SUFFIX:
                    lockKey = redisLock.key();
                    break;
                case THREAD_LOCAL:
                    lockKey = redisLockService.getKeyWithThreadLocal(joinPoint, redisLock);
                    break;
                default:
                    throw new RedisLockException("未知后缀获取类型" + redisLock.suffixKeyTypeEnum());
            }
        } catch (Exception e) {
            throw new RedisLockException("获取后缀key失败" + e);
        }

        RedisEnum redisEnum = RedisEnum.of(redisLock.redisEnum());
        switch (redisEnum) {
            case REDISSON:
                return redisLockService.lockByRedisson(lockKey, joinPoint, redisLock);
            case SPRING_REDIS:
                return redisLockService.lockBySpringRedis(lockKey, joinPoint, redisLock);
            default:
                throw new RedisLockException("未知redis工具" + redisLock.redisEnum());
        }
    }

}