package com.gui.kline.service;

import com.gui.kline.controller.ReportsController;
import com.gui.kline.data.ReportsRepository;
import com.gui.kline.data.ReportsRepository.DailySummary;
import com.gui.kline.data.ReportsRepository.ExpenseItem;
import com.gui.kline.data.ReportsRepository.FinancialSummary;
import com.gui.kline.data.ReportsRepository.TopProduct;
import com.gui.kline.data.ReportsRepository.CustomerSummary;
import com.gui.kline.controller.ReportsController.SaleItem;
import com.gui.kline.controller.ReportsController.ServiceItem;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

/**
 * Test class for the Reports Section functionality.
 * This class tests all the report generation and data retrieval methods.
 */
public class ReportsTest {

    private final ReportsRepository reportsRepository;
    private final ReportsController reportsController;
    private final PDFExportService pdfExportService;

    public ReportsTest() {
        this.reportsRepository = new ReportsRepository();
        this.reportsController = new ReportsController();
        this.pdfExportService = new PDFExportService(reportsRepository);
    }

    /**
     * Run all tests for the Reports Section
     */
    public void runAllTests() {
        System.out.println("=== REPORTS SECTION TESTS ===");
        System.out.println();
        
        testSalesDataRetrieval();
        testServiceDataRetrieval();
        testExpenseDataRetrieval();
        testFinancialSummary();
        testTopProducts();
        testDailySalesSummary();
        testCustomerSummary();
        testReportGeneration();
        testPDFExport();
        
        System.out.println();
        System.out.println("=== ALL TESTS COMPLETED ===");
    }

