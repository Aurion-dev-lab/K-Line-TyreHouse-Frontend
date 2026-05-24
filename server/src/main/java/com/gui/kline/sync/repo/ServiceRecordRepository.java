package com.gui.kline.sync.repo;

import com.gui.kline.sync.model.ServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, String> {
}

