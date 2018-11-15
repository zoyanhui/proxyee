package com.github.monkeywie.proxyee;

import com.github.monkeywie.proxyee.proxy.ProxyConfig;
import com.github.monkeywie.proxyee.proxy.ProxyType;
import com.github.monkeywie.proxyee.server.HttpProxyServer;
import com.github.monkeywie.proxyee.server.HttpProxyServerConfig;

public class DynamicHttpProxyServer {

  public static void main(String[] args) throws Exception {
   //new HttpProxyServer().start(9998);
    ProxyConfig proxyConfig = new ProxyConfig();
    proxyConfig.setProxyType(ProxyType.DYNAMIC_HTTP);
    HttpProxyServerConfig config =  new HttpProxyServerConfig();
    config.setBossGroupThreads(1);
    config.setWorkerGroupThreads(1);
    config.setProxyGroupThreads(1);
    new HttpProxyServer()
        .serverConfig(config)
            .proxyConfig(proxyConfig)
        .start(9999);
  }
}
