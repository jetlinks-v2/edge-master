package org.jetlinks.pro.edge.operations;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Headers;
import org.jetlinks.edge.core.EdgeOperations;
import org.jetlinks.edge.core.entity.EdgeInfoEntity;
import org.jetlinks.edge.core.monitor.EdgeRunningState;
import org.jetlinks.pro.device.entity.DeviceInstanceEntity;
import org.jetlinks.pro.device.service.LocalDeviceInstanceService;
import org.jetlinks.pro.gateway.DeviceMessageUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@AllArgsConstructor
public class RemoteEdgeOperations implements EdgeOperations {

    private final DeviceRegistry registry;


    private final LocalDeviceInstanceService deviceInstanceService;

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
     * 从平台数据库查询到边缘网关设备的详细数据。组装成 EdgeInfoEntity 返回
     *
     * @param edgeDeviceId 边缘网关设备ID
     * @return EdgeInfoEntity
     */
    @Override
    public Mono<EdgeInfoEntity> edgeDeviceInfo(String edgeDeviceId) {
        return deviceInstanceService
            .getDeviceDetail(edgeDeviceId)
            // EdgeInfoEntity 在 core 包，给 agent 及其他模块使用。从json传递数据
            .map(JSON::toJSON)
            .map(EdgeInfoEntity::convertDeviceInfoToEdgeInfo);
    }
}
