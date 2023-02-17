package org.jetlinks.pro.edge.frp;

import io.scalecube.services.annotations.ServiceMethod;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.cluster.ServerNode;
import org.jetlinks.core.rpc.RpcManager;
import org.jetlinks.edge.core.entity.FrpDistributeInfo;
import org.jetlinks.pro.edge.frp.entity.DistributeRequest;
import org.jetlinks.pro.edge.frp.entity.FrpServerConfigInfo;
import org.jetlinks.pro.edge.frp.server.FrpServerConfig;
import org.jetlinks.pro.edge.frp.server.FrpServerManager;
import org.jetlinks.pro.edge.frp.service.FrpServerService;
import org.jetlinks.pro.network.resource.NetworkResource;
import org.jetlinks.pro.network.resource.NetworkTransport;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * 默认frp网络资源管理器.
 *
 * @author zhangji 2023/1/11
 */
@Component
public class DefaultFrpNetworkResourceManager implements FrpNetworkResourceManager {

    private final FrpServerService service;

    private final RpcManager rpcManager;

    private final FrpServerManager serverManager;

    public DefaultFrpNetworkResourceManager(FrpServerService service,
                                            RpcManager rpcManager,
                                            FrpServerManager serverManager) {
        this.service = service;
        this.rpcManager = rpcManager;
        this.serverManager = serverManager;
        rpcManager.registerService(new ServiceImpl());
    }

    @Override
    public ServerNode getCurrentNode() {
        return ServerNode
            .builder()
            .id(rpcManager.currentServerId())
            .name(rpcManager.currentServerId())
            .build();
    }

    @Override
    public Flux<ServerNode> getClusterNodes() {
        return rpcManager
            .getServices(Service.class)
            .map(service -> ServerNode
                .builder()
                .id(service.serverNodeId())
                .name(service.serverNodeId())
                .build())
            .concatWithValues(getCurrentNode());
    }

    @Override
    public Flux<NetworkResource> getAliveResources() {
        return Flux.merge(
            getLocalAliveResources(),
            rpcManager
                .getServices(Service.class)
                .flatMap(s -> s.service().aliveResources())
        );
    }

    @Override
    public Flux<NetworkResource> getCurrentAliveResources() {
        return getLocalAliveResources();
    }

    @Override
    public Flux<NetworkResource> getAliveResources(String clusterNodeId) {
        if (Objects.equals(rpcManager.currentServerId(), clusterNodeId)) {
            return getLocalAliveResources();
        }

        return rpcManager
            .getService(clusterNodeId, Service.class)
            .flatMapMany(Service::aliveResources);
    }

    /**
     * 获取配置的内网穿透服务地址
     *
     * @return 地址
     */
    @Override
    public Mono<FrpDistributeInfo> distributeResource(String deviceId, NetworkTransport transport) {
        return serverManager
            // 先查询设备已分配的网络资源
            .getDistributedResource(deviceId)
            // 获取一个可用的网络资源
            .switchIfEmpty(this.getAliveResources().next())
            .switchIfEmpty(Mono.error(() -> new BusinessException("error.frp_available_resource_not_found")))
            .flatMap(resource -> serverManager
                .distributeRandomPort(DistributeRequest.of(deviceId, transport, resource)));
    }

    private Flux<NetworkResource> getLocalAliveResources() {
        if (!serverManager.isRunning()) {
            return Flux.error(() -> new BusinessException("error.frp_available_server_not_found"));
        }
        return service
            .getClusterFrpServerConfig(rpcManager.currentServerId())
            .map(FrpServerConfigInfo::getFrpServerConfig)
            .flatMapIterable(FrpServerConfig::parseResources)
            .map(NetworkResource::copy)
            .flatMap(resource -> serverManager
                .getDistributedResource()
                .doOnNext(port -> resource.removePorts(port.getPorts()))
                .then()
                .thenReturn(resource))
            .doOnNext(resource -> resource.setClusterNodeId(rpcManager.currentServerId()));
    }

    public class ServiceImpl implements Service {
        @Override
        public Flux<NetworkResource> aliveResources() {
            return getLocalAliveResources();
        }
    }

    @io.scalecube.services.annotations.Service
    public interface Service {
        @ServiceMethod
        Flux<NetworkResource> aliveResources();
    }
}
