package org.jetlinks.pro.edge.frp.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.edge.frp.enums.FrpServerState;
import org.jetlinks.pro.edge.frp.server.FrpProperties;
import org.jetlinks.pro.edge.frp.server.FrpServerConfig;

/**
 * frp服务配置.
 *
 * @author zhangji 2023/1/10
 */
@Getter
@Setter
public class FrpServerConfigInfo {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "服务所在节点ID")
    private String clusterNodeId;

    @Schema(description = "服务配置")
    private FrpServerConfig frpServerConfig;

    @Schema(description = "状态")
    private FrpServerState state;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "创建时间")
    private Long createTime;

    public static FrpServerConfigInfo of(FrpProperties properties) {
        FrpServerConfigInfo info = new FrpServerConfigInfo();
        FrpServerConfig config = properties.getConfig();
        info.setFrpServerConfig(config);
        info.setState(properties.isEnabled() ? FrpServerState.enabled : FrpServerState.disabled);
        return info;
    }
}
