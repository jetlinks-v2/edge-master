package org.jetlinks.pro.edge.frp.server;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * frp服务配置信息.
 *
 * @author zhangji 2023/1/10
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "frp")
public class FrpProperties {

    private boolean enabled = false;

    private FrpServerConfig config;

}
