package org.jetlinks.pro.edge.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.pro.edge.entity.EdgeDeviceProductEntity;
import org.jetlinks.pro.edge.service.EdgeDeviceService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/edge/product")
@Tag(name = "边缘网关产品管理")
@Resource(id="edge-product",name = "边缘网关产品管理")
public class EdgeDeviceController implements ReactiveServiceCrudController<EdgeDeviceProductEntity, String> {

    private final EdgeDeviceService service;

    @Override
    public EdgeDeviceService getService() {
        return service;
    }
}
