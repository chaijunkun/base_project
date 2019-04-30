package com.github.chaijunkun.distribution.test.id;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.Closeable;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.CountDownLatch;


@Slf4j
class Client implements Watcher, Closeable {

    private static final byte[] blankData = new byte[]{0};

    private String host;

    private int port;

    private int timeout;

    private ZooKeeper zk;

    /**
     * semaphore for waiting connection established
     */
    private CountDownLatch connectedSemaphore = new CountDownLatch(1);

    Client(String host, int port, int timeout) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }

    public synchronized void connect() throws IOException {
        if (null == this.zk) {
            this.zk = new ZooKeeper(String.format("%s:%d", this.host, this.port), this.timeout, this);
            try {
                connectedSemaphore.await();
            } catch (InterruptedException e) {
                throw new IOException("cannot wait until connected", e);
            }
        }
    }

    @Override
    public synchronized void close() {
        if (null != this.zk) {
            try {
                this.zk.close();
                this.zk = null;
            } catch (InterruptedException e) {
                log.warn("fail to close this client", e);
            }
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        //get event state
        Event.KeeperState keeperState = watchedEvent.getState();
        Event.EventType eventType = watchedEvent.getType();
        //if event state is SyncConnected
        if (Event.KeeperState.SyncConnected == keeperState) {
            // and event type is None
            if (Event.EventType.None == eventType) {
                // countdown semaphore, then the previous await statement released
                connectedSemaphore.countDown();
                log.info("zk connected");
            }
        }
    }

    /**
     * this operation should by executed by a quartz task.
     * for performance reason, check path whether exists is not essential for each acquire
     * for each acquire, it is considered that the path exists all the time.
     *
     * @param path
     * @throws IOException
     */
    public void createPath(String path) throws IOException {
        String[] pathNodes = StringUtils.split(path, "/");
        if (ArrayUtils.isNotEmpty(pathNodes)) {
            for (int i = 0; i < pathNodes.length; i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j <= i; j++) {
                    sb.append("/").append(pathNodes[j]);
                }
                String hierachyPath = sb.toString();
                log.info("create znode: {}", hierachyPath);
                try {
                    this.zk.create(hierachyPath, blankData, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                } catch (KeeperException e) {
                    if (!KeeperException.Code.NODEEXISTS.equals(e.code())) {
                        throw new IOException(e);
                    }
                } catch (InterruptedException e) {
                    throw new IOException(e);
                }
            }
        }

    }

    public Integer incrVersion(String path) throws KeeperException, InterruptedException {
        Stat stat = this.zk.setData(path, blankData, -1);
        return stat.getVersion();
    }

}

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

    private Client client;

    ZKIdTest() {
        super();
        client = new Client(HOST, PORT, TIMEOUT);
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
