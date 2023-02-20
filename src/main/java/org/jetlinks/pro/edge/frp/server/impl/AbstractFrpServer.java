package org.jetlinks.pro.edge.frp.server.impl;

import cn.hutool.core.util.RuntimeUtil;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.pro.edge.frp.server.FrpServer;
import org.jetlinks.pro.edge.frp.server.FrpServerConfig;
import org.jetlinks.pro.edge.utils.ReourceFileUtils;
import reactor.core.publisher.Mono;

import java.io.File;

/**
 * frp服务-抽象类.
 * 定义frp程序文件名称规则，通用的配置管理
 *
 * @author zhangji 2023/1/11
 */
@Generated
public abstract class AbstractFrpServer implements FrpServer {
    String FRPS_PREFIX = "frps";

    private Process process;

    @Setter
    @Getter
    protected FrpServerConfig config;

    /**
     * 规则："frps_" + 类型 + ".bin"
     * 添加.bin后缀，通过后缀过滤使maven不编译此类文件
     *
     * @return frps文件名
     */
    @Override
    public String getFrpFileName() {
        return FRPS_PREFIX + "_" + getType() + ".bin";
    }

    @Override
    @SneakyThrows
    public void start(String startShell) {
        FrpServerConfig config = getConfig();
        if (config == null) {
            throw new NotFoundException();
        }

        String command = "sh " + ReourceFileUtils.SHELL_TEMP_PATH + File.separator + startShell +
            " " + ReourceFileUtils.ABSOLUTE_APP_TEMP_PATH +
            " " + config.getBindPort() +
            " " + config.getToken() +
            " " + getFrpFileName();

        process = Runtime
            .getRuntime()
            .exec(command);
    }

    @Override
    public void stop() {
        process.destroy();
    }
}
