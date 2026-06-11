package com.viescloud.llc.venzora.dao.product;

import java.util.UUID;

import com.viescloud.eco.viesspringutils.dao.ViesUserAccessJpaRepository;
import com.viescloud.llc.venzora.model.product.ReturnRequest;

public interface ReturnRequestDao extends ViesUserAccessJpaRepository<ReturnRequest, UUID> {

}
