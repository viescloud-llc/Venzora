package com.viescloud.llc.venzora.service.product;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.CartDao;
import com.viescloud.llc.venzora.model.product.Cart;
import com.viescloud.llc.venzora.service.VenzoraCustomUserAccessService;

@Service
public class CartService extends VenzoraCustomUserAccessService<UUID, Cart, CartDao> {

    public CartService(DatabaseCall<UUID, Cart> databaseCall, CartDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(Cart object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(Cart object, UUID id) {
        object.setId(id);
    }

    /**
     * Fills in the server-owned fields a client is not expected to send, so the
     * create-empty-then-add-items pattern works with a bare {@code {}} body.
     *
     * <p>{@code Cart.userId} duplicates the {@code ownerUserId} the user-access
     * controller already stamps from the {@code user_id} header, but it is
     * {@code @Column(nullable = false)} — so a cart posted without it used to be
     * rejected by the framework's not-null check. Defaulting it to the owner
     * keeps the two in step instead of asking the client to send the same UUID
     * twice.
     *
     * <p>An empty {@code items} list is valid; items are added afterwards through
     * {@code /api/v1/cart/items}.
     */
    @Override
    protected Cart processingPostInput(Cart input) {
        input = super.processingPostInput(input);

        if (input.getUserId() == null) {
            UUID owner = input.getOwnerUserId() != null ? input.getOwnerUserId() : input.getInputUserId();
            if (owner == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "userId is required — send it in the body or supply the user_id header");
            }
            input.setUserId(owner);
        }
        if (input.getTotalPrice() == null) {
            input.setTotalPrice(BigDecimal.ZERO);
        }
        if (input.getActive() == null) {
            input.setActive(true);
        }
        return input;
    }
}
