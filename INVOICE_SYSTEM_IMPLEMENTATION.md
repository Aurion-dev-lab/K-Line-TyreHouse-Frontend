# Invoice & Billing System - Complete Implementation Guide

## Overview
This document describes the complete invoice and billing system implementation for K-Line Tyre House, including inventory management, CRUD operations, and the real-world tire shop business logic.

## System Architecture

### Database Schema
The system uses three main tables:

#### 1. `invoices` Table
```sql
CREATE TABLE invoices (
  id VARCHAR(36) PRIMARY KEY,
  invoice_id VARCHAR(64) UNIQUE,
  customer VARCHAR(255),
  invoice_date DATE,
  type VARCHAR(32),           -- 'Sale' or 'Service'
  status VARCHAR(32),         -- 'completed', 'draft', 'cancelled'
  subtotal DECIMAL(12,2),
  tax DECIMAL(12,2),
  grand_total DECIMAL(12,2),
  created_at DATETIME NOT NULL,
  updated_at DATETIME
) ENGINE=InnoDB;
```

#### 2. `invoice_line_items` Table
```sql
CREATE TABLE invoice_line_items (
  id VARCHAR(36) PRIMARY KEY,
  invoice_id VARCHAR(64),     -- External reference
  invoice_ref VARCHAR(36) NOT NULL,  -- FK to invoices.id
  product_id VARCHAR(36),     -- FK to products.id (NULL for services)
  description VARCHAR(255),   -- Product/Service name
  type VARCHAR(32),           -- 'Sale' or 'Service'
  qty INT,
  unit_price DECIMAL(12,2),
  total DECIMAL(12,2),
  created_at DATETIME NOT NULL,
  FOREIGN KEY (invoice_ref) REFERENCES invoices(id) ON DELETE CASCADE
) ENGINE=InnoDB;
```

#### 3. `products` Table (for inventory)
```sql
CREATE TABLE products (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE,
  category VARCHAR(128),
  buy_price DECIMAL(12,2),
  sell_price DECIMAL(12,2),
  stock INT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL
);
```

## Business Logic

### Invoice Types

#### 1. **Sales Invoice**
- Customer buys a product (e.g., tire, battery, oil)
- Product must have stock available
- Stock is **automatically deducted** when invoice is saved
- Inventory updates are synced to server

Example:
```
Invoice: INV-001
Customer: John Doe
Type: Sale
Item: Michelin 185/65/15 Tire
Qty: 1
Unit Price: Rs. 5,500.00
Total: Rs. 5,500.00
```

#### 2. **Service Invoice**
- Customer gets a service (e.g., tire fitting, wheel balancing, oil change)
- No inventory deduction
- Labour and Parts are tracked separately

Example:
```
Invoice: INV-002
Customer: Jane Smith
Type: Service
Service: Full Engine Service + Oil Change
Labour: Rs. 1,500.00
Parts: Rs. 2,500.00
Discount: 10%
Total: Rs. 3,600.00
```

### CRUD Operations

#### Create Invoice (AddInvoiceController)
1. User clicks "New Quatation" button
2. Dialog opens with form fields:
   - Customer Name (editable combo box for existing customers)
   - Phone Number
   - Vehicle Number
   - Invoice Type (Sale/Service radio)
   - Product Selection (for Sales) or Service Description (for Services)
   - Labour Amount (if Service)
   - Parts Amount (if Service)
   - Discount Percentage
3. User fills form and clicks "Save Invoice"
4. System:
   - Validates all inputs
   - Checks inventory availability (for Sales)
   - Saves invoice to database
   - Deducts inventory and enqueues sync (for Sales)
   - Saves/updates customer record
   - Shows success message

#### Read Invoice (InvoicesController)
1. User views invoice list in main table
2. User clicks "View" button to see invoice details
3. Right panel shows:
   - Invoice ID
   - Customer Name
   - Invoice Date
   - Invoice Type
   - Line Items
   - Subtotal, Tax, Grand Total
4. Details are read-only (no editing)

