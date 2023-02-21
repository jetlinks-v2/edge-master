package org.jetlinks.pro.edge.frp.server;

import cn.hutool.core.util.RuntimeUtil;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.pro.edge.utils.ReourceFileUtils;

import java.io.File;

/**
 * frp服务.
 *
 * @author zhangji 2023/1/10
 */
public interface FrpServer {
    String STOP_SHELL = "systemctl stop frps";

    /**
     * @return 服务类型，对应的系统（如x86，arm64）
     */
    String getType();

    /**
     * @return frp程序文件名
     */
    String getFrpFileName();

    /**
     * 判断操作系统版本是否匹配
     *
     * @param version 操作系统版本
     * @return 是否匹配
     */
    boolean matchRelease(String version);

    /**
     * 设置配置
     *
     * @param config frp服务配置
     * @return void
     */
    void setConfig(FrpServerConfig config);

    /**
     * 获取配置
     *
     * @return frp服务配置
     */
    FrpServerConfig getConfig();

    /**
     * 启动
     * 通过脚本启动服务
     *
     * @param startShell 脚本名称
     * @return void
     */
    void start(String startShell);

    /**
     * 停止服务
     */
    void stop();

    /**
     * 初始化服务
     * 在system service中配置frp程序与配置文件名
     *
     * @param initShell 脚本
     */
    default void init(String initShell) {
        String command = "sh " + ReourceFileUtils.SHELL_TEMP_PATH +
            File.separator + initShell +
            " " + ReourceFileUtils.ABSOLUTE_APP_TEMP_PATH +
            " " + getFrpFileName();

        RuntimeUtil.execForStr(command);
    }
}
