# Venzora — Checkout Flow

> A plain-English walkthrough of how a purchase moves through Venzora and the `vies-spring-utils` checkout module. Read this once and the moving parts should snap into place. For entity shapes and endpoints, see [`api.md`](api.md). For frontend intent, see [`frontend-client.md`](frontend-client.md) (storefront) or [`frontend-manager.md`](frontend-manager.md) (back-office).

---

## 1. The big idea: two orders, not one

Venzora keeps **two parallel records** of every purchase. They cover different concerns and live in different systems.

| Lives in | What it tracks | Example fields |
|---|---|---|
| **`Cart`** (Venzora) | The shopping bag, pre-purchase. | items, subtotal |
| **`OrderFulfillment`** (Venzora) | The order *the business* cares about: who bought what, where to ship, what state fulfillment is in. | `shippingAddress`, `status` (FulfillmentStatus), `items` (with FK to ProductVariant), `checkoutOrderId` |
| **`CheckoutOrder`** (`vies-spring-utils` library) | The payment record at the provider (PayPal today): order id, amount, approval URL, payment status. | `provider`, `providerOrderId`, `amountTotal`, `status` (CheckoutStatus), `approveUrl` |
| **`CheckoutTransaction`** (library) | The audit log of every money movement on a CheckoutOrder. | `kind` (`CAPTURE` / `REFUND` / …), `amount`, raw provider event |

**The link** that ties them together is one field: `OrderFulfillment.checkoutOrderId` points at `CheckoutOrder.id`.

A useful mental model:

- `OrderFulfillment` is **your order at the warehouse** — what to ship, to whom, what status fulfillment is in.
- `CheckoutOrder` is **the credit-card receipt at the bank** — how much was charged, did it clear, was any of it refunded.

They describe the same purchase from two angles. When you're confused about which side to query, ask yourself: *"Is this a fulfillment / shipping / inventory question, or is this a money question?"*

---

## 2. A worked example — Alice buys a Pro license

Alice browses the catalog, adds a $49.00 Pro license to her cart, and clicks "Checkout." Here is exactly what happens, end to end.

### Step 1 — Build the cart
Alice clicks "Add to cart" on the product page. The frontend hits a Venzora endpoint that writes a `CartItem` linked to her `Cart`. Nothing money-related has happened.

### Step 2 — Create the fulfillment-side order
Alice clicks "Checkout." The frontend calls:

```
POST /api/v1/orders          → creates an OrderFulfillment
```

with shipping/billing address, line items copied from the cart, `totalAmount: "49.00"`, `status: PENDING`, and `checkoutOrderId: null` (not yet linked to the payment side).

### Step 3 — Create the payment-side order
The frontend calls the checkout module:

```
POST /api/v1/checkout/orders/paypal
{
  "currency": "USD",
  "items": [
    { "sku": "PRO-LIC", "name": "Pro license",
      "quantity": 1, "unitPrice": "49.00" }
  ],
  "returnUrl": "https://venzora.app/checkout/return",
  "cancelUrl": "https://venzora.app/checkout/cancel"
}
```

The library forwards to PayPal, receives back a PayPal order id (e.g. `5O190127TN364715T`) and an `approveUrl`. It persists a `CheckoutOrder` row with `status: PENDING_APPROVAL` and returns the row to the frontend.

### Step 4 — Link the two records
The frontend updates the fulfillment:

```
PATCH /api/v1/orders/{orderFulfillmentId}
{ "checkoutOrderId": "<the new CheckoutOrder id>" }
```

Both records now know about each other.

### Step 5 — Send Alice to PayPal
The frontend redirects Alice to the `approveUrl`. She lands on PayPal's site, logs into her PayPal account, clicks "Pay $49.00." PayPal redirects her back to Venzora's `returnUrl`.

> Why the PayPal redirect is mandatory: PayPal's security model requires the buyer to log in *at PayPal* and approve. There is no skipping it. Stripe and most processors have an equivalent redirect-then-return flow.

### Step 6 — Capture the money
Back at Venzora, the frontend immediately calls:

```
POST /api/v1/checkout/orders/paypal/{checkoutOrderId}/capture
```

This is the "actually take the money now" call. The library:
- Tells PayPal to capture the previously-approved order.
- Receives PayPal's confirmation.
- Updates `CheckoutOrder.status` to `CAPTURED`, stamps `capturedAt`.
- Writes a `CheckoutTransaction` of `kind: CAPTURE` to the audit log.

### Step 7 — Show Alice the confirmation
"Thanks for your purchase! Order #VEN-12345."

### Step 8 (in parallel) — PayPal calls Venzora back
Independently of step 6, PayPal sends a webhook event (`PAYMENT.CAPTURE.COMPLETED`) to:

