package com.github.chaijunkun.distribution.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * zookeeper客户端
 * @author chaijunkun
 */
@Slf4j
public class ZookeeperClient implements Watcher, Closeable {

    private static final byte[] blankData = new byte[]{0};

    private String host;

    private int port;

    private int timeout;

    private ZooKeeper zk;

    /**
     * semaphore for waiting connection established
     */
    private CountDownLatch connectedSemaphore = new CountDownLatch(1);

    public ZookeeperClient(String host, int port, int timeout) {
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

    public synchronized void reconnect() {
        log.warn("try reconnect...");
        this.close();

        try {
            this.connect();
        } catch (IOException e) {
            log.error("reconnect error! ", e);
        }

    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        //get event state
        Event.KeeperState keeperState = watchedEvent.getState();
        Event.EventType eventType = watchedEvent.getType();
        switch (keeperState) {
            //if event state is SyncConnected
            case SyncConnected:
            {
                switch (eventType) {
                    // and event type is None
                    case None:
                    {
                        // countdown semaphore, then the previous await statement released
                        connectedSemaphore.countDown();
                        log.info("zk connected");
                        break;
                    }
                    default:
                        break;
                }
                break;
            }
            case Expired:
            {
                this.reconnect();
                break;
            }
            default:
                break;
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
