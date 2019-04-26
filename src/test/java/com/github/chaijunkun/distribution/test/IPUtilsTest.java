package com.github.chaijunkun.distribution.test;

import com.github.chaijunkun.distribution.utils.IPUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class IPUtilsTest {

    @Test
    public void traverseIP() {
        try {

            log.info("default: {}", IPUtils.getDefaultLocalIP());
            Map<String, List<String>> localIP = IPUtils.getLocalIP();
            if (!localIP.isEmpty()) {
                Set<Map.Entry<String, List<String>>> entries = localIP.entrySet();
                Iterator<Map.Entry<String, List<String>>> localIPIterator = entries.iterator();
                while (localIPIterator.hasNext()) {
                    Map.Entry<String, List<String>> next = localIPIterator.next();
                    String interfaceName = next.getKey();
                    List<String> ipList = next.getValue();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < ipList.size(); i++) {
                        sb.append(ipList.get(i)).append(",");
                    }
                    if (sb.length() > 0) {
                        sb.deleteCharAt(sb.length() - 1);
                    }
                    log.info("interface: {}, ip: {}", interfaceName, sb.toString());
                }

            }
        } catch (Exception e) {
            log.error("fail to load ip", e);
        }
    }


}
