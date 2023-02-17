package org.jetlinks.pro.edge.frp.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Range;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.utils.DigestUtils;
import org.jetlinks.pro.edge.frp.enums.FrpServerState;
import org.jetlinks.pro.edge.frp.server.FrpServerConfig;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import java.sql.JDBCType;
import java.util.HashMap;
import java.util.Map;

/**
 * frp服务配置.
 *
 * @author zhangji 2023/1/10
 */
@Getter
@Setter
@Table(name = "frp_server_config")
@Comment("frp服务配置表")
public class FrpServerConfigEntity extends GenericEntity<String> {

    @Column
    @Schema(description = "名称")
    private String name;

    @Column(length = 64, nullable = false, updatable = false)
    @NotBlank
    @Schema(description = "服务所在节点ID")
    private String clusterNodeId;

    @Column(nullable = false)
    @Range(min = 1, max = 65535)
    @Schema(description = "服务绑定端口")
    private int bindPort;

    @Column
    @JsonCodec
    @ColumnType(javaType = String.class, jdbcType = JDBCType.CLOB)
    @Schema(description = "服务配置")
    private Map<String, Object> configuration;

    @Column
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("enabled")
    @Schema(description = "状态")
    private FrpServerState state;

    @Column
    @Schema(description = "描述")
    private String description;

    @Column(nullable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "创建时间")
    private Long createTime;

    @Override
    public String getId() {
        if (super.getId() == null) {
            this.setId(DigestUtils.md5Hex(clusterNodeId));
        }

        return super.getId();
    }

    public FrpServerConfigInfo toConfigInfo() {
        FrpServerConfigInfo info = FastBeanCopier.copy(this, new FrpServerConfigInfo());
        info.setFrpServerConfig(FastBeanCopier.copy(configuration, new FrpServerConfig()));
        return info;
    }

    public static FrpServerConfigEntity of(FrpServerConfigInfo config) {
        FrpServerConfigEntity entity = FastBeanCopier.copy(config, new FrpServerConfigEntity());
        entity.setConfiguration(FastBeanCopier.copy(config.getFrpServerConfig(), new HashMap<>()));
        return entity;
    }
}
