# Tire Shop Business Logic Guide

**For K-Line Tyre House Operations**

## Understanding Your Tire Shop Operations

This guide explains how the invoice system models real tire shop business operations with proper inventory management.

## Transaction Types

### 1. **Tire Sales** (Most Common)

**Scenario**: Customer wants to buy tires

```
┌──────────────────────────────────────┐
│ Transaction: Tire Sale                │
├──────────────────────────────────────┤
│ Step 1: Customer arrives              │
│   Says: "I need 4 tires for my car"   │
│                                       │
│ Step 2: You check inventory           │
│   Michelin 185/65/15: 10 units        │
│   Bridgestone 185/65/15: 5 units      │
│   Budget Tire 185/65/15: 0 units      │
│                                       │
│ Step 3: Create invoice in system      │
│   Type: Sale                          │
│   Product: Michelin 185/65/15         │
│   Quantity: 4                         │
│   Unit Price: Rs. 5,500               │
│   Total: Rs. 22,000                   │
│                                       │
│ Step 4: System automatically:         │
│   - Saves invoice to database         │
│   - Deducts 4 from Michelin stock     │
│   - Updates: 10 → 6                   │
│   - Queues update to server           │
│                                       │
│ Result: ✓ Sale complete, stock = 6   │
└──────────────────────────────────────┘
```

**Real Example**:
```
Invoice#: INV-0001
Date: 25-May-2026
Customer: Roadside Auto Ltd
Vehicle: WP-AES-123 (Truck)

Item 1: Michelin Tire 225/75/16
  Qty: 4
  Rate: 8,500 Rs.
  Amount: 34,000 Rs.

Total: 34,000 Rs.
Tax: 0 Rs. (if applicable)
Grand Total: 34,000 Rs.

✓ Database Status
- Invoice saved to MySQL
- 4 inserted to invoice_line_items
- Michelin 225/75/16 stock: 10 → 6
- Sync queue updated
```

### 2. **Tire Installation/Fitting Service**

**Scenario**: Customer brings tires for installation

```
┌──────────────────────────────────────┐
│ Transaction: Tire Installation        │
├──────────────────────────────────────┤
│ Customer brings tires (8 new ones):   │
│   Service needed: Installation        │
│                                       │
│ Create invoice in system:             │
│   Type: Service                       │
│   Service: Tire Installation (8 tyres)│
│   Labour: 400 Rs./tire = 3,200       │
│   Parts (valves, etc): 800 Rs.       │
│   Discount: 10%                       │
│                                       │
│ Calculation:                          │
│   Subtotal: 3,200 + 800 = 4,000      │
│   Discount: 10% = 400                │
│   Total: 3,600 Rs.                   │
│                                       │
│ System status:                        │
│   ✓ Invoice saved                    │
│   ✓ No inventory changes             │
│     (You're not selling tires,        │
│      customer brought their own)      │
│                                       │
│ Result: Service invoice created       │
└──────────────────────────────────────┘
```

**Real Example**:
```
Invoice#: INV-0002
Date: 25-May-2026
Customer: City Motors
Vehicle: WP-ABC-567 (Car)

Service: Tire Installation & Wheel Balancing
  Labour: 1,200 Rs.
  Parts (Balancing weights): 300 Rs.
  
Subtotal: 1,500 Rs.
Tax: 0 Rs.
Discount: 0%
Grand Total: 1,500 Rs.

✓ Database Status
- Invoice saved to MySQL
- No inventory changes (no products sold)
- Sync queue updated
```

### 3. **Tire + Installation (Combined)** 

**Scenario**: Customer needs new tires AND installation

```
Option 1: Two separate invoices
  Invoice 1: Sale (4 Michelin tires: 22,000)
  Invoice 2: Service (Installation: 1,600)

Option 2: Combined service invoice
  Invoice: Service
  Items: Tire Installation (using customer's tires)
  Labour: 400 × 4 = 1,600
  
Note: Current system is optimized for single-type
      invoices. For combined sales, create two.
```

