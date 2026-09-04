package com.viescloud.llc.venzora.service.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.viescloud.llc.venzora.dao.product.CategoryDao;
import com.viescloud.llc.venzora.dao.product.TaxRuleDao;
import com.viescloud.llc.venzora.model.address.Address;
import com.viescloud.llc.venzora.model.checkout.TaxCalculation;
import com.viescloud.llc.venzora.model.product.Cart;
import com.viescloud.llc.venzora.model.product.CartItem;
import com.viescloud.llc.venzora.model.product.Category;
import com.viescloud.llc.venzora.model.product.Product;
import com.viescloud.llc.venzora.model.product.ProductVariant;
import com.viescloud.llc.venzora.model.product.Tag;
import com.viescloud.llc.venzora.model.product.TaxRule;

/** Alias matching, district, product matchers (incl. category ancestors), per-line mixed rules, discount proration. */
class TaxCalculatorTest {

    private TaxRuleDao taxRuleDao;
    private CategoryDao categoryDao;
    private TaxCalculator calculator;

    private final Category apparel = category("Apparel", null);
    private final Category tshirts = category("T-Shirts", apparel);
    private final Category food = category("Food", null);
    private final Tag luxury = tag("luxury");

    @BeforeEach
    void setup() {
        taxRuleDao = mock(TaxRuleDao.class);
        categoryDao = mock(CategoryDao.class);
        when(categoryDao.findAll()).thenReturn(List.of(apparel, tshirts, food));
        calculator = new TaxCalculator(taxRuleDao, categoryDao);
    }

    // ---- TaxRule matching --------------------------------------------------

    @Test
    void countryAliasMatchesAndAliasesDoNotAddSpecificity() {
        TaxRule us = rule("US 8%", "8.00", "US", null);
        us.setCountryAliases(List.of("United States", "USA"));
        assertTrue(us.matchesLocation("united states", null, null, null, null));
        assertTrue(us.matchesLocation("USA", null, null, null, null));
        assertTrue(us.matchesLocation("us", null, null, null, null));
        assertFalse(us.matchesLocation("Canada", null, null, null, null));
        assertEquals(1, us.specificity());
    }

    @Test
    void stateAliasAndDistrictMatchers() {
        TaxRule ny = rule("NYC district", "4.50", "US", "NY");
        ny.setStateAliases(List.of("New York"));
        ny.setDistrict("Manhattan");
        assertTrue(ny.matchesLocation("US", "new york", null, null, "manhattan"));
        assertFalse(ny.matchesLocation("US", "NY", null, null, "Brooklyn"));
        assertFalse(ny.matchesLocation("US", "NY", null, null, null));
        assertEquals(3, ny.specificity());
    }

    @Test
    void productMatchersRequireAnyOverlapAndCountOnce() {
        TaxRule r = rule("luxury apparel", "20.00", null, null);
        r.setTags(Set.of(luxury));
        r.setCategories(Set.of(apparel));
        assertEquals(2, r.specificity());
        assertTrue(r.hasProductMatchers());
        assertTrue(r.matchesProduct(Set.of(tshirts.getId(), apparel.getId()), Set.of(luxury.getId()), Set.of()));
        assertFalse(r.matchesProduct(Set.of(food.getId()), Set.of(luxury.getId()), Set.of()));      // wrong category
        assertFalse(r.matchesProduct(Set.of(apparel.getId()), Set.of(), Set.of()));                 // no tag
    }

    // ---- Calculator ---------------------------------------------------------

