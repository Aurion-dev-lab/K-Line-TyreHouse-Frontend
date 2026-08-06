package com.gui.kline.service;

import com.gui.kline.data.ReportsRepository;
import com.gui.kline.models.reports.CustomerSummary;
import com.gui.kline.models.reports.ExpenseItem;
import com.gui.kline.models.reports.FinancialSummary;
import com.gui.kline.controller.ReportsController.SaleItem;
import com.gui.kline.controller.ReportsController.ServiceItem;
import com.gui.kline.controller.ReportsController.PaymentItem;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

public class PDFExportService {

    private static final DateTimeFormatter DATE_FMT      = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final NumberFormat      NF            = NumberFormat.getInstance();
    static { NF.setMaximumFractionDigits(0); }

    private static final Color NAVY         = new Color(17,  24,  39);
    private static final Color BLUE         = new Color(30,  64, 175);
    private static final Color BLUE_LIGHT   = new Color(219,234,254);
    private static final Color GREEN        = new Color(22, 163,  74);
    private static final Color GREEN_LIGHT  = new Color(220,252,231);
    private static final Color RED          = new Color(220,  38,  38);
    private static final Color RED_LIGHT    = new Color(254,226,226);
    private static final Color GRAY         = new Color(107,114,128);
    private static final Color GRAY_LIGHT   = new Color(249,250,251);
    private static final Color GRAY_BORDER  = new Color(229,231,235);
    private static final Color GRAY_HEADER  = new Color(243,244,246);
    private static final Color GRAY_DARK    = new Color(55,  65,  81);
    private static final Color PURPLE       = new Color(147, 51, 234);
    private static final Color ORANGE       = new Color(234, 88,  12);
    private static final Color PURPLE_LIGHT = new Color(245,243,255);
    private static final Color ORANGE_LIGHT = new Color(255,247,237);

    private Font titleFont()     { return FontFactory.getFont(FontFactory.HELVETICA_BOLD,  22, Color.WHITE); }
    private Font subtitleFont()  { return FontFactory.getFont(FontFactory.HELVETICA,       13, new Color(156,163,175)); }
    private Font metaFont()      { return FontFactory.getFont(FontFactory.HELVETICA,        9, new Color(156,163,175)); }
    private Font secFont()       { return FontFactory.getFont(FontFactory.HELVETICA_BOLD,  11, NAVY); }
    private Font tblHdrFont()    { return FontFactory.getFont(FontFactory.HELVETICA_BOLD,   9, Color.WHITE); }
    private Font bodyFont()      { return FontFactory.getFont(FontFactory.HELVETICA,        9, NAVY); }
    private Font boldFont()      { return FontFactory.getFont(FontFactory.HELVETICA_BOLD,   9, NAVY); }
    private Font subtleFont()    { return FontFactory.getFont(FontFactory.HELVETICA,        8, GRAY); }
    private Font greenFont()     { return FontFactory.getFont(FontFactory.HELVETICA_BOLD,   9, GREEN); }
    private Font redFont()       { return FontFactory.getFont(FontFactory.HELVETICA_BOLD,   9, RED); }
    private Font blueFont()      { return FontFactory.getFont(FontFactory.HELVETICA_BOLD,   9, BLUE); }
    private Font purpleFont()    { return FontFactory.getFont(FontFactory.HELVETICA_BOLD,   9, PURPLE); }
    private Font orangeFont()    { return FontFactory.getFont(FontFactory.HELVETICA_BOLD,   9, ORANGE); }
    private Font totFont(Color c){ return FontFactory.getFont(FontFactory.HELVETICA_BOLD,  10, c); }

    private final ReportsRepository reportsRepository;

    public PDFExportService() { this.reportsRepository = new ReportsRepository(); }
    public PDFExportService(ReportsRepository r) { this.reportsRepository = r; }

    public boolean exportBusinessReportToPDF(LocalDate s, LocalDate e, File f) {
        return exportSummaryReportToPDF(s, e, f);
    }

