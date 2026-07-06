package com.onlinebanking.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.onlinebanking.entity.Account;
import com.onlinebanking.entity.Customer;
import com.onlinebanking.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
@RequiredArgsConstructor
public class StatementExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("ddMMMyyyy");

    // ─── PDF ─────────────────────────────────────────────────────────────────

    public byte[] generatePdf(Customer customer, Account account, List<Transaction> transactions)
            throws DocumentException, IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 50, 40);
        PdfWriter.getInstance(doc, out);
        doc.open();

        // Fonts
        Font headerFont  = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,  new BaseColor(30, 64, 175));
        Font subFont     = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, new BaseColor(100, 116, 139));
        Font labelFont   = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,  new BaseColor(51, 65, 85));
        Font valueFont   = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, BaseColor.BLACK);
        Font tableHeader = new Font(Font.FontFamily.HELVETICA,  8, Font.BOLD,  BaseColor.WHITE);
        Font tableCell   = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, new BaseColor(30, 41, 59));
        Font creditFont  = new Font(Font.FontFamily.HELVETICA,  8, Font.BOLD,  new BaseColor(21, 128, 61));
        Font debitFont   = new Font(Font.FontFamily.HELVETICA,  8, Font.BOLD,  new BaseColor(185, 28, 28));

        // ── Bank Header ──
        Paragraph bankName = new Paragraph("SmartBank", headerFont);
        bankName.setAlignment(Element.ALIGN_CENTER);
        doc.add(bankName);

        Paragraph tagline = new Paragraph("Account Statement", subFont);
        tagline.setAlignment(Element.ALIGN_CENTER);
        tagline.setSpacingAfter(6f);
        doc.add(tagline);

        // Divider
        LineSeparator line = new LineSeparator(1f, 100f, new BaseColor(203, 213, 225), Element.ALIGN_CENTER, -2f);
        doc.add(new Chunk(line));

        // ── Account Info table ──
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1.5f, 2.5f, 1.5f, 2.5f});
        infoTable.setSpacingBefore(12f);
        infoTable.setSpacingAfter(14f);

        addInfoCell(infoTable, "Account Holder", labelFont);
        addInfoCell(infoTable, customer.getFullName(), valueFont);
        addInfoCell(infoTable, "Customer ID", labelFont);
        addInfoCell(infoTable, customer.getCustomerId(), valueFont);

        addInfoCell(infoTable, "Account Number", labelFont);
        addInfoCell(infoTable, account.getAccountNumber(), valueFont);
        addInfoCell(infoTable, "Account Type", labelFont);
        addInfoCell(infoTable, account.getAccountType().name(), valueFont);

        addInfoCell(infoTable, "Mobile", labelFont);
        addInfoCell(infoTable, customer.getMobileNumber(), valueFont);
        addInfoCell(infoTable, "Current Balance", labelFont);
        addInfoCell(infoTable, "₹" + String.format("%,.2f", account.getBalance()), valueFont);

        addInfoCell(infoTable, "Email", labelFont);
        addInfoCell(infoTable, customer.getEmail(), valueFont);
        addInfoCell(infoTable, "Statement Date", labelFont);
        addInfoCell(infoTable, java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), valueFont);

        doc.add(infoTable);

        // ── Transactions Table ──
        PdfPTable txnTable = new PdfPTable(6);
        txnTable.setWidthPercentage(100);
        txnTable.setWidths(new float[]{2.2f, 1.4f, 1.6f, 1.6f, 1.2f, 2f});

        String[] headers = {"Transaction ID", "Type", "Amount (₹)", "Balance After", "Status", "Date & Time"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, tableHeader));
            cell.setBackgroundColor(new BaseColor(30, 64, 175));
            cell.setPadding(7f);
            cell.setBorderColor(new BaseColor(30, 64, 175));
            txnTable.addCell(cell);
        }

        boolean shade = false;
        for (Transaction t : transactions) {
            BaseColor rowBg = shade ? new BaseColor(248, 250, 252) : BaseColor.WHITE;
            shade = !shade;

            boolean isCredit = t.getReceiverAccount() != null && t.getReceiverAccount().getAccountNumber().equals(account.getAccountNumber());
            Font amtFont = isCredit ? creditFont : debitFont;
            String amtPrefix = isCredit ? "+" : "-";

            addTxnCell(txnTable, t.getTransactionId(), tableCell, rowBg);
            addTxnCell(txnTable, t.getTransactionType().name(), tableCell, rowBg);

            PdfPCell amtCell = new PdfPCell(new Phrase(
                    amtPrefix + String.format("%,.2f", t.getAmount()), amtFont));
            amtCell.setPadding(6f);
            amtCell.setBackgroundColor(rowBg);
            amtCell.setBorderColor(new BaseColor(226, 232, 240));
            txnTable.addCell(amtCell);

            BigDecimal bal = isCredit ? t.getReceiverBalanceAfter() : t.getSenderBalanceAfter();
            addTxnCell(txnTable,
                    bal != null ? String.format("%,.2f", bal) : "-",
                    tableCell, rowBg);
            addTxnCell(txnTable, t.getStatus().name(), tableCell, rowBg);
            addTxnCell(txnTable,
                    t.getTransactionDate() != null ? t.getTransactionDate().format(DATE_FMT) : "-",
                    tableCell, rowBg);
        }

        if (transactions.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("No transactions found", subFont));
            empty.setColspan(6);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setPadding(14f);
            txnTable.addCell(empty);
        }

        doc.add(txnTable);

        // Footer
        Paragraph footer = new Paragraph(
                "\nThis is a system-generated statement. For queries contact support@smartbank.in",
                new Font(Font.FontFamily.HELVETICA, 7, Font.ITALIC, new BaseColor(148, 163, 184)));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(12f);
        doc.add(footer);

        doc.close();
        return out.toByteArray();
    }

    private void addInfoCell(PdfPTable table, String text, com.itextpdf.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private void addTxnCell(PdfPTable table, String text, com.itextpdf.text.Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6f);
        cell.setBackgroundColor(bg);
        cell.setBorderColor(new BaseColor(226, 232, 240));
        table.addCell(cell);
    }

    // ─── CSV ─────────────────────────────────────────────────────────────────

    public void writeCsv(Customer customer, Account account,
                         List<Transaction> transactions, PrintWriter writer) throws IOException {
        // Metadata header
        writer.println("SmartBank - Account Statement");
        writer.println("Account Holder," + escapeCsv(customer.getFullName()));
        writer.println("Customer ID," + escapeCsv(customer.getCustomerId()));
        writer.println("Account Number," + escapeCsv(account.getAccountNumber()));
        writer.println("Account Type," + account.getAccountType().name());
        writer.println("Current Balance,\"" + String.format("%,.2f", account.getBalance()) + "\"");
        writer.println("Statement Date," + java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        writer.println();

        // Column headers
        writer.println("Transaction ID,Type,Amount (INR),Balance After (INR),Status,Date & Time,Remarks");

        for (Transaction t : transactions) {
            boolean isCredit = t.getReceiverAccount() != null && t.getReceiverAccount().getAccountNumber().equals(account.getAccountNumber());
            String sign = isCredit ? "+" : "-";
            BigDecimal bal = isCredit ? t.getReceiverBalanceAfter() : t.getSenderBalanceAfter();
            writer.printf("%s,%s,%s%s,%s,%s,%s,%s%n",
                    escapeCsv(t.getTransactionId()),
                    t.getTransactionType().name(),
                    sign,
                    String.format("%.2f", t.getAmount()),
                    bal != null ? String.format("%.2f", bal) : "",
                    t.getStatus().name(),
                    t.getTransactionDate() != null ? t.getTransactionDate().format(DATE_FMT) : "",
                    escapeCsv(t.getRemarks() != null ? t.getRemarks() : "")
            );
        }
        writer.flush();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ─── Excel ───────────────────────────────────────────────────────────────

    public byte[] generateExcel(Customer customer, Account account, List<Transaction> transactions) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Account Statement");

            // Styles
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 10);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Title block
            Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("SmartBank Account Statement");
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 13);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Account details
            int rowIdx = 2;
            createDetailRow(sheet, rowIdx++, "Account Holder:", customer.getFullName());
            createDetailRow(sheet, rowIdx++, "Customer ID:", customer.getCustomerId());
            createDetailRow(sheet, rowIdx++, "Account Number:", account.getAccountNumber());
            createDetailRow(sheet, rowIdx++, "Account Type:", account.getAccountType().name());
            createDetailRow(sheet, rowIdx++, "Current Balance:", "₹" + String.format("%,.2f", account.getBalance()));
            createDetailRow(sheet, rowIdx++, "Statement Date:", java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

            rowIdx++; // spacer

            // Table Headers
            Row tableHeaderRow = sheet.createRow(rowIdx++);
            String[] headers = {"Transaction ID", "Type", "Amount", "Balance After", "Status", "Date & Time", "Remarks"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = tableHeaderRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Table Data
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper creationHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));

            for (Transaction t : transactions) {
                Row row = sheet.createRow(rowIdx++);
                boolean isCredit = t.getReceiverAccount() != null && t.getReceiverAccount().getAccountNumber().equals(account.getAccountNumber());
                BigDecimal bal = isCredit ? t.getReceiverBalanceAfter() : t.getSenderBalanceAfter();
                double amt = t.getAmount().doubleValue();
                String sign = isCredit ? "+" : "-";

                row.createCell(0).setCellValue(t.getTransactionId());
                row.createCell(1).setCellValue(t.getTransactionType().name());
                row.createCell(2).setCellValue(sign + String.format("%.2f", amt));
                row.createCell(3).setCellValue(bal != null ? bal.doubleValue() : 0.0);
                row.createCell(4).setCellValue(t.getStatus().name());
                
                org.apache.poi.ss.usermodel.Cell dateCell = row.createCell(5);
                if (t.getTransactionDate() != null) {
                    dateCell.setCellValue(java.sql.Timestamp.valueOf(t.getTransactionDate()));
                    dateCell.setCellStyle(dateStyle);
                }
                
                row.createCell(6).setCellValue(t.getRemarks() != null ? t.getRemarks() : "");
            }

            // Autofit columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createDetailRow(Sheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }
}
