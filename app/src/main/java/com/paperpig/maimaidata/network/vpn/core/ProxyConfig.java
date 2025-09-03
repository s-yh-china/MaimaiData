package com.paperpig.maimaidata.network.vpn.core;

import com.paperpig.maimaidata.BuildConfig;
import com.paperpig.maimaidata.network.vpn.tcpip.CommonMethods;

import java.util.ArrayList;
import java.util.HashMap;

public class ProxyConfig {
    public static final ProxyConfig Instance = new ProxyConfig();
    public final static boolean IS_DEBUG = BuildConfig.DEBUG;
    public final static int FAKE_NETWORK_IP = CommonMethods.ipStringToInt("26.25.0.0");
    public static String AppInstallID;
    public static String AppVersion;

    ArrayList<IPAddress> m_IpList;
    ArrayList<IPAddress> m_DnsList;
    HashMap<String, Boolean> m_DomainMap;

    int m_dns_ttl = 10;
    String m_session_name = Constant.TAG;
    int m_mtu = 1500;

    public ProxyConfig() {
        m_IpList = new ArrayList<>();
        m_DnsList = new ArrayList<>();
        m_DomainMap = new HashMap<>();

        m_IpList.add(new IPAddress("26.26.26.2", 32));
        m_DnsList.add(IPAddress.of("119.29.29.29"));
        m_DnsList.add(IPAddress.of("223.5.5.5"));
        m_DnsList.add(IPAddress.of("8.8.8.8"));
    }

    public IPAddress getDefaultLocalIP() {
        return m_IpList.get(0);
    }

    public int getDnsTTL() {
        return m_dns_ttl;
    }

    public String getSessionName() {
        return m_session_name;
    }

    public int getMTU() {
        return m_mtu;
    }

    public void resetDomain(String[] items) {
        m_DomainMap.clear();
        addDomainToHashMap(items, 0, true);
    }

    private void addDomainToHashMap(String[] items, int offset, Boolean state) {
        for (int i = offset; i < items.length; i++) {
            String domainString = items[i].toLowerCase().trim();
            if (domainString.isEmpty()) {
                continue;
            }
            if (domainString.charAt(0) == '.') {
                domainString = domainString.substring(1);
            }
            m_DomainMap.put(domainString, state);
        }
    }

    public boolean needProxy(String host) {
        return true;
    }

    public boolean needProxy(int ip) {
        return true;
    }

    public record IPAddress(String Address, int PrefixLength) {
        public static IPAddress of(String ipAddresString) {
            String[] arrStrings = ipAddresString.split("/");
            String address = arrStrings[0];
            int prefixLength = 32;
            if (arrStrings.length > 1) {
                prefixLength = Integer.parseInt(arrStrings[1]);
            }
            return new IPAddress(address, prefixLength);
        }
    }
}