    public boolean exportSummaryReportToPDF(LocalDate startDate, LocalDate endDate, File outputFile) {
        try {
            FinancialSummary            summary  = reportsRepository.getFinancialSummary(startDate, endDate);
            java.util.List<SaleItem>    sales    = reportsRepository.getSalesData(startDate, endDate);
            java.util.List<ServiceItem> services = reportsRepository.getServiceData(startDate, endDate);
            java.util.List<ExpenseItem> expenses = reportsRepository.getExpenses(startDate, endDate);

            Document doc = new Document(PageSize.A4, 36, 36, 50, 40);
            PdfWriter.getInstance(doc, new FileOutputStream(outputFile));
            doc.open();

            buildHeader(doc, "Executive Business Summary", startDate, endDate);
            addSectionBand(doc, "Financial Summary");
            buildFinancialSummary(doc, summary);

            if (!sales.isEmpty())    { addSectionBand(doc, "Sales Overview (" + sales.size() + " transactions)"); buildSalesTable(doc, sales); }
            if (!services.isEmpty()) { addSectionBand(doc, "Service Overview (" + services.size() + " entries)"); buildServicesTable(doc, services); }
            if (!expenses.isEmpty()) { addSectionBand(doc, "Expenses Overview"); buildExpensesSection(doc, expenses); }

            doc.close();
            return true;
        } catch (Exception ex) {
            System.err.println("Export Summary failed: " + ex.getMessage()); ex.printStackTrace(); return false;
        }
    }

    public boolean exportSalesReportToPDF(LocalDate startDate, LocalDate endDate, File outputFile) {
        try {
            java.util.List<SaleItem> sales = reportsRepository.getSalesData(startDate, endDate);
            Document doc = new Document(PageSize.A4, 36, 36, 50, 40);
            PdfWriter.getInstance(doc, new FileOutputStream(outputFile));
            doc.open();

            buildHeader(doc, "Sales Analysis Report", startDate, endDate);
            if (!sales.isEmpty()) {
                addSectionBand(doc, "Detailed Sales Transactions (" + sales.size() + " items)");
                buildSalesTable(doc, sales);
            }
            doc.close();
            return true;
        } catch (Exception ex) {
            System.err.println("Export Sales failed: " + ex.getMessage()); ex.printStackTrace(); return false;
        }
    }

    public boolean exportServiceReportToPDF(LocalDate startDate, LocalDate endDate, File outputFile) {
        try {
            java.util.List<ServiceItem> services = reportsRepository.getServiceData(startDate, endDate);
            Document doc = new Document(PageSize.A4, 36, 36, 50, 40);
            PdfWriter.getInstance(doc, new FileOutputStream(outputFile));
            doc.open();

            buildHeader(doc, "Service Revenue Report", startDate, endDate);
            if (!services.isEmpty()) {
                addSectionBand(doc, "Service Entries (" + services.size() + " items)");
                buildServicesTable(doc, services);
            }
            doc.close();
            return true;
        } catch (Exception ex) {
            System.err.println("Export Services failed: " + ex.getMessage()); ex.printStackTrace(); return false;
        }
    }

    public boolean exportExpenseReportToPDF(LocalDate startDate, LocalDate endDate, File outputFile) {
        try {
            java.util.List<ExpenseItem> expenses = reportsRepository.getExpenses(startDate, endDate);
            Document doc = new Document(PageSize.A4, 36, 36, 50, 40);
            PdfWriter.getInstance(doc, new FileOutputStream(outputFile));
            doc.open();

            buildHeader(doc, "Expense Analysis Report", startDate, endDate);
            if (!expenses.isEmpty()) {
                addSectionBand(doc, "Categorized Expenses");
                buildExpensesSection(doc, expenses);
            }
            doc.close();
            return true;
        } catch (Exception ex) {
            System.err.println("Export Expenses failed: " + ex.getMessage()); ex.printStackTrace(); return false;
        }
    }

    public boolean exportCustomerReportToPDF(LocalDate startDate, LocalDate endDate, File outputFile) {
        try {
            java.util.List<CustomerSummary> customers = reportsRepository.getCustomerPurchaseSummary(startDate, endDate);
            java.util.List<PaymentItem> payments = reportsRepository.getPaymentTransactions(startDate, endDate);
            Document doc = new Document(PageSize.A4, 36, 36, 50, 40);
            PdfWriter.getInstance(doc, new FileOutputStream(outputFile));
            doc.open();

            buildHeader(doc, "Customer & Credit Analysis Report", startDate, endDate);
            if (customers != null && !customers.isEmpty()) {
                addSectionBand(doc, "Customer Credit Balances (" + customers.size() + " customers)");
                buildCustomerTable(doc, customers);
            }
            if (payments != null && !payments.isEmpty()) {
                addSectionBand(doc, "Payment Settlement History (" + payments.size() + " payments)");
                buildPaymentTable(doc, payments);
            }
            doc.close();
            return true;
        } catch (Exception ex) {
            System.err.println("Export Customer Analysis failed: " + ex.getMessage()); ex.printStackTrace(); return false;
        }
    }

