package org.jetlinks.pro.edge.frp.server;

import io.scalecube.services.annotations.ServiceMethod;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.rpc.RpcManager;
import org.jetlinks.core.rpc.RpcService;
import org.jetlinks.edge.core.entity.FrpClientConfig;
import org.jetlinks.edge.core.entity.FrpDistributeInfo;
import org.jetlinks.pro.edge.frp.entity.DistributeRequest;
import org.jetlinks.pro.edge.frp.enums.FrpServerState;
import org.jetlinks.pro.edge.frp.service.FrpServerService;
import org.jetlinks.pro.edge.utils.ReourceFileUtils;
import org.jetlinks.pro.gateway.annotation.Subscribe;
import org.jetlinks.pro.network.resource.NetworkResource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认frp服务管理器.
 *
 * @author zhangji 2023/1/10
 */
@Slf4j
public class DefaultFrpServerManager implements FrpServerManager {

    private static final String FRPS_SHELL_PATH  = "sh";
    private static final String FRPS_INIT_SHELL  = "frps-init.sh";
    private static final String FRPS_START_SHELL = "frps-start.sh";

    // 本节点已分发的网络资源，[设备ID，网络资源]
    private final Map<String, NetworkResource> distributedResource = new ConcurrentHashMap<>();

    private final FrpServerService frpServerService;

    private final FrpServerProvider provider;

    private final RpcManager rpcManager;

    private FrpServer runningServer;

    public DefaultFrpServerManager(FrpServerService frpServerService,
                                   FrpServerProvider provider,
                                   RpcManager rpcManager) {
        this.frpServerService = frpServerService;
        this.provider = provider;
        this.rpcManager = rpcManager;
        rpcManager.registerService(new ServiceImpl());

        ReourceFileUtils.copyShellFile(FRPS_SHELL_PATH, FRPS_INIT_SHELL);
        ReourceFileUtils.copyShellFile(FRPS_SHELL_PATH, FRPS_START_SHELL);

        init();
    }

    @Override
    public Mono<FrpServer> getServer() {
        return Mono.justOrEmpty(runningServer);
    }

    @Override
    public Mono<FrpDistributeInfo> distributeRandomPort(DistributeRequest request) {
        if (Objects.equals(rpcManager.currentServerId(), request.getResource().getClusterNodeId())) {
            return doDistributePort(request);
        }

        return rpcManager
            .getService(request.getResource().getClusterNodeId(), Service.class)
            .flatMap(service -> service.distributePort(request));
    }

    private Mono<FrpDistributeInfo> doDistributePort(DistributeRequest request) {
        if (!isRunning()) {
            return Mono.error(() -> new BusinessException("error.frp_available_server_not_found"));
        }
        return this
            .getRandomPort(request)
            .flatMap(port -> this
                .getServer()
                .map(FrpServer::getConfig)
                .map(FrpServerConfig::toDistributeInfo)
                .doOnNext(distributeInfo -> {
                    FrpClientConfig clientConfig = new FrpClientConfig();
                    clientConfig.setType(FrpDistributeInfo.FrpcType.of(request.getTransport().name()));
                    clientConfig.setRemotePort(port);
                    distributeInfo.setClientConfigList(Collections.singletonList(clientConfig));
                })
            );
    }

    /**
     * 随机分配一个端口
     * 若端口已被使用，则抛出异常
     *
     * @param request 分发端口请求
     * @return 随机端口
     */
    private Mono<Integer> getRandomPort(DistributeRequest request) {
        return Mono.fromSupplier(() -> distributedResource
            .compute(request.getDeviceId(), (key, value) -> {
                if (value != null) {
                    return value;
                }
                return Optional
                    .ofNullable(request.getResource())
                    .map(NetworkResource::getPorts)
                    .map(portMap -> portMap.get(request.getTransport()))
                    .flatMap(ports -> ports.stream().findAny())
                    .map(port -> {
                        distributedResource.values().forEach(res -> {
                            if (res.containsPort(request.getTransport(), port)) {
                                // 端口已被使用
                                throw new BusinessException("error.frp_port_used", port);
                            }
                        });
                        NetworkResource res = new NetworkResource();
                        res.setHost(request.getResource().getHost());
                        res.setClusterNodeId(request.getResource().getClusterNodeId());
                        res.addPorts(request.getTransport(), Collections.singleton(port));
                        return res;
                    })
                    .orElseThrow(() -> new BusinessException("error.frp_available_resource_not_found"));
            }))
            .map(res -> res.getPortList().get(0).getPort());
    }

