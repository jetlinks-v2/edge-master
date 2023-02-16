package org.jetlinks.pro.edge.frp.server;

import reactor.core.publisher.Mono;

/**
 * frp服务提供商.
 * 可注册服务，然后根据当前系统环境提供服务
 *
 * @author zhangji 2023/1/11
 */
public interface FrpServerProvider {

    /**
     * 注册frp服务
     * @param server frp服务
     */
    void register(FrpServer server);

    /**
     * @return 返回一个当前系统匹配的frp服务
     */
    Mono<FrpServer> getMatchedServer();

}