### 4. **Oil Change Service**

**Scenario**: Customer brings car for oil change

```
Invoice#: INV-0003
Date: 25-May-2026
Customer: Sharma Automobiles
Vehicle: WP-XYZ-999 (Car)

Service: Full Engine Oil Change
  Labour: 300 Rs.
  Parts (Mobil Oil 5L): 1,200 Rs.
  
Subtotal: 1,500 Rs.
Tax: 0 Rs.
Discount: 0%
Grand Total: 1,500 Rs.

✓ Status: Service invoice, no stock impact
```

### 5. **Wheel Alignment Service**

```
Invoice#: INV-0004
Date: 25-May-2026
Customer: Express Logistics
Vehicle: WP-EXP-888 (Truck)

Service: Wheel Alignment
  Labour: 2,000 Rs.
  Parts: 0 Rs. (no parts used)
  
Subtotal: 2,000 Rs.
Tax: 0 Rs.
Discount: 0%
Grand Total: 2,000 Rs.

✓ Status: Service invoice, no stock impact
```

## Inventory Management

### Stock Tracking for Products

```
Product: Michelin Tire 185/65/15
├─ Buy Price: Rs. 4,000
├─ Sell Price: Rs. 5,500
├─ Current Stock: 10 units
└─ Status: Healthy

Tracking:
  Day 1: Stock = 10
  Day 2: Sold 2 units → Stock = 8
  Day 3: Sold 1 unit → Stock = 7
  Day 4: Sold 3 units → Stock = 4
  Day 5: Sold 4 units → Stock = 0 ❌

Status after Day 5:
  - Cannot sell more Michelin 185/65/15
  - System shows error: "Out of stock"
  - Customer must wait for restock
```

### Preventing Overselling

```
Case 1: Normal Stock
  Product: Bridgestone Tire (Stock: 5)
  Customer: Needs 3 units
  System: ✓ Allows creation
  Result: Stock becomes 2

Case 2: Exact Stock
  Product: Budget Tire (Stock: 2)
  Customer: Needs 2 units
  System: ✓ Allows creation
  Result: Stock becomes 0 (Out of Stock)

Case 3: Insufficient Stock
  Product: Premium Tire (Stock: 1)
  Customer: Needs 3 units
  System: ✗ Prevents creation
  Error: "Insufficient stock"
  Customer: Must order more or choose different tire
```

### Real-time Stock Visibility

```
While creating invoice:

Step 1: Click dropdown to select product
  Shows all products available:
    ✓ Michelin Tire (Stock: 10)
    ✓ Bridgestone Tire (Stock: 5)
    ✓ Budget Tire (Stock: 0, disabled)
    ✓ Premium Tire (Stock: 2)

Step 2: Select product
  "Michelin Tire" selected
  Label shows: "Stock: 10 units" ✓

Step 3: System prevents overselling
  If you try to create for 11 units:
  Error appears: "Product out of stock"

Step 4: Save successful
  Creates 10 or fewer, stock updates
  Next time: Stock shows updated value
```

## Cost Analysis & Profitability

### Example 1: Tire Sale Profitability

```
Sales Transaction:
  Product: Michelin Tire
  Buy Price: 4,000 Rs. (cost to you)
  Sell Price: 5,500 Rs. (customer pays)
  Profit per Unit: 1,500 Rs.
  
Transaction:
  Quantity: 4 units
  Total Sale: 4 × 5,500 = 22,000 Rs.
  Total Cost: 4 × 4,000 = 16,000 Rs.
  Profit: 22,000 - 16,000 = 6,000 Rs.
  Margin: 6,000/22,000 = 27.3%
```

### Example 2: Service Profitability

