package org.jetlinks.pro.edge.protocol;

import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.defaults.CompositeProtocolSupport;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.DeviceConfigScope;
import org.jetlinks.core.metadata.types.PasswordType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.protocol.official.JetLinksAuthenticator;
import org.jetlinks.protocol.official.JetLinksMqttDeviceMessageCodec;
import org.jetlinks.supports.official.JetLinksDeviceMetadata;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class EdgeProtocolSupport extends CompositeProtocolSupport {


    public static final String ID = System.getProperty("jetlinks.system.protocol.edge.id", "jetlinks-edge-v1");
    public static final String NAME = System.getProperty("jetlinks.system.protocol.edge.name", "JetLinks融合网关1.0");

    public static final String VERSION = "v1";

    public static String METADATA;

    static {

        try (InputStream stream = new ClassPathResource("edge-device-metadata.json").getInputStream()) {
            METADATA = StreamUtils.copyToString(stream, StandardCharsets.UTF_8);
        } catch (Throwable e) {
            EdgeProtocolSupport.log.warn("load metadata failed", e);
        }
    }

    public static String getDefaultMetadata(){
        return METADATA;
    }

    @SneakyThrows
    public EdgeProtocolSupport() {
        setId(ID);
        setName(NAME);
        setMetadataCodec(new JetLinksDeviceMetadataCodec());
        //使用官方协议解析请求 https://github.com/jetlinks/jetlinks-official-protocol
        addMessageCodecSupport(new JetLinksMqttDeviceMessageCodec());
        //认证
        addAuthenticator(DefaultTransport.MQTT, new JetLinksAuthenticator());
        addConfigMetadata(DefaultTransport.MQTT, new DefaultConfigMetadata("认证配置", "")
            .add("secureId", "secureId", StringType.GLOBAL, DeviceConfigScope.device)
            .add("secureKey", "secureKey", PasswordType.GLOBAL, DeviceConfigScope.device)
        );
        //设置默认物模型
        JetLinksDeviceMetadata metadata = new JetLinksDeviceMetadata(JSON.parseObject(METADATA));
        addDefaultMetadata(DefaultTransport.MQTT, metadata);
    }

}
