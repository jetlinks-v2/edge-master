package org.jetlinks.pro.edge.frp.server;

import org.jetlinks.edge.core.entity.FrpDistributeInfo;
import org.jetlinks.pro.edge.frp.entity.DistributeRequest;
import org.jetlinks.pro.network.resource.NetworkResource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * frp服务管理器.
 *
 * @author zhangji 2023/1/10
 */
public interface FrpServerManager {

    /**
     * @return 当前的frp服务
     */
    Mono<FrpServer> getServer();

    /**
     * 启动当前的frp服务
     *
     * @return void
     */
    Mono<Void> start();

    /**
     * 停止当前的frp服务
     *
     * @return void
     */
    Mono<Void> stop();

    /**
     * 重启服务
     *
     * @param frpServerConfig 服务配置
     * @return void
     */
    Mono<Void> restart(FrpServerConfig frpServerConfig);

    /**
     * @return 是否启动
     */
    boolean isRunning();

    /**
     * 分配一个随机端口
     *
     * @return 端口
     */
    Mono<FrpDistributeInfo> distributeRandomPort(DistributeRequest request);

    /**
     * 释放一个端口
     * 客户端关闭时调用
     *
     * @param deviceId 网关设备ID
     * @return void
     */
    Mono<Void> restorePort(String deviceId);

    /**
     * 获取所有已分配的端口
     *
     * @return 网络资源信息
     */
    Flux<NetworkResource> getDistributedResource();

    /**
     * 获取设备已分配的端口
     *
     * @param deviceId 网关设备ID
     * @return 网络资源信息
     */
    Mono<NetworkResource> getDistributedResource(String deviceId);

}
