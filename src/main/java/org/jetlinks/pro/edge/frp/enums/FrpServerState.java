package org.jetlinks.pro.edge.frp.enums;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;

/**
 * frp服务状态.
 *
 * @author zhangji 2023/1/10
 */
@Generated
@Getter
@AllArgsConstructor
public enum FrpServerState implements I18nEnumDict<String> {
    enabled("正常"),
    disabled("禁用")
    ;

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
