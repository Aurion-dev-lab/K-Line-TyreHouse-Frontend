package com.gui.kline.sync.repo;

import com.gui.kline.sync.model.CreditSaleRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditSaleRecordRepository extends JpaRepository<CreditSaleRecord, String> {
	CreditSaleRecord findByCreditId(String creditId);
}

