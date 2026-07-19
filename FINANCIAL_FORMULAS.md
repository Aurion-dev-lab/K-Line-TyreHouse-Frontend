# K-Line TyreHouse — Financial & Salary Calculation Formulas

> **Last Updated:** July 19, 2026
> **Source Code Reference:** Analysis of controllers, models, and repositories in the K-Line TyreHouse Frontend system.

---

## Table of Contents

1. [Worker Attendance Calculation](#1-worker-attendance-calculation)
2. [Worker Salary Calculation](#2-worker-salary-calculation)
3. [Salary Payment Processing](#3-salary-payment-processing)
4. [Sales (Revenue) Calculation](#4-sales-revenue-calculation)
5. [Profit Calculation](#5-profit-calculation)
6. [Net Income (Net Profit) Calculation](#6-net-income-net-profit-calculation)
7. [Dashboard KPI Calculation](#7-dashboard-kpi-calculation)
8. [Summary of Key Relationships](#8-summary-of-key-relationships)

---

## 1. Worker Attendance Calculation

Each worker has daily attendance records stored in the `worker_attendance` database table with one of three statuses:

| Status        | Meaning        | Weight (Days) |
|---------------|----------------|---------------|
| `PRESENT`     | Full day worked     | **1.0** |
| `HALF_DAY`    | Half day worked     | **0.5** |
| `ABSENT`      | Not present         | **0.0** |

**Counts are derived via SQL** (from `LocalSalaryRepository.java`, lines 28–35):

```sql
SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present,
SUM(CASE WHEN a.status = 'HALF_DAY' THEN 1 ELSE 0 END) AS half_day,
SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absent
```

---

## 2. Worker Salary Calculation

### Gross Salary

**Formula** (from `LocalSalaryRepository.java`, line 64):

```
Gross Salary = (present + halfDay × 0.5) × dailyRate
```

Where:
- `present` = number of days marked PRESENT
- `halfDay` = number of days marked HALF_DAY
- `dailyRate` = the worker's daily rate stored in the `workers.rate` field

### Deductions

| Deduction | Source | How It's Computed |
|-----------|--------|-------------------|
| **Advances** | `salary_advances` table | `SUM(advances)` for this worker in the payroll period |
| **Credit Balance** | `worker_credit` table | `MAX(0, credit_given - credit_settlements)` for this worker in the period |
| **Paid Amount** | `salary_payments` table | `SUM(amount)` already paid for this worker/period |

**Code** (from `LocalSalaryRepository.java`, lines 44–68):

```java
double gross = (present + (halfDay * 0.5)) * rate;
double advances = advancesById.getOrDefault(workerId, 0.0);
double creditBalance = Math.max(0, creditsById.getOrDefault(workerId, 0.0));
double paidAmount = paidAmountsByWorkerId.getOrDefault(workerId, 0.0);
```

### Net Payable

**Formula** (from `WorkerSalary.java`, line 29):

```
Net Payable = MAX(0, Gross Salary - Advances - Credit Balance)
```

### Remaining Payable

**Formula** (from `WorkerSalary.java`, line 31):

```
Remaining Payable = MAX(0, Net Payable - Paid Amount)
```

### Payment Status

**Logic** (from `LocalSalaryRepository.java`, lines 157–162):

| Condition | Status |
|-----------|--------|
| `netPayable ≤ 0` | `NO PAYABLE` |
| `paidAmount ≥ netPayable` | `PAID` |
| `paidAmount > 0` | `PARTIALLY PAID` |
| Otherwise | `READY` |

---

## 3. Salary Payment Processing

When a salary payment is made (from `SalaryController.java`, lines 581–616):

1. User enters a payment amount
2. The system validates: `alreadyPaid + newPayment ≤ totalPayable`
3. A new record is inserted into `salary_payments` table
4. The payment is synced to the cloud via `sync_queue`

Each payment has an ID, worker reference, period range, amount, and timestamp.

---

## 4. Sales (Revenue) Calculation

**Total Revenue** is the sum of **5 revenue streams** (from `DashboardController.java`, lines 726–745 & `ReportsRepository.java`, lines 416–419):

| # | Revenue Source | Database Table | Formula |
|---|---------------|----------------|---------|
| 1 | **Invoice Sales** | `invoices` (status = 'completed') | `SUM(grand_total)` |
| 2 | **Credit Sales** | `credit_sales` | `SUM(COALESCE(subtotal, amount))` |
| 3 | **Service Revenue** | `services` | `SUM(price)` |
| 4 | **Quick Service Revenue** | `quick_services` | `SUM(price)` |
| 5 | **Tyre Export Revenue** | `tyre_exports` | `SUM(total_amount)` |

### Formula:

```
Total Revenue = Invoice Sales + Credit Sales + Services + Quick Services + Tyre Exports
```

### Detailed Breakdown

| Source | SQL Query Snippet |
|--------|-------------------|
| Invoices | `SELECT COALESCE(SUM(grand_total),0) FROM invoices WHERE status = 'completed' AND invoice_date BETWEEN ? AND ?` |
| Credit Sales | `SELECT COALESCE(SUM(COALESCE(subtotal, amount)),0) FROM credit_sales WHERE sale_date BETWEEN ? AND ?` |
| Services | `SELECT COALESCE(SUM(price),0) FROM services WHERE service_date BETWEEN ? AND ?` |
| Quick Services | `SELECT COALESCE(SUM(price),0) FROM quick_services WHERE service_date BETWEEN ? AND ?` |
| Tyre Exports | `SELECT COALESCE(SUM(total_amount),0) FROM tyre_exports WHERE export_date BETWEEN ? AND ?` |

---

## 5. Profit Calculation

### Per-Item Profit (Invoice Line Item)

**Formula** (from `ReportsRepository.java`, line 33):

```
Per-Item Profit = (unit_price - buy_price) × qty
```

Where:
- `unit_price` = selling price per unit
- `buy_price` = cost price from the `products` table
- `qty` = quantity sold

### Total Profit (Dashboard)

**Formula** (from `DashboardController.java`, lines 748–778):

```
Profit = Invoice Profit
       + Credit Sales Profit
       + Service Profit
       + Quick Service Profit
       + Tyre Export Profit
       - Paid Salaries
       - General Expenses
```

| Component | Formula | Table(s) |
|-----------|---------|----------|
| **Invoice Profit** | `SUM(qty × (unit_price - buy_price))` | `invoice_line_items` JOIN `products` |
| **Credit Sales Profit** | `SUM(subtotal or amount)` — treated as full margin | `credit_sales` |
| **Service Profit** | `SUM(price)` — full amount is profit (no COGS) | `services` |
| **Quick Service Profit** | `SUM(price)` — full amount is profit (no COGS) | `quick_services` |
| **Tyre Export Profit** | `SUM((custPrice - compPrice) × tyres + serviceFee)` | `tyre_exports` |
| **Paid Salaries** | `SUM(amount)` | `salary_payments` |
| **General Expenses** | `SUM(amount)` | `expenses` |

### Tyre Export Profit Detail

**Formula** (from `DashboardController.java`, line 788):

```
Tyre Export Profit = (custPrice - compPrice) × tyres + serviceFee
```

Where:
- `custPrice` = price charged to customer per tyre
- `compPrice` = price paid to supplier (company) per tyre
- `tyres` = number of tyres in the export
- `serviceFee` = additional service fee charged

---

## 6. Net Income (Net Profit) Calculation

### Reports Module Formula

**Formula** (from `ReportsRepository.java`, lines 417–428):

```
Total Revenue  = totalSales + creditSales + serviceRevenue + quickServiceRevenue + tyreExportRevenue

Total Costs    = generalExpenses + tyreExportCosts + workerCosts

Net Profit     = Total Revenue - Total Costs
```

Where:

| Cost Component | Formula |
|----------------|---------|
| **General Expenses** | `SUM(amount)` from `expenses` table + `SUM(qty × buy_price)` from completed invoices (product COGS) |
| **Tyre Export Costs** | `SUM(comp_price × tyres)` — cost of purchasing tyres from suppliers |
| **Worker Costs** | `SUM(amount)` from `salary_payments` in the period |

### Expanded Formula

```
Net Profit = (Invoice Sales + Credit Sales + Services + Quick Services + Tyre Export Revenue)
           - (General Expenses + Product Cost of Goods Sold + Tyre Purchase Cost + Worker Salaries Paid)
```

### Where the Values Come From

| Value | SQL / Source |
|-------|-------------|
| `totalSales` | `SUM(il.total)` FROM `invoice_line_items` JOIN `invoices` WHERE status='completed' |
| `creditSales` | `SUM(amount)` FROM `credit_sales` |
| `serviceRevenue` | `SUM(price)` FROM `services` |
| `quickServiceRevenue` | `SUM(price)` FROM `quick_services` |
| `tyreExportRevenue` | `SUM(total_amount)` FROM `tyre_exports` |
| `totalExpenses` | `SUM(amount)` FROM `expenses` + completed invoice product costs (`SUM(qty × buy_price)`) |
| `tyreExportCosts` | `SUM(comp_price × tyres)` FROM `tyre_exports` |
| `workerCosts` | `SUM(amount)` FROM `salary_payments` |

---

## 7. Dashboard KPI Calculation

The Dashboard uses a simplified profit formula (from `DashboardController.java`, line 777):

```
Dashboard Profit = Invoice Profit
                 + Service Revenue
                 + Quick Service Revenue
                 + Credit Sales
                 + Tyre Export Profit
                 - Salaries Paid
                 - Expenses
```

This is effectively **Revenue - COGS - Operating Expenses**, which approximates **Net Profit**.

The Dashboard also computes **trend percentages** for comparison against previous periods (line 263–267):

```
Trend % = ((currentValue - previousValue) / previousValue) × 100
```

- If `previous = 0` and `current > 0` → **+100%**
- If both zero → **0%**

---

## 8. Summary of Key Relationships

```
                            ┌───────────────────────────┐
                            │       GROSS SALARY        │
                            │ (attendance × daily rate) │
                            └───────────┬───────────────┘
                                        │
                                        ▼
                            ┌───────────────────────────┐
                            │        DEDUCTIONS          │
                            │  - Advances                │
                            │  - Credit Balance          │
                            └───────────┬───────────────┘
                                        │
                                        ▼
                            ┌───────────────────────────┐
                            │       NET PAYABLE          │
                            │   MAX(0, gross - ded.)     │
                            └───────────┬───────────────┘
                                        │
                                        ▼
                            ┌───────────────────────────┐
                            │    SALARY PAYMENT          │
                            │    (partial or full)       │
                            └───────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────────────┐
    │                            INCOME STATEMENT                              │
    ├─────────────────────────────────────────────────────────────────────────┤
    │                                                                          │
    │  REVENUE:                                                                │
    │    Invoice Sales        → SUM of completed invoice grand_totals          │
    │    Credit Sales         → SUM of credit sale amounts                     │
    │    Services             → SUM of service prices                          │
    │    Quick Services       → SUM of quick service prices                    │
    │    Tyre Exports         → SUM of tyre export total_amounts               │
    │    ─────────────────────────────────────────────────────────────────     │
    │    TOTAL REVENUE        = Σ (all of the above)                           │
    │                                                                          │
    │  COSTS & EXPENSES:                                                       │
    │    Product COGS         → SUM(qty × buy_price) from completed invoices   │
    │    Tyre Purchase Costs  → SUM(comp_price × tyres) from tyre_exports      │
    │    Worker Salaries      → SUM(amount) from salary_payments               │
    │    General Expenses     → SUM(amount) from expenses table                │
    │    ─────────────────────────────────────────────────────────────────     │
    │    TOTAL COSTS          = Σ (all of the above)                           │
    │                                                                          │
    │  NET PROFIT (Income)    = TOTAL REVENUE - TOTAL COSTS                    │
    │                                                                          │
    └─────────────────────────────────────────────────────────────────────────┘
```

### Data Flow Diagram

```
Worker Attendance (daily)
         │
         ▼
    Salary Calculation ──────► Gross Salary ──► Net Payable ──► Salary Payment
                                     │                │                │
                                     ▼                ▼                ▼
                              Advances DB     Credit DB        salary_payments DB
                                                                         │
                                                                         ▼
                                                              ┌──────────────────┐
                                                              │   NET PROFIT     │
                                                              │                  │
Sales (invoices) ─────────────────────────────────────────────►│ Revenue - Costs  │
                                                              │                  │
Services / Quick Services ────────────────────────────────────►│ = Net Income     │
                                                              │                  │
Tyre Exports ─────────────────────────────────────────────────►│                  │
                                                              └──────────────────┘
Expenses ─────────────────────────────────────────────────────►         ▲
                                                                         │
Product Costs (buy_price) ───────────────────────────────────────────────┘
```

---

*This document was generated from analysis of the K-Line TyreHouse Frontend source code, including `DashboardController.java`, `ReportsRepository.java`, `LocalSalaryRepository.java`, `SalaryController.java`, and `WorkerSalary.java`.*