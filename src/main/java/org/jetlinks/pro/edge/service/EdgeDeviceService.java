package org.jetlinks.pro.edge.service;

import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.pro.device.service.LocalDeviceProductService;
import org.jetlinks.pro.edge.entity.EdgeDeviceProductEntity;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.function.Function;

@Service
@Deprecated
public class EdgeDeviceService extends GenericReactiveCrudService<EdgeDeviceProductEntity, String> {

    private final LocalDeviceProductService productService;

    public EdgeDeviceService(LocalDeviceProductService productService) {
        this.productService = productService;
    }

    @Override
    @Transactional
    public Mono<SaveResult> save(Publisher<EdgeDeviceProductEntity> entityPublisher) {
        return this
            .doSave(entityPublisher, super::save);
    }

    @Override
    @Transactional
    public Mono<Integer> insert(Publisher<EdgeDeviceProductEntity> entityPublisher) {
        return this
            .doSave(entityPublisher, super::insert)
            .map(SaveResult::getTotal);
    }

    @Override
    @Transactional
    public Mono<Integer> insertBatch(Publisher<? extends Collection<EdgeDeviceProductEntity>> entityPublisher) {
        return this
            .insert(Flux.from(entityPublisher).flatMapIterable(Function.identity()));
    }

    @Override
    @Transactional
    public Mono<Integer> updateById(String id, Mono<EdgeDeviceProductEntity> entityPublisher) {
        return this
            .doSave(entityPublisher, e -> updateById(id, Mono.from(e)))
            .map(SaveResult::getTotal);
    }

    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return Flux
            .from(idPublisher)
            .flatMap(id -> productService
                .cancelDeploy(id)
                .then(productService.deleteById(Mono.just(id)))
                .thenReturn(id))
            .as(super::deleteById);
    }

    private Mono<SaveResult> doSave(Publisher<EdgeDeviceProductEntity> productEntity, Function<Publisher<EdgeDeviceProductEntity>, Mono<?>> handler) {
        return Flux
            .from(productEntity)
            .flatMap(product -> productService
                // 保存/修改 dev_product
                .save(Mono.just(product.toDeviceProduct()))
                // 处理原始数据 edge_product
                .then(handler.apply(Mono.just(product)))
                //激活产品
                .then(productService.deploy(product.getId()))
                .thenReturn(1))
            .count()
            .map(total -> SaveResult.of(0, total.intValue()));
    }
}
