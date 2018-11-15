package com.github.monkeywie.proxyee.proxy;

import static com.github.monkeywie.proxyee.proxy.ProxyType.DYNAMIC_HTTP;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import java.net.InetSocketAddress;

public class ProxyHandleFactory {
  private static ProxyListHandler proxyListHandler;

  public static void init(ProxyConfig proxyConfig){
      if(proxyConfig.getProxyType() == DYNAMIC_HTTP) {
          proxyListHandler = new ProxyListHandler();
          proxyListHandler.start();
      }
  }

  public static ProxyHandler build(ProxyConfig config) {
    ProxyHandler proxyHandler = null;
    if (config != null) {
      boolean isAuth = config.getUser() != null && config.getPwd() != null;
      InetSocketAddress inetSocketAddress = null;
      if(config.getProxyType() != DYNAMIC_HTTP) {
        inetSocketAddress = new InetSocketAddress(config.getHost(),
                config.getPort());
      }
      switch (config.getProxyType()) {
        case DYNAMIC_HTTP:
          String proxy = proxyListHandler.getProxy();
          String[] split = proxy.split(":");
          inetSocketAddress = new InetSocketAddress(split[0], Integer.valueOf(split[1]));
          proxyHandler = new HttpProxyHandler(inetSocketAddress);
          break;
        case HTTP:
          if (isAuth) {
            proxyHandler = new HttpProxyHandler(inetSocketAddress,
                config.getUser(), config.getPwd());
          } else {
            proxyHandler = new HttpProxyHandler(inetSocketAddress);
          }
          break;
        case SOCKS4:
          proxyHandler = new Socks4ProxyHandler(inetSocketAddress);
          break;
        case SOCKS5:
          if (isAuth) {
            proxyHandler = new Socks5ProxyHandler(inetSocketAddress,
                config.getUser(), config.getPwd());
          } else {
            proxyHandler = new Socks5ProxyHandler(inetSocketAddress);
          }
          break;
      }
    }
    return proxyHandler;

  }
}