    @Override
    public Mono<Void> restorePort(String deviceId) {
        return Flux
            .concat(
                doRestorePort(deviceId),
                rpcManager
                    .getServices(Service.class)
                    .map(RpcService::service)
                    .flatMap(service -> service.restorePort(deviceId))
            ).then();
    }

    private Mono<Void> doRestorePort(String deviceId) {
        return Mono.fromRunnable(() -> distributedResource.remove(deviceId));
    }

    @Override
    public Flux<NetworkResource> getDistributedResource() {
        return Flux.fromIterable(distributedResource.values());
    }

    @Override
    public Mono<NetworkResource> getDistributedResource(String deviceId) {
        return Mono.justOrEmpty(distributedResource.get(deviceId));
    }

    @Override
    public Mono<Void> start() {
        if (isRunning()) {
            return Mono.empty();
        }
        if (provider == null) {
            return Mono.error(() -> new BusinessException("error.frp_not_enable"));
        }
        return provider
            .getMatchedServer()
            .flatMap(this::startServer);
    }

    @Override
    public Mono<Void> stop() {
        if (isRunning()) {
            runningServer.stop();
            runningServer = null;
        }
        return Mono.empty();
    }

    @Override
    public Mono<Void> restart(FrpServerConfig frpServerConfig) {
        return Mono.fromRunnable(() -> doStartServer(runningServer, frpServerConfig));
    }

    @Override
    public boolean isRunning() {
        return runningServer != null;
    }

    private void init() {
        if (provider == null) {
            log.warn("frp is not enabled");
            return;
        }
        provider.getMatchedServer()
            .doOnNext(server -> server.init(FRPS_INIT_SHELL))
            .flatMap(this::startServer)
            .onErrorResume(error -> {
                log.error("init frp server error. ", error);
                return stop();
            })
            .subscribe();
    }

    @Subscribe("/device/*/*/message/event/stop-frp")
    public Mono<Void> handleFrpStop(EventMessage message) {
        return Mono.just(message)
            .map(CommonDeviceMessage::getDeviceId)
            .flatMap(this::restorePort);
    }

    private Mono<Void> startServer(@NotNull FrpServer server) {
        return frpServerService
            .getClusterFrpServerConfig(rpcManager.currentServerId())
            .filter(config -> config.getState() == FrpServerState.enabled)
            .switchIfEmpty(Mono.error(() -> new BusinessException("error.frp_no_server_config_avaliable")))
            .doOnNext(config -> doStartServer(server, config.getFrpServerConfig()))
            .then();
    }

    private void doStartServer(FrpServer server,
                               FrpServerConfig config) {
        if (server == null) {
            throw new BusinessException("error.frp_available_server_not_found");
        }
        config.validate();
        server.setConfig(config);
        server.start(FRPS_START_SHELL);

        this.runningServer = server;
    }

    public class ServiceImpl implements Service {
        @Override
        public Mono<FrpDistributeInfo> distributePort(DistributeRequest request) {
            return doDistributePort(request);
        }

        @Override
        public Mono<Void> restorePort(String deviceId) {
            return doRestorePort(deviceId);
        }

    }

    @io.scalecube.services.annotations.Service
    public interface Service {
        @ServiceMethod
        Mono<FrpDistributeInfo> distributePort(DistributeRequest request);

        @ServiceMethod
        Mono<Void> restorePort(String deviceId);
    }

}
