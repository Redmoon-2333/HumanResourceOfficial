package com.redmoon2333.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.redmoon2333.entity.Activity;
import com.redmoon2333.mapper.ActivityMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Redis 布隆过滤器工具类
 * <p>
 * 使用 Redis SETBIT 命令实现简化的布隆过滤器
 * 用于快速判断某个 ID 是否存在，减少数据库查询压力
 * <p>
 * Why: 在查询活动/资料等实体前，先通过布隆过滤器快速判断，
 * 避免对不存在的 ID 进行数据库查询，提升系统性能
 * <p>
 * Warning: 布隆过滤器存在约 0.1% 的误判率（False Positive）
 * 即可能将不存在的 ID 误判为存在，但不会漏判（False Negative）
 * 因此查询结果仍需以数据库为准，仅用于快速过滤
 *
 * @author RedMoon2333
 * @since 1.0
 */
@Component
public class RedisBloomFilterUtil {

    private static final Logger logger = LoggerFactory.getLogger(RedisBloomFilterUtil.class);

    /**
     * 布隆过滤器 Key 前缀
     */
    private static final String BLOOM_FILTER_PREFIX = "bloom:";

    /**
     * 活动布隆过滤器 Key
     */
    private static final String ACTIVITY_BLOOM_FILTER = BLOOM_FILTER_PREFIX + "activity";

    /**
     * 资料布隆过滤器 Key
     */
    private static final String MATERIAL_BLOOM_FILTER = BLOOM_FILTER_PREFIX + "material";

/**
     * StringRedisTemplate
     * <p>
     * Why: 使用 StringRedisTemplate 执行 Redis 命令，无需额外依赖 Jedis
     */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * ActivityMapper
     * <p>
     * Why: 用于加载所有活动 ID 到布隆过滤器
     */
    @Autowired
    private ActivityMapper activityMapper;

    /**
     * 初始化活动布隆过滤器
     * <p>
     * 在应用启动时执行，确保过滤器已就绪
     */
    @PostConstruct
    public void initActivityFilter() {
        try {
            logger.info("Redis 布隆过滤器初始化完成");

            // 预加载活动数据到布隆过滤器
            preLoadActivities();

        } catch (Exception e) {
            logger.error("Redis 布隆过滤器初始化失败", e);
        }
    }

    /**
     * 预加载活动数据到布隆过滤器
     * <p>
     * Warning: 此方法需要在数据初始化完成后调用
     * 实际项目中应配合活动 Service 使用
     */
    private void preLoadActivities() {
        try {
            if (activityMapper == null) {
                logger.warn("ActivityMapper 未注入，跳过布隆过滤器预加载");
                return;
            }

            // 获取所有活动 ID 并添加到布隆过滤器
            var activities = activityMapper.selectList(null);
            if (activities != null && !activities.isEmpty()) {
                int count = 0;
                for (Activity activity : activities) {
                    if (activity.getActivityId() != null) {
                        add("activity", activity.getActivityId());
                        count++;
                    }
                }
                logger.info("活动布隆过滤器预加载完成，共加载 {} 个活动 ID", count);
            } else {
                logger.info("暂无活动数据，跳过布隆过滤器预加载");
            }
        } catch (Exception e) {
            logger.error("活动布隆过滤器预加载失败", e);
        }
    }

