package org.jetlinks.pro.edge.frp.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.Headers;
import org.jetlinks.pro.device.entity.DeviceInstanceEntity;
import org.jetlinks.pro.device.enums.DeviceState;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.jetlinks.pro.PropertyConstants.accessProvider;
import static org.jetlinks.pro.device.enums.DeviceState.offline;

@Component
@AllArgsConstructor
@Slf4j
public class DeviceEntityEventHandler {

    private final static String OFFICIALEDGEGATEWAY= "official-edge-gateway";

    private final static String FUNCTIONID = "device-mapping-save";

    private final DeviceRegistry registry;

    @EventListener
    public void handleEdgeGatewayDeviceEvent(EntitySavedEvent<DeviceInstanceEntity> event) {
        event.async(
                Flux.fromIterable(event.getEntity())
                    .flatMap(entity -> registry
                            .getDevice(entity.getParentId())
                            .flatMap(parentDevice -> parentDevice
                                    .getProduct()
                                    .flatMap(parentProduct -> parentProduct
                                            .getConfig(accessProvider)
                                            .filter(accessId -> Objects.equals(accessId, OFFICIALEDGEGATEWAY))
                                            .flatMap(accessId -> parentDevice
                                                    .getState()
                                                    .map(DeviceState::of)
                                                    .flatMap(state -> {
                                                        if (Objects.equals(state,offline)){
                                                            entity.setParentId(null);
                                                            return Mono.empty();
                                                        }else {
                                                            Map<String, Object> properties = new HashMap<>();
                                                            Map<String, Object> vel = new HashMap<>();
                                                            vel.put("deviceId",entity.getId());
                                                            vel.put("deviceName",entity.getName());
                                                            properties.put("info",vel);
                                                            return parentDevice
                                                                    .messageSender()
                                                                    .invokeFunction(FUNCTIONID)
                                                                    .setParameter(properties)
                                                                    .header(Headers.force, true)
                                                                    .send()
                                                                    .then();
                                                        }
                                                    })
                                            )
                                    )
                            )
                    )
        );
    }
}
