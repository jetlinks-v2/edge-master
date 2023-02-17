package org.jetlinks.pro.edge.frp.server.impl;

import org.springframework.stereotype.Component;

/**
 * frp服务-x86.
 *
 * @author zhangji 2023/1/10
 */
@Component
public class FrpServerX86 extends AbstractFrpServer {

    private static final String VERSION = "x86_64";

    private static final String TYPE = "x86";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean matchRelease(String version) {
        return VERSION.equalsIgnoreCase(version);
    }

}
