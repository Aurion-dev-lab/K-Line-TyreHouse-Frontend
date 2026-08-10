package com.gui.kline.service;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.gui.kline.models.dto.InvoiceDetail;
import com.gui.kline.models.dto.LineItem;
import com.gui.kline.models.dto.PaymentRecord;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Modern, professional PDF generation service for completed invoices,
 * credit sales, and tyre export transactions.
 */
public class InvoicePdfService {

    // Palette Colors
    private static final Color NAVY         = new Color(15, 23, 42);    // #0F172A (Primary Header)
    private static final Color ACCENT_BLUE  = new Color(37, 99, 235);   // #2563EB (Accent)
    private static final Color DARK_TEXT    = new Color(30, 41, 59);    // #1E293B (Body Text)
    private static final Color MUTED_TEXT   = new Color(100, 116, 139); // #64748B (Secondary Text)
    private static final Color LIGHT_BG     = new Color(248, 250, 252); // #F8FAFC (Card/Zebra BG)
    private static final Color BORDER_COLOR = new Color(226, 232, 240); // #E2E8F0 (Grid borders)
    private static final Color SUBHEADER_BG = new Color(51, 65, 85);    // #334155 (Table Header 2)

    private static final Color PAID_GREEN_TEXT = new Color(22, 101, 52);  // #166534
    private static final Color PAID_GREEN_BG   = new Color(220, 252, 231); // #DCFCE7
    private static final Color UNPAID_RED_TEXT  = new Color(154, 52, 18);  // #9A3412
    private static final Color UNPAID_RED_BG    = new Color(255, 237, 213); // #FFEDD5

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    // Fonts
    private final Font fontTitle        = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, NAVY);
    private final Font fontSubtitle     = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED_TEXT);
    private final Font fontDocTitle     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, NAVY);
    private final Font fontSectionHeader= FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, NAVY);
    private final Font fontTableHeader  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
    private final Font fontTextBold     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, DARK_TEXT);
    private final Font fontTextNormal   = FontFactory.getFont(FontFactory.HELVETICA, 8, DARK_TEXT);
    private final Font fontTextMuted    = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED_TEXT);
    private final Font fontBadge        = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

    public void export(InvoiceDetail invoice, File outputFile) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        FileOutputStream output = new FileOutputStream(outputFile);
        try {
            PdfWriter.getInstance(document, output);
            document.open();

            // 1. Header Banner (Company Info & Document Title)
            addHeaderBanner(document, invoice);

            // Spacer
            addSpacer(document, 10);

            // 2. Info Cards (Billed To & Reference Metadata)
            addInfoCards(document, invoice);

            // Spacer
            addSpacer(document, 12);

            // 3. Line Items Table
            addLineItemsTable(document, invoice);

            // Spacer
            addSpacer(document, 10);

            // 4. Financial Totals Box
            addFinancialTotals(document, invoice);

            // Spacer
            addSpacer(document, 14);

            // 5. Settlement & Payment History Section
            addSettlementHistorySection(document, invoice);

            // Spacer
            addSpacer(document, 14);

            // 6. Terms, Conditions & Invoice Criteria Section
            addTermsAndCriteriaSection(document, invoice);

            // Spacer
            addSpacer(document, 16);

            // 7. Sign-off & Footer
            addFooter(document);

        } finally {
            if (document != null) {
                document.close();
            }
        }
    }

    private void addHeaderBanner(Document document, InvoiceDetail invoice) throws Exception {
        PdfPTable headerTable = new PdfPTable(new float[]{60f, 40f});
        headerTable.setWidthPercentage(100);

        // Left Cell: Company Branding
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("K-LINE TYRE HOUSE", fontTitle));
        leftCell.addElement(new Paragraph("Quality Tyres, Rims & Auto Care Center", fontSubtitle));
        leftCell.addElement(new Paragraph("Phone: +94 77 123 4567  |  Email: info@klinetyrehouse.lk", fontSubtitle));
        headerTable.addCell(leftCell);

        // Right Cell: Document Title & Badge
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        String statusStr = value(invoice.getStatus(), "quotation").toLowerCase();
        boolean isQuotation = "quotation".equalsIgnoreCase(statusStr);
        boolean isSettled = invoice.isFullyPaid();

        String docType;
        if (isQuotation) {
            docType = "SALES QUOTATION";
        } else {
            docType = value(invoice.getType(), "INVOICE").toUpperCase();
            if (!docType.contains("INVOICE") && !docType.contains("STATEMENT")) {
                docType = docType + " INVOICE";
            }
        }

        Paragraph titlePara = new Paragraph(docType, fontDocTitle);
        titlePara.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(titlePara);

        Paragraph idPara = new Paragraph("Doc ID: #" + value(invoice.getInvoiceId()), fontTextBold);
        idPara.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(idPara);

        String dateVal = value(invoice.getDate(), LocalDate.now().toString());
        Paragraph datePara = new Paragraph("Date: " + dateVal, fontTextNormal);
        datePara.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(datePara);

        // Status Badge
        String statusText;
        Color textClr, bgClr;
        if (isQuotation) {
            statusText = "QUOTATION (UNPAID ESTIMATE)";
            textClr = new Color(51, 65, 85);   // #334155 Slate
            bgClr   = new Color(241, 245, 249); // #F1F5F9 Slate BG
        } else if (isSettled) {
            statusText = "PAID / SETTLED";
            textClr = PAID_GREEN_TEXT;
            bgClr   = PAID_GREEN_BG;
        } else {
            statusText = String.format("AMOUNT REMAINING: Rs. %,.2f", invoice.getBalanceDue());
            textClr = UNPAID_RED_TEXT;
            bgClr   = UNPAID_RED_BG;
        }
        
        PdfPTable badgeTable = new PdfPTable(1);
        badgeTable.setWidthPercentage(85);
        badgeTable.setSpacingBefore(8f);
        badgeTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        
        Font bFont = new Font(fontBadge);
        bFont.setColor(textClr);
        
        PdfPCell badgeCell = new PdfPCell(new Paragraph(statusText, bFont));
        badgeCell.setBackgroundColor(bgClr);
        badgeCell.setBorderColor(textClr);
        badgeCell.setBorderWidth(1f);
        badgeCell.setPadding(5);
        badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        badgeTable.addCell(badgeCell);

        rightCell.addElement(badgeTable);
        headerTable.addCell(rightCell);

        document.add(headerTable);

        // Accent Divider Line
        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setFixedHeight(2f);
        lineCell.setBackgroundColor(ACCENT_BLUE);
        lineCell.setBorder(Rectangle.NO_BORDER);
        divider.addCell(lineCell);
        
        addSpacer(document, 4);
        document.add(divider);
    }

    private void addInfoCards(Document document, InvoiceDetail invoice) throws Exception {
        PdfPTable cardsTable = new PdfPTable(new float[]{49f, 2f, 49f});
        cardsTable.setWidthPercentage(100);

        // Left Card: Customer Details
        PdfPCell leftCard = new PdfPCell();
        leftCard.setBackgroundColor(LIGHT_BG);
        leftCard.setBorderColor(BORDER_COLOR);
        leftCard.setBorderWidth(1f);
        leftCard.setPadding(8);

        leftCard.addElement(new Paragraph("BILLED TO / CUSTOMER", fontSectionHeader));
        leftCard.addElement(new Paragraph("Name:  " + value(invoice.getCustomer()), fontTextBold));
        if (!invoice.getPhone().isBlank()) {
            leftCard.addElement(new Paragraph("Phone: " + invoice.getPhone(), fontTextNormal));
        }
        if (!invoice.getVehicleNumber().isBlank()) {
            leftCard.addElement(new Paragraph("Vehicle No: " + invoice.getVehicleNumber(), fontTextNormal));
        }

        // Middle Spacer Cell
        PdfPCell gap = new PdfPCell();
        gap.setBorder(Rectangle.NO_BORDER);

        // Right Card: Reference Details
        PdfPCell rightCard = new PdfPCell();
        rightCard.setBackgroundColor(LIGHT_BG);
        rightCard.setBorderColor(BORDER_COLOR);
        rightCard.setBorderWidth(1f);
        rightCard.setPadding(8);

        rightCard.addElement(new Paragraph("TRANSACTION DETAILS", fontSectionHeader));
        rightCard.addElement(new Paragraph("Issue Date: " + value(invoice.getDate()), fontTextNormal));
        if (!invoice.getDueDate().isBlank()) {
            rightCard.addElement(new Paragraph("Due Date:   " + invoice.getDueDate(), fontTextNormal));
        }
        if (!invoice.getSerialNumber().isBlank()) {
            rightCard.addElement(new Paragraph("Serial No:  " + invoice.getSerialNumber(), fontTextNormal));
        }
        if (!invoice.getTyreSize().isBlank() || !invoice.getTyreMake().isBlank()) {
            rightCard.addElement(new Paragraph("Tyre Spec:  " + value(invoice.getTyreSize()) + " " + value(invoice.getTyreMake()), fontTextNormal));
        }
        if (!invoice.getRemark().isBlank()) {
            rightCard.addElement(new Paragraph("Remarks:    " + invoice.getRemark(), fontTextMuted));
        }

        cardsTable.addCell(leftCard);
        cardsTable.addCell(gap);
        cardsTable.addCell(rightCard);

        document.add(cardsTable);
    }

    private void addLineItemsTable(Document document, InvoiceDetail invoice) throws Exception {
        document.add(new Paragraph("ITEMIZED ITEMS & SERVICES", fontSectionHeader));
        addSpacer(document, 4);

        PdfPTable table = new PdfPTable(new float[]{0.6f, 3.8f, 1.2f, 0.8f, 1.6f, 1.8f});
        table.setWidthPercentage(100);

        addTableHeader(table, "#");
        addTableHeader(table, "Description / Item");
        addTableHeader(table, "Category");
        addTableHeader(table, "Qty");
        addTableHeader(table, "Unit Price (Rs.)");
        addTableHeader(table, "Total (Rs.)");

        List<LineItem> items = invoice.getLineItems();
        if (items.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Paragraph("No items recorded.", fontTextMuted));
            empty.setColspan(6);
            empty.setPadding(8);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(empty);
        } else {
            int rowIdx = 1;
            for (LineItem item : items) {
                Color bg = (rowIdx % 2 == 0) ? LIGHT_BG : Color.WHITE;

                addTableCell(table, String.valueOf(rowIdx), fontTextNormal, Element.ALIGN_CENTER, bg);
                addTableCell(table, value(item.getDescription()), fontTextBold, Element.ALIGN_LEFT, bg);
                addTableCell(table, value(item.getType(), "Sale"), fontTextNormal, Element.ALIGN_LEFT, bg);
                addTableCell(table, String.valueOf(item.getQty()), fontTextNormal, Element.ALIGN_CENTER, bg);
                addTableCell(table, currency(item.getUnitPrice()), fontTextNormal, Element.ALIGN_RIGHT, bg);
                addTableCell(table, currency(item.getTotal()), fontTextBold, Element.ALIGN_RIGHT, bg);

                rowIdx++;
            }
        }

        document.add(table);
    }

    private void addFinancialTotals(Document document, InvoiceDetail invoice) throws Exception {
        PdfPTable container = new PdfPTable(new float[]{50f, 50f});
        container.setWidthPercentage(100);

        // Left Cell: Empty or Notes
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        if (!invoice.getDescription().isBlank()) {
            leftCell.addElement(new Paragraph("Notes / Description:", fontSectionHeader));
            leftCell.addElement(new Paragraph(invoice.getDescription(), fontTextMuted));
        }

        // Right Cell: Financial Breakdown Box
        PdfPTable totalsTable = new PdfPTable(new float[]{55f, 45f});
        totalsTable.setWidthPercentage(100);

        addTotalRow(totalsTable, "Subtotal:", currency(invoice.getSubtotal()), fontTextNormal, false);

        if (invoice.getDiscountAmount() > 0) {
            addTotalRow(totalsTable, "Discount:", "- " + currency(invoice.getDiscountAmount()), fontTextNormal, false);
        }
        if (invoice.getTax() > 0) {
            addTotalRow(totalsTable, "Tax:", currency(invoice.getTax()), fontTextNormal, false);
        }

        addTotalRow(totalsTable, "Grand Total:", "Rs. " + currency(invoice.getGrandTotal()), fontTextBold, true);

        boolean isQuotation = "quotation".equalsIgnoreCase(invoice.getStatus());
        double totalPaid = invoice.getTotalPaid();
        double balanceDue = invoice.getBalanceDue();
        boolean isSettled = invoice.isFullyPaid();

        if (isQuotation) {
            PdfPCell quoteCell = new PdfPCell();
            quoteCell.setColspan(2);
            quoteCell.setBackgroundColor(LIGHT_BG);
            quoteCell.setBorderColor(BORDER_COLOR);
            quoteCell.setBorderWidth(1f);
            quoteCell.setPadding(6);
            quoteCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph qPara1 = new Paragraph("QUOTATION TOTAL: Rs. " + currency(invoice.getGrandTotal()), fontTextBold);
            qPara1.setAlignment(Element.ALIGN_CENTER);
            quoteCell.addElement(qPara1);

            Paragraph qPara2 = new Paragraph("This is an estimated price quotation, not a paid tax invoice.", fontTextMuted);
            qPara2.setAlignment(Element.ALIGN_CENTER);
            quoteCell.addElement(qPara2);

            totalsTable.addCell(quoteCell);
        } else if (isSettled) {
            addTotalRow(totalsTable, "Total Payments Recorded:", "Rs. " + currency(totalPaid), fontTextBold, false);

            Font settledHeaderFont = new Font(fontBadge);
            settledHeaderFont.setColor(PAID_GREEN_TEXT);
            settledHeaderFont.setSize(9.5f);

            Font settledSubFont = new Font(fontTextNormal);
            settledSubFont.setColor(PAID_GREEN_TEXT);

            PdfPCell settledCell = new PdfPCell();
            settledCell.setColspan(2);
            settledCell.setBackgroundColor(PAID_GREEN_BG);
            settledCell.setBorderColor(PAID_GREEN_TEXT);
            settledCell.setBorderWidth(1.2f);
            settledCell.setPadding(7);
            settledCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph titleP = new Paragraph("✓ BILL FULLY SETTLED & PAID IN FULL", settledHeaderFont);
            titleP.setAlignment(Element.ALIGN_CENTER);
            settledCell.addElement(titleP);

            Paragraph subP = new Paragraph(String.format("Total Amount Paid: Rs. %,.2f   |   Outstanding Balance: Rs. 0.00", totalPaid), settledSubFont);
            subP.setAlignment(Element.ALIGN_CENTER);
            settledCell.addElement(subP);

            totalsTable.addCell(settledCell);
        } else {
            if (invoice.getPaymentHistory().isEmpty() && invoice.getInitialPayment() > 0) {
                addTotalRow(totalsTable, "Initial Payment:", currency(invoice.getInitialPayment()), fontTextNormal, false);
            }
            addTotalRow(totalsTable, "Total Payments Recorded:", "Rs. " + currency(totalPaid), fontTextBold, false);

            Font balFont = new Font(fontTextBold);
            balFont.setColor(UNPAID_RED_TEXT);
            balFont.setSize(9);

            PdfPCell lblCell = new PdfPCell(new Paragraph("AMOUNT REMAINING TO PAY:", balFont));
            lblCell.setBackgroundColor(UNPAID_RED_BG);
            lblCell.setBorderColor(UNPAID_RED_TEXT);
            lblCell.setPadding(6);

            PdfPCell valCell = new PdfPCell(new Paragraph("Rs. " + currency(balanceDue), balFont));
            valCell.setBackgroundColor(UNPAID_RED_BG);
            valCell.setBorderColor(UNPAID_RED_TEXT);
            valCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            valCell.setPadding(6);

            totalsTable.addCell(lblCell);
            totalsTable.addCell(valCell);
        }

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.addElement(totalsTable);

        container.addCell(leftCell);
        container.addCell(rightCell);

        document.add(container);
    }

    private void addTermsAndCriteriaSection(Document document, InvoiceDetail invoice) throws Exception {
        document.add(new Paragraph("TERMS, CONDITIONS & INVOICE CRITERIA", fontSectionHeader));
        addSpacer(document, 4);

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(LIGHT_BG);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBorderWidth(1f);
        cell.setPadding(8);

        if (!invoice.getPaymentTerms().isBlank()) {
            cell.addElement(new Paragraph("Payment Terms: " + invoice.getPaymentTerms(), fontTextBold));
        }

        String criteria = invoice.getTermsAndConditions();
        for (String line : criteria.split("\n")) {
            if (!line.isBlank()) {
                cell.addElement(new Paragraph(line.trim(), fontTextMuted));
            }
        }

        table.addCell(cell);
        document.add(table);
    }

    private void addSettlementHistorySection(Document document, InvoiceDetail invoice) throws Exception {
        List<PaymentRecord> history = invoice.getPaymentHistory();
        if (history == null || history.isEmpty()) {
            return; // Omit settlement history section entirely if there are no settlement records
        }

        document.add(new Paragraph("SETTLEMENT & PAYMENT HISTORY", fontSectionHeader));
        addSpacer(document, 4);

        PdfPTable table = new PdfPTable(new float[]{0.6f, 1.8f, 1.8f, 3.6f, 2.2f});
        table.setWidthPercentage(100);

        addSubTableHeader(table, "#");
        addSubTableHeader(table, "Payment Date");
        addSubTableHeader(table, "Method");
        addSubTableHeader(table, "Notes / Reference");
        addSubTableHeader(table, "Amount Paid (Rs.)");

        int idx = 1;
        double runningTotal = 0.0;
        for (PaymentRecord p : history) {
            Color bg = (idx % 2 == 0) ? LIGHT_BG : Color.WHITE;
            String dateStr = p.getDate() != null ? p.getDate().format(DATE_FMT) : "—";
            String notesStr = (p.getNotes() != null && !p.getNotes().isBlank()) ? p.getNotes() : "Settlement Payment";

            addTableCell(table, String.valueOf(idx), fontTextNormal, Element.ALIGN_CENTER, bg);
            addTableCell(table, dateStr, fontTextNormal, Element.ALIGN_LEFT, bg);
            addTableCell(table, value(p.getMethod(), "Cash"), fontTextBold, Element.ALIGN_LEFT, bg);
            addTableCell(table, notesStr, fontTextNormal, Element.ALIGN_LEFT, bg);
            addTableCell(table, currency(p.getAmount()), fontTextBold, Element.ALIGN_RIGHT, bg);

            runningTotal += p.getAmount();
            idx++;
        }

        // Summary row for total settlements
        PdfPCell sumLblCell = new PdfPCell(new Paragraph("Total Settlements Recorded:", fontTextBold));
        sumLblCell.setColspan(4);
        sumLblCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        sumLblCell.setPadding(6);
        sumLblCell.setBackgroundColor(LIGHT_BG);
        table.addCell(sumLblCell);

        PdfPCell sumValCell = new PdfPCell(new Paragraph("Rs. " + currency(runningTotal), fontTextBold));
        sumValCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        sumValCell.setPadding(6);
        sumValCell.setBackgroundColor(LIGHT_BG);
        table.addCell(sumValCell);

        document.add(table);
        addSpacer(document, 14);
    }

    private void addFooter(Document document) throws Exception {
        PdfPTable footerTable = new PdfPTable(new float[]{60f, 40f});
        footerTable.setWidthPercentage(100);

        // Left Cell: Thank you & Timestamp
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("Thank you for choosing K-Line Tyre House!", fontTextBold));
        leftCell.addElement(new Paragraph("Generated: " + LocalDateTime.now().format(TIME_FMT), fontSubtitle));

        // Right Cell: Signature Line
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph sigLine = new Paragraph("___________________________", fontTextMuted);
        sigLine.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(sigLine);

        Paragraph sigText = new Paragraph("Authorized Signature", fontTextMuted);
        sigText.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(sigText);

        footerTable.addCell(leftCell);
        footerTable.addCell(rightCell);

        document.add(footerTable);
    }

    // Helper methods for PDF rendering
    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, fontTableHeader));
        cell.setBackgroundColor(NAVY);
        cell.setPadding(6);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private void addSubTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, fontTableHeader));
        cell.setBackgroundColor(SUBHEADER_BG);
        cell.setPadding(5);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(align);
        cell.setPadding(5);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String val, Font font, boolean isHeader) {
        PdfPCell lblCell = new PdfPCell(new Paragraph(label, font));
        lblCell.setPadding(4);
        lblCell.setBorderColor(BORDER_COLOR);
        if (isHeader) lblCell.setBackgroundColor(LIGHT_BG);

        PdfPCell valCell = new PdfPCell(new Paragraph(val, font));
        valCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valCell.setPadding(4);
        valCell.setBorderColor(BORDER_COLOR);
        if (isHeader) valCell.setBackgroundColor(LIGHT_BG);

        table.addCell(lblCell);
        table.addCell(valCell);
    }

    private void addSpacer(Document document, float height) throws Exception {
        Paragraph p = new Paragraph(" ");
        p.setLeading(height);
        document.add(p);
    }

    private String currency(double amount) {
        return String.format("%,.2f", amount);
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
