package com.viescloud.llc.venzora.model.product.type;

public enum StockMovementType {
    PURCHASE,          // Stock added from supplier
    SALE,              // Stock sold to customer
    ADJUSTMENT,        // Manual adjustment (inventory count correction)
    RETURN,            // Customer return adding stock back
    DAMAGE,            // Damaged/spoiled inventory removed
    TRANSFER,          // Transfer between warehouses
    RESERVED,          // Stock reserved for pending order
    UNRESERVED         // Reserved stock released back
}
