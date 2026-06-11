# Venzora — REST API Reference

> A reference for every REST controller in the Venzora backend. The shape of every entity is in [`models.md`](models.md); the product intent is in [`frontend-intent.md`](frontend-intent.md). This document covers **the wire**: URLs, verbs, headers, query parameters, and the access gate for every endpoint.

---

## 1. How the API is shaped

Every controller in this project is generated from one of two base classes in `vies-spring-utils`. You do not need to read the framework code — this section captures the contract every controller honors.

### 1.1 Two flavors of controller

| Flavor                            | Base class                              | Used for                                                            |
|-----------------------------------|-----------------------------------------|---------------------------------------------------------------------|
| **Admin-gated**                   | `ViesAutoAdminCheckController`          | Catalog management, fulfillment, schema, all "back office" entities.|
| **User-scoped**                   | `ViesControllerWithUserAccess`          | Per-user data (cart, order history, wishlist, returns).             |

- **Admin-gated**: the request user must have admin permission. Non-admins receive a 403.
- **User-scoped**: rows are filtered by the authenticated user's id. List endpoints only return rows the user owns or has been shared with; create endpoints auto-stamp `ownerUserId` on the new row.

### 1.2 Shared headers

| Header             | Required | Notes                                                                                  |
|--------------------|----------|----------------------------------------------------------------------------------------|
| `Authorization`    | Yes (where auth applies) | `Bearer <jwt>`. The JWT identifies the user.                                |
| `user_id`          | No       | Optional override sent by upstream gateways. Trust the JWT in normal frontend traffic. |
| `Content-Type`     | Yes (write) | `application/json` for POST/PUT/PATCH.                                              |
| `Accept`           | Optional | `application/json`. Default is JSON.                                                  |

### 1.3 The seven endpoints every controller exposes

Every controller below honors the same seven endpoints, mounted under its own `@RequestMapping` base path. We list them once here and refer to them by short name (`GET /`, `GET /{id}`, etc.) in the per-controller sections.

#### `GET /` — list

```
GET {base}?page={n}&size={n}&{entity-field}={value}&...
```

