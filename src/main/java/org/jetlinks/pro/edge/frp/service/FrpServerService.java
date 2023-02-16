package org.jetlinks.pro.edge.frp.service;

import io.scalecube.services.annotations.ServiceMethod;
import lombok.AllArgsConstructor;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.core.rpc.RpcManager;
import org.jetlinks.pro.edge.frp.entity.FrpServerConfigEntity;
import org.jetlinks.pro.edge.frp.entity.FrpServerConfigInfo;
import org.jetlinks.pro.edge.frp.server.FrpProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * frp服务.
 *
 * @author zhangji 2023/1/11
 */
@Component
@AllArgsConstructor
public class FrpServerService extends GenericReactiveCrudService<FrpServerConfigEntity, String> {

    private final FrpProperties properties;
    private final RpcManager    rpcManager;


    /**
     * 查询frp服务配置，优先返回数据库的配置
     *
     * @param clusterNodeId 集群节点ID
     * @return 配置信息
     */
    public Mono<FrpServerConfigInfo> getClusterFrpServerConfig(String clusterNodeId) {
        if (Objects.equals(rpcManager.currentServerId(), clusterNodeId)) {
            return doGetFrpServerConfig(clusterNodeId);
        }

        return rpcManager
            .getService(clusterNodeId, Service.class)
            .flatMap(service -> service.getFrpServerConfig(clusterNodeId));
    }

    public Mono<FrpServerConfigInfo> doGetFrpServerConfig(String nodeId) {
        return this
            .createQuery()
            .where(FrpServerConfigEntity::getClusterNodeId, nodeId)
            .fetchOne()
            .map(FrpServerConfigEntity::toConfigInfo)
            .switchIfEmpty(Mono.fromSupplier(() -> {
                FrpServerConfigInfo info = FrpServerConfigInfo.of(properties);
                info.setClusterNodeId(nodeId);
                return info;
            }));
    }

    public class ServiceImpl implements Service {
        @Override
        public Mono<FrpServerConfigInfo> getFrpServerConfig(String nodeId) {
            return doGetFrpServerConfig(nodeId);
        }
    }

    @io.scalecube.services.annotations.Service
    public interface Service {
        @ServiceMethod
        Mono<FrpServerConfigInfo> getFrpServerConfig(String nodeId);
    }

}
