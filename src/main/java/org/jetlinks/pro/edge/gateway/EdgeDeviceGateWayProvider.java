package org.jetlinks.pro.edge.gateway;

import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.pro.gateway.DeviceGateway;
import org.jetlinks.pro.gateway.supports.DeviceGatewayProperties;
import org.jetlinks.pro.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.mqtt.gateway.device.MqttServerDeviceGateway;
import org.jetlinks.pro.network.mqtt.server.MqttServer;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

//参考 MqttServerDeviceGatewayProvider
@Component
public class EdgeDeviceGateWayProvider implements DeviceGatewayProvider {

    //网络组件管理器，用于统一管理网络组件
    private final NetworkManager networkManager;

    //设备注册中心
    private final DeviceRegistry registry;

    //设备会话管理器
    private final DeviceSessionManager sessionManager;

    //客户端消息解码处理接口（解码后的设备消息处理器）
    private final DecodedClientMessageHandler messageHandler;

    //协议支持，EdgeProtocolSupport
    private final ProtocolSupports protocolSupports;

    public EdgeDeviceGateWayProvider(NetworkManager networkManager,
                                     DeviceRegistry registry,
                                     DeviceSessionManager sessionManager,
                                     DecodedClientMessageHandler messageHandler,
                                     ProtocolSupports protocolSupports) {
        this.networkManager = networkManager;
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.messageHandler = messageHandler;
        this.protocolSupports = protocolSupports;
    }


    /**
     * 网关提供者id
     * 同时也是消息协议id，参看 EdgeProtocolSupport
     * @return
     */
    @Override
    public String getId() {
        return "official-edge-gateway";
    }

    /**
     * @return
     */
    @Override
    public String getName() {
        return "边缘网关接入";
    }

    @Override
    public String getDescription() {
        return "适用于接入官方边缘网关";
    }

    /**
     * 传输协议
     * @return
     */
    @Override
    public Transport getTransport() {
        return DefaultTransport.MQTT;
    }

    /**
     * @param properties 配置
     * @return
     */
    @Override
    public Mono<? extends DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {
        return networkManager
            .<MqttServer>getNetwork(DefaultNetworkType.MQTT_SERVER, properties.getChannelId()) //获取MqttServer网络组件
            .map(mqttServer -> new MqttServerDeviceGateway(
                properties.getId(),
                registry,
                sessionManager,
                mqttServer,
                messageHandler,
                Mono.empty() //是否可以将EdgeProtocolSupport取代Mono.empty()应用于此
            ));
    }

    @Override
    public Mono<? extends DeviceGateway> reloadDeviceGateway(DeviceGateway gateway,
                                                             DeviceGatewayProperties properties) {
        MqttServerDeviceGateway deviceGateway = ((MqttServerDeviceGateway) gateway);

        String networkId = properties.getChannelId();
        //网络组件发生了变化
        if (!Objects.equals(networkId, deviceGateway.getMqttServer().getId())) {
            return gateway
                .shutdown()
                .then(this
                    .createDeviceGateway(properties)
                    .flatMap(gate -> gate.startup().thenReturn(gate)));
        }
        return Mono.just(gateway);
    }
}