- **Pagination**: `page` (0-indexed) and `size`. Defaults are framework-defined (10–20 rows is a safe assumption; confirm by inspection).
- **Filtering**: any field of the entity may be passed as a query parameter to filter exact matches (e.g. `?status=ACTIVE`). Nested fields are not supported here — use [`POST /matches`](#post-matches--complex-match) instead.
- **Matcher & match-by enums**: two optional query parameters (`propertyMatcher`, `matchBy`) control how filtering operates (exact / contains / starts-with). See [§ 1.5](#15-matcher--match-by-enums).
- **Response**: `{ content: [...], page, size, totalElements, totalPages, ... }` (a `PageResponse`).

#### `GET /{id}` — single record

```
GET {base}/{id}
```

- `id` is the entity's UUID. Returns 404 if not found.

#### `POST /matches` — complex match

```
POST {base}/matches?page={n}&size={n}&propertyMatcher={...}&matchBy={...}
Content-Type: application/json

{ ...entity-shaped filter... }
```

- Send a partial entity in the body as a filter (e.g. `{ "status": "ACTIVE", "category": { "name": "Shoes" } }`). The server matches rows against the filled fields.
- Use this when you need to filter on nested objects or multiple fields that don't fit cleanly into a query string.

#### `POST /` — create

```
POST {base}
Content-Type: application/json

{ ...entity without id... }
```

- **Never send `id`** — UUIDv7 is generated server-side.
- For user-scoped controllers, `ownerUserId` is stamped automatically from the JWT — do not set it.
- Returns the created entity (with `id`, `createdAt`, etc.).

#### `PUT /{id}` — full replace

```
PUT {base}/{id}
Content-Type: application/json

{ ...full entity... }
```

- Replaces the row's mutable fields. Omitted fields may be cleared — pass the full entity.
- For owned children (variants, attributes, media on a Product), include them in the body to update in place; omit one to detach (where `orphanRemoval = true`).

#### `PATCH /{id}` — partial update

```
PATCH {base}/{id}
Content-Type: application/json

{ ...only the fields to change... }
```

- Only the keys present in the body are updated. Safer than PUT for partial edits.

#### `DELETE /{id}` — delete

```
DELETE {base}/{id}
```

- Returns 204 on success. Cascade deletes propagate per the entity's JPA config (deleting a Product wipes its variants/attributes/media).

### 1.4 Pagination response shape (`PageResponse<T>`)

```ts
export interface PageResponse<T> {
  content: T[];
  page: number;           // current page index, 0-based
  size: number;           // requested page size
  totalElements: number;  // total rows matching the filter
  totalPages: number;
  // additional pagination metadata may be present; treat unknown keys gracefully
}
```

When a list endpoint returns an array directly (no pagination wrapper), it has been called without `page`/`size`. Always pass pagination on list views — unbounded lists scale poorly.

### 1.5 Matcher & match-by enums

Two optional query parameters control how `GET /` and `POST /matches` filter rows. Names are framework-defined; consult `vies-spring-utils` for the canonical set if you need an exact list. Common values:

- `propertyMatcher`: `EXACT`, `CONTAINS`, `STARTS_WITH`, `ENDS_WITH`, `IGNORE_CASE`.
- `matchBy`: `ALL` (AND across fields), `ANY` (OR across fields).

For most frontend use, the defaults work. Reach for these when building admin search.

### 1.6 Errors

The server returns standard HTTP status codes:

| Status | Meaning                                           |
|--------|---------------------------------------------------|
| 200    | OK — read / write success                          |
| 201    | Created (POST may also return 200 — handle both)   |
| 204    | No content (DELETE success)                        |
| 400    | Validation error — see body for field errors      |
| 401    | Missing or invalid JWT                             |
| 403    | Authenticated but not authorized (admin-gated)    |
| 404    | Entity not found                                   |
| 409    | Conflict (unique violation, optimistic lock)      |
| 500    | Server error                                       |

Error bodies are framework-shaped; treat them defensively. Build one error-mapping service in the Angular app.

### 1.7 Identifier convention

All entity IDs are **UUIDv7** strings (sortable by time). Path variables expect canonical 36-char form (`018f0c8a-...`). Do not send numeric ids; the schema has been migrated off `Long`.

---

## 2. Endpoint map

22 controllers, 3 packages. Quick scan:

### 2.1 Catalog & schema (admin-gated)

| Controller | Base URL | Entity | Source |
|---|---|---|---|
| `ProductController` | `/api/v1/products` | [`Product`](models.md#51-product) | [src](../src/main/java/com/viescloud/llc/venzora/controller/product/ProductController.java) |
| `ProductVariantController` | `/api/v1/product/variants` | [`ProductVariant`](models.md#52-productvariant) | [src](../src/main/java/com/viescloud/llc/venzora/controller/product/ProductVariantController.java) |
| `ProductMediaController` | `/api/v1/product/medias` | [`ProductMedia`](models.md#53-productmedia) | [src](../src/main/java/com/viescloud/llc/venzora/controller/product/ProductMediaController.java) |
| `CategoryController` | `/api/v1/categories` | [`Category`](models.md#54-category) | [src](../src/main/java/com/viescloud/llc/venzora/controller/product/CategoryController.java) |
| `TagController` | `/api/v1/tags` | [`Tag`](models.md#55-tag) | [src](../src/main/java/com/viescloud/llc/venzora/controller/product/TagController.java) |
| `AttributeDefinitionController` | `/api/v1/product/attribute/definitions` | [`AttributeDefinition`](models.md#61-attributedefinition) | [src](../src/main/java/com/viescloud/llc/venzora/controller/product/AttributeDefinitionController.java) |
| `AttributeOptionController` | `/api/v1/product/attribute/options` | [`AttributeOption`](models.md#62-attributeoption) | [src](../src/main/java/com/viescloud/llc/venzora/controller/product/AttributeOptionController.java) |
| `ProductAttributeController` | `/api/v1/product/attributes` | [`ProductAttribute`](models.md#63-productattribute) | [src](../src/main/java/com/viescloud/llc/venzora/controller/product/ProductAttributeController.java) |
| `ProductVariantAttributeController` | `/api/v1/product/variant/attributes` | [`ProductVariantAttribute`](models.md#64-productvariantattribute) | [src](../src/main/java/com/viescloud/llc/venzora/controller/product/ProductVariantAttributeController.java) |

### 2.2 Commerce

| Controller | Base URL | Entity | Gate |
|---|---|---|---|
| `CartController` | `/api/v1/carts` | [`Cart`](models.md#71-cart) | **User-scoped** |
| `CartItemController` | `/api/v1/cart/items` | [`CartItem`](models.md#72-cartitem) | Admin |
| `OrderController` | `/api/v1/orders` | [`Order`](models.md#73-order) | **User-scoped** |
| `OrderItemController` | `/api/v1/order/items` | [`OrderItem`](models.md#74-orderitem) | Admin |
| `DiscountController` | `/api/v1/discounts` | [`Discount`](models.md#75-discount) | Admin |
| `PaymentController` | `/api/v1/payments` | [`Payment`](models.md#76-payment) | Admin |
| `ShipmentController` | `/api/v1/shipments` | [`Shipment`](models.md#77-shipment) | Admin |
| `ReturnRequestController` | `/api/v1/returns` | [`ReturnRequest`](models.md#78-returnrequest) | **User-scoped** |
| `StockMovementController` | `/api/v1/stock/movements` | [`StockMovement`](models.md#79-stockmovement) | Admin |

### 2.3 Social & user

| Controller | Base URL | Entity | Gate |
|---|---|---|---|
| `ReviewController` | `/api/v1/reviews` | [`Review`](models.md#84-review) | Admin |
| `WishProductController` | `/api/v1/wishlists` | [`WishProduct`](models.md#83-wishproduct) | **User-scoped** |
| `UserInfoController` | `/api/v1/user/infos` | [`UserInfo`](models.md#81-userinfo) | Admin |
| `UserAddressController` | `/api/v1/user/addresses` | [`UserAddress`](models.md#82-useraddress) | Admin |

---

## 3. Controllers in detail

Every controller below exposes the [seven standard endpoints from § 1.3](#13-the-seven-endpoints-every-controller-exposes). The per-controller sections call out anything that deviates, plus practical guidance for hitting the endpoint.

### 3.1 Catalog & schema

#### 3.1.1 `ProductController` — `/api/v1/products`

- Entity: [`Product`](models.md#51-product). Service: `ProductService`.
- Gate: admin.
- Cascade behavior matters here. A `DELETE /api/v1/products/{id}` wipes the product's variants, attributes, and media. Confirm with the user.
- On `POST` / `PUT`, the body may carry the full graph — variants, attributes, medias — because `cascade = ALL`. Use `PATCH` for narrow edits to avoid resending the whole tree.

#### 3.1.2 `ProductVariantController` — `/api/v1/product/variants`

- Entity: [`ProductVariant`](models.md#52-productvariant). Service: `ProductVariantService`.
- Gate: admin.
- `sku` is unique — a duplicate POST yields a 409.
- For the storefront variant picker, you typically read variants through the parent `Product`'s `variants` array, not this endpoint. This endpoint is most useful for admin per-variant edits (price, stock, status).

#### 3.1.3 `ProductMediaController` — `/api/v1/product/medias`

- Entity: [`ProductMedia`](models.md#53-productmedia). Service: `ProductMediaService`.
- Gate: admin.
- Exactly one of `product` or `productVariant` must be set per row — the server does not enforce this today, but the UI should.
- `isPrimary` is not enforced as unique-per-parent. The UI should clear other primaries when toggling one on.

#### 3.1.4 `CategoryController` — `/api/v1/categories`

- Entity: [`Category`](models.md#54-category). Service: `CategoryService`.
- Gate: admin.
- The tree is implied by `parentCategoryId` (a plain column, no FK). The UI must guard against cycles and orphans client-side.
- `attributeDefinitions` is a M2M — POST/PUT it to set which attributes apply in this category.

#### 3.1.5 `TagController` — `/api/v1/tags`

- Entity: [`Tag`](models.md#55-tag). Service: `TagService`.
- Gate: admin. Plain CRUD.

#### 3.1.6 `AttributeDefinitionController` — `/api/v1/product/attribute/definitions`

- Entity: [`AttributeDefinition`](models.md#61-attributedefinition). Service: `AttributeDefinitionService`.
- Gate: admin.
- `name` is unique. Owned `options` (for SELECT / MULTI_SELECT types) cascade-save with the definition — POST/PUT the parent with the options array nested.

#### 3.1.7 `AttributeOptionController` — `/api/v1/product/attribute/options`

- Entity: [`AttributeOption`](models.md#62-attributeoption). Service: `AttributeOptionService`.
- Gate: admin.
- The back-pointer `attributeDefinition` is `@JsonIgnore` (won't appear in the response).
- Prefer managing options as nested arrays on `AttributeDefinition`; this endpoint exists for surgical edits.

#### 3.1.8 `ProductAttributeController` — `/api/v1/product/attributes`

- Entity: [`ProductAttribute`](models.md#63-productattribute). Service: `ProductAttributeService`.
- Gate: admin.
- This is a *product-level* attribute value (one per Product per AttributeDefinition). For variant-level values, see § 3.1.9.
- The polymorphic `attributeValue` block: only fill the slot matching the linked `attributeDefinition.type`. See [`AttributeValue` in models.md](models.md#42-attributevalue-polymorphic--read-carefully).

#### 3.1.9 `ProductVariantAttributeController` — `/api/v1/product/variant/attributes`

- Entity: [`ProductVariantAttribute`](models.md#64-productvariantattribute). Service: `ProductVariantAttributeService`.
- Gate: admin.
- Same polymorphism rules as above, scoped to a single variant.

### 3.2 Commerce

#### 3.2.1 `CartController` — `/api/v1/carts`

- Entity: [`Cart`](models.md#71-cart). Service: `CartService`.
- Gate: **user-scoped**. The list endpoint only returns carts the JWT user owns or has been shared with.
- `userId` and `ownerUserId` are server-stamped on POST — do not set them in the body.
- `items` (CartItem) are owned with orphan removal; PUT the full items array to add/remove rows in one request.

#### 3.2.2 `CartItemController` — `/api/v1/cart/items`

- Entity: [`CartItem`](models.md#72-cartitem). Service: `CartItemService`.
- Gate: admin.
- Prefer managing items through the parent `Cart`. This endpoint exists for granular debugging; consumer UIs should not call it directly.

#### 3.2.3 `OrderController` — `/api/v1/orders`

- Entity: [`Order`](models.md#73-order). Service: `OrderService`.
- Gate: **user-scoped**. Shoppers see their own orders; admins (with their permission set) see assigned/shared rows.
- `orderNumber` is unique — the server should generate it; if the UI sets it, it must be guaranteed unique.
- `items`, `shippingAddress`, `billingAddress` are owned/embedded — send them in the create body.

#### 3.2.4 `OrderItemController` — `/api/v1/order/items`

- Entity: [`OrderItem`](models.md#74-orderitem). Service: `OrderItemService`.
- Gate: admin.
- `productSnapshot` is a JSON string — set it server-side at order-creation time so historical reads survive product edits.
- Prefer managing items through the parent `Order`.

#### 3.2.5 `DiscountController` — `/api/v1/discounts`

- Entity: [`Discount`](models.md#75-discount). Service: `DiscountService`.
- Gate: admin.
- `code` is unique. The storefront checkout reads this controller (via admin token, today) to validate a code; future work should add a public validation endpoint.
- `currentUses` is denormalized — increment server-side, never from the UI.

#### 3.2.6 `PaymentController` — `/api/v1/payments`

- Entity: [`Payment`](models.md#76-payment). Service: `PaymentService`.
- Gate: admin.
- This is a CRUD over the `Payment` record — it does **not** charge a card. Real processor integration is out of scope until added.
- `paymentStatus` is free-form text today; pick a convention server-side and document it before the UI codes against specific values.

#### 3.2.7 `ShipmentController` — `/api/v1/shipments`

- Entity: [`Shipment`](models.md#77-shipment). Service: `ShipmentService`.
- Gate: admin.
- `trackingNumber` is unique. Note that `estimatedDeliveryDate` and `actualDeliveryDate` are both non-nullable in the model — the UI must supply both at create time (workaround until the model is relaxed).

#### 3.2.8 `ReturnRequestController` — `/api/v1/returns`

- Entity: [`ReturnRequest`](models.md#78-returnrequest). Service: `ReturnRequestService`.
- Gate: **user-scoped**. Shoppers create and view their own; admins act on the queue.
- `returnNumber` is unique. Generate it server-side when wiring the real RMA flow.
- Status transitions are not enforced today — the UI should guard the lifecycle.

#### 3.2.9 `StockMovementController` — `/api/v1/stock/movements`

- Entity: [`StockMovement`](models.md#79-stockmovement). Service: `StockMovementService`.
- Gate: admin.
- This is an audit log — treat it as append-only from the UI's perspective. Adjustments create new rows; never PATCH `quantityAfter` directly.

### 3.3 Social & user

#### 3.3.1 `ReviewController` — `/api/v1/reviews`

- Entity: [`Review`](models.md#84-review). Service: `ReviewService`.
- Gate: **admin today**. `Review` does not extend `UserAccess`, so it cannot use the user-scoped controller. To let shoppers write/edit their own reviews, either:
  - Migrate `Review` to extend `TrackedTimeStampUserAccess` (model + DB change), then switch the controller to `ViesControllerWithUserAccess`, or
  - Add a custom controller that filters `userId` against the JWT.
- Until then, the storefront's "write a review" flow needs a backend change.

#### 3.3.2 `WishProductController` — `/api/v1/wishlists`

- Entity: [`WishProduct`](models.md#83-wishproduct). Service: `WishProductService`.
- Gate: **user-scoped**. Shoppers manage their own list.
- `productId` is a plain UUID column (not a FK relationship); the UI must resolve it against the product catalog to render.

#### 3.3.3 `UserInfoController` — `/api/v1/user/infos`

- Entity: [`UserInfo`](models.md#81-userinfo). Service: `UserInfoService`.
- Gate: **admin today** — see caveat. The entity is keyed on `userId` (the auth user id), so the `{id}` path variable is the user's UUID, not a separate identifier.
- This should arguably be user-scoped (a user editing their own profile). Add a custom user-scoped controller when the account flow is built; today only admins can mutate.

#### 3.3.4 `UserAddressController` — `/api/v1/user/addresses`

- Entity: [`UserAddress`](models.md#82-useraddress). Service: `UserAddressService`.
- Gate: **admin today** — same caveat as `UserInfoController`. The `{id}` path variable is the user's UUID.
- `addresses` is a `Set<Address>` (embedded). To add/remove one, PUT the whole row with the full updated set.

---

## 4. Practical notes for the Angular client

### 4.1 Suggested HTTP service layout

One service per entity, all extending a small generic base:

```ts
// core/http/crud.service.ts
export abstract class CrudService<T> {
  protected abstract baseUrl: string;
  constructor(protected http: HttpClient) {}

  list(params?: ListParams): Observable<PageResponse<T>> { /* GET / */ }
  getById(id: string): Observable<T>                      { /* GET /{id} */ }
  matches(filter: Partial<T>, params?: ListParams): Observable<PageResponse<T>> { /* POST /matches */ }
  create(body: T): Observable<T>                          { /* POST / */ }
  update(id: string, body: T): Observable<T>              { /* PUT /{id} */ }
  patch(id: string, body: Partial<T>): Observable<T>      { /* PATCH /{id} */ }
  delete(id: string): Observable<void>                    { /* DELETE /{id} */ }
}

// e.g. catalog/product.service.ts
@Injectable({ providedIn: 'root' })
export class ProductApi extends CrudService<Product> {
  protected baseUrl = '/api/v1/products';
}
```

Build it once. Every entity becomes a 5-line subclass.

### 4.2 JWT interceptor

```ts
// core/auth/jwt.interceptor.ts
intercept(req: HttpRequest<any>, next: HttpHandler) {
  const token = this.auth.token();
  if (!token) return next.handle(req);
  return next.handle(req.clone({
    setHeaders: { Authorization: `Bearer ${token}` },
  }));
}
```

Pair with a second interceptor that catches 401, clears the token, and redirects to `/auth/login`.

### 4.3 Error mapper

One service. Map the framework's error body to a `{ field?: string, message: string }[]` and surface in forms or as a toast.

### 4.4 PATCH vs PUT

Default to **PATCH** for narrow edits — it avoids accidentally clearing fields you forgot to include in the body. Use **PUT** when you genuinely want to replace the row (e.g. editing the full product graph including its variants).

### 4.5 Avoid relying on numeric ids

Every id is a UUID string. Do not parse to number. Do not compare with `===` against numeric literals.

### 4.6 List endpoints are not unbounded

Always pass `page` and `size`. Build a `<paged-table>` that exposes both. The framework returns a `PageResponse<T>` — wire `totalElements` into the pager.

---

## 5. Endpoints intentionally not implemented (yet)

These exist in the design but have **no controller** today. Track them as backend coordination items, not as latent bugs:

- **Public catalog read.** `ProductController` is admin-gated. Until a public read endpoint is added, the storefront cannot browse the catalog without an admin token.
- **Variant generator.** `POST /api/v1/products/{id}/generate-variants` — accepts a cartesian-product spec and creates variants. Today the UI must loop and POST individually.
- **Discount validation.** `POST /api/v1/discounts/validate` — accepts a `code` and a cart, returns the resolved discount or a reason it doesn't apply. Today the UI must replicate the validation rules client-side.
- **Order placement.** Today `POST /api/v1/orders` is generic CRUD. A real checkout endpoint (`POST /api/v1/orders/checkout`) would atomically: validate cart, lock stock, apply discount, create order, create payment intent. This is the single most important commerce endpoint to add.
- **Public review write.** See [§ 3.3.1](#331-reviewcontroller--apiv1reviews).
- **User self-service.** `UserInfo` and `UserAddress` are admin-gated; shoppers cannot edit their own profile/addresses today.
- **Payment processor integration.** `PaymentController` is CRUD over a row, not a card-charging endpoint.
- **Search.** No full-text product search endpoint. Use `POST /matches` with `propertyMatcher=CONTAINS` as a stopgap.
- **Media upload.** `ProductMedia.url` is opaque; no upload endpoint exists. Bring your own uploader.

---

## 6. Quick reference card

For frontend devs scanning fast:

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
DateTime     Structured object, NOT ISO 8601. See models.md § 3.4.
Errors       400 = validation, 401 = no/bad JWT, 403 = not admin, 404 = missing, 409 = unique violation.
```
