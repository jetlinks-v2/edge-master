package org.jetlinks.pro.edge.frp.server;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.edge.core.entity.FrpDistributeInfo;
import org.jetlinks.pro.network.resource.NetworkResource;
import org.jetlinks.pro.network.resource.NetworkTransport;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * frp服务配置.
 * 对应配置项：frp.config
 * 示例如下：
 * frp:
 *   config:
 *     publicHost: 127.0.0.1
 *     bindPort: 7000
 *     resources:
 *       - 8811-8820/tcp
 *
 * @author zhangji 2023/1/11
 */
@Getter
@Setter
public class FrpServerConfig {

    private static final String HOST = "0.0.0.0";

    /**
     * 公网暴露ip
     */
    @NotBlank
    private String publicHost;

    /**
     * 服务端监听端口，用于接收frpc的连接
     */
    private int bindPort;

    /**
     * token，用于鉴权
     */
    private String token;

    /**
     * 网络资源配置，支持[起始端口-终止端口/协议]的格式（例如8811-8820/tcp）
     */
    private List<String> resources = new ArrayList<>();

    private Map<Integer, String> domainMapping;

    public void validate() {
        ValidatorUtils.tryValidate(this);
        if (StringUtils.hasText(token) && token.contains(" ")) {
            throw new ValidationException("token", LocaleUtils.resolveMessage("error.frp_token_invalid"));
        }
    }

    public List<NetworkResource> parseResources() {
        List<NetworkResource> info = new ArrayList<>();

        for (String resource : resources) {
            NetworkTransport protocol = null;
            if (resource.contains("/")) {
                protocol = NetworkTransport.valueOf(resource
                    .substring(resource.indexOf("/") + 1)
                    .toUpperCase(Locale.ROOT));
                resource = resource.substring(0, resource.indexOf("/"));
            }
            NetworkResource res = new NetworkResource();
            res.setHost(HOST);
            //未指定时则同时支持UDP和TCP
            if (protocol == null) {
                List<Integer> ports = getPorts(resource);
                res.addPorts(NetworkTransport.UDP, ports);
                res.addPorts(NetworkTransport.TCP, ports);
            } else {
                res.addPorts(protocol, getPorts(resource));
            }
            info.add(res);
        }

        return info;

    }

    private List<Integer> getPorts(String port) {
        String[] ports = port.split("-");
        if (ports.length == 1) {
            return Collections.singletonList(Integer.parseInt(ports[0]));
        }
        int startWith = Integer.parseInt(ports[0]);
        int endWith = Integer.parseInt(ports[1]);
        if (startWith > endWith) {
            int temp = startWith;
            startWith = endWith;
            endWith = temp;
        }
        List<Integer> arr = new ArrayList<>(endWith - startWith);
        for (int i = startWith; i <= endWith; i++) {
            arr.add(i);
        }
        return arr;
    }

    public FrpDistributeInfo toDistributeInfo() {
        return FastBeanCopier.copy(this, new FrpDistributeInfo());
    }

}
