package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.OrderFulfillmentDao;
import com.viescloud.llc.venzora.model.product.OrderFulfillment;
import com.viescloud.llc.venzora.service.VenzoraCustomUserAccessService;

@Service
public class OrderFulfillmentService extends VenzoraCustomUserAccessService<UUID, OrderFulfillment, OrderFulfillmentDao> {

    public OrderFulfillmentService(DatabaseCall<UUID, OrderFulfillment> databaseCall, OrderFulfillmentDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(OrderFulfillment object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(OrderFulfillment object, UUID id) {
        object.setId(id);
    }

    /**
     * Orders are owned by their buyer ({@code ownerUserId} = buyer), but they are
     * back-office data: the Manager's order queue, order-detail editor, shipments
     * and returns all need ADMIN-group users to read and modify every order, not
     * just their own. Opt into the framework's admin bypass (vies-spring-utils
     * 6.3.4+).
     */
    @Override
    protected boolean allowAdminBypassUserAccess() {
        return true;
    }

    /**
     * The bypass (and the write gate below) keys on {@code orders:manage}, so
     * section admins — shipping, finance — get the full order queue on exactly
     * their grant, not blanket ADMIN membership. SUPER_ADMIN passes via *.
     */
    @Override
    protected String adminBypassAuthority() {
        return "orders:manage";
    }

    /**
     * Orders are FINANCIAL RECORDS: buyers read their own (row-level access),
     * but every mutation through the HTTP API is admin-only — a buyer must not
     * be able to rewrite their order's status, totals, or metadata even though
     * they own the row. Server-side flows (checkout orchestrator, webhook
     * listener) write through the DAO and are unaffected.
     */
    @Override
    protected void validatingBeforePut(OrderFulfillment input, OrderFulfillment oldObject) {
        requireAdminWriter(input);
        super.validatingBeforePut(input, oldObject);
    }

    @Override
    protected void validatingBeforePatch(OrderFulfillment input, OrderFulfillment oldObject) {
        requireAdminWriter(input);
        super.validatingBeforePatch(input, oldObject);
    }

    private void requireAdminWriter(OrderFulfillment input) {
        if (!isAdminBypassUser(input.getInputUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Orders can only be modified by back-office staff (orders:manage)");
        }
    }

}