```
Service Transaction:
  Service: Tire Installation
  Labour Rate: 400 Rs. per tire
  Material Cost: 100 Rs. per tire (valves, etc)
  
Transaction:
  Quantity: 4 tires
  Labour: 4 × 400 = 1,600 Rs.
  Material: 4 × 100 = 400 Rs.
  Total Sale: 2,000 Rs.
  
  Profit: 2,000 Rs.
  Margin: 100% (service, no product cost)
```

## Discount Scenarios

### Discount Application

```
Full Service with Discount:

Line Item: Tire Installation + Wheel Balance
  Labour: 1,500 Rs.
  Parts: 500 Rs.
  Subtotal: 2,000 Rs.
  
Apply 10% Discount:
  Discount Amount: 2,000 × 10% = 200 Rs.
  Final Total: 2,000 - 200 = 1,800 Rs.

System Calculation:
  1. Calculate subtotal: labour + parts
  2. Calculate discount: subtotal × discount%
  3. Final: subtotal - discount
```

## Multi-Item Invoices (Future Feature)

### Concept (Not yet implemented)

```
Advanced Scenario:
  Customer buys 4 tires AND gets installation

Current System (Single Type):
  Invoice 1: Sale
    - 4 Michelin Tires: 22,000 Rs.
    - Stock deduction: 4
  
  Invoice 2: Service
    - Installation: 1,600 Rs.
    - No stock change

Combined Future:
  Single Invoice: Mixed
    - Sale item: 4 Michelin Tires (22,000)
    - Service item: Installation (1,600)
    - Total: 23,600 Rs.
    - Stock: Deduct 4 tires

Note: Current system creates separate invoices.
      This keeps sales and services distinct.
```

## Daily Operations Flow

### Morning Opening

```
1. System starts, loads products from inventory
2. Database connection established
3. Invoice list loads (if any from yesterday)
4. Stock levels displayed

Inventory Check:
  ✓ Check which tires are low stock
  ✓ Note which are out of stock
  ✓ Plan restocking if needed
```

### During Business Hours

```
9:00 AM: Customer 1 - Tire Sale
  ├─ Create Sale Invoice
  ├─ Select Michelin Tire
  ├─ Qty: 4, Total: 22,000
  ├─ Stock: 10 → 6
  └─ Invoice saved ✓

10:30 AM: Customer 2 - Tire Fitting Service
  ├─ Create Service Invoice
  ├─ Service: Tire Installation
  ├─ Labour: 400/tire × 4 = 1,600
  ├─ No stock change
  └─ Invoice saved ✓

11:45 AM: Customer 3 - Oil Change
  ├─ Create Service Invoice
  ├─ Service: Oil Change
  ├─ Labour: 300, Parts: 1,200
  ├─ No stock change
  └─ Invoice saved ✓

2:00 PM: CHECK INVENTORY
  ├─ Michelin Tire: 6 units (was 10, 4 sold) ✓
  ├─ Budget Tire: 0 units (need to order)
  ├─ Premium Tire: 8 units (good)
  └─ Make purchase orders if needed
```

### End of Day

```
1. Review all invoices created today
2. Calculate daily revenue
3. Check for any errors
4. Verify inventory matches physical stock
5. Server sync queue status
   - All invoices queued ✓
   - All inventory updates queued ✓
```

## Integration with Existing Systems

### Inventory Management

```
Inventory Page Shows:
  Product Name | Category | Stock | Status
  ────────────────────────────────────────
  Michelin 185 | Tire     | 6     | Normal
  Budget Tire  | Tire     | 0     | ❌ Low
  Bridgestone  | Tire     | 15    | Good
  Oil (5L)     | Parts    | 8     | Normal
  Filters      | Parts    | 12    | Good

When creating invoice:
  Stock is pulled from this same database ✓
  After sale: Stock automatically updates ✓
  You see "10 → 6" in real-time ✓
```

### Customer Management

```
Customer Dropdown in Invoice:
  Automatically populated from:
  - Previous customers
  - Phone numbers tracked
  
Benefits:
  ✓ Quick re-invoicing for repeat customers
  ✓ Phone number automatically filled
  ✓ No re-entry of customer details
  
Note: Service invoices also populate this
```

