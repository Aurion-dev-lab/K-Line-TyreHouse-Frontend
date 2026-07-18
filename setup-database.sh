#!/bin/bash

# K-Line Tyre House - Database Setup & Validation Script
# This script ensures the MySQL database is properly configured for the invoice system

set -e  # Exit on error

echo "========================================"
echo "K-Line Invoice System - Database Setup"
echo "========================================"
echo ""

# Configuration
DB_HOST="${MYSQL_HOST:-localhost}"
DB_PORT="${MYSQL_PORT:-3306}"
DB_USER="${MYSQL_USER:-root}"
DB_NAME="kline_local"

echo "Database Configuration:"
echo "  Host: $DB_HOST:$DB_PORT"
echo "  User: $DB_USER"
echo "  Database: $DB_NAME"
echo ""

# Test MySQL connection
echo "Testing MySQL connection..."
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -e "SELECT 1;" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✓ MySQL connection successful"
else
    echo "✗ MySQL connection failed"
    echo "Please ensure MySQL is running and accessible"
    exit 1
fi
echo ""

# Create database if not exists
echo "Creating database if not exists..."
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -e "CREATE DATABASE IF NOT EXISTS $DB_NAME;"
echo "✓ Database ready"
echo ""

# SQL script to validate/fix schema
cat > /tmp/validate_schema.sql << 'EOF'
USE kline_local;

-- Ensure invoices table exists with proper engine
CREATE TABLE IF NOT EXISTS invoices (
    id VARCHAR(36) PRIMARY KEY,
    invoice_id VARCHAR(64) UNIQUE,
    customer VARCHAR(255),
    invoice_date DATE,
    type VARCHAR(32),
    status VARCHAR(32),
    subtotal DECIMAL(12,2) NOT NULL DEFAULT 0,
    tax DECIMAL(12,2) NOT NULL DEFAULT 0,
    grand_total DECIMAL(12,2) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
) ENGINE=InnoDB;

-- Ensure invoice_line_items table exists
CREATE TABLE IF NOT EXISTS invoice_line_items (
    id VARCHAR(36) PRIMARY KEY,
    invoice_id VARCHAR(64),
    invoice_ref VARCHAR(36) NOT NULL,
    product_id VARCHAR(36),
    description VARCHAR(255),
    type VARCHAR(32),
    qty INT,
    unit_price DECIMAL(12,2),
    total DECIMAL(12,2),
    created_at DATETIME NOT NULL,
    FOREIGN KEY (invoice_ref) REFERENCES invoices(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Ensure expenses table exists
CREATE TABLE IF NOT EXISTS expenses (
    id VARCHAR(36) PRIMARY KEY,
    expense_date DATE NOT NULL,
    description VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    amount DECIMAL(12,2) NOT NULL,
    created_at DATETIME NOT NULL,
    sync_status BOOLEAN DEFAULT false
) ENGINE=InnoDB;

-- Ensure tyre_exports table exists
CREATE TABLE IF NOT EXISTS tyre_exports (
    id VARCHAR(36) PRIMARY KEY,
    export_id VARCHAR(64),
    operation VARCHAR(32),
    serial_number VARCHAR(255),
    company VARCHAR(255),
    tyres INT,
    cust_price DECIMAL(12,2),
    comp_price DECIMAL(12,2),
    service_fee DECIMAL(12,2),
    paid_amount DECIMAL(12,2),
    total_amount DECIMAL(12,2),
    balance_amount DECIMAL(12,2),
    payment_status VARCHAR(32),
    status VARCHAR(32),
    export_date DATE,
    notes TEXT,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    sync_status BOOLEAN DEFAULT false,
    created_at DATETIME,
    updated_at DATETIME
) ENGINE=InnoDB;

-- Add serial_number column to existing tyre_exports table if it doesn't exist
ALTER TABLE tyre_exports ADD COLUMN IF NOT EXISTS serial_number VARCHAR(255) AFTER operation;

-- Verify columns exist
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS invoice_id VARCHAR(64) UNIQUE;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS customer VARCHAR(255);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS invoice_date DATE;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS type VARCHAR(32);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS status VARCHAR(32);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS subtotal DECIMAL(12,2);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS tax DECIMAL(12,2);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS grand_total DECIMAL(12,2);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS created_at DATETIME;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS updated_at DATETIME;

-- Check if foreign key exists
SELECT COUNT(*) as fk_exists
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_NAME = 'invoice_line_items'
AND COLUMN_NAME = 'invoice_ref'
AND REFERENCED_TABLE_NAME = 'invoices';

EOF

echo "Validating database schema..."
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" < /tmp/validate_schema.sql > /dev/null 2>&1
echo "✓ Schema validated and prepared"
echo ""

# Verify tables exist
echo "Verifying tables..."
TABLES=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME" -e "SHOW TABLES LIKE 'invoices';" 2>&1)
if [[ $TABLES == *"invoices"* ]]; then
    echo "✓ invoices table exists"
else
    echo "✗ invoices table missing"
    exit 1
fi

TABLES=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME" -e "SHOW TABLES LIKE 'invoice_line_items';" 2>&1)
if [[ $TABLES == *"invoice_line_items"* ]]; then
    echo "✓ invoice_line_items table exists"
else
    echo "✗ invoice_line_items table missing"
    exit 1
fi
echo ""

# Show table structures
echo "Table Structures:"
echo ""
echo "invoices table:"
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME" -e "SHOW COLUMNS FROM invoices;" 2>&1 | head -15
echo ""

echo "invoice_line_items table:"
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME" -e "SHOW COLUMNS FROM invoice_line_items;" 2>&1 | head -15
echo ""

# Check row counts
INVOICE_COUNT=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME" -e "SELECT COUNT(*) FROM invoices;" 2>&1 | tail -1)
LINEITEMS_COUNT=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME" -e "SELECT COUNT(*) FROM invoice_line_items;" 2>&1 | tail -1)

echo "Data Summary:"
echo "  Invoices: $INVOICE_COUNT records"
echo "  Line Items: $LINEITEMS_COUNT records"
echo ""

# Cleanup
rm -f /tmp/validate_schema.sql

echo "========================================"
echo "✓ Database setup complete!"
echo "========================================"
echo ""
echo "Next steps:"
echo "1. Start the application"
echo "2. Create a new invoice from Invoice Management"
echo "3. Verify inventory deduction works"
echo ""

