package com.gui.kline.models.reports;

/**
 * Model class representing financial summary totals for reports.
 */
public class FinancialSummary {
    private double totalSales;
    private double creditSales;
    private double serviceRevenue;
    private double quickServiceRevenue;
    private double tyreExportRevenue;
    private double totalExpenses;
    private double productCosts;
    private double workerCosts;
    private double netProfit;

    public double getTotalSales() { return totalSales; }
    public void setTotalSales(double totalSales) { this.totalSales = totalSales; }
    
    public double getCreditSales() { return creditSales; }
    public void setCreditSales(double creditSales) { this.creditSales = creditSales; }
    
    public double getServiceRevenue() { return serviceRevenue; }
    public void setServiceRevenue(double serviceRevenue) { this.serviceRevenue = serviceRevenue; }
    
    public double getQuickServiceRevenue() { return quickServiceRevenue; }
    public void setQuickServiceRevenue(double quickServiceRevenue) { this.quickServiceRevenue = quickServiceRevenue; }
    
    public double getTyreExportRevenue() { return tyreExportRevenue; }
    public void setTyreExportRevenue(double tyreExportRevenue) { this.tyreExportRevenue = tyreExportRevenue; }
    
    public double getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(double totalExpenses) { this.totalExpenses = totalExpenses; }
    
    public double getProductCosts() { return productCosts; }
    public void setProductCosts(double productCosts) { this.productCosts = productCosts; }
    
    public double getWorkerCosts() { return workerCosts; }
    public void setWorkerCosts(double workerCosts) { this.workerCosts = workerCosts; }
    
    public double getNetProfit() { return netProfit; }
    public void setNetProfit(double netProfit) { this.netProfit = netProfit; }
    
    public double getTotalRevenue() {
        return totalSales + creditSales + serviceRevenue + quickServiceRevenue + tyreExportRevenue;
    }
    
    public double getTotalCosts() {
        return totalExpenses + productCosts + workerCosts;
    }
}
