package com.gui.kline.sync.repo;

import com.gui.kline.sync.model.WorkerRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkerRecordRepository extends JpaRepository<WorkerRecord, String> {
}

