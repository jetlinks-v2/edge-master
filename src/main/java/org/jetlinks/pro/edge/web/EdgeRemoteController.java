package org.jetlinks.pro.edge.web;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.pro.config.ConfigManager;
import org.jetlinks.pro.edge.operations.RemoteEdgeOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * 边缘网关-远程控制.
 *
 * @author zhangji 2022/12/20
 */
@RestController
@RequestMapping("/edge/remote")
@Authorize
@Resource(id = "edge-operations", name = "边缘网关-操作")
@AllArgsConstructor
public class EdgeRemoteController {

    private static final String EDGE_BASE_INFO = "edge-base-info";
    private static final String CONFIG_SCOPE   = "paths";
    private static final String CONFIG_DEF_KEY = "frp-path";

    private final RemoteEdgeOperations remoteEdgeOperations;

    private final ConfigManager configManager;

    @GetMapping("/{deviceId:.+}")
    @Operation(summary = "远程控制")
    @ResourceAction(id = "remote", name = "远程控制")
    public Mono<Void> remote(@PathVariable String deviceId,
                             ServerWebExchange exchange) {
        // TODO: 2022/12/20 先获取IP尝试直接访问？ 查询边缘网关的网卡配置

        return remoteUrl(deviceId)
            .doOnNext(url -> {
                URI uri = URI.create(url);
                // 重定向到边缘网关地址
                exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                exchange.getResponse().getHeaders().setLocation(uri);
            })
            .then();
    }

    @GetMapping("/{deviceId:.+}/url")
    @Operation(summary = "获取远程控制地址")
    @ResourceAction(id = "remote", name = "远程控制")
    public Mono<String> remoteUrl(@PathVariable String deviceId) {
        return Mono
            .zip(
                getHost(),
                getEdgeSn(deviceId)
            )
            // 地址 = 内网穿透服务地址 + # + sn码
            .map(tp2 -> tp2.getT1() + "/#/" + tp2.getT2());
    }

    /**
     * 获取边缘网关的SN码
     * @param deviceId 边缘网关id
     * @return sn码
     */
    private Mono<String> getEdgeSn(String deviceId) {
        return remoteEdgeOperations
            .invokeFunction(deviceId, EDGE_BASE_INFO)
            .take(1)
            .singleOrEmpty()
            .map(obj -> JSONObject.parseObject(JSONObject.toJSONString(obj)))
            .map(json -> json.getString("sn"))
            .switchIfEmpty(Mono.error(() -> new BusinessException("error.can_not_get_edge_sn")));
    }

    /**
     * 获取配置的内网穿透服务地址
     * @return 地址
     */
    private Mono<String> getHost() {
        return configManager.getProperties(CONFIG_SCOPE)
            .mapNotNull(valueObject -> valueObject.getString(CONFIG_DEF_KEY, null))
            .switchIfEmpty(Mono.error(() -> new BusinessException("error.frp_path_not_found")));
    }
}
