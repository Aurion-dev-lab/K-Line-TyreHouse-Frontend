package com.gui.kline.service;

import com.gui.kline.data.ReportsRepository;
import com.gui.kline.data.ReportsRepository.DailySummary;
import com.gui.kline.data.ReportsRepository.ExpenseItem;
import com.gui.kline.data.ReportsRepository.FinancialSummary;
import com.gui.kline.data.ReportsRepository.TopProduct;
import com.gui.kline.data.ReportsRepository.CustomerSummary;
import com.gui.kline.controller.ReportsController.SaleItem;
import com.gui.kline.controller.ReportsController.ServiceItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for exporting reports to PDF format.
 * This service provides methods to generate PDF reports using different libraries.
 * Currently implements basic PDF generation, with options for Apache PDFBox and iText.
 */
public class PDFExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ReportsRepository reportsRepository;

    public PDFExportService() {
        this.reportsRepository = new ReportsRepository();
    }

    public PDFExportService(ReportsRepository reportsRepository) {
        this.reportsRepository = reportsRepository;
    }

    /**
     * Export comprehensive business report to PDF
     * @param startDate Start date of the report period
     * @param endDate End date of the report period
     * @param outputFile Output file path
     * @return true if export was successful
     */
    public boolean exportBusinessReportToPDF(LocalDate startDate, LocalDate endDate, File outputFile) {
        try {
            // For now, we'll generate a text-based PDF using a simple approach
            // In production, you would use a proper PDF library like Apache PDFBox or iText
            return generateTextBasedPDF(startDate, endDate, outputFile);
        } catch (Exception e) {
            System.err.println("Failed to export business report to PDF: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Export sales report to PDF
     */
    public boolean exportSalesReportToPDF(LocalDate startDate, LocalDate endDate, File outputFile) {
        try {
            return generateSalesPDF(startDate, endDate, outputFile);
        } catch (Exception e) {
            System.err.println("Failed to export sales report to PDF: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Export service revenue report to PDF
     */
    public boolean exportServiceReportToPDF(LocalDate startDate, LocalDate endDate, File outputFile) {
        try {
            return generateServicePDF(startDate, endDate, outputFile);
        } catch (Exception e) {
            System.err.println("Failed to export service report to PDF: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Export expense report to PDF
     */
    public boolean exportExpenseReportToPDF(LocalDate startDate, LocalDate endDate, File outputFile) {
        try {
            return generateExpensePDF(startDate, endDate, outputFile);
        } catch (Exception e) {
            System.err.println("Failed to export expense report to PDF: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Generate a default filename for the report
     */
    public File generateDefaultOutputFile(String reportType) {
        String timestamp = LocalDate.now().format(FILE_DATE_FORMATTER);
        String userHome = System.getProperty("user.home");
        File reportsDir = new File(userHome, "KLine_Reports");
        
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }
        
        return new File(reportsDir, "KLine_" + reportType + "_" + timestamp + ".pdf");
    }

    private boolean generateTextBasedPDF(LocalDate startDate, LocalDate endDate, File outputFile) throws IOException {
        // This is a placeholder implementation
        // In a real application, you would use a proper PDF library
        
        FinancialSummary summary = reportsRepository.getFinancialSummary(startDate, endDate);
        List<SaleItem> sales = reportsRepository.getSalesData(startDate, endDate);
        List<ServiceItem> services = reportsRepository.getServiceData(startDate, endDate);
        List<ExpenseItem> expenses = reportsRepository.getExpenses(startDate, endDate);
        
        StringBuilder content = new StringBuilder();
        content.append("K-LINE TYRE HOUSE - BUSINESS REPORT\n");
        content.append("====================================\n\n");
        content.append("Report Period: ").append(startDate.format(DATE_FORMATTER)).append(" to ")
               .append(endDate.format(DATE_FORMATTER)).append("\n");
        content.append("Generated: ").append(LocalDate.now().format(DATE_FORMATTER)).append("\n\n");
        
        content.append("FINANCIAL SUMMARY\n");
        content.append("----------------\n");
        content.append(String.format("Total Sales Revenue:     Rs. %,.0f\n", summary.getTotalSales()));
        content.append(String.format("Credit Sales:            Rs. %,.0f\n", summary.getCreditSales()));
        content.append(String.format("Service Revenue:         Rs. %,.0f\n", summary.getServiceRevenue()));
        content.append(String.format("Quick Services:          Rs. %,.0f\n", summary.getQuickServiceRevenue()));
        content.append(String.format("Total Revenue:           Rs. %,.0f\n\n", summary.getTotalRevenue()));
        
        content.append(String.format("Total Expenses:          Rs. %,.0f\n", summary.getTotalExpenses()));
        content.append(String.format("Worker Costs:            Rs. %,.0f\n", summary.getWorkerCosts()));
        content.append(String.format("Total Costs:             Rs. %,.0f\n\n", summary.getTotalCosts()));
        
        content.append(String.format("NET PROFIT:              Rs. %,.0f\n\n", summary.getNetProfit()));
        
        content.append("TOP 5 SALES\n");
        content.append("----------\n");
        sales.stream()
                .sorted((a, b) -> Double.compare(b.revenue(), a.revenue()))
                .limit(5)
                .forEach(item -> content.append(String.format("%-25s %10s %12s\n", 
                        truncate(item.name(), 25), 
                        formatCurrency(item.revenue()))));
        
        content.append("\nTOP 5 SERVICES\n");
        content.append("------------\n");
        services.stream()
                .sorted((a, b) -> Double.compare(b.fee(), a.fee()))
                .limit(5)
                .forEach(item -> content.append(String.format("%-25s %12s\n", 
                        truncate(item.name(), 25), 
                        formatCurrency(item.fee()))));
        
        // For now, save as text file (in real implementation, this would be PDF)
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(content.toString().getBytes());
            return true;
        }
    }

    private boolean generateSalesPDF(LocalDate startDate, LocalDate endDate, File outputFile) throws IOException {
        List<SaleItem> sales = reportsRepository.getSalesData(startDate, endDate);
        
        StringBuilder content = new StringBuilder();
        content.append("K-LINE TYRE HOUSE - SALES REPORT\n");
        content.append("==================================\n\n");
        content.append("Report Period: ").append(startDate.format(DATE_FORMATTER)).append(" to ")
               .append(endDate.format(DATE_FORMATTER)).append("\n\n");
        
        double totalRevenue = sales.stream().mapToDouble(SaleItem::revenue).sum();
        double totalProfit = sales.stream().mapToDouble(SaleItem::profit).sum();
        
        content.append("SUMMARY\n");
        content.append("-------\n");
        content.append(String.format("Total Transactions: %d\n", sales.size()));
        content.append(String.format("Total Revenue:      Rs. %,.0f\n", totalRevenue));
        content.append(String.format("Total Profit:       Rs. %,.0f\n\n", totalProfit));
        
        content.append("DETAILED SALES\n");
        content.append("------------\n");
        content.append(String.format("%-12s %-25s %6s %12s %12s\n", "Date", "Product", "Qty", "Revenue", "Profit"));
        content.append(String.format("%-12s %-25s %6s %12s %12s\n", "----", "-------", "---", "-------", "------"));
        
        sales.stream()
                .sorted((a, b) -> b.date().compareTo(a.date()))
                .forEach(item -> content.append(String.format("%-12s %-25s %6d %12s %12s\n",
                        item.date().format(DATE_FORMATTER),
                        truncate(item.name(), 25),
                        item.qty(),
                        formatCurrency(item.revenue()),
                        formatCurrency(item.profit()))));
        
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(content.toString().getBytes());
            return true;
        }
    }

    private boolean generateServicePDF(LocalDate startDate, LocalDate endDate, File outputFile) throws IOException {
        List<ServiceItem> services = reportsRepository.getServiceData(startDate, endDate);
        
        StringBuilder content = new StringBuilder();
        content.append("K-LINE TYRE HOUSE - SERVICE REVENUE REPORT\n");
        content.append("===========================================\n\n");
        content.append("Report Period: ").append(startDate.format(DATE_FORMATTER)).append(" to ")
               .append(endDate.format(DATE_FORMATTER)).append("\n\n");
        
        double totalRevenue = services.stream().mapToDouble(ServiceItem::fee).sum();
        
        content.append("SUMMARY\n");
        content.append("-------\n");
        content.append(String.format("Total Services: %d\n", services.size()));
        content.append(String.format("Total Revenue: Rs. %,.0f\n\n", totalRevenue));
        
        content.append("DETAILED SERVICES\n");
        content.append("----------------\n");
        content.append(String.format("%-12s %-30s %12s\n", "Date", "Service", "Fee"));
        content.append(String.format("%-12s %-30s %12s\n", "----", "-------", "---"));
        
        services.stream()
                .sorted((a, b) -> b.date().compareTo(a.date()))
                .forEach(item -> content.append(String.format("%-12s %-30s %12s\n",
                        item.date().format(DATE_FORMATTER),
                        truncate(item.name(), 30),
                        formatCurrency(item.fee()))));
        
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(content.toString().getBytes());
            return true;
        }
    }

    private boolean generateExpensePDF(LocalDate startDate, LocalDate endDate, File outputFile) throws IOException {
        List<ExpenseItem> expenses = reportsRepository.getExpenses(startDate, endDate);
        
        StringBuilder content = new StringBuilder();
        content.append("K-LINE TYRE HOUSE - EXPENSE REPORT\n");
        content.append("==================================\n\n");
        content.append("Report Period: ").append(startDate.format(DATE_FORMATTER)).append(" to ")
               .append(endDate.format(DATE_FORMATTER)).append("\n\n");
        
        // Group by category
        Map<String, List<ExpenseItem>> byCategory = expenses.stream()
                .collect(Collectors.groupingBy(ExpenseItem::getCategory));
        
        double totalExpenses = expenses.stream().mapToDouble(ExpenseItem::getAmount).sum();
        
        content.append("SUMMARY\n");
        content.append("-------\n");
        content.append(String.format("Total Expenses: Rs. %,.0f\n\n", totalExpenses));
        
        content.append("EXPENSES BY CATEGORY\n");
        content.append("--------------------\n");
        
        for (Map.Entry<String, List<ExpenseItem>> entry : byCategory.entrySet()) {
            content.append(String.format("\n%s\n", entry.getKey()));
            content.append(String.format("%-12s %-40s %12s\n", "Date", "Description", "Amount"));
            content.append(String.format("%-12s %-40s %12s\n", "----", "-----------", "------"));
            
            double categoryTotal = 0;
            for (ExpenseItem expense : entry.getValue()) {
                content.append(String.format("%-12s %-40s %12s\n",
                        expense.getDate().format(DATE_FORMATTER),
                        truncate(expense.getDescription(), 40),
                        formatCurrency(expense.getAmount())));
                categoryTotal += expense.getAmount();
            }
            
            content.append(String.format("%-12s %-40s %12s\n", "", "Category Total:", formatCurrency(categoryTotal)));
        }
        
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(content.toString().getBytes());
            return true;
        }
    }

    /**
     * Check if PDF export is available (if proper libraries are installed)
     */
    public boolean isPDFExportAvailable() {
        // Check for PDF libraries
        try {
            // Try to load Apache PDFBox
            Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            return true;
        } catch (ClassNotFoundException e) {
            try {
                // Try to load iText
                Class.forName("com.itextpdf.text.Document");
                return true;
            } catch (ClassNotFoundException e2) {
                return false;
            }
        }
    }

    /**
     * Get available PDF export libraries
     */
    public List<String> getAvailablePDFLibraries() {
        List<String> libraries = new java.util.ArrayList<>();
        
        try {
            Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            libraries.add("Apache PDFBox");
        } catch (ClassNotFoundException e) {
            // PDFBox not available
        }
        
        try {
            Class.forName("com.itextpdf.text.Document");
            libraries.add("iText");
        } catch (ClassNotFoundException e) {
            // iText not available
        }
        
        return libraries;
    }

    private String formatCurrency(double value) {
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance();
        nf.setMaximumFractionDigits(0);
        return nf.format(value);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Get the reports repository used by this service
     */
    public ReportsRepository getReportsRepository() {
        return reportsRepository;
    }
}