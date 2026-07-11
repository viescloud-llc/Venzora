# Venzora — API & Data Reference

> The single source of truth for every entity shape and every REST endpoint the Venzora backend exposes. Frontend developers should read § 1–4 once, then jump to whichever resource section they need. For end-to-end checkout flow see [`checkout.md`](checkout.md). For the *intent* of each frontend (storefront vs back-office), see [`frontend-client.md`](frontend-client.md) and [`frontend-manager.md`](frontend-manager.md).

---

## 1. Conventions

### 1.1 Auth & required headers

| Header | Required | Notes |
|---|---|---|
| `Authorization` | Yes (most endpoints) | `Bearer <jwt>`. Identifies the user. |
| `user_id` | No (read from JWT in normal traffic) | Optional gateway override. Custom Venzora endpoints (`/orders/checkout`, `/orders/{id}/complete`, `/discounts/validate`) accept the UUID directly here. |
| `Content-Type` | Yes (writes) | `application/json`. |
| `Accept` | Optional | `application/json` default. |

Two flavors of CRUD controller power the data endpoints:

- **Admin-gated** (`ViesAutoAdminCheckController`) — non-admins get `403`.
- **User-scoped** (`ViesControllerWithUserAccess`) — rows are auto-filtered by the JWT user's id; `POST` auto-stamps `ownerUserId`.

### 1.2 Type mapping (Java → TypeScript)

