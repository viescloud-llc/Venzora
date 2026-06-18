package com.viescloud.llc.venzora.dao.product;

import java.util.Optional;
import java.util.UUID;

import com.viescloud.eco.viesspringutils.dao.ViesUserAccessJpaRepository;
import com.viescloud.llc.venzora.model.product.OrderFulfillment;

public interface OrderFulfillmentDao extends ViesUserAccessJpaRepository<OrderFulfillment, UUID> {

    Optional<OrderFulfillment> findByCheckoutOrderId(UUID checkoutOrderId);
}
