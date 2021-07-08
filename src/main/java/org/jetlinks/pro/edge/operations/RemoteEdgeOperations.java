package org.jetlinks.pro.edge.operations;

import jdk.nashorn.internal.ir.annotations.Ignore;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Headers;
import org.jetlinks.edge.core.EdgeOperations;
import org.jetlinks.edge.core.entity.EdgeInfoDetail;
import org.jetlinks.edge.core.monitor.EdgeRunningState;
import org.jetlinks.pro.device.service.LocalDeviceInstanceService;
import org.jetlinks.pro.gateway.DeviceMessageUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class RemoteEdgeOperations implements EdgeOperations {

    private final DeviceRegistry registry;

    private final EventBus eventBus;

    @Override
    public Flux<Object> invokeFunction(String edgeDeviceId,
                                       String function,
                                       Map<String, Object> parameter) {
        return registry
            .getDevice(edgeDeviceId)
            .flatMapMany(deviceOperator -> deviceOperator
                .messageSender()
                .invokeFunction(function)
                .setParameter(parameter)
                .header(Headers.force, true)
                .send()
                .flatMap(reply -> {
                    if (reply.getOutput() == null) {
                        return Mono.empty();
                    }
                    return Mono.just(reply.getOutput());
                }))
            ;
    }

    public Flux<Object> invokeFunction(String edgeDeviceId, String function) {
        return this.invokeFunction(edgeDeviceId, function, new HashMap<>());
    }

    @Override
    public Mono<EdgeRunningState> getState(String edgeDeviceId) {

        return registry
            .getDevice(edgeDeviceId)
            .flatMap(device -> device
                .messageSender()
                .readProperty()
                .send()
                .singleOrEmpty()
                .flatMap(msg -> Mono
                    .justOrEmpty(DeviceMessageUtils.tryGetProperties(msg))
                    .map(props -> FastBeanCopier.copy(props, EdgeRunningState::new))));
    }

    @Override
    public Flux<EdgeRunningState> listenSate(String edgeDeviceId) {
        Subscription subscription = Subscription
            .builder()
            .local()
            .broker()
            .topics("/device/*/" + edgeDeviceId + "/properties/*")
            .subscriberId("edge-running-state-listener")
            .build();

        return eventBus
            .subscribe(subscription, DeviceMessage.class)
            .flatMap(msg -> Mono
                .justOrEmpty(DeviceMessageUtils.tryGetProperties(msg))
                .map(props -> FastBeanCopier.copy(props, EdgeRunningState::new)));
    }

    @Override
    public Mono<Object> getDevicePropertySate(String edgeDeviceId, String property) {
        return getState(edgeDeviceId)
            .map(state -> state.getPropertyValue(property));
    }

    /**
     * 通过边缘网关驱动的方式获取网关信息。functionId == "edge-base-config"。
     * 通过设备功能调用实现，发送/返回 的数据平台已经处理。entity 即为 reply.getOutput()
     *
     * @param edgeDeviceId 边缘网关设备ID
     * @return EdgeInfoDetail
     */
    @Override
    public Mono<EdgeInfoDetail> edgeDeviceInfo(String edgeDeviceId) {
        return invokeFunction(edgeDeviceId, "edge-base-config")
            .next()
            .map(entity -> FastBeanCopier.copy(entity, EdgeInfoDetail::new));
    }
}
