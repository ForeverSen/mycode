package common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisCommands;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

/**
 * @description: redis分布式锁
 */
@Slf4j
@Component
public class RedisLockUtil {
    private static final String LOCK_SUCCESS = "OK";

    private static final String SET_IF_NOT_EXIST = "NX";

    private static final String SET_WITH_EXPIRE_TIME = "PX";

    private static final String SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     * @description: 获取redis锁
     * @param: [jedis, lockKey, lockValue, expireTime]
     * @return: boolean
     */
    public boolean tryLock(String lockKey, String lockValue, long expireTime) {
        try {
            String execute = redisTemplate.execute((RedisCallback<String>) connection -> {
                JedisCommands jedisCommands = (JedisCommands) connection.getNativeConnection();
                return jedisCommands.set(lockKey, lockValue, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
            });
            return Objects.equals(execute, LOCK_SUCCESS);
        } catch (Exception e) {
            log.error("获取redis锁失败，错误信息: ", e);
        }
        return Boolean.FALSE;
    }

    /**
     * @description: 释放redis锁(使用lua脚本)，需要Redis服务器支持lua脚本
     * @param: [lockKey, lockValue]
     * @return: boolean
     */
    public boolean releaseLuaLock(String lockKey, String lockValue) {
        try {
            Long execute = redisTemplate.execute((RedisCallback<Long>) connection -> {
                Object nativeConnection = connection.getNativeConnection();
                // 集群模式
                if (nativeConnection instanceof JedisCluster) {
                    return (Long) ((JedisCluster) nativeConnection).eval(SCRIPT, Collections.singletonList(lockKey), Collections.singletonList(lockValue));
                } else if (nativeConnection instanceof Jedis) {
                    // 单机模式
                    return (Long) ((Jedis) nativeConnection).eval(SCRIPT, Collections.singletonList(lockKey), Collections.singletonList(lockValue));
                }
                return 0L;
            });
            return Objects.nonNull(execute) && execute > 0;
        } catch (Exception e) {
            log.error("释放redis锁失败，错误信息: ", e);
        }
        return Boolean.FALSE;
    }

    /**
     * @description: 释放redis锁, 高并发的情况下可能存在风险，无法保证get指令和del指令的原子性
     * @param: [lockKey, lockValue]
     * @return: boolean
     */
    public boolean releaseLock(String lockKey, String lockValue) {
        try {
            String value = redisTemplate.opsForValue().get(lockKey);
            if (Objects.equals(value, lockValue)) {
                return Optional.ofNullable(redisTemplate.delete(lockKey)).orElse(Boolean.FALSE);
            }
            log.error("释放redis锁失败，释放别人锁资源，lockKey: {}, lockValue: {} ", lockKey, lockValue);
        } catch (Exception e) {
            log.error("释放redis锁失败，错误信息: ", e);
        }
        return Boolean.FALSE;
    }

}
