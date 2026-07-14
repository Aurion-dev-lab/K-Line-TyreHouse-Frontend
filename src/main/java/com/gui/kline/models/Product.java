package com.gui.kline.models;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Product {
    private final StringProperty  id        = new SimpleStringProperty();
    private final StringProperty  code      = new SimpleStringProperty();
    private final StringProperty  name      = new SimpleStringProperty();
    private final StringProperty  category  = new SimpleStringProperty();
    private final DoubleProperty  buyPrice  = new SimpleDoubleProperty();
    private final DoubleProperty  sellPrice = new SimpleDoubleProperty();
    private final IntegerProperty stock     = new SimpleIntegerProperty();
    private final IntegerProperty minimumStockAlert = new SimpleIntegerProperty(5);
    private final StringProperty brand = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final StringProperty vehicleType = new SimpleStringProperty("");
    private final StringProperty material = new SimpleStringProperty("");
    private final StringProperty supplierName = new SimpleStringProperty("");
    private final StringProperty createdDate = new SimpleStringProperty("");
    private final java.util.List<String> imagePaths = new java.util.ArrayList<>();
    private final BooleanProperty active    = new SimpleBooleanProperty(true);

    public Product(String name, String category, double buyPrice,
                   double sellPrice, int stock) {
        this.id.set(java.util.UUID.randomUUID().toString());
        this.code.set("");
        this.name.set(name);
        this.category.set(category);
        this.buyPrice.set(buyPrice);
        this.sellPrice.set(sellPrice);
        this.stock.set(stock);
        this.minimumStockAlert.set(5);
        this.brand.set("");
        this.description.set("");
        this.vehicleType.set("");
        this.material.set("");
        this.supplierName.set("");
        this.createdDate.set(java.time.LocalDateTime.now().toString());
    }

    public Product(String id, String name, String category, double buyPrice,
                   double sellPrice, int stock) {
        this.id.set(id);
        this.code.set("");
        this.name.set(name);
        this.category.set(category);
        this.buyPrice.set(buyPrice);
        this.sellPrice.set(sellPrice);
        this.stock.set(stock);
    }

    public Product(String id, String code, String name, String category, double buyPrice,
                   double sellPrice, int stock, int minimumStockAlert, String brand, String description,
                   String vehicleType, String material, String supplierName, String createdDate, java.util.List<String> imagePaths) {
        this.id.set(id);
        this.code.set(code == null ? "" : code.trim());
        this.name.set(name);
        this.category.set(category);
        this.buyPrice.set(buyPrice);
        this.sellPrice.set(sellPrice);
        this.stock.set(stock);
        this.minimumStockAlert.set(minimumStockAlert);
        this.brand.set(brand == null ? "" : brand);
        this.description.set(description == null ? "" : description);
        this.vehicleType.set(vehicleType == null ? "" : vehicleType);
        this.material.set(material == null ? "" : material);
        this.supplierName.set(supplierName == null ? "" : supplierName);
        this.createdDate.set(createdDate == null ? "" : createdDate);
        if (imagePaths != null) {
            this.imagePaths.addAll(imagePaths);
        }
    }

    public Product(String id, String code, String name, String category, double buyPrice,
                   double sellPrice, int stock, int minimumStockAlert, String imagePath) {
        this.id.set(id);
        this.code.set(code == null ? "" : code.trim());
        this.name.set(name);
        this.category.set(category);
        this.buyPrice.set(buyPrice);
        this.sellPrice.set(sellPrice);
        this.stock.set(stock);
        this.minimumStockAlert.set(minimumStockAlert);
        this.brand.set("");
        this.description.set("");
        this.vehicleType.set("");
        this.material.set("");
        this.supplierName.set("");
        this.createdDate.set(java.time.LocalDateTime.now().toString());
        if (imagePath != null && !imagePath.isEmpty()) {
            this.imagePaths.add(imagePath);
        }
    }

    public Product(String id, String code, String name, String category, double buyPrice,
                   double sellPrice, int stock, int minimumStockAlert) {
        this.id.set(id);
        this.code.set(code == null ? "" : code.trim());
        this.name.set(name);
        this.category.set(category);
        this.buyPrice.set(buyPrice);
        this.sellPrice.set(sellPrice);
        this.stock.set(stock);
        this.minimumStockAlert.set(minimumStockAlert);
        this.brand.set("");
        this.description.set("");
        this.vehicleType.set("");
        this.material.set("");
        this.supplierName.set("");
        this.createdDate.set(java.time.LocalDateTime.now().toString());
    }

    public Product(String id, String code, String name, String category, double buyPrice,
                   double sellPrice, int stock) {
        this.id.set(id);
        this.code.set(code == null ? "" : code.trim());
        this.name.set(name);
        this.category.set(category);
        this.buyPrice.set(buyPrice);
        this.sellPrice.set(sellPrice);
        this.stock.set(stock);
        this.minimumStockAlert.set(5);
        this.brand.set("");
        this.description.set("");
        this.vehicleType.set("");
        this.material.set("");
        this.supplierName.set("");
        this.createdDate.set(java.time.LocalDateTime.now().toString());
    }

    public String  getId()        { return id.get(); }
    public String  getCode()      { return code.get(); }
    public String  getProductCode() { return code.get(); }
    public String  getName()      { return name.get(); }
    public String  getCategory()  { return category.get(); }
    public double  getBuyPrice()  { return buyPrice.get(); }
    public double  getSellPrice() { return sellPrice.get(); }
    public int     getStock()     { return stock.get(); }
    public int     getMinimumStockAlert() { return minimumStockAlert.get(); }
    public String getBrand()  { return brand.get(); }
    public String getDescription()  { return description.get(); }
    public String getVehicleType()  { return vehicleType.get(); }
    public String getMaterial()  { return material.get(); }
    public String getSupplierName()  { return supplierName.get(); }
    public String getCreatedDate()  { return createdDate.get(); }
    public java.util.List<String> getImagePaths()  { return imagePaths; }
    public boolean  isActive()    { return active.get(); }
    public boolean isInStock() { return getStock() > 0; }

    public StringProperty  idProperty()        { return id; }
    public StringProperty  codeProperty()      { return code; }
    public StringProperty  nameProperty()      { return name; }
    public StringProperty  categoryProperty()  { return category; }
    public DoubleProperty  buyPriceProperty()  { return buyPrice; }
    public DoubleProperty  sellPriceProperty() { return sellPrice; }
    public IntegerProperty stockProperty()     { return stock; }
    public IntegerProperty minimumStockAlertProperty() { return minimumStockAlert; }
    public StringProperty  brandProperty() { return brand; }
    public StringProperty  descriptionProperty() { return description; }
    public StringProperty  vehicleTypeProperty() { return vehicleType; }
    public StringProperty  materialProperty() { return material; }
    public StringProperty  supplierNameProperty() { return supplierName; }
    public StringProperty  createdDateProperty() { return createdDate; }
    public BooleanProperty activeProperty()   { return active; }

    public void setName(String v)      { name.set(v); }
    public void setCode(String v)      { code.set(v == null ? "" : v.trim()); }
    public void setCategory(String v)  { category.set(v); }
    public void setBuyPrice(double v)  { buyPrice.set(v); }
    public void setSellPrice(double v) { sellPrice.set(v); }
    public void setStock(int v)        { stock.set(v); }
    public void setMinimumStockAlert(int v) { minimumStockAlert.set(v); }
    public void setBrand(String v) { brand.set(v == null ? "" : v); }
    public void setDescription(String v) { description.set(v == null ? "" : v); }
    public void setVehicleType(String v) { vehicleType.set(v == null ? "" : v); }
    public void setMaterial(String v) { material.set(v == null ? "" : v); }
    public void setSupplierName(String v) { supplierName.set(v == null ? "" : v); }
    public void setCreatedDate(String v) { createdDate.set(v == null ? "" : v); }
    public void setActive(boolean v)    { active.set(v); }
    public void setId(String v)        { id.set(v); }
    
    public void addImagePath(String path) {
        if (path != null && !path.isEmpty()) {
            this.imagePaths.add(path);
        }
    }
    
    public void removeImagePath(String path) {
        this.imagePaths.remove(path);
    }
    
    public void clearImagePaths() {
        this.imagePaths.clear();
    }
    
    // Backward compatibility - get first image or empty string
    public String getImagePath() {
        return imagePaths.isEmpty() ? "" : imagePaths.get(0);
    }
    
    public void setImagePath(String v) {
        if (v == null || v.isEmpty()) {
            this.imagePaths.clear();
        } else {
            this.imagePaths.clear();
            this.imagePaths.add(v);
        }
    }

    public double getProfit() { return getSellPrice() - getBuyPrice(); }
    public boolean isLowStock() { return getStock() <= getMinimumStockAlert(); }

    @Override
    public String toString() {
        String codeValue = getCode();
        return (codeValue == null || codeValue.isBlank()) ? getName() : codeValue + " - " + getName();
    }
}