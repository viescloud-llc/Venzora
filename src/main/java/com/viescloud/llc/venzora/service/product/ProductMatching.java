package com.viescloud.llc.venzora.service.product;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.viescloud.llc.venzora.dao.product.CategoryDao;
import com.viescloud.llc.venzora.model.product.AttributeDefinition;
import com.viescloud.llc.venzora.model.product.Category;
import com.viescloud.llc.venzora.model.product.Product;
import com.viescloud.llc.venzora.model.product.ProductVariant;
import com.viescloud.llc.venzora.model.product.Tag;

/**
 * The ONE definition of "does this product match these tag / category /
 * attribute-definition matchers", shared by tax rules and discounts:
 * empty matcher set = any product; a set matcher needs ANY overlap; categories
 * match the product's category OR any ancestor.
 */
public final class ProductMatching {

    private ProductMatching() {}

    /** Everything a product matcher can look at, resolved once per line item. */
    public record ProductContext(Set<UUID> categoryIds, Set<UUID> tagIds, Set<UUID> attributeDefinitionIds) {
        public static final ProductContext EMPTY = new ProductContext(Set.of(), Set.of(), Set.of());
    }

    public static Map<UUID, Category> categoriesById(CategoryDao categoryDao) {
        return categoryDao.findAll().stream()
                .filter(c -> c.getId() != null)
                .collect(Collectors.toMap(Category::getId, c -> c, (a, b) -> a));
    }

    /** Category chain (incl. ancestors), tag ids, and attribute-definition ids (product-level + the variant's). */
    public static ProductContext contextFor(ProductVariant variant, Map<UUID, Category> categoriesById) {
        Product product = variant == null ? null : variant.getProduct();
        if (product == null) return ProductContext.EMPTY;

        Set<UUID> categoryIds = categoryChain(product.getCategory(), categoriesById);
        Set<UUID> tagIds = product.getTags() == null ? Set.of()
                : product.getTags().stream().map(Tag::getId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> defIds = new HashSet<>();
        if (product.getAttributes() != null) {
            product.getAttributes().forEach(a -> {
                if (a.getAttributeDefinition() != null && a.getAttributeDefinition().getId() != null) defIds.add(a.getAttributeDefinition().getId());
            });
        }
        if (variant.getAttributeValues() != null) {
            variant.getAttributeValues().forEach(a -> {
                if (a.getAttributeDefinition() != null && a.getAttributeDefinition().getId() != null) defIds.add(a.getAttributeDefinition().getId());
            });
        }
        return new ProductContext(categoryIds, tagIds, defIds);
    }

    /** The category plus every ancestor (cycle-safe). */
    public static Set<UUID> categoryChain(Category category, Map<UUID, Category> categoriesById) {
        Set<UUID> chain = new HashSet<>();
        Category current = category;
        while (current != null && current.getId() != null && chain.add(current.getId())) {
            UUID parentId = current.getParentCategoryId();
            current = parentId == null || categoriesById == null ? null : categoriesById.get(parentId);
        }
        return chain;
    }

    public static boolean hasMatchers(Set<Tag> tags, Set<Category> categories, Set<AttributeDefinition> definitions) {
        return (tags != null && !tags.isEmpty()) || (categories != null && !categories.isEmpty())
                || (definitions != null && !definitions.isEmpty());
    }

    public static boolean matches(Set<Tag> tags, Set<Category> categories, Set<AttributeDefinition> definitions, ProductContext ctx) {
        ProductContext c = ctx == null ? ProductContext.EMPTY : ctx;
        if (tags != null && !tags.isEmpty()
                && tags.stream().noneMatch(t -> t.getId() != null && c.tagIds().contains(t.getId()))) return false;
        if (categories != null && !categories.isEmpty()
                && categories.stream().noneMatch(k -> k.getId() != null && c.categoryIds().contains(k.getId()))) return false;
        if (definitions != null && !definitions.isEmpty()
                && definitions.stream().noneMatch(d -> d.getId() != null && c.attributeDefinitionIds().contains(d.getId()))) return false;
        return true;
    }
}
