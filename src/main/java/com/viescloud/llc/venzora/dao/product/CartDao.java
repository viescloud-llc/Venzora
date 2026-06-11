package com.viescloud.llc.venzora.dao.product;

import java.util.UUID;

import com.viescloud.eco.viesspringutils.dao.ViesUserAccessJpaRepository;
import com.viescloud.llc.venzora.model.product.Cart;

public interface CartDao extends ViesUserAccessJpaRepository<Cart, UUID> {

}
