package org.jetlinks.pro.edge.frp.server;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.pro.edge.utils.ReourceFileUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认frp服务提供商.
 *
 * @author zhangji 2023/1/11
 */
@Slf4j
public class DefaultFrpServerProvider implements FrpServerProvider {

    private static final String FRPS_APP_PATH = "frps";

    private final FrpSystemHepler frpSystemHepler;

    private final Map<String, FrpServer> servers = new ConcurrentHashMap<>();

    public DefaultFrpServerProvider(FrpSystemHepler frpSystemHepler) {
        this.frpSystemHepler = frpSystemHepler;
    }


    @Override
    public void register(FrpServer server) {
        if (server == null) {
            return;
        }
        servers.put(server.getType(), server);
        if (frpSystemHepler.isLinux()) {
            ReourceFileUtils.copyAppFile(FRPS_APP_PATH, server.getFrpFileName());
        }
    }

    @Override
    public Mono<FrpServer> getMatchedServer() {
        if (frpSystemHepler.isLinux()) {
            return Mono.fromSupplier(frpSystemHepler::getSystemVersion)
                .doOnNext(version -> log.info("system version : {}", version))
                .flatMap(version -> Flux.fromIterable(servers.values())
                    .filter(server -> server.matchRelease(version))
                    .single()
                    .onErrorMap(error -> new UnsupportedOperationException(
                        LocaleUtils.resolveMessage("error.frp_system_not_supported", "system not supported for frp", version), error)));
        } else {
            return Mono.error(() -> new UnsupportedOperationException(
                LocaleUtils.resolveMessage("error.frp_system_not_supported", "system not supported for frp", "")));
        }
    }

}
