package org.jetlinks.pro.edge.frp;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.rpc.RpcManager;
import org.jetlinks.pro.edge.frp.entity.FrpServerConfigEntity;
import org.jetlinks.pro.edge.frp.entity.FrpServerConfigInfo;
import org.jetlinks.pro.edge.frp.server.DefaultFrpServerManager;
import org.jetlinks.pro.edge.frp.server.DefaultFrpServerProvider;
import org.jetlinks.pro.edge.frp.server.FrpProperties;
import org.jetlinks.pro.edge.frp.server.FrpServer;
import org.jetlinks.pro.edge.frp.server.FrpServerConfig;
import org.jetlinks.pro.edge.frp.server.FrpServerManager;
import org.jetlinks.pro.edge.frp.server.FrpServerProvider;
import org.jetlinks.pro.edge.frp.server.FrpSystemHepler;
import org.jetlinks.pro.edge.frp.server.impl.FrpServerArm64;
import org.jetlinks.pro.edge.frp.server.impl.FrpServerX86;
import org.jetlinks.pro.edge.frp.service.FrpServerService;
import org.jetlinks.pro.network.resource.NetworkTransport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;

/**
 * frp服务生命周期-单元测试.
 *
 * @author zhangji 2023/2/14
 */
@Slf4j
public class FrpManagerTest {
    private static final String TEST_TYPE = "test";
    private static final String NODE_ID   = "test";
    private static final String TOKEN     = "test";
    private static final int    BIND_PORT = 7000;
    private static final String DEVICE_1  = "test1";
    private static final String DEVICE_2  = "test2";
    private static final String DEVICE_3  = "test3";

    private FrpProperties properties;

    @BeforeEach
    void init() {
        properties = new FrpProperties();
        properties.setEnabled(true);
        FrpServerConfig config = new FrpServerConfig();
        config.setPublicHost("0.0.0.0");
        config.setBindPort(BIND_PORT);
        config.setToken(TOKEN);
        // 分配2个端口
        config.setResources(Collections.singletonList("8811-8812/tcp"));
        properties.setConfig(config);
    }


    @Test
    void test() {
        FrpSystemHepler frpSystemHepler = mockFrpSystemHelper();
        FrpServerProvider provider = new DefaultFrpServerProvider(frpSystemHepler);
        FrpServer arm64Server = new FrpServerArm64();
        FrpServer x86Server = new FrpServerX86();
        FrpServer mockServer = mockServer();
        RpcManager rpcManager = Mockito.mock(RpcManager.class);
        Mockito.when(rpcManager.currentServerId()).thenReturn(NODE_ID);
        Mockito.when(rpcManager.getServices(Mockito.any())).thenReturn(Flux.empty());

        // 注册frp服务
        provider.register(arm64Server);
        provider.register(x86Server);
        provider.register(mockServer);

        FrpServerService service = new FrpServerService(properties, rpcManager) {
            @Override
            public ReactiveRepository<FrpServerConfigEntity, String> getRepository() {
                ReactiveRepository repository = Mockito.mock(ReactiveRepository.class);
                ReactiveQuery query = Mockito.mock(ReactiveQuery.class);

                Mockito.when(repository.createQuery()).thenReturn(query);
                Mockito.when(query.where(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any())).thenReturn(query);

                FrpServerConfigEntity entity = FrpServerConfigEntity.of(FrpServerConfigInfo.of(properties));
                entity.setClusterNodeId(NODE_ID);

                Mockito.when(query.fetchOne()).thenReturn(Mono.just(entity));
                return repository;
            }
        };
        FrpServerManager manager = new DefaultFrpServerManager(service, provider, rpcManager);

        manager
            .getServer()
            .as(StepVerifier::create)
            .expectNextMatches(server -> server.getType().equals(TEST_TYPE))
            .verifyComplete();

        manager
            .start()
            .as(StepVerifier::create)
            .verifyComplete();

        FrpNetworkResourceManager resourceManager = new DefaultFrpNetworkResourceManager(service, rpcManager, manager);

        // 分发端口给设备1，共2个端口
        resourceManager
            .distributeResource(DEVICE_1, NetworkTransport.TCP)
            .doOnNext(info -> log.info("distributeRandomPort finished. deviceId: {}, remotePort: {}", DEVICE_1, info.getClientConfigList().get(0).getRemotePort()))
            .as(StepVerifier::create)
            .expectNextMatches(info -> info.getBindPort() == BIND_PORT && info.getToken().equals(TOKEN))
            .verifyComplete();

        // 再次分发端口给设备1，重用端口
        resourceManager
            .distributeResource(DEVICE_1, NetworkTransport.TCP)
            .doOnNext(info -> log.info("distributeRandomPort finished. deviceId: {}, remotePort: {}",
                    DEVICE_1, info.getClientConfigList().get(0).getRemotePort()))
            .as(StepVerifier::create)
            .expectNextMatches(info -> info.getBindPort() == BIND_PORT && info.getToken().equals(TOKEN))
            .verifyComplete();

        // 分发端口给设备2，共2个端口，正常分发
        resourceManager
            .distributeResource(DEVICE_2, NetworkTransport.TCP)
            .doOnNext(info -> log.info("distributeRandomPort finished. deviceId: {}, remotePort: {}",
                DEVICE_2, info.getClientConfigList().get(0).getRemotePort()))
            .as(StepVerifier::create)
            .expectNextMatches(info -> info.getBindPort() == BIND_PORT && info.getToken().equals(TOKEN))
            .verifyComplete();

        // 分发端口给设备3，无可用端口
        resourceManager
            .distributeResource(DEVICE_3, NetworkTransport.TCP)
            .as(StepVerifier::create)
            .expectError(BusinessException.class)
            .verify();

        // 回收设备2的资源后，正常分发给设备3
        manager.restorePort(DEVICE_2)
            .as(StepVerifier::create)
            .verifyComplete();
        resourceManager
            .distributeResource(DEVICE_3, NetworkTransport.TCP)
            .as(StepVerifier::create)
            .expectNextMatches(info -> info.getBindPort() == BIND_PORT && info.getToken().equals(TOKEN))
            .verifyComplete();

        manager
            .stop()
            .as(StepVerifier::create)
            .verifyComplete();

    }

    private FrpSystemHepler mockFrpSystemHelper() {
        FrpSystemHepler frpSystemHepler = Mockito.mock(FrpSystemHepler.class);
        Mockito.when(frpSystemHepler.getSystemVersion()).thenReturn(TEST_TYPE);
        Mockito.when(frpSystemHepler.isLinux()).thenReturn(true);
        return frpSystemHepler;
    }

    private FrpServer mockServer() {
        FrpServer server = Mockito.mock(FrpServer.class);
        Mockito.when(server.getType()).thenReturn(TEST_TYPE);
        Mockito.when(server.matchRelease(Mockito.anyString())).thenReturn(true);

        FrpServerConfig config = new FrpServerConfig();
        config.setPublicHost("0.0.0.0");
        config.setBindPort(BIND_PORT);
        config.setToken(TOKEN);
        config.setResources(Arrays.asList("8811-8820/tcp"));
        Mockito.when(server.getConfig()).thenReturn(config);

        return server;
    }
}
