package com.gui.kline.server.repository;

import com.gui.kline.server.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Worker entity.
 */
@Repository
public interface WorkerRepository extends JpaRepository<Worker, String>, JpaSpecificationExecutor<Worker> {
    
    Optional<Worker> findByNic(String nic);
    
    Optional<Worker> findByPhone(String phone);
    
    Optional<Worker> findByEmail(String email);
    
    List<Worker> findByFirstNameContainingIgnoreCase(String firstName);
    
    List<Worker> findByLastNameContainingIgnoreCase(String lastName);
    
    List<Worker> findByFullNameContainingIgnoreCase(String fullName);
    
    List<Worker> findByRole(String role);
    
    List<Worker> findByRoleIn(List<String> roles);
    
    List<Worker> findBySalaryType(Worker.SalaryType salaryType);
    
    List<Worker> findByDepartment(String department);
    
    List<Worker> findByDesignation(String designation);
    
    List<Worker> findByActive(boolean active);
    
    List<Worker> findByJoinDate(LocalDate joinDate);
    
    List<Worker> findByJoinDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<Worker> findByDeviceId(String deviceId);
    
    List<Worker> findBySyncStatus(boolean syncStatus);
    
    List<Worker> findByDeviceIdAndSyncStatus(String deviceId, boolean syncStatus);
    
    List<Worker> findBySyncedAtAfter(LocalDateTime syncedAfter);
    
    @Query("SELECT w FROM Worker w WHERE w.firstName LIKE %:query% OR w.lastName LIKE %:query% OR w.fullName LIKE %:query% OR w.phone LIKE %:query% OR w.nic LIKE %:query%")
    List<Worker> searchByQuery(String query);
    
    @Query("SELECT w FROM Worker w WHERE w.role IN ('TECHNICIAN', 'MECHANIC', 'SERVICE_TECHNICIAN')")
    List<Worker> findTechnicians();
    
    @Query("SELECT w FROM Worker w WHERE w.active = true AND w.terminationDate IS NULL")
    List<Worker> findActiveEmployees();
    
    long countByActive(boolean active);
    
    long countByRole(String role);
    
    long countBySalaryType(Worker.SalaryType salaryType);
    
    boolean existsByNic(String nic);
    
    boolean existsByPhone(String phone);
    
    boolean existsByEmail(String email);
}