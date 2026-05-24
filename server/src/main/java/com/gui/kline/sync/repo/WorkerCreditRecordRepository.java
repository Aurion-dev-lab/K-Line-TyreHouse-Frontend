package com.gui.kline.sync.repo;

import com.gui.kline.sync.model.WorkerCreditRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkerCreditRecordRepository extends JpaRepository<WorkerCreditRecord, String> {
	java.util.List<WorkerCreditRecord> findByWorkerAndDateAndAmountAndTypeAndNote(String worker, java.time.LocalDate date, double amount, String type, String note);
	java.util.List<WorkerCreditRecord> findByWorkerAndDateAndAmountAndNote(String worker, java.time.LocalDate date, double amount, String note);
}

