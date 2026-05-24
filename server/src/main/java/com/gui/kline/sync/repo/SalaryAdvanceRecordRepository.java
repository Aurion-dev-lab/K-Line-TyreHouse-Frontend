package com.gui.kline.sync.repo;

import com.gui.kline.sync.model.SalaryAdvanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryAdvanceRecordRepository extends JpaRepository<SalaryAdvanceRecord, String> {
	java.util.List<SalaryAdvanceRecord> findByWorkerAndDateAndAmountAndNote(String worker, java.time.LocalDate date, double amount, String note);
}

