package com.github.chaijunkun.distribution.test.id;

import com.github.chaijunkun.distribution.utils.IPUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;

@Slf4j
public class RedisIdTest {

    private static String HOST = "127.0.0.1";

    private static int PORT = 6379;

    private static String REDIS_KEY_BIZ_ID = "order_id";

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

    private JedisPool pool;

    private String universalFlag = "";

    RedisIdTest() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(10);
        config.setMaxTotal(200);
        config.setTestOnBorrow(true);
        pool = new JedisPool(config, HOST, PORT);
        InetAddress defaultLocalAddress = IPUtils.getDefaultLocalAddress();
        if (null != defaultLocalAddress) {
            byte[] address = defaultLocalAddress.getAddress();
            // fetch the last two parts of ip address.
            // having considered the value between 0~255 for each part
            // make 0 index if value less than 100
            // for example: 10.27.74.132 -> 74.132 -> 074 132
            if (ArrayUtils.isNotEmpty(address) && address.length > 2) {
                byte lastByte = address[address.length - 1];
                byte lastSecondByte = address[address.length - 2];
                universalFlag = String.format("%03d%03d", lastSecondByte & 0xFF, lastByte & 0xFF);
            }

        }
    }

    @BeforeTest
    public void before() {
        log.info("start to run");
    }

    @Test(threadPoolSize = 100, invocationCount = 100)
    public void test1() {
        try (Jedis resource = pool.getResource()) {
            String keyParamDate = sdf.format(Calendar.getInstance().getTime());
            String keyParamIP = this.universalFlag;
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
