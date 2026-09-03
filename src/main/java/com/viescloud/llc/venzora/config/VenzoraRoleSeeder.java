package com.viescloud.llc.venzora.config;

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

    private static final Map<String, Set<String>> ROLES = new LinkedHashMap<>() {{
        put("SHIPPING_ADMIN", Set.of("shipments:*", "orders:read", "orders:update", "orders:manage", "returns:read"));
        put("INVENTORY_ADMIN", Set.of("inventory:*", "catalog:read"));
        put("CATALOG_ADMIN", Set.of("catalog:*", "schema:*"));
        put("FINANCE_ADMIN", Set.of("checkout:*", "orders:*", "orders:manage",
                "returns:*", "returns:manage", "discounts:*", "reports:read"));
    }};

    @PostConstruct
    public void seed() {
        try {
            for (var entry : ROLES.entrySet()) {
                if (roleDao.findByName(entry.getKey()).isEmpty()) {
                    roleDao.save(Role.builder()
                            .name(entry.getKey())
                            .description("Auto created Venzora section-admin role")
                            .permissions(entry.getValue())
                            .build());
                    log.info("Seeded role {} with {}", entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e) {
            log.error("Failed to seed Venzora roles: {}", e.getMessage(), e);
        }
    }
}
