package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.ProductVariantDao;
import com.viescloud.llc.venzora.dao.product.StockMovementDao;
import com.viescloud.llc.venzora.model.product.ProductVariant;
import com.viescloud.llc.venzora.model.product.StockMovement;
import com.viescloud.llc.venzora.model.product.type.StockMovementType;
import com.viescloud.llc.venzora.service.VenzoraService;

/**
 * A {@link StockMovement} is both an audit record and the <em>only</em> way stock
 * is meant to move outside checkout. Creating one therefore has a side effect:
 * the movement's {@code quantityChange} is applied to
 * {@link ProductVariant#getStockQuantity()} and the resulting balance is stamped
 * onto {@code quantityAfter}.
 *
 * <p>{@code stockQuantity} on the variant stays the source of truth for "how many
 * do we have"; movements are the ledger that explains how it got there. Stock is
 * not derived by summing movements — variants created with an opening balance
 * have no movement behind them.
 *
 * <p><b>Sign convention.</b> {@code quantityChange} is a signed delta, per the
 * field's own contract: positive adds, negative removes. The movement <em>type</em>
 * is descriptive metadata and does not flip the sign — a {@code SALE} is expected
 * to arrive with a negative {@code quantityChange}.
 *
 * <p><b>Scope.</b> The side effect fires on create only. Movements are an
 * append-only ledger; editing one through PUT/PATCH still updates the row without
 * re-applying a delta, so corrections should be entered as a new compensating
 * movement rather than by editing history.
 */
@Service
public class StockMovementService extends VenzoraService<UUID, StockMovement, StockMovementDao> {

    private final ProductVariantDao productVariantDao;

    public StockMovementService(DatabaseCall<UUID, StockMovement> databaseCall,
                                StockMovementDao repositoryDao,
                                ProductVariantDao productVariantDao) {
        super(databaseCall, repositoryDao);
        this.productVariantDao = productVariantDao;
    }

    @Override
    public UUID getIdFieldValue(StockMovement object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(StockMovement object, UUID id) {
        object.setId(id);
    }

    /**
     * Applies the movement to the variant's stock and computes {@code quantityAfter}.
     *
     * <p>Runs inside the framework's {@code @Transactional} {@code post()}, so a
     * failure anywhere later in the write rolls the stock change back with the
     * movement itself — the ledger and the balance cannot diverge.
     *
     * <p>{@code quantityAfter} is server-owned: whatever the client sends is
     * overwritten. Clients should not attempt to compute it.
     */
    @Override
    protected StockMovement processingPostInput(StockMovement input) {
        input = super.processingPostInput(input);

        UUID variantId = input.getProductVariant() != null ? input.getProductVariant().getId() : null;
        if (variantId == null) {
            throw badRequest("productVariant.id is required");
        }
        if (input.getQuantityChange() == null) {
            throw badRequest("quantityChange is required");
        }

        ProductVariant variant = productVariantDao.findById(variantId)
                .orElseThrow(() -> badRequest("productVariant not found: " + variantId));

        long current = variant.getStockQuantity() != null ? variant.getStockQuantity() : 0L;
        long after = current + input.getQuantityChange();
        if (after < 0) {
            throw badRequest(String.format(
                    "Insufficient stock for variant %s: current %d, change %d would leave %d",
                    variant.getSku(), current, input.getQuantityChange(), after));
        }

        variant.setStockQuantity(after);
        productVariantDao.save(variant);

        // Swap in the managed variant so the movement's FK resolves against a row
        // we know exists, and stamp the balance the client is not allowed to set.
        input.setProductVariant(variant);
        input.setQuantityAfter(after);
        return input;
    }

    /**
     * Records a checkout sale: one {@code SALE} ledger row whose creation ALSO
     * applies the (negative) delta to the variant's stock and stamps
     * {@code quantityAfter} — the same single-source-of-truth path the admin
     * Adjust panel uses, so checkout sales can never silently miss the audit
     * log again. Runs in the caller's transaction; insufficient stock surfaces
     * as the standard 400 naming the SKU.
     *
     * <p>Called by the checkout orchestrator's {@code complete()} and the
     * webhook-driven {@code CheckoutFulfillmentListener}, both guarded by the
     * shared {@code checkout.stockDecremented} metadata flag so the sale is
     * recorded exactly once.
     */
    public StockMovement recordCheckoutSale(UUID variantId, int quantity, UUID buyerUserId, String orderNumber) {
        StockMovement movement = new StockMovement();
        ProductVariant ref = new ProductVariant();
        ref.setId(variantId);
        movement.setProductVariant(ref);
        movement.setMovementType(StockMovementType.SALE);
        movement.setQuantityChange(-(long) quantity);
        movement.setReason("Checkout sale");
        movement.setReference(orderNumber);
        movement.setUserId(buyerUserId);
        return this.post(movement);
    }

    /**
     * Thrown as a plain {@link ResponseStatusException} rather than via
     * {@code HttpResponseThrowers} so the reason survives into the response body.
     * See {@code document/vies-spring-utils-fix-checklist.md}.
     */
    private static ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
