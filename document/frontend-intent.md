# Venzora Frontend — E-Commerce Intent

> A brief for designing and building the Venzora frontend, an **e-commerce website** with both a public storefront (shoppers) and an admin back office (catalog managers, fulfillment). Stack is **Angular + TypeScript**. For data shapes and TS interfaces, see [`models.md`](models.md). For the REST API surface (URLs, verbs, gates), see [`api.md`](api.md). This document is intent — what to build and why — not a data dictionary or API reference.

---

## 1. What Venzora is

Venzora is an e-commerce platform built around a **flexible product model**: every product has a dynamic schema of attributes (Size, Color, Material, Warranty…) that drives both how it's described to the shopper and how variants (SKU-level rows) are generated. The platform is currency-aware (ISO 4217), supports tagging and category hierarchies, and tracks the full commerce lifecycle from cart through shipment, return, and refund.

The frontend is the entire web product. It has two audiences:

- **Shoppers** — anonymous or logged-in customers browsing the catalog, adding to cart, checking out, tracking orders, leaving reviews, managing a wishlist.
- **Administrators** — internal users managing the catalog, the attribute schema, orders, returns, discounts, and inventory.

These are two distinct experiences in one codebase, sharing models, auth, and design system.

---

## 2. Tech stack & conventions

- **Angular** (latest stable). Prefer standalone components and the modern signals/control-flow syntax unless the team has agreed otherwise.
- **TypeScript** strict mode. All data types come from [`models.md`](models.md) — translate those into a shared `models/` directory.
- **State** — Service + signals is enough for v1. Reach for NgRx only if the cart/checkout flow turns out to need it.
- **Forms** — Reactive Forms for everything non-trivial (especially the dynamic attribute editor; see § 7.4).
- **Routing** — feature-routed; lazy-load the admin shell so shoppers never download admin code.
- **HTTP** — typed `HttpClient` services per resource, with an interceptor that attaches the JWT and a second that surfaces 401/403 cleanly.
- **i18n / currency** — every monetary string carries an explicit ISO currency code (no implicit USD). Build one `<money>` component, use it everywhere.
- **Theming** — pick one component library (Angular Material or PrimeNG) and stick to it. Skin once, reuse.

---

## 3. Backend at a glance

- **Stack**: Spring Boot 3.3.4, Java 17, JPA/Hibernate, H2 (local) / MySQL (prod). Default local port `8085`, base path `/api/v1`.
- **IDs are UUIDv7** (server-generated, time-sortable). Never send an `id` on a `POST`.
- **JWT auth.** Send `Authorization: Bearer <token>` on every authenticated request. The shopper experience is *partially anonymous* — public catalog browsing should work without a token, while cart/order/wishlist endpoints require one.
- **Admin gate.** Most current endpoints extend `ViesAutoAdminCheckController` (admin-only). User-scoped endpoints extend `ViesControllerWithUserAccess` and filter by the authenticated user automatically.
- **Eager loading everywhere** — responses are deep object trees. List views should expect heavier payloads than usual; raise it to the backend if a screen gets slow.
- **Cascade deletes** propagate (deleting a Product wipes its variants/attributes/media). Confirm destructive actions with dependent counts.

### Controllers wired today (full catalog in [`api.md`](api.md))

