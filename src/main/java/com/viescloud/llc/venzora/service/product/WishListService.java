package com.viescloud.llc.venzora.service.product;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.WishListDao;
import com.viescloud.llc.venzora.model.product.WishList;
import com.viescloud.llc.venzora.service.VenzoraService;

public class WishListService extends VenzoraService<Long, WishList, WishListDao> {

    public WishListService(DatabaseCall<Long, WishList> databaseCall, WishListDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public Long getIdFieldValue(WishList object) {
        return object.getUserId();
    }

    @Override
    public void setIdFieldValue(WishList object, Long id) {
        object.setUserId(id);
    }
    
}
