package com.gui.kline.sync.repo;

import com.gui.kline.sync.model.InvoiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRecordRepository extends JpaRepository<InvoiceRecord, String> {
}

