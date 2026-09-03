package com.viescloud.llc.venzora.service.product;

import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.OrderFulfillmentDao;
import com.viescloud.llc.venzora.dao.product.ReturnRequestDao;
import com.viescloud.llc.venzora.model.product.OrderFulfillment;
import com.viescloud.llc.venzora.model.product.ReturnRequest;
import com.viescloud.llc.venzora.model.product.type.ReturnStatus;
import com.viescloud.llc.venzora.service.VenzoraCustomUserAccessService;

@Service
public class ReturnRequestService extends VenzoraCustomUserAccessService<UUID, ReturnRequest, ReturnRequestDao> {

    private final OrderFulfillmentDao orderFulfillmentDao;

    public ReturnRequestService(DatabaseCall<UUID, ReturnRequest> databaseCall, ReturnRequestDao repositoryDao,
                                OrderFulfillmentDao orderFulfillmentDao) {
        super(databaseCall, repositoryDao);
        this.orderFulfillmentDao = orderFulfillmentDao;
    }

    @Override
    public UUID getIdFieldValue(ReturnRequest object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(ReturnRequest object, UUID id) {
        object.setId(id);
    }

    /**
     * Return requests are created by buyers (owner = buyer) but processed by the
     * back office -- admins must see and update every return, same as orders.
     * Opt into the framework's admin bypass (vies-spring-utils 6.3.4+).
     */
    @Override
    protected boolean allowAdminBypassUserAccess() {
        return true;
    }

    /** Same pattern as orders: back-office access keys on {@code returns:manage}. */
    @Override
    protected String adminBypassAuthority() {
        return "returns:manage";
    }

    /**
     * Stamp the server-owned fields on create (contract § 7.8: returnNumber is
     * server-generated, userId comes from the caller). Before this, the
     * framework's not-null validation demanded them from the client, so the
     * Manager's create flow -- which correctly renders both as disabled --
     * dead-ended on a 400.
     *
     * <ul>
     *   <li>{@code returnNumber} -- always regenerated ({@code RET-<8 hex>},
     *       same shape as order numbers); a client-sent value is ignored.</li>
     *   <li>{@code userId} -- defaulted from the row-access owner the
     *       user-scoped controller already stamped from the JWT/header.</li>
     *   <li>{@code status} -- defaults to {@code REQUESTED}.</li>
     * </ul>
     */
    @Override
    protected ReturnRequest processingPostInput(ReturnRequest input) {
        input = super.processingPostInput(input);

        validateOrderOwnership(input);

        input.setReturnNumber("RET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        if (input.getUserId() == null) {
            UUID owner = input.getOwnerUserId() != null ? input.getOwnerUserId() : input.getInputUserId();
            input.setUserId(owner);
        }
        if (input.getStatus() == null) {
            input.setStatus(ReturnStatus.REQUESTED);
        }
        if (input.getRefundShipping() == null) {
            input.setRefundShipping(false);
        }
        return input;
    }

    /**
     * A buyer may only file a return against THEIR OWN order, and the picked
     * line item must belong to that order. Admins may file on behalf of any
     * buyer (the Manager flow). Without this, any authenticated user who knew
     * an order's UUIDs could open an RMA against someone else's purchase.
     */
    private void validateOrderOwnership(ReturnRequest input) {
        UUID caller = input.getInputUserId();
        UUID orderId = input.getOrderFulfillment() != null ? input.getOrderFulfillment().getId() : null;
        if (orderId == null) {
            return; // presence is enforced by the required-relation validation
        }
        OrderFulfillment order = orderFulfillmentDao.findById(orderId).orElse(null);
        if (order == null) {
            return; // existence is enforced by the required-relation validation
        }
        if (!isAdminBypassUser(caller) && !Objects.equals(order.getUserId(), caller)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Returns can only be filed against your own orders");
        }
        UUID itemId = input.getOrderFulfillmentItem() != null ? input.getOrderFulfillmentItem().getId() : null;
        if (itemId != null && order.getItems() != null
                && order.getItems().stream().noneMatch(i -> Objects.equals(i.getId(), itemId))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "orderFulfillmentItem does not belong to the linked order");
        }
    }

    /**
     * Buyers may amend or cancel their own return ONLY while it is still
     * {@code REQUESTED} (and only to REQUESTED/CANCELLED) — every later stage,
     * and especially anything that leads to money moving, is back-office work.
     * Admins are unrestricted.
     */
    private void requireAllowedWriter(ReturnRequest input, ReturnRequest oldObject) {
        UUID caller = input.getInputUserId();
        if (isAdminBypassUser(caller)) {
            return;
        }
        if (oldObject.getStatus() != ReturnStatus.REQUESTED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only an admin can modify a return after it has been reviewed");
        }
        if (input.getStatus() != null
                && input.getStatus() != ReturnStatus.REQUESTED
                && input.getStatus() != ReturnStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only an admin can move a return to " + input.getStatus());
        }
    }

    @Override
    protected void validatingBeforePut(ReturnRequest input, ReturnRequest oldObject) {
        requireAllowedWriter(input, oldObject);
        // A buyer PUT must not overwrite the server-owned identity fields.
        if (!isAdminBypassUser(input.getInputUserId())) {
            input.setReturnNumber(oldObject.getReturnNumber());
            input.setUserId(oldObject.getUserId());
        }
        super.validatingBeforePut(input, oldObject);
    }

    @Override
    protected void validatingBeforePatch(ReturnRequest input, ReturnRequest oldObject) {
        requireAllowedWriter(input, oldObject);
        super.validatingBeforePatch(input, oldObject);
    }
}