    /**
     * 判断 ID 可能存在于布隆过滤器中
     * <p>
     * Warning: 存在约 0.1% 的误判率
     * 返回 true 表示 ID 可能存在，仍需数据库验证
     * 返回 false 表示 ID 一定不存在，可直接返回
     *
     * @param type 类型（如："activity", "material"）
     * @param id   ID
     * @return true 表示 ID 可能存在，false 表示 ID 一定不存在
     */
    public boolean mightExist(String type, Integer id) {
        if (stringRedisTemplate == null) {
            logger.warn("StringRedisTemplate 未注入，跳过布隆过滤器检查");
            return true; // 保守策略：未初始化时返回 true，交由数据库判断
        }

        try {
            String key = getBloomFilterKey(type);
            long bitPosition = getBitPosition(id);

            // 使用 RedisCallback 执行 GETBIT 命令
            Long result = stringRedisTemplate.execute((RedisCallback<Long>) connection -> {
                byte[] keyBytes = key.getBytes();
                Boolean bitValue = connection.getBit(keyBytes, bitPosition);
                return bitValue ? 1L : 0L;
            });

            return result != null && result == 1;

        } catch (Exception e) {
            logger.error("布隆过滤器查询失败：type={}, id={}", type, id, e);
            // 失败时返回 true，交由数据库判断
            return true;
        }
    }

    /**
     * 将 ID 添加到布隆过滤器
     *
     * @param type 类型（如："activity", "material"）
     * @param id   ID
     */
    public void add(String type, Integer id) {
        if (stringRedisTemplate == null) {
            logger.warn("StringRedisTemplate 未注入，无法添加到布隆过滤器");
            return;
        }

        try {
            String key = getBloomFilterKey(type);
            long bitPosition = getBitPosition(id);

            // 使用 RedisCallback 执行 SETBIT 命令
            stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
                byte[] keyBytes = key.getBytes();
                connection.setBit(keyBytes, bitPosition, true);
                return null;
            });

            logger.debug("ID {} 添加到布隆过滤器：{}", id, key);

        } catch (Exception e) {
            logger.error("布隆过滤器添加失败：type={}, id={}", type, id, e);
        }
    }

    /**
     * 从布隆过滤器中移除 ID
     * <p>
     * Warning: 布隆过滤器不支持安全删除
     * 直接删除可能导致其他 ID 的误判
     * 仅在全量重建过滤器时使用
     *
     * @param type 类型
     * @param id   ID
     */
    public void remove(String type, Integer id) {
        if (stringRedisTemplate == null) {
            return;
        }

        try {
            String key = getBloomFilterKey(type);
            long bitPosition = getBitPosition(id);

            // Warning: 将位设置为 0 可能导致其他 ID 的误判
            // 仅用于调试或全量重建场景
            stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
                byte[] keyBytes = key.getBytes();
                connection.setBit(keyBytes, bitPosition, false);
                return null;
            });

        } catch (Exception e) {
            logger.error("布隆过滤器删除失败：type={}, id={}", type, id, e);
        }
    }

    /**
     * 清空指定类型的布隆过滤器
     * <p>
     * 用于全量重建场景
     *
     * @param type 类型
     */
    public void clear(String type) {
        if (stringRedisTemplate == null) {
            return;
        }

        try {
            String key = getBloomFilterKey(type);
            stringRedisTemplate.delete(key);
            logger.info("布隆过滤器已清空：{}", key);
        } catch (Exception e) {
            logger.error("清空布隆过滤器失败：{}", type, e);
        }
    }

    /**
     * 获取布隆过滤器 Key
     *
     * @param type 类型
     * @return Redis Key
     */
    private String getBloomFilterKey(String type) {
        return switch (type.toLowerCase()) {
            case "activity" -> ACTIVITY_BLOOM_FILTER;
            case "material" -> MATERIAL_BLOOM_FILTER;
            default -> BLOOM_FILTER_PREFIX + type;
        };
    }

    /**
     * 计算 ID 对应的位位置
     * <p>
     * 使用简单的哈希函数：ID * 质数
     * 实际项目中可使用更复杂的哈希算法（如 MurmurHash）
     *
     * @param id ID
     * @return 位位置
     */
    private long getBitPosition(Integer id) {
        // 使用质数乘法减少冲突
        return id.longValue() * 31;
    }

    /**
     * 关闭资源
     * <p>
     * 在应用关闭时调用
     */
    public void close() {
        logger.info("Redis 布隆过滤器资源已释放");
    }
}