#### Update Invoice (Future Enhancement)
- Currently, edit mode is disabled to prevent inventory conflicts
- When enabled: system would need to:
  1. Reverse inventory deductions from old items
  2. Apply new inventory deductions for new items
  3. Handle partial updates carefully

#### Delete Invoice
1. User clicks "Delete" button on invoice row
2. Confirmation dialog appears
3. On confirmation:
   - Invoice line items deleted
   - Invoice deleted from database
   - **Note**: Inventory NOT restored (can be added later if needed)
4. Success message shown

### Inventory Management

#### Stock Deduction Flow
1. User creates Sales invoice
2. System checks if product has stock
3. On save:
   ```java
   int newStock = product.getStock() - quantity;
   if (newStock < 0) throw exception("Out of stock");
   product.setStock(newStock);
   catalogRepository.saveProduct(product);  // Update DB
   ```
4. Stock update queued for server sync
5. Inventory UI updated (if viewing inventory page)

#### Stock Display
- Product combo box shows real-time stock availability
- When user selects product, stock label updates: "Stock: 5 units"
- Prevents selection of out-of-stock items

## UI/UX Improvements

### Invoice Creation Dialog
```
┌─────────────────────────────────────────┐
│  🗙  New Invoice              #INV-0001  │
├─────────────────────────────────────────┤
│ CUSTOMER DETAILS                        │
│  Customer Name:        Phone Number:    │
│  [John Doe       ]     [07XXXXXXXX   ]  │
│  Vehicle Number:       Invoice Type:    │
│  [WP ABC-1234    ]     [Sale ▼]         │
├─────────────────────────────────────────┤
│ INVOICE DETAILS                         │
│  Product/Service  Stock: 5 units        │
│  [Michelin Tire ▼]                      │
├─────────────────────────────────────────┤
│ BILLING DETAILS                         │
│  Labour: [1500]  Parts: [2500]  Disc: [10]%
├─────────────────────────────────────────┤
│ TOTAL AMOUNT: Rs. 3,600.00              │
├─────────────────────────────────────────┤
│                        [Cancel] [Save]  │
└─────────────────────────────────────────┘
```

### Invoice List & Details
```
Invoice Management
────────────────────────────────────────
Date      Customer       Type    Items Total
─────── ───────────── ────── ────── ─────
5/25    John Doe      Sales    1   5500
5/24    Jane Smith    Service  1   3600
5/23    Bob Wilson    Sales    2   8900

[Left Panel: Invoice List]     [Right Panel: Details]
                              Invoice Detail    #INV-001
                              Customer: John Doe
                              Date: 2026-05-25
                              Type: Sales
                              ─────────────────
                              LINE ITEMS
                              Michelin Tire
                              1 × Rs. 5500.00
                              
                              Subtotal: Rs. 5500.00
                              Tax: Rs. 0.00
                              Grand Total: Rs. 5500.00
                              
                              [View] [Delete]
```

## Key Features

### 1. **Automatic Inventory Deduction**
- When a Sales invoice is created, product stock decreases automatically
- System prevents creating invoices for out-of-stock items
- Stock updates are synced to server

### 2. **Validation & Error Handling**
- Customer name required
- Phone number required
- Vehicle number required
- For Sales: Product must be selected and in stock
- For Services: Description required
- Amount must be > 0
- Clear error messages for each validation failure

### 3. **Real-time Stock Display**
- When product selected, stock shows: "Stock: 5 units"
- Out-of-stock products cannot be selected
- Stock displays in green (healthy) or red (low stock)

### 4. **Invoice Management**
- Search invoices by customer name, date, or invoice ID
- View complete invoice details
- Delete invoices with confirmation
- All operations update database and sync queue

### 5. **Customer Management**
- Auto-save customer on invoice creation
- Populate customer dropdown from previous invoices
- Track customer phone numbers

## Implementation Details

### AddInvoiceController
- Handles invoice creation dialog
- Shows product stock availability
- Validates all fields before save
- Deducts inventory automatically for sales
- Syncs invoice to server

### InvoicesController
- Displays invoice list with search
- Shows invoice details panel
- Handles invoice deletion
- Manages edit mode state
- Updates UI based on user actions

