package com.gui.kline.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gui.kline.server.dto.*;
import com.gui.kline.server.entity.*;
import com.gui.kline.server.exception.ResourceNotFoundException;
import com.gui.kline.server.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for handling data synchronization between client devices and server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {
    
    private final DeviceRepository deviceRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final WorkerRepository workerRepository;
    private final CreditSaleRepository creditSaleRepository;
    private final CreditSaleItemRepository creditSaleItemRepository;
    private final CreditSalePaymentRepository creditSalePaymentRepository;
    private final TyreExportRepository tyreExportRepository;
    private final TyreExportPaymentRepository tyreExportPaymentRepository;
    private final WorkerAttendanceRepository workerAttendanceRepository;
    private final SalaryAdvanceRepository salaryAdvanceRepository;
    private final WorkerCreditRepository workerCreditRepository;
    private final QuickServiceRepository quickServiceRepository;
    private final SyncLogRepository syncLogRepository;
    
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final UUIDGeneratorService uuidGeneratorService;
    
    /**
     * Upload data from a device to the server
     */
    @Transactional
    public SyncResponse uploadData(SyncRequest syncRequest) {
        log.info("Processing sync upload from device: {}", syncRequest.getDeviceId());
        
        SyncResponse response = new SyncResponse();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        try {
            // Validate device
            Device device = deviceRepository.findByDeviceId(syncRequest.getDeviceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Device not found", "deviceId", syncRequest.getDeviceId()));
            
            if (!device.isActive() || !device.isSyncEnabled()) {
                throw new IllegalStateException("Device is not active or sync is disabled");
            }
            
            // Log sync start
            SyncLog syncLog = logSyncStart(syncRequest, device);
            
            // Process each entity type
            successCount += processProducts(syncRequest.getProducts(), device, errors);
            successCount += processCustomers(syncRequest.getCustomers(), device, errors);
            successCount += processInvoices(syncRequest.getInvoices(), device, errors);
            successCount += processInvoiceLineItems(syncRequest.getInvoiceLineItems(), device, errors);
            successCount += processServiceRecords(syncRequest.getServiceRecords(), device, errors);
            successCount += processWorkers(syncRequest.getWorkers(), device, errors);
            successCount += processCreditSales(syncRequest.getCreditSales(), device, errors);
            successCount += processCreditSaleItems(syncRequest.getCreditSaleItems(), device, errors);
            successCount += processCreditSalePayments(syncRequest.getCreditSalePayments(), device, errors);
            successCount += processTyreExports(syncRequest.getTyreExports(), device, errors);
            successCount += processTyreExportPayments(syncRequest.getTyreExportPayments(), device, errors);
            successCount += processWorkerAttendances(syncRequest.getWorkerAttendances(), device, errors);
            successCount += processSalaryAdvances(syncRequest.getSalaryAdvances(), device, errors);
            successCount += processWorkerCredits(syncRequest.getWorkerCredits(), device, errors);
            successCount += processQuickServices(syncRequest.getQuickServices(), device, errors);
            
            failureCount = errors.size();
            
            // Update sync log
            updateSyncLog(syncLog, successCount, failureCount, device, errors);
            
            // Update device sync stats
            device.setLastSyncAt(LocalDateTime.now());
            device.setTotalSyncs(device.getTotalSyncs() + 1);
            device.setSuccessfulSyncs(device.getSuccessfulSyncs() + (failureCount == 0 ? 1 : 0));
            device.setFailedSyncs(device.getFailedSyncs() + (failureCount > 0 ? 1 : 0));
            device.setDailySyncCount(device.getDailySyncCount() + 1);
            deviceRepository.save(device);
            
            response.setSuccess(true);
            response.setMessage("Sync upload completed successfully");
            response.setTotalItems(successCount + failureCount);
            response.setSuccessCount(successCount);
            response.setFailureCount(failureCount);
            response.setErrors(errors);
            response.setSyncLogId(syncLog.getId());
            
        } catch (Exception e) {
            log.error("Error during sync upload: {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setMessage("Sync upload failed: " + e.getMessage());
            response.setErrors(Collections.singletonList(e.getMessage()));
        }
        
        return response;
    }
    
    /**
     * Download data for a specific device
     */
    @Transactional
    public SyncResponse downloadData(String deviceId, LocalDateTime lastSyncTime) {
        log.info("Processing sync download for device: {}", deviceId);
        
        SyncResponse response = new SyncResponse();
        
        try {
            // Validate device
            Device device = deviceRepository.findByDeviceId(deviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Device not found", "deviceId", deviceId));
            
            if (!device.isActive() || !device.isSyncEnabled()) {
                throw new IllegalStateException("Device is not active or sync is disabled");
            }
            
            // Get all data that has been modified since last sync
            List<ProductDTO> products = getUpdatedProducts(deviceId, lastSyncTime);
            List<CustomerDTO> customers = getUpdatedCustomers(deviceId, lastSyncTime);
            List<InvoiceDTO> invoices = getUpdatedInvoices(deviceId, lastSyncTime);
            List<InvoiceLineItemDTO> invoiceLineItems = getUpdatedInvoiceLineItems(deviceId, lastSyncTime);
            List<ServiceRecordDTO> serviceRecords = getUpdatedServiceRecords(deviceId, lastSyncTime);
            List<WorkerDTO> workers = getUpdatedWorkers(deviceId, lastSyncTime);
            List<CreditSaleDTO> creditSales = getUpdatedCreditSales(deviceId, lastSyncTime);
            List<CreditSaleItemDTO> creditSaleItems = getUpdatedCreditSaleItems(deviceId, lastSyncTime);
            List<CreditSalePaymentDTO> creditSalePayments = getUpdatedCreditSalePayments(deviceId, lastSyncTime);
            List<TyreExportDTO> tyreExports = getUpdatedTyreExports(deviceId, lastSyncTime);
            List<TyreExportPaymentDTO> tyreExportPayments = getUpdatedTyreExportPayments(deviceId, lastSyncTime);
            List<WorkerAttendanceDTO> workerAttendances = getUpdatedWorkerAttendances(deviceId, lastSyncTime);
            List<SalaryAdvanceDTO> salaryAdvances = getUpdatedSalaryAdvances(deviceId, lastSyncTime);
            List<WorkerCreditDTO> workerCredits = getUpdatedWorkerCredits(deviceId, lastSyncTime);
            List<QuickServiceDTO> quickServices = getUpdatedQuickServices(deviceId, lastSyncTime);
            
            // Create response
            SyncResponse dataResponse = new SyncResponse();
            dataResponse.setSuccess(true);
            dataResponse.setMessage("Data downloaded successfully");
            dataResponse.setProducts(products);
            dataResponse.setCustomers(customers);
            dataResponse.setInvoices(invoices);
            dataResponse.setInvoiceLineItems(invoiceLineItems);
            dataResponse.setServiceRecords(serviceRecords);
            dataResponse.setWorkers(workers);
            dataResponse.setCreditSales(creditSales);
            dataResponse.setCreditSaleItems(creditSaleItems);
            dataResponse.setCreditSalePayments(creditSalePayments);
            dataResponse.setTyreExports(tyreExports);
            dataResponse.setTyreExportPayments(tyreExportPayments);
            dataResponse.setWorkerAttendances(workerAttendances);
            dataResponse.setSalaryAdvances(salaryAdvances);
            dataResponse.setWorkerCredits(workerCredits);
            dataResponse.setQuickServices(quickServices);
            
            // Log sync
            SyncLog syncLog = new SyncLog();
            syncLog.setId(uuidGeneratorService.generateId());
            syncLog.setDeviceId(device.getId());
            syncLog.setDeviceName(device.getName());
            syncLog.setOperation(SyncLog.Operation.DOWNLOAD.name());
            syncLog.setSyncType(SyncLog.SyncType.INCREMENTAL.name());
            syncLog.setStatus(SyncLog.Status.COMPLETED.name());
            syncLog.setStartTime(LocalDateTime.now());
            syncLog.setEndTime(LocalDateTime.now());
            syncLog.setItemsToSync(0);
            syncLog.setItemsSynced(products.size() + customers.size() + invoices.size() + 
                    invoiceLineItems.size() + serviceRecords.size() + workers.size() +
                    creditSales.size() + creditSaleItems.size() + creditSalePayments.size() +
                    tyreExports.size() + tyreExportPayments.size() + workerAttendances.size() +
                    salaryAdvances.size() + workerCredits.size() + quickServices.size());
            syncLog.setItemsFailed(0);
            syncLogRepository.save(syncLog);
            
            dataResponse.setSyncLogId(syncLog.getId());
            
            return dataResponse;
            
        } catch (Exception e) {
            log.error("Error during sync download: {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setMessage("Sync download failed: " + e.getMessage());
            return response;
        }
    }
    
    /**
     * Get full sync data (for initial sync)
     */
    @Transactional
    public SyncResponse getFullSyncData(String deviceId) {
        log.info("Processing full sync for device: {}", deviceId);
        
        try {
            Device device = deviceRepository.findByDeviceId(deviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Device not found", "deviceId", deviceId));
            
            // Get all data for this device
            List<ProductDTO> products = productRepository.findByDeviceId(deviceId).stream()
                    .map(p -> modelMapper.map(p, ProductDTO.class))
                    .collect(Collectors.toList());
            
            List<CustomerDTO> customers = customerRepository.findByDeviceId(deviceId).stream()
                    .map(c -> modelMapper.map(c, CustomerDTO.class))
                    .collect(Collectors.toList());
            
            List<InvoiceDTO> invoices = invoiceRepository.findByDeviceId(deviceId).stream()
                    .map(i -> modelMapper.map(i, InvoiceDTO.class))
                    .collect(Collectors.toList());
            
            List<InvoiceLineItemDTO> invoiceLineItems = invoiceLineItemRepository.findByDeviceId(deviceId).stream()
                    .map(ili -> modelMapper.map(ili, InvoiceLineItemDTO.class))
                    .collect(Collectors.toList());
            
            List<ServiceRecordDTO> serviceRecords = serviceRecordRepository.findByDeviceId(deviceId).stream()
                    .map(s -> modelMapper.map(s, ServiceRecordDTO.class))
                    .collect(Collectors.toList());
            
            List<WorkerDTO> workers = workerRepository.findByDeviceId(deviceId).stream()
                    .map(w -> modelMapper.map(w, WorkerDTO.class))
                    .collect(Collectors.toList());
            
            List<CreditSaleDTO> creditSales = creditSaleRepository.findByDeviceId(deviceId).stream()
                    .map(cs -> modelMapper.map(cs, CreditSaleDTO.class))
                    .collect(Collectors.toList());
            
            List<CreditSaleItemDTO> creditSaleItems = creditSaleItemRepository.findByDeviceId(deviceId).stream()
                    .map(csi -> modelMapper.map(csi, CreditSaleItemDTO.class))
                    .collect(Collectors.toList());
            
            List<CreditSalePaymentDTO> creditSalePayments = creditSalePaymentRepository.findByDeviceId(deviceId).stream()
                    .map(csp -> modelMapper.map(csp, CreditSalePaymentDTO.class))
                    .collect(Collectors.toList());
            
            List<TyreExportDTO> tyreExports = tyreExportRepository.findByDeviceId(deviceId).stream()
                    .map(te -> modelMapper.map(te, TyreExportDTO.class))
                    .collect(Collectors.toList());
            
            List<TyreExportPaymentDTO> tyreExportPayments = tyreExportPaymentRepository.findByDeviceId(deviceId).stream()
                    .map(tep -> modelMapper.map(tep, TyreExportPaymentDTO.class))
                    .collect(Collectors.toList());
            
            List<WorkerAttendanceDTO> workerAttendances = workerAttendanceRepository.findByDeviceId(deviceId).stream()
                    .map(wa -> modelMapper.map(wa, WorkerAttendanceDTO.class))
                    .collect(Collectors.toList());
            
            List<SalaryAdvanceDTO> salaryAdvances = salaryAdvanceRepository.findByDeviceId(deviceId).stream()
                    .map(sa -> modelMapper.map(sa, SalaryAdvanceDTO.class))
                    .collect(Collectors.toList());
            
            List<WorkerCreditDTO> workerCredits = workerCreditRepository.findByDeviceId(deviceId).stream()
                    .map(wc -> modelMapper.map(wc, WorkerCreditDTO.class))
                    .collect(Collectors.toList());
            
            List<QuickServiceDTO> quickServices = quickServiceRepository.findByDeviceId(deviceId).stream()
                    .map(qs -> modelMapper.map(qs, QuickServiceDTO.class))
                    .collect(Collectors.toList());
            
            SyncResponse response = new SyncResponse();
            response.setSuccess(true);
            response.setMessage("Full sync data retrieved");
            response.setProducts(products);
            response.setCustomers(customers);
            response.setInvoices(invoices);
            response.setInvoiceLineItems(invoiceLineItems);
            response.setServiceRecords(serviceRecords);
            response.setWorkers(workers);
            response.setCreditSales(creditSales);
            response.setCreditSaleItems(creditSaleItems);
            response.setCreditSalePayments(creditSalePayments);
            response.setTyreExports(tyreExports);
            response.setTyreExportPayments(tyreExportPayments);
            response.setWorkerAttendances(workerAttendances);
            response.setSalaryAdvances(salaryAdvances);
            response.setWorkerCredits(workerCredits);
            response.setQuickServices(quickServices);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error during full sync: {}", e.getMessage(), e);
            SyncResponse response = new SyncResponse();
            response.setSuccess(false);
            response.setMessage("Full sync failed: " + e.getMessage());
            return response;
        }
    }
    
    // Private helper methods for processing each entity type
    private int processProducts(List<ProductDTO> productDTOs, Device device, List<String> errors) {
        if (productDTOs == null || productDTOs.isEmpty()) return 0;
        
        int count = 0;
        for (ProductDTO dto : productDTOs) {
            try {
                Product product = modelMapper.map(dto, Product.class);
                product.setDeviceId(device.getId());
                product.setSyncedAt(LocalDateTime.now());
                product.setSyncStatus(true);
                product.setUpdatedAt(LocalDateTime.now());
                
                productRepository.save(product);
                count++;
            } catch (Exception e) {
                errors.add("Failed to process product " + dto.getId() + ": " + e.getMessage());
                log.error("Error processing product: {}", e.getMessage(), e);
            }
        }
        return count;
    }
    
    private int processCustomers(List<CustomerDTO> customerDTOs, Device device, List<String> errors) {
        if (customerDTOs == null || customerDTOs.isEmpty()) return 0;
        
        int count = 0;
        for (CustomerDTO dto : customerDTOs) {
            try {
                Customer customer = modelMapper.map(dto, Customer.class);
                customer.setDeviceId(device.getId());
                customer.setSyncedAt(LocalDateTime.now());
                customer.setSyncStatus(true);
                customer.setUpdatedAt(LocalDateTime.now());
                
                customerRepository.save(customer);
                count++;
            } catch (Exception e) {
                errors.add("Failed to process customer " + dto.getId() + ": " + e.getMessage());
                log.error("Error processing customer: {}", e.getMessage(), e);
            }
        }
        return count;
    }
    
    // Similar methods for other entity types would be implemented here
    // For brevity, I'll implement a few key ones
    
    private int processInvoices(List<InvoiceDTO> invoiceDTOs, Device device, List<String> errors) {
        if (invoiceDTOs == null || invoiceDTOs.isEmpty()) return 0;
        
        int count = 0;
        for (InvoiceDTO dto : invoiceDTOs) {
            try {
                Invoice invoice = modelMapper.map(dto, Invoice.class);
                invoice.setDeviceId(device.getId());
                invoice.setSyncedAt(LocalDateTime.now());
                invoice.setSyncStatus(true);
                invoice.setUpdatedAt(LocalDateTime.now());
                
                invoiceRepository.save(invoice);
                count++;
            } catch (Exception e) {
                errors.add("Failed to process invoice " + dto.getId() + ": " + e.getMessage());
                log.error("Error processing invoice: {}", e.getMessage(), e);
            }
        }
        return count;
    }
    
    private int processInvoiceLineItems(List<InvoiceLineItemDTO> lineItemDTOs, Device device, List<String> errors) {
        if (lineItemDTOs == null || lineItemDTOs.isEmpty()) return 0;
        
        int count = 0;
        for (InvoiceLineItemDTO dto : lineItemDTOs) {
            try {
                InvoiceLineItem item = modelMapper.map(dto, InvoiceLineItem.class);
                item.setDeviceId(device.getId());
                item.setSyncedAt(LocalDateTime.now());
                item.setSyncStatus(true);
                item.setUpdatedAt(LocalDateTime.now());
                
                invoiceLineItemRepository.save(item);
                count++;
            } catch (Exception e) {
                errors.add("Failed to process invoice line item " + dto.getId() + ": " + e.getMessage());
                log.error("Error processing invoice line item: {}", e.getMessage(), e);
            }
        }
        return count;
    }
    
    private int processServiceRecords(List<ServiceRecordDTO> serviceRecordDTOs, Device device, List<String> errors) {
        if (serviceRecordDTOs == null || serviceRecordDTOs.isEmpty()) return 0;
        
        int count = 0;
        for (ServiceRecordDTO dto : serviceRecordDTOs) {
            try {
                ServiceRecord service = modelMapper.map(dto, ServiceRecord.class);
                service.setDeviceId(device.getId());
                service.setSyncedAt(LocalDateTime.now());
                service.setSyncStatus(true);
                service.setUpdatedAt(LocalDateTime.now());
                
                serviceRecordRepository.save(service);
                count++;
            } catch (Exception e) {
                errors.add("Failed to process service record " + dto.getId() + ": " + e.getMessage());
                log.error("Error processing service record: {}", e.getMessage(), e);
            }
        }
        return count;
    }
    
    // Additional processing methods for other entity types
    private int processWorkers(List<WorkerDTO> workerDTOs, Device device, List<String> errors) {
        if (workerDTOs == null || workerDTOs.isEmpty()) return 0;
        
        int count = 0;
        for (WorkerDTO dto : workerDTOs) {
            try {
                Worker worker = modelMapper.map(dto, Worker.class);
                worker.setDeviceId(device.getId());
                worker.setSyncedAt(LocalDateTime.now());
                worker.setSyncStatus(true);
                worker.setUpdatedAt(LocalDateTime.now());
                
                workerRepository.save(worker);
                count++;
            } catch (Exception e) {
                errors.add("Failed to process worker " + dto.getId() + ": " + e.getMessage());
                log.error("Error processing worker: {}", e.getMessage(), e);
            }
        }
        return count;
    }
    
    private int processCreditSales(List<CreditSaleDTO> creditSaleDTOs, Device device, List<String> errors) {
        if (creditSaleDTOs == null || creditSaleDTOs.isEmpty()) return 0;
        
        int count = 0;
        for (CreditSaleDTO dto : creditSaleDTOs) {
            try {
                CreditSale creditSale = modelMapper.map(dto, CreditSale.class);
                creditSale.setDeviceId(device.getId());
                creditSale.setSyncedAt(LocalDateTime.now());
                creditSale.setSyncStatus(true);
                creditSale.setUpdatedAt(LocalDateTime.now());
                
                creditSaleRepository.save(creditSale);
                count++;
            } catch (Exception e) {
                errors.add("Failed to process credit sale " + dto.getId() + ": " + e.getMessage());
                log.error("Error processing credit sale: {}", e.getMessage(), e);
            }
        }
        return count;
    }
    
    private int processCreditSaleItems(List<CreditSaleItemDTO> itemDTOs, Device device, List<String> errors) {
        if (itemDTOs == null || itemDTOs.isEmpty()) return 0;
        
        int count = 0;
        for (CreditSaleItemDTO dto : itemDTOs) {
            try {
                CreditSaleItem item = modelMapper.map(dto, CreditSaleItem.class);
                item.setDeviceId(device.getId());
                item.setSyncedAt(LocalDateTime.now());
                item.setSyncStatus(true);
                item.setUpdatedAt(LocalDateTime.now());
                
                creditSaleItemRepository.save(item);
                count++;
            } catch (Exception e) {
                errors.add("Failed to process credit sale item " + dto.getId() + ": " + e.getMessage());
                log.error("Error processing credit sale item: {}", e.getMessage(), e);
            }
        }
        return count;
    }
    
    private int processCreditSalePayments(List<CreditSalePaymentDTO> paymentDTOs, Device device, List<String> errors) {
        if (paymentDTOs == null || paymentDTOs.isEmpty()) return 0;
        
        int count = 0;
        for (CreditSalePaymentDTO dto : paymentDTOs) {
            try {
                CreditSalePayment payment = modelMapper.map(dto, CreditSalePayment.class);
                payment.setDeviceId(device.getId());
                payment.setSyncedAt(LocalDateTime.now());
                payment.setSyncStatus(true);
                payment.setUpdatedAt(LocalDateTime.now());
                
                creditSalePaymentRepository.save(payment);
                count++;
            } catch (Exception e) {
                errors.add("Failed to process credit sale payment " + dto.getId() + ": " + e.getMessage());
                log.error("Error processing credit sale payment: {}", e.getMessage(), e);
            }
        }
        return count;
    }
    
    private int processTyreExports(List<TyreExportDTO> exportDTOs, Device device, List<String> errors) {
        if (exportDTOs == null || exportDTOs.isEmpty()) return 0;
        
        int count = 0;
        for (TyreExportDTO dto : exportDTOs) {
            try {
                TyreExport tyreExport = modelMapper.map(dto, TyreExport.class);
                tyreExport.setDeviceId(device.getId());
                tyreExport.setSyncedAt(LocalDateTime.now());
                tyreExport.setSyncStatus(true);
                tyreExport.setUpdatedAt(LocalDateTime.now());
                
                tyreExportRepository.save(tyreExport);
                count++;
            } catch (Exception e) {
                errors.add("Failed to process tyre export " + dto.getId() + ": " + e.getMessage());
                log.error("Error processing tyre export: {}", e.getMessage(), e);
            }
        }
        return count;
    }
    
    private int processTyreExportPayments(List<TyreExportPaymentDTO> paymentDTOs, Device device, List<String> errors) {
        if (paymentDTOs == null || paymentDTOs.isEmpty()) return 0;
        
        int count = 0;
        for (TyreExportPaymentDTO dto : paymentDTOs) {
            try {
                TyreExportPayment payment = modelMapper.map(dto, TyreExportPayment.class);
                payment.setDeviceId(device.getId());
                payment.setSyncedAt(LocalDateTime.now());
                payment.setSyncStatus(true);
                payment.setUpdatedAt(LocalDateTime.now());
                
                tyreExportPaymentRepository.save(payment);
                count++;
            } catch (Exception e) {
                errors.add("Failed to process tyre export payment " + dto.getId() + ": " + e.getMessage());
                log.error("Error processing tyre export payment: {}", e.getMessage(), e);
            }
        }
        return count;
    }
    
    private int processWorkerAttendances(List<WorkerAttendanceDTO> attendanceDTOs, Device device, List<String> errors) {
        if (attendanceDTOs == null || attendanceDTOs.isEmpty()) return 0;
        
        int count = 0;
        for (WorkerAttendanceDTO dto : attendanceDTOs) {
            try {
                WorkerAttendance attendance = modelMapper.map(dto, WorkerAttendance.class);
                attendance.setDeviceId(device.getId());
                attendance.setSyncedAt(LocalDateTime.now());
                attendance.setSyncStatus(true);
                attendance.setUpdatedAt(LocalDateTime.now());
                
                workerAttendanceRepository.save(attendance);
                count++;
            } catch (Exception e) {
                errors.add("Failed to process worker attendance " + dto.getId() + ": " + e.getMessage());
                log.error("Error processing worker attendance: {}", e.getMessage(), e);
            }
        }
        return count;
    }
    
    private int processSalaryAdvances(List<SalaryAdvanceDTO> advanceDTOs, Device device, List<String> errors) {
        if (advanceDTOs == null || advanceDTOs.isEmpty()) return 0;
        
        int count = 0;
        for (SalaryAdvanceDTO dto : advanceDTOs) {
            try {
                SalaryAdvance advance = modelMapper.map(dto, SalaryAdvance.class);
                advance.setDeviceId(device.getId());
                advance.setSyncedAt(LocalDateTime.now());
                advance.setSyncStatus(true);
                advance.setUpdatedAt(LocalDateTime.now());
                
                salaryAdvanceRepository.save(advance);
                count++;
            } catch (Exception e) {
                errors.add("Failed to process salary advance " + dto.getId() + ": " + e.getMessage());
                log.error("Error processing salary advance: {}", e.getMessage(), e);
            }
        }
        return count;
    }
    
    private int processWorkerCredits(List<WorkerCreditDTO> creditDTOs, Device device, List<String> errors) {
        if (creditDTOs == null || creditDTOs.isEmpty()) return 0;
        
        int count = 0;
        for (WorkerCreditDTO dto : creditDTOs) {
            try {
                WorkerCredit credit = modelMapper.map(dto, WorkerCredit.class);
                credit.setDeviceId(device.getId());
                credit.setSyncedAt(LocalDateTime.now());
                credit.setSyncStatus(true);
                credit.setUpdatedAt(LocalDateTime.now());
                
                workerCreditRepository.save(credit);
                count++;
            } catch (Exception e) {
                errors.add("Failed to process worker credit " + dto.getId() + ": " + e.getMessage());
                log.error("Error processing worker credit: {}", e.getMessage(), e);
            }
        }
        return count;
    }
    
    private int processQuickServices(List<QuickServiceDTO> serviceDTOs, Device device, List<String> errors) {
        if (serviceDTOs == null || serviceDTOs.isEmpty()) return 0;
        
        int count = 0;
        for (QuickServiceDTO dto : serviceDTOs) {
            try {
                QuickService service = modelMapper.map(dto, QuickService.class);
                service.setDeviceId(device.getId());
                service.setSyncedAt(LocalDateTime.now());
                service.setSyncStatus(true);
                service.setUpdatedAt(LocalDateTime.now());
                
                quickServiceRepository.save(service);
                count++;
            } catch (Exception e) {
                errors.add("Failed to process quick service " + dto.getId() + ": " + e.getMessage());
                log.error("Error processing quick service: {}", e.getMessage(), e);
            }
        }
        return count;
    }
    
    // Private helper methods for getting updated data
    private List<ProductDTO> getUpdatedProducts(String deviceId, LocalDateTime lastSyncTime) {
        return productRepository.findByDeviceId(deviceId).stream()
                .filter(p -> p.isSyncStatus() && (lastSyncTime == null || p.getSyncedAt() != null && p.getSyncedAt().isAfter(lastSyncTime)))
                .map(p -> modelMapper.map(p, ProductDTO.class))
                .collect(Collectors.toList());
    }
    
    private List<CustomerDTO> getUpdatedCustomers(String deviceId, LocalDateTime lastSyncTime) {
        return customerRepository.findByDeviceId(deviceId).stream()
                .filter(c -> c.isSyncStatus() && (lastSyncTime == null || c.getSyncedAt() != null && c.getSyncedAt().isAfter(lastSyncTime)))
                .map(c -> modelMapper.map(c, CustomerDTO.class))
                .collect(Collectors.toList());
    }
    
    private List<InvoiceDTO> getUpdatedInvoices(String deviceId, LocalDateTime lastSyncTime) {
        return invoiceRepository.findByDeviceId(deviceId).stream()
                .filter(i -> i.isSyncStatus() && (lastSyncTime == null || i.getSyncedAt() != null && i.getSyncedAt().isAfter(lastSyncTime)))
                .map(i -> modelMapper.map(i, InvoiceDTO.class))
                .collect(Collectors.toList());
    }
    
    private List<InvoiceLineItemDTO> getUpdatedInvoiceLineItems(String deviceId, LocalDateTime lastSyncTime) {
        return invoiceLineItemRepository.findByDeviceId(deviceId).stream()
                .filter(ili -> ili.isSyncStatus() && (lastSyncTime == null || ili.getSyncedAt() != null && ili.getSyncedAt().isAfter(lastSyncTime)))
                .map(ili -> modelMapper.map(ili, InvoiceLineItemDTO.class))
                .collect(Collectors.toList());
    }
    
    private List<ServiceRecordDTO> getUpdatedServiceRecords(String deviceId, LocalDateTime lastSyncTime) {
        return serviceRecordRepository.findByDeviceId(deviceId).stream()
                .filter(s -> s.isSyncStatus() && (lastSyncTime == null || s.getSyncedAt() != null && s.getSyncedAt().isAfter(lastSyncTime)))
                .map(s -> modelMapper.map(s, ServiceRecordDTO.class))
                .collect(Collectors.toList());
    }
    
    private List<WorkerDTO> getUpdatedWorkers(String deviceId, LocalDateTime lastSyncTime) {
        return workerRepository.findByDeviceId(deviceId).stream()
                .filter(w -> w.isSyncStatus() && (lastSyncTime == null || w.getSyncedAt() != null && w.getSyncedAt().isAfter(lastSyncTime)))
                .map(w -> modelMapper.map(w, WorkerDTO.class))
                .collect(Collectors.toList());
    }
    
    private List<CreditSaleDTO> getUpdatedCreditSales(String deviceId, LocalDateTime lastSyncTime) {
        return creditSaleRepository.findByDeviceId(deviceId).stream()
                .filter(cs -> cs.isSyncStatus() && (lastSyncTime == null || cs.getSyncedAt() != null && cs.getSyncedAt().isAfter(lastSyncTime)))
                .map(cs -> modelMapper.map(cs, CreditSaleDTO.class))
                .collect(Collectors.toList());
    }
    
    private List<CreditSaleItemDTO> getUpdatedCreditSaleItems(String deviceId, LocalDateTime lastSyncTime) {
        return creditSaleItemRepository.findByDeviceId(deviceId).stream()
                .filter(csi -> csi.isSyncStatus() && (lastSyncTime == null || csi.getSyncedAt() != null && csi.getSyncedAt().isAfter(lastSyncTime)))
                .map(csi -> modelMapper.map(csi, CreditSaleItemDTO.class))
                .collect(Collectors.toList());
    }
    
    private List<CreditSalePaymentDTO> getUpdatedCreditSalePayments(String deviceId, LocalDateTime lastSyncTime) {
        return creditSalePaymentRepository.findByDeviceId(deviceId).stream()
                .filter(csp -> csp.isSyncStatus() && (lastSyncTime == null || csp.getSyncedAt() != null && csp.getSyncedAt().isAfter(lastSyncTime)))
                .map(csp -> modelMapper.map(csp, CreditSalePaymentDTO.class))
                .collect(Collectors.toList());
    }
    
    private List<TyreExportDTO> getUpdatedTyreExports(String deviceId, LocalDateTime lastSyncTime) {
        return tyreExportRepository.findByDeviceId(deviceId).stream()
                .filter(te -> te.isSyncStatus() && (lastSyncTime == null || te.getSyncedAt() != null && te.getSyncedAt().isAfter(lastSyncTime)))
                .map(te -> modelMapper.map(te, TyreExportDTO.class))
                .collect(Collectors.toList());
    }
    
    private List<TyreExportPaymentDTO> getUpdatedTyreExportPayments(String deviceId, LocalDateTime lastSyncTime) {
        return tyreExportPaymentRepository.findByDeviceId(deviceId).stream()
                .filter(tep -> tep.isSyncStatus() && (lastSyncTime == null || tep.getSyncedAt() != null && tep.getSyncedAt().isAfter(lastSyncTime)))
                .map(tep -> modelMapper.map(tep, TyreExportPaymentDTO.class))
                .collect(Collectors.toList());
    }
    
    private List<WorkerAttendanceDTO> getUpdatedWorkerAttendances(String deviceId, LocalDateTime lastSyncTime) {
        return workerAttendanceRepository.findByDeviceId(deviceId).stream()
                .filter(wa -> wa.isSyncStatus() && (lastSyncTime == null || wa.getSyncedAt() != null && wa.getSyncedAt().isAfter(lastSyncTime)))
                .map(wa -> modelMapper.map(wa, WorkerAttendanceDTO.class))
                .collect(Collectors.toList());
    }
    
    private List<SalaryAdvanceDTO> getUpdatedSalaryAdvances(String deviceId, LocalDateTime lastSyncTime) {
        return salaryAdvanceRepository.findByDeviceId(deviceId).stream()
                .filter(sa -> sa.isSyncStatus() && (lastSyncTime == null || sa.getSyncedAt() != null && sa.getSyncedAt().isAfter(lastSyncTime)))
                .map(sa -> modelMapper.map(sa, SalaryAdvanceDTO.class))
                .collect(Collectors.toList());
    }
    
    private List<WorkerCreditDTO> getUpdatedWorkerCredits(String deviceId, LocalDateTime lastSyncTime) {
        return workerCreditRepository.findByDeviceId(deviceId).stream()
                .filter(wc -> wc.isSyncStatus() && (lastSyncTime == null || wc.getSyncedAt() != null && wc.getSyncedAt().isAfter(lastSyncTime)))
                .map(wc -> modelMapper.map(wc, WorkerCreditDTO.class))
                .collect(Collectors.toList());
    }
    
    private List<QuickServiceDTO> getUpdatedQuickServices(String deviceId, LocalDateTime lastSyncTime) {
        return quickServiceRepository.findByDeviceId(deviceId).stream()
                .filter(qs -> qs.isSyncStatus() && (lastSyncTime == null || qs.getSyncedAt() != null && qs.getSyncedAt().isAfter(lastSyncTime)))
                .map(qs -> modelMapper.map(qs, QuickServiceDTO.class))
                .collect(Collectors.toList());
    }
    
    // Helper methods for sync logging
    private SyncLog logSyncStart(SyncRequest syncRequest, Device device) {
        SyncLog syncLog = new SyncLog();
        syncLog.setId(uuidGeneratorService.generateId());
        syncLog.setDeviceId(device.getId());
        syncLog.setDeviceName(device.getName());
        syncLog.setOperation(syncRequest.getOperation() != null ? syncRequest.getOperation() : SyncLog.Operation.UPLOAD.name());
        syncLog.setSyncType(syncRequest.getSyncType() != null ? syncRequest.getSyncType() : SyncLog.SyncType.FULL.name());
        syncLog.setStatus(SyncLog.Status.STARTED.name());
        syncLog.setStartTime(LocalDateTime.now());
        syncLog.setItemsToSync(calculateTotalItems(syncRequest));
        syncLog.setItemsSynced(0);
        syncLog.setItemsFailed(0);
        
        return syncLogRepository.save(syncLog);
    }
    
    private void updateSyncLog(SyncLog syncLog, int successCount, int failureCount, Device device, List<String> errors) {
        syncLog.setStatus(failureCount > 0 ? SyncLog.Status.PARTIAL.name() : SyncLog.Status.COMPLETED.name());
        syncLog.setEndTime(LocalDateTime.now());
        syncLog.setItemsSynced(successCount);
        syncLog.setItemsFailed(failureCount);
        syncLog.setErrorMessage(errors.isEmpty() ? null : String.join("; ", errors));
        
        syncLogRepository.save(syncLog);
    }
    
    private int calculateTotalItems(SyncRequest syncRequest) {
        return (syncRequest.getProducts() != null ? syncRequest.getProducts().size() : 0) +
               (syncRequest.getCustomers() != null ? syncRequest.getCustomers().size() : 0) +
               (syncRequest.getInvoices() != null ? syncRequest.getInvoices().size() : 0) +
               (syncRequest.getInvoiceLineItems() != null ? syncRequest.getInvoiceLineItems().size() : 0) +
               (syncRequest.getServiceRecords() != null ? syncRequest.getServiceRecords().size() : 0) +
               (syncRequest.getWorkers() != null ? syncRequest.getWorkers().size() : 0) +
               (syncRequest.getCreditSales() != null ? syncRequest.getCreditSales().size() : 0) +
               (syncRequest.getCreditSaleItems() != null ? syncRequest.getCreditSaleItems().size() : 0) +
               (syncRequest.getCreditSalePayments() != null ? syncRequest.getCreditSalePayments().size() : 0) +
               (syncRequest.getTyreExports() != null ? syncRequest.getTyreExports().size() : 0) +
               (syncRequest.getTyreExportPayments() != null ? syncRequest.getTyreExportPayments().size() : 0) +
               (syncRequest.getWorkerAttendances() != null ? syncRequest.getWorkerAttendances().size() : 0) +
               (syncRequest.getSalaryAdvances() != null ? syncRequest.getSalaryAdvances().size() : 0) +
               (syncRequest.getWorkerCredits() != null ? syncRequest.getWorkerCredits().size() : 0) +
               (syncRequest.getQuickServices() != null ? syncRequest.getQuickServices().size() : 0);
    }
    
    // Sync request and response DTOs
    public static class SyncRequest {
        private String deviceId;
        private String syncType = "FULL";
        private String operation = "UPLOAD";
        
        private List<ProductDTO> products;
        private List<CustomerDTO> customers;
        private List<InvoiceDTO> invoices;
        private List<InvoiceLineItemDTO> invoiceLineItems;
        private List<ServiceRecordDTO> serviceRecords;
        private List<WorkerDTO> workers;
        private List<CreditSaleDTO> creditSales;
        private List<CreditSaleItemDTO> creditSaleItems;
        private List<CreditSalePaymentDTO> creditSalePayments;
        private List<TyreExportDTO> tyreExports;
        private List<TyreExportPaymentDTO> tyreExportPayments;
        private List<WorkerAttendanceDTO> workerAttendances;
        private List<SalaryAdvanceDTO> salaryAdvances;
        private List<WorkerCreditDTO> workerCredits;
        private List<QuickServiceDTO> quickServices;
        
        // Getters and setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public String getSyncType() { return syncType; }
        public void setSyncType(String syncType) { this.syncType = syncType; }
        
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        
        public List<ProductDTO> getProducts() { return products; }
        public void setProducts(List<ProductDTO> products) { this.products = products; }
        
        public List<CustomerDTO> getCustomers() { return customers; }
        public void setCustomers(List<CustomerDTO> customers) { this.customers = customers; }
        
        public List<InvoiceDTO> getInvoices() { return invoices; }
        public void setInvoices(List<InvoiceDTO> invoices) { this.invoices = invoices; }
        
        public List<InvoiceLineItemDTO> getInvoiceLineItems() { return invoiceLineItems; }
        public void setInvoiceLineItems(List<InvoiceLineItemDTO> invoiceLineItems) { this.invoiceLineItems = invoiceLineItems; }
        
        public List<ServiceRecordDTO> getServiceRecords() { return serviceRecords; }
        public void setServiceRecords(List<ServiceRecordDTO> serviceRecords) { this.serviceRecords = serviceRecords; }
        
        public List<WorkerDTO> getWorkers() { return workers; }
        public void setWorkers(List<WorkerDTO> workers) { this.workers = workers; }
        
        public List<CreditSaleDTO> getCreditSales() { return creditSales; }
        public void setCreditSales(List<CreditSaleDTO> creditSales) { this.creditSales = creditSales; }
        
        public List<CreditSaleItemDTO> getCreditSaleItems() { return creditSaleItems; }
        public void setCreditSaleItems(List<CreditSaleItemDTO> creditSaleItems) { this.creditSaleItems = creditSaleItems; }
        
        public List<CreditSalePaymentDTO> getCreditSalePayments() { return creditSalePayments; }
        public void setCreditSalePayments(List<CreditSalePaymentDTO> creditSalePayments) { this.creditSalePayments = creditSalePayments; }
        
        public List<TyreExportDTO> getTyreExports() { return tyreExports; }
        public void setTyreExports(List<TyreExportDTO> tyreExports) { this.tyreExports = tyreExports; }
        
        public List<TyreExportPaymentDTO> getTyreExportPayments() { return tyreExportPayments; }
        public void setTyreExportPayments(List<TyreExportPaymentDTO> tyreExportPayments) { this.tyreExportPayments = tyreExportPayments; }
        
        public List<WorkerAttendanceDTO> getWorkerAttendances() { return workerAttendances; }
        public void setWorkerAttendances(List<WorkerAttendanceDTO> workerAttendances) { this.workerAttendances = workerAttendances; }
        
        public List<SalaryAdvanceDTO> getSalaryAdvances() { return salaryAdvances; }
        public void setSalaryAdvances(List<SalaryAdvanceDTO> salaryAdvances) { this.salaryAdvances = salaryAdvances; }
        
        public List<WorkerCreditDTO> getWorkerCredits() { return workerCredits; }
        public void setWorkerCredits(List<WorkerCreditDTO> workerCredits) { this.workerCredits = workerCredits; }
        
        public List<QuickServiceDTO> getQuickServices() { return quickServices; }
        public void setQuickServices(List<QuickServiceDTO> quickServices) { this.quickServices = quickServices; }
    }
    
    public static class SyncResponse {
        private boolean success;
        private String message;
        private String syncLogId;
        private int totalItems;
        private int successCount;
        private int failureCount;
        private List<String> errors = new ArrayList<>();
        
        // Data for download responses
        private List<ProductDTO> products;
        private List<CustomerDTO> customers;
        private List<InvoiceDTO> invoices;
        private List<InvoiceLineItemDTO> invoiceLineItems;
        private List<ServiceRecordDTO> serviceRecords;
        private List<WorkerDTO> workers;
        private List<CreditSaleDTO> creditSales;
        private List<CreditSaleItemDTO> creditSaleItems;
        private List<CreditSalePaymentDTO> creditSalePayments;
        private List<TyreExportDTO> tyreExports;
        private List<TyreExportPaymentDTO> tyreExportPayments;
        private List<WorkerAttendanceDTO> workerAttendances;
        private List<SalaryAdvanceDTO> salaryAdvances;
        private List<WorkerCreditDTO> workerCredits;
        private List<QuickServiceDTO> quickServices;
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getSyncLogId() { return syncLogId; }
        public void setSyncLogId(String syncLogId) { this.syncLogId = syncLogId; }
        
        public int getTotalItems() { return totalItems; }
        public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
        
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
        
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        
        public List<ProductDTO> getProducts() { return products; }
        public void setProducts(List<ProductDTO> products) { this.products = products; }
        
        public List<CustomerDTO> getCustomers() { return customers; }
        public void setCustomers(List<CustomerDTO> customers) { this.customers = customers; }
        
        public List<InvoiceDTO> getInvoices() { return invoices; }
        public void setInvoices(List<InvoiceDTO> invoices) { this.invoices = invoices; }
        
        public List<InvoiceLineItemDTO> getInvoiceLineItems() { return invoiceLineItems; }
        public void setInvoiceLineItems(List<InvoiceLineItemDTO> invoiceLineItems) { this.invoiceLineItems = invoiceLineItems; }
        
        public List<ServiceRecordDTO> getServiceRecords() { return serviceRecords; }
        public void setServiceRecords(List<ServiceRecordDTO> serviceRecords) { this.serviceRecords = serviceRecords; }
        
        public List<WorkerDTO> getWorkers() { return workers; }
        public void setWorkers(List<WorkerDTO> workers) { this.workers = workers; }
        
        public List<CreditSaleDTO> getCreditSales() { return creditSales; }
        public void setCreditSales(List<CreditSaleDTO> creditSales) { this.creditSales = creditSales; }
        
        public List<CreditSaleItemDTO> getCreditSaleItems() { return creditSaleItems; }
        public void setCreditSaleItems(List<CreditSaleItemDTO> creditSaleItems) { this.creditSaleItems = creditSaleItems; }
        
        public List<CreditSalePaymentDTO> getCreditSalePayments() { return creditSalePayments; }
        public void setCreditSalePayments(List<CreditSalePaymentDTO> creditSalePayments) { this.creditSalePayments = creditSalePayments; }
        
        public List<TyreExportDTO> getTyreExports() { return tyreExports; }
        public void setTyreExports(List<TyreExportDTO> tyreExports) { this.tyreExports = tyreExports; }
        
        public List<TyreExportPaymentDTO> getTyreExportPayments() { return tyreExportPayments; }
        public void setTyreExportPayments(List<TyreExportPaymentDTO> tyreExportPayments) { this.tyreExportPayments = tyreExportPayments; }
        
        public List<WorkerAttendanceDTO> getWorkerAttendances() { return workerAttendances; }
        public void setWorkerAttendances(List<WorkerAttendanceDTO> workerAttendances) { this.workerAttendances = workerAttendances; }
        
        public List<SalaryAdvanceDTO> getSalaryAdvances() { return salaryAdvances; }
        public void setSalaryAdvances(List<SalaryAdvanceDTO> salaryAdvances) { this.salaryAdvances = salaryAdvances; }
        
        public List<WorkerCreditDTO> getWorkerCredits() { return workerCredits; }
        public void setWorkerCredits(List<WorkerCreditDTO> workerCredits) { this.workerCredits = workerCredits; }
        
        public List<QuickServiceDTO> getQuickServices() { return quickServices; }
        public void setQuickServices(List<QuickServiceDTO> quickServices) { this.quickServices = quickServices; }
    }
}