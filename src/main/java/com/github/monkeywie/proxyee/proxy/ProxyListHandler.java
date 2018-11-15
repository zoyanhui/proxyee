package com.github.monkeywie.proxyee.proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;

/**
 * Author: Songhui Cui <cuisonghui@xiaomi.com>
 * Date: 2017-07-20 Time: 10:31
 */
public class ProxyListHandler {

    private DictQueue<String, Long> proxyQueue;

    class DictQueue<K, V> {

        private int capacity;
        private Map<K, V> m;
        private Set<Map.Entry<K, V>> entrySet;

        DictQueue(int capacity) {
            this.capacity = capacity;
            m = Collections.synchronizedMap(new LinkedHashMap<K, V>());
            entrySet = m.entrySet();
        }

        /**
         * if exists, overwrite old
         *
         * @param k
         * @param v
         */
        public void put(K k, V v) {
            synchronized (m) {
                if (m.size() == capacity) {
                    return;
                }
                m.put(k, v);
            }
        }

        /**
         * if exists, abort put
         *
         * @param k
         * @param v
         */
        public void putIfAbsent(K k, V v) {
            synchronized (m) {
                if (m.size() == capacity) {
                    return;
                }
                m.putIfAbsent(k, v);
            }
        }

        public Map.Entry<K, V> take() throws InterruptedException {
            while (true) {
                synchronized (m) {
                    Iterator<Map.Entry<K, V>> iterator = entrySet.iterator();
                    if (iterator.hasNext()) {
                        Map.Entry<K, V> next = iterator.next();
                        iterator.remove();
                        return next;
                    }
                }
                Thread.sleep(100);
            }
        }

        public int size() {
            return m.size();
        }
    }

    @PostConstruct
    public void start() {
        proxyQueue = new DictQueue<>(1000);
        Thread t = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        updateIpListFromProxyServer();
                    } catch (IOException e) {
                        System.out.println("update proxy error");
                    }
                    try {
                        Thread.sleep(1000 * 10);
                    } catch (InterruptedException e) {
                        System.out.println("Get interrupt exception, exit");
                    }
                }
            }
        });

        t.setDaemon(true);
        t.start();
    }

    public void updateIpListFromProxyServer() throws IOException {
        URL obj = new URL("http://api.ip.data5u.com/xiaomi/get.html?order=XM99832741F123OPLK0923UW443&ttl");
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder resposne = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            resposne.append(inputLine + "\n");
        }
        in.close();

        String content = resposne.toString();
        if (!content.contains("false")) {
            List<String> ipList = Arrays.asList(content.trim().split("\n"));
            int count = pickProxies(ipList);
            System.out.println(String.format("from proxyserver get proxy %d/%d, now in queue:%d", count, ipList.size(), proxyQueue.size()));
        } else {
            System.out.println("proxyserver err: " + content);
        }

    }

    private int pickProxies(List<String> ipList) {
        long start = System.currentTimeMillis();
        System.out.println("start picking proxies");
        final AtomicInteger count = new AtomicInteger();
        for (int i = 0; i < ipList.size(); i++) {
            String ipInfo = ipList.get(i);
            String[] ipInfos = ipInfo.split(",");
            long fetchTime = System.currentTimeMillis() + Long.valueOf(ipInfos[1]) - 5000;
            if (canConnect(ipInfos[0])) {
                proxyQueue.put(ipInfos[0], fetchTime);
                count.addAndGet(1);
            }
        }
        System.out.println("end picking proxies, cost:" + (System.currentTimeMillis() - start));
        return count.get();
    }

    private boolean canConnect(final String proxy) {
        String[] ipAndPort = proxy.split(":", 2);
        if (ipAndPort.length != 2) {
            return false;
        }
        Socket socket = null;
        try {
            socket = new Socket();
            socket.setReuseAddress(true);
            SocketAddress sa = new InetSocketAddress(ipAndPort[0], Integer.valueOf(ipAndPort[1]));
            socket.connect(sa, 3000);
            if (!socket.isConnected()) {
                System.out.println("connect proxy failed: " + proxy);
                return false;
            }
        } catch (IOException e) {
            System.out.println("connect proxy IOException, " + proxy + ", ERROR:" + e.getMessage());

            return false;
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    //无忧代理
    public String getProxy() {
        String ipPort = null;
        try {
            while (true) {
                Map.Entry<String, Long> proxyEntry = proxyQueue.take();
                if (proxyEntry.getValue() - System.currentTimeMillis() >= 0) {
                    ipPort = proxyEntry.getKey();
                    proxyQueue.putIfAbsent(proxyEntry.getKey(), proxyEntry.getValue());
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("take proxy from dict queue exception. " + e);
        }

        if (ipPort != null) {
            return ipPort;
        }
        return null;
    }

}