### LocalInvoiceRepository
- CRUD operations on invoices table
- Manages line items
- Handles invoice detail loading
- Supports invoice search and filtering
- Provides statistics (count, revenue)

### LocalCatalogRepository
- Loads products with inventory info
- Finds products by ID
- Updates product stock
- Saves/updates product records

## Testing Patterns

### Test Scenario 1: Create Sales Invoice
1. Open Invoice Management
2. Click "New Quatation"
3. Fill in customer details
4. Select "Sale" type
5. Choose product (verify stock shows)
6. Click Save
7. Verify:
   - Invoice appears in list
   - Product stock decreased
   - Success message shown

### Test Scenario 2: Create Service Invoice
1. Open Invoice Management
2. Click "New Quatation"
3. Fill in customer details
4. Select "Service" type
5. Enter service description
6. Enter labour and parts amounts
7. Click Save
8. Verify:
   - Invoice appears in list
   - No inventory change
   - Success message shown

### Test Scenario 3: Delete Invoice
1. View invoice list
2. Click Delete button
3. Confirm deletion
4. Verify:
   - Invoice removed from list
   - Success message shown

## Future Enhancements

1. **Edit Invoice**
   - Enable editing of existing invoices
   - Reverse inventory on edit
   - Update line items

2. **Invoice Printing**
   - Generate PDF invoice
   - Print-friendly layout

3. **Payment Tracking**
   - Record payment method
   - Track partial payments
   - Generate receipts

4. **Tax Management**
   - Configurable tax rates
   - Tax calculations

5. **Reports**
   - Daily sales report
   - Customer purchase history
   - Inventory usage analysis
   - Revenue tracking

6. **Discount Management**
   - Named discount codes
   - Bulk discounts
   - Customer-specific discounts

## Database Maintenance

### Ensure Schema Exists
The DatabaseManager.init() method automatically creates all required tables with proper engine types and constraints.

### Verify InnoDB Engine
```sql
SHOW CREATE TABLE invoices;
SHOW CREATE TABLE invoice_line_items;
```

All tables must use InnoDB for foreign key support.

### Enable Foreign Key Checks
```sql
SET FOREIGN_KEY_CHECKS=1;
```

## API/Service Integration

### Sync Queue
Invoices and inventory updates are added to sync_queue for server synchronization:

```json
{
  "entity_type": "invoice",
  "payload": {
    "invoiceId": "INV-001",
    "date": "2026-05-25",
    "customer": "John Doe",
    "type": "Sale",
    "total": 5500.00
  }
}
```

## Troubleshooting

### Issue: "Insufficient stock" error
- **Cause**: Product has 0 or negative stock
- **Fix**: Update inventory in Inventory management page

### Issue: "Unknown column" error
- **Cause**: Database schema incomplete
- **Fix**: Run DatabaseManager.init() to create missing columns

### Issue: Invoice not saving
- **Cause**: Database connection error
- **Fix**: Verify MySQL is running and accessible

### Issue: Stock not updating
- **Cause**: Inventory repository not called
- **Fix**: Restart application to reload product map

## Code Examples

### Creating an Invoice
```java
// 1. Create detail object
InvoiceDetail detail = new InvoiceDetail();
detail.setInvoiceId("INV-001");
detail.setCustomer("John Doe");
detail.setDate(LocalDate.now().toString());
detail.setType("Sale");

// 2. Add line items
LineItem item = new LineItem("Michelin Tire", "Sale", 1, 5500, productId);
detail.addLineItem(item);

// 3. Create row
InvoiceRow row = new InvoiceRow("INV-001", LocalDate.now().toString(),
    "John Doe", "Sale", 1, 5500);

// 4. Save
invoiceRepository.saveInvoice(detail, row);

// 5. Deduct inventory
deductInventory(detail);  // Automatic in current implementation
```

### Loading an Invoice
```java
InvoiceDetail detail = invoiceRepository.loadInvoiceDetail("INV-001");
// Detail now contains all line items and metadata
```

---

**Version**: 1.0  
**Last Updated**: May 25, 2026  
**Status**: Complete & Ready for Testing