```
POST /api/v1/checkout/webhooks/paypal
```

The library verifies the signature (this is why `PAYPAL_WEBHOOK_ID` matters in production) and writes the same updates as step 6 — but idempotently. If the capture call in step 6 already wrote them, the webhook is a no-op. The webhook is the safety net: if the capture call had timed out client-side, the webhook would still tell the server it happened.

---

## 3. After payment — fulfillment

`OrderFulfillment.status` is still `PENDING` at this point. **Today, nothing automatically flips it** — an admin transitions it through the lifecycle:

```
PENDING → PROCESSING → SHIPPED → DELIVERED
```

When the admin marks `SHIPPED`, they also create a `Shipment` row pointing back at the order via `Shipment.orderFulfillment`. The tracking number and carrier live on the `Shipment`.

The library doesn't know or care about any of this. Shipping is purely Venzora's concern.

---

## 4. If Alice wants a refund

She submits a `ReturnRequest` linked to her `OrderFulfillment` and the specific `OrderFulfillmentItem` being returned. An admin reviews, approves, and:

1. Calls the library's refund endpoint:
   ```
   POST /api/v1/checkout/orders/paypal/{checkoutOrderId}/refund?amount=49&reason=Damaged
   ```
2. The library tells PayPal to refund, writes a `CheckoutTransaction` of `kind: REFUND`, updates `CheckoutOrder.amountRefunded` and `status`.
3. The admin updates `OrderFulfillment.status` to `REFUNDED` (or `PARTIALLY_REFUNDED`).

Both systems stay consistent because the admin operates on both sides.

---

## 5. Mental model — who owns what

| Question | Where to ask |
|---|---|
| Who is Alice? What did she buy? Where does it ship? Is it shipped yet? | **Venzora** (`OrderFulfillment`) |
| Did her card actually clear? How much was charged? Was any of it refunded? | **Library** (`CheckoutOrder`) |
| Show me every dollar that moved on this order. | **Library** (`CheckoutTransaction` audit log) |
| Was this a returned item? Which one? | **Venzora** (`ReturnRequest` + `OrderFulfillmentItem`) |

Rule of thumb: **fulfillment questions → Venzora. Money questions → library.**

---

## 6. The one-shot checkout endpoint *(shipped)*

What the worked example above *should* look like end to end is the one-shot flow — and it is now wired. The frontend's responsibility shrinks to **two calls plus the PayPal redirect**.

### The endpoints

| Path | Body | Returns | Wraps |
|---|---|---|---|
| `POST /api/v1/orders/checkout` | `CheckoutStartRequest` | `{ orderFulfillment, approveUrl }` | Steps 2–4 of the worked example, plus discount validation and stock pre-check, in one transaction. |
| `POST /api/v1/orders/{id}/complete` | *(none)* | `OrderFulfillment` with `status: PROCESSING` | Step 6 (capture) + stock decrement + status flip, in one transaction. |