    @Test
    void perLineRulesWithCategoryAncestorsAndMixedResult() {
        TaxRule general = rule("US general 8%", "8.00", "US", null);
        TaxRule foodRule = rule("US food 2%", "2.00", "US", null);
        foodRule.setCategories(Set.of(food));
        TaxRule apparelRule = rule("US apparel 10%", "10.00", "US", null);
        apparelRule.setCategories(Set.of(apparel)); // T-Shirts must match through its ancestor
        when(taxRuleDao.findAllByActiveTrue()).thenReturn(List.of(general, foodRule, apparelRule));

        Cart cart = cart(
                item("SHIRT", tshirts, "50.00", 1),   // → apparel 10% → 5.00
                item("BREAD", food, "10.00", 2),      // → food 2% on 20.00 → 0.40
                item("MISC", null, "30.00", 1));      // → general 8% → 2.40
        TaxCalculation calc = calculator.calculateForCart(cart, BigDecimal.ZERO, address("US", "NY"));

        assertEquals(new BigDecimal("7.80"), calc.getTax());
        assertTrue(calc.isMixed());
        assertNull(calc.getAppliedRuleId());
        assertTrue(calc.getAppliedRuleName().startsWith("mixed:"));
        assertEquals(3, calc.getLines().size());
        assertEquals("US apparel 10%", calc.getLines().get(0).getRuleName());
        assertEquals("US food 2%", calc.getLines().get(1).getRuleName());
        assertEquals("US general 8%", calc.getLines().get(2).getRuleName());
        // effective rate = 7.80 / 100.00 * 100
        assertEquals(new BigDecimal("7.80"), calc.getRate());
    }

    @Test
    void singleRuleAcrossAllLinesKeepsRuleIdentity() {
        TaxRule general = rule("US general 8%", "8.00", "US", null);
        when(taxRuleDao.findAllByActiveTrue()).thenReturn(List.of(general));
        Cart cart = cart(item("A", null, "10.00", 1), item("B", null, "20.00", 1));
        TaxCalculation calc = calculator.calculateForCart(cart, BigDecimal.ZERO, address("USA", null));
        assertFalse(calc.isMixed());
        assertNull(calc.getAppliedRuleId()); // alias "USA" without alias list → no match → zero
        assertEquals(new BigDecimal("0.00"), calc.getTax().setScale(2));

        general.setCountryAliases(List.of("USA"));
        calc = calculator.calculateForCart(cart, BigDecimal.ZERO, address("USA", null));
        assertEquals(general.getId(), calc.getAppliedRuleId());
        assertEquals(new BigDecimal("2.40"), calc.getTax());
    }

