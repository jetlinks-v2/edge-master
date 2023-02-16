package org.jetlinks.pro.edge.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.pro.edge.frp.entity.FrpServerConfigEntity;
import org.jetlinks.pro.edge.frp.entity.FrpServerConfigInfo;
import org.jetlinks.pro.edge.frp.server.FrpServerManager;
import org.jetlinks.pro.edge.frp.service.FrpServerService;
import org.jetlinks.pro.web.response.ValidationResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 输入描述.
 *
 * @author zhangji 2023/1/12
 */
@RestController
@RequestMapping("/edge/frp")
@Authorize
@Tag(name = "边缘网关-远程控制配置")
@Resource(id = "edge-operations", name = "边缘网关-操作")
public class FrpServerController implements ReactiveServiceCrudController<FrpServerConfigEntity, String> {

    @Getter
    private final FrpServerService service;

    private final FrpServerManager serverManager;

    public FrpServerController(FrpServerService service,
                               FrpServerManager serverManager) {
        this.service = service;
        this.serverManager = serverManager;
    }

    @GetMapping("/config")
    @Operation(summary = "获取frp服务配置信息")
    public Mono<FrpServerConfigInfo> getFrpProperties(@RequestParam @Parameter(description = "集群节点ID") String clusterNodeId) {
        return service.getClusterFrpServerConfig(clusterNodeId);
    }

    @PatchMapping("/config")
    @Operation(summary = "保存frp服务配置")
    public Mono<SaveResult> saveFrpProperties(@RequestBody Mono<FrpServerConfigInfo> mono) {
        return mono
            .flatMap(config -> service
                .save(FrpServerConfigEntity.of(config))
                .flatMap(result -> serverManager
                    .restart(config.getFrpServerConfig())
                    .thenReturn(result)));
    }

    @GetMapping("/port/_validate")
    @QueryAction
    @Operation(summary = "验证端口是否合法", description = "同一节点的端口不能重复")
    public Mono<ValidationResult> portValidate(@RequestParam @Parameter(description = "端口") int bindPort,
                                               @RequestParam @Parameter(description = "服务所在节点ID") String clusterNodeId) {
        return LocaleUtils.currentReactive()
            .flatMap(locale -> {
                FrpServerConfigEntity entity = new FrpServerConfigEntity();
                entity.setClusterNodeId(clusterNodeId);
                entity.setBindPort(bindPort);
                entity.tryValidate("bindPort", CreateGroup.class);

                return service
                    .findById(entity.getId())
                    .map(serverConfig -> ValidationResult
                        .error(LocaleUtils
                            .resolveMessage("error.frp_port_used", locale, "The port is already used:" + bindPort, bindPort)));
            })
            .defaultIfEmpty(ValidationResult.success())
            .onErrorResume(ValidationException.class, e -> Mono.just(e.getI18nCode())
                .map(ValidationResult::error));
    }

}
