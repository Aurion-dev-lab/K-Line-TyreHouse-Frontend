package com.gui.kline.sync.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.gui.kline.sync.dto.SyncItemDto;
import com.gui.kline.sync.model.CreditSaleRecord;
import com.gui.kline.sync.model.InvoiceRecord;
import com.gui.kline.sync.model.ProductRecord;
import com.gui.kline.sync.model.QuickServiceRecord;
import com.gui.kline.sync.model.SalaryAdvanceRecord;
import com.gui.kline.sync.model.ServiceRecord;
import com.gui.kline.sync.model.TyreExportRecord;
import com.gui.kline.sync.model.WorkerCreditRecord;
import com.gui.kline.sync.model.WorkerRecord;
import com.gui.kline.sync.repo.CreditSaleRecordRepository;
import com.gui.kline.sync.repo.InvoiceRecordRepository;
import com.gui.kline.sync.repo.ProductRecordRepository;
import com.gui.kline.sync.repo.QuickServiceRecordRepository;
import com.gui.kline.sync.repo.SalaryAdvanceRecordRepository;
import com.gui.kline.sync.repo.ServiceRecordRepository;
import com.gui.kline.sync.repo.TyreExportRecordRepository;
import com.gui.kline.sync.repo.WorkerCreditRecordRepository;
import com.gui.kline.sync.repo.WorkerRecordRepository;

@Service
public class SyncPersistService {
    private final InvoiceRecordRepository invoiceRepo;
    private final CreditSaleRecordRepository creditSaleRepo;
    private final ProductRecordRepository productRepo;
    private final TyreExportRecordRepository tyreExportRepo;
    private final ServiceRecordRepository serviceRepo;
    private final WorkerRecordRepository workerRepo;
    private final SalaryAdvanceRecordRepository salaryAdvanceRepo;
    private final WorkerCreditRecordRepository workerCreditRepo;
    private final QuickServiceRecordRepository quickServiceRepo;

    public SyncPersistService(
            InvoiceRecordRepository invoiceRepo,
            CreditSaleRecordRepository creditSaleRepo,
            ProductRecordRepository productRepo,
            TyreExportRecordRepository tyreExportRepo,
            ServiceRecordRepository serviceRepo,
            WorkerRecordRepository workerRepo,
            SalaryAdvanceRecordRepository salaryAdvanceRepo,
            WorkerCreditRecordRepository workerCreditRepo,
            QuickServiceRecordRepository quickServiceRepo
    ) {
        this.invoiceRepo = invoiceRepo;
        this.creditSaleRepo = creditSaleRepo;
        this.productRepo = productRepo;
        this.tyreExportRepo = tyreExportRepo;
        this.serviceRepo = serviceRepo;
        this.workerRepo = workerRepo;
        this.salaryAdvanceRepo = salaryAdvanceRepo;
        this.workerCreditRepo = workerCreditRepo;
        this.quickServiceRepo = quickServiceRepo;
    }

    public void persist(SyncItemDto item, String deviceId) {
        String type = item.getEntityType();
        JsonNode payload = item.getPayload();
        if (payload == null) {
            return;
        }

        switch (type) {
            case "invoice" -> persistInvoice(item.getId(), deviceId, payload);
            case "credit_sale" -> persistCreditSale(item.getId(), deviceId, payload);
            case "product" -> persistProduct(item.getId(), deviceId, payload);
            case "tyre_export" -> persistTyreExport(item.getId(), deviceId, payload);
            case "service" -> persistService(item.getId(), deviceId, payload);
            case "worker" -> persistWorker(item.getId(), deviceId, payload);
            case "salary_advance" -> persistSalaryAdvance(item.getId(), deviceId, payload);
            case "worker_credit_given" -> persistWorkerCredit(item.getId(), deviceId, payload, "GIVEN");
            case "worker_credit_settle" -> persistWorkerCredit(item.getId(), deviceId, payload, "SETTLE");
            case "worker_credit" -> handleWorkerCreditOp(item.getId(), deviceId, payload);
            case "quick_service" -> persistQuickService(item.getId(), deviceId, payload);
            default -> {
            }
        }
    }

