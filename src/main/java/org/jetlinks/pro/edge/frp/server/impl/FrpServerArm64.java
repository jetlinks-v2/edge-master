package org.jetlinks.pro.edge.frp.server.impl;

import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * frp服务-arm64.
 *
 * @author zhangji 2023/1/10
 */
@Component
public class FrpServerArm64 extends AbstractFrpServer {

    private static final String[] VERSION = {"aarch64", "arm64"};

    private static final String TYPE = "arm64";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean matchRelease(String version) {
        return Arrays.stream(VERSION).anyMatch(v -> v.equalsIgnoreCase(version));
    }
}
