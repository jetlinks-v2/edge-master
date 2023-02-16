package org.jetlinks.pro.edge.frp;

import org.jetlinks.core.cluster.ServerNode;
import org.jetlinks.edge.core.entity.FrpDistributeInfo;
import org.jetlinks.pro.network.resource.NetworkResource;
import org.jetlinks.pro.network.resource.NetworkTransport;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * frp网络资源管理器.
 *
 * @author zhangji 2023/1/11
 */
public interface FrpNetworkResourceManager {

    /**
     * 获取当前节点信息
     *
     * @return 当前节点信息
     */
    ServerNode getCurrentNode();

    /**
     * 获取全部集群节点
     *
     * @return 集群节点信息
     */
    Flux<ServerNode> getClusterNodes();

    /**
     * 获取集群全部可用的网络资源信息
     *
     * @return 资源信息
     */
    Flux<NetworkResource> getAliveResources();

    /**
     * 获取当前节点可用的网络资源信息
     *
     * @return 资源信息
     */
    Flux<NetworkResource> getCurrentAliveResources();

    /**
     * 获取指定集群节点可用的网络资源信息
     *
     * @param clusterNodeId 集群节点ID
     * @return 资源信息
     */
    Flux<NetworkResource> getAliveResources(String clusterNodeId);

    /**
     * 分配一个可用资源
     * @param deviceId 网关设备ID
     * @param transport 传输协议
     * @return frp下发的配置信息
     */
    Mono<FrpDistributeInfo> distributeResource(String deviceId, NetworkTransport transport);

    /**
     * 判断指定的HOST和端口是否可用
     *
     * @param protocol 网络协议
     * @param host     HOST
     * @param port     端口
     * @return 是否可用
     */
    default Mono<Boolean> isAlive(NetworkTransport protocol, String host, int port) {
        return this
            .getAliveResources()
            .filter(resource -> resource.isSameHost(host) && resource.containsPort(protocol, port))
            .hasElements();
    }

    /**
     * 判断指定集群节点的HOST和端口是否可用
     *
     * @param clusterNodeId 集群节点ID
     * @param protocol      网络协议
     * @param host          HOST
     * @param port          端口
     * @return 是否可用
     */
    default Mono<Boolean> isAlive(String clusterNodeId,
                                  NetworkTransport protocol,
                                  String host,
                                  int port) {
        return this
            .getAliveResources(clusterNodeId)
            .filter(resource -> resource.isSameHost(host) && resource.containsPort(protocol, port))
            .hasElements();
    }
}