    private void handleWorkerCreditOp(String syncId, String deviceId, JsonNode payload) {
        String op = text(payload, "op");
        String worker = text(payload, "worker");
        java.time.LocalDate date = date(payload, "date");
        double amount = number(payload, "amount");
        String note = text(payload, "note");
        String type = text(payload, "type");
        if (op == null) return;
        if (op.equalsIgnoreCase("delete")) {
            java.util.List<WorkerCreditRecord> found = workerCreditRepo.findByWorkerAndDateAndAmountAndTypeAndNote(worker, date, amount, type, note);
            if (found != null && !found.isEmpty()) {
                workerCreditRepo.deleteAll(found);
            }
        } else if (op.equalsIgnoreCase("update")) {
            java.util.List<WorkerCreditRecord> found = workerCreditRepo.findByWorkerAndDateAndAmountAndNote(worker, date, amount, note);
            if (found != null && !found.isEmpty()) {
                for (WorkerCreditRecord r : found) {
                    r.setWorker(worker);
                    r.setDate(date);
                    r.setAmount(amount);
                    r.setNote(note);
                    r.setType(type);
                    workerCreditRepo.save(r);
                }
            }
        }
    }

    private void persistInvoice(String syncId, String deviceId, JsonNode payload) {
        if (invoiceRepo.existsById(syncId)) return;
        InvoiceRecord record = new InvoiceRecord();
        record.setSyncId(syncId);
        record.setDeviceId(deviceId);
        record.setInvoiceId(text(payload, "invoiceId"));
        record.setDate(date(payload, "date"));
        record.setCustomer(text(payload, "customer"));
        record.setType(text(payload, "type"));
        double total = number(payload, "grandTotal");
        if (total == 0) total = number(payload, "total");
        record.setGrandTotal(total);
        invoiceRepo.save(record);
    }

    private void persistCreditSale(String syncId, String deviceId, JsonNode payload) {
        if (creditSaleRepo.existsById(syncId)) return;
        CreditSaleRecord record = new CreditSaleRecord();
        record.setSyncId(syncId);
        record.setDeviceId(deviceId);
        record.setCreditId(text(payload, "creditId"));
        record.setDate(date(payload, "date"));
        record.setCustomer(text(payload, "customer"));
        record.setDueDate(date(payload, "dueDate"));
        record.setAmount(number(payload, "amount"));
        record.setStatus(text(payload, "status"));
        creditSaleRepo.save(record);
    }

    private void persistProduct(String syncId, String deviceId, JsonNode payload) {
        if (productRepo.existsById(syncId)) return;
        ProductRecord record = new ProductRecord();
        record.setSyncId(syncId);
        record.setDeviceId(deviceId);
        record.setOperation(text(payload, "operation"));
        record.setProductId(text(payload, "productId"));
        record.setProductCode(text(payload, "productCode"));
        record.setName(text(payload, "name"));
        record.setCategory(text(payload, "category"));
        record.setBuyPrice(number(payload, "buyPrice"));
        record.setSellPrice(number(payload, "sellPrice"));
        record.setStock(intNumber(payload, "stock"));
        productRepo.save(record);
    }

    private void persistTyreExport(String syncId, String deviceId, JsonNode payload) {
        String exportId = text(payload, "exportId");
        TyreExportRecord record = exportId == null ? null : tyreExportRepo.findByExportId(exportId);
        if (record == null) {
            record = new TyreExportRecord();
            record.setSyncId(syncId);
            record.setExportId(exportId == null || exportId.isBlank() ? syncId : exportId);
        }
        record.setDeviceId(deviceId);
        record.setOperation(text(payload, "operation"));
        record.setCompany(text(payload, "company"));
        record.setTyres(intNumber(payload, "tyres"));
        record.setCustPrice(number(payload, "custPrice"));
        record.setCompPrice(number(payload, "compPrice"));
        record.setServiceFee(number(payload, "serviceFee"));
        record.setPaidAmount(number(payload, "paidAmount"));
        record.setTotalAmount(number(payload, "totalAmount"));
        record.setBalanceAmount(number(payload, "balanceAmount"));
        record.setPaymentStatus(text(payload, "paymentStatus"));
        record.setDate(date(payload, "date"));
        record.setStatus(text(payload, "status"));
        tyreExportRepo.save(record);
    }

