package com.gui.kline.service;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;

import com.gui.kline.models.InvoiceDetail;
import com.gui.kline.models.LineItem;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/** Creates a printable PDF copy of a completed invoice. */
public class InvoicePdfService {

    public void export(InvoiceDetail invoice, File outputFile) throws Exception {
        Document document = new Document(PageSize.A4, 40, 40, 45, 45);
        FileOutputStream output = new FileOutputStream(outputFile);
        try {
            PdfWriter.getInstance(document, output);
            document.open();

            Font title = new Font(Font.HELVETICA, 20, Font.BOLD);
            Font heading = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font normal = new Font(Font.HELVETICA, 10);

            document.add(new Paragraph("K-LINE TYRE HOUSE", title));
            document.add(new Paragraph("INVOICE", heading));
            document.add(new Paragraph("Invoice: " + value(invoice.getInvoiceId()) + "    Date: "
                    + value(invoice.getDate(), LocalDate.now().toString()), normal));
            document.add(new Paragraph("Customer: " + value(invoice.getCustomer()), normal));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[] {4.5f, 1f, 1.7f, 1.8f});
            table.setWidthPercentage(100);
            addHeader(table, "Description", heading);
            addHeader(table, "Qty", heading);
            addHeader(table, "Unit price", heading);
            addHeader(table, "Total", heading);
            for (LineItem item : invoice.getLineItems()) {
                addCell(table, value(item.getDescription()), normal);
                addCell(table, String.valueOf(item.getQty()), normal);
                addCell(table, currency(item.getUnitPrice()), normal);
                addCell(table, currency(item.getTotal()), normal);
            }
            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Subtotal: Rs. " + currency(invoice.getSubtotal()), normal));
            document.add(new Paragraph("Discount: Rs. " + currency(invoice.getDiscountAmount()), normal));
            document.add(new Paragraph("Grand Total: Rs. " + currency(invoice.getGrandTotal()), heading));
        } finally {
            // Closing the document flushes and closes the underlying writer/stream.
            // This must happen BEFORE the stream is released, otherwise the flush
            // targets an already-closed stream ("stream is closed").
            if (document != null) {
                document.close();
            }
        }
    }

    private void addHeader(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(value, font));
        cell.setPadding(7);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(value, font));
        cell.setPadding(6);
        table.addCell(cell);
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
