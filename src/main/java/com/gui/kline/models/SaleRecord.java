package com.gui.kline.models;

import javafx.beans.property.*;

/**
 * Represents a single sale transaction.
 * isCredit = true  → customer has not paid yet (credit sale)
 * isCredit = false → cash / paid sale
 */
public class SaleRecord {

    private final StringProperty  date     = new SimpleStringProperty();
    private final StringProperty  product  = new SimpleStringProperty();
    private final IntegerProperty quantity = new SimpleIntegerProperty();
    private final DoubleProperty  total    = new SimpleDoubleProperty();
    private final DoubleProperty  profit   = new SimpleDoubleProperty();
    private final StringProperty  remark   = new SimpleStringProperty();
    private final BooleanProperty credit   = new SimpleBooleanProperty();

    // ── Constructors ────────────────────────────────────────────────────────

    /** Cash sale (backward-compatible) */
    public SaleRecord(String date, String product, int qty, double total, double profit, String remark) {
        this(date, product, qty, total, profit, remark, false);
    }

    /** Full constructor with credit flag */
    public SaleRecord(String date, String product, int qty, double total, double profit, String remark, boolean credit) {
        this.date.set(date);
        this.product.set(product);
        this.quantity.set(qty);
        this.total.set(total);
        this.profit.set(profit);
        this.remark.set(remark);
        this.credit.set(credit);
    }

    // ── Getters (plain) ─────────────────────────────────────────────────────

    public String  getDate()     { return date.get();     }
    public String  getProduct()  { return product.get();  }
    public int     getQuantity() { return quantity.get();  }
    public double  getTotal()    { return total.get();     }
    public double  getProfit()   { return profit.get();    }
    public String  getRemark()   { return remark.get();   }
    public boolean isCredit()    { return credit.get();    }

    // ── Property accessors (for TableView binding) ──────────────────────────

    public StringProperty  dateProperty()     { return date;     }
    public StringProperty  productProperty()  { return product;  }
    public IntegerProperty quantityProperty() { return quantity; }
    public DoubleProperty  totalProperty()    { return total;    }
    public DoubleProperty  profitProperty()   { return profit;   }
    public StringProperty  remarkProperty()   { return remark;   }
    public BooleanProperty creditProperty()   { return credit;   }
}