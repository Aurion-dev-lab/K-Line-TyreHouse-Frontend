# Invoice and Billing System - Quick Start Guide

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Invoice & Billing                         │
└─────────────────────────────────────────────────────────────────┘
                                  │
                ┌─────────────────┼─────────────────┐
                ▼                 ▼                 ▼
          ┌──────────┐      ┌──────────┐      ┌──────────┐
          │  Sales   │      │ Service  │      │  Track  │
          │ Invoice  │      │ Invoice  │      │ Sync    │
          └──────────┘      └──────────┘      └──────────┘
                │                 │                 │
                ▼                 ▼                 ▼
          ┌──────────────────────────────────────────────┐
          │      Local Database (MySQL)                  │
          │  ┌─────────────┐    ┌──────────────────┐   │
          │  │  invoices   │    │invoice_line_items│   │
          │  │  products   │    │  sync_queue      │   │
          │  └─────────────┘    └──────────────────┘   │
          └──────────────────────────────────────────────┘
                    │
                    ▼
          ┌──────────────────┐
          │ Sync to Server   │
          │ (Background)     │
          └──────────────────┘
```

## Step-by-Step Workflow

### Creating a Sales Invoice with Inventory Deduction

```
Step 1: Initialize Invoice
  ┌─ Click "+ New Quotation"
  ├─ Right panel enables
  └─ User can select products

Step 2: Add Line Items
  ┌─ Select "Sales" type
  ├─ Choose product from dropdown (loads from DB)
  ├─ Enter Quantity
  ├─ Enter Unit Price
  ├─ Click "+ Add Line"
  └─ Repeat as needed

Step 3: Generate Invoice
  ┌─ Click "Generate Invoice"
  ├─ System validates:
  │  ├─ Invoices not empty
  │  └─ At least 1 item
  │
  ├─ For each Sales item:
  │  ├─ Find product in inventory
  │  ├─ Check stock vs qty
  │  ├─ Deduct quantity
  │  ├─ Save new stock to DB
  │  └─ Enqueue product update
  │
  ├─ Save invoice to DB
  ├─ Enqueue invoice payload
  └─ Show success message

Step 4: View Inventory Update
  ┌─ Switch to Inventory tab
  ├─ Products load from DB
  ├─ Updated stock reflected
  ├─ Low stock warnings updated
  └─ Sync queue processes in background
```

## Data Flow for Sales Invoice

```
User Input
    │
    ├─ Product Selection
    │  └─ Query: SELECT * FROM products WHERE name = ?
    │
    ├─ Quantity & Amount
    │  └─ Create LineItem(product, qty, amount, productId)
    │
    └─ Generate
        │
        ├─ INVENTORY DEDUCTION LOGIC
        │  ├─ Get current stock
        │  ├─ Calculate: newStock = current - qty
        │  ├─ Validate: newStock >= 0
        │  └─ Update: UPDATE products SET stock = ?
        │
        ├─ INVOICE PERSISTENCE
        │  ├─ Save: INSERT INTO invoices (...)
        │  └─ Save: INSERT INTO invoice_line_items (...)
        │
        └─ SYNC QUEUE
            ├─ Enqueue: product update (inventory change)
            ├─ Enqueue: invoice creation
            └─ Status: PENDING → SYNCED
```

## Database Operations

### Invoice Creation SQL Sequence

```sql
-- 1. Save invoice
INSERT INTO invoices (
    id, invoice_id, customer, invoice_date, type, 
    status, subtotal, tax, grand_total, created_at, updated_at
) VALUES (UUID(), ?, ?, ?, ?, 'completed', ?, ?, ?, NOW(), NOW());

-- 2. For each line item
INSERT INTO invoice_line_items (
    id, invoice_id, product_id, description, type, 
    qty, unit_price, total, created_at
) VALUES (UUID(), ?, ?, ?, ?, ?, ?, ?, NOW());

-- 3. Update product stock (for Sales items only)
UPDATE products SET stock = ?, updated_at = NOW() WHERE id = ?;

-- 4. Enqueue sync for inventory
INSERT INTO sync_queue (id, entity_type, payload, status, created_at)
VALUES (UUID(), 'product', JSON_OBJECT(...), 'PENDING', NOW());

-- 5. Enqueue sync for invoice
INSERT INTO sync_queue (id, entity_type, payload, status, created_at)
VALUES (UUID(), 'invoice', JSON_OBJECT(...), 'PENDING', NOW());
```

## Key Features

### ✅ Sales Invoices
- Select from existing products
- Automatic inventory deduction
- Track product stock changes
- Maintain full audit trail

### ✅ Service Invoices
- Free-form service descriptions
- No inventory impact
- Flexible pricing
- Quick documentation

### ✅ Inventory Management
- Real-time stock updates
- Low stock warnings
- Sync with server
- Automatic product saves

### ✅ Sync Integration
- Offline-first approach
- Queue-based synchronization
- Automatic retry on failure
- Device ID tracking

## Error Handling

```
Scenario 1: Insufficient Stock
  └─ User sells 10 units but only 5 in stock
     ├─ Check: newStock = 5 - 10 = -5
     ├─ Validation: -5 < 0 ✗ FAIL
     └─ Action: Show error message & cancel operation

Scenario 2: Invalid Input
  ├─ Empty quantity field
  ├─ Negative price
  ├─ No product selected (Sales)
  ├─ Empty service description (Service)
  └─ Action: Show validation error

Scenario 3: Database Error
  ├─ SQL execution fails
  ├─ Transaction rolled back
  └─ Action: Show error with details
```

## Performance Considerations

| Operation | Time |  Notes |
|-----------|------|--------|
| Load products | <100ms | Cached in HashMap |
| Deduct inventory | <50ms | Single UPDATE query |
| Generate invoice | <200ms | Full transaction |
| Enqueue sync | <10ms | Memory operation |
| Sync to server | Variable | Background process |

## Testing Checklist

- [ ] Create sales invoice with 1 item → verify stock decreases
- [ ] Create sales invoice with 5 items → verify stock for all updates
- [ ] Attempt sale with insufficient stock → verify error message
- [ ] Create service invoice → verify inventory untouched
- [ ] Switch to Inventory → verify updated stock shown
- [ ] Check sync queue → verify 2 payloads enqueued
- [ ] Verify database records created for all items
- [ ] Test with different product types/categories
- [ ] Test invoice with 0 quantity → should error
- [ ] Test negative price → should error

## Color Scheme (Improved Visibility)

| Element | Color | Usage |
|---------|-------|-------|
| Primary Button | #06b6d4 | Actions (cyan) |
| Success | #10b981 | Positive amounts (green) |
| Warning | #f97316 | Low stock (orange) |
| Error | #ef4444 | Errors (red) |
| Background | #f8fafc | Light, clean |
| Panel | #1e293b | Dark blue-gray |
| Text | #334155 | Good contrast |
| Border | #cbd5e1 | Subtle separation |

## Integration Points

### ← From Inventory Controller
- Product list (names & IDs)
- Stock quantities
- Product details

### → To Inventory Controller  
- Updated stock quantities
- Product status changes
- Low stock indicators

### ← From Sync System
- Device ID
- Sync queue access
- Payload formatting

### → To Sync System
- Product updates
- Invoice records
- Sync payloads

## Next Steps

1. **Test the system** with sample data
2. **Monitor sync** queue for successful uploads
3. **Verify inventory** reflects changes across all tabs
4. **Check accounting** for profit calculations
5. **Export functionality** for reporting

---

For detailed implementation, see: `INVOICE_BILLING_IMPLEMENTATION.md`