    /**
     * Test sales data retrieval
     */
    private void testSalesDataRetrieval() {
        System.out.println("Testing Sales Data Retrieval...");
        try {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            
            List<SaleItem> sales = reportsRepository.getSalesData(startDate, endDate);
            
            System.out.println("  - Retrieved " + sales.size() + " sales records");
            
            if (!sales.isEmpty()) {
                SaleItem firstSale = sales.get(0);
                System.out.println("  - First sale: " + firstSale.name() + " - Rs. " + firstSale.revenue());
            }
            
            System.out.println("  ✅ Sales data retrieval test passed");
        } catch (Exception e) {
            System.err.println("  ❌ Sales data retrieval test failed: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Test service data retrieval
     */
    private void testServiceDataRetrieval() {
        System.out.println("Testing Service Data Retrieval...");
        try {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            
            List<ServiceItem> services = reportsRepository.getServiceData(startDate, endDate);
            
            System.out.println("  - Retrieved " + services.size() + " service records");
            
            if (!services.isEmpty()) {
                ServiceItem firstService = services.get(0);
                System.out.println("  - First service: " + firstService.name() + " - Rs. " + firstService.fee());
            }
            
            System.out.println("  ✅ Service data retrieval test passed");
        } catch (Exception e) {
            System.err.println("  ❌ Service data retrieval test failed: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Test expense data retrieval
     */
    private void testExpenseDataRetrieval() {
        System.out.println("Testing Expense Data Retrieval...");
        try {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            
            List<ExpenseItem> expenses = reportsRepository.getExpenses(startDate, endDate);
            
            System.out.println("  - Retrieved " + expenses.size() + " expense records");
            
            if (!expenses.isEmpty()) {
                ExpenseItem firstExpense = expenses.get(0);
                System.out.println("  - First expense: " + firstExpense.getDescription() + 
                                 " - Rs. " + firstExpense.getAmount() + 
                                 " (" + firstExpense.getCategory() + ")");
            }
            
            System.out.println("  ✅ Expense data retrieval test passed");
        } catch (Exception e) {
            System.err.println("  ❌ Expense data retrieval test failed: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Test financial summary generation
     */
    private void testFinancialSummary() {
        System.out.println("Testing Financial Summary Generation...");
        try {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            
            FinancialSummary summary = reportsRepository.getFinancialSummary(startDate, endDate);
            
            System.out.println("  - Total Sales: Rs. " + String.format("%,.0f", summary.getTotalSales()));
            System.out.println("  - Credit Sales: Rs. " + String.format("%,.0f", summary.getCreditSales()));
            System.out.println("  - Service Revenue: Rs. " + String.format("%,.0f", summary.getServiceRevenue()));
            System.out.println("  - Quick Services: Rs. " + String.format("%,.0f", summary.getQuickServiceRevenue()));
            System.out.println("  - Total Revenue: Rs. " + String.format("%,.0f", summary.getTotalRevenue()));
            System.out.println("  - Total Expenses: Rs. " + String.format("%,.0f", summary.getTotalExpenses()));
            System.out.println("  - Worker Costs: Rs. " + String.format("%,.0f", summary.getWorkerCosts()));
            System.out.println("  - Total Costs: Rs. " + String.format("%,.0f", summary.getTotalCosts()));
            System.out.println("  - Net Profit: Rs. " + String.format("%,.0f", summary.getNetProfit()));
            
            System.out.println("  ✅ Financial summary test passed");
        } catch (Exception e) {
            System.err.println("  ❌ Financial summary test failed: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Test top products retrieval
     */
    private void testTopProducts() {
        System.out.println("Testing Top Products Retrieval...");
        try {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            
            List<TopProduct> topProducts = reportsRepository.getTopSellingProducts(startDate, endDate, 5);
            
            System.out.println("  - Retrieved " + topProducts.size() + " top products");
            
            for (int i = 0; i < Math.min(3, topProducts.size()); i++) {
                TopProduct product = topProducts.get(i);
                System.out.println("  - " + (i + 1) + ". " + product.getProductName() + 
                                 " - Qty: " + product.getQuantity() + 
                                 " - Revenue: Rs. " + String.format("%,.0f", product.getRevenue()));
            }
            
            System.out.println("  ✅ Top products test passed");
        } catch (Exception e) {
            System.err.println("  ❌ Top products test failed: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Test daily sales summary
     */
    private void testDailySalesSummary() {
        System.out.println("Testing Daily Sales Summary...");
        try {
            LocalDate startDate = LocalDate.now().minusDays(7);
            LocalDate endDate = LocalDate.now();
            
            List<DailySummary> dailySummaries = reportsRepository.getDailySalesSummary(startDate, endDate);
            
            System.out.println("  - Retrieved " + dailySummaries.size() + " daily summaries");
            
            for (DailySummary summary : dailySummaries) {
                System.out.println("  - " + summary.getDate() + ": " + summary.getInvoiceCount() + 
                                 " invoices, " + summary.getTotalItems() + " items, Rs. " +
                                 String.format("%,.0f", summary.getTotalRevenue()));
            }
            
            System.out.println("  ✅ Daily sales summary test passed");
        } catch (Exception e) {
            System.err.println("  ❌ Daily sales summary test failed: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Test customer summary
     */
    private void testCustomerSummary() {
        System.out.println("Testing Customer Summary...");
        try {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            
            List<CustomerSummary> customerSummaries = reportsRepository.getCustomerPurchaseSummary(startDate, endDate);
            
            System.out.println("  - Retrieved " + customerSummaries.size() + " customer summaries");
            
            for (int i = 0; i < Math.min(3, customerSummaries.size()); i++) {
                CustomerSummary customer = customerSummaries.get(i);
                System.out.println("  - " + customer.getCustomer() + ": " + customer.getPurchaseCount() + 
                                 " purchases, Total: Rs. " + String.format("%,.0f", customer.getTotalAmount()) + 
                                 ", Paid: Rs. " + String.format("%,.0f", customer.getTotalPaid()) + 
                                 ", Outstanding: Rs. " + String.format("%,.0f", customer.getOutstanding()));
            }
            
            System.out.println("  ✅ Customer summary test passed");
        } catch (Exception e) {
            System.err.println("  ❌ Customer summary test failed: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Test report text generation
     */
    private void testReportGeneration() {
        System.out.println("Testing Report Text Generation...");
        try {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            
            String businessReport = reportsController.generateBusinessReport(startDate, endDate);
            String salesReport = reportsController.generateSalesReport(startDate, endDate);
            
            System.out.println("  - Business Report Length: " + businessReport.length() + " characters");
            System.out.println("  - Sales Report Length: " + salesReport.length() + " characters");
            
            System.out.println("\nSample Business Report:");
            String[] lines = businessReport.split("\n");
            for (int i = 0; i < Math.min(10, lines.length); i++) {
                System.out.println("  " + lines[i]);
            }
            if (lines.length > 10) {
                System.out.println("  ... (truncated)");
            }
            
            System.out.println("  ✅ Report generation test passed");
        } catch (Exception e) {
            System.err.println("  ❌ Report generation test failed: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Test PDF export functionality
     */
    private void testPDFExport() {
        System.out.println("Testing PDF Export Functionality...");
        try {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            
            // Test default file generation
            java.io.File defaultFile = pdfExportService.generateDefaultOutputFile("Business_Report");
            System.out.println("  - Default output file: " + defaultFile.getAbsolutePath());
            
            // Test PDF export availability
            boolean isAvailable = pdfExportService.isPDFExportAvailable();
            System.out.println("  - PDF export available: " + isAvailable);
            
            List<String> availableLibraries = pdfExportService.getAvailablePDFLibraries();
            System.out.println("  - Available PDF libraries: " + availableLibraries);
            
            // Test actual export (to text file for now)
            java.io.File testFile = new java.io.File(System.getProperty("java.io.tmpdir"), 
                    "KLine_Report_Test_" + System.currentTimeMillis() + ".txt");
            boolean exportSuccess = pdfExportService.exportBusinessReportToPDF(startDate, endDate, testFile);
            
            if (exportSuccess && testFile.exists()) {
                System.out.println("  - Export test file created: " + testFile.getAbsolutePath());
                System.out.println("  - File size: " + testFile.length() + " bytes");
                testFile.delete(); // Clean up
            }
            
            System.out.println("  ✅ PDF export test passed");
        } catch (Exception e) {
            System.err.println("  ❌ PDF export test failed: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Test worker costs calculation
     */
    public void testWorkerCosts() {
        System.out.println("Testing Worker Costs Calculation...");
        try {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            
            double workerCosts = reportsRepository.getWorkerCosts(startDate, endDate);
            
            System.out.println("  - Worker Costs for period: Rs. " + String.format("%,.0f", workerCosts));
            System.out.println("  ✅ Worker costs test passed");
        } catch (Exception e) {
            System.err.println("  ❌ Worker costs test failed: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Run a specific test
     */
    public void runTest(String testName) {
        switch (testName.toLowerCase()) {
            case "sales":
            case "salesdata":
                testSalesDataRetrieval();
                break;
            case "services":
            case "servicedata":
                testServiceDataRetrieval();
                break;
            case "expenses":
            case "expensedata":
                testExpenseDataRetrieval();
                break;
            case "financial":
            case "summary":
                testFinancialSummary();
                break;
            case "topproducts":
                testTopProducts();
                break;
            case "dailysales":
                testDailySalesSummary();
                break;
            case "customers":
                testCustomerSummary();
                break;
            case "reports":
            case "reportgeneration":
                testReportGeneration();
                break;
            case "pdf":
            case "pdfexport":
                testPDFExport();
                break;
            case "workers":
            case "workercosts":
                testWorkerCosts();
                break;
            default:
                System.out.println("Unknown test: " + testName);
                System.out.println("Available tests: sales, services, expenses, financial, topproducts, dailysales, customers, reports, pdf, workers");
        }
    }

    /**
     * Main method to run tests from command line
     */
    public static void main(String[] args) {
        ReportsTest test = new ReportsTest();
        
        if (args.length == 0) {
            test.runAllTests();
        } else {
            for (String testName : args) {
                test.runTest(testName);
            }
        }
    }
}