Both require the `user_id` header (the buyer's UUID). The orchestrator owns all cross-entity validation: cart belongs to buyer, cart is active and non-empty, currencies don't mix, stock is sufficient, discount is valid.

### Worked example, revised

Steps 2, 3, and 4 of the original walkthrough collapse into a single call:

```http
POST /api/v1/orders/checkout
user_id: <buyer's UUID>
Content-Type: application/json

{
  "cartId":          "0193de1a-...",
  "shippingAddress": { "street": "...", "city": "...", "type": "SHIPPING" },
  "billingAddress":  { "street": "...", "city": "...", "type": "BILLING" },
  "discountCode":    "SAVE10",
  "provider":        "paypal",
  "returnUrl":       "https://venzora.app/checkout/return",
  "cancelUrl":       "https://venzora.app/checkout/cancel"
}
```

```json
200 OK
{
  "orderFulfillment": { "id": "...", "checkoutOrderId": "...", "status": "PENDING", "totalAmount": "49.00", ... },
  "approveUrl":       "https://www.paypal.com/checkoutnow?token=5O190127TN364715T"
}
```

The frontend redirects Alice to `approveUrl`. After she returns, step 6 collapses to:

```http
POST /api/v1/orders/{orderFulfillmentId}/complete
user_id: <buyer's UUID>
```

```json
200 OK
{ "id": "...", "status": "PROCESSING", ... }
```

### What the orchestrator does atomically

`start()` — in one `@Transactional`:

1. Validates request fields and `user_id`.
2. Loads the `Cart`, checks it belongs to the buyer and is active.
3. Enforces single currency across all line items (no mixed-currency carts).
4. Pre-checks stock against `ProductVariant.stockQuantity`.
5. Sums the subtotal, validates the discount (`active`, `currentUses < maxUses`, `subtotal >= minimumOrderAmount`), computes the discount amount.
6. Builds the `CheckoutCreateOrderRequest` (currency, line items, metadata `{ cartId }`, return/cancel URLs).
7. Calls `CheckoutProviderRegistry.orderService(provider).createOrder(req, buyerId)` — library hits PayPal, persists `CheckoutOrder`, returns it with `approveUrl`.
8. Creates the `OrderFulfillment` in `PENDING` with full line items and `checkoutOrderId` set.
9. Bumps `Discount.currentUses` (if a discount was applied).
10. Marks the `Cart` inactive.

If any step throws, Spring rolls back the whole transaction. No orphaned rows.

`complete()` — in one `@Transactional`:

1. Loads the `OrderFulfillment`, validates ownership, status (`PENDING`), and that `checkoutOrderId` is set.
2. Calls `captureOrder(checkoutOrderId)` on the library — funds move.
3. Decrements `ProductVariant.stockQuantity` for each line item; throws if any would go negative (race detection).
4. Flips `OrderFulfillment.status` to `PROCESSING`.

### Caveats

- **The PayPal side is not rolled back if our DB write fails after.** If `createOrder` succeeds at PayPal but the subsequent `OrderFulfillment` insert fails, the PayPal order lingers as an orphan. Bounded but real. A future refinement could call `cancelOrder` (when the library exposes it) in the `@Transactional` rollback hook.
- **Tax uses self-hostable `TaxRule` entries** — admins define `{ country, state, city, postalCode, rate }` rules; the orchestrator picks the most specific match for the shipping address via `TaxCalculator`. No rule → zero tax. Import/export endpoints let admins ship a rule set as JSON. See [`api.md` § 7.11](api.md#711-taxrule).
- **Shipping uses `ShippingRule` per currency** — flat fee with optional free-above threshold. Missing or inactive rule for a currency means free shipping with a warning log.
- **Stock checks are not locking.** Two simultaneous buyers can both pass the pre-check; the `complete()` step's negative-stock guard catches the loser. For high-contention SKUs, add `@Version` to `ProductVariant` (clean) or a reservation entity (heavier).
- **Provider is taken from the request body.** Today only `"paypal"` is registered; passing anything else returns 503 via the library's registry lookup.

### Discount validation preview

The frontend's "Apply code" UX should call **`POST /api/v1/discounts/validate`** (not `/orders/checkout`) to test a code without committing. Returns `{ valid, discountAmount, reason }` at HTTP 200 — business rejections come back in the body, not as exceptions. See [`api.md` § 3.2.5a](api.md#325a-discount-validation--post-apiv1discountsvalidate).

---

## 7. The other missing piece — webhook → fulfillment listener

When the webhook in step 8 fires (`PAYMENT.CAPTURE.COMPLETED`), the library updates the `CheckoutOrder` but **does not touch `OrderFulfillment`** — it doesn't even know that record exists. So today the only thing that flips `OrderFulfillment.status` from `PENDING` to `PROCESSING` is an admin doing it by hand.

A small custom `@Service` subclassing or wrapping `PaypalCheckoutOrderService` could:

- Receive each transition (`CAPTURED`, `REFUNDED`, `PARTIALLY_REFUNDED`).
- Look up the matching `OrderFulfillment` by `checkoutOrderId`.
- Flip its status accordingly.

This is independent of the one-shot endpoint and could ship separately. It would make the post-purchase status auto-track payment events without admin work.

---

## 8. Subscriptions — deferred

The checkout module also ships `CheckoutPlan` and `CheckoutSubscription` for recurring billing. **Venzora has explicitly disabled those controllers** in `BeanConfig` for now (`setEnabledCheckoutSubscriptionController(false)`, `setEnabledCheckoutPlanController(false)`). If subscription pricing enters scope later, flip those flags to `true`, design the plan-management UX, and decide how subscriptions interact with `OrderFulfillment` (one fulfillment per renewal? a single subscription record?).

---

## 9. Frontend cheat sheet

Per purchase:

1. `POST /api/v1/orders/checkout` → server returns `{ orderFulfillment, approveUrl }`.
2. Redirect the buyer to `approveUrl`.
3. (Buyer approves at PayPal, returns to your `returnUrl`.)
4. `POST /api/v1/orders/{orderFulfillment.id}/complete` → server returns the updated `OrderFulfillment` with `status: PROCESSING`.
5. Show the confirmation page.

That's it. Cart deactivation, stock decrement, discount usage bump, and `checkoutOrderId` linking all happen server-side. The frontend never touches `/api/v1/checkout/orders/...` directly for a normal checkout.

If `complete` fails (rare — usually means PayPal rejected the capture or the network dropped), the webhook listener will eventually heal the state when PayPal retries the event. Show a clean error and let the user refresh.
