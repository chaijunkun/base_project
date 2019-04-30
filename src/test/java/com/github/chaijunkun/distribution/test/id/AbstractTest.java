package com.github.chaijunkun.distribution.test.id;

import com.github.chaijunkun.distribution.utils.IPUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

import java.net.InetAddress;
import java.text.SimpleDateFormat;

@Slf4j
public abstract class AbstractTest {

    protected static final String BIZ_ID = "order_id";

    protected static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

    @Getter
    private String universalFlag = "";

    private void setUnversalFlag() {
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

    AbstractTest() {
        this.setUnversalFlag();
    }

    @BeforeTest
    public void before() {
        log.info("start to run");
    }

    @AfterTest
    public void after() {
        log.info("stop");
    }

}
