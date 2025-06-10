package com.viescloud.llc.venzora.dao.product;

import com.viescloud.eco.viesspringutils.dao.ViesUserAccessJpaRepository;
import com.viescloud.llc.venzora.model.product.WishProduct;

public interface WishListDao extends ViesUserAccessJpaRepository<WishProduct, Long> {
    
}
