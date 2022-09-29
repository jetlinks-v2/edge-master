package org.jetlinks.pro.edge.service.term;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.springframework.stereotype.Component;

@Component
public class EdgeDeviceTerm extends AbstractTermFragmentBuilder {
    public EdgeDeviceTerm() {
        super("edge-product", "边缘网关相关产品数据");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {
        PrepareSqlFragments sqlFragments = PrepareSqlFragments.of();

        sqlFragments.addSql("exists(select 1 from ",getTableName("edge_product",column)," prod where prod.id =", columnFullName,")");

        return sqlFragments;
    }
}