| Java | TypeScript | Notes |
|---|---|---|
| `UUID` | `string` | UUIDv7 (sortable). 36-char canonical form. Never send on `POST`. |
| `String` | `string` | |
| `Boolean` | `boolean` | Stored as `'true'`/`'false'` text in DB; serialized as JSON boolean. |
| `Integer` | `number` | 32-bit; safe for JS numbers. |
| `Long` | `number` | Only used for counters/quantities, never IDs. Within safe integer range in practice. |
| `BigDecimal` | `string` | Treat money as a string. Avoid `Number()` for math. |
| Enum | string literal union | Serialized by `name()`, e.g. `'ACTIVE'`. See [§ 10](#10-enums). |
| `Set<T>` / `List<T>` | `T[]` | Order is not guaranteed for `Set`. |
| `DateTime` (custom) | `DateTime` interface | Structured object, **not** ISO 8601. See [§ 3.4](#34-datetime--date--time). |
| `Date` (custom) | `Date` interface | Date-only. |
| `Time` (custom) | `Time` interface | Time-of-day only. |

### 1.3 General rules

- **IDs are UUIDv7** — server-generated. Never send `id` on `POST`.
- **Relationships are eagerly loaded** — GET responses are deep trees. Plan list views accordingly.
- **Cascade deletes propagate.** Deleting a `Product` wipes its variants, attributes, media. Confirm with dependent counts in UIs.
- **Optionality marker**: `?` on a TS field means it may be missing or null in JSON. Required-in-DB fields are non-optional.

### 1.4 The `UserAccess` envelope

Entities extending `TrackedTimeStampUserAccess` (Cart, OrderFulfillment, ReturnRequest, WishProduct) include the [`UserAccess`](#33-useraccess-permissions-envelope) fields (`ownerUserId`, `sharedUsers`, `sharedGroups`, `sharedOthers`). Server-managed — the UI generally reads, never sets, unless you're building a sharing dialog.

---

## 2. The seven CRUD verbs

Every CRUD controller in this project honors the same seven endpoints, mounted under its `@RequestMapping` base path. We describe them once and refer to them by short name (`GET /`, `GET /{id}`, …) elsewhere.

### `GET /` — list

```
GET {base}?page={n}&size={n}&{entity-field}={value}&...
```

- **Pagination**: `page` (0-indexed), `size`. Framework defaults apply if omitted.
- **Filtering**: any field of the entity may be a query parameter for exact-match filtering.
- **Matcher / match-by enums**: `propertyMatcher` (`EXACT`, `CONTAINS`, `STARTS_WITH`, `ENDS_WITH`, `IGNORE_CASE`) and `matchBy` (`ALL`, `ANY`).
- **Response**: `PageResponse<T>` (see § 2.1).

### `GET /{id}` — single record

`id` is the entity's UUID. Returns 404 if missing.

### `POST /matches` — complex match

```
POST {base}/matches?page={n}&size={n}&propertyMatcher={...}&matchBy={...}
Content-Type: application/json

{ ...entity-shaped filter, including nested objects... }
```

Use when you need to filter on nested objects or many fields that wouldn't fit a query string.

### `POST /` — create

Never send `id`. UUIDv7 is server-generated. Returns the created entity.

### `PUT /{id}` — full replace

Replaces the row's mutable fields. Pass the full entity. For owned children (e.g. variants on a Product), include them in the body; omit a member to detach (where `orphanRemoval = true`).

### `PATCH /{id}` — partial update

Only fields present in the body are updated. **Prefer this for narrow form edits.**

### `DELETE /{id}` — delete

204 on success. Cascade deletes propagate per the entity's JPA config.

### 2.1 `PageResponse<T>` shape

```ts
export interface PageResponse<T> {
  content: T[];
  page: number;          // 0-based
  size: number;
  totalElements: number;
  totalPages: number;
  // additional metadata may be present; treat unknown keys gracefully
}
```

### 2.2 Errors

| Status | Meaning |
|---|---|
| 200 | OK |
| 201 | Created (POST may also return 200) |
| 204 | No content (DELETE success) |
| 400 | Validation error |
| 401 | Missing or invalid JWT |
| 403 | Authenticated but not authorized (admin-gated) |
| 404 | Entity not found |
| 409 | Conflict (unique violation, optimistic lock) |
| 503 | Service unavailable (e.g. checkout module not configured) |

Build one error-mapping service on the frontend.

---

## 3. Base shapes (inherited)

Every entity extending one of these gains these fields in JSON automatically.

### 3.1 `TrackedTimeStamp`

```ts
export interface TrackedTimeStamp {
  createdAt?: DateTime;
  updatedAt?: DateTime;
}
```

### 3.2 `TrackedTimeStampUserAccess`

```ts
export interface TrackedTimeStampUserAccess
  extends TrackedTimeStamp,
          UserAccess {}
```

### 3.3 `UserAccess` (permissions envelope)

```ts
export interface UserAccess {
  inputUserId?: string;     // server-set
  ownerUserId?: string;     // server-set
  sharedUsers?: UserPermission[];
  sharedGroups?: GroupPermission[];
  sharedOthers?: AccessPermissionEnum[];
}

export interface UserPermission { userId: string;  permissions: AccessPermissionEnum[]; }
export interface GroupPermission { groupId: string; permissions: AccessPermissionEnum[]; }

export type AccessPermissionEnum = 'READ' | 'WRITE' | 'DELETE';
```

### 3.4 `DateTime` / `Date` / `Time`

Structured objects, **not** ISO 8601. Round-trip as-is; convert to a JS `Date` only when rendering.

```ts
export interface DateTime {
  year: number;            // YYYY
  month: number;           // 1..12
  day: number;             // 1..31
  hour: number;            // 0..23
  minute: number;          // 0..59
  second: number;          // 0..59
  millis: number;
  currentZoneId?: string;  // e.g. 'America/New_York'
  bypassMax?: boolean;
}

export interface Date {
  year: number; month: number; day: number;
  currentZoneId?: string;
  bypassMax?: boolean;
}

export interface Time {
  hour: number; minute: number; second: number; millis: number;
  currentZoneId?: string;
  bypassMax?: boolean;
}
```

Build one `DateTimeUtil` with `fromJsDate(d): DateTime` and `toJsDate(dt): JsDate` and reuse it everywhere.

---

## 4. Value objects (embeddable, no endpoints)

### 4.1 `Address`

```ts
export interface Address {
  street?: string;
  suite?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  type?: AddressType;       // 'BILLING' | 'SHIPPING'
}
```

Used by:
- `UserAddress.addresses` — a `Set<Address>`.
- `OrderFulfillment.shippingAddress` / `OrderFulfillment.billingAddress` — column-prefixed `shipping_*` / `billing_*` server-side.

### 4.2 `AttributeValue` *(polymorphic — read carefully)*

Embedded into both `ProductAttribute` and `ProductVariantAttribute`. A single object with **all possible value slots**; only the slot matching the parent `AttributeDefinition.type` should be populated.

```ts
export interface AttributeValue {
  textValue?: string;
  numberValue?: string;          // BigDecimal → string
  booleanValue?: boolean;
  dateValue?: Date;              // see § 3.4
  timeValue?: Time;
  dateTimeValue?: DateTime;
  selectValue?: AttributeOption;        // single (FK)
  multiSelectValues?: AttributeOption[]; // M2M
}
```

Mapping rule (from `AttributeDefinition.type`):

| `type` | Populate | Other slots |
|---|---|---|
| `TEXT` | `textValue` | null |
| `NUMBER` | `numberValue` | null |
| `BOOLEAN` | `booleanValue` | null |
| `SELECT` | `selectValue` | null |
| `MULTI_SELECT` | `multiSelectValues` | null |
| `DATE` | `dateValue` | null |
| `TIME` | `timeValue` | null |
| `DATE_TIME` | `dateTimeValue` | null |

Switch slots cleanly: **clear the previous slot before writing the new one** so stale data doesn't persist.

---

## 5. Catalog

### 5.1 `Product`

`/api/v1/products` · **Admin**

The hero entity. Required: `category`, `currency`, `basePrice`, `name`.

```ts
export interface Product extends TrackedTimeStamp {
  id?: string;
  name: string;
  description?: string;
  category: Category;              // required FK, deep-loaded
  currency: Currency;              // required, ISO 4217 — see § 10.12
  basePrice: string;               // BigDecimal
  baseSku?: string;
  status?: ProductStatus;
  tags?: Tag[];                    // M2M
  variants?: ProductVariant[];     // owned, cascade delete
  attributes?: ProductAttribute[]; // owned, cascade delete
  medias?: ProductMedia[];         // owned, cascade delete
}
```

- `DELETE /api/v1/products/{id}` cascades to variants, attributes, media. Confirm with dependent counts.
- `PUT` / `POST` may carry the full graph (variants, attributes, medias). Use `PATCH` for narrow edits.
- The whole controller is currently admin-gated — see [§ 13](#13-not-implemented-yet) for the public-catalog gap.

### 5.2 `ProductVariant`

`/api/v1/product/variants` · **Admin**

One row per SKU-level combination (e.g. "Small Red T-Shirt").

```ts
export interface ProductVariant extends TrackedTimeStamp {
  id?: string;
  product?: Product;                       // back-ref; omit on PUT to avoid recursion
  sku: string;                             // unique
  variantName?: string;
  price?: string;                          // BigDecimal — raw value, interpreted through priceMode
  priceMode: VariantPriceMode;             // default 'NORMAL'; see § 10.13
  effectivePrice?: string;                 // BigDecimal — READ-ONLY, computed from price + priceMode
  stockQuantity?: number;                  // Long
  weight?: string;                         // BigDecimal
  status?: ProductVariantStatus;
  medias?: ProductMedia[];                 // owned
  attributeValues?: ProductVariantAttribute[]; // owned
}
```

- `sku` is unique — duplicate POST yields 409.
- For storefront variant pickers, read variants through `Product.variants` not this endpoint.
- **`price` + `priceMode` → `effectivePrice`**: `price` is the raw number the admin sets. `priceMode` decides how it's interpreted — see [§ 10.13](#1013-variantpricemode). `effectivePrice` is the resolved value the shopper should actually pay, computed server-side on every serialization. It's not persisted; treat it as read-only from the client. Cart-add flows, the checkout orchestrator's per-item math, and storefront price rendering should all use `effectivePrice`.

### 5.3 `ProductMedia`

`/api/v1/product/medias` · **Admin**

```ts
export interface ProductMedia {
  id?: string;
  product?: Product;                 // present when attached to a Product
  productVariant?: ProductVariant;   // present when attached to a Variant
  url?: string;                      // direct URL to the asset
  objectStorageDataId?: string;      // UUID of a data object in object storage
  mediaType?: ProductMediaType;      // 'IMAGE' | 'VIDEO'
  altText?: string;
  caption?: string;
  sortOrder: number;                 // default 0
  isPrimary: boolean;                // default false
}
```

- Exactly one of `product` or `productVariant` should be set (UI must enforce; server doesn't).
- **Source constraint (server-enforced)**: at least one of `url` or `objectStorageDataId` must be non-null on every create / update. Both null → `400` at insert/update via a JPA `@PrePersist` / `@PreUpdate` validator. Both set is allowed but discouraged — pick one source.
- Use `url` for externally-hosted assets (CDN links, third-party images). Use `objectStorageDataId` when the asset lives in the internal object-storage service; the storefront resolves it to a served URL at read time.

### 5.4 `Category`

`/api/v1/categories` · **Admin**

Self-referential tree via `parentCategoryId` (no FK constraint — plain column).

```ts
export interface Category {
  id?: string;
  name: string;
  description?: string;
  parentCategoryId?: string;                  // null at root
  attributeDefinitions?: AttributeDefinition[]; // M2M — which attrs apply
  parentCategory?: Category;                  // @Transient, may be unpopulated
  childrenCategories?: Category[];            // @Transient, may be unpopulated
}
```

- Build the tree client-side from `parentCategoryId`; do not rely on `parentCategory` / `childrenCategories` being populated.
- `attributeDefinitions` drives the suggested attribute list when editing a product in this category.

### 5.5 `Tag`

`/api/v1/tags` · **Admin**

```ts
export interface Tag {
  id?: string;
  name: string;
  description?: string;
}
```

Plain label. Joined to `Product` via M2M.

---

## 6. Attribute system

### 6.1 `AttributeDefinition`

`/api/v1/product/attribute/definitions` · **Admin**

```ts
export interface AttributeDefinition {
  id?: string;
  name: string;                       // unique, e.g. 'Size'
  displayName?: string;
  type?: ProductAttributeType;        // see § 10.3
  unit?: string;                      // e.g. 'cm', 'kg'
  options?: AttributeOption[];        // owned; only meaningful for SELECT / MULTI_SELECT
}
```

- An `AttributeDefinition` describes a *concept* (Size, Color, Material) — it does not decide how a specific product uses it. Whether an attribute is set once per product or per variant is determined by which row exists:
  - Row in `ProductAttribute` → applied to the whole product (a spec).
  - Row in `ProductVariantAttribute` → applied to a specific variant (a SKU-dimension).
- The **same definition can be used both ways on different products.** A "Color" definition can be a spec on a desk (`ProductAttribute`) and a variant driver on a t-shirt (`ProductVariantAttribute`).
- `name` is unique. POST/PUT with nested `options` cascade-saves them.

### 6.2 `AttributeOption`

`/api/v1/product/attribute/options` · **Admin**

```ts
export interface AttributeOption {
  id?: string;
  value: string;             // canonical key
  displayValue?: string;
  sortOrder?: number;        // Long
  // attributeDefinition is @JsonIgnore — NOT in JSON
}
```

Manage as nested arrays on `AttributeDefinition` for normal flows; this endpoint exists for surgical edits.

### 6.3 `ProductAttribute`

`/api/v1/product/attributes` · **Admin**

```ts
export interface ProductAttribute {
  id?: string;
  product?: Product;
  attributeDefinition: AttributeDefinition;
  attributeValue: AttributeValue;          // polymorphic — § 4.2
}
```

### 6.4 `ProductVariantAttribute`

`/api/v1/product/variant/attributes` · **Admin**

```ts
export interface ProductVariantAttribute {
  id?: string;
  variant?: ProductVariant;
  attributeDefinition: AttributeDefinition;
  attributeValue: AttributeValue;
}
```

---

## 7. Commerce

> **Architecture note — the checkout split.** Venzora no longer owns the payment-side of a purchase. The `vies-spring-utils` checkout module (6.2.9) ships `CheckoutOrder` + `CheckoutTransaction` for the payment lifecycle. Venzora retains the **fulfillment side** as `OrderFulfillment` + `OrderFulfillmentItem`, linked to a `CheckoutOrder` via `OrderFulfillment.checkoutOrderId`. The full Alice-buys-a-Pro-license walkthrough is in [`checkout.md`](checkout.md).

### 7.1 `Cart`

`/api/v1/carts` · **User-scoped**

```ts
export interface Cart extends TrackedTimeStampUserAccess {
  id?: string;
  userId: string;                  // UUID of the owning user
  items: CartItem[];               // owned, orphanRemoval
  totalPrice: string;              // BigDecimal, default '0'
  active: boolean;                 // default true
}
```

`userId` / `ownerUserId` are server-stamped on POST — do not set in the body. The checkout orchestrator marks the cart `active: false` when it deactivates a cart on `/orders/checkout`.

### 7.2 `CartItem`

`/api/v1/cart/items` · **Admin**

```ts
export interface CartItem extends TrackedTimeStamp {
  id?: string;
  cart?: Cart;
  productVariant: ProductVariant;
  quantity: number;                // Integer
  priceAtTime: string;             // BigDecimal — snapshot at add time
}
```

Prefer managing items through the parent `Cart`. This endpoint is for granular debugging.

### 7.3 `OrderFulfillment`

`/api/v1/orders` · **User-scoped**

The Venzora-side record of a purchase. Bridges to a library `CheckoutOrder` via `checkoutOrderId`. Table name: `order_fulfillments`.

```ts
export interface OrderFulfillment extends TrackedTimeStampUserAccess {
  id?: string;
  orderNumber: string;             // unique, human-facing (e.g. 'VEN-A3F2B8C1')
  userId: string;                  // UUID of the buyer
  checkoutOrderId?: string;        // UUID → CheckoutOrder.id; null until checkout
  currency: Currency;              // denormalized cart currency at checkout time
  items: OrderFulfillmentItem[];   // owned, orphanRemoval
  subtotal: string;
  tax: string;                     // computed by TaxCalculator at checkout
  shippingCost: string;            // computed by ShippingRule lookup
  discountAmount: string;          // default '0'
  totalAmount: string;
  status: FulfillmentStatus;       // see § 10.5 — no PAYMENT_* states
  shippingAddress: Address;
  billingAddress: Address;
  notes?: string;
  metadata?: Record<string, string>;  // snapshot bag — see "metadata convention" below
}
```

- `checkoutOrderId` is a plain UUID column (not a JPA `@ManyToOne`) — bridge stays loose so cross-package `@EntityScan` isn't needed.
- Resolve it against `GET /api/v1/checkout/orders/{id}` (library) for payment status, `approveUrl`, refund total.

#### Metadata convention

`metadata` is a free-form `Map<String, String>` bag stored on a side table. It serves two purposes:

1. **System-managed snapshot** — the checkout orchestrator writes these keys at sale time. Treat as immutable history (don't edit from the UI):

| Key | Meaning |
|---|---|
| `checkout.provider` | `"paypal"` etc. |
| `checkout.providerOrderId` | PayPal's order id |
| `checkout.approveUrl` | URL the buyer was redirected to |
| `checkout.cartId` | UUID of the originating Cart |
| `checkout.currency` | echo of `currency` for completeness |
| `checkout.capturedAt` | ISO instant when `complete()` ran |
| `discount.code` | applied coupon code (if any) |
| `discount.appliedAmount` | $ value of the discount |
| `discount.type` | `PERCENTAGE` / `FIXED_AMOUNT` / etc |
| `discount.ruleId` | `Discount.id` |
| `tax.ruleId` | `TaxRule.id` of the matched rule |
| `tax.ruleName` | name of that rule |
| `tax.rate` | percentage applied, e.g. `"8.875"` |
| `tax.jurisdiction` | formatted, e.g. `"US/NY/New York"` |
| `shipping.ruleId` | `ShippingRule.id` |
| `shipping.flatFee` | the flat-fee value on the rule |
| `shipping.freeShipping` | `"true"` if `freeAboveAmount` kicked in |

2. **Manager notes** — by convention, managers add free-form keys under `notes.*` to record incident context (`notes.fraudReview`, `notes.shippingDelay`, `notes.customerComplaint`, `notes.refundReason`, …). The backend doesn't enforce the prefix — anything goes.

Why a bag and not FKs? So the snapshot survives later edits or deletions of `TaxRule`, `ShippingRule`, `Discount`, or `CheckoutOrder`. The order's record of "this is what happened" doesn't depend on those rows staying alive.

### 7.4 `OrderFulfillmentItem`

`/api/v1/order/items` · **Admin**

```ts
export interface OrderFulfillmentItem extends TrackedTimeStamp {
  id?: string;
  orderFulfillment?: OrderFulfillment;
  productVariant: ProductVariant;   // required FK — preserved for inventory & returns
  quantity: number;                 // Integer
  unitPrice: string;
  totalPrice: string;
  lineItemSku?: string;             // mirrors CheckoutLineItem.sku
  productSnapshot?: string;         // JSON string — JSON.parse for historical product data
}
```

Prefer managing via the parent `OrderFulfillment`.

### 7.5 `Discount`

`/api/v1/discounts` · **Admin**

Coupon codes & promotions.

```ts
export interface Discount extends TrackedTimeStamp {
  id?: string;
  code: string;                    // unique
  description?: string;
  discountType: DiscountType;
  discountValue: string;           // BigDecimal — percent or fixed per discountType
  minimumOrderAmount?: string;
  maximumDiscountAmount?: string;
  validFrom: DateTime;             // enforced at checkout
  validTo: DateTime;               // enforced at checkout
  maxUses?: number;                // Integer — null = unlimited
  currentUses: number;             // default 0; bumped server-side
  active: boolean;                 // default true
}
```

`currentUses` is denormalized and bumped only by the checkout orchestrator. Never mutate from the UI.

### 7.6 *(Payment — removed from Venzora)*

Venzora's `Payment` entity was deleted. Payment records now live in the checkout module as `CheckoutTransaction` (kinds: `AUTHORIZE | CAPTURE | REFUND | VOID | RENEWAL_CHARGE | CHARGEBACK | DISPUTE`). Hit the library endpoints under `/api/v1/checkout/...` — full walkthrough in [`checkout.md`](checkout.md).

### 7.7 `Shipment`

`/api/v1/shipments` · **Admin**

```ts
export interface Shipment extends TrackedTimeStamp {
  id?: string;
  orderFulfillment: OrderFulfillment;    // required FK
  trackingNumber: string;                // unique
  carrier: string;                       // 'UPS' | 'FedEx' | etc — free text
  status: ShipmentStatus;
  estimatedDeliveryDate: DateTime;       // required
  actualDeliveryDate: DateTime;          // required (non-nullable in DB)
  notes?: string;
  trackingUrl?: string;
}
```

### 7.8 `ReturnRequest`

`/api/v1/returns` · **User-scoped**

```ts
export interface ReturnRequest extends TrackedTimeStampUserAccess {
  id?: string;
  returnNumber: string;                       // unique, human-facing
  orderFulfillment: OrderFulfillment;         // required FK
  orderFulfillmentItem: OrderFulfillmentItem; // required FK — which line item
  userId: string;                             // UUID
  status: ReturnStatus;
  reason: string;
  adminNotes?: string;
  returnQuantity: number;                     // Integer
  refundAmount: string;                       // BigDecimal — informational; real refund moves through CheckoutOrder.refundOrder
  trackingNumber?: string;
  refundShipping: boolean;                    // default false
}
```

### 7.9 `StockMovement`

`/api/v1/stock/movements` · **Admin**

Audit log of inventory changes. Treat append-only from the UI's perspective.

```ts
export interface StockMovement extends TrackedTimeStamp {
  id?: string;
  productVariant: ProductVariant;
  movementType: StockMovementType;
  quantityChange: number;          // Long
  quantityAfter: number;           // Long — denormalized running total
  reason?: string;
  reference?: string;              // free-form, e.g. order id
  userId?: string;                 // UUID — who performed it
}
```

Adjustments insert new rows; never PATCH `quantityAfter` directly.

### 7.10 `ShippingRule`

`/api/v1/shipping/rules` · **Admin**

One rule per currency (DB-level unique). Drives `OrderFulfillment.shippingCost`.

```ts
export interface ShippingRule extends TrackedTimeStamp {
  id?: string;
  currency: Currency;              // unique — see § 10.12
  flatFee: string;                 // BigDecimal — shipping when below the free-above threshold
  freeAboveAmount?: string;        // BigDecimal — null disables the free-shipping threshold
  description?: string;
  active: boolean;                 // default true
}
```

Missing or inactive rule → orchestrator falls back to zero shipping with a server-side warning log.

### 7.11 `TaxRule`

`/api/v1/tax/rules` · **Admin**

Self-hostable tax rules for any country. Each `country`/`state`/`city`/`postalCode` field is a **matcher** — null means "match any". A rule with all four matchers null is the implicit default catch-all.

```ts
export interface TaxRule extends TrackedTimeStamp {
  id?: string;
  name: string;                    // e.g. "NY State Sales Tax"
  rate: string;                    // BigDecimal — percentage, e.g. "8.00" for 8%
  country?: string;                // ISO 3166-1 alpha-2, e.g. "US"
  state?: string;                  // region code, e.g. "NY"
  city?: string;
  postalCode?: string;
  priority: number;                // Integer — tiebreaker when specificity is equal; higher wins
  active: boolean;                 // default true
  description?: string;
}
```

**Matching algorithm**: `TaxCalculator` loads all active rules, filters to those whose every non-null matcher equals the shipping address (case-insensitive for text), sorts by `(specificity DESC, priority DESC)`, applies the winner's rate to `(subtotal − discount)`. Falls back to zero when nothing matches.

#### `GET /api/v1/tax/rules/export`

Returns the full set of `TaxRule` rows as a JSON array. Use for backup, version control, or migrating curated rule sets between environments.

#### `POST /api/v1/tax/rules/import?mode={append|replace}`

Body: a JSON array of `TaxRule`s. `id`, `createdAt`, `updatedAt` are ignored — every imported rule gets a fresh UUID.

- `mode=append` (default): inserts; existing rules untouched.
- `mode=replace`: deletes every existing rule first, then inserts. Use for fresh installs / full re-syncs. Transactional — if any rule fails validation, nothing changes.

Response:
```json
{ "imported": 12, "replaced": 0, "mode": "append" }
{ "imported": 12, "replaced": 7, "mode": "replace" }
```

**Auth note**: the CRUD endpoints inherit framework admin gate; the import/export endpoints do **not**. Gate the path at the reverse proxy in production.

---

## 8. Custom flows (multi-entity orchestration)

These wrap multi-entity work into single atomic transactions. They are **not** CRUD; the seven verbs don't apply.

### 8.1 Checkout — `POST /api/v1/orders/checkout`

Header `user_id` required. Wraps cart validation, discount validation, stock pre-check, total computation, `OrderFulfillment` creation, library `CheckoutOrder` creation, and cart deactivation in one transaction.

```jsonc
// Request
{
  "cartId":          "0193de1a-b440-7c3c-ae0f-87a9a17a5edd",
  "shippingAddress": { "street": "...", "city": "...", "type": "SHIPPING" },
  "billingAddress":  { "street": "...", "city": "...", "type": "BILLING" },
  "discountCode":    "SAVE10",          // optional
  "provider":        "paypal",
  "returnUrl":       "https://venzora.app/checkout/return",
  "cancelUrl":       "https://venzora.app/checkout/cancel"
}

// 200 OK
{
  "orderFulfillment": { "id": "...", "checkoutOrderId": "...", "status": "PENDING", ... },
  "approveUrl":       "https://www.paypal.com/checkoutnow?token=..."
}
```

Errors: `400` (validation), `403` (cart not owned), `404` (cart missing), `503` (PayPal not configured).

### 8.2 Complete — `POST /api/v1/orders/{id}/complete`

Header `user_id` required. No body. Captures payment via the library, decrements stock, flips fulfillment to `PROCESSING`.

Returns the updated `OrderFulfillment`. Errors mirror checkout.

### 8.3 Discount validation — `POST /api/v1/discounts/validate`

Non-destructive coupon preview for the "Apply code" UX. Always returns 200 with a body — business rejections come back in the body, not as exceptions.

```jsonc
// Request
{ "code": "SAVE10", "cartId": "0193de1a-..." }

// 200 OK — valid
{ "valid": true,  "discountAmount": "4.90", "reason": null }

// 200 OK — rejected
{ "valid": false, "discountAmount": null,   "reason": "Discount expired (valid to 2025-12-31 23:59:59)" }
```

Checks: `code` present; cart exists and is yours; cart non-empty; code resolves; `active`; within `validFrom`/`validTo`; `currentUses < maxUses`; `subtotal >= minimumOrderAmount`. Real HTTP errors reserved for auth / cart-not-found.

### 8.4 Coexistence note

`OrderFulfillmentController` (CRUD) and `CheckoutOrchestratorController` (`/checkout`, `/{id}/complete`) live at the same `/api/v1/orders` base. Spring routes by full URL + verb; no collision. Likewise `DiscountController` (CRUD) and `DiscountValidationController` (`/validate`) both serve `/api/v1/discounts`.

---

## 8.4 Storefront read API (public, no auth)

Read-only endpoints the storefront uses to browse the catalog. **No JWT required** — anonymous shoppers can hit these. Returns only products with `status: ACTIVE`. Filtering rides the dynamic attribute schema so the frontend renders filter UI without baking the catalog's shape into the build.

### 8.4.1 List — `POST /api/v1/public/products/search` *(preferred)* / `GET /api/v1/public/products`

Two equivalent ways into the same logic — POST with a JSON body is the **primary** route (no URL-length limits, typed fields for everything); GET is kept for bookmarkability and quick curl/browser testing.

#### `POST /api/v1/public/products/search`

Request body — every field optional:

```jsonc
{
  "categoryId": "0193-...",
  "tagIds":     ["0193-tag-a", "0193-tag-b"],
  "q":          "cotton shirt",
  "currency":   "USD",
  "minPrice":   "20.00",
  "maxPrice":   "200.00",
  "attributes": {
    "Size":  ["Small", "Medium"],
    "Color": ["Red"],
    "Material": ["Cotton"],
    "ReleaseDate": ["2026-01-15"]
  },
  "page":    0,
  "size":    20,
  "sort":    "basePrice",
  "sortDir": "ASC"
}
```

Returns `PageResponse<Product>` (see [§ 2.1](#21-pageresponset-shape)). The `attributes` map is keyed by `AttributeDefinition.name`; each value list is OR-within-key. Different keys AND together. Identical semantics to the GET path — same service method underneath.

#### `GET /api/v1/public/products`

Same filters via query parameters. Subject to URL-length limits at proxies and browsers (typically 2–8 KB). Use this for simple cases and shareable URLs.

Query parameters:

| Parameter | Type | Notes |
|---|---|---|
| `categoryId` | UUID | Exact-match (no parent/child climb yet). |
| `tagIds` | UUID[] | Repeated values; product must have **any** of them (OR within key). |
| `q` | string | Case-insensitive substring on `name` + `description`. |
| `currency` | `Currency` | Only products in this currency. |
| `minPrice` / `maxPrice` | BigDecimal | Inclusive range on `basePrice`. |
| `attribute.<DefName>=<value>` | repeated | Faceted: **AND across attributes, OR within one**. See below. |
| `page` / `size` | int | 0-indexed; `size` defaults to 20, capped at 200. |
| `sort` | string | One of `basePrice`, `name`, `id`. Default: newest-first by `id` (UUIDv7 is time-sortable). |
| `sortDir` | `ASC` / `DESC` | Defaults to `DESC`. |

#### Attribute filtering example

```
GET /api/v1/public/products
   ?categoryId=...&attribute.Size=Small&attribute.Color=Red&attribute.Color=Blue
   &page=0&size=20
```

Means: *(Size = "Small") AND (Color = "Red" OR Color = "Blue")*. The frontend just renders one checkbox group per `AttributeDefinition`; the matching is done server-side.

The matcher checks both **product-level** (`ProductAttribute`) and **variant-level** (`ProductVariantAttribute`) values — a product matches if it has *any* row with that definition name and a value in the requested set. Every `AttributeDefinition.type` is honored: `TEXT`, `NUMBER` (exact), `BOOLEAN`, `SELECT`, `MULTI_SELECT`, `DATE`, `TIME`, `DATE_TIME` (all exact-match, ISO strings for the date/time types).

#### Combination semantics

Everything composes the way faceted-search shoppers expect:

- **AND across different keys** — `?categoryId=X&attribute.Size=S&minPrice=10` requires *every* condition to match.
- **OR within the same key** — `?attribute.Color=Red&attribute.Color=Blue` and `?tagIds=A&tagIds=B` match if *any* of the repeated values match.
- **Range pairs** — `minPrice` + `maxPrice` are inclusive bounds applied together.
- **Empty / missing parameters** — silently ignored. Only non-null filters narrow the result set.

Same combined example, both ways:

**POST (preferred)**:
```jsonc
POST /api/v1/public/products/search
{
  "categoryId": "...",
  "tagIds":     ["A", "B"],
  "q":          "cotton",
  "currency":   "USD",
  "minPrice":   "20", "maxPrice": "200",
  "attributes": {
    "Size":     ["S", "M"],
    "Material": ["Cotton"],
    "ReleaseDate": ["2026-01-15"]
  },
  "page": 0, "size": 20, "sort": "basePrice", "sortDir": "ASC"
}
```

**GET (equivalent)**:
```
GET /api/v1/public/products
  ?categoryId=...&tagIds=A&tagIds=B
  &q=cotton&currency=USD&minPrice=20&maxPrice=200
  &attribute.Size=S&attribute.Size=M
  &attribute.Material=Cotton
  &attribute.ReleaseDate=2026-01-15
  &page=0&size=20&sort=basePrice&sortDir=ASC
```

Both return products that:
1. Are in the given category, **and**
2. Have at least one of tags A or B, **and**
3. Mention "cotton" in name or description, **and**
4. Are priced in USD between $20 and $200, **and**
5. Have a Size attribute equal to either S or M, **and**
6. Have a Material attribute equal to Cotton, **and**
7. Have a ReleaseDate attribute equal to 2026-01-15.

Response: standard [`PageResponse<Product>`](#21-pageresponset-shape).

### 8.4.2 Single product — `GET /api/v1/public/products/{id}`

Returns the active product. 404 for unknown id or `status ≠ ACTIVE`.

### 8.4.3 Per-category filter dimensions — `GET /api/v1/public/products/filters?categoryId=`

A **lightweight, category-scoped** filter view. Per request, no caching. Useful when the storefront has narrowed the catalog to one category and wants only the attributes registered against it.

```json
{
  "categoryId": "0193...",
  "attributes": [
    {
      "id": "0193...", "name": "Size", "displayName": "Size",
      "type": "SELECT",
      "options": [
        { "id": "...", "value": "S", "displayValue": "Small",  "sortOrder": 1 },
        { "id": "...", "value": "M", "displayValue": "Medium", "sortOrder": 2 }
      ]
    },
    { "id": "...", "name": "Color", "type": "MULTI_SELECT", "options": [...] }
  ],
  "currencies": ["USD"],
  "priceRange": { "min": "12.00", "max": "499.00", "currency": "USD" }
}
```

- `attributes` is `Category.attributeDefinitions` when a category is supplied, or all `AttributeDefinition`s globally.
- `priceRange` is populated only when one currency is in scope; the frontend falls back to no slider when multi-currency.

### 8.4.4 Full filter map *(cached)* — `GET /api/v1/public/products/filter-map`

The **complete, self-describing filter catalog** for the storefront — *every* query parameter the shopper may use against `GET /public/products`, with a `kind` hint that tells the frontend how to render each one (text input, range slider, single-select dropdown, multi-select checkboxes, etc).

This is the recommended way to power dynamic filter UI. The storefront calls this once at startup (or per session), iterates `filters[]`, and renders a control per entry — no hardcoded knowledge of the catalog's schema needed.

**Backing**: a `volatile` in-memory cache refreshed by a background scheduler every **60 seconds**. The first request after startup may take longer while the cache warms; subsequent requests are served from memory. If a refresh fails (DB hiccup), the previous snapshot is kept rather than blanked.

```json
{
  "computedAt": "2026-06-18T17:02:59.123Z",
  "filters": [
    {
      "key": "q", "displayName": "Search",
      "kind": "TEXT_SEARCH",
      "meta": { "appliesTo": "name+description" }
    },
    {
      "key": "categoryId", "displayName": "Category",
      "kind": "SINGLE_SELECT",
      "options": [
        { "value": "0193-...-cat-shoes", "displayValue": "Shoes" },
        { "value": "0193-...-cat-shirts", "displayValue": "Shirts" }
      ]
    },
    {
      "key": "tagIds", "displayName": "Tags",
      "kind": "MULTI_SELECT", "multiValue": true,
      "options": [{ "value": "...", "displayValue": "Featured" }]
    },
    {
      "key": "currency", "displayName": "Currency",
      "kind": "SINGLE_SELECT",
      "options": [
        { "value": "USD", "displayValue": "USD" },
        { "value": "EUR", "displayValue": "EUR" }
      ]
    },
    {
      "key": "minPrice", "secondaryKey": "maxPrice", "displayName": "Price",
      "kind": "RANGE_PRICE",
      "ranges": {
        "USD": { "min": "10.00", "max": "500.00" },
        "EUR": { "min": "9.00",  "max": "450.00" }
      }
    },
    {
      "key": "attribute.Size", "displayName": "Size",
      "kind": "SINGLE_SELECT", "multiValue": true,
      "options": [
        { "value": "S", "displayValue": "Small" },
        { "value": "M", "displayValue": "Medium" }
      ],
      "meta": { "attribute.type": "SELECT" }
    },
    {
      "key": "attribute.Material", "displayName": "Material",
      "kind": "TEXT",
      "meta": { "attribute.type": "TEXT" }
    }
  ]
}
```

#### `kind` reference

| Kind | Render hint | How to submit |
|---|---|---|
| `TEXT_SEARCH` | Free-text search box | `?key=<value>` |
| `TEXT` | Exact-match text input | `?key=<value>` |
| `NUMBER` | Exact-match number input | `?key=<number>` |
| `BOOLEAN` | Toggle / checkbox | `?key=true` or `?key=false` |
| `SINGLE_SELECT` | Dropdown or radio group | `?key=<value>` |
| `MULTI_SELECT` | Checkbox list | `?key=v1&key=v2` (repeat) |
| `RANGE_NUMBER` | Min / max number inputs | `?key=<min>&secondaryKey=<max>` |
| `RANGE_PRICE` | Min / max number inputs, currency-aware | same as RANGE_NUMBER; use the `ranges[currency]` bounds for the active currency |
| `DATE` / `TIME` / `DATE_TIME` | Corresponding picker | `?key=<ISO>` |

#### Meta keys

The `meta` map carries any additional info that doesn't fit the primary fields. Known keys:

| Key | Where | Meaning |
|---|---|---|
| `appliesTo` | TEXT_SEARCH | What field(s) the search hits server-side. |
| `attribute.type` | attribute filters | Underlying `ProductAttributeType` (`TEXT`, `SELECT`, …). |
| `attribute.unit` | attribute filters | The definition's unit string (e.g. `"cm"`, `"kg"`). |

#### When to use which endpoint

- **`/filter-map`** — primary. Cached, has kind hints, covers everything. Use this to render the storefront's filter panel.
- **`/filters?categoryId=`** — narrower per-category view. Useful if you want only the attributes the admin explicitly attached to a category. Computed per request (no cache).

### 8.4.5 Public reviews — `GET /api/v1/public/products/{productId}/reviews`

Returns paginated `Review` rows for a product. Used by the product-detail page. Same pagination shape as § 8.4.1. No auth required.

---

## 8.5 Self-service "me" endpoints (user-scoped)

Per-user CRUD that bypasses the admin gate by reading the buyer's UUID from the `user_id` header and forcing ownership server-side. These work around the fact that `Review`, `UserInfo`, and `UserAddress` don't extend `UserAccess` (so they can't use `ViesControllerWithUserAccess`).

> **Auth note**: `user_id` is required on every endpoint here. Missing → 401; not a UUID → 400; mismatched ownership → 403.

### 8.6.1 My reviews — `/api/v1/me/reviews`

| Method | Path | Behavior |
|---|---|---|
| `GET` | `/` | Paginated list of my reviews. Same params as public reviews: `page`, `size`, `sort`, `sortDir`. |
| `POST` | `/` | Create. `userId` is server-stamped from the header — body's `userId` is ignored. Requires `productId` and `rating`. |
| `PUT` | `/{id}` | Update. 403 if the review doesn't belong to me. Only `comment` and `rating` are merged. |
| `DELETE` | `/{id}` | Delete. 403 if not mine. |

### 8.6.2 My profile — `/api/v1/me/info`

`UserInfo` keys on `userId` (one row per user), so there's no list endpoint — just self.

| Method | Path | Behavior |
|---|---|---|
| `GET` | `/` | Returns my `UserInfo`. 404 if the row hasn't been created yet. |
| `PUT` | `/` | Upsert — creates the row if missing, replaces all fields otherwise. `userId` is server-stamped. |
| `PATCH` | `/` | Partial update — only fields present in the body are merged. Creates an empty row if none exists. |

### 8.6.3 My addresses — `/api/v1/me/addresses`

Same shape as `UserInfo` — one row per user, holding a `Set<Address>`.

| Method | Path | Behavior |
|---|---|---|
| `GET` | `/` | Returns my `UserAddress`. **Returns an empty (unsaved) row** when none exists yet, so the frontend can render "add your first address" without a separate POST. |
| `PUT` | `/` | Upsert — replaces the full address set. To add or remove an address, the frontend mutates the array client-side and PUTs the whole row. |

---

## 8.6 Reports (analytics)

Read-only statistics over `OrderFulfillment` for dashboards and tax filing. All endpoints take ISO-8601 `from` and `to` query parameters; all responses split by currency (since multi-currency tenants can't sum dollars + euros). The frontend handles charts — these endpoints return raw JSON only.

> **Auth note**: the `/api/v1/reports/*` path inherits no automatic admin gate from the framework. In production, gate at the reverse proxy.

> **Performance note**: filtering is in-memory (load orders, prune by date, aggregate). Fine for tenants up to hundreds of thousands of orders. Larger tenants need a timestamp index or pre-aggregated daily totals — flag when load grows.

### 8.6.1 Tax — `GET /api/v1/reports/tax?from=&to=`

The headline. Groups every captured order by `(currency, country, state, city, postalCode)` from the shipping address. Numbers come from `OrderFulfillment.tax` (what was actually charged); the `matchingRule` field is resolved at report time against the *current* `TaxRule` registry and is informational only (it can drift from the rule that was applied at sale time — that historical fact lives in `OrderFulfillment.metadata.tax.*`).

```json
{
  "period": { "from": "2026-01-01T00:00:00Z", "to": "2026-03-31T23:59:59Z" },
  "byCurrency": [
    {
      "currency": "USD",
      "jurisdictions": [
        {
          "country": "US", "state": "NY", "city": "New York", "postalCode": null,
          "orderCount": 45,
          "grossSales": "12450.00", "taxableAmount": "11200.00",
          "taxCollected": "994.40", "taxRefunded": "44.50",
          "netTaxCollected": "949.90", "effectiveRate": "8.88",
          "matchingRule": { "id": "0193...", "name": "NYC Sales Tax", "rate": "8.875" }
        },
        { "country": "US", "state": "NY", "city": null, "postalCode": null, ... },
        { "country": "DE", "state": null, "city": null, "postalCode": null, ...,
          "matchingRule": { "name": "Germany VAT", "rate": "19.00" } }
      ],
      "totals": { "orderCount": 67, "grossSales": "17250.00",
                  "taxCollected": "1361.40", "taxRefunded": "44.50", "netTaxCollected": "1316.90" }
    }
  ]
}
```

Refund accounting: each order's refunded tax is `(amountRefunded / totalAmount) × tax`. The `amountRefunded` is pulled from `CheckoutOrder` via the library DAO; if the checkout module isn't configured, refunds are silently zero (no error).

### 8.6.2 Sales summary — `GET /api/v1/reports/sales/summary?from=&to=`

Top-line KPIs per currency.

```json
{
  "period": { "from": "...", "to": "..." },
  "byCurrency": [{
    "currency": "USD",
    "orderCount": 67, "grossRevenue": "17250.00", "discounts": "230.00",
    "tax": "1361.40", "shipping": "318.00", "totalGross": "18699.40",
    "averageOrderValue": "279.10",
    "refundCount": 3, "refundAmount": "189.50"
  }]
}
```

### 8.6.3 Sales timeseries — `GET /api/v1/reports/sales/timeseries?from=&to=&bucket={day|week|month}`

Bucketed for trend charts. `bucket` is the time grain.

```json
{
  "period": { "from": "...", "to": "..." },
  "bucket": "day",
  "byCurrency": [{
    "currency": "USD",
    "points": [
      { "bucket": "2026-01-15", "orderCount": 4, "revenue": "320.00", "tax": "25.40" },
      { "bucket": "2026-01-16", "orderCount": 7, "revenue": "560.00", "tax": "44.20" }
    ]
  }]
}
```

For `week`, the `bucket` label is the Monday of the week (`"2026-01-13"`). For `month`, it's `"2026-01"`.

### 8.6.4 Top products — `GET /api/v1/reports/products/top?from=&to=&by={revenue|quantity}&limit=`

Ranked product list per currency. `limit` defaults to 10, capped at 200.

```json
{
  "period": { "from": "...", "to": "..." },
  "orderedBy": "revenue",
  "byCurrency": [{
    "currency": "USD",
    "products": [
      { "productId": "0193...", "name": "Pro License", "unitsSold": 41, "revenue": "2009.00" },
      ...
    ]
  }]
}
```

### 8.6.5 Top categories — `GET /api/v1/reports/categories/top?from=&to=&by=&limit=`

Same shape as top products but grouped by `Category`.

### 8.6.6 Geography — `GET /api/v1/reports/geography?from=&to=&groupBy={country|state|city}`

```json
{
  "period": { "from": "...", "to": "..." },
  "groupBy": "country",
  "byCurrency": [{
    "currency": "USD",
    "locations": [
      { "country": "US", "state": null, "city": null, "orderCount": 50, "revenue": "12000.00" },
      { "country": "CA", "state": null, "city": null, "orderCount": 12, "revenue": "2400.00" }
    ]
  }]
}
```

### 8.6.7 Order status — `GET /api/v1/reports/orders/status?from=&to=`

Funnel / pipeline view. Counts by every `FulfillmentStatus`.

```json
{
  "period": { "from": "...", "to": "..." },
  "totalOrders": 78,
  "counts": {
    "PENDING": 4, "PROCESSING": 12, "SHIPPED": 18, "DELIVERED": 38,
    "CANCELLED": 2, "REFUNDED": 1, "PARTIALLY_REFUNDED": 2,
    "RETURNED": 0, "FAILED": 1
  }
}
```

### 8.6.8 Refunds — `GET /api/v1/reports/refunds?from=&to=`

```json
{
  "period": { "from": "...", "to": "..." },
  "byCurrency": [{
    "currency": "USD",
    "totalOrders": 67, "refundCount": 3, "totalRefunded": "189.50",
    "refundRate": "0.0448"
  }]
}
```

`refundRate` is `refundCount / totalOrders`, four decimals.

### 8.6.9 Customers — `GET /api/v1/reports/customers/summary?from=&to=&limit=`

```json
{
  "period": { "from": "...", "to": "..." },
  "totalCustomers": 42,
  "byCurrency": [{
    "currency": "USD",
    "topByRevenue": [
      { "userId": "0193...", "orderCount": 5, "revenue": "1240.00" },
      ...
    ]
  }]
}
```

### 8.6.10 Raw export — `GET /api/v1/reports/orders?from=&to=&page=&size=`

Denormalized one-row-per-order export for power users to drop into Excel / Metabase / Looker. Paginated (`size` defaults to 100, capped at 1000).

```json
{
  "period": { "from": "...", "to": "..." },
  "page": 0, "size": 100, "totalElements": 245,
  "rows": [
    {
      "orderFulfillmentId": "0193...", "checkoutOrderId": "5O190...",
      "orderNumber": "VEN-A3F2B8C1",
      "createdAt": { "year": 2026, "month": 1, "day": 15, ... },
      "userId": "0193...",
      "currency": "USD",
      "subtotal": "49.00", "discountAmount": "0.00", "tax": "4.35",
      "shippingCost": "0.00", "totalAmount": "53.35",
      "status": "DELIVERED",
      "shippingCountry": "US", "shippingState": "NY",
      "shippingCity": "New York", "shippingPostalCode": "10001",
      "itemCount": 1
    }
  ]
}
```

---

## 9. User & social

### 9.1 `UserInfo`

`/api/v1/user/infos` · **Admin today** *(should be self-service — see [§ 13](#13-not-implemented-yet))*

```ts
export interface UserInfo extends TrackedTimeStamp {
  userId: string;                  // UUID — primary key, supplied externally
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  avatarUrl?: string;
  verified?: boolean;
  inactive?: boolean;
}
```

`userId` is both `@Id` and the link to the auth user. No `id` field — use `userId`.

### 9.2 `UserAddress`

`/api/v1/user/addresses` · **Admin today** *(should be self-service — see [§ 13](#13-not-implemented-yet))*

```ts
export interface UserAddress extends TrackedTimeStamp {
  userId: string;                  // UUID — primary key
  addresses: Address[];            // Set<Address> — order not guaranteed
}
```

To add/remove an address, PUT the whole row with the full updated set.

### 9.3 `WishProduct`

`/api/v1/wishlists` · **User-scoped**

```ts
export interface WishProduct extends TrackedTimeStampUserAccess {
  id?: string;
  productId: string;               // UUID
  quantity: number;                // Long
}
```

`productId` is a plain UUID column (no FK relationship). Resolve against the product catalog to render.

### 9.4 `Review`

`/api/v1/reviews` · **Admin today** *(blocks shopper self-write — see [§ 13](#13-not-implemented-yet))*

```ts
export interface Review extends TrackedTimeStamp {
  id?: string;
  userId: string;                  // UUID
  productId: string;               // UUID
  comment?: string;
  rating: string;                  // BigDecimal, e.g. '4.5'
}
```

`Review` does not extend `UserAccess` — it can't use `ViesControllerWithUserAccess`. To let shoppers write/edit their own reviews, either migrate the model or add a custom user-scoped controller.

---

## 10. Enums

All enums serialize as their `name()` string. Define them as TS string literal unions or `as const` arrays.

### 10.1 `ProductStatus`
`'DRAFT' | 'ACTIVE' | 'INACTIVE' | 'DISCONTINUED'`

### 10.2 `ProductVariantStatus`
`'ACTIVE' | 'INACTIVE' | 'OUT_OF_STOCK'`

### 10.3 `ProductAttributeType`
`'TEXT' | 'NUMBER' | 'BOOLEAN' | 'SELECT' | 'MULTI_SELECT' | 'DATE' | 'TIME' | 'DATE_TIME'`

### 10.4 `ProductMediaType`
`'IMAGE' | 'VIDEO'`

### 10.5 `FulfillmentStatus`

Replaces the old `OrderStatus`. Payment-side states (`PAYMENT_PENDING`, `PAYMENT_CONFIRMED`) live on `CheckoutOrder.status` in the checkout module.

`'PENDING' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED' | 'RETURNED' | 'REFUNDED' | 'PARTIALLY_REFUNDED' | 'FAILED'`

### 10.6 `DiscountType`
`'PERCENTAGE' | 'FIXED_AMOUNT' | 'FREE_SHIPPING' | 'BUY_X_GET_Y'`

### 10.7 `ShipmentStatus`
`'PENDING' | 'PROCESSING' | 'PICKED' | 'PACKED' | 'SHIPPED' | 'IN_TRANSIT' | 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'FAILED' | 'RETURNED'`

### 10.8 `ReturnStatus`
`'REQUESTED' | 'APPROVED' | 'REJECTED' | 'SHIPPED' | 'RECEIVED' | 'INSPECTING' | 'REFUNDED' | 'REPLACED' | 'CANCELLED'`

### 10.9 `StockMovementType`
`'PURCHASE' | 'SALE' | 'ADJUSTMENT' | 'RETURN' | 'DAMAGE' | 'TRANSFER' | 'RESERVED' | 'UNRESERVED'`

### 10.10 `AddressType`
`'BILLING' | 'SHIPPING'`

### 10.11 `AccessPermissionEnum`
`'READ' | 'WRITE' | 'DELETE'`

### 10.12 `Currency`

ISO 4217 — 173 values. Render with a searchable combobox.

```ts
export type Currency =
  | 'AED' | 'AFN' | 'ALL' | 'AMD' | 'ANG' | 'AOA' | 'ARS' | 'AUD' | 'AWG' | 'AZN'
  | /* ... */
  | 'ZAR' | 'ZMW' | 'ZWL';
```

### 10.13 `VariantPriceMode`

Controls how `ProductVariant.price` is interpreted relative to the parent `Product.basePrice`. The server exposes the resolved value on the variant as `effectivePrice` (read-only) so consumers never need to branch on the mode themselves.

`'NORMAL' | 'FLAT_ADJUSTMENT' | 'PERCENT_ADJUSTMENT'`

| Mode | Meaning | Example (`basePrice = 100`) |
|---|---|---|
| `NORMAL` (default) | `price` IS the effective price. Ignores `basePrice`. | `price = 120` → `effectivePrice = 120` |
| `FLAT_ADJUSTMENT` | Signed delta added to `basePrice`. Positive = markup, negative = discount. | `price = +10` → `110`; `price = -10` → `90` |
| `PERCENT_ADJUSTMENT` | Signed percentage applied to `basePrice`. Formula: `basePrice × (1 + price/100)`, rounded to 2 decimals HALF_UP. | `price = +10` → `110`; `price = -10` → `90` |

Behavior notes:

- New variants default to `NORMAL` in Java and are persisted with `NOT NULL`. Clients that omit the field (or send `null`) get normalized to `NORMAL` at write time.
- If `product` or `product.basePrice` is missing at resolution time (e.g. a detached read), `effectivePrice` falls back to the raw `price`.
- On adjustment modes with `price = null`, the delta is treated as `0` — the effective price equals `basePrice`.

---

## 11. HTTP service blueprint

A generic base for every CRUD entity, then thin subclasses:

```ts
// core/http/crud.service.ts
export abstract class CrudService<T> {
  protected abstract baseUrl: string;
  constructor(protected http: HttpClient) {}

  list(params?: ListParams):    Observable<PageResponse<T>> { /* GET / */ }
  getById(id: string):          Observable<T>               { /* GET /{id} */ }
  matches(filter: Partial<T>, params?: ListParams): Observable<PageResponse<T>> { /* POST /matches */ }
  create(body: T):              Observable<T>               { /* POST / */ }
  update(id: string, body: T):  Observable<T>               { /* PUT /{id} */ }
  patch(id: string, body: Partial<T>): Observable<T>        { /* PATCH /{id} */ }
  delete(id: string):           Observable<void>            { /* DELETE /{id} */ }
}

// catalog/product.service.ts
@Injectable({ providedIn: 'root' })
export class ProductApi extends CrudService<Product> {
  protected baseUrl = '/api/v1/products';
}
```

JWT interceptor:

```ts
intercept(req: HttpRequest<any>, next: HttpHandler) {
  const token = this.auth.token();
  if (!token) return next.handle(req);
  return next.handle(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
}
```

Pair with a 401-catcher interceptor that clears the token and redirects to login.

---

## 12. Practical gotchas

1. **Bidirectional EAGER relationships can cause JSON cycles.** Only `AttributeOption.attributeDefinition` is `@JsonIgnore` today. Other back-refs (`ProductVariant.product`, `CartItem.cart`, `OrderFulfillmentItem.orderFulfillment`, `Shipment.orderFulfillment`) are serialized. If you receive unexpectedly deep or repeating objects, raise it.
2. **`BigDecimal` precision.** We treat it as `string` to be safe across JS floats. Confirm wire format with one round-trip before relaxing your TS type.
3. **`DateTime` is not ISO 8601.** Structured object. Build one utility module.
4. **UUIDv7 is sortable.** Sort lists by `id` for rough creation order.
5. **`productSnapshot` on `OrderFulfillmentItem` is a JSON string**, not a nested object. `JSON.parse` when needed.
6. **Categories form a tree by convention only.** No FK on `parentCategoryId`. Guard against cycles client-side.
7. **No DTOs.** Requests and responses use entity types directly. Payloads grow with the relationship graph; ask the backend for DTOs when a list view feels slow.
8. **PATCH over PUT for narrow edits.** PUT may clear fields you forgot to include.
9. **Cascade deletes are real.** Show dependent counts.
10. **Two product endpoints, two purposes.** Admin CRUD on `/api/v1/products` (see [§ 5.1](#51-product)) returns every product regardless of status; public read on `/api/v1/public/products` (see [§ 8.4](#84-storefront-read-api-public-no-auth)) returns only `ACTIVE` ones. Storefront calls the public path; the Manager uses the admin path.

---

## 13. Not implemented yet

Track these as backend coordination items, not as latent bugs:

- ~~**Public catalog read.**~~ **Shipped.** `GET /api/v1/public/products/...` — see [§ 8.4](#84-storefront-read-api-public-no-auth).
- ~~**Self-service for `Review` / `UserInfo` / `UserAddress`.**~~ **Shipped** via `/api/v1/me/...` — see [§ 8.5](#85-self-service-me-endpoints-user-scoped).
- **Webhook → fulfillment listener.** 6.2.9 ships `CheckoutWebhookListener`; we haven't implemented one yet. Today nothing automatically flips `OrderFulfillment.status` when a `CheckoutOrder` transitions out-of-band (browser closed, dashboard refund, dispute, chargeback).
- **Variant generator.** `POST /api/v1/products/{id}/generate-variants` — accept a cartesian-product spec and create variants. Today the UI loops.
- **Stock-race hardening.** Add `@Version` to `ProductVariant` so `complete()` detects concurrent captures cleanly.
- **Media uploader endpoint.** No upload endpoint yet — the frontend either sets `ProductMedia.url` to an externally-hosted asset or, once an object-storage service is wired, sets `ProductMedia.objectStorageDataId` to the uploaded blob's id. The server-side validator already enforces "at least one of the two must be set."
- **Search.** No full-text product search. Use `POST /matches?propertyMatcher=CONTAINS` as a stopgap.
- **Login / refresh-token paths.** Confirm exact paths exposed by `vies-spring-utils` and whether refresh tokens use HttpOnly cookies.
- **Currency consistency policy.** Mixed-currency carts are blocked at checkout but not at add-to-cart. Confirm UX.

---

## 14. Quick reference card

```
Auth         Authorization: Bearer <jwt>  on every authenticated call.
IDs          UUIDv7 strings, server-generated. Never send id on POST.
List         GET   /api/v1/<resource>?page=&size=&<field>=<value>...
Get one      GET   /api/v1/<resource>/{uuid}
Match        POST  /api/v1/<resource>/matches  (body = partial filter entity)
Create       POST  /api/v1/<resource>          (body = entity sans id)
Replace      PUT   /api/v1/<resource>/{uuid}   (body = full entity)
Update       PATCH /api/v1/<resource>/{uuid}   (body = partial entity)  ← prefer this
Delete       DELETE /api/v1/<resource>/{uuid}
Pagination   {content: T[], page, size, totalElements, totalPages}
Money        BigDecimal → string. Never coerce to number for math.
DateTime     Structured object, NOT ISO 8601 (§ 3.4).
Errors       400 validation, 401 no/bad JWT, 403 not admin, 404 missing, 409 unique, 503 module off.

Checkout     POST /api/v1/orders/checkout         (one-shot start)
             POST /api/v1/orders/{id}/complete    (one-shot capture)
Validate     POST /api/v1/discounts/validate      (non-destructive coupon preview)
Tax import   POST /api/v1/tax/rules/import?mode={append|replace}
Tax export   GET  /api/v1/tax/rules/export
```
