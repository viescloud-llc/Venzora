package com.viescloud.llc.venzora.model.product;

/**
 * UI-rendering hint for a single filter dimension. Maps roughly to the kind of
 * input control the frontend should render: search box, text input, number,
 * toggle, single-select, multi-select, numeric range, price range, date pickers.
 */
public enum FilterKind {
    /** Free-text substring search (case-insensitive). One value, one input. */
    TEXT_SEARCH,
    /** Exact-match text input. One value. */
    TEXT,
    /** Exact-match number input. One value. */
    NUMBER,
    /** Toggle / checkbox. One value, true or false. */
    BOOLEAN,
    /** Single-select dropdown / radio group. */
    SINGLE_SELECT,
    /** Multi-select checkboxes; the query param is repeatable (OR within key). */
    MULTI_SELECT,
    /** {@code key} = min, {@code secondaryKey} = max. Generic number range. */
    RANGE_NUMBER,
    /** Like RANGE_NUMBER but {@code ranges} is keyed by currency code. */
    RANGE_PRICE,
    DATE,
    TIME,
    DATE_TIME
}
