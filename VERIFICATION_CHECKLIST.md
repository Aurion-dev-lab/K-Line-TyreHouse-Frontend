# Invoice System - Setup & Verification Checklist ✅

## Pre-Deployment Verification

Follow this checklist to ensure everything is properly set up and working.

---

## Phase 1: Code & Compilation ✓

- [ ] **Read Files**
  - [ ] Review `INVOICE_SYSTEM_README.md`
  - [ ] Skim `INVOICE_QUICK_START.md`

- [ ] **Verify Compilation**
  ```bash
  mvn clean compile -DskipTests -q
  ```
  - [ ] No compile errors
  - [ ] No critical warnings
  - [ ] Output: `BUILD SUCCESS`

- [ ] **Check Modified Files**
  - [ ] `LocalInvoiceRepository.java` - Enhanced CRUD
  - [ ] `AddInvoiceController.java` - Inventory support
  - [ ] `InvoicesController.java` - Delete/View buttons
  - [ ] `add-invoice-dialog.fxml` - Stock label added

---

## Phase 2: Database Setup

- [ ] **MySQL Verification**
  ```bash
  mysql -u root -e "SELECT VERSION();"
  ```
  - [ ] MySQL running
  - [ ] Version shown (e.g., 5.7.x or 8.0.x)

- [ ] **Database Creation**
  ```bash
  mysql -u root -e "CREATE DATABASE IF NOT EXISTS kline_local;"
  ```
  - [ ] Database created
  - [ ] No errors shown

- [ ] **Run Setup Script** (optional)
  ```bash
  bash setup-database.sh
  ```
  - [ ] Tables created
  - [ ] Schema validated
  - [ ] No errors reported

- [ ] **Verify Tables**
  ```bash
  mysql -u root kline_local -e "SHOW TABLES;"
  ```
  - [ ] See: `invoices`
  - [ ] See: `invoice_line_items`
  - [ ] See: `products`

---

## Phase 3: Application Startup

- [ ] **Start Application**
  ```bash
  mvn javafx:run
  ```
  - [ ] Application window opens
  - [ ] Main menu visible
  - [ ] No database errors in console

- [ ] **Check Inventory Page**
  - Click: "Inventory" in sidebar
  - Verify:
    - [ ] Products list displayed
    - [ ] Stock counts visible
    - [ ] At least one product with stock > 0

- [ ] **Navigate to Invoices**
  - Click: "Invoice Management" in sidebar
  - Verify:
    - [ ] Invoice list appears (may be empty)
    - [ ] Search field visible
    - [ ] "+ New Quatation" button present

---

## Phase 4: Create Sales Invoice

- [ ] **Starting State**
  - [ ] Write down a product name from Inventory (e.g., "Michelin Tire")
  - [ ] Write down its current stock (e.g., "10")

- [ ] **Open New Invoice Dialog**
  - Click: "+ New Quatation"
  - Verify:
    - [ ] Dialog opens with form
    - [ ] Invoice ID auto-generated (e.g., "#INV-0001")

- [ ] **Fill Customer Details**
  - Customer Name: `John Doe` (type new)
  - Phone: `0712345678`
  - Vehicle: `WP ABC-1234`
  - Verify:
    - [ ] All fields populated
    - [ ] No error messages

- [ ] **Select Sale Type**
  - Click: "Invoice Type" dropdown
  - Select: `"Sale"`
  - Verify:
    - [ ] Product dropdown appears
    - [ ] Service description field hidden
    - [ ] Stock label visible but says "—"

- [ ] **Select Product**
  - Click: Product dropdown
  - Select: Product from Inventory (e.g., "Michelin Tire")
  - Verify:
    - [ ] Product selected
    - [ ] Stock label updates (e.g., "Stock: 10 units")
    - [ ] Green indicator (healthy stock)

- [ ] **Leave Amount Fields Blank**
  - [ ] Labour: empty
  - [ ] Parts: empty
  - [ ] Discount: `0`
  - Verify:
    - [ ] Total shows: `0.00`

- [ ] **Save Invoice**
  - Click: "Save Invoice" button
  - Expected:
    - [ ] Dialog closes
    - [ ] Success message appears
    - [ ] Invoice appears in list

