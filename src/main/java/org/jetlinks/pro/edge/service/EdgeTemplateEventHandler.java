package org.jetlinks.pro.edge.service;

import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.message.event.EventMessage;
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

    public EdgeTemplateEventHandler(EntityTemplateService entityTemplateService) {
        this.entityTemplateService = entityTemplateService;
    }

    /**
     * 保存边缘网关上报资源库事件
     */
    @Subscribe("/device/*/*/message/event/entity-template-upload")
    public Mono<SaveResult> handleEvent(EventMessage message) {
        return Mono.just(message)
            .map(msg -> FastBeanCopier.copy(message.getData(), new EntityTemplateEntity()))
            .as(entityTemplateService::save);
    }
}
