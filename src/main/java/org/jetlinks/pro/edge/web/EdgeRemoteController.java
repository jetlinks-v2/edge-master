package org.jetlinks.pro.edge.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.edge.core.entity.FrpDistributeReply;
import org.jetlinks.pro.edge.frp.FrpNetworkResourceManager;
import org.jetlinks.pro.edge.frp.server.FrpServerManager;
import org.jetlinks.pro.edge.frp.service.FrpServerService;
import org.jetlinks.pro.edge.operations.RemoteEdgeOperations;
import org.jetlinks.pro.network.resource.NetworkTransport;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;

/**
 * 边缘网关-远程控制.
 *
 * @author zhangji 2022/12/20
 */
@RestController
@RequestMapping("/edge/remote")
@Authorize
@Tag(name = "边缘网关-远程控制")
@Resource(id = "edge-operations", name = "边缘网关-操作")
@AllArgsConstructor
public class EdgeRemoteController {

    private static final String DISTRIBUTE_FUNCTION = "distribute-port";
    private static final String STOP_FUNCTION       = "stop-frp";

    private final RemoteEdgeOperations remoteEdgeOperations;

    private final FrpServerManager frpServerManager;

    private final FrpNetworkResourceManager resourceManager;

    private final FrpServerService service;

    @GetMapping("/{deviceId:.+}")
    @Operation(summary = "远程控制")
    @ResourceAction(id = "remote", name = "远程控制")
    public Mono<Void> remote(@PathVariable String deviceId,
                             ServerWebExchange exchange) {
        return remoteUrl(deviceId)
            .doOnNext(reply -> {
                URI uri = URI.create(reply.getUrl());
                // 重定向到边缘网关地址
                exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                exchange.getResponse().getHeaders().add("x-access-token", reply.getToken());
                exchange.getResponse().getHeaders().setLocation(uri);
            })
            .then();
    }

    @GetMapping("/{deviceId:.+}/url")
    @Operation(summary = "获取远程控制地址")
    @ResourceAction(id = "remote", name = "远程控制")
    public Mono<FrpDistributeReply> remoteUrl(@PathVariable String deviceId) {
        return getHost(deviceId);
    }

    @PostMapping("/{deviceId:.+}/stop")
    @Operation(summary = "关闭远程控制")
    @ResourceAction(id = "remote", name = "远程控制")
    public Mono<Void> stop(@PathVariable String deviceId) {
        return frpServerManager
            // 释放服务端资源
            .restorePort(deviceId)
            .then(
                // 关闭客户端
                remoteEdgeOperations
                    .invokeFunction(deviceId, STOP_FUNCTION)
                    .then()
            );
    }

    /**
     * 获取配置的内网穿透服务地址
     *
     * @return 地址
     */
    @SuppressWarnings("unchecked")
    private Mono<FrpDistributeReply> getHost(String deviceId) {
        return resourceManager
            .distributeResource(deviceId, NetworkTransport.TCP)
            .map(config -> FastBeanCopier.copy(config, new HashMap()))
            // 调用边缘网关功能
            .flatMap(config -> remoteEdgeOperations
                .invokeFunction(deviceId, DISTRIBUTE_FUNCTION, config)
                .next())
            .map(reply -> FastBeanCopier.copy(reply, new FrpDistributeReply()));

    }
}