- [ ] **Verify Invoice Saved**
  - Check Invoice List:
    - [ ] New invoice visible
    - [ ] Customer: "John Doe"
    - [ ] Type: "Sale"
    - [ ] Items: "1"
    - [ ] Total: recent

- [ ] **Verify Inventory Deducted**
  - Click: "Inventory" tab
  - Find: Product from earlier (e.g., "Michelin Tire")
  - Verify:
    - [ ] Stock decreased by 1
    - [ ] Old: 10 → New: 9 ✓

---

## Phase 5: Create Service Invoice

- [ ] **Create New Invoice**
  - Click: "+ New Quatation"
  - Verify: Dialog opens fresh

- [ ] **Fill Customer Details**
  - Customer: `Jane Smith` (new customer)
  - Phone: `0787654321`
  - Vehicle: `WP XYZ-5678`

- [ ] **Select Service Type**
  - Type: `"Service"`
  - Verify:
    - [ ] Product dropdown hidden
    - [ ] Service description field appears

- [ ] **Fill Service Details**
  - Service: `"Tire Installation"`
  - Labour: `500`
  - Parts: `1500`
  - Discount: `10`
  - Verify:
    - [ ] Total auto-calculates: (500+1500) - 10% = 1800

- [ ] **Save Invoice**
  - Click: "Save Invoice"
  - Verify:
    - [ ] Success message
    - [ ] Invoice appears in list

- [ ] **Verify No Inventory Change**
  - Click: "Inventory"
  - Verify:
    - [ ] All stock unchanged (not 9-1=8)
    - [ ] No product affected ✓

---

## Phase 6: View & Search Invoices

- [ ] **Back to Invoices Tab**
  - Click: "Invoice Management"

- [ ] **Search by Customer Name**
  - Type in Search: `John`
  - Verify:
    - [ ] Shows only John Doe's invoice
    - [ ] Jane Smith's hidden

- [ ] **Clear Search**
  - Clear search field
  - Verify:
    - [ ] Both invoices visible again

- [ ] **View Invoice**
  - Click: "View" button on first invoice
  - Verify Right Panel Shows:
    - [ ] Invoice ID
    - [ ] Customer name
    - [ ] Invoice date
    - [ ] Invoice type
    - [ ] Line items
    - [ ] Totals (Subtotal, Tax, Grand Total)

---

## Phase 7: Delete Invoice

- [ ] **Delete Operation**
  - Click: "✕" (delete) button on any invoice
  - Verify:
    - [ ] Confirmation dialog appears
    - [ ] Shows invoice ID
    - [ ] Asks "Are you sure?"

- [ ] **Confirm Delete**
  - Click: "OK" button
  - Verify:
    - [ ] Invoice removed from list
    - [ ] Success message appears
    - [ ] Right panel clears

- [ ] **Note on Stock**
  - Stock NOT restored (by design)
  - You would need to manually fix inventory

---

## Phase 8: Database Persistence

- [ ] **Verify Data in MySQL**
  ```bash
  mysql -u root kline_local
  ```

- [ ] **Check Invoices Table**
  ```sql
  SELECT invoice_id, customer, type, grand_total 
  FROM invoices 
  ORDER BY created_at DESC LIMIT 5;
  ```
  - [ ] See your created invoices
  - [ ] Correct customer names
  - [ ] Correct types (Sale/Service)

- [ ] **Check Line Items**
  ```sql
  SELECT ili.*, inv.invoice_id 
  FROM invoice_line_items ili
  JOIN invoices inv ON inv.id = ili.invoice_ref
  ORDER BY ili.created_at DESC LIMIT 5;
  ```
  - [ ] See line items for each invoice
  - [ ] Correct descriptions
  - [ ] Correct totals

- [ ] **Check Product Stock**
  ```sql
  SELECT name, stock 
  FROM products 
  WHERE name LIKE '%Michelin%' OR name LIKE '%Budget%';
  ```
  - [ ] Stock values updated
  - [ ] Reflect sales made

---

## Phase 9: Edge Cases Testing

- [ ] **Test Out of Stock**
  - [ ] In Inventory: Set stock to 0 for any product
  - [ ] Try to create sale invoice with that product
  - [ ] Expected: Error "Product out of stock"

