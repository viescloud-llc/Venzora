# Venzora Client Frontend — Intent

> A brief for designing and building the **Venzora Client** — the public-facing storefront where shoppers browse, add to cart, check out, track orders, and leave reviews. The internal admin tool is a separate codebase — see [`frontend-manager.md`](frontend-manager.md). For data shapes and REST endpoints, the source of truth is [`api.md`](api.md). For end-to-end checkout flow, see [`checkout.md`](checkout.md).

---

## 1. What this frontend is for

The Venzora Client is the **store**. It's what a customer lands on when they want to buy something. The bar is competitive UX: this isn't a backoffice tool, it's the surface that decides whether a real human completes a purchase.

Audiences:

- **Anonymous visitors** — browsing the catalog, reading product detail, building a cart.
- **Logged-in shoppers** — same as above, plus checkout, order history, returns, wishlist, reviews, account settings.

There is no admin surface here. Anyone who needs to *manage* the store uses the Manager frontend.

---

## 2. Tech stack & conventions

- **Angular** (latest stable). Standalone components, modern signals / control-flow syntax.
- **TypeScript** strict mode. All data types come from [`api.md`](api.md).
- **State** — Service + signals. NgRx only if the cart/checkout flow grows to need it.
- **Forms** — Reactive Forms.
- **Routing** — feature-routed; **SSR / static prerender** for catalog and product pages (SEO matters).
- **HTTP** — typed `HttpClient` services per resource. Use the `CrudService<T>` blueprint in [`api.md` § 11](api.md#11-http-service-blueprint).
- **i18n / currency** — every monetary string carries an explicit ISO currency code. One `<money>` component.
- **Theming** — pick a component library that doesn't look like a backoffice (Angular Material is fine; Tailwind + headless components also reasonable).

---

## 3. Backend at a glance

- **Stack**: Spring Boot 3.3.4, Java 17, JPA/Hibernate, H2 (local) / MySQL (prod). Default local port `8085`, base path `/api/v1`. Full conventions in [`api.md` § 1](api.md#1-conventions).
- **IDs**: UUIDv7 (sortable, time-ordered). Never send `id` on `POST`.
- **JWT auth.** The storefront experience is **partially anonymous** — public catalog browsing should work without a token, while cart/order/wishlist endpoints require one.
- **Eager loading everywhere** — responses are deep trees. Plan list views accordingly.

> **Catalog browsing**: anonymous shoppers hit `/api/v1/public/products/*` — see [`api.md` § 8.4](api.md#84-storefront-read-api-public-no-auth). No auth required; returns only `ACTIVE` products. Faceted attribute filtering is server-side; the frontend renders filter UI dynamically from `GET /api/v1/public/products/filters?categoryId=...`. The admin-gated `/api/v1/products` endpoint is for the Manager and should not be called from the client.

---

## 4. Information architecture

A single shell, lazy-loaded feature modules:

```
/                              home / featured
/shop                          catalog browse, filters
/shop/c/:slug                  category landing
/shop/p/:slug                  product detail
/search?q=...                  search results
/cart                          cart review (works anonymously)
/checkout                      multi-step wizard
/account                       logged-in shopper area
├── /orders                    history, detail, returns
├── /wishlist
├── /reviews                   my reviews
├── /addresses
└── /profile
/auth/{login,signup,recover}
```

All `/account/*` routes are auth-gated. `/cart` and `/checkout` work anonymously up to the point of placing an order; the auth wall is at "Place order" in checkout.

---

## 5. Core flows (ranked by importance)

The first three define whether the store works. The rest can be functional.

### 5.1 Catalog & search

- **Filter discovery (once per session, cached server-side)**:
  - Call `GET /api/v1/public/products/filter-map` at app startup. Returns the full, self-describing filter catalog with `kind` hints (`TEXT_SEARCH`, `SINGLE_SELECT`, `MULTI_SELECT`, `RANGE_PRICE`, etc.) so the frontend renders one input control per entry — no hardcoded "Size / Color / Material" — and re-renders automatically when admins add new attributes (cache refreshes every 60s server-side). See [`api.md` § 8.4.4](api.md#844-full-filter-map-cached--get-apiv1publicproductsfilter-map).
- **Category landing** (`/shop/c/:slug`):
  1. Apply the filter map (already in memory from app startup) to render the filter panel. Optionally call `GET /public/products/filters?categoryId=<X>` for a narrower per-category view of attributes attached to the category.
  2. Call `POST /api/v1/public/products/search` with a `{ categoryId, page: 0, size: 20 }` body for the initial product grid.
- **Use POST for list** (`POST /api/v1/public/products/search`). The frontend collects current filter selections into a `ProductListRequest` JSON body and posts. No URL-length limits, typed fields for every dimension. The GET equivalent (`GET /public/products?...`) is still available for bookmarkable URLs and quick curl tests but isn't the primary path.
- **Faceted filters** — each filter spec from the map carries the `key` to use. The `attribute.<Name>` keys go into the POST body under `attributes: { "<Name>": [values] }`. For `RANGE_PRICE`, use the `ranges[currency]` bounds to drive the slider, then submit `minPrice` / `maxPrice` as top-level fields. Example body when the user picks Size: Small + Color: Red+Blue:
  ```json
  {
    "categoryId": "...",
    "attributes": { "Size": ["Small"], "Color": ["Red", "Blue"] },
    "page": 0, "size": 20
  }
  ```
- **Search** (`/search?q=`) — passes `q=` to the same endpoint. Server-side substring match against name + description today. Real full-text is on the backlog.
- **Pagination** — every list response is a `PageResponse<Product>` (`{ content, metadata: { pageNumber, pageSize, totalElements, totalPage } }`); render with the framework's pager.
- **Listing card** — primary `ProductMedia` (from `product.medias` where `isPrimary=true`), name, currency-formatted price (`basePrice` + `currency`), category, badge for `OUT_OF_STOCK` or "NEW".

### 5.2 Product detail *(the hero page)*

Hero, in order:

1. **Gallery** — `ProductMedia` for the product, plus per-variant media when a variant is selected. Respect `isPrimary` and `sortOrder`.
2. **Identity** — name, category breadcrumb, average rating (derived from `Review`), short description.
3. **Variant picker** — enumerate the attributes that appear on the product's `ProductVariant.attributeValues[]` (i.e. `ProductVariantAttribute` rows), grouped by `AttributeDefinition.name`. Each group becomes a chooser (dropdown / swatches / etc.). Selecting one value per group resolves to a specific `ProductVariant`; show its SKU, price, stock badge, and an "Add to cart" CTA. Disable choices that have no matching variant.
4. **Specs** — the product's `Product.attributes[]` (`ProductAttribute` rows) rendered as a labeled value list via the polymorphic [`AttributeValue` rules](api.md#42-attributevalue-polymorphic--read-carefully).
5. **Reviews** — list via `GET /api/v1/public/products/{id}/reviews` (public, paginated, no auth). Write/edit/delete via `/api/v1/me/reviews` (auth required). See [`api.md` § 8.4.4](api.md#844-public-reviews--get-apiv1publicproductsproductidreviews) and [§ 8.5.1](api.md#851-my-reviews--apiv1mereviews).
6. **Related products** — same category, fallback to same tags.

### 5.3 Cart

- Works anonymously — persist to `localStorage` and merge with the server cart on sign-in.
- Each row: variant media, name (with selected attributes), unit price snapshot, quantity stepper, line total.
- Cart totals are computed client-side from `priceAtTime` × quantity. Do not recompute server pricing in the UI.
- **Apply coupon code** widget — calls [`POST /api/v1/discounts/validate`](api.md#83-discount-validation--post-apiv1discountsvalidate). The endpoint returns 200 with `{ valid, discountAmount, reason }`. Show `reason` inline if invalid.

### 5.4 Checkout *(multi-step wizard)*

All on one route with state in a service. Each step shows a summary of the next.

1. **Address** — pick from `UserAddress.addresses` or add new. One shipping, one billing (toggle "same as shipping"). Self-service editing of saved addresses is blocked — see [§ 11](#11-backend-gaps-that-affect-the-client).
2. **Shipping method** — derived from the backend's `ShippingRule` for the cart currency. Today shows a single rate.
3. **Discount** — apply a code; show validation feedback (live via `/discounts/validate`).
4. **Payment** — provider selector. Today only `paypal` is registered; UI may show "PayPal" as the only option.
5. **Review & place** — totals breakdown (subtotal / discount / tax / shipping / total), terms, **"Place order" button** that calls `POST /api/v1/orders/checkout` (auth required).

The backend returns `{ orderFulfillment, approveUrl }`. **Redirect the buyer to `approveUrl`** (PayPal). After the buyer approves and lands back at your `returnUrl`, call `POST /api/v1/orders/{orderFulfillment.id}/complete` and show the confirmation page.

Full step-by-step is in [`checkout.md`](checkout.md). Build a `CheckoutService` that owns the state machine and the two backend calls.

### 5.5 Post-purchase

- **Order history** (`/account/orders`) — list with status badges (`FulfillmentStatus`). Detail shows items, addresses, payment summary (fetched from the library at `/api/v1/checkout/orders/{checkoutOrderId}`), shipment tracking, and a "Request return" CTA per item.
- **Order detail → return** — open a `ReturnRequest` form pre-populated with the line item. Status updates flow back via the queue once an admin acts.
- **Shipment tracking** — surface `Shipment.trackingUrl` and the `ShipmentStatus` timeline.
- **Wishlist** (`/account/wishlist`) — list of `WishProduct` rows with "Move to cart" / "Remove".
- **Reviews** (`/account/reviews`) — list of the user's reviews with edit/delete. Currently blocked for self-service — see [§ 11](#11-backend-gaps-that-affect-the-client).

### 5.6 Account

- **Profile** (`/account/profile`) — bind to `UserInfo`. `GET /api/v1/me/info` to load (404 if first time → show empty form), `PUT /api/v1/me/info` to save, `PATCH` for narrow edits. See [`api.md` § 8.5.2](api.md#852-my-profile--apiv1meinfo).
- **Addresses** (`/account/addresses`) — `GET /api/v1/me/addresses` returns the user's `UserAddress` (with an empty `addresses` array for first-time users, no 404). The frontend mutates the array client-side and PUTs the whole row to add/remove. See [`api.md` § 8.5.3](api.md#853-my-addresses--apiv1meaddresses).

### 5.7 Auth shell

- Login, signup, password recover. Auth is provided by `vies-spring-utils`. Confirm exact paths before wiring (see [§ 11](#11-backend-gaps-that-affect-the-client)).
- JWT stored in memory; refresh token in an HttpOnly cookie if the backend supports it.
- Anonymous users may browse + build a cart. Auth wall hits at "Place order" (`Sign in to continue`).

---

## 6. Auth & permissions

- One JWT, one role for this frontend: the regular shopper. Admins do not use this app.
- 401 → clear token, redirect to `/auth/login?return=<currentPath>`.
- 403 → polite "you don't have access" view. Shouldn't normally happen.

---

## 7. Cross-cutting concerns

### 7.1 Money & currency

- `BigDecimal` arrives as `string` per [`api.md` § 1.2](api.md#12-type-mapping-java--typescript). Never coerce to `number` for math.
- One `<money [value] [currency]>` component centralizes `Intl.NumberFormat`.
- Mixed-currency carts are **blocked at checkout** server-side. Block at add-to-cart in the UI too — show a clear error.

### 7.2 DateTime

Custom `DateTime` is a structured object, not ISO. Build a `fromJsDate` / `toJsDate` utility once and reuse.

### 7.3 Optimistic UI

Quantity steppers, wishlist toggles, "save for later" — update locally first, roll back on error. Place-order is **not** optimistic; show a spinner.

### 7.4 SSR / SEO *(storefront-specific)*

- Server-side render or static prerender `/`, `/shop`, `/shop/c/*`, `/shop/p/*`. Angular Universal or `@angular/ssr`.
- Open Graph + Twitter Card meta on product detail. Use product name + primary image + currency-formatted price.
- Structured data (JSON-LD Product schema) on `/shop/p/:slug` for Google Shopping.
- Canonical URLs, sitemap.xml driven from the product list, robots.txt allowing `/shop/*`.

### 7.5 Image performance

- Lazy-load below the fold. Use primary media at low resolution for grid cards; full resolution only on detail.
- `<img loading="lazy">` plus `srcset` if you have multiple resolutions.
- Defer the gallery image swap until interaction.

### 7.6 Accessibility

- Keyboard navigation through the variant picker (arrow keys to step through options).
- Labels on every form input.
- `aria-live` for the cart count chip and toasts.
- Sufficient color contrast for status badges.

### 7.7 Error display

Server validation errors should land near the offending field (mainly in checkout and review forms). Toast as fallback. One error-mapper service.

---

## 8. Out of scope

- Anything admin (catalog management, schema, fulfillment queue, discount setup, rule editing). That's the [Manager](frontend-manager.md).
- Subscriptions, recurring billing (deferred — backend has the entities but they're disabled).
- Native mobile app.
- Multi-tenant per-store theming.

---

## 9. Recommended Angular layout

```
src/app/
├── core/
│   ├── auth/                  guards, JWT interceptor, login state
│   ├── http/                  CrudService<T> base, error mapper
│   ├── money/                 <money> component, BigDecimal helpers
│   └── datetime/              DateTime conversion utils
├── shared/
│   ├── attribute-value-field/ polymorphic reader (read-only on storefront)
│   ├── ui/                    buttons, badges, paged-grid, gallery
│   └── pipes/                 currency, status-label
├── home/
├── catalog/                   browse, search, category landing
├── product/                   product detail page
├── cart/                      cart page + apply-coupon
├── checkout/                  multi-step wizard + CheckoutService
├── account/
│   ├── orders/                history + detail + return flow
│   ├── wishlist/
│   ├── reviews/
│   ├── addresses/
│   └── profile/
└── auth/
```

---

## 10. Practical gotchas (read once)

1. **No public catalog endpoint.** `ProductController` is admin-gated. Block #1 to unblock everything storefront. Workaround for development: log in as admin and call the endpoint.
2. **EAGER everywhere.** Product responses come with the full variant + attribute + media graph. Big. Ask backend for thin DTOs once a list view feels slow.
3. **The polymorphic `AttributeValue`** ([`api.md` § 4.2](api.md#42-attributevalue-polymorphic--read-carefully)) drives the variant picker AND the spec display. Single shared component.
4. **Discount validation lives at a dedicated endpoint** (`POST /api/v1/discounts/validate`) — non-destructive, returns 200 with `{ valid, discountAmount, reason }`. Do not call `/orders/checkout` to "test" a code.
5. **Cart deactivation is automatic.** When `/orders/checkout` succeeds, the server marks `Cart.active = false`. The frontend should refresh the cart store after the redirect comes back from PayPal.
6. **`OrderStatus` is split.** `OrderFulfillment.status` covers shipping; `CheckoutOrder.status` covers payment. The order-detail view fetches both.
7. **Currency consistency.** Block adding items in a different currency to the same cart at add-to-cart time. The orchestrator will reject mixed carts at checkout, but you want to fail earlier.
8. **Self-service is hyper-explicit.** Every `/api/v1/me/*` endpoint requires the `user_id` header (UUID). It is *not* read from the JWT by the framework — the controller pulls it from the header. The interceptor that already attaches `Authorization: Bearer <jwt>` should also attach `user_id: <uuid>` to every authenticated call so it Just Works.
9. **PATCH over PUT for narrow edits.** PUT may clear fields you forgot to include.

---

## 11. Backend gaps that affect the Client

See [`api.md` § 13](api.md#13-not-implemented-yet) for the full list. The ones that block client features specifically:

- ~~**Public product browse API**~~ — **shipped** at `/api/v1/public/products/*` (see [`api.md` § 8.4](api.md#84-storefront-read-api-public-no-auth)).
- ~~**Self-service for `Review` / `UserInfo` / `UserAddress`**~~ — **shipped** at `/api/v1/me/*` (see [`api.md` § 8.5](api.md#85-self-service-me-endpoints-user-scoped)).
- **Webhook → fulfillment listener** — if the buyer's browser closes mid-flow (between `/checkout` and `/complete`), nothing automatically advances the order. The buyer reloading their order page won't see the right status until an admin sees the failure and acts. The library shipped the SPI in 6.2.9; backend implementation is pending.
- **Search endpoint** — `/matches?propertyMatcher=CONTAINS` is the stopgap; real full-text is on the backlog.
- **Login / refresh-token paths** — confirm exact paths exposed by `vies-spring-utils`.
- **Media uploader** — not a blocker for client (URLs come pre-set); blocker for admin.

---

## 12. How to start

1. **Read [`api.md`](api.md)** end-to-end. Scaffold `src/app/models/` from §§ 3–10. Read [`checkout.md`](checkout.md) for the checkout flow.
2. Build the **generic `CrudService<T>`** ([§ 11](api.md#11-http-service-blueprint)).
3. **Stand up the auth shell + JWT interceptor**. Anonymous browsing should work without a token; authed calls attach `Authorization`.
4. **Build the polymorphic attribute reader.** Same component as the Manager's editor, but the client mostly displays values (variant picker + specs).
5. **Catalog browse + product detail** — even if the backend is still admin-gated, build against an admin token so UI work happens in parallel with the backend public-read endpoint.
6. **Cart** — anonymous-first, localStorage-backed. Pair with the apply-coupon widget.
7. **Checkout wizard + `CheckoutService`**. The two-call dance (`/checkout` → redirect → `/complete`) is the highest-stakes piece — write it carefully, including failure recovery.
8. **Account area** — orders history first (highest-value), then wishlist. Reviews / profile / addresses last (gated on backend self-service).
9. **SSR / SEO** — once core flows work end to end, layer on Angular Universal for the public pages.
10. **Polish, a11y, performance pass.**

Defer payments-other-than-PayPal, subscriptions, and any AI/recommendation features until the linear-purchase flow is solid.
