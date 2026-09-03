package com.viescloud.llc.venzora.service.product;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.viescloud.llc.venzora.dao.product.AttributeDefinitionDao;
import com.viescloud.llc.venzora.dao.product.ProductDao;
import com.viescloud.llc.venzora.dao.product.ProductVariantDao;
import com.viescloud.llc.venzora.model.product.AttributeDefinition;
import com.viescloud.llc.venzora.model.product.AttributeOption;
import com.viescloud.llc.venzora.model.product.AttributeValue;
import com.viescloud.llc.venzora.model.product.GenerateVariantsRequest;
import com.viescloud.llc.venzora.model.product.GenerateVariantsResponse;
import com.viescloud.llc.venzora.model.product.Product;
import com.viescloud.llc.venzora.model.product.ProductVariant;
import com.viescloud.llc.venzora.model.product.ProductVariantAttribute;
import com.viescloud.llc.venzora.model.product.type.ProductAttributeType;
import com.viescloud.llc.venzora.model.product.type.ProductVariantStatus;
import com.viescloud.llc.venzora.model.product.type.VariantPriceMode;

import lombok.RequiredArgsConstructor;

/**
 * Server-side cartesian variant generator (intent § 11 gap — replaces the old
 * client-side loop in the product editor). Given a set of SELECT-definition
 * axes, creates one {@link ProductVariant} per combination:
 *
 * <ul>
 *   <li>SKU {@code {baseSku}-{OPT1}-{OPT2}} (option values sanitized to
 *       uppercase alphanumerics; a missing baseSku falls back to a product-id
 *       prefix)</li>
 *   <li>variantName {@code "{product.name} OPT1 / OPT2"}</li>
 *   <li>{@code priceMode NORMAL} with a null price (effectivePrice resolves to
 *       the product's basePrice), stock 0, status ACTIVE</li>
 *   <li>one {@link ProductVariantAttribute} per axis with
 *       {@code attributeValue.selectValue} = the option</li>
 * </ul>
 *
 * <p>Idempotent: combinations whose derived SKU already exists on this product
 * — or anywhere else, since SKU is globally unique — are skipped and counted,
 * so re-running after adding options only creates the new combinations.
 * Everything is saved through one cascading product write, mirroring how the
 * editor saves variants.
 */
@Service
@RequiredArgsConstructor
public class ProductVariantGeneratorService {

    /** Hard cap on combinations per run — a fat-fingered axis set shouldn't mint thousands of rows. */
    static final int MAX_COMBINATIONS = 500;

    private final ProductDao productDao;
    private final ProductVariantDao variantDao;
    private final AttributeDefinitionDao attributeDefinitionDao;