    public File generateDefaultOutputFile(String reportType) {
        File dir = new File(System.getProperty("user.home"), "KLine_Reports");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "KLine_Report_" + LocalDate.now().format(FILE_DATE_FMT) + ".pdf");
    }

    public boolean isPDFExportAvailable() { return true; }
    public ReportsRepository getReportsRepository() { return reportsRepository; }

    private void buildHeader(Document doc, String reportTitle, LocalDate start, LocalDate end) throws DocumentException {
        PdfPTable band = new PdfPTable(1);
        band.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(NAVY); cell.setPadding(22); cell.setBorder(Rectangle.NO_BORDER);
        Paragraph name = new Paragraph("K-LINE TYRE HOUSE", titleFont()); name.setAlignment(Element.ALIGN_CENTER);
        Paragraph sub = new Paragraph(reportTitle, subtitleFont()); sub.setAlignment(Element.ALIGN_CENTER);
        Paragraph meta = new Paragraph("Period:  " + start.format(DATE_FMT) + "  -  " + end.format(DATE_FMT) + "     |     Generated:  " + LocalDate.now().format(DATE_FMT), metaFont());
        meta.setAlignment(Element.ALIGN_CENTER); meta.setSpacingBefore(6);
        cell.addElement(name); cell.addElement(sub); cell.addElement(meta);
        band.addCell(cell); doc.add(band); doc.add(spacer(10));
    }

    private void buildFinancialSummary(Document doc, FinancialSummary s) throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(62); t.setHorizontalAlignment(Element.ALIGN_LEFT);
        t.setWidths(new float[]{3.5f, 2f}); t.setSpacingBefore(4); t.setSpacingAfter(14);

        summaryRow(t, "Invoice Sales Revenue", "Rs. " + fmt(s.getTotalSales()),         GRAY_LIGHT);
        summaryRow(t, "Credit Sales",          "Rs. " + fmt(s.getCreditSales()),         Color.WHITE);
        summaryRow(t, "Service Revenue",       "Rs. " + fmt(s.getServiceRevenue()),      GRAY_LIGHT);
        summaryRow(t, "Quick Services",        "Rs. " + fmt(s.getQuickServiceRevenue()), Color.WHITE);
        summaryRow(t, "Tyre Export Revenue",   "Rs. " + fmt(s.getTyreExportRevenue()),   GRAY_LIGHT);
        summaryRowTotal(t, "TOTAL REVENUE",    "Rs. " + fmt(s.getTotalRevenue()),        BLUE_LIGHT, totFont(BLUE));
        emptyRow(t);
        summaryRow(t, "Product Costs (COGS)", "Rs. " + fmt(s.getProductCosts()),  GRAY_LIGHT);
        summaryRow(t, "Total Expenses",       "Rs. " + fmt(s.getTotalExpenses()),  Color.WHITE);
        summaryRow(t, "Worker Costs",         "Rs. " + fmt(s.getWorkerCosts()),    GRAY_LIGHT);
        summaryRowTotal(t, "TOTAL COSTS",     "Rs. " + fmt(s.getTotalCosts()),     RED_LIGHT, totFont(RED));
        emptyRow(t);
        boolean pos = s.getNetProfit() >= 0;
        summaryRowTotal(t, "NET PROFIT", "Rs. " + fmt(s.getNetProfit()), pos ? GREEN_LIGHT : RED_LIGHT, totFont(pos ? GREEN : RED));
        doc.add(t);
    }

    private void buildSalesTable(Document doc, java.util.List<SaleItem> sales) throws DocumentException {
        PdfPTable t = new PdfPTable(5);
        t.setWidthPercentage(100); t.setWidths(new float[]{2f,4.5f,1.2f,2.5f,2.5f});
        t.setSpacingBefore(4); t.setSpacingAfter(14);
        tblHeader(t, "Date", "Product / Item", "Qty", "Revenue", "Profit");

        double totalRev = 0, totalPft = 0; boolean alt = false;
        for (SaleItem item : sales.stream().sorted((a,b)->b.date().compareTo(a.date())).toList()) {
            boolean isCredit = item.name().endsWith("(Credit)");
            boolean isExport = item.name().startsWith("Tyre Export -");
            Color bg = isCredit ? PURPLE_LIGHT : isExport ? ORANGE_LIGHT : (alt ? GRAY_LIGHT : Color.WHITE); alt = !alt;
            tblCell(t, item.date().format(DATE_FMT), subtleFont(), bg, Element.ALIGN_LEFT);
            tblCell(t, truncate(item.name(), 42), isCredit ? purpleFont() : isExport ? orangeFont() : bodyFont(), bg, Element.ALIGN_LEFT);
            tblCell(t, String.valueOf(item.qty()), bodyFont(), bg, Element.ALIGN_CENTER);
            tblCell(t, "Rs. " + fmt(item.revenue()), boldFont(), bg, Element.ALIGN_RIGHT);
            tblCell(t, "Rs. " + fmt(item.profit()), item.profit()>=0 ? greenFont() : redFont(), bg, Element.ALIGN_RIGHT);
            totalRev += item.revenue(); totalPft += item.profit();
        }
        tblFooter(t, "TOTALS", "", "", "Rs. " + fmt(totalRev), "Rs. " + fmt(totalPft));
        doc.add(t);
    }

    private void buildServicesTable(Document doc, java.util.List<ServiceItem> services) throws DocumentException {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100); t.setWidths(new float[]{2f,5f,2.5f,2f});
        t.setSpacingBefore(4); t.setSpacingAfter(14);
        tblHeader(t, "Date", "Service", "Source", "Fee");

        double total = 0; boolean alt = false;
        for (ServiceItem item : services.stream().sorted((a,b)->b.date().compareTo(a.date())).toList()) {
            boolean isQuick = "Quick Service".equals(item.assignedTo());
            boolean isInv   = "Service Invoice".equals(item.assignedTo());
            Color bg = isQuick ? new Color(240,253,244) : (alt ? GRAY_LIGHT : Color.WHITE); alt = !alt;
            tblCell(t, item.date().format(DATE_FMT), subtleFont(), bg, Element.ALIGN_LEFT);
            tblCell(t, truncate(item.name(), 50), bodyFont(), bg, Element.ALIGN_LEFT);
            tblCell(t, item.assignedTo()!=null ? item.assignedTo() : "Standard", isQuick ? greenFont() : isInv ? blueFont() : bodyFont(), bg, Element.ALIGN_CENTER);
            tblCell(t, "Rs. " + fmt(item.fee()), boldFont(), bg, Element.ALIGN_RIGHT);
            total += item.fee();
        }
        tblFooter(t, "TOTAL", "", "", "Rs. " + fmt(total));
        doc.add(t);
    }

    private void buildExpensesSection(Document doc, java.util.List<ExpenseItem> expenses) throws DocumentException {
        Map<String, java.util.List<ExpenseItem>> byCategory = expenses.stream().collect(Collectors.groupingBy(e->normalizeCategory(e.getCategory())));
        double grandTotal = 0;
        for (Map.Entry<String, java.util.List<ExpenseItem>> entry : byCategory.entrySet()) {
            Paragraph catLabel = new Paragraph(entry.getKey(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BLUE));
            catLabel.setSpacingBefore(10); catLabel.setSpacingAfter(3); doc.add(catLabel);

            PdfPTable t = new PdfPTable(3);
            t.setWidthPercentage(100); t.setWidths(new float[]{2f,5.5f,2.5f}); t.setSpacingAfter(4);
            tblHeader(t, "Date", "Description", "Amount");
            double catTotal = 0; boolean alt = false;
            for (ExpenseItem exp : entry.getValue()) {
                Color bg = alt ? GRAY_LIGHT : Color.WHITE; alt = !alt;
                tblCell(t, exp.getDate().format(DATE_FMT), subtleFont(), bg, Element.ALIGN_LEFT);
                tblCell(t, truncate(exp.getDescription(), 60), bodyFont(), bg, Element.ALIGN_LEFT);
                tblCell(t, "Rs. " + fmt(exp.getAmount()), boldFont(), bg, Element.ALIGN_RIGHT);
                catTotal += exp.getAmount();
            }
            tblFooter(t, "", "Category Total:", "Rs. " + fmt(catTotal));
            doc.add(t); grandTotal += catTotal;
        }
        doc.add(spacer(6));
        PdfPTable gt = new PdfPTable(3); gt.setWidthPercentage(100); gt.setWidths(new float[]{2f,5.5f,2.5f});
        PdfPCell empty = new PdfPCell(new Phrase("")); empty.setBorder(Rectangle.NO_BORDER); gt.addCell(empty);
        Font tf = totFont(RED);
        PdfPCell lc2 = new PdfPCell(new Phrase("GRAND TOTAL EXPENSES:", tf)); lc2.setBackgroundColor(RED_LIGHT); lc2.setPadding(8); lc2.setBorderColor(GRAY_BORDER); lc2.setBorderWidth(0.5f); gt.addCell(lc2);
        PdfPCell vc2 = new PdfPCell(new Phrase("Rs. " + fmt(grandTotal), tf)); vc2.setBackgroundColor(RED_LIGHT); vc2.setPadding(8); vc2.setBorderColor(GRAY_BORDER); vc2.setBorderWidth(0.5f); vc2.setHorizontalAlignment(Element.ALIGN_RIGHT); gt.addCell(vc2);
        doc.add(gt);
    }

    private void buildCustomerTable(Document doc, java.util.List<CustomerSummary> customers) throws DocumentException {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100); t.setWidths(new float[]{3.5f, 2.5f, 2.5f, 2.5f});
        t.setSpacingBefore(4); t.setSpacingAfter(14);
        tblHeader(t, "Customer / Company", "Total Amount", "Total Paid", "Outstanding Balance");

        double totalCredit = 0, totalPaid = 0, totalBal = 0; boolean alt = false;
        for (CustomerSummary c : customers) {
            Color bg = alt ? GRAY_LIGHT : Color.WHITE; alt = !alt;
            tblCell(t, truncate(c.getCustomer(), 40), boldFont(), bg, Element.ALIGN_LEFT);
            tblCell(t, "Rs. " + fmt(c.getTotalAmount()), bodyFont(), bg, Element.ALIGN_RIGHT);
            tblCell(t, "Rs. " + fmt(c.getTotalPaid()), greenFont(), bg, Element.ALIGN_RIGHT);
            tblCell(t, "Rs. " + fmt(c.getOutstanding()), c.getOutstanding() > 0 ? redFont() : bodyFont(), bg, Element.ALIGN_RIGHT);
            totalCredit += c.getTotalAmount(); totalPaid += c.getTotalPaid(); totalBal += c.getOutstanding();
        }
        tblFooter(t, "TOTALS", "Rs. " + fmt(totalCredit), "Rs. " + fmt(totalPaid), "Rs. " + fmt(totalBal));
        doc.add(t);
    }

    private void buildPaymentTable(Document doc, java.util.List<PaymentItem> payments) throws DocumentException {
        PdfPTable t = new PdfPTable(5);
        t.setWidthPercentage(100); t.setWidths(new float[]{2f, 3.5f, 2f, 2f, 2.5f});
        t.setSpacingBefore(4); t.setSpacingAfter(14);
        tblHeader(t, "Date", "Customer", "Type", "Method", "Amount");

        double totalAmount = 0; boolean alt = false;
        for (PaymentItem p : payments) {
            Color bg = alt ? GRAY_LIGHT : Color.WHITE; alt = !alt;
            tblCell(t, p.date().format(DATE_FMT), subtleFont(), bg, Element.ALIGN_LEFT);
            tblCell(t, truncate(p.customerOrCompany(), 35), bodyFont(), bg, Element.ALIGN_LEFT);
            tblCell(t, p.type() != null ? p.type() : "Settlement", blueFont(), bg, Element.ALIGN_CENTER);
            tblCell(t, p.method() != null ? p.method() : "Cash", bodyFont(), bg, Element.ALIGN_CENTER);
            tblCell(t, "Rs. " + fmt(p.amount()), greenFont(), bg, Element.ALIGN_RIGHT);
            totalAmount += p.amount();
        }
        tblFooter(t, "TOTAL", "", "", "", "Rs. " + fmt(totalAmount));
        doc.add(t);
    }

    private void addSectionBand(Document doc, String title) throws DocumentException {
        PdfPTable band = new PdfPTable(1); band.setWidthPercentage(100); band.setSpacingBefore(14); band.setSpacingAfter(0);
        PdfPCell cell = new PdfPCell(new Phrase("  " + title, secFont()));
        cell.setBackgroundColor(GRAY_HEADER); cell.setPadding(8); cell.setBorderColor(GRAY_BORDER); cell.setBorderWidth(0.5f);
        band.addCell(cell); doc.add(band);
    }

    private void summaryRow(PdfPTable t, String label, String value, Color bg) {
        PdfPCell lc = new PdfPCell(new Phrase(label, bodyFont())); lc.setBackgroundColor(bg); lc.setPadding(6); lc.setBorderColor(GRAY_BORDER); lc.setBorderWidth(0.5f);
        PdfPCell vc = new PdfPCell(new Phrase(value, bodyFont())); vc.setBackgroundColor(bg); vc.setPadding(6); vc.setHorizontalAlignment(Element.ALIGN_RIGHT); vc.setBorderColor(GRAY_BORDER); vc.setBorderWidth(0.5f);
        t.addCell(lc); t.addCell(vc);
    }

    private void summaryRowTotal(PdfPTable t, String label, String value, Color bg, Font f) {
        PdfPCell lc = new PdfPCell(new Phrase(label, f)); lc.setBackgroundColor(bg); lc.setPadding(8); lc.setBorderColor(GRAY_BORDER); lc.setBorderWidth(0.5f);
        PdfPCell vc = new PdfPCell(new Phrase(value, f)); vc.setBackgroundColor(bg); vc.setPadding(8); vc.setHorizontalAlignment(Element.ALIGN_RIGHT); vc.setBorderColor(GRAY_BORDER); vc.setBorderWidth(0.5f);
        t.addCell(lc); t.addCell(vc);
    }

    private void emptyRow(PdfPTable t) {
        for (int i = 0; i < t.getNumberOfColumns(); i++) { PdfPCell c = new PdfPCell(new Phrase("")); c.setBorder(Rectangle.NO_BORDER); c.setPadding(3); t.addCell(c); }
    }

    private void tblHeader(PdfPTable t, String... headers) {
        for (String h : headers) { PdfPCell c = new PdfPCell(new Phrase(h, tblHdrFont())); c.setBackgroundColor(GRAY_DARK); c.setPadding(7); c.setBorder(Rectangle.NO_BORDER); c.setHorizontalAlignment(Element.ALIGN_CENTER); t.addCell(c); }
    }

    private void tblCell(PdfPTable t, String text, Font f, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, f)); c.setBackgroundColor(bg); c.setPadding(5); c.setBorderColor(GRAY_BORDER); c.setBorderWidth(0.5f); c.setHorizontalAlignment(align); t.addCell(c);
    }

    private void tblFooter(PdfPTable t, String... values) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, NAVY);
        for (int i = 0; i < values.length; i++) {
            PdfPCell c = new PdfPCell(new Phrase(values[i], f)); c.setBackgroundColor(GRAY_HEADER); c.setPadding(7); c.setBorderColor(new Color(209,213,219)); c.setBorderWidth(0.5f);
            c.setHorizontalAlignment(i==values.length-1 ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT); t.addCell(c);
        }
    }

    private Paragraph spacer(float h) { Paragraph p = new Paragraph(" "); p.setSpacingAfter(h); return p; }

    private String normalizeCategory(String cat) {
        if (cat==null||cat.isBlank()) return "Other";
        String l = cat.trim().toLowerCase();
        if (l.contains("worker")||l.contains("salary")||l.contains("payroll")) return "Worker Salary";
        if (l.contains("transport")||l.contains("freight")||l.contains("delivery")) return "Transport";
        if (l.contains("tyre")||l.contains("export")) return "Tyre Purchase";
        return cat.trim().substring(0,1).toUpperCase()+cat.trim().substring(1);
    }

    private String fmt(double v) { NF.setMaximumFractionDigits(0); return NF.format(v); }
    private String truncate(String s, int max) { if (s==null) return ""; return s.length()<=max ? s : s.substring(0,max-3)+"..."; }
}
