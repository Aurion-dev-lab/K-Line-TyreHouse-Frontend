package com.gui.kline.models;

import javafx.beans.property.*;

public class Product {
    private final StringProperty  id        = new SimpleStringProperty();
    private final StringProperty  name      = new SimpleStringProperty();
    private final StringProperty  category  = new SimpleStringProperty();
    private final DoubleProperty  buyPrice  = new SimpleDoubleProperty();
    private final DoubleProperty  sellPrice = new SimpleDoubleProperty();
    private final IntegerProperty stock     = new SimpleIntegerProperty();

    public Product(String name, String category, double buyPrice,
                   double sellPrice, int stock) {
        this.id.set(java.util.UUID.randomUUID().toString());
        this.name.set(name);
        this.category.set(category);
        this.buyPrice.set(buyPrice);
        this.sellPrice.set(sellPrice);
        this.stock.set(stock);
    }

    public Product(String id, String name, String category, double buyPrice,
                   double sellPrice, int stock) {
        this.id.set(id);
        this.name.set(name);
        this.category.set(category);
        this.buyPrice.set(buyPrice);
        this.sellPrice.set(sellPrice);
        this.stock.set(stock);
    }

    public String  getId()        { return id.get(); }
    public String  getName()      { return name.get(); }
    public String  getCategory()  { return category.get(); }
    public double  getBuyPrice()  { return buyPrice.get(); }
    public double  getSellPrice() { return sellPrice.get(); }
    public int     getStock()     { return stock.get(); }

    public StringProperty  idProperty()        { return id; }
    public StringProperty  nameProperty()      { return name; }
    public StringProperty  categoryProperty()  { return category; }
    public DoubleProperty  buyPriceProperty()  { return buyPrice; }
    public DoubleProperty  sellPriceProperty() { return sellPrice; }
    public IntegerProperty stockProperty()     { return stock; }

    public void setName(String v)      { name.set(v); }
    public void setCategory(String v)  { category.set(v); }
    public void setBuyPrice(double v)  { buyPrice.set(v); }
    public void setSellPrice(double v) { sellPrice.set(v); }
    public void setStock(int v)        { stock.set(v); }
    public void setId(String v)        { id.set(v); }

    public double getProfit() { return getSellPrice() - getBuyPrice(); }
    public boolean isLowStock() { return getStock() <= 5; }
}