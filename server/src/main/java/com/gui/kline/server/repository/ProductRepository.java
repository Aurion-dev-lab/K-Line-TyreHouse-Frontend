package com.gui.kline.server.repository;

import com.gui.kline.server.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Product entity.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {
    
    Optional<Product> findByProductCode(String productCode);
    
    List<Product> findByNameContainingIgnoreCase(String name);
    
    List<Product> findByCategory(String category);
    
    List<Product> findByCategoryContainingIgnoreCase(String category);
    
    List<Product> findByBrand(String brand);
    
    List<Product> findByBrandContainingIgnoreCase(String brand);
    
    List<Product> findByBarcode(String barcode);
    
    List<Product> findBySupplierContainingIgnoreCase(String supplier);
    
    List<Product> findByActive(boolean active);
    
    List<Product> findByFeatured(boolean featured);
    
    List<Product> findByStockLessThanEqual(int stock);
    
    List<Product> findByStockGreaterThanEqual(int stock);
    
    List<Product> findByStockGreaterThanAndStockLessThanEqual(int minStock, int maxStock);
    
    List<Product> findByDeviceId(String deviceId);
    
    List<Product> findBySyncStatus(boolean syncStatus);
    
    List<Product> findByDeviceIdAndSyncStatus(String deviceId, boolean syncStatus);
    
    List<Product> findBySyncedAtAfter(LocalDateTime syncedAfter);
    
    List<Product> findBySyncId(String syncId);
    
    @Query("SELECT p FROM Product p WHERE p.name LIKE %:query% OR p.productCode LIKE %:query% OR p.barcode LIKE %:query% OR p.description LIKE %:query%")
    List<Product> searchByQuery(String query);
    
    @Query("SELECT p FROM Product p WHERE p.stock <= p.minStockLevel AND p.active = true")
    List<Product> findLowStockProducts();
    
    long countByActive(boolean active);
    
    long countByCategory(String category);
    
    boolean existsByProductCode(String productCode);
    
    boolean existsByBarcode(String barcode);
}