package org.jetlinks.pro.edge.frp.server.impl;

import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.edge.frp.server.FrpServer;
import org.jetlinks.pro.edge.frp.server.FrpServerConfig;
import reactor.core.publisher.Mono;

/**
 * frp服务-抽象类.
 * 定义frp程序文件名称规则，通用的配置管理
 *
 * @author zhangji 2023/1/11
 */
@Generated
public abstract class AbstractFrpServer implements FrpServer {
    String FRPS_PREFIX = "frps";

    @Setter
    @Getter
    protected FrpServerConfig config;

    /**
     * 规则："frps_" + 类型 + ".bin"
     * 添加.bin后缀，通过后缀过滤使maven不编译此类文件
     * @return frps文件名
     */
    @Override
    public String getFrpFileName() {
        return FRPS_PREFIX + "_" + getType() + ".bin";
    }
}