Every catalog, commerce, social, and user entity now has a CRUD controller. The seven standard endpoints (`GET /`, `GET /{id}`, `POST /matches`, `POST /`, `PUT /{id}`, `PATCH /{id}`, `DELETE /{id}`) are uniform across all of them — see [`api.md` § 1.3](api.md#13-the-seven-endpoints-every-controller-exposes).

Gate quick-reference:

| Surface area                                         | Gate                |
|------------------------------------------------------|---------------------|
| Catalog & schema (Product, Variant, Media, Category, Tag, Attribute*) | Admin               |
| Cart, Order, ReturnRequest, WishProduct              | **User-scoped**     |
| Payment, Shipment, Discount, StockMovement           | Admin               |
| CartItem, OrderItem                                  | Admin (manage via parent) |
| Review, UserInfo, UserAddress                        | Admin *(see § 11)* |

**Critical gap for v1**: there is no **public** product-browse endpoint today — `ProductController` is admin-gated, so anonymous shoppers cannot view the catalog. Coordinate with the backend to either lift the admin gate or add a parallel public read-only product API before the storefront work begins.

---

## 4. Information architecture

Two top-level shells, separated by route and bundle:

```
/ (storefront — public)
├── /                          home / featured
├── /shop                      catalog browse, filters
├── /shop/c/:slug              category landing
├── /shop/p/:slug              product detail
├── /search?q=...              search results
├── /cart                      cart review (works anonymously, persists for signed-in)
├── /checkout                  multi-step checkout (login required to complete)
├── /account                   logged-in shopper area
│   ├── /orders                history, detail, returns
│   ├── /wishlist
│   ├── /reviews               my reviews
│   ├── /addresses
│   └── /profile
└── /auth/{login,signup,recover}

/admin (back office — lazy-loaded, admin role required)
├── /catalog/products          list + editor
├── /catalog/categories        tree editor
├── /catalog/tags
├── /catalog/media             optional, depending on uploader
├── /schema/attributes         AttributeDefinition + AttributeOption
├── /orders                    fulfillment queue, order detail
├── /returns                   RMA queue
├── /inventory                 StockMovement, low-stock alerts
├── /discounts                 coupons & promotions
└── /reviews                   moderation queue
```

Use Angular's `loadChildren` to fence `/admin` behind a route guard plus its own lazy module — shoppers never download it.

---

## 5. The shopper experience

The storefront has to feel like a real store, not an admin demo. The bar is competitive UX.

### 5.1 Catalog & search
- **Category landing**: hero, child categories, then a product grid filtered by category.
- **Filters**: by category, tag, price range, attribute (faceted on `AttributeDefinition`/`AttributeOption`), in-stock toggle, currency-aware. Build a single faceted-search component; do not reinvent it per page.
- **Search**: full-text against name/description. Until the backend offers a search endpoint, filter client-side from a cached product list; flag it as a placeholder.
- **Listing card**: primary `ProductMedia`, name, currency-formatted price (use `basePrice` plus `currency`), category, badge for status (`OUT_OF_STOCK`, `NEW`).

### 5.2 Product detail
This is the hero page. Sections, in this order:

1. **Gallery** — `ProductMedia` for the product, plus per-variant media when a variant is selected. Respect `isPrimary` and `sortOrder`.
2. **Identity** — name, category breadcrumb, average rating (derived from `Review`), short description.
3. **Variant picker** — render the *variant-level* attribute definitions (`AttributeDefinition.variantLevel === true`) as choosers. Selecting all required choices resolves to a `ProductVariant`; show its SKU, price, stock badge, and an "Add to cart" CTA. Disable choices that have no matching variant.
4. **Specs** — the *product-level* attributes (`variantLevel === false`) as a labeled value list. Use the polymorphic [`AttributeValue` rules](models.md#42-attributevalue-polymorphic--read-carefully) to render each one with the right control.
5. **Reviews** — list + write-a-review form (logged in only).
6. **Related products** — same category, fallback to same tags.

### 5.3 Cart
- Works anonymously (persist to `localStorage`) and merges with the server cart on sign-in.
- Each row shows the variant's media, name (with selected attributes), unit price snapshot, quantity stepper, line total.
- Cart totals are computed client-side from `priceAtTime` × quantity; do not recompute server pricing in the UI.

### 5.4 Checkout
A multi-step wizard, all on one route with state in a service:

1. **Address** — pick from `UserAddress.addresses` or add new. One shipping, one billing (toggle "same as shipping").
2. **Shipping method** — placeholder until the backend offers options.
3. **Discount** — apply a `Discount.code`; show validation feedback (`active`, `validFrom`/`validTo`, `minimumOrderAmount`).
4. **Payment** — `PaymentMethodType` selector (`CARD | CASH | PAYPAL`). Card capture is **out of scope** until the backend integrates a processor; v1 may collect intent only.
5. **Review & place** — summary, terms, `POST /orders` (when the endpoint exists), success page with the new `orderNumber`.

### 5.5 Post-purchase
- **Order history**: list of orders with status badges (`OrderStatus`). Detail shows items, addresses, payment summary, shipment tracking, and a "Request return" CTA per item.
- **Order detail → return**: open a `ReturnRequest` form pre-populated with the line item.
- **Shipment tracking**: surface `Shipment.trackingUrl` and the `ShipmentStatus` timeline.
- **Wishlist**: list of `WishProduct` rows, with "Move to cart" / "Remove".
- **Reviews**: list of the user's reviews with edit/delete; ratings use the `BigDecimal` `rating` field (treat as string per the type map).

### 5.6 Account
- **Profile** — bind to `UserInfo` (firstName, lastName, phoneNumber, avatarUrl).
- **Addresses** — CRUD against `UserAddress.addresses` (a set of embedded `Address` objects); show `BILLING` vs `SHIPPING` as a chip.

### 5.7 Auth shell
- Login, signup, password recover. Auth is provided by `vies-spring-utils`; the frontend stores the JWT in memory (with a refresh token in an HttpOnly cookie if the backend supports it) and clears it on 401.
- Anonymous users should be allowed to browse and to add to cart; the wall hits at checkout (`Sign in to continue`).

---

## 6. The admin experience

The back office is what the previous version of this doc described. Keep it functional and dense — admins are repeat users.

### 6.1 Schema (the foundation)
Nothing else works until this exists. List/create/edit/delete `AttributeDefinition`, inline-edit `AttributeOption`s for SELECT/MULTI_SELECT types, toggle `variantLevel` and `required` with inline explanation.

### 6.2 Categories
Tree view with `parentCategoryId`. Drag-to-reparent is nice-to-have. Per-category, manage the M2M list of `AttributeDefinition`s that apply.

### 6.3 Products (the hero admin flow)
One multi-section editor:

1. **Basics** — name, description, category (required), tags, currency, basePrice, baseSku, status.
2. **Product-level attributes** — pick `AttributeDefinition`s where `variantLevel === false`, fill in their `AttributeValue` per the polymorphism rules.
3. **Variant-level attributes & generation** — pick the variant-level definitions and their option sets, then "Generate variants" produces the cartesian combinations. Each generated variant gets a default SKU (`{baseSku}-{opt1}-{opt2}`), base price, zero stock, `ACTIVE`. The combination generator may live client-side or call a backend endpoint when added.
4. **Variants table** — editable rows for SKU, name, price, stock, weight, status; row → variant detail with media and variant-level attribute values.
5. **Media** — gallery upload (paste URL for v1), per-product and per-variant.

### 6.4 Orders & fulfillment
- Queue with status filters and search by `orderNumber`.
- Detail view exposes status transitions (PENDING → PROCESSING → SHIPPED → DELIVERED), shipment creation, payment status, item-level returns.

### 6.5 Returns (RMA)
List of `ReturnRequest` with `ReturnStatus` lifecycle. Detail flow: review reason, approve/reject, mark shipped/received/refunded, write `adminNotes`, set `refundAmount`.

### 6.6 Inventory
`StockMovement` log filtered by variant, plus a "low stock" view. Adjustments create new `StockMovement` rows (type `ADJUSTMENT`); never edit `quantityAfter` directly.

### 6.7 Discounts
CRUD for `Discount`. The `discountType` selector switches the meaning of `discountValue` (percentage vs fixed); the form must adapt — see the attribute-value pattern for inspiration.

### 6.8 Reviews moderation
List with rating sort, status filter, delete control.

---

## 7. Cross-cutting concerns

### 7.1 Auth & guards
- One JWT, two roles: `USER` and `ADMIN`. Admin routes are guarded; user routes redirect to `/auth/login?return=`.
- Handle 401 with a clean logout + redirect; handle 403 with a polite "you don't have access" view.

### 7.2 Currency & money
- `BigDecimal` values arrive as `string` (per [`models.md`](models.md) § 1.1). Never coerce to `number` for math — use a library or string arithmetic.
- One `<money [value]="..." [currency]="...">` component centralizes formatting (`Intl.NumberFormat`).

### 7.3 DateTime
- The custom `DateTime` is a structured object, not an ISO string. Write one utility module to convert to/from the JS `Date` and reuse — never hand-roll the conversion at call sites.

### 7.4 The dynamic attribute form (the gnarly bit)
- Build **one** `<attribute-value-field [definition] [(value)]>` component. It:
  1. Reads `AttributeDefinition.type`.
  2. Renders the right control (text, number, toggle, single-select, multi-select, date, time, date-time).
  3. Writes to the single matching slot in `AttributeValue` and clears the others.
- Use the same component in the admin (product editor) and in the storefront (variant picker), passing different visual variants.

### 7.5 Optimistic UI
Cheap mutations (toggle status, reorder, qty stepper) update locally first and rollback on error. Heavy mutations (place order, edit product) show a spinner and refetch on success.

### 7.6 Error display
Server validation errors should land near the offending field where possible. Use a toast as the fallback. Build one error mapper service.

### 7.7 SEO & performance (storefront only)
- Server-side rendering or static prerender for `/`, `/shop`, `/shop/c/*`, `/shop/p/*`. Angular Universal or the modern `@angular/ssr`.
- Image hygiene: lazy load below the fold, use the primary media at low resolution for cards.
- A11y: keyboard navigation through the variant picker, labels on every input, `aria-live` for cart count and toasts.

---

## 8. Out of scope (v1)

- Multi-tenant / multi-store.
- Subscriptions, recurring billing, gift cards (no models for them).
- Real payment processor integration (collect intent only until the backend wires Stripe/PayPal).
- Marketing emails, transactional emails (server-side concern).
- Internal user/admin invitation flows (auth lives in `vies-spring-utils`; treat it as an external service).
- A native mobile app.

If a task brushes against these, stop and confirm with the user.

---

## 9. Recommended Angular layout

```
src/app/
├── core/
│   ├── auth/                  guards, JWT interceptor, login state
│   ├── http/                  http client wrappers, error mapper
│   ├── money/                 <money> component, BigDecimal helpers
│   └── datetime/              DateTime/Date/Time conversion utils
├── models/                    types from models.md (see § 10 of that file)
├── shared/
│   ├── attribute-value-field/ the polymorphic editor — used by storefront and admin
│   ├── ui/                    buttons, badges, status chips
│   └── pipes/                 currency, status-label, etc.
├── storefront/
│   ├── home/
│   ├── catalog/               browse, search, category landing
│   ├── product/               product detail
│   ├── cart/
│   ├── checkout/
│   ├── account/
│   └── auth/
└── admin/                     lazy-loaded
    ├── catalog/               products, categories, tags
    ├── schema/                attribute definitions & options
    ├── orders/
    ├── returns/
    ├── inventory/
    ├── discounts/
    └── reviews/
```

Each feature is a self-contained set of standalone components + a service for HTTP.

---

## 10. Practical gotchas (read once)

1. **No public catalog endpoint exists yet.** The storefront cannot browse the catalog until the backend either lifts the admin gate from `ProductController` or adds a parallel public read API. This is the single biggest blocker for v1 storefront work — call it out in the kickoff.
2. **The CRUD-only endpoints are not enough on their own.** Every entity is reachable over HTTP, but workflows that span multiple entities (checkout, return processing, discount validation, variant generation) still need purpose-built endpoints. See [§ 11](#11-open-questions--backend-gaps) and [`api.md` § 5](api.md#5-endpoints-intentionally-not-implemented-yet).
3. **Cascade deletes are real.** Show dependent counts in destructive confirmations.
4. **EAGER everywhere.** Responses are heavy. Consider asking for thin DTOs as soon as a list view feels slow.
5. **The polymorphic `AttributeValue` is the most failure-prone piece.** Centralize it (see § 7.4). Do not reimplement the slot logic per screen.
6. **Categories are a tree by convention, not by FK.** Build the tree client-side from `parentCategoryId`; validate for cycles/orphans before saving.
7. **Currency is on the Product, not the Cart or Order.** Mixing currencies in a single cart is undefined behavior today — either constrain at add-to-cart, or raise it to the backend.
8. **`Review`, `UserInfo`, `UserAddress` are admin-gated.** They cannot be self-served by shoppers today because the underlying entities don't extend `UserAccess`. Each needs a backend change (model migration or a custom controller) before its account-area screen ships.
9. **`PATCH` is your friend.** The default mental model is `PUT`, but `PATCH /{id}` lets you send only the changed fields and avoids accidentally clearing the rest. Use it for narrow form edits.

---

## 11. Open questions / backend gaps

CRUD endpoints for every entity have landed (see [`api.md`](api.md)). What remains are the multi-entity workflows and the public/self-service variants that simple CRUD can't express. Track these as coordination items with the backend team.

### Done

- [x] **CategoryController, TagController.**
- [x] **CartController, CartItemController.**
- [x] **OrderController, OrderItemController.**
- [x] **ReviewController** *(but admin-gated — see below).*
- [x] **ReturnRequestController.**
- [x] **ShipmentController.**
- [x] **DiscountController.**
- [x] **PaymentController** *(CRUD only; no processor integration).*
- [x] **UserInfoController, UserAddressController** *(but admin-gated — see below).*
- [x] **ProductMediaController** *(CRUD; uploader still TBD).*

### Outstanding

- [ ] **Public product browse API** — admin gate lifted on `ProductController`, or a parallel read-only endpoint. **Single biggest unlock for the storefront.**
- [ ] **Self-service for `Review` / `UserInfo` / `UserAddress`** — these entities don't extend `UserAccess`, so they can't use `ViesControllerWithUserAccess`. Either migrate the models or add a custom controller per resource that filters by the JWT user.
- [ ] **Variant-generation endpoint** — `POST /api/v1/products/{id}/generate-variants` consuming `{ "Size": ["S","M"], "Color": ["Red","Blue"] }` and creating the cartesian rows in one shot. Otherwise the admin UI must loop.
- [ ] **Checkout endpoint** — `POST /api/v1/orders/checkout` that atomically: validates the cart, locks stock, applies a discount, creates the order, and opens a payment intent. The single most important commerce endpoint to add.
- [ ] **Discount validation** — `POST /api/v1/discounts/validate` that accepts a `code` + cart context and returns the resolved discount (or a reason it doesn't apply). Without this, the checkout UI duplicates server rules.
- [ ] **Payment processor integration** — Stripe / PayPal / etc. Today `PaymentController` is just CRUD over a row.
- [ ] **Media uploader endpoint** — so the admin product editor can attach images directly instead of pasting URLs.
- [ ] **Search endpoint** — full-text on products. Use `POST /matches` with `propertyMatcher=CONTAINS` as a stopgap.
- [ ] **Login / refresh-token endpoint** — confirm the exact paths exposed by `vies-spring-utils` and whether refresh tokens use HttpOnly cookies.
- [ ] **Currency consistency policy** — confirm the rule for mixed-currency carts (block at add-to-cart? convert? error?).

---

## 12. How to start

1. **Read [`models.md`](models.md)** end-to-end and scaffold `src/app/models/` from it.
2. **Read [`api.md`](api.md) § 1**. Build a generic `CrudService<T>` ([example in api.md § 4.1](api.md#41-suggested-http-service-layout)) — every entity service becomes a 5-line subclass.
3. **Stand up the auth shell + HTTP interceptor** so every later request is authenticated cleanly.
4. **Build the polymorphic attribute editor** ([§ 7.4](#74-the-dynamic-attribute-form-the-gnarly-bit)) once — every later screen leans on it.
5. **Start in admin**, because the catalog endpoints exist and you need data to display:
   - Attribute definitions → Categories → Tags → Products → Variants.
6. **Pivot to storefront** the moment the public product API lands. Until then, point at the admin endpoint with a logged-in admin token so you can build UI in parallel.
7. **Defer payment, search, and SSR** until the core flows work end-to-end against the running backend.
