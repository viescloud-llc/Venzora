package com.viescloud.llc.venzora.config;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.viescloud.eco.viesspringutils.auto.dao.authentication.RoleDao;
import com.viescloud.eco.viesspringutils.auto.model.authentication.Role;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Seeds Venzora's section-admin roles (permission-system.md § 6) —
 * CREATE-IF-ABSENT only, so grants an admin has tuned are never overwritten.
 * SUPER_ADMIN ({@code *}) is seeded by the library and attached to the ADMIN
 * group. Money stays on the {@code checkout} resource and permission
 * administration on {@code iam}, so no {@code <resource>:*} grant can leak
 * either by accident.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VenzoraRoleSeeder {

    private final RoleDao roleDao;

    // maintenance:bypass on every staff role: staff keep working while customers
    // are held by the maintenance gate. orders:restock lets finance put refunded
    // goods back on the shelf (inventory admins get it via inventory:*).
    private static final Map<String, Set<String>> ROLES = new LinkedHashMap<>() {{
        put("SHIPPING_ADMIN", Set.of("shipments:*", "orders:read", "orders:update", "orders:manage",
                "returns:read", "maintenance:bypass"));
        put("INVENTORY_ADMIN", Set.of("inventory:*", "catalog:read", "maintenance:bypass"));
        put("CATALOG_ADMIN", Set.of("catalog:*", "schema:*", "maintenance:bypass"));
        put("FINANCE_ADMIN", Set.of("checkout:*", "orders:*", "orders:manage", "orders:restock",
                "returns:*", "returns:manage", "discounts:*", "reports:read", "maintenance:bypass"));
    }};

    /**
     * Create-if-absent, and ADDITIVE for existing seeded roles: baseline grants
     * introduced by a later release (e.g. maintenance:bypass) are unioned in,
     * but nothing an admin granted by hand is ever removed.
     */
    @PostConstruct
    public void seed() {
        try {
            for (var entry : ROLES.entrySet()) {
                Role existing = roleDao.findByName(entry.getKey()).orElse(null);
                if (existing == null) {
                    roleDao.save(Role.builder()
                            .name(entry.getKey())
                            .description("Auto created Venzora section-admin role")
                            .permissions(new HashSet<>(entry.getValue()))
                            .build());
                    log.info("Seeded role {} with {}", entry.getKey(), entry.getValue());
                } else {
                    Set<String> merged = new HashSet<>(existing.getPermissions() == null ? Set.of() : existing.getPermissions());
                    if (merged.addAll(entry.getValue())) {
                        existing.setPermissions(merged);
                        roleDao.save(existing);
                        log.info("Added baseline grants to role {} -> {}", entry.getKey(), merged);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to seed Venzora roles: {}", e.getMessage(), e);
        }
    }
}
