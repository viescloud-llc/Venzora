# Venzora — Domain Models Reference

> A reference for frontend developers (Angular + TypeScript) implementing UIs against the Venzora REST API. Source of truth is `src/main/java/com/viescloud/llc/venzora/model/`; if anything here drifts from the code, the code wins.

---

## 1. Conventions

### 1.1 Type mapping (Java → TypeScript)

| Java                                       | TypeScript                          | Notes                                                                          |
|--------------------------------------------|--------------------------------------|--------------------------------------------------------------------------------|
| `UUID`                                     | `string`                             | Canonical 36-char form, e.g. `'018f0c8a-...'`. All IDs are UUIDv7 (sortable).  |
| `String`                                   | `string`                             |                                                                                |
| `Boolean`                                  | `boolean`                            | Stored as `'true'`/`'false'` text in DB; serialized as JSON boolean.            |
| `Integer`                                  | `number`                             | 32-bit. Safe for JS numbers.                                                   |
| `Long`                                     | `number`                             | Only used for non-ID counters/quantities. Within safe integer range in practice.|
| `BigDecimal`                               | `string`                             | Always treat money/precision values as strings to avoid float drift.            |
| Enum                                       | string literal union                 | Serialized by `name()`, e.g. `'ACTIVE'`. See [§ 9 Enums](#9-enums).             |
| `Set<T>` / `List<T>`                       | `T[]`                                | Order is not guaranteed for `Set`; treat both as arrays in TS.                   |
| `DateTime` (custom)                        | `DateTime` interface                 | Structured object, NOT an ISO string. See [§ 3.4](#34-datetime--date--time).   |
| `Date` (custom)                            | `Date` interface                     | Date-only. See [§ 3.4](#34-datetime--date--time).                              |
| `Time` (custom)                            | `Time` interface                     | Time-of-day only. See [§ 3.4](#34-datetime--date--time).                       |

### 1.2 Names & general rules

- **IDs are UUIDv7** (time-ordered). Generated server-side via `@GeneratedUuidV7` — never send an `id` on a `POST`.
- **All relationship sides are eagerly loaded** server-side, so GET responses are deep object trees, not refs. Plan list views accordingly (consider asking the backend for a thinner DTO if list payloads get heavy).
- **Cascade deletes propagate**: deleting a Product cascades to its variants, attributes, media. UIs should confirm with the dependent count.
- **Optionality marker**: `?` in TS fields means *may be missing or null* in JSON. Required-in-DB fields are non-optional.

### 1.3 Auth & access control fields

Entities that extend `TrackedTimeStampUserAccess` (Cart, OrderFulfillment, ReturnRequest, WishProduct) include the [`UserAccess` envelope](#33-useraccess-permissions-envelope) (`inputUserId`, `ownerUserId`, `sharedUsers`, `sharedGroups`, `sharedOthers`). These are server-managed — the UI generally reads them, never sets them, unless you are explicitly building a sharing dialog.

---

## 2. Endpoint summary

Every controller extends `ViesAutoAdminCheckController` and exposes the standard CRUD verbs at the base path: `GET /`, `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}`. The `WishProductController` extends `ViesControllerWithUserAccess` instead (per-user scoping).

| Entity                  | Endpoint base                                  | Notes                              |
|-------------------------|------------------------------------------------|------------------------------------|
| `Product`               | `/api/v1/products`                             | Admin                              |
| `ProductVariant`        | `/api/v1/product/variants`                     | Admin                              |
| `ProductAttribute`      | `/api/v1/product/attributes`                   | Admin                              |
| `ProductVariantAttribute` | `/api/v1/product/variant/attributes`         | Admin                              |
| `AttributeDefinition`   | `/api/v1/product/attribute/definitions`        | Admin                              |
| `AttributeOption`       | `/api/v1/product/attribute/options`            | Admin                              |
| `WishProduct`           | `/api/v1/wishlists`                            | User-scoped                        |

Every entity now has a CRUD controller — see [`api.md`](api.md) for the full URL surface. Payment is no longer a Venzora entity; it lives in the checkout module as `CheckoutTransaction` (queried via `GET /api/v1/checkout/...`).

---

## 3. Base shapes & inherited fields

Every entity that extends `TrackedTimeStamp` or `TrackedTimeStampUserAccess` automatically gains these fields in JSON. Treat them as mixed into every entity that inherits.

### 3.1 `TrackedTimeStamp`

```ts
export interface TrackedTimeStamp {
  createdAt?: DateTime;
  updatedAt?: DateTime;
}
```

### 3.2 `TrackedTimeStampUserAccess`

Extends both `TrackedTimeStamp` and `UserAccess`.

```ts
export interface TrackedTimeStampUserAccess
  extends TrackedTimeStamp,
          UserAccess {}
```

### 3.3 `UserAccess` (permissions envelope)

```ts
export interface UserAccess {
  inputUserId?: string;     // user id passed in on the request (server-set)
  ownerUserId?: string;     // owner of this row (server-set)
  sharedUsers?: UserPermission[];
  sharedGroups?: GroupPermission[];
  sharedOthers?: AccessPermissionEnum[];   // serialized from Set<...>
}

export interface UserPermission {
  userId: string;
  permissions: AccessPermissionEnum[];     // serialized from HashSet<...>
}

export interface GroupPermission {
  groupId: string;
  permissions: AccessPermissionEnum[];
}

export type AccessPermissionEnum = 'READ' | 'WRITE' | 'DELETE';
```

### 3.4 `DateTime` / `Date` / `Time`

These are **structured objects**, not ISO 8601 strings. They are stored as plain text columns and serialized as objects. The frontend should keep them as-is when round-tripping and only convert to a JS `Date` for display.

```ts
export interface DateTime {
  year: number;
  month: number;          // 1..12
  day: number;            // 1..31
  hour: number;           // 0..23
  minute: number;         // 0..59
  second: number;         // 0..59
  millis: number;
  currentZoneId?: string; // e.g. 'America/New_York'
  bypassMax?: boolean;
}

export interface Date {
  year: number;
  month: number;
  day: number;
  currentZoneId?: string;
  bypassMax?: boolean;
}

export interface Time {
  hour: number;
  minute: number;
  second: number;
  millis: number;
  currentZoneId?: string;
  bypassMax?: boolean;
}
```

Recommended Angular helper: write a small `DateTimeUtil` with `fromJsDate(d: Date): DateTime` and `toJsDate(dt: DateTime): Date` once and reuse everywhere.

---

## 4. Embeddables & value objects

These are not entities — they are stored inline with the owning row. They appear inside other JSON objects but never on their own endpoints.

### 4.1 `Address`

```ts
export interface Address {
  street?: string;
  suite?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  type?: AddressType;     // 'BILLING' | 'SHIPPING'
}

export type AddressType = 'BILLING' | 'SHIPPING';
```

Used by:
- `UserAddress.addresses` — a `Set<Address>`.
- `OrderFulfillment.shippingAddress` and `OrderFulfillment.billingAddress` — column-prefixed with `shipping_` / `billing_` server-side.

### 4.2 `AttributeValue` *(polymorphic — read carefully)*

`AttributeValue` is embedded into both `ProductAttribute` and `ProductVariantAttribute`. It is a single object with **all possible value slots**; only the slot matching the parent `AttributeDefinition.type` should be populated, the rest stay null.

```ts
export interface AttributeValue {
  textValue?: string;
  numberValue?: string;          // BigDecimal → keep as string
  booleanValue?: boolean;
  dateValue?: Date;              // see § 3.4
  timeValue?: Time;
  dateTimeValue?: DateTime;
  selectValue?: AttributeOption;        // single option (FK)
  multiSelectValues?: AttributeOption[]; // M2M
}
```

Mapping rule (from `AttributeDefinition.type`):

| `type`         | Populate                | Other slots                              |
|----------------|-------------------------|------------------------------------------|
| `TEXT`         | `textValue`             | null                                     |
| `NUMBER`       | `numberValue`           | null                                     |
| `BOOLEAN`      | `booleanValue`          | null                                     |
| `SELECT`       | `selectValue`           | null                                     |
| `MULTI_SELECT` | `multiSelectValues`     | null                                     |
| `DATE`         | `dateValue`             | null                                     |
| `TIME`         | `timeValue`             | null                                     |
| `DATE_TIME`    | `dateTimeValue`         | null                                     |

The UI is responsible for choosing the correct slot — there is no server-side validation of this contract today. When switching attribute or type, **clear the other slots** before `PUT`, otherwise stale values persist.

---

## 5. Catalog

### 5.1 `Product`

Extends `TrackedTimeStamp`. The hero entity. Required: `category`, `currency`, `basePrice`, `name`.

```ts
export interface Product extends TrackedTimeStamp {
  id?: string;                     // UUID, omit on POST
  name: string;
  description?: string;
  category: Category;              // required FK, deep-loaded
  currency: Currency;              // required, ISO 4217 (see § 9.10)
  basePrice: string;               // BigDecimal
  baseSku?: string;
  status?: ProductStatus;
  tags?: Tag[];                    // many-to-many
  variants?: ProductVariant[];     // owned, cascade delete
  attributes?: ProductAttribute[]; // owned, cascade delete
  medias?: ProductMedia[];         // owned, cascade delete
}
```

- `category` is **required and embedded**. To assign a different category, change the nested object's `id` (the API needs the full object, not a bare id today).
- `variants`, `attributes`, `medias` are **owned**: include them in the `PUT` body to update them in place; omit a member to detach (orphanRemoval is on for variants/medias).
- `tags` is a join (M2M); the server upserts the relationship rows.

### 5.2 `ProductVariant`

Extends `TrackedTimeStamp`. One row per SKU-level combination (e.g. "Small Red T-Shirt").

```ts
export interface ProductVariant extends TrackedTimeStamp {
  id?: string;
  product?: Product;                       // FK back, may be omitted to avoid recursion on PUT
  sku: string;                             // unique
  variantName?: string;
  price?: string;                          // BigDecimal
  stockQuantity?: number;                  // Long, but always small enough for JS number
  weight?: string;                         // BigDecimal
  status?: ProductVariantStatus;
  medias?: ProductMedia[];                 // owned
  attributeValues?: ProductVariantAttribute[]; // owned, variant-level attrs
}
```

- `attributeValues` carries the variant's *variant-level* attribute selections (size, color, etc.). Product-level attributes live on `Product.attributes`, not here.
- `stockQuantity` is the source of truth for inventory; mutate it through orders/stock movements once those flows exist (today it is a plain editable field).

### 5.3 `ProductMedia`

```ts
export interface ProductMedia {
  id?: string;
  product?: Product;            // present when media belongs to the product
  productVariant?: ProductVariant; // present when media belongs to a variant
  url: string;
  mediaType?: ProductMediaType; // 'IMAGE' | 'VIDEO'
  altText?: string;
  caption?: string;
  sortOrder: number;            // default 0
  isPrimary: boolean;           // default false
}
```

- Media attaches to either a Product OR a ProductVariant; exactly one side should be set. The other will be null.
- `url` is treated as opaque — there is no upload endpoint in scope; provide URLs that the frontend already obtained from a separate uploader.

### 5.4 `Category`

Self-referential tree via `parentCategoryId` (not a hard FK — it is a plain column).

```ts
export interface Category {
  id?: string;
  name: string;
  description?: string;
  parentCategoryId?: string;                  // UUID of parent, null at root
  attributeDefinitions?: AttributeDefinition[]; // M2M — which attrs apply in this category
  parentCategory?: Category;                  // @Transient — populated only if the server enriches the response
  childrenCategories?: Category[];            // @Transient — same
}
```

- `parentCategory` and `childrenCategories` are `@Transient` — they are not persisted columns. Do not rely on them being populated; build the tree client-side from `parentCategoryId`.
- `attributeDefinitions` is the canonical list of attribute *types* that products in this category should expose to the editor.

### 5.5 `Tag`

```ts
export interface Tag {
  id?: string;
  name: string;
  description?: string;
}
```

Plain label entity, joined to `Product` via M2M. No relationships back.

---

## 6. Attribute system

This is the engine that lets Venzora model arbitrary product attributes (Size, Color, Material…). Read carefully — the polymorphism trips up naive forms.

### 6.1 `AttributeDefinition`

Defines an attribute type (its name, data type, options).

```ts
export interface AttributeDefinition {
  id?: string;
  name: string;                       // unique, e.g. 'Size'
  displayName?: string;               // 'Product Size'
  type?: ProductAttributeType;        // see § 6.5
  unit?: string;                      // e.g. 'cm', 'kg'
  required?: boolean;
  variantLevel?: boolean;             // true if this attr generates variants
  options?: AttributeOption[];        // owned; meaningful only for SELECT / MULTI_SELECT
}
```

- **`variantLevel`** is the most important flag:
  - `false` → attribute is set once per Product (`ProductAttribute`).
  - `true`  → attribute is set per Variant (`ProductVariantAttribute`), and combinations generate variants.

### 6.2 `AttributeOption`

A predefined value for a `SELECT` / `MULTI_SELECT` attribute.

```ts
export interface AttributeOption {
  id?: string;
  value: string;             // canonical key, e.g. 'RED'
  displayValue?: string;     // 'Ocean Red'
  sortOrder?: number;        // Long, use for ordering
  // attributeDefinition is @JsonIgnore — NOT in JSON to avoid back-ref loops
}
```

- The back-pointer `attributeDefinition` is marked `@JsonIgnore`; the server omits it from responses. The frontend always sees `AttributeOption`s nested inside their parent `AttributeDefinition.options`.

### 6.3 `ProductAttribute`

A single product-level attribute value. One row per (Product, AttributeDefinition).

```ts
export interface ProductAttribute {
  id?: string;
  product?: Product;                       // back-ref
  attributeDefinition: AttributeDefinition;
  attributeValue: AttributeValue;          // see § 4.2 polymorphism
}
```

### 6.4 `ProductVariantAttribute`

A single variant-level attribute value. One row per (ProductVariant, AttributeDefinition).

```ts
export interface ProductVariantAttribute {
  id?: string;
  variant?: ProductVariant;                // back-ref
  attributeDefinition: AttributeDefinition;
  attributeValue: AttributeValue;
}
```

### 6.5 The polymorphism — once more, explicit

When editing a `ProductAttribute` or `ProductVariantAttribute`:

1. Read `attributeDefinition.type`.
2. For `SELECT` / `MULTI_SELECT`, pull `attributeDefinition.options` for the choice list.
3. Populate only the matching slot in `attributeValue`. Clear the rest before saving.

See [§ 4.2](#42-attributevalue-polymorphic--read-carefully) for the full slot table.

---

## 7. Commerce

> **Architecture note — the checkout split.** Venzora no longer owns the payment-side of a purchase. The `vies-spring-utils` checkout module (6.2.8) ships a `CheckoutOrder` / `CheckoutTransaction` pair that handles the payment lifecycle (PayPal today). Venzora retains the **fulfillment side** of a purchase — shipping/billing address, fulfillment status, line items with FK to `ProductVariant`, totals — as `OrderFulfillment` / `OrderFulfillmentItem`. The two are linked by `OrderFulfillment.checkoutOrderId` (a plain UUID column). The old `Order`, `OrderItem`, and `Payment` entities are gone.
>
> See [`checkout.md`](checkout.md) for the worked end-to-end flow (Alice's purchase, refunds, the one-shot endpoint, the webhook listener).

### 7.1 `Cart`

Extends `TrackedTimeStampUserAccess`. One active cart per user (convention, not enforced).

```ts
export interface Cart extends TrackedTimeStampUserAccess {
  id?: string;
  userId: string;                  // UUID of the owning user
  items: CartItem[];               // owned, orphanRemoval
  totalPrice: string;              // BigDecimal, default '0'
  active: boolean;                 // default true
}
```

### 7.2 `CartItem`

Extends `TrackedTimeStamp`.

```ts
export interface CartItem extends TrackedTimeStamp {
  id?: string;
  cart?: Cart;                     // back-ref
  productVariant: ProductVariant;  // required
  quantity: number;                // Integer
  priceAtTime: string;             // BigDecimal — snapshot at add time
}
```

### 7.3 `OrderFulfillment`

Extends `TrackedTimeStampUserAccess`. The Venzora-side record of a purchase: shipping/billing addresses, fulfillment status, totals, line items. Bridges to a `CheckoutOrder` (library) via `checkoutOrderId`. Table name: `order_fulfillments`.

```ts
export interface OrderFulfillment extends TrackedTimeStampUserAccess {
  id?: string;
  orderNumber: string;             // unique, human-facing (e.g. 'VEN-2026-0001')
  userId: string;                  // UUID of the buyer
  checkoutOrderId?: string;        // UUID → CheckoutOrder.id; null until checkout completes
  items: OrderFulfillmentItem[];   // owned, orphanRemoval
  subtotal: string;                // BigDecimal
  tax: string;
  shippingCost: string;
  discountAmount: string;          // default '0'
  totalAmount: string;
  status: FulfillmentStatus;       // see § 9 — no PAYMENT_* states; payment lives on CheckoutOrder
  shippingAddress: Address;
  billingAddress: Address;
  notes?: string;
}
```

- `checkoutOrderId` is a plain UUID column (not a JPA `@ManyToOne`) — the bridge to the checkout module stays loose so cross-package entity scanning isn't required.
- Resolve `checkoutOrderId` against the checkout module's `GET /api/v1/checkout/orders/{id}` to get payment status, `approveUrl`, amounts, refund total, etc.

### 7.4 `OrderFulfillmentItem`

Extends `TrackedTimeStamp`. Line item on an `OrderFulfillment`. Preserves the FK to `ProductVariant` so inventory and returns can target a specific SKU.

```ts
export interface OrderFulfillmentItem extends TrackedTimeStamp {
  id?: string;
  orderFulfillment?: OrderFulfillment;
  productVariant: ProductVariant;   // required FK — preserved for inventory & returns
  quantity: number;                 // Integer
  unitPrice: string;                // BigDecimal
  totalPrice: string;
  lineItemSku?: string;             // mirrors CheckoutLineItem.sku on the linked CheckoutOrder
  productSnapshot?: string;         // JSON string — parse with JSON.parse if you need it
}
```

### 7.5 `Discount`

Extends `TrackedTimeStamp`. Coupon codes & promotions.

```ts
export interface Discount extends TrackedTimeStamp {
  id?: string;
  code: string;                    // unique
  description?: string;
  discountType: DiscountType;
  discountValue: string;           // BigDecimal — percentage or fixed amount per discountType
  minimumOrderAmount?: string;
  maximumDiscountAmount?: string;
  validFrom: DateTime;             // see § 3.4
  validTo: DateTime;
  maxUses?: number;                // Integer; null = unlimited
  currentUses: number;             // default 0
  active: boolean;                 // default true
}
```

### 7.6 *(Payment — removed)*

Venzora's `Payment` entity has been deleted. Payment records now live in the checkout module as `CheckoutTransaction` (kinds: `AUTHORIZE | CAPTURE | REFUND | VOID | RENEWAL_CHARGE | CHARGEBACK | DISPUTE`). Query via `CheckoutTransactionDao` server-side, or hit the checkout REST endpoints (see [library docs](#checkout)) from the frontend.

### 7.7 `Shipment`

Extends `TrackedTimeStamp`. Now references `OrderFulfillment` (renamed from `order`).

```ts
export interface Shipment extends TrackedTimeStamp {
  id?: string;
  orderFulfillment: OrderFulfillment;    // required FK
  trackingNumber: string;                // unique
  carrier: string;                       // 'UPS' | 'FedEx' | etc — free text
  status: ShipmentStatus;
  estimatedDeliveryDate: DateTime;       // required
  actualDeliveryDate: DateTime;          // required (note: also non-nullable in DB)
  notes?: string;
  trackingUrl?: string;
}
```

### 7.8 `ReturnRequest`

Extends `TrackedTimeStampUserAccess`. RMA workflow. Now points at `OrderFulfillment` / `OrderFulfillmentItem`.

```ts
export interface ReturnRequest extends TrackedTimeStampUserAccess {
  id?: string;
  returnNumber: string;                       // unique, human-facing
  orderFulfillment: OrderFulfillment;         // required FK
  orderFulfillmentItem: OrderFulfillmentItem; // required FK — which line item is being returned
  userId: string;                             // UUID
  status: ReturnStatus;
  reason: string;
  adminNotes?: string;
  returnQuantity: number;                     // Integer
  refundAmount: string;                       // BigDecimal — informational; the real refund moves through CheckoutOrder.refundOrder
  trackingNumber?: string;
  refundShipping: boolean;                    // default false
}
```

### 7.9 `StockMovement`

Extends `TrackedTimeStamp`. Audit log of inventory changes. Read-only from the UI's perspective unless you're building an admin adjustment screen.

```ts
export interface StockMovement extends TrackedTimeStamp {
  id?: string;
  productVariant: ProductVariant;  // required FK
  movementType: StockMovementType;
  quantityChange: number;          // Long — positive for additions, negative for reductions
  quantityAfter: number;           // Long — denormalized running total
  reason?: string;
  reference?: string;              // free-form, e.g. order id
  userId?: string;                 // UUID — who performed it
}
```

---

## 8. User-facing & social

### 8.1 `UserInfo`

Extends `TrackedTimeStamp`. Profile data keyed by the auth-system user id.

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

- `userId` is both the entity's `@Id` and the link to the auth user. There is no `id` field — use `userId`.

### 8.2 `UserAddress`

Extends `TrackedTimeStamp`. Bag of addresses per user.

```ts
export interface UserAddress extends TrackedTimeStamp {
  userId: string;                  // UUID — primary key
  addresses: Address[];            // Set<Address> — order not guaranteed
}
```

### 8.3 `WishProduct`

Extends `TrackedTimeStampUserAccess`. A user's wishlist entry.

```ts
export interface WishProduct extends TrackedTimeStampUserAccess {
  id?: string;
  productId: string;               // UUID
  quantity: number;                // Long — desired quantity
}
```

### 8.4 `Review`

Extends `TrackedTimeStamp`.

```ts
export interface Review extends TrackedTimeStamp {
  id?: string;
  userId: string;                  // UUID
  productId: string;               // UUID
  comment?: string;
  rating: string;                  // BigDecimal, e.g. '4.5'
}
```

---

## 9. Enums

All enums serialize as their `name()` string (e.g. `'ACTIVE'`). Define them as TypeScript string literal unions or `as const` arrays so you can iterate them in dropdowns.

### 9.1 `ProductStatus`
`'DRAFT' | 'ACTIVE' | 'INACTIVE' | 'DISCONTINUED'`

### 9.2 `ProductVariantStatus`
`'ACTIVE' | 'INACTIVE' | 'OUT_OF_STOCK'`

### 9.3 `ProductAttributeType`
`'TEXT' | 'NUMBER' | 'BOOLEAN' | 'SELECT' | 'MULTI_SELECT' | 'DATE' | 'TIME' | 'DATE_TIME'`

### 9.4 `ProductMediaType`
`'IMAGE' | 'VIDEO'`

### 9.5 `FulfillmentStatus`

Replaces the old `OrderStatus`. Payment-side states (`PAYMENT_PENDING`, `PAYMENT_CONFIRMED`) are gone — those live on `CheckoutOrder.status` in the checkout module.

`'PENDING' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED' | 'RETURNED' | 'REFUNDED' | 'PARTIALLY_REFUNDED' | 'FAILED'`

### 9.6 *(PaymentMethodType — removed)*

Venzora's enum is gone. The active payment method lives on the checkout module's `CheckoutOrder` (provider = `"paypal"`).

### 9.7 `DiscountType`
`'PERCENTAGE' | 'FIXED_AMOUNT' | 'FREE_SHIPPING' | 'BUY_X_GET_Y'`

### 9.8 `ShipmentStatus`
`'PENDING' | 'PROCESSING' | 'PICKED' | 'PACKED' | 'SHIPPED' | 'IN_TRANSIT' | 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'FAILED' | 'RETURNED'`

### 9.9 `ReturnStatus`
`'REQUESTED' | 'APPROVED' | 'REJECTED' | 'SHIPPED' | 'RECEIVED' | 'INSPECTING' | 'REFUNDED' | 'REPLACED' | 'CANCELLED'`

### 9.10 `StockMovementType`
`'PURCHASE' | 'SALE' | 'ADJUSTMENT' | 'RETURN' | 'DAMAGE' | 'TRANSFER' | 'RESERVED' | 'UNRESERVED'`

### 9.11 `AddressType`
`'BILLING' | 'SHIPPING'`

### 9.12 `Currency`

ISO 4217 — 173 values. Render with a searchable combobox. The set matches the standard list (`AED`, `AFN`, `ALL`, …, `ZAR`, `ZMW`, `ZWL`). The frontend can ship the list as a static constant; ask the backend for the canonical enum order if needed.

```ts
export type Currency =
  | 'AED' | 'AFN' | 'ALL' | 'AMD' | 'ANG' | 'AOA' | 'ARS' | 'AUD' | 'AWG' | 'AZN'
  // … 173 codes total
  | 'ZAR' | 'ZMW' | 'ZWL';
```

---

## 10. Suggested file layout for the Angular project

A workable starting point — adapt to your conventions:

```
src/app/models/
  base.ts             // TrackedTimeStamp, TrackedTimeStampUserAccess, UserAccess, DateTime/Date/Time
  enums.ts            // every enum from § 9
  address.ts          // Address, AddressType
  attribute.ts        // AttributeDefinition, AttributeOption, AttributeValue,
                      //   ProductAttribute, ProductVariantAttribute
  product.ts          // Product, ProductVariant, ProductMedia, Category, Tag
  commerce.ts         // Cart, CartItem, OrderFulfillment, OrderFulfillmentItem,
                      //   Discount, Shipment, ReturnRequest, StockMovement
  checkout.ts         // CheckoutOrder, CheckoutLineItem, CheckoutTransaction
                      //   (from the vies-spring-utils checkout module)
  user.ts             // UserInfo, UserAddress, WishProduct, Review
```

Each module should re-export from a single `index.ts` so consumers can `import { Product, ProductVariant } from '@app/models'`.

---

## 11. Practical gotchas

1. **Bidirectional EAGER relationships can create JSON cycles.** Today, only `AttributeOption.attributeDefinition` is marked `@JsonIgnore`. Other back-refs (`ProductVariant.product`, `CartItem.cart`, `OrderFulfillmentItem.orderFulfillment`, `Shipment.orderFulfillment`) are serialized. If you receive a payload that looks unexpectedly deep or repeats objects, flag it to the backend — the fix may be `@JsonManagedReference`/`@JsonBackReference`.
2. **`BigDecimal` precision.** Spring/Jackson defaults to emitting BigDecimal as a JSON number; we treat it as `string` here to be safe across JS floats. Confirm the wire format with one round-trip before committing your TS type — switch to `number` only if you measure consistent precision.
3. **`DateTime` is not ISO 8601.** It is a structured object. Build the conversion utility once; do not write ad-hoc parsers everywhere.
4. **UUIDv7 is sortable.** You can sort lists by `id` and get rough creation order for free, which is sometimes faster than reading `createdAt`.
5. **`productSnapshot` on `OrderFulfillmentItem` is a JSON string**, not a nested object. `JSON.parse` it if you need the historical product data.
6. **Categories form a tree by convention only.** `parentCategoryId` is a plain column with no FK constraint. Validate orphan/cycle prevention client-side until the backend adds it.
7. **No DTOs today.** Requests and responses use the entity types directly. This means every relationship is included in the payload. As payloads grow, ask the backend to introduce request/response DTOs.
