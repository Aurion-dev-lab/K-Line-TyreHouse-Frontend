package com.gui.kline.server.repository;

import com.gui.kline.server.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Customer entity.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, String>, JpaSpecificationExecutor<Customer> {
    
    Optional<Customer> findByPhone(String phone);
    
    List<Customer> findByNameContainingIgnoreCase(String name);
    
    List<Customer> findByCompanyNameContainingIgnoreCase(String companyName);
    
    List<Customer> findByEmailContainingIgnoreCase(String email);
    
    List<Customer> findByCustomerType(Customer.CustomerType customerType);
    
    List<Customer> findByActive(boolean active);
    
    List<Customer> findByMemberSinceAfter(LocalDate memberSinceAfter);
    
    List<Customer> findByDeviceId(String deviceId);
    
    List<Customer> findBySyncStatus(boolean syncStatus);
    
    List<Customer> findByDeviceIdAndSyncStatus(String deviceId, boolean syncStatus);
    
    List<Customer> findBySyncedAtAfter(LocalDateTime syncedAfter);
    
    @Query("SELECT c FROM Customer c WHERE c.name LIKE %:query% OR c.phone LIKE %:query% OR c.email LIKE %:query% OR c.companyName LIKE %:query%")
    List<Customer> searchByQuery(String query);
    
    @Query("SELECT c FROM Customer c WHERE c.creditLimit > 0 AND c.currentCredit < c.creditLimit AND c.active = true")
    List<Customer> findCustomersWithAvailableCredit();
    
    @Query("SELECT c FROM Customer c WHERE c.currentCredit > 0 AND c.active = true")
    List<Customer> findCustomersWithCurrentCredit();
    
    @Query("SELECT c FROM Customer c WHERE c.currentCredit > c.creditLimit AND c.active = true")
    List<Customer> findCustomersOverCreditLimit();
    
    @Query("SELECT c FROM Customer c WHERE c.memberSince BETWEEN :startDate AND :endDate")
    List<Customer> findByJoinDateRange(LocalDate startDate, LocalDate endDate);
    
    long countByActive(boolean active);
    
    long countByCustomerType(Customer.CustomerType customerType);
    
    boolean existsByPhone(String phone);
    
    boolean existsByEmail(String email);
}