package org.jetlinks.pro.edge.frp.entity;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.network.resource.NetworkResource;
import org.jetlinks.pro.network.resource.NetworkTransport;

import javax.validation.constraints.NotBlank;

/**
 * frp分发端口请求.
 *
 * @author zhangji 2023/2/15
 */
@Generated
@Setter
@Getter
@AllArgsConstructor(staticName = "of")
public class DistributeRequest {

    /**
     * 网关设备ID
     */
    @NotBlank
    private String deviceId;

    /**
     * 协议
     */
    @NotBlank
    private NetworkTransport transport;

    /**
     * 可用网络资源
     */
    private NetworkResource resource;
}
