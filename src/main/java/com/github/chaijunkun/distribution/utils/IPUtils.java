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

    public static Map<String, List<InetAddress>> getLocalAddress() {
        Map<String, List<InetAddress>> ret = new HashMap<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            NetworkInterface networkInterface;
            Enumeration<InetAddress> inetAddresses;
            InetAddress inetAddress;
            while (networkInterfaces.hasMoreElements()) {
                networkInterface = networkInterfaces.nextElement();
                inetAddresses = networkInterface.getInetAddresses();
                List<InetAddress> ipList = new ArrayList<>();
                while (inetAddresses.hasMoreElements()) {
                    inetAddress = inetAddresses.nextElement();
                    // filter for IPV4
                    if (inetAddress != null && inetAddress instanceof Inet4Address) {
                        ipList.add(inetAddress);
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

    public static InetAddress getDefaultLocalAddressByName(String name) {
        Map<String, List<InetAddress>> localIP = getLocalAddress();
        if (StringUtils.isBlank(name)) {
            List<InetAddress> ipList = localIP.get("eth0");
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
            List<InetAddress> ipList = localIP.get(name);
            if (ipList.size() > 0) {
                return ipList.get(0);
            } else {
                return null;
            }
        }
    }

    public static InetAddress getDefaultLocalAddress() {
        return getDefaultLocalAddressByName(null);
    }

}