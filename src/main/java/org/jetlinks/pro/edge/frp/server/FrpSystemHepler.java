package org.jetlinks.pro.edge.frp.server;

import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.system.OsInfo;

/**
 * frp工具类.
 *
 * @author zhangji 2023/2/14
 */
public class FrpSystemHepler {
    private static final String[] SHELL_SYSTEM_VERSION = new String[]{
        "/bin/sh", "-c",
        "uname -a | awk -F ' ' '{print $(NF-1)}'"
    };

    private final OsInfo OS = new OsInfo();

    public String getSystemVersion() {
        return RuntimeUtil
            .execForStr(SHELL_SYSTEM_VERSION)
            .replace("\n", "")
            .trim();
    }

    public boolean isLinux() {
        return OS.isLinux();
    }
}
