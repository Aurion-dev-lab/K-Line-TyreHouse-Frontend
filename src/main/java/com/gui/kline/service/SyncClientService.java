package com.gui.kline.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gui.kline.data.*;
import com.gui.kline.models.*;
import com.gui.kline.utils.AlertUtil;
import com.gui.kline.utils.JsonUtil;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import okhttp3.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Client-side service for handling synchronization with the remote server.
 * This service manages the upload and download of data when the upload button is clicked.
 */
public class SyncClientService {
    
    private static final String DEFAULT_SERVER_URL = "http://localhost:8080";
    private static final String API_SYNC_UPLOAD = "/api/sync/upload";
    private static final String API_SYNC_DOWNLOAD = "/api/sync/download/full";
    private static final String API_SYNC_INCREMENTAL = "/api/sync/download/incremental";
    private static final String API_PING = "/api/sync/ping";
    private static final String API_DEVICE_REGISTER = "/api/devices/register";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String serverUrl;
    private String deviceId;
    private String apiKey;
    private LocalDateTime lastSyncTime;
    
    private boolean isOnline = false;
    private boolean isSyncing = false;
    
    private List<Consumer<SyncProgress>> syncProgressListeners = new ArrayList<>();
    private List<Consumer<SyncResult>> syncResultListeners = new ArrayList<>();
    