    private void persistService(String syncId, String deviceId, JsonNode payload) {
        if (serviceRepo.existsById(syncId)) return;
        ServiceRecord record = new ServiceRecord();
        record.setSyncId(syncId);
        record.setDeviceId(deviceId);
        record.setService(text(payload, "service"));
        record.setRemark(text(payload, "remark"));
        record.setPrice(number(payload, "price"));
        record.setDate(date(payload, "date"));
        serviceRepo.save(record);
    }

    private void persistWorker(String syncId, String deviceId, JsonNode payload) {
        if (workerRepo.existsById(syncId)) return;
        WorkerRecord record = new WorkerRecord();
        record.setSyncId(syncId);
        record.setDeviceId(deviceId);
        record.setName(text(payload, "name"));
        record.setPhone(text(payload, "phone"));
        record.setRole(text(payload, "role"));
        record.setRate(text(payload, "rate"));
        workerRepo.save(record);
    }

    private void persistSalaryAdvance(String syncId, String deviceId, JsonNode payload) {
        if (salaryAdvanceRepo.existsById(syncId)) return;
        SalaryAdvanceRecord record = new SalaryAdvanceRecord();
        record.setSyncId(syncId);
        record.setDeviceId(deviceId);
        record.setWorker(text(payload, "worker"));
        record.setDate(date(payload, "date"));
        record.setAmount(number(payload, "amount"));
        record.setNote(text(payload, "note"));
        salaryAdvanceRepo.save(record);
    }

    private void persistWorkerCredit(String syncId, String deviceId, JsonNode payload, String type) {
        if (workerCreditRepo.existsById(syncId)) return;
        WorkerCreditRecord record = new WorkerCreditRecord();
        record.setSyncId(syncId);
        record.setDeviceId(deviceId);
        record.setWorker(text(payload, "worker"));
        record.setDate(date(payload, "date"));
        record.setAmount(number(payload, "amount"));
        record.setNote(text(payload, "note"));
        record.setType(type);
        workerCreditRepo.save(record);
    }

    private void persistQuickService(String syncId, String deviceId, JsonNode payload) {
        if (quickServiceRepo.existsById(syncId)) return;
        QuickServiceRecord record = new QuickServiceRecord();
        record.setSyncId(syncId);
        record.setDeviceId(deviceId);
        record.setService(text(payload, "service"));
        record.setPrice(number(payload, "price"));
        record.setDate(date(payload, "date"));
        quickServiceRepo.save(record);
    }

    private String text(JsonNode payload, String key) {
        JsonNode node = payload.get(key);
        return node == null || node.isNull() ? null : node.asText();
    }

    private double number(JsonNode payload, String key) {
        JsonNode node = payload.get(key);
        if (node == null || node.isNull()) return 0.0;
        if (node.isNumber()) return node.asDouble();
        try {
            return Double.parseDouble(node.asText());
        } catch (Exception ex) {
            return 0.0;
        }
    }

    private int intNumber(JsonNode payload, String key) {
        JsonNode node = payload.get(key);
        if (node == null || node.isNull()) return 0;
        if (node.isInt() || node.isLong()) return node.asInt();
        try {
            return Integer.parseInt(node.asText());
        } catch (Exception ex) {
            return 0;
        }
    }

    private LocalDate date(JsonNode payload, String key) {
        String value = text(payload, key);
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }
}

