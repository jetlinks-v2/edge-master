package org.jetlinks.pro.edge.service;

import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.pro.PropertyConstants;
import org.jetlinks.pro.gateway.annotation.Subscribe;
import org.jetlinks.pro.template.entity.EntityTemplateEntity;
import org.jetlinks.pro.template.impl.EntityTemplateService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 边缘网关-资源库事件处理.
 *
 * @author zhangji 2022/12/2
 */
@Component
public class EdgeTemplateEventHandler {

    private final EntityTemplateService entityTemplateService;

    private final DeviceRegistry deviceRegistry;

    public EdgeTemplateEventHandler(EntityTemplateService entityTemplateService,
                                    DeviceRegistry deviceRegistry) {
        this.entityTemplateService = entityTemplateService;
        this.deviceRegistry = deviceRegistry;
    }

    /**
     * 保存边缘网关上报资源库事件
     */
    @Subscribe("/device/*/*/message/event/entity-template-upload")
    public Mono<SaveResult> handleEvent(EventMessage message) {
        return Mono.just(message)
            .map(msg -> FastBeanCopier.copy(msg.getData(), new EntityTemplateEntity()))
            .flatMap(entity -> this
                .getDeviceName(entity.getSourceId())
                .map(sourceName -> {
                    entity.setSourceName(sourceName);
                    return entity;
                }))
            .as(entityTemplateService::save);
    }

    private Mono<String> getDeviceName(String deviceId) {
        return deviceRegistry
            .getDevice(deviceId)
            .flatMap(deviceOperator -> deviceOperator.getSelfConfig(PropertyConstants.deviceName));
    }
}
