package com.github.chaijunkun.distribution.test.id;

import com.github.chaijunkun.distribution.client.ZookeeperClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.KeeperException;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Zookeeper生成全局唯一id测试用例
 *
 * @author chaijunkun
 */
@Slf4j
public class ZKIdTest extends AbstractTest {

    private static String HOST = "127.0.0.1";

    private static int PORT = 2181;

    private static int TIMEOUT = 3000;

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

    private ZookeeperClient client;

    ZKIdTest() {
        super();
        client = new ZookeeperClient(HOST, PORT, TIMEOUT);
    }

    /**
     * you should create node /global first, and then /global/[BIZ_ID]...
     *
     * @param universalFlag
     * @param keyParamDate
     * @return
     */
    private String getPath(String universalFlag, String keyParamDate) {
        StringBuilder pathSB = new StringBuilder();
        pathSB.append("/global").append("/").append(BIZ_ID);
        if (StringUtils.isNotBlank(universalFlag)) {
            pathSB.append("/").append(universalFlag);
        }
        pathSB.append("/").append(keyParamDate);

        String path = pathSB.toString();
        return path;
    }

    @BeforeTest
    public void doConnect() throws IOException {
        this.client.connect();
        String path = this.getPath(this.getUniversalFlag(), sdf.format(Calendar.getInstance().getTime()));
        this.client.createPath(path);
    }


    @Test(threadPoolSize = 100, invocationCount = 100)
    public void acquireGlobalId() {
        String keyParamDate = sdf.format(Calendar.getInstance().getTime());
        String path = this.getPath(this.getUniversalFlag(), keyParamDate);
        Integer version = null;
        try {
            version = client.incrVersion(path);
        } catch (KeeperException e) {
            log.error("zookeeper error", e);
        } catch (InterruptedException e) {
            log.error("interrupt error", e);
        }
        String globalId = String.format("%s%s%07d", keyParamDate, this.getUniversalFlag(), version);
        log.info("zookeeper path: {}, global id: {}", path, globalId);
    }

    @AfterTest
    public void doClose() {
        this.client.close();
    }

}
