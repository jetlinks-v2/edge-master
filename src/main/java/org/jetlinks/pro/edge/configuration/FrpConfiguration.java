package org.jetlinks.pro.edge.configuration;

import org.jetlinks.core.rpc.RpcManager;
import org.jetlinks.pro.edge.frp.server.DefaultFrpServerManager;
import org.jetlinks.pro.edge.frp.server.DefaultFrpServerProvider;
import org.jetlinks.pro.edge.frp.server.FrpProperties;
import org.jetlinks.pro.edge.frp.server.FrpServer;
import org.jetlinks.pro.edge.frp.server.FrpServerManager;
import org.jetlinks.pro.edge.frp.server.FrpServerProvider;
import org.jetlinks.pro.edge.frp.server.FrpSystemHepler;
import org.jetlinks.pro.edge.frp.service.FrpServerService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * frp bean配置.
 *
 * @author zhangji 2023/1/10
 */
@Configuration
@EnableConfigurationProperties(FrpProperties.class)
public class FrpConfiguration {

    @Bean
    @ConditionalOnMissingBean(FrpServerProvider.class)
    @ConditionalOnProperty(prefix = "frp", name = "enabled", havingValue = "true")
    FrpServerProvider edgeNetworkConfigProvider(ObjectProvider<FrpServer> executor) {
        DefaultFrpServerProvider provider = new DefaultFrpServerProvider(new FrpSystemHepler());
        executor.forEach(provider::register);
        return provider;
    }

    @Bean(destroyMethod = "stop")
    FrpServerManager frpServerManager(FrpServerService frpServerService,
                                      ObjectProvider<FrpServerProvider> provider,
                                      RpcManager rpcManager) {
        return new DefaultFrpServerManager(frpServerService, provider.getIfAvailable(), rpcManager);
    }

}