    @Test
    void discountIsProratedAndSumsExactly() {
        TaxRule general = rule("10%", "10.00", null, null);
        when(taxRuleDao.findAllByActiveTrue()).thenReturn(List.of(general));
        // 3 lines of 10.00, discount 1.00 → shares 0.33, 0.33, 0.34 (remainder on last)
        Cart cart = cart(item("A", null, "10.00", 1), item("B", null, "10.00", 1), item("C", null, "10.00", 1));
        TaxCalculation calc = calculator.calculateForCart(cart, new BigDecimal("1.00"), address("US", null));
        BigDecimal taxableSum = calc.getLines().stream().map(TaxCalculation.TaxLine::getTaxable).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("29.00"), taxableSum);
        assertEquals(new BigDecimal("9.66"), calc.getLines().get(2).getTaxable());
        // Tax is rounded PER LINE (documented): 9.67→0.967→0.97, 9.67→0.97, 9.66→0.966→0.97 → 2.91
        assertEquals(new BigDecimal("2.91"), calc.getTax());
    }

    @Test
    void noMatchingRuleIsZeroAndProductRulesAreSkippedForAmountOnlyPath() {
        TaxRule foodRule = rule("food", "2.00", null, null);
        foodRule.setCategories(Set.of(food));
        when(taxRuleDao.findAllByActiveTrue()).thenReturn(List.of(foodRule));
        assertEquals(BigDecimal.ZERO, calculator.calculate(new BigDecimal("100.00"), address("US", null)).getTax());
        TaxCalculation calc = calculator.calculateForCart(cart(item("MISC", null, "30.00", 1)), BigDecimal.ZERO, address("US", null));
        assertEquals(new BigDecimal("0.00"), calc.getTax().setScale(2));
        assertNull(calc.getLines().get(0).getRuleId());
    }

    @Test
    void scopedDiscountIsProratedOnlyOverEligibleLines() {
        TaxRule general = rule("10%", "10.00", null, null);
        when(taxRuleDao.findAllByActiveTrue()).thenReturn(List.of(general));
        CartItem shirt = item("SHIRT", tshirts, "50.00", 1);
        CartItem bread = item("BREAD", food, "10.00", 1);
        Cart cart = cart(shirt, bread);
        // 5.00 off, scoped to the shirt only → bread's taxable stays 10.00
        TaxCalculation calc = calculator.calculateForCart(cart, new BigDecimal("5.00"), address("US", null),
                Set.of(shirt.getProductVariant().getId()));
        assertEquals(new BigDecimal("45.00"), calc.getLines().get(0).getTaxable());
        assertEquals(new BigDecimal("10.00"), calc.getLines().get(1).getTaxable());
        assertEquals(new BigDecimal("5.50"), calc.getTax());
    }

    @Test
    void discountProductMatchersUseTheSharedDefinition() {
        com.viescloud.llc.venzora.model.product.Discount d = new com.viescloud.llc.venzora.model.product.Discount();
        d.setId(UUID.randomUUID());
        assertFalse(d.hasProductMatchers());
        d.setCategories(new java.util.HashSet<>(Set.of(apparel)));
        assertTrue(d.hasProductMatchers());
        var byId = java.util.Map.of(apparel.getId(), apparel, tshirts.getId(), tshirts, food.getId(), food);
        assertTrue(d.matchesProduct(ProductMatching.contextFor(item("SHIRT", tshirts, "1", 1).getProductVariant(), byId)));
        assertFalse(d.matchesProduct(ProductMatching.contextFor(item("BREAD", food, "1", 1).getProductVariant(), byId)));
        // usage cap semantics: null / 0 / negative = unlimited
        assertTrue(d.isUnlimited());
        d.setMaxUses(0); assertTrue(d.isUnlimited());
        d.setMaxUses(3); assertFalse(d.isUnlimited());
    }

    // ---- fixtures -----------------------------------------------------------

    private static TaxRule rule(String name, String rate, String country, String state) {
        TaxRule r = new TaxRule();
        r.setId(UUID.randomUUID());
        r.setName(name);
        r.setRate(new BigDecimal(rate));
        r.setCountry(country);
        r.setState(state);
        r.setPriority(0);
        r.setActive(true);
        return r;
    }

    private static Category category(String name, Category parent) {
        Category c = new Category();
        c.setId(UUID.randomUUID());
        c.setName(name);
        c.setParentCategoryId(parent == null ? null : parent.getId());
        return c;
    }

    private static Tag tag(String name) {
        Tag t = new Tag();
        t.setId(UUID.randomUUID());
        t.setName(name);
        return t;
    }

    private CartItem item(String sku, Category category, String price, int qty) {
        Product p = new Product();
        p.setId(UUID.randomUUID());
        p.setName(sku);
        p.setCategory(category);
        if ("SHIRT".equals(sku)) p.setTags(new java.util.HashSet<>(Set.of(luxury)));
        ProductVariant v = new ProductVariant();
        v.setId(UUID.randomUUID());
        v.setSku(sku);
        v.setProduct(p);
        CartItem ci = new CartItem();
        ci.setProductVariant(v);
        ci.setQuantity(qty);
        ci.setPriceAtTime(new BigDecimal(price));
        return ci;
    }

    private static Cart cart(CartItem... items) {
        Cart c = new Cart();
        c.setItems(new java.util.ArrayList<>(List.of(items)));
        return c;
    }

    private static Address address(String country, String state) {
        Address a = new Address();
        a.setCountry(country);
        a.setState(state);
        return a;
    }
}