### Sales Reporting (Future)

```
System will support:
  - Daily sales report
  - Revenue by invoice type
  - Tire sales vs Services
  - Customer transaction history
  - Best-selling products
  - Profitability analysis
```

## Edge Cases & Error Handling

### Out of Stock Scenario

```
Situation: Customer wants item that's out of stock

1. User tries to create invoice
2. System checks: stock == 0
3. Error message appears:
   "Product out of stock. Cannot create invoice."
4. Invoice creation blocked
5. Recommendation:
   - Add stock in Inventory page
   - Choose different product
```

### Negative Stock Prevention

```
Situation: System tries to create qty > stock

Trigger: Stock = 3, User tries qty = 5

System Check:
  new_stock = 3 - 5 = -2
  if (new_stock < 0) {
    throw ERROR("Insufficient stock")
  }

Result: Invoice not created, stock remains 3
```

### Database Connection Failure

```
If MySQL not accessible:
  Error: "Failed to save invoice"
  Cause: Database connection timeout
  
Fix:
  1. Verify MySQL is running
  2. Check connection credentials
  3. Restart application
  4. Retry invoice creation
```

## Reconciliation & Auditing

### Daily Reconciliation

```
✓ Physical Inventory Count:
  You count: Michelin Tire = 5 units

✓ System Shows:
  Database: Michelin Tire = 5 units

✓ Result:
  Stock = Match ✓
  No discrepancy
```

### Discrepancy Handling

```
Scenario: Physical count differs from system

Physical: Michelin Tire = 4 units
System: Michelin Tire = 6 units
Difference: -2 units

Investigation:
  1. Check invoices created today
  2. Verify stock deductions
  3. Look for missing invoices
  4. Manual correction in inventory page

Future: Audit trail to track all changes
```

## Best Practices

### 1. **Invoice Creation**
- ✅ Always verify product before creating
- ✅ Check stock is displayed before saving
- ✅ Use search if customer is returning
- ❌ Don't create invoices for out-of-stock items

### 2. **Inventory Management**
- ✅ End of day: Verify system stock matches physical
- ✅ Weekly: Review low stock items
- ✅ Monthly: Reorder popular items
- ❌ Don't change stock manually unless reconciling

### 3. **Customer Data**
- ✅ Always enter phone number for future lookup
- ✅ Use existing customer names from dropdown
- ✅ Enter vehicle details for reference
- ❌ Don't create duplicate customer entries

### 4. **Service vs Sales**
- ✅ Use "Sale" for products you're selling
- ✅ Use "Service" for services you're providing
- ✅ For mixed transactions, create two invoices
- ❌ Don't mix types in single invoice

## Data Safety & Backup

### Automatic Backups
```
System maintains:
  ✓ Local MySQL database (daily)
  ✓ Server sync queue (immediate)
  ✓ Transaction logs

Recommendation:
  - MySQL backup daily
  - Check sync queue weekly
  - Monitor database size monthly
```

### Invoice Recovery
```
If invoice accidentally deleted:
  - Check server-side backup (if synced)
  - Check MySQL backup (old database)
  - Note: Current system can't recover
  
Prevention:
  - Always confirm before deleting
  - Review your invoices daily
  - Archive old invoices monthly
```

---

## Quick Reference: When to Use What

| Situation | Action | Revenue Impact |
|-----------|--------|-----------------|
| Customer buys tires | Sales Invoice | ↑ Product revenue |
| Customer installs their own tires | Service Invoice | ↑ Labour revenue |
| Customer buys + installs | Sales (prod) + Service (labour) | ↑ Both |
| Customer changes oil | Service Invoice | ↑ Labour + Parts |
| Warranty work | Service Invoice (0 charge) | — |

---

**This guide helps you model your real tire shop business in K-Line's invoice system.**

**Status**: Ready for use ✓  
**Last Updated**: May 27, 2026