    @Transactional
    public GenerateVariantsResponse generate(UUID productId, GenerateVariantsRequest request) {
        Product product = productDao.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productId));

        if (request == null || request.getAxes() == null || request.getAxes().isEmpty()) {
            throw bad("axes is required — at least one attribute definition to combine");
        }

        // Resolve each axis to its definition + concrete option list, in request order.
        Map<AttributeDefinition, List<AttributeOption>> axes = new LinkedHashMap<>();
        for (GenerateVariantsRequest.Axis axis : request.getAxes()) {
            if (axis == null || axis.getAttributeDefinitionId() == null) {
                throw bad("every axis needs an attributeDefinitionId");
            }
            AttributeDefinition def = attributeDefinitionDao.findById(axis.getAttributeDefinitionId())
                    .orElseThrow(() -> bad("attribute definition not found: " + axis.getAttributeDefinitionId()));
            if (def.getType() != ProductAttributeType.SELECT && def.getType() != ProductAttributeType.MULTI_SELECT) {
                throw bad("definition '" + def.getName() + "' is " + def.getType()
                        + " — only SELECT/MULTI_SELECT definitions can be generator axes");
            }
            if (axes.containsKey(def)) {
                throw bad("definition '" + def.getName() + "' appears more than once");
            }

            List<AttributeOption> options = def.getOptions() == null ? List.of() : def.getOptions();
            if (axis.getOptionIds() != null && !axis.getOptionIds().isEmpty()) {
                Set<UUID> wanted = new HashSet<>(axis.getOptionIds());
                options = options.stream().filter(o -> wanted.contains(o.getId())).collect(Collectors.toList());
                if (options.size() != wanted.size()) {
                    throw bad("some optionIds do not belong to definition '" + def.getName() + "'");
                }
            }
            if (options.isEmpty()) {
                throw bad("definition '" + def.getName() + "' has no options to combine");
            }
            axes.put(def, options);
        }

        long comboCount = axes.values().stream().mapToLong(List::size).reduce(1L, (a, b) -> a * b);
        if (comboCount > MAX_COMBINATIONS) {
            throw bad("would generate " + comboCount + " variants — cap is " + MAX_COMBINATIONS
                    + "; narrow the option lists");
        }

        // Cartesian product, depth-first so combination order is stable.
        List<List<AttributeOption>> combos = new ArrayList<>();
        cartesian(new ArrayList<>(axes.values()), 0, new ArrayList<>(), combos);

        if (product.getVariants() == null) {
            product.setVariants(new HashSet<>());
        }
        Set<String> localSkus = product.getVariants().stream()
                .map(ProductVariant::getSku)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

        String skuBase = (product.getBaseSku() == null || product.getBaseSku().isBlank())
                ? "P" + productId.toString().substring(0, 8).toUpperCase()
                : product.getBaseSku();

        List<AttributeDefinition> defOrder = new ArrayList<>(axes.keySet());
        int created = 0;
        int skipped = 0;

        for (List<AttributeOption> combo : combos) {
            String sku = skuBase + combo.stream()
                    .map(o -> "-" + sanitize(o.getValue()))
                    .collect(Collectors.joining());

            if (localSkus.contains(sku) || variantDao.existsBySku(sku)) {
                skipped++;
                continue;
            }

            ProductVariant variant = new ProductVariant();
            // Explicit back-reference: Product.syncChildBackRefs is @PrePersist/
            // @PreUpdate on Product, but adding to an inverse mappedBy collection
            // doesn't dirty the parent, so @PreUpdate may never fire — Hibernate
            // then rejects the cascade insert ("not-null property references a
            // null or transient value: ProductVariant.product").
            variant.setProduct(product);
            variant.setSku(sku);
            variant.setVariantName(buildName(product, combo));
            variant.setPriceMode(VariantPriceMode.NORMAL);
            variant.setStockQuantity(0L);
            variant.setStatus(ProductVariantStatus.ACTIVE);

            List<ProductVariantAttribute> attrs = new ArrayList<>();
            for (int i = 0; i < combo.size(); i++) {
                ProductVariantAttribute pva = new ProductVariantAttribute();
                pva.setAttributeDefinition(defOrder.get(i));
                AttributeValue value = new AttributeValue();
                value.setSelectValue(combo.get(i));
                pva.setAttributeValue(value);
                attrs.add(pva);
            }
            variant.setAttributeValues(attrs);

            product.getVariants().add(variant);
            localSkus.add(sku);
            created++;
        }

        // One cascading write — @PrePersist/@PreUpdate back-ref syncing on
        // Product/ProductVariant wires product & variant references, same as an
        // editor save.
        Product saved = created > 0 ? productDao.save(product) : product;
        return new GenerateVariantsResponse(created, skipped, saved);
    }

    private static void cartesian(List<List<AttributeOption>> axes, int depth,
                                  List<AttributeOption> current, List<List<AttributeOption>> out) {
        if (depth == axes.size()) {
            out.add(new ArrayList<>(current));
            return;
        }
        for (AttributeOption option : axes.get(depth)) {
            current.add(option);
            cartesian(axes, depth + 1, current, out);
            current.remove(current.size() - 1);
        }
    }

    /** Uppercase alphanumerics only, e.g. "Ocean Blue" -> "OCEANBLUE"; empty falls back to "OPT". */
    private static String sanitize(String value) {
        String cleaned = value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return cleaned.isEmpty() ? "OPT" : cleaned;
    }

    private static String buildName(Product product, List<AttributeOption> combo) {
        String opts = combo.stream()
                .map(o -> (o.getDisplayValue() == null || o.getDisplayValue().isBlank()) ? o.getValue() : o.getDisplayValue())
                .collect(Collectors.joining(" / "));
        return (product.getName() == null ? "" : product.getName() + " ") + opts;
    }

    private static ResponseStatusException bad(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
