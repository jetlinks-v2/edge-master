package org.jetlinks.pro.edge.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.pro.device.entity.DeviceProductEntity;
import org.jetlinks.pro.device.enums.DeviceType;
import org.jetlinks.pro.edge.protocol.EdgeProtocolSupport;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import java.util.Date;

@Table(name = "edge_product")
@Getter
@Setter
@Deprecated
public class EdgeDeviceProductEntity extends GenericEntity<String> {

    @Column(nullable = false)
    @Schema(description = "名称")
    @NotBlank(message = "名称不能为空")
    private String name;

    @Comment("图标地址")
    @Column(name = "photo_url", length = 1024)
    @Schema(description = "图标地址")
    private String photoUrl;

    @Column(nullable = false)
    @Schema(description = "厂家")
    @NotBlank(message = "厂家不能为空")
    private String manufacturer;

    @Column(nullable = false)
    @Schema(description = "型号")
    @NotBlank(message = "型号不能为空")
    private String model;

    @Column(nullable = false)
    @DefaultValue(EdgeProtocolSupport.VERSION)
    @Schema(description = "版本")
    private String version;

    @Comment("分类ID")
    @Column(name = "classified_id", length = 64)
    @Schema(description = "所属品类ID")
    private String classifiedId;

    @Comment("分类名称")
    @Column(name = "classified_name")
    @Schema(description = "所属品类名称")
    private String classifiedName;

    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "创建时间")
    private Date createTime;

    @Column
    @Schema(description = "说明")
    private String description;

    public DeviceProductEntity toDeviceProduct() {
        DeviceProductEntity entity = new DeviceProductEntity();
        Assert.hasText(getId(), "ID不能为空");
        entity.setId(getId());
        entity.setName(getName());
        entity.setTransportProtocol("MQTT");
        entity.setDeviceType(DeviceType.device);
        entity.setPhotoUrl(photoUrl);
        entity.setClassifiedId("edge-gateway");
        entity.setClassifiedName("边缘计算");
        if (StringUtils.hasText(version)) {
            entity.setMessageProtocol(version);
        } else {
            entity.setMessageProtocol(EdgeProtocolSupport.ID);
        }
        entity.setProtocolName(EdgeProtocolSupport.NAME);
        entity.setMetadata(EdgeProtocolSupport.METADATA);
        entity.setCreateTimeNow();
        entity.setDescribe(description);
        return entity;

    }
}
