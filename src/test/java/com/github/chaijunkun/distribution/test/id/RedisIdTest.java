package com.github.chaijunkun.distribution.test.id;

import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Redis生成全局唯一id测试用例
 * @author chaijunkun
 */
@Slf4j
public class RedisIdTest extends AbstractTest {

    private static String HOST = "127.0.0.1";

    private static int PORT = 6379;

    private static String REDIS_KEY_BIZ_ID = "order_id";

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

    private JedisPool pool;

    RedisIdTest() {
        super();
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(10);
        config.setMaxTotal(200);
        config.setTestOnBorrow(true);
        pool = new JedisPool(config, HOST, PORT);
    }

    @BeforeTest
    public void before() {
        log.info("start to run");
    }

    @Test(threadPoolSize = 100, invocationCount = 100)
    public void acquireGlobalId() {
        try (Jedis resource = pool.getResource()) {
            String keyParamDate = sdf.format(Calendar.getInstance().getTime());
            String keyParamIP = this.getUniversalFlag();
            // core code
            String key = String.format("global-%s-%s-%s", REDIS_KEY_BIZ_ID, keyParamDate, keyParamIP);
            Long incrId = resource.incr(key);
            String globalId = String.format("%s%s%07d", keyParamDate, keyParamIP, incrId);
            log.info("redis key: {}, global id: {}", key, globalId);
        } catch (Exception e) {
            log.error("fail to sleep", e);
        }

    }

    @AfterTest
    public void after() {
        log.info("stop");
    }
}
