package org.jetlinks.pro.edge.configuration;

import org.jetlinks.edge.core.EdgeOperations;
import org.jetlinks.edge.core.dashboard.EdgeGatewayStateSubscriptionProvider;
import org.jetlinks.edge.core.web.EdgeOperationsController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Configuration
public class EdgeMasterConfiguration {

    @Bean
    public EdgeOperationsController edgeOperationsController(EdgeOperations operations){
        return new EdgeOperationsController(operations);
    }

    @Bean
    public EdgeGatewayStateSubscriptionProvider localDashBoardSubscriptionProvider(EdgeOperations operations){
        return new EdgeGatewayStateSubscriptionProvider(operations);
    }
}