- [ ] **Test Missing Fields**
  - [ ] Try saving invoice without customer name
  - [ ] Expected: Error "Customer name is required"
  - [ ] Try without phone
  - [ ] Expected: Error "Phone number is required"

- [ ] **Test Invalid Types**
  - [ ] Try creating invoice without selecting type
  - [ ] Expected: Error or warning

- [ ] **Test Negative Amounts** (if possible)
  - [ ] Enter negative labour value
  - [ ] Verify system handles gracefully

---

## Phase 10: Performance Testing

- [ ] **Create Multiple Invoices**
  - [ ] Create 10 invoices rapidly
  - [ ] Verify:
    - [ ] All save successfully
    - [ ] No lag or delays
    - [ ] All appear in list

- [ ] **Search with Many Records**
  - [ ] Search should be fast (< 1 second)
  - [ ] Filtering works smoothly

- [ ] **Load Large Invoice**
  - [ ] View invoice with many line items
  - [ ] Should load instantly

---

## Phase 11: Sync Queue Verification

- [ ] **Check Sync Queue**
  ```bash
  mysql -u root kline_local
  ```
  ```sql
  SELECT entity_type, status, COUNT(*) as count
  FROM sync_queue
  GROUP BY entity_type, status;
  ```
  - [ ] See "invoice" entries
  - [ ] See "product" entries (for inventory updates)
  - [ ] Status: "pending" (waiting to sync to server)

---

## Phase 12: Documentation Review

- [ ] **Read relevant docs**
  - [ ] `INVOICE_QUICK_START.md` - How to use
  - [ ] `TIRE_SHOP_BUSINESS_LOGIC.md` - Your use case
  - [ ] `INVOICE_SYSTEM_IMPLEMENTATION.md` - Technical details

---

## Final Verification Checklist

- [ ] **Compilation**: ✓ No errors
- [ ] **Database**: ✓ Tables created
- [ ] **App Startup**: ✓ No errors
- [ ] **Sales Invoice**: ✓ Created, stock deducted
- [ ] **Service Invoice**: ✓ Created, no stock change
- [ ] **View**: ✓ Details display
- [ ] **Search**: ✓ Filters work
- [ ] **Delete**: ✓ Works with confirmation
- [ ] **Data Persistence**: ✓ Saved to MySQL
- [ ] **Sync Queue**: ✓ Changes queued
- [ ] **Performance**: ✓ Fast response
- [ ] **Documentation**: ✓ Read and understood

---

## Sign-Off

**System Status**: ✅ READY FOR PRODUCTION

When all checks are complete, you're ready to:
1. Train staff on the system
2. Import historical data (if needed)
3. Go live with invoice management
4. Monitor performance in production

---

## Troubleshooting During Verification

### Issue: Compilation fails
**Solution**: 
```bash
mvn clean -q
rm -rf ~/.m2/repository/com/gui/kline/
mvn compile -DskipTests -q
```

### Issue: Database connection error
**Solution**:
```bash
brew services start mysql  # macOS
mysqld.exe --console      # Windows
systemctl start mysql      # Linux
```

### Issue: Invoice won't save
**Solution**:
- Check all fields are filled
- Verify MySQL is still running
- Check database user has write permissions

### Issue: Stock doesn't decrease
**Solution**:
- Restart application
- Verify product exists in inventory
- Check MySQL for the product record

### Issue: Sync queue not populating
**Solution**:
- Check MySQL sync_queue table exists
- Verify SyncQueueRepository initialization
- Check for errors in application logs

---

## Next Steps After Verification

1. **Train Your Team**: Use INVOICE_QUICK_START.md as training guide
2. **Create Test Data**: Test with various scenarios
3. **Backup Database**: Set up automated backups
4. **Monitor Logs**: Watch for any errors or warnings
5. **Gather Feedback**: From users during testing
6. **Make Adjustments**: Based on real usage patterns
7. **Deploy to Production**: When comfortable

---

## Support Contacts

If issues arise:
1. Check documentation files
2. Review troubleshooting section
3. Check MySQL database directly
4. Check application logs for errors
5. Verify all prerequisites are met

---

**Verification Date**: ____________  
**Verified By**: _______________________  
**Status**: ✅ APPROVED FOR USE