    public SyncClientService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.objectMapper = JsonUtil.createObjectMapper();
        this.serverUrl = DEFAULT_SERVER_URL;
        this.lastSyncTime = LocalDateTime.now().minusDays(30); // Initial sync gets 30 days of data
    }
    
    /**
     * Initialize the sync service with device information
     */
    public void initialize(String deviceId, String apiKey, String serverUrl) {
        this.deviceId = deviceId;
        this.apiKey = apiKey;
        if (serverUrl != null && !serverUrl.isBlank()) {
            this.serverUrl = serverUrl;
        }
        
        // Check online status
        checkOnlineStatus();
    }
    
    /**
     * Check if the server is online
     */
    public void checkOnlineStatus() {
        CompletableFuture.runAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(serverUrl + API_PING)
                        .get()
                        .build();
                
                Response response = httpClient.newCall(request).execute();
                this.isOnline = response.isSuccessful();
                
                Platform.runLater(() -> notifyProgress(new SyncProgress(
                    SyncProgress.Type.STATUS, 
                    "Server " + (isOnline ? "online" : "offline"), 
                    isOnline ? 100 : 0
                )));
                
            } catch (IOException e) {
                this.isOnline = false;
                Platform.runLater(() -> notifyProgress(new SyncProgress(
                    SyncProgress.Type.ERROR, 
                    "Server unreachable: " + e.getMessage(), 
                    0
                )));
            }
        });
    }
    
    /**
     * Upload data to the server (called when upload button is clicked)
     */
    public void uploadData() {
        if (isSyncing) {
            Platform.runLater(() -> AlertUtil.showWarning("Sync in progress", "A sync operation is already in progress"));
            return;
        }
        
        if (!isOnline) {
            Platform.runLater(() -> AlertUtil.showWarning("Offline", "Cannot sync while offline. Please check your internet connection."));
            return;
        }
        
        if (deviceId == null || apiKey == null) {
            Platform.runLater(() -> AlertUtil.showError("Device not registered", "Device ID and API key are required for sync"));
            registerDeviceIfNeeded();
            return;
        }
        
        isSyncing = true;
        notifyProgress(new SyncProgress(SyncProgress.Type.STARTED, "Preparing data for upload...", 0));
        
        CompletableFuture.runAsync(() -> {
            try {
                // Collect all data that needs to be synced
                SyncRequest syncRequest = collectDataForSync();
                syncRequest.setDeviceId(deviceId);
                syncRequest.setSyncType("FULL");
                syncRequest.setOperation("UPLOAD");
                
                // Send data to server
                String jsonPayload = objectMapper.writeValueAsString(syncRequest);
                
                Request request = new Request.Builder()
                        .url(serverUrl + API_SYNC_UPLOAD)
                        .post(RequestBody.create(jsonPayload, MediaType.parse("application/json")))
                        .addHeader("Content-Type", "application/json")
                        .addHeader("X-DEVICE-ID", deviceId)
                        .addHeader("X-API-KEY", apiKey)
                        .build();
                
                Response response = httpClient.newCall(request).execute();
                
                if (response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "{}";
                    SyncResponse syncResponse = objectMapper.readValue(responseBody, SyncResponse.class);
                    
                    // Update last sync time
                    lastSyncTime = LocalDateTime.now();
                    
                    // Mark all synced items as synced in local database
                    markItemsAsSynced(syncRequest);
                    
                    SyncResult result = new SyncResult();
                    result.setSuccess(true);
                    result.setMessage("Sync completed successfully");
                    result.setItemsUploaded(syncResponse.getSuccessCount());
                    result.setItemsFailed(syncResponse.getFailureCount());
                    result.setTotalItems(syncResponse.getTotalItems());
                    result.setErrors(syncResponse.getErrors());
                    result.setSyncTime(LocalDateTime.now());
                    
                    Platform.runLater(() -> {
                        notifyResult(result);
                        notifyProgress(new SyncProgress(SyncProgress.Type.COMPLETED, result.getMessage(), 100));
                    });
                    
                } else {
                    String errorMessage = "Sync failed with HTTP " + response.code();
                    try {
                        String bodyMessage = objectMapper.readTree(response.body().string()).get("message").asText();
                        if (bodyMessage != null && !bodyMessage.isEmpty()) {
                            errorMessage = bodyMessage;
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                    
                    final String finalErrorMessage = errorMessage;
                    SyncResult result = new SyncResult();
                    result.setSuccess(false);
                    result.setMessage(finalErrorMessage);
                    result.setSyncTime(LocalDateTime.now());
                    
                    Platform.runLater(() -> {
                        notifyResult(result);
                        notifyProgress(new SyncProgress(SyncProgress.Type.ERROR, finalErrorMessage, 0));
                    });
                }
                
            } catch (JsonProcessingException e) {
                SyncResult result = new SyncResult();
                result.setSuccess(false);
                result.setMessage("Failed to serialize data: " + e.getMessage());
                result.setSyncTime(LocalDateTime.now());
                
                Platform.runLater(() -> {
                    notifyResult(result);
                    notifyProgress(new SyncProgress(SyncProgress.Type.ERROR, result.getMessage(), 0));
                });
                
            } catch (IOException e) {
                SyncResult result = new SyncResult();
                result.setSuccess(false);
                result.setMessage("Network error: " + e.getMessage());
                result.setSyncTime(LocalDateTime.now());
                
                Platform.runLater(() -> {
                    notifyResult(result);
                    notifyProgress(new SyncProgress(SyncProgress.Type.ERROR, result.getMessage(), 0));
                });
                
            } finally {
                isSyncing = false;
            }
        });
    }
    
    /**
     * Download data from the server
     */
    public void downloadData() {
        if (isSyncing) {
            Platform.runLater(() -> AlertUtil.showWarning("Sync in progress", "A sync operation is already in progress"));
            return;
        }
        
        if (!isOnline) {
            Platform.runLater(() -> AlertUtil.showWarning("Offline", "Cannot download while offline"));
            return;
        }
        
        if (deviceId == null || apiKey == null) {
            Platform.runLater(() -> AlertUtil.showError("Device not registered", "Device ID and API key are required"));
            return;
        }
        
        isSyncing = true;
        notifyProgress(new SyncProgress(SyncProgress.Type.STARTED, "Downloading data from server...", 0));
        
        CompletableFuture.runAsync(() -> {
            try {
                String url = serverUrl + API_SYNC_DOWNLOAD + "?deviceId=" + deviceId;
                
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("X-DEVICE-ID", deviceId)
                        .addHeader("X-API-KEY", apiKey)
                        .build();
                
                Response response = httpClient.newCall(request).execute();
                
                if (response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "{}";
                    SyncResponse syncResponse = objectMapper.readValue(responseBody, SyncResponse.class);
                    
                    // Process downloaded data
                    processDownloadedData(syncResponse);
                    
                    SyncResult result = new SyncResult();
                    result.setSuccess(true);
                    result.setMessage("Download completed successfully");
                    result.setItemsDownloaded(syncResponse.getTotalItems());
                    result.setSyncTime(LocalDateTime.now());
                    
                    Platform.runLater(() -> {
                        notifyResult(result);
                        notifyProgress(new SyncProgress(SyncProgress.Type.COMPLETED, result.getMessage(), 100));
                    });
                    
                } else {
                    String errorMessage = "Download failed with HTTP " + response.code();
                    
                    SyncResult result = new SyncResult();
                    result.setSuccess(false);
                    result.setMessage(errorMessage);
                    result.setSyncTime(LocalDateTime.now());
                    
                    Platform.runLater(() -> {
                        notifyResult(result);
                        notifyProgress(new SyncProgress(SyncProgress.Type.ERROR, errorMessage, 0));
                    });
                }
                
            } catch (IOException e) {
                SyncResult result = new SyncResult();
                result.setSuccess(false);
                result.setMessage("Network error: " + e.getMessage());
                result.setSyncTime(LocalDateTime.now());
                
                Platform.runLater(() -> {
                    notifyResult(result);
                    notifyProgress(new SyncProgress(SyncProgress.Type.ERROR, result.getMessage(), 0));
                });
                
            } finally {
                isSyncing = false;
            }
        });
    }
    
    /**
     * Register the device with the server if not already registered
     */
    private void registerDeviceIfNeeded() {
        if (deviceId == null || apiKey == null) {
            // Generate device info
            String deviceName = "JavaFX-Client-" + UUID.randomUUID().toString().substring(0, 8);
            String generatedDeviceId = UUID.randomUUID().toString();
            
            try {
                DeviceRegistrationRequest request = new DeviceRegistrationRequest();
                request.setDeviceId(generatedDeviceId);
                request.setName(deviceName);
                request.setShopName("Default Shop");
                
                String jsonPayload = objectMapper.writeValueAsString(request);
                
                Request httpRequest = new Request.Builder()
                        .url(serverUrl + API_DEVICE_REGISTER)
                        .post(RequestBody.create(jsonPayload, MediaType.parse("application/json")))
                        .addHeader("Content-Type", "application/json")
                        .build();
                
                Response response = httpClient.newCall(httpRequest).execute();
                
                if (response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "{}";
                    DeviceRegistrationResponse regResponse = objectMapper.readValue(responseBody, DeviceRegistrationResponse.class);
                    
                    this.deviceId = regResponse.getDeviceId();
                    this.apiKey = regResponse.getApiKey();
                    
                    Platform.runLater(() -> notifyProgress(new SyncProgress(
                        SyncProgress.Type.INFO, 
                        "Device registered successfully", 
                        100
                    )));
                    
                } else {
                    Platform.runLater(() -> AlertUtil.showError("Registration failed", "Failed to register device: " + response.code()));
                }
                
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Registration error", "Failed to register device: " + e.getMessage()));
            }
        }
    }
    
    /**
     * Collect all data that needs to be synced
     */
    private SyncRequest collectDataForSync() {
        SyncRequest request = new SyncRequest();
        
        try {
            // Get all unsynced products
            List<Product> unsyncedProducts = new ProductRepository().getUnsyncedProducts();
            request.setProducts(convertProducts(unsyncedProducts));
            
            // Get all unsynced customers
            List<Customer> unsyncedCustomers = new CustomerRepository().getUnsyncedCustomers();
            request.setCustomers(convertCustomers(unsyncedCustomers));
            
            // Get all unsynced invoices
            List<Invoice> unsyncedInvoices = new InvoiceRepository().getUnsyncedInvoices();
            request.setInvoices(convertInvoices(unsyncedInvoices));
            
            // Get all unsynced invoice line items
            List<LineItem> unsyncedLineItems = new InvoiceLineItemRepository().getUnsyncedLineItems();
            request.setInvoiceLineItems(convertLineItems(unsyncedLineItems));
            
            // Get all unsynced services
            List<ServiceRecord> unsyncedServices = new ServiceRecordRepository().getUnsyncedServices();
            request.setServiceRecords(convertServiceRecords(unsyncedServices));
            
            // Get all unsynced workers
            List<Worker> unsyncedWorkers = new WorkerRepository().getUnsyncedWorkers();
            request.setWorkers(convertWorkers(unsyncedWorkers));
            
            // Get all unsynced credit sales
            List<CreditSale> unsyncedCreditSales = new CreditSaleRepository().getUnsyncedCreditSales();
            request.setCreditSales(convertCreditSales(unsyncedCreditSales));
            
            // Get all unsynced tyre exports
            List<TyreExport> unsyncedExports = new TyreExportRepository().getUnsyncedExports();
            request.setTyreExports(convertTyreExports(unsyncedExports));
            
            // Get all unsynced attendances
            List<WorkerAttendance> unsyncedAttendances = new WorkerAttendanceRepository().getUnsyncedAttendances();
            request.setWorkerAttendances(convertAttendances(unsyncedAttendances));
            
            // Get all unsynced quick services
            List<QuickService> unsyncedQuickServices = new QuickServiceRepository().getUnsyncedQuickServices();
            request.setQuickServices(convertQuickServices(unsyncedQuickServices));
            
        } catch (Exception e) {
            // Log error but continue with available data
            System.err.println("Error collecting data for sync: " + e.getMessage());
            e.printStackTrace();
        }
        
        return request;
    }
    
    /**
     * Process downloaded data and update local database
     */
    private void processDownloadedData(SyncResponse response) {
        // This would update the local database with the downloaded data
        // For now, just mark that data was downloaded
        System.out.println("Processing downloaded data:");
        System.out.println("Products: " + (response.getProducts() != null ? response.getProducts().size() : 0));
        System.out.println("Customers: " + (response.getCustomers() != null ? response.getCustomers().size() : 0));
        System.out.println("Invoices: " + (response.getInvoices() != null ? response.getInvoices().size() : 0));
        
        // Update last sync time
        lastSyncTime = LocalDateTime.now();
    }
    
    /**
     * Mark items as synced in the local database
     */
    private void markItemsAsSynced(SyncRequest request) {
        // This would update the syncStatus field in the local database
        // For all items that were successfully uploaded
        try {
            // Update products
            if (request.getProducts() != null) {
                for (ProductDTO product : request.getProducts()) {
                    new ProductRepository().markAsSynced(product.getId());
                }
            }
            
            // Update customers
            if (request.getCustomers() != null) {
                for (CustomerDTO customer : request.getCustomers()) {
                    new CustomerRepository().markAsSynced(customer.getId());
                }
            }
            
            // Similar updates for other entity types
            
        } catch (Exception e) {
            System.err.println("Error marking items as synced: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Conversion methods
    private List<ProductDTO> convertProducts(List<Product> products) {
        List<ProductDTO> dtos = new ArrayList<>();
        for (Product product : products) {
            ProductDTO dto = new ProductDTO();
            dto.setId(product.getId());
            dto.setProductCode(product.getProductCode());
            dto.setName(product.getName());
            dto.setCategory(product.getCategory());
            dto.setBuyPrice(product.getBuyPrice());
            dto.setSellPrice(product.getSellPrice());
            dto.setStock(product.getStock());
            dto.setActive(product.isActive());
            // Set other fields as needed
            dtos.add(dto);
        }
        return dtos;
    }
    
    private List<CustomerDTO> convertCustomers(List<Customer> customers) {
        List<CustomerDTO> dtos = new ArrayList<>();
        for (Customer customer : customers) {
            CustomerDTO dto = new CustomerDTO();
            dto.setId(customer.getId());
            dto.setName(customer.getName());
            dto.setPhone(customer.getPhone());
            dto.setEmail(customer.getEmail());
            dto.setActive(customer.isActive());
            // Set other fields as needed
            dtos.add(dto);
        }
        return dtos;
    }
    
    private List<InvoiceDTO> convertInvoices(List<Invoice> invoices) {
        List<InvoiceDTO> dtos = new ArrayList<>();
        for (Invoice invoice : invoices) {
            InvoiceDTO dto = new InvoiceDTO();
            dto.setId(invoice.getId());
            dto.setInvoiceNumber(invoice.getInvoiceNumber());
            dto.setCustomerId(invoice.getCustomerId());
            dto.setCustomerName(invoice.getCustomerName());
            dto.setInvoiceDate(invoice.getInvoiceDate());
            dto.setGrandTotal(invoice.getGrandTotal());
            dto.setAmountPaid(invoice.getAmountPaid());
            dto.setBalanceDue(invoice.getBalanceDue());
            // Set other fields as needed
            dtos.add(dto);
        }
        return dtos;
    }
    
    private List<InvoiceLineItemDTO> convertLineItems(List<LineItem> lineItems) {
        List<InvoiceLineItemDTO> dtos = new ArrayList<>();
        for (LineItem item : lineItems) {
            InvoiceLineItemDTO dto = new InvoiceLineItemDTO();
            dto.setId(item.getId());
            dto.setInvoiceId(item.getInvoiceId());
            dto.setProductId(item.getProductId());
            dto.setDescription(item.getDescription());
            dto.setQuantity(item.getQuantity());
            dto.setUnitPrice(item.getUnitPrice());
            dto.setTotal(item.getTotal());
            dtos.add(dto);
        }
        return dtos;
    }
    
    private List<ServiceRecordDTO> convertServiceRecords(List<ServiceRecord> services) {
        List<ServiceRecordDTO> dtos = new ArrayList<>();
        for (ServiceRecord service : services) {
            ServiceRecordDTO dto = new ServiceRecordDTO();
            dto.setId(service.getId());
            dto.setServiceName(service.getServiceName());
            dto.setPrice(service.getPrice());
            dto.setServiceDate(service.getServiceDate());
            dto.setCustomerId(service.getCustomerId());
            dto.setCustomerName(service.getCustomerName());
            dtos.add(dto);
        }
        return dtos;
    }
    
    private List<WorkerDTO> convertWorkers(List<Worker> workers) {
        List<WorkerDTO> dtos = new ArrayList<>();
        for (Worker worker : workers) {
            WorkerDTO dto = new WorkerDTO();
            dto.setId(worker.getId());
            dto.setFirstName(worker.getFirstName());
            dto.setLastName(worker.getLastName());
            dto.setPhone(worker.getPhone());
            dto.setRole(worker.getRole());
            dto.setActive(worker.isActive());
            dtos.add(dto);
        }
        return dtos;
    }
    
    private List<CreditSaleDTO> convertCreditSales(List<CreditSale> creditSales) {
        List<CreditSaleDTO> dtos = new ArrayList<>();
        for (CreditSale sale : creditSales) {
            CreditSaleDTO dto = new CreditSaleDTO();
            dto.setId(sale.getId());
            dto.setCreditSaleNumber(sale.getCreditSaleNumber());
            dto.setCustomerId(sale.getCustomerId());
            dto.setCustomerName(sale.getCustomerName());
            dto.setSaleDate(sale.getSaleDate());
            dto.setAmount(sale.getAmount());
            dto.setPaidAmount(sale.getPaidAmount());
            dto.setBalanceAmount(sale.getBalanceAmount());
            dtos.add(dto);
        }
        return dtos;
    }
    
    private List<TyreExportDTO> convertTyreExports(List<TyreExport> exports) {
        List<TyreExportDTO> dtos = new ArrayList<>();
        for (TyreExport export : exports) {
            TyreExportDTO dto = new TyreExportDTO();
            dto.setId(export.getId());
            dto.setExportId(export.getExportId());
            dto.setCompany(export.getCompany());
            dto.setExportDate(export.getExportDate());
            dto.setTyres(export.getTyres());
            dto.setCustPrice(export.getCustPrice());
            dto.setCompPrice(export.getCompPrice());
            dto.setTotalAmount(export.getTotalAmount());
            dtos.add(dto);
        }
        return dtos;
    }
    
    private List<WorkerAttendanceDTO> convertAttendances(List<WorkerAttendance> attendances) {
        List<WorkerAttendanceDTO> dtos = new ArrayList<>();
        for (WorkerAttendance attendance : attendances) {
            WorkerAttendanceDTO dto = new WorkerAttendanceDTO();
            dto.setId(attendance.getId());
            dto.setWorkerId(attendance.getWorkerId());
            dto.setWorkerName(attendance.getWorkerName());
            dto.setAttendanceDate(attendance.getAttendanceDate());
            dto.setCheckInTime(attendance.getCheckInTime());
            dto.setCheckOutTime(attendance.getCheckOutTime());
            dto.setStatus(attendance.getStatus());
            dtos.add(dto);
        }
        return dtos;
    }
    
    private List<QuickServiceDTO> convertQuickServices(List<QuickService> services) {
        List<QuickServiceDTO> dtos = new ArrayList<>();
        for (QuickService service : services) {
            QuickServiceDTO dto = new QuickServiceDTO();
            dto.setId(service.getId());
            dto.setService(service.getService());
            dto.setPrice(service.getPrice());
            dto.setServiceDate(service.getServiceDate());
            dto.setCustomerName(service.getCustomerName());
            dto.setVehicleNumber(service.getVehicleNumber());
            dtos.add(dto);
        }
        return dtos;
    }
    
    // Listener methods
    public void addSyncProgressListener(Consumer<SyncProgress> listener) {
        syncProgressListeners.add(listener);
    }
    
    public void removeSyncProgressListener(Consumer<SyncProgress> listener) {
        syncProgressListeners.remove(listener);
    }
    
    public void addSyncResultListener(Consumer<SyncResult> listener) {
        syncResultListeners.add(listener);
    }
    
    public void removeSyncResultListener(Consumer<SyncResult> listener) {
        syncResultListeners.remove(listener);
    }
    
    private void notifyProgress(SyncProgress progress) {
        for (Consumer<SyncProgress> listener : syncProgressListeners) {
            try {
                listener.accept(progress);
            } catch (Exception e) {
                System.err.println("Error in sync progress listener: " + e.getMessage());
            }
        }
    }
    
    private void notifyResult(SyncResult result) {
        for (Consumer<SyncResult> listener : syncResultListeners) {
            try {
                listener.accept(result);
            } catch (Exception e) {
                System.err.println("Error in sync result listener: " + e.getMessage());
            }
        }
    }
    
    // Getters and setters
    public boolean isOnline() {
        return isOnline;
    }
    
    public boolean isSyncing() {
        return isSyncing;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public String getServerUrl() {
        return serverUrl;
    }
    
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
    
    public LocalDateTime getLastSyncTime() {
        return lastSyncTime;
    }
    
    // DTO classes for sync operations
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
        private List<InvoiceLineItemDTO> creditSaleItems;
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
        public List<InvoiceLineItemDTO> getCreditSaleItems() { return creditSaleItems; }
        public void setCreditSaleItems(List<InvoiceLineItemDTO> creditSaleItems) { this.creditSaleItems = creditSaleItems; }
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
        private List<String> errors;
        
        private List<ProductDTO> products;
        private List<CustomerDTO> customers;
        private List<InvoiceDTO> invoices;
        private List<InvoiceLineItemDTO> invoiceLineItems;
        private List<ServiceRecordDTO> serviceRecords;
        private List<WorkerDTO> workers;
        private List<CreditSaleDTO> creditSales;
        private List<InvoiceLineItemDTO> creditSaleItems;
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
        public List<InvoiceLineItemDTO> getCreditSaleItems() { return creditSaleItems; }
        public void setCreditSaleItems(List<InvoiceLineItemDTO> creditSaleItems) { this.creditSaleItems = creditSaleItems; }
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
    
    public static class SyncProgress {
        public enum Type { STARTED, IN_PROGRESS, COMPLETED, ERROR, STATUS, INFO }
        
        private Type type;
        private String message;
        private int progressPercentage;
        private Object data;
        
        public SyncProgress(Type type, String message, int progressPercentage) {
            this.type = type;
            this.message = message;
            this.progressPercentage = progressPercentage;
        }
        
        public SyncProgress(Type type, String message, int progressPercentage, Object data) {
            this(type, message, progressPercentage);
            this.data = data;
        }
        
        // Getters
        public Type getType() { return type; }
        public String getMessage() { return message; }
        public int getProgressPercentage() { return progressPercentage; }
        public Object getData() { return data; }
    }
    
    public static class SyncResult {
        private boolean success;
        private String message;
        private LocalDateTime syncTime;
        private int itemsUploaded;
        private int itemsDownloaded;
        private int itemsFailed;
        private int totalItems;
        private List<String> errors;
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public LocalDateTime getSyncTime() { return syncTime; }
        public void setSyncTime(LocalDateTime syncTime) { this.syncTime = syncTime; }
        public int getItemsUploaded() { return itemsUploaded; }
        public void setItemsUploaded(int itemsUploaded) { this.itemsUploaded = itemsUploaded; }
        public int getItemsDownloaded() { return itemsDownloaded; }
        public void setItemsDownloaded(int itemsDownloaded) { this.itemsDownloaded = itemsDownloaded; }
        public int getItemsFailed() { return itemsFailed; }
        public void setItemsFailed(int itemsFailed) { this.itemsFailed = itemsFailed; }
        public int getTotalItems() { return totalItems; }
        public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
    }
    
    public static class DeviceRegistrationRequest {
        private String deviceId;
        private String name;
        private String shopName;
        private String location;
        private String contactPhone;
        private String contactEmail;
        
        // Getters and setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getShopName() { return shopName; }
        public void setShopName(String shopName) { this.shopName = shopName; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
        public String getContactEmail() { return contactEmail; }
        public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    }
    
    public static class DeviceRegistrationResponse {
        private String id;
        private String deviceId;
        private String name;
        private String apiKey;
        private String shopName;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getShopName() { return shopName; }
        public void setShopName(String shopName) { this.shopName = shopName; }
    }
}