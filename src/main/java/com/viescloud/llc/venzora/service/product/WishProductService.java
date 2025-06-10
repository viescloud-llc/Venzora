package com.viescloud.llc.venzora.service.product;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.WishListDao;
import com.viescloud.llc.venzora.model.product.WishProduct;
import com.viescloud.llc.venzora.service.VenzoraCustomUserAccessService;

@Service
public class WishProductService extends VenzoraCustomUserAccessService<Long, WishProduct, WishListDao> {

    public WishProductService(DatabaseCall<Long, WishProduct> databaseCall, WishListDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public Long getIdFieldValue(WishProduct object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(WishProduct object, Long id) {
        object.setId(id);
    }
    
}
