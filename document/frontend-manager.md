# Venzora Manager Frontend — Intent

> A brief for designing and building the **Venzora Manager** — an internal admin tool used by catalog managers, fulfillment operators, finance, and ops to run the Venzora backend. This is the **back-office** frontend; the customer-facing storefront is a separate codebase — see [`frontend-client.md`](frontend-client.md). For data shapes and REST endpoints, the source of truth is [`api.md`](api.md). For end-to-end checkout flow, see [`checkout.md`](checkout.md).

---

## 1. What this frontend is for

Venzora is a self-hostable e-commerce platform. Every install needs an admin tool. This one. Users of the Manager are internal — never customers — and they:

- Define the **attribute schema** that gives products their shape (Size, Color, Material, …).
- Manage the **catalog**: products, variants, categories, tags, media.
- Run **fulfillment**: orders, shipments, returns.
- Manage **inventory** (stock movements, low-stock visibility).
- Manage **promotions** (discounts) and **commerce rules** (shipping, tax).
- Moderate **reviews**.

The user base is small, repeat, high-skill. Optimize for density and keyboard ergonomics, not first-impression polish.

---

## 2. Tech stack & conventions

- **Angular** (latest stable). Prefer standalone components and the modern signals / control-flow syntax.
- **TypeScript** strict mode. All data types come from [`api.md`](api.md).
- **State** — Service + signals is enough. Reach for NgRx only if a multi-step admin form (variant generator, checkout simulator) actually demands it.
- **Forms** — Reactive Forms for everything non-trivial. The dynamic attribute editor (see § 7.2) is the central reusable component.
- **Routing** — feature-routed; the whole app is one shell with a left-nav.
- **HTTP** — typed `HttpClient` services per resource. Use the `CrudService<T>` blueprint in [`api.md` § 11](api.md#11-http-service-blueprint).
- **i18n / currency** — every monetary string carries an explicit ISO currency code. One `<money>` component, used everywhere.
- **Theming** — pick one component library (Angular Material or PrimeNG) and stick to it. Density-optimized tables.

---

## 3. Backend at a glance

- **Stack**: Spring Boot 3.3.4, Java 17, JPA/Hibernate, H2 (local) / MySQL (prod). Default local port `8085`, base path `/api/v1`. Full conventions in [`api.md` § 1](api.md#1-conventions).
- **IDs**: UUIDv7 (sortable, time-ordered). Never send `id` on `POST`.
- **JWT auth.** Send `Authorization: Bearer <jwt>` on every request. Manager users are expected to have the admin role; non-admins receive `403` on virtually every endpoint.
- **Two gate styles**: most CRUD here is **admin-gated** (`ViesAutoAdminCheckController`). A few resources are **user-scoped** (Cart, OrderFulfillment, ReturnRequest, WishProduct) — admins see assigned/shared rows.
- **Eager loading everywhere** — responses are deep trees. Plan list views accordingly.
- **Cascade deletes propagate.** Show dependent counts in confirmation dialogs.

---

## 4. Information architecture

A single shell, lazy-loaded feature modules:

```
/
├── /catalog
│   ├── /products              list + editor (the hero flow)
│   ├── /categories            tree editor
│   ├── /tags
│   └── /media                 optional, depending on uploader status
├── /schema
│   ├── /attribute-definitions
│   └── /attribute-options     (or nested inside definitions)
├── /commerce
│   ├── /orders                fulfillment queue, order detail
│   ├── /returns               RMA queue
│   ├── /shipments             active shipments, tracking
│   └── /discounts             coupons & promotions
├── /inventory
│   ├── /stock                 variant stock view + low-stock alerts
│   └── /movements             StockMovement audit log
├── /rules
│   ├── /shipping              ShippingRule per-currency CRUD
│   └── /tax                   TaxRule + import/export
├── /reports                   tax filing, sales dashboard, exports
├── /reviews                   moderation queue
└── /auth/login                admin sign-in
```

The whole app is admin-gated at the route guard; a non-admin who somehow loads it gets redirected to `/auth/login`.

---

## 5. Core flows (ranked by importance)

The first three are the spine of the Manager. Make those polished. The rest can be functional.

### 5.1 Manage the attribute schema *(foundation)*

Nothing else works without this.

- List / create / edit / delete `AttributeDefinition`s ([`api.md` § 6.1](api.md#61-attributedefinition)).
- For `type ∈ {SELECT, MULTI_SELECT}`: inline editor for the `AttributeOption` list (value, displayValue, sortOrder). Reorderable by `sortOrder`.
- Definitions are pure concepts (Size, Color, Material) — there is **no** variant-level or required flag on the definition itself. Whether an attribute drives variants or is a spec is decided per-product in the product editor (§ 5.3), not at the schema layer. Whether it is required is a per-category / per-product form-validation concern, not a schema fact.

### 5.2 Manage categories

- Tree view (self-referential via `parentCategoryId`). Drag-to-reparent is nice-to-have, not required.
- Per-category, manage the M2M `attributeDefinitions` — a multi-select of definitions that apply.
- Validate against cycles client-side (no FK constraint server-side).

### 5.3 Create / edit a product *(the hero admin flow)*

Multi-section editor — tabs or a long form, your call:

1. **Basics**: name, description, category (required, tree picker), tags (multi-select), currency (searchable combobox, 173 ISO codes), `basePrice`, `baseSku`, status.
2. **Attributes for this product**: pick which `AttributeDefinition`s apply. For each picked definition, decide *for this product* whether it is:
   - A **spec** (product-level) → creates a `ProductAttribute` row on the product with the value filled in.
   - A **variant dimension** (variant-level) → participates in variant generation; values are set per variant, not on the product.
   The distinction lives in the row type, not on the definition. Same `AttributeDefinition` (e.g. Color) can be a spec on a desk product and a variant dimension on a t-shirt product.
3. **Variant generation**:
   - Combining the variant-dimension attributes picked in step 2, **"Generate variants"** produces the cartesian product. Default SKU `{baseSku}-{opt1}-{opt2}`, base price, zero stock, `ACTIVE` status. Implementation note: the backend doesn't have a generator endpoint yet — the frontend computes combinations and POSTs each variant.
   - Each generated variant gets a `ProductVariantAttribute` row per participating definition, carrying the chosen `AttributeValue`.
4. **Variants table**: per-row edit of SKU, name, price, stock, weight, status. Row click → variant detail panel with per-variant media and per-variant attribute values.
5. **Media**: per-product gallery, primary flag (UI must clear other primaries when toggling), per-variant media in the variant panel.

### 5.4 Orders & fulfillment

- Queue at `/commerce/orders` with status filters and search by `orderNumber`.
- Detail view exposes status transitions (`PENDING` → `PROCESSING` → `SHIPPED` → `DELIVERED`), shipment creation, return triggering.
- For payment status, resolve `OrderFulfillment.checkoutOrderId` against the library's `GET /api/v1/checkout/orders/{id}`. That gives `status` (`CAPTURED`/`REFUNDED`/…), `amountTotal`, `amountRefunded`, and the audit log of `CheckoutTransaction`s.
- The full flow context (when does what fire?) is in [`checkout.md`](checkout.md).

### 5.5 Returns (RMA)

- List of `ReturnRequest` with `ReturnStatus` lifecycle.
- Detail flow: review reason, approve/reject, mark shipped/received/refunded, write `adminNotes`, set `refundAmount`.
- When approved + refundable: call the library `POST /api/v1/checkout/orders/paypal/{checkoutOrderId}/refund?amount=…&reason=…`. Then update `OrderFulfillment.status` to `REFUNDED` / `PARTIALLY_REFUNDED`.

### 5.6 Inventory

- `/inventory/stock` — per-variant stock view filtered by category / product, with a "low stock" sub-view.
- Adjustments **insert new `StockMovement`** rows (type `ADJUSTMENT`). Never PATCH `quantityAfter` directly — it's denormalized from the running sum.
- Audit log at `/inventory/movements` — filter by variant, date, type.

### 5.7 Discounts

CRUD for `Discount`. The `discountType` selector switches the meaning of `discountValue` (percentage vs fixed) — the form must adapt. Borrow the polymorphic attribute editor pattern.

Validation rules (enforced server-side at checkout):

- `active`, `validFrom <= now <= validTo`, `currentUses < maxUses`, subtotal >= `minimumOrderAmount`.

### 5.8 Commerce rules — shipping & tax

#### Shipping (`/rules/shipping`)

- One `ShippingRule` per currency (DB-enforced unique).
- Fields: `currency`, `flatFee`, `freeAboveAmount` (nullable), `description`, `active`.
- Use a single page with a row per currency. New row → currency picker (only currencies without existing rules).

#### Tax (`/rules/tax`)

The most flexible piece — admin can model any country's tax. See [`api.md` § 7.11](api.md#711-taxrule).

- List view shows rules sorted by **specificity** (number of non-null matchers) descending, then `priority` descending. That's the order they're evaluated in.
- Editor: free-form `name`, `rate` (percentage), four matcher fields (`country`, `state`, `city`, `postalCode`) each with a "match any" toggle that nulls the field.
- **Import / export**:
  - `Export` button → `GET /api/v1/tax/rules/export` → downloads a JSON array.
  - `Import` modal → file picker → toggle for `append | replace` → `POST /api/v1/tax/rules/import?mode=…`.
  - On `replace`: confirmation dialog with the count of existing rules that will be deleted.
- Test pad (nice-to-have): an "evaluate" widget where the admin types a shipping address and sees which rule matches and what tax would apply. Pure client-side reproduction of the matching algorithm (load all active rules, score, pick winner).

### 5.9 Reviews moderation

List with rating sort, status filter, delete control. Plain table.

### 5.10 Reports & analytics

A `/reports` section that hits the read-only endpoints in [`api.md` § 8.5](api.md#85-reports-analytics). The backend returns raw JSON; the Manager owns all visualization.

Recommended views:

1. **Tax filing** *(highest value)* — `GET /reports/tax?from=&to=`. Render as a table grouped by jurisdiction with a "download as CSV" button. The accountant pastes this into the sales-tax return. Show the `matchingRule` column as informational only (it can drift from what was applied at sale time — point users at the order's `metadata.tax.*` for the historical record).
2. **Sales dashboard** — KPI cards from `/reports/sales/summary`, line/bar chart from `/reports/sales/timeseries?bucket=day`, leaderboards from `/reports/products/top` and `/reports/categories/top`, a world map from `/reports/geography?groupBy=country`.
3. **Order pipeline** — donut chart from `/reports/orders/status` showing where orders sit.
4. **Refunds & customers** — two small cards from `/reports/refunds` and `/reports/customers/summary`.
5. **Raw export** — a "Download all orders for this period" button that hits `/reports/orders?from=&to=` (paginated) and stitches pages into a CSV the user can drop into Excel / Metabase / Looker.

Every report takes the same `from` + `to`. Build one `<ReportPeriodPicker>` component and a service that fans out the request set.

### 5.11 Order metadata viewer

Every `OrderFulfillment` carries a `metadata: Map<String, String>` snapshot ([`api.md` § 7.3](api.md#73-orderfulfillment)). On the order detail page:

- Render the system keys (`checkout.*`, `tax.*`, `discount.*`, `shipping.*`) as a read-only "audit" panel — this is the historical record of what happened at sale time, and it must survive later edits to TaxRule, ShippingRule, Discount, etc.
- Render `notes.*` keys as an editable list grouped by topic. Add UI to create new `notes.<topic>` entries (free-form key, plain-text value) for incident notes, fraud reviews, customer-service decisions.
- The system keys should *not* be editable from the Manager. The notes keys should be.

---

## 6. Auth & permissions

- One JWT, one role for this frontend: **admin**. Non-admins should never reach the app shell — guard at the root route.
- Handle `401` with clean logout + redirect. Handle `403` with "you don't have access to that resource" (in case some endpoints require finer permissions in the future).
- Login form posts to whatever path `vies-spring-utils` exposes (TBD — see [`api.md` § 13](api.md#13-not-implemented-yet)). Confirm with backend before wiring.

---

## 7. Cross-cutting concerns

### 7.1 Money & currency

- `BigDecimal` arrives as `string` per [`api.md` § 1.2](api.md#12-type-mapping-java--typescript). Never coerce to `number` for math.
- One `<money [value] [currency]>` component centralizes `Intl.NumberFormat`.

### 7.2 The dynamic attribute editor *(the gnarly bit)*

Single `<attribute-value-field [definition] [(value)]>` component:

1. Reads `AttributeDefinition.type` ([`api.md` § 4.2](api.md#42-attributevalue-polymorphic--read-carefully)).
2. Renders the right control: text, number (with `unit` suffix), toggle, single-select, multi-select, date, time, date-time.
3. Writes to the matching slot in `AttributeValue` and clears the others.

The same component is used in:
- The product editor (both the product-level spec rows and the per-variant attribute rows).
- Any future "filter by attribute" admin tool.

Do not reimplement the slot logic per screen.

### 7.3 DateTime

Custom `DateTime` is a structured object, not ISO. Build `fromJsDate` / `toJsDate` utility once.

### 7.4 Optimistic UI

Cheap toggles (status flips, reorderings) update locally first and roll back on error. Heavy mutations (saving a full product graph) show a spinner and refetch on success.

### 7.5 Error display

Server validation errors land near the offending field where possible; toast as fallback. One error-mapper service.

### 7.6 Cascade-delete confirmations

Show dependent counts: "Deleting this product will also delete 6 variants, 12 attributes, and 4 media items. Continue?"

---

## 8. Out of scope

- Customer browsing, cart, checkout (those are the [client](frontend-client.md)).
- Building a payment processor integration in the UI (backend concern).
- File upload server (no media uploader endpoint yet).
- Analytics dashboards.
- Multi-tenant administration.

---

## 9. Recommended Angular layout

```
src/app/
├── core/
│   ├── auth/                  guards, JWT interceptor, login
│   ├── http/                  CrudService<T> base, error mapper
│   ├── money/                 <money> component, BigDecimal helpers
│   └── datetime/              DateTime conversion utils
├── shared/
│   ├── attribute-value-field/ the polymorphic editor
│   ├── ui/                    buttons, badges, status chips, paged-table
│   └── pipes/                 currency, status-label
├── catalog/
│   ├── products/              list + editor
│   ├── categories/            tree
│   ├── tags/
│   └── media/                 (when uploader lands)
├── schema/
│   ├── attribute-definitions/
│   └── attribute-options/
├── commerce/
│   ├── orders/
│   ├── returns/
│   ├── shipments/
│   └── discounts/
├── inventory/
│   ├── stock/
│   └── movements/
├── rules/
│   ├── shipping/
│   └── tax/                   includes import/export modal
├── reviews/
└── auth/
```

---

## 10. Practical gotchas (read once)

1. **EAGER everywhere.** Product responses come with their full variant + attribute + media graph. Big payloads. Consider asking the backend for thinner DTOs once a list view feels slow.
2. **The polymorphic `AttributeValue` is the most failure-prone piece.** Centralize it (see § 7.2). Do not reimplement per screen.
3. **Categories are a tree by convention.** No FK constraint on `parentCategoryId`. The Manager must guard against cycles client-side.
4. **`OrderStatus` is split.** Payment-side state lives on `CheckoutOrder.status` (library); fulfillment-side on `OrderFulfillment.status` (Venzora). The admin order-detail view needs to fetch both.
5. **Discount validation is not the admin's job.** The orchestrator validates at checkout. Admins set the rules; the system enforces them.
6. **Tax import `replace` mode is destructive.** Always show a confirmation with the count of rules being deleted.
7. **PATCH over PUT for narrow edits.** PUT may clear fields you forgot to include.

---

## 11. Backend gaps that affect the Manager

CRUD for every entity exists. What's missing is multi-entity workflows. See [`api.md` § 13](api.md#13-not-implemented-yet) for the full list; the ones that block Manager features specifically:

- **Variant generator** — `POST /api/v1/products/{id}/generate-variants` would replace the "loop and POST each variant" approach in the product editor.
- **Media uploader** — admins paste URLs today.
- **Webhook → fulfillment listener** — without it, admins manually flip `OrderFulfillment.status` after PayPal-dashboard refunds, chargebacks, etc.
- **Login / refresh-token paths** — confirm before wiring the auth shell.

---

## 12. How to start

1. **Read [`api.md`](api.md)** end-to-end. Scaffold `src/app/models/` from §§ 3–10.
2. Build the **generic `CrudService<T>`** ([§ 11](api.md#11-http-service-blueprint)). Every entity service is a 5-line subclass.
3. **Stand up the auth shell + JWT interceptor**. Verify a 401 redirect works.
4. **Build the polymorphic attribute editor** (§ 7.2). Every later screen leans on it.
5. **Schema editor first** — `AttributeDefinition` + `AttributeOption`. Without these, products can't be created meaningfully.
6. **Categories + Tags.**
7. **Product editor.** The longest single piece of work.
8. **Orders + returns + shipments.**
9. **Rules** (shipping, tax) — relatively quick once the patterns are in place.
10. **Inventory, reviews, polish.**

Defer the AI-assisted polish (auto-tag, auto-categorize) until everything above ships.
