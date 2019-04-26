package com.github.chaijunkun.distribution.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.*;
import java.util.*;

/**
 * IP地址组件
 * @author chaijunkun
 */
@Slf4j
public class IPUtils {

    public static Map<String, List<String>> getLocalIP() {
        Map<String, List<String>> ret = new HashMap<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            NetworkInterface networkInterface;
            Enumeration<InetAddress> inetAddresses;
            InetAddress inetAddress;
            String ip;
            while (networkInterfaces.hasMoreElements()) {
                networkInterface = networkInterfaces.nextElement();
                inetAddresses = networkInterface.getInetAddresses();
                List<String> ipList = new ArrayList<>();
                while (inetAddresses.hasMoreElements()) {
                    inetAddress = inetAddresses.nextElement();
                    // filter for IPV4
                    if (inetAddress != null && inetAddress instanceof Inet4Address) {
                        ip = inetAddress.getHostAddress();
                        ipList.add(ip);
                    }
                }
                if (ipList.size() > 0) {
                    ret.put(networkInterface.getName(), ipList);
                }
            }
        } catch (SocketException e) {
            log.error("fail to load network interface information", e);
        }
        return ret;
    }

    public static String getDefaultLocalIPByName(String name) {
        Map<String, List<String>> localIP = getLocalIP();
        if (StringUtils.isBlank(name)) {
            List<String> ipList = localIP.get("eth0");
            if (CollectionUtils.isNotEmpty(ipList)) {
                return ipList.get(0);
            } else {
                ipList = localIP.get("en0");
                if (CollectionUtils.isNotEmpty(ipList)) {
                    return ipList.get(0);
                } else {
                    return null;
                }

            }
        } else {
            List<String> ipList = localIP.get(name);
            if (ipList.size() > 0) {
                return ipList.get(0);
            } else {
                return null;
            }
        }
    }

    public static String getDefaultLocalIP() {
        return getDefaultLocalIPByName(null);
    }

}