package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.Tag;
import com.viescloud.llc.venzora.service.product.TagService;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController extends ViesAutoAdminCheckController<UUID, Tag, TagService> {

    public TagController(TagService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

}
